package com.falniak.devdoctor.detect;

/**
 * Represents different types of projects that can be detected.
 */
public enum ProjectType {
    JAVA_MAVEN("Java (Maven)"),
    JAVA_GRADLE("Java (Gradle)"),
    NODE("Node.js"),
    DOCKER_COMPOSE("Docker Compose");

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this project type.
     *
     * @return The display name
     */
    public String displayName() {
        return displayName;
    }
}
