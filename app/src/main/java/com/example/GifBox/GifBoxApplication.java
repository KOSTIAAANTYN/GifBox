package com.example.GifBox;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.example.GifBox.receiver.AccessibilitySettingsObserver;

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