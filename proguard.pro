-keepattributes *
-dontwarn **

-keep class com.github.sidit77.perfect_presentation.** { *; }
-keep class * implements net.fabricmc.api.ModInitializer { *; }
-keep class * implements net.fabricmc.api.ClientModInitializer { *; }
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }


-keepnames,allowobfuscation class windows.win32.system.com.IUnknown {}
-keepnames,allowobfuscation class windows.win32.graphics.direct3d11.ID3D11Texture2D {}
-keepnames,allowobfuscation class windows.win32.graphics.direct3d.ID3DBlob {}
-keepnames,allowobfuscation class windows.win32.graphics.dxgi.IDXGIFactory2 {}

-keepclassmembers class windows.win32.graphics.dxgi.IDXGISwapChain2 {
    java.lang.foreign.MemorySegment iid();
    windows.win32.graphics.dxgi.IDXGISwapChain2 wrap(java.lang.foreign.MemorySegment);
}

-keeppackagenames com.github.sidit77.perfect_presentation
-repackageclasses 'com.github.sidit77.perfect_presentation.internal'