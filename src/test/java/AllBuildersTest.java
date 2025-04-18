import com.example.massvideocutter.core.ffmpeg.command.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AllBuildersTest {
    private static final String TEST_INPUT = "test_video.mp4";
    private static final String TEST_OUTPUT = "output.mp4";
    private static final String START_TIME = "00:01:00";
    private static final String DURATION = "00:00:30";

    @Nested
    @DisplayName("MP4 Komut Yapısı Testleri")
    class MP4CommandTests {
        @Test
        @DisplayName("Doğrudan stream kopyalama kullanmalı")
        void shouldUseDirectStreamCopy() {
            List<String> cmd = new MP4CommandBuilder()
                    .buildCommand(TEST_INPUT, TEST_OUTPUT, START_TIME, DURATION);

            assertTrue(cmd.contains("-c") && cmd.contains("copy"),
                    "MP4 için verimli stream kopyalama (-c copy) kullanılmalı");
        }
    }

    @Nested
    @DisplayName("TS Komut Yapısı Testleri")
    class TSCommandTests {
        @Test
        @DisplayName("Audio için AAC codec kullanmalı")
        void shouldUseAACForAudio() {
            List<String> cmd = new TSCommandBuilder()
                    .buildCommand(TEST_INPUT, TEST_OUTPUT, START_TIME, DURATION);

            assertAll(
                    "TS komut yapısı kontrolü",
                    () -> assertTrue(cmd.contains("-c:a") && cmd.contains("aac"),
                            "Audio için AAC codec kullanılmalı"),
                    () -> assertTrue(cmd.contains("-c:v") && cmd.contains("copy"),
                            "Video için direkt kopyalama kullanılmalı")
            );
        }
    }
}