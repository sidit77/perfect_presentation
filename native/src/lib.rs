#[unsafe(no_mangle)]
pub unsafe extern "C" fn hello_word(a: i32, b: i32) -> i32 {
    a + b
}
