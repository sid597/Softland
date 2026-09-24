(ns formal.properties
  "The eight properties, each a function of a finished run: nil when it
  holds, a map describing the first violation when it does not."
  (:require [formal.model :as m]))

(defn- offers-made
  "Every offer made: the first under each name, and each later one sent
  under a used name with other content (change E)."
  [st]
  (concat (vals (:sent st)) (:reuses st)))

(defn p1-one-answer
  "Every offer gets exactly one yes or no, findable by its name, even across
  retries and failover. Findable strictly (change C): the lookup by the
  offer's name and layer, which reads one or two partitions, returns it.
  Each offer is told by its digest (change E), so an offer reusing a name
  is judged by what happened to its own content."
  [st]
  (let [landed (set (map (juxt :act :digest) (m/all-facts st)))]
    (first (for [o (offers-made st)
                 :let [nm (:name o)
                       own (filter #(= (:digest o) (:digest %)) (m/answers-for st nm))
                       said (set (map :answer own))
                       happened (if (landed [nm (:digest o)]) :yes :no)
                       found (m/lookup st nm (:layer o) (:digest o))]
                 :when (or (< 1 (count said))
                           ;; what was recorded for it must say what happened
                           (and (seq said) (not= said #{happened}))
                           ;; and the lookup must tell it
                           (not= happened (:answer found)))]
             {:offer nm :layer (:layer o) :reuse (boolean (some #{o} (:reuses st)))
              :happened happened
              :found-by-lookup (some-> found (select-keys [:answer :reason :where]))
              :recorded-for-it (mapv #(select-keys % [:answer :reason :stamp :where]) own)}))))

(defn p2-whole-acts
  "An act lands whole or not at all, at one moment; a reader never sees part
  of one."
  [st]
  (or (first (for [[[act d] fs] (group-by (juxt :act :digest) (m/all-facts st))
                   :let [offered (count (:facts (get-in st [:offers d])))
                         landed (count (distinct (map :id fs)))
                         stamps (vec (distinct (map :stamp fs)))]
                   :when (or (not= offered landed) (< 1 (count stamps)))]
               {:act act :offered offered :landed landed :stamps stamps}))
      (first (for [r (:reads st)
                   [[act d] fs] (group-by (juxt :act :digest) (:facts r))
                   :let [offered (count (:facts (get-in st [:offers d])))
                         seen (count (distinct (map :id fs)))]
                   :when (not= offered seen)]
               {:read-at-tick (:tick r) :as-of (:as-of r) :act act
                :offered offered :seen seen}))))

(defn p3-no-forks
  "No replacement chain forks: two admitted facts never replace the same fact."
  [st]
  (first (for [[[layer r] fs] (group-by (juxt :layer :replaces) (filter :replaces (m/all-facts st)))
               :let [ids (vec (distinct (map :id fs)))]
               :when (< 1 (count ids))]
           {:layer layer :replaced r :replaced-by ids})))

(defn p4-no-read-past-its-moment
  "No read points past its own as-of moment."
  [st]
  (let [exempt? (get-in st [:config :p4-erasure-exempt])]
    (first (for [r (:reads st)
                 x (concat (for [f (:facts r)] {:shows [:fact (:id f)] :dated (:stamp f)})
                           (when-not exempt?
                             (for [f (:facts r) :when (:erased-at f)]
                               {:shows [:erased (:id f)] :dated (:erased-at f)}))
                           (for [pr (:promotions r) s (:landing-stamps pr)]
                             {:shows [:landing-of (:request pr)] :dated s}))
                 :when (> (:dated x) (:as-of r))]
             (assoc x :read-at-tick (:tick r) :as-of (:as-of r))))))

(defn p5-clocks
  "A partition's stamps never go backward, and a fact is never stamped at or
  before anything it stood on."
  [st]
  (or (first (for [s [:stream :micro]
                   [i part] (map-indexed vector (get-in st [s :parts]))
                   [a b] (partition 2 1 (:stamps part))
                   :when (< b a)]
               {:unit [s i] :stamp a :then b}))
      (first (for [f (m/all-facts st)
                   id (m/stood-on f)
                   :let [g (m/fact-by-id st id)]
                   :when (and g (<= (:stamp f) (:stamp g)))]
               {:fact (:id f) :stamp (:stamp f) :stood-on id :its-stamp (:stamp g)}))))

(defn- person-forget-due?
  "Whether the persons forgotten so far should have taken this fact's value,
  judged at the grain it was locked at (its own subjects for a per-value
  lock, the act's for a per-act lock) and by change A as confirmed, whatever
  the config reads: in a one-owner layer the owner's forget takes it, and
  another subject's only when it is marked; in a shared layer 7b as written.
  Only if the value existed when that forget happened: what a forget owes
  values written after it is not ruled, so it is not checked."
  [f persons]
  (let [lk (:lock f)
        S (if (= :per-act (:grain lk)) (:subjects f) (:own-subjects f))
        due (m/wrap-closed (m/wrap :owner-required (get-in m/layers [(:layer f) :owner]) S
                                   (contains? (:mark f) :die-with-any))
                           persons)]
    (boolean (and due (< (:order f) (:order due))))))

(defn- lock-destroyed? [st f]
  (let [lk (:lock f)]
    (get-in st [(:store lk) :parts (:part lk) :erased (:id lk)])))

(defn p6-forget
  "After a forget nothing returns the value; pointers to it answer erased;
  about lists find everything about a forgotten person."
  [st]
  (let [cfg (:config st)
        F (set (keys (:persons st)))]
    (or
     ;; (a) no read after a forget returns the forgotten value through a copy
     ;; made from it (a promotion's landing); the forgotten fact itself can
     ;; only answer erased. Direction matters: an original that lives on is
     ;; not forgotten because a copy of it was. Which copies are exempt is
     ;; the reading in :p6-copies.
     (first (for [r (:reads st)
                  h (:facts r)
                  :let [e (get-in r [:erased (:source h)])]
                  :let [c (some->> (:crossing h) (m/fact-by-id st))]
                  :when (and (contains? h :value) e
                             (not (case (:p6-copies cfg)
                                    :none false
                                    :landed-before (< (:order h) (:order e))
                                    :crossed (boolean (and c (< (:order c) (:order e)))))))]
              {:part :a :read-at-tick (:tick r) :returns (:id h) :in (:layer h)
               :value (get-in h [:value :token]) :copied-from (:source h)
               :crossed-at-order (:order c) :landed-at-order (:order h)
               :original-erased-at-order (:order e)}))
     ;; (b) a person's forget takes every value whose own subjects are all forgotten
     (first (for [f (m/all-facts st)
                  :when (and (:lock f) (not (lock-destroyed? st f))
                             (person-forget-due? f (:persons st)) (m/readable? st f))]
              {:part :b :survives (:id f) :value (get-in f [:v :token])
               :own-subjects (:own-subjects f) :act-subjects (:subjects f)
               :lock-wrap (:wrap (:lock f)) :forgotten F}))
     ;; (c) a pointer to a forgotten value still resolves, to "erased": the
     ;; target's envelope must still be there; its value opens only through
     ;; its lock, so an erased target can only answer erased
     (first (for [f (m/all-facts st)
                  id (m/stood-on f)
                  :when (nil? (m/fact-by-id st id))]
              {:part :c :dangling-pointer-from (:id f) :to id}))
     ;; (d) the about list of a forgotten person holds everything about them
     (first (for [p F
                  f (m/all-facts st)
                  :when (and ((:own-subjects f) p) (not ((:subjects f) p)))]
              {:part :d :about p :missing (:id f)})))))

(defn- promotion-shown-wrong
  "The first promotion a read showed wrongly: done before its landing, or
  crossed other than exactly when its crossing fact is at or before the
  read's moment and it has neither landed nor been refused by then. With
  `hindsight?` false the store is judged by what it held when the read was
  taken; with it true, by every fact stamped at or before the moment,
  whenever it was admitted."
  [st hindsight?]
  (let [facts (m/all-facts st)
        answers (for [s [:stream :micro] p (get-in st [s :parts]) [_ a] (:answers p)] a)]
    (first (for [r (:reads st)
                 pr (:promotions r)
                 :let [req (:request pr)
                       by-then #(and (or hindsight? (<= (:tick %) (:tick r))) (<= (:stamp %) (:as-of r)))
                       landed (some #(and (:source %) (= req (:because-of %)) (by-then %)) facts)
                       crossing (some #(when (= (m/crossing-id req) (:id %)) %) facts)
                       crossed (and crossing (by-then crossing))
                       refused (some #(and (= req (:because-of %)) (= :no (:answer %)) (by-then %)) answers)
                       should-cross (boolean (and crossed (not landed) (not refused)))]
                 :when (or (and (= :done (:status pr)) (not landed))
                           (not= (= :crossed (:status pr)) should-cross))]
             {:read-at-tick (:tick r) :as-of (:as-of r) :request req :shown (:status pr)
              :crossing (select-keys crossing [:stamp :tick]) :landed (boolean landed)
              :refused (boolean refused)}))))

(defn p7-promotion-pending
  "A promotion into a shared layer shows done only once it has landed; it
  shows crossed exactly when its crossing fact is at or before the read's
  moment and it has not landed or been refused. Judged by what the store
  held when the read was taken."
  [st]
  (promotion-shown-wrong st false))

(defn p8-never-twice
  "With two gates and a failover, nothing is admitted twice: no fact of an
  act, and no promotion."
  [st]
  (or (first (for [[[id _] fs] (group-by (juxt :id :digest) (m/all-facts st))
                   :when (< 1 (count fs))]
               {:fact id :admitted (count fs) :where (mapv (juxt :store :part :stamp) fs)}))
      (first (for [[req fs] (group-by :because-of (filter :source (m/all-facts st)))
                   :let [acts (vec (distinct (map :act fs)))]
                   :when (< 1 (count acts))]
               {:promotion req :landed-as acts}))))

(defn x1-forget-overreach
  "Not one of the eight: a person's forget takes no value it should not, by
  the same grain as P6 (b), among values that existed when it happened."
  [st]
  (let [F (set (keys (:persons st)))]
    (first (for [f (m/all-facts st)
                 :when (and (:lock f) (not (lock-destroyed? st f))
                            (not (person-forget-due? f (:persons st)))
                            (not (m/readable? st f))
                            (< (:order f) (:order (m/erasure st f))))]
             {:erased (:id f) :value (get-in f [:v :token])
              :own-subjects (:own-subjects f) :act-subjects (:subjects f)
              :lock-wrap (:wrap (:lock f)) :forgotten F}))))

(defn rv-revocation
  "Change D: no offer is admitted under a permission whose revocation is
  earlier in that store's order. A violation says whether anything ordered
  the two: the admitting store's own order, or nothing, the revocation
  having been admitted in the other store (or on another stream partition)."
  [st]
  (let [facts (m/all-facts st)
        revoked (into {} (for [f facts :when (= :revoke (:k f))] [(get-in f [:v :permission]) f]))]
    (first (for [f facts
                 :let [o (get-in st [:offers (:digest f)])
                       r (revoked (:permission o))]
                 :when (and r (not (m/exempt? o)) (< (:order r) (:order f)))]
             {:admitted (:id f) :in (:layer f) :at [(:store f) (:part f)]
              :permission (:permission o) :revoked-by (:id r) :revocation-at [(:store r) (:part r)]
              :ordered-by (if (and (= (:store r) (:store f))
                                   (or (= :micro (:store f)) (= (:part r) (:part f))))
                            :the-admitting-store
                            :nothing)}))))

(defn x2-crossed-in-hindsight
  "Not one of the eight: P7's crossed check read literally, by every fact
  stamped at or before the read's moment, including facts admitted after the
  read was taken."
  [st]
  (promotion-shown-wrong st true))

(defn x3-one-id-one-fact
  "Not one of the eight: a fact's id names one fact. Two offers under one
  name that both land give two facts one id, and a pointer to it no longer
  resolves to one thing."
  [st]
  (first (for [[id fs] (group-by :id (m/all-facts st))
               :let [ds (distinct (map :digest fs))]
               :when (< 1 (count ds))]
           {:id id :facts (mapv (juxt :layer :store :part :stamp) fs)})))

(def properties
  (array-map
   :p1 ["one answer per offer" p1-one-answer]
   :p2 ["acts land whole" p2-whole-acts]
   :p3 ["no forked chains" p3-no-forks]
   :p4 ["no read past its moment" p4-no-read-past-its-moment]
   :p5 ["clock promises" p5-clocks]
   :p6 ["forget" p6-forget]
   :p7 ["promotion pending until landed" p7-promotion-pending]
   :p8 ["nothing admitted twice" p8-never-twice]
   :rv ["no admission after its permission's revocation" rv-revocation]
   :x1 ["(extra) forget takes nothing it should not" x1-forget-overreach]
   :x2 ["(extra) crossed, judged in hindsight" x2-crossed-in-hindsight]
   :x3 ["(extra) one id, one fact" x3-one-id-one-fact]))
