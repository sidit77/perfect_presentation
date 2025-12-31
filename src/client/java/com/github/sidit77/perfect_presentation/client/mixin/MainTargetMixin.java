package com.github.sidit77.perfect_presentation.client.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import static com.mojang.blaze3d.platform.GlConst.*;

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
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this._resize(i, j, bl));
        } else {
            this._resize(i, j, bl);
        }
    }

    @Unique
    private void _resize(int i, int j, boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._enableDepthTest();
        if (this.frameBufferId >= 0) {
            this.destroyBuffers();
        }

        this.createBuffers(i, j, bl);
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.unbindRead();
        this.unbindWrite();
        if (this.depthBufferId > -1) {
            TextureUtil.releaseTextureId(this.depthBufferId);
            this.depthBufferId = -1;
        }

        if (this.colorTextureId > -1) {
            TextureUtil.releaseTextureId(this.colorTextureId);
            this.colorTextureId = -1;
        }

        if (this.frameBufferId > -1) {
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
            GlStateManager._glDeleteFramebuffers(this.frameBufferId);
            this.frameBufferId = -1;
        }
    }

    @Override
    public void createBuffers(int i, int j, boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();
        int k = RenderSystem.maxSupportedTextureSize();
        if (i > 0 && i <= k && j > 0 && j <= k) {
            this.viewWidth = i;
            this.viewHeight = j;
            this.width = i;
            this.height = j;
            this.frameBufferId = GlStateManager.glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            if (this.useDepth) {
                this.depthBufferId = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(this.depthBufferId);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, 0);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, this.width, this.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
            }

            this.setFilterMode(GL_NEAREST);
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
            GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, this.frameBufferId);
            GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTextureId, 0);
            if (this.useDepth) {
                GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.depthBufferId, 0);
            }

            this.checkStatus();
            this.clear(bl);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + i + "x" + j + " size out of bounds (max. size: " + k + ")");
        }
    }

    @Override
    public void blitToScreen(int i, int j, boolean bl) {
        super.blitToScreen(i, j, bl);
    }
}
