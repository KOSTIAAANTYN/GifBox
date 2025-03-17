package com.example.GifBox.buttons;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.widget.Button;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;

import com.example.GifBox.MainActivity;
import com.example.GifBox.R;
import com.example.GifBox.functions.MiniSearchActivity;
import com.example.GifBox.ui.settings.SettingsFragment;
import com.example.GifBox.functions.TextProcessing;

public class OverlayButton extends AccessibilityService {
    private static final String TAG = "OverlayButton";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "GifBoxServiceChannel";
    
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
    
    private static final long BUTTON_COLOR_RESET_DELAY = 1000;
    
    private static final String PREFS_NAME = "GifBoxPrefs";
    
    private SharedPreferences sharedPreferences;
    private boolean overlayEnabled;
    private int overlayFunction;
    
    private boolean isTextProcessingActivityActive = false;
    
    private static final long ACTIVITY_TIMEOUT = 10000;
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        checkSelectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastSelectionTime > SELECTION_TIMEOUT) {
                    selectedText = null;
                    hasActiveSelection = false;
                    
                    if (isOverlayShown && overlayEnabled) {
                        hideOverlay();
                    }
                } else {
                    checkIfTextIsStillSelected();
                    handler.postDelayed(this, CHECK_INTERVAL_MS);
                }
            }
        };
        
        try {
            isTextProcessingActivityActive = false;
        } catch (Exception e) {
            //
        }
        
        startForegroundService();
    }
    
    @Override
    protected void onServiceConnected() {
        try {
            Toast.makeText(this, getString(R.string.service_activated), Toast.LENGTH_LONG).show();
            loadSettings();
            resetProcessingState();
            
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            
            info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED |
                             AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                             AccessibilityEvent.TYPE_VIEW_CLICKED |
                             AccessibilityEvent.TYPE_TOUCH_INTERACTION_END |
                             AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            
            info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                        AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            
            info.notificationTimeout = 100;
            
            setServiceInfo(info);
        } catch (Exception e) {
            //
        }
    }
    
    private void loadSettings() {
        overlayEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_OVERLAY_ENABLED, false);
        overlayFunction = sharedPreferences.getInt(SettingsFragment.KEY_OVERLAY_FUNCTION, SettingsFragment.FUNCTION_DIRECT_PROCESSING);
        
        if (!isOverlayShown && isTextProcessingActivityActive) {
            resetProcessingState();
        }
    }
    
    private void resetProcessingState() {
        isTextProcessingActivityActive = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        
        loadSettings();
        if (!overlayEnabled && isOverlayShown) {
            hideOverlay();
            return;
        }

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
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            CharSequence text = extractTextFromEvent(event, source);
            if (text != null && !text.toString().trim().isEmpty()) {
                selectedText = text.toString();
                lastSelectionTime = System.currentTimeMillis();
                hasActiveSelection = true;
                
                if (overlayEnabled && !isOverlayShown) {
                    contextMenuPosition.set(getNodeBounds(source));
                    showGifBoxButton();
                }
                
                handler.removeCallbacks(checkSelectionRunnable);
                handler.postDelayed(checkSelectionRunnable, CHECK_INTERVAL_MS);
            }
            source.recycle();
        }
    }

    private void handleWindowChangeEvent(AccessibilityEvent event) {
        boolean isContextMenu = isContextMenu(event);
        isContextMenuVisible = isContextMenu;
        
        if (isContextMenu && hasActiveSelection) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                findSelectedTextInTree(rootNode);
                rootNode.recycle();
            }
        }
    }

    private void handleContentChangeEvent(AccessibilityEvent event) {
        if (hasActiveSelection && overlayEnabled && isOverlayShown) {
            handler.postDelayed(() -> {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    boolean hasSelection = findSelectedTextInTree(rootNode);
                    if (!hasSelection) {
                        hideOverlay();
                    }
                    rootNode.recycle();
                }
            }, CONTENT_CHANGE_IGNORE_DELAY);
        }
    }

    private void handleInteractionEvent() {
        if (!isContextMenuVisible && overlayEnabled && isOverlayShown) {
            handler.postDelayed(() -> {
                verifyTextSelection();
            }, HIDE_BUTTON_DELAY);
        }
    }

    private boolean isContextMenu(AccessibilityEvent event) {
        if (event.getSource() == null) return false;
        
        CharSequence className = event.getClassName();
        if (className != null) {
            String classNameStr = className.toString().toLowerCase();
            boolean isContextMenu = classNameStr.contains("contextmenu") || 
                                    classNameStr.contains("contextual") ||
                                    classNameStr.contains("selectionmenu") ||
                                    classNameStr.contains("floatingtoolbar");
            
            if (isContextMenu) {
                AccessibilityNodeInfo menuNode = event.getSource();
                if (menuNode != null) {
                    contextMenuPosition.set(getNodeBounds(menuNode));
                    menuNode.recycle();
                }
                return true;
            }
        }
        
        return false;
    }

    private void verifyTextSelection() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            boolean hasSelection = findSelectedTextInTree(rootNode);
            if (!hasSelection) {
                hasActiveSelection = false;
                selectedText = null;
                
                if (isOverlayShown) {
                    hideOverlay();
                }
            }
            rootNode.recycle();
        }
    }

    private void checkIfTextIsStillSelected() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            boolean hasSelection = findSelectedTextInTree(rootNode);
            if (!hasSelection) {
                hasActiveSelection = false;
                selectedText = null;
                
                if (isOverlayShown && overlayEnabled) {
                    hideOverlay();
                }
            }
            rootNode.recycle();
        }
    }

    private boolean findSelectedTextInTree(AccessibilityNodeInfo root) {
        if (root == null) return false;
        
        if (isNodeWithSelectedText(root)) {
            return true;
        }
        
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                if (findSelectedTextInTree(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private boolean canNodeContainSelection(AccessibilityNodeInfo node) {
        if (node == null) return false;
        boolean hasText = node.getText() != null && !node.getText().toString().isEmpty();
        boolean isEditable = (node.isEditable() || 
                             (node.getClassName() != null && 
                              (node.getClassName().toString().contains("EditText") || 
                               node.getClassName().toString().contains("TextView"))));
        
        return hasText && (isEditable || node.isSelected() || node.isFocused());
    }
    
    private boolean isNodeWithSelectedText(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        if (!canNodeContainSelection(node)) {
            return false;
        }
        
        CharSequence text = node.getText();
        if (text == null || text.toString().isEmpty()) {
            return false;
        }
        
        boolean isTextSelected = false;
        int start = -1;
        int end = -1;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            start = node.getTextSelectionStart();
            end = node.getTextSelectionEnd();
            
            if (start >= 0 && end > start && end <= text.length()) {
                isTextSelected = true;
                selectedText = text.subSequence(start, end).toString();
            }
        }
        if (!isTextSelected && node.isSelected()) {
            isTextSelected = true;
            selectedText = text.toString();
        }
        
        if (isTextSelected) {
            lastSelectionTime = System.currentTimeMillis();
            hasActiveSelection = true;
            return true;
        }
        
        return false;
    }

    private CharSequence extractTextFromEvent(AccessibilityEvent event, AccessibilityNodeInfo source) {
        CharSequence eventText = event.getText() != null && !event.getText().isEmpty() 
                                ? event.getText().get(0) 
                                : null;
        
        if (eventText != null && !eventText.toString().isEmpty()) {
            return eventText;
        } else if (source != null) {
            CharSequence sourceText = source.getText();
            
            if (sourceText != null && !sourceText.toString().isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    int start = source.getTextSelectionStart();
                    int end = source.getTextSelectionEnd();
                    
                    if (start >= 0 && end > start && end <= sourceText.length()) {
                        return sourceText.subSequence(start, end);
                    }
                }
                if (source.isSelected()) {
                    return sourceText;
                }
            }
        }
        
        return null;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.service_channel_description));
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        
        startForeground(NOTIFICATION_ID, notification);
    }

    private void showGifBoxButton() {
        loadSettings();
        if (!overlayEnabled || isTextProcessingActivityActive) return;
        
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
                
                initializeButton();

                windowManager.addView(overlayView, params);
                isOverlayShown = true;
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void initializeButton() {
        Button gifBoxButton = overlayView.findViewById(R.id.gifBoxButton);
        
        gifBoxButton.setOnClickListener(v -> {
            if (isTextProcessingActivityActive) return;
            
            if (overlayFunction == SettingsFragment.FUNCTION_DIRECT_PROCESSING) {
                openTextProcessingActivity(gifBoxButton);
            } else if (overlayFunction == SettingsFragment.FUNCTION_MINI_SEARCH) {
                openMiniSearchActivity(gifBoxButton);
            }
        });
    }
    
    private void openTextProcessingActivity(Button button) {
        if (selectedText != null) {
            isTextProcessingActivityActive = true;
            setButtonColor(button, R.color.button_processing);
            
            try {
                boolean success = TextProcessing.processText(this, selectedText);
                if (success) {
                    setButtonColor(button, R.color.button_success);
                } else {
                    setButtonColor(button, R.color.button_error);
                }
            } catch (Exception e) {
                setButtonColor(button, R.color.button_error);
            }
            
            isTextProcessingActivityActive = false;
            handler.postDelayed(this::resetProcessingState, ACTIVITY_TIMEOUT);
        } else {
            setButtonColor(button, R.color.button_error);
        }
    }
    
    private void openMiniSearchActivity(Button button) {
        if (selectedText != null) {
            isTextProcessingActivityActive = true;
            setButtonColor(button, R.color.button_processing);
            
            try {
                Intent intent = new Intent(this, MiniSearchActivity.class);
                intent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                
                setButtonColor(button, R.color.button_success);
                handler.postDelayed(() -> {
                    isTextProcessingActivityActive = false;
                }, 500);
                handler.postDelayed(this::resetProcessingState, ACTIVITY_TIMEOUT);
            } catch (Exception e) {
                setButtonColor(button, R.color.button_error);
                isTextProcessingActivityActive = false;
            }
        } else {
            setButtonColor(button, R.color.button_error);
        }
    }
    
    private void setButtonColor(Button button, int colorResId) {
        if (button != null && button.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) button.getBackground();
            drawable.setColor(getResources().getColor(colorResId));
            
            handler.postDelayed(() -> {
                if (button != null && button.getBackground() instanceof GradientDrawable) {
                    drawable.setColor(getResources().getColor(R.color.purple_500));
                }
            }, BUTTON_COLOR_RESET_DELAY);
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
            }
        });
    }

    private void hideOverlay() {
        handler.post(() -> {
            if (overlayView != null && isOverlayShown) {
                try {
                    windowManager.removeView(overlayView);
                } catch (Exception e) {
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

    private Rect getNodeBounds(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        if (node != null) {
            try {
                node.getBoundsInScreen(bounds);
            } catch (Exception e) {
                bounds.set(0, 0, 0, 0);
            }
        }
        return bounds;
    }
} 