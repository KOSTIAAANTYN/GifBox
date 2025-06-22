package com.example.GifBox.TenorGif;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TenorApiClient {
    private static final String TAG = "TenorApiClient";
    private static final String API_KEY = "LIVDSRZULELA";
    private static final String TENOR_SEARCH_URL = "https://api.tenor.com/v1/search?key=%s&q=%s&limit=%d&pos=%s";
    private static final String TENOR_TRENDING_URL = "https://api.tenor.com/v1/trending?key=%s&limit=%d&pos=%s";
    private static final int DEFAULT_LIMIT = 20;

    public interface TenorApiListener {
        void onGifsLoaded(List<TenorGif> gifs, String nextPos);
        void onError(String errorMessage);
    }

    public void searchGifs(String query, String pos, TenorApiListener listener) {
        new TenorApiTask(listener, true, query, pos).execute();
    }

    public void getTrendingGifs(String pos, TenorApiListener listener) {
        new TenorApiTask(listener, false, null, pos).execute();
    }

    private class TenorApiTask extends AsyncTask<Void, Void, List<TenorGif>> {
        private TenorApiListener listener;
        private boolean isSearch;
        private String query;
        private String pos;
        private String errorMessage;
        private String nextPos = "";

        public TenorApiTask(TenorApiListener listener, boolean isSearch, String query, String pos) {
            this.listener = listener;
            this.isSearch = isSearch;
            this.query = query;
            this.pos = pos != null ? pos : "";
        }

        @Override
        protected List<TenorGif> doInBackground(Void... voids) {
            List<TenorGif> gifs = new ArrayList<>();
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url;
                if (isSearch) {
                    url = new URL(String.format(TENOR_SEARCH_URL, API_KEY, query, DEFAULT_LIMIT, pos));
                } else {
                    url = new URL(String.format(TENOR_TRENDING_URL, API_KEY, DEFAULT_LIMIT, pos));
                }

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server returned code: " + responseCode;
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.has("next")) {
                    nextPos = jsonResponse.getString("next");
                }
                
                JSONArray results = jsonResponse.getJSONArray("results");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject gifObject = results.getJSONObject(i);
                    String id = gifObject.getString("id");
                    String title = gifObject.getString("title");

                    JSONObject mediaObject = gifObject.getJSONArray("media").getJSONObject(0);
                    JSONObject gifFormat = mediaObject.getJSONObject("gif");
                    JSONObject tinyFormat = mediaObject.getJSONObject("tinygif");

                    String gifUrl = gifFormat.getString("url");
                    String previewUrl = tinyFormat.getString("url");

                    TenorGif gif = new TenorGif(id, title, previewUrl, gifUrl);
                    gifs.add(gif);
                }

                return gifs;
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching GIFs", e);
                errorMessage = e.getMessage();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(List<TenorGif> gifs) {
            if (gifs != null) {
                listener.onGifsLoaded(gifs, nextPos);
            } else {
                listener.onError(errorMessage);
            }
        }
    }
} 