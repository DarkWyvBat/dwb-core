package net.darkwyvbat.dwbcore.world.entity.ai.goal;

import net.darkwyvbat.dwbcore.world.entity.AbstractHumanoidEntity;
import net.darkwyvbat.dwbcore.world.entity.AbstractInventoryHumanoid;
import net.darkwyvbat.dwbcore.world.entity.EntityUtils;
import net.darkwyvbat.dwbcore.world.entity.ai.combat.CombatConfig;
import net.darkwyvbat.dwbcore.world.entity.ai.combat.CombatState;
import net.darkwyvbat.dwbcore.world.entity.ai.combat.CombatStrategyManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

//TODO
public class HumanoidCombatGoal extends Goal {
    protected final AbstractHumanoidEntity mob;
    protected LivingEntity target;
    protected CombatState state;
    private final CombatStrategyManager strategyManager;

    public HumanoidCombatGoal(AbstractInventoryHumanoid mob, CombatConfig config, CombatStrategyManager strategyManager) {
        this.mob = mob;
        this.state = new CombatState(mob, config);
        this.strategyManager = strategyManager;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (!EntityUtils.isValidTarget(target) || (mob.getAirSupply() < 10 && !mob.canBreatheUnderwater()) || !mob.canSelfMove())
            return false;
        this.target = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (!EntityUtils.isValidTarget(target)) return false;
        return this.target == target && (mob.getAirSupply() >= 5 || mob.canBreatheUnderwater());
    }

    @Override
    public void start() {
        super.start();
        mob.setAggressive(true);
        target = mob.getTarget();
        strategyManager.setState(state);
        strategyManager.onStart();
    }

    @Override
    public void stop() {
        super.stop();
        strategyManager.onStop();

        target = null;
        mob.setTarget(null);
        mob.setAggressive(false);
        mob.getNavigation().stop();
        mob.stopUsingItem();
    }

    @Override
    public void tick() {
        target = mob.getTarget();
        if (!EntityUtils.isValidTarget(target)) return;
        mob.getLookControl().setLookAt(target);

        updateCombatState();
        strategyManager.tick(state);
    }

    private void updateCombatState() {
        boolean see = mob.getSensing().hasLineOfSight(target);
        int seeTime = see ? Math.min(state.getSeeTime() + 1, 30) : Math.max(state.getSeeTime() - 1, -10);
        state.setTarget(target);
        state.setDistanceSqr(mob.distanceToSqr(target));
        state.setSeeTime(seeTime);
        state.setCanSeeTarget(see);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}