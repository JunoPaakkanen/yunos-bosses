package com.yuno.yunosbosses.item;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.item.custom.StaffItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // Components
    public static final FoodComponent STRANGE_FRUIT_COMPONENT = new FoodComponent.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build();

    // Items
    public static final Item STRANGE_FRUIT = registerItem("strange_fruit", new Item(new Item.Settings().food(STRANGE_FRUIT_COMPONENT)));

    // Staff Items
    public static final Item BASIC_MAGICAL_STAFF = registerItem("basic_magical_staff", new StaffItem(new Item.Settings().maxCount(1)));

    // Helper method to register item
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(YunosBosses.MOD_ID, name), item);
    }
    public static void registerModItems() {
        YunosBosses.LOGGER.info("Registering Mod Items for " + YunosBosses.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(BASIC_MAGICAL_STAFF);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(STRANGE_FRUIT);
        });
    }
}
