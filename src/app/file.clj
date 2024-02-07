(ns app.file
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))


(defonce file-location "/Users/sid597/Documents/Softland-files/softland.edn")

;; --------- Serialize and save event to file ------------
(defn save-event [function-name args]
  (let [file (io/file file-location)
        event {:function-name function-name
               :args          args}]
    (with-open [writer (io/writer file :append true)]
      (spit writer (prn-str event)))))

;; --------- De-serialize and save event to file ------------

(defn deserialize-and-execute [event]
    (let [function-symbol   (symbol (get event :function-name))
          args              (get event :args)
          resolved-function (resolve function-symbol)]
      (apply resolved-function args)))


(defn load-events []
  (let [events (atom nil)]
   (with-open [r (io/reader file-location)]
     (doall (reset! events (map edn/read-string (line-seq r)))))
   (doseq [event @events]
     (deserialize-and-execute event))
   true))

(comment (load-events))

;; ---------------- TEST --------------------

(comment
 (defn test [] (prn-str {:function-name "rama/add-new-node"
                         :args          [{:x {:id :x
                                              :x 100
                                              :y 300
                                              :type-specific-data {:text "GM Hello"
                                                                   :width 400
                                                                   :height 800}
                                              :type "rect"
                                              :fill  "lightblue"}}
                                         {:graph-name :main}]}))

 (test)
 (edn/read-string (test)))

