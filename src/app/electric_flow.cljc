(ns app.electric-flow
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.text-layout :as tl]
            [app.file-viewer :as fv]
            ;; block-write Lane A · server-only edit entry point deps (the write
            ;; layer, never on the client). face_projection.clj stays READ-ONLY
            ;; (its g12 grep forbids the append form), so the ONE :object/edit
            ;; entry point lives HERE in the artery, beside RecordFaceWear.
            #?@(:clj [[app.server.rama.object-container :as oc]
                      [app.server.rama.object-container.runtime :as ocr]
                      [app.server.rama.util-fns :as util-fns]])
            #?@(:cljs [[app.client.substrate.webgpu.renderer :as editor]
                       [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
                       [app.client.workspace.runtime.fonts :as runtime-fonts]
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

#?(:clj
   (defn submit-block-edit!
     "block-write Lane A · the ONE server edit entry point (CONTRACT §3 envelope →
      :object/edit on the EXISTING object-container stream path; no new module /
      depot / topology — BW-T1). Callable from Electric server context (the artery
      wiring in Main below; Lane B's reader-face outbox connects at INT).

      `env` carries client-minted fields: :request-id, :idempotency-key (deterministic
      f(request-id), object-key-scoped — nil lets the kernel builder derive it that
      way), :edit-client-id, :edit-seq, :actor (sid's map WITH capability :object/edit),
      :time-ms, :target {:target/kind :target/id}, and :payload {:document-container-id
      :object-key :content-text}. The SERVER stamps :content-hash itself via the kernel's
      own `oc/source-hash` (one hash rule, HERE — no cljs crypto port, no drift; any
      client-supplied hash is IGNORED). Appends with :ack (ack ⇒ event tree complete ⇒
      materialized, CONTRACT §3), reads the DURABLE decision (accepted OR rejected — the
      refusal reason SURFACES, G5), and on an ACCEPT bumps the ingest epoch so the generic
      FacePull re-reads truth. The bump lives in THIS ack continuation ONLY — never a
      render / m/latest path (BW-T9). Returns a plain, wire-safe map (no raw record)."
     ([oc-rt env] (submit-block-edit! oc-rt env util-fns/!ingest-epoch-atom))
     ([oc-rt env !epoch]
      (let [{:keys [request-id idempotency-key edit-client-id edit-seq actor time-ms
                    target payload]} env
            {:keys [document-container-id object-key content-text]} payload
            content-text (str content-text)
            request (oc/object-edit-request
                     (:target/kind target)
                     (:target/id target)
                     content-text
                     {:object-key            object-key
                      :document-container-id document-container-id
                      ;; server stamps the hash (CONTRACT §4) — client hash ignored
                      :content-hash          (oc/source-hash content-text)
                      :request-id            request-id
                      :idempotency-key       idempotency-key
                      :edit-client-id        edit-client-id
                      :edit-seq              edit-seq
                      :actor                 actor
                      :time-ms               time-ms})
            ;; BW-T5 replay detection: a same-request-id re-append hits the audit
            ;; short-circuit (object_container.clj:1818), which ack-returns the PRIOR
            ;; decision VERBATIM — no replay marker on the row. So the honest signal is
            ;; "was this request-id already decided BEFORE this append?" (one cheap point
            ;; read). A same-idempotency-key / different-request-id replay instead carries
            ;; :replayed-from-decision-id; we honour both.
            already-decided? (some? (ocr/read-decision oc-rt request))]
        ;; :ack IS the barrier on this stream topology (materialized on return —
        ;; the deterministic barrier, never a poll).
        (ocr/append-block-edit-request-durably! oc-rt request :ack)
        (let [decision  (ocr/read-decision oc-rt request)
              accepted? (oc/decision-accepted? decision)
              replay?   (or already-decided?
                            (some? (:replayed-from-decision-id decision)))]
          ;; BW-T9: epoch bump ONLY here, after the ack barrier, and ONLY on a real
          ;; truth change (a fresh accept — not a replay, not a rejection). A rejection
          ;; changes no state; the caller reverts the block from the returned reason.
          (when (and accepted? (not replay?)) (swap! !epoch inc))
          {:accepted?  accepted?
           :replay?    replay?
           :reason     (:reason decision)   ;; G5: refusal reason surfaced
           :errors     (:errors decision)
           :status     (:status decision)
           :request-id request-id
           :object-key object-key
           :target-id  (:target/id target)})))))

(e/defn SubmitBlockEdit [env]
  ;; block-write Lane A · the edit write e/defn (CONTRACT §3). Same indirection as
  ;; RecordFaceWear (fv): server-only body, oc-rt resolved from face-ctx, plain-map
  ;; result crosses back. The accept-side epoch bump inside submit-block-edit!
  ;; (BW-T9) drives the existing WatchIngestEpoch → FacePull re-read.
  (e/server (submit-block-edit! (:oc-rt (fv/face-ctx)) env)))

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

(defn- tokens->source-line
  "Compatibility reconstruction for legacy callers that do not supply the
   source line. This recovers source text only; it never clips or positions."
  [tokens]
  (let [n (reduce max 0 (map :to tokens))]
    (reduce (fn [line {:keys [text from to]}]
              (str (subs line 0 from) text (subs line to)))
            (apply str (repeat n " "))
            tokens)))

(defn layout-tokens
  "Layout tokens with optional folding support and theme.
   Returns {:render-ops [...] :line-mapping [...]} where line-mapping maps visual->logical line."
  ([lines-of-tokens start-x start-y font-size]
   ;; No folding - all lines visible, default theme
   (layout-tokens lines-of-tokens start-x start-y font-size [] #{} nil nil
                  default-theme-id nil nil))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines]
   (layout-tokens lines-of-tokens start-x start-y font-size fold-regions folded-lines
                  nil nil default-theme-id nil nil))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines char-advance line-h]
   (layout-tokens lines-of-tokens start-x start-y font-size fold-regions folded-lines
                  char-advance line-h default-theme-id nil nil))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines char-advance line-h theme-id]
   (layout-tokens lines-of-tokens start-x start-y font-size fold-regions folded-lines
                  char-advance line-h theme-id nil nil))
  ([lines-of-tokens start-x start-y font-size fold-regions folded-lines
    char-advance line-h theme-id provider source-lines]
   (let [char-advance (or char-advance (* font-size 0.56))
         line-h (or line-h (* font-size 1.2))
         active-theme-id (or theme-id default-theme-id)
         source-lines (vec (or source-lines
                               (map tokens->source-line lines-of-tokens)))
         visible-indices (filterv #(line-visible? % fold-regions folded-lines)
                                  (range (count lines-of-tokens)))
         visible-lines (mapv #(get source-lines % "") visible-indices)
         layout-result (tl/layout {:text (str/join "\n" visible-lines)
                                   :source-lines visible-lines
                                   :provider provider
                                   :font-size font-size
                                   :char-advance char-advance
                                   :line-height line-h
                                   :origin [start-x start-y]
                                   :baseline-offset font-size
                                   :line-map visible-indices
                                   :source-id :editor/document
                                   :source-revision (hash [source-lines folded-lines])})
         shaped? (not= :legacy/code-unit-grid
                       (get-in layout-result [:shaping :shaper-id]))
         render-ops
         (mapv
           (fn [visual-idx logical-idx]
             (let [tokens (nth lines-of-tokens logical-idx)
                   line (nth (:lines layout-result) visual-idx)
                   line-start (get-in line [:source-range 0 :offset] 0)
                   baseline (:baseline line)]
               (mapv
                 (fn [token]
                   (let [color (get-color (:type token) active-theme-id)
                         caret (tl/caret-result layout-result visual-idx
                                                (or (:from token) 0))
                         x (first (:position caret))
                         start (update (first (:source-range line)) :offset
                                       (constantly (+ line-start (or (:from token) 0))))
                         end (update (second (:source-range line)) :offset
                                     (constantly (+ line-start (or (:to token) 0))))]
                     (cond-> (merge token color
                                    {:x x :y (second baseline) :size font-size})
                       shaped?
                       (assoc :layout-result layout-result
                              :layout-line-id (:line/id line)
                              :layout-anchor [x (second baseline)]
                              :paint-source-range [start end]))))
                 tokens)))
           (range) visible-indices)]
     {:render-ops render-ops
      :line-mapping visible-indices
      :layout-result layout-result})))

#?(:cljs
   (do
     (defonce !window-debug-hooks-installed? (atom false))

     (defn- install-window-debug-hooks! []
       (when-not @!window-debug-hooks-installed?
         (reset! !window-debug-hooks-installed? true)
         (.addEventListener js/window "error"
           (fn [event]
             (js/console.error "[CLIENT/ERROR]"
                               {:message (.-message event)
                                :filename (.-filename event)
                                :lineno (.-lineno event)
                                :colno (.-colno event)
                                :error (.-error event)})))
         (.addEventListener js/window "unhandledrejection"
           (fn [event]
             (js/console.error "[CLIENT/UNHANDLED-REJECTION]" (.-reason event))))
         (js/console.log "[CLIENT] Installed global window debug hooks")))

     (defn- install-webgpu-debug-hooks! [^js device]
       (when (and device (not (true? (.-__softlandDebugHooksInstalled device))))
         (set! (.-__softlandDebugHooksInstalled device) true)
         (.addEventListener device "uncapturederror"
           (fn [event]
             (js/console.error "[WEBGPU/UNCAUGHT-ERROR]" (.-error event))))
         (-> (.-lost device)
             (.then (fn [info]
                      (js/console.error "[WEBGPU/DEVICE-LOST]"
                                        {:message (.-message info)
                                         :reason (.-reason info)})))
             (.catch (fn [err]
                       (js/console.error "[WEBGPU/DEVICE-LOST-HOOK-FAILED]" err))))
         (js/console.log "[WEBGPU] Installed device debug hooks")))))

(e/defn LoadWebGPU []
  (e/client
    (let [_ (install-window-debug-hooks!)
          gpu js/navigator.gpu
          _ (when-not gpu
              (js/console.error "[BOOT] navigator.gpu unavailable"))
          adapter (e/Task (await-promise (.requestAdapter ^js gpu)))
          device (e/Task (await-promise (.requestDevice ^js adapter)))
          initial-font-data (e/Task (await-promise (runtime-fonts/load-default-font-data-async)))]
      (when (and adapter device initial-font-data)
        (let [format (.getPreferredCanvasFormat ^js gpu)
              adapter-limits (gpu-budget/snapshot-adapter-limits adapter)
              tracker (gpu-budget/create-tracker adapter-limits)]
          (install-webgpu-debug-hooks! device)
          (js/console.log "[BOOT] WebGPU ready"
                          {:format format
                           :font-id (get-in initial-font-data [:font-config :id])
                           :font-backend (get-in initial-font-data [:font-assets :backend])
                           :adapter-limits adapter-limits})
          (merge initial-font-data
                 {:device device
                  :format format
                  :adapter-limits adapter-limits
                  :gpu-budget tracker}))))))

(e/defn Prepare-Geometry [device pipelines render-ops font-assets font-config]
  (e/client
    (let [font-defaults (:defaults font-config)
          font-size (or (:fontSize font-defaults) 19)
          char-width (or (:charWidth font-config) 0.56)
          px-range (or (:pxRange font-defaults) 8)
          sharpness (or (:sharpness font-defaults) 0.0)
          line-height-factor (or (:lineHeight font-defaults) 1.2)
          dpr (or (.-devicePixelRatio js/window) 1)
          snap-step (/ 1 dpr)
          snap (fn [v] (* (Math/round (/ v snap-step)) snap-step))
          line-h (snap (* font-size line-height-factor))]
      (js/console.log "[BOOT] Prepare geometry"
                      {:font-id (:id font-config)
                       :font-backend (:backend font-assets)
                       :render-line-count (count render-ops)
                       :font-size font-size
                       :char-width char-width
                       :px-range px-range
                       :line-height line-h
                       :dpr dpr})
      {:text (editor/update-text-data device (:text-sys pipelines) render-ops font-assets font-size
                                      :px-range px-range
                                      :line-height line-h
                                      :char-width char-width
                                      :snap-step snap-step
                                      :sharpness sharpness)
       :rect (editor/update-rects device (:rect-sys pipelines) [])
       ;; Shadow pools now own runtime shadow uploads; bootstrap only needs the pipeline state.
       :shadow (:shadow-sys pipelines)
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
                ;; Rama truth atoms — Electric subscriptions populate these
                !sidebar-truth (atom nil)
                !settings-truth (atom nil)
                !agent-trail-truth (atom nil)
                !flow-session-truth (atom nil)
                !workspace-truth (atom nil)
                ;; Trail face (view-mvp WP-B2): request set by the runtime
                ;; wiring, pull result + epoch pushed back to it.
                !trail-request (atom nil)
                !trail-data (atom nil)
                !ingest-epoch-remote (atom 0)
                ;; Faces-as-assemblies · the ONE generic face artery (CONTRACT §7).
                ;; Request set by face-wiring (a /face command + scrub), data-context
                ;; pulled back whole. W1-INT threads these into the runtime loop +
                ;; calls face-wiring/install-face-wiring!; this lane owns the atoms +
                ;; the generic pull only.
                !face-request (atom nil)
                !face-data (atom nil)
                ;; W2 (CONTRACT §16): two more GENERIC pulls through the SAME
                ;; FacePull (dispatch stays server-side, trap T8 — these are
                ;; constant surfaces, not per-face growth): the wear-time
                ;; assembly-source pull and the sidebar face-list pull. Plus
                ;; the codebase's first WRITE path: the wear outbox →
                ;; RecordFaceWear (arsenal-only, never through the read
                ;; artery — trap T15).
                !assembly-request (atom nil)
                !assembly-data (atom nil)
                !face-list-request (atom nil)
                !face-list-data (atom nil)
                ;; editable-material P3: one constant, batched facet projection
                ;; through the existing FacePull + ingest-epoch artery.
                !facet-materials-request (atom nil)
                !facet-materials-data (atom nil)
                ;; editable-material P2: console-triggered, read-only inspector
                ;; through the SAME server projection registry + FacePull.
                !material-inspector-request (atom nil)
                !material-inspector-data (atom nil)
                ;; editable-material P5: the interaction table (gesture × facet
                ;; → verb) read through the SAME registry + FacePull. Another
                ;; call site of one artery, never a new transport.
                !interaction-table-request (atom nil)
                !interaction-table-data (atom nil)
                ;; editable-material P7: the portal — the whole material world
                ;; around ONE pick, joined server-side and returned in ONE
                ;; data-context. Another call site of the same artery: the client
                ;; performs zero joins, which is the fence P7 stops on.
                !material-portal-request (atom nil)
                !material-portal-data (atom nil)
                !face-wear-outbox (atom nil)
                !face-wear-result (atom nil)
                ;; block-write Lane A · the edit write seam (CONTRACT §3/§5). Lane B's
                ;; reader-face outbox sets !block-edit-outbox (one envelope per
                ;; keystroke); the server e/watch below drives submit-block-edit! and
                ;; mirrors the plain result back. The epoch bump lives inside that fn's
                ;; ack continuation (BW-T9), so an accept re-pulls the face via the
                ;; SAME WatchIngestEpoch + FacePull path (no second epoch channel).
                !block-edit-outbox (atom nil)
                !block-edit-result (atom nil)
                ;; block-write INT · the §5 narrowing echo: an ACCEPTED edit
                ;; arms ONE single-unit truth pull (same generic FacePull
                ;; transport, same read-unit overlay river-page uses) — the
                ;; INV-19 1s debounce guards FULL-face pulls, not this narrow
                ;; read. Same shape as !assembly-request (a second FacePull
                ;; call site, not a new transport).
                !block-truth-request (atom nil)
                !block-truth-data (atom nil)]
            ;; Reactive sync: Rama PState → Electric → client atom.
            ;; Re-runs whenever the server-side PState changes.
            (reset! !sidebar-truth (fv/WatchSidebarTruth))
            (reset! !settings-truth (fv/WatchUserSettings))
            (reset! !agent-trail-truth (fv/WatchAgentTrail))
            (reset! !flow-session-truth (fv/WatchFlowSession))
            (reset! !workspace-truth (fv/WatchWorkspaceTruth))
            (reset! !ingest-epoch-remote (fv/WatchIngestEpoch))
            ;; Trail face pull: re-runs when the request changes (face entry,
            ;; expansion clicks, debounced epoch bumps re-stamp the request).
            (let [treq (e/watch !trail-request)]
              (when treq
                (reset! !trail-data
                        (case (:face treq)
                          :text
                          (let [params (second (:address treq))]
                            {:text (fv/TrailText (vec (:targets params))
                                                 (dissoc params :targets))})

                          :timeline
                          {:feed (fv/TrailFeed (:window treq)
                                               {:order (:order treq)})
                           :bundles (if (seq (:expanded treq))
                                      (let [b (fv/TrailBundle (vec (:expanded treq)) {})]
                                        (zipmap (:expanded treq) (repeat b)))
                                      {})}
                          nil))))
            ;; Faces-as-assemblies · the ONE generic face pull (CONTRACT §7, trap T8).
            ;; GENERIC: no `case` on the face — FacePull hands the whole request to the
            ;; server projection registry and returns ONE data-context; we reset ONE
            ;; !face-data atom. The Electric surface never grows per face.
            (let [freq (e/watch !face-request)]
              (when freq
                (reset! !face-data (fv/FacePull freq))))
            ;; W2: assembly-source pull (wear-time; face_wiring compiles the
            ;; served source client-side — T18: one .cljc compiler, two call
            ;; sites) and face-list pull (the sidebar roster from Rama, T14).
            ;; Same generic FacePull; no face-keyword dispatch here.
            (let [areq (e/watch !assembly-request)]
              (when areq
                (reset! !assembly-data (fv/FacePull areq))))
            (let [lreq (e/watch !face-list-request)]
              (when lreq
                (reset! !face-list-data (fv/FacePull lreq))))
            (let [mreq (e/watch !facet-materials-request)]
              (when mreq
                (reset! !facet-materials-data (fv/FacePull mreq))))
            (let [ireq (e/watch !material-inspector-request)]
              (when ireq
                (reset! !material-inspector-data (fv/FacePull ireq))))
            (let [breq (e/watch !interaction-table-request)]
              (when breq
                (reset! !interaction-table-data (fv/FacePull breq))))
            ;; P7: ONE pull, every portal answer. The server projection joins
            ;; five sub-projections behind this single request — nothing here
            ;; iterates masters, wearers or revisions.
            (let [preq (e/watch !material-portal-request)]
              (when preq
                (reset! !material-portal-data (fv/FacePull preq))))
            ;; W2: the wear write path — outbox value in, result mirrored back;
            ;; face_wiring clears the outbox on result (depth-1 queue by design,
            ;; recorded in W2-INT). Idempotent server-side by wear-id journal.
            (let [wear (e/watch !face-wear-outbox)]
              (when wear
                (reset! !face-wear-result (fv/RecordFaceWear wear))))
            ;; block-write Lane A · the edit write path (CONTRACT §3). Same shape as
            ;; the wear outbox: envelope in → server submit-block-edit! → plain result
            ;; mirrored back. The accept-side epoch bump inside submit-block-edit! (BW-T9)
            ;; drives the existing WatchIngestEpoch → debounced FacePull re-read; this
            ;; is NOT a second epoch channel. Lane B clears the outbox on result.
            (let [edit (e/watch !block-edit-outbox)]
              (when edit
                (reset! !block-edit-result (SubmitBlockEdit edit))))
            ;; block-write INT · the single-unit truth pull (§5 narrowing).
            (let [breq (e/watch !block-truth-request)]
              (when breq
                (reset! !block-truth-data (fv/FacePull breq))))
            ;; Sidebar visible: default true, but respect persisted workspace truth.
            ;; Must be initialized AFTER workspace truth loads so install-sidebar-watch!
            ;; sees the correct initial value and doesn't auto-show a hidden sidebar.
            (let [!sidebar-visible (atom (get @!workspace-truth :sidebar-visible true))
                  !file-load-request (atom nil)]
            (when resources
              (let [device (get resources :device)
                    format (get resources :format)
                    font-manifest (get resources :font-manifest)
                    font-config (get resources :font-config)
                    font-assets (get resources :font-assets)
                    pipelines (editor/create-editor-state resources)]
                (js/console.log "[BOOT] Client resources ready"
                                {:font-id (:id font-config)
                                 :font-backend (:backend font-assets)
                                 :font-manifest-count (count (:fonts font-manifest))
                                 :format format})

                (let [lines (str/split-lines file-content)
                      tokenized-lines (mapv tokenize-line lines)
                      gutter-w 40
                      layout-x (+ 50 gutter-w)
                      font-defaults (:defaults font-config)
                      font-size (or (:fontSize font-defaults) 19)
                      dpr (or (.-devicePixelRatio js/window) 1)
                      snap-step (/ 1 dpr)
                      snap (fn [v] (* (Math/round (/ v snap-step)) snap-step))
                      char-width (or (:charWidth font-config) 0.56)
                      char-advance (snap (* font-size char-width))
                      line-h (snap (* font-size (or (:lineHeight font-defaults) 1.2)))
                      layout-result (layout-tokens tokenized-lines layout-x 100 font-size [] #{} char-advance line-h)
                      render-ops (:render-ops layout-result)
                      line-lengths (mapv count lines)]

                  (let [geometry (Prepare-Geometry device pipelines render-ops font-assets font-config)]

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
                          (let [boot-w (max 1 (.-clientWidth dom/node))
                                boot-h (max 1 (.-clientHeight dom/node))
                                boot-dpr (or (.-devicePixelRatio js/window) 1)]
                            (set! (.-width dom/node) (Math/floor (* boot-w boot-dpr)))
                            (set! (.-height dom/node) (Math/floor (* boot-h boot-dpr)))
                            (js/console.log "[BOOT] Configuring WebGPU canvas"
                                            (str "{\"clientWidth\":" (.-clientWidth dom/node)
                                                 ",\"clientHeight\":" (.-clientHeight dom/node)
                                                 ",\"devicePixelRatio\":" (or (.-devicePixelRatio js/window) 1)
                                                 ",\"format\":\"" format "\""
                                                 ",\"copyDst\":true}")))
                          (.configure ^js ctx
                            (clj->js {:device device
                                      :format format
                                      :alphaMode "premultiplied"}))
                          (js/console.log "[BOOT] Starting runtime loop"
                                          {:initial-file (:path file-info)
                                           :font-id (:id font-config)
                                           :font-backend (:backend font-assets)})
                          (e/Task (loop/start-loop! dom/node device ctx geometry line-lengths
                                                    lines tokenize-line layout-tokens
                                                    find-matching-bracket detect-fold-regions
                                                    find-form-at-cursor sci-eval-form font-assets
                                                    :font-manifest font-manifest
                                                    :gpu-budget (:gpu-budget resources)
                                                    :!sidebar-visible !sidebar-visible
                                                    :!file-load-request !file-load-request
                                                    :!preview-el preview-el-atom
                                                    :!remote-sidebar-truth !sidebar-truth
                                                    :!remote-settings-truth !settings-truth
                                                    :!remote-agent-trail !agent-trail-truth
                                                    :!remote-flow-session !flow-session-truth
                                                    :!remote-workspace-truth !workspace-truth
                                                    :!trail-request !trail-request
                                                    :!trail-data !trail-data
                                                    :!face-request !face-request
                                                    :!face-data !face-data
                                                    :!assembly-request !assembly-request
                                                    :!assembly-data !assembly-data
                                                    :!face-list-request !face-list-request
                                                    :!face-list-data !face-list-data
                                                    :!facet-materials-request !facet-materials-request
                                                    :!facet-materials-data !facet-materials-data
                                                    :!material-inspector-request !material-inspector-request
                                                    :!material-inspector-data !material-inspector-data
                                                    :!interaction-table-request !interaction-table-request
                                                    :!interaction-table-data !interaction-table-data
                                                    :!material-portal-request !material-portal-request
                                                    :!material-portal-data !material-portal-data
                                                    :!face-wear-outbox !face-wear-outbox
                                                    :!face-wear-result !face-wear-result
                                                    ;; block-write Lane A · edit write seam threaded to the client
                                                    ;; runtime (Lane B destructures these in runtime.cljs at INT;
                                                    ;; start-loop! varargs ignores them until then — safe additive).
                                                    :!block-edit-outbox !block-edit-outbox
                                                    :!block-edit-result !block-edit-result
                                                    :!block-truth-request !block-truth-request
                                                    :!block-truth-data !block-truth-data
                                                    :!ingest-epoch-remote !ingest-epoch-remote
                                                    :initial-file file-info))))))))))))))))
