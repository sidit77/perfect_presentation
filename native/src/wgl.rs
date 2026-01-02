#![allow(non_snake_case, dead_code)]

use std::ffi::{c_int, c_uint, c_void};
use windows::core::{s, Error, IUnknown, Param, BOOL};
use windows::Win32::Foundation::HANDLE;
use windows::Win32::Graphics::Gdi::HDC;
use windows::Win32::Graphics::OpenGL::{wglGetProcAddress, HGLRC};

pub type GLenum = c_uint;
pub type GLuint = c_uint;
pub type GLint = c_int;

pub const CONTEXT_MAJOR_VERSION_ARB: GLenum = 0x2091;
pub const CONTEXT_MINOR_VERSION_ARB: GLenum = 0x2092;
pub const CONTEXT_PROFILE_MASK_ARB: GLenum = 0x9126;
pub const CONTEXT_CORE_PROFILE_BIT_ARB: GLenum = 0x00000001;
pub const CONTEXT_FLAGS_ARB: GLenum = 0x2094;
pub const CONTEXT_FORWARD_COMPATIBLE_BIT_ARB: GLenum = 0x00000002;

pub const ACCESS_READ_ONLY_NV: GLenum = 0x00000000;
pub const ACCESS_READ_WRITE_NV: GLenum = 0x00000001;
pub const ACCESS_WRITE_DISCARD_NV: GLenum = 0x00000002;

//TODO make a special DxDeviceHandle
pub type DxDeviceHandle = HANDLE;
pub type DxResourceHandle = HANDLE;

pub struct Wgl {
    CreateContextAttribsARB: unsafe extern "system" fn(HDC, HGLRC, *const c_int) -> HGLRC,
    DXOpenDeviceNV: unsafe extern "system" fn(*mut c_void) -> DxDeviceHandle,
    DXRegisterObjectNV: unsafe extern "system" fn(DxDeviceHandle, dxObject: *mut c_void, name: GLuint, type_: GLenum, access: GLenum) -> DxResourceHandle,
    DXLockObjectsNV: unsafe extern "system" fn(DxDeviceHandle, GLint, *mut DxResourceHandle) -> BOOL,
    DXUnlockObjectsNV: unsafe extern "system" fn(DxDeviceHandle, GLint, *mut DxResourceHandle) -> BOOL,
    DXUnregisterObjectNV: unsafe extern "system" fn(DxDeviceHandle, hObject: DxResourceHandle) -> BOOL,
    DXCloseDeviceNV: unsafe extern "system" fn(DxDeviceHandle) -> BOOL
}

impl Wgl {
    pub fn from_current_context() -> Self {
        unsafe {
            Self {
                CreateContextAttribsARB: wglGetProcAddress(s!("wglCreateContextAttribsARB"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXOpenDeviceNV: wglGetProcAddress(s!("wglDXOpenDeviceNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXRegisterObjectNV: wglGetProcAddress(s!("wglDXRegisterObjectNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXLockObjectsNV: wglGetProcAddress(s!("wglDXLockObjectsNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXUnlockObjectsNV: wglGetProcAddress(s!("wglDXUnlockObjectsNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXUnregisterObjectNV: wglGetProcAddress(s!("wglDXUnregisterObjectNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
                DXCloseDeviceNV: wglGetProcAddress(s!("wglDXCloseDeviceNV"))
                    .map(|fp| std::mem::transmute(fp))
                    .unwrap(),
            }
        }

    }
}

impl Wgl {
    pub unsafe fn CreateContextAttribsARB(&self, dc: HDC, share_context: Option<HGLRC>, attrib_list: &[GLenum]) -> Result<HGLRC, Error> {
        let t = unsafe { (self.CreateContextAttribsARB)(dc, share_context.unwrap_or_default(), attrib_list.as_ptr() as *const _) };
        (!t.is_invalid()).then_some(t).ok_or_else(|| Error::from_thread())
    }

    pub unsafe fn DXOpenDeviceNV<P: Param<IUnknown>>(&self, dxDevice: P) -> Result<DxDeviceHandle, Error> {
        let t = unsafe { (self.DXOpenDeviceNV)(dxDevice.param().abi()) };
        (!t.is_invalid()).then_some(t).ok_or_else(|| Error::from_thread())
    }

    pub unsafe fn DXRegisterObjectNV<P: Param<IUnknown>>(&self, hDevice: DxDeviceHandle, dxObject: P, name: GLuint, type_: GLenum, access: GLenum) -> Result<DxResourceHandle, Error> {
        let t = unsafe { (self.DXRegisterObjectNV)(hDevice, dxObject.param().abi(), name, type_, access) };
        (!t.is_invalid()).then_some(t).ok_or_else(|| Error::from_thread())
    }

    pub unsafe fn DXLockObjectsNV(&self, hDevice: DxDeviceHandle, hObjects: &[DxResourceHandle]) -> BOOL {
        unsafe { (self.DXLockObjectsNV)(hDevice, hObjects.len() as _, hObjects.as_ptr().cast_mut()) }
    }

    pub unsafe fn DXUnlockObjectsNV(&self, hDevice: DxDeviceHandle, hObjects: &[DxResourceHandle]) -> BOOL {
        unsafe { (self.DXUnlockObjectsNV)(hDevice, hObjects.len() as _, hObjects.as_ptr().cast_mut()) }
    }

    pub unsafe fn DXUnregisterObjectNV(&self, hDevice: DxDeviceHandle, hObject: DxResourceHandle) -> BOOL {
        unsafe { (self.DXUnregisterObjectNV)(hDevice, hObject) }
    }

    pub unsafe fn DXCloseDeviceNV(&self, hDevice: DxDeviceHandle) -> BOOL {
        unsafe { (self.DXCloseDeviceNV)(hDevice) }
    }
}