
(ns app.server.ttf
  (:import [org.apache.fontbox.ttf TTFParser]
           [org.apache.pdfbox.io RandomAccessReadBufferedFile]
           [java.io File])
  (:gen-class))

(defn parse-ttf [file-path]
  (println "Attempting to parse file:" file-path)
  (try
    (let [parser (TTFParser.)
          _ (println "Created TTFParser instance")
          raf (RandomAccessReadBufferedFile. file-path)
          _ (println "Created RandomAccessReadBufferedFile instance")
          font (.parse parser raf)
          _ (println "Successfully parsed font file")
          head-table (.getHeader font)
          maxp-table (.getMaximumProfile font)
          hhea-table (.getHorizontalHeader font)
          cmap-table (.getCmap font)
          glyf-table (.getGlyph font)]
      {:name (.getName font)
       :font-version (.getVersion font)
       :num-glyphs (.getNumberOfGlyphs font)
       :units-per-em (.getUnitsPerEm head-table)
       :bbox (.getFontBBox font)
       :cmap (str cmap-table) ; You might need to extract specific details here.
       :glyph-table (str glyf-table)}) ; Placeholder for glyph data extraction.
       
    (catch Exception e
      (println "Error parsing font file:" (.getMessage e))
      (.printStackTrace e)
      nil)))

(defn -main [& args]
  (println "Starting main function")
  (if (empty? args)
    (println "Please provide a path to a TTF file as an argument.")
    (let [file-path (first args)
          _ (println "Received file path:" file-path)
          font-data (parse-ttf file-path)]
      (if font-data
        (do
          (println "Font Data:")
          (doseq [[k v] font-data]
           (println (str (name k) ": " v))))
        (println "Failed to parse font data")))))

(println "Script loaded. Calling -main function:")
(apply -main *command-line-args*)

