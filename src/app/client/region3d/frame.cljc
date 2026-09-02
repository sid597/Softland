(ns app.client.region3d.frame
  "The Region3D frame dirty check: row identity plus the engine stamps that can change
   its projection.
   Takes: region draw-items, zoom, DPR, and the session revision.
   Gives: one key per region or the ordered frame key.
   Holds nothing.")

(defn region-key [draw-item zoom dpr session-revision]
  (let [region (:region/material draw-item)]
    [(:region/id region) (:region/revision region) (:container draw-item)
     zoom dpr session-revision]))

(defn frame-key [draw-items zoom dpr session-revision]
  (mapv #(region-key % zoom dpr session-revision) draw-items))
