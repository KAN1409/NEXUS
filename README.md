# NEXUS

Local-first personal intelligence agent for Android.

## Product contract
Observe → Remember → Model interests → Discover → Rank → Prepare action → User approval → Execute.

## Update 1/10
- Android app foundation
- Package `com.kareem.nexus`
- Compose + StateFlow + Hilt
- Room persistence model
- AppSearch dependency wired for Update 2 indexing
- WorkManager dependency wired for future background intelligence
- Domain models: observations, interests, knowledge entities, memories, discoveries, actions, feedback
- First working For You surface backed by Room
- CI workflow builds a debug APK

## Build
Requires JDK 17, Gradle 9.6 and Android SDK 37.

```bash
gradle :app:assembleDebug
```
