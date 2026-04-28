package com.github.sidit77.perfect_presentation.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;


    @Inject(
            method = "close()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/providers/FreeTypeUtil;destroy()V")
    )
    void leakingMainTargetFix(CallbackInfo ci) {
        mainRenderTarget.destroyBuffers();
    }

}
