package com.guysmith.beardedfox.entity;

import com.google.common.collect.Lists;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//import com.sun.istack.internal.Nullable;
import com.guysmith.beardedfox.registry.ModEntityTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.DiveJumpingGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class DivineFoxEntity extends AnimalEntity {
    private static final TrackedData<Integer> TYPE;
    private static final TrackedData<Byte> DIVINE_FOX_FLAGS;
    private static final TrackedData<Optional<UUID>> FRIEND;
    private static final TrackedData<Optional<UUID>> OTHER_TRUSTED;
    private static final Predicate<ItemEntity> PICKABLE_DROP_FILTER;
    private static final Predicate<Entity> JUST_ATTACKED_SOMETHING_FILTER;
    private static final Predicate<Entity> NOTICEABLE_PLAYER_FILTER;
    private float headRollProgress;
    private float lastHeadRollProgress;
    private float extraRollingHeight;
    private float lastExtraRollingHeight;
    private int eatingTime;

    /*
    *
    * Notes:
    *   - Divine Foxes do not seek out their own food or a dark place to sleep
    *       (because they needn't do either)
    *   - They come in all colours
    *   - Glow?
    *   - They do not hunt, pounce, sleep, or run away from players*
    *   - They provide/do things based on the reason they were summoned
    *
    *   *: They do run away from unfamiliar/unfriendly players, like those who hit them,
    *        or those who are unkind to foxes
    *
    * Maybes:
    *   - Divine Foxes will spawn based on Bearded Fox happiness?
    *   - They don't breed? Probably.
    *   - Can be coaxed/bribed with offerings of sweet berries and chicken
    *   - Mischievous? Probably. Still cute tho.
    *
    * */

    // the IDE complains but it seems to compile without; saved in comments in case I ever actually need it
    /*public DivineFoxEntity(World world) {
        super(ModEntityTypes.DIVINE_FOX, world);
        this.lookControl = new DivineFoxEntity.FoxLookControl();
        this.moveControl = new DivineFoxEntity.FoxMoveControl();
        this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }*/

    public DivineFoxEntity(EntityType<? extends DivineFoxEntity> entityType, World world) {
        super(entityType, world);
        this.lookControl = new DivineFoxEntity.FoxLookControl();
        this.moveControl = new DivineFoxEntity.FoxMoveControl();
        this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FRIEND, Optional.empty());
        this.dataTracker.startTracking(OTHER_TRUSTED, Optional.empty());
        this.dataTracker.startTracking(TYPE, 0);
        this.dataTracker.startTracking(DIVINE_FOX_FLAGS, (byte)0);
    }

    protected void initGoals() {
        this.goalSelector.add(0, new DivineFoxEntity.FoxSwimGoal());
        this.goalSelector.add(1, new DivineFoxEntity.StopWanderingGoal());
        this.goalSelector.add(2, new DivineFoxEntity.EscapeWhenNotAggressiveGoal(2.2D));
        this.goalSelector.add(4, new FleeEntityGoal(this, PlayerEntity.class, 16.0F, 1.6D, 1.4D, (livingEntity) -> NOTICEABLE_PLAYER_FILTER.test((Entity) livingEntity) && !this.canTrust(((Entity) livingEntity).getUuid()) && !this.isAggressive()));
        this.goalSelector.add(4, new FleeEntityGoal(this, WolfEntity.class, 8.0F, 1.6D, 1.4D, (livingEntity) -> !((WolfEntity)livingEntity).isTamed() && !this.isAggressive()));
        this.goalSelector.add(4, new FleeEntityGoal(this, PolarBearEntity.class, 8.0F, 1.6D, 1.4D, (livingEntity) -> !this.isAggressive()));
        this.goalSelector.add(6, new DivineFoxEntity.JumpChasingGoal());
        this.goalSelector.add(7, new DivineFoxEntity.AttackGoal(1.2000000476837158D, true));
        this.goalSelector.add(7, new DivineFoxEntity.DelayedCalmDownGoal());
        this.goalSelector.add(11, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(11, new DivineFoxEntity.PickupItemGoal());
        this.goalSelector.add(12, new DivineFoxEntity.LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
        this.goalSelector.add(13, new DivineFoxEntity.SitDownAndLookAroundGoal());
        this.targetSelector.add(3, new DivineFoxEntity.DefendFriendGoal(LivingEntity.class, false, false, (livingEntity) -> JUST_ATTACKED_SOMETHING_FILTER.test(livingEntity) && !this.canTrust(livingEntity.getUuid())));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(source == DamageSource.SWEET_BERRY_BUSH) {
            return false;
        } else {
            return super.damage(source, amount);
        }
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

    //    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, /*@Nullable*/ EntityData entityData, /*@Nullable*/ CompoundTag entityTag) {
        //Optional<RegistryKey<Biome>> optional = world.method_31081(this.getBlockPos());
        Random random = new Random();
        DivineFoxEntity.Type type = DivineFoxEntity.Type.fromId(random.nextInt(15));
        boolean bl = false;
        if (entityData instanceof DivineFoxData) {
            type = ((DivineFoxData)entityData).type;
            if (((DivineFoxData)entityData).getSpawnedCount() >= 2) {
                bl = true;
            }
        } else {
            entityData = new DivineFoxData(type);
        }

        this.setType(type);
        if (bl) {
            this.setBreedingAge(-24000);
        }

        if (world instanceof ServerWorld) {
            this.addSpecificGoals();
        }

        this.initEquipment(difficulty);
        return super.initialize(world, difficulty, spawnReason, (EntityData)entityData, entityTag);
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        // shouldn't be accessible
        return null;
    }

    private void addSpecificGoals() {
        // will be specific things that Divine Foxes do to help or hinder you
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

    public DivineFoxEntity.Type getFoxType() {
        return DivineFoxEntity.Type.fromId((Integer)this.dataTracker.get(TYPE));
    }

    private void setType(DivineFoxEntity.Type type) {
        this.dataTracker.set(TYPE, type.getId());
    }

    private List<UUID> getTrustedUuids() {
        List<UUID> list = Lists.newArrayList();
        list.add((UUID) ((Optional)this.dataTracker.get(FRIEND)).orElse((Object)null));
        list.add((UUID) ((Optional)this.dataTracker.get(OTHER_TRUSTED)).orElse((Object)null));
        return list;
    }

    private void addTrustedUuid(/*@Nullable*/ UUID uuid) {
        if (((Optional)this.dataTracker.get(FRIEND)).isPresent()) {
            this.dataTracker.set(OTHER_TRUSTED, Optional.ofNullable(uuid));
        } else {
            this.dataTracker.set(FRIEND, Optional.ofNullable(uuid));
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
        this.setType(DivineFoxEntity.Type.byName(tag.getString("Type")));
        this.setSitting(tag.getBoolean("Sitting"));
        this.setCrouching(tag.getBoolean("Crouching"));
        if (this.world instanceof ServerWorld) {
            this.addSpecificGoals();
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
            this.dataTracker.set(DIVINE_FOX_FLAGS, (byte)((Byte)this.dataTracker.get(DIVINE_FOX_FLAGS) | mask));
        } else {
            this.dataTracker.set(DIVINE_FOX_FLAGS, (byte)((Byte)this.dataTracker.get(DIVINE_FOX_FLAGS) & ~mask));
        }

    }

    private boolean getFoxFlag(int bitmask) {
        return ((Byte)this.dataTracker.get(DIVINE_FOX_FLAGS) & bitmask) != 0;
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
        ((DivineFoxEntity)child).addTrustedUuid(player.getUuid());
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

    public static boolean canJumpChase(DivineFoxEntity fox, LivingEntity chasedEntity) {
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
        TYPE = DataTracker.registerData(DivineFoxEntity.class, TrackedDataHandlerRegistry.INTEGER);
        DIVINE_FOX_FLAGS = DataTracker.registerData(DivineFoxEntity.class, TrackedDataHandlerRegistry.BYTE);
        FRIEND = DataTracker.registerData(DivineFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        OTHER_TRUSTED = DataTracker.registerData(DivineFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        PICKABLE_DROP_FILTER = (itemEntity) -> !itemEntity.cannotPickup() && itemEntity.isAlive();
        JUST_ATTACKED_SOMETHING_FILTER = (entity) -> {
            if (!(entity instanceof LivingEntity)) {
                return false;
            } else {
                LivingEntity livingEntity = (LivingEntity)entity;
                return livingEntity.getAttacking() != null && livingEntity.getLastAttackTime() < livingEntity.age + 600;
            }
        };
        NOTICEABLE_PLAYER_FILTER = (entity) -> !entity.isSneaky() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity);
    }

    class LookAtEntityGoal extends net.minecraft.entity.ai.goal.LookAtEntityGoal {
        public LookAtEntityGoal(MobEntity fox, Class<? extends LivingEntity> targetType, float range) {
            super(fox, targetType, range);
        }

        public boolean canStart() {
            return super.canStart()
                    && !DivineFoxEntity.this.isWalking()
                    && !DivineFoxEntity.this.isRollingHead();
        }

        public boolean shouldContinue() {
            return super.shouldContinue()
                    && !DivineFoxEntity.this.isWalking()
                    && !DivineFoxEntity.this.isRollingHead();
        }
    }

    public class FoxLookControl extends LookControl {
        public FoxLookControl() {
            super(DivineFoxEntity.this);
        }

        public void tick() {
            if (!DivineFoxEntity.this.isSleeping()) {
                super.tick();
            }

        }

        protected boolean shouldStayHorizontal() {
            return !DivineFoxEntity.this.isChasing()
                    && !DivineFoxEntity.this.isInSneakingPose()
                    && !DivineFoxEntity.this.isRollingHead()
                    & !DivineFoxEntity.this.isWalking();
        }
    }

    public class JumpChasingGoal extends DiveJumpingGoal {
        public boolean canStart() {
            if (!DivineFoxEntity.this.isFullyCrouched()) {
                return false;
            } else {
                LivingEntity livingEntity = DivineFoxEntity.this.getTarget();
                if (livingEntity != null && livingEntity.isAlive()) {
                    if (livingEntity.getMovementDirection() != livingEntity.getHorizontalFacing()) {
                        return false;
                    } else {
                        boolean bl = DivineFoxEntity.canJumpChase(DivineFoxEntity.this, livingEntity);
                        if (!bl) {
                            DivineFoxEntity.this.getNavigation().findPathTo((Entity)livingEntity, 0);
                            DivineFoxEntity.this.setCrouching(false);
                            DivineFoxEntity.this.setRollingHead(false);
                        }

                        return bl;
                    }
                } else {
                    return false;
                }
            }
        }

        public boolean shouldContinue() {
            LivingEntity livingEntity = DivineFoxEntity.this.getTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                double d = DivineFoxEntity.this.getVelocity().y;
                return (d * d >= 0.05000000074505806D
                        || Math.abs(DivineFoxEntity.this.pitch) >= 15.0F
                        || !DivineFoxEntity.this.onGround)
                        && !DivineFoxEntity.this.isWalking();
            } else {
                return false;
            }
        }

        public boolean canStop() {
            return false;
        }

        public void start() {
            DivineFoxEntity.this.setJumping(true);
            DivineFoxEntity.this.setChasing(true);
            DivineFoxEntity.this.setRollingHead(false);
            LivingEntity livingEntity = DivineFoxEntity.this.getTarget();
            DivineFoxEntity.this.getLookControl().lookAt(livingEntity, 60.0F, 30.0F);
            Vec3d vec3d = (new Vec3d(livingEntity.getX() - DivineFoxEntity.this.getX(),
                                     livingEntity.getY() - DivineFoxEntity.this.getY(),
                                     livingEntity.getZ() - DivineFoxEntity.this.getZ())).normalize();
            DivineFoxEntity.this.setVelocity(DivineFoxEntity.this.getVelocity().add(vec3d.x * 0.8D, 0.9D, vec3d.z * 0.8D));
            DivineFoxEntity.this.getNavigation().stop();
        }

        public void stop() {
            DivineFoxEntity.this.setCrouching(false);
            DivineFoxEntity.this.extraRollingHeight = 0.0F;
            DivineFoxEntity.this.lastExtraRollingHeight = 0.0F;
            DivineFoxEntity.this.setRollingHead(false);
            DivineFoxEntity.this.setChasing(false);
        }

        public void tick() {
            LivingEntity livingEntity = DivineFoxEntity.this.getTarget();
            if (livingEntity != null) {
                DivineFoxEntity.this.getLookControl().lookAt(livingEntity, 60.0F, 30.0F);
            }

            if (!DivineFoxEntity.this.isWalking()) {
                Vec3d vec3d = DivineFoxEntity.this.getVelocity();
                if (vec3d.y * vec3d.y < 0.029999999329447746D && DivineFoxEntity.this.pitch != 0.0F) {
                    DivineFoxEntity.this.pitch = MathHelper.lerpAngle(DivineFoxEntity.this.pitch, 0.0F, 0.2F);
                } else {
                    double d = Math.sqrt(Entity.squaredHorizontalLength(vec3d));
                    double e = Math.signum(-vec3d.y) * Math.acos(d / vec3d.length()) * 57.2957763671875D;
                    DivineFoxEntity.this.pitch = (float)e;
                }
            }

            if (livingEntity != null && DivineFoxEntity.this.distanceTo(livingEntity) <= 2.0F) {
                DivineFoxEntity.this.tryAttack(livingEntity);
            } else if (DivineFoxEntity.this.pitch > 0.0F
                    && DivineFoxEntity.this.onGround
                    && (float)DivineFoxEntity.this.getVelocity().y != 0.0F
                    && DivineFoxEntity.this.world.getBlockState(DivineFoxEntity.this.getBlockPos()).isOf(Blocks.SNOW)) {
                DivineFoxEntity.this.pitch = 60.0F;
                DivineFoxEntity.this.setTarget((LivingEntity)null);
                DivineFoxEntity.this.setWalking(true);
            }

        }
    }

    class FoxSwimGoal extends SwimGoal {
        public FoxSwimGoal() {
            super(DivineFoxEntity.this);
        }

        public void start() {
            super.start();
            DivineFoxEntity.this.stopActions();
        }

        public boolean canStart() {
            return DivineFoxEntity.this.isTouchingWater() && DivineFoxEntity.this.getFluidHeight(FluidTags.WATER) > 0.25D || DivineFoxEntity.this.isInLava();
        }
    }

    class EscapeWhenNotAggressiveGoal extends EscapeDangerGoal {
        public EscapeWhenNotAggressiveGoal(double speed) {
            super(DivineFoxEntity.this, speed);
        }

        public boolean canStart() {
            return !DivineFoxEntity.this.isAggressive() && super.canStart();
        }
    }

    class StopWanderingGoal extends Goal {
        int timer;

        public StopWanderingGoal() {
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.JUMP, Goal.Control.MOVE));
        }

        public boolean canStart() {
            return DivineFoxEntity.this.isWalking();
        }

        public boolean shouldContinue() {
            return this.canStart() && this.timer > 0;
        }

        public void start() {
            this.timer = 40;
        }

        public void stop() {
            DivineFoxEntity.this.setWalking(false);
        }

        public void tick() {
            --this.timer;
        }
    }

    public static class DivineFoxData extends PassiveEntity.PassiveData {
        public final Type type;

        public DivineFoxData(DivineFoxEntity.Type type) {
            super(false);
            this.type = type;
        }
    }

    class SitDownAndLookAroundGoal extends DivineFoxEntity.CalmDownGoal {
        private double lookX;
        private double lookZ;
        private int timer;
        private int counter;

        public SitDownAndLookAroundGoal() {
            super();
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        public boolean canStart() {
            return DivineFoxEntity.this.getAttacker() == null
                    && DivineFoxEntity.this.getRandom().nextFloat() < 0.02F
                    && !DivineFoxEntity.this.isSleeping()
                    && DivineFoxEntity.this.getTarget() == null
                    && DivineFoxEntity.this.getNavigation().isIdle()
                    && !this.canCalmDown()
                    && !DivineFoxEntity.this.isChasing()
                    && !DivineFoxEntity.this.isInSneakingPose();
        }

        public boolean shouldContinue() {
            return this.counter > 0;
        }

        public void start() {
            this.chooseNewAngle();
            this.counter = 2 + DivineFoxEntity.this.getRandom().nextInt(3);
            DivineFoxEntity.this.setSitting(true);
            DivineFoxEntity.this.getNavigation().stop();
        }

        public void stop() {
            DivineFoxEntity.this.setSitting(false);
        }

        public void tick() {
            --this.timer;
            if (this.timer <= 0) {
                --this.counter;
                this.chooseNewAngle();
            }

            DivineFoxEntity.this.getLookControl().lookAt(DivineFoxEntity.this.getX() + this.lookX, DivineFoxEntity.this.getEyeY(), DivineFoxEntity.this.getZ() + this.lookZ, (float)DivineFoxEntity.this.getBodyYawSpeed(), (float)DivineFoxEntity.this.getLookPitchSpeed());
        }

        private void chooseNewAngle() {
            double d = 6.283185307179586D * DivineFoxEntity.this.getRandom().nextDouble();
            this.lookX = Math.cos(d);
            this.lookZ = Math.sin(d);
            this.timer = 80 + DivineFoxEntity.this.getRandom().nextInt(20);
        }
    }

    class DelayedCalmDownGoal extends DivineFoxEntity.CalmDownGoal {
        private int timer;

        public DelayedCalmDownGoal() {
            super();
            this.timer = DivineFoxEntity.this.random.nextInt(140);
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
        }

        public boolean canStart() {
            if (DivineFoxEntity.this.sidewaysSpeed == 0.0F && DivineFoxEntity.this.upwardSpeed == 0.0F && DivineFoxEntity.this.forwardSpeed == 0.0F) {
                return this.canNotCalmDown() || DivineFoxEntity.this.isSleeping();
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
                return DivineFoxEntity.this.world.isDay() && this.isAtFavoredLocation() && !this.canCalmDown();
            }
        }

        public void stop() {
            this.timer = DivineFoxEntity.this.random.nextInt(140);
            DivineFoxEntity.this.stopActions();
        }

        public void start() {
            DivineFoxEntity.this.setSitting(false);
            DivineFoxEntity.this.setCrouching(false);
            DivineFoxEntity.this.setRollingHead(false);
            DivineFoxEntity.this.setJumping(false);
            DivineFoxEntity.this.setSleeping(true);
            DivineFoxEntity.this.getNavigation().stop();
            DivineFoxEntity.this.getMoveControl().moveTo(DivineFoxEntity.this.getX(), DivineFoxEntity.this.getY(), DivineFoxEntity.this.getZ(), 0.0D);
        }
    }

    abstract class CalmDownGoal extends Goal {
        private final TargetPredicate WORRIABLE_ENTITY_PREDICATE;

        private CalmDownGoal() {
            this.WORRIABLE_ENTITY_PREDICATE = (new TargetPredicate()).setBaseMaxDistance(12.0D).includeHidden().setPredicate(DivineFoxEntity.this.new WorriableEntityFilter());
        }

        protected boolean isAtFavoredLocation() {
            BlockPos blockPos = new BlockPos(DivineFoxEntity.this.getX(), DivineFoxEntity.this.getBoundingBox().maxY, DivineFoxEntity.this.getZ());
            return !DivineFoxEntity.this.world.isSkyVisible(blockPos) && DivineFoxEntity.this.getPathfindingFavor(blockPos) >= 0.0F;
        }

        protected boolean canCalmDown() {
            return !DivineFoxEntity.this.world.getTargets(LivingEntity.class, this.WORRIABLE_ENTITY_PREDICATE, DivineFoxEntity.this, DivineFoxEntity.this.getBoundingBox().expand(12.0D, 6.0D, 12.0D)).isEmpty();
        }
    }

    public class WorriableEntityFilter implements Predicate<LivingEntity> {
        public boolean test(LivingEntity livingEntity) {
            if (livingEntity instanceof BeardedFoxEntity) {
                return false;
            } else if (!(livingEntity instanceof HostileEntity)) {
                if (livingEntity instanceof TameableEntity) {
                    return !((TameableEntity)livingEntity).isTamed();
                } else if (livingEntity instanceof PlayerEntity
                        && (livingEntity.isSpectator()
                            || ((PlayerEntity)livingEntity).isCreative()
                            || ((PlayerEntity)livingEntity).getUuid().toString().equals(FRIEND.toString()))) {
                    return false;
                } else if (DivineFoxEntity.this.canTrust(livingEntity.getUuid())) {
                    return false;
                } else {
                    return !livingEntity.isSleeping() && !livingEntity.isSneaky();
                }
            } else {
                return true;
            }
        }
    }

    class DefendFriendGoal extends FollowTargetGoal<LivingEntity> {
        /*@Nullable*/
        private LivingEntity offender;
        private LivingEntity friend;
        private int lastAttackedTime;

        public DefendFriendGoal(Class<LivingEntity> targetEntityClass, boolean checkVisibility, boolean checkCanNavigate, /*@Nullable*/ Predicate<LivingEntity> targetPredicate) {
            super(DivineFoxEntity.this, targetEntityClass, 10, checkVisibility, checkCanNavigate, targetPredicate);
        }

        public boolean canStart() {
            if (this.reciprocalChance > 0 && this.mob.getRandom().nextInt(this.reciprocalChance) != 0) {
                return false;
            } else {
                Iterator var1 = DivineFoxEntity.this.getTrustedUuids().iterator();

                while(var1.hasNext()) {
                    UUID uUID = (UUID)var1.next();
                    if (uUID != null && DivineFoxEntity.this.world instanceof ServerWorld) {
                        Entity entity = ((ServerWorld)DivineFoxEntity.this.world).getEntity(uUID);
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

            DivineFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
            DivineFoxEntity.this.setAggressive(true);
            DivineFoxEntity.this.stopSleeping();
            super.start();
        }
    }

    class AttackGoal extends MeleeAttackGoal {
        public AttackGoal(double speed, boolean pauseWhenIdle) {
            super(DivineFoxEntity.this, speed, pauseWhenIdle);
        }

        protected void attack(LivingEntity target, double squaredDistance) {
            double d = this.getSquaredMaxAttackDistance(target);
            if (squaredDistance <= d && this.method_28347()) {
                this.method_28346();
                this.mob.tryAttack(target);
                DivineFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_BITE, 1.0F, 1.0F);
            }

        }

        public void start() {
            DivineFoxEntity.this.setRollingHead(false);
            super.start();
        }

        public boolean canStart() {
            return !DivineFoxEntity.this.isSitting() && !DivineFoxEntity.this.isSleeping() && !DivineFoxEntity.this.isInSneakingPose() && !DivineFoxEntity.this.isWalking() && super.canStart();
        }
    }

    class FoxMoveControl extends MoveControl {
        public FoxMoveControl() {
            super(DivineFoxEntity.this);
        }

        public void tick() {
            if (DivineFoxEntity.this.wantsToPickupItem()) {
                super.tick();
            }

        }
    }

    class PickupItemGoal extends Goal {
        public PickupItemGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        public boolean canStart() {
            if (!DivineFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
                return false;
            } else if (DivineFoxEntity.this.getTarget() == null && DivineFoxEntity.this.getAttacker() == null) {
                if (!DivineFoxEntity.this.wantsToPickupItem()) {
                    return false;
                } else if (DivineFoxEntity.this.getRandom().nextInt(10) != 0) {
                    return false;
                } else {
                    List<ItemEntity> list = DivineFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, DivineFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), DivineFoxEntity.PICKABLE_DROP_FILTER);
                    return !list.isEmpty() && DivineFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty();
                }
            } else {
                return false;
            }
        }

        public void tick() {
            List<ItemEntity> list = DivineFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, DivineFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), DivineFoxEntity.PICKABLE_DROP_FILTER);
            ItemStack itemStack = DivineFoxEntity.this.getEquippedStack(EquipmentSlot.MAINHAND);
            if (itemStack.isEmpty() && !list.isEmpty()) {
                DivineFoxEntity.this.getNavigation().startMovingTo((Entity)list.get(0), 1.2000000476837158D);
            }

        }

        public void start() {
            List<ItemEntity> list = DivineFoxEntity.this.world.getEntitiesByClass(ItemEntity.class, DivineFoxEntity.this.getBoundingBox().expand(8.0D, 8.0D, 8.0D), DivineFoxEntity.PICKABLE_DROP_FILTER);
            if (!list.isEmpty()) {
                DivineFoxEntity.this.getNavigation().startMovingTo((Entity)list.get(0), 1.2000000476837158D);
            }

        }
    }

    public static enum Type {
        CYAN(0, "cyan", BiomeKeys.BIRCH_FOREST, BiomeKeys.BIRCH_FOREST_HILLS, BiomeKeys.TALL_BIRCH_FOREST, BiomeKeys.TALL_BIRCH_HILLS, BiomeKeys.FOREST, BiomeKeys.WOODED_HILLS),
        RED(1, "red", BiomeKeys.BADLANDS, BiomeKeys.BADLANDS_PLATEAU, BiomeKeys.ERODED_BADLANDS, BiomeKeys.MODIFIED_BADLANDS_PLATEAU, BiomeKeys.MODIFIED_WOODED_BADLANDS_PLATEAU, BiomeKeys.WOODED_BADLANDS_PLATEAU),
        WHITE(2, "white", BiomeKeys.SNOWY_MOUNTAINS, BiomeKeys.SNOWY_TAIGA_MOUNTAINS),
        GREEN(3, "green", BiomeKeys.SWAMP, BiomeKeys.SWAMP_HILLS),
        GRAY(4, "gray", BiomeKeys.MOUNTAINS, BiomeKeys.GRAVELLY_MOUNTAINS, BiomeKeys.MODIFIED_GRAVELLY_MOUNTAINS, BiomeKeys.WOODED_MOUNTAINS),
        BROWN(5, "brown", BiomeKeys.DARK_FOREST, BiomeKeys.DARK_FOREST_HILLS), // up to Brown in this list spawn naturally
        LIME(6, "lime", BiomeKeys.THE_VOID), // just to be sure it compiles without errors, even if the IDE doesn't complain when there are no assigned "biomes"
        YELLOW(7, "yellow", BiomeKeys.THE_VOID), // we aren't gathering these biomes for spawning anyway
        BLACK(8, "black", BiomeKeys.THE_VOID),
        LIGHT_GRAY(9, "light_gray", BiomeKeys.THE_VOID),
        LIGHT_BLUE(10, "light_blue", BiomeKeys.THE_VOID),
        ORANGE(11, "orange", BiomeKeys.THE_VOID),
        PINK(12, "pink", BiomeKeys.THE_VOID),
        PURPLE(13, "purple", BiomeKeys.THE_VOID),
        MAGENTA(14, "magenta", BiomeKeys.THE_VOID),
        BLUE(15, "blue", BiomeKeys.THE_VOID);

        private static final DivineFoxEntity.Type[] TYPES = (DivineFoxEntity.Type[]) Arrays.stream(values()).sorted(Comparator.comparingInt(DivineFoxEntity.Type::getId)).toArray((i) -> new Type[i]);
        private static final Map<String, DivineFoxEntity.Type> NAME_TYPE_MAP = (Map)Arrays.stream(values()).collect(Collectors.toMap(DivineFoxEntity.Type::getKey, (type) -> {
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

        public static DivineFoxEntity.Type byName(String name) {
            return (DivineFoxEntity.Type)NAME_TYPE_MAP.getOrDefault(name, CYAN);
        }

        public static DivineFoxEntity.Type fromId(int id) {
            if (id < 0 || id > TYPES.length) {
                id = 0;
            }

            return TYPES[id];
        }
    }
}
