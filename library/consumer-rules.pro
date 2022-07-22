-assumenosideeffects class com.fox2code.foxcompat.FoxActivity {
    getStatusBarHeight();
    getActionBarHeight();
    getNavigationBarHeight();
    hasHardwareNavBar();
    hasSoftwareNavBar();
    isLightTheme();
}
-assumenosideeffects class com.fox2code.foxcompat.FoxApplication {
    isLightTheme();
}
-assumenosideeffects class com.fox2code.foxcompat.FoxThemeWrapper {
    isLightTheme();
}
-keep,allowobfuscation class com.fox2code.foxcompat.internal.FoxCompat {
    <clinit>();
}
