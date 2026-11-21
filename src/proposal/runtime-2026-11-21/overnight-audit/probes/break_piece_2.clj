(ns break-piece-2
  "Adversarial probes against store piece 2 (commits 34fdab0e..3544d026).
  Read-only against src-store, test-store, and gate-histories."
  (:require [clojure.pprint :as pprint]
            [softland.store.act :as act]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.words :as door-words]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.stamp :as stamp]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.layers :as layers]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows])
  (:import [clojure.lang PersistentQueue]
           [java.util UUID]))

;; Load gate-histories in-memory (read-only) to test reference vs gate walk
(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/reference.clj")
(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/walk.clj")

(def ref-judge (resolve 'gate-histories.reference/judge))
(def ref-start @(resolve 'gate-histories.reference/start))
(def ref-digest-var (resolve 'gate-histories.reference/*digest-of*))
(def gate-decide (resolve 'gate-histories.walk/decide))
(def gate-empty @(resolve 'gate-histories.walk/empty-record))

(defn arrived
  [layer depot uuid who session permission facts]
  {:act-id [layer depot uuid]
   :depot depot
   declaration/time-part 1700000000000
   :version 1
   :who who
   :layer layer
   :permission permission
   :session session
   :facts (vec facts)})

;; ---------------------------------------------------------------------------
;; PROBE 1: R5 + R2 contradiction & gate-histories reference vs gate divergence
;; ---------------------------------------------------------------------------

(defn probe-1-re-grant-act-permission []
  (println "=== PROBE 1A: R5 'After an end, the run or the act can be given a permission again' ===")
  (with-bindings {ref-digest-var digest/hex-for-its-permission}
    (let [layer :lab
          root-perm [:asha layer layer]
          u (UUID. 0 42)
          make-act (arrived layer :streaming (UUID. 0 1) :asha :s0 root-perm
                            [{:e layer :k :softland.store/depot :v :streaming}
                             {:e :asha :k :softland.store/permission :v root-perm}])
          unsigned (arrived layer :streaming u :ben :run-1 nil
                            [{:e :exp-1 :k :dose :v "5 mg"}])
          hex-x (digest/hex-for-its-permission unsigned)
          pa1 [:ben layer layer root-perm :see-and-act [:act u hex-x]]
          give-1 (arrived layer :streaming (UUID. 0 2) :asha :s0 root-perm
                          [{:e u :k :softland.store/act-permission :v pa1}])
          end-1 (arrived layer :streaming (UUID. 0 3) :asha :s0 root-perm
                         [{:e u :k :softland.store/act-permission}])
          attempt-1 (assoc unsigned :permission pa1)
          ;; Cal gets a layer permission and re-grants the act permission (or Asha re-grants pa1)
          cal-perm [:cal layer layer root-perm :see-and-act]
          give-cal (arrived layer :streaming (UUID. 0 4) :asha :s0 root-perm
                            [{:e :cal :k :softland.store/permission :v cal-perm}])
          pa2 [:ben layer layer cal-perm :see-and-act [:act u hex-x]]
          give-2 (arrived layer :streaming (UUID. 0 5) :cal :s0 cal-perm
                          [{:e u :k :softland.store/act-permission :v pa2}])
          attempt-2 (assoc unsigned :permission pa2)
          history [make-act give-1 end-1 attempt-1 give-cal give-2 attempt-2]]
      (loop [rec gate-empty
             ref ref-start
             idx 0
             todo history]
        (when-let [sent (first todo)]
          (let [{:keys [record outcome]} (gate-decide rec :streaming sent)
                ruled (ref-judge ref layer sent)]
            (println (format "  step %d: gate=%-28s | statements(R1-R10)=%s"
                             idx
                             (cond (:repeat outcome) ":repeat (DROPPED AT CHECK A)"
                                   :else (pr-str (:verdict outcome)))
                             (pr-str (if (:accept? ruled) :accepted (:why ruled)))))
            (when (= idx 6)
              (let [ans-row (get (:answers record) (rows/answer-key layer u))
                    door-ans (door-words/answer (rows/answer-of ans-row) (digest/hex-of attempt-2))
                    perm-row (get (:facts record) (chain/place layer pa2))
                    chain-map {
                               (chain/place layer pa2) perm-row
                               (chain/place layer cal-perm) (get (:facts record) (chain/place layer cal-perm))
                               (chain/place layer root-perm) (get (:facts record) (chain/place layer root-perm))}]
                (println "  -> After step 6:")
                (println "     pa2 stands in $$facts?     " (chain/stands? layer pa2 chain-map))
                (println "     exp-1 fact written?        " (some? (get (:facts record) (rows/place-key layer :exp-1 :dose))))
                (println "     door/answer-of for attempt2" door-ans)))
            (recur record (:state ruled) (inc idx) (rest todo))))))))

(defn probe-1b-see-only-burns-act-permission []
  (println "\n=== PROBE 1B: A :see-only reader reads :act-permission UUID and permanently burns the permitted act ===")
  (let [layer :studio
        ida-own [:ida layer layer]
        u (UUID. 0 99)
        make-act (arrived layer :streaming (UUID. 0 1) :ida :s0 ida-own
                          [{:e layer :k :softland.store/depot :v :streaming}
                           {:e :ida :k :softland.store/permission :v ida-own}])
        cal-see [:cal layer layer ida-own :see]
        give-cal-see (arrived layer :streaming (UUID. 0 2) :ida :s0 ida-own
                              [{:e :cal :k :softland.store/permission :v cal-see}])
        unsigned-joe (arrived layer :streaming u :joe :run-1 nil
                              [{:e :secret-exp :k :dose :v "5 mg"}])
        pa [:joe layer layer ida-own :see-and-act [:act u (digest/hex-for-its-permission unsigned-joe)]]
        give-joe-act (arrived layer :streaming (UUID. 0 3) :ida :s0 ida-own
                              [{:e u :k :softland.store/act-permission :v pa}])
        ;; Cal has ONLY :see. Cal queries the layer with :stores-own-facts true:
        rec-after-give (:record (reduce (fn [{:keys [record]} x] (gate-decide record :streaming x))
                                        {:record gate-empty}
                                        [make-act give-cal-see give-joe-act]))
        q (:ok (query/check {:asks [:run :first :last] :asker :cal :layers [layer] :stores-own-facts true}))
        known {(sources/own-rows-source layer layer)
               (select-keys (:facts rec-after-give) [(rows/place-key layer layer :softland.store/depot)])
               (sources/permission-source layer layer :cal)
               (get (:facts rec-after-give) (rows/place-key layer :cal :softland.store/permission))
               (sources/permission-source layer layer :ida)
               (get (:facts rec-after-give) (rows/place-key layer :ida :softland.store/permission))
               (first ((get sources/of-word :run) q layer nil))
               (:facts rec-after-give)}
        reading (sources/reading q known :once query/default-numbers)
        cal-read-answer (answer/whole q reading known)
        leaked-uuids (keep (fn [[[entity k] _]] (when (= :softland.store/act-permission k) entity))
                           (:places cal-read-answer))
        ;; Cal now sends any dummy act using the leaked UUID u!
        cal-burn (arrived layer :streaming (first leaked-uuids) :cal :cal-s cal-see
                          [{:e :anything :k :v :v 1}])
        after-burn (gate-decide rec-after-give :streaming cal-burn)
        ;; Now Joe sends his valid permitted act!
        joe-act (assoc unsigned-joe :permission pa)
        after-joe (gate-decide (:record after-burn) :streaming joe-act)
        joe-door-ans (door-words/answer
                      (rows/answer-of (get-in after-joe [:record :answers (rows/answer-key layer u)]))
                      (digest/hex-of joe-act))]
    (println "  UUIDs of :act-permission visible to :see-only :cal:" (vec leaked-uuids))
    (println "  Cal's dummy act under leaked UUID outcome:         " (get-in after-burn [:outcome :verdict]))
    (println "  Joe's valid permitted act outcome:                 " (:outcome after-joe))
    (println "  What door/answer-of tells Joe:                     " joe-door-ans)
    (println "  Did Joe's permitted fact land?                     "
             (some? (get-in after-joe [:record :facts (rows/place-key layer :secret-exp :dose)])))))

;; ---------------------------------------------------------------------------
;; PROBE 2: Maker's act + door/additions + digest/hex-for-its-permission footgun
;; ---------------------------------------------------------------------------

(defn probe-2-door-digest-mismatch []
  (println "\n=== PROBE 2: Maker's act passed to hex-for-its-permission vs door/send-act ===")
  (let [layer :studio
        act-id [layer :streaming (UUID. 0 77)]
        maker-act {:version 1
                   :who :joe
                   :layer layer
                   :session :run-1
                   :facts [{:e :exp :k :dose :v "5 mg"}]}
        hex-on-maker-act (digest/hex-for-its-permission maker-act)
        ;; If maker tries to pre-add :act-id and :depot before calling door:
        with-door-parts (assoc maker-act :act-id act-id :depot :streaming)
        door-reading-if-pre-added (additions/reading with-door-parts act-id)
        ;; What door actually appends when given maker-act + pa:
        pa [:joe layer layer [:ida layer layer] :see-and-act [:act (nth act-id 2) hex-on-maker-act]]
        signed-maker-act (assoc maker-act :permission pa)
        appended (:thing (additions/thing-to-append (additions/reading signed-maker-act act-id) act-id 1700000000000))
        gate-hex (decision/hex-for-its-permission appended (:ok (declaration/check appended)))]
    (println "  hex-for-its-permission(maker-act):      " hex-on-maker-act)
    (println "  hex-for-its-permission(appended-thing): " gate-hex)
    (println "  Match?                                  " (= hex-on-maker-act gate-hex))
    (println "  additions/reading if maker adds :act-id:" door-reading-if-pre-added)))

;; ---------------------------------------------------------------------------
;; PROBE 3: Read path rank inversion -> 8 rounds static, >9 rounds under 2 writes
;; ---------------------------------------------------------------------------

(defn- by-code-point [a b]
  (cond (= a b) 0 (rows/before? a b) -1 :else 1))

(defn- table [tables gate tbl]
  (get tables [gate tbl] (sorted-map-by by-code-point)))

(defn- put [tables gate tbl k row]
  (assoc tables [gate tbl] (assoc (table tables gate tbl) k row)))

(defn- fact-row* [layer who e k v stamp]
  (rows/fact-row {:e e :k k :text (when (some? v) (softland.store.door.text/text (act/normal-value v)))}
                 (UUID. 0 stamp) 0 stamp (* 1000 stamp)
                 (softland.store.door.text/text who)
                 (softland.store.door.text/text [who layer layer])))

(defn- marks-row* [layer since-by-key stamp]
  (-> (rows/fact-row {:e layer :k :softland.store/marked-keys :v (set (keys since-by-key))
                      :text (softland.store.door.text/text (set (keys since-by-key)))}
                     (UUID. 0 stamp) 0 stamp (* 1000 stamp) ":root" (str "[:root " layer " " layer "]"))
      (rows/with-level-since since-by-key)))

(defn- table-value [tables source]
  (let [t (table tables (:gate source) (:table source))
        [kind a b] (:path source)]
    (if (= :row kind)
      (get t a)
      (into (sorted-map-by by-code-point) (subseq t >= a < b)))))

(defn probe-3-live-rounds []
  (println "\n=== PROBE 3: Static live-read rounds with :from-layer + :session + :points-at ===")
  (let [s-root [:root :start :start]
        m-root [:root :mid :mid]
        tables (-> {}
                   ;; :start is :members, builds on [:mid :far], cal has run-permission in :run-7 beneath :root
                   (put :streaming :facts (rows/place-key :start :start :softland.store/depot)
                        (fact-row* :start :root :start :softland.store/depot :streaming 1))
                   (put :streaming :facts (rows/place-key :start :root :softland.store/permission)
                        (fact-row* :start :root :root :softland.store/permission s-root 2))
                   (put :streaming :facts (rows/place-key :start :start :softland.store/builds-on)
                        (fact-row* :start :root :start :softland.store/builds-on [:mid :far] 3))
                   (put :streaming :facts (rows/place-key :start :run-7 :softland.store/run-permission)
                        (fact-row* :start :root :run-7 :softland.store/run-permission
                                   [:cal :start :start s-root :see [:run :run-7]] 4))
                   ;; :mid is :members, cal has run-permission in :run-7 beneath :root
                   (put :streaming :facts (rows/place-key :mid :mid :softland.store/depot)
                        (fact-row* :mid :root :mid :softland.store/depot :streaming 1))
                   (put :streaming :facts (rows/place-key :mid :root :softland.store/permission)
                        (fact-row* :mid :root :root :softland.store/permission m-root 2))
                   (put :streaming :facts (rows/place-key :mid :run-7 :softland.store/run-permission)
                        (fact-row* :mid :root :run-7 :softland.store/run-permission
                                   [:cal :mid :mid m-root :see [:run :run-7]] 3))
                   ;; :far is :everyone, marks :parent, holds [:item-1 :parent :target]
                   (put :streaming :facts (rows/place-key :far :far :softland.store/depot)
                        (fact-row* :far :root :far :softland.store/depot :streaming 1))
                   (put :streaming :facts (rows/place-key :far :far :softland.store/who-may-see)
                        (fact-row* :far :root :far :softland.store/who-may-see :everyone 2))
                   (put :streaming :facts (rows/place-key :far :far :softland.store/marked-keys)
                        (marks-row* :far {:parent 3} 3))
                   (put :streaming :facts (rows/place-key :far :item-1 :parent)
                        (fact-row* :far :root :item-1 :parent :target 4))
                   (put :streaming :facts-by-value
                        (rows/by-value-key :far :parent 3 (rows/value-digest-of :target) :item-1)
                        (rows/by-value-row ":root" "[:root :far :far]" (UUID. 0 4) 0 4 4000)))
        q (:ok (query/check {:asks [:points-at :parent :target]
                             :asker :cal
                             :session :run-7
                             :from-layer :start}))]
    (loop [known {} held #{} round 0 trace []]
      (let [reading (sources/reading q known :live query/default-numbers held)
            {:keys [join leave]} (sources/moves reading held)]
        (if (and (empty? join) (empty? leave))
          (do (println "  Total rounds on STATIC store (sources.clj:90 claims <= 6):" round)
              (doseq [[r joined] trace]
                (println (format "    round %d joined %d sources: %s"
                                 r (count joined) (mapv (juxt :layer :table (comp first :path)) joined)))))
          (let [next-held (into (reduce disj held leave) join)
                next-known (into (reduce dissoc known leave)
                                 (map (fn [s] [s (table-value tables s)]))
                                 join)]
            (recur next-known next-held (inc round) (conj trace [round join]))))))))

;; ---------------------------------------------------------------------------
;; PROBE 4: Non-holder run-permission joins chain & leaks writes via {:anew}
;; ---------------------------------------------------------------------------

(defn probe-4-other-holders-run-permission []
  (println "\n=== PROBE 4: Other holder (:dov) granted & ended on :run-7 in a :may-not-see layer ===")
  (let [near-root [:root :near :near]
        bea-perm [:bea :near :near near-root]
        tables (-> {}
                   (put :streaming :facts (rows/place-key :near :near :softland.store/depot)
                        (fact-row* :near :root :near :softland.store/depot :streaming 1))
                   (put :streaming :facts (rows/place-key :near :root :softland.store/permission)
                        (fact-row* :near :root :root :softland.store/permission near-root 2))
                   (put :streaming :facts (rows/place-key :near :bea :softland.store/permission)
                        (fact-row* :near :root :bea :softland.store/permission bea-perm 3))
                   (put :streaming :facts (rows/place-key :near :p :text)
                        (fact-row* :near :root :p :text "secret" 4))
                   (put :streaming :facts (rows/place-key :far :far :softland.store/depot)
                        (fact-row* :far :root :far :softland.store/depot :streaming 1))
                   (put :streaming :facts (rows/place-key :far :far :softland.store/who-may-see)
                        (fact-row* :far :root :far :softland.store/who-may-see :everyone 2))
                   (put :streaming :facts (rows/place-key :far :p :text)
                        (fact-row* :far :root :p :text "public" 3)))
        q (:ok (query/check {:asks [:place :p :text] :asker :ann :session :run-7 :layers [:near :far]}))
        ;; Open live read for :ann (who has NO permission in :near)
        own-near (sources/own-rows-source :near :near)
        perm-ann (sources/permission-source :near :near :ann)
        run-7 (sources/run-permission-source :near :near :run-7)
        own-far (sources/own-rows-source :far :far)
        fact-far (sources/place-source :far :far :p :text)
        held0 #{own-near perm-ann run-7 own-far fact-far}
        known0 (into {} (map (fn [s] [s (table-value tables s)])) held0)
        initial-answer (answer/whole q (sources/reading q known0 :live query/default-numbers held0) known0)
        ;; Write 1: :bea gives :dov (NOT :ann!) a run-permission on :run-7
        dov-run-row (fact-row* :near :bea :run-7 :softland.store/run-permission
                               [:dov :near :near bea-perm :see [:run :run-7]] 100)
        tables1 (put tables :streaming :facts (rows/place-key :near :run-7 :softland.store/run-permission) dov-run-row)
        known1-before-join (assoc known0 run-7 dov-run-row)
        reading1 (sources/reading q known1-before-join :live query/default-numbers held0)
        moves1 (sources/moves reading1 held0)
        held1 (into held0 (:join moves1))
        ;; Before :bea and :root pushes arrive, known is known1-before-join:
        reading1-settled (sources/reading q known1-before-join :live query/default-numbers held1)
        [mb1 ma1] (answer/meanings q known1-before-join run-7 nil dov-run-row)
        step1 (answer/after-change q reading1-settled known1-before-join run-7 #{} mb1 ma1 (:join moves1) (:leave moves1) false)
        ;; Now :bea and :root arrive:
        known1-arrived (into known1-before-join (map (fn [s] [s (table-value tables1 s)])) (:join moves1))
        reading1-arrived (sources/reading q known1-arrived :live query/default-numbers held1)
        step-arrive (answer/after-arrival q reading1-arrived known1-arrived (first (:join moves1)) #{} #{} (:owed? step1))
        ;; Write 2: :bea ends :dov's run-permission on :run-7
        end-run-row (fact-row* :near :bea :run-7 :softland.store/run-permission nil 101)
        known2-raw (assoc known1-arrived run-7 end-run-row)
        reading2 (sources/reading q known2-raw :live query/default-numbers held1)
        moves2 (sources/moves reading2 held1)
        held2 (reduce disj held1 (:leave moves2))
        known2 (reduce dissoc known2-raw (:leave moves2))
        reading2-settled (sources/reading q known2 :live query/default-numbers held2)
        [mb2 ma2] (answer/meanings q known2 run-7 dov-run-row end-run-row)
        step2 (answer/after-change q reading2-settled known2 run-7 #{} mb2 ma2 (:join moves2) (:leave moves2) false)]
    (println "  :ann's standing in :near throughout:     " (get-in reading1-arrived [:standing :near]))
    (println "  Pushes joined in :near for :dov's chain: " (mapv #(second (:path %)) (:join moves1)))
    (println "  On :dov grant (step1):                   " step1)
    (println "  When :dov's chain arrives (step-arrive):  tell :anew? =" (some? (:anew (:tell step-arrive)))
             "| identical to initial answer? =" (= initial-answer (:anew (:tell step-arrive))))
    (println "  On :dov end (step2):                      tell :anew? =" (some? (:anew (:tell step2)))
             "| identical to initial answer? =" (= initial-answer (:anew (:tell step2))))
    ;; Also check read-once extra round trip!
    (let [once-r0 (sources/reading q {} :once query/default-numbers)
          known-r0 (into {} (map (fn [s] [s (table-value tables1 s)])) (:missing once-r0))
          once-r1 (sources/reading q known-r0 :once query/default-numbers)]
      (println "  read-once Round 1 standing in :near:     " (get-in once-r1 [:standing :near]))
      (println "  read-once Round 1 STILL fetches missing: " (mapv #(second (:path %)) (:missing once-r1))))))

;; ---------------------------------------------------------------------------
;; PROBE 5: Exhaustive callback-order model checker for tricky :session worlds
;; (marked :points-at, dynamic chain join, :from-layer, :stores-own-facts)
;; ---------------------------------------------------------------------------

(load-file "test-store/softland/store/read_path_pure_proof.clj")

(def rp-write (ns-resolve 'softland.store.read-path-pure-proof 'write))
(def rp-open-live (ns-resolve 'softland.store.read-path-pure-proof 'open-live))
(def rp-every-order (ns-resolve 'softland.store.read-path-pure-proof 'every-order))
(def rp-q (ns-resolve 'softland.store.read-path-pure-proof 'q))

(defn probe-5-callback-orders []
  (println "\n=== PROBE 5: Callback-order permutations on marked :points-at, dynamic chains, :from-layer, :stores-own-facts ===")
  (let [;; Scenario A: marked :points-at with dynamic chain join (:cal wasn't open initially!) + layer perm + run perm
        base-tables (reduce rp-write {}
                            [[:make :near 1] [:make :far 1]
                             [:setting :far :everyone 2]
                             [:marks :near #{:parent} 3] [:marks :far #{:parent} 3]
                             [:permission :near :root 4]
                             [:give :near :bea :root 5]
                             [:give :near :cal :bea 6]
                             [:give :near :ann :root 7]
                             [:fact :near :root :p1 :parent :canvas-x 10]
                             [:fact :far :root :p1 :parent :canvas-1 11]
                             [:fact :near :root :p2 :parent :canvas-1 12]])
        scenarios
        [["A: marked :points-at, give-run under dynamic chain (:cal) + end-own(:ann) + write :near"
          base-tables
          [[:give-run :near :run-7 :ann :cal 100]
           [:end :near :ann 101]
           [:fact :near :root :p1 :parent :canvas-1 102]]
          {:asks [:points-at :parent :canvas-1] :asker :ann :session :run-7 :layers [:near :far]}]

         ["B: marked :points-at, give-run(:cal) + end-own(:ann) + end(:bea above :cal) + write :far"
          base-tables
          [[:give-run :near :run-7 :ann :cal 100]
           [:end :near :ann 101]
           [:end :near :bea 102]
           [:fact :far :root :p3 :parent :canvas-1 103]]
          {:asks [:points-at :parent :canvas-1] :asker :ann :session :run-7 :layers [:near :far]}]

         ["C: :from-layer :near with builds-on [:far], end-own + give-run(:cal) + list change"
          (rp-write base-tables [:list :near [:far] 8])
          [[:end :near :ann 100]
           [:give-run :near :run-7 :ann :cal 101]
           [:fact :near :root :p1 :parent :canvas-1 102]]
          {:asks [:points-at :parent :canvas-1] :asker :ann :session :run-7 :from-layer :near}]

         ["D: :stores-own-facts true asking [:place :cal :softland.store/permission] while :cal is dynamic chain of :run-7"
          base-tables
          [[:give-run :near :run-7 :ann :cal 100]
           [:end :near :ann 101]
           [:end :near :cal 102]]
          {:asks [:place :cal :softland.store/permission] :asker :ann :session :run-7
           :layers [:near :far] :stores-own-facts true}]

         ["E: :stores-own-facts true asking [:entity :near] when :session == :near (run-permission inside own-rows!)"
          (reduce rp-write {}
                  [[:make :near 1] [:make :far 1] [:setting :far :everyone 2]
                   [:permission :near :root 3] [:give :near :bea :root 4]
                   [:fact :far :root :near :note "far-note" 10]])
          [[:give-run :near :near :ann :bea 100]
           [:end-run :near :near 101]
           [:give-run :near :near :ann :bea 102]]
          {:asks [:entity :near] :asker :ann :session :near :layers [:near :far] :stores-own-facts true}]]]
    (doseq [[label tables writes query-map] scenarios]
      (let [world (rp-open-live tables writes (rp-q query-map))
            res (rp-every-order world nil)]
        (println (format "  %-88s -> states=%-6d ends=%-5d wrong=%s"
                         label (:states res) (:ends res) (pr-str (:wrong res))))))))

(defn probe-6-additional-receipts []
  (println "\n=== PROBE 6A: read-once call count ('no call more' claim in read_path.clj:1228 & BUILD:70) ===")
  (let [tables (reduce rp-write {}
                       [[:make :m-run 1] [:permission :m-run :ada 2]
                        [:give-run :m-run :run-9 :cal :ada 3]
                        [:fact :m-run :ada :note :text "from m-run" 4]])
        play-once (ns-resolve 'softland.store.read-path-pure-proof 'play-once)
        q-no-run (rp-q {:asks [:place :note :text] :asker :cal :layers [:m-run]})
        q-in-run (rp-q {:asks [:place :note :text] :asker :cal :session :run-9 :layers [:m-run]})
        q-ada-no-run (rp-q {:asks [:place :note :text] :asker :ada :layers [:m-run]})
        q-ada-dov-run (rp-q {:asks [:place :note :text] :asker :ada :session :run-9 :layers [:m-run]})]
    (println "  :cal without :session -> rounds/calls:" ((juxt :rounds :calls) (play-once tables q-no-run)))
    (println "  :cal with :session    -> rounds/calls:" ((juxt :rounds :calls) (play-once tables q-in-run)))
    (let [tables-bea (reduce rp-write {}
                             [[:make :m-run 1] [:permission :m-run :ada 2] [:give :m-run :bea :ada 3]
                              [:give-run :m-run :run-9 :dov :bea 4]
                              [:fact :m-run :ada :note :text "from m-run" 5]])]
      (println "  :ada (first holder) without :session               -> rounds/calls:"
               ((juxt :rounds :calls) (play-once tables-bea q-ada-no-run)))
      (println "  :ada (first holder) with :session held by :dov/bea -> rounds/calls:"
               ((juxt :rounds :calls) (play-once tables-bea q-ada-dov-run)))))

  (println "\n=== PROBE 6B: 8 static live-read rounds when :far is also :members (leaving only 1 spare round before max-rounds=9) ===")
  (let [tables (reduce rp-write {}
                       [[:make :start 1] [:permission :start :root 2] [:list :start [:mid :far] 3]
                        [:give-run :start :run-7 :cal :root 4]
                        [:make :mid 1] [:permission :mid :root 2]
                        [:give-run :mid :run-7 :cal :root 3]
                        [:make :far 1] [:permission :far :root 2] [:marks :far #{:parent} 3]
                        [:give-run :far :run-7 :cal :root 4]
                        [:fact :far :root :item-1 :parent :canvas-1 10]])
        live-rounds (ns-resolve 'softland.store.read-path-pure-proof 'live-rounds)
        world (rp-open-live tables [] (rp-q {:asks [:points-at :parent :canvas-1]
                                             :asker :cal :session :run-7 :from-layer :start}))]
    (println "  Static live-read opening rounds (sources.clj:90 & read_path_pure_proof:1232 claim <= 6):" (:rounds world))
    (println "  Pushes held:" (count (:held world)) "| max-rounds in read_path.clj:" 9))

  (println "\n=== PROBE 6C: R5 holder ending :act-permission under itself & ending :see :run-permission under itself ===")
  (let [layer :lab
        root [:asha layer layer]
        u (UUID. 0 50)
        make-act (arrived layer :streaming (UUID. 0 1) :asha :s0 root
                          [{:e layer :k :softland.store/depot :v :streaming}
                           {:e :asha :k :softland.store/permission :v root}])
        unsigned (arrived layer :streaming u :ben :run-1 nil [{:e :doc :k :v :v 1}])
        pa [:ben layer layer root :see-and-act [:act u (digest/hex-for-its-permission unsigned)]]
        r-see [:ben layer layer root :see [:run :run-1]]
        give-both (arrived layer :streaming (UUID. 0 2) :asha :s0 root
                           [{:e u :k :softland.store/act-permission :v pa}
                            {:e :run-1 :k :softland.store/run-permission :v r-see}])
        rec (:record (reduce (fn [{:keys [record]} x] (gate-decide record :streaming x))
                             {:record gate-empty} [make-act give-both]))
        end-pa-new-uuid (arrived layer :streaming (UUID. 0 51) :ben :run-1 pa
                                 [{:e u :k :softland.store/act-permission}])
        end-pa-same-uuid (arrived layer :streaming u :ben :run-1 pa
                                  [{:e u :k :softland.store/act-permission}])
        end-r-see-self (arrived layer :streaming (UUID. 0 52) :ben :run-1 r-see
                                [{:e :run-1 :k :softland.store/run-permission}])]
    (println "  Ben ends pa under pa (fresh act-id): " (get-in (gate-decide rec :streaming end-pa-new-uuid) [:outcome :verdict]))
    (println "  Ben ends pa under pa (act's uuid u): " (get-in (gate-decide rec :streaming end-pa-same-uuid) [:outcome :verdict])
             "(AND burns uuid u!)")
    (println "  Ben ends :see run-perm under itself: " (get-in (gate-decide rec :streaming end-r-see-self) [:outcome :verdict])))

  (println "\n=== PROBE 6D: R2 'part for part' normal-form equivalence vs raw hex-for-its-permission ===")
  (let [u (UUID. 0 60)
        a1 {:act-id [:lab :streaming u] :depot :streaming
            :version 1 :who :ben :layer :lab :session :s1
            :facts [{:e :x :k :n :v 5}]}
        a2 {:act-id [:lab :streaming u] :depot :streaming
            :version 1 :who :ben :layer :lab :session :s1
            :subjects #{} :based-on {}
            :facts (list {:e :x :k :n :v 5N})}]
    (println "  act/parse normal forms equal?         "
             (= (:ok (act/parse (assoc (dissoc a1 :act-id :depot) :permission [:ben :lab :lab])))
                (:ok (act/parse (assoc (dissoc a2 :act-id :depot) :permission [:ben :lab :lab])))))
    (println "  hex-for-its-permission equal?         "
             (= (digest/hex-for-its-permission a1) (digest/hex-for-its-permission a2)))))

(probe-1-re-grant-act-permission)
(probe-1b-see-only-burns-act-permission)
(probe-2-door-digest-mismatch)
(probe-3-live-rounds)
(probe-4-other-holders-run-permission)
(probe-5-callback-orders)
(probe-6-additional-receipts)
