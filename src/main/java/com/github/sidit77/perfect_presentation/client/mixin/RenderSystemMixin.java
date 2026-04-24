package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Shadow
    private static GpuDevice DEVICE;

    @WrapOperation(
            method = "flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V")
    )
    private static void proxySwapBuffers(long window, Operation<Void> original) {
        if (DEVICE instanceof GpuDeviceExtensions ext) {
            ext.perfect_presentation$swapChainPresent();
        } else {
            original.call(window);
        }
    }

}
