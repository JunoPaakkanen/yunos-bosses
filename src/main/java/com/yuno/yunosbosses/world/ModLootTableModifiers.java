package com.yuno.yunosbosses.world;

import com.yuno.yunosbosses.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;

import java.util.Map;

/**
 * Handles injecting Strange Fruit into ancient and mystical dungeon structure chests.
 */
public class ModLootTableModifiers {

    // Structure Chest Loot Table -> Chance to generate a Strange Fruit (0.0 to 1.0)
    private static final Map<RegistryKey<?>, Float> DUNGEON_CHEST_CHANCES = Map.ofEntries(
            // Ancient City (deep cursed energy, high chance)
            Map.entry(LootTables.ANCIENT_CITY_CHEST, 0.35F),
            Map.entry(LootTables.ANCIENT_CITY_ICE_BOX_CHEST, 0.25F),

            // Woodland Mansion (mystical dark sorcery)
            Map.entry(LootTables.WOODLAND_MANSION_CHEST, 0.30F),

            // Stronghold (end dimension nexus)
            Map.entry(LootTables.STRONGHOLD_LIBRARY_CHEST, 0.35F),
            Map.entry(LootTables.STRONGHOLD_CORRIDOR_CHEST, 0.20F),
            Map.entry(LootTables.STRONGHOLD_CROSSING_CHEST, 0.20F),

            // Trial Chambers (high-tier combat vault rewards)
            Map.entry(LootTables.TRIAL_CHAMBERS_REWARD_RARE_CHEST, 0.25F),
            Map.entry(LootTables.TRIAL_CHAMBERS_REWARD_UNIQUE_CHEST, 0.40F),
            Map.entry(LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE_CHEST, 0.35F),
            Map.entry(LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST, 0.50F),

            // Nether Fortress & Bastion Remnants (otherworldly fire & soul magic)
            Map.entry(LootTables.NETHER_BRIDGE_CHEST, 0.20F),
            Map.entry(LootTables.BASTION_TREASURE_CHEST, 0.30F),

            // End City (endgame dimension treasure)
            Map.entry(LootTables.END_CITY_TREASURE_CHEST, 0.30F)
    );

    public static void registerLootTableModifiers() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Check if this loot table is one of our target structure chests
            Float chance = DUNGEON_CHEST_CHANCES.get(key);

            if (chance != null && source.isBuiltin()) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .conditionally(RandomChanceLootCondition.builder(chance))
                        .with(ItemEntry.builder(ModItems.STRANGE_FRUIT));

                tableBuilder.pool(poolBuilder);
            }
        });
    }
}
