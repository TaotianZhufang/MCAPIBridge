package org.taskchou.mcapibridge.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.taskchou.mcapibridge.Mcapibridge;
import org.taskchou.mcapibridge.ScreenDataState;

import java.util.*;

public class ScreenBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final MapCodec<ScreenBlock> CODEC = createCodec(ScreenBlock::new);

    public ScreenBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ScreenBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        int currentId = 1;
        if (world.getBlockEntity(pos) instanceof ScreenBlockEntity be) {
            currentId = be.screenId;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(currentId);

        ServerPlayNetworking.send((ServerPlayerEntity) player, Mcapibridge.SCREEN_OPEN_CONFIG_ID, buf);

        return ActionResult.SUCCESS;
    }

    public void configureScreen(World world, BlockPos startPos, PlayerEntity player, Direction facing, int screenId) {
        Set<BlockPos> connected = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        connected.add(startPos);
        queue.add(startPos);


        while (!queue.isEmpty() && connected.size() < 2000) {
            BlockPos curr = queue.poll();

            Direction[] searchDirs = {
                    Direction.UP,
                    Direction.DOWN,
                    facing.rotateYClockwise(),
                    facing.rotateYCounterclockwise()
            };

            for (Direction dir : searchDirs) {
                BlockPos n = curr.offset(dir);
                if (!connected.contains(n) &&
                        world.getBlockState(n).isOf(this) &&
                        world.getBlockState(n).get(FACING) == facing) {
                    connected.add(n);
                    queue.add(n);
                }
            }
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos p : connected) {
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
        }

        int w, h = maxY - minY + 1;
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            w = maxX - minX + 1;
        } else {
            w = maxZ - minZ + 1;
        }

        if (connected.size() != w * h) {
            player.sendMessage(Text.translatable("mcapibridge.msg.config.error.shape", connected.size(), w * h), true);
            return;
        }

        for (BlockPos p : connected) {
            if (world.getBlockEntity(p) instanceof ScreenBlockEntity be) {
                be.screenId = screenId;
                be.width = w;
                be.height = h;
                be.gridY = p.getY() - minY;

                switch (facing) {
                    case NORTH -> be.gridX = maxX - p.getX();
                    case SOUTH -> be.gridX = p.getX() - minX;
                    case WEST ->  be.gridX = p.getZ() - minZ;
                    case EAST ->  be.gridX = maxZ - p.getZ();
                }

                be.markDirty();
                ((ServerWorld) world).getChunkManager().markForUpdate(p);
            }
        }

        double centerX = (minX + maxX) / 2.0 + 0.5;
        double centerY = (minY + maxY) / 2.0 + 0.5;
        double centerZ = (minZ + maxZ) / 2.0 + 0.5;

        switch (facing) {
            case NORTH -> centerZ -= 0.5;
            case SOUTH -> centerZ += 0.5;
            case WEST  -> centerX -= 0.5;
            case EAST  -> centerX += 0.5;
        }

        if (!world.isClient && world.getServer() != null) {
            String dimId = world.getRegistryKey().getValue().toString();
            ScreenDataState state = ScreenDataState.getServerState(world.getServer());
            state.addScreen(screenId, new Vec3d(centerX, centerY, centerZ), dimId);
        }
        player.sendMessage(Text.translatable("mcapibridge.msg.config.success", w, h, screenId), true);

    }
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.isOf(newState.getBlock())) {
            return;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ScreenBlockEntity screenBe) {
            int screenId = screenBe.screenId;

            if (!world.isClient && world.getServer() != null) {
                ScreenDataState data = ScreenDataState.getServerState(world.getServer());

                List<ScreenDataState.ScreenLocation> locs = data.getScreens(screenId);

                Iterator<ScreenDataState.ScreenLocation> it = locs.iterator();
                while (it.hasNext()) {
                    ScreenDataState.ScreenLocation loc = it.next();

                    double distSq = (loc.x - (pos.getX() + 0.5)) * (loc.x - (pos.getX() + 0.5)) +
                            (loc.y - (pos.getY() + 0.5)) * (loc.y - (pos.getY() + 0.5)) +
                            (loc.z - (pos.getZ() + 0.5)) * (loc.z - (pos.getZ() + 0.5));

                    if (loc.dimension.equals(world.getRegistryKey().getValue().toString()) && distSq < 4.0) {
                        it.remove();
                        data.markDirty();
                        System.out.println("Screen " + screenId + " removed at " + pos);
                    }
                }

                if (locs.isEmpty()) {
                    data.removeScreen(screenId);
                }
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }
}