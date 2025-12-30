package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.PerfectPresentationNativeLibrary;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @WrapOperation(
            method = "flipFrame(J)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V")
    )
    private static void proxySwapBuffers(long window, Operation<Void> original) {
        PerfectPresentationNativeLibrary.swapBuffers(window);
    }

}
