package com.github.sidit77.perfect_presentation.client.mixin;

import com.github.sidit77.perfect_presentation.client.PerfectPresentationClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "resize(II)V",
            at = @At("TAIL")
    )
    void resizeSwapChain(int width, int height, CallbackInfo ci) {
        var ext = PerfectPresentationClient.getBackendExtensions();
        if (ext != null) {
            ext.perfect_presentation$resizeSwapChain(width, height);
        }
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
                    ordinal = 0
            )
    )
    void waitForSwapChain(ProfilerFiller instance, String s, Operation<Void> original) {
        var ext = PerfectPresentationClient.getBackendExtensions();
        if (ext != null) {
            instance.push("vsync");
            ext.perfect_presentation$waitForSwapChainSignal();
            instance.pop();
        }

        original.call(instance, s);
    }


}
