package com.yuno.yunosbosses.datagen;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.OnKilledCriterion;
import net.minecraft.advancement.criterion.TickCriterion;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends FabricAdvancementProvider {

    public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {

        // --- THE ROOT ADVANCEMENT ---
        AdvancementEntry rootAdvancement = Advancement.Builder.create()
                .display(
                        Items.NETHER_STAR, // The icon displayed for the tab
                        Text.translatable("advancements.yunosbosses.root.title"), // The Title
                        Text.translatable("advancements.yunosbosses.root.description"), // The Description
                        Identifier.of("textures/gui/advancements/backgrounds/stone.png"), // The background image
                        AdvancementFrame.TASK, // TASK, CHALLENGE, or GOAL
                        false, // Show toast (popup in top right)
                        false, // Announce to chat
                        false  // Hidden in the menu until unlocked
                )
                .criterion("always_unlocked", TickCriterion.Conditions.createTick()) // Criteria for unlocking the advancement
                .build(consumer, "yunosbosses:root");

        // --- UBEL ADVANCEMENT ---
        AdvancementEntry ubelAdvancement = Advancement.Builder.create()
                .parent(rootAdvancement)
                .display(
                        ModItems.UBEL_STAFF.getDefaultStack(),
                        Text.translatable("advancements.yunosbosses.ubel.title"),
                        Text.translatable("advancements.yunosbosses.ubel.description"),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        true,
                        false
                )
                .criterion("defeated_ubel", OnKilledCriterion.Conditions.createPlayerKilledEntity(
                        EntityPredicate.Builder.create().type(ModEntities.UBEL)
                ))
                .build(consumer, "yunosbosses:defeat_ubel");
    }


}
