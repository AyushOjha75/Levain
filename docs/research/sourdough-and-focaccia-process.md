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

The reference recipes used throughout this section:

- [The Perfect Loaf — A Simple Sourdough Focaccia](https://www.theperfectloaf.com/a-simple-focaccia/) (TPL)
- [King Arthur Baking — Big and Bubbly Focaccia](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-recipe),
  their 2025 Recipe of the Year — the **yeasted** reference (KAB-Y)
- [King Arthur Baking — Big and Bubbly Focaccia, Sourdough Edition](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-sourdough-edition-recipe) —
  the same formula with ripe starter substituted for commercial yeast (KAB-SD). **This pair is
  the single most useful source in this document**, because it isolates exactly what changes
  between the yeasted and sourdough routes with everything else held constant.
- [Serious Eats — Easy No-Knead Olive-Rosemary Focaccia](https://www.seriouseats.com/easy-no-knead-olive-rosemary-focaccia-with-pistachios-recipe) (SE)
- [ThermoWorks — Homemade Focaccia: Recipe, Temperatures, and Tips](https://blog.thermoworks.com/homemade-focaccia/) for doneness thermometry

### 2.1 Flour, hydration and oil

| Source | Flour | Hydration | Oil in dough | Salt |
| --- | --- | --- | --- | --- |
| [TPL](https://www.theperfectloaf.com/a-simple-focaccia/) | 30% high-protein (~13%), 70% AP (11–12%) | **76%** (78% counting oil) | **2%** | 1.8% |
| [KAB-Y](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-recipe) | 100% all-purpose (360 g) | **~79%** (284 g water) | **5%** (18 g) | 2.5% (9 g) + 5 g sugar |
| [SE](https://www.seriouseats.com/easy-no-knead-olive-rosemary-focaccia-with-pistachios-recipe) | AP or bread flour (500 g) | **65%** (325 g water) | ~¼ cup, mostly pan/top | 3% (15 g kosher) |
| [Modernist Pantry, Classic Focaccia](https://blog.modernistpantry.com/recipes/classic-focaccia/) | high-protein white | "high hydration" | present | — |

> **Disagreement worth carrying into the app:** focaccia hydration. TPL and KAB sit at 76–79%;
> Serious Eats' no-knead version is a clear outlier at **65%**, and secondary sources push as
> high as **80–88%**. This is not a measurement discrepancy — it is a different design target.
> The low-hydration route trades open crumb for a dough that can be handled and needs no folds
> at all; the high-hydration route buys the big irregular bubbles focaccia is prized for but
> requires folds and a pan to contain it. **Report both routes; don't average to ~72%.**

Note KAB is the only reference here that adds **sugar** (5 g, ~1.4%), for browning. TPL and SE
do not. Focaccia is otherwise a **lean dough** — oil is layered onto the surface and the pan
more than into the crumb, which is why [ThermoWorks](https://blog.thermoworks.com/homemade-focaccia/)
still classes it as lean for doneness purposes.

Focaccia hydration runs **higher than the same baker's loaf hydration** (TPL: 76% focaccia vs
72% loaf). The pan carries the dough, so it never has to hold its own shape.

### 2.2 Leavening: sourdough levain vs commercial yeast

This is where focaccia's dual identity lives, and the KAB pair documents it precisely with
everything else held constant:

| | KAB-Y (yeasted) | KAB-SD (sourdough) |
| --- | --- | --- |
| Leaven | **3 g instant yeast** (~0.8%) | **ripe sourdough starter**, no commercial yeast |
| Levain build step | **none** — yeast goes straight into the mix | **required** — starter must be fed and ripened first |
| First rise | **1 hour** at 70–75°F | folded into a longer, starter-paced schedule |
| Pan rise | **2–3 hours** | **4–6 hours** |
| Total time | ~3 h 40 min | substantially longer |

KAB-SD notes the pan rise runs "anywhere from **4 to 6 hours**" and that the range depends on
"the ripeness of your starter and the warmth of your kitchen" — i.e. the sourdough route is
*more* feel-dependent and *less* clock-predictable than the yeasted one, by the source's own
admission.

TPL's sourdough focaccia uses **levain at 19% of total dough weight** from a 100%-hydration
starter, with the ripeness cue being simply that the starter is at the point "you'd normally
give it a refreshment."

**Structural consequence for the app:** the levain build is an **optional leading step** in the
focaccia sequence. Everything downstream of the mix is identical in *kind* between the two
routes; only the *durations* stretch. This is the cleanest argument that leavening should be a
property that scales step durations, not a separate recipe.

### 2.3 Bulk fermentation and folds

| Source | Bulk duration | Temp | Folds |
| --- | --- | --- | --- |
| [TPL](https://www.theperfectloaf.com/a-simple-focaccia/) | **2 hours** | 76–78°F (24–25°C) | **4 sets**, every 30 min, first at 30 min |
| [KAB-Y](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-recipe) | **1 hour** | 70–75°F warm room temp | **4 bowl folds** at the end of the hour |
| [SE](https://www.seriouseats.com/easy-no-knead-olive-rosemary-focaccia-with-pistachios-recipe) | **overnight** at room temp | ambient | **none** — explicitly no-knead, no-stretch |

TPL's mixing is machine-forward (stand mixer, speed 1 for 1–2 min, speed 2 for 5 min, 10 min
rest, then oil worked in on speed 2 for 1–2 min) with an FDT of **76°F (24°C)**, so the folds
are structure-building on top of an already-developed dough. KAB and SE build all structure via
time and folds alone.

KAB's end-of-bulk cue is explicit and observable: after 1 hour "the dough should have **nearly
doubled** in size and will be **very puffy**."

Focaccia bulk is **markedly shorter than loaf bulk** (1–2 h vs 3–5 h) in every source that
publishes both, because focaccia carries a second long fermentation in the pan. The total
fermentation is comparable; it is split differently.

### 2.4 Panning instead of shaping: pan, oil, stretching

Focaccia has **no pre-shape, no bench rest, no final shape, no banneton, no scoring**. The
whole shaping block of the sourdough sequence collapses into one step: **transfer to an oiled
pan and coax to the edges.**

| Source | Pan | Oil in pan |
| --- | --- | --- |
| TPL | 9" × 13" rectangular, or two 10" × 2.25" rounds | "liberally oil interior" |
| KAB | **9" square**, sprayed with nonstick spray **then** 1 Tbsp olive oil, pan tilted to spread evenly | 1 Tbsp in pan, 1 Tbsp on top (26 g total, divided) |
| Generic high-hydration | half sheet pan (18" × 13") or cast iron | ~¼ cup, spread edge to edge |

The oil is doing two jobs and the sources are consistent on both: it prevents sticking, and it
**shallow-fries the base** during the bake, which is where focaccia's crisp bottom comes from.
This is why the quantity is much larger than a normal pan-greasing and why it must reach the
corners.

**Stretching to the pan is a repeated, gentle, feel-judged operation, not a single action.**
TPL: "Every 30 minutes for the first hour, uncover the pan and gently stretch the dough with
wet hands to the pan's edges." A high-hydration dough will retract; you stretch, it relaxes,
you stretch again. The cue to stop is simply that the dough **stays** at the edges rather than
pulling back.

Wet hands (TPL) or oiled hands (KAB) — both work; the point is preventing stick.

### 2.5 Pan proof: room temperature vs cold retard

| Route | Duration | Cue |
| --- | --- | --- |
| [KAB-Y](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-recipe) room temp | **2–3 hours** | "until it **nearly reaches the corners** and is **very close to the top edge**" |
| [KAB-SD](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-sourdough-edition-recipe) room temp | **4–6 hours** | "until it's **marshmallowy and jiggly**; the dough should nearly fill the corners of the pan and be very close to the top edge" |
| [TPL](https://www.theperfectloaf.com/a-simple-focaccia/) room temp | **4 hours** at 76–78°F | puffy, filled to the pan edges, bubbles visible under the surface |
| KAB cold retard | **8–24 hours** in the fridge, taken *before* panning (after the bowl folds) | — |
| TPL cold retard | after 2 h in the pan, cover airtight and refrigerate overnight | must **come back to room temperature before baking** |

> **Disagreement worth carrying into the app:** *where* the cold retard sits in the sequence.
> KAB retards **in the bowl, before panning** (8–24 h). TPL retards **in the pan, mid-proof**
> (2 h in pan, then overnight). Both are legitimate and they produce different schedules — the
> KAB route lets you pan and bake in a 3-hour window the next day; the TPL route lets you go
> essentially straight from fridge to oven. This is a real fork in the step graph, not a detail.

Note also the asymmetry with the sourdough loaf: the **loaf bakes straight from the fridge**,
but TPL's retarded **focaccia must warm up first**. A cold focaccia will not spring, and the
pan insulates the base. Do not generalise "bake cold" across the two breads.

"Marshmallowy and jiggly" (KAB-SD) is the single best-phrased feel cue found in this research
and should be used near-verbatim.

### 2.6 Dimpling

Dimpling replaces scoring. It happens **after** the pan proof, immediately before the bake, and
it is a feel-judged operation with a hard constraint attached.

- **Depth:** all the way down. TPL: dimples "go **all the way down to the bottom of the pan**."
  KAB: "press your fingertips into the dough **until they reach the bottom of the pan**."
- **Spacing:** KAB specifies dimples "spaced about **1½" apart**"; TPL says "evenly spaced."
- **Hands:** oiled (KAB) or wet (TPL).
- **The constraint, and the cue:** KAB — "The goal is to thoroughly dimple the dough **without
  deflating it** — aim for **decisive yet gentle** motions." The observable failure mode is the
  dough sighing flat and not recovering; a correctly dimpled slab keeps its puffed profile
  between the dimples.

Dimpling is also what creates the pockets that hold the surface oil, so the two steps are
paired: dimple, then **drizzle 1–2 Tbsp olive oil over the surface** — KAB notes "it's OK if it
pools in some dimples" — then flaky/coarse sea salt, then herbs or toppings pressed **gently**
into the dough.

### 2.7 Bake: oven temperature, time, doneness

| Source | Oven temp | Rack | Bake time |
| --- | --- | --- | --- |
| [KAB-Y](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-recipe) | **475°F (246°C)** | lower third | **15–18 min** |
| [TPL](https://www.theperfectloaf.com/a-simple-focaccia/) | **450°F (232°C)** | bottom third | **~30 min**, rotate front-to-back halfway |
| [ThermoWorks](https://blog.thermoworks.com/homemade-focaccia/) | **450°F (232°C)** | — | **20–30 min**, by thickness |
| [Modernist Pantry](https://blog.modernistpantry.com/recipes/classic-focaccia/) | **425°F (218°C)** | — | — |

> **Disagreement worth carrying into the app:** focaccia bake time spans **15 to 30 minutes**
> and temperature **425 to 475°F** — a wider spread than for the loaf. The driver is **dough
> depth**, not source preference: KAB's is a 9" square (small, hot, fast), TPL's is a 9"×13"
> (thicker slab, cooler, slower). Bake time for focaccia is therefore **pan-dependent** and
> must not be presented as a fixed number. Every source agrees on the **lower rack**, which is
> what crisps the oiled base.

**No steam.** This is a categorical divergence from the loaf. Focaccia is baked in a dry oven —
no Dutch oven, no lid, no water pan. Steam would soften the crust that the oil and the hot
lower rack are working to crisp.

**Doneness cues:**

- **Colour (primary, all sources):** KAB — "**brown in the highest spots and golden in the
  crevices**." TPL — "**deeply coloured on top**." The two-tone description is the useful one:
  the raised areas between dimples brown first.
- **Internal temperature: 190–210°F (88–99°C)**, per
  [ThermoWorks](https://blog.thermoworks.com/homemade-focaccia/) — a slightly lower and much
  wider window than the loaf's 205–210°F.
- **What explicitly does *not* work:** ThermoWorks warns against judging focaccia "by colour
  alone" or by thumping the bottom, because the oiled surface browns before the interior is
  set — the failure mode is a doughy centre under a convincing crust. **The hollow-tap test,
  valid for the loaf, is invalid for focaccia.**
- **Base check:** lift a corner with a spatula — the bottom should be uniformly golden and
  crisp, not pale or oil-logged.
- TPL adds an active-monitoring instruction rather than a fixed time: "Keep an eye on it during
  the last 5 minutes and pull it out if it's colouring too quickly."
- **Cooling:** focaccia is served warm and needs only a short rest; it does not require the
  loaf's 1–2 hour cool-before-slicing. Lift it out of the pan onto a rack promptly so the base
  doesn't steam itself soft.

---

## 3. Timed vs feel-judged: step classification

Classified against the existing `StepKind` in [`CONTEXT.md`](../../CONTEXT.md) and
[`Entities.kt`](../../app/src/main/java/com/ayushojha/levain/data/Entities.kt) — **TIMED**
(real duration, real timer), **JUDGED** (baker decides, guided by an advisory estimate *and* an
observable cue), **ACTION** (done the moment you do it, no duration).

The research supports this three-way split cleanly. It also surfaces one boundary case
(§3.3) worth deciding deliberately.

### 3.1 Sourdough loaf

| # | Step | Kind | Estimate | Observable cue (JUDGED only) |
| --- | --- | --- | --- | --- |
| 1 | Feed starter to build levain | ACTION | — | — |
| 2 | Levain ripens | **JUDGED** | 5–6 h @ 74–76°F (1:2:2 build); 8–10 h (dilute build) | **Domed and bubbly on top, at the sides and through the middle; smells sweetly sour, not sharp; just at or just past its peak.** Confirm with the float test if unsure. |
| 3 | Mix flour and water (autolyse) | ACTION | — | — |
| 4 | Autolyse rest | **TIMED** | 40 min – 1 h | — |
| 5 | Add levain and salt, mix | ACTION | — | — |
| 6 | Check dough temperature | ACTION | target 76–78°F (24–25°C) | — |
| 7 | Rest before first fold | **TIMED** | 30 min | — |
| 8 | Fold, set 1 of 3 | ACTION | — | — |
| 9 | Rest between folds (×2) | **TIMED** | 30 min each | — |
| 10 | Folds, sets 2 and 3 | ACTION | — | — |
| 11 | Bulk fermentation completes | **JUDGED** | 3–5 h total @ 74–78°F | **Risen by a quarter to a half (more for white flour, less for whole grain). Puffy — it jiggles as one mass when you shake the bowl. The top skin is taut and slightly domed, and the edge where the dough meets the bowl curves up rather than sagging. Bubbles visible on top and through the sides.** |
| 12 | Turn out and pre-shape | ACTION | — | — |
| 13 | Bench rest | **TIMED** | 20–30 min | — |
| 14 | Final shape into banneton | ACTION | — | — |
| 15 | Rest at room temperature | **TIMED** | 20 min | — |
| 16 | Cold retard in fridge | **TIMED** | 12–18 h @ 38°F (3°C) | — |
| 16a | *(alt)* Room-temperature proof | **JUDGED** | 2–4 h @ 75–80°F | **Poke test: a floured fingertip pressed ~1 cm in springs back slowly and partially, leaving a shallow dent. Springs back fast and fully = not yet. Doesn't spring back at all = gone too far.** Loaf looks puffed and holds a dome. |
| 17 | Preheat oven and Dutch oven | **TIMED** | 45 min–1 h @ 450–500°F | — |
| 18 | Score the loaf | ACTION | — | — |
| 19 | Bake covered | **TIMED** | 20 min | — |
| 20 | Remove lid | ACTION | — | — |
| 21 | Bake uncovered | **TIMED** | 20–30 min | — |
| 22 | Confirm doneness | **JUDGED** | — | **Crust is deep mahogany/chestnut, darker than feels comfortable. Hollow when tapped on the base. Internal temperature 205–210°F (96–99°C).** |
| 23 | Cool on a rack | **TIMED** | 1–2 h | — |

### 3.2 Focaccia

| # | Step | Kind | Estimate | Observable cue (JUDGED only) |
| --- | --- | --- | --- | --- |
| 1 | *(sourdough route only)* Feed starter | ACTION | — | — |
| 2 | *(sourdough route only)* Levain ripens | **JUDGED** | 5–6 h | **As §3.1 step 2** — domed, bubbly, sweetly sour. |
| 3 | Mix flour, water, salt, leaven | ACTION | — | — |
| 4 | Rest, then work in olive oil | **TIMED** | 10 min | — |
| 5 | Check dough temperature | ACTION | target 76°F (24°C) | — |
| 6 | Rest before first fold | **TIMED** | 30 min | — |
| 7 | Folds ×4 with rests | ACTION / **TIMED** | 30 min between sets | — |
| 8 | Bulk fermentation completes | **JUDGED** | 1–2 h @ 70–78°F | **Nearly doubled and very puffy; the surface is domed and shows bubbles.** (Shorter than the loaf — the pan proof carries the rest.) |
| 8a | *(alt)* Cold retard in the bowl | **TIMED** | 8–24 h in the fridge | — |
| 9 | Oil the pan | ACTION | — | — |
| 10 | Transfer dough to pan | ACTION | — | — |
| 11 | Stretch toward the pan edges (repeat ~3×) | **JUDGED** | every 30 min for the first hour | **The dough stops retracting — it stays where you put it instead of pulling back from the corners within a minute.** |
| 12 | Pan proof | **JUDGED** | 2–3 h yeasted; 4–6 h sourdough | **Marshmallowy and jiggly. It has nearly filled the corners and sits very close to the top edge of the pan. Large bubbles visible under the surface.** |
| 12a | *(alt)* Cold retard in the pan | **TIMED** | overnight, after ~2 h in the pan | — |
| 12b | *(if retarded)* Come to room temperature | **JUDGED** | 1–2 h | **The pan is no longer cold to the touch and the dough has loosened and re-puffed.** Focaccia does *not* bake from cold. |
| 13 | Preheat oven | **TIMED** | 20–30 min @ 425–475°F | — |
| 14 | Dimple | **JUDGED** | — | **Fingertips reach the bottom of the pan, dimples ~1½" apart, and the dough between them stays puffed. If it sighs flat and doesn't recover, you were too heavy.** |
| 15 | Drizzle oil, salt, add toppings | ACTION | — | — |
| 16 | Bake | **TIMED** | 15–18 min (9" square @ 475°F); 25–30 min (9"×13" @ 450°F) | — |
| 17 | Confirm doneness | **JUDGED** | — | **Brown on the raised spots, golden in the crevices. The base, lifted with a spatula, is evenly golden and crisp — not pale, not oil-logged. Internal temperature 190–210°F (88–99°C). Do not tap for hollowness — it doesn't work on focaccia.** |
| 18 | Lift from pan onto a rack | ACTION | — | — |

### 3.3 The boundary case: folds

A fold set is arguably **two** steps and the app has to pick a representation:

- The **waiting interval** between sets is unambiguously TIMED — 30 minutes, on a clock, and
  every source agrees. This is the one place a timer is genuinely authoritative.
- The **act of folding** is an ACTION — instantaneous, ticked off.
- But the **number of sets** has a judged component: sources give 3–4, and the real stopping
  condition is that the dough has come together smooth and cohesive and resists further
  stretching. A stiffer dough needs fewer.

The `CONTEXT.md` design of expanding repeats at snapshot ("four folds, thirty minutes apart"
becomes four Steps) handles this correctly, **provided the last fold or two is skippable
without breaking the projection**. Recommendation: model each set as a TIMED rest followed by
an ACTION fold, and let the baker complete the bulk JUDGED step early — which naturally
abandons remaining fold steps.

### 3.4 The general rule the sources support

**A step is TIMED when its duration is set by physics or by convention rather than by the
dough's state**: rests, preheats, bake phases, cooling, cold retards (the fridge is cold enough
that an extra two hours changes little). **A step is JUDGED when biology sets the duration**:
anything where yeast and bacteria decide — levain ripeness, bulk, proof, pan proof.

Two consequences worth noting:

1. **Every JUDGED step is a fermentation step, and every fermentation step is JUDGED.** The
   only exceptions are the cold retards, which are TIMED precisely because refrigeration
   flattens the rate curve.
2. **Doneness at the oven is JUDGED even though it has a number.** Internal temperature is
   measurable, but the primary cues are colour and crust, the temperature window is wide
   (205–210°F loaf; 190–210°F focaccia), and not every baker owns a probe. It needs the cue
   text, not just a threshold.

---

## 4. Shared vs divergent structure

### 4.1 The shared spine

Reduced to essentials, both breads run the same seven-phase spine:

```
LEAVEN  →  MIX  →  BULK (+ folds)  →  PRE-BAKE FORM  →  PROOF  →  BAKE  →  REST
```

Every phase is present in both. Nothing in focaccia is absent from the loaf's sequence — the
phases are **occupied differently**, not removed. That is the key structural finding, and it
means a single Step model covers both without optional phases or null-shaped holes.

Phase-by-phase, what is genuinely shared:

| Phase | Shared across both breads |
| --- | --- |
| Leaven | A ripeness JUDGED step with the same cue (domed, bubbly, sour aroma) — *when a starter is used* |
| Mix | Combine flour/water/salt/leaven; a dough-temperature target; an ACTION |
| Bulk | A JUDGED fermentation with a TIMED fold schedule inside it, folds front-loaded, 30-min intervals, ~4 folds per set |
| Pre-bake form | A single transition from bulk vessel to bake vessel |
| Proof | A JUDGED rise with a room-temp path and a cold-retard alternative |
| Bake | Preheat (TIMED), bake (TIMED), doneness (JUDGED with colour + internal temp) |
| Rest | Cooling before eating (TIMED) |

### 4.2 Where they diverge

| Phase | Sourdough loaf | Focaccia |
| --- | --- | --- |
| **Leaven** | Always a starter; levain build is mandatory | **Starter *or* commercial yeast.** Yeasted route deletes the levain build entirely — yeast goes straight into the mix |
| **Hydration** | 65–80%, must hold its own shape | 65–88%, typically higher; the pan holds the shape |
| **Fat** | None (lean) | Olive oil in the dough (2–5%), in the pan, and on top — the pan oil is a *cooking medium*, not a greasing |
| **Bulk length** | Long: 3–5 h, the dominant fermentation | Short: 1–2 h — total fermentation is redistributed into the pan proof |
| **Pre-bake form** | **Three steps**: pre-shape → TIMED bench rest → final shape into a banneton. Builds surface tension | **One step**: transfer to an oiled pan, then repeated gentle stretching to the edges. Builds no tension at all |
| **Proof vessel** | Banneton, seam up | The bake pan itself — proof and bake happen in the same vessel |
| **Proof length** | 12–18 h cold, or 2–4 h warm | 2–3 h (yeasted) / 4–6 h (sourdough) warm; retard optional |
| **Retard position** | After shaping, before bake — **bake straight from the fridge** | Either *before* panning (KAB) or *mid-pan-proof* (TPL) — and **must warm up before baking** |
| **Surface work** | **Score** with a lame, immediately before loading | **Dimple** with fingers to the pan floor, then oil, salt, toppings |
| **Steam** | **Essential** — covered Dutch oven, 20 min, lid off for colour | **None** — dry oven, lower rack; steam would ruin the crisp base |
| **Bake time** | 40–50 min in two phases (covered/uncovered) | 15–30 min in one phase, driven by pan depth |
| **Doneness** | 205–210°F; hollow tap **valid** | 190–210°F; hollow tap **invalid**; two-tone colour + crisp base |
| **Rest** | 1–2 h mandatory, or the crumb is gummy | Minutes; served warm |

### 4.3 Yeasted focaccia specifically

The [KAB yeasted / sourdough pair](https://www.kingarthurbaking.com/recipes/big-and-bubbly-focaccia-sourdough-edition-recipe)
isolates the difference precisely, since everything but the leaven is held constant:

1. **One step disappears.** The levain build (and its JUDGED ripeness cue) is absent. Instant
   yeast is an ingredient, not a preparation.
2. **Every downstream duration shortens.** First rise 1 h vs a starter-paced schedule; pan
   proof **2–3 h vs 4–6 h**.
3. **Nothing else changes.** Same flour, same hydration, same folds, same pan, same oil, same
   dimpling, same bake, same cues. KAB even keeps the wording of the pan-proof cue identical.
4. **The cues get *more* reliable, not different.** KAB explicitly attributes the sourdough
   version's wider 4–6 h window to "the ripeness of your starter and the warmth of your
   kitchen" — the sourdough route is less clock-predictable, which raises the value of the
   observable cue relative to the estimate.

### 4.4 What this means for the Step model

`CONTEXT.md` currently rules that "sourdough focaccia and yeasted focaccia are two Recipes, not
one Recipe with a switch." **The research is consistent with that ruling, and gives it a
concrete justification**: the yeasted route has a genuinely different step *count* (no levain
build) and different estimates throughout — not merely a different ingredient. Modelling it as
a switch would mean a Recipe whose step list changes shape, which is exactly what the "options
only where they don't change the step sequence" rule forbids.

What the shared Step model must express to cover both breads:

1. **The three kinds are sufficient.** Every step in both breads lands cleanly in TIMED,
   JUDGED, or ACTION. No fourth kind is needed.
2. **JUDGED steps need an estimate *and* a cue, and the estimate must be a range** — the cue
   text is doing the real work, and the ranges are wide (pan proof 2–6 h).
3. **Cues must be per-Recipe strings, not shared enums.** "Marshmallowy and jiggly, nearly
   filling the corners" and "risen by half, domed at the bowl edge, jiggles as one mass" are
   both "the proof is done" but share no vocabulary.
4. **Estimates must be temperature-anchored.** Bulk duration is a function of dough
   temperature (roughly ±1 h per ±2°F around 76°F). An estimate stated without its reference
   temperature is misleading. Either state the temperature alongside it or let the projection
   adjust.
5. **A JUDGED step completing early or late must reflow everything downstream** — this is the
   Projection, and it is load-bearing precisely because the two longest steps in both breads
   are JUDGED.
6. **Alternative branches are needed for retard vs room-temperature proof**, and the branches
   differ per bread: the loaf's retard replaces the proof and feeds straight into the bake; the
   focaccia's retard sits *inside* the proof and adds a warm-up step after it.
7. **Repeat expansion must tolerate early exit.** Folds expand to N steps, but the baker ending
   bulk early must not leave orphaned fold steps blocking the timeline.
8. **A Step must be able to reference a measurement** (dough temperature at mix, internal
   temperature at doneness) without becoming a timer.

---

## 5. Where sources disagree

Collected in one place. **None of these are averaged.**

| # | Question | The disagreement | Why it exists |
| --- | --- | --- | --- |
| 1 | **How much should dough rise during bulk?** | TPL: **20–50%**. Tartine: **20–30%**. KAB: "often, but not always, **double**." | TPL's guide argues volumetric rise is *inherently* misleading — white flour doubles while whole-wheat high-hydration is ready at 20–30%. The sources are describing different flour blends, so no single number is right. **Use tactile cues as primary.** |
| 2 | **Is the float test authoritative for levain?** | Tartine: yes, it's the test. KAB: fine for starter, **not reliable for dough**. TPL: prefers appearance and aroma. | The float test measures gas retention, which correlates with but doesn't equal readiness. Offer as secondary confirmation. |
| 3 | **Loaf bake temperature** | TPL: flat **450°F**. Tartine: preheat **500°F**, bake at **450°F**. KAB: **425–475°F** by method. | Different vessels and loaf sizes. The **20 min covered / 20–30 min uncovered split** is the part all three agree on — anchor on that, not the number. |
| 4 | **Focaccia hydration** | TPL **76%**, KAB **~79%**, Serious Eats **65%**, secondary sources **80–88%**. | Two different design targets, not a measurement spread. Low hydration = handleable, no folds, tighter crumb. High = open bubbles, needs folds and a pan. Both are valid focaccia. |
| 5 | **Focaccia bake temperature and time** | KAB **475°F / 15–18 min**. TPL **450°F / ~30 min**. ThermoWorks **450°F / 20–30 min**. Modernist Pantry **425°F**. | Driven by **pan size and dough depth**, not preference. A 9" square bakes hot and fast; a 9"×13" slab bakes cooler and longer. Bake time must be pan-dependent in the app. |
| 6 | **Where the focaccia cold retard sits** | KAB: **in the bowl, before panning**, 8–24 h. TPL: **in the pan, mid-proof**, after ~2 h. | A genuine fork in the step graph with different next-day schedules. KAB's route needs a 2–3 h pan window the next day; TPL's goes fridge → warm-up → oven. |
| 7 | **Does retarded dough bake cold?** | Loaf: **yes**, bake straight from the fridge (TPL, Tartine). Focaccia: **no**, must come to room temperature first (TPL). | Not a contradiction between sources but a difference between breads — worth flagging because the instinct to generalise is strong and the failure is silent. |
| 8 | **Loaf internal doneness temperature** | TPL **208°F**. KAB adaptations **~206°F**. General range **205–210°F**. | Minor, and within instrument error. Treat 205–210°F as one window. |

### Sources that could not be verified directly

[Serious Eats](https://www.seriouseats.com/) blocks automated fetching, so its parameters here
(500 g flour / 325 g water / 4 g instant yeast / 15 g salt, overnight room-temperature rest, no
kneading or stretching) were recovered from search-index excerpts rather than read from the
page. The figures are consistent across excerpts but should be confirmed against the live page
before being encoded into a bundled Recipe.

[Modernist Bread](https://modernistcuisine.com/books/modernist-bread/) is not available online;
the Modernist material cited here is from the publicly published
[Modernist Pantry](https://blog.modernistpantry.com/recipes/classic-focaccia/) recipe, which is
adjacent but not the book. Where this document cites "Modernist," read it as that lighter
source, and treat the book's numbers as still outstanding.

---

## 6. Source list
