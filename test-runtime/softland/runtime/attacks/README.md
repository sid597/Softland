# Runtime attacks — checks that fail today

[Parent: Runtime checks](../README.md) · [Runtime source entry](../../../../src-runtime/README.md).

Seven checks of the runtime's known faults and missing guards, written before
any repair or guard lands so that each fails today on the runtime at `d8ee975c`
and turns green when the builder repairs the fault or adds the guard.

## The numbers that the guards need (mine, for Sid to turn)

Defined in [`numbers.clj`](numbers.clj) (`default-guard-numbers`) as keys of
the runtime's numbers (`softland.runtime.seam/default-numbers`, passed through
`:numbers` at `softland.runtime.main/start` and `softland.runtime.seam/open`):

| Key | Default (mine, Sid's to turn) | What it bounds |
|---|---|---|
| `:sign-in-window-ms` | `10000` | The window in milliseconds over which posts to `/sign-in/*` from one remote address are counted. |
| `:max-sign-in-posts-per-address` | `30` | The most posts to `/sign-in/*` that one remote address may make in `:sign-in-window-ms` before further posts from that address are turned away before reaching Google or Auth. Allows up to three programs behind one address to poll `/sign-in/program/poll` once a second beside a browser sign-in and name confirmation. |
| `:max-watches-per-root` | `256` | The most live watches that one root — a person and every agent beneath her, across all of their connections — may hold open at once in one copy of the runtime. |
| `:max-visitor-watches` | `64` | The most live watches that visitors (connections with no credential) may hold open together at once in one copy of the runtime. |

## Shared overrides (`BUILD.md` section 2)

Every run below was executed from the root of a detached worktree of the head
(`d8ee975c`) behind the machine's cluster lock, with last night's classpath and
store overrides so no concurrent edit in the main tree could be loaded:

```sh
SDEPS='{:aliases {:runtime {:replace-paths ["/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store" "src/proposal/runtime-2026-11-21/spine/runtime-oracle/src" "test-auth" "src-wire" "src-runtime" "src-command" "test-wire" "test-runtime"]}}}'
LOCK=/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
STORE_PROP="-J-Dsoftland.store.path=/mnt/data/projects/research/softland/runtime-spine-2026-11-21/store-at-head/src-store"
```

When real `src-auth` is on `:runtime` and no concurrent edit is under way, each
check runs from the repository root with plain `clojure -M:runtime -m <ns>`.

## The seven checks, their commands, and their output today

### 1. Slow readers (`slow_readers.clj`)

- **Sentence:** `While several readers are slow, a copy still answers a page request, the line and a call to Auth.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.slow-readers
  ```
- **Output today (exit `1`):**
  ```text
  FAILED While several readers are slow, a copy still answers a page request, the line and a call to Auth.
         seen: {:cores 24, :slow-readers 26, :page {:ok false, :threw "java.net.SocketTimeoutException", :said "Read timed out"}, :line {:could-not :hear-the-runtime}, :auth {:could-not :hear-the-runtime}}

  0 passed, 1 failed.
  ```
  (Also verified at `14` slow readers across one 20-second ping on this 24-core
  JDK 21.0.13.1 host: `say!` plus one ping per slow reader pins `28 > 24`
  carriers, producing the same failure `{:cores 24, :slow-readers 14, :page {:ok false, :threw "java.net.SocketTimeoutException", :said "Read timed out"}, :line {:could-not :hear-the-runtime}, :auth {:could-not :hear-the-runtime}}`.)

### 2. The yes page (`yes_page.clj`)

- **Sentence:** `The yes page tells the truth: a second answer that names another actor than the one that stands is not told "Done"; a person who says no after her own yes is told that the program is signed in as her.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.yes-page
  ```
- **Output today (exit `1`):**
  ```text
  FAILED The yes page tells the truth: a second answer that names another actor than the one that stands is not told "Done"; a person who says no after her own yes is told that the program is signed in as her.
         seen: {:me-then-agent {:status 200, :text "Softland Softland Done. The program is signed in; go back to it."}, :agent-then-me {:status 200, :text "Softland Softland Done. The program is signed in; go back to it."}, :no-after-yes {:status 409, :text "Softland Softland This program was already answered before."}}

  0 passed, 1 failed.
  ```

### 3. Closed session (`closed_session.clj`)

- **Sentence:** `A session that is closed is not kept in use by the copy that holds it, past its next check.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.closed-session
  ```
- **Output today (exit `1`):**
  ```text
  FAILED A session that is closed is not kept in use by the copy that holds it, past its next check.
         seen: {:closed-before-renew? true, :bye-after-next-check nil, :act-sent-in-closed-session {:act-id [#uuid "8a697a11-268c-44ad-9a12-f34d27dd255d" :streaming #uuid "a3ed53a3-9fe3-4e28-967f-46bfa8011de3"], :digest "61799171ecb8ddb81baf626da4859cf8f71c2965840d50ce658fe3187d52711e", :ack :gate-ran, :answer {:answer :accepted, :stamp 2, :time 1795334073151}}}

  0 passed, 1 failed.
  ```

### 4. Standing that fails (`unwatched_standing.clj`)

- **Sentence:** `A standing that fails does not leave its connection unwatched.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.unwatched-standing
  ```
- **Output today (exit `1`):**
  ```text
  FAILED A standing that fails does not leave its connection unwatched.
         seen: {:conn-1 {:gone nil, :bye nil, :who-after-end {:who #uuid "62314097-a415-403e-92ac-5625f6d8340c", :root #uuid "62314097-a415-403e-92ac-5625f6d8340c", :sign-in #uuid "c19acd52-a4d4-44ba-b2d2-8b5249824e8d", :session #uuid "bae53feb-33fe-43c4-bfd2-e481445fed17", :kind :program, :own-layer #uuid "9e1caf28-41c7-427e-b6e6-7b8c1c0c990b", :until 1795938614936}}, :conn-2-same-root-recovery {:bye nil}, :conn-3 {:gone nil, :bye nil, :who-after-end {:who #uuid "62314097-a415-403e-92ac-5625f6d8340c", :root #uuid "62314097-a415-403e-92ac-5625f6d8340c", :sign-in #uuid "697196fc-7b7f-422d-831f-75009b455eeb", :session #uuid "6b65b88d-a12d-4e02-9ce1-a1613c0158aa", :kind :program, :own-layer #uuid "9e1caf28-41c7-427e-b6e6-7b8c1c0c990b", :until 1795938614946}}}

  0 passed, 1 failed.
  ```

### 5. Lost replacement (`lost_replacement.clj`)

- **Sentence:** `A replacement whose answer is lost does not lock the command line out.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" "$STORE_PROP" -M:runtime -m softland.runtime.attacks.lost-replacement
  ```
- **Output today (exit `1`):**
  ```text
  FAILED A replacement whose answer is lost does not lock the command line out.
         seen: {:case-a {:lost {:exit 1, :out {:threw "java.net.SocketException", :said "Connection reset", :could-not :reach-the-runtime}, :err ""}, :retry {:exit 1, :out {:why :wrong-secret, :could-not :take-that}, :err ""}, :who-after-grace {:exit 1, :out {:could-not :reach-the-runtime, :threw "java.net.http.WebSocketHandshakeException", :said "The sign-in stands for nobody now, or its session is closed. Run: sign-in", :status 401}, :err ""}}, :case-b {:lost {:exit 1, :out {:threw "java.net.SocketException", :said "Connection reset", :could-not :reach-the-runtime}, :err ""}, :who-after-grace {:exit 1, :out {:could-not :reach-the-runtime, :threw "java.net.http.WebSocketHandshakeException", :said "The sign-in stands for nobody now, or its session is closed. Run: sign-in", :status 401}, :err ""}}}

  0 passed, 1 failed.
  ```

### 6. Sign-in pacing by address (`sign_in_pace.clj`)

- **Sentence:** `Sign-in is paced by address: a flood of posts to `/sign-in/*` from one address, with forged tokens too, reaches neither Google nor Auth past the pace.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.sign-in-pace
  ```
- **Output today (exit `1`):**
  ```text
  FAILED Sign-in is paced by address: a flood of posts to `/sign-in/*` from one address, with forged tokens too, reaches neither Google nor Auth past the pace.
         seen: {:default-number-keys (:previous-ms :secret-ms :session-ms), :pace-limit 6, :flood-sent 30, :google-calls-during-flood 15, :auth-calls-during-flood 15, :flood-429-count 0, :other-address {:status 200, :form {:code "3AHSG4VN", :shown "3AHS-G4VN", :link "http://localhost:43733/yes?code=3AHSG4VN", :ends 1795334558154}}}

  0 passed, 1 failed.
  ```

### 7. Live watch caps (`watch_caps.clj`)

- **Sentence:** `Live watches are capped for each root and for visitors, in each copy.`
- **Command:**
  ```sh
  flock "$LOCK" clojure -Sdeps "$SDEPS" -M:runtime -m softland.runtime.attacks.watch-caps
  ```
- **Output today (exit `1`):**
  ```text
  FAILED Live watches are capped for each root and for visitors, in each copy.
         seen: {:default-number-keys (:previous-ms :secret-ms :session-ms), :root-5th-on-a {:anew {:layers [#uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e"], :left-out [], :place {:holds :no-fact}}}, :root-2-on-a {:anew {:layers [#uuid "b1efb5c5-8a94-45f7-8da9-934af6508288"], :left-out [], :place {:holds :no-fact}}}, :root-1-on-b {:anew {:layers [#uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e"], :left-out [], :place {:holds :no-fact}}}, :root-after-leave {:anew {:layers [#uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e"], :left-out [], :place {:holds :no-fact}}}, :visitor-4th-on-a {:anew {:layers [#uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e"], :left-out [{:layer #uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e", :why :may-not-see}], :place {:holds :no-fact}}}, :visitor-on-b {:anew {:layers [#uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e"], :left-out [{:layer #uuid "c568120a-195f-42fc-a877-bb17ea6e1b2e", :why :may-not-see}], :place {:holds :no-fact}}}, :visitor-reopened? true}

  0 passed, 1 failed.
  ```

## What was run, what was only read, what is open, and my choices

- **What was run:** All seven attack namespaces above, each in its own JVM from
  a detached worktree of `d8ee975c` behind `cluster.lock` with `BUILD.md`
  section 2's overrides; each printed `0 passed, 1 failed.` and exited `1` as
  shown verbatim above. Also verified `127.0.0.2 -> 127.0.0.1` loopback remote
  address binding on this host and `javap` bytecode of
  `JettyWebSocketRemoteEndpoint` and `missionary.impl.Observe`.
- **What was only read:** `AGENTS.md`; `overnight-audit/REVIEW-runtime-spine.md`;
  `spine/BUILD.md`; `src-runtime/README.md` and `src-runtime/softland/runtime/*`;
  `src-command/softland/command/{main,client}.clj`;
  `src-wire/softland/wire/{words,credential}.clj`; `test-wire/softland/wire/proof.clj`;
  `test-runtime/README.md` and `test-runtime/softland/runtime/{README.md,proof.clj,checks.clj,drive.clj,pure_proof.clj,command_proof.clj}`.
- **What is open (not reached by these seven checks):**
  1. Cross-copy global per-person write pacing and account-wide blocks in Auth
     (`BUILD.md` section 3, Option C's second half).
  2. Trusted reverse-proxy `X-Forwarded-For` handling when copies sit behind a
     load balancer rather than direct sockets (`:remote-addr` is then the proxy
     unless configured).
  3. Refusing `:replace-secret` over the line for `:browser` sign-ins until the
     pages rotate cookies (`REVIEW-runtime-spine.md`, C13).
  4. Real Google HTTPS JWKS fetches and multi-process `cluster-proof` kills.
- **My choices, each yours to turn:**
  1. **Guard number keys and defaults (`numbers.clj`):** `:sign-in-window-ms`
     (`10000`), `:max-sign-in-posts-per-address` (`30`), `:max-watches-per-root`
     (`256`), and `:max-visitor-watches` (`64`), placed on the runtime's
     `:numbers` map (`softland.runtime.seam/default-numbers`).
  2. **One namespace per attack:** Each check runs in its own JVM (`-m
     softland.runtime.attacks.<name>`) so JDK 21 virtual-thread carrier pinning
     in `slow-readers` cannot starve another check, and each has a single
     command and output.
  3. **Slow-readers sizing (`slow_readers.clj`):** Tests `cores + 2` slow
     readers across a full `jetty/ping-ms` cycle (after verifying `14` slow
     readers + 1 ping already freezes 24 cores), so both `jetty.clj`'s ping and
     `line.clj`'s `say!` must avoid pinning carriers.
  4. **Yes-page wording check (`yes_page.clj`):** Tests both actor-mismatch
     directions (`me -> agent` and `agent -> me`) for `409` without `"Done"`,
     and for `no` after her own `yes` checks `409`, no `"Done"`, no `"not
     signed in"`, and `"signed in"` + `"you"`, leaving exact prose to the
     builder.
  5. **Closed-session timing (`closed_session.clj`):** Uses `:session-ms 10000`
     (half-span `5 s`, unmoved re-check `1 s`) and waits `1.8 s` after a session
     closes right before `moved-on!`'s `open-session` with pushes dropped, so
     both a direct `who-is` re-read and `REVIEW-runtime-spine.md`'s 1-second
     timer backstop pass while today's 5-second false extension fails.
  6. **Standing failure coverage (`unwatched_standing.clj`):** Tests both
     `auth/watch-root` throwing on open (including same-root recovery on the
     next connection, catching a poisoned `delay` in `standing.clj`) and
     `standing/listen!` throwing into `seam/standing` (catching `line.clj`'s
     no-op `m/reduce` failure callback).
  7. **Lost-replacement coverage (`lost_replacement.clj`):** Tests both an
     immediate retry of `replace-secret` within `:previous-ms` and a `who` run
     after `:previous-ms` has expired following a dropped reply over a local TCP
     relay.
  8. **Guard refusal words (`sign_in_pace.clj`, `watch_caps.clj`):** Checks
     HTTP status `>= 400` and Google/Auth call counts for `/sign-in/*` pacing
     (plus `127.0.0.2` non-interference), and `{:ended {:could-not ...}}` with
     no `:anew` for watch caps (including person + agent sharing one `:root`,
     second copy isolation, and slot release on leave/close), leaving the exact
     `:could-not` keyword to the builder.
