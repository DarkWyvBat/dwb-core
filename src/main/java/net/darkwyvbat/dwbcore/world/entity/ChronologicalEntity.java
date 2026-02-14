package net.darkwyvbat.dwbcore.world.entity;

import net.darkwyvbat.dwbcore.util.time.Timeline;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface ChronologicalEntity<T extends ChronologicalEntity<T>> {

    Timeline<T> getTimeline();

    void defineTimeline();

    default void tickTimeline() {
        getTimeline().tick();
    }

    default void saveChronology(ValueOutput valueOutput) {
        valueOutput.putLong("chronological_time", getTimeline().getTime());
    }

    default void loadChronology(ValueInput valueInput) {
        getTimeline().setTime(valueInput.getLong("chronological_time").orElse(0L));
    }
}
