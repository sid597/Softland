(ns app.client.image.frame
  "The pure image-to-floor request for one frame.
   Takes: image ops plus the residency revision.
   Gives: the material revision/container frame key.
   Holds nothing.")

(defn frame-key [ops residency-rev]
  [(mapv (fn [op]
           (let [material (:image/material op)]
             [(:image/material-id material)
              (:image/revision material)
              (:container op)]))
         (or ops []))
   residency-rev])
