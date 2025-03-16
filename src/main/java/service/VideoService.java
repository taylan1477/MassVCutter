package main.java.service;

import main.java.model.VideoProcessor;

public class VideoService {
    private final VideoProcessor processor = new VideoProcessor();

    public void processVideo(String filePath) {
        processor.processVideo(filePath);
    }
}