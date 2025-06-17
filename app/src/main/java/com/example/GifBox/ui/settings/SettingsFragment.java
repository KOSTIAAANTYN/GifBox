package com.example.GifBox.ui.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.GifBox.R;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "GifBoxPrefs";
    public static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    public static final String KEY_CONTEXT_MENU_ENABLED = "context_menu_enabled";
    public static final String KEY_TRANSLATE_ENABLED = "translate_enabled";
    public static final String KEY_OVERLAY_FUNCTION = "overlay_function";
    public static final String KEY_CONTEXT_MENU_FUNCTION = "context_menu_function";
    public static final String KEY_TRANSLATE_FUNCTION = "translate_function";
    public static final String KEY_APP_THEME = "app_theme";
    public static final String KEY_APP_LANGUAGE = "app_language";
    

    public static final int FUNCTION_DIRECT_PROCESSING = 0;
    public static final int FUNCTION_MINI_SEARCH = 1;

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final int LANGUAGE_ENGLISH = 0;
    public static final int LANGUAGE_UKRAINIAN = 1;

    private SwitchCompat switchOverlay;
    private SwitchCompat switchContextMenu;
    private SwitchCompat switchTranslate;
    private Spinner spinnerOverlayFunction;
    private Spinner spinnerContextMenuFunction;
    private Spinner spinnerTranslateFunction;
    private Spinner spinnerTheme;
    private Spinner spinnerLanguage;
    private Button buttonAccessibility;
    private Button buttonSave;
    private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        switchOverlay = root.findViewById(R.id.switchOverlay);
        switchContextMenu = root.findViewById(R.id.switchContextMenu);
        switchTranslate = root.findViewById(R.id.switchTranslate);
        spinnerOverlayFunction = root.findViewById(R.id.spinnerOverlayFunction);
        spinnerContextMenuFunction = root.findViewById(R.id.spinnerContextMenuFunction);
        spinnerTranslateFunction = root.findViewById(R.id.spinnerTranslateFunction);
        spinnerTheme = root.findViewById(R.id.spinnerTheme);
        spinnerLanguage = root.findViewById(R.id.spinnerLanguage);
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
        
        ArrayAdapter<CharSequence> translateFunctionAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.button_functions,
            android.R.layout.simple_spinner_item
        );
        translateFunctionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTranslateFunction.setAdapter(translateFunctionAdapter);

        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.theme_options,
            android.R.layout.simple_spinner_item
        );
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);

        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.language_options,
            android.R.layout.simple_spinner_item
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);
    }

    private void loadSettings() {
        boolean overlayEnabled = sharedPreferences.getBoolean(KEY_OVERLAY_ENABLED, false);
        boolean contextMenuEnabled = sharedPreferences.getBoolean(KEY_CONTEXT_MENU_ENABLED, false);
        boolean translateEnabled = sharedPreferences.getBoolean(KEY_TRANSLATE_ENABLED, false);
        int overlayFunction = sharedPreferences.getInt(KEY_OVERLAY_FUNCTION, FUNCTION_DIRECT_PROCESSING);
        int contextMenuFunction = sharedPreferences.getInt(KEY_CONTEXT_MENU_FUNCTION, FUNCTION_MINI_SEARCH);
        int translateFunction = sharedPreferences.getInt(KEY_TRANSLATE_FUNCTION, FUNCTION_MINI_SEARCH);
        int appTheme = sharedPreferences.getInt(KEY_APP_THEME, THEME_SYSTEM);
        int appLanguage = sharedPreferences.getInt(KEY_APP_LANGUAGE, LANGUAGE_ENGLISH);

        switchOverlay.setChecked(overlayEnabled);
        switchContextMenu.setChecked(contextMenuEnabled);
        switchTranslate.setChecked(translateEnabled);
        spinnerOverlayFunction.setSelection(overlayFunction);
        spinnerContextMenuFunction.setSelection(contextMenuFunction);
        spinnerTranslateFunction.setSelection(translateFunction);
        spinnerTheme.setSelection(appTheme);
        spinnerLanguage.setSelection(appLanguage);
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
        boolean translateEnabled = switchTranslate.isChecked();
        int overlayFunction = spinnerOverlayFunction.getSelectedItemPosition();
        int contextMenuFunction = spinnerContextMenuFunction.getSelectedItemPosition();
        int translateFunction = spinnerTranslateFunction.getSelectedItemPosition();
        int appTheme = spinnerTheme.getSelectedItemPosition();
        int appLanguage = spinnerLanguage.getSelectedItemPosition();

        int oldAppTheme = sharedPreferences.getInt(KEY_APP_THEME, THEME_SYSTEM);
        int oldAppLanguage = sharedPreferences.getInt(KEY_APP_LANGUAGE, LANGUAGE_ENGLISH);
        boolean themeChanged = oldAppTheme != appTheme;
        boolean languageChanged = oldAppLanguage != appLanguage;
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_OVERLAY_ENABLED, overlayEnabled);
        editor.putBoolean(KEY_CONTEXT_MENU_ENABLED, contextMenuEnabled);
        editor.putBoolean(KEY_TRANSLATE_ENABLED, translateEnabled);
        editor.putInt(KEY_OVERLAY_FUNCTION, overlayFunction);
        editor.putInt(KEY_CONTEXT_MENU_FUNCTION, contextMenuFunction);
        editor.putInt(KEY_TRANSLATE_FUNCTION, translateFunction);
        editor.putInt(KEY_APP_THEME, appTheme);
        editor.putInt(KEY_APP_LANGUAGE, appLanguage);
        editor.apply();

        PackageManager pm = requireActivity().getPackageManager();
        ComponentName contextButtonComponent = new ComponentName(requireContext(), "com.example.GifBox.buttons.ContextButton");
        ComponentName translateButtonComponent = new ComponentName(requireContext(), "com.example.GifBox.buttons.TranslateOption");
        
        int contextMenuState = contextMenuEnabled 
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        int translateState = translateEnabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(
            contextButtonComponent,
            contextMenuState,
            PackageManager.DONT_KILL_APP
        );
        
        pm.setComponentEnabledSetting(
            translateButtonComponent,
            translateState,
            PackageManager.DONT_KILL_APP
        );

        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();

        if (themeChanged) {
            applyTheme(appTheme);
        }
        
        if (languageChanged) {
            applyLanguage(appLanguage);

            requireActivity().recreate();
        }
    }

    private void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void applyLanguage(int languageCode) {
        Locale locale;
        switch (languageCode) {
            case LANGUAGE_UKRAINIAN:
                locale = new Locale("uk");
                break;
            case LANGUAGE_ENGLISH:
            default:
                locale = new Locale("en");
                break;
        }
        
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        requireActivity().getResources().updateConfiguration(config, 
                requireActivity().getResources().getDisplayMetrics());
    }
} 