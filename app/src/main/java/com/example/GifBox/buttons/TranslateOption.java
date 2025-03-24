package com.example.GifBox.buttons;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.GifBox.functions.TextProcessing;
import com.example.GifBox.ui.settings.SettingsFragment;

public class TranslateOption extends AppCompatActivity {
    private static final String TAG = "TranslateOption";
    
    private static final String PREFS_NAME = "GifBoxPrefs";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean translateEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_TRANSLATE_ENABLED, false);
        
        if (!translateEnabled) {
            finish();
            return;
        }
        
        int translateFunction = sharedPreferences.getInt(SettingsFragment.KEY_TRANSLATE_FUNCTION, SettingsFragment.FUNCTION_MINI_SEARCH);
        
        Intent intent = getIntent();
        
        CharSequence text = null;
        if (intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        }
        
        if (text != null) {
            if (translateFunction == SettingsFragment.FUNCTION_DIRECT_PROCESSING) {
                TextProcessing.processText(this, text.toString());
            } else if (translateFunction == SettingsFragment.FUNCTION_MINI_SEARCH) {
                TextProcessing.openSearchDialog(this, text.toString());
            }
        }
        
        finish();
    }
} 