package com.example.massvideocutter.core.ffmpeg.command;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeCommandBuilder implements FFmpegCommandBuilder {

    private final FFmpegCommandBuilder[] builders;

    public CompositeCommandBuilder(FFmpegCommandBuilder... builders) {
        this.builders = builders;
    }

    public List<List<String>> buildCommands(String inputPath, String outputPath, String startTime, String duration) {
        List<List<String>> commands = new ArrayList<>();
        String tempPath = outputPath.replaceAll("\\.[^.]+$", "") + "_tmp.mp4";

        for (int i = 0; i < builders.length; i++) {
            FFmpegCommandBuilder builder = builders[i];
            String in = (i == 0) ? inputPath : tempPath;
            String out = (i == builders.length - 1) ? outputPath : tempPath;

            List<String> cmd = builder.buildCommand(in, out, startTime, duration);
            commands.add(cmd);
        }

        return commands;
    }

    @Override
    public List<String> buildCommand(String inputPath, String outputPath, String startTime, String duration) {
        throw new UnsupportedOperationException("CompositeCommandBuilder does not use buildCommand()");
    }
}
