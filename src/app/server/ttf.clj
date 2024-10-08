(ns app.server.ttf
  (:import [javax.imageio ImageIO]
           [java.io File]
           [java.awt.image BufferedImage])
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(def atlas-data (json/read-str
                  (slurp "/Users/sid597/Softland/resources/public/font_atlas.json")
                  :key-fn keyword))

(:metrics atlas-data)
(:atlas atlas-data)
(first (:glyphs atlas-data))
(println
  (get (reduce 
        (fn [acc glyph]
         (assoc acc (:unicode glyph)
                    glyph))
        {}
        (:glyphs atlas-data)) 
       33))
         

(defn shape-text [msdf-atlas text fsize]
  (let [atlas          (:atlas msdf-atlas)
        atlas-width    (:width atlas)
        atlas-height   (:height atlas)
        metrics        (:metrics msdf-atlas)
        line-height    (:lineHeight metrics)
        glyphs         (reduce (fn [acc glyph]
                                 (assoc acc (:unicode glyph)
                                            glyph))
                         {}
                         (:glyphs msdf-atlas))
        font-size      (* (/ 1 (:size atlas-data)) fsize)]
    (println "--" (take 2 glyphs) "=============" (get glyphs 72))

    (loop [chars (seq text)
           x     0
           y     0
           acc   []]
      (if (empty? chars)
        acc
        (let [ch        (first chars)
              codepoint (int ch)]
          (if (= ch \newline)
            ;; Handle newlines by resetting x and adjusting y
            (recur (rest chars)
              0
              (- y (* font-size line-height))
              acc)
            (let [glyph (get glyphs codepoint)]
              (println 'glyph glyph)
              (if glyph
                (let [advance        (* font-size (:advance glyph))
                      plane-bounds   (:planeBounds glyph)
                      atlas-bounds   (:atlasBounds glyph)
                      _ (println "advance" advance plane-bounds atlas-bounds)
                      ;; Scale plane bounds by font size
                      pl             (+ x (* font-size (get plane-bounds :left)))
                      pb             (+ y (* font-size (get plane-bounds :bottom)))
                      pr             (+ x (* font-size (get plane-bounds :right)))
                      pt             (+ y (* font-size (get plane-bounds :top)))
                      positions      [[pl pb] [pr pb] [pr pt] [pl pt]]
                      _ (println "--" positions)
                      ;; Calculate texture coordinates
                      al             (/ (get atlas-bounds :left) atlas-width)
                      ab             (/ (get atlas-bounds :bottom) atlas-height)
                      ar             (/ (get atlas-bounds :right) atlas-width)
                      at             (/ (get atlas-bounds :top) atlas-height)
                      uvs            [[al ab] [ar ab] [ar at] [al at]]]
                  (recur (rest chars)
                    (+ x advance)
                    y
                    (conj acc {:codepoint codepoint
                               :positions positions
                               :uvs uvs})))
                ;; Skip character if glyph not found
                (recur (rest chars)
                  x
                  y
                  acc)))))))))

(pprint  (shape-text atlas-data "Hello,World!" 48))

(defn png-to-bitmap
  "Reads a PNG file and returns a map containing the bitmap data and dimensions"
  [file-path]
  (let [^BufferedImage img (ImageIO/read (File. file-path))
        width (.getWidth img)
        height (.getHeight img)
        bitmap (int-array (* width height))]
    (.getRGB img 0 0 width height bitmap 0 width)
    {:width width
     :height height
     :bitmap bitmap}))

(png-to-bitmap "/Users/sid597/Softland/resources/public/font_atlas.png")

{:codepoint 33,
 :positions
 [[326.23941176470595 -0.46875]
  [336.73941176470595 -0.46875]
  [336.73941176470595 35.34375]
  [326.23941176470595 35.34375]],
 :uvs
 [[0.71466064453125 0.66107177734375]
  [0.72149658203125 0.66107177734375]
  [0.72149658203125 0.68438720703125]
  [0.71466064453125 0.68438720703125]]}
