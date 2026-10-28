package com.yuno.yunosbosses.mixin;

import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.util.FrameFreezeStateAccess;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> extends FeatureRenderer<S, M> {

    public ArmorFeatureRendererMixin(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Unique
    private BipedEntityModel<S> yunosbosses$kimonoModel;

    @Unique
    private S yunosbosses$currentState;

    @Inject(
            method = "<init>(Lnet/minecraft/client/render/entity/feature/FeatureRendererContext;Lnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;)V",
            at = @At("TAIL")
    )
    private void yunosbosses$initKimonoModel(FeatureRendererContext<S, M> context, A innerModel, A outerModel, A babyInnerModel, A babyOuterModel, EquipmentRenderer equipmentRenderer, CallbackInfo ci) {
        // Standard outer armor uses Dilation(1.0F), while inner armor uses Dilation(0.5F).
        // Using Dilation(0.3F) makes the kimono sit just outside the player's arm/sleeve (0.25F dilation)
        // while keeping the exact same shoulder pivot (0,0,0) so it never shifts or clips at the elbow when swinging!
        this.yunosbosses$kimonoModel = new ArmorEntityModel<>(ArmorEntityModel.getModelData(new Dilation(0.3F)).getRoot().createPart(64, 32));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void yunosbosses$captureState(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S bipedEntityRenderState, float f, float g, CallbackInfo ci) {
        this.yunosbosses$currentState = bipedEntityRenderState;
    }

    @ModifyVariable(
            method = "renderArmor",
            at = @At("HEAD"),
            argsOnly = true
    )
    private A yunosbosses$useThinKimonoModel(A armorModel, MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, EquipmentSlot armorSlot, int light) {
        A targetModel = armorModel;
        if (armorSlot == EquipmentSlot.CHEST && stack.isOf(ModItems.KIMONO) && this.yunosbosses$kimonoModel != null) {
            @SuppressWarnings("unchecked")
            A castModel = (A) this.yunosbosses$kimonoModel;
            targetModel = castModel;
        }

        if (this.yunosbosses$currentState instanceof FrameFreezeStateAccess access && access.yunosbosses$getCapturedPlayerAnimBones() != null) {
            this.getContextModel().copyTransforms(targetModel);
        }

        return targetModel;
    }
}
