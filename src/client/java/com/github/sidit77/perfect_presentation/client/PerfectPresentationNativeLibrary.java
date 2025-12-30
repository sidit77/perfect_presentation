package com.github.sidit77.perfect_presentation.client;

import org.lwjgl.system.Library;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.JNI.invokeI;

public class PerfectPresentationNativeLibrary {

    private static final SharedLibrary PP_NATIVE = Library.loadNative(
            PerfectPresentationNativeLibrary.class,
            "",
            System.getenv("PERFECT_PRESENTATION_NATIVE_LIB")
    );

    public static final class Functions {

        private Functions() {
        }

        public static final long
                hello_word = apiGetFunctionAddress(PP_NATIVE, "hello_word");

    }

    public static int hello_word(int a, int b) {
        long __functionAddress = Functions.hello_word;
        return invokeI(a, b, __functionAddress);
    }

}
