package io.github.taylan1477.massvideocutter.core.ffmpeg;

import io.github.taylan1477.massvideocutter.core.ffmpeg.command.CompositeCommandBuilder;
import io.github.taylan1477.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FFmpegWrapper {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
    private static final String FFMPEG_PATH = "C:/Projeler/HelperTools/ffmpeg-7.1.1/bin/ffmpeg.exe";
    private static final long PROCESS_TIMEOUT_MINUTES = 10;

    public static String getExecutablePath() { return FFMPEG_PATH; }

    /**
     * Check if FFmpeg is available and executable.
     */
    public static boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(FFMPEG_PATH, "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.debug("FFmpeg not available at: {}", FFMPEG_PATH);
            return false;
        }
    }

    public boolean trimVideo(String inputPath, String outputPath, String startTime, String duration) {
        String inputExt = getFileExtension(inputPath).toLowerCase();

        // If not mp4, convert outputPath to .mp4
        if (!inputExt.equals("mp4")) {
            outputPath = outputPath.replaceAll("\\.[^.]+$", "") + ".mp4";
            logger.debug("Adjusted outputPath to mp4: {}", outputPath);
        }

        FFmpegCommandBuilder builder = FFmpegCommandFactory.getBuilder(inputExt);
        logger.debug("Selected builder: {}", builder.getClass().getSimpleName());

        List<List<String>> commandList;

        if (builder instanceof CompositeCommandBuilder compositeBuilder) {
            commandList = compositeBuilder.buildCommands(inputPath, outputPath, startTime, duration);
        } else {
            commandList = List.of(builder.buildCommand(inputPath, outputPath, startTime, duration));
        }

        for (List<String> command : commandList) {
            command.set(0, FFMPEG_PATH);
            logger.info("Running command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            try {
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.trace("FFmpeg output: {}", line);
                    }
                }

                boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                if (!finished) {
                    logger.error("FFmpeg process timed out after {} minutes, killing process", PROCESS_TIMEOUT_MINUTES);
                    process.destroyForcibly();
                    return false;
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    logger.warn("FFmpeg process exited with non-zero code: {}", exitCode);
                    return false;
                }
            } catch (IOException e) {
                logger.error("IO error while executing FFmpeg command", e);
                return false;
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while waiting for FFmpeg process", e);
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
        logger.debug("Running silence-detect command: {}", String.join(" ", cmd));
        return new ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start();
    }
}
