package com.example.GifBox.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import java.util.Objects;
import java.util.Queue;
import java.util.LinkedList;
import java.lang.ref.WeakReference;
import com.example.GifBox.R;
import com.example.GifBox.utils.TextProcessingUtils;

public class GifBoxAccessibilityService extends AccessibilityService {
    private static final String TAG = "GifBoxService";
    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayShown = false;
    private Handler handler;
    private String selectedText = null;
    private Rect contextMenuPosition = new Rect();
    private static final int BUTTON_MARGIN = 16;
    private static final int BUTTON_WIDTH = 100;

    private static final long CHECK_INTERVAL_MS = 2000;
    private Runnable checkSelectionRunnable;

    private boolean isContextMenuVisible = false;
    private long lastSelectionTime = 0;
    private static final long SELECTION_TIMEOUT = 30000;

    private boolean hasActiveSelection = false;

    private static final long CONTENT_CHANGE_IGNORE_DELAY = 500;
    private static final long HIDE_BUTTON_DELAY = 300;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        checkSelectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (isOverlayShown) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSelectionTime > SELECTION_TIMEOUT) {
                        checkIfTextIsStillSelected();
                    }
                }
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        };

        handler.postDelayed(checkSelectionRunnable, CHECK_INTERVAL_MS);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();

        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                handleTextSelectionEvent(event);
                break;

            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowChangeEvent(event);
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleContentChangeEvent(event);
                break;

            case AccessibilityEvent.TYPE_VIEW_CLICKED:
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                handleInteractionEvent();
                break;
        }
    }

    private void handleTextSelectionEvent(AccessibilityEvent event) {
        lastSelectionTime = System.currentTimeMillis();
        handleTextSelection(event);
    }

    private void handleWindowChangeEvent(AccessibilityEvent event) {
        if (isContextMenu(event)) {
            isContextMenuVisible = true;
        } else if (isContextMenuVisible) {
            isContextMenuVisible = false;
            checkIfTextIsStillSelected();
        }
    }

    private void handleContentChangeEvent(AccessibilityEvent event) {
        long currentTime = System.currentTimeMillis();
        if (isOverlayShown && (currentTime - lastSelectionTime > CONTENT_CHANGE_IGNORE_DELAY)) {
            if (isContextMenu(event)) {
                isContextMenuVisible = true;
            } else if (!isContextMenuVisible) {
                checkIfTextIsStillSelected();
            }
        }
    }

    private void handleInteractionEvent() {
        if (isOverlayShown) {
            checkIfTextIsStillSelected();
        }
    }

    private boolean isContextMenu(AccessibilityEvent event) {
        if (event == null || event.getSource() == null) return false;

        CharSequence className = event.getClassName();
        if (className != null) {
            String classNameStr = className.toString().toLowerCase();

            boolean isMenu = classNameStr.contains("menu") ||
                    classNameStr.contains("popup") ||
                    classNameStr.contains("contextual") ||
                    classNameStr.contains("actionmode") ||
                    (classNameStr.contains("linearlayout") && event.getItemCount() > 0);

            return isMenu;
        }

        return false;
    }

    private void checkIfTextIsStillSelected() {
        if (isContextMenuVisible) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        try {
            boolean hasSelectedText = findSelectedTextInTree(root);

            hasActiveSelection = hasSelectedText;

            if (!hasSelectedText) {
                final WeakReference<GifBoxAccessibilityService> serviceRef = new WeakReference<>(this);

                handler.postDelayed(() -> {
                    GifBoxAccessibilityService service = serviceRef.get();
                    if (service == null) return;

                    AccessibilityNodeInfo newRoot = service.getRootInActiveWindow();
                    if (newRoot != null) {
                        try {
                            boolean stillHasSelectedText = service.findSelectedTextInTree(newRoot);
                            if (!stillHasSelectedText) {
                                service.hideOverlay();
                            }
                        } finally {
                            newRoot.recycle();
                        }
                    }
                }, HIDE_BUTTON_DELAY);
            }
        } finally {
            root.recycle();
        }
    }

    private boolean findSelectedTextInTree(AccessibilityNodeInfo root) {
        if (root == null) return false;

        Queue<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.poll();
            if (node == null) continue;

            try {
                if (isNodeWithSelectedText(node)) {
                    return true;
                }

                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) {
                        queue.add(child);
                    }
                }
            } finally {
                if (node != root) {
                    node.recycle();
                }
            }
        }

        return false;
    }

    private boolean isNodeWithSelectedText(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.isSelected() && node.getText() != null && !node.getText().toString().trim().isEmpty()) {
            return true;
        }

        if (node.getText() != null && !node.getText().toString().trim().isEmpty()) {
            CharSequence className = node.getClassName();
            if (className != null) {
                String classNameStr = className.toString().toLowerCase();
                if ((classNameStr.contains("edit") || classNameStr.contains("text")) &&
                        (node.isSelected() || node.isFocused())) {
                    if (node.getTextSelectionStart() >= 0 &&
                            node.getTextSelectionEnd() > node.getTextSelectionStart()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void handleTextSelection(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return;
        }

        try {
            CharSequence text = extractTextFromEvent(event, source);
            if (text == null) {
                return;
            }

            boolean isNodeSelected = source.isSelected();

            boolean isSelectionEvent = isNodeSelected ||
                    (event.getFromIndex() >= 0 && event.getToIndex() > event.getFromIndex()) ||
                    (event.getItemCount() > 0 && event.getCurrentItemIndex() >= 0);

            if (!text.toString().trim().isEmpty() && isSelectionEvent) {
                selectedText = text.toString().trim();

                Rect selectionRect = new Rect();
                source.getBoundsInScreen(selectionRect);

                contextMenuPosition.set(selectionRect);
                showGifBoxButton();

                lastSelectionTime = System.currentTimeMillis();
                hasActiveSelection = true;
            } else if (isOverlayShown) {
                checkIfTextIsStillSelected();
            }
        } finally {
            source.recycle();
        }
    }

    private CharSequence extractTextFromEvent(AccessibilityEvent event, AccessibilityNodeInfo source) {
        CharSequence text = null;

        if (event.getText() != null && !event.getText().isEmpty()) {
            text = event.getText().get(0);
        }

        if ((text == null || text.toString().trim().isEmpty()) && source.getText() != null) {
            text = source.getText();
        }

        return text;
    }

    @Override
    protected void onServiceConnected() {

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_END |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;

        info.notificationTimeout = 100;

        setServiceInfo(info);

        Toast.makeText(this, "GifBox service activated", Toast.LENGTH_SHORT).show();
    }

    private void showGifBoxButton() {
        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }

        handler.post(() -> {
            try {
                if (isOverlayShown) {
                    updateOverlayPosition();
                    return;
                }

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

                params.gravity = Gravity.TOP | Gravity.START;
                params.x = contextMenuPosition.left - BUTTON_MARGIN - BUTTON_WIDTH;
                params.y = contextMenuPosition.top;

                LayoutInflater inflater = LayoutInflater.from(this);
                overlayView = inflater.inflate(R.layout.gifbox_button_overlay, null);

                Button gifBoxButton = overlayView.findViewById(R.id.gifBoxButton);
                gifBoxButton.setText("GifBox");

                gifBoxButton.setOnClickListener(v -> {
                    openTextProcessingActivity();
                });

                windowManager.addView(overlayView, params);
                isOverlayShown = true;

                showSelectionToast();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openTextProcessingActivity() {
        if (selectedText != null) {
            Toast.makeText(this, "Text processing: " + selectedText, Toast.LENGTH_SHORT).show();
            try {
                boolean success = TextProcessingUtils.processText(this, selectedText);
                if (success) {
                    Toast.makeText(this, "Text successfully processed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Media files could not be found", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSelectionToast() {
        if (selectedText != null) {
            String displayText = selectedText.length() > 10 ?
                    selectedText.substring(0, 10) + "..." : selectedText;
            Toast.makeText(this, "GifBox: selected \"" + displayText + "\"", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOverlayPosition() {
        if (overlayView == null || !isOverlayShown) return;

        handler.post(() -> {
            try {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
                params.x = contextMenuPosition.left - BUTTON_MARGIN - BUTTON_WIDTH;
                params.y = contextMenuPosition.top;
                windowManager.updateViewLayout(overlayView, params);
            } catch (Exception e) {
                Log.d(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });
    }

    private void hideOverlay() {
        handler.post(() -> {
            if (overlayView != null && isOverlayShown) {
                try {
                    windowManager.removeView(overlayView);
                } catch (Exception e) {
                    Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                } finally {
                    isOverlayShown = false;
                    overlayView = null;
                    selectedText = null;
                    hasActiveSelection = false;
                }
            }
        });
    }

    @Override
    public void onInterrupt() {
        hideOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();

        handler.removeCallbacks(checkSelectionRunnable);
        handler.removeCallbacksAndMessages(null);
    }
} 