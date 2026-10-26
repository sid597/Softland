# Decision 4: who acts, and in which session

Decision-4 agent (Opus, xhigh), HEAD c90c7bcc. Nothing here is decided. "Run": what the store's pure functions said (`runs/decision-4-ends.clj`).

## (a) The question

When the road sends this repository's files in, whose name does each act carry, into which layer, in which session?

## (b) The ends

**Shared.** The four parts are in the act, and the layer leads the act-id (`src-store/softland/store/act.clj:155-158`, `:174-176`) and both row keys (`src-store/softland/store/record/rows.clj:163`, `:173`), so an act never moves. The run passes acts of A, D, E and the making acts of B, C, no rule changed: values in an unchanged form, so each **fits none of the three costs**.

Two costs apply. The base's copy of its rows on every task is a table cost of the layer, on A, D and E alike. The lens holds for what comes next: ending a permission, a session rule or a system signature would each be a version of the act; a table by session, a table cost; sign-in before the door, none. A store thrown away before the first real record hardens nothing (my reasoning) (`src-store/README.md:911-922`, `:941-951`, `:942-943`).

The base starts as "everyone", and any see-and-act holder there can change it, both marked "Not decided" (`src-store/softland/store/gate/first_act.clj:139-146`; `src-store/softland/store/gate/decision.clj:283-300`). Such a layer is shown to any asker before sign-in is checked (`src-store/softland/store/read_path/layers.clj:140-141`). The repository is closed source (‹60c1bcaa›).

| End | `:who` · `:layer` · `:permission` | Gives, costs |
|---|---|---|
| A. System, base | `:system` · `:base` · `[:system :base :base]` | Nothing to build. Any program at the ports can carry the system's name (`src-store/softland/store/door.clj:72-75`; `src-store/README.md:561-562`) |
| B. Sid, own layer | `<sid>` · `<layer uuid>` · `[sid L L]` | No code makes a person (`src-store/README.md:368`). The system cannot act in a layer it made for him (run) |
| C. System, own layer | `:system` · `:tools` · `[:system :tools :tools]` | Streaming (`first_act.clj:60`). Only the system is a member (my reading, `layers.clj:114-144`) |
| D. Importer, base | `:ingester-1` · `:base` · `[:ingester-1 :base :base [:system :base :base]]` | Names its program. Nothing ends its right (`decision.clj:61-67`) |

**E (missing): the importer under Sid, under the system.** `[:ingester-1 :base :base [sid :base :base [:system :base :base]]]` is accepted with a hand-made UUID (run). E needs a person's id; only keeping it needs sign-in (a store-made id joined to his accounts later, K8). One importer id serves one person: a second person's give is refused `:store-key-takes-one-write` (run; `src-store/softland/store/door/declaration.clj:224-230`; `decision.clj:302-330`).

**F (missing): the repository in the base as its last commit, edits made in Softland in a layer above,** so each place has one writer. The vision: "the last commit is its base layer whatever i build on top is in the active layer" (`vision/LOG.md:146`; in the record only inside a long prompt, ‹5940b98d›; also ‹86db6d30›). Without F, under E, the importer's replacing act is refused once another writer has written the place, and one that names nothing to replace overwrites unseen (`decision.clj:251-258`, `:236-243`).

**Session.** Required (`act.clj:158`), read only for its id's length (`src-store/softland/store/door/declaration.clj:498`). Sid pictures a session as starting at login (‹ac4db2eb›). Rows keep no session (`rows.clj:24-31`), so a table by session is no finer than the values written now: one id per run beats one standing id (my reasoning).

## (c) What Sid said

- "1. system"; "it ties back to a single individual" (‹f3f530b8›). The system tops the base's chain; who acts as it is not said.
- "by user's permission" … "if it is a system level thing like we as the architect of it are trying to do then it comes under that part" (‹ddf13d17›). The architect reading Softland's code in fits it: support for the system at the top (D, E). Whether this repository is that thing, it does not say.
- "The base there means like the code that we are writing"; "me making the changes or a few set of people or agents" (‹4123e23c›). This says what the base means, not where this repository goes.
- "1. spoken in the door will be left open but yeah I will uh, by default have A" (‹1088472f›). This answered a session's "(a) for now, with the door left open", on signing (`src/proposal/frame-2026-09-15/PROGRESS.md:63-64`).
- "we need the sign-in" (‹bec8ca63›). The prompt's "needs no sign-in yet" is a session's («ca81bcf6»; «9a48928c», pasted in ‹6ac75ce2›).

## (d) What the examples found

The old server's code and markdown importers acted as "system" (`src/app/server/ingest/clojure_adapter.clj:553`, `:623`; `markdown_adapter.clj:461`, `:531`). Its transcript importer acted under an id of its own, typed `:system` (`transcript_import.clj:592-605`). The matter room's resident and the autotag actor were typed `:agent` (`src/app/server/page/matter_room.cljc:274-284`; `src/app/server/episode/material_circulation.clj:305-309`).

## (e) Default, not a decision

D now, E before anything is kept, a new session uuid per run. Reason: "take the side of spectrum that captures more" (‹5ff89890›). He said it of reads; applying it here is mine. E keeps the program, the person and the system. Its cost is a sign-in build before any real data, with K7 designed anew, since its locks are gone (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:113`; `src-store/README.md:685-745`).

## (f) Questions

1. Each section of `src-store/README.md` goes in as an act whose name and layer never change, and anyone who can reach the store reads the shared base. Whose name, and where?
   A: the system, in the base. B: you, in your own layer. C: the system, beside the base. D: the importer, in the base, under the system. E: the importer, under you, under the system. F: E, with your edits in a layer above. Which files count as "the code that we are writing" is not decided; most of the 1,772 tracked `.md` files are not code. Default, either way: D now, E before anything is kept. If you say nothing, D, and nothing is kept.
   Source: (b); ‹4123e23c›; queue K14, V25.
2. Today any program on your machines can put the system's name on an act. You wrote that the road needs sign-in; tonight's prompt says it does not.
   A: start without sign-in. B: sign-in first. Default A, with nothing kept until sign-in exists. If you say nothing, A.
   Source: ‹1088472f›; ‹bec8ca63›; queue K7, K9, E7.
