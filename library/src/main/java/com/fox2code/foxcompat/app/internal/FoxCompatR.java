package com.fox2code.foxcompat.app.internal;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FoxCompatR {
    public static class style {
        public static final int ThemeOverlay_Material3_DynamicColors_Light;
        public static final int ThemeOverlay_Material3_DynamicColors_Dark;

        static {
            int tmpThemeOverlay_Material3_DynamicColors_Light = 0;
            int tmpThemeOverlay_Material3_DynamicColors_Dark = 0;
            try {
                tmpThemeOverlay_Material3_DynamicColors_Light =
                        com.google.android.material.R.style.
                                ThemeOverlay_Material3_DynamicColors_Light;
                tmpThemeOverlay_Material3_DynamicColors_Dark =
                        com.google.android.material.R.style.
                                ThemeOverlay_Material3_DynamicColors_Dark;
            } catch (Throwable ignored) {}
            ThemeOverlay_Material3_DynamicColors_Light = tmpThemeOverlay_Material3_DynamicColors_Light;
            ThemeOverlay_Material3_DynamicColors_Dark = tmpThemeOverlay_Material3_DynamicColors_Dark;
        }
    }

    public static class styleable {
        public static final int[] CardView;
        public static final int CardView_cardBackgroundColor;

        static {
            int[] tmpCardView = null;
            int tmpCardView_cardBackgroundColor = 0;
            try {
                tmpCardView = androidx.cardview.R.styleable.CardView;
                tmpCardView_cardBackgroundColor = androidx.cardview.R.styleable.CardView_cardBackgroundColor;
            } catch (Throwable ignored) {}
            CardView = tmpCardView;
            CardView_cardBackgroundColor = tmpCardView_cardBackgroundColor;
        }
    }
}
