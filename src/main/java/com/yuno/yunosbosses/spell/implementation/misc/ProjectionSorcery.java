package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.network.SpawnImagePayload;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ProjectionSorcery extends Spell {
    /*
    Projection Sorcery is a complex spell that by default projects multiple images of the caster's body in a straight line from the caster's position,
    in the caster's look direction. Their trajectory can vary, turning the camera after casting the spell can curve their trajectory in any direction.
    Once the images have been created, the caster has a total of 5 seconds to teleport through all of them.
    Recasting the spell will instantly teleport the caster to the next image. Failing to teleport through all images within the time limit
    will result in the caster being frozen for 2 seconds.
    After teleporting through all the images, you will receive a speed boost.
    Hitting an entity within 0.5 seconds of casting the spell will instead turn the trajectory of the images from a line to a circle, surrounding the hit entity.
    Each time you teleport through these images, you will re-hit that entity (dealing reduced damage per hit).
     */

    public ProjectionSorcery(Identifier id) {
        super(id, true);
    }

    @Override
    public void cast(World world, LivingEntity caster, ItemStack staff) {
        if (world.isClient) return;

        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        if (component.hasAltCastWindow(this)) {
            // Alternative cast is active
            altCast(world, caster, staff);
        }
        else {
            // Default cast
            defaultCast(world, caster, staff);
        }
    }

    public void defaultCast(World world, LivingEntity caster, ItemStack staff) {

        // Projection Sorcery Configuration
        int imageCount = 5;
        int maxTicks = 100; // 5 seconds
        List<Vec3d> imagePositions = new ArrayList<>();

        // Start the alt cast window
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        component.startAltCastWindow(this, maxTicks);

        // Calculate the image world positions and store them
        Vec3d startPos = caster.getPos();
        Vec3d lookDir = caster.getRotationVec(1.0F);
        Vec3d projectionPos = startPos;

        for (int i = 0; i < imageCount; i++) {
            projectionPos = projectionPos.add(lookDir.multiply(1.5));
            imagePositions.add(projectionPos);
            // Send a packet to the client to render the image
            SpawnImagePayload payload = new SpawnImagePayload(caster.getId(), projectionPos, maxTicks);
            for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, projectionPos, 64.0)) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        // Save the images to the caster's spell component memory
        component.setProjectionImages(imagePositions);
        component.setProjectionIndex(0);
    }

    public void altCast(World world, LivingEntity caster, ItemStack staff) {
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        List<Vec3d> images = component.getProjectionImages();
        int currentIndex = component.getProjectionIndex();

        // Safety check to make sure images exist
        if (images.isEmpty() || currentIndex >= images.size()) {
            component.clearAltCastWindow(this);
            return;
        }

        // Get the target position and teleport
        Vec3d targetPos = images.get(currentIndex);

        caster.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
        caster.fallDistance = 0.0F; // Reset fall distance so they don't take damage

        // Advance to the next image
        currentIndex++;
        component.setProjectionIndex(currentIndex);

        // Did the caster just hit the final image?
        if (currentIndex >= images.size()) {

            // Apply the Speed Boost reward
            ModEntityComponents.SPELL_DATA.get(caster).addSpeedStack();

            // Clean up
            component.clearAltCastWindow(this);
            component.setProjectionImages(new ArrayList<>());
            component.setProjectionIndex(0);
        }
    }

    @Override
    public Text getName() {
        return Text.translatable("yunosbosses.spell.projection_sorcery");
    }

    @Override
    public boolean canBeCharged() {
        return false;
    }

    @Override
    public float getManaCost(LivingEntity caster) {
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);
        if (component.hasAltCastWindow(this)) {
            return 10.0F; // Cost for alt cast
        }
        else {
            return 50.0F; // Cost for default cast
        }
    }
}
