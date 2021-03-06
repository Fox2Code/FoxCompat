package com.fox2code.foxcompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ContentCaptureOptions;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.widget.Openable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.fox2code.foxcompat.internal.FoxCompat;
import com.fox2code.foxcompat.internal.FoxNotch;
import com.fox2code.foxcompat.internal.FoxIntentActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Objects;

import rikka.core.res.ResourcesCompatLayoutInflaterListener;
import rikka.insets.WindowInsetsHelper;
import rikka.layoutinflater.view.LayoutInflaterFactory;

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
    LayoutInflaterFactory mLayoutInflaterFactory;
    private FoxStatusBarManager mFoxStatusBarManager;
    final WeakReference<FoxActivity> mSelfReference;
    private OnActivityResultCallback mOnActivityResultCallback;
    private OnBackPressedCallback mOnBackPressedCallback;
    private long mOnBackPressedTimeout; // Avoid accidental presses
    private long mOnBackPressedTimeoutDrawer;
    private MenuItem.OnMenuItemClickListener mMenuClickListener;
    private CharSequence mMenuContentDescription;
    private FoxActivityView mFoxActivityView;
    private boolean mShouldNullifyWindow;
    private int mDisplayCutoutHeight = 0;
    @Rotation private int mCachedRotation = 0;
    @StyleRes private int mSetThemeDynamic = 0;
    private boolean mAwaitOnWindowUpdate = false;
    private boolean mOnCreateCalledOnce = false;
    private boolean mOnCreateCalled = false;
    private boolean mIsRefreshUi = false;
    private boolean mHasHardwareNavBar;
    private Drawable mActionBarBackground;
    private Boolean mActionBarHomeAsUp;
    private Boolean mActionBarVisible;
    private Openable mDrawerOpenable;
    private int mDrawableResId;
    private MenuItem mMenuItem;
    boolean mUseDynamicColors;

    public FoxActivity() {
        this.mSelfReference = new WeakReference<>(this);
    }

    void attachFoxActivityView(FoxActivityView foxActivityView) {
        if (!(this instanceof Embeddable))
            throw new IllegalStateException("Activity is not embeddable!");
        if (foxActivityView.mFoxActivity != null)
            throw new IllegalStateException("FoxActivityView already bound");
        Context applicationContext = foxActivityView.getApplicationContext();
        super.attachBaseContext(applicationContext == null ?
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
        this.mFoxActivityView = foxActivityView;
        this.mOnCreateCalledOnce = true;
    }

    public void setUseDynamicColors(boolean useDynamicColors) {
        this.mUseDynamicColors = useDynamicColors;
    }

    public boolean useDynamicColors() {
        return this.mUseDynamicColors;
    }

    @Override
    public void setContentView(int layoutResID) {
        if (this.mFoxActivityView != null) {
            this.mFoxActivityView.removeAllViewsInLayout();
            LayoutInflater.from(this).inflate(
                    layoutResID, this.mFoxActivityView.mRealFrameLayout);
        } else super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        if (this.mFoxActivityView != null) {
            this.mFoxActivityView.mRealFrameLayout.removeAllViews();
            this.mFoxActivityView.mRealFrameLayout.addView(view);
        } else super.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (this.mFoxActivityView != null) {
            this.mFoxActivityView.mRealFrameLayout.removeAllViews();
            this.mFoxActivityView.mRealFrameLayout.addView(view, params);
        } else super.setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (this.mFoxActivityView != null) {
            this.mFoxActivityView.mRealFrameLayout.addView(view, params);
        } else super.addContentView(view, params);
    }

    // Post Windows API
    void postWindowUpdated() {
        if (this.mAwaitOnWindowUpdate) return;
        this.mAwaitOnWindowUpdate = true;
        handler.post(() -> {
            this.mAwaitOnWindowUpdate = false;
            if (this.isFinishing()) return;
            this.mCachedRotation = this.getRotation();
            this.mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
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
        if (!this.mOnCreateCalledOnce) {
            this.getLayoutInflater().setFactory2(this.mLayoutInflaterFactory =
                    (this instanceof Embeddable ? this.createLayoutInflaterFactory0() :
                            this.createLayoutInflaterFactory()));
            this.mHasHardwareNavBar = this.hasHardwareNavBar0();
            this.mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            this.mCachedRotation = this.getRotation();
            this.mOnCreateCalledOnce = true;
        }
        Resources.Theme theme = this.getTheme();
        boolean isLightTheme = FoxDisplay.isLightTheme(theme);
        boolean useDynamicColors = this.mUseDynamicColors &&
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
                        com.google.android.material.R.style.
                                ThemeOverlay_Material3_DynamicColors_Light :
                        com.google.android.material.R.style.
                                ThemeOverlay_Material3_DynamicColors_Dark, true);
            }
        }
        Application application = this.getApplicationIntent();
        if (this.mFoxActivityView == null) {
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application).onCreateFoxActivity(this);
            }
            this.mShouldNullifyWindow = false;
            super.onCreate(savedInstanceState);
        } else {
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application).onCreateEmbeddedFoxActivity(
                        this.mFoxActivityView.getParentFoxActivity(), this);
            }
            this.mShouldNullifyWindow = true;
            try {
                super.onCreate(savedInstanceState);
            } catch (NullPointerException ignored) {} finally {
                this.mShouldNullifyWindow = false;
            }
        }
        this.mOnCreateCalled = true;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.mOnCreateCalledOnce) {
            this.resetOnBackPressedTimeOut();
        }
    }

    final LayoutInflaterFactory createLayoutInflaterFactory0() {
        LayoutInflaterFactory layoutInflaterFactory =
                new LayoutInflaterFactory(this.getDelegate())
                        .addOnViewCreatedListener(WindowInsetsHelper.Companion.getLISTENER())
                        .addOnViewCreatedListener(FoxCompat.TOOLBAR_ALIGNMENT_FIX);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && FoxCompat.cardView) {
            layoutInflaterFactory.addOnViewCreatedListener(FoxCompat.CARD_VIEW_COLOR_FIX);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && FoxCompat.rikkaXCore) {
            layoutInflaterFactory.addOnViewCreatedListener(
                    ResourcesCompatLayoutInflaterListener.getInstance());
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !BuildCompat.isAtLeastS()
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
        this.mHasHardwareNavBar = this.hasHardwareNavBar0();
        if (this.mFoxActivityView == null) {
            super.onResume();
            this.resetOnBackPressedTimeOut();
        }
        this.refreshUI();
    }

    @Override
    protected void onPause() {
        if (this.mFoxActivityView == null) {
            super.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.mFoxActivityView == null) {
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
        if (this.mFoxStatusBarManager == null) {
            this.mFoxStatusBarManager = FoxStatusBarManager.createForActivity(this);
        }
        return this.mFoxStatusBarManager;
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
        if (this.mCachedRotation != this.getRotation() &&
                this.mOnCreateCalledOnce && !this.mAwaitOnWindowUpdate) {
            this.mCachedRotation = this.getRotation();
            this.mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            this.onWindowUpdated();
        }
    }

    @Override @CallSuper @RequiresApi(Build.VERSION_CODES.N)
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        this.postWindowUpdated();
    }

    @Override
    public void finish() {
        this.mOnActivityResultCallback = null;
        Animation finishAnimation = null;
        if (this.mOnCreateCalled) {
            if (this.getIntent().getBooleanExtra(EXTRA_FADE_OUT, false)) {
                finishAnimation = Animation.FADING;
            }
            int index = this.getIntent().getIntExtra(EXTRA_FOX_FINISH_ANIMATION, -1);
            try {
                finishAnimation = Animation.values()[index];
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
        if (this.mFoxActivityView != null) {
            this.mFoxActivityView.recreate();
        } else {
            super.recreate();
        }
    }

    @CallSuper
    public void refreshUI() {
        // Avoid recursive calls
        if (this.mIsRefreshUi || !this.mOnCreateCalled) return;
        this.mIsRefreshUi = true;
        try {
            this.mCachedRotation = this.getRotation();
            this.mDisplayCutoutHeight = FoxNotch.getNotchHeight(this);
            Application application = this.getApplication();
            if (application instanceof ApplicationCallbacks) {
                ((ApplicationCallbacks) application)
                        .onRefreshUI(this);
            }
            this.postWindowUpdated();
        } finally {
            this.mIsRefreshUi = false;
        }
    }

    private void resetOnBackPressedTimeOut() {
        this.mOnBackPressedTimeout = System.currentTimeMillis() + 250;
    }

    public final void forceBackPressed() {
        if (this.mFoxActivityView == null && !this.isFinishing()) {
            this.mOnBackPressedTimeoutDrawer = 0;
            this.mOnBackPressedTimeout = 0;
            super.onBackPressed();
        }
    }

    public final void pressBack() {
        if (this.mFoxActivityView == null && !this.isFinishing()) {
            this.mOnBackPressedTimeoutDrawer = 0;
            this.mOnBackPressedTimeout = 0;
            this.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mOnBackPressedTimeout > System.currentTimeMillis()
                || this.isFinishing()) return;
        Openable openableDrawer = this.mDrawerOpenable;
        if (openableDrawer != null) {
            if (openableDrawer.isOpen()) {
                this.resetOnBackPressedTimeOut();
                openableDrawer.close();
                return;
            } else if (this.mOnBackPressedTimeoutDrawer >
                    System.currentTimeMillis()) {
                openableDrawer.close(); // Make app more responsive
                return; // In case opening animation is still playing.
            }
        }
        OnBackPressedCallback onBackPressedCallback = this.mOnBackPressedCallback;
        this.mOnBackPressedCallback = null;
        if (onBackPressedCallback == null ||
                !onBackPressedCallback.onBackPressed(this)) {
            if (this.mFoxActivityView == null)
                super.onBackPressed();
        }
    }

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        if (this.mFoxActivityView != null) return;
        this.mActionBarHomeAsUp = showHomeAsUp;
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
            android.app.ActionBar actionBar = this.getActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
        }
    }

    public void hideActionBar() {
        this.mActionBarVisible = false;
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
            android.app.ActionBar actionBar = this.getActionBar();
            if (actionBar != null)
                actionBar.hide();
        }
    }

    public void showActionBar() {
        this.mActionBarVisible = true;
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
            android.app.ActionBar actionBar = this.getActionBar();
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
            android.app.ActionBar actionBar = this.getActionBar();
            return actionBar != null ? actionBar.getCustomView() : null;
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            if (this.mActionBarVisible != null &&
                    this.mActionBarVisible != actionBar.isShowing()) {
                this.mActionBarVisible = null;
            }
        }
        this.mMenuItem = null;
        super.setSupportActionBar(toolbar);
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            if (this.mActionBarBackground != null) {
                actionBar.setBackgroundDrawable(this.mActionBarBackground);
            }
            if (this.mActionBarHomeAsUp != null) {
                actionBar.setDisplayHomeAsUpEnabled(this.mActionBarHomeAsUp);
            }
            if (this.mActionBarVisible != null) {
                if (this.mActionBarVisible) {
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
            android.app.ActionBar actionBar = this.getActionBar();
            return actionBar != null && (actionBar.isShowing() || ((
                    customView = actionBar.getCustomView()) != null &&
                    customView.getVisibility() == View.VISIBLE)) ?
                    Math.max(customView == null ? 0 : customView.getHeight(),
                            actionBar.getHeight()) : 0;
        }
    }

    public void setActionBarBackground(Drawable drawable) {
        if (this.mFoxActivityView != null) return;
        if (drawable == null) drawable = FoxViewCompat.NULL_DRAWABLE;
        this.mActionBarBackground = drawable;
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
            android.app.ActionBar actionBar = this.getActionBar();
            if (actionBar != null)
                actionBar.setBackgroundDrawable(drawable);
        }
    }

    public boolean isActivityWindowed() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (super.isInMultiWindowMode() || super.isInPictureInPictureMode())) ||
                this.mFoxActivityView != null; // If Activity is in foxActivityView assume windowed
    }

    public ViewGroup getContentView() {
        if (this.mFoxActivityView != null)
            return this.mFoxActivityView.mRealFrameLayout;
        return findViewById(android.R.id.content);
    }

    @Nullable
    public WindowInsetsCompat getWindowInsets() {
        if (this.mFoxActivityView != null) return null;
        View view = this.getContentView();
        return view != null ? ViewCompat.getRootWindowInsets(view) : null;
    }

    /**
     * @return Activity status bar height, may be 0 if not affecting the activity.
     */
    @Dimension @Px
    @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    public int getStatusBarHeight() {
        if (this.mFoxActivityView != null) return 0;
        // Check display cutout height
        int height = this.getRotation() == 0 ?
                this.mDisplayCutoutHeight : 0;
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
        return this.mOnCreateCalledOnce ? this.mHasHardwareNavBar : this.hasHardwareNavBar0();
    }

    public boolean hasSoftwareNavBar() {
        if (this.mFoxActivityView == null) return false;
        if (!this.hasHardwareNavBar()) return true;
        int id = Resources.getSystem().getIdentifier(
                "config_showNavigationBar", "bool", "android");
        return (id > 0 && Resources.getSystem().getBoolean(id)) ||
                FoxLineage.getFoxLineage(this).isForceNavBar();
    }

    private boolean hasHardwareNavBar0() {
        return (ViewConfiguration.get(this).hasPermanentMenuKey() ||
                KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)) &&
                !"0".equals(SystemProperties.get("qemu.hw.mainkeys"));
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
        this.mDrawableResId = drawableResId;
        this.mMenuClickListener = menuClickListener;
        this.mMenuContentDescription = menuContentDescription;
        if (this.mMenuItem != null) {
            this.mMenuItem.setOnMenuItemClickListener(this.mMenuClickListener);
            if (this.mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)this.mMenuItem)
                        .setContentDescription(this.mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mMenuItem.setContentDescription(this.mMenuContentDescription);
            }
            this.mMenuItem.setIcon(this.mDrawableResId);
            this.mMenuItem.setEnabled(true);
            this.mMenuItem.setVisible(true);
        }
    }

    @SuppressLint("RestrictedApi")
    public void removeActionBarExtraMenuButton() {
        this.mDrawableResId = 0;
        this.mMenuClickListener = null;
        this.mMenuContentDescription = null;
        if (this.mMenuItem != null) {
            this.mMenuItem.setOnMenuItemClickListener(null);
            if (this.mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)this.mMenuItem)
                        .setContentDescription(this.mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mMenuItem.setContentDescription(this.mMenuContentDescription);
            }
            this.mMenuItem.setIcon(null);
            this.mMenuItem.setEnabled(false);
            this.mMenuItem.setVisible(false);
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
        if (!this.mOnCreateCalled) {
            this.setTheme(resId);
            return;
        }
        if (this.mSetThemeDynamic == resId)
            return;
        if (this.mSetThemeDynamic != 0)
            throw new IllegalStateException("setThemeDynamic called recursively");
        this.mSetThemeDynamic = resId;
        try {
            super.setTheme(resId);
        } finally {
            this.mSetThemeDynamic = 0;
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        if (resid != 0 && this.mSetThemeDynamic == resid) {
            super.onApplyThemeResource(theme, resid, first);
            Activity parent = this.getParent();
            (parent == null ? this : parent).recreate();
            super.overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            super.onApplyThemeResource(theme, resid, first);
        }
        if (theme != null) {
            boolean isLightTheme = FoxDisplay.isLightTheme(theme);
            boolean useDynamicColors = this.mUseDynamicColors &&
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
                            com.google.android.material.R.style.
                                    ThemeOverlay_Material3_DynamicColors_Light :
                            com.google.android.material.R.style.
                                    ThemeOverlay_Material3_DynamicColors_Dark, true);
                }
            }
        }
    }

    public void setOnBackPressedRunnable(@Nullable Runnable onBackPressedCallback) {
        this.setOnBackPressedCallback((OnBackPressedCallback) (onBackPressedCallback != null ?
                new RunnableBackPressedListener(onBackPressedCallback) : null));
    }

    public void setOnBackPressedCallback(@Nullable OnBackPressedCallback onBackPressedCallback) {
        this.mOnBackPressedCallback = onBackPressedCallback;
    }

    public OnBackPressedCallback getOnBackPressedCallback() {
        return this.mOnBackPressedCallback;
    }

    public void linkWebViewToBackButton(@NonNull WebView webView) {
        this.setOnBackPressedCallback(new WebViewBackPressedListener(webView));
    }

    public void disableBackButton() {
        if (this.mOnBackPressedCallback instanceof TrustedBackPressedListener) {
            ((TrustedBackPressedListener) this.mOnBackPressedCallback).disable = true;
        } else if (this.mOnBackPressedCallback != DISABLE_BACK_BUTTON) {
            this.setOnBackPressedCallback(DISABLE_BACK_BUTTON);
        }
    }

    public void enableBackButton() {
        if (this.mOnBackPressedCallback instanceof TrustedBackPressedListener) {
            ((TrustedBackPressedListener) this.mOnBackPressedCallback).disable = false;
        } else if (this.mOnBackPressedCallback == DISABLE_BACK_BUTTON) {
            this.setOnBackPressedCallback((OnBackPressedCallback) null);
        }
    }

    public void setDrawerOpenable(Openable drawerOpenable) {
        this.mDrawerOpenable = drawerOpenable;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Openable drawerOpenable = this.mDrawerOpenable;
            if (drawerOpenable != null) {
                if (drawerOpenable.isOpen()) {
                    drawerOpenable.close();
                } else {
                    this.mOnBackPressedTimeoutDrawer =
                            System.currentTimeMillis() + 450;
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
            android.app.ActionBar actionBar = this.getActionBar();
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
        this.mMenuItem = menu.findItem(R.id.compat_menu_item);
        if (this.mMenuClickListener != null) {
            this.mMenuItem.setOnMenuItemClickListener(this.mMenuClickListener);
            if (this.mMenuItem instanceof SupportMenuItem) {
                ((SupportMenuItem)this.mMenuItem)
                        .setContentDescription(this.mMenuContentDescription);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mMenuItem.setContentDescription(this.mMenuContentDescription);
            }
            this.mMenuItem.setIcon(this.mDrawableResId);
            this.mMenuItem.setEnabled(true);
            this.mMenuItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("RestrictedApi")
    public boolean clickMenu() {
        if (this.mMenuClickListener != null) {
            MenuItem menuItem = this.mMenuItem;
            if (menuItem == null) {
                menuItem = new ActionMenuItem(this, 0, 0, 0, 0, null);
                menuItem.setOnMenuItemClickListener(this.mMenuClickListener);
            }
            return this.mMenuClickListener.onMenuItemClick(menuItem);
        }
        return false;
    }

    public void startActivityForResult(Intent intent,
                                       OnActivityResultCallback onActivityResultCallback) {
        this.startActivityForResult(intent, null, onActivityResultCallback);
    }

    @SuppressWarnings("deprecation")
    public void startActivityForResult(Intent intent, @Nullable Bundle options,
                                       OnActivityResultCallback onActivityResultCallback) {
        super.startActivityForResult(intent, INTENT_ACTIVITY_REQUEST_CODE, options);
        this.mOnActivityResultCallback = onActivityResultCallback;
    }

    @Override
    @CallSuper
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == INTENT_ACTIVITY_REQUEST_CODE) {
            OnActivityResultCallback callback = this.mOnActivityResultCallback;
            if (callback != null) {
                this.mOnActivityResultCallback = null;
                callback.onActivityResult(resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean isLightTheme() {
        return FoxDisplay.isLightTheme(this.getTheme());
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
        if (!this.mOnCreateCalledOnce) {
            Log.w(TAG, "hasNotch() called before onCreate()");
            return FoxNotch.getNotchHeight(this) != 0;
        }
        return this.mDisplayCutoutHeight != 0;
    }

    @SuppressWarnings("deprecation")
    @Nullable @Override
    public Display getDisplay() {
        if (this.mFoxActivityView != null) {
            return FoxDisplay.getDisplay(
                    this.mFoxActivityView.getParentFoxActivity());
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
        return this.mSelfReference;
    }

    @Override
    public Intent patchIntent(Intent intent) {
        Application application = this.getApplicationIntent();
        if (application instanceof ApplicationCallbacks) {
            intent = ((ApplicationCallbacks) application)
                    .patchIntent(this, intent);
        }
        return super.patchIntent(intent);
    }

    @Override
    public Window getWindow() {
        if (this.mShouldNullifyWindow) return null;
        if (this.mFoxActivityView != null)
            return this.mFoxActivityView.getParentFoxActivity().getWindow();
        return super.getWindow();
    }

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        if (this.mFoxActivityView != null) return null;
        return super.getSupportActionBar();
    }

    @Override
    public WindowManager getWindowManager() {
        if (this.mFoxActivityView != null)
            return this.mFoxActivityView.getParentFoxActivity().getWindowManager();
        return super.getWindowManager();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (this.mFoxActivityView != null) {
            return this.mFoxActivityView.mViewModelStore;
        }
        return super.getViewModelStore();
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (this.mFoxActivityView != null) {
            if (this.mFoxActivityView.mViewModelProviderFactory == null) {
                this.mFoxActivityView.mViewModelProviderFactory =
                        new SavedStateViewModelFactory(getApplicationIntent(),
                        this, getIntent() != null ? getIntent().getExtras() : null);
            }
            return this.mFoxActivityView.mViewModelProviderFactory;
        }
        return super.getDefaultViewModelProviderFactory();
    }

    @Override
    public android.app.FragmentManager getFragmentManager() {
        if (this.mFoxActivityView != null) {
            return this.mFoxActivityView.getParentFoxActivity().getFragmentManager();
        }
        return super.getFragmentManager();
    }

    @NonNull
    @Override
    public FragmentManager getSupportFragmentManager() {
        if (this instanceof ExternallyEmbeddable)
            throw new IllegalStateException(
                    "ExternallyEmbeddable activities can't use getSupportFragmentManager");
        if (this.mFoxActivityView != null) {
            return this.mFoxActivityView.mFragmentManager;
        }
        return super.getSupportFragmentManager();
    }

    @Override
    protected Activity getIntentReceiver() {
        if (this.mFoxActivityView != null)
            return this.mFoxActivityView.getParentFoxActivity();
        return null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mFoxActivityView != null) return false;
        return super.dispatchKeyEvent(event);
    }

    public boolean isEmbedded() {
        return this.mFoxActivityView != null;
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

    public interface Embeddable {}

    public interface ExternallyEmbeddable extends Embeddable {}

    public interface ApplicationCallbacks {
        void onCreateFoxActivity(FoxActivity activity);

        void onCreateEmbeddedFoxActivity(Activity root, FoxActivity embedded);

        void onRefreshUI(FoxActivity activity);

        Intent patchIntent(FoxActivity activity, Intent intent);
    }

    static abstract class TrustedBackPressedListener implements OnBackPressedCallback {
        boolean disable;
    }

    static final class WebViewBackPressedListener extends TrustedBackPressedListener {
        private final WeakReference<WebView> reference;

        public WebViewBackPressedListener(WebView webView) {
            this.reference = new WeakReference<>(webView);
        }

        @Override
        public boolean onBackPressed(FoxActivity compatActivity) {
            if (this.disable) {
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
            if (!this.disable) this.runnable.run();
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
