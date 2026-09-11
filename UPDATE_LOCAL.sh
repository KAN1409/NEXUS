#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
umask 077

# Safe normal update only. Never uninstalls, clears data, or changes package lineage.
input_apk="${1:?Usage: bash UPDATE_LOCAL.sh candidate.apk expected_sha256}"
expected_sha="${2:?Provide the SHA-256 published with the validated candidate}"
[[ -f "$input_apk" ]] || { echo 'Candidate APK not found'; exit 2; }
[[ "$expected_sha" =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'Invalid SHA-256'; exit 2; }
actual_sha="$(sha256sum "$input_apk" | awk '{print $1}')"
[[ "${actual_sha,,}" == "${expected_sha,,}" ]] || { echo 'Candidate checksum mismatch'; exit 3; }

for binary in adb apksigner sha256sum; do
  command -v "$binary" >/dev/null || { echo "Missing $binary"; exit 4; }
done

# NEXUS development uses this primary ADB target unless explicitly overridden.
device="${NEXUS_ADB_DEVICE:-127.0.0.1:5555}"
adb -s "$device" get-state >/dev/null 2>&1 || {
  adb connect "$device" >/dev/null 2>&1 || true
}
adb -s "$device" get-state >/dev/null 2>&1 || { echo "ADB device unavailable: $device"; exit 5; }

package='com.kareem.nexus'
installed_path="$(adb -s "$device" shell pm path "$package" | tr -d '\r' | sed -n 's/^package://p' | head -1)"
[[ -n "$installed_path" ]] || { echo 'Existing NEXUS installation not found; stopped'; exit 6; }

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
adb -s "$device" pull "$installed_path" "$work_dir/installed.apk" >/dev/null

cert_digest() {
  apksigner verify --print-certs "$1" \
    | awk '/certificate SHA-256 digest:/ {print $NF; exit}' \
    | tr '[:upper:]' '[:lower:]'
}

installed_cert="$(cert_digest "$work_dir/installed.apk")"
[[ -n "$installed_cert" ]] || { echo 'Could not verify installed signer'; exit 7; }

export NEXUS_KEYSTORE="${NEXUS_KEYSTORE:-$HOME/NEXUS_SIGNING/nexus-release-v2.jks}"
export NEXUS_KEY_ALIAS="${NEXUS_KEY_ALIAS:-nexus}"
if [[ -z "${NEXUS_STORE_PASSWORD:-}" ]]; then
  pass_file="$HOME/NEXUS_SIGNING/nexus-release-v2.pass"
  [[ -f "$pass_file" ]] || { echo 'Permanent signing password file not found'; exit 8; }
  export NEXUS_STORE_PASSWORD="$(tr -d '\r\n' < "$pass_file")"
fi
export NEXUS_KEY_PASSWORD="${NEXUS_KEY_PASSWORD:-$NEXUS_STORE_PASSWORD}"
[[ -f "$NEXUS_KEYSTORE" ]] || { echo 'Permanent keystore not found'; exit 8; }

apksigner sign \
  --ks "$NEXUS_KEYSTORE" \
  --ks-key-alias "$NEXUS_KEY_ALIAS" \
  --ks-pass env:NEXUS_STORE_PASSWORD \
  --key-pass env:NEXUS_KEY_PASSWORD \
  --out "$work_dir/update.apk" \
  "$input_apk"

apksigner verify --verbose --print-certs "$work_dir/update.apk" >/dev/null
new_cert="$(cert_digest "$work_dir/update.apk")"
[[ -n "$new_cert" ]] || { echo 'Could not verify newly signed APK'; exit 9; }
[[ "$new_cert" == "$installed_cert" ]] || { echo 'Signer mismatch: stopped without changing the installed app'; exit 9; }

unset NEXUS_STORE_PASSWORD NEXUS_KEY_PASSWORD
output_dir="$HOME/NEXUS_UPDATES"
mkdir -p "$output_dir"
output_apk="$output_dir/NEXUS-1.1.0-signed.apk"
cp "$work_dir/update.apk" "$output_apk"

adb -s "$device" install -r "$output_apk"
adb -s "$device" shell am start -n com.kareem.nexus/.MainActivity >/dev/null

echo "UPDATE_OK"
echo "APK: $output_apk"
echo "Signer SHA-256: $new_cert"
echo 'Existing NEXUS data was preserved.'
