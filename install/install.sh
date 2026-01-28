#!/bin/sh
# DevDoctor Installer for macOS and Linux (works in Git Bash too)
# POSIX-compliant shell script

set -eu

# --- Colors (only if stdout is a TTY) ---
if [ -t 1 ]; then
  RED='\033[0;31m'
  GREEN='\033[0;32m'
  YELLOW='\033[1;33m'
  BLUE='\033[0;34m'
  NC='\033[0m'
else
  RED=''
  GREEN=''
  YELLOW=''
  BLUE=''
  NC=''
fi

# --- Defaults ---
INSTALL_PREFIX="${HOME}/.devdoctor"
BIN_DIR="${HOME}/.local/bin"
VERSION="latest"
UNINSTALL=false

# GitHub
REPO="falniak95/devdoctor"
API_BASE="https://api.github.com/repos/${REPO}"

info()    { printf "%bℹ%b %s\n"  "$BLUE" "$NC" "$1"; }
success() { printf "%b✓%b %s\n"  "$GREEN" "$NC" "$1"; }
warning() { printf "%b⚠%b %s\n"  "$YELLOW" "$NC" "$1"; }
error()   { printf "%b✗%b %s\n"  "$RED" "$NC" "$1" >&2; }

usage() {
  cat <<EOF
Usage: $0 [OPTIONS]

Options:
  --prefix <dir>     Installation directory (default: ~/.devdoctor)
  --bin-dir <dir>    Where to place the 'devdoctor' wrapper (default: ~/.local/bin)
  --version <tag>    Install specific tag (e.g. v1.0.0) or 'latest' (default: latest)
  --uninstall        Remove DevDoctor installation
  -h, --help         Show this help message

Examples:
  $0
  $0 --version v1.0.0
  $0 --prefix "\$HOME/.devdoctor" --bin-dir "\$HOME/.local/bin"
  $0 --uninstall
EOF
}

# --- Arg parsing ---
while [ $# -gt 0 ]; do
  case "$1" in
    --prefix)
      [ $# -ge 2 ] || { error "--prefix requires a value"; exit 1; }
      INSTALL_PREFIX="$2"
      shift 2
      ;;
    --bin-dir)
      [ $# -ge 2 ] || { error "--bin-dir requires a value"; exit 1; }
      BIN_DIR="$2"
      shift 2
      ;;
    --version)
      [ $# -ge 2 ] || { error "--version requires a value"; exit 1; }
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

# --- Helpers ---
need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { error "Required command not found: $1"; exit 1; }
}

check_java() {
  if ! command -v java >/dev/null 2>&1; then
    error "Java is not installed or not in PATH."
    error "Please install Java (JDK 11+) and try again."
    exit 1
  fi

  # Best-effort version check
  JAVA_VER_LINE=$(java -version 2>&1 | head -n 1 || true)
  JAVA_VER=$(printf "%s" "$JAVA_VER_LINE" | sed -n 's/.*"\([^"]*\)".*/\1/p' | head -n 1)
  # Convert "1.8" -> "8"
  JAVA_MAJOR=$(printf "%s" "$JAVA_VER" | sed 's/^1\.//' | cut -d'.' -f1 | tr -cd '0-9')
  if [ -n "${JAVA_MAJOR:-}" ] && [ "$JAVA_MAJOR" -lt 11 ] 2>/dev/null; then
    warning "Java version appears old ($JAVA_VER). DevDoctor recommends Java 11+."
  fi
}

# mktemp on macOS differs; handle gracefully
mktemp_file() {
  if command -v mktemp >/dev/null 2>&1; then
    mktemp 2>/dev/null || mktemp -t devdoctor 2>/dev/null
  else
    # last resort
    echo "${TMPDIR:-/tmp}/devdoctor.$$.$(date +%s)"
  fi
}

fetch_release_json() {
  need_cmd curl

  if [ "$VERSION" = "latest" ]; then
    API_URL="${API_BASE}/releases/latest"
  else
    API_URL="${API_BASE}/releases/tags/${VERSION}"
  fi

  info "Fetching release information..."

  # Use GitHub token if provided (increases rate limit)
  AUTH_HEADER=""
  if [ -n "${GITHUB_TOKEN:-}" ]; then
    AUTH_HEADER="Authorization: token ${GITHUB_TOKEN}"
  fi

  # IMPORTANT: do NOT redirect stderr into stdout (breaks JSON)
  if [ -n "$AUTH_HEADER" ]; then
    RELEASE_JSON=$(curl -fsSL -H "$AUTH_HEADER" "$API_URL") || {
      error "Failed to fetch release information."
      error "URL: $API_URL"
      error "Tip: You can set GITHUB_TOKEN to avoid rate limits."
      exit 1
    }
  else
    RELEASE_JSON=$(curl -fsSL "$API_URL") || {
      error "Failed to fetch release information."
      error "URL: $API_URL"
      error "Tip: You can set GITHUB_TOKEN to avoid rate limits."
      exit 1
    }
  fi

  # Basic validation: should start with "{"
  first_char=$(printf "%s" "$RELEASE_JSON" | head -c 1 || true)
  if [ "$first_char" != "{" ]; then
    error "GitHub API did not return JSON."
    error "First 200 chars:"
    printf "%s" "$RELEASE_JSON" | head -c 200 | sed 's/^/  /'
    echo ""
    exit 1
  fi
}

# --- JSON parsing (jq preferred, python fallback, minimal grep fallback) ---
have_jq() { command -v jq >/dev/null 2>&1; }
have_py() { command -v python >/dev/null 2>&1 || command -v python3 >/dev/null 2>&1; }

pycmd() {
  if command -v python >/dev/null 2>&1; then
    echo "python"
  else
    echo "python3"
  fi
}

json_get_tag() {
  if have_jq; then
    printf "%s" "$RELEASE_JSON" | jq -r '.tag_name // empty' 2>/dev/null
    return 0
  fi
  if have_py; then
    PYC=$(pycmd)
    printf "%s" "$RELEASE_JSON" | "$PYC" -c "import json,sys; j=json.load(sys.stdin); print(j.get('tag_name',''))" 2>/dev/null
    return 0
  fi
  # last resort (fragile)
  printf "%s" "$RELEASE_JSON" | sed -n 's/.*"tag_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1
}

# Select best jar URL:
# 1) exact name devdoctor.jar
# 2) any name starting with devdoctor and ending with .jar
json_get_jar_url() {
  if have_jq; then
    url=$(printf "%s" "$RELEASE_JSON" | jq -r '.assets[]? | select(.name=="devdoctor.jar") | .browser_download_url' 2>/dev/null | head -n 1)
    if [ -n "${url:-}" ]; then
      printf "%s" "$url"
      return 0
    fi
    url=$(printf "%s" "$RELEASE_JSON" | jq -r '.assets[]? | select(.name | test("^devdoctor.*\\.jar$")) | .browser_download_url' 2>/dev/null | head -n 1)
    printf "%s" "$url"
    return 0
  fi

  if have_py; then
    PYC=$(pycmd)
    printf "%s" "$RELEASE_JSON" | "$PYC" -c "
import json,sys,re
j=json.load(sys.stdin)
assets=j.get('assets',[])
# prefer exact
for a in assets:
  if a.get('name')=='devdoctor.jar':
    print(a.get('browser_download_url','')); sys.exit(0)
# fallback: devdoctor*.jar
for a in assets:
  n=a.get('name','')
  if re.match(r'^devdoctor.*\.jar$', n):
    print(a.get('browser_download_url','')); sys.exit(0)
print('')
" 2>/dev/null
    return 0
  fi

  # last resort: try to find around name then url (very fragile)
  # Look for the first occurrence of devdoctor*.jar then grab a nearby browser_download_url
  # (Works only if JSON is compact; may fail)
  printf "%s" "$RELEASE_JSON" | grep -n "devdoctor" >/dev/null 2>&1 || { echo ""; return 0; }
  printf "%s" "$RELEASE_JSON" \
    | sed 's/[{}]/\n&\n/g' \
    | grep -A 20 '"name"[[:space:]]*:[[:space:]]*"devdoctor' \
    | grep -m 1 '"browser_download_url"' \
    | sed -n 's/.*"browser_download_url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
}

json_list_assets() {
  if have_jq; then
    printf "%s" "$RELEASE_JSON" | jq -r '.assets[]?.name' 2>/dev/null
    return 0
  fi
  if have_py; then
    PYC=$(pycmd)
    printf "%s" "$RELEASE_JSON" | "$PYC" -c "import json,sys; j=json.load(sys.stdin); print('\n'.join([a.get('name','') for a in j.get('assets',[])]))" 2>/dev/null
    return 0
  fi
  # last resort
  printf "%s" "$RELEASE_JSON" | sed -n 's/.*"name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
}

get_release_info() {
  fetch_release_json

  TAG_NAME=$(json_get_tag || true)
  if [ -z "${TAG_NAME:-}" ]; then
    error "Failed to parse release tag_name from GitHub API response."
    error "First 200 chars:"
    printf "%s" "$RELEASE_JSON" | head -c 200 | sed 's/^/  /'
    echo ""
    exit 1
  fi

  DOWNLOAD_URL=$(json_get_jar_url || true)
  if [ -z "${DOWNLOAD_URL:-}" ]; then
    error "Could not find a suitable DevDoctor jar asset in release ${TAG_NAME}."
    error "Expected asset name: devdoctor.jar (preferred) or devdoctor*.jar"
    assets=$(json_list_assets | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g' || true)
    if [ -n "${assets:-}" ]; then
      error "Available assets: $assets"
    fi
    exit 1
  fi

  info "Found release: ${TAG_NAME}"
}

download_jar() {
  info "Downloading DevDoctor jar..."

  TEMP_FILE=$(mktemp_file)
  # ensure cleanup
  trap 'rm -f "$TEMP_FILE" 2>/dev/null || true' EXIT INT TERM

  # -L follow redirects, -f fail on HTTP errors, -s silent, -S show errors
  if curl -fLsS -o "$TEMP_FILE" "$DOWNLOAD_URL"; then
    success "Downloaded jar"
    JAR_FILE="$TEMP_FILE"
  else
    error "Failed to download jar from:"
    error "  $DOWNLOAD_URL"
    exit 1
  fi
}

install_devdoctor() {
  info "Installing DevDoctor..."

  mkdir -p "$INSTALL_PREFIX"
  cp "$JAR_FILE" "${INSTALL_PREFIX}/devdoctor.jar"
  success "Installed jar to ${INSTALL_PREFIX}/devdoctor.jar"

  mkdir -p "$BIN_DIR"

  WRAPPER="${BIN_DIR}/devdoctor"
  {
    echo '#!/bin/sh'
    echo 'set -e'
    echo 'if ! command -v java >/dev/null 2>&1; then'
    echo '  echo "DevDoctor requires Java (JDK 11+). Please install Java and try again." >&2'
    echo '  exit 1'
    echo 'fi'
    echo "exec java -jar \"${INSTALL_PREFIX}/devdoctor.jar\" \"\$@\""
  } > "$WRAPPER"
  chmod +x "$WRAPPER"
  success "Created wrapper: $WRAPPER"

  # PATH guidance
  case ":$PATH:" in
    *":$BIN_DIR:"*)
      success "DevDoctor is ready!"
      info "Run: devdoctor --help"
      ;;
    *)
      warning "The directory $BIN_DIR is not in your PATH."
      echo ""
      info "Add it to your shell profile, e.g.:"
      echo "  echo 'export PATH=\"\$PATH:$BIN_DIR\"' >> ~/.bashrc"
      echo "  source ~/.bashrc"
      echo ""
      info "Then run: devdoctor --help"
      ;;
  esac
}

uninstall_devdoctor() {
  info "Uninstalling DevDoctor..."

  if [ -f "${INSTALL_PREFIX}/devdoctor.jar" ]; then
    rm -f "${INSTALL_PREFIX}/devdoctor.jar"
    success "Removed ${INSTALL_PREFIX}/devdoctor.jar"
  fi

  if [ -f "${BIN_DIR}/devdoctor" ]; then
    rm -f "${BIN_DIR}/devdoctor"
    success "Removed ${BIN_DIR}/devdoctor"
  fi

  # remove install dir if empty
  if [ -d "$INSTALL_PREFIX" ] && [ -z "$(ls -A "$INSTALL_PREFIX" 2>/dev/null || true)" ]; then
    rmdir "$INSTALL_PREFIX" 2>/dev/null || true
  fi

  success "Uninstalled."
}

main() {
  if [ "$UNINSTALL" = "true" ]; then
    uninstall_devdoctor
    exit 0
  fi

  check_java
  get_release_info
  download_jar
  install_devdoctor

  success "Installation complete."
}

main "$@"