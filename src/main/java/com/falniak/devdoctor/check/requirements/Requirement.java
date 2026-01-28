package com.falniak.devdoctor.check.requirements;

/**
 * Represents a version requirement discovered from a project file.
 *
 * @param language The programming language (e.g., "node", "python", "go", "java")
 * @param sourceFile The file where the requirement was found (e.g., ".nvmrc", "package.json")
 * @param rawValue The raw requirement string as found in the file
 * @param parsedMajor The parsed major version number, or null if unparseable
 */
public record Requirement(
    String language,
    String sourceFile,
    String rawValue,
    Integer parsedMajor
) {
}
