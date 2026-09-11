# NEXUS 3.0 RELEASE

## Release identity

- Package: `com.kareem.nexus`
- Candidate version: `3.0.0` (`300`)
- Branch: `v3/unified-intelligence-migration`
- Database: Room schema `3`
- Normal update path: existing signed NEXUS → signed NEXUS 3.0 using `adb install -r`

## Permanent signing

The permanent keystore/password stay on the user's device under `~/NEXUS_SIGNING`. They must never be committed or uploaded to CI.

The validation APK produced by GitHub Actions is not the final installed signing artifact. The user signs it locally through `UPDATE_LOCAL.sh`, which:

1. verifies the published candidate SHA-256;
2. pulls the currently installed NEXUS APK and reads its signer;
3. signs the candidate with the local permanent NEXUS key;
4. refuses to continue if candidate signer and installed signer differ;
5. installs with `adb install -r` only;
6. launches NEXUS;
7. verifies installed version `3.0.0 (300)`.

## Local install command

After the final CI artifact is downloaded to the phone, use the final SHA-256 published with that artifact:

```bash
cd ~/NEXUS/NEXUS_Update1
git fetch origin
git checkout v3/unified-intelligence-migration
git pull --ff-only origin v3/unified-intelligence-migration

NEXUS_ADB_DEVICE=127.0.0.1:5555 \
  bash ./UPDATE_LOCAL.sh \
  "/storage/emulated/0/Download/NEXUS-3.0-validation.apk" \
  <PUBLISHED_SHA256>
```

Expected terminal end state:

```text
UPDATE_OK
Version: 3.0.0 (300)
Existing NEXUS data was preserved.
```

## Prohibited release shortcuts

- Do not uninstall NEXUS.
- Do not clear app data.
- Do not use a different signer.
- Do not use destructive Room migration.
- Do not merge the release branch to `main` before real-device acceptance.
- Do not call the release Done merely because CI is green.
