package com.example.massvideocutter.core.ffmpeg;

import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.MKVCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.MP4CommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.TSCommandBuilder;

public class FFmpegCommandFactory {
    public static FFmpegCommandBuilder getBuilder(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("Dosya uzantısı null olamaz");
        }
        System.out.println(">>> Factory called with extension: " + extension);

        return switch (extension.toLowerCase()) {
            case "mp4" -> new MP4CommandBuilder();
            case "ts" -> new TSCommandBuilder();
            case "mkv" -> new MKVCommandBuilder();
            default -> throw new UnsupportedOperationException("Desteklenmeyen format: " + extension);
        };
    }
}
