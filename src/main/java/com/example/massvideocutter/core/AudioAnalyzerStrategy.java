package com.example.massvideocutter.core;

import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;

import java.util.List;

public class AudioAnalyzerStrategy implements TrimStrategy {
    private final TrimFacade trimFacade;
    private final FFmpegWrapper ffmpegWrapper;
    private final AudioAnalyzer analyzer;
    private final double silenceThreshold;   // dB cinsinden
    private final double minSilenceDuration; // saniye cinsinden

    public AudioAnalyzerStrategy(TrimFacade trimFacade, FFmpegWrapper ffmpegWrapper,
                                 double silenceThreshold, double minSilenceDuration) {
        this.trimFacade        = trimFacade;
        this.ffmpegWrapper     = ffmpegWrapper;
        this.analyzer          = new AudioAnalyzer();
        this.silenceThreshold   = silenceThreshold;
        this.minSilenceDuration = minSilenceDuration;
    }

    @Override
    public boolean trim(String inputPath, String outputPath, double ignoredStart, double ignoredEnd) {
        try {
            // 1. FFmpegWrapper ile silencedetect filtresi çalıştır
            List<String> cmd = List.of(
                    FFmpegWrapper.getExecutablePath(),
                    "-i", inputPath,
                    "-af", String.format("silencedetect=n=%fdB:d=%f", silenceThreshold, minSilenceDuration),
                    "-f", "null", "-"
            );
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            // 2. Analiz et
            List<AudioAnalyzer.SilenceSegment> segments = analyzer.analyzeSilenceFromStream(process);
            if (segments.isEmpty()) {
                System.out.println("Sessiz segment bulunamadı.");
                return false;
            }

            // 3. İlk sessiz segmentin bitişinden başlayıp, son sessiz segmentin başlangıcına kadar kırp
            double trimStart = segments.getFirst().end;
            double trimEnd   = segments.getLast().start;

            System.out.printf("Trim aralığı: %.2f - %.2f%n", trimStart, trimEnd);

            // 4. TrimFacade ile gerçek kırpma
            return trimFacade.trimVideo(inputPath, outputPath, trimStart, trimEnd);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public FFmpegWrapper getFfmpegWrapper() {
        return ffmpegWrapper;
    }
}
