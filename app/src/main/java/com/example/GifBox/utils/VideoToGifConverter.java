package com.example.GifBox.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.example.GifBox.R;

import java.io.File;
import java.text.DecimalFormat;

public class VideoToGifConverter {
    
    private static final String TAG = "VideoToGifConverter";

    public interface ConversionCallback {
        void onConversionStart();
        void onConversionProgress(int progress);
        void onConversionComplete(File outputFile);
        void onConversionFailed(String errorMessage);
    }

    public interface SessionCallback {
        void onSessionCreated(FFmpegSession session);
    }

    public static void convertVideoToGif(Context context, File videoFile, ConversionCallback callback) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());
            
            String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            int fps = 30;
            
            if (frameRateStr != null && !frameRateStr.isEmpty()) {
                try {
                    float frameRateFloat = Float.parseFloat(frameRateStr);
                    fps = Math.max(1, Math.min(30, Math.round(frameRateFloat)));
                } catch (NumberFormatException e) {
                }
            }
            
            retriever.release();
            
            convertVideoToGifWithSession(context, videoFile, 720, fps, callback, null);
            
        } catch (Exception e) {
            convertVideoToGifWithSession(context, videoFile, 720, 24, callback, null);
        }
    }

    public static void convertVideoToGifWithSession(final Context context, final File videoFile, int maxDimension, int fps, 
                                            final ConversionCallback callback, final SessionCallback sessionCallback) {
        if (!videoFile.exists()) {
            callback.onConversionFailed(context.getString(R.string.conversion_failed, "File does not exist"));
            return;
        }

        if (!videoFile.canRead()) {
            callback.onConversionFailed(context.getString(R.string.conversion_failed, "Cannot read file"));
            return;
        }

        String inputPath = videoFile.getAbsolutePath();
        String originalFileName = videoFile.getName();
        String fileNameWithoutExtension = originalFileName;
        int lastDotIndex = originalFileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileNameWithoutExtension = originalFileName.substring(0, lastDotIndex);
        }
        
        String outputFileName = fileNameWithoutExtension + "Gif.gif";
        File outputDir = videoFile.getParentFile();
        File outputFile = new File(outputDir, outputFileName);
        
        int counter = 1;
        while (outputFile.exists()) {
            outputFileName = fileNameWithoutExtension + "Gif" + counter + ".gif";
            outputFile = new File(outputDir, outputFileName);
            counter++;
        }
        
        String outputPath = outputFile.getAbsolutePath();
        
        final File finalOutputFile = outputFile;
        
        if (!outputDir.canWrite()) {
            callback.onConversionFailed(context.getString(R.string.conversion_failed, "No write access to directory"));
            return;
        }
        
        long videoSize = videoFile.length();
        long freeSpace = outputDir.getFreeSpace();
        
        if (freeSpace < videoSize * 2) {
            callback.onConversionFailed(context.getString(R.string.conversion_failed, 
                    "Not enough free space. Approximately " + formatFileSize(videoSize * 2) + 
                    " needed, " + formatFileSize(freeSpace) + " available"));
            return;
        }

        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputPath);
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (widthStr == null || heightStr == null || durationStr == null) {
                callback.onConversionFailed(context.getString(R.string.conversion_failed, "Unable to get video metadata"));
                retriever.release();
                return;
            }

            int width = Integer.parseInt(widthStr);
            int height = Integer.parseInt(heightStr);
            final long durationMs = Long.parseLong(durationStr);

            int newWidth, newHeight;
            if (width > height) {
                newWidth = Math.min(width, maxDimension);
                newHeight = (int) (height * ((float) newWidth / width));
            } else {
                newHeight = Math.min(height, maxDimension);
                newWidth = (int) (width * ((float) newHeight / height));
            }

            callback.onConversionStart();

            String escapedInputPath = inputPath.replace("\"", "\\\"");
            String escapedOutputPath = outputPath.replace("\"", "\\\"");

            String command = String.format("-y -i \"%s\" -vf \"fps=%d,scale=%d:%d:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" -loop 0 \"%s\"",
                    escapedInputPath, fps, newWidth, newHeight, escapedOutputPath);

            FFmpegSession session = FFmpegKit.executeAsync(command, session2 -> {
                SessionState state = session2.getState();
                ReturnCode returnCode = session2.getReturnCode();
                
                try {
                    if (ReturnCode.isSuccess(returnCode)) {
                        callback.onConversionComplete(finalOutputFile);
                    } else {
                        String failStackTrace = session2.getFailStackTrace();
                        String errorOutput = session2.getAllLogsAsString();
                        
                        String errorMsg = "Code: " + returnCode;
                        if (failStackTrace != null && !failStackTrace.isEmpty()) {
                            errorMsg += ", details: " + failStackTrace;
                        } else if (errorOutput != null && !errorOutput.isEmpty()) {
                            errorMsg += ", details in log: " + errorOutput.substring(0, Math.min(errorOutput.length(), 200));
                        } else {
                            errorMsg += ", unknown error. Check file access and free space on device.";
                        }
                        callback.onConversionFailed(errorMsg);
                    }
                } catch (Exception e) {
                    callback.onConversionFailed("Internal error: " + e.getMessage());
                }
            }, log -> {
                try {
                    String message = log.getMessage();
                    if (message.contains("time=")) {
                        int timeIndex = message.indexOf("time=");
                        if (timeIndex > 0) {
                            String timeStr = message.substring(timeIndex + 5, timeIndex + 16);
                            String[] timeParts = timeStr.split(":");
                            
                            if (timeParts.length >= 3) {
                                int hours = Integer.parseInt(timeParts[0]);
                                int minutes = Integer.parseInt(timeParts[1]);
                                float seconds = Float.parseFloat(timeParts[2]);
                                
                                long processedTimeMs = (hours * 3600 + minutes * 60) * 1000 + (long)(seconds * 1000);
                                int progress = (int) ((processedTimeMs * 100) / durationMs);
                                progress = Math.min(progress, 99);
                                
                                callback.onConversionProgress(progress);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }, statistics -> {
            });

            if (sessionCallback != null) {
                sessionCallback.onSessionCreated(session);
            }

            retriever.release();
        } catch (Exception e) {
            final String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                callback.onConversionFailed("Unknown conversion error");
            } else {
                callback.onConversionFailed(errorMessage);
            }
        }
    }

    private static String formatFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
} 