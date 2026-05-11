package io.github.taylan1477.trimdb.repository;

import io.github.taylan1477.trimdb.entity.TrimRecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrimRecipeRepository extends JpaRepository<TrimRecipeEntity, Long> {
    List<TrimRecipeEntity> findBySeriesContainingIgnoreCase(String series);
}
