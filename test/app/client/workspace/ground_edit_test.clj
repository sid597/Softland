(ns app.client.workspace.ground-edit-test
  "first-light P2b — the open ground's pure edit machine (P2B.md receipt b).

   The committed-echo law under test: the rendered (text, caret) pair comes
   from ONE confirmed value; the intent queue is invisible; refusal rebases;
   birth mints nothing before the first content act."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.client.workspace.ground-edit :as ge]))

(def ^:private bi {:id "du:u1" :document-container-id "doc-1"})
(def ^:private ok "chat:feedbeef")

(defn- fresh [] (ge/init "test-client"))

(defn- type! [st s]
  (reduce (fn [{:keys [state envelopes]} ch]
            (let [r (ge/input state {:type :char :char (str ch)} bi ok)]
              {:state (:state r)
               :envelopes (cond-> envelopes (:envelope r) (conj (:envelope r)))}))
          {:state st :envelopes []}
          s))

(deftest anchor-and-escape-leave-nothing
  (testing "Escape on an anchor mints NOTHING (moment 1 / G4b)"
    (let [st (-> (fresh) (ge/set-anchor {:x 10 :y 20}) ge/escape)]
      (is (= :rest (:mode st)))
      (is (nil? (:anchor st)))
      (is (nil? (:birth st)))))
  (testing "a caret-only key at an anchor births nothing"
    (is (nil? (ge/begin-birth (-> (fresh) (ge/set-anchor {:x 0 :y 0}))
                              {:type :left} "b1")))))

(deftest birth-at-first-content-act
  (let [st (-> (fresh) (ge/set-anchor {:x 5 :y 7}))
        {:keys [state post]} (ge/begin-birth st {:type :char :char "h"} "b1")]
    (is (= :birthing (:mode state)))
    (is (= {:block-id "b1" :text "h" :pos {:x 5 :y 7}} post))
    (testing "keys during birth flight queue as intent — never rendered"
      (let [st' (-> state
                    (ge/birth-key {:type :char :char "i"})
                    (ge/birth-key {:type :char :char "!"}))]
        (is (= 2 (count (get-in st' [:birth :queue]))))
        (testing "the ack replays the queue as envelopes seq 0…"
          (let [{:keys [state envelopes]} (ge/birth-acked st' "du:u1" bi ok)]
            (is (= :editing (:mode state)))
            (is (= "du:u1" (:focus state)))
            (is (= 2 (count envelopes)))
            (is (= "hi" (get-in (first envelopes) [:payload :content-text])))
            (is (= "hi!" (get-in (second envelopes) [:payload :content-text])))
            (is (= [0 1] (mapv :edit-seq envelopes)))
            (testing "…but the RENDERED value is still the acked birth text"
              (is (= "h" (:text (ge/block-view state "du:u1" nil))))))))))
  (testing "paste as the first content act births"
    (let [st (-> (fresh) (ge/set-anchor {:x 0 :y 0}))
          {:keys [post]} (ge/begin-birth st {:type :paste :text "pasted"} "b2")]
      (is (= "pasted" (:text post))))))

(deftest p2-outside-paste-clamp-is-material-and-expandable
  (let [policy {:threshold-chars 4
                :max-share 0.5
                :header-copy "pasted"}
        small (ge/paste-decision "abcd" policy :clipboard)
        clamped (ge/paste-decision "abcdefghij" policy :clipboard)
        source (:insert-text clamped)
        collapsed (ge/paste-projection source false)
        expanded (ge/paste-projection source true)]
    (testing "the threshold and fraction come entirely from the worn policy"
      (is (= "abcd" (:insert-text small)))
      (is (false? (:paste/clamped? small)))
      (is (true? (:paste/clamped? clamped)))
      (is (= 10 (:paste/total-chars clamped)))
      (is (= 5 (:paste/shown-chars clamped))))
    (testing "the durable source carries the source mark and every pasted byte"
      (is (str/includes? source "pasted · clipboard"))
      (is (str/ends-with? source "abcdefghij"))
      (is (= "abcde" (:paste/body collapsed)))
      (is (= "abcdefghij" (:paste/body expanded)))
      (is (str/starts-with? (:paste/header collapsed) "▸"))
      (is (str/starts-with? (:paste/header expanded) "▾")))
    (testing "the ordinary edit result retains the complete envelope"
      (let [result (ge/apply-ground-keydown
                    {:text "before:" :caret 7}
                    {:type :paste
                     :text "abcdefghij"
                     :paste-policy policy
                     :paste/source-mark :clipboard})]
        (is (:paste/clamped? result))
        (is (str/ends-with? (:new-text result) "abcdefghij"))
        (is (= (count (:new-text result)) (:new-caret result)))))))

(deftest committed-echo-render-law
  (let [st (ge/focus-block (fresh) "du:u1" "hello" 5)
        {:keys [state envelopes]} (type! st "ab")]
    (testing "keystrokes mint envelopes on the PROJECTION…"
      (is (= ["helloa" "helloab"]
             (mapv #(get-in % [:payload :content-text]) envelopes))))
    (testing "…while the render stays on confirmed until decisions land"
      (is (= "hello" (:text (ge/block-view state "du:u1" nil))))
      (is (= 5 (:caret (ge/block-view state "du:u1" nil)))))
    (testing "an accepted decision advances confirmed to THAT revision"
      (let [rid (:request-id (first envelopes))
            st' (ge/on-decision state rid {:status :accepted})]
        (is (= "helloa" (:text (ge/block-view st' "du:u1" nil))))
        (is (= 6 (:caret (ge/block-view st' "du:u1" nil))))
        (is (= 1 (count (get-in st' [:queue :inflight]))))))
    (testing "an accepted LATER seq retires skipped intermediates (conflation heals)"
      (let [rid2 (:request-id (second envelopes))
            st' (ge/on-decision state rid2 {:status :accepted})]
        (is (= "helloab" (:text (ge/block-view st' "du:u1" nil))))
        (is (empty? (get-in st' [:queue :inflight])))))
    (testing "a rejection drops the WHOLE queue (rebase) + visible refusal"
      (let [rid (:request-id (first envelopes))
            st' (ge/on-decision state rid {:status :rejected :reason :edit/stale})]
        (is (= "hello" (:text (ge/block-view st' "du:u1" nil))) "back on confirmed")
        (is (empty? (get-in st' [:queue :inflight])))
        (is (= :edit/stale (:refusal (ge/block-view st' "du:u1" nil))))))))

(deftest optimistic-echo-mode
  "The typing-lag patch. Same queue, same envelopes, same rebase — only the
   END of the queue that gets painted moves. Every assertion here is the
   :confirmed law's mirror image, so the two modes stay visibly paired."
  (let [st (ge/focus-block (fresh) "du:u1" "hello" 5)
        {:keys [state envelopes]} (type! st "ab")]
    (testing "typed text paints IMMEDIATELY — no ack waited on"
      (is (= "helloab" (:text (ge/block-view state "du:u1" nil :optimistic))))
      (is (= 7 (:caret (ge/block-view state "du:u1" nil :optimistic)))))
    (testing "the envelopes are byte-identical to the committed-echo path"
      (is (= ["helloa" "helloab"]
             (mapv #(get-in % [:payload :content-text]) envelopes))))
    (testing "an ack changes NOTHING on screen — the head already showed it"
      (let [st' (ge/on-decision state (:request-id (first envelopes))
                                {:status :accepted})]
        (is (= "helloab" (:text (ge/block-view st' "du:u1" nil :optimistic)))
            "no repaint on ack: the render sig is unchanged")))
    (testing "a rejection VISIBLY rewinds to the last acknowledged revision"
      (let [st' (ge/on-decision state (:request-id (first envelopes))
                                {:status :rejected :reason :edit/stale})]
        (is (= "hello" (:text (ge/block-view st' "du:u1" nil :optimistic))))
        (is (= :edit/stale (:refusal (ge/block-view st' "du:u1" nil :optimistic))))))
    (testing "an unfocused block still renders served truth, both modes"
      (is (= "served" (:text (ge/block-view state "du:other" "served" :optimistic))))))
  (testing "a caret key MID-FLIGHT moves the head, so the next envelope and the
            painted caret agree (this was dropped on both sides before)"
    (let [{:keys [state]} (type! (ge/focus-block (fresh) "du:u1" "" 0) "abc")
          r    (ge/input state {:type :left} bi ok)
          st'  (:state r)
          r2   (ge/input st' {:type :char :char "X"} bi ok)]
      (is (nil? (:envelope r)) "caret keys never mint")
      (is (= 2 (:caret (ge/block-view st' "du:u1" nil :optimistic))))
      (is (= "abXc" (get-in (:envelope r2) [:payload :content-text]))
          "the insert lands at the MOVED caret, not the stale head caret"))))

(deftest caret-and-truth
  (testing "caret keys move INSIDE confirmed — one value, no envelope"
    (let [st (ge/focus-block (fresh) "du:u1" "ab\ncd" 5)
          r  (ge/input st {:type :up} bi ok)]
      (is (nil? (:envelope r)))
      (is (= 2 (get-in (:state r) [:queue :confirmed :caret]))
          "up from (1,2) lands at (0,2)")))
  (testing "truth adopts ONLY at rest (inflight empty)"
    (let [st (ge/focus-block (fresh) "du:u1" "old" nil)
          st' (ge/adopt-truth st "du:u1" "newer truth")]
      (is (= "newer truth" (get-in st' [:queue :confirmed :text]))))
    (let [{:keys [state]} (type! (ge/focus-block (fresh) "du:u1" "old" nil) "x")]
      (is (= "old" (get-in (ge/adopt-truth state "du:u1" "newer")
                           [:queue :confirmed :text]))
          "in-flight intent blocks adoption — the echo will catch up")))
  (testing "unfocused blocks render truth; blur drops the queue"
    (let [st (ge/blur (ge/focus-block (fresh) "du:u1" "mine" nil))]
      (is (= "served" (:text (ge/block-view st "du:u1" "served"))))
      (is (nil? (:queue st))))))

(deftest geometry-helpers
  (is (= {:line 0 :col 3} (ge/caret->line-col "abc\ndef" 3)))
  (is (= {:line 1 :col 0} (ge/caret->line-col "abc\ndef" 4)))
  (is (= 4 (ge/line-col->caret "abc\ndef" 1 0)))
  (is (= 7 (ge/line-col->caret "abc\ndef" 1 99)) "col clamps to line end")
  (is (= 4 (ge/line-col->caret "abc\ndef" 99 0)) "line clamps to the last line")
  (testing "the ~4px threshold splits click from drag"
    (is (not (ge/drag? [0 0] [2 2] 4.0)))
    (is (ge/drag? [0 0] [5 1] 4.0))))

(deftest selection-is-attention-state-over-confirmed
  (let [st (-> (fresh) (ge/focus-block "du:u1" "hello\nworld" 0))]
    (testing "begin+extend normalize across direction; text reads confirmed"
      (let [sel (-> st (ge/begin-select 8) (ge/extend-select 2))]
        (is (= [2 8] (ge/selection-range sel)))
        (is (= "llo\nwo" (ge/selection-text sel)))
        (is (= [2 8] (:selection (ge/block-view sel "du:u1" "hello\nworld"))))))
    (testing "collapsed selection is no selection"
      (let [sel (-> st (ge/begin-select 3) (ge/extend-select 3))]
        (is (nil? (ge/selection-range sel)))
        (is (nil? (ge/selection-text sel)))))
    (testing "the head clamps into confirmed text"
      (is (= [0 11] (ge/selection-range
                     (-> st (ge/begin-select 0) (ge/extend-select 999))))))
    (testing "a content key clears the selection (MVP: copy-only selection)"
      (let [sel (-> st (ge/begin-select 0) (ge/extend-select 4))
            r   (ge/input sel {:type :char :char "x"} bi ok)]
        (is (nil? (ge/selection-range (:state r))))
        (is (some? (:envelope r)))))
    (testing "a caret key clears the selection"
      (let [st5 (-> (fresh) (ge/focus-block "du:u1" "hello\nworld" 5))
            sel (-> st5 (ge/begin-select 0) (ge/extend-select 4))
            r   (ge/input sel {:type :left} bi ok)]
        (is (nil? (ge/selection-range (:state r))))
        (is (nil? (:envelope r)))))
    (testing "escape kills the selection with the focus"
      (is (nil? (:selection (-> st (ge/begin-select 0) (ge/extend-select 4)
                                ge/escape)))))
    (testing "re-focus starts clean"
      (is (nil? (:selection (-> st (ge/begin-select 0) (ge/extend-select 4)
                                (ge/focus-block "du:u2" "other" 0))))))
    (testing "selection outside :editing arms nothing"
      (is (nil? (:selection (ge/begin-select (fresh) 3)))))))
