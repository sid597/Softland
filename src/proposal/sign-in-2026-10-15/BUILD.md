# Sign-in: the build

The starter of the build session. Written on 15 October 2026 by the session that
explored sign-in with Sid (Opus 5.5, max effort), at his word ("yeah", to that
day's write-set). Nothing here is built yet. What is Sid's is marked with his
words and the id of his line in the record. Everything else is a session's: each
default below was told to him that day, and he could strike any of it. He struck
none.

## Read first

- The root `CLAUDE.md`, then `src-store/CLAUDE.md` and the decisions of 6 to 14
  October in `src-store/README.md`. Sign-in rests on these entries:
  - 8 October, entries 1 to 3: the system is at the top of the base; a
    signed-in person has her own layer and a right in it without asking; a
    right ties back to one person.
  - 9 October, entries 1 and 3: a person is a random id, and her sign-in
    accounts are joined to it; the system can hold many layers.
  - 11 and 12 October, entries 1, 4 and 7: who may see a layer; only programs
    on Sid's machines reach Rama's ports; who wrote a fact is within reach.
  - 12 October, entries 2, 4 and 7: nothing in the log is cut; whose reads are
    kept; an act from someone without a right is logged, then refused.
  - 13 October, entries 2 and 6: order is local; a change in what a gate
    accepts is a new version of the rules.
  - 14 October, entry 5: time travel is designed later.
- The Rama skill, before any Rama code.
- `../first-maker-2026-10-14/decision-4-who-acts.md` and its
  `checks/review-d4-report.md`: who acts, why the base reads as everyone, and
  why one importer id serves one person.
- The rig, the old server and Inland are examples. No form, no name, no call
  shape and no default comes from their code. One finding of Inland's: it kept
  passphrase hashes in a file at its host, and gave the browser an HttpOnly
  cookie.

## What sign-in is here

Sign-in is the way into the runtime for every actor: a person at a browser, an
agent, a program, the system. It makes the names on acts and reads true.

Today those names are claims. The door "takes `:who`, `:session` and
`:permission` as the maker gives them", and leaves room for sign-in that stands
"in front of the door … and changes none of it" (`door.clj`). The read path asks
as "what the caller says" (`read_path.clj`). Nothing outside `src-store/` and
`test-store/` uses the door or the read path yet, so the runtime does not exist.
This build makes its first part.

## Sid's decisions of 15 October 2026

1. **Sign-in now.** Sign-in is built now, "so we are not faking things and
   actually building" (‹f0adce47›, a long prompt of his that no session wrote).
   "we need to start work on sign-in now" (‹f1ceedce›).
2. **A person is the store's random id.** Her ways to sign in, and her name,
   point at it: "we treat the initial sign-in as one way of pointing a user to
   their identity … there can be multiple google logins multiplu other ways to
   login … we do need to have username for shorthands and also because people
   like that for anonymity reasons as well" (‹e5a2200e›; the record holds it as
   a long prompt).
3. **The log keeps Google's account number as given.** This is the `sub`,
   kept beside the person, not a scrambled stand-in: "1. A" (‹5325b353›). It
   becomes permanent only on the day real data is kept.
4. **Google's sign-in button, with no client secret.** "yes" (‹bb59807f›).
5. **Each agent is its own identity, someone's and independent.** "lets treat
   agents both as "someone's" and also an independent identity that anyone
   else other than a creator can also refer to use etc. … best is to have each
   agent their own identity … An agent has its sessions … lets treat them in
   the same way we treat intelliglible things" (‹e5a2200e›).
6. **A run, and a name.**
   - A run is "context + "llm provider with settings (model, effort etc.)" …
     what differentiates is the context" (‹e5a2200e›).
   - A name is a changeable shorthand: "Its a setting and yes it can change
     overtime same name refers to different ones its like a shorthand but when
     we need to resolve it we can get the data based on the version and time we
     are looking at right? I think so" (‹564310b8›).
   - A session's reading, told to him: acts keep the id, so they never need
     resolving. What a name meant at a given time can be answered from the kept
     facts. The read that asks "as of then" is the time-travel work of 14
     October, entry 5.
7. **Rights can be taken back, and blocked down to kinds of act.** "for
   malicious users we should be able to block as the system and then the user
   when giving permissions to different agents, tools, layers, gropus etc.
   should be able to do the same … they should also be able to block "type" of
   actions … we distinguish you from your intent and we are blocking your
   intent (since we have fine grained permissions)" (‹e5a2200e›).
8. **Who sees.**
   - "By default we make the base readable to all to contribute to it will have
     a diffrent layer"; "I (sid) has the "full" access to softland base"; "why
     are we not saying "signed in" "not signed in" … they all are then under
     these 2 blankets right??" (‹e5a2200e›).
   - Asked whether "all" takes in people who are not signed in: "A. Yes"
     (‹564310b8›).

## The defaults he did not strike

Each is a session's, told to him on 15 October.

**Who is who**

1. **Ids.**
   - Every person, agent and group is an id that says nothing. The system and
     the base keep their two known ids.
   - Names, ways to sign in and tokens point at ids.
   - The ids that sign-in makes are random: persons, layers, sessions, runs.
2. **Names.**
   - Every identity has one. A name is unique at any one time, and it can
     change.
   - Text stores the id and shows the name.
   - A freed name waits a while before another identity can take it. How long
     is a setting.
3. **Ways in.**
   - Google now. Other kinds come later, each a new check.
   - A person can have many.
   - A second account is linked only while she is signed in.
   - An account already linked to someone else is refused.
   - Two persons made by accident are not handled yet.
   - A person who loses every account is linked again by Sid from the command
     line.
4. **Sign-up by invitation.**
   - The first invitation is made by a command on Sid's machine. It gives him
     full rights in the base, beneath the system.
   - Open sign-up waits for three things: a limit on each sender, a way to
     block one, and an answer on forgetting a person.
   - His own word for it, in May: "I am inviting users into my world which is
     softland" (‹6854ef38›).

**Proof**

5. **How each actor proves who it is.**
   - People: Google's ID token, checked against Google's published keys.
   - Agents and programs: a random token, which a person makes for them. She
     pastes it, or approves it in her browser.
   - The store keeps a scrambled copy (a hash) of each token beside its
     session, never the token itself.
   - A token says who its holder is. It carries no rights.
6. **No secret goes into the store.** With decisions 3 and 4, and tokens kept
   as hashes, sign-in needs no secret at all today. Agents' model keys stay
   where the agent runs.

**Agents, sessions, runs**

7. **What an agent is.**
   - A lasting id with a changeable name.
   - A principal, recorded as a fact with history.
   - Its rights, and its memory in the land.
   - Its description (instructions, tools, the engine it prefers) is a record.
   - A copy of an agent is a new agent, with a link to the one it was made
     from.
8. **What an agent may do.**
   - An agent is never at the top of a chain of rights: every chain ends at a
     person or the system.
   - An agent's act stays its own until its person adopts it with an act of
     her own.
   - An agent acts, but gives no rights and changes no setting of a layer,
     unless its person allows it.
9. **Sessions and runs.**
   - A person's session is one sitting.
   - An agent's session is one conversation, and belongs to the agent.
   - A run is one model call in a session. It records:
     - the version of the agent's description;
     - the provider, the model and the effort;
     - its context: what it was given, and its reads, which are kept (12
       October, entry 4);
     - whether a human was in the loop;
     - what caused it.
10. **What an act's `:session` names.**
    - The occurrence that made the act: the sitting for a person, the run for
      an agent.
    - The store reads `:session` only as an id (`door/declaration.clj`,
      `ids-of`). So this is a meaning, not a change to the act's form.
    - No session is the top of a chain. Ending one is the runtime's job, and
      no gate reads a session.

**Rights and blocks**

11. **Ending a right.**
    - A right can be ended by the one who gave it, by anyone above it in its
      chain, or by the system.
    - Ending one ends what was given beneath it.
    - Raising "see" to "see and act" is an end followed by a new give.
12. **Which keys a right allows.**
    - The kind of an act is the keys it writes; "see" is reading.
    - Giving a right and changing a layer's setting are keys of the store like
      any other.
    - A give allows no more than the giver holds.
    - A block is the same list as a no: not these keys, not in these layers,
      not more than this many a minute.
13. **Where each rule is enforced.** The gate knows a layer; the runtime knows
    who is what.
    - Rights, ends and allowed keys live in a layer, and its gate enforces
      them.
    - The kind of an actor, sign-up, the system's blocks, the limits and "no
      agent at the top" all need facts from other layers. So the runtime
      enforces them, and the record keeps each one as a fact.
14. **Blocked attempts.**
    - A blocked attempt stops at the runtime. It is counted, and the first few
      are kept in full.
    - This narrows his decision of 12 October (entry 7, "why don't we store
      both?") for blocks alone. An act from someone without a right still goes
      to the log and is refused, as decided then.
15. **Limits.** A limit on each sender, by kind of actor, kept as a fact and
    enforced at the runtime.

**Seeing and writing**

16. **Who sees a layer.**
    - A layer's setting is one of: everyone, anyone signed in, or only those
      given a right.
    - The base is set to everyone (decision 8).
    - Contributions go in each person's own layer, above the base. Sid, or the
      few he chooses, accept them into the base.
17. **Who acts as the system.**
    - Only the runtime's own steps act as the system: making the base, and
      making a person and her layers at her first sign-in.
    - Sid, his agents and the importer act beneath the system in the base,
      under their own names.
    - Every maker, the importer too, goes through the runtime. Only the
      runtime opens Rama's ports; the proofs and Sid's own command line are the
      exceptions.
18. **Where the system's records go.**
    - What the system records about an identity (its sessions, tokens and ways
      in) goes in a small layer for that identity. Only the system writes it,
      and the identity can read it.
    - A person's own layer stays hers alone: the system makes it and keeps no
      right in it.
    - The links from accounts to persons are in one layer of the system's,
      found by account. Names are in another, found by name.

**Where**

19. **Where it runs.**
    - A store that is thrown away, on localhost only.
    - The new code goes in its own tree, `src-runtime/`, with its own aliases.
    - It requires the store's door and read path, and nothing of the rig, the
      old server or Inland.

## The first sign-in

The store gives no order between two layers (13 October, entry 2). So the
runtime waits for each answer before it sends the next act. It makes the steps
in this order:

1. the link from the account to a new person id;
2. her own layer and her record layer (the system's making acts);
3. the session.

Every later sign-in finishes whatever is missing, so a crash halfway never
makes a second person.

## The build

Three parts. Each ends with a proof that passes, so the work is whole wherever
it stops.

**Part 1 — who is acting.** Pure code, no cluster.
- The shapes of the records, as declared data: a person, a link from an
  account, a name, an agent, a session, a run, a token, an invitation, a block,
  a limit.
- Tokens: made at random, kept as a hash, checked, ended.
- The binding:
  - from who is acting, set `:who` and `:session`;
  - refuse before the door an act whose right is not that actor's;
  - refuse an agent's act that would make a layer.
- The first-sign-in steps as data, turned into acts, safe to run again.
- The check of Google's ID token:
  - the signature, against Google's published keys;
  - `aud` is the client ID;
  - `iss` is `accounts.google.com` or `https://accounts.google.com`;
  - `exp` has not passed.

  Key on `sub`: "Only use the Google ID token's `sub` field as the unique
  identifier for the user" (Google's page, read 15 October). Use a JWT
  library, as Google advises, not a hand-written check.

**The store's changes**, beside Part 1. Each changes what a gate accepts, so
each is a new version of the rules (13 October, entry 6), on a store that keeps
nothing yet. The fixture is made again on purpose, and the receipt says so. The
proof that runs the gates again must still pass. Each change carries its
docstrings and the store's page (`src-store/CLAUDE.md`).
- **a. A right can end.** The end is a later fact at the right's place. The
  check of a right then reads each level above it. Today it reads only one,
  because nothing ends (`gate/decision.clj`, namespace docstring, check (d)).
- **b. A right lists the keys it allows.** The list is checked for each fact,
  and a give allows no more than the giver's.
- **c. "Anyone signed in" as a layer's setting.** Today the read path reads any
  value it does not know as "members" (`read_path/layers.clj`).
- **d. A key that a layer marks as taking one write per place,** for unique
  names. Today a fact that names nothing to replace writes over whatever is
  there (`gate/decision.clj`, `places-needed`).

**Part 2 — the runtime on a cluster in one process,** with the store's module.
The proofs show:
- A stand-in for Google: a key pair of our own that signs tokens the way Google
  does, so the proofs need no network.
- A first sign-in and a later one; a second account linked; an invitation, and
  a stranger without one refused.
- An agent's token made, used and ended, with its runs recorded; a client that
  claims another's name refused.
- A read by someone who is not signed in sees the layers set to everyone, the
  base among them.
- A right ended; a key that is not allowed refused; a blocked sender stopped at
  the runtime and counted.
- A name claimed and changed; a second claim of a taken name refused.

Also in Part 2: the read-path check that failed in 3 of 20 runs
(`../first-maker-2026-10-14/ground-check.md`, section 2) is about a right given
while a screen watches, and sign-in gives rights all the time. Look at which
message came first in a failing run, then bring Sid its one question.

**Part 3 — Google for real,** on `http://localhost:8400`.
1. Sid makes the client (below) and gives its id.
2. He signs in with Google in a browser.
3. An agent with its token writes a note into his layer and reads it back,
   named as the agent, in its run, beneath Sid.
4. He ends the token, and the agent's next act is refused.

That is the action sign-in gives, and the evidence that it works.

### The Google client, for Sid

Checked on 15 October 2026 against Google's pages "Get your Google API client
ID" and "Verify the Google ID token on your server side".

1. In console.cloud.google.com, make a project.
2. In Google Auth Platform, open Branding and give an app name and your email.
   If it asks for an audience, choose External (that step is not on Google's
   page).
3. Open Clients, choose Create client, then Web application.
4. Under Authorized JavaScript origins, add both `http://localhost` and
   `http://localhost:8400`. Add no redirect URI. There is no client secret.
5. Give the client ID to the build session. It is not secret.

For the builder, from the same pages:
- Over http on localhost, the page sets `Referrer-Policy:
  no-referrer-when-downgrade`.
- When the credential arrives by a POST to the runtime, the `g_csrf_token`
  cookie must match the one in the body.

## How the build runs

- Part 1 and the store's changes run in parallel builders. Part 2 comes after
  Part 1, and Part 3 when the client id is in.
- Each builder runs its own proof; one review at the end.
- Commits by concern on `main`, staging only this build's paths. Read `git log`
  before each commit: other sessions commit in the same tree.
- Nothing is kept: the store is thrown away.
- Mid-flow state goes in `PROGRESS.md` beside this page.
- Never read `env.clj`. No Co-Authored-By line, and no Claude-Session link.

## Later, and not in this build

- Forgetting a person, which comes before anyone else can sign up.
- Whether the world, his "open group", is a layer of its own.
- A right given to a group, so that its members get it, or to a tool; a
  layer's "everyone signed in, except her".
- Open questions about agents:
  - who may change, run, ask or copy an agent;
  - whether an agent keeps a layer for its memory;
  - who answers when an agent that someone else asked for is wrong;
  - whether an agent can own things;
  - whether a group takes acts only from certain models.
- Acts signed by their makers: a later version of the act. His answer of
  September left that door open (‹1088472f›).
- Other ways to sign in: GitHub, passkeys.
- Keeping a model's reads (12 October, entry 4). Sign-in records the kind of
  actor; the keeping is a build of its own.
- The importer's road: decisions 1 to 3 of `../first-maker-2026-10-14/`.

## Where this came from

- The chat of 15 October 2026, in the session that wrote this page.
- The folder of the night before, `../first-maker-2026-10-14/`.
- A Fable agent at max effort, which thought through agents and runs on its
  own. It started from his two lines, without the session's answer, and reached
  the same place: the agent is the lasting identity, and the model is a fact of
  each run. Its framing is a session's, and is in the defaults above:
  - five parts: description, agent, session, run, act;
  - an agent's act stays its own until its person adopts it;
  - whether a human was in the loop;
  - no agent at the top of a chain.

The rows of the queue that this build takes up
(`../backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md`): E3, E5, E6, E7,
Q16, Q19, K7 to K10, K14, V9 to V13, V25.
