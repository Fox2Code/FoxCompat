package com.fox2code.foxcompat.os;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

import com.fox2code.foxcompat.app.FoxActivity;

public enum FoxNavigationMode {
    THREE_BUTTON_NAVIGATION,
    TWO_BUTTON_NAVIGATION,
    GESTURAL_NAVIGATION;

    @SuppressLint("DiscouragedApi")
    public static FoxNavigationMode queryForActivity(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return THREE_BUTTON_NAVIGATION;
        }
        if (activity instanceof FoxActivity && ((FoxActivity) activity).isEmbedded()) {
            return queryForActivity(((FoxActivity) activity).getContainerActivity());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            View decorView = activity.getWindow().peekDecorView();
            if (decorView != null) {
                WindowInsets windowInsets = decorView.getRootWindowInsets();
                if (windowInsets != null) {
                    Insets systemGestureInset = windowInsets.getSystemGestureInsets();
                    if (systemGestureInset.top != 0 && systemGestureInset.left != 0) {
                        return GESTURAL_NAVIGATION;
                    }
                }
            }
        }
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier(
                "config_navBarInteractionMode", "integer", "android");
        if (resourceId > 0) {
            switch (resources.getInteger(resourceId)) {
                case 0:
                    return THREE_BUTTON_NAVIGATION;
                case 1:
                    return TWO_BUTTON_NAVIGATION;
                case 2:
                    return GESTURAL_NAVIGATION;
            }
        }
        return THREE_BUTTON_NAVIGATION;
    }
}
