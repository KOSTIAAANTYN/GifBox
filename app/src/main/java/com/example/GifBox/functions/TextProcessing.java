package com.example.GifBox.functions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TextProcessing {
    
    private static final String TAG = "TextProcessing";
    
    /**
     * Process text and find matching media files
     * @param context Application context
     * @param searchText Text to search for
     * @return true if processing was successful, false otherwise
     */
    public static boolean processText(Context context, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return false;
        }
        
        searchText = searchText.trim();
        File storageDir = new File(context.getExternalFilesDir(null), "MyMedia");
        
        if (!storageDir.exists()) {
            Toast.makeText(context, "Media directory not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Is video?
        boolean isVideoSearch = searchText.toLowerCase().endsWith(".v");
        final String finalSearchText = isVideoSearch ? 
            searchText.substring(0, searchText.length() - 2) : searchText;

        List<File> gifFiles = new ArrayList<>();
        List<File> videoFiles = new ArrayList<>();
        
        File[] matchingFiles = storageDir.listFiles((dir, name) -> {
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
                shareFile(context, fileToUse);
            } else if (!gifFiles.isEmpty()) {
                // if Gif
                fileToUse = gifFiles.get(0);
                copyGifToClipboard(context, fileToUse);
            } else if (!videoFiles.isEmpty()) {
                // No Gif gave vid
                fileToUse = videoFiles.get(0);
                shareFile(context, fileToUse);
            } else {
                fileToUse = matchingFiles[0];
                shareFile(context, fileToUse);
            }
            return true;
        } else {
            Toast.makeText(context, "No matching media found for: " + finalSearchText, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Copy GIF file to clipboard
     * @param context Application context
     * @param file GIF file to copy
     */
    public static void copyGifToClipboard(Context context, File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider",
                file);

            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(context.getContentResolver(), "GIF", contentUri);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(context, "GIF copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error copying GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Share file using system share dialog
     * @param context Application context
     * @param file File to share
     */
    public static void shareFile(Context context, File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".provider",
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

            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(Intent.createChooser(shareIntent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open search dialog with pre-filled text
     * @param context Application context
     * @param searchText Text to search for
     */
    public static void openSearchDialog(Context context, String searchText) {
        try {
            Intent intent = new Intent(context, MiniSearchActivity.class);
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, searchText);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error opening search: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            processText(context, searchText);
        }
    }
} 