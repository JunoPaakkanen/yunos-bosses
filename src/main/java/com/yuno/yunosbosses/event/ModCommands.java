package com.yuno.yunosbosses.event;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yuno.yunosbosses.binding_vow.BindingVow;
import com.yuno.yunosbosses.binding_vow.BindingVowManager;
import com.yuno.yunosbosses.binding_vow.ModBindingVows;
import com.yuno.yunosbosses.component.BindingVowComponent;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class ModCommands {

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_VOWS = (context, builder) ->
            CommandSource.suggestMatching(ModBindingVows.getNames(), builder);

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

            // Commands for adding mana / setting max mana
            dispatcher.register(CommandManager.literal("mana")
                    .requires(source -> source.hasPermissionLevel(2)) // Require OP/cheats enabled
                    .then(CommandManager.literal("setmax")
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0.0F))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;

                                        float amount = FloatArgumentType.getFloat(context, "amount");
                                        var manaComponent = ModEntityComponents.MANA.get(player);

                                        manaComponent.setMaxMana(amount);

                                        player.sendMessage(Text.literal("§aMaximum mana set to: " + amount), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;

                                        float amount = FloatArgumentType.getFloat(context, "amount");
                                        var manaComponent = ModEntityComponents.MANA.get(player);

                                        manaComponent.addMana(amount);

                                        player.sendMessage(Text.literal("§aAdded " + amount + " mana! Current: " + manaComponent.getMana()), false);
                                        return 1;
                                    }))));

            // Unified Binding Vow Commands
            dispatcher.register(CommandManager.literal("bindingvow")
                    // /bindingvow list
                    .then(CommandManager.literal("list")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                BindingVowComponent component = ModEntityComponents.BINDING_VOWS.get(player);
                                Collection<BindingVow> allVows = ModBindingVows.getAll();

                                player.sendMessage(Text.literal("§6=== Binding Vows ==="), false);
                                for (BindingVow vow : allVows) {
                                    boolean active = component != null && component.hasVow(vow.getId());
                                    String status = active ? "§a[ACTIVE]" : "§7[SEALED]";
                                    player.sendMessage(Text.literal(status + " §f" + vow.getName().getString() + " §7(" + vow.getId().getPath() + ")"), false);
                                    player.sendMessage(Text.literal("   §cSacrifice: §7").append(vow.getSacrifice()), false);
                                    player.sendMessage(Text.literal("   §aGain: §7").append(vow.getGain()), false);
                                }
                                return 1;
                            }))
                    // /bindingvow info <vow>
                    .then(CommandManager.literal("info")
                            .then(CommandManager.argument("vow", StringArgumentType.word())
                                    .suggests(SUGGEST_VOWS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;

                                        String vowName = StringArgumentType.getString(context, "vow");
                                        BindingVow vow = ModBindingVows.get(vowName);
                                        if (vow == null) {
                                            player.sendMessage(Text.literal("§cUnknown binding vow: " + vowName), false);
                                            return 0;
                                        }

                                        player.sendMessage(Text.literal("§6=== " + vow.getName().getString() + " ==="), false);
                                        player.sendMessage(Text.literal("§7").append(vow.getDescription()), false);
                                        player.sendMessage(Text.literal("§cSacrifice: §f").append(vow.getSacrifice()), false);
                                        player.sendMessage(Text.literal("§aGain: §f").append(vow.getGain()), false);
                                        float cost = vow.getActivationManaCost(player);
                                        if (cost > 0) {
                                            player.sendMessage(Text.literal("§bActivation Cost: §f" + (int) cost + " Mana"), false);
                                        }
                                        return 1;
                                    })))
                    // /bindingvow pact <vow>
                    .then(CommandManager.literal("pact")
                            .then(CommandManager.argument("vow", StringArgumentType.word())
                                    .suggests(SUGGEST_VOWS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        String vowName = StringArgumentType.getString(context, "vow");
                                        BindingVow vow = ModBindingVows.get(vowName);
                                        if (vow == null) {
                                            player.sendMessage(Text.literal("§cUnknown binding vow: " + vowName), false);
                                            return 0;
                                        }
                                        return BindingVowManager.activateVow(player, vow.getId()) ? 1 : 0;
                                    })))
                    // /bindingvow break <vow>
                    .then(CommandManager.literal("break")
                            .then(CommandManager.argument("vow", StringArgumentType.word())
                                    .suggests(SUGGEST_VOWS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        String vowName = StringArgumentType.getString(context, "vow");
                                        BindingVow vow = ModBindingVows.get(vowName);
                                        if (vow == null) {
                                            player.sendMessage(Text.literal("§cUnknown binding vow: " + vowName), false);
                                            return 0;
                                        }
                                        return BindingVowManager.revokeVow(player, vow.getId(), true) ? 1 : 0;
                                    })))
                    // Fallback toggle: /bindingvow <vow>
                    .then(CommandManager.argument("vow", StringArgumentType.word())
                            .suggests(SUGGEST_VOWS)
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                String vowName = StringArgumentType.getString(context, "vow");
                                BindingVow vow = ModBindingVows.get(vowName);
                                if (vow == null) {
                                    player.sendMessage(Text.literal("§cUnknown binding vow: " + vowName + ". Use /bindingvow list to view all vows."), false);
                                    return 0;
                                }

                                BindingVowComponent comp = ModEntityComponents.BINDING_VOWS.get(player);
                                if (comp != null && comp.hasVow(vow.getId())) {
                                    return BindingVowManager.revokeVow(player, vow.getId(), true) ? 1 : 0;
                                } else {
                                    return BindingVowManager.activateVow(player, vow.getId()) ? 1 : 0;
                                }
                            })));
        });
    }
}
