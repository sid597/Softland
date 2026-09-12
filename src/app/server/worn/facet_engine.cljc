(ns app.server.worn.facet-engine
  "Pure, shared compiler and resolver for versioned facet material.
   Specs supply exact material keys, validators, default/floor forms and grammar
   versions. Source/forms compile to validity, errors, grammar and material;
   served shared/instance values resolve to complete wear maps. Contributions
   carry subject/facet/revision stamps and deterministic composition diagnostics.

   Owns no state, I/O or resource lifetime. It does not evaluate authored EDN,
   fetch revisions, activate pointers or render. facet-master supplies durable
   sources; material-truth and page projections supply served inputs. Compiled
   validators are trusted code and code-floor forms must themselves be valid."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(def common-form-keys
  #{:facet-master/id
    :facet-master/grammar
    :facet-master/facet})

(defn finite-number?
  "True for finite numeric values on the current host."
  [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn valid-rgba?
  "True for a four-element vector of finite channels in the inclusive range 0..1."
  [x]
  (and (vector? x)
       (= 4 (count x))
       (every? #(and (finite-number? %) (<= 0 % 1)) x)))

(defn non-negative-number?
  "True for a finite numeric value greater than or equal to zero."
  [x]
  (and (finite-number? x) (<= 0 x)))

(defn integer-number?
  "Apply the host integer? predicate used by grammar declarations."
  [x]
  (integer? x))

(defn- grammar-declaration
  "Look up the form's explicit grammar version in the supplied spec."
  [spec form]
  (get-in spec [:facet-master/grammars (:facet-master/grammar form)]))

(defn- form-validation-errors
  "RULING R1 · T9 — validate invariants spanning material keys.

   Predicates receive only the material-keys projection. The seam is optional:
   a declaration without `:form-validators` produces no errors, preserving every
   frozen grammar's prior bytes and meaning."
  [declaration material]
  (->> (:form-validators declaration)
       (keep (fn [{:keys [valid? error-type]}]
               (when-not (valid? material)
                 {:type error-type
                  :actual material})))))

(defn- valid-form-material?
  "The wear-time half of R1/T9. Keep this beside the compile-time reader so a
   cross-field invariant cannot accidentally reach candidates but not wears."
  [declaration material]
  (every? (fn [{:keys [valid?]}] (valid? material))
          (:form-validators declaration)))

(defn compile-form
  "Validate one facet form against its explicit grammar and return error data.
   Spec predicates must handle their inputs; exceptions are caught by
   compile-source, not by this form-level function.

   `:facet-master/grammars` maps a version to:
   - `:material-keys` — the exact keys returned to the renderer;
   - `:validators` — key -> {:valid? fn :error-type keyword};
   - optional `:form-validators` — [{:valid? fn :error-type keyword}], whose
     predicates receive the material-keys projection.

   Old grammar declarations remain explicit entries. Adding a grammar never
   reinterprets an already-durable form."
  [spec form]
  (let [master-id (:facet-master/id spec)
        facet (:facet-master/facet spec)
        declaration (when (map? form) (grammar-declaration spec form))
        material-keys (set (:material-keys declaration))
        material (when (map? form) (select-keys form material-keys))
        allowed-keys (into common-form-keys material-keys)
        unknown (when (and (map? form) declaration)
                  (seq
                   (sort-by pr-str
                            (remove allowed-keys (keys form)))))
        validation-errors
        (when declaration
          (->> (:validators declaration)
               (sort-by (comp pr-str key))
               (keep (fn [[k {:keys [valid? error-type]}]]
                       (when-not (valid? (get form k))
                         {:type error-type
                          :actual (get form k)})))))
        ;; T9 — independent key bounds cannot prove a relation such as min<max.
        ;; The same declaration is read again by `valid-material?` below.
        form-errors (when declaration
                      (form-validation-errors declaration material))
        errors (cond-> []
                 (not (map? form))
                 (conj {:type :facet-master/not-a-map})

                 (and (map? form)
                      (not= master-id (:facet-master/id form)))
                 (conj {:type :facet-master/id-invalid
                        :expected master-id
                        :actual (:facet-master/id form)})

                 (and (map? form)
                      (not (contains? (:facet-master/grammars spec)
                                      (:facet-master/grammar form))))
                 (conj {:type :facet-master/grammar-invalid
                        :expected
                        (vec (sort (keys (:facet-master/grammars spec))))
                        :actual (:facet-master/grammar form)})

                 (and (map? form)
                      (not= facet (:facet-master/facet form)))
                 (conj {:type :facet-master/facet-invalid
                        :expected facet
                        :actual (:facet-master/facet form)})

                 (seq validation-errors)
                 (into validation-errors)

                 (seq form-errors)
                 (into form-errors)

                 unknown
                 (conj {:type :facet-master/unknown-keys
                        :keys (vec unknown)}))]
    (if (seq errors)
      {:valid? false :errors errors :grammar nil :material nil}
      {:valid? true
       :errors []
       :grammar (:facet-master/grammar form)
       :material material})))

(defn compile-source
  "Read the source as EDN and compile it; caught reader or validator exceptions
   become :facet-master/parse-error data. Does not evaluate the source as code."
  [spec source]
  (try
    (compile-form spec (edn/read-string (str source)))
    (catch #?(:clj Throwable :cljs :default) t
      {:valid? false
       :errors [{:type :facet-master/parse-error
                 :message #?(:clj (.getMessage t)
                             :cljs (.-message t))}]
       :grammar nil
       :material nil})))

(defn source-for
  "Print a form as EDN using the caller's printer bindings; does not validate it."
  [form]
  (pr-str form))

(defn code-floor
  "Compile the spec's floor form and add its stable fallback identity.
   Requires a valid trusted floor declaration; this helper does not check or
   report compilation errors before constructing the returned wear map."
  [spec]
  (let [compiled (compile-form spec (:facet-master/floor-form spec))]
    (merge (:material compiled)
           {:facet-master/id (:facet-master/id spec)
            :facet-master/facet (:facet-master/facet spec)
            :facet-master/grammar (:grammar compiled)
            :facet-master/revision-id
            (:facet-master/code-floor-revision-id spec)
            :facet-master/floor? true})))

(defn valid-material?
  "Check exact material keys plus field and whole-form predicates for one grammar.
   An unknown grammar returns nil; predicates are trusted to handle their inputs."
  [spec grammar material]
  (when-let [declaration
             (get-in spec [:facet-master/grammars grammar])]
    (let [material-keys (set (:material-keys declaration))]
      (and (map? material)
           (= material-keys (set (keys material)))
           (every?
            (fn [[k {:keys [valid?]}]]
              (valid? (get material k)))
            (:validators declaration))
           ;; T9 — compile-only validation would let a durable malformed
           ;; cross-field material wear. This is the resolved-wear guard.
           (valid-form-material?
            declaration (select-keys material material-keys))))))

(defn resolved-wear
  "Resolve only complete served active material. Anything absent, malformed,
   or from the wrong master returns that facet's total code floor."
  [spec served]
  (let [master-id (:facet-master/id spec)
        revision-id (:facet-master/active-revision-id served)
        grammar (:facet-master/grammar served)
        material (:facet-master/material served)]
    (if (and (= master-id (:facet-master/id served))
             (string? revision-id)
             (valid-material? spec grammar material))
      (merge material
             {:facet-master/id master-id
              :facet-master/facet (:facet-master/facet spec)
              :facet-master/grammar grammar
              :facet-master/revision-id revision-id
              :facet-master/floor? false})
      (code-floor spec))))

(defn contribution-stamp
  "Causal render stamp at the grain currently known. The attachment is
   explicitly derived from the subject+facet until a durable attachment owner
   exists; view-instance never enters the identity."
  [wear subject site role slot]
  {:material/subject subject
   :material/attachment
   [:derived (:facet-master/facet wear) subject]
   :material/master (:facet-master/id wear)
   :material/revision (:facet-master/revision-id wear)
   :material/site site
   :material/role role
   :material/slot slot})

;; ===========================================================================
;; editable-material P6 · R2 — the INSTANCE tier as instance-scoped masters
;;
;; P5 made the instance tier legal with no durable owner. P6 gives it one, and
;; the owner is THIS machinery: at the first durable deviation of (facet,
;; subject) we mint another facet-master through the SAME adapter, its spec
;; synthesized from the parent's. Everything is inherited rather than rebuilt —
;; immutable revisions, candidate-vs-active, preview, activation events,
;; rollback, drill totality, latest ≠ active. That is the second-wearer law
;; applied to our own machinery: additive reuse, no parallel serve artery, no
;; compat adapter, and no new Rama module (R1).
;;
;; ID SHAPE — required by object-container/extract-object-key.
;; `object-container/extract-object-key` collapses an `fm:`-prefixed key to its
;; first TWO colon segments in every branch (`oc:block:`, `rev:`, `src:`,
;; `imp:fm:`) EXCEPT `oc:doc:`, which returns the whole remainder. A shared id
;; like `fm:attention` is invariant under both rules, which is why P1–P5 never
;; met this edge. An id with extra colon segments (`fm:attention:i:<sha8>`)
;; is not:
;; its document container would hash to one Rama partition while its own
;; active-pointer, revisions, source and import-completion hash to another —
;; the foreign-read mis-route class that has now fired four times in this
;; repo (imp:clj: G-F2, imp:sense-block: F2, imp:asm: G18, imp:ep: first-light).
;; Keeping the instance id to TWO colon segments makes the whole family route
;; consistently with ZERO kernel edits, which is what lets R1 hold. The marker
;; is therefore `~i~` inside the second segment, not `:i:` after it.
;; ===========================================================================

(def instance-marker
  "Separates a parent facet-master id from its subject digest WITHOUT adding a
   colon segment. See the routing note above — this is a correctness
   constraint, not a style choice."
  "~i~")

(defn instance-master-id
  "Join parent id, ~i~ marker and supplied digest without adding a colon segment."
  [parent-master-id subject-digest]
  (str parent-master-id instance-marker subject-digest))

(defn instance-master-id?
  "Recognize the ~i~ marker in a string; does not validate a complete instance id."
  [master-id]
  (and (string? master-id)
       #?(:clj (.contains ^String master-id instance-marker)
          :cljs (not= -1 (.indexOf master-id instance-marker)))))

(def instance-common-keys
  "The three keys an instance form adds to its parent's material. All three are
   REQUIRED — `valid-material?` compares key sets exactly — so `no pin` is the
   explicit value `nil` rather than an absent key. That is what makes unpinning
   a recorded material CHANGE (pin → nil) instead of a key deletion with no
   revision to stand in (T4/T11)."
  #{:facet-master/subject
    :facet-master/pin
    :facet-master/deviates?})

(defn valid-pin?
  "Accept nil or exactly one nonempty :pinned-revision-id string in a map."
  [x]
  (or (nil? x)
      (and (map? x)
           (= #{:pinned-revision-id} (set (keys x)))
           (string? (:pinned-revision-id x))
           (seq (:pinned-revision-id x)))))

(defn- non-empty-string?
  "Return truthy only for a string with at least one character."
  [x]
  (and (string? x) (seq x)))

(def ^:private instance-validators
  {:facet-master/subject
   {:valid? non-empty-string?
    :error-type :facet-master/subject-invalid}
   :facet-master/pin
   {:valid? valid-pin?
    :error-type :facet-master/pin-invalid}
   :facet-master/deviates?
   {:valid? boolean?
    :error-type :facet-master/deviates-invalid}})

(defn- instance-grammars
  "The parent's grammar map, each version widened by the three instance keys.
   Reusing the parent's VERSION NUMBERS reinterprets nothing: the grammar map
   is looked up on the INSTANCE spec, whose master-id is different, so there is
   no durable form anywhere that these declarations could re-read. The P3 law —
   a durable v0 form is never reinterpreted — is preserved structurally."
  [parent-grammars]
  (into {}
        (map (fn [[version declaration]]
               [version
                (-> declaration
                    (update :material-keys
                            #(into (set %) instance-common-keys))
                    (update :validators merge instance-validators))]))
        parent-grammars))

(defn instance-floor-revision-id
  "Derive the synthetic fallback revision label for an instance master."
  [instance-id]
  (str "code-floor:" instance-id))

(defn instance-form
  "One complete instance form: the parent's material keys (a snapshot of what
   is being deviated FROM, which is also what makes the diff computable) plus
   the three instance keys. `overrides` names the deviating keys."
  [parent-spec instance-id subject-uid
   {:keys [grammar material deviates? pin overrides]}]
  (let [grammar (or grammar (:facet-master/grammar
                             (:facet-master/floor-form parent-spec)))
        declaration (get-in parent-spec [:facet-master/grammars grammar])
        material-keys (set (:material-keys declaration))
        base (select-keys (merge material overrides) material-keys)]
    (merge base
           {:facet-master/id instance-id
            :facet-master/grammar grammar
            :facet-master/facet (:facet-master/facet parent-spec)
            :facet-master/subject (str subject-uid)
            :facet-master/pin pin
            :facet-master/deviates? (boolean deviates?)})))

(defn instance-spec
  "Synthesize an instance master's spec from its parent's. PURE and with no
   hashing, so the client can rebuild it from the served instance-master id and
   resolve wear without a second read. Minting the id (which needs a digest of
   the subject uid) is the server adapter's job."
  [parent-spec instance-id subject-uid]
  (let [floor-form (:facet-master/floor-form parent-spec)
        grammar (:facet-master/grammar floor-form)]
    {:facet-master/id instance-id
     :facet-master/facet (:facet-master/facet parent-spec)
     :facet-master/instance? true
     :facet-master/parent-id (:facet-master/id parent-spec)
     :facet-master/subject (str subject-uid)
     :facet-master/source-ref
     (str (:facet-master/source-ref parent-spec) "/instance/" subject-uid)
     :facet-master/default-form
     (instance-form parent-spec instance-id subject-uid
                    {:grammar grammar :material floor-form})
     :facet-master/floor-form
     (instance-form parent-spec instance-id subject-uid
                    {:grammar grammar :material floor-form})
     :facet-master/code-floor-revision-id
     (instance-floor-revision-id instance-id)
     :facet-master/grammars (instance-grammars
                             (:facet-master/grammars parent-spec))}))

(defn instance-holds
  "What one instance form HOLDS. Total over any material, including nonsense.

   A pin and a deviation can both be spelled in one form; the pin wins, because
   that is what pinning MEANS — do not follow the shared activation. The other
   keys are then the inherited snapshot and are inert, which the serve says out
   loud rather than leaving the reader to infer."
  [material]
  (cond
    (some? (:facet-master/pin material)) :holds/pin
    (true? (:facet-master/deviates? material)) :holds/deviation
    :else :holds/inherit))

(defn floor-master-id
  "Return the spec's code-floor revision label used as a deciding-master label."
  [spec]
  (:facet-master/code-floor-revision-id spec))

(defn wear-for-subject
  "R2's RESOLUTION LAW, pure and total:

     wear(subject, facet) = valid active INSTANCE revision
                          → else the shared ACTIVE revision
                          → else the code FLOOR (always present)

   This is P5's tier order made durable. Every branch returns a complete wear
   map of the same shape `resolved-wear` returns, widened by `:facet-master/
   tier`, `:facet-master/pinned?` and the instance link — so every renderer,
   stamp and dispatch path that already consumes a wear consumes this one
   unchanged.

   `instance-served` is nil when the subject has no instance master. Its
   `:facet-master/pinned` field carries the pinned SHARED revision already
   compiled by the serve, so resolution needs no second read (T5: the echo bar
   dies if this costs a read per block per keystroke).

   A malformed instance revision falls through to the shared active revision
   rather than to the floor: the deviation simply does not apply. A malformed
   PINNED revision falls to the floor, because a pin is a claim about which
   revision is worn and that claim cannot be honored (R2, verbatim)."
  [parent-spec shared-served instance-served]
  (let [shared-wear (resolved-wear parent-spec shared-served)
        shared-wear (assoc shared-wear
                           :facet-master/tier
                           (if (:facet-master/floor? shared-wear)
                             :floor
                             :shared)
                           :facet-master/pinned? false)
        instance-id (:facet-master/id instance-served)
        subject (:facet-master/subject instance-served)
        ispec (when (and instance-id subject)
                (instance-spec parent-spec instance-id subject))
        imaterial (:facet-master/material instance-served)
        instance-valid?
        (boolean
         (and ispec
              (string? (:facet-master/active-revision-id instance-served))
              (valid-material? ispec
                               (:facet-master/grammar instance-served)
                               imaterial)))
        holds (when instance-valid? (instance-holds imaterial))]
    (case (if instance-valid? holds :holds/none)
      :holds/pin
      (let [pinned (:facet-master/pinned instance-served)]
        (if (and (true? (:valid? pinned))
                 (valid-material? parent-spec (:grammar pinned)
                                  (:material pinned)))
          (merge (:material pinned)
                 {:facet-master/id (:facet-master/id parent-spec)
                  :facet-master/facet (:facet-master/facet parent-spec)
                  :facet-master/grammar (:grammar pinned)
                  :facet-master/revision-id (:revision-id pinned)
                  :facet-master/floor? false
                  :facet-master/tier :instance
                  :facet-master/pinned? true
                  :facet-master/instance-id instance-id
                  :facet-master/subject subject})
          (assoc (code-floor parent-spec)
                 :facet-master/tier :floor
                 :facet-master/pinned? true
                 :facet-master/instance-id instance-id
                 :facet-master/subject subject
                 :facet-master/pin-unresolved? true)))

      :holds/deviation
      (let [grammar (:facet-master/grammar instance-served)
            declaration (get-in parent-spec [:facet-master/grammars grammar])
            parent-material (select-keys imaterial
                                         (set (:material-keys declaration)))]
        (if (valid-material? parent-spec grammar parent-material)
          (merge parent-material
                 {:facet-master/id (:facet-master/id parent-spec)
                  :facet-master/facet (:facet-master/facet parent-spec)
                  :facet-master/grammar grammar
                  :facet-master/revision-id
                  (:facet-master/active-revision-id instance-served)
                  :facet-master/floor? false
                  :facet-master/tier :instance
                  :facet-master/pinned? false
                  :facet-master/instance-id instance-id
                  :facet-master/subject subject})
          shared-wear))

      ;; :holds/inherit and :holds/none — the subject follows the shared
      ;; master. T11: this is also what the REMOVAL of a deviation resolves to,
      ;; which is why removal is a rollback to the inherited state and never a
      ;; tombstone (a tombstone cell DELETES THE UNIT from the page).
      shared-wear)))

(defn deviation-diff
  "A deviation rendered against what the subject WOULD wear without it. The
   diff is a PROJECTION — computed on read from the two forms, never stored —
   so it can never drift from the material it describes (R2).

   `:diff/inherited-revision-id` is the honest answer to `what am I overriding`
   even when the subject is pinned, which is exactly when it matters most."
  [parent-spec shared-served instance-served]
  (when instance-served
    (let [inherited (resolved-wear parent-spec shared-served)
          worn (wear-for-subject parent-spec shared-served instance-served)
          imaterial (:facet-master/material instance-served)
          holds (instance-holds imaterial)
          declaration (get-in parent-spec
                              [:facet-master/grammars
                               (:facet-master/grammar instance-served)])
          material-keys (sort-by pr-str (:material-keys declaration))
          changed (into {}
                        (keep (fn [k]
                                (let [from (get inherited k)
                                      to (get imaterial k)]
                                  (when (not= from to)
                                    [k {:from from :to to}]))))
                        material-keys)]
      {:diff/parent-id (:facet-master/id parent-spec)
       :diff/instance-id (:facet-master/id instance-served)
       :diff/subject (:facet-master/subject instance-served)
       :diff/holds holds
       :diff/inherited-revision-id (:facet-master/revision-id inherited)
       :diff/worn-revision-id (:facet-master/revision-id worn)
       :diff/tier (:facet-master/tier worn)
       :diff/pinned? (true? (:facet-master/pinned? worn))
       :diff/changed (if (= :holds/deviation holds) changed {})})))

(defn compose
  "Minimum composition vocabulary demanded by the first collision.

   Descriptors carry `:wear`, `:stamp`, and `:value`. Same-slot contributions
   compose only when every active master declares `:append` and priorities are
   distinct. The deterministic order is still returned on conflict so the
   caller can keep rendering, but `:conflicts` must be rendered as lint."
  [descriptors]
  (let [descriptors (vec (remove nil? descriptors))
        groups (vals (group-by #(get-in % [:stamp :material/slot])
                               descriptors))
        conflicts
        (->> groups
             (keep
              (fn [group]
                (when (> (count group) 1)
                  (let [group
                        (sort-by
                         #(get-in % [:stamp :material/master])
                         group)
                        merges (mapv #(get-in % [:wear :facet-master/merge])
                                      group)
                        priorities
                        (mapv #(get-in % [:wear :facet-master/priority])
                              group)
                        reason
                        (cond
                          (not-every? #{:append} merges)
                          :material-composition/merge-missing-or-incompatible

                          (not-every? integer? priorities)
                          :material-composition/priority-missing

                          (not= (count priorities)
                                (count (set priorities)))
                          :material-composition/priority-tie)]
                    (when reason
                      {:type reason
                       :material/slot
                       (get-in (first group) [:stamp :material/slot])
                       :material/masters
                       (->> group
                            (map #(get-in % [:stamp :material/master]))
                            sort
                            vec)
                       :material/priorities priorities})))))
             (sort-by (juxt (comp pr-str :material/slot)
                            (comp pr-str :type)))
             vec)
        ordered
        (->> descriptors
             (sort-by
              (fn [{:keys [wear stamp]}]
                [(or (:facet-master/priority wear)
                     #?(:clj Long/MAX_VALUE :cljs js/Number.MAX_SAFE_INTEGER))
                 (str (:material/master stamp))
                 (pr-str (:material/site stamp))]))
             vec)]
    {:contributions ordered
     :conflicts conflicts}))
