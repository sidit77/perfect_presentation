mod wgl;

use std::cell::Cell;
use std::collections::BTreeMap;
use std::ops::Deref;
use std::sync::{Arc, Weak};
use windows::Win32::Foundation::HMODULE;
use windows::Win32::UI::WindowsAndMessaging::GetClientRect;

use crate::wgl::{DxDeviceHandle, DxResourceHandle, GLenum, GLuint, Wgl, ACCESS_READ_WRITE_NV, CONTEXT_CORE_PROFILE_BIT_ARB, CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, CONTEXT_MAJOR_VERSION_ARB, CONTEXT_MINOR_VERSION_ARB, CONTEXT_PROFILE_MASK_ARB};
pub use windows::Win32::Foundation::HWND;
use windows::Win32::Graphics::Direct3D11::{D3D11CreateDevice, ID3D11Device, ID3D11DeviceContext, ID3D11Texture2D, D3D11_BIND_RENDER_TARGET, D3D11_CREATE_DEVICE_BGRA_SUPPORT, D3D11_CREATE_DEVICE_DEBUG, D3D11_SDK_VERSION, D3D11_TEXTURE2D_DESC, D3D11_USAGE_DEFAULT};
use windows::Win32::Graphics::Direct3D::{D3D_DRIVER_TYPE_HARDWARE, D3D_FEATURE_LEVEL_11_1};
use windows::Win32::Graphics::Dxgi::Common::{DXGI_FORMAT_B8G8R8A8_UNORM, DXGI_SAMPLE_DESC};
use windows::Win32::Graphics::Gdi::{GetDC, ReleaseDC, HDC};
use windows::Win32::Graphics::OpenGL::{wglCreateContext, wglDeleteContext, wglMakeCurrent, ChoosePixelFormat, DescribePixelFormat, SetPixelFormat, SwapBuffers, HGLRC, PFD_DOUBLEBUFFER, PFD_DRAW_TO_WINDOW, PFD_MAIN_PLANE, PFD_SUPPORT_OPENGL, PFD_TYPE_RGBA, PIXELFORMATDESCRIPTOR};

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

    pub fn create(hwnd: HWND) -> Self {
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
        }
    }
}

pub fn get_window_size(hwnd: isize) -> (i32, i32) {
    let hwnd = HWND(hwnd as _);
    let mut rect = Default::default();
    unsafe { GetClientRect(hwnd, &mut rect).expect("GetClientRect failed"); }
    (rect.right - rect.left, rect.bottom - rect.top)
}

pub struct InteropContext {
    pub wgl: WglContext,
    pub device: ID3D11Device,
    pub device_context: ID3D11DeviceContext,
    pub interop_handle: DxDeviceHandle,
    pub shared_textures: BTreeMap<GLuint, SharedTexture>
}

impl InteropContext {
    pub fn new(hwnd: HWND) -> Self {
        let wgl = WglContext::create(hwnd);

        let (device, device_context) = unsafe {
            let mut device = None;
            let mut device_context = None;
            D3D11CreateDevice(
                None,
                D3D_DRIVER_TYPE_HARDWARE,
                HMODULE::default(),
                D3D11_CREATE_DEVICE_BGRA_SUPPORT | D3D11_CREATE_DEVICE_DEBUG, //TODO Add debug flag for debug builds?
                Some(&[D3D_FEATURE_LEVEL_11_1]),
                D3D11_SDK_VERSION,
                Some(&mut device),
                None,
                Some(&mut device_context),
            ).unwrap();
            (device.unwrap(), device_context.unwrap())
        };

        let interop_handle = unsafe { wgl.DXOpenDeviceNV(&device).unwrap() };

        Self { wgl, device, device_context, interop_handle, shared_textures: Default::default() }
    }

    pub fn create_shared_texture(&mut self, gl_ident: GLuint) -> &SharedTexture {
        todo!()
    }

    pub fn get_shared_texture(&self, gl_ident: GLuint) -> &SharedTexture {
        self.shared_textures.get(&gl_ident).expect("Shared texture not found")
    }

    pub fn delete_shared_texture(&mut self, gl_ident: GLuint) {
        self.shared_textures.remove(&gl_ident).expect("Shared texture not found");
    }

}

impl Drop for InteropContext {
    fn drop(&mut self) {
        unsafe { self.device_context.ClearState(); }
        SharedTexture::unlock(self.shared_textures.values());
        self.shared_textures.clear();
        unsafe { self.wgl.functions.DXCloseDeviceNV(self.interop_handle).unwrap(); }
        self.interop_handle = Default::default();
    }
}

unsafe impl Send for InteropContext {}
unsafe impl Sync for InteropContext {}

pub struct SharedTexture {
    context: Weak<InteropContext>,
    texture: ID3D11Texture2D,
    interop_handle: DxResourceHandle,
    locked: Cell<bool>
}

impl SharedTexture {

    fn new(context: &Arc<InteropContext>, gl_ident: GLuint, width: u32, height: u32) -> Self {
        let texture = unsafe {
            let mut temp = None;
            context.device.CreateTexture2D(&D3D11_TEXTURE2D_DESC {
                Width: width,
                Height: height,
                MipLevels: 1,
                ArraySize: 1,
                Format: DXGI_FORMAT_B8G8R8A8_UNORM,
                SampleDesc: DXGI_SAMPLE_DESC { Count: 1, Quality: 0 },
                Usage: D3D11_USAGE_DEFAULT,
                BindFlags: D3D11_BIND_RENDER_TARGET.0 as _,
                CPUAccessFlags: 0,
                MiscFlags: 0,
            }, None, Some(&mut temp)).unwrap();
            temp.unwrap()
        };

        let interop_handle = unsafe {
            pub const RENDERBUFFER: GLenum = 0x8D41;
            context.wgl.DXRegisterObjectNV(context.interop_handle, &texture, gl_ident, RENDERBUFFER, ACCESS_READ_WRITE_NV).unwrap()
        };

        Self {
            context: Arc::downgrade(context),
            texture,
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