package com.guysmith.beardedfox.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//import com.sun.istack.internal.Nullable;
import com.guysmith.beardedfox.registry.ModEntityTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.DiveJumpingGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.EscapeSunlightGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class BeardedFoxEntity extends AnimalEntity {
    private static final TrackedData<Integer> TYPE;
    private static final TrackedData<Byte> FOX_FLAGS;
    private static final TrackedData<Optional<UUID>> OWNER;
    private static final TrackedData<Optional<UUID>> OTHER_TRUSTED;
    private static final Predicate<ItemEntity> PICKABLE_DROP_FILTER;
    private static final Predicate<Entity> JUST_ATTACKED_SOMETHING_FILTER;
    private static final Predicate<Entity> CHICKEN_AND_RABBIT_FILTER;
    private static final Predicate<Entity> NOTICEABLE_PLAYER_FILTER;
    private Goal followChickenAndRabbitGoal;
    private Goal followBabyTurtleGoal;
    private Goal followFishGoal;
    private float headRollProgress;
    private float lastHeadRollProgress;
    private float extraRollingHeight;
    private float lastExtraRollingHeight;
    private int eatingTime;

    // the IDE complains but it seems to compile without; saved in comments in case I ever actually need it
    /*public BeardedFoxEntity(World world) {
        super(ModEntityTypes.BEARDED_FOX, world);
        this.lookControl = new BeardedFoxEntity.FoxLookControl();
        this.moveControl = new BeardedFoxEntity.FoxMoveControl();
        this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }*/

    public BeardedFoxEntity(EntityType<? extends BeardedFoxEntity> entityType, World world) {
        super(entityType, world);
        this.lookControl = new BeardedFoxEntity.FoxLookControl();
        this.moveControl = new BeardedFoxEntity.FoxMoveControl();
        this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OWNER, Optional.empty());
        this.dataTracker.startTracking(OTHER_TRUSTED, Optional.empty());
        this.dataTracker.startTracking(TYPE, 0);
        this.dataTracker.startTracking(FOX_FLAGS, (byte)0);
    }

    protected void initGoals() {
        this.followChickenAndRabbitGoal = new FollowTargetGoal(this, AnimalEntity.class, 10, false, false, (livingEntity) -> livingEntity instanceof ChickenEntity || livingEntity instanceof RabbitEntity);
        this.followBabyTurtleGoal = new FollowTargetGoal(this, TurtleEntity.class, 10, false, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER);
        this.followFishGoal = new FollowTargetGoal(this, FishEntity.class, 20, false, false, (livingEntity) -> livingEntity instanceof SchoolingFishEntity);
        this.goalSelector.add(0, new BeardedFoxEntity.FoxSwimGoal());
        this.goalSelector.add(1, new BeardedFoxEntity.StopWanderingGoal());
        this.goalSelector.add(2, new BeardedFoxEntity.EscapeWhenNotAggressiveGoal(2.2D));
        this.goalSelector.add(3, new BeardedFoxEntity.MateGoal(1.0D));
        this.goalSelector.add(4, new FleeEntityGoal(this, PlayerEntity.class, 16.0F, 1.6D, 1.4D, (livingEntity) -> NOTICEABLE_PLAYER_FILTER.test((Entity) livingEntity) && !this.canTrust(((Entity) livingEntity).getUuid()) && !this.isAggressive()));
        this.goalSelector.add(4, new FleeEntityGoal(this, WolfEntity.class, 8.0F, 1.6D, 1.4D, (livingEntity) -> !((WolfEntity)livingEntity).isTamed() && !this.isAggressive()));
        this.goalSelector.add(4, new FleeEntityGoal(this, PolarBearEntity.class, 8.0F, 1.6D, 1.4D, (livingEntity) -> !this.isAggressive()));
        this.goalSelector.add(5, new BeardedFoxEntity.MoveToHuntGoal());
        this.goalSelector.add(6, new BeardedFoxEntity.JumpChasingGoal());
        this.goalSelector.add(6, new BeardedFoxEntity.AvoidDaylightGoal(1.25D));
        this.goalSelector.add(7, new BeardedFoxEntity.AttackGoal(1.2000000476837158D, true));
        this.goalSelector.add(7, new BeardedFoxEntity.DelayedCalmDownGoal());
        this.goalSelector.add(8, new BeardedFoxEntity.FollowParentGoal(this, 1.25D));
        this.goalSelector.add(9, new BeardedFoxEntity.GoToVillageGoal(32, 200));
        this.goalSelector.add(10, new BeardedFoxEntity.EatSweetBerriesGoal(1.2000000476837158D, 12, 2));
        this.goalSelector.add(10, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(11, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(11, new BeardedFoxEntity.PickupItemGoal());
        this.goalSelector.add(12, new BeardedFoxEntity.LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
        this.goalSelector.add(13, new BeardedFoxEntity.SitDownAndLookAroundGoal());
        this.targetSelector.add(3, new BeardedFoxEntity.DefendFriendGoal(LivingEntity.class, false, false, (livingEntity) -> JUST_ATTACKED_SOMETHING_FILTER.test(livingEntity) && !this.canTrust(livingEntity.getUuid())));
    }

    public SoundEvent getEatSound(ItemStack stack) {
        return SoundEvents.ENTITY_FOX_EAT;
    }

    public void tickMovement() {
        if (!this.world.isClient && this.isAlive() && this.canMoveVoluntarily()) {
            ++this.eatingTime;
            ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND);
            if (this.canEat(itemStack)) {
                if (this.eatingTime > 600) {
                    ItemStack itemStack2 = itemStack.finishUsing(this.world, this);
                    if (!itemStack2.isEmpty()) {
                        this.equipStack(EquipmentSlot.MAINHAND, itemStack2);
                    }

                    this.eatingTime = 0;
                } else if (this.eatingTime > 560 && this.random.nextFloat() < 0.1F) {
                    this.playSound(this.getEatSound(itemStack), 1.0F, 1.0F);
                    this.world.sendEntityStatus(this, (byte)45);
                }
            }

            LivingEntity livingEntity = this.getTarget();
            if (livingEntity == null || !livingEntity.isAlive()) {
                this.setCrouching(false);
                this.setRollingHead(false);
            }
        }

        if (this.isSleeping() || this.isImmobile()) {
            this.jumping = false;
            this.sidewaysSpeed = 0.0F;
            this.forwardSpeed = 0.0F;
        }

        super.tickMovement();
        if (this.isAggressive() && this.random.nextFloat() < 0.05F) {
            this.playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
        }

    }

    protected boolean isImmobile() {
        return this.isDead();
    }

    private boolean canEat(ItemStack stack) {
        return stack.getItem().isFood() && this.getTarget() == null && this.onGround && !this.isSleeping();
    }

    protected void initEquipment(LocalDifficulty difficulty) {
        if (this.random.nextFloat() < 0.2F) {
            float f = this.random.nextFloat();
            ItemStack itemStack6;
            if (f < 0.05F) {
                itemStack6 = new ItemStack(Items.EMERALD);
            } else if (f < 0.2F) {
                itemStack6 = new ItemStack(Items.EGG);
            } else if (f < 0.4F) {
                itemStack6 = this.random.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
            } else if (f < 0.6F) {
                itemStack6 = new ItemStack(Items.WHEAT);
            } else if (f < 0.8F) {
                itemStack6 = new ItemStack(Items.LEATHER);
            } else {
                itemStack6 = new ItemStack(Items.FEATHER);
            }

            this.equipStack(EquipmentSlot.MAINHAND, itemStack6);
        }

    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte status) {
        if (status == 45) {
            ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                for(int i = 0; i < 8; ++i) {
                    Vec3d vec3d = (new Vec3d(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)).rotateX(-this.pitch * 0.017453292F).rotateY(-this.yaw * 0.017453292F);
                    this.world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, itemStack), this.getX() + this.getRotationVector().x / 2.0D, this.getY(), this.getZ() + this.getRotationVector().z / 2.0D, vec3d.x, vec3d.y + 0.05D, vec3d.z);
                }
            }
        } else {
            super.handleStatus(status);
        }

    }

    public static DefaultAttributeContainer.Builder createFoxAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896D).add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D);
    }

    public BeardedFoxEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        // possibility: breed Bearded Foxes with all banner-matching colour palettes through cross breeding and such?
        BeardedFoxEntity foxEntity = (BeardedFoxEntity) ModEntityTypes.BEARDED_FOX.create(serverWorld);
        foxEntity.setType(this.random.nextBoolean() ? this.getFoxType() : ((BeardedFoxEntity)passiveEntity).getFoxType());
        return foxEntity;
    }

    //    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, /*@Nullable*/ EntityData entityData, /*@Nullable*/ CompoundTag entityTag) {
        Optional<RegistryKey<Biome>> optional = world.method_31081(this.getBlockPos());
        BeardedFoxEntity.Type type = BeardedFoxEntity.Type.fromBiome(optional);
        boolean bl = false;
        if (entityData instanceof BeardedFoxData) {
            type = ((BeardedFoxData)entityData).type;
            if (((BeardedFoxData)entityData).getSpawnedCount() >= 2) {
                bl = true;
            }
        } else {
            entityData = new BeardedFoxData(type);
        }

        this.setType(type);
        if (bl) {
            this.setBreedingAge(-24000);
        }

        if (world instanceof ServerWorld) {
            this.addTypeSpecificGoals();
        }

        this.initEquipment(difficulty);
        return super.initialize(world, difficulty, spawnReason, (EntityData)entityData, entityTag);
    }

    private void addTypeSpecificGoals() {
        if (this.getFoxType() == Type.CYAN
                || this.getFoxType() == Type.RED
                || this.getFoxType() == Type.GREEN
                || this.getFoxType() == Type.BROWN) {
            this.targetSelector.add(4, this.followChickenAndRabbitGoal);
            this.targetSelector.add(4, this.followFishGoal);
            this.targetSelector.add(6, this.followBabyTurtleGoal);
        } else {
            this.targetSelector.add(4, this.followFishGoal);
            this.targetSelector.add(6, this.followChickenAndRabbitGoal);
            this.targetSelector.add(6, this.followBabyTurtleGoal);
        }
    }

    protected void eat(PlayerEntity player, ItemStack stack) {
        if (this.isBreedingItem(stack)) {
            this.playSound(this.getEatSound(stack), 1.0F, 1.0F);
        }

        super.eat(player, stack);
    }

    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return this.isBaby() ? dimensions.height * 0.85F : 0.4F;
    }

    public BeardedFoxEntity.Type getFoxType() {
        return BeardedFoxEntity.Type.fromId((Integer)this.dataTracker.get(TYPE));
    }

    private void setType(BeardedFoxEntity.Type type) {
        this.dataTracker.set(TYPE, type.getId());
    }

    private List<UUID> getTrustedUuids() {
        List<UUID> list = Lists.newArrayList();
        list.add((UUID) ((Optional)this.dataTracker.get(OWNER)).orElse((Object)null));
        list.add((UUID) ((Optional)this.dataTracker.get(OTHER_TRUSTED)).orElse((Object)null));
        return list;
    }

    private void addTrustedUuid(/*@Nullable*/ UUID uuid) {
        if (((Optional)this.dataTracker.get(OWNER)).isPresent()) {
            this.dataTracker.set(OTHER_TRUSTED, Optional.ofNullable(uuid));
        } else {
            this.dataTracker.set(OWNER, Optional.ofNullable(uuid));
        }

    }

    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        List<UUID> list = this.getTrustedUuids();
        ListTag listTag = new ListTag();
        Iterator var4 = list.iterator();

        while(var4.hasNext()) {
            UUID uUID = (UUID)var4.next();
            if (uUID != null) {
                listTag.add(NbtHelper.fromUuid(uUID));
            }
        }

        tag.put("Trusted", listTag);
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putString("Type", this.getFoxType().getKey());
        tag.putBoolean("Sitting", this.isSitting());
        tag.putBoolean("Crouching", this.isInSneakingPose());
    }

    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        ListTag listTag = tag.getList("Trusted", 11);

        for (net.minecraft.nbt.Tag value : listTag) {
            this.addTrustedUuid(NbtHelper.toUuid(value));
        }

        this.setSleeping(tag.getBoolean("Sleeping"));
        this.setType(BeardedFoxEntity.Type.byName(tag.getString("Type")));
        this.setSitting(tag.getBoolean("Sitting"));
        this.setCrouching(tag.getBoolean("Crouching"));
        if (this.world instanceof ServerWorld) {
            this.addTypeSpecificGoals();
        }

    }

    public boolean isSitting() {
        return this.getFoxFlag(1);
    }

    public void setSitting(boolean sitting) {
        this.setFoxFlag(1, sitting);
    }

    public boolean isWalking() {
        return this.getFoxFlag(64);
    }

    private void setWalking(boolean walking) {
        this.setFoxFlag(64, walking);
    }

    private boolean isAggressive() {
        return this.getFoxFlag(128);
    }

    private void setAggressive(boolean aggressive) {
        this.setFoxFlag(128, aggressive);
    }

    public boolean isSleeping() {
        return this.getFoxFlag(32);
    }

    private void setSleeping(boolean sleeping) {
        this.setFoxFlag(32, sleeping);
    }

    private void setFoxFlag(int mask, boolean value) {
        if (value) {
            this.dataTracker.set(FOX_FLAGS, (byte)((Byte)this.dataTracker.get(FOX_FLAGS) | mask));
        } else {
            this.dataTracker.set(FOX_FLAGS, (byte)((Byte)this.dataTracker.get(FOX_FLAGS) & ~mask));
        }

    }

    private boolean getFoxFlag(int bitmask) {
        return ((Byte)this.dataTracker.get(FOX_FLAGS) & bitmask) != 0;
    }

    public boolean canEquip(ItemStack stack) {
        EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack);
        if (!this.getEquippedStack(equipmentSlot).isEmpty()) {
            return false;
        } else {
            return equipmentSlot == EquipmentSlot.MAINHAND && super.canEquip(stack);
        }
    }

    public boolean canPickupItem(ItemStack stack) {
        Item item = stack.getItem();
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND);
        return itemStack.isEmpty() || this.eatingTime > 0 && item.isFood() && !itemStack.getItem().isFood();
    }

    private void spit(ItemStack stack) {
        if (!stack.isEmpty() && !this.world.isClient) {
            ItemEntity itemEntity = new ItemEntity(this.world, this.getX() + this.getRotationVector().x, this.getY() + 1.0D, this.getZ() + this.getRotationVector().z, stack);
            itemEntity.setPickupDelay(40);
            itemEntity.setThrower(this.getUuid());
            this.playSound(SoundEvents.ENTITY_FOX_SPIT, 1.0F, 1.0F);
            this.world.spawnEntity(itemEntity);
        }
    }

    private void dropItem(ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(this.world, this.getX(), this.getY(), this.getZ(), stack);
        this.world.spawnEntity(itemEntity);
    }

    protected void loot(ItemEntity item) {
        ItemStack itemStack = item.getStack();
        if (this.canPickupItem(itemStack)) {
            int i = itemStack.getCount();
            if (i > 1) {
                this.dropItem(itemStack.split(i - 1));
            }

            this.spit(this.getEquippedStack(EquipmentSlot.MAINHAND));
            this.method_29499(item);
            this.equipStack(EquipmentSlot.MAINHAND, itemStack.split(1));
            this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 2.0F;
            this.sendPickup(item, itemStack.getCount());
            item.remove();
            this.eatingTime = 0;
        }

    }

    public void tick() {
        super.tick();
        if (this.canMoveVoluntarily()) {
            boolean bl = this.isTouchingWater();
            if (bl || this.getTarget() != null || this.world.isThundering()) {
                this.stopSleeping();
            }

            if (bl || this.isSleeping()) {
                this.setSitting(false);
            }

            if (this.isWalking() && this.world.random.nextFloat() < 0.2F) {
                BlockPos blockPos = this.getBlockPos();
                BlockState blockState = this.world.getBlockState(blockPos);
                this.world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(blockState));
            }
        }

        this.lastHeadRollProgress = this.headRollProgress;
        if (this.isRollingHead()) {
            this.headRollProgress += (1.0F - this.headRollProgress) * 0.4F;
        } else {
            this.headRollProgress += (0.0F - this.headRollProgress) * 0.4F;
        }

        this.lastExtraRollingHeight = this.extraRollingHeight;
        if (this.isInSneakingPose()) {
            this.extraRollingHeight += 0.2F;
            if (this.extraRollingHeight > 3.0F) {
                this.extraRollingHeight = 3.0F;
            }
        } else {
            this.extraRollingHeight = 0.0F;
        }

    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Items.SWEET_BERRIES;
    }

    protected void onPlayerSpawnedChild(PlayerEntity player, MobEntity child) {
        ((BeardedFoxEntity)child).addTrustedUuid(player.getUuid());
    }

    public boolean isChasing() {
        return this.getFoxFlag(16);
    }

    public void setChasing(boolean chasing) {
        this.setFoxFlag(16, chasing);
    }

    public boolean isFullyCrouched() {
        return this.extraRollingHeight == 3.0F;
    }

    public void setCrouching(boolean crouching) {
        this.setFoxFlag(4, crouching);
    }

    public boolean isInSneakingPose() {
        return this.getFoxFlag(4);
    }

    public void setRollingHead(boolean rollingHead) {
        this.setFoxFlag(8, rollingHead);
    }

    public boolean isRollingHead() {
        return this.getFoxFlag(8);
    }

    @Environment(EnvType.CLIENT)
    public float getHeadRoll(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lastHeadRollProgress, this.headRollProgress) * 0.11F * 3.1415927F;
    }

    @Environment(EnvType.CLIENT)
    public float getBodyRotationHeightOffset(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lastExtraRollingHeight, this.extraRollingHeight);
    }

    public void setTarget(/*@Nullable*/ LivingEntity target) {
        if (this.isAggressive() && target == null) {
            this.setAggressive(false);
        }

        super.setTarget(target);
    }

    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return MathHelper.ceil((fallDistance - 5.0F) * damageMultiplier);
    }

    private void stopSleeping() {
        this.setSleeping(false);
    }

    private void stopActions() {
        this.setRollingHead(false);
        this.setCrouching(false);
        this.setSitting(false);
        this.setSleeping(false);
        this.setAggressive(false);
        this.setWalking(false);
    }

    private boolean wantsToPickupItem() {
        return !this.isSleeping() && !this.isSitting() && !this.isWalking();
    }

    public void playAmbientSound() {
        SoundEvent soundEvent = this.getAmbientSound();
        if (soundEvent == SoundEvents.ENTITY_FOX_SCREECH) {
            this.playSound(soundEvent, 2.0F, this.getSoundPitch());
        } else {
            super.playAmbientSound();
        }

    }

    //@Nullable
    protected SoundEvent getAmbientSound() {
        if (this.isSleeping()) {
            return SoundEvents.ENTITY_FOX_SLEEP;
        } else {
            if (!this.world.isDay() && this.random.nextFloat() < 0.1F) {
                List<PlayerEntity> list = this.world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(16.0D, 16.0D, 16.0D), EntityPredicates.EXCEPT_SPECTATOR);
                if (list.isEmpty()) {
                    return SoundEvents.ENTITY_FOX_SCREECH;
                }
            }

            return SoundEvents.ENTITY_FOX_AMBIENT;
        }
    }

    //@Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_FOX_HURT;
    }

    //@Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_FOX_DEATH;
    }

    private boolean canTrust(UUID uuid) {
        return this.getTrustedUuids().contains(uuid);
    }

    protected void drop(DamageSource source) {
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty()) {
            this.dropStack(itemStack);
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }

        super.drop(source);
    }

    public static boolean canJumpChase(BeardedFoxEntity fox, LivingEntity chasedEntity) {
        double d = chasedEntity.getZ() - fox.getZ();
        double e = chasedEntity.getX() - fox.getX();
        double f = d / e;
        //int i = true; // what the fuck was this?

        for(int j = 0; j < 6; ++j) {
            double g = f == 0.0D ? 0.0D : d * (double)((float)j / 6.0F);
            double h = f == 0.0D ? e * (double)((float)j / 6.0F) : g / f;

            for(int k = 1; k < 4; ++k) {
                if (!fox.world.getBlockState(new BlockPos(fox.getX() + h, fox.getY() + (double)k, fox.getZ() + g)).getMaterial().isReplaceable()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Environment(EnvType.CLIENT)
    public Vec3d method_29919() {
        return new Vec3d(0.0D, (double)(0.55F * this.getStandingEyeHeight()), (double)(this.getWidth() * 0.4F));
    }

    static {
        TYPE = DataTracker.registerData(BeardedFoxEntity.class, TrackedDataHandlerRegistry.INTEGER);
        FOX_FLAGS = DataTracker.registerData(BeardedFoxEntity.class, TrackedDataHandlerRegistry.BYTE);
        OWNER = DataTracker.registerData(BeardedFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        OTHER_TRUSTED = DataTracker.registerData(BeardedFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        PICKABLE_DROP_FILTER = (itemEntity) -> !itemEntity.cannotPickup() && itemEntity.isAlive();
        JUST_ATTACKED_SOMETHING_FILTER = (entity) -> {
            if (!(entity instanceof LivingEntity)) {
                return false;
            } else {
                LivingEntity livingEntity = (LivingEntity)entity;
                return livingEntity.getAttacking() != null && livingEntity.getLastAttackTime() < livingEntity.age + 600;
            }
        };
        CHICKEN_AND_RABBIT_FILTER = (entity) -> entity instanceof ChickenEntity || entity instanceof RabbitEntity;
        NOTICEABLE_PLAYER_FILTER = (entity) -> !entity.isSneaky() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity);
    }

    class LookAtEntityGoal extends net.minecraft.entity.ai.goal.LookAtEntityGoal {
        public LookAtEntityGoal(MobEntity fox, Class<? extends LivingEntity> targetType, float range) {
            super(fox, targetType, range);
        }

        public boolean canStart() {
            return super.canStart() && !BeardedFoxEntity.this.isWalking() && !BeardedFoxEntity.this.isRollingHead();
        }

        public boolean shouldContinue() {
            return super.shouldContinue() && !BeardedFoxEntity.this.isWalking() && !BeardedFoxEntity.this.isRollingHead();
        }
    }

    class FollowParentGoal extends net.minecraft.entity.ai.goal.FollowParentGoal {
        private final BeardedFoxEntity fox;

        public FollowParentGoal(BeardedFoxEntity fox, double speed) {
            super(fox, speed);
            this.fox = fox;
        }

        public boolean canStart() {
            return !this.fox.isAggressive() && super.canStart();
        }

        public boolean shouldContinue() {
            return !this.fox.isAggressive() && super.shouldContinue();
        }

        public void start() {
            this.fox.stopActions();
            super.start();
        }
    }

    public class FoxLookControl extends LookControl {
        public FoxLookControl() {
            super(BeardedFoxEntity.this);
        }

        public void tick() {
            if (!BeardedFoxEntity.this.isSleeping()) {
                super.tick();
            }

        }

        protected boolean shouldStayHorizontal() {
            return !BeardedFoxEntity.this.isChasing() && !BeardedFoxEntity.this.isInSneakingPose() && !BeardedFoxEntity.this.isRollingHead() & !BeardedFoxEntity.this.isWalking();
        }
    }

    public class JumpChasingGoal extends DiveJumpingGoal {
        public boolean canStart() {
            if (!BeardedFoxEntity.this.isFullyCrouched()) {
                return false;
            } else {
                LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
                if (livingEntity != null && livingEntity.isAlive()) {
                    if (livingEntity.getMovementDirection() != livingEntity.getHorizontalFacing()) {
                        return false;
                    } else {
                        boolean bl = BeardedFoxEntity.canJumpChase(BeardedFoxEntity.this, livingEntity);
                        if (!bl) {
                            BeardedFoxEntity.this.getNavigation().findPathTo((Entity)livingEntity, 0);
                            BeardedFoxEntity.this.setCrouching(false);
                            BeardedFoxEntity.this.setRollingHead(false);
                        }

                        return bl;
                    }
                } else {
                    return false;
                }
            }
        }

        public boolean shouldContinue() {
            LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                double d = BeardedFoxEntity.this.getVelocity().y;
                return (d * d >= 0.05000000074505806D || Math.abs(BeardedFoxEntity.this.pitch) >= 15.0F || !BeardedFoxEntity.this.onGround) && !BeardedFoxEntity.this.isWalking();
            } else {
                return false;
            }
        }

        public boolean canStop() {
            return false;
        }

        public void start() {
            BeardedFoxEntity.this.setJumping(true);
            BeardedFoxEntity.this.setChasing(true);
            BeardedFoxEntity.this.setRollingHead(false);
            LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
            BeardedFoxEntity.this.getLookControl().lookAt(livingEntity, 60.0F, 30.0F);
            Vec3d vec3d = (new Vec3d(livingEntity.getX() - BeardedFoxEntity.this.getX(), livingEntity.getY() - BeardedFoxEntity.this.getY(), livingEntity.getZ() - BeardedFoxEntity.this.getZ())).normalize();
            BeardedFoxEntity.this.setVelocity(BeardedFoxEntity.this.getVelocity().add(vec3d.x * 0.8D, 0.9D, vec3d.z * 0.8D));
            BeardedFoxEntity.this.getNavigation().stop();
        }

        public void stop() {
            BeardedFoxEntity.this.setCrouching(false);
            BeardedFoxEntity.this.extraRollingHeight = 0.0F;
            BeardedFoxEntity.this.lastExtraRollingHeight = 0.0F;
            BeardedFoxEntity.this.setRollingHead(false);
            BeardedFoxEntity.this.setChasing(false);
        }

        public void tick() {
            LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
            if (livingEntity != null) {
                BeardedFoxEntity.this.getLookControl().lookAt(livingEntity, 60.0F, 30.0F);
            }

            if (!BeardedFoxEntity.this.isWalking()) {
                Vec3d vec3d = BeardedFoxEntity.this.getVelocity();
                if (vec3d.y * vec3d.y < 0.029999999329447746D && BeardedFoxEntity.this.pitch != 0.0F) {
                    BeardedFoxEntity.this.pitch = MathHelper.lerpAngle(BeardedFoxEntity.this.pitch, 0.0F, 0.2F);
                } else {
                    double d = Math.sqrt(Entity.squaredHorizontalLength(vec3d));
                    double e = Math.signum(-vec3d.y) * Math.acos(d / vec3d.length()) * 57.2957763671875D;
                    BeardedFoxEntity.this.pitch = (float)e;
                }
            }

            if (livingEntity != null && BeardedFoxEntity.this.distanceTo(livingEntity) <= 2.0F) {
                BeardedFoxEntity.this.tryAttack(livingEntity);
            } else if (BeardedFoxEntity.this.pitch > 0.0F && BeardedFoxEntity.this.onGround && (float)BeardedFoxEntity.this.getVelocity().y != 0.0F && BeardedFoxEntity.this.world.getBlockState(BeardedFoxEntity.this.getBlockPos()).isOf(Blocks.SNOW)) {
                BeardedFoxEntity.this.pitch = 60.0F;
                BeardedFoxEntity.this.setTarget((LivingEntity)null);
                BeardedFoxEntity.this.setWalking(true);
            }

        }
    }

    class FoxSwimGoal extends SwimGoal {
        public FoxSwimGoal() {
            super(BeardedFoxEntity.this);
        }

        public void start() {
            super.start();
            BeardedFoxEntity.this.stopActions();
        }

        public boolean canStart() {
            return BeardedFoxEntity.this.isTouchingWater() && BeardedFoxEntity.this.getFluidHeight(FluidTags.WATER) > 0.25D || BeardedFoxEntity.this.isInLava();
        }
    }

    class GoToVillageGoal extends net.minecraft.entity.ai.goal.GoToVillageGoal {
        public GoToVillageGoal(int unused, int searchRange) {
            super(BeardedFoxEntity.this, searchRange);
        }

        public void start() {
            BeardedFoxEntity.this.stopActions();
            super.start();
        }

        public boolean canStart() {
            return super.canStart() && this.canGoToVillage();
        }

        public boolean shouldContinue() {
            return super.shouldContinue() && this.canGoToVillage();
        }

        private boolean canGoToVillage() {
            return !BeardedFoxEntity.this.isSleeping() && !BeardedFoxEntity.this.isSitting() && !BeardedFoxEntity.this.isAggressive() && BeardedFoxEntity.this.getTarget() == null;
        }
    }

    class EscapeWhenNotAggressiveGoal extends EscapeDangerGoal {
        public EscapeWhenNotAggressiveGoal(double speed) {
            super(BeardedFoxEntity.this, speed);
        }

        public boolean canStart() {
            return !BeardedFoxEntity.this.isAggressive() && super.canStart();
        }
    }

    class StopWanderingGoal extends Goal {
        int timer;

        public StopWanderingGoal() {
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.JUMP, Goal.Control.MOVE));
        }

        public boolean canStart() {
            return BeardedFoxEntity.this.isWalking();
        }

        public boolean shouldContinue() {
            return this.canStart() && this.timer > 0;
        }

        public void start() {
            this.timer = 40;
        }

        public void stop() {
            BeardedFoxEntity.this.setWalking(false);
        }

        public void tick() {
            --this.timer;
        }
    }

    public static class BeardedFoxData extends PassiveEntity.PassiveData {
        public final Type type;

        public BeardedFoxData(BeardedFoxEntity.Type type) {
            super(false);
            this.type = type;
        }
    }

    public class EatSweetBerriesGoal extends MoveToTargetPosGoal {
        protected int timer;

        public EatSweetBerriesGoal(double speed, int range, int maxYDifference) {
            super(BeardedFoxEntity.this, speed, range, maxYDifference);
        }

        public double getDesiredSquaredDistanceToTarget() {
            return 2.0D;
        }

        public boolean shouldResetPath() {
            return this.tryingTime % 100 == 0;
        }

        protected boolean isTargetPos(WorldView world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            return blockState.isOf(Blocks.SWEET_BERRY_BUSH) && (Integer)blockState.get(SweetBerryBushBlock.AGE) >= 2;
        }

        public void tick() {
            if (this.hasReached()) {
                if (this.timer >= 40) {
                    this.eatSweetBerry();
                } else {
                    ++this.timer;
                }
            } else if (!this.hasReached() && BeardedFoxEntity.this.random.nextFloat() < 0.05F) {
                BeardedFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_SNIFF, 1.0F, 1.0F);
            }

            super.tick();
        }

        protected void eatSweetBerry() {
            if (BeardedFoxEntity.this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                BlockState blockState = BeardedFoxEntity.this.world.getBlockState(this.targetPos);
                if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
                    int i = (Integer)blockState.get(SweetBerryBushBlock.AGE);
                    blockState.with(SweetBerryBushBlock.AGE, 1);
                    int j = 1 + BeardedFoxEntity.this.world.random.nextInt(2) + (i == 3 ? 1 : 0);
                    ItemStack itemStack = BeardedFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
                    if (itemStack.isEmpty()) {
                        BeardedFoxEntity.this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
                        --j;
                    }

                    if (j > 0) {
                        Block.dropStack(BeardedFoxEntity.this.world, this.targetPos, new ItemStack(Items.SWEET_BERRIES, j));
                    }

                    BeardedFoxEntity.this.playSound(SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, 1.0F, 1.0F);
                    BeardedFoxEntity.this.world.setBlockState(this.targetPos, (BlockState)blockState.with(SweetBerryBushBlock.AGE, 1), 2);
                }
            }
        }

        public boolean canStart() {
            return !BeardedFoxEntity.this.isSleeping() && super.canStart();
        }

        public void start() {
            this.timer = 0;
            BeardedFoxEntity.this.setSitting(false);
            super.start();
        }
    }

    class SitDownAndLookAroundGoal extends BeardedFoxEntity.CalmDownGoal {
        private double lookX;
        private double lookZ;
        private int timer;
        private int counter;

        public SitDownAndLookAroundGoal() {
            super();
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        public boolean canStart() {
            return BeardedFoxEntity.this.getAttacker() == null && BeardedFoxEntity.this.getRandom().nextFloat() < 0.02F && !BeardedFoxEntity.this.isSleeping() && BeardedFoxEntity.this.getTarget() == null && BeardedFoxEntity.this.getNavigation().isIdle() && !this.canCalmDown() && !BeardedFoxEntity.this.isChasing() && !BeardedFoxEntity.this.isInSneakingPose();
        }

        public boolean shouldContinue() {
            return this.counter > 0;
        }

        public void start() {
            this.chooseNewAngle();
            this.counter = 2 + BeardedFoxEntity.this.getRandom().nextInt(3);
            BeardedFoxEntity.this.setSitting(true);
            BeardedFoxEntity.this.getNavigation().stop();
        }

        public void stop() {
            BeardedFoxEntity.this.setSitting(false);
        }

        public void tick() {
            --this.timer;
            if (this.timer <= 0) {
                --this.counter;
                this.chooseNewAngle();
            }

            BeardedFoxEntity.this.getLookControl().lookAt(BeardedFoxEntity.this.getX() + this.lookX, BeardedFoxEntity.this.getEyeY(), BeardedFoxEntity.this.getZ() + this.lookZ, (float)BeardedFoxEntity.this.getBodyYawSpeed(), (float)BeardedFoxEntity.this.getLookPitchSpeed());
        }

        private void chooseNewAngle() {
            double d = 6.283185307179586D * BeardedFoxEntity.this.getRandom().nextDouble();
            this.lookX = Math.cos(d);
            this.lookZ = Math.sin(d);
            this.timer = 80 + BeardedFoxEntity.this.getRandom().nextInt(20);
        }
    }

    class DelayedCalmDownGoal extends BeardedFoxEntity.CalmDownGoal {
        private int timer;

        public DelayedCalmDownGoal() {
            super();
            this.timer = BeardedFoxEntity.this.random.nextInt(140);
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
        }

        public boolean canStart() {
            if (BeardedFoxEntity.this.sidewaysSpeed == 0.0F && BeardedFoxEntity.this.upwardSpeed == 0.0F && BeardedFoxEntity.this.forwardSpeed == 0.0F) {
                return this.canNotCalmDown() || BeardedFoxEntity.this.isSleeping();
            } else {
                return false;
            }
        }

        public boolean shouldContinue() {
            return this.canNotCalmDown();
        }

        private boolean canNotCalmDown() {
            if (this.timer > 0) {
                --this.timer;
                return false;
            } else {
                return BeardedFoxEntity.this.world.isDay() && this.isAtFavoredLocation() && !this.canCalmDown();
            }
        }

        public void stop() {
            this.timer = BeardedFoxEntity.this.random.nextInt(140);
            BeardedFoxEntity.this.stopActions();
        }

        public void start() {
            BeardedFoxEntity.this.setSitting(false);
            BeardedFoxEntity.this.setCrouching(false);
            BeardedFoxEntity.this.setRollingHead(false);
            BeardedFoxEntity.this.setJumping(false);
            BeardedFoxEntity.this.setSleeping(true);
            BeardedFoxEntity.this.getNavigation().stop();
            BeardedFoxEntity.this.getMoveControl().moveTo(BeardedFoxEntity.this.getX(), BeardedFoxEntity.this.getY(), BeardedFoxEntity.this.getZ(), 0.0D);
        }
    }

    abstract class CalmDownGoal extends Goal {
        private final TargetPredicate WORRIABLE_ENTITY_PREDICATE;

        private CalmDownGoal() {
            this.WORRIABLE_ENTITY_PREDICATE = (new TargetPredicate()).setBaseMaxDistance(12.0D).includeHidden().setPredicate(BeardedFoxEntity.this.new WorriableEntityFilter());
        }

        protected boolean isAtFavoredLocation() {
            BlockPos blockPos = new BlockPos(BeardedFoxEntity.this.getX(), BeardedFoxEntity.this.getBoundingBox().maxY, BeardedFoxEntity.this.getZ());
            return !BeardedFoxEntity.this.world.isSkyVisible(blockPos) && BeardedFoxEntity.this.getPathfindingFavor(blockPos) >= 0.0F;
        }

        protected boolean canCalmDown() {
            return !BeardedFoxEntity.this.world.getTargets(LivingEntity.class, this.WORRIABLE_ENTITY_PREDICATE, BeardedFoxEntity.this, BeardedFoxEntity.this.getBoundingBox().expand(12.0D, 6.0D, 12.0D)).isEmpty();
        }
    }

    public class WorriableEntityFilter implements Predicate<LivingEntity> {
        public boolean test(LivingEntity livingEntity) {
            if (livingEntity instanceof BeardedFoxEntity) {
                return false;
            } else if (!(livingEntity instanceof ChickenEntity) && !(livingEntity instanceof RabbitEntity) && !(livingEntity instanceof HostileEntity)) {
                if (livingEntity instanceof TameableEntity) {
                    return !((TameableEntity)livingEntity).isTamed();
                } else if (livingEntity instanceof PlayerEntity && (livingEntity.isSpectator() || ((PlayerEntity)livingEntity).isCreative())) {
                    return false;
                } else if (BeardedFoxEntity.this.canTrust(livingEntity.getUuid())) {
                    return false;
                } else {
                    return !livingEntity.isSleeping() && !livingEntity.isSneaky();
                }
            } else {
                return true;
            }
        }
    }

    class AvoidDaylightGoal extends EscapeSunlightGoal {
        private int timer = 100;

        public AvoidDaylightGoal(double speed) {
            super(BeardedFoxEntity.this, speed);
        }

        public boolean canStart() {
            if (!BeardedFoxEntity.this.isSleeping() && this.mob.getTarget() == null) {
                if (BeardedFoxEntity.this.world.isThundering()) {
                    return true;
                } else if (this.timer > 0) {
                    --this.timer;
                    return false;
                } else {
                    this.timer = 100;
                    BlockPos blockPos = this.mob.getBlockPos();
                    return BeardedFoxEntity.this.world.isDay() && BeardedFoxEntity.this.world.isSkyVisible(blockPos) && !((ServerWorld)BeardedFoxEntity.this.world).isNearOccupiedPointOfInterest(blockPos) && this.targetShadedPos();
                }
            } else {
                return false;
            }
        }

        public void start() {
            BeardedFoxEntity.this.stopActions();
            super.start();
        }
    }

    class DefendFriendGoal extends FollowTargetGoal<LivingEntity> {
        /*@Nullable*/
        private LivingEntity offender;
        private LivingEntity friend;
        private int lastAttackedTime;

        public DefendFriendGoal(Class<LivingEntity> targetEntityClass, boolean checkVisibility, boolean checkCanNavigate, /*@Nullable*/ Predicate<LivingEntity> targetPredicate) {
            super(BeardedFoxEntity.this, targetEntityClass, 10, checkVisibility, checkCanNavigate, targetPredicate);
        }

        public boolean canStart() {
            if (this.reciprocalChance > 0 && this.mob.getRandom().nextInt(this.reciprocalChance) != 0) {
                return false;
            } else {
                Iterator var1 = BeardedFoxEntity.this.getTrustedUuids().iterator();

                while(var1.hasNext()) {
                    UUID uUID = (UUID)var1.next();
                    if (uUID != null && BeardedFoxEntity.this.world instanceof ServerWorld) {
                        Entity entity = ((ServerWorld)BeardedFoxEntity.this.world).getEntity(uUID);
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity)entity;
                            this.friend = livingEntity;
                            this.offender = livingEntity.getAttacker();
                            int i = livingEntity.getLastAttackedTime();
                            return i != this.lastAttackedTime && this.canTrack(this.offender, this.targetPredicate);
                        }
                    }
                }

                return false;
            }
        }

        public void start() {
            this.setTargetEntity(this.offender);
            this.targetEntity = this.offender;
            if (this.friend != null) {
                this.lastAttackedTime = this.friend.getLastAttackedTime();
            }

            BeardedFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
            BeardedFoxEntity.this.setAggressive(true);
            BeardedFoxEntity.this.stopSleeping();
            super.start();
        }
    }

    class MateGoal extends AnimalMateGoal {
        public MateGoal(double chance) {
            super(BeardedFoxEntity.this, chance);
        }

        public void start() {
            ((BeardedFoxEntity)this.animal).stopActions();
            ((BeardedFoxEntity)this.mate).stopActions();
            super.start();
        }

        protected void breed() {
            ServerWorld serverWorld = (ServerWorld)this.world;
            BeardedFoxEntity foxEntity = (BeardedFoxEntity)this.animal.createChild(serverWorld, this.mate);
            if (foxEntity != null) {
                ServerPlayerEntity serverPlayerEntity = this.animal.getLovingPlayer();
                ServerPlayerEntity serverPlayerEntity2 = this.mate.getLovingPlayer();
                ServerPlayerEntity serverPlayerEntity3 = serverPlayerEntity;
                if (serverPlayerEntity != null) {
                    foxEntity.addTrustedUuid(serverPlayerEntity.getUuid());
                } else {
                    serverPlayerEntity3 = serverPlayerEntity2;
                }

                if (serverPlayerEntity2 != null && serverPlayerEntity != serverPlayerEntity2) {
                    foxEntity.addTrustedUuid(serverPlayerEntity2.getUuid());
                }

                if (serverPlayerEntity3 != null) {
                    serverPlayerEntity3.incrementStat(Stats.ANIMALS_BRED);
                    Criteria.BRED_ANIMALS.trigger(serverPlayerEntity3, this.animal, this.mate, foxEntity);
                }

                this.animal.setBreedingAge(6000);
                this.mate.setBreedingAge(6000);
                this.animal.resetLoveTicks();
                this.mate.resetLoveTicks();
                foxEntity.setBreedingAge(-24000);
                foxEntity.refreshPositionAndAngles(this.animal.getX(), this.animal.getY(), this.animal.getZ(), 0.0F, 0.0F);
                serverWorld.spawnEntityAndPassengers(foxEntity);
                this.world.sendEntityStatus(this.animal, (byte)18);
                if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                    this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
                }

            }
        }
    }

    class AttackGoal extends MeleeAttackGoal {
        public AttackGoal(double speed, boolean pauseWhenIdle) {
            super(BeardedFoxEntity.this, speed, pauseWhenIdle);
        }

        protected void attack(LivingEntity target, double squaredDistance) {
            double d = this.getSquaredMaxAttackDistance(target);
            if (squaredDistance <= d && this.method_28347()) {
                this.method_28346();
                this.mob.tryAttack(target);
                BeardedFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_BITE, 1.0F, 1.0F);
            }

        }

        public void start() {
            BeardedFoxEntity.this.setRollingHead(false);
            super.start();
        }

        public boolean canStart() {
            return !BeardedFoxEntity.this.isSitting() && !BeardedFoxEntity.this.isSleeping() && !BeardedFoxEntity.this.isInSneakingPose() && !BeardedFoxEntity.this.isWalking() && super.canStart();
        }
    }

    class MoveToHuntGoal extends Goal {
        public MoveToHuntGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        public boolean canStart() {
            if (BeardedFoxEntity.this.isSleeping()) {
                return false;
            } else {
                LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
                return livingEntity != null && livingEntity.isAlive() && BeardedFoxEntity.CHICKEN_AND_RABBIT_FILTER.test(livingEntity) && BeardedFoxEntity.this.squaredDistanceTo(livingEntity) > 36.0D && !BeardedFoxEntity.this.isInSneakingPose() && !BeardedFoxEntity.this.isRollingHead() && !BeardedFoxEntity.this.jumping;
            }
        }

        public void start() {
            BeardedFoxEntity.this.setSitting(false);
            BeardedFoxEntity.this.setWalking(false);
        }

        public void stop() {
            LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
            if (livingEntity != null && BeardedFoxEntity.canJumpChase(BeardedFoxEntity.this, livingEntity)) {
                BeardedFoxEntity.this.setRollingHead(true);
                BeardedFoxEntity.this.setCrouching(true);
                BeardedFoxEntity.this.getNavigation().stop();
                BeardedFoxEntity.this.getLookControl().lookAt(livingEntity, (float)BeardedFoxEntity.this.getBodyYawSpeed(), (float)BeardedFoxEntity.this.getLookPitchSpeed());
            } else {
                BeardedFoxEntity.this.setRollingHead(false);
                BeardedFoxEntity.this.setCrouching(false);
            }

        }

        public void tick() {
            LivingEntity livingEntity = BeardedFoxEntity.this.getTarget();
            BeardedFoxEntity.this.getLookControl().lookAt(livingEntity, (float)BeardedFoxEntity.this.getBodyYawSpeed(), (float)BeardedFoxEntity.this.getLookPitchSpeed());
            if (BeardedFoxEntity.this.squaredDistanceTo(livingEntity) <= 36.0D) {
                BeardedFoxEntity.this.setRollingHead(true);
                BeardedFoxEntity.this.setCrouching(true);
                BeardedFoxEntity.this.getNavigation().stop();
            } else {
                BeardedFoxEntity.this.getNavigation().startMovingTo(livingEntity, 1.5D);
            }

        }
    }

    class FoxMoveControl extends MoveControl {
        public FoxMoveControl() {
            super(BeardedFoxEntity.this);
        }

        public void tick() {
            if (BeardedFoxEntity.this.wantsToPickupItem()) {
                super.tick();
            }

        }
    }

    class PickupItemGoal extends Goal {
        public PickupItemGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        public boolean canStart() {
            if (!BeardedFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
                return false;
            } else if (BeardedFoxEntity.this.getTarget() == null && BeardedFoxEntity.this.getAttacker() == null) {
                if (!BeardedFoxEntity.this.wantsToPickupItem()) {
                    return false;
                } else if (BeardedFoxEntity.this.getRandom().nextInt(10) != 0) {
                    return false;
                } else {
                    List<ItemEntity> list = BeardedFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, BeardedFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), BeardedFoxEntity.PICKABLE_DROP_FILTER);
                    return !list.isEmpty() && BeardedFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty();
                }
            } else {
                return false;
            }
        }

        public void tick() {
            List<ItemEntity> list = BeardedFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, BeardedFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), BeardedFoxEntity.PICKABLE_DROP_FILTER);
            ItemStack itemStack = BeardedFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
            if (itemStack.isEmpty() && !list.isEmpty()) {
                BeardedFoxEntity.this.getNavigation().startMovingTo((Entity)list.get(0), 1.2000000476837158D);
            }

        }

        public void start() {
            List<ItemEntity> list = BeardedFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, BeardedFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), BeardedFoxEntity.PICKABLE_DROP_FILTER);
            if (!list.isEmpty()) {
                BeardedFoxEntity.this.getNavigation().startMovingTo((Entity)list.get(0), 1.2000000476837158D);
            }

        }
    }

    public static enum Type {
        CYAN(0, "cyan", BiomeKeys.BIRCH_FOREST, BiomeKeys.BIRCH_FOREST_HILLS, BiomeKeys.TALL_BIRCH_FOREST, BiomeKeys.TALL_BIRCH_HILLS, BiomeKeys.FOREST, BiomeKeys.WOODED_HILLS),
        RED(1, "red", BiomeKeys.BADLANDS, BiomeKeys.BADLANDS_PLATEAU, BiomeKeys.ERODED_BADLANDS, BiomeKeys.MODIFIED_BADLANDS_PLATEAU, BiomeKeys.MODIFIED_WOODED_BADLANDS_PLATEAU, BiomeKeys.WOODED_BADLANDS_PLATEAU),
        WHITE(2, "white", BiomeKeys.SNOWY_MOUNTAINS, BiomeKeys.SNOWY_TAIGA_MOUNTAINS),
        GREEN(3, "green", BiomeKeys.SWAMP, BiomeKeys.SWAMP_HILLS),
        GRAY(4, "gray", BiomeKeys.MOUNTAINS, BiomeKeys.GRAVELLY_MOUNTAINS, BiomeKeys.MODIFIED_GRAVELLY_MOUNTAINS, BiomeKeys.WOODED_MOUNTAINS),
        BROWN(5, "brown", BiomeKeys.DARK_FOREST, BiomeKeys.DARK_FOREST_HILLS); // that's all I'm going for at the moment
        /*LIME(6, "lime", BiomeKeys.FLOWER_FOREST, BiomeKeys.SUNFLOWER_PLAINS),
        YELLOW(7, "yellow", BiomeKeys.DESERT, BiomeKeys.DESERT_LAKES, BiomeKeys.DESERT_HILLS);*/

        private static final BeardedFoxEntity.Type[] TYPES = (BeardedFoxEntity.Type[]) Arrays.stream(values()).sorted(Comparator.comparingInt(BeardedFoxEntity.Type::getId)).toArray((i) -> {
            return new BeardedFoxEntity.Type[i];
        });
        private static final Map<String, BeardedFoxEntity.Type> NAME_TYPE_MAP = (Map)Arrays.stream(values()).collect(Collectors.toMap(BeardedFoxEntity.Type::getKey, (type) -> {
            return type;
        }));
        private final int id;
        private final String key;
        private final List<RegistryKey<Biome>> biomes;

        Type(int id, String key, RegistryKey<Biome>... registryKeys) {
            this.id = id;
            this.key = key;
            this.biomes = Arrays.asList(registryKeys);
        }

        public String getKey() {
            return this.key;
        }

        public int getId() {
            return this.id;
        }

        public List<RegistryKey<Biome>> getBiomes() {
            return this.biomes;
        }

        // throws Unsupported Operation
        /*public static List<RegistryKey<Biome>> getAllBiomes() {
            List<RegistryKey<Biome>> biomes = Type.CYAN.getBiomes();
            // this threw exceptions
            *//*biomes.addAll(RED.getBiomes());
            biomes.addAll(WHITE.getBiomes());
            biomes.addAll(GREEN.getBiomes());
            biomes.addAll(GRAY.getBiomes());
            biomes.addAll(BROWN.getBiomes());*//*

            for(int i = 1; i < TYPES.length; i++) {
                Type t = fromId(i);
                for(RegistryKey<Biome> key: t.biomes) {
                    biomes.add(key);
                }
            }
            return biomes;
        }*/

        public static BeardedFoxEntity.Type byName(String name) {
            return (BeardedFoxEntity.Type)NAME_TYPE_MAP.getOrDefault(name, CYAN);
        }

        public static BeardedFoxEntity.Type fromId(int id) {
            if (id < 0 || id > TYPES.length) {
                id = 0;
            }

            return TYPES[id];
        }

        public static BeardedFoxEntity.Type fromBiome(Optional<RegistryKey<Biome>> optional) {
            if(optional.isPresent()) {
                if(RED.biomes.contains(optional.get())) {
                    return RED;
                }
                else if(WHITE.biomes.contains(optional.get())) {
                    return WHITE;
                }
                else if(GREEN.biomes.contains(optional.get())) {
                    return GREEN;
                }
                else if(GRAY.biomes.contains(optional.get())) {
                    return GRAY;
                }
                else if(BROWN.biomes.contains(optional.get())) {
                    return BROWN;
                }
                else {
                    return CYAN;
                }
            } else {
                return CYAN; // just in case of weirdness
            }
        }
    }
}
