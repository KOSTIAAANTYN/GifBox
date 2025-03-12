package com.example.GifBox.receiver;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import com.example.GifBox.service.GifBoxAccessibilityService;

public class AccessibilitySettingsObserver extends ContentObserver {
    private final Context context;
    private boolean wasEnabled;

    public AccessibilitySettingsObserver(Context context, Handler handler) {
        super(handler);
        this.context = context;
        this.wasEnabled = isAccessibilityServiceEnabled();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        
        boolean isEnabled = isAccessibilityServiceEnabled();

        if (!wasEnabled && isEnabled) {
            startAccessibilityService();
        }
        
        wasEnabled = isEnabled;
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (services != null) {
                return services.toLowerCase().contains(context.getPackageName().toLowerCase());
            }
        }
        return false;
    }

    private void startAccessibilityService() {
        try {
            Intent serviceIntent = new Intent(context, GifBoxAccessibilityService.class);
            serviceIntent.setAction("android.accessibilityservice.AccessibilityService");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
        }
    }

    public void register() {
        context.getContentResolver().registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            this
        );
    }

    public void unregister() {
        context.getContentResolver().unregisterContentObserver(this);
    }
} 