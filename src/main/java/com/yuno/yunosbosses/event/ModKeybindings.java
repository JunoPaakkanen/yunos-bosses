package com.yuno.yunosbosses.event;

import com.yuno.yunosbosses.network.SpellCyclePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    public static KeyBinding spellCycleKey;

    public static void register() {
        // Register keybindings
        spellCycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yunosbosses.spellcycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.yunosbosses"
        ));

        // Listen for the keypress every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spellCycleKey.wasPressed()) {
                // Send the payload
                ClientPlayNetworking.send(new SpellCyclePayload());
            }
        });
    }
}
