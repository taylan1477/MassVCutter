package com.example.massvideocutter.core;

import java.io.IOException;

public class FFmpegWrapper {
    public static void clipVideo(String inputPath, String outputPath, double startTime, double endTime) throws IOException {
        String command = String.format("ffmpeg -ss %.2f -i %s -t %.2f -c:v copy -c:a copy %s", startTime, inputPath, endTime, outputPath);
        Runtime.getRuntime().exec(command);
    }
}