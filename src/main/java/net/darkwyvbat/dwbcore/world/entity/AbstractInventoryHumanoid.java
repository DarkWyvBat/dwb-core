package net.darkwyvbat.dwbcore.world.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.darkwyvbat.dwbcore.world.entity.ai.ItemInspector;
import net.darkwyvbat.dwbcore.world.entity.inventory.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractInventoryHumanoid extends AbstractHumanoidEntity implements InventoryUser {

    protected final HumanoidInventoryManager inventoryManager;
    public boolean shouldRevisionItems = true;
    protected ItemEntity wantedItem;
    public final IntOpenHashSet pickupBlacklist = new IntOpenHashSet();

    protected AbstractInventoryHumanoid(EntityType<? extends AbstractInventoryHumanoid> entityType, Level level) {
        super(entityType, level);
        inventoryManager = createInventoryManager();
    }

    protected abstract MobInventoryProfile getInventoryProfile();

    protected void populateInventory() {
        System.out.println("populated");
        inventoryManager.addItems(getInventoryProfile().items().stream().map(ItemStackTemplate::create).toList());
    }

    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, EntitySpawnReason entitySpawnReason, @Nullable SpawnGroupData spawnGroupData) {
        System.out.println("finalize");
        if (entitySpawnReason != EntitySpawnReason.CONVERSION) populateInventory();
        inventoryManager.updateInventoryEntries();
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, entitySpawnReason, spawnGroupData);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        if (tickCount % 64 == 0)
            scanForItems();

        useItemCd.tick();
    }

    public <T extends Mob> T convertToMob(EntityType<T> entityType, ConversionParams params, boolean keepInventory) {
        return convertTo(entityType, params, m -> {
            if (keepInventory && m instanceof InventoryUser inventoryUser) {
                inventoryUser.getInventoryManager().addItems(getInventory().getItems());
                ((HumanoidInventoryManager) inventoryUser.getInventoryManager()).setSlotRefs(inventoryManager.getEquipmentSlotInvRefs());
            }
        });
    }

    @Override
    public HumanoidInventoryManager createInventoryManager() {
        return new HumanoidInventoryManager(new SimpleContainer(getInventorySize()) {
            @Override
            public void setChanged() {
                super.setChanged();
                AbstractInventoryHumanoid.this.inventoryChanged();
            }
        }, getInventoryProfile().inventoryConfig());
    }

    @Override
    public HumanoidInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    @Override
    public int getInventorySize() {
        return 16;
    }

    @Override
    public SimpleContainer getInventory() {
        return inventoryManager.getInventory();
    }

    public void inventoryChanged() {
        inventoryManager.updateInventoryEntries();
        shouldRevisionItems = true;
        wantedItem = null;
        pickupBlacklist.clear();
    }

    public void ignoreItem(ItemEntity itemEntity) {
        pickupBlacklist.add(itemEntity.getId());
    }

    public boolean isItemIgnored(ItemEntity itemEntity) {
        return pickupBlacklist.contains(itemEntity.getId());
    }

    @Override
    public void onEquippedItemBroken(Item item, EquipmentSlot equipmentSlot) {
        super.onEquippedItemBroken(item, equipmentSlot);
        inventoryChanged();
    }

    @Override
    public void pickUpItem(ServerLevel serverLevel, ItemEntity itemEntity) {
        onItemPickup(itemEntity);
        InventoryCarrier.pickUpItem(serverLevel, this, this, itemEntity);
        inventoryManager.updateInventoryEntries();
    }

    private void scanForItems() {
        if (wantedItem != null && wantedItem.isAlive() && !wantedItem.hasPickUpDelay()) return;
        wantedItem = null;

        List<ItemEntity> itemsAround = getItemsAround(8);
        if (itemsAround.isEmpty()) return;
        ItemEntity closestItem = null;
        double minDistSqr = Double.MAX_VALUE;
        for (ItemEntity itemEntity : itemsAround) {
            if (isItemIgnored(itemEntity) || !itemEntity.isAlive() || itemEntity.hasPickUpDelay())
                continue;
            if (wantsToPickUp((ServerLevel) level(), itemEntity.getItem())) {
                double distSqr = distanceToSqr(itemEntity);
                if (distSqr < minDistSqr) {
                    minDistSqr = distSqr;
                    closestItem = itemEntity;
                }
            } else
                pickupBlacklist.add(itemEntity.getId());
        }
        wantedItem = closestItem;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel serverLevel, ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;

        Set<ItemCategory> categories = inventoryManager.getCategorizer().categorize(itemStack);
        if (categories.isEmpty()) return false;
        ItemCategory newCategory = null;
        for (ItemCategory cat : categories)
            if (newCategory == null || inventoryManager.isCategoryMoreImportant(cat, newCategory))
                newCategory = cat;
        if (inventoryManager.entryNotEmpty(newCategory)) {
            ItemInspector inspector = getInventoryProfile().itemInspectors().get(newCategory);
            if (inspector != null)
                if (!inspector.isWanted(this, itemStack, newCategory, inventoryManager))
                    return false;
        }
        if (getInventory().canAddItem(itemStack)) return true;
        ItemCategory lowestInInv = inventoryManager.getLowestPresentCategory();
        if (lowestInInv == null) return true;
        if (inventoryManager.isCategoryMoreImportant(newCategory, lowestInInv))
            return true;
        return newCategory == lowestInInv;
    }

    protected List<ItemEntity> getItemsAround(double r) {
        return level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(r));
    }

    @Override
    public void cleanInventory(int slotsToFree) {
        Set<Integer> trashIndices = inventoryManager.getPotentialTrash(slotsToFree, getInventoryProfile().inventoryConfig().cleanStrategies());
        throwItems(trashIndices, null, 1.0F, 40);
        trashIndices.forEach(i -> inventoryManager.getInventory().removeItem(i, inventoryManager.getInventory().getItem(i).getCount()));
    }

    public void throwItem(int i, int c, Vec3 dest, float f, int delay, boolean shouldUpdate) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 dir = dest == null ? Vec3.directionFromRotation(getRotationVector()).scale(f) : dest.normalize().scale(f);
        spawnThrownItem(inventoryManager.getInventory().getItem(i), dir, f, delay);
        if (shouldUpdate) inventoryManager.getInventory().removeItem(i, c);
        else inventoryManager.getInventory().removeItemNoUpdate(i);
    }

    public void throwItems(Set<Integer> items, Vec3 dest, float f, int delay) {
        swing(InteractionHand.MAIN_HAND);
        for (int i : items)
            throwItem(i, inventoryManager.getInventory().getItem(i).getCount(), dest, 0.3F, delay, false);

        inventoryManager.getInventory().setChanged();
    }

    public void prepareArmor() {
        Set<EquipmentSlot> alreadyEquipped = new HashSet<>();
        List<Integer> armorIndices = inventoryManager.getInventoryEntry(DwbItemCategories.ARMOR);
        for (int armorIndex : armorIndices) {
            ItemStack item = getInventory().getItem(armorIndex);
            if (item.isEmpty()) continue;
            EquipmentSlot slot = item.get(DataComponents.EQUIPPABLE).slot();
            if (!alreadyEquipped.contains(slot)) {
                equipFromInventory(slot, armorIndex);
                alreadyEquipped.add(slot);
            }
        }
    }

    protected void completeUsingItem() {
        InteractionHand hand = getUsedItemHand();
        EquipmentSlot slot = hand.asEquipmentSlot();
        int invIndex = inventoryManager.getEquipmentSlotInvRefs().get(slot);
        super.completeUsingItem();
        if (invIndex != -1)
            getInventory().setItem(invIndex, getItemInHand(hand));

        inventoryManager.updateInventoryEntries();
    }

    public int equipFromInventory(EquipmentSlot targetSlot, int newInvIndex) {
        Integer oldInvIndex = inventoryManager.getEquipmentSlotInvRefs().get(targetSlot);
        if (oldInvIndex.equals(newInvIndex)) return oldInvIndex;
        ItemStack item = newInvIndex != InventoryManager.INVALID_INDEX ? getInventory().getItem(newInvIndex) : ItemStack.EMPTY;
        setItemSlot(targetSlot, item);
        inventoryManager.getEquipmentSlotInvRefs().put(targetSlot, newInvIndex);
        return newInvIndex;
    }

    @Override
    public void disarm() {
        for (EquipmentSlot equipmentSlot : EquipmentSlotGroup.ARMOR)
            equipFromInventory(equipmentSlot, InventoryManager.INVALID_INDEX);
    }

    @Override
    public void freeHands() {
        equipFromInventory(EquipmentSlot.MAINHAND, InventoryManager.INVALID_INDEX);
        equipFromInventory(EquipmentSlot.OFFHAND, InventoryManager.INVALID_INDEX);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        writeInventoryToTag(valueOutput);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        readInventoryFromTag(valueInput);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource damageSource, boolean bl) {
        inventoryManager.getInventory().removeAllItems().forEach(itemStack -> {
            if (!EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
                spawnAtLocation(serverLevel, itemStack);
        });
    }

    public ItemEntity getWantedItem() {
        return wantedItem;
    }

    public void setWantedItem(ItemEntity itemEntity) {
        wantedItem = itemEntity;
    }
}