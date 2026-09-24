(ns formal.properties
  "The eight properties, each a function of a finished run: nil when it
  holds, a map describing the first violation when it does not."
  (:require [formal.model :as m]))

(defn p1-one-answer
  "Every offer gets exactly one yes or no, findable by its name, even across
  retries and failover."
  [st]
  (let [landed (set (map :act (m/all-facts st)))]
    (first (for [nm (keys (:sent st))
                 :let [as (m/answers-for st nm)
                       said (set (map :answer as))]
                 :when (or (empty? as)
                           (< 1 (count said))
                           ;; the answer must say what happened
                           (not= said #{(if (landed nm) :yes :no)}))]
             {:offer nm :landed (boolean (landed nm))
              :answers (mapv #(select-keys % [:answer :reason :stamp :where]) as)}))))

(defn p2-whole-acts
  "An act lands whole or not at all, at one moment; a reader never sees part
  of one."
  [st]
  (or (first (for [[act fs] (group-by :act (m/all-facts st))
                   :let [offered (count (:facts (get-in st [:sent act])))
                         landed (count (distinct (map :id fs)))
                         stamps (vec (distinct (map :stamp fs)))]
                   :when (or (not= offered landed) (< 1 (count stamps)))]
               {:act act :offered offered :landed landed :stamps stamps}))
      (first (for [r (:reads st)
                   [act fs] (group-by :act (:facts r))
                   :let [offered (count (:facts (get-in st [:sent act])))
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
  lock, the act's for a per-act lock), and only if the value existed when
  that forget happened. What a forget owes values written after it is not
  ruled, so it is not checked."
  [f persons]
  (let [lk (:lock f)
        S (if (= :per-act (:grain lk)) (:subjects f) (:own-subjects f))
        orders (keep #(get-in persons [% :order]) S)
        due-at (cond (and (= :all (:mode lk)) (seq orders)) (apply min orders)
                     (and (seq S) (= (count orders) (count S))) (apply max orders))]
    (boolean (and due-at (< (:order f) due-at)))))

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
                  :when (and (contains? h :value) e
                             (not (case (:p6-copies cfg)
                                    :none false
                                    :landed-before (< (:order h) (:order e))
                                    :released-before (boolean (and (:released-at h)
                                                                   (< (:released-at h) (:order e)))))))]
              {:part :a :read-at-tick (:tick r) :returns (:id h) :in (:layer h)
               :value (get-in h [:value :token]) :copied-from (:source h)
               :released-at-order (:released-at h) :landed-at-order (:order h)
               :original-erased-at-order (:order e)}))
     ;; (b) a person's forget takes every value whose own subjects are all forgotten
     (first (for [f (m/all-facts st)
                  :when (and (:lock f) (not (lock-destroyed? st f))
                             (person-forget-due? f (:persons st)) (m/readable? st f))]
              {:part :b :survives (:id f) :value (get-in f [:v :token])
               :own-subjects (:own-subjects f) :act-subjects (:subjects f)
               :lock-wrapped-under (:under (:lock f)) :forgotten F}))
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

(defn p7-promotion-pending
  "A promotion into a shared layer shows pending until it lands, never done
  before it has."
  [st]
  (let [landings (filter :because-of (m/facts-in st :micro))]
    (first (for [r (:reads st)
                 pr (:promotions r)
                 :when (and (= :done (:status pr))
                            (not-any? #(and (= (:request pr) (:because-of %))
                                            (<= (:tick %) (:tick r))
                                            (<= (:stamp %) (:as-of r)))
                                      landings))]
             {:read-at-tick (:tick r) :request (:request pr) :shown :done}))))

(defn p8-never-twice
  "With two gates and a failover, nothing is admitted twice: no fact of an
  act, and no promotion."
  [st]
  (or (first (for [[id fs] (group-by :id (m/all-facts st))
                   :when (< 1 (count fs))]
               {:fact id :admitted (count fs) :where (mapv (juxt :store :part :stamp) fs)}))
      (first (for [[req fs] (group-by :because-of (filter :because-of (m/all-facts st)))
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
              :lock-wrapped-under (:under (:lock f)) :forgotten F}))))

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
   :x1 ["(extra) forget takes nothing it should not" x1-forget-overreach]))
