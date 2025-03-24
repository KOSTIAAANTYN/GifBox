package com.example.GifBox.functions;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        
        adapter = new MiniGifAdapter(this, filteredMediaList);
        recyclerView.setAdapter(adapter);
        
        loadMedia();
        
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
                
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                
                searchHandler.removeCallbacks(performSearchRunnable);
                performSearchRunnable.run();
                
                return true;
            }
            return false;
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            performSearch("");
            searchEditText.requestFocus();
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
    
    private class MiniGifAdapter extends RecyclerView.Adapter<MiniGifAdapter.ViewHolder> {
        private Context context;
        private List<File> mediaList;

        public MiniGifAdapter(Context context, List<File> mediaList) {
            this.context = context;
            this.mediaList = mediaList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_gif, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = mediaList.get(position);
            String extension = file.getName().substring(file.getName().lastIndexOf("."));

            holder.itemView.setOnClickListener(v -> {
                if (extension.equalsIgnoreCase(".gif")) {
                    copyGifToClipboard(file);
                } else {
                    shareFile(file);
                }
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });

            if (extension.equals(".mp4") || extension.equals(".webm")) {
                holder.videoView.setVisibility(View.VISIBLE);
                holder.imageView.setVisibility(View.GONE);

                holder.videoView.getLayoutParams().height = 300;
                holder.videoView.setVideoPath(file.getAbsolutePath());
                holder.videoView.setOnPreparedListener(mp -> {
                    mp.setVolume(0f, 0f);
                    mp.setLooping(true);
                });
                
                holder.videoView.start();
            } else {
                holder.imageView.setVisibility(View.VISIBLE);
                holder.videoView.setVisibility(View.GONE);
                
                RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(300)
                    .fitCenter();
                
                Glide.with(context)
                    .asGif()
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

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                videoView = itemView.findViewById(R.id.videoView);
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
            } else {
                mimeType = "*/*";
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_sharing_file, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }
} 