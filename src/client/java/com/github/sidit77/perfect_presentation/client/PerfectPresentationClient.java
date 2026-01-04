package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    /*
    TODO:
        - Forward the context creation flags to external context creation
        - Better error handling on the native side
        - Improve the whole JNI setup
        - Use CreateWaitableTimerEx(CREATE_WAITABLE_TIMER_HIGH_RESOLUTION) as fps limiter
            - https://learn.microsoft.com/en-us/windows/win32/sync/waitable-timer-objects
     */

    public static ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Override
    public void onInitializeClient() {

    }
}
