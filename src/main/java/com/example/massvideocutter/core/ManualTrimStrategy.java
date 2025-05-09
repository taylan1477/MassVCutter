package com.example.massvideocutter.core;

public class ManualTrimStrategy implements TrimStrategy {
    private final TrimFacade trimFacade;

    public ManualTrimStrategy(TrimFacade facade) {
        this.trimFacade = facade;
    }

    @Override
    public boolean trim(String in, String out, double s, double e) {
        return trimFacade.trimVideo(in, out, s, e);
    }
}