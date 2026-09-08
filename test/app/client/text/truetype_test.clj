(ns app.client.text.truetype-test
  "The repository's three TrueType fonts read as path values: long and short
   glyph offsets, a monospace font with four horizontal metrics, simple and
   composite glyphs, the character map."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [app.client.path.value :as value]
            [app.client.text.truetype :as tt]))

(defn font [name] (tt/parse (.readAllBytes (io/input-stream (io/file "resources/public/fonts" name)))))

(deftest metrics-and-the-character-map
  (let [noto (font "noto_sans_regular.ttf") dejavu (font "dejavu_sans_mono.ttf") ubuntu (font "ubuntu_sans_variable.ttf")]
    (is (= {:upem 1000 :ascender 1069 :descender -293 :line-gap 0 :glyph-count 3317} (tt/metrics noto)))
    (is (= {:upem 2048 :ascender 1901 :descender -483 :line-gap 0 :glyph-count 3377} (tt/metrics dejavu)))
    (is (= [1 0] [(:loca-format noto) (:loca-format ubuntu)]) "long and short glyph offsets")
    (is (pos? (tt/glyph-index noto "A")))
    (is (= 0 (tt/glyph-index noto "ก")) "Thai is not in Noto Sans Latin")
    (is (not= (tt/glyph-index noto "a") (tt/glyph-index noto "b")))
    (is (= 8 (count (tt/digest (.getBytes "softland" "UTF-8")))))
    (is (= (tt/digest (:bytes noto)) (tt/digest (:bytes noto))))))

(deftest advances-simple-and-composite-outlines
  (let [noto (font "noto_sans_regular.ttf") dejavu (font "dejavu_sans_mono.ttf")]
    (testing "a proportional font: 'i' is narrower than 'm'"
      (is (< (:advance (tt/glyph-for-char noto "i")) (:advance (tt/glyph-for-char noto "m")))))
    (testing "a monospace font with four horizontal metrics: every letter shares the last advance"
      (is (= (:advance (tt/glyph-for-char dejavu "i")) (:advance (tt/glyph-for-char dejavu "m")) 1233)))
    (testing "a simple glyph is closed quadratic subpaths that validate as a path value"
      (let [{:keys [outline bbox contours]} (tt/glyph-for-char noto "o")]
        (is (= 2 contours) "the o has an outer and an inner contour")
        (is (= 2 (count (:subpaths outline))))
        (is (every? :closed? (:subpaths outline)))
        (is (every? #(#{:line :quad} (:kind %)) (mapcat :segments (:subpaths outline))))
        (is (= outline (value/validate! outline)))
        (is (< (nth bbox 0) (nth bbox 2)))
        (is (< (nth bbox 1) (nth bbox 3)))
        (is (every? (fn [[x y]] (and (<= (nth bbox 0) x (nth bbox 2)) (<= (nth bbox 1) y (nth bbox 3))))
                    (for [sp (:subpaths outline) s (:segments sp)] (:p s)))
            "every on-curve point lies within the glyph's bounding box")))
    (testing "a composite glyph resolves its components"
      (let [e (tt/glyph-for-char noto "e") acute (tt/glyph-for-char noto "é")]
        (is (= (:contours acute) (+ (:contours e) 1)) "é is the e's contours and the acute's")
        (is (= (:outline e) (value/validate! (:outline e))))
        (is (= (:outline acute) (value/validate! (:outline acute))))))
    (testing "a space inks nothing"
      (is (= {:contours 0 :bbox [0 0 0 0]} (select-keys (tt/glyph-for-char noto " ") [:contours :bbox])))
      (is (pos? (:advance (tt/glyph-for-char noto " ")))))
    (testing "the missing glyph stands for an unmapped character"
      (is (= (tt/glyph noto 0) (tt/glyph-for-char noto "ก"))))))
