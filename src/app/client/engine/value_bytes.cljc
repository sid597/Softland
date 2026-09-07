(ns app.client.engine.value-bytes
  "Data values and float32 arrays as portable EDN bytes.
   Takes values; gives UTF-8 EDN with explicit little-endian float payloads.
  Holds nothing. Functions and opaque objects are refused. Evidence:
   engine/surface_test.clj and engine/executor_test.clj."
  (:refer-clojure :exclude [bytes floats])
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])
            #?(:cljs [goog.crypt.base64 :as base64]))
  #?(:clj (:import [java.nio ByteBuffer ByteOrder]
                   [java.nio.charset StandardCharsets]
                   [java.util Base64])))

(defn floats? [x]
  #?(:clj (instance? (Class/forName "[F") x) :cljs (instance? js/Float32Array x)))

(defn floats [n] #?(:clj (float-array n) :cljs (js/Float32Array. n)))
(defn bytes [xs] #?(:clj (byte-array (map unchecked-byte xs)) :cljs (js/Uint8Array. (clj->js xs))))
(defn utf8 [s] #?(:clj (.getBytes ^String s StandardCharsets/UTF_8) :cljs (.encode (js/TextEncoder.) s)))
(defn text [bs] #?(:clj (String. ^bytes bs StandardCharsets/UTF_8) :cljs (.decode (js/TextDecoder. "utf-8" #js {:fatal true}) bs)))
(defn base64 [bs] #?(:clj (.encodeToString (Base64/getEncoder) bs) :cljs (base64/encodeByteArray bs)))
(defn unbase64 [s] #?(:clj (.decode (Base64/getDecoder) ^String s) :cljs (base64/decodeStringToUint8Array s)))

(defn float-bytes
  "Float32 array → little-endian bytes, independent of machine byte order."
  [xs]
  #?(:clj (let [b (doto (ByteBuffer/allocate (* 4 (alength ^floats xs))) (.order ByteOrder/LITTLE_ENDIAN))]
            (dotimes [i (alength ^floats xs)] (.putFloat b (aget ^floats xs i))) (.array b))
     :cljs (let [bs (js/Uint8Array. (* 4 (.-length xs))) v (js/DataView. (.-buffer bs))]
             (dotimes [i (.-length xs)] (.setFloat32 v (* 4 i) (aget xs i) true)) bs)))

(defn- restore-floats [{:keys [float32le length]}]
  (when-not (and (string? float32le) (integer? length) (<= 0 length))
    (throw (ex-info "Invalid float32 declaration" {:reason :array-length})))
  (let [bs (unbase64 float32le)]
    (when-not (= (* 4 length) (alength bs))
      (throw (ex-info "Float32 payload length differs" {:reason :array-length})))
    (let [xs (floats length)
          v #?(:clj (doto (ByteBuffer/wrap bs) (.order ByteOrder/LITTLE_ENDIAN))
               :cljs (js/DataView. (.-buffer bs) (.-byteOffset bs) (.-byteLength bs)))]
      (dotimes [i length]
        (aset xs i #?(:clj (.getFloat v) :cljs (.getFloat32 v (* 4 i) true)))) xs)))

(defn data
  "Value → EDN data with arrays encoded. Also the full-value equality form;
   array identity and hashes never decide whether two inputs agree."
  [x]
  (cond
    (floats? x) {:float32le (base64 (float-bytes x)) :length (alength x)}
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) (map (fn [[k v]] [(data k) (data v)])) x)
    (vector? x) (mapv data x)
    (set? x) (into #{} (map data) x)
    (sequential? x) (mapv data x)
    (or (nil? x) (string? x) (keyword? x) (boolean? x)
        (and (number? x) #?(:clj (Double/isFinite (double x)) :cljs (js/Number.isFinite x)))) x
    :else (throw (ex-info "Only data may cross the byte boundary" {:reason :not-data}))))

(defn restore [x]
  (cond
    (and (map? x) (contains? x :float32le)) (restore-floats x)
    (map? x) (into {} (map (fn [[k v]] [(restore k) (restore v)])) x)
    (vector? x) (mapv restore x)
    (set? x) (into #{} (map restore) x)
    :else x))

(defn equal? [a b] (= (data a) (data b)))
(defn encode [value] (utf8 (pr-str (data value))))
(defn decode [bs] (restore (edn/read-string (text bs))))
