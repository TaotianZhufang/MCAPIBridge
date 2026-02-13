package org.taskchou.mcapibridge.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.taskchou.mcapibridge.Mcapibridge;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.taskchou.mcapibridge.client.gui.IOConfigScreen;
import org.taskchou.mcapibridge.client.gui.ScreenConfigScreen;
import org.taskchou.mcapibridge.client.render.IOBlockRenderer;
import org.taskchou.mcapibridge.client.render.ScreenBlockRenderer;

public class McapibridgeClient implements ClientModInitializer {

    private static final KeyBinding[] MACRO_KEYS = new KeyBinding[5];

    @Override
    public void onInitializeClient() {
        System.out.println("MCAPIBridge Client Initialized");

        ModConfig.load();

        BlockEntityRendererRegistry.register(Mcapibridge.IO_BLOCK_ENTITY, IOBlockRenderer::new);
        BlockEntityRendererRegistry.register(Mcapibridge.SCREEN_BLOCK_ENTITY, ScreenBlockRenderer::new);


        ClientPlayNetworking.registerGlobalReceiver(Mcapibridge.SCREEN_FRAME_ID, (client, handler, buf, responseSender) -> {
            int screenId = buf.readInt();
            long timestamp = buf.readLong();
            int len = buf.readInt();
            byte[] imgData = new byte[len];
            buf.readBytes(imgData);

            client.execute(() -> {
                ScreenTextureManager.updateTexture(screenId, imgData, timestamp);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Mcapibridge.AUDIO_PACKET_ID, (client, handler, buf, responseSender) -> {
            String action = buf.readString();
            String id = buf.readString();
            int sampleRate = buf.readInt();
            int len = buf.readInt();
            byte[] data = new byte[len];
            buf.readBytes(data);

            client.execute(() -> {
                handleAudioPacket(action, id, sampleRate, data);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Mcapibridge.SCREEN_OPEN_CONFIG_ID, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int currentId = buf.readInt();

            client.execute(() -> {
                client.setScreen(new ScreenConfigScreen(pos, currentId));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Mcapibridge.IO_OPEN_CONFIG_ID, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int currentId = buf.readInt();
            boolean currentMode = buf.readBoolean();

            client.execute(() -> {
                client.setScreen(new IOConfigScreen(pos, currentId, currentMode));
            });
        });


        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            System.out.println("[MCAPIBridge] Cleaning up audio...");
            AudioPlayer.cleanup();
        });

        final boolean[] wasAttackPressed = {false};
        final boolean[] wasUsePressed = {false};

        for (int i = 0; i < 5; i++) {
            MACRO_KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.mcapibridge.macro" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_1 + i,
                    "category.mcapibridge"
            ));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            AudioPlayer.setClient(client);
            AudioPlayer.updateListener(
                    (float) client.player.getX(),
                    (float) client.player.getY() + client.player.getStandingEyeHeight(),
                    (float) client.player.getZ(),
                    client.player.getYaw(),
                    client.player.getPitch()
            );
            AudioPlayer.update();

            boolean isAttackDown = client.options.attackKey.isPressed();
            if (isAttackDown && !wasAttackPressed[0]) {
                sendClickPacket(1);
            }
            wasAttackPressed[0] = isAttackDown;

            boolean isUseDown = client.options.useKey.isPressed();
            if (isUseDown && !wasUsePressed[0]) {
                sendClickPacket(2);
            }
            wasUsePressed[0] = isUseDown;

            for (int i = 0; i < 5; i++) {
                while (MACRO_KEYS[i].wasPressed()) {
                    sendClickPacket(100 + (i + 1));
                }
            }
        });
    }

    private void sendClickPacket(int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(action);
        ClientPlayNetworking.send(Mcapibridge.CLICK_PACKET_ID, buf);
    }

    private void handleAudioPacket(String action, String id, int sampleRate, byte[] data) {
        switch (action) {
            case "load" -> {
                System.out.println("Client received LOAD packet: " + data.length + " bytes");
                AudioPlayer.loadAudio(id, data, sampleRate);
            }
            case "stream" -> AudioPlayer.streamAudio(id, data, sampleRate);
            case "loadStart" -> AudioPlayer.loadStart(id, data, sampleRate);
            case "loadContinue" -> AudioPlayer.loadContinue(id, data);
            case "loadEnd" -> AudioPlayer.loadEnd(id, data, sampleRate);
            case "play" -> {
                System.out.println("[CLIENT] Received PLAY for " + id + " at " + System.currentTimeMillis());
                float volume = 1.0f;
                boolean loop = false;
                float offset = 0.0f;

                if (data.length >= 5) {
                    volume = bytesToFloat(data, 0);
                    loop = data[4] != 0;
                }
                if (data.length >= 9) {
                    offset = bytesToFloat(data, 5);
                }
                System.out.println("Client received offset: " + offset);

                AudioPlayer.play(id, volume, loop, offset);
            }
            case "stopAll" -> AudioPlayer.stopAll();
            case "play3d" -> {
                if (data.length >= 29) {
                    float volume = bytesToFloat(data, 0);
                    float rolloff = bytesToFloat(data, 4);
                    float x = bytesToFloat(data, 8);
                    float y = bytesToFloat(data, 12);
                    float z = bytesToFloat(data, 16);
                    float offset = bytesToFloat(data, 20); // ★ 读取 Offset
                    boolean loop = data[24] != 0;

                    int len = ((data[25] & 0xFF) << 24) |
                            ((data[26] & 0xFF) << 16) |
                            ((data[27] & 0xFF) << 8) |
                            (data[28] & 0xFF);

                    String dimension = "minecraft:overworld";
                    if (len > 0 && data.length >= 29 + len) {
                        dimension = new String(data, 29, len, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    System.out.println("[CLIENT] Received PLAY for " + id + " at " + System.currentTimeMillis());
                    System.out.println("Client received offset: " + offset);
                    AudioPlayer.play3d(id, x, y, z, volume, rolloff, loop, dimension, offset);
                }
            }
            case "pause" -> AudioPlayer.pause(id);
            case "resume" -> AudioPlayer.resume(id);
            case "stop" -> AudioPlayer.stop(id);
            case "unload" -> AudioPlayer.unload(id);
            case "volume" -> {
                if (data.length >= 4) {
                    float volume = bytesToFloat(data, 0);
                    AudioPlayer.setVolume(id, volume);
                }
            }
            case "position" -> {
                if (data.length >= 12) {
                    float x = bytesToFloat(data, 0);
                    float y = bytesToFloat(data, 4);
                    float z = bytesToFloat(data, 8);
                    AudioPlayer.setPosition(id, x, y, z);
                }
            }
            case "clone" -> {
                String sourceId = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("[Debug] id:"+id+" sourceId:"+sourceId);
                AudioPlayer.cloneAudio(id, sourceId);
            }
            case "reset" -> {
                System.out.println("[CLIENT] Received RESET at " + System.currentTimeMillis());
                AudioPlayer.cleanup();
            }
        }
    }

    private float bytesToFloat(byte[] data, int offset) {
        int bits = (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }
}