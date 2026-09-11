(ns softland.inland.scene
  "Pure adaptation of authored shapes to the existing Region3D schema.
   Takes shapes, extent and camera/light options; gives a validated region material.
   Owns no resources; the render surface owns preparation and GPU lifetime. Uses
   primitive defaults, a perspective lens and a fixed workbench region identity.
   Embedded provenance tags are inherited adapter metadata, not Rama admission proof."
  (:require [app.client.region3d.component :as component]))

(defn tagged
  "RGB or RGBA vector → straight-alpha sRGB color, adding opaque alpha for RGB.
   Does not validate channel ranges; region validation is the consuming boundary."
  [rgba]
  {:rgba (if (= 3 (count rgba)) (conj rgba 1.0) rgba)
   :color-space :srgb :alpha-association :straight})

(defn trs
  "Translation and scale vectors → transform with identity quaternion rotation."
  [position scale]
  {:translation position :rotation [0.0 0.0 0.0 1.0] :scale scale})

(defn mesh
  "Object id and authored shape → Region3D mesh with primitive defaults.
   Reads transform, color and surface properties; no geometry is prepared here."
  [id shape]
  {:object/id id :object/kind :mesh :parent nil
   :transform (assoc (trs (:position shape) (:scale shape)) :rotation (:rotation shape [0.0 0.0 0.0 1.0]))
   :provenance {:asserted-by :sid :act :smalltalk-experiment}
   :mesh {:kind (:kind shape) :params (get component/primitive-defaults (:kind shape))}
   :component {:base-color (tagged (:color shape)) :metallic (:metallic shape 0.0) :roughness (:roughness shape 0.5)
               :emissive (tagged [0.0 0.0 0.0])}})

(defn light
  "Id, light kind, position, color and intensity → unshadowed light record.
   Point lights receive range 20; the region validator checks the resulting schema."
  [id kind position color intensity]
  {:object/id id :object/kind :light :parent nil
   :transform (trs position [1.0 1.0 1.0])
   :provenance {:asserted-by :sid :act :smalltalk-experiment}
   :light (cond-> {:kind kind :color (tagged color) :intensity intensity :cast-shadow false}
            (= :point kind) (assoc :range 20.0))})

(defn region
  "Shapes, width/height and complete view options → validated region material.
   Combines mesh/light ids into one scene and hashes its value as the revision.
   Requires valid ambient/camera/light inputs; throws on schema failure. Fixed
   :workbench identity means this adapter does not supply independent scene slots."
  [shapes width height options]
  (let [value {:region/id :workbench :region3d/version 1
               :extent {:width (double width) :height (double height) :depth 100.0}
               :background {:kind :transparent :color (tagged [0.0 0.0 0.0 0.0])}
               :ambient {:color (tagged (:ambient-color options)) :intensity (:ambient-intensity options)}
               :view (assoc (:camera options) :lens component/default-perspective-lens)
               :scene (merge (into {} (map (fn [[id shape]] [id (mesh id shape)]) shapes))
                             (into {} (for [{:keys [id kind position color intensity]} (:lights options)]
                                        [id (light id kind position color intensity)])))
               :region/rect {:x 0.0 :y 0.0 :w (double width) :h (double height)}}]
    (component/validate-region! (assoc value :region/revision (hash value)))))

