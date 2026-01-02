# Perfect Presentation

**THIS IS A WINDOWS-ONLY MOD**

Perfect Presentation is a Minecraft Fabric mod that improves Minecraft’s integration with the Windows graphics stack.

It replaces the traditional exclusive fullscreen mode with a borderless windowed mode that:

* Maintains the same frame latency as exclusive fullscreen
* Maintains support for variable refresh rate technologies such as (fullscreen) G-Sync
* Allows seamless Alt-Tabbing
* Works correctly with overlays

Even when running in windowed mode, this mod can improve frame latency and frame pacing.

## Technical Details

This mod replaces Minecraft’s default OpenGL swap chain with a modern DXGI flip-model swap chain.

To accomplish this, the mod moves Minecraft’s OpenGL rendering context into a separate, hidden window. Using DirectX–OpenGL interoperability extensions, the final copy from the internal framebuffer to the back buffer is performed via DirectX11 instead of OpenGL, which allows the use of a DXGI swap chain.

## Supported Platforms

* Windows x64
* Windows ARM64

**Note:** This is a Windows-only mod. While 32-bit Windows builds may be theoretically possible, it is unlikely that a 32-bit system would meet the other requirements.

## Requirements

* A graphics card that supports:

    * DirectX 11
    * OpenGL
    * The `NV_DX_interop2` extension
* Standard Minecraft system requirements

## Build Requirements

This mod contains native code and therefore requires a working Rust toolchain to build.

## License

This project is licensed under an open-source license. See the `LICENSE` file for details.
