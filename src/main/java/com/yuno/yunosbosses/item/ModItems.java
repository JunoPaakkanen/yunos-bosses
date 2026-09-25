package com.yuno.yunosbosses.item;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.item.custom.StrangeFruitItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    // Components
    public static final FoodComponent STRANGE_FRUIT_COMPONENT = new FoodComponent.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build();

    // Items
    public static final Item STRANGE_FRUIT = registerItem("strange_fruit", StrangeFruitItem::new, new Item.Settings().food(STRANGE_FRUIT_COMPONENT));

    // Staff Items
    public static final Item BASIC_MAGICAL_STAFF = registerItem("basic_magical_staff", settings -> new StaffItem(settings, 1), new Item.Settings().maxCount(1));
    public static final Item UBEL_STAFF = registerItem("ubel_staff", settings -> new StaffItem(settings, 1.5F), new Item.Settings().maxCount(1));
    public static final Item METHODE_STAFF = registerItem("methode_staff", settings -> new StaffItem(settings, 2.0F), new Item.Settings().maxCount(1));

    // Spawn Eggs
    public static final Item UBEL_SPAWN_EGG = registerItem("ubel_spawn_egg", settings -> new SpawnEggItem(ModEntities.UBEL, settings), new Item.Settings());
    public static final Item METHODE_SPAWN_EGG = registerItem("methode_spawn_egg", settings -> new SpawnEggItem(ModEntities.METHODE, settings), new Item.Settings());
    public static final Item USELESS_CHICKEN_SPAWN_EGG = registerItem("useless_chicken_spawn_egg", settings -> new SpawnEggItem(ModEntities.USELESS_CHICKEN, settings), new Item.Settings());

    // Helper method to register item
    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(YunosBosses.MOD_ID, name));
        T item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        YunosBosses.LOGGER.info("Registering Mod Items for " + YunosBosses.MOD_ID);

        // COMBAT ITEMS
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(BASIC_MAGICAL_STAFF);
            entries.add(UBEL_STAFF);
            entries.add(METHODE_STAFF);
        });

        // SPAWN EGGS
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(UBEL_SPAWN_EGG);
            entries.add(METHODE_SPAWN_EGG);
            entries.add(USELESS_CHICKEN_SPAWN_EGG);
        });

        // FOOD ITEMS
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(STRANGE_FRUIT);
        });
    }
}
