# Stonecutter notes — Iron Grate rehearsal

Written for the Portal Glimpse merge. Everything below is either measured from
this repo or recovered from the plugin jar; where something is unverified it says so.

## Status

The yarn layer is **done and verified**. All five versions (1.21, 1.21.3, 1.21.4,
1.21.5, 1.21.6) build from one source tree with `gradlew build`, and each jar was
checked against the branch it replaces.

| | |
|---|---|
| Branch | `stonecutter`, based on `1.21.6` |
| Layer done | yarn: 1.21, 1.21.3, 1.21.4, 1.21.5, 1.21.6 |
| Layer not started | Mojang: 26.1, 26.3 |

### Verification results

Resources — every file in each reference branch is present in the built jar and
**byte-identical**, and `fabric.mod.json` carries the right `version` and
`depends.minecraft` for each:

| MC | reference branch | resources | fabric.mod.json |
|---|---|---|---|
| 1.21 | `master` | identical | `1.0.0`, `>=1.21 <=1.21.1` |
| 1.21.3 | `1.21.3` | identical | `1.3.0`, `1.21.3` |
| 1.21.4 | `1.21.4` | identical | `1.4.0`, `1.21.4` |
| 1.21.5 | `1.21.5` | identical | `1.5.0`, `1.21.5` |
| 1.21.6 | `1.21.6` | identical | `1.6.0`, `>=1.21.6 <=1.21.11` |

Source — Stonecutter output compared against each reference branch, with the inert
directive comments stripped:

| MC | result |
|---|---|
| 1.21.5 | exact match |
| 1.21.4 | one comment typo (`abiant` to `ambiant`, fixed upstream in 1.21.5) |
| 1.21.3 | same typo, plus the reference carries a dead `WorldAccess` import that the generated source correctly omits |
| 1.21 | same typo, plus `WorldAccess` import ordering |
| 1.21.6 | active version — see gotcha 10; proven bytecode-identical to its branch separately |

All remaining differences are comments, import order, or a dead import. Nothing
behavioural.

## Version pin

Pinned **0.9.8**, not latest. Latest is **0.10**, published 2026-11-08 — the same
day as this write-up, so it has zero time in the wild and this is a de-risking
rehearsal. Revisit once 0.10 has some mileage.

- Coordinates: `dev.kikugie.stonecutter`, plugin marker `dev.kikugie.stonecutter.gradle.plugin`
- **Repo: `https://maven.kikugie.dev/releases`.** The marker pom resolves on the
  Gradle plugin portal, but the real `dev.kikugie:stonecutter` jar 303s there —
  you must add the kikugie maven to `pluginManagement.repositories`.
- Published versions end: `... 0.9.5, 0.9.6, 0.9.7, 0.9.8, 0.10`.
- **Sort trap:** `0.10` is newer than `0.9.8` numerically but *lower* as a string.
  Any script picking "latest" by string sort gets this wrong.

## The docs are broken — read this before wasting an hour

`stonecutter.kikugie.dev` currently serves **Bee Movie placeholder text** on
`/stonecutter/setup` and `/stonecutter/guide/setup` (HTTP 200, wrong body). The
GitHub README raw copy has the directive syntax but no setup instructions, and
the GitHub HTML page timed out repeatedly.

The DSL below was recovered by running `javap` over `stonecutter-0.9.8.jar`. Treat
it as authoritative for 0.9.8 signatures and as *unverified* for runtime semantics.

### Recovered DSL (0.9.8)

`settings.gradle` extension (`StonecutterSettingsExtension`):
- `create(Object, Action<TreeBuilder>)` — also `Object[]`/`Iterable` overloads, and `File`-based `load`
- `shared(Action<TreeBuilder>)`
- properties: `centralScript`, `kotlinController`

`TreeBuilder` (extends `BranchBuilder`):
- property `vcsVersion`
- **`branch(String)` / `branch(String, Action<BranchBuilder>)`**
- `mapBuilds(...)`, `inherit()`, `load(File)`

`BranchBuilder`:
- `versions(String...)`, `versions(Iterable)`, `versions(Map<String,String>)`
- `vers(String)` / `vers(String, String)`, `version(...)`, `versionPairs(...)`
- property **`branchScript`** — per-branch build script

Controller (root project): `active(Object)`, `parameters(Action)`, `tree`, `flags`,
`handlers`, `tasks`

Per-version build extension: `swaps`, `constants`, `dependencies`, `replacements`,
`filters`, `process(File, String)`

Version predicates: `eval(String version, String... predicates)`

Two things that bit me:
- **There is no `chiseledBuild` task in 0.9.8.** No string matching `chisel`
  appears anywhere in the jar. That name is from older releases. Aggregation now
  goes through `stonecutter.tasks.named(name, filter)`, which returns a
  node-to-TaskProvider map. In practice you do not need it for a plain build:
  plain `gradlew build` from the root builds every version, because each version
  is an ordinary Gradle subproject.
- **Default file handlers are `java, scala, groovy, gradle, json5`** (plus kotlin,
  shader, hash groups). Plain `.json` is *not* handled. See gotcha 4.

### Confirmed project layout

Verified by running it, not guessed:

```
settings.gradle                      stonecutter { create(rootProject) { versions ... } }
stonecutter.gradle                   the CONTROLLER - stonecutter.active '1.21.6'
build.gradle                         the SHARED PER-VERSION script (centralScript default)
gradle.properties                    shared keys only (loom_version, group, archive name)
versions/<ver>/gradle.properties     per-version keys - these DO resolve
overlays/<name>/...                  resource overlays for unpreprocessable changes
src/                                 the one shared source tree
```

The root project uses `stonecutter.gradle` as its build script, which frees the
root `build.gradle` to be the script every `versions/*` subproject runs. The
`versions/<ver>` directories otherwise stay empty — adding a `gradle.properties`
is also what makes git track them.

`gradlew build` from the root builds every version. Per-version:
`gradlew :1.21.3:build`. Jars land in `versions/<ver>/build/libs/`.

### Directive syntax (verbatim from the README)

```java
//? if >1.2.0
func()

//? if >1.2.0 {
func1()
func2()
//?}

//? if >1.2.0 || <0.3.0
func()

//? if id: >1.2.0
func()
```

Else form, and an inline form:

```java
//? >=1.12 {
category.addDetail(name, callable::call);
//?} else
/*category.setDetail(name, callable::call);*/
```

```java
target.call(arg1, arg2, /*? if dependency: >=0.2 >>*//*arg3,*/ arg4)
```

I deliberately used **two independent condition blocks** instead of `else` in the
committed source, because the multi-line `else` form is not documented anywhere I
could reach and I could not run the plugin to test it. Once verified, the `else`
form is tidier.

## Iron Grate actual deltas (measured, not estimated)

Java, across the yarn layer:

| Break | File | Boundary |
|---|---|---|
| `Settings.registryKey(...)` required on block + item | `ModBlocks.java` | `>=1.21.3` |
| `getStateForNeighborUpdate` signature; `WorldAccess` to `WorldView` + `ScheduledTickView` + `Random` | `IronGrateBlock.java` | `>=1.21.3` |
| `BlockRenderLayerMap` moved package, became static, `RenderLayer.getCutout()` to `BlockRenderLayer.CUTOUT` | `IronGrateClient.java` | `>=1.21.6` |

So the whole yarn layer needs **two predicates**: `>=1.21.3` and `>=1.21.6`.
`IronGrate.java`, `IronGrateDataGenerator.java` and `ExampleMixin.java` are
identical across all five 1.21.x versions. Java is identical across 1.21.3 /
1.21.4 / 1.21.5.

Non-Java, across the yarn layer:

| Change | Where | Boundary |
|---|---|---|
| `"M": {"item": "x"}` becomes `"M": "x"` | both `data/irongrate/recipe/*.json` | `>=1.21.3` |
| `models/item/iron_grate.json` **deleted**, `items/iron_grate.json` **added** | assets | `>=1.21.4` |
| `version` + `depends.minecraft` range | `fabric.mod.json` | every version |

**`26.2` is the same commit as `26.1`** (`1ac09e0`) — a bare local label, so there
are 7 distinct trees, not 8. Nothing to merge there.

## The layer decision: 2, not 3

Recommend **2 layers**, same as proposed — but the reasoning matters more than the
count, because the reasoning is what transfers to Portal Glimpse.

Draw the boundary where the **build regime** changes, not where the source churns:

| | 1.21.x | 26.x |
|---|---|---|
| Loom plugin id | `fabric-loom` | `net.fabricmc.fabric-loom` |
| mappings | `net.fabricmc:yarn:...:v2` | none (Mojang names) |
| Java | 21 | 25 |

Stonecutter exists to absorb *source* churn. It cannot absorb a change of plugin
id, mapping set, or toolchain — those are resolved once per Gradle build, before
any preprocessing happens. So each distinct (plugin id, mappings, Java) triple
needs its own layer. Iron Grate has exactly two.

A 1.21.5 split would buy nothing here: `1.21.4` to `1.21.5` is 3 Java lines and
`1.21.5` to `1.21.6` is 6, which is precisely what `//? if` is for. **Mirroring
the Portal Glimpse 3-way split onto this mod would rehearse nothing extra** — a
third layer is mechanically identical to a second one, just another branch. The
transferable lesson is the rule above, and it suggests PG should re-check whether
*its* 1.21.5 split is a build-regime boundary or merely a churn boundary. If it is
churn, PG collapses to 2 layers as well.

Note that the native `branch(name) { ... }` + `branchScript` in 0.9.8 means both
layers can live in **one** git branch and one Gradle build as two Stonecutter
branches, each with its own build script and source root. That is almost certainly
the right shape; it is also the part I could not run.

## Gotchas

1. **One Loom version per Gradle build.** `loom_version` differs across the
   existing branches (1.13, then 1.15, then 1.17) but a plugin version is resolved
   once for the whole build, so it cannot vary per version. Pick the newest Loom
   that handles the oldest MC in the layer and pin it layer-wide.
   `loader_version` and `fabric_version` are ordinary dependencies and *can* vary.
2. **The Loom plugin *id* differs between layers** (`fabric-loom` vs
   `net.fabricmc.fabric-loom`). This is the hard reason the mapping boundary
   cannot be a `//? if` — it lives in `plugins {}`.
3. **Java 21 vs 25 across layers**, so the toolchain is per-layer too. Related:
   the `26.3` branch needed a real GA 25 — an EA "25-loom" build is missing
   `Math.powExact` and dies in `LevelRenderer.<clinit>`.
4. **Plain `.json` is not preprocessable.** Only `json5` is handled by default,
   and even with a handler registered the directive comments survive into the
   output, which would make Minecraft JSON invalid. The recipe change at 1.21.3
   therefore needs a per-version resource dir, `replacements`, or datagen — not a
   directive.
5. **A preprocessor cannot add or delete files.** The 1.21.4 item-model move
   (`models/item/` to `items/`) is a file add plus delete. Either ship both files
   unconditionally (each is inert in the version that does not read it — untested)
   or use per-version resource dirs. This class of change is invisible to a line
   diff, so audit for added/removed files explicitly before planning a merge.
6. **`fabric.mod.json` changes every single version** (the `depends.minecraft`
   range). Drive it from a per-version property through `processResources`
   expansion rather than directives.
7. **Gradle 9 removed `-c` / `--settings-file`.** To build with an alternative
   settings file you must swap the file. Relevant because the Stonecutter
   `settings.gradle` cannot be used by a plain single-version build.
8. **The generated controller is Kotlin and does not translate.** Stonecutter
   writes `stonecutter.gradle.kts` containing `stonecutter active "1.21.6"` — the
   Kotlin infix form. Copy that into a Groovy `stonecutter.gradle` and Gradle fails
   with *"Could not get unknown property 'active'"*, because Groovy parses it as a
   property read. Groovy needs `stonecutter.active '1.21.6'`, and set
   `kotlinController = false` in the `create` block so the `.kts` is not regenerated.
9. **Resource overlays are the answer to gotchas 4 and 5.** What worked: move the
   version-varying resources out of `src/main/resources` into `overlays/<name>/`,
   then pick one per version in the shared build script:

   ```groovy
   sourceSets.main.resources.srcDir rootProject.file(
           stonecutter.eval(project.minecraft_version, ">=1.21.3") ? "overlays/recipes-modern" : "overlays/recipes-legacy")
   ```

   `stonecutter.eval` is available on the per-version extension, and
   `project.minecraft_version` comes from `versions/<ver>/gradle.properties`, so no
   extra Stonecutter API is needed. This handles both the JSON content change and
   the file move cleanly.
10. **The active version has no generated source.** There is no
    `build/generated/stonecutter` for whatever `stonecutter.active` points at —
    that version compiles the working tree directly. So a verification script that
    diffs generated-vs-reference reports every file "missing" for the active
    version. Verify that one against a reference *build* instead.
11. **Wrong resources build silently.** Before the overlays went in, `1.21` built
    successfully and produced a jar containing the 1.21.6 recipe format and the
    1.21.6 asset path — both wrong for 1.21, neither flagged. JSON is not validated
    at build time and a missing model is a runtime problem. Nothing in the build
    output hints at it. **This is the single most dangerous thing here**: a green
    multi-version build does not mean the old versions work. Diff jar resources
    against the reference branch every time.
12. **Directives are genuinely inert.** Verified: with all directives in place the
   `1.21.6` build is bytecode-identical to the `1.21.6` branch build — all six
   classes identical under `javap -p -c`, all resources identical modulo line
   endings. Jar size differs by 1 byte (comment/line shift). So adding directives
   to a working tree is safe and reviewable.

13. **Never hand-edit `stonecutter.active`.** Editing the controller file directly
    changes the *declared* active version without rewriting the working tree, so
    the comment state desyncs and the build fails with a pile of confusing
    "cannot find symbol" errors. Use the real tasks (their names contain spaces):
    `"Set active project to <ver>"`, `"Refresh active project"` (reprocesses
    comments for the current active version — this is the repair tool), and
    `"Reset active project"`, which returns the tree to `vcsVersion` and is
    explicitly meant to be run **before committing**. Committing while switched
    records flipped comments.
14. **Stonecutter warns that Groovy buildscripts are the awkward path.** Every
    invocation prints "Stonecutter is used with Groovy buildscripts / Stonecutter
    requires a more complex Gradle setup than typical Java projects". It works
    (this whole layer is Groovy), but Kotlin is the supported route — worth
    weighing for Portal Glimpse before committing to Groovy.
15. **Dev client and dev server share `versions/<ver>/run/`,** so running both at
    once makes them fight over `latest.log` / `debug.log`. Harmless, but give them
    separate run directories if you run them together often.

### A skipped version hides a boundary — the most transferable lesson here

Iron Grate had no 1.21.2 branch; the repo jumped 1.21 to 1.21.3. So the
`1.21 -> 1.21.3` diff bundled together changes that actually landed at *different*
versions, and every one of them looked like a single `1.21.3` boundary. Adding a
1.21.2 node proved three of the four predicates wrong:

| Change | Looked like | Actually |
|---|---|---|
| `getStateForNeighborUpdate` signature | `>=1.21.3` | **`>=1.21.2`** |
| `Settings.registryKey(...)` | `>=1.21.3` | **`>=1.21.2`** |
| recipe `"M": {"item": x}` to `"M": x` | `>=1.21.3` | **`>=1.21.2`** |
| `models/item/` to `items/` | `>=1.21.4` | `>=1.21.4` (correct) |

How each was caught matters, because only one was loud:

- The two Java ones **failed the compile** the moment a 1.21.2 node existed.
- The recipe one is **silent**. It builds clean and ships a 1.21.2 jar with a
  recipe 1.21.2 cannot parse. I caught it only by extracting a vanilla recipe from
  the 1.21.2 server jar in the Loom cache and comparing the shape:

  ```bash
  jar xf ~/.gradle/caches/fabric-loom/1.21.2/minecraft-extracted_server.jar \
      data/minecraft/recipe/iron_bars.json
  ```

  The item-model boundary was confirmed the same way, by counting entries under
  `assets/minecraft/items/` vs `assets/minecraft/models/item/` per version: 0/1833
  at 1.21.2 and 1.21.3, 1385/1076 at 1.21.4.

**For Portal Glimpse:** the branch-per-version model cannot reveal a boundary in a
version you never branched. When you convert, enumerate every MC version in the
supported range, not just the ones that have branches, and add nodes for the gaps.
Then check each boundary against *vanilla's own data*, not against your diffs —
your diffs only tell you something changed somewhere between two branches you
happen to have.

### The payoff, demonstrated

Iron Grate declared `"environment": "client"` in `fabric.mod.json` — on every
branch, since forever. The effect on a dedicated server: the block and its recipe
are never registered server-side, so the crafting output slot stays empty, and
creative-picking the item kills the connection with
`DecoderException: Failed to decode packet 'serverbound/minecraft:set_creative_mode_slot'`
because the client sends an item id the server has no registry entry for.
Singleplayer always worked, because the integrated server lives inside the client.

Changing it to `"environment": "*"` fixed it, confirmed on a real dedicated server
(mod loaded, `Found new data pack irongrate`, and the mod's own advancement fired
for a connected player).

The point for Portal Glimpse: that was **one edit to one shared file**, and it
landed in all six version jars at once, verified. Before Stonecutter it would have
been the same edit cherry-picked across six branches, with six chances to miss one.
That is the case for doing this.

## Verification harness (reuse this on Portal Glimpse)

Build the reference from a **detached** worktree so no branch ref can be dirtied:

```bash
git worktree add --detach /tmp/ref 1.21.6
```

Then compare, ignoring line-ending and line-number noise:

```bash
diff <(tr -d '\r' < sc/$f) <(tr -d '\r' < ref/$f)
diff <(javap -p -c sc/$f) <(javap -p -c ref/$f)
```

`javap -p -c` is the right granularity: it ignores the line-number shifts that
comments introduce but still catches any real codegen change. Plain `cmp` on the
jar is useless here — it reports a diff for a 1-byte comment shift.

Set this up **before** doing the merge, not after. It is what makes a
multi-version refactor trustworthy, and it is cheap.

## What I would do differently on an 11k-line mod

Iron Grate is ~630 lines of `src` across 70 files, of which only 4 are Java. At
11k lines the balance changes:

- **Keep the mapping boundary away from Stonecutter.** At 630 lines you *could*
  brute-force yarn/Mojang with directives; the `1.21.6` to `26.1` diff touches
  every import in every file. At 11k lines that is two interleaved copies of the
  mod in one tree — unreadable and unreviewable. Separate layers, always.
- **Expect the resources, not the Java, to be the problem.** Here 3 of the 6
  distinct changes were in JSON, and the two that Stonecutter genuinely cannot
  express were both resource-shaped (JSON content, file move). Inventory the
  resource changes first.
- **Audit added/deleted files before planning.** A `--stat` line diff hides them,
  and they are the changes with no preprocessor answer.
- **Keep directives out of hot files.** Where a file diverges heavily, a
  per-version source dir beats a thicket of `//? if`.
- **Land the equivalence harness first**, then convert one version at a time,
  proving bytecode equivalence at each step. That is the only way to keep an
  11k-line migration honest.
