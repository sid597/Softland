# Wire checks

[Parent: softland](../README.md) · [Wire source entry](../../../src-wire/README.md).

A check is explained once, by its sentence in its test file; a run prints that
sentence.

| File | What it checks | State that it changes |
|---|---|---|
| [proof.clj](proof.clj) | Nothing: it is what the proofs share, one way to say a check and to end the process with a status. | The counts of the run, in memory. |
| [pure_proof.clj](pure_proof.clj) | By cases: a credential, the words and their check, one frame, and what waits for a slow reader. | None. |

**What a pass shows.** Each rule of the forms holds for the cases written; and
for 500 random runs of a live read's messages, a reader for whom a part waited
and was merged ends with the same answer as one who took each message.

**What no check reaches.**

- The words as a ClojureScript program would read them. The files are Clojure,
  and the browser's client is not built.
- What a frame costs to read beyond one case: a token over the bound is turned
  away fast. The cost of the largest frame that is within the bounds is not
  held by a check.
