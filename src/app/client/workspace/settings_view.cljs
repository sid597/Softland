(ns app.client.workspace.settings-view
  "Font/theme settings panel: defaults, sliders, rects, and text ops."
  (:require [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node]]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.ui-primitives :refer [dt]]
            [app.client.workspace.sidebar :refer [sidebar-w]]))

(defn manifest-defaults->settings [manifest-settings]
  (let [get-default (fn [k fallback]
                      (or (get-in manifest-settings [k :default]) fallback))]
    {:font-size (get-default :fontSize 19)
     :line-height (get-default :lineHeight 1.2)
     :px-range (get-default :pxRange 8)
     :sharpness (get-default :sharpness 0.0)
     :snap-to-pixel? (get-default :snapToPixel true)
     :show-diagnostics? (get-default :showDiagnostics false)
     :theme-id (get-default :theme :gruvbox-dark)}))

(defn compact-map [m]
  (into {} (filter (comp some? val) m)))

(defn font-defaults->settings [font]
  (let [defaults (:defaults font)]
    (when defaults
      (compact-map
        {:font-size (or (:fontSize defaults) (:font-size defaults))
         :line-height (or (:lineHeight defaults) (:line-height defaults))
         :px-range (or (:pxRange defaults) (:px-range defaults))
         :sharpness (or (:sharpness defaults) (:sharpness defaults))
         :snap-to-pixel? (or (:snapToPixel defaults) (:snap-to-pixel? defaults))
         :show-diagnostics? (or (:showDiagnostics defaults) (:show-diagnostics? defaults))}))))

(defn slider-specs [settings]
  [{:id :theme-id :label "Theme" :val (themes/theme-index (:theme-id settings)) :min 0 :max (dec (count themes/theme-list)) :discrete true}
   {:id :font-size :label "Font Size" :val (:font-size settings) :min 8 :max 40}
   {:id :line-height :label "Line Height" :val (:line-height settings) :min 1.0 :max 2.0}
   {:id :snap-to-pixel? :label "Snap" :val (if (:snap-to-pixel? settings) 1 0) :min 0 :max 1}
   {:id :show-diagnostics? :label "Diagnostics" :val (if (:show-diagnostics? settings) 1 0) :min 0 :max 1}])

(defn compute-settings-panel-rects
  "Pure function: compute settings panel rectangles (background + font list + sliders)"
  [settings focus viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [;; Panel dimensions - centered modal
          panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; Colors (Modern Dark Theme)
          bg-color       {:r 0.12 :g 0.12 :b 0.14 :a 1.0} ;; Opaque modal background
          border-color   {:r 0.25 :g 0.25 :b 0.28 :a 1.0}
          separator-color {:r 0.20 :g 0.20 :b 0.23 :a 1.0}
          
          item-hover     {:r 0.18 :g 0.18 :b 0.22 :a 1.0}
          item-selected  {:r 0.22 :g 0.22 :b 0.26 :a 1.0}
          item-active    {:r 0.15 :g 0.25 :b 0.40 :a 0.8}  ;; Blue-ish highlight for active focus

          slider-track   {:r 0.20 :g 0.20 :b 0.24 :a 1.0}
          slider-fill    {:r 0.40 :g 0.60 :b 0.85 :a 1.0}  ;; Accent Blue
          slider-thumb   {:r 0.90 :g 0.90 :b 0.95 :a 1.0}

          ;; State
          current-focus (or (:focus-section settings) :fonts) ;; :fonts or :sliders
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          
          ;; Left Pane (Fonts)
          left-pane-x panel-x
          left-pane-y panel-y
          
          ;; Right Pane (Sliders)
          right-pane-x (+ panel-x left-w)
          right-pane-y panel-y
          
          ;; Header
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-rects (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               
                               bg (cond
                                    (and selected? focused?) item-active
                                    selected?                item-selected
                                    :else                    nil)]
                           (when bg
                             {:x (+ left-pane-x 8)
                              :y item-y
                              :w (- left-w 16)
                              :h (- font-item-h 4)
                              :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)})))
                       available-fonts)

          ;; Slider List
          sliders (slider-specs settings)
          
          slider-item-h 64
          slider-rects (map-indexed
                         (fn [idx slider]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))

                                 ;; Calculate ratio
                                 range (- (:max slider) (:min slider))
                                 ratio (/ (- (:val slider) (:min slider)) range)

                                 ;; Background highlight
                                 bg (cond
                                      (and selected? focused?) item-active
                                      selected?                item-selected
                                      :else                    nil)

                                 ;; Track geometry
                                 track-x (+ right-pane-x 20)
                                 track-y (+ base-y 34)
                                 track-w (- right-w 40)
                                 track-h 4
                                 fill-w (* track-w ratio)]

                             (concat
                               ;; Item Background
                               (when bg
                                 [{:x (+ right-pane-x 8) :y base-y :w (- right-w 16) :h (- slider-item-h 8)
                                   :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)}])
                               
                               ;; Track Background
                               [{:x track-x :y track-y :w track-w :h track-h
                                 :r (:r slider-track) :g (:g slider-track) :b (:b slider-track) :a (:a slider-track)}]
                               
                               ;; Filled Track
                               [{:x track-x :y track-y :w fill-w :h track-h
                                 :r (:r slider-fill) :g (:g slider-fill) :b (:b slider-fill) :a (:a slider-fill)}]
                               
                               ;; Thumb/Knob
                               [{:x (+ track-x fill-w -3) :y (- track-y 5) :w 6 :h 14
                                 :r (:r slider-thumb) :g (:g slider-thumb) :b (:b slider-thumb) :a (:a slider-thumb)}])))
                         sliders)]

      (vec
        (concat
          ;; Main Background
          [{:x panel-x :y panel-y :w panel-w :h panel-h
            :r (:r bg-color) :g (:g bg-color) :b (:b bg-color) :a (:a bg-color)}]
            
          ;; Header Separator
          [{:x panel-x :y (+ panel-y header-h) :w panel-w :h 1
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]
            
          ;; Vertical Separator
          [{:x (+ panel-x left-w) :y (+ panel-y header-h) :w 1 :h (- panel-h header-h)
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]

          ;; Content
          (filter some? font-rects)
          (mapcat identity slider-rects))))))

(defn <settings-panel-rects
  "Derived flow: settings panel rectangles.
   REACTIVE: font-size comes from !settings."
  [!settings !focus !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings focus viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-rects settings focus viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !focus)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

(defn compute-settings-panel-text
  "Pure function: compute settings panel text elements"
  [settings viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; State
          current-focus (or (:focus-section settings) :fonts)
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          right-pane-x (+ panel-x left-w)
          
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-texts (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               text-y (+ item-y (/ font-item-h 2) (/ font-size 3)) ;; Approx center
                               
                               color (cond
                                       (and selected? focused?) {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                       selected?                {:r 0.9 :g 0.9 :b 0.9 :a 1.0}
                                       :else                    {:r 0.6 :g 0.6 :b 0.6 :a 1.0})]
                           {:text (:name font)
                            :type :text
                            :from 0 :to (count (:name font))
                            :x (+ panel-x 20)
                            :y text-y
                            :size font-size
                            :r (:r color) :g (:g color) :b (:b color) :a (:a color)}))
                       available-fonts)

          ;; Slider Labels
          sliders (slider-specs settings)
          slider-item-h 64
          dpr (or (:dpr viewport) 1)
          snap? (:snap-to-pixel? settings)
          char-w (maybe-snap (* font-size 0.56) dpr snap?)

          slider-texts (mapcat
                         (fn [[idx slider]]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))

                                 label-color (if (and selected? focused?)
                                               {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                               {:r 0.8 :g 0.8 :b 0.8 :a 1.0})
                                 val-color   (if (and selected? focused?)
                                               {:r 0.4 :g 0.7 :b 1.0 :a 1.0}
                                               {:r 0.5 :g 0.5 :b 0.5 :a 1.0})

                                 val-str (case (:id slider)
                                           :theme-id (let [tid (or (:theme-id settings) :gruvbox-dark)
                                                           theme (themes/get-theme tid)]
                                                       (or (:name theme) (name tid)))
                                           :line-height (.toFixed (:val slider) 1)
                                           :snap-to-pixel? (if (pos? (:val slider)) "On" "Off")
                                           :show-diagnostics? (if (pos? (:val slider)) "On" "Off")
                                           (str (:val slider)))
                                 ;; Right-align value text: x = right-edge - text-width
                                 val-text-w (* (count val-str) char-w)
                                 val-x-raw (- (+ right-pane-x right-w -20) val-text-w)
                                 label-end (+ right-pane-x 20 (* (count (:label slider)) char-w) 12)
                                 val-x (max label-end val-x-raw)]

                             [{:text (:label slider)
                               :type :text
                               :from 0 :to (count (:label slider))
                               :x (+ right-pane-x 20)
                               :y (+ base-y 22)
                               :size font-size
                               :r (:r label-color) :g (:g label-color) :b (:b label-color) :a (:a label-color)}

                              {:text val-str
                               :type :number
                               :from 0 :to (count val-str)
                               :x val-x
                               :y (+ base-y 22)
                               :size font-size
                               :r (:r val-color) :g (:g val-color) :b (:b val-color) :a (:a val-color)}]))
                         (map-indexed vector sliders))
          
          ;; Headers
          title-str  "Settings"
          title-text {:text title-str
                      :type :macro
                      :from 0 :to (count title-str)
                      :x (+ panel-x 20)
                      :y (+ panel-y 26)
                      :size (+ font-size 2)
                      :r 0.9 :g 0.9 :b 0.9 :a 1.0}
                      
          hint-str  "Tab: Switch Pane   Arrows: Navigate/Adjust"
          hint-text {:text hint-str
                     :type :comment
                     :from 0 :to (count hint-str)
                     :x (+ panel-x 20)
                     :y (+ panel-y panel-h -12)
                     :size (- font-size 2)
                     :r 0.5 :g 0.5 :b 0.5 :a 0.8}]

      (vec
        (concat
          [title-text]
          font-texts
          slider-texts
          [hint-text])))))

(defn <settings-panel-text
  "Derived flow: settings panel text elements.
   REACTIVE: font-size comes from !settings."
  [!settings !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-text settings viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

