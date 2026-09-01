(ns app.client.path.frame
  "The pure path-to-floor request for one frame.
   Takes: path ops plus a zoom regime, or an effective container table and id.
   Gives: the revision/container frame key, or the compact transport slot.
   Holds nothing.")

(defn frame-key [ops regime]
  [(mapv (fn [op]
           (let [material (:path/material op)]
             [(:path/material-id material)
              (:path/revision material)
              (:container op)]))
         (or ops []))
   regime])

(defn slot [effective container]
  (let [transport-slot (get-in effective [container :transport-slot] ::missing)]
    (when (= ::missing transport-slot)
      (throw (ex-info "Path op names an unknown container"
                      {:error-type :path/unknown-container
                       :container container})))
    (int transport-slot)))
