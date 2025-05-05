import com.example.massvideocutter.core.BatchProcessFacade;
import com.example.massvideocutter.core.TaskManager;
import com.example.massvideocutter.core.TrimFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BatchProcessFacadeTest {

    private FakeTrimFacade fakeTrimFacade;
    private BatchProcessFacade batchProcessFacade;

    @BeforeEach
    void setUp() {
        fakeTrimFacade = new FakeTrimFacade();
        TaskManager taskManager = new TaskManager();  // Şu an boşsa değişmeyecek ama ileride genişleyebilir
        batchProcessFacade = new BatchProcessFacade(fakeTrimFacade, taskManager);
    }

    @Test
    void testProcessAll_invokesTrimForEachFile() throws InterruptedException {
        // Arrange
        File file1 = new File("test1.mp4");
        File file2 = new File("test2.mp4");
        List<File> files = List.of(file1, file2);

        List<String> callbackLogs = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(files.size());

        // Act
        batchProcessFacade.processAll(files, 10.0, 20.0, (file, success, output) -> {
            callbackLogs.add(file.getName() + "-" + success);
            latch.countDown();  // Bitince bir azalt
        });

        // Latch ile tüm işlemleri bekle (max 5 saniye bekle ki kitlenirse fail atsın)
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Tüm işlemler zamanında tamamlanmalı");

        // Assert
        assertEquals(2, fakeTrimFacade.callCount);

        List<String> expectedLogs = List.of("test1.mp4-true", "test2.mp4-true");
        assertTrue(callbackLogs.containsAll(expectedLogs), "Tüm callback sonuçları alınmalı");
    }

    // --- Fake TrimFacade ---
    static class FakeTrimFacade extends TrimFacade {

        int callCount = 0;

        @Override
        public boolean trimVideo(String inputPath, String outputPath, double startTime, double endTime) {
            callCount++;
            return true;
        }
    }
}
