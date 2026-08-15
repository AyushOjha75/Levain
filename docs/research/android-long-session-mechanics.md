# Android mechanics for a 24-hour guided Bake session

Research for [issue #12](https://github.com/AyushOjha75/Levain/issues/12). Target: `minSdk 26`, `targetSdk 34`, Kotlin/Compose/Room.

**Question.** What is the right Android machinery for a stateful multi-hour Bake with ~20 timed Steps that must survive app kill, device reboot, Doze, and aggressive OEM battery management, on API 26–35?

Domain terms (`Bake`, `Step`, `Timed`/`Judged`/`Action`, `Hold`, `Projection`) are used as defined in `CONTEXT.md`.

## Contents

1. [Recommendation](#1-recommendation)
2. [Machinery comparison](#2-machinery-comparison)
3. [Does per-Step `setExactAndAllowWhileIdle` scale to 20 Steps?](#3-does-per-step-setexactandallowwhileidle-scale-to-20-steps)
4. [Notification actions from the shade](#4-notification-actions-from-the-shade)
5. [Recovery after process death and reboot](#5-recovery-after-process-death-and-reboot)
6. [OEM battery killers](#6-oem-battery-killers)
7. [Google Play policy constraints](#7-google-play-policy-constraints)
8. [Sources](#8-sources)

---

## 1. Recommendation

**Do not run a 24-hour foreground service. Keep Room as the source of truth, and drive the Bake with exactly one armed alarm at a time — the next due Timed Step — re-armed after every mutation. This is precisely the shape `ReminderCoordinator` already has; it generalises to 20 Steps without change of kind.**

Concretely, for a ~20-Step Bake over 24 hours:

| Concern | Mechanism |
|---|---|
| Session state | Room. `Bake` + snapshotted `Step` rows with absolute `dueAtEpochMs` / `completedAtEpochMs`. Survives process death and reboot for free. |
| The next prompt | **One** `AlarmManager.setAlarmClock()` for the earliest incomplete Timed Step. Re-armed on every mutation (Step completed, snoozed, `Hold` entered/resumed, `Projection` recalculated). Fall back to `setExactAndAllowWhileIdle()` where `setAlarmClock`'s status-bar alarm icon is unwanted. |
| Exact-alarm permission | `USE_EXACT_ALARM` for personal sideload (auto-granted on install, not user-revocable, API 33+); `SCHEDULE_EXACT_ALARM` + `canScheduleExactAlarms()` gate + rationale UI if ever listed on Play. Keep the existing graceful degrade to `setAndAllowWhileIdle()`. |
| Ambient "a Bake is running" UI | An **ongoing, non-foreground-service** notification (`setOngoing(true)`, low-importance channel), updated in place as Steps land. No `foregroundServiceType` to justify, no Android 15 timeout, no 24h battery cost. |
| The moment a Step fires | `BroadcastReceiver` → `goAsync()` → repository. Optionally promote to a `shortService` foreground service for the ~3-minute window where you want a loud, guaranteed-alive countdown UI. |
| Reboot | `BootCompletedReceiver` recomputes the next due Step from Room and re-arms. Already present — extend it to Bakes. |
| Belt-and-braces | A `WorkManager` periodic worker (~1–6h) that re-asserts the alarm. Cheap insurance against an OEM silently dropping it; never the primary timing mechanism. |

**Why not a foreground service.** On API 34+ every foreground service must declare a `foregroundServiceType` ([Android 14 behavior changes][b14]), and **no existing type describes a bake timer**. `shortService` is capped at ~3 minutes; `systemExempted` is reserved for device owners, VPN apps and similar; the only honest fit is `specialUse`, which requires a free-form `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification that Google Play reviews ([foreground service types][fgstypes]). You would also be asking the user to tolerate a permanent notification and a wakelocked process for 24 hours to deliver ~20 events — a job one alarm at a time does better. A foreground service is the right tool for *continuous* work (playback, navigation, an upload). A Bake is *discontinuous*: it is idle 99.9% of the time and needs to wake up ~20 times.

**Why `setAlarmClock` over `setExactAndAllowWhileIdle`.** Doze rate-limits allow-while-idle alarms: *"Neither `setAndAllowWhileIdle()` nor `setExactAndAllowWhileIdle()` can fire alarms more than once per nine minutes, per app."* ([Doze and App Standby][doze]). `setAlarmClock()` has no such cap and is explicitly Doze-exempt: *"Alarms set with `setAlarmClock()` continue to fire normally. The system exits Doze shortly before those alarms fire."* ([Doze and App Standby][doze]) The docs describe it as the highest-priority alarm the platform offers: *"Invoke an alarm at a precise time in the future. Because these alarms are highly visible to users, the system never adjusts their delivery time. The system identifies these alarms as the most critical ones and leaves low-power modes if necessary to deliver the alarms."* ([Schedule alarms][alarms])

The cost is visibility: `setAlarmClock` surfaces the next alarm to the system (status-bar alarm icon, `getNextAlarmClock()`), which some users read as "my 6am alarm". For a Bake this is arguably a feature — "next Step at 14:30" is exactly what the baker wants to see — but it is the one real trade-off. A reasonable split: `setAlarmClock` for Steps during a `Hold` or overnight retard where reliability is paramount, `setExactAndAllowWhileIdle` for daytime Steps more than 9 minutes apart.

---

## 2. Machinery comparison

### 2.1 Foreground service

**API 26–33.** Legal, no type required before 34 (types were optional from API 29). Started with `startForegroundService()` + `startForeground()` within 5 seconds.

**API 31+ — cannot be started from the background.** *"Apps that target Android 12 or higher can't start foreground services while running in the background, except for a few special cases. If an app attempts to start a foreground service while running in the background, an exception occurs"* ([Android 12 behavior changes][b12]). This matters directly: a service started from a `BroadcastReceiver` when a Step's alarm fires is a background start. The relevant escape hatch is that the platform exempts an app that is *"executing a job that was scheduled with a high priority"* or that has been granted temporary allowlisting by an exact alarm — but the guidance the same page gives is the important one: *"To complete time-sensitive actions that the user requests, start foreground services within an exact alarm."* ([Android 12 behavior changes][b12]) So the alarm is the enabling primitive even in the foreground-service design.

**API 34 — a type is mandatory.** *"If your app targets Android 14 (API level 34) or higher, it must specify at least one foreground service type for each foreground service within your app."* ([Android 14 behavior changes][b14]) The full type list ([foreground service types][fgstypes]) is `camera`, `connectedDevice`, `dataSync`, `health`, `location`, `mediaPlayback`, `mediaProcessing`, `mediaProjection`, `microphone`, `phoneCall`, `remoteMessaging`, `shortService`, `specialUse`, `systemExempted`. Evaluated against a bake timer:

| Type | Verdict for a Bake |
|---|---|
| `dataSync` | **Misuse.** Scoped to *"data upload or download, backup-and-restore, import/export, fetch data, local file processing, transfer data between device and cloud"* ([fg service types][fgstypes]). A bake timer syncs nothing. Also capped at 6h/24h on API 35 (below). |
| `health` | **Misuse.** *"Fitness tracking, sensor-based health monitoring."* Sourdough is not a health sensor. |
| `shortService` | **Legal, but ~3 minutes.** No type-specific permission needed; *"cannot be combined with other service types"*, no sticky services, cannot start other foreground services; on timeout the system calls `Service.onTimeout()` and ANRs the app if it does not stop. *"Battery optimization settings don't exempt shortService from timeout."* ([fg service types][fgstypes]) Useful for a Step's alert window, useless for a session. |
| `systemExempted` | **Not available in practice.** Requires being a Device Owner, Profile Owner, `ROLE_EMERGENCY` holder, device-admin, demo mode, or a configured VPN app; declaring it without eligibility throws `ForegroundServiceTypeNotAllowedException` ([fg service types][fgstypes]). |
| `specialUse` | **The only honest fit — with a Play review attached.** Requires `FOREGROUND_SERVICE_SPECIAL_USE` plus `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="…"/>`, and the free-form explanation *"is reviewed during Google Play submission"* ([fg service types][fgstypes]). |

**API 35 — timeouts arrive.** `dataSync` and the new `mediaProcessing` are limited to *"a total of 6 hours in a 24-hour period, after which the system calls the running service's `Service.onTimeout(int, int)` method"*; the service then *"has a few seconds to call `Service.stopSelf()`"* or the system throws `android.app.RemoteServiceException: "A foreground service of type dataSync did not stop within its timeout"* ([Android 15 behavior changes][b15]). The 6 hours is shared across all the app's `dataSync` services and only resets when *"the user brings the app to the foreground"*. A 24-hour bake therefore **cannot** be carried by a `dataSync` service on API 35 even if you were willing to misuse the type — it would die around hour 6 with a fatal exception. `specialUse` is not currently timed out, but the direction of travel is unambiguous.

**API 35 — `BOOT_COMPLETED` cannot start most types.** *"`BOOT_COMPLETED` receivers are not allowed to launch the following types of foreground services: `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection`, `microphone`"*, and doing so throws `ForegroundServiceStartNotAllowedException` ([Android 15 behavior changes][b15]). A design that restores a Bake by restarting a `dataSync` service at boot is dead on API 35. Restoring by *re-arming an alarm* at boot is not affected.

### 2.2 Exact alarms

Three primitives ([Schedule alarms][alarms]):

- `setExactAndAllowWhileIdle()` — *"Invoke an alarm at a nearly precise time in the future, even if battery-saving measures are in effect."* Subject to the 9-minute cap.
- `setAndAllowWhileIdle()` — inexact; *"On Android 12 (API level 31) and higher, the system invokes the alarm within one hour of the supplied trigger time."* Also 9-minute capped. This is the app's current degraded path, and a one-hour slop on a 30-minute fold is a wrong answer.
- `setAlarmClock()` — precise, Doze-exempt, *"the system never adjusts their delivery time"*.

**Permissions.** From API 31, exact alarms require the *"Alarms & reminders"* special app access via `SCHEDULE_EXACT_ALARM`, and *"Exact alarms should only be used for user-facing features"* ([Android 12 behavior changes][b12]). From API 33 there are two options ([Schedule alarms][alarms]):

| | `SCHEDULE_EXACT_ALARM` | `USE_EXACT_ALARM` |
|---|---|---|
| Granted | By the user | Automatically, on install |
| Revocable | Yes, by user or system | No |
| Pre-granted on fresh install (33+) | No | Yes |
| Play gating | Existing Play policies | Restricted — publication blocked unless you qualify |

On API 33+, `USE_EXACT_ALARM` is *"a normal permission"* intended for apps that *"need to send calendar reminders, wake-up alarms, or alerts when the app is no longer running"* ([Android 14: exact alarms denied by default][b14alarm]). On API 34 the `SCHEDULE_EXACT_ALARM` permission is denied by default; only platform-signed apps, privileged apps, apps on the power allowlist, and `SYSTEM_WELLBEING` role holders are exempt ([Android 14: exact alarms denied by default][b14alarm]).

**Which one does a bake timer qualify for?** Play's Exact Alarm Permission policy lists exactly two eligible use cases: *"The app is an alarm or timer app"* and *"The app is a calendar app that shows event notifications"* ([Play: Permissions and APIs that Access Sensitive Information][playperms]). A guided Bake is, in its timing behaviour, **a timer app** — it exists to tell the baker "fold now, 30 minutes elapsed". That is a defensible `USE_EXACT_ALARM` claim, and for a personal sideloaded build it is unambiguously the better choice: auto-granted, not revocable, and no settings round-trip. If Levain is ever listed, the honest position is that a *sourdough tracker* whose timers are one feature among many is closer to the "not covered above" case, for which the policy says *"you should evaluate if using `SCHEDULE_EXACT_ALARM` as an alternative is an option"* ([Play][playperms]). Recommendation: ship `SCHEDULE_EXACT_ALARM` as the default (it is what `AlarmDueScheduler` already checks via `canScheduleExactAlarms()`), and treat `USE_EXACT_ALARM` as a build-flavour decision made at listing time.

**Permission revocation is a kill switch.** *"When the `SCHEDULE_EXACT_ALARM` permission is revoked for your app, your app stops, and all future exact alarms are canceled"* ([Schedule alarms][alarms]). The app must therefore listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, re-check `canScheduleExactAlarms()`, and reschedule — the docs say this *"logic should be similar to what your app does when it receives the `ACTION_BOOT_COMPLETED` broadcast"* ([Schedule alarms][alarms]). Levain has the boot path already; the permission-change receiver is missing and should be added alongside it.

### 2.3 WorkManager

**Not a timing mechanism for Steps.** WorkManager's contract is deferrable, guaranteed execution — the opposite of "wake me in exactly 30 minutes". The alarms guide itself steers exact work away from it, offering *"Use WorkManager API with expedited work requests"* only as a Doze workaround for non-precise work ([Schedule alarms][alarms]). Its work is subject to the App Standby job quotas below, and periodic work has a 15-minute floor.

**Where it does belong:** a low-frequency periodic worker that re-asserts the next alarm (OEM insurance, §6), and post-Bake housekeeping. Under App Standby, a worker is not free either — see the quota table in §3.

### 2.4 Combinations

- **Alarm + ongoing notification (recommended).** All the reliability of an exact alarm; the "a Bake is running" affordance without a foreground service.
- **Alarm → `shortService`.** Alarm fires, receiver starts a `shortService` FGS for a live countdown / loud alert, service stops inside 3 minutes. Legal on 34+ without a reviewed type. Note `Service.onTimeout()` does not exist below API 34, so the service must stop itself on its own timer for older devices ([fg service types][fgstypes]).
- **Alarm + WorkManager watchdog.** Alarm is primary; a periodic worker verifies an alarm is armed and re-arms if not.
- **Long-lived `specialUse` FGS (not recommended).** Only if a Play reviewer accepts the justification, and it still does not survive an aggressive OEM (§6) — it buys process priority, not immortality.

---

## 3. Does per-Step `setExactAndAllowWhileIdle` scale to 20 Steps?

**The framing is the trap, and the existing code already avoids it.** `ReminderCoordinator.reschedule()` does not schedule one alarm per Starter — it computes `minOrNull()` over due times and arms a *single* alarm, then re-arms on the next firing. Twenty Steps is not twenty alarms; it is **one alarm, re-armed twenty times**. That property is what makes the design scale, and it should be preserved verbatim for Bakes.

With that said, three real ceilings exist:

**(a) The 9-minute floor between firings.** *"Neither `setAndAllowWhileIdle()` nor `setExactAndAllowWhileIdle()` can fire alarms more than once per nine minutes, per app."* ([Doze and App Standby][doze]) Over 24 hours, 20 Steps average one every 72 minutes, so the cap is not structurally violated. It bites in two specific places:

- **Clustered Steps.** Shaping sequences and oven work are minutes apart — "score the loaf", "add steam", "drop to 220 °C after 20 minutes", "vent the oven". Any two consecutive Timed Steps under 9 minutes apart will have the second one deferred.
- **Per app, not per alarm.** The cap is shared with the existing feeding-due alarm. A Bake Step and a `Starter` feeding falling within 9 minutes of each other will collide.

Mitigations, in order of preference: use `setAlarmClock()` (uncapped, Doze-exempt) for the Bake's Steps; and coalesce — the coordinator already has a `coalesceWindow` concept, and sub-9-minute Steps should be folded into one prompt carrying both instructions rather than fought for with two alarms.

**(b) App Standby buckets.** A 24-hour Bake with a long overnight `Hold` is exactly the pattern that demotes an app. Published limits ([Power management resource limits][powerdetails]):

| Bucket | Regular jobs | Expedited jobs | Alarms | Network |
|---|---|---|---|---|
| Active | 20 min / rolling 60 min | 30 min / rolling 24h | No execution limits | Unrestricted |
| Working set | 10 min / rolling 4h | 15 min / rolling 24h | **10 per hour** | Unrestricted |
| Frequent | 10 min / rolling 12h | 10 min / rolling 24h | **2 per hour** | Unrestricted |
| Rare | 10 min / rolling 24h | 10 min / rolling 24h | **1 per hour** | Disabled |
| Restricted | Once/day, 10 min | 5 min / rolling 24h | **1 per day, exact or inexact** | Disabled |

At one alarm at a time, even the Rare bucket's 1/hour is survivable and Restricted's 1/day is the only genuinely fatal tier. Note that the restrictions apply on battery only (Restricted excepted), and that a Bake is a high-interaction session — the user ticking Steps off keeps the app in Active for most of the run. The danger window is the unattended overnight retard, which is precisely where `setAlarmClock` should be used.

**(c) Permission revocation and reboot** wipe every pending alarm, so the count of *pending* alarms is not the state — Room is. See §5.

**Verdict.** `setExactAndAllowWhileIdle` generalises to a 20-Step Bake **structurally** (one alarm re-armed, not twenty pending), but **degrades** on clustered Steps because of the 9-minute cap and on unattended overnight stretches because of bucket demotion. The fix is not a foreground service — it is `setAlarmClock()` plus coalescing sub-9-minute Steps into a single prompt.

---

## 8. Sources

- [alarms]: Schedule alarms — https://developer.android.com/develop/background-work/services/alarms/schedule
- [doze]: Optimize for Doze and App Standby — https://developer.android.com/training/monitoring-device-state/doze-standby
- [fgstypes]: Foreground service types are required (Android 14) — https://developer.android.com/develop/background-work/services/fgs/service-types
- [b12]: Behavior changes: Apps targeting Android 12 — https://developer.android.com/about/versions/12/behavior-changes-12
- [b14]: Behavior changes: Apps targeting Android 14 — https://developer.android.com/about/versions/14/behavior-changes-14
- [b14alarm]: Schedule exact alarms are denied by default — https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- [b15]: Behavior changes: Apps targeting Android 15 — https://developer.android.com/about/versions/15/behavior-changes-15
- [powerdetails]: Power management resource limits — https://developer.android.com/topic/performance/power/power-details
- [playperms]: Play — Permissions and APIs that Access Sensitive Information — https://support.google.com/googleplay/android-developer/answer/9888170

[alarms]: https://developer.android.com/develop/background-work/services/alarms/schedule
[doze]: https://developer.android.com/training/monitoring-device-state/doze-standby
[fgstypes]: https://developer.android.com/develop/background-work/services/fgs/service-types
[b12]: https://developer.android.com/about/versions/12/behavior-changes-12
[b14]: https://developer.android.com/about/versions/14/behavior-changes-14
[b14alarm]: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
[b15]: https://developer.android.com/about/versions/15/behavior-changes-15
[powerdetails]: https://developer.android.com/topic/performance/power/power-details
[playperms]: https://support.google.com/googleplay/android-developer/answer/9888170
