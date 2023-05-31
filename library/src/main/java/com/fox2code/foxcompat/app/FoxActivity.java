package com.fox2code.foxcompat.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentCaptureOptions;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.autofill.AutofillManager$AutofillClient;
import android.webkit.WebView;

import androidx.annotation.AnimRes;
import androidx.annotation.AttrRes;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.WindowDecorActionBar;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.widget.Openable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.fox2code.foxcompat.app.internal.FoxCompatR;
import com.fox2code.foxcompat.os.FoxNavigationMode;
import com.fox2code.foxcompat.view.FoxDisplay;
import com.fox2code.foxcompat.os.FoxLineage;
import com.fox2code.foxcompat.os.FoxStatusBarManager;
import com.fox2code.foxcompat.view.FoxViewCompat;
import com.fox2code.foxcompat.R;
import com.fox2code.foxcompat.app.internal.FoxCompat;
import com.fox2code.foxcompat.app.internal.FoxNotch;
import com.fox2code.foxcompat.app.internal.FoxIntentActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Objects;

import rikka.core.res.ResourcesCompatLayoutInflaterListener;
import rikka.insets.WindowInsetsHelper;
import rikka.layoutinflater.view.LayoutInflaterFactory;

@SuppressWarnings("unused")
public class FoxActivity extends FoxIntentActivity {
    private static final String EXTRA_FADE_OUT = "extra_fade_out";
    public static final String EXTRA_FOX_FINISH_ANIMATION = "extra_fox_finish_animation";
    static final Handler handler = new Handler(Looper.getMainLooper());
    public static final int INTENT_ACTIVITY_REQUEST_CODE = 0x01000000;
    private static final String TAG = "FoxActivity";
    public static final OnBackPressedCallback DISABLE_BACK_BUTTON =
            new OnBackPressedCallback() {
                @Override
                public boolean onBackPressed(FoxActivity compatActivity) {
                    compatActivity.setOnBackPressedCallback(this);
                    return true;
                }
            };
    public static final Animation[] ANIMATIONS = Animation.values();
    LayoutInflaterFactory mLayoutInflaterFactory;
    private FoxStatusBarManager mFoxStatusBarManager;
    final WeakReference<FoxActivity> mSelfReference;
    final FoxBackGestureHelper mFoxBackGestureHelper;
    private OnActivityResultCallback mOnActivityResultCallback;
    private MenuItem.OnMenuItemClickListener mMenuClickListener;
    private CharSequence mMenuContentDescription;
    private FoxActivityView mFoxActivityView;
    private boolean mShouldNullifyWindow;
    private int mDisplayCutoutHeight = 0;
    @Rotation private int mCachedRotation = 0;
    FoxNavigationMode mFoxNavigationMode;
    @StyleRes private int mSetThemeDynamic = 0;
    private boolean mAwaitOnWindowUpdate = false;
    private boolean mOnCreateCalledOnce = false;
    private boolean mOnCreateCalled = false;
    private boolean mIsRefreshUi = false;
    private boolean mHasHardwareNavBar;
    private boolean mShouldSkipRefreshUi;
    private Drawable mActionBarBackground;
    private Boolean mActionBarHomeAsUp;
    private Boolean mActionBarVisible;
    private FoxAppTask mAppTask;
    private Openable mDrawerOpenable;
    private int mDrawableResId;
    private MenuItem mMenuItem;
    boolean mUseDynamicColors;

    public FoxActivity() {
        mSelfReference = new WeakReference<>(this);
        mFoxBackGestureHelper = new FoxBackGestureHelper(this);
    }

    void attachFoxActivityView(FoxActivityView foxActivityView) {
        if (!(this instanceof Embeddable))
            throw new IllegalStateException("Activity is not embeddable!");
        if (foxActivityView.mFoxActivity != null)
            throw new IllegalStateException("FoxActivityView already bound");
        Context applicationContext = foxActivityView.getApplicationContext();
        super.attachBaseContextReal(applicationContext == null ?
                new ContextWrapper(foxActivityView.getContext()) {
            public void setContentCaptureOptions(ContentCaptureOptions contentCaptureOptions) {}

            public void setAutofillClient(AutofillManager$AutofillClient client) {}
        } : new ContextWrapper(foxActivityView.getContext()) {
            public void setContentCaptureOptions(ContentCaptureOptions contentCaptureOptions) {}

            public void setAutofillClient(AutofillManager$AutofillClient client) {}

            @Override
            public Resources.Theme getTheme() {
                return applicationContext.getTheme();
            }

            @Override
            public ClassLoader getClassLoader() {
                return applicationContext.getClassLoader();
            }

            @Override
            public String getPackageName() {
                return applicationContext.getPackageName();
            }

            @Override
            public ApplicationInfo getApplicationInfo() {
                return applicationContext.getApplicationInfo();
            }

            @Override
            public Resources getResources() {
                return applicationContext.getResources();
            }

            @Override
            public AssetManager getAssets() {
                return applicationContext.getAssets();
            }

            @Override
            public Context getApplicationContext() {
                Context context = applicationContext.getApplicationContext();
                return context == null ? applicationContext : context;
            }
        });
        foxActivityView.mFoxActivity = this;
        mFoxActivityView = foxActivityView;
        mOnCreateCalledOnce = true;
    }

    public void setUseDynamicColors(boolean useDynamicColors) {
        mUseDynamicColors = useDynamicColors;
    }

    public boolean useDynamicColors() {
        return mUseDynamicColors;
    }

    @Override
    public void setContentView(int layoutResID) {
        if (mFoxActivityView != null) {
            mFoxActivityView.removeAllViewsInLayout();
            LayoutInflater.from(this).inflate(
                    layoutResID, mFoxActivityView.mRealFrameLayout);
        } else super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        if (mFoxActivityView != null) {
            mFoxActivityView.mRealFrameLayout.removeAllViews();
            mFoxActivityView.mRealFrameLayout.addView(view);
        } else super.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mFoxActivityView != null) {
            mFoxActivityView.mRealFrameLayout.removeAllViews();
            mFoxActivityView.mRealFrameLayout.addView(view, params);
        } else super.setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (mFoxActivityView != null) {
            mFoxActivityView.mRealFrameLayout.addView(view, params);
        } else super.addContentView(view, params);
    }

    // Post Windows API
    void postWindowUpdated() {
        if (mAwaitOnWindowUpdate) return;
        mAwaitOnWindowUpdate = true;
        handler.post(() -> {
            mAwaitOnWindowUpdate = false;
            if (this.isFinishing()) return;
            mCachedRotation = this.getRotation();
            mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            this.onWindowUpdated();
        });
    }

    /**
     * Function to detect when Window state is updated
     * */
    protected void onWindowUpdated() {
        // No-op
    }

    public final void postOnUiThread(Runnable action) {
        handler.post(action);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (!mOnCreateCalledOnce) {
            this.getLayoutInflater().setFactory2(mLayoutInflaterFactory =
                    (this instanceof Embeddable ? this.createLayoutInflaterFactory0() :
                            this.createLayoutInflaterFactory()));
            mHasHardwareNavBar = this.hasHardwareNavBar0();
            mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            mCachedRotation = this.getRotation();
            mFoxBackGestureHelper.onCreateOnce();
            mShouldSkipRefreshUi = true;
            mOnCreateCalledOnce = true;
        }
        Application application = this.getApplicationIntent();
        if (mFoxActivityView == null) {
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application).onCreateFoxActivity(this);
            }
        } else {
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application).onCreateEmbeddedFoxActivity(
                        mFoxActivityView.getParentFoxActivity(), this);
            }
        }
        Resources.Theme theme = this.getTheme();
        boolean isLightTheme = FoxDisplay.isLightTheme(theme);
        boolean useDynamicColors = mUseDynamicColors &&
                FoxCompat.isDynamicAccentSupported(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && useDynamicColors) {
            theme.applyStyle(isLightTheme ?
                    R.style.FoxCompat_Overrides_System :
                    FoxLineage.getFoxLineage(this).isBlackMode() ?
                            R.style.FoxCompat_Overrides_System_Black :
                    R.style.FoxCompat_Overrides_System_Dark, true);
        } else {
            theme.applyStyle(FoxCompat.isOxygenOS() ?
                    R.style.FoxCompat_Overrides_Base_Oxygen :
                    FoxCompat.isDynamicAccentSupported(this) ?
                    R.style.FoxCompat_Overrides_Base_System :
                    R.style.FoxCompat_Overrides_Base, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors) {
                theme.applyStyle(isLightTheme ?
                        FoxCompatR.style.ThemeOverlay_Material3_DynamicColors_Light :
                        FoxCompatR.style.ThemeOverlay_Material3_DynamicColors_Dark, true);
            }
        }
        if (mFoxActivityView == null) {
            applyWindowThemeWorkarounds(theme);
            mShouldNullifyWindow = false;
            super.onCreate(savedInstanceState);
        } else {
            mShouldNullifyWindow = true;
            try {
                super.onCreate(savedInstanceState);
            } catch (NullPointerException ignored) {} finally {
                mShouldNullifyWindow = false;
            }
        }
        mOnCreateCalled = true;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mOnCreateCalledOnce) {
            mFoxBackGestureHelper.resetOnBackPressedTimeOut();
        }
    }

    final LayoutInflaterFactory createLayoutInflaterFactory0() {
        LayoutInflaterFactory layoutInflaterFactory =
                new LayoutInflaterFactory(this.getDelegate())
                        .addOnViewCreatedListener(WindowInsetsHelper.Companion.getLISTENER())
                        .addOnViewCreatedListener(FoxCompat.SWITCH_COMPAT_CRASH_FIX)
                        .addOnViewCreatedListener(FoxCompat.TOOLBAR_ALIGNMENT_FIX);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && FoxCompat.cardView) {
            layoutInflaterFactory.addOnViewCreatedListener(FoxCompat.CARD_VIEW_COLOR_FIX);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && FoxCompat.rikkaXCore) {
            layoutInflaterFactory.addOnViewCreatedListener(
                    ResourcesCompatLayoutInflaterListener.getInstance());
        }
        if (!(Build.VERSION.SDK_INT >= 31)
                && FoxCompat.monetCompat) { // Add Overscroll effect with Monet library
            layoutInflaterFactory.addOnViewCreatedListener(FoxCompat.STRETCH_OVERSCROLL);
        }
        return layoutInflaterFactory;
    }

    protected LayoutInflaterFactory createLayoutInflaterFactory() {
        return this.createLayoutInflaterFactory0();
    }

    @Override
    protected void onResume() {
        mAppTask = null;
        mHasHardwareNavBar = this.hasHardwareNavBar0();
        mFoxNavigationMode = FoxNavigationMode.queryForActivity(this);
        if (mFoxActivityView == null) {
            super.onResume();
            mFoxBackGestureHelper.resetOnBackPressedTimeOut();
            mFoxBackGestureHelper.updateState();
        }
        if (mShouldSkipRefreshUi) {
            Log.d(TAG, "Skipping refreshUI() call, as activity was just created");
            mShouldSkipRefreshUi = false;
        } else {
            this.refreshUI();
        }
    }

    @Override
    protected void onPause() {
        mAppTask = null;
        mShouldSkipRefreshUi = false;
        if (mFoxActivityView == null) {
            super.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        mAppTask = null;
        if (mFoxActivityView == null) {
            super.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (FoxStatusBarManager.FOX_STATUS_BAR_SERVICE.equals(name)) {
            return this.getStatusBarManager();
        }
        return super.getSystemService(name);
    }

    public final FoxStatusBarManager getStatusBarManager() {
        if (mFoxStatusBarManager == null) {
            mFoxStatusBarManager = FoxStatusBarManager.createForActivity(this);
        }
        return mFoxStatusBarManager;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        FoxLineage.getFoxLineage(newBase).fixConfiguration(
                newBase.getResources().getConfiguration());
        super.attachBaseContext(newBase);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        FoxLineage.getFoxLineage(this).fixConfiguration(newConfig);
        super.onConfigurationChanged(newConfig);
        if (mCachedRotation != this.getRotation() &&
                mOnCreateCalledOnce && !mAwaitOnWindowUpdate) {
            mCachedRotation = this.getRotation();
            mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            this.onWindowUpdated();
        }
    }

    @Override @CallSuper @RequiresApi(Build.VERSION_CODES.O)
    public void onMultiWindowModeChanged(
            boolean isInMultiWindowMode, @NonNull Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        this.postWindowUpdated();
    }

    @Override
    public void finish() {
        mOnActivityResultCallback = null;
        Animation finishAnimation = null;
        if (mOnCreateCalled) {
            if (this.getIntent().getBooleanExtra(EXTRA_FADE_OUT, false)) {
                finishAnimation = Animation.FADING;
            }
            int index = this.getIntent().getIntExtra(EXTRA_FOX_FINISH_ANIMATION, -1);
            try {
                finishAnimation = ANIMATIONS[index];
            } catch (IndexOutOfBoundsException ignored) {}
        }
        super.finish();
        if (finishAnimation != null) {
            super.overridePendingTransition(
                    finishAnimation.inAnimFinish,
                    finishAnimation.outAnimFinish);
        }
    }

    @Override
    public void recreate() {
        if (mFoxActivityView != null) {
            mFoxActivityView.recreate();
        } else {
            super.recreate();
        }
    }

    @CallSuper
    public void refreshUI() {
        // Avoid recursive calls
        if (mIsRefreshUi || !mOnCreateCalled) return;
        mIsRefreshUi = true;
        try {
            mCachedRotation = this.getRotation();
            mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            Application application = this.getApplication();
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application)
                        .onRefreshUI(this);
            }
            this.postWindowUpdated();
        } finally {
            mIsRefreshUi = false;
        }
    }

    public final void forceBackPressed() {
        mFoxBackGestureHelper.forceBackPressed();
    }

    public final void pressBack() {
        mFoxBackGestureHelper.pressBack();
    }

    /**
     * Use {@link #forceBackPressed()} instead.
     * <p>
     * For WebView integration use {@link #linkWebViewToBackButton(WebView)}
     */
    @Override
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public void onBackPressed() {
        mFoxBackGestureHelper.onBackPressed();
    }

    void super_onBackPressed() {
        if (mFoxActivityView == null) {
            super.onBackPressed();
        }
    }

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        if (mFoxActivityView != null) return;
        mActionBarHomeAsUp = showHomeAsUp;
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        if (compatActionBar != null) {
            compatActionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
            }
        }
    }

    public void hideActionBar() {
        mActionBarVisible = false;
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        if (compatActionBar != null) {
            compatActionBar.hide();
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null)
                actionBar.hide();
        }
    }

    public void showActionBar() {
        mActionBarVisible = true;
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        if (compatActionBar != null) {
            compatActionBar.show();
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null)
                actionBar.show();
        }
    }

    public View getActionBarView() {
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        if (compatActionBar != null) {
            return compatActionBar.getCustomView();
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            return actionBar != null ? actionBar.getCustomView() : null;
        }
    }

    public void disableDefaultSupportActionBar() {
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar instanceof WindowDecorActionBar) {
            throw new IllegalStateException( // Add message for developers! I hope it's enough! :3
                    "Cannot call setSupportActionBar() while default action bar is being used!\n" +
                    "Call disableDefaultSupportActionBar() early in onCreate() to use this method!");
        }
        if (actionBar != null) {
            if (mActionBarVisible != null &&
                    mActionBarVisible != actionBar.isShowing()) {
                mActionBarVisible = null;
            }
        }
        mMenuItem = null;
        super.setSupportActionBar(toolbar);
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            if (mActionBarBackground != null) {
                actionBar.setBackgroundDrawable(mActionBarBackground);
            }
            if (mActionBarHomeAsUp != null) {
                actionBar.setDisplayHomeAsUpEnabled(mActionBarHomeAsUp);
            }
            if (mActionBarVisible != null) {
                if (mActionBarVisible) {
                    actionBar.show();
                } else {
                    actionBar.hide();
                }
            }
        }
    }

    @Dimension @Px
    public int getActionBarHeight() {
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        View customView = null;
        if (compatActionBar != null) {
            return compatActionBar.isShowing() || ((customView =
                    compatActionBar.getCustomView()) != null &&
                    customView.getVisibility() == View.VISIBLE) ?
                    Math.max(customView == null ? 0 : customView.getHeight(),
                            compatActionBar.getHeight()) : 0;
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            return actionBar != null && (actionBar.isShowing() || ((
                    customView = actionBar.getCustomView()) != null &&
                    customView.getVisibility() == View.VISIBLE)) ?
                    Math.max(customView == null ? 0 : customView.getHeight(),
                            actionBar.getHeight()) : 0;
        }
    }

    public void setActionBarBackground(Drawable drawable) {
        if (mFoxActivityView != null) return;
        if (drawable == null) drawable = FoxViewCompat.NULL_DRAWABLE;
        mActionBarBackground = drawable;
        androidx.appcompat.app.ActionBar compatActionBar;
        try {
            compatActionBar = this.getSupportActionBar();
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSupportActionBar", e);
            compatActionBar = null; // Allow fallback to builtin actionBar.
        }
        if (compatActionBar != null) {
            compatActionBar.setBackgroundDrawable(drawable);
        } else {
            ActionBar actionBar = this.getSupportActionBar();
            if (actionBar != null)
                actionBar.setBackgroundDrawable(drawable);
        }
    }

    public boolean isActivityWindowed() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (super.isInMultiWindowMode() || super.isInPictureInPictureMode())) ||
                mFoxActivityView != null; // If Activity is in foxActivityView assume windowed
    }

    public ViewGroup getContentView() {
        if (mFoxActivityView != null)
            return mFoxActivityView.mRealFrameLayout;
        return findViewById(android.R.id.content);
    }

    @Nullable
    public WindowInsetsCompat getWindowInsets() {
        if (mFoxActivityView != null) return null;
        View view = this.getContentView();
        return view != null ? ViewCompat.getRootWindowInsets(view) : null;
    }

    /**
     * @return Activity status bar height, may be 0 if not affecting the activity.
     */
    @Dimension @Px
    @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    public int getStatusBarHeight() {
        if (mFoxActivityView != null) return 0;
        // Check display cutout height
        int height = this.getRotation() == 0 ?
                mDisplayCutoutHeight : 0;
        // Check consumed insets
        boolean windowed = this.isActivityWindowed();
        WindowInsetsCompat windowInsetsCompat = this.getWindowInsets();
        if (windowInsetsCompat != null || windowed) {
            if (windowInsetsCompat == null) // Fallback for windowed mode
                windowInsetsCompat = WindowInsetsCompat.CONSUMED;
            Insets insets = windowInsetsCompat.getInsets(
                    WindowInsetsCompat.Type.statusBars());
            if (windowed) return Math.max(insets.top, 0);
            height = Math.max(height, insets.top);
        }
        // Check system resources
        int id = Resources.getSystem().getIdentifier(
                "status_bar_height_default", "dimen", "android");
        if (id <= 0) {
            id = Resources.getSystem().getIdentifier(
                    "status_bar_height", "dimen", "android");
        }
        return id <= 0 ? height : Math.max(height,
                Resources.getSystem().getDimensionPixelSize(id));
    }

    /**
     * @return Activity status bar height, may be 0 if not affecting the activity.
     */
    @Dimension @Px
    @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    public int getNavigationBarHeight() {
        int height = 0;
        // Check consumed insets
        WindowInsetsCompat windowInsetsCompat = this.getWindowInsets();
        if (windowInsetsCompat != null) {
            // Note: isActivityWindowed does not affect layout
            Insets insets = windowInsetsCompat.getInsets(
                    WindowInsetsCompat.Type.navigationBars());
            height = Math.max(height, insets.bottom);
        }
        // Check system resources
        if (this.hasSoftwareNavBar()) {
            int id = Resources.getSystem().getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            Log.d(TAG, "Nav 2: " + id);
            return id <= 0 ? height : Math.max(height,
                    Resources.getSystem().getDimensionPixelSize(id));
        }
        return height;
    }

    public boolean hasHardwareNavBar() {
        // If onCreate has not been called yet, cached value is not valid
        return mOnCreateCalledOnce ? mHasHardwareNavBar : this.hasHardwareNavBar0();
    }

    public boolean hasSoftwareNavBar() {
        if (mFoxActivityView == null) return false;
        if (!this.hasHardwareNavBar()) return true;
        @SuppressLint("DiscouragedApi") int id = Resources.getSystem().getIdentifier(
                "config_showNavigationBar", "bool", "android");
        return (id > 0 && Resources.getSystem().getBoolean(id)) ||
                FoxLineage.getFoxLineage(this).isForceNavBar();
    }

    private boolean hasHardwareNavBar0() {
        return (ViewConfiguration.get(this).hasPermanentMenuKey() ||
                KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)) &&
                !"0".equals(SystemProperties.get("qemu.hw.mainkeys"));
    }

    public FoxNavigationMode getNavigationMode() {
        return mFoxNavigationMode != null ? mFoxNavigationMode :
                FoxNavigationMode.queryForActivity(this);
    }

    public void setActionBarExtraMenuButton(@DrawableRes int drawableResId,
                                            MenuItem.OnMenuItemClickListener menuClickListener) {
        this.setActionBarExtraMenuButton(drawableResId,
                menuClickListener, null);
    }

    public void setActionBarExtraMenuButton(@DrawableRes int drawableResId,
                                            MenuItem.OnMenuItemClickListener menuClickListener,
                                            @StringRes int menuContentDescription) {
        this.setActionBarExtraMenuButton(drawableResId,
                menuClickListener, this.getString(menuContentDescription));
    }

    @SuppressLint("RestrictedApi")
    public void setActionBarExtraMenuButton(@DrawableRes int drawableResId,
                                            MenuItem.OnMenuItemClickListener menuClickListener,
                                            CharSequence menuContentDescription) {
        Objects.requireNonNull(menuClickListener);
        mDrawableResId = drawableResId;
        mMenuClickListener = menuClickListener;
        mMenuContentDescription = menuContentDescription;
        if (mMenuItem != null) {
            mMenuItem.setOnMenuItemClickListener(mMenuClickListener);
            if (mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)mMenuItem)
                        .setContentDescription(mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMenuItem.setContentDescription(mMenuContentDescription);
            }
            mMenuItem.setIcon(mDrawableResId);
            mMenuItem.setEnabled(true);
            mMenuItem.setVisible(true);
        }
    }

    @SuppressLint("RestrictedApi")
    public void removeActionBarExtraMenuButton() {
        mDrawableResId = 0;
        mMenuClickListener = null;
        mMenuContentDescription = null;
        if (mMenuItem != null) {
            mMenuItem.setOnMenuItemClickListener(null);
            if (mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)mMenuItem)
                        .setContentDescription(mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMenuItem.setContentDescription(mMenuContentDescription);
            }
            mMenuItem.setIcon(null);
            mMenuItem.setEnabled(false);
            mMenuItem.setVisible(false);
        }
    }

    public void startActivity(Class<? extends Activity> activity) {
        this.startActivity(new Intent(this, activity));
    }

    public void startActivityWithAnimation(@NonNull Class<? extends Activity> activity,
                                           @NonNull Animation animation) {
        Bundle param = ActivityOptionsCompat.makeCustomAnimation(this,
                animation.inAnimLaunch, animation.outAnimLaunch).toBundle();
        Intent intent = new Intent(this, activity);
        intent.putExtra(EXTRA_FOX_FINISH_ANIMATION, animation.ordinal());
        if (animation == Animation.FADING) {
            intent.putExtra(EXTRA_FADE_OUT, true);
        }
        super.startActivity(intent, param);
    }

    public void startActivityWithAnimation(@NonNull Intent intent,
                                           @NonNull Animation animation) {
        intent = new Intent(intent);
        Bundle param = ActivityOptionsCompat.makeCustomAnimation(this,
                animation.inAnimLaunch, animation.outAnimLaunch).toBundle();
        intent.putExtra(EXTRA_FOX_FINISH_ANIMATION, animation.ordinal());
        if (animation == Animation.FADING) {
            intent.putExtra(EXTRA_FADE_OUT, true);
        }
        super.startActivity(intent, param);
    }

    // like setTheme but recreate the activity if needed
    public void setThemeRecreate(@StyleRes int resId) {
        if (!mOnCreateCalled) {
            this.setTheme(resId);
            return;
        }
        if (mSetThemeDynamic == resId)
            return;
        if (mSetThemeDynamic != 0)
            throw new IllegalStateException("setThemeDynamic called recursively");
        mSetThemeDynamic = resId;
        try {
            super.setTheme(resId);
        } finally {
            mSetThemeDynamic = 0;
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        if (resid != 0 && mSetThemeDynamic == resid) {
            Activity parent = this.getParent();
            (parent == null ? this : parent).recreate();
            super.overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
            return; // We are recreating ourself anyway, so ignore theming logic.
        }
        super.onApplyThemeResource(theme, resid, first);
        if (theme != null) {
            boolean isLightTheme = true;
            boolean useDynamicColors = mUseDynamicColors &&
                    FoxCompat.isDynamicAccentSupported(this);
            try {
                isLightTheme = FoxDisplay.isLightTheme(theme);
            } catch (Exception ignored) {
                useDynamicColors = false;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && useDynamicColors) {
                theme.applyStyle(isLightTheme ?
                        R.style.FoxCompat_Overrides_System :
                        FoxLineage.getFoxLineage(this).isBlackMode() ?
                                R.style.FoxCompat_Overrides_System_Black :
                                R.style.FoxCompat_Overrides_System_Dark, true);
            } else {
                theme.applyStyle(FoxCompat.isOxygenOS() ?
                        R.style.FoxCompat_Overrides_Base_Oxygen :
                        FoxCompat.isDynamicAccentSupported(this) ?
                        R.style.FoxCompat_Overrides_Base_System :
                        R.style.FoxCompat_Overrides_Base, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors) {
                    theme.applyStyle(isLightTheme ?
                            FoxCompatR.style.ThemeOverlay_Material3_DynamicColors_Light :
                            FoxCompatR.style.ThemeOverlay_Material3_DynamicColors_Dark, true);
                }
            }
            Application application = getApplicationIntent();
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application).onApplyFoxActivityThemeResource(this, theme, resid, first);
            }
            if (mFoxActivityView == null) {
                applyWindowThemeWorkarounds(theme);
            }
        }
    }

    /**
     * Fixes window bugs that require obscure or hard to find workarounds.
     */
    private static void applyWindowThemeWorkarounds(Resources.Theme theme) {
        if (theme == null) return;
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT &&
                typedValue.data == Color.TRANSPARENT) {
            Log.d(TAG, "Applying workaround for fully transparent windows!");
            theme.applyStyle(R.style.FoxCompat_Fixes_TransparentWindowBackgroundWorkaround, true);
        }
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT &&
                Color.alpha(typedValue.data) != 0xFF) {
            theme.resolveAttribute(android.R.attr.windowIsTranslucent, typedValue, true);
            if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN && typedValue.data != 0) {
                theme.resolveAttribute(android.R.attr.windowShowWallpaper, typedValue, true);
                if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN && typedValue.data != 0) {
                    return; // Skip transparency workaround.
                }
            }
            Log.d(TAG, "Applying wallpaper workaround for transparent windows!");
            theme.applyStyle(R.style.FoxCompat_Fixes_TranslucentWindowWallpaperWorkaround, true);
        }
    }

    public void setOnBackPressedRunnable(@Nullable Runnable onBackPressedCallback) {
        this.setOnBackPressedCallback(onBackPressedCallback != null ?
                new RunnableBackPressedListener(onBackPressedCallback) : null);
    }

    public void setOnBackPressedCallback(@Nullable OnBackPressedCallback onBackPressedCallback) {
        mFoxBackGestureHelper.setOnBackPressedCallback(onBackPressedCallback);
    }

    public OnBackPressedCallback getOnBackPressedCallback() {
        return mFoxBackGestureHelper.getOnBackPressedCallback();
    }

    public void linkWebViewToBackButton(@NonNull WebView webView) {
        this.setOnBackPressedCallback(new WebViewBackPressedListener(webView));
    }

    public void disableBackButton() {
        OnBackPressedCallback onBackPressedCallback =
                mFoxBackGestureHelper.getOnBackPressedCallback();
        if (onBackPressedCallback instanceof TrustedBackPressedListener) {
            ((TrustedBackPressedListener) onBackPressedCallback).setTrustedDisable(true);
        } else if (onBackPressedCallback != DISABLE_BACK_BUTTON) {
            this.setOnBackPressedCallback(DISABLE_BACK_BUTTON);
        }
    }

    public void enableBackButton() {
        OnBackPressedCallback onBackPressedCallback =
                mFoxBackGestureHelper.getOnBackPressedCallback();
        if (onBackPressedCallback instanceof TrustedBackPressedListener) {
            ((TrustedBackPressedListener) onBackPressedCallback).setTrustedDisable(false);
        } else if (onBackPressedCallback == DISABLE_BACK_BUTTON) {
            this.setOnBackPressedCallback(null);
        }
    }

    public Openable getDrawerOpenable() {
        return mDrawerOpenable;
    }

    public void setDrawerOpenable(Openable drawerOpenable) {
        mDrawerOpenable = drawerOpenable;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Openable drawerOpenable = mDrawerOpenable;
            if (drawerOpenable != null) {
                if (drawerOpenable.isOpen()) {
                    drawerOpenable.close();
                } else {
                    mFoxBackGestureHelper.applyDrawerTimeout();
                    drawerOpenable.open();
                }
                return true;
            }
            androidx.appcompat.app.ActionBar compatActionBar;
            try {
                compatActionBar = this.getSupportActionBar();
            } catch (Exception e) {
                Log.e(TAG, "Failed to call getSupportActionBar", e);
                compatActionBar = null; // Allow fallback to builtin actionBar.
            }
            ActionBar actionBar =
                    compatActionBar == null ? this.getSupportActionBar() : null;
            if (compatActionBar != null ? (compatActionBar.getDisplayOptions() &
                    androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP) != 0 :
                    actionBar != null && (actionBar.getDisplayOptions() &
                            android.app.ActionBar.DISPLAY_HOME_AS_UP) != 0) {
                this.onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.compat_menu, menu);
        mMenuItem = menu.findItem(R.id.compat_menu_item);
        if (mMenuClickListener != null) {
            mMenuItem.setOnMenuItemClickListener(mMenuClickListener);
            if (mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)mMenuItem)
                        .setContentDescription(mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMenuItem.setContentDescription(mMenuContentDescription);
            }
            mMenuItem.setIcon(mDrawableResId);
            mMenuItem.setEnabled(true);
            mMenuItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("RestrictedApi")
    public boolean clickMenu() {
        if (mMenuClickListener != null) {
            MenuItem menuItem = mMenuItem;
            if (menuItem == null) {
                menuItem = new ActionMenuItem(this, 0, 0, 0, 0, null);
                menuItem.setOnMenuItemClickListener(mMenuClickListener);
            }
            return mMenuClickListener.onMenuItemClick(menuItem);
        }
        return false;
    }

    public void startActivityForResult(Intent intent,
                                       OnActivityResultCallback onActivityResultCallback) {
        this.startActivityForResult(intent, null, onActivityResultCallback);
    }

    public void startActivityForResult(Intent intent, @Nullable Bundle options,
                                       OnActivityResultCallback onActivityResultCallback) {
        super.startActivityForResult(intent, INTENT_ACTIVITY_REQUEST_CODE, options);
        mOnActivityResultCallback = onActivityResultCallback;
    }

    @Override
    @CallSuper
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == INTENT_ACTIVITY_REQUEST_CODE) {
            OnActivityResultCallback callback = mOnActivityResultCallback;
            if (callback != null) {
                mOnActivityResultCallback = null;
                callback.onActivityResult(resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean isLightTheme() {
        return FoxDisplay.isLightThemeSafe(this.getTheme());
    }

    public String resolveId(@IdRes int id) {
        return FoxDisplay.resolveId(this, id);
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

    /**
     * Note: This value can change at runtime on some devices,
     * and return true if DisplayCutout is simulated.
     * */
    public boolean hasNotch() {
        if (!mOnCreateCalledOnce) {
            Log.w(TAG, "hasNotch() called before onCreate()");
            return FoxNotch.getNotchHeight(this) != 0;
        }
        return mDisplayCutoutHeight != 0;
    }

    @SuppressWarnings("deprecation")
    @Nullable @Override
    public Display getDisplay() {
        if (mFoxActivityView != null) {
            return FoxDisplay.getDisplay(
                    mFoxActivityView.getParentFoxActivity());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return super.getDisplay();
        }
        return this.getWindowManager().getDefaultDisplay();
    }

    @Rotation
    public int getRotation() {
        Display display = this.getDisplay();
        return display != null ? display.getRotation() :
                this.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE ?
                        Surface.ROTATION_90 : Surface.ROTATION_0;
    }

    public static FoxActivity getFoxActivity(View view) {
        return getFoxActivity(view.getContext());
    }

    public static FoxActivity getFoxActivity(Fragment fragment) {
        return getFoxActivity(fragment.getContext());
    }

    public static FoxActivity getFoxActivity(Context context) {
        while (!(context instanceof FoxActivity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else return null;
        }
        return (FoxActivity) context;
    }

    public WeakReference<FoxActivity> asWeakReference() {
        return mSelfReference;
    }

    @Override
    protected Intent patchIntent(Intent intent) {
        Application application = this.getApplicationIntent();
        if (application instanceof ApplicationCallbacks) {
            intent = ((ApplicationCallbacks) application)
                    .patchIntent(this, intent);
        }
        return super.patchIntent(intent);
    }

    @Override
    public Window getWindow() {
        if (mShouldNullifyWindow) return null;
        if (mFoxActivityView != null)
            return mFoxActivityView.getParentFoxActivity().getWindow();
        return super.getWindow();
    }

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        if (mFoxActivityView != null) return null;
        return super.getSupportActionBar();
    }

    @Override
    public WindowManager getWindowManager() {
        if (mFoxActivityView != null)
            return mFoxActivityView.getParentFoxActivity().getWindowManager();
        return super.getWindowManager();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (mFoxActivityView != null) {
            return mFoxActivityView.mViewModelStore;
        }
        return super.getViewModelStore();
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mFoxActivityView != null) {
            if (mFoxActivityView.mViewModelProviderFactory == null) {
                mFoxActivityView.mViewModelProviderFactory = new ViewModelProvider.AndroidViewModelFactory(getApplicationIntent());
            }
            return mFoxActivityView.mViewModelProviderFactory;
        }
        return super.getDefaultViewModelProviderFactory();
    }

    @Override
    public android.app.FragmentManager getFragmentManager() {
        if (mFoxActivityView != null) {
            return mFoxActivityView.getParentFoxActivity().getFragmentManager();
        }
        return super.getFragmentManager();
    }

    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        if (this instanceof ExternallyEmbeddable)
            throw new IllegalStateException(
                    "ExternallyEmbeddable activities can't use getSupportFragmentManager");
        if (mFoxActivityView != null) {
            return mFoxActivityView.mFragmentManager;
        }
        return super.getSupportFragmentManager();
    }

    @Override
    protected Activity getIntentReceiver() {
        if (mFoxActivityView != null)
            return mFoxActivityView.getParentFoxActivity();
        return null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mFoxActivityView != null) return false;
        return super.dispatchKeyEvent(event);
    }

    public boolean isEmbedded() {
        return mFoxActivityView != null;
    }

    public Activity getContainerActivity() {
        return mFoxActivityView == null ? null :
                mFoxActivityView.getParentFoxActivity();
    }

    public boolean hasHiddenApis() {
        return FoxCompat.getHiddenApiStatus(this);
    }

    public final FoxLineage getLineage() {
        return FoxLineage.getFoxLineage(this.getApplicationIntent());
    }

    public enum Animation {
        FADING(android.R.anim.fade_in, android.R.anim.fade_out),
        SLIDE_RIGHT(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right),
        SLIDE_LEFT(R.anim.slide_in_left, R.anim.slide_out_right,
                R.anim.slide_in_right, R.anim.slide_out_left);

        @AnimRes public final int inAnimLaunch, outAnimLaunch;
        @AnimRes public final int inAnimFinish, outAnimFinish;

        Animation(int inAnim, int outAnim) {
            this.inAnimLaunch = inAnim;
            this.outAnimLaunch = outAnim;
            this.inAnimFinish = inAnim;
            this.outAnimFinish = outAnim;
        }

        Animation(int inAnimLaunch, int outAnimLaunch, int inAnimFinish, int outAnimFinish) {
            this.inAnimLaunch = inAnimLaunch;
            this.outAnimLaunch = outAnimLaunch;
            this.inAnimFinish = inAnimFinish;
            this.outAnimFinish = outAnimFinish;
        }
    }

    @FunctionalInterface
    public interface OnActivityResultCallback {
        void onActivityResult(int resultCode, @Nullable Intent data);
    }

    @FunctionalInterface
    public interface OnBackPressedCallback {
        boolean onBackPressed(FoxActivity compatActivity);
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private FoxAppTask getAppTaskImpl(int taskId, boolean allowFallback) {
        // Don't cache AppTask if wrapped
        boolean wrapped = mFoxActivityView != null;
        FoxAppTask appTask = wrapped ? null : mAppTask;
        if (appTask != null) return appTask;
        appTask = FoxAppTask.getFoxAppTaskImpl(null, this, taskId,
                allowFallback ? this.getComponentName() : null);
        if (appTask != null && !wrapped) {
            mAppTask = appTask;
        }
        return appTask;
    }

    @Override
    public int getTaskId() {
        if (mFoxActivityView != null) {
            return mFoxActivityView.getParentFoxActivity().getTaskId();
        }
        return super.getTaskId();
    }

    @Nullable
    public FoxAppTask getAppTask() {
        if (this.isDestroyed() || this instanceof Embeddable) return null;
        return this.getAppTaskImpl(super.getTaskId(), true);
    }

    @Nullable
    public ActivityManager.RecentTaskInfo getTaskInfo() {
        FoxAppTask appTask = this.getAppTask();
        return appTask == null ? null : appTask.getTaskInfo();
    }

    public void setExcludeFromRecents(boolean exclude) {
        if (this.isDestroyed() || mFoxActivityView != null) return;
        FoxAppTask foxAppTask = this.getAppTaskImpl(this.getTaskId(), true);
        if (foxAppTask != null) foxAppTask.setExcludeFromRecents(exclude);
    }

    public interface Embeddable {}

    public interface ExternallyEmbeddable extends Embeddable {}

    public interface ApplicationCallbacks {
        Intent patchIntent(FoxActivity activity, Intent intent);

        void onCreateFoxActivity(FoxActivity activity);

        void onCreateEmbeddedFoxActivity(Activity root, FoxActivity embedded);

        void onRefreshUI(FoxActivity activity);

        void onApplyFoxActivityThemeResource(
                FoxActivity activity, Resources.Theme theme, int resid, boolean first);
    }

    static abstract class TrustedBackPressedListener implements FoxActivity.OnBackPressedCallback {
        boolean mDisable;

        void setTrustedDisable(boolean disable) {
            mDisable = disable;
        }
    }

    static final class WebViewBackPressedListener extends TrustedBackPressedListener {
        private final WeakReference<WebView> reference;

        public WebViewBackPressedListener(WebView webView) {
            this.reference = new WeakReference<>(webView);
        }

        @Override
        public boolean onBackPressed(FoxActivity compatActivity) {
            if (this.mDisable) {
                compatActivity.setOnBackPressedCallback(this);
                return true;
            }
            WebView webView = this.reference.get();
            if (webView == null) return false;
            compatActivity.setOnBackPressedCallback(this);
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            return false;
        }
    }

    static final class RunnableBackPressedListener extends TrustedBackPressedListener {
        private final Runnable runnable;

        RunnableBackPressedListener(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public boolean onBackPressed(FoxActivity compatActivity) {
            compatActivity.setOnBackPressedCallback(this);
            if (!this.mDisable) this.runnable.run();
            return true;
        }
    }

    @IntDef(open = true, value = {
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Rotation {}
}
