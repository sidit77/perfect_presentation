package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.ContextCreationFlags;
import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

import static com.mojang.blaze3d.opengl.GlConst.GL_TEXTURE_2D;
import static com.mojang.blaze3d.opengl.GlConst.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL12.*;

@Mixin(GlDevice.class)
public class GlDeviceMixin implements GpuDeviceExtensions {

    @Unique
    private InteropContext interopContext;

    @Shadow
    @Final
    private GlDebugLabel debugLabels;

    @WrapOperation(
            method = "Lcom/mojang/blaze3d/opengl/GlDevice;<init>(JIZLjava/util/function/BiFunction;Z)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V")
    )
    void createInteropContext(long window, Operation<Void> original) {
        var hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
        interopContext = new InteropContext(hwnd, new ContextCreationFlags());

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

    @Override
    public GpuTexture perfect_presentation$createSharedTexture(@Nullable Supplier<String> supplier, TextureFormat textureFormat, int i, int j) {
        return this.perfect_presentation$createSharedTexture(this.debugLabels.exists() && supplier != null ? supplier.get() : null, textureFormat, i, j);
    }

    @Override
    public GpuTexture perfect_presentation$createSharedTexture(@Nullable String debugName, TextureFormat textureFormat, int width, int height) {
        GlStateManager.clearGlErrors();
        int texId = GlStateManager._genTexture();
        if (debugName == null) {
            debugName = String.valueOf(texId);
        }

        GlStateManager._bindTexture(texId);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 0);
        if (textureFormat.hasDepthAspect()) {
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, 0);
        }

        interopContext.allocateSharedTexture(texId, GL_TEXTURE_2D, GlConst.toGlInternalId(textureFormat), width, height);

        int m = GlStateManager._getError();
        if (m != 0)
            throw new IllegalStateException("OpenGL error " + m);

        GlTexture glTexture = new SharedGlTexture(interopContext, debugName, textureFormat, width, height, 1, texId);
        this.debugLabels.applyLabel(glTexture);
        return glTexture;
    }

    @Override
    public void perfect_presentation$swapChainPresent() {
        interopContext.swapChainPresent();
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
    public void perfect_presentation$blitSharedTextureToSwapChain(int textureIdentifier) {
        interopContext.blitSharedTextureToSwapChain(textureIdentifier);
    }
}
