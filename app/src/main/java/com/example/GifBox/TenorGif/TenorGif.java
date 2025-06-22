package com.example.GifBox.TenorGif;

public class TenorGif {
    private String id;
    private String title;
    private String previewUrl;
    private String gifUrl;

    public TenorGif(String id, String title, String previewUrl, String gifUrl) {
        this.id = id;
        this.title = title;
        this.previewUrl = previewUrl;
        this.gifUrl = gifUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getGifUrl() {
        return gifUrl;
    }
} 