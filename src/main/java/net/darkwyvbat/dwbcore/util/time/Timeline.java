package net.darkwyvbat.dwbcore.util.time;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class Timeline<T> {
    private final T object;
    private final List<TimelineEvent<T>> events = new ArrayList<>();

    private long time = 0;
    private int nextEventIndex = 0;

    public Timeline(T object) {
        this.object = object;
    }

    public void init() {
        events.sort(Comparator.comparingInt(TimelineEvent::triggerTime));
    }

    public void addEvent(int triggerTime, Consumer<T> action) {
        events.add(new TimelineEvent<>(triggerTime, action));
    }

    public void tick() {
        ++time;
        while (nextEventIndex < events.size() && time >= events.get(nextEventIndex).triggerTime())
            events.get(nextEventIndex++).action().accept(object);
    }

    public void setTime(long time) {
        this.time = time;
        int newNextIndex = 0;
        for (TimelineEvent<T> event : events)
            if (this.time >= event.triggerTime()) ++newNextIndex;
            else break;
        this.nextEventIndex = newNextIndex;
    }

    public long getTime() {
        return time;
    }
}
