(ns app.client.engine.surface-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.engine.surface :as s]
            [app.client.engine.value-bytes :as vb])
  (:import [java.io ByteArrayInputStream] [javax.imageio ImageIO]))

(def declaration {:surface/id "test" :width 2 :height 2 :domain {:kind :plane :map [1 0 0 1 0 0]}
                  :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]})
(defn painted [surface rgba key]
  (s/paint surface nil {:kind :color :rgba rgba :opacity 0.5 :blend :source-over}
           {:coverage (fn [_ _] 1) :key key}))

(deftest branches-are-independent-values-and-clips-multiply
  (let [base (s/new declaration) red (painted base [1 0 0 1] "red") blue (painted base [0 0 1 1] "blue")
        read #(select-keys (s/sample [%] [0.5 0.5] :nearest {}) [:color :covered?])]
    (is (= {:color [0.5 0.0 0.0 0.5] :covered? true} (read red)))
    (is (= {:color [0.0 0.0 0.5 0.5] :covered? true} (read blue)))
    (is (= [0.0 0.0 0.0 0.0] (:color (read base))))
    (is (= [0.25 0.0 0.5 0.75] (:color (s/sample [red blue] [0.5 0.5] :nearest {}))))
    (is (= [] (:contributors (s/sample [red] [2 0] :nearest {}))))
    (is (= "test@initial" (:parent red)))
    (is (= 4 (:changed red)))
    (is (vb/equal? red (s/decode (s/encode red)))))
  (let [r (s/paint (s/new declaration) nil {:kind :color :rgba [1 0 0 1] :opacity 0.5}
                   {:key "clip" :coverage (fn [_ _] 0.5) :clips [(fn [_ _] 0.5)]})]
    (is (= [0.125 0.0 0.0 0.125] (:color (s/sample [r] [0.5 0.5] :nearest {}))))))

(deftest byte-length-domain-and-vocabulary-are-checked
  (let [surface (s/new declaration) encoded (vb/data surface)]
    (doseq [bad [(assoc-in encoded [:data :length] 12)
                 (assoc encoded :width 1)
                 (assoc-in encoded [:domain :map] [0 0 0 0 0 0])]]
      (is (thrown? Exception (s/decode (vb/encode bad)))))
    (is (thrown? Exception (vb/encode {:function identity}))))
  (is (thrown? Exception (s/paint (s/new declaration) nil {:kind :color :rgba [1 0 0 1]} {:coverage (fn [_ _] 1)})))
  (is (= :unsupported-blend (try (s/paint (s/new declaration) nil {:kind :color :rgba [1 0 0 1] :blend :multiply}
                                        {:key "x" :coverage (fn [_ _] 1)})
                               (catch Exception e (:reason (ex-data e)))))))

(deftest png-is-a-readable-picture
  (let [surface (painted (s/new declaration) [1 0 0 1] "red")
        bytes (s/png-bytes surface) image (ImageIO/read (ByteArrayInputStream. bytes))]
    (is (= 2 (.getWidth image)))
    (is (= 0x80ff0000 (bit-and 0xffffffff (.getRGB image 0 0))))
    (is (= (seq bytes) (seq (s/png-bytes surface))))))
