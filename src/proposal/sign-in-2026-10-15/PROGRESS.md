# Sign-in build — progress and the plan the builders work from

Mid-flow state of the build that `BUILD.md` starts. Written by the build session
(Opus 5.5), 15 October 2026. Everything in "The plan" is the session's: a form,
a word or a rule that `BUILD.md` did not fix was chosen here, and each is listed
under "Choices to tell Sid". Nothing here is a decision of Sid's unless it cites
`BUILD.md`'s numbered decisions or his line.

## The process, sized

- New design with hidden failure modes: the store's four changes (they change
  what a gate accepts, and what a reader may see), the runtime's records, the
  first sign-in. One plan (this page), then builders. No plan check, no
  validation paper.
- Builders, in parallel, split by file ownership:
  - **A, the gate** — store changes a, b, d. Big: runs its own proofs.
  - **B, the read path** — store change c, and the read path's half of a.
    Runs its own pure proof; the cluster proof runs at the end.
  - **C, the runtime's pure part** — Part 1, in `src-runtime/`. Big: runs its
    own proof.
- Then, once A, B and C are whole: the run-again fixture is made again on
  purpose (the receipt says so), and every store proof runs once.
- **D, the runtime on a cluster** — Part 2, after Part 1. Runs its own proof.
  Beside it, **E** looks at the read-path check that failed 3 runs of 20.
- Part 3 when Sid gives the Google client id.
- One review at the end (falsification pass). Commits by concern, by the build
  session, staging only this build's paths. Builders do not commit.
- Cluster proofs run under the lock that the store's builds share:
  `flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock <command>`.

## The plan

### The store's changes

`gate/chain.clj` is written (by the build session): the one rule of whether a
right stands, for the gate and for the read path. A right stands when each of
its levels stands: the holder's place holds exactly that level, and each level
above the first was written at a lower stamp than the level beneath it. So an
end reaches down for good, also when the same right is given again above.

**a. A right can end.**
- The end is a retract under `:softland.store/permission` on the holder's
  place, in the layer. `store-key-fault` no longer refuses it.
- Who may end it: an act whose permission is one of the levels above the ended
  right (`chain/above?`): the one who gave it, anyone above in its chain, and
  the system where the system is the chain's top. A holder does not end her own
  right this way.
- Words: `:no-right-to-end` when the place holds no value (empty, or ended);
  `:end-not-from-above` when the act's permission is not above it.
- Standing reads each level above (`chain/level-places`, `chain/stands?`). The
  walk reads those rows in step 4. A right whose own row is there but a level
  above has ended earns `:permission-above-has-ended` (a new word, placed after
  `:permission-does-not-stand`).
- A give onto a place whose row is a retract is taken (that is how "see" is
  raised to "see and act": an end, then a new give). A give onto a place whose
  row holds a value is still `:store-key-takes-one-write`.
- The read path's half: who holds a permission in a members layer is read the
  same way. A retract row is no permission, and a right whose chain has ended
  does not let her see. The read path reads the rows of the levels above (more
  sources, found once her row is read), and keeps them live, so an end above is
  told to a screen that watches.

**b. A right lists the keys it allows.**
- The fifth slot of a given permission is `:see`, `:see-and-act`, or a set of key
  patterns, at most 64. A pattern is a key (exact), `:ns/*` (every key of that
  namespace), or `:*` (every plain key, never a store key). `:softland.store/*`
  is every store key. A permission with no fifth slot allows every key, as now.
- Each fact's key must be allowed by the act's permission:
  `:key-not-allowed`, the first check of `places`.
- A give allows no more than the giver's permission:
  `:permission-fact-allows-more-than-the-givers`, with check (g).
- Giving (`:softland.store/permission`) and changing a layer's setting are keys
  like any other. So `who-may-write-fault` stays nil and its docstring says
  that the key list now answers it.
- Pure helpers in `act.clj`: what a permission allows (`allows`), whether it
  allows a key, whether one allows no more than another. The runtime uses
  them too.

**c. "Anyone signed in" as a layer's setting.**
- A third word for who may see a layer: `:signed-in`. With `:members` and
  `:everyone`. The read path gives `:yes` to an asker who is signed in, and
  `:may-not-see` to one who is not. A row with a word it does not know still
  reads as `:members`.

**d. A key that a layer marks as taking one write per place.**
- A new store key on the layer: `:softland.store/one-write-keys`, a set of plain
  keys (bounded as the marked keys are; no key of the store's namespace),
  a setting, written again. It starts as `#{}`.
- The rule: under a key that the layer marks so, a fact with a value that names
  nothing to replace is refused `:place-takes-one-write` when its place holds a
  value. A fact that names the current fact it replaces is held to check (e) as
  any fact. A retract is taken as any retract. A place whose row is a retract
  holds no value, so it can be claimed again.
- The walk reads the layer's one-write row in step 5, for an act with a fact
  under a plain key, and then the place of each such fact. An act is decided
  with the marks as they were before it.
- This gives unique claims: two first claims of one place, one is taken and the
  other refused, in the layer's order.

Each change is a new version of the rules on a store that keeps nothing yet
(13 October, entry 6): the code knows one version; the fixture is made again on
purpose, and the receipt says so.

### The runtime's records

New tree `src-runtime/`, checks in `test-runtime/`. Two aliases in `deps.edn`:
`:runtime` (the store's source and the runtime's, Clojure, and
`com.nimbusds/nimbus-jose-jwt 10.10`) for the pure code and its proof;
`:runtime-module` (the same, and Rama 1.6.0) for the cluster. Nothing of the
rig, the old server or Inland.

**Ids.** The store's two known ids `:system` and `:base`. Persons, agents,
programs, the layers that sign-in makes, sessions, runs, invitations: random
UUIDs. An account's id is a name-based UUID (version 5) of its kind and its
number, under a fixed namespace UUID of the runtime's, so that one account is
always one place; the account's number itself is kept as a value beside it
(decision 3).

**The system's layers.** Streaming layers, made by the runtime at its start
with the system as their top, members only:

| Layer | Found by | What it holds |
|---|---|---|
| `:system/identities` | identity id | kind, current name, record layer, own layer, who made it, what it was copied from, by which invitation |
| `:system/ways-in` | account id | the person it points at; the account as given |
| `:system/names` | name | the identity a name points at |
| `:system/invitations` | invitation id | the hash of its code, the rights it gives, who made it, who used it |
| `:system/rules` | kind of actor, or sender | the limit per kind; the system's blocks; the wait before a freed name is taken again |
| `:system/record` | session id | the system's own record layer: each start of the runtime is a session |

Each identity has a record layer (default 18): made by the system, written only
by the system, `:see` given to the identity, and to the person who made an
agent or a program. It holds the identity's sessions with their token hashes,
its ways in with the account as Google gave it, an agent's runs, and the
identity's blocked attempts. A person's own layer is made by the system with
her first permission and no right of the system's.

**Keys** (plain keys, the runtime's; a session's words):

- `:system/identities`, on the identity: `:identity/kind` (`:person`, `:agent`,
  `:program`, `:system`), `:identity/name`, `:identity/record-layer`,
  `:identity/own-layer`, `:identity/made-by`, `:identity/copied-from`,
  `:identity/invited-by`. All but the name are one-write keys of the layer.
- `:system/ways-in`, on the account: `:way-in/person` (one-write),
  `:way-in/account` (`{:kind :google :sub "…"}`).
- `:system/names`, on the name's entity `(keyword "name" n)`: `:name/of`
  (one-write). A name is 1 to 32 characters: lowercase letters, digits, `-`
  and `_`, starting with a letter. Renaming is one act: the new name claimed
  and the old one retracted, naming the fact it replaces; then the identity's
  `:identity/name`.
- `:system/invitations`, on the invitation: `:invitation/code-hash`,
  `:invitation/gives` (a vector of `{:layer l :allows a}`, each given beneath
  the system's first permission in a layer whose top is the system),
  `:invitation/made-by`, `:invitation/used` (`{:person p :account a}`,
  one-write).
- `:system/rules`: on `:person`, `:agent`, `:program`, `:rule/acts-per-minute`;
  on a sender, `:rule/block` (`{:keys #{…} :layers #{…} :per-minute n}`, each
  part optional; no `:keys` and no `:per-minute` blocks everything in its
  layers, reads too); on `:names`, `:rule/name-wait-ms`; on `:blocked`,
  `:rule/keep-first`.
- A record layer, on a session: `:session/of`, `:session/kind` (`:sitting`,
  `:conversation`, `:program-run`), `:session/token-hash`, `:session/made-by`,
  `:session/ended`. On an account: `:way-in/account` (sub, and the email and
  name that Google gave). On a run: `:run/session`, `:run/description` (the
  fact id of the agent's description at the run), `:run/engine`
  (`{:provider :model :effort}`), `:run/context`, `:run/human-in-loop`,
  `:run/cause`. On the identity: `:blocked/count`; on an attempt:
  `:blocked/attempt`.
- An agent's description is a record its person writes in her own layer, on
  the agent: `:agent/description` (`{:instructions :tools :engine}`).

**Tokens.** `sl1.<identity uuid>.<session uuid>.<secret>`, the secret 32 random
bytes in base64url. The store keeps the SHA-256 hex of the whole token beside
its session, never the token. Checked by reading the identity's record layer
and comparing the hashes in constant time; ended by retracting the hash and
writing `:session/ended`. An invitation's code: `sli.<invitation uuid>.<secret>`,
kept the same way. A token says who; it carries no right.

**Google's ID token.** Checked with Nimbus: the signature against a key set (RS256,
the key found by `kid`); `aud` is the client id; `iss` is
`accounts.google.com` or `https://accounts.google.com`; `exp` has not passed, against
a time given to the check. Keyed on `sub`. The keys are fetched from
`https://www.googleapis.com/oauth2/v3/certs` at each sign-in and kept nowhere:
a sign-in is once a sitting.

**The binding.** An actor is `{:id :kind :session}` (and an agent's run). For an
act: a `:who` or `:session` that is not the actor's is refused (a client that
claims another's name); otherwise both are set: the sitting for a person, the
run for an agent, which must be one of its session's. An act whose permission's
holder is not the actor is refused. An agent's or a program's act that would
make a layer is refused. For a read: the asker is set to the actor, or
`:not-signed-in`; a query that names another asker is refused. Blocks and limits
are checked before the door; a blocked attempt is counted and the first few are
kept in full in the sender's record layer, and an act from someone without a
right still goes to the log (12 October, entry 7).

**The first sign-in, as steps that are safe to run again.** Each step reads
first and does only what is missing; the runtime waits for each answer.
0. An account with no link needs an invitation: its code checked, then
   `:invitation/used` claimed for a fresh person id and this account. An
   invitation used by this account goes on with its person; by another, refused.
1. The link: `:way-in/person` claimed. Taken by another person: use that one.
2. The identity: kind, record layer, own layer, invitation (claimed); a
   provisional name, random, claimed in `:system/names`.
3. Her own layer and her record layer made; `:see` given to her in the record
   layer; the way in recorded there.
4. What the invitation gives: each right beneath the system.
5. The session, with its token's hash.

### Part 2, on a cluster (builder D)

The runtime as a value that holds a door and a read path; its start makes the
base and the system's layers when missing; the sign-in loop; a second account
linked; names; agents made, described and copied; tokens made (pasted, or asked
for by the agent and approved in her browser) and ended; runs recorded; acts and
reads through the binding; rights given and ended by her acts; blocks, limits
and counts. HTTP on `127.0.0.1:8400` with the JDK's own server: the page with
Google's button, the sign-in POST (the `g_csrf_token` cookie must match the
body; `Referrer-Policy: no-referrer-when-downgrade`), an HttpOnly cookie for
the sitting, bearer tokens for agents and programs. Sid's command line reaches
the runtime by an admin route whose token is in a file only his user can read.
Proofs on an in-process cluster with a stand-in for Google.

## Choices to tell Sid

Collected as the build goes; each is a session's, and his to strike.

- The words: `:signed-in`; `:softland.store/one-write-keys`; the key patterns
  `:*` and `:ns/*`; the refusal words above; the system's layer ids; the
  runtime's keys.
- The end reaches down by stamps: a right given again above does not bring back
  what was beneath it.
- An account's id is a name-based UUID of its kind and number; the number is
  kept beside it as given.
- Google's email and name are kept beside the way in, in her record layer
  (the default that keeps more).
- A first sign-in gets a random provisional name.
- Six system layers, where the build page named two.
- Blocked attempts are kept in the sender's record layer.

## State

- 15 Oct: plan written; `gate/chain.clj` written and run by hand (a chain of
  three stands; an end above breaks it; the same right given again above does
  not revive it).
- C (Part 1) whole: `clojure -M:runtime -m softland.runtime.pure-proof`,
  `{:test 12, :pass 87, :fail 0, :error 0}`. Changed at the build session's
  word: a malformed act from a known actor goes to the door with her name on it,
  and the gate logs and refuses it (12 October, entry 7); only a thing that is
  no map is refused at the runtime.
- B (read path) whole: read-path pure proof `{:test 14, :pass 232}`, cluster
  read-path proof with A's gate in `{:test 29, :pass 578}`, 0 fail. A live
  read now holds one mark per level above her right. Known cost, kept: an asker
  whose chain had already ended is sent one whole answer that repeats what she
  was told, when a level above changes again. Removing it needs a value kept
  between changes (her last-told standing), which is the kind of keeping that
  CLAUDE.md says to stop at; the answer is never wrong, only repeated. For Sid.
- `chain/stands?`: a row with no whole-number stamp does not stand (it threw).
- D (runtime service) and F (HTTP, page, command line, main) writing against
  the store as it lands; their cluster proofs wait for the store to be whole.
- A (gate) whole: act 176, pure 759, module 467, door 277, 0 fail.
  Departures, accepted: the one-write row is read only for an act that holds a
  claim, after every other check; `store-namespace` moved to `act.clj` (a
  require cycle); an empty key set is a right whose every fact is refused
  `:key-not-allowed`.
- The store whole, from the final source, the fixture made again on purpose:
  act 176, pure 759, read-path-pure 232, module 467, door 277, read-path 578,
  run-again 46; 0 fail, 0 error (outputs in the session's scratchpad, `suite/`).
  The remade fixture decides as the old one did (same counts of rows in each
  table, same frequency of every refusal word in both gates): no act in it
  reached the new rules. A adds them to the scenario and makes it again.
- The fixture made again with a scenario `rights-and-claims!` that reaches each
  new rule in both gates (390 acts, 566 rows); run-again 46, 0 fail, no
  DIFFERENT. Committed: `12621884`, the store's four changes.
- E, the read-path check that failed 3 runs in 20: 39 traced runs at
  59b2a703 all passed; the cause was traced in the read path (a push the read
  came to need counts as known once it is given, before its own arrival step
  has run). Then forced at 12621884 by holding back one arrival: the check
  failed 3 of 3, passed 1 of 1 without the hold-back. In a forced run Erin is
  told the whole, right answer first, then one repeat of a place her screen
  already shows; the screen ends right in every run. Evidence: `runs/`
  (`read-path-hold-back-*.txt`, `.diff`, `read-path-39-runs-index.txt`).
  The question goes to Sid; nothing changes until he answers. The build
  session's view: the repeat is fine and the check changes, since "exactly one"
  cannot be held for a right that ends without a value kept between changes.
- D (runtime service) whole: service proof 30 pass, 0 fail; pure proof still 87.
  Choices for Sid: defaults of 600 acts a minute for a person and an agent,
  60,000 for a program, a freed name waits 30 days (from the time on its
  retract, for everyone), 8 blocked attempts kept in full; requests made on a
  sender's behalf (rename, an agent made, a token, a run) are held to her block
  and limit; the runtime reads an agent's description as its maker, for the
  run's version. D adds three checks: two first sign-ins of one account at the
  same moment, a freed name after its wait, a block that names keys.
- D's three checks pass: service proof 33, 0 fail. The race, 10 rounds: one
  person, one own layer, one record layer each time; the losing run was refused
  `:place-takes-one-write` at each claim and went on with the winner's write
  (rounds 4 and 8 mixed winners, still whole). For Sid, a store question: a
  second making act of a layer already made is refused with words that read
  wrongly in the log (`:who-not-the-permissions-holder` for her own layer,
  `:permission-fact-not-beneath-the-acts-own` for her record layer); nothing
  reads them, and a word "already made" would say what happened.
- F (HTTP, page, admin token, command line, main) whole: HTTP proof 18 pass,
  0 fail; a smoke of `main` and `command invite` worked. Choices for Sid: the
  cookie `softland-sitting`; an agent names its run in a `Softland-Run` header;
  the server answers only when Host is localhost or 127.0.0.1 at its port; an
  admin token `sla.…` in `~/.config/softland/runtime-admin-token`, 0600, new
  at each start. To fix after the review: `/me` reads as the system where the
  service's `own-identity` is the call. F's report that a first end gives
  `:ended-before? true` was of D's code before D's fix; the runs show false
  then true.
- The review at the end: two falsification passes in parallel, R1 on the
  store's commit, R2 on the runtime. Both were cut off when the session's
  process ended, and resumed from their transcripts; the scratchpad under /tmp
  was lost (the suite's raw outputs among it; their count lines are above and
  in `12621884`'s message).
- R1 (store): nothing lets a right stand that should not, or an end from below.
  To fix, after R2 (whose cluster runs load src-store):
  1. a bound on the whole text of a permission id, the act's own and any it
     gives, 8,192 bytes (a session's number; Sid's Z5 of 12 October put a bound
     on size in the place of the depth limit): a chain 32 deep with 64 long key
     patterns at each level made one standing check read about 16 MB;
  2. the read path: for a reader who is not signed in, a layer's setting in the
     meaning is only whether it is `:everyone` (a switch between `:members` and
     `:signed-in` sent her a repeat that told her the setting was written);
  3. wording: a one-write key stops a blind second claim, not a replace that
     names the current fact, nor a retract by any writer with the key; a claim
     holds once only where one writer, such as the system, writes the key;
  4. docs: a live read can show a fact written after her right ended until the
     end's own push is handled, as for a setting;
  5. attribution: "taken back" is the build page's (a session's), not Sid's
     (`memtool who`: never typed by him); fixed in `src-store/README.md`; to fix
     in `gate/chain.clj`, `gate/decision.clj`, `store/README.md`.
  For Sid: whether a replace under a one-write key is limited (say, to the
  row's own writer); and that ending a right does not block the person (another
  branch can give again at once), so a person's own block in her layer has no
  form yet.
- R2 (runtime): what held — tokens (one text, constant-time compare, only hashes
  kept, none printed), the binding, Google's check (alg none, HS confusion, an
  embedded jwk, wrong kid, iss and aud variants, exp at now, no sub), the Origin
  check, the bind address, no CORS, EDN only, escaping; nothing taken from the
  rig, the old server or Inland. Ran and found, fixed by the build session's
  rulings (defaults marked for Sid):
  1. blocks and limits dodged by making a program (blocked from `:note/*`, her
     program wrote `:note/text`): a person's block reaches what she made; each
     act counts toward its sender and each person above it in the made-by
     chain; a program's default limit is a person's; bytes per minute too, 64
     MiB by default;
  2. an agent could give rights and change settings by default (a right given
     with no key list is "see and act"): an agent's or program's store-key fact
     needs a right that names the store key; a right given to an agent with no
     list is `#{:*}` (BUILD default 8 is deny by default);
  3. a blocked person signed in again and again: block and limit apply at
     sign-in, and each sign-in counts;
  4. token-request slots filled by anyone: a new request replaces the oldest;
  5. approvals of unknown ids leaked memory: UUIDs, looked up first;
  6. the invitation code reached Google in the Referer: carried after `#`;
  7. cookies not scoped by port, a sitting that never ended: a second
     `softland-sitting` refused, the limit written down; a sitting ends after 7
     days, kept as a fact;
  8. no anti-framing header: `X-Frame-Options: DENY`, `frame-ancestors 'none'`;
  9. `:session-ended` told before the secret was checked: an end keeps the hash;
  10. 127.0.0.1 served pages whose forms were refused: redirected to localhost;
  11. a bad `%` escape gave 500: 400;
  12. a docstring promised a re-link by invitation that cannot happen: unlinking
      is not designed, Sid's command line re-links;
  13. `/me` to `own-identity`; the admin token file removed at stop and written
      through its own channel;
  14. an approval in the browser left no fact: the request is written at
      approval.
  For Sid: the runtime reads a person's own layer as her (sign-in, a run's
  description); once reads are kept, those reads would be recorded as hers.
- Fixes dispatched: A and B (store, small) first; D and F write theirs and run
  their cluster proofs only once the store is settled; then every proof, store
  and runtime, runs once.
- A's fixes: `:permission-within-the-limit`, word `:permission-too-long`,
  `max-permission-bytes` 8,192 (a standing check reads about 530 KB at most,
  where it could read 16 MB); one-write wording; attribution. act 176, pure 765.
  B's fixes: the meaning as a reader who is not signed in can tell it; the
  live-read doc. read-path pure 234. The read path's README line on "taken
  back" fixed by the build session.
- The store suite after the fixes: act 176, pure 765, read-path pure 234,
  door 277, run-again 46 (no DIFFERENT) passed; module 2 failed, one case in
  both gates, whose permission was over the new bound and so got
  `:permission-too-long` before `:act-too-large` (A rebuilds the case);
  read-path failed 1, the known check of Sid's question (the whole answer, then
  a repeat), and passed 578 in each of two runs after it.
- A rebuilt the module case (a permission within the bound, a who that carries
  the rows over the act's bound): module 469. Committed: `2af6e6d6`, the
  store after its review.
- D's fixes: service proof 43, pure proof 88. Blocks and limits follow the
  made-by chain (bounded at 8, stops at a person), acts and bytes a minute at
  each level; an agent's or program's store-key fact needs a right whose set
  names the key (`:store-key-not-allowed-for-an-agent`); a right given to an
  agent or program with no list is `#{:*}`; sign-in held to her block and
  limit; a new token request drops the oldest; approval looks the request up
  first and records `:session/asked`; a sitting ends after 7 days
  (`:session/ends-at`); an end keeps the hash. Defaults for Sid: 600 acts and
  64 MiB a minute for each kind (a program now a person's, was 60,000); a
  sitting 7 days; a rule on an id before its kind's.
- F's fixes: HTTP proof 23 (the code after `#`, frame headers, 127.0.0.1
  redirected, a bad escape 400, a second sitting cookie refused, `/me` by
  `own-identity`, the admin file written through its channel and removed at
  stop), and an admin route and command `limit` for `set-limits!`.
- The runtime's three proofs from the final source: pure 88, service 43, HTTP
  23. Committed: `7f35472c`, the runtime. `RECEIPT.md` written.
- Waiting: Sid's Google client id, for Part 3; his answers in the receipt's
  "For Sid".
- `src-store/README.md`: section "Decisions of 15 October 2026 — sign-in"
  added; the two bullets of 11 and 12 October that the changes made untrue now
  say what changed.
