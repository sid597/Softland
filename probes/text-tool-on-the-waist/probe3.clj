;; probe3.clj — kill-probe for the reflection hypothesis; the consumed-items hole in check-inputs!
;; with the repo's own sphere fixtures; caret record fixed; zoom-8 one-source paint status.
(ns probe3
  (:require [app.client.engine.executor :as e]
            [app.client.engine.surface :as s]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as c]
            [app.client.region3d.records :as r3]
            [app.client.region3d.capabilities :as cap3]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

(def out-dir "probes/text-tool-on-the-waist/")
(defn report [label m] (println (str "\n=== " label " ===")) (pp/pprint m) (flush))
(defmacro probe [label & body]
  `(try (report ~label (do ~@body))
        (catch Throwable t# (report ~label {:threw (.getMessage t#) :data (ex-data t#)}))))
(defn ms [f] (let [t (System/nanoTime)] (f) (/ (- (System/nanoTime) t) 1e6)))
(defn med [xs] (nth (sort xs) (quot (count xs) 2)))
(defn save-png! [name painting]
  (with-open [o (io/output-stream (str out-dir name))] (.write o (s/png-bytes painting))) name)

;; ---------------------------------------------- 11. reflection kill-probe
(probe "11 is surface/new's 1.5 s reflection? (same fill, hinted vs unhinted)"
  (let [n (* 272 176 4) initial [0.0 0.0 0.0 0.0]
        unhinted (fn [] (let [data (vb/floats n)] (dotimes [i n] (aset data i (float (nth initial (mod i 4))))) data))
        hinted (fn [] (let [^floats data (vb/floats n)] (dotimes [i n] (aset data i (float (nth initial (mod i 4))))) data))
        t (fn [f] (med (repeatedly 5 #(ms f))))]
    (binding [*warn-on-reflection* true]
      {:floats n
       :fill-unhinted-ms (t unhinted)
       :fill-hinted-ms (t hinted)
       :surface-new-ms (t #(s/new {:surface/id "v" :width 272 :height 176 :domain {:kind :plane :map [8 0 0 8 0 0]}
                                   :color :linear-premultiplied-rgba :filter :nearest :initial initial}))
       :prediction "if reflection: unhinted ≈ surface/new ≫ hinted by ~1000×"})))

;; ---------------------------------------------- 12. the subject check and consumed items (repo fixtures)
(probe "12 check-inputs! — does a painting from a DIFFERENT event stream pass as the same subject?"
  (let [run (fn [record scope opts] (e/run (r3/program-record record) scope cap3/table opts))
        retained (fn [r out] (assoc (get-in r [:results out]) :subject (get-in r [:subjects out])))
        first-run (run r3/pickup {:host r3/host} {:budget 1})
        other-events (update r3/pickup :events conj {:id "dab-5" :curve "kA/arc-AB" :t 0.5})
        other-run (run other-events {:host r3/host} {:budget 1})
        second-record (-> r3/pickup (assoc :id "again" :inputs {:previous {:kind :painting :from {:record "pickup" :output :painting}}})
                          (assoc-in [:program :each :state :painting] [:get :inputs :previous]))
        opts {:budget 1 :records {"pickup" (r3/program-record r3/pickup)}}
        same (run second-record {:host r3/host :inputs {:previous (retained first-run :painting)}} opts)
        longer (run second-record {:host r3/host :inputs {:previous (retained other-run :painting)}} opts)
        radius (run second-record {:host r3/host :inputs {:previous (retained (run (assoc-in r3/pickup [:tool :radius] 9) {:host r3/host} {:budget 1}) :painting)}} opts)]
    {:same-stream (select-keys same [:status :reason])
     :five-events-not-four {:status (:status longer) :reason (:reason longer)
                            :consumed-in-subject (count (get-in other-run [:subjects :painting :consumed]))
                            :consumed-expected 4}
     :edited-tool-radius (select-keys radius [:status :reason])
     :note "check-inputs! compares program + reached roots (executor.cljc:234-254); :consumed rides in the subject but is never compared"}))

;; ---------------------------------------------- 13. caret record fixed; the one-source paint at zoom 8
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
(def table (merge c/capabilities
                  {:text/shape {:args [:keys :font] :needs [[:keys] [:font]] :run (fn [{:keys [keys font]} _] (shape-by-lookup font keys))}
                   :collect {:args [:into :item] :needs [[:into] [:item]] :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}}))
(def wrap-rule [:<= [:+ [:get :state :x] [:get :g :advance]] [:+ [:get :run :at 0] [:get :tool :width]]])
(def pen-x [:if wrap-rule [:get :state :x] [:get :run :at 0]])
(def pen-y [:if wrap-rule [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]])
(def layout-tool
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
    :return {:placements {:items [:get :state :placed]}
             :caret {:at [:if [:= [:get :state :caret] [:literal nil]] [[:get :state :x] [:get :state :y]] [:get :state :caret]]}}}})
(def paint-tool
  {:id "paint@1" :kind :tool :by "sid" :tool {:ink [0 0 0 1]}
   :inputs {:placements {:kind :placements :from {:record "layout@1" :output :placements}}}
   :program
   {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
    :each {:items [:get :inputs :placements :items] :item :p :fields [:rect :ink]
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
   :program {:steps [{:out :bar :op :path/source :args {:source {:kind :rect :x [:get :inputs :caret :at 0] :y [:get :inputs :caret :at 1] :w 1 :h [:get :tool :height]} :tool {}}}
                     {:out :painted :op :paint :args {:surface [:get :inputs :painting] :region [:get :bar :path] :rgba [:get :tool :ink] :opacity 1 :blend :source-over}}]
             :return {:painting [:get :painted]}}})
(defn tool-record [tool] {:program (:program tool) :roots (cond-> {:tool (:tool tool)} (:inputs tool) (assoc :inputs (:inputs tool)))})
(defn retained [r out] (assoc (get-in r [:results out]) :subject (get-in r [:subjects out])))
(def vantage {:id "vantage-1" :kind :vantage :by "sid" :from nil :subject {:kind :text/run :by "sid"} :tools ["layout@1" "paint@1" "caret@1"] :pins ["cursor-1"] :zoom 1})
(defn scope-of [v ks zoom] {:run run :cursor cursor :keys ks :font font :painting (painting (str (:id v) "/text") zoom)})
(def records {"layout@1" (tool-record layout-tool) "paint@1" (tool-record paint-tool)})

(probe "13 layout → paint → caret, chained by :inputs, at zoom 1 and zoom 8; a stale caret refused"
  (let [go (fn [zoom ks]
             (let [scope (scope-of vantage ks zoom) opts {:records records}
                   lay (e/run (tool-record layout-tool) scope table)
                   placements (retained lay :placements)
                   pnt (e/run (tool-record paint-tool) (assoc scope :inputs {:placements placements}) table opts)
                   crt (e/run (tool-record caret-tool) (assoc scope :inputs {:caret (retained lay :caret) :painting (retained pnt :painting)}) table opts)]
               {:layout (:status lay) :paint (:status pnt) :caret (select-keys crt [:status :reason :error :data])
                :caret-at (get-in lay [:results :caret :at])
                :surface (select-keys (get-in crt [:results :painting]) [:surface/id :revision :key :width :height])
                :painting (get-in crt [:results :painting])}))
        z1 (go 1 keys-typed) z8 (go 8 keys-typed)
        ;; the cursor moved: a caret retained from the old layout handed to a new paint
        moved-scope (assoc (scope-of vantage keys-typed 1) :cursor (assoc cursor :offset 5))
        old-lay (e/run (tool-record layout-tool) (scope-of vantage keys-typed 1) table)
        new-lay (e/run (tool-record layout-tool) moved-scope table)
        new-pnt (e/run (tool-record paint-tool) (assoc moved-scope :inputs {:placements (retained new-lay :placements)}) table {:records records})
        stale (e/run (tool-record caret-tool) (assoc moved-scope :inputs {:caret (retained old-lay :caret) :painting (retained new-pnt :painting)}) table {:records records})]
    {:zoom-1 (dissoc z1 :painting) :zoom-8 (dissoc z8 :painting)
     :stale-caret-after-cursor-moved (select-keys stale [:status :reason :detail])
     :png (save-png! "text-13-chain-caret-zoom8.png" (:painting z8))}))
(println "\nDONE")
