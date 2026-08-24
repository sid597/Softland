(ns app.machine-cut-serve-test
  "Machine-cut Lane B — the serve path (CONTRACT §§4.4, 6).
   Gates G4 (grouping pure core), G10 (foreign-asserter isolation, IPC),
   G12 (serve totality), plus the fixture↔pure-function parity pin.

   Run:
     clj -M:test -e \"(require 'app.machine-cut-serve-test)
                      (clojure.test/run-tests 'app.machine-cut-serve-test)\"

   The pure gates (G4/G12-pure + parity) need NO cluster. G10 and
   the G12 read-throw path boot ONE in-process relation-kernel cluster and seed
   :pairs-with edges BY HAND (rk/assert-request + append-relation-request!, no
   driver dependency) with a materialized-read barrier — microbatch :append-ack
   does NOT imply PState visibility (rama-pitfalls §6; LANES platform duty)."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container.transcript-identity :as tid]))

;; ===========================================================================
;; Synthetic turns + edges (pure fixtures). Turn :id = the river event-uuid;
;; block :id = unit-id (trap T6). An edge's endpoints address the events via
;; (tid/chat-message-id address event-uuid) — exactly the CONTRACT §4.2 form.
;; ===========================================================================

(def actor       fp/machine-cut-actor-v0)   ;; "llm:machine-cut/v1"
(def other-actor "sid")                      ;; a foreign (human) asserter
(def g4-address  "chat:g4test")

(def t1 {:id "t1" :speaker "human:sid"    :order 0
         :blocks [{:id "t1b1" :kind "human-message" :order 1 :time-ms 100 :text "question one, please"}]})
(def t2 {:id "t2" :speaker "model:claude" :order 1
         :blocks [{:id "t2b1" :kind "text" :order 1 :time-ms 200 :text "the answer to question one is here"}]})
(def t3 {:id "t3" :speaker "human:sid"    :order 2
         :blocks [{:id "t3b1" :kind "human-message" :order 1 :time-ms 300 :text "a second question, unanswered"}]})
(def g4-turns [t1 t2 t3])

(defn mk-edge
  "A RelationEdgeRow-shaped map (from = response event, to = prompt event) as the
   R1 query would surface it. Endpoint refs use the real rk/->target-ref, so
   target-ids and the derived relation-id match a live-seeded edge exactly."
  [address resp-uuid prompt-uuid an-actor]
  (let [from (rk/->target-ref :container (tid/chat-message-id address resp-uuid))
        to   (rk/->target-ref :container (tid/chat-message-id address prompt-uuid))
        rid  (rk/relation-id-for :pairs-with from to an-actor)]
    {:relation-id       rid
     :relation-kind     :pairs-with
     :from              from
     :to                to
     :asserter-actor-id an-actor
     :asserter-type     (if (= an-actor other-actor) :human :llm)
     :relation-status   :asserted}))

(defn- turn-ids-seen
  "Every turn id the structure claims — as prompt, response, or unpaired."
  [{:keys [pairs unpaired]}]
  (concat (map :id unpaired)
          (mapcat (fn [p] (cons (:id (:user-turn p)) (map :id (:responses p)))) pairs)))

;; ===========================================================================
;; G4 — grouping pure core (CONTRACT §6; MC-T10/T11/T14)
;; ===========================================================================

(deftest g4-basic-pair-and-totality
  (testing "one machine-cut edge → one pair; every served turn appears exactly once"
    (let [edge (mk-edge g4-address "t2" "t1" actor)
          out  (fp/derive-pair-structure g4-turns [edge] g4-address actor)
          st   (:conversation/structure out)]
      (is (= 1 (count (:pairs out))))
      (is (= "t1" (:id (first (:pairs out)))))
      (is (= t1 (:user-turn (first (:pairs out)))) ":user-turn is the served prompt turn verbatim")
      (is (= [t2] (:responses (first (:pairs out)))) ":responses is the served response turn")
      (is (= actor (:asserted-by (first (:pairs out)))))
      (is (= [t3] (:unpaired out)) "the unanswered prompt is unpaired")
      (let [seen (turn-ids-seen out)]
        (is (= (sort ["t1" "t2" "t3"]) (sort seen)) "every served turn appears exactly once")
        (is (apply distinct? seen) "no turn appears twice (totality)"))
      (is (= {:source :machine-cut :asserter actor :pairs-count 1 :unpaired-count 1
              :edges-read 1 :conflicts 0 :foreign-asserter-edges 0} st))
      (is (= "machine-cut v1 · 1 pairs · 1 unpaired" (:conversation/structure-line out))))))

(deftest g4-mc-t8-additive-keys-only
  (testing "derive returns ONLY the four additive keys — never :turns (MC-T8)"
    (is (= #{:pairs :unpaired :conversation/structure :conversation/structure-line}
           (set (keys (fp/derive-pair-structure g4-turns
                                                [(mk-edge g4-address "t2" "t1" actor)]
                                                g4-address actor)))))))

(deftest g4-mc-t14-both-copies-dedup
  (testing "both endpoint copies of ONE edge (same relation-id) → one pair, edges-read 1"
    (let [edge (mk-edge g4-address "t2" "t1" actor)
          ;; the o:/i: sort-key copies are byte-identical rows under the one key
          out  (fp/derive-pair-structure g4-turns [edge edge] g4-address actor)]
      (is (= 1 (count (:pairs out))) "dedup by relation-id before grouping (MC-T14)")
      (is (= [t2] (:responses (first (:pairs out)))) "the response is not double-listed")
      (is (= 1 (:edges-read (:conversation/structure out))) "edges-read counts distinct relation-ids"))))

(deftest g4-mc-t10-foreign-counted-never-merged
  (testing "a foreign asserter's :pairs-with edge is COUNTED, never merged (MC-T10)"
    (let [mc      (mk-edge g4-address "t2" "t1" actor)
          foreign (mk-edge g4-address "t2" "t1" other-actor)
          out     (fp/derive-pair-structure g4-turns [mc foreign] g4-address actor)
          st      (:conversation/structure out)]
      (is (not= (:relation-id mc) (:relation-id foreign))
          "asserter is part of identity (rk §3) → distinct relation-ids")
      (is (= 1 (count (:pairs out))) "only the machine-cut pair is served")
      (is (= actor (:asserted-by (first (:pairs out)))) "the served pair is machine-cut's, never sid's")
      (is (= 1 (:foreign-asserter-edges st)) "the foreign edge is counted")
      (is (= 1 (:edges-read st)) "edges-read excludes the foreign edge")
      (is (= :machine-cut (:source st))))))

(deftest g4-conflict-deterministic-pick
  (testing "one response with two prompts → earliest-river prompt wins, +1 conflict"
    (let [e1  (mk-edge g4-address "t2" "t1" actor)   ;; t2 responds to t1
          e2  (mk-edge g4-address "t2" "t3" actor)   ;; t2 ALSO responds to t3 (conflict)
          out (fp/derive-pair-structure g4-turns [e1 e2] g4-address actor)
          st  (:conversation/structure out)
          by  (into {} (map (juxt :id identity)) (:pairs out))]
      (is (= 2 (:pairs-count st)) "both prompts head a pair")
      (is (= 1 (:conflicts st)) "one response with two prompts → one conflict")
      (is (= [t2] (:responses (by "t1"))) "the river-order-earliest prompt (t1, order 0) wins the response")
      (is (= [] (:responses (by "t3"))) "the later prompt (t3) keeps an empty :responses")
      (let [seen (turn-ids-seen out)]
        (is (apply distinct? seen) "totality — the conflicted response is claimed once")
        (is (empty? (:unpaired out)) "t1/t3 are heads, t2 claimed → nothing unpaired")))))

(deftest g4-until-ms-cut-prompt-strands-responses
  (testing "MC-T11: a prompt cut by until-ms forms NO pair; its response strands into :unpaired"
    (let [post-cut [t2 t3]                              ;; t1 (the prompt) was cut
          edge     (mk-edge g4-address "t2" "t1" actor)
          out      (fp/derive-pair-structure post-cut [edge] g4-address actor)]
      (is (empty? (:pairs out)) "no pair without a served prompt")
      (is (= ["t2" "t3"] (map :id (:unpaired out))) "the stranded response falls to :unpaired")
      (is (= :none (:source (:conversation/structure out))))
      (is (= "no machine cut" (:conversation/structure-line out))))))

(deftest g4-until-ms-cut-responses-empty
  (testing "MC-T11: a served prompt whose responses are all cut keeps an empty :responses"
    (let [post-cut [t1 t3]                              ;; t2 (the response) was cut
          edge     (mk-edge g4-address "t2" "t1" actor)
          out      (fp/derive-pair-structure post-cut [edge] g4-address actor)
          p        (first (:pairs out))]
      (is (= 1 (count (:pairs out))) "the served prompt still heads its pair")
      (is (= "t1" (:id p)))
      (is (= [] (:responses p)) "all responses cut → empty :responses (not dropped)")
      (is (= ["t3"] (map :id (:unpaired out)))))))

(deftest g4-composition-cut-then-group
  (testing "the real projection order: blocks->turns → apply-until-ms → derive"
    (let [blocks [{:event-uuid "t1" :actor "human:sid" :unit-id "t1b1" :form :human-message
                   :text "q1" :order [1 0 "a"] :time-ms 100 :source-id "s1"}
                  {:event-uuid "t2" :actor "model:claude" :unit-id "t2b1" :form :text
                   :text "a1" :order [2 0 "a"] :time-ms 200 :source-id "s2"}
                  {:event-uuid "t3" :actor "human:sid" :unit-id "t3b1" :form :human-message
                   :text "q2" :order [3 0 "a"] :time-ms 300 :source-id "s3"}]
          turns (fp/apply-until-ms (fp/blocks->turns blocks) 250)   ;; keep t1,t2; cut t3
          edge  (mk-edge g4-address "t2" "t1" actor)
          out   (fp/derive-pair-structure turns [edge] g4-address actor)]
      (is (= ["t1" "t2"] (map :id turns)) "the cut kept the t1⊃t2 prefix")
      (is (= 1 (count (:pairs out))) "the surviving prompt/response form a pair")
      (is (= ["t2"] (map :id (:responses (first (:pairs out))))))
      (is (empty? (:unpaired out)) "t3 cut → nothing unpaired in the surviving prefix"))))

;; ===========================================================================
;; G12 — serve totality (CONTRACT §6; MC-T12): absent rk-rt / no edges / read
;; throw → honest :none, never a throw.
;; ===========================================================================

(deftest g12-absent-rk-rt-none
  (testing "absent rk-rt → [] edges → :none, with every turn unpaired (totality)"
    (is (= [] (fp/read-machine-cut-edges nil g4-address)) "absent rk-rt → [], never a throw")
    (let [out (fp/derive-pair-structure g4-turns [] g4-address actor)]
      (is (= [] (:pairs out)))
      (is (= :none (:source (:conversation/structure out))))
      (is (= "no machine cut" (:conversation/structure-line out)))
      (is (= (map :id g4-turns) (map :id (:unpaired out))) "no edges → every served turn unpaired")
      (let [seen (turn-ids-seen out)]
        (is (= (sort (map :id g4-turns)) (sort seen)) "totality preserved with zero edges")))))

(deftest g12-read-failure-degrades-to-empty
  (testing "MC-T12: a poisoned rk-rt handle → the read catches and yields [], never a throw"
    (is (= [] (fp/read-machine-cut-edges {:relations-for-targets-query :not-a-query} g4-address)))
    (is (= [] (fp/read-machine-cut-edges {} g4-address))
        "a runtime missing the query handle also degrades honestly")))

(deftest g12-nil-address-none
  (testing "a nil address (no conversation) yields [] and :none, total"
    (is (= [] (fp/read-machine-cut-edges nil nil)))
    (is (= [] (fp/read-machine-cut-edges {:relations-for-targets-query :x} nil)))))

(deftest g12-mc-t8-turns-byte-identical-under-merge
  (testing "the additive merge never disturbs :turns or any existing data-context key"
    (let [base      {:turns [t1 t2 t3]
                     :conversation/address g4-address
                     :conversation/river-events-total 247
                     :conversation/truncated? true
                     :face/rendered-at-ms 0}
          structure (fp/derive-pair-structure (:turns base)
                                              [(mk-edge g4-address "t2" "t1" actor)]
                                              g4-address actor)
          merged    (merge base structure)]
      (is (= (:turns base) (:turns merged)) ":turns byte-identical after merge (MC-T8)")
      (is (= (dissoc base :turns)
             (select-keys merged (keys (dissoc base :turns))))
          "every pre-existing key is unchanged")
      (is (contains? merged :pairs))
      (is (contains? merged :conversation/structure-line)))))

(deftest g12-none-structure-shape
  (testing "the none-structure landing value is well-formed and additive-only"
    (is (= #{:pairs :unpaired :conversation/structure :conversation/structure-line}
           (set (keys fp/none-structure))))
    (is (= :none (:source (:conversation/structure fp/none-structure))))
    (is (= [] (:pairs fp/none-structure)))
    (is (= "no machine cut" (:conversation/structure-line fp/none-structure)))))

;; ===========================================================================
;; G10 — foreign-asserter isolation over the REAL relation-kernel read route
;; (IPC; §4.4 one conversation-key call). Seeds edges BY HAND, awaits
;; materialization, reads via fp/read-machine-cut-edges, derives, asserts.
;; ===========================================================================

(defn- seed-edge!
  [rt address resp-uuid prompt-uuid an-actor an-actor-type]
  (let [from (rk/->target-ref :container (tid/chat-message-id address resp-uuid))
        to   (rk/->target-ref :container (tid/chat-message-id address prompt-uuid))
        rid  (rk/relation-id-for :pairs-with from to an-actor)
        req  (rk/assert-request
              {:kind :pairs-with :from from :to to
               :asserter-actor-id an-actor :asserter-type an-actor-type
               :asserted-at-ms 1000
               :request-id (str "mc:" rid)
               :idempotency-key (str "mc:" rid)})]
    (rk/append-relation-request! rt req)))

(deftest g10-foreign-asserter-isolation-ipc
  (testing "a hand-seeded machine-cut edge + a foreign sid edge on the same events →
            the projection serves machine-cut pairs ONLY, counts the foreign edge,
            merges nothing (MC-T10); §4.2 both copies colocate under the conv key"
    (let [rt (rk/start-relation-runtime!)]
      (try
        (let [address "chat:g10test"]
          (seed-edge! rt address "t2" "t1" actor :llm)
          (seed-edge! rt address "t2" "t1" other-actor :human)
          ;; materialized-read barrier — :append-ack ≠ PState visibility
          (rk/await-relation
           (fn [] (fp/read-machine-cut-edges rt address))
           (fn [rows] (>= (count rows) 2))
           6000)
          (let [result (rk/read-relations-for-targets rt [address] [:pairs-with] false)
                edges  (fp/read-machine-cut-edges rt address)
                out    (fp/derive-pair-structure [t1 t2 t3] edges address actor)
                st     (:conversation/structure out)]
            (is (contains? result address)
                "§4.2: both endpoint copies colocate under the conversation object-key")
            (is (= 2 (count edges)) "both asserters' edges read via the ONE conversation-key call")
            (is (= 2 (count (distinct (map :relation-id edges))))
                "distinct relation-ids — asserter is part of identity")
            (is (= 1 (count (:pairs out))) "the projection serves the machine-cut pair only")
            (is (= actor (:asserted-by (first (:pairs out)))) "machine-cut's pair, never sid's")
            (is (= ["t2"] (map :id (:responses (first (:pairs out))))))
            (is (= 1 (:foreign-asserter-edges st)) "the foreign sid edge is COUNTED (MC-T10)")
            (is (= 1 (:edges-read st)) "edges-read is the machine-cut count only")
            (is (= :machine-cut (:source st)))))
        (finally
          (rk/close-relation-runtime! rt))))))

(defn- read-fixture [rel]
  (edn/read-string (slurp (or (io/resource (str "app/fixtures/faces/" rel))
                              (str "test/app/fixtures/faces/" rel)))))
(def paired-conversation (delay (read-fixture "machine-cut-paired-conversation.edn")))

(deftest g4-fixture-parity
  (testing "the golden fixture's pair structure IS derive-pair-structure's output (no drift)"
    (let [conv    @paired-conversation
          address (:conversation/address conv)
          edge    (mk-edge address "t-claude-1" "t-sid-1" actor)
          out     (fp/derive-pair-structure (:turns conv) [edge] address actor)]
      (is (= (:pairs conv) (:pairs out)))
      (is (= (:unpaired conv) (:unpaired out)))
      (is (= (:conversation/structure conv) (:conversation/structure out)))
      (is (= (:conversation/structure-line conv) (:conversation/structure-line out))))))
