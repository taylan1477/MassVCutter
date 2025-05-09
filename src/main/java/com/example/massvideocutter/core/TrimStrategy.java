package com.example.massvideocutter.core;

public interface TrimStrategy {
    /**
     * @param inputPath  Girdi video dosyası
     * @param outputPath Çıktı dosya yolu
     * @param start      Başlangıç zamanı (saniye)
     * @param end        Bitiş zamanı (saniye)
     * @return Kırpma başarılı mı?
     */
    boolean trim(String inputPath, String outputPath, double start, double end);
}

