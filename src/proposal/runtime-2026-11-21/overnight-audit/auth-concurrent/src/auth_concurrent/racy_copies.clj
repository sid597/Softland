(ns auth-concurrent.racy-copies
  "Six purposefully racing implementations of the 18 calls of `softland.auth.client`.
  Every copy delegates its pure sequential logic to `softland.auth.reference`, so
  every copy behaves identically to the reference when called sequentially one
  call at a time, and fails ONLY when 2 to 8 threads race concurrently:

    1. `:racy-invite-split-locks` — checks `:claims` and `:persons` without a
       joint lock, yielding in between (`invite` race: two persons bind one
       claim, one person binds two claims, or wrong refusal order).
    2. `:racy-enter-check-then-act` — checks `:links` and `:claims` before
       acquiring the state lock (`enter` race: two accounts claim one waiting
       person, or one account claims two persons, or stale `:says` wins).
    3. `:racy-answer-ask-double-take` — checks `asks[U] == :waiting` outside the
       lock before committing on `:roots[P]` and `:asks[U]` (`answer-ask` race:
       two concurrent answers both execute, overwriting standing answer or
       leaking extra sign-ins/agents).
    4. `:racy-replace-vs-end-sign-in` — reads sign-in `g` outside the lock in
       `replace-secret`, yields while `end-sign-in` ends `G` and closes its
       sessions, then writes the rotated `:ended? false` sign-in record back
       (`replace-secret` vs `end-sign-in` race: revives ended sign-in).
    5. `:racy-end-actor-non-atomic-subtree` — computes live subtree/sign-ins of
       `A` first, yields while `make-actor` or `open-session` adds a child agent
       or open session under `A`, then marks only the pre-snapshot items ended
       (`end-actor` vs `open-session` & `make-actor` race: live child/session
       under ended actor).
    6. `:racy-unlocked-read-modify-write` — executes writes via unlocked `@atom`
       -> `ref/step` -> `reset!` without `locking` or `swap!` (lost updates
       across any concurrent writes)."
  (:require [softland.auth.reference :as ref]))

(deftype RacyCluster [mode modules])
(deftype RacyClient [place id])

(defn- pause-for-race! []
  ;; Tiny yield/park window (~150 microseconds) so concurrent threads interleave
  ;; reliably inside the split check-then-act window without slowing runs.
  (java.util.concurrent.locks.LockSupport/parkNanos 150000))

(defn make-cluster [mode & names]
  (RacyCluster.
   mode
   (into {}
         (map (fn [n]
                [n (atom {:mode mode
                          :state ref/empty-state
                          :watches {}
                          :closed #{}
                          :next-id 1})]))
         (filter string? names))))

(defn- client? [x] (instance? RacyClient x))
(defn- closed? [w ^RacyClient c] (contains? (:closed w) (.-id c)))

(defn- fire-watches! [place before after]
  (let [w @place]
    (doseq [[_ {:keys [root f]}] (sort-by key (:watches w))]
      (let [v-after (ref/live-under after root)
            v-before (ref/live-under before root)]
        (when (not= v-after v-before)
          (try (f v-after) (catch Throwable _ nil)))))))

(defn- commit-locked! [place step-fn]
  (let [[before after result]
        (locking place
          (let [w @place
                before (:state w)
                [after result] (step-fn before)]
            (swap! place assoc :state after)
            [before after result]))]
    (fire-watches! place before after)
    result))

(defn- racy-write! [client kind input]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [place (.-place ^RacyClient client)
          w0 @place]
      (if (closed? w0 client)
        {:could-not :closed}
        (let [mode (:mode w0)]
          (case [mode kind]
            ;; 1. Racy invite: check claim & person on snapshot, pause, then write blindly if snapshot passed
            [:racy-invite-split-locks :invite]
            (let [snap (:state @place)
                  [_ res0] (ref/step snap [:invite input])]
              (pause-for-race!)
              (if (= res0 {:invited (:person input)})
                (commit-locked!
                 place
                 (fn [st]
                   [(-> st
                        (assoc-in [:claims (:claim input)] (:person input))
                        (update-in [:persons (:person input)] assoc
                                   :claim (:claim input)
                                   :invited-by (:invited-by input)
                                   :invited-at (:at input)))
                    {:invited (:person input)}]))
                res0))

            ;; 2. Racy enter: evaluate enter on pre-lock snapshot, pause, then merge snapshot's person/link
            [:racy-enter-check-then-act :enter]
            (let [snap (:state @place)
                  [snap' res0] (ref/step snap [:enter input])]
              (pause-for-race!)
              (if (contains? res0 :person)
                (let [P (:person res0)
                      acc (:account input)]
                  (commit-locked!
                   place
                   (fn [st]
                     [(-> st
                          (assoc-in [:links acc] (get-in snap' [:links acc]))
                          (assoc-in [:persons P] (get-in snap' [:persons P]))
                          (update-in [:roots P] #(or % (get-in snap' [:roots P]))))
                      res0])))
                res0))

            ;; 3. Racy answer-ask: evaluate answer on pre-lock snapshot while waiting, pause, then write ask + root
            [:racy-answer-ask-double-take :answer-ask]
            (let [snap (:state @place)
                  code (:code input)
                  ask-before (get-in snap [:asks code])]
              (if (= :waiting (:state ask-before))
                (let [[snap' res0] (ref/step snap [:answer-ask input])]
                  (pause-for-race!)
                  (commit-locked!
                   place
                   (fn [st]
                     (let [P (:root input)]
                       [(-> st
                            (assoc-in [:asks code] (get-in snap' [:asks code]))
                            (assoc-in [:roots P] (get-in snap' [:roots P])))
                        res0]))))
                (commit-locked! place #(ref/step % [:answer-ask input]))))

            ;; 4. Racy replace-secret vs end-sign-in: check liveness & compute rotated sign-in outside lock, pause, write g'
            [:racy-replace-vs-end-sign-in :replace-secret]
            (let [snap (:state @place)
                  [snap' res0] (ref/step snap [:replace-secret input])]
              (pause-for-race!)
              (if (= res0 {:replaced (:sign-in input)})
                (let [P (:root input)
                      G (:sign-in input)
                      g' (get-in snap' [:roots P :sign-ins G])]
                  (commit-locked!
                   place
                   (fn [st]
                     [(assoc-in st [:roots P :sign-ins G] g')
                      res0])))
                res0))

            ;; 5. Racy end-actor: compute ended actors & sign-ins on snapshot, pause while make-actor/open-session runs,
            ;;    then mark ONLY the snapshot's actors/sign-ins/sessions ended (missing newly created child/session!)
            [:racy-end-actor-non-atomic-subtree :end-actor]
            (let [snap (:state @place)
                  [snap' res0] (ref/step snap [:end-actor input])]
              (pause-for-race!)
              (if (= res0 {:ended (:actor input)})
                (let [P (:root input)
                      ended-agents (keep (fn [[aid a]] (when (:ended? a) aid))
                                         (get-in snap' [:roots P :agents]))
                      ended-gs (keep (fn [[gid g]] (when (:ended? g) gid))
                                     (get-in snap' [:roots P :sign-ins]))
                      closed-ss (keep (fn [[sid s]] (when (:closed? s) sid))
                                      (get-in snap' [:roots P :sessions]))]
                  (commit-locked!
                   place
                   (fn [st]
                     [(update-in st [:roots P]
                                 (fn [r]
                                   (-> r
                                       (update :agents #(reduce (fn [m k] (if (contains? m k) (assoc-in m [k :ended?] true) m)) % ended-agents))
                                       (update :sign-ins #(reduce (fn [m k] (if (contains? m k) (assoc-in m [k :ended?] true) m)) % ended-gs))
                                       (update :sessions #(reduce (fn [m k] (if (contains? m k) (assoc-in m [k :closed?] true) m)) % closed-ss)))))
                      res0])))
                res0))

            ;; 6. Unlocked read-modify-write on the atom
            (if (= mode :racy-unlocked-read-modify-write)
              (let [before (:state @place)
                    [after result] (ref/step before [kind input])]
                (pause-for-race!)
                (swap! place assoc :state after)
                (fire-watches! place before after)
                result)
              ;; Default locked path for all other operations in copies 1..5
              (commit-locked! place #(ref/step % [kind input])))))))))

(defn open [cluster module-name]
  (if-let [place (and (instance? RacyCluster cluster)
                      (string? module-name)
                      (get (.-modules ^RacyCluster cluster) module-name))]
    (locking place
      (let [id (:next-id @place)]
        (swap! place update :next-id inc)
        (RacyClient. place id)))
    {:could-not :find-the-module}))

(defn close! [client]
  (when (client? client)
    (let [place (.-place ^RacyClient client)
          id (.-id ^RacyClient client)]
      (locking place
        (swap! place (fn [w]
                       (-> w
                           (update :closed conj id)
                           (update :watches #(into {} (remove (fn [[_ v]] (= id (:client v)))) %))))))))
  nil)

(defn- read-with [client f]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [w @(.-place ^RacyClient client)]
      (if (closed? w client)
        {:could-not :closed}
        (f (:state w))))))

(defn watch-root [client P f]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [place (.-place ^RacyClient client)]
      (locking place
        (let [w @place]
          (cond
            (closed? w client) {:could-not :closed}
            (not (and (uuid? P) (fn? f))) {:could-not :read-the-event}
            :else (let [wid (:next-id w)]
                    (swap! place #(-> %
                                      (update :next-id inc)
                                      (assoc-in [:watches wid] {:root P :f f :client (.-id ^RacyClient client)})))
                    (fn stop []
                      (locking place (swap! place update :watches dissoc wid))
                      nil))))))))

(def racy-api
  {:open           open
   :close!         close!
   :invite         #(racy-write! %1 :invite %2)
   :enter          #(racy-write! %1 :enter %2)
   :own-lane-made #(racy-write! %1 :own-lane-made %2)
   :begin-sign-in  #(racy-write! %1 :begin-sign-in %2)
   :replace-secret #(racy-write! %1 :replace-secret %2)
   :end-sign-in    #(racy-write! %1 :end-sign-in %2)
   :make-actor     #(racy-write! %1 :make-actor %2)
   :end-actor      #(racy-write! %1 :end-actor %2)
   :open-session   #(racy-write! %1 :open-session %2)
   :close-session  #(racy-write! %1 :close-session %2)
   :ask            #(racy-write! %1 :ask %2)
   :answer-ask     #(racy-write! %1 :answer-ask %2)
   :who-is         (fn [c q] (read-with c #(ref/who-is % q)))
   :ask-of         (fn [c u] (read-with c #(ref/ask-of % u)))
   :under          (fn [c p] (read-with c #(ref/under % p)))
   :watch-root     watch-root})

(def racy-copy-modes
  [[:racy-invite-split-locks
    "Check-then-act race on invite across claims and persons without joint lock"]
   [:racy-enter-check-then-act
    "Check-then-act race on enter across account links and claims"]
   [:racy-answer-ask-double-take
    "Check-then-act race on answer-ask allowing duplicate/overwriting answers"]
   [:racy-replace-vs-end-sign-in
    "Split-step race between replace-secret and end-sign-in reviving ended sign-in"]
   [:racy-end-actor-non-atomic-subtree
    "Non-atomic end-actor subtree snapshot racing make-actor and open-session"]
   [:racy-unlocked-read-modify-write
    "Unlocked read-modify-write atom updates losing concurrent writes"]])
