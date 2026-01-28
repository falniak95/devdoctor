package com.falniak.devdoctor.check.requirements;

import java.util.regex.Pattern;

/**
 * Utility class for parsing version strings from various sources.
 * Uses pragmatic major-version-only comparison logic.
 */
public class VersionParser {

    // Pattern for extracting major version from Node versions: "18", "v18.19.0", "^20", "20.x", ">=18"
    private static final Pattern NODE_VERSION_PATTERN = Pattern.compile(
        "^(?:v|>=|\\^)?(\\d+)(?:\\.|x|$)"
    );

    // Pattern for extracting major.minor from Python versions: "3.11", ">=3.9"
    private static final Pattern PYTHON_VERSION_PATTERN = Pattern.compile(
        "^(?:>=)?(\\d+)\\.(\\d+)"
    );

    // Pattern for extracting minor version from Go versions: "go 1.21" -> 21
    private static final Pattern GO_VERSION_PATTERN = Pattern.compile(
        "go\\s+(\\d+)\\.(\\d+)"
    );

    // Pattern for extracting major version from Java versions: "17", "1.17"
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
        "^(?:1\\.)?(\\d+)$"
    );

    private VersionParser() {
        // Utility class
    }

    /**
     * Parses a Node.js version string and extracts the major version.
     * Supports formats: "18", "v18.19.0", "^20", "20.x", ">=18"
     *
     * @param version The version string to parse
     * @return The major version number, or null if unparseable
     */
    public static Integer parseNodeVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        var matcher = NODE_VERSION_PATTERN.matcher(version.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses a Python version string and extracts the major version.
     * Supports formats: "3.11", ">=3.9"
     *
     * @param version The version string to parse
     * @return The major version number, or null if unparseable
     */
    public static Integer parsePythonVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        var matcher = PYTHON_VERSION_PATTERN.matcher(version.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses a Python version string and extracts major.minor as a combined integer.
     * Supports formats: "3.11" -> 311, "3.9" -> 39
     * Used for exact version comparison (not for ">=" ranges).
     *
     * @param version The version string to parse
     * @return The combined major.minor version (major * 100 + minor), or null if unparseable
     */
    public static Integer parsePythonVersionMinor(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        var matcher = PYTHON_VERSION_PATTERN.matcher(version.trim());
        if (matcher.find()) {
            try {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                return major * 100 + minor; // 3.11 -> 311, 3.9 -> 39
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses a Go version string and extracts the major version (treats Go 1.x as major x).
     * Supports format: "go 1.21" -> extracts 21 as major
     *
     * @param version The version string to parse (e.g., "go 1.21")
     * @return The major version number (the minor part of Go version), or null if unparseable
     */
    public static Integer parseGoVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        var matcher = GO_VERSION_PATTERN.matcher(version.trim());
        if (matcher.find()) {
            try {
                // For Go, we treat the minor version (1.x) as the "major" for comparison
                // Go 1.21 -> major 21
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses a Java version string and extracts the major version.
     * Supports formats: "17", "1.17", "17.0.1" (all map to major 17)
     *
     * @param version The version string to parse
     * @return The major version number, or null if unparseable
     */
    public static Integer parseJavaVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        String trimmed = version.trim();
        // Try legacy format first (1.x)
        var matcher = JAVA_VERSION_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // Try modern format with patch versions (17.0.1 -> 17)
        java.util.regex.Pattern modernPattern = java.util.regex.Pattern.compile("^(\\d+)(?:\\.|$)");
        var modernMatcher = modernPattern.matcher(trimmed);
        if (modernMatcher.find()) {
            try {
                return Integer.parseInt(modernMatcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Compares a required version with a local version.
     * For ">=X" patterns, checks if local >= required.
     * For exact/range patterns, checks if local == required.
     *
     * @param requiredVersion The required version string
     * @param localVersion The local version string
     * @param parser The parser function to use (e.g., VersionParser::parseNodeVersion)
     * @return true if local version satisfies the requirement, false otherwise
     */
    public static boolean satisfiesRequirement(String requiredVersion, String localVersion, 
                                                java.util.function.Function<String, Integer> parser) {
        if (requiredVersion == null || localVersion == null) {
            return false;
        }

        Integer requiredMajor = parser.apply(requiredVersion);
        Integer localMajor = parser.apply(localVersion);

        if (requiredMajor == null || localMajor == null) {
            return false;
        }

        // Check if requirement is a minimum version (>=X)
        if (requiredVersion.trim().startsWith(">=")) {
            return localMajor >= requiredMajor;
        }

        // For exact/range patterns, check equality
        return localMajor.equals(requiredMajor);
    }
}
