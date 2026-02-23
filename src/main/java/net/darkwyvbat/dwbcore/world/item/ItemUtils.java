package net.darkwyvbat.dwbcore.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Map;

public final class ItemUtils {

    public static ArmorStatsSummary getArmorProtection(ItemStack itemStack) {
        if (itemStack.isEmpty()) return ArmorStatsSummary.EMPTY;
        ArmorStatsSummary baseStats = getBaseArmorStats(itemStack);
        return new ArmorStatsSummary(baseStats.protection() + getEnchantmentLevel(itemStack, Enchantments.PROTECTION), baseStats.knockbackResistance());
    }

    public static ArmorStatsSummary getBaseArmorStats(ItemStack itemStack) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return ArmorStatsSummary.EMPTY;
        EquipmentSlot slot = equippable.slot();
        ItemAttributeModifiers modifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return new ArmorStatsSummary(modifiers.compute(Attributes.ARMOR, 0.0, slot), modifiers.compute(Attributes.KNOCKBACK_RESISTANCE, 0.0, slot));
    }

    public static int getEnchantmentLevel(ItemStack itemStack, ResourceKey<Enchantment> key) {
        for (Map.Entry<Holder<Enchantment>, Integer> entry : itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
            if (entry.getKey().is(key)) return entry.getValue();
        return 0;
    }

    public static double getItemAttackDamage(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0.0;
        ItemAttributeModifiers modifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
    }

    public static int getNutrition(ItemStack itemStack) {
        FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
        return foodProperties == null ? 0 : foodProperties.nutrition();
    }
}