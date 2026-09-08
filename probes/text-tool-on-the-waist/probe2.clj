;; probe2.clj — fixes to probe 3b/6, the ONE-SOURCE chain (layout → paint & hit through
;; :inputs), a caret record over the layout's output, and a micro-profile of a keystroke.
(ns probe2
  (:require [app.client.engine.executor :as e]
            [app.client.engine.surface :as s]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as c]
            [app.client.path.surface :as path-surface]
            [app.client.path.source :as source]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

(def out-dir "probes/text-tool-on-the-waist/")
(defn report [label m] (println (str "\n=== " label " ===")) (pp/pprint m) (flush))
(defmacro probe [label & body]
  `(try (report ~label (do ~@body))
        (catch Throwable t# (report ~label {:threw (.getMessage t#) :data (ex-data t#)}))))
(defn brief [p] (when (map? p) (select-keys p [:surface/id :revision :key :changed :width :height])))
(defn save-png! [name painting]
  (with-open [o (io/output-stream (str out-dir name))] (.write o (s/png-bytes painting))) name)
(defn sha [painting]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (java.security.MessageDigest/getInstance "SHA-256") (vb/float-bytes (:data painting))))))
(defn ms [f] (let [t (System/nanoTime)] (f) (/ (- (System/nanoTime) t) 1e6)))
(defn med [xs] (nth (sort xs) (quot (count xs) 2)))

(def font {:id "boxfont@1" :kind :font :by "sid"
           :glyphs {"a" {:advance 6 :box [1 3 4 5] :ink 1} "b" {:advance 6 :box [1 0 4 8] :ink 1}
                    "l" {:advance 3 :box [1 0 1 8] :ink 1} " " {:advance 3 :box [0 7 1 1] :ink 0}}})
(def keys-typed (vec (map-indexed (fn [i ch] {:i i :ch (str ch)}) "ball ab")))
(defn shape-by-lookup [font ks] (mapv (fn [{:keys [i ch]}] (merge {:i i :ch ch} (get-in font [:glyphs ch]))) ks))
(def run {:id "run-1" :kind :text/run :by "sid" :at [2 2]})
(def cursor {:id "cursor-1" :kind :cursor :by "sid" :in "run-1" :offset 3})
(defn painting [id zoom]
  {:surface/id id :width (int (* 34 zoom)) :height (int (* 22 zoom)) :domain {:kind :plane :map [zoom 0 0 zoom 0 0]}
   :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]})
(def shape-cap {:text/shape {:args [:keys :font] :needs [[:keys] [:font]] :run (fn [{:keys [keys font]} _] (shape-by-lookup font keys))}})
(def collect-cap {:collect {:args [:into :item] :needs [[:into] [:item]] :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}})
(def table (merge c/capabilities shape-cap collect-cap))

;; ------------------------------------------------ the text tool with its wrap rule as ONE parameter
(defn make-text-tool [fits]
  (let [pen-x [:if fits [:get :state :x] [:get :run :at 0]]
        pen-y [:if fits [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]]]
    {:id "text-tool@1" :kind :tool :by "sid" :tool {:width 30 :line-height 10 :ink [0 0 0 1]}
     :program
     {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}
              {:out :shaped :op :text/shape :args {:keys [:get :keys] :font [:get :font]}}]
      :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink]
             :state {:x [:get :run :at 0] :y [:get :run :at 1] :painting [:get :fresh] :caret [:literal nil]}
             :steps [{:out :box :op :path/source
                      :args {:source {:kind :rect :x [:+ pen-x [:get :g :box 0]] :y [:+ pen-y [:get :g :box 1]]
                                      :w [:get :g :box 2] :h [:get :g :box 3]} :tool {}}}
                     {:out :painted :op :paint :args {:surface [:get :state :painting] :region [:get :box :path]
                                                      :rgba [:get :tool :ink] :opacity [:get :g :ink] :blend :source-over}}]
             :next {:x [:+ pen-x [:get :g :advance]] :y pen-y :painting [:get :painted]
                    :caret [:if [:= [:get :g :i] [:get :cursor :offset]] [pen-x pen-y] [:get :state :caret]]}}
      :return {:painting [:get :state :painting]
               :caret [:if [:= [:get :state :caret] [:literal nil]] [[:get :state :x] [:get :state :y]] [:get :state :caret]]}}}))
(def wrap-rule [:<= [:+ [:get :state :x] [:get :g :advance]] [:+ [:get :run :at 0] [:get :tool :width]]])
(def text-tool (make-text-tool wrap-rule))
(defn tool-record [tool] {:program (:program tool) :roots {:tool (:tool tool)}})
(def vantage {:id "vantage-1" :kind :vantage :by "sid" :from nil :subject {:kind :text/run :by "sid"} :tools ["text-tool@1"] :pins ["cursor-1"] :zoom 1})
(defn scope-of [v ks] {:run run :cursor cursor :keys ks :font font :painting (painting (str (:id v) "/text") (:zoom v))})
(def scope (scope-of vantage keys-typed))

(probe "6' the wrap rule is one record field now: edit it, the text redraws, caret and kind hold"
  (let [a (e/run (tool-record text-tool) scope table)
        no-wrap (e/run (tool-record (make-text-tool true)) scope table)
        by-char-width-20 (e/run (tool-record (assoc-in text-tool [:tool :width] 20)) scope table)]
    {:wrap {:sha (subs (sha (get-in a [:results :painting])) 0 12) :caret (get-in a [:results :caret])
            :last-b (select-keys (get-in a [:history 6 :state-after]) [:x :y])}
     :no-wrap {:sha (subs (sha (get-in no-wrap [:results :painting])) 0 12) :caret (get-in no-wrap [:results :caret])
               :last-b (select-keys (get-in no-wrap [:history 6 :state-after]) [:x :y])}
     :width-20 {:sha (subs (sha (get-in by-char-width-20 [:results :painting])) 0 12) :caret (get-in by-char-width-20 [:results :caret])}
     :cursor-record-unchanged? (= cursor (:cursor scope))
     :png (save-png! "text-6b-nowrap-zoom8.png" (get-in (e/run (tool-record (make-text-tool true)) (assoc scope :painting (painting "vantage-1/text" 8)) table) [:results :painting]))}))

;; ------------------------------------------------ ONE SOURCE: a layout record emits placements; paint and hit consume them
(def layout-tool
  (let [pen-x [:if wrap-rule [:get :state :x] [:get :run :at 0]]
        pen-y [:if wrap-rule [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]]]
    {:id "layout@1" :kind :tool :by "sid" :tool {:width 30 :line-height 10}
     :program
     {:steps [{:out :shaped :op :text/shape :args {:keys [:get :keys] :font [:get :font]}}]
      :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink]
             :state {:x [:get :run :at 0] :y [:get :run :at 1] :placed [:literal []] :caret [:literal nil]}
             :steps [{:out :placed :op :collect
                      :args {:into [:get :state :placed]
                             :item {:i [:get :g :i] :ch [:get :g :ch] :ink [:get :g :ink]
                                    :rect [[:+ pen-x [:get :g :box 0]] [:+ pen-y [:get :g :box 1]] [:get :g :box 2] [:get :g :box 3]]
                                    :pen [pen-x pen-y] :run [:get :run :id] :by [:get :run :by]}}}]
             :next {:x [:+ pen-x [:get :g :advance]] :y pen-y :placed [:get :placed]
                    :caret [:if [:= [:get :g :i] [:get :cursor :offset]] [pen-x pen-y] [:get :state :caret]]}}
      :return {:placements [:get :state :placed]
               :caret [:if [:= [:get :state :caret] [:literal nil]] [[:get :state :x] [:get :state :y]] [:get :state :caret]]}}}))
(def paint-tool
  {:id "paint@1" :kind :tool :by "sid" :tool {:ink [0 0 0 1]}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
    :each {:items [:get :inputs :placements] :item :p :fields [:rect :ink]
           :state {:painting [:get :fresh]}
           :steps [{:out :box :op :path/source :args {:source {:kind :rect :x [:get :p :rect 0] :y [:get :p :rect 1] :w [:get :p :rect 2] :h [:get :p :rect 3]} :tool {}}}
                   {:out :painted :op :paint :args {:surface [:get :state :painting] :region [:get :box :path]
                                                    :rgba [:get :tool :ink] :opacity [:get :p :ink] :blend :source-over}}]
           :next {:painting [:get :painted]}}
    :return {:painting [:get :state :painting]}}})
(def caret-tool
  {:id "caret@1" :kind :tool :by "sid" :tool {:ink [1 0 0 1] :height 8}
   :inputs {:caret {:kind :point :from {:record "layout@1" :output :caret}}
            :painting {:kind :painting :from {:record "paint@1" :output :painting}}}
   :program {:steps [{:out :bar :op :path/source :args {:source {:kind :rect :x [:get :inputs :caret 0] :y [:get :inputs :caret 1] :w 1 :h [:get :tool :height]} :tool {}}}
                     {:out :painted :op :paint :args {:surface [:get :inputs :painting] :region [:get :bar :path] :rgba [:get :tool :ink] :opacity 1 :blend :source-over}}]
             :return {:painting [:get :painted]}}})
(def inside? [:if [:< [:get :point 0] [:get :p :rect 0]] false
              [:if [:< [:+ [:get :p :rect 0] [:get :p :rect 2]] [:get :point 0]] false
               [:if [:< [:get :point 1] [:get :p :rect 1]] false
                [:if [:< [:+ [:get :p :rect 1] [:get :p :rect 3]] [:get :point 1]] false true]]]])
(def hit-tool
  {:id "hit@1" :kind :tool :by "sid" :tool {}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program {:each {:items [:get :inputs :placements] :item :p :fields [:i :ch :rect :pen :run :by]
                    :state {:hit [:literal nil]} :steps []
                    :next {:hit [:if inside? {:what :text/run :run [:get :p :run] :glyph [:get :p :ch] :index [:get :p :i]
                                              :where [:get :p :pen] :by [:get :p :by] :drawn-by [:get :drawn-by]}
                                 [:get :state :hit]]}}
             :return {:hit [:get :state :hit]}}})
(defn retained [r out] (assoc (get-in r [:results out]) :subject (get-in r [:subjects out])))
(defn retained-value [r out] {:value (get-in r [:results out]) :subject (get-in r [:subjects out])})
(def records {"layout@1" (tool-record layout-tool) "paint@1" (assoc (tool-record paint-tool) :roots {:tool (:tool paint-tool) :inputs (:inputs paint-tool)})})

(probe "9 one source: layout → placements; paint and hit and caret consume them through :inputs"
  (let [lay (e/run (tool-record layout-tool) scope table)
        placements (retained lay :placements)   ; a vector value with :subject? vectors cannot carry :subject — see below
        _ (when-not (= :complete (:status lay)) (throw (ex-info "layout failed" (select-keys lay [:status :reason :error :data]))))
        with-subject (fn [r out] (with-meta (get-in r [:results out]) {}))]
    {:layout-status (:status lay)
     :placements (mapv #(select-keys % [:ch :rect :pen]) (get-in lay [:results :placements]))
     :caret (get-in lay [:results :caret])
     :subject-of-placements {:roots (sort (keys (get-in lay [:subjects :placements :recipe :roots])))
                             :consumed (count (get-in lay [:subjects :placements :consumed]))}
     :note "check-inputs! reads (get-in caller [:inputs input :subject]) — a vector output has nowhere to carry :subject"}))

(probe "9b consuming a vector output through :inputs — what the executor says"
  (let [lay (e/run (tool-record layout-tool) scope table)
        pl (get-in lay [:results :placements])
        opts {:records {"layout@1" (tool-record layout-tool)}}
        rec (assoc (tool-record paint-tool) :roots {:tool (:tool paint-tool) :inputs (:inputs paint-tool)})
        bare (e/run rec (assoc scope :inputs {:placements pl}) table opts)
        ;; a map wrapper carrying the subject beside the value
        wrapped (e/run rec (assoc scope :inputs {:placements (assoc {:value pl} :subject (get-in lay [:subjects :placements]))}) table opts)]
    {:bare-vector (select-keys bare [:status :reason :detail])
     :wrapped-in-a-map (select-keys wrapped [:status :reason :detail :error :data])}))

;; The executor's :subject check wants the retained value to BE a map with :subject; make placements a map.
(def layout-tool-m (assoc-in layout-tool [:program :return :placements] {:items [:get :state :placed]}))
(def paint-tool-m (assoc-in paint-tool [:program :each :items] [:get :inputs :placements :items]))
(def hit-tool-m (assoc-in hit-tool [:program :each :items] [:get :inputs :placements :items]))
(defn rec-with-inputs [tool] (assoc (tool-record tool) :roots {:tool (:tool tool) :inputs (:inputs tool)}))
(def records-m {"layout@1" (tool-record layout-tool-m) "paint@1" (rec-with-inputs paint-tool-m)})

(probe "9c one source, placements as a map value with its subject: paint, hit and caret all take it"
  (let [lay (e/run (tool-record layout-tool-m) scope table)
        placements (retained lay :placements)
        opts {:records records-m}
        pnt (e/run (rec-with-inputs paint-tool-m) (assoc scope :inputs {:placements placements}) table opts)
        painting (retained pnt :painting)
        crt (e/run (rec-with-inputs caret-tool) (assoc scope :inputs {:caret (retained-value lay :caret) :painting painting}) table opts)
        hit (fn [pt] (:hit (:results (e/run (rec-with-inputs hit-tool-m) (assoc scope :inputs {:placements placements} :point pt :drawn-by "paint@1") table opts))))
        stale (e/run (rec-with-inputs paint-tool-m) (assoc scope :inputs {:placements (retained (e/run (tool-record layout-tool-m) (scope-of vantage (conj keys-typed {:i 7 :ch "l"})) table) :placements)}) table opts)]
    {:paint (select-keys pnt [:status :reason :detail]) :painting (brief (:value painting))
     :caret-status (select-keys crt [:status :reason :detail :error :data])
     :hit-on-b (hit [4.5 5.5]) :hit-on-a (hit [9.5 6.5]) :hit-on-wrapped-b (hit [4.5 15.5]) :hit-on-nothing (hit [0.5 0.5])
     :stale-placements-from-a-longer-run (select-keys stale [:status :reason :detail])
     :png (let [big (e/run (rec-with-inputs paint-tool-m) (assoc scope :inputs {:placements placements} :painting (painting "vantage-1/text" 8)) table opts)]
            (when (= :complete (:status big)) (save-png! "text-9-one-source-zoom8.png" (get-in big [:results :painting]))))}))

;; ------------------------------------------------ the keystroke, measured by phase
(probe "10 where a keystroke's time goes (zoom 8 surface 272x176; one glyph)"
  (let [decl (painting "v/text" 8)
        fresh (s/new decl)
        rect (:path (source/build {:kind :rect :x 24 :y 16 :w 32 :h 64} {}))
        one-paint (fn [] (path-surface/paint {:surface fresh :region rect :rgba [0 0 0 1] :opacity 1 :blend :source-over} {:at 0 :step :p}))
        n 40 ks (vec (map-indexed (fn [i ch] {:i i :ch (str ch)}) (apply str (take (inc n) (cycle "ball ab ")))))
        sc (fn [k] (assoc (scope-of vantage (subvec ks 0 k)) :painting decl))
        rec (tool-record text-tool)
        held (:continuation (e/run rec (sc n) table {:until (dec n)}))
        held-no-history (assoc held :history [])
        stroke (fn [h] (e/resume h table {:record rec :scope (sc (inc n)) :until n}))
        _ (dotimes [_ 2] (stroke held))
        t (fn [f] (med (repeatedly 5 #(ms f))))]
    {:surface-new-ms (t #(s/new decl))
     :paint-one-glyph-ms (t one-paint)
     :aclone-ms (t #(aclone ^floats (:data fresh)))
     :resume-one-key-ms (t #(stroke held))
     :resume-one-key-history-stripped-ms (t #(stroke held-no-history))
     :run-one-key-fresh-ms (t #(e/run rec (sc 1) table))
     :run-n-keys-ms (t #(e/run rec (sc n) table))
     :encode-continuation-ms (t #(e/encode held))
     :continuation-bytes (alength (e/encode held))
     :history-rows (count (:history held))
     :bytes-per-history-row (quot (alength (e/encode held)) (max 1 (count (:history held))))}))
(println "\nDONE")
