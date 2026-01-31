package org.taskchou.mcapibridge.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
                    System.out.println("DEBUG: Press:" + (i + 1));
                    ClientPlayNetworking.send(new Mcapibridge.ClickPayload(100 + (i + 1)));
                }
            }
        });
    }
}
