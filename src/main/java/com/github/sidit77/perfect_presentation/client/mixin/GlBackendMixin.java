package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_ANY_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_COMPAT_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;

@Mixin(GlBackend.class)
public class GlBackendMixin {

    @WrapOperation(
            method = "setWindowHints()V",
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
            method = "setWindowHints()V",
            at = @At("TAIL")
    )
    void disableContextCreation(CallbackInfo ci) {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

}
