<div align="center">
  <img src="assets/mascot.png" width="180" alt="DevDoctor Mascot">
</div>

# DevDoctor

**Project-aware CLI tool that diagnoses environment and setup issues before they become problems.**

DevDoctor automatically detects your project type (Java, Node.js, Docker, etc.) and runs smart checks to answer a simple question: **"Is my machine ready to run this project?"**

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
# Verify installation
java -jar devdoctor.jar --version
```

### Examples

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

## Commands

### `detect`

Detect what type of project you're working with:

```bash
devdoctor detect
devdoctor detect --path /path/to/project
```

**Flags:**
- `--path`, `-p`: Path to the project directory (default: current directory)

### `check`

Run environment and project checks:

```bash
# Run all checks (default)
devdoctor check

# Run only system-level checks
devdoctor check --system-only

# Run only project-level checks
devdoctor check --project-only

# Show detailed output with suggestions
devdoctor check --verbose

# Show not applicable checks
devdoctor check --show-na

# Output results as JSON
devdoctor check --json

# Pretty-printed JSON
devdoctor check --json-pretty

# Specify project path
devdoctor check --path /path/to/project

# Use custom config file
devdoctor check --config /path/to/.devdoctor.yml
```

**Flags:**
- `--path`, `-p`: Path to the project directory (default: current directory)
- `--system-only`: Run only system-level checks
- `--project-only`: Run only project-level checks
- `--verbose`: Show detailed output including details and suggestions
- `--show-na`: Show not applicable checks in output
- `--json`: Output results as JSON
- `--json-pretty`: Output results as pretty-printed JSON (implies --json)
- `--config`: Path to the config file (default: .devdoctor.yml in project root)

### `fix`

Generate and optionally apply fixes for check failures:

```bash
# Show fix plan
devdoctor fix

# Apply fixes interactively
devdoctor fix --apply

# Apply fixes automatically (skip confirmation)
devdoctor fix --apply --yes

# Specify project path
devdoctor fix --path /path/to/project
```

**Flags:**
- `--path`, `-p`: Path to the project directory (default: current directory)
- `--apply`: Apply safe fixes automatically
- `--yes`: Skip confirmation prompt when applying fixes

## Configuration

Create a `.devdoctor.yml` file in your project root to customize DevDoctor behavior:

```yaml
# Ignore specific checks
ignore_checks:
  - system.docker

# Require specific checks to pass (fails with exit code 1 if they fail)
require_checks:
  - system.docker
  - system.java
```

**Example:**

```yaml
ignore_checks:
  - system.docker  # Docker not required for this project

require_checks:
  - system.java    # Java must be installed
  - project.java.requirements  # Java version must match requirements
```

## CI Usage

DevDoctor is designed to work seamlessly in CI/CD pipelines.

### Exit Codes

- `0`: All checks passed
- `1`: Failures detected or required checks failed
- `2`: Internal error (config parsing error, unexpected exception)

### JSON Output

Use `--json` or `--json-pretty` for machine-readable output:

```bash
# In CI script
java -jar devdoctor.jar check --json > results.json

# Check exit code
if [ $? -eq 0 ]; then
  echo "All checks passed"
else
  echo "Some checks failed"
  exit 1
fi
```

### GitHub Actions Example

```yaml
- name: Run DevDoctor checks
  run: |
    wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
    java -jar devdoctor.jar check --json
```

## Documentation

📚 **Full documentation is available on [GitHub Pages](https://falniak95.github.io/devdoctor)**

The documentation site includes detailed guides on:
- [Commands](https://falniak95.github.io/devdoctor/commands/) - Complete CLI reference
- [Configuration](https://falniak95.github.io/devdoctor/config/) - Config file reference
- [Checks](https://falniak95.github.io/devdoctor/checks/) - Available checks and their meanings
- [CI/CD Integration](https://falniak95.github.io/devdoctor/ci/) - Exit codes and automation examples

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:

- How to run locally
- Branch naming conventions
- PR checklist
- Commit message style

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Built with care by [Furkan Alniak](https://github.com/falniak95)
