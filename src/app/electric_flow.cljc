(ns app.electric-flow
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [app.client.webgpu.loop :as loop]
                       [global-flow :refer [await-promise]]
                       ["@lezer/lr" :as lr]
                       ["@nextjournal/lezer-clojure" :as clj-parser]])))

(def source-code 
  #?(:clj (slurp "src/app/electric_flow.cljc") 
     :cljs nil)) 

#?(:clj (defn init-lezer-parser! [] nil))

#?(:cljs
   (do
     (def lezer-parser (atom nil))
     
     (defn init-lezer-parser! []
       (when-not @lezer-parser
         (reset! lezer-parser (.-parser clj-parser))))

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
           (extract-tokens-from-syntax-tree tree line-text))))))

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

(defn layout-tokens [lines-of-tokens start-x start-y font-size]
  (let [char-width (* font-size 0.6)] 
    (:render-ops
      (reduce (fn [acc tokens]
                (let [line-y (:current-y acc)
                      line-ops (mapv (fn [token]
                                       (let [color (get-color (:type token))]
                                         (merge token color
                                                {:x (+ start-x (* (or (:from token) 0) char-width))
                                                 :y line-y
                                                 :size font-size})))
                                     tokens)]
                  {:current-y (+ line-y (* font-size 1.2))
                   :render-ops (conj (:render-ops acc) line-ops)}))
              {:current-y (+ start-y font-size) :render-ops []}
              lines-of-tokens))))

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
          
          (let [resources (LoadWebGPU)]
            (when resources
              (let [device (get resources :device)
                    format (get resources :format)
                    atlas (get resources :atlas)
                    pipelines (editor/create-editor-state resources)]
                
                (let [lines (str/split-lines file-content)
                      tokenized-lines (mapv tokenize-line lines)
                      render-ops (layout-tokens tokenized-lines 50 100 16)
                      
                      ;; NEW: Compute line lengths (character count per line)
                      line-lengths (mapv count lines)]
                  
                  (let [geometry (Prepare-Geometry device pipelines render-ops atlas)]
                    (dom/canvas
                      (dom/props {:id "webgpu-canvas" 
                                  :style {:width "100vw" :height "100vh" :display "block"}})
                      (let [ctx (.getContext dom/node "webgpu" (clj->js {:alpha true}))]
                        (.configure ^js ctx (clj->js {:device device :format format :alphaMode "premultiplied"}))
                        ;; Pass line-lengths to start-loop!
                        (let [loop-flow (e/Task (loop/start-loop! dom/node device ctx geometry line-lengths))]
                          (e/input loop-flow))))))))))))))
