# C6 replaces (6): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L294-294 · BODY · NEW-REASON C6**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- REPORTED. Datomic writes the replacement into the log. It does not leave it to be worked out. When a single-valued attribute changes, the same transaction carries a retraction of the old value and an assertion of the new one: `[42 :user/favorite-color :green 4567 true]` and `[42 :user/favorite-color :blue 4567 false]`. [D-MODEL]

---
**datalog L295-295 · BODY · DISAGREES C6,E1**

- REPORTED. Datomic's compare-and-set compares the expected *value*, not a version number: "If the entity has the expected value for the given attribute in db-before, then db/cas will expand to a list form asserting the new value. Otherwise, the transaction will abort." [D-TXFN]

---
**datalog L296-297 · BODY · NEW-REASON C6**

- INFERRED. Sid's offer already states the version it expects to replace. Keeping that number on the landed fact costs a few bytes, and it makes the chain of versions walkable from the log alone, with no index. The camp's habit is to put in the log whatever the log would otherwise need an index to reconstruct. Indexes are derived and get rebuilt. XTDB v1's reindexing "taking days to weeks" shows what it costs when the log is not self-sufficient. [X-HN23]


---
**frontiers L61-62 · BODY · NEW-REASON C6**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.2 The reasons, in McSherry's words*

**"New value for this key" is a poor change format.** REPORTED, on upserts: "at each moment in time you don't know what that prior value was" and "It seems like upsert based counting needs to maintain a copy of the collection just to interpret the changes flying at it." And why people do it anyway: "they are easier to produce, and put the burden of unpacking them on someone else." ([Upserts in Differential Dataflow, 2020](https://github.com/frankmcsherry/blog/blob/master/posts/2020-03-26.md))


---
**frontiers L79-79 · BODY · NEW-CASE C6**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.3 What changed over the years (worth more than the papers)*

- **Upserts had to be supported anyway**, with a stateful operator that holds the whole keyed collection, because "many folks show up with only upserts".

---
**frontiers L91-91 · BODY · NEW-REASON C6**
*1. Frank McSherry (Naiad, timely and differential dataflow,  › 1.4 Which questions McSherry speaks to*

- **(6) keep "replacing 25" on 37?** INFERRED from the upsert critique: yes. A landing that says only "new value" forces every matcher to hold prior state. A landing that names what it retracts can be processed without state.

---
**frontiers L318-318 · BODY · NEW-REASON C6**
*6. Nikolas Goebel (3DF, declarative differential dataflow) › 6.2 Reasons, in Goebel's words*

- Added or retracted. REPORTED: Goebel generalizes the boolean to "diff (difference, as in change in multiplicity)".

---
**frontiers L379-380 · BODY · NEW-REASON C6**
*7. Question by question: what this camp says*

**(6) Keep "replacing 25" on 37?** Yes (INFERRED from the upsert critique). Otherwise every matcher must hold prior state to know what just stopped being true.


---
**log L75-75 · BODY · NEW-REASON C6**
*Voice by voice › 1. Pat Helland*

- On version history: "A linear version history is sometimes referred to as being strongly consistent: one version replaces another; there's one parent and one child; each version is immutable; each version has an identity. The alternative to linear version history is a DAG (directed acyclic graph) of version history, in which there are many parents and/or many children." (*Immutability Changes Everything*.)

---
**log L485-486 · BODY · DISAGREES C6**
*Question by question: what this camp would say › (6) Version: when 37 replaces 25, is "replacing 25" kept on *

REPORTED. Helland: a new version "captures a replacement for or an augmentation of an earlier version", with "one parent and one child" when the store is linearizable and "many parents and/or many children" when it is not. Kleppmann's updates each carry "a set of predecessor hashes". Event Store leaves the predecessor implicit, because revisions inside a stream are consecutive.


---
**log L487-488 · BODY · NEW-REASON C6**

**For the first record (INFERRED).** Keep it. In Sid's store the gate's numbers are not consecutive within a cell (37 follows 25), so the predecessor is not implicit; it would have to be looked up in an index, and indexes are derived. The offer already states the version it expects, so keeping it costs one pointer. It is the only evidence of what the writer believed it was replacing. With hash names it makes each cell's history a chain that can be checked. And a list of parents extends to the day two stores, or two layers, meet; a bare number cannot.


---
**meaning L1018-1022 · BODY · DISAGREES C6**
*Part two — the people › 2.3 Unison (Paul Chiusano, Rúnar Bjarnason, Arya Irani), and*

**Where "replaces X" lived (6).** Not on the new thing. REPORTED: "replacements
are tracked in Unison patches [...] Patches identify their replacements by hash
instead of by name". And: "the old version of a definition doesn't have to be
available to use the patch."


---
**meaning L1173-1180 · BODY · NEW-REASON C6**
*Part two — the people › 2.5 Wikidata (Denny Vrandečić, Markus Krötzsch, Lydia Pintsc*

**What they do instead of deleting or overwriting.**

- Ranks. INSTITUTIONAL (Help:Deprecation): "Often, property values in Wikidata
  should be ranked as deprecated, not removed." One benefit listed: "it allows
  other users to know not to re-add the value". The reason must be attached: "A
  deprecated value should always have a P2241 qualifier." And a sharp line: a
  value that was right and is now out of date is *not* deprecated. It gets start
  and end dates.

---
**meaning L1363-1366 · BODY · DISAGREES C6**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

**What they changed.**

- The supersede and retract conventions came after the store. The 2016 paper
  lists them as future work. Five years of records were made first.

---
**meaning L1836-1841 · BODY · NEW-REASON C6**
*Part three — substrates › 3.1 Dynamicland's Realtalk, and Folk Computer*

**What they had to add, over the years.**

- Durable state. Statements vanish when their source does, so they added
  `Hold!`: "to create the equivalent of 'variables', stateful statements", with
  a key, so that a later hold with the same key replaces the earlier. That is a
  cell with supersession, arrived at from the other direction.

---
**meaning L1842-1846 · BODY · NEW-CASE C6**

- Supersession without a gap. REPORTED (Feb 2024): "don't fully retract a
  statement until the new replacement statement is in place, so that any retained
  consequences of the old statement stay alive under the new one". In July 2024
  they floated "maybe we track provenance of statements?" as the cure for
  flicker.

---
**meaning L2451-2459 · BODY · DISAGREES C6**
*Part four — Sid's questions, hung on the parts of the fact › (6) When 37 replaces 25, is "replacing 25" kept on 37?*

### (6) When 37 replaces 25, is "replacing 25" kept on 37?

**Camp.** On the new one: PROV revision, nanopublication `supersedes`, Armstrong's
parent tag, Git's parent (2.6, 2.9, 2.3). In the shape of the id: Xanadu (3.5).
Kept apart and later abandoned: Unison's patches (2.3). Removed because it blocked
deletion: AT Protocol's strong back-pointer, replaced with a clock value that "is
intentionally not a strong reference" (2.4). With a reason attached: Wikidata
(2.5). Without a gap for dependents: Folk (3.1).


---
**rama L327-328 · BODY · DISAGREES C6**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(6) "Replacing 25" on 37.** Not addressed. INFERRED: in his designs the newer record wins by position in the cell's list (Mastodon keeps "a list of status content versions… to capture the edit history"). He would call "replaces 25" a copy of something the order already says.


---
**skeptics L58-59 · BODY · DISAGREES C6**
*1. The ordinary default, question by question*

**(6) Is "replacing 25" kept on 37?** No. The history table keeps the old row. The new row does not point back. The expected version is a request parameter and is thrown away once checked (COMMON PRACTICE).


---
**skeptics L246-246 · BODY · DISAGREES C6**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (6) "replacing 25" | Not kept | Nothing found |

---
**sync L576-581 · BODY · DISAGREES C6,P0**
*2. Section one: sync and multiplayer › 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph*

**5. Resemblance and difference.** "One live value per object property" is
Sid's "one live row per entity + key + layer". A single authority orders.
Different: Figma keeps no history of losing values and no provenance; it shows
unconfirmed local changes; its partition is a file that one process can hold in
memory.


---
**sync L619-620 · BODY · DISAGREES C6**
*2. Section one: sync and multiplayer › 2.5 Linear (Tuomas Artman)*

- Rebase rewrites the base: "The `original` value of each transaction is
  updated to reflect the value from the delta packet".

---
**sync L837-840 · BODY · NEW-REASON C6,E5**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- Why "replaces" cannot be derived from "had seen": "The intention in the
  design of preds was that they are not redundant, because there are situations
  in which we don't want them to simply mirror the deps graph." (Kleppmann in
  issue #588, https://github.com/automerge/automerge/issues/588)

---
**sync L1430-1441 · BODY · DISAGREES C6,P0,C7**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- *The chain of previous commits was dropped.* "We mostly removed the concept
  of prev pointers (as CID Links) to previous commits, forming a chain of
  history." The field remains in the format, "virtually always null", kept
  "for v2 backwards compatibility", and its nullability was settled by a Go
  serialiser: "the idiomatic way to serialize data structures in golang works
  only one way or the other… Our current plan is to bend the rules of protocol
  stability… We don't love updating the spec to match implementation".
  (Newbold, Discussion #2181, 2024; spec, Repository) The design reason is in
  the sync proposal: verifying a chain "requires a local copy of the complete
  repository tree", which "is expensive"; they replaced it with an unsigned
  hint and per-commit checks, after which relays became "non-archival".
  (proposals/0006; "Relay Updates for Sync v1.1", 2025)

---
**sync L1465-1468 · BODY · NEW-REASON C6,C5**

- *A delete carries no context.* A request that the delete event include the
  deleted record was closed as not planned: "the firehose event for the
  deletion references only the rkey". (indigo issue #927, 2025) Every consumer
  must hold the prior state itself.

---
**sync L1507-1509 · BODY · DISAGREES C6**

- (6) They removed "replaces" from the signed record and keep an unsigned hint
  in the stream. INSTITUTIONAL. Consumers then cannot know what a delete
  deleted. REPORTED.

---
**sync L1608-1609 · BODY · DISAGREES C6,E3,P0**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- (6) No pointer to what was replaced; older versions may be dropped; order by
  the author's clock. REPORTED. fiatjaf regrets replaceability itself. REPORTED.

---
**sync L1917-1924 · BODY · NEW-REASON C6**
*3. Section two: versioning › 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von*

- Mercurial keeps the supersession record beside history, and shares it:
  "Unlike the previous way of handling such changes (which stripped the old
  changesets from the repository), obsolescence markers can be propagated
  between repositories." A marker holds "potential successors for a given
  changeset, the moment the changeset was marked as obsolete, and the user who
  performed the rewriting operation." (Mercurial wiki, "ChangesetEvolution",
  https://www.mercurial-scm.org/wiki/ChangesetEvolution)


---
**sync L2464-2465 · BODY · NEW-REASON C6,C5**
*4. Question by question › (9) Value: never removed, so how is one deleted, backups inc*

- A delete event that does not say what it deleted forces every consumer to
  hold the prior state: AT Protocol (issue closed as not planned). REPORTED.

---
**sync L2803-2812 · BODY · DISAGREES C6**
*4. Question by question › (6) Version: when 37 replaces 25, is "replacing 25" kept on *

**What the camp says.** Yes, from everyone who kept history. Automerge:
"Operations are stored with their predecessors in change chunks and with
successors in document chunks." p2panda: every operation has a `previous`.
Mercurial: a marker says which changeset, which successors, who, when, and they
wish it said what kind. jj keeps predecessors. Fossil's corrections name their
target. REPORTED. Those who did not keep it paid: Dolt rebuilt an old-to-new
mapping by hand ("pretty painful"); AT Protocol consumers cannot know what a
delete removed; Nostr "older versions MAY be discarded"; Linear rewrites the
base of a pending change on rebase. REPORTED.


