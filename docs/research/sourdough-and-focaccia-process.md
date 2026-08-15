# Sourdough and Focaccia: Real Process Parameters

Research for [issue #11](https://github.com/AyushOjha75/Levain/issues/11). Purpose: gather
process parameters precise enough to author guided recipes from, and answer the two
structural questions the step engine depends on — which steps are timed vs feel-judged,
and where the two breads share vs diverge in structure.

Sources are high-trust baking references, cited inline. **Where sources genuinely disagree,
the disagreement is reported rather than averaged.**

---

## 1. Canonical sourdough loaf

The reference recipes used throughout this section:

- [The Perfect Loaf — Beginner's Sourdough Bread](https://www.theperfectloaf.com/beginners-sourdough-bread/) (TPL)
- [Tartine Bread — Chad Robertson, Basic Country Bread](https://feelingfoodish.com/tartine-bread/), cross-checked against
  [Farine's detailed write-up](http://www.farine-mc.com/2010/10/chad-robertsons-basic-country-bread.html)
  and [The Fresh Loaf](https://www.thefreshloaf.com/node/64305/tartine-basic-country-bread)
- [King Arthur Baking — Extra-Tangy Sourdough Bread](https://www.kingarthurbaking.com/recipes/extra-tangy-sourdough-bread-recipe)
  and [King Arthur's guide to baking sourdough bread](https://www.kingarthurbaking.com/learn/sourdough) (KAB)

### 1.1 Flour and hydration

| Source | Flour blend | Total hydration |
| --- | --- | --- |
| [TPL Beginner's Sourdough](https://www.theperfectloaf.com/beginners-sourdough-bread/) | 80% bread flour, 15% whole wheat, 5% rye | **72%** |
| [Tartine Basic Country](https://feelingfoodish.com/tartine-bread/) | ~90% white bread flour, ~10% whole wheat | **75%** |
| [TPL High(er) Hydration](https://www.theperfectloaf.com/higher-hydration-sourdough-bread/) | high-protein white base | 80%+ |
| KAB house recipes | 100% unbleached bread/AP flour, no whole grain | ~65–68% |

**Working range: 65–80% hydration**, with 72–75% the canonical middle. The lower KAB end is
tied to using all white flour and a pan/free-form shape; whole grain in the blend pushes
hydration up because bran and germ absorb more water.
[TPL's hydration guide](https://www.theperfectloaf.com/dough-hydration/) is explicit that the
same percentage behaves differently across flours, so hydration is a **recipe parameter, not a
step**, and cannot be used as a step cue.

Salt is stable across every source at **1.8–2.2% of flour weight**.

### 1.2 Levain build: ratio, timing, ripeness cues

Two clearly different build philosophies, and they are a genuine divergence, not a rounding
difference:

**TPL — stiff-ish, small, fast build.** 38 g ripe 100% starter : 38 g whole wheat : 38 g bread
flour : 76 g water — i.e. **1 : 2 : 2 (starter : flour : water)**. Ripe in **5–6 hours at
74–76°F (23–24°C)**.

**Tartine — very dilute, slow overnight build.** 1 tablespoon (~15 g) mature starter : 200 g
flour : 200 g water — roughly **1 : 13 : 13**. Ripe in **8–10 hours** at room temperature,
built the night before.

The ratio drives the timing: a small seed inoculation takes overnight, a large one takes an
afternoon. Both land at the same endpoint.

**Ripeness cues (feel-judged, not timed):**

- TPL: "expanded, bubbly on top, inside, and at the sides, and have a slightly sour aroma."
- Tartine: **doubled in height**, plus the **float test** — a spoonful dropped in water floats.
- KAB's [float test article](https://www.kingarthurbaking.com/blog/2019/01/02/the-float-test-for-yeast-dough-and-sourdough-starter)
  endorses the float test for *starter* but cautions it is not reliable for judging *dough*.

> **Disagreement worth carrying into the app:** the float test is treated as authoritative by
> Tartine and as a rough indicator by KAB and TPL, who both prefer "risen and domed then just
> beginning to recede" plus aroma. Offer float as a secondary confirmation, not the primary cue.

### 1.3 Dough temperature and bulk fermentation

**Dough temperature is the single strongest driver of bulk duration**, which is why every
serious source specifies a target final/desired dough temperature (FDT/DDT) rather than only a
clock time.

| Source | Target dough temp | Bulk duration |
| --- | --- | --- |
| [TPL Beginner's](https://www.theperfectloaf.com/beginners-sourdough-bread/) | **78°F (25°C)** DDT, ambient 74–76°F | **~4 hours** |
| [TPL bulk fermentation guide](https://www.theperfectloaf.com/guides/the-ultimate-guide-to-bread-dough-bulk-fermentation/) | 74–78°F (23–25°C) | **2–5 hours** |
| [Tartine](https://feelingfoodish.com/tartine-bread/) | 78–82°F (25–28°C) | **3–4 hours** |
| [KAB Extra-Tangy](https://www.kingarthurbaking.com/recipes/extra-tangy-sourdough-bread-recipe) | not specified | "**5 hours or longer**" until doubled |

TPL's guide states the governing relationship directly:

> "Typically, I try to keep my dough around 74 to 76°F (23 to 24°C), which is an effective
> temperature for fermentation and results in bulk fermentation times between 2 and 5 hours."
> … "Warmer temperatures mean faster fermentation and a shorter bulk fermentation, whereas
> cooler temperatures mean slower fermentation and a longer bulk fermentation."

Practical rule of thumb used across the sourdough literature: **roughly ±1 hour of bulk per
±2°F (1°C)** away from the 76°F reference, within the 70–82°F band. Outside that band the
relationship stops being usable — below ~68°F the dough may never finish in a session, above
~84°F the dough degrades before it is properly aerated.

**End-of-bulk cues** — this is the definitive feel-judged step of the whole process:

- TPL Beginner's: "risen by **20% to 50%**. It should show some bubbles on the top and sides,
  and the edge of the dough where it meets the bowl should be **slightly domed**."
- TPL guide: dough is "smooth, elastic, with defined edges"; it "should be **puffy and jiggle
  when shaken gently**" with "**rounded edges** where it touches the container sides."
- Tartine: **20–30% rise**, dough feels airy and billowy, holds its shape when tipped out.
- KAB: "gently shake the bowl and it'll **jiggle** … the top of the dough should be stretched
  somewhat **taut** … soft and **pillowy** — if you press a finger into it, an **indent should
  remain**." KAB adds the dough "will often, but not always, **double** in size."

> **Disagreement worth carrying into the app:** percentage rise at end of bulk. TPL says
> 20–50%, Tartine says 20–30%, KAB says often doubled (100%). This is not noise — TPL's guide
> explicitly warns that "judging when to end bulk fermentation based on how much the dough
> rises (volumetric increase) **can be misleading**," because a white-flour dough may double
> while a whole-wheat high-hydration dough is equally ready at 20–30%. **Do not average these.**
> The app should treat percent-rise as a *hint scoped to the flour blend* and put the tactile
> cues (jiggle, dome, taut skin, retained indent) as the primary signal.

### 1.4 Fold schedule

Folds are the clearest example of a **genuinely timed** step — the interval is a clock interval,
and the sources agree closely.

| Source | Sets | Interval | Start |
| --- | --- | --- | --- |
| TPL Beginner's | **3 sets** | every **30 min** | 30 min after mix |
| TPL Simple Focaccia (for contrast) | 4 sets | every 30 min | 30 min after mix |
| Tartine | **3–4 turns** | every **30 min** | during first 2 h of bulk |

Every source front-loads the folds into the **first half of bulk** and leaves the dough
undisturbed for the remainder, so the gas built late is not knocked out. Tartine is explicit
that the last folds should be gentler than the first.

A set is **four folds** — north, south, east, west — per
[TPL's guide](https://www.theperfectloaf.com/guides/the-ultimate-guide-to-bread-dough-bulk-fermentation/):
"Typically, each set includes four stretches and folds."

The *number of sets* has a feel component even though the *interval* is timed: sources agree
you stop folding once the dough holds a smooth, cohesive mass and passes a windowpane-ish
stretch without tearing. A stiffer or lower-hydration dough needs fewer sets.

### 1.5 Shaping

A three-stage sequence, agreed on across sources:

1. **Pre-shape** — turn out, gently round into a loose boule with a bench knife. No degassing.
2. **Bench rest** — **20–30 min** (TPL: 25 min; Tartine: 20–30 min). Timed. Its purpose is
   gluten relaxation; the feel cue is that the round has spread slightly and no longer springs
   back hard.
3. **Final shape** — boule or bâtard, tightened against the bench to build surface tension,
   then into a banneton **seam-side up**.

The feel cue for a correctly shaped loaf is **surface tension**: the skin should be taut and
smooth, and the loaf should hold a domed profile on the bench for a few seconds rather than
flowing outward immediately.

### 1.6 Proof: room temperature vs cold retard

Two viable paths, and sources differ on which is canonical:

**Cold retard (preferred by TPL and Tartine):**
- TPL: 20 min at room temperature to set the shape, then **16 hours at 38°F (3°C)**.
- Tartine: **up to 12 hours** in the refrigerator, described as the preferred method.
- Common window across sources: **8–18 hours at 37–40°F (3–4°C)**.
- Bake **straight from the fridge** — the cold dough scores more cleanly and holds shape.

**Room-temperature proof:**
- Tartine alternative: **3–4 hours at 75–80°F**.
- KAB Extra-Tangy: shaped loaves rise "**2 to 4 hours**", until "**very puffy**".

**Proof doneness cue — the poke test** (feel-judged, agreed across all sources): press a
floured finger ~1 cm into the dough.
- Springs back immediately and fully → **under-proofed**, more time needed.
- Springs back **slowly and partially**, leaving a shallow dent → **ready**.
- Does not spring back at all, dent stays fully → **over-proofed**.

KAB phrases it as "if the indentation remains, it's ready to go. If the dough rebounds and your
finger mark disappears, it needs more time."

Note the cold retard changes the poke test: cold dough is stiff and always springs back more
than warm dough, so the test is applied *before* chilling, or read leniently after.

### 1.7 Bake: oven temperature, steam, time, doneness

| Source | Preheat | Bake temp | Covered | Uncovered |
| --- | --- | --- | --- | --- |
| [TPL](https://www.theperfectloaf.com/beginners-sourdough-bread/) | 450°F (232°C) | **450°F** throughout | **20 min** | **30 min** |
| [Tartine](https://feelingfoodish.com/tartine-bread/) | **500°F (260°C)**, 45 min with vessel inside | drop to **450°F** | **20 min** | **20–30 min** |
| [KAB Extra-Tangy](https://www.kingarthurbaking.com/recipes/extra-tangy-sourdough-bread-recipe) | — | **425–475°F** depending on method | — | 25–30 min |

**Steam.** Every source achieves steam the same way for a home oven: a **preheated covered
vessel** (Dutch oven, Lodge combo cooker, or Challenger pan) traps the loaf's own moisture for
the first phase. The lid comes off at the point where oven spring has finished and crust
colouring must begin. Alternatives cited when a vessel isn't available: a preheated cast-iron
skillet on the lower rack with boiling water poured in at load, or a covered roasting pan.

> **Disagreement worth carrying into the app:** preheat and bake temperature. Tartine preheats
> to 500°F and drops to 450°F; TPL holds a flat 450°F; KAB spans 425–475°F by method. The
> spread is real and reflects different vessels and loaf sizes, not a single correct number.
> The covered/uncovered *split* (~20 min covered, ~20–30 min uncovered) is the part all three
> agree on.

**Doneness cues** (feel/observation-judged, with one measurable):

- **Internal temperature: 205–210°F (96–99°C).** TPL specifies "around **208°F (97°C)**"; KAB
  adaptations cite ~206°F. This is the one hard, instrument-measurable doneness signal.
- **Colour:** "deep mahogany" (TPL), "chestnut brown" (Tartine), "very deep golden brown" (KAB).
  Note that all three call for a *darker* crust than most home bakers instinctively go to.
- **Sound:** hollow when tapped on the base; TPL adds the crust should "**crackle/crunch**"
  audibly as it cools (the "singing" loaf).
- **Cooling is part of doneness:** minimum **1–2 hours** on a wire rack before slicing. The
  interior is still setting; cutting early gives a gummy crumb. This is a timed step.

---

## 2. Canonical focaccia

### 2.1 Flour, hydration and oil

### 2.2 Leavening: sourdough levain vs commercial yeast

### 2.3 Bulk fermentation and folds

### 2.4 Panning instead of shaping: pan, oil, stretching

### 2.5 Pan proof: room temperature vs cold retard

### 2.6 Dimpling

### 2.7 Bake: oven temperature, time, doneness

---

## 3. Timed vs feel-judged: step classification

---

## 4. Shared vs divergent structure

---

## 5. Where sources disagree

---

## 6. Source list
