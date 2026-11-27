# Runtime checks

[Parent: softland](../README.md) · [Runtime source entry](../../../src-runtime/README.md).

A check is explained once, by its sentence in its test file; a run prints that
sentence. This page says, for each file, what it checks, what a pass shows,
what state it changes, and what it does not reach.

| File | What it checks | State that it changes |
|---|---|---|
| [pure_proof.clj](pure_proof.clj) | The check of an ID token against tokens that a stand-in signs. Then it reads the source of the runtime, the wire and the command line for what each tree may hold. | None. |
| [drive.clj](drive.clj) | Nothing: it drives a runtime as a browser and as a program would, for the two proofs below. | None. |
| [checks.clj](checks.clj) | The runtime as a system outside it meets it, over real requests and over the line with the command line's own client: who may become a person, the name box, a program's sign-in, the two carriers, an act and its repeat, nobody acting for another, a series, many watches on one connection, a session and a sign-in that end at another copy, an agent, an invitation, a secret that is replaced, a session's time, the bounds. It is handed addresses, and both proofs below run it. | People, lanes and acts in the store and Auth that the copies stand on. |
| [proof.clj](proof.clj) | A store that is not born, the setup, and then `checks.clj`, on Rama's in-process cluster with three copies of the runtime in one JVM. | A cluster that is thrown away at the end. |
| [command_proof.clj](command_proof.clj) | The command line as a program: each command run as a process with the command line's own classpath, against one copy of the runtime on the in-process cluster. | A cluster that is thrown away; a folder for the command line's file, under the JVM's temporary folder. |
| [command_pure_proof.clj](command_pure_proof.clj) | The command line's parts that need no runtime, called through their vars: how many plain arguments each command takes, and under which UUID an act of a file is sent. | None. |
| [command_review_proof.clj](command_review_proof.clj) | What the review of 26 November found in the command line, each command run as a process as `command_proof.clj` runs it: a command with an argument left out, and a file that is edited and sent again, or sent again after `new-session`. | A cluster that is thrown away; folders for the command line's files, under the JVM's temporary folder. |
| [cluster_proof.clj](cluster_proof.clj) | The setup and `checks.clj` across three processes on a cluster that stays up; then a copy of the runtime killed while an act is on its way, Rama's worker of the store's module killed during a series of acts, Rama's worker of Auth killed while a connection is held, and six thousand roots watched at once. A check of what follows a kill fails when no worker was found to kill. | The cluster's store and Auth, which keep it; the logs of the copies. |
| [worker_kill_proof.clj](worker_kill_proof.clj) | The cluster proof's two kills of Rama's workers alone, through its own steps, on one copy of the runtime. | The cluster's store and Auth; the log of the copy. |
| [two_lines_proof.clj](two_lines_proof.clj) | Where the store's line and the spine meet: a watch held on two copies when its permission is ended in the store, directly, above it, or for a run. On the in-process cluster, or on a cluster that stays up with two processes. | A cluster that is thrown away, or the cluster's store and Auth. |
| [review_faults_proof.clj](review_faults_proof.clj) | The five faults of the spine's review of 22 November, each as a sentence that failed before its fix. | A cluster that is thrown away. |
| [open_session_proof.clj](open_session_proof.clj) | The word `:open-session` and what drives a session: the wire's form, the browser's faces, a program's sessions across copies, and the command line's `new-session --model`. | A cluster that is thrown away. |
| [attacks/](attacks/README.md) | Seven attacks on the spine, written to fail before their fixes. | A cluster that is thrown away. |

## What a pass shows

- **The pure proof:** the check of a token holds for the cases written, and the
  source holds what the trees' pages say under "Boundaries".
- **The proof on the in-process cluster:** each sentence of `checks.clj` holds
  across copies of the runtime that share nothing but Rama, in one JVM.
- **The cluster proof:** the same sentences hold across three processes; and
  the sentences of the kills hold for the one run that was made, on one
  machine, with one worker for each module and no second copy of its data.

## What no check reaches

Each is where a next check comes from.

- **Google itself.** Every sign-in ran on a stand-in's tokens. A real sign-in
  needs the client id, which Sid makes. No browser has sent Google's post.
- **A real browser.** The pages are asked for by an HTTP client that writes the
  `Origin` it is told to.
- **More than one machine, and a module with more than one copy of its data.**
  A killed worker here takes the module away until the supervisor has started
  it again; with copies on other machines Rama moves the work, and that was
  not run.
- **A kill at a chosen instant.** A process is killed while acts are on their
  way; which step each act had reached is not chosen, and a run does not
  repeat the instant of another.
- **Load:** many connections, many watches, a slow reader over the line. What
  waits for a slow reader is merged by place (the wire's pure proof); no check
  holds it through a socket.
- **A watch that ends when its permission ends.** The two-lines proof holds
  what the store's read path does: the watch is told anew that the lane is
  left out, and stays open. The runtime's page wrote "a watch closes when its
  permission ends"; which of the two is meant is open.
- **What a frame costs to read,** but by one case of a token over the bound.
- **A session that is moved on for longer than a few seconds,** and a secret
  that lasts a week: both ran with short numbers.
