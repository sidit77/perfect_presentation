package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Context Creation Flags
        - Render resolution scale?
     */

    //public static Config config = new Config(2.0f, true);
    public static Config config = new Config(FabricLoader.getInstance().isDevelopmentEnvironment());

    @Override
    public void onInitializeClient() {

    }

    public record Config(boolean useDxDebugLayer) { }

}
