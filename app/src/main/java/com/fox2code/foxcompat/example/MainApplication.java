package com.fox2code.foxcompat.example;

import android.util.Log;

import com.fox2code.foxcompat.app.FoxActivity;
import com.fox2code.foxcompat.app.FoxApplication;

public class MainApplication extends FoxApplication {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onRefreshUI(FoxActivity foxActivity) {
        this.updateTheme(foxActivity);
        super.onRefreshUI(foxActivity);
    }

    @Override
    public void onCreateFoxActivity(FoxActivity foxActivity) {
        this.updateTheme(foxActivity);
        super.onCreateFoxActivity(foxActivity);
    }

    public void updateTheme(FoxActivity foxActivity) {
        boolean transparent = this.getSharedPreferences(
                "example", MODE_PRIVATE).getBoolean("transparent", false);
        Log.d("MainApplication", "isTransparent " + transparent);
        foxActivity.setThemeRecreate(transparent ?
                R.style.Theme_FoxCompat_Transparent : R.style.Theme_FoxCompat);
        if (!transparent) {
            foxActivity.setUseDynamicColors(true);
        }
    }
}
