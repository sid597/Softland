(ns app.client.path.frame
  "The pure path-to-engine request for one frame.
   Takes: path draw-items plus a zoom lod.
   Gives: the revision/group frame key.
   Holds nothing.")

(defn frame-key [draw-items lod]
  [(mapv (fn [draw-item]
           (let [component (:path/material draw-item)]
             [(:path/material-id component)
              (:path/revision component)
              (:container draw-item)]))
         (or draw-items []))
   lod])
