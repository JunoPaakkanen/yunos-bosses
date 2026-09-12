package com.yuno.yunosbosses.entity.projectile;

import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.entity.other.DomainShrineEntity;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.util.ActiveBarrier;
import com.yuno.yunosbosses.util.BarrierManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class FlameArrowEntity extends ProjectileEntity {
    private static final TrackedData<Float> POTENCY = DataTracker.registerData(FlameArrowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    public static final double MAX_RANGE = 100.0; // Strictly max 100 blocks travel distance

    private boolean hasDetonated = false;
    private Vec3d startPos = null;

    public FlameArrowEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    public FlameArrowEntity(EntityType<? extends ProjectileEntity> entityType, World world, LivingEntity owner, float potency) {
        super(entityType, world);
        this.setOwner(owner);
        this.setPotency(potency);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(POTENCY, 1.0f);
    }

    public float getPotency() {
        return this.dataTracker.get(POTENCY);
    }

    public void setPotency(float potency) {
        this.dataTracker.set(POTENCY, potency);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.hasDetonated) return;

        // Record spawn/start position for range calculation
        if (this.startPos == null) {
            this.startPos = this.getPos();
        }

        // 1. Check Max Range (100 blocks max) or lifetime: detonate if reached
        double traveled = this.getPos().distanceTo(this.startPos);
        if (traveled >= MAX_RANGE || this.age > 40) {
            if (!this.getWorld().isClient()) {
                detonate(this.getPos());
            } else {
                this.discard();
            }
            return;
        }

        Vec3d prevPos = this.getPos();
        Vec3d velocity = this.getVelocity();
        Vec3d newPos = prevPos.add(velocity);

        // Check if next step would exceed max range
        if (newPos.distanceTo(this.startPos) > MAX_RANGE) {
            Vec3d dir = velocity.normalize();
            double remaining = Math.max(0.0, MAX_RANGE - traveled);
            Vec3d clampPos = prevPos.add(dir.multiply(remaining));
            this.setPosition(clampPos.x, clampPos.y, clampPos.z);
            if (!this.getWorld().isClient()) {
                detonate(clampPos);
            } else {
                this.discard();
            }
            return;
        }

        // Only server executes collision & detonation logic
        if (!this.getWorld().isClient()) {
            // 2. Barrier Interception Check (Defensive Magic: Hex Shields and Spheres)
            // Defensive magic intercepts the flame arrow and causes it to explode on impact instead of deflecting
            Vec3d barrierHitPos = checkBarrierCollision(prevPos, newPos);
            if (barrierHitPos != null) {
                this.getWorld().playSound(null, barrierHitPos.x, barrierHitPos.y, barrierHitPos.z,
                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 1.2F);
                detonate(barrierHitPos);
                return;
            }

            // 3. Solid Block Collision Check along trajectory
            BlockHitResult blockHit = this.getWorld().raycast(new RaycastContext(
                    prevPos, newPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this
            ));

            Vec3d maxTraveled = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getPos() : newPos;

            // 4. Swept Entity Collision Check along path (generous swept hitbox so it never clips past entities)
            EntityHitResult entityHit = findEntityCollision(prevPos, maxTraveled, velocity);

            if (entityHit != null) {
                this.onEntityHit(entityHit);
                return;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onBlockHit(blockHit);
                return;
            }
        }

        // 5. Update Position & Orientation (using setPosition so bounding box & chunk tracking update properly)
        this.setPosition(newPos.x, newPos.y, newPos.z);

        double hDist = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        this.setYaw((float) Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        this.setPitch((float) Math.toDegrees(Math.atan2(velocity.y, hDist)));

        // Spawn forced particle effects in flight (visible up to 512 blocks away)
        spawnFlightParticles(prevPos, newPos, velocity);

        // Play periodic rushing flame sound
        if (this.age % 3 == 0) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5f, 1.4f + (this.random.nextFloat() * 0.3f));
        }
    }

    /**
     * Checks if the trajectory from prevPos to newPos collides with any active barrier.
     * Returns the exact hit position if intercepted, or null if clear.
     */
    private Vec3d checkBarrierCollision(Vec3d from, Vec3d to) {
        Entity owner = this.getOwner();

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);

            // Allow the caster to safely shoot through their own directional Hex Shields
            if (!isSphere && owner != null && barrier.getOwnerUuid().equals(owner.getUuid())) {
                continue;
            }

            if (isSphere) {
                Vec3d center = barrier.getPosition();
                double radius = barrier.getRadius();
                double d1 = from.distanceTo(center);
                double d2 = to.distanceTo(center);

                // Check if the segment crosses the sphere surface
                if ((d1 > radius && d2 <= radius) || (d1 < radius && d2 >= radius) ||
                        (d2 >= radius - 0.5 && d2 <= radius + 0.5)) {
                    Vec3d dir = to.subtract(center).normalize();
                    return center.add(dir.multiply(radius));
                }
            } else {
                // Hex Shield: Directional barrier
                Box hexBox = Box.from(barrier.getPosition()).expand(0.9);
                if (hexBox.contains(from)) {
                    return from;
                }
                Optional<Vec3d> rayHit = hexBox.raycast(from, to);
                if (rayHit.isPresent()) {
                    return rayHit.get();
                }
            }
        }
        return null;
    }

    /**
     * Performs a generous swept entity collision check between from and to positions.
     */
    private EntityHitResult findEntityCollision(Vec3d from, Vec3d to, Vec3d velocity) {
        Box sweepBox = new Box(
                Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z),
                Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z)
        ).expand(1.5);

        List<Entity> candidates = this.getWorld().getOtherEntities(this, sweepBox, this::canHit);

        Entity closestEntity = null;
        Vec3d closestPoint = null;
        double closestDistSq = Double.MAX_VALUE;

        Vec3d segment = to.subtract(from);
        double segLenSq = segment.lengthSquared();

        for (Entity candidate : candidates) {
            Box candidateBox = candidate.getBoundingBox().expand(0.40);

            // 1. Point-blank check: If projectile already starts inside candidate's hitbox
            if (candidateBox.contains(from)) {
                return new EntityHitResult(candidate, from);
            }

            // 2. Standard box raycast along flight segment
            Optional<Vec3d> hit = candidateBox.raycast(from, to);
            if (hit.isPresent()) {
                double distSq = from.squaredDistanceTo(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntity = candidate;
                    closestPoint = hit.get();
                }
            } else if (segLenSq > 1e-6) {
                // 3. High-speed continuous swept segment-to-point proximity check
                // Immune to discrete box edge misses or tunneling
                Vec3d center = candidate.getBoundingBox().getCenter();
                double t = Math.max(0.0, Math.min(1.0, center.subtract(from).dotProduct(segment) / segLenSq));
                Vec3d proj = from.add(segment.multiply(t));

                double hitRadius = Math.max(candidate.getWidth(), candidate.getHeight()) * 0.5 + 0.40;
                if (candidateBox.contains(proj) || proj.distanceTo(center) <= hitRadius) {
                    double distSq = from.squaredDistanceTo(proj);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestEntity = candidate;
                        closestPoint = proj;
                    }
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestPoint != null ? closestPoint : closestEntity.getBoundingBox().getCenter());
        }
        return null;
    }

    private void spawnFlightParticles(Vec3d prevPos, Vec3d newPos, Vec3d velocity) {
        World world = this.getWorld();
        Vec3d dir = velocity.normalize();

        // Local perpendicular vectors for spiral trail
        Vec3d globalUp = new Vec3d(0, 1, 0);
        Vec3d rightDir = Math.abs(dir.y) > 0.99 ? new Vec3d(1, 0, 0) : dir.crossProduct(globalUp).normalize();
        Vec3d upDir = rightDir.crossProduct(dir).normalize();

        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double fraction = (double) i / steps;
            Vec3d pt = prevPos.lerp(newPos, fraction);

            if (world instanceof ServerWorld serverWorld) {
                // Use forced particles so distant observers can see the arrow in flight
                spawnForcedParticles(serverWorld, ParticleTypes.FLAME, pt.x, pt.y, pt.z, 2, 0.04, 0.04, 0.04, 0.01);
                spawnForcedParticles(serverWorld, ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.005);
                spawnForcedParticles(serverWorld, ModParticles.FLAME_EMBER_PARTICLE, pt.x, pt.y, pt.z, 1,
                        -dir.x * 0.15, -dir.y * 0.15, -dir.z * 0.15, 0.08);

                // Lava spark burst
                if (this.random.nextFloat() < 0.25f) {
                    spawnForcedParticles(serverWorld, ParticleTypes.LAVA, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.0);
                }

                // Fiery corkscrew spiral ribbon around the arrow trajectory
                double spiralAngle = (this.age * 1.1) + (fraction * Math.PI * 2.0);
                double spiralRadius = 0.35 + (getPotency() * 0.06);
                Vec3d spiralOffset = rightDir.multiply(Math.cos(spiralAngle) * spiralRadius)
                        .add(upDir.multiply(Math.sin(spiralAngle) * spiralRadius));
                Vec3d spiralPt = pt.add(spiralOffset);
                spawnForcedParticles(serverWorld, ParticleTypes.SMALL_FLAME, spiralPt.x, spiralPt.y, spiralPt.z, 1, 0.01, 0.01, 0.01, 0.005);
            } else {
                world.addParticleClient(ParticleTypes.FLAME, pt.x, pt.y, pt.z, 0.0, 0.0, 0.0);
            }
        }

        // Periodic shockwave ring in flight
        if (this.age % 2 == 0 && world instanceof ServerWorld serverWorld) {
            spawnForcedParticles(serverWorld, ModParticles.FLAME_SHOCKWAVE_PARTICLE, newPos.x, newPos.y, newPos.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (!this.getWorld().isClient()) {
            detonate(entityHitResult.getPos());
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!this.getWorld().isClient()) {
            detonate(blockHitResult.getPos());
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient() && !this.hasDetonated) {
            detonate(hitResult.getPos());
        }
    }

    public void detonate(Vec3d hitPos) {
        if (this.hasDetonated) return;
        this.hasDetonated = true;

        if (this.getWorld().isClient()) {
            this.discard();
            return;
        }

        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        float potency = getPotency();

        // 1. Audio: Layered apocalyptic explosion sounds with high volume (volume 12.0 = 192 block reach!)
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 12.0f, 0.5f);
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 10.0f, 0.45f);
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 10.0f, 0.6f);
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 8.0f, 0.75f);

        // 2. Base Explosion Dome: Forced particle emission visible up to 512 blocks
        spawnForcedParticles(serverWorld, ParticleTypes.EXPLOSION_EMITTER, hitPos.x, hitPos.y + 0.5, hitPos.z, 6, 1.5, 0.5, 1.5, 0.0);
        spawnForcedParticles(serverWorld, ParticleTypes.EXPLOSION_EMITTER, hitPos.x, hitPos.y + 3.5, hitPos.z, 4, 2.0, 1.0, 2.0, 0.0);
        spawnForcedParticles(serverWorld, ModParticles.FLAME_EXPLOSION_PARTICLE, hitPos.x, hitPos.y + 1.0, hitPos.z, 16, 2.5, 1.0, 2.5, 0.12);

        // 3. Colossal Vertical Pillar of Fire & Rolling Clouds (rising up to 24-32 blocks!)
        double pillarHeight = 22.0 + (potency * 6.0);
        for (double yOffset = 0; yOffset <= pillarHeight; yOffset += 0.6) {
            double baseRadius = 1.4 + (potency * 0.5);
            // Top mushroom expansion
            double expansion = Math.max(0, (yOffset - 12.0) / (pillarHeight - 12.0)) * 3.8;
            double radius = baseRadius + expansion;

            int count = Math.max(8, (int) (radius * 7));
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI * i) / count;
                double px = hitPos.x + Math.cos(angle) * radius * (0.8 + serverWorld.random.nextDouble() * 0.4);
                double pz = hitPos.z + Math.sin(angle) * radius * (0.8 + serverWorld.random.nextDouble() * 0.4);

                spawnForcedParticles(serverWorld, ParticleTypes.FLAME, px, hitPos.y + yOffset, pz, 1, 0, 0.4, 0, 0.08);

                if (serverWorld.random.nextFloat() < 0.25f) {
                    spawnForcedParticles(serverWorld, ParticleTypes.LAVA, px, hitPos.y + yOffset, pz, 1, 0, 0.15, 0, 0.02);
                }
            }

            // Custom animated explosion clouds climbing along the pillar
            if (yOffset % 1.8 < 0.6) {
                spawnForcedParticles(serverWorld, ModParticles.FLAME_EXPLOSION_PARTICLE, hitPos.x, hitPos.y + yOffset, hitPos.z, 2, radius * 0.4, 0.3, radius * 0.4, 0.08);
            }

            // Smoke core inside the pillar
            spawnForcedParticles(serverWorld, ParticleTypes.CAMPFIRE_COSY_SMOKE, hitPos.x, hitPos.y + yOffset, hitPos.z, 2, 0.6, 0.2, 0.6, 0.06);
        }

        // Mushroom head plume burst at the summit
        spawnForcedParticles(serverWorld, ModParticles.FLAME_EXPLOSION_PARTICLE, hitPos.x, hitPos.y + pillarHeight, hitPos.z, 16, 3.0, 1.2, 3.0, 0.15);
        spawnForcedParticles(serverWorld, ModParticles.FLAME_SHOCKWAVE_PARTICLE, hitPos.x, hitPos.y + pillarHeight, hitPos.z, 4, 1.2, 0.2, 1.2, 0.0);

        // 4. Expanding Horizontal Fiery Shockwave Rings
        spawnForcedParticles(serverWorld, ModParticles.FLAME_SHOCKWAVE_PARTICLE, hitPos.x, hitPos.y + 0.3, hitPos.z, 6, 1.0, 0.1, 1.0, 0.08);

        float maxShockwaveRadius = 7.0f + (potency * 4.0f);
        for (double r = 1.5; r <= maxShockwaveRadius; r += 1.5) {
            int ringPoints = (int) (r * 14);
            for (int i = 0; i < ringPoints; i++) {
                double angle = (2 * Math.PI * i) / ringPoints;
                double rx = Math.cos(angle);
                double rz = Math.sin(angle);
                double px = hitPos.x + rx * r;
                double pz = hitPos.z + rz * r;

                // Outward expanding flame particles
                spawnForcedParticles(serverWorld, ParticleTypes.FLAME, px, hitPos.y + 0.3, pz, 0, rx * 0.5, 0.08, rz * 0.5, 0.55);
                if (r > 3.0 && i % 4 == 0) {
                    spawnForcedParticles(serverWorld, ParticleTypes.LAVA, px, hitPos.y + 0.5, pz, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // 5. Blazing Cinders & Embers raining and billowing around the crater
        int emberCount = (int) (70 + potency * 35);
        for (int i = 0; i < emberCount; i++) {
            double rx = (serverWorld.random.nextDouble() - 0.5) * (maxShockwaveRadius * 1.5);
            double rz = (serverWorld.random.nextDouble() - 0.5) * (maxShockwaveRadius * 1.5);
            double ry = serverWorld.random.nextDouble() * 8.0;
            double vx = (rx / maxShockwaveRadius) * 0.3;
            double vy = 0.2 + serverWorld.random.nextDouble() * 0.4;
            double vz = (rz / maxShockwaveRadius) * 0.3;

            spawnForcedParticles(serverWorld, ModParticles.FLAME_EMBER_PARTICLE,
                    hitPos.x + rx, hitPos.y + ry, hitPos.z + rz,
                    1, vx, vy, vz, 0.2);
        }

        // 6. Devastating Area Damage & Severe Knockback (With Barrier Shielding!)
        float baseDamage = 75.0f;
        float totalDamage = baseDamage * potency;
        double effectRadius = 6.5 + (potency * 3.5);

        Box hitArea = new Box(hitPos.x - effectRadius, hitPos.y - effectRadius, hitPos.z - effectRadius,
                hitPos.x + effectRadius, hitPos.y + effectRadius, hitPos.z + effectRadius);

        List<LivingEntity> targets = serverWorld.getEntitiesByClass(LivingEntity.class, hitArea,
                e -> e.isAlive() && e != this.getOwner());

        for (LivingEntity target : targets) {
            double dist = target.getBoundingBox().getCenter().distanceTo(hitPos);
            if (dist <= effectRadius) {
                // Check if target is shielded behind a defensive barrier
                if (isShieldedByBarrier(hitPos, target)) {
                    // Shield visual & audio feedback indicating damage was absorbed
                    serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, 0.3, 0.3, 0.3, 0.15);
                    serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.3f, 1.1f);
                    continue; // Shield completely blocks the blast damage!
                }

                float falloff = (float) Math.max(0.40, 1.0 - (dist / effectRadius));
                float finalDamage = totalDamage * falloff;

                DamageSource source = ModDamageTypes.of(serverWorld, ModDamageTypes.CUTTING_MAGIC, this.getOwner());
                target.damage(serverWorld, source, finalDamage);

                // Set victim on fire (15 - 30 seconds based on potency)
                target.setOnFireFor((int) (15 + potency * 10));

                // Cataclysmic explosive knockback
                Vec3d kbDir = target.getBoundingBox().getCenter().subtract(hitPos);
                if (kbDir.lengthSquared() < 0.001) {
                    kbDir = new Vec3d(0, 1, 0);
                } else {
                    kbDir = kbDir.normalize();
                }
                Vec3d kb = kbDir.multiply(2.0).add(0, 0.8, 0);
                target.setVelocity(kb);
                target.velocityModified = true;
            }
        }

        // 7. Scorched Ground & Fire Ignition (only outside shielded zones)
        int fireRadius = (int) (3 + potency * 2);
        BlockPos centerBlock = BlockPos.ofFloored(hitPos);
        for (int dx = -fireRadius; dx <= fireRadius; dx++) {
            for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                if ((dx * dx) + (dz * dz) <= fireRadius * fireRadius) {
                    if (serverWorld.random.nextFloat() < 0.65f) {
                        BlockPos targetGround = serverWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING, centerBlock.add(dx, 0, dz));
                        if (serverWorld.getBlockState(targetGround).isAir() &&
                                serverWorld.getBlockState(targetGround.down()).isSolidBlock(serverWorld, targetGround.down())) {
                            serverWorld.setBlockState(targetGround, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }
        }

        this.discard();
    }

    /**
     * Checks whether an entity is shielded from the explosion at explosionPos by an active barrier
     * (e.g. Hex Shield Defensive Magic or Spherical Domain Barrier).
     */
    public static boolean isShieldedByBarrier(Vec3d explosionPos, LivingEntity target) {
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        Vec3d toTarget = targetCenter.subtract(explosionPos);
        double distToTarget = toTarget.length();
        if (distToTarget < 0.01) return false;
        Vec3d dir = toTarget.normalize();

        for (ActiveBarrier barrier : BarrierManager.ACTIVE_BARRIERS) {
            boolean isSphere = barrier.getDirection().equals(Vec3d.ZERO);

            if (isSphere) {
                double barrierRadius = barrier.getRadius();
                double explosionDist = explosionPos.distanceTo(barrier.getPosition());
                double targetDist = targetCenter.distanceTo(barrier.getPosition());

                // Sphere barrier shields if target is inside and blast is outside (or on surface)
                if (targetDist < barrierRadius && explosionDist >= barrierRadius - 0.5) {
                    return true;
                }
                // Or if target is outside and blast is inside
                if (targetDist > barrierRadius && explosionDist <= barrierRadius + 0.5) {
                    return true;
                }
            } else {
                // Directional Hex Shield:
                // 1. Raycast line segment from blast to target through hex box
                Box hexBox = Box.from(barrier.getPosition()).expand(0.9);
                Optional<Vec3d> hit = hexBox.raycast(explosionPos, targetCenter);
                if (hit.isPresent() && hit.get().distanceTo(explosionPos) < distToTarget) {
                    return true;
                }

                // 2. Shield plane interception: check if target is on the opposite side of shield plane
                Vec3d shieldPos = barrier.getPosition();
                Vec3d shieldNormal = barrier.getDirection().normalize();
                double explosionSide = explosionPos.subtract(shieldPos).dotProduct(shieldNormal);
                double targetSide = targetCenter.subtract(shieldPos).dotProduct(shieldNormal);

                // If explosion is on front/outer side and target is on back/inner side
                if (explosionSide >= -0.25 && targetSide < 0.25) {
                    double denom = dir.dotProduct(shieldNormal);
                    if (Math.abs(denom) > 1e-5) {
                        double t = shieldPos.subtract(explosionPos).dotProduct(shieldNormal) / denom;
                        if (t >= 0.0 && t <= distToTarget) {
                            Vec3d planeIntersection = explosionPos.add(dir.multiply(t));
                            if (planeIntersection.distanceTo(shieldPos) <= 1.4) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sends particle packets with force=true and important=true to bypass the default 32-block culling,
     * allowing long-distance rendering up to 512 blocks away.
     */
    private <T extends ParticleEffect> void spawnForcedParticles(ServerWorld serverWorld, T particle,
                                                                 double x, double y, double z, int count,
                                                                 double deltaX, double deltaY, double deltaZ, double speed) {
        serverWorld.spawnParticles(particle, true, true, x, y, z, count, deltaX, deltaY, deltaZ, speed);
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (entity == this) return false;
        if (entity == this.getOwner()) return false;
        if (entity instanceof DomainShrineEntity) return false;
        if (entity.isSpectator()) return false;
        return entity.isAlive();
    }

    @Override
    public void writeCustomData(WriteView nbt) {
        super.writeCustomData(nbt);
        nbt.putFloat("Potency", this.getPotency());
        if (this.startPos != null) {
            nbt.putDouble("StartX", this.startPos.x);
            nbt.putDouble("StartY", this.startPos.y);
            nbt.putDouble("StartZ", this.startPos.z);
        }
    }

    @Override
    public void readCustomData(ReadView nbt) {
        super.readCustomData(nbt);
        this.setPotency(nbt.getFloat("Potency", 1.0f));
        double sx = nbt.getDouble("StartX", Double.NaN);
        if (!Double.isNaN(sx)) {
            this.startPos = new Vec3d(sx, nbt.getDouble("StartY", 0.0), nbt.getDouble("StartZ", 0.0));
        }
    }
}
