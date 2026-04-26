package io.github.taylan1477.massvideocutter.core;

public interface TrimStrategy {
    /**
     * @param inputPath  Input video file path
     * @param outputPath Output file path
     * @param start      Start time (seconds)
     * @param end        End time (seconds)
     * @return true if trim was successful
     */
    boolean trim(String inputPath, String outputPath, double start, double end);
}
