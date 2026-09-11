# NEXUS current state

- Repository: `KAN1409/NEXUS`
- Final integration branch: `v2/personal-intelligence-rebuild`
- Candidate: versionName `2.0.0`, versionCode `200`.
- Application ID remains `com.kareem.nexus`.
- Room schema is version 2 with an explicit, data-preserving `1 -> 2` migration and migration instrumentation test.
- Permanent v2 signing identity remains local to the user's device. CI artifacts are validation APKs and must be signed locally before normal installation.

## Product loop
OBSERVE → UNDERSTAND → CONNECT → PRIORITIZE → SUGGEST → ACT → LEARN

## NEXUS 2.0 product rebuild
- For You is centered on a daily brief, Top of mind, connected situations, learned interests, evidence and context-aware next actions.
- A structured local intelligence layer classifies requests, payments, appointments, deliveries, failures and follow-ups; extracts useful facts such as organization, amount, currency, date, time, URL and order/reference identifiers; ranks urgency/confidence; and suppresses common noise.
- Connected situations group related observations into one real-world thread instead of surfacing disconnected notification cards.
- Memory is a dense evidence timeline with full-history search, filters, Arabic/English normalization, typo tolerance, bilingual concept matching, OCR text and optional on-device semantic query expansion.
- Shared images are copied into private NEXUS storage. ML Kit OCR remains the baseline; supported devices can optionally use Gemini Nano through ML Kit Prompt API as a local second-pass text/context layer.
- Gemini Nano is optional. NEXUS remains usable with deterministic local intelligence when the system model is unavailable, downloading or fails.
- Suggested actions are context-specific: open source/reply, calendar, track, remind later, review failure and mark done. Marking an item done is tracked separately from dismissing it.
- Deferred items resurface after the local defer window; legacy `SAVED` defers remain backward-compatible.
- Repeated identical notification text can be remembered as a new event when Android supplies a new notification `postedAt`, while updates of the same event remain deduplicated.
- Activity separates Active, Later, Finished and Timeline, with explicit resolved/dismissed outcomes.
- Settings is a compact control center for Notification Access, Usage Access, local intelligence, optional on-device AI, privacy and version state.
- Package lineage, update-only installation and permanent signer continuity remain mandatory.

## Data and privacy boundaries
- Observations, OCR text, derived understanding, situations, actions and feedback are stored in the local app database/private app storage.
- App usage captures summarized recent behavioral signals rather than content.
- The deterministic intelligence layer is heuristic and confidence-ranked; it does not claim perfect understanding.
- Optional Gemini Nano inference is on-device and fail-closed; it is not required for core behavior.

## Release gate
Do not merge to `main` or call NEXUS 2.0 shipped until the latest branch head has a fully green CI run and the signed APK passes real-device update acceptance on the existing Samsung installation: signer continuity, data preservation, Room migration, launch/navigation, permission truth, Memory search, Top of mind quality, action lifecycle and UI review.
