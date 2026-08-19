(ns app.client.workspace.t2-block-join-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.workspace.t2-block-join :as join]
            [app.client.workspace.text-editing :as editing]))

(defn- block [id text object-key]
  {:id id :text text :kind :prose
   :document-container-id (str "oc:chat-message:" object-key ":event")
   :object-key object-key})

(defn- context [& turns]
  {:turns (mapv (fn [n blocks] {:id (str "turn-" n) :blocks (vec blocks)})
                (range) turns)})

(defn- mount [ctx]
  (join/reconcile-full (join/init) ctx "t2-client:1"))

(defn- commit-text [state text]
  (join/commit state {:vi (get-in state [:target :vi])
                      :text text
                      :caret (editing/tagged-offset (count text))
                      :ops [{:op :insert :text text}]}))

(deftest exact-selection-and-stable-occurrence
  (let [a (block "u-a" "a" "chat:a")
        b (block "u-b" "b" "chat:b")
        invalid {:id "u-x" :text "x"}
        s0 (mount (context [a invalid] [b]))
        s1 (join/reconcile-full s0 (context [b] [a]) "must-not-be-used")]
    (is (= "u-b" (get-in s0 [:target :block :id]))
        "initial selection is the final eligible delivered block")
    (is (= "u-b" (get-in s1 [:target :block :id]))
        "the current eligible unit survives a reordered whole projection")
    (is (= [:vi :t2-real-block "u-b"] (get-in s1 [:target :vi])))
    (is (= "t2-client:1" (:edit-client-id s1)))
    (is (nil? (:target (mount (context [invalid]))))
        "no owned eligible block means no occurrence")))

(deftest envelope-uses-served-provenance
  (let [b (block "u-thread" "old" "chat:thread-real")
        s (mount (context [b]))
        {:keys [state envelope]} (commit-text s "new")]
    (is (= "u-thread" (get-in envelope [:target :target/id])))
    (is (= "chat:thread-real" (get-in envelope [:payload :object-key])))
    (is (= (:document-container-id b)
           (get-in envelope [:payload :document-container-id])))
    (is (= "new" (:text (join/view state))))
    (testing "the tagged UTF-16 boundary fails closed on a surrogate interior"
      (is (thrown? Exception
                   (join/commit s {:vi (get-in s [:target :vi])
                                   :text "😀"
                                   :caret (editing/tagged-offset 1)
                                   :ops [{:op :insert :text "😀"}]}))))))

(deftest causal-keyed-truth-and-full-pull-barriers
  (let [b (block "u" "old" "chat:u")
        s0 (mount (context [b]))
        c1 (commit-text s0 "one")
        r1 (:request-id (:envelope c1))
        s1 (join/on-decision (:state c1) r1
                             {:status :accepted :request-nonce 1})
        stale-full (join/reconcile-full s1 (context [(assoc b :text "old")])
                                        "unused")
        c2 (commit-text stale-full "two")
        r2 (:request-id (:envelope c2))
        s2 (join/on-decision (:state c2) r2
                             {:status :accepted :request-nonce 2})
        old-keyed (join/on-keyed-truth s2 "u"
                                       {:found? true :text "one"
                                        :request-nonce 1})
        new-keyed (join/on-keyed-truth old-keyed "u"
                                       {:found? true :text "two-server"
                                        :request-nonce 2})]
    (is (= "one" (:text (join/view stale-full)))
        "a stale full projection cannot overwrite accepted projection")
    (is (= "two" (:text (join/view old-keyed)))
        "a delayed earlier keyed read cannot release the newer wait")
    (is (= 2 (get-in old-keyed [:awaiting-keyed-truth :nonce])))
    (is (= "two-server" (:text (join/view new-keyed))))
    (is (nil? (:awaiting-keyed-truth new-keyed)))))

(deftest decisions-are-target-session-correlated
  (let [s0 (mount (context [(block "u" "old" "chat:u")]))
        {:keys [state envelope]} (commit-text s0 "new")
        untouched (join/on-decision state "other-session:7"
                                    {:status :rejected :reason :edit/stale})
        rejected (join/on-decision state (:request-id envelope)
                                   {:status :rejected :reason :edit/stale})]
    (is (= state untouched))
    (is (= "old" (:text (join/view rejected))))
    (is (= {:unit-id "u" :reason :edit/stale} (:refusal rejected)))))
