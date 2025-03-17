package com.example.GifBox.buttons;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.GifBox.functions.TextProcessing;
import com.example.GifBox.ui.settings.SettingsFragment;

public class ContextButton extends AppCompatActivity {
    private static final String TAG = "ContextButton";
    
    private static final String PREFS_NAME = "GifBoxPrefs";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean contextMenuEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_CONTEXT_MENU_ENABLED, false);
                if (!contextMenuEnabled) {
            finish();
            return;
        }
        
        int contextMenuFunction = sharedPreferences.getInt(SettingsFragment.KEY_CONTEXT_MENU_FUNCTION, SettingsFragment.FUNCTION_MINI_SEARCH);
        
        Intent intent = getIntent();
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        
        if (text != null) {
            if (contextMenuFunction == SettingsFragment.FUNCTION_DIRECT_PROCESSING) {
                TextProcessing.processText(this, text.toString());
            } else if (contextMenuFunction == SettingsFragment.FUNCTION_MINI_SEARCH) {
                TextProcessing.openSearchDialog(this, text.toString());
            }
        }
        
        finish();
    }
} 