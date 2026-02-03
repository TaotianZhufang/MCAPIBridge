package org.taskchou.mcapibridge.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.taskchou.mcapibridge.Mcapibridge;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class McapibridgeClient implements ClientModInitializer {

    private static final KeyBinding[] MACRO_KEYS = new KeyBinding[5];

    @Override
    public void onInitializeClient() {
        System.out.println("MCAPIBridge Client Initialized");

        ClientPlayNetworking.registerGlobalReceiver(Mcapibridge.AudioDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                handleAudioPacket(payload.action(), payload.id(), payload.sampleRate(), payload.data());
            });
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
                ClientPlayNetworking.send(new Mcapibridge.ClickPayload(1));
            }
            wasAttackPressed[0] = isAttackDown;

            boolean isUseDown = client.options.useKey.isPressed();
            if (isUseDown && !wasUsePressed[0]) {
                ClientPlayNetworking.send(new Mcapibridge.ClickPayload(2));
            }
            wasUsePressed[0] = isUseDown;

            for (int i = 0; i < 5; i++) {
                while (MACRO_KEYS[i].wasPressed()) {
                    ClientPlayNetworking.send(new Mcapibridge.ClickPayload(100 + (i + 1)));
                }
            }
        });
    }

    private void handleAudioPacket(String action, String id, int sampleRate, byte[] data) {
        switch (action) {
            case "load" -> AudioPlayer.loadAudio(id, data, sampleRate);
            case "stream" -> AudioPlayer.streamAudio(id, data, sampleRate);
            case "loadStart" -> AudioPlayer.loadStart(id, data, sampleRate);
            case "loadContinue" -> AudioPlayer.loadContinue(id, data);
            case "loadEnd" -> AudioPlayer.loadEnd(id, data, sampleRate);
            case "play" -> {
                float volume = 1.0f;
                boolean loop = false;
                if (data.length >= 5) {
                    volume = bytesToFloat(data, 0);
                    loop = data[4] != 0;
                }
                AudioPlayer.play(id, volume, loop);
            }
            case "play3d" -> {
                if (data.length >= 21) {
                    float volume = bytesToFloat(data, 0);
                    float rolloff = bytesToFloat(data, 4);
                    float x = bytesToFloat(data, 8);
                    float y = bytesToFloat(data, 12);
                    float z = bytesToFloat(data, 16);
                    boolean loop = data[20] != 0;
                    AudioPlayer.play3d(id, x, y, z, volume, rolloff, loop);
                }
            }
            case "pause" -> AudioPlayer.pause(id);
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