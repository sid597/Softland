(ns app.client.region3d.evaluation
  "Retained Region3D evaluation across authoritative material and transient
  session transforms. Transform dirtiness is a component, not a material
  invalidation: topology/material changes derive; transform changes maintain."
  (:require [app.client.region3d.scene :as scene]))

(defn session-transform-map
  "Resolve authoritative transforms plus settled and one transient preview.
  Session overlays may name only objects owned by the region material."
  [region session-row]
  (let [object-ids (set (keys (:scene region)))
        settled (or (:settled-transforms session-row) {})
        preview (:preview-transform session-row)
        overlays (cond-> settled
                   (and (:object-id preview) (:transform preview))
                   (assoc (:object-id preview) (:transform preview)))]
    (doseq [object-id (keys overlays)]
      (when-not (contains? object-ids object-id)
        (throw (ex-info "Region3D session transform target is missing"
                        {:object-id object-id}))))
    (reduce-kv (fn [result object-id object]
                 (assoc result object-id
                        (get overlays object-id (:transform object))))
               {} (:scene region))))

(defn session-region-value [region session-row]
  (reduce-kv (fn [value object-id transform]
               (assoc-in value [:scene object-id :transform] transform))
             region (session-transform-map region session-row)))

(defn evaluation-key [region session-row]
  {:static-material
   (-> region
       (dissoc :background)
       (update :scene
               (fn [objects]
                 (into {} (map (fn [[object-id object]]
                                 [object-id (dissoc object :transform)]))
                       objects))))
   :transforms (session-transform-map region session-row)})

(defn evaluate-scene
  "Return the retained evaluated scene and the exact dirty component.
  Background is intentionally outside this function and remains the render
  edge's independent dirty role."
  [maintained prior-key region session-row]
  (let [next-key (evaluation-key region session-row)]
    (cond
      (or (nil? maintained)
          (not= (:static-material prior-key)
                (:static-material next-key)))
      {:maintained (scene/derive-scene
                    (session-region-value region session-row))
       :evaluation-key next-key
       :update-kind :full
       :affected-object-ids (set (keys (:scene region)))}

      (= (:transforms prior-key) (:transforms next-key))
      {:maintained maintained
       :evaluation-key next-key
       :update-kind :none
       :affected-object-ids #{}}

      :else
      (let [changed
            (into {}
                  (remove (fn [[object-id transform]]
                            (= transform
                               (get-in prior-key [:transforms object-id]))))
                  (:transforms next-key))
            maintained (scene/maintain-transforms maintained changed)]
        {:maintained maintained
         :evaluation-key next-key
         :update-kind :transform
         :affected-object-ids
         (get-in maintained [:receipt :affected-object-ids])}))))
