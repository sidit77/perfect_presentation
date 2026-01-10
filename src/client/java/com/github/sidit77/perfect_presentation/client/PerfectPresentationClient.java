package com.github.sidit77.perfect_presentation.client;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;

import java.util.HashMap;

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

    public final static HashMap<Long, Window> activeWindows = new HashMap<>();

    @Override
    public void onInitializeClient() {

    }

    public static void registerWindow(long windowId, Window window) {
        synchronized (activeWindows) {
            activeWindows.put(windowId, window);
        }
    }

    public static void unregisterWindow(long windowId) {
        synchronized (activeWindows) {
            activeWindows.remove(windowId);
        }
    }

}
