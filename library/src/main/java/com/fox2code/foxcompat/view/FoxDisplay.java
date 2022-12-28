package com.fox2code.foxcompat.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IdRes;
import androidx.annotation.Px;
import androidx.core.graphics.ColorUtils;

import com.fox2code.foxcompat.app.FoxActivity;

import org.jetbrains.annotations.Contract;

public final class FoxDisplay {
    public static final @ColorInt int COLOR_TRANSPARENT = Color.TRANSPARENT;
    public static final @ColorInt int COLOR_UNDEFINED = 0x00FFFFFF;

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

    /**
     * @param theme to check
     * @return if theme is light theme
     * @throws IllegalStateException if unable to determine if theme is dark/light mode.
     */
    @Contract("null -> false")
    public static boolean isLightTheme(Resources.Theme theme) throws IllegalStateException {
        return isLightTheme(theme, true);
    }

    /**
     * @param theme to check
     * @return if theme is light theme, use night mode as fallback
     * if unable to determine if theme is dark/light mode.
     */
    @Contract("null -> false")
    public static boolean isLightThemeSafe(Resources.Theme theme) {
        return isLightTheme(theme, false);
    }

    private static boolean isLightTheme(Resources.Theme theme,boolean doThrow) {
        if (theme == null) return false;
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute( // Check with google material first
                androidx.appcompat.R.attr.isLightTheme, typedValue, true);
        if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN) {
            return typedValue.data != 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            theme.resolveAttribute(android.R.attr.isLightTheme, typedValue, true);
            if (typedValue.type == TypedValue.TYPE_INT_BOOLEAN) {
                return typedValue.data != 0;
            }
        }
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return ColorUtils.calculateLuminance(typedValue.data) > 0.7D;
        }
        if (doThrow) {
            throw new IllegalStateException("Theme is not a valid theme!");
        } else {
            return (theme.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
        }
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

    public static String resolveId(Context context,@IdRes int id) {
        return resolveId(context.getResources(), id);
    }

    @SuppressLint("ResourceType")
    public static String resolveId(Resources resources, @IdRes int id) {
        String fieldValue;
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
}
