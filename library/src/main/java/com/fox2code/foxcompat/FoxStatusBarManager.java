package com.fox2code.foxcompat;

import android.annotation.SuppressLint;
import android.annotation.SystemService;
import android.app.Activity;
import android.app.SemStatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.fox2code.foxcompat.internal.FoxCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper for {@link android.app.StatusBarManager},
 * use {@link SemStatusBarManager} if available.
 * */
@SystemService(FoxStatusBarManager.FOX_STATUS_BAR_SERVICE)
public final class FoxStatusBarManager {
    public static final String FOX_STATUS_BAR_SERVICE = "fox_statusbar";
    private static final String TAG = "FoxStatusBarManager";
    private static Method sExpandNotificationsPanel, sExpandSettingsPanel, sCollapsePanels;

    static {
        try {
            FoxCompat.tryUnlockHiddenApisInternal();
            Class<?> cls = Class.forName("android.app.StatusBarManager");
            (sExpandNotificationsPanel = cls.getDeclaredMethod("expandNotificationsPanel")).setAccessible(true);
            (sExpandSettingsPanel = cls.getDeclaredMethod("expandSettingsPanel")).setAccessible(true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                (sCollapsePanels = cls.getDeclaredMethod("collapsePanels")).setAccessible(true);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to setup StatusBarManager reflection.", t);
        }
    }

    private final Context mContext;
    private final Object mSemStatusBarManager;
    private final Object mStatusBarManager;

    @SuppressLint("WrongConstant")
    public static FoxStatusBarManager from(Context context) {
        Object o = context.getSystemService(FoxStatusBarManager.FOX_STATUS_BAR_SERVICE);
        return o instanceof FoxStatusBarManager ? (FoxStatusBarManager) o :
                new FoxStatusBarManager(context);
    }

    @SuppressLint("WrongConstant")
    public static FoxStatusBarManager createForActivity(Activity activity) {
        Context baseContext = activity.getBaseContext();
        Object o = baseContext == null ? null : // In some rare cases baseContext can be null
                baseContext.getSystemService(FoxStatusBarManager.FOX_STATUS_BAR_SERVICE);
        return o instanceof FoxStatusBarManager ? (FoxStatusBarManager) o :
                new FoxStatusBarManager(activity);
    }

    @SuppressLint("WrongConstant")
    FoxStatusBarManager(Context context) {
        this.mContext = context;
        if (FoxCompat.samsungStatusBarManager) {
            this.mSemStatusBarManager = context.getSystemService("sem_statusbar");
        } else this.mSemStatusBarManager = null;
        this.mStatusBarManager = context.getSystemService("statusbar");
    }

    @RequiresPermission(anyOf = {permission.EXPAND_STATUS_BAR})
    public void expandNotificationsPanel() {
        if (this.mSemStatusBarManager != null) {
            try {
                ((SemStatusBarManager) this.mSemStatusBarManager).expandNotificationsPanel();
                return; // If SemStatusBarManager is successful, don't try system impl
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {}
        }
        if (this.mStatusBarManager != null && sExpandNotificationsPanel != null) {
            try {
                sExpandNotificationsPanel.invoke(this.mStatusBarManager);
            } catch (InvocationTargetException invocationTargetException) {
                Throwable cause = invocationTargetException.getTargetException();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            } catch (Throwable ignored) {}
        }
    }

    @RequiresPermission(anyOf = {permission.EXPAND_STATUS_BAR})
    public void expandSettingsPanel() {
        if (this.mSemStatusBarManager != null) {
            try {
                ((SemStatusBarManager) this.mSemStatusBarManager).expandQuickSettingsPanel();
                return; // If SemStatusBarManager is successful, don't try system impl
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {}
        }
        if (this.mStatusBarManager != null && sExpandSettingsPanel != null) {
            try {
                sExpandSettingsPanel.invoke(this.mStatusBarManager);
            } catch (InvocationTargetException invocationTargetException) {
                Throwable cause = invocationTargetException.getTargetException();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            } catch (Throwable ignored) {}
        }
    }

    @RequiresPermission(allOf = {permission.BROADCAST_CLOSE_SYSTEM_DIALOGS, permission.EXPAND_STATUS_BAR})
    public void collapsePanels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (this.mSemStatusBarManager != null) {
                try {
                    ((SemStatusBarManager) this.mSemStatusBarManager).collapsePanels();
                    return; // If SemStatusBarManager is successful, don't try system impl
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable ignored) {}
            }
            if (this.mStatusBarManager != null && sCollapsePanels != null) {
                try {
                    sCollapsePanels.invoke(this.mStatusBarManager); return;
                } catch (InvocationTargetException invocationTargetException) {
                    Throwable cause = invocationTargetException.getTargetException();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                } catch (Throwable ignored) {}
            }
        }
        this.mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                // This is to restrict the intent to system processes only
                .setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY), permission.STATUS_BAR);
    }

    public static class permission {
        public static final String BROADCAST_CLOSE_SYSTEM_DIALOGS =
                "android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS";
        public static final String EXPAND_STATUS_BAR =
                "android.permission.EXPAND_STATUS_BAR";
        public static final String STATUS_BAR =
                "android.permission.STATUS_BAR";
    }
}
