(ns app.server.rama.face-projection
  "Faces-as-assemblies · the projection layer + the projection registry (CONTRACT §7).

   ONE generic artery, dispatched server-side. This namespace is READ-ONLY over the
   block kernel's EXISTING query surface: it declares NO depots, NO topologies, NO
   PStates, and performs NO writes — it physically cannot mutate durable state (G12;
   trap T12). Its precedent is `trail_view.clj`: plain Clojure over kernel query APIs,
   same level (CONTRACT §2 placement ruling).

   Face dispatch lives HERE (trap T8): the Electric artery in electric_flow/file_viewer
   is generic and never grows per face — a new face registers a projection in the map
   below, it never adds a `case` branch on the client. The registry is a plain value
   `{<projection-kw> → (fn [ctx request] → data-context)}`, unit-testable without
   Electric and without IPC for its pure core (G13).

   Read discipline (block-kernel CONTRACT §8 F3/G13 split, inherited):
   - Block MATERIAL (form/text/kind/order) comes ONLY through the named block-kernel
     query composition `river-page` (which itself fans out over the query topologies
     read-common-material-for-source + read-unit). No PState paths, no new indexes.
   - The `:until-ms` SCRUB filter needs a per-block wall time, and NO block-material
     query API surfaces one (block-distiller units do not graduate → read-unit carries
     no timestamp; DerivedUnitRow has no time field). The honest, bounded source is the
     per-part `SourceArtifactRow.created-at-ms` (= the message production time, since the
     import request carries `:time-ms (:created-at-ms ctx)`), read via `ocr/read-source`
     — one point-read per DISTINCT rendered source (≤ page surfaces ≤ :limit). This is a
     FILTER/enumeration-grade read (block-kernel F3 input class), not block material, and
     it is neither a kernel edit nor a new index — so it stays inside G12 and the
     stop-clause is not triggered. Verified empirically monotone-nondecreasing across a
     real river page ⇒ `:until-ms` cuts are prefix-consistent (G11)."
  (:require [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.block-distiller :as bd]))

;; ===========================================================================
;; Limits — the projection serves ONE bounded page (block-kernel v0 discipline).
;; river-page hard-caps at bd/max-river-page-size and has NO cursor; full-corpus
;; paging is the block-kernel CONTRACT §10 scale extension, gated on a used-form
;; break (D-001) — NOT built here. `:until-ms` therefore scrubs WITHIN the first
;; page (see shape-conversation's :conversation/paging-lack note; D-005 data work).
;; ===========================================================================

(defn clamp-limit
  "Clamp a requested page limit into the block kernel's legal [1, max] window.
   river-page THROWS outside it — the projection never lets a request do that."
  [limit]
  (let [n (long (or limit bd/max-river-page-size))]
    (-> n (max 1) (min bd/max-river-page-size))))

;; ===========================================================================
;; Pure core (no IPC) — blocks-with-time → turns → data-context.
;; Directly unit-testable with synthetic river-page output (G13).
;; ===========================================================================

(defn blocks->turns
  "Group ordered river blocks into TURNS by :event-uuid, preserving river order
   (turn order = event-order; block order within a turn = river-page order). One
   turn per event; :speaker = the event's resolved actor (T1 role≠actor already
   handled by the block kernel). PURE — the block vector is river-page's output.

   Each block keeps its own :id (unit-id — drives :each node ids, trap T6),
   :kind (unit-kind/form), :text, :order (the [event-order part-index order-key]
   vector), and :time-ms (created-at-ms, attached by conversation-projection)."
  [blocks]
  (->> blocks
       (reduce
        (fn [{:keys [order->idx turns] :as acc} b]
          (let [euid    (:event-uuid b)
                block   {:id       (:unit-id b)
                         :kind     (:form b)
                         :text     (:text b)
                         :order    (:order b)
                         :time-ms  (:time-ms b)
                         :part-path (:part-path b)
                         :block-path (:block-path b)}]
            (if-let [idx (get order->idx euid)]
              (update-in acc [:turns idx :blocks] conj block)
              (let [idx (count turns)]
                (-> acc
                    (assoc-in [:order->idx euid] idx)
                    (update :turns conj
                            {:id     euid
                             :speaker (:actor b)
                             :order  (first (:order b))
                             :blocks [block]}))))))
        {:order->idx {} :turns []})
       :turns))

(defn apply-until-ms
  "Replay/scrub cut (CONTRACT §7). Keep the prefix of blocks (river order) whose
   EFFECTIVE time <= until-ms, drop turns left empty. PURE. Prefix-consistent BY
   CONSTRUCTION (G16 falsification fix): the cut compares against a running-max
   effective time over river order — a block with a missing/zero/backwards
   :time-ms inherits the floor of everything before it (the causal truth for a
   linear river), so no cut can include a later block while excluding an earlier
   one (G11 — T1 < T2 ⇒ result(T1) is a prefix of result(T2), for ANY data).
   On clean monotone data (the 7c80ce2a corpus) eff = own time — identical
   behavior. nil until-ms = no cut (the live/whole page)."
  [turns until-ms]
  (if (nil? until-ms)
    turns
    (let [cut (long until-ms)]
      (loop [ts turns, floor 0, out []]
        (if-let [turn (first ts)]
          (let [[kept floor']
                (reduce (fn [[acc fl] b]
                          (let [eff (max fl (long (or (:time-ms b) 0)))]
                            [(if (<= eff cut) (conj acc b) acc) eff]))
                        [[] floor]
                        (:blocks turn))]
            (recur (rest ts) floor'
                   (if (seq kept) (conj out (assoc turn :blocks kept)) out)))
          out)))))

(defn shape-conversation
  "Assemble the data-context (CONTRACT §7) from time-stamped river blocks + the
   river-page read-plan + the request. PURE. Top-level keys are the face's data
   contract (the assembly's :bind paths read `[:turns]`, then `[:speaker]` per turn,
   `[:blocks]`, `[:kind]`/`[:text]` per block); the `:conversation/*` metadata is for
   the host flow (truncation, totals, read-plan), not the assembly binds.

   Honesty (map-must-not-lie): `:conversation/truncated?` carries river-page's page
   ceiling (more durable material than this page shows); `:conversation/river-events-total`
   is the TRUE durable river-event count (247 on the 7c80ce2a corpus), never the page
   size; debris is excluded BY DESIGN (the projection serves river) and is reported as
   excluded, never dropped silently."
  [{:keys [blocks read-plan address limit until-ms rendered-at-ms]}]
  (let [turns   (-> blocks blocks->turns (apply-until-ms until-ms))
        n-blocks (reduce + 0 (map (comp count :blocks) turns))]
    {:turns turns
     :conversation/address            address
     :conversation/limit              limit
     :conversation/until-ms           until-ms
     :conversation/river-events-total (:river-events-total read-plan)
     :conversation/blocks-returned    n-blocks
     :conversation/truncated?         (:truncated? read-plan)
     :conversation/page-complete?     (:page-complete? read-plan)
     ;; The projection serves RIVER only; debris (the transcript's harness/meta
     ;; rows) is retained upstream and deliberately absent here — reported, not lost.
     :conversation/debris-excluded?   true
     ;; v0 lack, logged as ordered data work (D-005): `:until-ms` scrubs within the
     ;; first bounded page; full-conversation scrub needs river-page cursor paging
     ;; (block-kernel CONTRACT §10 scale extension), built only on a used-form break.
     :conversation/paging-lack        (when (:truncated? read-plan)
                                        :river-page-has-no-cursor)
     :conversation/read-plan          read-plan
     :face/rendered-at-ms             rendered-at-ms}))

;; ===========================================================================
;; IPC-backed :conversation projection — river-page composition + until-ms times.
;; ===========================================================================

(defn- attach-source-times
  "Attach :time-ms (per-part SourceArtifactRow.created-at-ms) to each block, reading
   ONE point per DISTINCT source (≤ page surfaces ≤ :limit). Bounded per-page cost,
   never conversation length (G11). Missing source ⇒ time 0 (honest absence, never a
   throw)."
  [oc-rt blocks]
  (let [src-ids (distinct (map :source-id blocks))
        times   (into {}
                      (map (fn [sid] [sid (:created-at-ms (ocr/read-source oc-rt sid))]))
                      src-ids)]
    (mapv (fn [b] (assoc b :time-ms (long (or (get times (:source-id b)) 0)))) blocks)))

(defn conversation-projection
  "The ONE Wave-1 projection: conversation → turns → blocks-with-kinds, over the
   block kernel's durable state via `river-page` (READ-ONLY). `ctx` carries the OC
   runtime; `request` is the CONTRACT §7 shape {:face :address :params :epoch}, where
   :address is the conversation object-key and :params {:limit :until-ms}. Returns the
   §7 data-context (never throws — a bad address yields an error data-context)."
  [{:keys [oc-rt]} {:keys [address params] :as request}]
  (let [limit    (clamp-limit (:limit params))
        until-ms (:until-ms params)]
    (if (nil? address)
      {:turns []
       ;; the map must not lie: a first-light runtime still distilling (or
       ;; failed) is a DIFFERENT reality than a genuinely blank request —
       ;; resolve-request (file_viewer) stamps the hint (G16 falsification fix)
       :conversation/error (:face/first-light request :missing-address)
       :conversation/river-events-total 0
       :conversation/blocks-returned 0
       :conversation/truncated? false
       :face/rendered-at-ms (System/currentTimeMillis)}
      (let [page      (bd/river-page {:oc-rt oc-rt :object-key address} limit)
            read-plan (:river-page/read-plan (meta page))
            blocks    (attach-source-times oc-rt page)]
        (shape-conversation {:blocks         blocks
                             :read-plan      read-plan
                             :address        address
                             :limit          limit
                             :until-ms       until-ms
                             :rendered-at-ms (System/currentTimeMillis)})))))

;; ===========================================================================
;; The projection registry + server-side face dispatch (trap T8).
;; ===========================================================================

(def projection-registry
  "Plain value: {<projection-kw> → (fn [ctx request] → data-context)}. Wave 1
   registers ONE projection. Extensible by adding an entry — never by an Electric
   `case`. Persisted form (Wave 2, schema §8) is keyword + code address, never fn
   values (trap T5)."
  {:conversation conversation-projection})

(def face->projection-kind
  "Server-side face → projection map (v0). Dispatch lives here, NOT in Electric
   (trap T8). New faces add an entry; the Electric surface never changes. Identity
   fallback: a request whose :face already names a registered projection routes
   straight through (so the client may address a projection by name)."
  {:outline      :conversation
   "outline-face" :conversation
   :conversation :conversation})

(defn error-data-context
  "Honest projection-side error (mirrors §4 error-card totality on the render side):
   a valid data-context that names the failure, never a throw into the pull."
  [request reason]
  {:turns []
   :conversation/error reason
   :conversation/requested-face (:face request)
   :conversation/river-events-total 0
   :conversation/blocks-returned 0
   :conversation/truncated? false
   :face/rendered-at-ms (System/currentTimeMillis)})

(defn serve
  "The artery's server entrypoint (CONTRACT §7): resolve the projection for a request
   and run it. Generic — the Electric side calls ONLY this, with the whole request; all
   per-face routing is the plain-map lookup below. Total: an unknown face returns an
   error data-context, never a throw (the render loop has no `try`, L13).

   Two arities: `(serve ctx request)` uses the module registry; `(serve registry ctx
   request)` injects one (the G13 dispatch unit test passes a stub registry — no IPC)."
  ([ctx request] (serve projection-registry ctx request))
  ([registry ctx request]
   (let [face (:face request)
         pkw  (get face->projection-kind face face)
         pfn  (get registry pkw)]
     (if pfn
       ;; totality is load-bearing (L13: no `try` upstream in the render
       ;; loop): corrupt/oversized durable state makes `river-page` throw
       ;; (scan guard, missing input surface) — that must land as an honest
       ;; error data-context, never a throw into the pull (G16 falsification
       ;; fix; the docstring's promise, now actually kept).
       (try
         (pfn ctx request)
         (catch Throwable t
           (println "[FACE] projection read failed:" (.getMessage t))
           (error-data-context request :projection-read-failed)))
       (error-data-context request :unknown-projection)))))
