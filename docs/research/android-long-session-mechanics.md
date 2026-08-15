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

## 4. Notification actions from the shade

### 4.1 The shape of an action

A notification may carry *"up to three action buttons that let the user respond quickly, such as to snooze a reminder"*, and they *"must not duplicate the action performed when the user taps the notification"* ([Create a notification][notify]). Three is the budget, which maps neatly onto a Step prompt: **Done**, **Snooze**, **Extend** (or `Hold`). The Bake's own screen carries everything else via `setContentIntent`.

Each action is a `PendingIntent`. The three targets:

| Target | Fit for Levain |
|---|---|
| `getBroadcast()` | **Correct choice.** The docs recommend it precisely for this: *"instead of launching an activity, you can do other things such as start a `BroadcastReceiver` that performs a job in the background so that the action doesn't interrupt the app that's already open"* ([Create a notification][notify]). Marking a Step done should not yank the user into the app. |
| `getActivity()` | Right for the notification *body* tap (open the Bake), wrong for Done/Snooze. |
| `getService()` | Avoid — on API 31+ a background service start is restricted, and a foreground service needs a type (§2.1). |

### 4.2 How the action reaches the repository

The path is the one `DueAlarmReceiver` already uses, and it is the correct one:

```
Notification action tapped
  → PendingIntent.getBroadcast fires
  → BroadcastReceiver.onReceive (main thread, ~10s budget)
  → goAsync()                       // extend past onReceive returning
  → CoroutineScope(Dispatchers.IO)
  → appContainer.repository.<mutation>()
  → coordinator.reschedule(now)     // re-arm the single alarm
  → result.finish()
```

`goAsync()` is what makes this safe: without it, the process may be killed the moment `onReceive` returns, mid-write. `DueAlarmReceiver` already does exactly this. For a Bake the receiver needs an `Intent` action plus a Step id extra to distinguish Done / Snooze / Extend — which drives the `PendingIntent` identity rules below.

**The mutation must be idempotent.** A user can tap Done on a stale notification long after the Step was completed in-app. `ReminderCoordinator` already models this correctly for feedings via `lastNotifiedDueAtEpochMs`; a Step needs the same guard — completing an already-complete Step is a no-op, not a second completion that shifts the `Projection`.

### 4.3 PendingIntent flags

**Mutability is mandatory from API 31.** *"If your app attempts to create a `PendingIntent` object without setting either mutability flag, the system throws an `IllegalArgumentException`"* with the message *"Targeting S+ (version 31 and above) requires that one of `FLAG_IMMUTABLE` or `FLAG_MUTABLE` be specified when creating a PendingIntent"* ([Intents and intent filters][intents]). The guidance is to *"Create immutable pending intents whenever possible"*; `FLAG_MUTABLE` is needed only for direct-reply (`RemoteInput`) and bubbles ([Intents and intent filters][intents]). Levain's Done/Snooze/Extend actions carry no user text, so **`FLAG_IMMUTABLE` throughout**. `Adapters.kt` already uses `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` correctly.

**Identity is `(requestCode, Intent.filterEquals)` — and `filterEquals` ignores extras.** This is the single most common bug in per-item notification actions. Two `PendingIntent`s built from `Intent(ctx, StepActionReceiver::class)` with different `stepId` *extras* but the same `requestCode` are the **same** `PendingIntent`; with `FLAG_UPDATE_CURRENT` the second silently overwrites the first's extras, and every button then acts on one Step. The docs state the rule in the direct-reply context: *"If you reuse a `PendingIntent`, a user might reply to a different conversation than the one they intend. You must provide a request code that is different for each conversation or provide an intent that doesn't return `true` when you call `equals()` on the reply intent of any other conversation"* ([Create a notification][notify]).

Two fixes, use both:
- Give each `(stepId, action)` pair a distinct `requestCode`.
- Make the intents genuinely unequal — distinct `Intent.action` strings (`ACTION_STEP_DONE`, `ACTION_STEP_SNOOZE`, `ACTION_STEP_EXTEND`) and a `data` `Uri` such as `levain://bake/{bakeId}/step/{stepId}`, since `filterEquals` compares action, data, type, package, component and categories.

Note that `AlarmDueScheduler.pendingIntent()` currently hardcodes `requestCode = 0` with no action or data. For the single feeding alarm that is deliberate and correct — it is what makes `cancel()` and re-arm target the same alarm. When Bakes are added, the Bake alarm must use a *different* request code or a distinguishing action, or arming a Step alarm will silently cancel the feeding alarm.

**`FLAG_UPDATE_CURRENT` vs `FLAG_ONE_SHOT`.** `FLAG_UPDATE_CURRENT` (current usage) is right for the alarm, which is re-armed repeatedly. For notification actions, `FLAG_UPDATE_CURRENT` ensures a rebuilt notification's buttons carry fresh extras.

### 4.4 The API 31+ trampoline ban

*"Apps that target Android 12 or higher can't start activities from services or broadcast receivers that are used as notification trampolines. In other words, after the user taps on a notification, or an action button within the notification, your app cannot call `startActivity()` inside of a service or broadcast receiver."* ([Android 12 behavior changes][b12]) The system blocks the start and logs:

```
Indirect notification activity start (trampoline) from PACKAGE_NAME,
    this should be avoided for performance reasons.
```

The prescribed fix is to point the notification at the activity directly: *"Create a `PendingIntent` object that is associated with the activity that users see after they tap on the notification"* and use it via `setContentIntent()` ([Android 12 behavior changes][b12]).

**What this means for Levain, concretely.** The tempting pattern — one receiver that handles Done/Snooze *and*, for a `Judged` Step, opens the Bake screen so the baker can assess the crumb — is exactly the banned trampoline. Instead:

- Actions that only mutate state (Done, Snooze, Extend) → `getBroadcast` → receiver. Never call `startActivity` from it.
- Actions that must show UI (open the Bake, log a `Health observation`, judge a `Judged` Step) → `getActivity` with a deep-link `Intent` straight to `MainActivity`, resolved by the activity itself. One hop, no receiver in between.

The existing `DueNotificationPresenter` is already compliant: `setContentIntent` holds a `getActivity` `PendingIntent` to `MainActivity`, and `DueAlarmReceiver` never starts an activity.

### 4.5 Two operational constraints

- **Rate limiting.** *"Android applies a rate limit when updating a notification. If you post updates to a notification too frequently—many in less than one second—the system might drop updates"* ([Create a notification][notify]). A live countdown in the ongoing Bake notification must tick at most every few seconds — or better, use `setUsesChronometer(true)` with `setWhen(dueAtEpochMs)` and let the system render the countdown with zero updates.
- **Alerting once.** Use `setOnlyAlertOnce()` on the ongoing Bake notification *"so your notification interrupts the user—with sound, vibration, or visual clues—only the first time"* ([Create a notification][notify]). The per-Step prompt is a separate, higher-importance notification that *should* alert.
- **`POST_NOTIFICATIONS` (API 33+).** *"Android 13 (API level 33) and higher supports a runtime permission for posting non-exempt (including Foreground Services (FGS)) notifications from an app"* ([Create a notification][notify]). Without it, every prompt in a 24-hour Bake is silently dropped. `targetSdk 34` means this must be requested — ideally at the moment the user starts their first Bake, where the rationale is self-evident.

---

## 5. Recovery after process death and reboot

### 5.1 The rule

**Persist the facts; recompute the `Projection`.** Android will kill the process: *"To determine which processes to kill when low on memory, Android places each process into an importance hierarchy"*, and a backgrounded app is a cached process which *"the system is free to kill as needed"* ([Processes and app lifecycle][proclife]). Even a foreground service only buys "visible" ranking, not immunity — *"Only in very critical situations does the system get to a point where all cached processes are killed and it must start killing service processes"* ([Processes and app lifecycle][proclife]). A 24-hour Bake will outlive its process. Recovery is not a fallback path; it is the normal path.

### 5.2 Persist

Room, written synchronously at each transition:

| Persist | Why |
|---|---|
| `Bake` row: recipe id, `scale`, `status` (`planned`/`active`/`held`/`finished`/`abandoned`), `startedAtEpochMs` | Identity and lifecycle. `CONTEXT.md` already makes the live session and the history entry the same row — this is exactly right for recovery: there is no separate "session" object to lose. |
| `Step` rows, **snapshotted at Bake start**, with repeats already expanded | `CONTEXT.md`: Steps are *"snapshotted from the Recipe when the Bake starts — never read live from the Recipe"*. Recovery therefore never depends on bundled content that may have changed under an app update. |
| Per Step: `dueAtEpochMs`, `completedAtEpochMs`, `snoozedUntilEpochMs` | **Absolute epoch instants, never durations-from-now.** A duration is meaningless after a reboot; an `Instant` is not. |
| `Hold`: `heldAtEpochMs`, `resumedAtEpochMs` | A `Hold` *"stops the timeline recalculating altogether, and resuming recomputes the remaining Steps from the moment of resume"* — so the hold boundaries are facts, and everything downstream is derived. |
| Per Step: `lastNotifiedDueAtEpochMs` | The existing idempotence guard, generalised from `Starter` to `Step`. Stops a reboot at the wrong moment re-firing a prompt the baker already dismissed. |

### 5.3 Recompute, never persist

- The `Projection` — *"derived from actual Step completion times and recalculates whenever one lands early or late"* (`CONTEXT.md`). Storing it invites a stale projection surviving a crash.
- Which alarm is armed. The set of pending alarms is **not** durable state: reboot clears all of them, and so does revoking `SCHEDULE_EXACT_ALARM` (*"your app stops, and all future exact alarms are canceled"* — [Schedule alarms][alarms]). The alarm is a cache of "what Room says is next", rebuilt on demand.
- Elapsed time. Compute `now - startedAtEpochMs`; never accumulate a counter in memory.

### 5.4 Restore triggers

All four run the same function — a `BakeCoordinator.reschedule(now)` mirroring `ReminderCoordinator.reschedule`:

1. **`ACTION_BOOT_COMPLETED`.** `BootCompletedReceiver` exists and already calls into the repository; extend it to Bakes. Note it must **not** try to start a foreground service on API 35 for most types ([Android 15 behavior changes][b15]) — re-arming an alarm is unaffected.
2. **`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.** Currently missing. The docs prescribe a receiver that confirms `canScheduleExactAlarms()` then *"Reschedules any exact alarms that your app needs, based on its current state"* ([Schedule alarms][alarms]).
3. **App start / `ON_START`.** Cheap self-heal for the OEM case (§6): if the app is open and no alarm is armed, arm one.
4. **`ACTION_MY_PACKAGE_REPLACED`.** App updates kill alarms too.

### 5.5 Catch-up on restore

Restoring after a gap must handle Steps whose `dueAt` is already in the past — a phone off for two hours mid-bulk, or an OEM that swallowed the alarm. `ReminderCoordinator` already encodes the right answer: `scheduler.scheduleExact(maxOf(nextDueAt, now))` — *"past-due events still need a firing; clamped to now"*. For a Bake, that clamp should fire once with an honest "this Step was due 40 minutes ago" and let the `Projection` absorb the drift, rather than firing a burst of ~5 stale prompts. This is the same coalescing instinct as the existing 15-minute `coalesceWindow`, applied backwards in time.

---

## 6. OEM battery killers

### 6.1 What actually happens

dontkillmyapp.com rates vendors by how badly they break background work; **Huawei, Xiaomi, OnePlus and Samsung all sit at the worst tier (5/5)**, with Meizu, Asus and Oppo at 4/5, while AOSP-line devices (Pixel, Android One), Nokia/HMD and HTC rate clean ([dontkillmyapp][dkma]). The characterisation is blunt: manufacturers *"prefer battery life over proper functionality of your apps"*, and under default settings *"background processing simply does not work right and apps using them will break"* ([dontkillmyapp: Xiaomi][dkmaxiaomi]).

Mechanically, three distinct things are done, none of them documented APIs:

1. **Autostart / boot denial.** MIUI's Autostart Manager gates whether the app may run at boot at all ([dontkillmyapp: Xiaomi][dkmaxiaomi]). If denied, `BOOT_COMPLETED` never arrives and §5.4's primary restore trigger is dead.
2. **Aggressive process termination.** Swiping the app from recents, or the screen going off, can kill the process *and* clear its scheduled alarms — including foreground services. Pinning/locking the app in the recents tray is the vendor-blessed workaround ([dontkillmyapp: Xiaomi][dkmaxiaomi]).
3. **Vendor battery savers layered on top of Doze.** MIUI's per-app "App Battery Saver" modes, Samsung's sleeping/deep-sleeping app lists, and OnePlus's advanced optimisation each add restrictions AOSP does not, and none of them are visible to `PowerManager.isIgnoringBatteryOptimizations()`.

**The consequence for the design:** a long-running foreground service is *not* the mitigation people assume. It is precisely the thing MIUI and OneUI target. An exact alarm re-armed from durable Room state degrades better — it needs the process alive only for milliseconds, twenty times.

### 6.2 Accepted mitigations

**In the app:**

- **Room as source of truth + re-arm on every app open** (§5.4 trigger 3). This is the mitigation that actually works, because it does not depend on the OEM honouring anything: the next time the baker looks at their phone, the schedule self-heals.
- **`setAlarmClock()`.** Vendor skins are markedly more careful with alarm-clock alarms than with generic ones, because killing them produces the one bug users unambiguously blame the phone for.
- **A WorkManager periodic watchdog** that re-asserts the alarm. Belt and braces, and cheap.
- **Detect and tell the user.** `PowerManager.isIgnoringBatteryOptimizations()` gives the AOSP half of the picture ([Doze and App Standby][doze]); MIUI's autostart state can be read via the community `MIUI-autostart` library ([dontkillmyapp: Xiaomi][dkmaxiaomi]). When a long Bake is being started on a known-bad vendor, a one-time explainer pointing at the right settings screen is the accepted pattern — dontkillmyapp exists to be linked to.
- **`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`** to deep-link the user to the battery-optimisation settings screen. *"Most apps can invoke an intent containing `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which takes users to the battery optimization settings where they can manually exempt the app"* ([Doze and App Standby][doze]). This is the Play-safe route; see §7 for why the direct-request variant is not.

**Note what an exemption does not buy you.** Even a granted exemption *"allows use of the network during Doze and App Standby"* and *"the ability to hold partial wake locks"* — but *"Regular `AlarmManager` alarms do not fire"* and jobs/syncs stay deferred ([Doze and App Standby][doze]). So it is not a substitute for using the right alarm API; it is orthogonal.

**Outside the app:** dontkillmyapp's own developer advice is to report device-specific breakage to Google via its IssueTracker template, and it points at Google's CTS-D (Compatibility Test Suite for Data) as the lever slowly forcing vendors into line ([dontkillmyapp][dkma]).

### 6.3 What shipping cooking-timer apps do

The observable pattern across long-session cooking and fermentation apps (and it is a pattern of resignation, not cleverness): exact alarms for the prompts, an ongoing notification rather than a 24-hour foreground service, full state in a local database so the timeline can be rebuilt from cold, a `BOOT_COMPLETED` receiver, and a first-run or first-long-session interstitial that tells the user to exempt the app from battery optimisation — usually linking dontkillmyapp.com. None of them solve the OEM problem; they detect it, warn about it, and make recovery cheap. **This is the correct posture for Levain too: assume the alarm may be lost, and make the cost of losing it a late prompt rather than a corrupt Bake.**

---

## 7. Google Play policy constraints

Relevant only if Levain is ever listed — it is personal-first today — but each of these constrains a design decision above, so they are worth fixing now rather than retrofitting.

**1. Exact alarms.** `USE_EXACT_ALARM` is a restricted permission with exactly two eligible use cases: *"The app is an alarm or timer app"* and *"The app is a calendar app that shows event notifications"*; otherwise *"you should evaluate if using `SCHEDULE_EXACT_ALARM` as an alternative is an option"* ([Play: Permissions and APIs][playperms]). Enforcement is at publication: *"Apps will not be able to publish a version of their app with this permission in the manifest unless they qualify based on the policy language"* ([Android 14: exact alarms denied by default][b14alarm]). A Bake timer has a real claim to "timer app", but a sourdough *tracker* is a mixed case. **Design implication: keep the exact-alarm call behind the `DueScheduler` interface it is already behind, so switching permission model is a one-file change.**

**2. Foreground service types are declared and reviewed.** All foreground services must be declared on the Play Console app content page, and `specialUse` additionally requires a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation that *"is reviewed during Google Play submission"* ([foreground service types][fgstypes]). **Design implication: the recommended no-FGS design has nothing to declare and nothing to justify — a real, if unglamorous, advantage.**

**3. Battery-optimisation exemption is close to prohibited.** *"Google Play policies prohibit apps from requesting direct exemption from Power Management features—Doze and App Standby—in Android 6.0 and above unless the core function of the app is adversely affected"* ([Doze and App Standby][doze]). The published acceptable list — messaging/VOIP that technically cannot use FCM, safety apps, task-automation apps, peripheral companion apps needing a persistent connection — does not include cooking timers ([Doze and App Standby][doze]). **Design implication: use `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (settings deep link, allowed for most apps) and never `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (direct request, policy-gated).**

**4. Notifications.** `POST_NOTIFICATIONS` is a runtime permission from API 33 ([Create a notification][notify]); Play's broader policy expects notifications to be functional and requested with context. Requesting at "start your first Bake" rather than at cold launch is both better UX and safer ground.

**5. General.** Nothing in the recommended design touches a sensitive or restricted permission beyond the exact-alarm choice: no location, no background location, no `QUERY_ALL_PACKAGES`, no accessibility service, no `SYSTEM_ALERT_WINDOW` (which Android 15 narrowed as an FGS-start exemption anyway — an app now *"needs to have the `SYSTEM_ALERT_WINDOW` permission and also have a visible overlay window"* ([Android 15 behavior changes][b15])).

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
- [notify]: Create a notification — https://developer.android.com/develop/ui/views/notifications/build-notification
- [intents]: Intents and intent filters (Declare the mutability of a PendingIntent) — https://developer.android.com/guide/components/intents-filters
- [proclife]: Processes and app lifecycle — https://developer.android.com/guide/components/activities/process-lifecycle
- [dkma]: Don't kill my app! — https://dontkillmyapp.com/
- [dkmaxiaomi]: Don't kill my app! — Xiaomi — https://dontkillmyapp.com/xiaomi

All primary sources retrieved 2026-08-16. Behaviour statements are tied to the API level named in the text; Android's power-management numbers are explicitly *"not guaranteed"* and *"subject to change in future Android releases"* ([Power management resource limits][powerdetails]), so the App Standby quota table should be treated as indicative, not contractual.

[alarms]: https://developer.android.com/develop/background-work/services/alarms/schedule
[doze]: https://developer.android.com/training/monitoring-device-state/doze-standby
[fgstypes]: https://developer.android.com/develop/background-work/services/fgs/service-types
[b12]: https://developer.android.com/about/versions/12/behavior-changes-12
[b14]: https://developer.android.com/about/versions/14/behavior-changes-14
[b14alarm]: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
[b15]: https://developer.android.com/about/versions/15/behavior-changes-15
[powerdetails]: https://developer.android.com/topic/performance/power/power-details
[playperms]: https://support.google.com/googleplay/android-developer/answer/9888170
[notify]: https://developer.android.com/develop/ui/views/notifications/build-notification
[intents]: https://developer.android.com/guide/components/intents-filters
[proclife]: https://developer.android.com/guide/components/activities/process-lifecycle
[dkma]: https://dontkillmyapp.com/
[dkmaxiaomi]: https://dontkillmyapp.com/xiaomi
