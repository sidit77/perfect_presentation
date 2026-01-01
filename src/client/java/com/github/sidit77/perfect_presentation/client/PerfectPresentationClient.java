package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Wrap glfwDestroyWindow call and also destroy to external context
        - Resize the swapchain in the correct location
        - Forward the context creation flags to external context creation
        - Better error handling on the native side
        - Fix the forced always-on-top for the fullscreen mode
        - Improve the whole JNI setup
     */

    public static ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Override
    public void onInitializeClient() {

    }
}
