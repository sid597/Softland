(ns app.client.workspace.ground
  "first-light A · P2b — the OPEN ground (CONTRACT §3.1 + §7 P2b, WALKTHROUGH
   moments 0–6). Arrival renders NOTHING: zero content pixels, zero caret, no
   hidden focused input — the substrate never chooses the first position; the
   symmetry is broken by the inhabitant, literally, with a click (Law 1).

   The P2 tip pattern (pre-placed blank-birth caret + the client-only typed
   buffer) DIED in this diff — deleted, never gated (the swap rule).

   What lives here (client glue, S2 shape — the pure machine is
   ground_edit.cljc):
   - the world camera (pan/zoom; shader terms: screen = world·zoom + pan) —
     wheel zooms at the pointer, drag on empty ground pans (Laws 2/3/9);
   - per-block scene slots in their OWN containers (the container-transform
     substrate): click = caret anchor on ground / edit caret on a block;
     press-drag past ~4 CSS px moves a block or pans (§9.4 grammar);
   - birth at the FIRST content act (POST /api/episode/block-birth — unit +
     birth-position in ONE acked import; Escape before content leaves
     NOTHING);
   - committed-echo typing: every keystroke rides the block-write
     :object/edit artery; glyphs + caret render from the ONE confirmed value
     (ground_edit's invisible intent queue — receipt P2B.md §b);
   - Ctrl+Enter = revision-pinned send from the FOCUSED block on the
     existing durable-BEFORE-agent lane; busy resident → visible transient
     refusal AT the block, no queue; the open turn renders an explicitly
     PROVISIONAL projection (process-state, never truth — Law 8) REPLACED at
     distill by the durable provenance-marked reply (Law 6);
   - camera + positions persist as SETTLE-STATE truth only: gesture-end ARMS
     the settle write, ~400ms debounce coalesces bursts, exit flush is a
     best-effort belt — SAFETY IS THE ACKNOWLEDGED SETTLE WRITE
     (/api/episode/geometry; settled cells, P2B.md receipt a). Return
     restores camera + positions, never focus/caret/hover/selection."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [missionary.core :as m]
            [app.client.workspace.agent :as agent]
            [app.client.workspace.block-edit-wiring :as bew]
            [app.client.workspace.face-wiring :as face-wiring]
            [app.client.workspace.ground-edit :as ge]
            [app.client.workspace.rect-tree :as rt :refer [rt-node]]
            [app.client.workspace.scene-runtime :as scene-rt]
            [app.client.workspace.scene-store :as ss]
            [app.shared.attention-material :as attention-material]
            [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.foldable-material :as foldable-material]
            [app.shared.material-inspector :as material-inspector]
            [app.shared.positioned-material :as positioned-material]
            [app.shared.provenance-material :as provenance-material]
            [app.shared.reply-to-block :as reply-to-block]
            [app.shared.space-material :as space-material]
            [app.shared.text-body-material :as text-body-material]
            [app.shared.threaded-material :as threaded-material]
            [app.shared.verb-registry :as verb-registry]))

;; ===========================================================================
;; Boot decision
;; ===========================================================================

(defn ground-boot?
  "The PRODUCT boot is the ground (T9). Builders launch the dev workspace
   explicitly with ?dev — never a link, never a keystroke, from the ground."
  []
  (not (str/includes? (str (.-search js/window.location)) "dev")))

(defn- drill-conversation-id
  "The G4b drill seam's client half (§11: machinery drills run on their OWN
   episode; the genesis stays virgin). ?drill=<conv-id> threads through the
   face pull + all three write lanes; absent = the genesis episode."
  []
  (second (re-find #"[?&]drill=([^&]+)" (str (.-search js/window.location)))))

;; ===========================================================================
;; Module state
;; ===========================================================================

(defonce ^:private !refs (atom nil))

(defonce !camera
  ;; shader terms (renderer.cljs vertex): screen = world·zoom + pan.
  (atom {:x 0.0 :y 0.0 :zoom 1.0}))

(defonce !ground-edit (atom nil))   ; ge/init at install (fresh client-id)

(defonce ^:private !world
  ;; :blocks {unit-id {:vi :cid :x :y :w :h :machine? :local? :settled {:x :y}}}
  ;; :context — last served face-context · :camera-restored? — boot-once flag
  (atom {:blocks {} :context nil :camera-restored? false}))

(defonce !ground-runs
  ;; Law 8, per-thread (one canvas, many conversations): thread-key → one
  ;; run map {:phase :activity :stream-text :error :source-unit-id :turn-id
  ;; :wrap-source-columns :await-reply}. thread-key = the thread's session uuid
  ;; string;
  ;; nil = the genesis thread. Every run is explicitly PROVISIONAL
  ;; (process-state, never truth); each projection anchors beneath ITS OWN
  ;; source block.
  ;; Busy is a per-thread fact — a mid-turn thread refuses at its block,
  ;; every other thread stays sendable.
  (atom {}))

(defonce ^:private !last-turn-ids
  ;; thread-key → last durable turn-id (the per-thread prev-turn chain)
  (atom {}))

(defonce ^:private !thread-of
  ;; unit-id → thread-key. No entry = no thread yet — the next Ctrl+Enter
  ;; mints a fresh uuid ("a block holds a multi-turn thread on one uuid").
  ;; EVERY block rides the new lanes (Sid 2026-07-21) — pre-thread turn
  ;; records map to nothing, so old blocks fork fresh lanes too; only drill
  ;; pages hold nil entries (their legacy single lane). Rebuilt from served
  ;; turn records on every reconcile; session mints added at send.
  (atom {}))

(defonce ^:private !provisional-open (atom #{}))  ; thread-keys with live slots

(defonce ^:private !pointer (atom {:phase :idle}))
(defonce ^:private !hover (atom nil))

(defonce ^:private !settle
  ;; :dirty {unit-id {:x :y}} :camera? bool :timer id
  ;; :acked {unit-id {:x :y}} — the revert base for a refused write (G4b)
  (atom {:dirty {} :camera? false :timer nil :acked {}}))

(defonce ^:private !notice (atom nil))  ; {:unit-id :text} transient (busy etc.)
(defonce ^:private !halo (atom nil))
;; per merged run block: which sections show (Task 7, Sid) — two independent
;; header toggles, :noise? (thinking + tool calls) and :prose? (the reply).
;; attention state like hover: ephemeral, never settled, never restored
(defonce ^:private !folds (atom {}))

;; Task 11: read-only selection over a MACHINE block's rendered lines —
;; {:uid :anchor {:line :col} :head {:line :col}}; attention state only
(defonce ^:private !machine-sel (atom nil))

;; Task 18 (Sid): marquee group-selection — shift+drag on EMPTY ground sweeps
;; a rect; blocks inside join the group. Pure attention state (Law 10): a
;; click, Escape, or a fresh marquee dissolves it; never saved, never restored.
(defonce ^:private !group-sel (atom #{}))

;; Task 5 observability (report()): sig-skip efficacy + reconcile timings
(defonce ^:private !rebuild-stats (atom {:builds 0 :skips 0}))
(defonce ^:private !reconcile-samples (atom []))

(def ^:private drag-threshold-px 4.0)
(def ^:private settle-debounce-ms 400)

(declare rebuild-block! reconcile! refresh-provisional! context-block-entry)

;; ===========================================================================
;; Camera math (screen = world·zoom + pan)
;; ===========================================================================

(defn- screen->world [sx sy]
  (let [{:keys [x y zoom]} @!camera]
    [(/ (- sx x) zoom) (/ (- sy y) zoom)]))

;; ===========================================================================
;; Metrics + block trees
;; ===========================================================================

(defn- metrics []
  (let [{:keys [!settings !active-font !viewport]} (:atoms @!refs)
        fs (:font-size @!settings 19)
        cw (:char-width @!active-font 0.56)]
    {:font-size fs
     :char-advance (* fs cw)
     :line-h (js/Math.round (* fs 1.4))
     :viewport @!viewport}))

(defn- text-op
  ;; :y is the glyph BASELINE (renderer convention).
  [text i line-h fs [r g b a] x]
  {:text text :type :text :from 0 :to (count text)
   :x x :y (+ (* i line-h) fs) :size fs
   :r r :g g :b b :a a})

(def ^:private fg [0.92 0.92 0.94 1.0])
(def ^:private dim [0.55 0.58 0.62 1.0])
(def ^:private err-col [0.95 0.45 0.40 1.0])
(def ^:private amber [0.92 0.75 0.35 1.0])

(def ^:private fold-sections
  "The run block's header rows, in render order — each names the section it
   labels and the `:foldable/defaults` key it toggles. P5: this vector IS the
   header grammar. Row 0/1 arithmetic used to live in the pointer path; now the
   header's own node is what the pick lands on, so the index is a render fact
   only and the kernel never computes a row again."
  [{:section :noise :fold-key :noise? :text-key :noise-text}
   {:section :prose :fold-key :prose? :text-key :reply-text}])


(defonce ^:private !wears-cache
  ;; {:served <the exact served value> :wears {facet → wear}}
  ;; P5 note (echo bar): reconcile rebuilds EVERY block per served context —
  ;; i.e. per keystroke — and each rebuild resolved every wear from scratch,
  ;; re-running every grammar validator. Adding binding rows to three grammars
  ;; would have multiplied that. The served value is replaced wholesale by the
  ;; face-materials watch, so `identical?` is an exact cache key: one resolve
  ;; per revision instead of one per block. It also makes `wears` the SAME
  ;; object in every block's render signature, so the per-keystroke sig compare
  ;; short-circuits on identity instead of deep-walking six maps.
  (atom nil))

(defn- resolve-material-wears
  [by-id]
  {:provenance
   (provenance-material/resolved-wear
    (get by-id provenance-material/master-id))
   :attention
   (attention-material/resolved-wear
    (get by-id attention-material/master-id))
   :foldable
   (foldable-material/resolved-wear
    (get by-id foldable-material/master-id))
   :positioned
   (positioned-material/resolved-wear
    (get by-id positioned-material/master-id))
   :space
   (space-material/resolved-wear
    (get by-id space-material/master-id))
   :threaded
   (threaded-material/resolved-wear
    (get by-id threaded-material/master-id))
   :text-body
   (text-body-material/resolved-wear
    (get by-id text-body-material/master-id))})

(defonce ^:private !preview
  ;; P6 · deliverable 3 — the PREVIEW MEMBRANE.
  ;;
  ;; {:master-id … :revision-id … :base <the real served value>
  ;;  :overlay <the same value with ONE master replaced by the candidate>}
  ;;
  ;; A preview is a client-side overlay over the REAL served material (T9): the
  ;; candidate replaces exactly one master and every other facet stays the
  ;; genuinely-served active revision. The active face is byte-untouched —
  ;; nothing durable moves, no pointer is edited, no request is appended.
  ;;
  ;; The overlay is a FRESH object, so `identical?` against the wears cache
  ;; misses and EVERYTHING re-derives under the candidate. That is the P3
  ;; reconcile-before-stamp law applied to the preview path verbatim, and it is
  ;; why no derived state from the previous revision can survive into the
  ;; preview — the cache cannot serve a stale wear it never keyed.
  (atom nil))

(defn- served-material-value
  "The value the client resolves wear from: the real serve, or the preview
   overlay while a candidate is being previewed. ONE door, so no render path
   can accidentally read past the membrane."
  []
  (let [!served (get-in @!refs [:atoms :!facet-materials])
        served (when !served @!served)
        preview @!preview]
    (if (and preview (identical? served (:base preview)))
      (:overlay preview)
      ;; the serve moved underneath the preview — the preview is stale and the
      ;; REAL material wins. A membrane that outlived its base would be showing
      ;; a candidate against material that no longer exists.
      served)))

(defn- current-material-wears
  "The SHARED tier: the seven masters as any subject wears them absent a
   deviation. P6 keeps this exactly as P5 left it — one resolve per served
   identity — and adds the per-subject tier beside it in `wears-for`."
  []
  (let [served (served-material-value)
        cached @!wears-cache]
    (if (and cached (identical? served (:served cached)))
      (:wears cached)
      (let [wears (resolve-material-wears (:facet-materials/by-id served))
            space-subject-instance?
            (boolean
             (some (fn [[_facet by-subject]]
                     (contains? by-subject space-material/space-subject))
                   (:facet-materials/instances served)))
            ;; T-R2/T5 — the wheel's fixed subject must be a by-subject cache HIT
            ;; even on the day-one no-instance path. If an instance is served,
            ;; leave the slot cold so its first read resolves and memoizes it.
            by-subject
            (if space-subject-instance?
              {}
              {space-material/space-subject wears})]
        (reset! !wears-cache
                {:served served :wears wears :by-subject by-subject})
        wears))))

(defn- wears-for
  "P6 · R2 — ONE subject's wears: instance revision → shared active → floor.

   T5 is the reason for the shape. Reconcile rebuilds every block per served
   context (i.e. per keystroke), so resolving instance masters per block per
   render is precisely the storm that kills the echo bar. Two properties keep
   it cheap:

   - a subject with NO deviation gets the SHARED wears object BACK BY IDENTITY,
     so the per-keystroke render-signature compare still short-circuits on
     `identical?` for every ordinary block — which is all of them, almost
     always;
   - a deviant subject resolves ONCE per served identity and is memoized in the
     same cache, which the served value's identity already invalidates.

   `:facet-materials/instances` is `{facet → {subject → served-instance}}`, so
   the lookup for a non-deviant subject costs one map probe per DEVIATING facet
   — not per facet, and not per block."
  [subject]
  (let [shared (current-material-wears)
        cached @!wears-cache
        instances (:facet-materials/instances (:served cached))]
    (if-let [hit (get-in cached [:by-subject subject])]
      hit
      (let [mine (persistent!
                  (reduce-kv
                   (fn [m facet by-subject]
                     (if-let [i (get by-subject subject)]
                       (assoc! m facet i)
                       m))
                   (transient {})
                   instances))]
        (if (zero? (count mine))
          shared
          (let [by-id (:facet-materials/by-id (:served cached))
                w (reduce-kv
                   (fn [acc facet inst]
                     (if-let [spec (facet-masters/spec-for-facet facet)]
                       (assoc acc facet
                              (facet-material/wear-for-subject
                               spec
                               (get by-id (:facet-master/id spec))
                               inst))
                       acc))
                   shared
                   mine)]
            (swap! !wears-cache assoc-in [:by-subject subject] w)
            w))))))

;; ===========================================================================
;; Halo P1 — subject condensation as a GPU scene citizen
;; ===========================================================================

(def ^:private halo-vi :ground-halo)

(def ^:private block-wear-census
  "The six real block wears. Interaction claim facets are intentionally not
   consulted: a claim says where a gesture landed, not what the citizen wears."
  [:provenance :attention :foldable :positioned :threaded :text-body])

(defn- halo-effect-label
  [verb-name]
  (some-> (verb-registry/effect-class verb-name) name))

(defn- halo-master-census
  [subject]
  (let [space? (= :space subject)
        subject-id (if space?
                     space-material/space-subject
                     (str subject))
        wears (wears-for subject-id)
        facets (if space? [:space] block-wear-census)]
    {:subject subject
     :subject-id subject-id
     :masters
     (->> facets
          (keep
           (fn [facet]
             (when-let [spec (get facet-masters/by-facet facet)]
               (when-let [wear (get wears facet)]
                 {:facet facet
                  :master-id (:facet-master/id spec)
                  :revision-id (:facet-master/revision-id wear)
                  :floor? (true? (:facet-master/floor? wear))
                  :tier (or (:facet-master/tier wear)
                            (if (:facet-master/floor? wear)
                              :floor
                              :shared))}))))
          vec)}))

(defn- halo-master-label
  [{:keys [facet master-id revision-id floor? tier]}]
  (str (name facet) " · " master-id " · "
       (if floor?
         "code-owned · floored"
         (str revision-id " · " (name tier)))))

(defn- halo-rows
  [{:keys [subject-id masters questions status]}]
  (let [preview-effect (halo-effect-label :matter/preview)
        say-effect (halo-effect-label :matter/say)
        base
        (into
         [{:text (str "halo · " subject-id)}
          {:text "ask · pure-projection"
           :action {:action :halo/handle
                    :halo/handle :ask
                    :halo/subject subject-id}}]
         (mapcat
          (fn [master]
            (let [master-id (:master-id master)
                  action (fn [handle]
                           {:action :halo/handle
                            :halo/handle handle
                            :halo/subject subject-id
                            :halo/master-id master-id})]
              [{:text (halo-master-label master)}
               {:text "  enter · navigation"
                :action (action :enter)}
               {:text (str "  preview · " preview-effect)
                :action (action :preview)}
               {:text (str "  say · " say-effect)
                :action (action :say)}]))
          masters))
        with-status
        (cond-> (vec base)
          (seq status) (conj {:text (str "status · " status)}))]
    (into
     with-status
     (map (fn [question]
            {:text (str "? " question)}))
     questions)))

(defn- halo-claim
  [subject-id]
  (when-let [block (get-in @!world [:blocks subject-id])]
    {:claim/subject subject-id
     :claim/site (if (:machine? block)
                   :block/machine-hit-area
                   :block/user-hit-area)
     :claim/facets binding-material/block-claim-facets
     :claim/args {}}))

(defn- render-halo!
  []
  (if-let [{:keys [x y subject-id] :as halo} @!halo]
    (let [{:keys [font-size char-advance line-h]} (metrics)
          rows (halo-rows halo)
          pad 10.0
          max-len (reduce max 1 (map (comp count :text) rows))
          w (+ (* 2 pad) (* max-len char-advance))
          h (+ (* 2 pad) (* (count rows) line-h))
          claim (halo-claim subject-id)
          ops
          (mapv (fn [i {:keys [text action]}]
                  (text-op text i line-h font-size
                           (if action amber fg)
                           pad))
                (range)
                rows)
          handles
          (->> rows
               (map-indexed vector)
               (keep
                (fn [[i {:keys [action]}]]
                  (when action
                    (rt-node
                     (keyword (str "halo-handle-" i))
                     :hit-area
                     {:x 0 :y (* i line-h) :w w :h line-h}
                     :data
                     (cond-> {:address subject-id
                              :actions action}
                       claim (assoc :material/claim claim))))))
               vec)
          tree
          (rt/resolve-layout
           (rt-node
            halo-vi :error-card
            {:x 0 :y 0 :w w :h h}
            :style {:bg [0.07 0.08 0.10 0.97]
                    :border-width 1.0
                    :border-color [0.92 0.75 0.35 0.9]
                    :radius 5}
            :text ops
            :data {:address subject-id}
            :children handles))]
      (if-let [slot (ss/slot (scene-rt/store-snapshot) halo-vi)]
        (do
          (swap! scene-rt/!scene-store ss/upsert-slot halo-vi
                 {:tree tree :container (:container slot)
                  :meta (:meta slot) :stratum (:stratum slot)
                  :pre-resolved? true})
          (scene-rt/set-transform! (:container slot) {:x x :y y}))
        (scene-rt/register-face-instance!
         halo-vi tree
         {:x x :y y :scale 1.0 :layer 7
          :meta {:ground-halo? true :subject subject-id}
          :pre-resolved? true})))
    (scene-rt/close-instance! halo-vi)))

(defn- open-halo!
  [subject world]
  (let [[wx wy] world
        {:keys [zoom]} @!camera
        offset (/ 10.0 zoom)]
    (reset! !halo
            (merge
             (halo-master-census subject)
             {:x (+ wx offset)
              :y (+ wy offset)
              :questions []
              :status nil}))
    (render-halo!)))

(defn- halo-action!
  [descriptor _ctx]
  (let [result (face-wiring/invoke-halo-handle! descriptor)]
    (-> (js/Promise.resolve result)
        (.then
         (fn [value]
           (if (= :ask (:halo/handle descriptor))
             (let [questions
                   (if (js/Array.isArray value)
                     (->> (array-seq value)
                          (keep #(aget % "question/ask"))
                          vec)
                     [])]
               (swap! !halo assoc
                      :questions questions
                      :status (str (count questions) " questions")))
             (swap! !halo assoc
                    :status
                    (if (nil? value)
                      "cancelled"
                      (str (name (:halo/handle descriptor)) " complete"))))
           (render-halo!)))
        (.catch
         (fn [error]
           (swap! !halo assoc :status (str "error · " (.-message error)))
           (render-halo!)))))
  true)

(scene-rt/register-action! :halo/handle halo-action!)

(defn- composition-lint-nodes
  [conflicts w h line-h font-size]
  (mapv
   (fn [i conflict]
     (rt-node (keyword (str "ground-material-conflict-" i))
              :error-card
              {:x 0
               :y (+ h (* i line-h))
               :w w
               :h line-h}
              :style {:bg [0.24 0.07 0.08 0.98]
                      :border-width 1.0
                      :border-color [0.9 0.3 0.3 1.0]}
              :text [(text-op
                      (str "material conflict · "
                           (name (:type conflict)))
                      0 line-h font-size err-col 0)]
              :data conflict))
   (range)
   conflicts))

(defn- wrap-lines
  "Display-only greedy wrap at `col` chars (monospace: char count IS width —
   see metrics). The raw text is never touched; machine blocks only. A word
   longer than col hard-breaks."
  [lines col]
  (vec (mapcat
        (fn [l]
          (if (<= (count l) col)
            [l]
            (loop [s l, out []]
              (if (<= (count s) col)
                (conj out s)
                (let [head (subs s 0 (inc col))
                      i    (str/last-index-of head " ")
                      cut  (if (and i (pos? i)) i col)]
                  (recur (str/triml (subs s cut))
                         (conj out (subs s 0 cut))))))))
        lines)))

(defn- lines-offset
  "Visual {:line :col} over rendered lines → flat offset into their
   newline-join, clamped."
  [lines {:keys [line col]}]
  (let [n    (count lines)
        line (max 0 (min (long (or line 0)) (dec (max 1 n))))
        col  (max 0 (min (long (or col 0)) (count (nth lines line ""))))]
    (+ (reduce + 0 (map #(inc (count %)) (take line lines))) col)))

(defn- reply-source-columns
  [src-txt]
  (when src-txt
    (reduce max 0 (map count (str/split src-txt #"\n" -1)))))

(defn- reply-wrap-col
  "A machine reply wraps at ITS SOURCE block's width (Sid): the source's
   longest line is the wrap column, bounded by the worn text-body floor; the
   worn fallback applies when no source resolves (e.g. pre-turn-record
  history). ONE rule shared by the settled render and provisional stream."
  [text-body-wear src-txt]
  (if-some [source-columns (reply-source-columns src-txt)]
    (max (:text-body/wrap-floor-columns text-body-wear)
         source-columns)
    (:text-body/wrap-fallback-columns text-body-wear)))

(defn- block-tree
  "One block's container-LOCAL resolved tree (root at 0,0; the container
   transform places it in the world). No wrap, no clip — width grows with
   the longest line (Law/WALKTHROUGH 14). Root carries [:data :address] so
   picks resolve to the unit (T7). At rest the land reads as material: the
   interaction box shows only on attention (Law 10); machine provenance is a
   quiet persistent edge tint (Law 6) — two separate primitives."
  [unit-id {:keys [text caret focused? refusal selection]} machine? hover? notice
   {:keys [font-size char-advance line-h]} wrap-col headers msel boundary?
   gsel? placement-derived? wears gold-marks silver-marks]
  (let [provenance-wear (:provenance wears)
        attention-wear (:attention wears)
        foldable-wear (:foldable wears)
        positioned-wear (:positioned wears)
        threaded-wear (:threaded wears)
        text-body-wear (:text-body wears)
        tint    (:provenance/tint provenance-wear)
        pad     (:attention/hit-padding attention-wear)
        provenance-stamp
        #(facet-material/contribution-stamp
          provenance-wear unit-id %1 %2 %3)
        attention-stamp
        #(facet-material/contribution-stamp
          attention-wear unit-id %1 %2 %3)
        foldable-stamp
        #(facet-material/contribution-stamp
          foldable-wear unit-id %1 %2 %3)
        positioned-stamp
        #(facet-material/contribution-stamp
          positioned-wear unit-id %1 %2 %3)
        threaded-stamp
        #(facet-material/contribution-stamp
          threaded-wear unit-id %1 %2 %3)
        text-body-stamp
        #(facet-material/contribution-stamp
          text-body-wear unit-id %1 %2 %3)
        lines   (cond-> (str/split (or text "") #"\n" -1)
                  (and machine? wrap-col) (wrap-lines wrap-col))
        nh      (count headers)
        lines   (if (pos? nh) (into (vec headers) lines) lines)
        n       (count lines)
        max-len (reduce max 1 (map count lines))
        w       (+ (* max-len char-advance) (* 2 pad))
        h       (+ (* n line-h) (* 2 pad))
        ops     (vec (map-indexed
                      (fn [i l]
                        (let [header? (< i nh)
                              body? (and machine? (not header?))
                              p-stamp
                              (when header?
                                (provenance-stamp
                                 :fold-header :text-color :block/content))
                              f-stamp
                              (when header?
                                (foldable-stamp
                                 (if (zero? i)
                                   :noise-header
                                   :prose-header)
                                 :header-copy
                                 :block/fold-header-text))
                              body-stamp
                              (when body?
                                (text-body-stamp
                                 :wrapped-body
                                 :wrap-policy
                                 :block/content-flow))]
                          (cond->
                              (text-op l i line-h font-size
                                       (cond
                                         header? tint
                                         machine? dim
                                         :else fg)
                                       0)
                            header? (merge p-stamp)
                            body? (merge body-stamp)
                            header?
                            (assoc :material/contributions
                                   [p-stamp f-stamp]))))
                      lines))
        caret-lc (when (and focused? caret)
                   (ge/caret->line-col text caret))
        ;; Task 11: the selection wash — one translucent rect per selected
        ;; line span (glyphs stay readable through the alpha). msel is the
        ;; machine-block selection in visual {:line :col} space.
        msel-range (when msel
                     (let [a (lines-offset lines (:anchor msel))
                           h (lines-offset lines (:head msel))]
                       (when (not= a h) [(min a h) (max a h)])))
        sel-nodes (when-let [[sel-s sel-e] (or (and focused? selection)
                                               msel-range)]
                    (loop [i 0, start 0, out []]
                      (if (>= i n)
                        out
                        (let [l    (nth lines i)
                              lend (+ start (count l))
                              s'   (max sel-s start)
                              e'   (min sel-e lend)]
                          (recur (inc i) (inc lend)
                                 (if (< s' e')
                                   (conj out
                                         (rt-node (keyword (str "ground-sel-" i))
                                                  :rect
                                                  {:x (* (- s' start) char-advance)
                                                   :y (* i line-h)
                                                   :w (max 2.0 (* (- e' s') char-advance))
                                                   :h line-h}
                                                  :style {:bg [0.35 0.5 0.8 0.3]}))
                                   out))))))
        decoration-composition
        (facet-material/compose
         [(when machine?
            {:wear provenance-wear
             :stamp
             (provenance-stamp
              :machine-rail :provenance-marker :block/decorations)
             :value
             (rt-node :ground-mark :rect
                      {:x (- pad) :y (- pad) :w 2.5 :h h}
                      :style {:bg tint}
                      :data
                      (provenance-stamp
                       :machine-rail
                       :provenance-marker
                       :block/decorations))})
          (when (or focused? hover?)
            {:wear attention-wear
             :stamp
             (attention-stamp
              :attention-box :attention-border :block/decorations)
             :value
             (rt-node :ground-box :rect
                      {:x (- pad) :y (- pad) :w w :h h}
                      :style
                      {:border-width
                       (:attention/border-width attention-wear)
                       :border-color
                       (:attention/border-color attention-wear)
                       :bg (:attention/background attention-wear)}
                      :data
                      (attention-stamp
                       :attention-box
                       :attention-border
                       :block/decorations))})])
        decoration-nodes
        (mapv :value (:contributions decoration-composition))
        conflict-nodes
        (composition-lint-nodes
         (:conflicts decoration-composition) w h line-h font-size)
        ;; P5 family 1 — one HIT-ONLY node per fold header row. It carries no
        ;; :bg, no :text and no :shadow, so tree->rects/tree->text-ops/
        ;; tree->shadows all emit nothing for it: the containment path gets a
        ;; finer grain at exactly zero pixels. Its band is the full block width
        ;; over its own line, which is precisely the region the deleted
        ;; `(<= row 1)` arithmetic used to select. Appended LAST so hit-test's
        ;; reverse child walk reaches it before the attention box.
        fold-header-nodes
        (when (pos? nh)
          (mapv
           (fn [i {:keys [section fold-key]}]
             (rt-node (keyword (str "ground-fold-header-hit-" (name section)))
                      :hit-area
                      {:x 0 :y (* i line-h) :w w :h line-h}
                      :data
                      (merge
                       (foldable-stamp
                        :fold-header-hit :fold-toggle :block/hit-area)
                       {:address unit-id
                        :material/claim
                        {:claim/subject unit-id
                         :claim/site :block/fold-header
                         :claim/facets [:foldable]
                         :claim/args {:section section :fold-key fold-key}}})))
           (range)
           (take nh fold-sections)))
        extra   (cond-> (vec sel-nodes)
                  ;; Task 18: group-selection member mark (marquee wash)
                  gsel?
                  (conj (rt-node :ground-gsel :rect
                                 {:x (- pad) :y (- pad) :w w :h h}
                                 :style {:border-width 1.5
                                         :border-color [0.55 0.65 0.9 0.8]
                                         :bg [0.35 0.5 0.8 0.10]}))

                  (seq decoration-nodes)
                  (into decoration-nodes)

                  (and focused? caret-lc)
                  (conj (rt-node :ground-caret :rect
                                 {:x (* (:col caret-lc) char-advance)
                                  :y (* (:line caret-lc) line-h)
                                  :w 2 :h line-h}
                                 :style {:bg [0.95 0.95 0.95 1.0]}))
                  (some? refusal)
                  (conj (rt-node :ground-refusal :text-run
                                 {:x 0 :y (* n line-h) :w w :h line-h}
                                 :text [(text-op (str "⟂ edit refused: "
                                                      (if (keyword? refusal)
                                                        (name refusal) (str refusal)))
                                                 0 line-h font-size err-col 0)]))
                  (some? notice)
                  (conj (rt-node :ground-notice :text-run
                                 {:x 0 :y (* (+ n (if refusal 1 0)) line-h)
                                  :w w :h line-h}
                                 :text [(text-op (str notice) 0 line-h font-size
                                                 amber 0)]))
                  ;; D-core boundary (Sid): a fresh CLI session opened here —
                  ;; a quiet line above the episode's first served block
                  boundary?
                  (conj (rt-node :ground-episode-boundary :text-run
                                 {:x 0 :y (- (* 1.6 line-h)) :w w :h line-h}
                                 :text [(text-op "— fresh session —" 0 line-h
                                                 font-size tint 0)]
                                 :data
                                 (provenance-stamp
                                  :episode-boundary
                                  :boundary-label
                                  :block/prelude)))

                  (seq conflict-nodes)
                  (into conflict-nodes)

                  ;; editable-material P4 / Halo P1 gold projection: durable
                  ;; source units remain their own OC blocks; this target-side
                  ;; line is only a projection coupled by :references.
                  (seq gold-marks)
                  (conj (rt-node :ground-wish-mark :text-run
                                 {:x 0 :y (- (* (if boundary? 2.8 1.4) line-h))
                                  :w (max w (* 42 char-advance))
                                  :h line-h}
                                 :text
                                 [(text-op
                                   (let [{:keys [text count]} (first gold-marks)]
                                     (str "⌁ " text
                                          (when (> count 1)
                                            (str "  +" (dec count)))))
                                   0 line-h font-size amber 0)]))

                  ;; Silver stays visibly machine and explicitly tentative.
                  ;; Like gold, this is a target-side projection; the source
                  ;; record remains separately addressable.
                  (seq silver-marks)
                  (conj
                   (rt-node
                    :ground-silver-mark :text-run
                    {:x 0
                     :y (- (* (+ (if boundary? 1.4 0.0)
                                 (if (seq gold-marks) 1.4 0.0)
                                 1.4)
                              line-h))
                     :w (max w (* 48 char-advance))
                     :h line-h}
                    :text
                    [(text-op
                      (let [{:keys [text count]} (first silver-marks)]
                        (str "≈ machine guess · " text
                             (when (> count 1)
                               (str "  +" (dec count)))))
                      0 line-h font-size dim 0)]))

                  (seq fold-header-nodes)
                  (into fold-header-nodes))]
    (rt/resolve-layout
     (rt-node :ground-block :text-run
              {:x 0 :y 0 :w (max w char-advance) :h (max h line-h)}
              :text ops
              :data
              (cond->
                  (merge
                   {:address unit-id}
                   (attention-stamp
                    :hit-box :hit-target :block/hit-area)
                   ;; P5 families 2+3 — the block-grain claim. The SITE is
                   ;; decided here, at build time, from what the block IS, so
                   ;; the dispatch function needs no machine?/user? branch:
                   ;; attention's rows say what landing attention here means,
                   ;; positioned's say what dragging it means.
                   {:material/claim
                    {:claim/subject unit-id
                     :claim/site (if machine?
                                   :block/machine-hit-area
                                   :block/user-hit-area)
                     :claim/facets [:attention :positioned]
                     :claim/args {}}})
                placement-derived?
                (assoc
                 :material/positioned
                 (positioned-stamp
                  :derived-placement
                  :placement-default
                  :block/placement))
                (not machine?)
                (assoc
                 :material/threaded
                 (threaded-stamp
                  :send-adoption
                  :column-adoption-policy
                  :block/thread-adoption)))
              :children extra))))

;; ===========================================================================
;; Slot lifecycle
;; ===========================================================================

(defn- block-vi [unit-id] [:vi :ground-block unit-id])

(defn- upsert-block-slot! [unit-id tree x y]
  (if-let [slot (ss/slot (scene-rt/store-snapshot) (block-vi unit-id))]
    (swap! scene-rt/!scene-store ss/upsert-slot (block-vi unit-id)
           {:tree tree :container (:container slot)
            :meta (:meta slot) :stratum (:stratum slot)
            :pre-resolved? true})
    (let [{:keys [container]}
          (scene-rt/register-face-instance!
           (block-vi unit-id) tree
           {:x x :y y :scale 1.0 :layer 2
            :meta {:ground-block unit-id}
            :pre-resolved? true})]
      (swap! !world assoc-in [:blocks unit-id :cid] container)))
  nil)

(def ^:private material-error-vi :ground-material-error)

(defn- drill-master-id
  []
  (or (some-> (second
               (re-find #"[?&]material-master=([^&]+)"
                        (str (.-search js/window.location))))
              js/decodeURIComponent)
      (first facet-masters/master-ids)))

(defn- material-render-state
  [served]
  (into (sorted-map)
        (map
         (fn [[master-id facet]]
           [master-id
            (select-keys
             facet
             [:facet-master/active-revision-id
              :facet-master/material
              :facet-master/candidate-revision-id
              :facet-master/candidate-errors])]))
        (:facet-materials/by-id served)))

(defn- refresh-material-error!
  "Malformed candidates land beside the worn surface as one generic card.
   It reports candidate truth only; active material remains the sole input to
   every block render."
  []
  (let [!served (get-in @!refs [:atoms :!facet-materials])
        served (when !served @!served)
        failures
        (->> (:facet-materials/by-id served)
             (keep
              (fn [[master-id facet]]
                (when (seq (:facet-master/candidate-errors facet))
                  {:master-id master-id
                   :revision-id
                   (:facet-master/candidate-revision-id facet)
                   :errors (:facet-master/candidate-errors facet)})))
             (sort-by :master-id)
             vec)]
    (if (and (drill-conversation-id) (seq failures))
      (let [{:keys [font-size char-advance line-h]} (metrics)
            lines
            (vec
             (mapcat
              (fn [{:keys [master-id revision-id errors]}]
                (into
                 [(str master-id " rejected · " revision-id)]
                 (map
                  #(pr-str
                    (select-keys % [:type :message :actual]))
                  (take 4 errors))))
              failures))
            pad 10.0
            max-len (reduce max 1 (map count lines))
            w (+ (* 2 pad) (* max-len char-advance))
            h (+ (* 2 pad) (* (count lines) line-h))
            ops (mapv (fn [i line]
                        (text-op line i line-h font-size err-col pad))
                      (range)
                      lines)
            [x y] (screen->world 18.0 18.0)
            tree (rt/resolve-layout
                  (rt-node material-error-vi :error-card
                           {:x 0 :y 0 :w w :h h}
                           :style {:bg [0.24 0.07 0.08 0.98]
                                   :border-width 1.0
                                   :border-color [0.9 0.3 0.3 1.0]
                                   :radius 4}
                           :text ops
                           :data
                           {:material/failures failures}))]
        (if-let [slot (ss/slot (scene-rt/store-snapshot) material-error-vi)]
          (do
            (swap! scene-rt/!scene-store ss/upsert-slot material-error-vi
                   {:tree tree :container (:container slot)
                    :meta (:meta slot) :stratum (:stratum slot)
                    :pre-resolved? true})
            (scene-rt/set-transform! (:container slot) {:x x :y y}))
          (scene-rt/register-face-instance!
           material-error-vi tree
           {:x x :y y :scale 1.0 :layer 6
            :meta {:ground-material-error? true}
            :pre-resolved? true})))
      (scene-rt/close-instance! material-error-vi))))

(defn- rebuild-material-sites!
  []
  (doseq [unit-id (keys (:blocks @!world))]
    (rebuild-block! unit-id)))

(defn- run-material-drill!
  []
  (when-let [drill-id (drill-conversation-id)]
    (-> (js/fetch "/api/material/facet-master/drill"
                  (clj->js {:method "POST"
                            :headers {"Content-Type" "application/edn"}
                            :body (pr-str
                                   {:drill-id drill-id
                                    :master-id (drill-master-id)})}))
        (.then (fn [resp]
                 (.then (.text resp)
                        (fn [body]
                          (if (.-ok resp)
                            (js/console.log
                             "[MATERIAL] malformed-candidate drill retained"
                             body)
                            (js/console.error
                             "[MATERIAL] malformed-candidate drill failed"
                             (.-status resp) body))))))
        (.catch (fn [e]
                  (js/console.error
                   "[MATERIAL] malformed-candidate drill unavailable" e))))))

(defn- truth-text
  "Materialized truth for a block: the narrowing overlay when present, else
   the served context text (via the per-context index — reconcile calls this
   for EVERY block on every context emission, i.e. per keystroke, so a turn
   scan here is O(blocks²)). NEVER a queue value."
  [unit-id]
  (or (get @bew/!truth-overlay unit-id)
      (:text (get (:block-index @!world) unit-id))))

(defn- context-block-entry
  "The served (post-merge) block map for a unit-id, or nil."
  [unit-id]
  (get (:block-index @!world) unit-id))

(defn- mark-preview
  "Bounded source-material preview for a target-side experience mark."
  [unit-id]
  (when-let [t (truth-text unit-id)]
    (let [s (str t)]
      (subs s 0 (min 96 (count s))))))

(defn- sec-line-count [s]
  (if (str/blank? (or s ""))
    0
    (loop [i 0 n 1]
      (let [j (.indexOf s "\n" i)]
        (if (neg? j) n (recur (inc j) (inc n)))))))

(defn- fold-header-line
  [foldable-wear shown? section text]
  (let [copy (:foldable/header-copy foldable-wear)
        label (get copy
                   (case section
                     :noise :noise-label
                     :prose :prose-label))]
    (if shown?
      (str (:expanded-marker copy) label (:hide-suffix copy))
      (str (:collapsed-marker copy) label (:show-suffix copy)
           (let [c (sec-line-count text)]
             (when (pos? c)
               (str (:line-count-prefix copy)
                    c
                    (:line-count-suffix copy))))))))

(defn- run-view
  "A run block's fold-derived render inputs {:display :headers}; nil for
   plain machine history (no run record). ONE assembly shared by the
   rebuild path and the machine-selection copy path."
  [unit-id foldable-wear]
  (let [cb  (context-block-entry unit-id)
        run (when (contains? cb :reply-text) cb)
        folds (get @!folds unit-id (:foldable/defaults foldable-wear))
        {:keys [noise? prose?]} folds]
    (when run
      {:display (cond
                  ;; both on → the full raw :text, served interleaved
                  ;; order (splitting the folds would lose the weave)
                  (and noise? prose?) (:text run)
                  noise?  (:noise-text run)
                  ;; a run with no prose (interrupted mid-work) still
                  ;; peeks its first raw lines — never an empty block
                  prose?  (let [r (:reply-text run)]
                            (if (str/blank? (or r ""))
                              (str/join "\n" (take 3 (str/split (or (:text run) "")
                                                                #"\n" -1)))
                              r))
                  :else   "")
       ;; the header LINES stay exactly the vector every consumer already
       ;; expects; `fold-sections` supplies the order, the copy selector, and
       ;; the toggle key, so nothing here names a row index
       :headers (mapv
                 (fn [{:keys [section fold-key text-key]}]
                   (fold-header-line
                    foldable-wear (get folds fold-key) section
                    (get run text-key)))
                 fold-sections)})))

(defn- machine-visual-lines
  "The RENDERED lines of a machine block (headers + folded display + wrap) —
   the visual text a machine selection lives over (copy what you see)."
  [uid]
  (when-let [b (get-in @!world [:blocks uid])]
    (when (:machine? b)
      ;; P6: a block wears ITS OWN foldable revision when it has one
      (let [foldable-wear (:foldable (wears-for uid))
            {:keys [display headers] :as rv} (run-view uid foldable-wear)
            text  (if rv display (or (truth-text uid) ""))
            lines (cond-> (str/split (or text "") #"\n" -1)
                    (:wrap-col b) (wrap-lines (:wrap-col b)))]
        (if rv (into (vec headers) lines) (vec lines))))))

(defn- machine-sel-text
  "The machine selection's visual substring (wrap breaks copy as newlines)."
  []
  (when-let [{:keys [uid anchor head]} @!machine-sel]
    (when-let [lines (machine-visual-lines uid)]
      (let [a (lines-offset lines anchor)
            h (lines-offset lines head)
            sel-s (min a h) sel-e (max a h)]
        (when (< sel-s sel-e)
          (subs (str/join "\n" lines) sel-s sel-e))))))

(defn- clear-machine-sel! []
  (when-let [{:keys [uid]} @!machine-sel]
    (reset! !machine-sel nil)
    (rebuild-block! uid)))

(defn rebuild-block!
  "Rebuild ONE block slot from the current edit state + truth (the
   keystroke-echo hot path — one small tree, same-frame paint). A merged
   run block (marked by :reply-text) folds into two sections, each behind
   its own material-supplied header-line toggle (Tasks 6+7, Sid).
   Task 5: the build SKIPS when the render inputs equal the last built
   signature — reconcile calls this for every block on every context
   emission (i.e. per keystroke), and layout+upsert over unchanged blocks
   was the typing lag."
  [unit-id]
  (when-let [b (get-in @!world [:blocks unit-id])]
    (let [st    @!ground-edit
          ;; P6 · R2: the per-subject tier. Non-deviant blocks get the shared
          ;; wears object back BY IDENTITY, so the per-keystroke signature
          ;; compare still short-circuits (T5).
          wears (wears-for unit-id)
          ;; fold-derived display + headers come from run-view (ONE assembly
          ;; shared with the machine-selection copy path)
          {:keys [display headers]}
          (when (:machine? b) (run-view unit-id (:foldable wears)))
          ;; typing-lag patch: :optimistic paints the queue HEAD, so a
          ;; keystroke lands this frame instead of after the durable ack
          ;; (ge's ns note). Flip this keyword to :confirmed to restore the
          ;; committed-echo law exactly.
          view (ge/block-view st unit-id (truth-text unit-id) :optimistic)
          view (if display (assoc view :text display) view)
          n      @!notice
          notice (when (= unit-id (:unit-id n)) (:text n))
          hover? (= unit-id @!hover)
          msel   (let [ms @!machine-sel] (when (and ms (= unit-id (:uid ms))) ms))
          bnd?   (boolean (:episode-boundary? (context-block-entry unit-id)))
          gsel?  (contains? @!group-sel unit-id)
          gold-specs
          (get-in @!world
                  [:context :conversation/experience
                   :experience/gold-marks-by-target unit-id])
          gold-marks
          (when (seq gold-specs)
            (let [texts
                  (->> gold-specs
                       (keep
                        (fn [mark]
                          (when-let [text
                                     (mark-preview
                                      (:source-unit-id mark))]
                            (str (if (= :halo/say (:mark/type mark))
                                   "say"
                                   "wish")
                                 " · " text))))
                       vec)]
              (when (seq texts)
                [{:text (str/join " | " texts) :count 1}])))
          silver-specs
          (get-in @!world
                  [:context :conversation/experience
                   :experience/silver-marks-by-target unit-id])
          silver-marks
          (when (seq silver-specs)
            (let [texts (->> silver-specs
                             (keep (comp mark-preview :record-unit-id))
                             vec)]
              (when (seq texts)
                [{:text (first texts) :count (count texts)}])))
          m      (metrics)
          ;; everything block-tree consumes (viewport excluded — unused):
          ;; equal sig ⇒ identical pixels ⇒ the build is pure waste
          sig    [view (:machine? b) hover? notice (:wrap-col b) headers msel bnd?
                  gsel? (:placement-derived? b) wears gold-marks silver-marks
                  (:font-size m) (:char-advance m) (:line-h m)]]
      (if (and (= sig (:render-sig b))
               (some? (ss/slot (scene-rt/store-snapshot) (block-vi unit-id))))
        (swap! !rebuild-stats update :skips inc)
        (let [tree (block-tree unit-id view (:machine? b) hover? notice
                               m (:wrap-col b) headers msel bnd? gsel?
                               (:placement-derived? b) wears
                               gold-marks silver-marks)]
          (swap! !rebuild-stats update :builds inc)
          (upsert-block-slot! unit-id tree (:x b) (:y b))
          (swap! !world update-in [:blocks unit-id]
                 assoc :w (get-in tree [:bounds :w]) :h (get-in tree [:bounds :h])
                 :render-sig sig))))))

(defn- clear-group-sel! []
  (let [uids @!group-sel]
    (when (seq uids)
      (reset! !group-sel #{})
      (doseq [uid uids] (rebuild-block! uid)))))

;; ===========================================================================
;; Anchor + provisional slots
;; ===========================================================================

(defn- refresh-anchor! []
  (let [st @!ground-edit
        {:keys [line-h]} (metrics)]
    (if-let [a (:anchor st)]
      (let [tree (rt/resolve-layout
                  (rt-node :ground-anchor :rect {:x 0 :y 0 :w 2 :h line-h}
                           :style {:bg [0.95 0.95 0.95 0.9]}))]
        (if-let [slot (ss/slot (scene-rt/store-snapshot) :ground-anchor)]
          (do (swap! scene-rt/!scene-store ss/upsert-slot :ground-anchor
                     {:tree tree :container (:container slot)
                      :meta (:meta slot) :stratum (:stratum slot)
                      :pre-resolved? true})
              (scene-rt/set-transform! (:container slot) {:x (:x a) :y (:y a)}))
          (scene-rt/register-face-instance! :ground-anchor tree
                                            {:x (:x a) :y (:y a) :scale 1.0
                                             :layer 4 :meta {:ground-anchor? true}
                                             :pre-resolved? true})))
      (scene-rt/close-instance! :ground-anchor))))

(defn- provisional-vi [thread-key]
  [:vi :ground-provisional (or thread-key "genesis")])

(defn- render-provisional-slot!
  "ONE run's PROVISIONAL projection (Law 8): one current-activity line + the
   arriving stream, dimmed — process-state, never truth, never camera motion.
   Placed beneath ITS run's source block; REPLACED at distill by the durable
   provenance-marked reply. The stream wraps at the source block's width —
   the same rule as the distilled reply, so the projection never runs wider
   than the truth that replaces it (Task 8)."
  [thread-key {:keys [phase activity stream-text error source-unit-id
                      wrap-source-columns]}]
  (let [{:keys [font-size line-h char-advance]} (metrics)
        wears (current-material-wears)
        positioned-wear (:positioned wears)
        text-body-wear (:text-body wears)
        fallback (:positioned/fallback-position positioned-wear)
        open?  (contains? #{:streaming :distilling} phase)
        vi     (provisional-vi thread-key)
        src    (get-in @!world [:blocks source-unit-id])
        sx     (if src (:x src) (:x fallback))
        sy     (if src
                 (+ (:y src)
                    (or (:h src) line-h)
                    (:positioned/reply-gap positioned-wear))
                 (:y fallback))
        wcol   (if-some [source-columns wrap-source-columns]
                 (max (:text-body/wrap-floor-columns text-body-wear)
                      source-columns)
                 (reply-wrap-col
                  text-body-wear
                  (truth-text source-unit-id)))
        slines (when (seq (str stream-text))
                 (wrap-lines (str/split-lines (str stream-text)) wcol))
        kids   (cond-> []
                 open?
                 (conj (rt-node :ground-activity :text-run
                                {:x 0 :y 0 :w 600 :h line-h}
                                :text [(text-op (str "· " (or activity "the resident is working"))
                                                0 line-h font-size dim 0)]))
                 (seq slines)
                 (conj (rt-node :ground-stream :text-run
                                {:x 0 :y line-h
                                 :w (+ (* (reduce max 1 (map count slines))
                                          char-advance) 16)
                                 :h (* (count slines) line-h)}
                                :text (vec (map-indexed
                                            (fn [i l] (text-op l i line-h font-size dim 0))
                                            slines))))
                 (some? error)
                 (conj (rt-node :ground-turn-error :text-run
                                {:x 0 :y 0 :w 600 :h line-h}
                                :text [(text-op (str "⟂ " error " — retry is a new turn")
                                                0 line-h font-size err-col 0)])))
        tree (rt/resolve-layout
              (rt-node :ground-provisional :stack
                       {:x 0 :y 0 :w 600 :h 0}
                       :layout {:direction :column :gap 6 :auto-height? true}
                       :data
                       (let [subject
                             (or source-unit-id
                                 (str "provisional:"
                                      (or thread-key "genesis")))]
                         (assoc
                          (positioned-material/contribution-stamp
                           positioned-wear
                           subject
                           :provisional-placement
                           :reply-placement
                           :appearance/placement)
                          :material/text-body
                          (text-body-material/contribution-stamp
                           text-body-wear
                           subject
                           :provisional-body
                           :wrap-policy
                           :appearance/content-flow)))
                       :children kids))]
    (if-let [slot (ss/slot (scene-rt/store-snapshot) vi)]
      (do (swap! scene-rt/!scene-store ss/upsert-slot vi
                 {:tree tree :container (:container slot)
                  :meta (:meta slot) :stratum (:stratum slot)
                  :pre-resolved? true})
          (scene-rt/set-transform! (:container slot) {:x sx :y sy}))
      (scene-rt/register-face-instance! vi tree
                                        {:x sx :y sy :scale 1.0 :layer 6
                                         :meta {:ground-provisional? true}
                                         :pre-resolved? true}))))

(defn- refresh-provisional*!
  "Render every live run's provisional slot; close slots whose runs went
   quiet (their distilled truth replaces them). One slot per thread — three
   concurrent runs = three activity lines, each under its own block."
  []
  (let [runs @!ground-runs
        want (into #{} (keep (fn [[k {:keys [phase error]}]]
                               (when (or (contains? #{:streaming :distilling} phase)
                                         (some? error))
                                 k)))
                   runs)]
    (doseq [k @!provisional-open]
      (when-not (contains? want k)
        (scene-rt/close-instance! (provisional-vi k))))
    (reset! !provisional-open want)
    (doseq [k want]
      (render-provisional-slot! k (get runs k)))))

(defonce ^:private !prov-frame (atom false))

(defn- refresh-provisional!
  "Coalesce provisional renders to ONE per animation frame (Task 5): the
   stream fires run-event! per text delta, and every render re-wraps the
   WHOLE accumulated stream — per-delta that is quadratic over a long
   reply, and it competes with typing on the main thread. Paint rate is
   the ceiling; rendering faster than the frame is invisible."
  []
  (when (compare-and-set! !prov-frame false true)
    (js/requestAnimationFrame
     (fn [_] (reset! !prov-frame false) (refresh-provisional*!)))))

;; ===========================================================================
;; The settle driver — camera + positions as SETTLE-STATE truth
;; ===========================================================================

(defn- fire-settle! [& {:keys [keepalive?]}]
  (let [{:keys [dirty camera?]} @!settle]
    (when (or (seq dirty) camera?)
      (let [cells  (mapv (fn [[uid p]] {:unit-id uid :x (:x p) :y (:y p)}) dirty)
            cam    @!camera
            body   (pr-str (cond-> {:cells cells
                                    :camera (when camera? cam)
                                    :settle-id (str (random-uuid))
                                    :time-ms (js/Date.now)}
                             (drill-conversation-id)
                             (assoc :conversation-id (drill-conversation-id))))]
        (swap! !settle assoc :dirty {} :camera? false)
        (-> (js/fetch "/api/episode/geometry"
                      (clj->js (cond-> {:method "POST"
                                        :headers {"Content-Type" "application/edn"}
                                        :body body}
                                 keepalive? (assoc :keepalive true))))
            (.then (fn [resp]
                     (if (.-ok resp)
                       (swap! !settle update :acked
                              (fn [a] (reduce (fn [m c] (assoc m (:unit-id c)
                                                               {:x (:x c) :y (:y c)}))
                                              a cells)))
                       ;; refused settle → VISIBLE revert to the last
                       ;; acknowledged positions (G4b forced-stale drill)
                       (do (doseq [c cells]
                             (when-let [acked (get-in @!settle [:acked (:unit-id c)])]
                               (swap! !world update-in [:blocks (:unit-id c)]
                                      assoc :x (:x acked) :y (:y acked))
                               (when-let [cid (get-in @!world [:blocks (:unit-id c) :cid])]
                                 (scene-rt/set-transform! cid acked)))
                             (reset! !notice {:unit-id (:unit-id c)
                                              :text "position write refused — reverted"})
                             (rebuild-block! (:unit-id c)))
                           (js/setTimeout (fn [] (reset! !notice nil)
                                            (doseq [c cells] (rebuild-block! (:unit-id c))))
                                          2500)))))
            (.catch (fn [_e]
                      ;; transport failure: keep dirty for the next arm — the
                      ;; acked state is the safety floor, never the exit
                      (swap! !settle (fn [s] (-> s
                                                 (update :dirty #(merge (into {} (map (fn [c] [(:unit-id c) {:x (:x c) :y (:y c)}]) cells)) %))
                                                 (assoc :camera? (boolean (or (:camera? s) camera?)))))))))))))

(defn- arm-settle!
  "Gesture-end ARMS the settle write; the debounce coalesces continuous
   bursts into ONE acked write (never per-event)."
  [kind & [unit-id pos]]
  (swap! !settle (fn [s]
                   (cond-> s
                     (= kind :cell)   (assoc-in [:dirty unit-id] pos)
                     (= kind :camera) (assoc :camera? true))))
  (when-let [t (:timer @!settle)] (js/clearTimeout t))
  (swap! !settle assoc :timer
         (js/setTimeout (fn [] (swap! !settle assoc :timer nil) (fire-settle!))
                        settle-debounce-ms)))

;; ===========================================================================
;; Reconcile — served truth → per-block slots (context/echo edge)
;; ===========================================================================

(defn- context-blocks
  "Reading-order blocks with :speaker attached (the served time merge)."
  [ctx]
  (vec (mapcat (fn [t] (map #(assoc % :speaker (:speaker t)) (:blocks t)))
               (:turns ctx))))

(defn- placement-anchor
  [blocks-acc prev-uid src-uid rule]
  (case rule
    :same-source-tail
    (let [prev (get blocks-acc prev-uid)]
      (when (and (:machine? prev)
                 (= (:src-uid prev) src-uid))
        prev))

    :source
    (get blocks-acc src-uid)

    :previous
    (get blocks-acc prev-uid)

    nil))

(defn- default-position
  "Derived default for a block with no settled cell and no live position:
   a machine block lands beneath its RUN's source block (§9.4 reply
   placement, left-aligned — parent-relative at birth, independent after);
   successive machine blocks of the SAME source stack beneath each other
   (all-anchored-on-source would pile them on one point) — but a machine
   block never chains under a DIFFERENT run's block (found live 2026-07-20:
   Task 3's reply stacked under Task 2's tail instead of under #TASK 3);
   anything else falls beneath the previous block in reading order.
   Ephemeral until a real gesture settles it — a derived default is not a
   settle-write."
  [positioned-wear blocks-acc prev-uid src-uid machine? {:keys [line-h]}]
  (let [order
        (get-in positioned-wear
                [:positioned/anchor-order
                 (if machine? :machine :ordinary)])
        anchor
        (some #(placement-anchor blocks-acc prev-uid src-uid %) order)
        fallback (:positioned/fallback-position positioned-wear)]
    (if anchor
      {:x (:x anchor)
       :y (+ (:y anchor)
             (or (:h anchor) line-h)
             (:positioned/reply-gap positioned-wear))}
      fallback)))

(defn reconcile!
  "Full reconcile of block slots against a served face-context (the context/
   echo edge — never the RAF edge). Live positions win over cells; cells win
   over derived defaults; return restores NO attention state (moment 6)."
  [ctx]
  (when (and @!refs ctx)
    ;; the context lands FIRST: rebuild-block! reads truth THROUGH it during
    ;; the placement loop (found live: end-of-fn assoc rendered every block
    ;; from the PREVIOUS pull — empty boxes on the first pull after boot).
    ;; :block-index rides along — the O(1) index behind truth-text /
    ;; context-block-entry (reconcile touches EVERY block per emission)
    (swap! !world assoc :context ctx
           :block-index (into {} (map (juxt :id identity)) (context-blocks ctx)))
    (let [m        (metrics)
          geometry (:conversation/geometry ctx)
          turn-recs (sort-by :time-ms (:conversation/turn-records ctx []))
          blocks   (context-blocks ctx)
          seen     (set (map :id blocks))
          wears (current-material-wears)
          positioned-wear (:positioned wears)
          text-body-wear (:text-body wears)
          ;; per-thread await windows (a settling reply matches ITS thread's
          ;; run, never another thread's — concurrent distills stay disjoint)
          awaits   (into {} (keep (fn [[k r]]
                                    (when-let [a (:await-reply r)] [k a])))
                         @!ground-runs)]
      ;; served turn records are the durable block→thread map. Pre-thread
      ;; records (nil thread) map to NOTHING (Sid 2026-07-21: everything
      ;; rides the new infra) — an old block's next send mints a fresh lane
      ;; like any block; the old genesis session goes dormant, its material
      ;; stays served
      (doseq [r turn-recs]
        (when-let [su (:source-unit-id r)]
          (when-let [tid (:thread-id r)]
            (swap! !thread-of assoc su tid))))
      ;; the per-thread prev-turn chain survives reload: served records seed
      ;; each thread's tail (turn-recs are time-sorted, so the reduce keeps
      ;; the newest per thread); a turn-id this session already holds
      ;; outranks the page — it is never older than what the serve returned
      (let [served-tail (reduce (fn [m r]
                                  (if-let [tid (:thread-id r)]
                                    (assoc m tid (:turn-id r))
                                    m))
                                {} turn-recs)]
        (swap! !last-turn-ids #(merge served-tail %)))
      ;; camera restore — ONCE, at boot (later logins resume the scene; the
      ;; live camera is the inhabitant's after that — Law 3)
      (when (and (not (:camera-restored? @!world)))
        (swap! !world assoc :camera-restored? true)
        (when-let [cam (:conversation/camera ctx)]
          (reset! !camera {:x (double (:x cam)) :y (double (:y cam))
                           :zoom (double (or (:zoom cam) 1.0))})))
      ;; place + upsert every served block
      (loop [bs blocks, placed {}, prev nil, new-replies []]
        (if-let [b (first bs)]
          (let [uid      (:id b)
                machine? (not= (str (:speaker b)) "sid")
                live     (get-in @!world [:blocks uid])
                cell     (get geometry uid)
                b-thread (:thread-id b)
                ;; the run this block answers: the latest turn record at or
                ;; before its time WITHIN ITS OWN THREAD names the source
                ;; block it ran from — ONE attribution shared by position
                ;; anchor and wrap width. Thread-scoped so interleaved
                ;; concurrent runs never claim each other's replies (nil ==
                ;; nil keeps pre-thread history byte-identical).
                src-uid  (when machine?
                           (:source-unit-id
                            (last (filter #(and (= (:thread-id %) b-thread)
                                                (<= (:time-ms % 0) (:time-ms b 0)))
                                          turn-recs))))
                pos      (cond
                           (and live
                                (:x live)
                                (not (:placement-derived? live)))
                           {:x (:x live) :y (:y live)}
                           cell {:x (double (:x cell)) :y (double (:y cell))}
                           :else
                           (default-position
                            positioned-wear
                            placed prev src-uid machine? m))
                derived? (and (nil? cell)
                              (or (nil? (and live (:x live)))
                                  (:placement-derived? live)))
                ;; a machine block wraps at ITS SOURCE block's width (Sid);
                ;; the worn text-body policy supplies the lower bound and the
                ;; no-source fallback.
                wrap-col (when machine?
                           (reply-wrap-col
                            text-body-wear
                            (when src-uid
                              (some #(when (= src-uid (:id %)) (:text %))
                                    blocks))))]
            (swap! !world update-in [:blocks uid]
                   (fn [e] (merge e {:machine? machine? :local? false
                                     :wrap-col wrap-col :source-uid src-uid
                                     :placement-derived? derived?}
                                  pos)))
            ;; transform only when the position MOVED — this loop runs per
            ;; keystroke; a store swap per unmoved block is pure waste
            (when (or (nil? live)
                      (not= [(:x live) (:y live)] [(:x pos) (:y pos)]))
              (when-let [cid (get-in @!world [:blocks uid :cid])]
                (scene-rt/set-transform! cid pos)))
            (rebuild-block! uid)
            (when cell
              (swap! !settle assoc-in [:acked uid] {:x (:x pos) :y (:y pos)}))
            (recur (rest bs)
                   (assoc placed uid (merge pos {:h (get-in @!world [:blocks uid :h])
                                                 :machine? machine?
                                                 :src-uid src-uid}))
                   uid
                   (cond-> new-replies
                     ;; a reply born by THIS session's distill: its computed
                     ;; birth position becomes durable (independent once
                     ;; born — moving the source later never moves it).
                     ;; Matched against ITS OWN thread's await window.
                     (and
                          (:positioned/persist-derived-reply-birth?
                           positioned-wear)
                          machine? derived?
                          (when-let [await (get awaits b-thread)]
                            (>= (:time-ms b 0) (:time-ms await 0))))
                     (conj [uid pos b-thread]))))
          (do
            ;; blocks gone from truth: close their slots (locally-birthed
            ;; blocks awaiting their first re-pull stay)
            (doseq [[uid e] (:blocks @!world)]
              (when (and (not (contains? seen uid)) (not (:local? e)))
                (scene-rt/close-instance! (block-vi uid))
                (swap! !world update :blocks dissoc uid)))
            (when (seq new-replies)
              (doseq [[uid pos _] new-replies]
                (arm-settle! :cell uid pos))
              ;; clear ONLY the matched threads' awaits; prune runs that are
              ;; fully quiet (idle, no error, nothing awaited)
              (doseq [tk (distinct (map #(nth % 2) new-replies))]
                (swap! !ground-runs update tk assoc :await-reply nil))
              (swap! !ground-runs
                     (fn [runs]
                       (into {} (remove (fn [[_ r]]
                                          (and (= :idle (:phase r))
                                               (nil? (:error r))
                                               (nil? (:await-reply r))))
                                        runs))))))))
      (refresh-provisional!)
      nil)))

(def ^:private noise-kinds
  "Run-block section split (Tasks 6+7): these kinds are the thinking +
   tool-call fold; everything else is the reply prose."
  #{:thinking :tool-use :tool-result-span :material-part})

(defn- merge-machine-turn-blocks
  "Interim render rule (Sid): everything the agent did in ONE turn-run —
   thinking, tool calls, tool results, the final reply — renders as ONE raw
   block. A run = the turn RECORD it answers (every ctrl+enter mints one:
   source block + time; a machine turn belongs to the latest record at or
   before its first block's time) — NOT \"consecutive non-sid turns\": Sid
   typing while the agent still works must not fuse the interrupted run
   with the next one (found live 2026-07-20: #TASK 3 typed mid-run glued
   Task 2's tail and the whole Task 3 reply into one block). Each run
   collapses to ONE turn seated at its first member's position, carrying a
   single block (served order, blank-line joins) under the FIRST unit's
   identity; Sid's interjections keep their own seats. Machine turns older
   than every record (pre-record history) pass through un-merged.
   Presentation only: the durable cut is untouched. While this holds,
   machine blocks are read-only (see pointer-up!) — an edit envelope
   carrying the merged text would rewrite unit 1's durable truth."
  [ctx]
  (let [recs   (vec (sort-by #(:time-ms % 0) (:conversation/turn-records ctx [])))
        ;; runs are per-thread: a machine turn belongs to the latest record
        ;; at or before its time IN ITS OWN THREAD (nil = genesis), so two
        ;; threads' interleaved replies never fuse into one block
        recs-by-thread (group-by :thread-id recs)
        run-of (fn [turn]
                 (let [tid   (:thread-id turn)
                       trecs (get recs-by-thread tid [])
                       tm    (:time-ms (first (:blocks turn)) 0)
                       n     (count (take-while #(<= (:time-ms % 0) tm) trecs))]
                   (when (pos? n) [tid (dec n)])))]
    (update ctx :turns
            (fn [turns]
              (let [keyed  (mapv (fn [t]
                                   (assoc t ::run
                                          (when-not (= "sid" (str (:speaker t)))
                                            (or (run-of t) ::recordless))))
                                 turns)
                    by-run (group-by ::run keyed)]
                (->> keyed
                     (keep (fn [t]
                             (let [r (::run t)]
                               (if (vector? r)
                                 (let [members (get by-run r)]
                                   (when (identical? t (first members))
                                     (let [bs (mapcat :blocks members)
                                           ;; the two folds (Tasks 6+7): the
                                           ;; reply prose and the thinking +
                                           ;; tool noise, each behind its own
                                           ;; header toggle
                                           prose (remove #(contains? noise-kinds
                                                                     (:kind %))
                                                         bs)
                                           noise (filter #(contains? noise-kinds
                                                                     (:kind %))
                                                         bs)]
                                       (-> (first members)
                                           (assoc :blocks
                                                  [(assoc (first bs)
                                                          ;; Task 18: the run renders under the FIRST
                                                          ;; member's identity — a delete tombstones ALL
                                                          :member-ids (mapv :id bs)
                                                          :text (str/join "\n\n"
                                                                          (map :text bs))
                                                          :reply-text
                                                          (str/join "\n\n"
                                                                    (map :text prose))
                                                          :noise-text
                                                          (str/join "\n\n"
                                                                    (map :text noise)))])
                                           (dissoc ::run)))))
                                 ;; sid turn, or pre-record machine history
                                 (dissoc t ::run)))))
                     vec))))))

;; served-page dedupe (Task 11 finding): a RESUMED thread session re-harvests
;; its whole jsonl, landing the same historical turns in the thread container
;; again — the run-merge then joins the duplicates into one block (text ×N,
;; seen live as triple-copy). Until the harvest is idempotent server-side,
;; the client drops repeated block ids — FIRST occurrence wins, the same
;; rule truth-text always applied, so render and copy stay one value.
(defonce ^:private !dup-blocks (atom 0))

(defn- dedupe-context-blocks
  [ctx]
  (let [seen    (volatile! #{})
        dropped (volatile! 0)
        turns'  (into []
                      (keep (fn [t]
                              (let [bs (into []
                                             (keep (fn [b]
                                                     (if (contains? @seen (:id b))
                                                       (do (vswap! dropped inc) nil)
                                                       (do (vswap! seen conj (:id b)) b))))
                                             (:blocks t))]
                                (when (seq bs) (assoc t :blocks bs)))))
                      (:turns ctx))]
    (reset! !dup-blocks @dropped)
    (when (pos? @dropped)
      (js/console.warn "[GROUND] served page repeats" @dropped
                       "block(s) — deduped client-side (thread re-harvest suspect)"))
    (assoc ctx :turns turns')))

(defn- mark-episode-boundaries
  "D-core (Sid): stamp :episode-boundary? on the first block of each turn
   whose :episode-id DIFFERS from the previous turn's in the SAME lane (nil
   lane = the main column; each thread column tracks its own chain). Sid's
   utterances ride the canvas container un-stamped, so the marker lands on
   the fresh session's first machine block — 'resuming fresh session,
   underneath it the new streaming message' (Sid). The very first turn a
   page shows sets the lane's baseline and is never marked (a page that
   OPENS mid-episode must not flash a boundary at its top edge)."
  [ctx]
  (let [[turns' _]
        (reduce
         (fn [[out seen] t]
           (let [lane (:thread-id t)
                 eid  (:episode-id t)
                 prev (get seen lane ::none)
                 boundary? (and (some? eid)
                                (not= prev ::none)
                                (not= prev eid)
                                (seq (:blocks t)))
                 t' (if boundary?
                      (update-in t [:blocks 0] assoc :episode-boundary? true)
                      t)]
             [(conj out t') (assoc seen lane eid)]))
         [[] {}]
         (:turns ctx))]
    (assoc ctx :turns turns')))

;; the raw served context last reconciled — the change guard compares RAW
;; (the world stores the merged view, so raw-vs-world would never match)
(defonce ^:private !last-raw-context (atom nil))

(defn on-face-bundle!
  "The <face-main consumer edge in ground mode (replaces build-main-face! —
   the outline-face tree never renders here; the ground IS per-block slots)."
  [{:keys [face-context]}]
  (when face-context
    (when-not (identical? face-context @!last-raw-context)
      (reset! !last-raw-context face-context)
      ;; the served page is honest about truncation; the ground must be too —
      ;; a cut page means durable turns exist that this render cannot show
      ;; (river-page has no cursor and reads from the FRONT), and the symptom
      ;; otherwise presents as "the agent's reply vanished at settle"
      (when (:conversation/truncated? face-context)
        (js/console.warn "[GROUND] served page truncated — newest turns may be missing:"
                         (:conversation/blocks-returned face-context) "blocks served of"
                         (:conversation/river-events-total face-context) "river events, limit"
                         (:conversation/limit face-context)))
      (let [t0 (js/performance.now)]
        (reconcile! (mark-episode-boundaries
                     (merge-machine-turn-blocks
                      (dedupe-context-blocks face-context))))
        (let [dt (- (js/performance.now) t0)]
          (swap! !reconcile-samples
                 (fn [xs] (let [xs (if (>= (count xs) 64) (subvec xs 1) xs)]
                            (conj xs dt))))
          (when (> dt 32)
            (js/console.warn "[GROUND] slow reconcile" (.toFixed dt 1)
                             "ms — paste __ground.report() output")))))
    ;; adopt narrowed truth into a resting confirmed value (cross-check)
    (when-let [fid (:focus @!ground-edit)]
      (swap! !ground-edit ge/adopt-truth fid (truth-text fid))
      (rebuild-block! fid))))

;; ===========================================================================
;; Typing — envelopes through the block-write artery (committed echo)
;; ===========================================================================

(defn- block-info
  "The focused block's envelope identity {:id :document-container-id} from
   served truth (BW-T7: the client computes nothing; O(1) via the context
   index — this runs per keystroke)."
  [unit-id]
  (when-let [b (get (:block-index @!world) unit-id)]
    {:id (:id b) :document-container-id (:document-container-id b)}))

;; narrow-echo samples (the G1 bar's measurement seam — envelope submit →
;; decision → confirmed render, ms). Read via window.__softland_atoms-style
;; console access; never rendered.
(defonce !echo-samples (atom []))

;; the "sometimes": every echo over the 52ms bar, with WHEN and what the
;; resident was doing — the report's answer to "what stalled?"
(defonce !echo-outliers (atom []))

;; client main-thread stall probe: a 250ms heartbeat that records how LATE
;; each tick fires. A late tick = THIS tab's main thread was blocked (GC /
;; long task) — the decisive split for slow echoes: outlier clusters that
;; match these timestamps are client freezes; clusters with a clean
;; heartbeat are server/transport stalls. Hidden tabs throttle timers, so
;; those ticks are ignored rather than recorded as fake stalls.
(defonce !client-stalls (atom []))
#_{:clj-kondo/ignore [:unused-private-var]}
(defonce ^:private !stall-probe
  (let [expected (atom (+ (js/performance.now) 250))]
    (js/setInterval
     (fn []
       (let [now  (js/performance.now)
             late (- now @expected)]
         (reset! expected (+ now 250))
         (when (and (> late 100) (not (.-hidden js/document)))
           (swap! !client-stalls
                  (fn [xs] (let [xs (if (>= (count xs) 32) (subvec xs 1) xs)]
                             (conj xs {:ms (js/Math.round late)
                                       :at (.toISOString (js/Date.))})))))))
     250)))

(defn- submit-envelope! [env]
  (when-let [submit! (bew/edit-submit!)]
    (let [t0 (js/performance.now)]
      (submit! env
               (fn [decision]
                 (swap! !ground-edit ge/on-decision (:request-id env) decision)
                 (when-let [fid (or (:focus @!ground-edit)
                                    (get-in env [:target :target/id]))]
                   (rebuild-block! fid))
                 (let [dt (- (js/performance.now) t0)]
                   (swap! !echo-samples
                          (fn [xs] (let [xs (if (>= (count xs) 512) (subvec xs 1) xs)]
                                     (conj xs dt))))
                   (when (> dt 52)
                     (let [ps  (set (map :phase (vals @!ground-runs)))
                           run (cond (ps :streaming)  "streaming"
                                     (ps :distilling) "distilling"
                                     :else            "idle")]
                       (swap! !echo-outliers
                              (fn [xs] (let [xs (if (>= (count xs) 32) (subvec xs 1) xs)]
                                         (conj xs {:ms (js/Math.round dt)
                                                   :at (.toISOString (js/Date.))
                                                   :run run}))))
                       (js/console.warn "[GROUND] slow echo" (.toFixed dt 1)
                                        "ms (bar 52, resident" run
                                        ") — paste __ground.report() output")))))))))

(defn- object-key* []
  (:conversation/address (:context @!world)))

;; ===========================================================================
;; Birth — the first content act mints the durable block (§5.1 lane)
;; ===========================================================================

(defn- context-at-world-point
  "Mechanical P4 receipt context at one world point. The server supplies
   created-during identity; the client supplies the real pick/placement/worn
   references and the act's timestamp."
  [world-point captured-at-ms]
  (assoc
   (scene-rt/bundle-at-world-point
    world-point (:viewport (metrics)) @!camera)
   :receipt/captured-at-ms captured-at-ms))

(defn- pointer-context
  "Context under the live pointer without changing keyboard focus. This is the
   no-ritual point→say seam used by Ctrl+Enter."
  [captured-at-ms]
  (let [{:keys [!mouse-x !mouse-y]} (:atoms @!refs)]
    (context-at-world-point
     (screen->world (or @!mouse-x 200) (or @!mouse-y 200))
     captured-at-ms)))

(defn- post-birth! [{:keys [block-id text pos]}]
  (let [time-ms (js/Date.now)
        scene-context (context-at-world-point [(:x pos) (:y pos)] time-ms)]
    (-> (js/fetch "/api/episode/block-birth"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str (cond-> {:block-id block-id
                                                 :text text
                                                 :time-ms time-ms
                                                 :position {:x (:x pos) :y (:y pos)}
                                                 :scene-context scene-context}
                                          (drill-conversation-id)
                                          (assoc :conversation-id (drill-conversation-id))))}))
      (.then (fn [resp] (.then (.text resp)
                               (fn [t] {:ok? (.-ok resp)
                                        :data (try (reader/read-string t)
                                                   (catch :default _ nil))}))))
      (.then
       (fn [{:keys [ok? data]}]
         (if-not (and ok? (= :accepted (:status data)))
           (do (swap! !ground-edit ge/birth-failed
                      (or (:error data) :birth-failed))
               (refresh-anchor!))
           (let [unit-id (first (:unit-ids data))
                 pos     {:x (double (:x pos)) :y (double (:y pos))}]
             ;; the block exists NOW: slot from the acked birth text
             (swap! !world assoc-in [:blocks unit-id]
                    (merge {:machine? false
                            :local? true
                            :placement-derived? false}
                           pos))
             (swap! !settle assoc-in [:acked unit-id] pos)
             (let [{:keys [state envelopes]}
                   (ge/birth-acked @!ground-edit unit-id
                                   {:id unit-id
                                    ;; envelope identity from the birth ack
                                    ;; (BW-T7: the client computes nothing)
                                    :document-container-id
                                    (:document-container-id data)}
                                   (or (object-key*) (:address data)))]
               (reset! !ground-edit state)
               (refresh-anchor!)
               (rebuild-block! unit-id)
               (doseq [env envelopes] (submit-envelope! env)))))))
      (.catch (fn [_e]
                (swap! !ground-edit ge/birth-failed :birth-unreachable)
                (refresh-anchor!))))))

;; ===========================================================================
;; The send lane — Ctrl+Enter, revision-pinned, durable-BEFORE-agent
;; ===========================================================================

(defn- run-event! [thread-key turn-id evt]
  (case (:kind evt)
    :episode-durable
    (do (swap! !last-turn-ids assoc thread-key turn-id)
        (swap! !ground-runs update thread-key assoc :phase :streaming
               :activity "the resident is reading" :error nil))

    :text-delta
    (swap! !ground-runs update thread-key
           (fn [r] (-> r (assoc :activity nil)
                       (update :stream-text str (:text evt)))))

    :thinking-delta
    (swap! !ground-runs update thread-key assoc :activity "thinking")

    :tool-use-start
    (swap! !ground-runs update thread-key assoc
           :activity (str "using " (:tool-name evt)))

    :run-done
    (swap! !ground-runs update thread-key assoc
           :phase :distilling :activity "landing the turn")

    :run-error
    ;; the turn closes VISIBLY at its source; retry is a new turn; the
    ;; durable failure fact is the turn cell's status (server-side)
    (swap! !ground-runs update thread-key assoc :phase :idle :activity nil
           :error (str (or (:error evt) :run-error)
                       (when (:detail evt) (str " " (pr-str (:detail evt))))))

    :episode-distilled
    ;; provisional text never survives as truth: the projection drops WHOLE;
    ;; the epoch re-pull renders the durable provenance-marked reply
    (swap! !ground-runs update thread-key
           (fn [r] {:phase :idle :activity nil :stream-text "" :error nil
                    :source-unit-id (:source-unit-id r) :turn-id nil
                    :await-reply {:source-unit-id (:source-unit-id r)
                                  :time-ms (js/Date.now)}}))

    nil)
  (refresh-provisional!))

(defn- transient-notice! [unit-id text]
  (reset! !notice {:unit-id unit-id :text text})
  (rebuild-block! unit-id)
  (js/setTimeout (fn [] (when (= text (:text @!notice))
                          (reset! !notice nil)
                          (rebuild-block! unit-id)))
                 2200))

(defn- adoptive-thread-for
  "Speak beneath a thread and you join it (Task 10): a fresh block's first
   send looks for the thread whose COLUMN it sits in — horizontally
   overlapping the thread's blocks (every source + their replies),
   vertically inside that column or within a short reach below its lowest
   block. Found → the send rides that thread, and the resident answers
   with the whole conversation behind it (the server resumes the thread's
   CLI session); not found → the send mints a fresh attention, as before.
   Judged at send time, so dragging a block into (or out of) a column
  before speaking counts."
  [fid]
  (let [threaded-wear (:threaded (wears-for fid))
        bs   (:blocks @!world)
        b    (get bs fid)
        tmap @!thread-of]
    (when (and b (:x b))
      (let [bx0   (:x b)
            bx1   (+ bx0 (:w b 0))
            reach (* (:threaded/column-adoption-reach-lines threaded-wear)
                     (:line-h (metrics)))
            cols  (reduce-kv
                   (fn [m uid e]
                     (let [tid (or (get tmap uid)
                                   (when (:machine? e)
                                     (get tmap (:source-uid e))))]
                       (if (and tid (not= uid fid) (:x e))
                         (update m tid (fnil conj []) e)
                         m)))
                   {} bs)]
        (->> cols
             (keep (fn [[tid es]]
                     (let [x0 (reduce min (map :x es))
                           x1 (reduce max (map #(+ (:x %) (:w % 0)) es))
                           y0 (reduce min (map :y es))
                           y1 (reduce max (map #(+ (:y %) (:h % 0)) es))]
                       (when (and (< bx0 x1) (> bx1 x0)
                                  (>= (:y b) y0)
                                  (<= (:y b) (+ y1 reach)))
                         [tid (max 0 (- (:y b) y1))]))))
             (sort-by second)
             ffirst)))))

(defn reply-to-block!
  "Invoke the durable resident reply for the ADDRESSED block. The pinned
   revision = the block's confirmed text at send time — the turn record
   makes the pin durable BEFORE the agent spawns; mid-stream edits never
   rewrite what the resident answered.

   One canvas, many conversations: the block's THREAD scopes the run. A
   block that already holds a thread (a served turn record, or a send this
   session) reuses its uuid — later sends resume that CLI session. A fresh
   block's first send joins the thread whose column it sits in
   (adoptive-thread-for — speak beneath a thread and you're in it) or
   MINTS a thread: a new attention with fresh context, running
   concurrently with every other thread. Busy is per-thread — a
   mid-turn thread refuses VISIBLY at its block; the rest of the canvas
   stays sendable. Drill pages (?drill=) keep the legacy single lane."
  [subject]
  (let [st @!ground-edit
        fid subject]
    (when (and fid (get-in @!world [:blocks fid]))
      (let [drill  (drill-conversation-id)
            adopted (when-not (or drill (contains? @!thread-of fid))
                      (adoptive-thread-for fid))
            thread (cond
                     drill nil
                     (contains? @!thread-of fid) (get @!thread-of fid)
                     :else (or adopted (str (random-uuid))))
            run    (get @!ground-runs thread)]
        (if (contains? #{:streaming :distilling} (:phase run))
          (transient-notice! fid "this thread is mid-turn — the block was not sent")
          (let [text (get-in st [:queue :confirmed :text])
                b    (get-in @!world [:blocks fid])]
            (when-not (str/blank? (or text ""))
              (let [turn-id (str (random-uuid))
                    time-ms (js/Date.now)
                    scene-context (pointer-context time-ms)
                    request-body
                    (reply-to-block/request
                     {:subject fid
                      :text text
                      :position {:x (:x b) :y (:y b)}
                      :turn-id turn-id
                      :time-ms time-ms
                      :scene-context scene-context
                      :prev-turn-id (get @!last-turn-ids thread)
                      :thread-id thread
                      :conversation-id drill
                      :wearers
                      (material-inspector/wearers-from-scene-store
                       (scene-rt/store-snapshot))})]
                (swap! !thread-of assoc fid thread)
                (when adopted
                  (transient-notice! fid "joined the conversation above"))
                (swap! !ground-runs assoc thread
                       {:phase :streaming :activity "reaching the land"
                        :stream-text "" :error nil
                        :source-unit-id fid :turn-id turn-id
                        ;; Keep the pinned source measurement, not its
                        ;; material-derived result: an active text-body
                        ;; revision can then re-wrap the live provisional
                        ;; without reading subsequently edited source text.
                        :wrap-source-columns (reply-source-columns text)
                        :await-reply nil})
                (refresh-provisional!)
                (agent/stream-agent-run!
                 "/api/episode/utterance"
                 request-body
                 (partial run-event! thread turn-id)
                 (fn [err]
                   (swap! !ground-runs update thread assoc
                          :phase :idle :activity nil
                          :error (str "send failed: " (.-message err)))
                   (refresh-provisional!)))))))))))

(defn submit-turn!
  "Compatibility entry for callers outside the material dispatch. New
   Ctrl+Enter input resolves `:resident/reply-to-block` and calls
   `reply-to-block!` with the decision subject."
  []
  (when-let [subject (:focus @!ground-edit)]
    (reply-to-block! subject)))

;; ===========================================================================
;; Keys — the :ground-input focus lane (ALL ground keys route here)
;; ===========================================================================

(defn- handle-content-key!
  "Route one content/caret key by the edit machine's mode."
  [event]
  (let [st @!ground-edit]
    (case (:mode st)
      :editing
      (let [fid (:focus st)
            bi  (or (block-info fid) {:id fid})
            {:keys [state envelope]} (ge/input st event bi (object-key*))]
        (reset! !ground-edit state)
        (when envelope (submit-envelope! envelope))
        (rebuild-block! fid))

      :anchor
      (when-let [{:keys [state post]} (ge/begin-birth st event (str (random-uuid)))]
        (reset! !ground-edit state)
        (refresh-anchor!)
        (post-birth! post))

      :birthing
      (reset! !ground-edit (ge/birth-key st event))

      :rest
      ;; type-without-click: the words land under the pointer (Law 1 — the
      ;; land never swallows speech waiting for a ceremony)
      (when (contains? #{:char :enter :paste} (:type event))
        (let [{:keys [!mouse-x !mouse-y]} (:atoms @!refs)
              [wx wy] (screen->world (or @!mouse-x 200) (or @!mouse-y 200))]
          (swap! !ground-edit ge/set-anchor {:x wx :y wy})
          (refresh-anchor!)
          (handle-content-key! event)))

      nil)))

(defn- copy-current!
  "Ctrl+C (Task 11): the selection when one exists, else the focused block's
   confirmed text, else the hovered machine block's reply prose (raw text
   fallback). A pure client read — clipboard only; machine blocks stay
   read-only; nothing durable moves."
  []
  (let [st  @!ground-edit
        fid (:focus st)
        sel (ge/selection-text st)
        msel-txt (machine-sel-text)
        [src uid text]
        (cond
          (seq (or sel ""))
          [:selection fid sel]

          (seq (or msel-txt ""))
          [:machine-selection (:uid @!machine-sel) msel-txt]

          (and fid (= :editing (:mode st)))
          [:focused-block fid (get-in st [:queue :confirmed :text])]

          :else
          (or (when-let [h @!hover]
                (let [b (get-in @!world [:blocks h])]
                  (when (:machine? b)
                    (let [cb (context-block-entry h)
                          r  (:reply-text cb)]
                      [:hovered-reply h
                       (or (when-not (str/blank? (or r "")) r)
                           (:text cb)
                           (truth-text h))]))))
              [:none nil nil]))]
    (js/console.log "[GROUND-COPY] ctrl+c" (str "source=" src)
                    (str "uid=" (pr-str uid))
                    (str "chars=" (count (or text "")))
                    (str "mode=" (:mode st))
                    (str "hover=" (pr-str @!hover)))
    (if-not (seq (or text ""))
      (js/console.log "[GROUND-COPY] nothing to copy — no selection, no focused block, no hovered machine block")
      (if-let [clip (.-clipboard js/navigator)]
        (-> (.writeText clip text)
            (.then (fn [_]
                     (js/console.log "[GROUND-COPY] clipboard write OK")
                     (when uid (transient-notice! uid (str "copied " (count text) " chars")))))
            (.catch (fn [e]
                      (js/console.error "[GROUND-COPY] clipboard write FAILED" e)
                      (when uid (transient-notice! uid "copy failed — clipboard unavailable")))))
        (do (js/console.error "[GROUND-COPY] navigator.clipboard undefined (insecure origin?)")
            (when uid (transient-notice! uid "copy failed — clipboard unavailable")))))))

(defn- delete-group!
  "Delete the marquee-selected blocks (Task 18). ONE durable tombstone write
   rides the geometry settle lane (:deleted? cells, append+await): the serve
   stops including tombstoned units and bumps the epoch, so the blocks leave
   through the normal truth loop — slots close only on the ACK (the tombstone
   IS acked truth), never optimistically. A merged run block expands to ALL
   its member unit-ids (it renders under the FIRST member's identity;
   tombstoning one member would resurrect the run re-keyed on the next)."
  []
  (let [uids  @!group-sel
        all   (vec (distinct (mapcat (fn [uid]
                                       (or (seq (:member-ids (context-block-entry uid)))
                                           [uid]))
                                     uids)))
        cells (mapv (fn [uid]
                      (let [b (get-in @!world [:blocks uid])]
                        {:unit-id uid
                         :x (double (or (:x b) 0.0))
                         :y (double (or (:y b) 0.0))
                         :deleted? true}))
                    all)]
    (when (seq cells)
      (js/console.log "[GROUND-DEL] delete" (count uids) "block(s) →"
                      (count cells) "unit tombstone(s)")
      (-> (js/fetch "/api/episode/geometry"
                    (clj->js {:method "POST"
                              :headers {"Content-Type" "application/edn"}
                              :body (pr-str (cond-> {:cells cells
                                                     :settle-id (str (random-uuid))
                                                     :time-ms (js/Date.now)}
                                              (drill-conversation-id)
                                              (assoc :conversation-id
                                                     (drill-conversation-id))))}))
          (.then (fn [resp]
                   (if (.-ok resp)
                     (do (js/console.log "[GROUND-DEL] tombstones acked —"
                                         "closing slots")
                         (reset! !group-sel #{})
                         (doseq [uid uids]
                           (when (get-in @!world [:blocks uid])
                             (scene-rt/close-instance! (block-vi uid))
                             (swap! !world update :blocks dissoc uid))))
                     (do (js/console.error "[GROUND-DEL] delete refused"
                                           (.-status resp))
                         (when-let [uid (first uids)]
                           (transient-notice! uid "delete refused"))))))
          (.catch (fn [e]
                    (js/console.error "[GROUND-DEL] delete unreachable" e)
                    (when-let [uid (first uids)]
                      (transient-notice! uid
                                         "delete failed — land unreachable"))))))))

(declare dispatch-key-eval!)

(defn ground-keys-consumer
  [_atoms <ground-keyboard]
  (->> <ground-keyboard
       (m/reduce
        (fn [_ event]
          (when event
            (case (:type event)
              :eval (dispatch-key-eval!)
              :copy (copy-current!)
              (:char :backspace :delete :enter :paste
               :left :right :up :down :home :end :word-left :word-right)
              ;; Task 18: delete/backspace fires the group delete when a
              ;; marquee selection stands and no block is being edited
              (if (and (contains? #{:backspace :delete} (:type event))
                       (seq @!group-sel)
                       (not= :editing (:mode @!ground-edit)))
                (delete-group!)
                (handle-content-key! event))
              nil))
          nil)
        nil)))

(defn handle-paste! [text]
  (when (seq text)
    (handle-content-key! {:type :paste :text text})))

(defn escape!
  "Escape: blur the focused block / discard the anchor (an abandoned anchor
   leaves NOTHING). Attention state only — nothing durable moves."
  []
  (clear-machine-sel!)
  (clear-group-sel!)
  (reset! !halo nil)
  (render-halo!)
  (swap! !ground-edit (fn [st]
                        (let [fid (:focus st)
                              st' (ge/escape st)]
                          (when fid (js/setTimeout #(rebuild-block! fid) 0))
                          st')))
  (refresh-anchor!))

;; ===========================================================================
;; The four stations of input (DIRECTION §The architecture) — editable-material
;; P5. Mechanism here, policy in material:
;;
;;   1. gesture      — normalize the raw event                     (kernel)
;;   2. pick         — ONE pick over the containment path          (kernel)
;;   3. claim        — innermost matching binding row              (MATERIAL)
;;   4. verb         — the named registry entry                    (kernel)
;;
;; Stations 3 and 4 are `binding-material/resolve-binding`, a pure function
;; over data. It decides; nothing here decides. The only meaning-bearing
;; branch left in this file is the one that picks a verb's implementation out
;; of a map, so a NEW interaction is a row plus (at most) a registry entry —
;; never a new line in pointer-down!/move!/up!.
;;
;; FROZEN BRANCH COUNTS (the package's per-family freeze): pointer-down! 0,
;; pointer-move! 3 (:idle hover · :pending threshold · :active continuation),
;; pointer-up! 2 (:pending tap · :active end), handle-wheel! 0. Those three
;; move!/up! branches are the kernel's phase MACHINE — "which continuation is
;; running" — not meaning. `dispatch-mechanism-budget` below states them as
;; data and the suite asserts it, so growing them is a visible act.
;;
;; The ~4 CSS px threshold (§9.4) still splits click from pan/drag, and the
;; wheel still zooms at the pointer — but both now arrive as rows.
;; ===========================================================================

(def dispatch-mechanism-budget
  "The kernel's whole dispatch surface, as data. Every legal gesture the
   normalizer can produce, every phase the pointer machine can be in, and the
   exact branch count of each entry point.

   FROZEN at what P5 migrated. New MEANINGS arrive as binding rows and never
   touch this map; it grows only for a genuinely new input device or
   continuation moment — a kernel change, a grammar version, and a suite edit,
   all visible.

   `:mechanism-branches` counts branches on the pointer MACHINE's own state
   (which continuation is running) and on a verb's declared continuation shape.
   None of them branch on a gesture, a modifier, a subject kind, or a
   geometric row — those were the ladders, and they are gone."
  {:gestures binding-material/legal-gestures
   :pointer-phases #{:idle :pending :active}
   :continuations verb-registry/continuations
   :meaning-branches 0
   :mechanism-branches {:pointer-down! 0
                        :pointer-move! 3
                        :pointer-up! 2
                        :handle-wheel! 0}})

(defn- pick-at [sx sy]
  (let [[wx wy] (screen->world sx sy)]
    (when (scene-rt/any-slots?) (scene-rt/pick-world [wx wy]))))

(defn- world->caret
  "World point → caret index inside a block's text (monospace math — the
   same line/col rule as the caret click)."
  [b text wx wy {:keys [line-h char-advance]}]
  (let [line (js/Math.floor (/ (- wy (:y b)) line-h))
        col  (js/Math.round (/ (- wx (:x b)) char-advance))]
    (ge/line-col->caret (or text "") (max 0 line) (max 0 col))))

(defn- world->lc
  "World point → visual {:line :col} inside a block (monospace grid)."
  [b wx wy {:keys [line-h char-advance]}]
  {:line (js/Math.floor (/ (- wy (:y b)) line-h))
   :col  (js/Math.round (/ (- wx (:x b)) char-advance))})

(defn- marquee-rect
  "The sweep rect in world coords, press point → pointer (Task 18)."
  [[ax ay] wx wy]
  {:x (min ax wx) :y (min ay wy)
   :w (max 2.0 (js/Math.abs (- wx ax)))
   :h (max 2.0 (js/Math.abs (- wy ay)))})

(defn- refresh-marquee!
  "Paint/resize the marquee slot (attention-only visual, anchor pattern)."
  [{:keys [x y w h]}]
  (let [tree (rt/resolve-layout
              (rt-node :ground-marquee :rect {:x 0 :y 0 :w w :h h}
                       :style {:border-width 1.0
                               :border-color [0.55 0.65 0.9 0.7]
                               :bg [0.35 0.5 0.8 0.08]}))]
    (if-let [slot (ss/slot (scene-rt/store-snapshot) :ground-marquee)]
      (do (swap! scene-rt/!scene-store ss/upsert-slot :ground-marquee
                 {:tree tree :container (:container slot)
                  :meta (:meta slot) :stratum (:stratum slot)
                  :pre-resolved? true})
          (scene-rt/set-transform! (:container slot) {:x x :y y}))
      (scene-rt/register-face-instance! :ground-marquee tree
                                        {:x x :y y :scale 1.0 :layer 4
                                         :meta {:ground-marquee? true}
                                         :pre-resolved? true}))))

(defn- marquee-hits
  "Unit-ids whose block AABB intersects the sweep rect."
  [{:keys [x y w h]}]
  (into #{}
        (keep (fn [[uid b]]
                (when (and (:x b)
                           (< x (+ (:x b) (or (:w b) 0.0)))
                           (< (:x b) (+ x w))
                           (< y (+ (:y b) (or (:h b) 0.0)))
                           (< (:y b) (+ y h)))
                  uid)))
        (:blocks @!world)))

(defn- drag-group
  "The rigid drag unit (Task 4, Sid): a user block + every machine block
   whose run launched from it move as ONE — grabbing either end moves both.
   A machine block with no resolved source drags alone."
  [uid]
  (let [bs  (:blocks @!world)
        e   (get bs uid)
        src (if (:machine? e) (:source-uid e) uid)]
    (if (nil? src)
      [uid]
      (into [src] (keep (fn [[k v]] (when (= src (:source-uid v)) k)) bs)))))

;; ---------------------------------------------------------------------------
;; Station 3's inputs — the three locality tiers
;; ---------------------------------------------------------------------------

(def ^:private floor-binding-rows
  "Every facet's CODE FLOOR rows, read from its SPEC, plus the space's table.
   A compile-time constant: this is the tier no revision can reach, which is
   what makes click-focus survive any data revision and the camera survive
   even a future `fm:space` master. Facets that carry no rows contribute nil."
  (binding-material/with-halo-floor-bindings
   (assoc
    (into {}
          (map (fn [spec]
                 [(:facet-master/facet spec)
                  (:facet-master/bindings (facet-material/code-floor spec))]))
          facet-masters/specs)
    ;; T2 — the space spec's material floor is zoom form only. These four rows
    ;; are the camera-inclusive CODE floor and therefore win this association.
    binding-material/space-facet binding-material/space-floor-bindings)))

(defonce ^:private !instance-bindings
  ;; [subject site] → [row …] — the INNERMOST locality tier.
  (atom {}))

(defn set-instance-bindings!
  "Install instance-tier rows for ONE subject at ONE site (DIRECTION: instance
   material is legal; locality tiers decide precedence). Rows pass the SAME
   closed grammar as master rows, so an instance row can no more name arbitrary
   code — or a floor-reserved verb — than a master row can. A malformed set is
   refused WHOLE; the tier is never half-installed.

   P5 makes the tier legal and resolves it. It does NOT mint a durable owner
   for instance rows: durable instance-level material is P6's instance-deviation
   work (DIRECTION §Horizon — the material-truth owner condenses at the first
   homeless truth, after a platform-check). Until then rows arrive through this
   seam and die with the page, which is the honest lifetime of a client value."
  [subject site rows]
  (let [rows (vec rows)]
    (cond
      (not (contains? binding-material/sites site))
      {:status :refused :error :binding/unknown-site :site site}

      ;; T-R4 — the fence stays ordered before owner-aware site legality, so the
      ;; lifted space instance can never expose a transient camera capture.
      (some #(binding-material/camera-gesture-reserved? site %) rows)
      {:status :refused :error :binding/camera-gesture-reserved
       :site site :subject subject}

      (some #(binding-material/meta-gesture-reserved? site %) rows)
      {:status :refused :error :binding/meta-gesture-reserved
       :site site :subject subject}

      ;; G10 / T-R3 — legality is owner-scoped: blocks keep their old sites and
      ;; only the space claim subject may open `:space/ground`.
      (not (binding-material/instance-site-legal? site subject))
      {:status :refused :error :binding/instance-site-refused
       :site site :subject subject
       :legal-sites (vec (sort-by str binding-material/instance-legal-sites))}

      (not (binding-material/valid-bindings? {site rows}))
      {:status :refused :error :facet-master/bindings-invalid
       :subject subject :site site}

      :else
      (do (swap! !instance-bindings assoc [subject site] rows)
          {:status :installed :subject subject :site site
           :rows (count rows)}))))

(defn clear-instance-bindings!
  ([] (reset! !instance-bindings {}) {:status :cleared})
  ([subject site]
   (swap! !instance-bindings dissoc [subject site])
   {:status :cleared :subject subject :site site}))

(defn- served-instance-binding-rows
  "P6 · deliverable 8 — the DURABLE instance tier feeding `resolve-binding`.

   P5 resolved this tier but had no owner for it, so rows could only arrive
   through the console seam and died with the page. They now come from served
   instance masters: a subject whose instance revision carries
   `:facet-master/bindings` claims those gestures for itself, at the innermost
   locality tier, with a revision id behind it and a rollback available.

   Derived from the wears cache, so it costs one map build per activation
   rather than one per gesture (T5). G10 is enforced HERE too, not only at the
   console seam — a durable revision must not be able to reach a fence that a
   console call cannot."
  []
  (let [cached @!wears-cache
        instances (:facet-materials/instances (:served cached))]
    (persistent!
     (reduce-kv
      (fn [acc facet by-subject]
        (reduce-kv
         (fn [acc subject _inst]
           (let [wear (get (wears-for subject) facet)]
             ;; T-R5 — every deviation snapshots inherited bindings, so ANY
             ;; active space deviation intentionally lifts tap/marquee together.
             (if (= :instance (:facet-master/tier wear))
               (reduce-kv
                (fn [acc site rows]
                  ;; T-R1 — durable space subjects are strings, while dispatch's
                  ;; collision-proof outer claim subject is the keyword `:space`.
                  (let [claim-subject
                        (if (and
                             (= binding-material/space-facet facet)
                             (= space-material/space-subject subject))
                          :space
                          subject)]
                    (if (and
                         (binding-material/instance-site-legal? site facet)
                         ;; T3/T4 — a served instance must cross the same fence
                         ;; as the console and fm:space master grammar.
                         (not-any?
                          #(or
                            (binding-material/camera-gesture-reserved? site %)
                            (binding-material/meta-gesture-reserved? site %))
                          rows))
                      (assoc! acc [claim-subject site]
                              (into (vec (get acc [claim-subject site])) rows))
                      acc)))
                acc
                (:facet-master/bindings wear))
               acc)))
         acc
         by-subject))
      (transient {})
      instances))))

(defn- instance-binding-rows
  "The instance tier the law sees: DURABLE served rows, then the ephemeral
   console seam layered over them. The console stays the experiment lane —
   it can shadow a durable row for a session, and it dies with the page, which
   is the honest lifetime of a client value."
  []
  (merge (served-instance-binding-rows) @!instance-bindings))

(defn- current-master-binding-rows
  "The MASTER tier: each served facet's active rows. Derived from the wears
   cache, so it costs one map build per activation, not one per gesture. A
   facet whose active revision predates its bindings grammar contributes nil —
   and its floor rows carry the behavior, which is why the client is correct
   before the P5 ingest is ever deployed."
  []
  (let [wears (current-material-wears)]
    (or (:master-rows @!wears-cache)
        (let [rows (persistent!
                    (reduce-kv
                     (fn [m facet wear]
                       ;; a FLOORED wear (absent or malformed revision) already
                       ;; IS the code floor: counting its rows as a master
                       ;; contribution would report `:master` for a decision the
                       ;; floor actually made, and the drill's tier receipt is
                       ;; the fence's evidence. The floor tier answers instead.
                       (assoc! m facet
                               (when-not (:facet-master/floor? wear)
                                 (:facet-master/bindings wear))))
                     (transient {})
                     wears))]
          (swap! !wears-cache assoc :master-rows rows)
          rows))))

(defn- claim-chain
  "Station 2 → station 3: ONE claim builder, INNERMOST FIRST and always ending
   at the space. `ss/pick` returns root→leaf, so the reverse walk is the law's
   outward fallthrough — header → block → space.

   `seed-claims` is the key/eval address lane. A pick MISS or stale slot yields
   no picked claims, then receives the same outer space rung by construction."
  [hit seed-claims]
  (let [blocks (:blocks @!world)
        claims (or seed-claims
                   (when hit
                     (into []
                           (keep
                            (fn [node]
                              (when-let [c (get-in node [:data :material/claim])]
                                (when (contains? blocks (:claim/subject c)) c))))
                           (rseq (:path hit))))
                   [])]
    ;; T1 — space-claim is a one-element VECTOR; conj would nest and kill it.
    (into claims binding-material/space-claim)))

;; ---------------------------------------------------------------------------
;; Conflict lint — a tie is rendered, never silent
;; ---------------------------------------------------------------------------

(def ^:private binding-lint-vi :ground-binding-lint)
(defonce ^:private !binding-conflicts (atom []))

(defn- refresh-binding-lint!
  "Same-depth, same-tier, same-priority ties render as a card in the land. The
   winner is still deterministic and the gesture still works — a tie degrades
   to lint, never to a dead interaction. Unlike the malformed-candidate card
   this is NOT drill-scoped: a tie lives in ACTIVE material, so it is the
   land's business."
  []
  (let [conflicts @!binding-conflicts]
    (if (seq conflicts)
      (let [{:keys [font-size char-advance line-h]} (metrics)
            lines
            (into ["binding conflict · deterministic winner shown"]
                  (map
                   (fn [c]
                     (str (pr-str (:binding/gesture c))
                          " @ " (:binding/site c)
                          " · " (pr-str (:binding/verbs c))
                          " → " (pr-str (:binding/winner c))
                          " (priority " (:binding/priority c) ")")))
                  (take 6 conflicts))
            pad 10.0
            max-len (reduce max 1 (map count lines))
            w (+ (* 2 pad) (* max-len char-advance))
            h (+ (* 2 pad) (* (count lines) line-h))
            ops (mapv (fn [i line]
                        (text-op line i line-h font-size amber pad))
                      (range)
                      lines)
            [x y] (screen->world 18.0 120.0)
            tree (rt/resolve-layout
                  (rt-node binding-lint-vi :error-card
                           {:x 0 :y 0 :w w :h h}
                           :style {:bg [0.20 0.15 0.05 0.96]
                                   :border-width 1.0
                                   :border-color [0.92 0.75 0.35 1.0]
                                   :radius 4}
                           :text ops
                           :data {:binding/conflicts conflicts}))]
        (if-let [slot (ss/slot (scene-rt/store-snapshot) binding-lint-vi)]
          (do
            (swap! scene-rt/!scene-store ss/upsert-slot binding-lint-vi
                   {:tree tree :container (:container slot)
                    :meta (:meta slot) :stratum (:stratum slot)
                    :pre-resolved? true})
            (scene-rt/set-transform! (:container slot) {:x x :y y}))
          (scene-rt/register-face-instance!
           binding-lint-vi tree
           {:x x :y y :scale 1.0 :layer 6
            :meta {:ground-binding-lint? true}
            :pre-resolved? true})))
      (scene-rt/close-instance! binding-lint-vi))))

(defn- binding-tables
  "The tiers as the interaction-table shape, for lint + the console listing."
  []
  (let [wears (current-material-wears)
        master (current-master-binding-rows)]
    (-> []
        ;; P6: the instance tier in the table is the DURABLE served rows plus
        ;; the ephemeral console seam — the same value the dispatch law sees,
        ;; so the table cannot disagree with what a gesture will actually do
        (into (map (fn [[[subject site] rows]]
                     {:tier :instance :facet nil
                      ;; T-R6 — a keyword subject must label as `instance:space`,
                      ;; never the stale double-colon `instance::space`.
                      :master-id
                      (str "instance:"
                           (if (keyword? subject) (name subject) subject))
                      :revision-id nil :floor? false
                      :bindings {site rows}}))
              (instance-binding-rows))
        (into (comp (filter (comp seq val))
                    (map (fn [[facet rows]]
                           (let [wear (get wears facet)]
                             {:tier :master :facet facet
                              :master-id (:facet-master/id wear)
                              :revision-id (:facet-master/revision-id wear)
                              :floor? (true? (:facet-master/floor? wear))
                              :bindings rows}))))
              master)
        (into (comp (filter (comp seq val))
                    (map (fn [[facet rows]]
                           ;; G14 (P5 gate finding 3): this emitted
                           ;; `code-floor:attention` + a nil revision while the
                           ;; server emitted `code-floor:fm:attention:v1` in
                           ;; BOTH fields — one row, two labels. Both sides now
                           ;; read the ONE map in facet-masters.
                           (let [label (facet-masters/floor-master-id facet)]
                             {:tier :floor :facet facet
                              :master-id label
                              :revision-id label
                              :floor? true
                              :bindings rows}))))
              floor-binding-rows))))

(defn- recompute-binding-conflicts!
  "Recompute lint over the WHOLE served table, not only what a gesture happens
   to trip. Called wherever the material changes, so a tie is visible before
   anyone touches it."
  []
  (let [conflicts (binding-material/table-conflicts
                   (binding-material/table-rows (binding-tables)))]
    (when (not= conflicts @!binding-conflicts)
      (reset! !binding-conflicts conflicts)
      (refresh-binding-lint!))))

;; ---------------------------------------------------------------------------
;; Station 4 — the verb implementations
;; ---------------------------------------------------------------------------

(defonce ^:private !verb-impls
  ;; verb-name → {continuation → (fn [ctx] …)}. The registry declares WHAT a
  ;; verb is (name, version, effect class); this atom holds the code. A row can
  ;; only reach a declared name, and only a declared name can be registered
  ;; here, so material and code meet at exactly one closed vocabulary.
  (atom {}))

(defn- register-verb!
  [verb-name impls]
  (when-not (verb-registry/entry verb-name)
    (throw (ex-info "verb not in the registry" {:verb verb-name})))
  (swap! !verb-impls assoc verb-name impls))

;; :focus/place-caret ← pointer-up! :pending, non-machine block
(register-verb! :focus/place-caret
  {:invoke
   (fn [{:keys [subject press]}]
     (when (get-in @!world [:blocks subject])
       (let [old   (:focus @!ground-edit)
             truth (or (truth-text subject) "")]
         (swap! !ground-edit ge/focus-block subject truth
                (:press/caret-truth press))
         (refresh-anchor!)
         (when (and old (not= old subject)) (rebuild-block! old))
         (rebuild-block! subject))))})

;; :focus/enter-block ← pointer-down!'s `(when (and text? (not= uid focus)) …)`
(register-verb! :focus/enter-block
  {:invoke
   (fn [{:keys [subject press]}]
     (when (and (get-in @!world [:blocks subject])
                (not= subject (:focus @!ground-edit)))
       (let [old   (:focus @!ground-edit)
             truth (or (truth-text subject) "")]
         (swap! !ground-edit ge/focus-block subject truth
                (:press/caret-truth press))
         (refresh-anchor!)
         (when old (rebuild-block! old))
         (rebuild-block! subject))))})

;; :focus/release ← pointer-up! :pending, machine block with no fold row
(register-verb! :focus/release
  {:invoke
   (fn [{:keys [subject]}]
     (let [old (:focus @!ground-edit)]
       (swap! !ground-edit ge/escape)
       (refresh-anchor!)
       (when old (rebuild-block! old))
     (rebuild-block! subject)))})

;; :resident/reply-to-block ← Ctrl+Enter's pre-P8 `submit-turn!` path. The
;; decision subject, not a second read of focus, is the durable target.
(register-verb! :resident/reply-to-block
  {:invoke
   (fn [{:keys [subject]}]
     (reply-to-block! subject))})

;; :fold/toggle-section ← pointer-up! :pending fold branch (Task 7). The
;; section arrives from the CLAIM: the header node hit is the section.
(register-verb! :fold/toggle-section
  {:invoke
   (fn [{:keys [subject args]}]
     (swap! !folds update subject
            (fn [f]
              (update
               (or f
                   (:foldable/defaults
                    (:foldable (wears-for subject))))
               (:fold-key args)
               not)))
     (rebuild-block! subject))})

;; :anchor/place ← pointer-up! :pending, :ground target (Law 1)
(register-verb! :anchor/place
  {:invoke
   (fn [{:keys [press]}]
     (let [[wx wy] (:press/world press)]
       (swap! !ground-edit ge/set-anchor {:x wx :y wy})
       (refresh-anchor!)
       ;; PRESERVED AS FOUND, not as intended: `set-anchor` already cleared
       ;; :focus, so this read is nil and the previously-focused block is NOT
       ;; rebuilt here (its ring clears on the next reconcile). The comment it
       ;; carried pre-P5 — "click-elsewhere leaves a focused block first" —
       ;; describes the intent, not the behavior. A strangler migration must
       ;; not silently change behavior, so the order is kept and the finding is
       ;; reported instead of smuggled.
       (when-let [old (:focus @!ground-edit)] (rebuild-block! old))))})

;; :selection/text-begin ← pointer-move! :pending `(:text? p)` + :selecting
(register-verb! :selection/text-begin
  {:begin
   (fn [{:keys [press]}]
     (clear-machine-sel!)
     (swap! !ground-edit ge/begin-select (:press/caret-confirmed press)))
   :move
   (fn [{:keys [subject world]}]
     (let [b (get-in @!world [:blocks subject])
           [wx wy] world]
       (when b
         (swap! !ground-edit ge/extend-select
                (world->caret b (get-in @!ground-edit
                                        [:queue :confirmed :text])
                              wx wy (metrics)))
         (rebuild-block! subject))))
   :end (fn [_] nil)})

;; :selection/machine-begin ← pointer-move! :pending `(:mtext? p)` + :mselecting
(register-verb! :selection/machine-begin
  {:begin
   (fn [{:keys [subject press]}]
     (when-let [old (:focus @!ground-edit)]
       (swap! !ground-edit assoc :selection nil)
       (rebuild-block! old))
     (reset! !machine-sel {:uid subject
                           :anchor (:press/lc press)
                           :head (:press/lc press)}))
   :move
   (fn [{:keys [subject world]}]
     (let [b (get-in @!world [:blocks subject])
           [wx wy] world]
       (when b
         (swap! !machine-sel assoc :head (world->lc b wx wy (metrics)))
         (rebuild-block! subject))))
   :end (fn [_] nil)})

;; :selection/marquee-begin ← pointer-move! :pending marquee + pointer-up!
;; :marquee (Task 18)
(register-verb! :selection/marquee-begin
  {:begin
   (fn [{:keys [press world]}]
     (clear-machine-sel!)
     (clear-group-sel!)
     (let [[wx wy] world]
       (refresh-marquee! (marquee-rect (:press/world press) wx wy))))
   :move
   (fn [{:keys [press world]}]
     (let [[wx wy] world]
       (refresh-marquee! (marquee-rect (:press/world press) wx wy))))
   :end
   (fn [{:keys [press world]}]
     (let [[wx wy] world
           hit (marquee-hits (marquee-rect (:press/world press) wx wy))]
       (scene-rt/close-instance! :ground-marquee)
       (js/console.log "[GROUND-SEL] marquee up" (str "hit=" (count hit)))
       (reset! !group-sel hit)
       (doseq [uid hit] (rebuild-block! uid))))})

;; :placement/drag-group ← pointer-move! :pending :else + :dragging +
;; pointer-up! :dragging (Task 4). Per-member grabs are frozen at press, so
;; the group drags as a RIGID formation.
(register-verb! :placement/drag-group
  {:begin (fn [_] nil)
   :move
   (fn [{:keys [subject press world]}]
     (let [[wx wy] world]
       (doseq [[guid [gx gy]] (or (seq (:press/grabs press))
                                  [[subject (:press/grab press)]])]
         (let [pos {:x (+ wx gx) :y (+ wy gy)}]
           (swap! !world update-in [:blocks guid] merge pos)
           (when-let [cid (get-in @!world [:blocks guid :cid])]
             (scene-rt/set-transform! cid pos))))))
   :end
   ;; gesture end ARMS the settle for EVERY dragged member — the debounce
   ;; coalesces the group into ONE acked cells write, never per-event. This is
   ;; why the verb's effect class is :durable-via-request.
   (fn [{:keys [subject press]}]
     (doseq [[guid _] (or (seq (:press/grabs press)) [[subject nil]])]
       (when-let [gb (get-in @!world [:blocks guid])]
         (arm-settle! :cell guid {:x (:x gb) :y (:y gb)}))))})

;; :camera/pan ← pointer-move! :panning + pointer-up! :panning. Floor-reserved.
(register-verb! :camera/pan
  {:begin (fn [_] nil)
   :move
   (fn [{:keys [press screen]}]
     (let [[sx sy] screen
           [sx0 sy0] (:press/screen press)
           cam0 (:press/camera press)]
       (reset! !camera (assoc cam0
                              :x (+ (:x cam0) (- sx sx0))
                              :y (+ (:y cam0) (- sy sy0))))))
   :end (fn [_] (arm-settle! :camera))})

;; :camera/zoom-at-pointer ← handle-wheel! (§9.4). Floor-reserved.
(register-verb! :camera/zoom-at-pointer
  {:invoke
   (fn [{:keys [wheel screen]}]
     (let [[x y] screen
           {:keys [zoom] :as _cam} @!camera
           ;; T-R2/T5 — read the space subject's instance-aware wear through the
           ;; existing cache. Activation identity invalidates it; no new state/read.
           space-wear (:space (wears-for space-material/space-subject))
           zoom-min (or (:space/zoom-min space-wear) 0.1)
           zoom-max (or (:space/zoom-max space-wear) 8.0)
           factor (js/Math.pow 1.0015 (- (:dy wheel)))
           zoom'  (-> (* zoom factor) (max zoom-min) (min zoom-max))
           [wx wy] (screen->world x y)]
       (reset! !camera {:x (- x (* wx zoom'))
                        :y (- y (* wy zoom'))
                        :zoom zoom'})
       (arm-settle! :camera)))})

;; Halo P1 — meta is an ordinary discrete dispatch decision. The decision's
;; subject is authoritative; the effect never re-reads mutable focus.
(register-verb! :halo/condense
  {:invoke
   (fn [{:keys [subject world]}]
     (open-halo! subject world))})

;; matter-room P3 / Halo P1 — these names complete the kernel's closed
;; registry,
;; but no current site can feed their required master/subject arguments.
;; Therefore a frozen-v1 row honestly reaches a no-op while v2+ refuses it
;; structurally. Real invocation is the separately named console/HTTP act lane;
;; preview remains the existing `__bindings.preview` client lane below.
(register-verb! :matter/deviate
  {:invoke (fn [_] nil)})

(register-verb! :matter/preview
  {:invoke (fn [_] nil)})

(register-verb! :matter/say
  {:invoke (fn [_] nil)})

(register-verb! :matter/activate
  {:invoke (fn [_] nil)})

(register-verb! :matter/rollback
  {:invoke (fn [_] nil)})

;; ---------------------------------------------------------------------------
;; THE dispatch — one law, no gesture-specific branch
;; ---------------------------------------------------------------------------

(defn- decide
  "Stations 1–3 as one pure-input call. Returns the decision; effects nothing."
  [{:keys [kind phase modifiers hit claims]}]
  (binding-material/resolve-binding
   {:gesture (binding-material/normalize-gesture kind phase modifiers)
    ;; T7 — pointer and key/eval both cross the ONE chain builder.
    :claims (claim-chain hit claims)
    :facet-rows (current-master-binding-rows)
    :floor-rows floor-binding-rows
    :instance-rows (instance-binding-rows)}))

(defn- conflict-key
  "A tie's identity: which gesture, at which site, in which tier, at which
   priority. `resolve-binding`'s live conflict carries a subject and a depth
   that the whole-table lint does not, so identity has to be the tie itself —
   otherwise one tie renders twice, once per shape."
  [c]
  [(:binding/gesture c) (:binding/site c)
   (:binding/tier c) (:binding/priority c)])

(defn- note-conflicts!
  [decision]
  (when-let [cs (seq (:decision/conflicts decision))]
    (let [known (into #{} (map conflict-key) @!binding-conflicts)
          fresh (remove #(contains? known (conflict-key %)) cs)]
      (when (seq fresh)
        (swap! !binding-conflicts into fresh)
        (refresh-binding-lint!)))))

(defn- invoke-verb!
  "Station 4. The ONE side-effecting site in the whole dispatch: everything
   upstream is pure data, which is why `no side effects in reactive queries`
   holds structurally — nothing above this line can effect even if a caller
   wanted it to."
  [decision continuation ctx]
  (when-let [verb-name (get-in decision [:decision/verb :verb/name])]
    (when-let [impl (get-in @!verb-impls [verb-name continuation])]
      (impl (assoc ctx
                   :verb verb-name
                   :subject (:decision/subject decision)
                   :args (:decision/args decision)
                   :decision decision))
      verb-name)))

(defn- dispatch!
  "Normalize → pick → resolve → invoke, for a DISCRETE gesture."
  [{:keys [kind phase modifiers hit press screen world wheel]}]
  (let [decision (decide {:kind kind :phase phase
                          :modifiers modifiers :hit hit})]
    (note-conflicts! decision)
    (invoke-verb! decision :invoke
                  {:press press :screen screen :world world :wheel wheel})
    decision))

(defn- dispatch-key-eval!
  "Ctrl+Enter mechanism: address the focused user block, then use the SAME
   normalized resolver and the SAME invocation site as every pointer gesture.
   The claim contains no behavior; it only names what the key act addressed."
  []
  (when-let [subject (:focus @!ground-edit)]
    (let [claim {:claim/subject subject
                 :claim/site :block/user-hit-area
                 :claim/facets binding-material/block-claim-facets
                 :claim/args {}}
          decision (decide {:kind :key/eval
                            :phase :complete
                            :modifiers #{}
                            :claims [claim]})]
      (note-conflicts! decision)
      (invoke-verb! decision :invoke {})
      decision)))

;; ---------------------------------------------------------------------------
;; The pointer machine — three phases, zero meaning
;; ---------------------------------------------------------------------------

(defn- press-record
  "Everything a later station may need about the press, all of it GEOMETRY
   (mechanism, frozen). The three caret derivations are computed here rather
   than in a verb so that a threshold or tap resolves against the point that
   was actually pressed — the pre-P5 code did exactly the same, eagerly, in
   pointer-down!."
  [sx sy shift? hit]
  (let [[wx wy] (screen->world sx sy)
        subject (let [addr (:address hit)]
                  (when (get-in @!world [:blocks addr]) addr))
        b       (when subject (get-in @!world [:blocks subject]))
        m       (metrics)]
    (cond-> {:press/screen [sx sy]
             :press/world [wx wy]
             :press/modifiers (if shift? #{:shift} #{})
             :press/hit hit
             :press/subject subject
             :press/camera @!camera}
      b (assoc
         :press/grab [(- (:x b) wx) (- (:y b) wy)]
         ;; per-member grabs frozen at press: the group drags as a RIGID
         ;; formation (each member keeps its offset)
         :press/grabs
         (vec (keep (fn [guid]
                      (when-let [gb (get-in @!world [:blocks guid])]
                        [guid [(- (:x gb) wx) (- (:y gb) wy)]]))
                    (drag-group subject)))
         :press/caret-truth
         (world->caret b (or (truth-text subject) "") wx wy m)
         :press/caret-confirmed
         (world->caret b (get-in @!ground-edit [:queue :confirmed :text])
                       wx wy m)
         :press/lc (world->lc b wx wy m)))))

(defn- reread-confirmed-caret
  "Re-read the confirmed-queue caret AFTER the press verbs have run.

   This is load-bearing, not tidying: `ge/focus-block` REPLACES the confirmed
   queue with the newly focused block's truth, and pre-P5 pointer-down! built
   its selection-anchor caret AFTER its own focus branch — so a shift-press that
   focused an unfocused block anchored the selection in THAT block's text.
   Reading it before the press dispatch would anchor in the previous focus's
   text instead. Same point, same math, correct moment."
  [press]
  (let [subject (:press/subject press)
        b (when subject (get-in @!world [:blocks subject]))]
    (if b
      (let [[wx wy] (:press/world press)]
        (assoc press :press/caret-confirmed
               (world->caret
                b (get-in @!ground-edit [:queue :confirmed :text])
                wx wy (metrics))))
      press)))

(defn pointer-down!
  ([sx sy] (pointer-down! sx sy false))
  ([sx sy shift?]
   (let [hit   (pick-at sx sy)
         ;; deictic seam (scene-substrate P4): pointing is a click act, never
         ;; a hover side effect
         _     (scene-rt/record-pick! (vec (screen->world sx sy)) hit)
         press (press-record sx sy shift? hit)]
     (js/console.log "[GROUND-SEL] down"
                     (str "shift?=" shift?)
                     (str "subject=" (pr-str (:press/subject press)))
                     (str "focus=" (pr-str (:focus @!ground-edit))))
     ;; the press gesture itself is dispatchable — attention's shift-press row
     ;; is what used to be pointer-down!'s inline focus branch
     (dispatch! {:kind :pointer/press :phase :begin
                 :modifiers (:press/modifiers press)
                 :hit hit :press press :screen [sx sy]
                 :world (:press/world press)})
     (reset! !pointer {:phase :pending
                       :press (reread-confirmed-caret press)}))))

(defn pointer-move! [sx sy]
  (let [p @!pointer]
    ;; hover = attention (Law 10) — ephemeral, never restored. Universal: it
    ;; runs for every idle move regardless of any claim, so it is not a branch
    ;; of meaning.
    (when (= :idle (:phase p))
      (let [uid (:address (pick-at sx sy))
            uid (when (get-in @!world [:blocks uid]) uid)]
        (when (not= uid @!hover)
          (let [old @!hover]
            (reset! !hover uid)
            (when old (rebuild-block! old))
            (when uid (rebuild-block! uid))))))
    (case (:phase p)
      ;; --- threshold: the press becomes a continuous gesture ---------------
      :pending
      (when (ge/drag? (:press/screen (:press p)) [sx sy] drag-threshold-px)
        (let [press    (:press p)
              decision (decide {:kind :pointer/press :phase :threshold
                                :modifiers (:press/modifiers press)
                                :hit (:press/hit press)})
              verb     (get-in decision [:decision/verb :verb/name])
              ctx      {:press press :screen [sx sy]
                        :world (vec (screen->world sx sy))}]
          (note-conflicts! decision)
          (js/console.log "[GROUND-SEL] threshold →" (str verb)
                          (str "subject=" (pr-str (:decision/subject decision)))
                          (str "tier=" (pr-str (:decision/tier decision))))
          (if (verb-registry/continuous? verb)
            (do (invoke-verb! decision :begin ctx)
                (reset! !pointer {:phase :active :press press
                                  :verb verb :decision decision}))
            ;; a discrete verb (or none) at the threshold consumes the press:
            ;; the release is inert, exactly as crossing the threshold has
            ;; always meant "this is no longer a tap"
            (do (invoke-verb! decision :invoke ctx)
                (reset! !pointer {:phase :active :press press
                                  :verb nil :decision nil})))))

      ;; --- the running gesture's continuation ------------------------------
      :active
      (invoke-verb! (:decision p) :move
                    {:press (:press p) :screen [sx sy]
                     :world (vec (screen->world sx sy))})
      nil)))

(defn pointer-up! [sx sy]
  (let [p @!pointer]
    (js/console.log "[GROUND-SEL] up" (str "phase=" (:phase p))
                    (str "verb=" (pr-str (:verb p)))
                    (str "sel=" (pr-str (ge/selection-range @!ground-edit))))
    (reset! !pointer {:phase :idle})
    (case (:phase p)
      ;; --- the press never crossed the threshold: it is a TAP --------------
      :pending
      (let [press (:press p)]
        ;; a clean click anywhere dissolves the machine + group selections.
        ;; Universal to the tap gesture, claim or no claim — not a branch.
        (clear-machine-sel!)
        (clear-group-sel!)
        ;; Scene descriptors are a generic consumer edge, not a new pointer
        ;; meaning branch. An absent/unregistered descriptor is a total no-op.
        (scene-rt/dispatch-action
         (:actions (:press/hit press))
         {:hit (:press/hit press)})
        (dispatch! {:kind :pointer/tap :phase :complete
                    :modifiers (:press/modifiers press)
                    :hit (:press/hit press) :press press
                    :screen [sx sy] :world (:press/world press)}))

      ;; --- the running gesture ends ---------------------------------------
      :active
      (invoke-verb! (:decision p) :end
                    {:press (:press p) :screen [sx sy]
                     :world (vec (screen->world sx sy))})
      nil)))

(defn handle-wheel!
  "The wheel rides the same law: normalize → pick → resolve → verb. Its row is
  on the space's code floor and its verb is floor-reserved, so no data
  revision can take the zoom away (§9.4)."
  [{:keys [x y shift?] :as wheel}]
  (dispatch! {:kind :wheel :phase :complete
              :modifiers (if shift? #{:shift} #{})
              :hit (pick-at x y)
              :wheel wheel
              :screen [x y]
              :world (vec (screen->world x y))}))

(defn handle-meta!
  "Halo P1 · H1/H2 — one contextmenu sample through the existing four
   stations. There is exactly one pick; miss still resolves against the
   outermost space claim."
  [{:keys [x y shift?]}]
  (let [hit (pick-at x y)
        world (vec (screen->world x y))]
    (scene-rt/record-pick! world hit)
    (dispatch! {:kind :pointer/meta
                :phase :complete
                :modifiers (if shift? #{:shift} #{})
                :hit hit
                :screen [x y]
                :world world})))

(defn meta-consumer
  [>meta-events]
  (->> >meta-events
       (m/reduce
        (fn [_ sample]
          (when sample (handle-meta! sample))
          nil)
        nil)))

;; ===========================================================================
;; The binding seam — window.__bindings: the served interaction table, the
;; locality-tier install point, and the malformed-bindings floor drill
;; ===========================================================================

(defn- served-facet-materials
  []
  (let [!served (get-in @!refs [:atoms :!facet-materials])]
    (:facet-materials/by-id (when !served @!served))))

;; ---------------------------------------------------------------------------
;; P6 · the PREVIEW MEMBRANE + the three scales of announcement
;; ---------------------------------------------------------------------------

(defn- re-derive-material!
  "Everything the served-material watch does, callable directly.

   The preview path MUST go through exactly this — the P3 reconcile-before-
   stamp law applies to it verbatim (T9). Derived placements and cached reply
   wraps recompute under the candidate; settled cells stay sovereign; the
   interaction table's lint recomputes on the same edge. A preview that
   re-rendered by some cheaper private route would be showing a candidate over
   state derived from the revision it is supposed to be replacing."
  []
  (if-let [ctx (:context @!world)]
    (reconcile! ctx)
    (rebuild-material-sites!))
  (refresh-material-error!)
  (recompute-binding-conflicts!))

(defn preview-candidate!
  "Render a candidate against the REAL served material with the active face
   untouched (deliverable 3 / first-light P4).

   What makes this a membrane rather than a sandbox: every OTHER facet stays
   the genuinely-served active revision, and nothing durable moves — no import,
   no pointer edit, no request. The candidate is compiled client-side against
   the real spec, so an invalid candidate is refused HERE and the land never
   renders material that could not have been activated anyway.

   Ending the preview restores the exact original served object by identity, so
   the wears cache re-derives back to the truth rather than keeping a preview
   value that no longer has a base."
  [master-id source]
  (let [spec (facet-masters/spec master-id)
        !served (get-in @!refs [:atoms :!facet-materials])
        base (when !served @!served)]
    (cond
      (nil? spec)
      {:status :refused :error :facet-master/unknown-master
       :master-id master-id}

      (nil? base)
      {:status :refused :error :facet-master/nothing-served}

      :else
      (let [compiled (facet-material/compile-source spec (str source))]
        (if-not (:valid? compiled)
          {:status :refused :error :facet-master/candidate-invalid
           :master-id master-id :errors (:errors compiled)}
          (let [active (get-in base [:facet-materials/by-id master-id])
                revision-id (str "preview:" master-id ":"
                                 (hash (str source)))
                overlay
                (assoc-in base [:facet-materials/by-id master-id]
                          (merge active
                                 {:facet-master/grammar (:grammar compiled)
                                  :facet-master/material (:material compiled)
                                  :facet-master/valid? true
                                  :facet-master/floor? false
                                  :facet-master/errors []
                                  :facet-master/active-revision-id revision-id
                                  ;; distinguishable, per G5 — a preview must
                                  ;; never be mistakable for an activation
                                  :facet-master/preview? true
                                  :facet-master/preview-of
                                  (:facet-master/active-revision-id active)}))]
            (reset! !preview {:master-id master-id
                              :revision-id revision-id
                              :base base
                              :overlay overlay})
            (re-derive-material!)
            {:status :previewing
             :master-id master-id
             :revision-id revision-id
             :preview-of (:facet-master/active-revision-id active)
             :material (:material compiled)}))))))

(defn end-preview!
  []
  (if-let [p @!preview]
    (do (reset! !preview nil)
        (re-derive-material!)
        {:status :ended :master-id (:master-id p)
         :restored-to
         (get-in (:base p) [:facet-materials/by-id (:master-id p)
                            :facet-master/active-revision-id])})
    {:status :no-preview}))

(defn preview-state
  []
  (if-let [p @!preview]
    {:previewing? true
     :master-id (:master-id p)
     :revision-id (:revision-id p)
     ;; a preview whose base has moved is STALE and no longer applied — said
     ;; out loud rather than left for the reader to discover by surprise
     :applied? (identical?
                (:base p)
                (let [!s (get-in @!refs [:atoms :!facet-materials])]
                  (when !s @!s)))}
    {:previewing? false}))

(defonce ^:private !weather (atom []))

(def ^:private weather-limit 12)

(defn- announce!
  "The three scales, from ONE change (deliverable 5).

   `:breath` is the local one — the affected appearances rebuild, which is the
   land's own way of drawing a breath at the blocks that moved. `:trace` is the
   recoverable one and always names a reversal path. `:weather` accumulates the
   ambient RecentChanges feed. All three carry the SAME `change-kind`, which is
   what keeps preview / deviation / scoped activation / canonical activation /
   rollback / recovery distinguishable at every scale instead of only the one
   the reader happens to be looking at."
  [{:keys [change-kind subjects master-id revision-id actor reversal] :as change}]
  (doseq [s (or subjects [])]
    (rebuild-block! s))
  (swap! !weather
         (fn [rows]
           (vec (take weather-limit
                      (cons {:weather/change-kind change-kind
                             :weather/master-id master-id
                             :weather/revision-id revision-id
                             :weather/actor-id (:actor/id actor)
                             :weather/subject-count (count (or subjects []))
                             :weather/at-ms (.getTime (js/Date.))}
                            rows)))))
  (assoc change :announced? true :reversal reversal))

(defn material-weather
  "The ambient feed — newest first, bounded. Client-side and session-scoped by
   design: the DURABLE weather is the served `:truth/announcements` projection,
   which is derived from the event trail and survives the page."
  []
  @!weather)


(defn- rows-under-served
  "The MASTER tier as it would resolve from a HYPOTHETICAL served map, through
   the SAME `resolved-wear` path the renderer uses — including the floored-wear
   filter `current-master-binding-rows` applies, so the drill's tier column
   reports who ACTUALLY answered. The drill measures real totality instead of a
   hand-made stand-in, and it writes nothing."
  [by-id]
  (persistent!
   (reduce-kv (fn [m facet wear]
                (assoc! m facet
                        (when-not (:facet-master/floor? wear)
                          (:facet-master/bindings wear))))
              (transient {})
              (resolve-material-wears by-id))))

(defn- drill-served-entry
  "One served entry, deliberately hostile."
  [spec kind]
  (let [master-id (:facet-master/id spec)
        facet (:facet-master/facet spec)
        grammar (:facet-master/grammar (facet-material/code-floor spec))
        material-keys (get-in spec [:facet-master/grammars grammar
                                    :material-keys])
        floor-material (select-keys (facet-material/code-floor spec)
                                    material-keys)]
    (case kind
      ;; bytes that cannot compile: `resolved-wear` must hand back the floor
      :malformed
      {:facet-master/id master-id
       :facet-master/facet facet
       :facet-master/grammar grammar
       :facet-master/active-revision-id "rev:drill:malformed-bindings"
       :facet-master/material {:drill/garbage true}}

      ;; a VALID revision that simply drops every tap row. Nothing refuses it —
      ;; and the floor tier underneath still answers the tap. This is the half
      ;; a malformed-only drill cannot prove.
      :tap-stripped
      {:facet-master/id master-id
       :facet-master/facet facet
       :facet-master/grammar grammar
       :facet-master/active-revision-id "rev:drill:tap-stripped"
       :facet-master/material
       (assoc floor-material
              :facet-master/bindings
              (into {}
                    (keep (fn [[site rows]]
                            (let [kept (filterv
                                        #(not= :pointer/tap
                                               (:binding/gesture %))
                                        rows)]
                              (when (seq kept) [site kept]))))
                    (:facet-master/bindings floor-material)))})))

(defn binding-floor-drill
  "The malformed-bindings drill, client side (the fence's proof: code-floor
   bindings unbreakable by any data revision).

   Three served worlds, one probe set, no durable write anywhere:
     :live          — what the cluster is serving now
     :absent        — the master is not served at all
     :malformed     — the active revision cannot compile
     :tap-stripped  — a VALID active revision with every tap row removed

   PASS means every probe's verb is identical across all four. The active
   revision may REBIND a gesture (that is P5's whole point); it can never make
   one disappear."
  ([] (binding-floor-drill (:facet-master/id attention-material/spec)))
  ([master-id]
   (let [spec (facet-masters/spec master-id)
         by-id (or (served-facet-materials) {})]
     (if (nil? spec)
       {:status :error :error :unknown-master :master-id master-id}
       (let [worlds
             {:live by-id
              :absent (dissoc by-id master-id)
              :malformed
              (assoc by-id master-id (drill-served-entry spec :malformed))
              :tap-stripped
              (assoc by-id master-id
                     (drill-served-entry spec :tap-stripped))}
             reports
             (into (sorted-map)
                   (map (fn [[world served]]
                          [world
                           (binding-material/drill-report
                            {:facet-rows (rows-under-served served)
                             :floor-rows floor-binding-rows
                             :instance-rows (instance-binding-rows)})]))
                   worlds)
             verbs-of #(mapv :probe/verb %)
             baseline (verbs-of (:live reports))
             deviations
             (into (sorted-map)
                   (keep (fn [[world report]]
                           (when (not= baseline (verbs-of report))
                             [world (verbs-of report)])))
                   reports)]
         {:status (if (seq deviations) :fail :pass)
          :master-id master-id
          :probes (mapv :probe/label (:live reports))
          :verbs baseline
          :tiers (into (sorted-map)
                       (map (fn [[world report]]
                              [world (mapv :probe/tier report)]))
                       reports)
          :deviations deviations
          :report (:live reports)})))))

(defn- binding-seam
  []
  #js {;; gesture × facet → verb, per-row master link (the served table's shape,
       ;; read from the same tiers the dispatch reads)
       :table (fn []
                (clj->js (binding-material/table-rows (binding-tables))))
       :verbs (fn [] (clj->js (verb-registry/declaration-rows)))
       :conflicts (fn [] (clj->js @!binding-conflicts))
       :tiers (fn [] (clj->js {:master (current-master-binding-rows)
                               :floor floor-binding-rows
                               :instance @!instance-bindings}))
       ;; the innermost locality tier — rows for ONE subject at ONE site
       :instance
       (fn [subject site rows-edn]
         (clj->js
          (try
            (let [site (keyword (str/replace (str site) #"^:" ""))
                  ;; T-R1 — bridge the JS/durable spelling to the one keyword
                  ;; subject carried by the space claim; a non-space subject
                  ;; stays itself so owner-aware legality refuses it (R3-G2c).
                  ;; This is bridge 2 of 2.
                  subject
                  (if (and (= :space/ground site)
                           (= space-material/space-subject (str subject)))
                    :space
                    subject)]
              (set-instance-bindings!
               subject site (reader/read-string (str rows-edn))))
            (catch :default e
              {:status :refused :error :binding/unreadable
               :message (.-message e)}))))
       :clearInstance (fn [] (clj->js (clear-instance-bindings!)))
       ;; ---- P6: the truth loop, from the console -----------------------
       ;; preview a candidate against the REAL served material; the active
       ;; face never moves and nothing durable is written
       :preview (fn [master-id source]
                  (clj->js (preview-candidate! (str master-id) (str source))))
       :endPreview (fn [] (clj->js (end-preview!)))
       :previewState (fn [] (clj->js (preview-state)))
       ;; the served instance tier, and what each deviant subject actually wears
       :instances
       (fn []
         (clj->js
          (:facet-materials/instances (:served @!wears-cache) {})))
       :wornBy
       (fn [subject]
         (clj->js
          (into (sorted-map)
                (map (fn [[facet wear]]
                       [facet {:tier (:facet-master/tier wear)
                               :pinned? (true? (:facet-master/pinned? wear))
                               :revision-id (:facet-master/revision-id wear)
                               :floor? (true? (:facet-master/floor? wear))}]))
                (wears-for (str subject)))))
       :weather (fn [] (clj->js (material-weather)))
       ;; the SERVER's own answer, through the generic FacePull artery. The
       ;; client's `table()` and this agreeing is the seam's proof; face-wiring
       ;; installs it before the ground exists, so the seam reaches for it here.
       :served (fn []
                 (if-let [f (.-__softland_served_bindings js/window)]
                   (f)
                   (js/Promise.reject
                    (js/Error.
                     "the served interaction table is not wired in this build"))))
       :drill (fn [master-id]
                (clj->js
                 (if (string? master-id)
                   (binding-floor-drill master-id)
                   (binding-floor-drill))))
       ;; every bindings-carrying master drilled in one call
       :drillAll
       (fn []
         (clj->js
          (into (sorted-map)
                (keep (fn [spec]
                        (let [mid (:facet-master/id spec)]
                          (when (or
                                 (= binding-material/space-facet
                                    (:facet-master/facet spec))
                                 (seq (:facet-master/bindings
                                       (facet-material/code-floor spec))))
                            [mid (binding-floor-drill mid)]))))
                facet-masters/specs)))
       :budget (fn [] (clj->js dispatch-mechanism-budget))})

;; ===========================================================================
;; Diagnostics — __ground.report(): ONE paste-able snapshot for lag reports
;; ===========================================================================

(defn- pct [xs p]
  (when (seq xs)
    (let [v (vec (sort xs))]
      (nth v (min (dec (count v)) (long (* p (count v))))))))

(defn- fmt1 [x] (if (number? x) (.toFixed x 1) "-"))

(defn- diag-report
  "Everything a lag report needs, as ONE paste-able string: echo + reconcile
   timings, sig-skip efficacy, canvas + served-page size, run phases.
   Console: copy(__ground.report()) — then paste into the session."
  []
  (let [st   @!ground-edit
        echo @!echo-samples
        rc   @!reconcile-samples
        {:keys [builds skips]} @!rebuild-stats
        bs   (:blocks @!world)
        idx  (:block-index @!world)
        ctx  @!last-raw-context]
    (str "[ground report " (.toISOString (js/Date.)) "]\n"
         "mode=" (name (:mode st)) " focus=" (pr-str (:focus st))
         " inflight=" (count (get-in st [:queue :inflight] []))
         " next-seq=" (:next-seq st)
         " selection=" (pr-str (ge/selection-range st))
         " machine-sel=" (pr-str @!machine-sel)
         " group-sel=" (pr-str @!group-sel)
         " pointer=" (name (:phase @!pointer :idle)) "\n"
         "echo ms (envelope→confirmed render, bar 52): n=" (count echo)
         " p50=" (fmt1 (pct echo 0.5)) " p95=" (fmt1 (pct echo 0.95))
         " p99=" (fmt1 (pct echo 0.99))
         " max=" (fmt1 (when (seq echo) (reduce max echo))) "\n"
         "reconcile ms (per context emission): n=" (count rc)
         " last=" (fmt1 (peek rc)) " p95=" (fmt1 (pct rc 0.95))
         " max=" (fmt1 (when (seq rc) (reduce max rc))) "\n"
         "rebuilds since boot: built=" builds " skipped=" skips "\n"
         "canvas: blocks=" (count bs)
         " machine=" (count (filter :machine? (vals bs)))
         " served-chars=" (reduce + 0 (map #(count (or (:text %) "")) (vals idx))) "\n"
         "page: river-events=" (:conversation/river-events-total ctx)
         " blocks-served=" (:conversation/blocks-returned ctx)
         " truncated?=" (boolean (:conversation/truncated? ctx))
         " dup-blocks-dropped=" (pr-str @!dup-blocks) "\n"
         "slow echoes (>52ms, last 8): "
         (pr-str (vec (take-last 8 @!echo-outliers))) "\n"
         "client stalls (main thread blocked >100ms, last 8): "
         (pr-str (vec (take-last 8 @!client-stalls))) "\n"
         "runs=" (pr-str (into {} (map (fn [[k r]] [(or k "genesis")
                                                    (name (:phase r :idle))]))
                               @!ground-runs))
         " zoom=" (fmt1 (:zoom @!camera)))))

;; ===========================================================================
;; Install (the boot seam)
;; ===========================================================================

(defn install-ground!
  "Boot the open ground: wear the conversation projection over the genesis
   episode (the pull artery — its TREE never renders; per-block slots do),
   route keys to :ground-input, and render NOTHING until the inhabitant
   points (T9: zero content pixels, zero caret, no hidden input)."
  [atoms]
  (reset! !refs {:atoms atoms})
  (reset! !ground-edit (ge/init (str "ground:" (random-uuid))))
  ;; T9 belt: the command panel boots :visible in the dev workspace — the
  ;; ground closes it before the first frame.
  (when-let [!p (:!cmd-panel atoms)] (swap! !p assoc :visible false))
  (reset! (:!face-state atoms)
          {:face :outline-face :address :episode
           ;; no :limit — the server serves its whole bounded page (clamp-limit
           ;; defaults to max-river-page-size). A client-pinned 64 is what kept
           ;; cutting settled replies off the ground: river-page reads from the
           ;; FRONT, so once the episode passed 64 events every NEW turn fell
           ;; outside the served window (streamed live, vanished at settle).
           :params (cond-> {}
                     (drill-conversation-id)
                     (assoc :drill-conversation-id (drill-conversation-id)))})
  (face-wiring/wear-face! atoms :outline-face :episode)
  (reset! (:!focus atoms) :ground-input)
  ;; narrowed truth (the §5 single-unit echo) re-renders its block — the
  ;; committed-echo cross-check channel for unfocused blocks
  (add-watch bew/!truth-overlay ::ground-truth
             (fn [_ _ old new]
               (doseq [uid (keys new)]
                 (when (and (not= (get old uid) (get new uid))
                            (get-in @!world [:blocks uid]))
                   (rebuild-block! uid)))))
  ;; P3: only served active revisions move worn surfaces. Every master arrives
  ;; through one epoch pull; invalid candidates change only the drill lint.
  ;; Reconcile before stamping the new revision: derived placements and cached
  ;; reply wraps must be recomputed under the newly served policy, while real
  ;; settled cells continue to win as instance truth.
  (when-let [!material (:!facet-materials atoms)]
    (add-watch !material ::facet-materials
               (fn [_ _ old new]
                 (when (not= (material-render-state old)
                             (material-render-state new))
                   (if-let [ctx (:context @!world)]
                     (reconcile! ctx)
                     (rebuild-material-sites!))
                   (refresh-material-error!)
                   ;; P5: an activation may change the interaction table, so
                   ;; its lint is recomputed on the same edge as the render
                   (recompute-binding-conflicts!))))
    (rebuild-material-sites!)
    (refresh-material-error!))
  (recompute-binding-conflicts!)
  (run-material-drill!)
  ;; dev observability (the __softland_atoms precedent): read-only state +
  ;; the narrow-echo samples — drives G4b console receipts, renders nothing
  (set! (.-__ground js/window)
        #js {:report    (fn [] (diag-report))
             ;; read the ACTUAL clipboard and count how many times its own
             ;; opening line repeats — judges copy by the clipboard itself,
             ;; not by any paste target's behavior
             :clip      (fn []
                          (-> (.readText (.-clipboard js/navigator))
                              (.then (fn [t]
                                       (let [probe (subs t 0 (min 60 (count t)))
                                             reps  (when (seq probe)
                                                     (loop [i 0 c 0]
                                                       (let [j (.indexOf t probe i)]
                                                         (if (neg? j) c (recur (inc j) (inc c))))))]
                                         (js/console.log "[CLIP]" (count t) "chars;"
                                                         "opening 60 chars appear" reps "time(s)"))))
                              (.catch (fn [e] (js/console.error "[CLIP] read failed" e)))))
             :echo      (fn [] (clj->js @!echo-samples))
             :camera    (fn [] (clj->js @!camera))
             ;; :w/:h are the RENDERED bounds — a fold toggle changes :h, which
             ;; is how a console receipt can see a pure-projection verb fire
             :blocks    (fn [] (clj->js (into {}
                                              (map (fn [[k v]]
                                                     [k (select-keys v [:x :y :w :h
                                                                        :machine? :local?])]))
                                              (:blocks @!world))))
             ;; P5: the pointer MACHINE's state — three phases and, while a
             ;; continuous gesture runs, the verb that claimed it
             :pointer   (fn [] (clj->js (select-keys @!pointer [:phase :verb])))
             :mode      (fn [] (name (:mode @!ground-edit)))
             :focus     (fn [] (str (:focus @!ground-edit)))
             :halo      (fn []
                          (clj->js
                           (select-keys @!halo
                                        [:subject :subject-id :masters
                                         :questions :status])))
             :confirmed (fn [] (clj->js (get-in @!ground-edit [:queue :confirmed])))
             ;; :run keeps the G4b receipt shape as the AGGREGATE phase;
             ;; :runs/:threads expose the per-thread truth
             :run       (fn [] (let [ps (set (map :phase (vals @!ground-runs)))]
                                 (name (cond (ps :streaming)  :streaming
                                             (ps :distilling) :distilling
                                             :else            :idle))))
             :runs      (fn [] (clj->js
                                (into {}
                                      (map (fn [[k r]]
                                             [(or k "genesis")
                                              (-> r
                                                  (select-keys [:phase :activity :error
                                                                :source-unit-id :turn-id])
                                                  (update :phase name))]))
                                      @!ground-runs)))
             :threads   (fn [] (clj->js
                                (into {}
                                      (map (fn [[uid tk]] [uid (or tk "genesis")]))
                                      @!thread-of)))})
  ;; P5 seam — the interaction table, the locality tiers, and the
  ;; malformed-bindings floor drill. Reads only; :instance installs
  ;; grammar-checked client-tier rows and nothing durable.
  (set! (.-__bindings js/window) (binding-seam))
  (js/console.log
   "[BINDINGS] window.__bindings installed — .table() / .verbs() /"
   " .conflicts() / .drillAll() / .instance(id, site, rows) /"
   " P6: .preview(master, edn) / .endPreview() / .instances() /"
   " .wornBy(subject) / .weather()")
  ;; exit flush — best-effort BELT; safety is the acknowledged settle write
  (js/window.addEventListener "beforeunload"
                              (fn [_] (fire-settle! :keepalive? true)))
  (js/document.addEventListener "visibilitychange"
                                (fn [_] (when (= "hidden" (.-visibilityState js/document))
                                          (fire-settle! :keepalive? true))))
  nil)

(defn ground-active? [] (some? @!refs))

(defn camera-snapshot [] @!camera)
