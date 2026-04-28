package com.github.sidit77.perfect_presentation.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    /*
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

     */

}
