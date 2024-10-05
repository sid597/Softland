

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
          vhea-table (.getVerticalHeader font)
          hmtx-table (.getHorizontalMetrics font)
          vmtx-table (.getVerticalMetrics font)
          cmap-table (.getCmap font)
          loca-table (.getIndexToLocation font)
          glyf-table (.getGlyph font)
          gpos-table (.getGsub font)

          ;; Extract the number of glyphs
          num-glyphs (.getNumGlyphs maxp-table)

          glyphs (mapv (fn [gid] 
                         {:gid gid 
                          :glyph-data (.getGlyph glyf-table gid)})
                      (range num-glyphs))

          horizontal-metrics (mapv (fn [gid]
                                    {:gid gid
                                     :advance-width (.getAdvanceWidth hmtx-table gid)
                                     :left-side-bearing (.getLeftSideBearing hmtx-table gid)})
                                  (range num-glyphs))
          vertical-metrics (when (some? vmtx-table) 
                             (mapv (fn [gid]
                                      {:gid gid
                                       :advance-height (.getAdvanceHeight vmtx-table gid)
                                       :left-side-bearing (.getLeftSideBearing vmtx-table gid)})
                                  (range num-glyphs)))] 

      {:name (.getName font)
       :font-version (.getVersion font)
       :num-glyphs num-glyphs
       :units-per-em (.getUnitsPerEm head-table)
       :bbox (.getFontBBox font)
       :head {:checkSumAdjustment (.getCheckSumAdjustment head-table)
                 :created (.getCreated head-table)
                 :flags (.getFlags head-table)
                 :fontDirectionHint (.getFontDirectionHint head-table)
                 :fontRevision (.getFontRevision head-table)
                 :glyphDataFormat (.getGlyphDataFormat head-table)
                 :indexToLocFormat (.getIndexToLocFormat head-table)
                 :lowestRecPPEM (.getLowestRecPPEM head-table)
                 :macStyle (.getMacStyle head-table)
                 :magicNumber (.getMagicNumber head-table)
                 :modified (.getModified head-table)
                 :unitsPerEm (.getUnitsPerEm head-table)
                 :version (.getVersion head-table)
                 :xMax (.getXMax head-table)
                 :xMin (.getXMin head-table)
                 :yMax (.getYMax head-table)
                 :yMin (.getYMin head-table)}
       :hhea {:advanceWidthMax (.getAdvanceWidthMax hhea-table)
                :ascender (.getAscender hhea-table)
                :caretSlopeRise (.getCaretSlopeRise hhea-table)
                :caretSlopeRun (.getCaretSlopeRun hhea-table)
                :descender (.getDescender hhea-table)
                :lineGap (.getLineGap hhea-table)
                :metricDataFormat (.getMetricDataFormat hhea-table)
                :minLeftSideBearing (.getMinLeftSideBearing hhea-table)
                :minRightSideBearing (.getMinRightSideBearing hhea-table)
                :numberOfHMetrics (.getNumberOfHMetrics hhea-table)
                :reserved1 (.getReserved1 hhea-table)
                :reserved2 (.getReserved2 hhea-table)
                :reserved3 (.getReserved3 hhea-table)
                :reserved4 (.getReserved4 hhea-table)
                :reserved5 (.getReserved5 hhea-table)
                :version (.getVersion hhea-table)
                :xMaxExtent (.getXMaxExtent hhea-table)}
       :hmtx {:metrics horizontal-metrics} 
       :vhea (when (some? vhea-table) 
               {:advanceHeightMax (.getAdvanceHeightMax vhea-table)
                   :ascender (.getAscender vhea-table)
                   :caretOffset (.getCaretOffset vhea-table)
                   :caretSlopeRise (.getCaretSlopeRise vhea-table)
                   :caretSlopeRun (.getCaretSlopeRun vhea-table)
                   :descender (.getDescender vhea-table)
                   :lineGap (.getLineGap vhea-table)
                   :metricDataFormat (.getMetricDataFormat vhea-table)
                   :minBottomSideBearing (.getMinBottomSideBearing vhea-table)
                   :minTopSideBearing (.getMinTopSideBearing vhea-table)
                   :numberOfVMetrics (.getNumberOfVMetrics vhea-table)
                   :reserved1 (.getReserved1 vhea-table)
                   :reserved2 (.getReserved2 vhea-table)
                   :reserved3 (.getReserved3 vhea-table)
                   :reserved4 (.getReserved4 vhea-table)
                   :version (.getVersion vhea-table)
                   :yMaxExtent (.getYMaxExtent vhea-table)})
       :vmtx {:metrics vertical-metrics}
       :maxp {:maxComponentDepth (.getMaxComponentDepth maxp-table)
                 :maxComponentElements (.getMaxComponentElements maxp-table)
                 :maxCompositeContours (.getMaxCompositeContours maxp-table)
                 :maxCompositePoints (.getMaxCompositePoints maxp-table)
                 :maxContours (.getMaxContours maxp-table)
                 :maxFunctionDefs (.getMaxFunctionDefs maxp-table)
                 :maxInstructionDefs (.getMaxInstructionDefs maxp-table)
                 :maxPoints (.getMaxPoints maxp-table)
                 :maxSizeOfInstructions (.getMaxSizeOfInstructions maxp-table)
                 :maxStackElements (.getMaxStackElements maxp-table)
                 :maxStorage (.getMaxStorage maxp-table)
                 :maxTwilightPoints (.getMaxTwilightPoints maxp-table)
                 :maxZones (.getMaxZones maxp-table)
                 :numGlyphs (.getNumGlyphs maxp-table)
                 :version (.getVersion maxp-table)}
       :cmap {:cmap-tables (vec (.getCmaps cmap-table))}
       :loca {:num-locations (.getOffsets loca-table)}
       :glyphs glyphs ; A vector of glyphs (just the first 5 for demonstration)
       :gpos  (.getGsubData gpos-table)})

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
        font-data
        (println "Failed to parse font data")))))

(println "Script loaded. Calling -main function:")
(apply -main *command-line-args*)

