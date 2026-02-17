package com.yuno.yunosbosses.spell;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.spell.implementation.SummonSpell;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ModSpells {
    public static final Map<Identifier, Spell> SPELLS = new HashMap<>();

    public static final Spell SUMMON_SPELL = registerSpell(new SummonSpell(Identifier.of("yunosbosses", "summonspell")));

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
}
