(ns break-piece-2-yes
  "Adversarial pure probes against Store Piece 2 repair + yes-with-own-id
  (commits 27d883a3 and d3290f37, diff since 1a2e9811).
  Read-only against src-store, test-store, and gate-histories."
  (:require [softland.store.act :as act]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]
            [softland.store.door.words :as door-words]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.read-path :as read-path]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.layers :as layers]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows])
  (:import [java.util UUID]
           [java.util.concurrent CompletableFuture]
           [java.util.concurrent.atomic AtomicBoolean AtomicInteger AtomicLong]))

(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/reference.clj")
(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/walk.clj")
(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/live.clj")
(load-file "src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/run.clj")
(load-file "test-store/softland/store/read_path_pure_proof.clj")

(def ref-judge (resolve 'gate-histories.reference/judge))
(def ref-start @(resolve 'gate-histories.reference/start))
(def gate-decide (resolve 'gate-histories.walk/decide))
(def gate-empty @(resolve 'gate-histories.walk/empty-record))
(def gh-play (resolve 'gate-histories.run/play))
(def gh-print-trace (ns-resolve 'gate-histories.run 'print-trace))

(def rp-write (ns-resolve 'softland.store.read-path-pure-proof 'write))
(def rp-open-live (ns-resolve 'softland.store.read-path-pure-proof 'open-live))
(def rp-every-order (ns-resolve 'softland.store.read-path-pure-proof 'every-order))
(def rp-told-var (ns-resolve 'softland.store.read-path-pure-proof 'told))
(def rp-q (ns-resolve 'softland.store.read-path-pure-proof 'q))

(defn maker-act [layer who session permission facts]
  (cond-> {:version 1
           :who who
           :layer layer
           :session session
           :facts (vec facts)}
    permission (assoc :permission permission)))

(defn arrived [layer depot uuid who session permission facts]
  (assoc (maker-act layer who session permission facts)
         :act-id [layer depot uuid]
         :depot depot
         declaration/time-part 1700000000000))

;; ---------------------------------------------------------------------------
;; PROBE 1A: P1 — gate-histories.run/play ITSELF breaks :unseen and leaks a
;; confidential fact to :dee on a valid 6-step gen-history input!
;; ---------------------------------------------------------------------------

(defn probe-1a-gate-histories-own-break []
  (println "=== PROBE 1A: gate-histories.run/play breaks :unseen AND leaks confidential data ===")
  (let [;; Every step below is in gate-histories.run/gen-op's exact grammar:
        ops [{:op :act :by :asha :in :run-1 :permission :current
              :facts [{:f :give :to :ben :allows :see-and-act :beneath nil}]}
             {:op :act :by :ben :in :run-1 :permission :current
              :facts [{:f :give :to :eve :allows :see-and-act :beneath nil}]}
             {:op :act :by :eve :in :run-1 :permission :current
              :facts [{:f :give :to :dee :allows :see :beneath nil}]}
             ;; :asha ends :ben -> :dee's chain (:asha -> :ben -> :eve -> :dee) is now dead!
             {:op :giver-ends :of :ben :in :run-1}
             ;; AFTER :dee lost standing, :asha writes confidential plain fact at [:thing-1 :colour]:
             {:op :act :by :asha :in :run-1 :permission :current
              :facts [{:f :plain :e :thing-1 :k :colour}]}
             ;; :asha gives :eve a fresh permission directly beneath :asha (:dee still :may-not-see!)
             {:op :act :by :asha :in :run-1 :permission :current
              :facts [{:f :give :to :eve :allows :see-and-act :beneath nil}]}
             ;; :eve atomically ends :eve and gives :dee beneath :eve:
             ;; At every stamp before and after, :dee is :may-not-see!
             {:op :act :by :eve :in :run-1 :permission :current
              :facts [{:f :end :of :eve}
                      {:f :give :to :dee :allows :see :beneath nil}]}]
        played (gh-play ops)]
    (println "  violations count:" (count (:violations played)))
    (doseq [v (:violations played)]
      (println "  violation:" (dissoc v :ref-before)))
    (gh-print-trace played)
    (assert (seq (:violations played)) "Expected gate-histories.run/play to fail :unseen!")))

;; ---------------------------------------------------------------------------
;; PROBE 1B: P1 — Pure read-path callback-order proof on BOTH :permission and
;; :run-permission (showing confidential leak AND spurious {:anew})
;; ---------------------------------------------------------------------------

(defn probe-1b-phantom-standing-chain []
  (println "\n=== PROBE 1B: Phantom standing chain across stale per-row pushes (pure read-path) ===")
  ;; 1. Via :permission (:end-and-give) using read_path_pure_proof's own every-order:
  (let [tables0 (reduce rp-write {}
                        [[:make :secret 1]
                         [:permission :secret :root 2]
                         [:give :secret :dave :root 3]
                         [:give :secret :bea :dave 4]
                         [:give :secret :ann :bea 5]
                         [:end :secret :dave 6]
                         ;; Written at stamp 7 AFTER :dave ended — :ann NEVER had standing for this!
                         [:fact :secret :root :rx :dose "TOP-SECRET-99mg" 7]
                         [:make :pub 1]
                         [:setting :pub :everyone 2]
                         [:fact :pub :root :rx :dose "aspirin-81mg" 3]
                         ;; Stamp 8: :root gives :bea a fresh permission beneath :root
                         [:give :secret :bea :root 8]])
        q (rp-q {:asks [:place :rx :dose] :asker :ann :layers [:secret :pub]})
        ;; At tables0, :ann is :may-not-see in :secret, and keeps pushes on :ann, :bea, :dave, :root
        tells (atom [])
        orig-told @rp-told-var
        world (rp-open-live tables0 [[:end-and-give :secret :bea :ann 9]] q)
        res (with-redefs-fn {rp-told-var (fn [w tell]
                                           (when tell (swap! tells conj tell))
                                           (orig-told w tell))}
              #(rp-every-order world nil))
        leaked-secret? (some #(= "TOP-SECRET-99mg" (get-in % [:anew :place :value])) @tells)]
    (println "  [:permission] :ann standing before & after stamp 9: [:may-not-see :may-not-see]")
    (println "  [:permission] states explored:" (:states res) "| total tells delivered (expected 0):" (count @tells))
    (println "  [:permission] leaked 'TOP-SECRET-99mg' to :ann?   " (boolean leaked-secret?))
    (println "  [:permission] distinct tells handed to :ann:")
    (doseq [t (distinct @tells)]
      (println "    ->" (pr-str t)))
    (assert leaked-secret? "Expected TOP-SECRET-99mg leak in callback order!"))

  ;; 2. Via :run-permission (:end-and-give-run) when :ann asks in :session :run-7:
  (let [tables0 (reduce rp-write {}
                        [[:make :secret 1]
                         [:permission :secret :root 2]
                         [:give :secret :dave :root 3]
                         [:give :secret :bea :dave 4]
                         [:give-run :secret :run-7 :ann :bea 5]
                         [:end :secret :dave 6]
                         [:fact :secret :root :rx :dose "RUN-SECRET-42mg" 7]
                         [:make :pub 1]
                         [:setting :pub :everyone 2]
                         [:fact :pub :root :rx :dose "aspirin-81mg" 3]
                         [:give :secret :bea :root 8]])
        q (rp-q {:asks [:place :rx :dose] :asker :ann :session :run-7 :layers [:secret :pub]})
        tells (atom [])
        orig-told @rp-told-var
        world (rp-open-live tables0 [[:end-and-give-run :secret :bea :run-7 :ann 9]] q)
        res (with-redefs-fn {rp-told-var (fn [w tell]
                                           (when tell (swap! tells conj tell))
                                           (orig-told w tell))}
              #(rp-every-order world nil))
        leaked-run-secret? (some #(= "RUN-SECRET-42mg" (get-in % [:anew :place :value])) @tells)]
    (println "  [:run-permission] states explored:" (:states res) "| total tells delivered (expected 0):" (count @tells))
    (println "  [:run-permission] leaked 'RUN-SECRET-42mg' to :ann?" (boolean leaked-run-secret?))
    (assert leaked-run-secret? "Expected RUN-SECRET-42mg leak in :run-permission callback order!")))

;; ---------------------------------------------------------------------------
;; PROBE 2: P2 — Refusal-word precedence & forged-yes misclassification in
;; decision/standing vs SPEC.md R9 and gate-histories/reference.clj
;; ---------------------------------------------------------------------------

(defn probe-2-standing-refusal-divergences []
  (println "\n=== PROBE 2: Refusal-word divergences (decision/standing vs SPEC.md R9 & reference.clj) ===")
  (let [layer :lab
        root [:asha layer layer]
        cal-p [:cal layer layer root :see-and-act]
        y-used (UUID. 89 1)
        y-ended (UUID. 89 2)
        maker-a (maker-act layer :ben :run-1 nil [{:e :doc :k :v :v "act-A"}])
        maker-b (maker-act layer :ben :run-1 nil [{:e :doc :k :v :v "act-B"}])
        hex-a (digest/hex-for-a-yes maker-a)
        hex-b (digest/hex-for-a-yes maker-b)
        pa-used [:ben layer layer root :see-and-act [:act y-used hex-a]]
        pa-ended [:ben layer layer root :see-and-act [:act y-ended hex-a]]
        run-p [:ben layer layer root :see-and-act [:run :run-1]]
        setup [(arrived layer :streaming (UUID. 0 1) :asha :run-1 root
                        [{:e layer :k :softland.store/depot :v :streaming}
                         {:e :asha :k :softland.store/permission :v root}])
               (arrived layer :streaming (UUID. 0 2) :asha :run-1 root
                        [{:e :cal :k :softland.store/permission :v cal-p}
                         {:e y-used :k :softland.store/act-permission :v pa-used}
                         {:e y-ended :k :softland.store/act-permission :v pa-ended}
                         {:e :run-1 :k :softland.store/run-permission :v run-p}])
               ;; Use y-used:
               (arrived layer :streaming (UUID. 0 3) :ben :run-1 pa-used (:facts maker-a))
               ;; End y-ended and :run-1:
               (arrived layer :streaming (UUID. 0 4) :asha :run-1 root
                        [{:e y-ended :k :softland.store/act-permission}
                         {:e :run-1 :k :softland.store/run-permission}])]
        [rec ref] (reduce (fn [[rec ref] t]
                            [(:record (gate-decide rec :streaming t))
                             (:state (ref-judge ref layer t))])
                          [gate-empty ref-start]
                          setup)
        ;; Never-given forged permissions on y-used:
        pa-forged-wrong-hex [:ben layer layer cal-p :see-and-act [:act y-used hex-b]]
        pa-forged-right-hex [:ben layer layer cal-p :see-and-act [:act y-used hex-a]]
        cases [["2a: wrong-digest act-B under USED yes y-used"
                (arrived layer :streaming (UUID. 0 10) :ben :run-1 pa-used (:facts maker-b))]
               ["2b: wrong-digest act-B under ENDED yes y-ended"
                (arrived layer :streaming (UUID. 0 11) :ben :run-1 pa-ended (:facts maker-b))]
               ["2c: wrong-session (:run-2) under ENDED :run-1 permission"
                (arrived layer :streaming (UUID. 0 12) :ben :run-2 run-p [{:e :doc :k :v :v "x"}])]
               ["2d: NEVER-GIVEN forged yes (wrong parent + wrong hex) on USED y-used"
                (arrived layer :streaming (UUID. 0 13) :ben :run-1 pa-forged-wrong-hex (:facts maker-a))]
               ["2e: NEVER-GIVEN forged yes (wrong parent + matching hex) on USED y-used"
                (arrived layer :streaming (UUID. 0 14) :ben :run-1 pa-forged-right-hex (:facts maker-a))]]]
    (doseq [[label t] cases]
      (let [g-word (get-in (gate-decide rec :streaming t) [:outcome :verdict])
            r-word (:why (ref-judge ref layer t))]
        (println (format "  %-70s\n     gate: %-28s | SPEC/reference: %s"
                         label (pr-str g-word) (pr-str r-word)))
        (assert (not= g-word r-word) (str "Expected divergence on " label))))))

;; ---------------------------------------------------------------------------
;; PROBE 3: P3 — read_path/settle! hitting (:live max-rounds) silently drops
;; unjoined `join` sources without calling end-read!, wedging the live read
;; ---------------------------------------------------------------------------

(defn probe-3-settle-max-rounds-wedge []
  (println "\n=== PROBE 3: settle! hitting (:live max-rounds) drops unjoined sources & wedges read ===")
  (let [q (:ok (query/check {:asks [:place :p :v] :asker :ann :from-layer :l0}))
        own-l0 (sources/own-rows-source :l0 :l0)
        perm-l0 (sources/permission-source :l0 :l0 :ann)
        place-l0 (sources/place-source :l0 :l0 :p :v)
        make-own-rows (fn [builds-on]
                        {(rows/place-key :l0 :l0 :softland.store/depot)
                         (rows/fact-row {:e :l0 :k :softland.store/depot :text ":streaming"}
                                        (UUID. 0 1) 0 1 1000 ":root" "[:root :l0 :l0]")
                         (rows/place-key :l0 :l0 :softland.store/who-may-see)
                         (rows/fact-row {:e :l0 :k :softland.store/who-may-see :text ":everyone"}
                                        (UUID. 0 1) 1 1 1000 ":root" "[:root :l0 :l0]")
                         (rows/place-key :l0 :l0 :softland.store/builds-on)
                         (rows/fact-row {:e :l0 :k :softland.store/builds-on :text (text/text builds-on)}
                                        (UUID. 0 2) 0 2 2000 ":root" "[:root :l0 :l0]")})
        calls (atom [])
        rp ((var-get #'softland.store.read-path/->ReadPath)
            "Store" nil nil nil nil (query/numbers nil)
            (atom {:pushes {own-l0 {:readers #{1}
                                    :called (AtomicBoolean. true)
                                    :opening (delay (CompletableFuture/completedFuture {:proxy :p-own}))}
                            place-l0 {:readers #{1}
                                      :called (AtomicBoolean. true)
                                      :opening (delay (CompletableFuture/completedFuture {:proxy :p-place}))}}
                   :counts {:streaming 2}
                   :reads {1 {:query q
                              :tell (fn [ev] (swap! calls conj ev))
                              :turn (Object.)
                              :sources #{own-l0 place-l0}
                              :opened? true
                              :owed? {:told {:l0 :yes}}}}})
            (AtomicLong. 1))
        step-n (AtomicInteger. 0)
        ;; Simulate concurrent ProxyState updates on :l0's builds-on list while settle! loops 16 times,
        ;; ending on [:l-final] at round 16:
        on-change @#'softland.store.read-path/on-change]
    (with-redefs-fn {#'softland.store.read-path/known-of
                     (fn [_rp* read]
                       (let [n (.getAndIncrement step-n)
                             target (keyword (str "l-" (min n 16)))]
                         (into {}
                               (keep (fn [s]
                                       (cond (= s own-l0) [s (make-own-rows [target])]
                                             (= s place-l0) [s nil]
                                             :else nil)))
                               (:sources read))))
                     #'softland.store.read-path/open-push
                     (fn [_ _ _]
                       ;; Unfinished future: push hasn't arrived yet
                       (CompletableFuture.))}
      #(on-change rp 1 own-l0 [[:put (rows/place-key :l0 :l0 :softland.store/builds-on)]]
                  (make-own-rows []) (make-own-rows [:l-0])))
    (let [read-after (get-in @(.-held rp) [:reads 1])
          wanted-final (sources/own-rows-source :l0 :l-16)]
      (println "  read still in :reads (end-read! NOT called)?     " (some? read-after))
      (println "  callbacks delivered to caller (:ended or :anew): " @calls)
      (println "  read :owed? after settle! bailed at round 16:    " (:owed? read-after))
      (println "  final wanted source (:l-16 own-rows) in :sources?" (contains? (:sources read-after) wanted-final))
      (assert (and (some? read-after)
                   (empty? @calls)
                   (true? (:owed? read-after))
                   (not (contains? (:sources read-after) wanted-final)))
              "Expected wedged read with :owed? true and missing unjoined source!"))))

;; ---------------------------------------------------------------------------
;; PROBE 4: P3 — digest/hex-for-a-yes skips declaration/check rules & bounds,
;; allowing permanently unredeemable standing `yes` grants in $$facts
;; ---------------------------------------------------------------------------

(defn probe-4-unredeemable-yes-grants []
  (println "\n=== PROBE 4: hex-for-a-yes hashes declaration-invalid acts -> unredeemable standing yeses ===")
  (let [layer :lab
        root [:asha layer layer]
        rec0 (:record (gate-decide gate-empty :streaming
                                   (arrived layer :streaming (UUID. 0 1) :asha :s0 root
                                            [{:e layer :k :softland.store/depot :v :streaming}
                                             {:e :asha :k :softland.store/permission :v root}])))
        long-kw (keyword (apply str (repeat 260 \a)))
        big-str (apply str (repeat 70000 \x))
        bad-makers [["4a: 257 facts (> max-facts 256)"
                     (maker-act layer :ben :s1 nil (mapv (fn [i] {:e (keyword (str "e" i)) :k :v :v i}) (range 257)))]
                    ["4b: 70 KB value (> max-value-bytes 65536)"
                     (maker-act layer :ben :s1 nil [{:e :doc :k :body :v big-str}])]
                    ["4c: duplicate place [:e :k] in same act"
                     (maker-act layer :ben :s1 nil [{:e :doc :k :v :v 1} {:e :doc :k :v :v 2}])]
                    ["4d: unknown key in softland.store namespace"
                     (maker-act layer :ben :s1 nil [{:e :doc :k :softland.store/not-a-key :v 1}])]
                    ["4e: 260-char entity keyword (> max-id-text 256)"
                     (maker-act layer :ben :s1 nil [{:e long-kw :k :v :v 1}])]]]
    (doseq [[idx [label m]] (map-indexed vector bad-makers)]
      (let [hex (digest/hex-for-a-yes m)
            yes-id (UUID. 99 idx)
            pa [:ben layer layer root :see-and-act [:act yes-id hex]]
            give-res (gate-decide rec0 :streaming
                                  (arrived layer :streaming (UUID. 1 idx) :asha :s0 root
                                           [{:e yes-id :k :softland.store/act-permission :v pa}]))
            redeem-res (gate-decide (:record give-res) :streaming
                                    (arrived layer :streaming (UUID. 2 idx) :ben :s1 pa (:facts m)))
            stands-after? (chain/stands? layer pa
                                         (select-keys (get-in redeem-res [:record :facts])
                                                      [(chain/place layer pa) (chain/place layer root)]))]
        (println (format "  %-46s -> hex?=%s | grant=%-9s | redeem=%-24s | stuck standing?=%s"
                         label
                         (some? hex)
                         (name (get-in give-res [:outcome :verdict]))
                         (name (get-in redeem-res [:outcome :verdict]))
                         stands-after?))
        (assert (and (string? hex)
                     (= :accepted (get-in give-res [:outcome :verdict]))
                     (not= :accepted (get-in redeem-res [:outcome :verdict]))
                     stands-after?))))))

;; ---------------------------------------------------------------------------
;; PROBE 5: Phantom fact-id [[layer depot act-uuid] k] in use-of-its-yes &
;; 256-fact / 771-write boundary
;; ---------------------------------------------------------------------------

(defn probe-5-phantom-fact-id-and-771-writes []
  (println "\n=== PROBE 5: use-of-its-yes phantom fact-id [[layer depot uuid] 256] & 771-write max ===")
  (let [layer :lab
        root [:asha layer layer]
        entities (mapv #(keyword (str "e-" %)) (range 256))
        setup [(arrived layer :streaming (UUID. 0 1) :asha :s0 root
                        [{:e layer :k :softland.store/depot :v :streaming}
                         {:e :asha :k :softland.store/permission :v root}])
               (arrived layer :streaming (UUID. 0 2) :asha :s0 root
                        [{:e layer :k :softland.store/marked-keys :v #{:tag}}])
               (arrived layer :streaming (UUID. 0 3) :asha :s0 root
                        (mapv (fn [e] {:e e :k :tag :v :old}) entities))]
        rec0 (reduce (fn [r t] (:record (gate-decide r :streaming t))) gate-empty setup)
        yes-id (UUID. 89 256)
        maker-256 (maker-act layer :ben :s1 nil (mapv (fn [e] {:e e :k :tag :v :new}) entities))
        pa [:ben layer layer root :see-and-act [:act yes-id (digest/hex-for-a-yes maker-256)]]
        rec1 (:record (gate-decide rec0 :streaming
                                   (arrived layer :streaming (UUID. 0 4) :asha :s0 root
                                            [{:e yes-id :k :softland.store/act-permission :v pa}])))
        redeem-uuid (UUID. 0 5)
        redeem-act (arrived layer :streaming redeem-uuid :ben :s1 pa (:facts maker-256))
        {:keys [record outcome]} (gate-decide rec1 :streaming redeem-act)
        q (:ok (query/check {:asks [:place yes-id :softland.store/act-permission]
                             :asker :asha :layers [layer]
                             :stores-own-facts true :with #{:writer :permission :time}}))
        yes-row (get (:facts record) (rows/place-key layer yes-id :softland.store/act-permission))
        pub-place-ans (answer/place-answer q layer yes-row)
        [_ phantom-idx] (:fact pub-place-ans)]
    (println "  256-fact marked yes verdict:" (:verdict outcome)
             "| total decision writes (walk docstring max = 771):" (count (:writes outcome)))
    (println "  public read-path place-answer on used yes:" pub-place-ans)
    (println "  returned :fact index:" phantom-idx "| count of (:facts act):" (count (:facts redeem-act))
             "| out-of-bounds of (:facts act)?" (>= phantom-idx (count (:facts redeem-act))))
    (assert (= 771 (count (:writes outcome))))
    (assert (= 256 phantom-idx))))

(probe-1a-gate-histories-own-break)
(probe-1b-phantom-standing-chain)
(probe-2-standing-refusal-divergences)
(probe-3-settle-max-rounds-wedge)
(probe-4-unredeemable-yes-grants)
(probe-5-phantom-fact-id-and-771-writes)
(println "\nALL PURE PROBES IN break_piece_2_yes COMPLETED.")
