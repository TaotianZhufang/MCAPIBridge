package org.taskchou.mcapibridge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.charset.StandardCharsets;
import java.io.OutputStreamWriter;

public class Mcapibridge implements ModInitializer {

    public static final Queue<String> eventQueue = new ConcurrentLinkedQueue<>();
    public static final Queue<String> chatQueue = new ConcurrentLinkedQueue<>();
    public static final Identifier CLICK_PACKET_ID = new Identifier("mcapibridge", "click_event");
    private static BridgeSocketServer serverThread;

    public record ClickPayload(int action) implements CustomPayload {
        public static final CustomPayload.Id<ClickPayload> ID = new CustomPayload.Id<>(CLICK_PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, ClickPayload> CODEC = PacketCodec.tuple(
                net.minecraft.network.codec.PacketCodecs.INTEGER, ClickPayload::action,
                ClickPayload::new
        );
        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    @Override
    public void onInitialize() {

        PayloadTypeRegistry.playC2S().register(ClickPayload.ID, ClickPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ClickPayload.ID, (payload, context) -> {
            System.out.println("DEBUG: Server received packet!");
            context.server().execute(() -> {

                ServerPlayerEntity player = context.player();
                HitResult hit = longDistanceRaycast(player.getServerWorld(), player, 500.0);
                BlockPos pos = ((BlockHitResult) hit).getBlockPos();

                eventQueue.add(String.format("%d,%d,%d,%d,%d,%d",
                        pos.getX(), pos.getY(), pos.getZ(), 0, player.getId(), payload.action()));
            });
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverThread = new BridgeSocketServer(server);
            serverThread.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (serverThread != null) serverThread.stopServer();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            player.sendMessage(Text.of("§e========================================"));
            player.sendMessage(Text.of("§bWelcome to MCAPIBridge !"));
            //player.sendMessage(Text.of("§7Your ID : §a" + player.getId()));
            player.sendMessage(Text.of("Use bridges(like Python) to control"));
            player.sendMessage(Text.of("§aConnect to the server IP at the port 4711"));
            player.sendMessage(Text.of("§e========================================"));

            // eventQueue.add("PLAYER_JOIN," + player.getName().getString() + "," + player.getId());
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

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String name = sender.getName().getString();
            String content = message.getContent().getString();

            chatQueue.add(name + "," + content.replace("|", ""));
        });

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
        public BridgeSocketServer(MinecraftServer mcServer) { this.mcServer = mcServer; }
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(4711);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    new BridgeClientHandler(clientSocket, mcServer).start();
                }
            } catch (Exception e) {}
        }
        public void stopServer() { running = false; try { if(serverSocket!=null) serverSocket.close(); } catch(Exception e){} }
    }

    private static class BridgeClientHandler extends Thread {
        private final Socket socket;
        private final MinecraftServer mcServer;
        public BridgeClientHandler(Socket socket, MinecraftServer mcServer) { this.socket = socket; this.mcServer = mcServer; }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        String res = handleCommand(line.trim());
                        if (res != null) out.println(res);
                    } catch (Exception e) {
                        System.err.println("Error:" + e.getMessage());
                        out.println("Error: Processing failed");
                    }
                }
            } catch (Exception e) {
                System.out.println("Lost connection");
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
                    default: return null;
                }
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }

        private String setBlock(String[] args) {
            // args: x, y, z, blockName, [optional: target/dimension]
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);
            String name = args[3].trim();
            if (!name.contains(":")) name = "minecraft:" + name;
            final String finalName = name;

            final String[] finalArgs = args;

            mcServer.execute(() -> {
                ServerWorld world = resolveWorld(finalArgs, 4);

                Identifier id = new Identifier(finalName);
                Block block = Registries.BLOCK.get(id);
                if (block != Blocks.AIR || finalName.equals("minecraft:air")) {
                    world.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
                }
            });
            return null;
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
                    mcServer.getCommandManager().getDispatcher().execute(command, mcServer.getCommandSource());
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
            String hit;
            while((hit=Mcapibridge.eventQueue.poll())!=null){ if(sb.length()>0)sb.append("|"); sb.append(hit); }
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
                        p.addStatusEffect(new StatusEffectInstance(entry.get(), duration, amplifier));
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

        private String getChatPosts() {
            StringBuilder sb = new StringBuilder();
            String chat;
            while ((chat = Mcapibridge.chatQueue.poll()) != null) {
                if (sb.length() > 0) sb.append("|");
                sb.append(chat);
            }
            return sb.toString();
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
    }
}