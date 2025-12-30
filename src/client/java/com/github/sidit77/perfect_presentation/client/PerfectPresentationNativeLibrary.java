package com.github.sidit77.perfect_presentation.client;

import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;

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
                createContextAndSwapChain = apiGetFunctionAddress(PP_NATIVE, "create_context_and_swap_chain"),
                makeContextCurrent = apiGetFunctionAddress(PP_NATIVE, "make_context_current"),
                swapBuffers = apiGetFunctionAddress(PP_NATIVE, "swap_buffers");

    }

    public static int createContextAndSwapChain(@NativeType("GLFWwindow*") long window, @NativeType("HWND") long hwnd) {
        long __functionAddress = Functions.createContextAndSwapChain;
        return JNI.invokePPI(window, hwnd, __functionAddress);
    }

    public static int makeContextCurrent(@NativeType("GLFWwindow*") long window) {
        long __functionAddress = Functions.makeContextCurrent;
        return JNI.invokePI(window, __functionAddress);
    }

    public static int swapBuffers(@NativeType("GLFWwindow*") long window) {
        long __functionAddress = Functions.swapBuffers;
        return JNI.invokePI(window, __functionAddress);
    }

}
