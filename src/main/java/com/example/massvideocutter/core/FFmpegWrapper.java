package com.example.massvideocutter.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FFmpegWrapper {
    private static final Logger LOGGER = Logger.getLogger(FFmpegWrapper.class.getName());

    public boolean trimVideo(String inputPath, String outputPath, String startTime, String duration) {
        String ffmpegPath = "C:\\Projeler\\ffmpeg-7.1.1\\bin\\ffmpeg.exe";

        ProcessBuilder builder = new ProcessBuilder(
                ffmpegPath,
                "-hide_banner",      // Versiyon bilgisini gizle
                "-loglevel", "error", // Sadece hataları göster
                "-i", inputPath,
                "-ss", startTime,
                "-t", duration,
                "-c", "copy",
                outputPath
        );

        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            // Capture FFmpeg output for logging
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                LOGGER.log(Level.SEVERE, "FFmpeg process failed with exit code: " + exitCode);
                return false;
            }

            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO Error executing FFmpeg command", e);
            return false;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "FFmpeg process was interrupted", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return false;
        }
    }
    public static class TestFFmpeg {
        public static void main(String[] args) {
            FFmpegWrapper cutter = new FFmpegWrapper();
            boolean success = cutter.trimVideo(
                    "C:\\Users\\Taylan Özgür Özdemir\\Desktop\\Files\\Live Wallpaper\\Fight Club - Darkness.mp4",
                    "output.mp4",
                    "00:00:01",
                    "00:00:10"
            );
            System.out.println("Kesme işlemi: " + (success ? "Başarılı" : "Başarısız"));
        }
    }

}