package com.example.GifBox;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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