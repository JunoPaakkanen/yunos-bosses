package com.yuno.yunosbosses.event;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.network.CastSpellPayload;
import com.yuno.yunosbosses.network.DomainClashInputPayload;
import com.yuno.yunosbosses.network.SpellCyclePayload;
import com.yuno.yunosbosses.render.gui.DomainClashClient;
import com.yuno.yunosbosses.render.gui.SpellInventoryScreen;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    public static KeyBinding spellCycleKey;
    public static KeyBinding castSpellKey;
    public static KeyBinding openSpellInventoryKey;

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
        openSpellInventoryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yunosbosses.open_spell_inventory",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.yunosbosses"
        ));

        // Intercept attack clicks during Domain Clash
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (DomainClashClient.isActive() && DomainClashClient.isLocalPlayerParticipant()) {
                DomainClashClient.registerLocalInput();
                if (DomainClashClient.getClashId() != null) {
                    ClientPlayNetworking.send(new DomainClashInputPayload(DomainClashClient.getClashId()));
                }
                return true; // Cancel vanilla attack
            }
            return false;
        });

        // Start client tick for held attack input & other keybindings
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (DomainClashClient.isActive() && DomainClashClient.isLocalPlayerParticipant()) {
                // If the player holds down the attack key, send steady mashing pulse every 4 ticks (5 CPS)
                if (client.options.attackKey.isPressed() && client.player != null && client.player.age % 4 == 0) {
                    DomainClashClient.registerLocalInput();
                    if (DomainClashClient.getClashId() != null) {
                        ClientPlayNetworking.send(new DomainClashInputPayload(DomainClashClient.getClashId()));
                    }
                }
            }
        });

        // Listen for the keypress every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spellCycleKey.wasPressed()) {
                // Send the payload
                ClientPlayNetworking.send(new SpellCyclePayload());
                // Play subtle UI click sound for feedback
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2F));
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
            while (openSpellInventoryKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new SpellInventoryScreen());
                }
            }
        });
    }
}
