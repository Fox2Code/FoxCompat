package com.fox2code.foxcompat.app.internal;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Application managing Intent hook, the class is separate form FoxApplication
 * because it is too confusing and require a lot more off effort if
 * everything is in FoxApplication
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FoxIntentApplication extends Application {
    @Override
    public void startActivity(Intent intent) {
        super.startActivity(this.patchIntent(intent));
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(this.patchIntent(intent), options);
    }

    @Override
    public void startActivities(Intent[] intents) {
        super.startActivities(this.patchIntents(intents));
    }

    @Override
    public void startActivities(Intent[] intents, @Nullable Bundle options) {
        super.startActivities(this.patchIntents(intents), options);
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return this.startService(service);
        }
        return super.startForegroundService(this.patchIntent(service));
    }

    @Nullable
    @Override
    public ComponentName startService(Intent service) {
        return super.startService(this.patchIntent(service));
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return super.bindService(this.patchIntent(service), conn, flags);
    }

    @Override
    public boolean bindService(Intent service, int flags, Executor executor, ServiceConnection conn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return this.bindService(service, conn, flags);
        }
        return super.bindService(this.patchIntent(service), flags, executor, conn);
    }

    @Override
    public boolean bindIsolatedService(Intent service, int flags, String instanceName, Executor executor, ServiceConnection conn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return this.bindService(service, flags | BIND_AUTO_CREATE, executor, conn);
        }
        return super.bindIsolatedService(this.patchIntent(service), flags, instanceName, executor, conn);
    }

    @Override
    public int checkSelfPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return super.checkSelfPermission(permission);
        }
        return super.checkPermission(permission, Process.myPid(), Process.myUid());
    }

    public final Intent[] patchIntents(Intent[] intents) {
        if (intents == null) return null;
        int i = 0;
        for (;i < intents.length; i++) {
            Intent original = intents[i];
            Intent patched = this.patchIntent(original);
            if (original != patched) {
                intents = Arrays.copyOf(intents, intents.length);
                intents[i] = patched;
                break;
            }
        }
        for (;i < intents.length; i++) {
            intents[i] = this.patchIntent(intents[i]);
        }
        return intents;
    }

    @CallSuper
    private Intent patchIntent(Intent intent) {
        if (intent == null) return null;
        return FoxProcessExt.patchIntent(intent);
    }
}
