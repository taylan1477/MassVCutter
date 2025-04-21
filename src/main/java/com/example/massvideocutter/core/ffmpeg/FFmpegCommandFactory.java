package com.example.massvideocutter.core.ffmpeg;

import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.MP4CommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.CompositeCommandBuilder;
import com.example.massvideocutter.core.ffmpeg.command.RemuxToMp4Builder;

public class FFmpegCommandFactory {
    public static FFmpegCommandBuilder getBuilder(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("Dosya uzantısı null olamaz");
        }
        System.out.println(">>> Factory called with extension: " + extension);

        return switch (extension.toLowerCase()) {
            case "mp4" -> new MP4CommandBuilder();
            case "ts", "mkv" -> new CompositeCommandBuilder(
                    new RemuxToMp4Builder(),
                    new MP4CommandBuilder()
            ) {};
            default -> throw new UnsupportedOperationException("Desteklenmeyen format: " + extension);
        };
    }
}