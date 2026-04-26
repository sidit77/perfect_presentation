package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
import com.github.sidit77.perfect_presentation.client.SharedGlTexture;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {

    @Final
    @Shadow
    private GlDevice device;

    @Inject(
            method = "Lcom/mojang/blaze3d/opengl/GlCommandEncoder;presentTexture(Lcom/mojang/blaze3d/textures/GpuTextureView;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_disableScissorTest()V"
            ),
            cancellable = true
    )
    void presentTexture(GpuTextureView gpuTextureView, CallbackInfo ci) {
        GpuDeviceExtensions ext = (GpuDeviceExtensions)device;
        ext.perfect_presentation$blitSharedTextureToSwapChain((SharedGlTexture) gpuTextureView.texture());
        ci.cancel();
    }

}
