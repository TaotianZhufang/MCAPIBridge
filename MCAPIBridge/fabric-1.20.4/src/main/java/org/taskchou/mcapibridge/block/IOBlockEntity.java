package org.taskchou.mcapibridge.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.taskchou.mcapibridge.Mcapibridge;

public class IOBlockEntity extends BlockEntity {
    public int channelId = 1;
    public boolean isOutputMode = false;
    public int power = 0;

    public IOBlockEntity(BlockPos pos, BlockState state) {
        super(Mcapibridge.IO_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Channel", channelId);
        nbt.putBoolean("Mode", isOutputMode);
        nbt.putInt("Power", power);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        channelId = nbt.getInt("Channel");
        isOutputMode = nbt.getBoolean("Mode");
        power = nbt.getInt("Power");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}