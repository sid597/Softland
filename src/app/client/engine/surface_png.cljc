(ns app.client.engine.surface-png
  "Deterministic PNG transport for the CPU compositor's picture.
   Takes dimensions and straight RGBA8 channels; gives PNG bytes, with
   stored DEFLATE blocks. Holds nothing; no canvas, device or I/O. Evidence:
   surface_test/png-is-a-readable-picture and browser pickup capture."
  (:require [app.client.engine.value-bytes :as vb]))

(defn- u32 [x] (mapv #(bit-and 255 (unsigned-bit-shift-right x %)) [24 16 8 0]))
(defn- crc [xs]
  (bit-xor -1 (reduce (fn [c x]
                       (loop [c (bit-xor c x) n 8]
                         (if (zero? n) c
                             (recur (bit-xor (unsigned-bit-shift-right c 1)
                                             (if (odd? c) 0xedb88320 0)) (dec n)))))
                     0xffffffff xs)))
(defn- png-chunk [tag payload]
  (let [body (into (mapv #?(:clj int :cljs #(.charCodeAt % 0)) tag) payload)]
    (into (into (u32 (count payload)) body) (u32 (crc body)))))
(defn- zlib [xs]
  (let [blocks (vec (partition-all 65535 xs))
        [a b] (reduce (fn [[a b] x] (let [a (mod (+ a x) 65521)] [a (mod (+ b a) 65521)])) [1 0] xs)]
    (vec (concat [0x78 0x01]
                 (mapcat (fn [i block] (let [n (count block) inv (bit-xor n 65535)]
                                        (concat [(if (= i (dec (count blocks))) 1 0)
                                                 (bit-and n 255) (bit-shift-right n 8)
                                                 (bit-and inv 255) (bit-shift-right inv 8)] block)))
                         (range) blocks)
                 (u32 (+ (* b 65536) a))))))
(defn encode [width height rgba]
  (let [scanlines (vec (mapcat #(cons 0 %) (partition (* width 4) rgba)))]
    (vb/bytes (vec (concat [137 80 78 71 13 10 26 10]
                           (png-chunk "IHDR" (into (into (u32 width) (u32 height)) [8 6 0 0 0]))
                           (png-chunk "IDAT" (zlib scanlines)) (png-chunk "IEND" []))))))
