(ns app.client.path.frame
  "The pure path-to-floor request for one frame.
   Takes: path ops plus a zoom regime.
   Gives: the revision/container frame key.
   Holds nothing.")

(defn frame-key [ops regime]
  [(mapv (fn [op]
           (let [material (:path/material op)]
             [(:path/material-id material)
              (:path/revision material)
              (:container op)]))
         (or ops []))
   regime])
