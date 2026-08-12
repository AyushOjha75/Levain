# Levain — Domain Glossary

The ubiquitous language for this project. Terms here are canonical: code, UI copy, and docs use these words with exactly these meanings.

## Terms

### Starter
A perpetual sourdough culture that is kept alive indefinitely through repeated feedings. The app manages **many** Starters; each has its own identity (name), its own feeding history, and its own health record. A Starter is never consumed by a bake — it persists.

### Levain (bake build)
An offshoot built *from* a Starter for one specific bake. Unlike a Starter, a Levain is temporary: it is created, ripened, used in a bake, and gone. Not the same thing as the Starter it came from. (The app is named after this; the distinction still holds.)

### Feeding
The act of refreshing a Starter: discarding some portion and adding fresh flour and water. The core maintenance event in a Starter's history.

### Maintenance loop
The perpetual cycle that keeps a Starter alive: feed → rise → (observe) → repeat. Distinct from the usage loop.

### Usage loop
The cycle of *using* a Starter: build a Levain from it → bake with the Levain → record the outcome. Bake outcomes can be traced back to the condition of the source Starter at build time. In scope for this app.

### Health observation
A recorded assessment of a Starter's condition. First-class in this domain (not a footnote on a Feeding) and the app's highest-priority concept. Structured parts: **rise rating** (`peaked` / `rising` / `sluggish` / `flat`, with optional time-to-peak), **smell** (picklist), **photo**. Everything else is free text.

### Lifecycle state
Every Starter is in exactly one state: **active** (in regular use, frequent feeding expected), **dormant** (hibernating, e.g. in the fridge; infrequent feeding expected), or **archived** (kept for history only; the app expects nothing of it). State determines what "overdue" means for that Starter.

### Bake
A record of the usage loop, deliberately minimal: date, source Starter, levain build notes, outcome rating, photo. A Bake links bread outcomes back to the condition of its source Starter. Levain builds are folded into the Bake as notes — not a separate entity.

### Feeding interval
Each Starter has a configurable feeding interval per lifecycle state (e.g. every 24h while active, every 7 days while dormant). A Starter is **due** when the interval has elapsed since its last Feeding, and **overdue** once past it. Archived Starters are never due.

### Timeline
The chronological history of a single Starter: Feedings, Health observations, and Bakes interleaved. The v1 history view — charts and richer trends are out of scope for v1.

## Notes on scope (v1)

- v1 includes the full loop: dashboard, Feedings, Health observations, reminders, Timeline, **and Bake logging**.
- A Feeding captures: timestamp (auto), ratio (starter:flour:water), flour type — pre-filled from the previous Feeding so the common case is two taps.
- Reminders: one notification per Starter when it comes due, fired once, no re-nagging — the dashboard's overdue status is the persistent signal. Starters coming due together are coalesced into one notification, never a flood.
- Photos are app-private (not in the device gallery); the manual export zip is the escape hatch.
- Personal-first: no accounts, no backend; data lives on-device. Android Auto Backup covers the database; a manual full export (data + photos) covers the rest.
- The app opens to a dashboard of Starter cards: state, last feeding, last observation, due status.
