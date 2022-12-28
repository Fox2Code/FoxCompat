-dontwarn android.app.ActivityThread
-assumenosideeffects class com.fox2code.foxcompat.app.FoxActivity {
    getStatusBarHeight();
    getActionBarHeight();
    getNavigationBarHeight();
    hasHardwareNavBar();
    hasSoftwareNavBar();
    isLightTheme();
    hasHiddenApis();
    hasNotch();
}
-assumenosideeffects class com.fox2code.foxcompat.app.FoxApplication {
    isLightTheme();
    isSystemProcess();
    hasHiddenApis();
}
-assumenosideeffects class com.fox2code.foxcompat.view.FoxThemeWrapper {
    isLightTheme();
}
-assumenosideeffects class com.fox2code.foxcompat.app.internal.FoxNotch {
    getNotchHeight(com.fox2code.foxcompat.app.FoxActivity);
    getNotchHeightModern(com.fox2code.foxcompat.app.FoxActivity);
    getNotchHeightLegacy(com.fox2code.foxcompat.app.FoxActivity);
}
-assumenosideeffects class com.fox2code.foxcompat.app.internal.FoxCompat {
    checkReflectionInternal(int);
}
-keep,allowobfuscation class com.fox2code.foxcompat.app.internal.FoxCompat {
    <clinit>();
}
