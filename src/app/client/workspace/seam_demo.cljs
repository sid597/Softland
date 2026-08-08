(ns app.client.workspace.seam-demo
  "The composed Region3D coexistence demo, enabled only by all three flags."
  (:require [app.client.substrate.connector-material :as connector-material]
            [app.client.substrate.region3d-material :as region3d-material]
            [app.client.workspace.editing-runtime :as editing-runtime]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.region3d-runtime :as region3d-runtime]
            [app.client.workspace.scene-runtime :as scene-runtime]
            [app.client.workspace.scene-store :as scene-store]))

(def demo-edge-vi :region3d-seam/demo-edge)
(def demo-relation-id :region3d-seam/paragraph-to-grey-box)
(def demo-text-object-id :seam/placed-paragraph)
(def demo-ink-object-id :seam/placed-ink)
(def demo-anchor-object-id :left-box)
(def demo-text-address :t2-input-floor/paragraph-text)
(def demo-ink-address :live-atoms/ink-pressure)
(def demo-region-address :region3d/fixture-region)
(def demo-hook-id :region3d-seam/demo)

(def demo-edge-instance-id
  [demo-relation-id editing-runtime/paragraph-vi
   [:region-object demo-region-address demo-anchor-object-id]])

(def task-script
  ["orbit"
   "click a mesh"
   "click the placed text and inspect its cluster route"
   "double-click the 2D paragraph and type; watch the 3D mirror"
   "drag the grey-box gizmo; watch the arrow and label follow"
   "reload with all three flags; session dress is gone and material replays"])

(defonce ^:private !mounted? (atom false))
(defonce ^:private !registrations (atom {}))

(defn flag-enabled-search? [search]
  (let [params (js/URLSearchParams. (or search ""))]
    (and (= "1" (.get params "live-atoms"))
         (= "1" (.get params "region3d"))
         (= "1" (.get params "seam-demo")))))

(defn enabled? [] (flag-enabled-search? (.-search js/location)))

(defn- transform [translation scale rotation]
  {:translation translation :rotation rotation :scale scale})

(defn- placed-text []
  {:object/id demo-text-object-id :object/kind :text :parent nil
   :transform (transform [-3.1 1.45 1.35] [0.012 0.012 0.012]
                         [0.0 0.258819 0.0 0.965926])
   :provenance {:asserted-by :sid}
   :text {:ref {:address demo-text-address}
          :params {:color nil :max-inline-size nil}}})

(defn- placed-ink []
  {:object/id demo-ink-object-id :object/kind :ink :parent nil
   :transform (transform [-2.6 -0.95 1.65] [0.018 0.018 0.018]
                         [0.0 -0.173648 0.0 0.984808])
   :provenance {:asserted-by :sid}
   :ink {:ref {:address demo-ink-address}}})

(defn seam-region []
  (-> (region3d-runtime/fixture-region)
      (assoc-in [:scene demo-text-object-id] (placed-text))
      (assoc-in [:scene demo-ink-object-id] (placed-ink))
      region3d-material/validate-region!))

(defn- region-tree []
  (rt/rt-node
   :region3d/fixture-region-root :group
   {:x 0.0 :y 0.0 :w 720.0 :h 480.0}
   :children
   [(rt/rt-node
     :region3d/fixture-region-node :region3d
     {:x 0.0 :y 0.0 :w 720.0 :h 480.0}
     :data {:address demo-region-address
            :region3d/id region3d-runtime/fixture-region-id
            :region3d/scene (seam-region)})]))

(defn- edge-material []
  (connector-material/validate-material!
   {:connector/relation-id demo-relation-id
    :connector/row-stamp [:region3d-seam demo-relation-id]
    :connector/dress-revision 0
    :connector/kind :references
    :connector/from {:bind :node :target demo-text-address :anchor :boundary}
    :connector/to {:bind :region-object :region demo-region-address
                   :object demo-anchor-object-id :local [0.4 0.25 0.3]}
    :connector/route {:policy :straight :waypoints []}
    :connector/heads {:from :none :to :triangle
                      :size-k connector-material/default-head-size-k}
    :connector/label {:text "same material · region object"
                      :at 0.58 :offset [0.0 -12.0]}
    :connector/paint
    {:color (connector-material/projection-color :references :human)
     :opacity 1.0 :width 3.0
     :color-space :srgb :alpha-association :straight}
    :connector/status :asserted
    :connector/provenance {:actor-id "sid" :asserter-type :human}}))

(defn- edge-tree []
  (rt/rt-node
   :region3d-seam/edge :connector
   {:x 0.0 :y 0.0 :w 1800.0 :h 1200.0}
   :data {:address demo-edge-instance-id
          :connector/edge-instance-id demo-edge-instance-id
          :connector/from-vi editing-runtime/paragraph-vi
          :connector/to-vi nil
          :connector/material (edge-material)}))

(defn replay-receipt []
  (let [tape (scene-store/scene-tape (scene-runtime/store-snapshot)
                                     (scene-runtime/effective-transforms))]
    {:scene (seam-region)
     :order-hash (:order-hash tape)}))

(defn- register! [vi tree opts]
  (scene-runtime/close-instance! vi)
  (let [registration
        (scene-runtime/register-face-instance!
         vi tree
         (merge {:scale 1.0 :stratum :world
                 :meta {:live-atoms? true :region3d-seam-demo? true
                        :material/id vi :material/revision 1}}
                opts))]
    (swap! !registrations assoc vi registration)
    registration))

(defn boot! []
  (when (and (enabled?) (compare-and-set! !mounted? false true))
    (let [region-registration
          (register! region3d-runtime/fixture-region-vi (region-tree)
                     {:x 256.0 :y 166.0 :layer 41 :sibling-rank 41})
          edge-registration
          (register! demo-edge-vi (edge-tree)
                     {:x 0.0 :y 0.0 :layer 43 :sibling-rank 43})
          receipt {:enabled true
                   :flags ["live-atoms=1" "region3d=1" "seam-demo=1"]
                   :region region3d-runtime/fixture-region-id
                   :placements [demo-text-object-id demo-ink-object-id]
                   :material-addresses [demo-text-address demo-ink-address]
                   :region-edge demo-relation-id
                   :task-script task-script
                   :registrations [region-registration edge-registration]}]
      (aset js/globalThis "__softlandSeamDemoReceipt" (clj->js receipt))
      (set! (.-seamDemo js/window)
            #js {:receipt (fn [] (clj->js receipt))
                 :script (fn [] (clj->js task-script))
                 :replay (fn [] (clj->js (replay-receipt)))})
      (js/console.log "[REGION3D-SEAM] THE SEAM DEMO mounted" (clj->js task-script))
      receipt)))

(defn install! []
  (region3d-runtime/install-fixture-hook! demo-hook-id boot!)
  (when (region3d-runtime/fixture-mounted?) (boot!))
  true)

(defn unmount! []
  (region3d-runtime/install-fixture-hook! demo-hook-id nil)
  (doseq [vi (keys @!registrations)] (scene-runtime/close-instance! vi))
  (reset! !registrations {})
  (reset! !mounted? false)
  (js-delete js/globalThis "__softlandSeamDemoReceipt")
  true)
