package com.github.sidit77.perfect_presentation.client;

import com.github.sidit77.perfect_presentation.client.mixin.GpuDeviceAccessor;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Render resolution scale?
     */

    //public static Config config = new Config(2.0f, true);
    public static Config config = new Config(FabricLoader.getInstance().isDevelopmentEnvironment());

    @Override
    public void onInitializeClient() {

    }

    public record Config(boolean useDxDebugLayer) { }


    public static @Nullable GpuDeviceBackendExtensions getBackendExtensions(GpuDevice device) {
        if(((GpuDeviceAccessor)device).perfect_presentation$getBackend() instanceof GpuDeviceBackendExtensions ext) {
            return ext;
        }
        return null;
    }

    public static @Nullable GpuDeviceBackendExtensions getBackendExtensions() {
        return getBackendExtensions(RenderSystem.getDevice());
    }

}
