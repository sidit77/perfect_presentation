package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.github.sidit77.perfect_presentation.client.InteropContext;
import com.github.sidit77.perfect_presentation.client.InteropContextProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin implements InteropContextProvider {

    @Shadow
    @Final
    private long window;

    @Unique
    private final ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Unique
    private InteropContext interopContext;


    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V")
    )
    void captureWindowHint(int hint, int value, Operation<Void> original) {
        switch (hint) {
            case GLFW_CLIENT_API -> glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            case GLFW_CONTEXT_CREATION_API -> { }
            case GLFW_CONTEXT_VERSION_MAJOR -> contextCreationFlags.majorVersion = value;
            case GLFW_CONTEXT_VERSION_MINOR -> contextCreationFlags.minorVersion = value;
            case GLFW_OPENGL_PROFILE -> contextCreationFlags.profile = switch (value) {
                case GLFW_OPENGL_CORE_PROFILE -> ContextCreationFlags.Profile.CORE;
                case GLFW_OPENGL_COMPAT_PROFILE -> ContextCreationFlags.Profile.COMPAT;
                case GLFW_OPENGL_ANY_PROFILE -> ContextCreationFlags.Profile.ANY;
                default -> throw new IllegalArgumentException("Unexpected value: " + value);
            };
            case GLFW_OPENGL_FORWARD_COMPAT -> contextCreationFlags.forwardCompatible = value != GLFW_FALSE;
            default -> glfwWindowHint(hint, value);
        }
    }

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J")
    )
    long setupInteropContext(int width, int height, CharSequence title, long monitor, long share, Operation<Long> original) {
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        var window = original.call(width, height, title, 0L, share);

        //TODO verify that we're on Windows
        var hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
        interopContext = new InteropContext(hwnd, contextCreationFlags);
        return window;
    }

    @WrapOperation(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V")
    )
    void proxyMakeCurrent(long window, Operation<Void> original) {
        interopContext.makeCurrent();
    }

    @WrapOperation(
            method = "updateVsync(Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V")
    )
    void proxySwapInterval(int interval, Operation<Void> original) {
        interopContext.setSyncInterval(interval);
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

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDestroyWindow(J)V"))
    void destroyInteropContext(CallbackInfo ci) {
        interopContext.close();
    }

    @Override
    public InteropContext prefect_presentation$getInteropContext() {
        return interopContext;
    }
}
