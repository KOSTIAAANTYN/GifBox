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

public class TextProcessingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        
        if (text != null) {
            String searchText = text.toString().trim();
            File storageDir = new File(getExternalFilesDir(null), "MyMedia");
            
            if (!storageDir.exists()) {
                Toast.makeText(this, "Media directory not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // log
            File[] allFiles = storageDir.listFiles();
            if (allFiles != null) {
                for (File f : allFiles) {
                    android.util.Log.d("GifBox", "Found file: " + f.getName());
                }
            }

            //Is video?
            boolean isVideoSearch = searchText.toLowerCase().endsWith(".v");
            final String finalSearchText = isVideoSearch ? 
                searchText.substring(0, searchText.length() - 2) : searchText;

            android.util.Log.d("GifBox", "Search text: " + finalSearchText + ", isVideoSearch: " + isVideoSearch);

            List<File> gifFiles = new ArrayList<>();
            List<File> videoFiles = new ArrayList<>();
            
            File[] matchingFiles = storageDir.listFiles((dir, name) -> {
                android.util.Log.d("GifBox", "Checking file: " + name + " against search: " + finalSearchText);
                String lowercaseName = name.toLowerCase();
                String searchLower = finalSearchText.toLowerCase();
                
                if (lowercaseName.startsWith(searchLower + ".")) {
                    if (lowercaseName.endsWith(".gif")) {
                        gifFiles.add(new File(dir, name));
                    } else if (lowercaseName.endsWith(".mp4") || lowercaseName.endsWith(".webm")) {
                        videoFiles.add(new File(dir, name));
                    }
                    return true;
                }
                return false;
            });

            if (matchingFiles != null && matchingFiles.length > 0) {
                File fileToUse;
                
                if (isVideoSearch && !videoFiles.isEmpty()) {
                    // if search vid
                    fileToUse = videoFiles.get(0);
                    shareFile(fileToUse);
                } else if (!gifFiles.isEmpty()) {
                    // if Gif
                    fileToUse = gifFiles.get(0);
                    copyGifToClipboard(fileToUse);
                } else if (!videoFiles.isEmpty()) {
                    // No Gif gave vid
                    fileToUse = videoFiles.get(0);
                    shareFile(fileToUse);
                } else {
                    fileToUse = matchingFiles[0];
                    shareFile(fileToUse);
                }
            } else {
                Toast.makeText(this, "No matching media found for: " + finalSearchText + " in " + storageDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        }
        
        finish();
    }
    
    private void copyGifToClipboard(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                file);

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "GIF", contentUri);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "GIF copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error copying GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareFile(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                file);
                
            String mimeType;
            if (file.getName().endsWith(".mp4")) {
                mimeType = "video/mp4";
            } else if (file.getName().endsWith(".webm")) {
                mimeType = "video/webm";
            } else {
                mimeType = "*/*";
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 