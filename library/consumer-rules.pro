-dontwarn android.app.ActivityThread
-assumenosideeffects class com.fox2code.foxcompat.FoxActivity {
    getStatusBarHeight();
    getActionBarHeight();
    getNavigationBarHeight();
    hasHardwareNavBar();
    hasSoftwareNavBar();
    isLightTheme();
    hasHiddenApis();
    hasNotch();
}
-assumenosideeffects class com.fox2code.foxcompat.FoxApplication {
    isLightTheme();
    isSystemProcess();
    hasHiddenApis();
}
-assumenosideeffects class com.fox2code.foxcompat.FoxThemeWrapper {
    isLightTheme();
}
-assumenosideeffects class com.fox2code.foxcompat.internal.FoxNotch {
    getNotchHeight(com.fox2code.foxcompat.FoxActivity);
    getNotchHeightModern(com.fox2code.foxcompat.FoxActivity);
    getNotchHeightLegacy(com.fox2code.foxcompat.FoxActivity);
}
-assumenosideeffects class com.fox2code.foxcompat.internal.FoxCompat {
    checkReflectionInternal(int);
}
-keep,allowobfuscation class com.fox2code.foxcompat.internal.FoxCompat {
    <clinit>();
}
