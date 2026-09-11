# NEXUS Release and Signing

## Core rule
Android updates must preserve both the application ID and the permanent NEXUS signing identity.

Package:
`com.kareem.nexus`

## Secret handling
The permanent NEXUS keystore must remain outside Git.

Never commit:
- `*.jks`
- `*.keystore`
- key passwords
- store passwords
- base64-encoded keystores

The active local v2 keystore is expected at:
`$HOME/NEXUS_SIGNING/nexus-release-v2.jks`

`SIGN_RELEASE.sh` should obtain credentials from the local protected password file/environment and must never print or commit them.

## Normal local signing
From the repository root:

```bash
./SIGN_RELEASE.sh path/to/input.apk path/to/NEXUS-2.0-signed.apk
```

Verify before installation:

```bash
apksigner verify --verbose --print-certs NEXUS-2.0-signed.apk
```

Install only as an update against the intended device:

```bash
adb -s 127.0.0.1:5555 install -r NEXUS-2.0-signed.apk
```

Do not uninstall during a normal release.

## CI
CI builds validation APKs and runs unit, migration and emulator acceptance tests. CI artifacts are not the permanent-signed production update. Production signing remains local unless a future release-signing flow uses a secure secret store; signing material must never be embedded in workflow YAML.

## Database safety
Any Room schema change requires:
1. Incrementing the Room database version.
2. Adding an explicit Migration.
3. Exporting the new schema.
4. Adding/updating migration tests.
5. Verifying upgrade from every still-supported schema version.

NEXUS 2.0 uses Room schema 2 and must migrate existing schema-1 installs through `MIGRATION_1_2`; destructive fallback is not an acceptable release path.

## One-time signer recovery
`RECOVER_SIGNING.sh` is an emergency-only path for a lost/unusable signing identity. It must not be used for normal NEXUS 2.0 installation. The normal path is permanent local signing plus `adb install -r` so app data and package lineage remain intact.
