package com.example.GifBox.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.example.GifBox.R;
import com.example.GifBox.service.GifBoxAccessibilityService;

/**
 * Receiver for starting the accessibility service when the device is booted
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || 
             intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))) {
            
            if (isAccessibilityServiceEnabled(context)) {
                Toast.makeText(context, context.getString(R.string.service_will_be_activated), Toast.LENGTH_SHORT).show();
            
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
        }
    }
    
    /**
     * Checks if the accessibility service is enabled
     * @param context The application context
     * @return true if the service is enabled
     */
    private boolean isAccessibilityServiceEnabled(Context context) {
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
} 