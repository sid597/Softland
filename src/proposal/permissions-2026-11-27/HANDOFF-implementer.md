# Handoff: what the first implementer session settled and did not settle

27 November 2026. From the session "permission-fix-implementer", which read
the code for batch 1 of `BUILD.md` and built nothing. For the session that
builds. A session's text throughout: nothing here is a decision of Sid's,
and nothing here starts the work. `BUILD.md` is the prompt and
`PERMISSIONS.md` (P) is its base; this page adds only what those two do not
hold. Where this page and the code differ, the code is right.

"Give" is the page's word, a session's: one permission, as given once. Sid
has never typed it as a noun (checked with `memtool who`), and asked what
it is. To him, say "permission".

## What is Sid's and still open

In his own line to the building session: the go; which goes first, this or
the fixes of the review of 26 November; whether a session goes on from one
batch to the next without a new go. This session's position, given to him:
start this now; keep the fixes out of the same files while it runs; stop
after batch 1 for a review from the diff.

## What was read

P section 12, "What the implementer read in the code", lists it. Since
then also: `read_path/sources.clj` lines 1 to 200 (the sources of a read's
rights and the rule for the rows above), `gate/stamp.clj`, and counts of
where permissions are written in the proofs and above the store. Still not
opened: `read_path.clj`, `read_path/query.clj`, `read_path/answer.clj`,
`read_path/acts.clj` beyond its permission lines, the rest of
`sources.clj`, `door/digest.clj` beyond its permission lines, the two gate
modules, every proof, every file above the store.

## The stored form, as this session would build it

    a give   {:e <give id>  :k :softland.store/give
              :v {:holder     H
                  :allows     #{:see}  or  #{:see :write :give}
                  :covers     [:default-group L]  or  [:act <digest>]
                  :sitting    S           only on a give good for one sitting
                  :given-by   <who>       held to the act's :who
                  :under      <give id>   held to the give the act names
                  :lent-from  <give id>}} none on a lane's first give
    an end   the same place, with no value
    an act   :give <give id>   in place of :permission; none on a lane's
                               making act and on an end of one's own

Why each choice, so that it can be turned:

- **The id is a uuid the giver's program makes, and the give sits on it**
  as a fact at (lane, id, key). A yes is built so today
  (`gate/chain.clj`, `place`; `decision.clj`, `:a-yes-is-given-once`). A
  place takes one give for good. An id made from the act (P's first
  point 1) is not an `:id` a fact can sit on. Every row already holds
  `:by-act` and `:by-index`, so which act made a give is kept either way.
- **One key, not three.** The three keys exist because a holder, a sitting
  and a yes were each the place. With the give's own id as the place, what
  it covers is a value.
- **`:sitting` apart from `:covers`.** Which acts a give is over, and in
  which sitting an act was sent, are two questions; today's sixth slot
  holds one or the other. Day one takes `:sitting` only with the default
  group.
- **`:allows` as a set** with two values taken, today's two. `:see-and-act`
  fuses see, write and give. Batch 2's "what may be given on" may change
  this field again.
- **`:given-by` and `:under` in the value**, though a row's own `:who` and
  the id of the give its act named say the same in batch 1. A change in
  batch 3 writes the row again with the changer's two.
- **`:lent-from` stated**, equal to `:under` in batch 1, so that batch 2's
  "stands on its own" is a value and not a new field.
- **The row's `:permission` column** (`record/tables.clj`, `facts` and
  `facts-by-value`) holds the whole text of the act's permission on every
  row today. It becomes the give's id. `max-act-bytes`
  (`door/declaration.clj`) counts that text once for each fact, and its
  docstring's numbers change.

## What follows in the gate, for batch 1

Traced in `gate/decision.clj` and `gate/chain.clj`; nothing run.

- **Standing.** Read the row at the give the act names. No row: never
  given. A row with no value: ended, or for a yes its use. A row with a
  value: hold the act's who to `:holder`, its session to `:sitting`, its
  digest to `:covers`, then read the rows above. Bug 3 is fixed by the
  place alone: today the word "ended" is given when the place (lane,
  holder) holds the end of another permission.
- **No stamps are compared.** `chain/stands?` compares stamps only because
  a give made again has the same id and a later row. With one give per id
  a give stands when its row and each row above hold a value.
- **An act that names no give** is taken when it is a lane's making act,
  or when every fact of it ends a give whose holder is its who. That is
  bug 4. Today a holder of `:see` is refused before her end is looked at
  (`standing`, `:permission-does-not-allow-acting`). To check: today a
  sitting's permission ends nothing but itself (`end-fault`); an own end
  that names no give cannot say which sitting sent it.
- **Ending someone else's** is as today: the giver, or anyone above the
  giver. Today that compares ids and reads no row (`chain/above?`). With
  ids it reads the rows above the give that is ended.
- **The use of a yes stays a row** the accept writes at the yes's place
  (`use-of-its-yes`). Point 11 of P section 12 is not in batch 1.
- **A second give to a holder is taken.** `give-over-fault` and
  `:holder-has-a-permission` go. `:place-named-once` no longer keeps an
  end and a give for one holder out of one act; this session would add no
  rule to forbid it, and would say so in the report.
- **Who may write a setting** is today "the act's permission has three
  slots". In batch 1 that is "the give has no `:lent-from`". In batch 2 a
  give that stands on its own has none either, so batch 2 or 3 needs
  another test; batch 3 makes it the owner's right.
- **The making act** is today exactly two facts, the depot and the first
  permission (`gate/first_act.clj`, `making-act?`). It stays two: the
  depot, and a give with an id of the maker's and no `:lent-from`. The
  system still makes a personal lane for its person.

## One fork left open on purpose: how the rows above are found

Today an act carries its whole chain, so the gate knows every key above
before it reads one (`chain/places-above`), and a read gets all the rows
above in one round, "since the row names each" (`sources.clj`, 44-51).
With one `:lent-from` id on a give, the rows above are found one at a
time. For the gate that is a loop on one task and costs the same reads.
For a read it is one round for each give above, where the bounds of a read
are 6 rounds asked once and 8 kept live (`sources.clj`, 64-75).

Two roads. (1) Walk it: a read's rounds then grow with the depth of the
chain, and the bound on rounds is set from the bound on depth. (2) The
give's value also states the ids of every give above it, nearest first,
held by the gate against the row above when the give is made. It never
changes in any of the four batches, because no change touches what a give
is lent from. The gate and a read then find the rows above as today, and
the length of the list is the bound on depth, held by the shape again.

This session leans to (2) and did not settle it. It is a stored copy of
what the rows above already say, kept for speed. `CLAUDE.md` puts a stop on
that pattern: examine in a fresh session why it would not be needed before
building it. What the examination should weigh: the copy cannot go stale;
today's form holds the same copy inside the id; the other road changes the
bounds of every read.

## What follows in the read path

- **Her gives by holder.** A read finds her permission today with one row
  at (lane, asker) and one at (lane, her sitting)
  (`sources.clj`, `permission-source`, `run-permission-source`). With any
  number of gives a read needs the run of her gives in a lane, written in
  the same decision as the give. A live read watches a run of keys only on
  a top-level map (`record/tables.clj` says so of `facts-by-value`), so
  this is likely a seventh table in each gate. For the base, a gate copies
  only `:fact` and `:by-value` writes to every task
  (`module/walk.clj`, `copies-of`): the new rows have to be copied too, or
  a read of the base on another task finds no give.
- **The live rule gets simpler, and was only traced.** A live read holds
  rows that arrive apart. Today a row above is opened only after her own
  row is seen (`:read-after`, `sources.clj`, 118-153), and the stamps of
  the two rows are then compared. With one give per place a row goes from
  a value to an end and never back, so a row above that is seen with a
  value after her row was seen held that value when her row was written.
  Batch 3's change breaks "written once": a row then goes from one value
  to another. Not read: how the rounds are run, and what a live read does
  when her run of gives changes.
- **The pushes one live viewer holds** go up: the run of her gives, each
  give's row, each row above. `BUILD.md` asks for the number, measured
  (`test-store/softland/store/read_measures.clj`).

## The proofs

Seven proofs and two measures under `test-store/softland/store/`, about
22,000 lines; `test-store/README.md` says how each is run. The memory note
"Store proof runs and the shell" applies before any run. Permissions are
mentioned about 1,800 times in the proof files and about 1,000 times in
the fixture of the run-again proof. Literal permission facts: 108 in
`pure_proof.clj`, 94 in `module_proof.clj`, 36 in
`read_path_pure_proof.clj`, a few elsewhere; some files have helpers
(`give!`, `given`, `chain-of-permissions`, `deep-permission`).

Two things to hold on to:

- **Three kinds of proof, to be told apart one by one.** One whose
  sentence still holds and whose data changes form. One whose sentence is
  now false (a second give is refused; a give made again is told from the
  first by its stamp; a holder of see cannot end). One that still passes
  and no longer reaches the branch it was written for. Nothing checks a
  proof, so the third kind is found only by reading.
- **The fixture is remade from the new code**
  (`run-again-proof make-fixture`). After that it shows that the store
  comes out the same twice. It no longer shows that the store is right:
  the code that is checked wrote the expected tables.

A cut this session put to Sid, as a proposal: before any source of the
store changes, the proofs and the callers above the store make every
permission through one small set of functions, in today's form. The check
is total: `src-store` is untouched and every proof passes as before. The
change of form then lands in those functions and in the proofs whose
sentence changes. Not tested: whether such functions survive the change of
form, since a give needs an id that its maker keeps. Try it on
`pure_proof.clj` before taking it for all seven.

## Above the store

Six small sites write a permission by hand:
`src-runtime/softland/runtime/sign_in.clj` (lines 59-66, 153, 284-286),
`src-server/softland/server/sign_in.clj` (151-156, 167),
`src-command/softland/command/main.clj` (nine mentions, not read),
`src-server-browser/softland/browser/app.cljc` (two),
`src-runtime/softland/runtime/main.clj` (one). `test-runtime/` has proofs
that name a permission. Not read: whether Auth keeps a permission in a
stored record of its own; if it does, that record's form is Sid's.

## What this touches and leaves

- **The Decisions of 20 and 21 November** in `src-store/README.md`. Batch 1
  makes three lines marked "a session's" false: entry 1, "the end is a
  permission fact with no value, at the holder's place"; entry 2, "a second
  give of the same parts has the same id"; entry 6, "a permission sits on
  what it covers", and its "not built: two permissions for the layer that
  stand at once for one holder". Draft the entry in the report; do not
  write it.
- **Fixes of the review page** whose lines this build passes through: F13
  (`act.clj:245`), F22 (the act-id's depot word), F24, F26, F28
  (`:session` stays on the act), F31 (the yes), F33 and F34 (the read
  path's answers and query words). Say where each is touched, and leave
  it. F29 is batch 4's.

## Where this session expects the build to be hard

By one test: a part is hard when a mistake in it can pass. Size is not it.

1. **Batch 2, telling a person from an agent or a run.** The store cannot
   today: "a gate checks no sender, and cannot tell what the holder of a
   first permission is, a person, a session or an agent"
   (`gate/first_act.clj`). Batch 2's "a give to an agent is always lent,
   and its chain ends at a person" needs a fact the store does not hold.
   Whatever supplies it is a new stored form that reaches the door and
   Auth, and most likely goes back to Sid before batch 2 is built.
2. **The live read**, in batch 1 and again in batches 3 and 4. No proof
   lists the orders in which a live viewer's rows can arrive, and P says
   the present rule rests on a property of Rama that no probe ran.
3. **The proofs and the fixture**, as above.
4. **Batch 3, the check of the whole result.** "The lane still has an
   owner" needs the lane's owner gives known at each decision and the same
   on a second run. Changes that name what the same act makes need the
   act's own rows laid over the rows read before it, where the walk reads
   only rows written before the act.

Long and not hard, each with a proof that fails when it is wrong: the
act's form, the declaration, the digest, the rows, the callers above the
store, and the gate's rules for batch 1, most of which get shorter.

## An order of work for batch 1

The proofs' and callers' one set of functions, if the trial holds. Then
the pure code with its pure proofs: the act's form, the declaration, the
rows, the chain, the decision. Then the walk and the two gate modules with
the module proof. Then the read path with its two proofs. Then the
fixture, the run-again proof and the measures. Then the callers above the
store. A fresh session reviews from the diff
(`.claude/output-styles/build.md`).
