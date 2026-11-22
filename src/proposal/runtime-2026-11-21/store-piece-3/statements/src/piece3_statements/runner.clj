(ns piece3-statements.runner
  "Generic Piece 3 history runner: takes *anything* that gives the store's
  door and the new read by stamp (`target`), plays a generated history against
  both `target` and a reference `model` side by side, and checks every
  statement of `piece3-statements.statements` at every step and read sweep.

  Target interface:
    :act-id-for   (fn [layer] -> [layer depot uuid] | {:could-not ...})
    :send-act!    (fn [maker-act act-id] -> {:act-id :digest :ack} | {:could-not ...})
    :append-raw!  (fn [depot raw-thing] -> any)
    :answer-of    (fn [act-id digest] -> {:answer :accepted|:refused|:nothing-yet|:not-mine ...})
    :read-line    (fn [{:keys [layer start max-count max-bytes asker not-signed-in session]}]
                    -> {:left-out nil|:not-made|:may-not-see
                        :last-stamp long-or-nil
                        :next-stamp long-or-nil
                        :entries [{:stamp long
                                   :status :whole|:withheld|:too-large
                                   :act-id [layer depot uuid]   ; absent when :withheld
                                   :answer {:verdict :reason :stamp :time :digest} ; absent when :withheld
                                   :thing  raw-thing}]}         ; present iff :whole
                    | {:could-not ...})
    :make-act-permission-fact
                  optional `(fn [{:keys [layer holder parent target-uuid]}] -> {:e :k :v})`
                  so Step 5 can supply the updated `:act-permission` form."
  (:require [piece3-statements.generator :as g]
            [piece3-statements.model :as model]
            [piece3-statements.statements :as st]))

(def ^:private wide-bytes (* 64 1024 1024))
(def ^:private tight-bytes (* 64 1024))

(defn- first-holder-of [layer]
  (if (= :base layer) :system :asha))

(defn- depot-of [layer]
  (st/depot-at-making layer))

(defn- other-depot-of [layer]
  (if (= :base layer) :streaming :microbatch))

(defn- ref-layer-state [ref-target layer]
  (let [depot (depot-of layer)]
    (or (get-in @(:state ref-target) [:gates depot layer])
        (model/empty-layer-state layer))))

(defn- current-standing-perm [ls holder]
  (when-let [cur (get-in ls [:perm-places [:holder holder]])]
    (when (st/permission-stands? (:perm-places ls) (:perm cur))
      (:perm cur))))

(defn- send-both!
  "Sends a maker's act (with explicit `act-id`) to both `target` and
  `ref-target`, checks `L5` door-answer agreement, and syncs `:door-time` in
  `ref-target` to what `target` stamped (when `target` is a real door with its
  own clock)."
  [target ref-target maker-act act-id]
  (let [r-target ((:send-act! target) maker-act act-id)
        ans-target (when-not (:could-not r-target)
                     ((:answer-of target) (:act-id r-target) (:digest r-target)))
        ;; Feed `ref-target` with the exact `:door-time` that `target` wrote so
        ;; `:time` and `:thing` match even when `target` reads `System/currentTimeMillis`.
        t-used (or (:time ans-target) 1700000000000)
        raw-for-ref (assoc maker-act
                           :act-id act-id
                           :depot (nth act-id 1)
                           :door-time t-used)
        _ ((:append-raw! ref-target) (nth act-id 1) raw-for-ref)
        ans-ref ((:answer-of ref-target) act-id (st/digest-hex raw-for-ref))]
    {:sent r-target
     :ans-target ans-target
     :ans-ref ans-ref
     :err (when (not= (dissoc ans-ref :time) (dissoc ans-target :time))
            {:statement :L5/door-answer-agreement
             :detail (str "Door answer mismatch for " act-id ": ref=" ans-ref ", got=" ans-target)})}))

(defn- append-raw-both! [target ref-target depot raw-thing]
  ((:append-raw! target) depot raw-thing)
  ((:append-raw! ref-target) depot raw-thing)
  nil)

(defn run-read-sweep
  "Checks `(:read-line target)` across askers, sessions, byte bounds, start
  positions, full pagination, and malformed requests against `ls`."
  [target layer ls]
  (let [read-line (:read-line target)
        first-h (first-holder-of layer)
        askers [{:asker first-h}
                {:asker :ben}
                {:asker :cal}
                {:asker :ben :session :run-1}
                {:asker :dov :session :run-1}
                {:asker :dov :session :run-2}
                {:asker :nia}
                {:asker :zed}
                {:asker :eve}
                {:not-signed-in true}]
        n-decs (count (:decisions ls))
        starts (distinct [0 1 (max 0 (quot n-decs 2)) (max 0 (dec n-decs)) n-decs (+ n-decs 3)])]
    (or ;; 1. Malformed queries (`R10`)
        (some (fn [bad]
                (st/check-single-page ls bad (try (read-line bad)
                                                  (catch Throwable t {:threw (str t)}))))
              [{:layer layer :start -1 :max-count 5 :asker first-h}
               {:layer layer :start 0 :max-count 0 :asker first-h}
               {:layer layer :start 0 :max-count 5}
               {:layer layer :start 0 :max-count 5 :asker first-h :not-signed-in true}
               {:layer layer :start 0 :max-count 5 :not-signed-in true :session :run-1}])
        ;; 2. Single-page reads across askers, starts, and wide/tight byte bounds
        (some (fn [who-map]
                (or (some (fn [s]
                            (st/check-page-entries-and-bounds
                             ls
                             (merge {:layer layer :start s :max-count 4 :max-bytes wide-bytes} who-map)
                             (read-line (merge {:layer layer :start s :max-count 4 :max-bytes wide-bytes} who-map))))
                          starts)
                    ;; Full pagination under wide byte bound (page-size 3)
                    (st/check-full-pagination
                     read-line ls (merge {:layer layer :max-bytes wide-bytes} who-map) 3)
                    ;; Full pagination under tight 64 KiB byte bound (page-size 2)
                    (st/check-full-pagination
                     read-line ls (merge {:layer layer :max-bytes tight-bytes} who-map) 2)))
              askers))))

(defn run-history!
  "Plays `history` (`{:layer :steps}`) against `target` alongside a fresh
  reference model. Returns `nil` when every statement held, or the first break
  `{:statement kw :detail str :step-index i :step step}`."
  [target {:keys [layer steps]}]
  (let [ref-target (model/make-target)
        depot (depot-of layer)
        other-depot (other-depot-of layer)
        first-h (first-holder-of layer)
        first-perm [first-h layer layer]
        used-act-ids (atom [])
        no-time-act-ids (atom [])]
    (loop [idx 0]
      (when (< idx (count steps))
        (let [step (nth steps idx)
              ls (ref-layer-state ref-target layer)
              aid [layer depot (:uuid step)]
              step-err
              (case (:op step)
                :make-layer
                (let [act (cond-> {:version 1
                                   :who first-h
                                   :session :s-init
                                   :layer layer
                                   :permission first-perm
                                   :facts [{:e layer :k :softland.store/depot :v depot}
                                           {:e first-h :k :softland.store/permission :v first-perm}]}
                            (not= :system first-h) (assoc :subjects #{first-h}))
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :give-layer-perm
                (let [{:keys [holder allows]} step
                      giver (if (and (= :cal holder)
                                     (= :see-and-act (some-> (current-standing-perm ls :ben) model/perm-allows)))
                              :ben
                              first-h)
                      g-perm (or (current-standing-perm ls giver) first-perm)
                      given [holder layer layer g-perm allows]
                      act {:version 1 :who giver :session :s1 :layer layer :permission g-perm
                           :subjects #{holder}
                           :facts [{:e holder :k :softland.store/permission :v given}]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :give-run-perm
                (let [{:keys [holder run allows]} step
                      given [holder layer layer first-perm allows [:run run]]
                      act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :subjects #{holder}
                           :facts [{:e run :k :softland.store/run-permission :v given}]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :give-act-perm
                (let [{:keys [holder target-uuid]} step
                      planned {:version 1 :who holder :session :s-act :layer layer
                               :act-id [layer depot target-uuid] :depot depot
                               :facts [{:e :once :k :v :v 1}]}
                      fact (if-let [mk (:make-act-permission-fact target)]
                             (mk {:layer layer :depot depot :holder holder :parent first-perm
                                  :target-uuid target-uuid :planned-act planned})
                             (let [hex (st/digest-hex planned)]
                               {:e target-uuid
                                :k :softland.store/act-permission
                                :v [holder layer layer first-perm :see-and-act [:act target-uuid hex]]}))
                      act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :subjects #{holder}
                           :facts [fact]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :end-perm
                (let [[kind e] (:target step)
                      k (if (= :run kind) :softland.store/run-permission :softland.store/permission)
                      act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :facts [{:e e :k k}]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :set-who-may-see
                (let [act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :facts [{:e layer :k :softland.store/who-may-see :v (:value step)}]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :set-who-may-read-acts
                (let [act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :facts [{:e layer :k :softland.store/who-may-read-acts :v (:value step)}]}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :bad-acts-setting
                (let [act (case (:kind step)
                            :given-holder
                            (let [bp (or (current-standing-perm ls :ben)
                                         [:ben layer layer first-perm :see-and-act])]
                              {:version 1 :who :ben :session :s1 :layer layer :permission bp
                               :facts [{:e layer :k :softland.store/who-may-read-acts :v :everyone}]})
                            :wrong-entity
                            {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                             :facts [{:e :not-the-layer :k :softland.store/who-may-read-acts :v :everyone}]}
                            :retract
                            {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                             :facts [{:e layer :k :softland.store/who-may-read-acts}]}
                            :bad-word
                            {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                             :facts [{:e layer :k :softland.store/who-may-read-acts :v :strangers}]}
                            :mark-it
                            {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                             :facts [{:e layer :k :softland.store/marked-keys :v #{:softland.store/who-may-read-acts}}]})
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :taken-act
                (let [sender (if (= :first (:sender step)) first-h (:sender step))
                      perm (or (current-standing-perm ls sender) first-perm)
                      who (if (= perm first-perm) first-h sender)
                      facts (if (:heavy? step)
                              g/medium-2-facts
                              [{:e (keyword (str "item-" idx)) :k :note :v (str "val-" idx)}])
                      act {:version 1 :who who :session :run-1 :layer layer :permission perm
                           :based-on {[[layer depot (g/det-uuid 9 idx)] 0] 0}
                           :because-of [layer depot (g/det-uuid 8 idx)]
                           :claimed-when 42
                           :subjects #{who :subject-x}
                           :facts facts}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :refused-with-standing
                (let [stale-fid [[layer depot (:stale-uuid step)] 0]
                      facts (if (:heavy? step)
                              (assoc-in g/heavy-9-facts [0 :replaces] stale-fid)
                              [{:e :stale-place :k :v :v "try" :replaces stale-fid}])
                      act {:version 1 :who first-h :session :s1 :layer layer :permission first-perm
                           :facts facts}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :refused-no-standing
                (let [sender (:sender step)
                      fake-perm [sender layer layer first-perm :see-and-act]
                      facts (if (:heavy? step)
                              g/heavy-9-facts
                              [{:e :unauth :k :v :v (str "from-" (name sender))}])
                      act {:version 1 :who sender :session :s-none :layer layer :permission fake-perm
                           :facts facts}
                      r (send-both! target ref-target act aid)]
                  (swap! used-act-ids conj [aid act])
                  (:err r))

                :refused-declaration
                (let [long-kw (keyword (apply str (repeat 260 "z")))
                      raw (case (:kind step)
                            :no-time-small
                            {:act-id aid :depot depot :version 1 :who :zed :session :s1
                             :layer layer :permission [:zed layer layer]
                             :facts [{:e :d :k :v :v "no-time"}]}
                            :no-time-heavy
                            {:act-id aid :depot depot :version 1 :who :zed :session :s1
                             :layer layer :permission [:zed layer layer]
                             :facts g/heavy-9-facts}
                            :val-100k
                            {:act-id aid :depot depot :door-time 1700000001234
                             :version 1 :who :zed :session :s1 :layer layer :permission [:zed layer layer]
                             :facts [{:e :d :k :big :v g/blob-100k}]}
                            :val-350k
                            {:act-id aid :depot depot :door-time 1700000001235
                             :version 1 :who :zed :session :s1 :layer layer :permission [:zed layer layer]
                             :facts [{:e :d :k :huge :v g/blob-350k}]}
                            :no-who
                            {:act-id aid :depot depot :door-time 1700000001236
                             :version 1 :layer layer :facts [{:e :d :k :v :v 1}]}
                            :bad-who-string
                            {:act-id aid :depot depot :door-time 1700000001237
                             :who "not-an-id" :layer layer}
                            :bad-who-long
                            {:act-id aid :depot depot :door-time 1700000001238
                             :who long-kw :layer layer})]
                  (when (= :no-time-small (:kind step))
                    (swap! no-time-act-ids conj [aid raw]))
                  (swap! used-act-ids conj [aid raw])
                  (append-raw-both! target ref-target depot raw))

                :stray
                (let [raw (case (:kind step)
                            :not-a-map [:not-a-map idx]
                            :no-act-id {:layer layer :who :zed}
                            :other-layer {:act-id [:another-layer depot (:uuid step)] :layer layer}
                            :other-gate-act-id {:act-id [layer other-depot (:uuid step)] :layer layer})]
                  (append-raw-both! target ref-target depot raw))

                :other-gate-arrival
                (let [other-aid [layer other-depot (:uuid step)]
                      raw {:act-id other-aid :depot other-depot :door-time 1700000002222
                           :version 1 :who :zed :session :s1 :layer layer
                           :permission [:zed layer layer]
                           :facts [{:e :wrong-gate :k :v :v idx}]}]
                  (append-raw-both! target ref-target other-depot raw))

                :duplicate-act-id
                (cond
                  (and (= :time-after-no-time (:kind step)) (seq @no-time-act-ids))
                  (let [[old-aid old-raw] (first @no-time-act-ids)]
                    (append-raw-both! target ref-target depot (assoc old-raw :door-time 1700000099999)))

                  (seq @used-act-ids)
                  (let [[old-aid old-map] (last @used-act-ids)
                        dup (if (= :same-new-time (:kind step))
                              (assoc old-map :act-id old-aid :depot depot :door-time (+ 1700000050000 idx))
                              {:act-id old-aid :depot depot :door-time (+ 1700000060000 idx)
                               :version 1 :who :eve :session :s-dup :layer layer
                               :permission [:eve layer layer]
                               :facts [{:e :hijack :k :v :v :different}]})]
                    (append-raw-both! target ref-target depot dup))

                  :else nil)

                :read-sweep
                (run-read-sweep target layer ls))]
          (if step-err
            (assoc step-err :step-index idx :step step)
            (recur (inc idx))))))))
