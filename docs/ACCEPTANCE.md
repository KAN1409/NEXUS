# NEXUS 1.1 Acceptance

The all-in-one release is complete only when every applicable gate passes.

## Automated release gates
1. Unit tests pass.
2. Debug APK and Android test APK compile.
3. Emulator UI acceptance passes across For You, Memory, Discover, Activity and Settings.
4. Capture/search/navigation survives Activity recreation.
5. Request/appointment classification tests pass in English and Arabic.
6. Promotional feedback prompts, social story/reaction noise and NEXUS self-notifications are suppressed.
7. Bilingual concept-aware Memory search and typo tolerance tests pass.
8. Existing Room schema remains compatible and destructive migration is not introduced.

## Installed-device gates
9. APK installs with `adb install -r` over the existing permanent v2-signed NEXUS.
10. Existing Memory, actions and interests survive the update.
11. No package/signing regression.
12. No crash on launch or navigation.
13. Notification and Usage Access state is truthful after returning from Android Settings.
14. Context refresh happens automatically when NEXUS resumes.
15. App-usage data remains bounded and does not flood Memory.
16. Shared images remain viewable; supported image text is OCR-indexed into Memory.
17. Memory search/filter behavior is usable with real Arabic and English context.
18. False-positive action volume is materially lower than the previous build.
19. Real requests, appointments, payments, deliveries, failures and follow-ups still surface when appropriate.
20. Later defers, Dismiss remains rejected, Approved can Start, and in-progress actions can Complete or Fail.
21. Discover shows meaningful situations/insights, not only generic theme bars.
22. Activity shows action states and a causal decision timeline.
23. No dead controls or placeholder screens.

## Five-second product test
Within five seconds of opening For You, the user should understand:
- what changed today;
- what actually needs attention;
- what NEXUS has learned;
- what evidence supports a suggestion;
- what action can be taken next.

If nothing useful needs attention, NEXUS should stay quiet rather than manufacture cards.

## UI target
The five primary surfaces should follow the NEXUS showcase direction: premium dark background, compact cyan/violet accents, strong hierarchy, dense but readable cards, concise status pills and consistent bottom navigation.

## Scope truth
- NEXUS is local-first and heuristic; it does not claim LLM certainty.
- Memory search uses local normalization, fuzzy matching and bilingual concept expansion rather than a neural embedding model.
- OCR is local and best-effort; script/image quality can limit extracted text.
- “Start tracking” records user progress and does not execute arbitrary external work without explicit user interaction.
