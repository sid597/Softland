;; Phase B (/rama skill) test suite for trail-view-module (build/trail-view/
;; CONTRACT.md §11). ONE IPC launch of all four modules (object-container →
;; transcript-ops → relation-kernel → trail-view), one shared fixture, all read
;; gates on disjoint fixture ids.
;;
;; Coverage: CONTRACT §11 gates 1-8, 11-14 (the trail-view halves). Kernel gates
;; 9/10/15 are green from Phase A (relation_kernel_test.clj); gate 16 = the whole
;; pre-existing suite staying green, run separately.
;;
;; Barriers (implementation-quirks discipline — deterministic, never polling as
;; proof): OC ingests use the decision-await barrier (OC is a STREAM topology);
;; relation asserts use the microbatch processed-count barrier keyed on the
;; relation module + topology. Both fully consume a request before any read.

(ns app.server.rama.trail-view-test
  (:require [app.server.rama.trail-view :as tv]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.markdown-adapter :as md]
            [app.server.rama.object-container.transcript-adapter :as tr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [com.rpl.rama.test :as rtest]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private rel-topo "relation-kernel-topology")

;; fixed reference clock (2026-ish); NEVER the wall clock — arrival buckets must
;; be deterministic so gate-4 window selection is reproducible.
(def ^:private T 1782000000000)
(def ^:private sixty-days 5184000000)

;; ── harness ──────────────────────────────────────────────────────────────────
(defn oc-ingest!
  [rt req]
  (ocr/append-object-container-request! rt req)
  (ocr/await-object-container-decision rt (oc/request-partition-key req) (oc/request-id req) 5000)
  req)

(defn rel-harness
  [rt]
  (let [!n (atom 0)]
    {:submit! (fn [req] (rk/append-relation-request! rt req) (swap! !n inc) req)
     :drain!  (fn [] (rtest/wait-for-microbatch-processed-count
                      (:ipc rt) (:module-name rt) rel-topo @!n 30000))}))

(defn tobs
  "Transcript observation map (mirrors object_container_test/transcript-observation)."
  [conversation-id line-idx message-uuid role content]
  (let [offset (* 100 line-idx)]
    {:transcript/ingest-request-id "tv-transcript-run"
     :transcript/source :claude-code
     :transcript/conversation-id conversation-id
     :transcript/message-uuid message-uuid
     :transcript/redacted-payload {:type "message" :message {:role role :content content}}
     :transcript/redacted-preview (pr-str content)
     :source/file-key (str "file:" conversation-id)
     :source/file-id (str "fid:" conversation-id)
     :source/file-path (str "/tmp/" conversation-id ".jsonl")
     :source/file-generation-key "generation-1"
     :source/byte-offset offset
     :source/byte-length 80
     :source/line-hash (oc/source-hash (pr-str content))}))

(defn container-of [req kind]
  (some #(when (= kind (:container-kind %)) %) (get-in req [:payload :object-containers])))

(defn tref [kind id] (rk/->target-ref kind id))

(defn assert-req
  [kind from to asserter atype claimed opts]
  (rk/assert-request (merge {:kind kind :from from :to to
                             :asserter-actor-id asserter :asserter-type atype
                             :asserted-at-ms claimed :sent-at-ms T}
                            opts)))

;; ── fixture material ─────────────────────────────────────────────────────────
(def ^:private doc1-raw
  "# Trail Fixture\nfirst paragraph body\n\n- alpha item\n- beta item\n\n## Section Two\nclosing paragraph")
(def ^:private doc2-raw "# Other Doc\nsupporting evidence text")
(def ^:private doc3-raw "# Lonely Doc\nno relations here")

(defn build-fixture!
  "Ingest material + relations; return the id map + harness the gate blocks read."
  [rt]
  (let [{:keys [submit! drain!] :as harness} (rel-harness rt)
        d1 (oc-ingest! rt (md/source-ingest-request doc1-raw "trail/doc1.md"
                                                     {:request/id "tv-d1" :time-ms 1000}))
        d2 (oc-ingest! rt (md/source-ingest-request doc2-raw "trail/doc2.md"
                                                     {:request/id "tv-d2" :time-ms 1000}))
        d3 (oc-ingest! rt (md/source-ingest-request doc3-raw "trail/doc3.md"
                                                     {:request/id "tv-d3" :time-ms 1000}))
        doc1-id (oc/document-id-for-object-key (:object/key d1))
        doc2-id (oc/document-id-for-object-key (:object/key d2))
        doc3-id (oc/document-id-for-object-key (:object/key d3))
        okey1   (:object/key d1)
        d1-src  (oc/source-id-for-object-key okey1)
        outline (ocr/read-outline rt doc1-id)
        blockA  (:target-id (first outline))
        blockB  (:target-id (second outline))
        blockA-anchor (oc/source-anchor-id blockA)
        blockA-text (:content-text (first outline))
        ;; transcript conversation — 4 messages incl. 1 tool call + 1 tool result
        conv-id "tv-conv"
        req1 (tr/transcript-observation-import-request
              (tobs conv-id 1 "m1" "assistant"
                    [{:type "text" :text "Inspecting the kernel."}
                     {:type "tool_use" :id "tu-1" :name "Read" :input {:file "relation_kernel.clj"}}])
              {:request/id "tv-tr1" :time-ms 10000})
        msg1 (container-of req1 :chat-message)
        conv (container-of req1 :chat-conversation)
        req2 (tr/transcript-observation-import-request
              (assoc (tobs conv-id 2 "m2" "user"
                           [{:type "tool_result" :tool_use_id "tu-1" :content "It matches."}])
                     :transcript/previous-message-container-id (:container-id msg1))
              {:request/id "tv-tr2" :time-ms 10100})
        msg2 (container-of req2 :chat-message)
        req3 (tr/transcript-observation-import-request
              (assoc (tobs conv-id 3 "m3" "assistant" [{:type "text" :text "Looks correct."}])
                     :transcript/previous-message-container-id (:container-id msg2))
              {:request/id "tv-tr3" :time-ms 10200})
        msg3 (container-of req3 :chat-message)
        req4 (tr/transcript-observation-import-request
              (assoc (tobs conv-id 4 "m4" "user" [{:type "text" :text "Ship it."}])
                     :transcript/previous-message-container-id (:container-id msg3))
              {:request/id "tv-tr4" :time-ms 10300})
        _ (oc-ingest! rt req1)
        _ (oc-ingest! rt req2)
        _ (oc-ingest! rt req3)
        _ (oc-ingest! rt req4)
        conv* (:container-id conv)
        msg1-id (:container-id msg1)
        conv-ref (tref :conversation conv*)
        d1-ref   (tref :container doc1-id)
        d2-ref   (tref :container doc2-id)
        bA-ref   (tref :container blockA)
        bB-ref   (tref :container blockB)
        base-opts (fn [rid] {:request-id (str rid "-req") :idempotency-key (str rid "-idem")})]
    ;; ── relations ──
    ;; based-on (cross-doc): doc1 -> doc2 (sid); re-asserted for gate 8 staleness.
    (submit! (assert-req :based-on d1-ref d2-ref "sid" :human T (base-opts "bo")))
    ;; produced (conversation -> doc1) by the importer.
    (submit! (assert-req :produced conv-ref d1-ref "import:transcript" :import T (base-opts "pr")))
    ;; dead-end (unary) on blockA (sid).
    (submit! (assert-req :dead-end bA-ref (rk/unary-to-ref bA-ref) "sid" :human T (base-opts "de")))
    ;; verdicts on blockA — disagreement (gate 6): sid refutes (with evidence), llm confirms.
    (submit! (assert-req :refutes conv-ref bA-ref "sid" :human T
                         (merge (base-opts "rfA")
                                {:evidence-source-id d1-src :evidence-anchor-id blockA-anchor})))
    (submit! (assert-req :confirms conv-ref bA-ref "llm:opus-4-8/run-x" :llm T (base-opts "cfA")))
    (drain!)
    ;; gate-8 staleness: re-assert based-on with a LATER claimed time (bumps status-changed).
    (submit! (assert-req :based-on d1-ref d2-ref "sid" :human (+ T 1000)
                         {:request-id "bo-req2" :idempotency-key "bo-idem2"}))
    (drain!)
    ;; gate-7 supersession fold on blockB: sid confirms, retracts confirm, then refutes.
    (submit! (assert-req :confirms conv-ref bB-ref "sid" :human T (base-opts "cfB")))
    (drain!)
    (submit! (rk/retract-request (merge {:kind :confirms :from conv-ref :to bB-ref
                                         :asserter-actor-id "sid" :asserter-type :human
                                         :asserted-at-ms (+ T 500) :sent-at-ms T
                                         :actor {:actor/id "sid" :actor/type :human}}
                                        {:request-id "cfB-retract" :idempotency-key "cfB-retract-idem"})))
    (drain!)
    (submit! (assert-req :refutes conv-ref bB-ref "sid" :human (+ T 1000) (base-opts "rfB")))
    ;; a supersedes row folding on blockB (keyed on its :to endpoint).
    (submit! (assert-req :supersedes d2-ref bB-ref "editor" :human T (base-opts "spB")))
    (drain!)
    ;; back-dated relation (gate 4): claimed 60 days ago, arrival = today.
    (submit! (assert-req :references d2-ref (tref :git-commit "sha-backdated") "sid" :human
                         (- T sixty-days) (base-opts "bd")))
    (drain!)
    {:harness harness
     :doc1-id doc1-id :doc2-id doc2-id :doc3-id doc3-id
     :okey1 okey1 :d1-src d1-src
     :blockA blockA :blockB blockB :blockA-anchor blockA-anchor :blockA-text blockA-text
     :conv* conv* :msg1-id msg1-id :conv-id conv-id
     :confirms-B-relid (rk/relation-id-for :confirms conv-ref bB-ref "sid")
     :refutes-B-relid (rk/relation-id-for :refutes conv-ref bB-ref "sid")
     :supersedes-B-relid (rk/relation-id-for :supersedes d2-ref bB-ref "editor")
     :based-on-relid (rk/relation-id-for :based-on d1-ref d2-ref "sid")
     :refutes-A-relid (rk/relation-id-for :refutes conv-ref bA-ref "sid")
     :confirms-A-relid (rk/relation-id-for :confirms conv-ref bA-ref "llm:opus-4-8/run-x")
     :backdated-relid (rk/relation-id-for :references d2-ref (tref :git-commit "sha-backdated") "sid")}))

;; ── the read gates ────────────────────────────────────────────────────────────
(deftest trail-view-read-gates-test
  (let [rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})]
    (try
      (let [fx (build-fixture! rt)
            {:keys [doc1-id doc2-id doc3-id okey1 blockA blockB blockA-anchor blockA-text
                    conv* msg1-id]} fx]

        (testing "Gate 1 — bundle completeness: six layers; rows == R1; :in-family block verdicts"
          (let [bundle (tv/read-context-bundle rt [doc1-id conv*] {})
                tb (get-in bundle [:bundle/targets doc1-id])]
            (is (contains? tb :identity))
            (is (contains? tb :material))
            (is (contains? tb :structure))
            (is (contains? tb :relations))
            (is (contains? tb :verdicts))
            (is (contains? (:times tb) :last-attested-ms))
            (is (= :doc (:kind tb)))
            (is (contains? (:bundle/targets bundle) conv*) "conversation target present")
            ;; seam consistency: bundle relation-ids for doc1 == R1 rows for its object-key
            (let [r1 (rk/read-relations-for-targets rt [okey1] nil false)
                  r1-ids (set (map :relation-id (get r1 okey1)))
                  bundle-ids (set (map :relation-id
                                       (concat (apply concat (vals (get-in tb [:relations :this])))
                                               (mapcat #(apply concat (vals %))
                                                       (vals (get-in tb [:relations :in-family]))))))]
              (is (= r1-ids bundle-ids) "every bundle relation equals an R1 row (seam)"))
            ;; :in-family carries the block verdicts on blockA
            (let [infam (get-in tb [:relations :in-family blockA])]
              (is (some? infam) "blockA verdicts roll up under :in-family")
              (is (contains? infam :refutes))
              (is (contains? infam :confirms)))))

        (testing "Gate 2 — address round-trip (bundle / trail)"
          (let [bundle (tv/read-context-bundle rt [doc1-id] {})
                addr   (:bundle/address bundle)
                again  (tv/resolve-address rt (pr-str addr))]
            (is (= addr (:bundle/address again)) "address is stable across round-trip")
            (is (= (:bundle/targets bundle) (:bundle/targets again))
                "same shape over current truth (modulo rendered-at)"))
          (let [trail (tv/read-conversation-trail rt conv* nil 200)
                again (tv/resolve-address rt (pr-str (:trail/address trail)))]
            (is (= (:trail/entries trail) (:trail/entries again)))))

        (testing "Gate 3 — omissions honesty: returned + omitted == totals per layer; feed :feed/uncovered"
          ;; force relations cap below the doc1 fixture relation count.
          (let [full (tv/read-context-bundle rt [doc1-id] {})
                tb-full (get-in full [:bundle/targets doc1-id])
                total (+ (count (apply concat (vals (get-in tb-full [:relations :this]))))
                         (reduce + (map (fn [m] (count (apply concat (vals m))))
                                        (vals (get-in tb-full [:relations :in-family])))))
                capped (tv/read-context-bundle rt [doc1-id] {:caps {:relations 1}})
                tb (get-in capped [:bundle/targets doc1-id])
                kept (+ (count (apply concat (vals (get-in tb [:relations :this]))))
                        (reduce + (map (fn [m] (count (apply concat (vals m))))
                                       (vals (get-in tb [:relations :in-family])))))
                om (some #(when (= :relations (:layer %)) %) (:omissions tb))]
            (is (> total 1) "fixture has more relations than the forced cap")
            (is (= 1 kept) "cap honoured")
            (is (some? om) "relations omission recorded")
            (is (= total (+ kept (:dropped om))) "returned + omitted == total (exactness)"))
          (let [feed (tv/read-recent-activity rt {:from-ms (- T 3600000) :to-ms (+ T 3600000)} {})]
            (is (seq (:feed/omissions feed)))
            (is (some #(= :feed/uncovered (:omission/kind %)) (:feed/omissions feed))
                "feed exposes its standing :feed/uncovered declaration")))

        (testing "Gate 4 — two clocks (v1.1): back-dated relation in today's arrival window"
          (let [win {:from-ms (- T 3600000) :to-ms (+ T 3600000)}
                feed (tv/read-recent-activity rt win {:order :claimed})
                entries (:feed/entries feed)
                rel-entries (filter #(= :relation-transition (:entry/kind %)) entries)
                bd (some #(when (= (:backdated-relid fx) (get-in % [:entry/detail :relation-id])) %)
                         rel-entries)]
            (is (some? bd) "back-dated relation appears in today's arrival window")
            (is (= (- T sixty-days) (:time/claimed-ms bd)) "claimed-ms is 60 days old")
            (is (= T (:time/arrival-ms bd)) "arrival-ms is today")
            ;; Trunk-5 gate fix (DIFF_FALSIFICATION_CROSS B1): rt entries MUST
            ;; project both lineage endpoints from the activity row — the
            ;; client threads exclusively off detail :from/:to; without them
            ;; the live face is threadless while fixtures stay green.
            (is (every? (fn [e]
                          (and (some? (get-in e [:entry/detail :from :id]))
                               (some? (get-in e [:entry/detail :from :kind]))
                               (contains? (:entry/detail e) :to)
                               (some? (get-in e [:entry/detail :to :kind]))
                               ;; a :dead-end has NO target — its row honestly
                               ;; carries {:id nil :kind :none}; every other
                               ;; kind must carry a real to-id
                               (or (= :dead-end (get-in e [:entry/detail :kind]))
                                   (some? (get-in e [:entry/detail :to :id])))))
                        rel-entries)
                "every relation entry projects BOTH endpoints verbatim ({:id :kind}; dead-end to-id honestly nil)")
            (is (every? #(and (contains? % :time/claimed-ms) (contains? % :time/arrival-ms)) entries)
                "both stamps present on every entry")
            ;; :order :claimed orders by claimed desc → the 60-day-old entry sits after today-claimed
            (let [claimed-seq (map :time/claimed-ms rel-entries)]
              (is (= claimed-seq (reverse (sort (map #(or % Long/MIN_VALUE) claimed-seq))))
                  "entries ordered by claimed clock (desc)"))
            ;; an arrival window covering ONLY 60-days-ago excludes it
            (let [old-win {:from-ms (- (- T sixty-days) 3600000) :to-ms (+ (- T sixty-days) 3600000)}
                  old-feed (tv/read-recent-activity rt old-win {})
                  ids (set (map #(get-in % [:entry/detail :relation-id]) (:feed/entries old-feed)))]
              (is (not (contains? ids (:backdated-relid fx)))
                  "arrival window at 60-days-ago does NOT return the (today-arrival) entry"))
            ;; a claimed-window SELECTION request is refused (§9.10)
            (is (thrown? clojure.lang.ExceptionInfo
                         (tv/read-recent-activity rt win {:select-clock :claimed})))))

        (testing "Gate 5 — evidence anchors resolve: md char-unit + transcript byte-unit"
          (let [bundle (tv/read-context-bundle rt [blockA msg1-id] {})
                tbA (get-in bundle [:bundle/targets blockA])
                md-anchor (some #(when (= blockA-anchor (:anchor-id %)) %)
                                (get-in tbA [:material :anchors]))]
            (is (= :chars (get-in tbA [:material :raw :offset-unit])) "markdown anchor is CHAR-unit")
            (is (some? md-anchor) "the refutes verdict's evidence anchor is in the block bundle")
            (is (str/includes? (subs doc1-raw (:start md-anchor) (:end md-anchor)) blockA-text)
                "md char-unit anchor resolves to a source span containing the expected block text"))
          (let [bundle (tv/read-context-bundle rt [msg1-id] {})
                tbM (get-in bundle [:bundle/targets msg1-id])
                tr-anchor (first (get-in tbM [:material :anchors]))]
            (is (= :bytes (get-in tbM [:material :raw :offset-unit])) "transcript anchor is BYTE-unit")
            (is (some? tr-anchor))
            (is (= 100 (:start tr-anchor)) "byte-offset start resolves (obs byte-offset)")
            (is (= 180 (:end tr-anchor)) "byte-offset end = offset + length")))

        (testing "Gate 6 — disagreement preserved: both asserters current, badged, unmerged"
          (let [bundle (tv/read-context-bundle rt [doc1-id] {})
                tb (get-in bundle [:bundle/targets doc1-id])
                current (get-in tb [:verdicts :current])]
            (is (contains? current "sid"))
            (is (contains? current "llm:opus-4-8/run-x"))
            (is (= #{:refutes} (set (map :kind (get current "sid")))) "sid refutes")
            (is (= #{:confirms} (set (map :kind (get current "llm:opus-4-8/run-x")))) "llm confirms")
            (is (every? :current (get current "sid")))
            (is (every? :current (get current "llm:opus-4-8/run-x")))))

        (testing "Gate 7 — supersession fold: retract confirms + assert refutes → only refutes; history via R2"
          (let [r1 (rk/read-relations-for-targets rt [okey1] nil true) ; include retracted
                rows (get r1 okey1)
                verdicts (tv/current-verdicts rows)
                blockB-sid (filter #(and (= "sid" (:asserted-by %)) (= blockB (get-in % [:to :id]))) verdicts)]
            (is (= [:refutes] (map :kind blockB-sid)) "current shows only refutes for [blockB, sid]")
            (let [blockB-editor (filter #(and (= "editor" (:asserted-by %)) (= blockB (get-in % [:to :id]))) verdicts)]
              (is (= [:supersedes] (map :kind blockB-editor)) "supersedes folds keyed on its :to (blockB)"))
            ;; R2 history of the retracted confirm is reachable
            (let [detail (tv/read-relation-detail rt (:confirms-B-relid fx))]
              (is (= :retracted (:relation-status (:row detail))))
              (is (= [:asserted :retracted] (mapv :relation-status (:history detail)))
                  "history reachable via R2"))
            ;; the retracted row is visible with include-retracted?
            (let [bundle (tv/read-context-bundle rt [blockB] {:include-retracted? true})
                  tb (get-in bundle [:bundle/targets blockB])
                  kinds (set (map :kind (apply concat (vals (get-in tb [:relations :this])))))]
              (is (contains? kinds :confirms) "retracted confirm visible with include-retracted?"))))

        (testing "Gate 8 — attestation staleness: last-attested bumped by re-assert; never-attested nil; walked unknown"
          (let [bundle (tv/read-context-bundle rt [doc1-id doc3-id] {})
                tb1 (get-in bundle [:bundle/targets doc1-id])
                tb3 (get-in bundle [:bundle/targets doc3-id])]
            (is (= (+ T 1000) (get-in tb1 [:times :last-attested-ms]))
                "re-asserting based-on bumped doc1 last-attested")
            (is (nil? (get-in tb3 [:times :last-attested-ms])) "never-attested target shows nil")
            (is (nil? (get-in tb1 [:times :last-walked-ms])) "last-walked always nil (WP1)")
            (is (str/includes? (tv/render-bundle-text bundle) "walked unknown"))))

        (testing "Gate 11 — conversation trail: byte-offset order; page cursor; re-import stable"
          (let [trail (tv/read-conversation-trail rt conv* nil 200)
                entries (:trail/entries trail)
                order-keys (map :order-key entries)]
            (is (>= (count entries) 4) "all four messages present")
            (is (= order-keys (sort order-keys)) "ordered by byte-offset order-key")
            ;; page cursor: first page of 1 then resume
            (let [p1 (tv/read-conversation-trail rt conv* nil 1)
                  cur (:trail/next-cursor p1)
                  p2 (tv/read-conversation-trail rt conv* cur 200)]
              (is (= 1 (count (:trail/entries p1))))
              (is (some? cur) "cursor exposed when a page is full")
              (is (not (contains? (set (map :order-key (:trail/entries p2))) (:order-key (first (:trail/entries p1))))))
              (is (= (dec (count entries)) (count (:trail/entries p2))) "resume returns the remainder"))
            ;; convergent re-import of the same observation leaves order + count unchanged
            (let [before (count entries)
                  dup (tr/transcript-observation-import-request
                       (tobs (:conv-id fx) 1 "m1" "assistant"
                             [{:type "text" :text "Inspecting the kernel."}
                              {:type "tool_use" :id "tu-1" :name "Read" :input {:file "relation_kernel.clj"}}])
                       {:request/id "tv-tr1-again" :time-ms 10000})
                  _ (oc-ingest! rt dup)
                  after-entries (:trail/entries (tv/read-conversation-trail rt conv* nil 200))]
              (is (= before (count after-entries)) "re-import leaves count unchanged")
              (is (= (map :order-key entries) (map :order-key after-entries))
                  "re-import leaves order unchanged"))))

        (testing "Gate 12 — view-spec resolution: :latest tracks newest source version; pinned :rev holds"
          ;; A view-spec is an ordinary ingested EDN doc. Re-ingest the SAME source-ref
          ;; with new content → a new source version; :latest moves, a pinned
          ;; source-version-key (= source-hash) stays put (F-3).
          (let [spec-ref "trail/view-spec.md"
                v1-content (pr-str {:spec/name "doc1-view" :spec/query 'trail/context-bundle
                                    :spec/params {:targets [doc1-id]}})
                v2-content (pr-str {:spec/name "doc1-view" :spec/query 'trail/context-bundle
                                    :spec/params {:targets [doc2-id]}})
                v1 (oc-ingest! rt (md/source-ingest-request v1-content spec-ref
                                                            {:request/id "spec-v1" :time-ms 1000}))
                v1-hash (get-in v1 [:payload :source-hash])
                v1-doc (oc/document-id-for-object-key (:object/key v1))
                _ (oc-ingest! rt (md/source-ingest-request v2-content spec-ref
                                                           {:request/id "spec-v2" :time-ms 2000}))
                latest (tv/resolve-via rt {:spec v1-doc :rev :latest})
                pinned (tv/resolve-via rt {:spec v1-doc :rev v1-hash})]
            ;; :latest resolved v2's params → targets doc2
            (is (contains? (:bundle/targets latest) doc2-id) ":latest reflects the newest spec version")
            ;; pinned :rev resolved v1's params → targets doc1
            (is (contains? (:bundle/targets pinned) doc1-id) "pinned :rev keeps resolving to the old params")))

        (testing "Gate 13 — text projection: all §8 markers, address header, ≤ 4000 chars"
          (let [bundle (tv/read-context-bundle rt [doc1-id] {})
                text (tv/render-bundle-text bundle)]
            (is (str/starts-with? text ";; trail-text v0 @"))
            (is (str/includes? text ";; address: (trail/context-bundle"))
            (is (str/includes? text (str "== " doc1-id)))
            (is (str/includes? text "-- material:"))
            (is (str/includes? text "-- relations:"))
            (is (str/includes? text "-- verdicts:"))
            (is (str/includes? text "-- omissions:"))
            (is (str/includes? text "walked unknown"))
            (is (<= (count text) 4000) "within the §8 budget")))

        (testing "Gate 14 — read-only by construction: no depots, no ETL, no writes in source"
          (let [src (slurp "src/app/server/rama/trail_view.clj")]
            (is (not (re-find #"foreign-append!" src)) "no foreign-append! in the module source")
            (is (not (re-find #"local-transform>" src)) "no local-transform> in the module source")
            (is (not (re-find #"declare-depot" src)) "no depots declared")
            (is (not (re-find #"stream-topology|microbatch-topology" src)) "no ETL topologies"))
          ;; a foreign-depot lookup on trail-view-module fails (it declares none)
          (is (thrown? Exception
                       (com.rpl.rama/foreign-depot (:ipc rt) (:trail-view-module-name rt) "*any-depot"))
              "trail-view declares no foreign-appendable depot"))

        ;; Gate-completing addition by the GATE REVIEWER (2026-07-05, test-only):
        ;; gate 10's trail-view half had no executed assertion — Phase A proved
        ;; custody on the ROWS; this block proves the bundle/text PROJECTION.
        ;; Runs AFTER gate 8 (sequential blocks), so the new doc3 relation cannot
        ;; disturb the never-attested-nil assertion above.
        (testing "Gate 10 (trail-view half) — custody projection: :written-by on divergence only"
          (let [{:keys [submit! drain!]} (:harness fx)]
            ;; divergent custody: payload asserter sid, envelope actor = the agent (trap 10)
            (submit! (assert-req :references (tref :container doc3-id) (tref :container doc2-id)
                                 "sid" :human T
                                 {:request-id "cu-req" :idempotency-key "cu-idem"
                                  :actor {:actor/id "agent:claude-code/session-x" :actor/type :llm}}))
            (drain!)
            (let [bundle (tv/read-context-bundle rt [doc3-id] {})
                  tb (get-in bundle [:bundle/targets doc3-id])
                  cu (some #(when (= :references (:kind %)) %)
                           (apply concat (vals (get-in tb [:relations :this]))))]
              (is (some? cu) "divergent-custody fixture edge present in the bundle")
              (is (= "agent:claude-code/session-x" (:written-by cu))
                  "bundle edge shows :written-by when envelope actor differs from asserter")
              (is (str/includes? (tv/render-bundle-text bundle) "(via agent:claude-code/session-x)")
                  "text projection badges 'via <writer>' on divergence (§8)"))
            ;; convergent custody: the based-on edge was asserted with NO envelope actor
            ;; (writer defaults to asserter) → no :written-by value, no via-badge.
            (let [bundle (tv/read-context-bundle rt [doc1-id] {})
                  tb (get-in bundle [:bundle/targets doc1-id])
                  bo (some #(when (= :based-on (:kind %)) %)
                           (apply concat (vals (get-in tb [:relations :this]))))]
              (is (some? bo) "convergent based-on edge present")
              (is (nil? (:written-by bo)) "no :written-by value when writer == asserter")
              (is (not (str/includes? (tv/render-bundle-text bundle) "(via "))
                  "no via-badge anywhere in a fully convergent bundle's text")))))
      (finally
        (tv/close-trail-view-runtime! rt)))))

;; ── G9 (git-spine WP2 Phase P3, display names / F-L3) ────────────────────────
;; ADDITIVE to the gate-passed trail-view suite. CONTRACT §3.D + gate G9:
;; md/doc/file feed entries carry a basename display-name; commit FEED entries
;; carry <sha7> derived from the "git-commit:<sha>" source-ref; the View-3 BUNDLE
;; rendering (which reads content) shows "<sha7> · <subject>" by parsing the §3.A
;; subject: line; the raw tid/hash stays the honest fallback where a name is absent.
;; The commit fixture is a SYNTHETIC source-ref flowing the EXISTING md-adapter
;; path (contract-validated R1/R2) — it does NOT require git_spine.

(def ^:private p3-sha "3f9a1c2b4d5e6f708192a3b4c5d6e7f809a1b2c3")   ; 40-hex
(def ^:private p3-sha7 "3f9a1c2")
(def ^:private p3-subject "Add the relation kernel")

(defn ^:private p3-commit-body
  "A §3.A labeled canonical-commit text (CONTRACT §3.A cross-builder interface):
   labeled header lines in fixed order, blank, body, blank, files:."
  [sha subject]
  (str "sha: " sha "\n"
       "parents: 0000000000000000000000000000000000000000\n"
       "author: Sid <sid@example.com>\n"
       "authored-at: 2026-07-05T10:00:00Z\n"
       "committed-at: 2026-07-05T10:00:00Z\n"
       "subject: " subject "\n"
       "\n"
       "Body line one.\nBody line two.\n"
       "\n"
       "files:\n"
       "src/app/server/rama/relation_kernel.clj"))

(deftest g9-display-name-helpers-test
  (testing "G9 — basename (md/doc/file entries)"
    (is (= "doc1.md" (tv/basename "trail/doc1.md")))
    (is (= "conv.jsonl" (tv/basename "/tmp/sessions/conv.jsonl")))
    (is (= "bare.md" (tv/basename "bare.md")))
    (is (nil? (tv/basename nil)))
    (is (nil? (tv/basename "")) "blank -> nil, so the id stays the fallback (never a \"\" title)"))
  (testing "G9 — commit source-ref classification + sha7"
    (let [ref (str "git-commit:" p3-sha)]
      (is (tv/commit-source-ref? ref))
      (is (not (tv/commit-source-ref? "trail/doc1.md")))
      (is (not (tv/commit-source-ref? nil)))
      (is (= p3-sha (tv/source-ref->sha ref)))
      (is (nil? (tv/source-ref->sha "trail/doc1.md")))
      (is (= p3-sha7 (tv/sha7 p3-sha)))
      (is (= "abc" (tv/sha7 "abc")) "sha7 tolerates a short sha")
      (is (nil? (tv/sha7 "")) "blank sha -> nil (a degenerate \"git-commit:\" ref cannot blank a title)")
      (is (nil? (tv/sha7 nil)))))
  (testing "G9 — feed display-name: md → basename, commit → sha7"
    (is (= "readme.md" (tv/source-ref->display-name "spine/readme.md")))
    (is (= p3-sha7 (tv/source-ref->display-name (str "git-commit:" p3-sha))))
    (is (nil? (tv/source-ref->display-name nil))))
  (testing "G9 — §3.A subject: line parse"
    (is (= p3-subject (tv/subject-line (p3-commit-body p3-sha p3-subject))))
    (is (nil? (tv/subject-line "no header here\njust some body text"))))
  (testing "G9 — bundle display-name: commit → sha7 · subject; md → basename"
    (let [commit-tb {:material {:raw {:source-ref (str "git-commit:" p3-sha)}
                                :content-text (p3-commit-body p3-sha p3-subject)}}
          md-tb     {:material {:raw {:source-ref "spine/readme.md"}
                                :content-text "# Readme\nbody"}}
          bare-tb   {:material {:raw {:source-ref nil}}}]
      (is (= (str p3-sha7 " · " p3-subject) (tv/bundle-display-name commit-tb))
          "commit bundle name is <sha7> · <subject>")
      (is (str/includes? (tv/bundle-display-name commit-tb) p3-sha7))
      (is (str/includes? (tv/bundle-display-name commit-tb) p3-subject))
      (is (= "readme.md" (tv/bundle-display-name md-tb)))
      (is (nil? (tv/bundle-display-name bare-tb)) "no source-ref → nil (tid is the fallback)"))))

(deftest g9-feed-and-view3-names-test
  (let [rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})]
    (try
      (let [commit-ref (str "git-commit:" p3-sha)
            commit-body (p3-commit-body p3-sha p3-subject)
            ;; :claimed/at-ms mirrors git_spine's commit->import-request
            ;; (t4-spine seam 1): a request that genuinely carries a
            ;; material-claimed clock declares it EXPLICITLY; the md request
            ;; below declares none, so its feed row must stay claimed-nil.
            p3-claimed 1751702400000
            c  (oc-ingest! rt (assoc (md/source-ingest-request commit-body commit-ref
                                                               {:request/id "p3-commit" :time-ms 1000})
                                     :claimed/at-ms p3-claimed))
            m  (oc-ingest! rt (md/source-ingest-request "# Readme\nsome supporting text"
                                                        "spine/readme.md"
                                                        {:request/id "p3-md" :time-ms 1000}))
            commit-doc-id (oc/document-id-for-object-key (:object/key c))
            md-doc-id     (oc/document-id-for-object-key (:object/key m))]

        (testing "G9 — feed: md entry carries basename; commit entry carries sha7"
          ;; source-ingested arrival = wall clock (core/now-ms) → window covers now.
          (let [now  (System/currentTimeMillis)
                win  {:from-ms (- now (* 7 86400000)) :to-ms (+ now 3600000)}
                feed (tv/read-recent-activity rt win {})
                src  (filter #(= :source-ingested (:entry/kind %)) (:feed/entries feed))
                by-id (into {} (map (juxt #(get-in % [:entry/target :id]) identity)) src)
                commit-entry (get by-id commit-doc-id)
                md-entry     (get by-id md-doc-id)]
            (is (some? commit-entry) "commit source-ingested entry present in the feed")
            (is (some? md-entry) "md source-ingested entry present in the feed")
            (is (= p3-sha7 (get-in commit-entry [:entry/target :display-name]))
                "commit FEED entry carries <sha7> derived from source-ref (G9)")
            (is (= "readme.md" (get-in md-entry [:entry/target :display-name]))
                "md FEED entry carries basename display-name (G9)")
            ;; t4-spine seam 1 — the two-clock stamp at the feed seam:
            (is (= p3-claimed (:time/claimed-ms commit-entry))
                "commit entry claimed-ms = the request's declared :claimed/at-ms")
            (is (nil? (:time/claimed-ms md-entry))
                "md entry stays claimed-nil (its :request/time-ms is never a claim)")
            (is (some? (:time/arrival-ms commit-entry)) "arrival clock untouched")))

        (testing "G9 — View-3 bundle rendering shows <sha7> · <subject> for commit material"
          (let [bundle (tv/read-context-bundle rt [commit-doc-id] {})
                tb   (get-in bundle [:bundle/targets commit-doc-id])
                text (tv/render-bundle-text bundle)]
            (is (= (str p3-sha7 " · " p3-subject) (tv/bundle-display-name tb))
                "bundle display-name reads the §3.A subject: line of the content-text")
            (is (str/includes? text (str p3-sha7 " · " p3-subject))
                "View-3 text shows <sha7> · <subject> (G9)")
            ;; hash stays the honest fallback: the raw tid heading is still present.
            (is (str/includes? text (str "== " commit-doc-id))
                "raw tid remains as the honest hash fallback in the heading")))

        (testing "G9 — View-3 shows names not hashes when display-names exist"
          (let [bundle (tv/read-context-bundle rt [md-doc-id] {})
                tb   (get-in bundle [:bundle/targets md-doc-id])
                text (tv/render-bundle-text bundle)]
            (is (= "readme.md" (tv/bundle-display-name tb)) "md bundle display-name = basename")
            ;; the §8 display-name slot now shows the NAME, not the container-kind.
            (is (str/includes? text "\"readme.md\"")
                "View-3 display-name slot shows the basename (name)")
            (is (not (str/includes? text "\"document\""))
                "the container-kind no longer occupies the display-name slot when a name exists")
            ;; and the honest hash fallback (raw tid) is still present in the heading.
            (is (str/includes? text (str "== " md-doc-id))))))
      (finally
        (tv/close-trail-view-runtime! rt)))))

;; ── REVIEWER-AUTHORED final-phase gate (HQ/Fable, 2026-07-05) ─────────────
;; G11 (git-spine CONTRACT v1.2): with one conversation→commit :produced join
;; asserted, the View-3 text projection shows the commit and the producing
;; session in ONE rendered context — text-level, pixels not required.
;; WP1-gate precedent for test-only blocks at gate.
(deftest g11-first-thread-commit-and-session-in-one-context
  (let [rt (tv/start-trail-view-runtime! {:tasks 2 :threads 2})]
    (try
      (let [{:keys [submit! drain!]} (rel-harness rt)
            commit-ref-str (str "git-commit:" p3-sha)
            c (oc-ingest! rt (md/source-ingest-request
                              (p3-commit-body p3-sha p3-subject)
                              commit-ref-str
                              {:request/id "g11-commit" :time-ms 1000}))
            commit-doc-id (oc/document-id-for-object-key (:object/key c))
            from-ref (rk/->target-ref :conversation
                                      "oc:chat-conversation:chat:g11-session")
            to-ref   (rk/->target-ref :container commit-doc-id)]
        (submit! (assert-req :produced from-ref to-ref "import:git-spine" :import 2000
                             {:request-id "g11-req" :idempotency-key "g11-idem"
                              :note "spine-v1|sha-verified"
                              :evidence-source-id "transcript:g11-session"
                              :evidence-anchor-id "g11-uuid"}))
        (drain!)
        (let [bundle (tv/read-context-bundle rt [commit-doc-id] {})
              text   (tv/render-bundle-text bundle)]
          (is (str/includes? text p3-sha7)
              "the commit (sha7 display-name) renders in the context")
          (is (str/includes? text "g11-session")
              "the producing session appears in the SAME rendered context")
          (is (str/includes? text "produced")
              "joined via :produced — one thread, text-level (G11)")))
      (finally
        (tv/close-trail-view-runtime! rt)))))
