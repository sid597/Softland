(ns app.client.view.records
  "The base records the first client stands in front of.
   Takes nothing. Gives a box font, a TrueType font record, the text tools as
   records over the view's table, two runs of keystrokes (Sid's and a foreign
   paste), the cursor, the first view, and the store holding them all. Holds
   immutable data.
   Evidence: run_test.clj; the browser entry (core.cljs) runs the same records.

   Every record carries :id :kind :by. A tool is {:tool parameters :inputs
   declared inputs :program the executor's program}, marked :per :run when it
   runs once per run; the runner (run.cljc) supplies the scope a view names."
  (:require [app.client.view.store :as store]))

;; ---------------------------------------------------------------- the fonts
(defn- box-glyph
  "An advance and a box [x y w h] measured down from the ascender line, the
   baseline at 8 → the glyph in the same shape a TrueType glyph takes: font
   units with y up, an outline, a bounding box."
  [advance [x y w h]]
  (let [top (- 8 y) bottom (- 8 (+ y h))]
    {:advance advance :ink 1 :bbox [x bottom (+ x w) top]
     :outline {:subpaths [{:closed? true :start [x top]
                           :segments [{:kind :line :p [(+ x w) top]} {:kind :line :p [(+ x w) bottom]} {:kind :line :p [x bottom]}]}]}}))

(defn- box-glyphs [chars advance box] (into {} (for [c chars] [(str c) (box-glyph advance box)])))

(def blank-glyph {:advance 3 :ink 0 :bbox [0 0 0 0] :outline {:subpaths []}})

(def font-boxes
  "A fixture font of boxes: ten units per em, the ascender at 8, so at size
   10 a glyph's box is in local units. The same record shape as a TrueType
   font, with the glyphs inline."
  {:id "font-boxes@1" :kind :font :by "sid"
   :upem 10 :ascender 8 :descender -2 :line-gap 0
   :default (box-glyph 6 [1 3 4 5])
   :glyphs (merge (box-glyphs "acemnorsuvwxz" 6 [1 3 4 5])
                  (box-glyphs "bdhk" 6 [1 0 4 8])
                  (box-glyphs "ft" 4 [1 0 2 8])
                  (box-glyphs "gpqy" 6 [1 3 4 7])
                  (box-glyphs "l" 3 [1 0 1 8])
                  (box-glyphs "i" 3 [1 2 1 6])
                  (box-glyphs "j" 3 [1 2 1 7])
                  (box-glyphs "ABCDEFGHIJKLMNOPQRSTUVWXYZ" 7 [1 0 5 8])
                  (box-glyphs "0123456789" 6 [1 1 4 7])
                  {" " blank-glyph
                   "." (box-glyph 2 [0 7 1 1]) "," (box-glyph 2 [0 7 1 2])
                   "-" (box-glyph 4 [0 5 3 1]) "'" (box-glyph 2 [0 0 1 2])})})

(def font-noto
  "Noto Sans Regular from the repository's font assets: the record names the
   file; its metrics, digest and glyphs come from the file when it is loaded
   (text/truetype.cljc), and the runtime puts the completed record back."
  {:id "font-noto-sans@1" :kind :font :by "import:truetype"
   :source "/fonts/noto_sans_regular.ttf"})

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

(def cursor-1
  "Sid's cursor: an offset into a run's text, and an anchor when a range is
   selected (the selection is [min max) of the two)."
  {:id "cursor-1" :kind :cursor :by "sid" :in "run-1" :offset 8 :anchor nil})
(def cursor-2
  "An agent's cursor in Sid's run: two people on one subject have two."
  {:id "cursor-2" :kind :cursor :by "agent:claude" :in "run-1" :offset 4 :anchor nil})

;; ---------------------------------------------------------------- the layout rule, as expressions
;; The pen for THIS glyph, computed once per item by the :value step and read
;; back as [:get :pen …]. The pieces are Clojure vars assembling one
;; expression each; the record holds the result.
(def scale
  "1 for the tool owner's own runs, the tool's foreign scale for anyone else's."
  [:if [:= [:get :g :by] [:get :tool :self]] 1 [:get :tool :foreign-scale]])
(def fs
  "Font units to local units: the tool's size over the font's units per em,
   times the run's scale."
  [:* [:/ [:get :tool :size] [:get :font :upem]] scale])
(def advance [:* [:get :g :advance] fs])
(def new-run? [:not [:= [:get :g :run] [:get :state :run]]])
(def limit [:+ [:get :g :at 0] [:get :tool :width]])
(def glyph-fits [:<= [:+ [:get :state :x] advance] limit])
(def word-fits [:<= [:+ [:get :state :x] [:* [:get :g :word-advance] fs]] limit])
(def fits
  "This glyph stays on the current line: not a break; whitespace hangs past
   the width; a word starts here only if the whole word ends within the
   width, unless the line is empty, where a word wider than the line starts
   anyway and breaks where its glyphs no longer fit."
  [:if [:= [:get :g :break] 1] false
   [:if [:= [:get :g :ink] 0] true
    [:if [:= [:get :g :word-start] 1]
     [:if word-fits true [:if [:= [:get :state :x] [:get :g :at 0]] glyph-fits false]]
     glyph-fits]]])
(def pen-x [:if new-run? [:get :g :at 0] [:if fits [:get :state :x] [:get :g :at 0]]])
(def pen-y [:if new-run? [:get :g :at 1] [:if fits [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]]])
(def baseline [:+ pen-y [:* [:get :font :ascender] fs]])

(def pen
  "What the :value step names for this glyph: the pen, the baseline, the
   scale from font units, the advance, the bounding box in local units, and
   whether this glyph opened a new line."
  {:x pen-x :y pen-y :fs fs :baseline baseline :advance advance
   :x0 [:+ pen-x [:* [:get :g :bbox 0] fs]]
   :y0 [:- baseline [:* [:get :g :bbox 3] fs]]
   :w [:* [:- [:get :g :bbox 2] [:get :g :bbox 0]] fs]
   :h [:* [:- [:get :g :bbox 3] [:get :g :bbox 1]] fs]
   :wrapped [:if new-run? false [:not fits]]})

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

(def query-by-tool
  "A narrower subject: the records whose kind and asserter the where-clause
   names."
  {:id "query-by@1" :kind :tool :by "sid" :tool {}
   :program
   {:each {:items [:get :store] :item :r :fields [:id :kind :by]
           :state {:hits [:literal []]}
           :steps [{:out :hits :op :collect
                    :args {:into [:get :state :hits]
                           :item [:if [:= [:get :r :kind] [:get :where :kind]]
                                  [:if [:= [:get :r :by] [:get :where :by]] [:get :r :id] [:literal nil]]
                                  [:literal nil]]}}]
           :next {:hits [:get :hits]}}
    :return {:runs {:ids [:get :state :hits]}}}})

(def layout-tool
  "One run's keystrokes → placements (for the hit and the caret), the
   outlines to paint, each placed at its pen in the tool's ink, and the run's
   box (its origin, the wrap width, the lines it filled). One instance per
   run the query names: a key appended to a run is a tail append of that
   run's items, so its layout resumes with the new glyph alone. The cursor is
   not read here, so the recipe holds only the tool's parameters and the
   font's metrics."
  {:id "layout@1" :kind :tool :by "sid" :per :run
   :tool {:size 10 :line-height 14 :width 100 :self "sid" :foreign-scale 0.5 :ink [0 0 0 1]}
   :program
   {:steps [{:out :shaped :op :text/shape :args {:run [:get :run] :font [:get :font]}}]
    :each {:items [:get :shaped] :item :g
           :fields [:i :ch :advance :bbox :outline :ink :break :word-start :word-advance :run :at :by]
           :state {:run [:literal nil] :x 0 :y 0 :lines 1 :placed [:literal []] :rings [:literal []]}
           :steps [{:out :pen :op :value :args {:value pen}}
                   {:out :placed :op :collect
                    :args {:into [:get :state :placed]
                           :item {:i [:get :g :i] :ch [:get :g :ch] :ink [:get :g :ink]
                                  :rect [[:get :pen :x0] [:get :pen :y0] [:get :pen :w] [:get :pen :h]]
                                  :pen [[:get :pen :x] [:get :pen :y]] :advance [:get :pen :advance]
                                  :run [:get :g :run] :by [:get :g :by]}}}
                   {:out :shape :op :path/place
                    :args {:path [:get :g :outline] :at [[:get :pen :x] [:get :pen :baseline]] :scale [:get :pen :fs]}}
                   {:out :rings :op :collect
                    :args {:into [:get :state :rings]
                           :item [:if [:= [:get :g :ink] 0] [:literal nil] {:path [:get :shape] :rgba [:get :tool :ink]}]}}]
           :next {:run [:get :g :run]
                  :x [:+ [:get :pen :x] [:get :pen :advance]] :y [:get :pen :y]
                  :lines [:+ [:get :state :lines] [:if [:get :pen :wrapped] 1 0]]
                  :placed [:get :placed] :rings [:get :rings]}}
    :return {:placements {:items [:get :state :placed]}
             :outline {:rings [:get :state :rings]
                       :box {:x [:get :run :at 0] :y [:get :run :at 1]
                             :w [:+ [:get :tool :width] 2]
                             :h [:* [:get :state :lines] [:get :tool :line-height]]}}}}})

(def paint-tool
  "The placed outlines of a run's layout → that run's painting, one paint
   per outline in its colour, on a surface the runner sizes to the layout's
   box. One region for the whole text was tried first: the CPU filler's
   coverage pass runs over the union box with every segment of the line in
   its band, 1.5 s per frame in the browser; a paint per outline copies the
   surface per outline but covers only that outline's box, and a run's
   surface is small. The outlines ride in through :items only, so the
   painting's subject does not reach :inputs and a later tool can take this
   painting through :inputs."
  {:id "paint@1" :kind :tool :by "sid" :per :run :tool {:rule :nonzero}
   :inputs {:outline {:kind :rings :from {:record "layout@1" :output :outline}}}
   :program
   {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
    :each {:items [:get :inputs :outline :rings] :item :ring :fields [:path :rgba]
           :state {:painting [:get :fresh]}
           :steps [{:out :inked :op :paint
                    :args {:surface [:get :state :painting]
                           :region {:path [:get :ring :path] :rule [:get :tool :rule]}
                           :rgba [:get :ring :rgba] :opacity 1 :blend :source-over}}]
           :next {:painting [:get :inked]}}
    :return {:painting [:get :state :painting]}}})

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

(def selected?
  "This placement lies in the cursor's selection: the cursor's run, an
   anchor set, and the glyph's index within [min max) of anchor and offset."
  [:if [:= [:get :p :run] [:get :cursor :in]]
   [:if [:= [:get :cursor :anchor] [:literal nil]] false
    [:if [:< [:get :p :i] [:min [:get :cursor :anchor] [:get :cursor :offset]]] false
     [:< [:get :p :i] [:max [:get :cursor :anchor] [:get :cursor :offset]]]]]
   false])

(defn- box-ring
  "A placement's pen and advance → a box the height of the line, as a ring for the painter."
  [rgba]
  (let [x0 [:get :p :pen 0] y0 [:get :p :pen 1] x1 [:+ [:get :p :pen 0] [:get :p :advance]] y1 [:+ [:get :p :pen 1] [:get :tool :height]]]
    {:path {:subpaths [{:closed? true :start [x0 y0]
                        :segments [{:kind :line :p [x1 y0]} {:kind :line :p [x1 y1]} {:kind :line :p [x0 y1]}]}]}
     :rgba rgba}))

(def select-tool
  "The cursor's selection over the placements → a highlight box per
   selected glyph, in the tool's ink. Reads the cursor, so it reruns when
   the selection moves; the loop paints nothing."
  {:id "select@1" :kind :tool :by "sid" :per :run :tool {:ink [1 0.85 0.2 0.45] :height 14}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:each {:items [:get :inputs :placements :items] :item :p :fields [:i :run :pen :advance]
           :state {:rings [:literal []]}
           :steps [{:out :rings :op :collect
                    :args {:into [:get :state :rings]
                           :item [:if selected? (box-ring [:get :tool :ink]) [:literal nil]]}}]
           :next {:rings [:get :rings]}}
    :return {:highlight {:rings [:get :state :rings]}}}})

(def highlight-paint-tool
  "The selection's boxes over the run's painting → the painting with the
   boxes painted over the text, translucent. Nothing selected, nothing
   painted; the surface is copied once."
  {:id "highlight-paint@1" :kind :tool :by "sid" :per :run :tool {:rule :nonzero}
   :inputs {:highlight {:kind :rings :from {:record "select@1" :output :highlight}}
            :painting {:kind :painting :from {:record "paint@1" :output :painting}}}
   :program
   {:each {:items [:get :inputs :highlight :rings] :item :ring :fields [:path :rgba]
           :state {:painting [:get :inputs :painting]}
           :steps [{:out :inked :op :paint
                    :args {:surface [:get :state :painting]
                           :region {:path [:get :ring :path] :rule [:get :tool :rule]}
                           :rgba [:get :ring :rgba] :opacity 1 :blend :source-over}}]
           :next {:painting [:get :inked]}}
    :return {:painting [:get :state :painting]}}})

(def no-caret? [:= [:get :inputs :caret :at] [:literal nil]])

(def caret-paint-tool
  "The caret place over the run's painting → the painting with one bar in
   the caret's ink; no bar when no caret stands in this run."
  {:id "caret-paint@1" :kind :tool :by "sid" :per :run :tool {:ink [0.85 0.1 0.1 1] :width 1 :height 12}
   :inputs {:caret {:kind :point :from {:record "caret-place@1" :output :caret}}
            :painting {:kind :painting :from {:record "highlight-paint@1" :output :painting}}}
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

(def inside?
  "The pointer's local point is within this placement's advance box: from
   its pen to its pen plus its advance, the tool's height down from the pen.
   The advance box, not the ink, so a space and a narrow letter answer too
   and a selection dragged across words never falls between glyphs."
  [:if [:< [:get :point 0] [:get :p :pen 0]] false
   [:if [:< [:+ [:get :p :pen 0] [:get :p :advance]] [:get :point 0]] false
    [:if [:< [:get :point 1] [:get :p :pen 1]] false
     [:if [:< [:+ [:get :p :pen 1] [:get :tool :height]] [:get :point 1]] false true]]]])

(def hit-tool
  "A point over a run's placements → what is there, where, whose, drawn by
   which tool."
  {:id "hit@1" :kind :tool :by "sid" :per :run :tool {:painter "paint@1" :height 14}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:each {:items [:get :inputs :placements :items] :item :p :fields [:i :ch :rect :pen :advance :run :by]
           :state {:hit [:literal nil]} :steps []
           :next {:hit [:if inside?
                        {:what :text/run :run [:get :p :run] :glyph [:get :p :ch] :index [:get :p :i]
                         :where [:get :p :pen] :rect [:get :p :rect] :advance [:get :p :advance]
                         :by [:get :p :by] :drawn-by [:get :tool :painter]}
                        [:get :state :hit]]}}
    :return {:hit [:get :state :hit]}}})

;; ---------------------------------------------------------------- the view
(def view-1
  "Where Sid stands: which subject, which tools in which order (a tool
   marked :per :run runs once per run the subject names), what is pinned
   under which scope name, at what zoom, and which view it came from."
  {:id "view-1" :kind :view :by "sid" :from nil
   :subject {:kind :text/run}
   :tools ["query@1" "layout@1" "paint@1" "select@1" "highlight-paint@1" "caret-place@1" "caret-paint@1"]
   :hit-tool "hit@1"
   :pins {:font "font-noto-sans@1" :cursor "cursor-1"}
   :zoom 4 :origin [0 0]})

(def view-2
  "Where an agent stands, handed view-1 to look from: only Sid's runs, its
   own cursor, its own zoom. Two people on one subject are two views; the
   chain of :from is how it got here."
  {:id "view-2" :kind :view :by "agent:claude" :from "view-1"
   :subject {:kind :text/run :by "sid"}
   :tools ["query-by@1" "layout@1" "paint@1" "select@1" "highlight-paint@1" "caret-place@1" "caret-paint@1"]
   :hit-tool "hit@1"
   :pins {:font "font-noto-sans@1" :cursor "cursor-2"}
   :zoom 3 :origin [0 0]})

(def store
  "The first store: every record above, by id."
  (reduce store/put (store/empty-store)
          [font-boxes font-noto run-1 run-2 cursor-1 cursor-2
           query-tool query-by-tool layout-tool paint-tool select-tool highlight-paint-tool
           caret-place-tool caret-paint-tool hit-tool
           view-1 view-2]))
