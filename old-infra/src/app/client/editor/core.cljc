(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.parser :refer [parse-doc parse-markdown parse-text nested-bold parsed->canvas-text]]
            [app.client.editor.events.click :refer [blinker-cursor]]
            [app.client.utils :refer [viewbox ui-mode subscribe]]
            [app.client.editor.events.utils :refer [editor-text letter-width c-x c-y pos]]
            [clojure.string :as string]
            #?@(:cljs [[app.client.editor.events.utils :refer [settings !pos initialise-canvas]]
                       [app.client.editor.events.click :refer [on-click]]])))

(def long-text "AAAAn h1 header\\\\\\\\n============\\\\\\\\n\\\\\\\\nParagraphs are separated by a blank line.\\\\\\\\n\\\\\\\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\\\\\\\nlook like:\\\\\\\\n\\\\\\\\n  * this one\\\\\\\\n  * that one\\\\\\\\n  * the other one\\\\\\\\n\\\\\\\\nNote that --- not considering the asterisk --- the actual text\\\\\\\\ncontent starts at 4-columns in.\\\\\\\\n\\\\\\\\n> Block quotes are\\\\\\\\n> written like so.\\\\\\\\n>\\\\\\\\n> They can span multiple paragraphs,\\\\\\\\n> if you like.\\\\\\\\n\\\\\\\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\\\\\\\\\\\\\"it's all\\\\\\\\nin chapters 12--14\\\\\\\\\\\\\\\"). Three dots ... will be converted to an ellipsis.\\\\\\\\nUnicode is supported. ☺\\\\\\\\n\\\\\\\\n\\\\\\\\n\\\\\\\\nAn h2 header\\\\\\\\n------------\\\\\\\\n\\\\\\\\nHere's a numbered list:\\\\\\\\n\\\\\\\\n 1. first item\\\\\\\\n 2. second item\\\\\\\\n 3. third item\\\\\\\\n\\\\\\\\nNote again how the actual text starts at 4 columns in (4 characters\\\\\\\\nfrom the left side). Here's a code sample:\\\\\\\\n\\\\\\\\n    # Let me re-iterate ...\\\\\\\\n    for i in 1 .. 10 { do-something(i) }\\\\\\\\n\\\\\\\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\\\\\\\nindenting the block, you can use delimited blocks, if you like:n h1 header\\\\n============\\\\n\\\\nParagraphs are separated by a blank line.\\\\n\\\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\\\nlook like:\\\\n\\\\n  * this one\\\\n  * that one\\\\n  * the other one\\\\n\\\\nNote that --- not considering the asterisk --- the actual text\\\\ncontent starts at 4-columns in.\\\\n\\\\n> Block quotes are\\\\n> written like so.\\\\n>\\\\n> They can span multiple paragraphs,\\\\n> if you like.\\\\n\\\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\\\\\"it's all\\\\nin chapters 12--14\\\\\\\"). Three dots ... will be converted to an ellipsis.\\\\nUnicode is supported. ☺\\\\n\\\\n\\\\n\\\\nAn h2 header\\\\n------------\\\\n\\\\nHere's a numbered list:\\\\n\\\\n 1. first item\\\\n 2. second item\\\\n 3. third item\\\\n\\\\nNote again how the actual text starts at 4 columns in (4 characters\\\\nfrom the left side). Here's a code sample:\\\\n\\\\n    # Let me re-iterate ...\\\\n    for i in 1 .. 10 { do-something(i) }\\\\n\\\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\\\nindenting the block, you can use delimited blocks, if you like:An h1 header\\\\n============\\\\n\\\\nParagraphs are separated by a blank line.\\\\n\\\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\\\nlook like:\\\\n\\\\n  * this one\\\\n  * that one\\\\n  * the other one\\\\n\\\\nNote that --- not considering the asterisk --- the actual text\\\\ncontent starts at 4-columns in.\\\\n\\\\n> Block quotes are\\\\n> written like so.\\\\n>\\\\n> They can span multiple paragraphs,\\\\n> if you like.\\\\n\\\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\\\\\"it's all\\\\nin chapters 12--14\\\\\\\"). Three dots ... will be converted to an ellipsis.\\\\nUnicode is supported. ☺\\\\n\\\\n\\\\n\\\\nAn h2 header\\\\n------------\\\\n\\\\nHere's a numbered list:\\\\n\\\\n 1. first item\\\\n 2. second item\\\\n 3. third item\\\\n\\\\nNote again how the actual text starts at 4 columns in (4 characters\\\\nfrom the left side). Here's a code sample:\\\\n\\\\n    # Let me re-iterate ...\\\\n    for i in 1 .. 10 { do-something(i) }\\\\n\\\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\\\nindenting the block, you can use delimited blocks, if you like:n h1 header\\n============\\n\\nParagraphs are separated by a blank line.\\n\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\nlook like:\\n\\n  * this one\\n  * that one\\n  * the other one\\n\\nNote that --- not considering the asterisk --- the actual text\\ncontent starts at 4-columns in.\\n\\n> Block quotes are\\n> written like so.\\n>\\n> They can span multiple paragraphs,\\n> if you like.\\n\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\"it's all\\nin chapters 12--14\\\"). Three dots ... will be converted to an ellipsis.\\nUnicode is supported. ☺\\n\\n\\n\\nAn h2 header\\n------------\\n\\nHere's a numbered list:\\n\\n 1. first item\\n 2. second item\\n 3. third item\\n\\nNote again how the actual text starts at 4 columns in (4 characters\\nfrom the left side). Here's a code sample:\\n\\n    # Let me re-iterate ...\\n    for i in 1 .. 10 { do-something(i) }\\n\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\nindenting the block, you can use delimited blocks, if you like:AAn h1 header\\\\n============\\\\n\\\\nParagraphs are separated by a blank line.\\\\n\\\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\\\nlook like:\\\\n\\\\n  * this one\\\\n  * that one\\\\n  * the other one\\\\n\\\\nNote that --- not considering the asterisk --- the actual text\\\\ncontent starts at 4-columns in.\\\\n\\\\n> Block quotes are\\\\n> written like so.\\\\n>\\\\n> They can span multiple paragraphs,\\\\n> if you like.\\\\n\\\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\\\\\"it's all\\\\nin chapters 12--14\\\\\\\"). Three dots ... will be converted to an ellipsis.\\\\nUnicode is supported. ☺\\\\n\\\\n\\\\n\\\\nAn h2 header\\\\n------------\\\\n\\\\nHere's a numbered list:\\\\n\\\\n 1. first item\\\\n 2. second item\\\\n 3. third item\\\\n\\\\nNote again how the actual text starts at 4 columns in (4 characters\\\\nfrom the left side). Here's a code sample:\\\\n\\\\n    # Let me re-iterate ...\\\\n    for i in 1 .. 10 { do-something(i) }\\\\n\\\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\\\nindenting the block, you can use delimited blocks, if you like:n h1 header\\n============\\n\\nParagraphs are separated by a blank line.\\n\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\nlook like:\\n\\n  * this one\\n  * that one\\n  * the other one\\n\\nNote that --- not considering the asterisk --- the actual text\\ncontent starts at 4-columns in.\\n\\n> Block quotes are\\n> written like so.\\n>\\n> They can span multiple paragraphs,\\n> if you like.\\n\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\"it's all\\nin chapters 12--14\\\"). Three dots ... will be converted to an ellipsis.\\nUnicode is supported. ☺\\n\\n\\n\\nAn h2 header\\n------------\\n\\nHere's a numbered list:\\n\\n 1. first item\\n 2. second item\\n 3. third item\\n\\nNote again how the actual text starts at 4 columns in (4 characters\\nfrom the left side). Here's a code sample:\\n\\n    # Let me re-iterate ...\\n    for i in 1 .. 10 { do-something(i) }\\n\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\nindenting the block, you can use delimited blocks, if you like:An h1 header\\n============\\n\\nParagraphs are separated by a blank line.\\n\\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\\nlook like:\\n\\n  * this one\\n  * that one\\n  * the other one\\n\\nNote that --- not considering the asterisk --- the actual text\\ncontent starts at 4-columns in.\\n\\n> Block quotes are\\n> written like so.\\n>\\n> They can span multiple paragraphs,\\n> if you like.\\n\\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \\\"it's all\\nin chapters 12--14\\\"). Three dots ... will be converted to an ellipsis.\\nUnicode is supported. ☺\\n\\n\\n\\nAn h2 header\\n------------\\n\\nHere's a numbered list:\\n\\n 1. first item\\n 2. second item\\n 3. third item\\n\\nNote again how the actual text starts at 4 columns in (4 characters\\nfrom the left side). Here's a code sample:\\n\\n    # Let me re-iterate ...\\n    for i in 1 .. 10 { do-something(i) }\\n\\nAs you probably guessed, indented 4 spaces. By the way, instead of\\nindenting the block, you can use delimited blocks, if you like:n h1 header\n============\n\nParagraphs are separated by a blank line.\n\n2nd paragraph. *Italic*, **bold**, and `monospace`. Itemized lists\nlook like:\n\n  * this one\n  * that one\n  * the other one\n\nNote that --- not considering the asterisk --- the actual text\ncontent starts at 4-columns in.\n\n> Block quotes are\n> written like so.\n>\n> They can span multiple paragraphs,\n> if you like.\n\nUse 3 dashes for an em-dash. Use 2 dashes for ranges (ex., \"it's all\nin chapters 12--14\"). Three dots ... will be converted to an ellipsis.\nUnicode is supported. ☺\n\n\n\nAn h2 header\n------------\n\nHere's a numbered list:\n\n 1. first item\n 2. second item\n 3. third item\n\nNote again how the actual text starts at 4 columns in (4 characters\nfrom the left side). Here's a code sample:\n\n    # Let me re-iterate ...\n    for i in 1 .. 10 { do-something(i) }\n\nAs you probably guessed, indented 4 spaces. By the way, instead of\nindenting the block, you can use delimited blocks, if you like:")

#?(:cljs (defn oc []
           (println "clicked the canvas.")))

(e/defn canvas [id]
  (e/client
    (let [id  (str "canvas-"id)
          dpr (or (.-devicePixelRatio js/window) 1)]
      (println "pos" pos "dpr" dpr)

      (dom/div
        (dom/props
          {:style {:overflow "scroll"}})
        (dom/canvas
          (dom/props {:id id
                      :tabIndex 0
                      :height 2000
                      :width 2000
                      :style {:border "1px solid red"
                              :margin "30px"
                              :background-color "beige"}})
          (let [el           (.getElementById js/document id)
                ctx          (.getContext el "2d")
                rect         (.getBoundingClientRect el)
                _            (initialise-canvas el rect dpr ctx)
                !text        (atom "Hello **world** ")
                text         (e/client (e/watch !text))
                !parsed-text (atom (parse-markdown text))
                !canvas-text (atom (parsed->canvas-text @!parsed-text ctx settings))
                canvas-text  (e/client (e/watch !canvas-text))
                x-pos        (atom 0)
                y-pos        (atom 20)]
            (reset! !text long-text)
            #_(parse-doc.)
            #_(blinker-cursor.)
            #_(parse-text.)
            #_(e/for-by
                (fn [x]
                  (println (str (first x) "--" (second x)))
                  (str (first x) "--" (second x)))
                [node (map-indexed vector @!text)]
                (println "sss" (second node))
                ;(println "node" node "-" @!text "-" (second node) @x-pos @y-pos (@letter-width (second node)))
                (.fillText ctx (second node) @x-pos @y-pos)
                (swap! !text str (second node))
                (if (> (+ @x-pos (@letter-width (second node))) @(:d-width settings))
                  (do
                    (reset! x-pos 20)
                    (reset! y-pos (+ @y-pos 20)))
                  (do
                    (reset! x-pos (+ @x-pos (@letter-width (second node)))))))

            (e/for-by (fn [x]
                        (let [ky (str (:x x) "--" (:y x) "--" (:text x)"--" (:type x))]
                         ;(println "for-by key: "  ky)
                         ky))
              [node canvas-text]
              (let [ch (:text node)
                    x (:x node)
                    y (:y node)
                    type (:type node)]
                (do
                  ;(println "---some change:--- " ch x y type @x-pos)
                  ;(println "c" ch x)
                  ;(println "canvas text")
                  (cond
                    (= :text  type) (set! (.-font ctx) "200  17px IA writer Quattro S")
                    (= :bold  type) (set! (.-font ctx) "bold 17px IA writer Quattro S"))
                  (if (> @x-pos x)
                    (do
                      ;(println "XPOS >>>>>>> X" @x-pos x (- @x-pos x))
                      (.clearRect ctx x @y-pos (- @x-pos x) 20)
                      (.fillText ctx ch x y)
                      (reset! x-pos x))
                    (do
                      ;(println "XPOS <<<<< X" @x-pos x)
                      (.fillText ctx ch x y)
                      (reset! x-pos x)
                      (reset! y-pos y))))))

            (dom/on! "click" (fn [e]
                               ;(println "4. clicked canvas")
                               #_(on-click. e)))
            (dom/on "keydown" (e/fn [e]
                                (let [key                (.-key e)
                                      code               (.-code e)]
                                  (println "code" code "key" key (@letter-width key))
                                  (cond
                                    (= "Space" code)           (swap! !text str " ")
                                    (= "Enter" code)           (swap! !text str "\n\n")
                                    (= "Backspace" code)       (do
                                                                 ;(println "backspace" @!text @x-pos)
                                                                 (reset! x-pos (- @x-pos (@letter-width (last @!text))))
                                                                 (.clearRect ctx @x-pos @y-pos (@letter-width (last @!text)) 20)
                                                                 ;(println "text before " @!text)
                                                                 (reset! !text (subs @!text 0 (dec (count @!text)))))
                                                                 ;(println "text after " @!text))
                                    (some? (@letter-width key)) (do
                                                                  (swap! !text str key #_(fn [s]
                                                                                           (str (subs s 0 1 ) "--" key (subs s 1 (count s))))))))))))))))


