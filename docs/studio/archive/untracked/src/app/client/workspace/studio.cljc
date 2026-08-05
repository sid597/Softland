(ns app.client.workspace.studio
  "Studio P1's ordinary-canvas surface and selected-subject preview.

   State that changes together lives in one atom: the live gesture, selected
   draft, candidate previews, status, and stroke receipts. Trees and anatomy
   overlays are pure derivations. Ground owns I/O and scene-store mutation at
   its action-consumer edge."
  (:require [app.client.workspace.face-assembly :as face-assembly]
            [app.client.workspace.face-primitives :as face-primitives]
            [app.client.workspace.rect-tree :as rt]
            [app.shared.anatomy-material :as anatomy]
            [app.shared.facet-material :as facet-material]
            [app.shared.studio :as studio]))

(def birth-view-instance :studio-birth-handle)
(def stroke-view-instance :studio-stroke)
(def controls-view-instance :studio-controls)
(def resize-view-instance :studio-resize-handle)

(def initial-state
  {:phase :idle
   :gesture nil
   :selected nil
   :drafts {}
   :preview-by-subject {}
   :prior-default nil
   :status "Draw a component"
   :details nil
   :stroke-echo []})

(defonce !state (atom initial-state))

(defn reset-state! [] (reset! !state initial-state))
(defn state [] @!state)
(defn selected [] (:selected @!state))
(defn draft [subject] (get-in @!state [:drafts subject]))

(def ^:private handle-assembly
  {:assembly/name "studio-birth-handle"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 184.0 :h 62.0 :padding [10.0 12.0]
            :bg [0.15 0.18 0.24 0.96]
            :border-width 1.0
            :border-color [0.92 0.70 0.28 0.95]
            :radius 6.0}
    :children
    [{:prim :text-run
      :props {:value "Draw a component" :w 160.0 :size 15.0
              :line-height 20.0 :color [0.96 0.90 0.76 1.0]}}
     {:prim :text-run
      :props {:value "press and drag from here" :w 160.0 :size 11.0
              :line-height 16.0 :color [0.78 0.74 0.62 0.95]}}]}})

(def ^:private stroke-assembly
  {:assembly/name "studio-live-stroke"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w {:bind [:w]} :h {:bind [:h]}
            :padding 8.0 :bg [0.18 0.22 0.30 0.18]
            :border-width 2.0
            :border-color [0.92 0.70 0.28 0.95]
            :radius 3.0}
    :children
    [{:prim :text-run
      :props {:value "Draft" :w {:bind [:label-w]} :size 14.0
              :line-height 18.0 :color [0.96 0.90 0.76 1.0]}}]}})

(def ^:private controls-assembly
  {:assembly/name "studio-controls"
   :assembly/grammar 0
   :root
   {:prim :box
    :props {:w 180.0 :padding 8.0 :gap 3.0
            :bg [0.07 0.08 0.10 0.97]
            :border-width 1.0
            :border-color [0.46 0.52 0.64 0.9]
            :radius 5.0}
    :children
    [{:prim :text-run :props {:value "Draft" :w 164.0 :size 14.0}}
     {:prim :text-run :props {:value "Duplicate" :w 164.0 :size 14.0}}
     {:prim :text-run :props {:value "Change color" :w 164.0 :size 14.0}}
     {:prim :text-run :props {:value "Test and accept" :w 164.0 :size 14.0}}
     {:prim :text-run :props {:value "Put back" :w 164.0 :size 14.0}}
     {:prim :text-run
      :props {:value {:bind [:status]} :w 164.0 :size 12.0
              :color [0.72 0.76 0.84 1.0]}}
     {:prim :text-run
      :props {:value {:bind [:details-label]} :w 164.0 :size 12.0
              :color [0.64 0.68 0.76 1.0]}}]}})

(def ^:private resize-assembly
  {:assembly/name "studio-resize-handle"
   :assembly/grammar 0
   :root {:prim :box
          :props {:w 18.0 :h 18.0 :bg [0.92 0.70 0.28 0.95]
                  :border-width 1.0
                  :border-color [0.10 0.11 0.14 1.0]
                  :radius 3.0}}})

(def ^:private compiled-handle
  (face-assembly/compile-assembly face-primitives/registry handle-assembly))
(def ^:private compiled-stroke
  (face-assembly/compile-assembly face-primitives/registry stroke-assembly))
(def ^:private compiled-controls
  (face-assembly/compile-assembly face-primitives/registry controls-assembly))
(def ^:private compiled-resize
  (face-assembly/compile-assembly face-primitives/registry resize-assembly))

(defn- apply-tree [compiled vi address data w]
  (face-assembly/apply-assembly
   compiled data
   {:view-instance vi :address address
    :geom {:content-w w :font-size 14.0 :char-advance 7.84
           :line-height 20.0}}))

(defn birth-handle-tree []
  (assoc-in
   (apply-tree compiled-handle birth-view-instance
               :studio/birth-handle {} 184.0)
   [:data :actions]
   {:action :studio/birth-draft
    :hit/action :studio/birth-draft
    :binding/gesture :pointer/press
    :binding/phase :threshold
    :binding/modifiers #{}}))

(defn stroke-tree [{:keys [w h]}]
  (apply-tree compiled-stroke stroke-view-instance :studio/live-stroke
              {:w w :h h :label-w (max 1.0 (- w 16.0))} w))

(def ^:private control-actions
  [nil :duplicate :retune :accept :put-back nil :details])

(defn controls-tree []
  (let [details? (some? (:details @!state))
        tree (apply-tree compiled-controls controls-view-instance
                         :studio/controls
                         {:status (:status @!state)
                          :details-label (if details? "Show details" "")}
                         180.0)]
    (reduce
     (fn [t [i intent]]
       (if intent
         (-> t
             (assoc-in [:children i :data :address]
                       [:studio/control intent])
             (assoc-in [:children i :data :actions]
                       {:action :studio/control :studio/intent intent}))
         t))
     tree
     (map-indexed vector control-actions))))

(defn resize-handle-tree [subject]
  (assoc-in
   (apply-tree compiled-resize resize-view-instance
               [:studio/resize subject] {} 18.0)
   [:data :actions]
   {:action :studio/resize-draft
    :studio/subject subject
    :binding/gesture :pointer/press
    :binding/phase :threshold
    :binding/modifiers #{}}))

(defn gesture-rect
  [[[x0 y0] [x1 y1]]]
  {:x (min x0 x1) :y (min y0 y1)
   :w (max 48.0 (abs (- x1 x0)))
   :h (max 36.0 (abs (- y1 y0)))})

(defn begin-gesture! [intent subject world time-ms]
  (swap! !state assoc
         :phase :drawing
         :gesture {:intent intent :subject subject
                   :points [world world] :started-at-ms time-ms}
         :status (if (= intent :resize) "Resize the draft" "Drawing a draft"))
  (:gesture @!state))

(defn move-gesture! [world]
  (swap! !state update :gesture assoc-in [:points 1] world)
  (some-> @!state :gesture :points gesture-rect))

(defn finish-gesture! [world]
  (let [g (swap! !state update :gesture assoc-in [:points 1] world)
        gesture (:gesture g)
        rect (gesture-rect (:points gesture))]
    (swap! !state assoc :phase :settling :gesture nil)
    (assoc gesture :rect rect)))

(defn note-painted! [started-at-ms painted-at-ms]
  (let [elapsed (- painted-at-ms started-at-ms)]
    (swap! !state
           (fn [s]
             (-> s
                 (update :stroke-echo
                         (fn [xs]
                           (let [xs (vec xs)]
                             (conj (if (>= (count xs) 512)
                                     (subvec xs 1) xs)
                                   elapsed))))
                 (assoc :phase :drawing))))
    elapsed))

(defn record-draft! [subject source form]
  (swap! !state
         (fn [s]
           (-> s
               (assoc :selected subject
                      :phase :idle
                      :status "Draft is real and ready"
                      :details nil)
               ;; R3 — preserve any prior `:acked-source` (select-keys on a
               ;; nil prior entry is `{}`, so a brand-new subject is unaffected).
               (update-in [:drafts subject]
                         (fn [prior]
                           (assoc (select-keys prior [:acked-source])
                                  :subject subject :source source :form form)))
               (assoc-in [:preview-by-subject subject] source)))))

(defn select! [subject]
  (when (get-in @!state [:drafts subject])
    (swap! !state assoc :selected subject)))

(defn set-candidate! [subject candidate]
  (when (= :candidate (:status candidate))
    (swap! !state
           (fn [s]
             (-> s
                 (assoc :selected subject)
                 ;; R3 — an optimistic settle overwrites the draft's working
                 ;; source/form, but must NOT drop the last DURABLY acked
                 ;; source: `revert-preview-on-refusal!` needs it if THIS
                 ;; settle is the one that gets refused.
                 (update-in [:drafts subject]
                           (fn [prior]
                             (assoc (select-keys prior [:acked-source])
                                    :subject subject
                                    :source (:source candidate)
                                    :form (:form candidate)
                                    :op-receipts (:op-receipts candidate))))
                 (assoc-in [:preview-by-subject subject] (:source candidate))))))
  candidate)

(defn preview-subjects! [subjects source]
  (swap! !state update :preview-by-subject
         merge (zipmap (map :instance/id subjects) (repeat source))))

(defn clear-preview-subjects! [subjects]
  (swap! !state update :preview-by-subject
         #(apply dissoc % (map :instance/id subjects))))

(defn clear-subject-preview! [subject]
  (swap! !state update :preview-by-subject dissoc subject))

(defn ack-draft-source!
  "The last DURABLY accepted source for a draft — recorded only after an
   ACCEPTED `:birth`/`:settle` write lands, never speculatively.
   `revert-preview-on-refusal!` is what reads this back."
  [subject source]
  (swap! !state assoc-in [:drafts subject :acked-source] source))

(defn revert-preview-on-refusal!
  "R3 — a refused durable write must never leave the preview showing material
   that does not durably exist: settle-draft-candidate!/persist-draft!/
   land-empty-draft! all install the optimistic candidate into
   `:preview-by-subject` BEFORE the write lands. Falls back to the subject's
   last acked source, or clears the preview entirely — the still-born-draft
   case, nothing has ever been acked for this subject."
  [subject]
  (if-let [acked (get-in @!state [:drafts subject :acked-source])]
    (swap! !state assoc-in [:preview-by-subject subject] acked)
    (clear-subject-preview! subject)))

(defn set-prior-default! [revision-id]
  (swap! !state assoc :prior-default revision-id))

(defn set-status!
  ([status] (set-status! status nil))
  ([status details]
   (swap! !state assoc :status (str status) :details details)))

(defn accept-subject! [subject source form]
  (swap! !state
         (fn [s]
           (-> s
               (assoc :status "Accepted" :details nil)
               (assoc-in [:drafts subject]
                         {:subject subject :source source :form form
                          :accepted? true})
               (update :preview-by-subject dissoc subject)))))

(defn wears-for
  "Overlay only named Studio subjects. Every other subject receives `wears`
   by identity, keeping the selected-subject preview from becoming broadcast."
  [subject wears]
  (if-let [source (get-in @!state [:preview-by-subject subject])]
    (let [compiled (facet-material/compile-source anatomy/spec source)]
      (if (:valid? compiled)
        (assoc wears :anatomy
               (merge (:material compiled)
                      {:facet-master/id anatomy/master-id
                       :facet-master/facet :anatomy
                       :facet-master/grammar (:grammar compiled)
                       :facet-master/revision-id "studio-preview"
                       :facet-master/floor? false
                       :facet-master/tier :instance
                       :facet-master/preview? true
                       :facet-master/subject subject}))
        wears))
    wears))

(defn new-draft-candidate [base-form rect]
  (studio/draft-candidate
   base-form
   {:w (:w rect) :h (:h rect)}))

(defn retune-candidate [form color]
  (studio/retune-frame-candidate form {:border-color color}))

(defn resize-candidate [form rect]
  (studio/retune-frame-candidate form {:w (:w rect) :h (:h rect)}))

(defn promotable-candidate [form]
  (studio/promotable-candidate form))

(defn stroke-report [] (:stroke-echo @!state))
