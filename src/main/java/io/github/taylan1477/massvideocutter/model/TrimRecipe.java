package io.github.taylan1477.massvideocutter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"version", "series", "description", "contributor", "created", "episodeCount", "episodes"})
public class TrimRecipe {
    
    private int version = 1;
    private String series;
    private String description;
    private String contributor;
    private String created;
    private int episodeCount;
    private List<EpisodeTrim> episodes = new ArrayList<>();

    public TrimRecipe() {
        // Jackson constructor
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public List<EpisodeTrim> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<EpisodeTrim> episodes) {
        this.episodes = episodes;
        this.episodeCount = episodes != null ? episodes.size() : 0;
    }
}
