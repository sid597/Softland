# The runtime: what Sid decided, what is open, and the order of the build

**21 November 2026.** This page began as carbon's write-up of the review of
the 20 November build ([PROPOSAL.md](../server-2026-11-20/PROPOSAL.md),
[BUILD.md](../server-2026-11-20/BUILD.md),
[REVIEW-assumptions.md](../server-2026-11-20/REVIEW-assumptions.md)). A
Claude session then held each row against Sid's own words, he answered what
was still open, and he asked for the page to be brought up to that: "update
the doc now and so that the actual build work can start".

Nothing on this page has been run. Three kinds of statement are kept apart
in every entry:

- **Sid's:** what he decided, in his words.
- **A session's:** what a session added, read into his words, or chose. It
  holds only while it serves his decisions, and he can turn it.
- **Not checked:** what the page leans on and nobody has verified. These are
  gathered [below](#not-checked).

## Where his words are

The record (softland-memory) holds what he typed in Claude Code and at the
Antigravity terminal in this folder. It does not hold what he sent to carbon
from the phone on 21 November, or what he typed to neon on the 20th. A
session that checks this page with `memtool who` finds nothing for those
lines. They are in Antigravity's own files under `~/.gemini/antigravity-cli/`.
Two of his turns to carbon were voice notes, known here by carbon's
transcription.

| Marked | Where the words are |
|---|---|
| ‹id› | The record, under that id. |
| (carbon, phone) | Carbon's conversation `5c9b0895-1991-4e1c-adce-4968029b11ac`, 21 November. Not in the record. |
| (neon) | Antigravity's `history.jsonl`, 20 November, 20:32 UTC. Not in the record. |
| (this session) | The Claude session `d5632e19` of 21 November that revised this page. ‹45c7127b› holds five of his answers; his later lines follow it in the record. |

## The rule of the work

Hold the engineering bar: correct with many processes, state in Rama spread
over tasks with no one task that everything passes through, bounds at the
edge before anything is parsed, proofs on a cluster that stays up. Stay open
across what Softland is for: people, fleets of agents, converters, services,
live views defined as data in layers.

Sid (‹45c7127b›): "we are providing spectrums of possibilites and choosing a
single defualt to start with". A session's reading: where an entry below
names one way, it names the default, and the build keeps the rest of the
spectrum buildable.

## Decisions

The letters are the ones on the sheets he answered, so that an entry can be
held against what he ticked.

1. **The program is called `runtime`** (`src-runtime`, `softland.runtime.*`).
   Sid decided: "As for naming lets go with runtime that maps to me" (carbon,
   phone). It turns the folder name `server` of 20 November.

2. **Nobody acts for another (0a = A).** The `:who` of an act is the
   signed-in maker that sends it, always. An author from outside Softland is
   a fact inside the act. Sid decided, of an importer that brings in Alice's
   note: "so 1-A it is" (neon). A session's: the runtime holds the act's
   `:who` against the caller and refuses a difference; the acts of another
   deployment would arrive as that deployment's own signed line, which is not
   designed.

3. **What a session read is written down, apart from `:based-on` (0b = B).**
   `:based-on` holds only the facts that an act rests on. What one sitting of
   a person, or one run of an agent, read is kept as a list of its own, in
   order: the trail. Sid decided: "for 2. I think its B", with "general
   scrolling around is recorded as reading not based on" (neon); and "yes to
   the general idea of reads are writtien down as well" (‹45c7127b›).
   A session's, which he was shown and answered "yes got it right" (this
   session):
   - The runtime writes the trail, because it is the one that serves each
     read. A client cannot be relied on to, and an agent on MCP has no client
     of ours. This is what keeps "a model's reads, always"
     (`src-store/README.md`, 12 October, entry 4).
   - The runtime also writes into the trail each act that the session sent.
     Every act already names its `:session`, so "everything read before this
     act" is a lookup, for any act.
   - The stored act gets no new part. Carbon's `:read-seq` is taken off.

4. **Movement is kept.** The pointer and the camera, as the client samples
   them, are kept in the same trail. Sid decided: "yes we keep it"; and of
   the rate, that the number 10 "is just a heuristic and it can change what
   it will be can be decided at the product layer" (‹45c7127b›). The client
   samples so that the wire is not hammered. This turns carbon's row, which
   let movement expire.

5. **Where state lives (1 = 1A).** The store stays what it is: two depots of
   acts, never trimmed, no clock in a gate, the same tables when it is run
   again. State that makes another promise about time lives in modules of its
   own beside it. Sid decided. He typed "1B" (‹93a88844›) and reopened it by
   voice; in carbon's transcription: "it seemed like 1A made more sense ...
   not just because I said something you have to fold". He asked whether
   separate modules sit on the same machine as the store's data, and then
   typed: "We agree on 1,4,5,6,7" (carbon, phone). A session's:
   - The ground is the promise about time, not the partition key. The store
     is kept for ever and can be run again; a sign-in link is kept and can be
     erased; a trail is kept and may one day be thinned. Partition keys show
     only that the split costs no hop: a person, an account and a session
     land on other tasks than a layer does, inside one module too.
   - The modules are Store, Auth and Trails. Carbon's page had a fourth,
     Signals, for movement that expires. With entry 4 that reason is gone,
     and this page drops it: "who is here now" is a table built from the
     trail's log. This is this page's choice, not designed, and his to turn.
   - A trail and "who is here now" are read through the same `read` and
     `watch` words as a layer, with the same meaning of "may see". Not
     designed, and it is the hard part of this entry.
   - His line of 12 October fits it: a group that needs other handling "might
     have a seperate depot" (`src-store/README.md`, 12 October, entry 2). A
     trail in its own module can be kept, thinned or trimmed without touching
     the store's promise.

6. **Permissions (2 = 2A, and his answers in this session).**
   - **A permission can be ended,** by its giver or by anyone above it, up to
     the layer's root holder. Everything given beneath it ends with it. Sid
     decided: "2A" (‹93a88844›).
   - **After an end, the holder can be given a permission again.** Those
     beneath do not come back by themselves; each is given again. Sid
     decided: "yess ... I think we should be able to give the permissions
     again its never revoking once for all" (‹45c7127b›). It holds for every
     holder, a person, a program or an agent.
   - **Only the layer's root holder writes its settings:** who may see it,
     what it builds on, the keys it marks. Sid decided: "2A". Until now any
     holder who may act could (`src-store/MODEL.md`, "Not decided, and not
     built").
   - **The layer is not the smallest thing a permission covers.** Sid
     decided: "so no it is not the smallest unit ... each act is the smallest
     atomic unit right and we can have permission over that"; a layer is "a
     term or concept that i use to define arbitrary boundary over any set of
     context a human can define as much as they want" (‹45c7127b›). His
     cases: a person's sign-in acts, held by the system or by her alone; an
     exam or a benchmark, where "we tell them answer eixists but we are
     testing you". This turns carbon's "layer-wide in this pass".
   - **How much one yes covers is a spectrum.** Sid: "the spectrum of
     everytime to always allow and that can be per run of some
     task/query/agent" (‹45c7127b›). A session's reading: a permission says
     what it covers, from one act up to a layer, and how long it stands, for
     one act, for one run, or until it is ended. The default is the whole
     layer, until ended.
   - **A thing is hidden by its boundary.** It sits in a boundary that the
     reader does not hold; it is not cut out of one that she holds. Sid
     decided: "yes to boundary" (this session). He also asked "do we have to
     answer this now?", and said that the field may already have worked out
     such queries. So an exception inside a boundary is not ruled out for
     good: the field's work is read before the finer permissions are built.
   - **Clearance is matched at the top.** Readers with the same clearance see
     the same thing, and a holder of the whole layer is not checked act by
     act. Sid's lean, put as a question: "we only need to match the top ones
     or no???" (‹45c7127b›). A session agrees: a view is one per clearance,
     and a gate makes one check per act, since an act names the permission
     that it writes under.

7. **The whole act goes on the wire (3 = 3A).** Sid decided: "3A (i think
   need more)" (‹93a88844›); carbon then unpacked it, and he did not contest
   it. As carbon wrote it:
   - The maker sends the store's own act, with `:who`, `:session` and an
     act-id that the maker made. The runtime holds `:who` and `:session`
     against the caller; it does not strip them and put its own in.
   - A write that a person waits on is one round trip, send and wait. An
     import sends many.
   - `:session` names one sitting or one run. It stays the same when the
     short secret beneath it is replaced, so a repeat of an act after a break
     has the same digest.

8. **An agent has its own who, and its own layer by default (4 = 4A).** Each
   program, command line, agent or service that attaches has a `:who` of its
   own. Attaching an agent makes it a branch layer that builds on the layer
   it works on, seen live through the stack, with a `promote` that brings its
   facts into that layer. A permission straight in the shared layer stays
   possible. Sid decided, by what can be turned into what (separate layers
   can be merged; one mixed layer cannot be cleanly parted): "We agree on
   1,4,5,6,7" (carbon, phone). A session's, not asked of him: that "a model
   drives this run" is said of the session and is not a kind of who.

9. **Refused acts can be read (5.2 = B-safe).** The store gains a read of a
   layer's acts by stamp, each whole and with its answer. A refused act can
   be read by its own who and by whoever holds a permission in the layer. A
   visitor of a layer that everyone may see cannot, unless the layer turns it
   on. Sid typed "5.2 B I think" (‹93a88844›), then agreed to carbon's
   narrowing of it (carbon, phone). The reason for the narrowing: anyone can
   send an act that will be refused, and under plain B every visitor of an
   open layer could read it for ever.

10. **The runtime speaks MCP over HTTP; the command line is Clojure
    (6 = 6A).** A harness is given one address and signs in through the
    browser once; nothing is installed. The command line and its library
    share the `.cljc` forms with the runtime and keep exact numbers. It turns
    the Node program of 20 November. Sid: "btw i dont know anythign about
    this" (‹93a88844›), then "We agree on 1,4,5,6,7" (carbon, phone). His yes
    rests on carbon's account of it; see [Not checked](#not-checked).

11. **Reactive from Rama to the screen (7).** `read_path.clj` stays: it is
    already driven by Rama's pushes. From the runtime to the client the
    changes travel as Missionary flows over plain data, many watches on one
    connection, to WebGPU and to views that are data. Sid decided: "we can
    have electric structures as well if and where they directly fit but we
    not forcing" (carbon, phone).

12. **Before real data is kept (8a, 8b, 8c).**
    - **No signature at the door yet (8a = A).** Rama's ports are kept from
      other programs, as code. Sid decided: "8a-A" (carbon, phone). It keeps
      his ruling of 19 November. A session's note: carbon and the Claude
      session of 20 November both leaned the other way, because a signature
      cannot be added to acts that are already kept.
    - **What Google gives at sign-in is kept, and she is called what she
      confirms (8b).** At her first sign-in Google's name for her is shown in
      a box; she confirms it or changes it. Sid decided: "I pick C ... we can
      make C default", and "writieng in the log gives us all the 3 cases"
      (‹45c7127b›). He called it a product question, and it is. A session's:
      Google's number and her email sit on the link in Auth, which can be
      erased (9 October, entry 1).
    - **Sign-up is by invitation or a list (8c = A).** Sid decided: "8c-A for
      now but will move to B when this projects picks up" (carbon, phone).

## The picture

A session's picture. The boxes are named, not designed.

```text
  browser (WebGPU)            command line (Clojure)       agents' harnesses
  Missionary over data;       the same .cljc forms;        MCP over HTTP; a who
  Electric where it fits      sign-in by device grant      of its own, a session per run
            \                           |                          /
             +--------------------------+-------------------------+
                                        v
  RUNTIME   JVM copies that hold no state of their own
    edge       bounds before anything is parsed; origin checks; pacing
    standing   who is calling: the person at the root, the actor, the session
    one connection, many watches
      acts     the whole act with the maker's act-id; send and wait; send many
      reads    changes to places as they happen; a layer's acts by stamp
      trail    what a session read, sent and moved through; who is here now
                                        v
  RAMA
    Store    by layer     two depots of acts, never trimmed, no clock in a gate
    Auth     by account   [provider subject] -> person; a link that can be erased
             by person    standing; counts for pacing; sign-ins under way
    Trails   by session   reads served, acts sent, sampled movement, one order
             by layer     who is here now, a table built from the same log
```

Kept from carbon's page, as a session's:

- Standing and the counts for pacing are both held by the person at the
  root, in place of the one ring of sessions that every sign-in passed
  through on 20 November. Standing is watched; the counts are not.
- A program's secret is never taken as a browser's cookie. A program that
  makes another can give it only less than it holds.

## Not settled

Each of these is Sid's. Before the slice that needs it, it is laid out for
him the way he asked on 21 November: the situation in a Softland example, the
options, what each leads to, and a pick.

**With the first slice (ending a permission):**
- What the record of an end is, and how a permission that is given again is
  told from the one that ended. A permission's id is made of its parts
  (`softland/store/act.clj`): holder, layer, the layer it was given in, the
  permission above, what it allows. A second give of the same parts has the
  same id as the ended one.
- The word of the refusal, for an act sent under an ended permission.
- Whether a holder can end her own permission.
- Who is the root holder of the base and of the system's layers. "Who may
  act as the system" has been open since 19 November.
- How deep a permission can be (already "not decided" in `MODEL.md`).

**With sign-in (the second slice):**
- The choices on [SIGN-IN-WRITES.md](../server-2026-11-20/SIGN-IN-WRITES.md)
  that the entries above do not answer. He has not seen that page. They are:
  how she learns where her own layer is, the words, and whether the base is
  made at the runtime's start.
- How he comes to hold rights in the system's layers.
- A first sign-in, and the making of an agent, write in two modules: the link
  in Auth and her acts in the store. What holds when it breaks between them.

**With the trail:**
- Who may read a session's trail.
- For a live read, whether each change that is pushed is written, or only the
  opening and the close.
- What a person's "turn it off" stops (12 October, entry 4).

**With `promote`:**
- What it writes. With entry 2 the who of a promoted fact is whoever
  promotes; where the agent's authorship and its own act go is not said.
- A branch larger than one act can hold cannot arrive in one act.
- `:based-on` is kept and no gate reads it (`src-store/README.md`, 6 October,
  entry 12), so it gives a merge no safety. What safety there is comes from
  `:replaces`.

**With finer permissions:**
- The form of "this one act" and "this one run" in a permission. The id grew
  once before by a slot at its end, `:see` or `:see-and-act` (11 and 12
  October), and older permissions kept their meaning.
- A permission to see that is sized by a query.
- Boundary or exception, after the field's work is read (entry 6).

**With MCP:** the step where a person says what an attaching agent gets,
since that is where the branch layer or the shared layer is chosen.

## Not checked

- **`pstate.proxy.limit`.** What it bounds is open
  (`src-store/OPEN-QUESTIONS-rama.md`, section 4). Standing puts one live
  read for each active person on each runtime copy, and the read path shares
  the same bound.
- **A pushed change over MCP.** Whether Claude Code or Codex hand it to the
  model.
- **Sign-in for MCP over HTTP.** "OAuth 2.1" makes the runtime an
  authorization server, or needs one in front of it. Its size is not known.
- **The act-id that a maker makes.** Carbon read `door.clj` as taking it
  already. This page did not read it.
- **A view over very many small layers.** With entry 6, small layers are how
  things are hidden. What such a read costs the read path is not known.
- **Rama's trim.** It keeps the newest N records of each partition, and the
  default is to keep all (the Rama skill's depot reference). Whether it can
  be set for one depot inside a module was not checked; a trail in its own
  module does not need the answer.
- **The start of a Clojure command line.** How long the JVM takes, and what
  that means for a tool run many times a minute.

## The order of the build

The order is a session's, and his to turn. Carbon's page built everything
and proved it last. The faults of 20 November were in the joins, so this
order proves each join when it is made:

- A slice lands whole, with its explanations and its proofs, before the next
  starts.
- A property is proved in the slice that owns it. Where it is about
  processes, it is proved on a cluster that stays up, with processes killed
  during the run.
- The narrowest path through every part comes before any part is widened.

1. **The store: ending a permission, giving again, root-only settings.**
   Entry 6, its first three points. [Below](#the-first-slice-to-start-from).
2. **The spine.** Auth (an account to a person, standing), the runtime's
   seam (the whole act, send and wait, one watch), and the Clojure command
   line with sign-in. Proved with two runtime processes on a cluster that
   stays up, under kills: one person from two first sign-ins at once, an act
   repeated across a break, a watch that closes when its permission ends.
   An agent can already work through the command line.
3. **Then, one at a time, in an order still to be chosen:**
   - permissions over one act and over one run;
   - the read of a layer's acts and refusals by stamp;
   - the trail: reads, acts sent, movement, and who is here now;
   - MCP over HTTP, with its sign-in;
   - `promote`;
   - the browser's client.

The trees of 20 November (`src-server`, `src-cli`, `src-server-browser`) stay
where they are until the runtime passes its proofs. Nothing moves to
`history/` unless Sid names it. The session that builds the spine first lists
which of 20 November's checks are still true statements under this page, and
carries those.

## The first slice, to start from

For the session that builds it. It starts with the short message that
`.claude/output-styles/build.md` asks for, and waits for his go.

**What he asked for.** "2A" to carbon's sheet: a permission is ended by its
giver or by anyone above, and what was given beneath ends with it; only the
root holder writes a layer's settings. And "yess" to giving again after an
end. He says "revoking" for it in passing; the store's own page says that
nothing "ends" a permission. Which word the product uses is his.

**What it adds to the store.** Three rules:

- an end, with its cascade;
- a give after an end;
- the root holder alone writes settings.

**What follows from them,** as a session reads it:

- Acts that were taken under a permission stay taken after it ends. The log
  is kept whole.
- An act sent after the end, by the holder or by anyone beneath her, is
  refused.
- A live read closes when its holder's permission ends, or one above it.
- An end is an act with a stamp in its layer, so the store comes out the same
  when it is run again.
- Today a holder has one permission in a layer. After this slice she can have
  had more than one over time. Whether she can hold two that stand at once is
  not asked here, and stays as it is: one.

**What comes to him inside the slice:** the five points under
[Not settled](#not-settled), the first of them before any record is kept.

**Where to read:** `src-store/CLAUDE.md` first; `src-store/README.md`, the
decisions of 8 October and of 11 and 12 October; `src-store/MODEL.md`, the
walk through an act and "Not decided, and not built"; in the code,
`gate/decision.clj` (`standing`, `who-may-write-fault`), `act.clj` (the forms
of a permission's id), `gate/first_act.clj`, and the read path where a
permission is watched. The Rama skill before any Rama code, and the memory
note on the store's proof runs before any proof.

**Proofs:** as the store's rules have them today: pure ones, the gate and the
read on a cluster, and the run-again proof; and one more, that a live read
closes when a permission above its holder ends.
