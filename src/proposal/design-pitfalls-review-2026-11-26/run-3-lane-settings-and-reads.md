# Run 3 of 5: a lane's settings and what a read gives back

A review of 26 November 2026 with the skill `design-pitfalls`
(`.claude/skills/design-pitfalls/SKILL.md`). One reviewer on Fable at xhigh
effort wrote the report below, and it is kept word for word. The brief the
reviewer was given is at the end. The merge of the five runs is in
[merge.md](merge.md).

---

# Run 3: a lane's settings and what a read gives back

Paths are under `/mnt/data/projects/Softland/src-store/softland/store/` unless written in full. "By grep" means located by grep, not reviewed.

My view in short: the read path keeps most of its jobs apart and draws most things at read. Five things break against the rule. Two of them are being fixed with every lane and every act made now.

## 1. What I read, and what I did not

**Read whole, by me**
- `/mnt/data/projects/Softland/.claude/skills/design-pitfalls/SKILL.md`
- The two paragraphs of the root `CLAUDE.md`, and `src-store/CLAUDE.md` as orientation.
- `door/declaration.clj` (629), `gate/first_act.clj` (79), `read_path.clj` (1,277).
- `read_path/`: `sources.clj` (626), `lanes.clj` (228), `query.clj` (350), `words.clj` (100), `acts.clj` (369), `answer.clj` (506), `README.md` (456).

**Gatherer:** none. I read every line myself and did the greps myself.

**Greps outside the reads.** Scope: the `*.clj` of `src-store`, `src-wire`, `src-command`, `src-runtime` and `src-auth`, with `record/tables.clj` kept out of each. Patterns:
- the depot key, `depot-at-making`, `starts-with`, `making-act?`, `setting-keys`, `second-write-keys`
- `:streaming` and `:microbatch`
- `builds-on`
- `marked-keys`, `marked-since`, `level-since`
- the query words, and `read-once`, `read-live`, `leave`, `open`
- the answer words
- the keyword `:run`

Hits are quoted where I use them. Inside my reads I also grepped `subjects`, `not-signed-in`, `session`, `layer` and `setting`.

**Not opened, and what each would have settled**
- `record/rows.clj`: what `:level-since` is, what a lane's own rows hold, what `:no-fact` means in the record, what `:answer` in a read of acts holds.
- `gate/decision.clj`: what a gate reads the depot fact for, how the stamp for each marked key is worked out, who may write a setting.
- `act.clj`: the act-id's depot slot, and `covers`.
- `door.clj` and `module/door_reads.clj`: whether anything reads the depot fact's value.
- `module/microbatch_gate.clj`: the copy of a microbatch lane's rows on every task.
- `gate/chain.clj`.
- `src-store/README.md`: the Decisions (run 5).
- The plans and specs that the code cites: `PLAN-read-path.md`, `PROGRESS.md`, `SPEC.md`, `IMPLICIT_SPEC.md`, `BUILD-reads.md`.
- `test-store/`: whether a proof writes builds-on or marks a key.

**Forks I took**
- "The depot" is the depot word, the depot fact and `depot-at-making`. The act-id's depot slot and the `:depot` part of an arrived act are run 2's.
- The read path's ten `:could-not` words are run 4's. `:may-not-see` and `:not-made` are words of an answer: mine as words, the permission session's as judgments.
- Who may see and who may read acts are pointers only. When a setting is fixed, which `starts-with` decides, I took as mine.
- I did not ask question 9. The record is not mine in this run.

## 2. The inventory

### The depot

**The depot word**, `:streaming` or `:microbatch` (`door/declaration.clj:271`). In my files it is read to decide:
1. Which gate's tables hold a lane's record: `read_path.clj:287-300`, `read_path/sources.clj:88-93`, `read_path/acts.clj:64-66`.
2. On which task a read reads the lane's facts: a streaming lane on its own task, a microbatch lane on the task of the read's first lane (`read_path/sources.clj:95-103`).
3. With whom a push is shared and under which count it is bounded (`read_path.clj:576-593`). This follows from 2.
4. Whether a read asked once is sure to hold an act whose ack the caller has: yes in a streaming lane, not in a microbatch lane (`read_path.clj:1079-1082`). This follows from 2.
5. The middle slot of the act-id and fact id that an answer gives (`read_path/answer.clj:72-77`, `read_path/acts.clj:178-182`). The id is run 2's.

- Decided: before the lane is made, by its id.
- Changes: never. "No act changes a lane's depot: where a lane starts is where it stays." (`gate/first_act.clj:29-30`)
- Count: one per lane, two kinds in all.
- By grep, the gate's word for an attempt to change it: `gate/decision.clj:388` `(and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided`.

**`depot-at-making`** (`gate/first_act.clj:22-24`) gives a lane's depot word from the lane's id alone: `(if (= base lane) :microbatch :streaming)`.
- Read in my files at `gate/first_act.clj:32, 51`, `read_path/sources.clj:93`, `read_path/acts.clj:66, 181`, `read_path/answer.clj:77`.
- By grep: `door.clj:156` `[:ok (or got (first-act/depot-at-making lane))])))`, `module/door_reads.clj:103`, `/mnt/data/projects/Softland/src-runtime/softland/runtime/sign_in.clj:65`.

**The fact under `:softland.store/depot`** (`door/declaration.clj:178-183`; `:setting true`, `:written-again false`):
1. Its presence on the lane's own entity says the lane is made (`read_path/lanes.clj:41-46`). Read at each read.
2. Its value repeats the depot word. It is written only in a making act, where it must equal what the id gives (`gate/first_act.clj:51, 60`). Nothing in my reads reads the value.

- Decided once, at write. One per lane.
- Its `:why` names job 2 only: "a gate must read which gate a lane belongs to" (`door/declaration.clj:179`).

### Builds-on

`:softland.store/builds-on` (`door/declaration.clj:214-219`): a vector of lane ids, 64 at most (`:68-71`), `[]` when no fact says it (`gate/first_act.clj:77`).

One job: for a read that says `:from-lane L`, it is the lanes looked through after L, nearest first (`read_path/lanes.clj:73-83, 167-182`).
- It is one level deep: "s, then the lanes of s's builds-on fact, and no further" (`read_path/lanes.clj:171-172`).
- A read that says `:lanes` does not read it (`read_path/lanes.clj:176-177`).
- It also sets the most lanes of one read at 65 (`read_path/query.clj:36-38`).
- Decided: the list at write, its use at each read.
- Changes: yes, written again whole.
- Count: one list per lane.
- Who changes it: permission session (`door/declaration.clj:139-141`).
- By grep: no file of the five trees outside my reads names `builds-on`. Nothing there writes one, and no gate reads it by name.

### Marked keys

`:softland.store/marked-keys` (`door/declaration.clj:220-225`): a set of plain keys, 32 at most (`:73-77`), `#{}` when no fact says it (`gate/first_act.clj:78`).

1. Tells a gate for which keys it keeps each fact once more, in the order by value (`door/declaration.clj:151-153`). Decided at the write of each fact.
2. Tells a read of `:points-at` whether the lane has an order for the asked key, and from which stamp:
   - `read_path/lanes.clj:85-87`, `read_path/sources.clj:236-241`, `read_path/answer.clj:137-144`
   - shown as `:orders` (`read_path/answer.clj:279-286`)
   - a change of it gives a live asker the whole answer anew (`read_path/lanes.clj:227-228`)

- The read path reads a stamp for each key, not the set. By grep the gate keeps that stamp in the row as `:level-since`: `record/rows.clj:129` `(assoc row :level-since (text/text since-by-key)))`, `gate/decision.clj:461` `(defn- level-since-after`.
- Changes: yes. "a mark is taken off by writing the set again without it" (`door/declaration.clj:153`).
- Count: one set per lane, many keys, each with a stamp of its own.
- Who changes it: permission session.
- By grep: nothing outside `src-store` names it, and no client in the five trees asks `:points-at`.

### The words of a query (`read_path/query.clj:129-138`)

All are decided by the caller at each call. Nothing of a query is stored.

| Word | Its jobs |
|---|---|
| `:asks` | Names what is read. Sets the form of the answer. Under `:acts`, makes the read once-only and of one lane (`read_path/query.clj:236-238`, `read_path.clj:1251`). |
| `:place`, `:entity`, `:run` | One place; every place of one entity; every place between two ends. |
| `:points-at` | The places under one key whose value is the thing. |
| `:acts` | A lane's decisions from a stamp on, so many at most. |
| `:asker` | See question a in section 5. |
| `:not-signed-in` | See question a. |
| `:session` | See question a. |
| `:lanes` | The lanes looked through, nearest first. Its first lane is also where a microbatch lane is read (`read_path/lanes.clj:184-193`). Under `:acts`: the one lane. |
| `:from-lane` | The start lane, and the instruction to take the rest from its builds-on fact. Also where a microbatch lane is read. Under `:acts`: only the lane. |
| `:with` | Which of writer, permission and time of a row are handed over (`read_path/answer.clj:62-70`). Not read under `:acts`. |
| `:stores-own-facts` | Whether rows under the store's keys count (`read_path/answer.clj:52-60`). Not read under `:acts`. |

Once or live is the call, not a word (`read_path/query.clj:5-7`).

### The words of an answer

All are worked out at read. The read path stores nothing (`read_path.clj:53-54`).

**An answer of facts** (`read_path/answer.clj:257-286`)
- `:lanes`: every lane of the read in order, those left out among them.
- `:left-out`, with `:lane` and `:why` (`:may-not-see` or `:not-made`). In an answer of facts it says the lane gave nothing. In an answer of acts the lane is left out while `:acts` still holds the asker's own (`read_path/acts.clj:262-271`).
- `:place`, or `:places`: the place-answers.
- `:orders`: for `:points-at`, for each lane she may see, `:level-since` or `:not-marked`.

**A place-answer** (`read_path/answer.clj:79-99`)
- `:holds`, one of `:no-fact`, `:nothing-here`, `:value`, `:not-a-value`. `:no-fact` does three jobs: finding 3.
- `:lane` and `:value`.
- `:stamp` and `:fact`: run 2's.
- When asked: `:writer`, `:permission`, `:time`.

**An answer of acts** (`read_path/acts.clj:262-275`)
- `:acts`: each entry is `:stamp` alone when withheld, else `:stamp`, `:act-id`, `:answer`, and `:thing` or `:no-thing`.
- `:next`: where the next read starts.
- `:last-stamp`: only for an asker who may see the lane.

**A live read** (`read_path.clj:1189-1205`): `:now`, `:leave-with`; `:anew`, `:changed`, `:ended`; `:left`, `:not-open`.

### Others

- **`:setting` and `:written-again`** (`door/declaration.clj:137-141, 231-235`). The first says a fact sits only on the lane's own entity. The second says it can be written again. They are two flags, and the depot has the first without the second.
- **`starts-with`** (`gate/first_act.clj:64-79`): what a lane reads as under a setting when no fact says it. Decided at each read, by code, from the lane's id.
- **The making act** (`gate/first_act.clj:34-62`): exactly two facts, the depot fact and the first permission.
- **`base`** (`gate/first_act.clj:18-20`): its jobs are in finding 2.
- **The nine numbers** (`read_path/query.clj:40-103`): values that an opener gives.

### What is fixed for good

- **Stored in acts:** the depot fact of each making act; a builds-on vector or a set of marked keys when a holder writes one; and the absence of any setting fact in every lane made so far.
- **Made to be stored:** `:fact` and `:stamp` of a place-answer, which a writer puts in `:replaces` and `:based-on` (`read_path/answer.clj:19-21`).
- **Shown to other trees already, by grep:**
  - `/mnt/data/projects/Softland/src-command/softland/command/main.clj:320-322` `{:asks [:place e k]` / `:asker (:who hello)` / `:lanes [...]`
  - `/mnt/data/projects/Softland/src-runtime/softland/runtime/sign_in.clj:228` `(= :not-made (:why left))) (:left-out answer)`
  - `/mnt/data/projects/Softland/src-wire/softland/wire/watching.clj:73` `(if (= :no-fact (:holds place-answer))`
  - `/mnt/data/projects/Softland/src-runtime/softland/runtime/seam.clj:688, 981` call `read-once` and `read-live`.
- **Not yet sent by any client in the five trees, by grep:** `:from-lane`, `:with`, `:entity`, `:run`, `:points-at`, `:acts`.
- **Written on a page as decided:** `read_path/README.md:67-95`, listed in section 6.

## 3. What holds

- **Builds-on is a default for a read, not a rule of the lane** (questions 1, 2, 5, 7). The reader's own list and the lane's list are two words of a query. The list is read at each read, and a live read is told when it changes. Any list of up to 65 lanes in any order can be asked.
- **Once or live is not in the query** (question 2). What is asked and how long it is kept have two lifecycles, and they are apart.
- **What is shared and what is handed over are apart.** `:with` and `:stores-own-facts` are no part of a source (`read_path/sources.clj:30-32`).
- **Sizes are values** (question 3). A run takes its ends as values. `:entity` and `:run` are put together by the same entry (`read_path/answer.clj:233-234`). The limits are numbers.
- **`:setting` and `:written-again` are two flags** (question 2).
- **Where a microbatch lane is read is drawn from the query at each read** and is in no answer (question 4). The order of `:lanes` so also chooses a task, but nothing is fixed by it.
- **The depot slot of an id in an answer is drawn at read** (question 5). It holds for as long as a lane's depot is a function of its id.
- **Marked keys** (questions 5, 6). The order by value is written at write because it is an index. The facts stay whole, and the answer says per lane from when its order counts. This rests on the gate's working of that stamp, which I did not read.
- **A failed read never says "no fact"** (`read_path/words.clj:93-100`).
- **No building on top of the rename** (question 10). No "layer" is left in my reads.

## 4. What breaks

The first two are fixed at the same pace: with each lane made and each act taken.

### 1. A setting that no fact says is decided by code at each read, and a caller's read of it gives `:no-fact`

**The thing:** the place of a setting on the lane's own entity, read with `starts-with`.

**Its jobs**
- It keeps what a holder wrote. That is decided at write, stored in an act, and changed by an act.
- When empty, it stands for a value that code gives from the lane's id. That is decided at each read, stored nowhere, and changed by whoever edits the function.

**How they differ:** in when decided, in who changes it, and in whether the lane's line holds it.

**The lines**
- "No fact is written at the making: a lane made before these keys existed holds none, so a reader must read "no fact" either way, and the form of a making act stays two facts. No gate calls it; a reader of the tables does." (`gate/first_act.clj:66-69`)
- `:softland.store/who-may-see (if (= base lane) :everyone :members)` (`gate/first_act.clj:75`)
- A making act can hold no setting: `(= 2 (count (:facts act)))` (`gate/first_act.clj:59`).
- A caller who reads the place gets `{:holds :no-fact}`, not what the lane reads as. The walk gives the row or nothing (`read_path/answer.clj:118-135, 274`). So each caller must carry the same function.

**Weight.** For builds-on and marked keys the value is "nothing", and I see no harm. The weight falls on who may see and who may read acts, and that judgment is the permission session's.

**Cost to separate now:** it touches the form of the making act and every lane already made.

**Cost later:** the same, for every lane made until then. Until then a change to the function changes existing lanes with no act in their line.

**Mark:** certain from what I read. The page lists the values as not decided (`read_path/README.md:327`); the making act and three functions of `read_path/lanes.clj` (lines 56, 69, 81) are built on it.

### 2. The id of the base, and the word `:microbatch`, each decide several things

**The things:** the lane id `:base` and the depot word.

**What the id `:base` decides**
- Which gate decides the lane (`gate/first_act.clj:24`).
- That its rows are read on the task of the read's first lane (`read_path/sources.clj:102`: `:routing-id (if (= :microbatch gate) home lane)`).
- Who may see it when no fact says (`gate/first_act.clj:75`).
- Who may make it (`gate/first_act.clj:58`; permission session).

**What the word `:microbatch` says at once**
- How the lane's acts are decided.
- On which tasks its rows are kept, and so where a read reads them.

**How they differ**
- The gate never changes.
- Who may see it can be written again.
- How widely a lane is read changes over its life.
- Only one lane can be `:base`. More than one could be decided in batches, or read by everyone.
- Today nothing differs in the built thing: all are set by the id, for good. By the brake this is the weaker of the two.

**The page's own lines keep them as two.** `read_path/README.md:89-90` quotes "The base's rows: a copy on every task. One task decides, in the line's order." and `read_path/README.md:253` says "A microbatch lane, which today is the base". Not checked against the record: run 5.

**Cost to separate now:** `depot-at-making` is read at six places in my files and at more by grep. A reader has to know a lane's gate before it can read the lane's record.

**Cost later:** on the read side it is one function, "The one place where a source gets its routing id" (`read_path/sources.clj:96`). What grows is the depot slot in every act-id (run 2's) and the gate that writes the copies.

**Mark:** the jobs are certain from what I read. That they will ever need to differ depends on what I did not read, and on Sid: whether any lane but the base is to be read by many or decided in batches.

### 3. `:no-fact` in an answer does three jobs

**Its jobs**
- In the record, by grep, `record/rows.clj:169`: "`[:no-fact]`: no fact was ever accepted at the place."
- In an answer of `:place`: no lane of this read that this asker may see holds a row there (`read_path/answer.clj:244`: "A place that the read does not give holds {:holds :no-fact}.").
- In `:changed`: take the place out. Under `:points-at` the place holds a fact, with another value (`read_path/answer.clj:181-182`).

**How they differ**
- The first is the same for everyone, and is never true again once a fact is accepted.
- The other two are decided at each read, by who asks, through which lanes and for which thing. They can go back and forth.

**The page calls them one:** "`:no-fact`, `:nothing-here`, `:value` and `:not-a-value` under `:holds` are the record's own" (`read_path/README.md:385`).

**Cost to separate now:** one more word. By grep it is already read at `/mnt/data/projects/Softland/src-wire/softland/wire/watching.clj:73`.

**Cost later:** each client that takes `:no-fact` to mean "nothing was ever written here".

**Mark:** certain from what I read. The record's meaning is by grep only. Part of the hiding is meant: she is told nothing of a lane she may not see.

### 4. "Run" names two things

**The two things**
- A range of places: "A run is every place of one lane from one end to the other" (`read_path/query.clj:14`). The query word is `:run` (`read_path/query.clj:119`).
- The run of a `:session`, the product's own:
  - "`:session` is the run that the read is asked in" (`read_path/query.clj:10`)
  - "`:a-run-has-an-asker`: a run is somebody's." (`read_path/query.clj:145`)
  - `(case (first (act/covers (:v fact))) :run :softland.store/run-permission` (`door/declaration.clj:466-467`)
  - by grep, `act.clj:14`: "A permission whose sixth slot is `[:run session]` covers one run"

**Where a caller meets both.** One list of reason words holds `:run-not-named` and `:run-with-no-asker` (`read_path/query.clj:119, 158`). `read_path/README.md:357` and `:360` use both senses three lines apart.

**How they differ:** they are two things, not two jobs of one.

**Cost to separate now:** the read path's words are "a session's" and "Sid's to change" (`read_path/README.md:331`), and by grep no client sends `[:run …]` yet. The other sense is already inside permissions in acts.

**Cost later:** the query word, four reason words and two numbers will be in clients.

**Mark:** certain from what I read.

### 5. `:acts` names decisions, and shares the query's form

**What `:acts` gives.** It is "the decisions of one lane from a stamp on … each with its answer and the thing whole" (`read_path/acts.clj:2-4`). "A taken act is held to the same rule as a refused thing." (`read_path/acts.clj:117-118`)

**An example.** Three things arrive in lane L, and the gate refuses the second. A holder's read of `[:acts 0 10]` gives three entries under `:acts`. The refused thing is among them, with an `:act-id`.

**Its jobs**
- `:acts` holds refused things as well as taken acts.
- Under it three parts of the one form change meaning. `:from-lane` means only the lane (`read_path/acts.clj:340`). `:with` and `:stores-own-facts` pass the check and are not read (`read_path/query.clj:24-26`).

**How they differ:** a read of acts is one lane and once only; a read of facts is up to 65 lanes, once or live.

**Cost to separate now:** small. No client sends `:acts` to the read path, by grep.

**Cost later:** the word on the wire, where by grep `:acts` already means acts to send (`/mnt/data/projects/Softland/src-wire/softland/wire/words.clj:85`: `:carries :acts`).

**Mark:** what is built is certain from what I read. Whether a refused thing is an act in Sid's sense depends on the record (`src-store/CLAUDE.md:47` says the word is his). Not checked against the record: run 5.

## 5. What is still open

### Run 1's questions

**a. `:asker`, `:not-signed-in` and `:session`**

`:asker`:
- It names the holder whose permission row is read in each lane (`read_path/sources.clj:110-112, 174-189`).
- Whether it is nil decides whether any right is read at all (`read_path/sources.clj:462, 472-478`).
- A run's permission counts only when its holder is the asker (`read_path/lanes.clj:107-121`).
- In a read of acts it is matched against a decision's `:who` to say what is "her own" (`read_path/acts.clj:129-132, 364-365`).
- For an asker outside the lane it names whose rows by who are read (`read_path/acts.clj:77-80, 306`).
- The read path holds it against nothing: "who asks is what the caller says" (`read_path/query.clj:11-12`).

`:not-signed-in`:
- It does one thing, at the check. Exactly one of it and `:asker` must be there (`read_path/query.clj:132, 225-226`).
- Nothing reads it after that. "Signed in" is `(some? asker)` everywhere, so `:system` counts as signed in.

`:session`:
- It names the entity whose `:softland.store/run-permission` row is read in each lane (`read_path/sources.clj:114-116, 184-188`; `read_path/acts.clj:350`).
- It needs an asker (`read_path/query.clj:228-229`).

What each decides about seeing is the permission session's.

**b. `:subjects`.** The read path does not read it. In my reads it occurs once, at `door/declaration.clj:416`, where the door holds each id to the length limit. A read of acts decides "her own" by the line row's `:who` alone and hands `:thing` whole. Subjects travel inside the thing and decide nothing.

### Sid's
- What "builds on" means beyond what is built: one level deep, read only by a `:from-lane` read, held by no gate, written by nothing in the five trees.
- The page's own "Not decided, kept for now" (`read_path/README.md:315-329`):
  - what `:left-out` says
  - how far a read walks
  - the store's own facts
  - `:points-at` through lanes
  - what a lane starts with
- Whether a read of acts is kept live (`read_path/README.md:176-177`).
- The read's mark (`read_path/README.md:237-238`).
- The word "standing" (`read_path/README.md:412`).
- Whether a depot can change.

### Other runs'
- **Run 1 and the permission session:** `:asker` is read as a holder and as a `:who`. Are they always one id? Are "session" and "run" one thing?
- **Run 1 and run 4:** `:asks` is also the text of what a program says of itself at sign-in. By grep, `/mnt/data/projects/Softland/src-runtime/softland/runtime/sign_in.clj:163`: "`:asks`, what the program says of itself."
- **Run 2:** inside one arrived thing the depot is said three times, and the lane's id gives all three (`door/declaration.clj:268, 441-442, 550`).
- **Whoever reads `record/rows.clj` and `gate/decision.clj`:**
  - whether "marked since" and `:level-since` are one stamp
  - whether a key marked a second time leaves its earlier order unread
  - what a gate reads the depot fact's value for
- **`gate/first_act.clj:18-20`** says the base is "The one lane that everyone subscribes to." Nothing in my reads puts the base into a read unless the query or a builds-on list names it.

### Page lines that no longer match the code
These are not findings of this skill.
- "no asker uses it but its proof" (`read_path/README.md:63-65`).
- "gives the four" own rows; there are five settings (`read_path/README.md:140-142`).
- "one function of lanes.clj is its one caller" (`read_path/README.md:327`); three functions call it.
- "The eight rule names" (`read_path/README.md:373`); there are ten.
- The place-answer's parts leave out `:time` (`read_path/README.md:385`).

## 6. Pointers

### Not checked against the record: run 5
- `read_path.clj:69-71`: "(Sid, 11 October 2026: "i also don't consider it a cache")". On it stands the read path holding no value: `held`, the sharing of pushes, `known-of`.
- `read_path.clj:1-8`: "the rulings that came after its check … 78 and 80; and 86, 87, 89 and 91". It does not say which code stands on which.
- `read_path/README.md:67-83`: the Decisions of 11 and 12 October 2026, entries 1 to 9.

| Entry | Code that stands on it |
|---|---|
| 1 | `read_path/lanes.clj:48-58, 134-150` |
| 2 | `read_path/lanes.clj:73-83, 167-182`; `read_path/answer.clj:118-135` |
| 3 | `read_path.clj` whole |
| 4 | `read_path.clj:44-51`; `read_path/sources.clj:282-298` |
| 6 | `read_path/query.clj:107-121`; `read_path/sources.clj:208-241`; `door/declaration.clj:220-225` |
| 7 | `read_path/answer.clj:62-70` |
| 9 | the read path keeps no record of a read |

- `read_path/README.md:84-86`: the store's keys (8 and 9 October), "the depot that a lane starts with (7 October)", the form of an id (6 October). On them stand `door/declaration.clj:124-225` and `gate/first_act.clj:22-32`.
- `read_path/README.md:88-95`: 13 October 2026, entry 4, quoted. On it stand `read_path/sources.clj:95-103`, `read_path/lanes.clj:184-193` and `read_path.clj:154-171`.

### Permission session
- `door/declaration.clj:139-146`: the first holder; give and end.
- `door/declaration.clj:148-149, 153-156, 202-213`: who may see, who may read acts.
- `door/declaration.clj:163-177, 184-201, 227-229, 306-311, 397-405, 458-494`: what a permission covers, and yes.
- `gate/first_act.clj:5-10, 38-46, 57-58, 75-76`.
- `read_path/lanes.clj:48-71, 89-165`.
- `read_path/sources.clj:110-189, 300-315`.
- `read_path/acts.clj:92-132`.
- `read_path/answer.clj:27-30, 69, 378-395`.
- `read_path.clj:44-51, 1217-1231`.
- `read_path/words.clj:87-91`.
- `read_path/README.md:74, 144-167, 322, 328`.

### Run 1
- `:who`: `door/declaration.clj:110-120`; `gate/first_act.clj:9-16, 38-39`; `read_path/acts.clj:120-125`; `read_path/answer.clj:68`.
- Session and run: `door/declaration.clj:165-168, 399, 415, 480`; `read_path/query.clj:10-11, 133`; `read_path/sources.clj:114-116`.
- Person, agent, sign-in: `gate/first_act.clj:6, 14-15`; `read_path/query.clj:132, 334-335`.

### Run 2
- The act's form, in `door/declaration.clj`: the limits (27-77), the shapes and rules (249-319), `ids-of` (407-429), the check (557-629).
- Act-id: `door/declaration.clj:360-365, 528-553`; `read_path/acts.clj:181`; `read_path/answer.clj:72-77`.
- A fact with no value: `door/declaration.clj:285-286, 378, 620-622`; `read_path/answer.clj:22-25`.
- Stamp: `read_path/answer.clj:20-21, 88`; `read_path/sources.clj:125-139`; `read_path/acts.clj` throughout.
- Time: `door/declaration.clj:79-106`; `read_path/answer.clj:70`.
- I met no "version".

### Run 4
- The read path's ten words, with what each carries and why a live read ends: `read_path/words.clj:14-85, 93-100`.
- The reasons of a query's check: `read_path/query.clj:172-182`.
- `read_path.clj:372-384`.
- The gate's reason words and the words of a stray: `door/declaration.clj:321-339`. I do not know whether these are run 4's.

---

# Appendix: the brief this reviewer was given

You are doing run 3 of a five-run review of the Softland repository (/mnt/data/projects/Softland) with the skill `design-pitfalls`. Run 3 only. Runs 1, 2, 4 and 5 are other reviewers' and the merge is mine; do not do any part of them.

## Why this review exists

Sid, the founder, has to find out whether what earlier agent sessions built is right. The case that started it: the store's unit carried his word "layer" while meaning something narrower, and one id did several jobs; that was corrected to "lane". He now wants the same adversarial check on **a lane's settings and what a read gives back**: the store keys that set up a lane, the first act of a lane, and the read path with the words of a query and of an answer. The job is to break the thing, not to confirm it, and not to redesign it. If it holds, say that it holds and what you asked of it.

## Read these first

- `.claude/skills/design-pitfalls/SKILL.md`, whole. Follow it: the inventory first, then the ten questions, the brake, and "How to answer".
- In the root `CLAUDE.md`: the paragraph "Defaults, not conflations" and the one after it ("The same holds between Sid and a session"). That is the rule being checked.
- `src-store/CLAUDE.md`, as orientation only. It is not under review; raise no finding on it.

## What you review (the whole of it)

All under `src-store/softland/store/`:

- **The store keys in `door/declaration.clj`** (the file is 629 lines): the part that declares a lane's settings. Run 2 reads the same file for the act as it is kept; what belongs to the act's form is run 2's, so point to it.
- `gate/first_act.clj` (79)
- `read_path.clj` (1,277)
- `read_path/`: `sources.clj` (626), `lanes.clj` (228), `query.clj` (350), `words.clj` (100), `acts.clj` (369), `answer.clj` (506), `README.md` (456)

Run 3 owns these: **the depot (streaming or microbatch), builds-on, marked keys, the words of a query and of an answer**. For each one you list every job it does. Also take into the inventory, under "others", any other setting of a lane or word of a read in these files that no run owns.

Where the edge with the permission session runs, in your files:
- The read path decides who may see and who may read a lane's acts. That is the permission session's: one-line pointer, no jobs, no judgment.
- Where `first_act.clj` decides who may make a lane or set its settings, that is "who may act": pointer.
- The settings themselves (what each is, when it is fixed, whether it can change, one or many) and the words of a query and an answer are yours.

Run 1 left these for this run. They are questions, not findings to confirm; answer each from your reads with file:line, or say what you did not read:
- a. A query's words `:asker`, `:not-signed-in` and `:session`: what does each do in the read path? (Run 1 saw only that the runtime's seam holds `:asker` and `:session` against the caller.)
- b. Does the read path read an act's `:subjects`, and to decide what?

A note on one word: in the skill and the root page, "a session" usually means an agent session that wrote code or pages. `:session` in the code is the product's own thing. Keep the two apart in your report.

## Bounds. These are strict; Sid asked for them in so many words

1. **Read only.** Write or edit no file anywhere: no notes on disk, no memory, no docs. No git command that changes anything. Start no process: no REPL, no tests, no proofs.
2. **The record is not yours.** Do not use memtool, the softland-memory tools, or recall, at all, for any reason. The skill's question 9 says to open the line Sid typed; in this run you do not. Where a docstring or page in your reads says Sid decided or called something, or cites a "Decisions of …" entry, write "not checked against the record: run 5" beside it with file:line, and list which code in your reads stands on it. Write nothing as Sid's decision yourself.
3. **Never read** `src/app/server/env.clj` or any `.env` file. Never open a `raw-` file under any `runs/` directory.
4. **Stay inside the reads above.** One allowance, so that the jobs of an id you own can be listed: you may *grep* the `*.clj` files of `src-store/`, `src-wire/`, `src-command/`, `src-runtime/` and `src-auth/` for where an id you own is read or written, and report each hit as "located by grep, not reviewed" with file:line and the matched line. Keep `src-store/softland/store/record/tables.clj` out of every grep (the store's tables are not in these runs). Do not open those files to read them through, and raise no finding on code outside your reads. Where a finding would need such a file, write it under "still open" and name the run that reads that file. If a file outside your reads looks necessary, do not open it: name it in the list of what you did not read and say what it would have settled. `src-store/README.md` is run 5's: do not open it.
5. **Never open**: `src-server/`, `src-cli/`, `src-server-browser/`, `src/app/` (the old server, the client, the renderer), `src-inland/`, the rig, `history/`, the store's tables, the plan for pieces 5 to 8.
6. **Permissions belong to a separate session.** It owns: who may act, who may see, who may read a lane's acts, end, yes, and what a permission covers. ("End" and "yes" are read here as the store's permission words.) Where you meet one of these, write one line: where you met it (file:line) and "permission session". Do not list its jobs and do not judge it.
7. **Ids of the other runs.** Where you meet one, write one line with file:line and the run number, and do not list its jobs:
   - run 1: person, root, agent, sign-in, session and run, `:who`
   - run 2: version, act-id, fact, a fact with no value, stamp, time
   - run 4: the words on the wire, the "could not" answers, the commands
   - run 5: the pages that say "Sid decided"

## Models, on Sid's instruction

You are the reviewer and the judgment is yours alone; it is never handed down. Your files are about 4,500 lines. Where you need collection (every place a setting or a word is written or read, with file:line and the quoted line), you may hand that to a read-only gatherer on a lower model and effort: the Agent tool with `subagent_type` `Explore` or `code-hunter`, `model: "sonnet"`, `effort: "low"` or `"medium"`. Its brief must carry bounds 1 to 5 above, since it loads neither this brief nor the root page. A gatherer returns facts with file:line and never a verdict. Read for yourself every passage a finding rests on, and quote only what you yourself read. If you have no Agent tool, or need no gatherer, gather with Grep and Read and say so.

## What to send back

Your final message is the whole deliverable, and it is all I will see, so make it complete. Plain words. Use Sid's word for a thing he has named (lane, act) and the standard term otherwise; add no vocabulary of your own. Give your view and its grounds. Name no fix beyond the split: the design is its author's and Sid's.

1. **What I read, and what I did not.** Each file, whole or in part. Any gatherer used, for what, on which model. Each grep outside the reads. Files you would have needed and did not open.
2. **The inventory.** For each owned thing (the depot and its two kinds, builds-on, marked keys, each word of a query, each word of an answer) and each other setting or word: every job, each in one plain sentence, with file:line for where it is written and where it is read. For each job: decided when (at write, at read), whether it can change later, one or many, who changes it. Then: what is fixed for good (stored in an act, shown to someone, or written on a page as decided). A job you cannot say in one plain sentence is a finding by itself.
3. **What holds.** What you tested that came through, and which questions you asked of it.
4. **What breaks.** Each finding as the skill's "How to answer" has it: the thing, its jobs, how they differ (in when decided, in whether they change, or in how many), what it costs to separate now, what it costs later. Quote the line with file:line. Order them by what gets fixed for good soonest. Mark each one "certain from what I read" or "depends on something I did not read", and say what. Hold to the brake: "these could be separate" is not a finding. A line on a page that nothing is built on, and that the page itself marks as a default for Sid to turn, goes under "still open", not here.
5. **What is still open.** What the reads cannot settle, and whose it is: Sid's, run N's, or the permission session's. Include the answers to questions a and b above, or why they could not be answered.
6. **Pointers.** The one-line entries from bounds 2, 6 and 7.

Where you took a fork in how to read the task, say which way you went.
