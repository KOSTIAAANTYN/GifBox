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
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.GifBox.buttons.ContextButton;
import com.example.GifBox.buttons.TranslateOption;
import com.example.GifBox.utils.FileUsageTracker;
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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.appcompat.app.AppCompatDelegate;
import android.content.res.Configuration;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private static final int REQUEST_CODE_PICK_FILES = 1;
    private RecyclerView recyclerView;
    private GifAdapter adapter;
    private List<File> mediaList = new ArrayList<>();
    private List<File> filteredMediaList = new ArrayList<>();
    private List<File> recentFiles = new ArrayList<>();
    private boolean showingRecentFiles = false;
    
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private static final String PREFS_NAME = "GifBoxPrefs";
    private static final String KEY_TEXT_PROCESSING_ENABLED = "text_processing_enabled";
    private static final String KEY_SHOW_RECENT_FILES = "show_recent_files";
    private static final String KEY_OPTIMIZED_MODE = "optimized_mode";
    private boolean optimizedMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeAndLanguage();
        
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeDefaultSettings();
        
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        optimizedMode = preferences.getBoolean(KEY_OPTIMIZED_MODE, false);

        recyclerView = findViewById(R.id.recyclerView);
        setupRecyclerViewLayout();

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
        setupSwipeRefresh();
        loadMedia();
        
        updateTextProcessingComponentState();
    }

    private void applyThemeAndLanguage() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int appTheme = sharedPreferences.getInt(SettingsFragment.KEY_APP_THEME, SettingsFragment.THEME_SYSTEM);
        int appLanguage = sharedPreferences.getInt(SettingsFragment.KEY_APP_LANGUAGE, SettingsFragment.LANGUAGE_ENGLISH);

        switch (appTheme) {
            case SettingsFragment.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsFragment.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SettingsFragment.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        Locale locale;
        switch (appLanguage) {
            case SettingsFragment.LANGUAGE_UKRAINIAN:
                locale = new Locale("uk");
                break;
            case SettingsFragment.LANGUAGE_ENGLISH:
            default:
                locale = new Locale("en");
                break;
        }
        
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    
    private void initializeDefaultSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(SettingsFragment.KEY_OVERLAY_ENABLED)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsFragment.KEY_OVERLAY_ENABLED, false);
            editor.putBoolean(SettingsFragment.KEY_CONTEXT_MENU_ENABLED, false);
            editor.putBoolean(SettingsFragment.KEY_TRANSLATE_ENABLED, false);
            editor.putInt(SettingsFragment.KEY_OVERLAY_FUNCTION, SettingsFragment.FUNCTION_DIRECT_PROCESSING);
            editor.putInt(SettingsFragment.KEY_CONTEXT_MENU_FUNCTION, SettingsFragment.FUNCTION_MINI_SEARCH);
            editor.putInt(SettingsFragment.KEY_TRANSLATE_FUNCTION, SettingsFragment.FUNCTION_MINI_SEARCH);
            editor.putBoolean(KEY_TEXT_PROCESSING_ENABLED, false);
            editor.putBoolean(KEY_SHOW_RECENT_FILES, false);
            editor.putBoolean(KEY_OPTIMIZED_MODE, false);
            editor.apply();
            
            updateTextProcessingComponentState();
        }
    }
    
    private void updateTextProcessingComponentState() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean contextMenuEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_CONTEXT_MENU_ENABLED, false);
        boolean translateEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_TRANSLATE_ENABLED, false);
        
        PackageManager pm = getPackageManager();
        ComponentName contextButtonComponent = new ComponentName(this, ContextButton.class);
        ComponentName translateButtonComponent = new ComponentName(this, TranslateOption.class);
        
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
    }
    
    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this);
        
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
        refreshMediaList();
    }

    public boolean filterMedia(String query) {
        filteredMediaList.clear();
        
        if (query.isEmpty()) {
            filteredMediaList.addAll(mediaList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (File file : mediaList) {
                if (file.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredMediaList.add(file);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment_content_main);
            if (fragment != null) {
                NavController navController = NavHostFragment.findNavController(fragment);
                NavDestination currentDestination = navController.getCurrentDestination();
                if (currentDestination != null && currentDestination.getId() == R.id.nav_home) {
                    activateHomeTab();
                }
            }
        } catch (Exception e) {
        }
        
        return !filteredMediaList.isEmpty();
    }

    private void loadMedia() {
        mediaList.clear();
        filteredMediaList.clear();
        
                SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean showRecentFiles = preferences.getBoolean("show_recent_files", false);
        
        if (showRecentFiles) {
                        recentFiles = FileUsageTracker.getRecentFiles(this, 20);             
            if (recentFiles.isEmpty()) {
                                showNoRecentFilesMessage(true);
            } else {
                                showNoRecentFilesMessage(false);
                
                                filteredMediaList.addAll(recentFiles);
                
                                sortMediaList(filteredMediaList);
                
                                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        } else {
                        showNoRecentFilesMessage(false);
            
                        loadAllMedia();
        }
    }
    
    private void loadAllMedia() {
        mediaList.clear();
        filteredMediaList.clear();
        
                File mediaDir = getMediaDirectory();
        if (mediaDir == null || !mediaDir.exists() || !mediaDir.isDirectory()) {
            return;
        }
        
                File[] files = mediaDir.listFiles();
        if (files == null) {
            return;
        }
        
                for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".gif") || name.endsWith(".mp4") || 
                name.endsWith(".webm") || name.endsWith(".3gp")) {
                mediaList.add(file);
            }
        }
        
                filteredMediaList.addAll(mediaList);
        
                sortMediaList(filteredMediaList);
        
                if (adapter != null) {
        adapter.notifyDataSetChanged();
        }
    }
    
    private void sortMediaList(List<File> files) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int sortType = preferences.getInt("sort_type", FileUsageTracker.SORT_NONE);
        
                List<FileUsageTracker.FileUsageInfo> fileInfoList = FileUsageTracker.getRecentFilesInfo(this, files);
        
                FileUsageTracker.sortFileInfoList(fileInfoList, sortType);
        
                files.clear();
        for (FileUsageTracker.FileUsageInfo info : fileInfoList) {
            files.add(new File(info.getFilePath()));
        }
    }
    
    private void showNoRecentFilesMessage(boolean show) {
                TextView noRecentFilesTextView = findViewById(R.id.noRecentFilesTextView);
        
        if (noRecentFilesTextView != null) {
            noRecentFilesTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
        private void toggleRecentFilesMode(boolean showRecentFiles) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("show_recent_files", showRecentFiles);
        editor.apply();
        
                loadMedia();
    }
    
        private void setSortType(int sortType) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("sort_type", sortType);
        editor.apply();
        
                refreshMediaList();
        
                invalidateOptionsMenu();
    }
    
        private int getSortType() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getInt("sort_type", FileUsageTracker.SORT_NONE);
    }
    
    public void refreshMediaList() {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int scrollPosition = 0;
        
        if (layoutManager != null) {
            if (layoutManager instanceof GridLayoutManager) {
                scrollPosition = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] positions = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(positions);
                scrollPosition = positions[0];
            }
        }
        
        loadMedia();
        
        if (adapter != null) {
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean showFilenames = preferences.getBoolean("show_filenames", false);
            boolean showExtensionIcon = preferences.getBoolean("show_extension_icon", false);
            optimizedMode = preferences.getBoolean(KEY_OPTIMIZED_MODE, false);
            
            adapter.setShowFilenames(showFilenames);
            adapter.setShowExtensionIcon(showExtensionIcon);
            adapter.setOptimizedMode(optimizedMode);
            adapter.notifyDataSetChanged();
            
            if (layoutManager != null && scrollPosition >= 0 && scrollPosition < adapter.getItemCount()) {
                layoutManager.scrollToPosition(scrollPosition);
            }
        }
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
        
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean showFilenames = preferences.getBoolean("show_filenames", false);
        boolean showExtensionIcon = preferences.getBoolean("show_extension_icon", false);
        boolean useFlexibleGrid = preferences.getBoolean("use_flexible_grid", false);
        boolean showRecentFiles = preferences.getBoolean("show_recent_files", false);
        boolean optimizedMode = preferences.getBoolean(KEY_OPTIMIZED_MODE, false);
        int sortType = preferences.getInt("sort_type", FileUsageTracker.SORT_NONE);
        
        MenuItem filenamesItem = menu.findItem(R.id.action_show_filenames);
        MenuItem extensionIconItem = menu.findItem(R.id.action_show_extension_icon);
        MenuItem flexibleGridItem = menu.findItem(R.id.action_flexible_grid);
        MenuItem recentFilesItem = menu.findItem(R.id.action_show_recent_files);
        MenuItem optimizedModeItem = menu.findItem(R.id.action_optimized_mode);
        
        filenamesItem.setChecked(showFilenames);
        extensionIconItem.setChecked(showExtensionIcon);
        flexibleGridItem.setChecked(useFlexibleGrid);
        recentFilesItem.setChecked(showRecentFiles);
        optimizedModeItem.setChecked(optimizedMode);
        
        updateSortMenuCheckedState(menu, sortType);
        
        return true;
    }

    private void updateSortMenuCheckedState(Menu menu, int sortType) {
                MenuItem sortSettingsItem = menu.findItem(R.id.action_sort_settings);
        if (sortSettingsItem == null || sortSettingsItem.getSubMenu() == null) {
            return;
        }
        
        Menu sortMenu = sortSettingsItem.getSubMenu();
        
                sortMenu.findItem(R.id.action_sort_by_most_used).setChecked(false);
        sortMenu.findItem(R.id.action_sort_by_date).setChecked(false);
        sortMenu.findItem(R.id.action_sort_alphabetically).setChecked(false);
        sortMenu.findItem(R.id.action_sort_by_last_used).setChecked(false);
        sortMenu.findItem(R.id.action_no_sort).setChecked(false);
        
                switch (sortType) {
            case FileUsageTracker.SORT_MOST_USED:
                sortMenu.findItem(R.id.action_sort_by_most_used).setChecked(true);
                break;
            case FileUsageTracker.SORT_CREATION_DATE:
                sortMenu.findItem(R.id.action_sort_by_date).setChecked(true);
                break;
            case FileUsageTracker.SORT_ALPHABETICAL:
                sortMenu.findItem(R.id.action_sort_alphabetically).setChecked(true);
                break;
            case FileUsageTracker.SORT_LAST_USED:
                sortMenu.findItem(R.id.action_sort_by_last_used).setChecked(true);
                break;
            case FileUsageTracker.SORT_NONE:
            default:
                sortMenu.findItem(R.id.action_no_sort).setChecked(true);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        
        if (itemId == R.id.action_show_filenames) {
            boolean showFilenames = !item.isChecked();
            item.setChecked(showFilenames);
            editor.putBoolean("show_filenames", showFilenames);
            editor.apply();
            refreshMediaList();
            return true;
        } 
        else if (itemId == R.id.action_show_extension_icon) {
            boolean showExtensionIcon = !item.isChecked();
            item.setChecked(showExtensionIcon);
            editor.putBoolean("show_extension_icon", showExtensionIcon);
            editor.apply();
            refreshMediaList();
            return true;
        }
        else if (itemId == R.id.action_flexible_grid) {
            boolean useFlexibleGrid = !item.isChecked();
            item.setChecked(useFlexibleGrid);
            editor.putBoolean("use_flexible_grid", useFlexibleGrid);
            editor.apply();
            
            setupRecyclerViewLayout();
            return true;
        }
        else if (itemId == R.id.action_show_recent_files) {
            boolean showRecentFiles = !item.isChecked();
            item.setChecked(showRecentFiles);
            toggleRecentFilesMode(showRecentFiles);
            return true;
        }
        else if (itemId == R.id.action_optimized_mode) {
            optimizedMode = !item.isChecked();
            item.setChecked(optimizedMode);
            editor.putBoolean(KEY_OPTIMIZED_MODE, optimizedMode);
            editor.apply();
            refreshMediaList();
            return true;
        }
        else if (itemId == R.id.action_sort_by_most_used) {
            item.setChecked(true);
            setSortType(FileUsageTracker.SORT_MOST_USED);
            return true;
        }
        else if (itemId == R.id.action_sort_by_date) {
            item.setChecked(true);
            setSortType(FileUsageTracker.SORT_CREATION_DATE);
            return true;
        }
        else if (itemId == R.id.action_sort_alphabetically) {
            item.setChecked(true);
            setSortType(FileUsageTracker.SORT_ALPHABETICAL);
            return true;
        }
        else if (itemId == R.id.action_sort_by_last_used) {
            item.setChecked(true);
            setSortType(FileUsageTracker.SORT_LAST_USED);
            return true;
        }
        else if (itemId == R.id.action_no_sort) {
            item.setChecked(true);
            setSortType(FileUsageTracker.SORT_NONE);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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

    public void activateHomeTab() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            performActivateHomeTab();
        } else {
            runOnUiThread(this::performActivateHomeTab);
        }
    }
    
    private void performActivateHomeTab() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (!(currentFragment instanceof HomeFragment)) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, new HomeFragment())
                .commit();
            
            binding.appBarMain.fab.show();
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            
            navigationView.setCheckedItem(R.id.nav_home);
        }
        
        refreshMediaList();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshMediaList();
            swipeRefreshLayout.setRefreshing(false);
        });
        
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_200,
            R.color.purple_700
        );
    }

    private void setupRecyclerViewLayout() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean useFlexibleGrid = preferences.getBoolean("use_flexible_grid", false);
        
        if (useFlexibleGrid) {
                        androidx.recyclerview.widget.StaggeredGridLayoutManager layoutManager = 
                new com.example.GifBox.GridLayoutManager.FlexibleGridLayoutManager(this, 3);
            recyclerView.setLayoutManager(layoutManager);
            adapter = new GifAdapter(this, filteredMediaList, true);
        } else {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
            adapter = new GifAdapter(this, filteredMediaList, false);
        }
    }

    private File getMediaDirectory() {
        File mediaDirectory = new File(getExternalFilesDir(null), "MyMedia");
        
        if (!mediaDirectory.exists()) {
            mediaDirectory.mkdirs();
        }
        
        return mediaDirectory;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
    }
}

