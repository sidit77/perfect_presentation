package com.github.sidit77.perfect_presentation.client;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.WindowsUtil;
import windows.win32.graphics.direct3d.D3D_DRIVER_TYPE;
import windows.win32.graphics.direct3d11.*;
import windows.win32.graphics.dxgi.common.DXGI_FORMAT;
import windows.win32.graphics.dxgi.common.DXGI_SAMPLE_DESC;
import windows.win32.system.com.IUnknownHelper;

import java.lang.foreign.Arena;
import java.util.HashMap;
import java.util.Map;

import static com.github.sidit77.perfect_presentation.client.WinError.checkSuccessful;
import static com.mojang.blaze3d.platform.GlConst.GL_RGBA8;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static org.lwjgl.opengl.WGLNVDXInterop.*;
import static org.lwjgl.system.Checks.check;
import static windows.win32.graphics.direct3d11.Apis.D3D11CreateDevice;
import static windows.win32.graphics.direct3d11.Constants.D3D11_SDK_VERSION;
import static windows.win32.graphics.direct3d11.D3D11_CREATE_DEVICE_FLAG.D3D11_CREATE_DEVICE_BGRA_SUPPORT;
import static windows.win32.graphics.direct3d11.D3D11_CREATE_DEVICE_FLAG.D3D11_CREATE_DEVICE_DEBUG;

public class InteropContext implements AutoCloseable {

    private static final ThreadLocal<InteropContext> CURRENT_CONTEXT = new ThreadLocal<>();
    public static InteropContext getCurrentContext() {
        var context = CURRENT_CONTEXT.get();
        if(context == null) {
            throw new IllegalStateException("No context bound to the current thread");
        }
        return context;
    }

    private final WGLContext openglContext;

    private final long interopDeviceHandle;

    private final ID3D11Device device;
    private final ID3D11DeviceContext context;
    //private IDXGISwapChain1 swapChain;
    private @Nullable ID3D11RenderTargetView renderTargetView = null;

    private final Map<Integer, SharedTexture> sharedTextures = new HashMap<>();

    public InteropContext(long hwnd, ContextCreationFlags flags) {
        openglContext = new WGLContext(hwnd, flags);
        try (var arena = Arena.ofConfined()) {
            var hr = 0;
            var devicePtr = arena.allocate(ADDRESS);
            var contextPtr = arena.allocate(ADDRESS);
            hr = D3D11CreateDevice(
                    NULL,
                    D3D_DRIVER_TYPE.HARDWARE,
                    NULL,
                    D3D11_CREATE_DEVICE_BGRA_SUPPORT | D3D11_CREATE_DEVICE_DEBUG,
                    NULL,
                    0,
                    D3D11_SDK_VERSION,
                    devicePtr,
                    NULL,
                    contextPtr
            );
            checkSuccessful(hr);

            this.device = ID3D11Device.wrap(devicePtr.get(ADDRESS, 0));
            this.context = ID3D11DeviceContext.wrap(contextPtr.get(ADDRESS, 0));

            this.interopDeviceHandle = check(wglDXOpenDeviceNV(IUnknownHelper.as_raw(device).address()));

        }
    }

    public void makeCurrent() {
        openglContext.makeCurrent();
        CURRENT_CONTEXT.set(this);
    }

    public void swapBuffers() {
        openglContext.swapBuffers();
    }

    public void allocateSharedTexture(int glTextureIdentifier, int glTextureType, int glTextureFormat, int width, int height) {
        if(sharedTextures.containsKey(glTextureIdentifier))
            throw new IllegalStateException("Shared texture already allocated for this identifier: " + glTextureIdentifier);

        var texture = new SharedTexture(glTextureIdentifier, glTextureType, glTextureFormat, width, height);
        texture.lock();
        sharedTextures.put(glTextureIdentifier, texture);
    }

    public void deallocateSharedTexture(int glTextureIdentifier) {
        var texture = sharedTextures.remove(glTextureIdentifier);
        if(texture == null)
            throw new IllegalStateException("No shared texture allocated for this identifier: " + glTextureIdentifier);
        texture.close();
    }

    @Override
    public void close() {
        context.ClearState();

        for (var texture : sharedTextures.values()) {
            texture.close();
        }
        sharedTextures.clear();

        if (renderTargetView != null) {
            renderTargetView.Release();
            renderTargetView = null;
        }

        wglDXCloseDeviceNV(interopDeviceHandle);

        context.Release();
        device.Release();

        openglContext.close();
    }

    public class SharedTexture implements AutoCloseable {

        private final ID3D11ShaderResourceView textureView;
        private final long interopHandle;
        private boolean locked = false;

        public SharedTexture(int glTextureIdentifier, int glTextureType, int glTextureFormat, int width, int height) {
            try (var arena = Arena.ofConfined()) {
                var hr = 0;

                var textureDesc = D3D11_TEXTURE2D_DESC.allocate(arena);
                D3D11_TEXTURE2D_DESC.Width(textureDesc, width);
                D3D11_TEXTURE2D_DESC.Height(textureDesc, height);
                D3D11_TEXTURE2D_DESC.MipLevels(textureDesc, 1);
                D3D11_TEXTURE2D_DESC.ArraySize(textureDesc, 1);
                D3D11_TEXTURE2D_DESC.Format(textureDesc, switch (glTextureFormat) {
                    case GL_RGBA8 -> DXGI_FORMAT.R8G8B8A8_UNORM;
                    default -> throw new IllegalStateException("Unexpected value: " + glTextureFormat);
                });
                DXGI_SAMPLE_DESC.Count(D3D11_TEXTURE2D_DESC.SampleDesc(textureDesc), 1);
                DXGI_SAMPLE_DESC.Quality(D3D11_TEXTURE2D_DESC.SampleDesc(textureDesc), 0);
                D3D11_TEXTURE2D_DESC.Usage(textureDesc, D3D11_USAGE.DEFAULT);
                D3D11_TEXTURE2D_DESC.BindFlags(textureDesc,
                        D3D11_BIND_FLAG.D3D11_BIND_RENDER_TARGET | D3D11_BIND_FLAG.D3D11_BIND_SHADER_RESOURCE);
                D3D11_TEXTURE2D_DESC.CPUAccessFlags(textureDesc, 0);
                D3D11_TEXTURE2D_DESC.MiscFlags(textureDesc, 0);

                var texturePtr = arena.allocate(ADDRESS);
                hr = device.CreateTexture2D(textureDesc, NULL, texturePtr);
                checkSuccessful(hr);

                var texture = ID3D11Texture2D.wrap(texturePtr.get(ADDRESS, 0));

                var textureViewPtr = arena.allocate(ADDRESS);
                hr = device.CreateShaderResourceView(IUnknownHelper.as_raw(texture), NULL, textureViewPtr);
                checkSuccessful(hr);
                textureView = ID3D11ShaderResourceView.wrap(textureViewPtr.get(ADDRESS, 0));

                interopHandle = check(wglDXRegisterObjectNV(
                        interopDeviceHandle,
                        IUnknownHelper.as_raw(texture).address(),
                        glTextureIdentifier,
                        glTextureType,
                        WGL_ACCESS_WRITE_DISCARD_NV));

                texture.Release();

            }
        }

        public void lock() {
            if (locked) {
                throw new IllegalStateException("Already locked");
            }

            try(var memStack = MemoryStack.stackPush()) {
                if(!wglDXLockObjectsNV(interopDeviceHandle, memStack.callocPointer(1).put(0, interopHandle))) {
                    WindowsUtil.windowsThrowException("Failed to lock the shared texture");
                }
            }

            locked = true;
        }

        public void unlock() {
            if (!locked) {
                throw new IllegalStateException("Already unlocked");
            }

            try(var memStack = MemoryStack.stackPush()) {
                if(!wglDXUnlockObjectsNV(interopDeviceHandle, memStack.callocPointer(1).put(0, interopHandle))) {
                    WindowsUtil.windowsThrowException("Failed to unlock the shared texture");
                }
            }

            locked = false;
        }

        @Override
        public void close() {
            if (locked) {
                unlock();
            }
            if(!wglDXUnregisterObjectNV(interopDeviceHandle, interopHandle)) {
                WindowsUtil.windowsThrowException("Failed to unregister the shared texture");
            }
            textureView.Release();
        }
    }
}
