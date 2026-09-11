# NEXUS STATE

## Active release candidate

- Branch: `v3/unified-intelligence-migration`
- Package: `com.kareem.nexus`
- Version: `3.0.0` (`300`)
- Room schema: `3`
- Release status: **candidate only** until CI and real-device acceptance pass.
- `main` must remain untouched until acceptance.

## Product contract

NEXUS 3 is value-first. Its two primary jobs are:

1. Do not let the user forget something that needs action.
2. Find something the user has seen or saved even when the query is vague.

Primary pipeline:

`Capture → Remember → Understand → Connect → Open Loop → Act → Outcome → Learn`

## Implemented in 3.0

- Existing observations and permissions are preserved; no uninstall is required.
- Explicit Room `2 → 3` migration adds persistent open loops, situation snapshots and action execution outcomes.
- Requests, payments, appointments, delivery updates, failures and follow-ups can become persistent open loops.
- Open loops have `OPEN`, `WAITING`, `SNOOZED`, `RESOLVED` and `DISMISSED` states that survive intelligence rebuilds.
- Home is now `Needs you`, `Waiting on`, `Upcoming`, `Changed` and an intentional all-clear state. Theme percentages are no longer a primary product surface.
- Situations show current state, what changed, evidence count, open-loop count and next step.
- Real Android actions include opening the source app, calendar insertion, dialer, navigation, copy and available tracking links.
- Reminders are scheduled locally through WorkManager and surface through a NEXUS notification channel.
- Activity records real action execution outcomes as well as NEXUS lifecycle events.
- Memory search handles Arabic/English concepts, typo tolerance, vague filler language, Arabic digits and remembered numeric anchors such as an amount.
- Captured evidence is backfilled into the existing local Memory/Entity tables during intelligence rebuild.
- Optional Gemini Nano remains an enhancement. The deterministic local engine is always the fallback.

## Intentional limitations / truthful boundaries

- Memory retrieval is currently hybrid lexical/fuzzy/concept + optional Nano query expansion. It is **not** yet a neural embedding index.
- Bundled ML Kit printed-text OCR is the default recognizer and does not provide native printed-Arabic script recognition. Nano image-text extraction is an optional supported-device assist, not a guaranteed OCR engine.
- Android does not guarantee deep-linking to the exact original notification/message in every third-party app. NEXUS opens the source app when a safe exact destination is unavailable.
- Reminder delivery requires Android notification permission and notification delivery to be enabled for NEXUS.
- NEXUS does not request location permission in 3.0.
- Irreversible third-party actions are not silently executed.

## Safety / release invariants

- Never change package lineage.
- Never require uninstall for a normal update.
- Never use destructive Room migration.
- Permanent signing identity stays on the user's device and is never committed.
- CI APKs are validation artifacts; the local updater signs them with the installed app's permanent signer before `adb install -r`.
