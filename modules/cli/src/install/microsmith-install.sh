#!/usr/bin/env sh
set -eu

SCRIPT_NAME="microsmith-install.sh"
DEFAULT_INSTALL_ROOT="${HOME}/.microsmith"
DEFAULT_REPOSITORY="LMLiam/microsmith"
MINIMUM_JAVA_FEATURE=24

info() {
  printf '[microsmith-install] %s\n' "$*"
}

warn() {
  printf '[microsmith-install] warning: %s\n' "$*" >&2
}

fail() {
  printf '[microsmith-install] error: %s\n' "$*" >&2
  exit 1
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

usage() {
  cat <<EOF
Usage: ${SCRIPT_NAME} [options]

Options:
  --version <x.y.z>              Install explicit Microsmith release version.
  --install-root <path>          Install root (default: ${DEFAULT_INSTALL_ROOT}).
  --bin-dir <path>               Command shim directory (default: <install-root>/bin).
  --repository <owner/repo>      GitHub repository for release assets (default: ${DEFAULT_REPOSITORY}).
  --dist-url <url>               Explicit CLI distribution URL or file:// URL.
  --dist-file <path>             Explicit local CLI distribution archive.
  --dist-sha256 <hex>            Expected CLI distribution SHA-256 checksum.
  --runtime-url <url>            Explicit runtime archive URL or file:// URL.
  --runtime-file <path>          Explicit local runtime archive path.
  --runtime-sha256 <hex>         Expected runtime archive SHA-256 checksum.
  --force-runtime-provision      Always provision runtime into installation.
  --skip-runtime-provision       Never provision runtime (requires Java ${MINIMUM_JAVA_FEATURE}+ available).
  --no-profile-update            Do not add bin directory to shell profile.
  --help                         Print this help.

Environment overrides:
  MICROSMITH_INSTALL_VERSION
  MICROSMITH_INSTALL_ROOT
  MICROSMITH_INSTALL_BIN_DIR
  MICROSMITH_INSTALL_REPOSITORY
  MICROSMITH_INSTALL_DIST_URL
  MICROSMITH_INSTALL_DIST_FILE
  MICROSMITH_INSTALL_DIST_SHA256
  MICROSMITH_INSTALL_RUNTIME_URL
  MICROSMITH_INSTALL_RUNTIME_FILE
  MICROSMITH_INSTALL_RUNTIME_SHA256
EOF
}

sha256_file() {
  target="$1"
  if command_exists sha256sum; then
    sha256sum "$target" | awk '{print $1}'
    return 0
  fi

  if command_exists shasum; then
    shasum -a 256 "$target" | awk '{print $1}'
    return 0
  fi

  if command_exists openssl; then
    openssl dgst -sha256 "$target" | awk '{print $NF}'
    return 0
  fi

  fail "No SHA-256 tool found (sha256sum, shasum, or openssl)."
}

source_text() {
  source_ref="$1"
  case "$source_ref" in
    http://*|https://*)
      command_exists curl || fail "curl is required to download '${source_ref}'."
      curl -fsSL "$source_ref"
      ;;
    file://*)
      cat "${source_ref#file://}"
      ;;
    *)
      cat "$source_ref"
      ;;
  esac
}

copy_or_download() {
  source_ref="$1"
  destination="$2"

  case "$source_ref" in
    http://*|https://*)
      command_exists curl || fail "curl is required to download '${source_ref}'."
      curl -fsSL "$source_ref" -o "$destination"
      ;;
    file://*)
      cp "${source_ref#file://}" "$destination"
      ;;
    *)
      cp "$source_ref" "$destination"
      ;;
  esac
}

source_basename() {
  source_ref="$1"
  value="$source_ref"
  value="${value%%\?*}"
  case "$value" in
    file://*)
      value="${value#file://}"
      ;;
  esac
  basename "$value"
}

extract_archive() {
  archive_path="$1"
  output_dir="$2"

  mkdir -p "$output_dir"
  case "$archive_path" in
    *.tar.gz|*.tgz)
      command_exists tar || fail "tar is required to extract '$archive_path'."
      tar -xzf "$archive_path" -C "$output_dir"
      ;;
    *.zip)
      command_exists unzip || fail "unzip is required to extract '$archive_path'."
      unzip -q "$archive_path" -d "$output_dir"
      ;;
    *)
      fail "Unsupported archive type '$archive_path'."
      ;;
  esac
}

first_checksum_token() {
  printf '%s\n' "$1" | awk '{print $1; exit}'
}

detect_os() {
  uname_value="$(uname -s)"
  case "$uname_value" in
    Linux)
      printf 'linux\n'
      ;;
    Darwin)
      printf 'mac\n'
      ;;
    *)
      fail "Unsupported operating system '$uname_value'."
      ;;
  esac
}

detect_arch() {
  arch_value="$(uname -m)"
  case "$arch_value" in
    x86_64|amd64)
      printf 'x64\n'
      ;;
    aarch64|arm64)
      printf 'aarch64\n'
      ;;
    *)
      fail "Unsupported CPU architecture '$arch_value'."
      ;;
  esac
}

infer_version_from_text() {
  printf '%s\n' "$1" | sed -n 's#.*microsmith-cli-\([0-9A-Za-z._+-]\+\)-dist\.\(tar\.gz\|zip\).*#\1#p' | head -n 1
}

resolve_latest_version() {
  repository="$1"
  metadata="$(source_text "https://api.github.com/repos/${repository}/releases/latest")"
  tag_name="$(printf '%s\n' "$metadata" | awk -F'"' '/"tag_name"[[:space:]]*:/ {print $4; exit}')"
  [ -n "$tag_name" ] || fail "Unable to determine latest release tag from GitHub API."
  printf '%s\n' "${tag_name#v}"
}

java_feature_for() {
  java_cmd="$1"
  version_line="$("$java_cmd" -version 2>&1 | awk 'NR==1 {print; exit}')"
  version_raw="$(printf '%s\n' "$version_line" | sed -n 's/.*version "\(.*\)".*/\1/p')"
  if [ -z "$version_raw" ]; then
    printf '0\n'
    return 0
  fi

  feature="$(printf '%s\n' "$version_raw" | awk -F. '{if ($1 == "1") print $2; else print $1}' | sed 's/[^0-9].*$//')"
  if [ -z "$feature" ]; then
    printf '0\n'
  else
    printf '%s\n' "$feature"
  fi
}

resolve_system_java_cmd() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    printf '%s\n' "${JAVA_HOME}/bin/java"
    return 0
  fi

  if command_exists java; then
    command -v java
    return 0
  fi

  printf '\n'
}

runtime_metadata_from_api() {
  runtime_os="$1"
  runtime_arch="$2"
  metadata_url="https://api.adoptium.net/v3/assets/latest/24/hotspot?architecture=${runtime_arch}&heap_size=normal&image_type=jre&jvm_impl=hotspot&os=${runtime_os}&vendor=eclipse"
  source_text "$metadata_url"
}

parse_runtime_metadata() {
  if ! command_exists python3; then
    fail "python3 is required for runtime metadata parsing when no explicit runtime URL/file is provided."
  fi

  python3 -c '
import json
import sys

payload = json.load(sys.stdin)
if not payload:
    sys.exit(2)

package = payload[0].get("binary", {}).get("package", {})
link = package.get("link", "")
checksum = package.get("checksum", "")
if not link:
    sys.exit(3)

print(link)
print(checksum)
'
}

update_profile_path() {
  if [ "$NO_PROFILE_UPDATE" = "true" ]; then
    return 0
  fi

  case ":${PATH:-}:" in
    *":${BIN_DIR}:"*)
      return 0
      ;;
  esac

  shell_name="$(basename "${SHELL:-}")"
  profile_file="${HOME}/.profile"
  case "$shell_name" in
    zsh)
      profile_file="${HOME}/.zshrc"
      ;;
    bash)
      profile_file="${HOME}/.bashrc"
      ;;
  esac

  mkdir -p "$(dirname "$profile_file")"
  touch "$profile_file"

  marker_start="# >>> microsmith path >>>"
  marker_end="# <<< microsmith path <<<"
  if grep -F "$marker_start" "$profile_file" >/dev/null 2>&1; then
    return 0
  fi

  {
    printf '\n%s\n' "$marker_start"
    printf 'export PATH="%s:$PATH"\n' "$BIN_DIR"
    printf '%s\n' "$marker_end"
  } >> "$profile_file"
}

VERSION="${MICROSMITH_INSTALL_VERSION:-}"
INSTALL_ROOT="${MICROSMITH_INSTALL_ROOT:-${DEFAULT_INSTALL_ROOT}}"
BIN_DIR="${MICROSMITH_INSTALL_BIN_DIR:-}"
REPOSITORY="${MICROSMITH_INSTALL_REPOSITORY:-${DEFAULT_REPOSITORY}}"
DIST_URL="${MICROSMITH_INSTALL_DIST_URL:-}"
DIST_FILE="${MICROSMITH_INSTALL_DIST_FILE:-}"
DIST_SHA256="${MICROSMITH_INSTALL_DIST_SHA256:-}"
RUNTIME_URL="${MICROSMITH_INSTALL_RUNTIME_URL:-}"
RUNTIME_FILE="${MICROSMITH_INSTALL_RUNTIME_FILE:-}"
RUNTIME_SHA256="${MICROSMITH_INSTALL_RUNTIME_SHA256:-}"
FORCE_RUNTIME_PROVISION=false
SKIP_RUNTIME_PROVISION=false
NO_PROFILE_UPDATE=false

while [ "$#" -gt 0 ]; do
  case "$1" in
    --version)
      [ "$#" -ge 2 ] || fail "Missing value for --version."
      VERSION="$2"
      shift 2
      ;;
    --install-root)
      [ "$#" -ge 2 ] || fail "Missing value for --install-root."
      INSTALL_ROOT="$2"
      shift 2
      ;;
    --bin-dir)
      [ "$#" -ge 2 ] || fail "Missing value for --bin-dir."
      BIN_DIR="$2"
      shift 2
      ;;
    --repository)
      [ "$#" -ge 2 ] || fail "Missing value for --repository."
      REPOSITORY="$2"
      shift 2
      ;;
    --dist-url)
      [ "$#" -ge 2 ] || fail "Missing value for --dist-url."
      DIST_URL="$2"
      shift 2
      ;;
    --dist-file)
      [ "$#" -ge 2 ] || fail "Missing value for --dist-file."
      DIST_FILE="$2"
      shift 2
      ;;
    --dist-sha256)
      [ "$#" -ge 2 ] || fail "Missing value for --dist-sha256."
      DIST_SHA256="$2"
      shift 2
      ;;
    --runtime-url)
      [ "$#" -ge 2 ] || fail "Missing value for --runtime-url."
      RUNTIME_URL="$2"
      shift 2
      ;;
    --runtime-file)
      [ "$#" -ge 2 ] || fail "Missing value for --runtime-file."
      RUNTIME_FILE="$2"
      shift 2
      ;;
    --runtime-sha256)
      [ "$#" -ge 2 ] || fail "Missing value for --runtime-sha256."
      RUNTIME_SHA256="$2"
      shift 2
      ;;
    --force-runtime-provision)
      FORCE_RUNTIME_PROVISION=true
      shift 1
      ;;
    --skip-runtime-provision)
      SKIP_RUNTIME_PROVISION=true
      shift 1
      ;;
    --no-profile-update)
      NO_PROFILE_UPDATE=true
      shift 1
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option '$1'."
      ;;
  esac
done

if [ "$FORCE_RUNTIME_PROVISION" = "true" ] && [ "$SKIP_RUNTIME_PROVISION" = "true" ]; then
  fail "--force-runtime-provision and --skip-runtime-provision cannot be used together."
fi

[ -n "$BIN_DIR" ] || BIN_DIR="${INSTALL_ROOT}/bin"
mkdir -p "$INSTALL_ROOT"

if [ -z "$VERSION" ]; then
  if [ -n "$DIST_FILE" ]; then
    VERSION="$(infer_version_from_text "$DIST_FILE")"
  elif [ -n "$DIST_URL" ]; then
    VERSION="$(infer_version_from_text "$DIST_URL")"
  fi
fi
if [ -z "$VERSION" ]; then
  VERSION="$(resolve_latest_version "$REPOSITORY")"
fi

os_name="$(detect_os)"
if [ -z "$DIST_URL" ] && [ -z "$DIST_FILE" ]; then
  DIST_URL="https://github.com/${REPOSITORY}/releases/download/v${VERSION}/microsmith-cli-${VERSION}-dist.tar.gz"
fi

tmp_dir="$(mktemp -d)"
staged_install_dir=""
backup_install_dir=""
cleanup() {
  rm -rf "$tmp_dir"
  if [ -n "${staged_install_dir:-}" ] && [ -e "$staged_install_dir" ]; then
    rm -rf "$staged_install_dir"
  fi
  if [ -n "${backup_install_dir:-}" ] && [ -e "$backup_install_dir" ]; then
    rm -rf "$backup_install_dir"
  fi
}
trap cleanup EXIT INT TERM

dist_source_ref="$DIST_URL"
if [ -n "$DIST_FILE" ]; then
  dist_source_ref="$DIST_FILE"
fi
dist_archive_name="$(source_basename "$dist_source_ref")"
[ -n "$dist_archive_name" ] || dist_archive_name="microsmith-cli-dist.tar.gz"
dist_archive="${tmp_dir}/${dist_archive_name}"
if [ -n "$DIST_FILE" ]; then
  info "Using local distribution archive: ${DIST_FILE}"
  copy_or_download "$DIST_FILE" "$dist_archive"
else
  info "Downloading CLI distribution: ${DIST_URL}"
  copy_or_download "$DIST_URL" "$dist_archive"
fi

if [ -z "$DIST_SHA256" ]; then
  if [ -n "$DIST_FILE" ] && [ -f "${DIST_FILE}.sha256" ]; then
    DIST_SHA256="$(first_checksum_token "$(cat "${DIST_FILE}.sha256")")"
  elif [ -n "$DIST_URL" ]; then
    checksum_source="${DIST_URL}.sha256"
    if checksum_text="$(source_text "$checksum_source" 2>/dev/null)"; then
      DIST_SHA256="$(first_checksum_token "$checksum_text")"
    fi
  fi
fi

if [ -n "$DIST_SHA256" ]; then
  actual_dist_sha256="$(sha256_file "$dist_archive")"
  if [ "$actual_dist_sha256" != "$DIST_SHA256" ]; then
    fail "Distribution checksum mismatch: expected '$DIST_SHA256', got '$actual_dist_sha256'."
  fi
else
  warn "No distribution checksum provided or discovered; integrity verification skipped."
fi

dist_extract_dir="${tmp_dir}/dist-extract"
extract_archive "$dist_archive" "$dist_extract_dir"
dist_root="$(find "$dist_extract_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1 || true)"
[ -n "$dist_root" ] || fail "Could not locate extracted distribution root."

install_dir="${INSTALL_ROOT}/installs/microsmith-cli-${VERSION}"
staged_install_dir="${INSTALL_ROOT}/installs/.microsmith-cli-${VERSION}.staging.$$"
mkdir -p "${INSTALL_ROOT}/installs"
rm -rf "$staged_install_dir"
mv "$dist_root" "$staged_install_dir"

system_java_cmd="$(resolve_system_java_cmd)"
system_java_feature=0
if [ -n "$system_java_cmd" ]; then
  system_java_feature="$(java_feature_for "$system_java_cmd")"
fi

should_provision_runtime="$FORCE_RUNTIME_PROVISION"
if [ "$should_provision_runtime" = "false" ] && [ "$system_java_feature" -lt "$MINIMUM_JAVA_FEATURE" ]; then
  should_provision_runtime=true
fi

if [ "$should_provision_runtime" = "true" ]; then
  if [ "$SKIP_RUNTIME_PROVISION" = "true" ]; then
    fail "Runtime provisioning was disabled but Java ${MINIMUM_JAVA_FEATURE}+ is unavailable."
  fi

  runtime_source_url="$RUNTIME_URL"
  runtime_source_file="$RUNTIME_FILE"
  runtime_checksum="$RUNTIME_SHA256"
  runtime_arch="$(detect_arch)"

  if [ -z "$runtime_source_url" ] && [ -z "$runtime_source_file" ]; then
    metadata_json="$(runtime_metadata_from_api "$os_name" "$runtime_arch")"
    metadata_values="$(printf '%s' "$metadata_json" | parse_runtime_metadata)"
    runtime_source_url="$(printf '%s\n' "$metadata_values" | awk 'NR==1 {print}')"
    runtime_checksum="$(printf '%s\n' "$metadata_values" | awk 'NR==2 {print}')"
  fi

  rm -rf "${staged_install_dir}/runtime"
  if [ -n "$runtime_source_file" ] && [ -d "$runtime_source_file" ]; then
    runtime_source_dir="$(CDPATH= cd -- "$runtime_source_file" && pwd -P)"
    info "Using local runtime directory: ${runtime_source_dir}"
    cp -R "$runtime_source_dir" "${staged_install_dir}/runtime"
  else
    runtime_source_ref="$runtime_source_url"
    if [ -n "$runtime_source_file" ]; then
      runtime_source_ref="$runtime_source_file"
    fi
    runtime_archive_name="$(source_basename "$runtime_source_ref")"
    [ -n "$runtime_archive_name" ] || runtime_archive_name="runtime.tar.gz"
    runtime_archive="${tmp_dir}/${runtime_archive_name}"
    if [ -n "$runtime_source_file" ]; then
      info "Using local runtime archive: ${runtime_source_file}"
      copy_or_download "$runtime_source_file" "$runtime_archive"
      if [ -z "$runtime_checksum" ] && [ -f "${runtime_source_file}.sha256" ]; then
        runtime_checksum="$(first_checksum_token "$(cat "${runtime_source_file}.sha256")")"
      fi
    else
      info "Downloading Java ${MINIMUM_JAVA_FEATURE} runtime archive."
      copy_or_download "$runtime_source_url" "$runtime_archive"
      if [ -z "$runtime_checksum" ] && checksum_text="$(source_text "${runtime_source_url}.sha256" 2>/dev/null)"; then
        runtime_checksum="$(first_checksum_token "$checksum_text")"
      fi
    fi

    [ -n "$runtime_checksum" ] || fail "Runtime checksum not available for verification."
    runtime_actual_sha256="$(sha256_file "$runtime_archive")"
    if [ "$runtime_actual_sha256" != "$runtime_checksum" ]; then
      fail "Runtime checksum mismatch: expected '$runtime_checksum', got '$runtime_actual_sha256'."
    fi

    runtime_extract_dir="${tmp_dir}/runtime-extract"
    extract_archive "$runtime_archive" "$runtime_extract_dir"
    runtime_java_path="$(find "$runtime_extract_dir" -type f -path '*/bin/java' | head -n 1 || true)"
    [ -n "$runtime_java_path" ] || fail "Unable to locate runtime java binary in extracted archive."

    runtime_home="$(CDPATH= cd -- "$(dirname "$runtime_java_path")/.." && pwd)"
    mv "$runtime_home" "${staged_install_dir}/runtime"
  fi
fi

if [ -x "${staged_install_dir}/runtime/bin/java" ]; then
  final_java_feature="$(java_feature_for "${staged_install_dir}/runtime/bin/java")"
elif [ -n "$system_java_cmd" ]; then
  final_java_feature="$system_java_feature"
else
  final_java_feature=0
fi
[ "$final_java_feature" -ge "$MINIMUM_JAVA_FEATURE" ] ||
  fail "No usable Java ${MINIMUM_JAVA_FEATURE}+ runtime available after install."

if ! version_output="$("${staged_install_dir}/bin/microsmith" --version 2>&1)"; then
  if [ -z "$version_output" ]; then
    fail "Installed CLI failed health check (--version) with no output."
  fi
  fail "Installed CLI failed health check (--version): ${version_output}"
fi
[ -n "$version_output" ] || fail "Installed CLI failed health check (--version) with no output."

if [ -e "$install_dir" ]; then
  backup_install_dir="${INSTALL_ROOT}/installs/.microsmith-cli-${VERSION}.backup.$$"
  rm -rf "$backup_install_dir"
  mv "$install_dir" "$backup_install_dir"
fi

if mv "$staged_install_dir" "$install_dir"; then
  staged_install_dir=""
else
  if [ -n "$backup_install_dir" ] && [ -e "$backup_install_dir" ] && [ ! -e "$install_dir" ]; then
    mv "$backup_install_dir" "$install_dir" || true
    backup_install_dir=""
  fi
  fail "Unable to promote staged install into '${install_dir}'."
fi

if [ -n "$backup_install_dir" ] && [ -e "$backup_install_dir" ]; then
  rm -rf "$backup_install_dir"
  backup_install_dir=""
fi

current_link="${INSTALL_ROOT}/current"
current_link_tmp="${INSTALL_ROOT}/current.tmp.$$"
rm -f "$current_link_tmp"
ln -s "$install_dir" "$current_link_tmp"
mv -f "$current_link_tmp" "$current_link"

mkdir -p "$BIN_DIR"
shim_path="${BIN_DIR}/microsmith"
cat > "$shim_path" <<EOF
#!/usr/bin/env sh
set -eu
exec "${INSTALL_ROOT}/current/bin/microsmith" "\$@"
EOF
chmod +x "$shim_path"

update_profile_path

info "Installed ${version_output} at ${INSTALL_ROOT}."
if command_exists microsmith; then
  if global_version="$(microsmith --version 2>/dev/null)"; then
    if [ -n "$global_version" ]; then
      info "Global command available: ${global_version}"
    else
      info "Global command is present but returned no version output in current shell."
    fi
  else
    info "Global command is present but failed version check in current shell."
  fi
else
  info "Use '${shim_path}' directly, or reload your shell to pick up PATH updates."
fi
