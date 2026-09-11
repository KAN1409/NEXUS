# NEXUS 1.0 Acceptance

The major release is complete only when all gates pass.

## Release gates
1. Unit tests pass.
2. Debug APK and Android test APK compile.
3. APK installs with `adb install -r` over the existing permanent v2-signed NEXUS.
4. Existing Memory, actions and interests survive the update.
5. No package/signing regression.
6. No crash on launch or navigation.
7. Notification and Usage Access state is truthful after returning from Android Settings.
8. Context refresh happens automatically when NEXUS resumes.
9. Memory search and filters work for Arabic and English text.
10. App-usage data is summarized instead of flooding Memory.
11. Needs Attention surfaces requests, appointments, payments, deliveries, errors and follow-ups.
12. Generic “Review Communication” style suggestions are retired.
13. Suggested actions are tied to real observed signals.
14. Later defers and resurfaces after the defer window.
15. Dismiss remains rejected across understanding rebuilds.
16. Approved actions can Start, then Complete or Fail.
17. Situations connect multiple related observations.
18. Discover shows connected situations and evidence-backed insights.
19. Activity shows the causal chain from observation to decision/result.
20. No dead controls, placeholder screens or destructive migration.

## NEXUS 1.0 product test
Within five seconds of opening Home, the user should understand:
- what changed;
- what may need attention;
- what NEXUS connected;
- what action is available next.

If there is nothing useful, NEXUS should stay quiet instead of manufacturing generic cards.

## Integration validation
Automated gates now include concurrent decision/event consistency, duplicate capture identity, deferred resurfacing, full-history retrieval, database reopen persistence, and emulator capture/search/navigation.

Human/device gates remain: permanent v2 signature match, update over the installed Samsung APK, existing real data retained, real notification capture and access-settings round trips. CI success alone does not satisfy these gates.

Scope clarification: “Start tracking” records the user's progress; it does not execute arbitrary external work. Image attachments are viewable but have no OCR. The engine uses heuristic topic/attention detection rather than an LLM.
