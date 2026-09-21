# The research camps: who they are, what they believe, how far their world is like Sid's

Written by the orchestrator session (Claude Fable 5.1) on 20 September 2026, first in chat, then copied here by script. Who each camp is comes partly from the orchestrator's own knowledge. What each found comes from the reports in this folder. `r1` to `r7` are the research sessions; `the-fact-and-its-questions.md` beside this file uses the same names.

**The problem.** Alice draws a relation. The store writes one fact. The store only appends, so it never goes back and changes that fact. Anything the fact does not carry at that moment can never be added to it. In ten years there will be billions of facts like it. So the question is which habits must be right from the very first fact.

To answer that, seven sessions each reconstructed how one group of builders would reason, from what that group wrote.

## Each camp in one line

| camp | what they are expert in |
|---|---|
| r1 Rama, Marz, Kreps | what the machines under you can and cannot do |
| r2 Hickey, Datomic, Nubank, XTDB | what a fact is, and what it cost to live on facts for ten years |
| r3 Kay, capabilities, substrates | what a stranger can understand decades later, and who is allowed to act |
| r4 the shared-log builders | what a log can honestly promise |
| r5 sync, multiplayer, version control | what it is like with many hands on one thing at once |
| r6 clocks, ids, determinism | what breaks under crashes, retries and rebuilt code |
| r7a frontiers and live views | how a computed answer stays true while its inputs move |
| r7b the ordinary way and the skeptics | whether any of this is worth it |

## r1. Rama, Nathan Marz, Jay Kreps

**Who.**
- Marz built Storm at Twitter, wrote *Big Data*, and then built Rama, the platform you stand on.
- Kreps built Kafka and wrote "The Log".

**What they built against.** Their world was huge data systems where people deploy bugs that corrupt data.

**What they believe.** Keep the raw record, never change it, and treat everything else as a view you can throw away and rebuild. Marz calls this protection against human error. Kreps' disagreement with Marz is narrow. He holds that one log and one codebase are enough, and that a log cut into partitions has no single order, only an order inside each partition.

**What they say about alice's fact.** They do not judge your design. They tell you what the ground really gives you:
- Order exists only inside a partition.
- The number of partitions is fixed when the system starts. (The reference says "currently", and that lifting this is "high priority".)
- A position in the log is not a safe name for a fact.
- A write that reports an error may still have happened. So alice's agent must make the id itself, and the gate's answer must be kept where a retry can find it.
- There are two kinds of gate:
  - One is fast and local. It answers in milliseconds and is atomic only inside one partition.
  - The other is slow and global. It takes 300 ms or more and cannot answer the offerer directly.
- Rama records no reads, has no logins, no permissions and no encryption. All of that is yours to build.

**Their challenge.** None of them holds "never rewritten". They hold that information and positions never change, while the bytes may.

**How far their world is like yours.**
- Like yours: it is your actual platform, so their physical facts bind hardest.
- Unlike yours: one company always owned their log. They never had many writers with their names on things, and they never ran one cluster for a planet.

## r2. Hickey, Datomic, Nubank, XTDB

**Who.**
- Hickey made Clojure and Datomic.
- Nubank is a bank with about a hundred million customers running on Datomic.
- XTDB is a similar database from JUXT, rebuilt from scratch in version 2.

**What they built against.** Businesses that must answer "what did we know, and when".

**What they believe.** A database is a growing pile of small facts that never change. "The database as of last Tuesday" is a thing you can hold and ask questions of. Updating in place destroys information.

**What hurt them.**
- The law forced Datomic to add deletion. It is costly, and it had a bug for years.
- Nubank's regrets:
  - no owner written on every transaction;
  - "implicit operations … that don't have origination information";
  - requests lost before they reached the database;
  - the fact store used for firehose data.
- XTDB version 1 kept fingerprints in the log and erasable documents beside it. Replay got stuck on erased documents. XTDB version 2 gave up the permanent log.

**What they say about alice's fact.** Your fact is their fact with more slots, so they speak most directly:
- You put on every fact what belongs on the act of saying it. Hickey: "the 'saying' of it *is* the transaction."
- Key as an id with its word as a fact about it is exactly what they do.
- For them "as of" is one number because they chose one order per database. Their advice is to choose the unit of order by meaning.
- "While the past may be forgotten, it is immutable."

**How far their world is like yours.**
- Like yours: the closest data model, and the longest lived experience with it.
- Unlike yours:
  - Hickey chose a closed world on purpose, "avoiding the challenges of universal naming, open-world, shared semantics".
  - Their stores have one writer with a hard ceiling.
  - They scale by running thousands of separate databases, never one.
  - They never recorded reads.

## r3. Kay, capabilities, substrates

**Who.** Two groups that belong together.

- **The meaning people:**
  - Alan Kay and his exchange with Hickey.
  - Kenton Varda, who designed protobuf's numbered tags.
  - Wikidata, which runs "properties are ids" at planet scale.
  - Unison, RDF and PROV.
  - Research mediums like Realtalk and Webstrates.
- **The authority people:**
  - Mark Miller and Norm Hardy on capabilities.
  - Christine Lemmer-Webber, who co-wrote ActivityPub.

**What they built against.** Systems meant to last decades and be used by strangers, where the writer and the reader never meet, and where programs act for people.

**What they believe.**
- On meaning: a record must let a future stranger recover what was meant. So ids are for machines and words are for people. Tags are never reused. Whatever interprets the record must be findable from it.
- On authority: permission should be something you hold and hand over. It should not be looked up from who you are. When it is looked up, a helpful program can be tricked into using its owner's power for someone else. Hardy named this in 1988.

**What they say about alice's fact.**
- A fact that is never rewritten is a message to a reader who is not there yet. Does it carry enough?
- The floor that interprets everything lives outside the store. A version number only points at it.
- Tools that fire by matching, with permission keyed on who you are, make the tricked-helper case possible. That case:
  - Alice enabled a tool.
  - Bob may only write notes.
  - Bob's note names something in alice's layer.
  - The tool fires and writes bob's words there as alice.
  - Every check passes.
- Their fix: every offer says which permission it is using.

**How far their world is like yours.**
- Like yours: they thought hardest about decades, strangers, and agents acting for people, which is your situation.
- Unlike yours:
  - Many of their systems never ran at scale. They bring strong arguments and thinner lived evidence.
  - Protobuf and Wikidata are lived at scale.
  - They also have one lived walk-back: Webstrates kept cursors durably and took it back.

## r4. The shared-log builders

**Who.**
- Pat Helland ("Immutability Changes Everything").
- Martin Kleppmann.
- Greg Young (event sourcing).
- Tango and Delos, which run Facebook's control plane.
- Hyder, Aurora, Lamport underneath.
- Certificate Transparency, Google's public logs that anyone can verify.

**What they built against.** Many machines that must agree on what happened.

**What they believe.** One shared, ordered log is the truth. Every machine's state is worked out from it. Position in the log is time.

**What hurt them.**
- Rebuilding state from a long log is slow ("the achilles' heel … is playback").
- At Facebook, machines running different code versions read the same log differently. That was "the only source of inconsistency in production".
- In Certificate Transparency, one flipped bit retired a whole log, because a log provable to strangers allows no repair.
- Greg Young now calls event-sourcing a whole system an anti-pattern.

**What they say about alice's fact.**
- A fact needs three names for three jobs: one made by the offerer, the gate's number, and a fingerprint.
- "Never rewritten" is really four promises, and you should pick which you mean:
  - never lost;
  - never different under one name;
  - never re-ordered;
  - provable to a stranger.
- The judge must not edit what it judges. Keep the offer exactly as offered, and write the gate's decision beside it.
- Never work the decision out again later, because the judge's code changes.
- Fix the rules for extending the envelope, not the list of its parts.

**How far their world is like yours.**
- Like yours: strongest on order, replay, and the true price of "immutable".
- Unlike yours:
  - Their logs carry commands for machines inside one organisation.
  - They hold no facts that people and agents read for meaning.
  - They have no layers and no authorship.

## r5. Sync, multiplayer, version control

**Who.**
- Bayou (Xerox PARC, 1994), Figma, Linear, Replicache, Automerge, Yjs, Convex, Croquet.
- The signed personal logs: Secure Scuttlebutt, Nostr, Bluesky's AT Protocol.
- Git, Dolt, Pijul, Mercurial.

**What they built against.** People editing the same thing at the same time, from many devices, sometimes offline, plus histories that must merge.

**What they believe.** The person must see their own action at once, and confirmation comes after. Everything that gets merged needs careful names.

**What hurt them.**
- Scuttlebutt named messages by fingerprint. It froze one JavaScript engine's way of printing JSON for ever, and it could not delete.
- Every one of Scuttlebutt's successors separated the value from the envelope.
- Git is nine years into changing its fingerprint method.
- Bluesky dropped permanent history because of deletion.
- Figma assumed one global order, and one slow shard stalled everyone.
- Yjs and Automerge recorded machines instead of authors and are fixing that now.
- Bayou built a "show only confirmed" view, and nobody chose it.

**What they say about alice's fact.**
- Never name a fact by its fingerprint.
- Make the value separable from day one.
- Let the floor record reads as "the range we scanned".
- Write down what a read was for, because later nobody knows.
- Make "as of" a ticket whose inside can change.
- Your "no optimism" rule with one gate for the world collides with physics. Their answer is to show the unconfirmed thing, clearly marked as unconfirmed.

**How far their world is like yours.**
- Like yours: the most experience with many simultaneous writers, with deletion in append-only logs, and with how it feels in the hand.
- Unlike yours: they mostly handle bounded documents for one team. They have no planet-wide store of attributed facts.

## r6. Clocks, ids, determinism

**Who.**
- FoundationDB, TigerBeetle (a database for money), Calvin.
- Google's line from Chubby to Spanner.
- Zanzibar, Google's permission system.
- Temporal and Restate, workflows that survive crashes by replaying their history.
- Kyle Kingsbury, who breaks databases' claims for a living.

**What they built against.** Money, locks and permissions, where a duplicate or a lost write is a disaster and machines die mid-step.

**What they believe.** Correctness comes from four things:
- ids made by the caller;
- decisions that are written down;
- a counter that goes up when control changes hands;
- replay under a pinned code version.

Never rest safety on "there is only one writer".

**What hurt them.**
- FoundationDB fused name and order into one value, and it broke when data moved between clusters.
- In TigerBeetle a refused transfer could succeed on a retry, until they recorded refusals under the offer's id.
- Temporal and Restate found that changing code breaks the replay of old histories.
- Datomic once had two writers during a failover. It stayed correct only because storage itself checked "is 25 still current?".
- Zanzibar had to invent a ticket so a permission check could not race a revocation.

**What they say about alice's fact.**
- A fact needs three fields: name, order, fingerprint.
- The gate's answer must be written in the same step as the check, where the fact lives.
- The code version goes on every decision and on every "what was shown".
- "Can be re-derived" holds only for one exact build. Your past never ends, and a running answer leaves nothing to compare against. So what was shown needs a fingerprint of what was shown.
- On permission: take r3's fix, and also record as of when the gate read the permission.

**How far their world is like yours.**
- Like yours: strongest on exactly your stress points, which are tens of agents retrying at machine rate and a floor rebuilt hundreds of times.
- Unlike yours: their records are fixed-shape machine commands in one organisation, and their histories eventually expire.

## r7a. Frontiers and live views

**Who.**
- Frank McSherry (Naiad, differential dataflow, Materialize).
- Feldera.
- Jamie Brandon.
- Noria, and K9db on privacy.
- Peter Alvaro and Joe Hellerstein.

**What they built against.** Keeping computed answers up to date while the inputs keep changing. Your "running answer" is what they build for a living.

**What they believe.** Every input carries a logical time. A frontier says "everything up to here has arrived". An answer is true only relative to one frontier. If you mix frontiers, the screen shows a state that never existed.

**What they say about alice's fact.**
- "One number, or one position per partition" is a false choice. Give alice one number, and have the store write down once what that number stands for.
- The gate's stamp can make two free promises: it never runs backward, and it is always later than everything the fact was based on. These cannot be added later.
- Record reads as "the question asked, as of when", including the ones that found nothing. Listing every row costs about 160 times the fact.
- Their sharpest point: as described, your map can lie with no stale read in it.

**How far their world is like yours.**
- Like yours: they are the camp for running answers.
- Unlike yours: they handle streams of rows in one organisation, with no authorship and no layers. They recompute and do not keep a history of reads.

## r7b. The ordinary way, and the skeptics

**Who.**
- The default everyone ships: rows that change, an audit table, logs that expire.
- Stonebraker and Pavlo, who have watched new database ideas get absorbed back into plain SQL for forty years.
- Operations lessons from:
  - Amazon;
  - Meta;
  - Microsoft's Orleans;
  - Google's Hyrum's Law;
  - Europe's privacy board.

**What they believe.** Boring and operable beats elegant. Every visible behaviour becomes something people depend on.

**What they say about alice's fact.**
- Amazon refuses fingerprint-as-name: "the caller actually wants two identical EC2 instances". The caller makes the id, and the stored request is the check.
- Do not put personal data, even locked, on a store that cannot erase it.
- When Meta turned stale reads into errors, years of hidden bugs surfaced.
- Any visible trait of an id becomes a promise. Twitter's clients sort by id.
- Amazon's newest database, Aurora DSQL, already runs your shape: several gates over separate keys, none ever stamping backward.

**Their challenge.**
- Amazon's own engineers left Dynamo for simpler services.
- AWS shut down its ledger database and pointed people at audit tables.
- Their challenge to you: show the win that rows plus an audit log cannot give.

**How far their world is like yours.**
- Like yours: a reality check, and they hold the bar the first workpiece must clear.
- Unlike yours: they operate known products, where you are building a medium that must explain itself from inside.

## Who speaks where on the fact

```
entity      rel1           r6 r1 r5        the offerer makes the id; nothing hidden inside it
key         :relation      r2 r3           an id; its word is a fact about it
value       {from f1…}     r2 r5 r7b       deleting from a store that never rewrites
by whom     alice          r3 r6 r5        her agent is itself; acting under which permission?
when        t25            r7a r4 r1       a stamp that can order, by rule
layer       A              r1 ↔ r2         where a layer lives, what stays in order   ← the only real fight
based on    [what she read] r7a r5 r2      write the question asked, not every row
because of  fact 24        r2              Nubank's biggest regret was not having it
version     25             r4 r5 r6        name, order and fingerprint are three things
one act     (missing)      r2 r4 r6 r1 r7b "the saying": the biggest finding
the gate's yes/no          r1 r6 r4        needed to be correct, not only for the record
which runtime              r4 r6 r7a r3    a rebuilt floor changes answers
the hand                   r3 r1 r2 r5     ticks kept for ever: all four say no
a click                    r3, r6 agrees   the tricked helper
the log itself             r1 r4 r5        "never rewritten" is four promises
the first facts            r2 r5 r7a r3    publish a list; do not compute them
the whole thing            r7b             "why not plain rows and an audit table?"
```

## How the camps fit together

- r1, r4, r2 and r7a are one family: a log, with state worked out from it.
- r6 tests that family's claims.
- r5 lives at the edge, where the people are.
- r3 asks what any of it means to a stranger, and who may act.
- r7b asks whether it is worth doing at all.

The camps carry different scars, and yet only one real disagreement needed an exchange. That was the unit of order, between r1 and r2, and it ended with both accepting the same hybrid. On everything else they give the same advice from different directions. I trust that agreement more than any single voice.

## Two things to keep in mind

- None of these camps shares your situation. Each had one owner, one organisation, or one document. Your combination is many writers with their names on things, agents acting for people, recorded reads, and one store for everyone. That is why "how far their world is like yours" sits under every camp, and why a famous name settles nothing here by itself.
- The most reliable signal is not what a camp believes. It is what that camp had to retrofit, such as authorship in Yjs, refusals in TigerBeetle, deletion in Datomic, and owner ids at Nubank. Every retrofit is something that was cheap on day one and painful in year five. That is your question, already answered by someone else's regret.
