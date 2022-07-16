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
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.fox2code.foxcompat.internal.FoxProcessExt;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * View that allow to include a FoxActivity
 */
public final class FoxActivityView extends FrameLayout {
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
        this.mRealFrameLayout = this;
        this.mApplicationContext = null;
        this.init(null, 0, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mRealFrameLayout = this;
        this.mApplicationContext = null;
        this.init(attrs, 0, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mRealFrameLayout = this;
        this.mApplicationContext = null;
        this.init(attrs, defStyleAttr, null);
    }

    public FoxActivityView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRealFrameLayout = this;
        this.mApplicationContext = null;
        this.init(attrs, defStyleAttr, null);
    }

    // Constructor for FoxActivityView cross process support V1.
    // If you wish to edit this constructor, create a new one instead to preserve compatibility.
    @Keep @RestrictTo(RestrictTo.Scope.LIBRARY)
    public FoxActivityView(@NonNull Context context,@NonNull Context applicationContext,
                           @Nullable FrameLayout realFrameLayout, @Nullable String activity) {
        super(context); this.mExternal = true;
        this.mRealFrameLayout = realFrameLayout == null ? this : realFrameLayout;
        this.mApplicationContext = Objects.requireNonNull(applicationContext);
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
        this.mRealFrameLayout = this;
        this.mApplicationContext = application;
        this.init(null, 0, activity);
    }

    private void init(AttributeSet attrs, int defStyleAttr, String activity) {
        FragmentActivity parent = this.getParentFoxActivityInternal();
        if (parent != null) {
            this.mFragmentManager = parent.getSupportFragmentManager();
            this.mViewModelStore = parent.getViewModelStore();
            this.mExternal = this.mFragmentManager == null;
        } else this.mExternal = true;
        if (activity == null && this.mRealFrameLayout == this) {
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
            this.mLoadActivity = activity;
            FoxActivity.handler.post(() -> {
                String activityFinal = this.mLoadActivity;
                this.mLoadActivity = null;
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
        if (this.mLoadActivity != null) {
            this.mLoadActivity = activity == null ? "" : activity;
            return;
        }
        if (activity == null) {
            FoxActivity oldFoxActivity = this.mFoxActivity;
            if (oldFoxActivity != null) {
                oldFoxActivity.onDestroy();
            }
            this.mFoxActivity = null;
            this.mRealFrameLayout.removeAllViews();
            this.mViewModelProviderFactory = null;
            return;
        }
        this.ensureInitialized();
        Class<? extends FoxActivity> activityClass = Class.forName(activity)
                .asSubclass(FoxActivity.Embeddable.class).asSubclass(FoxActivity.class);
        if (this.mExternal) { // Extra check needed to externally embeddable
            activityClass.asSubclass(FoxActivity.ExternallyEmbeddable.class);
        }
        FoxActivity newFoxActivity = activityClass.newInstance();
        FoxActivity oldFoxActivity = this.mFoxActivity;
        if (oldFoxActivity != null) {
            oldFoxActivity.onDestroy();
        }
        this.mFoxActivity = null;
        this.mRealFrameLayout.removeAllViews();
        this.mViewModelProviderFactory = null;
        newFoxActivity.attachFoxActivityView(this);
        this.mFoxActivity = newFoxActivity;
        try {
            newFoxActivity.onCreate(this.mSavedInstanceState);
        } finally {
            this.mSavedInstanceState = null;
        }
        if (this.mPaused) {
            newFoxActivity.onPause();
        }
    }

    @NonNull
    public Activity getParentFoxActivity() {
        Context context = this.mRealFrameLayout.getContext();
        while (!(context instanceof FragmentActivity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else throw new IllegalStateException("Not inside a Activity");
        }
        return Objects.requireNonNull((FragmentActivity) context);
    }

    @Nullable
    FragmentActivity getParentFoxActivityInternal() {
        Context context = this.mRealFrameLayout.getContext();
        while (!(context instanceof FragmentActivity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else return null;
        }
        return Objects.requireNonNull((FragmentActivity) context);
    }

    @Nullable
    public FoxActivity getChildFoxActivity() {
        return this.mFoxActivity;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (this.mRealFrameLayout == this)
            super.onVisibilityChanged(changedView, visibility);
        boolean paused = visibility != VISIBLE;
        if (this.mFoxActivity != null && this.mPaused != paused) {
            if (paused) {
                this.mFoxActivity.onPause();
            } else {
                this.mFoxActivity.onResume();
            }
        }
        this.mPaused = paused;
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            this.setActivity(null);
        } catch (ReflectiveOperationException ignored) {}
        if (this.mRealFrameLayout == this)
            super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mFoxActivity != null &&
                this.mFoxActivity.dispatchKeyEvent(event))
            return true;
        return this.mRealFrameLayout == this && super.dispatchKeyEvent(event);
    }

    void ensureInitialized() {
        if (this.mViewModelStore == null) {
            FragmentActivity parent = this.getParentFoxActivityInternal();
            if (parent != null) {
                this.mFragmentManager = parent.getSupportFragmentManager();
                this.mViewModelStore = parent.getViewModelStore();
                this.mExternal = this.mFragmentManager == null;
            } else {
                Activity activity = this.getParentFoxActivity();
                if (activity instanceof ComponentActivity) {
                    this.mViewModelStore = ((ComponentActivity)
                            activity).getViewModelStore();
                } else {
                    this.mViewModelStore = new ViewModelStore();
                    this.mExternal = true;
                }
            }
        }
    }

    public void recreate() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Must be called from main thread");
        if (this.mFoxActivity == null) return;
        Bundle savedInstance = new Bundle();
        this.mFoxActivity.onSaveInstanceState(savedInstance);
        this.mSavedInstanceState = savedInstance;
        try {
            this.setActivity(this.mFoxActivity.getClass().getName());
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Failed to recreate activity!", e);
        }
    }

    public boolean clickMenu() {
        FoxActivity foxActivity = this.mFoxActivity;
        return foxActivity != null && foxActivity.clickMenu();
    }

    public boolean isExternal() {
        return this.mExternal;
    }

    Context getApplicationContext() {
        return this.mApplicationContext;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState =
                new SavedState(super.onSaveInstanceState());
        if (this.mFoxActivity != null) {
            Bundle savedInstanceState = new Bundle();
            try {
                this.mFoxActivity.onSaveInstanceState(savedInstanceState);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save instance state!");
            }
            savedState.mSavedInstanceState = savedInstanceState;
            savedState.mActivity = this.mFoxActivity.getClass().getName();
            savedState.mViewModelStore = this.mViewModelStore;
        }
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state instanceof SavedState) {
            if (((SavedState) state).mViewModelStore != null)
                this.mViewModelStore = ((SavedState) state).mViewModelStore;
            this.mSavedInstanceState = ((SavedState) state).mSavedInstanceState;
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
            this.mActivity = source.readString();
            if (this.mActivity != null) {
                this.mSavedInstanceState = source.readBundle(
                        SavedState.class.getClassLoader());
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
            if (superState instanceof SavedState) {
                this.mActivity = ((SavedState) superState).mActivity;
                this.mSavedInstanceState =
                        ((SavedState) superState).mSavedInstanceState;
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.mActivity);
            if (this.mActivity != null)
                out.writeBundle(this.mSavedInstanceState);
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
