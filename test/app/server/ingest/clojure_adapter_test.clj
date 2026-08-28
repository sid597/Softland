(ns app.server.ingest.clojure-adapter-test
  "Phase P1 gates (code-atom work package) at ADAPTER grade, run GREEN in one
   IPC suite: G1 idempotent re-cut + import, G2 census + byte-fidelity, G3
   two-lane honesty, G4 deny-list, plus read-back and determinism (T4).

   Fixtures are pinned .txt copies of two specimens, produced from the SAME
   commit the phase was built on so determinism is a gate, never a live-file
   accident:
     HEAD = 7a48188690cd6766728309a664122bdfb6142867
     git show HEAD:src/app/server/rama/relation_kernel.clj \\
       > test/resources/code-atom/relation_kernel.clj.txt
     git show HEAD:src/app/electric_flow.cljc \\
       > test/resources/code-atom/electric_flow.cljc.txt
   Fixture sha-256 (recorded for tamper-detection):
     relation_kernel.clj.txt 808f00f412fdf2d2ba16943738bedeb517d74e7f5b4485fd370804c35184e929
     electric_flow.cljc.txt  f5a87f31b5b9d789b6b047f104b2bfb46e106446e0548e06f94129c9a9725c27
   Tests read the .txt fixtures ONLY — never the live src/ paths (T3 discipline)."
  (:require [app.server.rama.object-container :as oc]
            [app.server.ingest.clojure-adapter :as ca]
            [app.server.rama.object-container.runtime :as ocr]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

;; ---------------------------------------------------------------------------
;; Fixtures + pure helpers
;; ---------------------------------------------------------------------------

(def relation-kernel-fixture "test/resources/code-atom/relation_kernel.clj.txt")
(def electric-flow-fixture "test/resources/code-atom/electric_flow.cljc.txt")

;; T8: the U+0000 escape is expressed with string ops on escape characters,
;; NEVER a raw control byte in a source literal (harness NUL trap, quirks file).
(def u0000-escape (str \\ "u0000"))          ; the 6-char sequence backslash-u-0000

(defn read-fixture [path] (slurp path))

(defn reassemble
  "Sort units + comment-spans by offset, fill the gaps between them from the
   source, and concatenate. Byte-exact equality with the source is total
   coverage (SPEC §2.5 reconstruction guarantee)."
  [source units comment-spans]
  (let [segs (sort-by :start-offset
                      (concat (map #(select-keys % [:start-offset :end-offset :text]) units)
                              comment-spans))
        sb (StringBuilder.)]
    (reduce (fn [pos {:keys [start-offset end-offset text]}]
              (.append sb (subs source pos start-offset))
              (.append sb text)
              end-offset)
            0 segs)
    (.append sb (subs source (or (:end-offset (last segs)) 0)))
    (.toString sb)))

(defn kind-freq [units] (frequencies (map :unit-kind units)))

(defn units-by-block-path [units]
  (into {} (map (juxt :block-path identity)) units))

;; ---------------------------------------------------------------------------
;; IPC harness — ONE launch for the whole ns, injectable task counts (quirks
;; file: one IPC launch per ns; deterministic await; physical PState readers
;; for negative invariants).
;; ---------------------------------------------------------------------------

(defn launch-oc!
  "Launch ONLY object-container-module in a fresh IPC; return the runtime map
   the ocr/* fns + physical readers need. Task/thread counts are injectable."
  ([ipc] (launch-oc! ipc {:tasks 4 :threads 2}))
  ([ipc launch-opts]
   (let [module-name (get-module-name oc/object-container-module)]
     (rtest/launch-module! ipc oc/object-container-module launch-opts)
     {:ipc ipc
      :module-name module-name
      :object-container-requests-depot
      (foreign-depot ipc module-name "*object-container-requests-depot")
      :decisions-by-audit-id (foreign-pstate ipc module-name "$$decisions-by-audit-id")
      ;; G-F2 regression: the foreign completion read routes by extract-object-key.
      :import-completions-by-key (foreign-pstate ipc module-name "$$import-completions-by-key")
      :source-artifacts-by-id (foreign-pstate ipc module-name "$$source-artifacts-by-id")
      :containers-by-id (foreign-pstate ipc module-name "$$containers-by-id")
      :derived-units-by-id (foreign-pstate ipc module-name "$$derived-units-by-id")
      :source-derived-units-by-source
      (foreign-pstate ipc module-name "$$source-derived-units-by-source")
      :source-anchors-by-target (foreign-pstate ipc module-name "$$source-anchors-by-target")
      :outline-by-document (foreign-pstate ipc module-name "$$outline-by-document")
      :read-unit-query (foreign-query ipc module-name "read-unit")
      :read-common-material-for-source-query
      (foreign-query ipc module-name "read-common-material-for-source")})))

(def ^:private rt (atom nil))
(def ^:private ipc-ref (atom nil))

(defn with-ipc [f]
  (let [ipc (rtest/create-ipc)]
    (reset! ipc-ref ipc)
    (reset! rt (launch-oc! ipc))
    (try (f)
         (finally (.close ipc) (reset! rt nil) (reset! ipc-ref nil)))))

(use-fixtures :once with-ipc)

;; physical (non-query) reader: all derived-unit rows for a source id.
(defn physical-derived-units-for-source [runtime source-id]
  (foreign-select [(keypath source-id) MAP-VALS]
                  (:source-derived-units-by-source runtime)))

;; ===========================================================================
;; G2 — census + byte-fidelity (the load-bearing gate)
;; ===========================================================================

;; Expected census hand-derived ONCE from the pinned relation_kernel fixture
;; (rewrite-clj parse-string-all top-level children):
;;   103 :list forms  => head-freq {ns 1, def 9, defn 83, defn- 1, defrecord 8,
;;   defmodule 1}. Normalized (SPEC §3.4): ns 1, def 9, fn 84 (defn 83 + defn- 1),
;;   record 8, module 1. Total 103. 0 reader-conditionals (a .clj). utf16-len 51848.
(deftest g2-relation-kernel-census-and-fidelity
  (let [src (read-fixture relation-kernel-fixture)
        {:keys [units comment-spans]} (ca/clojure-form-v0 src)
        freq (kind-freq units)
        by-path (units-by-block-path units)]
    (testing "top-level form-unit census"
      (is (= 103 (count units)) "total top-level form units")
      (is (= 8 (:clj/record freq)) "defrecord count")
      (is (= 1 (:clj/module freq)) "defmodule count")
      (is (= 1 (:clj/ns freq)) "ns count")
      (is (= 9 (:clj/def freq)) "def/defonce count (~9)")
      (is (= 84 (:clj/fn freq)) "defn + defn- count")
      (is (= {:clj/ns 1 :clj/def 9 :clj/fn 84 :clj/record 8 :clj/module 1} freq)
          "the whole normalized kind histogram"))
    (testing "every unit :text == the blob substring (byte-for-byte)"
      (is (every? #(= (:text %) (subs src (:start-offset %) (:end-offset %))) units)))
    (testing "full reassembly reproduces the fixture byte-exactly"
      (is (= 51848 (.length src)) "P0-pinned UTF-16 length")
      (is (= src (reassemble src units comment-spans))))
    (testing "U+0000 escape region survives (T8)"
      (let [u (by-path "missing-idempotency-key")]
        (is (= :clj/def (:unit-kind u)))
        ;; the def's text carries the literal escape sequence, and it round-trips
        ;; through the full reassembly above (never a raw NUL byte anywhere).
        (is (str/includes? (:text u) u0000-escape))
        (is (= (:text u) (subs src (:start-offset u) (:end-offset u))))))
    (testing "^:private meta-wrapped 2nd symbol resolves its binding name"
      (is (= :clj/def (:unit-kind (by-path "id-part-separator")))))))

;; Expected from the pinned electric_flow fixture: 21 units = 11 :list + 10
;; top-level #? reader-conditionals; head-freq of lists {ns 1, def 4, defn 3,
;; e/defn 3}. Normalized: ns 1, def 4, fn 3, electric-fn 3, reader-cond 10.
;; utf16-len 32009. (P0's prose said "3 #? blocks"; the pinned fixture and P0's
;; own tools.reader row both show 10 — the fixture count governs.)
(deftest g2-electric-flow-reader-conditionals-and-fidelity
  (let [src (read-fixture electric-flow-fixture)
        {:keys [units comment-spans]} (ca/clojure-form-v0 src)
        freq (kind-freq units)]
    (testing "top-level reader conditionals are atomic :clj/reader-cond units (T7)"
      (is (= 10 (:clj/reader-cond freq)))
      (is (every? #(str/starts-with? (:text %) "#?")
                  (filter #(= :clj/reader-cond (:unit-kind %)) units))))
    (testing "e/defn forms classify :clj/electric-fn"
      (is (= 3 (:clj/electric-fn freq)))
      (is (every? #(str/starts-with? (:text %) "(e/defn")
                  (filter #(= :clj/electric-fn (:unit-kind %)) units))))
    (testing "whole normalized histogram"
      (is (= {:clj/ns 1 :clj/def 4 :clj/fn 3 :clj/electric-fn 3 :clj/reader-cond 10} freq)))
    (testing "reassembly exact"
      (is (= 32009 (.length src)))
      (is (every? #(= (:text %) (subs src (:start-offset %) (:end-offset %))) units))
      (is (= src (reassemble src units comment-spans))))))

;; ===========================================================================
;; R7 — block-path law (dedup #n, defmethod dispatch, positional, meta, ns).
;; Synthetic because the specimen has 0 duplicate names / 0 defmethods.
;; ===========================================================================

(deftest r7-block-path-law
  (let [src (str "(ns demo.core)\n"
                 "(def foo 1)\n"
                 "(def foo 2)\n"
                 "(defn foo [x] x)\n"
                 ";; banner line one\n"
                 ";; banner line two\n"
                 "(defmethod area :circle [c] (* 3 c))\n"
                 "(defmethod area :square [s] (* s s))\n"
                 "(println \"side effect\")\n"
                 "(def ^:private secret 42)\n"
                 "(comment (+ 1 2))\n"
                 "#?(:clj (def platform :jvm) :cljs (def platform :js))\n"
                 "^:private (def wrapped 1)\n")           ; top-level meta wrapper
        {:keys [units comment-spans]} (ca/clojure-form-v0 src)]
    ;; positional block-path = the 0-based code-lane ordinal (named forms consume
    ;; ordinals too), so the side-effect form is unit #6 -> "000006", the comment
    ;; form is #8, the reader-cond is #9 (the two banner ;; lines are a span, not
    ;; a unit, so they consume no ordinal).
    (is (= [["ns"           :clj/ns]
            ["foo"          :clj/def]
            ["foo~2"        :clj/def]        ; duplicate name -> ~2 in file order (F2: ~ not #)
            ["foo~3"        :clj/fn]         ; dedup is by binding name across kinds
            ["area::circle" :clj/multi]      ; defmethod appends dispatch value
            ["area::square" :clj/multi]
            ["000006"       :clj/other]      ; unnamed side-effect form -> positional
            ["secret"       :clj/def]        ; ^:private meta-wrapped name resolved
            ["000008"       :clj/rich-comment]
            ["000009"       :clj/reader-cond]
            ["wrapped"      :clj/def]]        ; head+name resolved THROUGH the top-level meta node
           (mapv (juxt :block-path :unit-kind) units)))
    (testing "the banner (two consecutive ;; lines) is ONE comment run"
      (is (= 1 (count comment-spans))))
    (testing "positional index is the code-lane ordinal (named forms consume it too)"
      (is (= "000006" (:block-path (nth units 6)))))
    (testing "cut still reassembles byte-exact"
      (is (= src (reassemble src units comment-spans))))))

;; ===========================================================================
;; F2 (DIFF_FALSIFICATION) — the `~N` dedup separator is collision-proof against
;; a literal `foo#2` var. `#` is legal inside a Clojure symbol (`foo#2` reads as
;; one symbol; `#'user/foo#2` compiles), so the old `#`-suffix collided: a dup
;; `foo` (dedup → `foo#2`) and a literal `(def foo#2 …)` produced ONE block-path
;; → ONE unit-id → the second DerivedUnitRow overwrote the first. `~` can never
;; be part of a symbol token, so it cannot collide.
;; ===========================================================================

(deftest f2-dedup-separator-no-collision-with-literal-hash-var
  (let [src (str "(ns x)\n"
                 "(def foo 1)\n"
                 "(def foo 2)\n"       ; duplicate binding name -> dedup to foo~2
                 "(def foo#2 3)\n")    ; a legal literal symbol foo#2 -> block-path "foo#2"
        {:keys [units]} (ca/clojure-form-v0 src)
        object-key "ok:f2-test"
        block-paths (mapv :block-path units)
        unit-ids (mapv #(ca/derived-unit-id object-key (:block-path %)) units)]
    (testing "the deduped duplicate and the literal foo#2 var get DISTINCT block-paths"
      (is (= ["ns" "foo" "foo~2" "foo#2"] block-paths)
          "dedup uses ~ (foo~2); the literal var keeps its own symbol name foo#2 — no collision"))
    (testing "distinct block-paths => distinct unit-ids (no last-write-wins overwrite)"
      (is (= 4 (count units)) "four top-level forms")
      (is (apply distinct? unit-ids) "all four derived unit-ids distinct"))
    (testing "control: the deduped duplicate is NOT foo#2 (which the old separator produced)"
      (is (not= "foo#2" (nth block-paths 2))))))

;; ===========================================================================
;; G-F1 (GATE_REVIEW 2026-07-09) — unparseable committed text degrades LOUD:
;; zero units + :parse-error on the cut; the import request still builds (raw
;; surface stores per R1 — atoms honestly absent) and passes kernel validation.
;; Pre-fix, parse-string-all threw and aborted the caller (a full-history
;; code-sync! died on the first of this repo's 5 committed-broken blobs).
;; ===========================================================================

(deftest gf1-unparseable-text-degrades-loud
  (let [src "(ns broken.core)\n(defn f [x]\n  (inc x)))\n"]   ; stray top-level )
    (testing "the cut degrades to zero units + a carried :parse-error (never throws)"
      (let [cut (ca/clojure-form-v0 src)]
        (is (= [] (:units cut)))
        (is (= [] (:comment-spans cut)))
        (is (some? (:parse-error cut)) "the failure is declared, not swallowed")))
    (testing "the import request still builds: surface stored (R1), zero derived units"
      (let [req (ca/clojure-source-import-request src "git-blob:gf1-broken"
                                                  {:request/id "gf1" :time-ms 7})]
        (is (= src (get-in req [:payload :source-raw-text])) "raw text verbatim")
        (is (= 0 (count (get-in req [:payload :derived-units]))) "no fake units")
        (is (empty? (oc/import-request-validation-errors req))
            "a 0-unit import is kernel-legal (the surface is real material)")))))

;; ===========================================================================
;; G1 — idempotent re-cut + import (IPC) + read-back
;; ===========================================================================

(deftest g1-idempotent-import-and-readback
  (let [runtime @rt
        src (read-fixture relation-kernel-fixture)
        source-ref "git-blob:relation-kernel-fixture"          ; P2 passes real "git-blob:<sha>"
        req-a (ca/clojure-source-import-request src source-ref
                                                {:request/id "clj-import-a" :time-ms 100000})
        req-b (ca/clojure-source-import-request src source-ref
                                                {:request/id "clj-import-b" :time-ms 100001})
        object-key (:object/key req-a)
        source-id (oc/source-id-for-object-key object-key)
        document-id (oc/document-id-for-object-key object-key)
        _ (ocr/append-object-container-request! runtime req-a)
        decision-a (ocr/await-object-container-decision runtime req-a 8000)
        _ (ocr/append-object-container-request! runtime req-b)
        decision-b (ocr/await-object-container-decision runtime req-b 8000)]
    (testing "both imports accepted; the second converges (import-completion replay)"
      (is (= :accepted (:status decision-a)))
      (is (= :accepted (:status decision-b)))
      (is (= object-key (:object/key req-b)) "same content+ref -> same object-key")
      (is (= (:import/key req-a) (:import/key req-b)) "deterministic import key"))
    (testing "physical PState read: NO duplicate unit rows after two imports"
      ;; physical reader (foreign-select over $$source-derived-units-by-source),
      ;; not the query API — negative invariants demand a physical scan.
      (is (= 103 (count (physical-derived-units-for-source runtime source-id)))))
    (testing "read-back: 3 named units resolve with correct kinds + anchors"
      (doseq [[block-path kind] [["ns" :clj/ns]
                                 ["relation-outcome" :clj/fn]
                                 ["RelationEdgeRow" :clj/record]]]
        (let [unit-id (ca/derived-unit-id object-key block-path)
              result (ocr/read-unit runtime unit-id)
              anchor (first (ocr/read-source-anchors runtime unit-id))]
          (is (some? result) (str "unit present: " block-path))
          (is (= kind (:unit-kind (:unit result))) (str "kind of " block-path))
          (is (= :derived-unit (:target-kind result)))
          (is (= (subs src (:start-offset anchor) (:end-offset anchor))
                 (:content-text result))
              (str "anchor span == stored form text for " block-path))
          (is (= document-id (:document-container-id (:unit result)))))))
    (testing "read-common-material-for-source returns the whole bundle"
      (let [bundle (ocr/read-common-material-for-source runtime source-id)]
        (is (= 1 (count (:containers bundle))) "one document container")
        (is (= 103 (count (:derived-units bundle))) "all form units")
        (is (seq (:anchors bundle)))
        (is (seq (:edges bundle)))))
    (testing "read-outline is EMPTY for code (kernel drops the :code-outline hint)"
      ;; the projection-hint <<cond has no :code-outline branch -> (default>)
      ;; skips it; outline is a computed projection whose code consumer is a
      ;; declared extension point, not v0. read-back rides read-unit instead.
      (is (empty? (ocr/read-outline runtime document-id))))))

;; ===========================================================================
;; G-F2 (GATE_REVIEW 2026-07-09; Sid-authorized kernel branch) — clojure import
;; keys "imp:clj:<object-key>:<sha>" route to the OBJECT-KEY partition. Pre-fix
;; extract-object-key fell to :else (whole string), so on a multi-task cluster a
;; foreign read-import-completion mis-routed and read nil (same class as
;; block-kernel F2). The unit assertion is DETERMINISTIC (routing is pure); the
;; IPC read confirms it end-to-end on the fixture's 4-task launch.
;; ===========================================================================

(deftest gf2-import-key-routes-to-object-key-partition
  (let [runtime @rt
        src (read-fixture relation-kernel-fixture)
        req (ca/clojure-source-import-request src "git-blob:gf2"
                                              {:request/id "gf2-import" :time-ms 222222})
        import-key (:import/key req)
        object-key (:object/key req)]
    (testing "extract-object-key routes imp:clj: to the object-key (pure, deterministic)"
      (is (= object-key (oc/extract-object-key import-key))
          "imp:clj: extracts the object-key ($$import-completions-by-key is partition-by-object-key), never :else"))
    (testing "end-to-end: the foreign completion read finds the row on the 4-task launch"
      (ocr/append-object-container-request! runtime req)
      (let [decision (ocr/await-object-container-decision runtime req 8000)]
        (is (= :accepted (:status decision)) "import accepted")
        (is (some? (ocr/read-import-completion runtime import-key))
            "read-import-completion finds the row (pre-fix: nil, mis-routed by :else on a multi-task cluster)")))))

;; ===========================================================================
;; G3 — two-lane honesty: comments mint no units; spans are exact + refinable
;; ===========================================================================

(deftest g3-two-lane-honesty
  (let [src (read-fixture relation-kernel-fixture)
        {:keys [units comment-spans]} (ca/clojure-form-v0 src)
        code-vocab #{:clj/ns :clj/def :clj/fn :clj/record :clj/protocol :clj/macro
                     :clj/multi :clj/test :clj/module :clj/electric-fn
                     :clj/reader-cond :clj/rich-comment :clj/other}]
    (testing "no unit carries a commentary-lane kind (all units are code-lane forms)"
      (is (every? code-vocab (map :unit-kind units)))
      (is (seq comment-spans) "standalone comment runs exist as spans"))
    (testing "comment spans have exact offsets"
      (is (every? #(= (:text %) (subs src (:start-offset %) (:end-offset %))) comment-spans)))
    (testing "units and comment spans never overlap (disjoint lanes)"
      (let [segs (sort-by first
                          (concat (map #(vector (:start-offset %) (:end-offset %) :unit) units)
                                  (map #(vector (:start-offset %) (:end-offset %) :comment) comment-spans)))]
        (is (every? (fn [[[_ e1 _] [s2 _ _]]] (<= e1 s2)) (partition 2 1 segs)))))
    (testing "a refinement anchor over a comment span is CONSTRUCTIBLE (P1 gate; full OC refinement is P2+)"
      (let [span (first comment-spans)
            ;; the same source-materialization the import used, for legal ids
            req (ca/clojure-source-import-request src "git-blob:g3" {:request/id "g3" :time-ms 1})
            payload (oc/request-payload req)
            source-row (first (:source-artifacts payload))
            doc-anchor (first (:source-anchors payload))       ; document anchor = [0, len]
            refinement (oc/->SourceAnchorRow (oc/source-anchor-id (str "refine:" (:start-offset span)))
                                             :comment-span
                                             (str "comment:" (:start-offset span))
                                             (:source-id source-row)
                                             (:source-ref source-row)
                                             (:source-hash source-row)
                                             (:start-offset span)
                                             (:end-offset span)
                                             nil
                                             (:event-id source-row))]
        ;; offsets legal against the source row's document anchor [start,end]
        (is (<= (:start-offset doc-anchor) (:start-offset refinement)))
        (is (<  (:start-offset refinement) (:end-offset refinement)))
        (is (<= (:end-offset refinement) (:end-offset doc-anchor)))
        (is (= (:source-id source-row) (:source-id refinement)))))))

;; ===========================================================================
;; G4 — deny-list (env.clj absent everywhere; denial counted with rule-id)
;; ===========================================================================

(deftest g4-deny-list
  (testing "env.clj is denied at every working-copy root (suffix match)"
    (is (true? (ca/denied-path? "src/app/server/env.clj")))
    (is (true? (ca/denied-path? "/home/sid/projects/Softland/src/app/server/env.clj")))
    (is (true? (ca/denied-path? "/mnt/data/projects/Softland/src/app/server/env.clj"))))
  (testing "ordinary code paths are allowed; no segment-boundary false positives"
    (is (false? (ca/denied-path? "src/app/server/rama/relation_kernel.clj")))
    (is (false? (ca/denied-path? "notsrc/app/server/env.clj")))
    (is (false? (ca/denied-path? "src/app/server/env.clj.bak"))))
  (testing "the guard REFUSES a denied path and NEVER runs the build thunk (T10)"
    (let [built? (atom false)
          marker (ca/guarded-clojure-import-request
                  "/mnt/data/projects/Softland/src/app/server/env.clj"
                  (fn [] (reset! built? true) :should-not-happen))]
      (is (false? @built?) "text-reading build thunk must not run for a denied path")
      (is (true? (:code-deny/denied? marker)))
      (is (= ca/code-deny-list-rule-id (:code-deny/rule-id marker)) "denial carries the rule-id")
      (is (= "code-deny-v1" (:code-deny/rule-id marker)))))
  (testing "the guard builds for an allowed path"
    (is (= :built (ca/guarded-clojure-import-request "src/app/foo.clj" (fn [] :built))))))

;; ===========================================================================
;; T4/determinism — no wall clock; import keys + unit ids identical across runs
;; ===========================================================================

(deftest determinism-no-wall-clock
  (let [src (read-fixture relation-kernel-fixture)
        opts {:request/id "det" :time-ms 424242}
        req1 (ca/clojure-source-import-request src "git-blob:det" opts)
        req2 (ca/clojure-source-import-request src "git-blob:det" opts)]
    (testing "identical inputs -> byte-identical request (no wall-clock leakage)"
      (is (= req1 req2)))
    (testing "the fixed clock is the only time source"
      (is (= 424242 (:request/time-ms req1)))
      (is (= 424242 (get-in req1 [:payload :object-containers 0 :created-at-ms]))))
    (testing "import key, object key, material fingerprint are stable strings"
      (is (= (:import/key req1) (:import/key req2)))
      (is (= (:object/key req1) (:object/key req2)))
      (is (= (:material/fingerprint req1) (:material/fingerprint req2))))
    (testing "every derived unit id recomputes to the same string"
      (let [ids #(mapv :unit-id (get-in % [:payload :derived-units]))]
        (is (= (ids req1) (ids req2)))
        (is (= 103 (count (ids req1))))))))
