package io.github.taylan1477.trimdb.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "episode_trims")
public class EpisodeTrimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("ep")
    private Integer episode;
    private String title;
    private Double duration;

    private Double introStart;
    private Double introEnd;

    private Double outroStart;
    private Double outroEnd;
}
