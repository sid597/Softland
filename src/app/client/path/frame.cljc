(ns app.client.path.frame
  "Decide when the packed path frame is stale.

   Input: ordered draw items and LOD. Output: a pure frame key. No retained
   state.

   Folder map: README.md.")

(defn frame-key
  "Draw items and LOD → ordered [material-id revision container] rows plus
   LOD.

   Small dependency key. Trusts revision to change for geometry/paint edits;
   excludes compact transform index changes."
  [draw-items lod]
  [(mapv (fn [draw-item]
           (let [component (:path/material draw-item)]
             [(:path/material-id component)
              (:path/revision component)
              (:container draw-item)]))
         (or draw-items []))
   lod])
