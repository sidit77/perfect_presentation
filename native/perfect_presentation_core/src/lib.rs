mod wgl;

use std::ops::Deref;
use windows::Win32::UI::WindowsAndMessaging::GetClientRect;

use crate::wgl::{Wgl, CONTEXT_CORE_PROFILE_BIT_ARB, CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, CONTEXT_MAJOR_VERSION_ARB, CONTEXT_MINOR_VERSION_ARB, CONTEXT_PROFILE_MASK_ARB};
pub use windows::Win32::Foundation::HWND;
use windows::Win32::Graphics::Gdi::{GetDC, HDC};
use windows::Win32::Graphics::OpenGL::{wglCreateContext, wglDeleteContext, wglMakeCurrent, ChoosePixelFormat, DescribePixelFormat, SetPixelFormat, SwapBuffers, HGLRC, PFD_DOUBLEBUFFER, PFD_DRAW_TO_WINDOW, PFD_MAIN_PLANE, PFD_SUPPORT_OPENGL, PFD_TYPE_RGBA, PIXELFORMATDESCRIPTOR};

pub struct WglContext {
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

unsafe impl Send for WglContext {}
unsafe impl Sync for WglContext {}

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

        Self { dc, rc: glrc, functions }

    }

    pub fn make_current(&self) {
        unsafe { wglMakeCurrent(self.dc, self.rc).unwrap(); }
    }

    pub fn swap_buffers(&self) {
        unsafe { SwapBuffers(self.dc).unwrap(); }
    }

}

pub fn get_window_size(hwnd: isize) -> (i32, i32) {
    let hwnd = HWND(hwnd as _);
    let mut rect = Default::default();
    unsafe { GetClientRect(hwnd, &mut rect).expect("GetClientRect failed"); }
    (rect.right - rect.left, rect.bottom - rect.top)
}