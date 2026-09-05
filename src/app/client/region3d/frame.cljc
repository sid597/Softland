(ns app.client.region3d.frame
  "Gate entry to region preparation.

   Input: region draw rows and engine/session stamps. Output: pure keys. No
   retained state.

   Folder map: README.md.")

(defn region-key
  "Draw item, zoom, DPR, session revision →
   ID/revision/container/zoom/DPR/session tuple.

   Outer invalidation contract. Session contents and resolved placement
   contents must change the supplied revision to get past this key."
  [draw-item zoom dpr session-revision]
  (let [region (:region/material draw-item)]
    [(:region/id region) (:region/revision region) (:container draw-item)
     zoom dpr session-revision]))

(defn frame-key
  "Ordered region draw items, zoom, DPR and session revision → ordered
   region keys.

   Mapv over the one-region key."
  [draw-items zoom dpr session-revision]
  (mapv #(region-key % zoom dpr session-revision) draw-items))
