package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.GpuDeviceExtensions;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    @NotNull
    private Window window;

    //@Shadow
    //private ProfilerFiller profiler;


    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(II)V"))
    void resizeSwapChain(CallbackInfo ci) {
        if (RenderSystem.getDevice() instanceof GpuDeviceExtensions ext) {
            ext.perfect_presentation$resizeSwapChain(window.getWidth(), window.getHeight());
        }
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "CONSTANT", args = "stringValue=render")
    )
    void waitForSwapChain(boolean bl, CallbackInfo ci, @Local(name = "profilerFiller") ProfilerFiller profiler) {
        if (RenderSystem.getDevice() instanceof GpuDeviceExtensions ext) {
            profiler.popPush("vsync");
            ext.perfect_presentation$waitForSwapChainSignal();
        }

    }

    /*
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
    void improveUpdateDisplayTimings(boolean bl, CallbackInfo ci, @Local(name = "profilerFiller") ProfilerFiller profiler) {
        profiler.popPush("framerateLimit");
    }

}
