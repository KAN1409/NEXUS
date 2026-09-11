# NEXUS 2.0 Acceptance

NEXUS 2.0 is releasable only when every applicable gate passes on the latest branch head.

## Automated release gates
1. Unit tests pass.
2. Debug APK and Android test APK compile.
3. Room `1 -> 2` migration test preserves legacy observations and creates the new structured-intelligence tables.
4. Emulator UI acceptance passes across For You, Memory, Discover, Activity and Settings.
5. Capture, search, navigation and Activity recreation pass.
6. English and Arabic request/appointment/payment classification tests pass.
7. Promotional feedback prompts, passive social/story noise and NEXUS self-notifications are suppressed.
8. Memory typo tolerance and bilingual concept matching tests pass.
9. No destructive Room migration is introduced.

## Installed-device gates
10. Candidate is signed with the permanent NEXUS v2 signing identity and installs only with `adb install -r` over the existing app.
11. Existing Memory, actions, feedback and interests survive the Room v1 -> v2 migration.
12. Package remains `com.kareem.nexus`; signer continuity is verified before install.
13. No crash on cold launch, resume, capture, navigation, background observation or action interaction.
14. Notification Access and Usage Access states remain truthful after returning from Android Settings.
15. Notification capture remembers genuinely new repeated events while deduplicating updates of the same notification event.
16. App-usage observations remain bounded and do not flood Memory.
17. Shared images remain stored privately; baseline OCR works when supported, and optional Gemini Nano failure never blocks capture/search.
18. Memory search/filter behavior is usable with real Arabic and English context; optional semantic expansion improves results without hiding deterministic direct matches.
19. Top of mind is materially quieter than the old generic-action UI while still surfacing real requests, appointments, payments, deliveries, failures and follow-ups.
20. Evidence opens correctly from Top of mind and Activity.
21. Context actions launch the source/calendar when possible and fail safely when Android cannot resolve the target.
22. `Remind tomorrow` moves the item to Later and deferred items can resurface.
23. `Done` records a resolved outcome; dismiss/reject remains a distinct outcome.
24. Connected situations group related evidence instead of duplicating independent cards.
25. Discover, Activity and Settings show the rebuilt NEXUS 2.0 product model rather than legacy placeholder/generic surfaces.
26. No dead controls, placeholder screens, package-lineage changes or hidden destructive behavior.

## Five-second product test
Within five seconds of opening For You, the user should understand what changed, what actually needs attention, what NEXUS connected, what evidence supports it and what useful step can be taken next. If nothing useful needs attention, NEXUS should stay quiet rather than manufacture cards.

## UI target
The five primary surfaces follow the NEXUS showcase direction: premium dark background, compact cyan/violet accents, strong hierarchy, dense but readable evidence cards, concise status pills and consistent bottom navigation.

## Scope truth
NEXUS is local-first. Its deterministic intelligence is heuristic and confidence-ranked. Optional Gemini Nano inference is an on-device enhancement, not a requirement or a claim of perfect semantic understanding. OCR quality still depends on image quality and the local recognizer/model available on the device.
