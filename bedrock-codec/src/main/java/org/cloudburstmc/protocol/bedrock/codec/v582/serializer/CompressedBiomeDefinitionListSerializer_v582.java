package org.cloudburstmc.protocol.bedrock.codec.v582.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.packet.CompressedBiomeDefinitionListPacket;
import org.cloudburstmc.protocol.common.util.VarInts;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompressedBiomeDefinitionListSerializer_v582 implements BedrockPacketSerializer<CompressedBiomeDefinitionListPacket> {
    public static final CompressedBiomeDefinitionListSerializer_v582 INSTANCE = new CompressedBiomeDefinitionListSerializer_v582();
    protected static final byte[] COMPRESSED_INDICATOR = new byte[]{ 0x43, 0x4f, 0x4d, 0x50, 0x52, 0x45, 0x53, 0x53, 0x45, 0x44 }; // COMPRESSED

    @Override
    public void serialize(ByteBuf buffer, BedrockCodecHelper helper, CompressedBiomeDefinitionListPacket packet) {
        // writeBytes() copies without transferring ownership, so the temporary
        // buffer must be released here.
        ByteBuf compressed = buffer.alloc().ioBuffer();
        try {
            this.writeCompressed(packet.getDefinitions(), compressed, helper);

            VarInts.writeUnsignedInt(buffer, compressed.readableBytes());
            buffer.writeBytes(compressed);
        } finally {
            compressed.release();
        }
    }

    @Override
    public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, CompressedBiomeDefinitionListPacket packet) {
        int length = VarInts.readUnsignedInt(buffer);
        // A slice needs no separate release, unlike the readBytes() copy it replaces.
        packet.setDefinitions(this.readCompressed(buffer.readSlice(length), helper, COMPRESSED_INDICATOR.length));
    }

    protected NbtMap readCompressed(ByteBuf buffer, BedrockCodecHelper helper, int length) {
        buffer.skipBytes(length);

        ByteBuf[] dictionary = new ByteBuf[buffer.readUnsignedShortLE()];
        ByteBuf decompressed = buffer.alloc().ioBuffer();
        try {
            for (int index = 0; index < dictionary.length; index++) {
                dictionary[index] = buffer.readBytes(buffer.readUnsignedByte());
            }

            while (buffer.isReadable()) {
                int key = buffer.readUnsignedByte();
                if (key != 0xff) {
                    decompressed.writeByte(key);
                    continue;
                }

                int index = buffer.readUnsignedShortLE();
                if (index >= 0 && index < dictionary.length) {
                    decompressed.writeBytes(dictionary[index].slice());
                } else {
                    decompressed.writeByte(key);
                }
            }
            return helper.readTag(decompressed, NbtMap.class);
        } finally {
            decompressed.release();
            for (ByteBuf buf : dictionary) {
                if (buf != null) {
                    buf.release();
                }
            }
        }
    }

    private void writeCompressed(NbtMap nbtMap, ByteBuf buffer, BedrockCodecHelper helper) {
        buffer.writeBytes(COMPRESSED_INDICATOR);
        // Ignore the dictionary compression for now
        buffer.writeShortLE(0);

        ByteBuf serialized = buffer.alloc().ioBuffer();
        try {
            helper.writeTag(serialized, nbtMap);

            while (serialized.isReadable()) {
                int key = serialized.readUnsignedByte();
                buffer.writeByte(key);
                if (key == 0xff) {
                    buffer.writeShortLE(1);
                }
            }
        } finally {
            serialized.release();
        }
    }
}
