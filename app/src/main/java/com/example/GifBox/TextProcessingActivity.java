package com.example.GifBox;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.example.GifBox.utils.TextProcessingUtils;

public class TextProcessingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        
        if (text != null) {
            TextProcessingUtils.processText(this, text.toString());
        }
        
        finish();
    }
} 