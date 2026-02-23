package net.darkwyvbat.dwbcore.world.entity.ai.perception;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

//TODO
public class PerceptionProfile {
    private int dangerLevel;

    public static final Codec<PerceptionProfile> CODEC = MapCodec.unit(new PerceptionProfile()).codec();

    public int getDangerLevel() {
        return dangerLevel;
    }

    public void setDangerLevel(int lvl) {
        dangerLevel = lvl;
    }

    public void addDangerLevel(int amt) {
        dangerLevel = Math.clamp((long) dangerLevel + amt, 0, Integer.MAX_VALUE);
    }
}
