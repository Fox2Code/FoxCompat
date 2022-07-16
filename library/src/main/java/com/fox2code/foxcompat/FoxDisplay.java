package com.fox2code.foxcompat;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;

import androidx.annotation.Dimension;
import androidx.annotation.Px;
import androidx.core.graphics.ColorUtils;

import com.fox2code.foxcompat.internal.FoxCompat;

import org.jetbrains.annotations.Contract;

public final class FoxDisplay {
    @SuppressWarnings("deprecation")
    public static Display getDisplay(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return activity.getDisplay();
        }
        if (activity instanceof FoxActivity) {
            return ((FoxActivity) activity).getDisplay();
        }
        return activity.getWindowManager().getDefaultDisplay();
    }

    @Contract("null -> false")
    public static boolean isLightTheme(Resources.Theme theme) {
        if (theme == null) return false;
        TypedValue typedValue = new TypedValue();
        if (FoxCompat.googleMaterial) {
            theme.resolveAttribute( // Check with google material first
                    com.google.android.material.R.attr.isLightTheme, typedValue, true);
            if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN) {
                return typedValue.data != 0;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            theme.resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
            if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN) {
                return typedValue.data != 0;
            }
        }
        theme.resolveAttribute(android.R.attr.background, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorUtils.calculateLuminance(typedValue.data) > 0.7D;
        }
        throw new IllegalStateException("Theme is not a valid theme!");
    }

    @Dimension @Px
    public static int dpToPixel(@Dimension(unit = Dimension.DP) float dp){
        return (int) (dp * ((float) Resources.getSystem().getDisplayMetrics()
                        .densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Dimension(unit = Dimension.DP)
    public static float pixelsToDp(@Dimension @Px int px){
        return (px / ((float) Resources.getSystem().getDisplayMetrics()
                        .densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
