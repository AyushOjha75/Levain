# Levain 🍞

A personal Android app to track and maintain sourdough starters.

## What it does

- **Dashboard** — every Starter at a glance: lifecycle state, last feeding, last observation, due status
- **Feedings** — two-tap logging, pre-filled from last time (ratio, flour)
- **Health observations** — rise rating, time-to-peak, smell, photo, notes
- **Bakes** — outcome ratings linked back to the source starter's condition
- **Reminders** — one coalesced notification when starters come due; fired once, never nagging
- **Timeline** — each starter's full story, chronologically

All data stays on-device. No accounts, no backend.

The domain glossary lives in [CONTEXT.md](CONTEXT.md); the v1 spec is
[issue #1](https://github.com/AyushOjha75/Levain/issues/1).

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · manual DI.
Tests run on the JVM at the ViewModel seam (Robolectric + in-memory Room)
with an injected `Clock` and fake alarm scheduler.

## Build

```
./gradlew :app:assembleDebug        # build APK
./gradlew :app:testDebugUnitTest    # run the test suite
```

Requires JDK 17 and the Android SDK (platform 34).
