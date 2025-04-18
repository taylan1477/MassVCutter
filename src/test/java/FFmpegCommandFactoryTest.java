import com.example.massvideocutter.core.ffmpeg.FFmpegCommandFactory;
import com.example.massvideocutter.core.ffmpeg.command.FFmpegCommandBuilder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class FFmpegCommandFactoryTest {

    @DisplayName("Format Uzantısına Göre Doğru Builder Döndürme")
    @ParameterizedTest(name = "[{index}] {0} uzantısı → {1}")
    @CsvSource({
            "mp4, MP4CommandBuilder",
            "ts, TSCommandBuilder",
            "MP4, MP4CommandBuilder",
            "mP4, MP4CommandBuilder"
    })
    void shouldReturnCorrectBuilderForExtension(String extension, String expectedBuilder) {
        FFmpegCommandBuilder builder = FFmpegCommandFactory.getBuilder(extension);

        assertEquals(expectedBuilder, builder.getClass().getSimpleName(),
                extension + " uzantısı için " + expectedBuilder + " bekleniyor");
    }

    @Test
    @DisplayName("Desteklenmeyen Format İçin Hata Fırlatma")
    void shouldThrowForUnsupportedFormats() {
        String[] unsupportedFormats = {"avi", "mov", "flv", ""};

        for (String format : unsupportedFormats) {
            assertThrows(UnsupportedOperationException.class, () ->
                            FFmpegCommandFactory.getBuilder(format),
                    format + " desteklenmeyen format olmalı");
        }
    }

    @Nested
    @DisplayName("Özel Durum Testleri")
    class SpecialCasesTest {

        @Test
        @DisplayName("Null uzantı için IllegalArgumentException fırlatmalı")
        void shouldThrowIllegalArgumentExceptionForNullExtension() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> FFmpegCommandFactory.getBuilder(null));

            assertEquals("Dosya uzantısı null olamaz", exception.getMessage());
        }

        @Test
        @DisplayName("Boş Uzantı İçin Hata")
        void shouldThrowOnEmptyExtension() {
            assertThrows(UnsupportedOperationException.class, () ->
                    FFmpegCommandFactory.getBuilder(""));
        }
    }
}