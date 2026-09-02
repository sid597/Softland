(ns app.client.image.frame
  "The pure image-to-engine request for one frame.
   Takes: image draw-items plus the residency revision.
   Gives: the component revision/group frame key.
   Holds nothing.")

(defn frame-key [draw-items residency-rev]
  [(mapv (fn [draw-item]
           (let [component (:image/component draw-item)]
             [(:image/component-id component)
              (:image/revision component)
              (:container draw-item)]))
         (or draw-items []))
   residency-rev])
