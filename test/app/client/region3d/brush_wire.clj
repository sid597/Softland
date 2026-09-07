(ns app.client.region3d.brush-wire
  "Fresh-process receipt: save the withheld brush; resume from bytes and a
   capability table. clj -M:test -m app.client.region3d.brush-wire save|resume DIR.
   Writes full expected values, a numeric golden and decoded PNG pixel counts."
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]
            [app.client.engine.surface :as surface]
            [app.client.region3d.records :as records]
            [app.client.region3d.capabilities :as capabilities])
  (:import [java.nio.file Files] [java.security MessageDigest]
           [java.io ByteArrayInputStream] [javax.imageio ImageIO]))

(defn- values [result] (select-keys result [:results :state :history :subjects]))
(defn- write-bytes! [file bytes] (with-open [out (io/output-stream file)] (.write out bytes)))
(defn- read-bytes [file] (Files/readAllBytes (.toPath file)))
(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %)) (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn golden [r]
  (let [painting (get-in r [:results :painting])
        image (ImageIO/read (ByteArrayInputStream. (surface/png-bytes painting)))]
    {:sha256 (sha (vb/float-bytes (:data painting)))
     :colors (mapv #(get-in % [:steps 1 :color]) (:history r))
     :carries (mapv #(get-in % [:state-after :carry]) (:history r))
     :changed (mapv #(get-in % [:steps 4 :changed]) (:history r))
     :key (:key painting) :revision (:revision painting) :components (alength (:data painting))
     :imageSize [(.getWidth image) (.getHeight image)]
     :paintedPixels (count (for [y (range (.getHeight image)) x (range (.getWidth image))
                                :when (pos? (bit-and 255 (unsigned-bit-shift-right (.getRGB image x y) 24)))] 1))}))

(defn -main [mode directory]
  (let [wire (io/file directory "coating-continuation.edn")
        expected (io/file directory "coating-eager.edn")]
    (case mode
      "save"
      (let [record (records/program-record records/pickup)
            eager (executor/run record {:host records/host} capabilities/table {:budget 1})
            held (executor/run record {:host records/host} capabilities/table {:budget 0})
            c (:continuation held) g (golden eager)]
        (assert (= [:suspended :pending 0] ((juxt :status :reason :at) held)))
        (assert (= :complete (:status eager)))
        (.mkdirs (io/file directory))
        (write-bytes! wire (executor/encode c))
        (write-bytes! expected (vb/encode (values eager)))
        (write-bytes! (io/file directory "cpu-region3d-coating.png") (surface/png-bytes (get-in eager [:results :painting])))
        (spit (io/file directory "region3d-coating.json") (str (json/write-str g) "\n"))
        (prn {:saved true :at (:at c) :committed (count (:history c)) :bytes (.length wire) :golden g}))
      "resume"
      (let [c (executor/decode (read-bytes wire) capabilities/table)
            resumed (executor/resume c capabilities/table {:budget 1})
            prior (vb/decode (read-bytes expected))
            matches (into {} (map (fn [k] [k (vb/equal? (k prior) (k resumed))]) (keys prior)))
            g (golden resumed)]
        (prn {:status (:status resumed) :cold-equal matches :golden g})
        (when-not (and (= :complete (:status resumed)) (every? true? (vals matches))) (System/exit 1))))))
