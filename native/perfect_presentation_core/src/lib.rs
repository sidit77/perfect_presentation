use std::ffi::{c_int, c_uint};
use windows::core::s;
use windows::Win32::UI::WindowsAndMessaging::GetClientRect;

pub use windows::Win32::Foundation::HWND;
use windows::Win32::Graphics::Gdi::{GetDC, HDC};
use windows::Win32::Graphics::OpenGL::{wglCreateContext, wglDeleteContext, wglGetProcAddress, wglMakeCurrent, ChoosePixelFormat, DescribePixelFormat, SetPixelFormat, SwapBuffers, HGLRC, PFD_DOUBLEBUFFER, PFD_DRAW_TO_WINDOW, PFD_MAIN_PLANE, PFD_SUPPORT_OPENGL, PFD_TYPE_RGBA, PIXELFORMATDESCRIPTOR};

pub const CONTEXT_MAJOR_VERSION_ARB: c_uint = 0x2091;
pub const CONTEXT_MINOR_VERSION_ARB: c_uint = 0x2092;
pub const CONTEXT_PROFILE_MASK_ARB: c_uint = 0x9126;
pub const CONTEXT_CORE_PROFILE_BIT_ARB: c_uint = 0x00000001;
pub const CONTEXT_FLAGS_ARB: c_uint = 0x2094;
pub const CONTEXT_FORWARD_COMPATIBLE_BIT_ARB: c_uint = 0x00000002;

pub struct WglContext {
    dc: HDC,
    rc: HGLRC,
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

            #[allow(non_snake_case)]
            let CreateContextAttribsARB: unsafe extern "system" fn(HDC, HGLRC, *const c_int) -> HGLRC = unsafe {
                wglGetProcAddress(s!("wglCreateContextAttribsARB"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap()
            };

            let glrc = unsafe {
                let t = CreateContextAttribsARB(dc, HGLRC::default(), [
                    CONTEXT_MAJOR_VERSION_ARB, 4,
                    CONTEXT_MINOR_VERSION_ARB, 6,
                    CONTEXT_PROFILE_MASK_ARB, CONTEXT_CORE_PROFILE_BIT_ARB,
                    CONTEXT_FLAGS_ARB, CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
                    0
                ].as_ptr() as _);
                (!t.is_invalid()).then_some(t).ok_or_else(|| windows::core::Error::from_thread()).unwrap()
            };

            unsafe { wglMakeCurrent(dc, HGLRC::default()).unwrap(); }
            unsafe { wglDeleteContext(temp_context).unwrap(); };
            glrc
        };

        Self { dc, rc: glrc }

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