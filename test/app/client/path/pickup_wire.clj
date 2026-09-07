(ns app.client.path.pickup-wire
  "Cold JVM receipt for the pickup continuation. Each command is a new
   process: clj -M:test -m app.client.path.pickup-wire save|resume <directory>."
  (:require [clojure.java.io :as io]
            [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as construction]
            [app.client.path.pickup-test :as pickup]
            [app.client.path.records :as records])
  (:import [java.nio.file Files] [java.security MessageDigest]))

(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and % 255)) (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn -main [mode directory]
  (let [wire (io/file directory "pickup-continuation.edn") expected (io/file directory "pickup-straight.edn")]
    (case mode
      "save" (let [straight (pickup/run records/pickup {})
                   c (:continuation (pickup/run records/pickup {:until 12}))]
               (.mkdirs (io/file directory))
               (with-open [out (io/output-stream wire)] (.write out (executor/encode c)))
               (with-open [out (io/output-stream expected)] (.write out (vb/encode (select-keys straight [:results :state :history :subjects]))))
               (prn {:saved true :bytes (.length wire)
                     :sha256 (sha (vb/float-bytes (get-in straight [:results :surface :data])))}))
      "resume" (let [c (executor/decode (Files/readAllBytes (.toPath wire)) construction/capabilities)
                     r (executor/resume c construction/capabilities {})
                     prior (vb/decode (Files/readAllBytes (.toPath expected)))
                     equal? (vb/equal? prior (select-keys r [:results :state :history :subjects]))]
                 (prn {:status (:status r) :cold-equal? equal?
                       :components (alength (get-in r [:results :surface :data]))
                       :sha256 (sha (vb/float-bytes (get-in r [:results :surface :data])))})
                 (when-not equal? (System/exit 1))))))
