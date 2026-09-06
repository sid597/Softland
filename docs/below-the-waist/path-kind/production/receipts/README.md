# Production slice 1 receipts

The broader client pure run is red on the pre-existing placed-text provider
error. `client-pure.log` and `baseline-placed-text.log` preserve it. The
focused path/placement tests are in `path-pure.log`; compile output is in
`build.log`. These receipts describe source commit `2391644`, with the
intentional two-image golden change committed alongside this directory.

From the checkout root:

```sh
clj -M:test -i test/app/client/path/run_pure.clj
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
node test/render_engine/run_verifier.mjs
node test/render_engine/dump_result.mjs <output-directory>
```

`verifier-before-record.log` shows the two expected red comparisons, with
six runtime guards green. `verifier-record.log` is the intentional run with
`RENDER_VERIFIER_RECORD_PATH_ROUTE=1`; only the self-crossing path and
placed-ink tree PNGs and their raw/PNG manifest hashes changed.
`verifier-final.log` is the subsequent run without recording enabled.
`verifier-final.json` is the repo runner's detailed receipt from that run.

The final dump writes `path-step.json`, `region3d-floor.json`,
`shader-digests.json`, and `png/`. `path-step.json` contains production pixel
coordinates/alpha, the original parity sweep, records/crossing/rate checks,
and separate source/geometry/packing/frame timings. All PNGs come from the
repo harness's readback; they are opaque previews, while alpha assertions
read the original RGBA bytes. The adapter record identifies SwiftShader.
`dump.log` records dump completion and lane results.

`golden-delta.json` compares the merged baseline PNGs with the production
captures: number of changed pixels, channel maximum, bounding box and sample
coordinates. To reproduce the pixel comparison after recording, extract the
old PNG with `git show dd5d34b:test/app/fixtures/render_engine/gpu-goldens/<file>`
to a scratch file and run the existing
`docs/below-the-waist/path-kind/judge-claude-receipts/probes/pngdiff.py`
against that and the current PNG. The handoff names the semantic reason for
the differences; no threshold in the verifier was relaxed.

The A2 probe is deliberately usable with both source trees:

```sh
clj -M:test -i test/app/client/path/probe_a2.clj
clj -Sdeps '{:paths ["/mnt/data/projects/Softland-claude/src" "src" "test"]}' -M:test -i test/app/client/path/probe_a2.clj
```

The resulting `a2-production.edn` and `a2-baseline.edn` correct the judge's
undeclared width rule and show the coincident-center regression. The
baseline command reads the Claude tree at `e8cb946`; it writes nothing there.
The standalone baseline placed-text receipt used the same source-path
override and ran `app.client.region3d.on-plane-test` with `clojure.test/run-tests`.

The broader client command reads the repo's registered pure namespace list
without loading its Rama-owning runner, selects every `app.client.*` entry,
and runs those unchanged namespaces:

```clojure
(require '[clojure.test :as t] '[clojure.java.io :as io] '[clojure.string :as str])
(let [form (with-open [r (java.io.PushbackReader. (io/reader "test/app/test_runner.clj"))]
             (first (filter #(and (seq? %) (= 'def (first %)) (= 'pure-namespaces (second %)))
                            (doall (take-while #(not= ::eof %)
                                              (repeatedly #(read {:eof ::eof} r)))))))
      names (filter #(str/starts-with? (str %) "app.client.") (second (nth form 2)))]
  (doseq [n names] (require n))
  (let [result (apply t/run-tests names)]
    (println result)
    (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1))))
```

Run that form with `clj -M:test`; the full server/Rama suite was not run in
this client-only slice. `custody.txt` records the branch/merge/source hashes
and exact final artifact paths; unrelated working files remain outside the
commits listed in the handoff.
