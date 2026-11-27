# Allowlist: Frozen Check Sentences Changed by the Permission Fix

Frozen at Loop 0c, 27 November 2026 (derived in Loop 0b, reconciled with frozen `CONTRACT.md` in Loop 0c). A session's page (Window B), derived from
`BUILD.md` batches 1 to 4, `CONTRACT.md` beside this file, and the dump of
the 2,081 check sentences of the seven proofs
(`/mnt/data/projects/research/softland/permissions-test-store-scan/check-sentences.txt`,
named by `LOOP0-EMPIRICAL-FACTS.md`). `PLAN.md` and `HANDOFF-implementer.md`
were not read. Each sentence below is copied from the dump by a script,
with its file and line as the dump has them.

## How a sentence got on the list

This session read each of the 2,081 sentences once. A sentence is on the
list when what it says, or what its check holds the store to, is made
false or empty by a line of `BUILD.md` or by a shape of `CONTRACT.md`.
Each entry names the first batch that reaches it; where a later batch
reaches it again, both are named.

Two kinds of reason are kept apart. A reason that begins with B follows
from `BUILD.md` whatever the contract says. `S1 party` and `S5 rights`
follow from a default of the contract that waits for Sid's word, and
`making` and `table` from the frozen contract's engineering defaults.

What was checked and what was not. The judgment is from the sentence. The
check's own code was opened for three: `door_proof.clj:1136`,
`module_proof.clj:3847` and `pure_proof.clj:3338`. Nothing was run. An
entry says that a sentence may change, not how it will read.

## What is not on the list, on purpose

- Every check that builds an act builds it in the new form: its `:who`, the
  give that it names, the facts of a give. Where the sentence and what it
  holds stay as they are, that is no change of a sentence.
- Sentences that B4 leaves true while the proof brings the state about
  another way, by a give to everyone and its end:
  `read_path_proof.clj:707`, `710`, `1270`, `1273`, `1300`, `1302`;
  `read_path_pure_proof.clj:1258`, `1700`.
- Who may read a lane's acts whole (`:softland.store/who-may-read-acts`).
  The contract marks it as S6 and does not change it. Its sentences are
  listed only where they say that the first holder alone writes it (B3):
  `pure_proof.clj:4650` and `read_path_proof.clj:2463`, which read the
  start value from code, are not listed, and BUILD batch 4, line 2, as
  worded reaches them.
- Who may make a lane for another holder. P section 12, point 15, speaks of
  it and no "must be true" line of `BUILD.md` does:
  `module_proof.clj:1242` and `pure_proof.clj:1227` are not listed.
- A sentence that only names a gate's reason word in its check and not in
  its text. The words are the implementer's, and a word that changes with
  no change of what is refused is not listed.
- New sentences that the batches bring. This is the list of what may
  change, not of what must come.
- What is no check sentence of a `*proof.clj` file: the `SHOWN` lines,
  `line_statements.clj`, the two measures, and the two files of
  `run_again_fixture/`, which any change of the act's form remakes whole.

## The reasons

- **B1 form** (123). BUILD batch 1, line 1; CONTRACT section 2, the act. An act names its give by id. The written-out permission is gone, and with it its slots (the one above, the word for what it allows, `[:run …]`, `[:act …]`), its depth of 32 as a matter of the form, its lane slot and the one rule that read it.
- **B1 place** (68). BUILD batch 1, line 1; CONTRACT section 2. A give sits on an id of its own, under the keys of a give. The three keys of a permission are gone, and so is "sits on its holder", "on its run", "at the holder's place".
- **B1 many** (16). BUILD batch 1, line 2 (bug 1). A holder can have any number of gives on a lane. "One permission in a lane", "one for a run" and a give refused because one stands are gone.
- **B1 again** (23). BUILD batch 1, line 3 (bug 3). A give never given is answered apart from one that ended. Given again it is a new give with a new id, and a place takes one give for good; so no stamp tells two gives of one id apart.
- **B1 own end** (4). BUILD batch 1, line 4 (bug 4). A holder ends her own give, a holder of see too.
- **B1 rows** (37). CONTRACT section 2, PStates; P section 12, point 14. A row holds the id of the give that its act named, a UUID, where it held the permission's whole text; and a count of bytes or of fields that rested on that text changes.
- **B1 digest** (3). BUILD batch 1, line 1; CONTRACT section 2.2. `:permission` is an optional UUID on `:act` (still stripped by `hex-for-a-yes`).
- **making** (22). CONTRACT section 2. A making act names no give. It holds the depot fact and the facts of the lane's first give, and in B4 it can hold a give to everyone: not "two facts", and not "the permission that the act names".
- **B2 lent** (15). BUILD batch 2, lines 1 to 3. A give to a person stands on its own unless its giver lends it; only what is lent falls with the give above it.
- **B2 give** (7). BUILD batch 2, lines 4 and 5. Passing a give to another person needs the right to give; whoever holds see or write lends it to an agent with no such right.
- **B2 end** (4). BUILD batch 2, line 6 (item 18). The four who may end a give.
- **B3 one act** (3). BUILD batch 3, line 1. Every change of one act is held against what its author held before it, and the result is held as a whole.
- **B3 change** (1). BUILD batch 3, lines 2 and 3. What a give allows is changed in place; "see" does not become "see and act" by an end and a give.
- **B3 owner** (11). BUILD batch 3, lines 4 and 5. Any owner writes the lane's settings; a lane's first give can end while an owner remains.
- **B4 public** (49). BUILD batch 4, lines 1 and 2; CONTRACT section 2. "Seen by everyone" is a give to everyone. `:softland.store/who-may-see` is no store key, and no lane is open by a value in code.
- **S1 party** (9). Frozen CONTRACT §1.2 (`1A`). Whoever acts, holds or asks is named as `:person.<id>` / `:agent.<id>` / `:run.<id>` / `:system` (plus holder-only `:anyone` / `:anyone-signed-in`).
- **S5 rights** (91). Frozen CONTRACT §1.3 (`2`); BUILD batch 4, line 4. A read's rights come from one query. The sources "her permission's row, her run's row, the rows above", the stamp that a row above names, and the counts of calls, rounds and pushes that follow from them, change.
- **table** (3). Frozen CONTRACT §2.4. Each gate has a seventh table, `permissions-by-holder`, and the base's gate copies it to every task.

An entry with two reasons is counted under each.

## Counts

| Proof | Sentences | Listed |
|---|---|---|
| `act_proof.clj` | 139 | 37 |
| `door_proof.clj` | 240 | 2 |
| `module_proof.clj` | 479 | 101 |
| `pure_proof.clj` | 743 | 151 |
| `read_path_proof.clj` | 206 | 51 |
| `read_path_pure_proof.clj` | 242 | 61 |
| `run_again_proof.clj` | 32 | 2 |
| all seven | 2,081 | 405 |

## act_proof.clj

- `act_proof.clj:179` the declared shape of an act and of each thing that an act holds
  **B1 form, S1 party.** The shape `:permission-id` is gone; `:who` is a party.
- `act_proof.clj:181` the one rule, with its reason
  **B1 form.** The one rule read the lane slot of the written-out permission.
- `act_proof.clj:183` the ten parts of an act
  **B1 form.** `:permission` becomes optional `:uuid`. Ten still.
- `act_proof.clj:193` the seven reason words, read from the two declarations
  **B1 form.** `:no-permission` and `:permission-not-in-lane` are words of the old part and of the old rule.
- `act_proof.clj:200` the one shape with a number of its own for how often it may hold itself is a permission id, and its number is the value's: 32
  **B1 form.** No shape holds itself.
- `act_proof.clj:238` the least act: version, who, lane, permission, session and one fact
  **B1 form.** The least act names no permission.
- `act_proof.clj:249` an act whose ids are UUIDs: who, the lane, the permission, the session, an entity and a subject
  **B1 form, S1 party.**
- `act_proof.clj:254` an act under a permission that a parent permission granted
  **B1 form.**
- `act_proof.clj:258` an act under a given permission that says what it allows: see
  **B1 form.**
- `act_proof.clj:262` an act under a given permission that says what it allows: see and act
  **B1 form.**
- `act_proof.clj:317` no permission
  **B1 form.** `:permission` is optional on `:act` (absent/nil on making acts and own-ends).
- `act_proof.clj:318` a permission of two items
  **B1 form.**
- `act_proof.clj:319` a permission that is a list
  **B1 form.**
- `act_proof.clj:320` a permission with an item that is a string
  **B1 form.**
- `act_proof.clj:321` a permission whose parent is no permission
  **B1 form.**
- `act_proof.clj:322` a permission whose fourth slot is a word for what it allows, and no permission
  **B1 form.**
- `act_proof.clj:324` a permission whose fifth slot is a third word
  **B1 form.**
- `act_proof.clj:326` a permission whose fifth slot is a permission
  **B1 form.**
- `act_proof.clj:328` a permission of six items
  **B1 form.**
- `act_proof.clj:330` a first permission with a word after it: the slot is only for a given permission
  **B1 form.**
- `act_proof.clj:332` a permission in another lane
  **B1 form.**
- `act_proof.clj:333` a permission whose parent is in the act's lane, but itself in another
  **B1 form.**
- `act_proof.clj:382` version 2, and no permission
  **B1 form.**
- `act_proof.clj:383` a :who that is a string, and no permission
  **B1 form.**
- `act_proof.clj:384` no permission, and no session
  **B1 form.**
- `act_proof.clj:387` a permission in another lane, and no session: the shape comes before the rule
  **B1 form.**
- `act_proof.clj:389` a permission in another lane, and no fact: the shape comes before the rule
  **B1 form.**
- `act_proof.clj:391` a permission in another lane, and a mark on a fact: the shape comes before the rule
  **B1 form.**
- `act_proof.clj:449` a permission five deep
  **B1 form.** The depth of a chain is the gate's bound, not the form's.
- `act_proof.clj:450` a permission 32 deep
  **B1 form.**
- `act_proof.clj:451` a permission 33 deep
  **B1 form.**
- `act_proof.clj:452` a permission 32 deep that says what it allows: the slot is a word, and counts for no depth
  **B1 form.**
- `act_proof.clj:454` a permission 33 deep that says what it allows
  **B1 form.**
- `act_proof.clj:464` who as a UUID
  **S1 party.**
- `act_proof.clj:465` the lane as a UUID, with the permission in it
  **B1 form.**
- `act_proof.clj:520` permission ids: three ids, UUIDs too; with a parent; five deep; 32 deep; not two items, not a list, not 33 deep
  **B1 form.**
- `act_proof.clj:525` permission ids with the slot that says what one allows: see; see and act; above a permission that has the slot too; not a third word, not a word in the fourth slot, not six items
  **B1 form.**

## door_proof.clj

- `door_proof.clj:1127` Alice gives, through the door, a permission in her lane that says see: a fact under a store key, and it is accepted
  **B1 place.** A give is facts under the give's own id; two at the giving, by the contract.
- `door_proof.clj:1176` a maker makes the digest that a yes names of her act as she made it, before it has an act-id or a permission: the door's one call for it, which the gate makes of the thing that arrives; Alice gives the yes on an id of its own, and it is accepted
  **B1 digest.** "before it has an act-id or a permission".

## module_proof.clj

- `module_proof.clj:1247` the record holds the depot as a fact on the lane's own entity, and the person's permission on its holder
  **B1 place.**
- `module_proof.clj:1255` a lane whose id is a UUID, made by a person whose id is a UUID
  **S1 party.** A person's id in the store is `:person.<id>`.
- `module_proof.clj:1257` a making act for a lane that is made, by the system under the person's permission, is refused
  **making.** The system's making act names no give.
- `module_proof.clj:1259` a making act for a lane that is made, by its holder, is refused: its permission is not beneath its own
  **making, B1 form.** The reason "not beneath its own" read the written-out parent.
- `module_proof.clj:1287` the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and the time on the act
  **B1 rows.**
- `module_proof.clj:1292` the row holds those seven fields and no other
  **B1 rows.** Seven still; one field changes its name and its class.
- `module_proof.clj:1386` a retract is recorded as the current fact of its place: a row with no value, which holds who retracted and under which permission
  **B1 rows.**
- `module_proof.clj:1449` an act with a permission for another lane is refused by the form
  **B1 form.** No form can tell a give of another lane: it does not stand here.
- `module_proof.clj:1451` a permission given beneath another by its holder is accepted, and sits on its holder as a fact
  **B1 place.**
- `module_proof.clj:1455` an act under it, with a value, is accepted: the row holds the text of the value, who wrote it and the permission that was given
  **B1 rows.**
- `module_proof.clj:1458` a permission given by one who does not hold the one above is refused: it is not beneath the giver's own
  **B1 form.** "beneath the giver's own" was read from the parent slot.
- `module_proof.clj:1464` a permission five deep is a permission: the holder of one four deep gives it, it sits on its holder as given, and its holder acts under it with a value, whose row holds the whole permission
  **B1 form, B1 place, B1 rows.**
- `module_proof.clj:1469` an act that names as its own a permission six deep that nobody gave is refused for its standing, with the word of a permission that does not stand: its depth is no fault
  **B1 form.**
- `module_proof.clj:1471` a granted-in that is not the act's lane is refused
  **B1 form.** No granted-in slot.
- `module_proof.clj:1472` a permission fact for another lane is refused
  **B1 form.** The lane is named by what the give covers.
- `module_proof.clj:1473` a permission with nothing above it, in a lane that is made, is refused
  **B2 lent.** A give to a person with nothing above it stands on its own.
- `module_proof.clj:1474` a permission that stands is not given again: a holder has one permission in a lane
  **B1 many.**
- `module_proof.clj:1535` a lane for these checks is made, and its first permission, the first of the proof's chain and of depth 1, sits on its holder
  **B1 place, B1 form.**
- `module_proof.clj:1537` each holder gives a permission beneath her own, one at each depth from 2 to 32: the 31 gives are each accepted
  **B1 form.** A chain is of gives that are lent, and its bound is the gate's.
- `module_proof.clj:1539` the permissions of depth 4, 5, 31 and 32 each sit on their holder as they were given
  **B1 place.**
- `module_proof.clj:1541` the give at depth 33 is refused as malformed: the refusal is recorded under its own act-id with the digest of what was sent, and nothing sits on that holder
  **B1 form.** Too deep is a gate's refusal, not the form's.
- `module_proof.clj:1543` an act under the permission of depth 5, and one under the permission of depth 32, each with a value, are accepted
  **B1 form.**
- `module_proof.clj:1546` the row of each holds the whole permission that the act named: it reads back as that permission, its text opens 5 and 32 vectors, and no part of it is cut for its depth
  **B1 rows.**
- `module_proof.clj:1551` an act that names as its own a permission of depth 33, deeper than any that was given, is refused as malformed, and writes nothing
  **B1 form.**
- `module_proof.clj:1553` a permission of depth 32 with the fifth slot is given, with each of the slot's two words: the row holds the slot; the holder of see and act acts with a value, and the holder of see cannot act
  **B1 form.**
- `module_proof.clj:1559` a give of depth 33 with the fifth slot is refused as malformed
  **B1 form.**
- `module_proof.clj:1639` d. a refused thing keeps the time that it carried, as a label, and its stamp holds nothing of it. Three things, each with a usable time one hour after the chosen one: one with a part that no form names, whose word comes before the time's in the declaration's order; one whose permission is for another lane, whose word comes after it; and an act of a stranger under a permission that no act gave, which the declaration takes and the gate refuses for its standing. Each gets the lane's last stamp plus one, 10, 11 and 12, and its answer holds the time one hour ahead
  **B1 form.** "one whose permission is for another lane": that rule is gone, so the thing whose word comes after the time's is another.
- `module_proof.clj:1734` an act of 256 values at that bound, under a who and a permission of more than 8,192 bytes of text, would write more than 18,874,368 bytes into the rows of its facts, counted by the proof with Clojure's printer: it is refused with the word of the bound on an act, the refusal recorded under its own act-id; the depot's log holds it whole, its 256 values plain; and no row of it is written
  **B1 rows.** A row holds a give's id, so no permission has 8,192 bytes of text; what the bound on an act counts changes.
- `module_proof.clj:1808` each row gives back the UTF-8 of that text, and holds seven fields and no other; its time is the time on the act
  **B1 rows.** As `module_proof.clj:1292`.
- `module_proof.clj:1831` each row holds who wrote it and the permission that the act named: the maker of the lane under her first permission
  **B1 rows.**
- `module_proof.clj:1833` a second holder, given a permission in the kept form without the slot, acts: her rows hold her and her permission; a retract's row holds them too; the give's row holds the giver and the giver's permission
  **B1 form, B1 rows.** "the kept form without the slot".
- `module_proof.clj:1840` the system made Alice's personal lane for her: the rows of that making hold the system as who, and Alice's permission; the two differ
  **making, B1 rows.** The rows of a making hold the system as who and no give.
- `module_proof.clj:1902` before any fact says it, the place of each of the three keys holds no row, and the lane reads as: seen by its members, building on nothing, marking no key
  **B4 public.** Three keys become two; "seen by its members" came from code.
- `module_proof.clj:1905` who may see a lane: written, written a second time, written again naming the fact that is there; then a stale replaces, a retract and a write on another entity are each refused with a recorded word
  **B4 public.**
- `module_proof.clj:1917` the row of a setting holds who wrote it and under which permission
  **B1 rows.**
- `module_proof.clj:1924` a wrong value under each key is refused by the declaration: a word that is neither; a set for a list; 65 lanes; a vector for the marks; 33 keys; a key of the store's own namespace
  **B4 public.** Its first case, "a word that is neither", is of the key that leaves.
- `module_proof.clj:1931` a permission that stands is not given again, and the lane's depot takes one write
  **B1 many.**
- `module_proof.clj:1934` who may write the three is the lane's first holder alone: a second holder of the lane, who may act in it, sets it to everyone and is refused; the row is as the first holder left it
  **B3 owner, B4 public.**
- `module_proof.clj:1937` a setting into a lane that is not made is refused as any act into it; a making act with a third fact, a setting, is no making act: nothing is written at the making but the depot and the first permission
  **making, B4 public.**
- `module_proof.clj:1977` a permission is given with the slot that says what it allows, see: accepted, and its row holds the whole text, slot and all
  **B1 form.**
- `module_proof.clj:1979` the holder of see acts and names her permission as the row has it: refused, an act needs act; the refusal is recorded, and nothing is written
  **B1 form.** She names her give by its id.
- `module_proof.clj:1981` she names it without the slot, and with see and act in the slot: the bytes are not the row's, so it does not stand; nobody raises her right by naming it
  **B1 form.** An id cannot be named another way.
- `module_proof.clj:1983` she cannot give: a give is an act; and her act with a value is refused with the same word, under its own act-id with the digest of what was sent, and writes no row
  **B2 give.** A holder of see lends it to her own agent.
- `module_proof.clj:1989` a second give to her, of see and act, while her permission stands, is refused: a holder has one permission in a lane; her row is as it was, and she still cannot act
  **B1 many.**
- `module_proof.clj:1995` a give to the holder of see by another giver, of see and act: refused with the same word; a holder has one permission in a lane, as built
  **B1 many.**
- `module_proof.clj:1998` a making act whose permission has the slot is no making act: refused as an act into a lane that is not made; the lane is then made by a right act
  **B1 form, making.**
- `module_proof.clj:2000` each of her six refusals got a stamp of its own in the lane: the stamps go up in the order sent, the first later than the lane's last stamp before them
  **B1 many, B2 give.** The count of her refusals follows the four above.
- `module_proof.clj:2060` a holder who is not above a permission does not end it; the lane's first permission does not end, by its own holder or by one beneath
  **B2 end, B3 owner.**
- `module_proof.clj:2063` only the lane's first holder writes its setting: a holder of a given permission is refused, and the first holder's write is taken
  **B3 owner.**
- `module_proof.clj:2066` a give at a place whose permission stands is refused, and the row is as it was
  **B1 many.**
- `module_proof.clj:2068` an end that names a give which is not the one at the place is refused as a stale fact; the end that names the give at the place is taken
  **B1 again.** A place takes one give for good, so no other give can be "the one at the place".
- `module_proof.clj:2070` the end is a fact at the holder's place: a row with no value, by the act that ended it, with that act's stamp, who ended it and under which permission
  **B1 place.**
- `module_proof.clj:2074` after the end her act is refused with the word of an end, and the act of each holder beneath her with the word of an end above: nothing is written at their places, and the row of the holder beneath still holds her permission
  **B2 lent.** A person beneath the end stays; in B1 it holds as worded.
- `module_proof.clj:2078` what was taken under those permissions before the end stays: each row holds its value, its writer and the permission that it was written under
  **B1 rows.**
- `module_proof.clj:2084` she is given the same permission again, which has the same id: her act is taken, and those beneath her do not come back with it
  **B1 again.**
- `module_proof.clj:2087` the holder beneath is given hers again by an act of its own, at a place that still held the one that ended: taken; her act is taken, and the one beneath her is still ended
  **B1 again, B1 place.**
- `module_proof.clj:2091` a holder beneath an end is given a permission by another holder, straight beneath the first: her act under the new one is taken, and under the one that ended it does not stand
  **B1 place.** Her two gives sit on two ids; the older one is refused for the end above it, not as one that does not stand.
- `module_proof.clj:2169` a permission for one run is given by a holder of the lane, and sits on the run's session under its own key
  **B1 place.** And CONTRACT S4: the sitting is a field of the give.
- `module_proof.clj:2174` a permission for a run gives nothing: its holder cannot give herself a permission for the lane beneath it, or a permission for another run; and it writes no setting
  **B2 give.** "gives nothing": in B2 its holder may lend it to her own agent; the three cases named stay refused.
- `module_proof.clj:2178` the declaration refuses a permission for a run under the key of a permission for the lane, and one that does not sit on its run
  **B1 place.**
- `module_proof.clj:2183` a run ends no permission but its own: her other run's is refused; a give at a run that has a permission which stands is refused
  **B1 own end, B1 many.**
- `module_proof.clj:2209` a yes stands as any permission does: given beneath a holder who is not the first, the end of that holder's permission ends it
  **B2 lent.** It falls with the giver's only when it is lent.
- `module_proof.clj:2283` a fact under the marked key, with a value that is a keyword id: its place is the run of that value, and the row holds the fact that wrote it, its stamp, the time on its act, who and the permission, and no value
  **B1 rows.**
- `module_proof.clj:2368` a second making act of the base, its two facts in the other order, is refused: the base is made, and the permission that the act gives is not beneath its own
  **making, B1 form.**
- `module_proof.clj:2374` the rows hold the depot as a fact on the lane's own entity, and the system's permission on its holder
  **B1 place.**
- `module_proof.clj:2421` the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and the time on the act
  **B1 rows.**
- `module_proof.clj:2426` the row holds those seven fields and no other, and every task holds it, the same in every field
  **B1 rows.** As `module_proof.clj:1292`.
- `module_proof.clj:2585` a retract is recorded as the current fact of its place, on every task: a row with no value, which holds who retracted and under which permission
  **B1 rows.**
- `module_proof.clj:2713` the system gave a person a permission in the base, beneath its own: accepted, and it sits on its holder as a fact
  **B1 place.**
- `module_proof.clj:2720` a permission given beside the giver's own, not beneath it, is refused, and gives nothing
  **B1 form.** "beside the giver's own, not beneath it" was read from the parent slot.
- `module_proof.clj:2723` in the base a permission that stands is not given again, an end at a place that holds no permission is refused, and the lane's depot takes one write: the depot again, another depot
  **B1 many.** Its first clause.
- `module_proof.clj:2735` a tool's own fact under the bare word :permission, on a holder's entity, is accepted in the base: its row is on every task, as any fact's, with the tool's value, beside the store's permission of that holder
  **B1 place.** The store's give no longer sits on the holder's entity, beside the tool's fact.
- `module_proof.clj:2785` in the base each holder gives a permission beneath her own, one at each depth from 2 to 32, appended one after the other with no wait for the gate: the 31 gives are each accepted, so each was decided against the row that the give before it wrote
  **B1 form.** As `module_proof.clj:1537`.
- `module_proof.clj:2787` the permissions of depth 4, 5, 31 and 32 each sit on their holder as they were given, on every task
  **B1 place.**
- `module_proof.clj:2790` the give at depth 33, appended in the same row, is refused as malformed: the refusal is recorded under its own act-id with the digest of what was sent, and no task holds a row on that holder
  **B1 form.**
- `module_proof.clj:2792` an act under the permission of depth 5, and one under the permission of depth 32, each with a value, are accepted
  **B1 form.**
- `module_proof.clj:2795` the row of each holds the whole permission that the act named: it reads back as that permission, its text opens 5 and 32 vectors, and no part of it is cut for its depth
  **B1 rows.**
- `module_proof.clj:2800` an act that names as its own a permission of depth 33, deeper than any that was given, is refused as malformed, and writes nothing on any task
  **B1 form.**
- `module_proof.clj:2803` a permission of depth 32 with the fifth slot is given, with each of the slot's two words: every row holds the slot; the holder of see and act acts with a value, and the holder of see cannot act
  **B1 form.**
- `module_proof.clj:2807` a give of depth 33 with the fifth slot is refused as malformed
  **B1 form.**
- `module_proof.clj:2851` an act of 256 values at that bound, under a who and a permission of more than 8,192 bytes of text, would write more than 18,874,368 bytes into the rows of its facts, counted by the proof with Clojure's printer: it is refused with the word of the bound on an act, the refusal recorded under its own act-id; the depot's log holds it whole, its 256 values plain; and no task holds a row of it
  **B1 rows.** As `module_proof.clj:1734`.
- `module_proof.clj:2961` d. a refused thing keeps the time that it carried, as a label, and its stamp holds nothing of it: a thing with a usable time one hour ahead that the declaration refuses for another fault, one with a part that no form names and one whose permission is for another lane, gets the base's last stamp plus one, and its answer holds the time one hour ahead
  **B1 form.** As `module_proof.clj:1639`.
- `module_proof.clj:3160` each row gives back the UTF-8 of that text, and holds seven fields and no other; its time is the time on the act
  **B1 rows.** As `module_proof.clj:1292`.
- `module_proof.clj:3188` each row holds who wrote it and the permission that the act named: the system's rows the system and its first permission; a person's rows her and the permission that the system gave her; a retract's row too; the give's row the giver
  **B1 rows.**
- `module_proof.clj:3244` before any fact says it, no task holds a row at the place of any of the three keys, and the base reads as: seen by everyone, building on nothing, marking no key
  **B4 public.** The base is seen by everyone by a give that the log holds.
- `module_proof.clj:3247` each of the three keys is written in the base, in one batch: accepted, and its row is on every task, the same on each
  **B4 public.** Three keys become two.
- `module_proof.clj:3256` a stale replaces, a retract, another entity and a wrong value are each refused with a recorded word, for each of the three keys
  **B4 public.**
- `module_proof.clj:3262` after those fifteen refusals every task holds the rows that the last accepted writes left, with the same stamps
  **B4 public.** The count of refusals follows the count of keys.
- `module_proof.clj:3269` who may write the three is the lane's first holder alone: a person with a permission in the base, who may act in it, sets the base to everyone and is refused; every task holds the row that the system left
  **B3 owner, B4 public.**
- `module_proof.clj:3304` the system gives a person a permission in the base that says see: accepted, and its row is on every task with the whole text, slot and all
  **B1 form.**
- `module_proof.clj:3312` she names her permission without the slot, and with see and act in it: it does not stand
  **B1 form.**
- `module_proof.clj:3314` she cannot give, and her act with a value is refused with the same word, under its own act-id with the digest of what was sent, and no task holds a row of it
  **B2 give.**
- `module_proof.clj:3318` a second give to her, of see and act, while her permission stands, is refused: a holder has one permission in a lane, and her row on each task is as it was, in every field
  **B1 many.**
- `module_proof.clj:3356` in one batch, in the order appended: an act of each before the end is taken; the end is taken; an act of each after it is refused, hers with the word of an end and the one beneath with the word of an end above
  **B2 lent.** A person beneath the end stays; in B1 it holds as worded.
- `module_proof.clj:3369` she is given the same permission again and acts in the same batch: taken; the one beneath her does not come back with it; her new row is on every task
  **B1 again.**
- `module_proof.clj:3425` the rows are on every task: the run's place an end after the batch; each yes's place its use, an end written by the act that used it; the act's fact, written under the second yes, which used it last
  **B1 place.** "the run's place".
- `module_proof.clj:3496` two facts on two entities with one value: both places are in the run of that value, each row of the run is on every task, and the row holds the fact that wrote it, its stamp, the time on its act, who and the permission, and no value
  **B1 rows.**
- `module_proof.clj:3587` two writes of who may see the base in one batch: both accepted, and every task holds one row, the second one's
  **B4 public.**
- `module_proof.clj:3847` the copies are still on every task: each of the gate's four tables holds on each task the rows that it held before the start, in every key and every field
  **table.** The copies of `gives-by-holder` are on every task too: four tables are five when the proof takes it in.
- `module_proof.clj:4259` on the second cluster, with the proof's two functions in the vars of the store's `writes` and `stray-reason`: a streaming lane is made; the base is made by a making act whose two facts are in the other order; and a key of the base is marked
  **making.**
- `module_proof.clj:4509` the first holder writes who may read the lane's acts, and writes it again; a holder of a given permission who may act is refused as for every setting; on another entity, with no value, and with a word that is neither, each is refused
  **B3 owner.** The key itself is CONTRACT S6 and is not changed.

## pure_proof.clj

- `pure_proof.clj:579` the six shapes that the door adds: what arrives, with the time on the act after the depot, whose facts are the maker's own; a depot word; and the values of the four keys that take a second write
  **B4 public, B1 place.** The word of who may see leaves the door's shapes, and the shapes of a give's values come.
- `pure_proof.clj:586` the twenty rules, the maker's first, each with its reason; the seven of the keys that take a second write stand after the four rules of a permission fact, who may read a lane's acts right behind who may see it
  **B1 form, B1 place, B4 public.** The maker's rule, the four rules of a permission fact and the rule of who may see: the count is not twenty.
- `pure_proof.clj:589` the words with which the check refuses, read from the two declarations, the word of the time's part among them, and the two of its last step
  **B1 form, B1 place.** The set of words changes.
- `pure_proof.clj:600` the bound on an act is 256 values at the bound of one value, and 8,192 bytes of who and permission for each of 256 facts
  **B1 rows.**
- `pure_proof.clj:603` the eight store keys, the shape of the value of each, which is a setting, and which is written again; a key in the store's namespace that is none of the eight, and a bare word, is no store key
  **B1 place, B4 public.** Three keys of a permission leave, the keys of a give come, and who may see leaves.
- `pure_proof.clj:732` the least thing in its one form on arrival: each part, the time on the act as it arrived, and its fact with its five parts, the text of the value in the place of the value
  **B1 form.** The parts of the one form on arrival.
- `pure_proof.clj:758` a making act: two facts under store keys, each with its value and the text of it
  **making.**
- `pure_proof.clj:768` each of the eight store keys is known: a fact under each, with a right value, passes; a permission for a run sits on its session, and one for an act on the uuid of its act
  **B1 place.**
- `pure_proof.clj:784` the values that the three keys of a second write take: each word of who may see; a list of lanes that is empty, names the lane itself, names a lane twice or names a lane that nobody made; a set of keys that is empty
  **B4 public.** "each word of who may see".
- `pure_proof.clj:794` a fact with no value under each of the three keys of a second write passes the declaration, as under the others: a gate refuses the retract
  **B4 public.**
- `pure_proof.clj:798` a permission fact whose value says what it allows passes: see, and see and act
  **B1 form.** What a give allows is a value of its terms.
- `pure_proof.clj:826` no permission
  **B1 form.**
- `pure_proof.clj:829` a permission for another lane
  **B1 form.**
- `pure_proof.clj:831` a permission 33 deep
  **B1 form.**
- `pure_proof.clj:872` a permission fact whose value is no permission
  **B1 place.**
- `pure_proof.clj:873` a permission fact that does not sit on its holder
  **B1 place.**
- `pure_proof.clj:874` a permission for a run under the key of a permission for the lane
  **B1 place.**
- `pure_proof.clj:876` a permission for the lane under the key of a permission for a run
  **B1 place.**
- `pure_proof.clj:877` a permission for an act under the key of a permission for a run
  **B1 place.**
- `pure_proof.clj:880` a permission for a run that does not sit on its run
  **B1 place.**
- `pure_proof.clj:882` a permission for an act that does not sit on the uuid of its act
  **B1 place.**
- `pure_proof.clj:885` a permission for an act that says see: it would allow nothing, and is no permission
  **B1 form.** The old shape took `:see-and-act` alone for one act; the contract's value for it is `{:write true}`, and what else the declaration refuses is the implementer's.
- `pure_proof.clj:891` who may see a lane, with a word that is neither
  **B4 public.**
- `pure_proof.clj:892` who may see a lane, as a string
  **B4 public.**
- `pure_proof.clj:893` who may see a lane, as a set of people
  **B4 public.**
- `pure_proof.clj:920` who and the holder of the act's permission
  **B1 form.**
- `pure_proof.clj:921` granted-in, in the act's permission
  **B1 form.**
- `pure_proof.clj:926` granted-in, in a permission that is given
  **B1 form.**
- `pure_proof.clj:927` who, in the permission above one that is given with the slot that says what it allows
  **B1 form.**
- `pure_proof.clj:957` the time's word stands after the word of a part that the form does not know, and before the word of each of the maker's own parts: of the things of `makers-rows` with the time taken off, the first, whose fault is such a part of the thing itself, keeps its word, and each of the other sixteen, whose fault is in a part of the maker's, gets the time's
  **B1 form.** "the other sixteen": three of the maker's rows are rows of the old permission.
- `pure_proof.clj:961` the time's word stands after the word of a depot that is absent or no depot word, and before each rule: of the things of `doors-rows` with the time taken off, the first two keep the depot's word, and each of the other thirty-two, whose fault a rule finds, gets the time's; a depot word that is not the act-id's is one of them
  **B1 place, B4 public.** "the other thirty-two": eleven of the door's rows are rows of a permission fact or of who may see.
- `pure_proof.clj:1023` the proof's own count of the bytes that the act at the edge of the bound on an act would write, and of the act one byte over; for each fact, of who and permission
  **B1 rows.**
- `pure_proof.clj:1027` the bytes of who and permission are counted for a fact with no value too: an act of 255 values and one retract, which is one byte over the bound only when the retract's row is counted, is refused; with one byte less it passes
  **B1 rows.**
- `pure_proof.clj:1059` no time, and no permission
  **B1 form.**
- `pure_proof.clj:1075` a permission for another lane, and a depot that is not the act-id's
  **B1 form.**
- `pure_proof.clj:1080` a wrong word of who may see, and a permission fact that does not sit on its holder: the rules of a permission come first
  **B1 place, B4 public.**
- `pure_proof.clj:1082` a wrong word of who may see, and a list that is no list: the rules of the three keys in their order
  **B4 public.**
- `pure_proof.clj:1129` an act under its own permission five deep
  **B1 form.**
- `pure_proof.clj:1130` an act under its own permission 32 deep
  **B1 form.**
- `pure_proof.clj:1131` an act under its own permission 32 deep that says what it allows
  **B1 form.**
- `pure_proof.clj:1133` an act under its own permission 33 deep
  **B1 form.**
- `pure_proof.clj:1134` a permission fact whose value is five deep
  **B1 form.**
- `pure_proof.clj:1135` a permission fact whose value is 32 deep
  **B1 form.**
- `pure_proof.clj:1136` a permission fact whose value is 32 deep and says what it allows
  **B1 form.**
- `pure_proof.clj:1138` a permission fact whose value is 33 deep: it is no value of the form, so the shape's word and not the rule's
  **B1 form.**
- `pure_proof.clj:1140` ids that are UUIDs: who, the lane, an entity
  **S1 party.**
- `pure_proof.clj:1218` the two facts in the other order
  **making.**
- `pure_proof.clj:1231` a third fact beside the two
  **making.**
- `pure_proof.clj:1232` a third fact that says who may see the lane: nothing is written at the making but the depot and the first permission
  **making, B4 public.**
- `pure_proof.clj:1236` a permission fact that is not the permission that the act names
  **making.** A making act names no give.
- `pure_proof.clj:1238` a first permission with a permission above it
  **B1 form.**
- `pure_proof.clj:1243` a first permission granted in another lane
  **B1 form.**
- `pure_proof.clj:1246` a permission that says what it allows, see and act: it has a permission above it and a slot, so it is no first permission
  **B1 form.**
- `pure_proof.clj:1278` what a lane starts with, where no fact says it: the base is seen by everyone and any other lane by its members; a lane builds on nothing; no key is marked; nothing for any other key
  **B4 public.**
- `pure_proof.clj:1374` the row of a fact with a value under a plain key: its value is the fact's text as it is, a String, and the row gives the UTF-8 of it back; the fact that wrote it, the stamp, the time on the act, who wrote it and the permission that the act named; each field of its declared class, and none a byte array
  **B1 rows.**
- `pure_proof.clj:1379` the row of a fact under a store key: the text of its value as a String, whose bytes the row gives back; the fact that wrote it, the stamp, the time, who and the permission, and no other field
  **B1 rows.**
- `pure_proof.clj:1391` the row of a retract: no value, and the row is there; it holds who retracted, under which permission and at what time, and no other field
  **B1 rows.**
- `pure_proof.clj:1394` who wrote a row and the permission that its act named are each the one text, so a who that is a UUID reads back as a UUID; nothing for no row, for a row with neither, and for a text that is no id or no permission
  **B1 rows, S1 party.** The give's id is a UUID in the row, and no text.
- `pure_proof.clj:1420` the first form of a row's String, read back: a depot, a permission, a tool's value; nothing for a retract, for no row, and for a String that reads as no form
  **B1 form.** A give's value read back is a map.
- `pure_proof.clj:1462` a key of the facts by value: lane, key, the stamp since which the key is marked, the value digest, then the entity; its row is the fact's row without its value, with the two texts and the time as they were given
  **B1 rows.** "the two texts".
- `pure_proof.clj:1644` the act's permission is at its holder's place
  **B1 place.**
- `pure_proof.clj:1645` an act under a permission given beneath another, at its holder's place
  **B1 place.**
- `pure_proof.clj:1646` the place holds another permission of the same holder
  **B1 place, B1 many.** Another give of one holder sits on another id.
- `pure_proof.clj:1654` a making act by the holder for a lane that is made: its permission stands, and it makes nothing
  **making.**
- `pure_proof.clj:1667` a holder of see who names her permission as the row has it: an act needs act
  **B1 form.**
- `pure_proof.clj:1669` a holder of see who names her permission without the slot: the bytes are not the row's, so naming more does not stand
  **B1 form.**
- `pure_proof.clj:1671` a holder of see who names her permission with see and act in the slot
  **B1 form.**
- `pure_proof.clj:1673` a holder of a permission that says see and act, named as the row has it
  **B1 form.**
- `pure_proof.clj:1675` a holder of a permission that was given without the slot: it reads as see and act
  **B1 form.**
- `pure_proof.clj:1677` a holder of see and act who names her permission with see in the slot: it does not stand, so nobody acts under less by naming less either
  **B1 form.**
- `pure_proof.clj:1681` a making act whose permission has the slot, into a lane that is not made: it is no making act
  **B1 form, making.**
- `pure_proof.clj:1693` the places that the standing reads: who's permission, then the lane's depot
  **B1 place.**
- `pure_proof.clj:1695` :alice :alice :softland.store/permission
  **B1 place.**
- `pure_proof.clj:1700` what a permission allows, read from its one form: a first permission and one given without the slot read as see and act; with the slot, what the slot says; the slot of a permission above it is not read
  **B1 form.**
- `pure_proof.clj:1730` the chain of a permission is read from its id: the one above it, the places above it nearest first, and none for a first permission
  **B1 form.** The chain is read from rows, one `:lent-from` at a time.
- `pure_proof.clj:1735` one permission is above another when it is in the chain above it: the one above, and the first; not itself, not the one beneath, not another holder's
  **B1 form.**
- `pure_proof.clj:1740` the rows that the standing reads beside the first two: the place of each permission above the act's own, and none when the first row does not hold the act's permission, or for a first permission
  **B1 form.** The rows above are not known from the act alone.
- `pure_proof.clj:1746` a permission stands when its row holds it and each one above it stands with an older row
  **B1 again, B2 lent.** No stamp is compared; and only what is lent rests on the row above.
- `pure_proof.clj:1752` what was given beneath it ends with it: Carol's row still holds her permission, and her act is refused
  **B2 lent.** In B1 it holds as worded.
- `pure_proof.clj:1755` Bob is given the same permission again, which has the same id: his act is taken again
  **B1 again.**
- `pure_proof.clj:1758` and Carol's does not come back with it: her row is older than the row of Bob's second give
  **B1 again.**
- `pure_proof.clj:1845` a permission given straight beneath the act's own, on an empty place
  **B1 place.**
- `pure_proof.clj:1846` a holder gives itself a second permission beneath its own: its place holds one
  **B1 many.**
- `pure_proof.clj:1849` a permission given for another lane
  **B1 form.** As `module_proof.clj:1472`.
- `pure_proof.clj:1850` a permission given with a granted-in that is not the act's lane
  **B1 form.**
- `pure_proof.clj:1851` a permission given with nothing above it, in a lane that is made
  **B2 lent.**
- `pure_proof.clj:1852` a permission given beneath a permission that is not the act's own
  **B1 form.**
- `pure_proof.clj:1854` the same permission given a second time, to a holder whose permission stands
  **B1 many, B1 again.**
- `pure_proof.clj:1861` an end that names a give which is no longer at the place: the word of a stale fact
  **B1 again.**
- `pure_proof.clj:1866` an end of the lane's first permission, by its own holder: it does not end
  **B3 owner.**
- `pure_proof.clj:1868` an end of a permission two beneath the act's own: anyone above ends it
  **B2 end.**
- `pure_proof.clj:1870` a give after an end: the place holds an end, and the give is taken
  **B1 again.** A place takes one give for good; given again is another id.
- `pure_proof.clj:1872` a give after an end, of another permission than the one that ended
  **B1 again.**
- `pure_proof.clj:1874` a give at a place whose permission no longer stands, because the one above it ended: it is taken, and its row takes the place
  **B1 again, B1 place.**
- `pure_proof.clj:1879` a give at a place whose permission stands beneath another holder: a holder has one permission in a lane
  **B1 many.**
- `pure_proof.clj:1889` a depot fact on another entity before a permission for another lane: the setting gives the word
  **B1 form.** Its second fault is the old lane rule's.
- `pure_proof.clj:1891` who may see the lane, on an empty place
  **B4 public.**
- `pure_proof.clj:1894` who may see the lane, written a second time on a place that holds one, naming nothing: a second write is taken
  **B4 public.**
- `pure_proof.clj:1898` who may see the lane, written again naming the fact that is there
  **B4 public.**
- `pure_proof.clj:1900` who may see the lane, written again naming a fact that is no longer there: a writer who must not lose a write names what she replaces
  **B4 public.**
- `pure_proof.clj:1906` a retract of who may see the lane
  **B4 public.**
- `pure_proof.clj:1909` who may see, on an entity that is not the lane
  **B4 public.**
- `pure_proof.clj:1912` a retract of who may see on an entity that is not the lane: the setting gives the word
  **B4 public.**
- `pure_proof.clj:1913` a second give to a holder of see whose permission stands, of see and act: see becomes see and act by an end and a give after it
  **B1 many, B3 change.**
- `pure_proof.clj:1916` a permission given with the slot, straight beneath the act's own, on an empty place
  **B1 form.**
- `pure_proof.clj:1917` a permission given with the slot beneath a permission that is not the act's own
  **B1 form.**
- `pure_proof.clj:1920` a tool's own fact under the bare word :permission, on an entity that holds a permission: no permission fact
  **B1 place.** "on an entity that holds a permission".
- `pure_proof.clj:1954` who may write the setting, the list and the marks: the holder of the lane's first permission, and no holder of a given one, whatever it allows
  **B3 owner.**
- `pure_proof.clj:1960` a setting, a list and a mark by a holder of a given permission are refused, each with that word; a fact of a tool by the same holder is taken
  **B3 owner.**
- `pure_proof.clj:1967` who ends a permission, when the act is Bob's, under his given permission: his own; one that he gave; not one beside his, not the one above his, not the lane's first
  **B2 end, B3 owner.**
- `pure_proof.clj:1981` the rows that a give needs beside its place: the places above the permission that its place holds, without those in hand; none for a give at an empty place or at an end, none for an end, none for a making act
  **B1 many.** No give is held against a give that its holder already has.
- `pure_proof.clj:1992` (1 of 2 sentences at this line) :alice :bob :softland.store/permission
  **B1 place.**
- `pure_proof.clj:1992` (2 of 2 sentences at this line) :alice :bob :softland.store/permission
  **B1 place.**
- `pure_proof.clj:2045` an accept of one value writes three rows: the fact, with the one text of its value, the time on the act, who wrote it and the permission that the act named; the answer, with the time on the act; the last stamp
  **B1 rows.**
- `pure_proof.clj:2063` the fact's row holds the text of the value in its normal form, whatever form its maker gave: its String as written out here, the value that it reads back as, its plain bytes, and the seven fields of the row
  **B1 rows.** "the seven fields".
- `pure_proof.clj:2069` the texts of who and of the permission are made once for the act and each row holds them: an act by a who that is a UUID, with a value and a retract
  **B1 rows, S1 party.**
- `pure_proof.clj:2075` an accept of a making act writes the depot, the first permission, the answer, the last stamp; each row holds who made the lane, the permission that the act named and the time on the act
  **making, B1 rows.** And the give's row by holder is one write more.
- `pure_proof.clj:2083` the system makes a personal lane for its person: the rows hold the system as who, and the person's permission
  **making, B1 rows.**
- `pure_proof.clj:2087` an accept of a retract and a permission given writes a row for each: the retract's has no value; each holds who, the permission and the time
  **B1 place, B1 rows.** A give writes its two facts and its row by holder.
- `pure_proof.clj:2097` a give with the slot writes the permission's whole text, slot and all: what the gate later compares with the permission that an act names
  **B1 form.** No text of a permission is compared.
- `pure_proof.clj:2101` :alice :bob :softland.store/permission
  **B1 place.**
- `pure_proof.clj:2102` a give of a permission 32 deep writes its whole text, as Clojure prints the permission, with its 32 vectors; the row holds that permission and not the one above it
  **B1 form.**
- `pure_proof.clj:2131` a setting and a list are written as any fact under a store key: the one text of the value, and no stamps of marked keys
  **B4 public.** "a setting" is who may see.
- `pure_proof.clj:2137` :alice :alice :softland.store/who-may-see
  **B4 public.**
- `pure_proof.clj:2407` the row holds a permission that says see, and the act names it without the slot
  **B1 form.**
- `pure_proof.clj:2409` the row holds another permission
  **B1 place.**
- `pure_proof.clj:2418` the base's making act for a base that is made: its permission stands, and it makes nothing
  **making.**
- `pure_proof.clj:2699` what a permission covers is its sixth slot, and what it allows its fifth: none for a permission with fewer slots; a run
  **B1 form.**
- `pure_proof.clj:2702` a permission sits on what it covers, under a key of its own for each kind: the lane's on its holder, a run's on the session
  **B1 place.**
- `pure_proof.clj:2704` :alice :bob :softland.store/permission
  **B1 place.**
- `pure_proof.clj:2705` a line of a lane's acts: a permission for a run covers the acts of that run by its holder, and no other run or who; it gives nothing, so its holder cannot give herself one for the lane beneath it; it writes no setting; it stands beside her permission for the lane; a run ends its own permission and no other of hers; an end of one leaves the others; a run is given one again. A holder ends a run's permission of hers under her permission for the lane. What was given beneath a permission for the lane, a run's too, ends with it. A run has one permission that stands
  **B1 many, B1 own end, B2 lent, B2 give.** "A run has one permission that stands"; "a run ends its own permission and no other of hers"; in B2 its holder may lend it to her own agent, so "it gives nothing" goes too.
- `pure_proof.clj:2824` a yes for one act sits on its own id, the uuid that its giver made, under the key of a yes; what it covers is [:act yes digest]
  **B1 place, B1 form.**
- `pure_proof.clj:2827` the digest that a yes holds is of the act as its maker made it, in its one form: 64 hex characters; the same for 5 and 5N, facts as a list or a vector, an empty part left out or given, with a permission or none, and as the thing arrives under any act-id with the time on it (the review's 1C, both halves)
  **B1 digest.** "with a permission or none".
- `pure_proof.clj:2833` it is the act's one form without its permission: what `softland.store.act/parse` gives, part for part, less the permission
  **B1 digest.**
- `pure_proof.clj:2849` a line of a lane's acts under yeses for one act: a try before the yes lands, one with another value and one in another run are refused and use nothing; the same thing again under its act-id is not decided; the act written another way is taken under any act-id of its maker's and uses the yes up, so the same act again is refused and the used yes has nothing to end; its id takes no give again. Two yeses for one act stand at once, and each is used once. A yes is ended by its giver, by one above the giver, and by its holder under her permission for the lane; never under another yes; it ends itself only when its act is its end, and that end is its use. A yes gives nothing, and a refused give uses nothing. A yes stands as any permission does: the end of a permission above it ends it, and the give again of the one above does not bring it back; its holder ends it all the same
  **B2 end, B1 own end, B2 give.** "A yes gives nothing": in B2 its holder may lend it to her own agent.
- `pure_proof.clj:2976` probe 6C, a holder ends her yes under itself: a yes for another act cannot, its act being another; a yes for the act that ends it does, and that end is its use, so the same end again is refused; a run's permission that says see takes no act, its own end neither
  **B1 own end.** "a run's permission that says see takes no act, its own end neither".
- `pure_proof.clj:3036` the words of the review's probe 2: an act whose digest is not the yes's, under a yes that is used and under one that is ended, is refused as not the act that the yes permits; an act of another run under a run's permission that is ended, as of another run; whatever the place of the permission holds (SPEC.md, R9)
  **B1 form.** The sitting and the digest are in the give's row; an act that names an id at an empty place has neither to be held against.
- `pure_proof.clj:3039` a yes that was never given, on the id of a used yes: with another digest it is refused as not the act that the yes permits; with the digest of the act, as a permission that does not stand, since the yes used there is the one yes given there, and never as ended
  **B1 form, B1 again.** An act names an id, so "a yes that was never given, on the id of a used yes" is the used yes.
- `pure_proof.clj:3296` what a gate may copy of them, by the proof's own filter: exactly the writes of a fact and of the facts by value, in their order, the row that is nil among them; they are the first five of the seven, each as it was written
  **table.** `copies-of` takes the rows of `gives-by-holder` too.
- `pure_proof.clj:3338` at the streaming gate, the making act of a group's lane
  **making.** The kinds of the writes of a making act: the facts of the first give and its row by holder.
- `pure_proof.clj:3350` at the microbatch gate, the base's making act
  **making.** As `pure_proof.clj:3338`.
- `pure_proof.clj:3649` a line into a base that is not made: an act before the making is refused; the first making act makes the base; a second one names a permission that is there and gives one that is not beneath its own; then an act, a give and two acts under it, one after the other, are accepted
  **making, B1 form.** "a second one names a permission that is there and gives one that is not beneath its own".
- `pure_proof.clj:3728` two writes of who may see the lane that name nothing are both accepted, and the row is the later one's
  **B4 public.**
- `pure_proof.clj:3734` a holder of see: her act is refused; naming her permission without the slot does not stand; her give is refused, so no permission is beneath a see
  **B1 form, B2 give.** "her give is refused, so no permission is beneath a see".
- `pure_proof.clj:4029` a fact under each of the five store keys is the maker's own object in the thing
  **B1 place, B4 public.** The store keys that the door's reading is held to.
- `pure_proof.clj:4148` the door comes back for a thing whose facts have no end, for a who that throws when it is hashed and for a permission that throws when it is read: it reads none of them, and builds the thing with each as given
  **B1 form.** "a permission that throws when it is read": `:permission` is an optional `:uuid`.
- `pure_proof.clj:4466` a sender has standing when the declaration took the thing and the act's permission stands and is hers: an accept, a refusal of the places, a holder who may only see, and a making act are kept up to 32 MiB; a permission that does not stand, ended, above ended, used up, of another holder or another run, a yes for another act, a lane not made or of the other gate, and a refusal of the declaration, up to 256 KiB
  **B1 again, B4 public.** The words of a standing that it counts off follow the gate's; in B4 a give held through everyone is hers too.
- `pure_proof.clj:4494` :a :asha 0000000000000000003
  **S1 party.** The key of a row by who holds the party's kind.
- `pure_proof.clj:4658` the first holder writes everyone and members; a holder of a given permission is refused as for every setting; on another entity, with no value, with a word that is neither or a string, and as a marked key, each is refused with the word of the other settings or of its own
  **B3 owner.**
- `pure_proof.clj:4669` a making act that carries the key as a third fact is no making act: refused, the lane not made
  **making, B4 public.**
- `pure_proof.clj:4789` no permission
  **B1 form.**

## read_path_proof.clj

- `read_path_proof.clj:608` asked with both: they are the act's who and the permission that it named
  **B1 rows.** What is handed is the id of the give.
- `read_path_proof.clj:610` for the system's making of a personal lane the two differ: the system wrote the fact, under the person's permission
  **making.** The making act names no give.
- `read_path_proof.clj:622` the entity of a holder, asked without the store's own facts: nothing; with them: her permission
  **B1 place.** Her give is not on her own entity.
- `read_path_proof.clj:628` a run across the whole lane, after values were written: no permission, setting or mark in the answer that did not ask; with the store's own facts the lane's depot, its holder's permission and its marked keys are there, and no fact under any other key of the store
  **B1 place.** The store's own facts of a whole lane are under other keys.
- `read_path_proof.clj:818` a place through three lanes: three calls, in one round
  **S5 rights.** The count of calls: the rights of a lane are one query.
- `read_path_proof.clj:819` the same place asked once while a live read holds a push open on each of its sources: the same three calls in one round, and the same answer. A read asked once reads the tables and takes no value of a push
  **S5 rights.**
- `read_path_proof.clj:823` the same from a lane: three calls in two rounds, the start lane's call first
  **S5 rights.**
- `read_path_proof.clj:824` an entity of five keys: one call
  **S5 rights.**
- `read_path_proof.clj:825` a run across entities of the base, from the base itself: one call, in one round, with the rights in it
  **S5 rights.** "with the rights in it".
- `read_path_proof.clj:826` the same run through her own lane and the base: two calls in one round, one for each lane
  **S5 rights.**
- `read_path_proof.clj:827` what points at a thing through three lanes: the rights, the orders of the two lanes that mark the key, the checks in the two nearer lanes
  **S5 rights.**
- `read_path_proof.clj:868` with a first read of 4 places and at most 10 places for one run: a run of 3 places is one call
  **S5 rights.** The count of calls.
- `read_path_proof.clj:870` a run of 8 places is three calls: the first read, the size by a bounded walk, the run whole
  **S5 rights.**
- `read_path_proof.clj:871` a run of 12 places is two calls, and then the word, with no place
  **S5 rights.**
- `read_path_proof.clj:873` a run of 12 places in a lane that the asker may not see: one call, no size, no word; the lane is left out
  **S5 rights.**
- `read_path_proof.clj:876` the widest thing that a query can name, a whole lane, ends in the word within two calls
  **S5 rights.**
- `read_path_proof.clj:907` asked once, from the base itself: the twelve places come whole from one task, in one call under the base's routing id
  **S5 rights.**
- `read_path_proof.clj:909` kept live: the same answer now, and two pushes, the run and the base's own rows, both under the base's routing id
  **S5 rights, B4 public.** The pushes of a read of the base.
- `read_path_proof.clj:1041` each of those reads made one call on the base's facts, under the routing id of the lane that it starts from, so on that lane's task; none was made under the base's routing id
  **S5 rights.**
- `read_path_proof.clj:1044` bob, through his own lane, is given the same place, by one call on the base's facts under his lane's routing id
  **S5 rights.**
- `read_path_proof.clj:1047` a run across entities of the base comes back whole from the one task of each asker's lane: the places that the base's own task gives, by one call on the base's facts under her lane's routing id
  **S5 rights.**
- `read_path_proof.clj:1050` what points at a thing in the base, its order by value, comes back whole from that one task too: the places that the base's own task gives, by one call on the base's facts, for its rights and its marks, and one on its facts by value, both under her lane's routing id
  **S5 rights.** "for its rights and its marks".
- `read_path_proof.clj:1058` kept live by two askers whose lanes are on two tasks, and by one who reads from the base itself: each has the value now; the three hold 12 pushes, and the base's row has three of them, one under each read's home
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1075` one asker through her lane holds 3 pushes on that lane's facts and 2 on the base's, its own rows and the place, counted under her lane's routing id; a second asker with the same home adds his permission in that lane and a push of his own on the row above it, which he reads after his own row, and shares the two pushes on the base
  **S5 rights, B1 place.**
- `read_path_proof.clj:1087` the first asker of a home leaves: the pushes on the base that the other asker of that home holds stay, and so do his own two in the home, on his permission and on hers above it, where her own push on her permission is closed; he leaves: they are gone, and the other home's stay; the last leaves: nothing is held
  **S5 rights, B1 place.**
- `read_path_proof.clj:1298` the base's setting goes to members under the open read of an asker who holds no permission there: the base is left out, and she is given no fact
  **B4 public.** "the base's setting".
- `read_path_proof.clj:1364` the permission above hers ends under her open read: she is told a whole answer that says that the lane is left out, with what now wins the place and nothing of the lane's facts; a read asked once gives the same
  **B2 lent.** In B1 it holds as worded.
- `read_path_proof.clj:1369` the one above her is given the same permission again: hers does not come back with it. Her read is given nothing, and a read asked once still leaves the lane out
  **B1 again, B2 lent.**
- `read_path_proof.clj:1507` the review's receipt 3: a live read in a run, of a lane that she may not see, is handed nothing while another holder's permission for her run is given and ended, as a witness of the same push was told both; and the read path holds no push of the row above that permission
  **S5 rights, B1 place.** Another holder's give for her sitting is under that holder, so her read never watches it.
- `read_path_proof.clj:1510` the review's receipt 4: the longest live read, from a start lane through two more in a run, with one write while it opens, opens with an answer, within the rounds that it is given
  **S5 rights.** The rounds of a live read.
- `read_path_proof.clj:1582` the review's P1 on the cluster: an asker who holds a live read on a lane that she may not see, through a chain whose permission above ended, is handed nothing when the giver beneath it is given hers again and then, in one act, gives the asker a permission beneath it and ends it; another asker holds the lane's fact open on the same read path, and a read path of its own is played too; a read asked once leaves the lane out
  **B1 again, S5 rights.** The case rests on a give made again under one id, and on a row above read after her own.
- `read_path_proof.clj:1585` the same for the asker's run: the giver gives her run a permission beneath hers and ends hers in one act, and the asker who asks in that run is handed nothing
  **B1 again, S5 rights.**
- `read_path_proof.clj:1588` the review's P2 through the door: an act whose digest is not the used yes's is refused as not the act that it permits; a yes that was never given, on the used yes's id, as one that does not stand; an act of another run under a run's permission that ended, as of another run
  **B1 form, B1 again.** As `pure_proof.clj:3036` and `3039`.
- `read_path_proof.clj:1689` one asker of a place in a members lane holds three pushes: the lane's own rows, her permission, the place
  **S5 rights, B1 place.**
- `read_path_proof.clj:1691` a second asker of the same place joins the two pushes that are open, and opens his own permission and a push of his own on the row above it, the first asker's permission, which he reads after his own row
  **S5 rights.**
- `read_path_proof.clj:1699` the first leaves: the two shared pushes stay, and so do his own two, on his permission and on hers above it; hers on her permission is closed; the last leaves: nothing is held; a read that has left is not open
  **S5 rights.**
- `read_path_proof.clj:1701` each push that the read path opened is closed when its last reader has left: Rama says terminated of all five
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1703` a third asker after the last one left opens new pushes, three, and gets a fresh value
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1704` what the read path holds for a live read and for a push: who looks at what, what each asked for, the pushes, and one mark; no field for a row, a value or an answer
  **S5 rights.** "no field for ... an answer": by S5(a) the runtime holds the query's last answer; the contract marks that for the stop on caching.
- `read_path_proof.clj:1707` 50 times, two threads open the same thing at once: one opening for each thing, five pushes for the two, and each is closed when both have left
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1802` a read path that may hold 3 pushes on one table under one routing id: one live read of a place in a members lane holds 3 there, the lane's own rows, her permission and the place; the count is the count of the map's entries
  **S5 rights, B1 place.**
- `read_path_proof.clj:1807` after it was turned away, the read path holds what it held: the first read and its 3 pushes
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1810` a second live read of the same thing opens: it shares the 3 pushes, and a push that is open already is not counted again
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:1819` 20 times, eight threads each open a read of another place of one lane at once, where 3 pushes may be held: one opens and seven are turned away with the word, the count stands at the bound, and nothing is held when the one has left
  **S5 rights.** The count of pushes that one read of a place holds.
- `read_path_proof.clj:1822` the two tables of one lane are counted apart: what points at a thing through two lanes holds 3 pushes on the nearer lane's facts, its own rows, her permission and the one check; 2 on the farther lane's facts; and 1 on its facts by value, the order
  **S5 rights, table.** A third table is counted.
- `read_path_proof.clj:1829` a read that starts from the base holds the base's own rows and a place, both counted under the base's routing id
  **S5 rights, B4 public.**
- `read_path_proof.clj:1831` in the base the count is of the pushes of one home: two reads of two places through one home that is not made hold 3 pushes on the base's facts under that home, its own rows and the two places, and 1 on the home's own rows
  **S5 rights, B4 public.**
- `read_path_proof.clj:1842` the checks of one read that is kept live have a number of their own: where one such read may need 1 check, what points at a thing with two places in the farther order is given whole when asked once, and kept live it is turned away with the word; 5 pushes were opened, for the rights of the two lanes and the one order, none for a check, and each was closed
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:2065` the three reading calls of Rama's client are named in the read path's one file and in no other file of the store's source
  **S5 rights.** A call of a query is a fourth reading call.
- `read_path_proof.clj:2176` no call was made to Rama for a read that was turned away: the selects are none, and the pushes are the three of the one read that opened
  **S5 rights.** The count of pushes.
- `read_path_proof.clj:2440` a read of a holder's acts is four rounds of one call each: the last stamp, the rights, the line, the things
  **S5 rights.** The round of the rights is one query.

## read_path_pure_proof.clj

- `read_path_pure_proof.clj:1089` an asker that is no id
  **S1 party.** An asker is a party.
- `read_path_pure_proof.clj:1156` the read path's nine numbers, written out a second time: a read kept live may need 250 checks of nearer lanes where one asked once may need 40,000, and the read path holds 900 pushes on one table under one routing id, under the 1,000 that Rama opens on one table on one task; a read of acts gives 1,000 decisions at most, and things that weigh 32 MiB in all, the heaviest thing that a gate keeps
  **S5 rights.** A bound on the gives of one answer is a tenth number, if it is made a number of the read path.
- `read_path_pure_proof.clj:1209` a permission beneath another, in a lane open to its members: she may see the lane while each permission above hers stands with an older row. The one above ends: she may not. The one above is given again: hers is older, and she still may not. Hers is given again: she may. Her own ends: she may not. A row above that is not read: not known yet
  **B1 again, B2 lent, S5 rights.** No row is compared by its stamp.
- `read_path_pure_proof.clj:1228` an asker who asks in a run, in a lane open to its members: she may see it under the run's permission when it names her as its holder and stands; not when the query names no run or another run; not when the run's permission names another holder; not when it is ended, or the one above it is; and under her permission for the lane whatever her run holds
  **B1 place, S5 rights.** A sitting has no place of its own: her gives for it are found under her.
- `read_path_pure_proof.clj:1245` the standing: the seven lines of its table
  **B4 public, S5 rights.** The table of the standing.
- `read_path_pure_proof.clj:1254` what no fact reads as: a lane with no setting is members, the base is everyone, a list is nothing, no key is marked
  **B4 public.**
- `read_path_pure_proof.clj:1263` a setting that is written again with the same word: the row differs, and its meaning does not
  **B4 public.** "a setting" is who may see.
- `read_path_pure_proof.clj:1284` a setting that goes to members for an asker who holds a permission means another thing, though her standing stays yes
  **B4 public.**
- `read_path_pure_proof.clj:1314` a fact's step that sees the new setting first joins her permission, tells nothing of its place, and owes
  **B4 public, S5 rights.**
- `read_path_pure_proof.clj:1345` her permission: one row, on its holder; in the base under the home
  **B1 place, S5 rights.**
- `read_path_pure_proof.clj:1396` a source of a streaming lane is the same under every home: the lane's own rows, a permission, a place, a check, a run and the order by value
  **B1 place, S5 rights.** "a permission" as a source.
- `read_path_pure_proof.clj:1412` each word reads the base as one fact source, and the base's rights as two: its own rows and her permission
  **S5 rights.**
- `read_path_pure_proof.clj:1421` two reads with one home have equal sources for the base: a list, a longer list and a start lane whose list is the longer one; two askers with one home share the base's own rows and the place, and each has her own permission row
  **S5 rights.**
- `read_path_pure_proof.clj:1445` which sources are a lane's rights, and which a check, in a streaming lane and in the base under a home
  **S5 rights.**
- `read_path_pure_proof.clj:1477` asked once: a place in one lane is one call in one round; through three lanes three calls in one round; from a lane one call and then two
  **S5 rights.** The count of calls.
- `read_path_pure_proof.clj:1480` asked once: an entity of the base is one call, with the rights in it; a run across entities of the base is one call too, and so is a whole lane
  **S5 rights.**
- `read_path_pure_proof.clj:1483` asked once through her own lane: the base is one call more, under her lane's routing id, for a place, an entity, a run across entities and a whole lane
  **S5 rights.**
- `read_path_pure_proof.clj:1487` asked once: what points at a thing through three lanes is three rounds, the rights, the orders, the checks; from a lane four
  **S5 rights.**
- `read_path_pure_proof.clj:1490` kept live: a place through an everyone lane opens in two rounds; through members lanes in three; what points at a thing through a given list in four. From a start lane that is members it is six: the list of a start lane is read only when she may see the lane, and that needs her permission, one round after the lane's own rows. None takes more than six
  **S5 rights, B4 public.** The rounds of a live read.
- `read_path_pure_proof.clj:1497` the worked case of the plan: a place kept live through three lanes, of which two are members, holds eight pushes
  **S5 rights.** The count of pushes.
- `read_path_pure_proof.clj:1499` a read asked once wants the place with the rights, in its first round; a live read does not
  **S5 rights.**
- `read_path_pure_proof.clj:1503` with the one predicate of that choice giving false, a read asked once reads the rights first, and gives the same answer
  **S5 rights.**
- `read_path_pure_proof.clj:1509` the calls of one round: a lane's rights and its fact are one call, in the base under the home's routing id
  **S5 rights.**
- `read_path_pure_proof.clj:1513` the calls of one round through her own lane: one call for each of the two tables, both under her lane's routing id, each with the rights and the fact
  **S5 rights.**
- `read_path_pure_proof.clj:1570` asked once, where every permission of the asker's was given her: a place through a given list settles in 2 rounds and from a lane in 4; what points at a thing through a given list in 4 and from a lane in 6, the most that the chains of a read's sources give
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:1573` kept live, the same reads open in 4, 7, 5 and 8 rounds: a lane's rights take one round more than asked once, since her permissions are joined only when its own rows say that it is open to its members; 8 is the most that the chains give
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:1576` asked once, a write before any round of the longest read lengthens nothing: each source is fetched once, so the read settles within 6 rounds whatever is written
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:1580` kept live, a list that gains a lane at the round at which the longest read would open asks 5 rounds more, the rights, the order and the checks of that lane: 13, within the 16 that a live read is given; none of the other writes, made before any round, asks more
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:1585` asked once: in a lane where she holds no permission, a read in a run whose permission is hers is one round more than the same read in no run, since the row above the run's permission is read after it; a read in a run whose permission is another holder's is not, since no row above it is read
  **S5 rights.**
- `read_path_pure_proof.clj:1588` asked once: where her permission was given her too, the rows above her run's ride the round that reads those above hers, one call each: rows more, and no call more
  **S5 rights.**
- `read_path_pure_proof.clj:1729` the store's own facts are left out of a run unless the query asks: the entity of a holder, and of the lane itself
  **B1 place.** "the entity of a holder".
- `read_path_pure_proof.clj:1800` :team :alice :softland.store/permission
  **B1 place.**
- `read_path_pure_proof.clj:1801` :team :bob :softland.store/permission
  **B1 place.**
- `read_path_pure_proof.clj:1803` what the base's own rows and her permission mean at a change, for a read through her own lane: each is read beside the other, which the read holds under her lane's routing id
  **S5 rights.**
- `read_path_pure_proof.clj:1814` what the row of a permission above hers means at a change, the row read under the source that her own row names: the end of the one above hers means that she holds none that stands; a change of another holder's permission, which is not above hers, means the same before and after
  **S5 rights, B2 lent.**
- `read_path_pure_proof.clj:1849` a live read in a run, of a lane that she may not see, is handed nothing in any order of up to three writes there: another holder's permission for her run, its end, the end of the one above it and its give again, a fact; and each order ends on the screen that a read asked once gives
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:1852` her read holds the lane's own rows, her permission's place and her run's, and never the rows above another holder's permission for her run
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:1858` as the read path was before this build, with the rows above any holder's permission for her run wanted and every move of a source that is no check owing a whole answer, the same writes hand her an answer that says what it said: the review's 2A; and each of the two rules of this build is enough by itself to hand her nothing
  **S5 rights.**
- `read_path_pure_proof.clj:1861` one order of the review's history alone: the give to another holder and its end hand her nothing
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:1877` a live read in a run, of a lane that she may not see, whose own permission and whose run's are given her beneath one that ends in the same act, and ended: in every order of every list of up to three such writes and a fact, no step hands her anything, and each order ends on what a read asked once gives
  **S5 rights, B3 one act.**
- `read_path_pure_proof.clj:1879` the road that the generated histories found, alone: Bea ends her own and gives her one beneath it; and then its end by the first holder
  **B3 one act.** An act that ends a give and makes one lent from it is refused whole.
- `read_path_pure_proof.clj:1881` the player can fail: without the rule that a change of her rights in a lane that she was told she may not see tells nothing, steps hand her answers that say what they said; and with a read that keeps nothing of what it told, which steps then judge by the meaning before each change, an order in which two changes are taken before either is handled ends on a screen that the tables do not give
  **S5 rights.**
- `read_path_pure_proof.clj:1898` the review's road on every order: an asker who holds a read open on a lane that she may not see, beneath a giver who is given a permission again and then gives her one beneath it and ends it in the same act, for the lane and for her run, is handed nothing, with the first holder's give again and a fact of the lane among the writes, in every list of up to three; and each order ends on what a read asked once gives
  **B1 again, B3 one act, S5 rights.**
- `read_path_pure_proof.clj:1900` the player can fail: with the rows above hers read from sources that name no stamp, as before this build, an order of the same writes hands her the fact that was written after her permission ended
  **S5 rights.** No source names a stamp.
- `read_path_pure_proof.clj:1904` each row above a permission of hers names the stamp of her row that names it, and a read asked once reads a place above both her permission and her run's once, in one call
  **S5 rights.**
- `read_path_pure_proof.clj:1922` the yes is where the gate keeps it: on its own id, under the key of a permission for one act, and it holds a permission whose holder is the asker
  **B1 place.**
- `read_path_pure_proof.clj:1929` the read wants no row under the key of a permission for one act: her place, her run's place and the lane's own rows, and the place asked
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:2000` the review's probe 3, a live read from a start lane in a run with the farthest lane open to everyone: 7 rounds, within the 8 that the chains of its sources give
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:2002` the review's probe 6B, the same with the farthest lane open to its members: 8 rounds, the most that the chains give, and half of the 16 that a live read is given
  **S5 rights.** The rounds.
- `read_path_pure_proof.clj:2004` the review's probe 4: an asker who may not see the nearer lane, asking in her run, while another holder's permission for that run is given and ended there: no step hands her anything, in any order, and each order ends on what a read asked once gives
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:2008` the review's probe 6A, asked once: Cal, who holds no permission in the lane, in no run is one round; in her run, whose permission is beneath the first, one round more, for the row above it; Ada, the first holder, in a run whose permission is Dov's, is one round, as in no run: nothing above another holder's is read
  **S5 rights.**
- `read_path_pure_proof.clj:2087` a place through two lanes, the nearer with a setting and a permission or none: every list of up to three writes of the setting, the permission and the two facts, 620 lists, none with an order that ends wrong
  **B4 public.** "the setting" among the writes.
- `read_path_pure_proof.clj:2093` a place through a lane in which the asker's permission is two beneath the first: every list of up to three writes of the end of the one above hers, its second give, the end of her own, her own second give by either holder, and a fact, 258 lists, none with an order that ends wrong; the same for an entity and for what points at a thing under a key that no lane marks, every list of up to two, 42 lists each
  **B1 again, B2 lent.** "its second give" is another give; the count of lists follows.
- `read_path_pure_proof.clj:2097` the player can fail. With the row of a permission above hers taken as a row that means nothing for her rights, lists that end the one above hers end wrong for what points at a thing: no fact of the lane is held there, so nothing else tells her that the lane is left out
  **S5 rights.**
- `read_path_pure_proof.clj:2100` such a read holds the row of each permission above hers beside her own; when the one above hers ends, the read leaves the lane's fact, keeps the rows that could let it in again, and the asker's screen gives the farther lane's fact and says that the nearer lane is left out
  **S5 rights.**
- `read_path_pure_proof.clj:2114` a place through a lane that the asker sees under the permission of the run that she asks in: every list of up to two writes of the end of the run's permission, its second give, the end of the one above it and its second give, a permission for the lane given to her and ended, the run given to another holder, and a fact, 72 lists, none with an order that ends wrong; the same for what points at a thing under a key that no lane marks
  **B1 place, B1 again.** "the run given to another holder".
- `read_path_pure_proof.clj:2116` such a read holds the row of the run's permission and the rows above it, beside the place where her own permission would sit; when the run's permission ends, the read leaves the lane's fact and her screen gives the farther lane's
  **S5 rights, B1 place.**
- `read_path_pure_proof.clj:2153` a place through her own lane and the base, the base with a setting and a permission or none, each source of the base under her lane's routing id: every list of up to three writes of the setting, the permission and the two facts, 620 lists, none with an order that ends wrong
  **B4 public.**
- `read_path_pure_proof.clj:2157` in those worlds the read holds the base's own rows, her permission and the fact as sources of the microbatch gate under her own lane's routing id, and none under the base's
  **S5 rights.**
- `read_path_pure_proof.clj:2488` a holder beneath a permission that ended reads as one who may not see the lane: the rows above hers are read, and her permission stands only while each of them does
  **B2 lent, S5 rights.**
- `read_path_pure_proof.clj:2578` a read of acts takes six rounds at most: four for the lane's first holder, five for a holder given hers beneath another's and for an asker outside, six for an asker outside whose permission's rows above must be read
  **S5 rights.** The rounds.

## run_again_proof.clj

- `run_again_proof.clj:3189` in the line in which a permission ends: after the end the holder's act is refused :permission-ended and the act of the holder beneath :permission-above-ended; after the second give the holder's act is accepted and the one beneath is still refused; after his own second give his act is accepted; after he ends his own his act is refused, in each gate
  **B2 lent, B1 again.** It names the two words; in B1 it holds but for "the second give", which is another give.
- `run_again_proof.clj:3193` in the line that writes who may read the lane's acts: the first holder writes everyone; a holder of a given permission is refused; on another entity, with no value, and with a word that is neither, each is refused; members again is accepted, in each gate
  **B3 owner.**
