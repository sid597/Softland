(ns app.server.ingest.clojure-adapter
  "Clojure source cut into top-level form units.
   Takes: source text, blob and path metadata, actor data, and claimed time.
   Gives: form units, source anchors, material rows, and import requests.
   Holds nothing."
  (:require [app.server.rama.envelope :as envelope]
            [app.server.rama.object-container :as oc]
            [clojure.string :as str]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser :as parser]))

(def clojure-distiller-id "clojure-form-v0")
(def clojure-distiller-version 1)

;; ---------------------------------------------------------------------------
;; Deny-list (R6, T10): deny by PATH before any text leaves git. The env.clj
;; path STRING is hardcoded here; its CONTENT is never opened by anything.
;; ---------------------------------------------------------------------------

(def code-deny-list-rule-id "code-deny-v1")

(def code-deny-list
  "Denied path suffixes (repo-relative). Fail-closed: a denied blob mints NO
   surface, units, or edges (SPEC §2.4). Suffix-matched so the same rule covers
   every absolute working-copy root (/mnt/... and /home/... both end in this)."
  #{"src/app/server/env.clj"})

(defn- normalize-path [path]
  (-> (str path) (str/replace "\\" "/")))

(defn denied-path?
  "True iff `path` is (or ends at a path-segment boundary with) any deny entry.
   Segment-boundary match (leading `/`) so `notsrc/app/server/env.clj` is NOT a
   false positive while `<root>/src/app/server/env.clj` is denied."
  [path]
  (let [p (normalize-path path)]
    (boolean
     (some (fn [d] (or (= p d) (str/ends-with? p (str "/" d))))
           code-deny-list))))

(defn path-denial
  "The denial marker a denied path yields instead of a request (carries the
   versioned rule-id for run-stats accounting, R6)."
  [path]
  {:code-deny/denied? true
   :code-deny/rule-id code-deny-list-rule-id
   :code-deny/path (str path)})

(defn guarded-clojure-import-request
  "Driver entry (R6/T10): refuse denied paths BEFORE reading text. `build-request`
   is a zero-arg thunk that produces the import request (the step that reads git
   blob text) — for a denied path it is NEVER called, so no denied bytes are read."
  [path build-request]
  (if (denied-path? path)
    (path-denial path)
    (build-request)))

;; ---------------------------------------------------------------------------
;; The free cut (code lane + commentary lane) — clojure-form-v0@1
;; ---------------------------------------------------------------------------

(def ^:private head-name->kind
  "Closed normalization table over the (unqualified) head symbol name
   (SPEC §3.4). Grows only by spec amendment; the raw head is always retained."
  {"ns"           :clj/ns
   "def"          :clj/def
   "defonce"      :clj/def
   "defn"         :clj/fn
   "defn-"        :clj/fn
   "defrecord"    :clj/record
   "deftype"      :clj/record
   "defprotocol"  :clj/protocol
   "defmacro"     :clj/macro
   "defmulti"     :clj/multi
   "defmethod"    :clj/multi
   "deftest"      :clj/test
   "defmodule"    :clj/module
   "comment"      :clj/rich-comment})

(def ^:private name-carrying-kinds
  "Kinds whose block-path is the binding name (R7). `:clj/ns` is handled
   separately (fixed \"ns\"); everything else is positional."
  #{:clj/def :clj/fn :clj/record :clj/protocol :clj/macro :clj/multi
    :clj/test :clj/module :clj/electric-fn})

(defn- meaningful-children [nd]
  (remove #(or (node/whitespace? %) (node/comment? %)) (node/children nd)))

(defn- resolve-through-meta
  "Unwrap `^meta ...` nodes to the wrapped value (SPEC §3.5 note: a
   metadata-wrapped form / metadata-wrapped binding name is one unit whose head
   and name resolve THROUGH the meta node). `^:private id-part-separator`
   resolves to the symbol `id-part-separator`."
  [nd]
  (if (and nd (= :meta (node/tag nd)))
    (recur (last (meaningful-children nd)))
    nd))

(defn- token-symbol
  "The symbol a token node denotes, or nil. Guarded: exotic tokens (ratios,
   `##Inf`, tagged) never abort the cut."
  [nd]
  (when (and nd (= :token (node/tag nd)))
    (let [sx (try (node/sexpr nd) (catch Exception _ nil))]
      (when (symbol? sx) sx))))

(defn- head-symbol [list-node]
  (token-symbol (first (meaningful-children list-node))))

(defn- reader-conditional?
  "Top-level `#?`/`#?@` (SPEC §3.4: atomic — one unit, no descent, T7)."
  [nd]
  (and (= :reader-macro (node/tag nd))
       (contains? #{"?" "?@"} (some-> (first (node/children nd)) node/string))))

(defn- head-string [nd reader-cond?]
  (cond
    (= :list (node/tag nd)) (some-> (head-symbol nd) str)
    (= :reader-macro (node/tag nd)) (str "#" (some-> (first (node/children nd)) node/string))
    :else nil))

(defn- classify-kind [head-sym reader-cond?]
  (cond
    reader-cond?      :clj/reader-cond
    (nil? head-sym)   :clj/other
    :else
    (let [nsp (namespace head-sym)
          nm  (name head-sym)]
      (cond
        ;; e/defn is Electric-qualified; check BEFORE the unqualified "defn"
        ;; fallthrough so it lands :clj/electric-fn, not :clj/fn (SPEC §3.4).
        (and (= nm "defn") (= nsp "e")) :clj/electric-fn
        :else (get head-name->kind nm :clj/other)))))

(defn- dedup-path
  "Duplicate names deterministically suffixed ~2, ~3 … in file order (R7).
   First occurrence keeps the bare name. Separator is `~` (F2 fix,
   DIFF_FALSIFICATION): `#` CAN appear in a legal Clojure symbol — `foo#2` reads
   as one symbol and `#'user/foo#2` compiles — so a `#`-suffix collides with a
   literal var literally named `foo#2`, giving two units the SAME block-path →
   SAME unit-id → last-write-wins overwrite (reproduced). `~` is the reader's
   unquote macro char: it can NEVER be part of a symbol token (verified —
   rewrite-clj splits `foo~2` into `foo` `~2`), so `foo~2` cannot collide with
   any var name. block-paths are opaque id strings downstream, never re-read as
   symbols, so `~` is inert everywhere it flows (unit-id / target-key / journal key)."
  [base seen]
  (let [n (get seen base 0)
        path (if (zero? n) base (str base "~" (inc n)))]
    [path (assoc seen base (inc n))]))

(defn- block-path-for [kind base-name order seen]
  (cond
    (= kind :clj/ns)
    (dedup-path "ns" seen)

    (and base-name (name-carrying-kinds kind))
    (dedup-path base-name seen)

    ;; unnamed forms: positional %06d over the code-lane ordinal (R7). Never
    ;; collides with a name (names are not 6-digit numbers) or with another
    ;; unnamed form (ordinals are unique), so no dedup is needed.
    :else
    [(format "%06d" order) seen]))

(defn- build-unit
  "Build one code-lane unit map from a top-level node and its [start,end)
   UTF-16 span. Returns [unit seen']."
  [source nd start end order seen]
  (let [reader-cond? (reader-conditional? nd)
        inner        (resolve-through-meta nd)
        head-sym     (when (= :list (node/tag inner)) (head-symbol inner))
        kind         (classify-kind head-sym reader-cond?)
        raw-name     (when (or (= kind :clj/ns) (name-carrying-kinds kind))
                       (let [bn (resolve-through-meta (second (meaningful-children inner)))]
                         (some-> (token-symbol bn) str)))
        ;; defmethod appends its dispatch value to the name (SPEC §3.5).
        base-name    (if (and (= kind :clj/multi)
                              (= "defmethod" (some-> head-sym name))
                              raw-name)
                       (if-let [dn (nth (meaningful-children inner) 2 nil)]
                         (str raw-name ":" (str/trim (node/string dn)))
                         raw-name)
                       raw-name)
        [block-path seen'] (block-path-for kind base-name order seen)]
    [{:block-path   block-path
      :unit-kind    kind
      :head         (head-string inner reader-cond?)   ; raw head, always retained
      :name         raw-name
      :text         (subs source start end)            ; == blob substring (G2)
      :start-offset start
      :end-offset   end}
     seen']))

(defn- comment-span [source start end]
  {:start-offset start :end-offset end :text (subs source start end)})

(declare ^:private parsed-form-cut)

(defn clojure-form-v0
  "Pure free cut `clojure-form-v0@1` (SPEC §3): text ->
   {:units [...] :comment-spans [...]}. TOP-LEVEL forms only — no eager
   sub-form cutting (T7; refinement is on-demand, later).

   rewrite-clj `parse-string-all` children TILE the source losslessly, so
   offsets accumulate `(.length (node/string n))` in UTF-16 code units
   (SPEC §2.3, P0 §a). Each unit's :text == (subs source start end) by
   construction, so U+0000 escape literals (rk:74/76) survive verbatim — the
   cut never re-encodes text (T8).

   Two lanes (R2): code-lane forms mint units; standalone `;;` comment runs are
   classified into :comment-spans but mint NO units (their spans stay unblocked
   surface, refinable later). Consecutive comment lines form ONE run (a banner);
   a blank line — a newline in the gap between two comment nodes — breaks it.
   Whitespace/newline nodes stay in gaps (neither unit nor span).

   UNPARSEABLE text (GATE_REVIEW 2026-07-09 finding G-F1): git history contains
   blobs that were committed BROKEN (unbalanced parens, reader-hostile tokens —
   5 of this repo's 895 historical code blobs). A parse failure must never abort
   a whole sync: the cut degrades to ZERO units + ZERO spans and carries
   `:parse-error` so the caller can COUNT it (no silent caps). The raw surface
   still stores verbatim (R1 holds — atoms are honestly absent, the map does not
   lie); reconstruction is trivially exact (the whole text is gap)."
  [raw-text]
  (let [source (str raw-text)
        parsed (try (parser/parse-string-all source)
                    (catch Exception e e))]
    (if (instance? Exception parsed)
      {:units [] :comment-spans []
       :parse-error {:message (.getMessage ^Exception parsed)}}
      (parsed-form-cut source (node/children parsed)))))

(defn- parsed-form-cut
  [source children]
    (loop [nodes  children
           offset 0
           order  0                 ; code-lane ordinal (positional block-paths)
           seen   {}                ; base-name -> count, for #n dedup (R7)
           units  []
           spans  []
           run    nil]              ; open comment run {:start :end} or nil
      (if-let [nd (first nodes)]
        (let [s     (node/string nd)
              start offset
              end   (+ offset (.length ^String s))
              more  (rest nodes)]
          (cond
            (node/comment? nd)
            ;; Continue the run iff the gap since the last comment holds no
            ;; newline (comment nodes already include their own trailing \n, so
            ;; any newline in the gap is a blank line -> break).
            (let [continues? (and run
                                  (not (str/includes? (subs source (:end run) start) "\n")))]
              (if continues?
                (recur more end order seen units spans (assoc run :end end))
                (recur more end order seen units
                       (if run (conj spans (comment-span source (:start run) (:end run))) spans)
                       {:start start :end end})))

            ;; Whitespace/newline: a gap. Keep any open run open across it — the
            ;; gap-newline test above decides run continuity when the next
            ;; comment arrives; a form (below) flushes the run.
            (node/whitespace? nd)
            (recur more end order seen units spans run)

            :else
            (let [spans'         (if run
                                   (conj spans (comment-span source (:start run) (:end run)))
                                   spans)
                  [unit seen'] (build-unit source nd start end order seen)]
              (recur more end (inc order) seen' (conj units unit) spans' nil))))
        {:units units
         :comment-spans (if run
                          (conj spans (comment-span source (:start run) (:end run)))
                          spans)})))

;; ---------------------------------------------------------------------------
;; Source-materialization — twin of markdown_adapter/source-materialization.
;; Same row assembly; parent of every unit is the document container (flat
;; top-level, md idiom, CONTRACT §4). Distiller id/version and source format
;; are the clojure ones; unit-kind/block-path come from the cut.
;; ---------------------------------------------------------------------------

(defn derived-unit-id
  [object-key block-path]
  (str "du:" object-key ":" clojure-distiller-id ":" block-path))

(defn- child-order-key
  "File-order key for composition/outline ordering. Unlike markdown (where the
   positional block-path IS the order), code block-paths are binding NAMES (R7),
   so file order rides this separate positional key."
  [order]
  (format "%06d" (long order)))

(defn source-materialization
  [request]
  (let [payload           (oc/request-payload request)
        source-ref        (oc/payload-source-ref payload)
        source-hash-value (oc/payload-source-hash payload)
        raw-text          (str (oc/payload-source-raw-text payload))
        source-ref-key    (oc/source-ref-key source-ref)
        object-key        (oc/object-key-for source-ref source-hash-value)
        source-id         (oc/source-id-for-object-key object-key)
        document-id       (oc/document-id-for-object-key object-key)
        event-id          (oc/event-id-for-request object-key request)
        created-at        (:request/time-ms request)
        created-by        (get-in request [:actor :actor/id])
        cut               (clojure-form-v0 raw-text)
        units             (:units cut)
        source-event-payload (oc/->SourceIngestedPayload source-id
                                                         source-ref
                                                         source-hash-value
                                                         document-id
                                                         clojure-distiller-id
                                                         clojure-distiller-version
                                                         (count units)
                                                         (count units))
        event             (oc/event-row event-id
                                        :source/ingested
                                        object-key
                                        :source-artifact
                                        source-id
                                        (:actor request)
                                        source-event-payload
                                        created-at
                                        request)
        source-row        (oc/->SourceArtifactRow source-id
                                                  source-ref
                                                  source-hash-value
                                                  (oc/payload-source-format payload)
                                                  raw-text
                                                  document-id
                                                  (long (count (.getBytes raw-text "UTF-8")))
                                                  created-at
                                                  created-by
                                                  event-id)
        version-row       (oc/->SourceVersionRow source-ref-key
                                                 source-ref
                                                 source-hash-value
                                                 source-id
                                                 document-id
                                                 object-key
                                                 (oc/fixed-width-order-key created-at (oc/request-id request))
                                                 created-at
                                                 event-id)
        completion-row    (oc/->SourceIngestCompletionRow source-id
                                                          source-ref-key
                                                          source-ref
                                                          source-hash-value
                                                          document-id
                                                          (count units)
                                                          (count units)
                                                          created-at
                                                          (oc/request-id request)
                                                          event-id
                                                          ;; material-claimed clock (git-spine: committer
                                                          ;; clock); nil-honest when the request declares none.
                                                          (:claimed/at-ms request))
        document-anchor-id (oc/source-anchor-id document-id)
        document-revision-id (oc/import-revision-id object-key document-id source-hash-value)
        document-revision-order-key (oc/fixed-width-order-key created-at (oc/request-id request))
        document-revision (oc/->RevisionRow document-revision-id
                                            document-id
                                            nil
                                            raw-text
                                            source-hash-value
                                            document-revision-order-key
                                            created-at
                                            created-by
                                            event-id)
        document-row      (oc/->ObjectContainerRow document-id
                                                   :document
                                                   object-key
                                                   :private
                                                   source-id
                                                   document-anchor-id
                                                   nil
                                                   document-id
                                                   document-revision-id
                                                   raw-text
                                                   source-hash-value
                                                   created-at
                                                   created-by
                                                   event-id)
        document-anchor   (oc/->SourceAnchorRow document-anchor-id
                                                :object-container
                                                document-id
                                                source-id
                                                source-ref
                                                source-hash-value
                                                0
                                                (count raw-text)
                                                nil
                                                event-id)
        unit-rows (mapv (fn [{:keys [block-path unit-kind text]}]
                          (let [unit-id   (derived-unit-id object-key block-path)
                                anchor-id (oc/source-anchor-id unit-id)]
                            (oc/->DerivedUnitRow unit-id
                                                 document-id
                                                 source-id
                                                 unit-kind
                                                 block-path
                                                 nil                 ; parent = document (flat)
                                                 anchor-id
                                                 text
                                                 ;; form-text hash (SPEC §4.2) — the datum P2's
                                                 ;; lineage law reads to tell re-addressed from
                                                 ;; superseded without faking supersessions (T2).
                                                 (oc/source-hash text)
                                                 clojure-distiller-id
                                                 clojure-distiller-version
                                                 event-id)))
                        units)
        unit-anchor-rows (mapv (fn [{:keys [block-path text start-offset end-offset]}]
                                 (let [unit-id (derived-unit-id object-key block-path)]
                                   (oc/->SourceAnchorRow (oc/source-anchor-id unit-id)
                                                         :derived-unit
                                                         unit-id
                                                         source-id
                                                         source-ref
                                                         source-hash-value
                                                         start-offset
                                                         end-offset
                                                         block-path
                                                         event-id)))
                               units)
        outline-rows (mapv (fn [{:keys [block-path unit-kind text]} order]
                             (let [unit-id   (derived-unit-id object-key block-path)
                                   anchor-id (oc/source-anchor-id unit-id)]
                               (assoc (oc/->OutlineNodeRow document-id
                                                           unit-id
                                                           block-path
                                                           nil
                                                           :derived-unit
                                                           unit-id
                                                           anchor-id
                                                           text
                                                           (oc/source-hash text)
                                                           false
                                                           nil
                                                           event-id)
                                      ;; carry the file-order key + kind so a future
                                      ;; :code-outline consumer can order + label. (The
                                      ;; kernel's projection-hint <<cond drops unknown
                                      ;; kinds via (default>), so this is inert in v0.)
                                      :order-key (child-order-key order)
                                      :unit-kind unit-kind)))
                           units (range))
        edge-rows (mapv (fn [{:keys [block-path]} order]
                          (let [unit-id (derived-unit-id object-key block-path)
                                ock     (child-order-key order)
                                edge-id (oc/composition-edge-id object-key document-id ock)]
                            (oc/->CompositionEdgeRow edge-id
                                                     object-key
                                                     document-id
                                                     document-id          ; parent-slot-id (document)
                                                     unit-id
                                                     ock                  ; child-order-key (file order)
                                                     :object-container    ; parent-target-kind
                                                     document-id
                                                     :derived-unit
                                                     unit-id
                                                     source-id
                                                     nil
                                                     event-id)))
                        units (range))]
    {:object-key object-key
     :source-ref-key source-ref-key
     :source-hash source-hash-value
     :source-id source-id
     :document-id document-id
     :event event
     :source-row source-row
     :version-row version-row
     :completion-row completion-row
     :document-row document-row
     :document-revision-row document-revision
     :document-anchor-row document-anchor
     :unit-rows unit-rows
     :unit-anchor-rows unit-anchor-rows
     :outline-rows outline-rows
     :edge-rows edge-rows
     :comment-spans (:comment-spans cut)}))

(defn materialization-object-key [m] (:object-key m))
(defn materialization-source-ref-key [m] (:source-ref-key m))
(defn materialization-source-hash [m] (:source-hash m))
(defn materialization-source-row [m] (:source-row m))
(defn materialization-source-id [m] (:source-id m))
(defn materialization-document-id [m] (:document-id m))
(defn materialization-unit-rows [m] (:unit-rows m))
(defn materialization-unit-anchor-rows [m] (:unit-anchor-rows m))
(defn materialization-outline-rows [m] (:outline-rows m))
(defn materialization-edge-rows [m] (:edge-rows m))
(defn materialization-comment-spans [m] (:comment-spans m))

;; ---------------------------------------------------------------------------
;; Import-request builder — twin of markdown_adapter/markdown-source-import-request.
;; Takes any source-ref (the P2 driver passes "git-blob:<sha>", R1).
;; ---------------------------------------------------------------------------

(defn- clojure-source-ingest-kernel-request
  ([raw-text source-ref] (clojure-source-ingest-kernel-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [raw-text        (str raw-text)
         source-hash     (or (:source/hash opts)
                             (:source-hash opts)
                             (oc/source-hash raw-text))
         source-ref-key  (oc/source-ref-key source-ref)
         object-key      (oc/object-key-for source-ref source-hash)
         source-id       (oc/source-id-for-object-key object-key)
         request-id      (or (:request/id opts) (:request-id opts) (envelope/random-id "req"))
         idempotency-key (or (:idempotency/key opts)
                             (:idempotency-key opts)
                             (str "source/ingest:" source-ref-key ":" source-hash))
         payload         (oc/->SourceIngestPayload source-ref
                                                   source-hash
                                                   raw-text
                                                   :clojure
                                                   clojure-distiller-id
                                                   clojure-distiller-version)]
     (assoc (envelope/action-request
             {:request-id request-id
              :request-type :source/ingest
              ;; T4: caller-supplied clock only; never a wall-clock stamp here.
              :time-ms (:time-ms opts)
              :actor (or (:actor opts)
                         {:actor/id "system"
                          :actor/type :system
                          :actor/capabilities #{:source/ingest :object/edit}})
              :branch (:branch opts)
              :context (:context opts)
              :target {:target/kind :source-artifact
                       :target/id source-id
                       :target/address {:source/ref source-ref
                                        :source/hash source-hash}}
              :action {:action/type :source/ingest
                       :action/capability :source/ingest
                       :action/params {:source/format :clojure}}
              :routing/key [:source-ref source-ref-key]
              :payload payload
              :causal (:causal opts)
              :provenance (or (:provenance opts)
                              {:source/type :manual
                               :source/ref source-ref})})
            :partition/key source-ref-key
            :idempotency/key idempotency-key
            :object/key object-key
            :claimed/at-ms (:claimed/at-ms opts)))))

(defn clojure-import-key
  [object-key source-ref-key source-hash]
  (str "imp:clj:" object-key ":" (envelope/sha-256 (str source-ref-key ":" source-hash))))

(defn clojure-import-payload
  [materialization]
  {:object-key      (:object-key materialization)
   :source-ref      (:source-ref (:source-row materialization))
   :source-hash     (:source-hash materialization)
   :source-raw-text (:source-raw-text (:source-row materialization))
   :source-format   (:source-format (:source-row materialization))
   :source-artifacts [(:source-row materialization)]
   :object-containers [(:document-row materialization)]
   :revisions       [(:document-revision-row materialization)]
   :derived-units   (:unit-rows materialization)
   :source-anchors  (into [(:document-anchor-row materialization)]
                          (:unit-anchor-rows materialization))
   :composition-edges (:edge-rows materialization)
   :source-versions [(:version-row materialization)]
   :projection-hints (mapv #(assoc % :projection-kind :code-outline)
                           (:outline-rows materialization))})

(defn clojure-source-import-request
  "Build the object-container/import-material request for one clojure blob.
   `source-ref` is opaque to the builder (md pattern); the P2 driver passes
   \"git-blob:<sha>\" per R1. Deterministic: same (raw-text, source-ref, opts)
   -> byte-identical request (import key, object key, unit ids, fingerprint)."
  ([raw-text source-ref] (clojure-source-import-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [legacy-request    (clojure-source-ingest-kernel-request raw-text source-ref opts)
         materialization   (source-materialization legacy-request)
         object-key        (:object-key materialization)
         source-ref-key    (:source-ref-key materialization)
         source-hash-value (:source-hash materialization)
         source-row        (:source-row materialization)
         import-key        (or (:import/key opts)
                               (:import-key opts)
                               (clojure-import-key object-key source-ref-key source-hash-value))
         idempotency-key   (or (:idempotency/key opts)
                               (:idempotency-key opts)
                               import-key)
         payload           (clojure-import-payload materialization)
         material-fingerprint (oc/import-material-fingerprint object-key import-key payload)
         actor (or (:actor opts)
                   {:actor/id "system"
                    :actor/type :system
                    :actor/capabilities #{:object-container/import-material
                                          :source/ingest
                                          :object/edit}})]
     (assoc (envelope/action-request
             {:request-id (oc/request-id legacy-request)
              :request-type :object-container/import-material
              :time-ms (:request/time-ms legacy-request)
              :actor actor
              :branch (:branch legacy-request)
              :context (:context legacy-request)
              :target {:target/kind :object-container-import
                       :target/id import-key
                       :target/address {:source/ref source-ref
                                        :source/hash source-hash-value
                                        :object/key object-key}}
              :action {:action/type :object-container/import-material
                       :action/capability :object-container/import-material
                       :action/params {:source/family :clojure-code
                                       :source/format :clojure}}
              :routing/key [:object-container/import object-key]
              :payload payload
              :causal (:causal legacy-request)
              :provenance (or (:provenance opts)
                              {:source/type :clojure
                               :source/ref source-ref})})
            :partition/key object-key
            :object/key object-key
            :import/key import-key
            :source/family :clojure-code
            :source/format (:source-format source-row)
            :idempotency/key idempotency-key
            :claimed/at-ms (:claimed/at-ms legacy-request)
            :material/fingerprint material-fingerprint))))
