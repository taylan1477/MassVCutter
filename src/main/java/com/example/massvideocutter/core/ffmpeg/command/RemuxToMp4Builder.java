package com.example.massvideocutter.core.ffmpeg.command;

import java.util.Arrays;
import java.util.List;

public class RemuxToMp4Builder implements FFmpegCommandBuilder {
    @Override
    public List<String> buildCommand(String input, String output, String unused1, String unused2) {
        return Arrays.asList(
                "ffmpeg",
                "-i", input,
                "-c", "copy",
                output
        );
    }
}
