package com.fox2code.foxcompat.internal;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Activity managing Intent hook, the class is separate form FoxActivity
 * because it is too confusing and require a lot more off effort if
 * everything is in FoxActivity
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FoxIntentActivity extends AppCompatActivity {
    @Override
    public void startActivity(Intent intent) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivity(intent);
        } else {
            super.startActivity(this.patchIntent(intent));
        }
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivity(intent, options);
        } else {
            super.startActivity(this.patchIntent(intent), options);
        }
    }

    @Override
    public void startActivities(Intent[] intents) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivities(intents);
        } else {
            super.startActivities(this.patchIntents(intents));
        }
    }

    @Override
    public void startActivities(Intent[] intents, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivities(intents, options);
        } else {
            super.startActivities(this.patchIntents(intents), options);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivityForResult(intent, requestCode, options);
        } else {
            super.startActivityForResult(this.patchIntent(intent), requestCode, options);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivityForResult(intent, requestCode);
        } else {
            super.startActivityForResult(this.patchIntent(intent), requestCode);
        }
    }

    @Override
    public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startActivityIfNeeded(intent, requestCode, options);
        } else {
            return super.startActivityIfNeeded(this.patchIntent(intent), requestCode, options);
        }
    }

    @Override
    public boolean startActivityIfNeeded(@NonNull Intent intent, int requestCode) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startActivityIfNeeded(intent, requestCode);
        } else {
            return super.startActivityIfNeeded(this.patchIntent(intent), requestCode);
        }
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, Intent intent, int requestCode) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivityFromChild(child, intent, requestCode);
        } else {
            super.startActivityFromChild(child, this.patchIntent(intent), requestCode);
        }
    }

    @Override
    public void startActivityFromChild(@NonNull Activity child, Intent intent, int requestCode, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            receiver.startActivityFromChild(child, intent, requestCode, options);
        } else {
            super.startActivityFromChild(child, this.patchIntent(intent), requestCode, options);
        }
    }

    @Override
    public boolean startNextMatchingActivity(@NonNull Intent intent) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startNextMatchingActivity(intent);
        } else {
            return super.startNextMatchingActivity(this.patchIntent(intent));
        }
    }

    @Override
    public boolean startNextMatchingActivity(@NonNull Intent intent, @Nullable Bundle options) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startNextMatchingActivity(intent, options);
        } else {
            return super.startNextMatchingActivity(this.patchIntent(intent), options);
        }
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return this.startService(service);
        }
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startForegroundService(service);
        } else {
            return super.startForegroundService(this.patchIntent(service));
        }
    }

    @Nullable
    @Override
    public ComponentName startService(Intent service) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.startService(service);
        } else {
            return super.startService(this.patchIntent(service));
        }
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.bindService(service, conn, flags);
        } else {
            return super.bindService(this.patchIntent(service), conn, flags);
        }
    }

    @Override
    public boolean bindService(Intent service, int flags, Executor executor, ServiceConnection conn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return this.bindService(service, conn, flags);
        }
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.bindService(service,  flags, executor, conn);
        } else {
            return super.bindService(this.patchIntent(service), flags, executor, conn);
        }
    }

    @Override
    public boolean bindIsolatedService(Intent service, int flags, String instanceName, Executor executor, ServiceConnection conn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return this.bindService(service, flags | BIND_AUTO_CREATE, executor, conn);
        }
        Activity receiver = this.getIntentReceiver();
        if (receiver != null) {
            return receiver.bindIsolatedService(service, flags, instanceName, executor, conn);
        } else {
            return super.bindIsolatedService(this.patchIntent(service), flags, instanceName, executor, conn);
        }
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
    public Intent patchIntent(Intent intent) {
        if (intent == null) return null;
        return FoxProcessExt.patchIntent(intent);
    }

    protected Activity getIntentReceiver() {
        return null;
    }

    public final Application getApplicationIntent() {
        Application application = super.getApplication();
        if (application != null) return application;
        Activity activity = this.getIntentReceiver();
        if (activity instanceof FoxIntentActivity) {
            return ((FoxIntentActivity) activity).getApplicationIntent();
        }
        application = FoxProcessExt.getApplication(this.getPackageName());
        return application != null ? application : (activity == null ?
                super.getApplication() : activity.getApplication());
    }
}
