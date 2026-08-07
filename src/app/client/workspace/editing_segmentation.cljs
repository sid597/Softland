(ns app.client.workspace.editing-segmentation
  "Browser segmentation provider for T2. Construction is explicit at flag
   boot; namespace load performs no host reads or side effects."
  (:require [app.client.workspace.text-layout :as tl]))

(defn supported? []
  (and (exists? js/Intl)
       (some? (.-Segmenter js/Intl))))

(defn provider-identity
  [provider]
  (select-keys provider [:api :locale :version :granularities]))

(defn create-provider
  ([] (create-provider "und"))
  ([locale]
   (when-not (supported?)
     (throw (ex-info "T2 requires Intl.Segmenter in the browser lane"
                     {:provider :intl-segmenter :locale locale})))
   {:api :intl-segmenter
    :locale locale
    :version 1
    :granularities [:grapheme :word]
    :grapheme (js/Intl.Segmenter. locale #js {:granularity "grapheme"})
    :word (js/Intl.Segmenter. locale #js {:granularity "word"})}))

(defn- segment-rows
  [^js segmenter text]
  (array-seq (js/Array.from (.segment segmenter text))))

(defn boundaries
  "Return deterministic UTF-16 boundary DATA for the pure kernel. Word spans
   include only Intl's word-like segments; the boundary vector also carries
   punctuation/space segment edges so Ctrl+Arrow always makes progress."
  [provider text]
  (let [text (str (or text ""))
        length (tl/code-unit-count text)
        grapheme-rows (segment-rows (:grapheme provider) text)
        word-rows (segment-rows (:word provider) text)
        row-end (fn [^js row]
                  (+ (.-index row)
                     (tl/code-unit-count (.-segment row))))]
    {:grapheme (->> grapheme-rows
                    (mapcat (fn [^js row] [(.-index row) (row-end row)]))
                    (concat [0 length]) distinct sort vec)
     :word (->> word-rows
                (mapcat (fn [^js row] [(.-index row) (row-end row)]))
                (concat [0 length]) distinct sort vec)
     :word-spans (->> word-rows
                      (filter (fn [^js row]
                                (true? (.-isWordLike row))))
                      (mapv (fn [^js row]
                              [(.-index row) (row-end row)])))}))
