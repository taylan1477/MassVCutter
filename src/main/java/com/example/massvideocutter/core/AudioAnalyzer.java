package com.example.massvideocutter.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Sadece FFmpeg'in silencedetect filtresinin çıktısını parse eder.
 */
public class AudioAnalyzer {

    public List<SilenceSegment> analyzeSilenceFromStream(Process ffmpegProcess) throws Exception {
        List<SilenceSegment> silences = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
            String line;
            Double start = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("silence_start:")) {
                    start = Double.parseDouble(line.split("silence_start:")[1].trim());
                } else if (line.contains("silence_end:")) {
                    String[] parts = line.split("silence_end:")[1].split("\\s+");
                    double end = Double.parseDouble(parts[0].trim());
                    silences.add(new SilenceSegment(start, end));
                    start = null;
                }
            }
        }

        ffmpegProcess.waitFor();
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
            return String.format("Silence [%.2f - %.2f]", start, end);
        }
    }
}
