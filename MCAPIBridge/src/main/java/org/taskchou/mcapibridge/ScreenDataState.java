package org.taskchou.mcapibridge;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public class ScreenDataState extends PersistentState {

    public final Map<Integer, List<Vec3d>> screens = new HashMap<>();

    public static final Type<ScreenDataState> TYPE = new Type<>(
            ScreenDataState::new,
            ScreenDataState::fromNbt,
            null
    );

    public static ScreenDataState getServerState(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(TYPE, "mcapibridge_screens");
    }

    public void addScreen(int id, Vec3d pos) {
        List<Vec3d> list = screens.computeIfAbsent(id, k -> new ArrayList<>());

        for (Vec3d existing : list) {
            if (existing.squaredDistanceTo(pos) < 1.0) return;
        }

        list.add(pos);
        markDirty();
    }

    public List<Vec3d> getScreens(int id) {
        return screens.getOrDefault(id, Collections.emptyList());
    }

    public void removeScreen(int id) {
        if (screens.containsKey(id)) {
            screens.remove(id);
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList rootList = new NbtList();

        screens.forEach((id, positions) -> {
            NbtCompound idTag = new NbtCompound();
            idTag.putInt("id", id);

            NbtList posList = new NbtList();
            for (Vec3d pos : positions) {
                NbtCompound p = new NbtCompound();
                p.putDouble("x", pos.x);
                p.putDouble("y", pos.y);
                p.putDouble("z", pos.z);
                posList.add(p);
            }
            idTag.put("positions", posList);
            rootList.add(idTag);
        });

        nbt.put("screens", rootList);
        return nbt;
    }

    public static ScreenDataState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ScreenDataState state = new ScreenDataState();
        NbtList rootList = nbt.getList("screens", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < rootList.size(); i++) {
            NbtCompound idTag = rootList.getCompound(i);
            int id = idTag.getInt("id");

            NbtList posList = idTag.getList("positions", NbtElement.COMPOUND_TYPE);
            List<Vec3d> positions = new ArrayList<>();

            for (int j = 0; j < posList.size(); j++) {
                NbtCompound p = posList.getCompound(j);
                positions.add(new Vec3d(p.getDouble("x"), p.getDouble("y"), p.getDouble("z")));
            }
            state.screens.put(id, positions);
        }
        return state;
    }

    public ScreenDataState() {}
}