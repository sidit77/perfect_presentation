package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.github.sidit77.perfect_presentation.client.GpuDeviceBackendExtensions;
import com.github.sidit77.perfect_presentation.client.InteropContext;
import com.github.sidit77.perfect_presentation.client.SharedGlTexture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.*;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(GlDevice.class)
public class GlDeviceMixin implements GpuDeviceBackendExtensions {

    @Unique
    private InteropContext interopContext;

    @Shadow
    @Final
    private GlDebugLabel debugLabels;

    @WrapOperation(
            method = "<init>(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V")
    )
    void createInteropContext(long window, Operation<Void> original) {
        var hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
        interopContext = new InteropContext(hwnd, ContextCreationFlags.CURRENT);

        interopContext.makeCurrent();
    }

    @Inject(method = "close", at = @At("TAIL"))
    void destroyInteropContext(CallbackInfo ci) {
        interopContext.close();
    }

    @WrapOperation(
            method = "Lcom/mojang/blaze3d/opengl/GlDevice;getImplementationInformation()Ljava/lang/String;",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetCurrentContext()J")
    )
    long patchGetContext(Operation<Long> original) {
        return InteropContext.getCurrentContext() == null ? 0 : 1;
    }

    @WrapOperation(
            method = "setVsync(Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V")
    )
    void updateVsync(int interval, Operation<Void> original) {
        interopContext.setSyncInterval(interval);
    }

    @Redirect(
            method = "presentFrame()V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V")
    )
    void presentWithDxgi(long window) {
        interopContext.swapChainPresent();
    }

    @Override
    public GpuTexture perfect_presentation$createSharedTexture(@Nullable Supplier<String> supplier, int usage, TextureFormat textureFormat, int i, int j) {
        return this.perfect_presentation$createSharedTexture(this.debugLabels.exists() && supplier != null ? supplier.get() : null, usage, textureFormat, i, j);
    }

    @Override
    public GpuTexture perfect_presentation$createSharedTexture(@Nullable String debugName, int usage, TextureFormat textureFormat, int width, int height) {
        GlTexture glTexture = interopContext.createSharedTexture(debugName, usage, textureFormat, width, height);
        this.debugLabels.applyLabel(glTexture);
        return glTexture;
    }

    @Override
    public void perfect_presentation$resizeSwapChain(int i, int j) {
        interopContext.resizeSwapChain(i, j);
    }

    @Override
    public void perfect_presentation$waitForSwapChainSignal() {
        interopContext.waitForSwapChainSignal();
    }

    @Override
    public void perfect_presentation$blitSharedTextureToSwapChain(SharedGlTexture texture) {
        interopContext.blitSharedTextureToSwapChain(texture);
    }

}
