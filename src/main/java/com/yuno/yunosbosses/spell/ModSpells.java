package com.yuno.yunosbosses.spell;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.spell.implementation.defensive.DefensiveMagic;
import com.yuno.yunosbosses.spell.implementation.offensive.KillingMagic;
import com.yuno.yunosbosses.spell.implementation.offensive.KillingMagicBarrage;
import com.yuno.yunosbosses.spell.implementation.summon.SummonSpell;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ModSpells {
    public static final Map<Identifier, Spell> SPELLS = new HashMap<>();

    // Summon spells
    public static final Spell SUMMON_SPELL = registerSpell(new SummonSpell(Identifier.of("yunosbosses", "summonspell")));
    // Offensive spells
    public static final Spell KILLING_MAGIC = registerSpell(new KillingMagic(Identifier.of("yunosbosses", "killingmagic")));
    public static final Spell KILLING_MAGIC_BARRAGE = registerSpell(new KillingMagicBarrage(Identifier.of("yunosbosses", "killingmagicbarrage")));
    // Defensive spells
    public static final Spell DEFENSIVE_MAGIC = registerSpell(new DefensiveMagic(Identifier.of("yunosbosses", "defensivemagic")));

    private static Spell registerSpell(Spell spell) {
        SPELLS.put(spell.getId(), spell);
        return spell;
    }

    public static Spell getSpell(Identifier id) {
        return SPELLS.getOrDefault(id, null);
    }

    public static void registerModSpells() {
        YunosBosses.LOGGER.info("Registering Mod Spells for " + YunosBosses.MOD_ID);
    }

    public static Spell getSpellByName(String name) {
        Identifier id = Identifier.of("yunosbosses", name);
        return SPELLS.get(id);
    }
}
