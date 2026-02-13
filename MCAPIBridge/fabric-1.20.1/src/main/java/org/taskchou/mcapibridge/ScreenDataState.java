package org.taskchou.mcapibridge;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.*;

public class ScreenDataState extends PersistentState {

    public static class ScreenLocation {
        public double x, y, z;
        public String dimension;

        public ScreenLocation(double x, double y, double z, String dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }

    public final Map<Integer, List<ScreenLocation>> screens = new HashMap<>();

    public static ScreenDataState getServerState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(ScreenDataState::fromNbt, ScreenDataState::new, "mcapibridge_screens");
    }

    public void addScreen(int id, Vec3d pos, String dimension) {
        List<ScreenLocation> list = screens.computeIfAbsent(id, k -> new ArrayList<>());

        for (ScreenLocation existing : list) {
            if (!existing.dimension.equals(dimension)) continue;

            double distSq = (existing.x - pos.x)*(existing.x - pos.x) +
                    (existing.y - pos.y)*(existing.y - pos.y) +
                    (existing.z - pos.z)*(existing.z - pos.z);

            if (distSq < 1.0) return;
        }

        list.add(new ScreenLocation(pos.x, pos.y, pos.z, dimension));
        markDirty();
    }

    public List<ScreenLocation> getScreens(int id) {
        return screens.getOrDefault(id, Collections.emptyList());
    }

    public void removeScreen(int id) {
        if (screens.containsKey(id)) {
            screens.remove(id);
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList rootList = new NbtList();
        screens.forEach((id, locations) -> {
            NbtCompound idTag = new NbtCompound();
            idTag.putInt("id", id);
            NbtList posList = new NbtList();
            for (ScreenLocation loc : locations) {
                NbtCompound p = new NbtCompound();
                p.putDouble("x", loc.x);
                p.putDouble("y", loc.y);
                p.putDouble("z", loc.z);
                p.putString("dim", loc.dimension);
                posList.add(p);
            }
            idTag.put("positions", posList);
            rootList.add(idTag);
        });
        nbt.put("screens", rootList);
        return nbt;
    }

    public static ScreenDataState fromNbt(NbtCompound nbt) {
        ScreenDataState state = new ScreenDataState();
        NbtList rootList = nbt.getList("screens", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < rootList.size(); i++) {
            NbtCompound idTag = rootList.getCompound(i);
            int id = idTag.getInt("id");
            NbtList posList = idTag.getList("positions", NbtElement.COMPOUND_TYPE);
            List<ScreenLocation> locations = new ArrayList<>();
            for (int j = 0; j < posList.size(); j++) {
                NbtCompound p = posList.getCompound(j);
                String dim = p.contains("dim") ? p.getString("dim") : "minecraft:overworld";
                locations.add(new ScreenLocation(p.getDouble("x"), p.getDouble("y"), p.getDouble("z"), dim));
            }
            state.screens.put(id, locations);
        }
        return state;
    }

    public ScreenDataState() {}
}