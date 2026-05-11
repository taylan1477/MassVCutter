package io.github.taylan1477.trimdb.controller;

import io.github.taylan1477.trimdb.entity.TrimRecipeEntity;
import io.github.taylan1477.trimdb.repository.TrimRecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final TrimRecipeRepository repository;

    @Autowired
    public RecipeController(TrimRecipeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TrimRecipeEntity> getAllRecipes(@RequestParam(required = false) String series) {
        if (series != null && !series.isEmpty()) {
            return repository.findBySeriesContainingIgnoreCase(series);
        }
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrimRecipeEntity> getRecipeById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TrimRecipeEntity createRecipe(@RequestBody TrimRecipeEntity recipe) {
        if (recipe.getEpisodes() != null) {
            recipe.setEpisodeCount(recipe.getEpisodes().size());
        }
        return repository.save(recipe);
    }
}
