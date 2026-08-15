# Levain — Domain Glossary

The ubiquitous language for this project. Terms here are canonical: code, UI copy, and docs use these words with exactly these meanings.

## Baking

### Recipe
The reusable plan for one bread — bundled content that ships with the app, never user-authored in 1.0. A Recipe declares its **Ingredients** at a reference batch (one loaf, one 9×13 pan) and the template its **Steps** are built from. Structurally different breads are *different Recipes*: sourdough focaccia and yeasted focaccia are two Recipes, not one Recipe with a switch. A Recipe carries options only where they don't change the step sequence — scale and pan size, not leavening.

### Bake
One run of a Recipe, from the moment it starts to the loaf coming out — and afterwards, the record of that run. **The live session and the history entry are the same thing**, distinguished only by status: *planned*, *active*, *finished*, or *abandoned*. A Bake may also be logged directly as *finished* with no Steps, for bread baked away from the app.

A Bake holds a **scale** (the multiple of the Recipe's reference batch it was baked at), its resolved quantities, its Steps, and its outcome. Its full set of statuses is *planned*, *active*, *held*, *finished*, *abandoned* — an abandoned Bake stays in history, because a failed bake is data.

### Hold
A deliberate pause on an active Bake — retarding overnight in the fridge, or simply stopping. Distinct from a Step running long: a Hold stops the timeline recalculating altogether, and resuming recomputes the remaining Steps from the moment of resume.

### Projection
The Bake's remaining timeline, including its projected out-of-the-oven time. It is derived from actual Step completion times and recalculates whenever one lands early or late — a projection the app maintains, never a contract it holds the baker to. Every automatic behaviour adjusts the Projection; every irreversible action belongs to the baker.

### Step
One instruction within a Bake, **snapshotted from the Recipe when the Bake starts** — never read live from the Recipe, so updating bundled content can never rewrite a bake in progress or a bake in history. Repeats are expanded at snapshot: "four folds, thirty minutes apart" becomes four Steps, each with its own due time and its own completion time.

Every Step is exactly one kind:

- **Timed** — a real duration with a real timer (a 30-minute fold interval, a 20-minute bake).
- **Judged** — the baker decides it's done, guided by an estimated window *and* an observable cue ("jiggly and domed, risen by half"). The estimate is advisory; no timer can tell you bulk fermentation is over.
- **Action** — done the moment you do it, ticked off (preheat the oven, score the loaf). Never given a fake duration.

### Ingredient
A structured, scalable quantity on a Recipe: name, amount, unit, baker's percentage, and the phase it belongs to. Steps *reference* Ingredients rather than restating them, so a Bake's scale resolves to real gram amounts everywhere at once.

### Levain (bake build)
An offshoot built *from* a Starter for one specific bake: created, ripened, used, and gone — not the same thing as the Starter it came from. **A step type, not an entity**: building a levain is a Step inside a Bake, not something tracked in its own right. (The app is named after this. The collision is deliberate and accepted.)

### Usage loop
The cycle of *using* a Starter: pick a Recipe → run a Bake → keep its outcome. A Bake's result can be traced back to the condition of its source Starter — when it had one.

## Starters

### Starter
A perpetual sourdough culture kept alive indefinitely through repeated feedings. The app manages **many** Starters; each has its own identity, feeding history, and health record. A Starter is never consumed by a Bake — it persists.

A Starter is **not** the root of the domain: a Bake may have no Starter at all (yeasted breads), and retiring a Starter never destroys the Bakes made with it.

### Feeding
The act of refreshing a Starter: discarding some portion and adding fresh flour and water. The core maintenance event in a Starter's history.

### Maintenance loop
The perpetual cycle that keeps a Starter alive: feed → rise → (observe) → repeat. Distinct from the usage loop.

### Health observation
A recorded assessment of a Starter's condition. First-class (not a footnote on a Feeding). Structured parts: **rise rating** (`peaked` / `rising` / `sluggish` / `flat`, with optional time-to-peak), **smell** (picklist), **photo**. Everything else is free text.

### Lifecycle state
Every Starter is in exactly one state: **active** (in regular use, frequent feeding expected), **dormant** (hibernating, e.g. in the fridge; infrequent feeding expected), or **archived** (kept for history only; the app expects nothing of it). State determines what "overdue" means for that Starter.

### Feeding interval
Each Starter has a configurable feeding interval per lifecycle state (e.g. every 24h while active, every 7 days while dormant). A Starter is **due** when the interval has elapsed since its last Feeding, and **overdue** once past it. Archived Starters are never due.

### Timeline
The chronological history of a single Starter: Feedings, Health observations, and the Bakes made with it, interleaved.

### Streak / Milestone
Continuity and age celebrations ("fed on time 14 feedings running", "Rye is 100 days old"). Derived from the Timeline, never stored. A streak counts consecutive Feedings whose **fed timestamp** (not log time — back-filling preserves it) is on or before that feeding's due time; one late feeding resets it. A dormant Starter's streak pauses rather than breaks; archived Starters have no streaks.

### Insights
Per-Starter derived statistics: average feeding gap, on-time rate (each gap judged against its own era's interval), rise trend, bake count and average outcome. Derived from the Timeline, never stored.

## Presentation and content

### Avatar
The illustrated face of a Starter: a parametric jar character drawn in code (Compose Canvas), not image assets. Its **Mood** is derived from health data — **beaming** (recently peaked), **content** (default), **sleepy** (recently sluggish/flat), **hungry** (due or overdue), **resting** (dormant), **retired** (archived). Moods are derived, never stored. The jar is the Starter section's motif, not the app's brand.

### Wizard
A guided multi-step flow: *first-feed onboarding* (set up an existing Starter), *create-a-starter* (7-day from-scratch program with built-in reminders), *troubleshooting* (symptom → diagnosis → plan). A guided bake is **not** a Wizard — it is a Bake with Steps.

### Tip
Contextual micro-education attached to a specific moment (a smell picked, a rise rating chosen, a state change). Bundled with the app, not fetched. Distinct from **School articles** (longer bundled reads, later).

### Backup
The manual "export everything" escape hatch: one zip holding all data plus every photo, written wherever the user chooses. Importing a Backup replaces everything. A backup is only a backup if it's restorable.
