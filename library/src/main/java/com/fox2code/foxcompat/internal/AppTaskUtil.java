package com.fox2code.foxcompat.internal;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.IAppTask;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RestrictTo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Utility to allow parcelable AppTask
 *
 * Note: This class only exists for performance, and is only
 * loaded/initialized if FoxAppTask is used as a parcelable
 */
@SuppressLint("SoonBlockedPrivateApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppTaskUtil {
    private static final String TAG = "AppTaskUtil";
    private static final int processUid = Process.myUid();
    private static final Constructor<ActivityManager.AppTask> sAppTaskUnWrapper;
    private static final Field sAppTaskWrapper;

    static {
        FoxCompat.tryUnlockHiddenApisInternal();
        Constructor<ActivityManager.AppTask> sAppTaskUnWrapperTmp;
        try {
            sAppTaskUnWrapperTmp = ActivityManager.
                    AppTask.class.getConstructor(IAppTask.class);
            sAppTaskUnWrapperTmp.setAccessible(true);
        } catch (Throwable ignored) {
            sAppTaskUnWrapperTmp = null;
        }
        sAppTaskUnWrapper = sAppTaskUnWrapperTmp;
        Field sAppTaskWrapperTmp;
        try {
            sAppTaskWrapperTmp = ActivityManager.
                    AppTask.class.getDeclaredField("mAppTaskImpl");
            sAppTaskWrapperTmp.setAccessible(true);
        } catch (Throwable ignored) {
            sAppTaskWrapperTmp = null;
        }
        sAppTaskWrapper = sAppTaskWrapperTmp;
    }

    public static void writeToParcel(Parcel parcel, ActivityManager.AppTask appTask) {
        if (appTask == null || sAppTaskWrapper == null) {
            parcel.writeInt(-1);
            return;
        }
        IAppTask iAppTask = null;
        try {
            iAppTask = (IAppTask) sAppTaskWrapper.get(appTask);
        } catch (Exception e) {
            Log.e(TAG, "Failed to unwrap AppTask", e);
        }
        if (iAppTask == null) {
            parcel.writeInt(-1);
            return;
        }
        parcel.writeInt(processUid);
        parcel.writeStrongBinder(iAppTask.asBinder());
    }

    public static ActivityManager.AppTask readFromParcel(Parcel parcel) {
        int uid = parcel.readInt();
        if (uid == -1) return null;
        IAppTask iAppTask = IAppTask.Stub.asInterface(
                parcel.readStrongBinder());
        if (iAppTask == null || uid != processUid || sAppTaskUnWrapper == null) {
            return null; // Do not allow unsafe cross process transfer
        }
        try {
            return sAppTaskUnWrapper.newInstance(iAppTask);
        } catch (Exception e) {
            Log.e(TAG, "Failed to construct AppTask", e);
            return null;
        }
    }
}
