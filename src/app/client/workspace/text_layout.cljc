(ns app.client.workspace.text-layout
  "Contract-T's single text-layout seam.

   T0 deliberately keeps the shipped monospace behavior. The value returned by
   `layout` is nevertheless the one immutable, versioned owner of wrap,
   measurement, positioned glyphs, caret stops, selection geometry, clipping,
   and hit testing. T1 replaces this provider with shaping; readers do not
   acquire another metric route."
  (:require [clojure.string :as str]))

(def layout-version 1)

(def legacy-index-space
  "T0's explicit adapter boundary. Offsets inside a layout result are UTF-16
   code-unit offsets; T2 can replace this source map without changing readers."
  {:domain :utf-16-code-unit :version 1})

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

(defn- tagged-index [offset]
  {:index-space legacy-index-space :offset offset})

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

(defn layout
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
                :output-hash (str id "/" (hash [texts logical-w logical-h]))
                :source-lines (vec (or source-lines texts))
                :font-shaper-environment legacy-provider}}))

(defn measure-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :metrics (:metrics layout-result)})

(defn wrap-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :lines (mapv :text (:lines layout-result))})

(defn paint-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :glyphs (vec (mapcat :glyphs (:lines layout-result)))
   :lines (:lines layout-result)})

(defn line-paint-ops
  "Adapt layout lines back to the existing text-op maps without changing the
   renderer-facing schema. Style/range keys come from `template`; positions and
   line text come only from the layout result."
  [layout-result template]
  (mapv (fn [{:keys [text baseline]}]
          (assoc template
                 :text text
                 :from 0
                 :to (code-unit-count text)
                 :x (first baseline)
                 :y (second baseline)))
        (:lines layout-result)))

(defn caret-result [layout-result line col]
  (let [lines (:lines layout-result)
        line (max 0 (min (long (or line 0)) (dec (max 1 (count lines)))))
        line-data (nth lines line {:text "" :logical-bounds {:x 0 :y 0 :h 0}
                                   :baseline [0 0]})
        col (max 0 (min (long (or col 0)) (code-unit-count (:text line-data))))
        advance (get-in layout-result [:font :size])
        char-advance (or (get-in (first (:glyphs line-data)) [:advance 0])
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
     :legacy/font-size advance}))

(defn selection-result
  [layout-result line col-start col-end & {:keys [min-width] :or {min-width 0}}]
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
            :h (:h line-bounds)}}))

(defn- ceil-long [x]
  (long (Math/ceil (double x))))

(defn- floor-long [x]
  (long (Math/floor (double x))))

(defn clip-result
  "Clip one existing text op through the layout's declared clip geometry.
   `range-mode` preserves the two legacy adapters exactly: `:left-right`
   rewrites :from/:to after substring clipping; `:right-only` preserves :from
   and only updates :to, matching rect-tree's old path."
  [layout-result op & {:keys [range-mode] :or {range-mode :left-right}}]
  (let [{:keys [left right top bottom]} (get-in layout-result [:constraints :clip])
        line (first (:lines layout-result))
        txt (:text line "")
        n (code-unit-count txt)
        [x y] (:baseline line [(:x op 0) (:y op 0)])
        cw (or (get-in (first (:glyphs line)) [:advance 0])
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
     :visible-range [(tagged-index skip) (tagged-index end)]}))

(defn hit-test-result
  "Point -> visual line -> optional logical line map -> legacy caret stop."
  [layout-result [x y]]
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
        cw (or (get-in (first (:glyphs first-line)) [:advance 0])
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
     :index-space legacy-index-space}))
