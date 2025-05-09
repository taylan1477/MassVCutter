package com.example.massvideocutter.core;

import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;

public class TrimFacade {
    private final FFmpegWrapper ffmpegWrapper;

    public TrimFacade() {
        this.ffmpegWrapper = new FFmpegWrapper();
    }

    public boolean trimVideo(String inputPath, String outputPath, double startSeconds, double endSeconds) {
        String start = formatSeconds(startSeconds);
        String duration = formatSeconds(endSeconds - startSeconds);
        return ffmpegWrapper.trimVideo(inputPath, outputPath, start, duration);
    }

    private String formatSeconds(double seconds) {
        int total = (int) seconds;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
