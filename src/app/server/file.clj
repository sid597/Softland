(ns app.server.file
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(defonce softland-edn "/Users/sid597/Documents/Softland-files/softland.edn")
(defonce dg-nodes-file-edn "/Users/sid597/Documents/Softland-files/dg-nodes.edn")
(defonce dg-edges-file-edn-edn "/Users/sid597/Documents/Softland-files/dg-edges.edn")
(defonce dg-page-data-edn "/Users/sid597/Downloads/dg-pages-all-data.edn")
(defonce dg-nodes-edn "/Users/sid597/Downloads/dg-nodes-matsulab.edn")
(defonce dg-edges-edn "/Users/sid597/Downloads/dg-edges-matsulab.edn")

;; --------- Serialize and save event to file ------------
(defn save-event [function-name args file-name]
  (let [file (io/file file-name)
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


(defn load-events [file-location sfn]
  (let [events (atom nil)
        read-fn (fn [x]
                    (let [ppp (-> x
                                (str/replace  #"::" ":")
                                (str/replace
                                  #"(\{|,)\s*:https://(\S+?)(\s|,|\})"
                                  (fn [[_ prefix url suffix]]
                                    (str prefix " \"https://" url "\"" suffix)))

                                #_(str/replace #":https://(\S+)"  ; New step to handle URL keywords
                                    (fn [[_ url]]
                                      (-> (str "\"https://" url "\"")
                                        (str/replace  #"(https://[^\"]+)"
                                          (fn [[_ url]]
                                            (-> url
                                              (str/replace "=" "%3D")
                                              (str/replace "?" "%3F")
                                              (str/replace "&" "%26")
                                              (str/replace "," "%2C")
                                              (str/replace "-" "%2D"))))))))]

                      (edn/read-string ppp)))]
    (with-open [r (io/reader file-location)]
      (doall (reset! events (map read-fn (line-seq r)))))
    (doseq [[index event] (map-indexed vector (take 400 @events))]
      (println "Processing event #" (inc index))
      #_(println "event:::: " event)
      (sfn event))
    #_(doseq [event @events]
        (println "event:::: " event)
        (sfn event))
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

