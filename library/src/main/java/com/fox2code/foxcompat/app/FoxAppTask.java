package com.fox2code.foxcompat.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fox2code.foxcompat.app.internal.FoxProcessExt;

import java.util.List;

/**
 * Compat version of {@link ActivityManager.AppTask}
 */
public final class FoxAppTask implements Parcelable {
    private ActivityManager.AppTask mAppTask;
    public final int mTaskId;

    public static final Creator<FoxAppTask> CREATOR = new Creator<FoxAppTask>() {
        @Override
        public FoxAppTask createFromParcel(Parcel in) {
            return new FoxAppTask(null, in.readInt());
        }

        @Override
        public FoxAppTask[] newArray(int size) {
            return new FoxAppTask[size];
        }
    };

    private FoxAppTask(ActivityManager.AppTask appTask, int taskId) {
        mAppTask = appTask;
        mTaskId = taskId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(mTaskId);
    }

    @Nullable
    public ActivityManager.AppTask asAppTask() {
        if (mAppTask == null) { // Fix AppTask not existing
            getFoxAppTaskImpl(this, FoxProcessExt.getInitialApplication(), mTaskId, null);
        }
        return mAppTask;
    }

    public void finishAndRemoveTask() {
        ActivityManager.AppTask appTask = this.asAppTask();
        if (appTask != null) {
            appTask.finishAndRemoveTask();
        } else {
            throw new IllegalArgumentException("Unable to find task ID " + mTaskId);
        }
    }

    @Nullable
    public ActivityManager.RecentTaskInfo getTaskInfo() {
        ActivityManager.AppTask appTask = this.asAppTask();
        if (appTask != null) {
            return appTask.getTaskInfo();
        } else {
            throw new IllegalArgumentException("Unable to find task ID " + mTaskId);
        }
    }

    public void moveToFront() {
        ActivityManager.AppTask appTask = this.asAppTask();
        if (appTask != null) {
            appTask.moveToFront();
        } else {
            throw new IllegalArgumentException("Unable to find task ID " + mTaskId);
        }
    }

    public void startActivity(Context context, Intent intent, Bundle options) {
        ActivityManager.AppTask appTask = this.asAppTask();
        if (appTask != null) {
            appTask.startActivity(context, intent, options);
        } else {
            throw new IllegalArgumentException("Unable to find task ID " + mTaskId);
        }
    }

    public void setExcludeFromRecents(boolean excluded) {
        ActivityManager.AppTask appTask = this.asAppTask();
        if (appTask != null) {
            appTask.setExcludeFromRecents(excluded);
        } else {
            throw new IllegalArgumentException("Unable to find task ID " + mTaskId);
        }
    }

    public int getTaskId() {
        return mTaskId;
    }

    public static FoxAppTask getFoxAppTask(Activity activity) {
        if (activity instanceof FoxActivity)
            return ((FoxActivity) activity).getAppTask();
        return getFoxAppTaskImpl(null, activity,
                activity.getTaskId(), activity.getComponentName());
    }

    @Nullable
    static FoxAppTask getFoxAppTaskImpl(FoxAppTask source,
                                        Context context, int taskId, ComponentName fallback) {
        if (source != null && fallback != null) {
            throw new IllegalArgumentException(
                    "'fallback' and 'source' cannot be defined at the same time");
        }
        FoxAppTask appTask = source;
        ActivityManager am = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks = am.getAppTasks();
        if (tasks != null && tasks.size() > 0) {
            FoxAppTask fallbackTask = null;
            for (ActivityManager.AppTask task : tasks) {
                ActivityManager.RecentTaskInfo recentTaskInfo = task.getTaskInfo();
                if (recentTaskInfo != null) {
                    int iTaskId = getTaskId(recentTaskInfo);
                    if (iTaskId == taskId) {
                        if (source != null) {
                            source.mAppTask = task;
                            return source;
                        }
                        appTask = new FoxAppTask(task, iTaskId);
                        break;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (fallbackTask == null && fallback != null &&
                                fallback.equals(recentTaskInfo.topActivity)) {
                            fallbackTask = new FoxAppTask(task, iTaskId);
                        }
                    }
                }
            }
            if (appTask == null && fallbackTask != null) {
                appTask = fallbackTask;
            }
        }
        return appTask;
    }

    private static int getTaskId(ActivityManager.RecentTaskInfo recentTaskInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return recentTaskInfo.taskId;
        } else {
            return recentTaskInfo.persistentId;
        }
    }
}
