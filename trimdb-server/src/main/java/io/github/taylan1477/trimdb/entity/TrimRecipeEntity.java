package io.github.taylan1477.trimdb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "trim_recipes")
public class TrimRecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer version;

    @Column(nullable = false)
    private String series;

    private String description;

    private String contributor;

    @CreationTimestamp
    private LocalDateTime created;

    private int episodeCount;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "recipe_id")
    private List<EpisodeTrimEntity> episodes = new ArrayList<>();
}
