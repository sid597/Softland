;; IMPORTANT: Before modifying this file, re-read
;; docs/current-mental-model/build/trail-view/CONTRACT.md (v1.1 BINDING) and
;; PLAN.md, and check docs/sessions/next-prompt.md.
;; Adhere to all previously decided design decisions. If the plan needs to
;; change, FAIL the phase — do not silently redesign while implementing.
;;
;; TRAIL-VIEW DATA LAYER — WP1 Phase B (build/trail-view/CONTRACT.md §7).
;;
;;   A NEW read-only module (CONTRACT §2 placement ruling): it declares ONLY
;;   mirror PStates + query topologies over the object-container kernel and the
;;   relation kernel. ZERO depots, ZERO stream/microbatch topologies, ZERO own
;;   PStates, and no depot-append or local-write forms anywhere — so "the view
;;   writes no truth" holds BY CONSTRUCTION (gate 14), not by policy.
;;
;;   Query→mirror-query path (CONTRACT §7): this implementation takes the
;;   contract-named CLIENT-COMPOSITION path. The trail-view query topologies
;;   gather ONLY the object-container MATERIAL layers by reading mirror PStates
;;   (the spike-proven mechanism: |hash$$ + local-select>, sibling-index reuse).
;;   The relation layers (R1/R2/R3) and the OC source-ref queries run as CLIENT
;;   foreign-queries and are merged into the bundle/feed by the PURE assemblers.
;;   Wrapper signatures + result shapes are identical to the single-roundtrip
;;   design (consumers are insulated; §7 "latency is not a gate; shape and
;;   honesty are"). This honours the style gate: object-container/ops PStates are
;;   read ONLY via this module's declared mirrors; relation reads go ONLY through
;;   the kernel's public R1/R2/R3 query topologies.
;;
;;   The seam (CONTRACT §2, §7): faces + agents consume ONLY the client wrappers
;;   below (read-context-bundle / read-recent-activity / read-conversation-trail /
;;   read-relation-detail / ->address / resolve-address / current-verdicts /
;;   render-bundle-text) — never PStates, never raw foreign-select.

(ns app.server.rama.trail-view
  "Mirror-only page bundles over object-container and relation reads.
   Takes: object-container and relation module names, addresses, cursors, and page limits.
   Gives: trail pages, relation bundles, and combined read plans.
   Holds nothing."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [clojure.edn :as edn]
            [clojure.string :as str]))

;; ── Constants / vocabulary ───────────────────────────────────────────────────
(def stance-kinds
  "Verdict-carrying relation kinds (CONTRACT §5.2). A verdict IS a relation."
  #{:confirms :refutes :supersedes})

(def default-caps
  "Contract-visible cap constants (CONTRACT §4 'Defaults ... are contract-visible
   constants'). Bounds per-layer collection sizes; overridable via opts :caps."
  {:children 50 :relations 200 :text 4000})

;; The feed's STANDING gap declaration (CONTRACT §6 'Named gaps'; gate 3). Static:
;; an agent reading the feed knows what the feed structurally cannot see yet.
(def feed-uncovered
  [{:omission/kind :feed/uncovered :feed/gap :editor-revisions
    :reason "direct editor ObjectEditPayload revisions are not projected as feed activity (D-008: CLI is the write surface)"}
   {:omission/kind :feed/uncovered :feed/gap :session-metadata
    :reason "session model/session_id metadata is thin on the OC path; the feed renders what exists"}])

;; ── Pure id classification (CONTRACT §4; PLAN §9.4 / trap 4) ─────────────────
(def ^:private material-id-prefixes
  ["oc:doc:" "oc:chat-conversation:" "oc:chat-message:" "oc:tool-call:"
   "oc:tool-result:" "oc:chat-artifact:" "oc:block:" "du:" "src:" "sa:" "ce:"])

(defn bundle-target-class
  "Classify a bundle target id (pure). :material → OC family read; :relation →
   R2 (rel:*); :unresolved → omission :target/unrecognized. Only §4's id set is
   admitted; tc:*/imp:*/unknown route to :unresolved so a foreign-shaped id never
   reaches a mirror read (trap 13)."
  [target-id]
  (let [s (str target-id)]
    (cond
      (str/starts-with? s "rel:") :relation
      (some #(str/starts-with? s %) material-id-prefixes) :material
      :else :unresolved)))

;; ── Pure derivations from the gathered material rows ─────────────────────────
(defn offset-unit-for
  "CONTRACT §4 / trap 12 / gate 5: markdown anchors are CHAR offsets, transcript
   anchors are BYTE offsets. Derived from the source artifact's :source-format."
  [source-artifact]
  (case (:source-format source-artifact)
    :markdown :chars
    :transcript :bytes
    nil))

(defn raw-stored?
  "CONTRACT §4: markdown raw is stored; transcript raw is addressed (:stored? false)."
  [source-artifact]
  (= :markdown (:source-format source-artifact)))

(defn target-kind-of
  [target-id container unit]
  (let [ck (:container-kind container)
        s  (str target-id)]
    (cond
      ck (case ck
           :document :doc
           :chat-conversation :conversation
           :chat-message :message
           :tool-call :tool-call
           :tool-result :tool-result
           :block :block
           ck)
      (str/starts-with? s "du:") :block
      (str/starts-with? s "src:") :source
      (some? unit) :block
      :else :unresolved)))

(defn bundle-source-id [container unit] (or (:source-id container) (:source-id unit)))
(defn bundle-revision-id [container] (:current-revision-id container))

(defn project-anchor
  [a]
  {:anchor-id (:source-anchor-id a)
   :start (:start-offset a)
   :end (:end-offset a)
   :block-path (:block-path a)})

(defn project-child
  [edge]
  {:id (:child-target-id edge)
   :order-key (:child-order-key edge)
   :kind (:child-target-kind edge)
   :preview nil})   ;; mechanical preview filled by the caps step; never a paraphrase (§9.6)

(defn gather-target-material
  "PURE (runs inside the context-bundle query topology). Builds a target's L0/L1/
   L2/L5-partial material from the gathered mirror rows. Relations (L3), verdicts
   (L4) and last-attested (L5) are added client-side after R1 (assemble-bundle)."
  [target-id container revision source-artifact unit graduation
   anchors children parents outline conv-proj]
  (let [kind (target-kind-of target-id container unit)
        content-text (or (:current-content-text container)
                         (:current-content-text graduation)
                         (:derived-content-text unit))
        anchor-vec (mapv project-anchor (or anchors []))
        child-vec  (mapv project-child (or children []))
        parent     (first (or parents []))
        order-sem  (if (= :bytes (offset-unit-for source-artifact)) :byte-offset :block-path)]
    {:id target-id
     :kind kind
     :identity {:created-by (or (:created-by container) (:created-by source-artifact))
                :current-revision-id (:current-revision-id container)
                :content-hash (or (:current-content-hash container)
                                  (:derived-content-hash unit))
                :container-kind (:container-kind container)
                :source-id (bundle-source-id container unit)}
     :material {:content-text content-text
                :raw {:stored? (raw-stored? source-artifact)
                      :source-ref (:source-ref source-artifact)
                      :source-id (:source-id source-artifact)
                      :offset-unit (offset-unit-for source-artifact)
                      :byte-count (:content-byte-count source-artifact)
                      :text (when (raw-stored? source-artifact)
                              (:source-raw-text source-artifact))
                      :anchor (when source-artifact
                                {:file-path (:source-ref source-artifact)
                                 :start 0
                                 :end (:content-byte-count source-artifact)})}
                :anchors anchor-vec}
     :structure {:parent (when parent
                           {:id (:parent-target-id parent) :edge-id (:edge-id parent)})
                 :children child-vec
                 :order-semantics order-sem}
     ;; L5 partial: OC created-at is an ARRIVAL clock; md ingest has no claimed
     ;; time (§6 note) → claimed nil-honest. last-attested/last-walked added later.
     :times {:created {:claimed-ms nil
                       :arrival-ms (or (:created-at-ms container) (:created-at-ms source-artifact))}
             :last-changed {:claimed-ms nil
                            :arrival-ms (or (:created-at-ms revision) (:created-at-ms container))}
             :last-attested-ms nil
             :last-walked-ms nil}          ;; ALWAYS nil in WP1 (§9.2 / gate 8)
     ;; totals for the exactness/omissions reconciliation (gate 3)
     :totals {:children (count (or children []))
              :anchors (count (or anchors []))}
     :conversation-entries (count (or conv-proj []))}))

(defn material-pairs->map
  [pairs]
  (persistent!
   (reduce (fn [m pair] (assoc! m (nth pair 0) (nth pair 1)))
           (transient {})
           (or pairs []))))

;; ── Feed row window helpers (pure) ──────────────────────────────────────────
(defn file-offset-updated-ms [row] (:updated-at-ms row))
(defn source-completion-ms [row] (:completed-at-ms row))

(defn flatten-row-groups
  "A |origin +vec-agg over a per-partition subselect yields a vector of vectors
   (one inner vector per source partition). Flatten to a single row vector."
  [groups]
  (vec (apply concat (or groups []))))

;; ── Display names (F-L3 / git-spine CONTRACT §3.D; INPUTS §5.7b) ─────────────
;;   A feed/bundle target's human name lives ADJACENT in the payload (the
;;   source-ref / file-path), never as persisted OC truth (the display-name
;;   TRUTH seam §5.7c is deferred). These PURE fns project it: md/doc/file →
;;   basename; commit artifacts (source-ref "git-commit:<sha>") → <sha7>. The
;;   View-3 bundle rendering additionally reads the §3.A `subject:` line to show
;;   "<sha7> · <subject>". The raw id/hash stays the honest fallback wherever a
;;   name is absent (card falls back to :id, cards.cljc:275; text keeps the
;;   `== <tid>` heading).
(def ^:private commit-source-ref-prefix
  "§3.A: a commit artifact rides the md path with this source-ref prefix."
  "git-commit:")

(defn commit-source-ref?
  "True when a source-ref names a git-commit artifact (§3.A / §3.D)."
  [source-ref]
  (and (string? source-ref) (str/starts-with? source-ref commit-source-ref-prefix)))

(defn source-ref->sha
  "The sha carried verbatim after the `git-commit:` prefix (§3.A), else nil."
  [source-ref]
  (when (commit-source-ref? source-ref)
    (subs source-ref (count commit-source-ref-prefix))))

(defn sha7
  "Git short-sha: the first 7 chars of a sha, tolerant of shorter input.
   nil on nil/blank so a degenerate ref never yields a blank display-name
   (the raw id must stay the honest fallback)."
  [sha]
  (let [s (str sha)]
    (when-not (str/blank? s)
      (subs s 0 (min 7 (count s))))))

(defn basename
  "Last '/'-separated segment of a path-like ref (pure). nil on nil/blank input
   so downstream `(or display-name id)` falls back to the id, never to \"\"."
  [path]
  (let [s (str path)]
    (when-not (str/blank? s)
      (peek (str/split s #"/")))))

(defn source-ref->display-name
  "F-L3 FEED display-name from a source-ref (CONTRACT §3.D / G9): commit
   artifacts → <sha7>; md/doc → basename. nil when there is no ref, so the id
   stays the honest fallback (cards.cljc:275). The feed row carries NO subject
   (validator S2); the subject rides the bundle rendering below."
  [source-ref]
  (when source-ref
    (if (commit-source-ref? source-ref)
      (sha7 (source-ref->sha source-ref))
      (basename source-ref))))

(defn subject-line
  "Parse the `subject:` header line out of a §3.A labeled canonical commit text
   (the cross-builder interface, CONTRACT §3.A). The header `subject:` precedes
   any body, so the first match is the commit subject. Returns it trimmed, else nil."
  [content-text]
  (when content-text
    (some (fn [line]
            (when (str/starts-with? line "subject:")
              (str/trim (subs line (count "subject:")))))
          (str/split-lines content-text))))

(defn bundle-display-name
  "F-L3 View-3 display-name for a MATERIAL bundle target (CONTRACT §3.D / G9).
   Commit material → \"<sha7> · <subject>\" (subject read from the §3.A
   content-text); md/doc material → basename of the source-ref; nil when neither
   applies (the raw tid then stays the honest fallback in the text heading)."
  [tb]
  (let [source-ref (get-in tb [:material :raw :source-ref])]
    (cond
      (commit-source-ref? source-ref)
      (let [s7      (sha7 (source-ref->sha source-ref))
            subject (subject-line (get-in tb [:material :content-text]))]
        (if (str/blank? subject) s7 (str s7 " · " subject)))
      source-ref (basename source-ref)
      :else nil)))

;; ─────────────────────────────────────────────────────────────────────────────
;;   MODULE  (read-only by construction: no depots, no ETL, no own PStates)
;; ─────────────────────────────────────────────────────────────────────────────
(def ^:private oc-module-name
  "app.server.rama.object-container/object-container-module")
(def ^:private oc-ops-module-name
  "app.server.rama.object-container/object-container-transcript-ops-module")

(defmodule trail-view-module [setup topologies]
  ;; ── Mirror PStates from the object-container kernel (all partition-by-object-key
  ;;    → sibling-index reuse: one |hash$$ routes the whole family, spike part 4).
  (mirror-pstate setup $$containers-by-id oc-module-name "$$containers-by-id")
  (mirror-pstate setup $$revisions-by-id oc-module-name "$$revisions-by-id")
  (mirror-pstate setup $$source-artifacts-by-id oc-module-name "$$source-artifacts-by-id")
  (mirror-pstate setup $$source-anchors-by-target oc-module-name "$$source-anchors-by-target")
  (mirror-pstate setup $$composition-children-by-parent oc-module-name "$$composition-children-by-parent")
  (mirror-pstate setup $$composition-parent-by-child oc-module-name "$$composition-parent-by-child")
  (mirror-pstate setup $$outline-by-document oc-module-name "$$outline-by-document")
  (mirror-pstate setup $$transcript-conversation-projection oc-module-name "$$transcript-conversation-projection")
  (mirror-pstate setup $$derived-units-by-id oc-module-name "$$derived-units-by-id")
  (mirror-pstate setup $$unit-graduations-by-id oc-module-name "$$unit-graduations-by-id")
  (mirror-pstate setup $$transcript-last-message-by-conversation oc-module-name "$$transcript-last-message-by-conversation")
  (mirror-pstate setup $$source-ingest-completions-by-ref oc-module-name "$$source-ingest-completions-by-ref")
  ;; ── Mirror PStates from the transcript-ops module (default partitioner) ──
  (mirror-pstate setup $$transcript-file-offsets oc-ops-module-name "$$transcript-file-offsets")

  ;; ── B1: context-bundle — MATERIAL gathering only. Per material target, ONE
  ;;    |hash$$ to the OC family task, then sibling reads (container/revision/
  ;;    source-artifact/unit/graduation/anchors/children/parents/outline/conv).
  ;;    Relations (R1) + verdicts are merged client-side (assemble-bundle).
  (<<query-topology topologies "context-bundle" [*target-ids :> *result]
    (ops/explode *target-ids :> *target-id)
    (oc/extract-object-key *target-id :> *object-key)
    (|hash$$ $$containers-by-id *object-key)
    (local-select> [(keypath *target-id)] $$containers-by-id :> *container)
    (local-select> [(keypath *target-id)] $$derived-units-by-id :> *unit)
    (local-select> [(keypath *target-id)] $$unit-graduations-by-id :> *graduation)
    (local-select> [(keypath *target-id) (subselect MAP-VALS)]
                   $$source-anchors-by-target :> *anchors)
    (local-select> [(keypath *target-id) (subselect MAP-VALS)]
                   $$composition-children-by-parent :> *children)
    (local-select> [(keypath *target-id) (subselect MAP-VALS)]
                   $$composition-parent-by-child :> *parents)
    (local-select> [(keypath *target-id) (subselect MAP-VALS)]
                   $$outline-by-document :> *outline)
    (local-select> [(keypath *target-id) (subselect MAP-VALS)]
                   $$transcript-conversation-projection :> *conv-proj)
    (bundle-source-id *container *unit :> *source-id)
    (bundle-revision-id *container :> *revision-id)
    (oc/string-present? *source-id :> *has-source?)
    (oc/string-present? *revision-id :> *has-revision?)
    (<<if *has-source?
      (local-select> [(keypath *source-id)] $$source-artifacts-by-id :> *source-artifact)
      (else>)
      (identity nil :> *source-artifact))
    (<<if *has-revision?
      (local-select> [(keypath *revision-id)] $$revisions-by-id :> *revision)
      (else>)
      (identity nil :> *revision))
    (gather-target-material *target-id *container *revision *source-artifact *unit
                            *graduation *anchors *children *parents *outline *conv-proj
                            :> *material)
    (vector *target-id *material :> *pair)
    (|origin)
    (aggs/+vec-agg *pair :> *pairs)
    (material-pairs->map *pairs :> *result))

  ;; ── B3: conversation-trail — single family task, one subindexed page +
  ;;    last-message tail. Byte-offset order comes from the projection order-key.
  (<<query-topology topologies "conversation-trail" [*conversation *cursor *limit :> *result]
    (oc/extract-object-key *conversation :> *object-key)
    (|hash$$ $$transcript-conversation-projection *object-key)
    (local-select> [(keypath *conversation)
                    (subselect (sorted-map-range-from *cursor *limit) MAP-VALS)]
                   $$transcript-conversation-projection :> *rows)
    (local-select> [(keypath *conversation)]
                   $$transcript-last-message-by-conversation :> *last)
    (identity {:rows *rows :last *last} :> *result)
    (|origin))

  ;; ── B2: recent-activity — MATERIAL branches. Two clean cross-partition mirror
  ;;    scans (file-updated + source-ingested); the relation-transition branch
  ;;    (R3) is merged client-side. Small registries at phase-1 scale (§6 read
  ;;    plan); |all$$ fans every source partition, |origin aggregates once.
  (<<query-topology topologies "recent-file-activity" [:> *result]
    (|all$$ $$transcript-file-offsets)
    (local-select> [(subselect MAP-VALS)] $$transcript-file-offsets :> *rows)
    (|origin)
    (aggs/+vec-agg *rows :> *groups)
    (flatten-row-groups *groups :> *result))

  (<<query-topology topologies "recent-source-activity" [:> *result]
    (|all$$ $$source-ingest-completions-by-ref)
    (local-select> [(subselect MAP-VALS MAP-VALS)] $$source-ingest-completions-by-ref :> *rows)
    (|origin)
    (aggs/+vec-agg *rows :> *groups)
    (flatten-row-groups *groups :> *result)))

;; ─────────────────────────────────────────────────────────────────────────────
;;   FOREIGN CLIENT — the ONLY product surface (CONTRACT §7 seam).
;;
;;   Faces + agents consume ONLY these wrappers + the exported pure fns. Relation
;;   reads go through the kernel's public R1/R2/R3; OC material through this
;;   module's mirrors; OC source-ref queries (trail/via) through the kernel's own
;;   public query topologies (queries ARE the public surface — style gate F-2).
;; ─────────────────────────────────────────────────────────────────────────────

(declare read-context-bundle read-recent-activity read-conversation-trail
         read-relation-detail resolve-address resolve-via)

;; ── Pure edge / verdict projections (CONTRACT §4 <edge>; §5.2 verdict fold) ──
(defn project-edge
  "RelationEdgeRow → CONTRACT §4 <edge>. `:written-by` appears ONLY when the
   envelope actor differs from the asserter (gate 10 token economy)."
  [row]
  {:relation-id (:relation-id row)
   :kind (:relation-kind row)
   :from {:kind (:target-kind (:from row)) :id (:target-id (:from row))}
   :to {:kind (:target-kind (:to row)) :id (:target-id (:to row))}
   :status (:relation-status row)
   :asserted-by (:asserter-actor-id row)
   :asserter-type (:asserter-type row)
   :written-by (when (not= (:envelope-actor-id row) (:asserter-actor-id row))
                 (:envelope-actor-id row))
   :evidence {:source-id (:evidence-source-id row) :anchor-id (:evidence-anchor-id row)}
   :note (:note row)
   :first-asserted-at-ms (:first-asserted-at-ms row)
   :last-changed-at-ms (:status-changed-at-ms row)})

(defn verdict-of
  [row]
  (assoc (project-edge row) :current true))

(defn current-verdicts
  "PURE fold, exported (CONTRACT §5.2; gates 6,7). Per judged target (a stance
   row's `to`), per asserter: the LATEST **asserted** stance-kind row (all three
   kinds fold uniformly incl. :supersedes). latest = max status-changed-at-ms,
   tiebreak relation-id. Retracted stances drop; disagreeing asserters are BOTH
   current, never merged."
  [edges]
  (->> (or edges [])
       (filter #(contains? stance-kinds (:relation-kind %)))
       (filter #(= :asserted (:relation-status %)))
       (group-by (fn [e] [(:target-id (:to e)) (:asserter-actor-id e)]))
       (map (fn [[_ grp]]
              (last (sort-by (juxt :status-changed-at-ms :relation-id) grp))))
       (map verdict-of)
       vec))

;; ── Family grouping (CONTRACT §4, trap 6; F-6; E-6) ─────────────────────────
(defn- endpoint-in-family? [ref object-key] (= (:target-key ref) object-key))

(defn group-relations
  "Split a family's R1 rows (all share `object-key`) into `:this` (the requested
   id is an endpoint) and `:in-family` (a sibling endpoint). F-6: a row between
   two distinct siblings files ONCE under `:in-family` keyed by `from`. E-6: a
   unary :none row lands in `:this` (its from is the requested id), never keyed by
   the nil :none id."
  [rows target-id object-key]
  (reduce
   (fn [acc row]
     (let [from (:from row) to (:to row)
           kind (:relation-kind row)
           edge (project-edge row)
           direct? (or (= target-id (:target-id from)) (= target-id (:target-id to)))
           from-fam? (endpoint-in-family? from object-key)
           to-fam? (endpoint-in-family? to object-key)]
       (cond
         direct?                (update-in acc [:this kind] (fnil conj []) edge)
         (and from-fam? to-fam?) (update-in acc [:in-family (:target-id from) kind] (fnil conj []) edge)
         to-fam?                (update-in acc [:in-family (:target-id to) kind] (fnil conj []) edge)
         from-fam?              (update-in acc [:in-family (:target-id from) kind] (fnil conj []) edge)
         :else                  acc)))
   {:this {} :in-family {}}
   (or rows [])))

;; ── Caps → omissions (CONTRACT §4 exactness rule, trap 7; gate 3) ───────────
(defn cap-children
  [children cap]
  (let [total (count (or children []))
        kept  (vec (take cap (or children [])))]
    [kept (when (> total cap)
            {:layer :structure :dropped (- total cap) :cap cap
             :cursor (:order-key (last kept))})]))

(defn- relation-entries
  [this in-family]
  (concat
   (for [[kind edges] this, edge edges] [:this nil kind edge])
   (for [[oid kinds] in-family, [kind edges] kinds, edge edges] [:in-family oid kind edge])))

(defn cap-relations
  [this in-family cap]
  (let [entries (relation-entries this in-family)
        total   (count entries)
        kept    (take cap entries)
        [t i] (reduce (fn [[t i] [where oid kind edge]]
                        (if (= where :this)
                          [(update t kind (fnil conj []) edge) i]
                          [t (update-in i [oid kind] (fnil conj []) edge)]))
                      [{} {}] kept)]
    [t i (when (> total cap)
           {:layer :relations :dropped (- total cap) :cap cap})]))

;; ── Addresses (CONTRACT §3: literal EDN, round-trip law, gate 2) ────────────
(defn ->address
  "One-line literal EDN list `(trail/<query> <params-map>)` (trap 8)."
  [query params]
  (list (symbol "trail" (name query)) params))

(defn assemble-bundle
  "PURE. Merge gathered MATERIAL (from the context-bundle topology) with the
   relation layers (R1 rel-map) and R2 relation-detail rows into the CONTRACT §4
   bundle shape. `rendered-at` is stamped by the wrapper (client clock)."
  [material-map targets material-ids relation-ids unresolved rel-map rel-details opts address]
  (let [caps (merge default-caps (get opts :caps {}))
        material-bundles
        (into {}
              (for [tid material-ids]
                (let [material   (get material-map tid)
                      object-key (oc/extract-object-key tid)
                      fam-rows   (get rel-map object-key [])
                      {:keys [this in-family]} (group-relations fam-rows tid object-key)
                      verdicts   (current-verdicts fam-rows)
                      direct     (filter #(or (= tid (:target-id (:from %)))
                                              (= tid (:target-id (:to %)))) fam-rows)
                      last-attested (when (seq direct)
                                      (apply max (map :status-changed-at-ms direct)))
                      [kept-children child-om] (cap-children
                                                (get-in material [:structure :children]) (:children caps))
                      [cap-this cap-infam rel-om] (cap-relations this in-family (:relations caps))
                      omissions (into [] (remove nil? [child-om rel-om]))
                      tb (-> material
                             (assoc-in [:structure :children] kept-children)
                             (assoc-in [:times :last-attested-ms] last-attested)
                             (assoc :relations {:this cap-this :in-family cap-infam}
                                    :verdicts {:current (group-by :asserted-by verdicts)
                                               :derived-from :relations}
                                    :address (->address :context-bundle {:targets [tid]})
                                    :omissions omissions)
                             (dissoc :totals :conversation-entries))]
                  [tid tb])))
        relation-bundles
        (into {}
              (for [rid relation-ids]
                (let [detail (get rel-details rid)
                      row (:row detail)]
                  [rid {:id rid :kind :relation
                        :address (->address :relation-detail {:relation-id rid})
                        :relation (when row (project-edge row))
                        :history (:history detail)
                        :omissions []}])))]
    {:bundle/address address
     :bundle/targets (merge material-bundles relation-bundles)
     :bundle/omissions (mapv (fn [tid] {:id tid :reason :target/unrecognized}) unresolved)}))

(defn read-context-bundle
  "CONTRACT §4/§7. One roundtrip of MATERIAL (trail-view topology over OC mirrors)
   + R1 (relations, batch-first) + R2 (rel:* targets), merged by assemble-bundle."
  [rt targets opts]
  (let [targets      (vec targets)
        classes      (group-by bundle-target-class targets)
        material-ids (vec (:material classes))
        relation-ids (vec (:relation classes))
        unresolved   (vec (:unresolved classes))
        material-map (if (seq material-ids)
                       (foreign-invoke-query (:context-bundle-query rt) material-ids)
                       {})
        object-keys  (vec (distinct (map oc/extract-object-key material-ids)))
        rel-map      (if (seq object-keys)
                       (rk/read-relations-for-targets rt object-keys nil
                                                       (boolean (:include-retracted? opts)))
                       {})
        rel-details  (into {} (map (fn [rid] [rid (read-relation-detail rt rid)]) relation-ids))
        address      (->address :context-bundle {:targets targets
                                                 :layers (get opts :layers :all)
                                                 :caps (get opts :caps {})})
        bundle       (assemble-bundle material-map targets material-ids relation-ids
                                      unresolved rel-map rel-details opts address)]
    (assoc bundle :bundle/rendered-at-ms (System/currentTimeMillis))))

;; ── Recent-activity feed (CONTRACT §6; gate 4) ──────────────────────────────
(defn- relation-activity-entry
  [row]
  {:entry/kind :relation-transition
   :entry/target {:id (:from-id row) :kind (:from-kind row)}
   :entry/address (->address :context-bundle {:targets [(:from-id row)]})
   :time/claimed-ms (:claimed-at-ms row)
   :time/arrival-ms (:arrival-at-ms row)
   :entry/actor {:asserted-by (:asserter-actor-id row)
                 :written-by (when (not= (:envelope-actor-id row) (:asserter-actor-id row))
                               (:envelope-actor-id row))}
   ;; Lineage endpoints (Trunk-5 gate fix, 2026-07-06): the activity row has
   ;; carried from/to since R3 landed; the entry MUST project them or the
   ;; client's lanes-from-edges pass sees zero lineage over the live corpus
   ;; (fixture-vs-live drift class — DIFF_FALSIFICATION_CROSS B1). Projected
   ;; verbatim from the row, never fabricated.
   :entry/detail {:kind (:relation-kind row) :status (:relation-status row)
                  :relation-id (:relation-id row)
                  :from {:id (:from-id row) :kind (:from-kind row)}
                  :to   {:id (:to-id row) :kind (:to-kind row)}}})

(defn- file-activity-entry
  [row]
  {:entry/kind :transcript-file-updated
   :entry/target {:id (:file-key row) :kind :transcript-file
                  :display-name (basename (:file-path row))}
   :entry/address (->address :context-bundle {:targets [(:file-key row)]})
   :time/claimed-ms nil
   :time/arrival-ms (file-offset-updated-ms row)
   :entry/actor {:asserted-by nil :written-by nil}
   :entry/detail {:file-path (:file-path row) :line-count (:line-count row)}})

(defn- source-activity-entry
  [row]
  {:entry/kind :source-ingested
   :entry/target {:id (:document-container-id row) :kind :doc
                  :display-name (source-ref->display-name (:source-ref row))}
   :entry/address (->address :context-bundle {:targets [(:document-container-id row)]})
   ;; Two clocks (t4-spine seam 1, band-2 stamp): claimed rides the completion
   ;; row's ADDITIVE :claimed-at-ms, populated ONLY when the import request
   ;; declared a material-claimed clock (`:claimed/at-ms` — git-spine commits:
   ;; the committer clock). Watcher md ingests declare none, so md rows stay
   ;; claimed-nil HONESTLY (their :request/time-ms is a wall-clock default,
   ;; never a claim). Rows written before the field existed read nil too.
   :time/claimed-ms (:claimed-at-ms row)
   :time/arrival-ms (source-completion-ms row)
   :entry/actor {:asserted-by nil :written-by nil}
   :entry/detail {:source-ref (:source-ref row)
                  :derived-unit-count (:derived-unit-count row)}})

(defn- in-window? [ms from to] (and ms (<= (long from) (long ms)) (<= (long ms) (long to))))

(defn assemble-feed
  "PURE. Merge the three branches, window-select by ARRIVAL always (F-1), order
   WITHIN the window by the chosen clock, attach the standing :feed/uncovered."
  [activity-rows file-offsets source-completions window order address]
  (let [from (:from-ms window) to (:to-ms window)
        entries (concat (map relation-activity-entry activity-rows)
                        (map file-activity-entry file-offsets)
                        (map source-activity-entry source-completions))
        in-win  (filter #(in-window? (:time/arrival-ms %) from to) entries)
        clock-k (if (= :claimed order) :time/claimed-ms :time/arrival-ms)
        ordered (sort-by (fn [e] [(or (clock-k e) Long/MIN_VALUE) (:time/arrival-ms e)])
                         #(compare %2 %1) in-win)]      ;; desc by chosen clock (CONTRACT §6)
    {:feed/address address
     :feed/entries (vec ordered)
     :feed/omissions feed-uncovered}))

(defn read-recent-activity
  "CONTRACT §6/§7. Window selection is ALWAYS arrival-time; `:order` covers
   in-window ordering. A :claimed-window SELECTION request is refused (§9.10)."
  [rt window opts]
  (when (= :claimed (:select-clock opts))
    (throw (ex-info "claimed-window selection refused: the recent-activity window is arrival-time only (CONTRACT §9.10)"
                    {:window window :select-clock :claimed})))
  (let [order   (get opts :order :arrival)
        lo      (rk/bucket-key (:from-ms window))
        hi      (rk/bucket-key (:to-ms window))
        activity (rk/read-relation-activity rt lo hi)
        files    (foreign-invoke-query (:recent-file-activity-query rt))
        sources  (foreign-invoke-query (:recent-source-activity-query rt))
        address  (->address :recent-activity {:window window :order order})
        feed     (assemble-feed activity files sources window order address)]
    (assoc feed :feed/rendered-at-ms (System/currentTimeMillis))))

;; ── Conversation trail (CONTRACT §7; gate 11) ───────────────────────────────
(defn read-conversation-trail
  [rt conversation cursor limit]
  (let [cur (or cursor "")
        lim (or limit oc/default-outline-page-size)
        raw (foreign-invoke-query (:conversation-trail-query rt) conversation cur lim)
        rows (vec (:rows raw))
        ;; sorted-map-range-from is inclusive-of-start, so an exclusive next-cursor
        ;; = last order-key + a minimal greater suffix (order-keys are distinct and
        ;; none is a prefix of another, so this lands strictly between pages).
        next-cursor (when (= (count rows) lim) (str (:order-key (last rows)) (char 1)))
        address (->address :conversation-trail
                           {:conversation conversation :cursor cursor :limit limit})]
    {:trail/address address
     :trail/rendered-at-ms (System/currentTimeMillis)
     :trail/conversation conversation
     :trail/entries rows
     :trail/last (:last raw)
     :trail/next-cursor next-cursor
     :trail/omissions (if next-cursor [{:layer :trail :cursor next-cursor}] [])}))

;; ── relation-detail (CONTRACT §3 law 4; delegates to kernel R2) ─────────────
(defn read-relation-detail
  [rt relation-id]
  (rk/read-relation-detail rt relation-id))

;; ── View-spec resolution (CONTRACT §3; gate 12; F-3 residue §10) ────────────
(defn resolve-via
  "Resolve a view-spec doc (an ingested EDN doc) to a query result. `:latest`
   tracks the newest source version of the spec's source-ref; a pinned `:rev`
   (a source-version-key = source-hash) keeps resolving to that version's params.
   `:overrides` shallow-merge at params level. The view WRITES nothing."
  [rt {:keys [spec rev overrides]}]
  (let [bundle     (read-context-bundle rt [spec] {})
        tb         (get-in bundle [:bundle/targets spec])
        source-ref (get-in tb [:material :raw :source-ref])
        source-row (if (or (nil? rev) (= :latest rev))
                     (foreign-invoke-query (:read-latest-source-by-ref-query rt) source-ref)
                     (foreign-invoke-query (:read-source-by-ref-version-query rt) source-ref rev))
        spec-map   (edn/read-string (:source-raw-text source-row))
        query      (keyword (name (:spec/query spec-map)))
        params     (merge (:spec/params spec-map) overrides)]
    (resolve-address rt (->address query params))))

(defn resolve-address
  "Parse a rendered address (string or list) and re-invoke the matching wrapper
   over CURRENT truth (CONTRACT §3 round-trip law; NO as-of, §9.1)."
  [rt address]
  (let [form   (if (string? address) (edn/read-string address) address)
        head   (name (first form))
        params (second form)]
    (case head
      "context-bundle"    (read-context-bundle rt (:targets params) (dissoc params :targets))
      "recent-activity"   (read-recent-activity rt (:window params) (dissoc params :window))
      "conversation-trail" (read-conversation-trail rt (:conversation params)
                                                    (:cursor params) (:limit params))
      "relation-detail"   (read-relation-detail rt (:relation-id params))
      "via"               (resolve-via rt params)
      (throw (ex-info "unknown trail address head" {:head head :address address})))))

;; ── View-3 text projection (CONTRACT §8; gate 13) ───────────────────────────
(defn- iso8601
  "Format a client-supplied ms stamp (NOT a wall-clock read) as ISO-8601."
  [ms]
  (if ms (str (java.time.Instant/ofEpochMilli (long ms))) "unknown"))

(defn- fmt-actor [asserted-by written-by]
  (if (and written-by (not= written-by asserted-by))
    (str asserted-by " (via " written-by ")")
    (str asserted-by)))

(defn- edge-line [prefix edge]
  ;; G11 fix (2026-07-05): print BOTH endpoints — dropping the far end made
  ;; View-3 silently lose WHO produced a target (the map must not lie; the
  ;; design's R6 rule: an edge always names its far end). Full triple:
  ;; from -> kind -> to.
  (let [ev (get-in edge [:evidence :anchor-id])]
    (str "   " (get-in edge [:from :id]) " " prefix " " (name (:kind edge))
         " " (get-in edge [:to :id])
         " by " (fmt-actor (:asserted-by edge) (:written-by edge))
         " @" (:last-changed-at-ms edge)
         (when ev (str " [ev " ev "]")))))

(defn render-bundle-text
  "CONTRACT §8: deterministic, versioned View-3 text. Reads rendered-at FROM the
   bundle (does not generate it). All markers present; ≤ 4,000 chars on the gate
   fixture; `walked unknown` printed while the field is nil (I-2 exactness)."
  [bundle]
  (let [lines
        (concat
         [(str ";; trail-text v0 @ " (iso8601 (:bundle/rendered-at-ms bundle)))
          (str ";; address: " (pr-str (:bundle/address bundle)))]
         (mapcat
          (fn [[tid tb]]
            (let [times (:times tb)
                  created (get-in times [:created])
                  attested (:last-attested-ms times)
                  verdicts (apply concat (vals (get-in tb [:verdicts :current])))
                  this-rels (apply concat (vals (get-in tb [:relations :this])))]
              (concat
               [(str "== " tid " (" (name (or (:kind tb) :unresolved)) " \""
                     (or (bundle-display-name tb)
                         (get-in tb [:identity :container-kind]) "") "\")")
                (str "   created " (or (:arrival-ms created) (:claimed-ms created))
                     " by " (or (get-in tb [:identity :created-by]) "unknown")
                     " | last-changed " (or (get-in tb [:times :last-changed :arrival-ms]) "unknown")
                     " | attested " (or attested "never")
                     " | walked unknown")
                (str "-- material: " (count (get-in tb [:structure :children])) " children, "
                     (or (get-in tb [:material :raw :byte-count]) 0) " raw ("
                     (if (get-in tb [:material :raw :stored?]) "stored" "addressed") " "
                     (or (get-in tb [:material :raw :source-ref]) "-") ")")
                "-- relations:"]
               (map #(edge-line "->" %) this-rels)
               ["-- verdicts:"]
               (map (fn [v]
                      (str "   " (:asserted-by v) ": " (name (:kind v)) " "
                           (get-in v [:to :id]) " @" (:last-changed-at-ms v)
                           (when-let [ev (get-in v [:evidence :anchor-id])] (str " [ev " ev "]"))
                           " (current)"))
                    verdicts)
               [(str "-- omissions: "
                     (if (seq (:omissions tb)) (pr-str (:omissions tb)) "none"))])))
          (:bundle/targets bundle)))]
    (str/join "\n" lines)))

;; ── Runtime (launches all four modules in dependency order; §1) ─────────────
(def ^:private oc-module-var oc/object-container-module)

(defn start-trail-view-runtime!
  "One IPC, four modules launched in dependency order (source modules BEFORE
   trail-view; Rama enforces mirror dependencies at launch). Returns a runtime map
   with the trail-view query handles, the relation kernel public queries (R1/R2/R3),
   the OC source-ref queries (trail/via), and the ingest depots/await handles the
   test fixture drives."
  ([] (start-trail-view-runtime! {}))
  ([{:keys [tasks threads] :or {tasks 4 threads 2}}]
   (let [ipc (create-ipc)
         opts {:tasks tasks :threads threads}
         oc-name (get-module-name oc/object-container-module)
         oc-ops-name (get-module-name oc/object-container-transcript-ops-module)
         rel-name (get-module-name rk/relation-kernel-module)
         tv-name (get-module-name trail-view-module)]
     (launch-module! ipc oc/object-container-module opts)
     (launch-module! ipc oc/object-container-transcript-ops-module opts)
     (launch-module! ipc rk/relation-kernel-module opts)
     (launch-module! ipc trail-view-module opts)
     {:ipc ipc
      :trail-view-module-name tv-name
      :oc-module-name oc-name
      :oc-ops-module-name oc-ops-name
      :module-name rel-name
      ;; ingest depots + await handles (fixture drive)
      :object-container-requests-depot (foreign-depot ipc oc-name "*object-container-requests-depot")
      :decisions-by-audit-id (foreign-pstate ipc oc-name "$$decisions-by-audit-id")
      :relation-request-depot (foreign-depot ipc rel-name "*relation-request-depot")
      ;; OC direct handles (fixture verification only — gate 5 span, gate 12 pinning)
      :containers-by-id (foreign-pstate ipc oc-name "$$containers-by-id")
      :source-artifacts-by-id (foreign-pstate ipc oc-name "$$source-artifacts-by-id")
      :source-anchors-by-target (foreign-pstate ipc oc-name "$$source-anchors-by-target")
      :source-versions-by-ref (foreign-pstate ipc oc-name "$$source-versions-by-ref")
      :source-latest-by-ref (foreign-pstate ipc oc-name "$$source-latest-by-ref")
      :outline-by-document (foreign-pstate ipc oc-name "$$outline-by-document")
      ;; relation kernel V1 + public queries
      :relations-by-id (foreign-pstate ipc rel-name "$$relations-by-id")
      :activity-by-bucket (foreign-pstate ipc rel-name "$$relation-activity-by-bucket")
      :relations-for-targets-query (foreign-query ipc rel-name "relations-for-targets")
      :relation-detail-query (foreign-query ipc rel-name "relation-detail")
      :relation-activity-query (foreign-query ipc rel-name "relation-activity")
      ;; OC public queries (client-level, for trail/via)
      :read-latest-source-by-ref-query (foreign-query ipc oc-name "read-latest-source-by-ref")
      :read-source-by-ref-version-query (foreign-query ipc oc-name "read-source-by-ref-version")
      :read-current-revision-query (foreign-query ipc oc-name "read-current-revision")
      ;; trail-view query topologies (the product read surface)
      :context-bundle-query (foreign-query ipc tv-name "context-bundle")
      :conversation-trail-query (foreign-query ipc tv-name "conversation-trail")
      :recent-file-activity-query (foreign-query ipc tv-name "recent-file-activity")
      :recent-source-activity-query (foreign-query ipc tv-name "recent-source-activity")})))

(defn close-trail-view-runtime!
  [rt]
  (when-let [ipc (:ipc rt)]
    (try (.close ^java.lang.AutoCloseable ipc) (catch Exception _ nil))))
