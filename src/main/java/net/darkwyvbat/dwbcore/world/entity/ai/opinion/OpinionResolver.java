package net.darkwyvbat.dwbcore.world.entity.ai.opinion;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpinionResolver {

    private final Map<Class<? extends Entity>, Opinion> baseOpinions;
    private final Map<Class<? extends Entity>, Opinion> cache = new ConcurrentHashMap<>();
    private final Opinion unknown;

    public OpinionResolver(Map<Class<? extends Entity>, Opinion> baseOpinions, Opinion unknown) {
        this.baseOpinions = baseOpinions;
        this.unknown = unknown;
    }

    public void addOpinion(Class<? extends Entity> entityClass, Opinion opinion) {
        baseOpinions.put(entityClass, opinion);
        cache.clear();
    }

    public Opinion get(Class<? extends Entity> entityClass) {
        return cache.computeIfAbsent(entityClass, this::findOpinion);
    }

    private Opinion findOpinion(Class<? extends Entity> entityClass) {
        Class<?> currentClass = entityClass;
        while (currentClass != null && Entity.class.isAssignableFrom(currentClass)) {
            Opinion opinion = baseOpinions.get(currentClass);
            if (opinion != null) return opinion;
            currentClass = currentClass.getSuperclass();
        }
        return unknown;
    }

    public Reputation getEntityRep(Entity entity) {
        return get(entity.getClass()).reputation();
    }
}
