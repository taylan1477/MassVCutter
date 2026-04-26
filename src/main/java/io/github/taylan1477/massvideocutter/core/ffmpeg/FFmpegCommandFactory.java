package io.github.taylan1477.massvideocutter.core.ffmpeg;

import io.github.taylan1477.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;
import io.github.taylan1477.massvideocutter.core.ffmpeg.command.MP4CommandBuilder;
import io.github.taylan1477.massvideocutter.core.ffmpeg.command.CompositeCommandBuilder;
import io.github.taylan1477.massvideocutter.core.ffmpeg.command.RemuxToMp4Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegCommandFactory {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegCommandFactory.class);

    public static FFmpegCommandBuilder getBuilder(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("File extension cannot be null");
        }
        logger.debug("Factory called with extension: {}", extension);

        return switch (extension.toLowerCase()) {
            case "mp4" -> new MP4CommandBuilder();
            case "ts", "mkv" -> new CompositeCommandBuilder(
                    new RemuxToMp4Builder(),
                    new MP4CommandBuilder()
            ) {};
            default -> throw new UnsupportedOperationException("Unsupported format: " + extension);
        };
    }
}