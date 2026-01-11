package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Remove the split source part from gradle as this is client only
     */

    //public static Config config = new Config(2.0f, true);
    public static Config config = new Config(1.0f, FabricLoader.getInstance().isDevelopmentEnvironment());

    @Override
    public void onInitializeClient() {

    }

    public record Config(float debugPieScale, boolean useDxDebugLayer) { }

}
