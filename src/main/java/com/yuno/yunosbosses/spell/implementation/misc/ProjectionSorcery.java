package com.yuno.yunosbosses.spell.implementation.misc;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.component.SpellComponent;
import com.yuno.yunosbosses.network.SpawnImagePayload;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.Spell;
import com.yuno.yunosbosses.spell.SpellRarity;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import com.yuno.yunosbosses.util.WallSlamData;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

    public ProjectionSorcery(Identifier id, SpellRarity rarity) {
        super(id, true, rarity);
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
        SpellComponent component = ModEntityComponents.SPELL_DATA.get(caster);

        // If images are already active/stored, don't spawn a new set
        if (!component.getProjectionImages().isEmpty()) {
            return;
        }

        // Projection Sorcery Configuration
        int imageCount = 5;
        int maxTicks = 100; // 5 seconds
        int intervalTicks = 2; // 0.1s delay between frame spawns (2 ticks)
        int currentStacks = component.getSpeedStacks();
        double baseDistance = 1.5; // 1.5 blocks base distance between frames
        double frameDistance = baseDistance + (currentStacks * 0.25); // Scales up dynamically based on the number of speed stacks

        // Start the alt cast window
        component.startAltCastWindow(this, maxTicks);

        // Reset storage
        List<Vec3d> imagePositions = new ArrayList<>();
        component.setProjectionImages(imagePositions);
        component.setProjectionIndex(0);

        // Mutable references to keep track of the last spawned frame's position and trajectory direction
        final Vec3d[] lastPos = new Vec3d[]{ caster.getPos() };
        final Vec3d[] currentDir = new Vec3d[]{ caster.getRotationVec(1.0F) };
        final Vec3d[] lastCasterLook = new Vec3d[]{ caster.getRotationVec(1.0F) };

        for (int i = 0; i < imageCount; i++) {
            int delayTicks = i * intervalTicks; // 0 ticks, 2 ticks, 4 ticks, 6 ticks, 8 ticks

            DelayedServerEffects.delay(delayTicks, () -> {
                // Ensure the caster is still alive/valid when the task runs
                if (!caster.isAlive() || caster.getWorld().isClient()) return;

                // Fetch current camera orientation at the EXACT moment this tick fires
                Vec3d casterLook = caster.getRotationVec(1.0F);
                Vec3d lookDelta = casterLook.subtract(lastCasterLook[0]);
                lastCasterLook[0] = casterLook;

                // Adjust current direction with the player's camera movement (if any)
                Vec3d travelDir = currentDir[0].add(lookDelta);
                if (travelDir.lengthSquared() > 1.0e-5) {
                    travelDir = travelDir.normalize();
                } else {
                    travelDir = casterLook;
                }

                // Calculate next frame position with collision checking & ricochet
                Vec3d[] outDir = new Vec3d[]{ travelDir };
                Vec3d nextFramePos = calculateFramePosition(world, caster, lastPos[0], travelDir, frameDistance, outDir);

                lastPos[0] = nextFramePos; // Update tracking pointer for the next frame in line
                currentDir[0] = outDir[0]; // Update direction so future frames follow the ricocheted trajectory

                // Save to the component list
                imagePositions.add(nextFramePos);

                // Dispatch packet to nearby clients for rendering
                SpawnImagePayload payload = new SpawnImagePayload(caster.getId(), nextFramePos, maxTicks);
                for (ServerPlayerEntity player : PlayerLookup.around((ServerWorld) world, nextFramePos, 64.0)) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        }
    }

    private Vec3d calculateFramePosition(World world, LivingEntity caster, Vec3d startPos, Vec3d travelDir, double distance, Vec3d[] outDir) {
        Vec3d stepPos = startPos;
        Vec3d dir = travelDir;
        double remainingDist = distance;
        int maxBounces = 3;

        for (int bounce = 0; bounce < maxBounces && remainingDist > 0.001; bounce++) {
            // Raycast starting slightly elevated off current surface to avoid immediate collision with the ground beneath feet
            Vec3d rayStart = stepPos.add(0, 0.1, 0);
            Vec3d rayEnd = rayStart.add(dir.multiply(remainingDist));

            BlockHitResult hit = world.raycast(new RaycastContext(
                    rayStart,
                    rayEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    caster
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d hitPos = hit.getPos();
                Direction side = hit.getSide();
                Vec3d normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());

                double traveled = rayStart.distanceTo(hitPos);
                remainingDist = Math.max(0.0, remainingDist - traveled);

                // Reflect direction off the solid surface normal: r = d - 2*(d . n)*n
                double dot = dir.dotProduct(normal);
                if (dot < 0) {
                    dir = dir.subtract(normal.multiply(2.0 * dot)).normalize();
                } else {
                    dir = normal;
                }

                // Place step position slightly off the hit surface along the normal
                stepPos = hitPos.add(normal.multiply(0.05));
            } else {
                stepPos = stepPos.add(dir.multiply(remainingDist));
                remainingDist = 0;
                break;
            }
        }

        // Safety check: ensure stepPos is not inside a solid block
        BlockPos blockPos = BlockPos.ofFloored(stepPos);
        if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
            stepPos = new Vec3d(stepPos.x, blockPos.getY() + 1.0, stepPos.z);
        }

        outDir[0] = dir;
        return stepPos;
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

        Vec3d targetPos = images.get(currentIndex);

        // Teleport to target position
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

    public static float shatterFrame(LivingEntity entity, DamageSource source, float damage) {
        // Execute only on the server
        if (!entity.getWorld().isClient() && entity.getWorld() instanceof ServerWorld serverWorld) {
            double x = entity.getX();
            double y = entity.getY() + (entity.getHeight() / 2.0); // Center at torso height
            double z = entity.getZ();

            // Play Frame Shatter sound effect (variation depends on damage)
            if (damage > 0.0) {
                serverWorld.playSound(null, x, y, z, ModSounds.FRAME_SHATTER_FROM_DAMAGE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            else {
                serverWorld.playSound(null, x, y, z, ModSounds.FRAME_SHATTER, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }

            // Spawn Frame Shatter particle
            serverWorld.spawnParticles(
                    ModParticles.FRAME_SHATTER_PARTICLE,
                    x, y, z,
                    1,
                    0.1, 0.1, 0.1,
                    0.1
            );

            // Layer bright critical hit stars
            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    x, y, z,
                    20,
                    0.2, 0.4, 0.2,
                    0.15
            );

            // Enable wall slamming damage
            if (entity instanceof WallSlamData data) {
                // During these 30 ticks the target will be able to take damage from slamming into a wall
                data.yunos$setWallSlamTimer(30);
            }

            // Damage the entity 150% if damage triggered frame shatter
            if (damage > 0.0) {
                damage *= 1.5F;
                return damage;
            }
            // Otherwise deal small damage
            return 5.0F;
        }
        return damage;
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
