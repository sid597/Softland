(ns app.electric-flow
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            #?@(:clj [[clj-http.client :as http]
                      [clojure.data.json :as json]])
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [app.client.webgpu.loop :as loop]
                       [global-flow :refer [await-promise]]
                       ["@lezer/lr" :as lr]
                       ["@nextjournal/lezer-clojure" :as clj-parser]
                       [sci.core :as sci]])))

(def source-code
  #?(:clj (slurp "src/app/electric_flow.cljc")
     :cljs nil))

;; === AI Integration (Server-side) ===

#?(:clj
   (defn call-claude-api
     "Call Claude API with a prompt and optional code context.
      Returns {:result text} or {:error message}.
      Requires ANTHROPIC_API_KEY environment variable."
     [user-prompt code-context]
     (let [api-key (System/getenv "ANTHROPIC_API_KEY")]
       (if (str/blank? api-key)
         ;; Return mock response when no API key is configured
         {:result (str "Mock AI Response\n\n"
                       "You asked: " user-prompt "\n\n"
                       "Code context: " (count code-context) " characters\n\n"
                       "To enable real AI responses, set ANTHROPIC_API_KEY environment variable.\n"
                       "Example: export ANTHROPIC_API_KEY=sk-ant-...")}
         ;; Make real API call
         (try
           (let [messages [{:role "user"
                            :content (str "Context (code I'm working on):\n```\n"
                                          (subs code-context 0 (min 4000 (count code-context)))
                                          "\n```\n\nTask: " user-prompt)}]
                 response (http/post "https://api.anthropic.com/v1/messages"
                                     {:headers {"x-api-key" api-key
                                                "anthropic-version" "2023-06-01"
                                                "content-type" "application/json"}
                                      :body (json/write-str
                                              {:model "claude-sonnet-4-20250514"
                                               :max_tokens 1024
                                               :messages messages})
                                      :as :json})]
             (if (= 200 (:status response))
               (let [content (-> response :body :content first :text)]
                 {:result content})
               {:error (str "API error: " (:status response))}))
           (catch Exception e
             {:error (str "Request failed: " (.getMessage e))}))))))

#?(:clj
   (defn call-claude-api-mock
     "Mock version for testing without API key"
     [user-prompt code-context]
     {:result (str "AI Response (Mock Mode)\n"
                   "─────────────────────────\n\n"
                   "Task: " user-prompt "\n\n"
                   "Analysis:\n"
                   "Based on the " (count (str/split-lines code-context)) " lines of code provided, "
                   "here are some suggestions:\n\n"
                   "1. The code appears to be a Clojure/ClojureScript application\n"
                   "2. Consider adding error handling\n"
                   "3. Documentation could be improved\n\n"
                   "Note: Set ANTHROPIC_API_KEY for real AI responses.")}))

;; Client-side placeholder for AI functions
#?(:cljs (defn call-claude-api [_ _] {:error "AI calls must go through server"}))
#?(:cljs (defn call-claude-api-mock [_ _] {:error "AI calls must go through server"}))

#?(:clj (defn init-lezer-parser! [] nil))
#?(:clj (defn find-matching-bracket [_ _ _] nil))
#?(:clj (defn detect-fold-regions [_ _] []))
#?(:clj (defn init-sci! [] nil))
#?(:clj (defn sci-eval [_] {:error "SCI only available in browser"}))
#?(:clj (defn sci-eval-form [_] "SCI only available in browser"))
#?(:clj (defn find-form-at-cursor [_ _ _] nil))
#?(:clj (defn inspect-all-values [_ _] []))
#?(:clj (defn find-all-top-level-forms [_ _] []))

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

     ;; === LIVE VALUE INSPECTOR (Project 1) ===

     (defn find-all-top-level-forms
       "Parse code and find all top-level forms with their positions.
        Returns [{:form-str :line :col :end-line :end-col :type}]"
       [lines line-lengths]
       (when (and @lezer-parser (seq lines))
         (let [full-text (str/join "\n" lines)
               tree (.parse ^js @lezer-parser full-text)
               forms (atom [])
               root-depth (atom 0)]
           ;; Find top-level forms (children of Program node)
           (.. ^js tree
               (iterate #js {:enter (fn [node]
                                      (let [node-name (.-name ^js (.-type ^js node))
                                            from (.-from ^js node)
                                            to (.-to ^js node)
                                            ;; Track depth - we want immediate children of Program
                                            depth @root-depth]
                                        (when (= node-name "Program")
                                          (reset! root-depth 0))
                                        (when (and (= depth 0)
                                                   (contains? #{"List" "Vector" "Map" "Set" "Number" "String" "Symbol" "Keyword"} node-name)
                                                   (not= node-name "Program"))
                                          (let [form-str (.substring full-text from to)
                                                start-pos (offset->line-col from line-lengths)
                                                end-pos (offset->line-col (max 0 (dec to)) line-lengths)]
                                            (swap! forms conj {:form-str form-str
                                                               :line (:line start-pos)
                                                               :col (:col start-pos)
                                                               :end-line (:line end-pos)
                                                               :end-col (:col end-pos)
                                                               :type node-name
                                                               :from from
                                                               :to to})))
                                        (swap! root-depth inc)))
                             :leave (fn [_] (swap! root-depth dec))}))
           @forms)))

     (defn evaluate-form-safely
       "Evaluate a form string and return result with value type info."
       [form-str]
       (try
         (when-not @sci-ctx (init-sci!))
         (let [result (sci/eval-string* @sci-ctx form-str)
               value-type (cond
                            (nil? result) :nil
                            (number? result) :number
                            (string? result) :string
                            (keyword? result) :keyword
                            (symbol? result) :symbol
                            (boolean? result) :boolean
                            (vector? result) :vector
                            (map? result) :map
                            (set? result) :set
                            (list? result) :list
                            (fn? result) :function
                            :else :other)]
           {:result result
            :value-type value-type
            :display (if (fn? result)
                       "#<fn>"
                       (pr-str result))})
         (catch :default e
           {:error (.-message e)
            :value-type :error
            :display (str "❌ " (.-message e))})))

     (defn inspect-all-values
       "Evaluate all top-level forms and return their values with positions.
        Returns [{:line :col :end-line :end-col :display :value-type :result}]"
       [lines line-lengths]
       (let [forms (find-all-top-level-forms lines line-lengths)]
         (->> forms
              (map (fn [{:keys [form-str line col end-line end-col type]}]
                     (let [eval-result (evaluate-form-safely form-str)]
                       (merge {:line line
                               :col col
                               :end-line end-line
                               :end-col end-col
                               :form-type type}
                              eval-result))))
              (vec))))

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

(def theme
  {:keyword   {:r 0.8 :g 0.4 :b 0.8 :a 1.0}
   :macro     {:r 0.3 :g 0.6 :b 1.0 :a 1.0}
   :string    {:r 0.6 :g 0.8 :b 0.4 :a 1.0}
   :comment   {:r 0.5 :g 0.5 :b 0.5 :a 1.0}
   :delimiter {:r 1.0 :g 1.0 :b 1.0 :a 0.6}
   :number    {:r 1.0 :g 0.6 :b 0.3 :a 1.0}
   :character {:r 0.9 :g 0.7 :b 0.4 :a 1.0}
   :boolean   {:r 0.7 :g 0.3 :b 0.9 :a 1.0}
   :nil       {:r 0.8 :g 0.3 :b 0.3 :a 1.0}
   :text      {:r 0.9 :g 0.9 :b 0.9 :a 1.0}})

(defn get-color [type] (get theme type (:text theme)))

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
  "Layout tokens with optional folding support.
   Returns {:render-ops [...] :line-mapping [...]} where line-mapping maps visual->logical line."
  ([lines-of-tokens start-x start-y font-size]
   ;; No folding - all lines visible
   (layout-tokens lines-of-tokens start-x start-y font-size [] #{}))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines]
   (let [char-width (* font-size 0.6)
         line-h (* font-size 1.2)]
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
             ;; Render this line at current visual-y
             (let [line-ops (mapv (fn [token]
                                    (let [color (get-color (:type token))]
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
    {:text (editor/update-text-data device (:text-sys pipelines) render-ops atlas 16)
     :rect (editor/update-rects device (:rect-sys pipelines) [])
     :pipelines pipelines}))

;; Atom to hold pending AI request (client-side)
#?(:cljs (defonce !ai-request (atom nil)))

(e/defn ProcessAIRequest [request]
  "Electric function to process AI request on server and return response."
  (e/server
    (when request
      (let [{:keys [prompt context]} request
            response (call-claude-api prompt context)]
        response))))

(e/defn main [ring-request]
  (e/server
    (let [file-content source-code]

      (e/client
        (binding [dom/node js/document.body]
          (dom/style {:margin "0" :padding "0"
                      :width "100vw" :height "100vh"
                      :overflow "hidden" :background "#111"
                      :user-select "none"})

          (init-lezer-parser!)
          (init-sci!)

          (let [resources (LoadWebGPU)]
            (when resources
              (let [device (get resources :device)
                    format (get resources :format)
                    atlas (get resources :atlas)
                    pipelines (editor/create-editor-state resources)

                    ;; AI request callback - stores request for Electric to process
                    ai-request-fn (fn [prompt context]
                                    (reset! !ai-request {:prompt prompt
                                                         :context context
                                                         :timestamp (js/Date.now)}))]

                ;; Watch for AI requests and process them
                (let [ai-req (e/watch !ai-request)]
                  (when ai-req
                    (let [response (ProcessAIRequest ai-req)]
                      ;; Dispatch response back to loop
                      (when response
                        (if (:result response)
                          (loop/dispatch-external-event! :ai-request-success (:result response))
                          (loop/dispatch-external-event! :ai-request-error (:error response)))
                        ;; Clear the request
                        (reset! !ai-request nil)))))

                (let [lines (str/split-lines file-content)
                      tokenized-lines (mapv tokenize-line lines)
                      ;; Layout constants (must match loop.cljs)
                      gutter-w 40
                      layout-x (+ 50 gutter-w)  ;; 90
                      ;; Initial render - no folds yet
                      layout-result (layout-tokens tokenized-lines layout-x 100 16)
                      render-ops (:render-ops layout-result)

                      ;; Compute line lengths (character count per line)
                      line-lengths (mapv count lines)]

                  (let [geometry (Prepare-Geometry device pipelines render-ops atlas)]
                    (dom/canvas
                      (dom/props {:id "webgpu-canvas"
                                  :style {:width "100vw" :height "100vh" :display "block"}})
                      (let [ctx (.getContext dom/node "webgpu" (clj->js {:alpha true}))]
                        (.configure ^js ctx (clj->js {:device device :format format :alphaMode "premultiplied"}))
                        ;; Pass all functions to start-loop! with AI callback and inspector
                        (let [loop-flow (e/Task (loop/start-loop! dom/node device ctx geometry line-lengths
                                                                   lines tokenize-line layout-tokens
                                                                   find-matching-bracket detect-fold-regions
                                                                   find-form-at-cursor sci-eval-form atlas
                                                                   {:ai-request-fn ai-request-fn
                                                                    :inspect-values-fn inspect-all-values}))]
                          (e/input loop-flow)))))))))))))))
