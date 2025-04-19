package com.example.massvideocutter.core.ffmpeg;

import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

public class FFmpegWrapper {
    private static final Logger LOGGER = Logger.getLogger(FFmpegWrapper.class.getName());

    public boolean trimVideo(String inputPath, String outputPath, String startTime, String duration) {
        String extension = getFileExtension(outputPath);
        FFmpegCommandBuilder builder = FFmpegCommandFactory.getBuilder(extension);
        List<String> command = builder.buildCommand(inputPath, outputPath, startTime, duration);

        // Replace the first command element with the full FFmpeg path
        String ffmpegPath = "C:/Projeler/ffmpeg-7.1.1/bin/ffmpeg.exe";
        command.set(0, ffmpegPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // Optionally log the FFmpeg output for debugging
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.log(Level.FINE, "FFmpeg output: {0}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.log(Level.WARNING, "FFmpeg process exited with non-zero code: {0}", exitCode);
                return false;
            }
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error while executing FFmpeg command", e);
            return false;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for FFmpeg process", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return false;
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1);
    }

    public static class TestFFmpeg {
        public static void main(String[] args) {
            FFmpegWrapper cutter = new FFmpegWrapper();
            boolean success = cutter.trimVideo(
                    "C:\\Users\\Taylan Özgür Özdemir\\Desktop\\Files\\Live Wallpaper\\Fight Club ts.ts",
                    "TS output.ts",
                    "00:00:01",
                    "00:00:10"
            );
            System.out.println("Kesme işlemi: " + (success ? "Başarılı" : "Başarısız"));
        }
    }
}