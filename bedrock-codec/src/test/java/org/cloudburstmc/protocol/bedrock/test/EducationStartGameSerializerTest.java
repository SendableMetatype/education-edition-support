package org.cloudburstmc.protocol.bedrock.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v1002.Bedrock_v1002;
import org.cloudburstmc.protocol.bedrock.codec.v1002.serializer.EducationStartGameSerializer_v1002;
import org.cloudburstmc.protocol.bedrock.codec.v944.serializer.StartGameSerializer_v944;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v898.serializer.EducationStartGameSerializer_v898;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.NetworkPermissions;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.SpawnBiomeType;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EducationStartGameSerializerTest {
    private static final BedrockCodecHelper V898_CODEC_HELPER = Bedrock_v898.EDUCATION_CODEC.createHelper();
    private static final EducationStartGameSerializer_v898 V898_SERIALIZER = EducationStartGameSerializer_v898.INSTANCE;
    private static final BedrockCodecHelper V1002_CODEC_HELPER = Bedrock_v1002.EDUCATION_CODEC.createHelper();
    private static final TestStartGameSerializer_v944 V944_SERIALIZER = new TestStartGameSerializer_v944();
    private static final TestEducationStartGameSerializer_v1002 V1002_SERIALIZER = new TestEducationStartGameSerializer_v1002();

    // Varint string lengths and UTF-8 data, signed VarInt policy 3, then true.
    private static final byte[] V1002_LEVEL_SETTINGS_TAIL = {
            1, 'C', 1, 'W', 1, 'R', 6, 1
    };

    @Test
    public void testV1002CodecRegistration() {
        assertEquals(1002, Bedrock_v1002.EDUCATION_CODEC.getProtocolVersion());
        assertSame(EducationStartGameSerializer_v1002.INSTANCE,
                Bedrock_v1002.EDUCATION_CODEC.getPacketDefinition(StartGamePacket.class).getSerializer());
    }

    @Test
    public void testEducationFieldsSurviveRoundTrip() {
        StartGamePacket packet = new StartGamePacket();
        populateRequiredFields(packet);

        packet.setEducationReferrerId("referrer-xyz");
        packet.setEducationCreatorWorldId("world-abc");
        packet.setEducationCreatorId("creator-123");

        ByteBuf buf = Unpooled.buffer();
        V898_SERIALIZER.serialize(buf, V898_CODEC_HELPER, packet);

        StartGamePacket out = new StartGamePacket();
        V898_SERIALIZER.deserialize(buf, V898_CODEC_HELPER, out);

        assertEquals("referrer-xyz", out.getEducationReferrerId());
        assertEquals("world-abc", out.getEducationCreatorWorldId());
        assertEquals("creator-123", out.getEducationCreatorId());
    }

    @Test
    public void testV1002LevelSettingsWireLayout() {
        StartGamePacket packet = new StartGamePacket();
        populateRequiredFields(packet);
        packet.setEducationCreatorId("C");
        packet.setEducationCreatorWorldId("W");
        packet.setEducationReferrerId("R");
        packet.setServerEditorConnectionPolicy(3);
        packet.setAllowAnonymousBlockDropsInEditorWorlds(true);

        ByteBuf v944 = Unpooled.buffer();
        ByteBuf encoded = Unpooled.buffer();
        ByteBuf fixture = Unpooled.buffer();
        try {
            V944_SERIALIZER.writeLevelSettingsForTest(v944, V1002_CODEC_HELPER, packet);
            V1002_SERIALIZER.writeLevelSettingsForTest(encoded, V1002_CODEC_HELPER, packet);

            assertEquals(v944, encoded.readSlice(v944.readableBytes()));
            assertArrayEquals(V1002_LEVEL_SETTINGS_TAIL, ByteBufUtil.getBytes(encoded));

            fixture.writeBytes(v944, v944.readerIndex(), v944.readableBytes());
            fixture.writeBytes(V1002_LEVEL_SETTINGS_TAIL);
            StartGamePacket decoded = new StartGamePacket();
            V1002_SERIALIZER.readLevelSettingsForTest(fixture, V1002_CODEC_HELPER, decoded);

            assertEquals("C", decoded.getEducationCreatorId());
            assertEquals("W", decoded.getEducationCreatorWorldId());
            assertEquals("R", decoded.getEducationReferrerId());
            assertEquals(3, decoded.getServerEditorConnectionPolicy());
            assertTrue(decoded.isAllowAnonymousBlockDropsInEditorWorlds());
            assertFalse(fixture.isReadable());
        } finally {
            v944.release();
            encoded.release();
            fixture.release();
        }
    }

    @Test
    public void testV1002OmitsLoggingChat() {
        StartGamePacket packet = new StartGamePacket();
        packet.setNetworkPermissions(new NetworkPermissions(true));
        packet.setLoggingChat(true);

        ByteBuf encoded = Unpooled.buffer();
        ByteBuf fixture = Unpooled.wrappedBuffer(new byte[]{1});
        try {
            V1002_SERIALIZER.writeNetworkPermissionsTailForTest(encoded, V1002_CODEC_HELPER, packet);
            assertArrayEquals(new byte[]{1}, ByteBufUtil.getBytes(encoded));

            StartGamePacket decoded = new StartGamePacket();
            V1002_SERIALIZER.readNetworkPermissionsTailForTest(fixture, V1002_CODEC_HELPER, decoded);
            assertTrue(decoded.getNetworkPermissions().isServerAuthSounds());
            assertFalse(decoded.isLoggingChat());
            assertFalse(fixture.isReadable());
        } finally {
            encoded.release();
            fixture.release();
        }
    }

    @SuppressWarnings("deprecation") // authoritativeMovementMode is deprecated but still written by the v898 serializer chain
    private static void populateRequiredFields(StartGamePacket packet) {
        packet.setPlayerGameType(GameType.SURVIVAL);
        packet.setPlayerPosition(Vector3f.ZERO);
        packet.setRotation(Vector2f.ZERO);
        packet.setLevelGameType(GameType.SURVIVAL);
        packet.setDefaultSpawn(Vector3i.ZERO);
        packet.setXblBroadcastMode(GamePublishSetting.NO_MULTI_PLAY);
        packet.setPlatformBroadcastMode(GamePublishSetting.NO_MULTI_PLAY);
        packet.setDefaultPlayerPermission(PlayerPermission.MEMBER);
        packet.setLevelId("test-level-id");
        packet.setLevelName("Test Level");
        packet.setPremiumWorldTemplateId("");
        packet.setMultiplayerCorrelationId("");
        packet.setVanillaVersion("1.21.130");
        packet.setBlockPalette(new NbtList<>(NbtType.COMPOUND, Collections.emptyList()));
        packet.setForceExperimentalGameplay(OptionalBoolean.empty());
        packet.setChatRestrictionLevel(ChatRestrictionLevel.NONE);
        packet.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER_WITH_REWIND);
        packet.setSpawnBiomeType(SpawnBiomeType.DEFAULT);
        packet.setCustomBiomeName("");
        packet.setEducationProductionId("");
        packet.setServerEngine("");
        packet.setPlayerPropertyData(NbtMap.EMPTY);
        packet.setWorldTemplateId(new UUID(0, 0));
        packet.setServerId("");
        packet.setWorldId("");
        packet.setScenarioId("");
        packet.setOwnerId("");
    }

    private static final class TestStartGameSerializer_v944 extends StartGameSerializer_v944 {
        void writeLevelSettingsForTest(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
            super.writeLevelSettings(buffer, helper, packet);
        }
    }

    private static final class TestEducationStartGameSerializer_v1002 extends EducationStartGameSerializer_v1002 {
        void writeLevelSettingsForTest(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
            super.writeLevelSettings(buffer, helper, packet);
        }

        void readLevelSettingsForTest(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
            super.readLevelSettings(buffer, helper, packet);
        }

        void writeNetworkPermissionsTailForTest(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
            super.writeBeforeNetworkPermissions(buffer, helper, packet);
        }

        void readNetworkPermissionsTailForTest(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
            super.readBeforeNetworkPermissions(buffer, helper, packet);
        }
    }
}
