package com.yuno.yunosbosses.block;

import com.yuno.yunosbosses.YunosBosses;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block DOMAIN_FLOOR = registerBlock("domain_floor", new DomainFloorBlock(AbstractBlock.Settings.create()
            .strength(-1.0f, 3600000.0f)
            .dropsNothing()
            .nonOpaque()
            .luminance(state -> 9)
            .sounds(BlockSoundGroup.MUD)
    ));

    private static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.of("yunosbosses", name);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        YunosBosses.LOGGER.info("Registering Mod Blocks for " + YunosBosses.MOD_ID);
    }
}
