# FoxCompat

Android library for my apps. Work smarter, not harder!

Note: Library is still need some refinements.

You can check the [developer documentation](https://github.com/Fox2Code/FoxCompat/tree/master/documentation)!

## Compatibility & Size

### Required libraries

- `androidx.activity:activity:1.6.1` (Forks are also supported)
- `androidx.appcompat:appcompat:1.5.1` (Forks are also supported)
- `androidx.customview:customview:1.1.0` (Forks are also supported)
- [`dev.rikka.rikkax.layoutinflater:layoutinflater:1.2.0`](https://github.com/RikkaApps/RikkaX/tree/master/layoutinflater)
- [`dev.rikka.rikkax.insets:insets:1.3.0`](https://github.com/RikkaApps/RikkaX/tree/master/insets)

Note: Required libraries are imported automatically.

### Optional libraries

- `com.google.android.material:material:1.7.0` (Forks are also supported)
- [`dev.rikka.rikkax.core:core:1.4.1`](https://github.com/RikkaApps/RikkaX/tree/master/core/core)
- [`com.github.KieronQuinn:MonetCompat:0.4.1`](https://github.com/KieronQuinn/MonetCompat)
- [`org.lsposed.hiddenapibypass:hiddenapibypass:4.3`](https://github.com/LSPosed/AndroidHiddenApiBypass)
- [`com.github.tiann:FreeReflection:3.1.0`](https://github.com/tiann/FreeReflection)

Note: Optional libraries are not imported automatically, and not required to use this library,
but improve some features of the library.

### Supported ROMs

This library contain code to improve support for the following ROMs:

- LineageOS/CyanogenMod (Including forks)
- OneUI/SamsungExperience (Including ports)

## Setup

### Gradle

Add jitpack, example to add to `settings.gradle`:
```groovy
// Only add if `dependencyResolutionManagement` already exists
dependencyResolutionManagement {
    repositories {
        maven {
            url 'https://jitpack.io'
        }
    }
}
```


```groovy
// Only add "repositories" if "dependencyResolutionManagement" didn't exists in "settings.gradle"
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

// Add to existing dependencies block
dependencies {
    implementation 'com.fox2code:FoxCompat:0.1.5'
}
```
