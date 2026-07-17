package com.yuno.yunosbosses.spell;

import com.yuno.yunosbosses.YunosBosses;
import com.yuno.yunosbosses.spell.implementation.defensive.DefensiveMagic;
import com.yuno.yunosbosses.spell.implementation.defensive.SphereBarrierSpell;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansionShrine;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import com.yuno.yunosbosses.spell.implementation.misc.ReverseCursedTechnique;
import com.yuno.yunosbosses.spell.implementation.offensive.CuttingMagicReelseiden;
import com.yuno.yunosbosses.spell.implementation.offensive.Dismantle;
import com.yuno.yunosbosses.spell.implementation.offensive.KillingMagic;
import com.yuno.yunosbosses.spell.implementation.offensive.KillingMagicBarrage;
import com.yuno.yunosbosses.spell.implementation.summon.SummonUselessChicken;
import com.yuno.yunosbosses.spell.implementation.summon.SummonVexSpell;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ModSpells {
    public static final Map<Identifier, Spell> SPELLS = new HashMap<>();

    // Summon spells
    public static final Spell SUMMON_VEX_SPELL = registerSpell(new SummonVexSpell(Identifier.of("yunosbosses", "summonvexspell")));
    public static final Spell SUMMON_USELESS_CHICKEN = registerSpell(new SummonUselessChicken(Identifier.of("yunosbosses", "summonuselesschicken"))); // STAFFLESS

    // Offensive spells
    public static final Spell KILLING_MAGIC = registerSpell(new KillingMagic(Identifier.of("yunosbosses", "killingmagic")));
    public static final Spell KILLING_MAGIC_BARRAGE = registerSpell(new KillingMagicBarrage(Identifier.of("yunosbosses", "killingmagicbarrage")));
    public static final Spell CUTTING_MAGIC_REELSEIDEN = registerSpell(new CuttingMagicReelseiden(Identifier.of("yunosbosses", "cuttingmagicreelseiden")));
    public static final Spell DISMANTLE = registerSpell(new Dismantle(Identifier.of("yunosbosses", "dismantle")));

    // Defensive spells
    public static final Spell DEFENSIVE_MAGIC = registerSpell(new DefensiveMagic(Identifier.of("yunosbosses", "defensivemagic")));
    public static final Spell SPHERE_BARRIER = registerSpell(new SphereBarrierSpell(Identifier.of("yunosbosses", "spherebarrier")));

    // Miscellaneous spells
    public static final Spell REVERSE_CURSED_TECHNIQUE = registerSpell(new ReverseCursedTechnique(Identifier.of("yunosbosses", "reversecursedtechnique")));
    public static final Spell DOMAIN_EXPANSION_SHRINE = registerSpell(new DomainExpansionShrine(Identifier.of("yunosbosses", "domainexpansionshrine")));
    public static final Spell PROJECTION_SORCERY = registerSpell(new ProjectionSorcery(Identifier.of("yunosbosses", "projectionsorcery")));

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
