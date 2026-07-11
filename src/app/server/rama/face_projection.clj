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
  (:require [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.object-container.assembly-adapter :as assembly-adapter]
            [app.server.rama.face-arsenal :as face-arsenal]))

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
  [{:keys [blocks read-plan address limit until-ms focus-turn rendered-at-ms]}]
  (let [turns   (-> blocks blocks->turns (apply-until-ms until-ms))
        n-blocks (reduce + 0 (map (comp count :blocks) turns))
        ;; W2-INT (G25; D-010 revert-cheap): the reader-pane data contract —
        ;; :params {:focus-turn <order>} picks the focused turn, default =
        ;; the FIRST served turn (reading order; deterministic under paging).
        ;; Additive key; faces without a reader ignore it. Focus CONTROL
        ;; (click/scroll-linked) is :actions-era — only the data serve lands.
        reader   (or (when focus-turn
                       (first (filter #(= focus-turn (:order %)) turns)))
                     (first turns))]
    {:turns turns
     :reader-turn reader
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
                             :focus-turn     (:focus-turn params)
                             :rendered-at-ms (System/currentTimeMillis)})))))

;; ===========================================================================
;; W2 arsenal projections (CONTRACT §16; gates G20/G21). READ-ONLY + TOTAL:
;; reads go ONLY through named OC query APIs (read-current-revision) + the
;; arsenal's OWN named read fns (read-face / list-faces / read-wear-count) —
;; no PState paths here (trap T15: no write can reach serve; G21's law).
;;
;; Routing note (worked at lane W2-D): the asm:<name> object-key carries a
;; colon, so `read-source`/`read-latest-source-by-ref` MISROUTE for assembly
;; objects (leading-object-key truncates at "asm"). The routing-correct
;; existing API carrying the same bytes is `read-current-revision` over the
;; document container `oc:doc:asm:<name>` (extract-object-key's full-remainder
;; oc:doc: branch; the revision hop is task-local) — the wear-time source read.
;; ===========================================================================

(defn assembly-projection
  "Wear-time source serve (§16): face NAME → OC source → verdict. The
   object-key derives from the NAME deterministically (§17), so a face present
   in OC but missing from the arsenal index is STILL servable — the index gap
   is named in the data (:assembly/indexed? false), never invented around
   (G20/T17 honest degradation). The verdict is recomputed with the SAME .cljc
   compiler + registry the client wears and the adapter used at ingest (trap
   T18: one compiler, verdicts equal by construction). Total: any read failure
   or absent face yields an error-shaped data-context, never a throw."
  [{:keys [oc-rt arsenal-rt]} {:keys [address] :as request}]
  (let [face-name (some-> address str)
        now (System/currentTimeMillis)]
    (if (or (nil? face-name) (not (assembly-adapter/valid-assembly-name? face-name)))
      {:assembly/name face-name
       :assembly/found? false
       :assembly/valid? false
       :assembly/errors [{:type :assembly/bad-name :value address}]
       :face/rendered-at-ms now}
      (let [object-key (assembly-adapter/assembly-object-key face-name)
            document-id (oc/document-id-for-object-key object-key)
            row (when arsenal-rt
                  (try (face-arsenal/read-face arsenal-rt face-name)
                       (catch Throwable _ nil)))
            revision (when oc-rt
                       (try (ocr/read-current-revision oc-rt document-id)
                            (catch Throwable _ nil)))]
        (if (nil? revision)
          {:assembly/name face-name
           :assembly/object-key object-key
           :assembly/found? false
           :assembly/valid? false
           :assembly/errors [{:type :assembly/not-found :object-key object-key}]
           ;; T17's gap window named in the data, both directions honest:
           ;; indexed-but-unreadable and unindexed-and-absent look different.
           :assembly/indexed? (some? row)
           :face/rendered-at-ms now}
          (let [source (:content-text revision)
                verdict (assembly-adapter/validate-assembly-source source)]
            {:assembly/name face-name
             :assembly/object-key object-key
             :assembly/found? true
             :assembly/source source
             :assembly/valid? (:valid? verdict)
             :assembly/errors (:errors verdict)
             :assembly/status (:status row)
             :assembly/indexed? (some? row)
             :assembly/source-hash (:content-hash revision)
             :assembly/revised-at-ms (:created-at-ms revision)
             :face/rendered-at-ms now}))))))

(defn face-list-projection
  "The arsenal roster (§16): names + status + wear counts + last-worn — read
   from RAMA, never the faces directory (trap T14: a file whose import failed
   must not list as wearable; :valid? carries that honestly). Total: a missing/
   corrupt arsenal yields an error-shaped data-context with an empty list,
   never a throw."
  [{:keys [arsenal-rt]} _request]
  (let [now (System/currentTimeMillis)]
    (if (nil? arsenal-rt)
      {:faces []
       :face-list/count 0
       :face-list/error :arsenal-unavailable
       :face/rendered-at-ms now}
      (try
        (let [rows (face-arsenal/list-faces arsenal-rt)
              faces (mapv (fn [row]
                            (let [cnt (try (face-arsenal/read-wear-count
                                            arsenal-rt (:face-name row))
                                           (catch Throwable _ nil))]
                              {:name (:face-name row)
                               :status (:status row)
                               :valid? (:valid? row)
                               :object-key (:object-key row)
                               :wear-count (long (or (:wear-count cnt) 0))
                               :last-worn-ms (:last-worn-ms cnt)}))
                          rows)]
          {:faces faces
           :face-list/count (count faces)
           :face/rendered-at-ms now})
        (catch Throwable t
          (println "[FACE] face-list read failed:" (.getMessage t))
          {:faces []
           :face-list/count 0
           :face-list/error :arsenal-read-failed
           :face/rendered-at-ms now})))))

;; ===========================================================================
;; The projection registry + server-side face dispatch (trap T8).
;; ===========================================================================

(def projection-registry
  "Plain value: {<projection-kw> → (fn [ctx request] → data-context)}. Wave 1
   registered ONE projection; W2 adds the two arsenal reads (:assembly wear-time
   source serve + :face-list roster). Extensible by adding an entry — never by an
   Electric `case`. Persisted form (Wave 2, schema §8) is keyword + code address,
   never fn values (trap T5)."
  {:conversation conversation-projection
   :assembly     assembly-projection
   :face-list    face-list-projection})

(def face->projection-kind
  "Server-side face → projection map (v0 static entries). Dispatch lives here,
   NOT in Electric (trap T8). W2 (G26 fix, 2026-07-11): faces are no longer
   added HERE — a face registered in the arsenal roster routes to
   :conversation by default (resolve-projection-kind below); this map keeps
   only the W1 static names + direct projection addressing. A per-face
   `:assembly/projection` envelope field is the pre-named extension when a
   face first needs a non-conversation data context."
  {:outline      :conversation
   "outline-face" :conversation
   :conversation :conversation})

(defn resolve-projection-kind
  "Face → projection kind, W2 resolution order (G26 fix — the live wearing
   found /face boxes-face serving :unknown-projection because dispatch only
   knew the W1 static names):
   1. the static map;
   2. a face registered in the arsenal roster → :conversation (the roster IS
      the face registry; T14 — Rama decides, never the faces directory);
   3. the face itself (direct projection addressing: :assembly, :face-list);
   an unregistered, unknown face resolves to itself and misses the registry →
   the honest :unknown-projection error (never a default-to-conversation for
   names the land has never seen)."
  [ctx face]
  (or (get face->projection-kind face)
      (when-let [arsenal (:arsenal-rt ctx)]
        (try
          (let [face-name (if (keyword? face) (name face) (str face))]
            (when (some? (face-arsenal/read-face arsenal face-name))
              :conversation))
          (catch Throwable _ nil)))
      face))

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
         pkw  (resolve-projection-kind ctx face)
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
