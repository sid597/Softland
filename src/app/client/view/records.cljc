(ns app.client.view.records
  "The base records the first client stands in front of.
   Takes nothing. Gives a box font, the text tools as records over the view's
   table, one run of keystrokes, a foreign paste, the cursor, the first view,
   and the store holding them all. Holds immutable data.
   Evidence: run_test.clj; the browser entry (core.cljs) runs the same records.

   Every record carries :id :kind :by. A tool is {:tool parameters :inputs
   declared inputs :program the executor's program}; the runner (run.cljc)
   supplies the scope a view names."
  (:require [app.client.view.store :as store]))

;; ---------------------------------------------------------------- the font
(defn- glyph [advance box] {:advance advance :box box :ink 1})
(defn- glyphs [chars advance box] (into {} (for [c chars] [(str c) (glyph advance box)])))

(def font
  "A glyph is a box relative to the pen: y down, ascender at 0, baseline at 8,
   with an advance. Boxes stand in for outlines until text/shaper.cljs and
   the font file are bound as the shape capability."
  {:id "font-boxes@1" :kind :font :by "sid"
   :default (glyph 6 [1 3 4 5])
   :glyphs (merge (glyphs "acemnorsuvwxz" 6 [1 3 4 5])
                  (glyphs "bdhk" 6 [1 0 4 8])
                  (glyphs "ft" 4 [1 0 2 8])
                  (glyphs "gpqy" 6 [1 3 4 7])
                  (glyphs "l" 3 [1 0 1 8])
                  (glyphs "i" 3 [1 2 1 6])
                  (glyphs "j" 3 [1 2 1 7])
                  (glyphs "ABCDEFGHIJKLMNOPQRSTUVWXYZ" 7 [1 0 5 8])
                  (glyphs "0123456789" 6 [1 1 4 7])
                  {" " {:advance 3 :box [0 7 1 1] :ink 0}
                   "." {:advance 2 :box [0 7 1 1] :ink 1}
                   "," {:advance 2 :box [0 7 1 2] :ink 1}
                   "-" {:advance 4 :box [0 5 3 1] :ink 1}
                   "'" {:advance 2 :box [0 0 1 2] :ink 1}})})

;; ---------------------------------------------------------------- the runs
(defn keystrokes
  "Text and a start time → one keystroke record per character, as typed."
  [text t0]
  (vec (map-indexed (fn [i ch] {:i i :ch (str ch) :t (+ t0 (* 90 i))}) text)))

(def run-1
  {:id "run-1" :kind :text/run :by "sid" :at [4 4]
   :keys (keystrokes "the tool under your fingers" 1000)})

(def run-2
  "A paste from outside: a run whose asserter is foreign."
  {:id "run-2" :kind :text/run :by "clipboard" :at [4 40]
   :keys (keystrokes "a paste from outside" 5000)})

(def cursor-1 {:id "cursor-1" :kind :cursor :by "sid" :in "run-1" :offset 8})

;; ---------------------------------------------------------------- the layout rule, as expressions
;; The pen for THIS glyph, computed once per item by the :value step and read
;; back as [:get :pen …]. The pieces are Clojure vars assembling one
;; expression each; the record holds the result.
(def scale
  "1 for the tool owner's own runs, the tool's foreign scale for anyone else's."
  [:if [:= [:get :g :by] [:get :tool :self]] 1 [:get :tool :foreign-scale]])
(def advance [:* [:get :g :advance] scale])
(def new-run? [:not [:= [:get :g :run] [:get :state :run]]])
(def fits
  "This glyph stays on the current line: no break, and it ends within the width."
  [:if [:= [:get :g :break] 1] false
   [:<= [:+ [:get :state :x] advance] [:+ [:get :g :at 0] [:get :tool :width]]]])
(def pen-x [:if new-run? [:get :g :at 0] [:if fits [:get :state :x] [:get :g :at 0]]])
(def pen-y [:if new-run? [:get :g :at 1] [:if fits [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]]])
(def in-cursor-run? [:= [:get :g :run] [:get :cursor :in]])
(def cursor-here? [:if in-cursor-run? [:= [:get :g :i] [:get :cursor :offset]] false])
(def cursor-after? [:if in-cursor-run? [:= [:+ [:get :g :i] 1] [:get :cursor :offset]] false])

(def pen
  "What the :value step names for this glyph: the pen, its scale and advance,
   the box corners, and whether the caret stands here or after it."
  {:x pen-x :y pen-y :scale scale :advance advance
   :x0 [:+ pen-x [:* [:get :g :box 0] scale]] :y0 [:+ pen-y [:* [:get :g :box 1] scale]]
   :w [:* [:get :g :box 2] scale] :h [:* [:get :g :box 3] scale]
   :here cursor-here? :after cursor-after?})

(defn- ring
  "Four corner expressions and a colour expression → one closed ring for the painter."
  [ax ay bx by rgba]
  {:closed? true :rgba rgba
   :anchors [{:p [ax ay]} {:p [bx ay]} {:p [bx by]} {:p [ax by]}]})

(defn- caret-ring
  "The caret bar standing at x on this glyph's line."
  [x]
  (ring x [:get :pen :y] [:+ x [:get :tool :caret-width]] [:+ [:get :pen :y] [:get :tool :caret-height]]
        [:get :tool :caret-ink]))

;; ---------------------------------------------------------------- the tools
(def query-tool
  "The view's subject: the records whose kind the where-clause names."
  {:id "query@1" :kind :tool :by "sid" :tool {}
   :program
   {:each {:items [:get :store] :item :r :fields [:id :kind :by]
           :state {:hits [:literal []]}
           :steps [{:out :hits :op :collect
                    :args {:into [:get :state :hits]
                           :item [:if [:= [:get :r :kind] [:get :where :kind]] [:get :r :id] [:literal nil]]}}]
           :next {:hits [:get :hits]}}
    :return {:runs {:ids [:get :state :hits]}}}})

(def layout-tool
  "Keystrokes of the queried runs → placements (for the hit), the rings to
   paint (each glyph's box in the tool's ink, and the caret bar in the
   caret's ink, at the cursor's place), and where the caret stands."
  {:id "layout@1" :kind :tool :by "sid"
   :tool {:width 100 :line-height 10 :self "sid" :foreign-scale 0.5
          :ink [0 0 0 1] :caret-ink [0.85 0.1 0.1 1] :caret-width 1 :caret-height 8}
   :inputs {:runs {:kind :run-ids :from {:record "query@1" :output :runs}}}
   :program
   {:steps [{:out :shaped :op :text/shape
             :args {:ids [:get :inputs :runs :ids] :store [:get :store] :font [:get :font]}}]
    :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink :break :run :at :by]
           :state {:run [:literal nil] :x 0 :y 0
                   :placed [:literal []] :rings [:literal []] :caret [:literal nil]}
           :steps [{:out :pen :op :value :args {:value pen}}
                   {:out :placed :op :collect
                    :args {:into [:get :state :placed]
                           :item {:i [:get :g :i] :ch [:get :g :ch] :ink [:get :g :ink]
                                  :rect [[:get :pen :x0] [:get :pen :y0] [:get :pen :w] [:get :pen :h]]
                                  :pen [[:get :pen :x] [:get :pen :y]]
                                  :run [:get :g :run] :by [:get :g :by]}}}
                   {:out :inked :op :collect
                    :args {:into [:get :state :rings]
                           :item [:if [:= [:get :g :ink] 0] [:literal nil]
                                  (ring [:get :pen :x0] [:get :pen :y0]
                                        [:+ [:get :pen :x0] [:get :pen :w]] [:+ [:get :pen :y0] [:get :pen :h]]
                                        [:get :tool :ink])]}}
                   {:out :rings :op :collect
                    :args {:into [:get :inked]
                           :item [:if [:get :pen :here] (caret-ring [:get :pen :x])
                                  [:if [:get :pen :after] (caret-ring [:+ [:get :pen :x] [:get :pen :advance]]) [:literal nil]]]}}]
           :next {:run [:get :g :run]
                  :x [:+ [:get :pen :x] [:get :pen :advance]] :y [:get :pen :y]
                  :placed [:get :placed] :rings [:get :rings]
                  :caret [:if [:get :pen :here] [[:get :pen :x] [:get :pen :y]]
                          [:if [:get :pen :after] [[:+ [:get :pen :x] [:get :pen :advance]] [:get :pen :y]] [:get :state :caret]]]}}
    :return {:placements {:items [:get :state :placed]}
             :outline {:rings [:get :state :rings]}
             :caret {:at [:get :state :caret]}}}})

(def paint-tool
  "The rings of a layout → one painting on the view's surface, one paint
   per ring in the ring's colour. One region for the whole text was tried
   first: the CPU filler's coverage pass runs over the union box with every
   segment of the line in its band, 1.5 s per frame in the browser; a paint
   per ring copies the surface per ring but covers only that ring's box.
   The rings ride in through :items only, so the painting's subject does not
   reach :inputs and a later tool can take this painting through :inputs."
  {:id "paint@1" :kind :tool :by "sid" :tool {:rule :nonzero}
   :inputs {:outline {:kind :rings :from {:record "layout@1" :output :outline}}}
   :program
   {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
    :each {:items [:get :inputs :outline :rings] :item :ring :fields [:closed? :anchors :rgba]
           :state {:painting [:get :fresh]}
           :steps [{:out :shape :op :path/source
                    :args {:source {:kind :anchors :contours [[:get :ring]]} :tool {}}}
                   {:out :inked :op :paint
                    :args {:surface [:get :state :painting]
                           :region {:path [:get :shape :path] :rule [:get :tool :rule]}
                           :rgba [:get :ring :rgba] :opacity 1 :blend :source-over}}]
           :next {:painting [:get :inked]}}
    :return {:painting [:get :state :painting]}}})

(def inside?
  "The pointer's local point is within this placement's rect."
  [:if [:< [:get :point 0] [:get :p :rect 0]] false
   [:if [:< [:+ [:get :p :rect 0] [:get :p :rect 2]] [:get :point 0]] false
    [:if [:< [:get :point 1] [:get :p :rect 1]] false
     [:if [:< [:+ [:get :p :rect 1] [:get :p :rect 3]] [:get :point 1]] false true]]]])

(def hit-tool
  "A point over the placements → what is there, where, whose, drawn by which tool."
  {:id "hit@1" :kind :tool :by "sid" :tool {:painter "paint@1"}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:each {:items [:get :inputs :placements :items] :item :p :fields [:i :ch :rect :pen :run :by]
           :state {:hit [:literal nil]} :steps []
           :next {:hit [:if inside?
                        {:what :text/run :run [:get :p :run] :glyph [:get :p :ch] :index [:get :p :i]
                         :where [:get :p :pen] :by [:get :p :by] :drawn-by [:get :tool :painter]}
                        [:get :state :hit]]}}
    :return {:hit [:get :state :hit]}}})

;; ---------------------------------------------------------------- the view
(def view-1
  "Where Sid stands: which subject, which tools in which order, what is pinned
   under which scope name, at what zoom, and which view it came from."
  {:id "view-1" :kind :view :by "sid" :from nil
   :subject {:kind :text/run}
   :tools ["query@1" "layout@1" "paint@1"]
   :hit-tool "hit@1"
   :pins {:font "font-boxes@1" :cursor "cursor-1"}
   :zoom 4 :origin [0 0]})

(def store
  "The first store: every record above, by id."
  (reduce store/put (store/empty-store)
          [font run-1 run-2 cursor-1 query-tool layout-tool paint-tool hit-tool view-1]))
