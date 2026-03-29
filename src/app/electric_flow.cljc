(ns app.electric-flow
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [app.client.workspace.themes :as themes]
            [app.file-viewer :as fv]
            #?@(:cljs [[app.client.substrate.webgpu.renderer :as editor]
                       [app.client.workspace.runtime :as loop]
                       [global-flow :refer [await-promise]]
                       ["@lezer/lr" :as lr]
                       ["@nextjournal/lezer-clojure" :as clj-parser]
                       [sci.core :as sci]])))

(def source-code
  #?(:clj (slurp "src/app/electric_flow.cljc")
     :cljs nil))

(def initial-file-info
  #?(:clj (let [project-dir (System/getProperty "user.dir")
                rel-path "src/app/electric_flow.cljc"]
            {:path (str project-dir "/" rel-path)
             :project project-dir})
     :cljs nil))

#?(:clj (defn init-lezer-parser! [] nil))
#?(:clj (defn find-matching-bracket [_ _ _] nil))
#?(:clj (defn detect-fold-regions [_ _] []))
#?(:clj (defn init-sci! [] nil))
#?(:clj (defn sci-eval [_] {:error "SCI only available in browser"}))
#?(:clj (defn sci-eval-form [_] "SCI only available in browser"))
#?(:clj (defn find-form-at-cursor [_ _ _] nil))

#?(:cljs
   (do
     (def lezer-parser (atom nil))

     (defn init-lezer-parser! []
       (when-not @lezer-parser
         (reset! lezer-parser (.-parser clj-parser))))

     ;; === SCI (Small Clojure Interpreter) ===

     (def sci-ctx (atom nil))

     (defn init-sci! []
       "Initialize SCI context with clojure.core and common namespaces"
       (when-not @sci-ctx
         (reset! sci-ctx
                 (sci/init {:namespaces {'user {}}
                            :classes {'js js/globalThis}}))))

     (defn sci-eval
       "Evaluate a Clojure string using SCI. Returns {:result value} or {:error message}"
       [code-str]
       (try
         (when-not @sci-ctx (init-sci!))
         {:result (sci/eval-string* @sci-ctx code-str)}
         (catch :default e
           {:error (.-message e)})))

     (defn sci-eval-form
       "Evaluate a single form string. Returns formatted result string."
       [form-str]
       (let [{:keys [result error]} (sci-eval form-str)]
         (if error
           (str "❌ " error)
           (str "=> " (pr-str result)))))

     (def macro-symbols
       #{"defn" "def" "defmacro" "defn-" "defonce" "defmulti" "defmethod" "defprotocol" "defrecord" "deftype"
         "let" "fn" "if" "if-let" "if-some" "do" "ns" "when" "when-let" "when-some" "when-not" "when-first"
         "cond" "condp" "case" "loop" "recur" "for" "doseq" "dotimes" "while"
         "try" "catch" "finally" "throw" "assert"
         "binding" "with-open" "with-local-vars" "with-redefs"
         "require" "import" "use" "refer" "in-ns"
         "->" "->>" "as->" "some->" "some->>" "cond->" "cond->>"
         "and" "or" "not"
         "lazy-seq" "delay" "future" "promise"
         "e/defn" "e/client" "e/server" "e/fn" "dom/on"})

     (defn classify-token [node-name text]
       (cond
         (= node-name "LineComment") :comment
         (or (= node-name "String") (= node-name "RegExp")) :string
         (= node-name "Character") :character
         (= node-name "Keyword") :keyword
         (= node-name "Number") :number
         (or (= node-name "Boolean") (= node-name "BooleanLiteral")) :boolean
         (= node-name "Nil") :nil
         (contains? #{"(" ")" "[" "]" "{" "}"} text) :delimiter
         (contains? macro-symbols text) :macro
         :else :text))

     (def container-node-types
       #{"Program" "List" "Vector" "Map" "Set" "Meta" "Deref" "Quote" 
         "SyntaxQuote" "Unquote" "UnquoteSplice" "Anon" "Regex"
         "VarQuote" "Discard" "NamespacedMap" "ReaderConditional"})

     (defn extract-tokens-from-syntax-tree [tree text]
       (let [tokens (atom [])]
         (.. ^js tree
             (iterate #js {:enter (fn [node]
                                    (let [node-name (.-name ^js (.-type ^js node))
                                          from (.-from ^js node)
                                          to (.-to ^js node)
                                          node-text (.substring text from to)]
                                      (when (and (not (contains? container-node-types node-name))
                                                 (or (= node-name "String")
                                                     (= node-name "LineComment")
                                                     (not (re-find #"\s" node-text)))
                                                 (not (str/blank? node-text))
                                                 (> (count node-text) 0))
                                        (swap! tokens conj {:text node-text
                                                            :type (classify-token node-name node-text)
                                                            :from from
                                                            :to to}))))}))
         @tokens))

     (defn tokenize-line [line-text]
       (if (or (empty? line-text) (not @lezer-parser))
         []
         (let [tree (.parse ^js @lezer-parser line-text)]
           (extract-tokens-from-syntax-tree tree line-text))))

     ;; Bracket matching pairs
     (def bracket-pairs
       {"(" ")" ")" "("
        "[" "]" "]" "["
        "{" "}" "}" "{"})

     (def open-brackets #{"(" "[" "{"})
     (def close-brackets #{")" "]" "}"})

     (defn offset->line-col
       "Convert absolute offset to {:line :col} given line-lengths"
       [offset line-lengths]
       (loop [remaining offset
              line-idx 0]
         (if (>= line-idx (count line-lengths))
           {:line (dec (count line-lengths)) :col (get line-lengths (dec (count line-lengths)) 0)}
           (let [line-len (inc (get line-lengths line-idx 0))] ;; +1 for newline
             (if (< remaining line-len)
               {:line line-idx :col remaining}
               (recur (- remaining line-len) (inc line-idx)))))))

     (defn line-col->offset
       "Convert {:line :col} to absolute offset given line-lengths"
       [{:keys [line col]} line-lengths]
       (let [lines-before (subvec line-lengths 0 (min line (count line-lengths)))
             offset-to-line (reduce + (map inc lines-before))] ;; +1 for each newline
         (+ offset-to-line col)))

     (defn find-form-at-cursor
       "Given cursor position and lines, find the outermost form containing cursor.
        Returns {:form-str :start-line :end-line} or nil"
       [cursor-pos lines line-lengths]
       (when (and @lezer-parser cursor-pos (seq lines))
         (let [full-text (str/join "\n" lines)
               cursor-offset (line-col->offset cursor-pos line-lengths)
               tree (.parse ^js @lezer-parser full-text)
               ;; Find the outermost List/Vector/Map containing cursor
               ;; Lezer iterates parent-first, so first match is outermost
               best-match (atom nil)]
           (.. ^js tree
               (iterate #js {:enter (fn [node]
                                      (let [node-name (.-name ^js (.-type ^js node))
                                            from (.-from ^js node)
                                            to (.-to ^js node)]
                                        ;; Check if cursor is inside this node
                                        (when (and (contains? #{"List" "Vector" "Map" "Set"} node-name)
                                                   (<= from cursor-offset)
                                                   (< cursor-offset to))
                                          ;; Keep only the first (outermost) match
                                          (when (nil? @best-match)
                                            (reset! best-match {:from from
                                                                :to to
                                                                :type node-name})))))}))
           (when @best-match
             (let [{:keys [from to]} @best-match
                   form-str (.substring full-text from to)
                   start-pos (offset->line-col from line-lengths)
                   end-pos (offset->line-col (dec to) line-lengths)]
               {:form-str form-str
                :start-line (:line start-pos)
                :end-line (:line end-pos)
                :from from
                :to to})))))

     (defn find-matching-bracket
       "Given cursor position and document, find matching bracket if cursor is on one.
        Returns {:open {:line :col} :close {:line :col}} or nil"
       [cursor-pos lines line-lengths]
       (when (and @lezer-parser cursor-pos (seq lines))
         (let [full-text (str/join "\n" lines)
               cursor-offset (line-col->offset cursor-pos line-lengths)
               ;; Check char at cursor and char before cursor
               char-at (when (< cursor-offset (count full-text))
                         (str (nth full-text cursor-offset)))
               char-before (when (and (> cursor-offset 0) (<= cursor-offset (count full-text)))
                             (str (nth full-text (dec cursor-offset))))
               ;; Determine which bracket we're on
               [bracket-char bracket-offset]
               (cond
                 (get bracket-pairs char-at) [char-at cursor-offset]
                 (get bracket-pairs char-before) [char-before (dec cursor-offset)]
                 :else [nil nil])]
           (when bracket-char
             (let [tree (.parse ^js @lezer-parser full-text)
                   ;; Walk tree to find the bracket's container node
                   result (atom nil)]
               ;; Iterate through tree to find matching container
               (.. ^js tree
                   (iterate #js {:enter (fn [node]
                                          (let [node-name (.-name ^js (.-type ^js node))
                                                from (.-from ^js node)
                                                to (.-to ^js node)]
                                            ;; Check if this is a container and bracket is at boundary
                                            (when (and (contains? container-node-types node-name)
                                                       (or (= from bracket-offset)
                                                           (= (dec to) bracket-offset)))
                                              (reset! result {:open (offset->line-col from line-lengths)
                                                              :close (offset->line-col (dec to) line-lengths)}))))}))
               @result)))))

     (def foldable-node-types
       #{"List" "Vector" "Map" "Set"})

     (defn detect-fold-regions
       "Detect all foldable regions in the document.
        Returns [{:start-line :end-line :type} ...] for multi-line forms"
       [lines line-lengths]
       (when (and @lezer-parser (seq lines))
         (let [full-text (str/join "\n" lines)
               tree (.parse ^js @lezer-parser full-text)
               regions (atom [])]
           (.. ^js tree
               (iterate #js {:enter (fn [node]
                                      (let [node-name (.-name ^js (.-type ^js node))
                                            from (.-from ^js node)
                                            to (.-to ^js node)]
                                        (when (contains? foldable-node-types node-name)
                                          (let [start-pos (offset->line-col from line-lengths)
                                                end-pos (offset->line-col (dec to) line-lengths)]
                                            ;; Only foldable if spans multiple lines
                                            (when (> (:line end-pos) (:line start-pos))
                                              (swap! regions conj {:start-line (:line start-pos)
                                                                   :end-line (:line end-pos)
                                                                   :type node-name}))))))}))
           ;; Sort by start line, nested regions come after their parents
           (sort-by :start-line @regions))))))

(def jvm-macro-symbols
  #{"defn" "def" "defmacro" "defn-" "defonce" "defmulti" "defmethod" "defprotocol" "defrecord" "deftype"
    "let" "fn" "if" "if-let" "if-some" "do" "ns" "when" "when-let" "when-some" "when-not" "when-first"
    "cond" "condp" "case" "loop" "recur" "for" "doseq" "dotimes" "while"
    "try" "catch" "finally" "throw" "assert"
    "binding" "with-open" "with-local-vars" "with-redefs"
    "require" "import" "use" "refer" "in-ns"
    "->" "->>" "as->" "some->" "some->>" "cond->" "cond->>"
    "and" "or" "not"
    "lazy-seq" "delay" "future" "promise"
    "e/defn" "e/client" "e/server" "e/fn" "dom/on"})

#?(:clj
   (defn tokenize-line [line-text]
     (if (empty? line-text)
       []
       (let [pattern #";.*|:[^ \[\]\(\)\s]+|[\(\)\[\]\{\}]|\"[^\"]*\"|\s+|[^ ;:\[\]\(\)\{\}\"\s]+"
             matches (re-seq pattern line-text)]
         (loop [tokens []
                pos 0
                [m & rest-matches] matches]
           (if (nil? m)
             tokens
             (let [match-start (.indexOf line-text m pos)
                   match-end (+ match-start (count m))
                   token {:text m
                          :from match-start
                          :to match-end
                          :type (cond
                                  (str/starts-with? m ";") :comment
                                  (str/starts-with? m ":") :keyword
                                  (str/starts-with? m "\"") :string
                                  (str/blank? m) :whitespace
                                  (contains? #{"(" ")" "[" "]" "{" "}"} m) :delimiter
                                  (contains? jvm-macro-symbols m) :macro
                                  :else :text)}]
               (if (= :whitespace (:type token))
                 (recur tokens match-end rest-matches)
                 (recur (conj tokens token) match-end rest-matches)))))))))

;; ============================================================================
;; SYNTAX HIGHLIGHTING (delegated to themes namespace)
;; ============================================================================

(def default-theme-id themes/default-theme-id)

(defn get-color
  "Get color for a token type from the specified theme"
  ([type] (themes/get-color type))
  ([type theme-id] (themes/get-color type theme-id)))

(defn line-visible?
  "Check if a logical line should be visible given fold regions and folded state.
   A line is hidden if it's inside a folded region (but not the first line of that region)."
  [line-idx fold-regions folded-lines]
  (not (some (fn [{:keys [start-line end-line]}]
               (and (contains? folded-lines start-line)  ;; This region is folded
                    (> line-idx start-line)              ;; Line is after the fold start
                    (<= line-idx end-line)))             ;; Line is within the fold
             fold-regions)))

(defn layout-tokens
  "Layout tokens with optional folding support and theme.
   Returns {:render-ops [...] :line-mapping [...]} where line-mapping maps visual->logical line."
  ([lines-of-tokens start-x start-y font-size]
   ;; No folding - all lines visible, default theme
   (layout-tokens lines-of-tokens start-x start-y font-size [] #{} nil nil default-theme-id))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines]
   (layout-tokens lines-of-tokens start-x start-y font-size fold-regions folded-lines nil nil default-theme-id))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines char-advance line-h]
   (layout-tokens lines-of-tokens start-x start-y font-size fold-regions folded-lines char-advance line-h default-theme-id))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines char-advance line-h theme-id]
   (let [char-width (or char-advance (* font-size 0.56))
         line-h (or line-h (* font-size 1.2))
         active-theme-id (or theme-id default-theme-id)]
     (loop [logical-idx 0
            visual-y (+ start-y font-size)
            render-ops []
            line-mapping []]  ;; Maps visual line index -> logical line index
       (if (>= logical-idx (count lines-of-tokens))
         {:render-ops render-ops
          :line-mapping line-mapping}
         (let [tokens (nth lines-of-tokens logical-idx)
               visible? (line-visible? logical-idx fold-regions folded-lines)]
           (if visible?
             ;; Render this line at current visual-y, using the active theme
             (let [line-ops (mapv (fn [token]
                                     (let [color (get-color (:type token) active-theme-id)]
                                       (merge token color
                                             {:x (+ start-x (* (or (:from token) 0) char-width))
                                              :y visual-y
                                              :size font-size})))
                                  tokens)]
               (recur (inc logical-idx)
                      (+ visual-y line-h)
                      (conj render-ops line-ops)
                      (conj line-mapping logical-idx)))
             ;; Skip this line (it's folded)
             (recur (inc logical-idx)
                    visual-y  ;; Don't advance visual-y
                    render-ops
                    line-mapping))))))))

#?(:cljs
   (defn load-resources-async []
     (js/Promise.all
       #js [(-> (js/fetch "/font_atlas.png") (.then #(.blob %)) (.then #(js/createImageBitmap %)))
            (-> (js/fetch "/font_atlas.json") (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))])))

#?(:cljs
   (defn load-font-manifest-async []
     "Load the font manifest from the fonts directory"
     (-> (js/fetch "/fonts/manifest.json")
         (.then #(.json %))
         (.then #(js->clj % :keywordize-keys true))
         (.catch (fn [_e]
                   ;; Fallback if manifest not found
                   {:fonts [{:name "Ubuntu Sans Mono"
                             :id "ubuntu-sans-mono"
                             :atlas "ubuntu_sans_mono_atlas.png"
                             :metrics "ubuntu_sans_mono_atlas.json"
                             :charWidth 0.56
                             :default true
                             :defaults {:fontSize 19
                                        :lineHeight 1.2
                                        :pxRange 8
                                        :sharpness 0.0
                                        :snapToPixel true
                                        :showDiagnostics false}}]
                    :settings {:fontSize {:default 19}
                               :lineHeight {:default 1.2}
                               :pxRange {:default 8}
                               :sharpness {:default 0.0}
                               :snapToPixel {:default true}
                               :showDiagnostics {:default false}}})))))

#?(:cljs
   (defn load-font-atlas-async [font-config]
     "Load a specific font's atlas and metrics given its config from manifest"
     (let [base-path "/fonts/"
           atlas-url (str base-path (:atlas font-config))
           metrics-url (str base-path (:metrics font-config))]
       (js/Promise.all
         #js [(-> (js/fetch atlas-url) (.then #(.blob %)) (.then #(js/createImageBitmap %)))
              (-> (js/fetch metrics-url) (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))]))))

(e/defn LoadWebGPU []
  (e/client
    (let [raw (e/Task (await-promise (load-resources-async)))]
      (when raw
        (let [bitmap (aget raw 0) 
              atlas (aget raw 1)
              gpu js/navigator.gpu
              adapter (e/Task (await-promise (.requestAdapter ^js gpu)))
              device (e/Task (await-promise (.requestDevice ^js adapter)))
              format (.getPreferredCanvasFormat ^js gpu)]
          (when (and device atlas bitmap)
            {:bitmap bitmap :atlas atlas :device device :format format}))))))

(e/defn Prepare-Geometry [device pipelines render-ops atlas]
  (e/client
    (let [font-size 19
          char-width 0.56
          dpr (or (.-devicePixelRatio js/window) 1)
          snap-step (/ 1 dpr)
          snap (fn [v] (* (Math/round (/ v snap-step)) snap-step))
          line-h (snap (* font-size 1.2))]
      {:text (editor/update-text-data device (:text-sys pipelines) render-ops atlas font-size
                                      :line-height line-h
                                      :char-width char-width
                                      :snap-step snap-step)
       :rect (editor/update-rects device (:rect-sys pipelines) [])
       :shadow (editor/update-shadows device (:shadow-sys pipelines) [])
       :pipelines pipelines})))

;; ============================================================================
;; SIDEBAR CONSTANTS (used by imperative DOM in loop.cljs)
;; ============================================================================

;; Sidebar constants removed — sidebar now rendered via WebGPU rect tree in loop.cljs

(e/defn main [ring-request]
  (e/server
    (let [file-content source-code
          file-info initial-file-info]

      (e/client
        (binding [dom/node js/document.body]
          (dom/style {:margin "0" :padding "0"
                      :width "100vw" :height "100vh"
                      :overflow "hidden" :background "#111"
                      :user-select "none"})

          (init-lezer-parser!)
          (init-sci!)

          (let [resources (LoadWebGPU)
                font-manifest (e/Task (await-promise (load-font-manifest-async)))
                ;; Sidebar state atoms (plain CLJS, not watched by Electric)
                !sidebar-visible (atom true)
                !file-load-request (atom nil)
                ;; Rama truth atoms — Electric subscriptions populate these
                !sidebar-truth (atom nil)
                !settings-truth (atom nil)
                !agent-trail-truth (atom nil)
                !flow-session-truth (atom nil)]
            ;; Reactive sync: Rama PState → Electric → client atom.
            ;; Re-runs whenever the server-side PState changes.
            (reset! !sidebar-truth (fv/WatchSidebarTruth))
            (reset! !settings-truth (fv/WatchUserSettings))
            (reset! !agent-trail-truth (fv/WatchAgentTrail))
            (reset! !flow-session-truth (fv/WatchFlowSession))
            (when resources
              (let [device (get resources :device)
                    format (get resources :format)
                    atlas (get resources :atlas)
                    pipelines (editor/create-editor-state resources)]

                (let [lines (str/split-lines file-content)
                      tokenized-lines (mapv tokenize-line lines)
                      gutter-w 40
                      layout-x (+ 50 gutter-w)
                      font-size 19
                      dpr (or (.-devicePixelRatio js/window) 1)
                      snap-step (/ 1 dpr)
                      snap (fn [v] (* (Math/round (/ v snap-step)) snap-step))
                      char-advance (snap (* font-size 0.56))
                      line-h (snap (* font-size 1.2))
                      layout-result (layout-tokens tokenized-lines layout-x 100 font-size [] #{} char-advance line-h)
                      render-ops (:render-ops layout-result)
                      line-lengths (mapv count lines)]

                  (let [geometry (Prepare-Geometry device pipelines render-ops atlas)]

                    ;; Extract preview overlay (hidden by default, shown when extract mode active)
                    (let [preview-el-atom (atom nil)]
                      (dom/div
                        (dom/props {:id "extract-preview-overlay"
                                    :style {:position "fixed"
                                            :top "0" :left "0"
                                            :width "50vw" :height "100vh"
                                            :display "none"
                                            :overflow "auto"
                                            :background "#111"
                                            :border-right "1px solid #333"
                                            :z-index "10"
                                            :padding "16px"
                                            :box-sizing "border-box"}})
                        (reset! preview-el-atom dom/node))

                      ;; Full-width canvas (sidebar rendered on GPU, no DOM element needed)
                      (dom/canvas
                        (dom/props {:id "webgpu-canvas"
                                    :style {:width "100vw"
                                            :height "100vh"
                                            :display "block"}})
                        (let [ctx (.getContext dom/node "webgpu" (clj->js {:alpha true}))]
                          (.configure ^js ctx (clj->js {:device device :format format :alphaMode "premultiplied"}))
                          (e/Task (loop/start-loop! dom/node device ctx geometry line-lengths
                                                    lines tokenize-line layout-tokens
                                                    find-matching-bracket detect-fold-regions
                                                    find-form-at-cursor sci-eval-form atlas
                                                    :font-manifest font-manifest
                                                    :!sidebar-visible !sidebar-visible
                                                    :!file-load-request !file-load-request
                                                    :!preview-el preview-el-atom
                                                    :!remote-sidebar-truth !sidebar-truth
                                                    :!remote-settings-truth !settings-truth
                                                    :!remote-agent-trail !agent-trail-truth
                                                    :!remote-flow-session !flow-session-truth
                                                    :initial-file file-info)))))))))))))))
