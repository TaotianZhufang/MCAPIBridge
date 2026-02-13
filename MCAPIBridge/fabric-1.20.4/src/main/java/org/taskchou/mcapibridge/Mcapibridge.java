package org.taskchou.mcapibridge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.map.MapState;
import org.taskchou.mcapibridge.block.IOBlock;
import org.taskchou.mcapibridge.block.IOBlockEntity;
import org.taskchou.mcapibridge.block.ScreenBlock;
import org.taskchou.mcapibridge.block.ScreenBlockEntity;
import org.taskchou.mcapibridge.item.IOBlockItem;
import org.taskchou.mcapibridge.item.ScreenBlockItem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

public class Mcapibridge implements ModInitializer {

    public static final Identifier CLICK_PACKET_ID = new Identifier("mcapibridge", "click_event");
    public static final Identifier AUDIO_PACKET_ID = new Identifier("mcapibridge", "audio");
    public static final Identifier SCREEN_FRAME_ID = new Identifier("mcapibridge", "screen_frame");

    public static final Identifier SCREEN_OPEN_CONFIG_ID = new Identifier("mcapibridge", "open_config");
    public static final Identifier SCREEN_SET_ID_ID = new Identifier("mcapibridge", "set_id");

    public static final Identifier IO_OPEN_CONFIG_ID = new Identifier("mcapibridge", "open_io_config");
    public static final Identifier IO_SET_CONFIG_ID = new Identifier("mcapibridge", "set_io_config");


    private static BridgeSocketServer serverThread;
    public static final List<BridgeClientHandler> activeClients = new CopyOnWriteArrayList<>();
    public static final java.util.Map<Integer, byte[]> customMapData = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, byte[]> pendingAudioData = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, AudioBuffer> audioBuffers = new java.util.concurrent.ConcurrentHashMap<>();
    public static MinecraftServer serverInstance;
    public static final List<BridgeClientHandler> activeHandlers = new CopyOnWriteArrayList<>();
    public static final Map<String, Long> lastSentDataTime = new ConcurrentHashMap<>();
    public static final Block IO_BLOCK = new IOBlock(FabricBlockSettings.create().strength(1.0f));
    public static BlockEntityType<IOBlockEntity> IO_BLOCK_ENTITY;
    private static class AudioBuffer {
        String target;
        int sampleRate;
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
    }
    public static class ActiveSound {
        public String id;
        public String sourceDataId;
        public int screenId;
        public volatile long startTime;
        public float duration;
        public float volume;
        public boolean loop;
        public boolean isGlobal;
        public volatile boolean isPaused = false;
        public volatile long pauseTime = 0;


        public float x, y, z;
        public String dimension;
        public float rolloff;

        public ActiveSound(String id, String sourceDataId, int screenId, long startTime, float duration, float volume, boolean loop) {
            this.id = id;
            this.sourceDataId = sourceDataId;
            this.screenId = screenId;
            this.startTime = startTime;
            this.duration = duration;
            this.volume = volume;
            this.loop = loop;
            this.rolloff = 1.0f;
            this.dimension = "";
            this.isGlobal=false;
        }

        public ActiveSound(String id, String sourceDataId, float x, float y, float z, String dimension, long startTime, float duration, float volume, float rolloff, boolean loop) {
            this.id = id;
            this.sourceDataId = sourceDataId;
            this.screenId = -1;
            this.startTime = startTime;
            this.duration = duration;
            this.volume = volume;
            this.loop = loop;
            this.x = x; this.y = y; this.z = z;
            this.dimension = dimension;
            this.rolloff = rolloff;
            this.isGlobal=false;
        }

        public ActiveSound(String id, String sourceDataId, float volume, boolean loop, float duration) {
            this.id = id;
            this.sourceDataId = sourceDataId;
            this.screenId = -2;
            this.volume = volume;
            this.loop = loop;
            this.duration = duration;
            this.startTime = System.currentTimeMillis();
            this.isGlobal = true;
            this.dimension = "";
            this.x=0; this.y=0; this.z=0; this.rolloff=0;
        }
    }

    public static final Map<String, ActiveSound> activeSounds = new ConcurrentHashMap<>();
    public static final Map<String, String> cloneMap = new ConcurrentHashMap<>();
    public static final Map<String, byte[]> audioDataCache = new ConcurrentHashMap<>();
    public static final Map<String, Integer> audioRateCache = new ConcurrentHashMap<>();
    public static final Map<Integer, Vec3d> SCREEN_LOCATIONS = new ConcurrentHashMap<>();
    public static final Block SCREEN_BLOCK = new ScreenBlock(FabricBlockSettings.create().strength(1.0f).luminance(15));
    public static BlockEntityType<ScreenBlockEntity> SCREEN_BLOCK_ENTITY;
    public static final Map<java.util.UUID, Set<String>> playerListeningState = new ConcurrentHashMap<>();
    private static final Map<String, Object> dataSendLocks = new ConcurrentHashMap<>();

    private static net.minecraft.network.packet.Packet<?> createAudioPacket(
            String action, String id, int sampleRate, byte[] data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(action);
        buf.writeString(id);
        buf.writeInt(sampleRate);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        return ServerPlayNetworking.createS2CPacket(AUDIO_PACKET_ID, buf);
    }

    private void updateAudioState() {
        if (activeSounds.isEmpty()) return;

        ScreenDataState screenState = ScreenDataState.getServerState(serverInstance);
        long now = System.currentTimeMillis();

        activeSounds.entrySet().removeIf(entry -> {
            ActiveSound s = entry.getValue();
            float offset = (now - s.startTime) / 1000.0f;
            return !s.loop && s.duration > 0 && offset >= s.duration;
        });

        for (ServerPlayerEntity player : serverInstance.getPlayerManager().getPlayerList()) {
            java.util.UUID uuid = player.getUuid();
            Set<String> listening = playerListeningState.computeIfAbsent(uuid, k -> new HashSet<>());
            String pDim = player.getWorld().getRegistryKey().getValue().toString();
            Vec3d pPos = player.getPos();

            for (ActiveSound sound : activeSounds.values()) {
                if (sound.isPaused) continue;

                boolean shouldSend = false;
                ScreenDataState.ScreenLocation bestLoc = null;

                if (sound.isGlobal) {
                    shouldSend = true;
                } else if (sound.screenId > 0) {
                    List<ScreenDataState.ScreenLocation> locs = screenState.getScreens(sound.screenId);
                    double minDst = Double.MAX_VALUE;
                    for (var loc : locs) {
                        if (loc.dimension.equals(pDim)) {
                            double dst = (loc.x - pPos.x)*(loc.x - pPos.x) +
                                    (loc.y - pPos.y)*(loc.y - pPos.y) +
                                    (loc.z - pPos.z)*(loc.z - pPos.z);
                            if (dst < 100*100 && dst < minDst) {
                                minDst = dst;
                                bestLoc = loc;
                            }
                        }
                    }
                    shouldSend = bestLoc != null;
                } else {
                    shouldSend = true;
                    bestLoc = new ScreenDataState.ScreenLocation(
                            sound.x, sound.y, sound.z, sound.dimension);
                }

                if (shouldSend) {
                    if (!listening.contains(sound.id)) {
                        float offset = (now - sound.startTime) / 1000.0f;
                        if (sound.loop && sound.duration > 0) offset %= sound.duration;

                        if (sound.isGlobal) {
                            sendPlayPacket2D(player, sound, offset);
                        } else {
                            sendPlayPacket(player, sound, bestLoc, offset);
                        }
                        listening.add(sound.id);
                    }
                } else {
                    if (listening.contains(sound.id)) {
                        sendStopPacket(player, sound.id);
                        listening.remove(sound.id);
                    }
                }
            }
            listening.retainAll(activeSounds.keySet());
        }
    }

    private void sendPlayPacket2D(ServerPlayerEntity player, ActiveSound sound, float offset) {
        new Thread(() -> {
            try {
                String dataId = sound.sourceDataId;
                Object lock = dataSendLocks.computeIfAbsent(dataId, k -> new Object());

                synchronized (lock) {
                    String key = player.getUuid() + "_" + dataId;
                    long lastTime = lastSentDataTime.getOrDefault(key, 0L);
                    long now = System.currentTimeMillis();

                    if (now - lastTime > 10000) {
                        byte[] rawData = audioDataCache.get(dataId);
                        Integer rate = audioRateCache.get(dataId);

                        if (rawData != null && rate != null) {
                            int chunkSize = 32000;
                            for (int i = 0; i < rawData.length; i += chunkSize) {
                                int end = Math.min(i + chunkSize, rawData.length);
                                byte[] chunk = java.util.Arrays.copyOfRange(rawData, i, end);
                                String action = (i == 0) ? "loadStart" : (end == rawData.length ? "loadEnd" : "loadContinue");
                                if (rawData.length <= chunkSize) action = "load";

                                player.networkHandler.sendPacket(createAudioPacket(action, dataId, rate, chunk));
                            }
                            Thread.sleep(100);
                            lastSentDataTime.put(key, System.currentTimeMillis());
                        }
                    }
                }

                if (!sound.id.equals(sound.sourceDataId)) {
                    byte[] cloneData = sound.sourceDataId.getBytes(StandardCharsets.UTF_8);
                    player.networkHandler.sendPacket(createAudioPacket("clone", sound.id, 0, cloneData));
                    Thread.sleep(10);
                }

                float finalOffset = (System.currentTimeMillis() - sound.startTime) / 1000.0f;

                if (sound.loop && sound.duration > 0) {
                    finalOffset %= sound.duration;
                } else if (finalOffset >= sound.duration) {
                    return;
                }

                byte[] data = new byte[9];
                packFloat(data, 0, sound.volume);
                data[4] = (byte)(sound.loop ? 1 : 0);
                packFloat(data, 5, finalOffset);

                player.networkHandler.sendPacket(createAudioPacket("play", sound.id, 0, data));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendPlayPacket(ServerPlayerEntity player, ActiveSound sound, ScreenDataState.ScreenLocation loc, float offset) {
        new Thread(() -> {
            try {
                String dataId = sound.sourceDataId;
                Object lock = dataSendLocks.computeIfAbsent(dataId, k -> new Object());

                synchronized (lock) {
                    String key = player.getUuid() + "_" + dataId;
                    long lastTime = lastSentDataTime.getOrDefault(key, 0L);
                    long now = System.currentTimeMillis();

                    if (now - lastTime > 10000) {
                        byte[] rawData = audioDataCache.get(dataId);
                        Integer rate = audioRateCache.get(dataId);

                        if (rawData != null && rate != null) {
                            int chunkSize = 32000;
                            for (int i = 0; i < rawData.length; i += chunkSize) {
                                int end = Math.min(i + chunkSize, rawData.length);
                                byte[] chunk = java.util.Arrays.copyOfRange(rawData, i, end);
                                String action = (i == 0) ? "loadStart" : (end == rawData.length ? "loadEnd" : "loadContinue");
                                if (rawData.length <= chunkSize) action = "load";

                                player.networkHandler.sendPacket(createAudioPacket(action, dataId, rate, chunk));
                            }
                            Thread.sleep(100);
                            lastSentDataTime.put(key, System.currentTimeMillis());
                        }
                    }
                }

                if (!sound.id.equals(sound.sourceDataId)) {
                    byte[] cloneData = sound.sourceDataId.getBytes(StandardCharsets.UTF_8);
                    player.networkHandler.sendPacket(createAudioPacket("clone", sound.id, 0, cloneData));
                    Thread.sleep(10);
                }

                float finalOffset = (System.currentTimeMillis() - sound.startTime) / 1000.0f;

                if (sound.loop && sound.duration > 0) {
                    finalOffset %= sound.duration;
                } else if (finalOffset >= sound.duration) {
                    return;
                }

                byte[] dimBytes = loc.dimension.getBytes(StandardCharsets.UTF_8);
                int dimLen = dimBytes.length;
                byte[] data = new byte[25 + 4 + dimLen];

                packFloat(data, 0, sound.volume);
                packFloat(data, 4, sound.rolloff);
                packFloat(data, 8, (float)loc.x);
                packFloat(data, 12, (float)loc.y);
                packFloat(data, 16, (float)loc.z);
                packFloat(data, 20, finalOffset);
                data[24] = (byte)(sound.loop ? 1 : 0);

                data[25] = (byte)(dimLen >> 24);
                data[26] = (byte)(dimLen >> 16);
                data[27] = (byte)(dimLen >> 8);
                data[28] = (byte)(dimLen);
                System.arraycopy(dimBytes, 0, data, 29, dimLen);

                player.networkHandler.sendPacket(createAudioPacket("play3d", sound.id, 0, data));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendStopPacket(ServerPlayerEntity player, String id) {
        player.networkHandler.sendPacket(createAudioPacket("stop", id, 0, new byte[0]));
    }

    private void ensureDataAndPlay(ServerPlayerEntity player, ActiveSound sound,
                                   ScreenDataState.ScreenLocation loc, float offset) {
        String dataId = sound.sourceDataId;
        String cacheKey = player.getUuid() + "_" + dataId;
        long lastTime = lastSentDataTime.getOrDefault(cacheKey, 0L);
        long now = System.currentTimeMillis();
        boolean needsTransfer = (now - lastTime > 10000);
        boolean needsClone = !sound.id.equals(sound.sourceDataId);

        if (!needsTransfer && !needsClone) {
            sendPlayNow(player, sound, loc, offset);
            return;
        }

        new Thread(() -> {
            try {
                long transferStart = System.currentTimeMillis();

                if (needsTransfer) {
                    byte[] rawData = audioDataCache.get(dataId);
                    Integer rate = audioRateCache.get(dataId);
                    if (rawData != null && rate != null) {
                        int chunkSize = 32000;
                        for (int i = 0; i < rawData.length; i += chunkSize) {
                            int end = Math.min(i + chunkSize, rawData.length);
                            byte[] chunk = java.util.Arrays.copyOfRange(rawData, i, end);
                            String action = (i == 0) ? "loadStart" :
                                    (end == rawData.length ? "loadEnd" : "loadContinue");
                            if (rawData.length <= chunkSize) action = "load";

                            final String a = action;
                            final byte[] c = chunk;
                            final int r = rate;
                            serverInstance.execute(() -> sendDirect(player, a, dataId, r, c));
                            Thread.sleep(2);
                        }
                        Thread.sleep(100);
                        lastSentDataTime.put(cacheKey, System.currentTimeMillis());
                    }
                }

                if (needsClone) {
                    byte[] cloneData = sound.sourceDataId.getBytes(StandardCharsets.UTF_8);
                    serverInstance.execute(() ->
                            sendDirect(player, "clone", sound.id, 0, cloneData));
                    Thread.sleep(50);
                }

                float transferLag = (System.currentTimeMillis() - transferStart) / 1000.0f;
                float finalOffset = offset + transferLag;
                if (sound.loop && sound.duration > 0) finalOffset %= sound.duration;
                else if (finalOffset >= sound.duration) return;

                final float fo = finalOffset;
                serverInstance.execute(() -> sendPlayNow(player, sound, loc, fo));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendPlayNow(ServerPlayerEntity player, ActiveSound sound,
                             ScreenDataState.ScreenLocation loc, float offset) {
        if (sound.isGlobal || loc == null) {
            byte[] data = new byte[9];
            packFloat(data, 0, sound.volume);
            data[4] = (byte) (sound.loop ? 1 : 0);
            packFloat(data, 5, offset);
            sendDirect(player, "play", sound.id, 0, data);
        } else {
            byte[] dimBytes = loc.dimension.getBytes(StandardCharsets.UTF_8);
            int dimLen = dimBytes.length;
            byte[] data = new byte[25 + 4 + dimLen];
            packFloat(data, 0, sound.volume);
            packFloat(data, 4, sound.rolloff);
            packFloat(data, 8, (float) loc.x);
            packFloat(data, 12, (float) loc.y);
            packFloat(data, 16, (float) loc.z);
            packFloat(data, 20, offset);
            data[24] = (byte) (sound.loop ? 1 : 0);
            data[25] = (byte) (dimLen >> 24);
            data[26] = (byte) (dimLen >> 16);
            data[27] = (byte) (dimLen >> 8);
            data[28] = (byte) (dimLen);
            System.arraycopy(dimBytes, 0, data, 29, dimLen);
            sendDirect(player, "play3d", sound.id, 0, data);
        }
    }

    private static void sendDirect(ServerPlayerEntity player, String action,
                                   String id, int sampleRate, byte[] data) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(action);
            buf.writeString(id);
            buf.writeInt(sampleRate);
            buf.writeInt(data.length);
            buf.writeBytes(data);
            ServerPlayNetworking.send(player, AUDIO_PACKET_ID, buf);
        } catch (Exception ignored) {}
    }

    private static void sendAudioPacketDirect(ServerPlayerEntity player, String action,
                                              String id, int sampleRate, byte[] data) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(action);
            buf.writeString(id);
            buf.writeInt(sampleRate);
            buf.writeInt(data.length);
            buf.writeBytes(data);
            ServerPlayNetworking.send(player, AUDIO_PACKET_ID, buf);
        } catch (Exception e) {
        }
    }

    public static void syncAudioDataToPlayer(ServerPlayerEntity player, String id, byte[] data, int rate) {
        new Thread(() -> {
            try {
                int chunkSize = 32000;
                for (int i = 0; i < data.length; i += chunkSize) {
                    int end = Math.min(i + chunkSize, data.length);
                    byte[] chunk = java.util.Arrays.copyOfRange(data, i, end);
                    String action = (i == 0) ? "loadStart" : (end == data.length ? "loadEnd" : "loadContinue");
                    if (data.length <= chunkSize) action = "load";

                    player.networkHandler.sendPacket(createAudioPacket(action, id, rate, chunk));
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void sendAudioPacket(ServerPlayerEntity player, String action, String id, int sampleRate, byte[] data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(action);
        buf.writeString(id);
        buf.writeInt(sampleRate);
        buf.writeInt(data.length);
        buf.writeBytes(data);
        ServerPlayNetworking.send(player, AUDIO_PACKET_ID, buf);
    }

    private static void packFloat(byte[] data, int offset, float value) {
        int bits = Float.floatToIntBits(value);
        data[offset] = (byte)(bits & 0xFF);
        data[offset + 1] = (byte)((bits >> 8) & 0xFF);
        data[offset + 2] = (byte)((bits >> 16) & 0xFF);
        data[offset + 3] = (byte)((bits >> 24) & 0xFF);
    }



    public static void broadcastEvent(String eventData) {
        for (BridgeClientHandler client : activeClients) {
            client.eventQueue.add(eventData);
        }
    }

    @Override
    public void onInitialize() {

        Registry.register(Registries.BLOCK, new Identifier("mcapibridge", "screen"), SCREEN_BLOCK);
        Item screenItem = Registry.register(Registries.ITEM, new Identifier("mcapibridge", "screen"), new ScreenBlockItem(SCREEN_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier("mcapibridge", "io"), IO_BLOCK);
        Item ioItem = Registry.register(Registries.ITEM, new Identifier("mcapibridge", "io"), new IOBlockItem(IO_BLOCK, new Item.Settings()));

        SCREEN_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier("mcapibridge", "screen_block_entity"),
                FabricBlockEntityTypeBuilder.create(ScreenBlockEntity::new, SCREEN_BLOCK).build()
        );
        IO_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier("mcapibridge", "io_block_entity"),
                FabricBlockEntityTypeBuilder.create(IOBlockEntity::new, IO_BLOCK).build()
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.add(screenItem));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(content -> content.add(ioItem));

        ServerPlayNetworking.registerGlobalReceiver(CLICK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            int action = buf.readInt();
            server.execute(() -> {
                HitResult hit = longDistanceRaycast(player.getServerWorld(), player, 500.0);
                BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                String eventData = String.format("%d,%d,%d,%d,%d,%d",
                        pos.getX(), pos.getY(), pos.getZ(), 0, player.getId(), action);
                broadcastEvent(eventData);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SCREEN_SET_ID_ID, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int newId = buf.readInt();
            server.execute(() -> {
                if (player.squaredDistanceTo(pos.toCenterPos()) > 64.0) return;
                BlockState state = player.getWorld().getBlockState(pos);
                if (state.getBlock() instanceof ScreenBlock screenBlock) {
                    screenBlock.configureScreen((ServerWorld)player.getWorld(), pos, player, state.get(ScreenBlock.FACING), newId);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(IO_SET_CONFIG_ID, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int newId = buf.readInt();
            boolean newMode = buf.readBoolean();
            server.execute(() -> {
                if (player.squaredDistanceTo(pos.toCenterPos()) > 64.0) return;
                BlockState state = player.getWorld().getBlockState(pos);
                if (state.getBlock() instanceof IOBlock ioBlock) {
                    ioBlock.configure((ServerWorld)player.getWorld(), pos, newId, newMode);
                    player.sendMessage(Text.translatable("mcapibridge.msg.io.success", newId), true);
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (server.getPlayerManager().getPlayerList().isEmpty()) {
                for (BridgeClientHandler h : BridgeSocketServer.activeHandlers) {
                    h.close();
                }
            }
        });


        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String name = sender.getName().getString();
            String content = message.getContent().getString();
            String chatData = name + "," + content.replace("|", "");

            for (BridgeClientHandler client : activeClients) {
                client.chatQueue.add(chatData);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[MCAPI] Server Starting...");
            serverInstance = server;
            if (serverThread != null) serverThread.stopServer();

            serverThread = new BridgeSocketServer(server);
            serverThread.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            System.out.println("[MCAPI] Server Stopping... Closing sockets.");

            if (serverThread != null) {
                serverThread.stopServer();
            }
            for (BridgeClientHandler handler : BridgeSocketServer.activeHandlers) {
                handler.close();
            }
            BridgeSocketServer.activeHandlers.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            playerListeningState.remove(handler.getPlayer().getUuid());

            playerListeningState.remove(player.getUuid());
            String uuidStr = player.getUuid().toString();
            lastSentDataTime.keySet().removeIf(key -> key.startsWith(uuidStr));

            player.sendMessage(Text.of("§e========================================"));
            player.sendMessage(Text.translatable("mcapibridge.msg.welcome.1"));
            //player.sendMessage(Text.of("§7Your ID : §a" + player.getId()));
            player.sendMessage(Text.translatable("mcapibridge.msg.welcome.2"));
            player.sendMessage(Text.translatable("mcapibridge.msg.welcome.3"));
            player.sendMessage(Text.of("§e========================================"));

            // eventQueue.add("PLAYER_JOIN," + player.getName().getString() + "," + player.getId());
        });

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    long now = System.currentTimeMillis();
                    var it = activeSounds.entrySet().iterator();
                    while (it.hasNext()) {
                        var entry = it.next();
                        ActiveSound s = entry.getValue();
                        if (!s.loop && s.duration > 0 && !s.isPaused) {
                            if (now - s.startTime > (s.duration * 1000 + 5000)) {
                                it.remove();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    if (serverInstance == null || !serverInstance.isRunning()) continue;
                    updateAudioState();
                } catch (Exception ignored) {}
            }
        }).start();

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {

        });


/*
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world instanceof ServerWorld) {
                // Action 1 = Left Click
                eventQueue.add(String.format("%d,%d,%d,%d,%d,1", pos.getX(), pos.getY(), pos.getZ(), 0, player.getId()));
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand == Hand.MAIN_HAND && world instanceof ServerWorld) {
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                    HitResult hit = longDistanceRaycast(world, serverPlayer, 500.0);

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();

                        // Action 2 = Right Click
                        eventQueue.add(String.format("%d,%d,%d,%d,%d,2", pos.getX(), pos.getY(), pos.getZ(), 0, player.getId()));
                        // System.out.println("Remote Hit at: " + pos);
                    }
                }
            }

            ItemStack stack = player.getStackInHand(hand);
            return TypedActionResult.pass(stack);
        });
        //Server Events
        */


    }


    private static HitResult longDistanceRaycast(World world, ServerPlayerEntity player, double maxDistance) {
        Vec3d start = player.getEyePos();
        Vec3d rotation = player.getRotationVec(1.0F);
        Vec3d end = start.add(rotation.x * maxDistance, rotation.y * maxDistance, rotation.z * maxDistance);

        return world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,//OR ANY
                player
        ));
    }

    private static class BridgeSocketServer extends Thread {
        private final MinecraftServer mcServer;
        private ServerSocket serverSocket;
        private boolean running = true;
        public static final List<BridgeClientHandler> activeHandlers = new CopyOnWriteArrayList<>();
        public BridgeSocketServer(MinecraftServer mcServer) { this.mcServer = mcServer; }
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(4711);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    BridgeClientHandler handler = new BridgeClientHandler(clientSocket, mcServer);
                    Mcapibridge.activeClients.add(handler);
                    handler.start();
                }
            } catch (Exception e) {}
        }
        public void stopServer() { running = false; try { if(serverSocket!=null) serverSocket.close(); } catch(Exception e){} }
    }

    private static class BridgeClientHandler extends Thread {
        private final Socket socket;
        private final MinecraftServer mcServer;
        public BridgeClientHandler(Socket socket, MinecraftServer mcServer) { this.socket = socket; this.mcServer = mcServer; }
        public final Queue<String> eventQueue = new ConcurrentLinkedQueue<>();
        public final Queue<String> chatQueue = new ConcurrentLinkedQueue<>();

        @Override
        public void run() {
            try {
                socket.setSoTimeout(1000);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                while (true) {
                    if (!mcServer.isRunning()) {
                        break;
                    }

                    try {
                        String line = in.readLine();
                        if (line == null) break;

                        String res = handleCommand(line.trim());
                        if (res != null) out.println(res);

                    } catch (java.net.SocketTimeoutException e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
                BridgeSocketServer.activeHandlers.remove(this);
            }
        }

        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[MCAPI] Client disconnected by server shutdown.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private String handleCommand(String cmd) {
            try {
                if (!cmd.contains("(")) return null;
                String method = cmd.substring(0, cmd.indexOf("("));
                String body = cmd.substring(cmd.indexOf("(") + 1, cmd.lastIndexOf(")"));
                String[] args = body.isEmpty() ? new String[0] : body.split(",");

                switch (method) {
                    case "world.setBlock": return setBlock(args);
                    case "world.spawnEntity": return spawnEntity(args);
                    case "world.spawnParticle": return spawnParticle(args);
                    case "player.getPos": return getPlayerPos(args);
                    case "player.getInfo": return getPlayerInfo();
                    case "server.runCommand": return runNativeCommand(args);
                    case "chat.post": return postChat(args);
                    case "events.block.hits": return getHits();
                    case "player.getDetails": return getPlayerDetails(args);
                    case "player.getInventory": return getInventory(args);
                    case "player.setHealth": return setHealth(args);
                    case "player.setFood": return setFood(args);
                    case "player.give": return giveItem(args);
                    case "player.clear": return clearItem(args);
                    case "player.effect": return giveEffect(args);
                    case "player.teleport": return teleport(args);
                    case "entity.teleport": return teleportEntity(args);
                    case "world.getPlayers": return getWorldPlayers();
                    case "events.chat.posts": return getChatPosts();
                    case "player.setFlying": return setFlying(args);
                    case "player.setSpeed": return setSpeed(args);
                    case "player.setGod": return setGod(args);
                    case "entity.setVelocity": return setEntityVelocity(args);
                    case "entity.setNoGravity": return setEntityNoGravity(args);
                    case "world.getBlock": return getBlock(args);
                    case "world.getEntities": return getEntities(args);
                    case "world.setSign": return setSignText(args);
                    case "player.lookAt": return lookAt(args);
                    case "entity.setNbt": return setEntityNbt(args);
                    case "block.setNbt": return setBlockNbt(args);
                    case "audio.load": return audioLoad(args);
                    case "audio.stream": return audioStream(args);
                    case "audio.play": return audioPlay(args);
                    case "audio.play3d": return audioPlay3d(args);
                    case "audio.pause": return audioPause(args);
                    case "audio.stop": return audioStop(args);
                    case "audio.unload": return audioUnload(args);
                    case "audio.volume": return audioVolume(args);
                    case "audio.position": return audioPosition(args);
                    case "audio.finishLoad": return audioFinishLoad(args);
                    case "screen.update": return screenUpdate(args);
                    case "screen.getPos": return getScreenPos(args);
                    case "screen.register": return registerScreen(args);
                    case "audio.clone": return audioClone(args);
                    case "audio.reset": return audioReset(args);
                    case "audio.playScreen": return audioPlayScreen(args);
                    case "audio.syncProgress": return audioSyncProgress(args);
                    case "io.write": return ioWrite(args);
                    case "io.read": return ioRead(args);
                    case "io.config": return ioConfig(args);
                    default: return null;
                }
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }

        private java.util.List<ServerPlayerEntity> getTargetPlayers(String target) {
            if (target.equals("@a")) {
                return new java.util.ArrayList<>(mcServer.getPlayerManager().getPlayerList());
            } else if (target.isEmpty()) {
                if (mcServer.getPlayerManager().getPlayerList().isEmpty()) {
                    return java.util.Collections.emptyList();
                }
                return java.util.List.of(mcServer.getPlayerManager().getPlayerList().get(0));
            } else {
                ServerPlayerEntity p = findPlayer(target);
                return p != null ? java.util.List.of(p) : java.util.Collections.emptyList();
            }
        }

        private String setBlock(String[] args) {
            // args: x, y, z, blockName, [optional: dimension]
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);
            String input = args[3].trim();

            String blockId;
            Map<String, String> properties = new HashMap<>();

            if (input.contains("[")) {
                int bracketIndex = input.indexOf("[");
                blockId = input.substring(0, bracketIndex);
                String propStr = input.substring(bracketIndex + 1, input.length() - 1); // 去掉 [ ]

                for (String p : propStr.split(",")) {
                    String[] kv = p.split("=");
                    if (kv.length == 2) {
                        properties.put(kv[0].trim(), kv[1].trim());
                    }
                }
            } else {
                blockId = input;
            }

            if (!blockId.contains(":")) blockId = "minecraft:" + blockId;
            final String finalId = blockId;
            final Map<String, String> finalProps = properties;

            final String[] finalArgs = args;

            mcServer.execute(() -> {
                ServerWorld world = resolveWorld(finalArgs, 4);

                Identifier id = new Identifier(finalId);
                Block block = Registries.BLOCK.get(id);

                if (block != Blocks.AIR || finalId.equals("minecraft:air")) {
                    BlockState state = block.getDefaultState();

                    if (!finalProps.isEmpty()) {
                        for (Property<?> prop : state.getProperties()) {
                            String propName = prop.getName();
                            if (finalProps.containsKey(propName)) {
                                state = applyProperty(state, prop, finalProps.get(propName));
                            }
                        }
                    }

                    world.setBlockState(new BlockPos(x, y, z), state);
                }
            });
            return null;
        }

        private <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String valueStr) {
            Optional<T> parsedValue = property.parse(valueStr);
            if (parsedValue.isPresent()) {
                return state.with(property, parsedValue.get());
            }
            return state;
        }

        private ServerWorld resolveWorld(String[] args, int index) {
            try {
                if (args.length <= index) {
                    List<ServerPlayerEntity> players = mcServer.getPlayerManager().getPlayerList();
                    if (players.isEmpty()) {
                        return mcServer.getOverworld();
                    }
                    return players.get(0).getServerWorld();
                }

                String target = args[index].trim();

                ServerPlayerEntity p = mcServer.getPlayerManager().getPlayer(target);
                if (p != null) {
                    return p.getServerWorld();
                }

                if (!target.contains(":")) target = "minecraft:" + target;
                for (ServerWorld w : mcServer.getWorlds()) {
                    if (w.getRegistryKey().getValue().toString().equals(target)) {
                        return w;
                    }
                }
            } catch (Exception e) {
                System.out.println("World resolution error: " + e.getMessage());
            }
            return mcServer.getOverworld();
        }

        private String spawnEntity(String[] args) {
            final double x = Double.parseDouble(args[0]);
            final double y = Double.parseDouble(args[1]);
            final double z = Double.parseDouble(args[2]);
            String name = args[3].trim();
            if (!name.contains(":")) name = "minecraft:" + name;
            final String finalName = name;

            final float yaw = args.length > 4 ? Float.parseFloat(args[4]) : 0.0f;
            final float pitch = args.length > 5 ? Float.parseFloat(args[5]) : 0.0f;

            final String[] finalArgs = args;

            return CompletableFuture.supplyAsync(() -> {
                ServerWorld world = resolveWorld(finalArgs, 6);
                Identifier id = new Identifier(finalName);
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                Entity entity = type.create(world);

                if (entity != null) {
                    entity.refreshPositionAndAngles(x, y, z, yaw, pitch);
                    entity.setYaw(yaw);
                    entity.setHeadYaw(yaw);

                    if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity ||
                            entity instanceof net.minecraft.entity.projectile.ExplosiveProjectileEntity) {

                        float f = 0.017453292F;
                        float dx = -net.minecraft.util.math.MathHelper.sin(yaw * f) * net.minecraft.util.math.MathHelper.cos(pitch * f);
                        float dy = -net.minecraft.util.math.MathHelper.sin(pitch * f);
                        float dz = net.minecraft.util.math.MathHelper.cos(yaw * f) * net.minecraft.util.math.MathHelper.cos(pitch * f);

                        entity.setVelocity(dx * 0.01, dy * 0.01, dz * 0.01);
                        entity.velocityModified = true;
                    }

                    if (entity instanceof net.minecraft.entity.LivingEntity) {
                        ((net.minecraft.entity.LivingEntity) entity).setBodyYaw(yaw);
                    }

                    world.spawnEntity(entity);
                    return String.valueOf(entity.getId());
                }
                return "-1";
            }, mcServer).join();
        }

        private String spawnParticle(String[] args) {
            final double x = Double.parseDouble(args[0]);
            final double y = Double.parseDouble(args[1]);
            final double z = Double.parseDouble(args[2]);

            String name = args[3].trim();
            if (!name.contains(":")) name = "minecraft:" + name;
            final String fName = name;

            final int count = args.length > 4 ? Integer.parseInt(args[4]) : 10;
            final double dx = args.length > 5 ? Double.parseDouble(args[5]) : 0.0;
            final double dy = args.length > 6 ? Double.parseDouble(args[6]) : 0.0;
            final double dz = args.length > 7 ? Double.parseDouble(args[7]) : 0.0;
            final double speed = args.length > 8 ? Double.parseDouble(args[8]) : 0.0;
            final String[] finalArgs = args;

            mcServer.execute(() -> {
                ServerWorld w = resolveWorld(finalArgs, 9);
                ParticleType<?> t = Registries.PARTICLE_TYPE.get(new Identifier(fName));
                if (t instanceof ParticleEffect) {
                    w.spawnParticles((ParticleEffect) t, x, y, z, count, dx, dy, dz, speed);
                }
            });
            return null;
        }

        private String getPlayerPos(String[] args) {
            String target = args.length > 0 ? args[0] : "";
            return CompletableFuture.supplyAsync(() -> {
                ServerPlayerEntity p = null;

                if (target.isEmpty()) {
                    List<ServerPlayerEntity> players = mcServer.getPlayerManager().getPlayerList();
                    if (!players.isEmpty()) p = players.get(0);
                } else {
                    p = findPlayer(target);
                }
                if (p == null) return "0,0,0,0,0";

                return String.format("%.2f,%.2f,%.2f,%.2f,%.2f",
                        p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
            }, mcServer).join();
        }

        private String getPlayerInfo() {
            return CompletableFuture.supplyAsync(() -> {
                if (mcServer.getPlayerManager().getPlayerList().isEmpty()) return "None,0,0";
                ServerPlayerEntity p = mcServer.getPlayerManager().getPlayerList().get(0);
                return String.format("%s,%.1f,%d", p.getName().getString(), p.getHealth(), p.getHungerManager().getFoodLevel());
            }, mcServer).join();
        }

        private String runNativeCommand(String[] args) {
            final String command = String.join(",", args);
            mcServer.execute(() -> {
                try {
                    ServerCommandSource silentSource = mcServer.getCommandSource().withSilent();
                    mcServer.getCommandManager().getDispatcher().execute(command, silentSource);
                } catch (Exception e) {}
            });
            return null;
        }
        private String postChat(String[] args) {
            final String msg = String.join(",", args);
            mcServer.execute(() -> mcServer.getPlayerManager().broadcast(Text.of(msg), false));
            return null;
        }
        private String getHits() {
            StringBuilder sb = new StringBuilder();
            String h;
            while((h=this.eventQueue.poll())!=null){if(sb.length()>0)sb.append("|");sb.append(h);}
            return sb.toString();
        }

        private String getChatPosts() {
            StringBuilder sb = new StringBuilder();
            String chat;
            while ((chat = this.chatQueue.poll()) != null) {
                if (sb.length() > 0) sb.append("|");
                sb.append(chat);
            }
            return sb.toString();
        }
        private ServerPlayerEntity findPlayer(String idOrName) {
            try {
                int id = Integer.parseInt(idOrName);

                for (ServerWorld world : mcServer.getWorlds()) {
                    Entity e = world.getEntityById(id);
                    if (e instanceof ServerPlayerEntity) {
                        return (ServerPlayerEntity) e;
                    }
                }
            } catch (NumberFormatException e) {
                return mcServer.getPlayerManager().getPlayer(idOrName);
            }
            return null;
        }
        //Name1,Id1|Name2,Id2|...
        private String getWorldPlayers() {
            return CompletableFuture.supplyAsync(() -> {
                List<ServerPlayerEntity> players = mcServer.getPlayerManager().getPlayerList();
                if (players.isEmpty()) return "";

                StringBuilder sb = new StringBuilder();
                for (ServerPlayerEntity p : players) {
                    if (sb.length() > 0) sb.append("|");
                    sb.append(p.getName().getString()).append(",").append(p.getId());
                }
                return sb.toString();
            }, mcServer).join();
        }

        private String getPlayerDetails(String[] args) {
            String target = args.length > 0 ? args[0] : "";
            return CompletableFuture.supplyAsync(() -> {
                ServerPlayerEntity p = target.isEmpty() ? mcServer.getPlayerManager().getPlayerList().get(0) : findPlayer(target);
                if (p == null) return "Error: Player not found";

                String mode = p.interactionManager.getGameMode().getName(); // survival, creative...
                float hp = p.getHealth();
                float maxHp = p.getMaxHealth();
                int food = p.getHungerManager().getFoodLevel();
                String heldItem = Registries.ITEM.getId(p.getMainHandStack().getItem()).toString();
                int heldCount = p.getMainHandStack().getCount();

                //Name,ID,Mode,HP,MaxHP,Food,HeldItem,Count
                return String.format("%s,%d,%s,%.1f,%.1f,%d,%s,%d",
                        p.getName().getString(), p.getId(), mode, hp, maxHp, food, heldItem, heldCount);
            }, mcServer).join();
        }


        private String getInventory(String[] args) {
            final String target = args.length > 0 ? args[0] : "";

            java.util.concurrent.FutureTask<String> task = new java.util.concurrent.FutureTask<>(() -> {
                ServerPlayerEntity p = target.isEmpty()
                        ? mcServer.getPlayerManager().getPlayerList().get(0)
                        : findPlayer(target);

                if (p == null) return "ERROR:NoPlayer";

                StringBuilder sb = new StringBuilder();
                PlayerInventory inv = p.getInventory();

                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (!stack.isEmpty()) {
                        if (sb.length() > 0) sb.append("|");
                        sb.append(i).append(":")
                                .append(Registries.ITEM.getId(stack.getItem())).append(":")
                                .append(stack.getCount());
                    }
                }

                if (sb.length() == 0) return "EMPTY";

                return sb.toString();
            });

            mcServer.execute(task);

            try {
                return task.get();
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:Exception";
            }
        }


        private String setHealth(String[] args) {
            String target = args[0];
            float amount = Float.parseFloat(args[1]);
            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    if (amount > p.getMaxHealth()) {
                        p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(amount);
                    }
                    p.setHealth(amount);
                }
            });
            return null;
        }

        private String setFood(String[] args) {
            String target = args[0];
            int amount = Integer.parseInt(args[1]);
            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    p.getHungerManager().setFoodLevel(amount);
                    p.getHungerManager().setSaturationLevel(5.0f);
                }
            });
            return null;
        }

        private String giveItem(String[] args) {
            String target = args[0];
            String itemID = args[1].contains(":") ? args[1] : "minecraft:" + args[1];
            int count = Integer.parseInt(args[2]);
            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    ItemStack stack = new ItemStack(Registries.ITEM.get(new Identifier(itemID)), count);
                    p.giveItemStack(stack);
                }
            });
            return null;
        }

        private String clearItem(String[] args) {
            String target = args[0];
            String itemID = args.length > 1 ? (args[1].contains(":") ? args[1] : "minecraft:" + args[1]) : "all";

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    PlayerInventory inv = p.getInventory();

                    if (itemID.equals("all")) {
                        inv.clear();
                    } else {
                        for (int i = 0; i < inv.main.size(); i++) {
                            ItemStack stack = inv.main.get(i);
                            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemID)) {
                                inv.main.set(i, ItemStack.EMPTY);
                            }
                        }

                        for (int i = 0; i < inv.armor.size(); i++) {
                            ItemStack stack = inv.armor.get(i);
                            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemID)) {
                                inv.armor.set(i, ItemStack.EMPTY);
                            }
                        }

                        for (int i = 0; i < inv.offHand.size(); i++) {
                            ItemStack stack = inv.offHand.get(i);
                            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(itemID)) {
                                inv.offHand.set(i, ItemStack.EMPTY);
                            }
                        }
                    }

                    p.currentScreenHandler.sendContentUpdates();
                    p.playerScreenHandler.onContentChanged(inv);
                }
            });
            return null;
        }

        private String giveEffect(String[] args) {
            String target = args[0];
            String effectName = args[1].contains(":") ? args[1] : "minecraft:" + args[1];
            int duration = Integer.parseInt(args[2]) * 20;
            int amplifier = Integer.parseInt(args[3]);

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    Identifier id = new Identifier(effectName);
                    Optional<RegistryEntry.Reference<StatusEffect>> entry = Registries.STATUS_EFFECT.getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, id));

                    if (entry.isPresent()) {
                        p.addStatusEffect(new StatusEffectInstance(entry.get().value(), duration, amplifier));
                    }
                }
            });
            return null;
        }
        private String teleport(String[] args) {
            final String target;
            final double x, y, z;
            if (args.length == 4) {
                target = args[0];
                x = Double.parseDouble(args[1]);
                y = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);
            } else {
                target = "";
                x = Double.parseDouble(args[0]);
                y = Double.parseDouble(args[1]);
                z = Double.parseDouble(args[2]);
            }
            mcServer.execute(() -> {
                ServerPlayerEntity p = null;
                if (target.isEmpty()) {
                    List<ServerPlayerEntity> players = mcServer.getPlayerManager().getPlayerList();
                    if (!players.isEmpty()) p = players.get(0);
                } else {
                    p = findPlayer(target);
                }

                if (p != null) {
                    p.teleport(p.getServerWorld(), x, y, z, p.getYaw(), p.getPitch());
                }
            });
            return null;
        }

        private String teleportEntity(String[] args) {
            final int id = Integer.parseInt(args[0]);
            final double x = Double.parseDouble(args[1]);
            final double y = Double.parseDouble(args[2]);
            final double z = Double.parseDouble(args[3]);

            mcServer.execute(() -> {
                Entity e = null;
                for (ServerWorld w : mcServer.getWorlds()) {
                    e = w.getEntityById(id);
                    if (e != null) break;
                }

                if (e != null) {
                    ServerWorld destinationWorld = (ServerWorld) e.getWorld();

                    if (e instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) e).teleport(destinationWorld, x, y, z, e.getYaw(), e.getPitch());
                    } else {
                        e.refreshPositionAndAngles(x, y, z, e.getYaw(), e.getPitch());
                        e.setVelocity(0,0,0);
                        e.velocityModified = true;
                    }
                }
            });
            return null;
        }


        private String setFlying(String[] args) {
            String target = args[0];
            boolean allow = Boolean.parseBoolean(args[1]);
            boolean isFlying = Boolean.parseBoolean(args[2]);

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    p.getAbilities().allowFlying = allow;
                    p.getAbilities().flying = isFlying;

                    p.sendAbilitiesUpdate();
                }
            });
            return null;
        }

        private String setSpeed(String[] args) {
            String target = args[0];
            boolean isFlySpeed = Boolean.parseBoolean(args[1]);
            float value = Float.parseFloat(args[2]);

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    if (isFlySpeed) {
                        p.getAbilities().setFlySpeed(value);
                    } else {
                        p.getAbilities().setWalkSpeed(value);
                    }
                    p.sendAbilitiesUpdate();
                }
            });
            return null;
        }

        private String setGod(String[] args) {
            String target = args[0];
            boolean invulnerable = Boolean.parseBoolean(args[1]);

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    p.getAbilities().invulnerable = invulnerable;
                    p.sendAbilitiesUpdate();
                }
            });
            return null;
        }

        private String setEntityVelocity(String[] args) {
            int id = Integer.parseInt(args[0]);
            double vx = Double.parseDouble(args[1]);
            double vy = Double.parseDouble(args[2]);
            double vz = Double.parseDouble(args[3]);

            mcServer.execute(() -> {
                Entity targetEntity = null;
                for (ServerWorld w : mcServer.getWorlds()) {
                    targetEntity = w.getEntityById(id);
                    if (targetEntity != null) break;
                }

                if (targetEntity != null) {
                    targetEntity.setVelocity(vx, vy, vz);
                    targetEntity.velocityModified = true;

                    if (targetEntity instanceof net.minecraft.entity.projectile.AbstractFireballEntity) {
                        net.minecraft.nbt.NbtCompound nbt = targetEntity.writeNbt(new net.minecraft.nbt.NbtCompound());

                        net.minecraft.nbt.NbtList powerList = new net.minecraft.nbt.NbtList();
                        powerList.add(net.minecraft.nbt.NbtDouble.of(vx * 0.1)); // 0.1 系数防止飞太快
                        powerList.add(net.minecraft.nbt.NbtDouble.of(vy * 0.1));
                        powerList.add(net.minecraft.nbt.NbtDouble.of(vz * 0.1));

                        nbt.put("power", powerList);

                        targetEntity.readNbt(nbt);
                    }

                    net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket packet =
                            new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(targetEntity);

                    ServerWorld entityWorld = (ServerWorld) targetEntity.getWorld();
                    for (ServerPlayerEntity p : entityWorld.getPlayers()) {
                        p.networkHandler.sendPacket(packet);
                    }
                }
            });
            return null;
        }

        private String setEntityNoGravity(String[] args) {
            int id = Integer.parseInt(args[0]);
            boolean noGravity = Boolean.parseBoolean(args[1]);

            mcServer.execute(() -> {
                Entity targetEntity = null;
                for (ServerWorld w : mcServer.getWorlds()) {
                    targetEntity = w.getEntityById(id);
                    if (targetEntity != null) break;
                }

                if (targetEntity != null) {
                    targetEntity.setNoGravity(noGravity);
                }
            });
            return null;
        }

        private String getBlock(String[] args) {
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);
            final String[] finalArgs = args;

            return CompletableFuture.supplyAsync(() -> {
                ServerWorld world = resolveWorld(finalArgs, 3);
                BlockPos pos = new BlockPos(x, y, z);
                return Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
            }, mcServer).join();
        }

        // args: x, y, z, radius, [dimension]
        // ID,Name,X,Y,Z|ID,Name,X,Y,Z...
        private String getEntities(String[] args) {
            final double x = Double.parseDouble(args[0]);
            final double y = Double.parseDouble(args[1]);
            final double z = Double.parseDouble(args[2]);
            final double radius = Double.parseDouble(args[3]);
            final String[] finalArgs = args;

            return CompletableFuture.supplyAsync(() -> {
                ServerWorld world = resolveWorld(finalArgs, 4);
                List<Entity> entities = world.getEntitiesByClass(Entity.class,
                        new net.minecraft.util.math.Box(x-radius, y-radius, z-radius, x+radius, y+radius, z+radius),
                        e -> true); // e -> !e.isSpectator()

                StringBuilder sb = new StringBuilder();
                for (Entity e : entities) {
                    if (sb.length() > 0) sb.append("|");
                    // ID,Type,X,Y,Z
                    String type = Registries.ENTITY_TYPE.getId(e.getType()).toString();
                    sb.append(e.getId()).append(",")
                            .append(type).append(",")
                            .append(String.format("%.2f,%.2f,%.2f", e.getX(), e.getY(), e.getZ()));
                }
                return sb.toString();
            }, mcServer).join();
        }

        // args: x, y, z, line1, line2, line3, line4, [dimension]
        private String setSignText(String[] args) {
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);
            final String l1 = args.length > 3 ? args[3] : "";
            final String l2 = args.length > 4 ? args[4] : "";
            final String l3 = args.length > 5 ? args[5] : "";
            final String l4 = args.length > 6 ? args[6] : "";

            final String[] finalArgs = args;

            mcServer.execute(() -> {
                ServerWorld world = resolveWorld(finalArgs, 7);
                BlockPos pos = new BlockPos(x, y, z);

                net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof net.minecraft.block.entity.SignBlockEntity) {
                    net.minecraft.block.entity.SignBlockEntity sign = (net.minecraft.block.entity.SignBlockEntity) be;
                    sign.setText(new net.minecraft.block.entity.SignText(
                            new Text[]{Text.of(l1), Text.of(l2), Text.of(l3), Text.of(l4)},
                            new Text[]{Text.of(l1), Text.of(l2), Text.of(l3), Text.of(l4)},
                            net.minecraft.util.DyeColor.BLACK, true
                    ), true); // true = front

                    sign.markDirty();
                    world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                }
            });
            return null;
        }

        private String lookAt(String[] args) {
            String target = args[0];
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);

            mcServer.execute(() -> {
                ServerPlayerEntity p = findPlayer(target);
                if (p != null) {
                    p.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(x, y, z));
                }
            });
            return null;
        }

        private String setEntityNbt(String[] args) {
            int id = Integer.parseInt(args[0]);

            String nbtRaw = reconstructArgs(args, 1);

            mcServer.execute(() -> {
                Entity entity = null;
                for (ServerWorld w : mcServer.getWorlds()) {
                    entity = w.getEntityById(id);
                    if (entity != null) break;
                }

                if (entity != null) {
                    try {
                        NbtCompound newNbt = StringNbtReader.parse(nbtRaw);

                        NbtCompound currentNbt = entity.writeNbt(new NbtCompound());

                        currentNbt.copyFrom(newNbt);

                        entity.readNbt(currentNbt);

                        if (entity instanceof ServerPlayerEntity) {
                        } else {
                        }
                    } catch (Exception e) {
                        System.out.println("NBT Error: " + e.getMessage());
                    }
                }
            });
            return null;
        }

        private String setBlockNbt(String[] args) {
            int x=Integer.parseInt(args[0]), y=Integer.parseInt(args[1]), z=Integer.parseInt(args[2]);
            final String[] fA = args;
            mcServer.execute(() -> {
                try {
                    ServerWorld world = mcServer.getOverworld();
                    String nbtRaw = "";
                    String lastArg = args[args.length-1].trim();
                    boolean hasDim = !lastArg.endsWith("}") && !lastArg.endsWith("]");
                    if (hasDim) {
                        world = resolveWorld(args, args.length-1);
                        StringBuilder sb = new StringBuilder();
                        for(int i=3;i<args.length-1;i++) sb.append(args[i]).append(i<args.length-2?",":"");
                        nbtRaw = sb.toString();
                    } else {
                        if(!mcServer.getPlayerManager().getPlayerList().isEmpty()) world=mcServer.getPlayerManager().getPlayerList().get(0).getServerWorld();
                        nbtRaw = reconstructArgs(args, 3);
                    }
                    ServerCommandSource source = mcServer.getCommandSource().withWorld(world).withSilent().withLevel(2);
                    mcServer.getCommandManager().getDispatcher().execute(String.format("data merge block %d %d %d %s",x,y,z,nbtRaw), source);
                } catch(Exception e){}
            }); return null;
        }

        private String reconstructArgs(String[] args, int startIndex) {
            StringBuilder sb = new StringBuilder();
            for (int i = startIndex; i < args.length; i++) {
                sb.append(args[i]);
                if (i < args.length - 1) sb.append(",");
            }
            return sb.toString();
        }

        private void packFloat(byte[] data, int offset, float value) {
            int bits = Float.floatToIntBits(value);
            data[offset] = (byte)(bits & 0xFF);
            data[offset + 1] = (byte)((bits >> 8) & 0xFF);
            data[offset + 2] = (byte)((bits >> 16) & 0xFF);
            data[offset + 3] = (byte)((bits >> 24) & 0xFF);
        }

        private void sendAudioToTarget(String target, String action, String id,
                                       int sampleRate, byte[] data, String dimension) {
            mcServer.execute(() -> {
                List<ServerPlayerEntity> players = getTargetPlayers(target);
                for (ServerPlayerEntity player : players) {
                    try {
                        player.networkHandler.sendPacket(createAudioPacket(action, id, sampleRate, data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private String audioLoad(String[] args) {
            String target = args[0];
            String id = args[1];


            try {
                int sampleRate = Integer.parseInt(args[2]);
                String base64Data = args[3];

                byte[] chunk = java.util.Base64.getDecoder().decode(base64Data);

                AudioBuffer buffer = new AudioBuffer();
                buffer.target = target;
                buffer.sampleRate = sampleRate;
                buffer.data.write(chunk);

                audioBuffers.put(id, buffer);
                //System.out.println("[Debug] Buffer PUT success: " + id);

            } catch (Exception e) {
                System.err.println("[Error] audioLoad Exception: " + e);
                e.printStackTrace();
            }
            return null;
        }

        private String audioStream(String[] args) {
            String id = args[1];
            //System.out.println("DEBUG: Stream chunk for " + id);

            AudioBuffer buffer = audioBuffers.get(id);
            if (buffer != null) {
                try {
                    String base64Data = args[3];
                    byte[] chunk = java.util.Base64.getDecoder().decode(base64Data);
                    buffer.data.write(chunk);
                } catch (Exception e) {
                    System.err.println("Stream Write Error: " + e);
                    e.printStackTrace();
                }
            } else {
                System.err.println("DEBUG: Buffer NOT FOUND for id: " + id);
            }
            return null;
        }

        private String audioPlay(String[] args) {
            String target = args[0];
            String id = args[1];
            float volume = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;
            boolean loop = args.length > 3 && Boolean.parseBoolean(args[3]);

            String dataId = cloneMap.getOrDefault(id, id);

            float duration = 3600f;
            if (audioDataCache.containsKey(dataId)) {
                int len = audioDataCache.get(dataId).length;
                int rate = audioRateCache.getOrDefault(dataId, 44100);
                duration = (float) len / (rate * 2.0f);
            }

            if (activeSounds.containsKey(id)) {
                ActiveSound existing = activeSounds.get(id);

                if (existing.sourceDataId.equals(dataId)) {
                    if (existing.isPaused) {
                        long pausedDuration = System.currentTimeMillis() - existing.pauseTime;
                        existing.startTime += pausedDuration;
                        existing.isPaused = false;

                        sendToTarget(target, "resume", id, 0, new byte[0]);
                    }

                    existing.volume = volume;
                    existing.loop = loop;
                    existing.duration = duration;
                    return null;
                }
            }

            ActiveSound sound = new ActiveSound(id, dataId, volume, loop, duration);
            activeSounds.put(id, sound);

            for (Set<String> listening : playerListeningState.values()) {
                listening.remove(id);
            }

            return null;
        }

        private void sendToTarget(String target, String action, String id,
                                  int sampleRate, byte[] data) {
            mcServer.execute(() -> {
                List<ServerPlayerEntity> players = getTargetPlayers(target);
                for (ServerPlayerEntity player : players) {
                    try {
                        player.networkHandler.sendPacket(
                                createAudioPacket(action, id, sampleRate, data));
                    } catch (Exception e) {}
                }
            });
        }


        private String audioPlayScreen(String[] args) {
            // args: target, audioId, screenId, volume, loop
            String audioId = args[1];
            int screenId = Integer.parseInt(args[2]);
            float volume = Float.parseFloat(args[3]);
            boolean loop = Boolean.parseBoolean(args[4]);

            String dataId = cloneMap.getOrDefault(audioId, audioId);

            float duration = 3600f;
            if (audioDataCache.containsKey(dataId)) {
                int len = audioDataCache.get(dataId).length;
                int rate = audioRateCache.getOrDefault(dataId, 44100);
                duration = (float) len / (rate * 2.0f);
            }

            ActiveSound sound = new ActiveSound(audioId, dataId, screenId, System.currentTimeMillis(), duration, volume, loop);
            activeSounds.put(audioId, sound);

            for (Set<String> listening : Mcapibridge.playerListeningState.values()) {
                listening.remove(audioId);
            }

            return null;
        }


        private String audioPause(String[] args) {
            String target = args[0];
            String id = args[1];

            ActiveSound s = activeSounds.get(id);
            if (s != null) {
                s.isPaused = true;
                s.pauseTime = System.currentTimeMillis();
                System.out.println("Paused: " + id);
            } else {
                System.out.println("Pause failed: sound not found " + id);
            }

            sendAudioToTarget(target, "pause", id, 0, new byte[0], null);
            return null;
        }

        private String audioStop(String[] args) {
            String target = args[0];
            String id = args[1];
            if (target.equals("@a")) {
                activeSounds.remove(id);
            }
            sendAudioToTarget(target, "stop", id, 0, new byte[0], null);
            return null;
        }

        private String audioUnload(String[] args) {
            String target = args[0];
            String id = args[1];
            sendAudioToTarget(target, "unload", id, 0, new byte[0], null);
            return null;
        }

        private String audioVolume(String[] args) {
            String target = args[0];
            String id = args[1];
            float volume = Float.parseFloat(args[2]);

            byte[] data = new byte[4];
            packFloat(data, 0, volume);

            sendAudioToTarget(target, "volume", id, 0, data, null);
            return null;
        }

        private String audioPosition(String[] args) {
            String target = args[0];
            String id = args[1];
            float x = Float.parseFloat(args[2]);
            float y = Float.parseFloat(args[3]);
            float z = Float.parseFloat(args[4]);

            byte[] data = new byte[12];
            packFloat(data, 0, x);
            packFloat(data, 4, y);
            packFloat(data, 8, z);

            sendAudioToTarget(target, "position", id, 0, data, null);
            return null;
        }

        private String screenUpdate(String[] args) {
            String target = args[0];
            int screenId = Integer.parseInt(args[1]);
            String base64 = args[2];
            byte[] imgData = Base64.getDecoder().decode(base64);
            long timestamp = System.currentTimeMillis();

            mcServer.execute(() -> {
                List<ServerPlayerEntity> players = getTargetPlayers(target);

                for (ServerPlayerEntity p : players) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(screenId);
                    buf.writeLong(timestamp);
                    buf.writeInt(imgData.length);
                    buf.writeBytes(imgData);
                    p.networkHandler.sendPacket(
                            ServerPlayNetworking.createS2CPacket(Mcapibridge.SCREEN_FRAME_ID, buf)
                    );
                }
            });
            return null;
        }

        private String audioFinishLoad(String[] args) {
            String target = args[0];
            String id = args[1];

            AudioBuffer buffer = audioBuffers.remove(id);
            if (buffer == null) return null;

            byte[] fullData = buffer.data.toByteArray();
            int sampleRate = buffer.sampleRate;

            audioDataCache.put(id, fullData);
            audioRateCache.put(id, sampleRate);

            mcServer.execute(() -> {
                List<ServerPlayerEntity> players = getTargetPlayers(target);

                for (ServerPlayerEntity player : players) {
                    int chunkSize = 32000;
                    for (int i = 0; i < fullData.length; i += chunkSize) {
                        int end = Math.min(i + chunkSize, fullData.length);
                        byte[] chunk = java.util.Arrays.copyOfRange(fullData, i, end);

                        String action;
                        if (i == 0) action = "loadStart";
                        else if (end == fullData.length) action = "loadEnd";
                        else action = "loadContinue";
                        if (fullData.length <= chunkSize) action = "load";

                        player.networkHandler.sendPacket(createAudioPacket(action, id, sampleRate, chunk));
                    }

                    String key = player.getUuid() + "_" + id;
                    lastSentDataTime.put(key, System.currentTimeMillis());
                }
            });
            return null;
        }

        private String getScreenPos(String[] args) {
            try {
                int id = Integer.parseInt(args[0]);
                ScreenDataState state = ScreenDataState.getServerState(mcServer);
                List<ScreenDataState.ScreenLocation> centers = state.getScreens(id);

                if (centers == null || centers.isEmpty()) return "ERROR";

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < centers.size(); i++) {
                    var loc = centers.get(i);
                    if (i > 0) sb.append("|");

                    sb.append(String.format(java.util.Locale.US, "%.2f,%.2f,%.2f,%s",
                            loc.x, loc.y, loc.z, loc.dimension));
                }
                return sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "ERROR";
        }

        private String registerScreen(String[] args) {
            try {
                int id = Integer.parseInt(args[0]);
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                String dimension = args.length > 4 ? args[4] : "minecraft:overworld";

                mcServer.execute(() -> {
                    ScreenDataState state = ScreenDataState.getServerState(mcServer);
                    state.addScreen(id, new Vec3d(x, y, z), dimension);
                });
            } catch (Exception e) {}
            return null;
        }


        private String audioClone(String[] args) {
            String target = args[0];
            String sourceId = args[1];
            String newId = args[2];

            String rootId = cloneMap.getOrDefault(sourceId, sourceId);
            cloneMap.put(newId, rootId);

            byte[] data = sourceId.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            sendAudioToTarget(target, "clone", newId, 0, data, null);
            return null;
        }

        private String audioReset(String[] args) {
            String target = args[0];

            Mcapibridge.activeSounds.clear();
            Mcapibridge.cloneMap.clear();
            Mcapibridge.audioDataCache.clear();
            Mcapibridge.audioRateCache.clear();

            sendAudioToTarget(target, "reset", "all", 0, new byte[0], null);

            return null;
        }

        private String audioPlay3d(String[] args) {
            String target = args[0];
            String id = args[1];
            float x = Float.parseFloat(args[2]);
            float y = Float.parseFloat(args[3]);
            float z = Float.parseFloat(args[4]);
            float volume = args.length > 5 ? Float.parseFloat(args[5]) : 1.0f;
            float rolloff = args.length > 6 ? Float.parseFloat(args[6]) : 1.0f;
            boolean loop = args.length > 7 && Boolean.parseBoolean(args[7]);
            String dimension = args.length > 8 ? args[8] : "minecraft:overworld";
            try {
                ServerWorld w = resolveWorld(args, 8);
                dimension = w.getRegistryKey().getValue().toString();
            } catch(Exception ignored) {}

            String dataId = cloneMap.getOrDefault(id, id);
            float duration = 3600f;
            if (audioDataCache.containsKey(dataId)) {
                int len = audioDataCache.get(dataId).length;
                int rate = audioRateCache.getOrDefault(dataId, 44100);
                duration = (float) len / (rate * 2.0f);
            }

            ActiveSound sound = new ActiveSound(id, dataId, x, y, z, dimension, System.currentTimeMillis(), duration, volume, rolloff, loop);
            activeSounds.put(id, sound);

            for (Set<String> listening : Mcapibridge.playerListeningState.values()) {
                listening.remove(id);
            }
            return null;
        }

        private String audioSyncProgress(String[] args) {
            String id = args[1];
            float progress = Float.parseFloat(args[2]);

            ActiveSound sound = Mcapibridge.activeSounds.get(id);
            if (sound != null) {
                sound.startTime = System.currentTimeMillis() - (long)(progress * 1000);
                // System.out.println("Synced progress for " + id + ": " + progress + "s");
            }
            return null;
        }

        private String ioWrite(String[] args) {
            try {
                int id = Integer.parseInt(args[0]);
                int power = Integer.parseInt(args[1]);

                mcServer.execute(() -> {
                    IODataState state = IODataState.getServerState(mcServer);
                    List<GlobalPos> list = state.getBlocks(id);

                    for (GlobalPos gp : list) {
                        ServerWorld world = mcServer.getWorld(gp.getDimension());
                        if (world != null) {
                            BlockPos pos = gp.getPos();
                            if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
                                if (!be.isOutputMode) {
                                    be.power = Math.max(0, Math.min(15, power));
                                    be.markDirty();
                                    world.updateNeighborsAlways(pos, be.getCachedState().getBlock());
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {}
            return null;
        }

        private String ioRead(String[] args) {
            try {
                int id = Integer.parseInt(args[0]);
                return CompletableFuture.supplyAsync(() -> {
                    IODataState state = IODataState.getServerState(mcServer);
                    List<GlobalPos> list = state.getBlocks(id);

                    int maxPower = 0;
                    for (GlobalPos gp : list) {
                        ServerWorld world = mcServer.getWorld(gp.getDimension());
                        if (world != null) {
                            BlockPos pos = gp.getPos();
                            if (world.getBlockEntity(pos) instanceof IOBlockEntity be) {
                                if (be.isOutputMode) {
                                    maxPower = Math.max(maxPower, be.power);
                                }
                            }
                        }
                    }
                    return String.valueOf(maxPower);
                }, mcServer).join();

            } catch (Exception e) { return "-1"; }
        }

        private String ioConfig(String[] args) {
            try {
                final int x = Integer.parseInt(args[0]);
                final int y = Integer.parseInt(args[1]);
                final int z = Integer.parseInt(args[2]);
                final int newId = Integer.parseInt(args[3]);
                final boolean newMode = Boolean.parseBoolean(args[4]);

                final ServerWorld world = resolveWorld(args, 5);

                mcServer.execute(() -> {
                    if (world == null) return;

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (state.getBlock() instanceof IOBlock ioBlock) {
                        ioBlock.configure(world, pos, newId, newMode);
                    }
                });

            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
            return null;
        }

    }
}