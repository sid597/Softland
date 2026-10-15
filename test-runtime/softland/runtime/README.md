# Runtime checks — who is acting

[Parent: namespace root](../README.md) · [Runtime verification](../../README.md) ·
[Runtime source](../../../src-runtime/softland/runtime/README.md).

## pure_proof.clj

What it checks: the runtime's pure part, each namespace of
`src-runtime/softland/runtime/`. The ids and an account's id; tokens and
invitation codes; the check of Google's ID token, with a key pair the proof
makes and tokens it signs the way Google does (a good one, and one wrong in each
way the check refuses); the records and their one-write keys; the binding of
acts and queries, blocks and limits; the runtime's start; and sign-in: a first
one, the same run started again from the state after each of its acts, two runs
of one account at once, a later sign-in, a second account linked, a taken
provisional name, reads of the base that lag, and each refusal of the plan.

How to run, from the repository root:

```sh
clojure -M:runtime -m softland.runtime.pure-proof
```

What a pass shows: each check's sentence after `PASSED`, the `SHOWN` lines of
what it saw, and the count line `{:test … :pass … :fail 0 :error 0}`; the
process exits 0. A failed check prints `FAILED` and its sentence, and the
process exits 1.

State it changes outside the code it checks: none. It writes no file, opens no
port and keeps nothing after the process.

What it does not reach:

- The real gates and the real read path. The steps run on
  [store_model.clj](store_model.clj), a model that holds each act to the act's
  one form and each query to the query's, and keeps the rules the steps lean
  on. Whether the store's gates accept these acts, mark the one-write keys, and
  answer reads as the model does is a proof on a cluster: the service proof
  below drives the start, sign-in and the runtime's other steps through the
  door and the read path, and cuts a first sign-in after each of its acts; the
  HTTP proof drives them over HTTP.
- A refusal word of the real gate where the model gives another: the steps do
  not read the words. The service proof shows the words of the refusals it
  drives on a cluster, not every word the model gives.
- Two runs at once that lag in the base while both give the same right (the
  service proof races two first sign-ins on the real base, and has not met
  that lag); a crash between an act's send and its answer that a door would
  report as `:nothing-yet`.
- Google's own keys and tokens, and the fetch of the keys.
- The runtime's other steps (`system_steps.clj`: a name, an agent, a token's
  session, a run, a blocked attempt) and the rules' defaults of the start: no
  pure check drives them; the service proof drives them on a cluster.

## service_proof.clj

What it checks: the runtime as a service (`softland.runtime.service`) on Rama's
in-process cluster with the store's module `Store`, each call through the
store's real door, gates and read path, with a stand-in for Google: a key pair
of the proof's own, with a kid, that signs ID tokens as Google does, and whose
public key set the service is given as `:google-keys`. In order: the start, and
the start again with nothing missing, with the rules' defaults; an invitation,
and a stranger without one refused; a first sign-in and a later one, and a
token with another secret, or of no form, refused; the invitation's right in
the base; a second account linked and signing in as the same person, refused
for another, and an account linked by Sid's command line; a client that claims
another's name, sitting, right or asker refused at the runtime; an act from
someone without a right logged and refused by the gate; a name claimed and
changed, a taken name refused, a freed name refused to another at once and
taken by her once its wait has passed (the proof opens with a wait of two
seconds); a read by someone not signed in, once and kept live; a layer set to anyone signed in; an agent made
with its description, its token made, its run recorded, its act in its
person's layer accepted in its run and read back named as the agent beneath
her right; a key that the agent's right does not allow refused by the gate,
and an agent refused at the runtime when it would make a layer or change a
name; a right ended by its giver and the agent's next act refused by the gate;
the agent's token ended, its next use refused at the runtime `:session-ended`,
and the same token with a wrong secret refused `:token-not-valid`; a token
asked for, approved by its person and collected once, with the request's id,
code and time recorded beside the session; nine requests for one agent, the
ninth dropping the oldest, and approvals of an id that is not held refused;
an agent's give and setting refused at the runtime under a right with no key
list (`:see-and-act`), its give taken under a list that names
`:softland.store/permission` and its setting still refused, and a right given
to an agent with `:allows` left out allowing `#{:*}`; a copy of an agent; a
blocked sender stopped at the runtime (its acts, a read, a rename, a sign-in),
counted in its record layer at 1, 2, 4, 8 with the first two attempts kept
whole, then unblocked; a block of `:tool/*` in one layer, which stops an act
under that key there and counts it, and lets `:note/text` there and `:tool/x`
in another layer through; a person's block of `:note/*` stopping the program
she made, its acts counting toward her limit so that it is stopped
`:over-limit` while its own count is far from its own, and a limit of bytes
a minute stopping an act that would pass it; a program held to a limit on
its own id; a sitting refused `:session-ended` once the service's clock is a
week and a second past its sign-in; a sign-out; a first
sign-in cut after each of its acts, one to nine, finished by the next sign-in
with the same person and no second one; and two first sign-ins of one account
at the same moment, on two threads released together by a latch, each with
its own fresh ids, ten times with a new account and invitation each time: one
person, one own layer, one record layer, the invitation used by that person,
and both runs ending with her. For the race the run shows each run's refused
acts with the gate's words, so that a pass shows that the two runs met.

How to run, from the repository root, under the lock that the store's cluster
proofs share:

```sh
flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:runtime-module -m softland.runtime.service-proof
```

What a pass shows: each check's sentence after `PASSED`, the `SHOWN` lines of
what each call gave (no token's secret is printed), and the count line
`{:test … :pass … :fail 0 :error 0}`; the process exits 0. A failed check
prints `FAILED` and its sentence, and the process exits 1.

State it changes outside the code it checks: one in-process cluster of Rama's,
with the store's module, closed at the end (its files are Rama's temporary
ones). It opens no port, writes no file and calls no network. It holds the
cluster lock while it runs.

What it does not reach:

- Google itself, its keys and their fetch: Part 3, with Sid's client id.
- More than two sign-ins of one account at once, and two at once whose read of
  the base lags behind the other's accepted right: in the runs so far the read
  after the refused right already showed it, so the `:step-refused` that such
  a lag gives (`softland.runtime.steps`) has not been seen on a cluster.
- A crash between an act's send and its answer (the proof cuts a run before a
  send, never after it); a cut in the runtime's other steps (a name, an
  agent, a token, a run): only a first sign-in is cut.
- A block of layers alone, a block's rate, and the window of a limit passing
  after a minute; two acts of one chain at once at the limit's last place;
  one sender's blocked attempts at once; a chain of makers longer than an
  agent or a program and its person; a sign-in stopped `:over-limit`.
- A blocked attempt too large to keep whole, kept as its digest and size.
- The token requests lapsing after ten minutes, and the bound on how many
  the process holds for all agents.
- A process started again: the counts of a minute, the blocked counts and the
  requests are memory; the blocked count taken up from the record at a new
  process is not driven.
- A live read whose actor's session ends while it is open.

## http_proof.clj

What it checks: the runtime's way in over HTTP, as `softland.runtime.main`
starts it, driven with `java.net.http.HttpClient` as a browser, an agent and
Sid's command line drive it. An admin route without the admin token, and with a
wrong one, refused; an invitation made by the command line through the admin
route, its link carrying the code in the fragment, `#invitation=`; the limit
of programs set by the command line, and a kind it cannot read refused; a
sign-in POST whose `g_csrf_token` cookie and body differ, one with no
cookie, one from a foreign Origin and one with no Origin, each refused and none
setting a cookie; a sign-in with a matching `g_csrf_token` and the invitation,
answered `303` to `/` with an HttpOnly, SameSite=Lax, Path=/ cookie and no
Secure flag; `/me` and the page with that cookie, and the page without it
(Google's script, the client id, the code read from `location.hash`), each
answer carrying `X-Frame-Options: DENY` and `Content-Security-Policy:
frame-ancestors 'none'`; a request to `127.0.0.1:<port>` answered `308` to the
same path at `localhost:<port>`; a bad `%` escape in a form refused
`:form-field-not-read` (400), and one in the query answered 400 by the JDK's
server before any route (sent over a plain socket, since Java's client will
not send it); the sitting's cookie sent twice refused `:sitting-cookie-twice`;
a POST by the cookie with no Origin refused; an agent made, a right given to it in her own layer, its token made; a
run started with the token; an act with the token and its run accepted, naming
the run as its session, and read back as the agent's, beneath her right; the
token ended and the agent's next act refused `:session-ended`; a token asked
for by the agent with no sign-in, refused before her approval, approved by her,
and collected once; a read with no cookie and no token that sees the base's
fact and leaves out her own layer as `:may-not-see`.

How to run, from the repository root, under the lock that the store's cluster
proofs share:

```sh
flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:runtime-module -m softland.runtime.http-proof
```

What a pass shows: each check's sentence after `PASSED`, the `SHOWN` lines of
what each answer was (no token's secret is printed), and the count line
`{:test … :pass … :fail 0 :error 0}`; the process exits 0. A failed check
prints `FAILED` and its sentence, and the process exits 1.

State it changes outside the code it checks: one in-process cluster of Rama's,
with the store's module, closed at the end (its files are Rama's temporary
ones); one server on a free port of 127.0.0.1, stopped at the end; one
temporary folder holding the admin token's file, removed at the end. It writes
nothing under `~/.config`. It holds the cluster lock while it runs.

What it does not reach:

- Google itself: its keys, its button and its JavaScript. The proof's key pair
  stands in for Google's keys, and no browser runs the page's script. That is
  Part 3, with Sid's client id, in a browser.
- A `Host` header other than localhost's or 127.0.0.1's: Java's client sets it
  from the URI and lets no caller set another, so the refusal
  `:not-this-host` is not driven.
- A frame, a click taken through one, and a cookie set by another server on
  localhost: the proof sees the headers and the refusal, not a browser obeying
  them.
- The page's forms as a browser posts them, except the sign-in's: the other
  routes are driven with EDN.

## store_model.clj

No check of its own: the model of the store that the proof runs the steps on.
Its namespace docstring says which rules of the store it keeps and from where.
It is a value; each call gives the next store.
