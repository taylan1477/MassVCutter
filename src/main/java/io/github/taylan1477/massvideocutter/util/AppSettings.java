package io.github.taylan1477.massvideocutter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Application settings with JSON persistence.
 * Stored at ~/.massvideocutter/config.json
 */
public class AppSettings {

    private static final Logger logger = LoggerFactory.getLogger(AppSettings.class);
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".massvideocutter");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    // Singleton
    private static AppSettings instance;

    // Settings fields
    private String outputDirectory = ""; // empty = same folder as source with _trimmed subfolder
    private String ffmpegPath = "";      // empty = use default
    private String lastOpenedDirectory = "";

    private AppSettings() {}

    public static AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
            instance.load();
        }
        return instance;
    }

    /**
     * Get the output path for a trimmed video.
     * If outputDirectory is set, saves there. Otherwise saves to source_folder/_trimmed/
     */
    public String getOutputPath(String inputPath) {
        File inputFile = new File(inputPath);
        String baseName = inputFile.getName();
        int dotIdx = baseName.lastIndexOf('.');
        String nameWithoutExt = dotIdx > 0 ? baseName.substring(0, dotIdx) : baseName;
        String ext = dotIdx > 0 ? baseName.substring(dotIdx) : ".mp4";

        File targetDir;
        String baseTargetName;

        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            // Use configured output directory
            targetDir = new File(outputDirectory);
            if (!targetDir.exists()) targetDir.mkdirs();
            baseTargetName = nameWithoutExt + "_trimmed";
        } else {
            // Default: save next to original file
            targetDir = inputFile.getParentFile();
            baseTargetName = nameWithoutExt + "_cut";
        }

        File outFile = new File(targetDir, baseTargetName + ext);
        int counter = 1;
        while (outFile.exists()) {
            outFile = new File(targetDir, baseTargetName + "_" + counter + ext);
            counter++;
        }

        return outFile.getAbsolutePath();
    }

    public void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            // Simple JSON manually (no dependency needed)
            String json = "{\n" +
                    "  \"outputDirectory\": \"" + escapeJson(outputDirectory) + "\",\n" +
                    "  \"ffmpegPath\": \"" + escapeJson(ffmpegPath) + "\",\n" +
                    "  \"lastOpenedDirectory\": \"" + escapeJson(lastOpenedDirectory) + "\"\n" +
                    "}";

            Files.writeString(CONFIG_FILE, json);
            logger.info("Settings saved to: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to save settings", e);
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            logger.debug("No config file found, using defaults");
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            outputDirectory = extractJsonValue(json, "outputDirectory");
            ffmpegPath = extractJsonValue(json, "ffmpegPath");
            lastOpenedDirectory = extractJsonValue(json, "lastOpenedDirectory");
            logger.info("Settings loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to load settings", e);
        }
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\": \"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return unescapeJson(json.substring(start, end));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescapeJson(String value) {
        return value.replace("\\\\", "\\").replace("\\\"", "\"");
    }

    // Getters and setters
    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String dir) { this.outputDirectory = dir != null ? dir : ""; }

    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String path) { this.ffmpegPath = path != null ? path : ""; }

    public String getLastOpenedDirectory() { return lastOpenedDirectory; }
    public void setLastOpenedDirectory(String dir) { this.lastOpenedDirectory = dir != null ? dir : ""; }
}
