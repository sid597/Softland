# The merge of the five runs

A review of 26 November 2026 with the skill `design-pitfalls`
(`.claude/skills/design-pitfalls/SKILL.md`). Five reviewers on Fable at xhigh
effort each took one run; their reports are beside this file, word for word:

- [run-1-who-is-acting.md](run-1-who-is-acting.md)
- [run-2-the-act-as-kept.md](run-2-the-act-as-kept.md)
- [run-3-lane-settings-and-reads.md](run-3-lane-settings-and-reads.md)
- [run-4-what-crosses-the-wire.md](run-4-what-crosses-the-wire.md)
- [run-5-pages-that-say-decided.md](run-5-pages-that-say-decided.md)

The session that briefed them wrote this merge, as it was given to Sid in the
chat. It is that session's view, and nothing in it is Sid's decision. The
session checked the lines each run's main findings quote against the files and
the commit times. It did not open the record, so Sid's lines below are as run 5
quotes them, with their ids. Permissions were outside these runs: a separate
session reviews them.

---

**My view:** the store's core holds: order and time are apart, account and person are apart, and the wire fixes nothing of an act. What breaks has the lane's shape: one stored thing carrying several jobs, and a session's reading written down as your decision. The five runs raised 38 findings; seven matter, and the first three are in every act stored now.

## What breaks

### 1. `:session` on an act does four jobs (runs 1 to 5)
- **A sitting or run in Auth:** a record with a sign-in, a time and what drives it; no act is sent without a live one (`seam.clj:286-291`).
- **Part of what makes a resend the same thing:** the digest leaves out only `:door-time` (`digest.clj:59-62`). A resend under the same act-id with another `:session` is told `:not-mine` (`door/words.clj:87`, by grep).
- **The "run" a permission covers,** at the gate and at a read, and it is inside the digest a yes holds (`act.clj:14-19`, `sources.clj:114-116`). The permission session judges this one.
- **The only place that says what drove an act,** and through which sign-in it came.

They differ in count and in when they change: the resend job is one per act and must never change, while a sitting covers many acts and ends.

The strain shows in four places:
- A page derives a session per form post (`seam.clj:366-369`).
- The act that makes her lane names a session that "is no session of Auth's" (`SPEC.md:304-306`).
- The command keeps one `:session` in a file across every command (`main.clj:313`).
- `send` run again after `new-session` no longer sends the same things (`main.clj:373-382`).

Where it came from: the runtime page's entry 7. Your typed line was "3A (i think need more)" (‹93a88844›). The sentence that gives `:session` both jobs is carbon's, which the page says you "did not contest" (`PROPOSAL.md:166-176`).

### 2. Her id on an act means "she did this" and "something she let act as her did this" (runs 1, 4, 5)
- `person?` tests who equals root, which is "not an agent" (`seam.clj:164-166`). Its docstring says "the person herself, signed in at a browser" (`sign_in.clj:202`).
- A program she says yes to "as herself" gets her root as its actor (`sign_in.clj:214`). It passes the test, may say `:invite`, `:under` and `:end-sign-in`, and its acts carry her `:who`.
- "Agent" as built is "an actor that another actor made" (`SPEC.md:51-52`). A model signed in as you is not one; a script with no model made by `:make-agent` is. Run 5 found no line of yours behind that sentence.
- What drives is said on the session by whoever opens it, unchecked. The command says it only with `new-session --model` (`main.clj:488`).
- The runtime page's entry 8 says each program "has a `:who` of its own" (`PROPOSAL.md:178-186`). The spec then added "yes, as herself" (`SPEC.md:369-370`).

### 3. The act-id is the name of a try, what other acts point to, and the route (runs 2 to 5)
- It is `[lane depot uuid]` (`act.clj:78-82`). One act can go under several act-ids (`act.clj:20-21`), while other acts point at one, through `:replaces`, `:based-on` and `:because-of`.
- The depot word is a function of the lane's id alone (`first_act.clj:22-24`). Each thing still stores it three times, with tests to keep them equal (`additions.clj:112-115`, `declaration.clj:441-442`).
- Two reads already rebuild it from the lane (`acts.clj:181`, `answer.clj:77`, by grep).
- The pages disagree on who makes it. The store's page says "The door makes act-ids" (6 October, entry 5); the runtime page says "an act-id that the maker made" (`PROPOSAL.md:170`); the code has the maker send the UUID (`seam.clj:45-48`).

### 4. What a lane is when no fact says it is decided by code, from its id (run 3)
- `starts-with` gives a lane's settings from its id: who may see the base is `:everyone` (`first_act.clj:64-79`). A making act can hold no setting (`first_act.clj:59`).
- A caller who reads that place gets `:no-fact`, so each caller must carry the same function.
- A change to the function changes existing lanes with no act in their line. That is a truth outside the log.
- The id `:base` alone also decides which gate decides it and where its rows are read (`first_act.clj:24`, `sources.clj:102`).

### 5. "Sid's call of 22 November" has no line of yours behind it (run 5)
- A session proposed the two rules under "## Your calls" («c07bb587»). It then wrote them in your first person into prompts: "My calls of 22 November on Auth's contract" («6c34ec12»). You pasted the prompts.
- A later session text says "Your yes to the two calls is already given, since you pasted them" («86aec2fc»).
- The replace-secret rule was added afterwards by a session "under my call 2" and stands in the spec as your call (`SPEC.md:323-326`).
- For questions 10, 17 and 24, which "stand", you were shown the numbers and not what they say, in what run 5 opened.
- Auth's contract is built on these (`fit.clj`, `schema.clj`).

### 6. The lane (run 5)
- The rename was committed as "Sid's word of 25 November 2026" at 11:47 UTC (`9d08cf53`). Your line "yes lane is the name / yes put in readme" (‹d5172c3f›) is 18:15 UTC.
- The note says to read "lane" where "layer" means what one gate puts in order, and names no entry. Which earlier "layer" of yours is a lane is left to each reader.
- `:own-lane` is built on "their personal layer" (‹f3f530b8›).
- The Auth spec still says `:own-layer` on 24 lines; the code checks `:own-lane` (`fit.clj:152`).

### 7. Parts of every act that nothing reads, fixed from photographs (runs 2, 5)
- `:claimed-when` has no unit, no writer and no reader. `:because-of` and `:subjects` have no reader beyond a check of id length. All three are inside both digests.
- The entry that fixes them (6 October, entry 4) rests on a session's reading of three photographs that the record does not keep; the page says so itself (`README.md:97-98`).
- The session's own picks, shown to you just before, had said the opposite on version and on who makes the act-id («a78296bc»).

### Smaller, and certain
- **A slip retracts a fact.** `write e k` with the value left out sends `:v nil` (`main.clj:397`, `frame.clj:59`), and a nil `:v` is a retract (`act.clj:413-414`).
- **A secret crosses early.** A program's secret is posted on every poll before her yes (`client.clj:78`), against two docstrings.
- **An email claim's key** must stay for good and must be erasable (`schema.clj:152`, `SPEC.md:261-263`, `443-445`).
- **"Run" names two things** in one file: a range of places, and a session's run (`query.clj:10`, `14`).
- **`:no-fact` in an answer** means both "nothing was ever written here" and "no lane you may see gives this place" (`answer.clj:244`).
- **`:acts` in a query** gives decisions, refused things among them (`acts.clj:2-4`, `117-118`).
- **Exit status 0** also covers her "no" and a watch that ended in a "could not" (`main.clj:184-191`).
- **Three "could not" words** are used for things their table entry does not say (`line.clj:116`, `120-123`, `222`; `main.clj:270`).
- **The wire table's `:who` column** is read by no code; the rule lives in the seam.
- **`made-by`** is who made an agent, whose end ends it, and who may end it.

## What holds
- A stamp reads no clock, and the time is outside the digest (`stamp.clj`, `digest.clj:59-62`).
- Account and person are two things. A sign-in, its secret and a session are apart.
- Who acts, how the credential is carried and what drives sit in three places.
- The wire has no word for a feature of the product, and its check reads nothing inside an act or a query (`words.clj:6-8`).
- Builds-on is a default for a read, not a rule of the lane. Once or live is not in the query. The read path stores nothing.
- A fact with no value has one meaning per kind of key, and is refused where none is built.
- A depot keeps each thing as given, so the finest grain is kept.
- On the store's page, the entries of 7 October, 12 October (1 to 7) and 14 October say what your lines say.

## What is still open
**Yours:**
- What `:session` is to be, and what "agent" and "as herself" put on an act.
- Whether pasting the prompts of 22 November is your yes to the two rules.
- Which earlier "layer" entries are lanes.
- Whether one act under two act-ids is one act or two, and whether a refused thing is an act.
- Whether the copy of the own lane on each sign-in falls under the stop on caching.

**The permission session's:**
- Who may see a lane when no fact says it.
- What bounds "one run"; a caller can keep one session open without end.
- "Any program that can send can name `system`" (`first_act.clj:5-7`).
- Whether a program signed in as her may invite and end sign-ins.

**In no run:**
- The runtime's `http.clj` and `google.clj`.
- The store's `door.clj`, `module/walk.clj`, `gate/decision.clj` and `record/rows.clj`. Several "by grep" grounds above sit there.
- Antigravity's files. The voice and phone lines behind runtime page entries 5, 7 and 8 are outside the record; the page itself marks them "carbon, phone".

## Scope kept, and where it slipped
- **Run 5 broke "write no file" once.** It wrote four parts of one session text to the briefing session's scratchpad and deleted them in its next call. The scratchpad was empty and the working tree clean when checked.
- **Run 5 read more of the spec** than its listed lines, and one of its greps may have matched key names in the store's tables file.
- **The record:** runs 1 to 4 and the merging session did not touch it. Run 5 made 77 calls and checked 84 of 100 entries; 13 are pointers to the permission session and 3 were not checked.
- **No gatherer** was used by any run; each reviewer read its own files.
- **Outside their files** the runs only grepped. Run 5 also opened seven short windows of code and ran read-only `git log` and `git show`, which its brief allowed.
- **Not checked by the merging session:** Sid's lines in the record, the full lists of jobs per id, and the pointer lists.

## Short file names used above

- `seam.clj`, `sign_in.clj`, `line.clj`: `src-runtime/softland/runtime/`
- `schema.clj`, `fit.clj`: `src-auth/softland/auth/`
- `words.clj`, `frame.clj`: `src-wire/softland/wire/` (`door/words.clj` is the store's)
- `main.clj`, `client.clj`: `src-command/softland/command/`
- `act.clj`, `first_act.clj`, `digest.clj`, `additions.clj`, `declaration.clj`, `stamp.clj`, `sources.clj`, `query.clj`, `answer.clj`, `acts.clj`: `src-store/softland/store/` and its `door/`, `gate/` and `read_path/`
- `SPEC.md`: `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`
- `PROPOSAL.md`: `src/proposal/runtime-2026-11-21/PROPOSAL.md`
- `README.md`: `src-store/README.md`
