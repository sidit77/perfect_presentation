package com.github.sidit77.perfect_presentation.client;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface GpuDeviceExtensions {

    GpuTexture perfect_presentation$createSharedTexture(@Nullable Supplier<String> supplier, int usage, TextureFormat textureFormat, int i, int j);

    GpuTexture perfect_presentation$createSharedTexture(@Nullable String string, int usage, TextureFormat textureFormat, int i, int j);

    void perfect_presentation$swapChainPresent();

    void perfect_presentation$resizeSwapChain(int i, int j);

    void perfect_presentation$waitForSwapChainSignal();

    void perfect_presentation$blitSharedTextureToSwapChain(SharedGlTexture texture);

    void perfect_presentation$setSwapInterval(int interval);

}
