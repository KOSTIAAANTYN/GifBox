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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.GifBox.R;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "GifBoxPrefs";
    public static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    public static final String KEY_CONTEXT_MENU_ENABLED = "context_menu_enabled";
    public static final String KEY_OVERLAY_FUNCTION = "overlay_function";
    public static final String KEY_CONTEXT_MENU_FUNCTION = "context_menu_function";
    

    public static final int FUNCTION_DIRECT_PROCESSING = 0;
    public static final int FUNCTION_MINI_SEARCH = 1;

    private SwitchCompat switchOverlay;
    private SwitchCompat switchContextMenu;
    private Spinner spinnerOverlayFunction;
    private Spinner spinnerContextMenuFunction;
    private Button buttonAccessibility;
    private Button buttonSave;
    private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        switchOverlay = root.findViewById(R.id.switchOverlay);
        switchContextMenu = root.findViewById(R.id.switchContextMenu);
        spinnerOverlayFunction = root.findViewById(R.id.spinnerOverlayFunction);
        spinnerContextMenuFunction = root.findViewById(R.id.spinnerContextMenuFunction);
        buttonAccessibility = root.findViewById(R.id.buttonAccessibility);
        buttonSave = root.findViewById(R.id.buttonSave);

        setupSpinners();
        loadSettings();
        setupListeners();

        return root;
    }
    
    private void setupSpinners() {        
        ArrayAdapter<CharSequence> functionAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.button_functions,
            android.R.layout.simple_spinner_item
        );
        functionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOverlayFunction.setAdapter(functionAdapter);
        
        ArrayAdapter<CharSequence> contextMenuFunctionAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.button_functions,
            android.R.layout.simple_spinner_item
        );
        contextMenuFunctionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContextMenuFunction.setAdapter(contextMenuFunctionAdapter);
    }

    private void loadSettings() {
        boolean overlayEnabled = sharedPreferences.getBoolean(KEY_OVERLAY_ENABLED, false);
        boolean contextMenuEnabled = sharedPreferences.getBoolean(KEY_CONTEXT_MENU_ENABLED, false);
        int overlayFunction = sharedPreferences.getInt(KEY_OVERLAY_FUNCTION, FUNCTION_DIRECT_PROCESSING);
        int contextMenuFunction = sharedPreferences.getInt(KEY_CONTEXT_MENU_FUNCTION, FUNCTION_MINI_SEARCH);

        switchOverlay.setChecked(overlayEnabled);
        switchContextMenu.setChecked(contextMenuEnabled);
        spinnerOverlayFunction.setSelection(overlayFunction);
        spinnerContextMenuFunction.setSelection(contextMenuFunction);
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
        boolean contextMenuEnabled = switchContextMenu.isChecked();
        int overlayFunction = spinnerOverlayFunction.getSelectedItemPosition();
        int contextMenuFunction = spinnerContextMenuFunction.getSelectedItemPosition();
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_OVERLAY_ENABLED, overlayEnabled);
        editor.putBoolean(KEY_CONTEXT_MENU_ENABLED, contextMenuEnabled);
        editor.putInt(KEY_OVERLAY_FUNCTION, overlayFunction);
        editor.putInt(KEY_CONTEXT_MENU_FUNCTION, contextMenuFunction);
        editor.apply();

        PackageManager pm = requireActivity().getPackageManager();
        ComponentName contextButtonComponent = new ComponentName(requireContext(), "com.example.GifBox.buttons.ContextButton");
        
        int newState = contextMenuEnabled 
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(
            contextButtonComponent,
            newState,
            PackageManager.DONT_KILL_APP
        );

        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
} 