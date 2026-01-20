
# Don't obfuscate anything - we only want to shrink the code
-dontobfuscate
-dontoptimize

# Keep all attributes for better compatibility
-keepattributes *

# Keep your mod code - this is the entry point
-keep class com.github.sidit77.perfect_presentation.** {
    *;
}

# Allow ProGuard to remove unused code from windows.* packages
# By default, ProGuard will remove all unreferenced code from these packages
# No explicit rules needed - just don't add -keep rules for them

# Keep the main Fabric mod initializers
-keep class * implements net.fabricmc.api.ModInitializer {
    *;
}

-keep class * implements net.fabricmc.api.ClientModInitializer {
    *;
}

# Keep mixin classes (they're referenced by name in JSON)
-keep @org.spongepowered.asm.mixin.Mixin class * {
    *;
}

# Don't warn about missing references
-dontwarn **

# Print usage information to understand what's being removed
-printusage build/proguard-usage.txt


# Temporary workaround for reflection use
-keep class windows.win32.graphics.dxgi.IDXGISwapChain2 {
    java.lang.foreign.MemorySegment iid();
    windows.win32.graphics.dxgi.IDXGISwapChain2 wrap(java.lang.foreign.MemorySegment);
}