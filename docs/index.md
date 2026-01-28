# DevDoctor

<div align="center">
  <img src="assets/mascot.png" width="180" alt="DevDoctor Mascot">
</div>

**Project-aware CLI tool that diagnoses environment and setup issues before they become problems.**

DevDoctor automatically detects your project type (Java, Node.js, Docker, etc.) and runs smart checks to answer a simple question: **"Is my machine ready to run this project?"**

## Overview

DevDoctor helps developers:

- **Reduce onboarding friction** - Quickly identify what's needed to run a project
- **Catch environment issues early** - Before they cause problems during development
- **Get actionable suggestions** - Clear guidance on how to fix issues
- **Integrate with CI/CD** - Machine-readable JSON output for automation

## Features

- **🔍 Project Detection**: Automatically detects Maven, Gradle, Node.js, Docker Compose, and other project types
- **✅ Environment Checks**: Validates Java, Node.js, Docker, Git, and other required tools
- **📋 Version Requirements**: Checks if installed versions match project requirements (package.json, pom.xml, go.mod, etc.)
- **🔧 Fix Generation**: Automatically generates fix plans for common issues
- **📊 JSON Output**: Machine-readable output for automation and CI/CD integration
- **⚙️ Configurable**: Customize checks via `.devdoctor.yml` config file

## Quickstart

### Download

Download the latest release JAR from [GitHub Releases](https://github.com/falniak95/devdoctor/releases):

```bash
# Download the JAR file
wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar

# Or use curl
curl -L -o devdoctor.jar https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
```

### Verify Installation

```bash
java -jar devdoctor.jar --version
```

### Basic Usage

```bash
# Detect project type
java -jar devdoctor.jar detect

# Run all checks
java -jar devdoctor.jar check

# Run checks with verbose output
java -jar devdoctor.jar check --verbose

# Output as JSON for CI/CD
java -jar devdoctor.jar check --json
```

### Make it Executable (Optional)

You can create an alias or add to PATH for easier access:

**Linux/macOS:**
```bash
alias devdoctor='java -jar /path/to/devdoctor.jar'
```

**Windows (PowerShell):**
```powershell
Set-Alias devdoctor "java -jar C:\path\to\devdoctor.jar"
```

## Next Steps

- Learn about [Commands](commands.md) - Detailed CLI reference
- Configure DevDoctor with [Configuration](config.md) - `.devdoctor.yml` reference
- Understand [Checks](checks.md) - Available checks and their meanings
- Integrate with [CI/CD](ci.md) - Exit codes and automation examples

## Contributing

We welcome contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.
