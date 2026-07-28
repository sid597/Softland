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
            [com.rpl.rama :refer [foreign-select foreign-select-one]]
            [com.rpl.rama.path :refer [keypath MAP-VALS]]
            [app.server.cascade :as cascade]
            [app.server-jetty :as sj]
            [app.server.episode :as episode]
            [app.server.rama.core :as core]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.git-spine :as git-spine]
            [app.server.rama.material-circulation :as circulation]
            [app.server.rama.material-portal :as mp]
            [app.server.rama.material-truth :as material-truth]
            [app.server.rama.object-container.facet-master :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.rama.relation-kernel :as rk]
            [app.shared.activation-event :as activation-event]
            [app.shared.attention-material :as attention]
            [app.shared.binding-material :as bm]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.foldable-material :as foldable]
            [app.shared.matter-room :as matter-room]
            [app.shared.material-portal :as portal]
            [app.shared.provenance-material :as provenance]
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

(defn- open-with-context!
  [ctx params]
  (mp/open ctx (fn [req] (fp/serve ctx req)) params))

(def entity-mode-regression-sha
  "Captured from HEAD 7756b760 before matter-room P1 opened source."
  "deb12d4d70383c0d55321225eb555797fe7203a4c18fef5c51b4f6d1e81db858")

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
;; matter-room P1 · G1/G2 — the type address, with honest anchor bases
;; ===========================================================================

(deftest matter-room-p1-entity-mode-bytes-stay-fixed
  (let [ctx {:oc-rt nil :rk-rt nil}
        serve-fn (constantly {})
        params {:entity-id "du:block:entity-mode-regression" :wearers []}
        result (mp/open ctx serve-fn params)
        result-with-ignored-master
        (mp/open ctx serve-fn (assoc params :master-id attention/master-id))
        bytes (portal/canonical-edn result)]
    (is (= entity-mode-regression-sha (core/sha-256 bytes))
        "adding :master-id mode must not drift one byte of entity mode")
    (is (= 22064 (count (.getBytes bytes "UTF-8"))))
    (is (= bytes (portal/canonical-edn result-with-ignored-master))
        "entity-id wins if both addresses are supplied")))

(deftest matter-room-p1-master-anchor-is-total-honest-and-deterministic
  (let [oc-rt (ocr/start-object-container-runtime!)
        rk-rt (rk/start-relation-runtime! {:tasks 4 :threads 2})
        ctx {:oc-rt oc-rt :rk-rt rk-rt}]
    (try
      (let [revs (boot! oc-rt)
            attention-only
            (wearer "du:block:attention-only"
                    [[attention/master-id (:attention revs)
                      :block/user-hit-area :attention/hit-padding]])
            foldable-only
            (wearer "du:block:foldable-only"
                    [[foldable/master-id (:foldable revs)
                      :block/fold-toggle :foldable/folded?]])
            master-id attention/master-id
            derived-room-id (matter-room/room-id master-id)
            params {:master-id master-id}
            registered (open-with-context! ctx params)
            repeated (open-with-context! ctx params)
            unknown (open-with-context! ctx {:master-id "fm:not-registered"})
            mixed-snapshot
            (open-with-context!
             ctx
             {:master-id master-id
              :wearers [attention-only foldable-only]})
            irrelevant-snapshot
            (open-with-context!
             ctx
             {:master-id master-id
              :wearers [foldable-only]})
            rendered (mp/render registered)
            masters-card (some #(when (= :masters (:card/id %)) %)
                               (:render/cards rendered))
            blast-card (some #(when (= :blast (:card/id %)) %)
                             (:render/cards rendered))
            canonical (portal/canonical-edn registered)]

        (testing "G1 has teeth at a registered master"
          (is (= [] (:portal/errors registered)))
          (is (true? (get-in registered
                             [:portal/identity :entity/found?])))
          (is (= master-id
                 (get-in registered [:portal/identity :entity/id])))
          (is (= :facet-master
                 (get-in registered [:portal/identity :entity/kind])))
          (is (= :attention
                 (get-in registered [:portal/identity :entity/facet])))
          (is (= (facet-material/floor-master-id attention/spec)
                 (get-in registered
                         [:portal/identity :entity/floor-master-id])))
          (is (= [master-id] (vec (keys (:portal/masters registered)))))
          (is (= {:placement/applicable? false
                  :placement/anchor :facet-master}
                 (:portal/placement registered)))
          (is (= {derived-room-id master-id} (:portal/room registered)))
          (is (= master-id
                 (matter-room/master-id-for-room derived-room-id)))
          (is (re-matches
               #"[0-9a-f]{8}-[0-9a-f]{4}-3[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
               derived-room-id))
          (is (= (count facet-masters/master-ids)
                 (count matter-room/master-id-by-room)))
          (is (= 17 (count (:portal/questions registered))))
          (is (= [] (portal/unanswered registered))))

        (testing "anchor wearer and blast answers use only this master"
          (is (= ["du:block:attention-only"]
                 (get-in mixed-snapshot
                         [:portal/wearers :wearers/entities])))
          (is (= ["du:block:attention-only"]
                 (get-in mixed-snapshot
                         [:portal/blast :blast/candidate-wearers])))
          (is (= ["du:block:attention-only"]
                 (get-in mixed-snapshot
                         [:portal/blast :blast/by-master master-id
                          :blast/will-move])))
          (is (= :current-client-scene
                 (get-in irrelevant-snapshot
                         [:portal/wearers :wearers/basis])))
          (is (= []
                 (get-in irrelevant-snapshot
                         [:portal/wearers :wearers/entities])))
          (is (= []
                 (get-in irrelevant-snapshot
                         [:portal/blast :blast/by-master master-id
                          :blast/will-move])))
          (is (= 0
                 (get-in irrelevant-snapshot
                         [:portal/blast :blast/by-master master-id
                          :blast/counted-over]))))

        (testing "anchor-priced recipe and bindings name their basis"
          (is (= master-id
                 (get-in registered
                         [:portal/recipe :recipe/anchor-master-id])))
          (is (= :rendered-contribution-stamps
                 (get-in registered [:portal/recipe :recipe/basis])))
          (is (every? #(= master-id (:table/master-id %))
                      (get-in registered [:portal/bindings :bindings/rows])))
          (is (every? #(= "await __portal.openMaster('fm:attention')"
                          (:question/call-console %))
                      (:portal/questions registered)))
          (is (str/includes?
               (slurp "src/app/client/workspace/face_wiring.cljs")
               ":openMaster (fn [master-id]")))

        (testing "an unknown master still answers every question"
          (is (= [] (:portal/errors unknown)))
          (is (false? (get-in unknown
                              [:portal/identity :entity/found?])))
          (is (= "fm:not-registered"
                 (get-in unknown [:portal/identity :entity/id])))
          (is (= ["fm:not-registered"]
                 (vec (keys (:portal/masters unknown)))))
          (is (= [] (portal/unanswered unknown)))
          (is (every? :question/answered? (:portal/questions unknown))))

        (testing "G2: no wearer evidence is never rendered as a confident zero"
          (is (= :no-wearer-snapshot-at-anchor
                 (get-in registered [:portal/blast :blast/basis])))
          (is (nil? (get-in registered
                            [:portal/blast :blast/counted-over])))
          (is (= :no-wearer-snapshot-at-anchor
                 (get-in registered [:portal/wearers :wearers/basis])))
          (is (seq (get-in registered [:portal/blast :blast/by-master])))
          (doseq [[_ per-master]
                  (get-in registered [:portal/blast :blast/by-master])]
            (is (= :no-wearer-snapshot-at-anchor
                   (:blast/basis per-master)))
            (is (nil? (:blast/counted-over per-master))))
          (is (some #(and (= "basis" (:row/label %))
                          (= ":no-wearer-snapshot-at-anchor"
                             (:row/value %)))
                    (:card/rows blast-card))
              "basis honesty must reach the rendered blast card"))

        (testing "every entity-relative here-value carries the sentinel"
          (doseq [k portal/master-here-keys]
            (is (= portal/not-applicable-at-anchor
                   (get-in registered [:portal/masters master-id k]))
                (str k)))
          (doseq [k portal/deviation-here-keys]
            (is (= portal/not-applicable-at-anchor
                   (get-in registered [:portal/deviations k]))
                (str k)))
          (doseq [k portal/wearer-here-keys]
            (is (= portal/not-applicable-at-anchor
                   (get-in registered [:portal/wearers k]))
                (str k)))
          (is (not (str/includes? (pr-str masters-card) "PINNED"))
              "the truthy sentinel must not mint a rendered PINNED claim"))

        (testing "experience reads once at the explicit room conversation"
          (is (= [master-id]
                 (get-in registered
                         [:portal/experience :experience/material-ids])))
          (is (= (episode/episode-object-key derived-room-id)
                 (get-in registered
                         [:portal/experience
                          :experience/conversation-address])))
          (is (= 1
                 (get-in registered
                         [:portal/experience :experience/query-plan
                          :relation-roundtrips])))
          (is (true?
               (get-in registered
                       [:portal/experience :experience/query-plan :batched?]))))

        (testing "the shipped in-JVM form is deterministic"
          (is (= canonical (portal/canonical-edn repeated)))
          (is (= canonical
                 (binding [*print-namespace-maps* true]
                   (portal/canonical-edn
                    (open-with-context! ctx params)))))
          (println "MATTER_ROOM_P1_CANONICAL_SHA"
                   (core/sha-256 canonical))
          (println "MATTER_ROOM_P1_ROOM_ID" derived-room-id)))
      (finally
        (rk/close-relation-runtime! rk-rt)
        (ocr/close-object-container-runtime! oc-rt)))))

;; ===========================================================================
;; matter-room P2 · G3/G4/G5 — the room: birth once, refresh, carried verbs
;; ===========================================================================

(defn- room-projection-rows
  "PHYSICAL PState read of one room's native rows — never the product query
   surface. A negative invariant (`zero duplicate residents`) needs a reader
   that can actually SEE a duplicate; the served projection dedups by
   construction and would mask exactly the bug this asserts against."
  [oc-rt object-key]
  (vec (foreign-select [(keypath (tid/chat-conversation-id object-key)) MAP-VALS]
                       (:transcript-conversation-projection oc-rt))))

(defn- unit-physical
  [oc-rt unit-id]
  (foreign-select-one [(keypath unit-id)] (:derived-units-by-id oc-rt)))

(def ^:private drill-floor-rows
  "The kernel's floor tier, rebuilt from the same specs (the binding-dispatch
   suite's fixture shape)."
  (assoc (into {}
               (map (fn [spec]
                      [(:facet-master/facet spec)
                       (:facet-master/bindings (facet-material/code-floor spec))]))
               facet-masters/specs)
         bm/space-facet bm/space-floor-bindings))

(defn- drill-at
  [subject instance-rows]
  (bm/drill-report {:facet-rows {}
                    :floor-rows drill-floor-rows
                    :instance-rows (or instance-rows {})
                    :subject subject}))

(deftest matter-room-p2-room-is-real-idempotent-and-machine-classified
  (let [oc-rt (ocr/start-object-container-runtime!)
        rk-rt (rk/start-relation-runtime! {:tasks 4 :threads 2})
        ctx {:oc-rt oc-rt :rk-rt rk-rt}]
    (try
      (let [_ (boot! oc-rt)
            master-id attention/master-id
            room-id (matter-room/room-id master-id)
            object-key (episode/episode-object-key room-id)
            open1 (sj/open-matter-room! ctx {:master-id master-id})
            open2 (sj/open-matter-room! ctx {:master-id master-id})
            unit-ids-1 (mapv :unit-id (:residents open1))
            unit-ids-2 (mapv :unit-id (:residents open2))
            rows (room-projection-rows oc-rt object-key)
            native (filterv #(= :episode-utterance (:entry-kind %)) rows)]

        (testing "G3 · the room is addressed deterministically and opens once"
          (is (= :ok (:status open1)))
          (is (= room-id (:room-id open1)))
          (is (= object-key (:object-key open1)))
          (is (= (str "?drill=" room-id) (:entry open1)))
          (is (= [] (:portal-errors open1)))
          (is (seq (:residents open1)))
          (is (every? #(= :accepted (:status %)) (:residents open1)))
          (is (every? #(= :birth (:act %)) (:residents open1))))

        (testing "G3 · a second open converges — no second row set (T2)"
          (is (= unit-ids-1 unit-ids-2) "resident ids are identity-only")
          (is (every? #(= :unchanged (:act %)) (:residents open2)))
          (is (= (count unit-ids-1) (count native))
              "one native row per resident, physically read")
          (is (= (count (distinct (map :import-key native)))
                 (count native))
              "no duplicate import minted a second row")
          (is (every? #(some? (unit-physical oc-rt %)) unit-ids-1)))

        (testing "G5 · birth carries the MACHINE actor all the way to render"
          (is (every? #(= matter-room/resident-actor-id (:role %)) native)
              "the role slot is what machine classification reads")
          (let [dc (fp/serve ctx {:face :conversation
                                  :address object-key
                                  :params {}})
                speakers (mapv :speaker (:turns dc))
                kinds (vec (mapcat #(map :kind (:blocks %)) (:turns dc)))]
            (is (= (count unit-ids-1) (count (:turns dc))))
            (is (= [matter-room/resident-actor-id] (distinct speakers))
                "a served room turn is NEVER spoken by sid")
            (is (not (contains? (set speakers) "sid")))
            (is (= [:material-part] (distinct kinds))
                "the whole-block :material cut, one unit per resident")))

        (testing "G5 · the room rides the ONE import path, with no new family"
          (is (every? #(str/starts-with? (str (:import-key %)) "imp:ep:")
                      (:residents open1))
              "the existing episode import prefix — no new import family")
          (let [seam (slurp "src/app/shared/matter_room.cljc")]
            ;; the seam composes CONTENT; it mints no request, no import key
            ;; and no envelope — that is what keeps it from becoming a second
            ;; import artery while passing a narrower grep (G5, R1 finding 1).
            (doseq [composer ["action-request" "utterance-import-request"
                              ":request-type" "imp:" ":payload"]]
              (is (not (str/includes? seam composer))
                  (str "the room seam must not compose imports: " composer))))
          ;; the package-wide form of G5: no NEW import-request builder joined
          ;; the tree, inside the room seam or anywhere else.
          (let [owners (->> (file-seq (io/file "src"))
                            (filter #(.isFile ^java.io.File %))
                            (filter #(re-find #"\.clj[cs]?$" (.getName ^java.io.File %)))
                            (remove #(str/ends-with? (str %) "env.clj"))
                            (keep (fn [f]
                                    (when (str/includes?
                                           (slurp f)
                                           ":request-type :object-container/import-material")
                                      (str/replace (str f) "\\" "/"))))
                            set)]
            (is (= #{"src/app/server/episode.clj"
                     "src/app/server/rama/material_circulation.clj"
                     "src/app/server/rama/object_container/assembly_adapter.clj"
                     "src/app/server/rama/object_container/block_distiller.clj"
                     "src/app/server/rama/object_container/clojure_adapter.clj"
                     "src/app/server/rama/object_container/facet_master.clj"
                     "src/app/server/rama/object_container/markdown_adapter.clj"
                     "src/app/server/rama/object_container/transcript_adapter.clj"}
                   owners)
                "matter-room adds NO import-request builder anywhere (G5)")))

        (testing "G2/L6 · experience widens to the room's residents, batched"
          (let [anchored (:portal/result
                          (fp/serve ctx {:face :material-portal
                                         :params {:master-id master-id}}))
                exp (:portal/experience anchored)]
            (is (= (into [master-id] (sort unit-ids-1))
                   (:experience/material-ids exp)))
            (is (= (count unit-ids-1) (:experience/room-resident-count exp)))
            (is (= object-key (:experience/conversation-address exp)))
            (is (= 1 (get-in exp [:experience/query-plan :relation-roundtrips]))
                "T3 — one batched relation read, never a scan")
            (is (true? (get-in exp [:experience/query-plan :batched?])))))

        (testing "L6 · a mark on a resident surfaces in the master's experience"
          (let [resident (first unit-ids-1)
                mark "du:block:room-mark"
                request (rk/assert-request
                         {:kind :felt-at
                          :from (rk/->target-ref :derived-unit mark)
                          :to (rk/->target-ref :derived-unit resident)
                          :asserter-actor-id "sid"
                          :asserter-type :human
                          :actor {:actor/id "sid" :actor/type :human}
                          :asserted-at-ms (now)
                          :sent-at-ms (now)
                          :request-id "matter-room-p2-mark"
                          :idempotency-key "matter-room-p2-mark"})
                _ (rk/append-relation-request! rk-rt request)
                anchored (:portal/result
                          (fp/serve ctx {:face :material-portal
                                         :params {:master-id master-id}}))
                exp (:portal/experience anchored)]
            (is (pos? (:experience/count exp)))
            (is (contains? (set (map :experience/origin-unit-id
                                     (:experience/items exp)))
                           resident)
                "the room resident's record is the type's record")))

        (testing "the rendered floor gains a room row, inside an existing card"
          (let [served (fp/serve ctx {:face :material-portal
                                      :params {:master-id master-id}})
                rendered (:portal/render served)
                cards (:render/cards rendered)
                identity-card (some #(when (= :identity (:card/id %)) %) cards)]
            (is (= (count portal/questions) (count cards))
                "the card SET is still the compile-time question list")
            (is (some #(and (= "room" (:row/label %))
                            (str/includes? (str (:row/value %)) room-id))
                      (:card/rows identity-card))))))
      (finally
        (rk/close-relation-runtime! rk-rt)
        (ocr/close-object-container-runtime! oc-rt)))))

(deftest matter-room-p2-refresh-rides-the-edit-lane-monotonically
  (let [oc-rt (ocr/start-object-container-runtime!)
        ctx {:oc-rt oc-rt :rk-rt nil}]
    (try
      (let [_ (boot! oc-rt)
            master-id attention/master-id
            open1 (sj/open-matter-room! ctx {:master-id master-id})
            object-key (:object-key open1)
            head-unit (:unit-id (first (filter #(= :head (:section %))
                                               (:residents open1))))
            trail-1 (filterv #(= :trail (:section %)) (:residents open1))
            candidate (adapter/import-candidate!
                       oc-rt attention/spec
                       (pr-str (assoc (:facet-master/default-form attention/spec)
                                      :attention/hit-padding 11.0))
                       {:request/id "matter-room-p2-candidate"})
            activation (adapter/activate!
                        oc-rt attention/spec (:revision-id candidate)
                        {:kind :activate :actor sid :time-ms (now)
                         :request/id "matter-room-p2-activate"})
            open2 (sj/open-matter-room! ctx {:master-id master-id})
            trail-2 (filterv #(= :trail (:section %)) (:residents open2))
            heads-2 (filterv #(= :head (:section %)) (:residents open2))
            native (filterv #(= :episode-utterance (:entry-kind %))
                            (room-projection-rows oc-rt object-key))]

        (testing "a new activation refreshes the head and APPENDS one resident"
          (is (= :accepted (get-in activation [:decision :status])))
          (is (= 1 (count heads-2)) "exactly one head resident, ever")
          (is (= :refresh (:act (first heads-2)))
              "changed content rides the edit lane — never a re-import")
          (is (= head-unit (:unit-id (first heads-2)))
              "the head keeps its unit id across the refresh")
          (is (= (inc (count trail-1)) (count trail-2))
              "exactly ONE new trail resident per activation")
          (is (= (mapv :unit-id trail-1)
                 (vec (take (count trail-1) (mapv :unit-id trail-2))))
              "append-only: the standing trail residents keep their ids")
          (is (every? #(= :unchanged (:act %))
                      (take (count trail-1) trail-2))
              "an existing trail resident is NEVER edited")
          (is (= (count (:residents open2)) (count native))
              "the refresh minted no extra row"))

        (testing "the edit seq is durable-read monotone, never a content hash"
          (let [a "ZZZZ later-in-hash-order"
                b "AAAA earlier-in-hash-order"
                _ (is (not= (compare (core/sha-256 a) (core/sha-256 b))
                            (compare 1 2))
                      "the fixtures invert content-hash order on purpose")
                seq-before (sj/matter-room-next-edit-seq oc-rt head-unit)
                e1 (sj/matter-room-refresh!
                    oc-rt object-key head-unit
                    (ocr/read-unit oc-rt head-unit) {:resident/text a})
                e2 (sj/matter-room-refresh!
                    oc-rt object-key head-unit
                    (ocr/read-unit oc-rt head-unit) {:resident/text b})]
            (is (= :accepted (:status e1)))
            (is (= :accepted (:status e2))
                "both refreshes land — a hash-derived seq would drop this one")
            (is (= seq-before (:edit-seq e1)))
            (is (= (inc (:edit-seq e1)) (:edit-seq e2)))
            (is (= b (:content-text (ocr/read-unit oc-rt head-unit)))
                "last write wins, in write order")
            (is (thrown-with-msg?
                 clojure.lang.ExceptionInfo
                 #"seq-read unavailable"
                 (with-redefs
                   [ocr/foreign-one
                    (fn [& _]
                      (throw
                       (ex-info "seq-read unavailable" {:type :test/outage})))]
                   (sj/matter-room-next-edit-seq oc-rt head-unit)))
                "a durable seq-read outage fails closed; it must never reset to 1")))

        (testing "a same-content re-open writes nothing at all"
          (let [next-seq (sj/matter-room-next-edit-seq oc-rt head-unit)
                open3 (sj/open-matter-room! ctx {:master-id master-id})
                head3 (first (filter #(= :head (:section %)) (:residents open3)))]
            ;; the head text was clobbered by the seq fixtures above, so this
            ;; open refreshes ONCE more and then converges
            (is (= :refresh (:act head3)))
            (is (= next-seq (:edit-seq head3))
                "the driver takes the seq the durable read hands it")
            (let [open4 (sj/open-matter-room! ctx {:master-id master-id})]
              (is (every? #(= :unchanged (:act %)) (:residents open4))
                  "converged: no revision noise from an unchanged room")))))
      (finally
        (ocr/close-object-container-runtime! oc-rt)))))

(deftest matter-room-p2-carried-verbs-are-site-matched
  (let [resident "du:chat:room:episode-native-v0:ep:abcd1234:000000"
        ground-block "du:block-1"
        by-label (fn [report] (into {} (map (juxt :probe/label identity)) report))
        resident-report (by-label (drill-at resident nil))
        ground-report (by-label (drill-at ground-block nil))
        outcome (fn [report label]
                  ((juxt :probe/verb :probe/tier :probe/facet :probe/outcome)
                   (get report label)))]

    (testing "G4 · a room resident resolves EXACTLY like a ground block"
      (is (= (drill-at ground-block nil) (drill-at resident nil))
          "the floor decides on site + gesture, never on who the subject is"))

    (testing "G4 · the MACHINE hit-area verbs a machine resident carries"
      (doseq [label ["tap a machine block → release focus"
                     "drag a machine block"
                     "tap a fold header → toggle its section"]]
        (is (= (outcome ground-report label) (outcome resident-report label))
            label)
        (is (= :claimed (:probe/outcome (get resident-report label))))))

    (testing "G4 · Sid's OWN room block carries the USER set, unchanged"
      (doseq [label ["tap a user block → focus"
                     "drag a user block"
                     "shift-drag a user block → text selection"]]
        (is (= (outcome ground-report label) (outcome resident-report label))
            label)
        (is (= :claimed (:probe/outcome (get resident-report label))))))

    (testing "G4 · an instance row on a resident behaves like one on a block"
      (let [row {:binding/gesture :pointer/tap
                 :binding/phase :complete
                 :binding/modifiers :any
                 :binding/verb {:verb/name :focus/release :verb/version 0}
                 :binding/priority 9999}
            at-resident (by-label
                         (drill-at resident
                                   {[resident :block/user-hit-area] [row]}))
            at-block (by-label
                      (drill-at ground-block
                                {[ground-block :block/user-hit-area] [row]}))
            label "tap a user block → focus"]
        (is (= :instance (:probe/tier (get at-resident label))))
        (is (= (outcome at-block label) (outcome at-resident label)))))

    (testing "G4 · camera reservation is untouched inside the room"
      (is (true? (bm/camera-gesture-reserved?
                  :space/ground
                  {:binding/gesture :wheel
                   :binding/phase :complete
                   :binding/modifiers :any})))
      (doseq [label ["drag empty space → pan the camera"
                     "wheel → zoom at the pointer"
                     "wheel at a block → zoom through the space rung"]]
        (is (= :space (:probe/facet (get resident-report label))) label)
        (is (= :claimed (:probe/outcome (get resident-report label))) label)))))

(deftest matter-room-p2-resident-composition-is-pure-and-total
  (testing "composition is total over garbage and over an entity-mode result"
    (is (= [] (matter-room/residents nil)))
    (is (= [] (matter-room/residents {:portal/identity "not a map"})))
    (is (= [] (matter-room/residents {:portal/master-id 42}))))

  (testing "ids are identity-only — content never enters a birth id"
    (let [base {:portal/master-id "fm:attention"
                :portal/identity {:entity/facet :attention}
                :portal/masters {"fm:attention" {:master/active-revision-id "r1"}}
                :portal/bindings {:bindings/rows [] :bindings/verbs []}
                :portal/room {"room-uuid" "fm:attention"}
                :portal/history {:history/available-cuts
                                 {"fm:attention" ["p2" "p1"]}}}
          moved (assoc-in base [:portal/masters "fm:attention"
                                :master/active-revision-id] "r2")
          ids (mapv :resident/turn-id (matter-room/residents base))]
      (is (= ["mr:fm:attention:head"
              "mr:fm:attention:bindings"
              "mr:fm:attention:trail:p1"
              "mr:fm:attention:trail:p2"]
             ids)
          "head · bindings · trail OLDEST first (a new activation appends)")
      (is (= ids (mapv :resident/turn-id (matter-room/residents moved)))
          "changed content must NOT move an id (T2 / the fingerprint law)")
      (is (not= (mapv :resident/text (matter-room/residents base))
                (mapv :resident/text (matter-room/residents moved)))
          "…while the content itself does change, which is why refresh exists")
      (is (= [0 1 2 3] (mapv :resident/time-ms (matter-room/residents base)))
          "the order key is the composition ordinal, not a clock (T10)")
      (is (= [true true false false]
             (mapv :resident/refreshable? (matter-room/residents base)))
          "trail residents are append-only")))

  (testing "a trail resident carries no fact about the PRESENT chain"
    (let [with-pointer {:portal/master-id "fm:attention"
                        :portal/masters {"fm:attention"
                                         {:master/pointer-revision-id "p2"}}
                        :portal/history {:history/available-cuts
                                         {"fm:attention" ["p2" "p1"]}}}
          moved (assoc-in with-pointer [:portal/masters "fm:attention"
                                        :master/pointer-revision-id] "p3")
          trail-of #(->> (matter-room/residents %)
                         (filter (comp #{:trail} :resident/section))
                         (mapv :resident/text))]
      (is (= (trail-of with-pointer) (trail-of moved))
          "a moved pointer must not rewrite a standing trail resident"))))

(deftest matter-room-p2-episode-parameterization-preserves-sids-lane
  (let [args {:object-key "chat:room" :turn-id "t1" :text "hello"
              :time-ms 1234567 :prev-turn-id nil}
        sid-request (episode/utterance-import-request args)
        machine-request (episode/utterance-import-request
                         (assoc args
                                :actor matter-room/resident-actor
                                :actor-id matter-room/resident-actor-id
                                :actor-role matter-room/resident-actor-role
                                :part-type matter-room/resident-part-type))
        hint-of #(first (get-in % [:payload :projection-hints]))]

    (testing "the default path is Sid's lane, unchanged"
      (is (= (episode/utterance-actor) (episode/utterance-actor nil)))
      (is (= {:actor/id "sid" :actor/type :human
              :actor/capabilities #{:object-container/import-material}}
             (episode/utterance-actor)))
      (is (= "sid" (:role (hint-of sid-request))))
      (is (= "sid" (get-in sid-request [:actor :actor/id])))
      (is (= :human-message
             (:unit-kind (first (get-in sid-request [:payload :derived-units])))))
      (is (= "user" (get-in sid-request [:payload :source-artifacts 0
                                         :production-event
                                         :production/actor-role]))))

    (testing "the machine path differs ONLY where machine-ness lives"
      (is (= matter-room/resident-actor-id (:role (hint-of machine-request)))
          "the role slot is the one field render classification reads")
      (is (= matter-room/resident-actor
             (:actor machine-request)))
      (is (= :agent (get-in machine-request [:actor :actor/type]))
          "core/actor-types has no :machine — :agent is the honest legal value")
      (is (contains? (get-in machine-request [:actor :actor/capabilities])
                     :object-container/import-material)
          "not :system, so authorized-request? genuinely checks this")
      (is (= :material-part
             (:unit-kind (first (get-in machine-request
                                        [:payload :derived-units]))))
          "the whole-block cut, one unit per resident")
      (is (= 1 (count (get-in machine-request [:payload :derived-units]))))
      (is (= (:import/key sid-request) (:import/key machine-request))
          "identity is the turn-id: the actor never enters the import key")
      (is (not= (:material/fingerprint sid-request)
                (:material/fingerprint machine-request))
          "…but the payload differs, so the fingerprint must too"))

    (testing "birth identity is replay-stable"
      (is (= (episode/utterance-import-request args)
             sid-request))
      (is (= (:idempotency/key sid-request) (:import/key sid-request))))))

;; ===========================================================================
;; matter-room P3 · G6/G7 — master-anchored mouth, existing-P6 hands
;; ===========================================================================

(deftest matter-room-p3-briefing-is-master-anchored-and-server-authoritative
  (let [rt (ocr/start-object-container-runtime!)
        ctx {:oc-rt rt :rk-rt nil}]
    (try
      (boot! rt)
      (let [master-id attention/master-id
            room-id (matter-room/room-id master-id)
            forged {:portal-open
                    {:master-id foldable/master-id
                     :master-ids [foldable/master-id text-body/master-id]
                     :entity-id alpha
                     :wearers [(wearer alpha [])]}}
            authoritative
            (matter-room/narrowed-portal-open
             (merge (:portal-open forged) {:conversation-id room-id}))
            resident-open (sj/resident-portal-open forged alpha room-id)
            human (fp/portal-briefing ctx authoritative)
            resident (fp/portal-briefing ctx resident-open)]

        (testing "the room id derives exactly one master-side anchor"
          (is (= {:master-id master-id
                  :conversation-id room-id
                  :narrowed? true}
                 authoritative))
          (is (= authoritative resident-open)
              "client-supplied master/entity/wearer coordinates cannot widen"))

        (testing "the human and Ctrl+Enter resident receive identical bytes"
          (is (= human resident))
          (is (str/includes? resident "<projection>"))
          (is (str/includes? resident (str ":entity/id " (pr-str master-id)))
              "the equality must contain the requested master's identity"))

        (testing "non-room conversations retain P8 block narrowing"
          (let [fallback (sj/resident-portal-open
                          forged alpha "conversation:not-a-matter-room")]
            (is (= alpha (:entity-id fallback)))
            (is (= "conversation:not-a-matter-room"
                   (:conversation-id fallback)))
            (is (= [] (:master-ids fallback))
                "P8 derives the set from surviving wearer stamps; it never
                 accepts the forged client master set"))))
      (finally
        (ocr/close-object-container-runtime! rt)))))

(deftest matter-room-p3-durable-acts-end-in-existing-p6-truth
  (let [rt (ocr/start-object-container-runtime!)
        ctx {:oc-rt rt :rk-rt nil}]
    (try
      (let [revs (boot! rt)
            master-id attention/master-id
            original-active (:attention revs)
            valid-source
            (pr-str (assoc (:facet-master/default-form attention/spec)
                           :attention/hit-padding 13.0))
            candidate
            (sj/matter-room-deviate!
             ctx {:master-id master-id
                  :source valid-source
                  :request-id "matter-room-p3-candidate"
                  :time-ms (now)
                  :actor sid})
            candidate-revision (:revision-id candidate)]

        (testing "master deviation is the existing retained-candidate import"
          (is (:accepted? candidate) (pr-str candidate))
          (is (= :master-candidate (:branch candidate)))
          (is (= candidate-revision
                 (get-in (adapter/read-master rt attention/spec)
                         [:latest-revision :revision-id])))
          (is (= original-active
                 (get-in (adapter/read-master rt attention/spec)
                         [:active-revision :revision-id]))
              "candidate import must not smuggle a pointer edit"))

        (testing "activate is the existing grammar-checked pointer act"
          (let [activated
                (sj/matter-room-activate!
                 ctx {:master-id master-id
                      :revision-id candidate-revision
                      :request-id "matter-room-p3-activate"
                      :time-ms (now)
                      :actor sid})]
            (is (:accepted? activated) (pr-str activated))
            (is (= :accepted (:decision-status activated)))
            (is (= :activate
                   (get-in activated [:event :activation/kind])))
            (is (= candidate-revision
                   (get-in (adapter/read-master rt attention/spec)
                           [:active-revision :revision-id])))))

        (testing "instance deviation is material-truth/deviate!, not a new lane"
          (let [deviation
                (sj/matter-room-deviate!
                 ctx {:master-id master-id
                      :subject-uid alpha
                      :overrides {:attention/hit-padding 24.0}
                      :request-id "matter-room-p3-instance"
                      :time-ms (now)
                      :actor sid})
                instance (adapter/instance-state rt attention/spec alpha)]
            (is (:accepted? deviation) (pr-str deviation))
            (is (= :instance (:branch deviation)))
            (is (true? (:deviates? instance)))
            (is (= 24.0 (get-in instance
                                [:material :attention/hit-padding])))))

        (testing "rollback executes only the currently served recovery offer"
          (let [rolled-back
                (sj/matter-room-rollback!
                 ctx {:master-id master-id
                      :revision-id original-active
                      :request-id "matter-room-p3-rollback"
                      :time-ms (now)
                      :actor sid})]
            (is (:accepted? rolled-back) (pr-str rolled-back))
            (is (= :recovery-offer (:branch rolled-back)))
            (is (= :rollback
                   (get-in rolled-back [:event :activation/kind])))
            (is (= original-active
                   (get-in (adapter/read-master rt attention/spec)
                           [:active-revision :revision-id]))))
          (let [before (adapter/read-master rt attention/spec)
                forged
                (sj/matter-room-rollback!
                 ctx {:master-id master-id
                      :revision-id "rev:forged"
                      :request-id "matter-room-p3-forged-rollback"
                      :time-ms (now)
                      :actor sid})]
            (is (false? (:accepted? forged)))
            (is (= :matter/recovery-offer-not-found (:error forged)))
            (is (= :error (get-in forged [:card :card/status])))
            (is (= before (adapter/read-master rt attention/spec))
                "a forged non-offer target cannot move durable truth")))

        (testing "a malformed candidate cards and leaves the worn revision"
          (let [malformed
                (sj/matter-room-deviate!
                 ctx {:master-id master-id
                      :source (str "{:facet-master/id " (pr-str master-id)
                                   " :drill/id \"matter-room-p3\"")
                      :request-id "matter-room-p3-malformed-import"
                      :time-ms (now)
                      :actor sid})
                before-active
                (get-in (adapter/read-master rt attention/spec)
                        [:active-revision :revision-id])
                refused
                (sj/matter-room-activate!
                 ctx {:master-id master-id
                      :revision-id (:revision-id malformed)
                      :request-id "matter-room-p3-malformed-activate"
                      :time-ms (now)
                      :actor sid})]
            (is (:accepted? malformed) (pr-str malformed))
            (is (= :rejected (:status refused)))
            (is (= :facet-master/activation-rejected (:error refused)))
            (is (= :error (get-in refused [:card :card/status])))
            (is (seq (get-in refused [:card :card/errors])))
            (is (= before-active
                   (get-in (adapter/read-master rt attention/spec)
                           [:active-revision :revision-id])))))

        (testing "the active floor remains matter-verb-free"
          (let [served (open-with-context! ctx {:master-id master-id})
                matter-verbs #{:matter/deviate :matter/preview
                               :matter/activate :matter/rollback}]
            (is (empty?
                 (filter #(contains? matter-verbs (:table/verb %))
                         (get-in served [:portal/bindings :bindings/rows])))))))
      (finally
        (ocr/close-object-container-runtime! rt)))))

(deftest matter-room-p3-invocation-and-read-only-disclosures-are-exact
  (let [server-src (slurp (io/resource "app/server_jetty.clj"))
        client-src (slurp (io/resource "app/client/workspace/face_wiring.cljs"))
        briefing-src (slurp (io/resource "app/shared/material_portal.cljc"))
        endpoint-names
        (set (map second
                  (re-seq
                   #"\"/api/matter-room/(deviate|activate|rollback|preview)\""
                   server-src)))]
    (testing "all and only the three durable endpoints are disclosed"
      (is (= #{"deviate" "activate" "rollback"} endpoint-names))
      (is (not (str/includes? server-src "\"/api/matter-room/preview\""))))

    (testing "preview stays on the existing client projection lane"
      (is (str/includes? client-src "(.-__bindings"))
      (is (str/includes? client-src "(.preview bindings"))
      (is (str/includes? client-src "(.endPreview bindings")))

    (testing "T9 names all four verbs and both honest effect classes"
      (doseq [token [":matter/deviate" ":matter/preview"
                     ":matter/activate" ":matter/rollback"
                     ":durable-via-request" ":pure-projection"]]
        (is (str/includes? briefing-src token) token))
      (is (not (str/includes?
                briefing-src
                "The portal is read-only: it does not execute writes"))))))

;; ===========================================================================
;; matter-room P4 · G8/G9 — citizens and the on-demand terminal-escape gauge
;; ===========================================================================

(deftest matter-room-p4-cascade-citizen-is-labeled-read-only-and-dark-in-fixture
  (let [dark-called? (atom false)
        dark-handler (fn [& _] (reset! dark-called? true))
        dark-row {:cascade/id :cascade/test-dark
                  :cascade/trigger :test/never-emitted
                  :cascade/handler dark-handler
                  :cascade/effect-class :pure-projection}
        injected
        (fp/cascade-rows-projection
         (constantly [dark-row])
         {}
         {:face :cascade-rows})
        production
        (fp/cascade-rows-projection {} {:face :cascade-rows})]
    (testing "G8 · the row source is injectable and serving never invokes it"
      (is (= [dark-row] (:cascade/rows injected)))
      (is (false? @dark-called?)
          "the fixture-only never-emitted row stays inert during projection")
      (is (= :fixture-injected (:cascade/source injected)))
      (is (= [:fixture-injected :in-process]
             (:cascade/labels injected)))
      (is (= :fixture-injected (:cascade/ownership injected)))
      (is (not= :code-owned (:cascade/ownership injected))
          "the injection seam cannot launder an arbitrary source as code-owned"))
    (testing "the real citizen states its current non-durable ownership"
      (is (= (cascade/rows) (:cascade/rows production)))
      (is (= 'app.server.cascade/rows (:cascade/source production)))
      (is (= [:code-owned :in-process] (:cascade/labels production)))
      (is (= :code-owned (:cascade/ownership production)))
      (is (= :in-process (:cascade/lifetime production)))
      (is (true? (:cascade/read-only? production)))
      (is (str/includes? (:cascade/source-swap-note production)
                         "durable cascade table")))))

(deftest matter-room-p4-portal-links-citizens-but-standard-open-runs-no-git
  (let [rt (ocr/start-object-container-runtime!)
        git-calls (atom 0)]
    (try
      (boot! rt)
      (with-redefs [git-spine/read-commits
                    (fn [_]
                      (swap! git-calls inc)
                      (throw (ex-info "git must not run in portal open" {})))]
        (let [result (open! rt {:master-id attention/master-id})
              cascade-section (:portal/cascade result)
              truncation-sections
              (set (map :truncation/section
                        (get-in result
                                [:portal/truncation
                                 :truncation/sections])))]
          (is (zero? @git-calls)
              "the standard portal open invokes no git-backed face")
          (is (= (cascade/rows) (:cascade/rows cascade-section)))
          (is (= [:code-owned :in-process]
                 (:cascade/labels cascade-section)))
          (is (= {:face :escape-gauge
                  :params {:master-id attention/master-id}
                  :on-demand? true
                  :embedded? false}
                 (:cascade/escape-gauge cascade-section)))
          (is (not (contains? result :portal/escape-gauge))
              "the report itself is never embedded")
          (is (contains? truncation-sections :cascade))
          (is (contains? truncation-sections :escape-gauge))
          (is (= 17 (count portal/questions))
              "P4 does not widen the seventeen-question card floor")
          (is (= (mapv :question/id portal/questions)
                 (mapv :card/id
                       (:render/cards (mp/render result)))))))
      (finally
        (ocr/close-object-container-runtime! rt)))))

(deftest matter-room-p4-gauge-is-provenance-pinned-and-refreshes-existing-resident
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (boot! rt)
      (adapter/ensure-master! rt provenance/spec)
      (let [state (atom {})
            git-roots (atom [])
            history-calls (atom [])
            read-history
            (fn [runtime container-id cursor limit]
              (swap! history-calls conj
                     [runtime container-id cursor limit])
              (ocr/read-revision-history
               runtime container-id cursor limit))
            deps {:state state
                  :read-commits
                  (fn [repo-root]
                    (swap! git-roots conj repo-root)
                    [{:sha "fixture-clean"
                      :committed-at-ms 0
                      :subject "fixture"
                      :files []}])
                  :read-revision-history read-history}
            ctx {:oc-rt rt :rk-rt nil}
            room-open (sj/open-matter-room!
                       ctx {:master-id attention/master-id})
            gauge-act
            (first (filter #(= :gauge (:section %))
                           (:residents room-open)))
            gauge-before (ocr/read-unit rt (:unit-id gauge-act))
            served
            (fp/escape-gauge-projection
             deps
             ctx
             {:face :escape-gauge
              :params {:master-id attention/master-id
                       :timeout-ms 5000}})
            report (:escape-gauge/report served)
            resident (:escape-gauge/resident served)
            refresh
            (sj/matter-room-refresh!
             rt
             (:object-key room-open)
             (:unit-id gauge-act)
             gauge-before
             resident)
            gauge-after (ocr/read-unit rt (:unit-id gauge-act))
            reopened (sj/open-matter-room!
                      ctx {:master-id attention/master-id})
            reopened-gauge
            (first (filter #(= :gauge (:section %))
                           (:residents reopened)))]
        (testing "G9 · clean IPC history is measured from the single provenance master"
          (is (= :measured (:terminal-escape/status report)))
          (is (= 0 (:terminal-escape/count report)))
          (is (= (vec (sort circulation/default-material-policy-paths))
                 (:terminal-escape/policy-paths report)))
          (is (= [(System/getProperty "user.dir")] @git-roots))
          (is (= [[rt
                   (adapter/active-pointer-container-id provenance/spec)
                   ""
                   100000]]
                 @history-calls)))
        (testing "the gauge resident holds the verbatim report through P2's edit lane"
          (is (= :gauge (:resident/section resident)))
          (is (= report (:resident/report resident)))
          (is (true? (:resident/refreshable? resident)))
          (is (= :refresh (:act refresh)))
          (is (= (:resident/text resident)
                 (:content-text gauge-after)))
          (is (= (:unit-id gauge-act) (:unit-id reopened-gauge)))
          (is (= :append-only (:act reopened-gauge))
              "a standard open never downgrades the last computed report")))
      (finally
        (ocr/close-object-container-runtime! rt)))))

(deftest matter-room-p4-gauge-renders-ambiguity-without-inference
  (let [deps {:state (atom {})
              :read-commits (constantly [])
              :read-revision-history
              (fn [& _]
                [{:revision-id "tip-a"
                  :parent-revision-id nil
                  :created-at-ms 1}
                 {:revision-id "tip-b"
                  :parent-revision-id nil
                  :created-at-ms 2}])}
        served
        (fp/escape-gauge-projection
         deps
         {:oc-rt :fixture}
         {:face :escape-gauge
          :params {:master-id attention/master-id
                   :timeout-ms 5000}})
        report (:escape-gauge/report served)
        resident (:escape-gauge/resident served)]
    (is (= :ambiguous-activation-history
           (:terminal-escape/status report)))
    (is (true? (:terminal-escape/ambiguous? report)))
    (is (nil? (:terminal-escape/count report)))
    (is (nil? (:terminal-escape/escapes report)))
    (is (= report (:resident/report resident)))
    (is (str/includes? (:resident/text resident)
                       ":ambiguous-activation-history"))
    (is (not (str/includes? (:resident/text resident)
                            ":terminal-escape/count 0"))
        "the resident does not infer a clean zero from an ambiguous history")))

(deftest matter-room-p4-gauge-timeout-is-poisoned-total-and-single-flight
  (let [release-git (promise)
        calls (atom 0)
        previous
        {:terminal-escape/status :measured
         :terminal-escape/count 7
         :terminal-escape/escapes []
         :terminal-escape/candidates []
         :terminal-escape/ambiguous? false
         :terminal-escape/policy-paths
         (vec (sort circulation/default-material-policy-paths))}
        state (atom {:last-report previous :in-flight nil})
        deps {:state state
              :read-commits
              (fn [_]
                (swap! calls inc)
                @release-git
                [])
              :read-revision-history
              (fn [& _]
                [{:revision-id "only-tip"
                  :parent-revision-id nil
                  :created-at-ms 1}])}
        request {:face :escape-gauge
                 :params {:master-id attention/master-id
                          :timeout-ms 1}}
        expired
        (fp/escape-gauge-projection deps {:oc-rt :fixture} request)
        flight (:in-flight @state)
        concurrent
        (fp/escape-gauge-projection deps {:oc-rt :fixture} request)]
    (testing "expiry is total and does not launch a second git process"
      (is (= :poisoned
             (get-in expired
                     [:escape-gauge/report
                      :terminal-escape/status])))
      (is (= :terminal-escape/timeout
             (get-in expired
                     [:escape-gauge/report
                      :terminal-escape/error
                      :type])))
      (doseq [k [:terminal-escape/status
                 :terminal-escape/count
                 :terminal-escape/escapes
                 :terminal-escape/candidates
                 :terminal-escape/policy-paths
                 :terminal-escape/activation-history-linear?]]
        (is (contains? (:escape-gauge/report expired) k) (str k)))
      (is (= 1 @calls))
      (is (true? (:escape-gauge/in-flight? expired))))
    (testing "concurrent and post-expiry asks receive the cached last report"
      (is (= previous (:escape-gauge/report concurrent)))
      (is (true? (:escape-gauge/cached? concurrent)))
      (is (true? (:escape-gauge/in-flight? concurrent)))
      (is (= 1 @calls)))
    (deliver release-git true)
    (is (not= ::timeout (deref flight 5000 ::timeout))
        "the abandoned worker finishes naturally")
    (is (nil? (:in-flight @state)))
    (is (= :measured
           (get-in @state
                   [:last-report :terminal-escape/status])))))

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
