# Sense-Line MVP — dependency map (rendered)

Same DAG as `DIRECTION.md` §2. Arrows read "feeds / unlocks".

```mermaid
graph TD
  S["THIS SESSION<br/>sense-line-model.md · DIRECTION.md<br/>CLAUDE.md registers + memory"]

  SPEC["SPEC ROOM — branch of this chat<br/>Sid + Fable, close iteration<br/>OUT: SPEC.md — block grammar · node kinds · relations<br/>(spec-dev ≠ benchmark)"]
  DESIGN["DESIGN ROOM — Sid's full attention<br/>systems exploration from direction + canon<br/>ui-design-pass prompt v2"]
  RAMA["RAMA ROOM — fresh, /rama, contract-grade<br/>reader · block grain · consolidator<br/>morning answer · return path"]
  BENCH["BENCHMARK ROOM — fresh<br/>bench-1: marker vs gold<br/>bench-2: self-marking effect (fork-decider)"]
  AM["AMENDMENTS A1 (D-002 unit) · A2 (D-008 write)<br/>Sid's hand → decisions.md as PROPOSED"]
  PS["PRODUCT-SIDE ROOM (exists)<br/>face-2 scope recheck"]

  S --> SPEC
  S --> DESIGN
  S --> AM
  S --> PS
  SPEC -->|"spec v0"| RAMA
  SPEC -->|"spec v0 + gold example"| BENCH
  SPEC -->|"one hand-marked example (fixtures)"| DESIGN
  BENCH -.->|"bench-2 verdict: author-mint vs reader-mint"| RAMA
```

Plain English: the SPEC ROOM gates three rooms. Design's systems exploration, the amendments, and the face-2 recheck need only this session's docs. Nothing depends on ingesting the past.
