package com.example.massvideocutter.core;

import java.util.HashMap;
import java.util.Map;

public class ManualTrimHandler {

    // Her video dosyası için start-end zamanlarını saklar
    private final Map<String, TrimPoints> trimPointsMap = new HashMap<>();

    public void setStartTime(String fileName, double timeInSeconds) {
        trimPointsMap.putIfAbsent(fileName, new TrimPoints());
        trimPointsMap.get(fileName).start = timeInSeconds;
    }

    public void setEndTime(String fileName, double timeInSeconds) {
        trimPointsMap.putIfAbsent(fileName, new TrimPoints());
        trimPointsMap.get(fileName).end = timeInSeconds;
    }

    public TrimPoints getTrimPoints(String fileName) {
        return trimPointsMap.get(fileName);
    }

    public Map<String, TrimPoints> getAllTrimPoints() {
        return trimPointsMap;
    }

    public static class TrimPoints {
        public double start = -1;
        public double end = -1;

        @Override
        public String toString() {
            return "Start: " + start + " | End: " + end;
        }
    }
}
