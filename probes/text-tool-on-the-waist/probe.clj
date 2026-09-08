;; probe.clj — how far the target state goes as RECORDS on the current waist.
;; Runs the repo's executor + CPU compositor + path capability table unchanged.
;; Every "CODE" capability added here is marked; nothing under src/ is touched.
(ns probe
  (:require [app.client.engine.executor :as e]
            [app.client.engine.surface :as s]
            [app.client.engine.value-bytes :as vb]
            [app.client.path.construction :as c]
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

;; ---------------------------------------------------------------- the records
;; Every record carries :id :kind :by. A font is records: a glyph is a box relative
;; to the pen (y down, ascender at 0, baseline at 8) with an advance. Boxes stand in
;; for outlines only because the real outlines come from the font file through the
;; shaper, which is code below the waist by law either way.
(def font
  {:id "boxfont@1" :kind :font :by "sid"
   :glyphs {"a" {:advance 6 :box [1 3 4 5] :ink 1}
            "b" {:advance 6 :box [1 0 4 8] :ink 1}
            "l" {:advance 3 :box [1 0 1 8] :ink 1}
            " " {:advance 3 :box [0 7 1 1] :ink 0}}})

;; The keystroke stream, saved as it is: one record per key, in order.
(def keys-typed (vec (map-indexed (fn [i ch] {:i i :ch (str ch)}) "ball ab")))
;; What a shaper gives back per key (metrics from the font). Probe 2a stores it as a
;; root (pre-shaped); probe 2b computes it through a capability.
(defn shape-by-lookup [font ks] (mapv (fn [{:keys [i ch]}] (merge {:i i :ch ch} (get-in font [:glyphs ch]))) ks))

(def run {:id "run-1" :kind :text/run :by "sid" :at [2 2]})
(def cursor {:id "cursor-1" :kind :cursor :by "sid" :in "run-1" :offset 3})

(defn painting [id zoom]
  {:surface/id id :width (int (* 34 zoom)) :height (int (* 22 zoom))
   :domain {:kind :plane :map [zoom 0 0 zoom 0 0]}
   :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]})

;; ---------------------------------------------------------------- the text tool
;; The pen for THIS glyph: wrap before the glyph that would overflow the width.
;; (No :let in the leaves, so the same expression is repeated where it is needed.)
(def fits  [:<= [:+ [:get :state :x] [:get :g :advance]] [:+ [:get :run :at 0] [:get :tool :width]]])
(def pen-x [:if fits [:get :state :x] [:get :run :at 0]])
(def pen-y [:if fits [:get :state :y] [:+ [:get :state :y] [:get :tool :line-height]]])

(def text-tool
  {:id "text-tool@1" :kind :tool :by "sid"
   :tool {:width 30 :line-height 10 :ink [0 0 0 1]}
   :program
   {:steps [{:out :fresh :op :surface/new :args {:declaration [:get :painting]}}]
    :each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink]
           :state {:x [:get :run :at 0] :y [:get :run :at 1] :painting [:get :fresh] :caret [:literal nil]}
           :steps [{:out :box :op :path/source
                    :args {:source {:kind :rect
                                    :x [:+ pen-x [:get :g :box 0]] :y [:+ pen-y [:get :g :box 1]]
                                    :w [:get :g :box 2] :h [:get :g :box 3]}
                           :tool {}}}
                   {:out :painted :op :paint
                    :args {:surface [:get :state :painting] :region [:get :box :path]
                           :rgba [:get :tool :ink] :opacity [:get :g :ink] :blend :source-over}}]
           :next {:x [:+ pen-x [:get :g :advance]] :y pen-y :painting [:get :painted]
                  :caret [:if [:= [:get :g :i] [:get :cursor :offset]] [pen-x pen-y] [:get :state :caret]]}}
    :return {:painting [:get :state :painting]
             :caret [:if [:= [:get :state :caret] [:literal nil]]
                     [[:get :state :x] [:get :state :y]] [:get :state :caret]]}}})

(defn tool-record [tool] {:program (:program tool) :roots {:tool (:tool tool)}})

;; The vantage: which subject, which tools, what is pinned, at what zoom, by whom,
;; from where. It runs nothing itself; it becomes the caller SCOPE of the tools.
(def vantage-sid {:id "vantage-1" :kind :vantage :by "sid" :from nil
                  :subject {:kind :text/run :by "sid"} :tools ["text-tool@1"] :pins ["cursor-1"] :zoom 1})

(defn scope-of [vantage & {:keys [shaped keys]}]
  (cond-> {:run run :cursor cursor :painting (painting (str (:id vantage) "/text") (:zoom vantage))}
    shaped (assoc :shaped shaped)
    keys (assoc :keys keys :font font)))

;; CODE #1 — the shaper as a capability (its JVM form is a lookup; the browser form is HarfBuzz)
(def shape-cap {:text/shape {:args [:keys :font] :needs [[:keys] [:font]]
                             :run (fn [{:keys [keys font]} _] (shape-by-lookup font keys))}})
;; CODE #2 — a loop that can emit a collection (one line, pure, total)
(def collect-cap {:collect {:args [:into :item] :needs [[:into] [:item]]
                            :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}})

(def text-tool-2b (assoc-in text-tool [:program :steps 1]
                            {:out :shaped :op :text/shape :args {:keys [:get :keys] :font [:get :font]}}))
(def table-2b (merge c/capabilities shape-cap))

;; ---------------------------------------------------------------- 0. leaf-language limits
(probe "0a string as loop items"
  (select-keys (e/run {:program {:each {:items [:get :text] :item :ch :fields [] :state {} :steps [] :next {}} :return {}}}
                      {:text "ab"} c/capabilities) [:status :error :data]))
(probe "0b computed :get (lookup by the item's own char)"
  (select-keys (e/run {:program {:steps [{:out :g :op :surface/new :args {:declaration [:get :font :glyphs [:get :k :ch]]}}] :return {}}}
                      {:font font :k {:ch "a"}} c/capabilities) [:status :error :data]))
(probe "0c operators the leaves lack"
  (into {} (for [expr [[:and true false] [:conj [:literal []] 1] [:count [:literal [1 2]]] [:str "a" "b"] [:let [:x 1] [:get :x]]]]
             [(first expr) (try (e/evaluate expr {}) (catch Exception ex (:error-type (ex-data ex))))])))
(probe "0d a loop with items missing a declared field (heterogeneous store)"
  (select-keys (e/run {:program {:each {:items [:get :store] :item :r :fields [:id :kind :by] :state {:n 0} :steps [] :next {:n [:+ [:get :state :n] 1]}} :return {:n [:get :state :n]}}}
                      {:store [run {:id "x" :kind :thing}]} c/capabilities) [:status :error :data]))

;; ---------------------------------------------------------------- 2a. text drawn, zero new code
(def scope-2a (scope-of vantage-sid :shaped (shape-by-lookup font keys-typed)))
(def r2a (e/run (tool-record text-tool) scope-2a c/capabilities))
(probe "2a run of text through the compositor — existing path table only"
  {:status (:status r2a) :reason (:reason r2a) :error (:error r2a) :data (:data r2a)
   :painting (brief (get-in r2a [:results :painting]))
   :caret (get-in r2a [:results :caret])
   :pens-by-item (mapv (fn [row] [(get-in row [:item :ch]) (select-keys (:state-after row) [:x :y])]) (:history r2a))
   :changed-per-glyph (mapv #(get-in % [:steps 1 :changed]) (:history r2a))
   :subject-roots-of-painting (sort (keys (get-in r2a [:subjects :painting :recipe :roots])))
   :unread (:unread r2a)
   :png (when (= :complete (:status r2a))
          (let [big (e/run (tool-record text-tool) (assoc scope-2a :painting (painting "vantage-1/text" 8)) c/capabilities)]
            (save-png! "text-2a-zoom8.png" (get-in big [:results :painting]))))})

;; ---------------------------------------------------------------- 2b. the shaper as a capability
(def scope-2b (scope-of vantage-sid :keys keys-typed))
(def r2b (e/run (tool-record text-tool-2b) scope-2b table-2b))
(probe "2b keys stream + :text/shape capability (CODE #1)"
  {:status (:status r2b) :reason (:reason r2b) :error (:error r2b)
   :same-painting-as-2a? (and (= :complete (:status r2b)) (= (sha (get-in r2a [:results :painting])) (sha (get-in r2b [:results :painting]))))
   :caret (get-in r2b [:results :caret])
   :recipe-roots (sort (keys (:roots (e/recipe (tool-record text-tool-2b) scope-2b))))
   :note "the :keys stream is reached only by :items, so it is NOT in the recipe — appending resumes"})

;; ---------------------------------------------------------------- 3. the pointer
(def p3 (get-in r2a [:results :painting]))
(probe "3a sample the painting where 'b' was drawn, and where nothing was"
  {:on-b (dissoc (s/sample [p3] [4.5 5.5] :nearest {}) :snapshot)
   :on-nothing (dissoc (s/sample [p3] [0.5 0.5] :nearest {}) :snapshot)
   :note "contributors name surface@revision of the whole painting, not the glyph"})

(def inside?
  [:if [:< [:get :point 0] [:+ pen-x [:get :g :box 0]]] false
   [:if [:< [:+ pen-x [:get :g :box 0] [:get :g :box 2]] [:get :point 0]] false
    [:if [:< [:get :point 1] [:+ pen-y [:get :g :box 1]]] false
     [:if [:< [:+ pen-y [:get :g :box 1] [:get :g :box 3]] [:get :point 1]] false true]]]])
(def hit-tool
  {:id "hit-tool@1" :kind :tool :by "sid"
   :tool {:width 30 :line-height 10}          ; the layout's parameters, DUPLICATED
   :program {:each {:items [:get :shaped] :item :g :fields [:i :ch :advance :box :ink]
                    :state {:x [:get :run :at 0] :y [:get :run :at 1] :hit [:literal nil] :hit-at [:literal nil]}
                    :steps []
                    :next {:x [:+ pen-x [:get :g :advance]] :y pen-y
                           :hit [:if inside? [:get :g :ch] [:get :state :hit]]
                           :hit-i [:if inside? [:get :g :i] [:get :state :hit-i]]
                           :hit-at [:if inside? [pen-x pen-y] [:get :state :hit-at]]}}
             :return {:what [:get :run :kind] :glyph [:get :state :hit] :index [:get :state :hit-i]
                      :where [:get :state :hit-at] :by [:get :run :by] :drawn-by [:get :drawn-by]}}})
(probe "3b point at the text — a hit record over the same items (zero code, boxes)"
  (let [ask (fn [pt] (let [r (e/run (tool-record hit-tool) (assoc scope-2a :point pt :drawn-by "text-tool@1") c/capabilities)]
                       (if (= :complete (:status r)) (:results r) (select-keys r [:status :reason :error :data]))))]
    {:at-4.5-5.5 (ask [4.5 5.5]) :at-9.5-6.5 (ask [9.5 6.5]) :at-4.5-15.5 (ask [4.5 15.5]) :at-0.5-0.5 (ask [0.5 0.5])
     :note "the hit re-derives the layout: paint and hit are two sources unless the loop can emit placements"}))

;; ---------------------------------------------------------------- 4. the subject as a query
(def foreign-run {:id "run-2" :kind :text/run :by "clipboard" :at [2 12]})
(def task-block {:id "block-9" :kind :task :by "sid"})
(def store [font run cursor text-tool hit-tool vantage-sid foreign-run task-block])
(def match [:if [:= [:get :r :kind] [:get :where :kind]] [:if [:= [:get :r :by] [:get :where :by]] true false] false])
(def query-count
  {:program {:each {:items [:get :store] :item :r :fields [:id :kind :by] :state {:n 0} :steps []
                    :next {:n [:+ [:get :state :n] [:if match 1 0]]}}
             :return {:n [:get :state :n]}}
   :roots {}})
(def query-collect
  {:program {:each {:items [:get :store] :item :r :fields [:id :kind :by] :state {:hits [:literal []]}
                    :steps [{:out :hits :op :collect :args {:into [:get :state :hits] :item [:if match [:get :r :id] [:literal nil]]}}]
                    :next {:hits [:get :hits]}}
             :return {:hits [:get :state :hits]}}
   :roots {}})
(probe "4 the vantage's subject as a query over the store"
  {:count-zero-code (:results (e/run query-count {:store store :where (:subject vantage-sid)} c/capabilities))
   :collect-with-CODE-2 (:results (e/run query-collect {:store store :where (:subject vantage-sid)} (merge c/capabilities collect-cap)))
   :any-run-by-anyone (:results (e/run query-collect {:store store :where {:kind :text/run}} (merge c/capabilities collect-cap {:collect (:collect collect-cap)})))})
(probe "4b a query whose where-clause omits a field the match reads"
  (select-keys (e/run query-collect {:store store :where {:kind :text/run}} (merge c/capabilities collect-cap)) [:status :error :data]))

;; ---------------------------------------------------------------- 5. the grain of typing
(defn typing-cost [zoom n]
  (let [ks (vec (map-indexed (fn [i ch] {:i i :ch (str ch)}) (apply str (take n (cycle "ball ab ")))))
        sc (fn [k] (assoc (scope-of vantage-sid :keys (subvec ks 0 k)) :painting (painting "v/text" zoom)))
        rec (tool-record text-tool-2b)
        full (fn [k] (e/run rec (sc k) table-2b))
        ;; a keystroke: the continuation held at k, the stream grown to k+1, resume to k+1
        held (atom (:continuation (e/run rec (sc 2) table-2b {:until 1})))
        stroke (fn [k] (let [r (e/resume @held table-2b {:record rec :scope (sc (inc k)) :until k})]
                         (when-not (= :suspended (:status r)) (throw (ex-info "stroke did not suspend" (select-keys r [:status :reason :at]))))
                         (reset! held (:continuation r))))
        _ (dotimes [_ 3] (full 8))                          ; warm up
        per-stroke (mapv (fn [k] (ms #(stroke k))) (range 2 n))
        full-ms (ms #(full n))]
    {:zoom zoom :surface [(:width (painting "v" zoom)) (:height (painting "v" zoom))] :n n
     :per-keystroke-ms {:median (nth (sort per-stroke) (quot (count per-stroke) 2)) :max (apply max per-stroke)
                        :first-5 (mapv #(Math/round (* 100 %)) (take 5 per-stroke)) :last-5 (mapv #(Math/round (* 100 %)) (take-last 5 per-stroke))}
     :full-rerun-ms full-ms
     :continuation-bytes (alength (e/encode @held))}))
(probe "5 typing = append a key + resume (cost per keystroke vs full rerun)"
  {:small (typing-cost 1 60) :big (typing-cost 8 60) :long (typing-cost 1 200)
   :note "the continuation exists only while an item is unconsumed: a completed run holds none, so the stream is held one key behind (see :until)"})
(probe "5b editing an earlier key vs appending — what resume says"
  (let [ks keys-typed rec (tool-record text-tool-2b)
        held (:continuation (e/run rec (scope-of vantage-sid :keys ks) table-2b {:until 3}))
        say (fn [ks'] (select-keys (e/resume held table-2b {:record rec :scope (scope-of vantage-sid :keys ks')}) [:status :reason :at]))]
    {:append (say (conj ks {:i 7 :ch "l"}))
     :edit-consumed-key (say (assoc-in ks [1 :ch] "l"))
     :edit-unconsumed-key (say (assoc-in ks [5 :ch] "l"))
     :move-the-run (select-keys (e/resume held table-2b {:record rec :scope (assoc (scope-of vantage-sid :keys ks) :run (assoc run :at [4 2]))}) [:status :reason :detail])}))

;; ---------------------------------------------------------------- 6. change the tool in place
(probe "6 point at the tool, change one record, the text redraws; cursor and kind intact"
  (let [narrow (assoc-in text-tool [:tool :width] 18)
        no-wrap (assoc-in text-tool [:program :each :next :y] [:get :state :y])
        no-wrap (assoc-in no-wrap [:program :each :next :x] [:+ [:get :state :x] [:get :g :advance]])
        run-it (fn [tool] (e/run (tool-record tool) scope-2a c/capabilities))
        a (run-it text-tool) b (run-it narrow) d (run-it no-wrap)
        held (:continuation (e/run (tool-record text-tool-2b) scope-2b table-2b {:until 3}))]
    {:before {:sha (subs (sha (get-in a [:results :painting])) 0 12) :caret (get-in a [:results :caret])}
     :width-18 {:sha (subs (sha (get-in b [:results :painting])) 0 12) :caret (get-in b [:results :caret])
                :pens (mapv (fn [row] [(get-in row [:item :ch]) (select-keys (:state-after row) [:x :y])]) (:history b))}
     :rule-edited-no-wrap {:sha (subs (sha (get-in d [:results :painting])) 0 12) :caret (get-in d [:results :caret])}
     :cursor-record-unchanged? (= cursor (:cursor scope-2a))
     :run-kind-intact? (= :text/run (get-in a [:subjects :painting :recipe :roots :run :kind]))
     :resume-typing-under-edited-tool (select-keys (e/resume held table-2b {:record (tool-record (assoc-in text-tool-2b [:tool :width] 18)) :scope scope-2b}) [:status :reason :detail])
     :png (save-png! "text-6-width18-zoom8.png" (get-in (e/run (tool-record narrow) (assoc scope-2a :painting (painting "vantage-1/text" 8)) c/capabilities) [:results :painting]))}))

;; ---------------------------------------------------------------- 7. the four small things as data
(def text-tool-foreign
  (-> text-tool
      (assoc-in [:tool :self] "sid")
      (assoc-in [:program :each :steps 0 :args :source]
                {:kind :rect
                 :x [:+ pen-x [:* [:get :g :box 0] [:if [:= [:get :run :by] [:get :tool :self]] 1 0.5]]]
                 :y [:+ pen-y [:* [:get :g :box 1] [:if [:= [:get :run :by] [:get :tool :self]] 1 0.5]]]
                 :w [:* [:get :g :box 2] [:if [:= [:get :run :by] [:get :tool :self]] 1 0.5]]
                 :h [:* [:get :g :box 3] [:if [:= [:get :run :by] [:get :tool :self]] 1 0.5]]})
      (assoc-in [:program :each :next :x] [:+ pen-x [:* [:get :g :advance] [:if [:= [:get :run :by] [:get :tool :self]] 1 0.5]]])))
(probe "7 a foreign paste draws at half size — a tool rule over the asserter (zero code)"
  (let [mine (e/run (tool-record text-tool-foreign) scope-2a c/capabilities)
        theirs (e/run (tool-record text-tool-foreign) (assoc scope-2a :run foreign-run) c/capabilities)
        both (assoc scope-2a :run foreign-run :painting (painting "vantage-1/text" 8))]
    {:mine (select-keys (:results mine) [:caret]) :mine-advance-b (get-in mine [:history 0 :state-after :x])
     :theirs (select-keys (:results theirs) [:caret]) :theirs-advance-b (get-in theirs [:history 0 :state-after :x])
     :kind-set-by-a-tool (assoc task-block :kind :task)
     :png (save-png! "text-7-foreign-zoom8.png" (get-in (e/run (tool-record text-tool-foreign) both c/capabilities) [:results :painting]))}))

;; ---------------------------------------------------------------- 8. two vantages, and returning
(probe "8 two people on one subject; returning to a vantage"
  (let [agent {:id "vantage-2" :kind :vantage :by "agent:claude" :from "vantage-1"
               :subject (:subject vantage-sid) :tools ["text-tool@1"] :pins [] :zoom 2}
        a (e/run (tool-record text-tool) scope-2a c/capabilities)
        b (e/run (tool-record text-tool) (scope-of agent :shaped (shape-by-lookup font keys-typed)) c/capabilities)
        again (e/run (tool-record text-tool) scope-2a c/capabilities)]
    {:sid-painting (brief (get-in a [:results :painting])) :agent-painting (brief (get-in b [:results :painting]))
     :roots-differ-in (vec (remove #(vb/equal? (get-in a [:subjects :painting :recipe :roots %]) (get-in b [:subjects :painting :recipe :roots %]))
                                   (keys (get-in a [:subjects :painting :recipe :roots]))))
     :returning-is-byte-identical? (and (vb/equal? (:results a) (:results again)) (= (:subjects a) (:subjects again)))
     :continuation-keeps-kinds? (let [held (:continuation (e/run (tool-record text-tool-2b) scope-2b table-2b {:until 2}))
                                      back (e/decode (e/encode held) table-2b)]
                                  [(get-in back [:scope :run :kind]) (get-in back [:scope :cursor :kind])])}))
(println "\nDONE")
