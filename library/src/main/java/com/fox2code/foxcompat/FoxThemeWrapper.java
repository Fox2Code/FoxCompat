package com.fox2code.foxcompat;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import com.fox2code.foxcompat.internal.FoxCompat;

public class FoxThemeWrapper extends ContextThemeWrapper {
    boolean mUseDynamicColors;

    public FoxThemeWrapper(Context base, @StyleRes int themeResId) {
        super(base, themeResId);
    }

    public FoxThemeWrapper(Context base, @StyleRes int themeResId, boolean useDynamicColors) {
        super(base, themeResId);
        mUseDynamicColors = useDynamicColors;
    }

    public void setUseDynamicColors(boolean useDynamicColors) {
        mUseDynamicColors = useDynamicColors;
    }

    public boolean useDynamicColors() {
        return mUseDynamicColors;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        if (theme != null) {
            boolean isLightTheme = true;
            boolean useDynamicColors = mUseDynamicColors &&
                    FoxCompat.isDynamicAccentSupported(this);
            try {
                isLightTheme = FoxDisplay.isLightTheme(theme);
            } catch (Exception ignored) {
                useDynamicColors = false;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && useDynamicColors) {
                theme.applyStyle(isLightTheme ?
                        R.style.FoxCompat_Overrides_System :
                        FoxLineage.getFoxLineage(this).isBlackMode() ?
                                R.style.FoxCompat_Overrides_System_Black :
                                R.style.FoxCompat_Overrides_System_Dark, true);
            } else {
                theme.applyStyle(FoxCompat.isOxygenOS() ?
                        R.style.FoxCompat_Overrides_Base_Oxygen :
                        FoxCompat.isDynamicAccentSupported(this) ?
                                R.style.FoxCompat_Overrides_Base_System :
                                R.style.FoxCompat_Overrides_Base, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors) {
                    theme.applyStyle(isLightTheme ?
                            com.google.android.material.R.style.
                                    ThemeOverlay_Material3_DynamicColors_Light :
                            com.google.android.material.R.style.
                                    ThemeOverlay_Material3_DynamicColors_Dark, true);
                }
            }
        }
    }

    public boolean isLightTheme() {
        return FoxDisplay.isLightTheme(this.getTheme());
    }

    @ColorInt
    public final int getColorCompat(@ColorRes @AttrRes int color) {
        TypedValue typedValue = new TypedValue();
        this.getTheme().resolveAttribute(color, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, color);
    }
}
