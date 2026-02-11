package org.taskchou.mcapibridge.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ScreenPayloads {

    public record OpenConfig(BlockPos pos, int currentId) implements CustomPayload {
        public static final Id<OpenConfig> ID = new Id<>(new Identifier("mcapibridge", "open_config"));
        public static final PacketCodec<RegistryByteBuf, OpenConfig> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenConfig::pos,
                PacketCodecs.INTEGER, OpenConfig::currentId,
                OpenConfig::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetId(BlockPos pos, int newId) implements CustomPayload {
        public static final Id<SetId> ID = new Id<>(new Identifier("mcapibridge", "set_id"));
        public static final PacketCodec<RegistryByteBuf, SetId> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, SetId::pos,
                PacketCodecs.INTEGER, SetId::newId,
                SetId::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}