package org.taskchou.mcapibridge.block;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.taskchou.mcapibridge.IODataState;
import org.taskchou.mcapibridge.Mcapibridge;

public class IOBlock extends BlockWithEntity {

    public static final BooleanProperty OUTPUT_MODE = BooleanProperty.of("output_mode");

    public IOBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(OUTPUT_MODE, false));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OUTPUT_MODE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IOBlockEntity(pos, state);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) { return true; }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
            if (!state.get(OUTPUT_MODE)) {
                return be.power;
            }
        }
        return 0;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;

        if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
            if (state.get(OUTPUT_MODE)) {
                int received = world.getReceivedRedstonePower(pos);
                if (received != be.power) {
                    be.power = received;
                    be.markDirty();

                    String event = String.format("io.update,%d,%d", be.channelId, received);
                    Mcapibridge.broadcastEvent(event);
                }
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeInt(be.channelId);
            buf.writeBoolean(state.get(OUTPUT_MODE));

            ServerPlayNetworking.send((ServerPlayerEntity) player, Mcapibridge.IO_OPEN_CONFIG_ID, buf);
        }
        return ActionResult.SUCCESS;
    }

    public void configure(World world, BlockPos pos, int id, boolean mode) {
        if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
            IODataState data = IODataState.getServerState(world.getServer());
            data.removeBlock(be.channelId, GlobalPos.create(world.getRegistryKey(), pos));

            be.channelId = id;
            be.isOutputMode = mode;
            be.markDirty();

            BlockState newState = world.getBlockState(pos).with(OUTPUT_MODE, mode);
            world.setBlockState(pos, newState);

            data.addBlock(id, GlobalPos.create(world.getRegistryKey(), pos));

            world.updateNeighborsAlways(pos, this);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world instanceof ServerWorld sw) {
            if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
                IODataState.getServerState(sw.getServer()).removeBlock(be.channelId, GlobalPos.create(world.getRegistryKey(), pos));
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}