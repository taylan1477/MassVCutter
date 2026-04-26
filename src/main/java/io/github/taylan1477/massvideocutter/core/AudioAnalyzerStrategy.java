package io.github.taylan1477.massvideocutter.core;

import io.github.taylan1477.massvideocutter.core.ffmpeg.FFmpegWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Audio-based trim strategy with two modes:
 * 1. SILENCE mode (original) - detects silence gaps
 * 2. ANIME mode - detects loud intro/outro music segments
 */
public class AudioAnalyzerStrategy implements TrimStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AudioAnalyzerStrategy.class);

    private final TrimFacade trimFacade;
    private final FFmpegWrapper ffmpegWrapper;
    private final AudioAnalyzer silenceAnalyzer;
    private final VolumeAnalyzer volumeAnalyzer;
    private final double silenceThreshold;
    private final double minSilenceDuration;
    private final boolean animeMode;

    /**
     * Constructor for anime mode (volume-based detection)
     */
    public AudioAnalyzerStrategy(TrimFacade trimFacade, boolean animeMode) {
        this.trimFacade = trimFacade;
        this.ffmpegWrapper = new FFmpegWrapper();
        this.silenceAnalyzer = new AudioAnalyzer();
        this.volumeAnalyzer = new VolumeAnalyzer();
        this.silenceThreshold = -30.0;
        this.minSilenceDuration = 5.0;
        this.animeMode = animeMode;
    }

    /**
     * Original constructor for silence-based detection
     */
    public AudioAnalyzerStrategy(TrimFacade trimFacade,
                                 FFmpegWrapper ffmpegWrapper,
                                 AudioAnalyzer analyzer,
                                 double silenceThreshold,
                                 double minSilenceDuration) {
        this.trimFacade = trimFacade;
        this.ffmpegWrapper = ffmpegWrapper;
        this.silenceAnalyzer = analyzer;
        this.volumeAnalyzer = new VolumeAnalyzer();
        this.silenceThreshold = silenceThreshold;
        this.minSilenceDuration = minSilenceDuration;
        this.animeMode = false;
    }

    @Override
    public boolean trim(String inputPath, String outputPath, double ignoredStart, double ignoredEnd) {
        try {
            if (animeMode) {
                return trimAnimeMode(inputPath, outputPath);
            } else {
                return trimSilenceMode(inputPath, outputPath);
            }
        } catch (Exception e) {
            logger.error("Trim failed for: {}", inputPath, e);
            return false;
        }
    }

    /**
     * Anime mode: detect loud intro/outro music and trim content between them
     */
    private boolean trimAnimeMode(String inputPath, String outputPath) throws Exception {
        logger.info("Using ANIME mode (volume-based)");

        VolumeAnalyzer.IntroOutroResult result = volumeAnalyzer.detectIntroOutro(inputPath);

        if (result == null) {
            logger.warn("Could not detect intro/outro for: {}", inputPath);
            return false;
        }

        double trimStart = result.recommendedTrimStart;
        double trimEnd = result.recommendedTrimEnd;

        logger.info("Anime trim: {:.2f}s - {:.2f}s", trimStart, trimEnd);

        return trimFacade.trimVideo(inputPath, outputPath, trimStart, trimEnd);
    }

    /**
     * Silence mode: detect silence gaps at beginning/end (original behavior)
     */
    private boolean trimSilenceMode(String inputPath, String outputPath) throws Exception {
        logger.info("Using SILENCE mode");

        Process process = ffmpegWrapper.executeSilenceDetect(
                inputPath, silenceThreshold, minSilenceDuration
        );

        List<AudioAnalyzer.SilenceSegment> segments =
                silenceAnalyzer.analyzeSilenceFromProcess(process);

        if (segments.isEmpty()) {
            logger.warn("No silence segments found for: {}", inputPath);
            return false;
        }

        double trimStart = segments.getFirst().end;
        double trimEnd = segments.getLast().start;

        logger.info("Silence trim: {:.2f}s - {:.2f}s", trimStart, trimEnd);

        return trimFacade.trimVideo(inputPath, outputPath, trimStart, trimEnd);
    }

    /**
     * Detect intro/outro without trimming (for UI preview)
     */
    public VolumeAnalyzer.IntroOutroResult detectOnly(String inputPath) {
        try {
            return volumeAnalyzer.detectIntroOutro(inputPath);
        } catch (Exception e) {
            logger.error("Detection failed for: {}", inputPath, e);
            return null;
        }
    }
}
