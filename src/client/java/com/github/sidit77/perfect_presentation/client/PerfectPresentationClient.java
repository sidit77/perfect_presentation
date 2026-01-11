package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Remove the native component
        - Update the readme
        - Disable the DirectX Debug Layer (maybe add a flag to enable it?)
        - Find a better way to handle the debug pie scaling
        - Remove the split source part from gradle as this is client only
     */

    public static Config config = new Config(2.0f, true);

    @Override
    public void onInitializeClient() {

    }

    public record Config(float debugPieScale, boolean useDxDebugLayer) { }

}
