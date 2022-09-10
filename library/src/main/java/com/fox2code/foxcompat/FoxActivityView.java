package com.fox2code.foxcompat;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

import com.fox2code.foxcompat.internal.FoxProcessExt;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * View that allow to include a FoxActivity
 */
public final class FoxActivityView extends FrameLayout implements SavedStateRegistryOwner {
    private static final String CLASS_NAME1 =
            "com.fox2code.foxcompat.FoxActivityView";
    private static final String CLASS_NAME2 =
            "com.fox2code.foxcompat.app.FoxActivityView";
    private static final String CLASS_NAME3 =
            "com.fox2code.foxcompat.widget.FoxActivityView";

    private static final String TAG = "FoxActivityView";
    @NonNull
    final FrameLayout mRealFrameLayout;
    final Context mApplicationContext;
    private Bundle mSavedInstanceState;
    ViewModelProvider.Factory
            mViewModelProviderFactory;
    FragmentManager mFragmentManager;
    ViewModelStore mViewModelStore;
    private String mLoadActivity;
    FoxActivity mFoxActivity;
    private boolean mExternal;
    private boolean mPaused;

    public FoxActivityView(@NonNull Context context) {
        super(context);
        mRealFrameLayout = this;
        mApplicationContext = null;
        this.init(null, 0, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mRealFrameLayout = this;
        mApplicationContext = null;
        this.init(attrs, 0, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRealFrameLayout = this;
        mApplicationContext = null;
        this.init(attrs, defStyleAttr, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mRealFrameLayout = this;
        mApplicationContext = null;
        this.init(attrs, defStyleAttr, null);
    }

    // Constructor for FoxActivityView cross process support V1.
    // If you wish to edit this constructor, create a new one instead to preserve compatibility.
    @Keep @RestrictTo(RestrictTo.Scope.LIBRARY)
    public FoxActivityView(@NonNull Context context,@NonNull Context applicationContext,
                           @Nullable FrameLayout realFrameLayout, @Nullable String activity) {
        super(context); mExternal = true;
        mRealFrameLayout = realFrameLayout == null ? this : realFrameLayout;
        mApplicationContext = Objects.requireNonNull(applicationContext);
        this.init(null, 0, activity);
        if (FoxProcessExt.getApplication(applicationContext.getPackageName()) == null
                && applicationContext instanceof Application) {
            FoxProcessExt.putApplication((Application) applicationContext);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private FoxActivityView(@NonNull Context context,@Nullable
            Application application, @Nullable String activity) {
        super(context);
        mRealFrameLayout = this;
        mApplicationContext = application;
        this.init(null, 0, activity);
    }

    private void init(AttributeSet attrs, int defStyleAttr, String activity) {
        FragmentActivity parent = this.getParentFoxActivityInternal();
        if (parent != null) {
            mFragmentManager = parent.getSupportFragmentManager();
            mViewModelStore = parent.getViewModelStore();
            mExternal = mFragmentManager == null;
        } else mExternal = true;
        if (activity == null && mRealFrameLayout == this) {
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.FoxActivityView, defStyleAttr, 0);
            activity = a.getString(R.styleable.FoxActivityView_activity);
            a.recycle();
        }
        if (activity != null && !activity.isEmpty()) {
            String packageName = this.getContext().getPackageName();
            if (activity.startsWith(packageName + "/")) {
                activity = activity.substring(packageName.length() + 1);
            }
            mLoadActivity = activity;
            FoxActivity.handler.post(() -> {
                String activityFinal = mLoadActivity;
                mLoadActivity = null;
                try {
                    this.setActivity(activityFinal.isEmpty() ? null : activityFinal);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set activity!", e);
                }
            });
        }
    }

    public void setActivity(String activity) throws ReflectiveOperationException {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Must be called from main thread");
        if (mLoadActivity != null) {
            mLoadActivity = activity == null ? "" : activity;
            return;
        }
        if (activity == null) {
            FoxActivity oldFoxActivity = mFoxActivity;
            if (oldFoxActivity != null) {
                oldFoxActivity.onDestroy();
            }
            mFoxActivity = null;
            mRealFrameLayout.removeAllViews();
            return;
        }
        this.ensureInitialized();
        Class<? extends FoxActivity> activityClass = Class.forName(activity)
                .asSubclass(FoxActivity.Embeddable.class).asSubclass(FoxActivity.class);
        if (mExternal) { // Extra check needed to externally embeddable
            activityClass.asSubclass(FoxActivity.ExternallyEmbeddable.class);
        }
        FoxActivity newFoxActivity = activityClass.newInstance();
        FoxActivity oldFoxActivity = mFoxActivity;
        if (oldFoxActivity != null) {
            oldFoxActivity.onDestroy();
        }
        mFoxActivity = null;
        mRealFrameLayout.removeAllViews();
        newFoxActivity.attachFoxActivityView(this);
        mFoxActivity = newFoxActivity;
        try {
            newFoxActivity.onCreate(mSavedInstanceState);
        } finally {
            mSavedInstanceState = null;
        }
        if (mPaused) {
            newFoxActivity.onPause();
        }
    }

    @NonNull
    public Activity getParentFoxActivity() {
        Context context = mRealFrameLayout.getContext();
        while (!(context instanceof FragmentActivity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else throw new IllegalStateException("Not inside a Activity");
        }
        return Objects.requireNonNull((FragmentActivity) context);
    }

    @Nullable
    FragmentActivity getParentFoxActivityInternal() {
        Context context = mRealFrameLayout.getContext();
        while (!(context instanceof FragmentActivity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else return null;
        }
        return Objects.requireNonNull((FragmentActivity) context);
    }

    @Nullable
    public FoxActivity getChildFoxActivity() {
        return mFoxActivity;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (mRealFrameLayout == this)
            super.onVisibilityChanged(changedView, visibility);
        boolean paused = visibility != VISIBLE;
        if (mFoxActivity != null && mPaused != paused) {
            if (paused) {
                mFoxActivity.onPause();
            } else {
                mFoxActivity.onResume();
            }
        }
        mPaused = paused;
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            this.setActivity(null);
        } catch (ReflectiveOperationException ignored) {}
        if (mRealFrameLayout == this)
            super.onDetachedFromWindow();
        mSavedInstanceState = null;
        mFragmentManager = null;
        mViewModelStore = null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mFoxActivity != null &&
                mFoxActivity.dispatchKeyEvent(event))
            return true;
        return mRealFrameLayout == this && super.dispatchKeyEvent(event);
    }

    void ensureInitialized() {
        if (mViewModelStore == null) {
            FragmentActivity parent = this.getParentFoxActivityInternal();
            if (parent != null) {
                mFragmentManager = parent.getSupportFragmentManager();
                mViewModelStore = parent.getViewModelStore();
                mExternal = mFragmentManager == null;
            } else {
                Activity activity = this.getParentFoxActivity();
                if (activity instanceof ComponentActivity) {
                    mViewModelStore = ((ComponentActivity)
                            activity).getViewModelStore();
                } else {
                    mViewModelStore = new ViewModelStore();
                    mExternal = true;
                }
            }
        }
    }

    public void recreate() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Must be called from main thread");
        if (mFoxActivity == null) return;
        Bundle savedInstance = new Bundle();
        mFoxActivity.onSaveInstanceState(savedInstance);
        mSavedInstanceState = savedInstance;
        try {
            this.setActivity(mFoxActivity.getClass().getName());
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Failed to recreate activity!", e);
        }
    }

    public boolean clickMenu() {
        FoxActivity foxActivity = mFoxActivity;
        return foxActivity != null && foxActivity.clickMenu();
    }

    public boolean isExternal() {
        return mExternal;
    }

    Context getApplicationContext() {
        return mApplicationContext;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState =
                new SavedState(super.onSaveInstanceState());
        if (mFoxActivity != null) {
            Bundle savedInstanceState = new Bundle();
            try {
                mFoxActivity.onSaveInstanceState(savedInstanceState);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save instance state!");
            }
            savedState.mSavedInstanceState = savedInstanceState;
            savedState.mActivity = mFoxActivity.getClass().getName();
            savedState.mViewModelStore = mViewModelStore;
        }
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state instanceof SavedState) {
            if (((SavedState) state).mViewModelStore != null)
                mViewModelStore = ((SavedState) state).mViewModelStore;
            mSavedInstanceState = ((SavedState) state).mSavedInstanceState;
            try {
                this.setActivity(((SavedState) state).mActivity);
            } catch (ReflectiveOperationException e) {
                Log.e(TAG, "Failed to restore activity", e);
            }
        } else if (state != null) {
            throw new IllegalArgumentException("Wrong state class, expecting FoxActivityView State "
                    + "but received " + state.getClass() + " instead. This usually happens "
                    + "when two views of different type have the same id in the same hierarchy. "
                    + "This view's id is " + resolveId(getContext(), getId()) + ". Make sure "
                    + "other views do not use the same id.");
        }
    }

    static Object resolveId(Context context, int id) {
        Object fieldValue;
        final Resources resources = context.getResources();
        if (id >= 0) {
            try {
                fieldValue = resources.getResourceTypeName(id) + '/' +
                        resources.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                fieldValue = "id/0x" + Integer.toHexString(id).toUpperCase();
            }
        } else {
            fieldValue = "NO_ID";
        }
        return fieldValue;
    }

    @NonNull
    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        FragmentActivity fragmentActivity =
                this.getParentFoxActivityInternal();
        return fragmentActivity != null ?
                fragmentActivity.getSavedStateRegistry() :
                this.mFoxActivity.getSavedStateRegistry();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return this.mFoxActivity.getLifecycle();
    }

    public static class SavedState extends BaseSavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[0];
            }
        };

        private String mActivity;
        private Bundle mSavedInstanceState;
        // ViewModelStore is not actually sent in parcelable
        private ViewModelStore mViewModelStore;

        public SavedState(Parcel source) {
            super(source);
            mActivity = source.readString();
            if (mActivity != null) {
                mSavedInstanceState = source.readBundle(
                        SavedState.class.getClassLoader());
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
            if (superState instanceof SavedState) {
                mActivity = ((SavedState) superState).mActivity;
                mSavedInstanceState =
                        ((SavedState) superState).mSavedInstanceState;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(mActivity);
            if (mActivity != null)
                out.writeBundle(mSavedInstanceState);
        }
    }

    @NonNull
    public static FrameLayout createFromLoadedComponent(
            @NonNull Context context, @NonNull ComponentName componentName) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(componentName, "componentName");
        if (context.getClassLoader() == FoxActivityView.class.getClassLoader() &&
                context.getPackageName().equals(componentName.getPackageName())) {
            return new FoxActivityView(context, null, componentName.getClassName());
        }
        Application application = FoxProcessExt.getApplication(componentName.getPackageName());
        if (application == null) throw new IllegalStateException(
                "Package \"" + componentName.getPackageName() + "\" not loaded");
        if (application.getClassLoader() == FoxActivityView.class.getClassLoader()) {
            return new FoxActivityView(context, application, componentName.getClassName());
        }
        if (Process.myUid() == Process.ROOT_UID || Process.myUid() == Process.SYSTEM_UID ||
                Process.myUid() == Process.PHONE_UID || Process.myUid() == Process.SHELL_UID) {
            throw new SecurityException("External loading is not allowed in privileged processes!");
        }
        try { // Create using cross process constructor
            Class<? extends FrameLayout> self;
            try { // in case I change the name in the future to be forward compatible
                self = Class.forName(CLASS_NAME1, true,
                        application.getClassLoader()).asSubclass(FrameLayout.class);
            } catch (ClassNotFoundException e) {
                try {
                    self = Class.forName(CLASS_NAME2, true,
                            application.getClassLoader()).asSubclass(FrameLayout.class);
                } catch (ClassNotFoundException e2) {
                    self = Class.forName(CLASS_NAME3, true,
                            application.getClassLoader()).asSubclass(FrameLayout.class);
                }
            }
            Constructor<? extends FrameLayout> constructor = self.getDeclaredConstructor(
                    Context.class, Context.class, FrameLayout.class, String.class);
            return constructor.newInstance(context, application,
                    null, componentName.getClassName());
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Package \"" + componentName.getPackageName() +
                            "\" do not support FoxActivityView");
        }
    }
}
