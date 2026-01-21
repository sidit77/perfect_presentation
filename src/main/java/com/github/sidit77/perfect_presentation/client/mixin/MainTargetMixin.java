package com.github.sidit77.perfect_presentation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.MainTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(MainTarget.class)
public class MainTargetMixin {

    @WrapOperation(method = "<init>(II)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/MainTarget;createFrameBuffer(II)V"))
    void delegateBufferCreation(MainTarget instance, int i, int j, Operation<Void> original) {
        instance.createBuffers(i, j, false);
    }

}
