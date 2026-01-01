package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.github.sidit77.perfect_presentation.client.PerfectPresentationClient;
import com.github.sidit77.perfect_presentation.client.PerfectPresentationNativeLibrary;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public class WindowMixin {

    @Shadow
    @Final
    private long window;

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V")
    )
    void captureWindowHint(int hint, int value, Operation<Void> original) {
        var storedContextFlags = PerfectPresentationClient.contextCreationFlags;
        switch (hint) {
            case GLFW_CLIENT_API -> glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            case GLFW_CONTEXT_CREATION_API -> { }
            case GLFW_CONTEXT_VERSION_MAJOR -> storedContextFlags.majorVersion = value;
            case GLFW_CONTEXT_VERSION_MINOR -> storedContextFlags.minorVersion = value;
            case GLFW_OPENGL_PROFILE -> storedContextFlags.profile = switch (value) {
                case GLFW_OPENGL_CORE_PROFILE -> ContextCreationFlags.Profile.CORE;
                case GLFW_OPENGL_COMPAT_PROFILE -> ContextCreationFlags.Profile.COMPAT;
                case GLFW_OPENGL_ANY_PROFILE -> ContextCreationFlags.Profile.ANY;
                default -> throw new IllegalArgumentException("Unexpected value: " + value);
            };
            case GLFW_OPENGL_FORWARD_COMPAT -> storedContextFlags.forwardCompatible = value != GLFW_FALSE;
            default -> glfwWindowHint(hint, value);
        }
    }

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
    )
    long createInteropSwapChain(int width, int height, CharSequence title, long monitor, long share, Operation<Long> original) {
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        var window = original.call(width, height, title, monitor, share);
        //TODO verify that we're on Windows
        var hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
        PerfectPresentationNativeLibrary.createContextAndSwapChain(window, hwnd);
        return window;
    }

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V")
    )
    void proxyMakeCurrent(long window, Operation<Void> original) {
        PerfectPresentationNativeLibrary.makeContextCurrent(window);
    }

    @WrapOperation(
            method = "updateVsync(Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V")
    )
    void proxySwapInterval(int interval, Operation<Void> original) {
        PerfectPresentationNativeLibrary.setSwapInterval(window, interval);
    }

}
