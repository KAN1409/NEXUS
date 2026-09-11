# NEXUS

Local-first personal intelligence for Android. Capture context, review what may need attention, find saved details, and track decisions.

Kotlin · Compose · Flow · Hilt · Room · WorkManager

## Major update candidate

- Daily brief and evidence-backed suggestions
- Arabic/English full-history text search with filters and typo tolerance
- Durable image storage and evidence detail viewer
- Action approval, 24-hour deferral, dismissal and completion tracking
- Transactional data updates and background understanding
- Dark cyan/violet UI with scrollable screens and visible errors

Understanding uses local rules. Image OCR, semantic embeddings and autonomous external execution are not included. Read `docs/STATE.md` for release status and `docs/ACCEPTANCE.md` for device gates.

## Build

JDK 17, Gradle 9.6.0, Android SDK 37:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
gradle :app:connectedDebugAndroidTest
```

CI debug APKs are validation artifacts. For the installed NEXUS, retain the permanent local v2 signing key and use `UPDATE_LOCAL.sh candidate.apk <published-sha256>`. It checks the existing signer and stops on mismatch; it never uninstalls or clears app data.
