(ns app.server.episode-test
  "first-light A P2 — pure units for the episode seam.

   The §5.1 kernel deliverable's G18-family routing unit (the thrice-fired
   foreign-read latent class) + the utterance import's validity, determinism
   (retry-idempotence), grain parity, and the flag-D skip predicate. The
   DURABLE halves (G3/G4) drill against the live cluster, not here."
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.episode :as ep]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.block-distiller :as bd]))

;; ===========================================================================
;; G18-family · imp:ep: routing unit (the §5.1 foreign-read gate, pure half)
;; ===========================================================================

(deftest imp-ep-extract-object-key-unit
  (testing "imp:ep: keys route to the object-key partition"
    (let [ok (ep/genesis-object-key)
          ik (ep/utterance-import-key ok "turn-0001")]
      (is (= ok (oc/extract-object-key ik))
          "the branch recovers the two-segment chat:<hex> object-key")
      (is (= (oc/partition-by-object-key 8 ik)
             (oc/partition-by-object-key 8 ok))
          "a foreign completion read lands on the task the import wrote on")
      (is (not= ik (oc/extract-object-key ik))
          "the pre-branch failure shape (fall-through to :else = whole string) is dead")
      (is (not= "chat" (oc/extract-object-key ik))
          "the leading-object-key truncation failure shape is dead")))
  (testing "existing families untouched (additive branch)"
    (is (= "abc123" (oc/extract-object-key "imp:md:abc123:deadbeef")))
    (is (= "chat:ffff" (oc/extract-object-key "imp:tr:chat:ffff:sb:deadbeef")))
    (is (= "asm:boxes" (oc/extract-object-key "imp:asm:asm:boxes:deadbeef")))
    (is (= "plain" (oc/extract-object-key "plain"))))
  (testing "the utterance surface reuses the routed src:tr: prefix (N1b precedent)"
    (let [ok (ep/genesis-object-key)
          sid (ep/utterance-source-id ok "turn-0001")]
      (is (= ok (oc/extract-object-key sid))))))

;; ===========================================================================
;; The utterance import request — valid, deterministic, right-grained
;; ===========================================================================

(def ^:private args
  {:object-key (ep/genesis-object-key)
   :turn-id "11111111-2222-3333-4444-555555555555"
   :text "hello land\n\nsecond paragraph"
   :time-ms 1752700000000
   :prev-turn-id nil})

(deftest utterance-import-request-valid
  (let [req (ep/utterance-import-request args)]
    (is (empty? (oc/import-request-validation-errors req))
        "the adapter-level request passes the kernel's import validation whole")
    (is (= (ep/genesis-object-key) (:partition/key req)))
    (is (= "sid" (get-in req [:actor :actor/id])) "asserted-by sid")
    (is (= :human (get-in req [:actor :actor/type])))))

(deftest p4-birth-and-turn-carry-the-real-pick-receipt
  (let [scene-context
        {:receipt/captured-at-ms 1752700000000
         :receipt/picked-at
         {:address "du:chat:p4:target"
          :src-path [:turns 2 :blocks 0]
          :view-instance [:vi :ground "target"]
          :point-world [90 120]}
         :receipt/placement {:point-world [90 120]}
         :receipt/worn-materials
         [{:material/master "fm:provenance"
           :material/revision "rev:fm:provenance:r1"
           :material/site :machine-rail}]
         :visible-count 9}
        birth (ep/utterance-import-request
               (assoc args
                      :position {:x 90 :y 120}
                      :scene-context scene-context))
        hint (first (filter #(= :episode-utterance (:entry-kind %))
                            (get-in birth [:payload :projection-hints])))
        unit (first (get-in birth [:payload :derived-units]))
        turn (ep/turn-record-request
              {:object-key (ep/genesis-object-key)
               :turn-id "turn-p4"
               :source-unit-id (:unit-id unit)
               :content-text "wish the target were calmer"
               :position {:x 90 :y 120}
               :time-ms 1752700000000
               :status :open
               :episode-id "episode-p4"
               :scene-context scene-context})
        turn-receipt
        (get-in turn [:payload :projection-hints 0 :turn :receipt])]
    (is (= "du:chat:p4:target"
           (get-in hint [:receipt :receipt/picked-at :address])))
    (is (= [:turns 2 :blocks 0]
           (get-in unit [:receipt :receipt/picked-at :src-path]))
        "the wish unit material itself retains the real pick provenance")
    (is (= (mapv :unit-id (get-in birth [:payload :derived-units]))
           (:origin-unit-ids hint))
        "every cut unit points back to the one birth receipt")
    (is (= "episode-p4"
           (get-in turn-receipt
                   [:receipt/created-during :episode/id])))
    (is (= "du:chat:p4:target"
           (get-in turn-receipt [:receipt/picked-at :address])))
    (is (empty? (oc/import-request-validation-errors birth)))
    (is (empty? (oc/import-request-validation-errors turn)))))

(deftest utterance-import-determinism
  (testing "an HTTP retry re-derives IDENTICAL import identity (T8 class)"
    (let [a (ep/utterance-import-request args)
          b (ep/utterance-import-request args)]
      (is (= (:import/key a) (:import/key b)))
      (is (= (:material/fingerprint a) (:material/fingerprint b)))
      (is (= (get-in a [:payload :derived-units])
             (get-in b [:payload :derived-units])))))
  (testing "distinct turns mint distinct identities"
    (let [a (ep/utterance-import-request args)
          b (ep/utterance-import-request (assoc args :turn-id "z-other-turn"))]
      (is (not= (:import/key a) (:import/key b)))
      (is (not= (mapv :unit-id (get-in a [:payload :derived-units]))
                (mapv :unit-id (get-in b [:payload :derived-units])))))))

(deftest utterance-grain-parity
  (testing "the cut rides free-cut-part :human-message — whole block + proper subs"
    (let [req (ep/utterance-import-request args)
          units (get-in req [:payload :derived-units])
          anchors (get-in req [:payload :source-anchors])]
      (is (= :human-message (:unit-kind (first units)))
          "block 0 is the whole-message block")
      (is (= (count units) (count anchors)) "1:1 unit/anchor pairing")
      (is (every? #(= ep/episode-distiller-id (:distiller-id %)) units)
          "the native stratum is id-disjoint from sense-block-v0")))
  (testing "the projection hint is ep:-namespaced, never :message / sb:"
    (let [req (ep/utterance-import-request args)
          [hint] (get-in req [:payload :projection-hints])]
      (is (= :episode-utterance (:entry-kind hint)))
      (is (clojure.string/starts-with? (str (:order-key hint)) "ep:"))
      (is (= "sid" (:role hint))))))

;; ===========================================================================
;; Flag D · the skip predicate (native-turn-event?)
;; ===========================================================================

(deftest native-turn-event-predicate
  (let [user-text {:type "user" :uuid "u1"
                   :message {:role "user" :content "plain typed text"}}
        tool-result {:type "user" :uuid "u2"
                     :message {:role "user"
                               :content [{:type "tool_result"
                                          :tool_use_id "t1"
                                          :content "result"}]}}
        assistant {:type "assistant" :uuid "a1"
                   :message {:role "assistant"
                             :content [{:type "text" :text "reply"}]}}]
    (is (ep/native-turn-event? user-text (bd/distill-event user-text 0))
        "a plain user text event is natively minted — skip")
    (is (not (ep/native-turn-event? tool-result (bd/distill-event tool-result 1)))
        "a tool_result-bearing user event is the agent's turn material — distill")
    (is (not (ep/native-turn-event? assistant (bd/distill-event assistant 2)))
        "assistant events always distill")))

;; ===========================================================================
;; first-light P2b — geometry cells + turn records (P2B.md receipt a)
;; ===========================================================================

(deftest geometry-settle-request-valid
  (let [req (ep/geometry-settle-request
             {:object-key (ep/genesis-object-key)
              :cells [{:unit-id "du:test:u1" :x 100.5 :y 200.25}]
              :camera {:x -40.0 :y 12.0 :zoom 1.5}
              :settle-id "settle-0001"
              :time-ms 1752700000000})]
    (is (empty? (oc/import-request-validation-errors req))
        "a hint-only settle import passes kernel validation (the F3 ruling)")
    (is (= (ep/genesis-object-key) (oc/extract-object-key (:import/key req)))
        "the settle key routes on the imp:ep: SHAPE — zero kernel edits")
    (let [hints (get-in req [:payload :projection-hints])]
      (is (= 2 (count hints)) "one cell + the camera")
      (is (= #{:episode-geometry :episode-camera} (set (map :entry-kind hints))))
      (is (every? #(clojure.string/starts-with? (str (:order-key %)) "geo:") hints)
          "geo: namespace — disjoint from ep:/sb:/%020d co-tenants")
      (is (= (ep/genesis-object-key)
             (get-in (first hints) [:geometry :world-id]))
          "placement identity is world-scoped [world-id, unit-id] (§9.3)"))))

(deftest geometry-settle-determinism-and-cells
  (testing "same settle-id + same payload = identical identity (retry no-op)"
    (let [args {:object-key (ep/genesis-object-key)
                :cells [{:unit-id "du:test:u1" :x 1.0 :y 2.0}]
                :camera nil :settle-id "s1" :time-ms 5}
          a (ep/geometry-settle-request args)
          b (ep/geometry-settle-request args)]
      (is (= (:import/key a) (:import/key b)))
      (is (= (:material/fingerprint a) (:material/fingerprint b)))))
  (testing "same settle-id + DIFFERENT geometry = fingerprint conflict material
            (the G4b forced-stale drill's mechanism)"
    (let [base {:object-key (ep/genesis-object-key)
                :cells [{:unit-id "du:test:u1" :x 1.0 :y 2.0}]
                :camera nil :settle-id "s1" :time-ms 5}
          a (ep/geometry-settle-request base)
          b (ep/geometry-settle-request (assoc-in base [:cells 0 :x] 999.0))]
      (is (= (:import/key a) (:import/key b)) "same settle-id → same key")
      (is (not= (:material/fingerprint a) (:material/fingerprint b))
          "geometry participates in the fingerprint via the preview carrier")))
  (testing "one unit = ONE cell address (upsert-in-place — settled semantics)"
    (is (= (ep/geometry-order-key "du:test:u1") (ep/geometry-order-key "du:test:u1")))
    (is (not= (ep/geometry-order-key "du:test:u1") (ep/geometry-order-key "du:test:u2")))))

(deftest turn-record-pins-the-revision
  (let [args {:object-key (ep/genesis-object-key)
              :turn-id "turn-77" :source-unit-id "du:test:u1"
              :content-text "the pinned words" :position {:x 3.0 :y 4.0}
              :time-ms 1752700000000 :prev-turn-id "turn-76" :status :open}
        req (ep/turn-record-request args)
        [hint] (get-in req [:payload :projection-hints])]
    (is (empty? (oc/import-request-validation-errors req)))
    (is (= :episode-turn (:entry-kind hint)))
    (is (clojure.string/starts-with? (str (:order-key hint)) "ep-turn:"))
    (is (= "the pinned words" (get-in hint [:turn :content-text]))
        "the pin carries the send-time content whole")
    (is (= (oc/source-hash "the pinned words") (get-in hint [:turn :content-hash]))
        "…and the kernel's own hash of it")
    (testing "status updates overwrite ONE cell under DISTINCT import keys"
      (let [done (ep/turn-record-request (assoc args :status :complete))]
        (is (= (:order-key hint)
               (:order-key (first (get-in done [:payload :projection-hints]))))
            "same cell address")
        (is (not= (:import/key req) (:import/key done))
            "a status change is a new import, never a fingerprint conflict")))
    (testing "same status retries converge"
      (let [again (ep/turn-record-request args)]
        (is (= (:import/key req) (:import/key again)))
        (is (= (:material/fingerprint req) (:material/fingerprint again)))))))

(deftest birth-carries-position-in-one-import
  (let [req (ep/utterance-import-request
             (assoc args :position {:x 55.0 :y 66.0}))
        hints (get-in req [:payload :projection-hints])
        geo   (filter #(= :episode-geometry (:entry-kind %)) hints)
        units (get-in req [:payload :derived-units])]
    (is (empty? (oc/import-request-validation-errors req))
        "birth + placement land in ONE valid acked import (§9.3 birth-position at mint)")
    (is (= (count units) (count geo)) "every birthed unit gets its cell")
    (is (= 55.0 (get-in (first geo) [:geometry :x])))
    (testing "no position → no geo hints (P2-shape unchanged)"
      (is (empty? (filter #(= :episode-geometry (:entry-kind %))
                          (get-in (ep/utterance-import-request args)
                                  [:payload :projection-hints])))))))

(deftest turn-record-carries-its-thread
  ;; one canvas, many conversations: the turn cell is the canvas's thread
  ;; registry — :thread-id names the lane's CLI session; nil = genesis
  (let [args {:object-key (ep/genesis-object-key)
              :turn-id "turn-91" :source-unit-id "du:test:u9"
              :content-text "spoken on a thread" :position {:x 1.0 :y 2.0}
              :time-ms 1753000000000 :prev-turn-id nil :status :open
              :thread-id "3d38aaaa-0000-4000-8000-000000000001"}
        req    (ep/turn-record-request args)
        [hint] (get-in req [:payload :projection-hints])]
    (is (empty? (oc/import-request-validation-errors req)))
    (is (= "3d38aaaa-0000-4000-8000-000000000001"
           (get-in hint [:turn :thread-id]))
        "the cell value carries the lane")
    (testing "the genesis thread is nil — pre-thread cells keep their shape"
      (let [bare (ep/turn-record-request (dissoc args :thread-id))]
        (is (nil? (get-in (first (get-in bare [:payload :projection-hints]))
                          [:turn :thread-id])))
        (is (= (:order-key hint)
               (:order-key (first (get-in bare [:payload :projection-hints]))))
            "thread never moves the cell address — same turn, same cell")))
    (testing "same turn + same thread retries converge"
      (let [again (ep/turn-record-request args)]
        (is (= (:import/key req) (:import/key again)))
        (is (= (:material/fingerprint req) (:material/fingerprint again)))))))

(deftest one-canvas-many-conversations-serve-probe
  ;; the composed chain on ONE in-process cluster: turn cell (the canvas's
  ;; thread registry) → thread-container read → one merged river with thread
  ;; stamps. Material lands through the REAL import lane; the CLI/harvest
  ;; halves are the proven per-session organs (identity follows the jsonl
  ;; line's sessionId — drilled live 2026-07-21), not re-proven here.
  (let [{:keys [oc-rt]} (bd/start-distiller-runtime!)]
    (try
      (let [canvas-conv "11111111-2222-4333-8444-555555555555"
            thread-conv "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
            canvas-key  (ep/episode-object-key canvas-conv)
            t0          1753100000000]
        (is (= :accepted (:status (ep/append-utterance!
                                   oc-rt {:text "the question block"
                                          :turn-id "blk-1" :time-ms t0
                                          :conversation-id canvas-conv})))
            "sid speaks at the canvas")
        (is (= :accepted (:status (ep/record-turn!
                                   oc-rt {:turn-id "blk-1"
                                          :source-unit-id (ep/utterance-unit-id
                                                           canvas-key "blk-1" 0)
                                          :content-text "the question block"
                                          :status :open :time-ms (+ t0 10)
                                          :conversation-id canvas-conv
                                          :thread-id thread-conv})))
            "the turn cell lands in the CANVAS, naming its thread")
        (is (= :accepted (:status (ep/append-utterance!
                                   oc-rt {:text "the thread reply material"
                                          :turn-id "reply-1" :time-ms (+ t0 500)
                                          :conversation-id thread-conv})))
            "the thread's material lives in ITS OWN container")
        (let [dc (fp/conversation-projection
                  {:oc-rt oc-rt}
                  {:face :conversation :address canvas-key :params {}})]
          (is (= [thread-conv] (:conversation/thread-ids dc))
              "the turn cells ARE the thread registry")
          (is (= thread-conv (:thread-id (first (:conversation/turn-records dc))))
              "served turn records carry the lane")
          (let [turns        (:turns dc)
                thread-turns (filter :thread-id turns)]
            (is (= 2 (count turns)) "canvas block + thread block: ONE river")
            (is (= [thread-conv] (mapv :thread-id thread-turns)))
            (is (= "the thread reply material"
                   (:text (first (:blocks (first thread-turns))))))
            (is (= ["the question block" "the thread reply material"]
                   (mapv #(:text (first (:blocks %))) turns))
                "time order holds across containers"))))
      (finally (bd/close-distiller-runtime! {:oc-rt oc-rt})))))

;; ===========================================================================
;; D-core (Sid 2026-07-22) · the episode chain — pure units + the serve weave
;; ===========================================================================

(deftest decide-episode-boundary-rule
  (let [mint (constantly "minted-uuid")]
    (testing "virgin lane: the first episode IS the lane (identity B), no seed"
      (is (= {:episode-id "lane-1" :fresh? true :seed? false}
             (ep/decide-episode {:lane-id "lane-1" :entry nil :last-turn nil
                                 :now-ms 1000 :mint-id mint}))))
    (testing "a warm lane rides its current episode"
      (is (= {:episode-id "ep-A" :fresh? false :seed? false}
             (ep/decide-episode {:lane-id "lane-1"
                                 :entry {:episode-id "ep-A" :last-turn-ms 1000}
                                 :now-ms (+ 1000 ep/episode-idle-ms -1)
                                 :mint-id mint}))))
    (testing "the boundary mints a seeded successor — the old episode is NEVER resumed"
      (is (= {:episode-id "minted-uuid" :fresh? true :seed? true}
             (ep/decide-episode {:lane-id "lane-1"
                                 :entry {:episode-id "ep-A" :last-turn-ms 1000}
                                 :now-ms (+ 1000 ep/episode-idle-ms)
                                 :mint-id mint}))))
    (testing "restart adoption: durable cells stand in for the lost runtime cell"
      (is (= {:episode-id "ep-B" :fresh? false :seed? false}
             (ep/decide-episode {:lane-id "lane-1" :entry nil
                                 :last-turn {:episode-id "ep-B" :time-ms 5000}
                                 :now-ms 6000 :mint-id mint})))
      (is (= {:episode-id "lane-1" :fresh? false :seed? false}
             (ep/decide-episode {:lane-id "lane-1" :entry nil
                                 :last-turn {:episode-id nil :time-ms 5000}
                                 :now-ms 6000 :mint-id mint}))
          "pre-chain cells adopt the lane itself"))))

(deftest summon-argv-episode-chain
  (testing "a fresh episode opens AS its uuid"
    (is (= ["claude" "--session-id" "ep-1" "-p" "hi"]
           (subvec (ep/summon-argv {:prompt "hi" :session-id "ep-1" :fresh? true})
                   0 5))))
  (testing "a warm episode resumes WITHIN its boundary (append-only, same file)"
    (is (= ["claude" "--resume" "ep-1" "-p" "hi"]
           (subvec (ep/summon-argv {:prompt "hi" :session-id "ep-1" :fresh? false})
                   0 5)))))

(deftest turn-record-carries-its-episode
  (let [args {:object-key (ep/genesis-object-key)
              :turn-id "turn-ep" :source-unit-id "u-1"
              :content-text "text" :position {:x 1 :y 2}
              :status :open :time-ms 1753100000000
              :episode-id "ep-2"}
        req  (ep/turn-record-request args)
        hint (first (get-in req [:payload :projection-hints]))]
    (is (= "ep-2" (get-in hint [:turn :episode-id]))
        "the cell is the durable chain link")
    (testing "pre-chain cells keep their shape (nil episode)"
      (is (nil? (get-in (first (get-in (ep/turn-record-request
                                        (dissoc args :episode-id))
                                       [:payload :projection-hints]))
                        [:turn :episode-id]))))
    (testing "the fingerprint pins the episode — a different session is a different fact"
      (is (not= (:material/fingerprint req)
                (:material/fingerprint (ep/turn-record-request
                                        (assoc args :episode-id "ep-3"))))))
    (testing "same turn + same episode retries converge"
      (is (= (:material/fingerprint req)
             (:material/fingerprint (ep/turn-record-request args)))))))

(deftest successor-episode-weave-pure
  (testing "no successors: canvas untouched (MC-T8 class)"
    (let [canvas [{:id "a" :time-ms 1} {:id "b" :time-ms 2}]]
      (is (= canvas (fp/merge-successor-episodes canvas [])))))
  (testing "successors weave by time, stamped :episode-id, NO :thread-id"
    (let [merged (fp/merge-successor-episodes
                  [{:id "a" :time-ms 100} {:id "c" :time-ms 300}]
                  [["ep-2" [{:id "b" :time-ms 200}]]])]
      (is (= ["a" "b" "c"] (mapv :id merged)))
      (is (= "ep-2" (:episode-id (second merged))))
      (is (nil? (:thread-id (second merged)))))))

(deftest episode-seed-composition
  (let [dc {:turns [{:speaker "sid"
                     :blocks [{:kind :paragraph :text "the question"}]}
                    {:speaker "resident"
                     :blocks [{:kind :thinking :text "hidden reasoning"}
                              {:kind :tool-use :text "tool json"}
                              {:kind :paragraph :text "the answer"}]}
                    {:speaker "sid" :thread-id "t-1"
                     :blocks [{:kind :paragraph :text "side thread talk"}]}]}]
    (testing "prose only, speaker-labelled, main-lane scoped"
      (let [seed (fp/compose-episode-seed dc nil)]
        (is (string? seed))
        (is (re-find #"sid: the question" seed))
        (is (re-find #"agent: the answer" seed))
        (is (not (re-find #"hidden reasoning" seed)) "noise folds away")
        (is (not (re-find #"side thread talk" seed)) "other lanes stay out")))
    (testing "a thread lane seeds from ITS OWN column"
      (let [seed (fp/compose-episode-seed dc "t-1")]
        (is (re-find #"side thread talk" seed))
        (is (not (re-find #"the question" seed)))))
    (testing "nothing to inherit → nil (the seedless virgin spawn)"
      (is (nil? (fp/compose-episode-seed {:turns []} nil))))))

(deftest episode-chain-serve-probe
  ;; the woven chain on ONE cluster: a turn cell carrying :episode-id (the
  ;; durable chain link) → successor-container read → ONE river, successor
  ;; blocks in the MAIN column (:episode-id stamp, no :thread-id stamp).
  (let [{:keys [oc-rt]} (bd/start-distiller-runtime!)]
    (try
      (let [canvas-conv "22222222-3333-4444-8555-666666666666"
            succ-conv   "bbbbbbbb-cccc-4ddd-8eee-ffffffffffff"
            canvas-key  (ep/episode-object-key canvas-conv)
            t0          1753200000000]
        (is (= :accepted (:status (ep/append-utterance!
                                   oc-rt {:text "before the boundary"
                                          :turn-id "blk-1" :time-ms t0
                                          :conversation-id canvas-conv}))))
        (is (= :accepted (:status (ep/record-turn!
                                   oc-rt {:turn-id "blk-2"
                                          :source-unit-id (ep/utterance-unit-id
                                                           canvas-key "blk-1" 0)
                                          :content-text "after the boundary"
                                          :status :open :time-ms (+ t0 100)
                                          :conversation-id canvas-conv
                                          :episode-id succ-conv})))
            "the turn cell carries the successor episode")
        (is (= :accepted (:status (ep/append-utterance!
                                   oc-rt {:text "successor reply material"
                                          :turn-id "reply-1" :time-ms (+ t0 500)
                                          :conversation-id succ-conv})))
            "the successor's material lives in ITS OWN container (identity B)")
        (let [dc    (fp/conversation-projection
                     {:oc-rt oc-rt}
                     {:face :conversation :address canvas-key :params {}})
              turns (:turns dc)]
          (is (= ["before the boundary" "successor reply material"]
                 (mapv #(:text (first (:blocks %))) turns))
              "one river across the chain, time order holds")
          (let [succ-turn (second turns)]
            (is (= succ-conv (:episode-id succ-turn))
                "the weave stamps the episode for the boundary marker")
            (is (nil? (:thread-id succ-turn))
                "successors continue the MAIN column, never a thread lane"))))
      (finally (bd/close-distiller-runtime! {:oc-rt oc-rt})))))
