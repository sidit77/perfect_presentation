package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {

    @WrapOperation(
            method = "createBuffers(II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
                    ordinal = 1
            )
    )
    GpuTexture createInteropColorBuffer(GpuDevice instance, Supplier<String> stringSupplier, int u, TextureFormat textureFormat, int w, int h, int l, int m, Operation<GpuTexture> original) {
        if (((RenderTarget)(Object)this) instanceof MainTarget && (instance instanceof GpuDeviceExtensions ext)) {
            return ext.perfect_presentation$createSharedTexture(stringSupplier, u, textureFormat, w, h);
        } else {
            return original.call(instance, stringSupplier, u, textureFormat, w, h, l, m);
        }
    }

}
