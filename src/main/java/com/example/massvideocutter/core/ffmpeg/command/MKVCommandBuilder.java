package com.example.massvideocutter.core.ffmpeg.command;

import java.util.Arrays;
import java.util.List;

public class MKVCommandBuilder implements FFmpegCommandBuilder {

    @Override
    public List<String> buildCommand(String inputPath, String outputPath, String startTime, String duration) {
        return Arrays.asList(
                "ffmpeg",
                "-i", inputPath,
                "-ss", startTime,
                "-t", duration,
                "-c", "copy",  // Video ve ses codec'lerini yeniden encode etmeden kopyala
                outputPath
        );
    }
}