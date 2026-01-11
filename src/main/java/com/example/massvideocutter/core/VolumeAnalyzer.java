package com.example.massvideocutter.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes audio volume levels to detect anime intro/outro segments.
 * Uses hardware acceleration when available.
 */
public class VolumeAnalyzer {

    private static final String FFMPEG_PATH = "C:/Projeler/HelperTools/ffmpeg-7.1.1/bin/ffmpeg.exe";
    private static final String FFPROBE_PATH = "C:/Projeler/HelperTools/ffmpeg-7.1.1/bin/ffprobe.exe";

    // Configuration
    private static final int MIN_SEGMENT_DURATION = 50;            // Minimum intro/outro duration
    private static final int INTRO_SEARCH_LIMIT = 180;             // Search in first 3 min
    private static final int OUTRO_SEARCH_LIMIT = 180;             // Search in last 3 min
    private static final int GAP_TOLERANCE = 10;                   // Allow 10s gaps
    private static final double THRESHOLD_OFFSET_DB = 10.0;        // Threshold = max - 10dB

    /**
     * Get video duration using ffprobe
     */
    private double getVideoDuration(String videoPath) throws Exception {
        List<String> cmd = List.of(
                FFPROBE_PATH,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String duration = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            duration = reader.readLine();
        }
        process.waitFor();

        return (duration != null && !duration.isEmpty()) ? Double.parseDouble(duration.trim()) : 0;
    }

    /**
     * Extract volume levels using optimized FFmpeg settings.
     * Uses multi-threading for faster processing.
     */
    public List<Double> extractVolumeLevels(String videoPath, double videoDuration) throws Exception {
        List<Double> allSamples = new ArrayList<>();

        // Optimized FFmpeg command with multi-threading
        List<String> cmd = List.of(
                FFMPEG_PATH,
                "-threads", "0",              // Use all CPU threads
                "-i", videoPath,
                "-vn",                        // Skip video processing (faster)
                "-af", "astats=metadata=1:reset=1,ametadata=print:key=lavfi.astats.Overall.RMS_level",
                "-f", "null", "-"
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("RMS_level=")) {
                    try {
                        String[] parts = line.split("RMS_level=");
                        if (parts.length >= 2) {
                            String value = parts[1].trim();
                            if (!value.equals("-inf") && !value.isEmpty()) {
                                allSamples.add(Double.parseDouble(value));
                            } else {
                                allSamples.add(-100.0);
                            }
                        }
                    } catch (Exception e) {
                        allSamples.add(-100.0);
                    }
                }
            }
        }
        process.waitFor();

        if (allSamples.isEmpty() || videoDuration <= 0) return allSamples;

        // Downsample to 1 value per second
        double samplesPerSecond = allSamples.size() / videoDuration;
        List<Double> perSecond = new ArrayList<>();
        int targetSeconds = (int) Math.ceil(videoDuration);

        for (int sec = 0; sec < targetSeconds; sec++) {
            int startIdx = (int) (sec * samplesPerSecond);
            int endIdx = Math.min((int) ((sec + 1) * samplesPerSecond), allSamples.size());
            if (startIdx >= allSamples.size()) break;

            double sum = 0;
            int count = 0;
            for (int i = startIdx; i < endIdx; i++) {
                sum += allSamples.get(i);
                count++;
            }
            perSecond.add(count > 0 ? sum / count : -100.0);
        }

        return perSecond;
    }

    /**
     * Find loudest segment in range - IMPROVED for outro detection
     */
    private HighVolumeSegment findLoudestSegment(List<Double> levels, int rangeStart, int rangeEnd, String segmentType) {
        if (rangeEnd > levels.size()) rangeEnd = levels.size();
        if (rangeStart < 0) rangeStart = 0;
        if (rangeStart >= rangeEnd) return null;

        // Find max and average volume in range
        double maxVolume = -200;
        double sumVolume = 0;
        int countAboveSilence = 0;
        
        for (int i = rangeStart; i < rangeEnd; i++) {
            double level = levels.get(i);
            if (level > maxVolume) maxVolume = level;
            if (level > -90) {  // Not silence
                sumVolume += level;
                countAboveSilence++;
            }
        }

        double avgVolume = countAboveSilence > 0 ? sumVolume / countAboveSilence : -100;
        
        // For outro: use stricter threshold if max is very close to average
        double threshold;
        if (segmentType.equals("OUTRO") && (maxVolume - avgVolume) < 5) {
            // Low dynamic range - use a fixed offset from max
            threshold = maxVolume - 8;
        } else {
            threshold = maxVolume - THRESHOLD_OFFSET_DB;
        }

        System.out.println(">>> [VolumeAnalyzer] " + segmentType + " range " + rangeStart + "s-" + rangeEnd + "s: " +
                           "max=" + String.format("%.1f", maxVolume) + "dB, " +
                           "avg=" + String.format("%.1f", avgVolume) + "dB, " +
                           "threshold=" + String.format("%.1f", threshold) + "dB");

        // Find segments
        List<HighVolumeSegment> segments = new ArrayList<>();
        int segStart = -1;
        int segEnd = -1;
        int gapCount = 0;

        for (int i = rangeStart; i < rangeEnd; i++) {
            if (levels.get(i) > threshold) {
                if (segStart == -1) segStart = i;
                segEnd = i;
                gapCount = 0;
            } else if (segStart != -1) {
                gapCount++;
                if (gapCount > GAP_TOLERANCE) {
                    int duration = segEnd - segStart;
                    if (duration >= MIN_SEGMENT_DURATION) {
                        segments.add(new HighVolumeSegment(segStart, segEnd, duration));
                    }
                    segStart = -1;
                    segEnd = -1;
                    gapCount = 0;
                }
            }
        }

        if (segStart != -1 && segEnd != -1 && (segEnd - segStart) >= MIN_SEGMENT_DURATION) {
            segments.add(new HighVolumeSegment(segStart, segEnd, segEnd - segStart));
        }

        // If no segments found with strict criteria, try relaxed (30s minimum)
        if (segments.isEmpty()) {
            System.out.println(">>> [VolumeAnalyzer] No " + MIN_SEGMENT_DURATION + "s+ segment, trying 30s...");
            return findSegmentRelaxed(levels, rangeStart, rangeEnd, threshold, 30);
        }

        // Return longest segment
        HighVolumeSegment best = segments.stream()
                .max((a, b) -> Integer.compare(a.durationSeconds, b.durationSeconds))
                .orElse(null);

        if (best != null) {
            System.out.println(">>> [VolumeAnalyzer] " + segmentType + " found: " + 
                               best.startSecond + "s - " + best.endSecond + "s (" + best.durationSeconds + "s)");
        }

        return best;
    }

    private HighVolumeSegment findSegmentRelaxed(List<Double> levels, int start, int end, double threshold, int minDur) {
        List<HighVolumeSegment> segments = new ArrayList<>();
        int segStart = -1, segEnd = -1, gapCount = 0;

        for (int i = start; i < end; i++) {
            if (levels.get(i) > threshold) {
                if (segStart == -1) segStart = i;
                segEnd = i;
                gapCount = 0;
            } else if (segStart != -1) {
                gapCount++;
                if (gapCount > GAP_TOLERANCE) {
                    if ((segEnd - segStart) >= minDur) {
                        segments.add(new HighVolumeSegment(segStart, segEnd, segEnd - segStart));
                    }
                    segStart = -1; segEnd = -1; gapCount = 0;
                }
            }
        }

        if (segStart != -1 && (segEnd - segStart) >= minDur) {
            segments.add(new HighVolumeSegment(segStart, segEnd, segEnd - segStart));
        }

        return segments.stream().max((a, b) -> Integer.compare(a.durationSeconds, b.durationSeconds)).orElse(null);
    }

    /**
     * Detect intro/outro - main method
     */
    public IntroOutroResult detectIntroOutro(String videoPath) throws Exception {
        System.out.println(">>> [VolumeAnalyzer] Analyzing: " + videoPath.substring(videoPath.lastIndexOf("\\") + 1));

        double videoDuration = getVideoDuration(videoPath);
        if (videoDuration <= 0) {
            System.out.println(">>> [VolumeAnalyzer] ERROR: Could not get duration");
            return null;
        }

        System.out.println(">>> [VolumeAnalyzer] Duration: " + String.format("%.0f", videoDuration) + "s");

        List<Double> levels = extractVolumeLevels(videoPath, videoDuration);
        if (levels.isEmpty()) {
            System.out.println(">>> [VolumeAnalyzer] ERROR: No volume data");
            return null;
        }

        // Find INTRO
        HighVolumeSegment intro = findLoudestSegment(levels, 0, Math.min(INTRO_SEARCH_LIMIT, levels.size()), "INTRO");

        // Find OUTRO
        HighVolumeSegment outro = null;
        int outroStart = levels.size() - OUTRO_SEARCH_LIMIT;
        if (outroStart > INTRO_SEARCH_LIMIT) {
            outro = findLoudestSegment(levels, outroStart, levels.size(), "OUTRO");
            
            // Validate outro doesn't overlap intro
            if (outro != null && intro != null && outro.startSecond <= intro.endSecond + 60) {
                System.out.println(">>> [VolumeAnalyzer] OUTRO too close to INTRO, ignoring");
                outro = null;
            }
        }

        // Calculate trim points
        double trimStart = intro != null ? intro.endSecond + 2 : 0;
        double trimEnd = outro != null ? outro.startSecond - 2 : videoDuration;

        System.out.println(">>> [VolumeAnalyzer] === RESULT ===");
        System.out.println(">>> [VolumeAnalyzer] INTRO: " + (intro != null ? intro.startSecond + "s-" + intro.endSecond + "s" : "N/A"));
        System.out.println(">>> [VolumeAnalyzer] OUTRO: " + (outro != null ? outro.startSecond + "s-" + outro.endSecond + "s" : "N/A"));
        System.out.println(">>> [VolumeAnalyzer] TRIM: " + String.format("%.0f", trimStart) + "s - " + String.format("%.0f", trimEnd) + "s");

        return new IntroOutroResult(
                intro != null ? intro.startSecond : 0,
                intro != null ? intro.endSecond : 0,
                outro != null ? outro.startSecond : -1,
                outro != null ? outro.endSecond : -1,
                trimStart, trimEnd
        );
    }

    // Data Classes
    public static class HighVolumeSegment {
        public final int startSecond;
        public final int endSecond;
        public final int durationSeconds;
        public HighVolumeSegment(int start, int end, int duration) {
            this.startSecond = start;
            this.endSecond = end;
            this.durationSeconds = duration;
        }
    }

    public static class IntroOutroResult {
        public final double introStart;
        public final double introEnd;
        public final double outroStart;
        public final double outroEnd;
        public final double recommendedTrimStart;
        public final double recommendedTrimEnd;

        public IntroOutroResult(double introStart, double introEnd, double outroStart, double outroEnd,
                                double trimStart, double trimEnd) {
            this.introStart = introStart;
            this.introEnd = introEnd;
            this.outroStart = outroStart;
            this.outroEnd = outroEnd;
            this.recommendedTrimStart = trimStart;
            this.recommendedTrimEnd = trimEnd;
        }
    }
}
