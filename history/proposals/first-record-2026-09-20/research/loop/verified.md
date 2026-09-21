# Primary sources the orchestrator opened itself

Only claims that could change a lean. "Holds" means I fetched or grepped the
source and read the words in context on 20 September 2026. It does not mean
the worker's inference from it holds.

| # | claim (worker) | source | result |
|---|---|---|---|
| 1 | Hickey: provenance goes "on the transaction (which can have an open set of attributes) ... substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)" (research-2) | Datomic Google Group, 20 May 2016, https://datomic.narkive.com/Rn7jWmvv/modelling-a-graph-using-reified-transactions | Holds word for word. He means who said it and when ("Lucy said that Fred likes Ethel"); he does not speak of recorded reads there. |
| 2 | The Kay / Hickey thread (research-3, softland-ff) | `../sources/hn-11945722-kay-hickey.md`, fetched through the HN Algolia API | Read in full; 18 comments by alankay, 7 by richhickey. |
| 3 | Rama: "Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place." (research-1) | `reference/rama/docs/14-depots.md:310` | Holds. |
| 4 | Rama: "An exception doesn't mean the append did not go through – it just means it didn't go through cleanly." (research-1) | `reference/rama/docs/14-depots.md:212` | Holds. |
| 5 | Rama: depot appends made inside a microbatch topology "currently do not have exactly-once semantics in the face of failures and retries. However, this is on our roadmap." (research-1) | `reference/rama/docs/12-microbatch-topologies.md:130` | Holds. |
| 6 | Rama: "The microbatch ID is a 64 bit value that increments by one with each successful microbatch ... each microbatch ID is associated with a specific range of data on each depot partition" (research-1) | `reference/rama/docs/25-integrating.md:336` | Holds. |
| 7 | Rama: the re-partition recipe "only works if your processing is deterministic, which may not be the case if your processing makes use of any random numbers (such as UUIDs)" (research-1) | `reference/rama/docs/19-operating-rama.md:489` | Holds. |
| 8 | Rama: the task count cannot be changed after launch (research-1) | `reference/rama/docs/19-operating-rama.md:483`; `.claude/skills/rama/references/operate.md:280` | Holds, with a nuance the summary dropped: "Currently Rama does not support changing the number of tasks for a module, though adding support for this is high priority for us." A present limit that RPL says it means to lift, not a permanent law. |
| 9 | Rama: a depot can take a custom partitioner, so a layer-keyed placement is possible (research-1, round three) | `reference/rama/docs/14-depots.md:45-64` (`Depot.Partitioning`) | Holds. |
| 10 | XTDB v1: "The indexer will pause consumption of transactions while waiting for all their documents to appear. When evicting documents they will have been compacted in Kafka, so replay will just block" (research-2) | github.com/xtdb/xtdb issue 184, "Eviction Will Cause Kafka Replay to Block", hraberg, 20 April 2019, closed | Holds. The issue proposes a tombstone in place of deletion. |
| 11 | XTDB v1: "There is a race condition which would happen if someone tries to resurrect a document in quick succession of evicting it, or replaying it." Still open. (research-2) | github.com/xtdb/xtdb issue 432, "Race Condition for Eviction Tombstone", hraberg, 22 November 2019, open | Holds, and it is still open. The proposed fix is a permanent record of evicted entity ids. |

Not yet opened by me (as reported by the workers, who say their quotes were
script-checked against saved source text): Nubank's regrets on InfoQ; the EDPB
2025 guidance; TigerBeetle on retried transfers; FoundationDB versionstamps and
Apple's incarnation prefix; Webstrates on cursor durability; Aurora DSQL's
adjudicators; Delos on code-version skew; Certificate Transparency's Yeti2022.
