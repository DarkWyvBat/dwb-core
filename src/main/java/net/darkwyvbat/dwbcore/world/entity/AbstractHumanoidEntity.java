package net.darkwyvbat.dwbcore.world.entity;

import net.darkwyvbat.dwbcore.network.DwbEntityDataSerializers;
import net.darkwyvbat.dwbcore.util.PoorRandom;
import net.darkwyvbat.dwbcore.util.time.TickingCooldown;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static net.darkwyvbat.dwbcore.DwbCore.INFO;

public abstract class AbstractHumanoidEntity extends PerceptionBasedMob implements Crouchable {

    public static final Vec3 DEFAULT_VEHICLE_ATTACHMENT = new Vec3(0.0, 0.6, 0.0);

    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
            .withEyeHeight(1.62F)
            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));
    public static final EntityDimensions SITTING_DIMENSIONS = STANDING_DIMENSIONS.scale(1.0F, 0.7F);
    public static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.3F, 0.3F);
    public static final EntityDimensions SMALL_HITBOX_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F)
            .withEyeHeight(0.4F);
    public static final EntityDimensions CROUCHING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.5F)
            .withEyeHeight(1.27F)
            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));
    public static final EntityDimensions DYING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F)
            .withEyeHeight(1.62F);

    private static final EntityDataAccessor<MobState> DATA_MOB_STATE = SynchedEntityData.defineId(AbstractHumanoidEntity.class, DwbEntityDataSerializers.MOB_STATE);

    public static final Identifier SHIELD_ATTRIBUTE_ID = INFO.id("shield");
    public static final AttributeModifier SHIELD_KNOCKBACK_RESISTANCE = new AttributeModifier(SHIELD_ATTRIBUTE_ID, 0.8, AttributeModifier.Operation.ADD_VALUE);

    protected TickingCooldown useItemCd = new TickingCooldown(0);

    protected AbstractHumanoidEntity(EntityType<? extends AbstractHumanoidEntity> entityType, Level level) {
        super(entityType, level);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOB_STATE, MobState.STANDING);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        valueOutput.store("mob_state", MobState.CODEC, getMobState());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        setMobState(valueInput.read("mob_state", MobState.CODEC).orElse(MobState.STANDING));
    }

    public static AttributeSupplier.Builder createHumanoidAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.SNEAKING_SPEED, 0.5);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        updateSwingTime();
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (!getNavigation().isDone()) {
            if (isSitting() || isSleeping()) {
                if (isSleeping()) stopSleeping();
                standUp();
            }
        }
    }

    public boolean canSelfMove() {
        return !isSleeping() && !isSitting() && isAlive();
    }

    public void standUp() {
        setMobState(MobState.STANDING);
    }

    public void startSitting() {
        startSitting(blockPosition());
    }

    public void startSitting(BlockPos blockPos) {
        if (isPassenger()) stopRiding();
        if (isSleeping()) stopSleeping();
        setMobState(MobState.SITTING);
        if (blockPos != null) setPos(blockPos.getX(), blockPos.getY() + 0.1, blockPos.getZ());
        setDeltaMovement(Vec3.ZERO);
        needsSync = true;
    }

    public void stopSitting() {
        if (isSitting())
            setMobState(MobState.STANDING);
    }

    public boolean isSitting() {
        return getMobState() == MobState.SITTING;
    }

    @Override
    public void startSleeping(BlockPos blockPos) {
        setMobState(MobState.SLEEPING);
        super.startSleeping(blockPos);
    }

    @Override
    public void stopSleeping() {
        setMobState(MobState.STANDING);
        super.stopSleeping();
    }

    @Override
    public boolean isSleeping() {
        return super.isSleeping() && getMobState() == MobState.SLEEPING;
    }

    public MobState getMobState() {
        return entityData.get(DATA_MOB_STATE);
    }

    public void setMobState(MobState state) {
        entityData.set(DATA_MOB_STATE, state);
        updateState(getMobState());
    }

    public void updateState(MobState state) {
        switch (state) {
            case STANDING -> setPose(Pose.STANDING);
            case CROUCHING -> setPose(Pose.CROUCHING);
            case SITTING -> setPose(Pose.SITTING);
            case SWIMMING -> setPose(Pose.SWIMMING);
            case SLEEPING -> setPose(Pose.SLEEPING);
            default -> setPose(Pose.STANDING);
        }
    }

    @Override
    public boolean canCrouch() {
        return true;
    }

    @Override
    public boolean isCrouching() {
        return getMobState() == MobState.CROUCHING;
    }

    @Override
    public void setCrouch(boolean crouching) {
        if (crouching)
            setMobState(MobState.CROUCHING);
        else if (getMobState() == MobState.CROUCHING) setMobState(MobState.STANDING);
    }

    @Override
    public EntityDimensions getCrouchDimension() {
        return getDefaultDimensions(Pose.CROUCHING);
    }

    @Override
    public void setSwimming(boolean swimming) {
        super.setSwimming(swimming);
        if (swimming)
            setMobState(MobState.SWIMMING);
        else if (getMobState() == MobState.SWIMMING)
            setMobState(MobState.STANDING);
    }

    @Override
    public void updateSwimming() {
        if (!level().isClientSide())
            setSwimming(isEffectiveAi() && isUnderWater());
    }

    @Override
    public boolean isVisuallySwimming() {
        return isSwimming() || super.isVisuallySwimming();
    }

    public TickingCooldown getUseItemCd() {
        return useItemCd;
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        super.startUsingItem(hand);
        ItemStack itemInHand = getItemInHand(hand);
        if (!level().isClientSide() && itemInHand.is(Items.SHIELD))
            applyShieldEffects();
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (!level().isClientSide())
            removeShieldEffects();
    }

    @Override
    protected void completeUsingItem() {
        ItemStack item = getUseItem();
        FoodProperties foodPropertiesItem = item.get(DataComponents.FOOD);
        super.completeUsingItem();

        if (foodPropertiesItem != null && !level().isClientSide()) {
            heal(foodPropertiesItem.nutrition());
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_BURP, getSoundSource(), 0.5F, PoorRandom.quickFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity entity) {
        boolean pass = super.doHurtTarget(serverLevel, entity);
        if (pass && entity instanceof LivingEntity livingEntity)
            this.getWeaponItem().hurtAndBreak(1, livingEntity, InteractionHand.MAIN_HAND);
        return pass;
    }

    @Override
    public void hurtArmor(DamageSource damageSource, float f) {
        this.doHurtEquipment(damageSource, f, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    public void onEquippedItemBroken(Item item, EquipmentSlot equipmentSlot) {
        if (item.equals(Items.SHIELD))
            removeShieldEffects();
        super.onEquippedItemBroken(item, equipmentSlot);
    }

    protected void spawnThrownItem(ItemStack itemStack, Vec3 dir, float f, int delay) {
        ItemEntity itemEntity = new ItemEntity(level(), getX(), getEyeY(), getZ(), itemStack);
        itemEntity.setThrower(this);
        itemEntity.setDeltaMovement(dir);
        itemEntity.setPickUpDelay(delay);
        level().addFreshEntity(itemEntity);
    }

    public void applyShieldEffects() {
        if (!getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(SHIELD_ATTRIBUTE_ID))
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).addTransientModifier(SHIELD_KNOCKBACK_RESISTANCE);
    }

    public void removeShieldEffects() {
        if (getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(SHIELD_ATTRIBUTE_ID))
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(SHIELD_KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void blockUsingItem(ServerLevel serverLevel, LivingEntity livingEntity) {
        ItemStack itemStack = this.getItemBlockingWith();
        BlocksAttacks blocksAttacks = itemStack != null ? itemStack.get(DataComponents.BLOCKS_ATTACKS) : null;
        float f = livingEntity.getSecondsToDisableBlocking();
        if (f > 0.0F && blocksAttacks != null) {
            useItemCd.set((int) (f * 20));
            blocksAttacks.disable(serverLevel, this, f, itemStack);
        }
    }

    @Override
    public float applyItemBlocking(ServerLevel serverLevel, DamageSource damageSource, float f) {
        ItemStack item = getItemBlockingWith();
        if (item != null && item.isDamageableItem())
            item.hurtAndBreak((int) f, this, getUsedItemHand());
        return super.applyItemBlocking(serverLevel, damageSource, f);
    }

    @Override
    public float getSpeed() {
        double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (isCrouching()) speed *= getAttributeValue(Attributes.SNEAKING_SPEED);
        if (isUsingItem()) speed *= 0.5;
        return (float) speed;
    }

    protected float getEyeHeightModifier() {
        return 1.0F;
    }

    protected Vec2 getDimModifier() {
        return null;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimension = switch (pose) {
            case SITTING -> SITTING_DIMENSIONS;
            case SLEEPING -> SLEEPING_DIMENSIONS;
            case FALL_FLYING, SWIMMING, SPIN_ATTACK -> SMALL_HITBOX_DIMENSIONS;
            case CROUCHING -> CROUCHING_DIMENSIONS;
            case DYING -> DYING_DIMENSIONS;
            default -> STANDING_DIMENSIONS;
        };
        Vec2 dimScale = getDimModifier();
        EntityDimensions scaled = (dimScale != null) ? dimension.scale(dimScale.x, dimScale.y) : dimension;
        return scaled.withEyeHeight(scaled.eyeHeight() * getEyeHeightModifier());
    }

    public abstract void disarm();

    public abstract void freeHands();

}