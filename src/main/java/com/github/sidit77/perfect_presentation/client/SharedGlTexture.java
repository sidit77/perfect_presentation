package com.github.sidit77.perfect_presentation.client;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;

public class SharedGlTexture extends GlTexture {

    private final InteropContext context;

    public SharedGlTexture(InteropContext context, String string, TextureFormat textureFormat, int i, int j, int k, int l) {
        super(string, textureFormat, i, j, k, l);
        this.context = context;
    }

    @Override
    public void close() {
        if(!closed) {
            context.deallocateSharedTexture(this.id);
        }
    }
}
