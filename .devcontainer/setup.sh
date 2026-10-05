#!/usr/bin/env bash
set -euo pipefail

CMDLINE_TOOLS_ZIP="https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip"

if [[ "$CMDLINE_TOOLS_ZIP" == "REPLACE_ME" ]]; then
  echo "Set CMDLINE_TOOLS_ZIP to the verified official Android command-line tools download URL." >&2
  exit 1
fi

sudo apt-get update
sudo apt-get install -y --no-install-recommends unzip curl zip

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
mkdir -p "$ANDROID_HOME/cmdline-tools"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$SDKMANAGER" ]]; then
  download_dir="$(mktemp -d)"
  trap 'rm -rf "$download_dir"' EXIT
  curl --fail --location --silent --show-error "$CMDLINE_TOOLS_ZIP" \
    --output "$download_dir/cmdline-tools.zip"
  unzip -q "$download_dir/cmdline-tools.zip" -d "$download_dir"
  mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
  cp -R "$download_dir/cmdline-tools/." "$ANDROID_HOME/cmdline-tools/latest/"
  rm -rf "$download_dir"
  trap - EXIT
fi

if [[ ! -f "$HOME/.bashrc" ]] || ! grep -Fq '# Android SDK environment (managed by .devcontainer/setup.sh)' "$HOME/.bashrc"; then
  cat >> "$HOME/.bashrc" <<'BASHRC'

# Android SDK environment (managed by .devcontainer/setup.sh)
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
BASHRC
fi

sdkmanager() {
  "$SDKMANAGER" "$@"
}

set +o pipefail
yes | sdkmanager --licenses
licenses_status=$?
set -o pipefail
if (( licenses_status != 0 )); then
  echo "Android SDK license acceptance failed (exit $licenses_status)." >&2
  exit "$licenses_status"
fi

package_list="$(sdkmanager --list)"
available_packages="$(awk '
  /^Available Packages:/ { available = 1; next }
  available { print }
' <<< "$package_list")"

mapfile -t api_versions < <(
  sed -nE 's/^[[:space:]]*platforms;android-([0-9]+)[[:space:]]*\|.*/\1/p' \
    <<< "$available_packages" | sort -rn -u
)

platform_api=""
for api in "${api_versions[@]}"; do
  if grep -Eq "^[[:space:]]*build-tools;${api//./\\.}\\.0\\.0[[:space:]]*\\|" <<< "$available_packages"; then
    platform_api="$api"
    break
  fi
done

if [[ -z "$platform_api" ]]; then
  echo "Could not find a stable Android platform/build-tools pair in sdkmanager --list." >&2
  exit 1
fi

sdkmanager \
  "platform-tools" \
  "platforms;android-$platform_api" \
  "build-tools;$platform_api.0.0"

if [[ ! -f "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  curl --fail --location --silent --show-error "https://get.sdkman.io" | bash
fi

# shellcheck disable=SC1091
source "$HOME/.sdkman/bin/sdkman-init.sh"
if [[ ! -x "$HOME/.sdkman/candidates/gradle/current/bin/gradle" ]]; then
  sdk install gradle
fi

printf 'Installed Android platform API %s and build-tools %s.0.0\n' \
  "$platform_api" "$platform_api"
