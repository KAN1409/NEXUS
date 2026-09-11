# NEXUS current state

- Repository: `KAN1409/NEXUS`
- Final integration branch: `v1/all-in-one-final`
- Candidate: versionName `1.1.0`, versionCode `102`.
- Application ID: `com.kareem.nexus`; Room schema remains version 1, so this update does not require a database migration.
- Permanent v2 signing key remains local to the user's device. CI artifacts are validation APKs and must be signed locally before normal installation.

## Product loop
OBSERVE → UNDERSTAND → CONNECT → PRIORITIZE → SUGGEST → ACT → LEARN

## Implemented in the all-in-one final candidate
- Premium dark cyan/violet UI rebuilt across For You, Discover, Memory, Activity, Capture and Settings to match the approved NEXUS showcase direction.
- For You now prioritizes daily brief, compact metrics, learning themes, evidence-backed actions, needs-attention items and connected context.
- Discover now separates connected situations, evidence-backed insights and recurring themes instead of presenting only generic bars.
- Memory has full-history search, filters, typed evidence cards, Arabic/English normalization, typo tolerance and bilingual concept matching.
- Shared images are copied into private app storage and scanned with on-device OCR when the recognizer supports the text; extracted text becomes searchable Memory context.
- Suggestion quality suppresses common promotional feedback prompts, social reactions/story updates, passive status notifications and NEXUS self-generated noise.
- Real requests, appointments, failures, payments, deliveries and follow-ups can still surface as reviewable actions.
- Activity provides active/later/finished states plus a causal decision timeline.
- Settings reports real Notification/Usage access, privacy posture, local intelligence status and release version.
- Atomic action/event writes, first-seen notification dedup, bounded app-usage snapshots and background understanding remain intact.
- `UPDATE_LOCAL.sh` verifies checksum and installed signer, signs with the permanent local v2 identity and installs only with `adb install -r`.

## Search/OCR boundaries
- OCR is local and best-effort. Recognition quality depends on the image and script supported by the bundled recognizer; image storage remains useful even when no text is extracted.
- Memory search is concept-aware and bilingual through local normalization, synonyms and fuzzy matching. It is not a neural embedding model and does not claim calibrated semantic certainty.
- Situations and insights are evidence-backed local heuristics, not an LLM judgment.

## Release gate
Do not merge or call the release complete until CI is green and the installed-device acceptance pass confirms signer continuity, data preservation, no launch/navigation crash, correct permission state, useful noise filtering and the redesigned UI on the real Samsung device.
