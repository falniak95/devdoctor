# DevDoctor Installer for Windows
# PowerShell script

param(
    [string]$Version = "latest",
    [switch]$Uninstall,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

# Colors for output (using Write-Host with colors)
function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# Print usage
function Show-Usage {
    Write-Host @"
Usage: .\install.ps1 [OPTIONS]

Options:
    -Version <tag>     Install specific version tag or 'latest' (default: latest)
    -Uninstall         Remove DevDoctor installation
    -Help              Show this help message

Examples:
    .\install.ps1
    .\install.ps1 -Version v1.0.0
    .\install.ps1 -Uninstall
"@
}

# GitHub API endpoint
$Repo = "falniak95/devdoctor"
$ApiBase = "https://api.github.com/repos/$Repo"

# Installation paths
$InstallDir = "$env:LOCALAPPDATA\DevDoctor"
$JarPath = "$InstallDir\devdoctor.jar"
$ShimPath = "$env:LOCALAPPDATA\DevDoctor\devdoctor.cmd"

# Check if Java is available
function Test-Java {
    try {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        if ($LASTEXITCODE -ne 0 -or -not $javaVersion) {
            throw "Java not found"
        }
        
        # Basic version check
        if ($javaVersion -match 'version "(\d+)') {
            $majorVersion = [int]$matches[1]
            if ($majorVersion -lt 11) {
                Write-Warning "Java version may be too old. DevDoctor requires Java 11 or later."
            }
        }
        
        Write-Success "Java is available"
    }
    catch {
        Write-Error "Java is not installed or not in PATH"
        Write-Error "Please install Java (JDK 11 or later) and try again"
        Write-Error "Download from: https://adoptium.net/"
        exit 1
    }
}

# Get release info from GitHub API
function Get-ReleaseInfo {
    if ($Version -eq "latest") {
        $apiUrl = "$ApiBase/releases/latest"
    }
    else {
        $apiUrl = "$ApiBase/releases/tags/$Version"
    }
    
    Write-Info "Fetching release information..."
    
    try {
        $releaseJson = Invoke-RestMethod -Uri $apiUrl -Method Get -ErrorAction Stop
        
        $script:TagName = $releaseJson.tag_name
        $script:DownloadUrl = ($releaseJson.assets | Where-Object { $_.name -eq "devdoctor.jar" }).browser_download_url
        
        if (-not $script:TagName) {
            Write-Error "Failed to parse release information"
            exit 1
        }
        
        if (-not $script:DownloadUrl) {
            Write-Error "devdoctor.jar asset not found in release $($script:TagName)"
            exit 1
        }
        
        Write-Info "Found release: $($script:TagName)"
    }
    catch {
        Write-Error "Failed to fetch release information"
        Write-Error "URL: $apiUrl"
        Write-Error "Error: $_"
        Write-Error "Please check your internet connection and try again"
        exit 1
    }
}

# Download the JAR file
function Get-JarFile {
    Write-Info "Downloading devdoctor.jar..."
    
    try {
        $tempFile = [System.IO.Path]::GetTempFileName()
        Invoke-WebRequest -Uri $script:DownloadUrl -OutFile $tempFile -ErrorAction Stop
        Write-Success "Downloaded devdoctor.jar"
        return $tempFile
    }
    catch {
        Write-Error "Failed to download devdoctor.jar"
        Write-Error "Error: $_"
        exit 1
    }
}

# Install DevDoctor
function Install-DevDoctor {
    Write-Info "Installing DevDoctor..."
    
    # Create installation directory
    if (-not (Test-Path $InstallDir)) {
        New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
    }
    
    # Copy JAR file
    $tempFile = Get-JarFile
    Copy-Item -Path $tempFile -Destination $JarPath -Force
    Remove-Item -Path $tempFile -Force
    Write-Success "Installed JAR to $JarPath"
    
    # Create CMD shim
    $shimContent = @"
@echo off
java -jar "%LOCALAPPDATA%\DevDoctor\devdoctor.jar" %*
"@
    
    Set-Content -Path $ShimPath -Value $shimContent -Encoding ASCII
    Write-Success "Created shim at $ShimPath"
    
    # Check if DevDoctor directory is in PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $devDoctorPath = "$env:LOCALAPPDATA\DevDoctor"
    
    if ($currentPath -notlike "*$devDoctorPath*") {
        Write-Warning "The directory $devDoctorPath is not in your PATH"
        Write-Host ""
        Write-Info "Would you like to add it to your user PATH? (Y/N)"
        $response = Read-Host
        
        if ($response -eq "Y" -or $response -eq "y") {
            try {
                $newPath = if ($currentPath) { "$currentPath;$devDoctorPath" } else { $devDoctorPath }
                [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
                Write-Success "Added $devDoctorPath to user PATH"
                Write-Warning "Please restart your terminal or run: `$env:Path = [System.Environment]::GetEnvironmentVariable('Path','User')"
            }
            catch {
                Write-Error "Failed to update PATH: $_"
                Write-Info "You can manually add $devDoctorPath to your PATH"
            }
        }
        else {
            Write-Info "To use DevDoctor, add this directory to your PATH:"
            Write-Host "  $devDoctorPath" -ForegroundColor Yellow
        }
    }
    else {
        Write-Success "DevDoctor is ready to use!"
        Write-Host ""
        Write-Info "Run: devdoctor --version to verify installation"
    }
}

# Uninstall DevDoctor
function Uninstall-DevDoctor {
    Write-Info "Uninstalling DevDoctor..."
    
    if (Test-Path $JarPath) {
        Remove-Item -Path $JarPath -Force
        Write-Success "Removed $JarPath"
    }
    
    if (Test-Path $ShimPath) {
        Remove-Item -Path $ShimPath -Force
        Write-Success "Removed $ShimPath"
    }
    
    if (Test-Path $InstallDir -and (Get-ChildItem $InstallDir -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) {
        Remove-Item -Path $InstallDir -Force -ErrorAction SilentlyContinue
    }
    
    # Remove from PATH if present
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $devDoctorPath = "$env:LOCALAPPDATA\DevDoctor"
    
    if ($currentPath -like "*$devDoctorPath*") {
        $newPath = ($currentPath -split ';' | Where-Object { $_ -ne $devDoctorPath }) -join ';'
        try {
            [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
            Write-Success "Removed $devDoctorPath from user PATH"
        }
        catch {
            Write-Warning "Could not remove $devDoctorPath from PATH: $_"
        }
    }
    
    Write-Success "DevDoctor has been uninstalled"
}

# Main execution
function Main {
    if ($Help) {
        Show-Usage
        exit 0
    }
    
    if ($Uninstall) {
        Uninstall-DevDoctor
        exit 0
    }
    
    Test-Java
    Get-ReleaseInfo
    Install-DevDoctor
    
    Write-Success "Installation complete!"
}

Main
