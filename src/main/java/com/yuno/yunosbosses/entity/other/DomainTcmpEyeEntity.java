package com.yuno.yunosbosses.entity.other;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The embryonic lunar eye centerpiece of Time Cell Moon Palace (時胞月宮殿).
 * Suspended behind the domain caster, observing and maintaining physical presence
 * while glaring intensely at whichever target has accumulated the most damage from
 * the domain's cellular laceration effect.
 */
public class DomainTcmpEyeEntity extends Entity implements GeoEntity {

    public static final float DAMAGE_CAP = 240.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final DustParticleEffect LUNAR_PALE_DUST = new DustParticleEffect(0xE0F7FA, 0.9f);
    private static final DustParticleEffect CELLULAR_RED_DUST = new DustParticleEffect(0xB71C1C, 0.8f);

    private static final TrackedData<String> OWNER_UUID_STRING = DataTracker.registerData(DomainTcmpEyeEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> TARGET_ENTITY_ID = DataTracker.registerData(DomainTcmpEyeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> TARGET_YAW = DataTracker.registerData(DomainTcmpEyeEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TARGET_PITCH = DataTracker.registerData(DomainTcmpEyeEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final Map<UUID, Float> damageDealtMap = new HashMap<>();
    private UUID topTargetUuid = null;

    public DomainTcmpEyeEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_UUID_STRING, "");
        builder.add(TARGET_ENTITY_ID, -1);
        builder.add(TARGET_YAW, 0.0f);
        builder.add(TARGET_PITCH, 0.0f);
    }

    public void setOwner(LivingEntity owner) {
        if (owner != null) {
            this.dataTracker.set(OWNER_UUID_STRING, owner.getUuid().toString());
        }
    }

    public UUID getOwnerUuid() {
        String s = this.dataTracker.get(OWNER_UUID_STRING);
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public float getDamageDealt(UUID uuid) {
        if (uuid == null) return 0.0f;
        return this.damageDealtMap.getOrDefault(uuid, 0.0f);
    }

    /**
     * Records damage dealt by the domain sure-hit to a target and updates the active glare target.
     */
    public void recordDomainDamage(LivingEntity target, float damage) {
        if (target == null || !target.isAlive()) return;
        UUID uuid = target.getUuid();
        float current = this.damageDealtMap.getOrDefault(uuid, 0.0f) + damage;
        this.damageDealtMap.put(uuid, current);
        updateTopTarget();
    }

    public void updateTopTarget() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        UUID bestUuid = null;
        float maxDamage = -1.0f;

        for (Map.Entry<UUID, Float> entry : this.damageDealtMap.entrySet()) {
            Entity ent = serverWorld.getEntity(entry.getKey());
            if (ent instanceof LivingEntity living && living.isAlive()) {
                if (entry.getValue() > maxDamage) {
                    maxDamage = entry.getValue();
                    bestUuid = entry.getKey();
                }
            }
        }

        this.topTargetUuid = bestUuid;
        if (bestUuid != null) {
            Entity targetEnt = serverWorld.getEntity(bestUuid);
            if (targetEnt != null) {
                this.dataTracker.set(TARGET_ENTITY_ID, targetEnt.getId());
            }
        }
    }

    private static float stepAngle(float from, float to, float maxStep) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + MathHelper.clamp(diff, -maxStep, maxStep);
    }

    @Override
    public void tick() {
        super.tick();

        // Safety cleanup if active for more than 60 seconds
        if (!this.getWorld().isClient && this.age > 1200) {
            this.discard();
            return;
        }

        // Stationary suspended in the air
        this.setVelocity(Vec3d.ZERO);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Emergence birth chime & resonance in the first tick
            if (this.age == 1) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2.5f, 0.7f);
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.0f, 0.85f);
            }

            // Radial orbit particles closely surrounding the eye
            if (this.age % 2 == 0) {
                double time = this.age * 0.05;
                int ringPoints = 3;
                double ringRadius = 2.0;

                for (int i = 0; i < ringPoints; i++) {
                    double angle = time + (i * (2.0 * Math.PI / ringPoints));
                    double px = this.getX() + Math.cos(angle) * ringRadius;
                    double pz = this.getZ() + Math.sin(angle) * ringRadius;
                    double py = this.getY() + 1.25 + Math.sin(time * 2.0 + i) * 0.3;

                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0.02, 0.02, 0.02, 0.02);
                    if (this.random.nextInt(3) == 0) {
                        serverWorld.spawnParticles(LUNAR_PALE_DUST, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }

            // Downward cellular dripping particles
            if (this.age % 4 == 0) {
                double ox = (this.random.nextDouble() - 0.5) * 1.8;
                double oz = (this.random.nextDouble() - 0.5) * 1.8;
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        this.getX() + ox, this.getY() + 0.3, this.getZ() + oz,
                        1, 0.02, -0.05, 0.02, 0.03);

                if (this.random.nextInt(4) == 0) {
                    serverWorld.spawnParticles(CELLULAR_RED_DUST,
                            this.getX() + ox, this.getY() + 0.5, this.getZ() + oz,
                            1, 0.04, -0.03, 0.04, 0.01);
                }
            }

            // Ominous heartbeat & pulse every 40 ticks
            if (this.age % 40 == 0) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.8f, 0.65f);
            }

            // Target evaluation
            LivingEntity activeTarget = null;
            if (this.topTargetUuid != null) {
                Entity ent = serverWorld.getEntity(this.topTargetUuid);
                if (ent instanceof LivingEntity living && living.isAlive()) {
                    activeTarget = living;
                } else {
                    this.topTargetUuid = null;
                    updateTopTarget();
                    if (this.topTargetUuid != null) {
                        Entity recheck = serverWorld.getEntity(this.topTargetUuid);
                        if (recheck instanceof LivingEntity livingRecheck && livingRecheck.isAlive()) {
                            activeTarget = livingRecheck;
                        }
                    }
                }
            } else {
                updateTopTarget();
                if (this.topTargetUuid != null) {
                    Entity ent = serverWorld.getEntity(this.topTargetUuid);
                    if (ent instanceof LivingEntity living && living.isAlive()) {
                        activeTarget = living;
                    }
                }
            }

            // Default fallback if nobody took damage yet: look at nearest enemy/non-caster
            if (activeTarget == null) {
                activeTarget = findFallbackTarget(serverWorld);
            }

            if (activeTarget != null) {
                this.dataTracker.set(TARGET_ENTITY_ID, activeTarget.getId());

                Vec3d eyeCenter = this.getPos().add(0, 1.25, 0);
                Vec3d targetCenter = activeTarget.getEyePos();

                double dx = targetCenter.x - eyeCenter.x;
                double dy = targetCenter.y - eyeCenter.y;
                double dz = targetCenter.z - eyeCenter.z;
                double horizDist = Math.sqrt(dx * dx + dz * dz);

                float targetYaw = (float) (MathHelper.atan2(-dx, dz) * (180.0 / Math.PI));
                float targetPitch = (float) (MathHelper.atan2(-dy, horizDist) * (180.0 / Math.PI));

                float newYaw = stepAngle(this.getYaw(), targetYaw, 18.0f);
                float newPitch = stepAngle(this.getPitch(), targetPitch, 18.0f);

                this.setYaw(newYaw);
                this.setPitch(newPitch);
                this.setBodyYaw(newYaw);
                this.setHeadYaw(newYaw);
                this.dataTracker.set(TARGET_YAW, newYaw);
                this.dataTracker.set(TARGET_PITCH, newPitch);
            } else {
                this.dataTracker.set(TARGET_ENTITY_ID, -1);
            }
        } else {
            // Client-side smooth gaze tracking
            int targetId = this.dataTracker.get(TARGET_ENTITY_ID);
            Entity target = targetId != -1 ? this.getWorld().getEntityById(targetId) : null;
            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3d eyeCenter = this.getPos().add(0, 1.25, 0);
                Vec3d targetCenter = living.getEyePos();

                double dx = targetCenter.x - eyeCenter.x;
                double dy = targetCenter.y - eyeCenter.y;
                double dz = targetCenter.z - eyeCenter.z;
                double horizDist = Math.sqrt(dx * dx + dz * dz);

                float targetYaw = (float) (MathHelper.atan2(-dx, dz) * (180.0 / Math.PI));
                float targetPitch = (float) (MathHelper.atan2(-dy, horizDist) * (180.0 / Math.PI));

                this.setYaw(stepAngle(this.getYaw(), targetYaw, 22.0f));
                this.setPitch(stepAngle(this.getPitch(), targetPitch, 22.0f));
            } else {
                this.setYaw(stepAngle(this.getYaw(), this.dataTracker.get(TARGET_YAW), 22.0f));
                this.setPitch(stepAngle(this.getPitch(), this.dataTracker.get(TARGET_PITCH), 22.0f));
            }
            this.setBodyYaw(this.getYaw());
            this.setHeadYaw(this.getYaw());
        }
    }

    private LivingEntity findFallbackTarget(ServerWorld serverWorld) {
        UUID owner = this.getOwnerUuid();
        Box box = Box.from(this.getPos()).expand(36.0);
        List<LivingEntity> list = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && (owner == null || !e.getUuid().equals(owner))
        );
        LivingEntity closest = null;
        double minDistSq = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = this.squaredDistanceTo(e);
            if (d < minDistSq) {
                minDistSq = d;
                closest = e;
            }
        }
        return closest;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isCollidable(Entity other) {
        return !this.isRemoved();
    }

    @Override
    public boolean collidesWith(Entity other) {
        return !this.isRemoved();
    }

    @Override
    public void pushAwayFrom(Entity entity) {}

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readCustomData(ReadView nbt) {
        nbt.read("OwnerUUID", Uuids.INT_STREAM_CODEC).ifPresent(uuid -> {
            this.dataTracker.set(OWNER_UUID_STRING, uuid.toString());
        });
    }

    @Override
    protected void writeCustomData(WriteView nbt) {
        UUID owner = getOwnerUuid();
        if (owner != null) {
            nbt.putNullable("OwnerUUID", Uuids.INT_STREAM_CODEC, owner);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
