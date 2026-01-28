# CI/CD Integration

DevDoctor is designed to work seamlessly in CI/CD pipelines. This page explains exit codes, JSON output, and provides examples for common CI platforms.

## Exit Codes

DevDoctor uses standard exit codes to indicate check results:

| Code | Meaning | When It Occurs |
|------|---------|----------------|
| `0` | Success | All checks passed |
| `1` | Failures detected | One or more checks failed, or a required check failed |
| `2` | Internal error | Config parsing error, unexpected exception, or other internal issue |

### Exit Code Behavior

- **Exit code 0**: All checks passed, environment is ready
- **Exit code 1**: Some checks failed - environment may not be ready
- **Exit code 2**: DevDoctor encountered an error - check logs for details

### Required Checks

If you configure `require_checks` in `.devdoctor.yml`, DevDoctor will exit with code `1` if any required check fails, even if other checks pass.

```yaml
require_checks:
  - system.java
  - project.java.requirements
```

## JSON Output

Use `--json` or `--json-pretty` for machine-readable output that can be parsed by CI scripts.

### Basic Usage

```bash
# Compact JSON
devdoctor check --json

# Pretty-printed JSON (easier to read)
devdoctor check --json-pretty
```

### JSON Structure

The JSON output includes:

- Project detection results
- All check results with status, summary, and details
- Config information (if used)
- Summary statistics

### Example JSON Output

```json
{
  "detection": {
    "root": "/path/to/project",
    "types": ["MAVEN", "DOCKER_COMPOSE"]
  },
  "config": {
    "path": "/path/to/project/.devdoctor.yml",
    "ignoreChecks": [],
    "requireChecks": ["system.java"]
  },
  "results": [
    {
      "id": "system.java",
      "status": "PASS",
      "summary": "Java is available (17.0.1)",
      "details": null,
      "suggestions": []
    },
    {
      "id": "system.docker",
      "status": "FAIL",
      "summary": "Docker is not available",
      "details": "Docker command not found in PATH",
      "suggestions": [
        "Install Docker from https://docs.docker.com/get-docker/"
      ]
    }
  ],
  "summary": {
    "total": 8,
    "passed": 7,
    "failed": 1,
    "warned": 0,
    "info": 0,
    "notApplicable": 0
  }
}
```

## GitHub Actions

### Basic Example

```yaml
name: DevDoctor Check

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Download DevDoctor
        run: |
          wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
      
      - name: Run DevDoctor checks
        run: java -jar devdoctor.jar check
```

### With JSON Output

```yaml
name: DevDoctor Check

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Download DevDoctor
        run: |
          wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
      
      - name: Run DevDoctor checks
        id: devdoctor
        run: |
          java -jar devdoctor.jar check --json > results.json
          cat results.json
        continue-on-error: true
      
      - name: Upload results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: devdoctor-results
          path: results.json
      
      - name: Fail if checks failed
        if: steps.devdoctor.outcome == 'failure'
        run: exit 1
```

### With Required Checks

```yaml
name: DevDoctor Check

on:
  push:
    branches: [ main ]

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Download DevDoctor
        run: |
          wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
      
      - name: Run DevDoctor checks
        run: java -jar devdoctor.jar check
        # Exit code 1 will fail the step if required checks fail
```

## GitLab CI

```yaml
devdoctor:
  image: openjdk:17
  before_script:
    - wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
  script:
    - java -jar devdoctor.jar check --json > results.json
  artifacts:
    when: always
    paths:
      - results.json
    expire_in: 1 week
```

## Jenkins

```groovy
pipeline {
    agent any
    
    stages {
        stage('DevDoctor Check') {
            steps {
                sh '''
                    wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
                    java -jar devdoctor.jar check
                '''
            }
        }
    }
}
```

## CircleCI

```yaml
version: 2.1

jobs:
  check:
    docker:
      - image: openjdk:17
    steps:
      - checkout
      - run:
          name: Download DevDoctor
          command: |
            wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
      - run:
          name: Run DevDoctor checks
          command: java -jar devdoctor.jar check

workflows:
  version: 2
  check:
    jobs:
      - check
```

## Best Practices

1. **Use JSON output** for programmatic processing
2. **Set up required checks** in `.devdoctor.yml` for critical dependencies
3. **Upload results as artifacts** for debugging failed builds
4. **Run checks early** in your pipeline to catch issues quickly
5. **Use `continue-on-error`** if you want to collect results even when checks fail

## Troubleshooting

### Exit Code 2

If DevDoctor exits with code 2, check:

- Config file syntax (`.devdoctor.yml`)
- Invalid check IDs in config
- File permissions
- Java version compatibility

### Parsing JSON Output

Example script to parse JSON results:

```bash
#!/bin/bash
java -jar devdoctor.jar check --json > results.json

# Extract failed checks
jq '.results[] | select(.status == "FAIL")' results.json

# Count failures
jq '.summary.failed' results.json
```

### Custom Error Handling

```bash
#!/bin/bash
java -jar devdoctor.jar check --json > results.json
EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
  echo "All checks passed"
elif [ $EXIT_CODE -eq 1 ]; then
  echo "Some checks failed"
  jq '.results[] | select(.status == "FAIL")' results.json
  exit 1
else
  echo "DevDoctor encountered an error"
  exit 2
fi
```
