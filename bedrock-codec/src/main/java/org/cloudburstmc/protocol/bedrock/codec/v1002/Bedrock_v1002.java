package org.cloudburstmc.protocol.bedrock.codec.v1002;

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v1002.serializer.EducationStartGameSerializer_v1002;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

/**
 * Protocol 1002 is used only by Minecraft Education Edition 26.30; no retail
 * client ships this number, so only an Education codec exists. It is the
 * v1001 (retail 26.33) codec with the Education StartGamePacket wire format,
 * see {@link EducationStartGameSerializer_v1002}.
 */
public class Bedrock_v1002 extends Bedrock_v1001 {

    public static final BedrockCodec EDUCATION_CODEC = Bedrock_v1001.CODEC.toBuilder()
            .protocolVersion(1002)
            .updateSerializer(StartGamePacket.class, EducationStartGameSerializer_v1002.INSTANCE)
            .build();
}
