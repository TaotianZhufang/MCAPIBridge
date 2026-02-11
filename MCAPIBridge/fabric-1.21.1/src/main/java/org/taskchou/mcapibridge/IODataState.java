package org.taskchou.mcapibridge;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public class IODataState extends PersistentState {

    public final Map<Integer, List<GlobalPos>> blocks = new HashMap<>();

    public static final Type<IODataState> TYPE = new Type<>(
            IODataState::new,
            IODataState::fromNbt,
            null
    );

    public static IODataState getServerState(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(TYPE, "mcapibridge_io");
    }

    public void addBlock(int id, GlobalPos pos) {
        List<GlobalPos> list = blocks.computeIfAbsent(id, k -> new ArrayList<>());
        if (!list.contains(pos)) {
            list.add(pos);
            markDirty();
        }
    }

    public void removeBlock(int id, GlobalPos pos) {
        List<GlobalPos> list = blocks.get(id);
        if (list != null) {
            list.remove(pos);
            if (list.isEmpty()) blocks.remove(id);
            markDirty();
        }
    }

    public List<GlobalPos> getBlocks(int id) {
        return blocks.getOrDefault(id, Collections.emptyList());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList rootList = new NbtList();
        blocks.forEach((id, positions) -> {
            NbtCompound idTag = new NbtCompound();
            idTag.putInt("id", id);
            NbtList posList = new NbtList();
            for (GlobalPos gp : positions) {
                NbtCompound p = new NbtCompound();
                p.putInt("x", gp.pos().getX());
                p.putInt("y", gp.pos().getY());
                p.putInt("z", gp.pos().getZ());
                p.putString("dim", gp.dimension().getValue().toString());
                posList.add(p);
            }
            idTag.put("positions", posList);
            rootList.add(idTag);
        });
        nbt.put("io_blocks", rootList);
        return nbt;
    }

    public static IODataState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        IODataState state = new IODataState();
        NbtList rootList = nbt.getList("io_blocks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < rootList.size(); i++) {
            NbtCompound idTag = rootList.getCompound(i);
            int id = idTag.getInt("id");
            NbtList posList = idTag.getList("positions", NbtElement.COMPOUND_TYPE);
            List<GlobalPos> positions = new ArrayList<>();
            for (int j = 0; j < posList.size(); j++) {
                NbtCompound p = posList.getCompound(j);
                BlockPos pos = new BlockPos(p.getInt("x"), p.getInt("y"), p.getInt("z"));
                String dim = p.getString("dim");
                RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dim));
                positions.add(GlobalPos.create(dimKey, pos));
            }
            state.blocks.put(id, positions);
        }
        return state;
    }

    public IODataState() {}
}