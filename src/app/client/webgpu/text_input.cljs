(ns app.client.webgpu.text-input
  (:require [clojure.string :as str]))

(defn- clamp [v min-v max-v]
  (-> v (max min-v) (min max-v)))

(defn- whitespace? [ch]
  (boolean (re-matches #"\s" (str ch))))

(defn- word-char? [ch]
  (boolean (re-matches #"\w" (str ch))))

(defn- normalize-single-selection [selection text-len]
  (when (and selection (number? (:start selection)) (number? (:end selection)))
    (let [start (clamp (int (:start selection)) 0 text-len)
          end   (clamp (int (:end selection)) 0 text-len)
          [s e] (if (<= start end) [start end] [end start])]
      (when (not= s e)
        {:start s :end e}))))

(defn- normalize-pos [lines pos]
  (let [line-count (count lines)
        line (clamp (int (or (:line pos) 0)) 0 (max 0 (dec line-count)))
        line-text (get lines line "")
        col (clamp (int (or (:col pos) 0)) 0 (count line-text))]
    {:line line :col col}))

(defn- pos<= [a b]
  (or (< (:line a) (:line b))
      (and (= (:line a) (:line b))
           (<= (:col a) (:col b)))))

(defn- normalize-multi-selection [selection lines]
  (when (and selection (map? (:start selection)) (map? (:end selection)))
    (let [start (normalize-pos lines (:start selection))
          end   (normalize-pos lines (:end selection))
          [s e] (if (pos<= start end) [start end] [end start])]
      (when (not (and (= (:line s) (:line e))
                      (= (:col s) (:col e))))
        {:start s :end e}))))

(defn- normalize-single [state]
  (let [text (or (:text state) "")
        len (count text)
        cursor (clamp (int (or (:cursor state) 0)) 0 len)
        selection (normalize-single-selection (:selection state) len)]
    (assoc state :text text :cursor cursor :selection selection)))

(defn- normalize-multi [state]
  (let [lines (vec (or (:lines state) [""]))
        lines (if (seq lines) lines [""])
        cursor (normalize-pos lines (or (:cursor state) {:line 0 :col 0}))
        desired (or (:desired-col state) (:col cursor))
        selection (normalize-multi-selection (:selection state) lines)]
    (assoc state
           :lines lines
           :cursor cursor
           :desired-col desired
           :selection selection)))

(defn- resolve-line-lengths [lines line-lengths]
  (if (and line-lengths (= (count line-lengths) (count lines)))
    line-lengths
    (mapv count lines)))

(declare delete-selection)

;; === Character Operations ===

(defn insert-char
  "Insert character at cursor position. Works for single or multi-line."
  [state ch multi-line?]
  (if (nil? ch)
    state
    (let [ch-str (str ch)]
      (if (zero? (count ch-str))
        state
        (if multi-line?
          (let [state (normalize-multi state)
                state (if (:selection state) (delete-selection state true) state)
                {:keys [lines cursor]} state
                line-idx (:line cursor)
                col (:col cursor)
                current-line (get lines line-idx "")
                before (subs current-line 0 col)
                after  (subs current-line col)
                parts (str/split ch-str #"\n" -1)
                part-count (count parts)]
            (if (= part-count 1)
              (let [new-line (str before (first parts) after)
                    new-lines (assoc lines line-idx new-line)
                    new-col (+ col (count (first parts)))]
                (assoc state :lines new-lines
                             :cursor {:line line-idx :col new-col}
                             :desired-col new-col
                             :selection nil))
              (let [first-line (str before (first parts))
                    last-line (str (last parts) after)
                    middle (subvec (vec parts) 1 (dec part-count))
                    new-lines (vec (concat (subvec lines 0 line-idx)
                                           [first-line]
                                           middle
                                           [last-line]
                                           (subvec lines (inc line-idx))))
                    new-line-idx (+ line-idx (dec part-count))
                    new-col (count (last parts))]
                (assoc state :lines new-lines
                             :cursor {:line new-line-idx :col new-col}
                             :desired-col new-col
                             :selection nil))))
          (let [state (normalize-single state)
                state (if (:selection state) (delete-selection state false) state)
                {:keys [text cursor]} state
                before (subs text 0 cursor)
                after  (subs text cursor)
                new-text (str before ch-str after)
                new-cursor (+ cursor (count ch-str))]
            (assoc state :text new-text :cursor new-cursor :selection nil)))))))

(defn delete-backward
  "Delete character before cursor (backspace)."
  [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)]
      (if (:selection state)
        (delete-selection state true)
        (let [{:keys [lines cursor]} state
              line-idx (:line cursor)
              col (:col cursor)
              current-line (get lines line-idx "")]
          (cond
            (> col 0)
            (let [before (subs current-line 0 (dec col))
                  after  (subs current-line col)
                  new-line (str before after)
                  new-lines (assoc lines line-idx new-line)
                  new-col (dec col)]
              (assoc state :lines new-lines
                           :cursor {:line line-idx :col new-col}
                           :desired-col new-col
                           :selection nil))

            (> line-idx 0)
            (let [prev-line (get lines (dec line-idx) "")
                  prev-len (count prev-line)
                  merged (str prev-line current-line)
                  new-lines (vec (concat (subvec lines 0 (dec line-idx))
                                         [merged]
                                         (subvec lines (inc line-idx))))]
              (assoc state :lines new-lines
                           :cursor {:line (dec line-idx) :col prev-len}
                           :desired-col prev-len
                           :selection nil))

            :else state))))
    (let [state (normalize-single state)]
      (if (:selection state)
        (delete-selection state false)
        (let [{:keys [text cursor]} state]
          (if (> cursor 0)
            (let [before (subs text 0 (dec cursor))
                  after  (subs text cursor)
                  new-text (str before after)
                  new-cursor (dec cursor)]
              (assoc state :text new-text :cursor new-cursor :selection nil))
            state))))))

(defn delete-forward
  "Delete character after cursor (delete key)."
  [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)]
      (if (:selection state)
        (delete-selection state true)
        (let [{:keys [lines cursor]} state
              line-idx (:line cursor)
              col (:col cursor)
              current-line (get lines line-idx "")
              line-len (count current-line)
              max-line (dec (count lines))]
          (cond
            (< col line-len)
            (let [before (subs current-line 0 col)
                  after  (subs current-line (inc col))
                  new-line (str before after)
                  new-lines (assoc lines line-idx new-line)]
              (assoc state :lines new-lines :selection nil))

            (< line-idx max-line)
            (let [next-line (get lines (inc line-idx) "")
                  merged (str current-line next-line)
                  new-lines (vec (concat (subvec lines 0 line-idx)
                                         [merged]
                                         (subvec lines (+ line-idx 2))))]
              (assoc state :lines new-lines :selection nil))

            :else state))))
    (let [state (normalize-single state)]
      (if (:selection state)
        (delete-selection state false)
        (let [{:keys [text cursor]} state
              text-len (count text)]
          (if (< cursor text-len)
            (let [before (subs text 0 cursor)
                  after  (subs text (inc cursor))
                  new-text (str before after)]
              (assoc state :text new-text :selection nil))
            state))))))

;; === Navigation ===

(defn move-cursor
  "Move cursor in direction (:left :right :up :down :home :end)."
  [state direction multi-line? line-lengths]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [lines cursor]} state
          line-lengths (resolve-line-lengths lines line-lengths)
          line (:line cursor)
          col (:col cursor)
          desired (:desired-col state)
          max-line (dec (count line-lengths))
          line-len (get line-lengths line 0)
          [new-pos new-desired]
          (case direction
            :left
            (let [np (if (> col 0)
                       {:line line :col (dec col)}
                       (if (> line 0)
                         (let [prev-len (get line-lengths (dec line) 0)]
                           {:line (dec line) :col prev-len})
                         cursor))]
              [np (:col np)])

            :right
            (let [np (if (< col line-len)
                       {:line line :col (inc col)}
                       (if (< line max-line)
                         {:line (inc line) :col 0}
                         cursor))]
              [np (:col np)])

            :up
            (if (> line 0)
              (let [prev-len (get line-lengths (dec line) 0)]
                [{:line (dec line) :col (min desired prev-len)} desired])
              [cursor desired])

            :down
            (if (< line max-line)
              (let [next-len (get line-lengths (inc line) 0)]
                [{:line (inc line) :col (min desired next-len)} desired])
              [cursor desired])

            :home
            [{:line line :col 0} 0]

            :end
            [{:line line :col line-len} line-len]

            [cursor desired])]
      (assoc state :cursor new-pos :desired-col new-desired :selection nil))
    (let [state (normalize-single state)
          {:keys [text cursor]} state
          text-len (count text)
          new-cursor (case direction
                       :left (max 0 (dec cursor))
                       :right (min text-len (inc cursor))
                       :home 0
                       :end text-len
                       cursor)]
      (assoc state :cursor new-cursor :selection nil))))

(defn move-word
  "Move cursor by word (:left :right)."
  [state direction multi-line? line-lengths]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [lines cursor]} state
          line-lengths (resolve-line-lengths lines line-lengths)
          line-idx (:line cursor)
          col (:col cursor)
          max-line (dec (count line-lengths))
          current-line (get lines line-idx "")
          new-pos
          (case direction
            :left
            (if (= col 0)
              (if (> line-idx 0)
                {:line (dec line-idx) :col (get line-lengths (dec line-idx) 0)}
                cursor)
              (let [before (subs current-line 0 col)
                    skip-ws (loop [i (dec (count before))]
                              (if (and (>= i 0) (whitespace? (nth before i)))
                                (recur (dec i))
                                i))
                    new-col (loop [i skip-ws]
                              (if (and (>= i 0) (word-char? (nth before i)))
                                (recur (dec i))
                                (inc i)))]
                {:line line-idx :col (max 0 new-col)}))

            :right
            (let [line-len (count current-line)]
              (if (= col line-len)
                (if (< line-idx max-line)
                  {:line (inc line-idx) :col 0}
                  cursor)
                (let [after (subs current-line col)
                      skip-word (loop [i 0]
                                  (if (and (< i (count after)) (word-char? (nth after i)))
                                    (recur (inc i))
                                    i))
                      new-col (loop [i skip-word]
                                (if (and (< i (count after)) (whitespace? (nth after i)))
                                  (recur (inc i))
                                  i))]
                  {:line line-idx :col (+ col new-col)})))
            cursor)]
      (assoc state :cursor new-pos :desired-col (:col new-pos) :selection nil))
    (let [state (normalize-single state)
          {:keys [text cursor]} state
          text-len (count text)
          new-cursor
          (case direction
            :left
            (if (zero? cursor)
              0
              (let [before (subs text 0 cursor)
                    skip-ws (loop [i (dec (count before))]
                              (if (and (>= i 0) (whitespace? (nth before i)))
                                (recur (dec i))
                                i))
                    new-col (loop [i skip-ws]
                              (if (and (>= i 0) (word-char? (nth before i)))
                                (recur (dec i))
                                (inc i)))]
                (max 0 new-col)))

            :right
            (if (= cursor text-len)
              text-len
              (let [after (subs text cursor)
                    skip-word (loop [i 0]
                                (if (and (< i (count after)) (word-char? (nth after i)))
                                  (recur (inc i))
                                  i))
                    new-col (loop [i skip-word]
                              (if (and (< i (count after)) (whitespace? (nth after i)))
                                (recur (inc i))
                                i))]
                (+ cursor new-col)))
            cursor)]
      (assoc state :cursor new-cursor :selection nil))))

;; === Selection ===

(defn select-all [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)
          lines (:lines state)
          last-line (max 0 (dec (count lines)))
          last-col (count (get lines last-line ""))]
      (assoc state
             :selection {:start {:line 0 :col 0}
                         :end {:line last-line :col last-col}}
             :cursor {:line last-line :col last-col}
             :desired-col last-col))
    (let [state (normalize-single state)
          text (:text state)
          text-len (count text)]
      (assoc state :selection {:start 0 :end text-len}
                   :cursor text-len))))

(defn get-selected-text [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [selection lines]} state]
      (when selection
        (let [{:keys [start end]} selection]
          (if (= (:line start) (:line end))
            (subs (get lines (:line start) "") (:col start) (:col end))
            (let [first-line (subs (get lines (:line start) "") (:col start))
                  middle (for [i (range (inc (:line start)) (:line end))]
                           (get lines i ""))
                  last-line (subs (get lines (:line end) "") 0 (:col end))]
              (str/join "\n" (concat [first-line] middle [last-line])))))))
    (let [state (normalize-single state)
          {:keys [selection text]} state]
      (when selection
        (subs text (:start selection) (:end selection))))))

(defn delete-selection
  "Delete selected text, return new state with cursor at selection start."
  [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [selection lines]} state]
      (if selection
        (let [{:keys [start end]} selection
              start-line (:line start)
              end-line (:line end)
              start-col (:col start)
              end-col (:col end)]
          (if (= start-line end-line)
            (let [line (get lines start-line "")
                  before (subs line 0 start-col)
                  after  (subs line end-col)
                  new-lines (assoc lines start-line (str before after))]
              (assoc state :lines new-lines
                           :cursor {:line start-line :col start-col}
                           :desired-col start-col
                           :selection nil))
            (let [first-line (subs (get lines start-line "") 0 start-col)
                  last-line (subs (get lines end-line "") end-col)
                  merged (str first-line last-line)
                  new-lines (vec (concat (subvec lines 0 start-line)
                                         [merged]
                                         (subvec lines (inc end-line))))]
              (assoc state :lines new-lines
                           :cursor {:line start-line :col start-col}
                           :desired-col start-col
                           :selection nil))))
        state))
    (let [state (normalize-single state)
          {:keys [selection text]} state]
      (if selection
        (let [start (:start selection)
              end   (:end selection)
              before (subs text 0 start)
              after  (subs text end)
              new-text (str before after)]
          (assoc state :text new-text :cursor start :selection nil))
        state))))

;; === Clipboard ===

(defn cut
  "Cut selection. Returns {:state new-state :text cut-text}."
  [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [selection lines cursor]} state]
      (if selection
        (let [text (get-selected-text state true)
              new-state (delete-selection state true)]
          {:state new-state :text text})
        (let [line-idx (:line cursor)
              line-text (get lines line-idx "")
              line-text (str line-text "\n")]
          (if (= 1 (count lines))
            {:state (assoc state :lines [""]
                                 :cursor {:line 0 :col 0}
                                 :desired-col 0
                                 :selection nil)
             :text line-text}
            (let [new-lines (vec (concat (subvec lines 0 line-idx)
                                         (subvec lines (inc line-idx))))
                  new-line-idx (min line-idx (dec (count new-lines)))]
              {:state (assoc state :lines new-lines
                                   :cursor {:line new-line-idx :col 0}
                                   :desired-col 0
                                   :selection nil)
               :text line-text})))))
    (let [state (normalize-single state)]
      (if (:selection state)
        (let [text (get-selected-text state false)
              new-state (delete-selection state false)]
          {:state new-state :text text})
        {:state state :text nil}))))

(defn copy
  "Copy selection. Returns selected text or current line for multi-line."
  [state multi-line?]
  (if multi-line?
    (let [state (normalize-multi state)
          {:keys [selection lines cursor]} state]
      (if selection
        (get-selected-text state true)
        (let [line-text (get lines (:line cursor) "")]
          (str line-text "\n"))))
    (let [state (normalize-single state)]
      (when (:selection state)
        (get-selected-text state false)))))

(defn paste [state text multi-line?]
  (if (nil? text)
    state
    (let [text-str (str text)]
      (if (zero? (count text-str))
        state
        (if multi-line?
          (let [state (normalize-multi state)
                state (if (:selection state) (delete-selection state true) state)
                {:keys [lines cursor]} state
                line-idx (:line cursor)
                col (:col cursor)
                current-line (get lines line-idx "")
                before (subs current-line 0 col)
                after  (subs current-line col)
                parts (str/split text-str #"\n" -1)
                part-count (count parts)]
            (if (= part-count 1)
              (let [new-line (str before (first parts) after)
                    new-lines (assoc lines line-idx new-line)
                    new-col (+ col (count (first parts)))]
                (assoc state :lines new-lines
                             :cursor {:line line-idx :col new-col}
                             :desired-col new-col
                             :selection nil))
              (let [first-line (str before (first parts))
                    last-line (str (last parts) after)
                    middle (subvec (vec parts) 1 (dec part-count))
                    new-lines (vec (concat (subvec lines 0 line-idx)
                                           [first-line]
                                           middle
                                           [last-line]
                                           (subvec lines (inc line-idx))))
                    new-line-idx (+ line-idx (dec part-count))
                    new-col (count (last parts))]
                (assoc state :lines new-lines
                             :cursor {:line new-line-idx :col new-col}
                             :desired-col new-col
                             :selection nil))))
          (let [state (normalize-single state)
                state (if (:selection state) (delete-selection state false) state)
                {:keys [text cursor]} state
                before (subs text 0 cursor)
                after  (subs text cursor)
                new-text (str before text-str after)
                new-cursor (+ cursor (count text-str))]
            (assoc state :text new-text :cursor new-cursor :selection nil)))))))

;; === Rendering Helpers ===

(defn calculate-caret-rect
  "Calculate caret rectangle for rendering."
  [cursor font-size origin-x origin-y line-h visible?]
  (when (and cursor visible?)
    (let [{:keys [line col]} (if (map? cursor) cursor {:line 0 :col cursor})
          char-w (* font-size 0.6)
          x (+ origin-x (* col char-w))
          y (+ origin-y (* line line-h))]
      {:x x :y y :w 2 :h line-h
       :r 0.9 :g 0.9 :b 0.9 :a 1.0})))

(defn calculate-selection-rects
  "Calculate selection highlight rectangles."
  [selection font-size origin-x origin-y line-h line-lengths]
  (when selection
    (let [{:keys [start end]} selection
          multi-line? (and (map? start) (contains? start :line))
          char-w (* font-size 0.6)
          r 0.2 g 0.4 b 0.9 a 0.5]
      (if (not multi-line?)
        (let [s (min start end)
              e (max start end)
              width-chars (- e s)]
          (when (> width-chars 0)
            [{:x (+ origin-x (* s char-w))
              :y origin-y
              :w (* width-chars char-w)
              :h line-h
              :r r :g g :b b :a a}]))
        (let [line-lengths (or line-lengths [])
              [s e] (if (pos<= start end) [start end] [end start])]
          (keep (fn [line-idx]
                  (let [line-len (get line-lengths line-idx 0)
                        col-start (if (= line-idx (:line s)) (:col s) 0)
                        col-end (if (= line-idx (:line e)) (:col e) line-len)
                        width-chars (- col-end col-start)]
                    (when (> width-chars 0)
                      {:x (+ origin-x (* col-start char-w))
                       :y (+ origin-y (* line-idx line-h))
                       :w (* width-chars char-w)
                       :h line-h
                       :r r :g g :b b :a a})))
                (range (:line s) (inc (:line e)))))))))
