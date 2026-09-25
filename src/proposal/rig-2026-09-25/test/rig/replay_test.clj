;; IMPORTANT: Before modifying this file, re-read PLAN-replays.md (its
;; validation's fixes F1 to F12 are applied in it) and BUILD_NOTES-replays.md.
(ns rig.replay-test
  "Phase 8, the replays (PLAN-replays.md): the formal model's fourteen fixed
  histories (scenarios.clj: A1 to A8, B1 to B4, D1, D2) played through the
  rig's door, gates and read exit, and through the model under `baseline`,
  the configuration the rig implements.

  The lockstep (RP1). The model queues offers and decides them later; the
  rig's door waits for its answer. So the model is stepped one op at a time
  with its own public functions, as `formal.model/run` composes them (its
  end state is `run`'s, which a test here checks), each op's effects are
  read off the model's state (sends, decisions, person forgets, reads), and
  each rig act is sent at the model op that decides its counterpart, never
  at the op that queued it. A continuation the model decides in a later op
  than the part before it is held in the rig until that op: a read-out by
  phase 4's hold and a resend, a landing by pausing the micro topology.

  Two observations (RP3). The store's judgment, for `play`'s `:values`: the
  rig opens each value with the store's own person locks (phase 2's
  `opens?`, phase 3's frontier read), whoever reads. A reader's view, for
  each read: the one exit, as a live reader. Both are printed, so a reader
  the exit cannot serve (KD10) shows beside the store's judgment.

  What is compared: `:values` and `:shown` as `scenarios/play` compares
  them, every answer name by name through the correspondence, and every
  read. Every difference is reported; one a known difference (KD1 to KD20)
  explains prints with its number; any other fails the test (RP7). A case
  whose stage has not merged is 'not practical' with the names missing, and
  nothing of it is played.

  Every rig API from stage 2 on is found at run time (`requiring-resolve`),
  so this namespace compiles and runs at every step of the build: the
  replays are a standing check. The model-side checks need no cluster.

  The report, `runs/phase8-replays.txt` in the rig folder (or the path in
  RIG_REPLAY_REPORT), is rewritten by every run, in a `finally` (RP8).
  RIG_REPLAY_ROAD=one-module forces the fallback road (one module, a fresh
  world per case) so a pass can exercise it.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fsc]
            [rig.store.module :as m])
  (:import [java.time ZoneId ZonedDateTime]
           [java.time.format DateTimeFormatter]))

;; =================================================================== the cases

(def predicted
  "Each case's status as the plan's case tables predict it, once the stages
  it needs resolve: `:practical` (played by the door alone) or
  `:approximated` with why. The line's outcome is predicted 'as said' for
  every case (values and shown; answers differing only as known)."
  {"A1" {:status :practical} "A2" {:status :practical} "A3" {:status :practical}
   "A4" {:status :practical} "A5" {:status :practical} "A6" {:status :practical}
   "A7" {:status :practical} "A8" {:status :practical}
   "B1" {:status :approximated
         :why "the hold at :before-read-out and a resend stand for the read-out's own step"}
   "B2" {:status :approximated
         :why "the hold at :before-read-out and a resend stand for the read-out's own step"}
   "B3" {:status :approximated
         :why "the paused micro topology stands for the model's queued landing"}
   "B4" {:status :approximated
         :why "the paused micro topology stands for the model's queued landing"}
   "D1" {:status :approximated
         :why "prepare and commit are one batch in the rig; its commit is a check"}
   "D2" {:status :practical}})

(defn- case-of [set-id cases i]
  (let [[title history expect :as c] (nth cases i)
        id (str (str/upper-case (name set-id)) (inc i))]
    {:id id :set set-id :index i :case c :title title :history history :expect expect
     :predicted (get predicted id)}))

(def cases
  "The fourteen fixed histories, in scenarios.clj's order, each with the id
  the plan uses."
  (vec (concat (map #(case-of :a fsc/a-cases %) (range (count fsc/a-cases)))
               (map #(case-of :b fsc/b-cases %) (range (count fsc/b-cases)))
               (map #(case-of :d fsc/d-cases %) (range (count fsc/d-cases))))))

(def case-ids (mapv :id cases))

(def case-by-id (into {} (map (juxt :id identity)) cases))

(def named-configs
  "The configurations `formal.run/-main` names for each case set (run.clj
  L239-252), with the named checks it reports for the set, as it passes them
  to `scenarios/report`. A test checks that these reproduce the model's own
  report, runs/phase8-model-report.txt, line for line."
  {:a {:title "A: forgetting"
       :configs [[:baseline fm/baseline]
                 [:baseline-but-not-owner-required (assoc fm/baseline :wrap :any-subject)]
                 [:baseline-with-a-read-as-owner-and-an-other (assoc fm/baseline :wrap :owner-and-an-other)]
                 [:baseline-with-the-third-reading-of-a (assoc fm/baseline :wrap :owner-and-marked)]]
       :checks [:p6 :x1]}
   :b {:title "B: promotion against a forget"
       :configs [[:baseline fm/baseline]
                 [:baseline-but-not-p6-line-at-the-read-out (assoc fm/baseline :p6-copies :none)]]
       :checks [:p1 :p2 :p5 :p6 :p7 :p8 :x2]}
   :d {:title "D: permissions"
       :configs [[:baseline fm/baseline]
                 [:baseline-but-not-permissions-in-their-layer (assoc fm/baseline :permission-home :anywhere)]]
       :checks [:p1 :p2 :rv]}})

;; ==================================================== the known differences

(def ^:private every-case (set case-ids))

(def known-differences
  "The differences known before the run (PLAN-replays.md, 'Differences known
  before the run, reported every run'), as data. Each cites the cases it
  touches; one no fixed history exercises cites none and says why. A case's
  line prints the numbers of those that showed; one predicted for a case
  that did not show prints 'predicted, not seen'. `:predicted? false` marks
  one that shows only on a named road (KD5)."
  [{:n 1 :group :refusals :cases #{"D1" "D2"}
    :title "a writer without permission is refused on the lease, and the value act on its face"
    :rule "a model no with reason R on a value act matches the rig's lease no with R plus the value act's face :no-such-lock; if the door sends no value act after a refused lease, the lease alone (F8)"
    :source "For Sid 1 and 14; P2 L1968-1973; P3 L225-232"}
   {:n 2 :group :acts :cases every-case
    :title "a lease act before each value write"
    :rule "the door leases in the value's layer; control acts lease nothing; every lease answers yes except under KD1"
    :source "default 1; P2 L105-107"}
   {:n 3 :group :acts :cases #{"B1" "B2" "B3" "B4"}
    :title "the landing lease before a promotion"
    :rule "a lease act of count 1 in the target, bound to the one landing the request causes; answers yes"
    :source "P4 L69-71, PR4, PR9"}
   {:n 4 :group :acts :cases #{"B1" "B2" "B3" "B4"}
    :title "the read-out as a continuation of the request's record"
    :rule "no queued offer: the crossing is decided in the request's event, so B1 and B2 need a hold"
    :source "P4 PR7, L649-652"}
   {:n 5 :group :acts :cases #{"B3" "B4"} :predicted? false
    :title "the re-send from the stored forward"
    :rule "shows only if B3 or B4 falls back to the :before-forward hold: the landing is then sent after the forget, and the outcome is the model's by another road"
    :source "P4 PR8, L660-664, L1023-1026"}
   {:n 6 :group :acts :cases every-case
    :title "every read through the exit is an act"
    :rule "an entry in the reader's working layer, sealed under its own lease, stamped after its moment; counted in the line, never compared"
    :source "defaults 3 and 4; P5 L259-277"}
   {:n 7 :group :acts :cases every-case
    :title "persons are data"
    :rule "the :people layer and the operator's make-person! and forget-person! acts, each answered and stamped, with a fan-out to every task"
    :source "P2 L1904-1906; IMPLICIT_SPEC D3"}
   {:n 8 :group :acts :cases every-case
    :title "layers, the base and the group are made by acts"
    :rule "the base is one-owner on the stream gate until the group's making re-classes it; the model's layers are static"
    :source "P10, default 6, P3 L641-644"}
   {:n 9 :group :refusals :cases every-case
    :title "permission ids and chains"
    :rule "the model's [p l :own] is [p l l] for p's own layer, else [p l l root]; [p l :session] is [p l :alice-hand]; on the session road a write cites [S l l pid] beneath it"
    :source "P3 L442-445, L478-484, M20; F9"}
   {:n 10 :group :refusals :cases #{"A1" "A3" "A4" "A6" "B2" "B4"}
    :title "after a person is forgotten, the exit cannot read for them, and nobody reads their one-owner layers through it"
    :rule "explains a refusal only for a reader whose forget-person! the rig answered yes before the read, refused by the entry's lease :person-forgotten, or :no-such-lock on the entry's face for a lease leased before the forget (F3)"
    :source "P2 L742, L1716-1726; F3, F4; For Sid 28"}
   {:n 11 :group :form :cases every-case
    :title "what a read is and what 'now' means"
    :rule "the model reads every layer as of the largest clock; the rig reads one layer as of its task's clock (a shared layer as of the frontier), a promotion's status by its own query"
    :source "P5 L1293-1316"}
   {:n 12 :group :form :cases every-case
    :title "stamps"
    :rule "hybrid stamps against the model's integers: never compared as values, only the relations both promise"
    :source "default 2; P3 L693-701"}
   {:n 13 :group :form :cases #{"B3" "B4"}
    :title "done means settled"
    :rule "the rig reads done once the landing is at or below the frontier; the replay reads after the frontier passes the landing's batch"
    :source "P4 L329-331, L553-555"}
   {:n 14 :group :not-exercised :cases #{}
    :title "leaders, epochs, fencing, failover and the deposed leader; the model's rulers :tick and :order"
    :why "no fixed history steps [:failover] or [:zombie]; a history that does is refused as not practical"
    :source "IMPLICIT_SPEC D7"}
   {:n 15 :group :not-exercised :cases #{}
    :title "Rama replays completed records after a crash; the model only unfinished offers"
    :why "no fixed history crashes"
    :source "IMPLICIT_SPEC D4"}
   {:n 16 :group :not-exercised :cases #{}
    :title "batch order: the rig orders one batch by the names' UUID7s, the model by its inbox"
    :why "no fixed history has two offers in one batch"
    :source "P3 L1144-1146"}
   {:n 17 :group :form :cases #{"B1" "B3"}
    :title "a value forget's answer carries :how"
    :rule ":row-deleted or :excised; the model's is a trace note; printed, not compared"
    :source "P2 L1108-1110"}
   {:n 18 :group :not-exercised :cases #{}
    :title "per-act lock grain with mixed marks"
    :why "no fixed history switches grain"
    :source "P2 L1512-1514"}
   {:n 19 :group :not-exercised :cases #{}
    :title "named in the plans and not exercised: a resend after a forget answered without a content check; a resend inside one micro batch heard as :name-taken; {:unreadable :does-not-open}; :no-such-person and :no-such-value; :landing-lock-gone, except as B4's risk; a landing into the base before its re-class; the group's mark-on-write rule"
    :why "no fixed history reaches them"
    :source "For Sid 2 and 15; P3 L289-295; P2 L749, L1239-1240, L1706-1707; P4 L121-124, L285-292; P3 O17"}
   {:n 20 :group :form :cases #{"B1" "B2" "B3" "B4" "D1" "D2"}
    :title "control facts differ in form"
    :rule "the request, crossing, value forget and revoke facts are compared on existence and on their references through the correspondence (source, target, request, pid), not on their literal value maps"
    :source "F2; traces L125-127, L181-184"}])

(defn kd-str [n] (str "KD" n))

(defn predicted-kds
  "The known differences predicted for a case: those citing it, less those
  that show only on a named road."
  [id]
  (into (sorted-set) (comp (filter #(contains? (:cases %) id))
                           (remove #(false? (:predicted? %)))
                           (map :n))
        known-differences))

(def plan-case-kds
  "The known differences the plan's case tables name in each case's
  predicted line, which the table above must cite for that case."
  {"A1" #{10} "A3" #{10} "A4" #{10} "A6" #{10}
   "B1" #{3 4} "B2" #{10} "B3" #{3 4 13} "B4" #{10}
   "D1" #{1} "D2" #{1}})

;; ============================================================ the step kinds

(def step-rows
  "Every step kind the fixed histories use, with what the model does and what
  the rig plays at the op that decides it (PLAN-replays.md, 'The step kinds
  and their rig counterparts'). `:covers?` says which ops of the kind the row
  covers; an op no row covers makes its history 'not practical', with the
  op named, and nothing of it is played."
  [{:kind :offer :covers? (fn [[_ spec]] (and (map? spec) (= 1 (:times spec 1)) (nil? (:stood-on spec))
                                              (contains? #{nil :session} (:cite spec))))
    :model "queues an offer; :times 1 sends once"
    :rig "at its decision: the door's offer of the act with the mapped permission, a lease act first (KD2); :cite :session cites [:alice l :alice-hand]"
    :faithful "yes, with KD2's added lease act"}
   {:kind :work :covers? (constantly true)
    :model "runs stream partition p's queue to empty"
    :rig "nothing of its own: each decision it made is played as its own effect"
    :faithful "yes"}
   {:kind :step :covers? (constantly true)
    :model "one stream event on p"
    :rig "the one decision that event made; a request whose read-out the model decides in a later op is held at :before-read-out; the read-out's decision is the release and a resend"
    :faithful "approximated where it splits a request from its read-out"}
   {:kind :batch :covers? (constantly true)
    :model "micro prepare and commit"
    :rig "each decision it made, sent and waited for, the frontier past its batch; for a held landing, the resume first"
    :faithful "yes; a held landing is approximated"}
   {:kind :prepare :covers? (constantly true)
    :model "decides at prepare, shows at commit"
    :rig "the decision's offer sent at [:prepare] and waited for, decided and committed together"
    :faithful "approximated: the rig has no step between a batch's decision and its commit"}
   {:kind :commit :covers? (constantly true)
    :model "records what prepare decided"
    :rig "a check that the answers played at [:prepare] are visible; nothing sent"
    :faithful "approximated with [:prepare]"}
   {:kind :forget-person :covers? (constantly true)
    :model "destroys p's person lock at once, stamped after everything"
    :rig "the operator's forget-person! act in :people and its fan-out to every task, waited for"
    :faithful "yes, with KD7"}
   {:kind :forget-value :covers? (constantly true)
    :model "queues a forget of the i-th readable locked value, by the owner (the operator in a shared layer)"
    :rig "at its decision: forget-value! by the same writer, the target through the correspondence; a target in a shared layer has no rig row tonight"
    :faithful "yes"}
   {:kind :promote :covers? (fn [[_ _ target times where]] (and (= :group target) (= 1 times) (nil? where)))
    :model "queues Alice's request for the i-th readable locked value of :alice into the group"
    :rig "at its decision: lease-landing! in :group, answered (KD3); then, per the holds, promote! of the request naming the mapped source and target"
    :faithful "yes, with KD3; approximated where a hold is needed"}
   {:kind :read :covers? (fn [[_ a]] (= :now a))
    :model "a read of every layer as of the largest clock, with each promotion's status"
    :rig "the reader's view through the exit, the store's judgment beside a refusal, and promotion-status, as of now"
    :faithful "yes, with KD6 and KD11"}
   {:kind :revoke :covers? (constantly true)
    :model "the operator's revoke, a fact in the permission's layer standing on its grant"
    :rig "the operator's revoke act in the mapped permission's layer through that layer's gate, standing on its grant"
    :faithful "yes"}])

(def rowless-kinds
  "Step kinds with no rig row, named so a later history is not silently
  skipped: the door's resend and a reuse (phase 1's), the owner's and the
  operator's setting acts, and the leader steps (KD14)."
  #{:retry :reuse :set-grain :reclass :failover :zombie})

(def step-row-by-kind (into {} (map (juxt :kind identity)) step-rows))

(defn history-refusal
  "Why a history cannot be played, or nil: the first op no row covers,
  named. Nothing of such a history is played."
  [history]
  (some (fn [[i op]]
          (let [row (step-row-by-kind (first op))]
            (cond
              (nil? row) (str "step " i " " (pr-str op) " has no rig row"
                              (when (contains? rowless-kinds (first op)) " (named, not built)"))
              (not ((:covers? row) op)) (str "step " i " " (pr-str op) " is outside its row, " (name (first op))))))
        (map-indexed vector history)))

;; ================================================================ the world

(def main-road-names
  "The rig's names on the main road: the model's own (a fresh module per
  case, RP2)."
  identity)

(defn fallback-names
  "The rig's names for case `id` on the fallback road (one module, a fresh
  world per case): persons, one-owner layers, the group and the sessions
  carry the case's id; the base stays one, since no fixed history writes
  into it. Entities and keys are the model's."
  [id]
  (let [sfx (str "-" (str/lower-case id))
        renamed #{:alice :bob :alice-hand :alice-agent :bob-hand :group :alice-session :bob-session}]
    (fn [x] (if (contains? renamed x) (keyword (str (name x) sfx)) x))))

(defn world
  "The rig's world as data, the model's (model.clj L37-67) mapped through
  `rn` (PLAN-replays.md, 'The world, as data'): persons made by the
  operator's acts; Alice's three layers, and [F4] Bob's rig-only working
  layer for his read entries; the base on the stream gate re-classed by the
  group's making; the group with both members; every model permission as
  the rig's id; a session per writer (RP6)."
  [rn]
  (let [[alice bob group] (map rn [:alice :bob :group])
        base-root [:operator :base :base]
        group-root [group group group]]
    {:persons [alice bob]
     :one-owner [[(rn :alice) {:kind :personal :owner alice}]
                 [(rn :alice-hand) {:kind :hand :owner alice}]
                 [(rn :alice-agent) {:kind :agent :owner alice}]
                 [(rn :bob-hand) {:kind :hand :owner bob}]]
     :stream-grants (vec (concat (for [l [:alice :alice-hand :alice-agent]] [alice (rn l) (rn l)])
                                 (for [l [:alice :alice-agent :group :base]] [alice (rn l) (rn :alice-hand)])
                                 [[bob (rn :bob-hand) (rn :bob-hand)]]))
     :base {:root base-root
            :grants [[alice :base :base base-root] [bob :base :base base-root]]}
     :group-layer group
     :group {:members #{alice bob}
             :root group-root
             :grants [[alice group group group-root] [bob group group group-root]]}
     :sessions {alice (rn :alice-session) bob (rn :bob-session)}
     :session-layers [[(rn :alice-session) alice [(rn :alice) group]]
                      [(rn :bob-session) bob [group]]]
     :working {alice [(rn :alice-hand) [alice (rn :alice-hand) (rn :alice-hand)]]
               bob [(rn :bob-hand) [bob (rn :bob-hand) (rn :bob-hand)]]}}))

(defn rig-pid
  "A model permission id as the rig's (KD9): [p l :own] is [p l l] where p
  owns l, else [p l l root] beneath l's root; [p l :session] is [p l
  :alice-hand], kept in Alice's hand layer."
  [rn [who layer where]]
  (let [p (rn who) l (rn layer)]
    (case where
      :session [p l (rn :alice-hand)]
      :own (cond
             (= who (get-in fm/layers [layer :owner])) [p l l]
             (= :base layer) [p l l [:operator :base :base]]
             :else [p l l [l l l]]))))

(def door-road
  "Which permission a write cites (the world table, F9): `:direct` cites the
  writer's own permission, the model's pid mapped; `:session` cites the
  session's grant [S l l pid] beneath it. Phase 2's and phase 3's doors, as
  built, take either (rig.store.permit: the holder is the act's :who or its
  :session), so the plan's direct road is taken; the report says so."
  :direct)

(defn cited-pid
  "The permission a write by `who` into `layer` cites on the door's road."
  [w rn model-pid]
  (let [pid (rig-pid rn model-pid)]
    (if (= :session door-road)
      (let [s (get-in w [:sessions (nth pid 0)])] [s (nth pid 1) (nth pid 1) pid])
      pid)))

(defn rig-value
  "A model value as the rig's (R17's domain: the model's value map verbatim),
  the persons it names renamed."
  [rn v]
  (cond-> v (set? (:persons v)) (update :persons #(into #{} (map rn) %))))

(defn one-owner-layer? [model-layer] (boolean (get-in fm/layers [model-layer :one-owner])))

;; ============================================================ the lockstep

(defn reduce-answers
  "[F6] The model's answers for one name, recorded at one or more partitions,
  as one: all yes to yes, all no with one reason to that no; otherwise
  `:split`, which is the model's own finding and fails the test, since the
  rig answers once per name."
  [as]
  (let [ys (set (map :answer as))
        rs (set (map :reason as))]
    (cond
      (empty? as) nil
      (= #{:yes} ys) {:answer :yes :stamp (apply max (map :stamp as)) :where (mapv :where as)}
      (and (= #{:no} ys) (= 1 (count rs))) {:answer :no :reason (first rs)
                                             :stamp (apply max (map :stamp as)) :where (mapv :where as)}
      :else {:answer :split :answers (vec as)})))

(defn- answered? [st nm] (boolean (seq (fm/answers-for st nm))))

(defn offer-kind
  "What a model offer is, for the rig: a request, a read-out (crossing), a
  landing, a value forget, a revoke, or a value act."
  [offer]
  (cond
    (:read-out offer) :crossing
    (:promote offer) :request
    (and (:source offer) (:because-of offer)) :landing
    (some #(= :forget (:k %)) (:facts offer)) :forget
    (some #(= :revoke (:k %)) (:facts offer)) :revoke
    :else :value))

(defn- decision-of
  "A name the op decided, with the offer as the model built it."
  [after nm client]
  (let [offer (get-in after [:sent nm])
        as (fm/answers-for after nm)
        r (reduce-answers as)]
    {:name nm :offer offer :kind (offer-kind offer) :client? (contains? client nm)
     :store (first (first (:where r (map :where as))))
     :answers (vec as) :model r :stamp (:stamp r)}))

(defn- order-decisions
  "The op's decisions in the order the model made them, or `{:guard why}`
  when that order cannot be told (the plan's guard: a later history that
  breaks it is caught, not mis-ordered). A work or step op decides on one
  stream partition, ordered by stamp; a batch in its inbox's order; a
  prepare in its delta's order; anything else at most one decision."
  [op before ds]
  (let [kind (first op)
        places (set (mapcat #(map :where (:answers %)) ds))]
    (cond
      (<= (count ds) 1) {:order (vec ds)}

      (and (#{:work :step} kind) (= 1 (count places)) (= :stream (ffirst places)))
      (let [sorted (vec (sort-by :stamp ds))]
        (if (apply < (map :stamp sorted))
          {:order sorted}
          {:guard (str "two decisions at one stamp in " (pr-str op))}))

      (#{:batch :commit} kind)
      (let [inbox (mapv :name (get-in before [:micro :inbox]))
            pos (fn [d] (or (first (keep-indexed (fn [i n] (when (= n (:name d)) i)) inbox)) Long/MAX_VALUE))]
        (if (every? #(= :micro (:store %)) ds)
          {:order (vec (sort-by pos ds))}
          {:guard (str "stream and micro decisions in one " (pr-str op))}))

      :else {:guard (str (count ds) " decisions on " (count places) " partitions in one "
                         (pr-str op) ", whose order the adapter cannot tell")})))

(defn op-effects
  "What one op did to the model, read off its state before and after (the
  plan's four effects): names new in `:sent` (with the offers as the model
  built them), names that gained a recorded answer, in the model's order,
  and for [:prepare] the decisions in its delta [F7]; persons forgotten;
  reads taken."
  [i op before after]
  (let [client (set (:client-sent after))
        sent (sort (remove #(contains? (:sent before) %) (keys (:sent after))))
        decided (for [nm (sort (keys (:sent after)))
                      :when (and (answered? after nm) (not (answered? before nm)))]
                  (decision-of after nm client))
        {:keys [order guard]} (order-decisions op before decided)
        prepared (when (= :prepare (first op))
                   (vec (for [{:keys [offer reason stamp parts]} (get-in after [:micro :prepared :delta])]
                          {:name (:name offer) :offer offer :kind (offer-kind offer)
                           :client? (contains? client (:name offer)) :store :micro
                           :model (if reason {:answer :no :reason reason :stamp stamp} {:answer :yes :stamp stamp})
                           :stamp stamp :parts parts :prepared? true})))]
    {:index i :op op :before before :after after
     :sends (vec (for [nm sent] {:name nm :offer (get-in after [:sent nm]) :client? (contains? client nm)}))
     :decided (or order []) :guard guard
     :prepared (or prepared [])
     :forgets (vec (for [[p rec] (:persons after) :when (not (contains? (:persons before) p))] (assoc rec :person p)))
     :reads (vec (for [[j r] (map-indexed vector (:reads after)) :when (>= j (count (:reads before)))]
                   {:read-index j :read r}))}))

(defn lockstep
  "Play `history` through the model one op at a time, as `formal.model/run`
  composes its public functions: the first facts, each op by `step`, the
  closing `drain`, then a tick and a read as of now. Returns the start and
  end states and one record of effects per op, the drain and the closing
  read last. The end state is `run`'s (checked by a test)."
  [config history]
  (let [st0 (fm/seed-permissions (fm/init config))
        [st steps] (reduce (fn [[st acc] [i op]]
                             (let [st' (fm/step st op)]
                               [st' (conj acc (op-effects i op st st'))]))
                           [st0 []]
                           (map-indexed vector history))
        n (count history)
        drained (fm/drain st)
        closed (-> drained
                   (update :tick inc)
                   (as-> s (update s :reads conj (fm/read-as-of s (fm/now s)))))]
    {:start st0 :end closed
     :steps (conj steps
                  (op-effects n [:drain] st drained)
                  (op-effects (inc n) [:closing-read] drained closed))}))

(defn seen-of
  "What `scenarios/play` compares, from a model state: each expected ref's
  value open, erased or missing (its `find-fact`, the model's own), and the
  statuses every read showed. A test checks it against `play` for every
  case."
  [st expect]
  (let [values (into {} (for [[ref _] (:values expect)
                              :let [f (#'fsc/find-fact st ref)]]
                          [ref (cond (nil? f) :missing (fm/readable? st f) :open :else :erased)]))
        shown (vec (for [r (:reads st) pr (:promotions r)] (:status pr)))]
    (cond-> {:values values} (:shown expect) (assoc :shown shown))))

(defn guard-refusal
  "Why the lockstep cannot order a history's decisions, or nil."
  [ls]
  (some (fn [s] (when (:guard s) (str "step " (:index s) ": " (:guard s)))) (:steps ls)))

(defn effect-refusal
  "Why an effect the lockstep found has no rig row, or nil: a value forget
  of a fact in a shared layer (the row covers a one-owner layer's forget)."
  [ls]
  (some (fn [s]
          (some (fn [d]
                  (when (= :forget (:kind d))
                    (let [target (get-in d [:offer :facts 0 :v :target])
                          f (fm/fact-by-id (:after s) target)]
                      (when-not (one-owner-layer? (:layer f))
                        (str "step " (:index s) ": a forget of " (fm/fid-str target) " in "
                             (name (:layer f)) " has no rig row tonight")))))
                (concat (:decided s) (:prepared s))))
        (:steps ls)))

;; ====================================================== the model's own lines

(defn model-lines
  "The lines `scenarios/report` prints for one case under one configuration,
  without its 'under' header: exactly the model's own report."
  [cname cfg c checks]
  (vec (rest (str/split-lines (with-out-str (fsc/report [(:case c)] [[cname cfg]] checks))))))

(defn model-report-lines
  "What `scenarios/report` prints for every case set under `named-configs`,
  in run.clj's order."
  []
  (vec (mapcat (fn [set-id]
                 (let [{:keys [configs checks]} (named-configs set-id)
                       cs (mapv :case (filter #(= set-id (:set %)) cases))]
                   (str/split-lines (with-out-str (fsc/report cs configs checks)))))
               [:a :b :d])))

;; ======================================================= the APIs, by stage

(def ^:private b-cases #{"B1" "B2" "B3" "B4"})
(def ^:private group-writes #{"A2" "A3" "A4" "A5" "A6" "A7" "D1" "D2"})
(def ^:private stream-writes #{"A1" "A8" "B1" "B2" "B3" "B4"})
(def ^:private person-forgets #{"A1" "A2" "A3" "A4" "A5" "A6" "A7" "A8" "B2" "B4"})

(def apis
  "Every rig API the replay calls (PLAN-replays.md, 'The APIs it needs, by
  stage'), with the var it binds to, found at run time. `:bound` says where
  the name comes from: `:built` as the stage's build names it (phase 1 and
  phase 5a on this branch; phase 2 on rig-build-locks at fd41f6d2; phase 3
  on rig-build-micro at e60c8ee1), `:plan` as the plan names it for a stage
  not built yet, a binding to confirm. `:plan-name` is the plan's name where
  the build's differs. `:cases` is who needs it; `:optional` ones never make
  a case not practical (the line names the road taken without them)."
  [;; ---- stage 1, phase 1's (built on this branch)
   {:id :client/build :stage "1" :var 'rig.store.client/build :bound :built :cases :all}
   {:id :client/offer-until-answered! :stage "1" :var 'rig.store.client/offer-until-answered! :bound :built :cases :all}
   {:id :client/record :stage "1" :var 'rig.store.client/record :bound :built :cases :all}
   {:id :client/settings :stage "1" :var 'rig.store.client/settings :bound :built :cases :all}
   {:id :client/grant-offer :stage "1" :var 'rig.store.client/grant-offer :bound :built :cases :all}
   {:id :client/make-layer-offer :stage "1" :var 'rig.store.client/make-layer-offer :bound :built :cases :all}
   {:id :client/revoke-offer :stage "1" :var 'rig.store.client/revoke-offer :bound :built :cases #{"D1"}}
   {:id :env/make-name :stage "1" :var 'rig.store.envelope/make-name :bound :built :cases b-cases}
   {:id :inject/reset-all! :stage "1" :var 'rig.store.inject/reset-all! :bound :built :cases :all}
   ;; ---- stage 2, phase 2's door, persons and forgets (rig-build-locks, fd41f6d2)
   {:id :client/people-layer :stage "2" :var 'rig.store.client/people-layer :bound :built :cases :all
    :plan-name "the :people layer (P2 L1926-1927)"}
   {:id :client/make-person! :stage "2" :var 'rig.store.client/make-person! :bound :built :cases :all}
   {:id :client/stock! :stage "2" :var 'rig.store.client/stock! :bound :built :cases stream-writes
    :plan-name "offer! sealing at the door, leasing when short; lease! (the lease act made visible: stock! leases ahead into the door's pool)"}
   {:id :client/forget-person! :stage "2" :var 'rig.store.client/forget-person! :bound :built :cases person-forgets}
   {:id :client/person-on-task :stage "2" :var 'rig.store.client/person-on-task :bound :built :cases person-forgets}
   {:id :client/forget-value! :stage "2" :var 'rig.store.client/forget-value! :bound :built :cases #{"B1" "B3"}}
   {:id :client/opens? :stage "2" :var 'rig.store.client/opens? :bound :built :cases stream-writes}
   {:id :client/depot-records :stage "2" :var 'rig.store.client/depot-records :bound :built
    :cases #{"A1" "A3" "A4" "A6" "B2" "B4"} :plan-name "(KD10's attribution: the entry's cited lease, F3)"}
   {:id :grammar/grammars :stage "2" :var 'rig.store.grammar/grammars :bound :built
    :cases #{"A1" "A2" "A4" "A5" "A6" "A7" "A8"}
    :check (fn [g] (= {:subjects-at [:persons]} (get g :mention)))
    :check-says "the grammar {:mention {:subjects-at [:persons]}}"}
   ;; ---- stage 3, phase 3's micro store (rig-build-micro, e60c8ee1)
   {:id :micro/connect :stage "3" :var 'rig.store.micro-client/connect :bound :built :cases :all}
   {:id :micro/make-base! :stage "3" :var 'rig.store.micro-client/make-base! :bound :built :cases :all}
   {:id :micro/make-group! :stage "3" :var 'rig.store.micro-client/make-group! :bound :built :cases :all}
   {:id :micro/open-session! :stage "3" :var 'rig.store.micro-client/open-session! :bound :built :cases :all}
   {:id :micro/settings-of :stage "3" :var 'rig.store.micro-client/settings-of :bound :built :cases :all}
   {:id :micro/offer! :stage "3" :var 'rig.store.micro-client/offer! :bound :built :cases :all
    :plan-name "the micro door, *micro-offers through the client's dispatch by tag"}
   {:id :micro/grant-offer :stage "3" :var 'rig.store.micro-client/grant-offer :bound :built :cases :all}
   {:id :micro/frontier :stage "3" :var 'rig.store.micro-client/frontier :bound :built :cases :all
    :plan-name "micro-frontier (F8: built as micro-client/frontier)"}
   {:id :micro/write! :stage "3" :var 'rig.store.micro-client/write! :bound :built :cases group-writes
    :plan-name "the micro door with its lease (micro leases)"}
   {:id :micro/lookup :stage "3" :var 'rig.store.micro-client/lookup :bound :built :cases #{"B3" "B4"}
    :plan-name "micro-lookup"}
   {:id :micro/open-act :stage "3" :var 'rig.store.micro-client/open-act :bound :built
    :cases #{"A2" "A3" "A4" "A5" "A6" "A7" "B3" "B4" "D1" "D2"}
    :plan-name "micro-act at F and the pure open of a row (open-row)"}
   {:id :micro/revoke-offer :stage "3" :var 'rig.store.micro-client/revoke-offer :bound :built :cases #{"D2"}}
   ;; ---- stage 4, phase 4's promotion (not built: the plan's names, to confirm)
   {:id :client/lease-landing! :stage "4" :var 'rig.store.client/lease-landing! :bound :plan :cases b-cases}
   {:id :client/promote! :stage "4" :var 'rig.store.client/promote! :bound :plan :cases b-cases}
   {:id :client/promotion-status :stage "4" :var 'rig.store.client/promotion-status :bound :plan :cases b-cases}
   {:id :inject/hold! :stage "4" :var 'rig.store.inject/hold! :bound :plan :cases #{"B1" "B2"}
    :plan-name "inject/hold! at :before-read-out and :before-forward (never rig.claims/hold!, F8)"}
   {:id :inject/release! :stage "4" :var 'rig.store.inject/release! :bound :plan :cases #{"B1" "B2"}}
   {:id :env/crossing-name :stage "1" :var 'rig.store.envelope/crossing-name :bound :built :cases b-cases
    :plan-name "env/crossing-name (the plan lists it under stage 4; phase 1 built it)"}
   {:id :env/landing-name :stage "1" :var 'rig.store.envelope/landing-name :bound :built :cases #{"B3" "B4"}
    :plan-name "env/landing-name (listed under stage 4; phase 1 built it, and phase 4 makes it take the class: the 3-arity is tried first)"}
   ;; ---- stage 5a, the one-owner read exit (built on this branch)
   {:id :exit/connect :stage "5a" :var 'rig.store.read-exit/connect :bound :built :cases :all}
   {:id :exit/read! :stage "5a" :var 'rig.store.read-exit/read! :bound :built :cases :all}
   ;; ---- stage 5, the rest: the exit's shared-layer read (to confirm; optional)
   {:id :shared/moment :stage "5-rest" :var 'rig.store.shared-reads/moment :bound :plan :cases :all :optional true
    :plan-name "the exit's shared-layer read (PLAN-reads-rest.md: read-exit/read! on a group layer, rig.store.shared-reads)"}])

(def stage-order ["1" "2" "3" "4" "5a" "5-rest"])

(defn- needs? [api id]
  (and (not (:optional api)) (or (= :all (:cases api)) (contains? (:cases api) id))))

(defn- ns-file-present?
  "Whether the var's namespace has a source file on the classpath, so a
  failure to load it is a broken build, not a stage not merged yet."
  [sym]
  (let [path (-> (namespace sym) (str/replace "-" "_") (str/replace "." "/"))]
    (boolean (or (io/resource (str path ".clj")) (io/resource (str path ".cljc"))))))

(defn resolve-api
  "One API found at run time: `{:value v}`, `{:missing why}` when its stage
  has not merged (no such namespace, or no such var in it), or `{:broken
  why}` when its namespace is there and does not load, or its check fails."
  [{:keys [var check check-says]}]
  (try
    (if-let [v (requiring-resolve var)]
      (let [x @v]
        (if (and check (not (check x)))
          {:broken (str "its value is not " check-says ": " (pr-str x))}
          {:value x}))
      {:missing "no such var"})
    (catch Throwable t
      (if (ns-file-present? var)
        {:broken (str "its namespace does not load: " (.getSimpleName (class t)) " " (ex-message t))}
        {:missing "no such namespace"}))))

(defn resolve-apis
  "Every API in the table, found now: {id result}."
  []
  (into {} (for [a apis] [(:id a) (assoc (resolve-api a) :api a)])))

(defn case-missing
  "The APIs case `id` needs that did not resolve, in stage order."
  [resolved id]
  (vec (for [a apis
             :when (needs? a id)
             :let [r (get resolved (:id a))]
             :when (not (contains? r :value))]
         (merge a (select-keys r [:missing :broken])))))

(defn missing-str
  "The missing names of a case, grouped by stage, for its line."
  [missing]
  (str/join "; " (for [[stage ms] (sort-by #(.indexOf ^java.util.List stage-order (key %)) (group-by :stage missing))]
                   (str "stage " stage " missing: "
                        (str/join ", " (for [m ms] (str (:var m) (when (:broken m) (str " (broken: " (:broken m) ")")))))))))

(defn api-fn
  "The resolved value of an API, or a throw naming it (a case that reached
  here was judged practical, so this is a bug in the adapter's table)."
  [resolved id]
  (let [r (get resolved id)]
    (if (contains? r :value)
      (:value r)
      (throw (ex-info (str "API not resolved: " id) {::adapter id})))))

;; ============================================================ the plumbing

(def module-options
  "Four tasks, so a layer's home, the group's entities and the person
  fan-out cross tasks; fixed, so runs repeat (RP2)."
  {:tasks 4 :threads 2})

(def micro-topology
  "The micro gate's microbatch topology, paused to hold a landing (phase 3's
  micro.clj L1149, built at e60c8ee1)."
  "micro")

(def wait-ms "Every wait's bound: an answer, a frontier, a fan-out (P3 L2128-2133)." 60000)
(def poll-ms 50)
(def relaunch-limit-ms "RP2: past this a second launch sends the run to the fallback road." 15000)

(def fan-keys
  "Routing keys for reading a person's entry on every task: 64 keys over four
  tasks miss one with a chance near 4 in 10^8."
  (mapv #(keyword (str "e" %)) (range 64)))

(defn- ms-since [t0] (quot (- (System/nanoTime) t0) 1000000))

(defn wait-for
  "Poll `f` every 50 ms until it gives a truthy value, for at most 60 s; a
  read that throws is 'not yet'. On a timeout, throws `::timeout`, which its
  case reports as 'differs: no answer within 60 s', never a hung suite."
  [what f]
  (let [deadline (+ (System/currentTimeMillis) wait-ms)]
    (loop []
      (let [v (try (f) (catch Exception _ nil))]
        (cond
          v v
          (> (System/currentTimeMillis) deadline)
          (throw (ex-info (str "no answer within 60 s: " what) {::timeout what}))
          :else (do (Thread/sleep poll-ms) (recur)))))))

(defn module-name [] (rama/get-module-name m/Store))

(defn launch! [ipc] (rtest/launch-module! ipc m/Store module-options))

(defn destroy! [ipc] (rtest/destroy-module! ipc (module-name)))

(defn road-check
  "RP2's check, made once per run before the cases: launch, destroy and
  launch again, timed. The fresh-module road runs unless the second launch
  fails or takes longer than 15 s, or RIG_REPLAY_ROAD=one-module forces the
  fallback. The module is destroyed after."
  [ipc]
  (let [forced (= "one-module" (System/getenv "RIG_REPLAY_ROAD"))
        t0 (System/nanoTime)
        first-err (try (launch! ipc) nil (catch Throwable e e))
        launch-ms (ms-since t0)
        t1 (System/nanoTime)
        _ (when-not first-err (destroy! ipc))
        destroy-ms (ms-since t1)
        t2 (System/nanoTime)
        second-err (when-not first-err (try (launch! ipc) nil (catch Throwable e e)))
        relaunch-ms (ms-since t2)]
    (when (and (not first-err) (not second-err)) (destroy! ipc))
    {:launch-ms launch-ms :destroy-ms destroy-ms :relaunch-ms (when-not first-err relaunch-ms)
     :error (some-> (or first-err second-err) ex-message)
     :forced forced
     :road (cond first-err :none
                 (or forced second-err (> relaunch-ms relaunch-limit-ms)) :one-module
                 :else :fresh)}))

(defn connect
  "The merged handles, once per module (F8): the read exit's (which wraps
  the client's) and the micro client's (which wraps it too), so one door,
  one lease pool and every query handle."
  [api ipc]
  (merge ((api-fn api :exit/connect) ipc) ((api-fn api :micro/connect) ipc)))

(defn answer-of
  "An answer's keyword and reason, from a stream ack, a micro-lookup result,
  or anything the door returns."
  [a]
  {:answer (:answer a) :reason (:reason a) :stamp (:stamp a)})

(defn yes? [a] (= :yes (:answer a)))

(defn answers-in
  "The answers inside what a seed call returns: an ack or a micro answer; a
  vector of them (make-base!); make-group!'s {:reclass {:answer} :made};
  open-session!'s {layer answer}."
  [x]
  (cond
    (and (map? x) (keyword? (:answer x))) [x]
    (and (map? x) (contains? x :made)) (concat (some-> (:reclass x) :answer vector) [(:made x)])
    (map? x) (mapcat answers-in (vals x))
    (sequential? x) (mapcat answers-in x)
    :else [{:answer :unknown :value x}]))

(defn- grant!
  "Grant a permission in the layer it lives in, through that layer's gate:
  the micro gate once the layer is by entity (as open-session! chooses)."
  [api store [_ _ in :as pid]]
  (if (= :by-entity (:class ((api-fn api :micro/settings-of) store in)))
    ((api-fn api :micro/offer!) store ((api-fn api :micro/grant-offer) pid))
    ((api-fn api :client/offer-until-answered!) store ((api-fn api :client/grant-offer) store pid))))

(defn seed-steps
  "The rig's seed (the world table, KD7, KD8), as labelled calls in order:
  the store layer and the persons, the one-owner layers (Bob's rig-only
  :bob-hand among them, F4) and the permissions kept in them, the base and
  the group (the group's making re-classes the base), then each writer's
  session. On the fallback road only the first case makes the store layer
  and the base; later cases grant their persons' base permissions."
  [api store w first?]
  (let [f #(api-fn api %)
        send (f :client/offer-until-answered!)]
    (concat
     (when first?
       [["the store layer" #(send store ((f :client/make-layer-offer) (f :client/people-layer) {:kind :store}))]])
     (for [p (:persons w)] [(str "person " (name p)) #((f :client/make-person!) store p)])
     (for [[l spec] (:one-owner w)] [(str "layer " (name l)) #(send store ((f :client/make-layer-offer) l spec))])
     (for [pid (:stream-grants w)] [(str "grant " (pr-str pid)) #(send store ((f :client/grant-offer) store pid))])
     (if first?
       [["the base" #((f :micro/make-base!) store (:base w))]]
       (for [pid (get-in w [:base :grants])] [(str "grant " (pr-str pid)) #(grant! api store pid)]))
     [["the group" #((f :micro/make-group!) store (:group-layer w) (:group w))]]
     (for [[S p layers] (:session-layers w)]
       [(str "session " (name S)) #((f :micro/open-session!) store S p layers)]))))

;; ============================================ judging, pure (tested without a cluster)

(defn answer-str
  "An answer in a line: yes, or no with its reason."
  [a]
  (cond
    (nil? a) "none"
    (= :yes (:answer a)) "yes"
    (= :no (:answer a)) (str "no " (some-> (:reason a) name))
    :else (str (some-> (:answer a) name) (when (:reason a) (str " " (name (:reason a)))))))

(defn judge-answer
  "The model's answer to a name against the rig's counterpart. `lease` is
  the lease the rig's act was sealed under, when the door leased for it.
  Returns {:ok? :kd :says}: KD1's rule is data here (a model no with reason
  R on a value act is the rig's lease no with R plus the value act's face
  `:no-such-lock`, or [F8] the lease alone when no value act was sent), and
  [F11] a refused lease any other way is a difference in itself."
  [model rig lease]
  (let [lease-no? (and lease (= :no (:answer lease)))
        kd1? (and (= :no (:answer model)) lease-no? (= (:reason model) (:reason lease))
                  (or (nil? rig) (and (= :no (:answer rig)) (= :no-such-lock (:reason rig)))))]
    (cond
      (= :split (:answer model))
      {:ok? false :says (str "the model's own finding: its partitions disagree (F6): " (pr-str (:answers model)))}

      kd1? {:ok? true :kd 1
            :says (str "lease no " (name (:reason lease))
                       (if rig ", the value act :no-such-lock on its face" ", no value act sent (F8)"))}

      lease-no? {:ok? false :says (str "its lease was refused: " (answer-str lease) "; model " (answer-str model)
                                       ", rig " (answer-str rig))}

      (and (= :yes (:answer model)) (= :yes (:answer rig))) {:ok? true}

      (and (= :no (:answer model)) (= :no (:answer rig)) (= (:reason model) (:reason rig))) {:ok? true}

      :else {:ok? false :says (str "model " (answer-str model) ", rig " (answer-str rig))})))

(defn judge-exit-refusal
  "[F3] Whether KD10 explains an exit read's refusal: only for a reader
  whose forget-person! the rig answered yes before the read, refused by the
  entry's lease `:person-forgotten`, or `:no-such-lock` on the entry's face
  for a lease leased before the forget. `reader-forget` is the rig's answer
  to the reader's forget (nil if none), `entry-lease` the record of the
  lease the entry cited, as the depot shows it (nil if none)."
  [refusal reader-forget entry-lease]
  (cond
    (not (yes? reader-forget))
    {:ok? false :says (str "refused " (some-> refusal name) " for a reader not forgotten")}

    (= :person-forgotten refusal)
    {:ok? true :kd 10 :says "refused :person-forgotten"}

    (and (= :no-such-lock refusal) (= :no (:answer entry-lease)) (= :person-forgotten (:reason entry-lease)))
    {:ok? true :kd 10 :says "the entry's lease refused :person-forgotten, the entry :no-such-lock on its face"}

    (and (= :no-such-lock refusal) (= :yes (:answer entry-lease))
         (int? (:stamp entry-lease)) (int? (:stamp reader-forget))
         (< (:stamp entry-lease) (:stamp reader-forget)))
    {:ok? true :kd 10 :says "the entry sealed under a lock leased before the forget, :no-such-lock on its face"}

    :else
    {:ok? false :says (str "refused " (some-> refusal name) "; the entry's lease " (answer-str entry-lease))}))

(defn control-refs
  "What a control fact's value refers to, as the rig names it (KD20): the
  request's source and target, the crossing's request and source, a
  forget's target, a revoke's permission. `rig-name` and `rig-fid` map the
  model's names and fact ids; nil where the model's has no counterpart."
  [rn rig-name rig-fid k v]
  (case k
    :promote-request [(rig-fid (:source v)) (rn (:target v))]
    :crossed [(rig-name (:request v)) (rig-fid (:source v))]
    :forget [(rig-fid (:target v))]
    :revoke [(rig-pid rn (:permission v))]
    [v]))

(defn judge-fact
  "One model fact as a model read showed it (`:value` or `:erased-at`),
  against the rig's row for its counterpart. A value must open to the same
  value (its persons renamed) or be erased on both sides; a control fact
  must be there and hold every reference the model's holds, through the
  correspondence (KD20); an erasure's date must be after the value's
  stamp (KD12's relation)."
  [rn refs {:keys [k value erased-at]} row]
  (let [control? (contains? fm/control-keys k)]
    (cond
      (nil? row) {:ok? false :says "no rig row"}
      (:refused row) {:ok? false :says (str "refused " (name (:refused row)))}
      (:absent row) {:ok? false :says "absent in the rig"}
      (contains? row :unreadable) {:ok? false :says (str "unreadable " (pr-str (:unreadable row)))}

      control?
      (let [seen (set (tree-seq coll? seq (:value row)))]
        (if (and (contains? row :value) (every? some? refs) (every? #(contains? seen %) refs))
          {:ok? true :kd 20}
          {:ok? false :says (str "a control fact without the model's references " (pr-str refs) ": " (pr-str (:value row)))}))

      (some? erased-at)
      (cond
        (not (contains? row :erased-at)) {:ok? false :says (str "model erased, rig " (pr-str (select-keys row [:value])))}
        (and (int? (:stamp row)) (int? (:erased-at row)) (<= (:erased-at row) (:stamp row)))
        {:ok? false :says "erased at or before the value's own stamp"}
        :else {:ok? true})

      (contains? row :erased-at) {:ok? false :says (str "model open " (pr-str value) ", rig erased")}

      (= (rig-value rn value) (:value row)) {:ok? true}

      :else {:ok? false :says (str "model " (pr-str (rig-value rn value)) ", rig " (pr-str (:value row)))})))

(def rig-only-keys
  "Fact keys only the rig's own acts carry, which a read that finds them
  does not count as a fact the model lacks: making and settings, grants,
  leases and session closes, persons and their forgets, read entries."
  #{:kind :owner :class :lock-grain :members :permission :lease :session-closed :person :forget-person
    :read/point :read/pattern})

(def ^:private status-rank {:practical 0 :approximated 1})

(defn worse-than-predicted?
  "Whether a played case came out worse than the plan predicted: a case
  predicted practical that needed a hold or a pause."
  [predicted-status status]
  (> (status-rank status 2) (status-rank predicted-status 0)))

;; ========================================================== the play record

(defn fresh-play [c rn w]
  {:case (:id c) :rn rn :w w
   :names {} :acts [] :answers [] :rig-only [] :requests [] :held #{} :paused? false
   :played #{} :forgotten {} :reads [] :approx [] :kd-seen #{} :diffs [] :notes []
   :stopped nil :error nil})

(defn- diff!
  "Record a difference: `kd` names the known difference that explains it;
  without one it is unexplained and fails the test."
  ([p where says] (diff! p where says nil))
  ([p where says kd]
   (swap! p update :diffs conj {:where where :says says :kd kd})
   (when kd (swap! p update :kd-seen conj kd))))

(defn- kd! [p n] (swap! p update :kd-seen conj n))
(defn- note! [p s] (swap! p update :notes conj s))
(defn- approx! [p s] (swap! p update :approx #(if (some #{s} %) % (conj % s))))

(defn- rig-fid-in [s [nm i]] (when-let [r (get-in s [:names nm])] [r (long i)]))
(defn- rig-fid [p fid] (rig-fid-in @p fid))

(defn- judge-and-record!
  "Record the rig's answer beside the model's and judge them."
  [p d rig lease]
  (let [j (judge-answer (:model d) (some-> rig answer-of) (some-> lease answer-of))]
    (swap! p update :answers conj {:model-name (:name d) :model (:model d) :rig (some-> rig answer-of)
                                   :lease (some-> lease answer-of) :judged j})
    (when (:kd j) (kd! p (:kd j)))
    (when-not (:ok? j) (diff! p (str "answer " (:name d)) (:says j)))
    j))

(defn- record-act!
  "The rig's counterpart of a model act: the correspondence, the act for
  the store's judgment, its lease among the rig-only acts (KD2), and its
  answer judged against the model's."
  [p d {:keys [rig lease answer] :as act}]
  (swap! p (fn [s]
             (cond-> (-> s
                         (assoc-in [:names (:name d)] rig)
                         (update :acts conj (assoc act :model (:name d) :kind (:kind d)
                                                   :model-layer (get-in d [:offer :layer]))))
               lease (update :rig-only conj {:what :lease :of (:name d) :name (:name lease) :answer (answer-of lease)}))))
  (when lease (kd! p 2))
  (when (int? (:stamp answer)) (kd! p 12))
  (judge-and-record! p d answer lease))

(defn- no-counterpart!
  "A model decision the rig cannot play, because something it names has no
  rig counterpart: itself a difference, reported, never guessed."
  [p d why]
  (swap! p update :answers conj {:model-name (:name d) :model (:model d) :rig nil
                                 :judged {:ok? false :says (str "no counterpart: " why)}})
  (diff! p (str "answer " (:name d)) (str "no counterpart: " why)))

;; ================================================================= the plays

(def call-ms
  "Every rig call's bound. A door call may itself wait 60 s for a micro
  answer, and lease first, so its bound is two of the waits' 60 s; the
  stream door's ack and its resend loop have none of their own that is
  shorter."
  120000)

(defn bounded-call
  "Run one rig call on its own thread and wait for it at most `call-ms`: a
  call that does not return is that case's 'differs: no answer within
  120 s', never a hung suite. Its exception, if it throws, is its own."
  [what thunk]
  (let [fut (future (thunk))
        v (try (deref fut call-ms ::timeout)
               (catch java.util.concurrent.ExecutionException e (throw (or (.getCause e) e))))]
    (if (= ::timeout v)
      (do (future-cancel fut)
          (throw (ex-info (str "no answer within " (quot call-ms 1000) " s: " what)
                          {::timeout what ::call true})))
      v)))

(defn- f
  "A resolved API as a function whose every call is bounded."
  [env id]
  (let [g (api-fn (:api env) id)]
    (fn [& args] (bounded-call (str (namespace id) "/" (name id)) #(apply g args)))))

(defn- wait-frontier!
  "Wait for the frontier to pass the batch that decided a micro answer, so
  every task has committed it (R5; P3 L2128-2133)."
  [env store a]
  (when-let [b (:batch a)]
    (wait-for (str "the frontier past batch " b)
              #(let [F ((f env :micro/frontier) store)] (and (int? F) (<= b F))))))

(defn- rig-facts
  "A model act's facts as the rig's: entity, key, the value verbatim (its
  persons renamed), and a mark when it has one."
  [rn facts]
  (mapv (fn [x] (cond-> {:e (:e x) :k (:k x) :v (rig-value rn (:v x))}
                  (seq (:mark x)) (assoc :mark (:mark x))))
        facts))

(defn- play-value!
  "A value act, at its decision (KD2): on the stream gate the door leases
  ahead (`stock!`, so the lease act and its answer are seen), then offers
  the act, sealed under the leased locks; on the micro gate the door's
  `write!` leases, seals, offers and waits, and the replay waits for the
  frontier past the act's batch."
  [env store p d]
  (let [{:keys [rn w]} @p
        offer (:offer d)
        who (rn (:who offer))
        layer (:layer offer)
        rl (rn layer)
        pid (cited-pid w rn (:permission offer))
        session (get-in w [:sessions who])
        facts (rig-facts rn (:facts offer))]
    (kd! p 9)
    (if (one-owner-layer? layer)
      (let [lease ((f env :client/stock!) store who rl session (count facts) pid)
            o ((f env :client/build) {:who who :layer rl :class :by-layer :permission pid :session session :facts facts})
            a ((f env :client/offer-until-answered!) store o)]
        (record-act! p d {:rig (:name o) :gate :stream :layer rl :facts facts :offer o :answer a :lease lease}))
      (let [r ((f env :micro/write!) store {:who who :layer rl :class :by-entity :permission pid :session session :facts facts})
            a (:answer r)
            lease (when-let [l (:lease r)] (assoc (answer-of (:answer l)) :name (:name l)))]
        (when (= :no-answer (:answer a)) (throw (ex-info "no answer within 60 s: a group write" {::timeout (:name d)})))
        (when (#{:yes :no} (:answer a)) (wait-frontier! env store a))
        (record-act! p d {:rig (get-in r [:offer :name]) :gate :micro :layer rl :facts facts :offer (:offer r)
                          :answer a :lease lease})))))

(defn- play-forget!
  "A value forget, at its decision: `forget-value!` by the same writer (the
  owner, or the operator in a shared layer), its target through the
  correspondence (KD17: its answer's :how printed)."
  [env store p d]
  (let [{:keys [rn]} @p
        offer (:offer d)
        target (get-in offer [:facts 0 :v :target])
        rfid (rig-fid p target)
        rl (rn (:layer offer))]
    (if-not rfid
      (no-counterpart! p d (str "its target " (fm/fid-str target) " has none"))
      (let [a ((f env :client/forget-value!) store (rn (:who offer)) rl rfid)]
        (when (:how a) (kd! p 17) (note! p (str (:name d) " :how " (name (:how a)))))
        (record-act! p d {:rig (:name a) :gate :stream :layer rl
                          :facts [{:k :forget :v {:target rfid}}] :answer a})))))

(defn- play-revoke!
  "The operator's revoke, at its decision, in the mapped permission's layer
  through that layer's gate, standing on its grant (the client's
  `revoke-offer`, the micro client's for a by-entity layer)."
  [env store p d]
  (let [{:keys [rn]} @p
        pid (get-in d [:offer :facts 0 :v :permission])
        rpid (rig-pid rn pid)
        in (nth rpid 2)
        micro? (= :by-entity (:class ((f env :micro/settings-of) store in)))
        o (if micro? ((f env :micro/revoke-offer) store rpid) ((f env :client/revoke-offer) store rpid))
        a (if micro? ((f env :micro/offer!) store o) ((f env :client/offer-until-answered!) store o))]
    (kd! p 9)
    (when (and micro? (#{:yes :no} (:answer a))) (wait-frontier! env store a))
    (record-act! p d {:rig (:name o) :gate (if micro? :micro :stream) :layer in :facts (:facts o) :offer o :answer a})))

(defn- play-person-forget!
  "A person forget (KD7): the operator's forget-person! act in :people,
  answered, then its fan-out waited for until every task's entry holds no
  lock (the ack covers the fan-out; `person-on-task` checks it)."
  [env store p {:keys [person]}]
  (let [{:keys [rn]} @p
        rp (rn person)
        a ((f env :client/forget-person!) store rp)]
    (kd! p 7)
    (swap! p #(-> %
                  (update :rig-only conj {:what :forget-person :of (name person) :name (:name a) :answer (answer-of a)})
                  (assoc-in [:forgotten rp] (answer-of a))))
    (if-not (yes? a)
      (diff! p (str "forget of " (name person)) (str "refused: " (answer-str a)))
      (wait-for (str "the fan-out of " (name rp) "'s forget to every task")
                (fn [] (every? #(let [e ((f env :client/person-on-task) store rp %)]
                                  (and (map? e) (nil? (:lock e)) (some? (:erased-at e))))
                               fan-keys))))))

;; ---- promotion: phase 4 is not built; each call's shape is a binding to confirm

(defn call-lease-landing!
  "Phase 4's landing lease (KD3; P4 L956: 'lease-landing! (the lease act and
  the public key)', no argument list given). Assumed: a spec map naming the
  writer, the target layer, the session, the permission there and the
  request's name, answered like any act. To confirm at wave 2."
  [env store spec]
  ((f env :client/lease-landing!) store spec))

(defn call-promote!
  "Phase 4's request (P4 L956-957: 'promote! (the request; its answer with
  the crossing's and the statements)'). Assumed: a spec map with the
  request's name and claimed-when given by the caller, so a resend rebuilds
  the same map and is answered from the record; the answer the request's,
  or a map holding it under :request. To confirm at wave 2."
  [env store spec]
  (let [a ((f env :client/promote!) store spec)]
    (if (map? (:request a)) (:request a) a)))

(defn call-promotion-status
  "Phase 4's status (P4 L480, `promotion-status [*layer *req *as-of]`).
  Assumed: the client function `[store layer req as-of]`, nil as-of for
  now, answering a map with :status (or the keyword). To confirm."
  [env store layer req]
  (let [r ((f env :client/promotion-status) store layer req nil)]
    (if (keyword? r) r (:status r))))

(defn landing-name-of
  "The landing's rig name, by the store's own function: phase 4's
  `landing-name` takes the class (P4's changes to stage 1), phase 1's does
  not; the 3-arity is tried first."
  [env req target]
  (let [ln (f env :env/landing-name)]
    (try (ln req target :by-entity)
         (catch clojure.lang.ArityException _ (ln req target)))))

(defn- decided-in? [step model-name]
  (boolean (some #(= model-name (:name %)) (concat (:decided step) (:prepared step)))))

(defn- decided-later?
  "Whether the model decides `model-name` in an op after `step`: the rule
  for holding a continuation (the plan: 'when the model decides a
  continuation in a later op than the part before it'). A continuation the
  model never decides (a read-out refused, so no landing) is not held."
  [p step model-name]
  (boolean (some #(and (> (:index %) (:index step)) (decided-in? % model-name)) (:steps @p))))

(defn- pause! [env p]
  (rtest/pause-microbatch-topology! (:ipc env) (module-name) micro-topology)
  (swap! p assoc :paused? true))

(defn- resume! [env p]
  (when (:paused? @p)
    (rtest/resume-microbatch-topology! (:ipc env) (module-name) micro-topology)
    (swap! p assoc :paused? false)))

(defn- hold! [env p point req]
  ((f env :inject/hold!) point req)
  (swap! p update :held conj [point req]))

(defn- release! [env p point req]
  ((f env :inject/release!) point req)
  (swap! p update :held disj [point req]))

(defn- hold-landing!
  "Hold a landing to the model's later op: pause the micro topology, so the
  forward waits in *micro-offers as the model's waits in its inbox; if
  pausing is not usable, phase 4's :before-forward hold (the plan's
  fallback, where KD5 shows)."
  [env p req]
  (try
    (pause! env p)
    (approx! p "the paused micro topology stands for the model's queued landing")
    (catch Throwable t
      (note! p (str "pausing the micro topology failed (" (ex-message t) "): the :before-forward hold instead"))
      (hold! env p :before-forward req)
      (approx! p "the hold at :before-forward and a resend stand for the model's queued landing (KD5)"))))

(defn- request-of [p model-req] (some #(when (= model-req (:model %)) %) (:requests @p)))

(defn- play-request!
  "Alice's promotion request, at its decision: the landing lease in the
  target, answered (KD3); the hold at :before-read-out armed when the model
  decides the read-out in a later op, or the landing held when only the
  landing is later; then `promote!` naming the mapped source and target."
  [env store p step d]
  (let [{:keys [rn w]} @p
        offer (:offer d)
        nm (:name d)
        {:keys [source target landing-permission]} (:promote offer)
        who (rn (:who offer))
        rl (rn (:layer offer))
        rt (rn target)
        session (get-in w [:sessions who])
        rsrc (rig-fid p source)]
    (if-not rsrc
      (no-counterpart! p d (str "its source " (fm/fid-str source) " has none"))
      (let [req ((f env :env/make-name) rl :by-layer)
            spec {:name req :who who :layer rl :session session
                  :permission (cited-pid w rn (:permission offer))
                  :source rsrc :target rt
                  :landing-permission (cited-pid w rn landing-permission)
                  :claimed-when (System/currentTimeMillis)}
            ll (call-lease-landing! env store {:who who :layer rt :session session
                                               :permission (:landing-permission spec) :request req})
            _ (swap! p #(-> %
                            (update :rig-only conj {:what :landing-lease :of nm :name (:name ll) :answer (answer-of ll)})
                            (update :requests conj {:model nm :rig req :layer rl :target rt :spec spec})))
            _ (kd! p 3)
            _ (kd! p 9)
            _ (when-not (yes? ll) (diff! p (str "landing lease for " nm) (str "refused: " (answer-str ll))))
            cross-later? (decided-later? p step (fm/crossing-name nm))
            land-later? (decided-later? p step (fm/landing-name nm))]
        (cond
          cross-later? (do (hold! env p :before-read-out req)
                           (approx! p "the hold at :before-read-out and a resend stand for the read-out's own step"))
          land-later? (hold-landing! env p req))
        (let [a (call-promote! env store spec)]
          (record-act! p d {:rig req :gate :stream :layer rl
                            :facts [{:k :promote-request :v {:source rsrc :target rt}}] :answer a}))))))

(defn- resend-request!
  "The door's resend of a held request, answered from the record, which
  continues the promotion from where the hold stopped it (PR15)."
  [env store p {:keys [spec model]}]
  (let [a (call-promote! env store spec)]
    (note! p (str "the resend of " model " answered " (answer-str a)))
    a))

(defn- play-crossing!
  "The read-out's decision (KD4): if its request was held at
  :before-read-out, the release and the door's resend (and the landing
  held first when the model decides it later still); then the crossing's
  answer, by its derived name in the source layer."
  [env store p step d]
  (let [req-model (get-in d [:offer :read-out :request])
        r (request-of p req-model)]
    (if-not r
      (no-counterpart! p d (str "its request " req-model " has none"))
      (let [rig (:rig r)]
        (when (contains? (:held @p) [:before-read-out rig])
          (when (decided-later? p step (fm/landing-name req-model)) (hold-landing! env p rig))
          (release! env p :before-read-out rig)
          (resend-request! env store p r))
        (let [cn ((f env :env/crossing-name) rig)
              a (wait-for "the crossing's answer" #((f env :client/record) store cn))]
          (kd! p 4)
          (record-act! p d {:rig cn :gate :stream :layer (:layer r) :facts nil :answer a}))))))

(defn- play-landing!
  "The landing's decision: the held landing let go (the micro topology
  resumed, or the :before-forward hold released and the request resent),
  then its answer by its derived name, and the frontier past its batch
  (KD13: done is read only once the landing is settled)."
  [env store p step d]
  (let [{:keys [rn]} @p
        req-model (:because-of (:offer d))
        r (request-of p req-model)]
    (if-not r
      (no-counterpart! p d (str "its request " req-model " has none"))
      (let [rig (:rig r)
            ln (landing-name-of env rig (:target r))]
        (resume! env p)
        (when (contains? (:held @p) [:before-forward rig])
          (release! env p :before-forward rig)
          (resend-request! env store p r)
          (kd! p 5))
        (let [a (wait-for "the landing's answer"
                          #(let [x ((f env :micro/lookup) store ln nil nil)]
                             (when (and (map? x) (#{:yes :no} (:answer x))) x)))
              src (fm/fact-by-id (:after step) (get-in d [:offer :source]))]
          (wait-frontier! env store a)
          (kd! p 13)
          (record-act! p d {:rig ln :gate :micro :layer (:target r)
                            :facts [{:e (:e src) :k (:k src) :v (rig-value rn (:v src))}] :answer a}))))))

(defn- check-commit!
  "[F7] A name played at [:prepare] gains its recorded answer at [:commit]:
  checked, not sent again. The model's recorded answer must be the one it
  prepared; the rig's was decided and committed together (D1's
  approximation)."
  [p d]
  (let [prepared (some #(when (= (:name d) (:model-name %)) (:model %)) (:answers @p))]
    (when (and prepared (not= (select-keys prepared [:answer :reason]) (select-keys (:model d) [:answer :reason])))
      (diff! p (str "commit " (:name d)) (str "the model committed " (answer-str (:model d))
                                               " where it prepared " (answer-str prepared))))))

(defn- play-decision! [env store p step d]
  (case (:kind d)
    :value (play-value! env store p d)
    :forget (play-forget! env store p d)
    :revoke (play-revoke! env store p d)
    :request (play-request! env store p step d)
    :crossing (play-crossing! env store p step d)
    :landing (play-landing! env store p step d)))

;; ================================================================= the reads

(defn seed-act? [nm] (str/starts-with? (str nm) "grant:"))

(defn model-read-facts
  "The case's own facts a model read showed, each with its key and entity:
  every fact but the first facts' grants (KD11: never the seed's)."
  [st read]
  (vec (for [x (:facts read)
             :when (not (seed-act? (:act x)))
             :let [full (fm/fact-by-id st (:id x))]]
         (assoc x :k (:k full) :e (:e full)))))

(defn- shared-read? [env] (contains? (get (:api env) :shared/moment) :value))

(defn- below-exit
  "A read below the exit, as the store's view (the read exit's own query,
  no entry): a point read of fids, or a pattern read."
  [store query layer for & args]
  (bounded-call (str (name query) " below the exit")
                #(apply rama/foreign-invoke-query (get store query) layer for args)))

(defn- entry-lease
  "The record of the lease a refused entry cited, found through the depot:
  the entry's record by its name, the lease name in its facts' lock ids,
  that lease's answer by name (F3's evidence)."
  [env store entry-name]
  (when entry-name
    (let [recs ((f env :client/depot-records) store)
          rec (last (filter #(= entry-name (:name %)) recs))
          lid (some :lock-id (:facts rec))]
      (when (vector? lid) ((f env :client/record) store (first lid))))))

(defn- live-member
  "[F4] The group's reader through the exit: Alice unless the rig forgot her,
  else Bob, a member not forgotten, in his rig-only working layer."
  [p]
  (let [{:keys [rn forgotten]} @p]
    (some #(when-not (yes? (get forgotten (rn %))) (rn %)) [:alice :bob])))

(defn- extra-rows
  "[F2] Rows a read found that are neither a counterpart of a model fact in
  this read nor a rig-only kind: each is a fact the rig holds and the model
  does not."
  [rows known]
  (vec (remove #(or (contains? known (:fid %)) (contains? rig-only-keys (:k %)) (:absent %)) rows)))

(defn- exit-read!
  "One read through the exit, its entry counted (KD6); a refusal judged by
  KD10's rule, with the store's view beside it (a point read below the
  exit)."
  [env store p {:keys [reader working wpid layer fids]}]
  (let [r ((f env :exit/read!) store {:reader reader :for reader :reader-kind :person :working working
                                      :permission wpid :layer layer :read [:point fids] :as-of nil})]
    (if (contains? r :rows)
      (do (kd! p 6)
          (swap! p update :rig-only conj {:what :entry :of (str "read of " (name layer)) :name (:entry r)
                                          :answer {:answer :yes :stamp (:entry-stamp r)}})
          {:road :exit :reader reader :rows (into {} (map (juxt :fid identity)) (:rows r))})
      (let [j (judge-exit-refusal (:refused r) (get-in @p [:forgotten reader])
                                  (try (entry-lease env store (:entry r)) (catch Throwable _ nil)))
            view (try (below-exit store :read-point layer reader fids nil) (catch Throwable t {:refused (ex-message t)}))]
        (when (:entry r)
          (kd! p 6)
          (swap! p update :rig-only conj {:what :entry :of (str "read of " (name layer)) :name (:entry r)
                                          :answer {:answer :no :reason (:refused r)} :kd (:kd j)}))
        (if (:ok? j)
          (diff! p (str "read of " (name layer) " for " (name reader)) (:says j) 10)
          (diff! p (str "read of " (name layer) " for " (name reader)) (:says j)))
        {:road :exit :reader reader :refused (:refused r) :judged j
         :store-view (into {} (map (juxt :fid identity)) (:rows view))
         :store-view-refused (:refused view)}))))

(defn- open-act-rows
  "Phase 3's frontier read of one group act: its rows on each entity it
  touches, opened through one F, as {index row}."
  [env store {:keys [rig facts]}]
  (into {} (for [e (distinct (map :e facts))
                 :let [idxs (vec (keep-indexed (fn [i x] (when (= e (:e x)) i)) facts))
                       rows ((f env :micro/open-act) store e rig)]
                 [j row] (map-indexed vector rows)
                 :when (< j (count idxs))]
             [(nth idxs j) row])))

(defn- read-layer!
  "The rig's side of one model read in one layer: the counterparts of the
  model's facts there, read as a live reader through the exit (a one-owner
  layer by its owner; the group through the exit once phase 5's rest
  resolves, else phase 3's frontier read), and every fact the rig holds
  there that the model does not [F2]."
  [env store p l facts]
  (let [{:keys [rn w]} @p
        rl (rn l)
        pairs (mapv (fn [x] [x (rig-fid p (:id x))]) facts)
        known (set (keep second pairs))]
    (if (one-owner-layer? l)
      (let [owner (rn (get-in fm/layers [l :owner]))
            [working wpid] (get-in w [:working owner])
            fids (vec known)
            seen (if (seq fids)
                   (exit-read! env store p {:reader owner :working working :wpid wpid :layer rl :fids fids})
                   {:road :exit :reader owner :rows {}})
            pattern (try (below-exit store :read-pattern rl owner [:all] nil 10000) (catch Throwable t {:refused (ex-message t)}))
            extra (extra-rows (:rows pattern) known)]
        (when (:refused pattern) (diff! p (str "pattern read of " (name l)) (str "refused " (pr-str (:refused pattern)))))
        (doseq [x extra] (diff! p (str "read of " (name l)) (str "a fact the model does not have: " (pr-str (select-keys x [:fid :k :value :erased-at])))))
        (assoc seen :layer l :pairs pairs :extra extra))
      (let [group-acts (filter #(and (= rl (:layer %)) (yes? (answer-of (:answer %)))) (:acts @p))
            all-fids (vec (distinct (concat known (for [a group-acts [i _] (map-indexed vector (:facts a))] [(:rig a) (long i)]))))
            seen (if (shared-read? env)
                   ;; [F4] a live member; with none left, Alice, whose refusal KD10 explains
                   (let [reader (or (live-member p) (rn :alice))
                         [working wpid] (get-in w [:working reader])]
                     (if (and reader (seq all-fids))
                       (exit-read! env store p {:reader reader :working working :wpid wpid :layer rl :fids all-fids})
                       {:road :exit :reader reader :rows {}}))
                   {:road :frontier
                    :rows (into {} (for [a group-acts [i row] (open-act-rows env store a)] [[(:rig a) i] (assoc row :fid [(:rig a) i])]))})
            rows (or (:rows seen) (:store-view seen))
            extra (vec (for [[fid row] rows
                             :when (and (not (contains? known fid))
                                        (not (contains? rig-only-keys (get-in (some #(when (= (:rig %) (first fid)) %) group-acts) [:facts (second fid) :k]))))]
                         (assoc row :fid fid)))]
        (doseq [x extra] (diff! p (str "read of " (name l)) (str "a fact the model does not have: " (pr-str (select-keys x [:fid :value :erased-at])))))
        (assoc seen :layer l :pairs pairs :extra extra)))))

(defn- compare-read!
  "Each model fact of one layer against the rig's row: through the exit's
  rows, or, where the exit refused, the store's view beside it (printed
  as such; the refusal itself judged by KD10)."
  [p l obs]
  (let [{:keys [rn]} @p
        rows (or (:rows obs) (:store-view obs) {})
        s @p
        rname #(get-in s [:names %])
        rfid #(rig-fid-in s %)]
    (vec (for [[x fid] (:pairs obs)
               :let [row (when fid (get rows fid))
                     refs (when (contains? fm/control-keys (:k x)) (control-refs rn rname rfid (:k x) (:value x)))
                     j (if fid (judge-fact rn refs x row) {:ok? false :says "no counterpart"})]]
           (do (when (:kd j) (kd! p (:kd j)))
               (when-not (:ok? j)
                 (diff! p (str "read of " (name l) " " (fm/fid-str (:id x))) (:says j)))
               {:model-fid (:id x) :rig-fid fid :model (select-keys x [:value :erased-at]) :row row :judged j})))))

(defn- play-read!
  "A model read: in each layer its case facts are in, the rig's read and
  the comparison, and each request the replay has sent so far with its
  status [F1] (`:shown` comes from these, reader-independent)."
  [env store p step {:keys [read]}]
  (let [st (:after step)
        facts (model-read-facts st read)
        label (if (= :closing-read (first (:op step))) "closing" (str (inc (count (:reads @p)))))
        ;; the layers the model's facts are in, and every layer the rig wrote into, so a
        ;; fact the rig holds where the model has none is read too [F2]
        layers (vec (for [l (distinct (concat (map :layer facts) (keep :model-layer (:acts @p))))
                          :let [obs (read-layer! env store p l (filterv #(= l (:layer %)) facts))]]
                      (assoc obs :compared (compare-read! p l obs))))
        promos (vec (for [{:keys [model rig layer]} (:requests @p)]
                      {:model model
                       :model-status (some #(when (= model (:request %)) (:status %)) (:promotions read))
                       :rig-status (call-promotion-status env store layer rig)}))]
    (kd! p 11)
    (doseq [{:keys [model model-status rig-status]} promos
            :when (not= model-status rig-status)]
      (diff! p (str "read " label " promotion " model) (str "model " (some-> model-status name) ", rig " (some-> rig-status name))))
    (swap! p update :reads conj {:label label :layers layers :promotions promos})))

;; ================================================ the store's judgment (:values)

(defn- open-rig-fact
  "The store's own opening of one rig fact, whoever reads (RP3): phase 2's
  `opens?` for a stream-side layer, phase 3's frontier read for a group
  row."
  [env store {:keys [gate layer rig] :as act} i]
  (if (= :stream gate)
    ((f env :client/opens?) store layer [rig (long i)])
    (get (open-act-rows env store act) i)))

(defn rig-values!
  "[F1] Each expected ref's status in the rig, found from the rig's own
  records: among the acts it admitted (sent by the replay, or a landing the
  store made), the facts in the ref's layer with its key and the persons
  the replay sent; opened by the store; missing when no admitted act
  carries one. A stream-side layer is also read by pattern below the exit,
  and a matching fact the replay does not know of is a difference."
  [env store p expect]
  (let [{:keys [rn]} @p]
    (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
          (for [[[layer k persons :as ref] _] (:values expect)]
            (let [rl (rn layer)
                  rp (when persons (into #{} (map rn) persons))
                  cands (vec (for [act (:acts @p)
                                   :when (and (= rl (:layer act)) (yes? (answer-of (:answer act))))
                                   [i x] (map-indexed vector (:facts act))
                                   :when (and (= k (:k x)) (= rp (get-in x [:v :persons])))]
                               [act i x]))
                  known (set (for [[act i] cands] [(:rig act) (long i)]))]
              (when (one-owner-layer? layer)
                (let [owner (rn (get-in fm/layers [layer :owner]))
                      r (try (below-exit store :read-pattern rl owner [:k k] nil 10000) (catch Throwable t {:refused (ex-message t)}))]
                  (doseq [row (:rows r)
                          :when (and (= rp (get-in row [:value :persons])) (not (contains? known (:fid row))))]
                    (diff! p (str "values " (pr-str ref)) (str "a fact the replay did not make: " (pr-str (select-keys row [:fid :value])))))))
              (when (> (count cands) 1)
                (diff! p (str "values " (pr-str ref)) (str (count cands) " rig facts where the model has one")))
              [ref (if-let [[act i x] (first cands)]
                     (let [o (open-rig-fact env store act i)]
                       (cond
                         (contains? o :value) (do (when (not= (:v x) (:value o))
                                                    (diff! p (str "values " (pr-str ref)) (str "opens to " (pr-str (:value o)) " where " (pr-str (:v x)) " was sent")))
                                                  :open)
                         (contains? o :erased-at) :erased
                         :else (do (diff! p (str "values " (pr-str ref)) (str "does not open: " (pr-str o))) :unreadable)))
                     :missing)])))))

(defn rig-seen
  "The rig's `seen`, as `play` shapes the model's: its values, and the
  statuses its reads showed."
  [p values expect]
  (cond-> {:values (into {} values)}
    (:shown expect) (assoc :shown (vec (for [r (:reads @p) pr (:promotions r)] (:rig-status pr))))))

;; ============================================================ one case, played

(defn- run-seed!
  "The seed's acts, every one a rig-only act that must answer yes [F11]; a
  refusal stops the case as 'differs: seed act refused', with its name and
  reason."
  [env store p first?]
  (doseq [[label call] (seed-steps (:api env) store (:w @p) first?)
          :while (not (:stopped @p))]
    (let [as (answers-in (bounded-call label call))
          bad (remove yes? as)]
      (swap! p update :rig-only conj {:what :seed :of label :answers (mapv answer-of as)})
      (kd! p 8)
      (when (str/starts-with? label "person ") (kd! p 7))
      (when (seq bad)
        (swap! p assoc :stopped (str "seed act refused: " label ", " (str/join "; " (map answer-str bad))))
        (diff! p "seed" (str label " refused: " (str/join "; " (map answer-str bad))))))))

(defn- play-step!
  "One model op's effects on the rig, in the model's order: the decisions
  of a [:prepare] from its delta [F7], then the decisions it recorded (a
  name played at [:prepare] only checked), then person forgets, then reads."
  [env store p step]
  (doseq [d (:prepared step)]
    (approx! p "prepare and commit are one batch in the rig; its commit is a check")
    (play-decision! env store p step d)
    (swap! p update :played conj (:name d)))
  (doseq [d (:decided step)]
    (if (contains? (:played @p) (:name d))
      (check-commit! p d)
      (play-decision! env store p step d)))
  (doseq [x (:forgets step)] (play-person-forget! env store p x))
  (doseq [r (:reads step)] (play-read! env store p step r)))

(defn- cleanup!
  "However a case ends: every hold let go, the micro topology resumed, so
  the next case cannot start paused."
  [env p]
  (doseq [[point req] (:held @p)]
    (try ((f env :inject/release!) point req) (catch Throwable _ nil)))
  (try (resume! env p) (catch Throwable _ nil)))

(defn play-case!
  "One practical case through the rig in lockstep with the model's run `ls`
  (under baseline). On the main road a fresh module is launched for it and
  destroyed after. Returns the play record with the rig's `seen`."
  [env c ls]
  (let [{:keys [ipc road]} env
        rn (if (= :one-module road) (fallback-names (:id c)) main-road-names)
        p (atom (assoc (fresh-play c rn (world rn)) :steps (:steps ls)))
        t0 (System/nanoTime)]
    (try
      ((f env :inject/reset-all!))
      (when (= :fresh road) (launch! ipc))
      (let [store (connect (:api env) ipc)]
        (run-seed! env store p (:first? env))
        (when-not (:stopped @p)
          (doseq [step (:steps ls) :while (not (:stopped @p))]
            (play-step! env store p step))
          (let [values (rig-values! env store p (:expect c))]
            (swap! p assoc :seen (rig-seen p values (:expect c))))))
      (catch Throwable t
        (let [timeout (::timeout (ex-data t))]
          (when (::call (ex-data t)) (swap! p assoc :stuck-call timeout))
          (swap! p assoc :error (if timeout (ex-message t) (str (.getSimpleName (class t)) ": " (ex-message t))))
          (diff! p "the play" (if timeout (ex-message t)
                                  (str "error " (.getSimpleName (class t)) ": " (ex-message t)
                                       (when-let [st (first (.getStackTrace t))] (str " at " st)))))))
      (finally
        (cleanup! env p)
        (when (= :fresh road) (try (destroy! ipc) (catch Throwable _ nil)))))
    (assoc (dissoc @p :steps) :ms (ms-since t0))))

;; ============================================================== the verdicts

(defn prepare-case
  "A case before any cluster: the model's lockstep under baseline, its
  `seen`, and whether it is practical (every API it needs resolved, every
  op covered by a row, every decision's order known). Nothing of a case
  that is not practical is played."
  [resolved c]
  (let [ls (lockstep fm/baseline (:history c))
        missing (case-missing resolved (:id c))
        refusal (or (history-refusal (:history c)) (guard-refusal ls) (effect-refusal ls))]
    {:id (:id c) :case c :ls ls
     :model-seen (seen-of (:end ls) (:expect c))
     :missing missing :refusal refusal
     :not-practical (cond (seq missing) (missing-str missing) refusal refusal)}))

(defn judge-case
  "A case's status, outcome and what fails the test (PLAN-replays.md, 'The
  test's verdict'): a baseline `:values` or `:shown` difference, any
  difference no known difference explains, a status worse than predicted
  once its stages resolved, a namespace it needs that does not load. A case
  not practical only because its stage has not merged fails nothing."
  [r]
  (let [{:keys [play missing refusal]} r
        c (:case r)]
    (if (:not-practical r)
      (let [broken (filter :broken missing)]
        (assoc r :status :not-practical
               :fails (cond-> []
                        (seq broken) (conj (str "a namespace it needs is there and does not load: "
                                                (str/join ", " (map :var broken))))
                        (and refusal (empty? missing)) (conj (str "not practical with its stages resolved: " refusal)))))
      (let [unexplained (vec (remove :kd (:diffs play)))
            status (if (seq (:approx play)) :approximated :practical)
            same? (= (:seen play) (:model-seen r))
            outcome (if (and same? (empty? unexplained)) :as-said :differs)]
        (assoc r :status status :outcome outcome
               :unexplained unexplained
               :not-seen (set/difference (predicted-kds (:id r)) (:kd-seen play))
               :fails (cond-> []
                        (not same?) (conj (str "seen differs from the model's: rig " (pr-str (:seen play))
                                               ", model " (pr-str (:model-seen r))))
                        (seq unexplained) (conj (str (count unexplained) " difference(s) no known difference explains"))
                        (worse-than-predicted? (get-in c [:predicted :status]) status)
                        (conj (str "worse than predicted: " (name status) " where the plan predicts "
                                   (name (get-in c [:predicted :status]))))))))))

;; ================================================================ the report

(defn- ref-str [ref] (#'fsc/ref-str ref))

(defn- seen-diff-str
  "Where two `seen`s differ, per value and in the statuses."
  [rig other]
  (str/join "; " (concat (for [[ref v] (:values rig) :let [o (get-in other [:values ref])] :when (not= v o)]
                           (str "rig " (ref-str ref) " " (some-> v name) ", this configuration " (some-> o name)))
                         (when (not= (:shown rig) (:shown other))
                           [(str "rig shown " (str/join " " (map #(some-> % name) (:shown rig)))
                                 ", this configuration " (str/join " " (map #(some-> % name) (:shown other))))]))))

(defn- obs-str
  "One row as a read line shows it."
  [row]
  (cond
    (nil? row) "none"
    (:absent row) "absent"
    (contains? row :erased-at) "erased"
    (contains? row :unreadable) (str "unreadable " (pr-str (:unreadable row)))
    (map? (:value row)) (or (get-in row [:value :token]) (pr-str (:value row)))
    :else (pr-str (:value row))))

(defn- read-str [rd]
  (str (:label rd) ": "
       (str/join ", " (concat
                       (for [lr (:layers rd)]
                         (str (name (:layer lr))
                              (case (:road lr) :frontier " (frontier)" "")
                              (when (:reader lr) (str " as " (name (:reader lr))))
                              (if (:refused lr)
                                (str " refused " (name (:refused lr)) "; the store's view "
                                     (str/join " " (for [c (:compared lr)] (str (fm/fid-str (:model-fid c)) " " (obs-str (:row c))))))
                                (str " " (str/join " " (for [c (:compared lr)] (str (fm/fid-str (:model-fid c)) " " (obs-str (:row c)))))))))
                       (for [pr (:promotions rd)] (str (:model pr) " " (some-> (:rig-status pr) name)))))))

(defn- rig-lines
  "The rig's line under baseline, and its detail lines."
  [r]
  (let [pad "              "
        play (:play r)]
    (if (= :not-practical (:status r))
      (cond-> [(str "      rig:    not practical (" (:not-practical r) ")")
               (str pad "predicted " (name (get-in r [:case :predicted :status])) ", as said; known when run: "
                    (str/join " " (map kd-str (predicted-kds (:id r)))))]
        (seq (:fails r)) (into (for [x (:fails r)] (str pad "FAILS    " x))))
      (let [lead (cond
                   (:error play) (str "differs: " (:error play))
                   (:stopped play) (str "differs: " (:stopped play))
                   :else (str (when (= :approximated (:status r)) (str "approximated (" (str/join "; " (:approx play)) "); "))
                              (if (= :as-said (:outcome r)) "as said" "differs")))
            as (:answers play)
            ok (count (filter #(get-in % [:judged :ok?]) as))
            by-what (frequencies (map :what (:rig-only play)))]
        (-> [(str "      rig:    " lead)
             (str pad "answers  " ok " of " (count as) " as the model's ("
                  (str/join ", " (for [a as] (str (:model-name a) " " (answer-str (:rig a))
                                                  (when-let [k (get-in a [:judged :kd])] (str " " (kd-str k)))))) ")")
             (str pad "reads    " (str/join " | " (map read-str (:reads play))))
             (str pad "values   " (str/join ", " (for [[ref v] (get-in play [:seen :values])]
                                                   (str (ref-str ref) " " (some-> v name) " (model "
                                                        (some-> (get-in r [:model-seen :values ref]) name) ")")))
                  (when-let [s (get-in play [:seen :shown])] (str "; shown " (str/join " " (map #(some-> % name) s))
                                                                  " (model " (str/join " " (map name (get-in r [:model-seen :shown]))) ")")))
             (str pad "rig-only " (str/join ", " (for [[w n] (sort-by key by-what)] (str (name w) " " n))))
             (str pad "known    " (str/join " " (map kd-str (sort (:kd-seen play))))
                  (when (seq (:not-seen r)) (str "; predicted, not seen: " (str/join " " (map kd-str (sort (:not-seen r)))))))
             (str pad "roads    permissions " (name door-road) "; stream door stock! then offer!; micro door write!; "
                  (if (some #(= :frontier (:road %)) (mapcat :layers (:reads play)))
                    "the group read through phase 3's frontier" "the group read through the exit")
                  "; " (:ms play) " ms")]
            (into (for [x (:notes play)] (str pad "note     " x)))
            (into (for [d (:diffs play) :when (:kd d)] (str pad "known    " (:where d) ": " (:says d) " (" (kd-str (:kd d)) ")")))
            (into (for [d (:diffs play) :when (not (:kd d))] (str pad "DIFFERS  " (:where d) ": " (:says d))))
            (into (for [x (:fails r)] (str pad "FAILS    " x))))))))

(def objections-note
  "; the objections above are the model's own properties (they use its :order, which the rig has no counterpart of), not the rig's")

(defn- other-config-line
  "The rig's line under a configuration other than baseline: the rig
  implements baseline, so its outcome is baseline's, set beside this
  configuration's."
  [r cfg-seen objections?]
  (str "      rig:    "
       (cond
         (= :not-practical (:status r)) "not practical (as under baseline)"
         (nil? (get-in r [:play :seen])) "as under baseline (it did not finish)"
         (= (get-in r [:play :seen]) cfg-seen) "as under baseline; agrees with this configuration"
         :else (str "as under baseline; differs from this configuration: " (seen-diff-str (get-in r [:play :seen]) cfg-seen)))
       (when objections? objections-note)))

(defn- sh-out [& args]
  (try (str/trim (:out (apply sh/sh args))) (catch Throwable _ "?")))

(def f10-lines
  "[F10] What the lockstep does not test, in the report's header."
  ["1. no rig offer is in flight while another is decided, except under a hold or the pause, so the rig's own order for offers queued together is not exercised (on the micro side that is KD16);"
   "2. in the rig a read-out runs in its request's own event (P4 PR7), so B1's and B2's order exists in the rig only on the recovery road after a crash, which the hold and the resend play;"
   "3. the door's retry and timeout behaviour, since every rig act is awaited."])

(defn report-lines
  "The whole report (PLAN-replays.md, 'The report')."
  [{:keys [started ms resolved check results error]}]
  (let [played (remove #(= :not-practical (:status %)) results)
        stage-line (fn [stage]
                     (let [as (filter #(= stage (:stage %)) apis)
                           rs (map #(get resolved (:id %)) as)
                           ok (count (filter #(contains? % :value) rs))]
                       (str "stage " stage ": " (if (= ok (count as)) (str "all " ok " resolved") (str ok " of " (count as) " resolved")))))]
    (concat
     ["Phase 8 replays: the model's fixed histories through the rig (PLAN-replays.md, RP8)"
      (str "run      " started (when ms (str ", " (quot ms 1000) " s")))
      (str "rig      " (sh-out "git" "rev-parse" "--short=8" "HEAD") " on " (sh-out "git" "rev-parse" "--abbrev-ref" "HEAD")
           (when (seq (sh-out "git" "status" "--porcelain" "--" ".")) ", with uncommitted changes in the rig folder"))
      (str "model    formal-model-2026-09-24 at " (sh-out "git" "log" "-1" "--format=%h" "--" "../formal-model-2026-09-24")
           "; the rig is compared under baseline")
      (str "cluster  in-process, " (:tasks module-options) " tasks, " (:threads module-options) " threads; road: "
           (case (:road check) :fresh "a fresh module per case" :one-module "one module, a fresh world per case (the fallback)"
                 :none "none (the module did not launch)" "not checked")
           (when check (str " (launch " (:launch-ms check) " ms, destroy " (:destroy-ms check) " ms, relaunch "
                            (:relaunch-ms check) " ms" (when (:forced check) ", forced by RIG_REPLAY_ROAD")
                            (when (:error check) (str ", error: " (:error check))) ")")))
      (str "stages   " (if resolved (str/join "; " (map stage-line stage-order)) "not resolved"))
      (str "summary  " (count results) " cases: " (count played) " played ("
           (count (filter #(= :as-said (:outcome %)) played)) " as said, "
           (count (filter #(= :differs (:outcome %)) played)) " differ), "
           (count (filter #(= :not-practical (:status %)) results)) " not practical; "
           (count (filter #(seq (:fails %)) results)) " failing")
      "not tested by the lockstep (F10):"]
     (map #(str "  " %) f10-lines)
     (when error [(str "RUN ERROR " error)])
     [""
      "Known differences, reported every run (KD1 to KD20; a case line names those that showed):"]
     (for [kd known-differences]
       (str "  " (kd-str (:n kd)) " [" (name (:group kd)) "] " (:title kd)
            (if (seq (:cases kd))
              (str "; cases " (str/join " " (filter (:cases kd) case-ids)))
              (str "; not exercised: " (:why kd)))
            " (" (:source kd) ")"))
     (mapcat (fn [set-id]
               (let [{:keys [title configs checks]} (named-configs set-id)
                     rs (filter #(= set-id (get-in % [:case :set])) results)]
                 (concat
                  ["" (str "== " title)]
                  (mapcat (fn [[cname cfg]]
                            (concat
                             [(str "  under " (name cname))]
                             (mapcat (fn [r]
                                       (let [[_ cfg-seen objections] (fsc/play cfg (:case (:case r)) checks)]
                                         (concat (model-lines cname cfg (:case r) checks)
                                                 (if (= :baseline cname)
                                                   (cond-> (rig-lines r)
                                                     (seq objections) (conj (str "              " (subs objections-note 2))))
                                                   [(other-config-line r cfg-seen (seq objections))]))))
                                     rs)))
                          configs))))
             [:a :b :d])
     ["" "The APIs, by stage (built: named as the stage's build names it; plan: the plan's name, a binding to confirm):"]
     (for [a apis
           :let [x (get resolved (:id a))]]
       (str "  " (:stage a) " " (:var a) " [" (name (:bound a)) "] "
            (cond (contains? x :value) "resolved" (:broken x) (str "BROKEN " (:broken x)) :else (str "missing (" (:missing x) ")"))
            (when (:plan-name a) (str "; plan: " (:plan-name a)))
            (when (:optional a) "; optional"))))))

(def report-path (or (System/getenv "RIG_REPLAY_REPORT") "runs/phase8-replays.txt"))

(defn write-report!
  "Write the report, whatever the run reached (called from a `finally`),
  and print it."
  [state]
  (let [lines (try (vec (report-lines state))
                   (catch Throwable t [(str "the report failed: " (.getSimpleName (class t)) " " (ex-message t))]))
        text (str (str/join "\n" lines) "\n")]
    (io/make-parents (io/file report-path))
    (spit report-path text)
    (println text)))

(defn- now-ist []
  (.format (ZonedDateTime/now (ZoneId/of "Asia/Kolkata")) (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss 'IST'")))

(defn run-replays!
  "The whole run, into `state` as it goes (so a `finally` can report a
  partial run): resolve the APIs; prepare every case (lockstep, and
  whether it is practical); one in-process cluster; RP2's road check; each
  practical case played (a fresh module each on the main road); each case
  judged."
  [state]
  (let [t0 (System/nanoTime)
        resolved (resolve-apis)
        prepared (mapv #(prepare-case resolved %) cases)]
    (swap! state assoc :resolved resolved :results (mapv judge-case prepared))
    (with-open [ipc (rtest/create-ipc)]
      (let [check (road-check ipc)
            road (:road check)
            _ (swap! state assoc :check check)
            _ (when (= :one-module road) (launch! ipc))
            played (volatile! 0)
            stuck (volatile! nil)
            results (mapv (fn [r]
                            (cond
                              (:not-practical r) (judge-case r)
                              (= :none road) (assoc (judge-case (assoc r :not-practical "the module did not launch"))
                                                    :fails [(str "the module did not launch: " (:error check))])
                              @stuck (assoc (judge-case (assoc r :not-practical (str "not run: " @stuck)))
                                            :fails [(str "not run: " @stuck)])
                              :else
                              (let [env {:ipc ipc :api resolved :road road :first? (or (= :fresh road) (zero? @played))}
                                    play (play-case! env (:case r) (:ls r))]
                                (vswap! played inc)
                                (when (:stuck-call play)
                                  (vreset! stuck (str "a call in " (:id r) " did not return (" (:stuck-call play)
                                                      "), so a stale call could reach a later case's module")))
                                (let [j (judge-case (assoc r :play play))]
                                  (swap! state update :results (fn [rs] (mapv #(if (= (:id %) (:id j)) j %) rs)))
                                  j))))
                          prepared)]
        (when (= :one-module road) (try (destroy! ipc) (catch Throwable _ nil)))
        (swap! state assoc :results results :ms (ms-since t0))))))

;; ============================================================== the replays

(deftest replays
  (let [state (atom {:started (now-ist)})]
    (try
      (run-replays! state)
      (catch Throwable t
        (swap! state assoc :error (str (.getSimpleName (class t)) ": " (ex-message t)))
        (throw t))
      (finally
        (write-report! @state)))
    (let [{:keys [results check]} @state]
      (testing "the module launches and relaunches in one cluster (RP2's check)"
        (is (not= :none (:road check)) (str "the module did not launch: " (:error check))))
      (testing "every case runs, or is not practical for want of names; none fails"
        (is (= (count cases) (count results)))
        (doseq [r results]
          (is (or (#{:practical :approximated} (:status r)) (seq (:missing r)))
              (str (:id r) " is not practical with its stages resolved: " (:refusal r)))
          (is (empty? (:fails r)) (str (:id r) ": " (str/join "; " (:fails r)))))))))

;; ======================================================= the model-side tests
;; None needs a cluster. They check the adapter's model side and its judging
;; rules, so the run above can only be as wrong as the rig is.

(def ^:private note-spec {:e :e0 :k :note :mention nil :replaces :none :mark #{} :other-layer nil})

(defn- offer-op [who layer] [:offer {:who who :layer layer :facts [note-spec] :stood-on nil :times 1}])

(deftest lockstep-ends-where-run-ends
  (doseq [c cases
          [cname cfg] (:configs (named-configs (:set c)))]
    (let [ls (lockstep cfg (:history c))
          [ok seen _] (fsc/play cfg (:case c) [])]
      (is (= (:end ls) (fm/run cfg (:history c)))
          (str (:id c) " under " (name cname) ": the lockstep's end state is run's"))
      (is (= seen (seen-of (:end ls) (:expect c)))
          (str (:id c) " under " (name cname) ": the lockstep's end state gives play's seen"))
      (when (= :baseline cname)
        (is ok (str (:id c) ": the model says as said under baseline"))))))

(deftest lockstep-finds-every-effect
  (testing "every decision, read and person forget of every case, each once, in order"
    (doseq [c cases]
      (let [{:keys [start end steps]} (lockstep fm/baseline (:history c))
            decided (mapcat :decided steps)
            prepared (mapcat :prepared steps)
            answered (set (for [nm (keys (:sent end)) :when (and (not (seed-act? nm)) (seq (fm/answers-for end nm)))] nm))]
        (is (= answered (set (map :name decided))) (str (:id c) ": every answered name is decided once"))
        (is (= (count decided) (count (distinct (map :name decided)))) (str (:id c) ": no name decided twice"))
        (is (every? #(contains? (set (map :name decided)) (:name %)) prepared)
            (str (:id c) ": a name decided at [:prepare] is recorded at [:commit]"))
        (is (= (:reads end) (mapv :read (mapcat :reads steps))) (str (:id c) ": every read, in order"))
        (is (= (set (keys (:persons end))) (set (map :person (mapcat :forgets steps))))
            (str (:id c) ": every person forget"))
        (is (empty? (:reads start)) (str (:id c) ": the first facts read nothing"))
        (is (every? #(not= :split (get-in % [:model :answer])) decided)
            (str (:id c) ": no name's partitions disagree"))
        (is (nil? (guard-refusal {:steps steps})) (str (:id c) ": every op's decisions have an order")))))
  (testing "B1: the request, the value forget and the read-out decided by three separate steps"
    (let [steps (:steps (lockstep fm/baseline (:history (case-by-id "B1"))))
          step-0s (filter #(= [:step 0] (:op %)) steps)]
      (is (= [["o2"] ["o3"] ["crossing:o2"]] (mapv #(mapv :name (:decided %)) step-0s)))
      (is (= {:answer :no :reason :source-erased}
             (select-keys (:model (first (:decided (last step-0s)))) [:answer :reason]))
          "the read-out refused :source-erased")))
  (testing "B3: the request then its read-out in one work step, by stamp; the landing at the batch, two partitions as one yes"
    (let [steps (:steps (lockstep fm/baseline (:history (case-by-id "B3"))))
          work (first (filter #(= ["o2" "crossing:o2"] (mapv :name (:decided %))) steps))
          landing (first (filter #(= ["landing:o2"] (mapv :name (:decided %))) steps))]
      (is (some? work))
      (is (apply < (map :stamp (:decided work))))
      (is (= [:batch] (:op landing)))
      (is (= 2 (count (:answers (first (:decided landing))))))
      (is (= :yes (get-in (first (:decided landing)) [:model :answer])))))
  (testing "D1 [F7]: decided at [:prepare], recorded at [:commit] the same; the revoke at [:step 1]"
    (let [steps (:steps (lockstep fm/baseline (:history (case-by-id "D1"))))
          at (fn [op] (first (filter #(= op (:op %)) steps)))]
      (is (= [["o0" :no :permission-from-another-layer]]
             (mapv (juxt :name (comp :answer :model) (comp :reason :model)) (:prepared (at [:prepare])))))
      (is (= [["o0" :no :permission-from-another-layer]]
             (mapv (juxt :name (comp :answer :model) (comp :reason :model)) (:decided (at [:commit])))))
      (is (= [["o2" :yes]] (mapv (juxt :name (comp :answer :model)) (:decided (at [:step 1])))))))
  (testing "D2 [F6]: the revoke and the write each recorded at two micro partitions, reduced to one answer"
    (let [ds (mapcat :decided (:steps (lockstep fm/baseline (:history (case-by-id "D2")))))]
      (is (= [["o0" :yes nil 2] ["o1" :no :permission-revoked 2]]
             (mapv (juxt :name (comp :answer :model) (comp :reason :model) (comp count :answers)) ds))))))

(deftest the-guard-catches-decisions-it-cannot-order
  (let [h [(offer-op :alice :alice) (offer-op :alice :alice-hand)]
        c {:id "X1" :set :a :history h :expect {:values {}} :case ["two offers the drain decides" h {:values {}}]
           :predicted {:status :practical}}
        ls (lockstep fm/baseline h)
        everything (into {} (for [a apis] [(:id a) {:value :resolved}]))
        r (judge-case (prepare-case everything c))]
    (is (re-find #"\[:drain\].*cannot tell" (str (guard-refusal ls))) "the drain decides on two stream partitions in one op")
    (is (= :not-practical (:status r)))
    (is (seq (:fails r)) "with its stages resolved, not practical fails the test")))

(deftest every-step-of-the-fixed-histories-has-a-row
  (doseq [c cases]
    (is (nil? (history-refusal (:history c))) (str (:id c) ": every op has a row that covers it"))
    (is (nil? (effect-refusal (lockstep fm/baseline (:history c)))) (str (:id c) ": every effect has a rig row")))
  (testing "a step kind with no row, or an op outside its row, is named; nothing is played"
    (is (re-find #"no rig row \(named, not built\)" (str (history-refusal [[:failover :stream 1]]))))
    (is (re-find #"no rig row \(named, not built\)" (str (history-refusal [[:retry 0]]))))
    (is (re-find #"outside its row, read" (str (history-refusal [[:read [:at 3]]]))))
    (is (re-find #"outside its row, offer" (str (history-refusal [[:offer {:who :alice :layer :alice :facts [note-spec] :times 2}]]))))
    (is (re-find #"outside its row, promote" (str (history-refusal [[:promote 0 :base 1]])))))
  (testing "a value forget in the group has no rig row tonight"
    (is (re-find #"forget of o0#0 in group has no rig row"
                 (str (effect-refusal (lockstep fm/baseline [(offer-op :bob :group) [:batch] [:forget-value 0] [:batch]])))))))

(deftest answers-per-name-reduce-to-one
  (is (= {:answer :yes :stamp 5 :where [[:micro 0] [:micro 2]]}
         (reduce-answers [{:answer :yes :stamp 5 :where [:micro 0]} {:answer :yes :stamp 5 :where [:micro 2]}])))
  (is (= :permission-revoked
         (:reason (reduce-answers [{:answer :no :reason :permission-revoked :stamp 3 :where [:micro 0]}
                                   {:answer :no :reason :permission-revoked :stamp 3 :where [:micro 2]}]))))
  (is (= :split (:answer (reduce-answers [{:answer :yes :stamp 3 :where [:micro 0]} {:answer :no :reason :x :stamp 3 :where [:micro 2]}]))))
  (is (= :split (:answer (reduce-answers [{:answer :no :reason :x :stamp 3} {:answer :no :reason :y :stamp 3}]))))
  (is (nil? (reduce-answers [])))
  (is (false? (:ok? (judge-answer {:answer :split :answers []} {:answer :yes} nil)))
      "a model name whose partitions disagree is the model's own finding, and fails"))

(deftest the-model-report-is-reproduced
  (let [lines (str/split-lines (slurp "runs/phase8-model-report.txt"))
        section (->> lines
                     (drop-while #(not= "== fixed histories" %))
                     rest
                     (take-while #(and (not (str/blank? %)) (not (str/starts-with? % "==")))))]
    (is (= 64 (count section)) "the saved report's fixed-history section")
    (is (= (vec section) (model-report-lines))
        "the named configurations and checks give the model's own report, line for line")))

(deftest known-differences-cite-their-cases
  (is (= (range 1 21) (map :n known-differences)) "KD1 to KD20, each once, in order")
  (doseq [kd known-differences
          :let [id (kd-str (:n kd))]]
    (is (seq (:title kd)) (str id " has a title"))
    (is (seq (:source kd)) (str id " cites its source"))
    (is (every? (set case-ids) (:cases kd)) (str id " cites only the fourteen cases"))
    (if (= :not-exercised (:group kd))
      (do (is (empty? (:cases kd)) (str id " is exercised by no fixed history"))
          (is (seq (:why kd)) (str id " says why no fixed history exercises it")))
      (do (is (seq (:cases kd)) (str id " cites the cases it touches"))
          (is (seq (:rule kd)) (str id " says how it is told from an unknown difference")))))
  (let [by-n (into {} (map (juxt :n identity)) known-differences)]
    (doseq [[id ns] plan-case-kds]
      (is (every? #(contains? (:cases (by-n %)) id) ns)
          (str id ": the plan's predicted line names only differences that cite it")))
    (doseq [id case-ids]
      (is (every? #(contains? (:cases (by-n %)) id) (predicted-kds id)) (str id ": its predictions cite it"))))
  (is (not (contains? (predicted-kds "B3") 5)) "KD5 shows only on the :before-forward fallback"))

(deftest the-world-grants-every-model-permission
  (doseq [rn [main-road-names (fallback-names "B3")]
          :let [w (world rn)
                granted (set (concat (:stream-grants w) (get-in w [:base :grants]) (get-in w [:group :grants])))
                one-owner (set (map first (:one-owner w)))]]
    (doseq [pid fm/permissions]
      (is (contains? granted (rig-pid rn pid)) (str (pr-str pid) " is granted in the rig's seed")))
    (is (every? #(contains? one-owner (nth % 2)) (:stream-grants w)) "stream grants live in one-owner layers")
    (is (every? #(= (:group-layer w) (nth % 2)) (get-in w [:group :grants])) "group grants live in the group")
    (is (every? #(= 4 (count %)) (concat (get-in w [:base :grants]) (get-in w [:group :grants])))
        "a member's permission sits beneath the layer's root"))
  (testing "[F4] Bob's rig-only working layer, his own, where his read entries land"
    (let [w (world main-road-names)]
      (is (= [:bob-hand [:bob :bob-hand :bob-hand]] (get-in w [:working :bob])))
      (is (some #{[:bob-hand {:kind :hand :owner :bob}]} (:one-owner w)))
      (is (some #{[:bob :bob-hand :bob-hand]} (:stream-grants w)))))
  (testing "[F9] on the session road a write cites its session's grant beneath the mapped permission"
    (let [w (world main-road-names)]
      (is (= [:alice :group :group [:group :group :group]] (cited-pid w main-road-names [:alice :group :own])))
      (with-redefs [door-road :session]
        (is (= [:alice-session :group :group [:alice :group :group [:group :group :group]]]
               (cited-pid w main-road-names [:alice :group :own]))))))
  (testing "the fallback road renames persons in values, the model's value otherwise verbatim"
    (let [rn (fallback-names "A4")]
      (is (= {:token "v1" :persons #{:alice-a4 :bob-a4}} (rig-value rn {:token "v1" :persons #{:alice :bob}})))
      (is (= {:token "v1"} (rig-value rn {:token "v1"})))
      (is (= [:alice-a4 :group-a4 :group-a4 [:group-a4 :group-a4 :group-a4]] (rig-pid rn [:alice :group :own])))
      (is (= [:alice-a4 :group-a4 :alice-hand-a4] (rig-pid rn [:alice :group :session])))
      (is (= [:bob-a4 :base :base [:operator :base :base]] (rig-pid rn [:bob :base :own]))))))

(deftest judging-an-answer
  (let [no (fn [r] {:answer :no :reason r})]
    (is (:ok? (judge-answer {:answer :yes} {:answer :yes} {:answer :yes})) "yes and yes, its lease yes")
    (is (:ok? (judge-answer (no :permission-revoked) (no :permission-revoked) {:answer :yes})) "the same no")
    (is (= 1 (:kd (judge-answer (no :permission-revoked) (no :no-such-lock) (no :permission-revoked))))
        "KD1: the reason on the lease, the value act refused on its face")
    (is (= 1 (:kd (judge-answer (no :permission-from-another-layer) nil (no :permission-from-another-layer))))
        "[F8] KD1 on the lease alone when no value act was sent")
    (is (not (:ok? (judge-answer (no :permission-revoked) (no :no-such-lock) (no :no-permission))))
        "a lease refused for another reason than the model's is unexplained")
    (is (not (:ok? (judge-answer (no :permission-revoked) (no :malformed) (no :permission-revoked))))
        "a value act refused otherwise than on its face is unexplained")
    (is (not (:ok? (judge-answer {:answer :yes} {:answer :yes} (no :person-forgotten))))
        "[F11] a refused lease is a difference in itself")
    (is (not (:ok? (judge-answer {:answer :yes} (no :class-mismatch) {:answer :yes}))) "the model admits, the rig refuses")
    (is (not (:ok? (judge-answer (no :source-erased) {:answer :yes} nil))) "the model refuses, the rig admits")
    (is (not (:ok? (judge-answer (no :source-erased) (no :stale-replaces) nil))) "two different reasons")))

(deftest judging-an-exit-refusal
  (let [forgot {:answer :yes :stamp 100}]
    (is (= 10 (:kd (judge-exit-refusal :person-forgotten forgot nil))) "refused :person-forgotten after the reader's forget")
    (is (= 10 (:kd (judge-exit-refusal :no-such-lock forgot {:answer :no :reason :person-forgotten})))
        "the entry's lease refused :person-forgotten, the entry refused on its face")
    (is (= 10 (:kd (judge-exit-refusal :no-such-lock forgot {:answer :yes :stamp 40})))
        "the entry sealed under a lock leased before the forget")
    (is (not (:ok? (judge-exit-refusal :no-such-lock forgot {:answer :yes :stamp 140})))
        "a lock leased after the forget explains nothing")
    (is (not (:ok? (judge-exit-refusal :person-forgotten nil nil))) "refused for a reader never forgotten")
    (is (not (:ok? (judge-exit-refusal :person-forgotten {:answer :no :reason :x} nil))) "refused after a forget the rig refused")
    (is (not (:ok? (judge-exit-refusal :not-visible forgot nil))) "another refusal after the forget")
    (is (not (:ok? (judge-exit-refusal :no-such-lock forgot nil))) "no lease found for the entry")))

(deftest judging-a-fact
  (let [rn main-road-names
        v {:token "v1" :persons #{:bob}}]
    (is (:ok? (judge-fact rn nil {:k :mention :value v} {:fid 1 :stamp 5 :value v})) "open, the same value")
    (is (not (:ok? (judge-fact rn nil {:k :mention :value v} {:fid 1 :stamp 5 :value {:token "v2" :persons #{:bob}}})))
        "open, another value")
    (is (:ok? (judge-fact rn nil {:k :note :erased-at 9} {:fid 1 :stamp 5 :erased-at 7})) "erased on both sides")
    (is (not (:ok? (judge-fact rn nil {:k :note :erased-at 9} {:fid 1 :stamp 5 :value {:token "v1"}}))) "erased, the rig open")
    (is (not (:ok? (judge-fact rn nil {:k :note :value {:token "v1"}} {:fid 1 :stamp 5 :erased-at 7}))) "open, the rig erased")
    (is (not (:ok? (judge-fact rn nil {:k :note :erased-at 9} {:fid 1 :stamp 5 :erased-at 5})))
        "an erasure dated at or before its value's own stamp (KD12's relation)")
    (is (not (:ok? (judge-fact rn nil {:k :note :value {:token "v1"}} {:fid 1 :unreadable :does-not-open}))) "unreadable")
    (is (not (:ok? (judge-fact rn nil {:k :note :value {:token "v1"}} {:fid 1 :absent true}))) "absent")
    (is (not (:ok? (judge-fact rn nil {:k :note :value {:token "v1"}} nil))) "no row")
    (let [src [[:alice :by-layer :offer (java.util.UUID. 1 1)] 0]
          refs (control-refs rn {} {["o0" 0] src} :forget {:target ["o0" 0]})]
      (is (= [src] refs))
      (is (= 20 (:kd (judge-fact rn refs {:k :forget :value {:target ["o0" 0]}} {:fid 2 :stamp 9 :value {:target src}})))
          "a control fact holding the mapped reference, whatever its form (KD20)")
      (is (not (:ok? (judge-fact rn refs {:k :forget :value {:target ["o0" 0]}} {:fid 2 :stamp 9 :value {:target [:elsewhere 0]}})))
          "a control fact that refers elsewhere")
      (is (not (:ok? (judge-fact rn [nil] {:k :forget :value {:target ["o9" 0]}} {:fid 2 :stamp 9 :value {:target src}})))
          "a reference with no rig counterpart"))))

(deftest the-verdict-rules
  (let [c (case-by-id "A1")
        b (case-by-id "B1")
        seen {:values {[:alice :note nil] :erased [:alice :mention #{:bob}] :erased}}
        played (fn [c play] (judge-case {:id (:id c) :case c :model-seen seen :missing []
                                         :play (merge {:seen seen :diffs [] :approx [] :kd-seen #{}} play)}))]
    (testing "as said, with or without known differences"
      (is (= [:practical :as-said []] ((juxt :status :outcome :fails) (played c {}))))
      (is (= [:as-said []] ((juxt :outcome :fails) (played c {:diffs [{:where "x" :says "y" :kd 10}]})))))
    (testing "an unexplained difference, or another seen, fails"
      (is (seq (:fails (played c {:diffs [{:where "x" :says "y"}]}))))
      (is (= :differs (:outcome (played c {:diffs [{:where "x" :says "y"}]}))))
      (is (seq (:fails (played c {:seen (assoc-in seen [:values [:alice :note nil]] :open)}))))
      (is (seq (:fails (played c {:seen nil}))) "a case that did not finish"))
    (testing "worse than predicted fails; better does not"
      (is (seq (:fails (played c {:approx ["a hold"]}))) "A1 is predicted practical")
      (is (empty? (:fails (played b {}))) "B1 played with no hold is better than predicted"))
    (testing "predictions that did not show are printed, not failed"
      (is (= #{2 6 7 8 9 10 11 12} (:not-seen (played c {}))))
      (is (= #{10} (:not-seen (played c {:kd-seen #{2 6 7 8 9 11 12}})))))
    (testing "not practical: for want of names nothing fails; a broken namespace or a refusal with its stages resolved fails"
      (is (empty? (:fails (judge-case {:id "A1" :case c :not-practical "stage 2 missing"
                                       :missing [{:var 'x/y :missing "no such namespace"}]}))))
      (is (seq (:fails (judge-case {:id "A1" :case c :not-practical "stage 2" :missing [{:var 'x/y :broken "does not load"}]}))))
      (is (seq (:fails (judge-case {:id "A1" :case c :not-practical "step 3 has no rig row" :missing []
                                    :refusal "step 3 has no rig row"})))))))

(deftest the-api-table
  (let [resolved (resolve-apis)]
    (doseq [a apis]
      (is (or (= :all (:cases a)) (every? (set case-ids) (:cases a))) (str (:var a) " names only the fourteen cases"))
      (is (not (contains? (get resolved (:id a)) :broken)) (str (:var a) ": its namespace is there and does not load")))
    (doseq [a apis :when (#{"1" "5a"} (:stage a))]
      (is (contains? (get resolved (:id a)) :value) (str (:var a) " is built on this branch")))
    (is (not-any? #{'rig.store.micro-client/make-person! 'rig.store.micro-client/forget-person! 'rig.claims/hold!}
                  (map :var apis))
        "[F8] never phase 3's placeholder person acts, never the hold that blocks a task")
    (doseq [id case-ids]
      (is (some #(and (= "2" (:stage %)) (needs? % id)) apis) (str id " needs phase 2's persons"))
      (is (some #(and (= "3" (:stage %)) (needs? % id)) apis) (str id " needs phase 3's seed")))))

(deftest the-report-renders
  (let [resolved (resolve-apis)
        results (mapv #(judge-case (prepare-case resolved %)) cases)
        lines (vec (report-lines {:started "now" :resolved resolved :results results
                                  :check {:road :fresh :launch-ms 1 :destroy-ms 1 :relaunch-ms 1}}))
        text (set lines)]
    (doseq [prefix ["run " "rig " "model " "cluster " "stages " "summary " "not tested by the lockstep (F10):"]]
      (is (some #(str/starts-with? % prefix) lines) (str "the header has " prefix)))
    (doseq [x f10-lines] (is (contains? text (str "  " x))))
    (doseq [n (range 1 21)] (is (some #(str/starts-with? % (str "  KD" n " [")) lines) (str "KD" n " is reported")))
    (doseq [set-id [:a :b :d]
            [cname cfg] (:configs (named-configs set-id))
            c (filter #(= set-id (:set %)) cases)
            l (model-lines cname cfg c (:checks (named-configs set-id)))]
      (is (contains? text l) (str (:id c) " under " (name cname) ": the model's own line")))
    (let [under (drop-while #(not= "  under baseline-but-not-p6-line-at-the-read-out" %) lines)
          rig-lines-there (take 4 (filter #(str/starts-with? % "      rig:    ") under))]
      (is (= [false false true true] (mapv #(str/ends-with? % objections-note) rig-lines-there))
          "B3's and B4's rig lines say the model's objections are its own (IV6)"))
    (is (= (+ 32 8 4) (count (filter #(str/starts-with? % "      rig:    ") lines)))
        "one rig line per case per configuration, under the model's (A 8 x 4, B 4 x 2, D 2 x 2)")))

(deftest holds-follow-the-model
  (let [at-request (fn [id]
                     (let [steps (:steps (lockstep fm/baseline (:history (case-by-id id))))
                           step (first (filter #(some #{"o2"} (map :name (:decided %))) steps))
                           p (atom {:steps steps})]
                       {:cross (#'decided-later? p step "crossing:o2") :land (#'decided-later? p step "landing:o2")
                        :p p :steps steps}))]
    (testing "B1 and B2: the read-out is decided later, so it is held; no landing is ever decided, so none is held"
      (doseq [id ["B1" "B2"]]
        (let [{:keys [cross land p steps]} (at-request id)
              crossing-step (first (filter #(some #{"crossing:o2"} (map :name (:decided %))) steps))]
          (is (true? cross) id)
          (is (false? land) id)
          (is (false? (#'decided-later? p crossing-step "landing:o2")) (str id ": nothing to hold at the read-out")))))
    (testing "B3 and B4: the read-out in the request's own op, not held; the landing later, held"
      (doseq [id ["B3" "B4"]]
        (let [{:keys [cross land]} (at-request id)]
          (is (false? cross) id)
          (is (true? land) id))))))

(deftest the-group-keeps-a-reader
  (let [p (fn [forgotten] (atom {:rn identity :forgotten forgotten}))]
    (is (= :alice (#'live-member (p {}))))
    (is (= :bob (#'live-member (p {:alice {:answer :yes}}))) "[F4] Bob reads the group after Alice's forget")
    (is (= :alice (#'live-member (p {:alice {:answer :no :reason :x}}))) "a refused forget leaves Alice live")
    (is (nil? (#'live-member (p {:alice {:answer :yes} :bob {:answer :yes}})))
        "no live member: the read falls to Alice, whose refusal KD10 judges")))

(deftest seed-answers-are-found-in-every-shape
  (let [y {:answer :yes :stamp 1}
        n {:answer :no :reason :class-mismatch}]
    (is (= [y] (answers-in y)) "an ack")
    (is (= [y y y] (answers-in [y y y])) "make-base!'s answers")
    (is (= [y n] (answers-in {:reclass {:offer {} :answer y} :made n :offer {}})) "make-group!'s, the re-class first")
    (is (= [y] (answers-in {:reclass nil :made y :offer {}})) "make-group! with the base already re-classed")
    (is (= #{y n} (set (answers-in {:alice y :group n}))) "open-session!'s, per layer")
    (is (= :unknown (:answer (first (answers-in 42)))) "anything else is not a yes")))

(deftest extra-rows-are-facts-the-model-lacks
  (let [known #{[:a 0]}
        rows [{:fid [:a 0] :k :note} {:fid [:b 0] :k :note} {:fid [:c 0] :k :lease} {:fid [:d 0] :k :read/point}
              {:fid [:e 0] :k :permission} {:fid [:f 0] :k :kind} {:fid [:g 0] :absent true} {:fid [:h 0] :k :revoke}]]
    (is (= [[:b 0] [:h 0]] (mapv :fid (#'extra-rows rows known)))
        "a value, or a control fact with no counterpart; never a counterpart or a rig-only kind")))

(deftest control-references-for-every-kind
  (let [rn identity
        req [:alice :by-layer :offer (java.util.UUID. 2 2)]
        src [[:alice :by-layer :offer (java.util.UUID. 1 1)] 0]
        rname {"o2" req}
        rfid {["o0" 0] src}]
    (is (= [src :group] (control-refs rn rname rfid :promote-request {:source ["o0" 0] :target :group})))
    (is (= [req src] (control-refs rn rname rfid :crossed {:request "o2" :source ["o0" 0]})))
    (is (= [src] (control-refs rn rname rfid :forget {:target ["o0" 0]})))
    (is (= [[:alice :group :alice-hand]] (control-refs rn rname rfid :revoke {:permission [:alice :group :session]})))
    (is (= [[:alice :group :group [:group :group :group]]] (control-refs rn rname rfid :revoke {:permission [:alice :group :own]})))))

(deftest a-call-or-a-wait-that-does-not-return-is-cut
  (with-redefs [call-ms 200 wait-ms 200]
    (let [t0 (System/nanoTime)
          e (try (bounded-call "a stuck call" #(Thread/sleep 5000)) nil (catch clojure.lang.ExceptionInfo e e))]
      (is (= "a stuck call" (::timeout (ex-data e))))
      (is (::call (ex-data e)) "marked as a call, so the run plays no later case")
      (is (< (ms-since t0) 2000) "cut at its bound, not waited out"))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"boom" (bounded-call "a call that throws" #(throw (ex-info "boom" {}))))
        "a call's own exception is its own")
    (is (= 7 (bounded-call "a call that returns" (constantly 7))))
    (let [e (try (wait-for "nothing comes" (constantly nil)) nil (catch clojure.lang.ExceptionInfo e e))]
      (is (= "nothing comes" (::timeout (ex-data e))))
      (is (not (::call (ex-data e))) "a wait that times out is that case's difference, and later cases still run"))
    (is (= :there (wait-for "it comes" (constantly :there))))))
