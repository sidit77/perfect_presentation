package com.github.sidit77.perfect_presentation.client;

import net.fabricmc.api.ClientModInitializer;

public class PerfectPresentationClient implements ClientModInitializer {

    public static ContextCreationFlags contextCreationFlags = new ContextCreationFlags();

    @Override
    public void onInitializeClient() {
        System.out.println("2 + 3 = " + PerfectPresentationNativeLibrary.hello_word(2, 3));
    }
}
