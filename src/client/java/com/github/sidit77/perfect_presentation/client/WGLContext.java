package com.github.sidit77.perfect_presentation.client;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;

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
            hwnd = check(nCreateWindowEx(
                    0,
                    memAddress(stack.UTF16("STATIC")),
                    memAddress(stack.UTF16("Hidden Context Window")),
                    WS_POPUP,
                    0, 0, 1, 1,
                    NULL, NULL, NULL, NULL
            ));

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

            int pixelFormat = ChoosePixelFormat(hdc, pfd);
            if (pixelFormat == 0) {
                windowsThrowException("Failed to choose an OpenGL-compatible pixel format");
            }

            if (DescribePixelFormat(hdc, pixelFormat, pfd) == 0) {
                windowsThrowException("Failed to obtain pixel format information");
            }

            if (!SetPixelFormat(hdc, pixelFormat, pfd)) {
                windowsThrowException("Failed to set the pixel format");
            }

            var tempContext = check(wglCreateContext(hdc));
            wglMakeCurrent(hdc, tempContext);

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

            wglMakeCurrent(hdc, 0);
            wglDeleteContext(tempContext);
        }



        //org.lwjgl.opengl.WGLNVDXInterop.wglDXOpenDeviceNV(hwnd);
    }

    public void makeCurrent() {
        wglMakeCurrent(hdc, hglrc);
    }

    @Override
    public void close() {
        wglMakeCurrent(hdc, 0);
        wglDeleteContext(hglrc);
        ReleaseDC(hwnd, hdc);
        DestroyWindow(hwnd);
    }
}
