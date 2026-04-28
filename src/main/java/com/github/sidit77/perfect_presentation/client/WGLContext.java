package com.github.sidit77.perfect_presentation.client;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.WGL.*;
import static org.lwjgl.opengl.WGLARBCreateContext.*;
import static org.lwjgl.opengl.WGLARBCreateContextProfile.*;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.windows.GDI32.*;
import static org.lwjgl.system.windows.GDI32.SetPixelFormat;
import static org.lwjgl.system.windows.User32.*;
import static org.lwjgl.system.windows.WindowsUtil.windowsThrowException;

public class WGLContext implements AutoCloseable {

    private final long hwnd;
    private final long hdc;
    private final long hglrc;

    public WGLContext(ContextCreationFlags flags) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            hwnd = CreateWindowEx(
                    pi,
                    0,
                    "STATIC",
                    "Hidden Context Window",
                    WS_POPUP,
                    0, 0, 1, 1,
                    NULL, NULL, NULL, NULL
            );
            if (hwnd == NULL) {
                windowsThrowException("Failed to register WGL window", pi);
            }

            hdc = check(GetDC(hwnd));
            PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.calloc(stack)
                    .nSize((short)PIXELFORMATDESCRIPTOR.SIZEOF)
                    .nVersion((short)1)
                    .dwFlags(PFD_SUPPORT_OPENGL)
                    .iPixelType(PFD_TYPE_RGBA)
                    .iLayerType(PFD_MAIN_PLANE)
                    .cColorBits((byte) 32)
                    .cDepthBits((byte) 24)
                    .cStencilBits((byte) 8);

            int pixelFormat = ChoosePixelFormat(pi, hdc, pfd);
            if (pixelFormat == 0) {
                windowsThrowException("Failed to choose an OpenGL-compatible pixel format", pi);
            }

            if (DescribePixelFormat(pi, hdc, pixelFormat, pfd) == 0) {
                windowsThrowException("Failed to obtain pixel format information", pi);
            }

            if (!SetPixelFormat(pi, hdc, pixelFormat, pfd)) {
                windowsThrowException("Failed to set the pixel format", pi);
            }

            var tempContext = check(wglCreateContext(pi, hdc));
            wglMakeCurrent(pi, hdc, tempContext);

            hglrc = check(wglCreateContextAttribsARB(hdc, 0, new int[] {
                    WGL_CONTEXT_MAJOR_VERSION_ARB, flags.majorVersion,
                    WGL_CONTEXT_MINOR_VERSION_ARB, flags.minorVersion,
                    WGL_CONTEXT_PROFILE_MASK_ARB, switch (flags.profile) {
                        case CORE -> WGL_CONTEXT_CORE_PROFILE_BIT_ARB;
                        case COMPAT -> WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
                        case ANY -> 0;
                    },
                    WGL_CONTEXT_FLAGS_ARB, flags.forwardCompatible ? WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB : 0,
                    0
            }));

            wglMakeCurrent(pi, hdc, 0);
            wglDeleteContext(pi, tempContext);
        }

    }

    public void makeCurrent() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            wglMakeCurrent(pi, hdc, hglrc);
        }
    }

    @Override
    public void close() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            wglMakeCurrent(pi, hdc, 0);
            wglDeleteContext(pi, hglrc);
            ReleaseDC(hwnd, hdc);
            DestroyWindow(pi, hwnd);
        }

    }
}
