package net.darkwyvbat.dwbcore.world.entity.inventory;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.*;

public class HumanoidInventoryManager extends InventoryManager {

    private final Map<Holder<MobEffect>, Integer> potionEffectCache = new HashMap<>();
    protected final Map<EquipmentSlot, Integer> equipmentSlotsInvRefs;

    public HumanoidInventoryManager(SimpleContainer inventory, InventoryConfig config) {
        super(inventory, config.categorizer(), config.comparators(), config.importanceOrder(), config.categories());
        equipmentSlotsInvRefs = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values())
            equipmentSlotsInvRefs.put(slot, INVALID_INDEX);
        updateInventoryEntries();
    }

    public ItemStack getBestArmorForSlot(EquipmentSlot slot) {
        for (int i : getInventoryEntry(DwbItemCategories.ARMOR)) {
            ItemStack item = getInventory().getItem(i);
            if (item.get(DataComponents.EQUIPPABLE).slot() == slot)
                return item;
        }
        return ItemStack.EMPTY;
    }

    public int getForHealIndex() {
        if (entryNotEmpty(DwbItemCategories.CONSUMABLE))
            return getFirstIndexInEntry(DwbItemCategories.CONSUMABLE);
        return getPotionWithEffectIndex(MobEffects.INSTANT_HEALTH, MobEffects.REGENERATION);
    }

    private void ensurePotionCacheBuilt() {
        if (!potionEffectCache.isEmpty()) return;
        scanPotionsForCategory(DwbItemCategories.ATTACK_POTION);
        scanPotionsForCategory(DwbItemCategories.SUPPORT_POTION);
    }

    private void scanPotionsForCategory(ItemCategory category) {
        List<Integer> entry = getInventoryEntry(category);
        if (entry == null) return;
        for (int i : getInventoryEntry(category)) {
            PotionContents contents = getInventory().getItem(i).get(DataComponents.POTION_CONTENTS);
            if (contents != null)
                for (MobEffectInstance effectInstance : contents.getAllEffects())
                    potionEffectCache.putIfAbsent(effectInstance.getEffect(), i);
        }
    }

    public Map<Holder<MobEffect>, Integer> getAvailablePotionEffectsWithIndices() {
        ensurePotionCacheBuilt();
        return Collections.unmodifiableMap(potionEffectCache);
    }

    @SafeVarargs
    public final int getPotionWithEffectIndex(Holder<MobEffect>... effects) {
        if (effects.length == 0) return INVALID_INDEX;
        ensurePotionCacheBuilt();
        for (Holder<MobEffect> effect : effects) {
            Integer index = potionEffectCache.get(effect);
            if (index != null) return index;
        }
        return INVALID_INDEX;
    }

    @Override
    public void updateInventoryEntries() {
        super.updateInventoryEntries();
        potionEffectCache.clear();
    }

    public Map<EquipmentSlot, Integer> getEquipmentSlotInvRefs() {
        return equipmentSlotsInvRefs;
    }

    public void setSlotRefs(Map<EquipmentSlot, Integer> refs) {
        equipmentSlotsInvRefs.putAll(refs);
    }
}
