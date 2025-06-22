package com.example.GifBox.TenorGif;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.GifBox.MainActivity;
import com.example.GifBox.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TenorSearchFragment extends Fragment implements TenorApiClient.TenorApiListener, TenorGifAdapter.OnGifDownloadedListener {

    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private TextView noResultsTextView;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TenorGifAdapter adapter;
    private TenorApiClient apiClient;
    private Handler searchHandler;
    private boolean isSearching = false;
    private boolean isLoadingMore = false;
    private Runnable performSearchRunnable;
    private String currentQuery = "";
    private String nextPos = "";
    private List<TenorGif> currentGifs = new ArrayList<>();
    private FloatingActionButton fab;
    private boolean isFabPlus = true;

    public static TenorSearchFragment newInstance() {
        return new TenorSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tenor_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchButton = view.findViewById(R.id.clearSearchButton);
        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.tenorRecyclerView);

        apiClient = new TenorApiClient();
        searchHandler = new Handler(Looper.getMainLooper());

        setupRecyclerView();
        setupSearchFunctionality();
        setupFab();

        loadTrendingGifs();
    }

    private void setupFab() {
        if (getActivity() instanceof MainActivity) {
            fab = ((MainActivity) getActivity()).binding.appBarMain.fab;
            rotateFabToX();
            
            fab.setOnClickListener(v -> {
                if (!isFabPlus) {
                    resetToHome();
                } else {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showBottomDialog();
                    }
                }
            });
        }
    }
    
    private void rotateFabToX() {
        if (fab != null && isFabPlus) {
            ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 135f);
            rotateAnimation.setDuration(300);
            rotateAnimation.setInterpolator(new AccelerateInterpolator());
            
            fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(android.R.color.holo_red_light)));
            
            rotateAnimation.start();
            isFabPlus = false;
        }
    }
    
    private void rotateFabToPlus() {
        if (fab != null && !isFabPlus) {
            ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(fab, "rotation", 135f, 0f);
            rotateAnimation.setDuration(300);
            rotateAnimation.setInterpolator(new DecelerateInterpolator());
            
            fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.purple_200)));
            
            rotateAnimation.start();
            isFabPlus = true;
        }
    }
    
    private void resetToHome() {
        if (getActivity() instanceof MainActivity) {
            rotateFabToPlus();
            ((MainActivity) getActivity()).activateHomeTab();
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TenorGifAdapter(getContext(), currentGifs);
        adapter.setOnGifDownloadedListener(this);
        recyclerView.setAdapter(adapter);
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (!isLoadingMore && !isSearching && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                            && firstVisibleItemPosition >= 0
                            && nextPos != null && !nextPos.isEmpty()) {
                        loadMoreGifs();
                    }
                }
            }
        });
    }

    private void loadMoreGifs() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        
        progressBar.setVisibility(View.VISIBLE);
        
        if (currentQuery.isEmpty()) {
            apiClient.getTrendingGifs(nextPos, this);
        } else {
            apiClient.searchGifs(currentQuery, nextPos, this);
        }
    }

    private void setupSearchFunctionality() {
        final long SEARCH_DELAY_MS = 800;

        performSearchRunnable = () -> {
            if (isSearching) return;
            isSearching = true;

            currentQuery = searchEditText.getText().toString().trim();
            currentGifs.clear();
            nextPos = "";
            if (currentQuery.isEmpty()) {
                loadTrendingGifs();
            } else {
                searchGifs(currentQuery);
            }
        };

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
                return true;
            }
            return false;
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            currentQuery = "";
            currentGifs.clear();
            nextPos = "";
            loadTrendingGifs();

            searchEditText.requestFocus();
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void loadTrendingGifs() {
        showLoading(true);
        apiClient.getTrendingGifs("", this);
    }

    private void searchGifs(String query) {
        showLoading(true);
        apiClient.searchGifs(query, "", this);
    }

    private void showLoading(boolean isLoading) {
        if (!isLoadingMore) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                noResultsTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onGifsLoaded(List<TenorGif> gifs, String newNextPos) {
        showLoading(false);
        
        this.nextPos = newNextPos;
        
        if (gifs.isEmpty() && currentGifs.isEmpty()) {
            noResultsTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noResultsTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            if (isLoadingMore) {
                int previousSize = currentGifs.size();
                currentGifs.addAll(gifs);
                adapter.notifyItemRangeInserted(previousSize, gifs.size());
            } else {
                currentGifs.clear();
                currentGifs.addAll(gifs);
                adapter.updateData(currentGifs);
            }
        }
        
        isSearching = false;
        isLoadingMore = false;
    }

    @Override
    public void onError(String errorMessage) {
        isSearching = false;
        isLoadingMore = false;
        showLoading(false);
        Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGifDownloaded(File downloadedGif) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshMediaList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
        
        rotateFabToPlus();
    }
} 