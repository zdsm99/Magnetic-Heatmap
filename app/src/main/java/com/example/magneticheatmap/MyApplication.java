package com.example.magneticheatmap;

import android.app.Application;
import android.preference.PreferenceManager;

import org.osmdroid.config.Configuration;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue("com.example.magneticheatmap");
    }
}
