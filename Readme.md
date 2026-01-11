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

## Requirements

* A graphics card that supports:

    * DirectX 11
    * OpenGL
    * The `NV_DX_interop2` extension
* Java >= 23

## Known Issues
- V-Sync doesn't seem to reliably work in windowed mode
  -  Whether it works or not seems to depend on whether the java executable is called `javaw.exe` specifically
  - It's likely that there is some kind of compatibility patch from either Windows or the NVIDIA driver involved
  - I'm not sure if screen tearing is even possible in windowed mode with the DXGI flip-model

## License

This project is licensed under an open-source license. See the `LICENSE` file for details.
