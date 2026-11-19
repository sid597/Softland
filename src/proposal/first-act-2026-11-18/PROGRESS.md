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
- **What he agreed to,** in the session's words:
  - A person signs in with Google at the server. The server finds her id
    from Google's account id, or registers her, and writes that id as the
    who of each act that she makes.
  - The store checks, on each act, that this who may write there. That check
    is built.
  - The system is the id that the code knows, `:system`. Nobody logs in as
    it.
  - Step one's code comes out of the tree and stays in the history.
- **Step one was taken off `main`** by `3873f78b`, a whole revert of
  `8ba0da10`. The store's source and checks are as they were before step
  one. On that commit the act's proof gave 166 passes and the pure proof
  685, with no failure, and the namespaces that need Rama load. The proofs
  on a cluster were not run.
- [HANDOFF.md](HANDOFF.md), [STEP-ONE-STATE.md](STEP-ONE-STATE.md) and
  [REVIEW-step-one.md](REVIEW-step-one.md) stay as the record of what was
  explored, built and reviewed. None is a source for a build.

## Why no key

The session's grounds, which Sid read before his go.

- **What makes a who true is the network rule.** Sid decided on 12 October
  that only programs on machines that he runs may reach Rama's ports, and
  that each such program is trusted with the whole store, in any person's
  name (`src-store/README.md`, "Decisions of 11 and 12 October 2026", entry
  4). His words that day: "this is a solved problem correct you can say the
  same for google.com".
- **A signature in a gate stops nothing that this rule leaves open.** Rama
  asks no name. By its docs, a plain `curl` appends to a depot or reads a
  table, and `rama deploy` and `rama destroy` need only the conductor's
  address (`/mnt/data/projects/research/softland/rama-reference/docs/`,
  `24-rest-api.md` and `19-operating-rama.md`; read, not run). So a program
  that could send a forged act could also read every table and replace the
  module, where the gates live.
- **The store cannot ask Google on each act.** Google's sign-in token lasts
  about an hour and is not tied to what is written. A gate reads no clock,
  and no layer but its own. So the check that it is her is made once, at
  sign-in, by the server.
- **How D came to be chosen.** The table that Sid chose D from on 18
  November («3978babd») said of a plain id: "Can someone else act as root?
  Yes, anything that reaches the store." It did not hold the decision of 12
  October beside that line. Step one then made the who itself a key, with a
  rule that a signed act gives a permission only to a who that is a key. So
  each person needed a key; when Sid refused that, the server was to sign
  for her; and he asked why.
- **Where a signature would earn something:** on what a deployment
  publishes, such as Sid's system layer for other deployments, which he
  marks "in future". It would be one signature over the published line,
  checked by the program that fetches it. A signature on each act does not
  fix the order of the acts, and the store's state depends on the order
  (finding 1 of the review).
- **What it costs.** The server can write as anyone. Only a key that each
  person holds herself would remove that, and Sid turned it down on 18
  November. A group that wants signed acts among its members can add them in
  its own layer.
- **Assumed, not run:** that Rama's ports cannot be opened for appends
  alone. If they can, a server's signature would start to stop something.

## Open, by where each becomes real

| What is open | Where it becomes real |
|---|---|
| The network rule itself. Nothing sets it today | When a cluster is first run for real data (entry 4 of 11 and 12 October) |
| The server takes the who from the sign-in session and never from the browser. Sid's size for sign-in, 15 October (‹3165b020›): "sign-in gives us a bunch of data and we just store that data in rama" | The sign-in build |
| How Sid comes to hold rights in the system's layers. In the answer that he agreed to: setup names his Google account, and the system gives his id rights at his first sign-in. Its form is not designed | The sign-in build |
| Who may act as the system. Today `:system` is a who like any other (`src-store/softland/store/door.clj`, "Room that the door leaves") | The sign-in build |
| Sid's agents run inside the network and can read every table. Keeping a thing from them means boxing them away from the ports, or sealing the value before the door | His thread of 18 November: "Keys, agents and reading" on the handoff |
| The first act of a store that is kept, a restart on the same data, and telling a new store from one that lost its data | The first store that is kept |
| How a changed rule counts when the gates are run again over acts that were kept before it | Before the first record is kept |
| A signature on what a deployment publishes | Other deployments, "in future" |

## Kept outside the repository

- `/mnt/data/projects/research/softland/first-act-pages-2026-11-18/`: his
  four handwritten pages.
- `/mnt/data/projects/research/softland/root-identity-and-merge-2026-11-18/`
  and `/mnt/data/projects/research/softland/top-key-custody-2026-11-18/`: the
  two research sweeps of the exploring session.
- `/mnt/data/projects/research/softland/first-act-review-2026-11-18/`: the
  review's probe and its logs.
- `/mnt/data/projects/research/softland/first-act-opinion-2026-11-18/`: the
  second read's script for the weak public keys, with its output.
