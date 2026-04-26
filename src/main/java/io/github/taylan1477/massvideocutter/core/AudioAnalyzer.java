package io.github.taylan1477.massvideocutter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses FFmpeg silencedetect filter stderr output to find silence segments.
 */
public class AudioAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(AudioAnalyzer.class);

    public List<SilenceSegment> analyzeSilenceFromProcess(Process ffmpegProcess) throws Exception {
        List<SilenceSegment> silences = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ffmpegProcess.getErrorStream()))) {
            String line;
            Double start = null;
            while ((line = reader.readLine()) != null) {
                logger.trace("FFmpeg stderr: {}", line);

                if (line.contains("silence_start:")) {
                    String[] parts = line.split("silence_start:");
                    start = Double.parseDouble(parts[1].trim());
                    logger.debug("Detected silence_start at {}s", start);
                } else if (line.contains("silence_end:")) {
                    String[] parts = line.split("silence_end:");
                    String[] sub = parts[1].trim().split("\\s+");
                    double end = Double.parseDouble(sub[0]);
                    logger.debug("Detected silence_end at {}s", end);
                    if (start != null) {
                        silences.add(new SilenceSegment(start, end));
                        logger.debug("Added SilenceSegment({}, {})", start, end);
                        start = null;
                    }
                }
            }
        }

        int exit = ffmpegProcess.waitFor();
        logger.debug("silence-detect process exited with code {}", exit);
        return silences;
    }

    public static class SilenceSegment {
        public final double start;
        public final double end;
        public SilenceSegment(double start, double end) {
            this.start = start;
            this.end   = end;
        }
        @Override
        public String toString() {
            return String.format("Silence[%.2f→%.2f]", start, end);
        }
    }
}
