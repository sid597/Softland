(ns formal.run
  "Random histories of offers, retries, promotions, failovers and forgets,
  checked against the eight properties under each reading of the rulings.

  clojure -M:run [histories-per-check] [config-name] [seed]"
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [formal.model :as m]
            [formal.properties :as props]
            [formal.scenarios :as scenarios]))

;; ------------------------------------------------------------- generators

(defn- gen-fact [allowed-layers]
  (gen/hash-map
   :e (gen/frequency [[4 (gen/return :e0)] [2 (gen/return :e1)] [1 (gen/return :e2)] [1 (gen/return :e3)]])
   :k (gen/elements [:note :mention])
   :mention (gen/elements [#{:alice} #{:bob} #{:alice :bob}])
   :replaces (gen/frequency [[4 (gen/return :none)] [5 (gen/return :head)] [1 (gen/return :stale)]])
   :mark (gen/frequency [[8 (gen/return #{})]
                         [1 (gen/return #{:own-row})]
                         [1 (gen/return #{:die-with-any})]])
   :other-layer (gen/frequency [[9 (gen/return nil)] [1 (gen/elements allowed-layers)]])))

(def gen-times
  "How many times the offerer sends it: once, or again under the same name."
  (gen/frequency [[4 (gen/return 1)] [1 (gen/return 2)]]))

(def gen-cite
  "Which of her permissions Alice names: the one in the layer she writes
  into, or the one in her session."
  (gen/frequency [[9 (gen/return :own)] [1 (gen/return :session)]]))

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
            times gen-times
            cite gen-cite]
    [:offer {:who who :layer layer :facts facts :stood-on stood :times times :cite cite}]))

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
    [8 (gen/fmap (fn [[i t n c]] [:promote i t n c])
                 (gen/tuple gen/nat (gen/elements [:group :base]) gen-times gen-cite))]
    [5 (gen/fmap (fn [i] [:retry i]) gen/nat)]
    [2 (gen/fmap (fn [i] [:revoke i]) gen/nat)]
    [4 (gen/fmap (fn [[i [_ spec]]] [:reuse i spec]) (gen/tuple gen/nat gen-offer))]
    [7 (gen/fmap (fn [i] [:forget-value i]) gen/nat)]
    [3 (gen/fmap (fn [p] [:forget-person p]) (gen/elements m/people))]
    [5 (gen/fmap (fn [[s k]] [:failover s k])
                 (gen/tuple (gen/elements [:stream :micro]) (gen/choose -3 3)))]
    [4 (gen/fmap (fn [s] [:zombie s]) (gen/elements [:stream :micro]))]
    [2 (gen/fmap (fn [l] [:set-grain l]) (gen/elements [:alice :alice-hand]))]
    [2 (gen/fmap (fn [l] [:reclass l]) (gen/elements [:alice :alice-hand :alice-agent]))]]))

(def gen-promotion-op
  "A narrower alphabet around promotions, so their paths are reached often:
  Alice's own layer, her promotions, her home partition, the micro batch,
  reads, forgets, retries and failovers."
  (gen/frequency
   [[6 (gen/fmap (fn [[f t c]] [:offer {:who :alice :layer :alice :facts [f] :stood-on nil :times t :cite c}])
                 (gen/tuple (gen/fmap #(assoc % :other-layer nil) (gen-fact [:alice])) gen-times gen-cite))]
    [6 (gen/fmap (fn [[i t n c]] [:promote i t n c])
                 (gen/tuple gen/nat (gen/elements [:group :base]) gen-times gen-cite))]
    [1 (gen/fmap (fn [i] [:revoke i]) gen/nat)]
    [8 (gen/return [:step 0])]
    [2 (gen/return [:work 0])]
    [4 (gen/return [:batch])]
    [2 (gen/return [:prepare])]
    [2 (gen/return [:commit])]
    [8 (gen/fmap (fn [a] [:read a])
                 (gen/one-of [(gen/return :now) (gen/fmap (fn [i] [:at i]) gen/nat)]))]
    [4 (gen/fmap (fn [i] [:forget-value i]) gen/nat)]
    [2 (gen/fmap (fn [p] [:forget-person p]) (gen/elements m/people))]
    [2 (gen/fmap (fn [i] [:retry i]) gen/nat)]
    [2 (gen/fmap (fn [[s k]] [:failover s k])
                 (gen/tuple (gen/elements [:stream :micro]) (gen/choose -3 3)))]
    [1 (gen/fmap (fn [s] [:zombie s]) (gen/elements [:stream :micro]))]]))

(def gen-permission-op
  "A narrower alphabet around permissions, so a revocation falls between a
  micro prepare and its commit often: writes into shared layers under either
  of Alice's permissions, the two halves of the micro batch, revocations of
  the permissions those writes name, and the stream partition Alice's
  session lives on."
  (gen/frequency
   [[6 (gen/fmap (fn [[l f c]] [:offer {:who :alice :layer l :facts [f] :stood-on nil :times 1 :cite c}])
                 (gen/tuple (gen/elements [:group :base])
                            (gen/fmap #(assoc % :other-layer nil) (gen-fact [:group]))
                            (gen/elements [:own :session :session])))]
    [2 (gen/fmap (fn [f] [:offer {:who :bob :layer :group :facts [f] :stood-on nil :times 1 :cite :own}])
                 (gen/fmap #(assoc % :other-layer nil) (gen-fact [:group])))]
    [3 (gen/return [:prepare])]
    [3 (gen/return [:commit])]
    [4 (gen/fmap (fn [pid] [:revoke pid])
                 (gen/elements [[:alice :group :session] [:alice :group :session]
                                [:alice :base :session] [:alice :base :session]
                                [:alice :group :own] [:alice :base :own] [:bob :group :own]]))]
    [8 (gen/return [:step 1])]
    [2 (gen/return [:work 1])]
    [2 (gen/return [:step 2])]
    [3 (gen/fmap (fn [a] [:read a])
                 (gen/one-of [(gen/return :now) (gen/fmap (fn [i] [:at i]) gen/nat)]))]
    [1 (gen/fmap (fn [k] [:failover :micro k]) (gen/choose -3 3))]
    [1 (gen/return [:zombie :micro])]
    [1 (gen/fmap (fn [i] [:retry i]) gen/nat)]]))

(def gen-history
  "Three histories in five from every op; one in five from the promotion
  alphabet, one in five from the permission alphabet."
  (gen/frequency [[3 (gen/vector gen-op)]
                  [1 (gen/vector gen-promotion-op)]
                  [1 (gen/vector gen-permission-op)]]))

;; ----------------------------------------------------------------- configs

(def configs
  "The literal reading; the baseline, every reading turned; each reading put
  back alone; the carried landing name beside the derived one; and the
  demonstrations: copies exempt only if landed, the landing checking its
  source, the epoch taken away."
  (concat
   [[:ruled m/ruled]
    [:baseline m/baseline]]
   (for [[a change] m/readings]
     [(keyword (str "baseline-but-not-" (name a)))
      (merge m/baseline (select-keys m/ruled (keys change)))])
   [[:baseline-with-the-landing-name-carried (assoc m/baseline :forward-name :carried)]
    [:baseline-with-a-read-as-owner-and-an-other (assoc m/baseline :wrap :owner-and-an-other)]
    [:baseline-with-the-third-reading-of-a (assoc m/baseline :wrap :owner-and-marked)]
    [:baseline-but-copies-exempt-only-if-landed-before
     (assoc m/baseline :p6-copies :landed-before)]
    [:same-with-the-landing-checking-its-source
     (assoc m/baseline :p6-copies :landed-before :landing-checks-source true)]
    [:baseline-without-fencing (assoc m/baseline :fencing false)]]))

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
   ["a name was reused for other content" (traced #"reuses the name")]
   ["a reuse was refused by its digest" (traced #"refused by (its )?digest")]
   ["a reuse was refused on its face, made for another layer or class" (traced #"refused on its face")]
   ["two offers under one name both landed" #(props/x3-one-id-one-fact %)]
   ["a stream failover replayed an unfinished offer" (traced #"will replay")]
   ["a micro failover lost a prepared batch" (traced #"deposed micro leader")]
   ["a deposed leader's write was refused by its epoch" (traced #"is refused")]
   ["a promotion landed" #(some :source (m/facts-in % :micro))]
   ["a read showed a promotion pending, not yet read out" (traced #"promotions:.*\bpending")]
   ["a read showed a promotion crossed, not yet landed" (traced #"promotions:.*\bcrossed")]
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
   ["an offer was refused: fact outside the act's layer" (traced #"fact-outside")]
   ["a permission was revoked" #(some (fn [f] (= :revoke (:k f))) (m/all-facts %))]
   ["an offer was refused: its permission revoked" (traced #"permission-revoked")]
   ["an offer was refused: its permission from another layer" (traced #"permission-from-another-layer")]
   ["an offer was admitted after its permission's revocation" #(props/rv-revocation %)]])

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

(defn- differs-from-baseline [cfg]
  (into (sorted-map) (remove (fn [[k v]] (= v (m/baseline k))) cfg)))

(defn -main [& args]
  (let [n (or (some-> (first args) parse-long) 1000)
        only (second args)
        seed (or (some-> (nth args 2 nil) parse-long) seed)]
    (println "histories per check:" n " seed:" seed)
    (when (or (nil? only) (= only "scenarios"))
      (println)
      (println "== fixed histories")
      (scenarios/report scenarios/a-cases
                        [[:baseline m/baseline]
                         [:baseline-but-not-owner-required (assoc m/baseline :wrap :any-subject)]
                         [:baseline-with-a-read-as-owner-and-an-other (assoc m/baseline :wrap :owner-and-an-other)]
                         [:baseline-with-the-third-reading-of-a (assoc m/baseline :wrap :owner-and-marked)]]
                        [:p6 :x1])
      (scenarios/report scenarios/b-cases
                        [[:baseline m/baseline]
                         [:baseline-but-not-p6-line-at-the-read-out (assoc m/baseline :p6-copies :none)]]
                        [:p1 :p2 :p5 :p6 :p7 :p8 :x2])
      (scenarios/report scenarios/d-cases
                        [[:baseline m/baseline]
                         [:baseline-but-not-permissions-in-their-layer (assoc m/baseline :permission-home :anywhere)]]
                        [:p1 :p2 :rv]))
    (when (or (nil? only) (= only "baseline"))
      (println)
      (println "== coverage: of" n "histories under baseline, how many reached each path")
      (coverage m/baseline n seed))
    (doseq [[cname cfg] configs
            :when (or (nil? only) (= only (name cname)))]
      (println)
      (println "==" (name cname) "   differs from baseline:" (pr-str (differs-from-baseline cfg)))
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
