(ns app.client.workspace.block-edit-test
  "Unit tests for the edit identity, envelope, and semantic key transition
   shared by the real ground editor."
  (:require [clojure.test :refer [deftest testing is]]
            [app.client.workspace.block-edit :as be]))

(def block
  {:id "oc:unit:conv1:7"
   :document-container-id "oc:chat-message:conv1:abc123"})
(def object-key "oc:chat-conversation:conv1")

;; ---------------------------------------------------------------------------
;; Envelope law (CONTRACT §3)
;; ---------------------------------------------------------------------------

(deftest op-id-determinism
  (testing "request-id is a deterministic fn of (client-id, seq) — BW-T5"
    (is (= "edit:C:0" (be/request-id "edit:C" 0)))
    (is (= (be/request-id "edit:C" 3) (be/request-id "edit:C" 3)))
    (is (not= (be/request-id "edit:C" 3) (be/request-id "edit:C" 4))
        "different keystrokes get distinct ids"))
  (testing "idempotency-key reuses the kernel builder's default shape VERBATIM
            (object_container.clj:1680-1682) — BW-T5"
    (let [rid (be/request-id "edit:C" 2)]
      (is (= (str "object/edit:" object-key ":" (:id block) ":" rid)
             (be/idempotency-key object-key (:id block) rid)))
      (is (= (be/idempotency-key object-key (:id block) rid)
             (be/idempotency-key object-key (:id block) rid))
          "same request replays to a byte-identical key → journaled no-op"))))

(deftest envelope-shape
  (let [env (be/mint-envelope {:block block :object-key object-key
                               :content-text "hello" :edit-client-id "edit:C"
                               :edit-seq 5 :actor be/sid-actor})]
    (testing "target = derived-unit + unit-id FOREVER (BW-T7)"
      (is (= {:target/kind :derived-unit :target/id (:id block)} (:target env))))
    (testing "document-container-id copied VERBATIM off the block (PHASE_0 / BW-T7)"
      (is (= (:document-container-id block)
             (get-in env [:payload :document-container-id]))))
    (testing "object-key = the face address (partition key, §4)"
      (is (= object-key (get-in env [:payload :object-key]))))
    (testing "payload carries content-text ONLY — client never hashes (BW-T5)"
      (is (= "hello" (get-in env [:payload :content-text])))
      (is (not (contains? (:payload env) :content-hash))))
    (testing "actor carries :object/edit (real capability check exercised)"
      (is (contains? (:actor/capabilities (:actor env)) :object/edit))
      (is (not= :system (:actor/type (:actor env)))))
    (testing "request-id + idempotency-key are the deterministic pair"
      (is (= "edit:C:5" (:request-id env)))
      (is (= (be/idempotency-key object-key (:id block) "edit:C:5")
             (:idempotency-key env))))))

(deftest semantic-keydown
  (testing "content keys co-supply new-text + new-caret (BW-T6); one :edit per key (BW-T3)"
    (is (= {:op :edit :new-text "ahbc" :new-caret 2}
           (be/apply-keydown {:text "abc" :caret 1} {:type :char :char "h"})))
    (is (= {:op :edit :new-text "bc" :new-caret 0}
           (be/apply-keydown {:text "abc" :caret 1} {:type :backspace})))
    (is (= {:op :edit :new-text "ac" :new-caret 1}
           (be/apply-keydown {:text "abc" :caret 1} {:type :delete})))
    (is (= {:op :edit :new-text "a\nbc" :new-caret 2}
           (be/apply-keydown {:text "abc" :caret 1} {:type :enter}))))
  (testing "caret keys move without minting (revision-noise guard)"
    (is (= {:op :caret :new-caret 0} (be/apply-keydown {:text "abc" :caret 1} {:type :left})))
    (is (= {:op :caret :new-caret 3} (be/apply-keydown {:text "abc" :caret 1} {:type :end}))))
  (testing "boundary no-ops return nil (no envelope, no state churn)"
    (is (nil? (be/apply-keydown {:text "abc" :caret 0} {:type :backspace})))
    (is (nil? (be/apply-keydown {:text "abc" :caret 3} {:type :delete})))
    (is (nil? (be/apply-keydown {:text "abc" :caret 1} {:type :eval})))))
