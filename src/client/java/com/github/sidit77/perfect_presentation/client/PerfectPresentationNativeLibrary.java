package com.github.sidit77.perfect_presentation.client;

import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;

public class PerfectPresentationNativeLibrary {

    private static final String ARCH = switch(System.getProperty("os.arch")) {
        case "amd64" -> "x64";
        default -> throw new IllegalStateException("Unsupported architecture: " + System.getProperty("os.arch"));
    };

    private static final SharedLibrary PP_NATIVE = Library.loadNative(
            PerfectPresentationNativeLibrary.class, "", "natives/" + ARCH + "/perfect_presentation.dll"
    );

    public static final class Functions {

        private Functions() {
        }

        public static final long
                createContextAndSwapChain = apiGetFunctionAddress(PP_NATIVE, "create_context_and_swap_chain"),
                destroyContext = apiGetFunctionAddress(PP_NATIVE, "destroy_context"),
                makeContextCurrent = apiGetFunctionAddress(PP_NATIVE, "make_context_current"),
                swapBuffers = apiGetFunctionAddress(PP_NATIVE, "swap_buffers"),
                setSwapInterval = apiGetFunctionAddress(PP_NATIVE, "set_swap_interval"),
                createSharedTexture = apiGetFunctionAddress(PP_NATIVE, "create_shared_texture"),
                deleteSharedTexture = apiGetFunctionAddress(PP_NATIVE, "delete_shared_texture"),
                blitSharedTextureToScreen = apiGetFunctionAddress(PP_NATIVE, "blit_shared_texture_to_screen"),
                resizeSwapChain = apiGetFunctionAddress(PP_NATIVE, "resize_swap_chain");

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

    public static int setSwapInterval(@NativeType("GLFWwindow*") long window, int swapInterval) {
        long __functionAddress = Functions.setSwapInterval;
        return JNI.invokePI(window, swapInterval, __functionAddress);
    }

    public static int createSharedTexture(
            @NativeType("GLuint") int textureId,
            int width,
            int height
    ) {
        long __functionAddress = Functions.createSharedTexture;
        return JNI.invokeI(textureId, width, height, __functionAddress);
    }

    public static int deleteSharedTexture(@NativeType("GLuint") int textureId) {
        long __functionAddress = Functions.deleteSharedTexture;
        return JNI.invokeI(textureId, __functionAddress);
    }

    public static int blitSharedTextureToScreen(@NativeType("GLuint") int textureId) {
        long __functionAddress = Functions.blitSharedTextureToScreen;
        return JNI.invokeI(textureId, __functionAddress);
    }

    public static int resizeSwapChain(@NativeType("GLFWwindow*") long window, int width, int height) {
        long __functionAddress = Functions.resizeSwapChain;
        return JNI.invokePI(window, width, height, __functionAddress);
    }

    public static int destroyContext(@NativeType("GLFWwindow*") long window) {
        long __functionAddress = Functions.destroyContext;
        return JNI.invokePI(window, __functionAddress);
    }

}
