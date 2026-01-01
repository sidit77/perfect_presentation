package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Forward the context creation flags to external context creation
        - Better error handling on the native side
        - Improve the whole JNI setup
        - Waitable swap chain?
     */

    public static ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Override
    public void onInitializeClient() {

    }
}
