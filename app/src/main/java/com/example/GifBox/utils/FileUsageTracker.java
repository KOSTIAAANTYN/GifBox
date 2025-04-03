package com.example.GifBox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FileUsageTracker {
    private static final String TAG = "FileUsageTracker";
    private static final String PREFS_NAME = "GifBoxUsagePrefs";
    private static final String KEY_USAGE_COUNT = "usage_count_";
    private static final String KEY_LAST_USED = "last_used_";
    private static final String KEY_RECENT_FILES = "recent_files";
    private static final int MAX_RECENT_FILES = 20;
    
    private static final String SORT_TYPE_KEY = "sort_type";
    
    public static final int SORT_MOST_USED = 0; 
    public static final int SORT_CREATION_DATE = 1; 
    public static final int SORT_ALPHABETICAL = 2; 
    public static final int SORT_LAST_USED = 3; 
    public static final int SORT_NONE = 4; 
    
    
    public static final int SORT_BY_USAGE = SORT_MOST_USED;
    public static final int SORT_BY_DATE_CREATED = SORT_CREATION_DATE;
    public static final int SORT_BY_ALPHABETICAL = SORT_ALPHABETICAL;
    public static final int SORT_BY_LAST_USED = SORT_LAST_USED;
    public static final int SORT_DEFAULT = SORT_NONE;
    
        public static void trackFileUsage(Context context, File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        String filePath = file.getAbsolutePath();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        
        int currentCount = prefs.getInt(KEY_USAGE_COUNT + filePath, 0);
        editor.putInt(KEY_USAGE_COUNT + filePath, currentCount + 1);
        
        
        long currentTime = System.currentTimeMillis();
        editor.putLong(KEY_LAST_USED + filePath, currentTime);
        
        
        Set<String> recentFiles = new HashSet<>(prefs.getStringSet(KEY_RECENT_FILES, new HashSet<>()));
        recentFiles.add(filePath);
        
        
        if (recentFiles.size() > MAX_RECENT_FILES) {
            List<FileUsageInfo> fileInfoList = getRecentFilesInfo(context);
            
            Collections.sort(fileInfoList, (a, b) -> Long.compare(a.getLastUsed(), b.getLastUsed()));
            
            
            while (fileInfoList.size() > MAX_RECENT_FILES) {
                FileUsageInfo oldestFile = fileInfoList.remove(0);
                recentFiles.remove(oldestFile.getFilePath());
            }
        }
        
        editor.putStringSet(KEY_RECENT_FILES, recentFiles);
        editor.apply();
    }
    
        public static List<File> getRecentFiles(Context context, int limit) {
        if (limit <= 0) {
            limit = MAX_RECENT_FILES;
        }
        
        List<FileUsageInfo> fileInfoList = getRecentFilesInfo(context);
        
        
        int sortType = getSortType(context);
        
        
        sortFileInfoList(fileInfoList, sortType);
        
        
        if (fileInfoList.size() > limit) {
            fileInfoList = fileInfoList.subList(0, limit);
        }
        
        List<File> files = new ArrayList<>();
        for (FileUsageInfo info : fileInfoList) {
            File file = new File(info.getFilePath());
            if (file.exists()) {
                files.add(file);
            }
        }
        
        return files;
    }
    
        public static List<File> getRecentFiles(Context context) {
        return getRecentFiles(context, MAX_RECENT_FILES);
    }
    
        public static List<FileUsageInfo> getRecentFilesInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentFiles = prefs.getStringSet(KEY_RECENT_FILES, new HashSet<>());
        
        List<FileUsageInfo> fileInfoList = new ArrayList<>();
        
        for (String filePath : recentFiles) {
            File file = new File(filePath);
            if (file.exists()) {
                int usageCount = prefs.getInt(KEY_USAGE_COUNT + filePath, 0);
                long lastUsed = prefs.getLong(KEY_LAST_USED + filePath, 0);
                
                fileInfoList.add(new FileUsageInfo(filePath, usageCount, lastUsed, file.lastModified()));
            }
        }
        
        return fileInfoList;
    }
    
        public static List<FileUsageInfo> getRecentFilesInfo(Context context, List<File> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<FileUsageInfo> fileInfoList = new ArrayList<>();
        
        for (File file : files) {
            if (file.exists()) {
                String filePath = file.getAbsolutePath();
                int usageCount = prefs.getInt(KEY_USAGE_COUNT + filePath, 0);
                long lastUsed = prefs.getLong(KEY_LAST_USED + filePath, 0);
                
                fileInfoList.add(new FileUsageInfo(filePath, usageCount, lastUsed, file.lastModified()));
            }
        }
        
        return fileInfoList;
    }
    
        public static void sortFileInfoList(List<FileUsageInfo> fileInfoList, int sortType) {
        switch (sortType) {
            case SORT_MOST_USED:
                
                Collections.sort(fileInfoList, (a, b) -> Integer.compare(b.getUsageCount(), a.getUsageCount()));
                break;
            case SORT_CREATION_DATE:
                
                Collections.sort(fileInfoList, (a, b) -> Long.compare(b.getCreationDate(), a.getCreationDate()));
                break;
            case SORT_ALPHABETICAL:
                
                Collections.sort(fileInfoList, (a, b) -> {
                    String nameA = new File(a.getFilePath()).getName();
                    String nameB = new File(b.getFilePath()).getName();
                    return nameA.compareToIgnoreCase(nameB);
                });
                break;
            case SORT_LAST_USED:
                
                Collections.sort(fileInfoList, (a, b) -> Long.compare(b.getLastUsed(), a.getLastUsed()));
                break;
            case SORT_NONE:
            default:
                
                break;
        }
    }
    
        public static int getSortType(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(SORT_TYPE_KEY, SORT_LAST_USED); 
    }
    
        public static void setSortType(Context context, int sortType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SORT_TYPE_KEY, sortType);
        editor.apply();
    }
    
        public static List<File> sortFiles(Context context, List<File> files) {
        
        Map<String, FileUsageInfo> usageMap = new HashMap<>();
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int sortType = prefs.getInt(SORT_TYPE_KEY, SORT_LAST_USED);
        
        
        for (File file : files) {
            String filePath = file.getAbsolutePath();
            int usageCount = prefs.getInt(KEY_USAGE_COUNT + filePath, 0);
            long lastUsed = prefs.getLong(KEY_LAST_USED + filePath, 0);
            usageMap.put(filePath, new FileUsageInfo(filePath, usageCount, lastUsed, file.lastModified()));
        }
        
        
        List<File> sortedFiles = new ArrayList<>(files);
        
        
        switch (sortType) {
            case SORT_MOST_USED:
                Collections.sort(sortedFiles, (a, b) -> {
                    int usageA = usageMap.containsKey(a.getAbsolutePath()) ? 
                                 usageMap.get(a.getAbsolutePath()).getUsageCount() : 0;
                    int usageB = usageMap.containsKey(b.getAbsolutePath()) ? 
                                 usageMap.get(b.getAbsolutePath()).getUsageCount() : 0;
                    return Integer.compare(usageB, usageA); 
                });
                break;
            case SORT_CREATION_DATE:
                Collections.sort(sortedFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                break;
            case SORT_ALPHABETICAL:
                Collections.sort(sortedFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case SORT_LAST_USED:
                Collections.sort(sortedFiles, (a, b) -> {
                    long lastUsedA = usageMap.containsKey(a.getAbsolutePath()) ? 
                                    usageMap.get(a.getAbsolutePath()).getLastUsed() : 0;
                    long lastUsedB = usageMap.containsKey(b.getAbsolutePath()) ? 
                                    usageMap.get(b.getAbsolutePath()).getLastUsed() : 0;
                    return Long.compare(lastUsedB, lastUsedA); 
                });
                break;
            case SORT_NONE:
            default:
                
                break;
        }
        
        return sortedFiles;
    }
    
        public static class FileUsageInfo {
        private final String filePath;
        private final int usageCount;
        private final long lastUsed;
        private final long creationDate;
        
        public FileUsageInfo(String filePath, int usageCount, long lastUsed, long creationDate) {
            this.filePath = filePath;
            this.usageCount = usageCount;
            this.lastUsed = lastUsed;
            this.creationDate = creationDate;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public int getUsageCount() {
            return usageCount;
        }
        
        public long getLastUsed() {
            return lastUsed;
        }
        
        public long getCreationDate() {
            return creationDate;
        }
    }
} 