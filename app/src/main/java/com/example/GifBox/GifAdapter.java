package com.example.GifBox;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.GifBox.R;
import java.io.File;
import java.util.List;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.ViewHolder> {
    private Context context;
    private List<File> mediaList;

    public GifAdapter(Context context, List<File> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
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

        if (extension.equals(".mp4") || extension.equals(".webm")) {
            holder.videoView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVideoPath(file.getAbsolutePath());
            holder.videoView.setOnPreparedListener(mp -> {
                mp.setVolume(0f, 0f);
                mp.setLooping(true);
            });
            holder.videoView.start();
        } else {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.videoView.setVisibility(View.GONE);
            Glide.with(context).asGif().load(file).into(holder.imageView);
        }
    }

    private void showGifDialog(File file) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gif_view);

        ImageView gifImageView = dialog.findViewById(R.id.gifImageView);
        TextView gifNameTextView = dialog.findViewById(R.id.gifNameTextView);
        Button closeButton = dialog.findViewById(R.id.closeButton);
        VideoView videoView = dialog.findViewById(R.id.videoView);

        gifNameTextView.setText(file.getName());
        if (file.getName().endsWith(".mp4") || file.getName().endsWith(".webm")) {
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

        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (videoView.getVisibility() == View.VISIBLE) {
                videoView.stopPlayback();
            }
        });

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.show();
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
}

