package com.yuno.yunosbosses.item;

import com.yuno.yunosbosses.YunosBosses;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // Items
    public static final Item BASIC_MAGICAL_STAFF = registerItem("basic_magical_staff", new Item(new Item.Settings()));

    // Helper method to register item
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(YunosBosses.MOD_ID, name), item);
    }
    public static void registerModItems() {
        YunosBosses.LOGGER.info("Registering Mod Items for " + YunosBosses.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(BASIC_MAGICAL_STAFF);
        });
    }
}
