# NEXUS current state

- Repository: KAN1409/NEXUS
- Integration branch: `v1/integrated-major-update`
- Based on the existing major completion branch at `480532c`.
- Candidate: versionName 1.0.0, versionCode 101.
- Application ID: `com.kareem.nexus`; Room schema remains version 1.
- Release status: validation candidate, awaiting CI and the installed-device acceptance gate.
- Permanent v2 signing key remains local to the user's device. CI artifacts are NOT signed for normal installation.

## Implemented
- Atomic understanding rebuilds and action/event writes; repeated approval cannot create duplicate events.
- Repeated notification capture preserves first-seen time. Background work processes new context and resurfaces deferred actions.
- Manual/shared requests generate actions; completed, rejected and deferred actions suppress their attention cards.
- Arabic diacritics/alef/digit normalization, token matching and one-character typo tolerance in full-history Memory search.
- Durable image attachment copying, bounded image decoding, full evidence viewer and explicit source/link opening.
- Scrollable capture/settings, visible operation errors, save confirmation after persistence, lifecycle-safe navigation.
- Compact dark cyan/violet Home, Discover evidence, Memory filters and action/timeline Activity.
- Removed destructive database downgrade fallback and unused placeholder screen.
- Added unit regressions, Room concurrency/dedup/persistence tests and an emulator capture/search/navigation test with screenshots.
- `UPDATE_LOCAL.sh` verifies candidate checksum and installed signer, signs locally and uses only `adb install -r`.

## Boundaries
Understanding remains a local heuristic system; it does not provide an LLM or calibrated certainty. Topic grouping does not prove messages belong to the same real-world thread. Action Start tracks user progress; external actions require explicit user interaction. Image OCR and semantic embedding search are not implemented. Memory matching scans stored observations on a background dispatcher; very large archives require a future indexed/paged search design.

The installed Samsung device's signer, data preservation and behavior must still be verified before calling the release complete. No merge or production release has been performed.
