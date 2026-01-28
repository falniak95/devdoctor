# Checks Reference

DevDoctor runs various checks to verify your environment and project setup. This page documents all available checks and their status meanings.

## Check Statuses

Each check can return one of the following statuses:

| Status | Meaning | Exit Code Impact |
|--------|---------|------------------|
| **PASS** | Check passed successfully | No impact |
| **WARN** | Potential issue detected, but not critical | No impact |
| **FAIL** | Check failed - action required | Causes exit code 1 |
| **INFO** | Informational message only | No impact |
| **NOT_APPLICABLE** | Check doesn't apply to this project | No impact (hidden by default) |

## System Checks

System checks verify that required tools are installed and available on your system.

### `system.java`

Checks if Java is installed and reports the version.

**Statuses:**
- **PASS**: Java is installed and accessible
- **FAIL**: Java is not installed or not in PATH

**Example output:**
```
✓ system.java: Java is available (17.0.1)
```

### `system.git`

Checks if Git is installed and reports the version.

**Statuses:**
- **PASS**: Git is installed and accessible
- **FAIL**: Git is not installed or not in PATH

**Example output:**
```
✓ system.git: Git is available (2.39.0)
```

### `system.node`

Checks if Node.js is installed and reports the version.

**Statuses:**
- **PASS**: Node.js is installed and accessible
- **FAIL**: Node.js is not installed or not in PATH

**Example output:**
```
✓ system.node: Node.js is available (v18.17.0)
```

### `system.docker`

Checks if Docker is installed and reports the version.

**Statuses:**
- **PASS**: Docker is installed and accessible
- **FAIL**: Docker is not installed or not in PATH

**Example output:**
```
✓ system.docker: Docker is available (24.0.5)
```

## Project Checks

Project checks provide information about detected projects and verify project-specific requirements.

### `project.java`

Provides information about detected Java projects (Maven, Gradle).

**Statuses:**
- **INFO**: Java project detected (Maven or Gradle)
- **NOT_APPLICABLE**: No Java project detected

**Example output:**
```
ℹ project.java: Maven project detected
```

### `project.node`

Provides information about detected Node.js projects.

**Statuses:**
- **INFO**: Node.js project detected (package.json found)
- **NOT_APPLICABLE**: No Node.js project detected

**Example output:**
```
ℹ project.node: Node.js project detected
```

### `project.compose`

Provides information about detected Docker Compose projects.

**Statuses:**
- **INFO**: Docker Compose project detected (docker-compose.yml found)
- **NOT_APPLICABLE**: No Docker Compose project detected

**Example output:**
```
ℹ project.compose: Docker Compose project detected
```

## Requirement Checks

Requirement checks verify that installed tool versions match project requirements.

### `project.java.requirements`

Checks if the installed Java version matches project requirements (from pom.xml or build.gradle).

**Statuses:**
- **PASS**: Java version matches requirements
- **WARN**: Java version may not match requirements (version parsing issue)
- **FAIL**: Java version does not meet requirements
- **NOT_APPLICABLE**: No Java project or no version requirements found

**Example output:**
```
✓ project.java.requirements: Java version 17.0.1 meets requirement >=17
```

### `project.node.requirements`

Checks if the installed Node.js version matches project requirements (from package.json engines field).

**Statuses:**
- **PASS**: Node.js version matches requirements
- **WARN**: Node.js version may not match requirements (version parsing issue)
- **FAIL**: Node.js version does not meet requirements
- **NOT_APPLICABLE**: No Node.js project or no version requirements found

**Example output:**
```
✓ project.node.requirements: Node.js version 18.17.0 meets requirement >=18.0.0
```

### `project.python.requirements`

Checks if the installed Python version matches project requirements (from requirements.txt or pyproject.toml).

**Statuses:**
- **PASS**: Python version matches requirements
- **WARN**: Python version may not match requirements (version parsing issue)
- **FAIL**: Python version does not meet requirements
- **NOT_APPLICABLE**: No Python project or no version requirements found

**Example output:**
```
✓ project.python.requirements: Python version 3.11.0 meets requirement >=3.9
```

### `project.go.requirements`

Checks if the installed Go version matches project requirements (from go.mod).

**Statuses:**
- **PASS**: Go version matches requirements
- **WARN**: Go version may not match requirements (version parsing issue)
- **FAIL**: Go version does not meet requirements
- **NOT_APPLICABLE**: No Go project or no version requirements found

**Example output:**
```
✓ project.go.requirements: Go version 1.21.0 meets requirement >=1.20
```

## Check Filtering

You can control which checks run using command-line flags:

- `--system-only`: Run only system checks
- `--project-only`: Run only project checks
- `--show-na`: Show NOT_APPLICABLE checks in output

## Configuring Checks

Use `.devdoctor.yml` to customize which checks run:

```yaml
# Skip specific checks
ignore_checks:
  - system.docker

# Require specific checks to pass
require_checks:
  - system.java
  - project.java.requirements
```

See [Configuration](config.md) for more details.

## Understanding Check Results

### Verbose Output

Use `--verbose` to see detailed information about each check:

```bash
devdoctor check --verbose
```

This shows:
- Detailed descriptions
- Suggestions for fixing issues
- Additional context about failures

### JSON Output

Use `--json` or `--json-pretty` for machine-readable output:

```bash
devdoctor check --json-pretty
```

This is useful for:
- CI/CD integration
- Automated reporting
- Parsing results programmatically

See [CI/CD Integration](ci.md) for examples.
