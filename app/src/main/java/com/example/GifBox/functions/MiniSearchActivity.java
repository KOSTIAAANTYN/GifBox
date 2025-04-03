package com.example.GifBox.functions;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.GifBox.R;
import com.example.GifBox.utils.FileUsageTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiniSearchActivity extends Activity {
    private static final String TAG = "MiniSearchActivity";
    
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private ImageButton closeButton;
    private TextView noResultsTextView;
    private RecyclerView recyclerView;
    private MiniGifAdapter adapter;
    private List<File> mediaList = new ArrayList<>();
    private List<File> filteredMediaList = new ArrayList<>();
    private Handler searchHandler;
    private boolean isSearching = false;
    private Runnable performSearchRunnable;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (isLaunchedFromService()) {
            setIntent(getIntent().putExtra("launched_as_dialog", true));
        }
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_mini_search);
        
        setupWindow();
        
        searchEditText = findViewById(R.id.miniSearchEditText);
        clearSearchButton = findViewById(R.id.miniClearSearchButton);
        closeButton = findViewById(R.id.closeButton);
        noResultsTextView = findViewById(R.id.miniNoResultsTextView);
        recyclerView = findViewById(R.id.miniRecyclerView);
        searchHandler = new Handler(Looper.getMainLooper());
        
        setupRecyclerViewLayout();
        
        loadMedia();
        
        
        new Handler(Looper.getMainLooper()).postDelayed(this::checkVisibleItems, 500);
        
        performSearchRunnable = () -> {
            if (isSearching) return;
            
            String query = searchEditText.getText().toString();
            performSearch(query);
        };
        
        setupSearchFunctionality();
        
        Intent intent = getIntent();
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        
        if (text != null) {
            searchEditText.setText(text);
            performSearch(text.toString());
        }
        
        closeButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
    
    @Override
    public void finish() {
        if (isLaunchedFromService()) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
        }
        super.finish();
    }
    
    private void setupWindow() {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        window.setAttributes(params);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        
        if (isLaunchedFromService()) {
            setTheme(android.R.style.Theme_Translucent_NoTitleBar);
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | 
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | 
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            );
        }
    }
    
    private boolean isLaunchedFromService() {
        Intent intent = getIntent();
        return intent != null && intent.hasExtra(Intent.EXTRA_PROCESS_TEXT);
    }
    
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
            finish();
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isLaunchedFromService()) {
            finish();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (isLaunchedFromService()) {
            finish();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isLaunchedFromService() && !hasFocus) {
            finish();
        }
    }
    
    private void setupSearchFunctionality() {
        final long SEARCH_DELAY_MS = 800;
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                
                searchHandler.removeCallbacks(performSearchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.postDelayed(performSearchRunnable, SEARCH_DELAY_MS);
            }
        });
        
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                
                searchHandler.removeCallbacks(performSearchRunnable);
                performSearchRunnable.run();
                
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                
                return true;
            }
            return false;
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            performSearch("");
        });
    }
    
    private void performSearch(String query) {
        isSearching = true;
        
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
        
        searchHandler.post(() -> {
            try {
                adapter.notifyDataSetChanged();
                noResultsTextView.setVisibility(filteredMediaList.isEmpty() && !query.isEmpty() ? 
                        View.VISIBLE : View.GONE);
                
                isSearching = false;
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI after search", e);
                isSearching = false;
            }
        });
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
    
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        
        SharedPreferences preferences = getSharedPreferences("GifBoxPrefs", Context.MODE_PRIVATE);
        boolean showFilenames = preferences.getBoolean("show_filenames", false);
        boolean showExtensionIcon = preferences.getBoolean("show_extension_icon", false);
        boolean useFlexibleGrid = preferences.getBoolean("use_flexible_grid", false);
        
        
        if (adapter != null && 
            (adapter.showFilenames != showFilenames || 
             adapter.showExtensionIcon != showExtensionIcon ||
             adapter.useFlexibleGrid != useFlexibleGrid)) {
            adapter.showFilenames = showFilenames;
            adapter.showExtensionIcon = showExtensionIcon;
            adapter.useFlexibleGrid = useFlexibleGrid;
            adapter.notifyDataSetChanged();
        }
        
        
        if (adapter != null && adapter.useFlexibleGrid != useFlexibleGrid) {
            setupRecyclerViewLayout();
        }
        
        
        new Handler(Looper.getMainLooper()).postDelayed(this::checkVisibleItems, 300);
    }
    
    private void setupRecyclerViewLayout() {
        SharedPreferences preferences = getSharedPreferences("GifBoxPrefs", Context.MODE_PRIVATE);
        boolean useFlexibleGrid = preferences.getBoolean("use_flexible_grid", false);
        
        if (useFlexibleGrid) {
            
            androidx.recyclerview.widget.StaggeredGridLayoutManager layoutManager = 
                new androidx.recyclerview.widget.StaggeredGridLayoutManager(3, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL);
            layoutManager.setGapStrategy(androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE);
            recyclerView.setLayoutManager(layoutManager);
            adapter = new MiniGifAdapter(this, filteredMediaList, true);
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            adapter = new MiniGifAdapter(this, filteredMediaList, false);
        }
        recyclerView.setAdapter(adapter);
        
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkVisibleItems();
                }
            }
        });
    }
    
    
    private void checkVisibleItems() {
        if (recyclerView == null || adapter == null) return;
        
        
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int firstVisibleItem = 0;
        int lastVisibleItem = 0;
        
        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition();
            lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();
        } 
        else if (layoutManager instanceof androidx.recyclerview.widget.StaggeredGridLayoutManager) {
            androidx.recyclerview.widget.StaggeredGridLayoutManager staggeredGridLayoutManager = 
                (androidx.recyclerview.widget.StaggeredGridLayoutManager) layoutManager;
            
            int[] firstPositions = new int[staggeredGridLayoutManager.getSpanCount()];
            int[] lastPositions = new int[staggeredGridLayoutManager.getSpanCount()];
            
            staggeredGridLayoutManager.findFirstVisibleItemPositions(firstPositions);
            staggeredGridLayoutManager.findLastVisibleItemPositions(lastPositions);
            
            firstVisibleItem = findMin(firstPositions);
            lastVisibleItem = findMax(lastPositions);
        }
        
        
        if (lastVisibleItem >= firstVisibleItem) {
            for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
                if (i >= 0 && i < adapter.getItemCount()) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                    if (viewHolder instanceof MiniGifAdapter.ViewHolder) {
                        MiniGifAdapter.ViewHolder holder = (MiniGifAdapter.ViewHolder) viewHolder;
                        
                        
                        if (holder.videoView.getVisibility() == View.VISIBLE) {
                            if (!holder.videoView.isPlaying()) {
                                try {
                                    File file = adapter.mediaList.get(i);
                                    String extension = file.getName().substring(file.getName().lastIndexOf(".")).toLowerCase();
                                    
                                    if (extension.equals(".mp4") || extension.equals(".webm") || extension.equals(".3gp")) {
                                        adapter.configureVideoView(holder, file);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error reconfiguring video", e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    
    private int findMin(int[] array) {
        int min = array[0];
        for (int value : array) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private int findMax(int[] array) {
        int max = array[0];
        for (int value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
    
    private class MiniGifAdapter extends RecyclerView.Adapter<MiniGifAdapter.ViewHolder> {
        private Context context;
        private List<File> mediaList;
        private boolean showFilenames = false;
        private boolean showExtensionIcon = false;
        private boolean useFlexibleGrid = false;

        public MiniGifAdapter(Context context, List<File> mediaList) {
            this(context, mediaList, false);
        }
        
        public MiniGifAdapter(Context context, List<File> mediaList, boolean useFlexibleGrid) {
            this.context = context;
            this.mediaList = mediaList;
            this.useFlexibleGrid = useFlexibleGrid;
            
            
            SharedPreferences preferences = context.getSharedPreferences("GifBoxPrefs", Context.MODE_PRIVATE);
            this.showFilenames = preferences.getBoolean("show_filenames", false);
            this.showExtensionIcon = preferences.getBoolean("show_extension_icon", false);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (useFlexibleGrid) {
                view = LayoutInflater.from(context).inflate(R.layout.item_gif_flexible, parent, false);
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_gif, parent, false);
            }
            return new ViewHolder(view);
        }

        public void configureVideoView(ViewHolder holder, File file) {
            try {
                
                if (!file.exists() || !file.canRead()) {
                    Log.e(TAG, "Video file does not exist or cannot be read: " + file.getAbsolutePath());
                    return;
                }
                
                
                holder.videoView.stopPlayback();
                holder.videoView.setOnPreparedListener(null);
                holder.videoView.setOnErrorListener(null);
                
                
                Uri videoUri = Uri.fromFile(file);
                
                
                holder.videoView.setOnPreparedListener(mp -> {
                    try {
                        
                        mp.setVolume(0f, 0f);
                        mp.setLooping(true);
                        
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(1.0f));
                        }
                        
                        
                        int width = mp.getVideoWidth();
                        int height = mp.getVideoHeight();
                        
                        if (width > 0 && height > 0) {
                            
                            ViewGroup.LayoutParams layoutParams = holder.videoView.getLayoutParams();
                            int viewWidth = holder.videoView.getWidth();
                            if (viewWidth == 0) viewWidth = layoutParams.width;
                            if (viewWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                                
                                viewWidth = ((View)holder.videoView.getParent()).getWidth();
                            }
                            
                            
                            int calculatedHeight = (viewWidth * height) / width;
                            
                            
                            int minHeight = useFlexibleGrid ? 150 : 200;
                            calculatedHeight = Math.max(calculatedHeight, minHeight);
                            
                            
                            if (layoutParams.height != calculatedHeight) {
                                layoutParams.height = calculatedHeight;
                                holder.videoView.setLayoutParams(layoutParams);
                            }
                        }
                        
                        
                        mp.setOnBufferingUpdateListener((mediaPlayer, percent) -> {
                            if (percent > 5 && !mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }
                        });
                        
                        
                        mp.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onPrepared", e);
                    }
                });
                
                
                holder.videoView.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "VideoView error: what=" + what + ", extra=" + extra);
                    try {
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                holder.videoView.setVideoURI(videoUri);
                                holder.videoView.requestFocus();
                                holder.videoView.start();
                            } catch (Exception e) {
                                Log.e(TAG, "Error in error retry", e);
                            }
                        }, 500);
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling error", e);
                    }
                    return true;
                });
                
                
                holder.videoView.setOnCompletionListener(mp -> {
                    try {
                        mp.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Error restarting video on completion", e);
                    }
                });
                
                
                holder.videoView.setVideoURI(videoUri);
                holder.videoView.requestFocus();
                
                
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        holder.videoView.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting video after delay", e);
                    }
                }, 100);
                
            } catch (Exception e) {
                Log.e(TAG, "Error configuring video view", e);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = mediaList.get(position);
            String fileName = file.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

            holder.itemView.setOnClickListener(v -> {
                if (extension.equalsIgnoreCase(".gif")) {
                    copyGifToClipboard(file);
                } else {
                    shareFile(file);
                }
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });

            
            if (showFilenames) {
                holder.fileNameTextView.setVisibility(View.VISIBLE);
                String fileNameWithoutExtension = fileName;
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0) {
                    fileNameWithoutExtension = fileName.substring(0, dotIndex);
                }
                holder.fileNameTextView.setText(fileNameWithoutExtension);
            } else {
                holder.fileNameTextView.setVisibility(View.GONE);
            }
            
            
            if (showExtensionIcon) {
                holder.extensionIconView.setVisibility(View.VISIBLE);
                if (extension.equals(".gif")) {
                    holder.extensionIconView.setImageResource(R.drawable.ic_extension_gif);
                } else if (extension.equals(".mp4") || extension.equals(".webm") || extension.equals(".3gp")) {
                    holder.extensionIconView.setImageResource(R.drawable.ic_extension_video);
                }
            } else {
                holder.extensionIconView.setVisibility(View.GONE);
            }

            if (extension.equals(".mp4") || extension.equals(".webm") || extension.equals(".3gp")) {
                holder.videoView.setVisibility(View.VISIBLE);
                holder.imageView.setVisibility(View.GONE);
                
                
                configureVideoView(holder, file);
                
            } else if (extension.equals(".gif")) {
                holder.imageView.setVisibility(View.VISIBLE);
                holder.videoView.setVisibility(View.GONE);
                
                RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
                
                if (useFlexibleGrid) {
                    
                    options = options.centerCrop();
                } else {
                    
                    options = options.override(300)
                            .fitCenter();
                }
                
                Glide.with(context)
                    .asGif()
                    .load(file)
                    .apply(options)
                    .into(holder.imageView);
            } else {
                
                holder.imageView.setVisibility(View.VISIBLE);
                holder.videoView.setVisibility(View.GONE);
                
                RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);
                
                if (useFlexibleGrid) {
                    
                    options = options.centerCrop();
                } else {
                    
                    options = options.override(300)
                            .fitCenter();
                }
                
                Glide.with(context)
                    .load(file)
                    .apply(options)
                    .into(holder.imageView);
            }
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            if (holder.videoView.isPlaying()) {
                holder.videoView.stopPlayback();
            }
            Glide.with(context).clear(holder.imageView);
            holder.imageView.setImageDrawable(null);
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            VideoView videoView;
            TextView fileNameTextView;
            ImageView extensionIconView;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                videoView = itemView.findViewById(R.id.videoView);
                fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
                extensionIconView = itemView.findViewById(R.id.extensionIconView);
            }
        }
    }
    
    private void copyGifToClipboard(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                file);

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newUri(getContentResolver(), "GIF", contentUri);
            clipboard.setPrimaryClip(clip);
            
            
            FileUsageTracker.trackFileUsage(this, file);

            Toast.makeText(this, R.string.gif_copied, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_copying_gif, e.getMessage()), Toast.LENGTH_SHORT).show();
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
            } else if (file.getName().endsWith(".3gp")) {
                mimeType = "video/3gpp";
            } else {
                mimeType = "*/*";
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            
            FileUsageTracker.trackFileUsage(this, file);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_sharing_file, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }
} 