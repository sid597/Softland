(ns app.server.rama.object-container.block-distiller
  "sense-block-v0 — the block layer over the container kernel.

   NOT a Rama module: pure adapter fns + a driver (git_spine.clj shape) over
   the EXISTING object-container-module and relation-kernel-module public APIs.
   Realizes SPEC.md (block layer v0) + CONTRACT.md (v2, adapter shape).

   This file is built in the CONTRACT's phase order. Phase 0 is the PURE layer:
   §A ids, §B classification, §C actor resolution, §D production events, §E part
   extraction, §F the free cut, §H mechanical-edge specs — plus `distill-event`,
   the pure per-event assembler the golden is taken over. Phase 1 (this pass)
   adds §G row builders (SPEC nouns → container-kernel rows) and §I the driver
   (foreign client: read stored sources → distill river events → submit OC import
   requests), plus the F2 delegation-home decision (option A: :production-event as
   an extra record key on the per-part surface, proven by the F2 spike).

   Traps cited by number are CONTRACT §7. Rulings (R*, SC*) are CONTRACT §3 /
   PLAN §0. Forms are SPEC §4.6 (a CLOSED vocabulary)."
  (:require [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.markdown-adapter :as md]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [clojure.edn :as edn]
            [clojure.string :as str]))

;; ===========================================================================
;; §A · Identity constants + deterministic id minters (pure)
;; ===========================================================================

(def distiller-id "sense-block-v0")
(def distiller-version 1)
(def river-debris-classifier-id "river-debris-v0")
(def episode-native-classifier-id
  "The :native class-ledger stamp (first-light A P2, flag D): marks a user
   text event whose material is already durable under imp:ep: — see
   class-hint-import-request's 4-arity."
  "episode-native-v0")
(def mechanical-asserter "sense-block/mechanical@1")
(def mechanical-asserter-type :machine)

(defn derived-unit-id
  "du:<object-key>:sense-block-v0:<block-path> (markdown_adapter.clj:13-15
   shape). The distiller segment keeps strata id-disjoint (G5, CONTRACT R3)."
  [object-key block-path]
  (str "du:" object-key ":" distiller-id ":" block-path))

(defn block-path
  "Globally unique within an object-key: encodes event-order · part-index ·
   block-index so unit-ids never collide in $$derived-units-by-id (N1c)."
  [event-order part-index block-index]
  (format "%06d:%02d:%06d" (long event-order) (long part-index) (long block-index)))

(defn per-part-source-id
  "src:tr:<object-key>:<hash> — MUST reuse the `src:tr:` prefix so
   extract-object-key routes per-part surfaces to the conversation's task (N1b).
   The hash discriminates (event-uuid, part-path) from the transcript adapter's
   own src:tr: line surfaces (disjoint hash inputs), so no collision."
  [object-key event-uuid part-path]
  (str "src:tr:" object-key ":" (core/sha-256 (str "part " event-uuid " " part-path))))

(defn import-key
  "imp:tr:<object-key>:sb:<hash> — REUSES the handled `imp:tr:` prefix so
   extract-object-key strips it → leading-object-key → chat:<hex>, routing a
   FOREIGN read-import-completion to the task the import topology wrote on (the
   completion is keyed by import-key but written on partition(object-key); the
   read must extract that same object-key). The `:sb:`-namespaced hash keeps this
   a DISTINCT full key from the transcript adapter's own `imp:tr:<ok>:<hash>` (no
   `sb:` segment — a pure hex hash can never equal `sb:<hex>`), so a second
   distillation over an already-ingested source still dedups on its own key
   (R4 / §3.3). Mirrors the `src:tr:` reuse in per-part-source-id. (F2, 2026-07-09:
   the old bare `imp:sense-block:` prefix fell through extract-object-key to :else
   → whole-string partition → a foreign read mis-routed to nil; CONTRACT T4.)"
  [object-key event-uuid]
  (str "imp:tr:" object-key ":sb:" (core/sha-256 (str event-uuid))))

(defn edge-idempotency-key
  "sha256(unit-id ∥ kind ∥ target-id) — relation-scoped idempotency (T11 /
   CONTRACT §4 last row)."
  [from-id kind to-id]
  (core/sha-256 (str from-id " " (name kind) " " to-id)))

;; ===========================================================================
;; small readers over a parsed (redacted) transcript payload
;; ===========================================================================

(defn event-type*
  "Top-level jsonl `type` (mode / user / assistant / file-history-snapshot …).
   The classification key — NOT the message role (T1)."
  [parsed]
  (or (:type parsed) (get-in parsed [:message :role]) "unknown"))

(defn message-role
  [parsed]
  (or (get-in parsed [:message :role]) (:role parsed)))

(defn message-content
  [parsed]
  (get-in parsed [:message :content]))

(defn event-key
  "Stable per-event identity for goldens/rows. uuid on message events; messageId
   on file-history-snapshot; else order-scoped. (Debris events often lack uuid.)"
  [parsed order]
  (or (:uuid parsed)
      (:messageId parsed)
      (get-in parsed [:message :id])
      (str "evt:" order)))

(defn- parse-timestamp
  [ts]
  (when (string? ts)
    (try (.toEpochMilli (java.time.Instant/parse ts))
         (catch Exception _ nil))))

(defn- tool-result-type? [t] (contains? #{"tool_result" "tool-result"} t))
(defn- tool-use-type? [t] (contains? #{"tool_use" "tool-use"} t))

(defn- tool-use-text
  "A tool_use block's content = name + redacted args (SPEC §4.3). The args come
   from the ALREADY-redacted payload (T5 idempotency / redaction inherited)."
  [block]
  (str/trim (str (:name block) " " (pr-str (:input block)))))

(defn- tool-result-text
  "tool_result .content is string-or-array (verified: 76 string / 3 array)."
  [content]
  (cond
    (string? content) content
    (sequential? content) (str/join "\n" (keep (fn [b]
                                                  (when (map? b)
                                                    (str (or (:text b) (:content b) ""))))
                                                content))
    :else (str content)))

(defn- whole-message-text
  "A human message's material: the bare string, or its text blocks joined.
   (User content is polymorphic — 27 bare strings / 95 arrays in the real file;
   the reference reader drops strings, hence this local reader.)"
  [content]
  (cond
    (string? content) content
    (sequential? content) (str/join "\n" (keep (fn [b] (when (map? b) (:text b))) content))
    :else (str content)))

(defn- command-wrapper?
  "A user message that is a local-command wrapper (<command-name> …,
   <local-command-stdout> …) — debris/command (SPEC §3.2, §15 events 5/6)."
  [parsed]
  (boolean (re-find #"^\s*<(command-name|command-message|command-args|local-command-stdout|local-command-stderr|command-contents)>"
                    (str (whole-message-text (message-content parsed))))))

(defn- user-has-tool-result?
  [parsed]
  (let [content (message-content parsed)]
    (and (sequential? content)
         (boolean (some #(and (map? %) (tool-result-type? (:type %))) content)))))

;; ===========================================================================
;; §B · Classification — river vs debris (pure). SPEC §3, gate G1.
;; ===========================================================================

(def debris-event-types
  "Harness/operational jsonl types (VERIFIED census, real 7c80ce2a file).
   Everything here is retained but unmarkable (T9) and never enters the river
   block stream (SPEC §3.2)."
  #{"mode" "permission-mode" "file-history-snapshot" "attachment" "last-prompt"
    "ai-title" "bridge-session" "system" "custom-title" "agent-name"})

(defn classify-event
  "→ {:class :river|:debris :reason … :classifier-id}. Debris: the harness
   types + isMeta wrappers + command wrappers + unparseable (F1: a payload that
   would not read-string is classified debris and never cut). River: assistant
   content, human messages, tool_result material (SPEC §3.3)."
  [parsed raw-type]
  (let [base {:classifier-id river-debris-classifier-id}]
    (cond
      (nil? parsed)                               (assoc base :class :debris :reason :unparseable)
      (contains? debris-event-types raw-type)     (assoc base :class :debris :reason :harness-op)
      (and (= raw-type "user") (:isMeta parsed))  (assoc base :class :debris :reason :meta)
      (and (= raw-type "user") (command-wrapper? parsed)) (assoc base :class :debris :reason :command)
      (= raw-type "user")                         (assoc base :class :river
                                                          :reason (if (user-has-tool-result? parsed) :material :human))
      (= raw-type "assistant")                    (assoc base :class :river :reason :assistant)
      :else                                       (assoc base :class :debris :reason :default-debris))))

;; ===========================================================================
;; §C · Actor resolution (pure). SPEC §3.4 role ≠ actor (T1). Gate G1.
;; ===========================================================================

(defn resolve-actor
  "Who ACTUALLY produced this part — never the message role. tool_result →
   `tool`; isMeta/command → `harness`; assistant → the model id; genuine
   user-role → `human:<userType>`. Classing a tool_result as human speech is a
   conformance failure (SPEC §16.1)."
  [parsed part]
  (let [role (message-role parsed)]
    (cond
      (= (:part-type part) :tool-result) "tool"
      (:isMeta parsed)                   "harness"
      (= role "assistant")               (or (get-in parsed [:message :model]) "assistant")
      (= role "user")                    (str "human:" (or (:userType parsed) "external"))
      :else                              "harness")))

(defn- event-actor
  "The event's primary actor (production-event home; per-part actors still
   resolve individually via resolve-actor). A tool_result-bearing user event is
   produced by the tool, not the human (T1 at event grain)."
  [parsed]
  (let [role (message-role parsed)]
    (cond
      (:isMeta parsed)               "harness"
      (user-has-tool-result? parsed) "tool"
      (= role "assistant")           (or (get-in parsed [:message :model]) "assistant")
      (= role "user")                (str "human:" (or (:userType parsed) "external"))
      :else                          "harness")))

;; ===========================================================================
;; §D · Production event (pure). SPEC §7. Gate G2.
;; ===========================================================================

(defn production-event
  "Actor + delegation chain + degenerate context-parents + time. Reads
   parentUuid/isSidechain/promptId off the parsed payload (present on 100% of
   message events). Context-parents are recorded AS degenerate (linear-prefix
   pointer) — the map must not lie about which it has (SPEC §7.2)."
  [parsed class actor]
  {:production/class          (:class class)
   :production/classifier-id  (:classifier-id class)
   :production/actor          actor
   :production/actor-role     (message-role parsed)
   :delegation/parent-uuid    (:parentUuid parsed)
   :delegation/is-sidechain   (boolean (:isSidechain parsed))
   :delegation/prompt-id      (:promptId parsed)
   :delegation/on-behalf-of   (when (:isSidechain parsed) (:parentUuid parsed))
   :context/parents           (when-let [p (:parentUuid parsed)] [p])
   :context/degenerate?       true
   :production/time-ms        (parse-timestamp (:timestamp parsed))})

;; ===========================================================================
;; §E · Part extraction (pure). One part = one surface (SPEC §1). Gate G3/G6.
;; ===========================================================================

(defn- assistant-part
  [i block]
  (let [t (:type block)]
    (cond
      (= t "text")     {:part-path (str "content/" i) :part-index i :part-type :text
                        :text (str (:text block))}
      (= t "thinking") {:part-path (str "content/" i) :part-index i :part-type :thinking
                        :text (str (:thinking block))}
      (tool-use-type? t)    {:part-path (str "content/" i) :part-index i :part-type :tool-use
                             :tool-name (:name block) :tool-input (:input block)
                             :tool-use-id (:id block) :text (tool-use-text block)}
      (tool-result-type? t) {:part-path (str "content/" i) :part-index i :part-type :tool-result
                             :tool-use-id (:tool_use_id block) :is-error (boolean (:is_error block))
                             :text (tool-result-text (:content block))}
      (= t "image")    {:part-path (str "content/" i) :part-index i :part-type :image :text ""}
      :else            {:part-path (str "content/" i) :part-index i :part-type :material
                        :text (str (:text block ""))})))

(defn event-parts
  "Ordered per-event surfaces.
   - assistant → one part per typed content block (thinking/text/tool_use …;
     SPEC §4.2 'blocks as given').
   - user + tool_result(s) → one part per tool_result (material, §4.3).
   - human message → ONE whole-message part (humans are never forced to
     pre-chunk, §4.4; keeps the whole-message block anchorable in one surface)."
  [parsed]
  (let [role (message-role parsed)
        content (message-content parsed)]
    (cond
      (= role "assistant")
      (vec (keep-indexed (fn [i b] (when (map? b) (assistant-part i b)))
                         (if (sequential? content) content [])))

      (= role "user")
      (if (user-has-tool-result? parsed)
        (vec (keep-indexed (fn [i b]
                             (when (and (map? b) (tool-result-type? (:type b)))
                               {:part-path (str "content/" i) :part-index i :part-type :tool-result
                                :tool-use-id (:tool_use_id b) :is-error (boolean (:is_error b))
                                :text (tool-result-text (:content b))}))
                           content))
        [{:part-path "message" :part-index 0 :part-type :human-message
          :text (whole-message-text content)}])

      :else [])))

;; ===========================================================================
;; §F · The free cut (pure). CONTRACT §6 / SPEC §4. Gates G3, G11.
;; ===========================================================================
;; N2: md/markdown-block-v0 special-cases ONLY headings and list items —
;; fences/blockquotes/tables become paragraphs. SPEC §4.2 MUST never split a
;; fence, so atomicity for those three is THIS distiller's job, layered over md.

(defn- line-fence-marker
  "Opening code-fence marker (``` or ~~~, ≥3, ≤3 leading spaces) on a line, or nil."
  [text]
  (second (re-find #"^\s{0,3}(`{3,}|~{3,})" (str text))))

(defn- fence-close-line?
  "A closing fence: trimmed line is only the fence char repeated ≥ opener length."
  [text marker]
  (let [c (first marker)
        t (str/trim (str text))]
    (and (>= (count t) (count marker))
         (every? #(= % c) t))))

(defn- blockquote-line? [text] (boolean (re-find #"^\s{0,3}>" (str text))))

(defn- table-delimiter-line?
  "A GFM delimiter row: only |, -, :, whitespace, with at least one -."
  [text]
  (let [t (str/trim (str text))]
    (and (seq t) (str/includes? t "-") (boolean (re-matches #"[|:\-\s]+" t)))))

(defn- classify-cut-regions
  "Partition md/markdown-source-lines records into contiguous regions tagged
   :code-fence / :blockquote / :table (all atomic) or :markdown (→ md)."
  [lines]
  (let [v (vec lines)
        n (count v)]
    (loop [i 0, regions [], run []]
      (if (>= i n)
        (cond-> regions (seq run) (conj {:type :markdown :lines run}))
        (let [{:keys [text]} (v i)
              marker (line-fence-marker text)
              flush (fn [rs] (cond-> rs (seq run) (conj {:type :markdown :lines run})))]
          (cond
            marker
            (let [close (loop [j (inc i)]
                          (cond (>= j n) nil
                                (fence-close-line? (:text (v j)) marker) j
                                :else (recur (inc j))))
                  end (or close (dec n))]
              (recur (inc end) (conj (flush regions) {:type :code-fence :lines (subvec v i (inc end))}) []))

            (blockquote-line? text)
            (let [end (loop [j i]
                        (if (and (< j n) (blockquote-line? (:text (v j)))) (recur (inc j)) (dec j)))]
              (recur (inc end) (conj (flush regions) {:type :blockquote :lines (subvec v i (inc end))}) []))

            (and (str/includes? (str text) "|")
                 (< (inc i) n)
                 (table-delimiter-line? (:text (v (inc i)))))
            (let [end (loop [j i]
                        (if (and (< j n)
                                 (not (str/blank? (:text (v j))))
                                 (str/includes? (str (:text (v j))) "|"))
                          (recur (inc j)) (dec j)))]
              (recur (inc end) (conj (flush regions) {:type :table :lines (subvec v i (inc end))}) []))

            :else
            (recur (inc i) regions (conj run (v i)))))))))

(defn- map-md-form
  [md-kind]
  (case md-kind
    :markdown/paragraph :prose-para
    :markdown/heading   :header
    :markdown/list-item :list-item
    :prose-para))

(defn- sub-form
  "Human structural sub-blocks all collapse to form :human-sub (SPEC §4.6);
   assistant text keeps the structural form."
  [natural-form human?]
  (if human? :human-sub natural-form))

(defn free-cut-text
  "Cut a text part into blocks. Atomic fenced/quote/table regions stay whole;
   markdown gaps go through md/markdown-block-v0. Offsets are ABSOLUTE UTF-16
   into `text` (T2), and every block's :text is the exact slice (subs text
   start end) so anchors slice honestly (G6). `:human?` collapses forms to
   :human-sub (§4.4 silver subs)."
  ([text] (free-cut-text text {}))
  ([text {:keys [human?] :or {human? false}}]
   (let [lines (md/markdown-source-lines text)
         regions (classify-cut-regions lines)]
     (vec
      (mapcat
       (fn [{:keys [type lines]}]
         (let [start (:start-offset (first lines))
               end (:end-offset (last lines))]
           (case type
             :code-fence [{:unit-kind (sub-form :code-fence human?) :start-offset start :end-offset end
                           :text (subs text start end)}]
             :blockquote [{:unit-kind (sub-form :blockquote human?) :start-offset start :end-offset end
                           :text (subs text start end)}]
             :table      [{:unit-kind (sub-form :table human?) :start-offset start :end-offset end
                           :text (subs text start end)}]
             :markdown
             (for [b (md/markdown-block-v0 (subs text start end))
                   :let [abs-start (+ start (long (:start-offset b)))
                         abs-end (+ start (long (:end-offset b)))]
                   :when (> abs-end abs-start)]
               {:unit-kind (sub-form (map-md-form (:unit-kind b)) human?)
                :start-offset abs-start
                :end-offset abs-end
                :text (subs text abs-start abs-end)}))))
       regions)))))

(defn- whole-block
  "One block spanning [0, len). Empty text → no block (G11 non-empty)."
  [unit-kind text]
  (if (str/blank? text)
    []
    [{:unit-kind unit-kind :start-offset 0 :end-offset (count text) :text text}]))

(defn free-cut-part
  "CONTRACT §6 / SPEC §4 — cut ONE part into blocks (pure).
   tool_result & image → [] (surface only, §4.3; the tool-result-span block
   mints lazily on edge-demand in Phase 3b, §5.1). Offsets are UTF-16 into the
   part's :text (T2)."
  [{:keys [part-type text] :as _part}]
  (case part-type
    :thinking      (whole-block :thinking text)
    :tool-use      (whole-block :tool-use text)
    :text          (free-cut-text text)
    :human-message (let [len (long (count text))]
                     (into (whole-block :human-message text)
                           ;; SPEC §6.1: the whole-message block already occupies [0,len);
                           ;; drop any silver sub coincident with it (a single-paragraph human
                           ;; turn) so one (surface,span) never mints two unit-ids. Subs survive
                           ;; only where real structure carves a PROPER sub-span (§4.4). F1.
                           (remove #(and (= 0 (long (:start-offset %)))
                                         (= len (long (:end-offset %))))
                                   (free-cut-text text {:human? true}))))
    :tool-result   []
    :image         []
    :material      (whole-block :material-part text)
    []))

;; ===========================================================================
;; §H · Mechanical-edge specs (pure). SPEC §11.3, gate G8. Driver asserts.
;; ===========================================================================

(def write-tools #{"Edit" "Write" "MultiEdit" "NotebookEdit"})
(def read-tools  #{"Read" "Grep" "Glob" "NotebookRead" "WebFetch" "WebSearch"})

(defn write-tool? [tool-name] (contains? write-tools tool-name))
(defn read-tool?  [tool-name] (contains? read-tools tool-name))

(defn edge-spec
  "A pure mechanical-edge spec (the driver turns it into rk/assert-request in
   Phase 3b). Deterministic relation-scoped idempotency key (T11)."
  [kind from-id to-id]
  {:kind kind
   :from-id from-id
   :to-id to-id
   :idempotency-key (edge-idempotency-key from-id kind to-id)})

(defn tool-edge-kind
  "SPEC §11.3 mechanical floor: write-tool → :produced (the act produced the
   artifact touched), read-tool → :grounds (the read grounds in what it returned).
   Non-tool / unknown tools → nil (no floor edge)."
  [tool-name]
  (cond (write-tool? tool-name) :produced
        (read-tool?  tool-name) :grounds
        :else                   nil))

(defn tool-index
  "Pure. Across all distilled river events, index tool_use PARTS and tool_result
   PARTS by tool-use-id, carrying the coordinates to address each one's block +
   surface. The tool_use block is P1-minted (whole-block :tool-use, block-index 0);
   the tool_result block is P3b-demand-minted (N3 option A). Pairing is by
   tool-use-id (§4.3: the pair is bound by an edge, not by containment)."
  [object-key distilled-events]
  (reduce
   (fn [idx d]
     (let [order      (:order d)
           event-uuid (:event-key d)
           t          (get-in d [:production-event :production/time-ms])
           uid        (fn [pi] (derived-unit-id object-key (block-path order pi 0)))]
       (reduce
        (fn [idx part]
          (case (:part-type part)
            :tool-use
            (assoc-in idx [:uses (:tool-use-id part)]
                      {:tool-use-id (:tool-use-id part) :order order :part-index (:part-index part)
                       :part-path (:part-path part) :tool-name (:tool-name part)
                       :event-uuid event-uuid :production-time-ms t :unit-id (uid (:part-index part))})
            :tool-result
            (assoc-in idx [:results (:tool-use-id part)]
                      {:tool-use-id (:tool-use-id part) :order order :part-index (:part-index part)
                       :part-path (:part-path part) :text (str (:text part))
                       :event-uuid event-uuid :unit-id (uid (:part-index part))})
            idx))
        idx (:parts d))))
   {:uses {} :results {}}
   distilled-events))

(defn hole-endpoint-id
  "The absent :block address for an unpaired write/read tool_use — the awaited-but-
   unminted tool_result (SPEC §9 hole; the frontier where the result has not
   arrived). A du:-shaped id that is deterministically NEVER minted, so G9's
   dangling target is queryable and provably absent from $$derived-units-by-id."
  [object-key tool-use-id]
  (str "du:" object-key ":" distiller-id ":pending-result:" tool-use-id))

(defn mechanical-edge-plan
  "Pure. The mechanical edge floor (SPEC §11.3; gates G8/G9). For each write/read
   tool_use, plan ONE produced/grounds edge from the tool_use block to its endpoint:
   PAIRED → the tool_result's coarse block (N3 option A, Sid 2026-07-09); UNPAIRED →
   a HOLE (hole-endpoint-id; G9). Refs via rk/->target-ref (:block, N7); relation-
   scoped idempotency key (T11); asserted-at = the tool_use event's production time
   (replay-stable, never the wall clock). Deterministic order by tool-use-id."
  [object-key distilled-events]
  (let [{:keys [uses results]} (tool-index object-key distilled-events)]
    (->> (sort-by key uses)
         (keep (fn [[tuid use]]
                 (when-let [kind (tool-edge-kind (:tool-name use))]
                   (let [result   (get results tuid)
                         from-uid (:unit-id use)
                         to-uid   (if result (:unit-id result) (hole-endpoint-id object-key tuid))
                         from-ref (rk/->target-ref :block from-uid)
                         to-ref   (rk/->target-ref :block to-uid)]
                     {:tool-use-id tuid
                      :kind kind
                      :from-unit-id from-uid
                      :to-unit-id to-uid
                      :from-ref from-ref
                      :to-ref to-ref
                      :relation-id (rk/relation-id-for kind from-ref to-ref mechanical-asserter)
                      :idempotency-key (edge-idempotency-key from-uid kind to-uid)
                      :production-time-ms (long (or (:production-time-ms use) 0))
                      :paired? (some? result)
                      :result result}))))
         vec)))

;; ===========================================================================
;; distill-event — the pure per-event assembler (the golden is taken over this)
;; ===========================================================================

(defn distill-event
  "Pure distillation of ONE parsed (redacted) event. Returns
   {:order :event-key :raw-type :class :production-event :parts :blocks}.
   Blocks carry :block-path :part-path :part-index :block-index :unit-kind
   :start-offset :end-offset :text :text-hash :actor. Debris events yield no
   blocks (classified, retained, unmarkable — T9). NO IPC; the driver (Phase 1)
   turns parts→surfaces and blocks→units/anchors."
  [parsed order]
  (let [raw-type (event-type* parsed)
        class (classify-event parsed raw-type)
        parts (event-parts parsed)
        river? (= :river (:class class))
        blocks (when river?
                 (vec (mapcat
                       (fn [part]
                         (let [actor (resolve-actor parsed part)]
                           (map-indexed
                            (fn [bi b]
                              (assoc b
                                     :part-path (:part-path part)
                                     :part-index (:part-index part)
                                     :block-index bi
                                     :block-path (block-path order (:part-index part) bi)
                                     :actor actor
                                     :text-hash (oc/source-hash (:text b))))
                            (free-cut-part part))))
                       parts)))]
    {:order order
     :event-key (event-key parsed order)
     :raw-type raw-type
     :class class
     :production-event (production-event parsed class (event-actor parsed))
     :parts parts
     :blocks (vec blocks)}))

;; ===========================================================================
;; §G · Row builders (pure). SPEC nouns → container-kernel rows (CONTRACT §4).
;; Mirror transcript_adapter.clj:221-623 keys + routing exactly (T4). The driver
;; supplies ids/time/actor; these assemble the OC import payload. Record ctors
;; are POSITIONAL — field order is the shape trap (object_container.clj:82-124).
;; ===========================================================================

(defn per-part-source-ref
  "Stable descriptive ref STORED on a per-part surface. Never routed (routing is
   by source-id via extract-object-key), so any deterministic value is safe — it
   is not an id extract-object-key must recognize."
  [event-uuid part-path]
  (str "tr-part:" event-uuid ":" part-path))

(def import-actor-id
  "The distiller's OC-importer identity (git_spine's `import:<name>` convention).
   DISTINCT from the RK mechanical-asserter — this is the requester of the import,
   not the asserter of a relation; content authorship rides `created-by` per row."
  "import:sense-block-v0")

(defn import-actor
  "The distiller's importer identity. `:system` type (core.clj:48 valid set
   #{:human :agent :system :bot} — :machine is RK-only) + the import capability ⇒
   authorized-request? passes (core.clj:343-350)."
  []
  {:actor/id import-actor-id
   :actor/type :system
   :actor/capabilities #{:object-container/import-material :source/ingest :object/edit}})

(defn part-rows
  "Build {:surface :units :anchors} for ONE part (SPEC §1: one part = one surface).
   - surface: per-part `SourceArtifactRow`, source-format :transcript (skips the
     markdown hash-check, §3.3), source-raw-text = the exact redacted part text
     (R4/G6), created-by = the part's resolved actor (G1 home). `:production-event`
     is assoc'd as an EXTRA record key — the delegation home (F2 option A; the F2
     spike proves it survives the $$source-artifacts-by-id round-trip).
   - units: one `DerivedUnitRow` per block; derived-content-text = the exact span
     (N5 verified cache — read-unit renders it, no query topology exposes offsets;
     G6 hash-checks it). unit-kind = SPEC §4.6 form value (T14 vocab, not schema).
   - anchors: EXACTLY one per unit, target-id = unit-id (import validation
     :785-795 needs the 1:1 pairing); offsets are UTF-16 into the part text (R2)."
  [{:keys [object-key event-uuid document-container-id event-id created-at-ms
           production-event]}
   part actor blocks]
  (let [part-path (:part-path part)
        text (str (:text part))
        source-id (per-part-source-id object-key event-uuid part-path)
        source-ref (per-part-source-ref event-uuid part-path)
        text-hash (oc/source-hash text)
        surface (assoc (oc/->SourceArtifactRow
                        source-id source-ref text-hash :transcript text
                        document-container-id
                        (long (count (.getBytes text "UTF-8")))
                        (long (or created-at-ms 0)) actor event-id)
                       :production-event production-event)
        units (mapv
               (fn [b]
                 (let [unit-id (derived-unit-id object-key (:block-path b))
                       btext (str (:text b))]
                   (oc/->DerivedUnitRow
                    unit-id document-container-id source-id (:unit-kind b) (:block-path b)
                    nil                                   ; parent-slot-id: containment computed (§0/SPEC §6.2)
                    (oc/source-anchor-id unit-id)         ; "sa:" unit-id
                    btext (oc/source-hash btext)          ; derived-content-text + hash (N5/G6)
                    distiller-id distiller-version event-id)))
               blocks)
        anchors (mapv
                 (fn [b]
                   (let [unit-id (derived-unit-id object-key (:block-path b))]
                     (oc/->SourceAnchorRow
                      (oc/source-anchor-id unit-id) :derived-unit unit-id
                      source-id source-ref text-hash
                      (long (:start-offset b)) (long (:end-offset b)) (:block-path b) event-id)))
                 blocks)]
    {:surface surface :units units :anchors anchors}))

(defn import-payload
  "The 10-key OC import payload (mirror transcript_adapter.clj:522-531). Empty
   :object-containers (containment computed, §0), :composition-edges (SPEC §6.2
   MUST-NOT-store), :revisions/:source-versions/:source-line-statuses.
   :projection-hints carry the class ledger (added in Phase 2; [] here)."
  [object-key surfaces units anchors projection-hints]
  {:object-key           object-key
   :source-artifacts     (vec surfaces)
   :object-containers    []
   :revisions            []
   :derived-units        (vec units)
   :source-anchors       (vec anchors)
   :composition-edges    []
   :source-versions      []
   :projection-hints     (vec projection-hints)
   :source-line-statuses []})

(defn request-id-for
  "Deterministic per-event request-id (shared by import-request and the class
   projection hint so their provenance agrees)."
  [object-key event-uuid]
  (str "req:sense-block:" object-key ":" (core/sha-256 (str event-uuid))))

(defn class-projection-hint
  "A river/debris class-ledger entry for the conversation projection (SPEC §3.2:
   debris/river retained + VISIBLE). Reuses the EXISTING `:transcript-conversation-
   projection` projection-kind + `entry-kind` field (T14 — new VALUE only, no new
   row type / case> edit; dispatch writes it verbatim, object_container.clj:2160-
   2171). The `sb:`-namespaced order-key co-tenants under the same conversation key
   WITHOUT colliding with the transcript adapter's `%020d:` :message rows, and the
   driver's F3 read filters on `entry-kind :message` so it never re-ingests these."
  [{:keys [conv-id order event-uuid event-id role content-preview]} imp-key request-id entry-kind]
  (oc/->TranscriptConversationProjectionRow
   :transcript-conversation-projection
   conv-id
   (str "sb:" (format "%020d" (long order)))
   entry-kind
   nil nil nil nil nil nil        ; container/revision/anchor/source/ref/line-key — a class marker, not a container ref
   event-id request-id imp-key
   event-uuid role content-preview
   nil))

(defn import-request
  "One OC import request per river event (mirror transcript_adapter.clj:602-623).
   Deterministic request-id/import-key ⇒ re-runs converge (G4). Fingerprint via
   the PUBLIC oc/import-material-fingerprint (:1524; the topology trusts + compares
   the request's fingerprint for dedup at :1831-1835, so determinism is all G4
   needs). Actor carries the import capability (T4 routing = :partition/key)."
  [object-key event-uuid payload {:keys [time-ms] :as _opts}]
  (let [imp-key    (import-key object-key event-uuid)
        request-id (request-id-for object-key event-uuid)
        now        (long (or time-ms 0))
        fingerprint (oc/import-material-fingerprint object-key imp-key payload)]
    (assoc (core/action-request
            {:request-id   request-id
             :request-type :object-container/import-material
             :time-ms      now
             :actor        (import-actor)
             :target       {:target/kind :object-container-import
                            :target/id imp-key
                            :target/address {:object/key object-key}}
             :action       {:action/type :object-container/import-material
                            :action/capability :object-container/import-material
                            :action/params {:source/format :transcript}}
             :routing/key  [:object-container/import object-key]
             :payload      payload
             :provenance   {:source/type :transcript}})
           :partition/key      object-key
           :object/key         object-key
           :import/key         imp-key
           :idempotency/key    imp-key
           :material/fingerprint fingerprint)))

;; ===========================================================================
;; §I · Driver (side-effecting, foreign client — T3/T11). Reads already-ingested
;; transcript sources (R4: no re-ingest / no second redaction), distills river
;; events, submits OC import requests. Barrier = :append-ack + await the
;; per-request decision (object_container_test's append-and-await pattern).
;; ===========================================================================

(defn safe-read-payload
  "read-string a stored redacted payload, guarding the F1 non-EDN shape
   (whitespace-key debris) — a throw ⇒ nil ⇒ classified debris, never cut. F1 was
   falsified on the real 7c80ce2a (river + file-history-snapshot both round-trip),
   so this catch is defense-in-depth, not the common path."
  [payload-str]
  (try (edn/read-string (str payload-str)) (catch Exception _ nil)))

(defn event-ctx
  "Per-event OC import context (ids / time / delegation home). ONE source of truth
   for event-id + document-container-id derivation, shared by the P1 surface import
   (event-import-request) and the P3b demand-mint of tool_result endpoint blocks
   (coarse-block-import). Pure — the event-uuid is the distilled event's key."
  [object-key distilled]
  (let [event-uuid (:event-key distilled)]
    {:object-key            object-key
     :event-uuid            event-uuid
     :document-container-id (tid/chat-message-id object-key (core/sha-256 (str event-uuid)))
     :event-id             (str "evt:" object-key ":" (core/sha-256 (str "sense-block " event-uuid)))
     :created-at-ms        (or (get-in distilled [:production-event :production/time-ms]) 0)
     :production-event     (:production-event distilled)}))

(defn event-material
  "Assemble ALL per-part surfaces/units/anchors for ONE distilled river event.
   One surface per NON-EMPTY part (SPEC §1); block-bearing parts always have
   non-empty text, so every unit's anchor.source-id is a declared surface
   (import validation :789). Empty parts (e.g. an image placeholder) mint nothing
   in P1 — a P1 scope note, revisited if the benchmark wants pasted images."
  [parsed distilled ctx]
  (let [blocks-by-part (group-by :part-path (:blocks distilled))]
    (reduce
     (fn [acc part]
       (if (str/blank? (str (:text part)))
         acc
         (let [actor (resolve-actor parsed part)
               {:keys [surface units anchors]}
               (part-rows ctx part actor (get blocks-by-part (:part-path part) []))]
           (-> acc
               (update :surfaces conj surface)
               (update :units into units)
               (update :anchors into anchors)))))
     {:surfaces [] :units [] :anchors []}
     (:parts distilled))))

(defn event-import-request
  "Build the OC import request for ONE event (nil for debris / no-surface events).
   Classify-first: only river events are cut (F1 — a debris payload is never
   turned into rows even if it read-string'd fine)."
  [object-key parsed order]
  (let [distilled (distill-event parsed order)]
    (when (= :river (get-in distilled [:class :class]))
      (let [event-uuid (:event-key distilled)
            imp-key    (import-key object-key event-uuid)
            request-id (request-id-for object-key event-uuid)
            ctx        (event-ctx object-key distilled)
            {:keys [surfaces units anchors]} (event-material parsed distilled ctx)]
        (when (seq surfaces)
          (let [hint (class-projection-hint
                      {:conv-id (tid/chat-conversation-id object-key)
                       :order order
                       :event-uuid event-uuid
                       :event-id (:event-id ctx)
                       :role (message-role parsed)
                       :content-preview (get-in distilled [:production-event :production/actor])}
                      imp-key request-id :river)]
            (import-request object-key event-uuid
                            (import-payload object-key surfaces units anchors [hint])
                            {:time-ms (:created-at-ms ctx)})))))))

(defn class-hint-import-request
  "Hint-only OC import carrying ONE event's durable versioned class row (SPEC §3.1),
   for an event that produces NO surface import: every debris event, AND the rare
   river event whose only part is empty (e.g. an empty tool_result — river-class,
   zero blocks; Gate-R2 Finding 1). entry-kind = the event's ACTUAL class; the
   classifier-id + reason ride in :content-preview so a physical reader tells a
   classified event from a never-classified one. Returns {:request <import-request>
   :class <:river|:debris>}. A normal river event carries its class hint on its
   surface import (event-import-request) and never reaches here."
  ([object-key parsed order]
   (class-hint-import-request object-key parsed order nil))
  ;; entry-kind-override (first-light A P2, flag D): the episode distill
  ;; classes a natively-minted user text event :native — its material lives
  ;; under imp:ep:, so no surface import here, but the class ledger stays
  ;; total (SPEC §3.1). river-page ignores it (river-ledger-row? demands
  ;; :river); the merge lane renders the NATIVE units instead. Same
  ;; deterministic keys → re-runs converge (T8/G4).
  ([object-key parsed order entry-kind-override]
   (let [distilled  (distill-event parsed order)
         class-map  (:class distilled)
         clazz      (:class class-map)
         entry-kind (or entry-kind-override clazz)
         event-uuid (:event-key distilled)
         imp-key    (import-key object-key event-uuid)
         request-id (request-id-for object-key event-uuid)
         ctx        (event-ctx object-key distilled)
         hint (class-projection-hint
               {:conv-id (tid/chat-conversation-id object-key)
                :order order
                :event-uuid event-uuid
                :event-id (:event-id ctx)
                :role (message-role parsed)
                :content-preview (if entry-kind-override
                                   (str episode-native-classifier-id "/native")
                                   (str (:classifier-id class-map) "/" (name (:reason class-map))))}
               imp-key request-id entry-kind)]
     {:request (import-request object-key event-uuid
                               (import-payload object-key [] [] [] [hint])
                               {:time-ms (:created-at-ms ctx)})
      :class clazz})))

(defn read-conversation-inputs
  "Physically enumerate a conversation's ordered per-message stored payloads (F3:
   no query-topology door exists). Reads $$transcript-conversation-projection by
   conversation-container-id (order-key order = file order) → :message entries →
   $$source-artifacts-by-id per source-id → source-raw-text (= pr-str payload). A
   foreign-client read of the kernel substrate (normal); NOT the G13-governed
   OUTPUT path (that is river-page, query topologies only)."
  [oc-rt object-key]
  (let [conv-id (tid/chat-conversation-id object-key)
        projection (ocr/read-transcript-conversation-projection oc-rt conv-id "" 100000)
        message-rows (filter #(= :message (:entry-kind %)) projection)]
    (vec
     (map-indexed
      (fn [order row]
        (let [src (ocr/read-source oc-rt (:source-id row))]
          {:order order
           :source-id (:source-id row)
           :payload-str (:source-raw-text src)}))
      message-rows))))

;; ===========================================================================
;; §J · Mechanical edge floor driver (side-effecting, foreign — T3/T11). SPEC
;; §11.3, gates G8/G9. Pairs (tool_use, tool_result) across the conversation and
;; asserts produced/grounds edges into the RELATION kernel. The one Sid-ruled fork
;; (N3, 2026-07-09): the tool_result endpoint is a demand-minted coarse
;; tool-result-span block (option A) — SPEC §5.1 lazy-on-engagement (the edge IS
;; the engagement), NOT eager pre-chunking of §4.3's dump. RK is MICROBATCH: the
;; driver appends :append-ack; the deterministic processed-count barrier lives in
;; the TEST (product code never depends on the test harness).
;; ===========================================================================

(defn- tool-result-part
  "The tool_result part (by tool-use-id) inside a distilled event's parts."
  [distilled tool-use-id]
  (first (filter #(and (= :tool-result (:part-type %)) (= tool-use-id (:tool-use-id %)))
                 (:parts distilled))))

(defn coarse-block-import
  "N3 option A (Sid 2026-07-09): the ONE coarse whole-span block (form
   :tool-result-span, span [0,len)) for a tool_result, demand-minted BY the floor
   edge needing a :block endpoint. An OC import over the tool_result's EXISTING
   surface (part-rows re-declares it idempotently) + the coarse unit + its anchor.
   The import-key is DISTINCT from the P1 event import of the same surface (a
   synthetic import identity) so the OC journal does not dedup this as a P1 replay —
   else the coarse block would never land."
  [object-key parsed distilled part]
  (let [ctx   (event-ctx object-key distilled)
        text  (str (:text part))
        block {:block-path (block-path (:order distilled) (:part-index part) 0)
               :unit-kind :tool-result-span
               :text text :start-offset 0 :end-offset (count text)
               :text-hash (oc/source-hash text)}
        actor (resolve-actor parsed part)                 ; "tool" (T1)
        {:keys [surface units anchors]} (part-rows ctx part actor [block])
        endpoint-uuid (str (:event-uuid ctx) ":tool-result-span:" (:part-path part))]
    (import-request object-key endpoint-uuid
                    (import-payload object-key [surface] units anchors [])
                    {:time-ms (:created-at-ms ctx)})))

(defn mechanical-edge-request
  "Turn ONE pure edge-plan entry into an rk/assert-request. Asserter
   sense-block/mechanical@1 (silver by asserter — N6, no tier field); asserted/sent
   at the tool_use event's production time (replay-stable, never wall-clock);
   request-id == relation-scoped idempotency key ⇒ a re-run journal-replays and
   writes nothing (G4-style)."
  [{:keys [kind from-ref to-ref production-time-ms idempotency-key]}]
  (rk/assert-request {:kind kind :from from-ref :to to-ref
                      :asserter-actor-id mechanical-asserter
                      :asserter-type mechanical-asserter-type
                      :asserted-at-ms production-time-ms
                      :sent-at-ms production-time-ms
                      :request-id idempotency-key
                      :idempotency-key idempotency-key}))

(defn assert-mechanical-edges!
  "P3b (SPEC §11.3; gates G8/G9). Re-distills the conversation's river events (pure;
   reuses the already-read `inputs`), plans the floor edges (pure mechanical-edge-
   plan), then: (a) demand-mints the paired tool_result endpoint blocks via OC
   import (await-decision so the endpoint resolves to a real block before the edge);
   (b) appends each produced/grounds edge into the RELATION kernel (:append-ack).
   Unpaired write/read tool_use → its edge points at a HOLE (no coarse block minted;
   G9). Returns {:edges plan :edge-count N} to merge into the driver summary."
  [{:keys [oc-rt rk-rt object-key inputs]}]
  (let [river (keep (fn [{:keys [order payload-str]}]
                      (let [parsed (safe-read-payload payload-str)
                            d      (distill-event parsed order)]
                        (when (= :river (get-in d [:class :class]))
                          {:parsed parsed :distilled d})))
                    inputs)
        by-order (into {} (map (fn [{:keys [distilled] :as e}] [(:order distilled) e]) river))
        plan     (mechanical-edge-plan object-key (map :distilled river))]
    ;; (a) demand-mint the paired tool_result endpoint blocks (option A; OC import).
    (doseq [{:keys [tool-use-id result]} plan :when result]
      (let [{:keys [parsed distilled]} (by-order (:order result))
            part (tool-result-part distilled tool-use-id)]
        (when part
          (let [req (coarse-block-import object-key parsed distilled part)]
            (ocr/append-object-container-request! oc-rt req)
            (ocr/await-object-container-decision oc-rt req 20000)))))
    ;; (b) assert the floor edges (RK microbatch; barrier is the test's, not here).
    (doseq [e plan]
      (rk/append-relation-request! rk-rt (mechanical-edge-request e)))
    {:edges plan :edge-count (count plan)}))

(defn distill-conversation!
  "Drive one already-ingested conversation: read its stored per-message payloads
   (F3) → classify-first → distill river events → submit one OC import per river
   event (:append-ack; await the per-request decision). Debris is skipped (it is
   retained upstream by the transcript ingest; its river/debris ledger is Phase 2).
   When an `:rk-rt` (relation-kernel runtime) is supplied, ALSO runs the P3b
   mechanical edge floor (SPEC §11.3, gates G8/G9). When a `:skip-event?`
   predicate is supplied ((fn [parsed distilled] -> bool) — first-light A P2,
   flag D), matching events mint NO surface import: they get a class-only
   :native ledger row (their material is already durable under imp:ep:) and
   count under :native in the summary. Returns
   {:object-key :requests :decisions :river :debris :native :edges :edge-count}."
  [{:keys [oc-rt rk-rt source conversation-id skip-event?]}]
  (let [object-key (tid/transcript-object-key source conversation-id)
        inputs (read-conversation-inputs oc-rt object-key)
        summary (reduce
                 (fn [acc {:keys [order payload-str]}]
                   (let [parsed  (safe-read-payload payload-str)
                         skip?   (boolean (and skip-event? parsed
                                               (skip-event? parsed (distill-event parsed order))))
                         request (when-not skip?
                                   (event-import-request object-key parsed order))]
                     (if request
                       (do (ocr/append-object-container-request! oc-rt request)
                           (-> acc
                               (update :requests conj request)
                               (update :decisions conj (ocr/await-object-container-decision oc-rt request 20000))
                               (update :river inc)))
                       ;; F3 + Gate-R2 Finding 1: no surface import → this event still
                       ;; needs a DURABLE versioned class row (SPEC §3.1). True for
                       ;; every debris event AND the rare river event whose only part
                       ;; is empty (e.g. an empty tool_result) AND (flag D) every
                       ;; skip-event? match (:native — material durable under imp:ep:).
                       ;; Count by ACTUAL class, :native counted separately.
                       (let [{hreq :request clazz :class}
                             (class-hint-import-request object-key parsed order
                                                        (when skip? :native))]
                         (ocr/append-object-container-request! oc-rt hreq)
                         (-> acc
                             (update (cond skip? :native
                                           (= :river clazz) :river
                                           :else :debris) inc)
                             (update :class-hint-decisions conj
                                     (ocr/await-object-container-decision oc-rt hreq 20000)))))))
                 {:object-key object-key :requests [] :decisions [] :river 0 :debris 0 :native 0 :class-hint-decisions []}
                 inputs)]
    (if rk-rt
      (merge summary (assert-mechanical-edges! {:oc-rt oc-rt :rk-rt rk-rt
                                                :object-key object-key :inputs inputs}))
      (assoc summary :edges [] :edge-count 0))))

;; ===========================================================================
;; §K · Refinement (side-effecting, foreign — T3/T11). SPEC §5 demand law, gate G7.
;; Demand-mint a FINER block inside an existing coarse block, over the SAME surface.
;; Adds, never invalidates (§5.2 — the coarse block + its marks persist). Identity by
;; (surface, sub-span) (§6.1) — a re-refine of the same span RESOLVES to the same id.
;; The engagement is the refinement's provenance (§5.1 MUST) and rides the :refines
;; edge's declared :note field (no new row).
;; ===========================================================================

(defn refine-block-path
  "block-path for a refined unit, deterministic from (surface, sub-span). The
   `refine:` segment is disjoint from the free-cut's `%06d:%02d:%06d` paths (N1c — no
   unit-id collision) and from the `pending-result:` hole ids. Because it is a pure fn
   of (surface-id, span), a second refine of the SAME sub-span over the SAME surface
   resolves to the SAME unit-id (SPEC §6.1 identity-by-construction; R3 span identity)."
  [surface-id start end]
  (str "refine:" (core/sha-256 (str surface-id)) ":"
       (format "%010d" (long start)) "-" (format "%010d" (long end))))

(defn composition-edge-request
  "rk/assert-request for a composition edge (:refines §5 / :assembled-from §8). Like
   mechanical-edge-request, but carries the engagement/occurrence in the RelationEdge's
   declared :note field (SPEC §5.1 / §8.3 — a home that needs NO new row). Silver by
   asserter (N6); replay-stable time (never the wall clock); request-id == the
   relation-scoped idempotency key ⇒ a re-run journal-replays and writes nothing."
  [{:keys [kind from-ref to-ref time-ms idempotency-key note]}]
  (rk/assert-request {:kind kind :from from-ref :to to-ref
                      :asserter-actor-id mechanical-asserter
                      :asserter-type mechanical-asserter-type
                      :asserted-at-ms time-ms :sent-at-ms time-ms
                      :request-id idempotency-key :idempotency-key idempotency-key
                      :note note}))

(def refine-resolve-scan-limit
  "Per-surface unit-scan bound for refine! identity resolution. A surface is ONE
   conversation part; its distiller units are few — this guard is never hit in
   practice. Not a page hot-path (refine! is a demand op)."
  100000)

(defn- resolve-existing-unit-at-span
  "SPEC §6.1: identity = (surface-id, span). If a sense-block-v0 unit already
   occupies (surface-id, [start,end)) — e.g. a free-cut structural sub-block whose
   POSITIONAL id differs from the refine: id — return that unit-id so refine! REUSES
   it instead of minting a duplicate identity (the F4 bug). nil when the span is
   genuinely new. Enumerates the surface's derived-unit refs (one range scan;
   target-id encodes the distiller, so foreign strata are rejected without a read),
   then reads each candidate's anchor by unit-id ($$source-anchors-by-target holds
   the full row with offsets; the by-source index stores span-less refs) and matches
   the exact [start,end). Foreign-read class (like the coarse-unit read), NOT a G13
   output path."
  [oc-rt source-id object-key start end]
  (let [du-seg (str ":" distiller-id ":")
        bundle (ocr/read-common-material-for-source
                oc-rt source-id [:derived-units] {} refine-resolve-scan-limit)
        candidate-ids (->> (:derived-units bundle)
                           (map :target-id)
                           (filter #(str/includes? (str %) du-seg)))]
    (some (fn [uid]
            (let [a (first (ocr/read-source-anchors oc-rt uid))]
              (when (and a
                         (= (long start) (long (:start-offset a)))
                         (= (long end) (long (:end-offset a))))
                uid)))
          candidate-ids)))

(defn refine!
  "SPEC §5 demand law (gate G7). Mint a FINER block inside the coarse block
   `:coarse-unit-id`, over the SAME surface, spanning `:sub-span` = [start end)
   ABSOLUTE UTF-16 offsets into the surface. MUST be a non-empty, surrogate-safe,
   proper child of the coarse block (coarse.start ≤ start < end ≤ coarse.end; G11).
   Adds, never invalidates (§5.2): refine! rewrites neither the coarse unit nor any
   sibling unit — only the surface is re-declared BYTE-IDENTICALLY (the OC source write
   is a termval overwrite keyed by source-id, object_container.clj:1950, so the stored
   surface row is read back and passed through unchanged) and the finer unit + its
   anchor are added under a new key. Identity by (surface, sub-span) (§6.1) ⇒ a
   re-refine of the same span resolves to the same finer unit-id. `:engagement` is the
   refinement's provenance (§5.1 MUST) — it rides the :refines edge's :note. `:form`
   overrides the finer block's form (default: inherit the coarse block's form).
   `:asserted-at-ms` stamps the edge (default: the surface's created-at-ms; never the
   wall clock).

   Reads (foreign-client substrate reads — the read-conversation-inputs class, NOT a
   G13 OUTPUT path): the coarse unit (ocr/read-unit → source-id + form), its anchor
   ($$source-anchors-by-target → the coarse span), and the surface ($$source-artifacts-
   by-id → raw-text + the row to re-declare). Writes: ONE OC import (surface + finer
   unit + finer anchor; DISTINCT import-key so the OC journal does not dedup it as a P1
   replay), then ONE :refines edge finer→coarse (RK microbatch; the caller/test barriers
   on processed-count). Returns {:finer-unit-id :coarse-unit-id :surface-id :relation-id
   :note :edge-count :decision}."
  [{:keys [oc-rt rk-rt coarse-unit-id sub-span engagement form asserted-at-ms]}]
  (let [[raw-start raw-end] sub-span
        sub-start   (long raw-start)
        sub-end     (long raw-end)
        object-key  (oc/extract-object-key coarse-unit-id)
        coarse-unit (:unit (ocr/read-unit oc-rt coarse-unit-id))
        _ (when (nil? coarse-unit)
            (throw (ex-info "refine!: coarse block absent from $$derived-units-by-id"
                            {:coarse-unit-id coarse-unit-id})))
        coarse-anchor (first (ocr/read-source-anchors oc-rt coarse-unit-id))
        _ (when (nil? coarse-anchor)
            (throw (ex-info "refine!: coarse block has no anchor (cannot resolve surface/span)"
                            {:coarse-unit-id coarse-unit-id})))
        source-id    (:source-id coarse-anchor)
        coarse-start (long (:start-offset coarse-anchor))
        coarse-end   (long (:end-offset coarse-anchor))
        _ (when-not (and (<= coarse-start sub-start) (< sub-start sub-end) (<= sub-end coarse-end))
            (throw (ex-info "refine!: sub-span is not a non-empty proper child of the coarse block (G11/§5.1)"
                            {:coarse-unit-id coarse-unit-id
                             :coarse-span [coarse-start coarse-end] :sub-span [sub-start sub-end]})))
        surface (ocr/read-source oc-rt source-id)
        _ (when (nil? surface)
            (throw (ex-info "refine!: surface absent from $$source-artifacts-by-id" {:source-id source-id})))
        raw (str (:source-raw-text surface))
        splits-surrogate? (fn [pos]
                            (and (pos? (long pos)) (< (long pos) (count raw))
                                 (Character/isHighSurrogate (.charAt raw (dec (long pos))))
                                 (Character/isLowSurrogate (.charAt raw (long pos)))))
        _ (when (or (splits-surrogate? sub-start) (splits-surrogate? sub-end))
            (throw (ex-info "refine!: sub-span splits a surrogate pair (G11)"
                            {:sub-span [sub-start sub-end]})))
        time-ms    (long (or asserted-at-ms (:created-at-ms surface) 0))
        ;; F4 / SPEC §6.1: (surface-id, span) IS the identity. If a unit already
        ;; occupies this exact span — e.g. a free-cut structural sub-block, whose
        ;; positional id differs from the refine: id — REUSE it; minting a second
        ;; id for one identity was the F4 duplicate. Only mint when the span is new.
        existing-uid (resolve-existing-unit-at-span oc-rt source-id object-key sub-start sub-end)
        _ (when (= existing-uid coarse-unit-id)
            (throw (ex-info "refine!: sub-span equals the coarse block's span — not a refinement; §6.1 resolves it to the coarse unit itself"
                            {:coarse-unit-id coarse-unit-id :sub-span [sub-start sub-end]})))
        reuse?     (some? existing-uid)
        bpath      (refine-block-path source-id sub-start sub-end)
        finer-uid  (or existing-uid (derived-unit-id object-key bpath))
        decision   (when-not reuse?
                     (let [finer-text (subs raw sub-start sub-end)
                           finer-form (or form (:unit-kind coarse-unit))
                           anchor-id  (oc/source-anchor-id finer-uid)
                           finer-unit (oc/->DerivedUnitRow
                                       finer-uid (:document-container-id surface) source-id
                                       finer-form bpath nil
                                       anchor-id finer-text (oc/source-hash finer-text)
                                       distiller-id distiller-version (:event-id surface))
                           finer-anchor (oc/->SourceAnchorRow
                                         anchor-id :derived-unit finer-uid
                                         source-id (:source-ref surface) (:source-hash surface)
                                         sub-start sub-end bpath (:event-id surface))
                           endpoint-uuid (str "refine:" source-id ":" sub-start "-" sub-end)
                           oc-req   (import-request object-key endpoint-uuid
                                                    (import-payload object-key [surface] [finer-unit] [finer-anchor] [])
                                                    {:time-ms time-ms})]
                       (ocr/append-object-container-request! oc-rt oc-req)
                       (ocr/await-object-container-decision oc-rt oc-req 20000)))
        from-ref (rk/->target-ref :block finer-uid)
        to-ref   (rk/->target-ref :block coarse-unit-id)
        idem     (edge-idempotency-key finer-uid :refines coarse-unit-id)
        note     (when (some? engagement)
                   (if (string? engagement) engagement (pr-str engagement)))
        edge-req (composition-edge-request {:kind :refines :from-ref from-ref :to-ref to-ref
                                            :time-ms time-ms :idempotency-key idem :note note})]
    (rk/append-relation-request! rk-rt edge-req)
    {:finer-unit-id  finer-uid
     :coarse-unit-id coarse-unit-id
     :surface-id     source-id
     :resolved?      reuse?
     :relation-id    (rk/relation-id-for :refines from-ref to-ref mechanical-asserter)
     :note           note
     :edge-count     1
     :decision       decision}))

;; ===========================================================================
;; §L · Assembly (side-effecting, foreign — T3/T11). SPEC §8 composition, gate G10.
;; Assembly IS a production event: inputs = blocks, output = a NEW `assembled` surface
;; + one :assembled-from edge to EVERY source block. Transclude by default (§8.2): the
;; source blocks are RE-ADDRESSED via edges, never copied — their rows are untouched
;; (G10). The edge carries the source block's POSITION in :note, so the edge IS the
;; occurrence record (block-id, assembly-surface, position) (§8.3) for v0. A first-class
;; occurrence ROW would be a new row TYPE (T14 stop-clause) + a marks-layer extension
;; (CONTRACT §10) — NOT built here.
;; ===========================================================================

(def assembled-source-format
  "OC source-format for an assembled surface. NON-:markdown ⇒ the OC skips its
   source-hash check (object_container.clj:735), and no `known-format` gate exists
   (:715-805). The SPEC §4.6/§8 form `assembled` lives on the assembled unit's
   unit-kind — source-format is the OC storage tag, unit-kind is the sense-line form."
  :assembled)

(defn assembled-source-id
  "src:tr:<object-key>:<hash(block-ids)> — reuses the handled `src:tr:` prefix so
   extract-object-key routes the assembly to the (first block's) object-key task (N1b).
   Deterministic from the ordered source blocks ⇒ re-assembling the same blocks in the
   same order resolves to the same surface (idempotent)."
  [object-key block-ids]
  (str "src:tr:" object-key ":" (core/sha-256 (str "assembled " (str/join "|" block-ids)))))

(defn assemble!
  "SPEC §8.1 (gate G10). Assemble `:block-ids` (source block unit-ids, in order) into a
   NEW `assembled` surface, with one :assembled-from edge per source block. Transclude
   by default (§8.2): the source blocks are RE-ADDRESSED (edges), never copied — G10
   asserts their rows stay byte-identical. `:text` optionally supplies the assembled
   material (the §8.2 'new words written' case); default = the source blocks'
   derived-content-texts joined with newlines, in order (a pure-transclusion rendering).
   `:asserted-at-ms` stamps the edges (default 0; never the wall clock). The assembly
   co-tenants under the FIRST source block's object-key (v0: one chat; the surface-id is
   distinct, so this is routing/colocation only — cross-object assembly still works, the
   RK target copies hop per endpoint). Returns {:assembled-source-id :assembled-unit-id
   :assembled-from :edge-count :decision}."
  [{:keys [oc-rt rk-rt block-ids text asserted-at-ms]}]
  (when (empty? block-ids)
    (throw (ex-info "assemble!: no source blocks" {})))
  (let [object-key   (oc/extract-object-key (first block-ids))
        source-units (mapv (fn [bid]
                             (or (:unit (ocr/read-unit oc-rt bid))
                                 (throw (ex-info "assemble!: source block absent" {:block-id bid}))))
                           block-ids)
        asm-text (or text (str/join "\n" (map #(str (:derived-content-text %)) source-units)))
        _ (when (str/blank? asm-text)
            (throw (ex-info "assemble!: assembled material is empty (G11 non-empty)" {:block-ids block-ids})))
        asm-src-id   (assembled-source-id object-key block-ids)
        bpath        (str "assembled:" (core/sha-256 (str/join "|" block-ids)))
        asm-uid      (derived-unit-id object-key bpath)
        asm-ref      (str "assembled:" bpath)
        dci          (tid/chat-message-id object-key (core/sha-256 asm-src-id))
        event-id     (str "evt:" object-key ":" (core/sha-256 (str "assembled " asm-src-id)))
        content-hash (oc/source-hash asm-text)
        time-ms      (long (or asserted-at-ms 0))
        anchor-id    (oc/source-anchor-id asm-uid)
        surface   (oc/->SourceArtifactRow
                   asm-src-id asm-ref content-hash assembled-source-format asm-text
                   dci (long (count (.getBytes asm-text "UTF-8"))) time-ms import-actor-id event-id)
        asm-unit  (oc/->DerivedUnitRow
                   asm-uid dci asm-src-id :assembled bpath nil
                   anchor-id asm-text content-hash distiller-id distiller-version event-id)
        asm-anchor (oc/->SourceAnchorRow
                    anchor-id :derived-unit asm-uid asm-src-id asm-ref content-hash
                    0 (count asm-text) bpath event-id)
        oc-req   (import-request object-key (str "assembled:" asm-src-id)
                                 (import-payload object-key [surface] [asm-unit] [asm-anchor] [])
                                 {:time-ms time-ms})
        _        (ocr/append-object-container-request! oc-rt oc-req)
        decision (ocr/await-object-container-decision oc-rt oc-req 20000)
        from-ref (rk/->target-ref :block asm-uid)
        edges    (vec (map-indexed
                       (fn [i bid]
                         (let [to-ref (rk/->target-ref :block bid)
                               idem   (edge-idempotency-key asm-uid :assembled-from bid)
                               note   (str "occurrence position " i)]
                           {:source-block-id bid :position i
                            :from-unit-id asm-uid :to-unit-id bid
                            :idempotency-key idem :note note
                            :relation-id (rk/relation-id-for :assembled-from from-ref to-ref mechanical-asserter)
                            :request (composition-edge-request {:kind :assembled-from :from-ref from-ref
                                                                :to-ref to-ref :time-ms time-ms
                                                                :idempotency-key idem :note note})}))
                       block-ids))]
    (doseq [e edges] (rk/append-relation-request! rk-rt (:request e)))
    {:assembled-source-id asm-src-id
     :assembled-unit-id   asm-uid
     :assembled-from      edges
     :edge-count          (count edges)
     :decision            decision}))

;; ===========================================================================
;; §M · River page (foreign read composition). SPEC §2.3/§3.2, gates G12/G13.
;; The class ledger and original transcript surface are INPUT substrate reads (F3).
;; Rendered block material is read ONLY through OC query topologies: the per-source
;; ordered unit index, then read-unit for the exact stored form/text (N5).
;; ===========================================================================

(def max-river-page-size
  "Hard v0 bound for the composition-first read plan. A page examines at most this
   many river events, per-part surfaces, and unit refs; the dedicated conversation-
   page query topology remains the CONTRACT §10 scale extension."
  64)

(def conversation-projection-scan-limit
  "The class ledger co-tenants with transcript rows and does not carry source ids.
   One bounded range seek supplies the ordered message lookup plus the `sb:` ledger;
   the real 7c80ce2a transcript is well below this v0 guard."
  100000)

(defn- river-ledger-order
  [row]
  (let [order-key (str (:order-key row))]
    (when (str/starts-with? order-key "sb:")
      (try
        (Long/parseLong (subs order-key 3))
        (catch NumberFormatException _ nil)))))

(defn- river-ledger-row?
  [row]
  (and (= :river (:entry-kind row))
       (some? (river-ledger-order row))))

(defn- page-limit
  [limit]
  (let [n (long (or limit 0))]
    (when-not (<= 1 n max-river-page-size)
      (throw (ex-info "river-page: limit must be within the bounded v0 page size"
                      {:limit limit :max-limit max-river-page-size})))
    n))

(defn- render-river-source
  "Read one per-part surface's ordered block refs through CommonMaterialBundle,
   resolve at most `remaining` refs through read-unit, and return stored block
   material. `block-path` is the material-ref order-key and therefore preserves
   the free cut's span order within this surface."
  [oc-rt source-id event-order event-uuid event part remaining]
  (let [bundle (ocr/read-common-material-for-source
                oc-rt source-id [:derived-units] {} remaining)
        du-seg (str ":" distiller-id ":")
        ;; F2/G12: filter to THIS distiller's refs BEFORE read-unit. The ref's
        ;; target-id encodes the distiller (du:<ok>:sense-block-v0:<path>), so a
        ;; foreign stratum sharing this surface is rejected with zero point-reads
        ;; — it can no longer inflate the per-page seek plan (was O(limit^2) when
        ;; foreign units were read then discarded). Keeps unit-reads <= page size.
        refs   (->> (:derived-units bundle)
                    (filter #(str/includes? (str (:target-id %)) du-seg))
                    (sort-by :order-key))
        actor  (resolve-actor event part)]
    {:unit-reads (count refs)
     :blocks
     (->> refs
          (keep (fn [ref]
                  (let [read-result (ocr/read-unit oc-rt (:target-id ref))
                        unit (:unit read-result)]
                    (when (and unit
                               (= source-id (:source-id unit))
                               (= distiller-id (:distiller-id unit)))
                      {:order       [event-order (:part-index part) (:order-key ref)]
                       :event-uuid  event-uuid
                       :actor       actor
                       :form        (:unit-kind unit)
                       ;; block-write S2 fix (INT, 2026-07-12): :text serves the
                       ;; UnitReadResult OVERLAY field — the graduation row's
                       ;; current content when the unit has been edited, the raw
                       ;; derived text otherwise (total, unit-read-result). The
                       ;; raw row field made every edit invisible to river-page
                       ;; (the import underneath is never mutated by design), so
                       ;; edited content could not survive a reboot (G8). Un-
                       ;; edited blocks: (:content-text read-result) is byte-
                       ;; identical to (:derived-content-text unit) — G9 holds.
                       :text        (:content-text read-result)
                       :unit-id     (:unit-id unit)
                       :source-id   source-id
                       :part-path   (:part-path part)
                       ;; block-write PHASE_0 rule 1 (additive projection field):
                       ;; carry the derived unit's OWN document-container-id so the
                       ;; edit envelope (CONTRACT §3) can stamp the graduated
                       ;; container's document parent WITHOUT the client computing
                       ;; or tracking any container id (BW-T7). No new read — `unit`
                       ;; is already read here, so the seek plan is unchanged (G9).
                       :document-container-id (:document-container-id unit)
                       :block-path  (:block-path unit)}))))
          (take remaining)
          vec)}))

(defn- native-utterance-row?
  "first-light A P2: a native episode row (entry-kind :episode-utterance,
   ep:-namespaced order-key) — material minted at utterance time under
   imp:ep:, enumerated from the SAME projection range read as everything
   else and rendered beside the river."
  [row]
  (and (= :episode-utterance (:entry-kind row))
       (str/starts-with? (str (:order-key row)) "ep:")))

(defn- render-native-source
  "Blocks for ONE native utterance row: source → this stratum's unit refs →
   read-unit (G13 paths — same seek shape as render-river-source; the
   graduation overlay serves edited content with the same honesty). Actor =
   the row's role slot (the native lane stores the ACTOR id there — sid);
   :lane :episode marks the block for the projection's time merge."
  [oc-rt row remaining]
  (let [source-id (:source-id row)
        bundle (ocr/read-common-material-for-source
                oc-rt source-id [:derived-units] {} remaining)
        du-seg (str ":" episode-native-classifier-id ":")
        refs   (->> (:derived-units bundle)
                    (filter #(str/includes? (str (:target-id %)) du-seg))
                    (sort-by :order-key))]
    {:unit-reads (count refs)
     :blocks
     (->> refs
          (keep (fn [ref]
                  (let [read-result (ocr/read-unit oc-rt (:target-id ref))
                        unit (:unit read-result)]
                    (when (and unit
                               (= source-id (:source-id unit))
                               (= episode-native-classifier-id (:distiller-id unit)))
                      {:order       [:ep (:order-key row) (:order-key ref)]
                       :event-uuid  (:message-uuid row)
                       :actor       (:role row)
                       :form        (:unit-kind unit)
                       :text        (:content-text read-result)
                       :unit-id     (:unit-id unit)
                       :source-id   source-id
                       :part-path   "utterance"
                       :document-container-id (:document-container-id unit)
                       :block-path  (:block-path unit)
                       :lane        :episode}))))
          (take remaining)
          vec)}))

(defn river-page
  "Render the first bounded page of a conversation's persisted river blocks.

   Input enumeration (F3, G13-exempt): one range read of the transcript conversation
   projection gives ordered `:message` rows plus the `sb:` river class ledger; each
   selected river event reads its already-redacted stored payload and deterministically
   re-derives per-part source ids. Output material (G13-governed) uses ONLY
   read-common-material-for-source + read-unit query topologies.

   `limit` bounds all three fan-out dimensions: at most `limit` river events, at
   most `limit` per-part source queries, and at most `limit` unit query invocations.
   The returned vector carries `:river-page/read-plan` metadata with the measured
   counts. Since read-unit performs the unit + graduation point reads, the v0 seek
   bound is 1 + events-read + surfaces-read + 2*unit-reads <= 1 + 4*limit (G12).
   The projection seek iterates sequentially; CommonMaterialBundle uses one subindexed
   range seek per selected surface."
  [{:keys [oc-rt object-key]} limit]
  (let [limit      (page-limit limit)
        conv-id    (tid/chat-conversation-id object-key)
        projection (vec (ocr/read-transcript-conversation-projection
                         oc-rt conv-id "" conversation-projection-scan-limit))
        _ (when (= conversation-projection-scan-limit (count projection))
            (throw (ex-info "river-page: conversation projection hit the v0 scan guard"
                            {:conversation-id conv-id
                             :scan-limit conversation-projection-scan-limit})))
        messages   (vec (filter #(= :message (:entry-kind %)) projection))
        all-river-rows (->> projection
                            (filter river-ledger-row?)
                            (sort-by river-ledger-order))
        total-river-events (count all-river-rows)
        river-rows (take limit all-river-rows)
        initial    {:blocks [] :events-read 0 :surfaces-read 0 :unit-reads 0}
        result
        (reduce
         (fn [{:keys [blocks surfaces-read] :as acc} ledger-row]
           (if (or (>= (count blocks) limit) (>= surfaces-read limit))
             (reduced acc)
             (let [order       (river-ledger-order ledger-row)
                   message-row (get messages order)
                   _ (when (nil? message-row)
                       (throw (ex-info "river-page: class-ledger order has no transcript message"
                                       {:conversation-id conv-id :order order
                                        :event-uuid (:message-uuid ledger-row)})))
                   source      (ocr/read-source oc-rt (:source-id message-row))
                   _ (when (nil? source)
                       (throw (ex-info "river-page: transcript input surface is absent"
                                       {:conversation-id conv-id :order order
                                        :source-id (:source-id message-row)})))
                   event       (safe-read-payload (:source-raw-text source))
                   distilled   (distill-event event order)
                   event-uuid  (or (:message-uuid ledger-row) (:event-key distilled))
                   parts       (remove #(str/blank? (str (:text %))) (:parts distilled))
                   event-acc   (update acc :events-read inc)]
               (reduce
                (fn [{:keys [blocks surfaces-read] :as part-acc} part]
                  (let [remaining (- limit (count blocks))]
                    (if (or (zero? remaining) (>= surfaces-read limit))
                      (reduced part-acc)
                      (let [source-id (per-part-source-id object-key event-uuid (:part-path part))
                            rendered  (render-river-source oc-rt source-id order event-uuid
                                                          event part remaining)]
                        (-> part-acc
                            (update :blocks into (:blocks rendered))
                            (update :surfaces-read inc)
                            (update :unit-reads + (:unit-reads rendered)))))))
                event-acc
                parts))))
         initial
         river-rows)
        ;; first-light A P2 — the native lane: episode-utterance rows from the
        ;; SAME projection read (zero extra range seeks), each source's units
        ;; rendered through the same G13 paths. Bounded by the SAME limit.
        native-rows (->> projection (filter native-utterance-row?) (sort-by :order-key))
        native (reduce
                (fn [{:keys [blocks surfaces-read] :as acc} row]
                  (if (or (>= (count blocks) limit) (>= surfaces-read limit))
                    (reduced acc)
                    (let [rendered (render-native-source oc-rt row (- limit (count blocks)))]
                      (-> acc
                          (update :blocks into (:blocks rendered))
                          (update :surfaces-read inc)
                          (update :unit-reads + (:unit-reads rendered))))))
                {:blocks [] :surfaces-read 0 :unit-reads 0}
                native-rows)
        seek-count (+ 1
                      (:events-read result)
                      (:surfaces-read result)
                      (* 2 (:unit-reads result))
                      (:surfaces-read native)
                      (* 2 (:unit-reads native)))
        blocks-returned (+ (min limit (count (:blocks result)))
                           (count (:blocks native)))
        ;; F2: never present a capped/short page as complete. Truncated when ANY
        ;; ceiling was hit — unrendered river events remain, or the surface/block
        ;; cap stopped the walk (either lane). A consumer re-pages until
        ;; :truncated? is false (a cursor/dedicated query is the CONTRACT §10
        ;; scale extension, not v0).
        truncated? (boolean (or (> total-river-events (:events-read result))
                                (>= (:surfaces-read result) limit)
                                (>= (count (:blocks result)) limit)
                                (> (count native-rows) (:surfaces-read native))
                                (>= (count (:blocks native)) limit)))
        read-plan  {:projection-range-seeks 1
                    :projection-rows-iterated (count projection)
                    :events-read (:events-read result)
                    :input-source-point-seeks (:events-read result)
                    :surfaces-read (:surfaces-read result)
                    :common-material-range-seeks (:surfaces-read result)
                    :unit-reads (:unit-reads result)
                    :unit-point-seeks (* 2 (:unit-reads result))
                    :seek-count seek-count
                    :seek-bound (+ 1 (* 4 limit))
                    :limit limit
                    :river-events-total total-river-events
                    :river-events-rendered (:events-read result)
                    :surfaces-rendered (:surfaces-read result)
                    :native-rows-total (count native-rows)
                    :native-surfaces-read (:surfaces-read native)
                    :native-unit-reads (:unit-reads native)
                    :native-blocks-returned (count (:blocks native))
                    :blocks-returned blocks-returned
                    :truncated? truncated?
                    :page-complete? (not truncated?)}]
    ;; Native blocks APPEND after the river page (each lane bounded by the
    ;; same limit); global reading order is the projection's time merge
    ;; (conversation-projection), never this vector's order.
    ;; first-light P2b: geometry cells (entry-kind :episode-geometry, geo:
    ;; order-keys — settled position/camera truth) and turn records
    ;; (:episode-turn, ep-turn: keys) ride the SAME projection read; expose
    ;; them as additive meta so the serve layer threads them without a
    ;; second range seek. Both are invisible to the block lanes above
    ;; (positive entry-kind filters).
    (with-meta (into (vec (take limit (:blocks result))) (:blocks native))
      {:river-page/read-plan read-plan
       :river-page/geo-rows  (vec (filter #(contains? #{:episode-geometry
                                                        :episode-camera}
                                                      (:entry-kind %))
                                          projection))
       :river-page/turn-rows (vec (sort-by :order-key
                                           (filter #(= :episode-turn (:entry-kind %))
                                                   projection)))})))

(defn start-distiller-runtime!
  "Launch the object-container runtime (OC + transcript-ops on one IPC; every
   foreign handle). Pass {:relations? true} to ALSO launch the relation-kernel
   runtime (P3b mechanical edges) on its OWN IPC — the driver is a foreign client
   to both, and a foreign client spans IPCs (N4: two IPCs cleanly separate the OC
   stream-await from the RK microbatch barrier). Returns {:oc-rt <ocr> :rk-rt? <rk>}."
  ([] (start-distiller-runtime! {}))
  ([{:keys [relations?]}]
   (cond-> {:oc-rt (ocr/start-object-container-runtime!)}
     relations? (assoc :rk-rt (rk/start-relation-runtime!)))))

(defn close-distiller-runtime!
  "Close whichever runtimes start-distiller-runtime! opened (OC always; RK when
   :relations? was set)."
  [{:keys [oc-rt rk-rt]}]
  (when oc-rt (ocr/close-object-container-runtime! oc-rt))
  (when rk-rt (rk/close-relation-runtime! rk-rt)))
