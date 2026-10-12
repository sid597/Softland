# The scripts of the review of the whole read build

Each is a helper of `../../FULL_SPEC_REVIEW.md`, kept so that what that paper says it ran can be run
again. None is a proof, and none changes a file. Run each from the repository root.

| File | What it shows | Command |
|---|---|---|
| `docstrings.clj` | Every top-level definition of `src-store` and `test-store`, and which has no docstring | `clojure -M:store -i <file>` |
| `links.py` | Every link of every map of both trees, and which leads nowhere | `python3 <file>` |
| `words.clj` | Every keyword in the code of the read path, the record, the gates and the act, and which is on no list of words | `clojure -M:store -i <file>` |
| `members.clj` | Each declared word of each class (the read path's words, why a live read ends, the query's faults, the numbers, the act's and the declaration's reasons, the strays), and how many checks name it | `clojure -M:store -i <file>` |
| `r3_trial.clj` | 10,768 hostile things handed to the check of a query, and every path that the read path would build for those that are taken | `clojure -M:store-module -i <file>` |
| `measures_table.py` | Two runs of the measures side by side | `python3 <file> <the run before> <the run after>` |

What `words.clj` reports as "on no list" needs reading: Rama's own option names, the act's shape
language and the words of the store module's build are there beside any word of a newer build.
