package io.github.taylan1477.massvideocutter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpisodeTrim {
    
    private int ep;
    private Double duration; // For matching
    
    // Optional trim points
    private Double introStart;
    private Double introEnd;
    private Double outroStart;
    private Double outroEnd;

    public EpisodeTrim() {
        // Jackson constructor
    }

    public EpisodeTrim(int ep, double duration) {
        this.ep = ep;
        this.duration = duration;
    }

    public int getEp() {
        return ep;
    }

    public void setEp(int ep) {
        this.ep = ep;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Double getIntroStart() {
        return introStart;
    }

    public void setIntroStart(Double introStart) {
        this.introStart = introStart;
    }

    public Double getIntroEnd() {
        return introEnd;
    }

    public void setIntroEnd(Double introEnd) {
        this.introEnd = introEnd;
    }

    public Double getOutroStart() {
        return outroStart;
    }

    public void setOutroStart(Double outroStart) {
        this.outroStart = outroStart;
    }

    public Double getOutroEnd() {
        return outroEnd;
    }

    public void setOutroEnd(Double outroEnd) {
        this.outroEnd = outroEnd;
    }
}
