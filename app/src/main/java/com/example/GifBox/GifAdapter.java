package com.example.GifBox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        if (extension.equals(".mp4") || extension.equals(".webm")) {
            holder.videoView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVideoPath(file.getAbsolutePath());
            holder.videoView.setOnPreparedListener(mp -> mp.setLooping(true));
            holder.videoView.start();
        } else {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.videoView.setVisibility(View.GONE);
            Glide.with(context).asGif().load(file).into(holder.imageView);
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
}

