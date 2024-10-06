(ns app.server.ttf
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(def atlas-data (json/read-str
                  (slurp "/Users/sid597/Softland/resources/public/font_atlas.json")
                  :key-fn keyword))

(defn shape-text [msdf-atlas text font-size]
  (let [atlas          (:atlas msdf-atlas)
        atlas-width    (:width atlas)
        atlas-height   (:height atlas)
        metrics        (:metrics msdf-atlas)
        line-height    (:lineHeight metrics)
        glyphs        (reduce (fn [acc glyph]
                                (assoc acc (:unicode glyph)
                                           glyph))
                        {}
                        (:glyphs msdf-atlas))]
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


#_{"unicode":33,
   "advance":0.59999999999999998,
   "planeBounds":{"left":0.19665441176470588,
                  "bottom":-0.009765625,
                  "right":0.41540441176470588,
                  "top":0.736328125},
   "atlasBounds":{"left":5854.5,
                  "bottom":5415.5,
                  "right":5910.5,
                  "top":5606.5}}

