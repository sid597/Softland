(ns rig.store.grammar-micro-test
  "Step 6b: pre-batch admission, subjects, projection composition, reads,
  reclass, landing and rebuild. The cluster runs only in the final suite."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rt]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.grammar :as grammar]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.promote-client :as pc]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as sr]
            [rig.store.standing :as sg]))

(def plain-grammar {:shape [:any] :subjects-at nil :opaque false :index #{}})
(defn gfact [k g] {:e k :k :grammar :v g})
(def next-offer (atom 0))
(defn op
  "Ordered fixture UUIDv7s make M2's fold order explicit, even in one ms."
  [facts]
  (mc/build {:who :operator :layer :group :session :six-b :facts facts
             :name (env/make-name :group :by-entity :offer
                                  (java.util.UUID. 0x7000 (bit-or Long/MIN_VALUE (swap! next-offer inc))))}))

(defn gathered
  "The actual gather's pure steps, for operator controls/retracts."
  [offers keyrows]
  (into {}
        (mapcat
         (fn [o]
           (let [in (micro/intake o)
                 sk (micro/skeleton in {:status :ok :owned []} nil nil)
                 ns (micro/name-step sk nil 0 0)
                 ls (micro/layer-rows sk {:kind :group :class :by-entity :grain :per-value}
                                      nil {} {} {})
                 first-rows (into (:rows ns) (:rows ls))]
             (concat (micro/grammar-rows in keyrows)
                     (mapcat (fn [[i e]] (micro/entity-rows (:sk ns) e 0 0 {} first-rows (zero? i)))
                             (map-indexed vector (:entities sk)))))) offers)))

(defn fold [offers keyrows] (:writes (micro/prepare (gathered offers keyrows) 1000 9)))
(defn records [writes]
  (into {} (for [[kind nm field _ v] writes :when (and (= :name kind) (= :answer field))] [nm v])))

(deftest grammar-use-in-both-orders
  (doseq [grammar-first? [true false]]
    (let [gf (gfact :new-key (assoc plain-grammar :index #{:by-value}))
          vf {:e :entity :k :new-key :v nil}
          [a b] (mapv #(op [%]) (if grammar-first? [gf vf] [vf gf]))
          g (if grammar-first? a b)
          v (if grammar-first? b a)
          ws (fold [b a] {})
          recs (records ws)]
      (is (= :grammar-change-needs-rebuild (:reason (get recs (:name g)))))
      (is (= :yes (:answer (get recs (:name v)))))
      (is (= [{:used true :grammar nil}]
             (for [[kind L field k row] ws :when (= [:entity :group :key-rows :new-key] [kind L field k])] row)))))
  (let [o (op [(gfact :new-key (assoc plain-grammar :opaque true)) {:e :x :k :new-key :v nil}])]
    (is (= :grammar-change-needs-rebuild (:reason (get (records (fold [o] {})) (:name o)))))))

(deftest composed-rows-and-refusal-precedence
  (let [a (op [(gfact :link plain-grammar)])
        b (op [{:e :x :k :link :v nil}])
        last-g (assoc plain-grammar :shape [:string])
        d (op [(gfact :link last-g)])
        ws (fold [d b a] {})
        kwrites (filter #(= [:entity :group :key-rows :link] (subvec % 0 4)) ws)
        row (last (first kwrites))]
    (is (every? #(= :yes (:answer %)) (vals (records ws))))
    (is (= 1 (count kwrites)))
    (is (true? (:used row)))
    (is (= [(:name d) 0] (get-in row [:grammar :fid])))
    (is (= {:link (dissoc last-g :index)} (grammar/grammars-of {:link row}))))
  (let [bad (op [{:e :elsewhere :k :kind :v :group}
                (gfact :link (assoc plain-grammar :index #{:by-value}))])]
    (is (= :malformed-control (:reason (get (records (fold [bad] {:link {:used true}})) (:name bad)))))))

(deftest refused-offers-still-count-as-batch-uses
  (let [g (op [(gfact :link (assoc plain-grammar :index #{:by-value}))])
        state (assoc (gathered [g] {}) [:use :group :link] true)
        ws (:writes (micro/prepare state 1000 9))]
    (is (= :grammar-change-needs-rebuild (:reason (get (records ws) (:name g)))))
    (is (empty? (filter #(= :key-rows (nth % 2)) ws)) "a refusal must not persist the synthetic used flag"))
  (let [g (op [(gfact :link (assoc plain-grammar :index #{:by-value}))])
        state (assoc (gathered [g] {}) [:use :base :link] true)
        ws (:writes (micro/prepare state 1000 9))]
    (is (= :yes (:answer (get (records ws) (:name g)))))
    (is (= [:group] (mapv second (filter #(= :key-rows (nth % 2)) ws)))
        "a use in another layer neither blocks this grammar nor writes there")))

(deftest saved-subjects-are-per-value
  (let [lease (env/make-name :group :by-entity)
        ids (locks/lease-ids lease 2)
        ks (zipmap ids (repeatedly 2 locks/fresh-lock))
        facts [{:e :a :k :custom :v {:people #{:alice}}}
               {:e :b :k :custom :v {:people #{:bob}}}]
        sealed (mc/seal (op facts) (mc/assign-by-grain :per-value ids) ks)
        in (micro/intake sealed)
        lrows (into {} (map (fn [[id K]] [id {:under nil :sealed K :layer :group :session :six-b :kind :group}])) ks)
        g {:custom {:shape [:map {:people [:set-of [:keyword] 0 2]}] :subjects-at [:people] :opaque false :index #{}}}
        arr (micro/arrival-open in lrows {} g)
        ww (micro/row-wraps in lrows {} (:subjects arr))]
    (is (nil? (:value-reason arr)))
    (is (= #{:alice :bob} (:union arr)))
    (is (= #{:alice} (get-in arr [:subjects 0])))
    (is (= [:alice] (get-in ww [:wraps (first ids) :any-of])))
    (is (= [:bob] (get-in ww [:wraps (second ids) :any-of])))
    (is (not-any? bytes? (tree-seq coll? seq (micro/skeleton in arr nil nil))))
    (is (= #{} (:union (micro/arrival-open in lrows {} {}))) "no grammar means no value-named people")))

(def reader {:reader :alice :reader-kind :person :working :alice-hand
             :permission [:alice :alice-hand :alice-hand]})
(defn wait-until [f]
  (let [end (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [v (f)]
        (if (or v (> (System/currentTimeMillis) end)) v
            (do (Thread/sleep 50) (recur)))))))

(defn settle! [st b]
  (is (wait-until #(every? (fn [task]
                            (<= b (or (:frontier (foreign-invoke-query (:micro-progress st) task)) -1)))
                          (range 4)))
      (str "batch " b " settled on every task")))

(defn rd [st L pattern] (rx/read! st (merge reader {:layer L :read [:pattern pattern]})))
(defn yes! [a] (is (= :yes (:answer a)) (pr-str a)) a)
(defn grammar! [st L k g]
  (let [o (mc/build {:who :operator :layer L :facts [(gfact k g)]})
        a (yes! (mc/offer! st o))]
    (when-let [b (:batch (mc/record-of st (:name o)))] (settle! st b))
    a))
(defn write-value! [st L facts]
  (let [r (mc/write! st {:who :operator :layer L :session :six-b :facts facts})]
    (yes! (:answer r))
    (when-let [b (:batch (mc/record-of st (get-in r [:offer :name])))] (settle! st b))
    r))
(defn seal-for-batch! [st facts]
  (let [l (mc/lease! st {:who :operator :layer :group :session :six-b :n (count facts)})
        ks (mc/take-locks st (:name l))]
    (yes! (:answer l))
    (mc/seal (op facts) (mc/assign-by-grain :per-value (:ids l)) ks)))
(defn in-one-batch! [ipc st offers]
  (rt/pause-microbatch-topology! ipc (:module-name st) "micro")
  (try (doseq [o offers] (mc/send! st o))
       (finally (rt/resume-microbatch-topology! ipc (:module-name st) "micro")))
  (let [answers (mapv #(mc/await-answer st %) offers)
        batches (keep #(:batch (mc/record-of st (:name %))) offers)]
    (when (seq batches) (settle! st (apply max batches)))
    answers))

(def pair-shape [:map {:left [:set-of [:keyword] 0 2] :right [:set-of [:keyword] 0 2]}])
(def left-grammar {:shape pair-shape :subjects-at [:left] :opaque false :index #{:by-value}})
(def right-grammar (assoc left-grammar :subjects-at [:right]))

(deftest grammars-through-the-micro-store
  (inject/reset-all!)
  (with-open [ipc (rt/create-ipc)]
    ;; Multiple workers exercise the actual handoff serialization.
    (rt/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 2})
    (let [st (merge (rx/connect ipc) (pc/connect ipc))
          value {:left #{:alice} :right #{:bob}}
          keyrow #(foreign-select-one [(keypath %1 :key-rows %2)] (:micro st))]
      (doseq [a (c/seed! st)] (yes! a))
      (doseq [a (mc/make-base! st)] (yes! a))
      (yes! (c/offer-until-answered! st (c/build {:who :operator :layer :base :class :by-layer
                                                :facts [(gfact :inherited left-grammar)]})))
      (let [g (mc/make-group! st :group (:group mc/shared-world))] (yes! (:made g)))

      (testing "a key named only in facts controls shape, subjects and value indexes"
        (grammar! st :group :custom left-grammar)
        (let [r (write-value! st :group [{:e :node :k :custom :v value}])
              nm (get-in r [:offer :name])]
          (is (= #{:alice} (:subjects (mc/record-of st nm))))
          (is (= [:alice] (get-in (mc/row-of st :node [nm 0]) [:lock :any-of])))
          (is (= [value] (mapv :value (:rows (rd st :group [:kv :custom value])))))
          (is (true? (:used (keyrow :group :custom)))))
        (is (= :value-shape (get-in (mc/write! st {:who :operator :layer :group :session :six-b
                                                   :facts [{:e :bad :k :custom :v 42}]}) [:answer :reason])))
        (is (= :control-not-allowed
               (:reason (mc/offer! st (mc/build {:who :alice :layer :group
                                                :permission [:alice :group :group [:group :group :group]]
                                                :facts [(gfact :other plain-grammar)]})))))
        (is (= :not-indexed (:refused (rd st :group [:kv :note "not compiled here"]))))
        (is (= :not-visible (:refused (foreign-invoke-query (:read-pattern st) :group :stranger
                                                            [:kv :custom value] nil 10)))))

      (testing "a subject edit in the same batch governs only the next batch"
        (doseq [grammar-first? [true false]]
          (let [k (if grammar-first? :before :after)
                _ (grammar! st :group k left-grammar)
                facts [{:e k :k k :v value}]
                g-before (when grammar-first? (op [(gfact k right-grammar)]))
                v (seal-for-batch! st facts)
                g (or g-before (op [(gfact k right-grammar)]))
                answers (in-one-batch! ipc st [v g])]
            (is (= grammar-first? (neg? (compare (nth (:name g) 3) (nth (:name v) 3)))))
            (doseq [a answers] (yes! a))
            (is (= (:batch (mc/record-of st (:name g))) (:batch (mc/record-of st (:name v)))))
            (is (= #{:alice} (:subjects (mc/record-of st (:name v)))))
            (is (= [:alice] (get-in (mc/row-of st k [(:name v) 0]) [:lock :any-of])))
            (let [next (write-value! st :group facts)]
              (is (= #{:bob} (:subjects (mc/record-of st (get-in next [:offer :name]))))))
            ;; Recorded retries answer from their record even under the new grammar.
            (is (= :yes (:answer (mc/offer! st v))))
            (is (= #{:alice} (:subjects (mc/record-of st (:name v)))))
            (is (= [:alice] (get-in (mc/row-of st k [(:name v) 0]) [:lock :any-of]))))))

      (testing "shape changes also start at the next batch"
        (grammar! st :group :shape-change (assoc plain-grammar :shape [:string]))
        (let [g (op [(gfact :shape-change (assoc plain-grammar :shape [:keyword]))])
              v (seal-for-batch! st [{:e :shape-node :k :shape-change :v "still old"}])]
          (doseq [a (in-one-batch! ipc st [g v])] (yes! a))
          (is (= :value-shape
                 (get-in (mc/write! st {:who :operator :layer :group :session :six-b
                                        :facts [{:e :shape-node :k :shape-change :v "now wrong"}]})
                         [:answer :reason])))))

      (testing "hint changes count all offered uses in either order"
        (doseq [grammar-first? [true false]]
          (let [k (if grammar-first? :hints-before :hints-after)
                g-before (when grammar-first? (op [(gfact k (assoc plain-grammar :index #{:by-value}))]))
                v (seal-for-batch! st [{:e k :k k :v "batch use"}])
                g (or g-before (op [(gfact k (assoc plain-grammar :index #{:by-value}))]))
                [va ga] (in-one-batch! ipc st [v g])]
            (is (= grammar-first? (neg? (compare (nth (:name g) 3) (nth (:name v) 3)))))
            (yes! va)
            (is (= :grammar-change-needs-rebuild (:reason ga)))
            (is (= {:used true :grammar nil} (keyrow :group k)))))
        (let [k :missing-lock-use
              v (seal-for-batch! st [{:e k :k k :v "missing"}])
              ;; A valid but unowned lease id takes the missing-lock face path.
              bad (assoc-in v [:facts 0 :lock-id 1] 255)
              g (op [(gfact k (assoc plain-grammar :index #{:by-value}))])
              [va ga] (in-one-batch! ipc st [bad g])]
          (is (= :no-such-lock (:reason va)))
          (is (= :grammar-change-needs-rebuild (:reason ga)))
          (is (nil? (keyrow :group k)))))

      (testing "reclass falls back to stream rows, then micro rows take precedence"
        (let [r (write-value! st :base [{:e :inherited-node :k :inherited :v value}])]
          (is (= #{:alice} (:subjects (mc/record-of st (get-in r [:offer :name])))))
          (is (= [value] (mapv :value (:rows (rd st :base [:kv :inherited value]))))))
        (grammar! st :base :inherited right-grammar)
        (let [r (write-value! st :base [{:e :inherited-node-2 :k :inherited :v value}])]
          (is (= #{:bob} (:subjects (mc/record-of st (get-in r [:offer :name]))))))
        (is (= :grammar-change-needs-rebuild
               (:reason (mc/offer! st (mc/build {:who :operator :layer :base
                                                :facts [(gfact :inherited (assoc right-grammar :index #{}))]}))))))

      (testing "custom hints reach openings, deltas, opaque reads and rebuilds"
        (grammar! st :group :private (assoc plain-grammar :index #{:by-value :no-copy}))
        (grammar! st :group :opaque (assoc plain-grammar :opaque true))
        (let [opaque (write-value! st :group [{:e :opaque-node :k :opaque :v {:anything "opaque"}}])
              nm (get-in opaque [:offer :name])
              point (rx/read! st (merge reader {:layer :group :read [:point [[nm 0]]]}))]
          (is (true? (:opaque (first (:rows point)))))
          (is (true? (:opaque (first (:rows (rd st :group [:k :opaque]))))))
          (is (= :opaque (:refused (rd st :group [:kv :opaque {:anything "opaque"}])))))
        (let [opening (sg/subscribe! st (merge reader {:layer :group :pattern [:kv :private "secret"]}))
              r (write-value! st :group [{:e :private-node :k :private :v "secret"}])
              nm (get-in r [:offer :name])
              got (sg/deliver! st (:handle opening))
              entries #(foreign-select [(keypath :group :ix-ek) MAP-VALS] (:micro st))
              mine #(first (filter (fn [e] (= [nm 0] (:fid e))) (entries)))]
          (is (:handle opening) (pr-str opening))
          (is (= ["secret"] (mapv :value (:rows got))) (pr-str got))
          (is (false? (:copy (mine))))
          (is (nil? (:sealed (mine))))
          (is (pos? (:pages (rx/rebuild-micro! st))))
          (is (= ["secret"] (mapv :value (:rows (rd st :group [:kv :private "secret"]))))
              "put and sweep both use the custom grammar")
          (is (false? (:copy (mine))))
          (is (nil? (:sealed (mine))))
          (is (= :opaque (:refused (rd st :group [:kv :opaque {:anything "opaque"}]))))))

      (testing "a landing names subjects under the target grammar"
        (grammar! st :group :landed right-grammar)
        (let [source (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                               :facts [{:e :landing-source :k :landed :v value}]})
              _ (yes! (c/offer-until-answered! st source))
              p (pc/promote! st {:who :alice :layer :alice :source [(:name source) 0]
                                 :target :group :class :by-entity :session :six-b-landing
                                 :landing-permission [:alice :group :group [:group :group :group]]})
              status (wait-until #(let [s (pc/promotion-status st :alice (:name p) nil)]
                                   (when (#{:done :refused} (:status s)) s)))
              landing (env/landing-name (:name p) :group :by-entity)]
          (is (= :done (:status status)) (pr-str status))
          (when-let [b (:batch (mc/record-of st landing))] (settle! st b))
          (is (= #{:bob} (:subjects (mc/record-of st landing))))
          (is (= [:bob] (get-in (mc/row-of st :landing-source [landing 0]) [:lock :any-of])))
          (is (= [value] (mapv :value (:rows (rd st :group [:kv :landed value]))))))))))
