package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.implementation.misc.ProjectionSorcery;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void yunosbosses$increaseMeterOnFullChargeAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if the attack cooldown is fully charged
        if (player.getAttackCooldownProgress(0.5F) >= 0.9F) {
            var component = ModEntityComponents.SPELL_DATA.get(player);
            Spell[] equippedSpells = component.getEquippedSpells();
            if (Arrays.asList(equippedSpells).contains(ModSpells.PROJECTION_SORCERY)) {
                component.addFrameMeter(25);
            }
        }
    }
}