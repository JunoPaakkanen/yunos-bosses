package com.yuno.yunosbosses.entity.projectile;

import com.yuno.yunosbosses.entity.damage.ModDamageTypes;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SlashProjectileEntity extends ProjectileEntity {
    private Vec3d startPos;
    public static final double MAX_RANGE = 5.0; // Strictly 5-meter range in Frieren lore

    private final Set<UUID> hitEntities = new HashSet<>();
    private boolean hitSolidBlock = false;

    private static final TrackedData<Float> DAMAGE = DataTracker.registerData(SlashProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ROLL_ANGLE = DataTracker.registerData(SlashProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> COMBO_STEP = DataTracker.registerData(SlashProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SLASH_WIDTH = DataTracker.registerData(SlashProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> IS_FINISHER = DataTracker.registerData(SlashProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public SlashProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SlashProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world, float baseDamage) {
        this(entityType, world, baseDamage, 0.0f, 0, 1.3f, false);
    }

    public SlashProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world,
                                 float baseDamage, float rollAngle, int comboStep, float slashWidth, boolean isFinisher) {
        super(entityType, world);
        this.setDamage(baseDamage);
        this.setRollAngle(rollAngle);
        this.setComboStep(comboStep);
        this.setSlashWidth(slashWidth);
        this.setIsFinisher(isFinisher);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DAMAGE, 0.0f);
        builder.add(ROLL_ANGLE, 0.0f);
        builder.add(COMBO_STEP, 0);
        builder.add(SLASH_WIDTH, 1.3f);
        builder.add(IS_FINISHER, false);
    }

    public float getDamage() { return this.dataTracker.get(DAMAGE); }
    public void setDamage(float damage) { this.dataTracker.set(DAMAGE, damage); }

    public float getRollAngle() { return this.dataTracker.get(ROLL_ANGLE); }
    public void setRollAngle(float roll) { this.dataTracker.set(ROLL_ANGLE, roll); }

    public int getComboStep() { return this.dataTracker.get(COMBO_STEP); }
    public void setComboStep(int step) { this.dataTracker.set(COMBO_STEP, step); }

    public float getSlashWidth() { return this.dataTracker.get(SLASH_WIDTH); }
    public void setSlashWidth(float width) { this.dataTracker.set(SLASH_WIDTH, width); }

    public boolean isFinisher() { return this.dataTracker.get(IS_FINISHER); }
    public void setIsFinisher(boolean finisher) { this.dataTracker.set(IS_FINISHER, finisher); }

    @Override
    public void tick() {
        super.tick();

        // Store starting position on the first tick
        if (this.startPos == null) {
            this.startPos = this.getPos();
        }

        // Check if the projectile has reached its maximum range (5.0 blocks)
        double distanceTraveled = this.getPos().distanceTo(this.startPos);
        if (distanceTraveled >= MAX_RANGE || this.hitSolidBlock) {
            this.discard();
            return;
        }

        Vec3d currentPos = this.getPos();
        Vec3d velocity = this.getVelocity();
        Vec3d nextPos = currentPos.add(velocity);

        // 1. Raycast for terrain / solid walls along movement step
        BlockHitResult blockHit = this.getWorld().raycast(new RaycastContext(
                currentPos,
                nextPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        ));

        if (blockHit.getType() != HitResult.Type.MISS) {
            this.onBlockHit(blockHit);
            this.setPos(blockHit.getPos().x, blockHit.getPos().y, blockHit.getPos().z);
            this.hitSolidBlock = true;
            this.discard();
            return;
        }

        // 2. Multi-target cleaving along the cutting arc
        if (!this.getWorld().isClient) {
            cleaveEntities(currentPos, nextPos);
            spawnFlightParticles((ServerWorld) this.getWorld(), currentPos, velocity);
        }

        // 3. Move the projectile
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // Safety timeout (discard after 10 ticks)
        if (this.age > 10) {
            this.discard();
        }
    }

    private void cleaveEntities(Vec3d from, Vec3d to) {
        float width = this.getSlashWidth();
        Box sweepBox = new Box(from, to).expand(width * 0.75, 0.8, width * 0.75);
        List<Entity> candidates = this.getWorld().getOtherEntities(this, sweepBox, this::canHit);
        Entity owner = this.getOwner();
        ServerWorld serverWorld = (ServerWorld) this.getWorld();

        for (Entity candidate : candidates) {
            if (candidate instanceof LivingEntity target && !this.hitEntities.contains(target.getUuid())) {
                this.hitEntities.add(target.getUuid());
                applySlashDamage(serverWorld, target, owner);
            }
        }
    }

    private void applySlashDamage(ServerWorld serverWorld, LivingEntity target, Entity owner) {
        Vec3d hitPos = target.getBoundingBox().getCenter();
        float damage = this.getDamage();

        // 1. Point-blank severance bonus (within 2.5m of owner)
        boolean isPointBlank = owner != null && owner.distanceTo(target) <= 2.5;
        if (isPointBlank) {
            damage *= 1.35f;
        }

        // 2. "Visualization: Cloth/Shields Can Be Cut" (Bypass & Disable Shields)
        if (target.isBlocking()) {
            if (target instanceof PlayerEntity playerTarget) {
                playerTarget.getItemCooldownManager().set(playerTarget.getActiveItem(), 60);
                playerTarget.clearActiveItem();
                serverWorld.sendEntityStatus(playerTarget, (byte) 30); // Vanilla shield break status
                serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.2f, 1.05f);
            }
        }

        // 3. Inflict cutting magic damage (bypasses armor)
        DamageSource source = ModDamageTypes.of(serverWorld, ModDamageTypes.CUTTING_MAGIC, owner != null ? owner : this);
        target.damage(serverWorld, source, damage);

        // 4. Sound feedback
        float hitPitch = 1.05f + (this.random.nextFloat() * 0.25f);
        if (this.isFinisher() || isPointBlank) {
            hitPitch = 1.40f;
            serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    SoundEvents.ITEM_SHEARS_SNIP, SoundCategory.PLAYERS, 1.3f, 1.2f);
        }
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 1.25f, hitPitch);

        // 5. Visual Scissors & Slicing Sparks
        spawnImpactParticles(serverWorld, hitPos, isPointBlank);
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld().isClient) return;

        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = serverWorld.getBlockState(blockPos);
        Vec3d hitPos = blockHitResult.getPos();

        // Crisp stone incision sound
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.15f, 1.35f);
        serverWorld.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                ModSounds.REELSEIDEN_HIT, SoundCategory.PLAYERS, 0.85f, 1.5f);

        // Eject stone / surface fragments without world destruction
        if (!state.isAir()) {
            serverWorld.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                    hitPos.x, hitPos.y, hitPos.z, 14, 0.18, 0.18, 0.18, 0.08);
        }

        // Tactical debris / dust puff (Episode 24 reference)
        serverWorld.spawnParticles(ParticleTypes.POOF,
                hitPos.x, hitPos.y, hitPos.z, 5, 0.15, 0.15, 0.15, 0.03);
    }

    private void spawnImpactParticles(ServerWorld serverWorld, Vec3d pos, boolean isPointBlank) {
        // The iconic scissors impact particle
        int count = (isPointBlank || this.isFinisher()) ? 2 : 1;
        for (int i = 0; i < count; i++) {
            serverWorld.spawnParticles(
                    ModParticles.SLASH_IMPACT_SCISSORS_PARTICLE,
                    pos.x, pos.y, pos.z,
                    1,
                    0.08, 0.08, 0.08,
                    0.0
            );
        }

        // Sharp surgical incision sparks
        int critCount = isPointBlank ? 12 : 6;
        serverWorld.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, critCount, 0.22, 0.22, 0.22, 0.1);

        // Vacuum air displacement sweep
        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    private void spawnFlightParticles(ServerWorld serverWorld, Vec3d currentPos, Vec3d velocity) {
        // Subtle vacuum air streak in flight
        if (this.random.nextFloat() < 0.65f) {
            serverWorld.spawnParticles(ParticleTypes.WHITE_SMOKE,
                    currentPos.x, currentPos.y, currentPos.z, 1,
                    -velocity.x * 0.04, -velocity.y * 0.04, -velocity.z * 0.04, 0.01);
        }
        if (this.random.nextFloat() < 0.35f) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    currentPos.x, currentPos.y, currentPos.z, 1,
                    0.15, 0.15, 0.15, 0.02);
        }
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (this.getCommandTags().contains("domain_cosmetic")) {
            return false;
        }
        if (entity == this.getOwner()) {
            return false;
        }
        if (entity.isSpectator()) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    @Override
    public void writeCustomData(WriteView nbt) {
        super.writeCustomData(nbt);
        nbt.putFloat("BaseDamage", this.getDamage());
        nbt.putFloat("RollAngle", this.getRollAngle());
        nbt.putInt("ComboStep", this.getComboStep());
        nbt.putFloat("SlashWidth", this.getSlashWidth());
        nbt.putBoolean("IsFinisher", this.isFinisher());
    }

    @Override
    public void readCustomData(ReadView nbt) {
        super.readCustomData(nbt);
        this.setDamage(nbt.getFloat("BaseDamage", 0.0F));
        this.setRollAngle(nbt.getFloat("RollAngle", 0.0F));
        this.setComboStep(nbt.getInt("ComboStep", 0));
        this.setSlashWidth(nbt.getFloat("SlashWidth", 1.3F));
        this.setIsFinisher(nbt.getBoolean("IsFinisher", false));
    }
}
