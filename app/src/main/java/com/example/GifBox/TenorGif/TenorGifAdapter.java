package com.example.GifBox.TenorGif;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.GifBox.MainActivity;
import com.example.GifBox.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TenorGifAdapter extends RecyclerView.Adapter<TenorGifAdapter.ViewHolder> {
    private Context context;
    private List<TenorGif> tenorGifs;
    private ExecutorService downloadExecutor;
    private OnGifDownloadedListener listener;

    public TenorGifAdapter(Context context, List<TenorGif> tenorGifs) {
        this.context = context;
        this.tenorGifs = tenorGifs;
        this.downloadExecutor = Executors.newFixedThreadPool(3);
    }

    public void setOnGifDownloadedListener(OnGifDownloadedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tenor_gif, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TenorGif gif = tenorGifs.get(position);

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(500)
                .fitCenter();

        Glide.with(context)
                .asGif()
                .load(gif.getPreviewUrl())
                .apply(options)
                .placeholder(new ColorDrawable(Color.LTGRAY))
                .into(holder.gifImageView);

        holder.downloadButton.setOnClickListener(v -> downloadGif(gif));
    }

    private String getUniqueFileName(File dir, String baseFileName) {
        String fileName = baseFileName;
        String nameWithoutExtension = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
        String extension = baseFileName.substring(baseFileName.lastIndexOf('.'));

        File file = new File(dir, fileName);
        int counter = 1;

        while (file.exists()) {
            fileName = nameWithoutExtension + "(" + counter + ")" + extension;
            file = new File(dir, fileName);
            counter++;
        }

        return fileName;
    }

    private void downloadGif(TenorGif gif) {
        downloadExecutor.execute(() -> {
            try {
                URL url = new URL(gif.getGifUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                File dir = new File(context.getExternalFilesDir(null), "MyMedia");
                if (!dir.exists()) dir.mkdirs();

                String baseFileName = "tenor_" + gif.getId() + ".gif";
                String uniqueFileName = getUniqueFileName(dir, baseFileName);
                File outputFile = new File(dir, uniqueFileName);

                FileOutputStream outputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                input.close();

                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "GIF downloaded", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onGifDownloaded(outputFile);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "ErrorL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return tenorGifs.size();
    }

    public void updateData(List<TenorGif> newGifs) {
        this.tenorGifs = newGifs;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView gifImageView;
        Button downloadButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            gifImageView = itemView.findViewById(R.id.tenorGifImageView);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }
    }

    public interface OnGifDownloadedListener {
        void onGifDownloaded(File downloadedGif);
    }
}