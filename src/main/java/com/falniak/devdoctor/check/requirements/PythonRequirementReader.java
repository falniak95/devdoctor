package com.falniak.devdoctor.check.requirements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads Python version requirements from project files.
 * Priority order: .python-version, pyproject.toml [project] requires-python
 */
public class PythonRequirementReader {

    private static final Pattern PYPROJECT_REQUIRES_PYTHON_PATTERN = Pattern.compile(
        "requires-python\\s*=\\s*[\"']([^\"']+)[\"']"
    );

    /**
     * Reads the Python version requirement from the project root.
     *
     * @param projectRoot The project root directory
     * @return Optional Requirement if found, empty otherwise
     */
    public Optional<Requirement> read(Path projectRoot) {
        // Try .python-version first
        Path pythonVersionPath = projectRoot.resolve(".python-version");
        if (Files.exists(pythonVersionPath) && Files.isRegularFile(pythonVersionPath)) {
            try {
                String content = Files.readString(pythonVersionPath).trim();
                if (!content.isEmpty()) {
                    Integer major = VersionParser.parsePythonVersion(content);
                    return Optional.of(new Requirement("python", ".python-version", content, major));
                }
            } catch (IOException e) {
                // Continue to next source
            }
        }

        // Try pyproject.toml [project] requires-python
        Path pyprojectTomlPath = projectRoot.resolve("pyproject.toml");
        if (Files.exists(pyprojectTomlPath) && Files.isRegularFile(pyprojectTomlPath)) {
            try {
                String content = Files.readString(pyprojectTomlPath);
                // Simple line-based parsing - look for requires-python in [project] section
                boolean inProjectSection = false;
                for (String line : content.lines().toList()) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("[project]")) {
                        inProjectSection = true;
                        continue;
                    }
                    if (trimmed.startsWith("[") && !trimmed.startsWith("[project")) {
                        inProjectSection = false;
                        continue;
                    }
                    if (inProjectSection) {
                        var matcher = PYPROJECT_REQUIRES_PYTHON_PATTERN.matcher(trimmed);
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            Integer major = VersionParser.parsePythonVersion(version);
                            return Optional.of(new Requirement("python", "pyproject.toml", version, major));
                        }
                    }
                }
            } catch (IOException e) {
                // Continue (file might be invalid)
            }
        }

        return Optional.empty();
    }
}
