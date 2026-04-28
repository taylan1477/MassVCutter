package io.github.taylan1477.massvideocutter.core.trimdb;

import io.github.taylan1477.massvideocutter.model.EpisodeTrim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EpisodeMatcher {
    private static final Logger logger = LoggerFactory.getLogger(EpisodeMatcher.class);

    // Matches common episode patterns: "E01", "Ep 14", "- 05", "[04]", etc.
    private static final Pattern[] EPISODE_PATTERNS = {
            Pattern.compile("(?i)[^a-z0-9](?:ep|episode|e)[\\s\\-]*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\\s\\-_\\[\\(]+(\\d+)[\\s\\-_\\]\\)]+"), // Standalone number padded by spaces/brackets
            Pattern.compile("(\\d+)") // Fallback: first number found
    };

    /**
     * Attempts to find the best matching EpisodeTrim for the given video file.
     * 
     * @param videoFile The local video file
     * @param videoDuration The duration of the local video file in seconds
     * @param candidates The list of episodes from the TrimRecipe
     * @return An Optional containing the matched EpisodeTrim, or empty if no good match
     */
    public Optional<EpisodeTrim> match(File videoFile, double videoDuration, List<EpisodeTrim> candidates) {
        String filename = videoFile.getName();
        int extractedEp = extractEpisodeNumber(filename);
        
        logger.debug("Matching file: {} (extracted ep: {}, duration: {}s)", filename, extractedEp, videoDuration);

        EpisodeTrim bestMatch = null;
        double bestScore = -1.0;

        for (EpisodeTrim candidate : candidates) {
            double score = 0;
            
            // 1. Episode Number Match (High weight)
            if (extractedEp != -1 && candidate.getEp() != 0 && extractedEp == candidate.getEp()) {
                score += 5.0;
            }

            // 2. Duration Match (Medium weight)
            // If duration is within 2 seconds, it's a very strong match
            if (candidate.getDuration() != null) {
                double diff = Math.abs(videoDuration - candidate.getDuration());
                if (diff <= 2.0) {
                    score += 3.0;
                } else if (diff <= 5.0) {
                    score += 1.0;
                }
            }

            // If we have a good score, consider it a potential match
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        // Require at least a decent score to prevent wrong matches
        if (bestScore >= 3.0) {
            logger.info("Matched {} to episode {} (Score: {})", filename, bestMatch.getEp(), bestScore);
            return Optional.of(bestMatch);
        }

        logger.warn("No confident match found for {}", filename);
        return Optional.empty();
    }

    public static int extractEpisodeNumber(String filename) {
        // Clean filename extension and resolution first to avoid matching them as episode numbers (e.g. 1080p, 720p, 4k)
        String cleanName = filename.replaceAll("(?i)\\b(1080p|720p|480p|4k)\\b", "")
                                   .replaceAll("\\.(mp4|mkv|avi|ts|mov|webm|flv)$", "");

        for (Pattern pattern : EPISODE_PATTERNS) {
            Matcher m = pattern.matcher(cleanName);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1; // Not found
    }
}
