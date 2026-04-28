package com.github.sidit77.perfect_presentation.client;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.WindowsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import windows.win32.graphics.direct3d11.ID3D11ShaderResourceView;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.WGLNVDXInterop.*;
import static org.lwjgl.system.windows.WinBase.GetLastError;

public class SharedGlTexture extends GlTexture {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedGlTexture.class);

    private final long interopDeviceHandle;

    private final ID3D11ShaderResourceView textureView;
    private final long interopHandle;
    private boolean locked = false;

    protected SharedGlTexture(
            ID3D11ShaderResourceView textureView,
            long interopHandle,
            long interopDeviceHandle,
            int usage,
            String string,
            TextureFormat textureFormat,
            int width,
            int height,
            int layers,
            int mipmaps,
            int texId
    ) {
        super(usage, string, textureFormat, width, height, layers, mipmaps, texId);
        this.textureView = textureView;
        this.interopHandle = interopHandle;
        this.interopDeviceHandle = interopDeviceHandle;
    }

    public ID3D11ShaderResourceView getTextureView() {
        return textureView;
    }

    public void lock() {
        if (locked) {
            LOGGER.warn("Shared texture is already locked");
            return;
        }

        try(var memStack = MemoryStack.stackPush()) {
            if(!wglDXLockObjectsNV(interopDeviceHandle, memStack.callocPointer(1).put(0, interopHandle))) {
                IntBuffer pi = memStack.ints(GetLastError());
                WindowsUtil.windowsThrowException("Failed to lock the shared texture", pi);
            }
        }

        locked = true;
    }

    public void unlock() {
        if (!locked) {
            LOGGER.warn("Shared texture is already unlocked");
            return;
        }

        try(var memStack = MemoryStack.stackPush()) {
            if(!wglDXUnlockObjectsNV(interopDeviceHandle, memStack.callocPointer(1).put(0, interopHandle))) {
                IntBuffer pi = memStack.ints(GetLastError());
                WindowsUtil.windowsThrowException("Failed to unlock the shared texture", pi);
            }
        }

        locked = false;
    }

    @Override
    public void close() {
        if(!closed) {
            if (locked) {
                unlock();
            }
            if(!wglDXUnregisterObjectNV(interopDeviceHandle, interopHandle)) {
                try(var memStack = MemoryStack.stackPush()) {
                    IntBuffer pi = memStack.ints(GetLastError());
                    WindowsUtil.windowsThrowException("Failed to unregister the shared texture", pi);
                }
            }
            textureView.Release();
        }
        super.close();
    }
}
