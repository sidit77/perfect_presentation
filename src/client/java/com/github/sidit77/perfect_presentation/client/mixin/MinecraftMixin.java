package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.InteropContextProvider;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

import java.util.Objects;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Final
    @Shadow
    @NotNull
    private Window window;

    @Shadow
    private ProfilerFiller profiler;

    @Unique
    private final float pieScale = 2.0f;

    @SuppressWarnings("resource")
    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(IIZ)V"))
    void resizeSwapChain(CallbackInfo ci) {
        InteropContextProvider provider = (InteropContextProvider)(Object) window;
        provider
                .prefect_presentation$getInteropContext()
                .resizeSwapChain(window.getWidth(), window.getHeight());
    }

    @Inject(
            method = "runTick(Z)V",
            at = @At(value = "CONSTANT", args = "stringValue=render")
    )
    void waitForSwapChain(boolean bl, CallbackInfo ci) {
        profiler.push("vsync");
        //TODO Implement waitable swap chains
        profiler.pop();
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
        return (int)((double)original / pieScale);
    }

    @ModifyExpressionValue(
            method = "renderFpsMeter(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/profiling/ProfileResults;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")
    )
    int scaleProfilePieHeight(int original) {
        return (int)((double)original / pieScale);
    }

}
