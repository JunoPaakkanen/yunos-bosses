package com.yuno.yunosbosses.util;

import com.yuno.yunosbosses.binding_vow.BindingVowManager;
import com.yuno.yunosbosses.binding_vow.BloodForManaBindingVow;
import com.yuno.yunosbosses.binding_vow.VexLifeLeechBindingVow;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.domain.clash.DomainClashManager;
import com.yuno.yunosbosses.effect.AntiTeleportEffect;
import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.item.custom.StaffItem;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.implementation.misc.DomainExpansion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SpellCastHelper {

    /**
     * Attempts to consume mana, or if the Blood for Mana binding vow is active,
     * drains available mana and converts any remaining mana deficit directly into player HP damage.
     * @return true if consumption (mana, health, or combined) succeeded.
     */
    public static boolean consumeManaOrHealth(PlayerEntity player, float manaCost) {
        return consumeManaOrHealth(player, manaCost, false);
    }

    /**
     * Attempts to consume mana, or if the Blood for Mana binding vow is active,
     * drains available mana and converts any remaining mana deficit directly into player HP damage.
     * @param isContinuousDrain whether this is continuous charging drain, which tones down particles so sightline stays clear
     * @return true if consumption (mana, health, or combined) succeeded.
     */
    public static boolean consumeManaOrHealth(PlayerEntity player, float manaCost, boolean isContinuousDrain) {
        if (manaCost <= 0.0F) return true;

        var manaComponent = ModEntityComponents.MANA.get(player);
        float currentMana = manaComponent.getMana();

        if (currentMana >= manaCost) {
            return manaComponent.useMana(manaCost);
        }

        // Check if player has Blood for Mana vow
        if (BindingVowManager.hasVow(player, BloodForManaBindingVow.ID)) {
            float deficit = manaCost - currentMana;
            float hpCost = deficit / BloodForManaBindingVow.MANA_PER_HP;

            // Player must have more health than hpCost to prevent accidental suicide or allow casting down to death
            // In Minecraft, damage will kill if health <= hpCost.
            // Consume whatever mana they have
            if (currentMana > 0) {
                manaComponent.setMana(0);
            }

            // Damage player using outOfWorld/generic damage so it bypasses armor and resistances
            if (!player.isCreative() && player.getWorld() instanceof ServerWorld serverWorld) {
                player.timeUntilRegen = 0;
                player.damage(serverWorld, player.getDamageSources().outOfWorld(), hpCost);

                boolean continuous = isContinuousDrain || (player.isUsingItem() && player.getActiveItem().getItem() instanceof StaffItem);

                if (continuous) {
                    // Spawn 2 subtle particles around waist level
                    serverWorld.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 0.4, player.getZ(), 2, 0.2, 0.1, 0.2, 0.05);
                    serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.6F, 0.8F);
                } else {
                    // One-off burst for instant casts
                    serverWorld.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 0.7, player.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
                    serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.8F, 0.8F);
                }
            }

            return player.isAlive();
        }

        return false;
    }

    // Check if the player has enough mana to start casting the spell
    public static boolean canStartCasting(Spell spell, LivingEntity caster) {
        if (caster == null || caster.hasStatusEffect(ModEffects.FRAME_FREEZE)) {
            return false;
        }

        if (spell == ModSpells.PROJECTION_SORCERY && caster.hasStatusEffect(ModEffects.ANTI_TELEPORT)) {
            AntiTeleportEffect.onTeleportBlocked(caster);
            return false;
        }

        if (!(caster instanceof PlayerEntity player)) {
            if (spell instanceof DomainExpansion && DomainClashManager.hasBurnout(caster.getUuid())) {
                return false;
            }
            return true;
        }

        // Vex Binding Vow check: If player has VexLifeLeechBindingVow active, cannot cast Summon Vex above 50% HP
        if (spell == ModSpells.SUMMON_VEX_SPELL && BindingVowManager.hasVow(player, VexLifeLeechBindingVow.ID)) {
            if (player.getHealth() > (player.getMaxHealth() * 0.5F)) {
                player.sendMessage(Text.literal("\u00a7c[Binding Vow] You cannot cast Summon Vex while above 50% HP!\u00a7r"), true);
                return false;
            }
        }

        if (spell instanceof DomainExpansion && DomainClashManager.hasBurnout(player.getUuid())) {
            int sec = DomainClashManager.getBurnoutSeconds(player.getUuid());
            player.sendMessage(Text.literal("\u00a7cCannot expand domain! Technique Burnout (" + sec + "s remaining)\u00a7r"), true);
            return false;
        }

        var spellComponent = ModEntityComponents.SPELL_DATA.get(player);
        if (spell instanceof DomainExpansion && BarrierManager.hasActiveDomain(player.getUuid()) && !spellComponent.hasAltCastWindow(spell)) {
            return false;
        }

        if (spell == ModSpells.SHRINE && spellComponent.getShrineCooldown() > 0) {
            return false;
        }

        // Do not consume mana if Projection Sorcery is in its alt-cast window but the next frame hasn't spawned yet
        if (spell == ModSpells.PROJECTION_SORCERY && spellComponent.hasAltCastWindow(spell)) {
            if (spellComponent.getProjectionImages().isEmpty()
                    || spellComponent.getProjectionIndex() >= spellComponent.getProjectionImages().size()) {
                return false;
            }
        }

        float baseManaCost = spell.getManaCost(player);

        // Reverse Cursed Technique cannot be cast using health via Blood for Mana
        if (spell == ModSpells.REVERSE_CURSED_TECHNIQUE) {
            return ModEntityComponents.MANA.get(player).useMana(baseManaCost);
        }

        return consumeManaOrHealth(player, baseManaCost);
    }

    // Execute the spell with the given charge level
    public static void castChargedSpell(Spell spell, World world, LivingEntity caster, ItemStack staff, int chargeLevel) {
        if (world.isClient || caster == null) return;

        spell.cast(world, caster, staff, chargeLevel);
    }

    // Used for immediate casting of spells
    public static boolean tryCastSpell(Spell spell, World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient || caster == null || caster.hasStatusEffect(ModEffects.FRAME_FREEZE)) return false;

        if (canStartCasting(spell, caster)) {
            // Force a Level 1 cast instantly
            castChargedSpell(spell, world, caster, staff, 1);
            return true;
        }
        return false;
    }
}
