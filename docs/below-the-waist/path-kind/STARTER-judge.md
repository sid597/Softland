# Path kind — the starter for the judge's session (2026-09-06, both first deliveries landed)

Written by the Claude builder at Sid's question ("should I start a new session and a prompt that we are judging, or be here because you are the implementer and should defend"). The builder sessions stay open to answer the card's questions and to repair; the judging runs from two fresh sessions, one per family, each holding the whole arc and having chosen neither approach; the same prompt goes to both unchanged. The two first deliveries are frozen at their hashes below; the card scores them apart from anything landed after feedback.

`````
Fence: you sit in the main checkout `/mnt/data/projects/Softland`; read anything under `docs/below-the-waist/` and, in the two worktrees, `src/app/client/`, `test/` and the git history of their branches; never `src/app/server/env.clj`. You commit on neither builder branch; what you find goes in `docs/below-the-waist/path-kind/` as `judge-<your family>.md`, exact paths, plain commits on `main`; push is mine. Two builder sessions are open, one per lane; I carry your questions to them and their answers back. A judge session of the other family holds this same prompt; where your marks disagree, the claim is resolved on code or an executed example, never by vote.

What I am building is every 2D tool as data above a waist of code that exists once, so that humans and agents can create, edit and reuse tools as records. The exploration of the path kind is done and the prototype is satisfied from both models; what remains is production, and the first implementation round is a comparison. I am not sure who is better, so it is a two-way thing first, to see for the meta and the future which models to use for what and how they play together. Both lanes got the same starter, the same starting code and the same behavioural target, and each owns design, implementation and verification end to end. The judge is the definer's records passing, the harness running and my own eye, never one session ranking the other; each family rates its own higher, so a counterexample with numbers weighs everything and an opinion weighs nothing. Nothing here needs a screen built for it: the prototype artifact already shows the thing, and a client's pictures are files its harness writes. The best in the field is a datapoint; nothing existing is carried forward because it exists; the question held is how the best team in the world for this would do it. What I want to end up holding is the card filled in for both lanes with a receipt behind every mark, the costs beside it, and the ground for assigning the roles of the rounds after.

Terrain, in the order the problem was worked. Everything is under `docs/below-the-waist/path-kind/`. `from-0-sid-questions.md` is my questions that opened the round, verbatim. `path-kind.html` (twin `path-kind.md`, live at https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478 ) is the picture, the contract block and the rulings on its ledger with the time. `attack-1.md` to `attack-4.md` are the definer's hard cases as records with numbers. `bench-9/waist-bench.html` (live at https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc ) with `bench-9/HANDOVER.md` is the prototype: the record vocabulary, the fixtures, the measured numbers that are the expected values for any client, and `bench-9/node-route/` runs its pure declarations in Node. `from-12-to-client.md` is the line into the client: the proposed first change, its callers, the five scenarios, what stays on the list. `docs/below-the-waist/two-chairs.md` holds the roles, the comparison and my card; `STARTER-client.md` is the prompt both builders received. The two first deliveries, each frozen at its hash: `waist/path-claude` at 4d9e0d6 in `/mnt/data/projects/Softland-claude` (two commits after it add only test scripts and this starter), and `waist/path-codex` at 07ea014 in `/mnt/data/projects/Softland-codex`. In each, start from `src/app/client/path/README.md`, its harness under `src/app/client/harness/`, and whatever its builder left under `docs/below-the-waist/path-kind/` naming its lane; every number you cite is one you produced from that lane's client. Each run keeps its model, version, effort and tools attached, yours included. Memory holds the proven roads for driving the app and for a WebGL2 bench headless.

Work the card's hardest row against whatever each build shows, and say where a claimed receipt hides work that was never run; end on the exact open question each lane leaves. Where you give a mark, say what supports it, when it stops holding, and what else changes with it.
`````

## The reading order, for Sid himself

The same terrain, as a list with what each file answers.

| Order | File | What it settles for the judge |
|---|---|---|
| 1 | `from-0-sid-questions.md` | The problem in your words. |
| 2 | `path-kind.md` (or the live page) | The picture, the contract, the three rulings. What "the path kind" means. |
| 3 | `attack-1.md` to `attack-4.md` | The hard cases with numbers; the definer's frame. |
| 4 | `bench-9/HANDOVER.md`, then the live bench | The prototype and its measured numbers, the expected values for both clients. |
| 5 | `from-12-to-client.md` | What the first client change had to do and what it may leave. |
| 6 | `two-chairs.md` | The roles, the comparison, the card. |
| 7 | `STARTER-client.md` | What both builders were told. |
| 8 | Each branch's handoff and READMEs, its harness, its receipts | What each says it did; then the commands below to see for yourself. |

## Testing the Claude lane yourself, from `../Softland-claude` at 4d9e0d6

The pure layer on the JVM, the definer's records as tests, about a minute:

```
clj -M:test -i test/app/client/path/run_pure.clj
```

The same code by hand, in a REPL (`clj -M:test`), so you make your own change-it moves:

```clojure
(require '[app.client.path.component :as c] '[app.client.path.records :as r])
(map :kind (:regions (c/run r/harness-z {})))                 ; (:stroke)  one skin
(count (:regions (c/run r/z-as-dabs {})))                     ; 24 dabs
(c/classify r/harness-z [64.0 64.0])                          ; :inside, the crossing once
(keys (:reads (c/run r/harness-z {})))                        ; what it read: no name, no colour
(keys (:reads (c/run r/border {:scale 2.0 :pan [1.0 0.0]})))  ; the border reads the view
(c/run (assoc r/harness-z :path/construction                  ; your own recipe, no engine change
         {:steps [{:out "path" :op :path/source :source "source" :tool "tool"}
                  {:out "skin" :op :path/envelope :path "path" :tool "tool" :stroke "paint.stroke.geometry"}
                  {:out "fill" :op :path/fill-region :path "skin.0.path" :rule :even-odd}]
          :return {:path "path" :regions ["fill"]}}) {})
;; under that recipe (c/classify ...) at the crossing answers :outside, a hole by parity;
;; the union above answers :inside. One skin, two readings, no engine change.
(:missing (c/run (assoc-in r/harness-z [:path/construction]   ; an op the table lacks is a failed run
         {:steps [{:out "face" :op :geometry/arrange :curves "source"}] :return {:path "face" :regions []}}) {}))
```

The browser lanes, every number and picture from the client, a few minutes:

```
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
node test/render_engine/run_verifier.mjs            # pass/fail, six guards, golden hashes
node test/render_engine/dump_result.mjs /tmp/claude-lane   # path-step.json, region3d-floor.json, png/
```

Compare `path-step.json` against `bench-9/HANDOVER.md`'s tables: the crossing as a union and as dabs, the dab count, the edge agreement, the rates, the parity rows. The pictures in `png/` are what the client painted. To see the crossing-painted-twice test fail, set the Z's `:overlap` to `:accumulate` in the crossing check's union record and run the verifier: the union assertion reads 0.8556 and fails.

What was deleted, and that nothing calls it:

```
git diff --stat bedb890..4d9e0d6
grep -rn "tessellation\|derive-mesh-set\|draw-path-range" src/app/client test/app/client
```

The builder's answers to the card's questions (the ten-line data flow, what was carried forward and why, what broke, what it is least sure of) are in its session and summarised in `HANDOFF-client-claude.md` section 3.
