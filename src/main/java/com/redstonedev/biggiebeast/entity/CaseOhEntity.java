package com.redstonedev.biggiebeast.entity;

import com.mojang.math.Vector3f;
import com.redstonedev.biggiebeast.init.ModSounds;
import com.redstonedev.biggiebeast.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.List;

public class CaseOhEntity extends Monster implements IAnimatable {

    private static final String K = "animation.caseoh.";
    private static final EntityDataAccessor<Boolean> DATA_MOVING =
            SynchedEntityData.defineId(CaseOhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(CaseOhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(CaseOhEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private double lastX, lastZ;
    private int stompCooldown = 0;
    private int ambientCooldown;
    private int lifeTicks = 0;
    private int chaseTicks = 0;       // time chasing without eating
    private LivingEntity eating = null;
    private int eatTicks = 0;
    private boolean mouthMode = false;
    private int mouthTimer = 0;

    public CaseOhEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.maxUpStep = 1.5F;
        this.ambientCooldown = 200 + this.random.nextInt(400);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000000.0D)        // clamps to vanilla cap, effectively unkillable
                .add(Attributes.ATTACK_DAMAGE, 1000000000.0D)  // 1 billion
                .add(Attributes.MOVEMENT_SPEED, 0.25D)          // medium - player can outrun
                .add(Attributes.FOLLOW_RANGE, 256.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MOVING, false);
        this.entityData.define(DATA_ATTACKING, false);
        this.entityData.define(DATA_CLIMBING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        WallClimberNavigation nav = new WallClimberNavigation(this, level); // can climb
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public boolean isMovingAnim() { return this.entityData.get(DATA_MOVING); }
    public boolean isAttackingAnim() { return this.entityData.get(DATA_ATTACKING); }
    public boolean isClimbingFlag() { return this.entityData.get(DATA_CLIMBING); }
    @Override public boolean onClimbable() { return this.isClimbingFlag(); }

    // === Tick =================================================================
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;

        lifeTicks++;
        if (stompCooldown > 0) stompCooldown--;
        if (ambientCooldown > 0) ambientCooldown--;
        this.entityData.set(DATA_CLIMBING, this.horizontalCollision);

        // Random ambient sounds during the world (dweller-style), never on spawn.
        if (ambientCooldown <= 0) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.AMBIENT.get(this.random.nextInt(ModSounds.AMBIENT.size())).get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            ambientCooldown = 500 + this.random.nextInt(900);
        }

        // Moving flag + stomp + screen shake.
        double mdx = this.getX() - lastX, mdz = this.getZ() - lastZ;
        boolean moving = (mdx * mdx + mdz * mdz) > 1.0E-5D;
        lastX = this.getX(); lastZ = this.getZ();
        this.entityData.set(DATA_MOVING, moving && eating == null && !mouthMode);
        if (moving && stompCooldown <= 0 && !mouthMode) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.2F, 0.8F);
            shakeNearby();
            stompCooldown = 12;
        }

        if (mouthMode) { tickMouth(); return; }
        if (eating != null) { tickEating(); return; }

        // Smell: always know where the nearest player/entity is (ignores walls).
        LivingEntity target = findPrey();
        this.setTarget(target);
        makeNearbyFlee(target);

        if (target != null) {
            chaseTicks++;
            digToward(target);
            this.getNavigation().moveTo(target, 1.0D);
            if (this.distanceTo(target) < 2.6D) startEating(target);
            if (chaseTicks >= 1200) enterMouthMode(); // chased 1 min without eating
        } else {
            chaseTicks = 0;
        }

        if (lifeTicks >= 3600 && !mouthMode) this.discard(); // 3 min lifetime if no finale
    }

    // === Prey / fleeing =======================================================
    private LivingEntity findPrey() {
        LivingEntity best = null;
        double bestSq = 256.0D * 256.0D;
        List<LivingEntity> list = this.level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(256.0D, 128.0D, 256.0D), this::validPrey);
        for (int i = 0; i < list.size(); i++) {
            LivingEntity e = list.get(i);
            double d = e.distanceToSqr(this);
            if (d < bestSq) { bestSq = d; best = e; }
        }
        return best;
    }

    private boolean validPrey(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof CaseOhEntity) return false;
        if (e instanceof Player) {
            Player p = (Player) e;
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof Mob; // players + other mobs (incl. custom entities)
    }

    /** Nearby creatures flee from CaseOh, but a slowness keeps them from outrunning him. */
    private void makeNearbyFlee(LivingEntity target) {
        if (this.tickCount % 10 != 0) return;
        List<Mob> mobs = this.level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(12.0D),
                m -> m != this && !(m instanceof CaseOhEntity) && m.isAlive());
        for (int i = 0; i < mobs.size(); i++) {
            Mob m = mobs.get(i);
            Vec3 away = m.position().subtract(this.position()).normalize().scale(8.0D);
            BlockPos to = new BlockPos(m.getX() + away.x, m.getY(), m.getZ() + away.z);
            m.getNavigation().moveTo(to.getX(), to.getY(), to.getZ(), 1.2D);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
        }
    }

    // === Digging ==============================================================
    private void digToward(LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position());
        int ox = (int) Math.signum(dir.x);
        int oz = (int) Math.signum(dir.z);
        BlockPos base = this.blockPosition();
        int broken = 0;
        // Clear his body column plus the slab ahead (so he can walk through any wall).
        for (int dx = -1; dx <= 1 && broken < 40; dx++) {
            for (int dz = -1; dz <= 1 && broken < 40; dz++) {
                for (int dy = 0; dy <= 3 && broken < 40; dy++) {
                    BlockPos p = base.offset(dx + ox, dy, dz + oz);
                    if (breakAny(p)) broken++;
                }
            }
        }
        // Dig DOWN toward an underground target.
        if (dir.y < -1.5D) {
            for (int dx = 0; dx <= 1 && broken < 40; dx++)
                for (int dz = 0; dz <= 1 && broken < 40; dz++)
                    if (breakAny(base.offset(dx, -1, dz))) broken++;
        }
        // Dig UP toward a target above (e.g. a tower).
        if (dir.y > 2.0D) {
            for (int dx = -1; dx <= 1 && broken < 40; dx++)
                for (int dz = -1; dz <= 1 && broken < 40; dz++)
                    if (breakAny(base.offset(dx, 4, dz))) broken++;
        }
    }

    private boolean breakAny(BlockPos p) {
        if (this.level.getBlockState(p).isAir()) return false;
        if (this.level.getBlockState(p).getDestroySpeed(this.level, p) < 0
                && this.level.getBlockState(p).getBlock() != net.minecraft.world.level.block.Blocks.BEDROCK) {
            // unbreakable but not bedrock (e.g. barrier) - skip
        }
        return this.level.destroyBlock(p, false); // removes ANY block, bedrock included
    }

    // === Eating ===============================================================
    private void startEating(LivingEntity victim) {
        eating = victim;
        eatTicks = 0;
        this.entityData.set(DATA_ATTACKING, true);
        this.getNavigation().stop();
    }

    private Vec3 mouthPos() {
        Vec3 fwd = Vec3.directionFromRotation(0, this.getYRot());
        return this.position().add(fwd.x * 0.6D, this.getBbHeight() * 0.85D, fwd.z * 0.6D);
    }

    private void tickEating() {
        if (eating == null || !eating.isAlive()) { stopEating(); return; }
        Vec3 mouth = mouthPos();
        Vec3 to = mouth.subtract(eating.position());
        eating.setDeltaMovement(to.normalize().scale(0.6D));
        eating.hurtMarked = true;
        eating.fallDistance = 0;
        eatTicks++;
        if (to.length() < 1.2D || eatTicks >= 25) {
            devour(eating);
            stopEating();
        }
    }

    private void stopEating() {
        eating = null;
        eatTicks = 0;
        this.entityData.set(DATA_ATTACKING, false);
        chaseTicks = 0;
    }

    private void devour(LivingEntity victim) {
        bloodBurst(victim);
        boolean wasPlayer = victim instanceof Player;
        victim.hurt(ModDamage.CASEOH_EAT, 1_000_000_000.0F);
        MinecraftServer server = this.level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("CaseOh: Light Snack"), false);
        }
        if (wasPlayer) chaseTicks = 0;
    }

    private void bloodBurst(Entity at) {
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) this.level;
        DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.55F, 0.0F, 0.0F), 1.6F);
        sl.sendParticles(blood, at.getX(), at.getY() + at.getBbHeight() * 0.5D, at.getZ(),
                40, 0.4D, 0.5D, 0.4D, 0.02D);
    }

    // === GET IN MY MOUTH finale ===============================================
    private void enterMouthMode() {
        mouthMode = true;
        mouthTimer = 600; // 30 seconds
        this.entityData.set(DATA_ATTACKING, true);
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        MinecraftServer server = this.level.getServer();
        if (server != null) server.getPlayerList().broadcastSystemMessage(
                Component.literal("CaseOh: GET IN MY MOUTH!!!!"), false);
    }

    private void tickMouth() {
        mouthTimer--;
        this.getNavigation().stop();
        Vec3 mouth = mouthPos();
        // Pull all nearby PLAYERS into his mouth - no escape.
        List<Player> players = this.level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(28.0D),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Vec3 to = mouth.subtract(p.position());
            p.setDeltaMovement(to.normalize().scale(0.7D));
            p.hurtMarked = true;
            p.fallDistance = 0;
            if (to.length() < 1.6D) { bloodBurst(p); p.hurt(ModDamage.CASEOH_EAT, 1_000_000_000.0F); }
        }
        if (mouthTimer <= 0) {
            MinecraftServer server = this.level.getServer();
            if (server != null) server.getPlayerList().broadcastSystemMessage(
                    Component.literal("CaseOh: Very Light Snack."), false);
            this.discard();
        }
    }

    // === Combat / death =======================================================
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity && eating == null && !mouthMode) {
            startEating((LivingEntity) target);
        }
        return true;
    }

    @Override
    public void die(DamageSource source) {
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.DEATH.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        super.die(source);
    }

    @Override protected float getSoundVolume() { return 1.0F; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    private void shakeNearby() {
        List<ServerPlayer> players = this.level.getEntitiesOfClass(ServerPlayer.class,
                this.getBoundingBox().inflate(24.0D));
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer sp = players.get(i);
            double dist = sp.distanceTo(this);
            float intensity = (float) Math.max(0.2D, 1.0D - dist / 24.0D);
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new PacketHandler.ShakePacket(8, intensity));
        }
    }

    // === Animation ============================================================
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "loco", 3, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        String anim;
        if (isAttackingAnim()) anim = K + "attack";
        else if (isMovingAnim()) anim = K + "walk";
        else anim = K + "idle";
        event.getController().setAnimation(new AnimationBuilder().loop(anim));
        return PlayState.CONTINUE;
    }

    @Override public AnimationFactory getFactory() { return factory; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("ChaseTicks", chaseTicks);
        tag.putBoolean("Mouth", mouthMode);
        tag.putInt("MouthTimer", mouthTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        chaseTicks = tag.getInt("ChaseTicks");
        mouthMode = tag.getBoolean("Mouth");
        mouthTimer = tag.getInt("MouthTimer");
    }
}
