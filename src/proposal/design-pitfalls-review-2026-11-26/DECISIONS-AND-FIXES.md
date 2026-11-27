# The review of 26 November: what Sid decided, what is a session's, what is open, what is to fix

As of 27 November 2026. Written by the Claude Code session that ran the five
`design-pitfalls` runs of 26 November and merged them ("the review session"),
to be handed to other sessions and reviewed by carbon and neon. It assumes
nothing of any conversation. Nothing was built or fixed for this page, and no
other page was changed.

Five kinds of statement are kept apart:

- IN THE CODE: what the review session or its reviewers read on 26 and 27
  November, with file and line. "By grep" means only the matched line was
  seen. Line numbers are those of the working tree at commit `e7f5ca33`.
- SID'S: words he typed, quoted. The review session did not open the record,
  so an id beside a quote comes from run 5's report, from a page, or from the
  permission page.
- DECIDED: a question put to Sid and his explicit reply to it.
- REPORTED: carbon's sheet of 27 November says he answered carbon so. His
  words are in carbon's session and were not seen here.
- A SESSION'S: a reading, a proposal or a default. It is his to turn. It is
  not a decision of his.

By his rule (permission page, section 6, item 15) a page is approved only
where he answered explicitly. So only the DECIDED parts of this page are
approved.

Short file names: `seam.clj`, `sign_in.clj`, `line.clj`, `http.clj` are under
`src-runtime/softland/runtime/`; `schema.clj`, `fit.clj` under
`src-auth/softland/auth/`; `words.clj`, `frame.clj`, `credential.clj`,
`watching.clj` under `src-wire/softland/wire/`; `main.clj`, `client.clj`
under `src-command/softland/command/`; `act.clj`, `first_act.clj`,
`stamp.clj`, `decision.clj`, `digest.clj`, `additions.clj`,
`declaration.clj`, `lanes.clj`, `sources.clj`, `query.clj`, `answer.clj`,
`acts.clj`, `read_path.clj` under `src-store/softland/store/` and its
`door/`, `gate/` and `read_path/`. `SPEC.md` is
`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`, `PROPOSAL.md` is
`src/proposal/runtime-2026-11-21/PROPOSAL.md`, `README.md` is
`src-store/README.md`.

## 1. In short

- Three sessions reviewed the same five areas on 26 November: this one,
  carbon and neon. They agree on the core.
- The fault has two shapes. One stored thing carries several jobs. And a
  session's reading is written down as Sid's decision.
- Sid answered six questions in the review session (section 3.1) and
  seventeen in the permission session (the permission page, section 6).
- Section 7 lists the fixes in three groups: those that can start, those
  that need his yes on a changed stored form or page, and those that wait
  for the new permission design.
- No real data is kept yet. Pieces 5 to 8 wait until the fixes are done
  (SID'S, permission page, item 17).

## 2. Where the material is

- The five reports and their merge: this folder, `run-1-…` to `run-5-…` and
  `merge.md`. Each report is word for word and ends with its brief.
- Carbon's merged review and its sheet of consolidated answers: in carbon's
  session. Not in the repo.
- Neon's review:
  `~/.gemini/antigravity-cli/brain/ddfed5cc-62d0-434b-a2cc-1f8b0e932a42/design-pitfalls-review-runs-1-to-5.md`.
  Outside the repo.
- Permissions: `src/proposal/permissions-2026-11-27/PERMISSIONS.md` ("the
  permission page"). This page points to it and does not copy it.
- This folder and the permission folder are not committed.

### How far the three reviews agree

- All three: `:session` on an act carries several jobs; the calls of 22
  November have no line of Sid's; the lane rename was committed before his
  line; the act-id carries a depot word the lane already gives; `person?`
  tests "not an agent"; `made-by` is three jobs; parts of every act have no
  reader; runtime page entries 5, 7 and 8 rest on voice and phone lines
  outside the record.
- Found by carbon and neon, missed by the review session's runs, and
  checked here against the code: fixes F14, F17, F18, F22, F23 and the
  second job of `:version` (E5). All six sit in files our reviewers read.
- False in both carbon's and neon's reviews: that `(= 1.00M 1M)` is false.
  In the project's Clojure 1.12.4 it is true (one expression run on the JVM,
  no project code loaded). What is true: `normal-value` drops trailing
  zeros, so `1.00M` reads back as `1M` (`act.clj:200-212`).
- Only in the review session's reports: F6; the lane's settings decided by
  code (F29); the ruling from photographs behind 6 October entries 4 and 5;
  code that cites an entry for a sentence the entry marks as a session's
  (F4).

## 3. Decided

### 3.1 In the review session, 27 November

His reply to the six, word for word: "1, keep 2.keep 3. yes, 4. both yes
5.yse   6. yes". The ids of these lines were not looked up.

**1. A refused write stays in the lane's record.**
Asked: "Does a refused write stay in the lane's record? Example: an agent
with no permission writes to your lane. Kept: the lane's record shows
'refused' with what was sent, and it can be read back. Dropped: the agent is
told 'refused' and the lane holds nothing of it. On 12 October you said
keep: 'if they have it in the log, we can send them back, hey, this was
rejected'. Carbon's sheet says dropped, and tags that as agreed by you on
review. I say keep."
Sid: "keep".
Stands as: kept, and readable back. This reverses item 10 of carbon's sheet.

**2. A lane may still name the lanes a read looks through by default.**
Asked: "May a lane still name the lanes a read looks through by default? On
11 and 12 October you decided: the lane says which lanes it builds on, and a
view may ask for another list. Carbon's sheet drops that, from your line
'any type of relation between lanes, not a fixed shape'. I say keep. A
default list fixes no shape, because a view can already pass any list."
Sid: "keep".
Stands as: the default list stays, and no fixed hierarchy is enforced. This
changes item 8 of carbon's sheet.

**3. "Everyone" is a holder like any other.**
Asked: "Is 'everyone' a holder like any other? Making a lane public would be
giving 'see' to everyone, as an act in that lane. Today it is a setting of
the lane, and for the base it is only in code. I say yes."
Sid: "yes".
Stands as: as asked. It closes (a) of the permission page's section 8.

**4. Owners.**
Asked: "4a. With several owners, may each act alone, without the others
agreeing? I say yes. 4b. Can the last owner leave only by handing ownership
on? I say yes."
Sid: "both yes".
Stands as: as asked. It closes (b) of the permission page's section 8.

**5. The README sentence of 25 November.**
Asked: "May the README sentence lose 'and shows to its members'? It would
read: 'What one gate puts in order is a lane.' I say yes."
Sid: "yse".
Stands as: yes. The page is not changed yet (F1).

**6. The order of work.**
Asked: "Fixes first, and the new permission design after as its own build?
You said pieces 5 to 8 wait 'until the review fixes are done'. Carbon's
sheet mixes fixes to what exists with new design: parties and gives, runs
with a start and an end, and a place holding two facts at once. I say yes.
One fix would wait for the new design: taking `:session` off the act needs
the run."
Sid: "yes".
Stands as: as asked.

Two more lines of his from that session, which are not decisions:

- On whether a program may write under his own who: "does this 1. not
  informed atleat by what i have said in the permission's work??"
- On this session's part: "we don't do any of the fixes or anything in this
  session".

### 3.2 In the permission session, 26 and 27 November

His replies, word for word with their ids, are in the permission page,
section 6: items 1 to 11 and 15 to 17. Not copied here.

One reading there goes past his words. Item 7's "stands as" adds "the same
act sent twice counts once"; his words were "idempotent that is passed and
failed as one". The store already works that way, by 7 October entry 7
below.

### 3.3 Earlier decisions that the fixes stand on

Quoted from `README.md`, whose lines the review session read. The record was
not opened for them.

- **7 October, entries 3 and 4** (`README.md:230-235`): "The members of a
  layer set its class, as a fact on the layer", and of the other layers
  "members choose yes, default is streaming yes". F22 stands on these.
- **7 October, entries 6 and 7** (`README.md:256-268`): the door reads an
  act's answer from the record by act-id; "so yeah A it is". The page adds:
  "a repeat is answered, not decided twice".
- **9 October, entry 1** (`README.md:372-378`): an account "is added,
  replaced or removed by its link". F18 stands on this.
- **11 and 12 October, entry 2** (`README.md:554-567`): "A layer says which
  layers it builds on, in order, as a fact on the layer", "A view may give
  another list", and, shown to him and let stand, "the nearest layer that
  holds a fact at a place wins, also when that fact says 'nothing here'".
- **12 October, entry 7** (`README.md:816-848`): "why don't we store both?
  Like the data is useful", "if they have it in the log, we can send them
  back, hey, this was rejected", "we are not choosing the destructive choice
  here".
- **6 October, entry 9** (`README.md:161-178`): an id is a readable keyword
  or a UUID. The page says the half on a key is a session's, that he picked
  "5 (keys as ids): confirm" on 24 September, and that he "has not ruled
  between the two".

## 4. Reported by carbon

Carbon's sheet marks these as his explicit answers to carbon. Carbon to give
his line for each. Until then a fix that rests on one alone is marked
REPORTED in section 7.

- No program, command line or model writes under a person's who. An approved
  program gets a party of its own and a give.
- `:driven-by` is dropped.
- Auth stays a module of its own beside the store.
- A person can have several accounts. An erased or unlinked email is freed.
- A fact's key may be an id or a keyword: "yeah both".
- "there can be any type of relation between lanes, not a fixed shape".
- His name comes off the entries of 22 November that he pasted.

## 5. A session's, not decided

- A refused write takes a stamp. That is today's code (`stamp.clj:5-10`).
  The review session's ground for keeping it: stamps then do not depend on
  what a gate decides, so fixing a gate rule does not renumber later acts.
- A refused thing is an act whose answer is "refused". Section 3.1 item 1
  decides that it is kept, not what it is called.
- A person's own acts carry no run.
- Approving a program gives it nothing by default.
- A command line that a person types into herself also gets a who of its
  own. Carbon's reported answer covers it; the review session drew the same.
- `:subjects` and `:claimed-when` leave the act (F24). Carbon's call; the
  review session agrees, because each can be a plain fact about the act.
- A once-only yes as "a give with one use" (carbon's item 13). The review
  session objects: see E3.
- The permission page's section 7, and its section 8, c to h.

## 6. Open

### 6.1 For Sid; none of these blocks group 1

- O1. Whether invite, list my agents and end a sign-in can be lent to a
  program. After F15 a command line can say none of them
  (`seam.clj:728-754`).
- O2. An invitation has no end and cannot be taken back (`SPEC.md:267-268`).
  With freed emails (F18), whoever gets the address later can become the
  invited person.
- O3. Whether the copy of the own lane on each sign-in falls under the stop
  on caching (`schema.clj:984-986`, `1464-1467`).
- O4. Whether a refused thing is called an act, and so what the read word
  `:acts` is called (`acts.clj:2-4`, `117-118`).
- O5. His word for a group of acts, and which earlier "layer" entries are
  lanes. The permission page's item 14.
- O6. In Auth, whose end ends an agent. Today `made-by` is who made it,
  whose end ends it and who may end it (`schema.clj:1161-1170`,
  `seam.clj:668`).
- O7. A refused thing heavier than 256 KiB stays in its depot and is in no
  read (`decision.clj:594-608`), beside his "we can send them back".
  `src/proposal/lane-2026-11-25/OPEN.md:85-94` already asks whether the two
  weights are his.
- O8. The voice and phone lines behind runtime page entries 7 and 8.
- O9. The permission page's section 9, items 12 and 13.

### 6.2 Engineering to settle before the fix that needs it

- E1. A run across lanes. Every act sits in one lane and a gate reads only
  its own lane. Say where a run's start and end acts are written, what each
  lane's gate reads to know the run is live, what an act of a run carries,
  and who checks that this agent may use that run. Blocks F28.
- E2. The stored form of "both are kept" (permission item 10). Today a place
  holds one current fact: a stale `:replaces` is refused
  (`decision.clj:245-251`) and an act names a place once
  (`declaration.clj:516-517`). Blocks F32.
- E3. A yes for one exact act. Today's yes names the act by its digest
  (`act.clj:16-19`), so what he approved is what lands. A give with one use
  allows any one act it covers. "How many times" and "which act" are two
  fields. Blocks F31.
- E4. Whether a refusal is the final answer of an act-id. If it is, the same
  act needs a new id after any refusal, and acts that pointed at the old id
  point at a refused try (run 2, finding 1). Blocks the act-id's form in
  F22.
- E5. The rules' edition. `:version` is "the edition of the list of parts"
  (`act.clj:29-31`); 13 October entry 6 makes it also the edition of what a
  gate accepts. A store key was added under version 1 and one case is
  handled by hand (`declaration.clj:288-294`). Say what a second run decides
  for old acts after a gate rule changes. Blocks F26.
- E6. A lane's depot: chosen at making only, or changeable later; and where
  it is looked up once it leaves the act-id. Blocks F22.
- E7. A backfill of a marked key cannot run inside one decision of a large
  lane. Today's "since" stamp per key exists for that reason.
- E8. Erasure in Auth. The email is the key of the invites depot
  (`schema.clj:152`) and must be erasable "its events too"
  (`SPEC.md:443-446`). Blocks F18.
- E9. What the yes page gives when a program is approved. Blocks the surface
  of F15.

### 6.3 Claims of other reviews that nobody checked

- Neon: a lane's last stamp is kept per gate, so a thing sent to the wrong
  depot takes a stamp from a second count (`walk.clj:279`, by grep). It goes
  away with F22.
- Carbon: a read with `:stores-own-facts` lets a visitor list every
  permission of a public lane. For the permission session.
- Neon and carbon: Auth allows agents 32 deep and the store 31
  (`OPEN.md:95-98` lists it).
- Neon: `Caller.until` is stale after a secret is replaced; a browser cannot
  replace its own cookie's secret; a query's outer vector uses one of the 32
  levels of a value.

## 7. To fix

"Follows from" names the decision, or says engineering. Details and quotes
are in the reports; the last column says where.

### 7.1 Can start: no stored form of the store or of Auth changes

| # | Fix | Where | Follows from | Detail |
|---|---|---|---|---|
| F1 | The sentence reads "What one gate puts in order is a lane." `src-store/CLAUDE.md:47-48` says the same and follows. | `README.md:1172-1175` | DECIDED 3.1 item 5 | run 5, break 1 |
| F3 | The spec says `:own-layer` on 24 lines; the code says `:own-lane`. | `SPEC.md`; `fit.clj:152` | engineering | run 1, finding 9 |
| F4 | Code cites an entry for a sentence the entry marks as a session's. | `seam.clj:39-40`; `words.clj:10-11`, `31-34` | engineering | run 5, break 8 |
| F5 | Page lines that no longer match the code. | `read_path/README.md:63-65`, `140-142`, `327`, `373`, `385` | engineering | run 3; neon T3-5 |
| F6 | A program's secret is posted on every poll before her yes; two docstrings say it is not. | `client.clj:78`; `credential.clj:19-20` | engineering | run 4, finding 5 |
| F7 | `write e k` with no value sends a retract; text that is no EDN is stored as a string. | `main.clj:175-180`, `397`; `frame.clj:59` | engineering | run 4, finding 1 |
| F8 | `send` reuses ids when the count of acts is the same. | `main.clj:373-378` | engineering | run 4, finding 2 |
| F9 | Exit status 0 covers her "no" and a failed watch; 3 covers a "could not". | `main.clj:184-191` | engineering | run 4, finding 6 |
| F10 | "Could not" words used for what their entry does not say; the library drops the runtime's word on a refused opening. | `line.clj:115-123`, `222`; `main.clj:270`, `486`; `client.clj:34-36` | engineering | run 4, findings 7, 8, 10 |
| F11 | The line's `:id`: a refused watch answers two ways; a second watch under a live number ends the first in the library; answers with no number are never read; `watch` ignores what the library returns. | `line.clj:115-123`, `155-156`; `client.clj:100-106`; `main.clj:409-414` | engineering | run 4, finding 9 |
| F12 | The wire table's `:who` column is read by no code. | `words.clj:78-111` | engineering | run 4, answer a |
| F13 | An act with one part more is refused `:unknown-part` before its version is read. | `act.clj:245`; `declaration.clj:266-270` | engineering | run 2, finding 4 |
| F14 | An agent can be signed in again. The spec allows a yes to name a live agent; the runtime never does. The yes page's new choice is shown to Sid. | `SPEC.md:385-388`; `sign_in.clj:214`; `seam.clj:643-651` | engineering; his reply 9 | carbon 4.1; neon T2-1 |
| F15 | No program under a person's who: the yes "as herself" goes for programs, and `person?` tests what its name says. Needs F14 and E9. | `SPEC.md:369-370`; `sign_in.clj:214`; `seam.clj:164-166` | REPORTED; his replies 1, 5, 8, 9, 11 | merge, finding 2 |
| F33 | `:no-fact` in an answer means both "never written" and "not given to you". | `answer.clj:244`; `watching.clj:73` | engineering | run 3, finding 3 |
| F34 | The query word `:run` names a range of places; "run" is now a party. | `query.clj:10`, `14`, `119` | engineering; permission item 8 | run 3, finding 4 |

### 7.2 Need Sid's yes on a changed stored form or page

| # | Fix | Where | Follows from | Detail |
|---|---|---|---|---|
| F2 | Entries that carry his name without his line are marked "a session's default, Sid's to turn": the calls of 22 November, "writing Auth first", 13 October entry 6, 6 October entries 4 and 5. | `SPEC.md:9-21` and its ten marks; `README.md:114-123`, `967-977` | REPORTED for 22 November; his rule in item 15 | run 5, breaks 2, 4, 6 |
| F16 | `:driven-by` goes from Auth's session, the wire and the command. | `schema.clj:36-37`, `94`; `http.clj:611-612`; `words.clj:206-213`; `main.clj:487-488` | REPORTED | run 1, finding 4; neon T1-14 |
| F17 | "Its time passed" is no longer stored as "ended". | `SPEC.md:404-422`; `schema.clj:908-950` | engineering | carbon 4.5; neon T1-17 |
| F18 | A person can have several accounts, and an erased email is freed. Needs E8; O2 goes with it. | `schema.clj:83-85`, `152`; `SPEC.md:261-265` | 9 October entry 1; REPORTED | run 1, finding 5; neon T1-16 |
| F22 | The depot word leaves the act-id, and a lane's members choose its depot, default streaming. Needs E4 and E6. | `act.clj:78-86`; `first_act.clj:22-24`, `51-60`; `additions.clj:112-115`; `words.clj:125`, `132` | 7 October entries 3 and 4 | merge, finding 3; run 2, finding 1 |
| F23 | A fact's key may be an id or a keyword. | `act.clj:222-223` | REPORTED; `README.md:161-178` | neon T1-8 |
| F24 | `:subjects` and `:claimed-when` leave the act. | `act.clj:66-67`; `sign_in.clj:67`, `154`, `285`; `main.clj:431` | A SESSION'S | run 2, finding 3 |
| F26 | The rules' edition is kept apart from `:version`. Needs E5. | `act.clj:29-31`; `README.md:967-977` | engineering | carbon 1b; neon T1-6 |

### 7.3 Wait for the new permission design

| # | Fix | Where | Follows from | Detail |
|---|---|---|---|---|
| F28 | `:session` leaves the act and the run takes its place. With it go `:session-for`, the making act's session, and the two writes under `open-session`. Needs E1. | `act.clj:63`; `seam.clj:286-291`, `366-369`, `406-412`; `digest.clj:59-62`; `main.clj:313` | DECIDED: permission item 8; 3.1 item 6 | merge, finding 1 |
| F29 | "Everyone" is a holder, and what a lane starts with is in the log, not in code. | `first_act.clj:59`, `64-79` | DECIDED 3.1 item 3 | run 3, finding 1 |
| F30 | `made-by` in Auth is split. Needs O6. | `schema.clj:1161-1170`; `seam.clj:623-634`, `668` | engineering; permission items 5, 6 | run 1, finding 7 |
| F31 | The yes for one act keeps "this exact act". Needs E3. | `act.clj:14-23` | A SESSION'S | section 6.2 |
| F32 | Two acts written from the same one are both kept and the conflict is raised. Needs E2. | `decision.clj:245-251`; `declaration.clj:516-517` | DECIDED: permission item 10 | section 6.2 |
| F25 | References between acts go both ways and take more than one. Today `:because-of` holds one act-id and nothing reads it. | `act.clj:64-65`, `102-104` | DECIDED: permission items 9, 10 | run 2, finding 3 |

### 7.4 Limits of the read path, to carry; nothing is stored on them

- A read that starts from the base is served by the base's one task
  (`lanes.clj:184-193`, `read_path.clj:160-162`).
- Builds-on is walked one level (`lanes.clj:171-172`).
- Marked keys are one set of at most 32 (`declaration.clj:73-77`).
- A run of places stops at 20,000 with no way to continue (`query.clj:86`).

### 7.5 Not to build

- Refused writes dropped from the lane (carbon's item 10). Reversed by 3.1
  item 1.
- Builds-on dropped (carbon's item 8). Reversed by 3.1 item 2.
- Anything on the ground that decimals break `=`. They do not.

## 8. Order

1. Carbon and neon review this page. Their objections go to Sid.
2. Group 7.1 can start then. Each fix is small and none waits on another,
   but for F15, which needs F14 and E9.
3. Group 7.2: each fix shows Sid the new stored form or the page edit, and
   starts on his yes. Every change of the act's form remakes the fixture and
   the digests, so F22, F23 and F24 go in one change.
4. Group 7.3 waits for the build page of the new permission design and for
   E1 to E3. F28 changes the act's form a second time. A session may hold
   F22 to F24 until then, to change it once.
5. Pieces 5 to 8 start after the fixes (SID'S, permission page, item 17).

A fix session starts fresh, with this page and the docs of the tree it
changes as its base.

## 9. Not checked

- The record. The review session did not open it. Run 5 did, on 26 November,
  for the pages' entries; carbon and neon did for theirs.
- Carbon's and neon's quotes of his lines, and the items of section 4.
- The permission page was read only in its sections 1, 6, 8, 9, 10 and 11.
- Nothing was run but one expression on the JVM.
- The cost of each fix. Nothing here is measured.
