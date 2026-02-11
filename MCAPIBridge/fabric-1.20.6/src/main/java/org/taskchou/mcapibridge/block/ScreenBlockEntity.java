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

public class ScreenBlockEntity extends BlockEntity {
    public int screenId = 0;
    public int width = 1;
    public int height = 1;
    public int gridX = 0;
    public int gridY = 0;

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(Mcapibridge.SCREEN_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("ScreenId", screenId);
        nbt.putInt("W", width);
        nbt.putInt("H", height);
        nbt.putInt("GX", gridX);
        nbt.putInt("GY", gridY);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        screenId = nbt.getInt("ScreenId");
        width = Math.max(1, nbt.getInt("W"));
        height = Math.max(1, nbt.getInt("H"));
        gridX = nbt.getInt("GX");
        gridY = nbt.getInt("GY");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

}