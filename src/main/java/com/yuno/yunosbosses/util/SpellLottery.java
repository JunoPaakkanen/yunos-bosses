package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SpellLottery {

    // Map for Spell rarity weights
    private static final Map<SpellRarity, Integer> RARITY_WEIGHTS = new EnumMap<>(SpellRarity.class);

    static {
        RARITY_WEIGHTS.put(SpellRarity.COMMON, 60);
        RARITY_WEIGHTS.put(SpellRarity.UNCOMMON, 25);
        RARITY_WEIGHTS.put(SpellRarity.RARE, 10);
        RARITY_WEIGHTS.put(SpellRarity.EPIC, 4);
        RARITY_WEIGHTS.put(SpellRarity.LEGENDARY, 1);
        RARITY_WEIGHTS.put(SpellRarity.SPECIAL, 0); // Special techniques don't drop randomly
    }

    // Get a random spell of any rarity, equal odds
    public static Spell getRandomSpell(Random random) {
        List<Spell> spells = new ArrayList<>(ModSpells.SPELLS.values());

        if (spells.isEmpty()) {return null;}

        int randomIndex = random.nextInt(spells.size());
        return spells.get(randomIndex);
    }

    // Get a random spell of a specific rarity
    public static Spell getRandomSpell(Random random, SpellRarity rarity) {
        List<Spell> spells = new ArrayList<>();

        for (Spell spell : ModSpells.SPELLS.values()) {
            if (spell.getRarity() == rarity) {
                spells.add(spell);
            }
        }

        if (spells.isEmpty()) {return null;}

        int randomIndex = random.nextInt(spells.size());
        return spells.get(randomIndex);
    }

    // Get a random spell rarity, odds based on weights
    public static SpellRarity getRandomSpellRarity(Random random) {
        int totalWeight = 0;
        for (int weight : RARITY_WEIGHTS.values()) {
            totalWeight += weight;
        }

        SpellRarity spellRarityToPick = SpellRarity.COMMON;

        int roll = random.nextInt(totalWeight);
        int currentSum = 0;

        for (Map.Entry<SpellRarity, Integer> entry : RARITY_WEIGHTS.entrySet()) {
            currentSum += entry.getValue();
            if (roll < currentSum) {
                spellRarityToPick = entry.getKey();
                break;
            }
        }
        return spellRarityToPick;
    }
}
