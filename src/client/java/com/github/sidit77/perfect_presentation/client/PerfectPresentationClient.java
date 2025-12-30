package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Wrap glfwDestroyWindow call and also destroy to external context
        - Wrap the glfwSetSwapInterval call
        - Move the OpenGL context to a hidden window and create a DXGI swapchain the main window
        - Resize the swapchain on window resize
        - Change the proxied swap_buffers function to call swap_chain->present instead
        - Implement the DirectX-backed framebuffers
        - Forward the context creation flags to external context creation
     */

    public static ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Override
    public void onInitializeClient() {

    }
}
