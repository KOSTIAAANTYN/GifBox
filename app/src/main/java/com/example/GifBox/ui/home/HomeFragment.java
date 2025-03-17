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

import com.example.GifBox.MainActivity;
import com.example.GifBox.R;

public class HomeFragment extends Fragment {

    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private TextView noResultsTextView;
    private Handler searchHandler;
    private boolean isSearching = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        
        searchEditText = root.findViewById(R.id.searchEditText);
        clearSearchButton = root.findViewById(R.id.clearSearchButton);
        noResultsTextView = root.findViewById(R.id.noResultsTextView);
        searchHandler = new Handler(Looper.getMainLooper());
        
        setupSearchFunctionality();
        
        return root;
    }
    
    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                if (isSearching) return true;
                isSearching = true;
                
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                
                String query = searchEditText.getText().toString();
                
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    boolean hasResults = activity.filterMedia(query);
                    noResultsTextView.setVisibility(hasResults || query.isEmpty() ? View.GONE : View.VISIBLE);
                }
                
                searchHandler.post(() -> {
                    isSearching = false;
                });
                
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
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null) {
            searchHandler.removeCallbacksAndMessages(null);
        }
    }
}