package com.example.massvideocutter.core.ffmpeg;

import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.MP4CommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.TSCommandBuilder;

public class FFmpegCommandFactory {

    public static FFmpegCommandBuilder getBuilder(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("Dosya uzantısı null olamaz");
        }

        return switch (extension.toLowerCase()) {
            case "mp4" -> new MP4CommandBuilder();
            case "ts" -> new TSCommandBuilder();
            default -> throw new UnsupportedOperationException("Desteklenmeyen format: " + extension);
        };
    }
}
