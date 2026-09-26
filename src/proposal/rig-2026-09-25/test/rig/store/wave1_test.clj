(ns rig.store.wave1-test
  "Wave 1's seams, each asserted as the rulings require (phases 2 and 3
  merged onto the read exit; BUILD_NOTES-wave1.md). One in-process cluster
  of 4 tasks for the store's seams, and a pure test of the stamp seam.

  - A value forget purges the read exit's indexes in its own event (the
    forget ruling: gone for everyone including the past; the rig constraint:
    every index over values purgeable by value id, so a forget reaches
    it); a read through the exit afterwards cannot confirm the value.
  - A person forget purges every value that dies with the person, on every
    task, before its answer returns; a value that survives (7b) keeps its
    entries.
  - The exit opens sealed values through phase 2's open step, and time
    travel shows 'erased on this date' and nothing else from after the
    moment (the forget sharpening, I-L7).
  - The exit's read entries lease and seal at the door like any offer
    (default 1), and an entry is stamped after its moment though its value
    is sealed (ruling 4, the read exit's F1).
  - A group's making act passes with `:members`; a value forget in the
    group reaches its lock in the record (ruling 7: in the record for group
    layers; excision is the operator's), an `:own-row` value's lock row is
    deleted, and a per-act lock is forgotten as a whole across entities
    (ruling 7).
  - Persons are phase 2's acts on the micro side too.

  Crashes (R3) come last; tests assert 'at least once' (R4)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.toy-grammars :as tg]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ helpers

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

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

(defn- bytes->vec [x] (walk/postwalk #(if (bytes? %) (vec %) %) x))

(defn- act
  "`who`'s act into `layer`, citing [who layer layer] unless the operator."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- field
  "One index field of a layer, whole, {address-or-fid value}."
  [st layer f]
  (into {} (retrying #(foreign-select [(keypath layer f) ALL] (:layers st)))))

(defn- fields [st layer] (into {} (for [f [:ix-ek :ix-ke :ix-kv :ix-of]] [f (field st layer f)])))

(defn- entries-of
  "The id-index entries of fact `fid` in a layer's fields."
  [fs fid]
  (for [f [:ix-ek :ix-ke] e (vals (get fs f)) :when (= fid (:fid e))] e))

(defn- text-anywhere?
  "Whether `text` appears anywhere in the data (addresses and values), its
  bytes as numbers."
  [data text]
  (str/includes? (pr-str (bytes->vec data)) text))

(defn- fids-of [r] (mapv :fid (:rows r)))

(def alice {:reader :alice :reader-kind :person :working :alice-hand :permission [:alice :alice-hand :alice-hand]})

(defn- rd [st spec] (rx/read! st (merge alice spec)))

(defn- q-pattern [st layer for pattern & [as-of limit]]
  (retrying #(foreign-invoke-query (:read-pattern st) layer for pattern as-of limit)))

(def root [:group :group :group])
(defn- gp [p] [p :group :group root])
(defn- sp [s p] [s :group :group (gp p)])
(def base-root [:operator :base :base])
(defn- bp [p] [p :base :base base-root])

;; --------------------------------------------------- the stamp seam, pure

(defn- seal-raw
  "A plaintext offer sealed as the door seals it, under locks made up here.
  [raw delivered]."
  [raw]
  (let [lease [(:layer raw) :by-layer :offer (UUID/randomUUID)]
        idx (keep-indexed (fn [i f] (when (env/value-fact? f) i)) (:facts raw))
        ids (zipmap idx (map (fn [j] [lease (long j)]) (range)))
        Ks (into {} (map (fn [id] [id (locks/fresh-lock)])) (vals ids))]
    [(update raw :facts (fn [fs] (vec (map-indexed (fn [i f]
                                                     (if-let [id (get ids i)]
                                                       (-> f (dissoc :v) (assoc :sealed (locks/seal (get Ks id) (locks/canonical-bytes (:v f))) :lock-id id))
                                                       f))
                                                   fs))))
     Ks]))

(deftest a-sealed-read-entry-is-stamped-after-its-moment
  (testing "ruling 4 and the read exit's F1 under sealing: the gate reads an entry's moment from the value it opened"
    (let [wall (System/currentTimeMillis)
          moment (hlc/pack (+ wall 100000) 7)
          raw (-> {:version 1 :who :alice :layer :alice-hand :class :by-layer
                   :permission [:alice :alice-hand :alice-hand] :session :s1 :stood-on {} :subjects #{}
                   :facts [{:e :read-w1 :k :read/pattern
                            :v {:layer :alice :moment {:stamp moment} :role :shown :pattern [:all] :mark :complete
                                :count 0 :fingerprint "x" :fp-secret :read-fp/v1}}]}
                  (as-> r (assoc r :name (env/name-for r))))
          [sealed Ks] (seal-raw raw)
          o (:ok (env/parse sealed))
          settings {:kind :hand :owner :alice :class :by-layer :grain :per-value}
          persons {:alice {:lock (locks/fresh-lock) :erased-at nil}}
          rv (locks/value-context o settings Ks)
          lx (locks/lock-context Ks rv persons nil nil (locks/fresh-for o settings rv))
          d (gate/decide o settings {[:alice :alice-hand :alice-hand] {:granted [(:name raw) 0]}} {} 0 wall (env/digest o) lx)]
      (is (some? o) "the entry parses once sealed")
      (is (nil? (:v (first (:facts o)))) "the gate holds no plaintext of the entry before it opens it")
      (is (= :yes (get-in d [:record :answer])))
      (is (< moment (:stamp d)) "stamped after the moment the sealed line records")
      (is (> moment (gate/stamp-for o {} 0 wall)) "without the opened value the moment would not count: the seam matters")
      (is (= (:stamp d) (gate/stamp-for o {} 0 wall (:values (:read lx))))))))

;; ------------------------------------------------------------- the cluster

(deftest wave1-seams
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (merge (mc/connect ipc) (select-keys (rx/connect ipc) [:read-point :read-pattern :index-ops]))
          send! (fn [o] (c/offer-until-answered! st o))
          ok! (fn [o] (let [a (send! o)] (is (= :yes (:answer a)) (pr-str (:facts o))) a))
          layer! (fn [layer kind owner]
                   (ok! (c/make-layer-offer layer {:kind kind :owner owner}))
                   (ok! (c/grant-offer st [owner layer layer]))
                   ;; phase 6: the toy grammars as facts in the layer (D-P4)
                   (doseq [a (tg/write! st [layer])] (is (= :yes (:answer a)))))]

      (testing "seed: the one-owner world, the base (the micro world's making act, default 6), Carol and Dan"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (every? #(= :yes (:answer %)) (mc/make-base! st)))
        ;; phase 6: the toy grammars as facts in the seeded layers and the base (D-P4)
        (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-hand :alice-agent :base])))
        (doseq [p [:carol :dan]] (is (= :yes (:answer (mc/make-person! st p))) (str "made " p " by phase 2's act"))))

      (testing "persons are phase 2's acts on the micro side: the entry is on every task before the answer"
        (let [a (mc/make-person! st :w1-ann)
              task-keys (for [i (range 64)] (keyword (str "k" i)))]
          (is (= :yes (:answer a)))
          (is (every? #(locks/lock? (:lock %)) (map #(retrying (fn [] (c/person-on-task st :w1-ann %))) task-keys)))
          (is (= (vec (:lock (c/person st :w1-ann))) (vec (:lock (mc/person-entry st :w1-ann))))
              "the micro gate reads the gate topology's $$persons, one PState")
          (let [f (mc/forget-person! st :w1-ann)]
            (is (= :yes (:answer f)) "a forget is a fact, answered by the gate")
            (is (= {:lock nil :erased-at (:stamp f)} (select-keys (mc/person-entry st :w1-ann) [:lock :erased-at]))
                "the lock destroyed, dated by the act's stamp"))))

      ;; ---------------------------------------------------- the value forget
      (testing "a value forget purges its index entries in its own event; a read through the exit then cannot confirm the value"
        (let [secret "w1 value forget secret"
              o (act :alice :alice [{:e :w1a :k :note :v secret} {:e :w1a :k :tag :v 1}])
              a (ok! o)
              fid [(:name o) 0]
              other [(:name o) 1]
              before (rd st {:layer :alice :read [:pattern [:kv :note secret]]})]
          (is (= [fid] (fids-of before)) "before: the value index confirms it through the exit")
          (is (some #(str/includes? % secret) (keys (field st :alice :ix-kv))) "before: its text is in a value-index address")
          (is (contains? (field st :alice :ix-of) fid))
          (inject/record-purges!)
          (let [f (c/forget-value! st :alice :alice fid)
                purged @inject/purges
                fs (fields st :alice)
                es (entries-of fs fid)]
            (is (= [:yes :row-deleted] ((juxt :answer :how) f)))
            (is (= [{:layer :alice :fid fid :forget-stamp (:stamp f)}] (map #(dissoc % :task) purged))
                "the purge seam was called once, from the forget, with exactly the erased value and the forget's stamp")
            (is (not-any? #(str/includes? % secret) (keys (:ix-kv fs))) "right after the forget's answer: no value-index address holds the text")
            (is (not (contains? (:ix-of fs) fid)) "nor any :ix-of entry for it")
            (is (= 2 (count es)) "its two id-index entries stay, as tombstones")
            (is (every? #(and (= (:stamp f) (:erased-at %)) (not-any? (fn [k] (some? (get % k))) [:v :sealed :lock :digest])) es)
                "each dated by the forget, holding no value, sealed bytes, lock or digest")
            (is (not (text-anywhere? fs secret)) "a scan of the four fields finds the text nowhere")
            (is (seq (entries-of fs other)) "the act's other fact keeps its entries")
            (let [after (rd st {:layer :alice :read [:pattern [:kv :note secret]]})
                  e (rd st {:layer :alice :read [:pattern [:e :w1a]]})
                  p (rd st {:layer :alice :read [:point [fid]]})]
              (is (= [] (:rows after)) "the exit cannot confirm the forgotten value by its text")
              (is (not= (:fingerprint before) (:fingerprint after)) "and the earlier fingerprint cannot be reached again (RD6)")
              (is (= [[fid (:stamp f) false] [other nil true]]
                     (mapv (juxt :fid :erased-at #(contains? % :value)) (remove #(= :forget (:k %)) (:rows e))))
                  "[:e] shows the fact with its erasure date only, the other fact open (the forget fact itself beside them)")
              (is (= [(:stamp f) false] ((juxt :erased-at #(contains? % :value)) (first (:rows p))))
                  "a point read shows the date, no value")
              (is (= :erased (get-in (first (retrying #(c/facts st :alice-hand (:entry p)))) [:v :shown]))
                  "and the entry records it as erased")))))

      ;; --------------------------------------------------- the person forget
      (testing "a person forget purges every value that dies with the person, on every task, before its answer; survivors keep their entries"
        (is (= :yes (:answer (mc/make-person! st :w1p))))
        (layer! :w1p-layer :personal :w1p)
        (let [dies-own (act :w1p :w1p-layer [{:e :w1o :k :note :v "w1p own note"}])
              dies-marked (act :alice :alice [{:e :w1m :k :note :v "w1 marked about w1p" :mark #{:die-with-any}}] :subjects #{:w1p})
              lives-alice (act :alice :alice [{:e :w1u :k :note :v "w1 unmarked about w1p"}] :subjects #{:w1p})
              dies-base (act :alice :base [{:e :w1b :k :note :v "w1 base about w1p"}] :subjects #{:w1p} :permission (bp :alice))
              lives-base (act :alice :base [{:e :w1c :k :note :v "w1 base about w1p and alice"}] :subjects #{:w1p :alice} :permission (bp :alice))
              _ (doseq [o [dies-own dies-marked lives-alice dies-base lives-base]] (ok! o))
              fid (fn [o] [(:name o) 0])
              layer-of {dies-own :w1p-layer dies-marked :alice lives-alice :alice dies-base :base lives-base :base}
              text-of {dies-own "w1p own note" dies-marked "w1 marked about w1p" lives-alice "w1 unmarked about w1p"
                       dies-base "w1 base about w1p" lives-base "w1 base about w1p and alice"}
              ;; the value's canonical text as a value-index address holds it (quoted, so one text is not found inside another)
              vtext (fn [o] (env/encode-value (text-of o)))]
          (say "homes of the layers (a task each):" (into {} (for [l [:w1p-layer :alice :base]] [l (:task (mc/act st l [l :by-entity :offer (UUID. 0 0)] -1))])))
          (doseq [o (keys layer-of)]
            (is (some #(str/includes? % (vtext o)) (keys (field st (layer-of o) :ix-kv))) (str "before: " (text-of o) " is indexed by value")))
          (inject/record-purges!)
          (let [f (c/forget-person! st :w1p)
                purged @inject/purges
                purged-fids (set (map :fid purged))]
            (is (= :yes (:answer f)))
            (say "person forget purges:" (mapv #(select-keys % [:task :layer]) purged))
            (doseq [o [dies-own dies-marked dies-base]
                    :let [l (layer-of o) fs (fields st l) es (entries-of fs (fid o))]]
              (is (contains? purged-fids (fid o)) (str (text-of o) ": handed to the purge"))
              (is (every? #(= (:stamp f) (:forget-stamp %)) (filter #(= (fid o) (:fid %)) purged)) "dated by the person's forget")
              (is (not-any? #(str/includes? % (vtext o)) (keys (:ix-kv fs))) (str (text-of o) ": no value-index address left, right after the answer"))
              (is (not (contains? (:ix-of fs) (fid o))))
              (is (and (= 2 (count es)) (every? #(= (:stamp f) (:erased-at %)) es)) (str (text-of o) ": its id entries are tombstones dated by the forget"))
              (is (= [] (:rows (q-pattern st l (if (= :base l) :alice (get {:w1p-layer :w1p :alice :alice} l)) [:kv :note (text-of o)])))
                  "the value index cannot confirm it"))
            (doseq [o [lives-alice lives-base]
                    :let [l (layer-of o)]]
              (is (not (contains? purged-fids (fid o))) (str (text-of o) ": survives (7b), not purged"))
              (is (some #(str/includes? % (vtext o)) (keys (field st l :ix-kv))) "its value entry stays")
              (is (= [(fid o)] (fids-of (rd st {:layer l :read [:pattern [:kv :note (text-of o)]]}))) "and the exit still confirms it"))
            (is (= #{(fid dies-own) (fid dies-marked) (fid dies-base)} purged-fids) "exactly the values that died")
            (testing "the exit shows each dead value's date and nothing else"
              (let [r (rd st {:layer :alice :read [:point [(fid dies-marked) (fid lives-alice)]]})]
                (is (= [[(:stamp f) nil] [nil "w1 unmarked about w1p"]] (mapv (juxt :erased-at :value) (:rows r))))))
            (testing "For Sid 28: the forgotten person can no longer record a read, so their own layer is read below the exit"
              (let [r (rx/read! st {:reader :w1p :reader-kind :person :working :w1p-layer :permission [:w1p :w1p-layer :w1p-layer]
                                    :layer :w1p-layer :read [:pattern [:all]]})]
                (is (contains? #{:person-forgotten :no-such-lock} (:refused r)) (pr-str r))
                (is (= [[(fid dies-own) (:stamp f)]]
                       (keep #(when (= :note (:k %)) [(:fid %) (:erased-at %)]) (:rows (q-pattern st :w1p-layer :w1p [:all]))))))))))

      (testing "two forgets of one value, in both orders: the index keeps the date the open step gives (L16; the first date stays)"
        (is (= :yes (:answer (mc/make-person! st :w1q))))
        (is (= :yes (:answer (mc/make-person! st :w1r))))
        (let [vq (act :alice :alice [{:e :w1v1 :k :note :v "w1 value then person" :mark #{:die-with-any}}] :subjects #{:w1q})
              pq (act :alice :alice [{:e :w1v2 :k :note :v "w1 person then value" :mark #{:die-with-any}}] :subjects #{:w1q})
              qr (act :alice :alice [{:e :w1v3 :k :note :v "w1 about q and r" :mark #{:die-with-any}}] :subjects #{:w1q :w1r})
              _ (doseq [o [vq pq qr]] (ok! o))
              fid (fn [o] [(:name o) 0])
              dates (fn [o] (set (map :erased-at (entries-of (fields st :alice) (fid o)))))
              fv (c/forget-value! st :alice :alice (fid vq))
              _ (inject/record-purges!)
              fq (c/forget-person! st :w1q)
              purged-q @inject/purges
              fv2 (c/forget-value! st :alice :alice (fid pq))
              _ (inject/record-purges!)
              fr (c/forget-person! st :w1r)
              purged-r @inject/purges]
          (is (every? #(= :yes (:answer %)) [fv fq fv2 fr]))
          (is (= #{(:stamp fv)} (dates vq)) "a value forget first: its date stays; the person forget does not re-date it")
          (is (not-any? #(= (fid vq) (:fid %)) purged-q) "nor purges it again (its ledger entry says a forget purged it)")
          (is (some #(= (fid pq) (:fid %)) purged-q))
          (is (= #{(:stamp fv2)} (dates pq))
              "a value forget after the person forget: the index takes its date, as the open step does (the ledger first, L16)")
          (is (= {:erased-at (:stamp fv2)} (c/opens? st :alice (fid pq))))
          (is (= #{(:stamp fq)} (dates qr)) "a value needing both persons closed on the first forget's date, and the second keeps it")
          (is (= {:erased-at (:stamp fq)} (c/opens? st :alice (fid qr))))
          (is (every? #(= (:stamp fq) (:forget-stamp %)) (filter #(= (fid qr) (:fid %)) purged-r))
              "the second person forget purges it again, idempotently, dated by the wrap's close, not by its own stamp")
          (is (some #(= (fid qr) (:fid %)) purged-r))))

      (testing ":members is a control key only the micro gate acts on: at the stream gate it is refused, never kept as plaintext"
        (let [o (act :alice :alice [{:e :w1mem :k :members :v #{:alice}}])
              a (send! o)]
          (is (= [:no :control-not-allowed] ((juxt :answer :reason) a)))
          (is (empty? (c/raw-rows st :alice (:name o))) "nothing written")))

      ;; ------------------------------------------ the exit over sealed values
      (testing "the exit opens sealed values through phase 2's open step; time travel shows 'erased on this date' and nothing else from after the moment"
        (let [o1 (act :alice :alice [{:e :w1t :k :note :v "w1 first"}])
              s1 (:stamp (ok! o1))
              fid1 [(:name o1) 0]
              raw (c/raw-row st :alice fid1)]
          (is (and (bytes? (:sealed raw)) (nil? (:v raw)) (some? (:lock-id raw))) "stored sealed, with its lock's id, no text")
          (is (= ["w1 first"] (mapv :value (:rows (rd st {:layer :alice :read [:pattern [:e :w1t]]})))) "the exit shows the opened value")
          (let [o2 (act :alice :alice [{:e :w1t :k :note :v "w1 second" :replaces fid1}])
                s2 (:stamp (ok! o2))
                f (c/forget-value! st :alice :alice fid1)
                at1 (rd st {:layer :alice :read [:pattern [:e :w1t]] :as-of s1})
                pt1 (rd st {:layer :alice :read [:point [fid1 [(:name o2) 0]]] :as-of s1})
                now (rd st {:layer :alice :read [:pattern [:e :w1t]]})]
            (is (< s1 s2 (:stamp f)))
            (is (= [fid1] (fids-of at1)) "as of s1: nothing admitted after it")
            (is (= (:stamp f) (:erased-at (first (:rows at1)))) "erased on the forget's date, though the forget is after the moment")
            (is (not (contains? (first (:rows at1)) :value)))
            (is (not-any? #(= :forget (:k %)) (:rows at1)) "the forget fact itself is after the moment")
            (is (= [[fid1 (:stamp f)] [[(:name o2) 0] nil]] (mapv (juxt :fid :erased-at) (:rows pt1))))
            (is (:absent (second (:rows pt1))) "the replacement is absent as of s1")
            (is (= ["w1 second"] (keep :value (filter #(= :note (:k %)) (:rows now)))) "now: the replacement open, the forgotten one erased"))))

      ;; ---------------------------------- the exit's entries seal at the door
      (testing "the exit's read entries lease and seal at the door like any offer"
        (let [nm (env/make-name :alice-hand :by-layer)
              r (rd st {:layer :alice :read [:pattern [:e :w1t]] :entry-name nm})
              rows (c/raw-rows st :alice-hand nm)
              recs (filter #(= nm (:name %)) (c/depot-records st))
              lid (:lock-id (first rows))
              lease-name (first lid)]
          (is (contains? r :rows))
          (is (seq rows))
          (is (every? #(and (bytes? (:sealed %)) (nil? (:v %)) (some? (:lock-id %))) rows) "the entry's line is sealed in the log")
          (is (seq recs) "its depot records")
          (is (every? (fn [rec] (every? #(and (contains? % :sealed) (not (contains? % :v))) (:facts rec))) recs)
              "every depot copy of the entry is sealed: no plaintext line in the depot (default 1)")
          (is (= :alice-hand (nth lease-name 0)) "its lock was leased in the working layer")
          (is (= :yes (:answer (c/lookup st lease-name nil))) "by a lease act answered by name, like any offer's")
          (is (not-any? #(= lid (first %)) (c/lease-rows st :alice-hand (c/default-session :alice))) "the lock consumed at the entry's decision")
          (is (< (:stamp (:moment r)) (:entry-stamp r)) "stamped after its moment, read from its sealed value")
          (testing "an entry is a value like any other: it can be forgotten, and then reads as erased"
            (let [f (c/forget-value! st :alice :alice-hand [nm 0])]
              (is (= :yes (:answer f)))
              (is (= {:erased-at (:stamp f)} (c/opens? st :alice-hand [nm 0])))))))

      (testing "an agent's reads are recorded in its person's session layer: its lease is sealed under the layer's owner (wave 1, For Sid 32)"
        (ok! (c/grant-offer st [:w1-agent :alice-agent :alice-agent]))
        (let [r (rx/read! st {:reader :w1-agent :for :alice :reader-kind :model :working :alice-agent
                              :permission [:w1-agent :alice-agent :alice-agent] :layer :alice :read [:pattern [:e :w1t]]})
              lid (:lock-id (first (c/raw-rows st :alice-agent (:entry r))))]
          (is (contains? r :rows) (pr-str r))
          (is (= :w1-agent (:who (c/record st (:entry r)))))
          (is (every? #(= :alice (:under (second %))) (c/lease-rows st :alice-agent (c/default-session :w1-agent)))
              "the agent's unconsumed lease rows are sealed under Alice's lock, so her forget reaches them")
          (is (some? lid))))

      ;; ------------------------------------------------------ the micro side
      (let [g (mc/make-group! st :group (:group mc/shared-world))]
        (is (every? #(= :yes (:answer %)) (tg/write-micro! st [:group])))
        (testing "a group's making act passes with :members, a control fact the sealed parse takes as plaintext"
          (is (= :yes (get-in g [:made :answer])))
          (is (= #{:alice :bob} (set (keys (mc/members-of st :group)))))
          (is (contains? (micro/parse-micro (:offer g)) :ok))
          (is (contains? env/control-keys :members))
          (let [bad (mc/build {:who :alice :layer :group :session :w1x :permission (sp :w1x :alice)
                               :facts [{:e :w1x :k :note :v "plain"}]})]
            (is (= :not-sealed (:refuse (micro/parse-micro bad))) "a value fact still must be sealed"))))
      (mc/open-session! st :w1s :bob [:group])
      (let [write! (fn [spec & opts] (apply mc/write! st spec opts))
            bob (fn [facts & {:as more}] (merge {:who :bob :layer :group :session :w1s :permission (sp :w1s :bob) :facts facts} more))]
        (testing "a value forget in the group reaches its lock in the record: excised, the ledger dated (ruling 7)"
          (let [r (write! (bob [{:e :w1g0 :k :note :v {:token "group value"}} {:e :w1g0 :k :tag :v 2}]))
                nm (get-in r [:offer :name])
                fid [nm 0]
                row (mc/row-of st :w1g0 fid)
                lid (:lock-id row)]
            (is (= :yes (get-in r [:answer :answer])))
            (is (some? (:lock row)) "ruling 7: a group value's wrapped lock is in the record")
            (is (nil? (mc/lock-row st :w1g0 lid)) "and has no lock row")
            (is (= [{:token "group value"} 2] (map :value (mc/open-act st :w1g0 nm))))
            (let [f (mc/forget-value! st :group fid :w1g0)]
              (is (= :yes (:answer f)))
              (is (nil? (:lock (mc/row-of st :w1g0 fid))) "the record's lock excised")
              (is (some? (:sealed (mc/row-of st :w1g0 fid))) "the sealed bytes stay, opened by nothing")
              (is (= {:stamp (:stamp f) :how :excised} (select-keys (mc/ledger-entry st :w1g0 lid) [:stamp :how])))
              (is (= [{:erased-at (:stamp f)} {:value 2}] (map #(select-keys % [:erased-at :value]) (mc/open-act st :w1g0 nm)))
                  "the value reads erased on the forget's date; the act's other value stays open")
              (let [f2 (mc/forget-value! st :group fid :w1g0)]
                (is (= :yes (:answer f2)) "a second forget: yes")
                (is (= (:stamp f) (:stamp (mc/ledger-entry st :w1g0 lid))) "nothing changes: the first date stays")))))
        (testing "an :own-row value in the group: its lock row deleted, the ledger :row-deleted"
          (let [r (write! (bob [{:e :w1g1 :k :note :v {:token "own row"} :mark #{:own-row}}]))
                nm (get-in r [:offer :name])
                fid [nm 0]
                lid (:lock-id (mc/row-of st :w1g1 fid))]
            (is (= :yes (get-in r [:answer :answer])))
            (is (some? (mc/lock-row st :w1g1 lid)) "a lock row on the value's entity task")
            (let [f (mc/forget-value! st :group fid :w1g1)]
              (is (= :yes (:answer f)))
              (is (nil? (mc/lock-row st :w1g1 lid)) "the lock row deleted")
              (is (= {:stamp (:stamp f) :how :row-deleted} (select-keys (mc/ledger-entry st :w1g1 lid) [:stamp :how])))
              (is (= [{:erased-at (:stamp f)}] (map #(select-keys % [:erased-at :value]) (mc/open-act st :w1g1 nm)))))))
        (testing "a forget of no value of the group is refused :no-such-value; a stream-era target too (M25)"
          (let [r (write! (bob [{:e :w1g2 :k :note :v {:token "x"}}]))
                nm (get-in r [:offer :name])]
            (is (= [:no :no-such-value] ((juxt :answer :reason) (mc/forget-value! st :group [nm 7] :w1g2))))
            (is (= [:no :no-such-value] ((juxt :answer :reason) (mc/forget-value! st :group [nm 0] :w1g-elsewhere)))
                "the forget names its target's entity; elsewhere there is no such value")))
        (testing "per-act grain: one lock for the act across entities, forgotten as a whole (ruling 7)"
          (let [ents (vec (take 64 (for [i (range)] (keyword (str "w1pe" i)))))
                task-of (memoize (fn [e] (mc/task-of st e)))
                [ea eb] (or (first (for [a ents b ents :when (not= (task-of a) (task-of b))] [a b])) [:w1pe0 :w1pe1])
                sw (mc/offer! st (mc/build {:who :operator :layer :group :facts [{:e :group :k :lock-grain :v :per-act}]}))
                r (write! (bob [{:e ea :k :note :v {:token "per-act a"}} {:e eb :k :note :v {:token "per-act b"}}]))
                nm (get-in r [:offer :name])
                lid-a (:lock-id (mc/row-of st ea [nm 0]))
                lid-b (:lock-id (mc/row-of st eb [nm 1]))]
            (say "per-act entities" ea eb "on tasks" (task-of ea) (task-of eb))
            (is (= :yes (:answer sw)))
            (is (= :yes (get-in r [:answer :answer])))
            (is (= lid-a lid-b) "one lock for the act's values")
            (let [spread (retrying #(into {} (foreign-select [(keypath nm :entities) ALL] (:names st))))]
              (is (= #{ea eb} (set (keys spread))) "the act's name row keeps the entities its lock spans"))
            (let [f (mc/forget-value! st :group [nm 0] ea)]
              (is (= :yes (:answer f)))
              (doseq [[e i] [[ea 0] [eb 1]]]
                (is (nil? (:lock (mc/row-of st e [nm i]))) (str e ": excised"))
                (is (= {:stamp (:stamp f) :how :excised} (select-keys (mc/ledger-entry st e lid-a) [:stamp :how])) (str e ": dated")))
              (is (= [{:erased-at (:stamp f)}] (map #(select-keys % [:erased-at :value]) (mc/open-act st eb nm)))
                  "the value on the other entity reads erased too: forgettable only as a whole"))
            (is (= :yes (:answer (mc/offer! st (mc/build {:who :operator :layer :group :facts [{:e :group :k :lock-grain :v :per-value}]}))))
                "back to per value"))))

      ;; ------------------------------------------------------------ crashes
      (testing "the purge is the forget's own event: a crash after the forget's writes discards both, the replay writes both (R3, R4)"
        (let [o (act :alice :alice [{:e :w1z :k :note :v "w1 crashed forget"}])
              _ (ok! o)
              fid [(:name o) 0]
              lid (:lock-id (c/raw-row st :alice fid))
              fo (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                           :stood-on {fid (:stamp (c/record st (:name o)))} :facts [{:e :w1z :k :forget :v {:target fid}}]})]
          (inject/record-purges!)
          (inject/watch! (:name fo))
          (inject/arm! :after-writes (:name fo))
          (try (c/offer! st fo) (catch Exception _ nil))
          (let [rec (wait-until #(let [r (c/lookup st (:name fo) nil)] (when (map? r) r)) 120000)
                fs (fields st :alice)
                calls (filter #(= fid (:fid %)) @inject/purges)]
            (is (= :yes (:answer rec)))
            (is (<= 1 (inject/fired-count :after-writes (:name fo))) "the crash fired")
            (is (<= 2 (count calls)) "the purge ran in the failed attempt and again in the replay (at least once each)")
            (is (= {:stamp (:stamp rec) :how :row-deleted} (get (c/ledger st :alice) lid)) "the ledger holds the committed stamp")
            (is (every? #(= (:stamp rec) (:erased-at %)) (entries-of fs fid))
                "the tombstones carry the committed forget's stamp: the failed attempt's writes went with it")
            (is (not-any? #(str/includes? % "w1 crashed forget") (keys (:ix-kv fs))))))))))
