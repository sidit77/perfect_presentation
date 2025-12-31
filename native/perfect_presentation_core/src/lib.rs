mod wgl;

use std::ops::Deref;
use windows::Win32::Foundation::HMODULE;
use windows::Win32::UI::WindowsAndMessaging::GetClientRect;

use crate::wgl::{DxDeviceHandle, Wgl, CONTEXT_CORE_PROFILE_BIT_ARB, CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, CONTEXT_MAJOR_VERSION_ARB, CONTEXT_MINOR_VERSION_ARB, CONTEXT_PROFILE_MASK_ARB};
pub use windows::Win32::Foundation::HWND;
use windows::Win32::Graphics::Direct3D11::{D3D11CreateDevice, ID3D11Device, ID3D11DeviceContext, D3D11_CREATE_DEVICE_BGRA_SUPPORT, D3D11_CREATE_DEVICE_DEBUG, D3D11_SDK_VERSION};
use windows::Win32::Graphics::Direct3D::{D3D_DRIVER_TYPE_HARDWARE, D3D_FEATURE_LEVEL_11_1};
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
    pub interop_handle: DxDeviceHandle
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
        
        Self { wgl, device, device_context, interop_handle }
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