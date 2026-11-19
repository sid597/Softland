# Progress: the store's first who

Where this work stands. It began from [HANDOFF.md](HANDOFF.md).

## Now (19 November 2026)

- **Nothing in the store holds a key.** Sid decided it on 19 November. It
  turns his choice of D of the day before.
  - His question (‹f00f9421›): "why are you adament on having a key? is it
    not provided by google some sort of id and then in the store we check
    each event for who wrote it and if that user is first of all registered
    and is able to write where he wants to write."
  - His go, to the session's answer (‹afbff6f5›): "cool lets build this i
    agrre now with this".
- **What he agreed to,** in the session's words. For "the server" here, the
  decisions of 11 and 12 October say "the runtime", and the store's pages
  say "the sending program".
  - A person signs in with Google at the server. The server finds her id
    from Google's account id, or registers her, and writes that id as the
    who of each act that she makes.
  - The store checks, on each act, that this who may write there. That check
    is built.
  - The system is an id that the code knows, and nobody signs in as it. The
    answer put a fork to him here and leaned to one word, `:system`, as
    today. The other side of the fork is in the table of what is open.
  - Step one's code comes out of the tree and stays in the history.
- **His go was to the first form of the grounds.** A review on the same day
  found one ground stated too strongly and two costs missing. "Why no key"
  below holds the corrected grounds and marks each correction. Whether they
  change anything is his to say. The two calls that they leave him are
  marked in the table of what is open.
- **Step one was taken off `main`** by `3873f78b`, a whole revert of
  `8ba0da10`. The store's source and checks are as they were before step
  one. `c4f03d4b`, `629e70b3` and `1ff2d7f7` then changed what the store's
  pages say of who sent an act; no code changed. On each of the four
  commits the act's proof gave 166 passes and the pure proof 685, with no
  failure, and the namespaces that need Rama load. The proofs on a cluster
  were not run.
- [HANDOFF.md](HANDOFF.md), [STEP-ONE-STATE.md](STEP-ONE-STATE.md) and
  [REVIEW-step-one.md](REVIEW-step-one.md) stay as the record of what was
  explored, built and reviewed. None is a source for a build.

## Why no key

The session's grounds, as corrected after the review.

- **What makes a who true is the network rule.** Sid decided on 12 October
  that only programs on machines that he runs may reach Rama's ports; that
  each such program is trusted with the whole store, in any person's name;
  and that a program which is not trusted that far talks to the runtime
  (`src-store/README.md`, "Decisions of 11 and 12 October 2026", entry 4).
  His words that day: "this is a solved problem correct you can say the
  same for google.com".
- **What a signature in a gate would stop, and what it would not.**
  Corrected: the first form said that it stops nothing.
  - A hostile program inside the network can get past it. Rama asks no
    name. To send a thing that has the form of an act, a program needs
    Rama's own client, and that client needs only the conductor's host and
    port. `rama deploy` and `rama destroy` need no more. So such a program
    can read every table and replace the module, where the gates live.
  - What a signature still gives there is a record. If that program cannot
    get its acts signed, a later run of the true gates over the depots
    refuses them. Without signatures, such a run cannot tell them from true
    acts.
  - It would stop a program inside the network that is mistaken, and not
    hostile, from writing in a person's name: an agent, a converter, a
    test or a migration aimed at the real cluster.
  - Most of that stop is bought without a signature, for the programs that
    need not reach the ports: by the decision of 12 October, a program that
    is not trusted with the whole store talks to the runtime. That also
    covers reading, which a signature does not. Which programs those are is
    open (the first row of the table below). A deploy, a migration, or a
    test aimed at the real cluster still reaches the ports.
- **The store cannot ask Google on each act.** Google's sign-in token lasts
  about an hour and is not tied to what is written. A gate reads no clock,
  and no layer but its own. So the check that it is her is made once, at
  sign-in, by the server.
- **How D came to be chosen.** Corrected: the first form named one row of
  the table.
  - Sid gave no reason for D (‹c3075e08›: "I think I prefer D .. lets talk
    about how it is to be done").
  - The table before him («3978babd») set a plain id in the code against a
    key in three rows: "Can someone else act as root?" (for the plain id:
    "Yes, anything that reaches the store"), "Does the record prove itself
    later?" (no, against yes) and "Can two stores' roots be told apart?"
    (no, against yes).
  - The table did not hold the decision of 12 October beside the first row.
    The other two rows are costs of a plain id; they are under "What it
    costs".
  - Step one then made the who itself a key, with a rule that a signed act
    gives a permission only to a who that is a key. So each person needed a
    key; when Sid refused that, the server was to sign for her; and he
    asked why.
- **Where a signature would earn something:** on what a deployment
  publishes, such as Sid's system layer for other deployments, which he
  marks "in future". It would be one signature over the published line,
  checked by the program that fetches it. A signature on each act does not
  fix the order of the acts, and the store's state depends on the order
  (finding 1 of the review of step one).
- **What it costs.** Corrected: the first form named the server alone.
  - Every program that can reach Rama's ports can write as anyone, the
    server among them. For the server, only a key that each person holds
    herself would remove that, and Sid turned it down on 18 November
    (‹e77c7cbe›). A group that wants signed acts among its members can add
    them in its own layer. Whether a layer may ask its gate to check them
    is not decided.
  - The record does not prove itself later. A who is true through the
    network rule at the time of writing, and the log holds nothing of that.
    After a breach, or a mistake of a program inside, nobody can tell from
    the log whose the acts of that time were. A signature made when an act
    is written, with a key that the breached or mistaken program does not
    hold, would tell them apart. It would not for the time of a breach of
    the server itself, wherever its key is kept: a signer signs what the
    server asks. An act that is kept without a signature cannot be given
    one of its own later.
  - Two deployments' systems cannot be told apart while the system is one
    word in the code.
- **What the grounds rest on in Rama.** Read in its docs
  (`/mnt/data/projects/research/softland/rama-reference/docs/`), and not
  run.
  - No login, token or certificate is in them.
  - `RamaClusterManager` needs "the host and port of the Conductor node",
    and the CLI needs `conductor.host` (`19-operating-rama.md`). That one
    port carries both a client and a deploy is implied there, and not
    stated.
  - The REST API appends to a depot and reads a table with a plain `curl`
    (`24-rest-api.md`). It cannot send a thing that has the form of an act:
    an act-id holds a UUID, and the REST API's JSON has no type for one.
    The supervisors serve it apart from the conductor, so a network could
    open appends and reads without deploys. No port opens appends without
    reads.
  - So the grounds assume this: no program gets the reach of Rama's client
    without the conductor's. If one could, it could send acts and not
    replace the module, and a gate's check of a signature would stop more.

## Open, by where each becomes real

| What is open | Where it becomes real |
|---|---|
| The network rule itself. Nothing sets it today. With it: which programs reach Rama's ports at all, so that every other program, Sid's agents among them, talks to the runtime | When a cluster is first run for real data (entry 4 of 11 and 12 October) |
| The server takes the who from the sign-in session and never from the browser. Sid's size for sign-in, 15 October (‹3165b020›): "sign-in gives us a bunch of data and we just store that data in rama" | The sign-in build |
| How Sid comes to hold rights in the system's layers. In the answer that he agreed to: setup names his Google account, and the system gives his id rights at his first sign-in. Its form is not designed | The sign-in build |
| Who may act as the system. Today `:system` is a who like any other (`src-store/softland/store/door.clj`, "Room that the door leaves") | The sign-in build |
| Sid's agents run inside the network and can read every table. Keeping a thing from them means boxing them away from the ports, or sealing the value before the door | His thread of 18 November: "Keys, agents and reading" on the handoff |
| **Sid's call, with both sides named.** Acts are kept with no signature, as decided. What that gives up: the record does not prove itself later, and after a breach or a mistake of a program other than the server, a later run of the gates cannot tell that program's acts from true ones. The least form of the other side: the server signs each act with a key that the other programs do not hold; no gate checks it; a later reader, or a later run of the gates, does. What that costs: a key for the server and the keeping of it, a signing step on each act, a part for the signature in the act's form, and nothing gained for the time of a breach of the server itself. Acts that are kept before signing begins can then be covered only by one signature over the log as it stands on that day | Before the first record is kept |
| **Sid's call.** The system's id: one word in every deployment, as today, or an id fixed for each deployment in its deployed code. Either way every gate knows it without reading another layer. With one word, a system layer that arrives in another deployment has the same who as that deployment's own system. A session reads that as against his "First so that no collision" (18 November, on the handoff), which he said of registering deployments by name and id. What the other side costs: the fixture of the run-again proof holds 33 acts by `:system`, and the proofs use the word throughout | Before the first store is kept |
| The first act of a store that is kept, a restart on the same data, and telling a new store from one that lost its data. One rule of the review of step one stands without a key: a lost store is restored from a copy of its depots, and never by having its makers send their acts again through the door; on one machine the acts after the copy are lost. The facts gathered so far are on the handoff, "Running a store that keeps its data" | The first store that is kept |
| How a changed rule counts when the gates are run again over acts that were kept before it | Before the first record is kept |
| A signature on what a deployment publishes | Other deployments, "in future" |

## The review of 19 November

A fresh agent on another model reviewed the commits that took step one off,
in two rounds. Its job was to break them. It held that the decision can
stand.

- The first round, on `3873f78b`, `f315a206` and `c4f03d4b`, found the ground
  that was too strong, the two costs, faults in the outcome section of the
  review of step one, and the store's pages, which said one statement in
  five places and put a session's sentence under Sid's name. `629e70b3` and
  `f4821643` correct those.
- The second round found sentences in those corrections that still said too
  much, and one side missing from Sid's call on signatures. `1ff2d7f7` and
  the commit of this page correct those. Nobody reviewed these last
  corrections.

Its two reports are in the building session's transcript.

## Kept outside the repository

- `/mnt/data/projects/research/softland/first-act-pages-2026-11-18/`: his
  four handwritten pages.
- `/mnt/data/projects/research/softland/root-identity-and-merge-2026-11-18/`
  and `/mnt/data/projects/research/softland/top-key-custody-2026-11-18/`: the
  two research sweeps of the exploring session.
- `/mnt/data/projects/research/softland/first-act-review-2026-11-18/`: the
  probe of the review of step one, and its logs.
- `/mnt/data/projects/research/softland/first-act-opinion-2026-11-18/`: the
  second read's script for the weak public keys, with its output.
- `/mnt/data/projects/research/softland/first-act-taken-off-2026-11-19/logs/`:
  the logs of the act's proof, the pure proof and the load, on each commit.
