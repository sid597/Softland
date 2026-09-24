(ns formal.model
  "Softland's store rules as plain data and pure functions.

  A toy for one question: do the rulings in
  src/proposal/frame-2026-09-15/PROGRESS.md (24 September 2026) contradict
  each other? No Rama, no network, no disk. A state is a map; every step is
  a function from one state to the next.

  Two fact stores, each with its own gate (ruling 1): :stream serves the
  one-owner layers (personal, hand session, agent session), :micro the
  shared ones (group, base). Each store has N partitions and a leader with
  an epoch. The stream gate handles one event on one partition at a time and,
  after a failover, replays every offer it had begun and not finished (at
  least once). The micro gate decides a whole batch across partitions and
  commits it in one go.

  Vocabulary as ruled on 24 September: a fact's key is `:k`; a lock is what
  encrypts. Every value gets its own lock at write, wrapped under the person
  locks of the people it is about (ruling 7).

  Where PROGRESS.md is silent the model takes a reading. Each reading is an
  entry in the config, so a run can turn it the other way: `ruled` holds the
  literal readings, `amendments` the other side of each."
  (:require [clojure.string :as str]))

;; ------------------------------------------------------------------ world

(def people [:alice :bob])

(def n-parts "N, the task count, fixed at launch (ruling 2)." 3)

(def entities [:e0 :e1 :e2 :e3])
(def entity-part {:e0 0 :e1 1 :e2 2 :e3 0})

(def layers
  "Four layer kinds; a session is hand or agent. :home is the partition that
  holds a layer's facts while it is placed by layer, and its settings."
  (array-map
   :alice       {:kind :personal :owner :alice :one-owner true :home 0}
   :alice-hand  {:kind :hand     :owner :alice :one-owner true :home 1}
   :alice-agent {:kind :agent    :owner :alice :one-owner true :home 2}
   :group       {:kind :group    :members #{:alice :bob}      :home 1}
   :base        {:kind :base                                  :home 2}))

(def control-keys
  "Fact keys the store itself acts on. Their values are ids and settings, so
  the model gives them no lock."
  #{:forget :lock-grain :class :promote-request})

;; ----------------------------------------------------------------- config

(def ruled
  "The literal reading of PROGRESS.md; where it is silent, the plain one."
  {:act-layer             :per-fact ; 'layer' is a named part; fact or act is not said
   :reclass-moves-gate    false     ; ruling 2 re-classes placement; ruling 1 picks the gate by ownership
   :forward-name          :minted   ; a promotion's second offer is named by the store as it sends it
   :lock-subjects         :act      ; ruling 7: per-value locks inherit the act's subject list
   :landing-checks-source false     ; the micro gate does not look at the source again when it lands a copy
   :fencing               true      ; the epoch: a deposed leader's writes are refused
   ;; how two of the eight properties are read
   :p4-erasure-exempt     false     ; P4 literally: a read shows nothing dated after its moment
   :p6-copies             :none})   ; P6 literally: after a forget no copy anywhere returns the value;
                                    ; :landed-before exempts copies that had landed before the forget,
                                    ; :released-before those whose value was read out before it

(def amendments
  "Each entry turns one reading the other way."
  (array-map
   :layer-on-the-act         {:act-layer :per-act}
   :reclass-moves-the-gate   {:reclass-moves-gate true}
   :second-offer-named-first {:forward-name :carried}
   :value-locks-own-subjects {:lock-subjects :fact}
   :p4-erasure-date-exempt   {:p4-erasure-exempt true}
   :p6-released-copies-exempt {:p6-copies :released-before}))

(def amended (apply merge ruled (vals amendments)))

;; ------------------------------------------------------------------ state

(defn- partition-state []
  {:clock 0     ; the last stamp this task gave
   :stamps []   ; every stamp it gave, in order
   :log []      ; admitted facts, in order
   :answers {}  ; offer name -> {:answer :yes|:no, :reason, :stamp}
   :locks {}    ; lock-store rows kept on this task, beside their values
   :erased {}}) ; lock id -> {:stamp :tick :order :how}, once a forget destroyed it

(defn init [config]
  {:config config
   :tick 0  ; the op counter; stands in for wall time
   :order 0 ; one count per effect (admission, erasure, release), for true order within an op
   :seq 0   ; for fresh names and value tokens
   :stream {:epoch 0 :skew 0 :parts (vec (repeatedly n-parts partition-state))
            :inbox (vec (repeat n-parts [])) :unacked {} :zombie []}
   :micro {:epoch 0 :skew 0 :parts (vec (repeatedly n-parts partition-state))
           :inbox [] :prepared nil :zombie nil}
   :settings (into {} (for [[l m] layers]
                        [l {:class (if (:one-owner m) :by-layer :by-entity)
                            :grain :per-value}]))
   :persons {}     ; person -> {:stamp :tick :order} once their person lock is destroyed
   :lock-clock 0   ; the lock store's last stamp
   :sent {}        ; offer name -> offer, for every offer anyone made
   :client-sent [] ; names of offers people and the operator made, in order
   :reads []
   :trace []})

(defn- fresh [st prefix] [(str prefix (:seq st)) (update st :seq inc)])
(defn- next-order [st] (update st :order inc))

(defn- note [st & xs]
  (update st :trace conj (apply str "[t" (:tick st) "] " xs)))

(defn facts-in [st store] (for [p (get-in st [store :parts]) f (:log p)] f))
(defn all-facts [st] (concat (facts-in st :stream) (facts-in st :micro)))
(defn fact-by-id [st id] (some #(when (= id (:id %)) %) (all-facts st)))

(defn now
  "The latest stamp given anywhere: every partition's clock and the lock store's."
  [st]
  (apply max (:lock-clock st)
         (for [s [:stream :micro] p (get-in st [s :parts])] (:clock p))))

(defn answers-for [st nm]
  (for [s [:stream :micro]
        [i p] (map-indexed vector (get-in st [s :parts]))
        :let [a (get-in p [:answers nm])]
        :when a]
    (assoc a :where [s i])))

(defn stood-on
  "Ids an admitted fact stood on: its act's reads, what it replaces, and a
  promotion's source."
  [f]
  (cond-> (vec (:stood-on f))
    (:replaces f) (conj (:replaces f))
    (:source f) (conj (:source f))))

(defn fid-str [[act i]] (str act "#" i))

(defn describe-fact [f]
  (str (name (:e f)) " " (name (:k f))
       (when-let [p (get-in f [:v :person])] (str " " (name p)))
       (when-let [t (get-in f [:v :token])] (str " " t))
       " in " (name (:layer f))
       (when (:replaces f) (str ", replacing " (fid-str (:replaces f))))
       (when (seq (:mark f)) (str ", marked " (str/join "," (map name (:mark f)))))))

;; --------------------------------------------------------------- subjects

(defn fact-subjects
  "Ruling 8's three sources applied to one fact: the layer's owner, the
  key's grammar (a :mention names a person), and the tool (none here)."
  [layer f]
  (cond-> #{}
    (get-in layers [layer :owner]) (conj (get-in layers [layer :owner]))
    (and (= :mention (:k f)) (get-in f [:v :person])) (conj (get-in f [:v :person]))))

;; -------------------------------------------------------------- placement

(defn layer-of
  "The layer a fact is in: its own under :per-fact, the act's under :per-act."
  [st offer f]
  (if (= :per-act (get-in st [:config :act-layer])) (:layer offer) (:layer f)))

(defn act-subjects
  "The act's subject slot (ruling 8): the three sources over all its facts."
  [st offer]
  (reduce into #{} (for [f (:facts offer)] (fact-subjects (layer-of st offer f) f))))

(defn- gate-of
  "Ruling 1: the stream gate for one-owner layers, the micro gate for shared
  ones. Under :reclass-moves-gate a one-owner layer placed by entity moves to
  the micro gate."
  [st layer cls]
  (if (and (get-in layers [layer :one-owner])
           (or (= :by-layer cls) (not (get-in st [:config :reclass-moves-gate]))))
    :stream
    :micro))

(defn home-of
  "[store partition] for a fact, from the class the offer carries (ruling 2):
  by layer for one-owner layers, by entity otherwise. Forget and settings
  facts carry their place in :at."
  [st offer f]
  (or (:at f)
      (let [l (layer-of st offer f)
            cls (:class offer)]
        [(gate-of st l cls)
         (if (and (get-in layers [l :one-owner]) (= :by-layer cls))
           (get-in layers [l :home])
           (entity-part (:e f)))])))

(defn placed
  "Each fact of an offer as [index fact store partition]."
  [st offer]
  (for [[i f] (map-indexed vector (:facts offer))
        :let [[s p] (home-of st offer f)]]
    [i f s p]))

(defn- on [st offer store p]
  (filter (fn [[_ _ s q]] (and (= store s) (= p q))) (placed st offer)))

(defn stream-plan
  "The stream partitions an offer's facts land on, in the order it visits them."
  [st offer]
  (vec (sort (distinct (for [[_ _ s p] (placed st offer) :when (= :stream s)] p)))))

;; ----------------------------------------------------------------- chains

(defn chain-head
  "The latest fact for (layer, entity, fact key) that nothing replaces."
  [st layer e k]
  (let [fs (filter #(= layer (:layer %)) (all-facts st))
        replaced (set (keep :replaces fs))]
    (->> fs
         (filter #(and (= e (:e %)) (= k (:k %)) (not (replaced (:id %)))))
         (sort-by :stamp)
         last)))

(defn- replaceable?
  "A fact may replace r only while r heads its chain in that layer."
  [st layer f]
  (let [fs (filter #(= layer (:layer %)) (all-facts st))
        r (some #(when (= (:replaces f) (:id %)) %) fs)]
    (boolean (and r (= (:e f) (:e r)) (= (:k f) (:k r))
                  (not-any? #(= (:replaces f) (:replaces %)) fs)))))

;; ------------------------------------------------------------------ locks

(defn- lock-for
  "Ruling 7. The value's own lock, or its act's in a per-act layer; a row in
  the lock store for personal and hand layers or when marked, otherwise
  wrapped into the record; wrapped under the people it is about, any one of
  whom can open it unless it is marked to die with any of them (7b)."
  [st store p offer f fid layer]
  (let [grain (get-in st [:settings layer :grain])]
    {:id (if (= :per-act grain) [:act (:name offer) p] [:value fid])
     :grain grain
     :row? (boolean (or (#{:personal :hand} (get-in layers [layer :kind]))
                        (contains? (:mark f) :own-row)))
     :store store
     :part p
     :under (if (or (= :per-act grain) (= :act (get-in st [:config :lock-subjects])))
              (act-subjects st offer)
              (fact-subjects layer f))
     :mode (if (contains? (:mark f) :die-with-any) :all :any)}))

(defn erasure
  "Why a fact's value can no longer be opened, or nil while it can: its lock
  row deleted or its wrapped lock excised, or the person locks it was
  wrapped under destroyed."
  [st f]
  (when-let [lk (:lock f)]
    (or (get-in st [(:store lk) :parts (:part lk) :erased (:id lk)])
        (let [under (:under lk)
              dead (keep #(get-in st [:persons %]) under)]
          (when (seq under)
            (case (:mode lk)
              :any (when (= (count dead) (count under)) (apply max-key :order dead))
              :all (when (seq dead) (apply min-key :order dead))))))))

(defn readable? [st f] (boolean (and f (nil? (erasure st f)))))

;; ------------------------------------------------------------------- gate

(defn- refusal
  "Why the gate refuses the facts it is deciding now, or nil."
  [st offer here]
  (let [cfg (:config st)
        rs (keep (fn [[_ f]] (:replaces f)) here)]
    (cond
      (and (= :per-act (:act-layer cfg))
           (some #(not= (:layer offer) (:layer %)) (:facts offer)))
      :fact-outside-the-acts-layer

      (some (fn [[_ f]] (not= (:class offer)
                              (get-in st [:settings (layer-of st offer f) :class])))
            here)
      :class-mismatch

      (or (not= (count rs) (count (set rs)))
          (some (fn [[_ f]] (and (:replaces f)
                                 (not (replaceable? st (layer-of st offer f) f))))
                here))
      :stale-replaces

      (or (:source-erased offer)
          (and (:landing-checks-source cfg) (:source offer)
               (not (readable? st (fact-by-id st (:source offer))))))
      :source-erased)))

(defn- stamp-for
  "Ruling 4 as a hybrid clock: never behind the leader's wall clock, never
  back on any partition the act lands on, always after anything the act
  stood on."
  [st store parts offer here]
  (let [wall (+ (:tick st) (get-in st [store :skew]))
        before (concat (map #(get-in st [store :parts % :clock]) parts)
                       (keep #(:stamp (fact-by-id st %))
                             (concat (:stood-on offer)
                                     (keep (fn [[_ f]] (:replaces f)) here)
                                     (some-> (:source offer) vector))))]
    (apply max wall (map inc before))))

(defn- apply-control [st f layer stamp]
  (case (:k f)
    :lock-grain (assoc-in st [:settings layer :grain] (:v f))
    :class (assoc-in st [:settings layer :class] (:v f))
    :forget (let [lk (:lock (fact-by-id st (get-in f [:v :target])))]
              (if (or (nil? lk)
                      (get-in st [(:store lk) :parts (:part lk) :erased (:id lk)]))
                st
                (-> st
                    (assoc-in [(:store lk) :parts (:part lk) :erased (:id lk)]
                              {:stamp stamp :tick (:tick st) :order (:order st)
                               :how (if (:row? lk) :row-deleted :excised)})
                    next-order
                    (update-in [(:store lk) :parts (:part lk) :locks] dissoc (:id lk)))))
    st))

(defn- admit [st store p offer here stamp]
  (reduce
   (fn [st [i f]]
     (let [layer (layer-of st offer f)
           fid [(:name offer) i]
           lk (when-not (control-keys (:k f)) (lock-for st store p offer f fid layer))]
       (-> st
           (update-in [store :parts p :log] conj
                      {:id fid :act (:name offer) :idx i :layer layer
                       :e (:e f) :k (:k f) :v (:v f) :replaces (:replaces f)
                       :mark (:mark f) :lock lk :stamp stamp :tick (:tick st) :order (:order st)
                       :by (:who offer) :store store :part p
                       :subjects (act-subjects st offer)
                       :own-subjects (fact-subjects layer f)
                       :stood-on (:stood-on offer) :source (:source offer)
                       :because-of (:because-of offer) :released-at (:released-at offer)})
           (cond-> (:row? lk) (assoc-in [store :parts p :locks (:id lk)] lk))
           next-order
           (apply-control f layer stamp))))
   st here))

(defn- decide
  "Admit or refuse the facts `here` on `parts` at one stamp, and record the
  answer on each of those partitions."
  [st store parts offer here stamp reason]
  (let [st (if reason
             st
             (reduce (fn [st p] (admit st store p offer (filter #(= p (nth % 3)) here) stamp))
                     st parts))]
    (reduce (fn [st p]
              (-> st
                  (assoc-in [store :parts p :answers (:name offer)]
                            {:answer (if reason :no :yes) :reason reason :stamp stamp
                             :because-of (:because-of offer)})
                  (update-in [store :parts p :clock] max stamp)
                  (update-in [store :parts p :stamps] conj stamp)))
            st parts)))

(defn- say-decision [st store p offer & [who]]
  (let [a (get-in st [store :parts p :answers (:name offer)])]
    (note st "    " (name store) " p" p " (" (or who (str "epoch " (get-in st [store :epoch]))) "): "
          (:name offer) " " (name (:answer a))
          (when (:reason a) (str " (" (name (:reason a)) ")"))
          " at stamp " (:stamp a))))

;; ---------------------------------------------------------------- sending

(defn deliver-offer
  "Put an offer on the queue of each gate its facts go to."
  [st offer]
  (let [plan (stream-plan st offer)]
    (cond-> st
      (seq plan) (update-in [:stream :inbox (first plan)] conj {:offer offer :stage 0})
      (some #(= :micro (nth % 2)) (placed st offer)) (update-in [:micro :inbox] conj offer))))

(defn- send-offer [st offer client?]
  (-> st
      (assoc-in [:sent (:name offer)] offer)
      (cond-> client? (update :client-sent conj (:name offer)))
      (deliver-offer offer)))

(defn- forward
  "A promotion's second offer, into the shared layer. Under :minted the store
  names it as it sends it; under :carried it uses the name the person put in
  the request."
  [st req]
  (let [{:keys [source target replaces fwd-name]} (:promote req)
        src (fact-by-id st source)
        [nm st] (if (= :carried (get-in st [:config :forward-name]))
                  [fwd-name st]
                  (fresh st (str (:name req) "-m")))
        gone (not (readable? st src))]
    (-> (next-order st)
        (note "    promotion " (:name req) " sends its landing offer " nm " to the micro gate"
              (when gone " (source already erased)"))
        (send-offer {:name nm :who (:who req) :layer target :class :by-entity
                     :permission (:permission req) :because-of (:name req)
                     :source source :source-erased gone
                     :released-at (when-not gone (:order st))
                     :stood-on [source [(:name req) 0]]
                     :facts [{:layer target :e (:e src) :k (:k src)
                              :v (when-not gone (:v src)) :replaces replaces :mark #{}}]}
                    false))))

;; ------------------------------------------------------------ stream gate

(defn stream-step
  "The stream leader handles the next event on partition p: one partition's
  share of an offer, or the acknowledgement that ends it."
  [st p]
  (if-let [{:keys [offer stage]} (first (get-in st [:stream :inbox p]))]
    (let [nm (:name offer)
          st (-> st
                 (update-in [:stream :inbox p] #(vec (rest %)))
                 (assoc-in [:stream :unacked nm] offer))]
      (if (= :ack stage)
        (-> st
            (update-in [:stream :unacked] dissoc nm)
            (note "    stream p" p ": " nm " finished and acknowledged"))
        (let [plan (stream-plan st offer)
              here (on st offer :stream p)
              st (if (get-in st [:stream :parts p :answers nm])
                   (note st "    stream p" p ": " nm " already decided here")
                   (-> st
                       (decide :stream [p] offer here
                               (stamp-for st :stream [p] offer here)
                               (refusal st offer here))
                       (say-decision :stream p offer)))
              last? (= stage (dec (count plan)))
              st (if (and last? (:promote offer)
                          (= :yes (get-in st [:stream :parts p :answers nm :answer])))
                   (forward st offer)
                   st)]
          (update-in st [:stream :inbox (if last? p (plan (inc stage)))]
                     conj {:offer offer :stage (if last? :ack (inc stage)) :cont true}))))
    st))

;; ------------------------------------------------------------- micro gate

(defn- micro-decision [w offer]
  (let [here (filter #(= :micro (nth % 2)) (placed w offer))
        parts (vec (sort (distinct (map #(nth % 3) here))))]
    (when (and (seq parts)
               (not-any? #(get-in w [:micro :parts % :answers (:name offer)]) parts))
      {:offer offer :parts parts :here here
       :reason (refusal w offer here)
       :stamp (stamp-for w :micro parts offer here)})))

(defn- commit-decision [st {:keys [offer parts here stamp reason]} & [who]]
  (let [st (-> st
               (decide :micro parts offer here stamp reason)
               (say-decision :micro (first parts) offer who))]
    (if (and (:promote offer) (nil? reason)) (forward st offer) st)))

(defn micro-prepare
  "The micro leader decides everything queued, in order, each offer seeing
  the ones before it; nothing is visible until the commit."
  [st]
  (let [batch (get-in st [:micro :inbox])
        [_ delta] (reduce (fn [[w ds] o]
                            (if-let [d (micro-decision w o)]
                              [(decide w :micro (:parts d) o (:here d) (:stamp d) (:reason d))
                               (conj ds d)]
                              [w ds]))
                          [st []] batch)
        skipped (remove (set (map (comp :name :offer) delta)) (map :name batch))]
    (-> st
        (assoc-in [:micro :prepared] {:epoch (get-in st [:micro :epoch])
                                      :n (count batch) :delta delta})
        (note "    micro leader (epoch " (get-in st [:micro :epoch]) ") prepares a batch"
              (when (seq batch) (str ": " (str/join ", " (map :name batch))))
              (when (seq skipped) (str "; already decided, skipped: " (str/join ", " skipped)))))))

(defn micro-commit [st]
  (if-let [prepared (get-in st [:micro :prepared])]
    (-> (reduce commit-decision (note st "    micro commits its batch") (:delta prepared))
        (update-in [:micro :inbox] #(vec (drop (:n prepared) %)))
        (assoc-in [:micro :prepared] nil))
    st))

;; -------------------------------------------------------------- failover

(defn failover
  "A new leader with the next epoch. The micro leader's prepared batch is
  lost with the old leader. The stream leader's pending hops are lost, and
  every offer it had begun and not acknowledged starts again from the top."
  [st store skew]
  (let [old-skew (get-in st [store :skew])
        old-epoch (get-in st [store :epoch])
        st (-> st
               (note "    " (name store) " fails over to epoch " (inc (get-in st [store :epoch]))
                     "; the new leader's clock is off by " skew)
               (update-in [store :epoch] inc)
               (assoc-in [store :skew] skew))]
    (case store
      :micro (-> st
                 (assoc-in [:micro :zombie] (get-in st [:micro :prepared]))
                 (assoc-in [:micro :prepared] nil))
      :stream (let [{:keys [inbox unacked]} (:stream st)]
                (reduce (fn [st o]
                          (-> st
                              (update-in [:stream :inbox (first (stream-plan st o))]
                                         #(into [{:offer o :stage 0}] %))
                              (note "    stream will replay " (:name o) " from its start")))
                        (-> st
                            (assoc-in [:stream :inbox] (mapv #(vec (remove :cont %)) inbox))
                            (assoc-in [:stream :zombie]
                                      (vec (for [q inbox e q :when (:cont e)]
                                             (assoc e :skew old-skew :epoch old-epoch))))
                            (assoc-in [:stream :unacked] {}))
                        (vals unacked))))))

(defn zombie
  "The deposed leader acts on what it held when it was deposed. With
  fencing its epoch is refused and nothing happens."
  [st store]
  (let [fenced (get-in st [:config :fencing])]
    (case store
      :micro
      (if-let [z (get-in st [:micro :zombie])]
        (let [st (assoc-in st [:micro :zombie] nil)]
          (if fenced
            (note st "    the deposed micro leader tries to commit; epoch " (:epoch z) " is refused")
            (reduce #(commit-decision %1 %2 (str "deposed leader, epoch " (:epoch z)))
                    (note st "    the deposed micro leader commits its old batch")
                    (:delta z))))
        st)
      :stream
      (if-let [{:keys [offer stage skew epoch]} (first (get-in st [:stream :zombie]))]
        (let [st (update-in st [:stream :zombie] #(vec (rest %)))]
          (cond
            fenced (note st "    the deposed stream leader tries a step of " (:name offer)
                         "; its epoch is refused")
            (= :ack stage) st
            ;; it decides from what it saw before the failover: no dedup, its own clock
            :else (let [p ((stream-plan st offer) stage)]
                    (-> st
                        (decide :stream [p] offer (on st offer :stream p) (+ (:tick st) skew) nil)
                        (say-decision :stream p offer (str "deposed leader, epoch " epoch))))))
        st))))

;; ---------------------------------------------------- the offerers' side

(defn- resolve-fact
  "The offerer turns a generated fact spec into a fact, reading the store for
  the chain head it means to replace."
  [st layer spec]
  (let [l (or (:other-layer spec) layer)
        [tok st] (fresh st "v")
        in-layer (filter #(= l (:layer %)) (all-facts st))
        replaced (set (keep :replaces in-layer))
        chain (sort-by :stamp (filter #(and (= (:e spec) (:e %)) (= (:k spec) (:k %))) in-layer))
        head (chain-head st l (:e spec) (:k spec))
        stale (first (filter #(replaced (:id %)) chain))]
    [{:layer l :e (:e spec) :k (:k spec)
      :v (cond-> {:token tok} (= :mention (:k spec)) (assoc :person (:mention spec)))
      :replaces (case (:replaces spec) :none nil :head (:id head) :stale (:id stale))
      :mark (:mark spec)}
     st]))

(defn- send-again
  "The offerer did not hear back in time and sends the same offer again."
  [st offer times]
  (reduce (fn [st _]
            (-> st
                (note "the offerer of " (:name offer) " sends it again under the same name")
                (deliver-offer offer)))
          st (range (dec (or times 1)))))

(defn- op-offer [st {:keys [who layer facts stood-on times]}]
  (let [[nm st] (fresh st "o")
        [fs st] (reduce (fn [[fs st] spec]
                          (let [[f st] (resolve-fact st layer spec)] [(conj fs f) st]))
                        [[] st] facts)
        pool (sort-by (juxt :tick :id) (all-facts st))
        offer {:name nm :who who :layer layer
               :class (get-in st [:settings layer :class])
               :permission [:session who]
               :stood-on (if (and stood-on (seq pool))
                           [(:id (nth pool (mod stood-on (count pool))))]
                           [])
               :facts fs}]
    (-> st
        (note (name who) " offers " nm " (act layer " (name layer) ", class "
              (name (:class offer)) "): " (str/join "; " (map describe-fact fs)))
        (send-offer offer true)
        (send-again offer times))))

(defn- op-retry [st i]
  (let [names (:client-sent st)]
    (if (empty? names)
      st
      (let [nm (nth (rseq names) (mod i (count names)))]
        (-> st
            (note "the offerer of " nm " sends it again under the same name")
            (deliver-offer (get-in st [:sent nm])))))))

(defn- op-promote [st i target times]
  (let [cands (reverse (sort-by (juxt :tick :id)
                                (filter #(and (= :alice (:layer %)) (:lock %) (readable? st %))
                                        (all-facts st))))]
    (if (empty? cands)
      st
      (let [src (nth cands (mod i (count cands)))
            [nm st] (fresh st "o")
            head (chain-head st target (:e src) (:k src))]
        (-> st
            (note "alice asks to promote " (fid-str (:id src)) " (" (get-in src [:v :token])
                  ") into " (name target) " as " nm)
            (as-> st (let [req {:name nm :who :alice :layer :alice
                                :class (get-in st [:settings :alice :class])
                                :permission [:session :alice] :stood-on [(:id src)]
                                :promote {:source (:id src) :target target :replaces (:id head)
                                          :fwd-name (str nm "-fwd")}
                                :facts [{:layer :alice :e (:e src) :k :promote-request
                                         :v {:source (:id src) :target target} :mark #{}}]}]
                       (-> st (send-offer req true) (send-again req times)))))))))

(defn- op-forget-value [st i]
  (let [cands (sort-by (juxt :tick :id) (filter #(and (:lock %) (readable? st %)) (all-facts st)))]
    (if (empty? cands)
      st
      (let [t (nth cands (mod i (count cands)))
            lk (:lock t)
            who (or (get-in layers [(:layer t) :owner]) :operator)
            [nm st] (fresh st "o")]
        (-> st
            (note (name who) " forgets " (fid-str (:id t)) " (" (get-in t [:v :token]) ") as " nm
                  (if (:row? lk)
                    ": its lock row will be deleted"
                    ": its lock is in the record, so the operator excises it"))
            (send-offer {:name nm :who who :layer (:layer t)
                         :class (get-in st [:settings (:layer t) :class])
                         :permission [:session who] :stood-on [(:id t)]
                         :facts [{:layer (:layer t) :e (:e t) :k :forget
                                  :v {:target (:id t)} :mark #{}
                                  :at [(:store lk) (:part lk)]}]}
                        true))))))

(defn- op-forget-person [st p]
  (if (get-in st [:persons p])
    st
    (let [s (inc (now st))]
      (-> st
          (assoc-in [:persons p] {:stamp s :tick (:tick st) :order (:order st)})
          next-order
          (assoc :lock-clock s)
          (note (name p) " is forgotten: their person lock is destroyed at stamp " s)))))

(defn- op-layer-setting [st layer k v]
  (let [[nm st] (fresh st "o")
        cls (get-in st [:settings layer :class])
        who (if (= :class k) :operator (get-in layers [layer :owner]))]
    (-> st
        (note (name who) " sets " (name layer) " " (name k) " to " (name v) " as " nm)
        (send-offer {:name nm :who who :layer layer :class cls
                     :permission [:session who] :stood-on []
                     :facts [{:layer layer :e layer :k k :v v :mark #{}
                              :at [(gate-of st layer cls) (get-in layers [layer :home])]}]}
                    true))))

;; ------------------------------------------------------------------ reads

(defn promotion-status
  "The person's view of a promotion as of T, with no optimism: done only once
  the landing is in the shared layer."
  [st req T]
  (let [landed (filter #(and (= req (:because-of %)) (<= (:stamp %) T)) (facts-in st :micro))
        refused (for [p (get-in st [:micro :parts])
                      [_ a] (:answers p)
                      :when (and (= req (:because-of a)) (= :no (:answer a)) (<= (:stamp a) T))]
                  a)]
    {:request req
     :status (cond (seq landed) :done (seq refused) :refused :else :pending)
     :landing-stamps (mapv :stamp landed)}))

(defn read-as-of
  "What a reader sees as of stamp T: every admitted fact stamped at or before
  T, with its value or the date it was erased, and each promotion's status."
  [st T]
  (let [facts (all-facts st)]
    {:tick (:tick st)
     :as-of T
     :facts (vec (for [f facts
                       :when (<= (:stamp f) T)
                       :let [e (erasure st f)]]
                   (cond-> (select-keys f [:id :act :layer :stamp :tick :order :source :released-at])
                     e (assoc :erased-at (:stamp e))
                     (not e) (assoc :value (:v f)))))
     ;; every fact erased by now, whatever its stamp, and when it was erased
     :erased (into {} (for [f facts
                            :let [e (erasure st f)]
                            :when e]
                        [(:id f) {:layer (:layer f) :tick (:tick e) :order (:order e) :stamp (:stamp e)}]))
     :promotions (vec (for [f facts
                            :when (and (= :promote-request (:k f)) (<= (:stamp f) T))]
                        (promotion-status st (:act f) T)))}))

(defn- op-read [st as-of]
  (let [pool (sort-by (juxt :stamp :id) (all-facts st))
        T (if (and (vector? as-of) (seq pool))
            (:stamp (nth pool (mod (second as-of) (count pool))))
            (now st))
        r (read-as-of st T)]
    (-> st
        (update :reads conj r)
        (note "a read as of stamp " T " sees "
              (if (empty? (:facts r))
                "nothing"
                (str/join ", " (for [f (:facts r)]
                                 (str (fid-str (:id f)) "@" (:stamp f) " "
                                      (if (:erased-at f)
                                        (str "erased on " (:erased-at f))
                                        (or (get-in f [:value :token]) "(setting)"))))))
              (when (seq (:promotions r))
                (str "; promotions: "
                     (str/join ", " (for [p (:promotions r)]
                                      (str (:request p) " " (name (:status p)))))))))))

;; ------------------------------------------------------------------- runs

(defn step
  "Apply one op of a history. Picks are indexes the model resolves against
  what exists, so a shrunk history still means something."
  [st op]
  (let [st (update st :tick inc)
        [kind a b c] op]
    (case kind
      :offer (op-offer st a)
      :retry (op-retry st a)
      :promote (op-promote st a b c)
      :step (stream-step st a)
      :prepare (micro-prepare st)
      :commit (micro-commit st)
      :batch (-> st micro-prepare micro-commit)
      :work (loop [st st, n 0]
              (if (and (seq (get-in st [:stream :inbox a])) (< n 200))
                (recur (stream-step st a) (inc n))
                st))
      :failover (failover st a b)
      :zombie (zombie st a)
      :forget-value (op-forget-value st a)
      :forget-person (op-forget-person st a)
      :set-grain (op-layer-setting st a :lock-grain :per-act)
      :reclass (op-layer-setting st a :class :by-entity)
      :read (op-read st a))))

(defn- quiet? [st]
  (and (every? empty? (get-in st [:stream :inbox]))
       (empty? (get-in st [:micro :inbox]))))

(defn- unanswered [st]
  (for [nm (distinct (:client-sent st)) :when (empty? (answers-for st nm))] nm))

(defn drain
  "After the history: no more failures. The stream gate works off its queues,
  then the micro gate takes a batch; every offerer still waiting sends
  again; until nothing moves."
  [st]
  (loop [st (note st "-- drain: no more failovers; the gates work off their queues")
         rounds 0]
    (let [st (loop [st st, n 0]
               (if-let [p (first (filter #(seq (get-in st [:stream :inbox %])) (range n-parts)))]
                 (if (< n 500) (recur (step st [:step p]) (inc n)) st)
                 st))
          st (if (seq (get-in st [:micro :inbox])) (-> st (step [:prepare]) (step [:commit])) st)
          st (if (quiet? st)
               (reduce (fn [st nm]
                         (-> st (update :tick inc)
                             (note "the offerer of " nm " has no answer and sends it again")
                             (deliver-offer (get-in st [:sent nm]))))
                       st (unanswered st))
               st)]
      (if (or (<= 30 rounds) (and (quiet? st) (empty? (unanswered st))))
        st
        (recur st (inc rounds))))))

(defn run
  "Play a history from an empty store, drain it, and take a last read as of now."
  [config history]
  (let [st (drain (reduce step (init config) history))]
    (-> st
        (update :tick inc)
        (as-> st (update st :reads conj (read-as-of st (now st)))))))
