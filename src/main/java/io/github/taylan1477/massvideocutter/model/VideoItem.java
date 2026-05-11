package io.github.taylan1477.massvideocutter.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.File;
import java.util.Objects;

/**
 * Wrapper model for video files in the list.
 * Tracks selection state (checkbox-like) and processing state.
 */
public class VideoItem {

    private File file;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final ObjectProperty<ProcessState> state = new SimpleObjectProperty<>(ProcessState.PENDING);
    private String errorMessage; // Tooltip for ERROR state

    public VideoItem(File file) {
        this.file = file;
    }

    // --- File ---
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    // --- Selected (checkbox-like toggle) ---
    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void toggleSelected() {
        selected.set(!selected.get());
    }

    // --- ProcessState ---
    public ProcessState getState() {
        return state.get();
    }

    public void setState(ProcessState state) {
        this.state.set(state);
    }

    public ObjectProperty<ProcessState> stateProperty() {
        return state;
    }

    // --- Error Message ---
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // --- Convenience ---
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoItem videoItem = (VideoItem) o;
        return Objects.equals(file, videoItem.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
