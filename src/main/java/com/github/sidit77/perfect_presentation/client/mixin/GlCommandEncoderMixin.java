package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.GpuDeviceBackendExtensions;
import com.github.sidit77.perfect_presentation.client.SharedGlTexture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

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
        GpuDeviceBackendExtensions ext = (GpuDeviceBackendExtensions)device;
        ext.perfect_presentation$blitSharedTextureToSwapChain((SharedGlTexture) gpuTextureView.texture());
        ci.cancel();
    }

    // Calling glTexParameteri with either GL_TEXTURE_MAX_LEVEL or GL_TEXTURE_BASE_LEVEL on a D3D11 backed texture
    // causes a bunch of GL_INVALID_OPERATION errors in strange places, and setting the
    // D3D11_RESOURCE_MISC_RESOURCE_CLAMP flag when creating the D3D11 texture breaks the creation of the interop
    // object.
    //
     // To fix this, we just disable the LOD clamping for shared textures.
    @WrapOperation(
            method = "trySetup(Lcom/mojang/blaze3d/opengl/GlRenderPass;Ljava/util/Collection;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texParameter(III)V")
    )
    void preventLodClamping(int i, int j, int k, Operation<Void> original, @Local GlTexture glTexture) {
        boolean isSharedTexture = glTexture instanceof SharedGlTexture;
        if(isSharedTexture && (j == GL_TEXTURE_MAX_LEVEL || j == GL_TEXTURE_BASE_LEVEL))
            return;
        original.call(i, j, k);
    }

}
