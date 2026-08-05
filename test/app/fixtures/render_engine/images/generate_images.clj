(ns generate-images
  "Deterministic JVM image corpus for IMAGE-ATOM Package 2.

   The profiled fixture deliberately uses the only admitted ImageIO road:
   explicit native PNG metadata with a PRE-DEFLATED iCCP payload.  Generation
   fails unless the written chunk inflates byte-for-byte to a parseable ICC
   profile."
  (:import (java.awt Transparency)
           (java.awt.color ColorSpace ICC_ColorSpace ICC_Profile)
           (java.awt.image BufferedImage ComponentColorModel DataBuffer)
           (java.io ByteArrayInputStream ByteArrayOutputStream File)
           (java.nio.charset StandardCharsets)
           (java.nio.file Files)
           (java.security MessageDigest)
           (java.util Arrays)
           (java.util.zip Deflater InflaterInputStream)
           (javax.imageio IIOImage ImageIO ImageTypeSpecifier)
           (javax.imageio.metadata IIOMetadataNode)))

(def ^:private native-png-format "javax_imageio_png_1.0")
(def ^:private output-dir
  (File. "test/app/fixtures/render_engine/images"))

(defn- hex [bytes]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff)) bytes)))

(defn- sha256 [bytes]
  (hex (.digest (MessageDigest/getInstance "SHA-256") bytes)))

(defn- clamp-byte [value]
  (int (max 0 (min 255 value))))

(defn- argb [a r g b]
  (unchecked-int
   (bit-or (bit-shift-left (clamp-byte a) 24)
           (bit-shift-left (clamp-byte r) 16)
           (bit-shift-left (clamp-byte g) 8)
           (clamp-byte b))))

(defn- raster-image [width height pixel-fn]
  (doto (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
    (#(doseq [y (range height)
              x (range width)]
        (.setRGB ^BufferedImage % x y (int (pixel-fn x y)))))))

(defn- write-png! [filename image]
  (let [file (File. output-dir filename)]
    (Files/deleteIfExists (.toPath file))
    (when-not (ImageIO/write image "png" file)
      (throw (ex-info "No ImageIO PNG writer" {:file filename})))
    file))

(defn- deflate [bytes]
  (let [deflater (Deflater. Deflater/BEST_COMPRESSION)
        out (ByteArrayOutputStream.)
        buffer (byte-array 8192)]
    (.setInput deflater bytes)
    (.finish deflater)
    (while (not (.finished deflater))
      (let [written (.deflate deflater buffer)]
        (.write out buffer 0 written)))
    (.end deflater)
    (.toByteArray out)))

(defn- write-profiled-png! [filename]
  (let [profile (ICC_Profile/getInstance ColorSpace/CS_LINEAR_RGB)
        color-space (ICC_ColorSpace. profile)
        component-count (.getNumComponents color-space)
        ;; Force the JVM CMM to materialize profile state before snapshotting
        ;; getData; the executed R2 probe found call-order-dependent bytes.
        _ (.toRGB color-space (float-array component-count))
        profile-bytes (.getData profile)
        bits (int-array (repeat component-count 8))
        color-model (ComponentColorModel.
                     color-space bits false false Transparency/OPAQUE
                     DataBuffer/TYPE_BYTE)
        raster (.createCompatibleWritableRaster color-model 8 8)
        _ (doseq [y (range 8)
                  x (range 8)
                  band (range component-count)]
            (.setSample raster x y band
                        (bit-and (+ (* x 32) (* y 8) (* band 3)) 0xff)))
        image (BufferedImage. color-model raster false nil)
        iccp (doto (IIOMetadataNode. "iCCP")
               (.setAttribute "profileName" "SoftlandLinearRGB")
               (.setAttribute "compressionMethod" "deflate")
               (.setUserObject (deflate profile-bytes)))
        root (doto (IIOMetadataNode. native-png-format)
               (.appendChild iccp))
        writer (.next (ImageIO/getImageWritersByFormatName "png"))
        metadata (.getDefaultImageMetadata
                  writer (ImageTypeSpecifier. image) nil)
        file (File. output-dir filename)]
    (.mergeTree metadata native-png-format root)
    (Files/deleteIfExists (.toPath file))
    (with-open [stream (ImageIO/createImageOutputStream file)]
      (.setOutput writer stream)
      (.write writer nil (IIOImage. image nil metadata) nil))
    (.dispose writer)
    {:file file :profile-bytes profile-bytes}))

(defn- u32-be [bytes offset]
  (bit-or (bit-shift-left (bit-and (aget bytes offset) 0xff) 24)
          (bit-shift-left (bit-and (aget bytes (+ offset 1)) 0xff) 16)
          (bit-shift-left (bit-and (aget bytes (+ offset 2)) 0xff) 8)
          (bit-and (aget bytes (+ offset 3)) 0xff)))

(defn- png-chunks [bytes]
  (loop [offset 8 chunks []]
    (if (>= offset (alength bytes))
      chunks
      (let [length (u32-be bytes offset)
            type (String. bytes (+ offset 4) 4 StandardCharsets/US_ASCII)
            start (+ offset 8)
            data (Arrays/copyOfRange bytes start (+ start length))
            next-offset (+ start length 4)]
        (recur next-offset (conj chunks {:type type :data data}))))))

(defn- inflate [bytes]
  (with-open [input (InflaterInputStream. (ByteArrayInputStream. bytes))
              out (ByteArrayOutputStream.)]
    (let [buffer (byte-array 8192)]
      (loop []
        (let [read (.read input buffer)]
          (when (pos? read)
            (.write out buffer 0 read)
            (recur)))))
    (.toByteArray out)))

(defn- assert-profile-roundtrip! [{:keys [^File file profile-bytes]}]
  (let [png-bytes (Files/readAllBytes (.toPath file))
        chunks (png-chunks png-bytes)
        iccp (first (filter #(= "iCCP" (:type %)) chunks))
        data (:data iccp)
        null-index (first (keep-indexed #(when (zero? (bit-and (int %2) 0xff)) %1)
                                        data))]
    (when-not (and iccp null-index (= 0 (bit-and (aget data (inc null-index)) 0xff)))
      (throw (ex-info "Profiled fixture lacks a valid iCCP header"
                      {:file (.getName file)
                       :chunks (mapv :type chunks)})))
    (let [compressed (Arrays/copyOfRange data (+ null-index 2) (alength data))
          inflated (inflate compressed)]
      (when-not (Arrays/equals profile-bytes inflated)
        (throw (ex-info "Written iCCP payload does not round-trip"
                        {:expected (sha256 profile-bytes)
                         :actual (sha256 inflated)})))
      (ICC_Profile/getInstance inflated)
      (when-not (ImageIO/read file)
        (throw (ex-info "Profiled fixture is not ImageIO-readable"
                        {:file (.getName file)})))
      {:profile-sha256 (sha256 inflated)
       :profile-bytes (alength inflated)
       :chunks (mapv :type chunks)})))

(defn- corpus-images []
  {"coverage-white-srgb.png"
   (raster-image 4 4 (fn [_ _] (argb 255 255 255 255)))

   "seam-byte-srgb.png"
   (raster-image 8 8 (fn [_ _] (argb 255 64 128 192)))

   "alpha-reference-straight.png"
   (raster-image 160 160 (fn [_ _] (argb 128 200 80 40)))

   "atlas-opaque-srgb.png"
   (raster-image 32 24
                 (fn [x y]
                   (let [edge? (or (zero? x) (zero? y) (= x 31) (= y 23))
                         checker? (even? (+ (quot x 4) (quot y 4)))]
                     (if edge?
                       (argb 255 246 58 42)
                       (if checker?
                         (argb 255 (+ 32 (* x 5)) (+ 48 (* y 6)) 224)
                         (argb 255 22 188 (+ 40 (* x 3))))))))

   "clip-stripes-srgb.png"
   (raster-image 40 32
                 (fn [x y]
                   (cond
                     (< x 10) (argb 255 244 52 52)
                     (< x 20) (argb 255 52 214 92)
                     (< x 30) (argb 255 54 104 238)
                     :else (argb 255 242 206 (+ 24 (mod (* y 7) 80))))))

   "dedicated-alpha-straight.png"
   (raster-image 160 96
                 (fn [_ _] (argb 128 200 80 40)))

   "dedicated-alpha-premultiplied.png"
   (raster-image 160 96
                 (fn [_ _] (argb 128 100 40 20)))})

(defn -main [& _]
  (.mkdirs output-dir)
  (let [files (mapv (fn [[filename image]] (write-png! filename image))
                    (sort-by key (corpus-images)))
        profiled (write-profiled-png! "profiled-linear-rgb.png")
        profile-receipt (assert-profile-roundtrip! profiled)
        all-files (conj files (:file profiled))]
    (doseq [^File file (sort-by #(.getName ^File %) all-files)]
      (let [bytes (Files/readAllBytes (.toPath file))]
        (println (pr-str {:file (.getName file)
                          :bytes (alength bytes)
                          :sha256 (sha256 bytes)}))))
    (println (pr-str {:profile profile-receipt}))))

(apply -main *command-line-args*)
