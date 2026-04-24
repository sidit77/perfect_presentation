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

    /*
    @Shadow
    protected int colorTextureId;

    @WrapOperation(
            method = "createBuffers(II)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", ordinal = 1)
    )
    void createInteropColorBuffer(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer data, Operation<Void> original) {
        if (((RenderTarget)(Object)this) instanceof MainTarget) {
            InteropContext.getCurrentContext().allocateSharedTexture(this.colorTextureId, target, internalFormat, width, height);
        } else {
            original.call(target, level, internalFormat, width, height, border, format, type, data);
        }
    }

    @Inject(
            method = "destroyBuffers()V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;releaseTextureId(I)V", ordinal = 1)
    )
    void destroyInteropColorBuffer(CallbackInfo ci) {
        if (((RenderTarget)(Object)this) instanceof MainTarget) {
            InteropContext.getCurrentContext().deallocateSharedTexture(this.colorTextureId);
        }
    }
     */

    @WrapOperation(
            method = "createBuffers(II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/TextureFormat;III)Lcom/mojang/blaze3d/textures/GpuTexture;",
                    ordinal = 1
            )
    )
    GpuTexture createInteropColorBuffer(GpuDevice instance, @Nullable Supplier<String> stringSupplier, TextureFormat textureFormat, int i, int j, int k, Operation<GpuTexture> original) {
        if (((RenderTarget)(Object)this) instanceof MainTarget && (instance instanceof GpuDeviceExtensions ext)) {
            return ext.perfect_presentation$createSharedTexture(stringSupplier, textureFormat, i, j);
        } else {
            return original.call(instance, stringSupplier, textureFormat, i, j, k);
        }
    }

}
