package com.github.sidit77.perfect_presentation.client;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.WindowsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import windows.win32.foundation.WAIT_EVENT;
import windows.win32.graphics.direct3d.D3D_DRIVER_TYPE;
import windows.win32.graphics.direct3d.D3D_PRIMITIVE_TOPOLOGY;
import windows.win32.graphics.direct3d.ID3DBlob;
import windows.win32.graphics.direct3d11.*;
import windows.win32.graphics.dxgi.*;
import windows.win32.graphics.dxgi.common.DXGI_ALPHA_MODE;
import windows.win32.graphics.dxgi.common.DXGI_FORMAT;
import windows.win32.graphics.dxgi.common.DXGI_SAMPLE_DESC;
import windows.win32.system.com.IUnknown;
import windows.win32.system.com.IUnknownHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.github.sidit77.perfect_presentation.client.WinError.checkSuccessful;
import static com.mojang.blaze3d.platform.GlConst.GL_RGBA8;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.nio.charset.StandardCharsets.*;
import static org.lwjgl.opengl.WGLNVDXInterop.*;
import static org.lwjgl.system.Checks.check;
import static windows.win32.foundation.Apis.CloseHandle;
import static windows.win32.graphics.direct3d.fxc.Apis.D3DCompile;
import static windows.win32.graphics.direct3d11.Apis.D3D11CreateDevice;
import static windows.win32.graphics.direct3d11.Constants.D3D11_SDK_VERSION;
import static windows.win32.graphics.direct3d11.D3D11_CREATE_DEVICE_FLAG.D3D11_CREATE_DEVICE_BGRA_SUPPORT;
import static windows.win32.graphics.direct3d11.D3D11_CREATE_DEVICE_FLAG.D3D11_CREATE_DEVICE_DEBUG;
import static windows.win32.graphics.dxgi.Apis.CreateDXGIFactory1;
import static windows.win32.system.threading.Apis.WaitForSingleObject;

public class InteropContext implements AutoCloseable {

    private static final ThreadLocal<InteropContext> CURRENT_CONTEXT = new ThreadLocal<>();
    public static InteropContext getCurrentContext() {
        var context = CURRENT_CONTEXT.get();
        if(context == null) {
            throw new IllegalStateException("No context bound to the current thread");
        }
        return context;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(InteropContext.class);
    private static final int SWAP_CHAIN_FLAGS = DXGI_SWAP_CHAIN_FLAG.ALLOW_TEARING | DXGI_SWAP_CHAIN_FLAG.FRAME_LATENCY_WAITABLE_OBJECT;

    private final WGLContext openglContext;

    private final long interopDeviceHandle;

    private final ID3D11Device device;
    private final ID3D11DeviceContext context;
    private final IDXGISwapChain2 swapChain;
    private final WaitHandle waitHandle;
    private @Nullable ID3D11RenderTargetView renderTargetView = null;

    private final Map<Integer, SharedTexture> sharedTextures = new HashMap<>();
    private int syncInterval = 1;

    public InteropContext(long hwnd, ContextCreationFlags flags) {
        openglContext = new WGLContext(flags);
        try (var arena = Arena.ofConfined()) {

            {
                var devicePtr = arena.allocate(ADDRESS);
                var contextPtr = arena.allocate(ADDRESS);
                var hr = D3D11CreateDevice(
                        NULL,
                        D3D_DRIVER_TYPE.HARDWARE,
                        NULL,
                        D3D11_CREATE_DEVICE_BGRA_SUPPORT | (PerfectPresentationClient.config.useDxDebugLayer() ? D3D11_CREATE_DEVICE_DEBUG : 0),
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
            }

            this.interopDeviceHandle = check(wglDXOpenDeviceNV(IUnknownHelper.as_raw(device).address()));

            var factory = makeResource(arena, ptr -> CreateDXGIFactory1(IDXGIFactory2.iid(), ptr), IDXGIFactory2::wrap);

            var swapChainDesc = DXGI_SWAP_CHAIN_DESC1.allocate(arena);
            DXGI_SWAP_CHAIN_DESC1.Format(swapChainDesc, DXGI_FORMAT.B8G8R8A8_UNORM);
            DXGI_SAMPLE_DESC.Count(DXGI_SWAP_CHAIN_DESC1.SampleDesc(swapChainDesc), 1);
            DXGI_SAMPLE_DESC.Quality(DXGI_SWAP_CHAIN_DESC1.SampleDesc(swapChainDesc), 0);
            DXGI_SWAP_CHAIN_DESC1.BufferUsage(swapChainDesc, DXGI_USAGE.RENDER_TARGET_OUTPUT);
            DXGI_SWAP_CHAIN_DESC1.BufferCount(swapChainDesc, 2);
            DXGI_SWAP_CHAIN_DESC1.Scaling(swapChainDesc, DXGI_SCALING.NONE);
            DXGI_SWAP_CHAIN_DESC1.SwapEffect(swapChainDesc, DXGI_SWAP_EFFECT.FLIP_DISCARD);
            DXGI_SWAP_CHAIN_DESC1.AlphaMode(swapChainDesc, DXGI_ALPHA_MODE.UNSPECIFIED);
            DXGI_SWAP_CHAIN_DESC1.Flags(swapChainDesc, SWAP_CHAIN_FLAGS);

            var swapChain1 = makeResource(arena,
                    ptr -> factory.CreateSwapChainForHwnd(
                            IUnknownHelper.as_raw(device),
                            MemorySegment.ofAddress(hwnd),
                            swapChainDesc,
                            NULL,
                            NULL,
                            ptr),
                    IDXGISwapChain1::wrap);

            swapChain = comCast(arena, swapChain1, IDXGISwapChain2.class);

            checkSuccessful(swapChain.SetMaximumFrameLatency(1));
            waitHandle = new WaitHandle(swapChain.GetFrameLatencyWaitableObject());

            swapChain1.Release();
            factory.Release();

            var shaderSource = arena.allocateFrom(BLIT_SHADER_SOURCE, UTF_8);

            var vertexShaderBlob = compileShaderSource(arena, shaderSource, "VsMain", "vs_5_0");
            var vertexShader = makeResource(arena, ptr -> device.CreateVertexShader(vertexShaderBlob.GetBufferPointer(), vertexShaderBlob.GetBufferSize(), NULL, ptr), ID3D11VertexShader::wrap);
            vertexShaderBlob.Release();

            var pixelShaderBlob = compileShaderSource(arena, shaderSource, "PsMain", "ps_5_0");
            var pixelShader = makeResource(arena, ptr -> device.CreatePixelShader(pixelShaderBlob.GetBufferPointer(), pixelShaderBlob.GetBufferSize(), NULL, ptr), ID3D11PixelShader::wrap);
            pixelShaderBlob.Release();

            var rasterizerStateDesc = D3D11_RASTERIZER_DESC.allocate(arena);
            D3D11_RASTERIZER_DESC.FillMode(rasterizerStateDesc, D3D11_FILL_MODE.D3D11_FILL_SOLID);
            D3D11_RASTERIZER_DESC.CullMode(rasterizerStateDesc, D3D11_CULL_MODE.D3D11_CULL_NONE);
            var rasterizerState = makeResource(arena, ptr -> device.CreateRasterizerState(rasterizerStateDesc, ptr), ID3D11RasterizerState::wrap);

            var samplerStateDesc = D3D11_SAMPLER_DESC.allocate(arena);
            D3D11_SAMPLER_DESC.Filter(samplerStateDesc, D3D11_FILTER.MIN_MAG_MIP_POINT);
            D3D11_SAMPLER_DESC.AddressU(samplerStateDesc, D3D11_TEXTURE_ADDRESS_MODE.D3D11_TEXTURE_ADDRESS_WRAP);
            D3D11_SAMPLER_DESC.AddressV(samplerStateDesc, D3D11_TEXTURE_ADDRESS_MODE.D3D11_TEXTURE_ADDRESS_WRAP);
            D3D11_SAMPLER_DESC.AddressW(samplerStateDesc, D3D11_TEXTURE_ADDRESS_MODE.D3D11_TEXTURE_ADDRESS_WRAP);
            var samplerState = makeResource(arena, ptr -> device.CreateSamplerState(samplerStateDesc, ptr), ID3D11SamplerState::wrap);

            context.IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY.D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
            context.VSSetShader(IUnknownHelper.as_raw(vertexShader), NULL, 0);
            context.RSSetState(IUnknownHelper.as_raw(rasterizerState));
            context.PSSetShader(IUnknownHelper.as_raw(pixelShader), NULL, 0);
            context.PSSetSamplers(0, 1, arena.allocateFrom(ADDRESS, IUnknownHelper.as_raw(samplerState)));

            vertexShader.Release();
            pixelShader.Release();
            rasterizerState.Release();
            samplerState.Release();
        }
    }

    private static ID3DBlob compileShaderSource(Arena arena, MemorySegment source, String entryPoint, String target) {
        var vertexShaderBlobPtr = arena.allocate(ADDRESS);
        var vertexShaderErrorBlobPtr = arena.allocateFrom(ADDRESS, NULL);
        var hr = D3DCompile(
                source,
                source.byteSize(),
                NULL, NULL, NULL,
                arena.allocateFrom(entryPoint, UTF_8),
                arena.allocateFrom(target, UTF_8),
                0, 0,
                vertexShaderBlobPtr, vertexShaderErrorBlobPtr);
        if(hr != 0) {
            var buffer = ID3DBlob.wrap(vertexShaderErrorBlobPtr.get(ADDRESS, 0));
            var content = buffer.GetBufferPointer().reinterpret(buffer.GetBufferSize()).toArray(ValueLayout.JAVA_BYTE);
            buffer.Release();
            throw new RuntimeException("Failed to compile shader: " + new String(content, UTF_8));
        }
        checkSuccessful(hr);
        return ID3DBlob.wrap(vertexShaderBlobPtr.get(ADDRESS, 0));
    }

    private static <T extends IUnknown> T makeResource(Arena arena, Function<MemorySegment, Integer> factory, Function<MemorySegment, T> wrapper) {
        var ptr = arena.allocate(ADDRESS);
        var hr = factory.apply(ptr);
        checkSuccessful(hr);
        return wrapper.apply(ptr.get(ADDRESS, 0));
    }

    public <T extends IUnknown> T comCast(Arena arena, IUnknown resource, Class<T> clazz){
        try {
            Method iidMethod = clazz.getMethod("iid");
            MemorySegment iid = (MemorySegment) iidMethod.invoke(null);

            Method wrapMethod = clazz.getMethod("wrap", MemorySegment.class);

            var ptr = arena.allocate(ADDRESS);
            checkSuccessful(resource.QueryInterface(iid, ptr));
            return clazz.cast(wrapMethod.invoke(null, ptr.get(ADDRESS, 0)));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeCurrent() {
        openglContext.makeCurrent();
        CURRENT_CONTEXT.set(this);
    }

    public void setSyncInterval(int syncInterval) {
        this.syncInterval = syncInterval;
    }

    public void waitForSwapChainSignal() {
        waitHandle.waitForSignal();
    }

    public void swapChainPresent() {
        checkSuccessful(swapChain.Present(syncInterval, syncInterval == 0 ? DXGI_PRESENT.ALLOW_TEARING : 0));
    }

    public void resizeSwapChain(int width, int height) {
        if(renderTargetView != null) {
            renderTargetView.Release();
            renderTargetView = null;
        }
        checkSuccessful(swapChain.ResizeBuffers(0, width, height, DXGI_FORMAT.UNKNOWN, SWAP_CHAIN_FLAGS));
    }

    public void blitSharedTextureToSwapChain(int glTextureIdentifier) {
        var texture = sharedTextures.get(glTextureIdentifier);
        if(texture == null) {
            throw new IllegalStateException("No shared texture allocated for this identifier: " + glTextureIdentifier);
        }
        texture.unlock();
        try (var arena = Arena.ofConfined()) {
            if(renderTargetView == null) {
                var backBuffer = makeResource(arena, ptr -> swapChain.GetBuffer(0, ID3D11Texture2D.iid(), ptr), ID3D11Texture2D::wrap);

                renderTargetView = makeResource(arena, ptr -> device.CreateRenderTargetView(IUnknownHelper.as_raw(backBuffer), NULL, ptr), ID3D11RenderTargetView::wrap);

                var backBufferDesc = D3D11_TEXTURE2D_DESC.allocate(arena);
                backBuffer.GetDesc(backBufferDesc);

                var viewPort = D3D11_VIEWPORT.allocate(arena);
                D3D11_VIEWPORT.Width(viewPort, D3D11_TEXTURE2D_DESC.Width(backBufferDesc));
                D3D11_VIEWPORT.Height(viewPort, D3D11_TEXTURE2D_DESC.Height(backBufferDesc));
                D3D11_VIEWPORT.MaxDepth(viewPort, 1.0f);

                context.RSSetViewports(1, viewPort);

                backBuffer.Release();
            }

            context.PSSetShaderResources(0, 1, arena.allocateFrom(ADDRESS, IUnknownHelper.as_raw(texture.textureView)));
            context.OMSetRenderTargets(1, arena.allocateFrom(ADDRESS, IUnknownHelper.as_raw(renderTargetView)), NULL);
            context.Draw(3, 0);

        }
        texture.lock();
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

        waitHandle.close();
        swapChain.Release();

        wglDXCloseDeviceNV(interopDeviceHandle);

        context.Release();
        device.Release();

        openglContext.close();
    }

    public class SharedTexture implements AutoCloseable {

        private final ID3D11ShaderResourceView textureView;
        private final long interopHandle;
        private boolean locked = false;

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        public SharedTexture(int glTextureIdentifier, int glTextureType, int glTextureFormat, int width, int height) {
            try (var arena = Arena.ofConfined()) {
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

                var texture = makeResource(arena, ptr -> device.CreateTexture2D(textureDesc, NULL, ptr), ID3D11Texture2D::wrap);
                textureView = makeResource(arena, ptr -> device.CreateShaderResourceView(IUnknownHelper.as_raw(texture), NULL, ptr), ID3D11ShaderResourceView::wrap);

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
            if (locked) {
                unlock();
            }
            if(!wglDXUnregisterObjectNV(interopDeviceHandle, interopHandle)) {
                WindowsUtil.windowsThrowException("Failed to unregister the shared texture");
            }
            textureView.Release();
        }
    }

    private record WaitHandle(MemorySegment handle) implements AutoCloseable {

        public void waitForSignal() {
            try(var arena = Arena.ofConfined()) {
                var errorState = arena.allocate(Linker.Option.captureStateLayout());
                var result = WaitForSingleObject(errorState, handle, 1000);

                switch (result) {
                    case WAIT_EVENT.WAIT_OBJECT_0 -> {}
                    case WAIT_EVENT.WAIT_ABANDONED -> LOGGER.warn("Swap chain wait abandoned unexpectedly");
                    case WAIT_EVENT.WAIT_TIMEOUT -> LOGGER.warn("Swap chain wait timed out");
                    case WAIT_EVENT.WAIT_FAILED -> checkSuccessful(errorState);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to wait for swap chain signal", e);
            }
        }

        @Override
        public void close() {
            try(var arena = Arena.ofConfined()) {
                var errorState = arena.allocate(Linker.Option.captureStateLayout());
                var result = CloseHandle(errorState, handle);
                if(result == 0) {
                    checkSuccessful(errorState);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to close wait handle", e);
            }

        }
    }

    private static final String BLIT_SHADER_SOURCE = """
            struct VSOut
            {
                float4 pos : SV_Position;
                float2 uv  : TEXCOORD0;
            };
            
            VSOut VsMain(uint id : SV_VertexID)
            {
                VSOut o;
                o.pos = float4(id >> 1, id & 1, 0, 0.5) * 4 - 1;
                o.uv  = float2(id >> 1, id & 1) * 2;
            
                return o;
            }
            
            Texture2D srcTex : register(t0);
            SamplerState samp : register(s0);
            
            float4 PsMain(VSOut i) : SV_Target
            {
                return srcTex.Sample(samp, i.uv);
            }
            """;

}
