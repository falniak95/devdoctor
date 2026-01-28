# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-28

### Added
- Initial release of DevDoctor CLI tool
- Project detection for Maven, Gradle, Node.js, Docker Compose, Go, and Python projects
- Environment checks for Java, Node.js, Docker, Git, Go, and Python
- Version requirement validation from project configuration files
  - Java: Reads from `pom.xml` and `build.gradle`
  - Node.js: Reads from `.nvmrc`, `.node-version`, and `package.json`
  - Python: Reads from `.python-version` and `pyproject.toml`
  - Go: Reads from `go.mod`
- Project-aware diagnostics with actionable suggestions
- JSON output support for automation and CI/CD integration
- Fix generation and application for common environment issues
- Configurable checks via `.devdoctor.yml` configuration file
- Console and JSON renderers for check results
- Support for system-only and project-only check modes
- Verbose output mode with detailed suggestions

### Features
- `detect` command: Identify project types and capabilities
- `check` command: Run comprehensive environment and project checks
- `fix` command: Generate and apply fixes for detected issues
- Cross-platform support (Windows, Linux, macOS)
- Lightweight standalone JAR distribution
