package com.jethers.mobcompfinalproject;

import android.app.Application;
import android.content.Context;
import androidx.multidex.MultiDex;

public class TranslatorApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
