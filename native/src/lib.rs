use perfect_presentation_core::{WglContext, HWND};
use std::collections::BTreeMap;
use std::sync::Mutex;


#[repr(transparent)]
#[derive(Debug, Copy, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct WindowIdentifier(usize);

static GLFW_WINDOWS: Mutex<BTreeMap<WindowIdentifier, WglContext>> = Mutex::new(BTreeMap::new());

#[unsafe(no_mangle)]
pub unsafe extern "C" fn create_context_and_swap_chain(window: WindowIdentifier, hwnd: HWND) -> i32 {
    println!("create_context_and_swap_chain: {:?}", window);

    let context = WglContext::create(hwnd);
    GLFW_WINDOWS.lock().unwrap().insert(window, context);
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn make_context_current(window: WindowIdentifier) -> i32 {
    println!("make_context_current: {:?}", window);

    GLFW_WINDOWS.lock().unwrap().get(&window).unwrap().make_current();
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn swap_buffers(window: WindowIdentifier) -> i32 {
    //println!("swap_buffers: {:?}", window);

    GLFW_WINDOWS.lock().unwrap().get(&window).unwrap().swap_buffers();
    0
}




/*
struct GLFW {
    getWin32Window: unsafe extern "C" fn(window: *const GLFWwindow) -> HWND
}

impl GLFW {
    pub fn load() -> Self {
        let lib = unsafe { LoadLibraryW(w!("glfw3.dll")) }.expect("Failed to load glfw3.dll");
        let getWin32Window = unsafe {
            GetProcAddress(lib, s!("glfwGetWin32Window"))
                .map(|fp| std::intrinsics::transmute(fp))
                .expect("Failed to get glfwGetWin32Window")
        };
        Self { getWin32Window }
    }
}

static GLFW: LazyLock<GLFW> = LazyLock::new(|| GLFW::load());
 */
