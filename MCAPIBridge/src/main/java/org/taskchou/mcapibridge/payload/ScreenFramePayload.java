package org.taskchou.mcapibridge.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ScreenFramePayload(int screenId, byte[] imageData) implements CustomPayload {
    public static final CustomPayload.Id<ScreenFramePayload> ID =
            new CustomPayload.Id<>(new Identifier("mcapibridge", "screen_frame"));

    public static final PacketCodec<RegistryByteBuf, ScreenFramePayload> CODEC = new PacketCodec<>() {
        @Override
        public ScreenFramePayload decode(RegistryByteBuf buf) {
            int id = buf.readInt();
            int len = buf.readInt();
            byte[] data = new byte[len];
            buf.readBytes(data);
            return new ScreenFramePayload(id, data);
        }
        @Override
        public void encode(RegistryByteBuf buf, ScreenFramePayload payload) {
            buf.writeInt(payload.screenId);
            buf.writeInt(payload.imageData.length);
            buf.writeBytes(payload.imageData);
        }
    };
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}