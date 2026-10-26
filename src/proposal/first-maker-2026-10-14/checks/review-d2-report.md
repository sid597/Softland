# Review of decision 2's page: what it misses, gets wrong, and has

The reviewer of decision 2 wrote this (Opus 5.5, max effort). It is phase two, one pass over `decision-2-what-a-file-becomes.md` (889 words) and its runs, at HEAD c90c7bcc. My own thinking, written before I read the page, is `checks/review-d2-own-thinking.md`, and its runs are in `checks/review-d2-runs.txt`.

How to read the marks. "Page n" is a line of the page. A `path:line` is at HEAD. ‹id› is a line that Sid typed. "Maps n" and "detail n" are lines of `runs/decision-2-maps-output.txt` and `runs/decision-2-detail-output.txt`. "My Run X" is in `checks/review-d2-runs.txt`. "My reasoning" is mine. I did not edit the page.

## What the page misses

- **Sid's own lines on this very question.** Page 34-38 cites four lines of his, and none comes from `vision/LOG.md`. The log holds his notes on exactly this subject, and the task's sources named those lines:
  - The block is his unit of text: "each block is the base layer ... A bunch of block with children is a section" (`vision/LOG.md:126`, and also `:112, :141`). The page's default stops at headings (page 43, 51-52) and does not say that this departs from his picture.
  - The function is his unit of code, with the file as its parent (`LOG.md:193-195`). This supports the page's Parts B.
  - Functions can be independent of their file, and grouped and situated in different ways (`LOG.md:197`). This bears on the id: the id should not change when a function moves.
  - The last commit is the base, and work goes in a layer above it (`LOG.md:146`).
  - Code comes with "3 identies", and he asks which one is native (`LOG.md:170-173`).
  - "we don't have to fatten this" (`LOG.md:144`). This would support page 45's "computed" fields.
  - His open question: cut at ingest, or later (`LOG.md:119`).
  - Also ‹b22e6278›, ‹5ff89890›, ‹1082f32d› and ‹6b8c2421›.

  (My reasoning: the log is the primary source, and four of these lines bear directly on the page's two questions to Sid.)
- **The raw file is not kept.** The default (page 43-45) keeps the text of each named form and heading. It does not keep:
  - the whole file, since 129 files are over the bound (page 10);
  - the 213 top-level forms that have no name (maps 7);
  - the comments and blank lines between forms;
  - any order of the parts.

  So the store cannot give a file back. Sid's "we keep both the raw but also store it in softland way" (‹b22e6278›; `LOG.md:257`) is neither met nor weighed. A cut whose parts tile the file, with an order key, keeps the raw and the native as one set of facts. In my Run D, `stamp.clj` cut so gives two parts that are the file exactly, and `parse` and the door's check give `:ok` (my reasoning and my run).
- **Ids across layers.** The page never weighs its ids against layers. Sid's picture is a base with an active layer above it (`LOG.md:146`). A read takes "the nearest layer that holds a fact at a place" (`src-store/README.md:535`). So one thing needs one id in every layer. Each end then fares differently:
  - D (page 22) breaks this: an edit in the layer above gets another id, and the base's version shows through.
  - Under C (page 19), a layer above must read the base's registry to name a thing of the base.
  - Under B, a layer above can compute the id itself.

  (My reasoning.)
- **Names repeated across files merge silently under Id A.** The page's code ids are `:<ns>/<name>` (`runs/decision-2-maps.clj:222`). In this repository, 20 ns/name pairs are each defined in two or three files (my Run B). One is `probe.util/say`, which is in three probe projects under `src/proposal/backend-on-rama-2026-10-02/`.
  - One act per file puts each copy in a different act. A fact that names nothing to replace writes over another act's row, with no refusal (`src-store/softland/store/gate/decision.clj:233-240`; decision 3's run 1 shows it, `runs/decision-3-maps-output.txt:1-4`).
  - The page's scan counts only a name repeated within one file (`decision-2-maps.clj:85`), so it never meets this.
- **What "fits none of the three costs" costs.** Page 7 says that the log keeps each end for ever and that later acts cannot remove it, and stops there. The cost that decides between the id ends lies elsewhere:
  - every fact in other people's layers that points at an old id or key must be rewritten by its own writer;
  - a reader of history needs a link from the old id to the new;
  - a converter must keep its old way of deriving ids, or it cannot find the old things.

  This cost grows with the number of writers and with the years. It is also why the "72" of page 20 understates the cost of Id A: it counts only the converter's own facts, not the pointers that others hold. (My reasoning; `src-store/README.md:685-751`.)
- **Id C costs more than a read and a mark** (page 21).
  - No gate check makes the first writer win (`decision.clj:233-240`). So two converters that meet one new file mint two ids (my reasoning; not run).
  - A store started again from git gets new ids for everything.
  - The base's 32 marks (`src-store/softland/store/door/declaration.clj:127-134`) are shared by every tool of the system. d2-3 marks `:file/path` and `:code/in` for one vocabulary; one "part of" key shared by code and markdown would use one mark where a key per vocabulary uses several (my reasoning).
- **K5's grammar half.** Page 31 makes a key's word a fact (d2-4), and says nothing of its grammar. If a gate is ever to check a value against its key's grammar, the grammar must be in the act's own layer or in code. Order is local (`src-store/README.md:899-903`), and a gate reads no table of the other gate (`README.md:858-861`, a session's finding that Sid has not seen). So a gate of a person's layer cannot check a grammar stated once in the base. (My reasoning.)
- **What lands in the base is copied and shown.** The base is written on every task and, as built, everyone can read it, a person who is not signed in too (`README.md:905-922`; `src-store/softland/store/gate/first_act.clj:146`).
  - The page's scan takes every tracked .clj and .md (`decision-2-maps.clj:28-33`). That includes `history/`, which is 26.9 MB of the 49.0 MB (my Run A), and the vision log. The repository is closed source (`CLAUDE.md:92`).
  - A commit's author, if it became a fact, would be a person's data in a log that cannot forget yet (`README.md:718-719`).
  - This is not decision 2's question alone, but the cut sets what lands in the base, and the page does not raise it. (My reasoning.)
- **A relation held as a set cannot be found.** What points at a thing matches the whole value of a marked key (`src-store/softland/store/record/rows.clj:55-63`; `read_path/sources.clj:203-207`). So a relation can be found from its target only when it is one fact whose value is the target. The page's default "parent" (page 45) is a single value, but the page never states the rule. The walk's act 9, `:md/cites-sid #{"296a2a7b"}` (`walk-2-readme.md:24`), would never be found from the line it cites. (My reasoning.)
- **`.cljc` files.** The walk gave one definition with two bodies to decision 2 (`walk-1-stamp.md:67`), and the page never mentions it. The page's reader reads with `:read-cond :allow` (`decision-2-maps.clj:57`), which keeps one platform's branch, so its cut of a .cljc file would lose the other body (my reading of that line; not run). My Run B counts 79 top-level reader conditionals (a heuristic).
- **Two repositories.** Id B is made from the path and the name (page 18). Two repositories each hold a `README.md`, so once a second repository is read, B's ids collide unless the repository is part of them (my reasoning).

## What the page gets wrong

- **The "72 against 3" in the question to Sid.** Page 20 and page 56 say that a rename takes 3 changes under random labels and 72 under labels made from names. The 3 comes from decision 3's run, which kept the file's id and the namespace's id the same across the rename (`runs/decision-3-maps.clj:165-177`). That run assumed the road already knew that the moved file was the same thing. Without a rule that recognises a move, which is decision 3's to give, random labels also give new things and as many changes. So the "What happened" of Sid's question 2 does not hold as written. Page 24 itself says that C "leaves that open". (My reasoning, from that code.)
- **Folding `declare` does not cover the four files.** Page 43 and page 52 make the default "with the announcement folded in". Page 50 says that four of the store's files "name one thing twice", and gives `act.clj`'s `declare conform` as the example. But `read_path.clj` names `print-method` twice and `print-dup` twice, with `defmethod` (detail 31; `src-store/softland/store/read_path.clj:313, 316, 332, 335`). Those are real definitions that differ only in their dispatch value. With ids made from the path and the name, they still collide, and the door refuses `:place-named-twice`. The default needs the dispatch value in the name (my reasoning).
- **The id default tests one scheme and keeps another.** Page 44 and page 58 run B while the store is thrown away, and switch to C before anything is kept. So the trial never exercises what the kept store will rely on: the read before each write, the marks, the registry, and the `:replaces` that the read makes possible. And every id changes at the moment of keeping. A single scheme, an id made from the names when a thing is born and kept after that, gives B's repeatable ids and leaves decision 3 open as C does (my reasoning; my own page, section 4.2, I5).
- **The claims about names.**
  - "Code names fit (run)" (page 17) and "Code has names to make keywords from" (page 35) are true of the keyword form (maps 9). They are not true of identity: 20 ns/name pairs each belong to two or three files (my Run B).
  - "markdown parts have no names" (page 17, page 35) is also not so. A heading is a name. The trouble is that 59 files repeat a title and 18 a whole heading path (maps 15-16), and that a heading holds characters that a keyword cannot.

  (My reasoning.)
- **A paragraph's id need not be its position.** Page 14 says "A paragraph has no name, so its id would be its position". The page's own Id C, a minted id kept after birth and matched across commits by decision 3, is another answer. Tying Parts C to ids by position makes C look worse than it must (my reasoning).
- **"Computed in the read path" is not something the read path does.** Page 45 says docstring, arity and hash "can be computed in the read path". As built, a gate keeps a plain value's text and reads nothing of it (`declaration.clj:273-277`). A query has four words (`src-store/softland/store/read_path/query.clj:10-24`), and "A new word is added once, in code" (`query.clj:52`). So this means new read-path code, or a tool that works on what a read gives it (my reasoning).
- **d2-3 breaks the page's own rule.** d2-3, the instance of C on page 19, puts the base's mark of `:file/path` in the same act as the first file's path. "The act is decided with the marks as they were before it, so a fact of the act that writes the mark of its own key is not in it" (`decision.clj:392-395`). So that file would not be found by its path. The page's rule on page 21 and page 27, mark before the first file, is right. Its own act map breaks it, and the run checked only `parse` and the door's check (maps 21).

## What the page has that I did not think of

- **`declare` repeats a name.** A forward declaration gives one name twice in one file. Cut A over the store's own `act.clj` is refused `:place-named-twice` (maps 26; page 12). The same holds in `door/text.clj`, `read_path.clj` and `pure_proof.clj` (detail 29-32). The page also gives a rule: fold each `declare` into its definition. The list of def heads in my Run B left out `declare`, so I found only the `defmethod` pairs.
- **A real reader, and a better count of unnamed forms.** The page reads every code file with Clojure's reader and a resolver, and none is unreadable (`decision-2-maps.clj:39-65`; maps 2). That gives 213 top-level forms with no name, in 46 files (maps 7). My "about 900", from a line heuristic, counted forms without a def head and overstated the unnamed forms about four times. The page's count is the better one.
- **Paragraphs measured.** At paragraphs, 482 markdown files give more than 256 facts in one act per file, and 2 paragraphs are over the value bound, the largest 346,890 bytes (maps 12, 14). So even a cut at paragraphs needs a rule for a piece over the bound, and Parts C has a price in acts. I did not measure paragraphs.
- **Where the overflows are.** 13 of the 15 sections over the bound are in `history/` (detail 2). But five of the store's own proof files give more than 256 facts in one act: `door_proof` 344, `module_proof` 724, `pure_proof` 780, `read_path_proof` 304 and `run_again_proof` 680 (detail 35). So narrowing the inlet's scope removes most markdown overflow, and none of the code overflow in the store's own tree.
- **A path as a keyword.** With `/` read as `.`, only 2 tracked paths fail, both file names with a colon and spaces, and no two paths share one keyword at HEAD (maps 17; detail 24). I had treated a path as no keyword (act 5) without measuring a path made into one.
- **The seed of key words cannot sit in the store's namespace.** A key's word put under the store's namespace, `:softland.store/word`, is refused `:store-key-not-known` (maps 23, d2-5). So the seed of "keys are ids" cannot live under `softland.store` unless the code adds a store key. I had the regress, but not where the seed may live.
- **A git blob id as a keyword.** It is refused when the sha begins with a digit (`99ab32f4…`), and accepted when it begins with a letter (`c57f8910…`) (maps 19-20, 25). It shows plainly that hex ids cannot be keyword names.

## Does the page write any decision as if it were made?

No. Page 3 says that nothing is decided, and each default is marked as one (page 42, 52, 58). Two lines come close:
- Page 32 sets aside the queue's K5 reasoning by naming "the lens", which is a session's derivation of 14 October. It does not give the 13 October reason, that the tables are made again by running the gates over the depots (`src-store/README.md:842-861`).
- Page 58, "If you say nothing, A, and nothing is kept", ties whether the first road's store is kept to Sid's silence. No decision of his covers that.

Neither is written as a ruling.
