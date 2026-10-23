package com.yuno.yunosbosses.item.custom;

import net.minecraft.item.equipment.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public class ModArmorMaterials {
    public static final RegistryKey<EquipmentAsset> KIMONO_KEY =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("yunosbosses", "kimono"));

    // Armor material definitions
    public static final ArmorMaterial KIMONO_ARMOR_MATERIAL = new ArmorMaterial(
            750,
            new EnumMap(
                    Map.of(
                            EquipmentType.BOOTS,
                            2,
                            EquipmentType.LEGGINGS,
                            5,
                            EquipmentType.CHESTPLATE,
                            6,
                            EquipmentType.HELMET,
                            2,
                            EquipmentType.BODY,
                            0
                    )
            ),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            0.0F,
            0.0F,
            ItemTags.CHEST_ARMOR,
            KIMONO_KEY
    );
}
