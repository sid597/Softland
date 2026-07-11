(ns app.server.rama.object-container.assembly-adapter
  "Faces-as-assemblies Wave 2 · the `.edn` assembly source adapter (framework
   CONTRACT §8/§16/§17; lane W2-D). `markdown_adapter.clj:443-517` is the
   template: materialization → `imp:asm:` import key → action-request envelope
   through the EXISTING object-container import seam. NO new row types, NO new
   module — assemblies ride the OC ontology (trap T11).

   Identity (§17): the assembly object-key is `asm:<assembly-name>` —
   deterministic on the NAME (Sid's handle, the thing `/face` wears), never the
   file path. Renaming a face in its file mints a NEW object (a fork — linked
   by envelope `based-on`/`new-direction` edges, never magic).

   Validation at ingest (§17, trap T18): the adapter validates the parsed form
   with the SAME `.cljc` compiler + registry the client wears
   (`face-assembly/compile-assembly` against `face-primitives/registry`) — one
   compiler, two call sites, verdicts equal by construction. A malformed or
   invalid `.edn` STILL ingests as source (block-kernel R1 discipline: the raw
   surface always lands); its derived material carries
   `{:assembly/valid? false :assembly/errors [...]}` honestly.

   Routing note (the G18 class, worked in full at lane W2-D): `asm:<name>`
   contains a colon, so ids behind `leading-object-key`-truncating prefixes
   (`src:` `rev:` `evt:` `du:` `sa:` `ce:`) MISROUTE on foreign point reads
   (extract-object-key would return \"asm\"). The adapter therefore keeps the
   template's id shapes (they are only ever read task-LOCALLY by the import
   topology, or by test-only `{:pkey}` physical readers), and the PRODUCT read
   surface for assemblies uses only correctly-routing paths: the decision/audit
   seam (raw `:partition/key`), `read-import-completion` (via the new
   `imp:asm:` extract-object-key branch, G18), the document container
   `oc:doc:asm:<name>` (extract-object-key's full-remainder `oc:doc:` branch)
   + `read-current-revision` (its revision hop is task-local), and
   `$$source-latest-by-ref` (re-hashed to the colonless source-ref-key).
   `read-source`/`read-unit`/`read-common-material-for-source` misroute for
   `asm:` keys — a NAMED lack for W2-INT, never product-ridden here."
  (:require [app.client.workspace.face-assembly :as face-assembly]
            [app.client.workspace.face-primitives :as face-primitives]
            [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.relation-kernel :as rk]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def assembly-distiller-id "assembly-compile-v0")
(def assembly-distiller-version 1)

(def system-asserter-actor-id
  "Version-free system actor for envelope edges when `:assembly/author` is
   absent (§17: asserted-by uses the author when present, else the system
   watcher actor; the git-spine `import:git-spine` precedent)."
  "import:face-arsenal")

;; ===========================================================================
;; Identity (§17) — object-key deterministic on the NAME.
;; ===========================================================================

(defn valid-assembly-name?
  "A usable face handle: non-blank string, no colon (the object-key's segment
   separator — a colon in the name would break `imp:asm:` two-segment routing,
   G18), no whitespace."
  [x]
  (and (string? x)
       (not (str/blank? x))
       (nil? (str/index-of x ":"))
       ;; (?U): unicode whitespace too (G26 LOW — an NBSP in a name causes
       ;; silent /face lookup misses; routing was never at risk, only UX)
       (nil? (re-find #"(?U)\s" x))))

(defn source-ref-file-stem
  "The file stem of a source-ref path (basename minus a trailing .edn) — the
   deterministic FALLBACK name when the envelope carries no usable
   `:assembly/name` (a malformed file still needs a stable identity so the R1
   source lands on one object across retries)."
  [source-ref]
  (let [base (last (str/split (str source-ref) #"/"))
        stem (if (str/ends-with? base ".edn")
               (subs base 0 (- (count base) 4))
               base)
        cleaned (str/replace stem #"[:\s]" "-")]
    (if (str/blank? cleaned) "unnamed-assembly" cleaned)))

(defn assembly-name-for
  "The face name (identity): the envelope's `:assembly/name` when usable, else
   the file stem. Returns {:name <string> :name-source :envelope|:file-stem}."
  [source-ref parsed]
  (let [envelope-name (when (map? parsed) (:assembly/name parsed))]
    (if (valid-assembly-name? envelope-name)
      {:name envelope-name :name-source :envelope}
      {:name (source-ref-file-stem source-ref) :name-source :file-stem})))

(defn assembly-object-key
  "`asm:<assembly-name>` (§17) — deterministic on the worn NAME."
  [assembly-name]
  (str "asm:" assembly-name))

(defn assembly-import-key
  "`imp:asm:<object-key>:<sha-256(source-ref-key:source-hash)>` — the
   `markdown-import-key` shape verbatim (§17; `markdown_adapter.clj:443-445`).
   With object-key = `asm:<name>` the literal string is
   `imp:asm:asm:<name>:<sha>`; the extract-object-key branch (G18) strips
   `imp:asm:` and takes the first TWO segments back — `asm:<name>`."
  [object-key source-ref-key source-hash]
  (str "imp:asm:" object-key ":" (core/sha-256 (str source-ref-key ":" source-hash))))

(defn derived-assembly-unit-id
  "The ONE derived-material row id per assembly object. `du:`-prefixed ids
   misroute on FOREIGN reads for asm objects (routing note above) — this row
   is written and validated task-locally by the import topology and read by
   test receipts via `{:pkey object-key}`; product serves recompute the verdict
   from the source (T18: same compiler)."
  [object-key]
  (str "du:" object-key ":" assembly-distiller-id ":assembly"))

;; ===========================================================================
;; Parse + validate (T18 — ONE compiler, both call sites).
;; ===========================================================================

(defn parse-assembly-source
  "Safe EDN read (clojure.edn — no eval, no reader tags honored beyond
   defaults). Never throws: a malformed file returns {:parse-error ...} and
   still ingests as source (R1)."
  [raw-text]
  (try
    {:parsed (edn/read-string (str raw-text))}
    (catch Throwable t
      {:parsed nil
       :parse-error {:type :assembly/parse-error
                     :message (str (.getMessage t))}})))

(defn validate-assembly-source
  "The ingest-side validation verdict via the SAME `.cljc` compiler + registry
   the client wears (trap T18): {:valid? bool :errors [...] :name <string|nil>}.
   Total — never throws."
  [raw-text]
  (let [{:keys [parsed parse-error]} (parse-assembly-source raw-text)]
    (cond
      parse-error
      {:valid? false :errors [parse-error] :name nil :parsed nil}

      (not (map? parsed))
      {:valid? false
       :errors [{:type :assembly/not-a-map :value-type (str (type parsed))}]
       :name nil :parsed parsed}

      :else
      (let [compiled (face-assembly/compile-assembly face-primitives/registry parsed)]
        (if (face-assembly/error? compiled)
          {:valid? false
           :errors (vec (face-assembly/compile-errors compiled))
           :name (:assembly/name parsed)
           :parsed parsed}
          {:valid? true :errors [] :name (:assembly/name parsed) :parsed parsed})))))

;; ===========================================================================
;; Materialization — the OC row set (markdown template, assembly-sized:
;; source + document container + revision + ONE derived verdict row + anchors
;; + one composition edge).
;; ===========================================================================

(defn envelope-provenance
  "The CLOSED §17 provenance field set from a parsed envelope. Nil-safe: a
   malformed file (no parse) yields all-nil. NO passthrough of arbitrary
   fields — this closed map is what makes an unregistered relation kind
   impossible by construction (G19)."
  [parsed]
  (when (map? parsed)
    {:birthed-by (:assembly/birthed-by parsed)
     :based-on   (:assembly/based-on parsed)
     :supersedes (:assembly/supersedes parsed)
     :author     (:assembly/author parsed)
     :status     (:assembly/status parsed)}))

(defn assembly-status
  "Envelope `:assembly/status` ∈ #{:candidate :worn :retired} (§17), default
   :candidate. An unknown value degrades to :candidate honestly (the envelope
   still carries it verbatim in the source; the INDEX never invents a status)."
  [parsed]
  (let [s (when (map? parsed) (:assembly/status parsed))]
    (if (contains? #{:candidate :worn :retired} s) s :candidate)))

(defn assembly-materialization
  "Pure: raw-text + source-ref + opts → the full OC import row set + identity
   + the validation verdict. All ids deterministic from (name, content,
   request-id) — retries and re-imports converge (D-008.3)."
  [raw-text source-ref opts]
  (let [raw-text (str raw-text)
        source-hash (or (:source/hash opts) (oc/source-hash raw-text))
        source-ref-key (oc/source-ref-key source-ref)
        verdict (validate-assembly-source raw-text)
        {:keys [name name-source]} (assembly-name-for source-ref (:parsed verdict))
        object-key (assembly-object-key name)
        import-key (or (:import/key opts)
                       (assembly-import-key object-key source-ref-key source-hash))
        source-id (oc/source-id-for-object-key object-key)
        document-id (oc/document-id-for-object-key object-key)
        request-id (or (:request/id opts) (core/random-id "req"))
        created-at (or (:time-ms opts) (core/now-ms))
        created-by (or (get-in opts [:actor :actor/id]) "system")
        event-id (str "evt:" object-key ":" request-id)
        order-key (oc/fixed-width-order-key created-at request-id)
        source-row (oc/->SourceArtifactRow source-id
                                           source-ref
                                           source-hash
                                           :edn
                                           raw-text
                                           document-id
                                           (long (count (.getBytes raw-text "UTF-8")))
                                           created-at
                                           created-by
                                           event-id)
        version-row (oc/->SourceVersionRow source-ref-key
                                           source-ref
                                           source-hash
                                           source-id
                                           document-id
                                           object-key
                                           order-key
                                           created-at
                                           event-id)
        ;; revision-id deterministic on CONTENT (the markdown template's
        ;; import-revision-id discipline): identical bytes → same revision-id;
        ;; an edited save → a new revision on the SAME object-key (G17).
        revision-id (oc/import-revision-id object-key document-id source-hash)
        revision-row (oc/->RevisionRow revision-id
                                       document-id
                                       nil
                                       raw-text
                                       source-hash
                                       order-key
                                       created-at
                                       created-by
                                       event-id)
        document-anchor-id (oc/source-anchor-id document-id)
        ;; The face container is the IDENTITY ANCHOR, not an inline-content
        ;; copy: current-content-text/hash stay nil, and the material rides
        ;; the REVISION chain (each save = one revision on this stable
        ;; container, §17 — read via read-current-revision). This is what
        ;; makes a revisable-by-import object lawful under the kernel's
        ;; native-identity claim (native-claim-compatible? branch 2 compares
        ;; the container's content-hash across imports; a stable identity
        ;; with nil inline content claims compatibly forever, while an
        ;; inline-content copy would REJECT every edit as a native-identity
        ;; conflict — found by this lane's G17 edited-save receipt). The
        ;; markdown template inlines content because its object-keys are
        ;; content-addressed (a new object per save); assembly identity is
        ;; the NAME, deliberately.
        document-row (oc/->ObjectContainerRow document-id
                                              :assembly
                                              object-key
                                              :private
                                              source-id
                                              document-anchor-id
                                              nil
                                              document-id
                                              revision-id
                                              nil
                                              nil
                                              created-at
                                              created-by
                                              event-id)
        document-anchor (oc/->SourceAnchorRow document-anchor-id
                                              :object-container
                                              document-id
                                              source-id
                                              source-ref
                                              source-hash
                                              0
                                              (count raw-text)
                                              nil
                                              event-id)
        ;; ONE derived-material row: the validation verdict (§8 — the validated
        ;; assembly form is the derived material; §17/R1 — `:assembly/valid?`
        ;; honest per file). Verdict travels pr-str in the text field (the
        ;; block-kernel R4 pr-str precedent, ruled 2026-07-09); the KIND is the
        ;; mechanically-assertable verdict.
        unit-id (derived-assembly-unit-id object-key)
        unit-text (pr-str {:assembly/name name
                           :assembly/name-source name-source
                           :assembly/valid? (:valid? verdict)
                           :assembly/errors (:errors verdict)})
        unit-anchor-id (oc/source-anchor-id unit-id)
        unit-row (oc/->DerivedUnitRow unit-id
                                      document-id
                                      source-id
                                      (if (:valid? verdict) :assembly/valid :assembly/invalid)
                                      "assembly"
                                      nil
                                      unit-anchor-id
                                      unit-text
                                      (oc/source-hash unit-text)
                                      assembly-distiller-id
                                      assembly-distiller-version
                                      event-id)
        unit-anchor (oc/->SourceAnchorRow unit-anchor-id
                                          :derived-unit
                                          unit-id
                                          source-id
                                          source-ref
                                          source-hash
                                          0
                                          (count raw-text)
                                          "assembly"
                                          event-id)
        edge-row (oc/->CompositionEdgeRow (oc/composition-edge-id object-key document-id "assembly")
                                          object-key
                                          document-id
                                          document-id
                                          unit-id
                                          "assembly"
                                          :object-container
                                          document-id
                                          :derived-unit
                                          unit-id
                                          source-id
                                          nil
                                          event-id)]
    {:object-key object-key
     :source-ref-key source-ref-key
     :source-hash source-hash
     :source-id source-id
     :document-id document-id
     :import-key import-key
     :request-id request-id
     :created-at created-at
     :assembly-name name
     :name-source name-source
     :verdict verdict
     :status (assembly-status (:parsed verdict))
     :provenance (envelope-provenance (:parsed verdict))
     :source-row source-row
     :version-row version-row
     :revision-row revision-row
     :document-row document-row
     :document-anchor-row document-anchor
     :unit-row unit-row
     :unit-anchor-row unit-anchor
     :edge-row edge-row}))

(defn assembly-import-payload
  "The import-material payload (markdown-import-payload shape). The verdict +
   provenance ride as EXTRA namespaced keys (harmless to the kernel's
   fingerprint/validation, both of which read a closed key set) so the
   watcher's post-accept steps (arsenal register, lineage edges) derive from
   the REQUEST alone."
  [m]
  {:object-key (:object-key m)
   :source-ref (:source-ref (:source-row m))
   :source-hash (:source-hash m)
   :source-raw-text (:source-raw-text (:source-row m))
   :source-format :edn
   :source-artifacts [(:source-row m)]
   :object-containers [(:document-row m)]
   :revisions [(:revision-row m)]
   :derived-units [(:unit-row m)]
   :source-anchors [(:document-anchor-row m) (:unit-anchor-row m)]
   :composition-edges [(:edge-row m)]
   :source-versions [(:version-row m)]
   :assembly/name (:assembly-name m)
   :assembly/name-source (:name-source m)
   :assembly/valid? (:valid? (:verdict m))
   :assembly/errors (:errors (:verdict m))
   :assembly/status (:status m)
   :assembly/provenance (:provenance m)})

(defn assembly-source-import-request
  "Build the ONE import request for an assembly `.edn` source, through the
   EXISTING seam shape (`markdown-source-import-request` verbatim template):
   `append-object-container-request!` → `await-object-container-decision`."
  ([raw-text source-ref] (assembly-source-import-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [m (assembly-materialization raw-text source-ref opts)
         object-key (:object-key m)
         import-key (:import-key m)
         payload (assembly-import-payload m)
         material-fingerprint (oc/import-material-fingerprint object-key import-key payload)
         actor (or (:actor opts)
                   {:actor/id "system"
                    :actor/type :system
                    :actor/capabilities #{:object-container/import-material
                                          :source/ingest
                                          :object/edit}})]
     (assoc (core/action-request
             {:request-id (:request-id m)
              :request-type :object-container/import-material
              :time-ms (:created-at m)
              :actor actor
              :branch (:branch opts)
              :context (:context opts)
              :target {:target/kind :object-container-import
                       :target/id import-key
                       :target/address {:source/ref source-ref
                                        :source/hash (:source-hash m)
                                        :object/key object-key}}
              :action {:action/type :object-container/import-material
                       :action/capability :object-container/import-material
                       :action/params {:source/family :assembly
                                       :source/format :edn}}
              :routing/key [:object-container/import object-key]
              :payload payload
              :causal (:causal opts)
              :provenance (or (:provenance opts)
                              {:source/type :assembly
                               :source/ref source-ref})})
            :partition/key object-key
            :object/key object-key
            :import/key import-key
            :source/family :assembly
            :source/format :edn
            :idempotency/key import-key
            :material/fingerprint material-fingerprint))))

;; ===========================================================================
;; Lineage edges (§17, gate G19) — envelope provenance → EXISTING D-004 kinds.
;; ===========================================================================

(defn- face-target-ref
  "Normalize an envelope provenance value to a relation target: a bare face
   NAME (no colon) → that face's document container `oc:doc:asm:<name>`;
   a face OBJECT-KEY (`asm:<name>` — §17 sanctions both forms) → the SAME
   doc-container (G26 fix: the two forms must join, not mint two targets);
   any other colon-carrying value (a foreign object-key or container address,
   e.g. `chat:<hex>`) passes through verbatim as a :container ref.
   `->target-ref` derives the target-key via extract-object-key (routes
   `oc:doc:asm:<name>` → `asm:<name>` correctly — the full-remainder branch)."
  [value]
  (let [v (str value)]
    (cond
      (nil? (str/index-of v ":"))
      (rk/->target-ref :container (oc/document-id-for-object-key (assembly-object-key v)))

      (str/starts-with? v "asm:")
      (rk/->target-ref :container (oc/document-id-for-object-key v))

      :else
      (rk/->target-ref :container v))))

(def ^:private provenance-field->kind
  "The CLOSED field → kind map (§17; EXISTING D-004 kinds only, verified
   registered in `relation-kinds`, relation_kernel.clj:58-73). NO passthrough:
   an envelope cannot name a kind — only these fields exist, so an
   unregistered kind is impossible by construction (G19). `pairs-with` (or any
   addition) is a stop-clause escalation, never an enum edit."
  {:birthed-by :produced
   :based-on   :based-on
   :supersedes :supersedes})

(defn edge-specs
  "Pure: materialization-identity + provenance → the edge assert specs.
   Directions (§8/§17): `produced` = birthing conversation → face;
   `based-on`/`supersedes` = face → target. Idempotency key derived from
   (face object-key, kind, target) — STABLE across imports (the ACTUAL
   git-spine `git_spine.clj:224` discipline). G26 fix, 2026-07-11: CONTRACT
   §17 originally named import-key as a key component — that defeats the rk
   journal on every EDITED save (new import-key → new idempotency key →
   phantom re-assert transitions), leaving only the racy async read-back as
   defense. The contract was amended in place; the note field keeps the
   import-key for provenance."
  [{:keys [object-key document-id import-key created-at provenance]}]
  (let [face-ref (rk/->target-ref :container document-id)
        author (:author provenance)
        asserter-id (if (string? author) author system-asserter-actor-id)
        asserter-type (if (string? author) :human :import)]
    (into []
          (keep (fn [[field kind]]
                  (when-let [value (get provenance field)]
                    (let [target (face-target-ref value)
                          [from to] (if (= :produced kind)
                                      [target face-ref]
                                      [face-ref target])
                          k (core/sha-256 (str object-key "|" (name kind) "|"
                                               (str value)))]
                      {:kind kind
                       :from from
                       :to to
                       :asserter-actor-id asserter-id
                       :asserter-type asserter-type
                       :asserted-at-ms created-at
                       :request-id k
                       :idempotency-key k
                       :note (str "assembly-envelope|" (name field) "|" import-key)}))))
          provenance-field->kind)))

(defn request-edge-identity
  "Recover the edge-spec inputs from an accepted import REQUEST (the watcher
   holds only the request + decision at post-accept time)."
  [request]
  (let [payload (oc/request-payload request)]
    {:object-key (:object-key payload)
     :document-id (oc/document-id-for-object-key (:object-key payload))
     :import-key (oc/request-import-key request)
     :created-at (:request/time-ms request)
     :provenance (:assembly/provenance payload)}))

(defn- edge-already-asserted?
  "Cost guard on top of the rk journal (the git-spine `edge-already-asserted?`
   precedent): skip the append when the relation is already asserted."
  [runtime relation-id]
  (boolean (some-> (rk/read-relation-detail runtime relation-id)
                   :row :relation-status (= :asserted))))

(defn assert-envelope-edges!
  "Assert the envelope's lineage edges for one ACCEPTED assembly import
   (called by the watcher post-accept; T17-adjacent: idempotent by stable key,
   so the next accepted decision — replay included — converges any gap).
   Returns {:asserted n :skipped n :specs [...]}. When the runtime carries no
   relation-kernel handle the edges are SKIPPED and reported (honest
   degradation, never a throw)."
  [runtime request]
  (let [specs (edge-specs (request-edge-identity request))]
    (if (or (empty? specs) (nil? (:relation-request-depot runtime)))
      {:asserted 0 :skipped (count specs)
       :skip-reason (when (and (seq specs) (nil? (:relation-request-depot runtime)))
                      :no-relation-runtime)
       :specs specs}
      (reduce
       (fn [acc {:keys [kind from to asserter-actor-id] :as spec}]
         (let [relation-id (rk/relation-id-for kind from to asserter-actor-id)]
           (if (edge-already-asserted? runtime relation-id)
             (update acc :skipped inc)
             (do (rk/append-relation-request! runtime (rk/assert-request spec))
                 (update acc :asserted inc)))))
       {:asserted 0 :skipped 0 :specs specs}
       specs))))
