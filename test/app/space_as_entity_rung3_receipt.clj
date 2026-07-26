(ns app.space-as-entity-rung3-receipt
  "Rung-3 live-cluster receipts. Every durable drive mints fresh request ids;
   every truth claim is read back from the server, never from a client cache."
  (:require [app.server.rama.cluster :as cluster]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.material-truth :as material-truth]
            [app.server.rama.object-container.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.attention-material :as attention]
            [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.space-material :as space]))

(def sid {:actor/id "sid" :actor/type :human})

(def camera-row
  {:binding/gesture :wheel
   :binding/phase :complete
   :binding/modifiers #{}
   :binding/verb {:verb/name :camera/zoom-at-pointer :verb/version 0}
   :binding/priority 10})

(def foreign-space-row
  {:binding/gesture :pointer/tap
   :binding/phase :complete
   :binding/modifiers #{}
   :binding/verb {:verb/name :anchor/place :verb/version 0}
   :binding/priority 10})

(defn- runtime!
  []
  (or (cluster/object-container-runtime)
      (throw (ex-info "live cluster unavailable — run bin/land up" {}))))

(defn- request-ids
  [label]
  {:request-id (str "space-r3-" label "-" (random-uuid))
   :activation-request-id
   (str "space-r3-" label "-activate-" (random-uuid))})

(defn- write-opts
  [ids]
  {:actor sid
   :time-ms (System/currentTimeMillis)
   :request-id (:request-id ids)
   :activation-request-id (:activation-request-id ids)})

(defn- floor-rows
  []
  (assoc
   (into {}
         (map (fn [spec]
                [(:facet-master/facet spec)
                 (:facet-master/bindings (facet-material/code-floor spec))]))
         facet-masters/specs)
   binding-material/space-facet binding-material/space-floor-bindings))

(defn- served
  [runtime]
  (face-projection/facet-materials-projection
   {:oc-rt runtime}
   {:params {:drill? true :subjects [space/space-subject]}}))

(defn- state
  [runtime]
  (let [s (served runtime)
        shared (get-in s [:facet-materials/by-id space/master-id])
        instance (get-in s [:facet-materials/instances
                            :space space/space-subject])
        shared-wear (space/resolved-wear shared)
        wear (facet-material/wear-for-subject space/spec shared instance)
        interaction
        (face-projection/interaction-table-projection
         {:oc-rt runtime}
         {:params {:drill? true :subjects [space/space-subject]}})]
    {:served s
     :shared shared
     :instance instance
     :shared-wear shared-wear
     :wear wear
     :interaction interaction}))

(defn- drill
  [{:keys [served wear]}]
  (let [by-id (:facet-materials/by-id served)
        shared-wears
        (into {}
              (map (fn [spec]
                     [(:facet-master/facet spec)
                      (facet-material/resolved-wear
                       spec (get by-id (:facet-master/id spec)))]))
              facet-masters/specs)
        facet-rows
        (into {}
              (map (fn [[facet w]]
                     [facet (when-not (:facet-master/floor? w)
                              (:facet-master/bindings w))]))
              shared-wears)
        instance-rows
        (if (= :instance (:facet-master/tier wear))
          ;; T-R1 — drill input is keyed by [claim-subject site], not by a
          ;; claim subject whose value is the whole bindings map.
          (into {}
                (map (fn [[site rows]] [[:space site] rows]))
                (:facet-master/bindings wear))
          {})]
    (binding-material/drill-report
     {:facet-rows facet-rows
      :floor-rows (floor-rows)
      :instance-rows instance-rows})))

(defn- tiers-by-label
  [report]
  (into {} (map (juxt :probe/label :probe/tier)) report))

(defn- history-counts
  [runtime parent-spec subject]
  (let [ispec (facet-master/instance-spec parent-spec subject)]
    {:revisions
     (count
      (ocr/read-revision-history
       runtime (facet-master/document-id ispec) "" 10000))
     :pointers
     (count
      (ocr/read-revision-history
       runtime (facet-master/active-pointer-container-id ispec) "" 10000))}))

(defn g1
  []
  (let [runtime (runtime!)
        s (state runtime)
        report (drill s)
        tiers (tiers-by-label report)
        instance-table-rows
        (filterv #(= :instance (:table/tier %))
                 (get-in s [:interaction :interaction-table/rows]))
        checks
        {:no-space-instance? (nil? (:instance s))
         :twelve-probes? (= 12 (count report))
         :all-claimed? (every? #(= :claimed (:probe/outcome %)) report)
         :tap-master?
         (= :master (get tiers "tap empty space → caret anchor"))
         :marquee-master?
         (= :master (get tiers "shift-drag empty space → marquee"))
         :camera-floor?
         (every?
          #(= :floor (get tiers %))
          ["drag empty space → pan the camera"
           "wheel → zoom at the pointer"
           "wheel at a block → zoom through the space rung"])
         :zero-server-instance-table-rows? (empty? instance-table-rows)
         :shared-base?
         (= [0.1 8.0]
            ((juxt :space/zoom-min :space/zoom-max) (:shared-wear s)))
         :worn-base?
         (= [0.1 8.0]
            ((juxt :space/zoom-min :space/zoom-max) (:wear s)))}
        pass? (every? true? (vals checks))]
    {:gate :R3-G1
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :active-shared-revision
     (:facet-master/active-revision-id (:shared s))
     :space-instance (:instance s)
     :probe-decisions
     (mapv (juxt :probe/label :probe/verb :probe/tier
                 :probe/facet :probe/outcome)
           report)
     :server-instance-table-rows instance-table-rows
     :zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear s))
     :checks checks}))

(defn g3-start
  []
  (let [runtime (runtime!)
        ids (request-ids "g3-deviate")
        result
        (material-truth/deviate!
         runtime space/spec space/space-subject
         {:space/zoom-max 3.0}
         (write-opts ids))
        s (state runtime)
        report (drill s)
        tiers (tiers-by-label report)
        ispec (facet-master/instance-spec space/spec space/space-subject)
        instance-state (facet-master/read-master runtime ispec)
        source (get-in instance-state [:active-revision :content-text])
        reimport-id (str "space-r3-g3-reimport-" (random-uuid))
        reimport
        (facet-master/import-candidate!
         runtime ispec source
         {:request/id reimport-id
          :time-ms (System/currentTimeMillis)
          :actor sid})
        active-revision-id
        (get-in s [:instance :facet-master/active-revision-id])
        checks
        {;; A first deviation is a bootstrap import+pointer and reports accepted.
         ;; A repeat of those content-addressed bytes hits the pre-existing
         ;; bootstrap-vs-normal import fingerprint asymmetry, but its fresh
         ;; activation still lands. Keep both facts visible while judging the
         ;; gate by server-read durable effect, not the adapter's aggregate flag.
         :fresh-write-path-accepted?
         (or (true? (:accepted? result))
             (true? (get-in result [:activation :accepted?])))
         :deviation-landed?
         (= (:revision-id result) active-revision-id)
         :served-facet-space?
         (= :space (get-in s [:instance :facet-master/facet]))
         :served-subject-string?
         (= space/space-subject
            (get-in s [:instance :facet-master/subject]))
         :shared-stays-eight?
         (= 8.0 (:space/zoom-max (:shared-wear s)))
         :instance-clamp-three?
         (= 3.0 (:space/zoom-max (:wear s)))
         :tap-instance?
         (= :instance (get tiers "tap empty space → caret anchor"))
         :marquee-instance?
         (= :instance (get tiers "shift-drag empty space → marquee"))
         :camera-floor?
         (every?
          #(= :floor (get tiers %))
          ["drag empty space → pan the camera"
           "wheel → zoom at the pointer"
           "wheel at a block → zoom through the space rung"])
         :reimport-byte-identical?
         (= (:revision-id result) (:revision-id reimport))}
        pass? (every? true? (vals checks))]
    {:gate :R3-G3
     :stage :deviation-active-awaiting-browser-drive
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-ids (assoc ids :reimport-request-id reimport-id)
     :deviation-write
     {:accepted? (:accepted? result)
      :bootstrap? (:bootstrap? result)
      :import
      (let [write (:import result)]
        {:accepted? (:accepted? write)
         :replay? (:replay? write)
         :status (get-in write [:decision :status])
         :reason (get-in write [:decision :reason])
         :errors (vec (get-in write [:decision :errors]))})
      :activation
      (let [write (:activation result)]
        {:accepted? (:accepted? write)
         :replay? (:replay? write)
         :status (get-in write [:decision :status])
         :reason (get-in write [:decision :reason])
         :errors (vec (get-in write [:decision :errors]))})}
     :instance-revision-id (:revision-id result)
     :reimport-revision-id (:revision-id reimport)
     :shared-revision-id
     (:facet-master/active-revision-id (:shared s))
     :shared-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:shared-wear s))
     :worn-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear s))
     :space-probe-tiers
     (select-keys tiers
                  ["tap empty space → caret anchor"
                   "shift-drag empty space → marquee"
                   "drag empty space → pan the camera"
                   "wheel → zoom at the pointer"
                   "wheel at a block → zoom through the space rung"])
     :checks checks}))

(defn g3-finish
  []
  (let [runtime (runtime!)
        ids (request-ids "g3-release")
        result
        (material-truth/release-deviation!
         runtime space/spec space/space-subject (write-opts ids))
        s (state runtime)
        report (drill s)
        tiers (tiers-by-label report)
        checks
        {:release-accepted? (true? (:accepted? result))
         :wear-restored-to-master?
         (= :shared (:facet-master/tier (:wear s)))
         :clamp-restored-to-eight? (= 8.0 (:space/zoom-max (:wear s)))
         :tap-master?
         (= :master (get tiers "tap empty space → caret anchor"))
         :marquee-master?
         (= :master (get tiers "shift-drag empty space → marquee"))
         :camera-floor?
         (every?
          #(= :floor (get tiers %))
          ["drag empty space → pan the camera"
           "wheel → zoom at the pointer"
           "wheel at a block → zoom through the space rung"])}
        pass? (every? true? (vals checks))]
    {:gate :R3-G3
     :stage :released-and-restored
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-ids ids
     :instance-revision-id (:revision-id result)
     :worn-tier (:facet-master/tier (:wear s))
     :worn-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear s))
     :space-probe-tiers
     (select-keys tiers
                  ["tap empty space → caret anchor"
                   "shift-drag empty space → marquee"
                   "drag empty space → pan the camera"
                   "wheel → zoom at the pointer"
                   "wheel at a block → zoom through the space rung"])
     :checks checks}))

(defn g4-start
  []
  (let [runtime (runtime!)
        before (state runtime)
        base-id (:facet-master/active-revision-id (:shared before))
        pin-ids (request-ids "g4-pin")
        pin
        (material-truth/pin!
         runtime space/spec space/space-subject base-id (write-opts pin-ids))
        candidate-id (str "space-r3-g4-shared-candidate-" (random-uuid))
        candidate
        (facet-master/import-candidate!
         runtime space/spec
         (pr-str (assoc space/default-form :space/zoom-max 2.0))
         {:request/id candidate-id
          :time-ms (System/currentTimeMillis)
          :actor sid})
        activation-id (str "space-r3-g4-shared-activate-" (random-uuid))
        activation
        (facet-master/activate!
         runtime space/spec (:revision-id candidate)
         {:request/id activation-id
          :time-ms (System/currentTimeMillis)
          :actor sid
          :activation/kind :activate
          :activation/grounds
          [{:ground/type :gate
            :ground/ref "space-as-entity/R3-G4-shared-2.0"}]})
        after (state runtime)
        checks
        {:pin-accepted? (true? (:accepted? pin))
         :shared-activation-accepted? (true? (:accepted? activation))
         :shared-is-two? (= 2.0 (:space/zoom-max (:shared-wear after)))
         :pin-holds-eight? (= 8.0 (:space/zoom-max (:wear after)))
         :wear-is-pinned? (true? (:facet-master/pinned? (:wear after)))
         :pin-target-is-base?
         (= base-id (:facet-master/revision-id (:wear after)))}
        pass? (every? true? (vals checks))]
    {:gate :R3-G4
     :stage :pin-holds-awaiting-browser-drive
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-ids
     (assoc pin-ids
            :shared-candidate-request-id candidate-id
            :shared-activation-request-id activation-id)
     :base-revision-id base-id
     :shared-two-revision-id (:revision-id candidate)
     :shared-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:shared-wear after))
     :worn-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear after))
     :checks checks}))

(defn g4-unpin
  []
  (let [runtime (runtime!)
        ids (request-ids "g4-unpin")
        result
        (material-truth/unpin!
         runtime space/spec space/space-subject (write-opts ids))
        after (state runtime)
        checks
        {:unpin-accepted? (true? (:accepted? result))
         :wear-joins-shared? (= :shared (:facet-master/tier (:wear after)))
         :shared-is-two? (= 2.0 (:space/zoom-max (:shared-wear after)))
         :wear-is-two? (= 2.0 (:space/zoom-max (:wear after)))}
        pass? (every? true? (vals checks))]
    {:gate :R3-G4
     :stage :unpinned-awaiting-browser-drive
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-ids ids
     :shared-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:shared-wear after))
     :worn-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear after))
     :checks checks}))

(defn g4-finish
  [base-revision-id]
  (let [runtime (runtime!)
        request-id (str "space-r3-g4-rollback-" (random-uuid))
        result
        (facet-master/activate!
         runtime space/spec base-revision-id
         {:request/id request-id
          :time-ms (System/currentTimeMillis)
          :actor sid
          :activation/kind :rollback
          :activation/grounds
          [{:ground/type :gate
            :ground/ref "space-as-entity/R3-G4-rollback-base"}]})
        after (state runtime)
        checks
        {:rollback-accepted? (true? (:accepted? result))
         :base-active?
         (= base-revision-id
            (:facet-master/active-revision-id (:shared after)))
         :shared-restored-eight?
         (= 8.0 (:space/zoom-max (:shared-wear after)))
         :wear-restored-eight? (= 8.0 (:space/zoom-max (:wear after)))}
        pass? (every? true? (vals checks))]
    {:gate :R3-G4
     :stage :shared-pointer-rolled-back
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-id request-id
     :active-shared-revision
     (:facet-master/active-revision-id (:shared after))
     :shared-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:shared-wear after))
     :worn-zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:wear after))
     :checks checks}))

(defn g5
  []
  (let [runtime (runtime!)
        foreign-subject (str "space-r3-g5-foreign-" (random-uuid))
        before-space
        (history-counts runtime space/spec space/space-subject)
        before-foreign
        (history-counts runtime attention/spec foreign-subject)
        camera-ids (request-ids "g5a-camera")
        camera
        (material-truth/deviate!
         runtime space/spec space/space-subject
         {:facet-master/bindings {:space/ground [camera-row]}}
         (write-opts camera-ids))
        foreign-ids (request-ids "g5b-foreign")
        foreign
        (material-truth/deviate!
         runtime attention/spec foreign-subject
         {:facet-master/bindings {:space/ground [foreign-space-row]}}
         (write-opts foreign-ids))
        clamp-ids (request-ids "g5c-clamp")
        clamp
        (material-truth/deviate!
         runtime space/spec space/space-subject
         {:space/zoom-min 5.0 :space/zoom-max 4.0}
         (write-opts clamp-ids))
        after-space
        (history-counts runtime space/spec space/space-subject)
        after-foreign
        (history-counts runtime attention/spec foreign-subject)
        checks
        {:camera-refused? (false? (:accepted? camera))
         :camera-error?
         (= [:facet-master/bindings-invalid]
            (mapv :type (:errors camera)))
         :foreign-refused? (false? (:accepted? foreign))
         :foreign-site-error?
         (= :facet-master/instance-site-refused (:reason foreign))
         :clamp-refused? (false? (:accepted? clamp))
         :clamp-error?
         (= [:space/zoom-clamp-invalid] (mapv :type (:errors clamp)))
         :space-history-unchanged? (= before-space after-space)
         :foreign-history-unchanged? (= before-foreign after-foreign)}
        pass? (every? true? (vals checks))]
    {:gate :R3-G5
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :request-ids
     {:camera camera-ids :foreign foreign-ids :clamp clamp-ids}
     :camera-result camera
     :foreign-result foreign
     :clamp-result clamp
     :space-history {:before before-space :after after-space}
     :foreign-history {:before before-foreign :after after-foreign}
     :checks checks}))

(defn- emit!
  [receipt]
  (println (pr-str receipt))
  (System/exit (if (= :green (:status receipt)) 0 2)))

(defn -main
  [& [gate arg]]
  (try
    (case gate
      "g1" (emit! (g1))
      "g3-start" (emit! (g3-start))
      "g3-finish" (emit! (g3-finish))
      "g4-start" (emit! (g4-start))
      "g4-unpin" (emit! (g4-unpin))
      "g4-finish"
      (if (seq arg)
        (emit! (g4-finish arg))
        (throw (ex-info "g4-finish requires the base revision id" {})))
      "g5" (emit! (g5))
      (do
        (println
         (str "usage: clj -M:test -m app.space-as-entity-rung3-receipt "
              "g1|g3-start|g3-finish|g4-start|g4-unpin|"
              "g4-finish <base-revision-id>|g5"))
        (System/exit 64)))
    (catch Throwable t
      (binding [*out* *err*]
        (println
         (pr-str {:gate gate :status :error :message (.getMessage t)})))
      (System/exit 1))))
