package com.yuno.yunosbosses.item;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class ModItemGroups{

    public static final ItemGroup YUNOS_BOSSES = Registry.register(Registries.ITEM_GROUP, Identifier.of(YunosBosses.MOD_ID, "yunos_bosses_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.UBEL_STAFF))
                    .displayName(Text.translatable("itemGroup.yunosbosses.yunos_bosses_items"))
                    .entries((displayContext, entries) -> {
                        // Add all items to the group
                        entries.add(ModItems.BASIC_MAGICAL_STAFF);
                        entries.add(ModItems.UBEL_STAFF);
                        entries.add(ModItems.STRANGE_FRUIT);
                        // Add all blocks to the group
                        entries.add(ModBlocks.DOMAIN_FLOOR.asItem());
                    }).build());

    public static void registerItemGroups() {
        YunosBosses.LOGGER.info("Registering Item Groups for " + YunosBosses.MOD_ID);
    }

}
