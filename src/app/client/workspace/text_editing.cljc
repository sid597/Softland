(ns app.client.workspace.text-editing
  "Pure T2 input-floor kernel.

   The kernel owns tagged UTF-16 session offsets, edit operations as data,
   grapheme/word motion over injected boundaries, visual-line motion through
   Contract-T readers, IME lifecycle, and exact paste normalization. Browser
   concerns (DOM events, clipboard promises, and painting) stay in the runtime."
  (:require [clojure.string :as str]
            [app.client.workspace.text-layout :as tl]))

(def index-space tl/legacy-index-space)
(def default-paste-budget 100000)

(def pinned-pass-throughs
  "Named classes the live session silences from legacy routing without
   preventing their browser/OS default."
  #{:browser-zoom :function-key :os-shortcut :dead-key :modified-key
    :unidentified-key})

(defn tagged-offset [offset]
  {:index-space index-space :offset (long offset)})

(defn tagged-offset? [value]
  (and (map? value)
       (= index-space (:index-space value))
       (integer? (:offset value))
       (not (neg? (:offset value)))))

(defn offset
  "Fail closed when an untagged or foreign-domain offset reaches the kernel."
  [value]
  (if (tagged-offset? value)
    (long (:offset value))
    (throw (ex-info "T2 requires a tagged UTF-16 source offset"
                    {:value value :required-index-space index-space}))))

(defn- code-unit-at [text i]
  #?(:clj (int (.charAt ^String text i))
     :cljs (.charCodeAt text i)))

(defn surrogate-interior?
  "True only for the illegal boundary between a UTF-16 high/low pair."
  [text n]
  (let [text (str (or text ""))
        n (long n)
        length (tl/code-unit-count text)]
    (and (pos? n)
         (< n length)
         (<= 0xD800 (code-unit-at text (dec n)) 0xDBFF)
         (<= 0xDC00 (code-unit-at text n) 0xDFFF))))

(defn safe-offset?
  [text value]
  (and (tagged-offset? value)
       (let [n (offset value)
             length (tl/code-unit-count (str (or text "")))]
         (and (<= 0 n length)
              (not (surrogate-interior? text n))))))

(defn assert-safe-offset!
  [text value]
  (when-not (safe-offset? text value)
    (throw (ex-info "T2 offset is outside the document or splits a surrogate pair"
                    {:index value :document-code-units (tl/code-unit-count text)})))
  value)

(defn- checked-range
  [text [start end :as source-range]]
  (when-not (and (= 2 (count source-range))
                 (tagged-offset? start)
                 (tagged-offset? end))
    (throw (ex-info "T2 source range requires two tagged offsets"
                    {:source-range source-range})))
  (assert-safe-offset! text start)
  (assert-safe-offset! text end)
  (when (> (offset start) (offset end))
    (throw (ex-info "T2 source range must be ordered"
                    {:source-range source-range})))
  source-range)

(defn initial-state
  [{:keys [text revision caret affinity anchor]
    :or {text "" revision 0 affinity :downstream}}]
  (let [text (str text)
        caret (or caret (tagged-offset 0))
        state {:document {:text text :revision (long revision)}
               :caret {:index caret :affinity (or affinity :downstream)}
               :anchor anchor
               :desired-x nil
               :composition {:status :idle}}]
    (assert-safe-offset! text caret)
    (when anchor (assert-safe-offset! text anchor))
    state))

(defn composing? [state]
  (= :composing (get-in state [:composition :status])))

(defn selection-range
  [state]
  (when-let [anchor (:anchor state)]
    (let [caret (get-in state [:caret :index])
          [a b] (sort [(offset anchor) (offset caret)])]
      (when (< a b)
        [(tagged-offset a) (tagged-offset b)]))))

(defn selected-text
  [state]
  (if-let [source-range (selection-range state)]
    (let [[start end] (mapv offset source-range)]
      (subs (get-in state [:document :text]) start end))
    ""))

(defn- edit-op
  [start end inserted]
  (cond
    (= start end) {:op :insert :at (tagged-offset start) :text inserted}
    (empty? inserted) {:op :delete-range
                       :range [(tagged-offset start) (tagged-offset end)]}
    :else {:op :replace-range
           :range [(tagged-offset start) (tagged-offset end)]
           :text inserted}))

(defn apply-replacement
  "Apply one semantic replacement and return {:state state' :ops [op]}.
   The operation boundary validates both the incoming and resulting caret."
  [state source-range inserted]
  (let [text (get-in state [:document :text])
        [start-index end-index] (checked-range text source-range)
        start (offset start-index)
        end (offset end-index)
        inserted (str (or inserted ""))
        next-text (str (subs text 0 start) inserted (subs text end))
        next-offset (+ start (tl/code-unit-count inserted))
        next-index (tagged-offset next-offset)
        op (edit-op start end inserted)
        next-state (-> state
                       (assoc :document {:text next-text
                                         :revision (inc (get-in state
                                                                [:document :revision]
                                                                0))}
                              :caret {:index next-index :affinity :downstream}
                              :anchor nil
                              :desired-x nil
                              :composition {:status :idle}))]
    (assert-safe-offset! next-text next-index)
    {:state next-state :ops [op] :status :applied}))

(defn replace-input
  "Typing, paste, and IME commit all converge on this one replacement road."
  [state inserted]
  (let [caret (get-in state [:caret :index])]
    (apply-replacement state (or (selection-range state) [caret caret]) inserted)))

(defn- normalize-boundaries
  [text xs]
  (let [length (tl/code-unit-count text)
        values (->> xs (map #(if (tagged-offset? %) (offset %) (long %)))
                    (concat [0 length]) distinct sort vec)]
    (doseq [n values]
      (assert-safe-offset! text (tagged-offset n)))
    values))

(defn normalized-boundaries
  "Validate the injected segmentation provider result against this document."
  [state boundaries]
  (let [text (get-in state [:document :text])]
    {:grapheme (normalize-boundaries text (:grapheme boundaries))
     :word (normalize-boundaries text (:word boundaries))
     :word-spans (mapv (fn [[a b]]
                         (let [[start end] (checked-range
                                           text [(if (tagged-offset? a)
                                                   a (tagged-offset a))
                                                 (if (tagged-offset? b)
                                                   b (tagged-offset b))])]
                           [(offset start) (offset end)]))
                       (:word-spans boundaries))}))

(defn- neighbor-boundary
  [boundaries current direction]
  (case direction
    :left (or (last (filter #(< % current) boundaries)) current)
    :right (or (first (filter #(> % current) boundaries)) current)
    current))

(defn- set-caret
  [state index affinity extend?]
  (let [old-index (get-in state [:caret :index])]
    (cond-> (assoc state :caret {:index index :affinity affinity}
                         :desired-x nil)
      extend? (update :anchor #(or % old-index))
      (not extend?) (assoc :anchor nil))))

(defn move-horizontal
  [state direction unit boundaries extend?]
  (let [text (get-in state [:document :text])
        normalized (normalized-boundaries state boundaries)
        candidates (get normalized unit)
        selected (selection-range state)
        current (offset (get-in state [:caret :index]))
        target (if (and selected (not extend?))
                 (if (= direction :left)
                   (offset (first selected))
                   (offset (second selected)))
                 (neighbor-boundary candidates current direction))
        index (tagged-offset target)
        affinity (if (= direction :left) :upstream :downstream)]
    (assert-safe-offset! text index)
    {:state (set-caret state index affinity extend?)
     :ops [] :status :moved}))

(defn delete-direction
  [state direction boundaries]
  (if-let [source-range (selection-range state)]
    (apply-replacement state source-range "")
    (let [normalized (normalized-boundaries state boundaries)
          candidates (:grapheme normalized)
          current (offset (get-in state [:caret :index]))
          neighbor (neighbor-boundary candidates current direction)
          [start end] (sort [current neighbor])]
      (if (= start end)
        {:state state :ops [] :status :boundary}
        (apply-replacement state [(tagged-offset start) (tagged-offset end)] "")))))

(defn- line-includes-offset?
  [line-data n]
  (let [[start end] (tl/line-source-bounds (:source-range line-data))]
    (<= start n end)))

(defn caret-geometry
  "Read one stored offset+affinity through Contract T. No metric is computed
   here: candidate visual lines are selected by source ownership and every x/y
   comes from tl/caret-result, including injected grapheme-interior stops."
  [layout-result caret boundaries]
  (let [n (offset (:index caret))
        affinity (:affinity caret)
        candidates (->> (:lines layout-result)
                        (map-indexed vector)
                        (filter (fn [[_ line-data]]
                                  (and (not= :header (first (:source-range line-data)))
                                       (line-includes-offset? line-data n))))
                        (sort-by (fn [[line line-data]]
                                   (let [[start end]
                                         (tl/line-source-bounds
                                          (:source-range line-data))]
                                     (cond
                                       (and (= affinity :downstream) (= n start)) -2
                                       (and (= affinity :upstream) (= n end)) -2
                                       :else line)))))]
    (or
     (some (fn [[line line-data]]
             (let [start (first (tl/line-source-bounds (:source-range line-data)))
                   result (tl/caret-result
                           layout-result line (- n start)
                           {:grapheme-boundaries (:grapheme boundaries)
                            :affinity affinity})]
               (when (= n (tl/source-index-offset (:index result))) result)))
           candidates)
     (let [{:keys [line col]} (tl/source-offset->line-col layout-result n)]
       (tl/caret-result layout-result line col
                        {:grapheme-boundaries (:grapheme boundaries)
                         :affinity affinity})))))

(defn move-vertical
  [state direction layout-result boundaries extend?]
  (let [current (caret-geometry layout-result (:caret state) boundaries)
        lines (:lines layout-result)
        delta (if (= direction :up) -1 1)
        target-line (max 0 (min (dec (count lines)) (+ (:line current) delta)))
        desired-x (or (:desired-x state) (first (:position current)))
        target-bounds (get-in lines [target-line :logical-bounds])
        target-y (+ (:y target-bounds) (/ (:h target-bounds) 2.0))
        hit (tl/hit-test-result
             layout-result [desired-x target-y]
             {:grapheme-boundaries (:grapheme boundaries)})
        next-state (set-caret state (:index hit) (:affinity hit) extend?)]
    {:state (assoc next-state :desired-x desired-x)
     :ops [] :status :moved}))

(defn move-line-edge
  [state edge layout-result boundaries extend?]
  (let [current (caret-geometry layout-result (:caret state) boundaries)
        line-data (get (:lines layout-result) (:line current))
        [start end] (tl/line-source-bounds (:source-range line-data))
        consumed-start (some-> line-data :consumed-range first
                               tl/source-index-offset)
        target (if (= edge :home) start (or consumed-start end))
        affinity (if (= edge :home) :downstream :upstream)]
    {:state (set-caret state (tagged-offset target) affinity extend?)
     :ops [] :status :moved}))

(defn word-range-at
  [state boundaries value]
  (let [n (offset value)
        spans (:word-spans (normalized-boundaries state boundaries))]
    (when-let [[start end]
               (or (first (filter (fn [[a b]] (and (<= a n) (< n b))) spans))
                   (first (filter (fn [[a _]] (= a n)) spans)))]
      [(tagged-offset start) (tagged-offset end)])))

(defn select-word
  [state boundaries value]
  (if-let [[start end] (word-range-at state boundaries value)]
    {:state (assoc state
                   :anchor start
                   :caret {:index end :affinity :upstream}
                   :desired-x nil)
     :ops [] :status :selected}
    {:state state :ops [] :status :no-word}))

(defn normalize-paste
  "CRLF and bare CR become LF; C0 controls except LF/TAB are removed."
  [input]
  (let [text (str (or input ""))
        length (tl/code-unit-count text)]
    (loop [i 0 out []]
      (if (>= i length)
        (apply str out)
        (let [unit (code-unit-at text i)]
          (cond
            (= unit 13)
            (recur (+ i (if (and (< (inc i) length)
                                 (= 10 (code-unit-at text (inc i))))
                          2 1))
                   (conj out "\n"))

            (and (< unit 32) (not (#{9 10} unit)))
            (recur (inc i) out)

            :else
            (recur (inc i) (conj out (subs text i (inc i))))))))))

(defn paste
  ([state input] (paste state input default-paste-budget))
  ([state input budget]
   (let [raw (str (or input ""))]
     (if (> (tl/code-unit-count raw) budget)
       {:state state :ops [] :status :refused :reason :paste/over-budget
        :budget budget :code-units (tl/code-unit-count raw)}
       (let [normalized (normalize-paste raw)]
         (if (> (tl/code-unit-count normalized) budget)
           {:state state :ops [] :status :refused :reason :paste/over-budget
            :budget budget :code-units (tl/code-unit-count normalized)}
           (assoc (replace-input state normalized)
                  :normalized-text normalized)))))))

(defn composition-start
  [state]
  (let [caret (get-in state [:caret :index])
        source-range (or (selection-range state) [caret caret])]
    {:state (-> state
                (assoc :composition {:status :composing
                                     :preedit ""
                                     :caret-in-preedit 0
                                     :source-range source-range
                                     :restore {:caret (:caret state)
                                               :anchor (:anchor state)}}
                       :caret {:index (first source-range)
                               :affinity :downstream}
                       :anchor nil
                       :desired-x nil))
     :ops [] :status :composition-started}))

(defn composition-update
  [state preedit caret-in-preedit]
  (if-not (composing? state)
    {:state state :ops [] :status :composition-ignored}
    (let [preedit (str (or preedit ""))
          caret (max 0 (min (long (or caret-in-preedit
                                     (tl/code-unit-count preedit)))
                            (tl/code-unit-count preedit)))
          caret (if (surrogate-interior? preedit caret) (inc caret) caret)]
      {:state (-> state
                  (assoc-in [:composition :preedit] preedit)
                  (assoc-in [:composition :caret-in-preedit] caret))
       :ops [] :status :composition-updated})))

(defn composition-view
  [state]
  (let [text (get-in state [:document :text])]
    (if-not (composing? state)
      {:text text
       :caret-index (get-in state [:caret :index])
       :preedit-range nil}
      (let [[start-index end-index] (get-in state [:composition :source-range])
            start (offset start-index)
            end (offset end-index)
            preedit (get-in state [:composition :preedit] "")
            preedit-length (tl/code-unit-count preedit)
            caret-in-preedit (get-in state [:composition :caret-in-preedit] 0)]
        {:text (str (subs text 0 start) preedit (subs text end))
         :caret-index (tagged-offset (+ start caret-in-preedit))
         :preedit-range [(tagged-offset start)
                         (tagged-offset (+ start preedit-length))]}))))

(defn composition-cancel
  [state]
  (if-not (composing? state)
    {:state state :ops [] :status :composition-idle}
    (let [{:keys [caret anchor]} (get-in state [:composition :restore])]
      {:state (assoc state :composition {:status :idle}
                          :caret caret :anchor anchor :desired-x nil)
       :ops [] :status :composition-cancelled})))

(defn composition-end
  [state committed]
  (cond
    (not (composing? state))
    {:state state :ops [] :status :composition-idle}

    (empty? (str (or committed "")))
    (composition-cancel state)

    :else
    (let [source-range (get-in state [:composition :source-range])]
      (assoc (apply-replacement state source-range (str committed))
             :status :composition-committed))))

(defn dispatch-key
  "Pure raw-key decision. `:pass-through` means browser/OS default is allowed;
   the runtime still returns true from the shared intercept so legacy routers
   never observe the event while a T2 session is live."
  [{:keys [key ctrl? meta? alt? shift? is-composing? key-code]}]
  (let [key (str (or key ""))
        lower (str/lower-case key)
        command? (or ctrl? meta?)]
    (cond
      (or is-composing? (= 229 key-code)) {:action :drop-composing-key}
      (= key "Escape") {:action :exit}
      (and command? (#{"+" "=" "-" "0"} key))
      {:action :pass-through :reason :browser-zoom}
      (re-matches #"F(?:[1-9]|1[0-2])" key)
      {:action :pass-through :reason :function-key}
      (= key "Dead") {:action :pass-through :reason :dead-key}
      (and command? (= lower "c")) {:action :copy}
      (and command? (= lower "x")) {:action :cut}
      (and command? (= lower "v")) {:action :native-paste}
      (and command? (= key "ArrowLeft"))
      {:action :move :direction :left :unit :word :extend? (boolean shift?)}
      (and command? (= key "ArrowRight"))
      {:action :move :direction :right :unit :word :extend? (boolean shift?)}
      command? {:action :pass-through :reason :os-shortcut}
      alt? {:action :pass-through :reason :modified-key}
      (= key "ArrowLeft")
      {:action :move :direction :left :unit :grapheme :extend? (boolean shift?)}
      (= key "ArrowRight")
      {:action :move :direction :right :unit :grapheme :extend? (boolean shift?)}
      (= key "ArrowUp") {:action :vertical :direction :up :extend? (boolean shift?)}
      (= key "ArrowDown") {:action :vertical :direction :down :extend? (boolean shift?)}
      (= key "Home") {:action :line-edge :edge :home :extend? (boolean shift?)}
      (= key "End") {:action :line-edge :edge :end :extend? (boolean shift?)}
      (= key "Backspace") {:action :delete :direction :left}
      (= key "Delete") {:action :delete :direction :right}
      (= key "Enter") {:action :insert :text "\n"}
      (= key "Tab") {:action :insert :text "\t"}
      (or (empty? key) (= key "Unidentified"))
      {:action :pass-through :reason :unidentified-key}
      (contains? #{"Shift" "Control" "Meta" "Alt" "CapsLock"
                   "PageUp" "PageDown" "Insert" "ContextMenu"} key)
      {:action :pass-through :reason :modified-key}
      :else {:action :insert :text key})))

(defn handle-key
  [state event {:keys [boundaries layout-result]}]
  (let [{:keys [action direction unit extend? edge text reason] :as decision}
        (dispatch-key event)]
    (case action
      :drop-composing-key {:state state :ops [] :status :dropped-composing-key}
      :exit {:state (:state (composition-cancel state)) :ops []
             :status :exit :exit? true}
      :pass-through {:state state :ops [] :status :pass-through :reason reason}
      :native-paste {:state state :ops [] :status :native-paste}
      :copy {:state state :ops [] :status :copy :clipboard (selected-text state)}
      :cut (let [clipboard (selected-text state)]
             (if-let [source-range (selection-range state)]
               (assoc (apply-replacement state source-range "")
                      :status :cut :clipboard clipboard)
               {:state state :ops [] :status :cut :clipboard ""}))
      :insert (replace-input state text)
      :delete (delete-direction state direction boundaries)
      :move (move-horizontal state direction unit boundaries extend?)
      :vertical (move-vertical state direction layout-result boundaries extend?)
      :line-edge (move-line-edge state edge layout-result boundaries extend?)
      {:state state :ops [] :status :pass-through :decision decision})))
