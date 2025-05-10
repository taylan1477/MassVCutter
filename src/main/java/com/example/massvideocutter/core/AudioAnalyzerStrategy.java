package com.example.massvideocutter.core;

import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;

import java.util.List;
import java.util.Locale;

public class AudioAnalyzerStrategy implements TrimStrategy {
    private final TrimFacade trimFacade;
    private final FFmpegWrapper ffmpegWrapper;
    private final AudioAnalyzer analyzer;
    private final double silenceThreshold;   // dB cinsinden
    private final double minSilenceDuration; // saniye cinsinden


    public AudioAnalyzerStrategy(TrimFacade trimFacade,
                                 FFmpegWrapper ffmpegWrapper,
                                 double silenceThreshold,
                                 double minSilenceDuration) {
        this.trimFacade        = trimFacade;
        this.ffmpegWrapper     = ffmpegWrapper;
        this.analyzer          = new AudioAnalyzer();
        this.silenceThreshold  = silenceThreshold;
        this.minSilenceDuration = minSilenceDuration;
    }

    @Override
    public boolean trim(String inputPath, String outputPath, double ignoredStart, double ignoredEnd) {
        try {
            // FFmpegWrapper içine yeni metot ekleyelim:
            Process process = ffmpegWrapper.executeSilenceDetect(
                    inputPath, silenceThreshold, minSilenceDuration
            );
            List<String> cmd = List.of(
                    FFmpegWrapper.getExecutablePath(),
                    "-i", inputPath,
                    "-af", String.format(Locale.US, "silencedetect=n=%sdB:d=%f",
                            silenceThreshold, minSilenceDuration),
                    "-f", "null", "-"
            );

            // stderr üzerinden sessiz segmentleri al
            List<AudioAnalyzer.SilenceSegment> segments =
                    analyzer.analyzeSilenceFromProcess(process);

            if (segments.isEmpty()) {
                System.out.println("Sessiz segment bulunamadı.");
                return false;
            }

            // İlk ve son segmente index ile eriş
            double trimStart = segments.getFirst().end;
            double trimEnd   = segments.getLast().start;

            System.out.printf("Trim aralığı: %.2f - %.2f%n", trimStart, trimEnd);

            // Trim işlemi (TrimFacade zaten start/end string değil saniye bazlı overload da ekleyebilir)
            return trimFacade.trimVideo(inputPath, outputPath, trimStart, trimEnd);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
