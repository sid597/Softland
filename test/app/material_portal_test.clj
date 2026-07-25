(ns app.material-portal-test
  "editable-material P7 — the portal proper. Gates G1–G14.

   The portal's whole claim is that opening one pick answers every question the
   material world can be asked, deterministically, totally, in one roundtrip,
   with no model anywhere on the path. Each gate below attacks one clause of
   that sentence.

   G1 is the package's headline and the shape of P7's required output: the
   question list is a VALUE, and this gate walks it, printing the replayable call
   beside each answered question. A question the portal only claims to answer
   fails here."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.material-portal :as mp]
            [app.server.rama.material-truth :as material-truth]
            [app.server.rama.object-container.facet-master :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.activation-event :as activation-event]
            [app.shared.attention-material :as attention]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.foldable-material :as foldable]
            [app.shared.material-portal :as portal]
            [app.shared.text-body-material :as text-body]))

(def sid {:actor/id "sid" :actor/type :human})
(def alpha "du:block:alpha")
(def beta "du:block:beta")
(def gamma "du:block:gamma")

(defn- now [] (System/currentTimeMillis))

(defn- wearer
  "One appearance snapshot row in the shape `wearers-from-scene-store` produces.
   `facets` is [[master-id revision-id site slot] …] so a test can describe a
   partial wearer — the describe-never-gate cases need entities that wear less
   than the full composition."
  [entity-id facets]
  {:wearer/entity-id entity-id
   :wearer/facets
   (mapv (fn [[master-id revision-id site slot]]
           {:wearer/master-id master-id
            :wearer/revision-id revision-id
            :wearer/attachments [[:derived :x entity-id]]
            :wearer/subjects [entity-id]
            :wearer/contribution-sites [site]
            :wearer/contribution-roles [:box]
            :wearer/contribution-slots [slot]})
         facets)
   :wearer/view-instances [(str "vi-" entity-id)]})

(defn- boot!
  "Bootstrap the three masters this suite reads, the way the deploy-time ingest
   does. Returns {facet → active-revision-id}."
  [rt]
  (into {}
        (map (fn [[spec source slug]]
               (adapter/ensure-master! rt spec)
               [(:facet-master/facet spec)
                (get-in (adapter/ensure-active-source!
                         rt spec source
                         {:request-id (str "p7-" slug)
                          :activation-request-id (str "p7-act-" slug)
                          :time-ms (now)})
                        [:state :active-revision :revision-id])]))
        [[attention/spec attention/strict-bindings-source "att"]
         [foldable/spec (:facet-master/default-source foldable/spec) "fold"]
         [text-body/spec (:facet-master/default-source text-body/spec) "text"]]))

(defn- open!
  ([rt params] (open! rt (fn [req] (fp/serve {:oc-rt rt :rk-rt nil} req)) params))
  ([rt serve-fn params]
   (mp/open {:oc-rt rt :rk-rt nil} serve-fn params)))

;; ===========================================================================
;; G1 — the portal question list, answered one by one with replayable calls
;; ===========================================================================

(deftest g1-every-portal-question-answered-with-a-replayable-call
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]
                                    [foldable/master-id (:foldable revs)
                                     :block/fold-header :header]
                                    [text-body/master-id (:text-body revs)
                                     :block/user-hit-area :body]])
                     (wearer beta [[attention/master-id (:attention revs)
                                    :block/user-hit-area :border]])]
            result (open! rt {:entity-id alpha :wearers wearers})
            rows (:portal/questions result)]

        (testing "every question in DIRECTION's portal list has an answer"
          (is (= [] (portal/unanswered result))
              "a question whose answer path is absent is an unanswered question")
          (is (true? (portal/answered? result)))
          (is (= (count portal/questions) (count rows)))
          (is (every? :question/answered? rows)))

        (testing "each question carries the call that replays it"
          (doseq [q rows]
            (is (string? (:question/call-jvm q)))
            (is (string? (:question/call-console q)))
            (is (string? (:question/ask q)))
            (is (vector? (:question/answers-at q)))))

        ;; P7's output form: the list, answered one by one, printed with its
        ;; replayable calls. This is a receipt, not decoration — it is what the
        ;; gate report quotes.
        (println "\n=== P7 · the portal question list, answered ===")
        (doseq [[i q] (map-indexed vector rows)]
          (println (format "%2d. %-64s %s"
                           (inc i) (:question/ask q)
                           (if (:question/answered? q) "ANSWERED" "MISSING")))
          (println (format "    at %s" (pr-str (:question/answers-at q))))
          (println (format "    jvm     %s" (:question/call-jvm q)))
          (println (format "    console %s" (:question/call-console q))))
        (println "=== end question list ===\n"))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G2 — determinism: equal worlds, byte-equal portals
;; ===========================================================================

(deftest g2-determinism-byte-equal-across-opens
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            p (fn [] (portal/canonical-edn
                      (open! rt {:entity-id alpha :wearers wearers})))
            a (p) b (p)]
        (testing "two opens of an unchanged world are byte-identical"
          (is (= a b))
          (is (= (count a) (count b))))

        (testing "no wall clock reached the result"
          (is (not (str/includes? a "rendered-at-ms"))
              "an embedded projection's :face/rendered-at-ms would make the
               portal's bytes change every millisecond (T-P7-1)"))

        (testing "the canonical bytes do not depend on *print-namespace-maps*"
          ;; the P6 determinism trap: TRUE at a REPL, FALSE in a program, and
          ;; every portal key is in one namespace
          (is (= a (binding [*print-namespace-maps* true]
                     (portal/canonical-edn
                      (open! rt {:entity-id alpha :wearers wearers}))))))

        (testing "a real change DOES change the bytes"
          (material-truth/deviate!
           rt attention/spec alpha {:attention/hit-padding 24.0}
           {:actor sid :time-ms (now)})
          (is (not= a (p)) "a deviation the portal cannot see is a blind portal")))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G3 — batched: one roundtrip, and a sub-serve count that does NOT grow
;; ===========================================================================

(deftest g3-batched-no-client-n+1
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            calls (atom [])
            counting (fn [req]
                       (swap! calls conj (:face req))
                       (fp/serve {:oc-rt rt :rk-rt nil} req))
            one [(wearer alpha [[attention/master-id (:attention revs)
                                 :block/user-hit-area :border]])]
            many (into one
                       (map #(wearer % [[attention/master-id (:attention revs)
                                         :block/user-hit-area :border]
                                        [foldable/master-id (:foldable revs)
                                         :block/fold-header :header]])
                            [beta gamma "du:block:delta" "du:block:epsilon"]))
            _ (reset! calls [])
            r1 (open! rt counting {:entity-id alpha :wearers one})
            n1 (count @calls)
            _ (reset! calls [])
            r2 (open! rt counting {:entity-id alpha :wearers many})
            n2 (count @calls)]

        (testing "the portal declares one client roundtrip and zero client joins"
          (is (= 1 (get-in r1 [:portal/query-plan :plan/client-roundtrips])))
          (is (= 0 (get-in r1 [:portal/query-plan :plan/client-joins])))
          (is (true? (get-in r1 [:portal/query-plan :plan/joins-server-side?]))))

        (testing "the sub-serve count is FIXED — five, whatever the page size"
          (is (= 5 n1) (str "sub-serves: " (pr-str @calls)))
          (is (= n1 n2)
              "a per-wearer or per-master serve would be the N+1 this fence
               forbids, moved server-side instead of removed"))

        (testing "the answer is whole — nothing is left for the client to fetch"
          (is (= [] (portal/unanswered r1)))
          (is (= [] (portal/unanswered r2))))

        (testing "the blast radius counts the whole visible page, not the pick"
          (is (= 1 (get-in r1 [:portal/blast :blast/by-master
                               attention/master-id :blast/counted-over])))
          (is (= 5 (get-in r2 [:portal/blast :blast/by-master
                               attention/master-id :blast/counted-over])))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G4 — totality: a broken section is a PRESENT section with a named error
;; ===========================================================================

(deftest g4-error-cards-never-silence
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            exploding
            (fn [face]
              (fn [req]
                (if (= face (:face req))
                  (throw (ex-info "injected failure" {:face face}))
                  (fp/serve {:oc-rt rt :rk-rt nil} req))))]

        (doseq [face [:facet-materials :material-inspector :interaction-table
                      :material-truth :material-experience]]
          (testing (str "a failing " face " sub-serve error-cards, never hides")
            (let [r (open! rt (exploding face) {:entity-id alpha
                                                :wearers wearers})]
              (is (= [] (portal/unanswered r))
                  "the portal still answers every question")
              (is (seq (:portal/errors r))
                  "and says what broke, at the top level")
              (is (some #(= :portal/section-read-failed (:error/type %))
                        (:portal/errors r)))
              (testing "the floor still renders all cards"
                (let [rendered (mp/render r)]
                  (is (= portal/question-ids (:render/card-ids rendered)))
                  (is (= (count portal/questions)
                         (count (:render/cards rendered)))))))))

        (testing "a sub-serve that returns serve's own error data-context is named"
          (let [r (open! rt
                         (fn [req]
                           (if (= :material-truth (:face req))
                             {:conversation/error :projection-read-failed}
                             (fp/serve {:oc-rt rt :rk-rt nil} req)))
                         {:entity-id alpha :wearers wearers})]
            (is (= [] (portal/unanswered r)))
            (is (some #(= :portal/sub-projection-failed (:error/type %))
                      (:portal/errors r))
                "serve is total by contract; a section receiving its error map
                 must not look merely empty"))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G5 — describe, never gate
;; ===========================================================================

(deftest g5-describe-never-gate
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)]
        (testing "an entity that exists in no store still projects"
          (let [r (open! rt {:entity-id "du:block:never-existed" :wearers []})]
            (is (= [] (portal/unanswered r)))
            (is (false? (get-in r [:portal/identity :entity/found?])))
            (is (= [] (:portal/errors r)))))

        (testing "a nil entity-id still projects"
          (let [r (open! rt {:entity-id nil :wearers []})]
            (is (= [] (portal/unanswered r)))
            (is (false? (get-in r [:portal/identity :entity/addressable?])))))

        (testing "an entity stamping NO facets gets a whole-world description"
          (let [r (open! rt {:entity-id alpha :wearers []})]
            ;; T-P7-3: pricing widens to the registry rather than narrowing to
            ;; nothing — an untyped entity is described, not refused
            (is (= (count facet-masters/master-ids)
                   (get-in r [:portal/query-plan :plan/masters-priced])))
            (is (= [] (portal/unanswered r)))))

        (testing "a PARTIAL wearer (one facet of five) projects and is not typed"
          (let [r (open! rt {:entity-id alpha
                             :wearers [(wearer alpha
                                               [[attention/master-id
                                                 (:attention revs)
                                                 :block/user-hit-area :border]])]})]
            (is (= [] (portal/unanswered r)))
            (is (false? (get-in r [:portal/recipe :recipe/covers-worn-five?])))
            (is (nil? (get-in r [:portal/recipe :recipe/name]))
                "a fragment is described by its id, never named `block`")))

        (testing "nothing in the projection is a permission"
          (let [r (open! rt {:entity-id alpha :wearers []})]
            (is (false? (get-in r [:portal/recipe :recipe/gates?])))
            (is (false? (get-in r [:portal/chrome :chrome/self-editing?])))
            (is (false? (get-in r [:portal/chrome
                                   :chrome/writable-from-inside?]))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G5b — the four silences the falsification pass found
;;
;; Each of these was a real defect in the first working build: the portal was
;; green and answering every question while quietly saying something untrue.
;; They are gated together because they are one class — a value that reads as
;; `nothing here` when the truth is `I cannot explain this`.
;; ===========================================================================

(deftest g5b-no-silent-absences
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            ;; a stamp naming a master this build has never heard of
            alien (wearer alpha [["fm:from-the-future" "rev:whatever"
                                  :block/user-hit-area :border]
                                 [attention/master-id (:attention revs)
                                  :block/user-hit-area :border]])
            r (open! rt {:entity-id alpha :wearers [alien]})]

        (testing "an unknown master is named in the RECIPE too"
          ;; the composition is facet-based and an unknown master names no facet,
          ;; so dropping it is right — dropping it silently is not
          (is (= ["fm:from-the-future"]
                 (get-in r [:portal/recipe :recipe/unknown-masters]))))

        (testing "an empty composition shared by two entities has not recurred"
          (let [nothing (open! rt {:entity-id alpha
                                   :wearers [(wearer alpha [["fm:x" "r" :block/user-hit-area :s]])
                                             (wearer beta [["fm:y" "r" :block/user-hit-area :s]])]})]
            (is (= [] (get-in nothing [:portal/recipe :recipe/facets])))
            (is (false? (get-in nothing [:portal/recipe :recipe/recurred?]))
                "`nothing` recurring is still nothing")))

        (testing "an unknown master is NAMED, never dropped from the answer"
          (let [m (get-in r [:portal/masters "fm:from-the-future"])]
            (is (some? m)
                "a silently absent master reads as `no such fact` when the truth
                 is `a fact I cannot explain`")
            (is (= :unknown-master (:master/error m)))
            (is (false? (:master/valid? m)))))

        (testing "why-this-pixel on an unknown master says found? FALSE"
          (let [w (:portal/why
                   (open! rt {:entity-id alpha :wearers [alien]
                              :why {:material/subject alpha
                                    :material/master "fm:from-the-future"
                                    :material/revision "rev:whatever"
                                    :material/site :block/user-hit-area}}))]
            (is (true? (:why/asked? w)))
            (is (false? (:why/found? w))
                "a confident chain of blanks is a causal trace that lies")
            (is (false? (:why/master-known? w)))))

        (testing "no history is COMPLETE, not truncated"
          ;; `nothing to show` and `more than I showed` are different answers
          (let [fresh (open! rt {:entity-id "du:block:no-history" :wearers []})
                sections (get-in fresh [:portal/truncation :truncation/sections])
                ann (first (filter #(= :announcements (:truncation/section %))
                                   sections))]
            (is (false? (:truncation/truncated? ann))
                "a master with no activation history is not withholding one")))

        (testing "the experience gauge keeps its shape even when the read fails"
          (let [broken (open! rt
                              (fn [req]
                                (if (= :material-experience (:face req))
                                  (throw (ex-info "injected" {}))
                                  (fp/serve {:oc-rt rt :rk-rt nil} req)))
                              {:entity-id alpha :wearers [alien]})
                comp (get-in broken [:portal/experience :experience/composition])]
            (is (every? #(contains? comp %) [:receipt :silver :gold])
                "a reader of the gauge should not have to branch on whether the
                 read succeeded"))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G6 — why this pixel, from any rendered contribution
;; ===========================================================================

(deftest g6-why-this-pixel
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            ;; alpha deviates; beta does not — the two answers must differ
            _ (material-truth/deviate!
               rt attention/spec alpha {:attention/hit-padding 24.0}
               {:actor sid
                :grounds [(activation-event/ground
                           :grounded-in :experience "exp:reply-width")]
                :time-ms (now)})
            base (open! rt {:entity-id alpha :wearers wearers})
            offered (get-in base [:portal/why :why/available-stamps])]

        (testing "the portal names the pixels that CAN be asked about"
          (is (false? (get-in base [:portal/why :why/asked?])))
          (is (seq offered)
              "an unasked why-section that does not say how to ask is a dead end"))

        (testing "every offered stamp resolves to DIRECTION's full causal chain"
          (doseq [stamp offered]
            (let [w (:portal/why (open! rt {:entity-id alpha
                                            :wearers wearers
                                            :why stamp}))]
              (is (true? (:why/asked? w)))
              (is (true? (:why/found? w)))
              ;; entity · placement · attachment · master revision · recipe
              ;; revision · override/pin that won · source path
              (is (= alpha (:why/entity w)))
              (is (contains? w :why/placement))
              (is (some? (:why/attachment w)))
              (is (some? (:why/master-revision-id w)))
              (is (contains? w :why/recipe-revision-id))
              (is (some? (:why/won-by w)))
              (is (string? (:why/source-path w)))
              (is (seq (:why/bindings-at-site w))
                  "what the pixel DOES belongs to why it looks that way"))))

        (testing "the deviation is named as the thing that won"
          (let [w (:portal/why (open! rt {:entity-id alpha :wearers wearers
                                          :why (first offered)}))]
            (is (= :instance-deviation (:why/won-by w)))
            (is (= :instance (:why/tier w)))
            (is (some? (:why/deviation w)))))

        (testing "a STALE pixel is detected — the stamp is older than the wear"
          (let [stale (assoc (first offered) :material/revision "rev:stale")
                w (:portal/why (open! rt {:entity-id alpha :wearers wearers
                                          :why stale}))]
            (is (true? (:why/stale? w))
                "only the stamp remembers what was actually drawn"))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G7 — the resident's briefing IS the projection, and no LLM is on the path
;; ===========================================================================

(deftest g7-briefing-is-the-projection-verbatim
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            r (open! rt {:entity-id alpha :wearers wearers})
            edn (portal/canonical-edn r)
            b (mp/briefing r)]

        (testing "the briefing carries the canonical projection byte for byte"
          (is (str/includes? b edn)
              "EXACTLY this projection — not a summary of it")
          (is (= edn (-> b
                         (str/split #"<projection>") second
                         (str/split #"</projection>") first))))

        (testing "the briefing is deterministic"
          (is (= b (mp/briefing (open! rt {:entity-id alpha
                                           :wearers wearers})))))

        (testing "the summon entry point exists, is total, and briefs identically"
          ;; the gesture is P8's; the seam and its totality are P7's
          (let [via (fp/portal-briefing {:oc-rt rt :rk-rt nil}
                                        {:entity-id alpha :wearers wearers})]
            (is (= b via)
                "the resident's entry point and the human's portal must produce
                 one briefing, or `EXACTLY this projection` is not true"))
          ;; and with NO runtime at all the resident is still briefed — the
          ;; briefing degrades to a world of named absences rather than to nil.
          ;; That is stronger than the totality this gate was written to check:
          ;; `portal-briefing`'s catch is the backstop, the portal's own section
          ;; floors are the actual mechanism, and they hold first.
          (let [none (fp/portal-briefing nil {:entity-id alpha :wearers wearers})]
            (is (string? none))
            (is (str/includes? none "<projection>"))
            (is (str/includes? none ":entity/found? false")
                "an unbriefable world is briefed AS unbriefable, never silently")))

        (testing "the projection says the briefing has no model in its path"
          (is (false? (get-in r [:portal/briefing-of :briefing/llm-in-path?])))
          (is (= 0 (get-in r [:portal/query-plan :plan/llm-calls]))))

        (testing "and the source agrees — no model namespace is reachable"
          (doseq [path ["app/shared/material_portal.cljc"
                        "app/server/rama/material_portal.clj"]]
            (let [src (slurp (io/resource path))
                  ;; a require of the llm module, an anthropic/openai call, or a
                  ;; prompt render would all show up as a symbol reference
                  offenders (->> ["llm-module" "app.server.llm" "anthropic"
                                  "render-autotag-prompt" "autotag-material!"]
                                 (filter #(str/includes? src %)))]
              (is (= [] offenders)
                  (str path " must contain no path to a model: "
                       (pr-str offenders)))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G8 — blast radius BEFORE activation, and the portal writes nothing
;; ===========================================================================

(deftest g8-blast-radius-pre-activation-writes-nothing
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            _ (material-truth/deviate!
               rt attention/spec beta {:attention/hit-padding 30.0}
               {:actor sid :time-ms (now)})
            _ (material-truth/pin!
               rt attention/spec gamma (:attention revs)
               {:actor sid :time-ms (now)})
            wearers (mapv #(wearer % [[attention/master-id (:attention revs)
                                       :block/user-hit-area :border]])
                          [alpha beta gamma])
            before (portal/canonical-edn (adapter/read-master rt attention/spec))
            r (open! rt {:entity-id alpha :wearers wearers})
            after (portal/canonical-edn (adapter/read-master rt attention/spec))
            b (get-in r [:portal/blast :blast/by-master attention/master-id])]

        (testing "opening the portal wrote nothing — byte-identical master"
          (is (= before after))
          (is (true? (get-in r [:portal/blast :blast/writes-nothing?])))
          (is (true? (get-in r [:portal/blast :blast/pre-activation?]))))

        (testing "the radius separates will-move from pinned from deviating"
          (is (= [alpha] (:blast/will-move b)))
          (is (= [gamma] (:blast/pinned b)))
          (is (= [beta] (:blast/deviating b)))
          (is (= 3 (:blast/counted-over b))))

        (testing "future wearers are included BY REFERENCE and said out loud"
          (is (true? (:blast/includes-future-wearers? b))))

        (testing "a subject-scoped activation names a radius of one"
          (let [r2 (open! rt {:entity-id alpha :wearers wearers
                              :scope [:scope/subject alpha]})
                b2 (get-in r2 [:portal/blast :blast/by-master
                               attention/master-id])]
            (is (= [alpha] (:blast/will-move b2)))
            (is (false? (:blast/includes-future-wearers? b2))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G9 — the dull floor: a code rendering no data revision can break
;; ===========================================================================

(deftest g9-code-floor-drill-pure
  (testing "the floor renders the SAME cards from any value at all"
    (doseq [garbage [nil
                     {}
                     "not a map"
                     42
                     [:a :b]
                     {:portal/identity "a string where a map belongs"}
                     {:portal/masters [:not :a :map]
                      :portal/recipe 7
                      :portal/bindings nil
                      :portal/truncation {:truncation/sections "nope"}
                      :portal/blast {:blast/by-master "nope"}
                      :portal/recovery {:recovery/offers 3}
                      :portal/chrome {:chrome/wears :nope}}]]
      (let [r (portal/render-model garbage)]
        (is (= portal/question-ids (:render/card-ids r))
            "card SET and ORDER come from code, not from data")
        (is (= (count portal/questions) (count (:render/cards r))))
        (is (every? #(vector? (:card/rows %)) (:render/cards r)))
        (is (true? (:render/floor? r)))
        (is (string? (:render/title r))))))

  (testing "a card whose section is hostile carries a named render error"
    (let [hostile (reify clojure.lang.ILookup
                    (valAt [_ _] (throw (ex-info "hostile" {})))
                    (valAt [_ _ _] (throw (ex-info "hostile" {}))))
          r (portal/render-model {:portal/identity hostile})]
      (is (= (count portal/questions) (count (:render/cards r))))
      (is (some (fn [c] (some :row/error? (:card/rows c))) (:render/cards r))
          "a throwing section must card, never propagate")))

  (testing "the briefing is total over garbage too"
    (doseq [garbage [nil {} "x"]]
      (is (string? (portal/briefing garbage))))))

(deftest g9b-code-floor-drill-durable
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            before (open! rt {:entity-id alpha :wearers wearers})
            ;; a malformed candidate is retained durably and its activation is
            ;; refused — the P1 drill, re-run because the kernel grew a portal
            drill (adapter/malformed-drill! rt attention/spec "p7-floor-drill")
            after (open! rt {:entity-id alpha :wearers wearers :drill? true})]

        (testing "the drill's candidate is durable and did NOT become active"
          (is (some? (:candidate-revision-id drill)))
          (is (seq (:activation-errors drill))))

        (testing "the worn surface is unharmed — same active revision"
          (is (= (get-in before [:portal/masters attention/master-id
                                 :master/active-revision-id])
                 (get-in after [:portal/masters attention/master-id
                                :master/active-revision-id]))))

        (testing "the portal still answers everything, and names the candidate"
          (is (= [] (portal/unanswered after)))
          (is (true? (get-in after [:portal/masters attention/master-id
                                    :master/candidate?]))
              "latest ≠ active is exactly what a rejected candidate looks like")
          (is (= (:candidate-revision-id drill)
                 (get-in after [:portal/masters attention/master-id
                                :master/candidate-revision-id]))))

        (testing "and the floor rendering is intact after the drill"
          (let [rendered (mp/render after)]
            (is (= portal/question-ids (:render/card-ids rendered)))
            (is (every? #(seq (:card/rows %)) (:render/cards rendered))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G10 — previous-revision recovery, offered and executable
;; ===========================================================================

(deftest g10-previous-revision-recovery
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            r (open! rt {:entity-id alpha :wearers wearers})
            offer (first (filter #(= attention/master-id (:recovery/master-id %))
                                 (get-in r [:portal/recovery :recovery/offers])))]

        (testing "the portal offers a previous revision to go back to"
          (is (some? offer))
          (is (= :rollback (:recovery/kind offer)))
          (is (string? (:recovery/to-revision-id offer)))
          (is (not= (:recovery/from-revision-id offer)
                    (:recovery/to-revision-id offer)))
          (is (string? (:recovery/call offer))
              "T-P7-5: a recovery a reader cannot execute is decoration"))

        (testing "and the offer is EXECUTABLE — rollback is another activation"
          (let [act (adapter/activate!
                     rt attention/spec (:recovery/to-revision-id offer)
                     {:kind :rollback :actor sid :time-ms (now)
                      :request/id "p7-recovery-exec"})
                r2 (open! rt {:entity-id alpha :wearers wearers})]
            (is (= :accepted (get-in act [:decision :status]))
                (pr-str (:decision act)))
            (is (= (:recovery/to-revision-id offer)
                   (get-in r2 [:portal/masters attention/master-id
                               :master/active-revision-id]))
                "the previous revision is worn again, from inside the portal's
                 own answer")))

        (testing "the code floor is named beneath every offer"
          (is (contains? (get-in r [:portal/recovery :recovery/code-floor])
                         attention/master-id))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G11 — latest ≠ candidate ≠ active ≠ pinned ≠ previous, and standable history
;; ===========================================================================

(deftest g11-five-states-distinct-and-history-standable
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            v2 (:attention revs)
            ;; a candidate that is NOT activated: latest ≠ active
            cand (adapter/import-candidate!
                  rt attention/spec
                  (pr-str (assoc (:facet-master/default-form attention/spec)
                                 :attention/hit-padding 11.0))
                  {:request/id "p7-candidate"})
            _ (material-truth/pin! rt attention/spec beta v2
                                   {:actor sid :time-ms (now)})
            wearers (mapv #(wearer % [[attention/master-id v2
                                       :block/user-hit-area :border]])
                          [alpha beta])
            r (open! rt {:entity-id beta :wearers wearers})
            m (get-in r [:portal/masters attention/master-id])]

        (testing "all five states are separate keys, none inferred"
          (is (= v2 (:master/active-revision-id m)))
          (is (= (:revision-id cand) (:master/latest-revision-id m)))
          (is (true? (:master/candidate? m)))
          (is (= (:revision-id cand) (:master/candidate-revision-id m)))
          (is (true? (:master/pinned-here? m)))
          (is (= v2 (:master/pinned-revision-id m)))
          (is (string? (:master/previous-revision-id m)))
          (is (not= (:master/active-revision-id m)
                    (:master/previous-revision-id m))))

        (testing "history is standable — and the cuts are NAMEABLE (T-P7-4)"
          (let [cuts (get-in r [:portal/history :history/available-cuts])
                mine (get cuts attention/master-id)]
            (is (seq mine) "a cut you cannot name is not standable")
            (is (= :pointer-revision-id
                   (get-in r [:portal/history :history/named-by])))
            (let [at (open! rt {:entity-id beta :wearers wearers
                                :cut {attention/master-id (last mine)}})
                  stood (get-in at [:portal/history :history/masters
                                    attention/master-id])]
              (is (true? (get-in at [:portal/history :history/standable?])))
              (is (true? (:found? stood)))
              (is (string? (:worn-revision-id stood)))
              (is (= {attention/master-id (last mine)}
                     (get-in at [:portal/history :history/cut]))))))

        (testing "an unnameable cut answers found? false, never a throw"
          (let [at (open! rt {:entity-id beta :wearers wearers
                              :cut {attention/master-id "rev:does-not-exist"}})]
            (is (= [] (portal/unanswered at)))
            (is (false? (get-in at [:portal/history :history/standable?]))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G12 — the recipe: exhaust, distribution-measured, named only when earned
;; ===========================================================================

(deftest g12-recipe-is-exhaust
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            ;; the worn five on this suite's three booted masters is not the
            ;; whole five, so the composition is built explicitly here — the
            ;; recipe is derived from STAMPS, which is exactly what lets a test
            ;; describe a composition the suite has not booted
            five (fn [eid]
                   (wearer eid
                           [[attention/master-id (:attention revs)
                             :block/user-hit-area :border]
                            [foldable/master-id (:foldable revs)
                             :block/fold-header :header]
                            [text-body/master-id (:text-body revs)
                             :block/user-hit-area :body]
                            ["fm:positioned" "rev:positioned"
                             :block/user-hit-area :position]
                            ["fm:threaded" "rev:threaded"
                             :block/user-hit-area :column]]))]

        (testing "one wearer only: nothing has recurred, nothing is named"
          (let [r (open! rt {:entity-id alpha :wearers [(five alpha)]})
                rec (:portal/recipe r)]
            (is (false? (:recipe/recurred? rec)))
            (is (= :single (:recipe/recurrence-class rec)))
            (is (nil? (:recipe/name rec)))
            (is (true? (:recipe/covers-worn-five? rec)))))

        (testing "two distinct entities, one composition: recurred, and NAMED"
          (let [r (open! rt {:entity-id alpha
                             :wearers [(five alpha) (five beta)]})
                rec (:portal/recipe r)]
            (is (true? (:recipe/recurred? rec)))
            (is (= "block" (:recipe/name rec)))
            (is (= :working-scaffolding (:recipe/name-status rec)))
            (is (= :sid (:recipe/name-authority rec))
                "DIRECTION §Only-Sid: names finalize by recurrence, Sid names")
            (is (= 2 (get-in rec [:recipe/distribution
                                  :distribution/distinct-entities])))
            (testing "and the recurrence class is honest about WHY it recurred"
              (is (= :derived-uniform (:recipe/recurrence-class rec))
                  "one composition across the whole basis is code attaching
                   uniformly — not the land choosing a type"))))

        (testing "two DIFFERENT compositions: the class becomes :lived"
          (let [r (open! rt {:entity-id alpha
                             :wearers [(five alpha) (five beta)
                                       (wearer gamma
                                               [[attention/master-id
                                                 (:attention revs)
                                                 :block/user-hit-area :border]])]})
                rec (:portal/recipe r)]
            (is (= :lived (:recipe/recurrence-class rec)))
            (is (= 2 (get-in rec [:recipe/distribution
                                  :distribution/distinct-compositions])))
            (testing "distribution, not count — the histogram is served"
              (is (= 2 (count (get-in rec [:recipe/distribution
                                           :distribution/histogram])))))))

        (testing "a deviating wearer stays in the SAME composition"
          (material-truth/deviate!
           rt attention/spec beta {:attention/hit-padding 30.0}
           {:actor sid :time-ms (now)})
          (let [r (open! rt {:entity-id alpha
                             :wearers [(five alpha) (five beta)]})]
            (is (= 2 (get-in r [:portal/recipe :recipe/distribution
                                :distribution/distinct-entities]))
                "a deviation is a variation of a type, not a different type"))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G13 — truncation metadata, and the experience ladder
;; ===========================================================================

(deftest g13-truncation-and-experience
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [revs (boot! rt)
            wearers [(wearer alpha [[attention/master-id (:attention revs)
                                     :block/user-hit-area :border]])]
            r (open! rt {:entity-id alpha :wearers wearers})
            t (:portal/truncation r)]

        (testing "every bounded read declares its bound in ONE place"
          (is (contains? t :truncation/complete?))
          (is (= #{:announcements :experience :instances :placement
                   :revision-trail :wearers}
                 (set (map :truncation/section (:truncation/sections t)))))
          (doseq [s (:truncation/sections t)]
            (is (contains? s :truncation/truncated?))
            (is (string? (:truncation/note s)))))

        (testing "the experience ladder's composition is exposed"
          (let [e (:portal/experience r)]
            (is (contains? e :experience/composition))
            (is (contains? e :experience/count))
            (is (contains? e :experience/origin-link-count))
            (is (every? #(contains? (:experience/composition e) %)
                        [:receipt :silver :gold])
                "receipt/silver/gold never fuse (DIRECTION §one epistemic
                 ladder), so all three are counted separately")))

        (testing "the query plan is honest about its own reads"
          (let [p (:portal/query-plan r)]
            (is (= 5 (count (:plan/sub-projections p))))
            (is (seq (:plan/known-duplicate-reads p))
                "a declared duplicate read is a fact; a hidden one is a lie"))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G14 — the portal is read-only by construction
;; ===========================================================================

(defn- code-without-strings
  "Source with every string literal removed.

   The naive form of this scan failed on its first run, and the failure was
   instructive: it flagged `activate!` in `material_portal.clj` — inside the
   RECOVERY OFFER's paste-able call text, which is the opposite of a write path
   (it is the portal telling a reader how to perform one elsewhere). A gate that
   cannot tell a call from a quotation is measuring the wrong thing, so string
   literals come out before the scan runs."
  [src]
  (str/replace src #"\"(?:[^\"\\]|\\.)*\"" "\"\""))

(deftest g14-portal-cannot-write
  (testing "no write verb is CALLED in either portal namespace"
    (doseq [path ["app/shared/material_portal.cljc"
                  "app/server/rama/material_portal.clj"]]
      (let [src (code-without-strings (slurp (io/resource path)))
            ;; the layer's write verbs, as CALL forms. A portal that could reach
            ;; any of these would be P8 arriving early, without its fences.
            offenders (->> ["deviate!" "release-deviation!" "pin!" "unpin!"
                            "activate!" "import-candidate!" "ensure-master!"
                            "ensure-active-source!" "malformed-drill!"
                            "append-object-container-request!"
                            "register-instance-masters!"
                            "rebuild-instance-registry!"]
                           (filter #(str/includes? src %)))]
        (is (= [] offenders)
            (str path " must contain no write verb: " (pr-str offenders))))))

  (testing "and BEHAVIOURALLY: no portal mode moves a single durable byte"
    ;; the text scan proves nothing about what the sub-serves do; this does
    (let [rt (ocr/start-object-container-runtime!)]
      (try
        (let [revs (boot! rt)
              _ (material-truth/deviate!
                 rt attention/spec alpha {:attention/hit-padding 24.0}
                 {:actor sid :time-ms (now)})
              _ (material-truth/pin! rt attention/spec beta (:attention revs)
                                     {:actor sid :time-ms (now)})
              wearers (mapv #(wearer % [[attention/master-id (:attention revs)
                                         :block/user-hit-area :border]])
                            [alpha beta])
              snapshot (fn []
                         (portal/canonical-edn
                          (mapv (fn [spec]
                                  [(adapter/read-master rt spec)
                                   (adapter/instance-state rt spec alpha)
                                   (adapter/instance-state rt spec beta)])
                                facet-masters/specs)))
              before (snapshot)
              cuts (-> (open! rt {:entity-id alpha :wearers wearers})
                       (get-in [:portal/history :history/available-cuts])
                       (get attention/master-id))]
          ;; every mode the portal has
          (doseq [params [{:entity-id alpha :wearers wearers}
                          {:entity-id alpha :wearers wearers :drill? true}
                          {:entity-id alpha :wearers wearers
                           :scope [:scope/subject alpha]}
                          {:entity-id alpha :wearers wearers
                           :cut {attention/master-id (last cuts)}}
                          {:entity-id alpha :wearers wearers
                           :why {:material/subject alpha
                                 :material/master attention/master-id
                                 :material/revision (:attention revs)
                                 :material/site :block/user-hit-area}}
                          {:entity-id nil :wearers []}
                          {:entity-id "du:block:unknown" :wearers wearers}]]
            (open! rt params))
          (is (= before (snapshot))
              "opening the portal in any mode must leave the world byte-identical"))
        (finally (ocr/close-object-container-runtime! rt)))))

  (testing "portal self-editing is declared OFF (P7's fence; P8+ opens it)"
    (let [r (portal/render-model
             {:portal/chrome {:chrome/self-editing? false}})]
      (is (some (fn [c] (and (= :chrome (:card/id c))
                             (some #(= "false" (:row/value %)) (:card/rows c))))
                (:render/cards r))))))
