package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.PerfectPresentationNativeLibrary;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
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
    private Window window;

    @Inject(method = "resizeDisplay", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(IIZ)V"))
    void resizeSwapChain(CallbackInfo ci) {
        PerfectPresentationNativeLibrary.resizeSwapChain(window.getWindow(), window.getWidth(), window.getHeight());
    }

}
