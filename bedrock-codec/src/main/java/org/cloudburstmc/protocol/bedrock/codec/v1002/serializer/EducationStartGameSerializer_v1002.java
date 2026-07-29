package org.cloudburstmc.protocol.bedrock.codec.v1002.serializer;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v944.serializer.StartGameSerializer_v944;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.common.util.VarInts;

/**
 * Education Edition variant of the StartGamePacket serializer for protocol 1002
 * (Education 26.30).
 * <p>
 * Education 26.30 forked from the retail 26.30 snapshot, which predates the
 * {@code loggingChat} field v1001 (retail 26.33) writes after network
 * permissions; extending the v944 serializer keeps that field out. The client
 * additionally expects three Education string fields inside LevelSettings,
 * after {@code disablingPlayerInteractions} and before the editor connection
 * tail, which is replicated here from the v1001 serializer. The string order
 * is reversed compared to the v898 Education serializer.
 */
public class EducationStartGameSerializer_v1002 extends StartGameSerializer_v944 {

    public static final EducationStartGameSerializer_v1002 INSTANCE = new EducationStartGameSerializer_v1002();

    @Override
    protected void writeLevelSettings(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
        super.writeLevelSettings(buffer, helper, packet);
        helper.writeString(buffer, packet.getEducationCreatorId());
        helper.writeString(buffer, packet.getEducationCreatorWorldId());
        helper.writeString(buffer, packet.getEducationReferrerId());

        VarInts.writeInt(buffer, packet.getServerEditorConnectionPolicy());
        buffer.writeBoolean(packet.isAllowAnonymousBlockDropsInEditorWorlds());
    }

    @Override
    protected void readLevelSettings(ByteBuf buffer, BedrockCodecHelper helper, StartGamePacket packet) {
        super.readLevelSettings(buffer, helper, packet);
        packet.setEducationCreatorId(helper.readString(buffer));
        packet.setEducationCreatorWorldId(helper.readString(buffer));
        packet.setEducationReferrerId(helper.readString(buffer));

        packet.setServerEditorConnectionPolicy(VarInts.readInt(buffer));
        packet.setAllowAnonymousBlockDropsInEditorWorlds(buffer.readBoolean());
    }
}
