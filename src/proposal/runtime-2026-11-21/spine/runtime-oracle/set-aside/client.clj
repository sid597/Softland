(ns softland.auth.client
  "Non-production test shim for the runtime oracle phase: pure in-memory
  state-machine implementation of `spine/auth/SPEC.md` shared across all
  client handles opened against the same cluster handle, with ordered async
  `watch-root` callbacks and `:driven-by` session support. Never used in
  production or real-auth runs."
  (:require [com.rpl.rama :as rama])
  (:import [java.util Collections WeakHashMap]
           [java.util.concurrent ExecutorService Executors ThreadFactory]))

;; ------------------------------------------------------------------ validation

(def ^:private max-text 256)
(def ^:private max-says-entries 32)
(def ^:private max-says-val 2048)
(def ^:private max-actor-depth 32)

(defn- plain-map? [x]
  (and (map? x) (not (record? x))))

(defn- hex64? [x]
  (boolean (and (string? x) (re-matches #"[0-9a-f]{64}" x))))

(defn- code8? [x]
  (boolean (and (string? x) (= 8 (.length ^String x)))))

(defn- text? [x]
  (boolean (and (string? x) (<= (.length ^String x) max-text))))

(defn- text-or-nil? [x]
  (or (nil? x) (text? x)))

(defn- driven-by? [x]
  (or (nil? x)
      (boolean
       (and (plain-map? x)
            (case (:kind x)
              :person (= #{:kind} (set (keys x)))
              :model  (and (= #{:kind :model} (set (keys x)))
                           (text? (:model x)))
              false)))))

(defn- says? [x]
  (boolean
   (and (plain-map? x)
        (<= (count x) max-says-entries)
        (every? (fn [[k v]]
                  (and (string? k) (<= (.length ^String k) max-text)
                       (string? v) (<= (.length ^String v) max-says-val)))
                x))))

(defn- claim? [c]
  (boolean
   (and (plain-map? c)
        (case (:by c)
          :email (and (= #{:by :email} (set (keys c))) (text? (:email c)))
          :code  (and (= #{:by :hash} (set (keys c))) (hex64? (:hash c)))
          false))))

(defn- account? [a]
  (boolean
   (and (vector? a)
        (= 2 (count a))
        (keyword? (first a))
        (<= (count (if-let [ns (namespace (first a))]
                     (str ns "/" (name (first a)))
                     (name (first a))))
            max-text)
        (text? (second a)))))

;; ---------------------------------------------------------- shared cluster state

(defonce ^:private stores
  (Collections/synchronizedMap (WeakHashMap.)))

(defn- empty-store []
  {:invitations-by-person {}    ;; P -> {:person P :claim C :invited-by B :at t :used? bool}
   :invitations-by-claim  {}    ;; C -> P
   :accounts              {}    ;; [provider subject] -> {:person P :says m :at t}
   :persons               {}    ;; P -> {:own-layer L :making {:uuid U :session M} :own-layer-made? bool}
   :roots                 {}    ;; P -> {:actors-order [A ...] :actors {A {...}} :sign-ins-order [G ...] :sign-ins {G {...}} :sessions {S {...}}}
   :asks                  {}    ;; U -> {:hash H :asks text :ends t1 :state :waiting|:yes|:no :answer m-or-nil}
   :watchers              {}})  ;; P -> {watch-id {:client c :f f :stopped? atom :lock Object}}

(defn- new-notifier []
  (Executors/newSingleThreadExecutor
   (reify ThreadFactory
     (newThread [_ runnable]
       (doto (Thread. runnable "softland-auth-oracle-notify") (.setDaemon true))))))

(defn- store-for [cluster module-name]
  (locking stores
    (let [by-mod (or (.get stores cluster)
                     (let [m (atom {})]
                       (.put stores cluster m)
                       m))]
      (or (get @by-mod module-name)
          (let [s {:state (atom (empty-store))
                   :lock (Object.)
                   :notifier (new-notifier)}]
            (swap! by-mod assoc module-name s)
            s)))))

(deftype OracleClient [store module-name closed? lock])

(defn open
  "Opens an oracle Auth client against a cluster handle. Checks that the
  module is deployed on `cluster` so missing-module handling matches real
  Auth."
  [cluster module-name]
  (try
    (let [deployed (set (rama/deployed-module-names cluster))]
      (if (contains? deployed module-name)
        (->OracleClient (store-for cluster module-name) module-name (atom false) (Object.))
        {:could-not :find-the-module :module module-name}))
    (catch Throwable t
      {:could-not :find-the-module :module module-name :threw (.getName (class t)) :said (.getMessage t)})))

(defn close!
  "Closes an oracle Auth client and drops all watches opened through it.
  Acquires the client's callback lock so no watch callback `f` is running or
  can run after `close!` returns."
  [^OracleClient client]
  (when (instance? OracleClient client)
    (locking (.-lock client)
      (reset! (.-closed? client) true))
    (let [store (.-store client)]
      (locking (:lock store)
        (swap! (:state store)
               (fn [st]
                 (update st :watchers
                         (fn [ws]
                           (into {}
                                 (keep (fn [[root entries]]
                                         (let [kept (into {}
                                                          (keep (fn [[id e]]
                                                                  (if (identical? client (:client e))
                                                                    (do (reset! (:stopped? e) true) nil)
                                                                    [id e])))
                                                          entries)]
                                           (when (seq kept) [root kept]))))
                                 ws))))))))
  nil)

(defn- closed-client? [^OracleClient client]
  (or (not (instance? OracleClient client)) @(.-closed? client)))

;; ---------------------------------------------------------- state transitions

(defn- actor-ended? [root-data actor]
  (let [actors (:actors root-data)]
    (loop [a actor steps (inc (count actors))]
      (let [info (get actors a)]
        (cond
          (nil? info) true
          (:ended? info) true
          (nil? (:made-by info)) false
          (zero? steps) true
          :else (recur (:made-by info) (dec steps)))))))

(defn- sign-in-live? [root-data g at]
  (when-let [s (get-in root-data [:sign-ins g])]
    (and (not (:ended? s))
         (< (long at) (long (:until s)))
         (not (actor-ended? root-data (:actor s))))))

(defn- live-snapshot
  "Produces the `watch-root` snapshot `{:sign-ins {...} :sessions {...}}` for
  a root's current state (reflecting what has not been ended/closed). Per
  SPEC.md, `:driven-by` is omitted from `watch-root` snapshots."
  [root-data]
  (let [live-sign-ins
        (into {}
              (keep (fn [[g s]]
                      (when (and (not (:ended? s))
                                 (not (actor-ended? root-data (:actor s))))
                        [g (select-keys s [:actor :kind :hash :until :previous])])))
              (:sign-ins root-data))
        live-sessions
        (into {}
              (keep (fn [[sid sess]]
                      (when (and (not (:closed? sess))
                                 (contains? live-sign-ins (:sign-in sess)))
                        [sid (select-keys sess [:sign-in :until])])))
              (:sessions root-data))]
    {:sign-ins live-sign-ins :sessions live-sessions}))

(defn- sweep-root
  "Per SPEC.md: before judging any write naming `:root P`, ends every sign-in
  and closes every session under `P` whose `:until <= at`."
  [st root at]
  (if-let [rd (get-in st [:roots root])]
    (let [at (long at)
          expired-sign-ins (into #{}
                                 (keep (fn [[g s]]
                                         (when (and (not (:ended? s)) (<= (long (:until s)) at)) g)))
                                 (:sign-ins rd))
          sign-ins' (reduce (fn [m g] (assoc-in m [g :ended?] true)) (:sign-ins rd) expired-sign-ins)
          sessions' (into {}
                          (map (fn [[sid sess]]
                                 (if (and (not (:closed? sess))
                                          (or (<= (long (:until sess)) at)
                                              (contains? expired-sign-ins (:sign-in sess))
                                              (:ended? (get sign-ins' (:sign-in sess)))))
                                   [sid (assoc sess :closed? true)]
                                   [sid sess])))
                          (:sessions rd))]
      (assoc-in st [:roots root] (assoc rd :sign-ins sign-ins' :sessions sessions')))
    st))

(defn- apply-write!
  "Atomically applies `(f state) -> [new-state result]` under the store lock
  and enqueues strictly ordered async notifications onto the store's
  single-threaded notifier when `root`'s live snapshot changed."
  [^OracleClient client root f]
  (if (closed-client? client)
    {:could-not :closed}
    (let [store (.-store client)]
      (locking (:lock store)
        (if (closed-client? client)
          {:could-not :closed}
          (let [before @(:state store)
                [after result] (f before)]
            (reset! (:state store) after)
            (when root
              (let [before-snap (live-snapshot (get-in before [:roots root]))
                    after-snap  (live-snapshot (get-in after [:roots root]))]
                (when (not= before-snap after-snap)
                  (let [entries (vec (vals (get-in after [:watchers root])))
                        ^ExecutorService notifier (:notifier store)]
                    (when (seq entries)
                      (.execute notifier
                                (fn []
                                  (doseq [{:keys [^OracleClient client f stopped?]} entries]
                                    (locking (.-lock client)
                                      (when-not (or @(.-closed? client) @stopped?)
                                        (try (f after-snap) (catch Throwable _ nil))))))))))))
            result))))))

;; --------------------------------------------------------------------- writes

(defn invite [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [person claim invited-by at]} m]
      (if-not (and (uuid? person) (claim? claim) (or (uuid? invited-by) (= :system invited-by)) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client nil
         (fn [st]
           (let [existing-by-p (get-in st [:invitations-by-person person])
                 existing-by-c (get-in st [:invitations-by-claim claim])]
             (cond
               (and existing-by-p (= claim (:claim existing-by-p)))
               [st {:invited person}]

               (and existing-by-c (not= person existing-by-c))
               [st {:refused :claim-in-use}]

               existing-by-p
               [st {:refused :person-in-use}]

               :else
               [(-> st
                    (assoc-in [:invitations-by-person person]
                              {:person person :claim claim :invited-by invited-by :at (long at) :used? false})
                    (assoc-in [:invitations-by-claim claim] person))
                {:invited person}]))))))))

(defn enter [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [account says email code-hash own-layer making at]} m]
      (if-not (and (account? account)
                   (says? says)
                   (text-or-nil? email)
                   (or (nil? code-hash) (hex64? code-hash))
                   (uuid? own-layer)
                   (plain-map? making) (uuid? (:uuid making)) (uuid? (:session making))
                   (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client nil
         (fn [st]
           (if-let [link (get-in st [:accounts account])]
             (let [p (:person link)
                   pinfo (get-in st [:persons p])]
               [(assoc-in st [:accounts account] {:person p :says says :at (long at)})
                {:person p
                 :own-layer (:own-layer pinfo)
                 :making (:making pinfo)
                 :own-layer-made? (:own-layer-made? pinfo)}])
             (let [code-claim (when code-hash {:by :code :hash code-hash})
                   email-claim (when email {:by :email :email email})
                   find-waiting (fn [c]
                                  (when c
                                    (when-let [p (get-in st [:invitations-by-claim c])]
                                      (let [inv (get-in st [:invitations-by-person p])]
                                        (when (and inv (not (:used? inv)))
                                          [p c])))))
                   found (or (find-waiting code-claim) (find-waiting email-claim))]
               (if-not found
                 [st {:refused :not-invited}]
                 (let [[p _c] found
                       st' (-> st
                               (assoc-in [:invitations-by-person p :used?] true)
                               (assoc-in [:accounts account] {:person p :says says :at (long at)})
                               (assoc-in [:persons p] {:own-layer own-layer :making making :own-layer-made? false})
                               (assoc-in [:roots p] {:actors-order []
                                                     :actors {p {:actor p :made-by nil :label nil :ended? false :depth 0}}
                                                     :sign-ins-order []
                                                     :sign-ins {}
                                                     :sessions {}}))]
                   [st' {:person p
                         :own-layer own-layer
                         :making making
                         :own-layer-made? false}]))))))))))

(defn own-layer-made [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [person at]} m]
      (if-not (and (uuid? person) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client nil
         (fn [st]
           (if (contains? (:persons st) person)
             [(assoc-in st [:persons person :own-layer-made?] true) {:made person}]
             [st {:refused :no-such-person}])))))))

(defn begin-sign-in [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root actor sign-in kind hash until label at]} m]
      (if-not (and (uuid? root) (uuid? actor) (uuid? sign-in)
                   (contains? #{:browser :program} kind) (hex64? hash)
                   (int? until) (text-or-nil? label) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])
                 existing (get-in rd [:sign-ins sign-in])
                 actor-info (get-in rd [:actors actor])]
             (cond
               (and existing
                    (= actor (:actor existing))
                    (= kind (:kind existing))
                    (= hash (:initial-hash existing)))
               [st {:begun sign-in}]

               (nil? actor-info)
               [st {:refused :no-such-actor}]

               (actor-ended? rd actor)
               [st {:refused :ended}]

               (some? existing)
               [st {:refused :in-use}]

               :else
               (let [ended? (<= (long until) (long at))
                     entry {:sign-in sign-in :actor actor :kind kind
                            :hash hash :initial-hash hash :until (long until)
                            :previous nil :label label :begun (long at) :ended? ended?}]
                 [(-> st
                      (assoc-in [:roots root :sign-ins sign-in] entry)
                      (update-in [:roots root :sign-ins-order] (fnil conj []) sign-in))
                  {:begun sign-in}])))))))))

(defn replace-secret [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root sign-in old-hash new-hash until previous-until at]} m]
      (if-not (and (uuid? root) (uuid? sign-in) (hex64? old-hash) (hex64? new-hash)
                   (int? until) (int? previous-until) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])
                 s (get-in rd [:sign-ins sign-in])]
             (cond
               (and s (= new-hash (:hash s)) (= old-hash (get-in s [:previous :hash])))
               [st {:replaced sign-in}]

               (not (sign-in-live? rd sign-in at))
               [st {:refused :not-live}]

               (= old-hash (:hash s))
               (let [ended? (<= (long until) (long at))
                     s' (assoc s
                               :hash new-hash
                               :until (long until)
                               :previous {:hash old-hash :until (long previous-until)}
                               :ended? ended?)
                     st' (assoc-in st [:roots root :sign-ins sign-in] s')
                     st' (if ended? (sweep-root st' root at) st')]
                 [st' {:replaced sign-in}])

               :else
               [st {:refused :wrong-secret}]))))))))

(defn end-sign-in [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root sign-in at]} m]
      (if-not (and (uuid? root) (uuid? sign-in) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])]
             (if-not (contains? (:sign-ins rd) sign-in)
               [st {:refused :no-such-sign-in}]
               (let [st' (assoc-in st [:roots root :sign-ins sign-in :ended?] true)
                     sessions' (into {}
                                     (map (fn [[sid sess]]
                                            (if (= sign-in (:sign-in sess))
                                              [sid (assoc sess :closed? true)]
                                              [sid sess])))
                                     (get-in st' [:roots root :sessions]))]
                 [(assoc-in st' [:roots root :sessions] sessions') {:ended sign-in}])))))))))

(defn make-actor [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root actor made-by label at]} m]
      (if-not (and (uuid? root) (uuid? actor) (uuid? made-by) (text? label) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])
                 existing (get-in rd [:actors actor])
                 maker (get-in rd [:actors made-by])]
             (cond
               (and existing (= made-by (:made-by existing)))
               [st {:made actor}]

               (nil? maker)
               [st {:refused :no-such-maker}]

               (actor-ended? rd made-by)
               [st {:refused :ended}]

               (some? existing)
               [st {:refused :in-use}]

               (>= (long (:depth maker)) max-actor-depth)
               [st {:refused :too-deep}]

               :else
               [(-> st
                    (assoc-in [:roots root :actors actor]
                              {:actor actor :made-by made-by :label label
                               :ended? false :depth (inc (long (:depth maker)))})
                    (update-in [:roots root :actors-order] (fnil conj []) actor))
                {:made actor}]))))))))

(defn- descendants-of [actors target]
  (let [children (group-by :made-by (vals actors))]
    (loop [queue [target] seen #{}]
      (if-let [a (first queue)]
        (if (contains? seen a)
          (recur (subvec queue 1) seen)
          (let [kids (map :actor (get children a))]
            (recur (into (subvec queue 1) kids) (conj seen a))))
        seen))))

(defn end-actor [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root actor at]} m]
      (if-not (and (uuid? root) (uuid? actor) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])]
             (cond
               (not (contains? (:actors rd) actor))
               [st {:refused :no-such-actor}]

               (= root actor)
               [st {:refused :is-a-person}]

               :else
               (let [to-end (descendants-of (:actors rd) actor)
                     actors' (reduce (fn [m a] (assoc-in m [a :ended?] true)) (:actors rd) to-end)
                     ended-sign-ins (into #{}
                                          (keep (fn [[g s]] (when (contains? to-end (:actor s)) g)))
                                          (:sign-ins rd))
                     sign-ins' (reduce (fn [m g] (assoc-in m [g :ended?] true)) (:sign-ins rd) ended-sign-ins)
                     sessions' (into {}
                                     (map (fn [[sid sess]]
                                            (if (contains? ended-sign-ins (:sign-in sess))
                                              [sid (assoc sess :closed? true)]
                                              [sid sess])))
                                     (:sessions rd))]
                 [(assoc-in st [:roots root] (assoc rd :actors actors' :sign-ins sign-ins' :sessions sessions'))
                  {:ended actor}])))))))))

(defn open-session [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root sign-in session until at driven-by]} m]
      (if-not (and (uuid? root) (uuid? sign-in) (uuid? session)
                   (int? until) (int? at) (driven-by? driven-by))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])
                 live? (sign-in-live? rd sign-in at)
                 existing (get-in rd [:sessions session])]
             (cond
               (and existing (= sign-in (:sign-in existing)))
               ;; Per SPEC.md: a repeat returns {:open S}; if still open under a live
               ;; sign-in it moves :until forward without changing :driven-by; if closed
               ;; or expired it leaves state unchanged (never reopens).
               (if (and live? (not (:closed? existing)))
                 [(assoc-in st [:roots root :sessions session :until]
                            (max (long (:until existing)) (long until)))
                  {:open session}]
                 [st {:open session}])

               (not live?)
               [st {:refused :not-live}]

               (some? existing)
               [st {:refused :in-use}]

               :else
               (let [closed? (<= (long until) (long at))]
                 [(assoc-in st [:roots root :sessions session]
                            {:sign-in sign-in :until (long until) :closed? closed?
                             :driven-by driven-by})
                  {:open session}])))))))))

(defn close-session [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root session at]} m]
      (if-not (and (uuid? root) (uuid? session) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 rd (get-in st [:roots root])]
             (if-not (contains? (:sessions rd) session)
               [st {:refused :no-such-session}]
               [(assoc-in st [:roots root :sessions session :closed?] true) {:closed session}]))))))))

(defn ask [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [code hash asks ends at]} m]
      (if-not (and (code8? code) (hex64? hash) (text? asks) (int? ends) (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client nil
         (fn [st]
           (let [existing (get-in st [:asks code])]
             (cond
               (and existing (= hash (:hash existing)))
               [st {:asked code}]

               (some? existing)
               [st {:refused :in-use}]

               :else
               [(assoc-in st [:asks code]
                          {:hash hash :asks asks :ends (long ends) :state :waiting :answer nil})
                {:asked code}]))))))))

(defn answer-ask [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [code yes? root actor new-agent sign-in until at]} m]
      (if-not (and (code8? code)
                   (boolean? yes?)
                   (uuid? root)
                   (uuid? actor)
                   (or (nil? new-agent) (and (plain-map? new-agent) (text? (:label new-agent))))
                   (uuid? sign-in)
                   (int? until)
                   (int? at))
        {:could-not :read-the-event}
        (apply-write!
         client root
         (fn [st]
           (let [st (sweep-root st root at)
                 a (get-in st [:asks code])
                 rd (get-in st [:roots root])]
             (cond
               (nil? a)
               [st {:refused :no-such-ask}]

               (= :yes (:state a))
               [st {:yes (:answer a)}]

               (= :no (:state a))
               [st {:no code}]

               (<= (long (:ends a)) (long at))
               [st {:refused :over}]

               (not yes?)
               (if (nil? rd)
                 [st {:refused :no-such-actor}]
                 [(assoc-in st [:asks code :state] :no) {:no code}])

               (nil? rd)
               [st {:refused :no-such-actor}]

               new-agent
               (cond
                 (or (contains? (:actors rd) actor) (contains? (:sign-ins rd) sign-in))
                 [st {:refused :in-use}]

                 :else
                 (let [ended? (<= (long until) (long at))
                       ans {:root root :actor actor :sign-in sign-in}
                       entry {:sign-in sign-in :actor actor :kind :program
                              :hash (:hash a) :initial-hash (:hash a) :until (long until)
                              :previous nil :label (:asks a) :begun (long at) :ended? ended?}]
                   [(-> st
                        (assoc-in [:roots root :actors actor]
                                  {:actor actor :made-by root :label (:label new-agent) :ended? false :depth 1})
                        (update-in [:roots root :actors-order] (fnil conj []) actor)
                        (assoc-in [:roots root :sign-ins sign-in] entry)
                        (update-in [:roots root :sign-ins-order] (fnil conj []) sign-in)
                        (assoc-in [:asks code :state] :yes)
                        (assoc-in [:asks code :answer] ans))
                    {:yes ans}]))

               :else
               (let [actor-info (get-in rd [:actors actor])]
                 (cond
                   (nil? actor-info) [st {:refused :no-such-actor}]
                   (actor-ended? rd actor) [st {:refused :ended}]
                   (contains? (:sign-ins rd) sign-in) [st {:refused :in-use}]
                   :else
                   (let [ended? (<= (long until) (long at))
                         ans {:root root :actor actor :sign-in sign-in}
                         entry {:sign-in sign-in :actor actor :kind :program
                                :hash (:hash a) :initial-hash (:hash a) :until (long until)
                                :previous nil :label (:asks a) :begun (long at) :ended? ended?}]
                     [(-> st
                          (assoc-in [:roots root :sign-ins sign-in] entry)
                          (update-in [:roots root :sign-ins-order] (fnil conj []) sign-in)
                          (assoc-in [:asks code :state] :yes)
                          (assoc-in [:asks code :answer] ans))
                      {:yes ans}])))))))))))

;; ---------------------------------------------------------------------- reads

(defn who-is [^OracleClient client m]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (plain-map? m)) {:could-not :read-the-event}
    :else
    (let [{:keys [root sign-in session]} m]
      (if-not (and (uuid? root) (uuid? sign-in) (or (nil? session) (uuid? session)))
        {:could-not :read-the-event}
        (let [st @(:state (.-store client))
              rd (get-in st [:roots root])
              pinfo (get-in st [:persons root])
              s (get-in rd [:sign-ins sign-in])
              sess (when session (get-in rd [:sessions session]))]
          {:sign-in (when s
                      {:actor (:actor s)
                       :kind (:kind s)
                       :hash (:hash s)
                       :until (:until s)
                       :previous (:previous s)
                       :ended? (boolean (:ended? s))
                       :actor-ended? (boolean (actor-ended? rd (:actor s)))})
           :session (when sess
                      {:sign-in (:sign-in sess)
                       :until (:until sess)
                       :closed? (boolean (:closed? sess))
                       :driven-by (:driven-by sess)})
           :own-layer (:own-layer pinfo)
           :own-layer-made? (boolean (:own-layer-made? pinfo))})))))

(defn ask-of [^OracleClient client code]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (code8? code)) {:could-not :read-the-event}
    :else
    (get-in @(:state (.-store client)) [:asks code])))

(defn under [^OracleClient client root]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (uuid? root)) {:could-not :read-the-event}
    :else
    (let [st @(:state (.-store client))
          rd (get-in st [:roots root])]
      {:actors (mapv (fn [a]
                       (let [info (get-in rd [:actors a])]
                         {:actor (:actor info)
                          :made-by (:made-by info)
                          :label (:label info)
                          :ended? (boolean (actor-ended? rd a))}))
                     (:actors-order rd))
       :sign-ins (vec
                  (keep (fn [g]
                          (let [s (get-in rd [:sign-ins g])]
                            (when (and s (not (:ended? s)) (not (actor-ended? rd (:actor s))))
                              {:sign-in (:sign-in s)
                               :actor (:actor s)
                               :kind (:kind s)
                               :label (:label s)
                               :begun (:begun s)
                               :until (:until s)})))
                        (:sign-ins-order rd)))})))

(defn watch-root [^OracleClient client root f]
  (cond
    (closed-client? client) {:could-not :closed}
    (not (and (uuid? root) (fn? f))) {:could-not :read-the-event}
    :else
    (let [store (.-store client)
          id (Object.)
          stopped? (atom false)]
      (locking (:lock store)
        (if (closed-client? client)
          {:could-not :closed}
          (do (swap! (:state store)
                     assoc-in [:watchers root id] {:client client :f f :stopped? stopped?})
              (fn []
                (locking (.-lock client)
                  (reset! stopped? true))
                (locking (:lock store)
                  (swap! (:state store)
                         (fn [st]
                           (let [entries (dissoc (get-in st [:watchers root]) id)]
                             (if (seq entries)
                               (assoc-in st [:watchers root] entries)
                               (update st :watchers dissoc root))))))
                nil)))))))
