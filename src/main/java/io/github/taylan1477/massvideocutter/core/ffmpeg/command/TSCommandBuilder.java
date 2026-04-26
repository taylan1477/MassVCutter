package io.github.taylan1477.massvideocutter.core.ffmpeg.command;

import java.util.Arrays;
import java.util.List;

public class TSCommandBuilder implements FFmpegCommandBuilder {

    @Override
    public List<String> buildCommand(String inputPath, String outputPath, String startTime, String duration) {
        return Arrays.asList(
                "ffmpeg",
                "-i", inputPath,
                "-ss", startTime,
                "-t", duration,
                "-c:v", "copy",
                "-c:a", "aac", // TS format sometimes requires audio re-encoding
                outputPath
        );
    }
}
