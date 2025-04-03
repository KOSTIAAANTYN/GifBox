package com.example.GifBox.GridLayoutManager;

import android.content.Context;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class FlexibleGridLayoutManager extends StaggeredGridLayoutManager {
    
    public FlexibleGridLayoutManager(Context context, int spanCount) {
        super(spanCount, VERTICAL);
        setGapStrategy(GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);

        setGapStrategy(GAP_HANDLING_NONE);
        setItemPrefetchEnabled(true);
    }
} 