package com.github.sidit77.perfect_presentation.client;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.WindowsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import windows.win32.graphics.direct3d11.ID3D11ShaderResourceView;

import static org.lwjgl.opengl.WGLNVDXInterop.*;

public class SharedGlTexture extends GlTexture {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedGlTexture.class);

    private final long interopDeviceHandle;

    private final ID3D11ShaderResourceView textureView;
    private final long interopHandle;
    private boolean locked = false;

    protected SharedGlTexture(ID3D11ShaderResourceView textureView, long interopHandle, long interopDeviceHandle, String string, TextureFormat textureFormat, int i, int j, int k, int l) {
        super(string, textureFormat, i, j, k, l);
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
                WindowsUtil.windowsThrowException("Failed to lock the shared texture");
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
                WindowsUtil.windowsThrowException("Failed to unlock the shared texture");
            }
        }

        locked = false;
    }

    @Override
    public void close() {
        if(!closed) {
            System.out.println("Closing shared texture");
            if (locked) {
                unlock();
            }
            if(!wglDXUnregisterObjectNV(interopDeviceHandle, interopHandle)) {
                WindowsUtil.windowsThrowException("Failed to unregister the shared texture");
            }
            textureView.Release();
        }
        super.close();
    }
}
