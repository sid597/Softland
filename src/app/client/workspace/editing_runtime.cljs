(ns app.client.workspace.editing-runtime
  "Flag-only T2 session runtime. Session/document truth stays client-local;
   every view document crosses Contract T once, and the resulting glyph and
   editing-furniture geometry is retained in the fixture slot."
  (:require [app.client.substrate.frame-scheduler :as frame-scheduler]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.editing-segmentation :as segmentation]
            [app.client.workspace.events :as events]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.runtime.mouse :as mouse]
            [app.client.workspace.runtime.render :as render]
            [app.client.workspace.scene-runtime :as scene-runtime]
            [app.client.workspace.scene-store :as scene-store]
            [app.client.workspace.text-editing :as editing]
            [app.client.workspace.text-layout :as tl]))

(def paragraph-vi :t2-input-floor/paragraph)
(def clipped-card-vi :t2-input-floor/clipped-card)
(def blink-deadline-id :t2/caret-blink)
(def blink-cadence-ms 530.0)

(def fixture-specs
  [{:vi paragraph-vi
    :address :t2-input-floor/paragraph-text
    :kind :paragraph
    :text (str "T2 input floor — office affinity and proportional wrapping. "
               "Arrows enter the fi ligature; e\u0301 moves as one grapheme. "
               "Mixed direction stays one geometry truth: سلام Softland. "
               "Paste may add\nmore than one visual line without a second seam.")
    :font-size 20.0 :line-height 28.0 :baseline-offset 21.0
    :wrap-col 39 :origin [18.0 14.0]
    :bounds {:x 0.0 :y 0.0 :w 510.0 :h 168.0}
    :container {:x 60.0 :y 680.0 :layer 18 :sibling-rank 18}
    :text-color [0.94 0.95 0.98 1.0]}
   {:vi clipped-card-vi
    :address :t2-input-floor/clipped-text
    :kind :clipped-card
    :text "office e\u0301 — edit across this clipped edge and keep every byte"
    :font-size 20.0 :line-height 28.0 :baseline-offset 21.0
    :wrap-col 27 :origin [-6.0 22.0]
    :bounds {:x 0.0 :y 0.0 :w 330.0 :h 132.0}
    :container {:x 650.0 :y 680.0 :layer 19 :sibling-rank 19}
    :text-color [1.0 1.0 1.0 0.96]}])

(defonce ^:private !booted? (atom false))
(defonce ^:private !mounted? (atom false))
(defonce ^:private !provider (atom nil))
(defonce ^:private !segmenter (atom nil))
(defonce ^:private !camera-provider (atom nil))
(defonce ^:private !effective-provider (atom nil))
(defonce ^:private !documents (atom {}))
(defonce ^:private !registrations (atom {}))
(defonce ^:private !session (atom nil))
(defonce ^:private !listeners (atom []))
(defonce ^:private !ime-host (atom nil))
(defonce ^:private !receipt (atom nil))

(defn enabled-search? [search]
  (= "1" (.get (js/URLSearchParams. (or search "")) "live-atoms")))

(defn- publish-receipt! []
  (when-let [receipt @!receipt]
    (aset js/globalThis "__softlandEditingReceipt" (clj->js receipt)))
  @!receipt)

(defn- receipt-assoc! [& kvs]
  (when @!receipt
    (apply swap! !receipt assoc kvs)
    (publish-receipt!)))

(defn- receipt-update! [key f & args]
  (when @!receipt
    (apply swap! !receipt update key f args)
    (publish-receipt!)))

(defn- spec-for-vi [vi]
  (some #(when (= vi (:vi %)) %) fixture-specs))

(defn layout-session-document
  "The only T2 owner that calls tl/layout. The result is material-local at
   origin zero/zoom one and is carried unchanged into paint and all readers."
  [spec text revision]
  (receipt-update! :seam-calls (fnil inc 0))
  (tl/layout {:text text
              :provider @!provider
              :font-size (:font-size spec)
              :line-height (:line-height spec)
              :baseline-offset (:baseline-offset spec)
              :origin [0.0 0.0]
              :wrap-policy :block-greedy
              :wrap-col (:wrap-col spec)
              :source-id (:vi spec)
              :source-revision revision
              :direction :bidi
              :zoom 1.0}))

(defn- document-record [spec text revision]
  (let [layout-result (layout-session-document spec text revision)]
    {:text text
     :revision revision
     :layout layout-result
     :boundaries (segmentation/boundaries @!segmenter text)}))

(defn selection-geometry
  "Project a tagged source range into per-line Contract-T selection rects."
  [layout-result source-range]
  (when source-range
    (let [[selection-start selection-end] (mapv tl/source-index-offset source-range)]
      (->> (:lines layout-result)
           (map-indexed
            (fn [line line-data]
              (when-not (= :header (first (:source-range line-data)))
                (let [[line-start line-end]
                      (tl/line-source-bounds (:source-range line-data))
                      start (max selection-start line-start)
                      end (min selection-end line-end)]
                  (when (< start end)
                    (let [result (tl/selection-result
                                  layout-result line (- start line-start)
                                  (- end line-start))]
                      (or (:rects result) [(:rect result)])))))))
           (mapcat identity)
           (remove nil?)
           vec))))

(defn- addressed-data [spec & [more]]
  (merge {:address (:address spec) :t2-editable? true} more))

(defn- rect-node [spec id rect color & [more-data]]
  (rt/rt-node id :rect rect
              :style {:bg color}
              :data (addressed-data spec more-data)))

(defn- active-for? [spec]
  (= (:vi spec) (get-in @!session [:target :vi])))

(defn- session-view [spec document]
  (if-not (active-for? spec)
    {:layout (:layout document)
     :boundaries (:boundaries document)
     :state nil
     :blink-visible? false
     :preedit-range nil}
    (let [session @!session]
      {:layout (:layout session)
       :boundaries (:boundaries session)
       :state (:state session)
       :blink-visible? (:blink-visible? session)
       :preedit-range (:preedit-range session)})))

(defn- editing-nodes [spec document]
  (let [{:keys [layout boundaries state blink-visible? preedit-range]}
        (session-view spec document)
        selection (some-> state editing/selection-range)
        selection-rects (selection-geometry layout selection)
        preedit-rects (selection-geometry layout preedit-range)
        composition-view (when state (editing/composition-view state))
        caret (when state
                {:index (or (:caret-index composition-view)
                            (get-in state [:caret :index]))
                 :affinity (get-in state [:caret :affinity] :downstream)})
        caret-geometry (when caret
                         (editing/caret-geometry layout caret boundaries))
        highlight-nodes
        (mapv (fn [n rect]
                (rect-node spec [:t2/selection n] rect
                           [0.30 0.52 0.96 0.34]
                           {:editing-visual :selection}))
              (range) selection-rects)
        text-node
        (rt/rt-node
         :t2/text :text
         (get-in layout [:metrics :logical-bounds])
         :text (tl/line-paint-ops
                layout
                (let [[r g b a] (:text-color spec)]
                  {:size (:font-size spec) :r r :g g :b b :a a :type :normal}))
         :data (addressed-data spec
                               {:layout/id (:layout/id layout)
                                :paint-source :t2/session-layout}))
        underline-nodes
        (mapv (fn [n {:keys [x y w h]}]
                (rect-node spec [:t2/preedit n]
                           {:x x :y (+ y h -2.0) :w w :h 1.5}
                           [0.96 0.82 0.32 1.0]
                           {:editing-visual :preedit-underline}))
              (range) preedit-rects)
        caret-node
        (when (and blink-visible? caret-geometry)
          (rect-node spec :t2/caret
                     (assoc (:rect caret-geometry) :w 2.0)
                     [0.98 0.98 1.0 1.0]
                     {:editing-visual :caret}))]
    (cond-> (into highlight-nodes [text-node])
      (seq underline-nodes) (into underline-nodes)
      caret-node (conj caret-node))))

(defn- fixture-tree [spec document]
  (let [[content-x content-y] (:origin spec)
        root-data (addressed-data spec
                                  (when (= :clipped-card (:kind spec))
                                    {:gpu-clip? true}))
        background-color (if (= :clipped-card (:kind spec))
                           [0.14 0.31 0.58 1.0]
                           [0.075 0.085 0.11 0.96])
        background (rect-node spec :t2/background (:bounds spec)
                              background-color)
        content (rt/rt-node
                 :t2/content :group
                 {:x content-x :y content-y
                  :w (:w (:bounds spec)) :h (:h (:bounds spec))}
                 :data (addressed-data spec)
                 :children (editing-nodes spec document))]
    (rt/rt-node (:address spec) :group (:bounds spec)
                :clip? (= :clipped-card (:kind spec))
                :data root-data
                :children [background content])))

(defn- upsert-tree! [vi]
  (when-let [spec (spec-for-vi vi)]
    (when-let [slot (scene-store/slot (scene-runtime/store-snapshot) vi)]
      (let [document (get @!documents vi)
            tree (fixture-tree spec document)]
        (swap! scene-runtime/!scene-store scene-store/upsert-slot vi
               {:tree tree
                :container (:container slot)
                :container-slot (:container-slot slot)
                :stack-path (:stack-path slot)
                :meta (:meta slot)
                :stratum (:stratum slot)})
        (when (= vi (get-in @!session [:target :vi]))
          (let [open-container (get-in @!session [:container-at-open])
                current-container (:container
                                   (scene-store/slot
                                    (scene-runtime/store-snapshot) vi))
                visual-count
                (count (filter #(get-in % [:data :editing-visual])
                               (tree-seq (comp seq :children) :children tree)))
                max-visual-count
                (max visual-count
                     (get-in @!receipt [:census :max-session-visual-count] 0))]
            (swap! !receipt assoc :layout-id
                   (get-in @!session [:layout :layout/id])
                   :census {:container-at-open open-container
                            :container-current current-container
                            :container-stable? (= open-container current-container)
                            :session-visual-count visual-count
                            :max-session-visual-count max-visual-count})
            (publish-receipt!)))
        tree))))

(defn- install-fixtures! []
  (doseq [spec fixture-specs]
    (let [document (document-record spec (:text spec) 0)
          registration (scene-runtime/register-face-instance!
                        (:vi spec) (fixture-tree spec document)
                        (merge {:scale 1.0 :stratum :world
                                :meta {:live-atoms? true
                                       :t2-input-floor? true
                                       :manipulable-fixture? true
                                       :material/id (:address spec)
                                       :material/revision 1}}
                               (:container spec)))]
      (swap! !documents assoc (:vi spec) document)
      (swap! !registrations assoc (:vi spec) registration))))

(defn- event-point [event]
  (let [sx (double (.-clientX event))
        sy (double (.-clientY event))
        {:keys [x y zoom]} (if @!camera-provider
                             (@!camera-provider)
                             {:x 0.0 :y 0.0 :zoom 1.0})]
    {:screen [sx sy]
     :world [(/ (- sx (or x 0.0)) (or zoom 1.0))
             (/ (- sy (or y 0.0)) (or zoom 1.0))]}))

(defn- pick-event [event]
  (scene-runtime/pick-world (event-point event)))

(defn hit-stop
  "The T2 caret subroute: object pick is already complete; local point minus
   the fixture's text origin enters the retained layout's hit-test reader."
  [spec document point-local]
  (let [[origin-x origin-y] (:origin spec)
        [x y] point-local]
    (tl/hit-test-result (:layout document)
                        [(- x origin-x) (- y origin-y)]
                        {:grapheme-boundaries
                         (get-in document [:boundaries :grapheme])})))

(defn- current-caret-screen-point []
  (when-let [{:keys [target state layout boundaries]} @!session]
    (let [spec (spec-for-vi (:vi target))
          view (editing/composition-view state)
          caret {:index (:caret-index view)
                 :affinity (get-in state [:caret :affinity] :downstream)}
          geometry (editing/caret-geometry layout caret boundaries)
          [origin-x origin-y] (:origin spec)
          local [(+ origin-x (first (:position geometry)))
                 (+ origin-y (second (:position geometry)))]
          slot (scene-store/slot (scene-runtime/store-snapshot) (:vi target))
          effective (when @!effective-provider
                      (get (@!effective-provider) (:container slot)))
          [world-x world-y] (if effective
                              (containers/forward-point effective local)
                              local)
          {:keys [x y zoom]} (if @!camera-provider
                               (@!camera-provider)
                               {:x 0.0 :y 0.0 :zoom 1.0})]
      [(+ (* world-x (or zoom 1.0)) (or x 0.0))
       (+ (* world-y (or zoom 1.0)) (or y 0.0))])))

(defn- position-ime-host! []
  (when-let [host @!ime-host]
    (when-let [[x y] (current-caret-screen-point)]
      (set! (.. host -style -left) (str x "px"))
      (set! (.. host -style -top) (str y "px"))
      (receipt-assoc! :ime-host {:focused? (= host (.-activeElement js/document))
                                 :screen-point [x y]}))))

(defn- arm-blink! [logical-time]
  (when @!session
    (swap! !session assoc :blink-visible? true)
    (frame-scheduler/register-deadline!
     blink-deadline-id
     {:next-deadline (+ (max 0.0 (double (or logical-time 0.0)))
                        blink-cadence-ms)
      :cadence blink-cadence-ms
      :stop-predicate #(nil? @!session)})
    (receipt-assoc! :blink-armed true)))

(defn- retire-blink! []
  (frame-scheduler/unregister-deadline! blink-deadline-id)
  (receipt-assoc! :blink-armed false))

(defn- consume-due-deadlines! [due-ids]
  (when (and @!session (some #{blink-deadline-id} due-ids))
    (let [before (:seam-calls @!receipt)]
      (swap! !session update :blink-visible? not)
      (receipt-update! :blink-toggles (fnil inc 0))
      (upsert-tree! (get-in @!session [:target :vi]))
      (receipt-assoc! :last-blink-seam-delta
                      (- (:seam-calls @!receipt) before)))))

(declare close-session! apply-kernel-result!)

(defn- open-session!
  [vi caret affinity logical-time]
  (when-let [spec (spec-for-vi vi)]
    (when @!session (close-session!))
    (let [document (get @!documents vi)
          slot (scene-store/slot (scene-runtime/store-snapshot) vi)
          state (editing/initial-state {:text (:text document)
                                        :revision (:revision document)
                                        :caret caret
                                        :affinity affinity})]
      (reset! !session {:target {:vi vi :address (:address spec)}
                        :state state
                        :layout (:layout document)
                        :document-layout (:layout document)
                        :boundaries (:boundaries document)
                        :view-text (:text document)
                        :preedit-range nil
                        :blink-visible? true
                        :drag nil
                        :container-at-open (:container slot)})
      (receipt-assoc! :session-target {:vi vi :address (:address spec)}
                      :layout-id (get-in document [:layout :layout/id]))
      (when-let [host @!ime-host]
        (set! (.-value host) "")
        (.focus host #js {:preventScroll true}))
      (arm-blink! logical-time)
      (upsert-tree! vi)
      (position-ime-host!)
      @!session)))

(defn close-session!
  ([] (close-session! :explicit))
  ([reason]
   (when-let [session @!session]
     (let [state (:state session)
           cancelled (:state (editing/composition-cancel state))
           vi (get-in session [:target :vi])]
       (swap! !session assoc :state cancelled)
       (retire-blink!)
       (reset! !session nil)
       (when-let [host @!ime-host]
         (set! (.-value host) "")
         (.blur host))
       (upsert-tree! vi)
       (receipt-assoc! :session-target nil
                       :exit-reason reason
                       :blink-armed false)))
   nil))

(defn- relayout-view!
  [old-session next-state]
  (let [vi (get-in old-session [:target :vi])
        spec (spec-for-vi vi)
        document (get @!documents vi)
        view (editing/composition-view next-state)
        committed-changed? (not= (get-in old-session [:state :document :revision])
                                 (get-in next-state [:document :revision]))
        view-changed? (not= (:view-text old-session) (:text view))]
    (cond
      committed-changed?
      (let [next-document (document-record
                           spec (get-in next-state [:document :text])
                           (get-in next-state [:document :revision]))]
        (swap! !documents assoc vi next-document)
        (assoc old-session
               :state next-state
               :layout (:layout next-document)
               :document-layout (:layout next-document)
               :boundaries (:boundaries next-document)
               :view-text (:text next-document)
               :preedit-range nil))

      (and (editing/composing? next-state) view-changed?)
      (let [layout-result
            (layout-session-document
             spec (:text view)
             [(get-in next-state [:document :revision])
              :composition
              (get-in next-state [:composition :preedit])])]
        (assoc old-session
               :state next-state
               :layout layout-result
               :boundaries (segmentation/boundaries @!segmenter (:text view))
               :view-text (:text view)
               :preedit-range (:preedit-range view)))

      (and (not (editing/composing? next-state))
           (not= (:layout old-session) (:document-layout old-session)))
      (assoc old-session
             :state next-state
             :layout (:document-layout old-session)
             :boundaries (:boundaries document)
             :view-text (:text document)
             :preedit-range nil)

      :else
      (assoc old-session :state next-state
                         :preedit-range (:preedit-range view)))
    ;; The caller records the exact call delta after installing this value.
    ))

(defn apply-kernel-result!
  [result logical-time transition]
  (when @!session
    (if (:exit? result)
      (close-session! :escape)
      (let [old-session @!session
            before (:seam-calls @!receipt)
            next-session (relayout-view! old-session (:state result))
            vi (get-in old-session [:target :vi])]
        (reset! !session next-session)
        (arm-blink! logical-time)
        (upsert-tree! vi)
        (position-ime-host!)
        (receipt-assoc!
         :last-transition {:kind transition
                           :status (:status result)
                           :ops (:ops result)
                           :seam-delta (- (:seam-calls @!receipt) before)}
         :last-paste-refusal (when (= :paste/over-budget (:reason result))
                               (select-keys result [:reason :budget :code-units]))))))
  result)

(defn- raw-key-event [event]
  {:key (.-key event)
   :ctrl? (.-ctrlKey event)
   :meta? (.-metaKey event)
   :alt? (.-altKey event)
   :shift? (.-shiftKey event)
   :is-composing? (.-isComposing event)
   :key-code (.-keyCode event)})

(defn- write-clipboard! [text]
  (when (seq text)
    (if-let [clipboard (.-clipboard js/navigator)]
      (-> (.writeText clipboard text)
          (.then #(receipt-assoc! :clipboard-write {:ok? true :text text}))
          (.catch #(receipt-assoc! :clipboard-write
                                   {:ok? false :error (str %)})))
      (receipt-assoc! :clipboard-write {:ok? false :error :clipboard-unavailable}))))

(defn- handle-keydown! [event]
  (if-not @!session
    false
    (let [event-map (raw-key-event event)
          decision (editing/dispatch-key event-map)
          pass-default? (#{:pass-through :native-paste :drop-composing-key}
                         (:action decision))
          result (editing/handle-key
                  (get-in @!session [:state]) event-map
                  {:boundaries (:boundaries @!session)
                   :layout-result (:layout @!session)})]
      (when-not pass-default? (.preventDefault event))
      (when (#{:copy :cut} (:status result))
        (write-clipboard! (:clipboard result)))
      (when-not (#{:pass-through :native-paste :dropped-composing-key}
                 (:status result))
        (apply-kernel-result! result (.-timeStamp event) :keydown))
      (receipt-update! :keydown-intercepts (fnil inc 0))
      true)))

(defn- handle-paste! [event]
  (if-not @!session
    false
    (let [text (.getData (.-clipboardData event) "text/plain")
          result (editing/paste (get-in @!session [:state]) text)]
      (.preventDefault event)
      (apply-kernel-result! result (.-timeStamp event) :paste)
      (receipt-update! :paste-intercepts (fnil inc 0))
      true)))

(defn- update-composition! [preedit caret logical-time source]
  (when @!session
    (let [state (get-in @!session [:state])
          result (editing/composition-update state preedit caret)]
      (when (or (not= (get-in state [:composition :preedit])
                      (get-in (:state result) [:composition :preedit]))
                (not= (get-in state [:composition :caret-in-preedit])
                      (get-in (:state result) [:composition :caret-in-preedit])))
        (apply-kernel-result! result logical-time source)))))

(defn- install-ime-host! []
  (let [host (.createElement js/document "textarea")]
    (set! (.-id host) "softland-t2-ime-host")
    (set! (.-ariaLabel host) "Softland text composition input")
    (set! (.-spellcheck host) false)
    (set! (.. host -style -position) "fixed")
    (set! (.. host -style -width) "1px")
    (set! (.. host -style -height) "1px")
    (set! (.. host -style -opacity) "0")
    (set! (.. host -style -pointerEvents) "none")
    (set! (.. host -style -zIndex) "-1")
    (.appendChild (.-body js/document) host)
    (.addEventListener
     host "keydown"
     (fn [event]
       (when (handle-keydown! event)
         (.stopPropagation event))))
    (.addEventListener
     host "compositionstart"
     (fn [event]
       (when @!session
         (set! (.-value host) "")
         (apply-kernel-result!
          (editing/composition-start (get-in @!session [:state]))
          (.-timeStamp event) :composition-start))))
    (.addEventListener
     host "compositionupdate"
     (fn [event]
       (let [preedit (or (.-data event) (.-value host) "")
             caret (or (.-selectionStart host) (tl/code-unit-count preedit))]
         (update-composition! preedit caret (.-timeStamp event)
                              :composition-update))))
    (.addEventListener
     host "input"
     (fn [event]
       (when (and @!session (editing/composing? (get-in @!session [:state])))
         (let [preedit (.-value host)
               caret (or (.-selectionStart host) (tl/code-unit-count preedit))]
           (update-composition! preedit caret (.-timeStamp event)
                                :composition-input)))))
    (.addEventListener
     host "compositionend"
     (fn [event]
       (when @!session
         (let [result (editing/composition-end
                       (get-in @!session [:state]) (or (.-data event) ""))]
           (set! (.-value host) "")
           (apply-kernel-result! result (.-timeStamp event) :composition-end)))))
    (reset! !ime-host host)
    host))

(defn- fixture-hit? [hit]
  (boolean (and hit (spec-for-vi (:vi hit)))))

(defn- open-from-event! [event hit]
  (let [spec (spec-for-vi (:vi hit))
        document (get @!documents (:vi hit))
        stop (hit-stop spec document (:point-local hit))]
    (open-session! (:vi hit) (:index stop) (:affinity stop)
                   (.-timeStamp event))))

(defn- handle-dblclick! [event]
  (let [hit (pick-event event)]
    (cond
      (and @!session (= (:vi hit) (get-in @!session [:target :vi]))
           (not (.-shiftKey event)))
      (let [spec (spec-for-vi (:vi hit))
            stop (hit-stop spec {:layout (:layout @!session)
                                 :boundaries (:boundaries @!session)}
                           (:point-local hit))
            result (editing/select-word (get-in @!session [:state])
                                        (:boundaries @!session) (:index stop))]
        (.preventDefault event)
        (.stopImmediatePropagation event)
        (apply-kernel-result! result (.-timeStamp event) :word-select))

      (and (nil? @!session) (fixture-hit? hit))
      (do (.preventDefault event)
          (.stopImmediatePropagation event)
          (open-from-event! event hit))

      :else nil)))

(defn- handle-outside-pointerdown! [event]
  (when @!session
    (let [hit (pick-event event)]
      (when (not= (:vi hit) (get-in @!session [:target :vi]))
        (close-session! :outside-pointer)))))

(defn- handle-mousedown! [event]
  (when @!session
    (let [hit (pick-event event)
          target-vi (get-in @!session [:target :vi])]
      (cond
        (.-shiftKey event)
        (receipt-update! :pointer-pass-throughs (fnil inc 0))

        (= (:vi hit) target-vi)
        (let [spec (spec-for-vi target-vi)
              stop (hit-stop spec {:layout (:layout @!session)
                                   :boundaries (:boundaries @!session)}
                             (:point-local hit))
              state (get-in @!session [:state])
              next-state (assoc state :caret {:index (:index stop)
                                               :affinity (:affinity stop)}
                                      :anchor nil :desired-x nil)]
          (.preventDefault event)
          (.stopImmediatePropagation event)
          (swap! !session assoc :state next-state :drag (:index stop))
          (arm-blink! (.-timeStamp event))
          (upsert-tree! target-vi)
          (position-ime-host!))

        :else nil))))

(defn- handle-mousemove! [event]
  (when-let [anchor (:drag @!session)]
    (let [hit (pick-event event)
          target-vi (get-in @!session [:target :vi])]
      (when (= (:vi hit) target-vi)
        (let [spec (spec-for-vi target-vi)
              stop (hit-stop spec {:layout (:layout @!session)
                                   :boundaries (:boundaries @!session)}
                             (:point-local hit))]
          (.preventDefault event)
          (.stopImmediatePropagation event)
          (swap! !session update :state
                 #(assoc % :anchor anchor
                           :caret {:index (:index stop)
                                   :affinity (:affinity stop)}
                           :desired-x nil))
          (arm-blink! (.-timeStamp event))
          (upsert-tree! target-vi)
          (position-ime-host!))))))

(defn- handle-mouseup! [event]
  (when (:drag @!session)
    (.preventDefault event)
    (.stopImmediatePropagation event)
    (swap! !session assoc :drag nil)))

(defn- handle-click! [event]
  (when (and @!session (not (.-shiftKey event))
             (= (:vi (pick-event event)) (get-in @!session [:target :vi])))
    (.preventDefault event)
    (.stopImmediatePropagation event)))

(defn- add-window-listener! [event-name handler capture?]
  (.addEventListener js/window event-name handler capture?)
  (swap! !listeners conj [event-name handler capture?]))

(defn- mount! []
  (when (compare-and-set! !mounted? false true)
    (install-ime-host!)
    (add-window-listener! "pointerdown" handle-outside-pointerdown! true)
    (add-window-listener! "mousedown" handle-mousedown! true)
    (add-window-listener! "mousemove" handle-mousemove! true)
    (add-window-listener! "mouseup" handle-mouseup! true)
    (add-window-listener! "click" handle-click! true)
    (add-window-listener! "dblclick" handle-dblclick! true))
  true)

(defn boot!
  "Boot after chrome and frame runtime. Caller supplies the already-live font,
   camera, and effective-transform providers; no provider fallback is hidden."
  [{:keys [layout-provider camera-provider effective-provider]}]
  (if @!booted?
    (publish-receipt!)
    (let [segmenter (segmentation/create-provider)
          provider-environment (select-keys layout-provider
                                            [:face-id :face-revision :shaper-id
                                             :shaper-version])]
      (reset! !receipt {:enabled true
                        :session-target nil
                        :layout-id nil
                        :seam-calls 0
                        :font-shaper-environment provider-environment
                        :segmentation-provider
                        (segmentation/provider-identity segmenter)
                        :blink-armed false
                        :blink-toggles 0
                        :due-consumer-installed false
                        :intercepts-installed false
                        :keydown-intercepts 0
                        :paste-intercepts 0
                        :pointer-pass-throughs 0
                        :census {}})
      (reset! !provider layout-provider)
      (reset! !segmenter segmenter)
      (reset! !camera-provider camera-provider)
      (reset! !effective-provider effective-provider)
      (install-fixtures!)
      (events/install-keydown-intercept! handle-keydown!)
      (mouse/install-paste-intercept! handle-paste!)
      (render/install-due-deadline-consumer! consume-due-deadlines!)
      (mount!)
      (reset! !booted? true)
      (receipt-assoc! :fixtures (mapv :vi fixture-specs)
                      :intercepts-installed true
                      :due-consumer-installed true
                      :provider-shaped?
                      (and (ifn? (:shape-line layout-provider))
                           (not= :legacy/code-unit-grid
                                 (:shaper-id layout-provider))))
      (publish-receipt!))))

(defn teardown! []
  (close-session! :teardown)
  (doseq [[event-name handler capture?] @!listeners]
    (.removeEventListener js/window event-name handler capture?))
  (reset! !listeners [])
  (when-let [host @!ime-host]
    (.remove host))
  (reset! !ime-host nil)
  (events/install-keydown-intercept! nil)
  (mouse/install-paste-intercept! nil)
  (render/install-due-deadline-consumer! nil)
  (doseq [vi (keys @!registrations)]
    (scene-runtime/close-instance! vi))
  (reset! !registrations {})
  (reset! !documents {})
  (reset! !mounted? false)
  (reset! !booted? false)
  (js-delete js/globalThis "__softlandEditingReceipt")
  true)

;; Browser-verifier controls exercise the real runtime state and listener
;; paths; they are intentionally thin and do not duplicate kernel behavior.
(defn open-session-for-verifier!
  ([vi source-offset] (open-session-for-verifier! vi source-offset 0.0))
  ([vi source-offset logical-time]
   (open-session! vi (editing/tagged-offset source-offset) :downstream logical-time)))

(defn set-selection-for-verifier! [anchor caret affinity logical-time]
  (when @!session
    (swap! !session update :state
           #(assoc % :anchor (editing/tagged-offset anchor)
                     :caret {:index (editing/tagged-offset caret)
                             :affinity affinity}
                     :desired-x nil))
    (arm-blink! logical-time)
    (upsert-tree! (get-in @!session [:target :vi])))
  @!session)

(defn dispatch-paste-for-verifier! [event]
  (handle-paste! event))

(defn consume-deadlines-for-verifier! [due-ids]
  (consume-due-deadlines! due-ids)
  (publish-receipt!))

(defn fixture-render-snapshot [vi]
  (let [slot (scene-store/slot (scene-runtime/store-snapshot) vi)]
    {:vi vi
     :container (:container slot)
     :ops (:ops slot)
     :tree (:tree slot)
     :document (get @!documents vi)
     :session (when (= vi (get-in @!session [:target :vi])) @!session)}))

(defn session-snapshot [] @!session)
(defn document-snapshot [vi] (get @!documents vi))
(defn ime-host [] @!ime-host)
(defn receipt [] (publish-receipt!))
