# DevDoctor Installation Scripts

This directory contains cross-platform installation scripts for DevDoctor.

## Quick Install

### macOS and Linux

```bash
curl -fsSL https://falniak95.github.io/devdoctor/install.sh | sh
```

Or download and run manually:

```bash
curl -O https://falniak95.github.io/devdoctor/install.sh
chmod +x install.sh
./install.sh
```

### Windows

```powershell
irm https://falniak95.github.io/devdoctor/install.ps1 | iex
```

Or download and run manually:

```powershell
Invoke-WebRequest -Uri https://falniak95.github.io/devdoctor/install.ps1 -OutFile install.ps1
.\install.ps1
```

## Usage

### macOS/Linux (`install.sh`)

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

### Windows (`install.ps1`)

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

## Requirements

- **Java**: JDK 11 or later must be installed and available in PATH
- **Network**: Internet connection to download releases from GitHub
- **Unix tools** (macOS/Linux): `curl` (usually pre-installed)
- **Windows**: PowerShell 5.1 or later (Windows 10+)

## Verification

After installation, verify that DevDoctor is working:

```bash
# macOS/Linux
devdoctor --version

# Windows
devdoctor --version
```

## Troubleshooting

### Java not found

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
```

**Windows:**
- Download and install from [Adoptium](https://adoptium.net/)
- Make sure Java is added to your system PATH

### PATH not configured

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

To your user PATH environment variable.

### Download fails

If the download fails:
- Check your internet connection
- Verify GitHub is accessible
- Try installing a specific version: `./install.sh --version v1.0.0`

### Permission denied

**macOS/Linux:**
```bash
chmod +x install.sh
```

**Windows:**
If PowerShell execution is blocked, run:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## Manual Installation

If you prefer to install manually:

1. Download `devdoctor.jar` from [GitHub Releases](https://github.com/falniak95/devdoctor/releases)
2. Place it in a directory of your choice
3. Create a wrapper script/batch file that runs `java -jar <path-to-jar> "$@"`
4. Add the wrapper to your PATH

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
