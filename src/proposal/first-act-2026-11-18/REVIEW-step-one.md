# Review of step one of the store's first who, 18 November 2026

## The outcome, 19 November 2026

Sid decided that nothing in the store holds a key, and step one was taken off
`main` by `3873f78b` ([PROGRESS.md](PROGRESS.md) has his words and the
grounds). The review below stays as the record of what a fresh session found
in commit `8ba0da10`. What its findings mean now, in the view of the session
that took the code off:

- **These go with the code:** findings 1 and 3, the weak public keys, the
  rule for a resend that carries a signature, the three proofs that pass for
  the wrong reason, the cost of a signed check, and the three keys with a
  backup. Each exists only where an act is signed.
- **Finding 2 is the store as it is meant.** Any program that can send can
  name `:system`, and such a program is trusted with the whole store (the
  decision of 12 October, entry 4). That `system` can make a layer for any
  holder is how the system makes a person's layer at her first sign-in. Who
  may act as the system stays open, for the sign-in build.
- **One thing stays from finding 4,** before the first record is kept: how a
  changed rule counts when the gates are run again over acts that were kept
  before it. It no longer rests on a sender who plants acts ahead of a rule:
  under the decision of 12 October no program outside Sid's machines can
  send.
- **"Reads", under "Not on the list", stays as it was:** who asks is what the
  caller says. The server is to hold that at sign-in, as it does for an act.
- **Two lines of "Sid's answers, 18 November" are superseded:** that the
  sign-in server signs a person's acts, and the single backup key. "People
  hold no keys" stands, and now holds for the system and the server too.

## Sid's question, which the outcome answers

The review session ended on this question, and the next session began with
it. Sid, as he typed it:

> why are you adament on having a key? is it not provided by google some sort
> of id and then in the store we check each event for who wrote it and if that
> user is first of all registered and is able to write where he wants to write.

It questions one line of "Sid's answers, 18 November" at the end of this page:
that the sign-in server signs a person's acts. Everything on this page about a
signature on a person's act waited on it.

Where the conversation stood when the review session ended. This is the
reviewer's view, not Sid's words:

- **Sid's model works with what is already decided.**
  - Google proves who a person is at sign-in, and the sign-in server links her
    Google account to her fixed id (9 October, entry 1).
  - The server then sends her acts, and the store checks each one: is the
    person registered, and may she write here.
  - The store already checks the second part: that is what a permission is.
- **What makes "this act is Sid's" true is 12 October, entry 4:** "Only
  programs on machines that Sid runs may reach Rama's ports", and each such
  program is "trusted with the whole store, in any person's name". Under that
  rule, a person needs no key and her acts need no signature.
- **A signature would only be a hardening,** and it would be the server's, not
  the person's. It would guard against a program that can reach the ports but
  should not write as anyone, such as Sid's own agents on his machines.
- **The store cannot check Google itself on each act.** Google's sign-in token
  lasts about an hour, is not tied to what is written, and a gate reads no
  clock. So the Google check happens once, at sign-in, on the server.
- **The signed first act still has a use for the system.** Another deployment
  that takes Sid's system layer can check that the layer is his, which a
  network rule cannot do.
- **To weigh next:** this review assumed that anything can send an act, as the
  store's own docstrings say. Under entry 4, once real data is kept, only
  Sid's programs can. What each finding means under that rule is the next
  session's question.

## About this page

A review of commit `8ba0da10`, "Store: a signed act, and a store's first who".
A fresh session did it, and its job was to break the build. It is working
material and no decision page. The findings below are as the session reported
them to Sid. The sections added afterwards are marked.

**How it was made.** The session read the diff and the code it touches first,
and formed the findings from that. It read `STEP-ONE-STATE.md` and
`HANDOFF.md` only after. "Ran" marks a finding that a run showed; "derived" one
read from the code. One subagent ran, as one batch, the four proofs that the
commit names and the session's probe. They ran against an export of the tree
at `8ba0da10` (`git archive`), so that edits in the shared working tree could
not reach the run. All four proofs pass with the builder's counts: 205, 766,
23 and 46.

**The probe and its logs** are in
`/mnt/data/projects/research/softland/first-act-review-2026-11-18/`:

- `probe/review_probe.clj`: the probe. Pure checks first (P1 to P3), then two
  in-process clusters: store one (S1), and a second store with nothing in it
  (S2). It prints PROBE lines and never fails on a mismatch.
- `run_batch.sh`: the batch, one step per call. Its paths point at the scratch
  folder of the session, where it ran.
- `logs/`: each step's log and exit code. `summary.txt` is the batch's
  summary, with every PROBE line.

To run the probe again, export the tree with
`git archive 8ba0da10 deps.edn src-store test-store | tar -x -C <dir>`. Then,
from `<dir>`, run
`clojure -M:store-module -i /mnt/data/projects/research/softland/first-act-review-2026-11-18/probe/review_probe.clj`.

**Words.** "Signing who" is the builder's word: a who whose id holds a public
key, `:who/ed25519.<64 hex>`. The gate checks a signed act's signature against
the key in its who and reads no row for it. Sign-in plays no part.

**People hold no keys** (Sid, 18 November). Only the system, and the server
programs it trusts, hold keys. A person signs in with Google or another
account, and the sign-in server signs her acts for her. The last section says
what follows for the later steps.

## The claims the review was asked to break

1. At most one first act of the base is ever accepted.
2. No act is accepted under a who that holds a public half without a signature
   made with that who's private half.
3. In a layer that a signed act made, every accepted act is signed.
4. A signed act is good under one act-id in one layer and nowhere else.
5. Sending the kept first act again changes no row.
6. Nothing that was kept before this build is decided differently now: the
   run-again fixture comes out the same.
7. Each page and docstring says what the code does.

## The claims, one line each

1. Holds, within one store.
2. Breaks two ways: through the id `:system`, and through a weak public key.
3. Holds as stated, with two caveats.
4. Breaks across stores.
5. Holds.
6. Holds for the fixture, but only because of what the fixture happens to
   contain. No mechanism guarantees it.
7. Breaks: docstrings and pages in five places, and three proof sentences, say
   more than the code does.

## 1. A signed act is accepted in any store where its who holds the same permission (breaks claim 4). Ran.

- **What holds:** within one store, an act-id gets one answer. The act-id,
  layer and depot are all inside what is signed, so a copy under another
  act-id, or with any part changed, is refused.
- **What breaks:** nothing signed names the store. A first permission is
  `[who layer layer]`, the same in every store whose layer that who made.
  - The kept first act is built to make "the base for the same who" in any
    empty store (`src-store/softland/store/setup.clj:49`), and the setup proof
    does exactly that.
  - From then on, any signed act of the first store can be replayed into the
    second, in whatever subset and order the replayer picks. That includes
    acts by anyone the first who gave a permission to.
- **The acts:**
  - Store 1: W makes the base. W gives B see-and-act, and B writes. W writes
    `x="one"`, then replaces it with `"two"`. A stale replace of `"one"` with
    `"three"` is refused `:replaced-fact-not-current`.
  - Store 2, empty: the kept first act is accepted at stamp 0. The give to B
    and B's act are replayed, each under its own act-id: both accepted. Then
    `"one"` and the refused `"three"` are replayed: both accepted.
  - So an act W saw refused took effect elsewhere. x reads "three" there and
    "two" at home.
- **After data loss:** a store that lost its rows is the reason the file is
  kept. Re-making that base reopens every old signed act to anyone who holds a
  copy, and every value on a depot is plain to whoever can read it.
- **The handoff's premise doesn't hold here.** It left the store out of the
  signed bytes because "each store's root has its own key". The build's own
  setup and proof put one key on two stores. Sid's question at 10:53 UTC, about
  reusing his ssh key, points the same way.
- **View:** Sid's picture of deployments subscribing to his system layer wants
  signatures that are good across stores. So the fix isn't "never across
  stores". What's missing is a binding the receiving store controls: the
  base's first act-id inside what is signed, or the act's place in the line.
  Which one is Sid's call.
- `signature.clj:43` and the commit say "good under one act-id in one layer".
  That is true per store only.

## 2. Anyone can still act as `:system` and make a layer for any holder; the real first who can't (breaks claims 2 and 7). Ran.

- **What holds:** an unsigned act can't name a signing who as its own `:who`,
  and can't act at all in a layer that a signed act made.
- **What breaks:**
  - The unsigned branch of `making-act?` still takes `:who :system` with any
    holder (`src-store/softland/store/gate/first_act.clj:145`).
  - The new version-1 rule checks only the act's own `:who`
    (`src-store/softland/store/act.clj:644`), not the holder of the
    permission it names. So an unsigned act is accepted under a signing who's
    first permission, with nobody's signature, and writes a permission row on
    that who.
  - The signed branch requires who = holder. So W, the store's actual system,
    can't make a layer for anyone else, but an id anyone can name can.
- **The acts,** in a store whose base W made:
  - An unsigned `:system` act makes `:held-by-w2` with W2 as first holder:
    accepted. W2 then acts there, signed: accepted.
  - W, signed, makes a layer for W2: refused `:layer-not-made`.
  - Unsigned `:system`, same layer id: accepted.
  - Unsigned `:system`, a layer whose first holder is W itself: accepted.
- **Pages that say the opposite:**
  - `src-store/softland/store/door.clj:118`: "the system is the signing who of
    that act … the gate holds its signature".
  - `src-store/softland/store/gate/first_act.clj:39`: "no id that the code
    knows names it".
  - The store README's "with an unsigned act for any holder" matches the code,
    so the pages also disagree with each other.
- **Harm:** anyone can make layers in any signing who's name, squat the ids
  the system would make at a person's first sign-in, and put W down as first
  holder of layers W never made. They can't act in those layers afterwards.
- **Direction:** in a store whose base a signed act made, drop the `:system`
  branch, or require the first who's signature on it. Personal layers then
  need a signed form of "one who makes a layer for another".

## 3. The identity point is a who that every sender can be (breaks claims 2 and 7). Ran.

- **What holds:** for a pair the JVM made, a fitting signature needs the
  private key. The JDK refuses the reshaped S + L form of a real signature.
- **What breaks:** `:who/ed25519.` followed by `01` and 62 zeros is the
  identity point. The pattern takes it as a signing who, and the JDK takes it
  as a public key. The signature `01` followed by 126 zeros fits every
  message, and no Ed25519 private key exists for that point.
- **The acts:** W gives that who see-and-act in the base, then a sender with no
  key acts as it: accepted. A streaming layer made and then acted in by that
  who, with the same fixed signature: both accepted.
- **Stated without the exception:** `signature.clj:54` admits weak points. But
  `src-store/softland/store/door/declaration.clj:779` and
  `src-store/MODEL.md:256` say no sender without the private key is accepted,
  with no exception.
- **"Nobody gets such a who without choosing it":** the one who chooses
  needn't be the one who gets hurt. Hand W this key and W's give works for
  everyone.
- **Why it ranks below 2:** it needs that one choice, and it's cheap to close
  by refusing the short known list of small-order encodings in
  `public-half-hex`.

## 4. Claim 6 holds by content, and "signed" is written as "version 2" in three places. Derived; run-again 46/0.

- **What holds:** the fixture's two version-2 acts name `:alice` and still get
  `:unknown-version`. No fixture act names a who in the `who` namespace (a
  grep of the fixture).
- **It rests on what depots happen to hold.** Anyone can append anything. Take
  a thing sent before this build with version 2, a `:who/ed25519.…` who and a
  valid signature. The old code refused it `:unknown-part`; this code accepts
  it on a run-again. Every later edition needs some feature no kept record
  has. Once a store is kept and open, a sender can plant records ahead of any
  edition that seems likely.
- **The version-1 refusal changed version 1 in place.** Decision 6 allows that
  only until the first real record.
- **Three places test `(= 2 (:version act))`:** `act.clj:644`,
  `declaration.clj:597` and `first_act.clj:143`. Add a signed version 3 and
  miss the second, and v3 acts can give permissions to plain ids inside a
  signed layer. Claim 3 would then fail silently.
- **Direction:** test "signed" once, from the edition. Make "an edition begins
  to count" an act in the line, so a run-again decides each thing under the
  editions in force at its place.

## Proofs that pass for the wrong reason

- `test-store/softland/store/setup_proof.clj:282`, "a store holds only what
  its own line gave". It passes only because the proof never replays store 1's
  line into store 2. Finding 1 does, and the replay is accepted.
- `test-store/softland/store/pure_proof.clj:4041`, "each thing that the gate
  accepted is a signed act whose signature fits its who". It checks with the
  same `fits?` the gate used, so it grades itself. None of its forgeries uses
  a weak who.
- `test-store/softland/store/setup_proof.clj:181` gives the reason as "the
  base's first permission takes one write". The word it actually checks,
  `:permission-fact-not-beneath-the-acts-own`, comes from check (g), not from
  that rule.

## Claims 1, 3 and 5: why they hold

- **Claim 1:**
  - Only the microbatch gate can make `:base`, and the streaming gate refuses
    base acts by layer id.
  - Every base act is on one partition, decided one at a time by a walk that
    never yields.
  - A second making act finds the depot row or its own permission row and is
    refused.
  - This is per store: the same first act becomes the first act of every store
    it reaches.
- **Claim 3, by induction:**
  - The first holder is a signing who.
  - A give sits straight beneath the giver's own permission, in the same
    layer, and a signed give goes only to a signing who.
  - Standing requires the act's who to hold its permission, and an unsigned act
    can't name a signing who.
  - Caveats: "signed" includes the identity point (finding 3). And a layer
    that `:system` made for a signing who has the same permission rows as one
    that who made, so a reader can't tell the two apart (finding 2).
- **Claim 5:** an act-id that already has an answer writes nothing, and the
  second send has the same digest. The pure proof compares the whole record
  before and after.

## Not on the list

- **What an unsigned act can still do** in a store that holds signed layers:
  - squat any unmade layer (already true before this build);
  - make layers for any holder via `:system` (finding 2);
  - give a signing who an unasked permission in an unsigned layer;
  - make an empty store's base first, which the setup detects by reading its
    answer.
- **Reads:** the read path takes the asker's who as given
  (`src-store/softland/store/read_path.clj:262`). So a members-only signed
  layer is readable by anyone who names one of its whos. It's documented as
  room for sign-in, but "made by a signed act" is easy to misread as
  "protected".
- **Cost:** by the builder's own number, a signed check takes 0.44 ms against
  0.016 ms unsigned. It runs before any row is read and holds the layer's
  task, so a thing with a bad signature costs that much without any
  permission. For the base, all of that lands on its one task.
- **The digest includes the signature:** a keeper that signs
  non-deterministically would make a re-signed resend under the same act-id
  read `:not-mine`. The current keeper and the kept file don't do this.

## Whose call

The handoff said to bring Sid two things before building: "any rule for a
layer other than the base" and "anything that changes what an act of version 1
means". Both are built: signed making acts for any layer, and the version-1
refusal. In the record, the version-1 concern was raised as a view at 10:22
UTC. Both were listed after the commit as "all yours to turn", and Sid's reply
was "how many steps are there". So both stand on the builder's word, and
nothing kept depends on them yet.

## What the review did not look at

- The door, module and read-path proofs were not run; the review took the
  builder's counts for those.
- No kept or restarted cluster, and no two first acts arriving at the same
  moment. Claim 1 rests on the partition-and-walk argument.
- No small-order point other than the identity point, no non-canonical
  encodings and no mixed-order keys. Nor whether a future JDK might decide a
  crafted kept act differently.
- Of the pure proof's 766 cases, only the new sections were read.
- What a 77-character who does to row keys beyond the id limit.

## After the review (added when this page was written)

The text above is as reported. Two of its directions do not work as written:

- **Finding 2's direction** ("in a store whose base a signed act made, drop
  the `:system` branch, or require the first who's signature on it") cannot
  be a rule of a gate. A gate reads only its own layer, so the gate of another
  layer cannot know how the base was made. That is the builder's objection,
  and the session's own answer to Sid the same day. The rule has to be one of
  the code that the store runs: it takes no version 1 at all.
- **Finding 1's direction** ("the base's first act-id inside what is signed")
  does not tell a store restored from the kept first act from the original.
  That is the builder's objection. Beyond it, no gate but the base's can know
  the base's first act-id.

## The reviewer's response to the plan, corrected after a second read

The plan came from the building session's list («7f6624d3», «fb9a21c9»),
which Sid forwarded. That list had one more item, marked as Sid's to decide
now, which the forwarded copy left out: whether the who is the public key.
Sid's answer is in the last section.

The reviewer answered the plan with seven points. The builder accepted all
seven in its own session («0de708af»), including two remedies that turned out
to be wrong. A second read then judged the seven against the code at
`8ba0da10`: Fable, at max effort, read only. It agreed with all seven in
direction and corrected four. The points below are as corrected; each
correction is marked.

The frame: the plan's reason holds for today, since nothing is kept and no
user exists. The points disagree where steps 2 to 5, which are built before
step 6, could lean on these gaps.

1. **A person's layer, before step 3.** Step 3 has the system make each
   person's layer at first sign-in. Today only the unsigned `:system` branch
   lets one who make a layer for another. A signed form of that job is no
   fix as it stands. The gate of a new layer has no rows and cannot read the
   base, so it cannot know who the system is. Any signer could make any who
   the first holder: finding 2 again, with a signature on it.

   With Sid's answer that people hold no keys, the question before step 3 is
   how every gate knows which key is the sign-in server's, the key that acts
   for signed-in people. That includes the gate of a new layer. It is the
   same wall as finding 2. Whatever names the server's key has to be
   something each gate can know without reading the base. It can be its own
   layer's rows, which a new layer does not have yet, or the code that the
   store runs (Sid: "The root user is basically the code base"). The same
   choice decides what replacing the server's key costs. Open; this is the
   reviewer's view.

   *Corrected:* the first version asked for "a signed way to make a person's
   layer by step 3", which the builder accepted. It fails for the reason
   above. A second version weighed a person's own key against a fixed id,
   and Sid's answer settled it: a person is a fixed id and holds no key.

2. **Replay and restore.** Both suggested fixes fail as the builder says, and
   no gate but the base's can know the base's first act-id.
   - **The chain per who doesn't close finding 1.** A gate can check "the act
     before it by the same who" only inside its own layer. And order per who
     is not the layer's order. W writes x, B replaces it, and W's stale
     replace is refused. In a restored store, a replayer sends both of W's
     acts before B's: every chain is intact, and W's replace is accepted.
   - **A chain per layer** could be checked with one row, since the walk
     reads the layer's last stamp. But a refusal takes a stamp too, so any
     refused thing voids every act signed against the head, and of two
     writers only one lands.

   So:
   - Restore a lost store from a copy of its depots, never by re-sending
     acts.
   - This protects only acts the copy holds. Acts sent after the copy can
     still be replayed in any subset and order, and a permission ended after
     the copy is lost. On one machine every loss is such a restore. Only not
     losing the log protects that tail: the question of one machine against
     three, open for step 6.
   - A store with no copy to restore from is a new store, with a new first
     act.
   - "Is this my store" becomes a read of the kept act-id's answer, not a
     re-send (the builder's addition). `send-again!` answers `[:accepted 0]`
     both in the store that holds the act and in an empty one. No code does
     what `setup.clj:49-51` says the file does: "tells its holder which of
     the two she has".
   - One key in two different stores still needs the store named in what is
     signed. A gate outside the base can know the store only from the code
     it runs, and that matters for a making act alone. Any other act can be
     tied to its layer's making act through its permission row, which holds
     the act that wrote it (`record/rows.clj:246-248`). Decide at step 6,
     before the first record.

   *Corrected:* the first version said that after a restore from the depots,
   every replay hits an act-id with an answer. That holds only for acts the
   copy holds.

3. **Weak keys.** Refuse all eight small-order points, in every encoding, as
   the store's own rule in `act/public-half-hex`. Seven values, compared with
   the top bit of the last byte masked, cover all fourteen encodings.
   - The JDK takes the eight standard encodings and refuses the six others.
     That is Fable's reading of the JDK 24 source; the store runs 21, taken
     on trust to decode the same.
   - All eight can be signed for with no private key. The probe's fixed
     signature fits about one message in 2, 4 or 8 for the other seven, and
     a sender picks her own act-id, so she can try until one fits.
   - The probe tests the identity point only, so a check built from it alone
     would pass a one-key fix.
   - `fits?` "gives the same answer whenever they are asked"
     (`signature.clj:157-158`) holds for one JDK and provider. From the first
     record, what the verifier accepts is a rule of version 2. That goes on
     step 6's list.

4. **The second road.** It cannot be a second verifier: a direct JDK verify
   accepts the identity-point forgery just as `fits?` does. Record the thing
   each keeper was asked to sign, in the proof's `signed` helper. The check is
   that each accepted thing, without its signature and time, is in the record
   under its own who. Compare things, call no function of the store, and send
   at least one act under a who that has no keeper.

   *Corrected:* the builder's wording, "every accepted act's signature must
   be in the record", is too weak. It passes a real signature moved onto
   another act.

5. **Name what stays open.** With the fixes deferred, the corrected
   docstrings must say what the code does now, not only drop the over-claim.
   src-store/CLAUDE.md: "When what is intended and what the code does are
   different, say both." The form this tree uses is a limit at the function
   that has the gap. The checks built from the probes should hold today's
   answer, accepted, so that step 6 changes them on purpose. Until step 6,
   one reading rule: "W holds L's first permission" does not say that W made
   L. The writer on L's depot row does.

6. **Resends.** A double landing needs no unusual keeper.
   - `send-signed!` takes a new act-id on every call. When the append fails,
     it hands back the door's word, with the act-id and digest but not the
     signed act (`setup.clj:131-133`). So the one safe retry, the same bytes
     under the same act-id, cannot be made through it. Step 2's gives go
     through it.
   - Before step 2: `send-signed!` hands back the signed act with the door's
     word.
   - For whatever signs: a resend sends the same signed bytes under the same
     act-id, and never signs again. With people holding no keys, that
     signer is the sign-in server.
   - In the docstring pass: `door.clj:583-589` and `digest.clj:26-30` should
     say that a second send reads its answer as its own only when it carries
     the same signature.

   *Corrected:* the first version put the cause in a passkey in the browser,
   at step 4. With Sid's answer, no browser holds a key, so the cause is the
   one above: `send-signed!` as it is.

7. **Reads.** Fine not to act. A see-only permission stops a signed who from
   acting, and stops no reading, since the asker is what the caller says. Step
   2 can claim "this key cannot write here" and nothing about what a program
   sees.

**Agreed as planned:**
- test "signed" in one place;
- unsigned acts, and how a new version begins to count, wait for step 6,
  before the first record, with point 1 above;
- not acting on the cost of a signed check.

For how a version counts, Fable adds three conditions: it is settled before
the first record, and for each layer; until then each new rule is for signed
acts only; and the fixture needs signed acts too before the first record.

Fable's script for the weak keys, and its output, are in
`/mnt/data/projects/research/softland/first-act-opinion-2026-11-18/`.

## Sid's answers, 18 November

- **People hold no keys.**
  - A person is one fixed id, with her sign-in accounts attached: Google, an
    email, more later (9 October, entry 1).
  - She signs in, the sign-in server checks the account, and the server
    signs her acts for her. The store checks the server's signature. Losing
    access is mended through her account.
  - Only the system, and the server programs it trusts, hold keys.
  - What that costs: whoever controls the server can write as anyone. 12
    October, entry 4 already has it: such programs are "trusted with the
    whole store, in any person's name".
- **So step 2 changes.** HANDOFF.md's row "Root gives rights to a program and
  to a person's own key" is superseded. Step 2 is root giving the sign-in
  server a key that may act for signed-in people.
- **A single backup key.** The leaked key must never be able to replace
  itself, so the replacement of the system's top key is set up in advance.
  Sid chose a single backup key over several holders who must agree.

  The reviewer's form of it, not yet ruled on: the system holds three keys.
  - The top key is offline and used rarely.
  - The server's daily key sits beneath it, and the top key can replace it.
  - The backup key is kept apart and never used, until it replaces the top
    key. The first act names the top key and the backup key's fingerprint,
    which is one more fact in the making act of the base.

  A replacement written in the base reaches only the base, since a gate reads
  only its own layer. In every other layer where the replaced key is named,
  the change is one act more, and until it lands there the old key still
  works. If both the top key and the backup are lost, the last road is the
  code: a change, by Sid, that names a new top key from a point in the log.

**Still open:**
- **Before step 3:** how every gate knows the sign-in server's key (point 1
  above). It needs a design first, then Sid's call.
- **No deadline:** the two things built without Sid's go, signed acts making
  any layer and the one change to version 1. Both reviewers would keep them,
  and claim 3's induction rests on the second.
