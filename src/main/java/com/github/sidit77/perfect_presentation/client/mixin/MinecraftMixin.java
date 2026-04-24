package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.InteropContext;
import com.github.sidit77.perfect_presentation.client.InteropContextProvider;
import com.github.sidit77.perfect_presentation.client.PerfectPresentationClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    @NotNull
    private Window window;

    //@Shadow
    //private ProfilerFiller profiler;

    @Unique
    private InteropContext getInteropContext() {
        return ((InteropContextProvider)(Object) window).prefect_presentation$getInteropContext();
    }

    /*
    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(II)V"))
    void resizeSwapChain(CallbackInfo ci) {
        getInteropContext().resizeSwapChain(window.getWidth(), window.getHeight());
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "CONSTANT", args = "stringValue=render")
    )
    void waitForSwapChain(boolean bl, CallbackInfo ci, @Local ProfilerFiller profiler) {
        profiler.popPush("vsync");
        getInteropContext().waitForSwapChainSignal();
    }

    @WrapOperation(
            method = "runTick(Z)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V")
    )
    void blitWithDirectX(RenderTarget instance, int i, int j, Operation<Void> original) {
        getInteropContext().blitSharedTextureToSwapChain(instance.getColorTextureId());
    }

     */

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;getFramerateLimit()I")
    )
    void improveUpdateDisplayTimings(boolean bl, CallbackInfo ci, @Local ProfilerFiller profiler) {
        profiler.popPush("framerateLimit");
    }

}
