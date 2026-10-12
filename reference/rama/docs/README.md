# Rama Documentation — Local Reference Guide

> Extracted from [Red Planet Labs Documentation](https://redplanetlabs.com/docs/~/index.html)
> Generated: 2026-03-22

## Which version this copy is

Checked on 12 October 2026. The store runs Rama 1.6.0 (the root `deps.edn`).

**The words of this copy are the words of Rama's docs while 1.6.0 was the newest
release.** So a statement that is read from a page here is read from the docs
of 1.6.0. One thing in the copy is of another version: the Maven example names
1.5.0, in `19-operating-rama.md`, line 291, and in
`32-downloads-maven-local-dev.md`, line 44. The site's example still named
1.5.0 when the copy was made. For the store the number is 1.6.0.

No page of the copy was changed for this check, so a line number that a paper
cites still points at the same line.

What the statement rests on. Each of these was checked on 12 October 2026; none
is from memory:

- **The site keeps one version.** A page of
  `https://redplanetlabs.com/docs/~/` carries no version. Today the site's
  Maven example names 1.9.0, and all 34 pages were last built on 6 July 2026
  (the `Last-Modified` of each).
- **When each release came out.** 1.5.0 on 23 January 2026, 1.6.0 on 16 March,
  1.7.0 on 27 May, 1.8.0 on 29 May, 1.9.0 on 3 July: the dates of the files in
  Red Planet Labs' Maven repository. This README gives 22 March 2026 for the
  copy, which is six days after 1.6.0. The copy came into git on 3 July 2026
  and its pages have not changed there since.
- **The copy against the site today**, all 34 pages, word by word, about
  121,000 words. The copy has the label `java` on a code listing, 571 times,
  and the number before an item of a numbered list, 68 times; the site's text
  has neither, and both are marks of the markdown. In the two lines above the
  copy has 1.5.0 where the site has 1.9.0. No other word differs.
- **The site today against the Wayback Machine**, all 34 pages, line by line,
  with the last capture of each page before 27 May 2026. The text is the same
  but for the version in those two lines. Of those captures 17 are from the
  time when 1.6.0 was the newest release (30 March to 21 May 2026). For each of
  those 17 pages the last capture before 16 March has the same text too: the
  page did not change when 1.6.0 came out. The other 17 captures are from
  3 November 2025 to 7 March 2026.

For those other 17 pages the archive holds no capture between 16 March and
27 May. That their text was the same in that time is derived: it is the same
before it and after it, and the copy, made inside that time, has it too.

What the docs do not tell:

- **What a release changed.** That is in
  [the release notes](../release-notes.md). Of the five changes that they list
  for 1.6.0, one is of a thing that these pages name: the dynamic option
  `topology.microbatch.pstate.flush.path.count` became an option of a PState,
  where before it was an option of a topology only.
  `12-microbatch-topologies.md`, line 160, describes the option and does not
  say that. The pages have the text that they had before 1.7.0 came out, and
  they name none of the functions that the notes list for 1.7.0, 1.8.0 or
  1.9.0. So nothing here describes a thing that came after the store's Rama.
- **What a function does.** The site's Clojure API pages are a different set of
  pages, and today they are for 1.9.0. They were not part of this check. For
  what a function of 1.6.0 takes and gives, the jar that the store runs is the
  source.
- **What Rama does.** Nothing here was run. This is a check of the words of the
  docs. A statement that a paper marks "read, not run" is still not run.

The fetched pages, the captures, the texts that they were turned into, the
differences and the four scripts are outside the repository, in
`/mnt/data/projects/research/softland/rama-docs-for-1.6.0-2026-10-12/`. The
README there says how to run the check again.

Rama is a distributed-first programming platform that combines database and stream processing into a single system. It replaces the need for separate databases, message queues, caches, and coordination services.

## Core Concepts at a Glance

- **Depots** — append-only logs (event sourcing). Data enters Rama through depots.
- **PStates** — partitioned states (materialized views). Arbitrary nested data structures, not just tables.
- **Topologies** — dataflow pipelines that read from depots, transform data, and write to PStates.
- **Tasks** — module partitions distributed across workers. Must be powers of two.
- **Modules** — Rama's deployable units. Contain depots, PStates, and topologies.

## Reading Order

### Tutorial (start here)
| # | File | Topic |
|---|------|-------|
| 01 | [01-index.md](01-index.md) | Documentation overview |
| 02 | [02-why-use-rama.md](02-why-use-rama.md) | Why use Rama — problem statement and benefits |
| 03 | [03-first-module.md](03-first-module.md) | Hello World — clusters, modules, depots, event sourcing |
| 03a | [03a-distributed-programming.md](03a-distributed-programming.md) | Tasks, partitioning, consistency |
| 04 | [04-depots-etls-pstates.md](04-depots-etls-pstates.md) | PState schemas, ETL pipelines, querying |
| 04a | [04a-dataflow-programming.md](04a-dataflow-programming.md) | Vars, branching, conditionals, loops, custom ops |
| 05 | [05-types-of-etls.md](05-types-of-etls.md) | Stream vs microbatch vs query topologies |
| 06 | [06-tying-it-together.md](06-tying-it-together.md) | Complete social network backend (~180 LOC) |

### Reference — Terminology
| # | File | Topic |
|---|------|-------|
| 07 | [07-terminology.md](07-terminology.md) | All key terms defined |

### Deep Dive — Data & Dataflow
| # | File | Topic |
|---|------|-------|
| 08 | [08-paths.md](08-paths.md) | Path navigators — reading/writing PStates |
| 09 | [09-intermediate-dataflow.md](09-intermediate-dataflow.md) | Batch blocks, joins, macros, yieldIfOvertime |
| 10 | [10-aggregators.md](10-aggregators.md) | Accumulators, combiners, two-phase aggregation, topMonotonic |

### Deep Dive — Topology Types
| # | File | Topic |
|---|------|-------|
| 11 | [11-stream-topologies.md](11-stream-topologies.md) | Stream topology details |
| 12 | [12-microbatch-topologies.md](12-microbatch-topologies.md) | Microbatch topology details |
| 13 | [13-query-topologies.md](13-query-topologies.md) | Query topology details |

### Deep Dive — Core Components
| # | File | Topic |
|---|------|-------|
| 14 | [14-depots.md](14-depots.md) | Depot internals — partitioning, acking, cross-module |
| 15 | [15-pstates.md](15-pstates.md) | PState internals — subindexing, reactivity, mirrors |
| 16 | [16-partitioners.md](16-partitioners.md) | All partitioner types |
| 17 | [17-serialization.md](17-serialization.md) | Custom serialization |
| 18 | [18-module-dependencies.md](18-module-dependencies.md) | Cross-module depot sourcing and PState queries |

### Operations & Infrastructure
| # | File | Topic |
|---|------|-------|
| 19 | [19-operating-rama.md](19-operating-rama.md) | Cluster setup, deployment, monitoring, updates |
| 20 | [20-heterogenous-clusters.md](20-heterogenous-clusters.md) | Mixed-hardware clusters |
| 21 | [21-replication.md](21-replication.md) | Replication model |
| 22 | [22-backups.md](22-backups.md) | Backup and restore |

### Integration & API
| # | File | Topic |
|---|------|-------|
| 23 | [23-acid-semantics.md](23-acid-semantics.md) | ACID guarantees in Rama |
| 24 | [24-rest-api.md](24-rest-api.md) | REST API for depot appends and PState queries |
| 25 | [25-integrating.md](25-integrating.md) | Integrating with external tools |
| 26 | [26-all-configs.md](26-all-configs.md) | Configuration reference |
| 27 | [27-testing.md](27-testing.md) | Testing with InProcessCluster |

### Clojure API
| # | File | Topic |
|---|------|-------|
| 28 | [28-clj-defining-modules.md](28-clj-defining-modules.md) | Defining modules in Clojure |
| 29 | [29-clj-dataflow-lang.md](29-clj-dataflow-lang.md) | Clojure dataflow language |
| 30 | [30-clj-serialization.md](30-clj-serialization.md) | Clojure serialization |
| 31 | [31-clj-testing.md](31-clj-testing.md) | Clojure testing |

### Setup
| # | File | Topic |
|---|------|-------|
| 32 | [32-downloads-maven-local-dev.md](32-downloads-maven-local-dev.md) | Downloads, Maven, local development |

## Quick Reference — Naming Conventions

| Prefix | Meaning | Example |
|--------|---------|---------|
| `*` | Dataflow variable (var) | `*token`, `*userId` |
| `$$` | PState name | `$$wordCounts`, `$$followers` |
| `**` | Unground var (outer joins) | `**v2` |
| `*___` | Delayed unground var | `*___tuple2` |

## Quick Reference — Key Patterns

```java
// Declare depot with hash partitioning
setup.declareDepot("*depot", Depot.hashBy(ExtractField::new, "userId"));

// Declare PState with nested schema
s.pstate("$$data", PState.mapSchema(String.class,
    PState.listSchema(PState.mapSchema(String.class, Object.class))));

// Stream ETL: source → partition → aggregate
s.source("*depot").out("*record")
 .hashPartition("*key")
 .compoundAgg("$$counts", CompoundAgg.map("*key", Agg.count()));

// Query PState
pstate.selectOne(Path.key("myKey"))
pstate.selectOne(Path.key("k").sortedMapRangeFrom(0, 100).mapVals())

// Transform PState
.localTransform("$$data", Path.key("*k").termVal("*v"))
```

## Softland-Relevant Sections

For Softland's architecture (Rama as event sourcing substrate), the most relevant pages are:

1. **PStates** (15) — the core data model. Arbitrary nested structures, subindexing, reactive queries
2. **Depots** (14) — append-only event log, partitioning strategies
3. **Stream topologies** (11) — real-time event processing
4. **Paths** (08) — navigating and transforming nested PState data
5. **ACID semantics** (23) — consistency guarantees
6. **Clojure API** (28-31) — since Softland is a Clojure project
7. **Module dependencies** (18) — cross-module PState queries (relevant for multi-slice architecture)
