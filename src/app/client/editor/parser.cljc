(ns app.client.editor.parser
  (:require [clojure.string :as str]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.editor.data-schema :refer [long-text]]
            [app.client.flow-calc :as fc]
            [hyperfiddle.rcf :refer [tests tap %]]
            [app.client.editor.events.utils :refer [d-width letter-width c-width c-height calc-line-position editor-text add-text rc cursor-width cursor-height pos new-line-pos s-x s-y ctx calc-new-position c-x c-y]]
            #?(:cljs [app.client.editor.events.utils :refer [!editor-text !pos !curr-pos settings]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))

(def example-text "An h1 header\n============\n\nParagraphs are separated by a blank line.\n\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\nlook like:\n\n  * this one\n  * that one\n  * the other one\n\nNote that --- not considering the asterisk --- the actual text\ncontent starts at 4-columns in.\n\n> Block quotes are\n> written like so.\n>\n> They can span multiple paragraphs,\n> if you like.\n\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \"it's all\nin chapters 12--14\"). Three dots ... will be converted to an ellipsis.\nUnicode is supported. ☺\n\n\n\nAn h2 header\n------------\n\nHere's a numbered list:\n\n 1. first item\n 2. second item\n 3. third item\n\nNote again how the actual text starts at 4 columns in (4 characters\nfrom the left side). Here's a code sample:\n\n    # Let me re-iterate ...\n    for i in 1 .. 10 { do-something(i) }\n\nAs you probably guessed, indented 4 spaces. By the way, instead of\nindenting the block, you can use delimited blocks, if you like:")


(defn nested-bold [text]
  (let [stack (atom [])
        ctr   (atom 0)
        cnt   (count text)
        ns     (atom "")
        bold-map (atom [])]
    (while (< @ctr (- cnt 2))
      (let [sb            (subs text @ctr (+ 2 @ctr))
            n             (= "**" sb)
            add?          (and n (not= \space (nth text (+ 2 @ctr))))
            remove?       (boolean
                            (and
                              (when (> @ctr 1) (not= \space (nth text (- @ctr 1))))
                              (not-empty @stack)
                              n))
            normal?  (and (empty? @stack)
                       (not add?) (not remove?))]
        (when (and (not= "" @ns)
                (or add? remove?))
          (do
            (swap! bold-map conj {:type :text
                                  :content @ns})
            (reset! ns "")))
        (cond
          remove? (do (when (not-empty @stack)
                        (let [l (last @stack)]
                          (swap! stack pop)
                          (when (empty? @stack)
                            (swap! bold-map conj {:type :bold
                                                   :content (subs text l (+ 2 @ctr))}))))
                      (swap! ctr + 2))
          add?    (do (swap! stack conj @ctr)
                      (swap! ctr + 2))
          normal? (do (swap! ns str (nth text @ctr))
                      (swap! ctr + 1))
          :else   (swap! ctr + 1))))

    (if (not-empty @stack)
      (swap! bold-map conj {:type :text
                            :content (subs text (last @stack) cnt)})
      (swap!
            bold-map
            (fn [x]
              (let [lc (subs text @ctr cnt)
                    cnt (if (not= lc "**")
                          (str @ns lc)
                          @ns)]
                ;(println  "lc" lc)
                ;(println "ns" @ns)
                ;(println "cnt" (not-empty cnt))
                (if (not-empty cnt)
                  (conj x {:type :text
                           :content cnt})
                  x)))))

    @bold-map))

(tests
  (nested-bold "aabccdefghijklomnop  qrst  Shift**helloShift**") := [{:type :text, :content "aabccdefghijklomnop  qrst  Shift"}
                                                                     {:type :text, :content "**helloShift**"}]

  (nested-bold "  Shift**testingShift** looks bad very bad too mBackspaceba d ShiftI would say") := [{:type :text, :content "  Shift"}
                                                                                                     {:type :bold, :content "**testingShift**"}
                                                                                                     {:type :text, :content " looks bad very bad too mBackspaceba d ShiftI would say"}]

 (nested-bold "**This is **bold** also**, but **not** this * *or** *t*his**.") := [{:type :bold, :content "**This is **bold** also**"}
                                                                                   {:type :text, :content ", but "}
                                                                                   {:type :bold, :content "**not**"}
                                                                                   {:type :text, :content " this * *or** *t*his"}
                                                                                   {:type :text, :content "**."}])



(defn paragraph [text]
  (let [stack (atom [])
        ctr   (atom 0)
        cnt   (count text)
        ns     (atom "")
        bold-map (atom [])]
    (println "text" (subs  text @ctr (+ 2 @ctr)))
    (while (< @ctr (- cnt 1))
      (println "ctr" ctr)
      (let [match         "\n\n"
            current-char  (subs text @ctr (+ 2 @ctr))]
        (println match)
        (if (= match current-char)
          (do
            (swap! bold-map conj {:para @ns})
            (reset! ns "")
            (swap! ctr + 2))
          (do
            (println "ns" @ns)
            (swap! ns str (nth text @ctr))
            (swap! ctr + 1)))))
    (swap! bold-map conj {:para @ns})
    @bold-map))

#_(e/defn parsed->canvas-text [parsed-text ctx settings canvas-text]
    (let [;canvas-text (atom [])
          current-x   (atom 20)
          current-y   (atom 20)]
      (println "parsed->canvas-text" parsed-text)
      (doseq [node (reverse parsed-text)]
        (println "node: " node)
        (let [content (reverse (str (:content node)))
              type    (:type    node)]
          (cond
            (= :text  type) (set! (.-font ctx) "200  17px IA writer Quattro S")
            (= :bold  type) (set! (.-font ctx) "bold 17px IA writer Quattro S"))
          (e/for-by (fn [x]
                      (println "----" x)
                      (str (first x) "--" (second x) "--" type))
            [mi  (map-indexed vector content)]
            (let [ch         (second mi)
                  char-width (Math/round (.-width (.measureText ctx (str ch))))
                  new-line?  (> (+ @current-x char-width) @(:d-width settings))
                  new-x      (if new-line? 20 (+ @current-x char-width))
                  new-y      (if new-line? (+ @current-y 20) @current-y)]
              (println "char-width" char-width "new-x" new-x "new-y" new-y "ch" ch "type" type)
              (swap! canvas-text conj {:x @current-x
                                       :y @current-y
                                       :text ch
                                       :type type})
              (reset! current-x new-x)
              (reset! current-y new-y)))))
      (println "canvas text-->" @canvas-text)
      @canvas-text))


(defn parsed->canvas-text [parsed-text ctx settings]
  (let [canvas-text (atom [])
        current-x   (atom 20)
        current-y   (atom 20)]
    ;(println "parsed->canvas-text" parsed-text)
    (doseq [node parsed-text]
    ;  (println "node: " node)
      (let [content (str (:content node))
            type    (:type    node)]
        (cond
          (= :text  type) (set! (.-font ctx) "200  17px IA writer Quattro S")
          (= :bold  type) (set! (.-font ctx) "bold 17px IA writer Quattro S"))
        (doseq [ch content]
          (let [char-width (Math/round (.-width (.measureText ctx (str ch))))
                new-line?  (> (+ @current-x char-width) @(:d-width settings))
                new-x      (if new-line? 20 (+ @current-x char-width))
                new-y      (if new-line? (+ @current-y 20) @current-y)]
            ;(println "char-width" char-width "new-x" new-x "new-y" new-y "ch" ch "type" type)
            (swap! canvas-text conj {:x @current-x
                                     :y @current-y
                                     :text ch
                                     :type type})
            (reset! current-x new-x)
            (reset! current-y new-y)))))
    ;(println "canvas text-->" @canvas-text)
    @canvas-text))


#_(paragraph tex)


(e/defn parse-doc []
  (e/client
    (let [lines (str/split-lines "An h1 header\n============\n\nParagraphs are separated by a blank line.\n\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\nlook like:\n\n  * this one\n  * that one\n  * the other one\n\nNote that --- not considering the asterisk --- the actual text\ncontent starts at 4-columns in.\n\n> Block quotes are\n> written like so.\n>\n> They can span multiple paragraphs,\n> if you like.\n\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \"it's all\nin chapters 12--14\"). Three dots ... will be converted to an ellipsis.\nUnicode is supported. ☺\n\n\n\nAn h2 header\n------------\n\nHere's a numbered list:\n\n 1. first item\n 2. second item\n 3. third item\n\nNote again how the actual text starts at 4 columns in (4 characters\nfrom the left side). Here's a code sample:\n\n    # Let me re-iterate ...\n    for i in 1 .. 10 { do-something(i) }\n\nAs you probably guessed, indented 4 spaces. By the way, instead of\nindenting the block, you can use delimited blocks, if you like:")]
      (dorun (map-indexed
               (fn [idx line]
                 (do
                   (set! (.-font ctx) "17px IA writer Quattro S")
                   (set! (.-textBaseline ctx) "hanging")
                   (.fillText ctx line 10 (* (+ 1 idx) 20))))
               lines)))))
(defn find-markdown-bold [text]
  (re-seq #"(?:(\*\*|__)(.+?)\1)" text))

#_(let [sample-text "This is **bold** text and this is __also bold__ text."]
    (println (find-markdown-bold sample-text)))



(e/defn parse-text []
  (e/client
    (let [lx (atom 0)
          ly (atom 20)
          cw (into {} (map (fn [char]
                             [char (Math/round (.-width (.measureText ctx (str char))))])
                        (apply str (map char (range 32 127)))))
          calculate-width (fn [text]
                            (reduce + (map (fn [char]
                                             (cw char))
                                           text)))]
      (println "nested text" @!editor-text)
      (add-text. lx ly editor-text :text)
      #_(e/for-by key [tnode editor-text #_(nested-bold editor-text)]
          (println "tnode" tnode)
          #_(let [text (str (:content tnode))
                  type (:type    tnode)]
              (cond
               (= :text type) (add-text. lx ly text :text)
               (= :bold type) (add-text. lx ly text :bold)))))))
          ;(swap! lx + nx)
          ; (swap! ly + ny)))))))