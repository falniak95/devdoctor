#!/bin/sh
# DevDoctor Installer for macOS and Linux
# POSIX-compliant shell script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
INSTALL_PREFIX="${HOME}/.devdoctor"
BIN_DIR="${HOME}/.local/bin"
VERSION="latest"
UNINSTALL=false

# GitHub API endpoint
REPO="falniak95/devdoctor"
API_BASE="https://api.github.com/repos/${REPO}"

# Print colored messages
info() {
    echo "${BLUE}ℹ${NC} $1"
}

success() {
    echo "${GREEN}✓${NC} $1"
}

warning() {
    echo "${YELLOW}⚠${NC} $1"
}

error() {
    echo "${RED}✗${NC} $1" >&2
}

# Print usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    --prefix <dir>     Installation directory (default: ~/.devdoctor)
    --version <tag>    Install specific version tag or 'latest' (default: latest)
    --uninstall        Remove DevDoctor installation
    -h, --help         Show this help message

Examples:
    $0
    $0 --prefix /opt/devdoctor
    $0 --version v1.0.0
    $0 --uninstall
EOF
}

# Parse arguments
while [ $# -gt 0 ]; do
    case "$1" in
        --prefix)
            INSTALL_PREFIX="$2"
            BIN_DIR="$(dirname "$2")/bin"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --uninstall)
            UNINSTALL=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Check if Java is available
check_java() {
    if ! command -v java >/dev/null 2>&1; then
        error "Java is not installed or not in PATH"
        error "Please install Java (JDK 11 or later) and try again"
        exit 1
    fi
    
    # Verify Java version (basic check)
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 11 ] 2>/dev/null; then
        warning "Java version may be too old. DevDoctor requires Java 11 or later."
    fi
}

# Get latest release info from GitHub API
get_release_info() {
    if [ "$VERSION" = "latest" ]; then
        API_URL="${API_BASE}/releases/latest"
    else
        API_URL="${API_BASE}/releases/tags/${VERSION}"
    fi
    
    # Check if curl is available
    if ! command -v curl >/dev/null 2>&1; then
        error "curl is required but not installed"
        error "Please install curl and try again"
        exit 1
    fi
    
    info "Fetching release information..."
    
    # Fetch release info
    RELEASE_JSON=$(curl -s -f "$API_URL" || {
        error "Failed to fetch release information"
        error "URL: $API_URL"
        error "Please check your internet connection and try again"
        exit 1
    })
    
    # Extract tag name and download URL
    TAG_NAME=$(echo "$RELEASE_JSON" | grep -o '"tag_name":"[^"]*"' | cut -d'"' -f4)
    DOWNLOAD_URL=$(echo "$RELEASE_JSON" | grep -o '"browser_download_url":"[^"]*devdoctor\.jar[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$TAG_NAME" ]; then
        error "Failed to parse release information"
        exit 1
    fi
    
    if [ -z "$DOWNLOAD_URL" ]; then
        error "devdoctor.jar asset not found in release ${TAG_NAME}"
        exit 1
    fi
    
    info "Found release: ${TAG_NAME}"
}

# Download the JAR file
download_jar() {
    info "Downloading devdoctor.jar..."
    
    # Create temporary file
    TEMP_FILE=$(mktemp)
    trap "rm -f $TEMP_FILE" EXIT
    
    # Download with progress
    if curl -f -L --progress-bar -o "$TEMP_FILE" "$DOWNLOAD_URL"; then
        success "Downloaded devdoctor.jar"
        JAR_FILE="$TEMP_FILE"
    else
        error "Failed to download devdoctor.jar"
        exit 1
    fi
}

# Install DevDoctor
install_devdoctor() {
    info "Installing DevDoctor..."
    
    # Create installation directory
    mkdir -p "$INSTALL_PREFIX"
    
    # Copy JAR file
    cp "$JAR_FILE" "${INSTALL_PREFIX}/devdoctor.jar"
    success "Installed JAR to ${INSTALL_PREFIX}/devdoctor.jar"
    
    # Create bin directory
    mkdir -p "$BIN_DIR"
    
    # Create wrapper script
    WRAPPER="${BIN_DIR}/devdoctor"
    {
        echo "#!/bin/sh"
        echo "exec java -jar \"${INSTALL_PREFIX}/devdoctor.jar\" \"\$@\""
    } > "$WRAPPER"
    
    # Make wrapper executable
    chmod +x "$WRAPPER"
    success "Created executable wrapper at ${WRAPPER}"
    
    # Check if bin directory is in PATH
    if ! echo "$PATH" | grep -q "${BIN_DIR}"; then
        warning "The directory ${BIN_DIR} is not in your PATH"
        echo ""
        info "To add it to your PATH, add this line to your shell profile:"
        echo ""
        if [ -n "$ZSH_VERSION" ]; then
            echo "  ${GREEN}echo 'export PATH=\"\${PATH}:${BIN_DIR}\"' >> ~/.zshrc${NC}"
            echo "  ${GREEN}source ~/.zshrc${NC}"
        elif [ -n "$BASH_VERSION" ]; then
            echo "  ${GREEN}echo 'export PATH=\"\${PATH}:${BIN_DIR}\"' >> ~/.bashrc${NC}"
            echo "  ${GREEN}source ~/.bashrc${NC}"
        else
            echo "  ${GREEN}export PATH=\"\${PATH}:${BIN_DIR}\"${NC}"
        fi
        echo ""
    else
        success "DevDoctor is ready to use!"
        echo ""
        info "Run: ${GREEN}devdoctor --version${NC} to verify installation"
    fi
}

# Uninstall DevDoctor
uninstall_devdoctor() {
    info "Uninstalling DevDoctor..."
    
    if [ -f "${INSTALL_PREFIX}/devdoctor.jar" ]; then
        rm -f "${INSTALL_PREFIX}/devdoctor.jar"
        success "Removed ${INSTALL_PREFIX}/devdoctor.jar"
    fi
    
    if [ -d "$INSTALL_PREFIX" ] && [ -z "$(ls -A "$INSTALL_PREFIX" 2>/dev/null)" ]; then
        rmdir "$INSTALL_PREFIX" 2>/dev/null || true
    fi
    
    if [ -f "${BIN_DIR}/devdoctor" ]; then
        rm -f "${BIN_DIR}/devdoctor"
        success "Removed ${BIN_DIR}/devdoctor"
    fi
    
    success "DevDoctor has been uninstalled"
}

# Main execution
main() {
    if [ "$UNINSTALL" = true ]; then
        uninstall_devdoctor
        exit 0
    fi
    
    check_java
    get_release_info
    download_jar
    install_devdoctor
    
    success "Installation complete!"
}

main "$@"
