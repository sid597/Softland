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

  Order between the two stores exists only through stood-on: a landing
  stands on its crossing fact, so landing-after-read-out is defined; a
  forget and a read-out on one partition are ordered by it; a forget
  against a landing is ordered by nothing. A check one gate makes against
  the other store is a read, never atomic with that store's writes. The
  properties may use `:order`, the true order of every effect, as their
  ruler; the gates never do.

  Where PROGRESS.md is silent the model takes a reading. Each reading is an
  entry in the config, so a run can turn it the other way: `ruled` holds the
  literal readings, `readings` the other side of each, and `baseline`, the
  default, is `ruled` with every reading in `readings` turned."
  (:require [clojure.string :as str]))

;; ------------------------------------------------------------------ world

(def people [:alice :bob])

(def n-parts "N, the task count, fixed at launch (ruling 2)." 3)

(def entities [:e0 :e1 :e2 :e3])
(def entity-part {:e0 0 :e1 1 :e2 2 :e3 0 :perm-alice 1 :perm-bob 2})

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
  #{:forget :lock-grain :class :promote-request :crossed :permission :revoke})

(def permissions
  "Who may write into which layer, and where the permission's fact lives:
  [person layer where]. :own lives in the layer it grants writes into;
  :session in the person's session, the root narrower permissions sit under
  (only Alice has one here, her hand session). Both kinds are first facts
  in every run; which a gate accepts is the reading in :permission-home."
  (vec (concat (for [l [:alice :alice-hand :alice-agent :group :base]] [:alice l :own])
               (for [l [:alice :alice-agent :group :base]] [:alice l :session])
               [[:bob :group :own] [:bob :base :own]])))

(defn permission-layer
  "The layer a permission's fact lives in."
  [[_ layer where]]
  (if (= :session where) :alice-hand layer))

(defn perm-entity [[who _ _]] (keyword (str "perm-" (name who))))

(defn exempt?
  "The operator and the store's own steps act at the root; they cite no
  permission."
  [offer]
  (contains? #{:operator :store} (:who offer)))

;; ----------------------------------------------------------------- config

(def ruled
  "The literal reading of PROGRESS.md; where it is silent, the plain one."
  {:act-layer             :per-fact ; 'layer' is a named part; fact or act is not said
   :reclass-moves-gate    false     ; ruling 2 re-classes placement; ruling 1 picks the gate by ownership
   :forward-name          :minted   ; a promotion's second offer is named by the store as it sends it;
                                    ; :derived names it from the request's name under a scheme,
                                    ; :carried has the person make a second random name up front
   :lock-subjects         :act      ; ruling 7: per-value locks inherit the act's subject list
   :wrap                  :any-subject ; 7b over every subject, the owner one among them; see `wrap`
   :landing-checks-source false     ; the micro gate does not look at the source again when it lands a copy
   :name-row              false     ; the micro gate records an answer only where the offer's facts land
   :digest-check          false     ; a decided name answers any offer under it from the record
   :name-tag              :none     ; what a name's scheme tag carries; :layer-and-class, the
                                    ; layer and class it was made for, so it reaches one gate
   :permission-home       :anywhere ; a gate takes a cited permission wherever its fact lives
   :fencing               true      ; the epoch: a deposed leader's writes are refused
   ;; how two of the eight properties are read
   :p4-erasure-exempt     false     ; P4 literally: a read shows nothing dated after its moment
   :p6-copies             :none})   ; P6 literally: after a forget no copy anywhere returns the value;
                                    ; :landed-before exempts copies that had landed before the forget,
                                    ; :crossed those whose crossing fact came before it

(def readings
  "Each entry turns one reading the other way. The first six Sid confirmed as
  his on 24 September."
  (array-map
   :layer-on-the-act         {:act-layer :per-act}
   :reclass-moves-the-gate   {:reclass-moves-gate true}
   :landing-named-first      {:forward-name :derived}
   :value-locks-own-subjects {:lock-subjects :fact}
   :p4-erasure-shows-its-date {:p4-erasure-exempt true}
   :p6-line-at-the-read-out  {:p6-copies :crossed}
   ;; change A, confirmed 24 September
   :owner-required           {:wrap :owner-required}
   ;; change C
   :findable-by-name         {:name-row true}
   ;; change E
   :reuse-refused-by-digest  {:digest-check true}
   ;; proposed in round 2 to make E hold, Sid's since 25 September: without
   ;; it a reuse that reaches the other store is invisible to the digest
   :name-carries-layer-and-class {:name-tag :layer-and-class}
   ;; change D
   :permissions-in-their-layer {:permission-home :in-their-layer}))

(def baseline
  "The default configuration: every reading turned."
  (apply merge ruled (vals readings)))

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
   :sent {}        ; offer name -> the first offer sent under it, for every offer anyone made
   :reuses []      ; later offers sent under a name already used, with other content (change E)
   :offers {}      ; digest -> offer
   :name-tags {}   ; name -> [layer class] it was made for; stands for the name's own tag
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

(defn answers-for
  "Every answer recorded under a name, found by scanning every partition."
  [st nm]
  (for [s [:stream :micro]
        [i p] (map-indexed vector (get-in st [s :parts]))
        :let [a (get-in p [:answers nm])]
        :when a]
    (assoc a :where [s i])))

(defn name-part
  "The micro partition a name picks for its name row (change C)."
  [nm]
  (mod (hash nm) n-parts))

(defn lookup
  "Change C: the answer to an offer from its name and its layer, by point
  reads, never a scan. A one-owner layer's answers are on its home partition
  in the stream store while it is placed by layer; once re-classed onto the
  micro gate, and for every shared layer, the name row on the micro partition
  the name picks. Without a name tag, a one-owner layer takes two reads,
  home then name row, since a re-class moves its later names. Under
  :name-tag :layer-and-class the name itself says its layer and class, so
  one read. Given the asker's digest and under :digest-check (change E), a
  record made for other content answers no: the name is taken."
  [st nm layer & [digest]]
  (let [[tl tc] (when (= :layer-and-class (get-in st [:config :name-tag]))
                  (get-in st [:name-tags nm]))
        layer (or tl layer)
        home #(some-> (get-in st [:stream :parts (get-in layers [layer :home]) :answers nm])
                      (assoc :where [:stream (get-in layers [layer :home])]))
        row #(some-> (get-in st [:micro :parts (name-part nm) :answers nm])
                     (assoc :where [:micro (name-part nm)]))
        rec (cond
              (and tc (get-in layers [layer :one-owner]) (= :by-layer tc)) (home)
              tc (row)
              ;; no tag, or the store's own read-out, which claims no class
              :else (or (when (get-in layers [layer :one-owner]) (home)) (row)))]
    (if (and rec digest (get-in st [:config :digest-check]) (not= digest (:digest rec)))
      {:answer :no :reason :name-taken :where (:where rec)}
      rec)))

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
       (when-let [ps (get-in f [:v :persons])] (str " " (str/join "+" (map name (sort ps)))))
       (when-let [t (get-in f [:v :token])] (str " " t))
       " in " (name (:layer f))
       (when (:replaces f) (str ", replacing " (fid-str (:replaces f))))
       (when (seq (:mark f)) (str ", marked " (str/join "," (map name (:mark f)))))))

;; --------------------------------------------------------------- subjects

(defn fact-subjects
  "Ruling 8's three sources applied to one fact: the layer's owner, the
  key's grammar (a :mention names one or two people), and the tool (none
  here)."
  [layer f]
  (cond-> #{}
    (get-in layers [layer :owner]) (conj (get-in layers [layer :owner]))
    (= :mention (:k f)) (into (get-in f [:v :persons]))))

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

;; ------------------------------------------------------------ permissions

(defn permission-fact
  "The admitted fact that grants permission `pid`, or nil."
  [st pid]
  (some #(when (and (= :permission (:k %)) (= pid (get-in % [:v :id]))) %) (all-facts st)))

(defn revocation
  "The admitted fact that revokes permission `pid`, or nil."
  [st pid]
  (some #(when (and (= :revoke (:k %)) (= pid (get-in % [:v :permission]))) %) (all-facts st)))

;; ------------------------------------------------------------------ locks

(defn wrap
  "Whose person locks a value's lock is wrapped under, as {:required #{..}
  :any-of #{..}}: it opens while every required lock is alive and, when
  :any-of is not empty, at least one of those. `subjects` are the people it
  is about, `owner` the layer's person owner (nil in shared layers),
  `marked?` a mark to die with any of them (7b). The readings of change A:
    :any-subject        7b over every subject; the owner is one among them
    :owner-required     A as confirmed: in a one-owner layer the owner's lock
                        always, the others only by a mark (7b's survive
                        default among them, as Sid ruled on 25 September);
                        in a shared layer 7b as written
    :owner-and-an-other A read the other way: the owner's lock, and one of
                        the other subjects' while there are others
    :owner-and-marked   the third reading: only the owner's lock and marked
                        subjects, so an unmarked subject in a shared layer
                        gives no wrap at all"
  [reading owner subjects marked?]
  (let [subjects (cond-> (set subjects) owner (conj owner))
        others (disj subjects owner)]
    (cond
      marked? {:required subjects :any-of #{}}
      (= :any-subject reading) {:required #{} :any-of subjects}
      owner {:required #{owner}
             :any-of (if (= :owner-and-an-other reading) others #{})}
      (= :owner-and-marked reading) {:required #{} :any-of #{}}
      :else {:required #{} :any-of subjects})))

(defn wrap-closed
  "The forget that closed a wrap, from `persons` (person -> the record of
  their forget), or nil while it opens."
  [{:keys [required any-of]} persons]
  (let [dead-req (keep persons required)
        dead-any (keep persons any-of)]
    (first (sort-by :order (cond-> (vec dead-req)
                             (and (seq any-of) (= (count dead-any) (count any-of)))
                             (conj (apply max-key :order dead-any)))))))

(defn- lock-for
  "Ruling 7. The value's own lock, or its act's in a per-act layer; a row in
  the lock store for personal and hand layers or when marked, otherwise
  wrapped into the record; wrapped under the people it is about as `wrap`
  reads change A."
  [st store p offer f fid layer]
  (let [grain (get-in st [:settings layer :grain])]
    {:id (if (= :per-act grain) [:act (:name offer) p] [:value fid])
     :grain grain
     :row? (boolean (or (#{:personal :hand} (get-in layers [layer :kind]))
                        (contains? (:mark f) :own-row)))
     :store store
     :part p
     :wrap (wrap (get-in st [:config :wrap])
                 (get-in layers [layer :owner])
                 (if (or (= :per-act grain) (= :act (get-in st [:config :lock-subjects])))
                   (act-subjects st offer)
                   (fact-subjects layer f))
                 (contains? (:mark f) :die-with-any))}))

(defn erasure
  "Why a fact's value can no longer be opened, or nil while it can: its lock
  row deleted or its wrapped lock excised, or its wrap closed by a person's
  forget."
  [st f]
  (when-let [lk (:lock f)]
    (or (get-in st [(:store lk) :parts (:part lk) :erased (:id lk)])
        (wrap-closed (:wrap lk) (:persons st)))))

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

      ;; a read-out is the store's own step, placed where the source's lock is; it claims no class
      (and (not (:read-out offer))
           (some (fn [[_ f]] (not= (:class offer)
                                   (get-in st [:settings (layer-of st offer f) :class])))
                 here))
      :class-mismatch

      ;; change D: the cited permission covers this write, lives where the
      ;; reading says a gate may take it from, exists, and is not revoked
      (and (not (exempt? offer))
           (let [[pw pl _] (:permission offer)] (or (not= pw (:who offer)) (not= pl (:layer offer)))))
      :permission-does-not-cover-this

      (and (not (exempt? offer))
           (= :in-their-layer (:permission-home cfg))
           (not= (permission-layer (:permission offer)) (:layer offer)))
      :permission-from-another-layer

      (and (not (exempt? offer)) (nil? (permission-fact st (:permission offer))))
      :no-permission

      (and (not (exempt? offer)) (revocation st (:permission offer)))
      :permission-revoked

      (or (not= (count rs) (count (set rs)))
          (some (fn [[_ f]] (and (:replaces f)
                                 (not (replaceable? st (layer-of st offer f) f))))
                here))
      :stale-replaces

      (and (:read-out offer) (nil? (:lock (fact-by-id st (get-in offer [:read-out :source])))))
      :source-has-no-value

      ;; the read-out opens the source through its lock, on the partition that holds it
      (or (and (:read-out offer)
               (not (readable? st (fact-by-id st (get-in offer [:read-out :source])))))
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
                       :because-of (:because-of offer) :crossing (:crossing offer)
                       :digest (:digest offer)})
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
                             :tick (:tick st) :because-of (:because-of offer)
                             :digest (:digest offer)})
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

(defn digest-of
  "Change E: a digest of what an offer says, its name aside."
  [offer]
  (hash (dissoc offer :name :digest)))

(defn tag-of
  "What a name made for this offer would carry under :name-tag
  :layer-and-class: its layer, and the class that places it. An offer the
  store places itself (a forget or a setting with the value or layer it
  acts on, a read-out with the source's lock) is placed by no class, so its
  name carries none."
  [offer]
  [(:layer offer) (when-not (every? :at (:facts offer)) (:class offer))])

(defn mis-tagged?
  "Under :name-tag :layer-and-class, whether an offer claims another layer or
  class than its name was made for; a gate refuses it on its face, reading
  nothing."
  [st offer]
  (and (= :layer-and-class (get-in st [:config :name-tag]))
       (not= (tag-of offer) (get-in st [:name-tags (:name offer)]))))

(defn- with-digest [offer]
  (if (:digest offer) offer (assoc offer :digest (digest-of offer))))

(defn deliver-offer
  "Put an offer on the queue of each gate its facts go to."
  [st offer]
  (let [offer (with-digest offer)
        plan (stream-plan st offer)]
    (cond-> st
      (seq plan) (update-in [:stream :inbox (first plan)] conj {:offer offer :stage 0})
      (some #(= :micro (nth % 2)) (placed st offer)) (update-in [:micro :inbox] conj offer))))

(defn- send-offer [st offer client?]
  (let [offer (with-digest offer)
        before (get-in st [:sent (:name offer)])]
    (-> st
        (assoc-in [:offers (:digest offer)] offer)
        (cond-> (nil? before) (-> (assoc-in [:sent (:name offer)] offer)
                                  (assoc-in [:name-tags (:name offer)] (tag-of offer)))
                (and before (not= (:digest before) (:digest offer))) (update :reuses conj offer)
                client? (update :client-sent conj (:name offer)))
        (deliver-offer offer))))

(defn landing-name
  "The landing offer's name derived from its request's: a name under the
  landing scheme, so it is fixed before the first gate and needs no second
  random name."
  [req-name]
  (str "landing:" req-name))

(defn crossing-name
  "The read-out's name, always derived from the request's: it is the gate's
  own next step on the owner's partition, and a replay must find it decided."
  [req-name]
  (str "crossing:" req-name))

(defn crossing-id [req-name] [(crossing-name req-name) 0])

(defn- send-read-out
  "A promotion's request is in. The gate's next step is the read-out, on the
  partition that holds the source's lock: it opens the value through the
  lock and writes the crossing fact there, or, the source already erased,
  refuses. Until then the promotion is pending and the value forgettable."
  [st req]
  (let [{:keys [source]} (:promote req)
        src (fact-by-id st source)
        lk (:lock src)
        ;; where two facts share an id the pointer may resolve to one with no
        ;; value; the read-out then goes where the request is, to refuse
        [s p] (if lk [(:store lk) (:part lk)] (let [[_ _ s p] (first (placed st req))] [s p]))
        nm (crossing-name (:name req))]
    (-> st
        (note "    promotion " (:name req) " queues its read-out " nm
              " on " (name s) " p" p)
        (send-offer {:name nm :who :store :layer (:layer src)
                     :permission (:permission req) :because-of (:name req)
                     :read-out {:request (:name req) :source source}
                     :promote (:promote req)
                     :stood-on [source [(:name req) 0]]
                     :facts [{:layer (:layer src) :e (:e src) :k :crossed
                              :v {:request (:name req) :source source} :mark #{}
                              :at [s p]}]}
                    false))))

(defn- forward
  "A promotion's landing offer, into the shared layer, sent at the read-out;
  it stands on the crossing fact. Under :minted the store names it as it
  sends it; under :derived its name comes from the request's; under :carried
  it uses the second name the person put in the request. A replay that finds
  the read-out decided sends it again, while the source can still be opened."
  [st ro]
  (let [{:keys [source target replaces fwd-name]} (:promote ro)
        req (get-in ro [:read-out :request])
        src (fact-by-id st source)]
    (if-not (and (:lock src) (readable? st src))
      (note st "    " (:name ro) " was read out before; its source is erased now, so nothing is sent again")
      (let [[nm st] (case (get-in st [:config :forward-name])
                      :derived [(landing-name req) st]
                      :carried [fwd-name st]
                      :minted (fresh st (str req "-m")))]
        (-> st
            (note "    promotion " req " sends its landing offer " nm " to the micro gate")
            (send-offer {:name nm :who (:who (get-in st [:sent req])) :layer target :class :by-entity
                         :permission (get-in ro [:promote :landing-permission]) :because-of req
                         :source source :crossing (crossing-id req)
                         :stood-on [source (crossing-id req)]
                         :facts [{:layer target :e (:e src) :k (:k src)
                                  :v (:v src) :replaces replaces :mark #{}}]}
                        false))))))

(defn- continue-promotion
  "After a yes: a request goes on to its read-out, a read-out to its landing."
  [st offer]
  (cond (:read-out offer) (forward st offer)
        (:promote offer) (send-read-out st offer)
        :else st))

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
              rec (get-in st [:stream :parts p :answers nm])
              tagged-else? (mis-tagged? st offer)
              taken? (or tagged-else?
                         (and rec (get-in st [:config :digest-check]) (not= (:digest rec) (:digest offer))))
              st (cond
                   tagged-else? (note st "    stream p" p ": " nm " was made for another layer or class; refused on its face")
                   taken? (note st "    stream p" p ": " nm " is taken by other content; refused by its digest")
                   rec (note st "    stream p" p ": " nm " already decided here")
                   :else (-> st
                             (decide :stream [p] offer here
                                     (stamp-for st :stream [p] offer here)
                                     (refusal st offer here))
                             (say-decision :stream p offer)))
              last? (or taken? (= stage (dec (count plan))))
              st (if (and last? (not taken?) (:promote offer)
                          (= :yes (get-in st [:stream :parts p :answers nm :answer])))
                   (continue-promotion st offer)
                   st)]
          (update-in st [:stream :inbox (if last? p (plan (inc stage)))]
                     conj {:offer offer :stage (if last? :ack (inc stage)) :cont true}))))
    st))

;; ------------------------------------------------------------- micro gate

(defn- micro-decision
  "What the micro leader would decide for an offer, or nil if it has none of
  the offer's facts or already decided it. Under :name-row the commit also
  writes the answer on the partition the name picks (change C), so that
  partition is part of the decision."
  [w offer]
  (let [here (filter #(= :micro (nth % 2)) (placed w offer))
        fact-parts (distinct (map #(nth % 3) here))
        parts (vec (sort (distinct (cond-> fact-parts
                                     (and (seq fact-parts) (get-in w [:config :name-row]))
                                     (conj (name-part (:name offer)))))))]
    (when (and (seq parts)
               (not (mis-tagged? w offer))
               (not-any? #(get-in w [:micro :parts % :answers (:name offer)]) parts))
      {:offer offer :parts parts :here here
       :reason (refusal w offer here)
       :stamp (stamp-for w :micro parts offer here)})))

(defn- commit-decision [st {:keys [offer parts here stamp reason]} & [who]]
  (let [st (-> st
               (decide :micro parts offer here stamp reason)
               (say-decision :micro (first parts) offer who))]
    (if (and (:promote offer) (nil? reason)) (continue-promotion st offer) st)))

(defn micro-prepare
  "The micro leader decides everything queued, in order, each offer seeing
  the ones before it; nothing is visible until the commit."
  [st]
  (let [batch (get-in st [:micro :inbox])
        [w delta] (reduce (fn [[w ds] o]
                            (if-let [d (micro-decision w o)]
                              [(decide w :micro (:parts d) o (:here d) (:stamp d) (:reason d))
                               (conj ds d)]
                              [w ds]))
                          [st []] batch)
        decided (set (map (comp :digest :offer) delta))
        skipped (remove #(decided (:digest %)) batch)
        taken? (fn [o] (and (get-in st [:config :digest-check])
                            (some #(when-let [a (get-in w [:micro :parts % :answers (:name o)])]
                                     (not= (:digest a) (:digest o)))
                                  (range n-parts))))
        [taken skipped] ((juxt filter remove) taken? skipped)
        [face taken] ((juxt filter remove) #(mis-tagged? st %) taken)
        [face2 skipped] ((juxt filter remove) #(mis-tagged? st %) skipped)
        face (concat face face2)]
    (-> st
        (assoc-in [:micro :prepared] {:epoch (get-in st [:micro :epoch])
                                      :n (count batch) :delta delta})
        (note "    micro leader (epoch " (get-in st [:micro :epoch]) ") prepares a batch"
              (when (seq batch) (str ": " (str/join ", " (map :name batch))))
              (when (seq skipped) (str "; already decided, skipped: " (str/join ", " (map :name skipped))))
              (when (seq taken) (str "; taken by other content, refused by digest: "
                                     (str/join ", " (map :name taken))))
              (when (seq face) (str "; made for another layer or class, refused on its face: "
                                    (str/join ", " (map :name face))))))))

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
      :v (cond-> {:token tok} (= :mention (:k spec)) (assoc :persons (:mention spec)))
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

(defn- cite
  "The permission an offerer names for writing into `layer`: its own-layer
  one, or, when it asks for :session and has a session, the one there."
  [who layer where]
  [who layer (if (and (= :session where) (= :alice who) (not= :alice-hand layer)) :session :own)])

(defn- op-offer [st {:keys [who layer facts stood-on times] :as spec} & [reused]]
  (let [[nm st] (if reused [reused st] (fresh st "o"))
        [fs st] (reduce (fn [[fs st] spec]
                          (let [[f st] (resolve-fact st layer spec)] [(conj fs f) st]))
                        [[] st] facts)
        pool (sort-by (juxt :tick :id) (all-facts st))
        offer {:name nm :who who :layer layer
               :class (get-in st [:settings layer :class])
               :permission (cite who layer (:cite spec))
               :stood-on (if (and stood-on (seq pool))
                           [(:id (nth pool (mod stood-on (count pool))))]
                           [])
               :facts fs}]
    (-> st
        (note (name who) (if reused " reuses the name " " offers ") nm
              (when reused " for other content")
              " (act layer " (name layer) ", class "
              (name (:class offer)) "): " (str/join "; " (map describe-fact fs)))
        (send-offer offer true)
        (send-again offer times))))

(defn- op-reuse
  "Change E: an offerer sends other content under a name already used."
  [st i spec]
  (let [names (:client-sent st)]
    (if (empty? names)
      st
      (op-offer st spec (nth (rseq names) (mod i (count names)))))))

(defn- op-retry [st i]
  (let [names (:client-sent st)]
    (if (empty? names)
      st
      (let [nm (nth (rseq names) (mod i (count names)))]
        (-> st
            (note "the offerer of " nm " sends it again under the same name")
            (deliver-offer (get-in st [:sent nm])))))))

(defn- op-promote [st i target times & [where]]
  (let [cands (reverse (sort-by (juxt :tick :id)
                                (filter #(and (= :alice (:layer %)) (:lock %) (readable? st %))
                                        (all-facts st))))]
    (if (empty? cands)
      st
      (let [src (nth cands (mod i (count cands)))
            [nm st] (fresh st "o")
            ;; under :carried the person makes the landing's name too, a second random one
            [fwd st] (if (= :carried (get-in st [:config :forward-name])) (fresh st "o") [nil st])
            head (chain-head st target (:e src) (:k src))]
        (-> st
            (note "alice asks to promote " (fid-str (:id src)) " (" (get-in src [:v :token])
                  ") into " (name target) " as " nm)
            (as-> st (let [req {:name nm :who :alice :layer :alice
                                :class (get-in st [:settings :alice :class])
                                :permission (cite :alice :alice where) :stood-on [(:id src)]
                                :promote {:source (:id src) :target target :replaces (:id head)
                                          :fwd-name fwd
                                          :landing-permission (cite :alice target where)}
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
                         :permission [who (:layer t) :own] :stood-on [(:id t)]
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
                     :permission [who layer :own] :stood-on []
                     :facts [{:layer layer :e layer :k k :v v :mark #{}
                              :at [(gate-of st layer cls) (get-in layers [layer :home])]}]}
                    true))))

(defn- op-revoke
  "Change D: the operator revokes a permission, by a fact in the layer the
  permission's fact lives in, standing on it. `a` is a permission, or an
  index into those still standing."
  [st a]
  (let [cands (vec (for [pid permissions
                         :let [f (permission-fact st pid)]
                         :when (and f (not (revocation st pid)))]
                     pid))]
    (if (empty? cands)
      st
      (let [pid (if (vector? a) a (cands (mod a (count cands))))
            pf (permission-fact st pid)
            l (permission-layer pid)
            [nm st] (fresh st "o")]
        (if-not pf
          st
          (-> st
              (note "operator revokes " (pr-str pid) " (in " (name l) ") as " nm)
              (send-offer {:name nm :who :operator :layer l :class (get-in st [:settings l :class])
                           :stood-on [(:id pf)]
                           :facts [{:layer l :e (:e pf) :k :revoke :v {:permission pid} :mark #{}}]}
                          true)))))))

;; ------------------------------------------------------------------ reads

(defn promotion-status
  "The person's view of a promotion as of T, with no optimism: done only once
  the landing is in the shared layer; before that, crossed once the crossing
  fact is at or before T (read out, no longer forgettable), else pending (not
  yet read out, still forgettable); refused if its read-out or landing said
  no."
  [st req T]
  (let [facts (all-facts st)
        landed (filter #(and (:source %) (= req (:because-of %)) (<= (:stamp %) T)) facts)
        crossed (filter #(and (= (crossing-id req) (:id %)) (<= (:stamp %) T)) facts)
        refused (for [s [:stream :micro]
                      p (get-in st [s :parts])
                      [_ a] (:answers p)
                      :when (and (= req (:because-of a)) (= :no (:answer a)) (<= (:stamp a) T))]
                  a)]
    {:request req
     :status (cond (seq landed) :done (seq refused) :refused (seq crossed) :crossed :else :pending)
     :landing-stamps (mapv :stamp landed)
     :crossing-stamps (mapv :stamp crossed)}))

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
                   (cond-> (select-keys f [:id :act :digest :layer :stamp :tick :order :source :crossing])
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
      :reuse (op-reuse st a b)
      :revoke (op-revoke st a)
      :promote (op-promote st a b c (nth op 4 nil))
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

(defn seed-permissions
  "The first facts: every permission, as a fact in the layer it lives in,
  admitted through the gates before any history."
  [st]
  (let [st (reduce (fn [st [who layer where :as pid]]
                     (let [l (permission-layer pid)]
                       (send-offer st {:name (str "grant:" (name who) ":" (name layer)
                                                  (when (= :session where) "@session"))
                                       :who :operator :layer l
                                       :class (get-in st [:settings l :class]) :stood-on []
                                       :facts [{:layer l :e (perm-entity pid) :k :permission
                                                :v {:id pid} :mark #{}}]}
                                   false)))
                   st permissions)
        st (reduce (fn [st p]
                     (loop [st st, n 0]
                       (if (and (seq (get-in st [:stream :inbox p])) (< n 100))
                         (recur (stream-step st p) (inc n))
                         st)))
                   st (range n-parts))
        st (-> st micro-prepare micro-commit)]
    (assoc st :trace ["[t0] first facts: every permission, in the layer it lives in"])))

(defn run
  "Play a history from the first facts, drain it, and take a last read as of now."
  [config history]
  (let [st (drain (reduce step (seed-permissions (init config)) history))]
    (-> st
        (update :tick inc)
        (as-> st (update st :reads conj (read-as-of st (now st)))))))
