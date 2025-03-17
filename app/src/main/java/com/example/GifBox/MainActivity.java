package com.example.GifBox;


import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.GifBox.buttons.ContextButton;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.example.GifBox.databinding.ActivityMainBinding;
import com.example.GifBox.ui.home.HomeFragment;
import com.example.GifBox.ui.settings.SettingsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_PICK_FILES = 1;
    private RecyclerView recyclerView;
    private GifAdapter adapter;
    private List<File> mediaList = new ArrayList<>();
    private List<File> filteredMediaList = new ArrayList<>();
    private Handler mainHandler;
    
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    
    private static final String PREFS_NAME = "GifBoxPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeDefaultSettings();
        
        mainHandler = new Handler(Looper.getMainLooper());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new GifAdapter(this, filteredMediaList);
        recyclerView.setAdapter(adapter);

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view -> showBottomDialog());

        drawer = binding.drawerLayout;
        navigationView = binding.navView;

        drawerToggle = new ActionBarDrawerToggle(
            this, drawer, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
                
        setupNavigation();
        loadMedia();
        
        updateTextProcessingComponentState();
    }
    
    private void initializeDefaultSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(SettingsFragment.KEY_OVERLAY_ENABLED)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsFragment.KEY_OVERLAY_ENABLED, false);
            editor.putBoolean(SettingsFragment.KEY_CONTEXT_MENU_ENABLED, false);
            editor.putInt(SettingsFragment.KEY_OVERLAY_FUNCTION, SettingsFragment.FUNCTION_DIRECT_PROCESSING);
            editor.putInt(SettingsFragment.KEY_CONTEXT_MENU_FUNCTION, SettingsFragment.FUNCTION_MINI_SEARCH);
            editor.apply();
            
            updateTextProcessingComponentState();
        }
    }
    
    private void updateTextProcessingComponentState() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean contextMenuEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_CONTEXT_MENU_ENABLED, false);
        
        PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName(this, ContextButton.class);
        
        int newState = contextMenuEnabled 
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        pm.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        );
    }
    
    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, new HomeFragment())
                    .commit();
                binding.appBarMain.fab.show();
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else if (id == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, new SettingsFragment())
                    .commit();
                binding.appBarMain.fab.hide();
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
            }
            
            drawer.closeDrawers();
            return true;
        });
        
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, new HomeFragment())
            .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedia();
    }

    public boolean filterMedia(String query) {
        List<File> newFilteredList = new ArrayList<>();
        
        if (query == null || query.isEmpty()) {
            newFilteredList.addAll(mediaList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (File file : mediaList) {
                if (file.getName().toLowerCase().contains(lowerCaseQuery)) {
                    newFilteredList.add(file);
                }
            }
        }
        
        filteredMediaList.clear();
        filteredMediaList.addAll(newFilteredList);
        
        mainHandler.post(() -> {
            try {
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
            }
        });
        
        return !filteredMediaList.isEmpty();
    }

    private void loadMedia() {
        mediaList.clear();
        filteredMediaList.clear();
        
        File mediaDirectory = new File(getExternalFilesDir(null), "MyMedia");

        if (!mediaDirectory.exists()) {
            mediaDirectory.mkdirs();
        }

        if (mediaDirectory.exists()) {
            File[] files = mediaDirectory.listFiles();
            if (files != null) {
                mediaList.addAll(Arrays.asList(files));
                filteredMediaList.addAll(mediaList);
            }
        }

        adapter.notifyDataSetChanged();
    }


    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout fileLayout = dialog.findViewById(R.id.layoutGIF);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        fileLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"image/gif", "image/webp", "image/apng", "video/mp4", "video/webm"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQUEST_CODE_PICK_FILES);
        });

        cancelButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILES && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri fileUri = data.getClipData().getItemAt(i).getUri();
                    saveFileToAppFolder(fileUri);
                }
            } else if (data.getData() != null) {
                saveFileToAppFolder(data.getData());
            }
        }
    }

    private void saveFileToAppFolder(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File dir = new File(getExternalFilesDir(null), "MyMedia");
            if (!dir.exists()) dir.mkdirs();

            String fileName = getFileName(uri);
            File newFile = new File(dir, fileName);
            OutputStream outputStream = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            loadMedia();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return "file_" + System.currentTimeMillis();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (drawer.isDrawerOpen(navigationView)) {
            drawer.closeDrawer(navigationView);
        } else {
            drawer.openDrawer(navigationView);
        }
        return true;
    }
}

