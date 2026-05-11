package com.yuno.yunosbosses.event;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.network.CastSpellPayload;
import com.yuno.yunosbosses.network.SpellCyclePayload;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    public static KeyBinding spellCycleKey;
    public static KeyBinding castSpellKey;

    public static void register() {
        // Register keybindings
        spellCycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yunosbosses.spellcycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.yunosbosses"
        ));
        castSpellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yunosbosses.castspell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.yunosbosses"
        ));

        // Listen for the keypress every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spellCycleKey.wasPressed()) {
                // Send the payload
                ClientPlayNetworking.send(new SpellCyclePayload());
            }
            while (castSpellKey.wasPressed()) {
                if (client.player == null || client.world == null) return;

                SpellComponent component = ModEntityComponents.SPELL_DATA.get(client.player);
                Spell activeSpell = component.getActiveSpell();

                if (activeSpell != null && activeSpell.canCastWithoutStaff()) {
                    // Send the payload to cast the spell
                    CastSpellPayload.sendCastSpellPacket(activeSpell.getId());
                }
            }
        });
    }
}
