# Installation

DevDoctor can be installed on macOS, Linux, and Windows using automated installers or manually.

## Quick Install

### macOS and Linux

Install the latest version with a single command:

```bash
curl -fsSL https://falniak95.github.io/devdoctor/install.sh | sh
```

This will:
- Download the latest `devdoctor.jar` from GitHub Releases
- Install it to `~/.devdoctor/devdoctor.jar`
- Create an executable wrapper at `~/.local/bin/devdoctor`
- Provide instructions to add `~/.local/bin` to your PATH if needed

### Windows

Install using PowerShell:

```powershell
irm https://falniak95.github.io/devdoctor/install.ps1 | iex
```

This will:
- Download the latest `devdoctor.jar` from GitHub Releases
- Install it to `%LOCALAPPDATA%\DevDoctor\devdoctor.jar`
- Create a CMD shim at `%LOCALAPPDATA%\DevDoctor\devdoctor.cmd`
- Offer to add the directory to your user PATH

## Requirements

Before installing, ensure you have:

- **Java**: JDK 11 or later installed and available in your PATH
- **Network**: Internet connection to download releases from GitHub
- **Unix tools** (macOS/Linux): `curl` (usually pre-installed)
- **Windows**: PowerShell 5.1 or later (Windows 10+)

### Verifying Java Installation

Check if Java is installed:

```bash
# macOS/Linux
java -version

# Windows
java -version
```

If Java is not installed, download it from [Adoptium](https://adoptium.net/) or use your system's package manager.

## Installation Options

### macOS/Linux Installer

The `install.sh` script supports several options:

```bash
# Install latest version (default)
./install.sh

# Install to custom directory
./install.sh --prefix /opt/devdoctor

# Install specific version
./install.sh --version v1.0.0

# Uninstall
./install.sh --uninstall

# Show help
./install.sh --help
```

**Options:**

- `--prefix <dir>`: Installation directory (default: `~/.devdoctor`)
- `--version <tag|latest>`: Install specific version tag or 'latest' (default: latest)
- `--uninstall`: Remove DevDoctor installation
- `-h, --help`: Show help message

**Installation locations:**
- JAR file: `~/.devdoctor/devdoctor.jar` (or custom prefix)
- Executable wrapper: `~/.local/bin/devdoctor`

### Windows Installer

The `install.ps1` script supports:

```powershell
# Install latest version (default)
.\install.ps1

# Install specific version
.\install.ps1 -Version v1.0.0

# Uninstall
.\install.ps1 -Uninstall

# Show help
.\install.ps1 -Help
```

**Parameters:**

- `-Version <tag>`: Install specific version tag or 'latest' (default: latest)
- `-Uninstall`: Remove DevDoctor installation
- `-Help`: Show help message

**Installation locations:**
- JAR file: `%LOCALAPPDATA%\DevDoctor\devdoctor.jar`
- CMD shim: `%LOCALAPPDATA%\DevDoctor\devdoctor.cmd`

## Manual Installation

If you prefer to install manually or the automated installer doesn't work for your setup:

1. **Download the JAR file:**
   - Visit [GitHub Releases](https://github.com/falniak95/devdoctor/releases)
   - Download `devdoctor.jar` from the latest release

2. **Place the JAR file:**
   - Choose a location (e.g., `~/.devdoctor/` on Unix, `%LOCALAPPDATA%\DevDoctor\` on Windows)

3. **Create a wrapper script:**

   **macOS/Linux** (`~/.local/bin/devdoctor`):
   ```bash
   #!/bin/sh
   exec java -jar "$HOME/.devdoctor/devdoctor.jar" "$@"
   ```
   Make it executable: `chmod +x ~/.local/bin/devdoctor`

   **Windows** (`%LOCALAPPDATA%\DevDoctor\devdoctor.cmd`):
   ```batch
   @echo off
   java -jar "%LOCALAPPDATA%\DevDoctor\devdoctor.jar" %*
   ```

4. **Add to PATH:**
   - Ensure the directory containing the wrapper is in your PATH

## Verification

After installation, verify that DevDoctor is working:

```bash
devdoctor --version
```

You should see the installed version number. If you get a "command not found" error, see the [Troubleshooting](#troubleshooting) section below.

## Updating

To update to the latest version, simply run the installer again:

```bash
# macOS/Linux
curl -fsSL https://falniak95.github.io/devdoctor/install.sh | sh

# Windows
irm https://falniak95.github.io/devdoctor/install.ps1 | iex
```

Or install a specific version:

```bash
# macOS/Linux
./install.sh --version v1.2.0

# Windows
.\install.ps1 -Version v1.2.0
```

## Uninstallation

### macOS/Linux

```bash
./install.sh --uninstall
```

Or manually:
```bash
rm -rf ~/.devdoctor
rm ~/.local/bin/devdoctor
```

### Windows

```powershell
.\install.ps1 -Uninstall
```

Or manually:
1. Delete `%LOCALAPPDATA%\DevDoctor\devdoctor.jar`
2. Delete `%LOCALAPPDATA%\DevDoctor\devdoctor.cmd`
3. Remove `%LOCALAPPDATA%\DevDoctor` from your PATH (if added)

## Troubleshooting

### Java Not Found

If you see an error about Java not being found:

**macOS:**
```bash
# Install via Homebrew
brew install openjdk@11

# Or download from Adoptium
# https://adoptium.net/
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-11-jdk

# Fedora/RHEL
sudo dnf install java-11-openjdk-devel

# Arch Linux
sudo pacman -S jdk11-openjdk
```

**Windows:**
- Download and install from [Adoptium](https://adoptium.net/)
- Make sure Java is added to your system PATH
- Restart your terminal after installation

### Command Not Found

If `devdoctor` command is not found after installation:

**macOS/Linux:**

Add this line to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):
```bash
export PATH="${PATH}:${HOME}/.local/bin"
```

Then reload your shell:
```bash
source ~/.bashrc  # or ~/.zshrc
```

**Windows:**

The installer will prompt you to add the directory to PATH. If you skipped it, manually add:
```
%LOCALAPPDATA%\DevDoctor
```

To your user PATH environment variable:

1. Open System Properties → Environment Variables
2. Edit the "Path" variable under "User variables"
3. Add `%LOCALAPPDATA%\DevDoctor`
4. Restart your terminal

### Download Fails

If the download fails:

- Check your internet connection
- Verify GitHub is accessible from your network
- Try installing a specific version: `./install.sh --version v1.0.0`
- Check if your firewall or proxy is blocking the connection

### Permission Denied (macOS/Linux)

If you get a "permission denied" error:

```bash
chmod +x install.sh
./install.sh
```

### PowerShell Execution Policy (Windows)

If PowerShell blocks script execution:

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

This allows locally created scripts to run. For more information, see [PowerShell Execution Policies](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies).

### Custom Installation Directory Issues

If you use `--prefix` on macOS/Linux, make sure:

1. The directory is writable
2. The wrapper script path is updated accordingly
3. You update your PATH to include the bin directory in your custom prefix

## CI/CD Installation

For CI/CD pipelines, you can download the JAR directly:

```bash
# Download latest
wget https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar

# Or use curl
curl -L -o devdoctor.jar https://github.com/falniak95/devdoctor/releases/latest/download/devdoctor.jar
```

Then run:
```bash
java -jar devdoctor.jar check --json
```

See the [CI/CD Integration](ci.md) guide for more details.
