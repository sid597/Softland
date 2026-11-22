**Verdict:** of the night's 28 changes to the runtime, 26 hold and open nothing that matters (one of them, the clock arithmetic, fixed no real fault and does no harm); two open something new: the pings, now one virtual thread each, can stop every virtual thread of a copy on this JDK 21 while a few readers are slow, and the yes page can still say "Done" for an answer of hers that did not stand, and no longer tells her, when she says no after a yes, that the program is signed in. Sign-in otherwise holds where it was pressed. Question 17: yes, a closed session can be kept in use through the seam's reading of `{:open S}`, but only when its closing never reaches the copy as a push, and then until the copy's next look, up to half a session's span (12 hours by default) and never past the secret's end; under Auth's contract the push comes, and the session is kept for one push and one read. With the two defects of its own checks repaired, the runtime's proof gives 49 passed, 0 failed, in four runs of four, and each repaired check fails against a runtime that breaks its sentence.

# Review: the night's changes to the runtime, and the repair of its checks

**22 November 2026.** Written by a Claude Code session (named "5") at the
prompt that Sid pasted to it: break the night's changes to the runtime, press
hardest on sign-in, settle question 17, and repair the two defects of the
runtime's own checks that [`spine/BUILD.md`](../spine/BUILD.md) names. No
source was changed for this page. The one file changed beside it is
`test-runtime/softland/runtime/checks.clj`, committed as `a1cb405e`.

## What was compared, and what was read

- **The diff.** The night's changes went into the first commit of the trees
  (`f2ff04ca`), so git does not show them. I made a detached worktree of
  `f2ff04ca` and diffed `src-runtime`, `src-command` and `test-runtime`
  against the copy taken before them,
  `/mnt/data/projects/research/softland/runtime-spine-2026-11-21/as-written-2026-11-22`,
  which I did not change: 249 added or removed lines in `src-runtime`, 179
  in `src-command`, 17 in `test-runtime`; `deps.edn` is the same in both. The
  three trees, the oracle shim, the wire and the store are the same at the
  head (`7cebbac2`) as at `f2ff04ca`, so what is said here holds for the head.
- **Read whole:** the diff; `seam.clj`, `http.clj`, `sign_in.clj`,
  `standing.clj`, `line.clj`, `jetty.clj`, and the command line's `main.clj`
  and `client.clj`; the wire's `credential.clj` and its words' statuses;
  `checks.clj`, `proof.clj` and `drive.clj`; the five pages the prompt named.
  **Read in part:** the oracle shim's `open-session`, `sweep-root`,
  `apply-write!` and `watch-root`; the reference's `write-open-session` and
  `take-out`; question 17 and the spec's `open-session` as committed in
  `7cebbac2`; the store's read path on a query's `:session`.
- **Facts of the platform that findings rest on:** the JDK here is OpenJDK
  21.0.13; Missionary is `b.46`, whose blocking `?` on an interrupt cancels
  the task, waits for it to end and sets the interrupt again (read from
  `missionary/impl/Fiber$1$1.class`), and whose `via` interrupts its thread
  on cancel; Jetty's WebSocket API is `11.0.18`, whose `RemoteEndpoint` has a
  `sendPing(ByteBuffer, WriteCallback)` that does not block.
- **Not done:** no source edit; no run against a real Auth, since none is
  built; the cluster proof and the command line's proof were not run; the
  pinning of section 2 was derived and not run.

The changes are numbered C1 to C28 here. `BUILD.md` counts them by fault (its
"11 HIGH and top MEDIUM"); the diff holds 28 separate changes, and each is
judged on its own.

## 1. Sign-in, pressed hardest

### Cookies

**C6. `http.clj:578–605`, `accept`: a `softland-sign-in` cookie that does not
parse, or comes twice, is answered 401; before, the line took the browser
as a visitor.** The fault was real but small: the seam's own rule is that "a
credential that stands for nobody is told so and is not made a visitor"
(`seam.clj:425–427`), and a broken cookie was the one way around it. The
change holds: the 401 comes before the `Origin` check, but a page of another
site cannot read why a socket failed, so it learns nothing. What it opens: a
browser that holds two such cookies, its own and one under another path or
set for a parent domain (which a sibling site under that domain can do), is
now kept off the line with 401 until the other cookie goes, where before it
was a visitor; and the pages treat the same browser as signed out
(`of-cookie`, `http.clj:236–248`), so signing in again replaces her own
cookie and not the other. Neither takes away anything she had; it is noted
so that the browser's client (piece 7) shows it.

Nothing else of the cookie changed: its attributes (`HttpOnly`,
`SameSite=Lax`, `Secure` over https, `Max-Age` the secret's time) and the
`Origin` held on every post and every cookie connection are as written.

### The yes to a program

**C3. `http.clj:440–442`, `GET /yes`: an ask whose `:ends` has passed shows
409.** Real: the form was shown for an ask that was over (Auth's `ask-of`
judges no time), and her post was then refused with `:over`. Holds.

**C4. `http.clj:474` and `:479`: an agent's name is asked for only with a
yes.** Real: a no with "an agent of its own" chosen and no name was refused
with "An agent needs a name". Holds.

**C5. `http.clj:480–491`, `POST /yes`: "Done" only when she said yes and
the yes that stands is under her root; otherwise 409.** Real: before, every
answer that came back `{:yes ...}` read "Done. The program is signed in",
also to a second person whose answer lost, and to a person who had said no.
The race between two persons now holds: the loser reads 409. Two cases are
left, and one is new:

1. **Left: a second answer that names another actor reads "Done".** The
   comparison is by root alone. A person who answered "it acts as me" and
   then answers "an agent of its own" (a second tab, a form posted again)
   reads "Done. The program is signed in" while what stands is the program
   acting as herself, with all of her standing, not the agent she just
   chose. The page should compare what she chose with what stands: as
   herself when the actor is her root, as an agent when it is not.
2. **New: a no after her own yes reads "This program was already answered
   before."** Before the night it read "Done. The program is signed in".
   Hers is the case that matters: a person who says no after a yes has most
   likely seen the page's warning too late, and she is the one who must
   learn that the program is signed in as her. No page lets her end a
   program's sign-in either (only `end-sign-in` over the line does), so the
   page is her one chance to learn it.

Both need her to answer one ask twice, so both are narrow. They are low, and
they sit at the moment the page's warning exists for.

### A secret that is replaced

**C13. `seam.clj:384`, `:660–673`: the caller holds its secret's hash in an
atom, and a replacing over the connection moves it.** Real: a second
`:replace-secret` on one long-held connection sent the first secret's hash
as `:old-hash` and was refused with `:wrong-secret`. Holds against the
contract as committed: a replacement is "judged by the present hash and the
one before it alone" (spec, `replace-secret`). It gives a connection no
power that a new connection with the new secret would not have: the atom
moves only on `{:replaced G}`, and the connection made the new secret
itself. What it leaves:

- After a replacing, `:who-am-i` still gives the `:until` of the secret the
  connection came with (`about`, `seam.clj:209–218`); the atom moves the
  hash and not the time. Nothing reads it today but a person.
- Older, made repeatable by C13: a browser's connection to the line may say
  `:replace-secret`, and its cookie keeps the old secret, which stands one
  more minute; the browser is then signed out. The runtime should refuse
  `:replace-secret` to a `:browser` sign-in until the pages replace a cookie
  ("the replacing of a browser's secret by the pages", under "Not built" in
  `src-runtime/softland/runtime/README.md`).
- Older: the command line loses its sign-in when a replacement is taken and
  its answer is lost. It makes a new secret for each try and keeps it only
  after `:replaced` (`command/main.clj:198–206`, `:368–377`); the old secret
  stands one more minute, and a later try with yet another secret is
  refused. The contract makes a retry with the same new secret safe ("the
  same replacement again"), so the fix is to keep the new secret before
  sending it, and to send that one again until an answer comes.

**C26. `command/main.clj:368–377`: `replace-secret` does no replacing of its
own first.** Real: with less than two days left, the command replaced twice,
and before C13 the second was refused. Holds.

### A session that is kept in use

**C20. `seam.clj:845–854`: the next look.** Real but small: the old time,
half of what was left less a second and never under a second, looked twice
for each moving on and moved a session on with a quarter of its span left;
for a span of a second or less it looked after the session's end. Now the
look comes 50 ms after half of the span is left, so a held session is moved
on once in each half span, as the spec sizes it (`open-session` "at most
once in half of a session's span"). When moving on failed, it looks again
within a second until the session's end: one read and one write a second for
each held connection while Auth refuses or fails the write; bounded by the
session's end. Holds.

**C18. `seam.clj:870`: a push's look runs on the seam's executor.** Real:
each listener's Auth read ran on the one drain thread of the root, so the
connections of one person at a copy were looked at one after another, each
waiting for the others' reads. Holds. It adds to section 2: each look keeps
its monitor across Auth's calls, and now the looks of one root run at once.

**C19. `seam.clj:872–877`: cancelling takes the look's lock.** Real: a look
in flight could set a new timer after the cancel had cancelled the old one;
that stray timer held the caller until it fired, then did nothing. Holds. A
close on Jetty's thread can now wait for one Auth call of a look in flight.

**C21. `standing.clj:148–150`: an opening of Auth's watch that gives no
function tells the root's listeners `not-reached`.** Real, and the most
serious of the night's faults: the root's entry stayed with no watch, so
every connection of that person at that copy ran unwatched for as long as
any stayed, caught only by its own timer (up to half a span apart). Holds:
the entry goes, each connection ends with `:reach-auth`, and the next
connection tries anew. What it costs: while Auth cannot open the watch (for
example at Rama's bound on live reads of one task), every connection of that
root ends as it opens.

### Question 17: can a closed session be kept in use, and for how long

`seam.clj` takes a repeat's `{:open S}` to mean that the session's time
moved on: `moved-on!` gives `later` when the result holds `:open`
(`seam.clj:364–368`). The spec as committed now writes question 17's reading
at `open-session`: the same id under the same sign-in, when the session is
closed or was taken out, "gives `{:open S}`, the result of the first, and the
session stays closed; only `who-is` says that it is closed"
(`spine/auth/SPEC.md:353–356`). The reference (`write-open-session`,
`test-auth/softland/auth/reference.clj:609`) and the oracle shim the proof
runs on (`open-session`) both do so. So the seam can believe that a closed
session runs another span.

**Where the belief is used.** `moved-on!` has two callers. `caller-now`
drops its result (`seam.clj:387–389`): whether a caller is made rests on the
`who-is` read just before it (`session-fault`), so a false "moved on" there
changes nothing. `standing`'s look uses it for one thing: when to look next
(`seam.clj:838–862`). After a "moved on" the next look is at half the span
and 50 ms: 12 hours and 50 ms with the default span of a day, 2.05 seconds at
the proof's copy `c`; never later than the secret's end, since the next look
is the nearer of the two (`seam.clj:847`). Between two looks nothing reads
Auth for that connection: a word is held against the caller as it was made
(`needs-a-who`, `seam.clj:433–442`), and the store takes an act in any
session that the caller names, since it knows nothing of Auth's sessions.

**What ends it sooner.** Every way that a session closes is an event under
its root that changes what is live there: `close-session`; `end-sign-in` and
`end-actor`, with their sessions; and the taking out of a session whose time
has passed, which every write under the root does at its `:at`, the repeat
itself included (spec, "What takes a thing out of what is live"). Auth's
watch pushes each such change to every copy that watches the root. The
pushed value then lacks the session, so `seems-gone?` holds
(`seam.clj:789–799`) and a look is started at once (`seam.clj:870`); it waits
for the look in flight, reads `who-is`, finds `:closed? true`, and ends the
connection with `:find-her-session`. The oracle pushes on every change of a
root's live view too (`apply-write!`), so the proof meets the same.

**So:** a closed session is kept in use that way only when its closing never
reaches the copy as a push while the watch is not reported lost (a lost
watch ends the connection with `:reach-auth`). That is outside Auth's
contract. Then the session is kept until the next look: up to half a span,
12 hours by default, never past the secret's end, and every act sent in it
meanwhile is taken. Under the contract it is kept for one push and one read,
the same window that any race between a close and a word has.

**What I would do.** The runtime's own rule is "a push is a signal, and a
read decides" (`src-runtime/softland/runtime/README.md:81`), and the spec
says a repeat's result "does not say how the thing stands now, which a read
says" (`SPEC.md:93–94`). A write's result should not set the next look
either. The cheapest repair is for `moved-on!` to give the time it held,
written or not: the next look then comes within a second
(`seam.clj:852–853`) and its read gives the session's true end, moved on or
closed. That costs one `who-is` more for each held session in each half
span, and makes the timer the backstop it is meant to be whatever the watch
does. It is a change of `seam.clj`, which this page does not make.

## 2. What the night opened: the pings can stop a copy (C8)

**C8. `jetty.clj:160`, `:168–181`: each ping runs on a virtual thread of its
own, under the connection's monitor.** The fault it fixed was real: the
pings ran on one thread, and one send stuck on a slow reader (`say!` holds
the connection's monitor while Jetty's blocking send waits,
`line.clj:98–102`) held that thread, so a quiet, healthy connection could go
60 seconds without a ping and be closed by Jetty's idle limit
(`jetty.clj:55–63`).

What it opens. On JDK 21, a virtual thread that waits to enter a monitor, or
blocks inside one, pins the platform thread that carries it, and the
scheduler adds no carrier for that (JEP 444; JDK 24 lifts it with JEP 491).
There are as many carriers as cores. A slow reader whose send buffer is full
pins one carrier with the send that waits (already so before the night), and
now one more for each ping that comes meanwhile: one every 20 seconds until
Jetty's 60-second limit closes the connection, so up to three. When the
pinned carriers reach the number of cores, every virtual thread of the copy
stops: Jetty's page requests (`jetty.clj:145–146` runs them on virtual
threads), the words of every line, every call of the seam to Auth and the
store, and the standing's looks, until the idle limits close the slow
connections. With 24 cores, as here, that takes six to eight slow readers at
once; on a machine of two cores, one. Before the night a slow reader pinned
one carrier, not up to four. This is derived from the code and the JDK's
rule; it was not run.

What I would do: send each ping with Jetty's `sendPing(ByteBuffer,
WriteCallback)`, which does not block, from the one ping thread and without
the connection's monitor, skipping a connection that is closed; Jetty queues
a control frame beside a message being sent. Apart from the pings, `say!`'s
monitor pins already, and so does `standing`'s look, which keeps its monitor
across Auth's calls (`seam.clj:834–863`) and since C18 runs one look per
connection of a root at once: a `ReentrantLock` in place of each does not
pin on JDK 21, or the runtime can ask for JDK 24.

## 3. The other changes, one by one

| # | Where | The change | Was the fault real | Does it hold, and what it opens |
|---|---|---|---|---|
| C1 | `google.clj:102–105` | One HTTP client for fetching Google's keys | Slight: a client and its thread for each sign-in, freed only when collected | Holds. Nothing |
| C2 | `http.clj:310–316` | What the provider said: keys cut to 256 characters; scalars taken, then 32 | Only for a token with a key over 256, which neither Google nor the stand-in sends; Auth's rule of what fits now needs it | Holds. Two keys that share 256 characters become one |
| C7 | `jetty.clj:81–89` | A connection is put in the ping's map before it is read again as open | Yes: a close between the read and the put left a closed connection pinged for ever | Holds: `end!` marks it closed before it takes it out |
| C9 | `line.clj:92–104` | A send that throws ends the connection | Yes: a dead socket kept its watches and words | Holds |
| C10 | `line.clj:106–125`, `:206–216` | Ending clears the queue and interrupts the connection's thread | Yes for the interrupt (a word in progress, such as a long series, ran to its end after the end); no for the queue (the thread read `:closed?` after each word) | Holds through Missionary's cancel. An act cut off may or may not be in; the maker sends it again under its UUID |
| C11 | `line.clj:141–157`, `:178` | A watch's place holds a token, so a late end cannot take a reused number | Yes, in a narrow race; its cost was a live read left open | Holds |
| C12 | `main.clj:191–193` | Setup's `--person` must be a UUID | Yes: a wrong id became a new person | Holds |
| C14 | `seam.clj:474–492` | A query that names a `:session` must name the caller's | Yes, and serious: the store's read path gives a query that names a run that run's permission beside the asker's own (`read_path.clj:1391`), so another session's run permission could be read | Holds, for `:read` and `:watch`. It also keeps a caller from naming another holder's run, the case of the store's finding 2A (`REVIEW-store-piece-2.md`) |
| C15 | `seam.clj:551–553` | A series stops between acts when its thread is interrupted | Yes, with C10 | Holds. It reports `:find-her-sign-in` whatever ended the connection; nobody receives it |
| C16 | `seam.clj:695–702` | `:under` leaves out sign-ins whose time has passed on this copy's clock | Yes: what Auth gives is as of the root's last event | Holds |
| C17 | `seam.clj:521`, `:528` | The waits' clock arithmetic does not throw on overflow | No: on Linux the clock counts from boot, and a wait is at most 60 s (`longest-wait-ms`) | Holds. No harm |
| C22 | `command/client.clj:69–77` | One HTTP client for the command line | Slight: a client for each poll while she answers | Holds |
| C23 | `command/client.clj:214–222`, `:294–301` | Sends and the close one at a time per connection | Yes: the JDK's socket throws on a send before the last one ended | Holds |
| C24 | `command/client.clj:224–279` | A word or a watch is put in against `:gone` in one step, and taken out when its send fails or its wait passes | Yes: a word put in after the line went waited its whole 90 s | Holds |
| C25 | `command/main.clj:105–128` | The file of the sign-in is made readable by its owner alone from its birth, and moved into place | Yes: under the usual umask the credential was readable by others between the write and the change of mode | Holds where files have POSIX permissions. Where they do not (Windows) the command can no longer keep a sign-in; a kill between the making and the move leaves a `.softland-*.tmp` beside the file |
| C27 | `command/main.clj:379–388` | `new-session` refuses with no sign-in, and keeps the old session when closing it failed but with 401 | Yes: with no sign-in it wrote `nil` into the file | Holds |
| C28 | `test-runtime/.../command_proof.clj:36–46` | `-Dsoftland.store.path` swaps the store's path for the command line's runs | A tool of the night's, so that another session's store was not loaded | Holds. It copies the `:command` alias, which can drift from `deps.edn` |

C3 to C6, C13, C18 to C21 and C26 are in section 1, C8 in section 2.

## 4. Found beside the changes

These are older than the night's changes.

- **A standing that fails leaves its connection unwatched.** The line starts
  the standing with two callbacks that do nothing (`line.clj:217–225`). The
  standing flow ends with a word for every end the seam knows, but a throw
  while it opens (the seam's executor shut, or Auth's client breaking its
  rule that no call throws) ends it with no word, and the connection stays
  open with nothing watching it. The failure callback should end the
  connection, as a lost watch does. Low.
- **A secret's replacing whose answer is lost**, and **a browser that
  replaces its secret over the line**: in section 1, under C13.
- **A third check that does not test its sentence.** "No live read and no
  watched root is left behind by connections that ended."
  (`checks.clj:276`) waits for no live read and never looks at the watched
  roots. Testing that half needs the checks to close the connections that
  they open, or to count only those that ended. `BUILD.md` lists it. I left
  it, since it is not one of the two defects named; it is open.
- The checks' namespace defines `run!` without excluding Clojure's, which
  prints a warning at each load. Cosmetic.

## 5. The checks: the two defects, repaired

Both are in `test-runtime/softland/runtime/checks.clj`, the one file changed.
No sentence changed: the 44 sentences of the file are the same, byte for
byte, before and after.

**Defect A: `first` was the first person's account.** `run!` took the
account handed in as `:first` under the name `first`, and three checks then
called `first` as Clojure's function: on the watches' messages
(`checks.clj:254`), on the two sign-ins at one moment (`:347`), on the two
own layers (`:355`). A map called with a vector gives nil, so the three
failed whatever the runtime did. The account is now bound as `first-person`
(`:126`, and the three uses of the account); the key that both proofs hand
`run!` stays `:first`.

**Defect B: the times of the replaced secret.** Copy `c`'s secret lasts 6
seconds, the secret before it 1, its session 4. The checks replaced the
secret at once after the program's yes, so the new secret ended almost when
the first did, and then slept 1.5, 5.5 and 5 seconds: by the second check
the sign-in had ended, and no runtime that keeps the spec could pass the
second or the third. Beside that, the new credential was let in at copy `a`
in the held session, which moved the session on by copy `a`'s span of a day
and left that line open, so the keeping of the session was never tested.

The repair (`checks.clj:357–411`, with two helpers at `:59` and `:77`):

- Every time is read from what the copy reports: the end of the secret that
  the held connection came with is in its hello, and the end of the new
  secret is in the replacing's result. The waits are to moments of the
  clock, not spans.
- The secret is replaced when half of the first one's time is left, so the
  new one outlasts it by 3 seconds. The second check then looks half a
  second after the first secret's end, and holds that the moment is after
  that end and before the new one's.
- Every line that names the held session is at copy `c` and, but the held
  connection, is closed at once, so what keeps the session past the time it
  was opened with is the held connection alone. The third check looks when
  a span and half a second have passed since the last moment at which
  anything else could have moved the session on.
- The first check now tests all of its sentence: a line with the old
  credential is let in right after the replacing ("stands for a moment",
  which it did not test before), and one is refused half a second after the
  old secret's last moment ("then for nobody"). Both go to copy `a` with no
  session, so neither can touch the held session.
- The third check also opens a new line in the held session (the session
  stands, read by the runtime itself), and after the 401 of the session
  that nobody held, a line with the new credential and no session, let in,
  so that the 401 cannot have come from the sign-in.

**Each repaired check can fail.** Three copies of the head, each with the
repaired checks and one change of `seam.clj` that makes one sentence false,
were run as the proof is run (scratch worktrees; nothing of them is kept):

| Copy of the seam | What it makes false | Result |
|---|---|---|
| M1: the secret before never stands after a replacing (`proves?`) | The first sentence | 48 passed, 1 failed: the first; its line with the old credential was refused 2 ms after the replacing |
| M2: a held connection is judged by the end of the secret it came with (`live-now`) | The second | 47 passed, 2 failed: the second and the third, both `{:could-not :find-her-sign-in :why :over}` |
| M3: a held session is never moved on (`moved-on!`) | The third | 47 passed, 2 failed: the second and the third, both `{:could-not :find-her-session :why :closed}` |

The second and third sentences both say that the held session stands, so a
runtime that breaks one breaks both. No other check failed under any of the
three. No check failed after the repair, so the repair turned up no finding
about the runtime.

## 6. What was run, what is open, and my choices

**What was run.** `softland.runtime.proof`, as `BUILD.md` section 2 ran it,
from the root of a detached worktree, behind
`flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`:

```text
clojure -Sdeps '{:aliases {:runtime {:replace-paths
  ["/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store"
   "src/proposal/runtime-2026-11-21/spine/runtime-oracle/src"
   "src-wire" "src-runtime" "src-command" "test-wire" "test-runtime"]}}}' \
  -M:runtime -m softland.runtime.proof
```

The command is the night session's own, from its log
(`~/.gemini/antigravity-cli/log/cli-20261122_020643.log`). `store-at-head`
is the same as `src-store` at the head. Output, with the counts that the
proof prints last:

| Worktree | `checks.clj` | Result | Exit | Time |
|---|---|---|---|---|
| Head `7cebbac2` | repaired | 49 passed, 0 failed | 0 | 11:23:08 to 11:23:40 |
| The same | repaired | 49 passed, 0 failed | 0 | to 11:25:45 |
| The same | repaired | 49 passed, 0 failed | 0 | to 11:26:15 |
| The same | repaired | 49 passed, 0 failed | 0 | to 11:26:45 |
| `f2ff04ca` | as written | 44 passed, 5 failed | 1 | 11:23:40 to 11:24:16 |
| M1, M2, M3 | repaired | as in section 5 | 1 | to 11:28:18 |

The five failures as written are the five that `BUILD.md` names, with the
same seen values: the watches' first message, the two sign-ins at one
moment, the one own layer (defect A); the held connection, ended with
`{:why :over, :could-not :find-her-sign-in}`, and the held session (defect
B). Each run also prints two warnings: a path outside the project, from the
store's snapshot, and Clojure's `run!` replaced in the checks' namespace.

**What is open.**

1. The pings (section 2): an async ping on the one thread, and a lock that
   does not pin in place of the connection's monitor and the look's, or JDK
   24. A change of `jetty.clj`, `line.clj` and `seam.clj`.
2. The yes page's two cases (C5); and no page ends a program's sign-in.
3. Question 17: `moved-on!` should give the time it held and let the next
   look read (section 1).
4. Older: the standing that fails silently; the lost answer of a replacing;
   a browser's replacing over the line; the check of watched roots.
5. Not run: the cluster proof and the command line's proof; nothing against
   a real Auth. Another session is moving the oracle shim onto Auth's
   reference; when that lands, the proof runs on the reference, and these
   runs were on the shim at the head.
6. `BUILD.md`'s list of findings that the night did not fix was not checked
   again here.

**My choices, each yours to turn.**

1. I read "each check tests its own sentence" as asking the first check to
   test "the secret before it stands for a moment", which it did not; it
   now does. It asks for a line within a second of the replacing; the runs
   made it in 2 ms.
2. The times come from what the copy reports, with margins of half a
   second, and the waits are to moments of the clock. That assumes the
   proof and the copies share one clock: true for both proofs today (one
   JVM, or processes on one machine).
3. Lines in the held session go to copy `c` and are closed at once; the old
   credential's two lines go to copy `a` with no session.
4. The third check's two added lines: a new line in the held session, and a
   line with the new credential after the 401, as its control.
5. `first-person` as the name of the first person's account in `run!`.
6. The third check that does not test its sentence (watched roots) is left
   as it was.
7. The numbering of the changes, the judgment of each, and the weights:
   high for C14 and C21 among the faults, and for C8 among what the night
   opened; low for the yes page's two cases and for question 17 under the
   contract.
8. The mutants were run in scratch and not kept: their changes are the three
   one-line edits named in section 5.
9. Two commits: `checks.clj` alone under the subject that the prompt gave
   (`a1cb405e`), and this page in a commit of its own.
