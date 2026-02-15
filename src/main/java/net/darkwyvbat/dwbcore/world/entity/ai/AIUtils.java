package net.darkwyvbat.dwbcore.world.entity.ai;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public final class AIUtils {

    public static Holder<MobEffect> getSuitableAttackPotion(LivingEntity attacker, LivingEntity target, Set<Holder<MobEffect>> availableEffects) {
        if (availableEffects.contains(MobEffects.SLOWNESS) && !target.hasEffect(MobEffects.SLOWNESS)) {
            if (attacker.distanceToSqr(target) > 25.0)
                return MobEffects.SLOWNESS;
        }
        if (availableEffects.contains(MobEffects.POISON) && !target.hasEffect(MobEffects.POISON))
            return MobEffects.POISON;
        if (availableEffects.contains(MobEffects.WEAKNESS) && !target.hasEffect(MobEffects.WEAKNESS))
            return MobEffects.WEAKNESS;
        if (availableEffects.contains(MobEffects.INSTANT_DAMAGE))
            return MobEffects.INSTANT_DAMAGE;

        return null;
    }

}