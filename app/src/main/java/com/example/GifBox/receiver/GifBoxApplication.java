package com.example.GifBox.receiver;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

public class GifBoxApplication extends Application {
    private AccessibilitySettingsObserver accessibilitySettingsObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        
        accessibilitySettingsObserver = new AccessibilitySettingsObserver(
            this, 
            new Handler(Looper.getMainLooper())
        );
        accessibilitySettingsObserver.register();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (accessibilitySettingsObserver != null) {
            accessibilitySettingsObserver.unregister();
        }
    }
} 