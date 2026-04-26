import io.github.taylan1477.massvideocutter.core.AudioAnalyzer;
import io.github.taylan1477.massvideocutter.core.AudioAnalyzerStrategy;
import io.github.taylan1477.massvideocutter.core.TrimFacade;
import io.github.taylan1477.massvideocutter.core.ffmpeg.FFmpegWrapper;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AudioAnalyzerStrategyTest {

    // Helper: dummy Process
    static class DummyProcess extends Process {
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream()   { return InputStream.nullInputStream(); }
        @Override public InputStream getErrorStream()   { return InputStream.nullInputStream(); }
        @Override public int waitFor()                  { return 0; }
        @Override public int exitValue()                { return 0; }
        @Override public void destroy()                 { }
    }

    // Stub FFmpegWrapper
    static class StubFFmpegWrapper extends FFmpegWrapper {
        @Override
        public Process executeSilenceDetect(String inputPath, double threshold, double duration) {
            // Return fake process, analyzer stub’a beslenecek
            return new DummyProcess();
        }
    }

    // Stub TrimFacade
    static class StubTrimFacade extends TrimFacade {
        String lastIn, lastOut;
        double lastStart, lastEnd;
        boolean returnValue;

        StubTrimFacade(boolean toReturn) { this.returnValue = toReturn; }

        @Override
        public boolean trimVideo(String inputPath, String outputPath, double start, double end) {
            this.lastIn = inputPath;
            this.lastOut = outputPath;
            this.lastStart = start;
            this.lastEnd = end;
            return returnValue;
        }
    }

    // Stub AudioAnalyzer
    static class StubAnalyzer extends AudioAnalyzer {
        private final List<SilenceSegment> segments;
        StubAnalyzer(List<SilenceSegment> segments) {
            this.segments = segments;
        }
        @Override
        public List<SilenceSegment> analyzeSilenceFromProcess(Process p) {
            return segments;
        }
    }

    @Test
    void testTrim_successful() {
        // Set up a stub segment list: [0-5] and [55-60]
        AudioAnalyzer.SilenceSegment seg1 = new AudioAnalyzer.SilenceSegment(0, 5);
        AudioAnalyzer.SilenceSegment seg2 = new AudioAnalyzer.SilenceSegment(55, 60);

        StubFFmpegWrapper ff = new StubFFmpegWrapper();
        StubTrimFacade tf = new StubTrimFacade(true);
        StubAnalyzer an = new StubAnalyzer(List.of(seg1, seg2));
        AudioAnalyzerStrategy strat = new AudioAnalyzerStrategy(
                tf, ff, an, -30.0, 0.5
        );

        boolean result = strat.trim("in.mp4", "out.mp4", 0, 0);
        assertTrue(result);
        // Verify correct start/end were passed
        assertEquals(5.0, tf.lastStart);
        assertEquals(55.0, tf.lastEnd);
        assertEquals("in.mp4", tf.lastIn);
        assertEquals("out.mp4", tf.lastOut);
    }

    @Test
    void testTrim_noSilence() {
        StubFFmpegWrapper ff = new StubFFmpegWrapper();
        StubTrimFacade tf = new StubTrimFacade(true);
        StubAnalyzer an = new StubAnalyzer(List.of());  // empty segments
        AudioAnalyzerStrategy strat = new AudioAnalyzerStrategy(
                tf, ff, an, -30.0, 0.5
        );

        boolean result = strat.trim("video.mp4", "out.mp4", 0, 0);
        assertFalse(result);
    }
}
