(ns app.server.episode-test
  "first-light A P2 — pure units for the episode seam.

   The §5.1 kernel deliverable's G18-family routing unit (the thrice-fired
   foreign-read latent class) + the utterance import's validity, determinism
   (retry-idempotence), grain parity, and the flag-D skip predicate. The
   DURABLE halves (G3/G4) drill against the live cluster, not here."
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.episode :as ep]
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
