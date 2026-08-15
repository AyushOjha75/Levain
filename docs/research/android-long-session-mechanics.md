# Android mechanics for a 24-hour guided Bake session

Research for [issue #12](https://github.com/AyushOjha75/Levain/issues/12). Target: `minSdk 26`, `targetSdk 34`, Kotlin/Compose/Room.

**Question.** What is the right Android machinery for a stateful multi-hour Bake with ~20 timed Steps that must survive app kill, device reboot, Doze, and aggressive OEM battery management, on API 26–35?

> Status: in progress. Sections are appended as sources are read; every claim carries a citation.

## Contents

1. [Recommendation](#1-recommendation)
2. [Machinery comparison](#2-machinery-comparison)
3. [Does per-Step `setExactAndAllowWhileIdle` scale to 20 Steps?](#3-does-per-step-setexactandallowwhileidle-scale-to-20-steps)
4. [Notification actions from the shade](#4-notification-actions-from-the-shade)
5. [Recovery after process death and reboot](#5-recovery-after-process-death-and-reboot)
6. [OEM battery killers](#6-oem-battery-killers)
7. [Google Play policy constraints](#7-google-play-policy-constraints)
8. [Sources](#8-sources)
