package org.taskchou.mcapibridge.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class IOPayloads {

    public record OpenConfig(BlockPos pos, int currentId, boolean currentMode) implements CustomPayload {
        public static final Id<OpenConfig> ID = new Id<>(Identifier.of("mcapibridge", "open_io_config"));
        public static final PacketCodec<RegistryByteBuf, OpenConfig> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenConfig::pos,
                PacketCodecs.INTEGER, OpenConfig::currentId,
                PacketCodecs.BOOLEAN, OpenConfig::currentMode,
                OpenConfig::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetConfig(BlockPos pos, int newId, boolean newMode) implements CustomPayload {
        public static final Id<SetConfig> ID = new Id<>(Identifier.of("mcapibridge", "set_io_config"));
        public static final PacketCodec<RegistryByteBuf, SetConfig> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, SetConfig::pos,
                PacketCodecs.INTEGER, SetConfig::newId,
                PacketCodecs.BOOLEAN, SetConfig::newMode,
                SetConfig::new
        );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}