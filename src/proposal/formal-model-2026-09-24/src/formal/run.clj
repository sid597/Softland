(ns formal.run
  "Random histories of offers, retries, promotions, failovers and forgets,
  checked against the eight properties under each reading of the rulings.

  clojure -M:run [histories-per-check] [config-name] [seed]"
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [formal.model :as m]
            [formal.properties :as props]))

;; ------------------------------------------------------------- generators

(defn- gen-fact [allowed-layers]
  (gen/hash-map
   :e (gen/frequency [[4 (gen/return :e0)] [2 (gen/return :e1)] [1 (gen/return :e2)] [1 (gen/return :e3)]])
   :k (gen/elements [:note :mention])
   :mention (gen/elements m/people)
   :replaces (gen/frequency [[4 (gen/return :none)] [5 (gen/return :head)] [1 (gen/return :stale)]])
   :mark (gen/frequency [[8 (gen/return #{})]
                         [1 (gen/return #{:own-row})]
                         [1 (gen/return #{:die-with-any})]])
   :other-layer (gen/frequency [[9 (gen/return nil)] [1 (gen/elements allowed-layers)]])))

(def gen-times
  "How many times the offerer sends it: once, or again under the same name."
  (gen/frequency [[4 (gen/return 1)] [1 (gen/return 2)]]))

(def gen-offer
  ;; alice's own layer is the one promotions start from, so it gets more weight
  (gen/let [who (gen/elements [:alice :bob])
            [layer facts stood] (let [allowed (if (= :alice who) (vec (keys m/layers)) [:group :base])]
                                  (gen/tuple (if (= :alice who)
                                               (gen/frequency [[3 (gen/return :alice)]
                                                               [1 (gen/elements (rest allowed))]])
                                               (gen/elements allowed))
                                             (gen/vector (gen-fact allowed) 1 3)
                                             (gen/frequency [[3 (gen/return nil)] [1 gen/nat]])))
            times gen-times]
    [:offer {:who who :layer layer :facts facts :stood-on stood :times times}]))

(def gen-op
  (gen/frequency
   [[20 gen-offer]
    [20 (gen/fmap (fn [p] [:step p]) (gen/choose 0 (dec m/n-parts)))]
    [5 (gen/return [:prepare])]
    [5 (gen/return [:commit])]
    [6 (gen/return [:batch])]
    [6 (gen/fmap (fn [p] [:work p]) (gen/choose 0 (dec m/n-parts)))]
    [8 (gen/fmap (fn [a] [:read a])
                 (gen/one-of [(gen/return :now) (gen/fmap (fn [i] [:at i]) gen/nat)]))]
    [8 (gen/fmap (fn [[i t n]] [:promote i t n])
                 (gen/tuple gen/nat (gen/elements [:group :base]) gen-times))]
    [5 (gen/fmap (fn [i] [:retry i]) gen/nat)]
    [7 (gen/fmap (fn [i] [:forget-value i]) gen/nat)]
    [3 (gen/fmap (fn [p] [:forget-person p]) (gen/elements m/people))]
    [5 (gen/fmap (fn [[s k]] [:failover s k])
                 (gen/tuple (gen/elements [:stream :micro]) (gen/choose -3 3)))]
    [4 (gen/fmap (fn [s] [:zombie s]) (gen/elements [:stream :micro]))]
    [2 (gen/fmap (fn [l] [:set-grain l]) (gen/elements [:alice :alice-hand]))]
    [2 (gen/fmap (fn [l] [:reclass l]) (gen/elements [:alice :alice-hand :alice-agent]))]]))

(def gen-history (gen/vector gen-op))

;; ----------------------------------------------------------------- configs

(def configs
  "The literal reading; every reading turned; each turned reading put back
  one at a time; and the epoch taken away."
  (concat
   [[:ruled m/ruled]
    [:amended m/amended]]
   (for [[a change] m/amendments]
     [(keyword (str "amended-but-not-" (name a)))
      (merge m/amended (select-keys m/ruled (keys change)))])
   [[:amended-but-copies-exempt-only-if-landed-before
     (assoc m/amended :p6-copies :landed-before)]
    [:same-with-the-landing-checking-its-source
     (assoc m/amended :p6-copies :landed-before :landing-checks-source true)]
    [:amended-without-fencing (assoc m/amended :fencing false)]]))

;; ----------------------------------------------------------------- checks

(def seed 20260924)
(def max-size "Histories run up to this many ops." 60)

(defn check
  "Run n random histories under a config against one property. On failure,
  the shrunk history and what happened in it."
  [config pk n seed]
  (let [f (second (props/properties pk))
        res (tc/quick-check n (prop/for-all [h gen-history] (nil? (f (m/run config h))))
                            :seed seed :max-size max-size)]
    (if (:pass? res)
      {:pass true :tests (:num-tests res)}
      (let [h (first (get-in res [:shrunk :smallest]))
            r (get-in res [:shrunk :result])]
        (if (instance? Throwable r)
          {:error r :history h}
          (let [st (m/run config h)]
            {:pass false :tests (:num-tests res) :history h
             :violation (f st) :trace (:trace st)}))))))

;; --------------------------------------------------------------- coverage

(defn- traced [re] (fn [st] (some #(re-find re %) (:trace st))))

(def paths
  "Paths a history can take, to count how often the random histories reach them."
  [["a retry under the same name" (traced #"sends it again under the same name")]
   ["the stream gate met a name it had already decided" (traced #"already decided here")]
   ["the micro gate met a name it had already decided" (traced #"already decided, skipped")]
   ["a stream failover replayed an unfinished offer" (traced #"will replay")]
   ["a micro failover lost a prepared batch" (traced #"deposed micro leader")]
   ["a deposed leader's write was refused by its epoch" (traced #"is refused")]
   ["a promotion landed" #(some :because-of (m/facts-in % :micro))]
   ["a promotion was refused because its source was erased" (traced #"source-erased")]
   ["a promotion landed and its source was later forgotten"
    (fn [st] (some #(and (:source %) (m/erasure st (m/fact-by-id st (:source %))))
                   (m/facts-in st :micro)))]
   ["a value forget deleted a lock row" (traced #"lock row will be deleted")]
   ["a value forget was an operator excision" (traced #"operator excises")]
   ["a person forget erased a value that existed before it"
    (fn [st] (some #(let [e (m/erasure st %)] (and e (not (:how e)) (< (:tick %) (:tick e))))
                   (m/all-facts st)))]
   ["a read as of an earlier moment showed an erased value" (traced #"a read as of stamp .* erased on")]
   ["a layer was re-classed" #(some (fn [[_ v]] (and (= :by-entity (:class v)))) (select-keys (:settings %) [:alice :alice-hand :alice-agent]))]
   ["one act landed on two partitions of the micro store"
    (fn [st] (some (fn [[_ fs]] (< 1 (count (distinct (map :part fs)))))
                   (group-by :act (m/facts-in st :micro))))]
   ["a per-act lock was used" #(some (fn [f] (= :per-act (get-in f [:lock :grain]))) (m/all-facts %))]
   ["an offer was refused: stale replaces" (traced #"stale-replaces")]
   ["an offer was refused: class mismatch" (traced #"class-mismatch")]
   ["an offer was refused: fact outside the act's layer" (traced #"fact-outside")]])

(defn coverage
  "Of n histories drawn from the same generator, how many reach each path."
  [config n seed]
  (let [sts (for [i (range n)] (m/run config (gen/generate gen-history (mod i (inc max-size)) (+ seed i))))
        counts (reduce (fn [acc st]
                         (reduce (fn [acc [label f]] (if (f st) (update acc label (fnil inc 0)) acc))
                                 acc paths))
                       {} sts)]
    (doseq [[label _] paths]
      (println (format "   %6d  %s" (get counts label 0) label)))))

(defn- differs-from-ruled [cfg]
  (into (sorted-map) (remove (fn [[k v]] (= v (m/ruled k))) cfg)))

(defn -main [& args]
  (let [n (or (some-> (first args) parse-long) 1000)
        only (second args)
        seed (or (some-> (nth args 2 nil) parse-long) seed)]
    (println "histories per check:" n " seed:" seed)
    (when (or (nil? only) (= only "amended"))
      (println)
      (println "== coverage: of" n "histories under amended, how many reached each path")
      (coverage m/amended n seed))
    (doseq [[cname cfg] configs
            :when (or (nil? only) (= only (name cname)))]
      (println)
      (println "==" (name cname) "   differs from ruled:" (pr-str (differs-from-ruled cfg)))
      (doseq [pk (keys props/properties)]
        (let [r (check cfg pk n seed)
              label (str (name pk) " " (first (props/properties pk)))]
          (cond
            (:error r)
            (do (println "  " label ": MODEL ERROR" (.getMessage ^Throwable (:error r)))
                (println "     history:" (pr-str (:history r))))

            (:pass r)
            (println "  " label ": holds," (:tests r) "histories")

            :else
            (do (println "  " label ": FAILS at history" (:tests r))
                (println "     smallest history (" (count (:history r)) "ops):" (pr-str (:history r)))
                (doseq [l (:trace r)] (println "       " l))
                (println "     violation:" (pr-str (:violation r))))))))
    (shutdown-agents)))
