# Run 1 of 5: who is acting

A review of 26 November 2026 with the skill `design-pitfalls`
(`.claude/skills/design-pitfalls/SKILL.md`). One reviewer on Fable at xhigh
effort wrote the report below, and it is kept word for word. The brief the
reviewer was given is at the end. The merge of the five runs is in
[merge.md](merge.md).

---

# Run 1 of 5, design-pitfalls: person, root, agent, sign-in, session and run, `:who`

**Verdict.** The ids on this path are mostly kept apart well. Nine things break against the rule; two of them are already being stored in acts (`:session`, and `:who` for a program signed in as her).

A word on words: below, "`:session`" or "Auth's session" is the product's thing. The agent session that wrote this code I call "the build's author".

Forks I took:
- **"end" and "yes" in bound 6.** I read them as the store's permission words. Auth's own ends (`end-sign-in`, `end-actor`) and the yes of an ask I treated as the life of a sign-in and of an agent, which are mine. I list who triggers them, give a pointer, and do not judge who may.
- **"The store's tables".** I took this to be `src-store/softland/store/record/tables.clj` and kept it out of every grep.
- **Greps.** Over `*.clj` only, so no page of `src-store/` that holds "Decisions of …" was touched.
- **Gatherer.** None used. Every read and grep is my own.

## 1. What I read, and what I did not

**Read whole:**
- `src-auth/README.md`, `src-auth/softland/README.md`, `src-auth/softland/auth/README.md`
- `src-auth/softland/auth/schema.clj` (1,595 lines), `module.clj` (622), `client.clj` (710), `fit.clj` (222)
- `src-runtime/softland/runtime/seam.clj` (999), `sign_in.clj` (294), `standing.clj` (162)
- `src-wire/softland/wire/credential.clj` (124)
- `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md` (499)
- `.claude/skills/design-pitfalls/SKILL.md`

**Not done:** no memtool, no softland-memory, no recall, no write, no process, no git.

**Greps outside the reads** (`*.clj` in `src-store`, `src-wire`, `src-command`, `src-runtime`, without `tables.clj` and without my own files). Patterns: `:who`, `:session`, `run|sitting`, `:asker|:not-signed-in`, `driven-by`, `own-lane`, `:root`, `:subjects`, `(def system|base`, and in the unread runtime files `:carrier`, `:session-for`, `:driven-by`, `seam/…` and `sign-in/…` calls. The hits I use are quoted in section 2 as "grep, not reviewed". The rest was noise, such as "a run of rows" and `:who-may-see-word`.

**Files I would have needed and did not open:**
- `src-runtime/softland/runtime/http.clj` and `line.clj`: whether every caller that reaches `answer!` and `name!` is made from a cookie, and whether a page's `:session` is ever closed.
- `src-runtime/softland/runtime/google.clj`: whether the `:email` handed to `enter!` is only one that Google marks as verified.
- `src-store/softland/store/door/digest.clj`: whether the digest covers `:session`.
- `src-store/softland/store/gate/decision.clj` and `act.clj`: what the store decides from `:who` and `:session`.
- `spine/auth/PLAN.md`: how erasing "is kept possible".
- `test-auth/softland/auth/QUESTIONS.md`: what questions 10, 17 and 24 are.
- The runtime's page (`PROPOSAL.md`): entries 2, 5, 7, 8 and 12.

## 2. The inventory

Each job ends with: decided when; can it change; one or many; who changes it.

### Person (a UUID)
1. **Names a human being, also before she has an account** (the invitation). Made at `sign_in.clj:257` (random, at setup) and `seam.clj:730` (hashed from the inviter's root and a UUID). Stored at `schema.clj:431-434`, `537-539`, `806-810`, `838`. At invite; never; one; nobody.
2. **Is an actor**: the who of everything sent under a sign-in whose actor is her. Written at `sign_in.clj:118` (a browser) and `sign_in.clj:214` (a program "as herself"). Read at `seam.clj:341-344`. At begin of each sign-in; never; one id for many sign-ins.
3. **Is the root** (next heading).
4. **Is written in acts**: as `:who`, in `:subjects`, as the entity of her name and of her permission fact, and as holder in permission tuples (`sign_in.clj:59-67`, `149-155`, `285-287`). At write; for good.
5. **Is shown**: `about` (`seam.clj:173`); setup's result (`sign_in.clj:292`); grep, not reviewed, `http.clj:284`: `"<p>You are signed in. Your id is <code>" (escaped (seam/who-of caller))`.

### Root (the person's id, as "the person at the top of who made whom", `SPEC.md:53`)
1. **Where everything under one person is kept, and the one order it is decided in**: the partition of `*root-events` (`module.clj:532`), the front of every key of `$$root-data` (`schema.clj:178-239`), the key of `$$live` (`module.clj:545`). For an agent: at make; never; one.
2. **First part of the credential**: the address of the sign-in's record and no proof (`credential.clj:9-16`, `77-86`; read at `seam.clj:334-336`). At begin; for the sign-in's life.
3. **What a write takes out under**: each write that names a root ends what has passed its time under the whole root (`SPEC.md:404-409`; `module.clj:61-71`).
4. **The unit of Auth's watch** (`client.clj:616-710`, `standing.clj:94-104`, `module.clj:553`) **and of the cap on watches** (`seam.clj:513-518`).
5. **An input of derived ids**: an agent, its sign-in, an invited person, a page's `:session` (`seam.clj:611-621`, used at `351`, `643-644`, `730`).
6. **The scope in which Auth checks an id** (`SPEC.md:88-89`: "checks an id only within its root").
7. **Compared with who to tell an agent from the actor at the root** (`seam.clj:164-166`; `schema.clj:972`, `1099`, `1105`).
8. **The person who answers an ask**, under whom a new agent is made (`sign_in.clj:208-218`; `schema.clj:1388`).
9. **Told** to a system (`seam.clj:173`) and to a program whose ask got a yes (`sign_in.clj:189`).

### Agent (a UUID; Auth's `:actor` when it is not the root)
1. **The `:who` of acts sent under its sign-ins.** Id made at `seam.clj:643` (hashed) or `sign_in.clj:214` (random). At make; never; one.
2. **Key of its record** `[actor made-by depth label ended? seq]` (`schema.clj:87`, `1115`).
3. **What a program signs in as** (`schema.clj:986`, `1366`, `1390`).
4. **What ends, with all it made and their sign-ins and sessions** (`schema.clj:1145-1191`). Once; never taken back.
5. **`made-by`** does four things in one slot. It says who made it, shown in `under` (`schema.clj:1530-1534`). It decides whose end ends it (`schema.clj:1161-1170`). It decides who may end it (`seam.clj:623-634`, `668`). It gives its depth, held to 32 (`schema.clj:1108-1113`). At make; never; one. See finding 7.

### Sign-in (a UUID)
1. **Binds one secret to one actor under one root** (`schema.clj:986`). At begin; never; one actor.
2. **Second part of the credential** (`credential.clj:77-86`).
3. **`kind`, `:browser` or `:program`**: which carrier it is taken from (`seam.clj:262`). At begin; never.
4. **Holds the secret's hash and time, and the one before** (`schema.clj:1035-1037`). Changes at each `replace-secret`, by its holder (`seam.clj:690-703`).
5. **What Auth's sessions are under**; its end closes them (`schema.clj:1061-1085`).
6. **Key of the pacing of session openings** (`seam.clj:500-505`).
7. **Is ended** by `:sign-out` and `:end-sign-in` (`seam.clj:705-707`, `749-754`), by its time (take-out), and by the end of its actor.
8. **Carries a copy of the root's own lane** and of "made at begin" (`schema.clj:92`, `984-986`). See finding 6.
9. **`initial-hash`**: kept so that the same begin is known again (`schema.clj:965-969`).

### Session and run (a UUID)
There is no id for a run in my reads. "Run" is the spec's word for a program's `:session` (`SPEC.md:59`: "One sitting of a person or one run of a program").

1. **Names what an act was sent in**; held against the caller (`seam.clj:412`). Stored on every act. At send; for good.
2. **Is a record of Auth** `[session sign-in until closed? driven-by]` (`schema.clj:94`, `1222`) that is live or closed. Opened once; time moved on by the runtime (`seam.clj:293-304`); closed by `:close-session`, by its time, or by the end of its sign-in or actor; never opened again (`SPEC.md:358-361`).
3. **Is the gate of sending**: no act without a live `:session` of the caller's own sign-in (`seam.clj:286-291`, `391-395`).
4. **Is part of what makes a second send the same act** (`seam.clj:29-31`; `SPEC.md:306-307`).
5. **Carries `:driven-by`** (below).
6. **Its id is random** (`seam.clj:354`, `712`) **or hashed from the root and a UUID that a page made for one act** (`seam.clj:351`).
7. **On an act of the system it names no session at all**: it is `making-session`, kept by Auth (`SPEC.md:304-306`: "that session is no session of Auth's"), or a random id kept nowhere (`sign_in.clj:266`).
8. **May be named on a query** (`seam.clj:424-427`).
9. **Grep, not reviewed: in the store it is "the run" that a permission can cover.**
   - `act.clj:14`: "A permission whose sixth slot is `[:run session]` covers one run: the"
   - `decision.clj:171-172`: "One for a run covers the acts that carry that run as their `:session`, of the permission's holder alone."
   - `decision.clj:202`: `(and (= :run kind) (not= what (:session act)))`
   - `sources.clj:313`: `(= k (rows/place-key lane (:session query) :softland.store/run-permission))) :run-permission`
   - `query.clj:10`: "`:session` is the run that the read is asked in, as an act names its"

### `:who`
1. **On an act: the actor that sent it**, written by the sender and held against the caller (`seam.clj:19-23`, `411`). For the system's acts it is the store's `system` (`seam.clj:775`; `sign_in.clj:62`, `281`). At send; for good.
2. **On the caller: the actor of the proven sign-in** (`seam.clj:341-344`); nil means a visitor (`seam.clj:126`, `388`).
3. **What a query's `:asker` must equal** (`seam.clj:420`).
4. **The value of `:made-by` and of `:invited-by`** (`seam.clj:646`, `734`).
5. **Grep, not reviewed: what the store decides from it.**
   - `decision.clj:199`: `(not= (:who act) (nth permission 0))`
   - `first_act.clj:57`: `(or (= (:who act) holder) (= (:who act) system))`
   - `rows.clj:109`: `:time (long time-millis) :who who-text :permission permission-text}`
   - `acts.clj:120`: "Her own: a decision whose thing names her id as its `:who`, the line"
6. **Grep, not reviewed: the same key means another thing in the wire's table.** `words.clj:78`: "`:who` is who may say the word: `:anybody`, a visitor too; `:an-actor`,"

### Others that carry who is acting
- **Account** `[provider subject]`. What a provider has proven (`schema.clj:142-147`); the key of `$$accounts` and the partition of `*enters` (`module.clj:535`); copied into the root's record (`schema.clj:808`), where nothing in my reads reads it.
- **Claim** (`e:<email>` or `c:<hash>`, `schema.clj:149-159`). Says who may become a waiting person; keeps a used claim from naming another person; holds her email in plain. See finding 5.
- **`:kind`** of a sign-in: above.
- **`:driven-by`**: `{:kind :person}`, `{:kind :model :model text}`, or absent (`fit.clj:137-141`; `schema.clj:36-51`). Said by whoever opens the session (`seam.clj:715`; grep, not reviewed, `http.clj:267`, `612`: `:driven-by {:kind :person}`; `main.clj:488`: `{:kind :model :model (or (:model flags) "")}`). Nothing checks it. At open; never; one for a session. The only place in my reads that reads it is `about` (`seam.clj:176`). See finding 4.
- **`:asker`**: who asks a read; must be the caller's who, or absent for a visitor (`seam.clj:414-427`).
- **The system**: an id in acts (`first-act/system`), the keyword `:system` in Auth (`client.clj:460`). Two forms for one actor.
- **`:making {:uuid :session}`**: the two ids of the act that makes her own lane, kept for each person for good in one event and three records (`schema.clj:59-60`, `83-85`, `113-116`).
- **Own lane**: one per person, fixed at her first enter (`schema.clj:806-810`). Kept in the root's record, the link, each sign-in's record, and each `who-is`. For an agent's caller it is its root's lane, not the agent's (`schema.clj:984`; `seam.clj:344`). Grep, not reviewed: the command takes it as its default lane (`main.clj:310`: `(let [lane (if lane (value-of lane) (:own-lane hello))]`).
- **The caller** (`seam.clj:124`): a snapshot of who, root, sign-in, `:session`, kind, hash, until, own lane, `:driven-by` and from, made once for a request or a connection.
- **An ask and its code**: the short text she types within ten minutes (`sign_in.clj:45-47`), and the ask's key for good (`SPEC.md:393-395`).
- **`:invited-by`**: who invited; kept and read by nothing in my reads.
- **`op-id`**: a random id for one call of `enter` or `answer-ask` (`client.clj:471`, `542`).

### What is fixed for good
- **Stored in an act**: `:who` (person, agent or system), `:session`, the person's id in `:subjects`, facts and permission tuples, and the own lane's id.
- **Shown to someone**: the credential text (root and sign-in); the hello `{:who :root :sign-in :session :kind :own-lane :until}`; her id on the signed-in page; `under`, with `:made-by` and labels; an ask's answer.
- **Written on a page as decided**: the contract in `SPEC.md` ("This contract is fixed here", line 78) and the lines in section 6.
- **What Sid was shown and what he answered**: not read. The record is run 5's.

## 3. What holds

- **Account and person are two things.** The link is a record of its own, a person's id says nothing, and nothing of the account is in an act. Asked: questions 1, 2, 4.
- **Root.** It has many jobs, but each is decided once, never changes and is one per actor. None of them is in an act, and the credential has a version (`sl2_`). Asked: 1, 2, 4, 5. By the brake this is no finding.
- **A sign-in's id and its secret are apart**, and so are a `:session` and the secret: `replace-secret` touches a `:session` only when the new time has already passed (`schema.clj:1038-1055`). Asked: 2.
- **Three things that could have been one are three.** Who acts is the actor. How the credential is carried is the sign-in's kind. What drives is the `:session`'s `:driven-by`. Asked: 1, 3.
- **Who invited is apart from who made whom.** An invited person is her own root. Asked: 1.
- **The root in a credential is only an address.** A sign-in is found only under its own root (`module.clj:568`), and the proof is the secret.
- **A who from the wire cannot become a caller.** Both an act's `:who` and its `:session`, and a query's `:asker`, are held against a caller that only the seam makes (`seam.clj:406-427`).
- **An end is pushed and then read again** before anything is ended (`seam.clj:809-817`, `866-906`).
- **`kind` and the depth of 32 are values**, not forms. Asked: 3.
- **An ask's code** does two jobs with two lifetimes, ten minutes and for good. I raise nothing: there are about 8.5 × 10¹¹ codes, none is in an act, and the table can be keyed anew.

## 4. What breaks, by what is fixed for good soonest

### 1. `:session` on an act does two jobs that differ in count and in when they are decided
- **The jobs.** It names the sitting or run an act was sent in (Auth's record: a sign-in, a time, `:driven-by`). It is also part of what makes a second send the same act.
- **How they differ.** Which act it is, is decided once by its maker and must be the same at every send: one per act. Which sitting it is sent in, is decided by the connection at each send and can differ between two sends: the session closed, its day passed, she signed in again. One for many acts.
- **The reads show the wall three times.**
  - `seam.clj:29-31`: "The session outlives the secret: `:replace-secret` changes no session, so an act that is sent again after a break, under the same UUID, has the same digest and is taken once."
  - `seam.clj:366-369`: "`:session-for` a UUID that a page made for its one act … the session is derived from the root and it (`derived`), so that a form that is posted twice is in one session". For a page, Auth's "one sitting of a person" is then one form post. Grep, not reviewed, `http.clj:266`: `(uuid? act) (assoc :session-for act`.
  - `SPEC.md:304-306`: "`:making` holds its UUID and the id that it names as its session. Auth keeps the two and gives them back, and nothing more: that session is no session of Auth's."
- **A third job, by grep.** In the store this id is also the size of a permission "for one run" (inventory, session 9). In my reads the caller decides how long that is: it can keep one `:session` open without bound by naming it again (`seam.clj:347-349`).
- **Cost now.** The act's form and the digest are the store's, so I cannot price that side. On this side, `making-session` and `:session-for` exist only for this.
- **Cost later.** Every stored act holds its `:session` and, by these lines, was digested with it. A change later leaves two rules in a log that is never trimmed.
- **Certain from what I read:** the jobs and how they differ. **Depends on** `digest.clj` (run 2) for whether the digest covers `:session`, which my reads say twice but do not show.

### 2. "Who is the root" is read as "the person herself"
- **The test.** `seam.clj:164-166`: `(and (some? (.-who caller)) (= (.-who caller) (.-root caller)))`, named `person?`.
- **What it tests.** That the caller is no agent.
- **What is put on it.** `sign_in.clj:202`: "The caller is the person herself, signed in at a browser." `sign_in.clj:141`: "The caller must be the person herself, in a session."
- **Who else passes.** A yes "as herself" begins a `:program` sign-in whose actor is the root (`sign_in.clj:214`: `:actor (if (and yes? label) (UUID/randomUUID) root)`; `SPEC.md:369-370`). For that program `person?` is true.
- **How the jobs differ.** In count: one human, and many things that hold her who (each browser, each program she said yes to, each `:session` that says a model drives it).
- **What rests on it.**
  - `needs-the-person` (`seam.clj:397-400`) lets such a program say `:invite`, `:under` and `:end-sign-in` (`seam.clj:728-754`).
  - `answer!` tests `person?` alone (`sign_in.clj:206`). "At a browser" holds only if every face makes that caller from a cookie. Grep, not reviewed: `http.clj:264` `:carrier :cookie` and `http.clj:479`.
  - `name!` tests nothing (`sign_in.clj:140-157`).
- **Fixed for good.** Acts that such a program sent are stored with her id as `:who`. Only the `:session`, looked up in Auth under her root, tells them from her own, and only where `:driven-by` was said.
- **Cost now.** A name and three docstrings, and a call of Sid's on what "as herself" is to mean on an act. **Cost later.** Every act sent meanwhile.
- **Certain from what I read:** what the test tests and who passes. **Depends on** `http.clj` and `line.clj` for whether a caller from a header can reach `answer!`.

### 3. The word "agent" names something other than a model at work
- **What is built.** `SPEC.md:51-52`: "An **agent** is an actor that another actor made; it has an id of its own".
- **In examples.** A model-driven program that she signs in as herself is no agent, and its acts carry her id. A script with no model, made with `:make-agent` (`seam.clj:636-656`), is an agent. Whether a model is at work is said on the `:session`, never on the agent.
- **Against the root page.** It speaks of "humans and agents" working together. The built thing overlaps with that and is neither the whole nor a part of it.
- **Cost now.** A word. **Cost later.** The word is already in Auth's contract and on the wire (`:make-agent`, `:end-agent`, `:new-agent`), and it is shown in `under`.
- **Certain from what I read:** what is built. What Sid means by the word is not checked against the record: run 5.

### 4. `:driven-by` is given a second job by the spec
- **The jobs.** It says what drives a session. By `SPEC.md:351-353` it also decides a keeping: "When `:driven-by` is absent (or nil), it means not said, and the runtime then keeps that session's reads."
- **How they differ.** What drives is fixed at open and never changes (`SPEC.md:351`). The keeping, by the same paragraph, is "a person's until she turns it off": it changes later, and she changes it.
- **A second point on the same key.** The opener says it and nothing checks it, and its kinds are two record forms (`schema.clj:36-37`), so a driver that is neither has only "not said".
- **Cost now.** Nothing in my reads acts on the second job (`seam.clj:56-58`). **Cost later.** Sessions are stored with it for good.
- **Depends on** the trail, piece 5, which I did not open.

### 5. An email claim's key must stay for good and must be erasable
- **The key.** `schema.clj:152`: `:email (str "e:" val)`. It is the key of `$$claims`, the partition of `*invites` (`module.clj:534`), and it is in the root's record (`schema.clj:81-85`).
- **Job one.** `SPEC.md:261-263`: "A claim names one person for good: once an account has proven it, it is used up, and it never names another person."
- **Job two.** `SPEC.md:443-445`: "What names a human being is her account, her email, and what her provider said. All of it must be erasable later from everything that Auth keeps, its events too".
- **How they differ.** One must never go and the other must be able to go. With the row gone, `invite-step-1` finds nothing (`schema.clj:408-409`) and the email can name another person.
- **Cost now.** A key and a depot's partition. **Cost later.** A depot cannot be keyed anew.
- **Depends on** `PLAN.md`, which says how erasing is kept possible.

### 6. `who-is` answers two questions, and a copy is kept for the second
- **The jobs.** It answers whether this sign-in and `:session` stand, and what the root's own lane is.
- **The copy.** For the second, each sign-in's record holds the root's own lane and a mark, made at begin (`schema.clj:984-986`). `schema.clj:1464-1467`: "True lets `who-is` skip reading `r`."
- **How they differ.** Standing changes and is asked on every request. The own lane is the person's, one, fixed at her first enter, and used by `about`, `name-of` and `name!` alone.
- **The flag is unused.** `SPEC.md:311-312` says of it: "the flag of `who-is` informs and nothing acts on it".
- **Cost now.** One more seek; the slow path is written (`schema.clj:1491`). **Cost later.** Little: the table can be built again from the depot.
- **Certain from what I read.** Whether a copy made at write falls under the root page's stop on caching is Sid's.

### 7. `made-by` is who made an agent, whose end ends it, and who may end it
- **How they differ.** The first is a fact of the past and cannot change by its nature. The other two are arrangements that take effect later (`schema.clj:1161-1170`; `seam.clj:668`).
- **Why I raise it.** The root page names this pair: "provenance vs. cascade". Nothing in my reads needs them to differ today.
- **A related fact.** What `made-by` records follows how the maker was signed in: a program as herself records the person (`seam.clj:646`).
- **Cost now.** One field. **Cost later.** Low: it is in Auth's events and in no act, and old events can take the maker as default.
- **Certain from what I read.**

### 8. `open-session` is two writes under one name
- **The jobs.** It opens a session once, and it moves an open session's time on, many times (`schema.clj:1203-1212`).
- **What that costs today.** Its result cannot tell the two apart. `seam.clj:295-297`: "a repeat of `open-session` gives `{:open S}` even on a closed session, so the next look must read the session's real standing from Auth." Each opening is therefore followed by a read (`seam.clj:326`).
- **Certain from what I read.** The spec marks this as question 17: run 5.

### 9. The contract's page still says "layer"
- **The page.** `SPEC.md:163`: `:code-hash H-or-nil :own-layer L :making {:uuid U :session M}`. The word is on some twenty lines, and "lane" on none.
- **The code.** `fit.clj:152`: `:own-lane id? :making making? :at time-val?}`. By the spec's own rule of what fits, an input written as the page has it is refused.
- **Who cites it.** `client.clj:3` and `fit.clj:3` cite this page as the contract.
- **Certain from what I read.** Whether the page is to follow the code or stand as it was written is Sid's and run 5's.

## 5. What is still open

**Sid's:**
- Whether a program signed in as herself is to carry her who (finding 2), and what "agent" names (finding 3).
- How long a closed `:session`'s record must stay readable. An act's `:session` means nothing without it, and the spec says both "may one day be thinned" (line 35) and "Nothing is deleted" (line 471).
- Whether an agent's "own lane" is meant to be its root's.
- An invitation has no time and cannot be taken back (`SPEC.md:267-268`).
- Who reads `google.clj`: no run in my brief is named for it.

**Run 2:**
- What the digest covers.
- What a second send of an act in another `:session` meets.
- The times `:at` and `:until`, and the runtime's clock as judge.

**Run 3:**
- The rename changed the class names of depot records (`schema.clj:60`, `67`). Whether any depot holds records under the old names, my reads cannot tell.
- The words `:asker`, `:not-signed-in` and `:session` of a query.
- `:subjects`.

**Run 4:**
- `:who` as "who may say the word" in `words.clj`.
- The words `:make-agent`, `:end-agent` and `:a-person`.

**Permission session:**
- What bounds "one run". In my reads it is whatever ends Auth's session, and the caller can keep one open without end.
- Whether `:invite`, `:under` and `:end-sign-in` are meant for a program signed in as her.

## 6. Pointers

### Pages that say Sid decided: not checked against the record, run 5
For each, the code in my reads that stands on it.
- `SPEC.md:9-13` (entries 2, 5, 7, 8, 12 and the go of 21 November): `sign_in.clj:243-294` (one setup command); `schema.clj:79-82`, `402-539` and `seam.clj:728-734` (an invitation is a person id); `sign_in.clj:75-107` and `schema.clj:798-874` (Auth first).
- `SPEC.md:15-21`: the calls of 22 November as a set, and questions 10, 17, 24 "stand".
- `SPEC.md:91-92` (a result comes from what stands, never from a memory of calls): `schema.clj:425-427`, `589-601`, `965-969`, `1021-1024`, `1095-1096`, `1203-1212`, `1259-1260`, `1298-1308`. Beside it for run 5 to hold against the line: `$$enter-ops` and the `ok:` rows keep one row for each refused enter and each answer, by the call's own id (`schema.clj:136-138`, `236-239`, `725-741`; `module.clj:479-495`).
- `SPEC.md:121-128` (what fits): `fit.clj:113-193`; `schema.clj:298-362`; `module.clj:170`, `326`, `440`, `529`.
- `SPEC.md:274-278`: `schema.clj:639-646`, `817`, `857-861`.
- `SPEC.md:288-292`: `schema.clj:590-601`, `629-637`, `842-855`.
- `SPEC.md:323-326`: `schema.clj:91-92`, `1020-1037`.
- `SPEC.md:332-334`, question 10: `schema.clj:997-1007`, `1034-1050`.
- `SPEC.md:362-363`, question 17: `schema.clj:1203-1212`; `seam.clj:293-330`.
- `SPEC.md:375-378`: `schema.clj:1298-1308`, `1412-1460`.
- `SPEC.md:385-388`, question 24: `schema.clj:1344-1375`. The runtime never names an agent that exists (`sign_in.clj:214`).
- `SPEC.md:389-391`: `schema.clj:1336-1342`.
- `SPEC.md:353-356` (12 October, entry 4, and entry 8): `schema.clj:36-51`, `73`, `94`, `109`; `fit.clj:137-141`, `167-168`; `seam.clj:176`, `313-330`, `715-722`. In that line "a session's default" means the build's author, inside a paragraph on Auth's sessions.
- `SPEC.md:481-483` (11 and 12 October, entry 4): `module.clj:7-12`; `schema.clj:270-275`.
- `seam.clj:39-40` (9 October, entry 1): `seam.clj:611-621`, `351`, `643-644`, `730`.
- `SPEC.md:136-138`: "The build's default of 23 November, for Sid to turn": `fit.clj:46-67`.

### Permission session
- `sign_in.clj:59`, `64`, `66`
- `sign_in.clj:153`
- `sign_in.clj:277-287` (`:see-and-act`)
- `seam.clj:397-400`
- `seam.clj:658-670`
- `seam.clj:683-684`
- `seam.clj:686-688`
- `sign_in.clj:205-207`
- `SPEC.md:266-267`, `340-341`, `351-355`, `489`
- By grep: `decision.clj:171-172`, `199`, `202`; `first_act.clj:57`; `acts.clj:120`; `act.clj:14`; `sources.clj:313`

### Run 2
- Version: `sign_in.clj:60`, `149`, `280`.
- Act-id: `seam.clj:45-48`, `552-558`; `sign_in.clj:100`, `156`, `271`, `288`.
- Facts: `sign_in.clj:65-66`, `155`, `286`.
- Time: `SPEC.md:69-73`, `107-110`, `397-427`; `seam.clj:220-225`; `schema.clj:172-176`; `fit.clj:30-34`.
- Digest: `seam.clj:31`, `568`, `684`.

### Run 3
- Auth's depots and stream topology: `module.clj:39-40`, `531-537`.
- `sign_in.clj:65` (`:softland.store/depot`).
- A query's words: `seam.clj:414-427`; `sign_in.clj:136`, `234-237`.
- An answer's words: `sign_in.clj:71`, `137`, `226-228`.
- `:subjects`: `sign_in.clj:67`, `154`, `285`.

### Run 4
- The words: `seam.clj:676-756`.
- Each `words/could-not` in `seam.clj` and `sign_in.clj`.
- Auth's `:could-not`: `SPEC.md:83-85`, `139-147`.
- `credential.clj`, whole.
- By grep: `words.clj:78`.

---

# Appendix: the brief this reviewer was given

You are doing run 1 of a five-run review of the Softland repository (/mnt/data/projects/Softland) with the skill `design-pitfalls`. Run 1 only. The other four runs and the merge are not yours and have not been started; do not do any part of them.

## Why this review exists

Sid, the founder, has to find out whether what earlier agent sessions built is right. The case that started it: the store's unit carried his word "layer" while meaning something narrower, and one id did several jobs; that was corrected to "lane". He now wants the same adversarial check on the path that answers "who is acting": from a Google account to the `:who` and `:session` on an act. The job is to break the thing, not to confirm it, and not to redesign it. If it holds, say that it holds and what you asked of it.

## Read these two first

- `.claude/skills/design-pitfalls/SKILL.md`, whole. Follow it: the inventory first, then the ten questions, the brake, and "How to answer".
- In the root `CLAUDE.md`: the paragraph "Defaults, not conflations" and the one after it ("The same holds between Sid and a session"). That is the rule being checked.

## What you review (the whole of it)

- `src-auth/`, all seven files (about 3,200 lines)
- `src-runtime/softland/runtime/seam.clj`, `sign_in.clj`, `standing.clj`: these three files only, not the rest of that directory
- `src-wire/softland/wire/credential.clj`
- `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`

Run 1 owns these: **person, root, agent, sign-in, session and run, `:who`**. For each one you list every job it does. Also take into the inventory any other id, key, word or record in these files that carries who is acting.

A note on one word: in the skill and the root page, "a session" usually means an agent session that wrote code or pages. `:session` in the code is the product's own thing. Keep the two apart in your report.

## Bounds. These are strict; Sid asked for them in so many words

1. **Read only.** Write or edit no file anywhere: no notes on disk, no memory, no docs. No git command that changes anything. Start no process: no REPL, no tests, no proofs.
2. **The record is not yours.** Do not use memtool, the softland-memory tools, or recall, at all, for any reason. The skill's question 9 says to open the line Sid typed; in this run you do not. Where a page in your reads says Sid decided or called something (for example "Sid's call of 22 November" in the SPEC), write "not checked against the record: run 5" beside it, and list which code in your reads stands on it. Write nothing as Sid's decision yourself.
3. **Never read** `src/app/server/env.clj` or any `.env` file.
4. **Stay inside the reads above.** One allowance, so that the jobs of an id you own can be listed: you may *grep* `src-store/`, `src-wire/`, `src-command/` and `src-runtime/` for where an id you own is read, and report each hit as "located by grep, not reviewed" with file:line and the matched line. Do not open those files to read them through, and raise no finding on code outside your reads. Where a finding would need such a file, write it under "still open" and name the run that reads that file. If a file outside your reads looks necessary to the path (for example another file in `src-runtime/softland/runtime/`), do not open it: name it in the list of what you did not read and say what it would have settled.
5. **Never open**: `src-server/`, `src-cli/`, `src-server-browser/`, `src/app/` (the old server, the client, the renderer), `src-inland/`, the rig, `history/`, the store's tables, the plan for pieces 5 to 8.
6. **Permissions belong to a separate session.** It owns: who may act, who may see, who may read a lane's acts, end, yes, and what a permission covers. Where you meet one of these, write one line: where you met it (file:line) and "permission session". Do not list its jobs and do not judge it.
7. **Ids of the other runs.** Where you meet one, write one line with file:line and the run number, and do not list its jobs:
   - run 2: version, act-id, fact, a fact with no value, stamp, time
   - run 3: the depot (streaming or microbatch), builds-on, marked keys, the words of a query and of an answer
   - run 4: the words on the wire, the "could not" answers, the commands. (`credential.clj` sits in a tree run 4 reads as a whole. You read it for how a credential becomes who is acting; the wire's words are run 4's.)
   - run 5: the pages that say "Sid decided"

## Models, on Sid's instruction

You are the reviewer and the judgment is yours alone; it is never handed down. Where you need collection (every place an id is written or read, with file:line and the quoted line), you may hand that to a read-only gatherer on a lower model and effort: the Agent tool with `subagent_type` `Explore` or `code-hunter`, `model: "sonnet"`, `effort: "low"` or `"medium"`. Its brief must carry bounds 1 to 5 above, since it loads neither this brief nor the root page. A gatherer returns facts with file:line and never a verdict. Read for yourself every passage a finding rests on, and quote only what you yourself read. If you have no Agent tool, gather with Grep and Read and say so.

## What to send back

Your final message is the whole deliverable, and it is all I will see, so make it complete. Plain words. Use Sid's word for a thing he has named (lane, act) and the standard term otherwise; add no vocabulary of your own. Give your view and its grounds. Name no fix beyond the split: the design is its author's and Sid's.

1. **What I read, and what I did not.** Each file, whole or in part. Any gatherer used, for what, on which model. Each grep outside the reads. Files you would have needed and did not open.
2. **The inventory.** For each owned id (person, root, agent, sign-in, session, run, `:who`) and each other id, key, word or record that carries who is acting: every job, each in one plain sentence, with file:line for where it is written and where it is read. For each job: decided when (at write, at read), whether it can change later, one or many, who changes it. Then: what is fixed for good (stored in an act, shown to someone, or written on a page as decided). A job you cannot say in one plain sentence is a finding by itself.
3. **What holds.** What you tested that came through, and which questions you asked of it.
4. **What breaks.** Each finding as the skill's "How to answer" has it: the thing, its jobs, how they differ (in when decided, in whether they change, or in how many), what it costs to separate now, what it costs later. Quote the line with file:line. Order them by what gets fixed for good soonest. Mark each one "certain from what I read" or "depends on something I did not read", and say what. Hold to the brake: "these could be separate" is not a finding.
5. **What is still open.** What the reads cannot settle, and whose it is: Sid's, run N's, or the permission session's.
6. **Pointers.** The one-line entries from bounds 2, 6 and 7.

Where you took a fork in how to read the task, say which way you went.
