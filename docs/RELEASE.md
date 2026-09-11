# NEXUS Release and Signing

## Core rule
Android updates must preserve the same application ID and signing identity.

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

## Local signing
The user currently keeps the permanent NEXUS keystore outside the repository.

Use environment variables rather than hard-coded secrets:

```bash
export NEXUS_KEYSTORE="$HOME/NEXUS_SIGNING/nexus-release.jks"
export NEXUS_KEY_ALIAS="nexus"
export NEXUS_STORE_PASSWORD="..."
export NEXUS_KEY_PASSWORD="..."
```

Then run:

```bash
./SIGN_RELEASE.sh path/to/input.apk path/to/NEXUS-signed.apk
```

## Verification
Before installation:

```bash
apksigner verify --verbose --print-certs NEXUS-signed.apk
```

Install only as an update:

```bash
adb -s 127.0.0.1:5555 install -r NEXUS-signed.apk
```

Do not uninstall as part of a normal release.

## CI
CI may build unsigned/debug artifacts for validation. A CI release-signing flow must use repository secrets or another secure secret store. Never place signing material directly in workflow YAML.

## Database safety
Any Room schema change requires:
1. Incrementing the Room database version.
2. Adding an explicit Migration.
3. Exporting the new schema.
4. Adding/updating migration tests.
5. Verifying upgrade from every still-supported schema version.
