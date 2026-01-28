package com.falniak.devdoctor.detect;

/**
 * Represents different types of projects that can be detected.
 */
public enum ProjectType {
    JAVA_MAVEN("Java (Maven)"),
    JAVA_GRADLE("Java (Gradle)"),
    NODE("Node.js"),
    DOCKER_COMPOSE("Docker Compose"),
    PYTHON_PYPROJECT("Python (pyproject)"),
    PYTHON_REQUIREMENTS("Python (requirements)"),
    PYTHON_PIPENV("Python (Pipenv)"),
    PYTHON_SETUPPY("Python (setup.py)"),
    GO_MODULES("Go (modules)"),
    DOTNET_SOLUTION(".NET (solution)"),
    DOTNET_CSHARP_PROJECT(".NET (C# project)"),
    DOTNET_FSHARP_PROJECT(".NET (F# project)"),
    RUST_CARGO("Rust (Cargo)");

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
