package io.github.taylan1477.massvideocutter.core.trimdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.taylan1477.massvideocutter.model.TrimRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client for communicating with the TrimDB REST API.
 * Uses Java's built-in {@link HttpClient} (no external dependencies).
 */
public class TrimDbApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TrimDbApiClient.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String baseUrl;

    public TrimDbApiClient() {
        this(DEFAULT_BASE_URL);
    }

    public TrimDbApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Search recipes by series name. If seriesQuery is null or empty, returns all recipes.
     */
    public List<TrimRecipe> searchRecipes(String seriesQuery) throws IOException, InterruptedException {
        String url = baseUrl + "/api/recipes";
        if (seriesQuery != null && !seriesQuery.isBlank()) {
            url += "?series=" + URLEncoder.encode(seriesQuery, StandardCharsets.UTF_8);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        logger.info("TrimDB API GET: {}", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            List<TrimRecipe> recipes = mapper.readValue(response.body(), new TypeReference<List<TrimRecipe>>() {});
            logger.info("TrimDB search returned {} recipes", recipes.size());
            return recipes;
        } else {
            logger.error("TrimDB search failed with status {}: {}", response.statusCode(), response.body());
            return Collections.emptyList();
        }
    }

    /**
     * Get a single recipe by its ID.
     */
    public TrimRecipe getRecipeById(long id) throws IOException, InterruptedException {
        String url = baseUrl + "/api/recipes/" + id;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        logger.info("TrimDB API GET: {}", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), TrimRecipe.class);
        } else {
            logger.error("TrimDB getRecipeById failed with status {}: {}", response.statusCode(), response.body());
            return null;
        }
    }

    /**
     * Upload (POST) a new recipe to the TrimDB server.
     * Returns the saved recipe with its server-assigned ID, or null on failure.
     */
    public TrimRecipe uploadRecipe(TrimRecipe recipe) throws IOException, InterruptedException {
        String url = baseUrl + "/api/recipes";
        String json = mapper.writeValueAsString(recipe);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        logger.info("TrimDB API POST: {} ({} episodes)", url, recipe.getEpisodeCount());

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            TrimRecipe saved = mapper.readValue(response.body(), TrimRecipe.class);
            logger.info("TrimDB upload successful. Recipe ID: {}", saved.getSeries());
            return saved;
        } else {
            logger.error("TrimDB upload failed with status {}: {}", response.statusCode(), response.body());
            return null;
        }
    }

    /**
     * Quick connectivity check — tries to reach the server.
     */
    public boolean isServerReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/recipes"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            logger.warn("TrimDB server not reachable at {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
