# Commands Reference

DevDoctor provides three main commands: `detect`, `check`, and `fix`. This page documents all available commands and their options.

## `detect`

Detect what type of project you're working with.

### Usage

```bash
devdoctor detect [OPTIONS]
```

### Options

| Flag | Short | Description | Default |
|------|-------|-------------|---------|
| `--path` | `-p` | Path to the project directory | Current directory |

### Examples

```bash
# Detect project in current directory
devdoctor detect

# Detect project in specific path
devdoctor detect --path /path/to/project
devdoctor detect -p ./my-project
```

### Output

```
Project root: /path/to/project
Detected project types:
  - Maven
  - Docker Compose
```

## `check`

Run environment and project checks to verify your setup.

### Usage

```bash
devdoctor check [OPTIONS]
```

### Options

| Flag | Short | Description | Default |
|------|-------|-------------|---------|
| `--path` | `-p` | Path to the project directory | Current directory |
| `--system-only` | | Run only system-level checks | `false` |
| `--project-only` | | Run only project-level checks | `false` |
| `--verbose` | | Show detailed output including details and suggestions | `false` |
| `--show-na` | | Show not applicable checks in output | `false` |
| `--json` | | Output results as JSON | `false` |
| `--json-pretty` | | Output results as pretty-printed JSON (implies --json) | `false` |
| `--config` | | Path to the config file | `.devdoctor.yml` in project root |

### Examples

**Basic usage:**
```bash
# Run all checks
devdoctor check

# Run checks in specific directory
devdoctor check --path /path/to/project
```

**Filter checks:**
```bash
# Only system-level checks (Java, Git, Node, Docker)
devdoctor check --system-only

# Only project-level checks (project info, requirements)
devdoctor check --project-only
```

**Output options:**
```bash
# Verbose output with suggestions
devdoctor check --verbose

# Show not applicable checks
devdoctor check --show-na

# JSON output for automation
devdoctor check --json

# Pretty-printed JSON
devdoctor check --json-pretty
```

**Custom config:**
```bash
# Use custom config file
devdoctor check --config /path/to/custom-config.yml
```

### Exit Codes

- `0`: All checks passed
- `1`: Failures detected or required checks failed
- `2`: Internal error (config parsing error, unexpected exception)

See [CI/CD Integration](ci.md) for more details on exit codes.

## `fix`

Generate and optionally apply fixes for check failures.

### Usage

```bash
devdoctor fix [OPTIONS]
```

### Options

| Flag | Short | Description | Default |
|------|-------|-------------|---------|
| `--path` | `-p` | Path to the project directory | Current directory |
| `--apply` | | Apply safe fixes automatically | `false` |
| `--yes` | | Skip confirmation prompt when applying fixes | `false` |

### Examples

**View fix plan:**
```bash
# Show what fixes would be applied
devdoctor fix

# Show fixes for specific project
devdoctor fix --path /path/to/project
```

**Apply fixes:**
```bash
# Apply fixes interactively (prompts for confirmation)
devdoctor fix --apply

# Apply fixes automatically (no confirmation)
devdoctor fix --apply --yes
```

### Fix Risk Levels

Fixes are categorized by risk level:

- **SAFE**: Automatically applied when using `--apply`
- **CAUTION**: Suggestions only, not applied automatically
- **MANUAL**: Requires manual intervention

Only SAFE fixes are applied automatically. CAUTION and MANUAL fixes are shown as suggestions.

## Global Options

All commands support these global options:

| Flag | Description |
|------|-------------|
| `--help` | Show help message |
| `--version` | Show version information |

### Examples

```bash
# Show help for main command
devdoctor --help

# Show help for specific command
devdoctor check --help

# Show version
devdoctor --version
```

## Command Combinations

**Common workflows:**
```bash
# Detect and check in one go
devdoctor detect && devdoctor check

# Check with verbose output and save to file
devdoctor check --verbose > check-results.txt

# Check and fix in CI
devdoctor check --json || devdoctor fix --apply --yes
```
