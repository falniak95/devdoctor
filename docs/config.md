# Configuration

DevDoctor can be configured using a `.devdoctor.yml` file in your project root. This allows you to customize which checks run and which are required.

## Configuration File

Create a `.devdoctor.yml` file in your project root:

```yaml
# Ignore specific checks
ignore_checks:
  - system.docker

# Require specific checks to pass (fails with exit code 1 if they fail)
require_checks:
  - system.docker
  - system.java
```

## Options

### `ignore_checks`

A list of check IDs to skip during execution. These checks will not run at all.

**Example:**
```yaml
ignore_checks:
  - system.docker      # Docker not required for this project
  - system.node       # Node.js not used
```

### `require_checks`

A list of check IDs that must pass. If any of these checks fail, DevDoctor will exit with code `1`, even if other checks pass.

**Example:**
```yaml
require_checks:
  - system.java                    # Java must be installed
  - project.java.requirements      # Java version must match requirements
  - system.docker                  # Docker must be available
```

## Check IDs

### System Checks

| ID | Description |
|----|-------------|
| `system.java` | Checks if Java is installed |
| `system.git` | Checks if Git is installed |
| `system.node` | Checks if Node.js is installed |
| `system.docker` | Checks if Docker is installed |

### Project Checks

| ID | Description |
|----|-------------|
| `project.java` | Provides information about detected Java projects |
| `project.node` | Provides information about detected Node.js projects |
| `project.compose` | Provides information about detected Docker Compose projects |
| `project.java.requirements` | Checks Java version against project requirements |
| `project.node.requirements` | Checks Node.js version against project requirements |
| `project.python.requirements` | Checks Python version against project requirements |
| `project.go.requirements` | Checks Go version against project requirements |

See [Checks](checks.md) for detailed information about each check.

## Examples

### Java Project

For a Java project that requires Java 17+ and Docker:

```yaml
require_checks:
  - system.java
  - system.docker
  - project.java.requirements

ignore_checks:
  - system.node
  - project.node
  - project.node.requirements
```

### Node.js Project

For a Node.js project that doesn't use Docker:

```yaml
require_checks:
  - system.node
  - project.node.requirements

ignore_checks:
  - system.docker
  - project.compose
```

### Multi-language Project

For a project with both Java and Node.js:

```yaml
require_checks:
  - system.java
  - system.node
  - project.java.requirements
  - project.node.requirements
```

## Custom Config Path

You can specify a custom config file path using the `--config` flag:

```bash
devdoctor check --config /path/to/custom-config.yml
```

## Config File Location

DevDoctor looks for `.devdoctor.yml` in the following order:

1. Path specified by `--config` flag (if provided)
2. Project root (detected by `detect` command)
3. Current directory (if no project detected)

## Validation

If the config file has syntax errors or invalid check IDs, DevDoctor will:

- Print an error message
- Exit with code `2` (internal error)
- Not run any checks

## Best Practices

1. **Commit `.devdoctor.yml`** to version control so all team members use the same configuration
2. **Use `require_checks`** for critical dependencies that must be present
3. **Use `ignore_checks`** for tools that aren't needed for your project
4. **Keep it simple** - only configure what you need
