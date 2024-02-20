(ns app.client.editor.parser
  (:require [clojure.string :as str]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [rc cursor-width cursor-height pos new-line-pos s-x s-y ctx calc-new-position c-x c-y]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos !curr-pos settings]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(defn bold [text]
  (let [stack (atom [])
        ctr   (atom 0)
        cnt   (count text)
        ns     (atom "")
        bold-map (atom [])]
    (while (< @ctr (- cnt 3))
      (println @ctr (nth text @ctr))

      (let [sb(subs text @ctr (+ 2 @ctr))
            n             (= "**" sb)
            add?          (and n (not= \space (nth text (+ 2 @ctr))))
            remove?       (when (> @ctr 0)
                            (and (not= \space (nth text (- @ctr 1)))
                              n))
            #_#_normal?       (and (empty? @stack)
                                (not n))]

        (when (or add? remove?)

          (println @ctr " --->" add? sb remove?)
          (do
            #_(println @ctr " --->" add? (subs text (- @ctr 2) (+ 1 @ctr)) remove?)
            #_(swap! bold-map conj @ns)
            (reset! ns "")))

        (cond
          add?  (do (swap! stack conj @ctr)
                    (swap! ctr + 2))
          remove? (do (when (not-empty @stack)
                          (swap! stack pop)
                          (if (empty? @stack)
                            (swap! bold-map conj (subs text (last @stack) (+ 2 @ctr)))))
                      (swap! ctr + 2))
          #_#_normal? (do (println @ctr "---" curr "-" normal? (subs text @ctr (+ 2 @ctr)) add? remove?)
                          (swap! ns str curr))
          :else (swap! ctr + 1))
        #_(if (= "**" (subs text @ctr (+ 2 @ctr)))
            (swap! ctr + 2)
           (swap! ctr + 1))))
    #_(swap! bold-map conj @ns)
    @bold-map))


(bold "**This is **bold** also** but **not** this * *or** *t*his**.")



(re-find #"(\*\*)(.*?)\1#" "this is **bold**")
(re-seq #"(?:(\*\*|__)(.+?)\1)" "**this is **bold** and __italic__**")

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

(let [sample-text "This is **bold** text and this is __also bold__ text."]
  (println (find-markdown-bold sample-text)))

