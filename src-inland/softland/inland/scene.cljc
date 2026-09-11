(ns softland.inland.scene
  "Pure scene declaration over the existing Region3D primitives. Accepted shape
   components determine the meshes; the owning render view manages resources."
  (:require [app.client.region3d.component :as component]))

(defn tagged [rgba]
  {:rgba (if (= 3 (count rgba)) (conj rgba 1.0) rgba)
   :color-space :srgb :alpha-association :straight})

(defn trs [position scale]
  {:translation position :rotation [0.0 0.0 0.0 1.0] :scale scale})

(defn mesh [id shape]
  {:object/id id :object/kind :mesh :parent nil
   :transform (assoc (trs (:position shape) (:scale shape)) :rotation (:rotation shape [0.0 0.0 0.0 1.0]))
   :provenance {:asserted-by :sid :act :smalltalk-experiment}
   :mesh {:kind (:kind shape) :params (get component/primitive-defaults (:kind shape))}
   :component {:base-color (tagged (:color shape)) :metallic (:metallic shape 0.0) :roughness (:roughness shape 0.5)
               :emissive (tagged [0.0 0.0 0.0])}})

(defn light [id kind position color intensity]
  {:object/id id :object/kind :light :parent nil
   :transform (trs position [1.0 1.0 1.0])
   :provenance {:asserted-by :sid :act :smalltalk-experiment}
   :light (cond-> {:kind kind :color (tagged color) :intensity intensity :cast-shadow false}
            (= :point kind) (assoc :range 20.0))})

(defn region [shapes width height options]
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

