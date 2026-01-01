mod wgl;

use crossbeam_utils::atomic::AtomicCell;
use std::cell::Cell;
use std::collections::BTreeMap;
use std::iter::once;
use std::ops::Deref;
use std::slice::from_raw_parts;
use std::sync::{Arc, Weak};
use windows::core::{s, w};
use windows::Win32::Foundation::{FALSE, HMODULE};
use windows::Win32::UI::WindowsAndMessaging::{CreateWindowExW, DestroyWindow, GetClientRect, WINDOW_EX_STYLE, WS_POPUP};

use crate::wgl::{DxDeviceHandle, DxResourceHandle, GLenum, GLuint, Wgl, ACCESS_WRITE_DISCARD_NV, CONTEXT_CORE_PROFILE_BIT_ARB, CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, CONTEXT_MAJOR_VERSION_ARB, CONTEXT_MINOR_VERSION_ARB, CONTEXT_PROFILE_MASK_ARB};
pub use windows::Win32::Foundation::HWND;
use windows::Win32::Graphics::Direct3D::Fxc::D3DCompile;
use windows::Win32::Graphics::Direct3D::{D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST, D3D_DRIVER_TYPE_HARDWARE, D3D_FEATURE_LEVEL_11_1};
use windows::Win32::Graphics::Direct3D11::{D3D11CreateDevice, ID3D11Device, ID3D11DeviceContext, ID3D11RenderTargetView, ID3D11ShaderResourceView, ID3D11Texture2D, D3D11_BIND_RENDER_TARGET, D3D11_BIND_SHADER_RESOURCE, D3D11_CREATE_DEVICE_BGRA_SUPPORT, D3D11_CREATE_DEVICE_DEBUG, D3D11_CULL_NONE, D3D11_FILL_SOLID, D3D11_FILTER_MIN_MAG_MIP_POINT, D3D11_RASTERIZER_DESC, D3D11_SAMPLER_DESC, D3D11_SDK_VERSION, D3D11_TEXTURE2D_DESC, D3D11_TEXTURE_ADDRESS_WRAP, D3D11_USAGE_DEFAULT, D3D11_VIEWPORT};
use windows::Win32::Graphics::Dxgi::Common::{DXGI_ALPHA_MODE_UNSPECIFIED, DXGI_FORMAT_B8G8R8A8_UNORM, DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_UNKNOWN, DXGI_SAMPLE_DESC};
use windows::Win32::Graphics::Dxgi::{CreateDXGIFactory1, IDXGIFactory2, IDXGISwapChain1, DXGI_PRESENT_ALLOW_TEARING, DXGI_SCALING_NONE, DXGI_SWAP_CHAIN_DESC1, DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING, DXGI_SWAP_EFFECT_FLIP_DISCARD, DXGI_USAGE_RENDER_TARGET_OUTPUT};
use windows::Win32::Graphics::Gdi::{GetDC, ReleaseDC, HDC};
use windows::Win32::Graphics::OpenGL::{wglCreateContext, wglDeleteContext, wglMakeCurrent, ChoosePixelFormat, DescribePixelFormat, SetPixelFormat, SwapBuffers, GL_TEXTURE_2D, HGLRC, PFD_DOUBLEBUFFER, PFD_DRAW_TO_WINDOW, PFD_MAIN_PLANE, PFD_SUPPORT_OPENGL, PFD_TYPE_RGBA, PIXELFORMATDESCRIPTOR};

pub struct WglContext {
    hwnd: HWND,
    dc: HDC,
    rc: HGLRC,
    pub functions: Wgl
}

impl Deref for WglContext {
    type Target = Wgl;

    fn deref(&self) -> &Self::Target {
        &self.functions
    }
}

impl WglContext {

    pub fn create() -> Self {

        let hwnd = unsafe {
            CreateWindowExW(
                WINDOW_EX_STYLE::default(),
                w!("STATIC"),
                w!("Hidden Context Window"),
                WS_POPUP,
                0,
                0,
                1,
                1,
                None,
                None,
                None,
                None
            ).unwrap()
        };

        let dc = unsafe { GetDC(Some(hwnd)) };

        {
            let format = unsafe {
                let format = ChoosePixelFormat(
                    dc,
                    &PIXELFORMATDESCRIPTOR {
                        nSize: size_of::<PIXELFORMATDESCRIPTOR>() as u16,
                        nVersion: 1,
                        dwFlags: PFD_SUPPORT_OPENGL | PFD_DRAW_TO_WINDOW | PFD_DOUBLEBUFFER,
                        iPixelType: PFD_TYPE_RGBA,
                        iLayerType: PFD_MAIN_PLANE.0 as _,
                        cColorBits: 32,
                        cDepthBits: 24,
                        cStencilBits: 8,
                        ..Default::default()
                    }
                );
                (format != 0).then_some(format).ok_or_else(|| windows::core::Error::from_thread()).unwrap()
            };

            let pfd = unsafe {
                let mut pfd = PIXELFORMATDESCRIPTOR::default();
                DescribePixelFormat(dc, format, size_of::<PIXELFORMATDESCRIPTOR>() as u32, Some(&mut pfd));
                pfd
            };

            unsafe { SetPixelFormat(dc, format, &pfd).unwrap() };
        }

        let glrc = {
            let temp_context = unsafe { wglCreateContext(dc).unwrap() };
            unsafe { wglMakeCurrent(dc, temp_context).unwrap() };

            let wgl = Wgl::from_current_context();

            let glrc = unsafe {
                wgl.CreateContextAttribsARB(dc, None, &[
                    CONTEXT_MAJOR_VERSION_ARB, 4,
                    CONTEXT_MINOR_VERSION_ARB, 6,
                    CONTEXT_PROFILE_MASK_ARB, CONTEXT_CORE_PROFILE_BIT_ARB,
                    CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
                    0
                ]).unwrap()
            };

            unsafe { wglMakeCurrent(dc, HGLRC::default()).unwrap(); }
            unsafe { wglDeleteContext(temp_context).unwrap(); };
            glrc
        };

        let functions = unsafe {
            wglMakeCurrent(dc, glrc).unwrap();
            let wgl = Wgl::from_current_context();
            wglMakeCurrent(dc, HGLRC::default()).unwrap();
            wgl
        };

        Self { hwnd, dc, rc: glrc, functions }

    }

    pub fn make_current(&self) {
        unsafe { wglMakeCurrent(self.dc, self.rc).unwrap(); }
    }

    pub fn swap_buffers(&self) {
        unsafe { SwapBuffers(self.dc).unwrap(); }
    }

}

impl Drop for WglContext {
    fn drop(&mut self) {
        unsafe {
            wglMakeCurrent(self.dc, HGLRC::default()).unwrap();
            wglDeleteContext(self.rc).unwrap();
            ReleaseDC(Some(self.hwnd), self.dc);
            DestroyWindow(self.hwnd).unwrap();
        }
    }
}

pub fn get_window_size(hwnd: isize) -> (i32, i32) {
    let hwnd = HWND(hwnd as _);
    let mut rect = Default::default();
    unsafe { GetClientRect(hwnd, &mut rect).expect("GetClientRect failed"); }
    (rect.right - rect.left, rect.bottom - rect.top)
}

const _: () = assert!(AtomicCell::<Option<ID3D11RenderTargetView>>::is_lock_free());

pub struct InteropContext {
    pub wgl: WglContext,
    pub dxgi_factory: IDXGIFactory2,
    pub device: ID3D11Device,
    pub device_context: ID3D11DeviceContext,
    pub swap_chain: IDXGISwapChain1,
    pub interop_handle: DxDeviceHandle,

    cached_render_target_view: AtomicCell<Option<ID3D11RenderTargetView>>,
}

impl InteropContext {
    pub fn new(hwnd: HWND) -> Self {
        let wgl = WglContext::create();

        let dxgi_factory: IDXGIFactory2 = unsafe { CreateDXGIFactory1().unwrap() };

        let debug_layer = cfg!(debug_assertions)
            .then_some(D3D11_CREATE_DEVICE_DEBUG)
            .unwrap_or_default();
        let (device, device_context) = unsafe {
            let mut device = None;
            let mut device_context = None;
            D3D11CreateDevice(
                None,
                D3D_DRIVER_TYPE_HARDWARE,
                HMODULE::default(),
                D3D11_CREATE_DEVICE_BGRA_SUPPORT | debug_layer,
                None,
                D3D11_SDK_VERSION,
                Some(&mut device),
                None,
                Some(&mut device_context),
            ).unwrap();
            (device.unwrap(), device_context.unwrap())
        };

        let swap_chain = unsafe { dxgi_factory.CreateSwapChainForHwnd(
            &device,
            hwnd,
            &DXGI_SWAP_CHAIN_DESC1 {
                Width: 0,
                Height: 0,
                Format: DXGI_FORMAT_B8G8R8A8_UNORM,
                Stereo: FALSE,
                SampleDesc: DXGI_SAMPLE_DESC { Count: 1, Quality: 0 },
                BufferUsage: DXGI_USAGE_RENDER_TARGET_OUTPUT,
                BufferCount: 2,
                Scaling: DXGI_SCALING_NONE,
                SwapEffect: DXGI_SWAP_EFFECT_FLIP_DISCARD,
                AlphaMode: DXGI_ALPHA_MODE_UNSPECIFIED,
                Flags: DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING.0 as _,
            },
            None,
            None
        ).unwrap() };

        let interop_handle = unsafe { wgl.DXOpenDeviceNV(&device).unwrap() };

        let shader_source = include_bytes!("blit_shader.hlsl");
        let vertex_shader_blob = make_resource(|r| unsafe {
            D3DCompile(shader_source.as_ptr() as _, shader_source.len(), s!("blit_shader.hlsl"), None, None, s!("VsMain"), s!("vs_5_0"), 0, 0, r.unwrap(), None)
        });
        let vertex_shader = make_resource(|r| unsafe {
            device.CreateVertexShader(from_raw_parts(vertex_shader_blob.GetBufferPointer() as _, vertex_shader_blob.GetBufferSize()), None, r)
        });

        let pixel_shader_blob = make_resource(|r| unsafe {
            D3DCompile(shader_source.as_ptr() as _, shader_source.len(), s!("blit_shader.hlsl"), None, None, s!("PsMain"), s!("ps_5_0"), 0, 0, r.unwrap(), None)
        });
        let pixel_shader = make_resource(|r| unsafe {
            device.CreatePixelShader(from_raw_parts(pixel_shader_blob.GetBufferPointer() as _, pixel_shader_blob.GetBufferSize()), None, r)
        });

        let rasterizer_state = make_resource(|r| unsafe {
            device.CreateRasterizerState(&D3D11_RASTERIZER_DESC {
                FillMode: D3D11_FILL_SOLID,
                CullMode: D3D11_CULL_NONE,
                ..Default::default()
            }, r)
        });

        let sampler_state = make_resource(|r| unsafe {
            device.CreateSamplerState(&D3D11_SAMPLER_DESC {
                Filter: D3D11_FILTER_MIN_MAG_MIP_POINT,
                AddressU: D3D11_TEXTURE_ADDRESS_WRAP,
                AddressV: D3D11_TEXTURE_ADDRESS_WRAP,
                AddressW: D3D11_TEXTURE_ADDRESS_WRAP,
                ..Default::default()
            }, r)
        });

        unsafe {
            device_context.IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);

            device_context.VSSetShader(&vertex_shader, None);

            device_context.RSSetState(&rasterizer_state);

            device_context.PSSetShader(&pixel_shader, None);
            device_context.PSSetSamplers(0, Some(&[Some(sampler_state)]));
        }

        Self { wgl, dxgi_factory, device, device_context, swap_chain, interop_handle, cached_render_target_view: AtomicCell::new(None) }
    }

    pub fn present_swap_chain(&self, interval: u32) {
        let flags = (interval == 0).then_some(DXGI_PRESENT_ALLOW_TEARING).unwrap_or_default();
        unsafe { self.swap_chain.Present(interval, flags).unwrap(); }
    }

    pub fn copy_shared_texture_to_back_buffer(&self, texture: &SharedTexture) {
        SharedTexture::unlock(once(texture));
        let rtv = self.cached_render_target_view.take().unwrap_or_else(|| make_resource(|r| unsafe {
            let framebuffer = self.swap_chain.GetBuffer::<ID3D11Texture2D>(0).unwrap();
            self.device.CreateRenderTargetView(&framebuffer, None, r)
        }));
        self.cached_render_target_view.store(Some(rtv.clone()));
        unsafe {
            let desc = self.swap_chain.GetDesc1().unwrap();
            self.device_context.RSSetViewports(Some(&[D3D11_VIEWPORT {
                Width: desc.Width as f32,
                Height: desc.Height as f32,
                MaxDepth: 1.0,
                ..Default::default()
            }]));

            self.device_context.PSSetShaderResources(0, Some(&[Some(texture.texture_view.clone())]));
            self.device_context.OMSetRenderTargets(Some(&[Some(rtv)]), None);
            self.device_context.Draw(3, 0);
            //self.device_context.CopyResource(&self.swap_chain.GetBuffer::<ID3D11Texture2D>(0).unwrap(), &texture.texture);

        }
        SharedTexture::lock(once(texture));
    }

    pub fn resize_swap_chain(&self, width: u32, height: u32) {
        self.cached_render_target_view.take();
        unsafe {
            self.swap_chain.ResizeBuffers(0, width, height, DXGI_FORMAT_UNKNOWN, DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING).unwrap();
        }
    }

}

impl Drop for InteropContext {
    fn drop(&mut self) {
        unsafe { self.wgl.functions.DXCloseDeviceNV(self.interop_handle).unwrap(); }
        self.interop_handle = Default::default();
    }
}

unsafe impl Send for InteropContext {}
unsafe impl Sync for InteropContext {}

pub struct InteropState {
    context: Arc<InteropContext>,
    shared_textures: BTreeMap<GLuint, SharedTexture>,
    pub swap_interval: u32,
}

impl Deref for InteropState {
    type Target = InteropContext;

    fn deref(&self) -> &Self::Target {
        &self.context
    }
}

impl InteropState {

    pub fn new(hwnd: HWND) -> Self {
        Self { context: Arc::new(InteropContext::new(hwnd)), shared_textures: Default::default(), swap_interval: 1 }
    }

    pub fn create_shared_texture(&mut self, gl_ident: GLuint, width: u32, height: u32) -> &SharedTexture {
        let shared_texture = SharedTexture::new(&self.context, gl_ident, GL_TEXTURE_2D, RGBA8, width, height);
        let old = self.shared_textures.insert(gl_ident, shared_texture);
        assert!(old.is_none(), "A shared texture with the same gl_ident already exists");
        self.get_shared_texture(gl_ident)
    }

    pub fn get_shared_texture(&self, gl_ident: GLuint) -> &SharedTexture {
        self.shared_textures.get(&gl_ident).expect("Shared texture not found")
    }

    pub fn delete_shared_texture(&mut self, gl_ident: GLuint) {
        self.shared_textures.remove(&gl_ident).expect("Shared texture not found");
    }

}

impl Drop for InteropState {
    fn drop(&mut self) {
        unsafe { self.context.device_context.ClearState(); }
        SharedTexture::unlock(self.shared_textures.values());
        self.shared_textures.clear();
        assert_eq!(Arc::strong_count(&self.context), 1, "InteropContext leaked");
    }
}

pub struct SharedTexture {
    context: Weak<InteropContext>,
    texture_view: ID3D11ShaderResourceView,
    interop_handle: DxResourceHandle,
    locked: Cell<bool>
}

unsafe impl Send for SharedTexture {}
unsafe impl Sync for SharedTexture {}

pub const RENDERBUFFER: GLenum = 0x8D41;
pub const RGBA8: GLenum = 0x8058;

impl SharedTexture {

    fn new(context: &Arc<InteropContext>, gl_ident: GLuint, gl_type: GLenum, format: GLenum, width: u32, height: u32) -> Self {

        let texture = make_resource(|r| unsafe {
            context.device.CreateTexture2D(&D3D11_TEXTURE2D_DESC {
                Width: width,
                Height: height,
                MipLevels: 1,
                ArraySize: 1,
                Format: match format {
                    RGBA8 => DXGI_FORMAT_R8G8B8A8_UNORM,
                    _ => panic!("Unsupported format")
                },
                SampleDesc: DXGI_SAMPLE_DESC { Count: 1, Quality: 0 },
                Usage: D3D11_USAGE_DEFAULT,
                BindFlags: (D3D11_BIND_RENDER_TARGET.0 | D3D11_BIND_SHADER_RESOURCE.0) as _,
                CPUAccessFlags: 0,
                MiscFlags: 0,
            }, None, r)
        });

        let texture_view = make_resource(|r| unsafe {
            context.device.CreateShaderResourceView(&texture, None, r)
        });

        let interop_handle = unsafe {
            context.wgl.DXRegisterObjectNV(context.interop_handle, &texture, gl_ident, gl_type, ACCESS_WRITE_DISCARD_NV).unwrap()
        };

        Self {
            context: Arc::downgrade(context),
            texture_view,
            interop_handle,
            locked: Cell::new(false),
        }
    }

    fn context(&self) -> Arc<InteropContext> {
        self.context.upgrade().expect("SharedTexture outlived its context")
    }

    fn update_lock_status<'a, I: IntoIterator<Item = &'a Self>>(textures: I, locked: bool) {
        let mut handles = heapless::Vec::<DxResourceHandle, 16>::new();
        let mut lock_status = heapless::Vec::<&Cell<bool>, 16>::new();
        let mut context = None;
        for texture in textures {
            assert_ne!(texture.locked.get(), locked);
            assert_eq!(Arc::as_ptr(context.get_or_insert_with(|| texture.context())), texture.context.as_ptr(), "Not all textures belong to the same context");
            handles.push(texture.interop_handle).expect("Can only lock 16 textures at once");
            lock_status.push(&texture.locked).unwrap();
        }
        if let Some(context) = context {
            if locked {
                unsafe { context.wgl.functions.DXLockObjectsNV(context.interop_handle, handles.as_slice()).unwrap(); }
            } else {
                unsafe { context.wgl.functions.DXUnlockObjectsNV(context.interop_handle, handles.as_slice()).unwrap(); }
            }
            for status in lock_status {
                status.set(locked);
            }
        }
    }

    pub fn lock<'a, I: IntoIterator<Item = &'a Self>>(textures: I) {
        SharedTexture::update_lock_status(textures, true);
    }

    pub fn unlock<'a, I: IntoIterator<Item = &'a Self>>(textures: I) {
        SharedTexture::update_lock_status(textures, false);
    }

}

impl Drop for SharedTexture {
    fn drop(&mut self) {
        let context = self.context();
        unsafe {
            if self.locked.get() {
                context.wgl.functions.DXUnlockObjectsNV(context.interop_handle, &[self.interop_handle]).unwrap();
            }
            context.wgl.functions.DXUnregisterObjectNV(context.interop_handle, self.interop_handle).unwrap();
        }
    }
}

#[track_caller]
fn make_resource<T>(func: impl FnOnce(Option<*mut Option<T>>) -> windows::core::Result<()>) -> T {
    let mut obj = None;
    func(Some(&mut obj))
        .expect("Resource creation failed");
    obj.expect("Returned resource is null")
}