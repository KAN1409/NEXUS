# NEXUS Current State

## Source of truth
- Repository: `KAN1409/NEXUS`
- Active completion branch: `v1/major-intelligence-completion`
- Package: `com.kareem.nexus`
- Database: Room schema version 1; no destructive migration required
- Target release: NEXUS 1.0.0
- Permanent signing lineage: local v2 identity; never commit signer material

## Product
NEXUS is a local-first personal intelligence system.

Core loop:
OBSERVE → UNDERSTAND → CONNECT → PRIORITIZE → SUGGEST → ACT → LEARN

## NEXUS 1.0 scope
- Notification observation
- Share/manual capture
- App usage context
- Automatic context refresh
- Searchable/filterable Memory
- Local bilingual attention detection
- Urgency/request/appointment/payment/delivery/follow-up/error classification
- Connected Situations
- Daily Brief
- Needs Attention
- Evidence-backed Discover insights
- Context-specific suggested actions
- Persistent approval/defer/reject/start/complete/fail lifecycle
- Causal Activity history
- Stable user decisions across rebuilds
- Bounded usage snapshots
- Permanent update-safe signing flow

## Release rule
Do not ship or merge the completion branch into normal release flow until CI is green and the device acceptance checklist in `docs/ACCEPTANCE.md` passes.
