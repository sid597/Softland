(ns app.server.worn.facet-master
  "Foreign-client adapter for revisioned facet material in object-container.
   Given a caller-owned runtime, master spec, source or revision id and request
   options, builds/imports candidates and edits a separate active-pointer
   container. Reads latest, active and parent-linked history independently;
   multi-call operations are not one snapshot or transaction.

   Durable requests, decisions, source, revisions and pointers belong to the
   object-container module. This namespace owns no handles or worker lifetime.
   Accepted writes not already observed by request id increment the borrowed
   ingest-epoch notification atom. Instance masters reuse this same adapter;
   material-truth adds their episode discovery index."
  (:require [clojure.string :as str]
            [app.server.rama.envelope :as envelope]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.ingest-epoch :as ingest-epoch]
            [app.server.worn.activation-event :as activation-event]
            [app.server.worn.binding-material :as binding-material]
            [app.server.worn.facet-engine :as facet-engine]))

(defn master-id
  "Return the spec's object-container routing key and facet-master identity."
  [spec]
  (:facet-master/id spec))

(defn source-ref
  "Return the spec's stable source reference, shared by its candidate versions."
  [spec]
  (:facet-master/source-ref spec))

(defn document-id
  "Derive the master document container id from its object key."
  [spec]
  (oc/document-id-for-object-key (master-id spec)))

(defn active-pointer-container-id
  "Derive the separate block container whose revision content selects worn material."
  [spec]
  (oc/block-container-id (master-id spec) "active-pointer"))

(defn- master-slug
  "Remove an fm: prefix for bootstrap request naming; otherwise keep the id."
  [spec]
  (let [id (master-id spec)]
    (if (str/starts-with? id "fm:")
      (subs id 3)
      id)))

(defn import-key
  "Combine the master id and source hash into the content-keyed import identity."
  [spec source-hash]
  (str "imp:fm:" (master-id spec) ":" source-hash))

(def import-capabilities
  #{:object-container/import-material
    :source/ingest
    :facet-master/activate})

(def activation-capabilities
  #{:object/edit})

(def oc-actor-types
  "Document the actor types accepted by the object-container request validator.
   This value is not consulted by normalize-actor and does not authorize a caller."
  #{:human :agent :system :bot})

(defn normalize-actor
  "Fill missing actor id/type and union the requested capabilities into the actor.
   This constructs a request actor; it does not authenticate or authorize the
   caller, and an unsupported supplied actor type is not changed."
  [actor capabilities]
  (let [a (or actor {:actor/id "system" :actor/type :system})]
    {:actor/id (str (or (:actor/id a) "system"))
     :actor/type (or (:actor/type a) :system)
     :actor/capabilities (into (set (:actor/capabilities a)) capabilities)}))

(defn materialization
  "Build source, version, container, anchor and immutable-revision rows for a
   candidate. Stringifies source and content-hashes it; no storage write occurs.
   Missing :request/id and :time-ms sample a random id and the clock, so this is
   deterministic only when those options are supplied. Optional active-pointer
   rows bootstrap either bare revision-id content or caller-supplied event bytes."
  [spec source opts]
  (let [master-id (master-id spec)
        source-ref (source-ref spec)
        document-id (document-id spec)
        pointer-container-id (active-pointer-container-id spec)
        source (str source)
        source-hash (oc/source-hash source)
        source-ref-key (oc/source-ref-key source-ref)
        import-key (import-key spec source-hash)
        source-id (oc/source-id-for-object-key master-id)
        request-id (or (:request/id opts) (envelope/random-id "req"))
        created-at (long (or (:time-ms opts) (envelope/now-ms)))
        created-by (or (get-in opts [:actor :actor/id]) "system")
        event-id (str "evt:" master-id ":" request-id)
        order-key (oc/fixed-width-order-key created-at request-id)
        revision-id (oc/import-revision-id master-id document-id source-hash)
        source-row (oc/->SourceArtifactRow
                    source-id source-ref source-hash :edn source document-id
                    (long (count (.getBytes source "UTF-8")))
                    created-at created-by event-id)
        version-row (oc/->SourceVersionRow
                     source-ref-key source-ref source-hash source-id document-id
                     master-id order-key created-at event-id)
        revision-row (oc/->RevisionRow
                      revision-id document-id nil source source-hash order-key
                      created-at created-by event-id)
        anchor-id (oc/source-anchor-id document-id)
        container-row (oc/->ObjectContainerRow
                       document-id :facet-master master-id :private
                       source-id anchor-id nil document-id revision-id nil nil
                       created-at created-by event-id)
        anchor-row (oc/->SourceAnchorRow
                    anchor-id :object-container document-id source-id source-ref
                    source-hash 0 (count source) nil event-id)
        pointer-revision-id
        (when (:include-active-pointer? opts)
          (oc/import-revision-id
           master-id
           pointer-container-id
           (str "active:" revision-id)))
        pointer-row
        (when pointer-revision-id
          (oc/->ObjectContainerRow
           pointer-container-id :text-block master-id :private
           source-id (oc/source-anchor-id pointer-container-id) nil
           document-id pointer-revision-id nil nil
           created-at created-by event-id))
        ;; P6 · R4: a bootstrap pointer may carry a full activation event
        ;; instead of the bare revision-id. Absent `:pointer-source` the bytes
        ;; are EXACTLY what P1 shipped — every already-durable bootstrap
        ;; pointer on the cluster stays byte-identical, and those v0 rows are
        ;; the live fixture G8 reads as `unknown grounds`.
        pointer-content (or (:pointer-source opts) revision-id)
        pointer-revision-row
        (when pointer-revision-id
          (oc/->RevisionRow
           pointer-revision-id pointer-container-id nil pointer-content
           (oc/source-hash pointer-content)
           (oc/fixed-width-order-key created-at (str request-id ":active"))
           created-at created-by event-id))
        pointer-anchor-row
        (when pointer-revision-id
          (oc/->SourceAnchorRow
           (oc/source-anchor-id pointer-container-id)
           :object-container pointer-container-id source-id source-ref
           source-hash 0 (count source) nil event-id))]
    {:source source
     :source-hash source-hash
     :source-ref-key source-ref-key
     :source-id source-id
     :import-key import-key
     :request-id request-id
     :created-at created-at
     :revision-id revision-id
     :source-row source-row
     :version-row version-row
     :revision-row revision-row
     :container-row container-row
     :anchor-row anchor-row
     :pointer-row pointer-row
     :pointer-revision-row pointer-revision-row
     :pointer-anchor-row pointer-anchor-row}))

(defn candidate-import-request
  "Build an object-container/import-material ActionRequest without appending it.
   Carries candidate rows and optional bootstrap pointer rows from materialization;
   import identity is master plus source hash. Does not compile the candidate,
   so invalid EDN may be retained for inspection."
  ([spec source] (candidate-import-request spec source {}))
  ([spec source opts]
   (let [master-id (master-id spec)
         source-ref (source-ref spec)
         m (materialization spec source opts)
         payload {:object-key master-id
                  :source-ref source-ref
                  :source-hash (:source-hash m)
                  :source-raw-text (:source m)
                  :source-format :edn
                  :source-artifacts [(:source-row m)]
                  :object-containers (cond-> [(:container-row m)]
                                       (:pointer-row m) (conj (:pointer-row m)))
                  :revisions (cond-> [(:revision-row m)]
                               (:pointer-revision-row m)
                               (conj (:pointer-revision-row m)))
                  :derived-units []
                  :source-anchors (cond-> [(:anchor-row m)]
                                    (:pointer-anchor-row m)
                                    (conj (:pointer-anchor-row m)))
                  :composition-edges []
                  :source-versions [(:version-row m)]}
         fingerprint (oc/import-material-fingerprint
                      master-id (:import-key m) payload)
         actor (normalize-actor (:actor opts) import-capabilities)]
     (assoc
      (envelope/action-request
       {:request-id (:request-id m)
        :request-type :object-container/import-material
        :time-ms (:created-at m)
        :actor actor
        :target {:target/kind :object-container-import
                 :target/id (:import-key m)
                 :target/address {:object/key master-id}}
        :action {:action/type :object-container/import-material
                 :action/capability :object-container/import-material
                 :action/params {:source/family :facet-master
                                 :source/format :edn}}
        :routing/key [:object-container/import master-id]
        :payload payload
        :provenance {:source/type :facet-master
                     :source/ref source-ref}})
      :partition/key master-id
      :object/key master-id
      :import/key (:import-key m)
      :source/family :facet-master
      :source/format :edn
      :idempotency/key (:import-key m)
      :material/fingerprint fingerprint
      :facet-master/revision-id (:revision-id m)))))

(defn- append-and-read!
  "Read any prior audit decision, append with :ack, then read the decision again.
   :replay? means a decision existed before this call, including a rejection;
   it is not an acceptance test or the kernel's idempotency-replay classification.
   Foreign-client failures propagate; these calls are not one transaction."
  [runtime request]
  (let [already-decided? (some? (ocr/read-decision runtime request))]
    (ocr/append-object-container-request! runtime request :ack)
    {:decision (ocr/read-decision runtime request)
     :replay? already-decided?}))

(defn import-candidate!
  "Append candidate rows and return request, durable decision, acceptance,
   pre-observed replay flag and revision id. Unvalidated source is allowed;
   :revision-id alone is not proof that the import was accepted. Increment the
   borrowed ingest epoch only for acceptance not observed before this append.
   Ordinary imports leave the active pointer alone; bootstrap opts include it."
  ([runtime spec source] (import-candidate! runtime spec source {}))
  ([runtime spec source opts]
   (let [request (candidate-import-request spec source opts)
         {:keys [decision replay?]} (append-and-read! runtime request)
         accepted? (oc/decision-accepted? decision)]
     (when (and accepted? (not replay?))
       (swap! ingest-epoch/!ingest-epoch-atom inc))
     {:request request
      :decision decision
      :accepted? accepted?
      :replay? replay?
      :revision-id (:facet-master/revision-id request)})))

(defn read-master
  "Read latest candidate, current pointer and the named active material revision
   independently. Pointer content may be a declared event or a legacy bare id;
   parse retains errors and unknown grounds. Returns missing rows as nil. These
   separate foreign reads are not a coherent snapshot during concurrent writes."
  [runtime spec]
  (let [latest (ocr/read-current-revision runtime (document-id spec))
        pointer (ocr/read-current-revision
                 runtime (active-pointer-container-id spec))
        event (when pointer
                (activation-event/parse (:content-text pointer)))
        active-id (:activation/revision-id event)
        active (when (string? active-id)
                 (ocr/read-revision runtime active-id))]
    {:facet-master-id (master-id spec)
     :latest-revision latest
     :active-pointer pointer
     :active-event event
     :active-revision active}))

(defn activate!
  "Read and compile a candidate, then request an edit of the master's active
   pointer. A missing revision, wrong container or invalid form returns rejection
   data before append. Otherwise serialize a declared event and append with :ack;
   :accepted? comes from the decision read back, not from depot acknowledgement.

   opts supply actor, time, request/idempotency identity, kind, scope and grounds.
   The scope defaults to this instance's subject or all-unpinned for a shared
   master. This function does not validate the event's declared kind/scope/grounds
   or enforce their match to the pointer owner; callers must provide valid ones.
   No wearer fan-out occurs. Foreign failures propagate; epoch notification
   follows acceptance not already observed by request id."
  ([runtime spec revision-id] (activate! runtime spec revision-id {}))
  ([runtime spec revision-id opts]
   (let [master-id (master-id spec)
         document-id (document-id spec)
         pointer-container-id (active-pointer-container-id spec)
         candidate (ocr/read-revision runtime revision-id)
         compiled (when candidate
                    (facet-engine/compile-source
                     spec (:content-text candidate)))
         errors (cond-> []
                  (nil? candidate)
                  (conj {:type :revision/not-found
                         :revision-id revision-id})

                  (and candidate (not= document-id (:container-id candidate)))
                  (conj {:type :revision/facet-master-mismatch
                         :revision-id revision-id
                         :actual-container-id (:container-id candidate)})

                  (and candidate (false? (:valid? compiled)))
                  (into (:errors compiled)))]
     (if (seq errors)
       {:request nil
        :decision nil
        :accepted? false
        :replay? false
        :reason :facet-master/activation-rejected
        :errors errors
        :revision-id revision-id}
       (let [request-id (or (:request/id opts)
                            (:request-id opts)
                            (envelope/random-id "req"))
             ;; R3 — the HONEST wall clock of the requesting act. A caller may
             ;; pass `:time-ms` (the client's `Date.now()` through the write
             ;; path, or a test's explicit constant); absent that, the request
             ;; is being created HERE and now is the truth. Deterministic
             ;; constants in a shipped write path are what made P1/P3/P5's
             ;; activation history non-monotone; G11 keeps a fourth from
             ;; joining them.
             time-ms (long (or (:time-ms opts) (envelope/now-ms)))
             ;; R4 — the pointer's source is a CLOSED activation event. The
             ;; default scope follows the master: an instance master can only
             ;; ever move its own subject, a shared master reaches every
             ;; unpinned wearer BY REFERENCE (T1 — never a fan-out write).
             event
             (activation-event/event
              {:revision-id revision-id
               :kind (or (:activation/kind opts) :activate)
               :scope (or (:activation/scope opts)
                          (if-let [subject (:facet-master/subject spec)]
                            (activation-event/subject-scope subject)
                            (activation-event/all-unpinned-scope)))
               :actor (:actor opts)
               :time-ms time-ms
               :grounds (:activation/grounds opts)})
             request
             (oc/object-edit-request
              :object-container
              pointer-container-id
              (activation-event/source-for event)
              {:object/key master-id
               :document/container-id document-id
               :request/id request-id
               :time-ms time-ms
               :revision/id
               (str "rev:" master-id ":active:"
                    (envelope/sha-256 request-id))
               :edit/client-id (str "facet-master-activation:" request-id)
               :edit/seq 0
               :edit/lineage-key pointer-container-id
               :idempotency/key
               (or (:idempotency/key opts)
                   (:idempotency-key opts)
                   (str "facet-master/activate:" master-id ":"
                        revision-id ":" request-id))
               :actor (normalize-actor (:actor opts) activation-capabilities)
               :provenance
               (or (:provenance opts)
                   {:source/type :facet-master-activation
                    :source/ref master-id})})
             {:keys [decision replay?]} (append-and-read! runtime request)
             accepted? (oc/decision-accepted? decision)]
         (when (and accepted? (not replay?))
           (swap! ingest-epoch/!ingest-epoch-atom inc))
         {:request request
          :decision decision
          :accepted? accepted?
          :replay? replay?
          :event event
          :revision-id revision-id})))))

(defn ensure-master!
  "Install the spec's default source and a bootstrap pointer when the latest
   revision is absent; repair a missing pointer by activating the latest revision.
   Uses deterministic bootstrap ids and time 0, then returns import/activation
   results and a reread state. This is default-form, not necessarily grammar v0
   (space defaults to v1). Existing candidate and pointer state are not migrated."
  [runtime spec]
  (let [slug (master-slug spec)
        before (read-master runtime spec)
        imported (when (nil? (:latest-revision before))
                   (import-candidate!
                    runtime spec
                    (facet-engine/source-for
                     (:facet-master/default-form spec))
                    {:request/id (str "facet-master-" slug "-v0")
                     :time-ms 0
                     :include-active-pointer? true}))
        state (read-master runtime spec)
        revision-id (or (some-> state :latest-revision :revision-id)
                        (:revision-id imported))
        activated (when (and revision-id (nil? (:active-pointer state)))
                    (activate!
                     runtime spec revision-id
                     {:request/id
                      (str "facet-master-" slug "-activate-v0")
                      :time-ms 0
                      :idempotency/key
                      (str "facet-master/activate:" (master-id spec)
                           ":" revision-id ":bootstrap")}))]
    {:import imported
     :activation activated
     :state (read-master runtime spec)}))

(defn ensure-active-source!
  "Import exact source bytes and, if their revision differs from the observed
   active revision, request activation with caller-supplied request ids and time.
   Returns both write results plus a reread state; import and activation are
   separate operations. Each revision still compiles under its declared grammar."
  [runtime spec source {:keys [request-id activation-request-id time-ms]}]
  (let [imported
        (import-candidate!
         runtime spec source
         {:request/id request-id
          :time-ms time-ms})
        revision-id (:revision-id imported)
        state (read-master runtime spec)
        activation
        (when (not= revision-id
                    (some-> state :active-revision :revision-id))
          (activate!
           runtime spec revision-id
           {:request/id activation-request-id
            :time-ms time-ms
            :idempotency/key
            (str "facet-master/activate:" (master-id spec)
                 ":" revision-id ":" activation-request-id)}))]
    {:import imported
     :activation activation
     :state (read-master runtime spec)}))

(defn malformed-drill!
  "Retain a malformed candidate for any known master and close before pointer
   edit. Candidate and active truth remain independently queryable."
  [runtime spec drill-id]
  (let [drill-id (str drill-id)
        master-id (master-id spec)
        slug (master-slug spec)
        source (str "{:facet-master/id " (pr-str master-id)
                    " :drill/id " (pr-str drill-id))
        hash (oc/source-hash source)
        imported (import-candidate!
                  runtime spec source
                  {:request/id
                   (str "facet-master-" slug "-drill-import-"
                        (subs hash 0 16))})
        activation (activate!
                    runtime spec (:revision-id imported)
                    {:request/id
                     (str "facet-master-" slug "-drill-activate-"
                          (envelope/sha-256 drill-id))
                     :idempotency/key
                     (str "facet-master/activate:" master-id ":"
                          (:revision-id imported) ":drill:" drill-id)})]
    {:drill-id drill-id
     :master-id master-id
     :candidate-revision-id (:revision-id imported)
     :import-decision (:decision imported)
     :activation-decision (:decision activation)
     :activation-errors (:errors activation)
     :state (read-master runtime spec)}))

;; ===========================================================================
;; editable-material P6 · R2 — INSTANCE-SCOPED masters, through THIS adapter
;;
;; No new module, PState, depot, topology or routing branch (R1). An instance
;; master is another facet-master: same import, same immutable revisions, same
;; explicit revisioned active-pointer, same candidate-vs-active, same drill
;; totality. Its spec is SYNTHESIZED from its parent's rather than authored, so
;; a facet gains an instance tier by existing, not by opting in.
;;
;; The id keeps to TWO colon segments (`facet-engine/instance-marker`) — see
;; the routing note in `app.server.worn.facet-engine`. That is what makes every
;; derived id (document, pointer, revisions, source, import-completion) hash to
;; ONE Rama partition with zero kernel edits.
;; ===========================================================================

(defn subject-digest
  "Return the first eight SHA-256 hex characters of the stringified subject uid.
   The full uid is also stored in instance material; this shortened identifier
   has no collision detection in this adapter."
  [subject-uid]
  (subs (envelope/sha-256 (str subject-uid)) 0 8))

(defn instance-master-id
  "Derive a parent-qualified instance id using the subject's shortened digest."
  [parent-spec subject-uid]
  (facet-engine/instance-master-id
   (master-id parent-spec) (subject-digest subject-uid)))

(defn instance-spec
  "Synthesize the instance spec for this parent and subject; no storage access."
  [parent-spec subject-uid]
  (facet-engine/instance-spec
   parent-spec (instance-master-id parent-spec subject-uid) subject-uid))

(defn candidate-revision-id
  "The revision-id an import of exactly these bytes WILL mint. Content-hash
   keyed, so it is knowable before the append — which is what lets a bootstrap
   pointer name the revision it is about to activate."
  [spec source]
  (oc/import-revision-id
   (master-id spec) (document-id spec) (oc/source-hash (str source))))

(defn compiled-active
  "Read the active revision and attach its id to a compiler result, or return nil
   when no active revision resolves. Invalid source yields diagnostics; foreign
   read failures still propagate."
  [runtime spec]
  (let [{:keys [active-revision]} (read-master runtime spec)]
    (when active-revision
      (assoc (facet-engine/compile-source
              spec (:content-text active-revision))
             :revision-id (:revision-id active-revision)))))

(defn inherited-material
  "Read shared active material as the base for a new instance revision.
   Missing or invalid shared material uses the code floor. Returns :grammar and
   :material only; it does not merge an existing instance's deviating values."
  [runtime parent-spec]
  (let [compiled (compiled-active runtime parent-spec)]
    (if (:valid? compiled)
      {:grammar (:grammar compiled) :material (:material compiled)}
      (let [floor (facet-engine/code-floor parent-spec)]
        {:grammar (:facet-master/grammar floor)
         :material (dissoc floor
                           :facet-master/id :facet-master/facet
                           :facet-master/grammar :facet-master/revision-id
                           :facet-master/floor?)}))))

(defn instance-state
  "One subject's instance master as the write paths need it: does it exist, is
   it valid, and what does it currently hold?"
  [runtime parent-spec subject-uid]
  (let [ispec (instance-spec parent-spec subject-uid)
        state (read-master runtime ispec)
        compiled (when (:active-revision state)
                   (facet-engine/compile-source
                    ispec (:content-text (:active-revision state))))]
    {:spec ispec
     :instance-master-id (master-id ispec)
     :exists? (some? (:latest-revision state))
     :pointer? (some? (:active-pointer state))
     :state state
     :compiled compiled
     :material (when (:valid? compiled) (:material compiled))
     :holds (when (:valid? compiled)
              (facet-engine/instance-holds (:material compiled)))
     :pin (when (:valid? compiled)
            (:facet-master/pin (:material compiled)))
     :deviates? (when (:valid? compiled)
                  (true? (:facet-master/deviates? (:material compiled))))}))

(defn write-instance-revision!
  "Mint ONE instance revision and activate it as a recorded event.

   The first revision of a (facet, subject) rides the SAME bootstrap shape
   `ensure-master!` uses — one import that carries its own active pointer —
   with `:pointer-source` set so even the very first deviation is a declared
   activation event and not a bare string. Every later revision is an import
   plus a pointer activation, exactly like a shared master's.

   `overrides` names the deviating keys; everything else is the inherited
   snapshot, which is what makes `deviation-diff` computable without storing
   anything. Refuses before append when sites are illegal or the resulting form
   does not compile. Each call starts from current inherited material, not the
   existing instance snapshot; callers must supply any overrides they intend to retain.

   The returned :accepted? currently treats a pre-observed decision (:replay?)
   as success as well as an accepted decision. A replay flag may also describe a
   prior rejection; inspect the nested import/activation decisions when auditing
   the outcome. Separate import and activation calls are not one transaction."
  [runtime parent-spec subject-uid
   {:keys [kind overrides deviates? pin actor time-ms grounds
           request-id activation-request-id]
    :or {kind :activate}}]
  (let [{:keys [spec exists? pointer? state]} (instance-state
                                               runtime parent-spec subject-uid)
        iid (master-id spec)
        {:keys [grammar material]} (inherited-material runtime parent-spec)
        form (facet-engine/instance-form
              parent-spec iid subject-uid
              {:grammar grammar
               :material material
               :overrides overrides
               :deviates? deviates?
               :pin pin})
        ;; G10 / T-R3, the DURABLE half: owner-aware legality prevents a
        ;; non-space facet from minting dead `:space/ground` rows.
        ;; T-R4 — do NOT call the camera fence here: fm:space's inherited
        ;; bindings validator refuses camera rows at compile-form below.
        illegal-sites
        (vec
         (remove #(binding-material/instance-site-legal?
                   % (:facet-master/facet parent-spec))
                 (keys (:facet-master/bindings form))))
        compiled (facet-engine/compile-form spec form)
        time-ms (long (or time-ms (envelope/now-ms)))]
    (cond
      (seq illegal-sites)
      {:accepted? false
       :reason :facet-master/instance-site-refused
       :sites illegal-sites
       :legal-sites (vec (sort-by str binding-material/instance-legal-sites))
       :instance-master-id iid
       :subject subject-uid}

      (not (:valid? compiled))
      {:accepted? false
       :reason :facet-master/instance-form-invalid
       :errors (:errors compiled)
       :instance-master-id iid
       :subject subject-uid}

      :else
      (let [source (facet-engine/source-for form)
            revision-id (candidate-revision-id spec source)
            req-id (or request-id
                       (str "fm-instance-" (subject-digest subject-uid) "-"
                            (subs (oc/source-hash source) 0 16)))
            ;; The activation id is keyed to the TRANSITION — content PLUS the
            ;; pointer revision it moves from — never to content alone. A
            ;; byte-identical form re-activated later (pin → unpin → pin → the
            ;; second unpin) would otherwise reuse the first activation's
            ;; request-id and be replayed by the idempotency journal: the
            ;; pointer never moves while the caller is told accepted (gate P6
            ;; finding F1; the machine-cut A-F2/F3 stable-per-transition
            ;; class). A true retry — same content, same pre-state — still
            ;; replays, which is what idempotency is for.
            pointer-tip (get-in state [:active-pointer :revision-id] "genesis")
            act-id (or activation-request-id
                       (str req-id ":activate:"
                            (subs (envelope/sha-256 (str pointer-tip)) 0 8)))
            event (activation-event/event
                   {:revision-id revision-id
                    :kind kind
                    :scope (activation-event/subject-scope subject-uid)
                    :actor actor
                    :time-ms time-ms
                    :grounds grounds})
            bootstrap? (not (and exists? pointer?))
            imported (import-candidate!
                      runtime spec source
                      (cond-> {:request/id req-id
                               :time-ms time-ms
                               :actor actor}
                        bootstrap?
                        (assoc :include-active-pointer? true
                               :pointer-source
                               (activation-event/source-for event))))
            activated (when-not bootstrap?
                        (activate!
                         runtime spec revision-id
                         {:request/id act-id
                          :time-ms time-ms
                          :actor actor
                          :activation/kind kind
                          :activation/scope
                          (activation-event/subject-scope subject-uid)
                          :activation/grounds grounds
                          :idempotency/key
                          (str "facet-master/activate:" iid ":"
                               revision-id ":" act-id)}))]
        ;; Non-bootstrap: the IMPORT being a replay is normal (content-keyed by
        ;; design); what must have landed is the ACTIVATION. Reading the import
        ;; alone is how finding F1 reported success over a pointer that never
        ;; moved.
        {:accepted? (boolean
                     (and (or (:accepted? imported) (:replay? imported))
                          (or bootstrap?
                              (:accepted? activated)
                              (:replay? activated))))
         :bootstrap? bootstrap?
         :instance-master-id iid
         :subject subject-uid
         :parent-id (master-id parent-spec)
         :revision-id revision-id
         :kind kind
         :event event
         :import imported
         :activation activated
         :state (read-master runtime spec)}))))

(defn deviate!
  "The FIRST durable deviation of (facet, subject) mints the instance master;
   later ones are ordinary revisions of it. Any existing pin is preserved —
   deviating and pinning are independent axes, each with its own event kind."
  [runtime parent-spec subject-uid overrides opts]
  (let [{:keys [pin]} (instance-state runtime parent-spec subject-uid)]
    (write-instance-revision!
     runtime parent-spec subject-uid
     (merge opts {:kind :activate :overrides overrides
                  :deviates? true :pin pin}))))

(defn release-deviation!
  "T11 — removing a deviation is the ACTIVATION OF THE INHERITED STATE, kind
   `:rollback`. Never a tombstone and never a deletion: a tombstone cell
   DELETES THE UNIT from the page (Task 18 semantics, proven in the P5 gate's
   cell forensics), which is a catastrophically different act from `this block
   goes back to wearing what everyone else wears`."
  [runtime parent-spec subject-uid opts]
  (let [{:keys [pin]} (instance-state runtime parent-spec subject-uid)]
    (write-instance-revision!
     runtime parent-spec subject-uid
     (merge opts {:kind :rollback :deviates? false :pin pin}))))

(defn pin!
  "Write a pin to a named shared revision and preserve the current deviates? flag.
   The target id is recorded without checking its existence; served resolution
   compiles it later. Current implementation rebuilds other material from the
   shared inherited snapshot unless opts supplies overrides. Preserving the flag
   alone does not preserve previous override values. See write-instance-revision!."
  [runtime parent-spec subject-uid pinned-revision-id opts]
  (let [{:keys [deviates?]} (instance-state runtime parent-spec subject-uid)]
    (write-instance-revision!
     runtime parent-spec subject-uid
     (merge opts {:kind :pin
                  :deviates? (boolean deviates?)
                  :pin {:pinned-revision-id (str pinned-revision-id)}}))))

(defn unpin!
  "Write a nil pin while preserving the current deviates? flag. Without deviation,
   subsequent wear resolution follows current shared material immediately.
   Current implementation rebuilds material from the shared snapshot unless opts
   supplies overrides; retaining the flag alone does not carry old values forward."
  [runtime parent-spec subject-uid opts]
  (let [{:keys [deviates?]} (instance-state runtime parent-spec subject-uid)]
    (write-instance-revision!
     runtime parent-spec subject-uid
     (merge opts {:kind :unpin
                  :deviates? (boolean deviates?)
                  :pin nil}))))

;; ===========================================================================
;; The activation trail — CAUSAL, never clock-ordered (R3 · T2)
;; ===========================================================================

(def ^:private trail-page-limit 1000)

(defn activation-trail
  "Read up to 1000 pointer history rows from the beginning of the order-key index,
   then follow parent-revision-id from the separately read current tip, newest
   first. Causal traversal is not timestamp sorting; clock regressions are
   reported over the reversed chain. Stop at an absent row or a repeated id.

   This does not paginate or fetch missing parents. :complete? only says fewer
   than 1000 rows were returned, not that the chain reached its root. A tip
   outside that first page can yield an empty chain despite existing history."
  [runtime spec]
  (let [pointer-id (active-pointer-container-id spec)
        current (ocr/read-current-revision runtime pointer-id)
        rows (ocr/read-revision-history runtime pointer-id "" trail-page-limit)
        by-id (into {} (map (juxt :revision-id identity)) rows)
        chain (loop [id (:revision-id current) seen #{} acc []]
                (let [row (when (and id (not (contains? seen id)))
                            (get by-id id))]
                  (if (nil? row)
                    acc
                    (recur (:parent-revision-id row)
                           (conj seen id)
                           (conj acc
                                 {:pointer-revision-id (:revision-id row)
                                  :parent-revision-id (:parent-revision-id row)
                                  :created-at-ms (:created-at-ms row)
                                  :created-by (:created-by row)
                                  :event (activation-event/parse
                                          (:content-text row))})))))]
    {:facet-master-id (master-id spec)
     :complete? (< (count rows) trail-page-limit)
     ;; newest first for reading; the regression check needs oldest-first
     :chain chain
     :regressions (activation-event/trail-regressions
                   (mapv :event (reverse chain)))}))

(defn worn-at
  "Find a pointer revision in activation-trail's bounded causal chain and return
   the material revision it names, its event and newer entries under :since.
   A missing cut returns :found? false; it may be outside the fetched page.
   This is one master's pointer cut, not a timestamp or a global snapshot."
  [runtime spec pointer-revision-id]
  (let [{:keys [chain]} (activation-trail runtime spec)
        idx (first (keep-indexed
                    (fn [i e]
                      (when (= pointer-revision-id (:pointer-revision-id e)) i))
                    chain))]
    (if (nil? idx)
      {:found? false :cut pointer-revision-id}
      (let [entry (nth chain idx)]
        {:found? true
         :cut pointer-revision-id
         :worn-revision-id (get-in entry [:event :activation/revision-id])
         :event (:event entry)
         ;; everything causally AFTER the cut, newest first
         :since (vec (take idx chain))}))))
