#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
umask 077

# Normal update only. Never uninstalls or clears app data.
input_apk="${1:?Usage: bash UPDATE_LOCAL.sh candidate.apk expected_sha256}"
expected_sha="${2:?Provide the SHA-256 published with the validated candidate}"
[[ -f "$input_apk" ]] || { echo 'Candidate APK not found'; exit 2; }
[[ "$expected_sha" =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'Invalid SHA-256'; exit 2; }
actual_sha="$(sha256sum "$input_apk" | cut -d ' ' -f 1)"
[[ "${actual_sha,,}" == "${expected_sha,,}" ]] || { echo 'Candidate checksum mismatch'; exit 3; }
for binary in adb apksigner; do command -v "$binary" >/dev/null || { echo "Missing $binary"; exit 4; }; done

device="${NEXUS_ADB_DEVICE:-}"
if [[ -z "$device" ]]; then
  mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  [[ "${#devices[@]}" == 1 ]] || { echo 'Set NEXUS_ADB_DEVICE to the connected device serial'; exit 5; }
  device="${devices[0]}"
fi
package='com.kareem.nexus'
installed_path="$(adb -s "$device" shell pm path "$package" | tr -d '\r' | sed -n 's/^package://p' | head -1)"
[[ -n "$installed_path" ]] || { echo 'Existing NEXUS installation not found; stopped'; exit 6; }
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
adb -s "$device" pull "$installed_path" "$work_dir/installed.apk" >/dev/null
installed_cert="$(apksigner verify --print-certs "$work_dir/installed.apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
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
apksigner sign --ks "$NEXUS_KEYSTORE" --ks-key-alias "$NEXUS_KEY_ALIAS" \
  --ks-pass env:NEXUS_STORE_PASSWORD --key-pass env:NEXUS_KEY_PASSWORD \
  --out "$work_dir/update.apk" "$input_apk"
new_cert="$(apksigner verify --print-certs "$work_dir/update.apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
[[ "$new_cert" == "$installed_cert" ]] || { echo 'Signer mismatch: stopped without changing the installed app'; exit 9; }
unset NEXUS_STORE_PASSWORD NEXUS_KEY_PASSWORD
output_dir="$HOME/NEXUS_UPDATES"
mkdir -p "$output_dir"
cp "$work_dir/update.apk" "$output_dir/NEXUS-1.0.0-signed.apk"
adb -s "$device" install -r "$output_dir/NEXUS-1.0.0-signed.apk"
adb -s "$device" shell am start -n com.kareem.nexus/.MainActivity
echo 'Update installed. Existing app data was not cleared.'
