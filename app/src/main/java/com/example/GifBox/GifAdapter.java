package com.example.GifBox;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.GifBox.R;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.app.ProgressDialog;
import com.example.GifBox.utils.VideoToGifConverter;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import com.example.GifBox.MainActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.ViewHolder> {
    private static final String TAG = "GifAdapter";
    private Context context;
    private List<File> mediaList;
    private RecyclerView recyclerView;
    private Handler scrollHandler;
    private Runnable scrollRunnable;
    private static final long SCROLL_DELAY = 150;

    public GifAdapter(Context context, List<File> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
        this.scrollHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        setupScrollListener();
    }

    private void setupScrollListener() {
        scrollRunnable = this::checkVisibleItems;
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                scrollHandler.removeCallbacks(scrollRunnable);
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrollHandler.postDelayed(scrollRunnable, SCROLL_DELAY);
                }
            }
        });
    }

    private void checkVisibleItems() {
        if (recyclerView == null) return;

        int firstVisible = ((RecyclerView.LayoutManager) recyclerView.getLayoutManager()).getChildCount();
        
        for (int i = 0; i < firstVisible; i++) {
            View view = recyclerView.getChildAt(i);
            if (view != null) {
                ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(view);
                int position = holder.getAdapterPosition();
                
                if (position != RecyclerView.NO_POSITION) {
                    android.graphics.Rect rect = new android.graphics.Rect();
                    boolean isPartiallyVisible = view.getGlobalVisibleRect(rect);
                    
                    if (isPartiallyVisible) {
                        startMediaPlayback(holder, position);
                    }
                }
            }
        }
    }

    private void startMediaPlayback(ViewHolder holder, int position) {
        File file = mediaList.get(position);
        String extension = file.getName().substring(file.getName().lastIndexOf("."));
        
        if ((extension.equals(".mp4") || extension.equals(".webm")) && 
            holder.videoView.getVisibility() == View.VISIBLE) {
            if (!holder.videoView.isPlaying()) {
                holder.videoView.start();
            }
        } else if (extension.equals(".gif") && 
                   holder.imageView.getVisibility() == View.VISIBLE) {
            if (holder.imageView.getDrawable() == null) {
                RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(500)
                    .fitCenter();
                
                Glide.with(context)
                    .asGif()
                    .load(file)
                    .apply(options)
                    .into(holder.imageView);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gif, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = mediaList.get(position);
        String extension = file.getName().substring(file.getName().lastIndexOf("."));

        holder.itemView.setOnLongClickListener(v -> {
            showGifDialog(file);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (extension.equalsIgnoreCase(".gif")) {
                copyGifToClipboard(file);
            } else {
                Toast.makeText(context, "Only GIF files can be copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        if (extension.equals(".mp4") || extension.equals(".webm")) {
            holder.videoView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);

            holder.videoView.getLayoutParams().height = 500;
            holder.videoView.setVideoPath(file.getAbsolutePath());
            holder.videoView.setOnPreparedListener(mp -> {
                mp.setVolume(0f, 0f);
                mp.setLooping(true);
            });
            
            View view = holder.itemView;
            android.graphics.Rect rect = new android.graphics.Rect();
            if (view.getGlobalVisibleRect(rect)) {
                holder.videoView.start();
            }
        } else {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.videoView.setVisibility(View.GONE);
            
            View view = holder.itemView;
            android.graphics.Rect rect = new android.graphics.Rect();
            if (view.getGlobalVisibleRect(rect)) {
                RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(500)
                    .fitCenter();
                
                Glide.with(context)
                    .asGif()
                    .load(file)
                    .apply(options)
                    .into(holder.imageView);
            }
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.videoView.isPlaying()) {
            holder.videoView.stopPlayback();
        }
        Glide.with(context).clear(holder.imageView);
        holder.imageView.setImageDrawable(null);
    }

    private void showGifDialog(File file) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gif_view);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onPause(LifecycleOwner owner) {
                    if (dialog.isShowing()) {
                        VideoView videoView = dialog.findViewById(R.id.videoView);
                        if (videoView != null && videoView.isPlaying()) {
                            videoView.stopPlayback();
                        }
                        dialog.dismiss();
                    }
                    activity.getLifecycle().removeObserver(this);
                }
            });
        }

        ImageView gifImageView = dialog.findViewById(R.id.gifImageView);
        TextView gifNameTextView = dialog.findViewById(R.id.gifNameTextView);
        VideoView videoView = dialog.findViewById(R.id.videoView);
        View shareButton = dialog.findViewById(R.id.shareButton);
        View renameButton = dialog.findViewById(R.id.renameButton);
        View deleteButton = dialog.findViewById(R.id.deleteButton);
        View convertButton = dialog.findViewById(R.id.convertButton);

        boolean isVideo = file.getName().endsWith(".mp4") || file.getName().endsWith(".webm");
        
        if (convertButton != null) {
            if (file.getName().toLowerCase().endsWith(".mp4") ||
                file.getName().toLowerCase().endsWith(".webm") ||
                file.getName().toLowerCase().endsWith(".3gp")) {

                convertButton.setVisibility(View.VISIBLE);
                convertButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    startVideoToGifConversion(context, file);
                });
            } else {
                convertButton.setVisibility(View.GONE);
            }
        }

        gifNameTextView.setText(file.getName());
        if (isVideo) {
            videoView.setVisibility(View.VISIBLE);
            gifImageView.setVisibility(View.GONE);
            videoView.setVideoPath(file.getAbsolutePath());
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(1f, 1f);
            });
            videoView.start();
        } else {
            videoView.setVisibility(View.GONE);
            gifImageView.setVisibility(View.VISIBLE);
            Glide.with(context).asGif().load(file).into(gifImageView);
        }

        shareButton.setOnClickListener(v -> {
            Uri contentUri = FileProvider.getUriForFile(context, 
                context.getApplicationContext().getPackageName() + ".provider", 
                file);
            
            String mimeType;
            if (file.getName().endsWith(".mp4")) {
                mimeType = "video/mp4";
            } else if (file.getName().endsWith(".webm")) {
                mimeType = "video/webm";
            } else if (file.getName().endsWith(".gif")) {
                mimeType = "image/gif";
            } else {
                mimeType = "*/*";
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        renameButton.setOnClickListener(v -> {
            showRenameDialog(file, dialog, gifNameTextView);
        });

        deleteButton.setOnClickListener(v -> {
            showDeleteDialog(file, dialog);
        });

        View.OnClickListener dismissListener = v -> {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            dialog.dismiss();
        };

        FrameLayout mediaContainer = dialog.findViewById(R.id.mediaContainer);
        if (mediaContainer != null) {
            mediaContainer.setOnClickListener(dismissListener);
        }

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.show();
    }

    private void showRenameDialog(File file, Dialog parentDialog, TextView gifNameTextView) {
        Dialog renameDialog = new Dialog(context);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.dialog_rename);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onPause(LifecycleOwner owner) {
                    if (renameDialog.isShowing()) {
                        renameDialog.dismiss();
                    }
                    if (parentDialog.isShowing()) {
                        VideoView videoView = parentDialog.findViewById(R.id.videoView);
                        if (videoView != null && videoView.isPlaying()) {
                            videoView.stopPlayback();
                        }
                        parentDialog.dismiss();
                    }
                    activity.getLifecycle().removeObserver(this);
                }
            });
        }

        EditText editTextFileName = renameDialog.findViewById(R.id.editTextFileName);
        Button buttonCancel = renameDialog.findViewById(R.id.buttonCancel);
        Button buttonSave = renameDialog.findViewById(R.id.buttonSave);

        String fileName = file.getName();
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        editTextFileName.setText(nameWithoutExtension);
        editTextFileName.setSelection(nameWithoutExtension.length());

        buttonCancel.setOnClickListener(v -> renameDialog.dismiss());

        buttonSave.setOnClickListener(v -> {
            String newName = editTextFileName.getText().toString().trim();
            if (!newName.isEmpty()) {
                String extension = fileName.substring(fileName.lastIndexOf("."));
                File newFile = new File(file.getParent(), newName + extension);
                
                if (file.renameTo(newFile)) {
                    int position = mediaList.indexOf(file);
                    if (position != -1) {
                        mediaList.set(position, newFile);
                        notifyItemChanged(position);
                    }
                    
                    gifNameTextView.setText(newFile.getName());
                    renameDialog.dismiss();
                    parentDialog.dismiss();
                    Toast.makeText(context, "File renamed successfully", Toast.LENGTH_SHORT).show();
                    
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        mainActivity.refreshMediaList();
                    }
                } else {
                    Toast.makeText(context, "Error renaming file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        renameDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        renameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        renameDialog.show();
    }

    private void showDeleteDialog(File file, Dialog parentDialog) {
        Dialog confirmDialog = new Dialog(context);
        confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        confirmDialog.setContentView(R.layout.dialog_delete);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onPause(LifecycleOwner owner) {
                    if (confirmDialog.isShowing()) {
                        confirmDialog.dismiss();
                    }
                    if (parentDialog.isShowing()) {
                        VideoView videoView = parentDialog.findViewById(R.id.videoView);
                        if (videoView != null && videoView.isPlaying()) {
                            videoView.stopPlayback();
                        }
                        parentDialog.dismiss();
                    }
                    activity.getLifecycle().removeObserver(this);
                }
            });
        }

        Button buttonCancel = confirmDialog.findViewById(R.id.buttonCancel);
        Button buttonDelete = confirmDialog.findViewById(R.id.buttonDelete);
        
        buttonCancel.setOnClickListener(view -> confirmDialog.dismiss());
        
        buttonDelete.setOnClickListener(view -> {
            if (file.delete()) {
                int position = mediaList.indexOf(file);
                if (position != -1) {
                    mediaList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
                }
                confirmDialog.dismiss();
                parentDialog.dismiss();
                
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.refreshMediaList();
                }
            } else {
                Toast.makeText(context, "Error deleting file", Toast.LENGTH_SHORT).show();
            }
        });
        
        confirmDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);
        confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmDialog.show();
    }

    private void copyGifToClipboard(File file) {
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
            Toast.makeText(context, "Error copying GIF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        VideoView videoView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            videoView = itemView.findViewById(R.id.videoView);
        }
    }

    private void startVideoToGifConversion(Context context, File videoFile) {
        VideoToGifConverter.convertVideoToGif(context, videoFile, new VideoToGifConverter.ConversionCallback() {
            @Override
            public void onConversionStart() {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setTitle(context.getString(R.string.converting));
                    progressDialog.setMessage(context.getString(R.string.conversion_progress, 0));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMax(100);
                    progressDialog.setProgress(0);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    
                    this.setTag(progressDialog);
                });
            }
            
            @Override
            public void onConversionProgress(int progress) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    ProgressDialog progressDialog = (ProgressDialog) this.getTag();
                    if (progressDialog != null) {
                        progressDialog.setProgress(progress);
                        progressDialog.setMessage(context.getString(R.string.conversion_progress, progress));
                    }
                });
            }
            
            @Override
            public void onConversionComplete(File outputFile) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    ProgressDialog progressDialog = (ProgressDialog) this.getTag();
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    
                    Toast.makeText(context, context.getString(R.string.conversion_complete) + ": " + outputFile.getName(), Toast.LENGTH_LONG).show();
                    
                    if (outputFile.exists()) {
                        try {
                            if (context instanceof MainActivity) {
                                MainActivity activity = (MainActivity) context;
                                activity.refreshMediaList();
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            }
            
            @Override
            public void onConversionFailed(String errorMessage) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    ProgressDialog progressDialog = (ProgressDialog) this.getTag();
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.conversion_failed, ""))
                        .setMessage(errorMessage)
                        .setPositiveButton(android.R.string.ok, null);
                    builder.show();
                });
            }
            
            private ProgressDialog tag;
            public void setTag(ProgressDialog dialog) {
                this.tag = dialog;
            }
            public Object getTag() {
                return tag;
            }
        });
    }
}


