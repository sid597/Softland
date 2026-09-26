(ns rig.store.read-model-test
  "T1 (PLAN-read-exit.md): the rig's reads answer as the formal model's do
  for the same history.

  The model has no pattern reads; it has `read-as-of`, every admitted fact
  at or before T with its value or `:erased-at`. The test writes histories
  in the model's own op vocabulary, restricted to what the rig has tonight
  (`:offer` on Alice's three one-owner layers, with replaces, `:retry`,
  `:reuse`, `[:read :now]`, `[:read [:at i]]`), plays each through
  `formal.model/step` under `baseline`, op by op, and through the rig in
  lockstep, and at every model read compares, per layer:
  - the model's facts in that layer with the rig's `[:all]` read;
  - the model's facts filtered by each pattern (`[:e]`, `[:ek]`,
    `[:latest]`, `[:k]`, `[:kv :note ..]`) with the rig's pattern reads;
  - each fact by id with a point read, and a fact admitted after the moment
    as absent;
  and at the end every offer's answer. Compared: which facts, their order,
  value, replaces. Not compared, by construction: stamp values (the model's
  clock is a tick, the rig's a hybrid stamp), and the rig's layer-making
  facts (`:kind`, `:owner`, `:class`, `:lock-grain` on the layer's own
  entity, P10), which the model keeps as settings.

  The moment. A model read as of T shows, in each layer, a prefix of that
  layer's admission order; the rig reads each layer as of the rig stamp of
  the last fact in that prefix (the rig stamp of the model's chosen fact,
  in the chosen fact's own layer). The plan's single number, the chosen
  fact's rig stamp applied to every layer, is also run and each difference
  it gives is reported (OBSERVED lines), not asserted: the model's
  partitions keep their own clocks, which a hybrid clock does not share.
  The rig runs on simulated time, one millisecond per model op, so its
  stamps follow the op order across tasks.

  The rig's three layers are renamed per history (`:alice` becomes
  `:alice-h1`, and so on) so each history starts empty in one cluster; the
  model's permission ids map to the rig's ([who layer :own] to [who layer
  layer], [who layer :session] to [who layer hand]). The model has no
  retracts (its offerer always makes a value), so retracts are tested in
  rig.store.read-exit-test only. scenarios.clj's fixed histories are listed
  with what keeps each from being replayed tonight, and that is checked.

  Wave 1 (phase 2 merged): values are sealed at the door, and the rig
  differs from the model where RIG.md's For Sid 14 and 2 name it: an offer
  by a writer who cannot lease in the layer (Bob into Alice's layers) is
  refused on its face `:no-such-lock` and recorded nowhere, where the model
  records the permission reason; so a reuse of such a name would be
  decided fresh in the rig (the model keeps it refused), and is not sent;
  and a reuse of a name whose act was refused and recorded, under the same
  parts, is answered with the recorded refusal (the parts digest holds no
  value), where the model says `:name-taken`. Each is counted and named
  (OBSERVED), never hidden; every other answer and every read still
  compares exactly."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fsc]
            [rig.store.client :as c]
            [rig.store.toy-grammars :as tg]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads])
  (:import [com.rpl.rama.helpers TopologyUtils]))

(defn- say [& xs] (apply println "OBSERVED" xs))

;; ------------------------------------------------------------- histories

(def one-owner [:alice :alice-hand :alice-agent])
(def home {:alice 0 :alice-hand 1 :alice-agent 2})

(defn- nf
  "A model fact spec: entity, key, a replaces pick (:none :head :stale), and
  persons for a mention."
  ([e k] (nf e k :none nil))
  ([e k replaces] (nf e k replaces nil))
  ([e k replaces persons]
   {:e e :k k :replaces replaces :mention (when (= :mention k) (or persons #{:bob})) :mark #{} :other-layer nil}))

(defn- offer [who layer & facts]
  [[:offer {:who who :layer layer :facts (vec facts)}] [:work (home layer)]])

(def settle [[:work 0] [:work 1] [:work 2]])

(def fixed-histories
  {:chains
   (vec (concat
         (offer :alice :alice (nf :e0 :note) (nf :e1 :mention :none #{:bob}))
         (offer :alice :alice (nf :e0 :note :head))
         [[:read :now]]
         (offer :alice :alice-hand (nf :e0 :note) (nf :e2 :note))
         (offer :alice :alice (nf :e0 :note :head) (nf :e1 :mention :head #{:alice :bob}))
         [[:retry 0]] settle
         [[:read [:at 3]]]
         (offer :alice :alice-agent (nf :e3 :note) (nf :e3 :mention :none #{:alice}))
         [[:reuse 1 {:who :alice :layer :alice :facts [(nf :e2 :note)]}]] settle
         (offer :bob :alice (nf :e0 :note))
         (offer :alice :alice (nf :e0 :note :stale))
         (offer :alice :alice (nf :e0 :note :head) (nf :e0 :note :head))
         (offer :alice :alice-hand (nf :e2 :note :head))
         [[:read [:at 0]] [:read [:at 7]] [:read [:at 11]] [:read :now]]))
   :stood-on
   (vec (concat
         (offer :alice :alice-hand (nf :e1 :note))
         [[:offer {:who :alice :layer :alice :facts [(nf :e1 :note)] :stood-on 2}] [:work 0]]
         (offer :alice :alice-agent (nf :e1 :note))
         [[:read [:at 2]] [:read [:at 5]] [:read :now]]))})

(defn- random-history
  "A history over the restricted vocabulary, from a seed."
  [seed n]
  (let [rng (java.util.Random. (long seed))
        pick (fn [xs] (nth xs (.nextInt rng (count xs))))]
    (vec (mapcat (fn [_]
                   (case (pick [:offer :offer :offer :offer :offer :retry :reuse :read :read])
                     :offer (let [layer (pick one-owner)
                                  who (pick [:alice :alice :alice :alice :bob])]
                              (apply offer who layer
                                     (repeatedly (inc (.nextInt rng 2))
                                                 #(nf (pick [:e0 :e1 :e2]) (pick [:note :note :mention])
                                                      (pick [:none :none :head :head :stale])
                                                      (pick [#{:bob} #{:alice} #{:alice :bob}])))))
                     :retry (into [[:retry (.nextInt rng 4)]] settle)
                     :reuse (into [[:reuse (.nextInt rng 4) {:who :alice :layer (pick one-owner)
                                                              :facts [(nf (pick [:e0 :e1]) :note)]}]]
                                  settle)
                     :read [[:read (if (.nextBoolean rng) :now [:at (.nextInt rng 30)])]]))
                 (range n)))))

;; -------------------------------------------------------------- the rig side

(defn- rig-world
  "The model's one-owner world under per-history layer names."
  [ren]
  (let [[l1 l2 l3] (map ren one-owner)]
    ;; wave 1: an owner is a person first (L11); after the first history the person acts are refused as made
    {:persons [:alice :bob]
     :layers [[l1 {:kind :personal :owner :alice}] [l2 {:kind :hand :owner :alice}] [l3 {:kind :agent :owner :alice}]]
     :permissions (vec (concat (for [l [l1 l2 l3]] [:alice l l])
                               (for [l [l1 l3 :group :base]] [:alice l l2])))}))

(defn- rig-pid
  "A model permission id as the rig's: its layer renamed, and where it lives."
  [ren [who layer where]]
  (let [rl (get ren layer layer)]
    [who rl (if (= :session where) (ren :alice-hand) rl)]))

(defn- tr-value
  "A model fact's value as the rig holds it (a permission's id mapped)."
  [ren k v]
  (if (and (= :permission k) (map? v) (:id v)) (update v :id #(rig-pid ren %)) v))

(defn- start
  "Seed the rig for one history and map the model's first facts (its
  permission grants in one-owner layers) to the rig's."
  [st ren]
  (let [world (rig-world ren)
        answers (c/seed! st world)
        ;; phase 6 (V-F12): the model's grammars as facts in each layer, before its first :note or :mention
        _ (doseq [a (tg/write! st (map ren one-owner))]
            (when-not (= :yes (:answer a)) (throw (ex-info "toy grammars refused" {:answer a}))))
        ;; wave 1: phase 2's seed answers the store layer, then each person, then each layer
        grants (drop (+ 1 (count (:persons world)) (count (:layers world))) answers)
        by-pid (zipmap (:permissions world) grants)]
    (reduce (fn [ctx [who layer where :as pid]]
              (let [nm (str "grant:" (name who) ":" (name layer) (when (= :session where) "@session"))
                    a (get by-pid (rig-pid ren pid))]
                (if (and a (contains? (set one-owner) (fm/permission-layer pid)))
                  (-> ctx (assoc-in [:names nm] (:name a)) (assoc-in [:stamps (:name a)] (:stamp a)))
                  ctx)))
            {:names {} :stamps {} :sent {} :diffs [] :answers [] :observed []}
            fm/permissions)))

(defn- rig-fid [ctx [mnm idx]] (when-let [n (get-in ctx [:names mnm])] [n (long idx)]))

(defn- rig-offer
  "The model's offer as the rig's, under the rig name given."
  [ctx ren mo rname]
  (c/build {:name rname :who (:who mo) :layer (ren (:layer mo)) :class :by-layer
            :permission (rig-pid ren (:permission mo))
            :stood-on (into {} (for [id (:stood-on mo) :let [f (rig-fid ctx id)] :when f]
                                 [f (get-in ctx [:stamps (first f)])]))
            :facts (mapv (fn [f] (cond-> {:e (:e f) :k (:k f) :v (:v f) :mark (:mark f)}
                                   (:replaces f) (assoc :replaces (rig-fid ctx (:replaces f)))))
                         (:facts mo))}))

(defn- send! [st o]
  (TopologyUtils/advanceSimTime 1)
  (c/offer-until-answered! st o))

;; ----------------------------------------------------------- comparisons

(def making-keys #{:kind :owner :class :lock-grain})

(def door-keys
  "Wave 1: the door's own acts in a layer, which the model does not have
  (RIG.md For Sid 16): the lease before a write, a session close. Phase 6:
  the toy grammars written as facts before a history (V-F12, D-P4), which
  the model has compiled."
  #{:lease :session-closed :grammar})

(defn- kt [k] (subs (str k) 1))

(defn- model-view
  "A model fact as the rig would show it."
  [ctx ren f]
  {:fid (rig-fid ctx (:id f)) :e (:e f) :k (:k f)
   :replaces (some->> (:replaces f) (rig-fid ctx))
   :value (tr-value ren (:k f) (:value f))})

(defn- rig-view [row] (select-keys row [:fid :e :k :replaces :value]))

(defn- rig-rows
  "The rig's rows of a pattern read, the layer-making facts and the door's
  own acts (a lease, a session close: For Sid 16) left out."
  [st rl pattern m]
  (let [a (foreign-invoke-query (:read-pattern st) rl :alice pattern m 10000)]
    (when (contains? a :refused) (throw (ex-info "read refused" {:answer a :pattern pattern})))
    (into [] (comp (remove #(and (= rl (:e %)) (making-keys (:k %))))
                   (remove #(door-keys (:k %)))
                   (map rig-view))
          (:rows a))))

(defn- compare-read
  "Compare one model read with the rig's reads, per layer; returns ctx with
  any differences added."
  [ctx st ren ms r label]
  (let [facts (fm/all-facts ms)
        by-id (into {} (map (juxt :id identity)) facts)]
    (reduce
     (fn [ctx L]
       (let [rl (ren L)
             shown (->> (:facts r) (filter #(= L (:layer %)))
                        (map #(merge (by-id (:id %)) (select-keys % [:value :erased-at :stamp])))
                        (sort-by (juxt :stamp #(second (:id %)))))
             later (->> facts (filter #(and (= L (:layer %)) (> (:stamp %) (:as-of r)))))
             m (reduce max 0 (map #(get-in ctx [:stamps (get-in ctx [:names (first (:id %))])] 0) shown))
             view (mapv #(model-view ctx ren %) shown)
             diff (fn [ctx what model rig]
                    (if (= model rig) ctx
                        (update ctx :diffs conj {:where label :layer L :what what :model model :rig rig})))
             by-stamp (fn [rows] (sort-by (juxt #(get-in ctx [:stamps (first (:fid %))]) #(second (:fid %))) rows))
             sel (fn [pred sort-key] (->> (map vector shown view) (filter (comp pred first)) (sort-by (comp sort-key first)) (mapv second)))
             es (distinct (map :e shown))
             eks (distinct (map (juxt :e :k) shown))
             notes (distinct (keep #(when (= :note (:k %)) (:value %)) shown))]
         (as-> ctx ctx
           (diff ctx [:all] view (vec (by-stamp (rig-rows st rl [:all] m))))
           (reduce (fn [ctx e]
                     (diff ctx [:e e] (sel #(= e (:e %)) (juxt #(kt (:k %)) :stamp #(second (:id %))))
                           (rig-rows st rl [:e e] m)))
                   ctx (conj (vec es) :nobody))
           (reduce (fn [ctx [e k]]
                     (let [chain (sel #(and (= e (:e %)) (= k (:k %))) (juxt :stamp #(second (:id %))))]
                       (-> ctx
                           (diff [:ek e k] chain (rig-rows st rl [:ek e k] m))
                           (diff [:latest e k] (vec (take-last 1 chain)) (rig-rows st rl [:latest e k] m)))))
                   ctx eks)
           (reduce (fn [ctx k]
                     (diff ctx [:k k] (sel #(= k (:k %)) (juxt #(kt (:e %)) :stamp #(second (:id %))))
                           (rig-rows st rl [:k k] m)))
                   ctx (distinct (map :k shown)))
           (reduce (fn [ctx v]
                     (diff ctx [:kv :note v] (sel #(and (= :note (:k %)) (= v (:value %))) (juxt :stamp #(second (:id %))))
                           (rig-rows st rl [:kv :note v] m)))
                   ctx notes)
           (let [fids (mapv :fid view)
                 pr (when (seq fids) (foreign-invoke-query (:read-point st) rl :alice fids m))]
             (cond-> ctx
               (seq fids) (diff [:point :shown] (mapv #(select-keys % [:fid :value]) view)
                                (mapv #(select-keys % [:fid :value]) (:rows pr)))
               (seq later) (diff [:point :later]
                                 [{:fid (rig-fid ctx (:id (first later))) :absent true}]
                                 (:rows (foreign-invoke-query (:read-point st) rl :alice [(rig-fid ctx (:id (first later)))] m))))))))
     ctx one-owner)))

(defn- observe-single-moment
  "The plan's single moment (the chosen fact's rig stamp) applied to every
  layer: report where the fact sets differ from the model's."
  [ctx st ren ms r label]
  (let [pool (sort-by (juxt :stamp :id) (fm/all-facts ms))
        chosen (last (filter #(<= (:stamp %) (:as-of r)) pool))
        m (get-in ctx [:stamps (get-in ctx [:names (first (:id chosen))])])]
    (if (nil? m)
      ctx
      (reduce (fn [ctx L]
                (let [model (set (keep #(when (= L (:layer %)) (rig-fid ctx (:id %))) (:facts r)))
                      rig (set (map :fid (rig-rows st (ren L) [:all] m)))]
                  (if (= model rig)
                    ctx
                    (update ctx :observed conj {:where label :layer L :model-only (count (remove rig model))
                                                :rig-only (count (remove model rig))}))))
              ctx one-owner))))

;; ------------------------------------------------------------------ replay

(defn- face-refused?
  "A rig answer refused on its face (no stamp: nothing was recorded)."
  [a]
  (boolean (and (map? a) (= :no (:answer a)) (nil? (:stamp a)))))

(def permission-reasons
  #{:permission-does-not-cover-this :permission-from-another-layer :no-permission :permission-revoked})


(defn- replay
  "Play a history through the model op by op and through the rig in
  lockstep; compare at every read; return the context with differences."
  [st hname history]
  (let [ren (into {} (for [l one-owner] [l (keyword (str (name l) "-" (name hname)))]))
        ctx0 (start st ren)]
    (loop [ms (fm/seed-permissions (fm/init fm/baseline)) ops history ctx ctx0 i 0]
      (if (empty? ops)
        (assoc ctx :ms ms :ren ren)
        (let [[kind a :as op] (first ops)
              names (:client-sent ms)
              pick (fn [j] (when (seq names) (nth (rseq names) (mod j (count names)))))
              ms2 (fm/step ms op)]
          (recur
           ms2 (rest ops)
           (case kind
             :offer (let [mnm (last (:client-sent ms2))
                          mo (get-in ms2 [:sent mnm])
                          ro (rig-offer ctx ren mo nil)
                          ans (send! st ro)]
                      (-> ctx
                          (assoc-in [:names mnm] (:name ro))
                          (assoc-in [:sent (:name ro)] ro)
                          (assoc-in [:stamps (:name ro)] (:stamp ans))
                          (update :answers conj {:op i :kind :offer :model-name mnm :rig ans})))
             :retry (if-let [mnm (pick a)]
                      (let [ro (get-in ctx [:sent (get-in ctx [:names mnm])])]
                        (update ctx :answers conj {:op i :kind :retry :model-name mnm :rig (send! st ro)}))
                      ctx)
             :reuse (if-let [mnm (pick a)]
                      (let [mo (last (:reuses ms2))
                            rnm (get-in ctx [:names mnm])
                            first-rig (some #(when (and (= :offer (:kind %)) (= mnm (:model-name %))) (:rig %)) (:answers ctx))]
                        (if (face-refused? first-rig)
                          ;; wave 1 (For Sid 14): the name holds no record in the rig, so a reuse would be
                          ;; decided fresh where the model keeps it refused; named, not sent
                          (update ctx :known conj {:op i :kind :reuse-of-a-face :model-name mnm})
                          (let [ro (rig-offer ctx ren mo rnm)]
                            (update ctx :answers conj {:op i :kind :reuse :model-name mnm :rig (send! st ro)}))))
                      ctx)
             :read (let [r (last (:reads ms2))
                         label [hname i op]]
                     (-> ctx (compare-read st ren ms2 r label) (observe-single-moment st ren ms2 r label)
                         (update :reads (fnil inc 0))))
             ctx)
           (inc i)))))))

(defn- model-outcome [ms mnm]
  (let [a (first (fm/answers-for ms mnm))]
    (when a (if (= :yes (:answer a)) :yes (:reason a)))))

(defn- rig-outcome [a] (if (= :yes (:answer a)) :yes (:reason a)))

(defn- answer-diffs
  "Each offer's answer against the model's; a retry must repeat the first
  answer; a reuse is refused (taken, or made for another layer) and leaves
  the first record as it was."
  [ctx]
  (let [ms (:ms ctx)
        first-answer (into {} (for [x (:answers ctx) :when (= :offer (:kind x))] [(:model-name x) (:rig x)]))]
    (vec (for [x (:answers ctx)
               :let [mo (model-outcome ms (:model-name x))
                     ro (rig-outcome (:rig x))
                     fa (first-answer (:model-name x))
                     bad (case (:kind x)
                           ;; wave 1, For Sid 14: a writer who cannot lease is refused on its face
                           :offer (not (or (= mo ro) (and (= :no-such-lock ro) (contains? permission-reasons mo))))
                           :retry (not= (dissoc fa :name) (dissoc (:rig x) :name))
                           ;; wave 1, For Sid 2: a recorded refusal under the same parts is answered as recorded
                           :reuse (not (or (contains? #{:name-taken :mis-tagged} ro)
                                           (and (= :no (:answer fa)) (some? (:stamp fa)) (= (:reason fa) ro)))))]
               :when bad]
           (assoc x :model mo :rig-outcome ro)))))

(defn- known-differences
  "The answers that differ from the model's only as RIG.md names (For Sid
  14 and 2), and the reuses not sent: counted, for the OBSERVED line."
  [ctx]
  (let [ms (:ms ctx)
        first-answer (into {} (for [x (:answers ctx) :when (= :offer (:kind x))] [(:model-name x) (:rig x)]))]
    (concat (:known ctx)
            (for [x (:answers ctx)
                  :let [mo (model-outcome ms (:model-name x))
                        ro (rig-outcome (:rig x))
                        fa (first-answer (:model-name x))]
                  :when (or (and (= :offer (:kind x)) (not= mo ro))
                            (and (= :reuse (:kind x)) (not (contains? #{:name-taken :mis-tagged} ro))))]
              {:op (:op x) :kind (:kind x) :model mo :rig ro :first fa}))))

;; ------------------------------------------------------------------ the test

(defn- uses-op? [history op] (boolean (some #(= op (first %)) history)))

(defn- offers-into-group? [history]
  (boolean (some #(and (= :offer (first %)) (= :group (:layer (second %)))) history)))

(def not-replayed
  "scenarios.clj's fixed histories, each with what keeps it from the rig
  tonight, as a check over the history itself."
  (concat (for [c fsc/a-cases]
            [(first c) (second c) "a person forget (phase 2), and for most the group layer (phase 3)"
             #(uses-op? % :forget-person)])
          (for [c fsc/b-cases]
            [(first c) (second c) "a promotion (phase 4) into the group layer (phase 3), and a forget (phase 2)"
             #(and (uses-op? % :promote) (or (uses-op? % :forget-value) (uses-op? % :forget-person)))])
          (for [c fsc/d-cases]
            [(first c) (second c) "a write into the group layer on the micro gate (phase 3)"
             #(offers-into-group? %)])))

(deftest read-model
  (testing "scenarios.clj's fixed histories: each needs what the rig does not have tonight"
    (doseq [[what history why needs?] not-replayed]
      (say "not replayed:" what "--" why)
      (is (needs? history) what)))
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (with-open [_ (TopologyUtils/startSimTime)]
      (TopologyUtils/advanceSimTime (+ (System/currentTimeMillis) 10000))
      (let [st (rx/connect ipc)
            histories (merge fixed-histories
                             (into {} (for [s [11 23 37]] [(keyword (str "random" s)) (random-history s 25)])))]
        (doseq [[hname history] (sort-by key histories)]
          (testing (str "history " (name hname))
            (let [ctx (replay st hname history)
                  run (fm/run fm/baseline history)]
              (say (name hname) ":" (count history) "ops," (:reads ctx 0) "reads compared,"
                   (count (:answers ctx)) "offers answered;" (count (:diffs ctx)) "differences;"
                   "single-moment observations:" (pr-str (:observed ctx)))
              (say (name hname) ": known differences (For Sid 14, 2):" (pr-str (map #(select-keys % [:kind :model :rig]) (known-differences ctx))))
              (is (= (butlast (:reads run)) (:reads (:ms ctx)))
                  "formal.model/run gives the same reads: every offer was worked before its read")
              (is (empty? (:diffs ctx)) (with-out-str (doseq [d (take 5 (:diffs ctx))] (prn d))))
              (let [ad (answer-diffs ctx)]
                (is (empty? ad) (pr-str (take 3 ad))))
              (is (some #(= :yes (rig-outcome (:rig %))) (:answers ctx)) "the history admitted something")
              (testing "and the model's closing read, as of now"
                (let [ctx2 (compare-read (assoc ctx :diffs []) st (:ren ctx) (:ms ctx) (last (:reads run)) [hname :final])]
                  (is (empty? (:diffs ctx2)) (pr-str (take 3 (:diffs ctx2)))))))))))))
