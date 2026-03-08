(ns app.client.workspace.trail
  "Markdown parsing, reasoning trail -> chat node building, agent panel sizing."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :refer [rt-node wrap-line]]
            [app.client.workspace.ui-primitives :as ui :refer [dt typo-title typo-subtitle typo-body typo-caption]]))

(def md-style-colors
  "Colors for inline markdown styles in reasoning blocks.
   Tuned for warm terminal-like feel on dark bg."
  {:normal {:r 0.72 :g 0.71 :b 0.71 :a 1.0}    ;; warm neutral, softer than terminal #a4a1a1
   :bold   {:r 0.88 :g 0.87 :b 0.87 :a 1.0}    ;; brighter for emphasis but not harsh white
   :code   {:r 0.00 :g 0.63 :b 0.89 :a 1.0}    ;; terminal blue (color4 #00a0e4)
   :link   {:r 0.00 :g 0.63 :b 0.89 :a 0.85}}) ;; same blue, slightly dimmer

(defn parse-md-inline-spans
  "Parse inline markdown: **bold**, *emphasis*, `code`, [link](url).
   Returns [{:text str :style :normal/:bold/:code/:link} ...]"
  [line]
  (let [len (count line)]
    (loop [i 0 spans [] cur ""]
      (if (>= i len)
        (let [final (if (seq cur) (conj spans {:text cur :style :normal}) spans)]
          (if (empty? final) [{:text "" :style :normal}] final))
        (let [ch (.charAt line i)]
          (cond
            ;; **bold**
            (and (= ch \*) (< (inc i) len) (= (.charAt line (inc i)) \*))
            (let [end (str/index-of line "**" (+ i 2))]
              (if (and end (> end (+ i 2)))
                (recur (+ end 2)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (+ i 2) end) :style :bold}))
                       "")
                (recur (+ i 2) spans (str cur "**"))))
            ;; *emphasis* (single asterisk, not followed by another *)
            (and (= ch \*)
                 (or (>= (inc i) len) (not= (.charAt line (inc i)) \*)))
            (let [end (str/index-of line "*" (inc i))]
              (if (and end (> end (inc i))
                       ;; Ensure closing * is not part of **
                       (or (>= (inc end) len) (not= (.charAt line (inc end)) \*)))
                (recur (inc end)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (inc i) end) :style :bold}))
                       "")
                (recur (inc i) spans (str cur "*"))))
            ;; `code`
            (= ch \`)
            (let [end (str/index-of line "`" (inc i))]
              (if (and end (> end (inc i)))
                (recur (inc end)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (inc i) end) :style :code}))
                       "")
                (recur (inc i) spans (str cur "`"))))
            ;; [link](url)
            (= ch \[)
            (let [close-bracket (str/index-of line "](" i)]
              (if close-bracket
                (let [close-paren (str/index-of line ")" (+ close-bracket 2))]
                  (if close-paren
                    (recur (inc close-paren)
                           (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                               (conj {:text (subs line (inc i) close-bracket) :style :link}))
                           "")
                    (recur (inc i) spans (str cur "["))))
                (recur (inc i) spans (str cur "["))))
            ;; Normal character
            :else
            (recur (inc i) spans (str cur ch))))))))

(defn wrap-md-spans
  "Word-wrap styled spans to fit max-chars per line.
   Returns [[{:text str :style kw} ...] ...] — one vector of spans per visual line."
  [spans max-chars]
  (let [total-len (reduce + 0 (map (comp count :text) spans))]
    (if (<= total-len max-chars)
      [spans]
      ;; Build flat [char style] vector, then greedy-wrap
      (let [flat (vec (mapcat (fn [{:keys [text style]}]
                                (map #(vector % style) text))
                              spans))
            n (count flat)
            reconstitute (fn [chars]
                           (if (empty? chars)
                             [{:text "" :style :normal}]
                             (->> chars
                                  (partition-by second)
                                  (mapv (fn [g] {:text (apply str (map first g))
                                                :style (second (first g))})))))]
        (loop [pos 0 lines []]
          (if (>= pos n)
            lines
            (let [remaining (- n pos)
                  line-end (+ pos (min remaining max-chars))]
              (if (<= remaining max-chars)
                ;; Last line
                (conj lines (reconstitute (subvec flat pos n)))
                ;; Find last space in [pos, line-end) to break at word boundary
                (let [break-at (loop [j (dec line-end)]
                                 (cond
                                   (<= j pos) -1
                                   (= (first (nth flat j)) \space) j
                                   :else (recur (dec j))))]
                  (if (>= break-at 0)
                    (recur (inc break-at)
                           (conj lines (reconstitute (subvec flat pos break-at))))
                    ;; No space found — hard break at max-chars
                    (recur line-end
                           (conj lines (reconstitute (subvec flat pos line-end))))))))))))))

(defn spans->text-ops
  "Convert a single visual line of styled spans into positioned text-ops.
   style-colors maps :normal/:bold/:code/:link to {:r :g :b :a}."
  [spans x y font-size char-advance style-colors]
  (loop [ss spans cx x ops []]
    (if (empty? ss)
      ops
      (let [{:keys [text style]} (first ss)
            c (get style-colors style (get style-colors :normal))
            op {:text text :type :comment
                :from 0 :to (count text)
                :x cx :y y
                :size font-size
                :r (:r c) :g (:g c) :b (:b c) :a (:a c)}]
        (recur (rest ss) (+ cx (* (count text) char-advance)) (conj ops op))))))

(defn- decorative-line?
  "True when line is a backtick-wrapped decorative border (contains ─ or ★)."
  [trimmed]
  (and (str/starts-with? trimmed "`")
       (str/ends-with? trimmed "`")
       (> (count trimmed) 2)
       (re-find #"[\u2500\u2605]" trimmed)))

(defn- decorative-inner
  "Extract meaningful ASCII text from a decorative border line."
  [trimmed]
  (-> (subs trimmed 1 (dec (count trimmed)))
      (str/replace #"[\u2500\u2605\u2014\u2022]" "")
      str/trim))

(defn parse-md-blocks
  "Parse markdown text into block-level elements.
   States: :normal, :in-code, :in-callout.
   Returns [{:type :header/:paragraph/:code-block/:list/:callout ...}]"
  [text]
  (let [src-lines (str/split-lines text)]
    (loop [ls src-lines state :normal blocks [] cur-para [] code-lang nil callout-label nil]
      (if (empty? ls)
        ;; Flush remaining
        (cond
          (= state :in-code)
          (conj blocks {:type :code-block :lang code-lang :lines cur-para})
          (= state :in-callout)
          (let [body-text (str/join "\n" cur-para)
                body-blocks (when (seq body-text) (parse-md-blocks body-text))]
            (conj blocks {:type :callout :label callout-label :body (or body-blocks [])}))
          (seq cur-para)
          (conj blocks {:type :paragraph :content (str/join " " cur-para)})
          :else blocks)
        (let [line (first ls)
              trimmed (str/trim line)]
          (case state
            :in-code
            (if (str/starts-with? trimmed "```")
              (recur (rest ls) :normal
                     (conj blocks {:type :code-block :lang code-lang :lines cur-para})
                     [] nil nil)
              (recur (rest ls) :in-code blocks (conj cur-para line) code-lang nil))

            :in-callout
            (if (and (decorative-line? trimmed) (empty? (decorative-inner trimmed)))
              ;; Closing border — emit callout block with recursively-parsed body
              (let [body-text (str/join "\n" cur-para)
                    body-blocks (when (seq body-text) (parse-md-blocks body-text))]
                (recur (rest ls) :normal
                       (conj blocks {:type :callout :label callout-label :body (or body-blocks [])})
                       [] nil nil))
              ;; Content inside callout — collect lines
              (recur (rest ls) :in-callout blocks (conj cur-para line) nil callout-label))

            ;; :normal state
            (cond
              ;; Code fence opening
              (str/starts-with? trimmed "```")
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    lang (let [r (str/trim (subs trimmed 3))] (when (seq r) r))]
                (recur (rest ls) :in-code blocks [] lang nil))
              ;; Decorative border line: backtick-wrapped ★/─ chars (Insight blocks)
              (decorative-line? trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    inner (decorative-inner trimmed)]
                (if (seq inner)
                  ;; Has meaningful text (e.g., "Insight") — enter callout mode
                  (recur (rest ls) :in-callout blocks [] nil inner)
                  ;; Just decorative — horizontal rule
                  (recur (rest ls) :normal (conj blocks {:type :hr}) [] nil nil)))
              ;; Table lines (pipe-delimited)
              (str/starts-with? trimmed "|")
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining table-lines]
                    (loop [rem ls tl []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (str/starts-with? t "|"))
                          (recur (rest rem) (conj tl t))
                          [rem tl])))]
                (recur remaining :normal
                       (conj blocks {:type :table :lines table-lines}) [] nil nil))
              ;; Header
              (re-find #"^#{1,6}\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    level (count (re-find #"^#+" trimmed))
                    content (str/trim (subs trimmed (inc level)))]
                (recur (rest ls) :normal
                       (conj blocks {:type :header :level level :content content})
                       [] nil nil))
              ;; Horizontal rule: --- or *** or ___ or repeated ─
              (or (re-find #"^[-*_]{3,}\s*$" trimmed)
                  (re-find #"^\u2500{3,}" trimmed))
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)]
                (recur (rest ls) :normal (conj blocks {:type :hr}) [] nil nil))
              ;; Bullet list item
              (re-find #"^[-*]\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining items]
                    (loop [rem ls items []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (re-find #"^[-*]\s+" t))
                          (recur (rest rem) (conj items {:content (str/trim (subs t 2))}))
                          [rem items])))]
                (recur remaining :normal (conj blocks {:type :list :items items}) [] nil nil))
              ;; Numbered list item (1. 2. 3. etc.)
              (re-find #"^\d+\.\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining items]
                    (loop [rem ls items []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (re-find #"^\d+\.\s+" t))
                          (let [after-num (str/replace-first t #"^\d+\.\s+" "")]
                            (recur (rest rem) (conj items {:content after-num})))
                          [rem items])))]
                (recur remaining :normal (conj blocks {:type :numbered-list :items items}) [] nil nil))
              ;; Blank line — paragraph break
              (empty? trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)]
                (recur (rest ls) :normal blocks [] nil nil))
              ;; Regular text — accumulate into paragraph
              :else
              (recur (rest ls) :normal blocks (conj cur-para trimmed) nil nil))))))))

(defn trail-node-color
  "Color for a trail node by kind. Returns {:r :g :b :a}."
  [kind tool-name]
  (case kind
    :reasoning    {:r 0.72 :g 0.71 :b 0.71 :a 1.0}
    :thinking     {:r 0.65 :g 0.65 :b 0.75 :a 0.6}  ;; dimmed — internal reasoning
    :tool-call    (case tool-name
                    ("Read" "read")        {:r 0.4 :g 0.85 :b 0.95 :a 1.0}  ;; cyan
                    ("Edit" "edit")        {:r 0.95 :g 0.85 :b 0.35 :a 1.0}  ;; yellow
                    ("Write" "write")      {:r 0.95 :g 0.85 :b 0.35 :a 1.0}  ;; yellow
                    ("Grep" "grep")        {:r 0.55 :g 0.9 :b 0.55 :a 1.0}   ;; green
                    ("Glob" "glob")        {:r 0.55 :g 0.9 :b 0.55 :a 1.0}   ;; green
                    ("Bash" "bash")        {:r 0.9 :g 0.65 :b 0.4 :a 1.0}    ;; orange
                    ("Task" "task")        {:r 0.75 :g 0.6 :b 0.95 :a 1.0}   ;; purple
                                           {:r 0.7 :g 0.7 :b 0.85 :a 1.0})  ;; default blue-gray
    :tool-call-start {:r 0.6 :g 0.6 :b 0.7 :a 0.7}
    :tool-result  {:r 0.55 :g 0.55 :b 0.6 :a 0.7}  ;; dim
    {:r 0.75 :g 0.75 :b 0.75 :a 1.0}))

(defn- tool-input-summary
  "One-line summary of tool input for card header."
  [tool-name input]
  (cond
    (and (string? (:file_path input)) (seq (:file_path input)))
    (:file_path input)
    (and (string? (:pattern input)) (seq (:pattern input)))
    (str "\"" (:pattern input) "\"")
    (and (string? (:command input)) (seq (:command input)))
    (let [cmd (:command input)]
      (if (> (count cmd) 60) (str (subs cmd 0 57) "...") cmd))
    :else ""))

(defn- file-op-tool?
  "True for tools that are read-only file operations (groupable)."
  [tool-name]
  (contains? #{"Read" "read" "Grep" "grep" "Glob" "glob"} tool-name))

(defn- group-trail-blocks
  "Group consecutive trail nodes into logical blocks.
   Merges consecutive :reasoning and :thinking nodes.
   Groups :tool-call-start + :tool-call + :tool-result by tool-id.
   Returns [{:block-type :nodes [...]}]."
  [trail]
  (reduce
    (fn [acc node]
      (let [kind (:kind node)
            last-block (peek acc)]
        (case kind
          :reasoning
          (if (and last-block (= :reasoning (:block-type last-block)))
            (conj (pop acc) (update last-block :nodes conj node))
            (conj acc {:block-type :reasoning :nodes [node]}))

          :thinking
          (if (and last-block (= :thinking (:block-type last-block)))
            (conj (pop acc) (update last-block :nodes conj node))
            (conj acc {:block-type :thinking :nodes [node]}))

          :tool-call-start
          (conj acc {:block-type :tool-card
                     :tool-id (:tool-id node)
                     :tool-name (:tool-name node)
                     :status :pending
                     :nodes [node]})

          :tool-call
          ;; Find matching tool-card block by tool-id, update it
          (let [tid (:tool-id node)
                idx (some (fn [i]
                            (when (and (= :tool-card (:block-type (nth acc i)))
                                       (= tid (:tool-id (nth acc i))))
                              i))
                          (range (dec (count acc)) -1 -1))]
            (if idx
              (update (vec acc) idx
                      (fn [b] (-> b (assoc :status :complete :input (:input node))
                                    (update :nodes conj node))))
              ;; Orphan tool-call — make standalone card
              (conj acc {:block-type :tool-card
                         :tool-id tid
                         :tool-name (:tool-name node)
                         :status :complete
                         :input (:input node)
                         :nodes [node]})))

          :tool-result
          (let [tid (:tool-id node)
                idx (some (fn [i]
                            (when (and (= :tool-card (:block-type (nth acc i)))
                                       (= tid (:tool-id (nth acc i))))
                              i))
                          (range (dec (count acc)) -1 -1))]
            (if idx
              (update (vec acc) idx
                      (fn [b] (-> b (assoc :result-content (:content node))
                                    (update :nodes conj node))))
              ;; Orphan result
              (conj acc {:block-type :tool-card
                         :tool-id tid
                         :tool-name "?"
                         :status :complete
                         :result-content (:content node)
                         :nodes [node]})))

          ;; Unknown kind — treat as reasoning
          (conj acc {:block-type :reasoning :nodes [node]}))))
    []
    trail))

(defn- group-consecutive-file-ops
  "Collapse runs of 3+ consecutive file-op tool cards into group cards."
  [blocks]
  (loop [bs blocks result []]
    (if (empty? bs)
      result
      (let [b (first bs)]
        (if (and (= :tool-card (:block-type b))
                 (file-op-tool? (:tool-name b)))
          ;; Count consecutive file-op cards
          (let [run (take-while #(and (= :tool-card (:block-type %))
                                      (file-op-tool? (:tool-name %)))
                                bs)
                n (count run)]
            (if (>= n 3)
              (recur (drop n bs)
                     (conj result {:block-type :tool-group
                                   :cards (vec run)
                                   :count n}))
              (recur (rest bs) (conj result b))))
          (recur (rest bs) (conj result b)))))))

(defn trail->chat-nodes
  "Convert trail to rt-node children for the chat pane.
   Each logical block becomes an rt-node with typed visual treatment.
   Returns [rt-node ...] — children for the chat body container."
  [trail pane-w font-size char-advance shimmer-alpha collapsed]
  (let [colors (:colors dt)
        fg (:fg colors)
        fg-dim (:fg-muted colors)
        border (:border colors)
        pad 12
        max-chars (max 20 (int (/ (- pane-w (* 2 pad)) char-advance)))
        line-h (+ font-size 4)
        card-h-header 28
        blocks (-> trail group-trail-blocks group-consecutive-file-ops)]
    (vec
      (map-indexed
        (fn [bi block]
          (case (:block-type block)
            ;; --- Reasoning: markdown-formatted text ---
            :reasoning
            (let [merged-text (apply str (map :text (:nodes block)))
                  md-blocks (parse-md-blocks merged-text)
                  inner-w (- pane-w (* 2 pad))
                  inner-max-chars (max 20 (int (/ inner-w char-advance)))
                  code-bg (get-in dt [:colors :bg-muted])
                  code-pad 8
                  code-max-chars (max 20 (int (/ (- inner-w (* 2 code-pad)) char-advance)))
                  block-gap 8
                  ;; Build child nodes for each markdown block
                  children
                  (vec
                    (map-indexed
                      (fn [mi mb]
                        (case (:type mb)
                          :header
                          (let [level (or (:level mb) 2)
                                hdr-size (if (<= level 2) (:size typo-title) (:size typo-subtitle))
                                hdr-color {:r 0.90 :g 0.89 :b 0.89 :a 1.0}
                                hdr-max (max 20 (int (/ inner-w (* hdr-size 0.56))))
                                text (:content mb)
                                wrapped (wrap-line text hdr-max)
                                hdr-line-h (+ hdr-size 5)
                                text-h (* (count wrapped) hdr-line-h)
                                ;; Top margin + text + bottom accent + gap
                                top-margin (if (<= level 2) 10 6)
                                bottom-pad 6
                                h (+ top-margin text-h bottom-pad)
                                text-ops (vec (map-indexed
                                               (fn [i ln]
                                                 {:text ln :type :keyword
                                                  :from 0 :to (count ln)
                                                  :x pad :y (+ top-margin hdr-size (* i hdr-line-h))
                                                  :size hdr-size
                                                  :r (:r hdr-color) :g (:g hdr-color)
                                                  :b (:b hdr-color) :a (:a hdr-color)})
                                               wrapped))
                                ;; Subtle bottom border for h1/h2
                                accent-line (when (<= level 2)
                                              (rt-node (keyword (str "md-hdr-line-" bi "-" mi)) :hdr-accent
                                                {:x pad :y (- h 2) :w (min (* (count (first wrapped)) (* hdr-size 0.56)) inner-w) :h 1}
                                                :style {:bg (:border-subtle colors)}))]
                            (rt-node (keyword (str "md-hdr-" bi "-" mi)) :md-header
                              {:x 0 :y 0 :w pane-w :h h}
                              :text text-ops
                              :children (if accent-line [accent-line] [])))

                          :paragraph
                          (let [spans (parse-md-inline-spans (:content mb))
                                wrapped-lines (wrap-md-spans spans inner-max-chars)
                                all-ops (vec (apply concat
                                              (map-indexed
                                                (fn [li line-spans]
                                                  (spans->text-ops line-spans pad
                                                                   (+ font-size (* li line-h))
                                                                   font-size char-advance md-style-colors))
                                                wrapped-lines)))
                                h (+ 4 (* (count wrapped-lines) line-h))]
                            (rt-node (keyword (str "md-para-" bi "-" mi)) :md-paragraph
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :code-block
                          (let [code-color {:r 0.00 :g 0.63 :b 0.32 :a 1.0}
                                code-lines (:lines mb)
                                wrapped-lines (vec (mapcat #(wrap-line % code-max-chars) code-lines))
                                text-ops (vec (map-indexed
                                               (fn [i ln]
                                                 {:text ln :type :comment
                                                  :from 0 :to (count ln)
                                                  :x (+ pad code-pad) :y (+ code-pad font-size (* i line-h))
                                                  :size font-size
                                                  :r (:r code-color) :g (:g code-color)
                                                  :b (:b code-color) :a (:a code-color)})
                                               wrapped-lines))
                                body-h (+ (* 2 code-pad) (* (count wrapped-lines) line-h))
                                total-h (+ body-h 4)]
                            (rt-node (keyword (str "md-code-" bi "-" mi)) :md-code-block
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :children
                              [(rt-node (keyword (str "md-code-bg-" bi "-" mi)) :code-bg
                                 {:x pad :y 0 :w inner-w :h body-h}
                                 :style {:bg code-bg :radius 4})
                               (rt-node (keyword (str "md-code-text-" bi "-" mi)) :code-text
                                 {:x 0 :y 0 :w pane-w :h body-h}
                                 :text text-ops)]))

                          :list
                          (let [items (:items mb)
                                bullet-indent 2
                                item-max-chars (max 10 (- inner-max-chars bullet-indent))
                                item-data (mapv (fn [item]
                                                  (let [spans (parse-md-inline-spans (:content item))
                                                        wrapped (wrap-md-spans spans item-max-chars)]
                                                    {:wrapped wrapped}))
                                                items)
                                all-ops (loop [items-rem item-data li 0 ops []]
                                          (if (empty? items-rem)
                                            ops
                                            (let [{:keys [wrapped]} (first items-rem)
                                                  item-ops
                                                  (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (let [bullet-ops (when (= wi 0)
                                                                          [{:text "- " :type :comment
                                                                            :from 0 :to 2
                                                                            :x pad :y (+ font-size (* (+ li wi) line-h))
                                                                            :size font-size
                                                                            :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                              span-ops (spans->text-ops
                                                                         line-spans
                                                                         (+ pad (* bullet-indent char-advance))
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors)]
                                                          (into (vec (or bullet-ops [])) span-ops)))
                                                      wrapped)))]
                                              (recur (rest items-rem) (+ li (count wrapped)) (into ops item-ops)))))
                                total-lines (reduce + 0 (map (comp count :wrapped) item-data))
                                h (+ 4 (* total-lines line-h))]
                            (rt-node (keyword (str "md-list-" bi "-" mi)) :md-list
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :hr
                          (let [rule-h 8
                                border-c (:border-subtle colors)]
                            (rt-node (keyword (str "md-hr-" bi "-" mi)) :md-hr
                              {:x 0 :y 0 :w pane-w :h rule-h}
                              :children
                              [(rt-node (keyword (str "md-hr-line-" bi "-" mi)) :hr-line
                                 {:x pad :y 3 :w inner-w :h 1}
                                 :style {:bg border-c})]))

                          :numbered-list
                          (let [items (:items mb)
                                item-data (mapv (fn [idx item]
                                                  (let [prefix (str (inc idx) ". ")
                                                        prefix-w (count prefix)
                                                        item-max (max 10 (- inner-max-chars prefix-w))
                                                        spans (parse-md-inline-spans (:content item))
                                                        wrapped (wrap-md-spans spans item-max)]
                                                    {:wrapped wrapped :prefix prefix :prefix-w prefix-w}))
                                                (range) items)
                                all-ops (loop [items-rem item-data li 0 ops []]
                                          (if (empty? items-rem)
                                            ops
                                            (let [{:keys [wrapped prefix prefix-w]} (first items-rem)
                                                  item-ops
                                                  (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (let [num-ops (when (= wi 0)
                                                                        [{:text prefix :type :comment
                                                                          :from 0 :to (count prefix)
                                                                          :x pad :y (+ font-size (* (+ li wi) line-h))
                                                                          :size font-size
                                                                          :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                              span-ops (spans->text-ops
                                                                         line-spans
                                                                         (+ pad (* prefix-w char-advance))
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors)]
                                                          (into (vec (or num-ops [])) span-ops)))
                                                      wrapped)))]
                                              (recur (rest items-rem) (+ li (count wrapped)) (into ops item-ops)))))
                                total-lines (reduce + 0 (map (comp count :wrapped) item-data))
                                h (+ 4 (* total-lines line-h))]
                            (rt-node (keyword (str "md-nlist-" bi "-" mi)) :md-numbered-list
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :callout
                          (let [label (:label mb)
                                body-blocks (:body mb)
                                accent-c (:accent colors)
                                label-c {:r 0.70 :g 0.80 :b 1.0 :a 1.0}
                                callout-pad (+ pad 10)
                                callout-w (- pane-w callout-pad pad)
                                callout-max (max 20 (int (/ callout-w char-advance)))
                                ;; Header node
                                hdr-h (+ font-size 6)
                                hdr-node (rt-node (keyword (str "md-co-hdr-" bi "-" mi)) :callout-hdr
                                           {:x 0 :y 0 :w pane-w :h hdr-h}
                                           :text [{:text label :type :keyword
                                                   :from 0 :to (count label)
                                                   :x callout-pad :y (+ font-size 2)
                                                   :size font-size
                                                   :r (:r label-c) :g (:g label-c)
                                                   :b (:b label-c) :a (:a label-c)}])
                                ;; Body content nodes — reuse the same rendering logic
                                body-children
                                (vec (map-indexed
                                  (fn [ci cb]
                                    (case (:type cb)
                                      :paragraph
                                      (let [spans (parse-md-inline-spans (:content cb))
                                            wrapped-lines (wrap-md-spans spans callout-max)
                                            ops (vec (apply concat
                                                      (map-indexed
                                                        (fn [li ls]
                                                          (spans->text-ops ls callout-pad
                                                                           (+ font-size (* li line-h))
                                                                           font-size char-advance md-style-colors))
                                                        wrapped-lines)))
                                            h (+ 4 (* (count wrapped-lines) line-h))]
                                        (rt-node (keyword (str "md-co-p-" bi "-" mi "-" ci)) :callout-para
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      :list
                                      (let [items (:items cb)
                                            bullet-indent 2
                                            item-mc (max 10 (- callout-max bullet-indent))
                                            item-d (mapv (fn [item]
                                                           {:wrapped (wrap-md-spans (parse-md-inline-spans (:content item)) item-mc)})
                                                         items)
                                            ops (loop [ir item-d li 0 o []]
                                                  (if (empty? ir) o
                                                    (let [{:keys [wrapped]} (first ir)
                                                          io (vec (apply concat
                                                               (map-indexed
                                                                 (fn [wi ls]
                                                                   (let [bp (when (= wi 0)
                                                                              [{:text "- " :type :comment :from 0 :to 2
                                                                                :x callout-pad :y (+ font-size (* (+ li wi) line-h))
                                                                                :size font-size :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                                         sp (spans->text-ops ls (+ callout-pad (* bullet-indent char-advance))
                                                                              (+ font-size (* (+ li wi) line-h))
                                                                              font-size char-advance md-style-colors)]
                                                                     (into (vec (or bp [])) sp)))
                                                                 wrapped)))]
                                                      (recur (rest ir) (+ li (count wrapped)) (into o io)))))
                                            tl (reduce + 0 (map (comp count :wrapped) item-d))
                                            h (+ 4 (* tl line-h))]
                                        (rt-node (keyword (str "md-co-l-" bi "-" mi "-" ci)) :callout-list
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      :numbered-list
                                      (let [items (:items cb)
                                            item-d (mapv (fn [idx item]
                                                           (let [pfx (str (inc idx) ". ")
                                                                 pw (count pfx)]
                                                             {:wrapped (wrap-md-spans (parse-md-inline-spans (:content item))
                                                                         (max 10 (- callout-max pw)))
                                                              :prefix pfx :prefix-w pw}))
                                                         (range) items)
                                            ops (loop [ir item-d li 0 o []]
                                                  (if (empty? ir) o
                                                    (let [{:keys [wrapped prefix prefix-w]} (first ir)
                                                          io (vec (apply concat
                                                               (map-indexed
                                                                 (fn [wi ls]
                                                                   (let [np (when (= wi 0)
                                                                              [{:text prefix :type :comment :from 0 :to (count prefix)
                                                                                :x callout-pad :y (+ font-size (* (+ li wi) line-h))
                                                                                :size font-size :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                                         sp (spans->text-ops ls (+ callout-pad (* prefix-w char-advance))
                                                                              (+ font-size (* (+ li wi) line-h))
                                                                              font-size char-advance md-style-colors)]
                                                                     (into (vec (or np [])) sp)))
                                                                 wrapped)))]
                                                      (recur (rest ir) (+ li (count wrapped)) (into o io)))))
                                            tl (reduce + 0 (map (comp count :wrapped) item-d))
                                            h (+ 4 (* tl line-h))]
                                        (rt-node (keyword (str "md-co-n-" bi "-" mi "-" ci)) :callout-nlist
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      ;; Other block types inside callout — render as paragraph
                                      (let [content (or (:content cb) "")
                                            spans (parse-md-inline-spans content)
                                            wrapped-lines (wrap-md-spans spans callout-max)
                                            ops (vec (apply concat
                                                      (map-indexed
                                                        (fn [li ls]
                                                          (spans->text-ops ls callout-pad
                                                                           (+ font-size (* li line-h))
                                                                           font-size char-advance md-style-colors))
                                                        wrapped-lines)))
                                            h (+ 4 (* (count wrapped-lines) line-h))]
                                        (rt-node (keyword (str "md-co-x-" bi "-" mi "-" ci)) :callout-misc
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))))
                                  body-blocks))
                                all-children (into [hdr-node] body-children)
                                body-gap 4
                                content-h (+ (reduce + 0 (map #(get-in % [:bounds :h] 0) all-children))
                                             (* body-gap (max 0 (dec (count all-children)))))
                                total-h (+ content-h 4)]
                            (rt-node (keyword (str "md-callout-" bi "-" mi)) :md-callout
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :layout {:direction :column :gap body-gap :padding [0 0 0 0]}
                              :children
                              (into [(rt-node (keyword (str "md-co-bar-" bi "-" mi)) :accent-bar
                                       {:x pad :y 2 :w 3 :h (- total-h 4)}
                                       :data {:layout-skip? true}
                                       :style {:bg accent-c :radius 2})]
                                    all-children)))

                          :table
                          (let [table-lines (:lines mb)
                                ;; Filter separator rows (|---|---|)
                                is-separator? #(boolean (re-find #"^\|[\s\-:|\+]+\|$" %))
                                content-rows (filterv (complement is-separator?) table-lines)
                                ;; Parse each row: split by |, trim cells
                                parse-row (fn [row-str]
                                            (->> (str/split row-str #"\|")
                                                 (map str/trim)
                                                 (filterv #(seq %))))
                                rows (mapv parse-row content-rows)
                                header-row (first rows)
                                data-rows (rest rows)
                                ;; Render each data row as "Name — value — value" with inline md
                                table-pad (+ pad code-pad)
                                table-max (max 20 (int (/ (- inner-w (* 2 code-pad)) char-advance)))
                                ;; Build text-ops: header row bold, data rows with inline parsing
                                all-ops
                                (loop [rs (cons {:cells header-row :is-header true}
                                                (map #(hash-map :cells % :is-header false) data-rows))
                                       li 0 ops []]
                                  (if (empty? rs)
                                    ops
                                    (let [{:keys [cells is-header]} (first rs)
                                          row-text (str/join "  |  " cells)
                                          spans (if is-header
                                                  [{:text row-text :style :bold}]
                                                  (parse-md-inline-spans row-text))
                                          wrapped (wrap-md-spans spans table-max)
                                          row-ops (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (spans->text-ops line-spans table-pad
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors))
                                                      wrapped)))]
                                      (recur (rest rs) (+ li (count wrapped)) (into ops row-ops)))))
                                total-lines (+ (if header-row
                                                 (count (wrap-md-spans [{:text (str/join "  |  " header-row) :style :bold}] table-max))
                                                 0)
                                               (reduce + 0
                                                 (map (fn [cells]
                                                        (count (wrap-md-spans
                                                                 (parse-md-inline-spans (str/join "  |  " cells))
                                                                 table-max)))
                                                      data-rows)))
                                body-h (+ (* 2 code-pad) (* total-lines line-h))
                                total-h (+ body-h 4)]
                            (rt-node (keyword (str "md-table-" bi "-" mi)) :md-table
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :children
                              [(rt-node (keyword (str "md-table-bg-" bi "-" mi)) :table-bg
                                 {:x pad :y 0 :w inner-w :h body-h}
                                 :style {:bg (get-in dt [:colors :bg-subtle]) :radius 4})
                               (rt-node (keyword (str "md-table-text-" bi "-" mi)) :table-text
                                 {:x 0 :y 0 :w pane-w :h body-h}
                                 :text all-ops)]))

                          ;; Fallback for unknown block types
                          (rt-node (keyword (str "md-unk-" bi "-" mi)) :md-unknown
                            {:x 0 :y 0 :w pane-w :h 0})))
                      md-blocks))
                  ;; Compute total height including gaps between blocks
                  total-h (+ (reduce + 0 (map #(get-in % [:bounds :h] 0) children))
                             (* block-gap (max 0 (dec (count children)))))]
              (rt-node (keyword (str "reasoning-" bi)) :reasoning-block
                {:x 0 :y 0 :w pane-w :h total-h}
                :layout {:direction :column :gap block-gap}
                :children children))

            ;; --- Thinking: dimmed block with left accent bar ---
            :thinking
            (let [merged-text (apply str (map :text (:nodes block)))
                  tid (str "thinking-" bi)
                  collapsed? (contains? collapsed (keyword tid))
                  header-text "Thinking..."
                  c (trail-node-color :thinking nil)
                  accent-color [0.45 0.55 0.75 0.5]
                  header-op {:text header-text :type :comment
                             :from 0 :to (count header-text)
                             :x (+ pad 8) :y (+ font-size 0)
                             :size font-size
                             :r (:r c) :g (:g c) :b (:b c) :a (:a c)}
                  arrow-text (if collapsed? ">" "v")
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 0)
                            :size font-size
                            :r (:r c) :g (:g c) :b (:b c) :a 0.5}]
              (if collapsed?
                ;; Collapsed thinking — just header
                (rt-node (keyword tid) :thinking-block
                  {:x 0 :y 0 :w pane-w :h (+ card-h-header 4)}
                  :data {:collapse-id (keyword tid)}
                  :children
                  [(rt-node (keyword (str tid "-accent")) :accent-bar
                     {:x 2 :y 2 :w 3 :h (- card-h-header 0)}
                     :style {:bg accent-color :radius 2})
                   (rt-node (keyword (str tid "-hdr")) :tool-header
                     {:x 0 :y 0 :w pane-w :h card-h-header}
                     :data {:collapse-id (keyword tid)}
                     :text [header-op arrow-op])])
                ;; Expanded thinking — header + body
                (let [lines (mapcat #(wrap-line % (- max-chars 2))
                                    (str/split-lines merged-text))
                      body-ops (vec (map-indexed
                                      (fn [i line]
                                        {:text line :type :comment
                                         :from 0 :to (count line)
                                         :x (+ pad 8) :y (+ font-size (* i line-h))
                                         :size font-size
                                         :r (:r c) :g (:g c) :b (:b c) :a (* (:a c) 0.8)})
                                      lines))
                      body-h (+ 4 (* (count lines) line-h))
                      total-h (+ card-h-header body-h 4)]
                  (rt-node (keyword tid) :thinking-block
                    {:x 0 :y 0 :w pane-w :h total-h}
                    :data {:collapse-id (keyword tid)}
                    :children
                    [(rt-node (keyword (str tid "-accent")) :accent-bar
                       {:x 2 :y 2 :w 3 :h (- total-h 4)}
                       :style {:bg accent-color :radius 2})
                     (rt-node (keyword (str tid "-hdr")) :tool-header
                       {:x 0 :y 0 :w pane-w :h card-h-header}
                       :data {:collapse-id (keyword tid)}
                       :text [header-op arrow-op])
                     (rt-node (keyword (str tid "-body")) :thinking-body
                       {:x 0 :y card-h-header :w pane-w :h body-h}
                       :text body-ops)]))))

            ;; --- Tool card: status dot + header + collapsible result ---
            :tool-card
            (let [tool-name (:tool-name block)
                  tid (or (:tool-id block) (str "tool-" bi))
                  status (:status block)
                  pending? (= :pending status)
                  input (:input block)
                  ;; Navigation target: extract file path + line from tool input
                  nav-target (when input
                               (let [fp (or (:file_path input) (:path input))]
                                 (when (and (string? fp) (seq fp))
                                   {:file-path fp
                                    :line (or (:offset input) (:line input) 0)})))
                  summary (if input (tool-input-summary tool-name input)
                                    (some-> (:nodes block) first :tool-name (str "...")))
                  header-label (str tool-name (when (seq summary) (str "  " summary)))
                  header-label (if (> (count header-label) (- max-chars 6))
                                 (str (subs header-label 0 (- max-chars 9)) "...")
                                 header-label)
                  collapsed? (contains? collapsed (keyword tid))
                  c (trail-node-color :tool-call tool-name)
                  ;; Status dot: yellow = pending, green = complete
                  dot-color (if pending?
                              [0.95 0.85 0.25 (if pending? shimmer-alpha 1.0)]
                              [0.4 0.85 0.45 1.0])
                  ;; Header text alpha pulses with shimmer when pending
                  text-alpha (if pending? shimmer-alpha (:a c))
                  header-text-op {:text header-label :type :keyword
                                  :from 0 :to (count header-label)
                                  :x (+ pad 14) :y (+ font-size 2)
                                  :size font-size
                                  :r (:r c) :g (:g c) :b (:b c) :a text-alpha}
                  arrow-text (if collapsed? ">" "v")
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 2)
                            :size font-size
                            :r (nth fg-dim 0) :g (nth fg-dim 1) :b (nth fg-dim 2) :a 0.5}
                  ;; Result body
                  result-content (:result-content block)
                  has-body? (and result-content (not collapsed?))
                  body-lines (when has-body?
                               (let [raw (if (string? result-content) result-content (pr-str result-content))
                                     all-lines (mapcat #(wrap-line % (- max-chars 2))
                                                       (str/split-lines raw))
                                     ;; Truncate to 2 lines max
                                     limited (take 2 all-lines)]
                                 (vec limited)))
                  body-h (if has-body? (+ 4 (* (count body-lines) line-h)) 0)
                  total-h (+ card-h-header body-h 6)
                  card-bg (:bg-subtle colors)]
              (rt-node (keyword (str "tc-" bi)) :tool-card-node
                {:x 0 :y 0 :w pane-w :h total-h}
                :style {:bg card-bg :radius 4
                        :border-width 1
                        :border-color (if nav-target (:border colors) (:border-subtle colors))}
                :data (when nav-target {:nav nav-target})
                :children
                (cond-> [(rt-node (keyword (str "dot-" bi)) :status-dot
                           {:x (+ pad 2) :y 10 :w 6 :h 6}
                           :style {:bg dot-color :radius 3})
                         (rt-node (keyword (str "tch-" bi)) :tool-header
                           {:x 0 :y 0 :w pane-w :h card-h-header}
                           :data {:collapse-id (keyword tid)}
                           :text [header-text-op arrow-op])]
                  has-body?
                  (conj (rt-node (keyword (str "tcb-" bi)) :tool-body
                          {:x 0 :y card-h-header :w pane-w :h body-h}
                          :text (vec (map-indexed
                                       (fn [i line]
                                         (let [rc (trail-node-color :tool-result nil)]
                                           {:text (str "  " line) :type :comment
                                            :from 0 :to (+ 2 (count line))
                                            :x pad :y (+ font-size (* i line-h))
                                            :size font-size
                                            :r (:r rc) :g (:g rc) :b (:b rc) :a (:a rc)}))
                                       body-lines)))))))

            ;; --- Tool group: collapsed run of 3+ file ops ---
            :tool-group
            (let [n (:count block)
                  cards (:cards block)
                  group-id (str "tg-" bi)
                  collapsed? (contains? collapsed (keyword group-id))
                  header-label (str n " file operations")
                  c {:r 0.55 :g 0.75 :b 0.65 :a 1.0}
                  arrow-text (if collapsed? ">" "v")
                  header-text-op {:text header-label :type :keyword
                                  :from 0 :to (count header-label)
                                  :x (+ pad 4) :y (+ font-size 2)
                                  :size font-size
                                  :r (:r c) :g (:g c) :b (:b c) :a (:a c)}
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 2)
                            :size font-size
                            :r (:r c) :g (:g c) :b (:b c) :a 0.5}]
              (if collapsed?
                (rt-node (keyword group-id) :tool-group-node
                  {:x 0 :y 0 :w pane-w :h (+ card-h-header 6)}
                  :style {:bg (:bg-subtle colors) :radius 4
                          :border-width 1 :border-color (:border-subtle colors)}
                  :children
                  [(rt-node (keyword (str group-id "-hdr")) :tool-header
                     {:x 0 :y 0 :w pane-w :h card-h-header}
                     :data {:collapse-id (keyword group-id)}
                     :text [header-text-op arrow-op])])
                ;; Expanded: show each card as a summary line
                (let [item-lines
                      (vec (map-indexed
                             (fn [i card]
                               (let [tn (:tool-name card)
                                     inp (:input card)
                                     summary (if inp (tool-input-summary tn inp) "")
                                     label (str ">> " tn " " summary)
                                     label (if (> (count label) max-chars)
                                             (str (subs label 0 (- max-chars 3)) "...")
                                             label)
                                     tc (trail-node-color :tool-call tn)]
                                 {:text label :type :keyword
                                  :from 0 :to (count label)
                                  :x (+ pad 4) :y (+ font-size (* i line-h))
                                  :size font-size
                                  :r (:r tc) :g (:g tc) :b (:b tc) :a (:a tc)}))
                             cards))
                      body-h (+ 4 (* n line-h))
                      total-h (+ card-h-header body-h 6)]
                  (rt-node (keyword group-id) :tool-group-node
                    {:x 0 :y 0 :w pane-w :h total-h}
                    :style {:bg (:bg-subtle colors) :radius 4
                            :border-width 1 :border-color (:border-subtle colors)}
                    :children
                    [(rt-node (keyword (str group-id "-hdr")) :tool-header
                       {:x 0 :y 0 :w pane-w :h card-h-header}
                       :data {:collapse-id (keyword group-id)}
                       :text [header-text-op arrow-op])
                     (rt-node (keyword (str group-id "-body")) :tool-group-body
                       {:x 0 :y card-h-header :w pane-w :h body-h}
                       :text item-lines)]))))

            ;; Fallback — render as reasoning
            (let [text (pr-str block)
                  lines (wrap-line text max-chars)
                  c {:r 0.75 :g 0.75 :b 0.75 :a 1.0}
                  text-ops (vec (map-indexed
                                  (fn [i line]
                                    {:text line :type :comment
                                     :from 0 :to (count line)
                                     :x pad :y (+ font-size (* i line-h))
                                     :size font-size
                                     :r (:r c) :g (:g c) :b (:b c) :a (:a c)})
                                  lines))
                  h (+ 4 (* (count lines) line-h))]
              (rt-node (keyword (str "unknown-" bi)) :reasoning-block
                {:x 0 :y 0 :w pane-w :h h}
                :text text-ops))))
        blocks))))

(defn trail->display-lines
  "Convert trail nodes to [{:text :color}]. Merges consecutive reasoning nodes."
  [trail]
  (reduce
    (fn [acc node]
      (case (:kind node)
        :reasoning
        (let [last-entry (peek acc)]
          (if (and last-entry (= :reasoning (:kind last-entry)))
            ;; Merge with previous reasoning node
            (conj (pop acc) (update last-entry :text str (:text node)))
            (conj acc {:kind :reasoning :text (:text node)
                       :color (trail-node-color :reasoning nil)})))

        :thinking
        (let [last-entry (peek acc)]
          (if (and last-entry (= :thinking (:kind last-entry)))
            (conj (pop acc) (update last-entry :text str (:text node)))
            (conj acc {:kind :thinking :text (:text node)
                       :color (trail-node-color :thinking nil)})))

        :tool-call
        (let [input-summary (let [inp (:input node)]
                              (cond
                                (and (string? (:file_path inp)) (seq (:file_path inp)))
                                (:file_path inp)
                                (and (string? (:pattern inp)) (seq (:pattern inp)))
                                (str "\"" (:pattern inp) "\"")
                                (and (string? (:command inp)) (seq (:command inp)))
                                (let [cmd (:command inp)]
                                  (if (> (count cmd) 60)
                                    (str (subs cmd 0 57) "...")
                                    cmd))
                                :else ""))]
          (conj acc {:kind :tool-call
                     :text (str ">> " (:tool-name node) " " input-summary)
                     :color (trail-node-color :tool-call (:tool-name node))}))

        :tool-call-start
        (conj acc {:kind :tool-call-start
                   :text (str "> " (:tool-name node) "...")
                   :color (trail-node-color :tool-call-start nil)})

        :tool-result
        (let [raw-content (or (:content node) "")
              content (if (string? raw-content) raw-content (pr-str raw-content))
              short (if (> (count content) 120)
                      (str (subs content 0 117) "...")
                      content)]
          (conj acc {:kind :tool-result
                     :text (str "  <- " short)
                     :color (trail-node-color :tool-result nil)}))

        ;; Unknown kind — render as-is
        (conj acc {:kind (:kind node) :text (pr-str node)
                   :color {:r 0.75 :g 0.75 :b 0.75 :a 1.0}})))
    []
    trail))

(defn agent-wrapped-line-count
  "Count wrapped display lines for agent output. Trail-aware: uses structured trail
   when available, falls back to flat :output text. Includes header line in count."
  [agent-output max-chars]
  (let [status (:status agent-output)
        provider-name (some-> (:provider agent-output) name str/upper-case)
        prompt (:prompt agent-output)
        header-text (when status (str "[" provider-name "] " (name status) ": " prompt))
        trail (:trail agent-output)
        display-entries (when (seq trail) (trail->display-lines trail))
        raw-lines (if display-entries
                    ;; Trail path: header + structured trail lines
                    (cond-> []
                      header-text (conj {:text header-text})
                      (and (= status :running) (empty? display-entries)) (conj {:text "..."})
                      (seq display-entries) (into display-entries))
                    ;; Flat text fallback
                    (let [output-lines (str/split-lines (or (:output agent-output) ""))
                          flat-lines (cond-> []
                                       header-text (conj header-text)
                                       (and (= status :running) (empty? output-lines)) (conj "...")
                                       (seq output-lines) (into output-lines))]
                      (mapv (fn [l] {:text l}) flat-lines)))]
    (count (into [] (mapcat (fn [entry]
                              (let [nl-lines (str/split-lines (or (:text entry) ""))]
                                (mapcat #(wrap-line % max-chars) nl-lines))))
                    raw-lines))))

(defn compute-agent-panel-h
  "Pure: dynamic panel height from agent output content.
   Returns 0 when no agent output, otherwise sizes to content capped at 50% viewport.
   Wraps lines to viewport width for accurate height. Trail-aware."
  [agent-output font-size viewport-height viewport-width char-advance]
  (if-not (some? (:status agent-output))
    0
    (let [agent-x 24
          right-pad 24
          available-w (- viewport-width agent-x right-pad)
          max-chars (if (pos? char-advance) (max 1 (int (/ available-w char-advance))) 80)
          line-count (agent-wrapped-line-count agent-output max-chars)
          line-step (* font-size 1.2)
          content-h (+ 16 (* line-count line-step))
          max-h (* viewport-height 0.5)]
      (min content-h max-h))))
