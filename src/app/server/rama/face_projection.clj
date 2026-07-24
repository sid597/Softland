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
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.rama.object-container.assembly-adapter :as assembly-adapter]
            [app.server.rama.object-container.provenance-material :as provenance-adapter]
            [app.server.rama.face-arsenal :as face-arsenal]
            [app.shared.provenance-material :as provenance-material]
            ;; READ-ONLY use of the relation kernel's PUBLIC query surface (rk
            ;; CONTRACT §7 — read-relations-for-targets ONLY; never a PState path,
            ;; never an append). Machine-cut pair structure (CONTRACT §4.4/§6).
            [app.server.rama.relation-kernel :as rk]
            ;; ONE def, two call sites (INT re-home, LANES §INT item 3): the
            ;; asserter id this projection filters served edges to is the
            ;; DRIVER's canonical constant — drift impossible while both exist
            ;; (the T18 parity shape). Required for the constant only; nothing
            ;; else of the driver is read here (the projection stays read-only).
            [app.server.rama.machine-cut :as machine-cut]))

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
                block   (cond-> {:id       (:unit-id b)
                                 :kind     (:form b)
                                 :text     (:text b)
                                 :order    (:order b)
                                 :time-ms  (:time-ms b)
                                 :part-path (:part-path b)
                                 ;; block-write PHASE_0 rule 2 (additive thread-through):
                                 ;; surface the unit's own document-container-id (from
                                 ;; river-page rule 1) alongside :id, so the edit outbox
                                 ;; (Lane B) copies it verbatim into the §3 payload — the
                                 ;; client tracks no container id (BW-T7). Purely additive;
                                 ;; existing served keys are byte-stable (MC-T8 class).
                                 :document-container-id (:document-container-id b)
                                 :block-path (:block-path b)}
                          ;; one canvas, many conversations: the lane a block's
                          ;; material came from (additive; canvas blocks carry none)
                          (:thread-id b) (assoc :thread-id (:thread-id b))
                          ;; D-core: the episode (CLI session) the material rode —
                          ;; absent = the lane's first episode (pre-chain material)
                          (:episode-id b) (assoc :episode-id (:episode-id b)))]
            (if-let [idx (get order->idx euid)]
              (update-in acc [:turns idx :blocks] conj block)
              (let [idx (count turns)]
                (-> acc
                    (assoc-in [:order->idx euid] idx)
                    (update :turns conj
                            (cond-> {:id     euid
                                     :speaker (:actor b)
                                     :order  (first (:order b))
                                     :blocks [block]}
                              (:thread-id b) (assoc :thread-id (:thread-id b))
                              (:episode-id b) (assoc :episode-id (:episode-id b)))))))))
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

(def episode-thread-source
  "Matches app.server.episode/episode-source — the transcript source every
   thread session's jsonl harvests under. Duplicated by VALUE because episode
   is the adapter layer above this one (requiring it here inverts layering);
   the constant is birth-fixed (2026-07-17) — it names on-disk identity and
   cannot drift."
  :claude-code)

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

(defn merge-episode-lanes
  "first-light A P2 — the episode time merge (PURE). river-page appends the
   native (:lane :episode) blocks after the river page; global reading order
   for an episode conversation is a STABLE sort by effective time (the
   apply-until-ms running-max discipline: a zero/backwards clock inherits the
   floor of everything before it IN ITS LANE, so within-lane order is
   preserved and no cross-lane sort can reorder a lane against itself).
   Conversations with no native blocks return the vector UNTOUCHED — the
   proven river order stays byte-identical (MC-T8 class)."
  [blocks]
  (if-not (some #(= :episode (:lane %)) blocks)
    blocks
    (let [eff (fn [floor b] (max (long floor) (long (or (:time-ms b) 0))))
          stamp (fn [lane-blocks]
                  (loop [bs lane-blocks, floor 0, out []]
                    (if-let [b (first bs)]
                      (let [t (eff floor b)]
                        (recur (rest bs) t (conj out (assoc b ::eff t))))
                      out)))
          lanes (group-by #(= :episode (:lane %)) blocks)
          stamped (into (stamp (get lanes false [])) (stamp (get lanes true [])))]
      (->> stamped
           (sort-by ::eff)                 ; clojure sort is stable
           (mapv #(dissoc % ::eff))))))

(defn- merge-stamped-lanes
  "The serve-level river merge core (PURE): canvas as ONE lane, each extra
   lane stamped by `stamp-fn [lane-id block]`. Same running-max floor
   discipline as merge-episode-lanes: a zero/backwards clock inherits its
   lane's floor, so no cross-lane sort can reorder a lane against itself.
   Empty lanes returns canvas-blocks UNTOUCHED (MC-T8 class)."
  [canvas-blocks lanes stamp-fn]
  (if (empty? lanes)
    (vec canvas-blocks)
    (let [stamp (fn [lane-blocks]
                  (loop [bs lane-blocks, floor 0, out []]
                    (if-let [b (first bs)]
                      (let [t (max (long floor) (long (or (:time-ms b) 0)))]
                        (recur (rest bs) t (conj out (assoc b ::eff t))))
                      out)))
          all   (reduce (fn [acc [lid blocks]]
                          (into acc (stamp (mapv #(stamp-fn lid %) blocks))))
                        (stamp (vec canvas-blocks))
                        lanes)]
      (->> all
           (sort-by ::eff)                 ; stable — within-lane order holds
           (mapv #(dissoc % ::eff))))))

(defn merge-thread-lanes
  "One canvas, many conversations — the thread weave (PURE shell over
   merge-stamped-lanes). `thread-lanes` = seq of [thread-id blocks]; every
   thread block is stamped :thread-id. Empty thread-lanes returns
   canvas-blocks UNTOUCHED — the single-thread serve stays byte-identical
   (MC-T8 class)."
  [canvas-blocks thread-lanes]
  (merge-stamped-lanes canvas-blocks thread-lanes
                       (fn [tid b] (assoc b :thread-id (str tid)))))

(defn merge-successor-episodes
  "D-core weave (PURE shell over merge-stamped-lanes): a lane's successor-
   episode containers merge INTO the lane — no :thread-id stamp, successors
   continue the same column; :episode-id rides each block for the client's
   boundary marker. `episode-lanes` = seq of [episode-id blocks]. Empty =
   canvas-blocks untouched (MC-T8 class)."
  [canvas-blocks episode-lanes]
  (merge-stamped-lanes canvas-blocks episode-lanes
                       (fn [eid b] (assoc b :episode-id (str eid)))))

;; ===========================================================================
;; Machine-cut pair structure (CONTRACT §4.4/§6). The :conversation projection
;; gains pair structure served from the relation kernel's :pairs-with edges;
;; `:turns` and every existing key stay byte-identical (MC-T8) — these are
;; ADDITIVE keys derived AFTER the until-ms cut (MC-T11). The PURE core
;; (derive-pair-structure) is directly unit-testable with synthetic turns +
;; edges (G4); the IPC read (read-machine-cut-edges) is ONE total call.
;; ===========================================================================

(def machine-cut-actor-v0
  "The v0 machine-cut asserter id this projection filters served edges to
   (CONTRACT §4.3/§4.4). Aliased from the driver's canonical def (INT re-home,
   2026-07-12) — one value, two call sites, drift impossible (T18 shape)."
  machine-cut/machine-cut-actor-id)

(defn actor->structure-label
  "Header label for a machine-cut asserter id: \"llm:machine-cut/v1\" →
   \"machine-cut v1\" (CONTRACT §6 structure-line example). Strips the leading
   asserter-type segment and renders the version readably. PURE."
  [actor]
  (let [s (str actor)
        s (if (str/starts-with? s "llm:") (subs s 4) s)]
    (str/replace s "/" " ")))

(defn dedup-edges-by-relation-id
  "MC-T14: both endpoint copies of every intra-conversation edge land under ONE
   target-key (relation_kernel.clj:505-508), so a target read can carry each edge
   TWICE (o:/i: sort-key copies, byte-identical). Keep the FIRST occurrence per
   relation-id, order-stable. (The R1 query already dedups; this makes the pure
   core robust to a both-copies fixture — the MC-T14 pin.) PURE."
  [edge-rows]
  (:out
   (reduce (fn [{:keys [seen] :as acc} row]
             (let [rid (:relation-id row)]
               (if (contains? seen rid)
                 acc
                 (-> acc (update :seen conj rid) (update :out conj row)))))
           {:seen #{} :out []}
           edge-rows)))

(defn derive-pair-structure
  "PURE (CONTRACT §4.4/§6; MC-T10/T11/T14). Partition the POST-CUT `turns` (river
   order) into pair structure over the machine-cut :pairs-with edges.

   `turns`     — the shape-conversation :turns AFTER apply-until-ms (MC-T11: a
                 cut prompt strands its responses in :unpaired; a served prompt
                 whose responses are all cut keeps an empty :responses).
   `edge-rows` — raw RelationEdgeRow-like maps under the conversation key: may
                 carry both endpoint copies (MC-T14) AND foreign asserters
                 (MC-T10). `from` = response event, `to` = prompt event; each
                 endpoint's :target-id is (tid/chat-message-id address event-uuid).
   `address`   — the conversation object-key (matches the served turns' events).
   `actor`     — the configured machine-cut asserter id; foreign asserters are
                 COUNTED, never merged (MC-T10).

   Returns ONLY the four ADDITIVE keys (never :turns — MC-T8):
   {:pairs :unpaired :conversation/structure :conversation/structure-line}.
   Totality (§6): every served turn appears in exactly one of :pairs (as prompt
   or response) or :unpaired."
  [turns edge-rows address actor]
  (let [turn-index    (into {} (map-indexed (fn [i t] [(:id t) i])) turns)
        turn-by-id    (into {} (map (fn [t] [(:id t) t])) turns)
        ;; Served turns only — the map answers \"is this event on the page?\".
        tid->turn-id  (into {} (map (fn [t] [(tid/chat-message-id address (:id t))
                                             (:id t)]))
                            turns)
        order-of      (fn [tid] (get turn-index tid Long/MAX_VALUE))
        ;; MC-T14 dedup, then MC-T10 split by asserter (foreign counted, not merged).
        deduped       (dedup-edges-by-relation-id edge-rows)
        grouped       (group-by (fn [e] (= (str (:asserter-actor-id e)) (str actor)))
                                deduped)
        mc-edges      (vec (get grouped true))
        foreign-cnt   (count (get grouped false))
        ;; Resolve each machine-cut edge to (response-turn-id, prompt-turn-id);
        ;; nil endpoints are events not on the served page (cut or off-page).
        resolved      (map (fn [e]
                             {:resp   (tid->turn-id (:target-id (:from e)))
                              :prompt (tid->turn-id (:target-id (:to e)))})
                           mc-edges)
        ;; MC-T11: a pair forms only if its PROMPT is served (post-cut). A cut
        ;; prompt drops the edge → its response (if served) falls to :unpaired.
        usable        (filter (fn [{:keys [prompt]}] (some? prompt)) resolved)
        prompt-ids    (into #{} (map :prompt) usable)
        ;; Response assignment: a response is claimed only if it is served AND is
        ;; not itself a pair head (totality — a head is never nested as another
        ;; pair's response; the defensive chain rule). Conflict (MC-T11/§6): a
        ;; response with ≥2 candidate prompts → the river-order-earliest wins,
        ;; +1 :conflicts.
        resp->prompts (reduce (fn [m {:keys [resp prompt]}]
                                (if (and (some? resp) (not (contains? prompt-ids resp)))
                                  (update m resp (fnil conj #{}) prompt)
                                  m))
                              {}
                              usable)
        conflicts     (count (filter (fn [[_ ps]] (> (count ps) 1)) resp->prompts))
        resp->chosen  (into {}
                            (map (fn [[resp ps]]
                                   [resp (apply min-key order-of (vec ps))]))
                            resp->prompts)
        prompt->resps (reduce (fn [m [resp prompt]]
                                (update m prompt (fnil conj []) resp))
                              {}
                              resp->chosen)
        pairs         (->> prompt-ids
                           (sort-by order-of)
                           (mapv (fn [pid]
                                   {:id pid
                                    :user-turn (turn-by-id pid)
                                    :responses (->> (get prompt->resps pid [])
                                                    (sort-by order-of)
                                                    (mapv turn-by-id))
                                    :asserted-by actor})))
        paired-ids    (into prompt-ids (keys resp->chosen))
        unpaired      (->> turns
                           (remove (fn [t] (contains? paired-ids (:id t))))
                           vec)
        pairs-count   (count pairs)
        ;; Scope note (FALSIFY_B F3, recorded 2026-07-12): :pairs-count and
        ;; :unpaired-count are WINDOW-scoped (the served page); :edges-read,
        ;; :conflicts and :foreign-asserter-edges count ALL machine-cut edges
        ;; under the conversation key — diagnostics of the whole conversation,
        ;; not the page. They coincide at the default limit (the annotation
        ;; window IS the served page in v0).
        structure     {:source                 (if (pos? pairs-count) :machine-cut :none)
                       :asserter               actor
                       :pairs-count            pairs-count
                       :unpaired-count         (count unpaired)
                       :edges-read             (count mc-edges)
                       :conflicts              conflicts
                       :foreign-asserter-edges foreign-cnt}
        line          (if (pos? pairs-count)
                        (str (actor->structure-label actor)
                             " · " pairs-count " pairs · " (count unpaired) " unpaired")
                        "no machine cut")]
    {:pairs                      pairs
     :unpaired                   unpaired
     :conversation/structure     structure
     :conversation/structure-line line}))

(def none-structure
  "The totality landing value (CONTRACT §6, G12): absent rk-rt / no edges / a read
   failure → honest :none, never a throw. Additive keys only (MC-T8)."
  {:pairs                      []
   :unpaired                   []
   :conversation/structure     {:source                 :none
                                :asserter               machine-cut-actor-v0
                                :pairs-count            0
                                :unpaired-count         0
                                :edges-read             0
                                :conflicts              0
                                :foreign-asserter-edges 0}
   :conversation/structure-line "no machine cut"})

(defn read-machine-cut-edges
  "Total read of the conversation's :pairs-with edges (CONTRACT §4.4): ONE
   conversation-key call to the relation kernel's PUBLIC query surface (rk
   CONTRACT §7 — never a direct PState path, never an append). The stored
   target-key of every intra-conversation pair edge IS the conversation
   object-key (§4.2), so `address` is both the read key and the returned map key
   (the query already retract-filters and dedups by relation-id). Absent rk-rt or
   ANY read failure → [] (serve totality, G12 — never a throw; MC-T12: a poisoned
   boot handle degrades honestly here, it never re-throws)."
  [rk-rt address]
  (if (or (nil? rk-rt) (nil? address))
    []
    (try
      (get (rk/read-relations-for-targets rk-rt [address] [:pairs-with] false)
           address [])
      (catch Throwable t
        (println "[FACE] machine-cut edge read failed:" (.getMessage t))
        []))))

(defn conversation-projection
  "The ONE Wave-1 projection: conversation → turns → blocks-with-kinds, over the
   block kernel's durable state via `river-page` (READ-ONLY). `ctx` carries the OC
   runtime; `request` is the CONTRACT §7 shape {:face :address :params :epoch}, where
   :address is the conversation object-key and :params {:limit :until-ms}. Returns the
   §7 data-context (never throws — a bad address yields an error data-context)."
  [{:keys [oc-rt rk-rt]} {:keys [address params] :as request}]
  (let [limit    (clamp-limit (:limit params))
        until-ms (:until-ms params)]
    (if (nil? address)
      (merge
       none-structure                     ; additive pair keys, totality (MC-T8/§6)
       {:turns []
        ;; the map must not lie: a first-light runtime still distilling (or
        ;; failed) is a DIFFERENT reality than a genuinely blank request —
        ;; resolve-request (file_viewer) stamps the hint (G16 falsification fix)
        :conversation/error (:face/first-light request :missing-address)
        :conversation/river-events-total 0
        :conversation/blocks-returned 0
        :conversation/truncated? false
        :face/rendered-at-ms (System/currentTimeMillis)})
      (let [page      (bd/river-page {:oc-rt oc-rt :object-key address} limit)
            read-plan (:river-page/read-plan (meta page))
            canvas-blocks (merge-episode-lanes (attach-source-times oc-rt page))
            ;; one canvas, many conversations: the canvas's turn records ARE
            ;; its thread registry — each :thread-id names a per-thread CLI
            ;; session whose distilled material lives in that session's OWN
            ;; container (identity follows the jsonl line's sessionId). Read
            ;; each thread's page and merge it into the one river. Guarded +
            ;; total: a failed thread page degrades to absence, named in
            ;; :conversation/thread-errors — never a thrown canvas.
            cell-of*  (fn [row]
                        (or (:turn row)
                            (try (some-> (:content-preview row) edn/read-string)
                                 (catch Exception _ nil))))
            cells     (vec (keep cell-of* (:river-page/turn-rows (meta page))))
            thread-ids (->> cells (keep :thread-id) distinct vec)
            ;; D-core: the turn cells are the durable episode CHAIN. A lane's
            ;; FIRST episode is its own container (canvas = address; thread =
            ;; the thread-id's container); only SUCCESSOR episodes are extra
            ;; reads, woven INTO their lane by merge-successor-episodes (no
            ;; :thread-id stamp — same column, :episode-id for the boundary).
            lane-key  (fn [eid] (tid/transcript-object-key
                                 episode-thread-source eid))
            successors (fn [tid]
                         (->> cells
                              (filter #(= tid (:thread-id %)))
                              (keep :episode-id)
                              distinct
                              (remove #(if tid
                                         (= % tid)
                                         (= (lane-key %) address)))
                              vec))
            episode-page (fn [eid]
                           (try
                             (let [epage (bd/river-page
                                          {:oc-rt oc-rt :object-key (lane-key eid)}
                                          limit)
                                   eplan (:river-page/read-plan (meta epage))]
                               {:eid eid
                                :blocks (attach-source-times oc-rt epage)
                                :truncated? (:truncated? eplan)})
                             (catch Throwable t
                               {:eid eid :error (.getMessage t)})))
            ok-lanes  (fn [reads]
                        (vec (keep (fn [{:keys [eid blocks error]}]
                                     (when-not error [eid blocks]))
                                   reads)))
            main-ep-reads (mapv episode-page (successors nil))
            canvas-blocks (merge-successor-episodes canvas-blocks
                                                    (ok-lanes main-ep-reads))
            thread-reads (mapv (fn [tid]
                                 (let [tkey (lane-key tid)]
                                   (try
                                     (let [tpage (bd/river-page
                                                  {:oc-rt oc-rt :object-key tkey}
                                                  limit)
                                           tplan (:river-page/read-plan (meta tpage))
                                           ereads (mapv episode-page (successors tid))]
                                       {:tid tid
                                        :blocks (merge-successor-episodes
                                                 (attach-source-times oc-rt tpage)
                                                 (ok-lanes ereads))
                                        :ep-errors (into {} (keep (fn [{:keys [eid error]}]
                                                                    (when error [eid error]))
                                                                  ereads))
                                        :truncated? (or (:truncated? tplan)
                                                        (boolean (some :truncated? ereads)))})
                                     (catch Throwable t
                                       {:tid tid :error (.getMessage t)}))))
                               thread-ids)
            thread-lanes (vec (keep (fn [{:keys [tid blocks error]}]
                                      (when-not error [tid blocks]))
                                    thread-reads))
            thread-errors (into {} (keep (fn [{:keys [tid error]}]
                                           (when error [tid error]))
                                         thread-reads))
            episode-errors (into {}
                                 (concat
                                  (keep (fn [{:keys [eid error]}]
                                          (when error [eid error]))
                                        main-ep-reads)
                                  (mapcat :ep-errors thread-reads)))
            blocks    (merge-thread-lanes canvas-blocks thread-lanes)
            ;; first-light P2b (ADDITIVE keys, MC-T8 class): settled geometry
            ;; cells + the world camera + revision-pinned turn records, from
            ;; the SAME projection read river-page already performed (meta
            ;; keys — zero extra seeks). Cell values ride the :geometry/:turn
            ;; extra keys (round-trip proven, P2B.md receipt a); the
            ;; content-preview pr-str copy is the belt for any row whose
            ;; extra key did not survive.
            cell-of   (fn [row k]
                        (or (get row k)
                            (try (some-> (:content-preview row)
                                         edn/read-string)
                                 (catch Exception _ nil))))
            geo-rows  (:river-page/geo-rows (meta page))
            geometry  (into {}
                            (keep (fn [r]
                                    (when (= :episode-geometry (:entry-kind r))
                                      (when-let [g (cell-of r :geometry)]
                                        [(:unit-id g) g]))))
                            geo-rows)
            camera    (some (fn [r]
                              (when (= :episode-camera (:entry-kind r))
                                (cell-of r :geometry)))
                            geo-rows)
            turn-recs (vec (keep #(cell-of % :turn)
                                 (:river-page/turn-rows (meta page))))
            ;; Task 18: tombstoned units (geometry cell :deleted?) leave the
            ;; serve — the client's reconcile closes vanished slots through
            ;; the normal truth loop; the geometry endpoint bumps the epoch
            ;; so the re-pull lands promptly
            blocks    (if (some :deleted? (vals geometry))
                        (vec (remove #(:deleted? (get geometry (:unit-id %)))
                                     blocks))
                        blocks)
            dc        (shape-conversation {:blocks         blocks
                                           :read-plan      read-plan
                                           :address        address
                                           :limit          limit
                                           :until-ms       until-ms
                                           :focus-turn     (:focus-turn params)
                                           :rendered-at-ms (System/currentTimeMillis)})
            dc        (assoc dc
                             :conversation/geometry geometry
                             :conversation/camera camera
                             :conversation/turn-records turn-recs)
            ;; thread honesty (additive; map-must-not-lie): a truncated thread
            ;; page or a failed thread read is a named fact, never silence
            dc        (cond-> dc
                        (seq thread-ids)
                        (assoc :conversation/thread-ids thread-ids)
                        (some :truncated? thread-reads)
                        (assoc :conversation/truncated? true)
                        (seq thread-errors)
                        (assoc :conversation/thread-errors thread-errors)
                        ;; D-core honesty: a truncated or failed successor-
                        ;; episode read is a named fact, never silence
                        (some :truncated? main-ep-reads)
                        (assoc :conversation/truncated? true)
                        (seq episode-errors)
                        (assoc :conversation/episode-errors episode-errors))
            ;; §4.4/§6: read the machine-cut :pairs-with edges (total; [] on any
            ;; failure or absent rk-rt), derive pair structure over the SAME
            ;; post-until-ms `:turns` shape-conversation served (MC-T11). Merge
            ;; ONLY the four additive keys — `:turns` + every existing key stay
            ;; byte-identical (MC-T8).
            edges     (read-machine-cut-edges rk-rt address)
            structure (derive-pair-structure (:turns dc) edges address
                                             machine-cut-actor-v0)]
        (merge dc structure)))))

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

(defn provenance-material-projection
  "P1 one-facet serve. Latest and active are resolved independently: the
   material container names latest, while the explicit pointer container
   names the worn immutable revision. A malformed latest candidate is
   reportable under the drill scope without replacing active material."
  [{:keys [oc-rt]} request]
  (let [now (System/currentTimeMillis)
        drill? (true? (get-in request [:params :drill?]))
        floor {:facet-master/id provenance-material/master-id
               :facet-master/found? false
               :facet-master/valid? false
               :facet-master/material
               (select-keys provenance-material/code-floor [:provenance/tint])
               :facet-master/active-revision-id
               provenance-material/code-floor-revision-id
               :facet-master/floor? true
               :facet-master/errors [{:type :facet-master/unavailable}]
               :face/rendered-at-ms now}]
    (if (nil? oc-rt)
      floor
      (let [{:keys [latest-revision active-pointer active-revision]}
            (provenance-adapter/read-master oc-rt)
            latest-compiled (when latest-revision
                              (provenance-material/compile-source
                               (:content-text latest-revision)))
            active-compiled (when active-revision
                              (provenance-material/compile-source
                               (:content-text active-revision)))
            active-valid? (true? (:valid? active-compiled))
            active-id (:revision-id active-revision)
            latest-id (:revision-id latest-revision)
            candidate-invalid? (and latest-revision
                                    (not= latest-id active-id)
                                    (false? (:valid? latest-compiled)))]
        (cond-> {:facet-master/id provenance-material/master-id
                 :facet-master/found? (some? latest-revision)
                 :facet-master/valid? active-valid?
                 :facet-master/material
                 (if active-valid?
                   (:material active-compiled)
                   (select-keys provenance-material/code-floor
                                [:provenance/tint]))
                 :facet-master/active-revision-id
                 (or active-id provenance-material/code-floor-revision-id)
                 :facet-master/latest-revision-id latest-id
                 :facet-master/pointer-revision-id (:revision-id active-pointer)
                 :facet-master/floor? (not active-valid?)
                 :facet-master/errors
                 (if active-valid?
                   []
                   (or (:errors active-compiled)
                       [{:type :facet-master/active-revision-missing}]))
                 :face/rendered-at-ms now}
          (and drill? candidate-invalid?)
          (assoc :facet-master/candidate-revision-id latest-id
                 :facet-master/candidate-errors (:errors latest-compiled)))))))

;; ===========================================================================
;; The projection registry + server-side face dispatch (trap T8).
;; ===========================================================================

(defn block-truth-projection
  "block-write INT · the §5 narrowing serve: edited units' materialized truth
   via the SAME read-unit overlay river-page uses (the graduation overlay
   returns edited content — Lane A G1). Narrower read, same transport, same
   criterion (CONTRACT §5). Read-only by construction (G12). Request:
   {:face :block-truth :address <object-key>
    :params {:units {<unit-id> <nonce>}} :epoch n} — a UNION map, not a
   single id (FALSIFY F1: Electric conflates a single-value request atom to
   the latest value, silently dropping a cross-unit pull; a union map makes
   conflation lossless — the latest value contains every armed unit; capped
   client-side). Reaches this entry by direct projection addressing (resolve
   order 3). Total: unknown units serve found? false, never a throw (L13)."
  [{:keys [oc-rt]} request]
  (let [units (keys (get-in request [:params :units]))]
    {:block-truth/units
     (into {}
           (map (fn [unit-id]
                  ;; UnitReadResult's TOP-LEVEL :content-text is the overlay:
                  ;; the graduation row's current content when edited
                  ;; (refreshed per revision, object_container.clj:1491-1499),
                  ;; the derived text when never edited — the same truth
                  ;; river-page serves. Read AT EXECUTION time: a late pull
                  ;; can never serve stale content.
                  (let [result (ocr/read-unit oc-rt unit-id)]
                    [unit-id {:found? (some? result)
                              :text   (:content-text result)}])))
           units)
     :face/rendered-at-ms (System/currentTimeMillis)}))

(def projection-registry
  "Plain value: {<projection-kw> → (fn [ctx request] → data-context)}. Wave 1
   registered ONE projection; W2 adds the two arsenal reads (:assembly wear-time
   source serve + :face-list roster). Extensible by adding an entry — never by an
   Electric `case`. Persisted form (Wave 2, schema §8) is keyword + code address,
   never fn values (trap T5). block-write INT adds :block-truth (the §5
   single-unit echo read)."
  {:conversation conversation-projection
   :assembly     assembly-projection
   :face-list    face-list-projection
   :provenance-material provenance-material-projection
   :block-truth  block-truth-projection})

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
  (merge
   none-structure                         ; additive pair keys, totality (MC-T8/§6)
   {:turns []
    :conversation/error reason
    :conversation/requested-face (:face request)
    :conversation/river-events-total 0
    :conversation/blocks-returned 0
    :conversation/truncated? false
    :face/rendered-at-ms (System/currentTimeMillis)}))

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
         ;; the edit-ack path shares this Electric session's sequential
         ;; propagation: a slow serve here is FELT as a typing stall (the
         ;; ground report's slow-echo outliers) — name it in the log so the
         ;; two sides correlate by timestamp
         (let [t0 (System/nanoTime)
               r  (pfn ctx request)
               ms (/ (- (System/nanoTime) t0) 1e6)]
           (when (> ms 100.0)
             (println (format "[FACE] slow serve %.0fms face=%s at=%s"
                              ms (str face) (str (java.time.Instant/now)))))
           r)
         (catch Throwable t
           (println "[FACE] projection read failed:" (.getMessage t))
           (error-data-context request :projection-read-failed)))
       (error-data-context request :unknown-projection)))))

;; ===========================================================================
;; D-core · the successor-episode seed (Sid 2026-07-22). A fresh episode's
;; first prompt inherits its lane's conversation as PLAIN PROSE — utterances
;; and reply prose, noise folded away — so the new CLI session continues the
;; conversation without replaying a single foreign jsonl line.
;; ===========================================================================

(def seed-noise-kinds
  "The run-block noise split, duplicated by VALUE from the client's fold rule
   (ground.cljs noise-kinds — the client is the layer above; requiring it here
   is impossible): these kinds fold away; the seed carries prose only."
  #{:thinking :tool-use :tool-result-span :material-part})

(defn compose-episode-seed
  "PURE over the §7 data-context: the lane's turns (canvas lane when
   `lane-thread-id` is nil, else that thread's) as a speaker-labelled prose
   transcript, wrapped for a fresh session's first message. Returns nil when
   there is nothing to inherit — the seedless virgin-lane spawn."
  [dc lane-thread-id]
  (let [ltid  (some-> lane-thread-id str not-empty)
        lines (->> (:turns dc)
                   (filter #(= ltid (some-> (:thread-id %) str)))
                   (keep (fn [t]
                           (let [prose (->> (:blocks t)
                                            (remove #(contains? seed-noise-kinds
                                                                (:kind %)))
                                            (keep :text)
                                            (remove str/blank?)
                                            (str/join "\n\n"))]
                             (when-not (str/blank? prose)
                               (str (if (= "sid" (str (:speaker t))) "sid" "agent")
                                    ": " prose)))))
                   vec)]
    (when (seq lines)
      (str "<conversation-so-far>\n"
           "This fresh session continues an ongoing conversation on the same"
           " canvas. The thread so far, prose only (tool work elided):\n\n"
           (str/join "\n\n" lines)
           "\n</conversation-so-far>\n\n"))))

(defn episode-seed
  "Driver shell (TOTAL): read the canvas's conversation through the SAME
   projection the render serves, compose the lane's seed. Any failure yields
   nil — a seedless fresh session, never a blocked spawn."
  [ctx canvas-address lane-thread-id]
  (try
    (compose-episode-seed
     (conversation-projection ctx {:address canvas-address :params {}})
     lane-thread-id)
    (catch Throwable t
      (println "[FACE] episode-seed failed:" (.getMessage t))
      nil)))
