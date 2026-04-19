package com.yuno.yunosbosses.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

public class ModCommands {
    private static final Map<String, RegistryEntry<StatusEffect>> EFFECTS = Map.of(
            "gojo", ModEffects.GOJO_BINDING_VOW
    );

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_WORDS = (context, builder) ->
            CommandSource.suggestMatching(EFFECTS.keySet(), builder);

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
            dispatcher.register(CommandManager.literal("bindingvow")
                    .then(CommandManager.argument("vow", StringArgumentType.word())
                            .suggests(SUGGEST_WORDS)
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                String typedVow = StringArgumentType.getString(context, "vow").toLowerCase();
                                RegistryEntry<StatusEffect> effect = EFFECTS.get(typedVow);

                                if (effect != null) {
                                    var manaComponent = ModEntityComponents.MANA.get(player);
                                    float bindingVowCost = manaComponent.getMaxMana() / 2;
                                    // Attempt to use the mana
                                    if (manaComponent.useMana(bindingVowCost)) {
                                        // SUCCESS
                                        player.addStatusEffect(new StatusEffectInstance(ModEffects.GOJO_BINDING_VOW, StatusEffectInstance.INFINITE));
                                        player.sendMessage(Text.literal("§bActivated binding vow: " + typedVow + "!"), false);
                                        return 1;
                                    } else {
                                        // FAILURE (Not enough mana)
                                        player.sendMessage(Text.literal("§cNot enough mana! You need 50% of your max mana to activate the binding vow."), false);
                                        return 0;
                                    }
                                } else {
                                    return 0;
                                }
                            })));
        });
    }
}
