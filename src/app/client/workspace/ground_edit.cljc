(ns app.client.workspace.ground-edit
  "first-light P2b — the open ground's PURE edit state machine.

   The committed-echo law (CONTRACT §7 P2b typing truth, receipt P2B.md §b):
   visible text and the key-induced caret derive from ONE confirmed value —
   the last acknowledged revision. The queue below is an INVISIBLE ordered
   intent queue: it exists SOLELY to construct and sequence idempotent edit
   envelopes (each keystroke's envelope is built on the queue's projected
   text, since envelopes carry whole content-text) — it is never a rendered
   text source. Block-write's painted-pending (block_edit.cljc :buffer
   rendering inside the focused block) is the pattern this REPLACES.

   TYPING-LAG PATCH (deliberate, reversible, NOT the settled answer): the
   law above made every glyph wait for a durable ack, so typing rendered at
   round-trip latency and — because the outbox conflates — in jumps.
   block-view now takes an `echo-mode`: :confirmed keeps the law verbatim
   (still the default, still tested), :optimistic renders the queue HEAD
   instead of its tail. The queue, the envelopes, the seq law and the
   rebase-on-refusal are all unchanged; only the choice of which end of the
   ONE value is painted moves. ground.cljs passes :optimistic. The real fix
   is a local-first edit model, not a render-source keyword.

   Birth lifecycle (moment 1): :rest → :anchor (click / type-without-click;
   ephemeral, Escape leaves NOTHING) → :birthing (the FIRST content act
   posts the durable mint; keys during flight queue as intent) → :editing
   (unit-id known; confirmed = the acked birth text; queued intent replays
   as envelopes seq 0…).

   Envelope identity reuses block-write's own op-id law verbatim
   (be/mint-envelope — one law, no drift). One session-wide monotonic seq
   satisfies the kernel's per-(target, client-id) staleness monotonicity.

   PURE .cljc: no atoms, no I/O, no clock, no randomness — block-id /
   turn-id / client-id are minted by the caller (ground.cljs)."
  (:require [clojure.string :as str]
            [app.client.workspace.block-edit :as be]))

;; ============================================================================
;; State
;; ============================================================================

(defn init
  "Fresh ground-edit session. `edit-client-id` minted fresh per session by
   the caller (BW-T8: a resumed old client-id with a reset seq is the
   :edit/stale storm; fresh id per session makes the reset harmless)."
  [edit-client-id]
  {:mode           :rest   ; :rest | :anchor | :birthing | :editing
   :anchor         nil     ; {:x :y} world point (ephemeral — never durable)
   :birth          nil     ; {:block-id :text :caret :pos :queue [events]}
   :focus          nil     ; focused unit-id (:editing)
   :queue          nil     ; {:confirmed {:text :caret :seq} :inflight [..]}
   :selection      nil     ; {:anchor :head} caret span over confirmed (Task 11)
   :refusal        nil     ; {:unit-id :reason} — visible, at the block
   :edit-client-id edit-client-id
   :next-seq       0})

(declare caret->line-col line-col->caret)

(def ^:private paste-marker-prefix "⟦softland-paste:v0:")
(def ^:private paste-marker-suffix "⟧")

(defn- string-index-of
  [s needle from]
  #?(:clj (.indexOf ^String s ^String needle (int from))
     :cljs (.indexOf s needle from)))

(defn- string-last-index-of
  [s needle from]
  #?(:clj (.lastIndexOf ^String s ^String needle (int from))
     :cljs (.lastIndexOf s needle from)))

(defn- parse-long-total
  [s]
  (try
    #?(:clj (Long/parseLong s)
       :cljs (js/parseInt s 10))
    (catch #?(:clj Throwable :cljs :default) _ nil)))

(defn paste-decision
  "The paste path's ONE material consult. Outside text over the worn threshold
   keeps every byte in the durable edit, but gains a source-marked fold header
   and a marker carrying the material-decided collapsed extent. There are no
   clamp constants here: missing/malformed policy means verbatim paste."
  [text {:keys [threshold-chars max-share header-copy]} source-mark]
  (let [text (str text)
        n (count text)
        valid? (and (number? threshold-chars)
                    (pos? threshold-chars)
                    (number? max-share)
                    (pos? max-share)
                    (<= max-share 1)
                    (string? header-copy)
                    (seq header-copy))]
    (if (and valid? (> n threshold-chars))
      (let [shown (max 1
                       (min n
                            (long #?(:clj (Math/floor (* n max-share))
                                     :cljs (js/Math.floor (* n max-share))))))
            copy (str/replace header-copy #"\s+" " ")
            source (str/replace
                    (if (keyword? source-mark)
                      (name source-mark)
                      (str (or source-mark "outside")))
                    #"\s+" "-")
            header (str "▸ " copy " · " source " · " n
                        " chars — showing " shown)
            marker (str paste-marker-prefix n ":" shown paste-marker-suffix)]
        {:insert-text (str header " " marker "\n" text)
         :paste/clamped? true
         :paste/source-mark source
         :paste/total-chars n
         :paste/shown-chars shown})
      {:insert-text text
       :paste/clamped? false
       :paste/source-mark (if (keyword? source-mark)
                            (name source-mark)
                            (str (or source-mark "outside")))
       :paste/total-chars n
       :paste/shown-chars n})))

(defn paste-projection
  "Project the first durable paste envelope in `text`. The complete body stays
   in source; collapsed display substitutes only the material-decided prefix.
   `nil` means ordinary text. The existing fold verb supplies `expanded?`."
  [text expanded?]
  (let [text (str text)
        marker-at (string-index-of text paste-marker-prefix 0)]
    (when (not (neg? marker-at))
      (let [marker-end (string-index-of text paste-marker-suffix marker-at)
            line-end (when (not (neg? marker-end))
                       (string-index-of text "\n" marker-end))
            line-start (inc (string-last-index-of text "\n" marker-at))
            payload (when (and marker-end (not (neg? marker-end)))
                      (subs text
                            (+ marker-at (count paste-marker-prefix))
                            marker-end))
            [total-s shown-s] (str/split (or payload "") #":" 2)
            total (parse-long-total total-s)
            shown (parse-long-total shown-s)]
        (when (and line-end (not (neg? line-end))
                   (number? total) (number? shown)
                   (<= 0 shown total)
                   (<= (+ (inc line-end) total) (count text)))
          (let [header (str/trim
                        (subs text line-start marker-at))
                body-start (inc line-end)
                body-end (+ body-start total)
                body (subs text body-start body-end)
                visible (if expanded? body (subs body 0 shown))
                header (str (if expanded? "▾" "▸")
                            (subs header (min 1 (count header))))]
            {:paste/header header
             :paste/body
             (str (subs text 0 line-start)
                  visible
                  (subs text body-end))
             :paste/expanded? (boolean expanded?)
             :paste/total-chars total
             :paste/shown-chars shown}))))))

(defn apply-ground-keydown
  "block-write's apply-keydown extended with the ground's grain: :paste
   (insert at caret; a content act) and :up/:down (caret line moves over
   multi-line blocks — caret-only, no envelope)."
  [{:keys [text caret] :as tc} {:keys [type] :as ev}]
  (let [text  (or text "")
        caret (max 0 (min (long (or caret 0)) (count text)))]
    (case type
      :paste (let [decision (paste-decision
                             (:text ev)
                             (:paste-policy ev)
                             (:paste/source-mark ev))
                   t (:insert-text decision)]
               (when (seq t)
                 (merge
                  decision
                  {:op :edit
                   :new-text (str (subs text 0 caret) t (subs text caret))
                   :new-caret (+ caret (count t))})))
      (:up :down)
      (let [{:keys [line col]} (caret->line-col text caret)
            line' (if (= type :up) (dec line) (inc line))]
        (when (>= line' 0)
          {:op :caret :new-caret (line-col->caret text line' col)}))
      (be/apply-keydown tc ev))))

;; ============================================================================
;; Anchor + focus
;; ============================================================================

(defn set-anchor
  "Click on empty ground (or type-without-click): an ephemeral caret anchor
   at the chosen world point. Blurs any focused block."
  [st world-pos]
  (assoc st :mode :anchor :anchor world-pos
         :birth nil :focus nil :queue nil :refusal nil))

(defn escape
  "Escape: an :anchor dies leaving NOTHING (no unit was ever minted); an
   :editing focus blurs (the queue dies with it — confirmed was acked truth,
   so nothing is lost). :birthing is NOT abandoned — the content act already
   happened; escape just stops routing keys to it."
  [st]
  (case (:mode st)
    :anchor   (assoc st :mode :rest :anchor nil :refusal nil)
    :editing  (assoc st :mode :rest :focus nil :queue nil :refusal nil
                     :selection nil)
    :birthing (assoc st :mode :rest :refusal nil)
    (assoc st :refusal nil)))

(defn focus-block
  "Enter a block: confirmed seeds from materialized truth (never a buffer).
   `caret` clamps into the truth text (click position → caret index)."
  [st unit-id truth-text caret]
  (let [txt (or truth-text "")]
    (assoc st :mode :editing :focus unit-id :anchor nil :birth nil
           :selection nil
           :queue {:confirmed {:text txt
                               :caret (max 0 (min (long (or caret (count txt)))
                                                  (count txt)))
                               :seq -1}
                   :inflight []}
           :refusal nil)))

(defn blur [st] (escape (assoc st :mode :editing)))

;; ============================================================================
;; Birth (moment 1 — the first content act mints the durable block)
;; ============================================================================

(defn begin-birth
  "First content act at an anchor: → :birthing. `block-id` is caller-minted
   ONCE (retries converge server-side). Returns {:state :post} where :post
   is the birth mint the caller sends ({:block-id :text :pos})."
  [st keydown block-id]
  (when (= :anchor (:mode st))
    (when-let [r (apply-ground-keydown {:text "" :caret 0} keydown)]
      (when (= :edit (:op r))
        {:state (assoc st :mode :birthing :anchor nil
                       :birth {:block-id block-id
                               :text (:new-text r) :caret (:new-caret r)
                               :pos (:anchor st) :queue []})
         :post {:block-id block-id :text (:new-text r) :pos (:anchor st)}}))))

(defn birth-key
  "A key during birth flight: queue it as intent (replayed on the ack).
   Never rendered — the committed-echo law holds through birth."
  [st keydown]
  (if (= :birthing (:mode st))
    (update-in st [:birth :queue] conj keydown)
    st))

(declare input)

(defn birth-acked
  "The birth ack landed: the unit exists, confirmed = the acked birth text.
   Replays the queued intent through the normal input path — each content
   key mints one envelope (seq 0…). Returns {:state :envelopes}."
  [st unit-id block-info object-key]
  (if-not (= :birthing (:mode st))
    {:state st :envelopes []}
    (let [b  (:birth st)
          st (assoc st :mode :editing :focus unit-id :birth nil
                    :queue {:confirmed {:text (:text b) :caret (:caret b) :seq -1}
                            :inflight []})]
      (reduce (fn [{:keys [state envelopes]} ev]
                (let [r (input state ev block-info object-key)]
                  {:state (:state r)
                   :envelopes (cond-> envelopes (:envelope r) (conj (:envelope r)))}))
              {:state st :envelopes []}
              (:queue b)))))

(defn birth-failed
  "The birth mint failed: back to :rest with a visible refusal. The typed
   intent does not survive (honest visible loss, never silent)."
  [st reason]
  (assoc st :mode :rest :birth nil :anchor nil
         :refusal {:unit-id nil :reason (or reason :birth-failed)}))

;; ============================================================================
;; Typing (the invisible intent queue — receipt P2B.md §b)
;; ============================================================================

(defn projection
  "The queue's head text/caret — what the NEXT envelope must be built on
   (envelopes carry whole content-text).

   Under the committed-echo law this is NEVER rendered. Under the
   :optimistic echo mode (block-view's 4-arity, the ground's typing-lag
   patch) it IS the render source: one value carrying BOTH text and caret,
   so the caret law (no second signal to glitch against) still holds —
   the value is simply the head of the queue instead of its tail."
  [{:keys [queue]}]
  (or (peek (:inflight queue)) (:confirmed queue)))

(defn- displayed-text
  "The text the eye and the clipboard read: the projection's. Identical to
   confirmed whenever nothing is in flight, which is every at-rest moment."
  [st]
  (or (:text (projection st)) ""))

(defn input
  "One keystroke on the focused block. Content keys mint ONE envelope built
   on the projection and push it in-flight; caret keys move the caret INSIDE
   confirmed (clamped — text and caret stay one value). Returns
   {:state :envelope|nil}."
  [st keydown block-info object-key]
  (if-not (and (= :editing (:mode st)) (:focus st))
    {:state st :envelope nil}
    (let [proj (projection st)]
      (if-let [r (apply-ground-keydown {:text (:text proj) :caret (:caret proj)} keydown)]
        (case (:op r)
          :caret
          (let [move (fn [{:keys [text] :as e}]
                       (assoc e :caret (max 0 (min (long (:new-caret r))
                                                   (count (or text ""))))))]
            {:state (-> st
                        (assoc :selection nil)
                        (update :queue
                                (fn [q]
                                  ;; The caret moves on BOTH ends of the queue.
                                  ;; confirmed: the committed-echo render source.
                                  ;; in-flight HEAD: the base the next envelope is
                                  ;; built on AND the :optimistic render source —
                                  ;; without this an arrow key pressed mid-flight
                                  ;; was dropped twice over (the next envelope
                                  ;; inserted at the stale caret, and on-decision
                                  ;; overwrote confirmed's caret with the entry's
                                  ;; on every ack). Each entry keeps its own
                                  ;; (text, caret) coherent — no tear either way.
                                  (cond-> (update q :confirmed move)
                                    (seq (:inflight q))
                                    (update-in [:inflight (dec (count (:inflight q)))]
                                               move)))))
             :envelope nil})
          :edit
          (let [seq' (:next-seq st)
                env  (be/mint-envelope {:block          block-info
                                        :object-key     object-key
                                        :content-text   (:new-text r)
                                        :edit-client-id (:edit-client-id st)
                                        :edit-seq       seq'})]
            {:state (-> st
                        (update :next-seq inc)
                        (assoc :refusal nil :selection nil)
                        (update-in [:queue :inflight]
                                   (fn [q]
                                     (conj (if (>= (count q) 64) (subvec q 1) q)
                                           {:seq seq' :request-id (:request-id env)
                                            :text (:new-text r) :caret (:new-caret r)}))))
             :envelope env}))
        {:state st :envelope nil}))))

(defn on-decision
  "The durable decision for an in-flight envelope.
   Accepted seq N → confirmed := the queued (text, caret, N); every entry
   ≤ N retires (Electric conflation skips intermediates — their decisions
   never come; an accepted later seq heals the queue).
   Rejected → the ENTIRE in-flight queue drops (it was built on a dead
   base — this IS the rebase onto the last confirmed revision) and the
   refusal explains itself visibly at the block."
  [st request-id decision]
  (let [q (:queue st)
        entry (some #(when (= request-id (:request-id %)) %) (:inflight q))]
    (cond
      (nil? q) st
      (nil? entry) st
      (= :accepted (:status decision))
      (update st :queue
              (fn [q]
                {:confirmed {:text (:text entry) :caret (:caret entry)
                             :seq (:seq entry)}
                 :inflight (vec (remove #(<= (:seq %) (:seq entry))
                                        (:inflight q)))}))
      :else
      (-> st
          (assoc-in [:queue :inflight] [])
          (assoc :refusal {:unit-id (:focus st)
                           :reason (or (:reason decision) :edit-refused)})))))

(defn adopt-truth
  "Cross-check channel: narrowed/served truth for the FOCUSED block, adopted
   only when nothing is in flight (truth wins at rest; normally
   byte-identical to confirmed by construction)."
  [st unit-id truth-text]
  (if (and (= :editing (:mode st))
           (= unit-id (:focus st))
           (empty? (get-in st [:queue :inflight]))
           (some? truth-text)
           (not= truth-text (get-in st [:queue :confirmed :text])))
    (update-in st [:queue :confirmed]
               (fn [c] {:text truth-text
                        :caret (min (:caret c) (count truth-text))
                        :seq (:seq c)}))
    st))

;; ============================================================================
;; Selection (Task 11 — attention-state: never settled, never restored)
;; ============================================================================

(defn begin-select
  "Press on the FOCUSED block arms a text selection at `caret`; drag extends
   it (§9.4 amendment: attention-in makes the drag textual — a focused block
   moves only after blur). Lives over CONFIRMED text only — the one value
   the eye and the clipboard may read."
  [st caret]
  (if (= :editing (:mode st))
    (let [n (count (displayed-text st))
          c (max 0 (min (long (or caret 0)) n))]
      (assoc st :selection {:anchor c :head c}))
    st))

(defn extend-select
  "Drag moves the selection head (the anchor stays)."
  [st caret]
  (if (and (= :editing (:mode st)) (:selection st))
    (let [n (count (displayed-text st))
          c (max 0 (min (long (or caret 0)) n))]
      (assoc-in st [:selection :head] c))
    st))

(defn selection-range
  "Normalized [start end] over the DISPLAYED text, or nil when
   collapsed/absent (clamped — adopt-truth may shrink the text under a live
   selection). Displayed = confirmed at rest, the projection while in flight,
   so the highlight always spans the string actually on screen."
  [st]
  (when-let [{:keys [anchor head]} (and (= :editing (:mode st)) (:selection st))]
    (let [n (count (displayed-text st))
          s (min (min anchor head) n)
          e (min (max anchor head) n)]
      (when (< s e) [s e]))))

(defn selection-text
  "The selected substring of the DISPLAYED text, or nil — what the eye sees is
   what the clipboard gets."
  [st]
  (when-let [[s e] (selection-range st)]
    (subs (displayed-text st) s e)))

;; ============================================================================
;; Render derivation (ONE value — no tear)
;; ============================================================================

(defn block-view
  "Render source for ONE block: the focused block renders ONE queue value
   (text + caret together — no second signal, no tear); every other block
   renders `truth-text`, caret nil. The refusal rides along when addressed
   here.

   `echo-mode` picks WHICH end of the queue that one value comes from:
   - :confirmed (default) — the committed-echo law: the last acknowledged
     revision. Nothing typed is visible until the durable decision lands.
   - :optimistic — the projection (queue head). Keystrokes paint on the
     spot; the queue behind them still sequences envelopes exactly as
     before, and a REJECTION drops the in-flight queue, so the block
     visibly rewinds to the last acknowledged revision with the refusal
     showing. That visible rewind is the whole price of the mode: the
     committed-echo law bought its absence with the typing latency.

   The 3-arity keeps the law intact for every existing caller and for the
   committed-echo-render-law test; ground.cljs opts into :optimistic at its
   one call site, so the flip is one keyword to reverse."
  ([st unit-id truth-text] (block-view st unit-id truth-text :confirmed))
  ([st unit-id truth-text echo-mode]
   (let [focused? (and (= :editing (:mode st)) (= unit-id (:focus st)) (:queue st))
         src      (if (= :optimistic echo-mode)
                    (projection st)
                    (get-in st [:queue :confirmed]))]
     (cond-> (if focused?
               {:unit-id unit-id
                :text  (:text src)
                :caret (:caret src)
                :selection (selection-range st)
                :focused? true}
               {:unit-id unit-id :text (or truth-text "") :caret nil :focused? false})
       (= unit-id (:unit-id (:refusal st)))
       (assoc :refusal (:reason (:refusal st)))))))

;; ============================================================================
;; Geometry helpers (pure)
;; ============================================================================

(defn caret->line-col
  "Caret index → {:line :col} over the text's lines."
  [text caret]
  (let [before (subs (or text "") 0 (max 0 (min (long (or caret 0))
                                                (count (or text "")))))
        lines  (str/split before #"\n" -1)]
    {:line (dec (count lines)) :col (count (last lines))}))

(defn line-col->caret
  "{:line :col} → caret index, clamped into the text."
  [text line col]
  (let [lines (str/split (or text "") #"\n" -1)
        line  (max 0 (min (long line) (dec (count lines))))
        col   (max 0 (min (long col) (count (nth lines line))))]
    (+ (reduce + 0 (map #(inc (count %)) (take line lines))) col)))

(defn drag?
  "The ONE screen-space threshold (~4 CSS px, zoom-independent) splitting
   click/caret from pan/drag (§9.4 pointer grammar)."
  [[x0 y0] [x1 y1] threshold]
  (let [dx (- x1 x0) dy (- y1 y0)]
    (> (+ (* dx dx) (* dy dy)) (* threshold threshold))))
