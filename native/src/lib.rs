use std::cell::Cell;
use perfect_presentation_core::{InteropContext, InteropState, SharedTexture, WglContext, HWND};
use std::collections::BTreeMap;
use std::iter::once;
use std::sync::Mutex;


#[repr(transparent)]
#[derive(Debug, Copy, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct WindowIdentifier(usize);

static GLFW_WINDOWS: Mutex<BTreeMap<WindowIdentifier, InteropState>> = Mutex::new(BTreeMap::new());

thread_local! {
    static CURRENT_CONTEXT: Cell<Option<WindowIdentifier>> = Cell::new(None);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn create_context_and_swap_chain(window: WindowIdentifier, hwnd: HWND) -> i32 {
    println!("create_context_and_swap_chain: {:?}", window);

    let context = InteropState::new(hwnd);
    GLFW_WINDOWS.lock().unwrap().insert(window, context);
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn make_context_current(window: WindowIdentifier) -> i32 {
    println!("make_context_current: {:?}", window);

    GLFW_WINDOWS.lock().unwrap().get(&window).unwrap().wgl.make_current();
    CURRENT_CONTEXT.with(|cell| cell.set(Some(window)));
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn swap_buffers(window: WindowIdentifier) -> i32 {
    //println!("swap_buffers: {:?}", window);

    GLFW_WINDOWS.lock().unwrap().get(&window).unwrap().present_swap_chain(1);
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn create_shared_texture(gl_ident: i32, width: i32, height: i32) -> i32 {
    let window = CURRENT_CONTEXT.with(|cell| cell.get().expect("No current context"));
    let mut lock = GLFW_WINDOWS.lock().unwrap();
    let context = lock.get_mut(&window).unwrap();
    let tex = context.create_shared_texture(gl_ident as _, width as _, height as _);
    SharedTexture::lock(once(tex));
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn delete_shared_texture(gl_ident: i32) -> i32 {
    //println!("swap_buffers: {:?}", window);
    let window = CURRENT_CONTEXT.with(|cell| cell.get().expect("No current context"));
    GLFW_WINDOWS.lock().unwrap().get_mut(&window).unwrap().delete_shared_texture(gl_ident as _);
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn blit_shared_texture_to_screen(gl_ident: i32) -> i32 {
    //println!("swap_buffers: {:?}", window);
    let window = CURRENT_CONTEXT.with(|cell| cell.get().expect("No current context"));
    let lock = GLFW_WINDOWS.lock().unwrap();
    let context = lock.get(&window).unwrap();
    let texture = context.get_shared_texture(gl_ident as _);
    context.copy_shared_texture_to_back_buffer(texture);
    0
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn resize_swap_chain(_window: WindowIdentifier, width: i32, height: i32) -> i32 {
    let window = CURRENT_CONTEXT.with(|cell| cell.get().expect("No current context"));
    let lock = GLFW_WINDOWS.lock().unwrap();
    let context = lock.get(&window).unwrap();
    context.resize_swap_chain(width as u32, height as u32);
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
