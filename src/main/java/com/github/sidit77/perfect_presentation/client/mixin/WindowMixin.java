package com.github.sidit77.perfect_presentation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuBackend;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Shadow
    @Final
    private long handle;

    @Inject(
            method = "createGlfwWindow(IILjava/lang/String;JLcom/mojang/blaze3d/systems/GpuBackend;)J",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"
            )
    )
    private static void preventIconification(int width, int height, String title, long monitor, GpuBackend backend, CallbackInfoReturnable<Long> cir) {
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
    }

    @WrapOperation(method = "setMode", at = @At(value = "INVOKE", ordinal = 0, target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V"))
    void replaceFullscreenWithBorderlessWindow(long window, long monitor, int xpos, int ypos, int width, int height, int refreshRate, Operation<Void> original) {
        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
        original.call(window, 0L, xpos, ypos, width, height, -1);
    }

    @Inject(method = "setMode", at = @At(value = "INVOKE", ordinal = 1, target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V"))
    void ReEnabledWindowBorder(CallbackInfo ci) {
        glfwSetWindowAttrib(handle, GLFW_DECORATED, GLFW_TRUE);
    }

}
