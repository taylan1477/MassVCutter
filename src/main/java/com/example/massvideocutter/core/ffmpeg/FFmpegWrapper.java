package com.example.massvideocutter.core.ffmpeg;

import com.example.massvideocutter.core.ffmpeg.command.CompositeCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FFmpegWrapper {
    private static final Logger LOGGER = Logger.getLogger(FFmpegWrapper.class.getName());
    private static final String FFMPEG_PATH = "C:/Projeler/HelperTools/ffmpeg-7.1.1/bin/ffmpeg.exe";
    public static String getExecutablePath() {return FFMPEG_PATH;}

    public boolean trimVideo(String inputPath, String outputPath, String startTime, String duration) {
        String inputExt = getFileExtension(inputPath).toLowerCase();

        // Eğer mp4 değilse, outputPath'i .mp4'e çevir (BU ADIM ÖNCE OLMALI)
        if (!inputExt.equals("mp4")) {
            outputPath = outputPath.replaceAll("\\.[^.]+$", "") + ".mp4";
            System.out.println(">>> Adjusted outputPath to mp4: " + outputPath);
        }

        FFmpegCommandBuilder builder = FFmpegCommandFactory.getBuilder(inputExt);
        System.out.println(">>> Selected builder: " + builder.getClass().getSimpleName());

        List<List<String>> commandList;

        if (builder instanceof CompositeCommandBuilder compositeBuilder) {
            commandList = compositeBuilder.buildCommands(inputPath, outputPath, startTime, duration);
        } else {
            commandList = List.of(builder.buildCommand(inputPath, outputPath, startTime, duration));
        }

        for (List<String> command : commandList) {
            command.set(0, FFMPEG_PATH);
            System.out.println(">>> Running command: " + command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();

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
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IO error while executing FFmpeg command", e);
                return false;
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Thread interrupted while waiting for FFmpeg process", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1);
    }

    public Process executeSilenceDetect(String inputPath,
                                        double silenceThreshold,
                                        double minSilenceDuration) throws IOException {
        List<String> cmd = List.of(
                FFMPEG_PATH,
                "-i", inputPath,
                "-af", String.format(Locale.US, "silencedetect=n=%sdB:d=%f",
                        silenceThreshold, minSilenceDuration),
                "-f", "null", "-"
        );
        System.out.println(">>> [DEBUG] Running silence-detect command: " + String.join(" ", cmd));
        return new ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start();
    }

}
