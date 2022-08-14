package com.fox2code.foxcompat.internal;

import android.app.ActivityThread;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.fox2code.foxcompat.FoxApplication;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Observer;
import java.util.Properties;

/**
 * Implement cross class loader application module
 */
@SuppressWarnings("unchecked")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class FoxProcessExt {
    private static final String REAL_PACKAGE_NAME = "realPackageName";
    private static final String WRAPPED_PACKAGE_NAME = "wrappedPackageName";
    private static final String ON_REDIRECTED = "onRedirectComponentName";
    static final String EXTRA_ORIGINAL_TARGET = "original_target";
    private static final HashMap<String, Object> processExtMap;
    private static final Method attachBaseContext;
    private static final Map<String, Reference<Application>> applications;
    private static final Map<ComponentName, ComponentName> componentsRedirects;
    private static final Map<String, ContentProvider> contentProviders;
    private static final Consumer<ComponentName> onRedirectComponentName;
    private static final String realPackageName, wrappedPackageName;
    private static Reference<Application> initialApplication;

    static {
        Properties properties = System.getProperties();
        HashMap<String, Object> processExtMapTmp =
                (HashMap<String, Object>) properties.get("ext");
        if (processExtMapTmp == null) {
            processExtMapTmp = new HashMap<>();
            properties.put("ext", processExtMapTmp);
        }
        processExtMap = processExtMapTmp;
        applications = obtainMap("applications");
        componentsRedirects = obtainMap("redirects");
        contentProviders = obtainMap("providers");
        onRedirectComponentName = getConsumer(ON_REDIRECTED);
        realPackageName = stringIfy(processExtMapTmp.get(REAL_PACKAGE_NAME));
        wrappedPackageName = stringIfy(processExtMapTmp.get(WRAPPED_PACKAGE_NAME));
        try {
            (attachBaseContext = ContextWrapper.class.getDeclaredMethod(
                    "attachBaseContext", Context.class)).setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try {
            initialApplication = new SoftReference<>(
                    ActivityThread.currentApplication());
        } catch (Throwable throwable) {
            initialApplication = realPackageName != null ?
                    applications.get(realPackageName) : null;
        }
    }

    private static String stringIfy(Object o) {
        if (o == null) return null;
        return String.valueOf(o);
    }

    public static <K, V> Map<K, V> obtainMap(String key) {
        Map<K, V> element = (Map<K, V>) processExtMap.get(key);
        if (element == null) {
            element = new HashMap<>();
            processExtMap.put(key, element);
        }
        return element;
    }

    public static Observer getObserver(String key) {
        return (Observer) processExtMap.get(key);
    }

    public static void putObserver(String key, Observer observer) {
        processExtMap.put(key, observer);
    }

    // Consumer is from AndroidX, so we must change it
    // to Observer for cross class loader compatibility.
    public static <T> Consumer<T> getConsumer(String key) {
        Observer observer = (Observer) processExtMap.get(key);
        if (observer == null) return null;
        return t -> observer.update(null, t);
    }

    public static <T> void putConsumer(String key, Consumer<T> consumer) {
        if (consumer == null) {
            processExtMap.put(key, null);
        } else {
            processExtMap.put(key, (Observer)
                    (o, arg) -> consumer.accept((T) arg));
        }
    }

    public static Application makeApplication(Context applicationContext)
            throws ReflectiveOperationException {
        if (applicationContext instanceof Application) {
            return (Application) applicationContext;
        }
        Application application = getApplication(applicationContext.getPackageName());
        if (application != null) return application; // Try getting local application first.
        String applicationClass = applicationContext.getApplicationInfo().className;
        if (applicationClass == null || applicationClass.isEmpty() ||
                applicationClass.equals("android.app.Application")) {
            application = new Application();
        } else {
            application = Class.forName(applicationClass, false,
                            applicationContext.getClassLoader())
                    .asSubclass(Application.class).newInstance();
        }
        attachBaseContext.invoke(application, applicationContext);
        return application;
    }

    public static void putApplication(@NotNull Application application) {
        applications.put(application.getPackageName(), new WeakReference<>(application));
    }

    public static void putRedirect(
            @NotNull ComponentName from, @NotNull ComponentName to) {
        componentsRedirects.put(from, to);
    }

    @Nullable
    public static Application getApplication(@NotNull String packageName) {
        Reference<Application> applicationReference = applications.get(packageName);
        return applicationReference == null ? null : applicationReference.get();
    }

    public static Application getWrappedApplication() {
        return getApplication(wrappedPackageName);
    }

    public static Application getRealApplication() {
        return getApplication(realPackageName);
    }

    public static void register(FoxApplication foxApplication) {
        applications.put(foxApplication.getPackageName(), new SoftReference<>(foxApplication));
        if (isRootLoader()) processExtMap.put(REAL_PACKAGE_NAME, foxApplication.getPackageName());
        if (initialApplication == null) initialApplication = new WeakReference<>(foxApplication);
    }

    static Intent patchIntent(Intent intent) {
        if (isRootLoader() || intent.hasExtra(EXTRA_ORIGINAL_TARGET))
            return intent; // Do not patch intent twice, or if not needed.
        ComponentName componentName = intent.getComponent();
        if (componentName == null) return intent;
        ComponentName newComponent = componentsRedirects.get(componentName);
        if (newComponent == null) return intent;
        onRedirectComponentName.accept(componentName);
        intent = new Intent(intent);
        if (!newComponent.getPackageName().equals(newComponent.getPackageName()) &&
                !newComponent.getPackageName().equals(realPackageName)) {
            intent.removeExtra("secret"); // Do not allow secret leak
        }
        intent.putExtra(EXTRA_ORIGINAL_TARGET, componentName);
        intent.setComponent(newComponent);
        if (componentName.getPackageName().equals(intent.getPackage())) {
            intent.setPackage(newComponent.getPackageName());
        }
        return intent;
    }

    public static boolean isRootLoader() {
        return wrappedPackageName == null;
    }

    @NonNull
    public static Application getInitialApplication() {
        Application currentApplication = initialApplication == null ?
                null : initialApplication.get();
        if (currentApplication == null) {
            currentApplication = ActivityThread.currentApplication();
            if (isRootLoader()) {
                initialApplication = new SoftReference<>(currentApplication);
            }
        }
        return Objects.requireNonNull(currentApplication);
    }
}
