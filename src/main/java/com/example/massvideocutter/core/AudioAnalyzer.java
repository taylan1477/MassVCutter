package com.example.massvideocutter.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Sadece FFmpeg'in silencedetect filtresinin stderr çıktısını parse eder.
 */
public class AudioAnalyzer {

    public List<SilenceSegment> analyzeSilenceFromProcess(Process ffmpegProcess) throws Exception {
        List<SilenceSegment> silences = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ffmpegProcess.getErrorStream()))) {
            String line;
            Double start = null;
            while ((line = reader.readLine()) != null) {
                // Her satırı bastır
                System.out.println(">>> [DEBUG stderr] " + line);

                if (line.contains("silence_start:")) {
                    String[] parts = line.split("silence_start:");
                    start = Double.parseDouble(parts[1].trim());
                    System.out.println(">>> [DEBUG] Detected silence_start at " + start + "s");
                } else if (line.contains("silence_end:")) {
                    String[] parts = line.split("silence_end:");
                    String[] sub = parts[1].trim().split("\\s+");
                    double end = Double.parseDouble(sub[0]);
                    System.out.println(">>> [DEBUG] Detected silence_end at " + end + "s");
                    if (start != null) {
                        silences.add(new SilenceSegment(start, end));
                        System.out.println(">>> [DEBUG] Added SilenceSegment(" + start + "," + end + ")");
                        start = null;
                    }
                }
            }
        }

        int exit = ffmpegProcess.waitFor();
        System.out.println(">>> [DEBUG] silence-detect process exited with code " + exit);
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
