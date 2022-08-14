package com.fox2code.foxcompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.annotation.AttrRes;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.fox2code.foxcompat.internal.FoxAlert;
import com.fox2code.foxcompat.internal.FoxCompat;
import com.fox2code.foxcompat.internal.FoxProcessExt;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import rikka.core.util.ResourceUtils;

public class FoxApplication extends Application implements FoxActivity.ApplicationCallbacks {
    private static final String TAG = "FoxApplication";

    private WeakReference<FoxActivity> mLastCompatActivity;
    private Application mDelegate;
    boolean mOnCreateCalled;
    FoxAlert mFoxAlert;

    public FoxApplication() {}

    @Override
    public AssetManager getAssets() {
        return this.getResources().getAssets();
    }

    @Override
    public Intent patchIntent(FoxActivity compatActivity, Intent intent) {
        return intent;
    }

    @Override
    public void onCreate() {
        mOnCreateCalled = true;
        super.onCreate();
        if (FoxCompat.rikkaXCore) {
            ResourceUtils.setPackageName(this.getPackageName());
        }
        if (mDelegate != null) {
            mDelegate.onCreate();
        }
        Bundle metaData = this.getApplicationInfo().metaData;
        boolean useHiddenApis = metaData != null &&
                metaData.getBoolean("useHiddenApis");
        if (useHiddenApis) {
            if (!FoxCompat.getHiddenApiStatus(this) &&
                    mFoxAlert == null) {
                mFoxAlert = FoxAlert.HIDDEN_APIS_FAIL;
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mDelegate != null) {
            mDelegate.onTerminate();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDelegate != null) {
            mDelegate.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mDelegate != null) {
            mDelegate.onLowMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mDelegate != null) {
            mDelegate.onTrimMemory(level);
        }
    }

    public void setDelegate(Application delegate) {
        mDelegate = delegate;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        FoxProcessExt.register(this);
    }

    public boolean isLightTheme() {
        Resources.Theme theme = this.getTheme();
        TypedValue typedValue = new TypedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            theme.resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
            if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN) {
                return typedValue.data == 1;
            }
        }
        theme.resolveAttribute(android.R.attr.background, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorUtils.calculateLuminance(typedValue.data) > 0.7D;
        }
        throw new IllegalStateException("Theme is not a valid theme!");
    }

    @ColorInt
    public final int getColorCompat(@ColorRes @AttrRes int color) {
        TypedValue typedValue = new TypedValue();
        this.getTheme().resolveAttribute(color, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, color);
    }

    @Override
    @CallSuper
    public void onCreateFoxActivity(FoxActivity foxActivity) {
        mLastCompatActivity = foxActivity.mSelfReference;
        if (mFoxAlert != null) {
            mFoxAlert.show(foxActivity);
            mFoxAlert = null;
        } else if (!mOnCreateCalled) {
            FoxAlert.ON_CREATE_NOT_CALLED.show(foxActivity);
        }
    }

    @Override
    @CallSuper
    public void onCreateEmbeddedFoxActivity(Activity root, FoxActivity embedded) {}

    @Override
    @CallSuper
    public void onRefreshUI(FoxActivity foxActivity) {
        if (!foxActivity.isEmbedded()) {
            mLastCompatActivity = foxActivity.mSelfReference;
        }
    }

    @Nullable
    public FoxActivity getLastCompatActivity() {
        return mLastCompatActivity == null ?
                null : mLastCompatActivity.get();
    }

    public boolean hasHiddenApis() {
        return FoxCompat.getHiddenApiStatus(this);
    }

    public final FoxLineage getLineage() {
        return FoxLineage.getFoxLineage(this);
    }

    /**
     * Returns the name of the current process. A package's default process name
     * is the same as its package name. Non-default processes will look like
     * "$PACKAGE_NAME:$NAME", where $NAME corresponds to an android:process
     * attribute within AndroidManifest.xml.
     */
    public static String getProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        } else {
            return ActivityThread.currentProcessName();
        }
    }

    /**
     * @return the first Application object made in the process.
     */
    @NonNull
    public static Application getInitialApplication() {
        return FoxProcessExt.getInitialApplication();
    }

    @Deprecated // Will be put in a separate library later
    public Application loadApplicationIntoProcess(String packageName)
            throws PackageManager.NameNotFoundException, ReflectiveOperationException {
        if (isSystemProcess()) throw new SecurityException(
                "External loading is not allowed in privileged processes!");
        Application application = FoxProcessExt.getApplication(packageName);
        if (application != null) return application;
        Context context = this.createPackageContext(packageName,
                CONTEXT_IGNORE_SECURITY | CONTEXT_INCLUDE_CODE);
        Context applicationContext = context.getApplicationContext();
        if (applicationContext instanceof Application) {
            application = (Application) applicationContext;
        } else {
            application = FoxProcessExt.makeApplication(context);
        }
        FoxProcessExt.putApplication(application);
        return application;
    }

    @Deprecated // Will be put in a separate library later
    public Application loadApplicationIntoProcess(@NonNull File packageFile)
            throws IOException, ReflectiveOperationException {
        return this.loadApplicationIntoProcess(packageFile, null, null, null);
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Deprecated // Will be put in a separate library later
    public Application loadApplicationIntoProcess(
            @NonNull File packageFile, @Nullable ComponentName activityRedirect,
            @Nullable ComponentName serviceRedirect, @Nullable PackageInfo[] packageInfoPtr)
            throws IOException, ReflectiveOperationException {
        packageFile = packageFile.getAbsoluteFile().getCanonicalFile();
        String filePath = packageFile.getPath();
        PackageInfo packageInfo = this.getPackageManager().getPackageArchiveInfo(filePath,
                PackageManager.GET_META_DATA | PackageManager.GET_ACTIVITIES |
                        PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS |
                        PackageManager.GET_PROVIDERS | PackageManager.GET_INTENT_FILTERS);
        if (packageInfo == null) throw new IOException("Failed to parse package file!");
        Application application = FoxProcessExt.getApplication(packageInfo.packageName);
        if (application != null) return application;
        final BaseDexClassLoader baseDexClassLoader = new BaseDexClassLoader(
                filePath, null, null, Application.class.getClassLoader());
        Resources originalResources = this.getResources();
        Resources resources = new Resources(AssetManager.class.newInstance(),
                originalResources.getDisplayMetrics(), originalResources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ResourcesLoader resourcesLoader = new ResourcesLoader();
            resourcesLoader.addProvider(ResourcesProvider.loadFromApk(
                    ParcelFileDescriptor.open(packageFile, ParcelFileDescriptor.MODE_READ_ONLY)));
            resources.addLoaders(resourcesLoader);
        } else {
            @SuppressWarnings("JavaReflectionMemberAccess")
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.invoke(resources.getAssets(), filePath);
        }
        Application[] applicationPtr = new Application[1];
        // Must be android.view.ContextThemeWrapper because
        // androidx.appcompat.view.ContextThemeWrapper only exists in our classLoader
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, 0) {
            @Override
            public Resources getResources() {
                return resources;
            }

            @Override
            public AssetManager getAssets() {
                return resources.getAssets();
            }

            @Override
            public ClassLoader getClassLoader() {
                return baseDexClassLoader;
            }

            @Override
            public ApplicationInfo getApplicationInfo() {
                return packageInfo.applicationInfo;
            }

            @Override
            public String getPackageName() {
                return packageInfo.packageName;
            }

            @Override
            public Context getApplicationContext() {
                Application application = applicationPtr[0];
                return application == null ? this : application;
            }
        };
        application = applicationPtr[0] =
                FoxProcessExt.makeApplication(contextThemeWrapper);
        FoxProcessExt.putApplication(application);
        if (activityRedirect != null) {
            for (ActivityInfo activityInfo : packageInfo.activities) {
                FoxProcessExt.putRedirect(new ComponentName(
                        activityInfo.packageName, activityInfo.name), activityRedirect);
            }
        }
        if (serviceRedirect != null) {
            for (ServiceInfo serviceInfo : packageInfo.services) {
                FoxProcessExt.putRedirect(new ComponentName(
                        serviceInfo.packageName, serviceInfo.name), serviceRedirect);
            }
        }
        if (packageInfoPtr != null) {
            packageInfoPtr[0] = packageInfo;
        }
        return application;
    }

    @SuppressLint("InlinedApi")
    public static boolean isSystemProcess() {
        return Process.myUid() == Process.ROOT_UID || Process.myUid() == Process.SYSTEM_UID ||
                Process.myUid() == Process.PHONE_UID || Process.myUid() == Process.SHELL_UID;
    }
}
