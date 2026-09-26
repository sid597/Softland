(ns rig.store.read-exit-test
  "The one-owner read exit on an in-process cluster of 4 tasks
  (PLAN-read-exit.md, 'Namespaces and tests'): T2 to T6, T8 to T16, T18 and
  T19, and T11's crashes last, since each restarts a worker. One cluster,
  seeded with the model's one-owner world plus the layers these tests need;
  scenarios use their own entities or layers, so they do not interfere.

  Every read goes through `read!`, recorded by the helper `rd`, and a global
  check at the end asserts that every read that returned rows has its
  entry's answer :yes (T11). Crashes are injected through rig.store.inject
  (R3); tests assert 'at least once', never a replay count (R4).

  Wave 1 (phases 2 and 3 merged): values are sealed at the door and opened
  by phase 2's open step, so what the log implies is computed from the
  store's own opening (`read-as-of`, the internal view), index entries are
  compared with their bytes as vectors (a byte array compares by
  identity), and T19 erases with a real value forget instead of the
  pass-through's test double, which the merge removed."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.toy-grammars :as tg]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]))

;; ------------------------------------------------------------------ helpers

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- retrying
  "A foreign call, retried while a worker restarts after an injected crash."
  [f]
  (let [deadline (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [r (try [:ok (f)] (catch Exception e [:err e]))]
        (cond (= :ok (first r)) (second r)
              (> (System/currentTimeMillis) deadline) (throw (second r))
              :else (do (Thread/sleep 100) (recur)))))))

(defn- wait-until
  ([f] (wait-until f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 50) (recur))))))))

(defn- act
  "`who`'s act into `layer`, citing [who layer layer] unless the operator."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(def alice
  {:reader :alice :reader-kind :person :working :alice-hand :permission [:alice :alice-hand :alice-hand]})

(defonce shown (atom []))

(defn- rd
  "Read through the exit, as Alice unless the spec says otherwise; every
  read that returns rows is remembered for the global check."
  [st spec]
  (let [r (rx/read! st (merge alice spec))]
    (when (contains? r :rows) (swap! shown conj (:entry r)))
    r))

(defn- line
  "The read entry's one line, as recorded in the working layer."
  [st working entry-name]
  (first (retrying #(c/facts st working entry-name))))

(defn- bytes->vec
  "Data with every byte array as a vector of its bytes, so two reads of the
  same stored bytes compare equal (wave 1: entries carry sealed values)."
  [x]
  (walk/postwalk #(if (bytes? %) (vec %) %) x))

(defn- field
  "One index field of a layer, whole, as {address-or-fid value}, its bytes
  as vectors."
  [st layer f]
  (bytes->vec (into {} (retrying #(foreign-select [(keypath layer f) ALL] (:layers st))))))

(defn- fields [st layer] (into {} (for [f [:ix-ek :ix-ke :ix-kv :ix-of]] [f (field st layer f)])))

(defn- open-result
  "One fact of `read-as-of` as the open step's result for it."
  [f]
  (cond (contains? f :value) {:value (:value f) :stamp (:stamp f)}
        (contains? f :erased-at) {:erased-at (:erased-at f)}
        :else {:unreadable (:unreadable f)}))

(defn- log-acts
  "A layer's yes acts as `reads/implied` takes them, read by foreign-select,
  each row with the result of the store's own open step now (wave 1: phase
  2's, through the internal `read-as-of`, which opens every row as the
  rebuild does)."
  [st layer]
  (let [opened (into {} (map (fn [f] [(:id f) (open-result f)]))
                     (:facts (retrying #(c/read-as-of st layer Long/MAX_VALUE))))]
    (vec (for [[nm rec] (retrying #(foreign-select [(keypath layer :answers) ALL] (:layers st)))
               :when (= :yes (:answer rec))
               :let [rows (retrying #(foreign-select [(keypath layer :log nm) ALL] (:layers st)))]]
           {:name nm :stamp (:stamp rec) :rows rows
            :opens (vec (for [i (range (count rows))] (get opened [nm i] {:unreadable :no-such-fact})))}))))

(defn- implied [st layer] (bytes->vec (reads/implied reads/seed-hints layer (log-acts st layer))))

(defn- q-pattern [st layer for pattern & [as-of limit]]
  (retrying #(foreign-invoke-query (:read-pattern st) layer for pattern as-of limit)))

(defn- q-point [st layer for fids & [as-of]]
  (retrying #(foreign-invoke-query (:read-point st) layer for fids as-of)))

(defn- fids-of [r] (mapv :fid (:rows r)))

(defn- contains-anywhere?
  "Whether x appears as a value anywhere inside data."
  [data x]
  (boolean (some #(= x %) (tree-seq coll? seq data))))

(defn- make-layer! [st layer kind owner & grants]
  (is (= :yes (:answer (c/offer-until-answered! st (c/make-layer-offer layer {:kind kind :owner owner})))) (str layer))
  ;; phase 6: the toy grammars as facts in the layer (D-P4)
  (is (every? #(= :yes (:answer %)) (tg/write! st [layer])) (str "grammars in " layer))
  (doseq [pid grants]
    (is (= :yes (:answer (c/offer-until-answered! st (c/grant-offer st pid)))) (pr-str pid))))

;; --------------------------------------------------------------- the cluster

(deftest read-exit
  (inject/reset-all!)
  (reset! shown [])
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (rx/connect ipc)
          send! (fn [o] (c/offer-until-answered! st o))
          ok! (fn [o] (let [a (send! o)] (is (= :yes (:answer a)) (pr-str (:facts o))) a))]

      (testing "seed: the model's one-owner world, Bob's layers, the base, an agent's and a tool's permissions"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        ;; phase 6: the toy grammars as facts in the seeded layers (D-P4)
        (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-hand :alice-agent])))
        (make-layer! st :bob :personal :bob [:bob :bob :bob])
        (make-layer! st :bob-hand :hand :bob [:bob :bob-hand :bob-hand])
        (make-layer! st :base :base :operator)
        (doseq [pid [[:agent-a :alice-agent :alice-agent] [:tool-t :alice-agent :alice-agent]]]
          (is (= :yes (:answer (send! (c/grant-offer st pid)))))))

      (testing "T2: the entry is acknowledged before the answer returns"
        (ok! (act :alice :alice [{:e :t2 :k :note :v "two"}]))
        (let [nm (env/make-name :alice-hand :by-layer)]
          (inject/watch! nm)
          (let [r (rd st {:layer :alice :read [:pattern [:e :t2]] :entry-name nm})
                points (mapv first (filter #(= nm (second %)) @inject/passes))]
            (say "T2 points for the entry:" points)
            (is (= ["two"] (mapv :value (:rows r))))
            (is (< (.indexOf points :after-writes) (.indexOf points :exit-shown))
                "the gate committed the entry before the exit showed anything")
            (is (<= 0 (.indexOf points :after-writes)))
            (is (= :yes (:answer (c/lookup st nm nil))))
            (is (= (:entry-stamp r) (:stamp (c/record st nm)))))))

      (testing "T3: a refused entry hides the answer"
        (make-layer! st :alice-h2 :hand :alice [:alice :alice-h2 :alice-h2])
        (let [spec {:layer :alice :read [:pattern [:e :t2]] :working :alice-h2 :permission [:alice :alice-h2 :alice-h2]}]
          (is (contains? (rd st spec) :rows) "answered while the permission stands")
          (is (= :yes (:answer (send! (retrying #(c/revoke-offer st [:alice :alice-h2 :alice-h2]))))))
          (let [r (rd st spec)]
            (is (= :permission-revoked (:refused r)))
            (is (some? (:entry r)))
            (is (not (contains? r :rows)))
            (is (= :no (:answer (c/lookup st (:entry r) nil))) "the refusal is the gate's, recorded under the entry's name")))
        (let [r (rd st {:layer :alice :read [:pattern [:e :t2]] :working :alice-nowhere
                        :permission [:alice :alice-nowhere :alice-nowhere]})]
          ;; wave 1: the entry is sealed at the door, so it leases first; a lease into a
          ;; layer never made is refused (recorded under the lease's name) and the entry,
          ;; citing no leased lock, is refused on its face (RIG.md For Sid 14)
          (is (= :no-such-lock (:refused r)) "the entry's lease was refused :no-such-layer, the entry :no-such-lock")
          (is (not (contains? r :rows)))))

      (testing "T4: an empty pattern read is recorded, with count 0, complete, and the empty set's fingerprint"
        (let [r (rd st {:layer :alice :read [:pattern [:e :nobody]]})
              l (line st :alice-hand (:entry r))]
          (is (= [] (:rows r)))
          (is (= :read/pattern (:k l)))
          (is (= [0 :complete (reads/fingerprint #{})] ((juxt :count :mark :fingerprint) (:v l))))
          (is (= [] (:exact (:v l))) "a person's line carries its (empty) exact list")
          (is (= [:shown [:e :nobody] :alice] ((juxt :role :pattern :layer) (:v l))) "the role, by default :shown; the pattern; the layer"))
        (let [r (rd st {:layer :alice :read [:pattern [:e :nobody]] :role :stood-on})]
          (is (= :stood-on (:role (:v (line st :alice-hand (:entry r))))) "a role given is recorded")))

      (testing "T5: complete and partial marks, and nothing of the entry past the limit"
        (let [admitted (vec (for [i [4 3 2 1 0]]
                              (let [o (act :alice :alice [{:e :t5 :k (keyword (str "n" i)) :v (str "five-" i)}])]
                                [i (:name o) (:stamp (ok! o))])))
              by-key (into {} (map (fn [[i n s]] [i [[n 0] s]]) admitted))
              [fourth-fid fourth-stamp] (by-key 3)
              r5 (rd st {:layer :alice :read [:pattern [:e :t5]] :limit 5})
              r3 (rd st {:layer :alice :read [:pattern [:e :t5]] :limit 3})
              r4 (rd st {:layer :alice :read [:pattern [:e :t5]] :limit 4})]
          (is (= [5 :complete] [(count (:rows r5)) (:mark r5)]))
          (is (= [3 :partial] [(count (:rows r3)) (:mark r3)]))
          (is (= [4 :partial] [(count (:rows r4)) (:mark r4)]))
          (is (= [:n0 :n1 :n2] (mapv :k (:rows r3))) "the first three in address order")
          (is (not (contains-anywhere? r3 fourth-fid)) "F3: the fourth's fact id is nowhere in the return")
          (is (not (contains-anywhere? r3 fourth-stamp)) "nor its stamp")
          (doseq [[r n mark] [[r5 5 :complete] [r3 3 :partial] [r4 4 :partial]]]
            (is (= [n mark] ((juxt :count :mark) (:v (line st :alice-hand (:entry r)))))
                "the entry line carries each mark and count")))
        (testing "F8: 500 facts on :e1 after s0 and one on :e0 at s0: [:all] as of s0, limit 10, is partial"
          (make-layer! st :t5b :personal :alice [:alice :t5b :t5b])
          (let [s0 (:stamp (ok! (act :alice :t5b [{:e :e0 :k :note :v "at s0"}])))
                _ (ok! (act :alice :t5b (vec (for [i (range 500)] {:e :e1 :k :note :v (str "later-" i)}))))
                r (rd st {:layer :t5b :read [:pattern [:all]] :as-of s0 :limit 10})
                all-now (q-pattern st :t5b :alice [:all] s0 10000)]
            (is (= :partial (:mark r)) "the scan budget ran out")
            (is (= ["at s0"] (keep #(when (= :e0 (:e %)) (:value %)) (:rows r))))
            (is (= :complete (:mark all-now)) "with a budget of 16 × 10,001 the same read completes")
            (is (< (count (:rows r)) (count (:rows all-now)))
                "the partial read stopped before the facts after the 500"))))

      (testing "T6: the fingerprint changes with a matched fact, not with an unmatched one"
        (let [f1 (act :alice :alice [{:e :t6 :k :note :v "first"}])
              _ (ok! f1)
              mention (act :alice :alice [{:e :t6 :k :mention :v {:persons #{:bob}}}])
              _ (ok! mention)
              r1 (rd st {:layer :alice :read [:pattern [:ek :t6 :note]]})
              m1 (:stamp (:moment r1))
              _ (ok! (act :alice :alice [{:e :t6 :k :note :v "second" :replaces [(:name f1) 0]}]))
              r2 (rd st {:layer :alice :read [:pattern [:ek :t6 :note]]})
              _ (ok! (act :alice :alice [{:e :t6x :k :note :v "elsewhere"}]))
              r3 (rd st {:layer :alice :read [:pattern [:ek :t6 :note]]})
              _ (ok! (act :alice :alice [{:e :t6 :k :mention :v {:persons #{:alice}} :replaces [(:name mention) 0]}]))
              r4 (rd st {:layer :alice :read [:pattern [:ek :t6 :note]]})
              r5 (rd st {:layer :alice :read [:pattern [:ek :t6 :note]] :as-of m1})]
          (is (= 1 (count (:rows r1))))
          (is (= 2 (count (:rows r2))))
          (is (not= (:fingerprint r1) (:fingerprint r2)) "a replace adds a matched fact")
          (is (= (:fingerprint r2) (:fingerprint r3)) "a fact on another entity changes nothing")
          (is (= (:fingerprint r2) (:fingerprint r4)) "a replace in an unmatched chain changes nothing")
          (is (= (:fingerprint r1) (:fingerprint r5)) "the first moment again gives the first fingerprint")
          (is (= (reads/fingerprint (:matched r2)) (:fingerprint r2)) "over the ids and stamps that matched")))

      (testing "T8: as-of reads show nothing admitted after the moment, except an erasure's date"
        (let [f1 (act :alice :alice [{:e :t8 :k :note :v "t8 one"}])
              s1 (:stamp (ok! f1))
              f2 (act :alice :alice [{:e :t8 :k :note :v "t8 two" :replaces [(:name f1) 0]}])
              s2 (:stamp (ok! f2))
              fid1 [(:name f1) 0] fid2 [(:name f2) 0]]
          (is (= [fid1] (fids-of (rd st {:layer :alice :read [:pattern [:ek :t8 :note]] :as-of s1}))))
          (is (= [fid1] (fids-of (rd st {:layer :alice :read [:pattern [:latest :t8 :note]] :as-of s1}))))
          (let [never [(env/make-name :alice :by-layer) 0]
                past [(:name f1) 5]
                r (rd st {:layer :alice :read [:point [fid2 never past fid1]] :as-of s1})]
            (is (= [{:fid fid2 :absent true} {:fid never :absent true} {:fid past :absent true}] (take 3 (:rows r)))
                "admitted after the moment, never admitted, past the act's rows: absent alike (RD2)")
            (is (= "t8 one" (:value (last (:rows r)))))
            (is (= [:absent :absent :absent :value] (mapv (comp :shown :v) (retrying #(c/facts st :alice-hand (:entry r)))))
                "the entry records each fact id read and how it came out")
            (is (= [[fid1 s1]] (:matched r))))
          (is (= [] (:rows (rd st {:layer :alice :read [:pattern [:all]] :as-of 0}))) "RD4: as of before anything, nothing")
          (let [all (rd st {:layer :alice :read [:pattern [:all]] :as-of s1})]
            (is (every? #(<= (:stamp %) s1) (:rows all)))
            (is (= {:stamp s1} (:moment all))))
          (is (= [fid1 fid2] (fids-of (rd st {:layer :alice :read [:pattern [:ek :t8 :note]]}))))
          (is (= [fid2] (fids-of (rd st {:layer :alice :read [:pattern [:latest :t8 :note]]}))))
          (testing "FR2: a read asked for a moment past the clock records the clock"
            (let [before (c/clock st :alice)
                  asked (+ before (bit-shift-left 1 40))
                  r (rd st {:layer :alice :read [:pattern [:e :t8]] :as-of asked})
                  m (:stamp (:moment r))]
              (is (< m asked))
              (is (<= before m (c/clock st :alice)))
              (is (= {:stamp m} (:moment (:v (line st :alice-hand (:entry r))))))))
          (testing "the erasure exception: after a purge dated s3 > s2, a read as of s1 shows f1 with that date and no value"
            (let [s3 (+ s2 (bit-shift-left 1 20))]
              (is (= {:purged true} (rx/index-op! st {:layer :alice :op :purge :fid fid1 :forget-stamp s3})))
              (let [r (rd st {:layer :alice :read [:pattern [:ek :t8 :note]] :as-of s1})
                    [row] (:rows r)]
                (is (= 1 (count (:rows r))))
                (is (= [fid1 s3] [(:fid row) (:erased-at row)]))
                (is (not (contains? row :value)))
                (is (not (contains-anywhere? r "t8 one")))
                (is (not (contains-anywhere? r fid2)) "nothing else from after s1"))))))

      (testing "T9: a purge by value id removes the value from every index"
        (let [secret "t9 distinctive value"
              f (act :alice :alice [{:e :t9 :k :note :v secret} {:e :t9 :k :tag :v 1}])
              _ (ok! f)
              fid [(:name f) 0]
              before (rd st {:layer :alice :read [:pattern [:kv :note secret]]})
              _ (is (= [fid] (fids-of before)))
              line-before (line st :alice-hand (:entry before))
              forget (+ (c/clock st :alice) 1000)
              _ (is (= {:purged true} (rx/index-op! st {:layer :alice :op :purge :fid fid :forget-stamp forget})))
              fs (fields st :alice)
              text (env/encode-value secret)]
          (let [after (rd st {:layer :alice :read [:pattern [:kv :note secret]]})]
            (is (not= (:fingerprint before) (:fingerprint after))
                "RD6: a re-run after the forget cannot reach the first fingerprint, as expected")
            (is (= line-before (line st :alice-hand (:entry before))) "E8: the earlier entry is never rewritten"))
          (is (not-any? #(str/includes? % text) (keys (:ix-kv fs))) "no value entry holds it")
          (is (not (contains? (:ix-of fs) fid)) "no :ix-of entry for it")
          (doseq [f [:ix-ek :ix-ke]]
            (let [es (filter #(= fid (:fid %)) (vals (f fs)))]
              (is (= 1 (count es)) (str f))
              (is (every? #(and (= forget (:erased-at %)) (not (contains? % :v))) es) (str f ": a tombstone"))))
          (is (= [] (:rows (rd st {:layer :alice :read [:pattern [:kv :note secret]]}))) "[:kv] no longer matches it")
          (let [e (rd st {:layer :alice :read [:pattern [:e :t9]]})]
            (is (= [[fid forget nil] [[(:name f) 1] nil 1]]
                   (mapv (juxt :fid :erased-at :value) (:rows e)))
                "[:e] shows it with its erasure date only; the act's other fact is untouched"))
          (is (not (str/includes? (pr-str fs) text)) "a scan of the four fields finds the value's text nowhere"))
        (testing "a purge while a paged read runs: the read shows the value or its erasure date, one or the other"
          (make-layer! st :t9b :personal :alice [:alice :t9b :t9b])
          (let [big (act :alice :t9b (vec (for [i (range 1500)] {:e :e0 :k :note :v (str "page " i)})))
                _ (ok! big)
                target [(:name big) 1200]
                reads (future (q-pattern st :t9b :alice [:e :e0] nil 10000))
                _ (rx/index-op! st {:layer :t9b :op :purge :fid target :forget-stamp 4242})
                r @reads
                row (first (filter #(= target (:fid %)) (:rows r)))]
            (say "purge during a paged read: the row shows" (if (contains? row :erased-at) "the erasure date" "the value"))
            (is (= 1500 (count (:rows r))))
            (is (or (= "page 1200" (:value row)) (and (= 4242 (:erased-at row)) (not (contains? row :value))))))))

      (testing "T10: a rebuild from the log reproduces the indexes exactly"
        (make-layer! st :t10 :personal :alice [:alice :t10 :t10])
        (let [a1 (act :alice :t10 [{:e :e0 :k :note :v "a"} {:e :e1 :k :mention :v {:persons #{:bob}}}])
              _ (ok! a1)
              _ (ok! (act :alice :t10 [{:e :e0 :k :note :v "b" :replaces [(:name a1) 0]}]))
              _ (ok! (act :alice :t10 [{:e :e1 :k :mention :v nil :replaces [(:name a1) 1]}]))
              _ (ok! (act :alice :t10 [{:e :t10 :k :lock-grain :v :per-act}]))
              _ (rd st {:layer :t10 :read [:pattern [:all]] :working :t10 :permission [:alice :t10 :t10]})
              _ (rd st {:layer :t10 :read [:point [[(:name a1) 0]]] :working :t10 :permission [:alice :t10 :t10]})
              snap (fields st :t10)]
          (is (= snap (implied st :t10)) "the fields equal what the log implies")
          (is (seq (:ix-kv snap)))
          (let [r (rx/rebuild! st :t10 :acts 2 :entries 3)]
            (say "T10 rebuild over an intact layer:" (dissoc r :sweeps))
            (is (= [0 0] [(:deleted r) (:rewritten r)]) "deletes nothing, rewrites nothing")
            (is (< 1 (:put-pages r)) "the small pages span the history")
            (is (= snap (fields st :t10))))
          (testing "dropped to empty, then rebuilt: entry for entry the snapshot"
            (doseq [f [:ix-ek :ix-ke :ix-kv :ix-of]]
              (loop [] (when (pos? (:dropped (rx/index-op! st {:layer :t10 :op :drop :field f :entries 512}))) (recur))))
            (is (= {:ix-ek {} :ix-ke {} :ix-kv {} :ix-of {}} (fields st :t10)))
            (rx/rebuild! st :t10 :acts 2 :entries 3)
            (is (= snap (fields st :t10))))
          (testing "a stale entry at an address no act implies is deleted by the sweep"
            (let [stale (str "zz" reads/sep "note" reads/sep "stale")]
              (rx/index-op! st {:layer :t10 :op :put :field :ix-ek :address stale :fid [(:name a1) 0] :stamp 5 :e :zz :k :note})
              (is (contains? (field st :t10 :ix-ek) stale))
              (let [r (rx/rebuild! st :t10 :acts 2 :entries 3)]
                (is (= 1 (:deleted r))))
              (is (= snap (fields st :t10)))))
          (testing "an offer admitted between two pages keeps its entries"
            (let [p1 (rx/index-op! st {:layer :t10 :op :rebuild-put :after nil :acts 2})
                  mid (act :alice :t10 [{:e :e2 :k :note :v "between pages"}])]
              (is (false? (:done? p1)))
              (ok! mid)
              (loop [after (:next p1)]
                (let [p (rx/index-op! st {:layer :t10 :op :rebuild-put :after after :acts 2})]
                  (when-not (:done? p) (recur (:next p)))))
              (doseq [f [:ix-ek :ix-ke :ix-kv :ix-of]]
                (loop [after nil]
                  (let [p (rx/index-op! st {:layer :t10 :op :rebuild-sweep :field f :after after :entries 3})]
                    (when-not (:done? p) (recur (:next p))))))
              (let [now (fields st :t10)]
                (is (= now (implied st :t10)))
                (is (some #(= [(:name mid) 0] (:fid %)) (vals (:ix-ek now)))))))
          (testing "a page sent twice (as a resend after an append error is) changes nothing"
            (let [before (fields st :t10)
                  op {:layer :t10 :op :rebuild-sweep :field :ix-ek :after nil :entries 3}]
              (is (= (rx/index-op! st op) (rx/index-op! st op)))
              (is (= (rx/index-op! st {:layer :t10 :op :rebuild-put :after nil :acts 2})
                     (rx/index-op! st {:layer :t10 :op :rebuild-put :after nil :acts 2})))
              (is (= before (fields st :t10)))))))

      (testing "T12: index writes replay to the same entries"
        (make-layer! st :t12 :personal :alice [:alice :t12 :t12])
        (ok! (act :alice :t12 [{:e :e0 :k :note :v "before the crash"}]))
        (let [o (act :alice :t12 [{:e :e0 :k :note :v "crashed after its writes"} {:e :e1 :k :tag :v 2}])
              nm (:name o)]
          (inject/watch! nm)
          (inject/arm! :after-writes nm)
          (try (c/offer! st o) (catch Exception _ nil))
          (is (map? (wait-until #(let [r (c/lookup st nm (env/offer-digest o))] (when (map? r) r)) 120000)))
          (is (= 1 (inject/fired-count :after-writes nm)) "the crash fired")
          (let [now (fields st :t12)]
            (is (= now (implied st :t12)) "after the replay the fields equal what the log implies")
            (doseq [f [:ix-ek :ix-ke]]
              (is (= 2 (count (filter #(= nm (first (:fid %))) (vals (f now))))) (str f ": each fact once")))
            (is (= 1 (count (filter #(= nm (first %)) (map first (:ix-of now)))))))))

      (testing "T13: visibility is decided for the person the read is for"
        (let [bob {:reader :bob :reader-kind :person :working :bob-hand :permission [:bob :bob-hand :bob-hand]}
              nm (env/make-name :bob-hand :by-layer)
              r (rx/read! st (merge bob {:layer :alice :read [:pattern [:all]] :entry-name nm}))]
          (is (= {:refused :not-visible} r))
          (is (= :no-answer (c/lookup st nm nil)) "nothing recorded: nothing was read")
          (ok! (act :operator :base [{:e :b0 :k :note :v "in the base"}]))
          (let [b (rx/read! st (merge bob {:layer :base :read [:pattern [:e :b0]]}))]
            (swap! shown conj (:entry b))
            (is (= ["in the base"] (mapv :value (:rows b))) "the base is read by anyone")))
        (let [agent {:reader :agent-a :for :alice :reader-kind :model :working :alice-agent
                     :permission [:agent-a :alice-agent :alice-agent]}
              r1 (rd st (merge agent {:layer :alice :read [:pattern [:e :t2]]}))
              r2 (rd st (merge agent {:layer :alice-agent :read [:pattern [:all]]}))]
          (is (contains? r1 :rows) "F12: a model reader for Alice reads her personal layer")
          (is (contains? r2 :rows) "and her agent session layer")
          (is (= :agent-a (:who (c/record st (:entry r1)))) "its entry's :who stays the agent")
          (is (= :alice-agent (nth (:entry r1) 0)))
          (is (= {:refused :not-visible} (rx/read! st (merge agent {:for :bob :layer :alice :read [:pattern [:all]]}))))))

      (testing "T15, T16: agent session reads recorded there; readers' kinds decide the exact list"
        (let [agent {:reader :agent-a :for :alice :reader-kind :model :working :alice-agent
                     :permission [:agent-a :alice-agent :alice-agent]}
              tool {:reader :tool-t :for :alice :reader-kind :tool :working :alice-agent
                    :permission [:tool-t :alice-agent :alice-agent]}
              pat {:layer :alice :read [:pattern [:e :t5]]}
              lm (line st :alice-agent (:entry (rd st (merge agent pat))))
              lp (line st :alice-hand (:entry (rd st pat)))
              lt (line st :alice-agent (:entry (rd st (merge tool pat))))
              lr (line st :alice-agent (:entry (rd st (merge tool pat {:rows? true}))))]
          (is (= 5 (count (:exact (:v lm)))) "T15: a model's line in the agent session layer carries the exact list")
          (is (= 5 (count (:exact (:v lp)))) "a person's too")
          (is (not (contains? (:v lt) :exact)) "a tool's line does not")
          (is (= 5 (:count (:v lt))))
          (is (= 5 (count (:exact (:v lr)))) "a tool that asks for rows gets them")
          (let [fs (vec (take 3 (map :fid (:rows (rd st pat)))))
                r (rd st {:layer :alice :read [:point fs]})
                rows (retrying #(c/facts st :alice-hand (:entry r)))]
            (is (= 3 (count rows)) "a point read of three fact ids records three rows")
            (is (every? #(= :read/point (:k %)) rows))
            (is (= fs (mapv (comp :fid :v) rows)))
            (is (every? #(= :value (:shown (:v %))) rows))
            (is (not-any? #(contains? (:v %) :value) rows) "a row never carries the value"))))

      (testing "T19: F4, F5, F7"
        (is (= {:refused :not-visible}
               (rx/read! st {:reader :bob :reader-kind :person :working :bob-hand :permission [:bob :bob-hand :bob-hand]
                             :layer :alice-never-made :read [:pattern [:all]]}))
            "F4: a layer never made answers as a private one")
        (let [fa (act :alice :alice [{:e :t19 :k :note :v "t19 same"}])
              fb (act :alice :alice [{:e :t19 :k :note :v "t19 same"}])
              sa (:stamp (ok! fa))
              sb (:stamp (ok! fb))
              fa-id [(:name fa) 0]
              kv-a (reads/address :ix-kv {:k :note :vtext (env/encode-value "t19 same") :stamp sa :fid fa-id})
              ;; wave 1: a real value forget of fa (in place of the pass-through's test double)
              forgot (c/forget-value! st :alice :alice fa-id)]
          (is (= [:yes :row-deleted] ((juxt :answer :how) forgot)))
          (is (not (contains? (field st :alice :ix-kv) kv-a)) "the forget purged fa's value entry in its own event")
          ;; F5 needs a value-index entry whose value no longer opens: plant fa's, a copy of its row
          (is (= {:put true} (rx/index-op! st {:layer :alice :op :put :field :ix-kv :address kv-a :fid fa-id
                                               :stamp sa :e :t19 :k :note :copy? true})))
          (let [r (rd st {:layer :alice :read [:pattern [:kv :note "t19 same"]]})
                l (line st :alice-hand (:entry r))]
            (is (= [[(:name fb) 0]] (fids-of r)) "F5: the candidate that no longer opens is not shown")
            (is (= 1 (:count (:v l))) "nor counted")
            (is (= (reads/fingerprint #{[[(:name fb) 0] sb]}) (:fingerprint r)) "nor in the fingerprint"))
          (let [p (rd st {:layer :alice :read [:point [fa-id]]})]
            (is (= [(:stamp forgot) false] ((juxt :erased-at #(contains? % :value)) (first (:rows p))))
                "E8 R3: a point read of an erased fact shows the forget's date, no value")
            (is (= :erased (:shown (:v (line st :alice-hand (:entry p)))))))
          (loop [after nil]
            (let [pg (rx/index-op! st {:layer :alice :op :rebuild-sweep :field :ix-kv :after after :entries 512})]
              (when-not (:done? pg) (recur (:next pg)))))
          (is (not (contains? (field st :alice :ix-kv) kv-a))
              "a sweep deletes the planted entry: the log implies no value entry for an erased value"))
        (let [r (rd st {:layer :alice :read [:pattern [:e :t2]]})
              ent (reads/entry-entity (:entry r))
              es (filter #(= ent (:e %)) (vals (field st :alice-hand :ix-ek)))]
          (is (= 1 (count es)))
          (is (every? #(and (false? (:copy %)) (not (contains? % :v))) es) "F7: the line's id entries carry no copy")
          (let [k (rd st {:layer :alice-hand :read [:pattern [:k :read/pattern]]})
                mine (first (filter #(= ent (:e %)) (:rows k)))]
            (is (= [:e :t2] (:pattern (:value mine))) "[:k :read/pattern] still shows the line's value, from its row"))))

      (testing "D5: an act's facts in the act's order, and [:latest] picks its last, past index 9"
        (let [o (act :alice :alice (vec (for [i (range 12)] {:e :t20 :k :note :v (str "twelve " i)})))
              _ (ok! o)
              ek (rd st {:layer :alice :read [:pattern [:ek :t20 :note]]})
              lt (rd st {:layer :alice :read [:pattern [:latest :t20 :note]]})]
          (is (= (mapv #(vector (:name o) %) (range 12)) (fids-of ek)))
          (is (= [[(:name o) 11]] (fids-of lt)) "the model's chain-head: the last of the act")))

      (testing "D7: a layer re-classed by entity is not read here"
        (make-layer! st :t21 :personal :alice [:alice :t21 :t21])
        (ok! (act :alice :t21 [{:e :e0 :k :note :v "before the re-class"}]))
        (ok! (act :operator :t21 [{:e :t21 :k :class :v :by-entity}]))
        (is (= {:refused :re-classed} (rx/read! st (merge alice {:layer :t21 :read [:pattern [:all]]}))))
        (is (= {:refused :not-visible}
               (rx/read! st {:reader :bob :reader-kind :person :working :bob-hand :permission [:bob :bob-hand :bob-hand]
                             :layer :t21 :read [:pattern [:all]]}))
            "decided after visibility: Bob learns nothing of it"))

      (testing "T14: malformed reads and records are answered as data; no worker restarts"
        (let [n1 (env/make-name :alice :by-layer)
              c0 (count @inject/fired)
              ;; called directly, not retried: a throw in a query topology restarts the
              ;; worker and reaches the client as an exception, which must not happen
              direct (fn [f] (try (f) (catch Exception e {:threw (.getName (class e))})))]
          (doseq [[layer pattern as-of limit] [[:alice [:kv] nil nil] [:alice "all" nil nil] [:alice nil nil nil]
                                                [:alice [:all] -1 nil] [:alice [:all] "now" nil] [:alice [:all] nil 0]
                                                [:alice [:all] nil 10001] ["alice" [:all] nil nil] [42 [:all] nil nil]
                                                [:alice [:kv :note (loop [v 1 i 0] (if (< i 40) (recur [v] (inc i)) v))] nil nil]]]
            (let [r (direct #(foreign-invoke-query (:read-pattern st) layer :alice pattern as-of limit))]
              (is (= :bad-pattern (:refused r)) (pr-str [layer pattern as-of limit r]))))
          (doseq [[layer fids as-of] [[:alice [] nil] [:alice [n1] nil] [:alice "fids" nil] [:alice [[n1 0]] -3]
                                      ["alice" [[n1 0]] nil] [nil [[n1 0]] nil]]]
            (is (= {:refused :bad-read} (direct #(foreign-invoke-query (:read-point st) layer :alice fids as-of)))
                (pr-str [layer fids as-of])))
          (is (= {:refused :not-visible} (direct #(foreign-invoke-query (:read-pattern st) :alice "alice" [:all] nil nil)))
              "a :for that is not a keyword")
          (doseq [bad [42 "op" {} {:layer :alice :op :nope} {:layer :alice :op :rebuild-put :acts 0}
                       {:layer :alice :op :purge :fid "x" :forget-stamp 1} {:layer :alice :op :drop :field :zz :entries 1}]]
            (is (= {:refused :bad-op} (rx/index-op! st bad)) (pr-str bad)))
          (is (= {:refused :bad-read} (rx/read! st (merge alice {:layer :alice :read [:point []]}))) "D6")
          (is (= {:refused :bad-read} (rx/read! st (merge alice {:layer :alice :for :bob :read [:pattern [:all]]})))
              "D12: a person reads for itself")
          (is (= {:refused :bad-read} (rx/read! st (merge alice {:layer :alice :read [:pattern [:all]] :role :glanced}))))
          (is (= c0 (count @inject/fired)) "no crash was injected")
          (is (contains? (rd st {:layer :alice :read [:pattern [:e :t2]]}) :rows) "the queries and the gate keep answering")))

      (testing "T18: an entry decided on another task is stamped after its moment"
        (make-layer! st :t18 :personal :alice [:alice :t18 :t18])
        (let [ws (vec (for [i (range 8)] (keyword (str "t18w" i))))]
          (doseq [w ws] (make-layer! st w :hand :alice [:alice w w]))
          (let [c0 (c/clock st :t18)
                push (+ c0 (bit-shift-left 1 40))
                far (:stamp (ok! (act :alice :t18 [{:e :e0 :k :note :v "far ahead"}]
                                      :stood-on {[(env/make-name :t18 :by-layer) 0] push})))
                behind (filter #(< (c/clock st %) far) ws)
                w (first behind)]
            (say "T18 read layer clock" far "| working layers on other tasks:" (vec behind))
            (is (some? w) "a working layer whose home is another task")
            (when w
              (let [r (rd st {:layer :t18 :read [:pattern [:all]] :working w :permission [:alice w w]})
                    m (:stamp (:moment r))]
                (is (<= far m))
                (is (< m (:entry-stamp r)) "the entry is after its moment")
                (is (every? #(< (:stamp %) (:entry-stamp r)) (:rows r)) "and after every matched stamp")
                (is (<= (inc m) (c/clock st w)) "the working layer's task clock jumped past the moment"))))))

      ;; the crashes come last: each restarts a worker and replays records

      (testing "T11: crashes between the query and the entry, and between the entry and the answer"
        (ok! (act :alice :alice [{:e :t11 :k :note :v "crash cases"}]))
        (let [spec {:layer :alice :read [:pattern [:e :t11]]}
              pattern-lines (fn [nm]
                              (filter #(= (reads/entry-entity nm) (:e %))
                                      (:rows (q-pattern st :alice-hand :alice [:k :read/pattern]))))]
          (testing "(a) after the query: nothing shown, nothing recorded"
            (let [nm (env/make-name :alice-hand :by-layer)]
              (inject/arm! :exit-after-query nm)
              (is (thrown? Exception (rd st (assoc spec :entry-name nm))))
              (is (= :no-answer (c/lookup st nm nil)))
              (is (empty? (pattern-lines nm)))))
          (testing "(b) after the entry: nothing shown, the entry recorded"
            (let [nm (env/make-name :alice-hand :by-layer)]
              (inject/arm! :exit-after-entry nm)
              (is (thrown? Exception (rd st (assoc spec :entry-name nm))))
              (is (= :yes (:answer (c/lookup st nm nil))))
              (is (= 1 (count (pattern-lines nm))))))
          (testing "(b2) just before the return: as after the entry"
            (let [nm (env/make-name :alice-hand :by-layer)]
              (inject/arm! :exit-shown nm)
              (is (thrown? Exception (rd st (assoc spec :entry-name nm))))
              (is (= :yes (:answer (c/lookup st nm nil))))))
          (testing "(c) the gate crashes before the entry's writes: the worker restarts, the exit resends, rows shown, one entry"
            (let [nm (env/make-name :alice-hand :by-layer)]
              (inject/watch! nm)
              (inject/arm! :before-writes nm)
              (let [r (rd st (assoc spec :entry-name nm))]
                (is (= 1 (inject/fired-count :before-writes nm)) "the crash fired")
                (is (= ["crash cases"] (mapv :value (:rows r))))
                (is (= :yes (:answer (c/lookup st nm nil))))
                (is (= 1 (count (retrying #(c/facts st :alice-hand nm)))) "one line")
                (is (= 1 (count (retrying #(pattern-lines nm))))))))
          (testing "(d) the gate crashes after the entry's writes: the resend is answered from the record, rows shown"
            (let [nm (env/make-name :alice-hand :by-layer)]
              (inject/watch! nm)
              (inject/arm! :after-writes nm)
              (let [r (rd st (assoc spec :entry-name nm))]
                (is (= 1 (inject/fired-count :after-writes nm)) "the crash fired")
                (is (= ["crash cases"] (mapv :value (:rows r))))
                (is (= 1 (count (retrying #(c/facts st :alice-hand nm)))) "one line"))))))

      (testing "T11, across every test: every read that returned rows has its entry's answer :yes"
        (say "reads that returned rows:" (count @shown))
        (is (< 30 (count @shown)))
        (doseq [nm @shown]
          (is (= :yes (:answer (retrying #(c/lookup st nm nil)))) (pr-str nm)))))))
