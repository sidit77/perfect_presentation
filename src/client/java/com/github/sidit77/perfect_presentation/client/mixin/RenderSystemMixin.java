package com.github.sidit77.perfect_presentation.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Redirect(
            method = "flipFrame(J)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V")
    )
    private static void proxySwapBuffers(long window) {
        //We swap the buffers afterward in the WindowMixin.actuallySwapBuffers method
    }

}
