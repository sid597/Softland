(ns app.client.region3d.frame
  "The Region3D frame gate: row identity plus the floor stamps that can change
   its projection.
   Takes: region ops, zoom, DPR, and the session revision.
   Gives: one key per region or the ordered frame key.
   Holds nothing.")

(defn region-key [op zoom dpr session-revision]
  (let [region (:region/material op)]
    [(:region/id region) (:region/revision region) (:container op)
     zoom dpr session-revision]))

(defn frame-key [ops zoom dpr session-revision]
  (mapv #(region-key % zoom dpr session-revision) ops))
