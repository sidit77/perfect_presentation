package com.github.sidit77.perfect_presentation.client;

public class ContextCreationFlags {
    public int majorVersion = 3;
    public int minorVersion = 2;
    public Profile profile = Profile.CORE;
    public boolean forwardCompatible = true;

    public enum Profile {
        CORE, COMPAT, ANY
    }

    //I'm not sure if volatile is enough to make a gobal safe,
    // but I'm pretty sure it should only be accessed from a single thread anyway
    public static volatile ContextCreationFlags CURRENT = new ContextCreationFlags();
}
