package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.InteropContext;
import com.github.sidit77.perfect_presentation.client.InteropContextProvider;
import com.github.sidit77.perfect_presentation.client.PerfectPresentationClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    @NotNull
    private Window window;

    @Shadow
    private ProfilerFiller profiler;

    @Unique
    private InteropContext getInteropContext() {
        return ((InteropContextProvider)(Object) window).prefect_presentation$getInteropContext();
    }

    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(IIZ)V"))
    void resizeSwapChain(CallbackInfo ci) {
        getInteropContext().resizeSwapChain(window.getWidth(), window.getHeight());
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "CONSTANT", args = "stringValue=render")
    )
    void waitForSwapChain(boolean bl, CallbackInfo ci) {
        profiler.push("vsync");
        getInteropContext().waitForSwapChainSignal();
        profiler.pop();
    }

    @WrapOperation(
            method = "runTick(Z)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V")
    )
    void blitWithDirectX(RenderTarget instance, int i, int j, Operation<Void> original) {
        getInteropContext().blitSharedTextureToSwapChain(instance.getColorTextureId());
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getFramerateLimit()I")
    )
    void improveUpdateDisplayTimings(boolean bl, CallbackInfo ci) {
        profiler.popPush("framerateLimit");
    }

    @ModifyExpressionValue(
            method = "renderFpsMeter(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/profiling/ProfileResults;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I")
    )
    int scaleProfilePieWidth(int original) {
        return (int)((double)original / PerfectPresentationClient.config.debugPieScale());
    }

    @ModifyExpressionValue(
            method = "renderFpsMeter(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/profiling/ProfileResults;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")
    )
    int scaleProfilePieHeight(int original) {
        return (int)((double)original / PerfectPresentationClient.config.debugPieScale());
    }

}
