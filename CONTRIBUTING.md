# Contributing to DevDoctor

Thank you for your interest in contributing to DevDoctor! This document provides guidelines and instructions for contributing.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Git

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/falniak95/devdoctor.git
   cd devdoctor
   ```

2. **Run tests:**
   ```bash
   mvn test
   ```

3. **Build the project:**
   ```bash
   mvn package
   ```
   
   This creates `target/devdoctor.jar` which you can run:
   ```bash
   java -jar target/devdoctor.jar --version
   ```

4. **Run the application:**
   ```bash
   java -jar target/devdoctor.jar check
   ```

## Development Workflow

### Branch Naming

Use descriptive branch names with prefixes:

- `feat/*` - New features (e.g., `feat/add-python-check`)
- `fix/*` - Bug fixes (e.g., `fix/docker-check-error`)
- `chore/*` - Maintenance tasks (e.g., `chore/update-dependencies`)

### Commit Messages

Follow conventional commit message style:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(check): add Python version requirement check

Add support for checking Python version requirements from
requirements.txt and pyproject.toml files.

Closes #123
```

```
fix(check): handle missing docker command gracefully

Previously, if docker command was not found, the check would
throw an exception. Now it properly returns a FAIL status.

Fixes #456
```

## Pull Request Process

### Before Submitting

1. **Ensure tests pass:**
   ```bash
   mvn test
   ```

2. **Ensure code compiles:**
   ```bash
   mvn compile
   ```

3. **Check for style issues:**
   - Follow existing code style
   - Use meaningful variable and method names
   - Add JavaDoc comments for public APIs

### PR Checklist

- [ ] Code follows existing style and conventions
- [ ] Tests added/updated and passing (`mvn test`)
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow conventional format
- [ ] Branch is up to date with `main`
- [ ] No merge conflicts

### Submitting a PR

1. Push your branch to your fork
2. Create a pull request targeting the `main` branch
3. Fill out the PR template (if available)
4. Link any related issues
5. Wait for review and address feedback

## Code Style

- Use Java 17 features where appropriate
- Follow existing code patterns
- Keep methods focused and small
- Add JavaDoc for public classes and methods
- Use meaningful variable names

## Questions?

If you have questions or need help, please open an issue or reach out to the maintainers.

Thank you for contributing to DevDoctor! 🎉
