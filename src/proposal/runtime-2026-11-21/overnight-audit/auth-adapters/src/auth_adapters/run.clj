(ns auth-adapters.run
  "Runs the committed, unmodified `auth-blind` property suite against:
     1. `spine/runtime-oracle/src/softland/auth/client.clj`
     2. `test-auth/softland/auth/reference.clj`
   plus differential multi-seed / edge-case probes to surface every difference
   between `SPEC.md` (as read by `auth-blind`), `softland.auth.client`, and
   `softland.auth.reference`.

   Neither `auth-blind/` nor either target implementation is modified."
  (:require [auth-blind.generators :as g]
            [auth-blind.model :as blind-model]
            [auth-blind.properties :as props]
            [auth-blind.run :as blind-run]
            [clojure.pprint :as pprint]
            [clojure.test.check :as tc]
            [com.rpl.rama :as rama]
            [softland.auth.client :as oracle]
            [softland.auth.reference :as ref])
  (:import [java.util Map]
           [java.util.concurrent ExecutorService]))

(deftype FakeOracleCluster [deployed])
(defrecord SampleRecord [person at])

(def oracle-auth
  {:open           oracle/open
   :close!         oracle/close!
   :invite         oracle/invite
   :enter          oracle/enter
   :own-layer-made oracle/own-layer-made
   :begin-sign-in  oracle/begin-sign-in
   :replace-secret oracle/replace-secret
   :end-sign-in    oracle/end-sign-in
   :make-actor     oracle/make-actor
   :end-actor      oracle/end-actor
   :open-session   oracle/open-session
   :close-session  oracle/close-session
   :ask            oracle/ask
   :answer-ask     oracle/answer-ask
   :who-is         oracle/who-is
   :ask-of         oracle/ask-of
   :under          oracle/under
   :watch-root     oracle/watch-root})

(defn new-oracle-world! []
  (let [c (->FakeOracleCluster #{"auth"})]
    {:cluster c
     :module-name "auth"
     :cleanup! (fn []
                 (let [^Map stores @(resolve 'softland.auth.client/stores)]
                   (locking stores
                     (when-let [by-mod (.remove stores c)]
                       (doseq [[_ {:keys [^ExecutorService notifier]}] @by-mod]
                         (when notifier (.shutdownNow notifier)))))))}))

(def reference-auth
  {:open           ref/open
   :close!         ref/close!
   :invite         ref/invite
   :enter          ref/enter
   :own-layer-made ref/own-layer-made
   :begin-sign-in  ref/begin-sign-in
   :replace-secret ref/replace-secret
   :end-sign-in    ref/end-sign-in
   :make-actor     ref/make-actor
   :end-actor      ref/end-actor
   :open-session   ref/open-session
   :close-session  ref/close-session
   :ask            ref/ask
   :answer-ask     ref/answer-ask
   :who-is         ref/who-is
   :ask-of         ref/ask-of
   :under          ref/under
   :watch-root     ref/watch-root})

(defn new-reference-world! []
  {:cluster (ref/open)
   :module-name "auth"
   :cleanup! nil})

(defn- print-failures [label suite-res]
  (let [failed (remove :pass? (:results suite-res))]
    (println (format "\nSummary for %s: %d/%d passed, %d failed."
                     label
                     (- (count (:results suite-res)) (count failed))
                     (count (:results suite-res))
                     (count failed)))
    (doseq [f failed]
      (println "  FAILED:" (:id f) "—" (:sentence f))
      (pprint/pprint (select-keys f [:result :num-tests :seed :shrunk :threw :message])))))

(defn run-targeted-diff-probes
  "Runs deterministic micro-scenarios across all three implementations
  (`auth-blind.model`, `softland.auth.client`, `softland.auth.reference`)
  to pinpoint every exact behavioral divergence."
  []
  (println "\n=== 3. Targeted 3-Way Differential Probes (blind-model vs runtime-oracle vs test-auth/reference) ===")
  (let [blind (blind-model/make-auth)
        run-3 (fn [label scenario-fn]
                (let [b-world (let [w (blind-model/make-world "auth")] {:cluster w :module-name "auth" :cleanup! nil})
                      o-world (new-oracle-world!)
                      r-world (new-reference-world!)
                      run-1 (fn [auth w]
                              (try
                                (let [c1 ((:open auth) (:cluster w) (:module-name w))
                                      c2 ((:open auth) (:cluster w) (:module-name w))]
                                  (try
                                    (scenario-fn auth (:cluster w) c1 c2)
                                    (finally
                                      ((:close! auth) c1)
                                      ((:close! auth) c2))))
                                (catch Throwable t
                                  {:threw (.getName (class t)) :msg (.getMessage t)})
                                (finally
                                  (when-let [cl (:cleanup! w)] (cl)))))
                      rb (run-1 blind b-world)
                      ro (run-1 oracle-auth o-world)
                      rr (run-1 reference-auth r-world)
                      same? (= rb ro rr)]
                  (println (format "  [%s] %s" (if same? "SAME" "DIFF") label))
                  (when-not same?
                    (println "    blind-model     :" (pr-str rb))
                    (println "    runtime-oracle  :" (pr-str ro))
                    (println "    test-auth/ref   :" (pr-str rr)))))
        p1  (g/trial-uuid 900 1)
        p2  (g/trial-uuid 900 2)
        l1  (g/trial-uuid 900 3)
        u1  (g/trial-uuid 900 4)
        m1  (g/trial-uuid 900 5)
        g1  (g/trial-uuid 900 6)
        g2  (g/trial-uuid 900 7)
        s1  (g/trial-uuid 900 8)
        a1  (g/trial-uuid 900 9)
        h1  (g/trial-hash 900 1)
        h2  (g/trial-hash 900 2)
        h3  (g/trial-hash 900 3)
        cd1 "ABCDEFGH"]

    ;; Probe A: open with unknown module name on valid cluster
    (run-3 "A. (open valid-cluster \"missing-module\")"
           (fn [auth cluster _ _]
             (let [r ((:open auth) cluster "missing-module")]
               (if (map? r) (select-keys r [:could-not]) :returned-client))))

    ;; Probe B: call with non-client first arg (e.g. (under nil p1), (invite :bad {...}))
    (run-3 "B. Non-client handle passed to read and write"
           (fn [auth _ _ _]
             {:under-nil ((:under auth) nil p1)
              :invite-kw ((:invite auth) :not-a-client {:person p1 :claim {:by :email :email "a@b.co"}
                                                        :invited-by :system :at 100})}))

    ;; Probe C: unknown extra keys on otherwise valid write and read maps
    (run-3 "C. Unknown extra key on input map (:extra 1)"
           (fn [auth _ c1 _]
             {:invite-extra ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"}
                                                :invited-by :system :at 100 :extra 1})
              :under-after  ((:under auth) c1 p1)
              :who-is-extra ((:who-is auth) c1 {:root p1 :sign-in g1 :session nil :extra 1})}))

    ;; Probe D: omitted -or-nil keys vs explicit nil keys (e.g. who-is without :session, enter without :code-hash, begin-sign-in without :label, answer-ask without :new-agent)
    (run-3 "D. Omitted `-or-nil` keys on enter / begin-sign-in / who-is / answer-ask"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             (let [ent-omit ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co"
                                               :own-layer l1 :making {:uuid u1 :session m1} :at 101})
                   _ ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                        :own-layer l1 :making {:uuid u1 :session m1} :at 101})
                   bsi-omit ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                                       :hash h1 :until 500 :at 102})
                   _ ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                                :hash h1 :until 500 :label nil :at 102})
                   who-omit (let [r ((:who-is auth) c1 {:root p1 :sign-in g1})]
                              (if (:could-not r) r :accepted))
                   _ ((:ask auth) c1 {:code cd1 :hash h2 :asks "cli" :ends 500 :at 102})
                   ans-omit ((:answer-ask auth) c1 {:code cd1 :yes? false :root p1 :actor p1
                                                    :sign-in g2 :until 500 :at 103})]
               {:enter-omit ent-omit
                :bsi-omit bsi-omit
                :who-is-omit who-omit
                :ans-omit ans-omit})))

    ;; Probe E: Uppercase characters in email
    (run-3 "E. Uppercase characters in email (\"Alice@Example.com\")"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "Alice@Example.com"}
                                 :invited-by :system :at 100})))

    ;; Probe F: :making with non-UUID :session (e.g. string "store-sess-1")
    (run-3 "F. `:making` with non-UUID `:session` (\"store-sess-1\")"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session "store-sess-1"} :at 101})))

    ;; Probe G: Defrecord passed instead of plain map
    (run-3 "G. Record instance (`defrecord`) passed where plain input map is expected"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:own-layer-made auth) c1 (->SampleRecord p1 102))))

    ;; Probe H: Repeat of `enter` after `own-layer-made` — does `:own-layer-made?` return true or original false?
    (run-3 "H. Repeat of exact first `enter` after `own-layer-made` was recorded"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             (let [e1 ((:enter auth) c1 {:account [:google "sub-1"] :says {"name" "A"} :email "a@b.co" :code-hash nil
                                         :own-layer l1 :making {:uuid u1 :session m1} :at 101})
                   _  ((:own-layer-made auth) c1 {:person p1 :at 102})
                   e2 ((:enter auth) c1 {:account [:google "sub-1"] :says {"name" "A"} :email "a@b.co" :code-hash nil
                                         :own-layer l1 :making {:uuid u1 :session m1} :at 101})]
               {:e1-made? (:own-layer-made? e1) :e2-made? (:own-layer-made? e2)})))

    ;; Probe I: Repeat of `begin-sign-in` (with initial hash h1) AFTER `replace-secret` changed hash to h2
    (run-3 "I. Repeat of `begin-sign-in` (initial hash h1) after `replace-secret` moved hash to h2"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                        :hash h1 :until 500 :label "web" :at 102})
             ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                         :until 600 :previous-until 200 :at 103})
             {:repeat-h1 ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                                    :hash h1 :until 500 :label "web" :at 102})
              :repeat-h2 ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                                    :hash h2 :until 500 :label "web" :at 102})}))

    ;; Probe J: `replace-secret` whose new `:until <= :at`
    (run-3 "J. `replace-secret` whose new `:until` <= `:at` (expires immediately)"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                        :hash h1 :until 500 :label "web" :at 102})
             ((:open-session auth) c1 {:root p1 :sign-in g1 :session s1 :until 500 :at 103})
             (let [rep ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                                   :until 110 :previous-until 200 :at 110})
                   w   ((:who-is auth) c1 {:root p1 :sign-in g1 :session s1})
                   u   ((:under auth) c1 p1)]
               {:rep rep
                :sign-in-ended? (get-in w [:sign-in :ended?])
                :session-closed? (get-in w [:session :closed?])
                :under-sign-ins (count (:sign-ins u))})))

    ;; Probe K: Ask created with `ends <= at` ("never live"), then answered by slower clock (`answer-at < ends`)
    (run-3 "K. Ask with `ends <= at` at creation, then `answer-ask` with slower clock `at' < ends`"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:ask auth) c1 {:code cd1 :hash h1 :asks "cli" :ends 200 :at 250})
             ((:answer-ask auth) c1 {:code cd1 :yes? true :root p1 :actor p1 :new-agent nil
                                     :sign-in g1 :until 900 :at 150})))

    ;; Probe L: Second different `answer-ask` on an ALREADY answered ask when `at >= ends` or invalid actor
    (run-3 "L. Second different `answer-ask` on already-answered ask after `:ends` and from unknown root"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:ask auth) c1 {:code cd1 :hash h1 :asks "cli" :ends 200 :at 102})
             ((:answer-ask auth) c1 {:code cd1 :yes? true :root p1 :actor p1 :new-agent nil
                                     :sign-in g1 :until 900 :at 150})
             {:same-after-ends  ((:answer-ask auth) c1 {:code cd1 :yes? true :root p1 :actor p1 :new-agent nil
                                                        :sign-in g1 :until 900 :at 250})
              :other-after-ends ((:answer-ask auth) c1 {:code cd1 :yes? false :root p1 :actor p1 :new-agent nil
                                                        :sign-in g2 :until 900 :at 250})
              :other-bad-root   ((:answer-ask auth) c1 {:code cd1 :yes? false :root p2 :actor p2 :new-agent nil
                                                        :sign-in g2 :until 900 :at 160})}))

    ;; Probe M: `answer-ask` with `:yes? false` and nil `:actor` / `:sign-in` / `:until`
    (run-3 "M. `answer-ask` with `:yes? false` and nil `:actor`, `:sign-in`, `:until`"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:ask auth) c1 {:code cd1 :hash h1 :asks "cli" :ends 500 :at 102})
             ((:answer-ask auth) c1 {:code cd1 :yes? false :root p1 :actor nil :new-agent nil
                                     :sign-in nil :until nil :at 150})))

    ;; Probe N: `open-session` repeat with a DIFFERENT `:driven-by` when session is open vs closed, and `until <= at` on repeat
    (run-3 "N. `open-session` repeat with smaller `:until` and different `:driven-by`"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                        :hash h1 :until 900 :label nil :at 102})
             ((:open-session auth) c1 {:root p1 :sign-in g1 :session s1 :until 500 :at 103
                                       :driven-by {:kind :person}})
             (let [rep1 ((:open-session auth) c1 {:root p1 :sign-in g1 :session s1 :until 300 :at 104
                                                  :driven-by {:kind :model :model "gpt"}})
                   w1   ((:who-is auth) c1 {:root p1 :sign-in g1 :session s1})
                   rep2 ((:open-session auth) c1 {:root p1 :sign-in g1 :session s1 :until 700 :at 105
                                                  :driven-by {:kind :model :model "gpt"}})
                   w2   ((:who-is auth) c1 {:root p1 :sign-in g1 :session s1})]
               {:rep1 rep1 :w1 (:session w1) :rep2 rep2 :w2 (:session w2)})))

    ;; Probe O: `make-actor` depth boundary (32 vs 31 below root)
    (run-3 "O. `make-actor` depth boundary (person=0, agent-1..32, then 33rd)"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             (let [chain (reduce (fn [acc depth]
                                   (let [parent (:tip acc)
                                         child  (g/trial-uuid 950 depth)
                                         r      ((:make-actor auth) c1 {:root p1 :actor child :made-by parent
                                                                        :label (str "d" depth) :at (+ 101 depth)})]
                                     (assoc acc :tip child (keyword (str "d" depth)) r)))
                                 {:tip p1}
                                 (range 1 34))]
               (select-keys chain [:d31 :d32 :d33]))))

    ;; Probe P: `replace-secret` hash cycle H1 -> H2 -> H1, then replaying first replacement H1 -> H2
    (run-3 "P. `replace-secret` hash cycle H1->H2->H1 then replay H1->H2"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                        :hash h1 :until 900 :label nil :at 102})
             (let [r1 ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                                  :until 910 :previous-until 200 :at 103})
                   r2 ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h2 :new-hash h1
                                                  :until 920 :previous-until 210 :at 104})
                   r1-again ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                                        :until 910 :previous-until 200 :at 103})]
               {:r1 r1 :r2 r2 :r1-again r1-again
                :now-hash (get-in ((:who-is auth) c1 {:root p1 :sign-in g1 :session nil}) [:sign-in :hash])})))

    ;; Probe Q: `replace-secret` late replay AFTER sign-in expired (`:not-live` vs `:wrong-secret`)
    (run-3 "Q. Outdated `replace-secret` replayed after sign-in ended (`:not-live` vs `:wrong-secret`)"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:begin-sign-in auth) c1 {:root p1 :actor p1 :sign-in g1 :kind :browser
                                        :hash h1 :until 900 :label nil :at 102})
             ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                         :until 910 :previous-until 200 :at 103})
             ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h2 :new-hash h3
                                         :until 920 :previous-until 210 :at 104})
             ((:end-sign-in auth) c1 {:root p1 :sign-in g1 :at 105})
             ((:replace-secret auth) c1 {:root p1 :sign-in g1 :old-hash h1 :new-hash h2
                                         :until 910 :previous-until 200 :at 106})))

    ;; Probe R: `answer-ask` with `:new-agent` when root has >= 32 actors (new-agent is direct child of person, depth=1)
    (run-3 "R. `answer-ask` with `:new-agent {:label \"\" :extra 1}` (unknown key inside `:new-agent`)"
           (fn [auth _ c1 _]
             ((:invite auth) c1 {:person p1 :claim {:by :email :email "a@b.co"} :invited-by :system :at 100})
             ((:enter auth) c1 {:account [:google "sub-1"] :says {} :email "a@b.co" :code-hash nil
                                :own-layer l1 :making {:uuid u1 :session m1} :at 101})
             ((:ask auth) c1 {:code cd1 :hash h1 :asks "cli" :ends 500 :at 102})
             ((:answer-ask auth) c1 {:code cd1 :yes? true :root p1 :actor a1
                                     :new-agent {:label "bot" :extra 1}
                                     :sign-in g1 :until 900 :at 103})))))

(defn run-against-reference-breaks
  "Cross-checks how many of `test-auth/softland/auth/reference.clj`'s 20
  independent `breaks` variants are caught by the committed `auth-blind`
  properties (excluding `:no-call-throws`, which already fails on unmodified
  `reference/open`)."
  [seed]
  (println "\n=== 4. Cross-Check: Committed `auth-blind` Suite vs `test-auth/reference/breaks` (20 variants) ===")
  (let [orig-step ref/step
        usable-props (remove #(= :no-call-throws (:id %)) props/properties)
        outcomes
        (mapv (fn [[break-key desc]]
                (let [caught-by
                      (with-redefs [ref/step (fn
                                               ([s c] (orig-step s c break-key))
                                               ([s c b] (orig-step s c (or b break-key))))]
                        (some (fn [{:keys [id trials make-prop]}]
                                (let [res (tc/quick-check trials
                                                          (make-prop reference-auth new-reference-world!)
                                                          :seed seed
                                                          :max-size 45)]
                                  (when-not (:pass? res) id)))
                              usable-props))]
                  (println (format "  [%s] %-32s -> %s"
                                   (if caught-by "CAUGHT" "MISSED")
                                   (name break-key)
                                   (if caught-by (name caught-by) desc)))
                  {:id break-key :caught? (some? caught-by) :caught-by caught-by}))
              ref/breaks)]
    (println (format "Cross-check result: %d/%d of `test-auth/reference/breaks` caught by `auth-blind`."
                     (count (filter :caught? outcomes))
                     (count outcomes)))))

(defn -main [& args]
  (let [seed (if (seq args) (parse-long (first args)) 20261121)]
    (println "=== 1. Running Committed Blind Suite against `spine/runtime-oracle/src/softland/auth/client.clj` ===")
    (with-redefs [rama/deployed-module-names
                  (fn [cluster]
                    (if (instance? FakeOracleCluster cluster)
                      (.-deployed ^FakeOracleCluster cluster)
                      (throw (IllegalArgumentException. "Not a Rama cluster handle"))))]
      (let [oracle-res (blind-run/run-suite oracle-auth {:new-world! new-oracle-world! :seed seed})]
        (print-failures "runtime-oracle/client.clj" oracle-res))

      (println "\n=== 2. Running Committed Blind Suite against `test-auth/softland/auth/reference.clj` ===")
      (let [ref-res (blind-run/run-suite reference-auth {:new-world! new-reference-world! :seed seed})]
        (print-failures "test-auth/reference.clj" ref-res))

      (run-targeted-diff-probes)
      (run-against-reference-breaks seed))
    (System/exit 0)))
