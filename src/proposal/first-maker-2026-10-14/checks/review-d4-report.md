# Review of the page on decision 4 (phase two)

Reviewer: Opus 5.5 at max effort, the same reviewer whose own thinking, written
before reading the page, is `checks/review-d4-own-thinking.md`. The page,
`decision-4-who-acts.md` (51 lines), was read once, with
`runs/decision-4-ends.clj` and `runs/decision-4-ends-output.txt`. "page:N" is a
line of the page. [mine] marks my reasoning. For this report I made one run
(`clojure -M:store -e`, pure code): a second person's give of the same importer
id in the base is refused `:store-key-takes-one-write` when the place already
holds the first give, and passes when the place is empty.

## What the page misses

1. **Where it puts the repository, everyone can read it.** A, D and E, and both
   of the page's defaults (page:37, page:48), write this repository into the
   base. The base starts as "everyone"
   (`src-store/softland/store/gate/first_act.clj:139-146`). A layer set to
   everyone is shown to any asker before the read path asks whether she is
   signed in (`src-store/softland/store/read_path/layers.clj:140-141`). Any
   holder of "see and act" in the base can change that setting
   (`src-store/softland/store/gate/decision.clj:283-300`). Sid made the
   repository closed source (‹60c1bcaa›). The page cites K14 as a source for
   question 1 (page:49) but never says what the base shows, or to whom. [mine]
   Of everything about "where", this is the consequence Sid would most want in
   front of him when he answers question 1, and it ties the default to V25.

2. **Two writers of one place, and Sid's own picture of "base".** Under E
   (page:20, page:37) the importer and Sid both write into the base. The page
   does not say where edits and notes made inside Softland, by Sid or by an
   agent, are to go. If they go into the base, the importer's next replacing act
   is refused once someone else has written the place
   (`decision.clj:249-257`). If that act names nothing to replace, it writes over
   the edit unseen (`decision.clj:233-240`). The vision log draws the other
   shape: "the last commit is its base layer whatever i build on top is in the
   active layer" (`vision/LOG.md:146`; the record holds this only in a long
   prompt of his, ‹5940b98d›). He also wrote that ingested data is the base,
   used "as active layer" (‹86db6d30›). The read path already shows the nearest
   layer's fact over the one below (`src-store/README.md:528-541`). [mine] None of
   the page's ends puts a layer above the base for work in progress, and that end
   gives each place one writer.

3. **What a session is.** The decision asks "what an ingester's session is". The
   page gives three values and their score under the lens (page:22), not what a
   session is for. It does not have:
   - Sid's picture of a session that starts at login (‹ac4db2eb›).
   - The other ends: the session of whoever started the run; a session as a
     layer (the hand and agent session layers of the rulings file,
     `src/proposal/frame-2026-09-15/PROGRESS.md:109-112`, a session's text
     written on his go); and a session that facts describe.
   - That the E6 sheet's "lean" changed ‹ddf13d17› from a permission to a session
     (`/mnt/data/projects/research/softland/all-decisions-rest-2026-10-06/make_rest.py:48-50`).
   - That rows keep no session (`src-store/softland/store/record/rows.clj:24-31`).
     So a later table of acts by session can be no finer than the values written
     now. [mine] That is why an id for each run beats "one standing id", which the
     page scores the same.

4. **E's hidden cost: one importer id serves one person.** A holder has one
   permission in a layer, and a permission's place takes one write
   (`src-store/softland/store/door/declaration.clj:224-230`;
   `decision.clj:302-330`). Once Sid has given `:ingester-1` its permission
   beneath his own in the base (page:20), a second architect cannot give the same
   id one beneath hers. My run gives `:store-key-takes-one-write`, and nil with
   the place empty. [mine, from the run] Unless V12 changes
   (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:178`),
   each person who runs the importer needs her own importer id.

5. **What the importer's right lets it do.** D and E (page:18, page:20) give the
   importer "see and act" in the base. A holder of that right can give a
   permission beneath her own (check (g), `decision.clj:267-281`). She can also
   write the base's settings, including who may see it (`decision.clj:283-300`).
   A permission says only `:see` or `:see-and-act`
   (`src-store/softland/store/act.clj:187-190`), so nothing allows acting without
   giving. The page says "nothing ends it" and stops there. [mine] The set of
   those who write the base can therefore grow from the importer's right. That is
   the practical half of "who may act as the system".

6. **Many writers, many readers, years.** The page does not take up what the
   prompt asked for at that scale. On 13 October Sid left open "whether the layer
   that many people write into is a separate layer from the base", and the store's
   page adds that "Nothing is built for a microbatch layer with many writers"
   (`src-store/README.md:873-876`). [mine] Under E, the base becomes that layer as
   soon as a second person or an agent runs the importer. Over years, the base's
   history is useful only if it says who changed what. E keeps that and A does
   not, and the page does not put it in those terms.

7. **Two concerns, and the shape of the code.** This repository is both a
   codebase being onboarded and Softland's own code (`vision/LOG.md:226-241`; the
   record holds that passage first in a session's text, «6c79c222»;
   `docs/carry-on.md:101`). The page's default moves from D on a thrown-away
   store to E on a kept one (page:37). It does not say that the run should take
   its actor, permission, layer and session as inputs. [mine] With them as inputs,
   that move is a setting and not a rewrite. The same code then also runs for a
   person into her own layer, the case of every other inhabitant: "a new user can
   come and describe the adapter for whatever external data that they have"
   (‹6b40e2e2›).

8. **Agents as actors.** None of A to E has an agent as the actor. Commits made
   at Sid's word carry his name: 148 of the 149 commits since 1 October carry
   `sid597` (`git log`), and the project's rules have sessions commit on main
   (`CLAUDE.md`, "Commit mechanics"). How many of those commits a session made is
   not checked. Sid works "with my 10 agents" (‹12fed86b›). The page cites E7 for
   question 2 (page:51) but has no end for it. E4's reading, "each actor acts
   under its own permission" (`src-store/README.md:317-318`, a session's), would
   put the agent where the page puts the importer. [mine]

9. **Sid's September answer on senders that sign nothing.** Question 2 (page:50)
   asks whether to start without sign-in. In September he chose no signing at
   launch, with the door left open for it: "1. spoken in the door will be left
   open but yeah I will uh, by default have A" (‹1088472f›). He wrote that to a
   session's "(a) for now, with the door left open" (rulings file,
   `frame-2026-09-15/PROGRESS.md:63-64`). The sentence there that facts before
   signing rest on the operator's word is the session's. The page does not cite
   this. [mine] It is the nearest answer he has already given.

10. **What "nothing kept until sign-in" costs.** The page's default (page:50)
    makes sign-in a condition of the first real record. The page of what must be
    true before real data puts what waits for sign-in under "Not on this page"
    (`src/proposal/backend-on-rama-2026-10-02/WHAT-MUST-BE-TRUE.md:64-69`). K7,
    where a person's link lives, rests on destroying a link's lock
    (`OPEN-QUESTIONS.md:113`), and the store has kept no locks since 12 October
    (`src-store/README.md:685-745`). [mine] So the default puts a sign-in build,
    with K7 designed anew, before any real data. The page does not say so.

11. **Where the lens holds.** The page says where the lens fails for these ends
    (page:11). It does not say where the lens holds for what decision 4 brings
    next. [mine]
    - Ending a permission (V10), a rule on sessions (E5), or a signature for
      `system` would each change what a gate accepts, so each is a version of the
      act (13 October entry 6, `src-store/README.md:941-951`).
    - A table of acts by session would be the table cost.
    - Sign-in in front of the door is in none of the three; its cost falls on the
      acts sent before it (`src-store/softland/store/door.clj:109-110`).

12. **Only the vision log is left out.** The page excludes `vision/LOG.md` as "his
    words, not code" (page:28, page:37). [mine] By that measure most of the
    repository's 1,772 tracked `.md` files (proposals, history, research, memory)
    are not "the code that we are writing" either (‹4123e23c›), yet the default
    would read them into the base.

13. **The reason behind the Inland precedent is gone.** Inland read the repository
    into the base as the operator because a tool's lock lease could not run there
    (page:33; `src-inland/softland/inland/material.clj:1-9`). The locks went on
    12 October (`src-store/README.md:685-745`). [mine] That reason no longer bears
    on the store. Low.

## What the page gets wrong

1. **"(A adds a table cost)": so do D and E.** Page:11 and page:15 give the
   base's copy on every task to A only, but D (page:18) and E (page:20) write into
   the base too. The copy belongs to the layer, whoever acts (13 October entry 4,
   `src-store/README.md:911-922`). [mine] Read as written, the table tells Sid that
   D avoids a cost that A has. It does not.

2. **Page:28 gives a reading as what Sid said.** The page writes "The code goes to
   the base, changed by named actors." His line is "me making the changes or a
   few set of people or agents" (‹4123e23c›), and "named" is not in it. Named
   actors under the system, against a few who act as the system, is exactly the
   K9 question. The same dictation also gives the base a product meaning, "the
   global feed that gets deployed on Mastodon. So that anyone can go there and
   just surf around" (‹a06ac414›), and ‹1bd5708f› adds "maybe a few data based
   layers". Whether this repository's material is the base is what decision 4
   asks («e73248af» lists it as the question). [mine]

3. **The old server's importers were not given agent ids.** Page:33 says its
   importer got an agent id, "NOT `:system`". The old server's code, markdown and
   transcript importers acted with `:actor/type :system`
   (`src/app/server/ingest/clojure_adapter.clj:553`, `:623`;
   `markdown_adapter.clj:461`, `:531`; `transcript_import.clj:598-605`). The one
   actor typed `:agent` is the matter room's resident actor
   (`src/app/server/page/matter_room.cljc:274-284`). What the old server did give
   its importers is an id of their own, by "git_spine's `import:<name>`
   convention" (`transcript_import.clj:592-596`). [mine] That is real evidence for
   D, but of a different kind than the page states.

4. **E needs a person, not sign-in.** Page:20 says "It needs Sid's id, so it
   waits for sign-in." The page's own run plays E with a UUID made by hand
   (`runs/decision-4-ends.clj:8`, `:28-33`), and it passes. What sign-in adds is
   that the id is the store's own and that his accounts are later joined to it. A
   person made before sign-in needs K8's join of an account to a person who exists
   (`OPEN-QUESTIONS.md:114`). [mine] So E could be tried tonight on the store that
   is thrown away; what waits is keeping it.

5. **"Silent on which kind this repository is."** Page:27 says this of
   ‹ddf13d17›. But the line names the case "if it is a system level thing like we
   as the architect of it are trying to do then it comes under that part", and
   reading Softland's own code in, by its architect, fits that case. [mine] The
   line supports the system's chain at the top, which D and E keep. Calling it
   silent drops that support.

6. **"Sign".** Page:15, page:41 and page:50 say an act is "signed", and question 1
   asks "Who signs". Nothing is signed: an act names a `:who` that no one checks
   (`door.clj:72-75`; `first_act.clj:30-33`). Signing is what Sid deferred in
   September (‹1088472f›; rulings file `PROGRESS.md:63-64`, "No signing"). [mine]
   Because the word has that meaning in this project, "whose name the act carries"
   says what happens. Low.

## What the page has that I did not think of

1. **E: the importer under Sid, under the system, in the base.** The permission
   is `[:ingester-1 :base :base [sid :base :base [:system :base :base]]]`, and the
   gate's functions accept it (page:20; `runs/decision-4-ends-output.txt:11-14`).
   One chain keeps the program (the `:who`), the person who ran it, and the system
   at the top. It is in every act and in every fact's row, where a read can get it
   (`rows.clj:29-31`; W12 B, `src-store/README.md:603-614`). It answers three of
   his lines at once: "by user's permission" (‹ddf13d17›), "it ties back to a
   single individual" (‹d05a12e6›), and the system at the top of the base's chain
   (8 October entry 1). I had three separate ends (the importer under the system,
   Sid under the system, an agent under Sid) and did not join them. [mine] For
   anything kept, E is better than my own default of the importer under the
   system alone. Its cost is in "misses" 4.

2. **The old server's finding that `:system` skipped the checks.** Its
   `authorized-request?` lets through any actor typed `:system`, and "does not
   authenticate the actor identity" (`src/app/server/rama/envelope.clj:367-377`).
   One actor was typed `:agent` so that its capabilities would be checked
   (`matter_room.cljc:274-284`). [mine] That example teaches keeping content off
   the system's id. I had found only the old server's default system actor.

3. **One rule that stages the whole decision.** D while the store is thrown away,
   E before anything is kept, and nothing kept until sign-in exists (page:37,
   page:48, page:50). I left the choices for the first real record open to Sid.
   The page gives him one rule that keeps every end free now and puts a person in
   every kept chain. [mine] Its cost is in "misses" 10.

4. **The first source of "needs no sign-in yet".** It is «9a48928c» (17:26 UTC,
   14 October), which Sid pasted as paste 2 of ‹6ac75ce2› (page:29); `memtool who`
   confirms it. My own text named «3c021c3a», five minutes later.

5. **What happens if Sid says nothing.** Each question states the end that holds
   if he says nothing (page:48, page:50). Mine gave defaults without that. Low.

## Does the page write any decision as if it were made?

Yes, one sentence. Page:28 says "The code goes to the base, changed by named
actors." It stands unmarked in the section on what Sid said. His line says what
the base means, not where this repository goes, and "named" is the page's end D
or E, not his word. Elsewhere the page marks its defaults (page:3, page:35,
page:37, page:48, page:50) and its readings (page:11, page:17, page:37).
