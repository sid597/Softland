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
  "This glyph stays on the current line: not a break; whitespace hangs past
   the width; anything inked ends within it."
  [:if [:= [:get :g :break] 1] false
   [:if [:= [:get :g :ink] 0] true
    [:<= [:+ [:get :state :x] advance] [:+ [:get :g :at 0] [:get :tool :width]]]]])
(def pen-x [:if new-run? [:get :g :at 0] [:if fits [:get :state :x] [:get :g :at 0]]])
(def pen-y [:if new-run? [:get :g :at 1] [:if fits [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]]])

(def pen
  "What the :value step names for this glyph: the pen, its scale and advance,
   the box corners, and whether this glyph opened a new line."
  {:x pen-x :y pen-y :scale scale :advance advance
   :x0 [:+ pen-x [:* [:get :g :box 0] scale]] :y0 [:+ pen-y [:* [:get :g :box 1] scale]]
   :w [:* [:get :g :box 2] scale] :h [:* [:get :g :box 3] scale]
   :wrapped [:if new-run? false [:not fits]]})

(defn- ring
  "Four corner expressions and a colour expression → one closed ring for the painter."
  [ax ay bx by rgba]
  {:closed? true :rgba rgba
   :anchors [{:p [ax ay]} {:p [bx ay]} {:p [bx by]} {:p [ax by]}]})

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
  "One run's keystrokes → placements (for the hit and the caret), the rings
   to paint (each glyph's box in the tool's ink) and the run's box (its
   origin, the wrap width, the lines it filled). One instance per run the
   query names: a key appended to a run is a tail append of that run's
   items, so its layout resumes with the new glyph alone. The cursor is not
   read here, so the recipe holds only the tool's parameters."
  {:id "layout@1" :kind :tool :by "sid" :per :run
   :tool {:width 100 :line-height 10 :self "sid" :foreign-scale 0.5 :ink [0 0 0 1]}
   :program
   {:steps [{:out :shaped :op :text/shape :args {:run [:get :run] :font [:get :font]}}]
    :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink :break :run :at :by]
           :state {:run [:literal nil] :x 0 :y 0 :lines 1 :placed [:literal []] :rings [:literal []]}
           :steps [{:out :pen :op :value :args {:value pen}}
                   {:out :placed :op :collect
                    :args {:into [:get :state :placed]
                           :item {:i [:get :g :i] :ch [:get :g :ch] :ink [:get :g :ink]
                                  :rect [[:get :pen :x0] [:get :pen :y0] [:get :pen :w] [:get :pen :h]]
                                  :pen [[:get :pen :x] [:get :pen :y]] :advance [:get :pen :advance]
                                  :run [:get :g :run] :by [:get :g :by]}}}
                   {:out :rings :op :collect
                    :args {:into [:get :state :rings]
                           :item [:if [:= [:get :g :ink] 0] [:literal nil]
                                  (ring [:get :pen :x0] [:get :pen :y0]
                                        [:+ [:get :pen :x0] [:get :pen :w]] [:+ [:get :pen :y0] [:get :pen :h]]
                                        [:get :tool :ink])]}}]
           :next {:run [:get :g :run]
                  :x [:+ [:get :pen :x] [:get :pen :advance]] :y [:get :pen :y]
                  :lines [:+ [:get :state :lines] [:if [:get :pen :wrapped] 1 0]]
                  :placed [:get :placed] :rings [:get :rings]}}
    :return {:placements {:items [:get :state :placed]}
             :outline {:rings [:get :state :rings]
                       :box {:x [:get :run :at 0] :y [:get :run :at 1]
                             :w [:+ [:get :tool :width] 2]
                             :h [:* [:get :state :lines] [:get :tool :line-height]]}}}}})

(def caret-place-tool
  "The cursor over the placements → where the caret stands: at the glyph
   the offset names, or after the glyph before it. Reads the cursor, so it
   reruns when the cursor moves; the loop paints nothing."
  {:id "caret-place@1" :kind :tool :by "sid" :per :run :tool {}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:each {:items [:get :inputs :placements :items] :item :p :fields [:i :run :pen :advance]
           :state {:at [:literal nil]} :steps []
           :next {:at [:if [:= [:get :p :run] [:get :cursor :in]]
                       [:if [:= [:get :p :i] [:get :cursor :offset]] [:get :p :pen]
                        [:if [:= [:+ [:get :p :i] 1] [:get :cursor :offset]]
                         [[:+ [:get :p :pen 0] [:get :p :advance]] [:get :p :pen 1]]
                         [:get :state :at]]]
                       [:get :state :at]]}}
    :return {:caret {:at [:get :state :at]}}}})

(def no-caret? [:= [:get :inputs :caret :at] [:literal nil]])

(def caret-paint-tool
  "The caret place over the text's painting → the painting with one bar in
   the caret's ink; no bar when no caret stands."
  {:id "caret-paint@1" :kind :tool :by "sid" :per :run :tool {:ink [0.85 0.1 0.1 1] :width 1 :height 8}
   :inputs {:caret {:kind :point :from {:record "caret-place@1" :output :caret}}
            :painting {:kind :painting :from {:record "paint@1" :output :painting}}}
   :program
   {:steps [{:out :bar :op :path/source
             :args {:source {:kind :rect
                             :x [:if no-caret? 0 [:get :inputs :caret :at 0]]
                             :y [:if no-caret? 0 [:get :inputs :caret :at 1]]
                             :w [:get :tool :width] :h [:get :tool :height]}
                    :tool {}}}
            {:out :painted :op :paint
             :args {:surface [:get :inputs :painting] :region [:get :bar :path]
                    :rgba [:get :tool :ink] :opacity [:if no-caret? 0 1] :blend :source-over}}]
    :return {:painting [:get :painted]}}})

(def paint-tool
  "The rings of a run's layout → that run's painting, one paint per ring in
   the ring's colour, on a surface the runner sizes to the layout's box. One
   region for the whole text was tried first: the CPU filler's coverage pass
   runs over the union box with every segment of the line in its band, 1.5 s
   per frame in the browser; a paint per ring copies the surface per ring but
   covers only that ring's box, and a run's surface is small. The rings ride
   in through :items only, so the painting's subject does not reach :inputs
   and a later tool can take this painting through :inputs."
  {:id "paint@1" :kind :tool :by "sid" :per :run :tool {:rule :nonzero}
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
  {:id "hit@1" :kind :tool :by "sid" :per :run :tool {:painter "paint@1"}
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
  "Where Sid stands: which subject, which tools in which order (a tool
   marked :per :run runs once per run the subject names), what is pinned
   under which scope name, at what zoom, and which view it came from."
  {:id "view-1" :kind :view :by "sid" :from nil
   :subject {:kind :text/run}
   :tools ["query@1" "layout@1" "paint@1" "caret-place@1" "caret-paint@1"]
   :hit-tool "hit@1"
   :pins {:font "font-boxes@1" :cursor "cursor-1"}
   :zoom 4 :origin [0 0]})

(def store
  "The first store: every record above, by id."
  (reduce store/put (store/empty-store)
          [font run-1 run-2 cursor-1 query-tool layout-tool paint-tool caret-place-tool caret-paint-tool hit-tool view-1]))
