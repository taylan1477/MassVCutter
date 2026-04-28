package io.github.taylan1477.massvideocutter.core.trimdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.taylan1477.massvideocutter.model.TrimRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class RecipeManager {
    private static final Logger logger = LoggerFactory.getLogger(RecipeManager.class);
    private final ObjectMapper mapper;

    public RecipeManager() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportRecipe(File destination, TrimRecipe recipe) throws IOException {
        recipe.setCreated(Instant.now().toString());
        recipe.setEpisodeCount(recipe.getEpisodes() != null ? recipe.getEpisodes().size() : 0);
        
        try {
            mapper.writeValue(destination, recipe);
            logger.info("Exported trim recipe to {}", destination.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to export recipe", e);
            throw e;
        }
    }

    public TrimRecipe importRecipe(File source) throws IOException {
        try {
            TrimRecipe recipe = mapper.readValue(source, TrimRecipe.class);
            if (recipe == null || recipe.getEpisodes() == null) {
                throw new IOException("Invalid recipe file structure.");
            }
            logger.info("Imported trim recipe for series: {}", recipe.getSeries());
            return recipe;
        } catch (IOException e) {
            logger.error("Failed to import recipe", e);
            throw e;
        }
    }
}
