# NEXUS Current State

## Source of truth
This file describes only the current operational state. Keep it short and update it when the active milestone changes.

## Current baseline
- Repository: `KAN1409/NEXUS`
- Branch: `main`
- Package: `com.kareem.nexus`
- Database: Room schema version 1
- Current active milestone: Update 5
- Latest known Update 5 commit at time this file was created: `68f6e2c6b20f71ccc45c70e1b48a2d8f8e7b8761`

## Completed milestones
### Update 1
Local-first foundation, Room, repository, navigation, core models.

### Update 2
Manual capture, Android Share intake, notification observation, usage access, Memory UI.

### Update 3
Understanding engine, interests, ranked patterns, functional Discover, deduplication improvements.

### Update 4
Prepared suggestions, Ready count, transparent Activity timeline.

## Active milestone — Update 5
Action lifecycle and approval loop.

Implemented in code:
- Approve
- Later / defer
- Dismiss / reject
- Start
- Complete
- Fail
- Persistent action-state transitions
- Feedback persistence
- Lifecycle labels in Activity
- Preservation of user decisions across understanding rebuilds

## Installed-app invariants
- Existing user data must be preserved.
- No normal release may require uninstall.
- The installed app now uses the permanent NEXUS v2 signing identity created locally outside the repository after the one-time signer recovery on 2026-09-11.
- The repository must never contain the private keystore or its password.

## Known gaps
- Memory still needs richer search, grouping, filtering, source icons, and stronger bilingual presentation.
- The local understanding engine is still mostly rules-based.
- Suggested actions need to become more specific and contextually useful.
- Notification understanding still needs stronger entity, urgency, date/time, and commitment extraction.
- Activity should eventually show a complete causal chain from observation to result.

## Next action
Update 5 CI is green. Signer recovery is complete and the installed app is now on the permanent v2 signing identity. Next: verify on-device data preservation and run the complete Update 5 action-lifecycle acceptance.
