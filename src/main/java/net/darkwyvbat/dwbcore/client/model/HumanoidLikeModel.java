package net.darkwyvbat.dwbcore.client.model;

import net.darkwyvbat.dwbcore.client.renderer.entity.HumanoidLikeRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class HumanoidLikeModel<S extends HumanoidLikeRenderState> extends HumanoidModel<S> {
    public HumanoidLikeModel(ModelPart modelPart) {
        super(modelPart);
    }

    public HumanoidLikeModel(ModelPart modelPart, Function<Identifier, RenderType> function) {
        super(modelPart, function);
    }

    protected void setupAttackAnimation(S state, float f) {
        super.setupAttackAnimation(state);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        setupAttackAnimation(state, state.ageInTicks);
        if (state.isSitting) {
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = (float) (-Math.PI / 10);
            this.leftLeg.zRot = -0.07853982F;
        }
    }
}
