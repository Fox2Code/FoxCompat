package android.app;

import android.os.Build;

public class ActivityThread {
    public static String currentProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        } else return System.getProperty("currentProcessName");
    }

    public static Application currentApplication() {
        return null;
    }
}
