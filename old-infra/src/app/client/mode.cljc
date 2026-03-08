(ns app.client.mode
  (:require [hyperfiddle.electric :as e]))

#?(:cljs (def dark-mode
           {:svg-background "#111110"
            :svg-dots "#3B3A37"
            :editor-background "#111110" #_"#191919"
            :editor-text "#6F6D66"
            :editor-border "#2A2A28"
            :button-background "#222221"
            :button-border "#3C2E69"
            :button-text "#6F6D66"
            :button-div "#33255B"
            :edge-color "#71FF8F4B"
            :context-menu "#1B1A17"
            :context-menu-text "#1B1A17"}))
#?(:cljs (def light-mode
           {:svg-background "#f5f5f5"
            :svg-dots "#b9bdc4"
            :editor-background "lightblue"
            :editor-text "#75c9f3"
            :editor-border "#28A5FF72"
            :button-background "#1184FC33"
            :button-border "#977DFEA8"
            :button-text "#0D141F"
            :button-div "#FEBB0036"
            :edge-color "#71FF8F4B"
            :context-menu "#111a29"
            :context-menu-text "#75c8f2"}))

(e/defn theme [mode]
  (e/client
    (if (= :dark mode)
      dark-mode
      light-mode)))
