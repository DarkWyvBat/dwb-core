package net.darkwyvbat.dwbcore.world.entity.ai;

import net.minecraft.core.component.DataComponents;

public class ItemInspectors {
    public static final ItemInspector FILL_EMPTY_SLOT = (_, _, c, m) -> !m.entryNotEmpty(c);
    public static final ItemInspector ALWAYS = (_, _, _, _) -> true;
    public static final ItemInspector ITEM_UPGRADE = (mob, i, c, m) -> m.getItemComparators().get(c).compare(i, m.getFirstItemInEntry(c)) < 0;
    public static ItemInspector ARMOR_UPGRADE = (mob, i, c, m) -> m.getItemComparators().get(c).compare(i, m.getBestArmorForSlot(i.get(DataComponents.EQUIPPABLE).slot())) < 0;

    public static ItemInspector needsMore(int count) {
        return (_, _, c, m) -> m.getItemCountInEntry(c) < count;
    }
}