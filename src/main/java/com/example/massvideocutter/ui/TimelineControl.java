package com.example.massvideocutter.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Combined Timeline + Waveform control with draggable START/END markers.
 * Waveform covers the ENTIRE video duration for full sync with timeline.
 */
public class TimelineControl extends Pane {

    // Properties
    private final DoubleProperty duration = new SimpleDoubleProperty(100);
    private final DoubleProperty currentTime = new SimpleDoubleProperty(0);
    private final DoubleProperty startMarker = new SimpleDoubleProperty(0);
    private final DoubleProperty endMarker = new SimpleDoubleProperty(100);

    // Visual elements
    private final Canvas canvas;

    // Waveform data - covers entire video duration
    private double[] amplitudes;
    private double waveformDuration = 0; // Duration that waveform data covers

    // Constants
    private static final double PADDING = 15;
    private static final double MARKER_WIDTH = 10;

    // Colors
    private static final Color TRACK_BG = Color.web("#1a1a1a");
    private static final Color WAVEFORM_COLOR_1 = Color.web("#00BFFF", 0.85);
    private static final Color WAVEFORM_COLOR_2 = Color.web("#FF6B00", 0.85);
    private static final Color RANGE_COLOR = Color.web("#FFA500", 0.20);
    private static final Color START_MARKER_COLOR = Color.web("#00CC66");
    private static final Color END_MARKER_COLOR = Color.web("#FF4444");
    private static final Color PLAYHEAD_COLOR = Color.web("#FFFFFF");

    // FFmpeg path
    private static final String FFMPEG_PATH = "C:/Projeler/HelperTools/ffmpeg-7.1.1/bin/ffmpeg.exe";

    // Drag state
    private enum DragTarget { NONE, START, END, PLAYHEAD }
    private DragTarget dragging = DragTarget.NONE;

    // Callbacks
    private Runnable onStartMarkerChanged;
    private Runnable onEndMarkerChanged;
    private Runnable onSeek;

    public TimelineControl() {
        canvas = new Canvas();
        getChildren().add(canvas);

        // Bind canvas size to pane size
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Redraw on size change
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());

        // Redraw on property changes
        duration.addListener((obs, oldVal, newVal) -> draw());
        currentTime.addListener((obs, oldVal, newVal) -> draw());
        startMarker.addListener((obs, oldVal, newVal) -> draw());
        endMarker.addListener((obs, oldVal, newVal) -> draw());

        // Mouse handlers
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseMoved(this::handleMouseMoved);

        setPrefHeight(60);
        setMinHeight(60);

        // Empty waveform initially
        amplitudes = new double[0];
    }

    /**
     * Load waveform from video file asynchronously.
     * Returns the waveform data for caching.
     */
    public CompletableFuture<double[]> loadWaveform(String videoPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                amplitudes = extractWaveform(videoPath);
                waveformDuration = duration.get();
                javafx.application.Platform.runLater(this::draw);
                return amplitudes;
            } catch (Exception e) {
                System.err.println("Failed to extract waveform: " + e.getMessage());
                amplitudes = generatePlaceholderWaveform();
                waveformDuration = duration.get();
                javafx.application.Platform.runLater(this::draw);
                return amplitudes;
            }
        });
    }

    /**
     * Set waveform data directly (from cache)
     */
    public void setWaveformData(double[] data) {
        this.amplitudes = data;
        this.waveformDuration = duration.get();
        draw();
    }

    private double[] extractWaveform(String videoPath) throws Exception {
        int sampleCount = 400; // More bars = more detail
        List<Short> rawSamples = new ArrayList<>();

        // Extract audio for full video (no -t limit)
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_PATH,
                "-i", videoPath,
                "-ac", "1",                     // Mono
                "-ar", "8000",                  // 8kHz sample rate
                "-f", "s16le",                  // Raw 16-bit samples
                "-vn",                          // No video
                "-"                             // Output to stdout
        );

        pb.redirectErrorStream(false); // Don't mix stderr
        Process process = pb.start();

        // Read raw audio data
        byte[] buffer = new byte[8192];
        var inputStream = process.getInputStream();
        int bytesRead;
        
        // Read all audio data
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead - 1; i += 2) {
                short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                rawSamples.add(sample);
            }
        }

        process.waitFor();

        if (rawSamples.isEmpty()) {
            System.out.println("No audio samples extracted, using placeholder");
            return generatePlaceholderWaveform();
        }

        System.out.println("Extracted " + rawSamples.size() + " audio samples");

        // Downsample to target sample count with PEAK detection (not RMS)
        double[] result = new double[sampleCount];
        int samplesPerBar = Math.max(1, rawSamples.size() / sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            int startIdx = i * samplesPerBar;
            int endIdx = Math.min(startIdx + samplesPerBar, rawSamples.size());

            // Find PEAK amplitude in this segment (more sensitive than RMS)
            double maxAbs = 0;
            double sumSquares = 0;
            
            for (int j = startIdx; j < endIdx; j++) {
                double val = Math.abs(rawSamples.get(j) / 32768.0);
                maxAbs = Math.max(maxAbs, val);
                sumSquares += val * val;
            }

            // Combine peak and RMS for better visual representation
            double rms = Math.sqrt(sumSquares / (endIdx - startIdx));
            double combined = (maxAbs * 0.6 + rms * 0.4); // Weight towards peak
            
            // Apply logarithmic scaling for better dynamic range
            double scaled = Math.log1p(combined * 10) / Math.log1p(10);
            
            // Normalize and amplify
            result[i] = Math.min(1.0, scaled * 1.5);
        }

        // Normalize to use full height
        double maxVal = 0;
        for (double v : result) maxVal = Math.max(maxVal, v);
        if (maxVal > 0) {
            for (int i = 0; i < result.length; i++) {
                result[i] = result[i] / maxVal;
            }
        }

        return result;
    }

    private double[] generatePlaceholderWaveform() {
        double[] fake = new double[400];
        for (int i = 0; i < fake.length; i++) {
            // More varied pattern
            double base = 0.3 + 0.3 * Math.sin(i * 0.05);
            double variation = 0.2 * Math.sin(i * 0.15) * Math.cos(i * 0.03);
            double noise = 0.1 * Math.random();
            fake[i] = Math.max(0.1, Math.min(1.0, base + variation + noise));
        }
        return fake;
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        if (w <= 0 || h <= 0) return;

        // Clear
        gc.clearRect(0, 0, w, h);

        double trackWidth = w - (PADDING * 2);
        double waveformHeight = h - 12;

        // Draw background
        gc.setFill(TRACK_BG);
        gc.fillRoundRect(PADDING - 5, 0, trackWidth + 10, waveformHeight, 8, 8);

        // Draw range highlight (between start and end markers)
        double startX = timeToX(startMarker.get());
        double endX = timeToX(endMarker.get());
        gc.setFill(RANGE_COLOR);
        gc.fillRect(startX, 0, endX - startX, waveformHeight);

        // Draw waveform - synchronized with timeline
        if (amplitudes != null && amplitudes.length > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, WAVEFORM_COLOR_1),
                    new Stop(1, WAVEFORM_COLOR_2)
            );
            gc.setFill(gradient);

            double barWidth = trackWidth / amplitudes.length;
            double centerY = waveformHeight / 2;
            double maxBarHeight = waveformHeight - 6;

            for (int i = 0; i < amplitudes.length; i++) {
                double amplitude = amplitudes[i];
                
                // Minimum visible height + scaled height
                double barHeight = 2 + (amplitude * (maxBarHeight - 2));
                double x = PADDING + i * barWidth;
                double y = centerY - barHeight / 2;

                // Draw bar with slight gap between bars
                double actualWidth = Math.max(1, barWidth - 1);
                gc.fillRoundRect(x, y, actualWidth, barHeight, 1, 1);
            }
        }

        // Draw START marker
        drawMarker(gc, startX, waveformHeight, START_MARKER_COLOR, "S");

        // Draw END marker
        drawMarker(gc, endX, waveformHeight, END_MARKER_COLOR, "E");

        // Draw playhead
        double playheadX = timeToX(currentTime.get());
        gc.setFill(PLAYHEAD_COLOR);
        gc.fillRect(playheadX - 1, 0, 2, waveformHeight);

        // Playhead circle at bottom
        gc.fillOval(playheadX - 4, waveformHeight - 4, 8, 8);
    }

    private void drawMarker(GraphicsContext gc, double x, double h, Color color, String label) {
        // Marker line
        gc.setFill(color);
        gc.fillRect(x - 1, 0, 3, h);

        // Marker flag at top
        double flagWidth = 14;
        double flagHeight = 14;

        if (label.equals("S")) {
            gc.fillRoundRect(x - 1, 0, flagWidth, flagHeight, 3, 3);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText(label, x + 3, 10);
        } else {
            gc.fillRoundRect(x - flagWidth + 1, 0, flagWidth, flagHeight, 3, 3);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText(label, x - 10, 10);
        }
    }

    private double timeToX(double time) {
        double trackWidth = getWidth() - (PADDING * 2);
        double dur = duration.get();
        if (dur <= 0) return PADDING;
        return PADDING + (time / dur) * trackWidth;
    }

    private double xToTime(double x) {
        double trackWidth = getWidth() - (PADDING * 2);
        double time = ((x - PADDING) / trackWidth) * duration.get();
        return Math.max(0, Math.min(duration.get(), time));
    }

    private void handleMousePressed(MouseEvent e) {
        double x = e.getX();
        double startX = timeToX(startMarker.get());
        double endX = timeToX(endMarker.get());
        double tolerance = MARKER_WIDTH + 5;

        if (Math.abs(x - startX) < tolerance) {
            dragging = DragTarget.START;
        } else if (Math.abs(x - endX) < tolerance) {
            dragging = DragTarget.END;
        } else {
            dragging = DragTarget.PLAYHEAD;
            currentTime.set(xToTime(x));
            if (onSeek != null) onSeek.run();
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        double time = xToTime(e.getX());

        switch (dragging) {
            case START -> {
                if (time < endMarker.get() - 1) {
                    startMarker.set(time);
                    if (onStartMarkerChanged != null) onStartMarkerChanged.run();
                }
            }
            case END -> {
                if (time > startMarker.get() + 1) {
                    endMarker.set(time);
                    if (onEndMarkerChanged != null) onEndMarkerChanged.run();
                }
            }
            case PLAYHEAD -> {
                currentTime.set(time);
                if (onSeek != null) onSeek.run();
            }
            default -> {}
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        dragging = DragTarget.NONE;
    }

    private void handleMouseMoved(MouseEvent e) {
        double x = e.getX();
        double startX = timeToX(startMarker.get());
        double endX = timeToX(endMarker.get());
        double tolerance = MARKER_WIDTH + 5;

        if (Math.abs(x - startX) < tolerance || Math.abs(x - endX) < tolerance) {
            setCursor(Cursor.H_RESIZE);
        } else {
            setCursor(Cursor.HAND);
        }
    }

    public void clearWaveform() {
        amplitudes = new double[0];
        waveformDuration = 0;
        draw();
    }

    // Property accessors
    public DoubleProperty durationProperty() { return duration; }
    public DoubleProperty currentTimeProperty() { return currentTime; }
    public DoubleProperty startMarkerProperty() { return startMarker; }
    public DoubleProperty endMarkerProperty() { return endMarker; }

    public double getDuration() { return duration.get(); }
    public void setDuration(double value) { duration.set(value); endMarker.set(value); }

    public double getCurrentTime() { return currentTime.get(); }
    public void setCurrentTime(double value) { currentTime.set(value); }

    public double getStartMarker() { return startMarker.get(); }
    public void setStartMarker(double value) { startMarker.set(value); }

    public double getEndMarker() { return endMarker.get(); }
    public void setEndMarker(double value) { endMarker.set(value); }

    public void setOnStartMarkerChanged(Runnable callback) { this.onStartMarkerChanged = callback; }
    public void setOnEndMarkerChanged(Runnable callback) { this.onEndMarkerChanged = callback; }
    public void setOnSeek(Runnable callback) { this.onSeek = callback; }
}
