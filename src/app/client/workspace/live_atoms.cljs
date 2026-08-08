(ns app.client.workspace.live-atoms
  "Dev-flag-only product join for the image, path, and connector atoms.

   Namespace loading is pure. The URL is read only when `augment-pipelines!`
   is called; flag-off returns the exact input pipelines value and performs no
   construction or scene mutation."
  (:require [app.client.substrate.image-material :as image-material]
            [app.client.substrate.connector-material :as connector-material]
            [app.client.substrate.connector-route :as connector-route]
            [app.client.substrate.webgpu.chrome-gpu :as chrome-gpu]
            [app.client.substrate.webgpu.connector-gpu :as connector-gpu]
            [app.client.substrate.webgpu.path-gpu :as path-gpu]
            [app.client.substrate.webgpu.renderer :as renderer]
            [app.client.workspace.chrome-runtime :as chrome-runtime]
            [app.client.workspace.editing-runtime :as editing-runtime]
            [app.client.workspace.frame-runtime :as frame-runtime]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.seam-demo :as seam-demo]
            [app.client.workspace.scene-runtime :as scene-runtime]))

(def fixture-vi :live-atoms/fixture)
(def fixture-secondary-vi :live-atoms/fixture-secondary)
(def fixture-image-vi :live-atoms/fixture-image)
(def fixture-ink-pressure-vi :live-atoms/fixture-ink-pressure)
(def fixture-ink-self-cross-vi :live-atoms/fixture-ink-self-cross)
(def fixture-holed-vi :live-atoms/fixture-holed)

(defn flag-enabled-search? [search]
  (= "1" (.get (js/URLSearchParams. (or search "")) "live-atoms")))

(defn live-atoms-enabled? []
  (flag-enabled-search? (.-search js/location)))

(defn- sha256-hex [bytes]
  (-> (.digest (.-subtle js/crypto) "SHA-256" bytes)
      (.then
       (fn [digest]
         (apply str
                (map (fn [byte]
                       (.padStart (.toString byte 16) 2 "0"))
                     (array-seq (js/Uint8Array. digest))))))))

(defn- procedural-png-bytes! []
  (let [size 32
        canvas (js/OffscreenCanvas. size size)
        context (.getContext canvas "2d")]
    (set! (.-fillStyle context) "#10243d")
    (.fillRect context 0 0 size size)
    (doseq [index (range 0 size 4)]
      (set! (.-fillStyle context)
            (if (even? (quot index 4)) "#f4b942" "#46c2a8"))
      (.fillRect context index 0 4 size))
    (set! (.-fillStyle context) "rgba(250, 250, 255, 0.78)")
    (.beginPath context)
    (.arc context 16 16 9 0 (* 2 js/Math.PI))
    (.fill context)
    (-> (.convertToBlob canvas #js {:type "image/png"})
        (.then #(.arrayBuffer %)))))

(defn- paint [color opacity]
  {:color color :opacity opacity
   :color-space :srgb :alpha-association :straight})

(defn- ink-material [id revision samples color]
  {:path/material-id id
   :path/revision revision
   :path/kind :ink
   :path/geometry
   {:knots (mapv (fn [index [x y pressure]]
                   {:knot/id [id index]
                    :position [x y]
                    :pressure pressure
                    :gesture-time :explicitly-absent
                    :source-event-ids []})
                 (range) samples)
    :base-width 18.0 :cap :round :join :round}
   :path/paint (paint color 1.0)
   :path/provenance {:actor :live-atoms
                     :act :procedural-fixture
                     :parents []}})

(defn- shape-material []
  {:path/material-id :live-atoms/holed-concave
   :path/revision 1
   :path/kind :shape
   :path/geometry
   {:open-width 4.0
    :contours
    [{:contour/id :outer :role :outer
      :points [[0.0 0.0] [130.0 0.0] [130.0 110.0]
               [78.0 110.0] [78.0 48.0] [52.0 48.0]
               [52.0 110.0] [0.0 110.0]]}
     {:contour/id :hole :role :hole
      :points [[12.0 12.0] [42.0 12.0] [42.0 40.0] [12.0 40.0]]}]}
   :path/paint (paint [0.94 0.34 0.22 0.92] 1.0)
   :path/provenance {:actor :live-atoms
                     :act :procedural-fixture
                     :parents []}})

(defn- connector-material
  [relation-id from to actor-id asserter-type
   & {:keys [route heads label width]
      :or {route {:policy :straight :waypoints []}
           heads {:from :none :to :triangle
                  :size-k connector-material/default-head-size-k}
           width 2.5}}]
  (connector-material/validate-material!
   {:connector/relation-id relation-id
    :connector/row-stamp [:live-atoms relation-id]
    :connector/dress-revision 0
    :connector/kind :references
    :connector/from {:bind :node :target from :anchor :boundary}
    :connector/to {:bind :node :target to :anchor :boundary}
    :connector/route route
    :connector/heads heads
    :connector/label label
    :connector/paint
    {:color (connector-material/projection-color :references asserter-type)
     :opacity 1.0 :width width
     :color-space :srgb :alpha-association :straight}
    :connector/status :asserted
    :connector/provenance {:actor-id actor-id
                           :asserter-type asserter-type}}))

(defn- connector-node [relation-id from-vi to-vi material]
  (let [edge-instance-id [relation-id from-vi to-vi]]
    (rt/rt-node
     [:live-atoms/connector relation-id] :connector
     {:x 0.0 :y 0.0 :w 1000.0 :h 500.0}
     :data {:address edge-instance-id
            :connector/edge-instance-id edge-instance-id
            :connector/from-vi from-vi
            :connector/to-vi to-vi
            :connector/material material})))

(defn- connector-fixtures []
  [(connector-node
    :live-atoms/straight-label fixture-image-vi fixture-ink-pressure-vi
    (connector-material
     :live-atoms/straight-label :live-atoms/image :live-atoms/ink-pressure
     "sid" :human
     :label {:text "references" :at 0.5 :offset [0.0 -12.0]}))
   (connector-node
    :live-atoms/elbow-waypoints fixture-image-vi fixture-holed-vi
    (connector-material
     :live-atoms/elbow-waypoints :live-atoms/image :live-atoms/holed-concave
     "sid" :human
     :route {:policy :elbow/v1 :waypoints [[330.0 286.0]]}
     :heads {:from :triangle :to :triangle
             :size-k connector-material/default-head-size-k}
     :width 3.0))
   (connector-node
    :live-atoms/overlap-human fixture-ink-self-cross-vi fixture-holed-vi
    (connector-material
     :live-atoms/overlap-human :live-atoms/ink-self-cross
     :live-atoms/holed-concave "sid" :human))
   (connector-node
    :live-atoms/overlap-llm fixture-ink-self-cross-vi fixture-holed-vi
    (connector-material
     :live-atoms/overlap-llm :live-atoms/ink-self-cross
     :live-atoms/holed-concave "llm:connector-fixture" :llm))
   (connector-node
    :live-atoms/cross-container fixture-holed-vi fixture-secondary-vi
    (connector-material
     :live-atoms/cross-container :live-atoms/holed-concave
     :live-atoms/secondary-target "sid" :human))
   (connector-node
    :live-atoms/unresolved fixture-image-vi :connector/unresolved-to
    (connector-material
     :live-atoms/unresolved :live-atoms/image :live-atoms/does-not-exist
     "sid" :human))])

(defn fixture-secondary-tree []
  (rt/rt-node
   :live-atoms/secondary-root :group {:x 0.0 :y 0.0 :w 170.0 :h 150.0}
   :children
   [(rt/rt-node
     :live-atoms/secondary-target :rect {:x 24.0 :y 32.0 :w 112.0 :h 72.0}
     :style {:bg [0.22 0.78 0.56 0.9] :radius 10.0}
     :data {:address :live-atoms/secondary-target})]))

(defn fixture-tree [digest]
  (rt/rt-node
   :live-atoms/root :group {:x 0.0 :y 0.0 :w 1000.0 :h 500.0}
   :children
   (into
    [(rt/rt-node
     :live-atoms/image :image {:x 56.0 :y 68.0 :w 112.0 :h 112.0}
     :data {:address :live-atoms/image
            :image/digest digest
            :image/color-tag :srgb
            :image/alpha-association :straight
            :image/intrinsic-size [32 32]
            :image/opacity 1.0})
    (rt/rt-node
     :live-atoms/ink-pressure :path {:x 210.0 :y 66.0 :w 150.0 :h 100.0}
     :data {:address :live-atoms/ink-pressure
            :path/material
            (ink-material :live-atoms/ink-pressure 1
                          [[8.0 56.0 0.2] [46.0 22.0 0.45]
                           [96.0 70.0 0.72] [138.0 28.0 1.0]]
                          [0.20 0.68 0.96 0.95])})
    (rt/rt-node
     :live-atoms/ink-self-cross :path {:x 210.0 :y 206.0 :w 150.0 :h 130.0}
     :data {:address :live-atoms/ink-self-cross
            :path/material
            (ink-material :live-atoms/ink-self-cross 1
                          [[12.0 18.0 0.55] [132.0 108.0 0.8]
                           [20.0 110.0 1.0] [132.0 18.0 0.65]]
                          [0.86 0.48 0.96 0.62])})
    (rt/rt-node
     :live-atoms/holed-concave :path {:x 420.0 :y 92.0 :w 140.0 :h 120.0}
     :data {:address :live-atoms/holed-concave
            :path/material (shape-material)})]
    (connector-fixtures))))

(defn- standalone-node [node]
  (assoc node :bounds (assoc (:bounds node) :x 0.0 :y 0.0)))

(defn- fixture-parts [digest]
  (let [children (:children (fixture-tree digest))
        by-id (into {} (map (juxt :id identity)) children)]
    [{:vi fixture-image-vi :node (standalone-node (get by-id :live-atoms/image))
      :x 96.0 :y 100.0 :layer 8}
     {:vi fixture-ink-pressure-vi
      :node (standalone-node (get by-id :live-atoms/ink-pressure))
      :x 250.0 :y 98.0 :layer 8}
     {:vi fixture-ink-self-cross-vi
      :node (standalone-node (get by-id :live-atoms/ink-self-cross))
      :x 250.0 :y 238.0 :layer 8}
     {:vi fixture-holed-vi
      :node (standalone-node (get by-id :live-atoms/holed-concave))
      :x 460.0 :y 124.0 :layer 8}]))

(defn- connector-fixture-tree []
  (rt/rt-node :live-atoms/connectors-root :group
              {:x 0.0 :y 0.0 :w 1000.0 :h 500.0}
              :children (connector-fixtures)))

(defn- image-source [digest]
  {:image/digest digest
   :image/color-tag :srgb
   :image/width 32 :image/height 32
   :image/bytes-route {:kind :live-atoms/procedural-client-bytes}
   :image/ingress-receipt image-material/ingress-receipt
   :image/alpha-association :straight})

(defn augment-pipelines!
  "Construct all atom systems and inject the fixture only behind `?live-atoms=1`.
   The scene-store upsert waits until every async source registration resolves,
   preventing the image prepare identity guard from pinning a placeholder."
  [resources pipelines]
  (if-not (live-atoms-enabled?)
    (js/Promise.resolve pipelines)
    (let [device (:device resources)
          tracker (:gpu-budget resources)
          format (:format pipelines)
          camera-buffer (get-in pipelines [:text-sys :camera-uniform-buffer])
          containers-buffer (:containers-buffer pipelines)
          scene-color (:scene-color pipelines)
          image-system (renderer/init-image-system
                        device format camera-buffer containers-buffer
                        :tracker tracker :scene-color scene-color)
          path-system (path-gpu/init-path-system
                       device format camera-buffer containers-buffer
                       :tracker tracker :scene-color scene-color)
          connector-system
          (connector-gpu/init-connector-system
           device format camera-buffer containers-buffer
           :tracker tracker :scene-color scene-color
           :text-api {:clone renderer/clone-text-system
                      :update renderer/update-text-data
                      :destroy renderer/destroy-text-system!})
          chrome-system
          (chrome-gpu/init-chrome-system
           device format camera-buffer containers-buffer
           :tracker tracker :scene-color scene-color)
          augmented (assoc pipelines
                           :image-system image-system
                           :path-system path-system
                           :connector-system connector-system
                           :chrome-system chrome-system)]
      (connector-route/set-live-effective-provider!
       scene-runtime/effective-transforms)
      (-> (procedural-png-bytes!)
          (.then (fn [bytes]
                   (-> (sha256-hex bytes)
                       (.then (fn [digest] {:bytes bytes :digest digest})))))
          (.then
           (fn [{:keys [bytes digest]}]
             (-> (js/Promise.all
                  #js [(renderer/register-image-source!
                        image-system (image-source digest) bytes)])
                 (.then (fn [registrations]
                          (when-not (aget registrations 0)
                            (throw (js/Error.
                                    "Live-atoms procedural image registration failed")))
                          (scene-runtime/close-instance! fixture-vi)
                          (scene-runtime/close-instance! fixture-secondary-vi)
                          (doseq [vi [fixture-image-vi fixture-ink-pressure-vi
                                     fixture-ink-self-cross-vi fixture-holed-vi]]
                            (scene-runtime/close-instance! vi))
                          (let [opacity-group
                                (frame-runtime/install-live-opacity-group!)
                                opacity-container (:container opacity-group)
                                secondary-registration
                                (scene-runtime/register-face-instance!
                                 fixture-secondary-vi (fixture-secondary-tree)
                                 {:x 748.0 :y 156.0 :scale 1.0 :layer 9
                                  :sibling-rank 9
                                  :parent opacity-container
                                  :meta {:live-atoms? true
                                         :material/id
                                         :live-atoms/connector-secondary
                                         :material/revision 1}})
                                fixture-registrations
                                (mapv
                                 (fn [{:keys [vi node x y layer]}]
                                   (scene-runtime/register-face-instance!
                                    vi node
                                    {:x x :y y :scale 1.0 :layer layer
                                     :sibling-rank layer
                                     :parent opacity-container
                                     :meta {:live-atoms? true
                                            :manipulable-fixture? true
                                            :material/id vi
                                            :material/revision 1}}))
                                 (fixture-parts digest))
                                registration
                                (scene-runtime/register-face-instance!
                                 fixture-vi (connector-fixture-tree)
                                 {:x 40.0 :y 32.0 :scale 1.0 :layer 10
                                  :sibling-rank 10
                                  :parent opacity-container
                                  :meta {:live-atoms? true
                                         :connector-fixture? true
                                         :material/id :live-atoms/connectors
                                         :material/revision 1}})
                                receipt {:enabled true
                                         :image-system true
                                         :path-system true
                                         :connector-system true
                                         :chrome-system true
                                         :fixture fixture-vi
                                         :secondary-fixture fixture-secondary-vi
                                         :image-digest digest
                                         :registration registration
                                         :opacity-group opacity-group
                                         :fixture-registrations fixture-registrations
                                         :secondary-registration
                                         secondary-registration
                                         :awaited-registrations
                                         (.-length registrations)}]
                            (chrome-runtime/boot! chrome-system)
                            ;; W4 boot follows chrome so the pulse stop predicate
                            ;; reads chrome's published selection census.
                            (frame-runtime/boot!)
                            ;; T2 boots last: its Escape/input precedence composes
                            ;; over chrome, and blink joins W4's scheduler sink.
                            (editing-runtime/boot!
                             {:layout-provider
                              (get-in resources [:font-assets :layout-provider])
                              :camera-provider ground/camera-snapshot
                              :effective-provider
                              scene-runtime/effective-transforms})
                            (seam-demo/install!)
                            (aset js/globalThis
                                  "__softlandLiveAtomsReceipt"
                                  (clj->js receipt))
                            augmented))))))))))
