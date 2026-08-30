(ns app.client.text.layout
  "Text layout: wraps and measures text into lines of positioned glyphs, with
   caret stops, selection geometry, clipping, and hit testing, as one immutable
   result.
   Takes: raw text, a shaping provider, font size, character advance, line
   height, an origin; optionally inline size, wrap policy, headers, clip,
   source id and revision, zoom, tab stops.
   Gives: the layout result (lines, glyph positions, planes) plus accessors to
   read lines and glyph ranges back.
   Holds nothing; the retained arrays live in layout-planes."
  (:require [clojure.string :as str]
            [app.client.text.layout-planes :as planes]
            [app.client.text.shaped-line :as sl]
            #?(:cljs [goog.crypt :as gcrypt])
            #?(:cljs [goog.crypt.Sha256])))

(def layout-version 2)

(def legacy-index-space
  "T0's explicit adapter boundary. Offsets inside a layout result are UTF-16
   code-unit offsets; T2 can replace this source map without changing readers."
  {:domain :utf-16-code-unit :version 1})

(def index-space-token [:utf-16-code-unit 1])

(def legacy-provider
  {:face-id :legacy/monospace
   :face-revision "t0-v1"
   :shaper-id :legacy/code-unit-grid
   :shaper-version 1})

(defn code-unit-count [s]
  #?(:clj  (.length ^String (str (or s "")))
     :cljs (.-length (str (or s "")))))

(defn- code-unit-at [s i]
  #?(:clj  (int (.charAt ^String s i))
     :cljs (.charCodeAt s i)))

(def ^:private tagged-index-intern-limit 131072)

(def ^:private !tagged-index-cache
  ;; Flyweight: one shared immutable value per small offset. The same offset is
  ;; named by ~8-10 retained ranges per glyph (glyph cluster, cluster range,
  ;; run range, line ranges), so interning collapses millions of value-equal
  ;; maps to one per offset (measured 2026-08-08: 163MB of live tagged-index
  ;; maps on a 228k-glyph boot). Values are identical under =/hash.
  #?(:cljs (js/Array. tagged-index-intern-limit)
     :clj (object-array tagged-index-intern-limit)))

(defn tagged-index [offset]
  (if (and (number? offset)
           #?(:cljs (js/Number.isInteger offset)
              :clj (integer? offset))
           (<= 0 offset)
           (< offset tagged-index-intern-limit))
    (let [i (long offset)]
      (or #?(:cljs (aget !tagged-index-cache i)
             :clj (aget ^objects !tagged-index-cache i))
          (let [v {:index-space legacy-index-space :offset offset}]
            #?(:cljs (aset !tagged-index-cache i v)
               :clj (aset ^objects !tagged-index-cache i v))
            v)))
    {:index-space legacy-index-space :offset offset}))

(defn header-index [header-ordinal offset]
  [:header header-ordinal offset])

(defn source-index-offset
  "Return the numeric offset carried by a Contract-T body or per-header index."
  [index]
  (cond
    (map? index) (:offset index)
    (and (vector? index) (= :header (first index))) (nth index 2)
    :else nil))

(defn line-source-bounds
  "Return [start end] numeric offsets for either exact line range schema."
  [source-range]
  (if (and (vector? source-range) (= :header (first source-range)))
    (nth source-range 2)
    (mapv source-index-offset source-range)))

(defn legacy-char-advance
  "The sole T0 owner of the shipped `font-size * char-width`, including the
   existing DPR snap. Callers supply font/style inputs, never redo the metric."
  ([font-size char-width]
   (* font-size char-width))
  ([font-size char-width dpr snap?]
   (let [v (* font-size char-width)]
     (if snap?
       (/ (Math/round (double (* v (or dpr 1)))) (or dpr 1))
       v))))

(defn legacy-char-advance-step
  "Renderer adapter for the existing arbitrary snap-step projection."
  [font-size char-width snap-step]
  (let [v (legacy-char-advance font-size char-width)]
    (if (and snap-step (pos? snap-step))
      (* (Math/round (double (/ v snap-step))) snap-step)
      v)))

(defn wrap-line
  "The shipped word-boundary wrapper, now owned by the T0 provider."
  [line max-chars]
  (if (or (<= (code-unit-count line) max-chars) (< max-chars 1))
    [line]
    (let [words (str/split line #" ")]
      (loop [ws words cur "" result []]
        (if (empty? ws)
          (if (seq cur) (conj result cur) result)
          (let [w (first ws)
                candidate (if (seq cur) (str cur " " w) w)]
            (cond
              (<= (code-unit-count candidate) max-chars)
              (recur (rest ws) candidate result)

              (seq cur)
              (recur ws "" (conj result cur))

              :else
              (let [chunks (loop [remaining w acc []]
                             (if (<= (code-unit-count remaining) max-chars)
                               (conj acc remaining)
                               (recur (subs remaining max-chars)
                                      (conj acc (subs remaining 0 max-chars)))))]
                (recur (rest ws) (peek chunks) (into result (pop chunks)))))))))))

(defn block-wrap-lines
  "The shipped ground-block greedy column wrapper, owned by the same provider.
   Its behavior intentionally differs from `wrap-line`."
  [lines col]
  (vec
   (mapcat
    (fn [line]
      (if (<= (code-unit-count line) col)
        [line]
        (loop [remaining line out []]
          (if (<= (code-unit-count remaining) col)
            (conj out remaining)
            (let [head (subs remaining 0 (inc col))
                  i (str/last-index-of head " ")
                  cut (if (and i (pos? i)) i col)]
              (recur (str/triml (subs remaining cut))
                     (conj out (subs remaining 0 cut))))))))
    lines)))

(defn- visual-lines
  [{:keys [text source-lines wrap-policy max-chars headers]}]
  (let [body (or source-lines
                 (case wrap-policy
                   :word (str/split-lines (or text ""))
                   :block-greedy (str/split (or text "") #"\n" -1)
                   ;; Painting and hit adapters preserve explicit empty lines.
                   (str/split (or text "") #"\n" -1)))
        body (case wrap-policy
               :word (if max-chars
                       (vec (mapcat #(wrap-line % max-chars) body))
                       (vec body))
               :block-greedy (if max-chars
                               (block-wrap-lines body max-chars)
                               (vec body))
               (vec body))]
    (into (vec (or headers [])) body)))

(defn- input-id [semantic-input]
  ;; The semantic input itself remains in :receipts; this stable process-local
  ;; hash is identity, never a source offset or shaping substitute.
  (str "t0/" (hash semantic-input)))

(defn- legacy-layout
  "Produce the immutable Contract-T result using the behavior-identical T0
   monospace provider.

   Required semantic inputs are source text (or exact `source-lines`) and the
   current advance/line height. `origin` is material-local. `baseline-offset`
   defaults to zero because existing text-op call sites already carry baseline
   y; ground blocks pass font-size to preserve their existing baseline."
  [{:keys [text source-lines font-size char-advance line-height origin
           baseline-offset inline-size max-chars wrap-policy headers clip
           line-map source-id source-revision]
    :or {text "" font-size 14 char-advance 7.84 line-height 14
         origin [0 0] baseline-offset 0 wrap-policy :none}}]
  (let [[ox oy] origin
        text (str (or text ""))
        max-chars (or max-chars
                      (when (and (= wrap-policy :word)
                                 (number? inline-size)
                                 (pos? char-advance))
                        (max 1 (long (/ inline-size char-advance)))))
        semantic-input
        {:text text
         :source-lines source-lines
         :source-id source-id
         :source-revision source-revision
         :index-space legacy-index-space
         :font legacy-provider
         :font-size font-size
         :char-advance char-advance
         :line-height line-height
         :origin origin
         :baseline-offset baseline-offset
         :inline-size inline-size
         :max-chars max-chars
         :wrap-policy wrap-policy
         :headers headers
         :clip clip
         :line-map line-map}
        id (input-id semantic-input)
        texts (visual-lines semantic-input)
        ;; Visual offsets are an explicit T0 adapter map. They are not silently
        ;; claimed to be shaped-cluster boundaries.
        line-data
        (loop [i 0 visual-offset 0 texts texts acc []]
          (if (empty? texts)
            acc
            (let [line-text (first texts)
                  n (code-unit-count line-text)
                  top-y (+ oy (* i line-height))
                  baseline-y (+ top-y baseline-offset)
                  glyphs
                  (mapv (fn [col]
                          (let [start (+ visual-offset col)]
                            {:glyph-id (code-unit-at line-text col)
                             :character (subs line-text col (inc col))
                             :cluster {:source-range [(tagged-index start)
                                                      (tagged-index (inc start))]}
                             :position [(+ ox (* col char-advance)) baseline-y]
                             :advance [char-advance 0]
                             :offset [0 0]
                             :ink-bounds nil}))
                        (range n))
                  source-range [(tagged-index visual-offset)
                                (tagged-index (+ visual-offset n))]
                  line {:line/id [id i]
                        :line/index i
                        :text line-text
                        :source-range source-range
                        :baseline [ox baseline-y]
                        :advance (* n char-advance)
                        :logical-bounds {:x ox :y top-y
                                         :w (* n char-advance) :h line-height}
                        :ink-bounds nil
                        :run-range [i (inc i)]
                        :glyphs glyphs}]
              (recur (inc i) (+ visual-offset n 1) (rest texts) (conj acc line)))))
        runs (mapv (fn [line]
                     {:source-range (:source-range line)
                      :direction :ltr
                      :font-revision (:face-revision legacy-provider)
                      :glyphs (:glyphs line)})
                   line-data)
        clusters (mapv (fn [glyph]
                         (let [[start end] (get-in glyph [:cluster :source-range])
                               [x y] (:position glyph)
                               [advance _] (:advance glyph)]
                           {:source-range [start end]
                            :caret-stops [{:index start :position [x y]
                                           :affinity :downstream}
                                          {:index end :position [(+ x advance) y]
                                           :affinity :upstream}]
                            :logical-bounds {:x x :y (- y baseline-offset)
                                             :w advance :h line-height}
                            :ink-bounds nil}))
                       (mapcat :glyphs line-data))
        logical-w (reduce max 0 (map :advance line-data))
        logical-h (* (max 1 (count line-data)) line-height)]
    (planes/compact-result
     {:text-layout/version layout-version
     :layout/id id
     :source {:id source-id
              :revision source-revision
              :text text
              :index-space legacy-index-space
              :source-map {:kind :legacy-visual-lines
                           :visual-text (str/join "\n" texts)}}
     :font (merge legacy-provider {:size font-size :variations {}
                                   :features [] :fallback-chain []})
     :shaping {:shaper-id (:shaper-id legacy-provider)
               :version (:shaper-version legacy-provider)
               :language :und :script :legacy :direction :ltr}
     :space {:coordinates :material-local}
     :constraints {:inline-size (or inline-size :unbounded)
                   :wrap wrap-policy
                   :line-height line-height
                   :alignment :start
                   :tab-stops :legacy
                   :clip clip
                   :line-map line-map}
     :metrics {:advance [logical-w logical-h]
               :stack-advance (* (count line-data) line-height)
               :ink-bounds nil
               :logical-bounds {:x ox :y oy :w logical-w :h logical-h}
               :ascent baseline-offset
               :descent (- line-height baseline-offset)
               :leading 0}
     :lines line-data
     :runs runs
     :clusters clusters
     :clip-plan {:visible-lines (mapv :line/id line-data)
                 :visible-glyph-ranges (mapv :source-range line-data)
                 :clip-geometry clip}
     :receipts {:input-hash id
                :output-hash (str id "/" (hash [layout-version texts
                                                 logical-w logical-h]))
                :source-lines (vec (or source-lines texts))
                :font-shaper-environment legacy-provider}})))

(defn- shaped-provider? [provider]
  (and provider (fn? (:shape-line provider))))

(defn- provider-identity [provider]
  (select-keys provider [:face-id :face-revision :shaper-id :shaper-version
                         :features :variations :axes :fallback-chain :upem
                         :metrics]))

(defn layout-key
  "The shaping-correction keying source. Only full visible projection,
   stable address/revision, provider identity, and layout metrics enter.
   Paint, camera, origin, selection, hover, backend, and broad rebuild `sig`
   are deliberately absent (T6/T7/T8)."
  [{:keys [address stamp body-text header-texts
           provider font-size line-height baseline-offset wrap-policy wrap-col
           language direction tab-stops]}]
  (when (nil? address)
    (throw (ex-info "Text layout identity requires an opaque :address." {})))
  (let [body-text (str (or body-text ""))
        header-texts (mapv str (or header-texts []))
        features (or (:effective-features provider) (:features provider) [])
        variations (or (:effective-variations provider) (:variations provider) {})
        metrics (:metrics provider)
        source-token [address
                      (if (some? stamp) [:stamped stamp] [:unstamped])
                      [body-text header-texts]]
        provider-token [(:face-id provider) (:face-revision provider)
                        (:shaper-id provider) (:shaper-version provider)
                        features variations (:axes provider)
                        (or (:fallback-chain provider) []) (:upem provider)
                        (:ascender metrics) (:descender metrics)
                        (:lineGap metrics)]
        wrap-token (if (and (= :block-greedy wrap-policy)
                            (number? wrap-col) (pos? wrap-col))
                     [:columns wrap-col]
                     [:unbounded])
        metric-token [layout-version font-size line-height baseline-offset
                      wrap-policy wrap-token (or language "und")
                      (or direction :bidi) (or tab-stops {:columns 4})
                      index-space-token]]
    [source-token provider-token metric-token]))

(defn- source-line-records [text source-lines]
  (let [lines (vec (or source-lines (str/split (str (or text "")) #"\n" -1)))]
    (loop [remaining lines line-index 0 source-start 0 records []]
      (if (empty? remaining)
        records
        (let [line-text (first remaining)
              n (code-unit-count line-text)]
          (recur (rest remaining) (inc line-index) (+ source-start n 1)
                 (conj records {:text line-text
                                :logical-line line-index
                                :source-start source-start
                                :source-end (+ source-start n)})))))))

(defn break-whitespace-at?
  "The shaping-correction break class: U+0020 SPACE and U+0009 TAB only.
   T14: never replace this with a platform-dependent `\\s` predicate."
  [text offset]
  (when (and (<= 0 offset) (< offset (code-unit-count text)))
    (let [cu (code-unit-at text offset)]
      (or (= 0x20 cu) (= 0x09 cu)))))

(defn- work+! [!work key n]
  (vswap! !work update key (fnil + 0) n))

;; --- the flat road: spans, wrap, and ink over columns -----------------------

(defn- sort-glyph-order
  "Glyph indexes of one shaped line ordered by (cluster-start, cluster-end,
   index): the exact grouping `glyph-span-index` used to build from maps."
  [line]
  (let [g (long (:glyph-count line))
        cs (:cluster-start line)
        ce (:cluster-end line)]
    #?(:clj (let [order (sl/i32-array g)]
              (doseq [[k i] (map-indexed vector
                                         (sort-by (fn [i] [(sl/u32-get cs i)
                                                           (sl/u32-get ce i) i])
                                                  (range g)))]
                (sl/i32-set! order k i))
              order)
       :cljs (let [order (js/Int32Array. g)]
               (dotimes [i g] (aset order i i))
               (.sort order (fn [a b]
                              (let [d (- (aget cs a) (aget cs b))]
                                (if (zero? d)
                                  (let [e (- (aget ce a) (aget ce b))]
                                    (if (zero? e) (- a b) e))
                                  d))))
               order))))

(defn- line-spans
  "One pass over a shaped line's cluster columns: the span table — sorted by
   (source-start, source-end), each span's ascending glyph indexes laid out
   contiguously in `:order`, its [min, max+1) glyph range, its font-unit
   left/right edge, and whether it is contiguous. Pure; the caller counts
   `glyph-visits` and `cluster-index-writes` exactly as the map road did."
  [line]
  (let [g (long (:glyph-count line))
        order (sort-glyph-order line)
        cs (:cluster-start line)
        ce (:cluster-end line)
        gx (:glyph-x line)
        ax (:advance-x line)
        starts (sl/i32-array g)
        ends (sl/i32-array g)
        gstarts (sl/i32-array g)
        gends (sl/i32-array g)
        ostarts (sl/i32-array g)
        oends (sl/i32-array g)
        lefts (sl/i32-array g)
        rights (sl/i32-array g)
        contiguous (sl/u8-array g)
        count
        (loop [k 0 s 0]
          (if (>= k g)
            s
            (let [i (sl/i32-get order k)
                  start (sl/u32-get cs i)
                  end (sl/u32-get ce i)
                  ;; the group [k, k') shares (start, end); indexes ascend
                  k' (loop [k' (inc k)]
                       (if (and (< k' g)
                                (let [j (sl/i32-get order k')]
                                  (and (= start (sl/u32-get cs j))
                                       (= end (sl/u32-get ce j)))))
                         (recur (inc k'))
                         k'))
                  [lo hi left right]
                  (loop [m k lo i hi i
                         left (sl/i32-get gx i)
                         right (+ (sl/i32-get gx i) (sl/i32-get ax i))]
                    (if (>= m k')
                      [lo hi left right]
                      (let [j (sl/i32-get order m)
                            x (sl/i32-get gx j)
                            x2 (+ x (sl/i32-get ax j))]
                        (recur (inc m) (min lo j) (max hi j)
                               (min left x) (max right x2)))))]
              (sl/i32-set! starts s start)
              (sl/i32-set! ends s end)
              (sl/i32-set! gstarts s lo)
              (sl/i32-set! gends s (inc hi))
              (sl/i32-set! ostarts s k)
              (sl/i32-set! oends s k')
              (sl/i32-set! lefts s (min left right))
              (sl/i32-set! rights s (max left right))
              (sl/u8-set! contiguous s (if (= (- k' k) (- (inc hi) lo)) 1 0))
              (recur k' (inc s)))))]
    {:count count
     :starts starts :ends ends
     :glyph-starts gstarts :glyph-ends gends
     :order order :order-starts ostarts :order-ends oends
     :lefts lefts :rights rights
     :contiguous contiguous
     :monotonic? (loop [s 0]
                   (cond (>= s count) true
                         (zero? (sl/u8-get contiguous s)) false
                         :else (recur (inc s))))}))

(defn- span-start [spans i] (sl/i32-get (:starts spans) i))
(defn- span-end [spans i] (sl/i32-get (:ends spans) i))

(defn- span-width [spans i]
  (Math/abs (- (double (sl/i32-get (:rights spans) i))
               (double (sl/i32-get (:lefts spans) i)))))

(defn- span-break-whitespace? [text spans i]
  (let [start (span-start spans i) end (span-end spans i)]
    (and (< start end)
         (every? #(break-whitespace-at? text %) (range start end)))))

(defn- scan-wrap-cut
  "Choose one segment-relative cut from an already-shaped source line.
   A cluster is revisited at most once after the chosen whitespace boundary,
   keeping the complete cut walk linear with a <=2C visit bound. (The map
   road's algorithm, re-keyed to the span table.)"
  [line-text spans start-index segment-start max-units !work]
  (let [cluster-count (long (:count spans))
        source-length (code-unit-count line-text)]
    (loop [i start-index
           advance 0.0
           last-fitting nil
           last-candidate nil]
      (if (>= i cluster-count)
        {:kind :final :paint-end source-length :owned-end source-length
         :next-index cluster-count}
        (let [_ (work+! !work :wrap-candidate-visits 1)
              whitespace? (span-break-whitespace? line-text spans i)]
          (if whitespace?
            (let [run (loop [j i width 0.0 fitting-end last-fitting
                             fitting-next-index i]
                        (if (and (< j cluster-count)
                                 (span-break-whitespace? line-text spans j))
                          (let [width' (+ width (span-width spans j))
                                fits? (<= (+ advance width') max-units)]
                            (when (> j i)
                              (work+! !work :wrap-candidate-visits 1))
                            (recur (inc j) width'
                                   (if fits? (span-end spans j) fitting-end)
                                   (if fits? (inc j) fitting-next-index)))
                          {:next-index j
                           :run-end (if (> j i)
                                      (span-end spans (dec j))
                                      (span-end spans i))
                           :width width
                           :fitting-end fitting-end
                           :fitting-next-index fitting-next-index}))
                  run-start (span-start spans i)
                  candidate? (and (> run-start segment-start)
                                  (< (:run-end run) source-length)
                                  (<= advance max-units))
                  candidate (if candidate?
                              {:kind :break
                               :paint-end run-start
                               :owned-end (:run-end run)
                               :consumed-start run-start
                               :consumed-end (:run-end run)
                               :next-index (:next-index run)}
                              last-candidate)
                  next-advance (+ advance (:width run))]
              (if (> next-advance max-units)
                (or candidate
                    (when (:fitting-end run)
                      {:kind :hard :paint-end (:fitting-end run)
                       :owned-end (:fitting-end run)
                       :next-index (:fitting-next-index run)})
                    {:kind :hard
                     :paint-end (span-end spans i)
                     :owned-end (span-end spans i)
                     :next-index (inc i)})
                (recur (:next-index run) next-advance (:run-end run) candidate)))
            (let [next-advance (+ advance (span-width spans i))
                  fitting? (<= next-advance max-units)
                  last-fitting (if fitting? (span-end spans i) last-fitting)]
              (if (and (not fitting?) (> next-advance max-units))
                (or last-candidate
                    (when last-fitting
                      {:kind :hard :paint-end last-fitting
                       :owned-end last-fitting :next-index i})
                    {:kind :hard
                     :paint-end (span-end spans i)
                     :owned-end (span-end spans i)
                     :next-index (inc i)})
                (recur (inc i) next-advance last-fitting last-candidate)))))))))

(defn- shaped-segments
  "Choose all cuts from one shaped pass, then let `shaped-layout` reshape only
   the final segments. The returned ranges are relative to the source line.
   `spans` is the span table of `shaped`."
  [line-text shaped spans inline-size !work]
  (let [source-length (code-unit-count line-text)
        cluster-count (long (:count spans))]
    (cond
      (or (not (number? inline-size)) (not (pos? inline-size)))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped :spans spans}]

      (and (pos? source-length) (zero? cluster-count))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped :spans spans
        :provider-fault? true}]

      (or (zero? source-length) (<= (double (or (:advance shaped) 0))
                                    inline-size))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped :spans spans}]

      :else
      (loop [segment-start 0 start-index 0 result []]
        (if (>= segment-start source-length)
          result
          (let [{:keys [paint-end owned-end consumed-start consumed-end
                        next-index] :as cut}
                (scan-wrap-cut line-text spans start-index segment-start
                               inline-size !work)
                paint-end (max segment-start (min source-length paint-end))
                owned-end (max paint-end (min source-length owned-end))
                ;; Provider cluster boundaries are the only progress unit.
                owned-end (if (= owned-end segment-start)
                            (min source-length (span-end spans start-index))
                            owned-end)]
            (recur owned-end
                   (max (or next-index (inc start-index))
                        (loop [i start-index]
                          (if (and (< i cluster-count)
                                   (< (span-start spans i) owned-end))
                            (recur (inc i))
                            i)))
                   (conj result
                         (cond-> {:text (subs line-text segment-start paint-end)
                                  :relative-start segment-start
                                  :relative-end paint-end
                                  :owned-end owned-end}
                           consumed-start
                           (assoc :consumed-relative [consumed-start consumed-end])
                           (:provider-fault? cut)
                           (assoc :provider-fault? true))))))))))

(defn- union-bounds [bounds]
  (when (seq bounds)
    (let [x1 (reduce min (map :x bounds))
          y1 (reduce min (map :y bounds))
          x2 (reduce max (map #(+ (:x %) (:w %)) bounds))
          y2 (reduce max (map #(+ (:y %) (:h %)) bounds))]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

(declare nearest-by)

(defn line-by-id
  "Resolve a line through the retained line-id index. The optional fallback is
   explicit so callers can count its single extra visit."
  ([layout-result line-id]
   (get-in layout-result [:line-index line-id]))
  ([layout-result line-id fallback-y]
   (or (line-by-id layout-result line-id)
       (nearest-by fallback-y #(second (:baseline %)) (:lines layout-result)))))

(defn glyphs-in-source-range
  "Indexed paint/clip selection. `visited-glyphs` counts only the selected
   span, never the full line vector (G6's executable receipt)."
  ([line-data source-range]
   (planes/glyphs-in-source-range line-data source-range))
  ([line-data source-range dx dy]
   (planes/glyphs-in-source-range line-data source-range dx dy)))

(defn line-glyphs
  "Derive Contract-T glyph maps for one line; the maps are never retained."
  ([line-data] (planes/line-glyphs line-data))
  ([line-data dx dy] (planes/line-glyphs line-data dx dy)))

(defn line-clusters
  "Derive Contract-T cluster views for one line from the span plane."
  [line-data]
  (planes/line-clusters line-data))

(defn glyph-span-index-view
  "Derived compatibility/oracle view of one line's indexed spans."
  [line-data]
  (planes/glyph-span-index-view line-data))

(defn first-glyph-advance-x [line-data]
  (planes/first-glyph-advance-x line-data))

(defn result-runs [layout-result]
  (planes/result-runs layout-result))

(defn plane-census [layout-result]
  (planes/plane-census layout-result))

(defn retained-rich-map? [layout-result]
  (planes/retained-rich-map? layout-result))

(defn within-span-bound? [{:keys [visited-glyphs glyph-span-count]}]
  (<= (long (or visited-glyphs 0)) (+ (long (or glyph-span-count 0)) 8)))

(defn glyph-indexes-in-source-range
  "Indexed selection without glyph maps: the result-wide glyph indexes of a
   source range plus the span receipt (the flat paint road's read)."
  [line-data source-range]
  (planes/glyph-indexes-in-source-range line-data source-range))

(defn glyph-views
  "Derive Contract-T glyph maps for explicit result-wide indexes (the oracle
   paint road)."
  [line-data indexes dx dy]
  (planes/glyph-views line-data indexes dx dy))

(defn pack-glyphs!
  "The pack door: walk result-wide `indexes` of one line calling
   `(f index glyph-id shaped? tab? font-id x y cluster-start cluster-end)`
   with primitives only. See `layout-planes/pack-glyphs!`."
  [line-data indexes dx dy f]
  (planes/pack-glyphs! line-data indexes dx dy f))

(defn result=
  "Layout-result equality that sees through typed planes."
  [a b]
  (planes/result= a b))

(defn- shaped-layout
  "The flat road: shape every visual line into columns, derive each line's
   span table in one pass, then fill the result-wide planes directly — no
   glyph map between the provider and the retained result. The map road it
   is fenced against lives verbatim in `app.client.text.layout-oracle`."
  [{:keys [text source-lines provider font-size line-height origin
           baseline-offset inline-size wrap-policy wrap-col headers clip
           line-map source-id source-revision features variations language
           direction tab-stops zoom]
    :or {text "" font-size 14 line-height 14 origin [0 0]
         baseline-offset 0 wrap-policy :none zoom 1}}]
  (let [text (str (or text ""))
        headers (mapv str (or headers []))
        [ox oy] origin
        shape-opts {:features (or features (:features provider))
                    :variations (or variations (:variations provider))
                    :language (or language "und")
                    :direction (or direction :bidi)
                    :tab-columns (or (:columns tab-stops) 4)}
        !work (volatile! {:glyph-visits 0
                          :cluster-index-writes 0
                          :cluster-index-reads 0
                          :run-index-writes 0
                          :run-index-reads 0
                          :wrap-candidate-visits 0
                          :shape-calls 0
                          :reference-shapes 0
                          :provider-fault 0})
        ;; The one seam: a map-shaped provider result is coerced here.
        shape! (fn [s]
                 (work+! !work :shape-calls 1)
                 (sl/from-maps ((:shape-line provider) s shape-opts)))
        upem (double (or (:upem provider) 1000))
        scale (/ font-size upem)
        positive-wrap-col? (and (= :block-greedy wrap-policy)
                                (number? wrap-col) (pos? wrap-col))
        reference-shaped
        (when positive-wrap-col?
          (work+! !work :reference-shapes 1)
          (sl/from-maps ((:shape-line provider) " " shape-opts)))
        reference-units
        (when reference-shaped
          (let [advance (:advance reference-shaped)]
            (when (and (pos? (long (:glyph-count reference-shaped)))
                       (number? advance) (pos? advance))
              (double advance))))
        _ (when (and positive-wrap-col? (nil? reference-units))
            (work+! !work :provider-fault 1))
        reference-advance (some-> reference-units (* scale))
        effective-inline-size
        (cond
          positive-wrap-col? (when reference-units (* wrap-col reference-units))
          (= :word wrap-policy) (when (and (number? inline-size)
                                           (pos? inline-size))
                                  (/ inline-size scale))
          :else nil)
        source-records (source-line-records text source-lines)
        header-records
        (mapv (fn [h header-text]
                (let [shaped (shape! header-text)]
                  (when (and (pos? (code-unit-count header-text))
                             (zero? (long (:glyph-count shaped))))
                    (work+! !work :provider-fault 1))
                  {:kind :header :header-index h :text header-text
                   :relative-start 0 :relative-end (code-unit-count header-text)
                   :owned-end (code-unit-count header-text) :shaped shaped}))
              (range) headers)
        visual-records
        (vec
         (concat
          header-records
          (mapcat
           (fn [{line-text :text source-start :source-start :as source-line}]
             (let [wrapped? (and (#{:word :block-greedy} wrap-policy)
                                 (number? effective-inline-size)
                                 (pos? effective-inline-size))
                   initial (when wrapped? (shape! line-text))
                   initial-spans (when initial (line-spans initial))
                   fault? (and initial (pos? (code-unit-count line-text))
                               (zero? (long (:glyph-count initial))))
                   _ (when fault? (work+! !work :provider-fault 1))
                   segments (if wrapped?
                              (shaped-segments line-text initial initial-spans
                                               effective-inline-size !work)
                              [{:text line-text :relative-start 0
                                :relative-end (code-unit-count line-text)
                                :owned-end (code-unit-count line-text)}])]
               (map (fn [segment]
                      (let [relative-start (:relative-start segment)
                            relative-end (:relative-end segment)
                            owned-end (:owned-end segment)
                            consumed (:consumed-relative segment)]
                        (cond-> (merge source-line segment
                                       {:kind :body
                                        :source-start (+ source-start relative-start)
                                        :paint-end (+ source-start relative-end)
                                        :source-end (+ source-start owned-end)})
                          consumed
                          (assoc :consumed-absolute
                                 (mapv #(+ source-start %) consumed)))))
                    segments)))
           source-records)))
        semantic-input {:text text :source-lines source-lines
                        :headers headers
                        :source-id source-id :source-revision source-revision
                        :index-space legacy-index-space
                        :font (provider-identity provider)
                        :font-size font-size :line-height line-height
                        :origin origin :baseline-offset baseline-offset
                        :inline-size inline-size :wrap-policy wrap-policy
                        :wrap-col wrap-col
                        :features (:features shape-opts)
                        :variations (:variations shape-opts)
                        :language (:language shape-opts)
                        :direction (:direction shape-opts)
                        :tab-stops tab-stops :clip clip :line-map line-map
                        :zoom zoom}
        id (str "t1/" (hash semantic-input))
        ;; Pass 1 — every visual line shaped and span-indexed; the plane shape
        ;; (advance-y column, glyph order) is decided over the whole result.
        prepared
        (mapv
         (fn [{:keys [text shaped spans provider-fault?] :as visual}]
           (let [shaped (or shaped (shape! text))
                 spans (or spans (line-spans shaped))
                 g (long (:glyph-count shaped))
                 _ (work+! !work :glyph-visits g)
                 _ (work+! !work :cluster-index-writes (:count spans))
                 line-fault? (and (pos? (code-unit-count text)) (zero? g))
                 _ (when (and line-fault? (not provider-fault?))
                     (work+! !work :provider-fault 1))]
             {:visual visual :shaped shaped :spans spans}))
         visual-records)
        total-glyphs (reduce + 0 (map #(long (:glyph-count (:shaped %))) prepared))
        total-spans (reduce + 0 (map #(+ (long (:count (:spans %)))
                                         (if (:consumed-absolute (:visual %)) 1 0))
                                     prepared))
        non-monotonic? (boolean (some #(not (:monotonic? (:spans %))) prepared))
        advance-y? (boolean
                    (some (fn [{:keys [shaped]}]
                            (let [ay (:advance-y shaped)
                                  g (long (:glyph-count shaped))]
                              (loop [i 0]
                                (cond (>= i g) false
                                      (not (zero? (sl/i32-get ay i))) true
                                      :else (recur (inc i))))))
                          prepared))
        b (planes/plane-builder total-glyphs total-spans advance-y?
                                (if non-monotonic? total-glyphs 0))
        source {:id source-id :revision source-revision :text text
                :headers headers
                :index-space legacy-index-space
                :source-map {:kind :shaped-visual-lines
                             :visual-lines (mapv #(select-keys % [:text :source-start
                                                                 :source-end :logical-line])
                                                 visual-records)}}
        ;; Pass 2 — fill the planes line by line; one line-spec map per line.
        [line-specs glyph-ids]
        (loop [remaining prepared visual-index 0 gb 0 sb 0 rb 0 ob 0
               specs (transient []) ids (transient [])]
          (if-let [{:keys [visual shaped spans]} (first remaining)]
            (let [{:keys [text source-start source-end paint-end logical-line
                          kind consumed-absolute]} visual
                  g (long (:glyph-count shaped))
                  top-y (+ oy (* visual-index line-height))
                  baseline-y (+ top-y baseline-offset)
                  header? (= kind :header)
                  header-ordinal (:header-index visual)
                  span-source-offset (if header? 0 source-start)
                  index-of (fn [offset]
                             (if header?
                               (header-index header-ordinal offset)
                               (tagged-index (+ source-start offset))))
                  gid-col (:glyph-id shaped)
                  cs-col (:cluster-start shaped)
                  ce-col (:cluster-end shaped)
                  gx-col (:glyph-x shaped)
                  gy-col (:glyph-y shaped)
                  ax-col (:advance-x shaped)
                  ay-col (:advance-y shaped)
                  offx-col (:offset-x shaped)
                  offy-col (:offset-y shaped)
                  ix-col (:ink-x shaped)
                  iy-col (:ink-y shaped)
                  iw-col (:ink-w shaped)
                  ih-col (:ink-h shaped)
                  run-col (:run-index shaped)
                  ;; glyph rows + the line's ink union (doubles, as the map road)
                  [ink-x1 ink-y1 ink-x2 ink-y2 ink? ids]
                  (loop [i 0 ink-x1 0.0 ink-y1 0.0 ink-x2 0.0 ink-y2 0.0
                         ink? false ids ids]
                    (if (>= i g)
                      [ink-x1 ink-y1 ink-x2 ink-y2 ink? ids]
                      (let [run (sl/u32-get run-col i)
                            tab? (sl/run-tab? shaped run)
                            rtl? (sl/run-rtl? shaped run)
                            gid (sl/u32-get gid-col i)
                            gx (sl/i32-get gx-col i)
                            gy (sl/i32-get gy-col i)
                            px (+ ox (* gx scale))
                            py (- baseline-y (* gy scale))
                            ax (* (sl/i32-get ax-col i) scale)
                            ay (* (sl/i32-get ay-col i) scale)
                            off-x (* (sl/i32-get offx-col i) scale)
                            off-y (* (sl/i32-get offy-col i) scale)
                            glyph-ink? (sl/glyph-has-ink? shaped i)
                            [gix giy giw gih]
                            (if glyph-ink?
                              (let [xb (sl/i32-get ix-col i)
                                    yb (sl/i32-get iy-col i)
                                    w (sl/i32-get iw-col i)
                                    h (sl/i32-get ih-col i)
                                    x1 (+ ox (* gx scale) (* xb scale))
                                    x2 (+ x1 (* w scale))
                                    y1 (- baseline-y (* (+ gy yb) scale))
                                    y2 (- y1 (* h scale))]
                                [(min x1 x2) (min y1 y2)
                                 (Math/abs (- x2 x1)) (Math/abs (- y2 y1))])
                              [0 0 0 0])]
                        (planes/put-glyph! b (+ gb i) px py ax ay off-x off-y
                                           glyph-ink? gix giy giw gih
                                           (if tab? 0 gid) tab? rtl?
                                           (sl/u32-get cs-col i)
                                           (sl/u32-get ce-col i))
                        (recur (inc i)
                               (if glyph-ink? (if ink? (min ink-x1 gix) gix) ink-x1)
                               (if glyph-ink? (if ink? (min ink-y1 giy) giy) ink-y1)
                               (if glyph-ink?
                                 (if ink? (max ink-x2 (+ gix giw)) (+ gix giw))
                                 ink-x2)
                               (if glyph-ink?
                                 (if ink? (max ink-y2 (+ giy gih)) (+ giy gih))
                                 ink-y2)
                               (or ink? glyph-ink?)
                               (conj! ids (if tab? nil gid))))))
                  line-ink (when ink?
                             {:x ink-x1 :y ink-y1
                              :w (- ink-x2 ink-x1) :h (- ink-y2 ink-y1)})
                  ;; span rows (+ the trailing empty consumed span)
                  span-count (long (:count spans))
                  _ (dotimes [s span-count]
                      (work+! !work :cluster-index-reads 1)
                      (planes/put-span! b (+ sb s)
                                        (+ span-source-offset (span-start spans s))
                                        (+ span-source-offset (span-end spans s))
                                        (if non-monotonic?
                                          (+ ob (sl/i32-get (:order-starts spans) s))
                                          (+ gb (sl/i32-get (:glyph-starts spans) s)))
                                        (if non-monotonic?
                                          (+ ob (sl/i32-get (:order-ends spans) s))
                                          (+ gb (sl/i32-get (:glyph-ends spans) s)))))
                  _ (when consumed-absolute
                      (let [[consumed-start consumed-end] consumed-absolute
                            at (if non-monotonic? (+ ob g) (+ gb g))]
                        (planes/put-span! b (+ sb span-count)
                                          consumed-start consumed-end at at)))
                  _ (when non-monotonic?
                      (let [order (:order spans)]
                        (dotimes [k g]
                          (planes/put-order! b (+ ob k) (+ gb (sl/i32-get order k))))))
                  line-span-count (+ span-count (if consumed-absolute 1 0))
                  ;; run records: retained per run, glyph spans result-wide
                  run-count (long (:run-count shaped))
                  runs
                  (mapv
                   (fn [r]
                     (let [local-start (sl/u32-get (:run-source-start shaped) r)
                           local-end (sl/u32-get (:run-source-end shaped) r)
                           abs-start (+ span-source-offset local-start)
                           abs-end (+ span-source-offset local-end)
                           face (sl/run-face shaped r)
                           ;; spans owned by the run: source-start in [abs-start, abs-end)
                           [lo hi] (loop [s 0 lo nil hi nil]
                                     (if (>= s span-count)
                                       [lo hi]
                                       (let [ss (+ span-source-offset (span-start spans s))]
                                         (if (and (<= abs-start ss) (< ss abs-end))
                                           (let [g0 (sl/i32-get (:glyph-starts spans) s)
                                                 g1 (sl/i32-get (:glyph-ends spans) s)]
                                             (recur (inc s)
                                                    (if lo (min lo g0) g0)
                                                    (if hi (max hi g1) g1)))
                                           (recur (inc s) lo hi)))))
                           _ (do (work+! !work :run-index-writes 1)
                                 (work+! !work :run-index-reads 1))
                           first-index (or lo 0)
                           font-id (when (< first-index g)
                                     (:id (sl/run-face shaped
                                                       (sl/u32-get run-col first-index))))
                           advance-y-column?
                           (boolean
                            (when lo
                              (loop [i lo]
                                (cond (>= i hi) false
                                      (not (zero? (sl/i32-get ay-col i))) true
                                      :else (recur (inc i))))))]
                       (cond-> {:source-range [(index-of local-start)
                                               (index-of local-end)]
                                :direction (if (sl/run-rtl? shaped r) :rtl :ltr)
                                :font-revision (:revision face)
                                :glyph-span (if lo [(+ gb lo) (+ gb hi)] [gb gb])
                                :font-id font-id}
                         advance-y-column? (assoc :advance-y-column? true))))
                   (range run-count))
                  run-source-index
                  (->> runs
                       (map-indexed
                        (fn [i run]
                          (let [[start end] (mapv source-index-offset
                                                  (:source-range run))]
                            {:source-start start :source-end end :run-index i})))
                       (sort-by (juxt :source-start :source-end))
                       vec)
                  advance (* (:advance shaped 0) scale)
                  logical-bounds {:x ox :y top-y :w advance :h line-height}
                  line-source-range
                  (if header?
                    [:header header-ordinal [0 (code-unit-count text)]]
                    [(tagged-index source-start) (tagged-index source-end)])
                  paint-source-range
                  (if header?
                    line-source-range
                    [(tagged-index source-start)
                     (tagged-index (or paint-end source-end))])
                  spec {:line/id [id visual-index]
                        :line/index visual-index
                        :logical-line logical-line
                        :text text
                        :source-range line-source-range
                        :paint-source-range paint-source-range
                        :consumed-range (when consumed-absolute
                                          (mapv tagged-index consumed-absolute))
                        :baseline [ox baseline-y]
                        :advance advance
                        :logical-bounds logical-bounds
                        :ink-bounds line-ink
                        :run-range [rb (+ rb run-count)]
                        :glyph-start gb
                        :glyph-end (+ gb g)
                        :cluster-start sb
                        :cluster-end (+ sb line-span-count)
                        :runs runs
                        ::planes/shaped? (pos? g)
                        ::planes/run-source-index run-source-index}]
              (recur (next remaining) (inc visual-index) (+ gb g)
                     (+ sb line-span-count) (+ rb run-count) (+ ob g)
                     (conj! specs spec) ids))
            [(persistent! specs) (persistent! ids)]))
        planes (planes/finish-planes! b source)
        lines (mapv #(assoc % ::planes/planes planes) line-specs)
        logical-w (reduce max 0 (map :advance lines))
        logical-h (* (max 1 (count lines)) line-height)
        metrics (:metrics provider)
        ascent (* (or (:ascender metrics) 0) scale)
        descent (* (- (or (:descender metrics) 0)) scale)
        leading (* (or (:lineGap metrics) 0) scale)
        legal-zoom? (<= 0.01 zoom 1000)
        work @!work]
    (when-not legal-zoom?
      (throw (ex-info "Text zoom is outside Contract-T's legal material range."
                      {:zoom zoom :legal-range [0.01 1000]})))
    {:text-layout/version layout-version
     :layout/id id
     :layout/planes planes
     :source source
     :font (merge (provider-identity provider)
                  {:size font-size :variations (:variations shape-opts)
                   :features (:features shape-opts)})
     :shaping {:shaper-id (:shaper-id provider)
               :version (:shaper-version provider)
               :language (:language shape-opts) :script :auto
               :direction :bidi}
     :space {:coordinates :material-local}
     :regime {:legal-zoom [0.01 1000] :zoom zoom
              :precision :material-f64
              :paint-road :consumer-selected}
     :constraints {:inline-size (or inline-size :unbounded)
                   :wrap wrap-policy :line-height line-height
                   :alignment :start :tab-stops (or tab-stops {:columns 4})
                   :clip clip :line-map line-map}
     :metrics {:advance [logical-w logical-h]
               :stack-advance (* (count lines) line-height)
               :ink-bounds (union-bounds (keep :ink-bounds lines))
               :logical-bounds {:x ox :y oy :w logical-w :h logical-h}
               :ascent ascent :descent descent :leading leading}
     :lines lines
     :line-index (into {} (map (juxt :line/id identity)) lines)
     :reference-advance reference-advance
     :inline-size (when effective-inline-size (* effective-inline-size scale))
     :clip-plan {:visible-lines (mapv :line/id lines)
                 :visible-glyph-ranges (mapv :source-range lines)
                 :clip-geometry clip}
     :receipts {:input-hash id
                :output-hash (str id "/" (hash [layout-version glyph-ids
                                                logical-w logical-h]))
                :source-lines (vec (or source-lines (mapv :text source-records)))
                :font-shaper-environment (provider-identity provider)
                :proportionality (dissoc work :provider-fault :reference-shapes)
                :reference-shapes (:reference-shapes work)
                :provider-fault (:provider-fault work)}}))

(defn layout
  "Produce the one immutable Contract-T result. A real provider selects T1;
   absence of a provider preserves the exact T0 compatibility road."
  [{:keys [provider] :as input}]
  (if (shaped-provider? provider)
    (shaped-layout input)
    (legacy-layout input)))

(defn- sha256-hex [s]
  #?(:clj
     (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                           (.getBytes (str s) "UTF-8"))]
       (apply str (map #(format "%02x" (bit-and (int %) 0xff)) digest)))
     :cljs
     (let [digest (goog.crypt.Sha256.)]
       (.update digest (gcrypt/stringToUtf8ByteArray (str s)))
       (gcrypt/byteArrayToHex (.digest digest)))))

(defn layout-id-digest [results]
  (->> results
       vals
       (map :layout/id)
       (map str)
       sort
       vec
       pr-str
       sha256-hex))

(defn empty-layout-cache []
  {:address->key {}
   :key->result {}
   :hits 0 :misses 0 :replacements 0
   :oracle-checks 0 :oracle-mismatches 0})

(defn layout-cache-report [cache]
  {:size (count (:key->result cache))
   :address-count (count (:address->key cache))
   :hits (:hits cache 0)
   :misses (:misses cache 0)
   :replacements (:replacements cache 0)
   :oracle-checks (:oracle-checks cache 0)
   :oracle-mismatches (:oracle-mismatches cache 0)
   :id-digest (layout-id-digest (:key->result cache))})

(defn layout-cache-reset-counters
  "Open a new measurement window without changing cache ownership or values."
  [cache]
  (assoc (or cache (empty-layout-cache))
         :hits 0 :misses 0 :replacements 0
         :oracle-checks 0 :oracle-mismatches 0))

(defn oracle-match?
  "I2's oracle comparator. Sees through typed planes (`planes/result=`): two
   results holding distinct typed arrays are equal when their planes are
   element-wise equal and the rest is structurally equal."
  [cached fresh]
  (planes/result= cached fresh))

(defn layout-cache-acquire
  "Pure one-current-entry-per-address cache transition. `build-result` is a
   zero-arity batch oracle/constructor. On a production hit, oracle mode may
   recompute without changing miss/layout conservation."
  [cache address key build-result & {:keys [oracle?]}]
  (let [cache (or cache (empty-layout-cache))
        current-key (get-in cache [:address->key address])
        cached (get-in cache [:key->result key])]
    (if (and (= current-key key) cached)
      (let [cache (update cache :hits (fnil inc 0))]
        (if oracle?
          (let [fresh (build-result)
                match? (oracle-match? cached fresh)
                cache (cond-> (update cache :oracle-checks (fnil inc 0))
                        (not match?)
                        (update :oracle-mismatches (fnil inc 0)))]
            {:cache cache :result cached :hit? true
             :oracle-result fresh :oracle-match? match?})
          {:cache cache :result cached :hit? true}))
      (let [result (build-result)
            replacement? (some? current-key)
            cache (cond-> cache
                    current-key (update :key->result dissoc current-key)
                    true (assoc-in [:address->key address] key)
                    true (assoc-in [:key->result key] result)
                    true (update :misses (fnil inc 0))
                    replacement? (update :replacements (fnil inc 0)))]
        {:cache cache :result result :hit? false
         :replacement? replacement?}))))

(defn layout-cache-remove-addresses [cache addresses]
  (reduce
   (fn [cache address]
     (if-let [key (get-in cache [:address->key address])]
       (-> cache
           (update :address->key dissoc address)
           (update :key->result dissoc key))
       cache))
   (or cache (empty-layout-cache))
   addresses))

(def work-counter-keys
  [:glyph-visits :cluster-index-writes :cluster-index-reads
   :run-index-writes :run-index-reads :wrap-candidate-visits])

(defn work-units
  "G1's exact proportionality vocabulary; shaping calls are deliberately
   reported separately."
  [receipt]
  (reduce + 0 (map #(long (or (get receipt %) 0)) work-counter-keys)))

(defn within-work-bound?
  ([receipt glyph-count cluster-count run-count]
   (within-work-bound? receipt glyph-count cluster-count run-count
                       :non-monotonic? false))
  ([receipt glyph-count cluster-count run-count & {:keys [non-monotonic?]}]
   (let [glyph-count (long glyph-count)
         base (* 4 (+ glyph-count cluster-count run-count))
         sort-allowance
         (if (and non-monotonic? (pos? glyph-count))
           (* glyph-count
              (long (Math/ceil
                     (/ (Math/log (inc glyph-count)) (Math/log 2)))))
           0)]
     (<= (work-units receipt) (+ base sort-allowance)))))

(defn measure-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :metrics (:metrics layout-result)})

(defn wrap-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :lines (mapv :text (:lines layout-result))})

(defn copy-result
  "Source-based copy. Break-consumed characters remain copyable even though
   their painted width is zero. Header ranges stay in their own domain."
  [layout-result source-range]
  (if (and (vector? source-range) (= :header (first source-range)))
    (let [[_ h [start end]] source-range
          text (get-in layout-result [:source :headers h] "")]
      {:layout/id (:layout/id layout-result)
       :source-range source-range
       :text (subs text start end)})
    (let [[start end] (line-source-bounds source-range)
          text (get-in layout-result [:source :text] "")]
      {:layout/id (:layout/id layout-result)
       :source-range source-range
       :text (subs text start end)})))

(defn paint-result [layout-result]
  (let [lines (planes/rich-lines layout-result)]
    {:layout/id (:layout/id layout-result)
     :glyphs (vec (mapcat :glyphs lines))
     :lines lines}))

(defn line-paint-ops
  "Adapt layout lines back to the existing text-op maps without changing the
   renderer-facing schema. Style/range keys come from `template`; positions and
   line text come only from the layout result."
  [layout-result template]
  (mapv (fn [{:keys [line/id text baseline source-range paint-source-range]}]
          (assoc template
                 :text text
                 :from 0
                 :to (code-unit-count text)
                 :x (first baseline)
                 :y (second baseline)
                 :layout-result layout-result
                 :layout-line-id id
                 :layout-anchor [(first baseline) (second baseline)]
                 :paint-source-range (or paint-source-range source-range)))
        (:lines layout-result)))

(defn source-offset->line-col
  "Map one body UTF-16 source offset into the retained visual layout. Interior
   consumed offsets stay at the preceding line; consumed-end belongs to the
   following line's start."
  [layout-result offset]
  (let [offset (long (or offset 0))
        body-lines (vec (remove #(= :header (first (:source-range %)))
                                (:lines layout-result)))
        chosen (or (first
                    (filter
                     (fn [line]
                       (let [[start end] (line-source-bounds
                                          (:source-range line))
                             consumed-end (some-> line :consumed-range second
                                                  source-index-offset)]
                         (and (<= start offset)
                              (if consumed-end (< offset end) (<= offset end)))))
                     body-lines))
                   (last body-lines)
                   (first (:lines layout-result)))
        [start end] (line-source-bounds (:source-range chosen))]
    {:line (:line/index chosen 0)
     :col (- (max start (min offset end)) start)}))

(defn- shaped-result? [layout-result]
  (not= :legacy/code-unit-grid (get-in layout-result [:shaping :shaper-id])))

(defn- cluster-caret-stops
  "A cluster's two edge caret stops. Shaped clusters no longer retain
   :caret-stops (memory: they are a pure function of retained fields); stored
   stops win when present (legacy grid path, consumed clusters)."
  [cluster]
  (or (:caret-stops cluster)
      (when-let [{:keys [x y w]} (:logical-bounds cluster)]
        (let [[start end] (:source-range cluster)
              rtl? (= :rtl (:direction cluster))
              left x
              right (+ x w)
              start-x (if rtl? right left)
              end-x (if rtl? left right)]
          [{:index start :position [start-x y] :affinity :downstream}
           {:index end :position [end-x y] :affinity :upstream}]))))

(defn- injected-cluster-stops
  "T2 additive reader capability. Grapheme boundaries are injected DATA; an
   interior boundary interpolates between the shaped cluster's declared edge
   stops by its UTF-16 advance fraction."
  [cluster grapheme-boundaries]
  (let [[start-index end-index] (:source-range cluster)
        start (source-index-offset start-index)
        end (source-index-offset end-index)
        [start-stop end-stop] (cluster-caret-stops cluster)
        [start-x start-y] (:position start-stop)
        [end-x end-y] (:position end-stop)
        span (- end start)]
    (if (and (pos? span) (seq grapheme-boundaries))
      (->> grapheme-boundaries
           (map #(if (map? %) (source-index-offset %) %))
           (filter #(and (number? %) (< start % end)))
           distinct
           sort
           (mapv (fn [boundary]
                   (let [fraction (/ (- boundary start) span)]
                     {:index (tagged-index boundary)
                      :position [(+ start-x (* fraction (- end-x start-x)))
                                 (+ start-y (* fraction (- end-y start-y)))]
                      :affinity :downstream
                      :interior? true}))))
      [])))

(defn- line-caret-stops
  ([line-data] (line-caret-stops line-data nil))
  ([line-data grapheme-boundaries]
   (->> (line-clusters line-data)
        (mapcat (fn [cluster]
                  (concat (cluster-caret-stops cluster)
                          (injected-cluster-stops cluster
                                                  grapheme-boundaries))))
        vec)))

(defn- nearest-by [value value-fn xs]
  (when (seq xs)
    (reduce (fn [best candidate]
              (if (< (Math/abs (- (double (value-fn candidate)) value))
                     (Math/abs (- (double (value-fn best)) value)))
                candidate
                best))
            (first xs) (rest xs))))

(defn- shaped-caret-result
  [layout-result line col {:keys [grapheme-boundaries affinity]}]
  (let [lines (:lines layout-result)
        line (max 0 (min (long (or line 0)) (dec (max 1 (count lines)))))
        line-data (nth lines line {:text "" :logical-bounds {:x 0 :y 0 :h 0}
                                   :source-range [(tagged-index 0) (tagged-index 0)]})
        [line-start line-end] (line-source-bounds (:source-range line-data))
        requested (+ line-start (max 0 (min (long (or col 0))
                                               (- line-end line-start))))
        consumed-end (some-> line-data :consumed-range second source-index-offset)
        next-line-index (when (and consumed-end (= requested consumed-end)
                                   (< (inc line) (count lines)))
                          (inc line))
        next-line-data (when next-line-index (nth lines next-line-index))
        next-line-start (some-> next-line-data :source-range line-source-bounds first)
        move-to-next? (and next-line-data
                           (= requested next-line-start)
                           (not= :upstream affinity))
        line (if move-to-next? next-line-index line)
        line-data (if move-to-next? next-line-data line-data)
        [line-start _] (line-source-bounds (:source-range line-data))
        stops (line-caret-stops line-data grapheme-boundaries)
        exact (filter #(= requested (source-index-offset (:index %))) stops)
        stop (or (when affinity
                   (first (filter #(= affinity (:affinity %)) exact)))
                 (first (filter #(= :downstream (:affinity %)) exact))
                 (first exact)
                 (nearest-by requested #(source-index-offset (:index %)) stops)
                 {:index (if (= :header (first (:source-range line-data)))
                           (header-index (second (:source-range line-data)) line-start)
                           (tagged-index line-start))
                  :position [(:x (:logical-bounds line-data))
                             (:y (:logical-bounds line-data))]
                  :affinity :downstream})
        [x _] (:position stop)
        bounds (:logical-bounds line-data)
        actual-col (- (or (source-index-offset (:index stop)) line-start)
                      line-start)]
    {:layout/id (:layout/id layout-result)
     :index (:index stop)
     :line line
     :col actual-col
     :position [x (:y bounds)]
     :rect {:x x :y (:y bounds) :w 2 :h (:h bounds)}
     :affinity (:affinity stop)}))

(defn caret-result
  ([layout-result line col]
   (caret-result layout-result line col nil))
  ([layout-result line col options]
  (if (shaped-result? layout-result)
    (shaped-caret-result layout-result line col options)
    (let [lines (:lines layout-result)
        line (max 0 (min (long (or line 0)) (dec (max 1 (count lines)))))
        line-data (nth lines line {:text "" :logical-bounds {:x 0 :y 0 :h 0}
                                   :baseline [0 0]})
        col (max 0 (min (long (or col 0)) (code-unit-count (:text line-data))))
        advance (get-in layout-result [:font :size])
        char-advance (or (first-glyph-advance-x line-data)
                         (let [w (get-in line-data [:logical-bounds :w] 0)
                               n (code-unit-count (:text line-data))]
                           (if (pos? n) (/ w n) 0)))
        bounds (:logical-bounds line-data)]
    {:layout/id (:layout/id layout-result)
     :index (tagged-index (+ (get-in line-data [:source-range 0 :offset] 0) col))
     :line line
     :col col
     :position [(+ (:x bounds) (* col char-advance)) (:y bounds)]
     :rect {:x (+ (:x bounds) (* col char-advance))
            :y (:y bounds) :w 2 :h (:h bounds)}
     :affinity :downstream
     :legacy/font-size advance}))))

(defn- shaped-selection-result [layout-result line col-start col-end min-width]
  (let [a (shaped-caret-result layout-result line col-start nil)
        b (shaped-caret-result layout-result line col-end nil)
        [start end] (sort [(source-index-offset (:index a))
                           (source-index-offset (:index b))])
        line-data (get (:lines layout-result) (:line a))
        selected (filter
                   (fn [cluster]
                     (let [[cs ce] (mapv source-index-offset
                                         (:source-range cluster))]
                       (and (< cs end) (> ce start))))
                   (line-clusters line-data))
        rects (mapv :logical-bounds selected)
        rects (if (seq rects)
                rects
                [{:x (first (:position a))
                  :y (get-in line-data [:logical-bounds :y] 0)
                  :w min-width
                  :h (get-in line-data [:logical-bounds :h] 0)}])
        bounding (or (union-bounds rects)
                     {:x (first (:position a)) :y 0 :w min-width :h 0})]
    {:layout/id (:layout/id layout-result)
     :source-range [(:index a) (:index b)]
     :rects rects
     :rect (update bounding :w max min-width)}))

(defn selection-result
  [layout-result line col-start col-end & {:keys [min-width] :or {min-width 0}}]
  (if (shaped-result? layout-result)
    (shaped-selection-result layout-result line col-start col-end min-width)
    (let [a (caret-result layout-result line col-start)
        b (caret-result layout-result line col-end)
        x1 (first (:position a))
        x2 (first (:position b))
        line-bounds (get-in layout-result [:lines (:line a) :logical-bounds]
                            {:y 0 :h 0})]
    {:layout/id (:layout/id layout-result)
     :source-range [(:index a) (:index b)]
     :rect {:x (min x1 x2) :y (:y line-bounds)
            :w (max min-width (Math/abs (- x2 x1)))
            :h (:h line-bounds)}})))

(defn- ceil-long [x]
  (long (Math/ceil (double x))))

(defn- floor-long [x]
  (long (Math/floor (double x))))

(defn clip-result
  "Clip one existing text op through the layout's declared clip geometry.
   `range-mode` preserves the two legacy adapters exactly: `:left-right`
   rewrites :from/:to after substring clipping; `:right-only` preserves :from
   and only updates :to, matching the prior layout path."
  [layout-result op & {:keys [range-mode] :or {range-mode :left-right}}]
  (if (shaped-result? layout-result)
    (let [{:keys [left right top bottom]} (get-in layout-result [:constraints :clip])
          requested-line-id (:layout-line-id op)
          line (or (line-by-id layout-result requested-line-id (:y op 0))
                   (first (:lines layout-result)))
          [line-start line-end] (line-source-bounds (:source-range line))
          [op-start op-end]
          (if-let [paint-range (:paint-source-range op)]
            (line-source-bounds paint-range)
            [(+ line-start (long (or (:from op) 0)))
             (+ line-start (long (or (:to op)
                                     (code-unit-count (:text op "")))))])
          vertical? (and (or (nil? top) (>= (second (:baseline line)) top))
                         (or (nil? bottom) (< (second (:baseline line)) bottom)))
          candidate-range (if (= :header (first (:source-range line)))
                            [:header (second (:source-range line))
                             [op-start op-end]]
                            [(tagged-index op-start) (tagged-index op-end)])
          candidate-selection (glyphs-in-source-range line candidate-range)
          visible (filter
                    (fn [glyph]
                      (let [[x _] (:position glyph)
                            [advance _] (:advance glyph)
                            x2 (+ x advance)
                            glyph-left (min x x2)
                            glyph-right (max x x2)]
                        (and (or (nil? left) (> glyph-right left))
                             (or (nil? right) (< glyph-left right)))))
                    (:glyphs candidate-selection))
          visible-start (if (seq visible)
                          (reduce min
                                  (map #(source-index-offset
                                         (get-in % [:cluster :source-range 0]))
                                       visible))
                          op-start)
          visible-end (if (seq visible)
                        (reduce max
                                (map #(source-index-offset
                                       (get-in % [:cluster :source-range 1]))
                                     visible))
                        op-start)
          relative-start (- visible-start line-start)
          relative-end (- visible-end line-start)
          visible-range (if (= :header (first (:source-range line)))
                          [:header (second (:source-range line))
                           [visible-start visible-end]]
                          [(tagged-index visible-start)
                           (tagged-index visible-end)])]
      {:layout/id (:layout/id layout-result)
       :visited-lines (if (line-by-id layout-result requested-line-id) 1 2)
       :visited-glyphs (:visited-glyphs candidate-selection)
       :op (when (and vertical? (or (and (nil? left) (nil? right)) (seq visible)))
             (cond-> (assoc op
                            :paint-source-range visible-range)
               (= range-mode :left-right)
               (assoc :from relative-start :to relative-end)

               (= range-mode :right-only)
               (assoc :to relative-end)))
       :visible-range visible-range})
    (let [{:keys [left right top bottom]} (get-in layout-result [:constraints :clip])
        line (first (:lines layout-result))
        txt (:text line "")
        n (code-unit-count txt)
        [x y] (:baseline line [(:x op 0) (:y op 0)])
        cw (or (first-glyph-advance-x line)
               (let [w (get-in line [:logical-bounds :w] 0)]
                 (if (pos? n) (/ w n) 0)))
        text-end (+ x (* n cw))
        vertical? (and (or (nil? top) (>= y top))
                       (or (nil? bottom) (< y bottom)))
        horizontal? (and (or (nil? right) (< x right))
                         (or (nil? left) (> text-end left)))
        skip (if (and left (< x left) (pos? cw))
               (min n (ceil-long (/ (- left x) cw)))
               0)
        adj-x (+ x (* skip cw))
        take-n (if (and right (pos? cw))
                 (max 0 (floor-long (/ (- right adj-x) cw)))
                 (- n skip))
        end (min n (+ skip take-n))]
    {:layout/id (:layout/id layout-result)
     :op (when (and vertical?
                    (if (or left right) (and horizontal? (< skip end)) true))
           (if-not (or left right)
             op
             (cond-> (assoc op :text (subs txt skip end) :x adj-x)
               (= range-mode :left-right)
               (assoc :from skip :to end)

               (= range-mode :right-only)
               (assoc :to end))))
     :visible-range [(tagged-index skip) (tagged-index end)]})))

(defn- shaped-hit-test-result
  [layout-result [x y] {:keys [grapheme-boundaries]}]
  (let [lines (:lines layout-result)
        line-data (or (first (filter (fn [line]
                                      (let [{ly :y h :h} (:logical-bounds line)]
                                        (<= ly y (+ ly h))))
                                    lines))
                      (nearest-by y #(get-in % [:logical-bounds :y]) lines)
                      (first lines))
        stops (line-caret-stops line-data grapheme-boundaries)
        consumed-start (some-> line-data :consumed-range first source-index-offset)
        painted-end (+ (get-in line-data [:logical-bounds :x] 0)
                       (get-in line-data [:logical-bounds :w] 0))
        stop (or (when (and consumed-start (> x painted-end))
                   (first (filter #(= consumed-start
                                      (source-index-offset (:index %)))
                                  stops)))
                 (nearest-by x #(first (:position %)) stops)
                 {:index (first (:source-range line-data))})
        visual-line (:line/index line-data 0)
        logical-line (or (get-in layout-result [:constraints :line-map visual-line])
                         (:logical-line line-data)
                         visual-line)
        source-line-start (first (line-source-bounds (:source-range line-data)))
        col (max 0 (- (or (source-index-offset (:index stop)) source-line-start)
                      source-line-start))]
    {:layout/id (:layout/id layout-result)
     :visual-line visual-line
     :line logical-line
     :col col
     :index (:index stop)
     :affinity (:affinity stop)
     :index-space legacy-index-space}))

(defn hit-test-result
  "Point -> visual line -> optional logical line map -> source caret stop.
   T2 may inject grapheme boundaries so shaped-cluster interiors become lawful
   stops without introducing a second metric route."
  ([layout-result point]
   (hit-test-result layout-result point nil))
  ([layout-result [x y] options]
  (if (shaped-result? layout-result)
    (shaped-hit-test-result layout-result [x y] options)
    (let [{:keys [line-map]} (:constraints layout-result)
        source-lines (or (get-in layout-result [:receipts :source-lines])
                         (mapv :text (:lines layout-result)))
        line-count (max 1 (if (seq line-map) (count line-map) (count source-lines)))
        bounds (get-in layout-result [:metrics :logical-bounds])
        line-h (get-in layout-result [:constraints :line-height])
        visual-line (-> (/ (- y (:y bounds)) line-h)
                        (Math/floor) long (max 0) (min (dec line-count)))
        logical-line (if (seq line-map)
                       (get line-map visual-line visual-line)
                       visual-line)
        line-text (get source-lines logical-line "")
        line-len (code-unit-count line-text)
        first-line (first (:lines layout-result))
        cw (or (first-glyph-advance-x first-line)
               (let [w (get-in first-line [:logical-bounds :w] 0)
                     n (code-unit-count (:text first-line ""))]
                 (if (pos? n) (/ w n) 0)))
        col (if (pos? cw)
              (-> (/ (- x (:x bounds)) cw)
                  double Math/round long (max 0) (min line-len))
              0)]
    {:layout/id (:layout/id layout-result)
     :visual-line visual-line
     :line logical-line
     :col col
     :index-space legacy-index-space}))))
