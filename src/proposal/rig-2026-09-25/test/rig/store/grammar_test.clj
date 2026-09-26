(ns rig.store.grammar-test
  "The grammar fact (PLAN-tools-and-grammars.md 4, 7.1): admission, the
  row, the rebuild refusal, what the rows give the value checks and the
  read exit, and the gate's refusals in their order. No cluster."
  (:require [clojure.test :refer [deftest is testing]]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.grammar :as grammar]
            [rig.store.reads :as reads]
            [rig.store.toy-grammars :as tg]))

(def ^:private mention (:v tg/mention))
(def ^:private note (:v tg/note))
(def ^:private blob {:shape [:any] :subjects-at nil :opaque true :index #{}})

(deftest admission
  (is (nil? (grammar/refusal mention)))
  (is (nil? (grammar/refusal note)))
  (is (nil? (grammar/refusal blob)))
  (testing "an opaque grammar has shape [:any], no subjects and no :by-value (4.4)"
    (is (= :opaque-with-a-shape (grammar/refusal (assoc blob :shape [:string]))))
    (is (= :bad-subjects-at (grammar/refusal (assoc blob :subjects-at [:persons]))) "no path through [:any]")
    (is (= :opaque-by-value (grammar/refusal (assoc blob :index #{:by-value}))))
    (is (nil? (grammar/refusal (assoc blob :index #{:no-copy}))) "no-copy is about id entries, not values"))
  (testing "a subjects path walks required map entries to a set or vector of at most 256 keywords (4.3)"
    (is (= :bad-subjects-at (grammar/refusal (assoc mention :shape [:map {:persons [:set-of [:keyword] 1 2]}
                                                                    {:optional #{:persons}}])))
        "through an optional entry")
    (is (= :bad-subjects-at (grammar/refusal (assoc mention :shape [:map {:persons [:keyword]}]))) "ending on a scalar")
    (is (= :bad-subjects-at (grammar/refusal (assoc mention :shape [:map {:persons [:set-of [:keyword] 1 257]}])))
        "a collection over 256")
    (is (= :bad-subjects-at (grammar/refusal (assoc mention :shape [:map {:persons [:set-of [:string] 1 2]}])))
        "a collection of strings")
    (is (nil? (grammar/refusal (assoc mention :shape [:map {:persons [:vector-of [:keyword] 0 256]}])))))
  (testing "the parts, exactly"
    (is (= :not-a-grammar (grammar/refusal (dissoc mention :index))))
    (is (= :not-a-grammar (grammar/refusal (assoc mention :extra 1))))
    (is (= :bad-index (grammar/refusal (assoc mention :index #{:by-shape}))))
    (is (= :bad-opaque (grammar/refusal (assoc mention :opaque nil))))
    (is (= :not-a-shape (grammar/refusal (assoc mention :shape [:nope]))))
    (is (= :not-a-grammar (grammar/refusal "grammar")))))

(def ^:private settings {:kind :personal :owner :alice :class :by-layer :grain :per-value})
(def ^:private alice-row {[:alice :alice :alice] {:granted [[:alice :by-layer :offer #uuid "00000000-0000-7000-8000-000000000009"] 0]}})

(defn- act [who facts]
  (let [o {:version 1 :who who :layer :alice :class :by-layer
           :permission (when-not (= :operator who) [who :alice :alice])
           :stood-on {} :subjects #{} :facts facts}]
    (:ok (env/parse (assoc o :name (env/name-for o))))))

(deftest the-gate-refuses-in-order
  (let [rows-of (fn [o] (if (= :operator (:who o)) {} alice-row))
        refuse (fn [o key-rows] (gate/refusal o settings (rows-of o) {} key-rows))]
    (testing "admitted: the operator's and the owner's grammar"
      (is (nil? (refuse (act :operator [tg/mention]) {})))
      (is (nil? (refuse (act :alice [tg/note]) {}))))
    (testing ":malformed-control: a grammar outside the language, on a store key, two for one key"
      (is (= :malformed-control (refuse (act :operator [(assoc-in tg/note [:v :shape] [:nope])]) {})))
      (is (= :malformed-control (refuse (act :operator [(assoc tg/note :e :permission)]) {})))
      (is (= :malformed-control (refuse (act :operator [(assoc tg/note :e :read/pattern)]) {})))
      (is (= :malformed-control (refuse (act :operator [(assoc tg/note :e :grammar)]) {})))
      (is (= :malformed-control (refuse (act :operator [tg/note (assoc-in tg/note [:v :index] #{})]) {}))))
    (testing ":control-not-allowed: a writer who is neither the operator nor the owner"
      (is (= :control-not-allowed (gate/refusal (act :bob [tg/note]) settings {[:bob :alice :alice] {:granted [[:alice :by-layer :offer #uuid "00000000-0000-7000-8000-000000000009"] 0]}} {} {}))))
    (testing ":grammar-change-needs-rebuild after :control-not-allowed (b1, V-F4)"
      (is (= :grammar-change-needs-rebuild (refuse (act :operator [tg/note]) {:note {:used true :grammar nil}}))
          "a first grammar with :by-value on a used key")
      (is (= :grammar-change-needs-rebuild (refuse (act :operator [tg/note {:e :e0 :k :note :v nil}]) {}))
          "the act itself writes under the key")
      (is (nil? (refuse (act :operator [tg/mention]) {:mention {:used true :grammar nil}}))
          "same hints as none: #{} and not opaque")
      (let [row {:used true :grammar (grammar/row [[:alice :by-layer :offer #uuid "00000000-0000-7000-8000-000000000009"] 0] 1 note)}]
        (is (nil? (refuse (act :operator [(assoc-in tg/note [:v :shape] [:string])]) {:note row}))
            "a changed shape keeps the hints: admitted")
        (is (= :grammar-change-needs-rebuild (refuse (act :operator [(assoc tg/note :v blob)]) {:note row}))
            "made opaque: no longer indexed by value")))
    (testing "the 4-arity, the micro fold's: no key rows, no rebuild check; a sealed grammar there is malformed"
      (is (nil? (gate/refusal (act :operator [tg/note {:e :e0 :k :note :v nil}]) settings {} {})))
      (is (= :malformed-control (gate/refusal (update (act :operator [tg/note]) :facts #(mapv (fn [f] (assoc f :v nil)) %))
                                              settings {} {}))
          "D-P1: the micro client seals a grammar fact; its value never reaches the gate, so it is refused as data"))))

(deftest rows-and-what-they-give
  (let [nm [:alice :by-layer :offer #uuid "00000000-0000-7000-8000-000000000010"]
        facts [tg/mention {:e :e1 :k :note :v nil} {:e :e2 :k :permission :v {:id [:x :alice :alice]}}]
        w (grammar/key-row-writes facts {} nm 99 gate/store-key?)
        rows (into {} w)]
    (testing "a grammar fact sets its key's grammar; a first use sets :used; a store key gets no row (4.7)"
      (is (= [:mention :note] (map first w)))
      (is (= {:used false :grammar {:fid [nm 0] :stamp 99 :shape (env/canonical (:shape mention))
                                    :subjects-at [:persons] :opaque false :index #{}}}
             (:mention rows)))
      (is (= {:used true :grammar nil} (:note rows)))
      (is (= [] (grammar/key-row-writes [{:e :e1 :k :note :v nil}] {:note {:used true :grammar nil}} nm 99 gate/store-key?))
          "a used key is not written again"))
    (testing "the rows to read: the act's keys that are not the store's, and the keys its grammars govern"
      (is (= [:note :mention] (grammar/rows-to-read [{:e :e1 :k :note} {:e :mention :k :grammar} {:e :x :k :lease}] gate/store-key?))))
    (testing "grammars-of and named: shape, opacity and subjects from the rows"
      (let [g (grammar/grammars-of (assoc (tg/rows) :blob {:used false :grammar (grammar/row [nm 0] 1 blob)}))]
        (is (= #{:bob} (grammar/named g :mention {:persons #{:bob} :token "t"})))
        (is (= :value-shape (grammar/named g :mention {:persons #{:a :b :c}})))
        (is (= :value-shape (grammar/named g :mention {:persons "bob"})))
        (is (= #{} (grammar/named g :blob "anything")) "opaque: never interpreted")
        (is (= #{} (grammar/named g :tag {:persons #{:bob}})) "no grammar: permissive, names no one")
        (is (= #{:bob} (grammar/named grammar/grammars :mention {:persons [:bob]})) "the constant: no shape, as before")))
    (testing "hints: from the rows, with the store's own keys' no-copy (4.5)"
      (let [r (assoc (tg/rows) :blob {:used false :grammar (grammar/row [nm 0] 1 blob)})]
        (is (= {:by-value #{:note} :opaque #{:blob} :no-copy #{}} (grammar/hints r)))
        (is (= {:by-value #{:note} :opaque #{:blob} :no-copy (:no-copy reads/seed-hints)} (reads/hints-of r)))
        (is (= {:by-value #{} :opaque #{} :no-copy (:no-copy reads/seed-hints)} (reads/hints-of nil))
            "a layer with no grammar indexes no key by value")))))
