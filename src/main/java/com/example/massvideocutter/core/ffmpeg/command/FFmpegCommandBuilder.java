package com.example.massvideocutter.core.ffmpeg.command;

import java.util.List;

public interface FFmpegCommandBuilder {
    List<String> buildCommand(String inputPath, String outputPath, String startTime, String duration);
}
