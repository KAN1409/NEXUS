#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

PACKAGE="com.kareem.nexus"
DEVICE="${NEXUS_ADB_DEVICE:-127.0.0.1:5555}"
INPUT_APK="${1:-}"
ROOT="${HOME}/NEXUS_SIGNING"
KEYSTORE="${ROOT}/nexus-release-v2.jks"
PASSFILE="${ROOT}/nexus-release-v2.pass"
ALIAS="nexus"
RECOVERY_ROOT="${HOME}/NEXUS_RECOVERY"
STAMP="$(date +%Y%m%d_%H%M%S)"
SESSION="${RECOVERY_ROOT}/${STAMP}"
BACKUP="${SESSION}/NEXUS_data_backup.tar"
SIGNED="${SESSION}/NEXUS_recovered_signed.apk"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -n "$INPUT_APK" ]] || fail "Usage: ./RECOVER_SIGNING.sh /path/to/input.apk"
[[ -f "$INPUT_APK" ]] || fail "Input APK not found: $INPUT_APK"

command -v adb >/dev/null || fail "adb not found in Termux"
command -v apksigner >/dev/null || fail "apksigner not found"
command -v keytool >/dev/null || fail "keytool not found"
command -v tar >/dev/null || fail "tar not found"

mkdir -p "$ROOT" "$SESSION"
chmod 700 "$ROOT" "$RECOVERY_ROOT" "$SESSION"

adb -s "$DEVICE" get-state >/dev/null 2>&1 || fail "ADB target unavailable: $DEVICE"
adb -s "$DEVICE" shell pm path "$PACKAGE" >/dev/null 2>&1 || fail "$PACKAGE is not installed"

echo "[1/8] Force-stopping NEXUS"
adb -s "$DEVICE" shell am force-stop "$PACKAGE"

echo "[2/8] Backing up app data before any uninstall"
if ! adb -s "$DEVICE" exec-out run-as "$PACKAGE" tar -cf - . > "$BACKUP"; then
  rm -f "$BACKUP"
  fail "run-as backup failed; nothing was uninstalled"
fi
[[ -s "$BACKUP" ]] || fail "Backup is empty; nothing was uninstalled"
tar -tf "$BACKUP" >/dev/null || fail "Backup validation failed; nothing was uninstalled"

echo "[3/8] Preparing a new permanent local signing identity"
if [[ ! -f "$KEYSTORE" ]]; then
  if [[ ! -f "$PASSFILE" ]]; then
    umask 077
    head -c 32 /dev/urandom | base64 | tr -d '\n' > "$PASSFILE"
    printf '\n' >> "$PASSFILE"
    chmod 600 "$PASSFILE"
  fi
  PASS="$(tr -d '\r\n' < "$PASSFILE")"
  keytool -genkeypair     -keystore "$KEYSTORE"     -storetype PKCS12     -storepass "$PASS"     -keypass "$PASS"     -alias "$ALIAS"     -keyalg RSA     -keysize 4096     -validity 10000     -dname "CN=NEXUS, OU=KAN1409, O=KAN1409, C=EG" >/dev/null
  chmod 600 "$KEYSTORE"
else
  [[ -f "$PASSFILE" ]] || fail "Found $KEYSTORE but password file is missing"
fi

PASS="$(tr -d '\r\n' < "$PASSFILE")"

echo "[4/8] Signing candidate APK"
apksigner sign   --ks "$KEYSTORE"   --ks-key-alias "$ALIAS"   --ks-pass "pass:$PASS"   --key-pass "pass:$PASS"   --out "$SIGNED"   "$INPUT_APK"

apksigner verify --verbose --print-certs "$SIGNED"
NEW_DIGEST="$(apksigner verify --print-certs "$SIGNED" | sed -n 's/.*certificate SHA-256 digest: //p' | head -n1)"
[[ -n "$NEW_DIGEST" ]] || fail "Could not read new signer digest"

echo "[5/8] Safety checkpoint passed"
echo "Backup: $BACKUP"
echo "New signer SHA-256: $NEW_DIGEST"

echo "[6/8] Replacing package identity while preserving the validated backup"
adb -s "$DEVICE" uninstall "$PACKAGE" >/dev/null || fail "Uninstall failed; backup remains at $BACKUP"
adb -s "$DEVICE" install "$SIGNED" >/dev/null || fail "Install failed; backup remains at $BACKUP"

echo "[7/8] Restoring app data"
adb -s "$DEVICE" push "$BACKUP" /data/local/tmp/NEXUS_data_backup.tar >/dev/null
adb -s "$DEVICE" shell "cat /data/local/tmp/NEXUS_data_backup.tar | run-as $PACKAGE tar -xf - -C /data/data/$PACKAGE"
adb -s "$DEVICE" shell rm -f /data/local/tmp/NEXUS_data_backup.tar
adb -s "$DEVICE" shell am force-stop "$PACKAGE"

echo "[8/8] Verifying installed package"
adb -s "$DEVICE" shell pm path "$PACKAGE"
adb -s "$DEVICE" shell dumpsys package "$PACKAGE" | grep -E 'versionName=|versionCode=' | head -n4 || true

cat <<EOF

RECOVERY_COMPLETE
Backup retained: $BACKUP
Signed APK retained: $SIGNED
New permanent keystore: $KEYSTORE
Password is stored locally in: $PASSFILE
You do not need to type or remember it.

IMPORTANT:
- Keep both $KEYSTORE and $PASSFILE backed up securely.
- Future NEXUS releases must use this same v2 signing identity.
EOF
