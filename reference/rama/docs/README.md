# Rama Documentation — Local Reference Guide

> Extracted from [Red Planet Labs Documentation](https://redplanetlabs.com/docs/~/index.html)
> Generated: 2026-03-22

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
