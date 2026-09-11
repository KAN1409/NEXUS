#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

INPUT_APK="${1:-}"
OUTPUT_APK="${2:-}"

if [[ -z "$INPUT_APK" || -z "$OUTPUT_APK" ]]; then
  echo "Usage: ./SIGN_RELEASE.sh input.apk output.apk"
  exit 2
fi

if [[ ! -f "$INPUT_APK" ]]; then
  echo "Input APK not found: $INPUT_APK"
  exit 3
fi

DEFAULT_ROOT="${HOME}/NEXUS_SIGNING"
DEFAULT_KEYSTORE="${DEFAULT_ROOT}/nexus-release-v2.jks"
DEFAULT_PASSFILE="${DEFAULT_ROOT}/nexus-release-v2.pass"

NEXUS_KEYSTORE="${NEXUS_KEYSTORE:-$DEFAULT_KEYSTORE}"
NEXUS_KEY_ALIAS="${NEXUS_KEY_ALIAS:-nexus}"

if [[ -z "${NEXUS_STORE_PASSWORD:-}" && -f "$DEFAULT_PASSFILE" ]]; then
  NEXUS_STORE_PASSWORD="$(tr -d '\r\n' < "$DEFAULT_PASSFILE")"
fi

if [[ -z "${NEXUS_KEY_PASSWORD:-}" && -n "${NEXUS_STORE_PASSWORD:-}" ]]; then
  NEXUS_KEY_PASSWORD="$NEXUS_STORE_PASSWORD"
fi

: "${NEXUS_STORE_PASSWORD:?Set NEXUS_STORE_PASSWORD or keep $DEFAULT_PASSFILE}"
: "${NEXUS_KEY_PASSWORD:?Set NEXUS_KEY_PASSWORD or keep $DEFAULT_PASSFILE}"

if [[ ! -f "$NEXUS_KEYSTORE" ]]; then
  echo "Keystore not found: $NEXUS_KEYSTORE"
  exit 4
fi

apksigner sign \
  --ks "$NEXUS_KEYSTORE" \
  --ks-key-alias "$NEXUS_KEY_ALIAS" \
  --ks-pass "pass:$NEXUS_STORE_PASSWORD" \
  --key-pass "pass:$NEXUS_KEY_PASSWORD" \
  --out "$OUTPUT_APK" \
  "$INPUT_APK"

apksigner verify --verbose --print-certs "$OUTPUT_APK"
echo "SIGNED_OK: $OUTPUT_APK"
