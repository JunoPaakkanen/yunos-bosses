package com.yuno.yunosbosses.item;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.item.custom.StrangeFruitItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
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
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(UBEL_STAFF);
        });
        // FOOD ITEMS
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(STRANGE_FRUIT);
        });
    }
}
