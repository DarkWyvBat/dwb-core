package net.darkwyvbat.dwbcore.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class EntityUtils {

    public static boolean isFirelineClear(Entity shooter, double dist, Predicate<Entity> predicate) {
        Vec3 startPos = shooter.getEyePosition();
        Vec3 lookVec = shooter.getViewVector(1.0F);
        AABB aabb = shooter.getBoundingBox().expandTowards(lookVec.scale(dist)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(shooter, startPos, startPos.add(lookVec.scale(dist)), aabb, predicate, dist * dist) == null;
    }

    public static <T extends Entity> List<T> getEntities(Entity entity, Class<T> clazz, double dist) {
        return entity.level().getEntitiesOfClass(clazz, entity.getBoundingBox().inflate(dist), e -> e != entity);
    }

    public static List<LivingEntity> getLivingEntities(Entity entity, double dist) {
        return getEntities(entity, LivingEntity.class, dist);
    }

    public static List<Entity> getEntitiesXZ(Entity entity, double dist, double h) {
        AABB aabb = new AABB(entity.getX() - dist, entity.getY() - h, entity.getZ() - dist, entity.getX() + dist, entity.getY() + h, entity.getZ() + dist);
        return entity.level().getEntitiesOfClass(Entity.class, aabb, e -> e != entity);
    }

    public static <T extends Entity> T getNearestEntity(Entity entity, Class<T> clazz, double dist) {
        return entity.level().getEntitiesOfClass(clazz, entity.getBoundingBox().inflate(dist), e -> e != entity).stream().min(Comparator.comparingDouble(entity::distanceToSqr)).orElse(null);
    }

    public static List<Entity> getEntitiesInAABB(LivingEntity entity, int dist) {
        return getEntitiesInAABB(entity, dist, false);
    }

    public static List<Entity> getEntitiesInAABB(LivingEntity entity, int dist, boolean canSee) {
        if (entity == null) return new ArrayList<>();
        AABB aabb = new AABB(entity.getX() - dist, entity.getY() - dist, entity.getZ() - dist, entity.getX() + dist, entity.getY() + dist, entity.getZ() + dist);
        return entity.level().getEntitiesOfClass(Entity.class, aabb, e -> (!canSee || entity.hasLineOfSight(e)) && e != entity);
    }

    public static void angerNearbyMobs(ServerLevel level, LivingEntity target, double r, boolean see, long ticks, Predicate<Mob> predicate) {
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(r), m -> m != target && m.isAlive() && predicate.test(m));
        for (Mob mob : mobs) {
            if (see && !mob.getSensing().hasLineOfSight(target)) continue;
            LivingEntity finalTarget = target;
            if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER) && target instanceof Player) {
                Player player = level.getNearestPlayer(TargetingConditions.forCombat().range(r), mob, target.getX(), target.getY(), target.getZ());
                if (isValidTarget(player)) finalTarget = player;
            }
            applyAnger(mob, finalTarget, level, ticks);
        }
    }

    public static void applyAnger(Mob mob, LivingEntity target, ServerLevel level, long ticks) {
        mob.setTarget(target);
        if (mob instanceof NeutralMob neutralMob) {
            neutralMob.setPersistentAngerTarget(EntityReference.of(target));
            neutralMob.setTimeToRemainAngry(ticks);
        }
        Brain<?> brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED))
            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, ticks);
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED))
            brain.setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
        if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER) && brain.checkMemory(MemoryModuleType.UNIVERSAL_ANGER, MemoryStatus.REGISTERED))
            brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
    }

    public static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return false;

        return true;
    }
}
