# Run 2 of 5: the act as it is kept

A review of 26 November 2026 with the skill `design-pitfalls`
(`.claude/skills/design-pitfalls/SKILL.md`). One reviewer on Fable at xhigh
effort wrote the report below, and it is kept word for word. The brief the
reviewer was given is at the end. The merge of the five runs is in
[merge.md](merge.md).

---

# Run 2: the act as it is kept (version, act-id, fact, a fact with no value, stamp, time)

Short names: `act.clj:59` means `/mnt/data/projects/Softland/src-store/softland/store/act.clj` line 59; the same base for `door/…`, `gate/…`, `module/…`, `record/…`, `read_path/…`. `R/` = `/mnt/data/projects/Softland/src-runtime/softland/runtime/`, `C/` = `/mnt/data/projects/Softland/src-command/softland/command/`, `W/` = `/mnt/data/projects/Softland/src-wire/softland/wire/`.
A cite marked **(grep)** is "located by grep, not reviewed": I saw the matched line only. Section 7 gives those lines.
"Thing", "try" and "send" are the code's words (a thing is what arrives on a depot). `:session` below is always the product's part of an act, never an agent session.

Summary of the view: stamp, time, fact and a fact with no value hold. The act-id does not: it names a try, it is the name other acts point to, and it carries the route. The thing on a depot is one map for two things the code itself counts as two. Three parts of the act have no job that the declaration says. Two small ones: version, and the name `:door-time`.

## 1. What I read, and what I did not

Read whole, myself:
- `/mnt/data/projects/Softland/.claude/skills/design-pitfalls/SKILL.md`
- root `CLAUDE.md`, the two paragraphs (as loaded in my context)
- `/mnt/data/projects/Softland/src-store/CLAUDE.md` (orientation only)
- `act.clj` (426), `door/additions.clj` (116), `door/declaration.clj` (629), `door/digest.clj` (294), `gate/stamp.clj` (35). In `act.clj` and `declaration.clj` I read the permission parts and the store keys to follow the file and list nothing for them.

No gatherer. Every grep was mine: `grep -rnE --include='*.clj'` over `src-store src-wire src-command src-runtime src-auth`, with `tables.clj`, `raw-*` and `runs` excluded, matched lines only, no context lines. Patterns: version; door-time / time-part / usable-time / claimed-when / currentTimeMillis; the word "time" in src-store; act-id / act-uuid; `:depot` / depot-at-making / gate-depot; based-on / because-of / subjects / replaces; subject; retract / no value; stamp; digest / not-mine; `(:lane act)`, `(:facts act)`, `(:e fact)`; destructured reads of act parts (none found); a few phrase greps in `door.clj`, `walk.clj`, `decision.clj`, `sign_in.clj`, `rows.clj`. One file listing with line counts (`find | wc -l`) touched every `.clj` of the five trees for its count, `tables.clj` included: the count only.

Not opened, and what each would have settled:
- `door/text.clj`: what "the maker's syntax" reaches in a digest, and whether a map's text depends on how she built it.
- `door.clj`, `door/words.clj`: what the door does at a second send; the series; the answer words.
- `module/walk.clj`, `gate/decision.clj`: what a gate does with a second thing under one act-id; every use of the stamp.
- `gate/first_act.clj`: what `depot-at-making` is.
- `record/rows.clj`, `read_path/acts.clj`, `read_path/answer.clj`, `gate/chain.clj`: the keys built on a stamp, the act-id as given back at read, who is shown a stamp.
- `R/seam.clj`, `R/sign_in.clj`, `C/main.clj`, `W/words.clj`.
- `src-store/README.md` (run 5), the record (bound 2), the proposal page cited at `declaration.clj:292`.

Forks I took:
1. The skill's question 9 says to open Sid's line; your brief forbids it. I followed the brief.
2. The depot is run 3's, but the act-id's second slot holds a depot word. I list the act-id's job of routing and stop there; what a depot is, and whether a lane's depot can change, I point to run 3.
3. The digest is not in the owned list but is "the act as kept": it is in the inventory under "others". The digest a yes holds is described for question a and not judged.
4. "Time" I took as both `:door-time` and `:claimed-when`.
5. The greps returned several hundred lines. Section 7 lists every line this report stands on; the rest I give by file, not line by line.
6. Bound 4 says a finding that needs an outside file goes under "still open"; item 4 says to mark "depends". Under "What breaks" each finding says which part stands on my reads alone; the dependent part is repeated under "still open" with whose it is.

## 2. The inventory

### version (`act.clj:29-31`, `:59`)
- One job: says which edition of the list of an act's parts the maker wrote, so that a gate that does not know it refuses the act `:unknown-version`.
- Written by each maker, as the literal 1: `C/main.clj:311`, `R/sign_in.clj:60`, `:149`, `:280` (grep). Read at `act.clj:59` only, by `parse` (`act.clj:393-400`) and by `check`, which takes the maker's parts from `act.clj` (`declaration.clj:266-270`).
- Decided at write. Never changes. One per act. Nobody changes it. Both digests cover it.

### act-id, `[lane depot uuid]` (`act.clj:78-82`)
1. Names one try: the thing a gate answers once. `act.clj:79-80`; `declaration.clj:529-531`; `digest.clj:17-18` ("a gate keeps the digest of the first thing to arrive"); `module/walk.clj:23`, `:173-177`, `record/rows.clj:43-44` (grep).
2. Carries the route: the first slot is the lane, the second says which depot the door appends to and which gate answers. `act.clj:79-80`; `additions.clj:39-41`, `:66-69`, `:114`; `declaration.clj:550`; `door.clj:362` (grep).
3. Names the act for other acts: `:because-of` (`act.clj:65`); first slot of a fact-id in `:replaces` (`act.clj:76`, `:84-86`) and in the keys of `:based-on` (`act.clj:64`).
4. Lets a second send of the same thing read the first one's answer. `digest.clj:16-18`.
5. Is inside the digest of a thing and outside the digest that a yes holds. `digest.clj:6-14`.
6. Read by the rules `:depot-is-the-act-ids-depot` (`declaration.clj:441-442`) and `:no-fact-replaces-a-fact-of-its-act` (`declaration.clj:519-521`), the stray tests (`declaration.clj:545-552`), the id limit on its lane slot (`declaration.clj:414-418`).
- Written at `additions.clj:112-113`. Three hands make it: the uuid is the door's (`act.clj:78` "The door makes it") or, over the wire, the maker's (`R/seam.clj:45-46`, `W/words.clj:18`, grep); the lane slot is the act's `:lane`; the depot slot is what the door read of the lane, or `depot-at-making` (`door.clj:156`, grep).
- Decided before the gate. Never changes. One per thing; many things can carry one act-id, and the first decided has the answer; one act as its maker made it can go under many act-ids (`act.clj:20-21`).
- At read it is put together again from the lane, `depot-at-making` and the uuid that a row keeps: `read_path/acts.clj:181`, `read_path/answer.clj:77` (grep).

### fact (`act.clj:70-76`) and fact-id (`act.clj:84-86`)
1. A fact says: at this entity and key of the act's lane, the value is this. `act.clj:71-76`; the place is lane, `:e`, `:k` (`gate/decision.clj:230`, grep).
2. It is the one unit in which a tool's data enters the store. `act.clj:9-10`, `:140-143`.
3. Under a key of the store's namespace it is a setting of the lane or a give of a permission. `declaration.clj:124-225`: run 3 and the permission session.
4. `:replaces` names the fact it takes the place of; a gate holds that to the place's current fact (`gate/decision.clj:249-250`, `record/rows.clj:133-136`, grep).
5. A fact-id `[act-id index]` names one fact by its position in the act's `:facts`; read's `:fact` gives it back (`read_path/answer.clj:72-99`, grep).
- Decided at write by the maker. Never changes. 1 to 256 per act (`act.clj:68`, `declaration.clj:27-29`, `:444-445`); one act names a place once (`declaration.clj:516-517`).

### a fact with no value
1. Under a plain key: a retract. `act.clj:70`, `:413-414` ("A fact that gave no `:v`, or a nil one, holds `:v nil` and is a retract").
2. Under a permission key: the end of the permission at that place. `declaration.clj:143-145`, `:472-473`: permission session.
3. Under the five settings: refused. `declaration.clj:146`, `:285-286`; `gate/decision.clj:387` `:store-key-retract-not-built` (grep).
4. In the declaration it keeps every rule of a store key (`declaration.clj:378`, `:383`, `:393`) and adds no bytes (`declaration.clj:575-576`); in the one form on arrival it is the fact whose `:text` is nil (`declaration.clj:617-622`). Its row is kept all the same (`record/rows.clj:96-97`, grep).
- Decided at write. Never changes. Note: `nil` is a value inside a collection (`act.clj:183`) and "no value" at the top of a fact.

### stamp (`gate/stamp.clj`)
1. The position of one decision in its lane: accepted or refused, the lane's last stamp plus one. `stamp.clj:2-10`, `:28-35`; written at `module/walk.clj:279-280` (grep).
Everything else I located is that position put to use (all grep, none reviewed): the key of the lane's line and of the rows by who (`record/rows.clj:369-370`, `:379-380`); the lane's last stamp as the bound of a read (`read_path/acts.clj:25-26`); "no gap" as the test that a read is whole (`read_path/acts.clj:32-33`); where a read goes on (`read_path/acts.clj:241-244`); since when a key is marked (`gate/decision.clj:437`, run 3); whether a permission was given after the one above it (`gate/chain.clj:83`, permission session); given back in the door's answer and with a read (`door/words.clj:109`, `:113`; `read_path/answer.clj:88`, `:99`).
2. `:carried-stamp` in `:based-on` (`act.clj:102-106`): the stamp of a fact as its maker saw it; a label, no gate reads it.
- Decided by a gate at the decision. Never changes. One per decision; two lanes can hold the same number (`stamp.clj:7-9`). Reads no clock.

### time
- `:door-time` (`declaration.clj:81-91`): a label of when the thing came to the door. Written at `additions.clj:112-115` from the door's clock (`door.clj:165`, `:358`, grep), or by anything else that appends, "what it will" (`declaration.clj:87-88`). Read: its form at `declaration.clj:269` (absent or wrong: `:no-door-time`); `usable-time` (`declaration.clj:93-106`) → `gate/decision.clj:50-57` → `module/walk.clj:189` → the answer's row, each fact's row, the rows by value, the row of a used yes, the line's row (`record/rows.clj:62-64`, `:107-109`, `:119-120`, `:308-310`, `:426-429`), all grep. Shown in the door's answer (`door/words.clj:110`, `:114`) and by `:with :time` on a read (`read_path/query.clj:136`, `read_path/answer.clj:70`), grep. The digest leaves it out (`digest.clj:47-62`); the weight counts it (`digest.clj:163-166`).
  Decided at each send; of two sends under one act-id the first decided one's time is kept. One per thing, many per act-id.
- `:claimed-when` (`act.clj:66`): an optional whole number. No sentence says what it is or its unit; the only words are "It reads no clock and gives `:claimed-when` no default" (`act.clj:420`). No writer and no reader in the five trees (grep). In both digests.

### others
- `:lane` (`act.clj:61`): which lane's gate decides the act (`additions.clj:38-41`); must be the act-id's first slot (`declaration.clj:551`); with `:e` and `:k` the place of each fact; the entity a lane's settings sit on (`declaration.clj:138-139`, run 3); the lane of the act's permission (`act.clj:130-132`, permission session). The lane's several jobs are the case the skill names in its question 1; I add nothing.
- `:depot` on the thing: the act-id's second slot once more (`additions.clj:114`). Its only readers are its shape (`declaration.clj:268`) and the rule that holds it equal to the slot (`declaration.clj:441-442`); no other reader in the five trees (grep). Run 3.
- `:based-on` (`act.clj:64`): the facts the act is based on, as a label; up to 1,024 (`declaration.clj:31-32`). No reader located.
- `:because-of` (`act.clj:65`): one act-id. No sentence in my reads says what it is for. No reader located.
- `:subjects` (`act.clj:67`, `:33-34`): up to 256 ids. No sentence in my reads. Written at `R/sign_in.clj:67`, `:154`, `:285`, `C/main.clj:431` (grep). No reader located.
- The digest of a thing and the digest a yes holds: see question a in section 5. Kept in the answer's row and the line's row (`record/rows.clj:63`, `:427`, grep) and in a maker's receipt (`door.clj:281`, grep).
- The refusal words (`act.clj:148-156`, `declaration.clj:331-339`) and the stray words (`declaration.clj:321-329`): one word per fault, kept in answers.
- `:who`, `:session`: run 1. `:permission`: permission session.

### What is fixed for good
- Stored in each thing on a depot, as given (`additions.clj:12-13` "a depot keeps it for good"): the names and forms of the ten parts and the door's three; `:version 1`; the form `[lane depot uuid]`, also inside other acts' `:replaces`, `:based-on`, `:because-of`; the name `:door-time` and its unit; "no `:v`, or nil, is a retract"; the carried stamps.
- Stored in rows (grep, not reviewed): the stamp, the time, the digest as hex, the act's uuid and the fact's index.
- Shown: the receipt `{:act-id :digest :ack}`; the answer with its stamp, time and reason word; `:not-mine`; `:fact`, `:stamp`, `:time` with a read.
- Written on a page as decided: nothing in my five code files says so. What Sid was shown and answered: not available to this run.

## 3. What holds

- **Stamp and time are two things and are kept two** (questions 1, 2, 4). "Order never uses the time" (`stamp.clj:18`); the time is out of the digest and out of every rule. An order is the one thing an act must have decided as it passes, and it is the only thing the stamp is. Because refused things are numbered too, a gate that runs again gives the same stamps whatever it decides (`stamp.clj:5-10`).
- **Stamp and act-id are two names for a decided thing, made at two moments**, before the decision and by it, and neither stands in for the other (question 2). `:based-on` is not used for order (`stamp.clj:22-26`).
- **A fact with no value** has one meaning per kind of key, and where none is built it is refused, not given a meaning by default (questions 1, 2).
- **Kinds are values, not forms** (question 3): "a tool adds facts, never a part" (`act.clj:140-143`); each limit is one number in one place.
- **The finest grain is kept** (question 6): the depot keeps each thing as given; a fact is the smallest unit and has a name; an act is taken or refused whole (`act.clj:6-7`).
- **`:replaces`** is one job seen from two sides (the write holds only if that fact is current; this fact takes its place). They cannot come apart.
- **The two digests** are named apart and have two functions. I tested the limit the file states (`digest.clj:52-57`, a thing with no time and the same thing with one have one digest): it needs someone who appends without the door and already has her act-id and her whole thing, and what she loses is that one act-id.
- **The declaration** is data in one place; the arrived shape is built from the maker's, not written twice (`declaration.clj:266-270`).
- Two notes, not findings. `:id-too-long` is a word of both closed sets (`declaration.clj:319`, `:327`); the two readers never meet. In the one form on arrival `:v` is nil for every fact under a plain key (`declaration.clj:562-564`), so there `:text`, not `:v`, says whether a fact has a value; that is a shape inside code, not stored.
- Questions 7, 9 and 10 could not be asked here: nothing in my reads is put to Sid, the record is closed to this run, and no plan of a fix is in my reads.

## 4. What breaks

Order: all five are already in the form of stored things, so I ordered by how far each form has spread (into other acts, onto the wire, into makers' hands).

### 1. The act-id: the name of a try, the name other acts point to, and the route

Jobs: (a) names one try, which a gate answers once, and the answer stays when it is a refusal; (b) is how another act points at this act and its facts; (c) carries the lane and a depot word, for routing.

> `;; [lane depot uuid]. The door makes it. The lane and the depot are there so that the act can be routed and the door can find its answer in the right record.` (`act.clj:78-80`)

How they differ:
- **(a) and (b) differ in how many.** One act as its maker made it can have several tries: "So a yes is made before the act has an act-id, and the act goes under any act-id of its maker's" (`act.clj:20-21`). A refusal stays as the act-id's answer: `digest.clj:52-57` says a later send "under the same act-id" reads the earlier refusal as its own. But another act points at it by one act-id (`act.clj:64-65`, `:76`, `:84-86`). So when a try is refused and the act goes again under a new act-id, every act that named the old one has to be made again; and a yes for such an act no longer names it, because the digest a yes holds covers `:replaces`, `:based-on` and `:because-of` (`digest.clj:172-183` take out the permission and the door's three parts only). The code already needed the act apart from its try, for the yes, and got it by a digest; pointers between acts did not get it.
- **(c) differs from both in who decides it and in whether it can change.** The name is the maker's or the door's and never changes. The depot word is a setting of the lane. Today one thing holds it three times, with tests to keep them equal: the slot, the part `:depot` (`additions.clj:114`), and the depot it lies on (`declaration.clj:441-442`, `:550`). It also makes a fault of its own: "An act-id of the lane whose depot slot is a depot word and not the lane's depot passes: the thing then goes to the depot that the act-id names, and a gate refuses it there" (`additions.clj:66-69`). And a maker who writes `:replaces` or `:because-of` has to write the depot word of the act she points at (`act.clj:82`, `:86`).

Cost to separate now: the form `[lane depot uuid]` is in each thing on a depot, inside other acts, in the digest of a thing, and on the wire (`W/words.clj:132`, grep). It is a change of the form of a stored record, so it is Sid's, and from then on both forms are read.
Cost later: every further act, and every pointer and yes made on it.

Certain from what I read: the three slots and their stated reason; the three copies and their tests; the fault at the door; that one act goes under many act-ids; that a refusal stays; that pointers and the yes's digest hold whole act-ids.
Depends on what I did not read: (i) how often the count bites. The case I can name is a series whose act-ids are made ahead so that a later act names an earlier one before it is sent (`door.clj:258-261`, grep), and the flow "A try that is refused … is sent again under a new act-id" (`gate/decision.clj:178-179`, grep). A refused try wrote no fact, so nothing valid points at its facts otherwise. (ii) Whether a lane's depot can ever change: run 3. What I located: a gate refuses a change with the word `:changing-a-depot-not-decided` (`gate/decision.clj:388`, grep), and two reads already draw the slot from the lane alone (`read_path/acts.clj:181`, `read_path/answer.clj:77`, grep).

### 2. The thing on a depot is one map for two things

Jobs: it holds the act as its maker made it (ten parts), and it holds the door's record of one send (act-id, depot, time).

How they differ: in who decides (the maker; the door), in when (at making; at each send), and in how many: "a maker's second send of one act under one act-id carries another time" (`digest.clj:16-17`). The code says they are two: "Two digests, of two things" (`digest.clj:6`).

What the one map costs today is that each reader tells the two apart by a list of names:
- the digest of a thing takes `:door-time` out (`digest.clj:59-62`), with a special case: "Only the top-level `:door-time` of a plain map is taken out; a `:door-time` deeper in a thing stays" (`digest.clj:18-20`);
- the digest a yes holds takes four names out (`digest.clj:180-183`);
- `parse` refuses the door's three as `:unknown-part` (`act.clj:417-418`), the door refuses them `:add-its-parts` (`additions.clj:55-63`), and `check` demands them (`declaration.clj:266-270`);
- `:version` is one of the maker's ten (`act.clj:59`), so the door's three have no edition.

Cost to separate now: the form of every thing on a depot, and both digests are defined on it. Nothing is lost today: a depot keeps each send whole.
Cost later: the digests are kept (answers, receipts, the sixth slot of a yes); a fourth part of the door's changes what each list takes out, with nothing on the thing to say which edition of the door wrote it.

Certain from what I read. It is the weaker of the two structural findings: the cost is in code and in the digests' definitions, not in anything a reader of the store sees today. It is the same split as 1(a) seen from the stored shape.

### 3. Three parts of the act have no job that the declaration says

By the skill, a job that cannot be said in one plain sentence is a finding by itself. From my reads I cannot say these:
- `:claimed-when` (`act.clj:66`): no sentence, no unit. No maker in the five trees writes it and nothing reads it (grep).
- `:because-of` (`act.clj:65`): no sentence. It takes one act-id, where `:based-on` takes up to 1,024 facts; nothing reads it (grep).
- `:subjects` (`act.clj:67`): no sentence in the declaration. The only one I located is in sign-in: "the act names her as its subject, so that what the log holds of her can / be found by her id." (`R/sign_in.clj:56-57`, grep). Nothing in the five trees finds anything by a subject (grep).

Beside them: `:based-on` has its sentence (`act.clj:102-104`), but each entry carries a stamp that its fact-id already fixes, written by the maker and read by no one.

Why it counts: the list of parts is closed (`act.clj:140-143`), all four are in both digests, and a depot keeps them for good. A form fixed with no reader has not been tried by one.
Cost now: saying each job, and the unit of `:claimed-when`, while almost nothing is stored under them. Cost later: `:subjects` is written already by sign-in and the command; whole numbers under `:claimed-when` with no stated unit cannot be told apart afterwards.

The silence is certain from what I read. "Nothing reads it" depends on the reach of my grep: the five trees without the store's tables; `src-server/`, `src/app/` and the rest were not looked at.

### 4. Version: its one job is not done in the case its name gives

> `"The edition of the list of parts. This code knows this one version only."` (`act.clj:29-31`)

`conform-parts` tests for a part the list does not know before it reads any part: `(not-every? (into #{} (map first) entries) (keys x)) [:no :unknown-part]` (`act.clj:245`). So an act of a later edition that has one part more is refused `:unknown-part`; `:unknown-version` comes only when the later edition has the same part names. The reason word goes into the answer and stays there.
Cost now: nothing is stored under it yet. Cost later: answers that old gates gave to newer makers hold the other word. Certain from what I read. Small.

### 5. `:door-time` is named for the door, and the floor holds only "a whole number not below zero"

> `Anything else that appends a thing writes there what it will: a gate knows that the part holds a whole number not below zero, and not who wrote it.` (`declaration.clj:87-89`)

The root page: "a floor mechanism should be named only for what it actually enforces". The part is also demanded (`declaration.clj:269`, refused `:no-door-time`) although "no rule reads it" (`declaration.clj:89`). Each fact's row takes the number as `:time` and a read shows it (grep), so what a reader is shown as the time of a fact is the door's clock or any appender's number, and nothing says which.
Cost now: the name is made in one place (`declaration.clj:81-91`), plus the reason word and one sentence of the door's words. Cost later: every thing on a depot holds the key. Certain from what I read. Small, and the docstring already says the gap.

## 5. What is still open

### Question a: what the digest covers
There are two digests (`digest.clj:6-14`).
1. **The digest of a thing** (`digest`, `hex-of`, `readable-hex-of`, `weighed`): SHA-256 over the one text of the thing as it arrived, with the top-level `:door-time` taken out and nothing else (`digest.clj:59-62`, `:71`). So: `:session` yes, `:who` yes, the act-id yes (all three slots), and also `:depot`, `:version`, `:lane`, `:permission`, `:based-on`, `:because-of`, `:claimed-when`, `:subjects`, `:facts` and any part the form does not know. It is of the thing as given, not of the one form: "it holds the act-id, the depot and the maker's syntax" (`digest.clj:7-8`). How far "syntax" reaches is `door/text.clj`'s, which I did not read; `door.clj:304` (grep) says a list and a vector of the same facts get two digests.
2. **The digest that a yes holds** (`hex-for-a-yes`, `hex-of-the-act`): over the one form that `parse` gives, with `:act-id`, `:depot`, `:door-time` and `:permission` taken out (`digest.clj:172-183`, `:195-199`). So: `:session` yes, `:who` yes, the act-id no. `act.clj:17-19` says the same.

### Question b: a second send
**Same act-id, everything else the same.** From my reads: the door's reading (`additions.clj:78-87`) tests only that the thing is a plain map, holds none of the door's three parts, and that the act-id is of the thing's lane; it reads no record. So the second send passes and is made ready with a new time (`additions.clj:112-115`). Its digest is the first one's, because the time is out (`digest.clj:16-18`). Past my reads (grep): the door "keeps nothing between two calls" (`door.clj:40`) and leaves a thing on the depot "for each try, and one answer for each act-id" (`door.clj:12`); the gate does not decide it again: "One act-id has one answer: that of the first thing that the gate decided" (`module/walk.clj:23`), "A thing under an act-id that has an answer: 1 read, no write." (`module/walk.clj:38`). So it meets: a second thing on the depot, no second stamp, no row, and the first one's answer read as her own, accepted or refused, with the first one's stamp and time (`door/words.clj:84-85`, `door.clj:316-318`, grep).

**Same act-id, a different `:session`.** Certain from my reads: `:session` is a top-level part, so this thing has another digest. Past my reads (grep): it is appended, and not decided if the act-id has an answer; asked with its own digest, the record says `:not-mine`, "the record holds an answer under the act-id whose digest is not the digest given; it says nothing more" (`door/words.clj:87`, `:105-106`). She learns that something was decided under her UUID and not what; with the first receipt's digest she can still ask. If the thing with the other session arrives first, it is the one decided and the original reads `:not-mine`. For run 1: `C/main.clj:382` (grep) writes `:session` from `hello` into each act, so a second send with the same UUID after the session changed lands here.

### Sid's (through run 5; none of it checked against the record)
- Whether a refused thing is an act in his sense. The store gives it an act-id, a stamp and a row (`stamp.clj:5`; `record/rows.clj:58-59`, grep: "one row for every act for ever").
- Whether one act sent under two act-ids is one act or two. The store has two of everything for it and one digest for a yes.
- What he means by a stamp. The skill page quotes "we have the stamps for each act right?" (SKILL.md:65-66); the built stamp is for each decision, refused ones too.
- Whether `nil` at the top of a fact should be "no value". A maker whose value comes out nil retracts the fact and is not told.
- Whether `:claimed-when`, `:because-of`, `:subjects`, `:based-on` were his.

### Run 3's
- Whether a lane's depot can change, and what `depot-at-making` is (finding 1c).
- The words `:time`, `:stamp`, `:fact` of an answer, and the name `:time` in a row against `:door-time` on the thing.

### Run 4's
- On the wire the key `:act-id` holds a UUID when an act is sent and a whole `[lane depot uuid]` when its answer is asked: `W/words.clj:125`, `:132` (grep).

### Run 1's
- `:who` and `:session` are in both digests (question a); the second send after a change of session (question b).

### The permission session's
- The digest a yes holds covers `:session`, so a yes names the act with its session.
- `gate/chain.clj:83` (grep) compares two stamps; `stamp.clj:25-26` says a stamp of one lane is no measure against another's. `gate/decision.clj:273-274` (grep) looks like it keeps both in one lane; I did not read it.
- A sender gets the stamp of a refused thing in her answer (`door/words.clj:113`, grep), and a stamp is a count of every decision in the lane; the read of a lane's acts keeps stamps from an asker outside (`read_path/acts.clj:115-117`, grep). Whether a sender with no standing gets that answer is not in my reads.
- `max-depth` is one number for how deep a value goes and how deep a permission goes (`act.clj:36-39`, `:113-118`).
- A row with no value at a yes's place is its end or its use (`gate/chain.clj:12`, grep).

### No run named in my brief
- `door/text.clj`: the one text has no edition, and a digest's hex names none (`act.clj:227`). Version covers the list of parts only; the inside of a part has changed without it (`act.clj:362-364`, permission session). When a gate runs again under later code, the stamps stay the same and the verdicts and rows may not; `declaration.clj:288-295` treats one such case by hand.
- `R/sign_in.clj:56-57` says a subject is there so that what the log holds of her can be found. If such rows are ever taken out, the lane's line has a gap that the read calls a fault (`read_path/acts.clj:32-33`, grep). A question, not a finding.

## 6. Pointers

**Bound 2 (not checked against the record: run 5).**
- `src-store/CLAUDE.md:7` "They follow Sid's word of 25 November 2026." No code in my reads stands on it.
- `src-store/CLAUDE.md:47` "'Act' is the word of this tree, and it is Sid's." Standing on it: the namespace and the shape `:act` (`act.clj:1`, `:57`), `:act-id` (`act.clj:81`), `:arrived-act` (`declaration.clj:266`).
- `src-store/CLAUDE.md:47-51`, "lane" and "layer". Standing on it: `:lane` (`act.clj:61`), the act-id's first slot (`act.clj:82`), "the position of one decision in its lane" (`stamp.clj:2`).
- SKILL.md:15 and its quoted lines of Sid's. I used one (SKILL.md:65-66) as a pointer only.
- My five code files: none says Sid decided anything or cites a "Decisions of …" entry (grep over the five). One cites a proposal page, `declaration.clj:292`; the order of the two rules at `declaration.clj:310-311` stands on it. Not opened.
- Outside my reads: `W/words.clj:33` (grep) cites "src-store/README.md, 12 October 2026, entry 7".

**Bound 6 (permission session).** `act.clj:14-23`, `:62`, `:88-100`, `:108-123`, `:125-132`, `:360-391`; `declaration.clj:40-59`, `:135-177`, `:184-213`, `:227-229`, `:306-311`, `:397-405`, `:438-439`, `:458-494`; `digest.clj:172-270` (described under question a, not judged).

**Bound 7.**
- Run 1: `:who` `act.clj:60`; `:session` `act.clj:63`, `:420-421`; run and session `act.clj:14-15`; `declaration.clj:110-120` (`usable-who`), `:415`; `digest.clj:242`.
- Run 3: the depot `act.clj:82`, `additions.clj:114`, `declaration.clj:178-183`, `:268`, `:271`, `:301`, `:441-442`, `:455-456`; builds-on `declaration.clj:68-71`, `:214-219`, `:312-313`, `:496-500`; marked keys `declaration.clj:73-77`, `:220-225`, `:314-316`, `:502-514`; words of a query and an answer `read_path/query.clj:136`, `read_path/answer.clj:70` (grep).
- Run 4: the door's "could not" words at `additions.clj:34`, `:51`, `:63`, `:73`, `:76`, `:81`, `:83`, `:99`, `:116`; `W/words.clj:125`, `:132` (grep).
- Run 5: as under bound 2.

## 7. The grep lines this report stands on (located by grep, not reviewed)

```
C/main.clj:311                 {:version 1
R/sign_in.clj:60               (cond-> {:version 1
R/sign_in.clj:149              :act {:version 1
R/sign_in.clj:280              {:version 1
C/main.clj:382                 {:act (assoc act :who (:who hello) :session (:session hello)) :act-id uuid})
C/main.clj:431                 :subjects #{who}
R/sign_in.clj:56-57            the act names her as its subject, so that what the log holds of her can / be found by her id."
R/sign_in.clj:67               (not= first-act/system holder) (assoc :subjects #{holder}))))
R/sign_in.clj:154              :subjects #{who}
R/sign_in.clj:285              :subjects #{person}
R/seam.clj:45-46               The act-id. A maker sends the random part, a UUID, with the act. The seam / asks the door for an act-id of the act's lane and puts the maker's UUID
W/words.clj:18                 nothing and writes nothing into them. The act-id is the maker's too: a
W/words.clj:33                 under its act-id (src-store/README.md, 12 October 2026, entry 7); a query
W/words.clj:125                [:act-id :uuid {:absent :act-id-not-given :reason :act-id-is-no-uuid}]
W/words.clj:132                [:act-id  :act-id {:absent :act-id-not-given :reason :act-id-has-no-form}]
door.clj:12                    depot for each try, and one answer for each act-id that it is sent under.
door.clj:40                    The door keeps nothing between two calls: no act-id that it gave, no
door.clj:156                   [:ok (or got (first-act/depot-at-making lane))])))
door.clj:165                   (System/currentTimeMillis))
door.clj:258-261               "The act-ids of a series in one lane, before the first of them is sent. / A maker may use an id of the series in a later act of it before anything / is sent: as `:because-of`, and as the act-id of a fact id in `:replaces` / and `:based-on`.
door.clj:281                   asked for ahead, or nil. Gives {:act-id :digest :ack}, or one of the
door.clj:304                   facts as a list gets another digest than one who gives the same facts as
door.clj:316-318               A second send of one act under one act-id carries the clock of the / second send. The digest leaves the time out, so both sends have one / digest and read one answer as their own; a gate decides the first of the
door.clj:358                   (let [built (additions/thing-to-append reading act-id (server-clock))]
door.clj:362                   depot (nth act-id 1)
door/words.clj:84              {:accepted    "the record holds an answer under the act-id whose digest is the digest given, and a gate accepted the act; it carries the stamp of the decision and the time on the act"
door/words.clj:85              :refused     "the record holds an answer under the act-id whose digest is the digest given, and a gate refused the act; it carries the gate's reason word, the stamp, and the time on the thing when it held a usable one"
door/words.clj:87              :not-mine    "the record holds an answer under the act-id whose digest is not the digest given; it says nothing more"})
door/words.clj:105-106         (not (and (string? digest) (= digest (:digest read)))) / {:answer :not-mine}
door/words.clj:109-114         (cond-> {:answer :accepted :stamp (:stamp read)} / (some? (:time read)) (assoc :time (:time read))) / (cond-> {:answer :refused :reason (:reason read) :stamp (:stamp read)} / (some? (:time read)) (assoc :time (:time read)))))
module/walk.clj:23             One act-id has one answer: that of the first thing that the gate decided
module/walk.clj:38             - A thing under an act-id that has an answer: 1 read, no write.
module/walk.clj:173-177        ;; 1. Check (a): an act-id that has an answer is not decided again. / (get *thing :act-id :> *act-id) / (nth *act-id 0 :> *lane) / (nth *act-id 2 :> *act-uuid) / (record/answer-key *lane *act-uuid :> *answer-key)
module/walk.clj:189            (decision/time-on *thing :> *act-time)
module/walk.clj:279-280        (local-select> (keypath :last-stamp *lane-text) $$task :> *last-stamp) / (stamp/next-stamp *last-stamp :> *stamp)
gate/decision.clj:50,57        (defn time-on / (declaration/usable-time thing))
gate/decision.clj:178-179      another act-id. A try that is refused writes no use, and is sent again / under a new act-id, since an act-id keeps its first answer. Two yeses for
gate/decision.clj:230          (record/place-key (:lane act) (:e fact) (:k fact)))
gate/decision.clj:249-250      (when-let [replaces (:replaces fact)] / (when-not (record/written-by? gate-depot (:lane act) (get rows (place-of act fact)) replaces)
gate/decision.clj:273-274      (not= (:lane act) (nth given 1)) :permission-fact-for-another-lane / (not= (:lane act) (nth given 2)) :permission-fact-granted-in-another-lane
gate/decision.clj:387          (and (declaration/store-key? k) retract?) :store-key-retract-not-built
gate/decision.clj:388          (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
gate/decision.clj:437          key's stamp: the stamp since which the order of the key is level. A
gate/chain.clj:12              A row with no value is an end, or for a yes for one act its use; a row
gate/chain.clj:83              (if (and upper-row (< (long (:stamp upper-row)) (long (:stamp row))))
record/rows.clj:43-44          "The depot slot of the act-id is not in the key: a gate answers only under / an act-id of its own depot. The lane is in it, although the uuid alone is
record/rows.clj:58-59          "An accepted act has no `:refused`, and a thing with no usable time no / `:time`: there is one row for every act for ever, so the row keeps the
record/rows.clj:62-64          [refused stamp time-millis digest] / (cond-> {:stamp (long stamp) :digest digest} / (some? time-millis) (assoc :time (long time-millis))
record/rows.clj:96-97          store key alike. A retract has no `:text`: its row holds no value, and is / there all the same, so that a later fact can replace the retract.
record/rows.clj:107-109        [fact act-uuid index stamp time-millis who-text permission-text] / (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp) / :time (long time-millis) :who who-text :permission permission-text}
record/rows.clj:119-120        [act-uuid stamp time-millis who-text permission-text] / {:by-act act-uuid :stamp (long stamp) :time (long time-millis) :who who-text :permission permission-text})
record/rows.clj:133-136        (let [[[named-lane named-depot named-uuid] named-index] fact-id] / (= lane named-lane) / (= gate-depot named-depot)
record/rows.clj:308-310        [who-text permission-text act-uuid index stamp time-millis] / {:by-act act-uuid :by-index (long index) :stamp (long stamp) / :time (long time-millis) :who who-text :permission permission-text})
record/rows.clj:369-370        [lane stamp] / (str (declaration/id-text lane) " " (stamp-text stamp)))
record/rows.clj:379-380        [lane who stamp] / (str (declaration/id-text lane) " " (declaration/id-text who) " " (stamp-text stamp)))
record/rows.clj:426-429        [act-uuid refused time-millis digest who weight not-kept] / (cond-> {:act act-uuid :digest (text/hex digest)} / (some? time-millis) (assoc :time (long time-millis))
read_path/acts.clj:25-26       Why in that order. The last stamp is read before the rights, and only / decisions up to it are given. A decision at that stamp or before was
read_path/acts.clj:32-33       decision's one write with the last stamp, the window holds a row for / every stamp up to it: a row that is not there is a fault and no race. A
read_path/acts.clj:115-117     every other one withheld, given as its stamp alone. An asker outside / lane, not its stamps and not its last stamp. A taken act is held to the
read_path/acts.clj:181         :act-id [lane (first-act/depot-at-making lane) (:act row)]
read_path/acts.clj:241-244     To an asker who may see the lane the stamps are consecutive from the / start, none above the last stamp, and `:next` is the start and the count / of what was given; she is at the end when `:next` is the last stamp and / one. To an asker outside, `:next` is the stamp after her last decision
read_path/answer.clj:70        (contains? (:with query) :time)       (assoc :time (:time row))))
read_path/answer.clj:77        [[lane (first-act/depot-at-making lane) (:by-act row)] (:by-index row)]))
read_path/answer.clj:88,99     (cond-> {:holds kind :lane lane :stamp (:stamp row)} / {:holds :value :value thing :lane lane :fact (fact-id lane row) :stamp (:stamp row)}))
read_path/query.clj:136        [:with             [:set-of [:enum :writer :permission :time] {:max 3}]
```

Searched with no hit outside my reads: `:claimed-when` (only `act.clj:66`, `:420`); a read of `:depot`, `:based-on`, `:because-of` or `:subjects` from an act, plain or destructured; `act/version`. The other hits of the stamp, act-id, digest and time greps (several hundred lines, mostly in `gate/decision.clj`, `record/rows.clj`, `read_path/acts.clj`, `door.clj`, `module/walk.clj`, `R/seam.clj`) I looked at as matched lines and do not stand on.

---

# Appendix: the brief this reviewer was given

You are doing run 2 of a five-run review of the Softland repository (/mnt/data/projects/Softland) with the skill `design-pitfalls`. Run 2 only. Runs 1, 3, 4 and 5 are other reviewers' and the merge is mine; do not do any part of them.

## Why this review exists

Sid, the founder, has to find out whether what earlier agent sessions built is right. The case that started it: the store's unit carried his word "layer" while meaning something narrower, and one id did several jobs; that was corrected to "lane". He now wants the same adversarial check on **the act as it is kept**: the form of an act, what is added to it at the door, how it is declared, its digest and its stamp. The job is to break the thing, not to confirm it, and not to redesign it. If it holds, say that it holds and what you asked of it.

## Read these first

- `.claude/skills/design-pitfalls/SKILL.md`, whole. Follow it: the inventory first, then the ten questions, the brake, and "How to answer".
- In the root `CLAUDE.md`: the paragraph "Defaults, not conflations" and the one after it ("The same holds between Sid and a session"). That is the rule being checked.
- `src-store/CLAUDE.md`, as orientation only. It is not under review; raise no finding on it.

## What you review (the whole of it)

All under `src-store/softland/store/`:

- `act.clj` (426 lines), **without the permission**. The parts on `:permission`, `:permission-id` and what a permission covers belong to a separate session. Read past them to follow the file; list no jobs for them and raise no finding on them.
- `door/additions.clj` (116)
- `door/declaration.clj` (629). Run 3 reads the same file for the store keys of a lane's settings (the depot, builds-on, marked keys). Those are run 3's: point to it.
- `door/digest.clj` (294)
- `gate/stamp.clj` (35)

Run 2 owns these: **version, act-id, fact, a fact with no value, stamp, time**. For each one you list every job it does. Also take into the inventory, under "others", any other key or record of the act as kept that no run owns (for example `:lane`, `:subjects`).

`:who` and `:session` on an act are run 1's: point to it and do not list their jobs. Two exceptions, because run 1 could not settle them from its files and they sit in yours. These are questions, not findings to confirm; answer each from your reads with file:line, or say what you did not read:

- a. What exactly does the digest cover? Is `:session` in it? Is `:who`? Is the act-id?
- b. What does a second send of an act meet when it carries the same act-id (the maker's UUID) and everything else the same? And when it carries the same act-id and a different `:session`?

A note on one word: in the skill and the root page, "a session" usually means an agent session that wrote code or pages. `:session` in the code is the product's own thing. Keep the two apart in your report.

## Bounds. These are strict; Sid asked for them in so many words

1. **Read only.** Write or edit no file anywhere: no notes on disk, no memory, no docs. No git command that changes anything. Start no process: no REPL, no tests, no proofs.
2. **The record is not yours.** Do not use memtool, the softland-memory tools, or recall, at all, for any reason. The skill's question 9 says to open the line Sid typed; in this run you do not. Where a docstring or page in your reads says Sid decided or called something, or cites a "Decisions of …" entry, write "not checked against the record: run 5" beside it with file:line, and list which code in your reads stands on it. Write nothing as Sid's decision yourself.
3. **Never read** `src/app/server/env.clj` or any `.env` file. Never open a `raw-` file under any `runs/` directory.
4. **Stay inside the reads above.** One allowance, so that the jobs of an id you own can be listed: you may *grep* the `*.clj` files of `src-store/`, `src-wire/`, `src-command/`, `src-runtime/` and `src-auth/` for where an id you own is read, and report each hit as "located by grep, not reviewed" with file:line and the matched line. Keep `src-store/softland/store/record/tables.clj` out of every grep (the store's tables are not in these runs). Do not open those files to read them through, and raise no finding on code outside your reads. Where a finding would need such a file, write it under "still open" and name the run that reads that file. If a file outside your reads looks necessary, do not open it: name it in the list of what you did not read and say what it would have settled. `src-store/README.md` is run 5's: do not open it.
5. **Never open**: `src-server/`, `src-cli/`, `src-server-browser/`, `src/app/` (the old server, the client, the renderer), `src-inland/`, the rig, `history/`, the store's tables, the plan for pieces 5 to 8.
6. **Permissions belong to a separate session.** It owns: who may act, who may see, who may read a lane's acts, end, yes, and what a permission covers. ("End" and "yes" are read here as the store's permission words.) Where you meet one of these, write one line: where you met it (file:line) and "permission session". Do not list its jobs and do not judge it.
7. **Ids of the other runs.** Where you meet one, write one line with file:line and the run number, and do not list its jobs:
   - run 1: person, root, agent, sign-in, session and run, `:who`
   - run 3: the depot (streaming or microbatch), builds-on, marked keys, the words of a query and of an answer
   - run 4: the words on the wire, the "could not" answers, the commands
   - run 5: the pages that say "Sid decided"

## Models, on Sid's instruction

You are the reviewer and the judgment is yours alone; it is never handed down. Where you need collection (every place an id is written or read, with file:line and the quoted line), you may hand that to a read-only gatherer on a lower model and effort: the Agent tool with `subagent_type` `Explore` or `code-hunter`, `model: "sonnet"`, `effort: "low"` or `"medium"`. Its brief must carry bounds 1 to 5 above, since it loads neither this brief nor the root page. A gatherer returns facts with file:line and never a verdict. Read for yourself every passage a finding rests on, and quote only what you yourself read. Your files are about 1,500 lines, so you may well need no gatherer; if you use none, say so.

## What to send back

Your final message is the whole deliverable, and it is all I will see, so make it complete. Plain words. Use Sid's word for a thing he has named (lane, act) and the standard term otherwise; add no vocabulary of your own. Give your view and its grounds. Name no fix beyond the split: the design is its author's and Sid's.

1. **What I read, and what I did not.** Each file, whole or in part. Any gatherer used, for what, on which model. Each grep outside the reads. Files you would have needed and did not open.
2. **The inventory.** For each owned id (version, act-id, fact, a fact with no value, stamp, time) and each other key or record of the act as kept: every job, each in one plain sentence, with file:line for where it is written and where it is read. For each job: decided when (at write, at read), whether it can change later, one or many, who changes it. Then: what is fixed for good (stored in an act, shown to someone, or written on a page as decided). A job you cannot say in one plain sentence is a finding by itself.
3. **What holds.** What you tested that came through, and which questions you asked of it.
4. **What breaks.** Each finding as the skill's "How to answer" has it: the thing, its jobs, how they differ (in when decided, in whether they change, or in how many), what it costs to separate now, what it costs later. Quote the line with file:line. Order them by what gets fixed for good soonest. Mark each one "certain from what I read" or "depends on something I did not read", and say what. Hold to the brake: "these could be separate" is not a finding. A line on a page that nothing is built on, and that the page itself marks as a default for Sid to turn, goes under "still open", not here.
5. **What is still open.** What the reads cannot settle, and whose it is: Sid's, run N's, or the permission session's. Include the answers to questions a and b above, or why they could not be answered.
6. **Pointers.** The one-line entries from bounds 2, 6 and 7.

Where you took a fork in how to read the task, say which way you went.
