(ns app.client.harness.coating
  "The definer's 3D brush through the client executor and CPU compositor.
   Gives full-value continuation comparisons, RGBA32F hash, four carries,
   and a PNG whose decoded pixels are read. Holds only temporary run values.
   JVM twin: region3d/brush_test.clj; verifier golden: region3d-coating.json."
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.surface :as surface]
            [app.client.engine.value-bytes :as vb]
            [app.client.region3d.records :as records]
            [app.client.region3d.capabilities :as capabilities]
            [app.client.harness.shared :as shared]))

(defn- pixels!
  "Decode the exported PNG and read every pixel through the browser canvas."
  [url]
  (js/Promise.
   (fn [resolve reject]
     (let [img (js/Image.)]
       (set! (.-onerror img) reject)
       (set! (.-onload img)
             (fn []
               (try
                 (let [canvas (js/document.createElement "canvas")
                       w (.-naturalWidth img) h (.-naturalHeight img)]
                   (set! (.-width canvas) w)
                   (set! (.-height canvas) h)
                   (let [ctx (.getContext canvas "2d")]
                     (.drawImage ctx img 0 0)
                     (let [data (.-data (.getImageData ctx 0 0 w h))
                           painted (count (filter #(pos? (aget data %)) (range 3 (alength data) 4)))]
                       (set! (.-width canvas) (* 8 w))
                       (set! (.-height canvas) (* 8 h))
                       (set! (.-imageSmoothingEnabled ctx) false)
                       (.drawImage ctx img 0 0 (* 8 w) (* 8 h))
                       (resolve {:image-size [w h]
                                 :painted-pixels painted
                                 :preview {:file "cpu-region3d-coating-8x.png"
                                           :png-data-url (.toDataURL canvas "image/png")}}))))
                 (catch :default error (reject error)))))
       (set! (.-src img) url)))))

(defn run-check!
  "Run eager, withheld/resumed and encoded/resumed brush → browser receipt.
   Hashes are evidence only; continuation equality compares complete values."
  []
  (let [t0 (js/performance.now)
        record (records/program-record records/pickup)
        eager (executor/run record {:host records/host} capabilities/table {:budget 1})
        held (executor/run record {:host records/host} capabilities/table {:budget 0})
        c (:continuation held) bytes (executor/encode c)
        delayed (executor/resume c capabilities/table {:budget 1})
        restored (executor/resume (executor/decode bytes capabilities/table) capabilities/table {:budget 1})
        checks (mapv (fn [other]
                       (into {} (map (fn [k] [k (vb/equal? (k eager) (k other))]) [:state :history :results :subjects])))
                     [delayed restored])
        painting (get-in eager [:results :painting])
        url (str "data:image/png;base64," (vb/base64 (surface/png-bytes painting)))
        cpu-ms (- (js/performance.now) t0)]
    (.then (js/Promise.all #js [(shared/sha256-bytes (vb/float-bytes (:data painting))) (pixels! url)])
           (fn [out]
             (merge (aget out 1)
                    {:sha256 (aget out 0)
                     :colors (mapv #(get-in % [:steps 1 :color]) (:history eager))
                     :carries (mapv #(get-in % [:state-after :carry]) (:history eager))
                     :changed (mapv #(get-in % [:steps 4 :changed]) (:history eager))
                     :key (:key painting) :revision (:revision painting) :components (alength (:data painting))
                     :checkpoint {:at (:at c) :bytes (alength bytes) :committed (count (:history c))
                                  :delayed (first checks) :encoded (second checks)}
                     :image {:file "cpu-region3d-coating.png" :png-data-url url}
                     :cpu-ms cpu-ms
                     :pass? (and (= :complete (:status eager) (:status delayed) (:status restored))
                                 (= [:suspended :pending 0] ((juxt :status :reason :at) held))
                                 (= [] (:history c)) (= [1 0 0 1] (get-in c [:state :carry]))
                                 (every? true? (mapcat vals checks))
                                 (= [64 32] (:image-size (aget out 1)))
                                 (= 847 (:painted-pixels (aget out 1))))})))))
