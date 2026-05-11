package io.github.taylan1477.massvideocutter.model;

/**
 * Represents the processing state of a video in the list.
 */
public enum ProcessState {
    PENDING,     // Henüz işlem görmedi
    PROCESSING,  // Şu an işleniyor
    SUCCESS,     // Başarıyla kırpıldı
    ERROR,       // Hata oluştu
    IGNORED      // Kullanıcı tarafından atlandı
}
