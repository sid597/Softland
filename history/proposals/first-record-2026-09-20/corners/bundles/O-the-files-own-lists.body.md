# O the files' own lists: body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**log L407-408 · BODY · OWN-LIST C5**
*Question by question: what this camp would say › (9) Value: never removed, so how is one deleted, backups inc*

**For the first record (INFERRED).** Three things cannot be retrofitted. First, the value must be separable from the envelope, so that the value can go while the id, the edges and the verdict stay. Sid's "who read the erased thing" depends on exactly that. Second, nothing personal may sit in what stays: ids, keys, and above all "by whom". The actor must be an opaque id, with the link from that id to a person held as an erasable value. Third, if a content hash stays behind, it must not leak the value; a bare hash of a short value such as a name or a yes/no can be guessed, so the hash needs a random salt that is erased with the value. For backups, choose between short-lived backups that age out inside the legal deadline and forget-the-key, and know that lawyers dispute the second. One thing here favours Sid: the camp has no way to find derived copies of erased content, and Verraes can only broadcast a deletion event and hope. Walking based-on finds them.


---
**meaning L2218-2228 · BODY · OWN-LIST C4,C1**
*Part four — Sid's questions, hung on the parts of the fact › (17) The first facts*

**My read.** The first facts are: the grammar of grammar facts, which describes
itself; the policy for policy facts, which permits itself; the keys needed to say
those two things; the base layer; the gate as an actor; and an anchor to the
written definition of the body language and of the byte encoding. They are
written by hand, by a founder actor, in one genesis offer that the gate admits
under a rule that exists only for genesis. That rule is the one hand-cut circle.
They should be the same in every store. That rules out ids handed out by a gate's
counter for them. Their ids are either published constants or computed from their
content (2.6, trusty URIs). Unison and Git show what a translation table costs
when ids are local (2.3).


---
**sync L2291-2295 · BODY · OWN-LIST P0**
*4. Question by question › (0) The log: never rewritten, or only never lost?*

**Before the first record.**

1. State the promise in logical terms. An admitted fact is never changed and
   never silently absent. Its value may be removed by a recorded erasure. Its
   physical encoding may change.

---
**sync L2296-2296 · BODY · OWN-LIST C1,C7**

2. So nothing in the envelope may be computed from physical bytes (see (1)).

---
**sync L2297-2299 · BODY · OWN-LIST -**

3. Decide what the gate's "yes" means physically: durable on how many machines
   before the answer is given. Figma's history shows that this number exists
   whether or not it is chosen.

---
**sync L2300-2302 · BODY · OWN-LIST P0**

4. If anything is ever trimmed, the store must say what is missing (Bayou's
   omitted vector). Otherwise absence lies.


---
**sync L2336-2340 · BODY · OWN-LIST C1**
*4. Question by question › (2) Entity: how is an id made so two never clash? May it giv*

**Before the first record.**

1. The maker mints; the gate refuses a clash at admission. No peer-to-peer
   system can do the second half. It turns Yjs's silent corruption into an
   ordinary refusal.

---
**sync L2341-2343 · BODY · OWN-LIST C1**

2. Either at least 122 random bits, or "id of the creating offer plus an
   index" (the Pijul and Automerge shape). The second gives uniqueness by
   construction and reveals nothing by inspection.

---
**sync L2344-2345 · BODY · OWN-LIST C1**

3. No time, no place, no type, no shard in the id. Whatever can be read from an
   id will be relied on. If storage wants order, solve it below the id.

---
**sync L2346-2349 · BODY · OWN-LIST C1**

4. Do put a *scheme marker* in the written form of an id (Convex's format
   version; Git's lesson that an id quoted in free text does not say which
   scheme it belongs to; jj's alphabet). A scheme marker says nothing about
   when or where.

---
**sync L2350-2351 · BODY · OWN-LIST C1**

5. Knowing an id must never grant anything.


---
**sync L2378-2383 · BODY · OWN-LIST C4**
*4. Question by question › (17) The first facts*

**Before the first record.**

1. Fixed, published ids for the base layer, the gate as actor, the bootstrap
   actor, the key that means "this is a key", the key that holds a grammar, and
   the first policy. The same in every store, so two stores share a vocabulary
   from birth.

---
**sync L2384-2386 · BODY · OWN-LIST C4,C2**

2. One fact that is *different* in every store: the store's own id. A second
   store's gate numbers are then never confused with the first's (did:plc:
   a replica "could plausibly assign different sequence numbers").

---
**sync L2387-2389 · BODY · OWN-LIST C7**

3. The first facts are written by the bootstrap actor and their "checked
   against" list is empty, as in Matrix. They are axioms. The record should
   say so rather than pretend the gate checked them.

---
**sync L2390-2391 · BODY · OWN-LIST C4,C7**

4. The grammar of grammars is the one thing the runtime must know without
   looking it up. Keep it tiny, and version it (Cambria's "lens inception").

---
**sync L2392-2393 · BODY · OWN-LIST C4**

5. Anything named "placeholder" at genesis should be assumed permanent.


---
**sync L2425-2429 · BODY · OWN-LIST X2,E1**
*4. Question by question › (3) Key: a word, or an id with its name and shape as facts?*

**Before the first record.**

1. The non-negotiable part: each fact, or the gate's verdict on it, names the
   exact grammar version it was admitted under. Every system here either has
   this or says it misses it.

---
**sync L2430-2432 · BODY · OWN-LIST C4**

2. Within a key, only additive, optional change. Anything else is a new key.
   AT Protocol's rules are not a style choice for Sid; they are forced, because
   old facts can never be made to pass a new grammar.

---
**sync L2433-2437 · BODY · OWN-LIST C4**

3. My position on word or id: an id, with the name as a fact. Reason: under
   never-rewrite a word in stored facts can never be renamed, and AT Protocol
   shows that names do need to change. Dolt's failure had one cause,
   independent minting that must later converge, and one gate removes it inside
   one store: registering a name in the base layer is a compare-and-set.

---
**sync L2438-2443 · BODY · OWN-LIST C4,E6**

4. The costs of that position. A pattern written with a name must resolve it at
   some position, so name lookup is itself a read and belongs in based-on. Two
   people defining "deadline" in their own layers make two keys until someone
   states they are the same; promotion to base must handle that. A second store
   brings Dolt's problem back in full; p2panda's "name plus definition id" is
   the known answer there. Debugging raw facts gets harder.

---
**sync L2444-2446 · BODY · OWN-LIST P0,C4**

5. How a kind is kept (forever, replaced, not stored) is a property of the kind
   (Nostr's ranges, tldraw's scopes). Put it in the grammar fact now, because
   it decides what the store promises.

---
**sync L2447-2449 · BODY · OWN-LIST E1,X2**

6. The gate reads grammar and policy facts on every admission. That is a hot
   path through the thing it guards. AT Protocol chose not to do that.


---
**sync L2477-2481 · BODY · OWN-LIST C5**
*4. Question by question › (9) Value: never removed, so how is one deleted, backups inc*

**Before the first record.**

1. Envelope and value are separate physical things from the first fact. The
   envelope may hold a tagged digest of the value, never the value's bytes in
   a form that a hash chain depends on.

---
**sync L2482-2485 · BODY · OWN-LIST C5,P0**

2. An erasure is itself a fact: by whom, under which policy, because of what.
   It destroys the value, keeps the envelope, and leaves a do-not-readmit mark
   keyed by the fact's id. (A mark keyed by the hash of a short value, such as
   a name, can be reversed by guessing. My note, not a source's.)

---
**sync L2486-2488 · BODY · OWN-LIST E2,C5**

3. Crossings should record reads, not rendered copies. Patchwork's "minimal
   set" does exactly this. Then an erasure does not have to chase copies
   through "what was shown".

---
**sync L2489-2492 · BODY · OWN-LIST C5,E6**

4. Facts that are not re-derivable and stood on the erased value (a model's
   reply, a summary) are found by walking based-on. What to do with them is
   policy. They are findable only because reads were recorded. This is where
   Sid's design beats everyone in this camp.

---
**sync L2493-2495 · BODY · OWN-LIST C5**

5. Backups hold copies of values. Either they expire on a known horizon, or the
   value is stored so that destroying one thing destroys every copy. The second
   is the crypto-shredding question, which moved to research 7.

---
**sync L2496-2498 · BODY · OWN-LIST C5**

6. Between stores a delete is a request plus an audit trail. No one here found
   more.


---
**sync L2528-2531 · BODY · OWN-LIST C8**
*4. Question by question › (8) By whom: who checks it? Is an agent itself, or the perso*

**Before the first record.**

1. By-whom is the thing that acted: this agent, this model, this tool, this
   lane, this person. Never the person on whose behalf.

---
**sync L2532-2535 · BODY · OWN-LIST C8,E4,E1**

2. "Acts for" is a grant fact. The acting fact stands on the grant at its
   version (a based-on entry), and the gate's verdict names the grant it
   checked. One store makes Nostr's reader burden disappear: the gate resolves
   delegation once, at admission.

---
**sync L2536-2537 · BODY · OWN-LIST A1,A2**

3. The actor's definition (model, prompt, tool body) must be findable as of
   that version.

---
**sync L2538-2540 · BODY · OWN-LIST C8**

4. Do not mint a new long-lived actor per agent run. Automerge shows what
   thousands of short-lived actors cost. A run is a session of a standing
   actor.

---
**sync L2541-2542 · BODY · OWN-LIST C5,C8**

5. A human name is a fact about an actor entity. It can change and can be
   erased.

---
**sync L2543-2546 · BODY · OWN-LIST C8**

6. Who checks: the gate, against the session's credential. Signatures matter
   only when facts leave the store's trust, and they do not stay verifiable
   forever.


---
**sync L2561-2568 · BODY · OWN-LIST E3**
*4. Question by question › (11) When: whose clock? Ever used for order?*

**Before the first record.** "When" is the gate's admission time. It is never
used for order; the position is. Make it monotone along reads (the later of
the wall clock and one tick past the latest "when" among the facts read), so
it can never contradict based-on. A claimed time of the act (published in
1998; on screen at 10:03:07 by the person's machine) is a value under its own
key, not part of the envelope. If several gate instances ever exist, their
clocks agree only roughly, which is one more reason.


---
**sync L2582-2590 · BODY · OWN-LIST X4,C2**
*4. Question by question › (16) Layer: who sees a fact before any permissions exist?*

**Before the first record.** The layer is the first permission. Visibility is
structural and default-deny: a session layer is seen by that session, a
personal layer by that person and the agents they grant, the base by all. The
first policy facts are part of genesis (17). One global position counter leaks
activity across layers (Linear), so positions should be per partition. A base
fact whose based-on points into a private layer must show, to a reader without
rights, that something is hidden and not what. The gate sees everything; a
matched tool sees what its grant allows.


---
**sync L2611-2614 · BODY · OWN-LIST C3,C2**
*4. Question by question › (10) Order: can two gates write one layer? What must share a*

**Before the first record.**

1. The unit of compare-and-set is the cell. All versions of a cell come from
   one sequencer. A cell lives in one partition for life.

---
**sync L2615-2618 · BODY · OWN-LIST C3,E1**

2. What else must share that partition is whatever must be checked together
   with it: invariants across cells (Figma's no-cycles check; a name unique in
   base). List them before the first record. They decide partitioning, and
   the brief's three checks have no place for them yet.

---
**sync L2619-2621 · BODY · OWN-LIST C2**

3. "As of" is an opaque token that only the store can compare. It may hold one
   number today and many later. Nothing outside the store does arithmetic on
   it. This is the cheapest protection against Figma's "deeply baked".

---
**sync L2622-2622 · BODY · OWN-LIST E2,C2**

4. A crossing records the one token its screen or prompt was built at.

---
**sync L2623-2625 · BODY · OWN-LIST C3,A3**

5. A layer is not a partition. Two gate instances may write one layer if they
   own different cells. Personal and session layers are natural single-owner
   partitions (Frazee's rule). The base is not.

---
**sync L2626-2630 · BODY · OWN-LIST E1,A3**

6. Bayou's alternative to bare compare-and-set deserves a look under
   machine-rate contention: the offer carries a check (a "query and its
   expected result") and its own fallback. Sid's expected version is the
   simplest case of that.


---
**sync L2650-2654 · BODY · OWN-LIST E6**
*4. Question by question › (11) Based on: is every read listed, including reads that on*

**Before the first record.**

1. The runtime fills in based-on. An actor's own list of what it read is a
   claim. A list the runtime captured is a record. Only the second can carry
   "the map must not lie".

---
**sync L2655-2658 · BODY · OWN-LIST E6,C2**

2. Sid's three kinds of read are enough. The pattern read must also carry the
   position token (see (5)) and the *viewpoint*: the stack of layers it was
   evaluated under. The same pattern under a different stack is a different
   read.

---
**sync L2659-2659 · BODY · OWN-LIST E6**

3. The read that only made a tool fire is the because-of. Do not list it twice.

---
**sync L2660-2661 · BODY · OWN-LIST E6**

4. For a model's reply, based-on is one pointer to the crossing that fed the
   model. The crossing lists the reads. Do not copy them.

---
**sync L2662-2664 · BODY · OWN-LIST E6**

5. At machine rate, based-on is most of the bytes. Compress the common case the
   way Eg-walker does, and point at crossings the way (4) says.


---
**sync L2688-2691 · BODY · OWN-LIST E5**
*4. Question by question › (4) Based on: does each read say whether the fact depends on*

**Before the first record.**

1. A provenance read always stays on the version read. It is a statement about
   the past. "Stale" is then computed: the pinned version is no longer current.

---
**sync L2692-2694 · BODY · OWN-LIST E5**

2. Each read needs a role written when the fact is made, because it cannot be
   worked out later. Two roles carry most of the weight. *Requires*: staleness
   flows through it. *Was exposed to*: doubt may flow; staleness need not.

---
**sync L2695-2697 · BODY · OWN-LIST E5**

3. The role often follows from the actor. A deterministic tool requires all its
   reads. A model or a person was exposed to theirs, unless they say more (a
   person citing a source makes that read a requirement).

---
**sync L2698-2699 · BODY · OWN-LIST C4,E5**

4. Whether a *value* that points at another entity floats or pins is set by the
   key's grammar, not decided read by read.

---
**sync L2700-2702 · BODY · OWN-LIST C6,E5**

5. Two more relations stay separate from both: what this fact replaces (6) and
   what started the work (because-of).


---
**sync L2718-2724 · BODY · OWN-LIST C2**
*4. Question by question › (5) Based on: if an index was behind the log, is how far it *

**Before the first record.** Yes. The position on a pattern read is the
*index's* position, not the log's head. Otherwise a fact that was in the log
but not yet in the index will be counted as seen, and staleness will be
computed against a moment the reader never saw. A session also carries its own
high-water token, so it never reads from an index that is behind its own last
admitted fact. And lag that is large is shown, as Croquet shows it.


---
**sync L2734-2742 · BODY · OWN-LIST C7,E8**
*4. Question by question › (11) Because of: always filled; empty only when it starts a *

**Before the first record.** Two honest options. *Empty means a chain starts.*
Simple, and "find all chain starts" is a scan for emptiness. *Never empty.* A
chain that starts from a person's hand points at the session fact (12). Then
every chain is tied to an actor's session and a runtime build for free, and
there is no null to handle. The cost is a small stretch of meaning: the session
did not cause the click. I lean to never empty, for the same reason jj does.
Either way, what a person was looking at when they acted is a based-on entry
(the crossing), not the because-of.


---
**sync L2773-2779 · BODY · OWN-LIST C1**
*4. Question by question › (1) Version: is a fact pointed at by the gate's number, or b*

**Before the first record.**

1. Both. The fact's *name* is its own id, minted by the offerer when the offer
   is made (random, or actor plus counter). The gate's number is its
   *position*. Inside the store, position is how you order and say "as of".
   Across stores and in anything written down outside, the name is how you
   point.

---
**sync L2780-2784 · BODY · OWN-LIST C1**

2. A structural reason the name cannot be a content hash of the whole fact: the
   fact contains "when" and "version", which only the gate knows. A hash the
   offerer can compute names the offer, not the admitted fact. A hash the gate
   computes cannot be cited before admission. Automerge lived exactly this
   problem between its frontend and backend.

---
**sync L2785-2787 · BODY · OWN-LIST C1,E1**

3. Refusals settle it. A refused offer never gets a gate number. If refusals
   are kept (7), something else must name them. That something is the offer's
   own id.

---
**sync L2788-2793 · BODY · OWN-LIST C1,C5**

4. If integrity is wanted, carry a digest as an *attribute*: tagged with its
   algorithm, computed over a specified canonical form of the logical envelope,
   with the hash of the value inside it and not the value (Matrix, Bamboo,
   Pijul). More than one algorithm may coexist (Fossil). Inside one trusted
   gate nobody needs it; it earns its place between stores and for outside
   audit.

---
**sync L2794-2800 · BODY · OWN-LIST C1**

5. The question this opens, which the camp cannot answer for Sid: may offer B
   cite offer A before A is admitted? If no, a chain of agent work serialises
   through the gate, one round trip per step. If yes, B names A's offer id, and
   the gate admits B only once A is admitted (Kleppmann: a missing dependency
   "simply results in the update … never being delivered"). Automerge's regret
   applies: such a record must wait, not be dropped.


---
**sync L2813-2822 · BODY · OWN-LIST C6,C7,X3**
*4. Question by question › (6) Version: when 37 replaces 25, is "replacing 25" kept on *

**Before the first record.** Keep it. It is free: the offer already states the
version it expects to be current, and on admission that *is* the version
replaced. Write "none" explicitly for the first version in a cell; Bayou's
slot-with-infinity shows the value of a field that is always present. Keep
*superseding* (same cell) apart from *shadowing* (same entity and key, higher
layer). A shadowing fact should stand on the lower fact at its version, as a
based-on read. Upwelling found that when the base moves, every open layer must
be re-examined. That is only computable if each override recorded which base
version it overrode.


---
**sync L2845-2850 · BODY · OWN-LIST E1**
*4. Question by question › (7) Beside each fact: is the gate's yes or no kept? Where? A*

**Before the first record.**

1. A yes is the fact plus what was checked: the grammar version, the policy or
   grant version, the expected cell version. Three references. Put them in the
   envelope. Inline cannot be orphaned and does not double the write count; a
   sibling fact would do both.

---
**sync L2851-2852 · BODY · OWN-LIST E1**

2. A no is a refusal record named by the offer's id: who, which cell, which
   check failed, against which versions, and what would make it pass.

---
**sync L2853-2855 · BODY · OWN-LIST E1,C5**

3. The offered *value* in a refusal need not be kept. It may be exactly what
   policy forbade, or a secret, or junk; did:plc shows that kept rejects
   sometimes have to be purged.

---
**sync L2856-2858 · BODY · OWN-LIST E1**

4. Decide the retention class of refusals now. With machine-rate agents
   contending on cells they can outnumber facts. The camp's default is not to
   keep them at all; only systems built for audit do.

---
**sync L2859-2860 · BODY · OWN-LIST E1**

5. A verdict should be re-checkable from recorded things alone.


---
**sync L2877-2888 · BODY · OWN-LIST E8,A2,E2**
*4. Question by question › (12) Start of a session: is the runtime version and the kind*

**Before the first record.** Yes. A session fact: the actor, the runtime build
(as an anchor outside the store, a git commit, which Sid's third kind of read
already allows), the kind of machine or client, the stack of layers, and the
envelope format it speaks. A rebuild is a fact. A crossing names its session,
so "re-derivable" always has a referent. One cheap addition deserves thought: a
crossing can carry a digest of what was rendered. Then, after the fiftieth
rebuild, "yesterday's view no longer re-derives to the same thing" is a
detectable fact and not a silent lie. Figma and Automerge both reach for a
digest at exactly this kind of boundary. The lesson from Croquet's regret runs
the other way and Sid already has it: never let the code version into the
*data's* identity.


---
**sync L2904-2914 · BODY · OWN-LIST E7,E2**
*4. Question by question › (14) The hand: which motions become facts by default? Is bei*

**Before the first record.** Nobody in this camp persists pointer motion, and
the one team that persisted everything says why not. Pointing: not a fact by
default. Selecting: a fact when it is the operand of an offer (what was
selected when the person acted), or when it changes what is shown. Panning: a
fact only through the crossing it causes; a new viewport is a new "what was
shown". Being shown is not looking. Shown is something the runtime did, and the
runtime is its actor. Looking is a claim about a person; the hand is weak
evidence for it. Give them different keys and different actors. Every sample of
the hand must name the crossing it was made against; that is PushPin's admitted
gap. Hand facts need their own retention class (13).


---
**sync L2928-2936 · BODY · OWN-LIST E4,E7,E2**
*4. Question by question › (15) A click: which tools may act in a person's name?*

**Before the first record.** A click is a fact by the person: the named action,
its operand, and the crossing it was made on. Tools matched by that fact act as
themselves, because of the click, under a grant scoped at least by key, layer
and time. Only two things are ever in the person's name: the click, and a
promotion the person makes. A click approves what was shown, not whatever is
current, so the gate should compare the crossing's position or digest at
admission. That is compare-and-set for intent, and it is the same move as the
expected version.


---
**sync L2962-2972 · BODY · OWN-LIST C7,P0,E1**
*4. Question by question › (13) Storage: plain maps or classes? Ever trimmed? Backups?*

**Before the first record.** Plain, self-describing, language-neutral data for
the logical fact. No class or struct of any one runtime defines it; the runtime
will be rebuilt hundreds of times and the facts must not notice. An explicit
envelope-format version on every fact. Every writer keeps fields it does not
understand; there is one gate, which makes this easy, but re-encoders and
exporters are writers too. Value types limited to what every runtime reads the
same way, with explicit rules for big integers and floats. Retention classes
per key, fixed in the grammar: facts forever; refusals; hand samples; perhaps
session layers. Whatever is trimmed leaves a record of the omission. A backup
is good only if a replay from it can be shown equal to the store.


