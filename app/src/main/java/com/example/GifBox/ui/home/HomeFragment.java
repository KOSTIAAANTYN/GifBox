package com.example.GifBox.ui.home;

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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.GifBox.MainActivity;
import com.example.GifBox.R;

public class HomeFragment extends Fragment {

    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private TextView noResultsTextView;
    private Handler searchHandler;
    private boolean isSearching = false;
    private Runnable performSearchRunnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        
        searchEditText = root.findViewById(R.id.searchEditText);
        clearSearchButton = root.findViewById(R.id.clearSearchButton);
        noResultsTextView = root.findViewById(R.id.noResultsTextView);
        searchHandler = new Handler(Looper.getMainLooper());

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            RecyclerView recyclerView = activity.findViewById(R.id.recyclerView);
            if (recyclerView != null) {
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        if (newState == RecyclerView.SCROLL_STATE_IDLE && searchEditText != null) {
                            searchEditText.requestFocus();
                            
                            
                            searchEditText.setSelection(searchEditText.getText().length());
                        }
                    }
                });
            }
        }
        
        performSearchRunnable = () -> {
            if (isSearching) return;
            isSearching = true;
            
            String query = searchEditText.getText().toString();
            
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                boolean hasResults = activity.filterMedia(query);
                noResultsTextView.setVisibility(hasResults || query.isEmpty() ? View.GONE : View.VISIBLE);
            }
            
            searchHandler.post(() -> {
                
                searchEditText.requestFocus();
                
                
                searchEditText.setSelection(searchEditText.getText().length());
                
                
                if (getActivity() != null) {
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
                
                isSearching = false;
            });
        };
        
        setupSearchFunctionality();
        
        return root;
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
                
                return true;
            }
            return false;
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.filterMedia("");
                noResultsTextView.setVisibility(View.GONE);
            }
            
            searchEditText.requestFocus();
            
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
    }
    

    public void resetSearch() {
        if (searchEditText != null) {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            noResultsTextView.setVisibility(View.GONE);
        }
    }
}