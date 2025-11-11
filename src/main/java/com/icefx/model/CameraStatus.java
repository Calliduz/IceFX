package com.icefx.model;

/**
 * Enum representing the current status of the camera.
 */
public enum CameraStatus {
    DISCONNECTED("Camera Disconnected", "#e53935"),
    CONNECTING("Connecting...", "#fb8c00"),
    READY("Camera Ready", "#43a047"),
    DETECTING("Detecting Face...", "#1e88e5"),
    RECOGNIZED("Face Recognized", "#00897b"),
    FAILED("Recognition Failed", "#e53935");

    private final String displayText;
    private final String color;

    CameraStatus(String displayText, String color) {
        this.displayText = displayText;
        this.color = color;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getColor() {
        return color;
    }
}
