package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Shadow
    @Final
    private long window;

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V")
    )
    void captureWindowHint(int hint, int value, Operation<Void> original) {
        switch (hint) {
            case GLFW_CLIENT_API -> { }
            case GLFW_CONTEXT_CREATION_API -> { }
            case GLFW_CONTEXT_VERSION_MAJOR -> ContextCreationFlags.CURRENT.majorVersion = value;
            case GLFW_CONTEXT_VERSION_MINOR -> ContextCreationFlags.CURRENT.minorVersion = value;
            case GLFW_OPENGL_PROFILE -> ContextCreationFlags.CURRENT.profile = switch (value) {
                case GLFW_OPENGL_CORE_PROFILE -> ContextCreationFlags.Profile.CORE;
                case GLFW_OPENGL_COMPAT_PROFILE -> ContextCreationFlags.Profile.COMPAT;
                case GLFW_OPENGL_ANY_PROFILE -> ContextCreationFlags.Profile.ANY;
                default -> throw new IllegalArgumentException("Unexpected value: " + value);
            };
            case GLFW_OPENGL_FORWARD_COMPAT -> ContextCreationFlags.CURRENT.forwardCompatible = value != GLFW_FALSE;
        }
        original.call(hint, value);
    }


    @Inject(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
    )
    void disableOpenGLContextCreation(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @WrapOperation(
            method = "updateVsync(Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V")
    )
    void proxySwapInterval(int interval, Operation<Void> original) {
        if(RenderSystem.getDevice() instanceof GpuDeviceExtensions ext) {
            ext.perfect_presentation$setSwapInterval(interval);
        } else {
            original.call(interval);
        }
    }

    @WrapOperation(method = "setMode", at = @At(value = "INVOKE", ordinal = 0, target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V"))
    void replace_fullscreen_with_borderless_window(long window, long monitor, int xpos, int ypos, int width, int height, int refreshRate, Operation<Void> original) {
        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
        original.call(window, 0L, xpos, ypos, width, height, -1);
    }

    @Inject(method = "setMode", at = @At(value = "INVOKE", ordinal = 1, target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V"))
    void re_enable_window_border(CallbackInfo ci) {
        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_TRUE);
    }

}
