(ns auth-blind.properties
  "Blind property suite for Auth's contract (`spine/auth/SPEC.md` + tonight's
  `:driven-by` addition on `open-session`).

  Every property takes `auth` as a map of the 18 contract functions:
    `open`, `close!`, `invite`, `enter`, `own-layer-made`, `begin-sign-in`,
    `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`, `open-session`,
    `close-session`, `ask`, `answer-ask`, `who-is`, `ask-of`, `under`,
    `watch-root`
  plus `new-world!`, a 0-arg function returning
    `{:cluster <handle> :module-name <string> :cleanup! <0-arg fn>}`.
  Each property is named with the spec's own sentence."
  (:require [auth-blind.generators :as g]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def required-calls
  [:open :close! :invite :enter :own-layer-made :begin-sign-in :replace-secret
   :end-sign-in :make-actor :end-actor :open-session :close-session :ask
   :answer-ask :who-is :ask-of :under :watch-root])

(defn- pick-nth [coll n]
  (when (seq coll)
    (nth coll (mod n (count coll)))))

(defn- with-clients [auth new-world! f]
  (let [{:keys [cluster module-name cleanup!]} (new-world!)]
    (try
      (let [c1 ((:open auth) cluster module-name)
            c2 ((:open auth) cluster module-name)]
        (try
          (f {:cluster cluster :module-name module-name :c1 c1 :c2 c2})
          (finally
            (try ((:close! auth) c1) (catch Throwable _ nil))
            (try ((:close! auth) c2) (catch Throwable _ nil)))))
      (finally
        (when cleanup! (cleanup!))))))

(defn- could-not-map? [x]
  (and (map? x) (keyword? (:could-not x))))

;; ---------------------------------------------------------------------------
;; Full 18-call history runner (drives multiple properties over rich histories)
;; ---------------------------------------------------------------------------

(defn play-history
  "Executes a generated symbolic history of all 18 Auth calls across two opened
  clients `c1` and `c2`, tracking everything ever created and recording invariant
  checks along the way."
  [auth {:keys [cluster module-name c1 c2]} trial-id steps]
  (let [slot (atom 0)
        next-slot! #(swap! slot inc)
        clients [c1 c2]
        watch-events (atom {}) ; root -> [snapshot ...]
        stop-fns (atom [])]
    (try
      (loop [todo steps
             now 1000000
             st {:invites []         ; [{:person P :claim C}]
                 :links []           ; [{:account acc :person P :own-layer L :making M}]
                 :roots []           ; [P ...]
                 :actors {}          ; P -> [A ...] (includes P)
                 :ended-actors #{}   ; #{[P A] ...}
                 :sign-ins []        ; [{:root P :actor A :sign-in G :kind K :hash H}]
                 :ended-sign-ins #{} ; #{[P G] ...}
                 :sessions []        ; [{:root P :sign-in G :session S :driven-by D}]
                 :closed-sessions #{}; #{[P S] ...}
                 :asks []            ; [{:code U :hash H :asks text :ends t1}]
                 :answered-asks #{}  ; #{U ...}
                 :taken-writes []    ; [{:op kw :arg map :res map}]
                 :errors []}]
        (let [record-err (fn [st msg data]
                           (update st :errors conj {:msg msg :data data :at now}))
              ;; Monotonicity & non-deletion check on current state:
              check-monotonic!
              (fn [st]
                (let [st (reduce (fn [st [p a]]
                                   (let [u ((:under auth) c1 p)
                                         found (first (filter #(= a (:actor %)) (:actors u)))]
                                     (if (and (not= p a) (or (nil? found) (not (:ended? found))))
                                       (record-err st "ended agent missing or became live again in under"
                                                   {:root p :actor a :under u})
                                       st)))
                                 st (:ended-actors st))
                      st (reduce (fn [st [p g]]
                                   (let [w ((:who-is auth) c1 {:root p :sign-in g :session nil})
                                         u ((:under auth) c1 p)]
                                     (cond
                                       (nil? (:sign-in w))
                                       (record-err st "ended sign-in deleted from who-is" {:root p :sign-in g})

                                       (not (get-in w [:sign-in :ended?]))
                                       (record-err st "ended sign-in became live again" {:root p :sign-in g :who-is w})

                                       (some #(= g (:sign-in %)) (:sign-ins u))
                                       (record-err st "ended sign-in still listed as live in under" {:root p :sign-in g})

                                       :else st)))
                                 st (:ended-sign-ins st))
                      st (reduce (fn [st [p s]]
                                   (let [w ((:who-is auth) c1 {:root p :sign-in (g/trial-uuid trial-id 999999) :session s})]
                                     (cond
                                       (nil? (:session w))
                                       (record-err st "closed session deleted from who-is" {:root p :session s})

                                       (not (get-in w [:session :closed?]))
                                       (record-err st "closed session became open again" {:root p :session s :who-is w})

                                       :else st)))
                                 st (:closed-sessions st))]
                  st))]
          (if (or (empty? todo) (seq (:errors st)))
            (let [st (check-monotonic! st)]
              (assoc st :watch-events @watch-events))
            (let [step (first todo)
                  now (+ now (long (:dt step 0)))
                  client (nth clients (:client step 0))]
              (case (:op step)
                :advance-time
                (recur (rest todo) (+ now (long (:jump step))) st)

                :open-and-close-temp-client
                (let [tc ((:open auth) cluster module-name)
                      _ ((:close! auth) tc)
                      after-close ((:under auth) tc (g/trial-uuid trial-id 1))
                      st (if (= :closed (:could-not after-close))
                           st
                           (record-err st "closed temp client did not return :closed" {:got after-close}))]
                  (recur (rest todo) now st))

                :watch-root
                (if-let [p (pick-nth (:roots st) (:r-pick step))]
                  (let [initial-calls (atom 0)
                        reg-done? (atom false)
                        stop! ((:watch-root auth) c1 p
                               (fn [snap]
                                 (when-not @reg-done? (swap! initial-calls inc))
                                 (swap! watch-events update p (fnil conj []) snap)))
                        _ (reset! reg-done? true)
                        st (if (pos? @initial-calls)
                             (record-err st "watch-root called f during registration" {:calls @initial-calls})
                             st)]
                    (if (and (:stop? step) (fn? stop!))
                      (stop!)
                      (when (fn? stop!) (swap! stop-fns conj stop!)))
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :invite
                (let [k (next-slot!)
                      p (g/trial-uuid trial-id k)
                      claim (if (:by-code? step)
                              {:by :code :hash (g/trial-hash trial-id k)}
                              {:by :email :email (g/trial-email trial-id k)})
                      arg {:person p :claim claim :invited-by :system :at now}
                      res ((:invite auth) client arg)
                      st (if (= {:invited p} res)
                           (-> st
                               (update :invites conj {:person p :claim claim})
                               (update :taken-writes conj {:op :invite :arg arg :res res}))
                           (record-err st "fresh invite not taken" {:arg arg :res res}))]
                  (recur (rest todo) now st))

                :invite-conflict
                (if-let [{:keys [person claim]} (pick-nth (:invites st) (:pick step))]
                  (let [k (next-slot!)
                        [arg expected]
                        (case (:kind step)
                          :exact-repeat [{:person person :claim claim :invited-by (g/trial-uuid trial-id k) :at now}
                                         {:invited person}]
                          :same-claim   [{:person (g/trial-uuid trial-id k) :claim claim :invited-by :system :at now}
                                         {:refused :claim-in-use}]
                          :same-person  [{:person person :claim {:by :email :email (g/trial-email trial-id k)}
                                          :invited-by :system :at now}
                                         {:refused :person-in-use}])
                        res ((:invite auth) c1 arg)
                        st (if (= expected res)
                             st
                             (record-err st "invite conflict/repeat mismatch"
                                         {:kind (:kind step) :expected expected :got res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :enter
                (let [k (next-slot!)]
                  (if (and (:use-linked? step) (seq (:links st)))
                    (let [{:keys [account person own-layer making]} (pick-nth (:links st) (:pick step))
                          arg {:account account :says (:says step) :email nil :code-hash nil
                               :own-layer (g/trial-uuid trial-id k)
                               :making {:uuid (g/trial-uuid trial-id (next-slot!))
                                        :session (g/trial-uuid trial-id (next-slot!))}
                               :at now}
                          res ((:enter auth) c1 arg)
                          st (if (and (= person (:person res))
                                      (= own-layer (:own-layer res))
                                      (= making (:making res)))
                               st
                               (record-err st "re-enter changed person/own-layer/making"
                                           {:expected [person own-layer making] :got res}))]
                      (recur (rest todo) now st))
                    (if-let [{:keys [claim]} (pick-nth (:invites st) (:pick step))]
                      (let [acc [(:provider step) (g/trial-subject trial-id k)]
                            own-l (g/trial-uuid trial-id (next-slot!))
                            mk {:uuid (g/trial-uuid trial-id (next-slot!))
                                :session (g/trial-uuid trial-id (next-slot!))}
                            arg {:account acc
                                 :says (:says step)
                                 :email (when (= :email (:by claim)) (:email claim))
                                 :code-hash (when (= :code (:by claim)) (:hash claim))
                                 :own-layer own-l
                                 :making mk
                                 :at now}
                            res ((:enter auth) c1 arg)
                            st (if (:person res)
                                 (let [p (:person res)]
                                   (-> st
                                       (update :links conj {:account acc :person p :own-layer (:own-layer res) :making (:making res)})
                                       (cond-> (not (some #{p} (:roots st)))
                                         (-> (update :roots conj p)
                                             (assoc-in [:actors p] [p])))))
                                 st)]
                        (recur (rest todo) now st))
                      (recur (rest todo) now st))))

                :own-layer-made
                (if-let [p (pick-nth (:roots st) (:pick step))]
                  (let [arg {:person p :at now}
                        res ((:own-layer-made auth) c1 arg)
                        w ((:who-is auth) c1 {:root p :sign-in (g/trial-uuid trial-id 999998) :session nil})
                        st (if (and (= {:made p} res) (:own-layer-made? w))
                             (update st :taken-writes conj {:op :own-layer-made :arg arg :res res})
                             (record-err st "own-layer-made failed on entered person" {:res res :who-is w}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :begin-sign-in
                (if-let [p (pick-nth (:roots st) (:r-pick step))]
                  (let [a (pick-nth (get-in st [:actors p]) (:a-pick step))
                        k (next-slot!)
                        gid (g/trial-uuid trial-id k)
                        h (g/trial-hash trial-id k)
                        until (+ now (long (:span step)))
                        arg {:root p :actor a :sign-in gid :kind (:kind step)
                             :hash h :until until :label (:label step) :at now}
                        res ((:begin-sign-in auth) client arg)
                        st (if (= {:begun gid} res)
                             (-> st
                                 (update :sign-ins conj {:root p :actor a :sign-in gid :kind (:kind step) :hash h})
                                 (update :taken-writes conj {:op :begin-sign-in :arg arg :res res})
                                 (cond-> (<= until now) (update :ended-sign-ins conj [p gid])))
                             st)]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :replace-secret
                (if-let [{:keys [root sign-in]} (pick-nth (:sign-ins st) (:g-pick step))]
                  (let [w ((:who-is auth) c1 {:root root :sign-in sign-in :session nil})
                        cur-h (get-in w [:sign-in :hash])
                        k (next-slot!)
                        new-h (g/trial-hash trial-id k)
                        old-h (if (:wrong? step) (g/trial-hash trial-id (next-slot!)) cur-h)
                        arg {:root root :sign-in sign-in :old-hash old-h :new-hash new-h
                             :until (+ now (long (:span step)))
                             :previous-until (+ now (long (:prev-span step)))
                             :at now}
                        res ((:replace-secret auth) c1 arg)
                        st (if (= {:replaced sign-in} res)
                             (update st :taken-writes conj {:op :replace-secret :arg arg :res res})
                             st)]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :end-sign-in
                (if-let [{:keys [root sign-in]} (pick-nth (:sign-ins st) (:g-pick step))]
                  (let [arg {:root root :sign-in sign-in :at now}
                        res ((:end-sign-in auth) c1 arg)
                        sessions-under (keep #(when (and (= root (:root %)) (= sign-in (:sign-in %)))
                                                [root (:session %)])
                                             (:sessions st))
                        st (if (= {:ended sign-in} res)
                             (-> st
                                 (update :ended-sign-ins conj [root sign-in])
                                 (update :closed-sessions into sessions-under)
                                 (update :taken-writes conj {:op :end-sign-in :arg arg :res res}))
                             (record-err st "end-sign-in did not end known sign-in" {:arg arg :res res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :make-actor
                (if-let [p (pick-nth (:roots st) (:r-pick step))]
                  (let [b (pick-nth (get-in st [:actors p]) (:b-pick step))
                        a (g/trial-uuid trial-id (next-slot!))
                        arg {:root p :actor a :made-by b :label (:label step) :at now}
                        res ((:make-actor auth) c1 arg)
                        st (if (= {:made a} res)
                             (-> st
                                 (update-in [:actors p] conj a)
                                 (update :taken-writes conj {:op :make-actor :arg arg :res res}))
                             st)]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :end-actor
                (if-let [p (pick-nth (:roots st) (:r-pick step))]
                  (let [a (pick-nth (get-in st [:actors p]) (:a-pick step))
                        arg {:root p :actor a :at now}
                        res ((:end-actor auth) c1 arg)
                        st (cond
                             (= p a)
                             (if (= {:refused :is-a-person} res)
                               st
                               (record-err st "end-actor on person did not refuse :is-a-person" {:res res}))

                             (= {:ended a} res)
                             (-> st
                                 (update :ended-actors conj [p a])
                                 (update :taken-writes conj {:op :end-actor :arg arg :res res}))

                             :else
                             (record-err st "end-actor failed on known agent" {:arg arg :res res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :open-session
                (if-let [{:keys [root sign-in]} (pick-nth (:sign-ins st) (:g-pick step))]
                  (let [s (g/trial-uuid trial-id (next-slot!))
                        until (+ now (long (:span step)))
                        db (:driven-by step)
                        arg (cond-> {:root root :sign-in sign-in :session s :until until :at now}
                              (not= :absent db) (assoc :driven-by db))
                        expected-db (when (not= :absent db) db)
                        res ((:open-session auth) c1 arg)
                        st (if (= {:open s} res)
                             (let [w ((:who-is auth) c1 {:root root :sign-in sign-in :session s})]
                               (if (not= expected-db (get-in w [:session :driven-by]))
                                 (record-err st "who-is :driven-by mismatch after open-session"
                                             {:expected expected-db :got (get-in w [:session :driven-by])})
                                 (-> st
                                     (update :sessions conj {:root root :sign-in sign-in :session s :driven-by expected-db})
                                     (cond-> (<= until now) (update :closed-sessions conj [root s])))))
                             st)]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :refresh-session
                (if-let [{:keys [root sign-in session driven-by]} (pick-nth (:sessions st) (:s-pick step))]
                  (let [db2 (:driven-by step)
                        arg (cond-> {:root root :sign-in sign-in :session session
                                     :until (+ now (long (:span step))) :at now}
                              (not= :absent db2) (assoc :driven-by db2))
                        _ ((:open-session auth) c2 arg)
                        w ((:who-is auth) c1 {:root root :sign-in sign-in :session session})
                        st (if (not= driven-by (get-in w [:session :driven-by]))
                             (record-err st "repeat open-session mutated :driven-by"
                                         {:expected driven-by :got (get-in w [:session :driven-by])})
                             st)]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :close-session
                (if-let [{:keys [root session]} (pick-nth (:sessions st) (:s-pick step))]
                  (let [arg {:root root :session session :at now}
                        res ((:close-session auth) c1 arg)
                        st (if (= {:closed session} res)
                             (-> st
                                 (update :closed-sessions conj [root session])
                                 (update :taken-writes conj {:op :close-session :arg arg :res res}))
                             (record-err st "close-session failed on known session" {:arg arg :res res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :ask
                (let [k (next-slot!)
                      u (g/trial-code trial-id k)
                      h (g/trial-hash trial-id k)
                      ends (+ now (long (:span step)))
                      arg {:code u :hash h :asks (:asks step) :ends ends :at now}
                      res ((:ask auth) c1 arg)
                      st (if (= {:asked u} res)
                           (-> st
                               (update :asks conj {:code u :hash h :asks (:asks step) :ends ends})
                               (update :taken-writes conj {:op :ask :arg arg :res res}))
                           (record-err st "fresh ask failed" {:arg arg :res res}))]
                  (recur (rest todo) now st))

                :answer-ask
                (if-let [{:keys [code]} (pick-nth (:asks st) (:u-pick step))]
                  (if-let [p (pick-nth (:roots st) (:r-pick step))]
                    (let [k (next-slot!)
                          gid (g/trial-uuid trial-id k)
                          until (+ now (long (:span step)))
                          [yes? actor new-agent]
                          (case (:mode step)
                            :no [false p nil]
                            :yes-self [true p nil]
                            :yes-existing [true (pick-nth (get-in st [:actors p]) k) nil]
                            :yes-new-agent [true (g/trial-uuid trial-id (next-slot!)) {:label (:label step)}])
                          arg {:code code :yes? yes? :root p :actor actor :new-agent new-agent
                               :sign-in gid :until until :at now}
                          res ((:answer-ask auth) c1 arg)
                          st (cond
                               (:yes res)
                               (let [{r-out :root a-out :actor g-out :sign-in} (:yes res)]
                                 (-> st
                                     (update :answered-asks conj code)
                                     (cond-> (and (= r-out p) (= g-out gid))
                                       (-> (update :sign-ins conj {:root p :actor a-out :sign-in gid :kind :program :hash nil})
                                           (cond-> new-agent (update-in [:actors p] conj a-out))))))
                               (:no res)
                               (update st :answered-asks conj code)
                               :else st)]
                      (recur (rest todo) now st))
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :repeat-write
                (if-let [{:keys [op arg res]} (pick-nth (:taken-writes st) (:pick step))]
                  (let [c (if (:other-client? step) c2 c1)
                        arg' (if-not (:vary-extra? step)
                               (assoc arg :at now)
                               (case op
                                 :invite (assoc arg :invited-by (g/trial-uuid trial-id (next-slot!)) :at now)
                                 :begin-sign-in (assoc arg :label "varied-on-repeat" :until (+ now 9999) :at now)
                                 :make-actor (assoc arg :label "varied-on-repeat" :at now)
                                 :ask (assoc arg :asks "varied-on-repeat" :ends (+ now 9999) :at now)
                                 (assoc arg :at now)))
                        res2 ((get auth op) c arg')
                        st (if (= res res2)
                             st
                             (record-err st "repeat of taken write did not return first result"
                                         {:op op :first res :repeat res2 :arg arg'}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :who-is
                (if-let [{:keys [root sign-in]} (pick-nth (:sign-ins st) (:g-pick step))]
                  (let [sess (pick-nth (filter #(= root (:root %)) (:sessions st)) (:s-pick step))
                        res ((:who-is auth) c1 {:root root :sign-in sign-in :session (:session sess)})
                        st (if (and (map? res) (some? (:sign-in res)))
                             st
                             (record-err st "who-is missing known sign-in" {:res res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :ask-of
                (if-let [{:keys [code hash asks ends]} (pick-nth (:asks st) (:u-pick step))]
                  (let [res ((:ask-of auth) c1 code)
                        st (if (and (= hash (:hash res)) (= asks (:asks res)) (= ends (:ends res)))
                             st
                             (record-err st "ask-of mismatch" {:expected [hash asks ends] :got res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st))

                :under
                (if-let [p (pick-nth (:roots st) (:r-pick step))]
                  (let [res ((:under auth) c1 p)
                        st (if (and (vector? (:actors res)) (vector? (:sign-ins res)))
                             st
                             (record-err st "under did not return vectors" {:got res}))]
                    (recur (rest todo) now st))
                  (recur (rest todo) now st)))))))
      (finally
        (doseq [stop! @stop-fns]
          (try (stop!) (catch Throwable _ nil)))))))

;; ---------------------------------------------------------------------------
;; Helper to bootstrap an entered person P in focused properties
;; ---------------------------------------------------------------------------

(defn- setup-person! [auth c trial-id base-slot at]
  (let [p (g/trial-uuid trial-id base-slot)
        own-l (g/trial-uuid trial-id (+ base-slot 1))
        mk {:uuid (g/trial-uuid trial-id (+ base-slot 2))
            :session (g/trial-uuid trial-id (+ base-slot 3))}
        email (g/trial-email trial-id base-slot)]
    ((:invite auth) c {:person p :claim {:by :email :email email} :invited-by :system :at at})
    ((:enter auth) c {:account [:google (g/trial-subject trial-id base-slot)]
                      :says {"name" "Ada"}
                      :email email :code-hash nil
                      :own-layer own-l :making mk :at at})
    {:person p :own-layer own-l :making mk}))

;; ---------------------------------------------------------------------------
;; Properties — one for each thing the spec says must hold, named with the
;; spec's own sentence
;; ---------------------------------------------------------------------------

(def properties
  [{:id :no-call-throws
    :sentence "No call throws for what a caller gave or for what Rama did. A call that could not be made gives {:could-not <word> ...}, a map that holds :could-not."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [bad-mod (gen/elements ["missing-auth" "" 123 nil])]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [cluster c1]}]
              (let [bad-open ((:open auth) cluster bad-mod)
                    bad-calls (for [[op bad-arg] (g/invalid-inputs-for-trial tid)]
                                ((get auth op) c1 bad-arg))
                    bad-watch ((:watch-root auth) c1 "not-a-uuid" (fn [_]))]
                (and (could-not-map? bad-open)
                     (= :find-the-module (:could-not bad-open))
                     (every? could-not-map? bad-calls)
                     (could-not-map? bad-watch))))))))}

   {:id :read-the-event-writes-nothing
    :sentence "A write or a read whose input does not fit gives {:could-not :read-the-event}, and a write then writes nothing."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    gid (g/trial-uuid tid 10)
                    h (g/trial-hash tid 10)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in gid
                                                 :kind :browser :hash h :until (+ at 50)
                                                 :label "ok" :at at})
                    ;; Even with :at far past sign-in's :until, an invalid write must write NOTHING (not even sweep!)
                    bad-results (mapv (fn [[op arg]] ((get auth op) c1 arg))
                                      (g/invalid-inputs-for-trial tid))
                    bad-root-write ((:end-sign-in auth) c1 {:root person :sign-in "not-a-uuid" :at (+ at 5000)})
                    still-live ((:who-is auth) c1 {:root person :sign-in gid :session nil})]
                (and (every? #(= {:could-not :read-the-event} (select-keys % [:could-not])) bad-results)
                     (= :read-the-event (:could-not bad-root-write))
                     (= false (get-in still-live [:sign-in :ended?])))))))))}

   {:id :closed-client-contract
    :sentence "watch-root on a closed client gives {:could-not :closed} in place of the function, and close! calls no f again."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c2 tid 1 at)
                    called-after-close (atom 0)
                    closed-flag (atom false)
                    _ ((:watch-root auth) c1 person
                       (fn [_] (when @closed-flag (swap! called-after-close inc))))
                    _ ((:close! auth) c1)
                    _ (reset! closed-flag true)
                    ;; Trigger live state change via open client c2:
                    gid (g/trial-uuid tid 20)
                    _ ((:begin-sign-in auth) c2 {:root person :actor person :sign-in gid
                                                 :kind :browser :hash (g/trial-hash tid 20)
                                                 :until (+ at 1000) :label nil :at at})
                    w-on-closed ((:watch-root auth) c1 person (fn [_]))
                    r-on-closed ((:under auth) c1 person)
                    wr-on-closed ((:end-sign-in auth) c1 {:root person :sign-in gid :at (+ at 1)})]
                (Thread/sleep 10)
                (and (zero? @called-after-close)
                     (= :closed (:could-not w-on-closed))
                     (= :closed (:could-not r-on-closed))
                     (= :closed (:could-not wr-on-closed)))))))))}

   {:id :write-arrives-twice
    :sentence "A write that arrives twice, from one copy of the runtime or from two, leaves the same state as once. The second call gives the result of the first, which says that the write was taken; it does not say how the thing stands now, which a read says."
    :trials 50
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [steps g/gen-history]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [ctx]
              (empty? (:errors (play-history auth ctx tid steps))))))))}

   {:id :refusal-binds-nothing
    :sentence "A refusal binds nothing: the same write after the state changed can be taken."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [p (g/trial-uuid tid 1)
                    email (g/trial-email tid 1)
                    enter-arg {:account [:google (g/trial-subject tid 1)]
                               :says {} :email email :code-hash nil
                               :own-layer (g/trial-uuid tid 2)
                               :making {:uuid (g/trial-uuid tid 3) :session (g/trial-uuid tid 4)}
                               :at at}
                    olm-arg {:person p :at at}
                    gid (g/trial-uuid tid 5)
                    bsi-arg {:root p :actor p :sign-in gid :kind :browser
                             :hash (g/trial-hash tid 5) :until (+ at 500) :label nil :at at}
                    sid (g/trial-uuid tid 6)
                    os-arg {:root p :sign-in gid :session sid :until (+ at 500) :at at
                            :driven-by {:kind :person}}
                    ;; 1. All refuse before prerequisites exist:
                    r-enter-1 ((:enter auth) c1 enter-arg)
                    r-olm-1   ((:own-layer-made auth) c1 olm-arg)
                    r-bsi-1   ((:begin-sign-in auth) c1 bsi-arg)
                    r-os-1    ((:open-session auth) c1 os-arg)
                    ;; 2. Satisfy prerequisites one by one and retry exact same writes:
                    _         ((:invite auth) c1 {:person p :claim {:by :email :email email} :invited-by :system :at at})
                    r-olm-wait ((:own-layer-made auth) c1 olm-arg)
                    r-enter-2 ((:enter auth) c1 enter-arg)
                    r-olm-2   ((:own-layer-made auth) c1 olm-arg)
                    r-bsi-2   ((:begin-sign-in auth) c1 bsi-arg)
                    r-os-2    ((:open-session auth) c1 os-arg)]
                (and (= {:refused :not-invited} r-enter-1)
                     (= {:refused :no-such-person} r-olm-1)
                     (= {:refused :no-such-person} r-olm-wait)
                     (= {:refused :no-such-actor} r-bsi-1)
                     (= {:refused :not-live} r-os-1)
                     (= p (:person r-enter-2))
                     (= {:made p} r-olm-2)
                     (= {:begun gid} r-bsi-2)
                     (= {:open sid} r-os-2))))))))}

   {:id :invite-claim-and-person-binding
    :sentence "A claim names one person for good: once an account has proven it, it is used up, and it never names another person. A person has one claim: P under another claim is refused. The same P under the same C again changes nothing, also after she has entered."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [by-code? gen/boolean
                     at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [p1 (g/trial-uuid tid 1)
                    p2 (g/trial-uuid tid 2)
                    c1-claim (if by-code?
                               {:by :code :hash (g/trial-hash tid 1)}
                               {:by :email :email (g/trial-email tid 1)})
                    c2-claim {:by :email :email (g/trial-email tid 2)}
                    inv1 ((:invite auth) c1 {:person p1 :claim c1-claim :invited-by :system :at at})
                    same-claim ((:invite auth) c2 {:person p2 :claim c1-claim :invited-by :system :at at})
                    other-claim ((:invite auth) c2 {:person p1 :claim c2-claim :invited-by :system :at at})
                    ent1 ((:enter auth) c1 {:account [:google (g/trial-subject tid 1)]
                                            :says {}
                                            :email (when-not by-code? (:email c1-claim))
                                            :code-hash (when by-code? (:hash c1-claim))
                                            :own-layer (g/trial-uuid tid 3)
                                            :making {:uuid (g/trial-uuid tid 4) :session (g/trial-uuid tid 5)}
                                            :at at})
                    repeat-after-enter ((:invite auth) c2 {:person p1 :claim c1-claim :invited-by p1 :at (+ at 10)})
                    reuse-claim-after-enter ((:invite auth) c2 {:person p2 :claim c1-claim :invited-by :system :at (+ at 10)})
                    second-acc-same-claim ((:enter auth) c2 {:account [:google (g/trial-subject tid 2)]
                                                             :says {}
                                                             :email (when-not by-code? (:email c1-claim))
                                                             :code-hash (when by-code? (:hash c1-claim))
                                                             :own-layer (g/trial-uuid tid 6)
                                                             :making {:uuid (g/trial-uuid tid 7) :session (g/trial-uuid tid 8)}
                                                             :at (+ at 10)})]
                (and (= {:invited p1} inv1)
                     (= {:refused :claim-in-use} same-claim)
                     (= {:refused :person-in-use} other-claim)
                     (= p1 (:person ent1))
                     (= {:invited p1} repeat-after-enter)
                     (= {:refused :claim-in-use} reuse-claim-after-enter)
                     (= {:refused :not-invited} second-acc-same-claim))))))))}

   {:id :linked-account-ignores-claim
    :sentence "An account that has a link gives its person, whatever claim came with it."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [p1 (g/trial-uuid tid 1)
                    p2 (g/trial-uuid tid 2)
                    e1 (g/trial-email tid 1)
                    e2 (g/trial-email tid 2)
                    acc1 [:google (g/trial-subject tid 1)]
                    acc2 [:google (g/trial-subject tid 2)]
                    _ ((:invite auth) c1 {:person p1 :claim {:by :email :email e1} :invited-by :system :at at})
                    _ ((:invite auth) c1 {:person p2 :claim {:by :email :email e2} :invited-by :system :at at})
                    _ ((:enter auth) c1 {:account acc1 :says {} :email e1 :code-hash nil
                                         :own-layer (g/trial-uuid tid 3)
                                         :making {:uuid (g/trial-uuid tid 4) :session (g/trial-uuid tid 5)}
                                         :at at})
                    ;; acc1 re-enters carrying p2's waiting email e2; must return p1 AND leave e2 waiting for acc2!
                    re-ent ((:enter auth) c1 {:account acc1 :says {"name" "P1"} :email e2 :code-hash nil
                                              :own-layer (g/trial-uuid tid 6)
                                              :making {:uuid (g/trial-uuid tid 7) :session (g/trial-uuid tid 8)}
                                              :at (+ at 5)})
                    ent-p2 ((:enter auth) c1 {:account acc2 :says {} :email e2 :code-hash nil
                                              :own-layer (g/trial-uuid tid 9)
                                              :making {:uuid (g/trial-uuid tid 10) :session (g/trial-uuid tid 11)}
                                              :at (+ at 6)})]
                (and (= p1 (:person re-ent))
                     (= p2 (:person ent-p2)))))))))}

   {:id :enter-claim-precedence
    :sentence "An account that has none takes the person of the claim that it proves: the code's, when :code-hash names an invitation that waits; else the email's, when :email names one."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [p-code (g/trial-uuid tid 1)
                    p-mail (g/trial-uuid tid 2)
                    ch (g/trial-hash tid 1)
                    em (g/trial-email tid 2)
                    _ ((:invite auth) c1 {:person p-code :claim {:by :code :hash ch} :invited-by :system :at at})
                    _ ((:invite auth) c1 {:person p-mail :claim {:by :email :email em} :invited-by :system :at at})
                    ;; Account 1 presents BOTH waiting code-hash and waiting email -> takes p-code!
                    ent1 ((:enter auth) c1 {:account [:google (g/trial-subject tid 1)]
                                            :says {} :email em :code-hash ch
                                            :own-layer (g/trial-uuid tid 3)
                                            :making {:uuid (g/trial-uuid tid 4) :session (g/trial-uuid tid 5)}
                                            :at at})
                    ;; Account 2 presents the now-used code-hash AND the still-waiting email -> falls back to p-mail!
                    ent2 ((:enter auth) c1 {:account [:google (g/trial-subject tid 2)]
                                            :says {} :email em :code-hash ch
                                            :own-layer (g/trial-uuid tid 6)
                                            :making {:uuid (g/trial-uuid tid 7) :session (g/trial-uuid tid 8)}
                                            :at (+ at 1)})]
                (and (= p-code (:person ent1))
                     (= p-mail (:person ent2)))))))))}

   {:id :own-layer-and-making-fixed
    :sentence ":own-layer and :making are what the runtime offers for a person who has none yet. The first enter that makes her an actor fixes them; every later enter gives the ones that were fixed, and its own offer is dropped."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [p (g/trial-uuid tid 1)
                    em (g/trial-email tid 1)
                    acc [:github (g/trial-subject tid 1)]
                    l1 (g/trial-uuid tid 2)
                    m1 {:uuid (g/trial-uuid tid 3) :session (g/trial-uuid tid 4)}
                    l2 (g/trial-uuid tid 5)
                    m2 {:uuid (g/trial-uuid tid 6) :session (g/trial-uuid tid 7)}
                    _ ((:invite auth) c1 {:person p :claim {:by :email :email em} :invited-by :system :at at})
                    e1 ((:enter auth) c1 {:account acc :says {"v" "1"} :email em :code-hash nil
                                          :own-layer l1 :making m1 :at at})
                    e2 ((:enter auth) c2 {:account acc :says {"v" "2"} :email nil :code-hash nil
                                          :own-layer l2 :making m2 :at (+ at 1)})
                    _  ((:own-layer-made auth) c1 {:person p :at (+ at 2)})
                    e3 ((:enter auth) c2 {:account acc :says {"v" "3"} :email nil :code-hash nil
                                          :own-layer l2 :making m2 :at (+ at 3)})
                    w  ((:who-is auth) c1 {:root p :sign-in (g/trial-uuid tid 99) :session nil})]
                (and (= {:person p :own-layer l1 :making m1 :own-layer-made? false} e1)
                     (= {:person p :own-layer l1 :making m1 :own-layer-made? false} e2)
                     (= {:person p :own-layer l1 :making m1 :own-layer-made? true} e3)
                     (= l1 (:own-layer w))
                     (true? (:own-layer-made? w)))))))))}

   {:id :concurrent-enters-on-one-claim
    :sentence "Two accounts that prove one claim at the same moment: one becomes the person and the other is refused."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [p (g/trial-uuid tid 1)
                    em (g/trial-email tid 1)
                    _ ((:invite auth) c1 {:person p :claim {:by :email :email em} :invited-by :system :at at})
                    r1 ((:enter auth) c1 {:account [:google (g/trial-subject tid 1)] :says {} :email em :code-hash nil
                                          :own-layer (g/trial-uuid tid 2)
                                          :making {:uuid (g/trial-uuid tid 3) :session (g/trial-uuid tid 4)}
                                          :at at})
                    r2 ((:enter auth) c2 {:account [:google (g/trial-subject tid 2)] :says {} :email em :code-hash nil
                                          :own-layer (g/trial-uuid tid 5)
                                          :making {:uuid (g/trial-uuid tid 6) :session (g/trial-uuid tid 7)}
                                          :at at})
                    results [r1 r2]]
                (and (= 1 (count (filter #(= p (:person %)) results)))
                     (= 1 (count (filter #(= {:refused :not-invited} %) results))))))))))}

   {:id :begin-sign-in-bound-once
    :sentence "From now on the secret whose hash is H stands for the actor A, until t1. A is under the root P and is live. A sign-in id is bound once: the same id with the same parts again changes nothing; with other parts it is refused."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [kind g/gen-kind
                     at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    gid (g/trial-uuid tid 10)
                    h1 (g/trial-hash tid 10)
                    h2 (g/trial-hash tid 11)
                    t1 (+ at 500)
                    b1 ((:begin-sign-in auth) c1 {:root person :actor person :sign-in gid :kind kind
                                                  :hash h1 :until t1 :label "first" :at at})
                    b-repeat ((:begin-sign-in auth) c2 {:root person :actor person :sign-in gid :kind kind
                                                        :hash h1 :until (+ t1 999) :label "second" :at (+ at 1)})
                    b-diff-hash ((:begin-sign-in auth) c2 {:root person :actor person :sign-in gid :kind kind
                                                           :hash h2 :until t1 :label "first" :at (+ at 1)})
                    w ((:who-is auth) c1 {:root person :sign-in gid :session nil})
                    u ((:under auth) c1 person)]
                (and (= {:begun gid} b1)
                     (= {:begun gid} b-repeat)
                     (= {:refused :in-use} b-diff-hash)
                     (= {:actor person :kind kind :hash h1 :until t1 :previous nil :ended? false :actor-ended? false}
                        (:sign-in w))
                     (= [{:sign-in gid :actor person :kind kind :label "first" :begun at :until t1}]
                        (:sign-ins u)))))))))}

   {:id :replace-secret-rules
    :sentence "The sign-in's secret becomes the one whose hash is H', with a new end t1. H is the sign-in's hash now; or it is the same replacement again, which is so when H' is the sign-in's hash now and H is the one before it. A replacement that arrives after a newer one is no repeat, and is refused with :wrong-secret. The old secret keeps standing until t2 and no longer. No session changes."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    gid (g/trial-uuid tid 10)
                    sid (g/trial-uuid tid 11)
                    h0 (g/trial-hash tid 10)
                    h1 (g/trial-hash tid 11)
                    h2 (g/trial-hash tid 12)
                    h3 (g/trial-hash tid 13)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in gid :kind :browser
                                                 :hash h0 :until (+ at 500) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in gid :session sid
                                                :until (+ at 600) :at at :driven-by {:kind :person}})
                    sess-before (:session ((:who-is auth) c1 {:root person :sign-in gid :session sid}))
                    rep1 ((:replace-secret auth) c1 {:root person :sign-in gid :old-hash h0 :new-hash h1
                                                     :until (+ at 1000) :previous-until (+ at 200) :at (+ at 10)})
                    rep1-again ((:replace-secret auth) c2 {:root person :sign-in gid :old-hash h0 :new-hash h1
                                                           :until (+ at 9999) :previous-until (+ at 9999) :at (+ at 11)})
                    w1 ((:who-is auth) c1 {:root person :sign-in gid :session sid})
                    rep2 ((:replace-secret auth) c1 {:root person :sign-in gid :old-hash h1 :new-hash h2
                                                     :until (+ at 1500) :previous-until (+ at 300) :at (+ at 20)})
                    stale-rep ((:replace-secret auth) c2 {:root person :sign-in gid :old-hash h0 :new-hash h1
                                                          :until (+ at 1000) :previous-until (+ at 200) :at (+ at 21)})
                    using-prev ((:replace-secret auth) c2 {:root person :sign-in gid :old-hash h1 :new-hash h3
                                                           :until (+ at 2000) :previous-until (+ at 400) :at (+ at 22)})
                    w2 ((:who-is auth) c1 {:root person :sign-in gid :session sid})]
                (and (= {:replaced gid} rep1)
                     (= {:replaced gid} rep1-again)
                     (= h1 (get-in w1 [:sign-in :hash]))
                     (= (+ at 1000) (get-in w1 [:sign-in :until]))
                     (= {:hash h0 :until (+ at 200)} (get-in w1 [:sign-in :previous]))
                     (= sess-before (:session w1))
                     (= {:replaced gid} rep2)
                     (= {:refused :wrong-secret} stale-rep)
                     (= {:refused :wrong-secret} using-prev)
                     (= h2 (get-in w2 [:sign-in :hash]))
                     (= {:hash h1 :until (+ at 300)} (get-in w2 [:sign-in :previous]))
                     (= sess-before (:session w2)))))))))}

   {:id :end-sign-in-closes-sessions
    :sentence "The sign-in ends, and every session under it is closed. Ending it again changes nothing."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    g1 (g/trial-uuid tid 10)
                    g2 (g/trial-uuid tid 11)
                    s1 (g/trial-uuid tid 12)
                    s2 (g/trial-uuid tid 13)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g1 :kind :browser
                                                 :hash (g/trial-hash tid 10) :until (+ at 1000) :label nil :at at})
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g2 :kind :program
                                                 :hash (g/trial-hash tid 11) :until (+ at 1000) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s1 :until (+ at 1000) :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g2 :session s2 :until (+ at 1000) :at at})
                    e1 ((:end-sign-in auth) c1 {:root person :sign-in g1 :at (+ at 10)})
                    e2 ((:end-sign-in auth) c2 {:root person :sign-in g1 :at (+ at 20)})
                    w1 ((:who-is auth) c1 {:root person :sign-in g1 :session s1})
                    w2 ((:who-is auth) c1 {:root person :sign-in g2 :session s2})
                    u  ((:under auth) c1 person)]
                (and (= {:ended g1} e1)
                     (= {:ended g1} e2)
                     (true? (get-in w1 [:sign-in :ended?]))
                     (false? (get-in w1 [:sign-in :actor-ended?]))
                     (true? (get-in w1 [:session :closed?]))
                     (false? (get-in w2 [:sign-in :ended?]))
                     (false? (get-in w2 [:session :closed?]))
                     (= [g2] (mapv :sign-in (:sign-ins u))))))))))}

   {:id :make-actor-and-depth-bound
    :sentence "Makes the agent A under the root P, made by the live actor B of that root. Who made whom goes 32 deep at most, the store's own bound on how deep a permission can be; no read costs more with the depth."
    :trials 15
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    ;; Build chain A1..A32 below root `person`:
                    chain-ids (mapv #(g/trial-uuid tid (+ 100 %)) (range 1 34))
                    results (loop [idx 0
                                   maker person
                                   acc []]
                              (if (= idx 33)
                                acc
                                (let [aid (nth chain-ids idx)
                                      res ((:make-actor auth) c1 {:root person :actor aid :made-by maker
                                                                  :label (str "agent-" (inc idx)) :at at})]
                                  (recur (inc idx) aid (conj acc res)))))
                    u ((:under auth) c1 person)]
                (and (every? (fn [[aid res]] (= {:made aid} res))
                             (map vector (take 32 chain-ids) (take 32 results)))
                     (= {:refused :too-deep} (nth results 32))
                     (= (vec (take 32 chain-ids)) (mapv :actor (:actors u))))))))))}

   {:id :end-actor-cascades-and-spares-person
    :sentence "The agent ends, with every actor that it made, in any depth, and every sign-in and session of each. A person is not ended by this call."
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    a1 (g/trial-uuid tid 10)
                    a2 (g/trial-uuid tid 11)
                    a3 (g/trial-uuid tid 12)
                    sib (g/trial-uuid tid 13)
                    g-a3 (g/trial-uuid tid 20)
                    s-a3 (g/trial-uuid tid 21)
                    g-sib (g/trial-uuid tid 22)
                    s-sib (g/trial-uuid tid 23)
                    _ ((:make-actor auth) c1 {:root person :actor a1 :made-by person :label "a1" :at at})
                    _ ((:make-actor auth) c1 {:root person :actor a2 :made-by a1 :label "a2" :at at})
                    _ ((:make-actor auth) c1 {:root person :actor a3 :made-by a2 :label "a3" :at at})
                    _ ((:make-actor auth) c1 {:root person :actor sib :made-by person :label "sib" :at at})
                    _ ((:begin-sign-in auth) c1 {:root person :actor a3 :sign-in g-a3 :kind :program
                                                 :hash (g/trial-hash tid 20) :until (+ at 1000) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g-a3 :session s-a3 :until (+ at 1000) :at at})
                    _ ((:begin-sign-in auth) c1 {:root person :actor sib :sign-in g-sib :kind :program
                                                 :hash (g/trial-hash tid 22) :until (+ at 1000) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g-sib :session s-sib :until (+ at 1000) :at at})
                    end-p ((:end-actor auth) c1 {:root person :actor person :at (+ at 5)})
                    end-a1 ((:end-actor auth) c1 {:root person :actor a1 :at (+ at 6)})
                    w-a3 ((:who-is auth) c1 {:root person :sign-in g-a3 :session s-a3})
                    w-sib ((:who-is auth) c1 {:root person :sign-in g-sib :session s-sib})
                    u ((:under auth) c1 person)]
                (and (= {:refused :is-a-person} end-p)
                     (= {:ended a1} end-a1)
                     (true? (get-in w-a3 [:sign-in :ended?]))
                     (true? (get-in w-a3 [:sign-in :actor-ended?]))
                     (true? (get-in w-a3 [:session :closed?]))
                     (false? (get-in w-sib [:sign-in :ended?]))
                     (false? (get-in w-sib [:session :closed?]))
                     (= [a1 a2 a3 sib] (mapv :actor (:actors u)))
                     (= [true true true false] (mapv :ended? (:actors u))))))))))}

   {:id :open-session-driven-by
    :sentence "open-session takes an optional :driven-by, either {:kind :person} or {:kind :model :model <text of at most 256 characters>}; it is kept with the session, given back by who-is inside its :session map, and a repeat does not change it."
    :trials 35
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [db1 g/gen-driven-by
                     db2 g/gen-driven-by
                     at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    gid (g/trial-uuid tid 10)
                    sid (g/trial-uuid tid 11)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in gid :kind :browser
                                                 :hash (g/trial-hash tid 10) :until (+ at 2000) :label nil :at at})
                    watched (atom [])
                    stop! ((:watch-root auth) c1 person #(swap! watched conj %))
                    arg1 (cond-> {:root person :sign-in gid :session sid :until (+ at 500) :at at}
                           (not= :absent db1) (assoc :driven-by db1))
                    arg2 (cond-> {:root person :sign-in gid :session sid :until (+ at 900) :at (+ at 10)}
                           (not= :absent db2) (assoc :driven-by db2))
                    expected-db (when (not= :absent db1) db1)
                    r1 ((:open-session auth) c1 arg1)
                    w1 ((:who-is auth) c1 {:root person :sign-in gid :session sid})
                    r2 ((:open-session auth) c2 arg2)
                    w2 ((:who-is auth) c1 {:root person :sign-in gid :session sid})
                    _  ((:close-session auth) c1 {:root person :session sid :at (+ at 20)})
                    w3 ((:who-is auth) c1 {:root person :sign-in gid :session sid})]
                (when (fn? stop!) (stop!))
                (Thread/sleep 10)
                (and (= {:open sid} r1)
                     (= {:open sid} r2)
                     (= expected-db (get-in w1 [:session :driven-by]))
                     (= expected-db (get-in w2 [:session :driven-by]))
                     (= (+ at 900) (get-in w2 [:session :until]))
                     ;; Still kept after close-session:
                     (true? (get-in w3 [:session :closed?]))
                     (= expected-db (get-in w3 [:session :driven-by]))
                     ;; watch-root never carries :driven-by:
                     (every? (fn [snap]
                               (every? #(not (contains? % :driven-by)) (vals (:sessions snap))))
                             @watched))))))))}

   {:id :session-renewal-and-never-reopened
    :sentence "The same id under the same sign-in again, while the session is open, moves its time to the :until that came, when that is later: that is how the runtime keeps a session that is in use. A session that is closed, or was taken out because its time passed, is not opened again."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    g1 (g/trial-uuid tid 10)
                    s-closed (g/trial-uuid tid 11)
                    s-expired (g/trial-uuid tid 12)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g1 :kind :browser
                                                 :hash (g/trial-hash tid 10) :until (+ at 5000) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-closed :until (+ at 500) :at at})
                    ;; Shorter :until does NOT shrink time; later :until DOES advance time:
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-closed :until (+ at 200) :at (+ at 10)})
                    u-after-shrink (get-in ((:who-is auth) c1 {:root person :sign-in g1 :session s-closed}) [:session :until])
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-closed :until (+ at 800) :at (+ at 20)})
                    u-after-grow (get-in ((:who-is auth) c1 {:root person :sign-in g1 :session s-closed}) [:session :until])
                    ;; Close s-closed, then try to open-session again:
                    _ ((:close-session auth) c1 {:root person :session s-closed :at (+ at 30)})
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-closed :until (+ at 2000) :at (+ at 40)})
                    w-closed ((:who-is auth) c1 {:root person :sign-in g1 :session s-closed})
                    ;; Expire s-expired by advancing :at past its :until, then try open-session:
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-expired :until (+ at 100) :at (+ at 40)})
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s-expired :until (+ at 3000) :at (+ at 100)})
                    w-expired ((:who-is auth) c1 {:root person :sign-in g1 :session s-expired})]
                (and (= (+ at 500) u-after-shrink)
                     (= (+ at 800) u-after-grow)
                     (true? (get-in w-closed [:session :closed?]))
                     (true? (get-in w-expired [:session :closed?])))))))))}

   {:id :ask-and-answer-ask-rules
    :sentence "An answer whose :at is at or after the ask's :ends is refused with :over. The first answer stands: the same answer again gives the same result, and another answer gives what stands."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{p1 :person} (setup-person! auth c1 tid 1 at)
                    {p2 :person} (setup-person! auth c2 tid 10 at)
                    c-over (g/trial-code tid 1)
                    c-yes  (g/trial-code tid 2)
                    c-no   (g/trial-code tid 3)
                    h-yes  (g/trial-hash tid 2)
                    _ ((:ask auth) c1 {:code c-over :hash (g/trial-hash tid 1) :asks "tool-a" :ends (+ at 100) :at at})
                    _ ((:ask auth) c1 {:code c-yes  :hash h-yes                :asks "tool-b" :ends (+ at 500) :at at})
                    _ ((:ask auth) c1 {:code c-no   :hash (g/trial-hash tid 3) :asks "tool-c" :ends (+ at 500) :at at})
                    ;; 1. At exact :ends, refused :over, and ask-of still says :waiting (ask-of judges no time):
                    r-over ((:answer-ask auth) c1 {:code c-over :yes? true :root p1 :actor p1 :new-agent nil
                                                   :sign-in (g/trial-uuid tid 30) :until (+ at 900) :at (+ at 100)})
                    ao-over ((:ask-of auth) c1 c-over)
                    ;; 2. Yes with new-agent: first answer stands when p2 answers differently, and sign-in label = :asks:
                    new-a (g/trial-uuid tid 31)
                    g-yes (g/trial-uuid tid 32)
                    r-yes1 ((:answer-ask auth) c1 {:code c-yes :yes? true :root p1 :actor new-a
                                                   :new-agent {:label "worker"} :sign-in g-yes :until (+ at 900)
                                                   :at (+ at 10)})
                    r-yes-other ((:answer-ask auth) c2 {:code c-yes :yes? false :root p2 :actor p2 :new-agent nil
                                                        :sign-in (g/trial-uuid tid 33) :until (+ at 900) :at (+ at 20)})
                    u-p1 ((:under auth) c1 p1)
                    ;; 3. No needs only an entered person and judges no other part (even if sign-in G is already in-use!):
                    r-no1 ((:answer-ask auth) c1 {:code c-no :yes? false :root p1 :actor (g/trial-uuid tid 99)
                                                  :new-agent nil :sign-in g-yes :until (+ at 900) :at (+ at 15)})
                    r-no-other ((:answer-ask auth) c2 {:code c-no :yes? true :root p2 :actor p2 :new-agent nil
                                                       :sign-in (g/trial-uuid tid 34) :until (+ at 900) :at (+ at 25)})]
                (and (= {:refused :over} r-over)
                     (= :waiting (:state ao-over))
                     (= {:yes {:root p1 :actor new-a :sign-in g-yes}} r-yes1)
                     (= r-yes1 r-yes-other)
                     (= "tool-b" (:label (first (:sign-ins u-p1))))
                     (= {:no c-no} r-no1)
                     (= {:no c-no} r-no-other))))))))}

   {:id :root-writes-sweep-expired-items
    :sentence "Each write that names a :root takes out of what is live under that root every sign-in and every session whose time is at or before the write's :at, before the write itself is judged, and whether the write is then taken, is a repeat, or is refused."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    g-exp (g/trial-uuid tid 10)
                    s-exp (g/trial-uuid tid 11)
                    g-never (g/trial-uuid tid 12)
                    s-never (g/trial-uuid tid 13)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g-exp :kind :browser
                                                 :hash (g/trial-hash tid 10) :until (+ at 100) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g-exp :session s-exp :until (+ at 50) :at at})
                    ;; Non-root writes (own-layer-made, ask) at (+ at 200) take NOTHING out:
                    _ ((:own-layer-made auth) c1 {:person person :at (+ at 200)})
                    _ ((:ask auth) c1 {:code (g/trial-code tid 1) :hash (g/trial-hash tid 1)
                                       :asks "cli" :ends (+ at 500) :at (+ at 200)})
                    before-root-write ((:who-is auth) c1 {:root person :sign-in g-exp :session s-exp})
                    ;; A REFUSED root write at (+ at 50) sweeps s-exp (until == at+50) while leaving g-exp (until == at+100):
                    refused-close ((:close-session auth) c1 {:root person :session (g/trial-uuid tid 99) :at (+ at 50)})
                    mid-read ((:who-is auth) c1 {:root person :sign-in g-exp :session s-exp})
                    ;; A REFUSED root write at (+ at 100) sweeps g-exp (until == at+100):
                    refused-end ((:end-sign-in auth) c1 {:root person :sign-in (g/trial-uuid tid 98) :at (+ at 100)})
                    after-read ((:who-is auth) c1 {:root person :sign-in g-exp :session s-exp})
                    ;; Sign-in and session with until <= at at creation are taken and never live:
                    g-live (g/trial-uuid tid 14)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g-live :kind :browser
                                                 :hash (g/trial-hash tid 14) :until (+ at 500) :label nil :at (+ at 100)})
                    b-never ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g-never :kind :browser
                                                       :hash (g/trial-hash tid 12) :until (+ at 100) :label nil :at (+ at 100)})
                    o-never ((:open-session auth) c1 {:root person :sign-in g-live :session s-never
                                                      :until (+ at 100) :at (+ at 100)})
                    w-never ((:who-is auth) c1 {:root person :sign-in g-never :session s-never})]
                (and (false? (get-in before-root-write [:sign-in :ended?]))
                     (false? (get-in before-root-write [:session :closed?]))
                     (= {:refused :no-such-session} refused-close)
                     (false? (get-in mid-read [:sign-in :ended?]))
                     (true? (get-in mid-read [:session :closed?]))
                     (= {:refused :no-such-sign-in} refused-end)
                     (true? (get-in after-read [:sign-in :ended?]))
                     (= {:begun g-never} b-never)
                     (= {:open s-never} o-never)
                     (true? (get-in w-never [:sign-in :ended?]))
                     (true? (get-in w-never [:session :closed?])))))))))}

   {:id :refusal-precedence-and-session-in-use
    :sentence "Where two refusals apply to one write, a repeat is recognised first, then the refusals in the order in which the contract lists them. A session's id that is another sign-in's, open or not, is :in-use."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [;; 1. invite: :claim-in-use before :person-in-use
                    p1 (g/trial-uuid tid 1)
                    p2 (g/trial-uuid tid 2)
                    c1-claim {:by :email :email (g/trial-email tid 1)}
                    c2-claim {:by :email :email (g/trial-email tid 2)}
                    _ ((:invite auth) c1 {:person p1 :claim c1-claim :invited-by :system :at at})
                    _ ((:invite auth) c1 {:person p2 :claim c2-claim :invited-by :system :at at})
                    inv-both ((:invite auth) c1 {:person p2 :claim c1-claim :invited-by :system :at at})
                    ;; 2. Setup entered root for sign-in / session / actor precedence:
                    {:keys [person]} (setup-person! auth c1 tid 10 at)
                    g1 (g/trial-uuid tid 20)
                    g2 (g/trial-uuid tid 21)
                    s1 (g/trial-uuid tid 22)
                    h1 (g/trial-hash tid 20)
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g1 :kind :browser
                                                 :hash h1 :until (+ at 1000) :label nil :at at})
                    _ ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g2 :kind :browser
                                                 :hash (g/trial-hash tid 21) :until (+ at 1000) :label nil :at at})
                    _ ((:open-session auth) c1 {:root person :sign-in g1 :session s1 :until (+ at 1000) :at at})
                    _ ((:close-session auth) c1 {:root person :session s1 :at (+ at 1)})
                    ;; s1 is CLOSED under g1; opening s1 under live g2 must refuse :in-use:
                    s-other-live ((:open-session auth) c1 {:root person :sign-in g2 :session s1 :until (+ at 1000) :at (+ at 2)})
                    ;; End g2; now opening s1 under ended g2 has BOTH :not-live and :in-use -> :not-live comes first!
                    _ ((:end-sign-in auth) c1 {:root person :sign-in g2 :at (+ at 3)})
                    s-other-dead ((:open-session auth) c1 {:root person :sign-in g2 :session s1 :until (+ at 1000) :at (+ at 4)})
                    ;; Repeat of g1's begin-sign-in after g1 is ended: repeat comes BEFORE any refusal!
                    _ ((:end-sign-in auth) c1 {:root person :sign-in g1 :at (+ at 5)})
                    bsi-repeat-after-end ((:begin-sign-in auth) c1 {:root person :actor person :sign-in g1 :kind :browser
                                                                    :hash h1 :until (+ at 1000) :label nil :at (+ at 6)})
                    ;; begin-sign-in with non-existent actor AND already-used sign-in g1 -> :no-such-actor before :in-use!
                    bsi-two-faults ((:begin-sign-in auth) c1 {:root person :actor (g/trial-uuid tid 99) :sign-in g1
                                                              :kind :browser :hash (g/trial-hash tid 99)
                                                              :until (+ at 1000) :label nil :at (+ at 7)})]
                (and (= {:refused :claim-in-use} inv-both)
                     (= {:refused :in-use} s-other-live)
                     (= {:refused :not-live} s-other-dead)
                     (= {:begun g1} bsi-repeat-after-end)
                     (= {:refused :no-such-actor} bsi-two-faults))))))))}

   {:id :watch-root-tracks-live-set
    :sentence "watch-root calls f with one argument each time what is live under the root P changed, through whichever copy of the runtime the change came"
    :trials 25
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1 c2]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    seen (atom [])
                    stop! ((:watch-root auth) c1 person #(swap! seen conj %))
                    no-call-at-reg? (empty? @seen)
                    g1 (g/trial-uuid tid 10)
                    h1 (g/trial-hash tid 10)
                    s1 (g/trial-uuid tid 11)
                    _ ((:begin-sign-in auth) c2 {:root person :actor person :sign-in g1 :kind :browser
                                                 :hash h1 :until (+ at 500) :label "web" :at at})
                    _ ((:open-session auth) c2 {:root person :sign-in g1 :session s1 :until (+ at 300)
                                                :at (+ at 1) :driven-by {:kind :model :model "opus"}})
                    _ (Thread/sleep 20)
                    snap-live (last @seen)
                    _ ((:end-sign-in auth) c2 {:root person :sign-in g1 :at (+ at 2)})
                    _ (Thread/sleep 20)
                    snap-empty (last @seen)
                    count-before-stop (count @seen)
                    _ (when (fn? stop!) (stop!))
                    _ ((:begin-sign-in auth) c2 {:root person :actor person :sign-in (g/trial-uuid tid 12)
                                                 :kind :browser :hash (g/trial-hash tid 12)
                                                 :until (+ at 900) :label nil :at (+ at 3)})
                    _ (Thread/sleep 20)]
                (and no-call-at-reg?
                     (= {:sign-ins {g1 {:actor person :kind :browser :hash h1 :until (+ at 500) :previous nil}}
                         :sessions {s1 {:sign-in g1 :until (+ at 300)}}}
                        snap-live)
                     (= {:sign-ins {} :sessions {}} snap-empty)
                     (= count-before-stop (count @seen)))))))))}

   {:id :nothing-is-deleted
    :sentence "Nothing is deleted. An ended sign-in, a closed session, an ended actor and an answered ask can still be read: the facts of them stay."
    :trials 30
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [at (gen/choose 1000 5000)]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [{:keys [c1]}]
              (let [{:keys [person]} (setup-person! auth c1 tid 1 at)
                    agent-a (g/trial-uuid tid 10)
                    gid (g/trial-uuid tid 11)
                    sid (g/trial-uuid tid 12)
                    code (g/trial-code tid 1)
                    ask-h (g/trial-hash tid 1)
                    _ ((:make-actor auth) c1 {:root person :actor agent-a :made-by person :label "bot" :at at})
                    _ ((:ask auth) c1 {:code code :hash ask-h :asks "bot-cli" :ends (+ at 500) :at at})
                    _ ((:answer-ask auth) c1 {:code code :yes? true :root person :actor agent-a :new-agent nil
                                              :sign-in gid :until (+ at 1000) :at (+ at 5)})
                    _ ((:open-session auth) c1 {:root person :sign-in gid :session sid :until (+ at 800)
                                                :at (+ at 6) :driven-by {:kind :model :model "m1"}})
                    _ ((:close-session auth) c1 {:root person :session sid :at (+ at 7)})
                    _ ((:end-actor auth) c1 {:root person :actor agent-a :at (+ at 8)})
                    w ((:who-is auth) c1 {:root person :sign-in gid :session sid})
                    u ((:under auth) c1 person)
                    ao ((:ask-of auth) c1 code)]
                (and (= {:actor agent-a :kind :program :hash ask-h :until (+ at 1000)
                         :previous nil :ended? true :actor-ended? true}
                        (:sign-in w))
                     (= {:sign-in gid :until (+ at 800) :closed? true :driven-by {:kind :model :model "m1"}}
                        (:session w))
                     (= [{:actor agent-a :made-by person :label "bot" :ended? true}]
                        (:actors u))
                     (= [] (:sign-ins u))
                     (= {:hash ask-h :asks "bot-cli" :ends (+ at 500) :state :yes
                         :answer {:root person :actor agent-a :sign-in gid}}
                        ao))))))))}

   {:id :an-end-is-never-taken-back
    :sentence "An end is never taken back: no later event makes an ended sign-in, a closed session or an ended actor live again."
    :trials 40
    :make-prop
    (fn [auth new-world!]
      (prop/for-all [steps g/gen-history]
        (let [tid (g/next-trial-id!)]
          (with-clients auth new-world!
            (fn [ctx]
              (empty? (:errors (play-history auth ctx tid steps))))))))}])
