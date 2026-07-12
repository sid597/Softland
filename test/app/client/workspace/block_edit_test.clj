(ns app.client.workspace.block-edit-test
  "Lane B unit gates for block-write (CONTRACT §3/§5). JVM-run over the pure
   .cljc logic — no cljs runtime. Covers:
   - envelope law: op-id determinism (BW-T5), edit-seq monotonicity + fresh
     client-id (BW-T8), verbatim document-container-id + unit-id target (BW-T7),
     content-text-only payload (no client hash);
   - G6 caret/text co-variance: one-signal derivation never tears (BW-T6/L8),
     pending-input never renders outside the focused block (BW-T4);
   - G5 client half: refusal → buffer revert to materialized truth + notice."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string]
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

(deftest edit-seq-monotonic-and-fresh-client-id
  (testing "each keystroke advances the seq; ids stay unique (BW-T8)"
    (let [s0 (be/init-state "edit:C")
          s1 (be/focus s0 (:id block) "")
          step (fn [st txt]
                 (be/input st {:block block :object-key object-key
                               :new-text txt :new-caret (count txt)}))
          r1 (step s1 "a")
          r2 (step (:state r1) "ab")
          r3 (step (:state r2) "abc")]
      (is (= [0 1 2] [(get-in r1 [:envelope :edit-seq])
                      (get-in r2 [:envelope :edit-seq])
                      (get-in r3 [:envelope :edit-seq])]))
      (is (= 3 (:next-seq (:state r3))))
      (is (apply distinct? (map #(get-in % [:envelope :request-id]) [r1 r2 r3])))))
  (testing "a fresh session mints a distinct client-id → distinct request-ids (BW-T8)"
    (let [a (be/init-state "edit:A")
          b (be/init-state "edit:B")
          mk (fn [st] (:envelope (be/input (be/focus st (:id block) "")
                                           {:block block :object-key object-key
                                            :new-text "x" :new-caret 1})))]
      (is (not= (:request-id (mk a)) (:request-id (mk b))))
      (is (not= (:idempotency-key (mk a)) (:idempotency-key (mk b)))))))

(deftest input-only-on-focused-block
  (testing "a keystroke on a non-focused block is a no-op (BW-T4)"
    (let [s (be/focus (be/init-state "edit:C") "other-block" "")
          {:keys [state envelope]}
          (be/input s {:block block :object-key object-key
                       :new-text "z" :new-caret 1})]
      (is (nil? envelope))
      (is (= s state)))))

;; ---------------------------------------------------------------------------
;; G6 — caret / text co-variance (one-signal derivation, BW-T6 / L8)
;; ---------------------------------------------------------------------------

(deftest g6-caret-text-covariance
  (testing "a burst of buffer states never tears caret from one and text from
            another — block-view sources both from the SINGLE buffer value"
    (let [;; caret is defined = (count text) for each snapshot, so any tear
          ;; (caret from state N, text from state M) would break caret==count text
          texts   ["h" "he" "hel" "hell" "hello" "hell" "hel"]
          states  (reductions
                   (fn [st txt]
                     (:state (be/input st {:block block :object-key object-key
                                           :new-text txt :new-caret (count txt)})))
                   (be/focus (be/init-state "edit:C") (:id block) "")
                   texts)]
      (doseq [st (rest states)]
        (let [{:keys [text caret pending?]} (be/block-view st (:id block) "IGNORED")]
          (is (= caret (count text)) "caret and text come from the same snapshot")
          (is pending? "focused block shows pending-input")))))
  (testing "pending-input NEVER renders outside the focused block (BW-T4)"
    (let [st (:state (be/input (be/focus (be/init-state "edit:C") (:id block) "")
                               {:block block :object-key object-key
                                :new-text "typed" :new-caret 5}))
          other (be/block-view st "some-other-unit" "truth-text")]
      (is (= false (:pending? other)))
      (is (nil? (:caret other)))
      (is (= "truth-text" (:text other)) "other blocks render materialized truth"))))

;; ---------------------------------------------------------------------------
;; G5 client half — refusal visibility + buffer revert
;; ---------------------------------------------------------------------------

(deftest g5-refusal-reverts-buffer
  (let [s0 (be/focus (be/init-state "edit:C") (:id block) "truth")
        {:keys [state envelope]}
        (be/input s0 {:block block :object-key object-key
                      :new-text "truthX" :new-caret 6})
        rid (:request-id envelope)]
    (testing "accepted decision retires pending, raises NO refusal, keeps buffer"
      (let [s (be/on-decision state rid {:status :accepted} "truth")]
        (is (nil? (:refusal s)))
        (is (empty? (:pending s)))
        (is (= "truthX" (get-in s [:buffer :text])) "pending-input stays until pull")))
    (testing "rejected decision reverts the buffer to materialized truth + notice (G5)"
      (let [s (be/on-decision state rid {:status :rejected :reason :edit/stale} "truth")]
        (is (= :edit/stale (:reason (:refusal s))))
        (is (= (:id block) (:block-id (:refusal s))))
        (is (= "truth" (get-in s [:buffer :text])) "buffer reverts — no phantom text (BW-T4)")
        (is (= 5 (get-in s [:buffer :caret])) "caret follows the reverted text")
        (is (empty? (:pending s)))
        (testing "the refusal surfaces in the block view (no silent drop)"
          (is (= :edit/stale (:refusal (be/block-view s (:id block) "truth")))))))))

;; ---------------------------------------------------------------------------
;; INT wiring pure fns (orchestrating session): keydown transitions + the
;; face-context projection that ENFORCES the BW-T4 boundary at render.
;; ---------------------------------------------------------------------------

(deftest int-apply-keydown
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

(deftest int-move-caret-clamps
  (let [s0 (be/focus (be/init-state "edit:C") (:id block) "abc")]
    (is (= 2 (get-in (be/move-caret s0 2) [:buffer :caret])))
    (is (= 3 (get-in (be/move-caret s0 99) [:buffer :caret])) "clamped to text")
    (is (= 0 (get-in (be/move-caret s0 -5) [:buffer :caret])) "clamped to 0")
    (is (= "abc" (get-in (be/move-caret s0 2) [:buffer :text])) "text untouched")))

(deftest int-overlay-face-context
  (let [ctx {:turns [{:id "t1" :blocks [{:id (:id block) :text "truth-a"}
                                        {:id "u2" :text "truth-b"}]}]
             :reader-turn {:id "t1" :blocks [{:id (:id block) :text "truth-a"}
                                             {:id "u2" :text "truth-b"}]}
             :conversation/address object-key}]
    (testing "idle state → the SAME ctx object (scene cache still short-circuits)"
      (is (identical? ctx (be/overlay-face-context ctx (be/init-state "edit:C") {}))))
    (testing "focused block renders pending-input + caret glyph; others untouched (BW-T4)"
      (let [st (:state (be/input (be/focus (be/init-state "edit:C") (:id block) "truth-a")
                                 {:block block :object-key object-key
                                  :new-text "typed" :new-caret 5}))
            out (be/overlay-face-context ctx st {})
            [b1 b2] (get-in out [:turns 0 :blocks])]
        (is (= (str "typed" be/caret-glyph) (:text b1)) "buffer + caret at 5")
        (is (= "truth-b" (:text b2)) "pending-input NEVER outside the focused block")
        (is (= (:text b1) (get-in out [:reader-turn :blocks 0 :text]))
            "reader-turn shows the SAME transformed block")))
    (testing "truth overlay repaints UNFOCUSED blocks (the §5 narrowing echo)"
      (let [out (be/overlay-face-context ctx (be/init-state "edit:C") {"u2" "newer-truth"})]
        (is (= "newer-truth" (get-in out [:turns 0 :blocks 1 :text])))
        (is (= "truth-a" (get-in out [:turns 0 :blocks 0 :text])))))
    (testing "refusal surfaces as a visible notice line (G5 — no silent drop)"
      (let [st (-> (be/init-state "edit:C")
                   (be/focus (:id block) "truth-a")
                   (assoc :refusal {:block-id "u2" :reason :edit/stale}))
            out (be/overlay-face-context ctx st {})]
        (is (clojure.string/includes? (get-in out [:turns 0 :blocks 1 :text])
                                      "edit refused")
            "the refusal block carries the notice")))))

;; ---------------------------------------------------------------------------
;; Gate-review fixes (FALSIFY F3/F6) — regressions per the finder's named
;; cheapest falsifiers.
;; ---------------------------------------------------------------------------

(deftest f6-refusal-lifecycle
  (let [s0 (be/focus (be/init-state "edit:C") (:id block) "truth")
        {:keys [state envelope]} (be/input s0 {:block block :object-key object-key
                                               :new-text "truthX" :new-caret 6})
        refused (be/on-decision state (:request-id envelope)
                                {:status :rejected :reason :edit/stale} "truth")]
    (is (some? (:refusal refused)) "precondition: refusal raised")
    (testing "resuming typing dismisses the refusal (the documented dismiss path)"
      (let [{s' :state} (be/input refused {:block block :object-key object-key
                                           :new-text "truthY" :new-caret 6})]
        (is (nil? (:refusal s')))))
    (testing "blur clears the refusal — a notice never outlives the edit session"
      (is (nil? (:refusal (be/blur refused)))))))

(deftest f3-bounds-and-unknown-decisions
  (testing ":pending is bounded at 64, dropping the OLDEST seq"
    (let [s (reduce (fn [s i]
                      (:state (be/input s {:block block :object-key object-key
                                           :new-text (str "t" i) :new-caret 1})))
                    (be/focus (be/init-state "edit:C") (:id block) "truth")
                    (range 80))]
      (is (= 64 (count (:pending s))))
      (is (= 16 (apply min (map :seq (vals (:pending s)))))
          "entries 0-15 trimmed; newest 64 kept")))
  (testing "a rejected decision for an unknown/trimmed request-id is a no-op
            (no nil-addressed refusal)"
    (let [s0 (be/focus (be/init-state "edit:C") (:id block) "truth")
          s  (be/on-decision s0 "edit:C:999" {:status :rejected :reason :edit/stale} "truth")]
      (is (nil? (:refusal s)))
      (is (= s0 s)))))
