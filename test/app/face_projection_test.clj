(ns app.face-projection-test
  "Framework Wave 1 · Lane W1-C gates G10-G13 (CONTRACT §11).

   G13's pure core (blocks->turns / apply-until-ms / shape-conversation / serve
   dispatch) + the G12 read-only source scan run with NO IPC. G10 (full-real-corpus
   receipt) and G11 (bounded prefix-consistent scrub) boot ONE in-process OC cluster,
   harvest + distill the REAL 7c80ce2a transcript, and ASSERT the projection against
   the block kernel's durable state. Both are GUARDED by find-real-transcript so the
   suite stays green off-box (like the block-distiller real receipt)."
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [app.server.episode :as episode]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.material-circulation :as circulation]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.dogfood.transcript :as tr]))

;; ===========================================================================
;; G13 (pure) — the projection registry + shaping as plain fns, request→data-context.
;; ===========================================================================

(def ^:private synthetic-page
  "Ordered river blocks as river-page would emit them, with :time-ms attached (as
   conversation-projection does). Two turns (e1 with 2 blocks, e2 with 1), river/time
   order preserved."
  [{:event-uuid "e1" :actor "human:external" :unit-id "u1" :form :human-message
    :text "hello" :order [1 0 "a"] :time-ms 100 :source-id "s1" :part-path "message" :block-path "1a"}
   {:event-uuid "e1" :actor "human:external" :unit-id "u2" :form :human-sub
    :text "world" :order [1 0 "b"] :time-ms 100 :source-id "s1" :part-path "message" :block-path "1b"}
   {:event-uuid "e2" :actor "model:claude" :unit-id "u3" :form :assistant-text
    :text "reply" :order [2 0 "a"] :time-ms 200 :source-id "s2" :part-path "text" :block-path "2a"}])

(deftest g13-pure-blocks->turns
  (testing "blocks group into turns by event-uuid, river order + speaker + kinds preserved"
    (let [turns (fp/blocks->turns synthetic-page)]
      (is (= 2 (count turns)))
      (is (= ["e1" "e2"] (mapv :id turns)) "turn order = river/event order")
      (is (= ["human:external" "model:claude"] (mapv :speaker turns)))
      (is (= [1 2] (mapv :order turns)) "turn :order = event-order")
      (is (= 2 (count (:blocks (first turns)))) "e1 has both its blocks")
      (is (= ["u1" "u2"] (mapv :id (:blocks (first turns)))) "block ids + order preserved")
      (is (= [:human-message :human-sub] (mapv :kind (:blocks (first turns)))) "per-block kind")
      (is (= [:assistant-text] (mapv :kind (:blocks (second turns)))))
      (is (= "reply" (:text (first (:blocks (second turns)))))))))

(deftest g13-pure-until-ms-prefix
  (testing "apply-until-ms is prefix-consistent at block grain (G11 pure form)"
    (let [turns (fp/blocks->turns synthetic-page)
          flat  (fn [ts] (vec (mapcat #(map :id (:blocks %)) ts)))
          r-nil (fp/apply-until-ms turns nil)
          r-100 (fp/apply-until-ms turns 100)
          r-150 (fp/apply-until-ms turns 150)
          r-200 (fp/apply-until-ms turns 200)]
      (is (= ["u1" "u2" "u3"] (flat r-nil)) "nil = whole page")
      (is (= ["u1" "u2"] (flat r-100)) "cut at 100 keeps e1 only")
      (is (= ["u1" "u2"] (flat r-150)) "150 < e2's 200 → still e1 only")
      (is (= ["u1" "u2" "u3"] (flat r-200)) "200 includes e2")
      ;; T1 < T2 ⇒ result(T1) is a prefix of result(T2)
      (doseq [[a b] [[r-100 r-150] [r-100 r-200] [r-150 r-200] [r-200 r-nil]]]
        (let [fa (flat a) fb (flat b)]
          (is (= fa (subvec fb 0 (count fa)))
              "smaller until-ms is a prefix of larger"))))))

(deftest g13-pure-shape-conversation
  (testing "shape-conversation emits the §7 data-context keys the face binds against"
    (let [dc (fp/shape-conversation
              {:blocks synthetic-page
               :read-plan {:river-events-total 247 :truncated? true :page-complete? false}
               :address "chat:abc" :limit 64 :until-ms nil :rendered-at-ms 999})]
      (is (= 2 (count (:turns dc))))
      (is (= "chat:abc" (:conversation/address dc)))
      (is (= 247 (:conversation/river-events-total dc)) "TRUE durable total, not page size")
      (is (= 3 (:conversation/blocks-returned dc)))
      (is (true? (:conversation/truncated? dc)))
      (is (true? (:conversation/debris-excluded? dc)))
      (is (= :river-page-has-no-cursor (:conversation/paging-lack dc)) "the v0 lack is logged")
      (is (= 999 (:face/rendered-at-ms dc))))))

(deftest g13-serve-dispatch
  (testing "serve routes server-side by face→projection; unknown face → error, never throw"
    (let [stub {:conversation (fn [_ctx req] {:turns [] :stub true :echo req})}
          ctx  {:oc-rt :fake}]
      (is (:stub (fp/serve stub ctx {:face :outline :address "x"})) ":outline → :conversation")
      (is (:stub (fp/serve stub ctx {:face "outline-face"})) "string face name maps too")
      (is (:stub (fp/serve stub ctx {:face :conversation})) "identity route")
      (let [r (fp/serve stub ctx {:face :nope})]
        (is (= :unknown-projection (:conversation/error r)) "unknown face → error data-context")
        (is (= [] (:turns r)) "error context is a valid renderable shape (no throw)"))))
  (testing "the real registry exposes exactly the registered projections as plain fns
            (W2 CONTRACT §16 superseded the W1 [:conversation] pin — amended at
            W2-INT; block-write INT adds :block-truth, the §5 single-unit echo)"
    (is (fn? (:conversation fp/projection-registry)))
    (is (fn? (:assembly fp/projection-registry)))
    (is (fn? (:face-list fp/projection-registry)))
    (is (fn? (:facet-materials fp/projection-registry)))
    (is (fn? (:material-inspector fp/projection-registry)))
    (is (fn? (:material-experience fp/projection-registry)))
    (is (fn? (:interaction-table fp/projection-registry)))
    (is (fn? (:block-truth fp/projection-registry)))
    (is (= #{:conversation :assembly :face-list :facet-materials
             :material-inspector :material-experience :interaction-table
             :block-truth}
           (set (keys fp/projection-registry))))))

(deftest interaction-table-is-total-without-a-cluster
  (testing "editable-material P5: the served interaction table answers `what
            does this gesture do, and who decided?` — and with no oc-rt it
            still answers, from the code floor alone (projection totality)"
    (let [r (fp/interaction-table-projection {:oc-rt nil} {})
          rows (:interaction-table/rows r)]
      (is (= 0 (:interaction-table/version r)))
      (is (seq rows))
      (is (empty? (:interaction-table/conflicts r))
          "the shipped table has no same-priority ties")
      (is (every? #(and (some? (:table/master-id %))
                        (some? (:table/verb %))
                        (some? (:table/effect-class %)))
                  rows)
          "every row links to the master and revision that decided it")
      (is (= (set (map (juxt :table/gesture :table/phase) rows))
             (set (:interaction-table/gestures r)))
          "every gesture the kernel can produce is covered")
      (is (= [:instance :master :floor] (:interaction-table/tiers r)))
      (is (contains? (set (map :table/verb rows)) :camera/pan))
      (is (contains? (set (map :table/verb rows)) :fold/toggle-section)))))

(deftest material-experience-names-failed-record-sources
  (with-redefs [episode/read-receipt-records
                (fn [& _] (throw (ex-info "receipt read poisoned" {})))
                circulation/read-circulation-records
                (fn [& _] (throw (ex-info "silver read poisoned" {})))]
    (let [result
          (fp/material-experience-projection
           {:oc-rt :present :rk-rt nil}
           {:address "du:chat:map-honesty:target"
            :params {:conversation-address "chat:map-honesty"}})
          sources (set (map :source (:experience/source-errors result)))]
      (is (= #{:receipt :silver-record} sources))
      (is (false?
           (get-in result
                   [:experience/query-plan :relation-runtime-available?])))
      (is (true?
           (get-in result
                   [:experience/query-plan
                    :object-container-runtime-available?])))
      (is (= 0
             (get-in result
                     [:experience/query-plan :relation-roundtrips]))))))

;; ===========================================================================
;; G12 (review-time, mechanical) — READ-ONLY by construction.
;; ===========================================================================

(deftest g12-read-only-by-construction
  (testing "face_projection.clj declares no depots/topologies and performs no writes"
    (let [src (slurp "src/app/server/rama/face_projection.clj")]
      (doseq [forbidden ["defmodule" "<<sources" "local-transform>" "->transform"
                         "foreign-append" "append-object-container-request"
                         "append-relation-request" "declare-depot" "stream-topology"
                         "microbatch-topology" "query-topology"]]
        (is (not (str/includes? src forbidden))
            (str "must not contain write/topology form: " forbidden)))
      ;; It reads ONLY the block kernel's existing query surface.
      (is (str/includes? src "river-page") "reads via river-page (block material)")
      (is (str/includes? src "read-source") "reads created-at-ms for :until-ms (bounded filter)"))))

;; ===========================================================================
;; G10/G11 (IPC, guarded) — real 7c80ce2a corpus receipt + bounded scrub.
;; ===========================================================================

(defn- find-real-transcript []
  (let [dir (io/file (str (System/getProperty "user.home")
                          "/.claude/projects/-mnt-data-projects-Softland"))]
    (when (.isDirectory dir)
      (first (filter #(re-find #"^7c80ce2a-.*\.jsonl$" (.getName ^java.io.File %))
                     (.listFiles dir))))))

(def ^:private corpus (atom nil))

(defn- boot-corpus!
  "Boot ONE OC cluster, harvest + distill the real 7c80ce2a into durable state.
   Shared across G10/G11 (the harvest+distill is minutes on the 6.9MB corpus)."
  []
  (when-let [file (find-real-transcript)]
    (let [rt (bd/start-distiller-runtime!)
          oc-rt (:oc-rt rt)
          req (tr/transcript-request
               :transcript/harvest
               {:transcript/request-id "face-projection-test-7c80ce2a"
                :transcript/source :claude-code
                :transcript/paths [(.getPath ^java.io.File file)]
                :time-ms 0})
          obs (vec (tr/read-jsonl-observations req file 0))
          conv-id (some #(when (str/starts-with? (str %) "7c80ce2a") %)
                        (distinct (map :transcript/conversation-id obs)))
          harvest (tr/harvest-transcripts-into-object-container! oc-rt req)
          summary (bd/distill-conversation! {:oc-rt oc-rt :source :claude-code
                                             :conversation-id conv-id})]
      {:rt rt :oc-rt oc-rt :object-key (:object-key summary) :summary summary})))

(use-fixtures :once
  (fn [f]
    (reset! corpus (boot-corpus!))
    (try (f)
      (finally
        (when-let [rt (:rt @corpus)] (bd/close-distiller-runtime! rt))
        (reset! corpus nil)))))

(deftest g10-projection-receipt
  (if-let [{:keys [oc-rt object-key summary]} @corpus]
    (let [dc   (fp/conversation-projection {:oc-rt oc-rt}
                                           {:face :conversation :address object-key
                                            :params {:limit bd/max-river-page-size}})
          page (bd/river-page {:oc-rt oc-rt :object-key object-key} bd/max-river-page-size)
          plan (:river-page/read-plan (meta page))
          proj-blocks (vec (mapcat :blocks (:turns dc)))]
      (testing "247-river-event durable baseline, honestly reported (never the page size)"
        (is (= 247 (:river summary)) "distill summary durable river count")
        (is (= 247 (:conversation/river-events-total dc)) "projection reports the TRUE total")
        (is (= (:river-events-total plan) (:conversation/river-events-total dc))))
      (testing "every river block appears exactly once across returned turns, in river order"
        (is (= (count page) (count proj-blocks)) "no block dropped or duplicated")
        (is (= (mapv :unit-id page) (mapv :id proj-blocks)) "flatten(turns) = river order, bijection")
        (is (= (mapv :form page) (mapv :kind proj-blocks)) "per-block kind carried")
        (is (= (mapv :text page) (mapv :text proj-blocks)) "exact stored text carried"))
      (testing "turn order = river order (event-order monotone non-decreasing)"
        (let [orders (mapv :order (:turns dc))]
          (is (= orders (vec (sort orders))))
          (is (apply distinct? orders) "one turn per event")))
      (testing "every block carries a kind + a wall time; time is monotone (prefix basis)"
        (is (every? (comp keyword? :kind) proj-blocks))
        (let [times (mapv :time-ms proj-blocks)]
          (is (every? (comp pos? long) times))
          (is (= times (sort times)) "monotone non-decreasing across the page")))
      (testing "truncation is signalled when :limit cuts (247 events > a 64-block page)"
        ;; the ceiling was raised 64 → 512 (ea8f120: the pinned serve window
        ;; cut settled replies), so truncation needs an EXPLICIT small limit
        (let [cut (fp/conversation-projection {:oc-rt oc-rt}
                                              {:face :conversation :address object-key
                                               :params {:limit 64}})]
          (is (true? (:conversation/truncated? cut)))
          (is (= :river-page-has-no-cursor (:conversation/paging-lack cut)))))
      (testing "the raised ceiling serves the 247-event corpus WHOLE — honestly complete"
        (is (false? (:conversation/truncated? dc)))
        (is (nil? (:conversation/paging-lack dc))))
      (testing "debris excluded BY DESIGN — reported, never leaked as a river block"
        (is (true? (:conversation/debris-excluded? dc)))
        (is (= 399 (:debris summary)) "the 399 debris rows are retained upstream, absent here")
        (is (every? #(not= :debris (:kind %)) proj-blocks))))
    (println "  [skip] real 7c80ce2a transcript not present — G10 skipped off-box")))

(deftest g11-bounded-prefix-consistent-scrub
  (if-let [{:keys [oc-rt object-key]} @corpus]
    (let [limit bd/max-river-page-size
          serve (fn [until-ms]
                  (fp/conversation-projection {:oc-rt oc-rt}
                                              {:face :conversation :address object-key
                                               :params {:limit limit :until-ms until-ms}}))
          flat  (fn [dc] (vec (mapcat #(map :id (:blocks %)) (:turns dc))))
          full  (serve nil)
          times (sort (mapv :time-ms (mapcat :blocks (:turns full))))
          t-lo  (nth times (quot (count times) 3))
          t-mid (nth times (quot (count times) 2))]
      (testing "per-page cost is bounded by :limit, never conversation length"
        (let [plan (:conversation/read-plan full)]
          (is (<= (:blocks-returned plan) limit))
          (is (= (+ 1 (* 4 limit)) (:seek-bound plan)) "seek bound = 1 + 4*limit (page-sized)")
          (is (<= (:seek-count plan) (:seek-bound plan)))
          (is (<= (:events-read plan) limit))
          (is (<= (:surfaces-read plan) limit))
          (is (<= (:unit-reads plan) limit))))
      (testing ":until-ms cuts are prefix-consistent (T1 < T2 ⇒ prefix at block grain)"
        (let [r-lo  (flat (serve t-lo))
              r-mid (flat (serve t-mid))
              r-full (flat full)]
          (is (seq r-lo) "a mid-window cut returns a non-empty prefix")
          (is (< (count r-lo) (count r-full)) "an interior cut is a strict prefix of the whole page")
          (is (= r-lo (subvec r-mid 0 (count r-lo))) "result(t-lo) ⊑ result(t-mid)")
          (is (= r-mid (subvec r-full 0 (count r-mid))) "result(t-mid) ⊑ result(full)")
          (is (every? (fn [b] (<= (:time-ms b) t-lo))
                      (mapcat :blocks (:turns (serve t-lo))))
              "every kept block is within the cut"))))
    (println "  [skip] real 7c80ce2a transcript not present — G11 skipped off-box")))

;; ===========================================================================
;; One canvas, many conversations — the thread merge (pure half)
;; ===========================================================================

(deftest thread-lanes-merge-into-one-river
  (let [canvas [{:event-uuid "e1" :actor "sid" :unit-id "c1" :form :prose
                 :text "one" :order [0 0 "a"] :time-ms 100}
                {:event-uuid "e2" :actor "sid" :unit-id "c2" :form :prose
                 :text "four" :order [1 0 "a"] :time-ms 400}]
        t1     [{:event-uuid "e3" :actor "model:claude" :unit-id "t1a" :form :prose
                 :text "two" :order [0 0 "a"] :time-ms 200}]
        t2     [{:event-uuid "e4" :actor "model:claude" :unit-id "t2a" :form :prose
                 :text "three" :order [0 0 "a"] :time-ms 300}
                {:event-uuid "e5" :actor "model:claude" :unit-id "t2b" :form :prose
                 :text "zero clock" :order [1 0 "a"] :time-ms 0}]
        merged (fp/merge-thread-lanes canvas [["th-1" t1] ["th-2" t2]])]
    (is (= ["c1" "t1a" "t2a" "t2b" "c2"] (mapv :unit-id merged))
        "global time order across lanes; the zero clock inherits ITS lane's floor")
    (is (= [nil "th-1" "th-2" "th-2" nil] (mapv :thread-id merged))
        "thread blocks stamped with their lane; canvas blocks untouched")
    (testing "no threads → the canvas is byte-identical (single-thread serve unchanged)"
      (is (= canvas (fp/merge-thread-lanes canvas []))))))

(deftest thread-stamp-flows-into-turns
  (let [blocks [{:event-uuid "ev1" :actor "model:claude" :unit-id "u1" :form :prose
                 :text "hi" :order [0 0 "a"] :time-ms 10 :thread-id "th-9"}
                {:event-uuid "ev1" :actor "model:claude" :unit-id "u2" :form :prose
                 :text "more" :order [0 1 "b"] :time-ms 11 :thread-id "th-9"}
                {:event-uuid "ev2" :actor "sid" :unit-id "u3" :form :prose
                 :text "canvas words" :order [1 0 "a"] :time-ms 12}]
        turns  (fp/blocks->turns blocks)]
    (is (= "th-9" (:thread-id (first turns))) "the turn carries its lane")
    (is (= ["th-9" "th-9"] (mapv :thread-id (:blocks (first turns)))))
    (testing "canvas turns carry NO thread key — absent, not nil (byte-stable serve)"
      (is (not (contains? (second turns) :thread-id)))
      (is (not (contains? (first (:blocks (second turns))) :thread-id))))))
