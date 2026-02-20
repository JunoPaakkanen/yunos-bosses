package com.yuno.yunosbosses.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(CommandManager.literal("learnspell")
                    .then(CommandManager.argument("spell", StringArgumentType.string())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                String spellName = StringArgumentType.getString(context, "spell");

                                // Get the component from SPELL_DATA key
                                var component = ModEntityComponents.SPELL_DATA.get(player);

                                // Find the spell by name and learn it
                                Spell spell = ModSpells.getSpellByName(spellName);

                                if (spell != null) {
                                    component.learnSpell(spell);
                                    component.setActiveSpell(spell);
                                    ModEntityComponents.SPELL_DATA.sync(player);

                                    player.sendMessage(Text.literal("Learned " + spellName), false);
                                    return 1; // Success
                                }
                                return 0; // Fail
                            })));
            dispatcher.register(CommandManager.literal("listspells")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        var component = ModEntityComponents.SPELL_DATA.get(player);
                        var spells = component.getKnownSpells();
                        if (spells.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou don't know any spells yet!"), false);
                        } else {
                            player.sendMessage(Text.literal("§aKnown Spells:"), false);
                            for (Spell spell : spells) {
                                player.sendMessage(Text.literal("- " + spell.getId().toString()), false);
                            }
                        }
                        return 1;
                    }));
        });
    }
}
