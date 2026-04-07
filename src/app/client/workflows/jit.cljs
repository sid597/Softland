(ns app.client.workflows.jit
  "JIT workflow: command parsing plus hardcoded preview artifacts."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :refer [rt-node]]
            [app.client.workspace.ui-primitives :refer [dt]]))

(defn parse-jit-command
  "Parse JIT workflow commands. Returns nil when the command does not belong
   to JIT."
  [trimmed]
  (cond
    (str/starts-with? trimmed "/extract ")
    (let [args (-> trimmed (subs (count "/extract ")) str/trim)]
      (if (str/blank? args)
        {:kind :error :message "Usage: /extract [url] [selector]\n  /extract https://site.com .card\n  /extract .card\n  /extract clear"}
        (if (= args "clear")
          {:kind :extract-component :clear? true}
          (let [parts (str/split args #"\s+" 2)
                first-part (first parts)]
            (if (str/starts-with? first-part "http")
              {:kind :extract-component :url first-part :selector (second parts)}
              {:kind :extract-component :selector args})))))

    (= trimmed "/extract")
    {:kind :extract-component}

    (str/starts-with? trimmed "/hardcode ")
    {:kind :hardcode :name (-> trimmed (subs (count "/hardcode ")) str/trim)}

    (= trimmed "/hardcode")
    {:kind :hardcode :name "button"}

    :else
    nil))

(defn hardcoded-preview-node
  "Return a hardcoded rt-node demo for the current JIT prototype."
  [name]
  (let [sc-primary [0.898 0.898 0.898 1.0]
        sc-primary-fg [0.09 0.09 0.09 1.0]
        sc-secondary [0.149 0.149 0.149 1.0]
        sc-secondary-fg [0.98 0.98 0.98 1.0]
        sc-destructive [1.0 0.392 0.404 1.0]
        sc-foreground [0.98 0.98 0.98 1.0]
        sc-fg-muted [0.831 0.831 0.831 1.0]
        sc-muted [0.149 0.149 0.149 1.0]
        sc-border [1.0 1.0 1.0 0.15]
        sc-bg [0.039 0.039 0.039 1.0]
        sc-radius 10
        sc-h 32
        sc-font 14
        make-btn (fn [id label bg-color fg-color & {:keys [border-w border-c]}]
                   (let [char-w (* sc-font 0.56)
                         text-w (* (count label) char-w)
                         btn-w (+ text-w 20)]
                     (rt-node id :button
                              {:x 0 :y 0 :w btn-w :h sc-h}
                              :style (cond-> {:bg bg-color
                                              :radius sc-radius}
                                       border-w (assoc :border-width border-w)
                                       border-c (assoc :border-color border-c))
                              :text [{:text label
                                      :type :text
                                      :from 0
                                      :to (count label)
                                      :x 10
                                      :y (+ sc-font 5)
                                      :size sc-font
                                      :r (nth fg-color 0)
                                      :g (nth fg-color 1)
                                      :b (nth fg-color 2)
                                      :a (nth fg-color 3)}])))
        section-label (fn [id text]
                        (rt-node id :text
                                 {:x 0 :y 0 :w 200 :h 18}
                                 :text [{:text text
                                         :type :keyword
                                         :from 0
                                         :to (count text)
                                         :x 0
                                         :y 13
                                         :size 12
                                         :r 0.55
                                         :g 0.55
                                         :b 0.60
                                         :a 1.0}]))]
    (case name
      "button"
      (rt-node :hc-button-demo :rect
               {:x 0 :y 0 :w 520 :h 500}
               :style {:bg sc-bg}
               :layout {:direction :column :padding [24 24 24 24] :gap 12}
               :children
               [(rt-node :hc-title :text
                         {:x 0 :y 0 :w 472 :h 24}
                         :text [{:text "shadcn/ui Button (v4) -- extracted from live page"
                                 :type :keyword :from 0 :to 49 :x 0 :y 17
                                 :size 15 :r 0.98 :g 0.98 :b 0.98 :a 1.0}])
                (section-label :hc-s1 "Variants")
                (rt-node :hc-variant-row :rect
                         {:x 0 :y 0 :w 472 :h sc-h}
                         :style {:bg [0 0 0 0]}
                         :layout {:direction :row :gap 10}
                         :children
                         [(make-btn :hc-default "Default" sc-primary sc-primary-fg)
                          (make-btn :hc-secondary "Secondary" sc-secondary sc-secondary-fg)
                          (make-btn :hc-destructive "Destructive"
                                    (assoc sc-destructive 3 0.2) sc-destructive)
                          (make-btn :hc-outline "Outline"
                                    [1.0 1.0 1.0 0.04] sc-fg-muted
                                    :border-w 1 :border-c sc-border)
                          (make-btn :hc-ghost "Ghost" [0 0 0 0] sc-fg-muted)
                          (make-btn :hc-link "Link" [0 0 0 0] [0.35 0.55 0.95 1.0])])
                (section-label :hc-s2 "Hover states (simulated)")
                (rt-node :hc-hover-row :rect
                         {:x 0 :y 0 :w 472 :h sc-h}
                         :style {:bg [0 0 0 0]}
                         :layout {:direction :row :gap 10}
                         :children
                         [(make-btn :hc-hov-default "Default"
                                    (assoc sc-primary 3 0.8) sc-primary-fg)
                          (make-btn :hc-hov-secondary "Secondary"
                                    (assoc sc-secondary 3 0.8) sc-secondary-fg)
                          (make-btn :hc-hov-destructive "Destructive"
                                    (assoc sc-destructive 3 0.3) sc-destructive)
                          (make-btn :hc-hov-outline "Outline"
                                    sc-muted sc-foreground
                                    :border-w 1 :border-c sc-border)
                          (make-btn :hc-hov-ghost "Ghost" sc-muted sc-foreground)
                          (make-btn :hc-hov-link "Link" [0 0 0 0] [0.35 0.55 0.95 1.0])])
                (section-label :hc-s3 "Sizes")
                (rt-node :hc-size-row :rect
                         {:x 0 :y 0 :w 472 :h 48}
                         :style {:bg [0 0 0 0]}
                         :layout {:direction :row :gap 10 :align :end}
                         :children
                         [(let [f 12 ch (* f 0.56) l "Extra Small" w (+ (* (count l) ch) 16)]
                            (rt-node :hc-sz-xs :button
                                     {:x 0 :y 0 :w w :h 24}
                                     :style {:bg sc-primary :radius 8}
                                     :text [{:text l :type :text :from 0 :to (count l)
                                             :x 8 :y 17 :size f
                                             :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))
                          (let [f 12 ch (* f 0.56) l "Small" w (+ (* (count l) ch) 20)]
                            (rt-node :hc-sz-sm :button
                                     {:x 0 :y 0 :w w :h 28}
                                     :style {:bg sc-primary :radius 8}
                                     :text [{:text l :type :text :from 0 :to (count l)
                                             :x 10 :y 19 :size f
                                             :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))
                          (make-btn :hc-sz-md "Default" sc-primary sc-primary-fg)
                          (let [f 14 ch (* f 0.56) l "Large" w (+ (* (count l) ch) 28)]
                            (rt-node :hc-sz-lg :button
                                     {:x 0 :y 0 :w w :h 40}
                                     :style {:bg sc-primary :radius 10}
                                     :text [{:text l :type :text :from 0 :to (count l)
                                             :x 14 :y 26 :size f
                                             :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))])
                (section-label :hc-s4 "Token mapping: shadcn v4 -> Softland dt")
                (rt-node :hc-token-info :text
                         {:x 0 :y 0 :w 472 :h 100}
                         :text [{:text "primary [229,229,229] -> dt :fg" :type :text
                                 :from 0 :to 30 :x 0 :y 14
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                {:text "secondary [38,38,38] -> dt :bg-muted" :type :text
                                 :from 0 :to 35 :x 0 :y 28
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                {:text "destructive [255,100,103] -> dt :destructive" :type :text
                                 :from 0 :to 44 :x 0 :y 42
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                {:text "background [10,10,10] -> dt :bg" :type :text
                                 :from 0 :to 30 :x 0 :y 56
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                {:text "foreground [250,250,250] -> dt :fg" :type :text
                                 :from 0 :to 33 :x 0 :y 70
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                {:text "radius: 10px | border: white@15% | font: 14/500" :type :text
                                 :from 0 :to 48 :x 0 :y 84
                                 :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])])

      (rt-node :hc-unknown :rect
               {:x 0 :y 0 :w 300 :h 60}
               :style {:bg (:bg-muted (:colors dt))}
               :text [{:text (str "Unknown: " name)
                       :type :text
                       :from 0
                       :to (+ 9 (count name))
                       :x 16
                       :y 36
                       :size 14
                       :r 0.90
                       :g 0.30
                       :b 0.30
                       :a 1.0}]))))

(defn handle-jit-command!
  "Apply JIT command side effects through the provided runtime env.
   Returns true when the command was handled."
  [parsed {:keys [!extract-preview show-flow-info!]}]
  (case (:kind parsed)
    :extract-component
    (do
      (if (:clear? parsed)
        (do
          (reset! !extract-preview nil)
          (show-flow-info! "Extract preview cleared."))
        (let [url (:url parsed)
              selector (:selector parsed)
              msg (cond
                    (and url selector) (str "Extract mode active.\nURL: " url "\nSelector: " selector
                                            "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                    url (str "Extract mode active.\nURL: " url "\nSelector: auto-detect"
                             "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                    selector (str "Extract mode active.\nSelector: " selector
                                  "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                    :else (str "Extract mode active.\nSelector: auto-detect"
                               "\n\nWaiting for extracted data...\nUse /extract clear to exit."))]
          (reset! !extract-preview {:pending? true :url url :selector selector})
          (show-flow-info! msg)))
      true)

    :hardcode
    (do
      (reset! !extract-preview {:rt-node (hardcoded-preview-node (:name parsed))})
      (show-flow-info! (str "Hardcoded: " (:name parsed)))
      true)

    false))
