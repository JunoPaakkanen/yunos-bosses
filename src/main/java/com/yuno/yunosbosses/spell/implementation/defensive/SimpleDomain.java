package com.yuno.yunosbosses.spell.implementation.defensive;

import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.SimpleDomainManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class SimpleDomain extends Spell {

    public SimpleDomain(Identifier id, SpellRarity rarity) {
        super(id, rarity);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;

        caster.swingHand(Hand.MAIN_HAND, true);

        // Simple Domain: 3.5 block radius, 280 ticks (14 seconds) duration
        SimpleDomainManager.activate(serverWorld, caster, caster.getPos(), 3.5F, 280);

        if (caster instanceof PlayerEntity player) {
            player.sendMessage(Text.literal("§b[Simple Domain] Deployed! Domain sure-hits neutralized while inside stance circle.§r"), true);
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.simple_domain");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        return 50.0F;
    }
}
