# Stonecutter notes — Iron Grate rehearsal

Written for the Portal Glimpse merge. Everything below is either measured from
this repo or recovered from the plugin jar; where something is unverified it says so.

## Status

| | |
|---|---|
| Branch | `stonecutter`, based on `1.21.6` |
| Verified | source directives for the yarn layer; `1.21.6` builds **bytecode-identical** to the `1.21.6` branch |
| Not verified | anything requiring the Stonecutter plugin to actually run (see Blocker) |

### Blocker

The Stonecutter plugin could not be executed in this session: adding it to
`settings.gradle` makes Gradle fetch and run third-party code from
`maven.kikugie.dev`, which the sandbox refuses ("Code from External"). So the
version declarations here are written but **never run**. No multi-version build,
no per-version jars, no in-game test list. That needs a session allowed to
execute the plugin.

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
  node-to-TaskProvider map. The actual aggregate task name still needs to be read
  off a real `gradlew tasks` run.
- **Default file handlers are `java, scala, groovy, gradle, json5`** (plus kotlin,
  shader, hash groups). Plain `.json` is *not* handled. See gotcha 4.

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
8. **Directives are genuinely inert.** Verified: with all directives in place the
   `1.21.6` build is bytecode-identical to the `1.21.6` branch build — all six
   classes identical under `javap -p -c`, all resources identical modulo line
   endings. Jar size differs by 1 byte (comment/line shift). So adding directives
   to a working tree is safe and reviewable.

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
