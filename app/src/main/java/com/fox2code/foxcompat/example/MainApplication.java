package com.fox2code.foxcompat.example;

import com.fox2code.foxcompat.FoxApplication;

public class MainApplication extends FoxApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        boolean monet = this.getSharedPreferences(
                "example", MODE_PRIVATE).getBoolean("monet", false);
    }
}
