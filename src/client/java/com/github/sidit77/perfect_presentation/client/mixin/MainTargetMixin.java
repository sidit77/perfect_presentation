package com.github.sidit77.perfect_presentation.client.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MainTarget.class)
public class MainTargetMixin extends RenderTarget {

    private MainTargetMixin(boolean bl) { super(bl); }

    /**
     * @author sidit77
     * @reason The backing textures for the main framebuffer must be created using DirectX
     * and exposed via DirectX <-> OpenGL interop to allow for efficient copying into the DXGI swapchain.
     */
    @Overwrite
    private void createFrameBuffer(int i, int j) {
        createBuffers(i, j, false);
    }

    @Override
    public void resize(int i, int j, boolean bl) {
        super.resize(i, j, bl);
    }

    @Override
    public void destroyBuffers() {
        super.destroyBuffers();
    }

    @Override
    public void createBuffers(int i, int j, boolean bl) {
        super.createBuffers(i, j, bl);
    }

    @Override
    public void blitToScreen(int i, int j, boolean bl) {
        super.blitToScreen(i, j, bl);
    }
}
