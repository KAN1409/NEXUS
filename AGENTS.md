# NEXUS Agent Guide

## Identity
- App: NEXUS
- Package: `com.kareem.nexus`
- Repository: `KAN1409/NEXUS`
- Stack: Kotlin, Jetpack Compose, Coroutines/Flow, MVVM, Hilt, Room
- Product principle: local-first personal intelligence

## Non-negotiable invariants
- Never change `applicationId` or package lineage.
- Never require uninstall for normal updates.
- Preserve user data across every update.
- Future APKs must use the permanent NEXUS signing identity.
- Never commit private signing keys, keystores, passwords, tokens, or secrets.
- No XML UI and no Java unless explicitly required.
- No placeholder TODO implementations in shipping code.
- Any database schema change requires an explicit Room migration and migration test.
- Any major UI milestone must remain realistically implementable and match the approved NEXUS visual language.

## Product loop
`OBSERVE -> UNDERSTAND -> PRIORITIZE -> SUGGEST -> ACT -> LEARN`

## UX language
- Dark premium visual system.
- Cyan/violet accents.
- Compact but readable information hierarchy.
- Cohesive NEXUS icon language.
- Avoid raw package names where a human-readable app label exists.
- Mixed Arabic/English content must remain readable and not create broken RTL/LTR ordering.

## Delivery rules
- CI must be green before delivery.
- Prefer direct APK delivery to the user instead of asking them to browse GitHub artifacts.
- Verify package name and signing identity before installation.
- Install as update with data preserved.
- Update `docs/STATE.md` when a milestone meaningfully changes project state.

## Read first
Before changing code, read:
1. `docs/STATE.md`
2. `docs/RELEASE.md`
3. `docs/ACCEPTANCE.md`
