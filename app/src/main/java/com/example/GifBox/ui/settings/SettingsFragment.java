package com.example.GifBox.ui.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.GifBox.R;
import com.example.GifBox.TextProcessingActivity;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "GifBoxPrefs";
    private static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    private static final String KEY_TEXT_PROCESSING_ENABLED = "text_processing_enabled";

    private SwitchCompat switchOverlay;
    private SwitchCompat switchTextProcessing;
    private Button buttonAccessibility;
    private Button buttonSave;
    private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        switchOverlay = root.findViewById(R.id.switchOverlay);
        switchTextProcessing = root.findViewById(R.id.switchTextProcessing);
        buttonAccessibility = root.findViewById(R.id.buttonAccessibility);
        buttonSave = root.findViewById(R.id.buttonSave);

        loadSettings();
        setupListeners();

        return root;
    }

    private void loadSettings() {
        boolean overlayEnabled = sharedPreferences.getBoolean(KEY_OVERLAY_ENABLED, true);
        boolean textProcessingEnabled = sharedPreferences.getBoolean(KEY_TEXT_PROCESSING_ENABLED, true);

        switchOverlay.setChecked(overlayEnabled);
        switchTextProcessing.setChecked(textProcessingEnabled);
    }

    private void setupListeners() {
        buttonAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        
        buttonSave.setOnClickListener(v -> saveSettings());
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void saveSettings() {
        boolean overlayEnabled = switchOverlay.isChecked();
        boolean textProcessingEnabled = switchTextProcessing.isChecked();
        
        boolean previousTextProcessingEnabled = sharedPreferences.getBoolean(KEY_TEXT_PROCESSING_ENABLED, true);
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_OVERLAY_ENABLED, overlayEnabled);
        editor.putBoolean(KEY_TEXT_PROCESSING_ENABLED, textProcessingEnabled);
        editor.apply();
        
        if (previousTextProcessingEnabled != textProcessingEnabled) {
            updateTextProcessingComponent(textProcessingEnabled);
        }

        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
    
    private void updateTextProcessingComponent(boolean enabled) {
        PackageManager pm = requireActivity().getPackageManager();
        ComponentName componentName = new ComponentName(requireActivity(), TextProcessingActivity.class);
        
        int newState = enabled 
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        );
    }
} 