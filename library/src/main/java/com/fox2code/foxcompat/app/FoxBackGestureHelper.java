package com.fox2code.foxcompat.app;

import android.os.Build;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;
import androidx.core.util.Consumer;
import androidx.customview.widget.Openable;

import com.fox2code.foxcompat.os.FoxNavigationMode;

import java.lang.ref.WeakReference;

@OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
public final class FoxBackGestureHelper {
    private static final String TAG = "FoxBackGestureHelper";
    private FoxActivity.OnBackPressedCallback mOnBackPressedCallback;
    private long mOnBackPressedTimeout; // Avoid accidental presses
    private long mOnBackPressedTimeoutDrawer;
    private final FoxActivity mFoxActivity;
    private final Runnable mUpdateStateRunnable;
    private Consumer<FoxActivity> mFallbackOnBackPressed;
    private final OnBackPressedCallback mOnBackPressedCallbackX;

    FoxBackGestureHelper(FoxActivity foxActivity) {
        mFoxActivity = foxActivity;
        mUpdateStateRunnable = this::updateState;
        mOnBackPressedCallbackX = BuildCompat.isAtLeastT() ?
                new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (mFoxActivity.mFoxNavigationMode !=
                        FoxNavigationMode.GESTURAL_NAVIGATION &&
                        mOnBackPressedTimeout > System.currentTimeMillis()) {
                    return;
                }
                if (handleBackButton()) {
                    this.setEnabled(false);
                    doTiramisuOnBackPressed();
                    this.setEnabled(true);
                }
            }
        } : null;
    }

    public boolean canUseOnBackPressed() {
        return !(mFoxActivity.isEmbedded() ||
                mFoxActivity.isDestroyed() ||
                mFoxActivity.isFinishing());
    }

    void onCreateOnce() {
        if (BuildCompat.isAtLeastT()) {
            mFoxActivity.getOnBackPressedDispatcher().addCallback(mOnBackPressedCallbackX);
        }
    }

    public void setOnBackPressedCallback(
            @Nullable FoxActivity.OnBackPressedCallback onBackPressedCallback) {
        FoxActivity.OnBackPressedCallback oldOnBackPressedCallback = mOnBackPressedCallback;
        if (oldOnBackPressedCallback == onBackPressedCallback) return;
        mOnBackPressedCallback = onBackPressedCallback;
        if (oldOnBackPressedCallback instanceof StatefulOnBackPressedCallback) {
            ((StatefulOnBackPressedCallback) oldOnBackPressedCallback)
                    .removeReference(mFoxActivity.mSelfReference);
        }
        if (onBackPressedCallback instanceof StatefulOnBackPressedCallback) {
            ((StatefulOnBackPressedCallback) onBackPressedCallback)
                    .mBoundActivity = mFoxActivity.mSelfReference;
        }
    }

    /**
     * Used when FoxCompat doesn't know what to do with the current application state.
     */
    public void setFallbackOnBackPressed(Consumer<FoxActivity> fallbackOnBackPressed) {
        mFallbackOnBackPressed = fallbackOnBackPressed;
    }

    public FoxActivity.OnBackPressedCallback getOnBackPressedCallback() {
        return mOnBackPressedCallback;
    }

    void applyDrawerTimeout() {
        mOnBackPressedTimeoutDrawer =
                System.currentTimeMillis() + 450;
    }

    public void onBackPressed() {
        if (!this.canUseOnBackPressed())
            return;
        if (mOnBackPressedTimeout > System.currentTimeMillis()
                || mFoxActivity.isFinishing()) return;
        if (BuildCompat.isAtLeastT() &&
                mOnBackPressedCallbackX.isEnabled()) {
            Log.d(TAG, "onBackPressed() called while AndroidX callback registered.");
            mFoxActivity.super_onBackPressed();
        } else if (handleBackButton()) {
            if (BuildCompat.isAtLeastT()) {
                doTiramisuOnBackPressed();
            } else {
                doPreTiramisuOnBackPressed();
            }
        }
    }

    private boolean handleBackButton() {
        Log.d(TAG, "handleBackButton() called!");
        Openable openableDrawer = mFoxActivity.getDrawerOpenable();
        if (openableDrawer != null) {
            if (openableDrawer.isOpen()) {
                this.resetOnBackPressedTimeOut();
                openableDrawer.close();
                return false;
            } else if (mOnBackPressedTimeoutDrawer >
                    System.currentTimeMillis()) {
                openableDrawer.close(); // Make app more responsive
                return false; // In case opening animation is still playing.
            }
        }
        FoxActivity.OnBackPressedCallback onBackPressedCallback = mOnBackPressedCallback;
        mOnBackPressedCallback = null;
        boolean ret = onBackPressedCallback == null ||
                !onBackPressedCallback.onBackPressed(mFoxActivity);
        updateState();
        return ret;
    }

    private void doTiramisuOnBackPressed() {
        if (mFoxActivity.getOnBackPressedDispatcher().hasEnabledCallbacks()) {
            mFoxActivity.getOnBackPressedDispatcher().onBackPressed();
        } else if (!mFoxActivity.isFinishing()) {
            if (!mFoxActivity.isTaskRoot()) {
                mFoxActivity.finishAfterTransition();
            } else if (mFallbackOnBackPressed != null) {
                mFallbackOnBackPressed.accept(mFoxActivity);
            } else {
                mFoxActivity.finish();
            }
        }
    }

    private void doPreTiramisuOnBackPressed() {
        boolean hasCallbacks = mFoxActivity.getOnBackPressedDispatcher().hasEnabledCallbacks();
        mFoxActivity.super_onBackPressed();
        if (!hasCallbacks && !mFoxActivity.isTaskRoot() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            doFallbackOnBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void doFallbackOnBackPressed() {
        if (!mFoxActivity.isActivityTransitionRunning() &&
                !mFoxActivity.isFinishing()) {
            if (!mFoxActivity.isTaskRoot()) {
                mFoxActivity.finishAfterTransition();
            } else if (mFallbackOnBackPressed != null) {
                mFallbackOnBackPressed.accept(mFoxActivity);
            } else {
                mFoxActivity.finish();
            }
        }
    }

    void resetOnBackPressedTimeOut() {
        mOnBackPressedTimeout = System.currentTimeMillis() + 250;
    }

    public void forceBackPressed() {
        if (this.canUseOnBackPressed()) {
            mOnBackPressedTimeoutDrawer = 0;
            mOnBackPressedTimeout = 0;
            if (BuildCompat.isAtLeastT()) {
                this.doTiramisuOnBackPressed();
            } else {
                this.doPreTiramisuOnBackPressed();
            }
        }
    }

    public void pressBack() {
        if (this.canUseOnBackPressed()) {
            mOnBackPressedTimeoutDrawer = 0;
            mOnBackPressedTimeout = 0;
            if (BuildCompat.isAtLeastT()) {
                this.doTiramisuOnBackPressed();
            } else {
                this.doPreTiramisuOnBackPressed();
            }
        }
    }

    public void updateState() {
        FoxActivity.OnBackPressedCallback onBackPressedCallback = mOnBackPressedCallback;
        boolean hasBackPressHandler = onBackPressedCallback != null &&
                !(onBackPressedCallback instanceof StatefulOnBackPressedCallback &&
                        !((StatefulOnBackPressedCallback) onBackPressedCallback).kindaEnabled());
        if (BuildCompat.isAtLeastT()) {
            Log.d(TAG, "handleBackButton() called!");
            boolean enableAndroidXCallback = hasBackPressHandler ||
                    /*mFoxActivity.mFoxNavigationMode !=
                            FoxNavigationMode.GESTURAL_NAVIGATION ||/**/
                    !mFoxActivity.isTaskRoot();
            if (mOnBackPressedCallbackX.isEnabled() != enableAndroidXCallback) {
                mOnBackPressedCallbackX.setEnabled(enableAndroidXCallback);
            }
        }
    }

    public static abstract class StatefulOnBackPressedCallback
            extends FoxActivity.TrustedBackPressedListener {
        private WeakReference<FoxActivity> mBoundActivity;
        private boolean mEnabled = true;

        @Override
        public final boolean onBackPressed(FoxActivity compatActivity) {
            if (this.mDisable) {
                compatActivity.setOnBackPressedCallback(this);
                return true;
            }
            return mEnabled && this.onBackPressedStateful(compatActivity);
        }

        public abstract boolean onBackPressedStateful(FoxActivity compatActivity);

        public final void setEnabled(boolean enabled) {
            if (mEnabled == enabled) return;
            mEnabled = enabled;
            notifyUpdateState();
        }

        public final boolean isEnabled() {
            return mEnabled;
        }

        @Override
        void setTrustedDisable(boolean disable) {
            if (mDisable != disable) {
                mDisable = disable;
                notifyUpdateState();
            }
        }

        void removeReference(WeakReference<FoxActivity> reference) {
            if (mBoundActivity == reference) {
                mBoundActivity = null;
            }
        }

        private void notifyUpdateState() {
            WeakReference<FoxActivity> boundActivity = mBoundActivity;
            FoxActivity activity;
            if (boundActivity != null && (activity = boundActivity.get()) != null) {
                activity.runOnUiThread(activity.mFoxBackGestureHelper.mUpdateStateRunnable);
            }
        }

        private boolean kindaEnabled() {
            return mDisable || mEnabled;
        }
    }
}
