(ns app.server.rama.object-container.provenance-material
  "P1 provenance facet-master adapter. Candidate bytes and revisions ride the
   existing object-container import seam; activation edits a second ordinary
   revisioned container whose current content is the explicit active pointer."
  (:require [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.util-fns :as util-fns]
            [app.shared.provenance-material :as material]))

(def source-ref "softland://facet-master/provenance")
(def document-id (oc/document-id-for-object-key material/master-id))
(def active-pointer-container-id
  (oc/block-container-id material/master-id "active-pointer"))

(defn import-key
  [source-hash]
  (str "imp:fm:" material/master-id ":" source-hash))

(defn materialization
  "Pure minimal revisioned-OC materialization for one provenance candidate."
  [source opts]
  (let [source (str source)
        source-hash (oc/source-hash source)
        source-ref-key (oc/source-ref-key source-ref)
        import-key (import-key source-hash)
        source-id (oc/source-id-for-object-key material/master-id)
        request-id (or (:request/id opts) (core/random-id "req"))
        created-at (long (or (:time-ms opts) (core/now-ms)))
        created-by (or (get-in opts [:actor :actor/id]) "system")
        event-id (str "evt:" material/master-id ":" request-id)
        order-key (oc/fixed-width-order-key created-at request-id)
        revision-id (oc/import-revision-id material/master-id document-id source-hash)
        source-row (oc/->SourceArtifactRow
                    source-id source-ref source-hash :edn source document-id
                    (long (count (.getBytes source "UTF-8")))
                    created-at created-by event-id)
        version-row (oc/->SourceVersionRow
                     source-ref-key source-ref source-hash source-id document-id
                     material/master-id order-key created-at event-id)
        revision-row (oc/->RevisionRow
                      revision-id document-id nil source source-hash order-key
                      created-at created-by event-id)
        anchor-id (oc/source-anchor-id document-id)
        container-row (oc/->ObjectContainerRow
                       document-id :facet-master material/master-id :private
                       source-id anchor-id nil document-id revision-id nil nil
                       created-at created-by event-id)
        anchor-row (oc/->SourceAnchorRow
                    anchor-id :object-container document-id source-id source-ref
                    source-hash 0 (count source) nil event-id)
        pointer-revision-id
        (when (:include-active-pointer? opts)
          (oc/import-revision-id
           material/master-id
           active-pointer-container-id
           (str "active:" revision-id)))
        pointer-row
        (when pointer-revision-id
          (oc/->ObjectContainerRow
           active-pointer-container-id :text-block material/master-id :private
           source-id (oc/source-anchor-id active-pointer-container-id) nil
           document-id pointer-revision-id nil nil
           created-at created-by event-id))
        pointer-revision-row
        (when pointer-revision-id
          (oc/->RevisionRow
           pointer-revision-id active-pointer-container-id nil revision-id
           (oc/source-hash revision-id)
           (oc/fixed-width-order-key created-at (str request-id ":active"))
           created-at created-by event-id))
        pointer-anchor-row
        (when pointer-revision-id
          (oc/->SourceAnchorRow
           (oc/source-anchor-id active-pointer-container-id)
           :object-container active-pointer-container-id source-id source-ref
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
  ([source] (candidate-import-request source {}))
  ([source opts]
   (let [m (materialization source opts)
         payload {:object-key material/master-id
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
                      material/master-id (:import-key m) payload)
         actor (or (:actor opts)
                   {:actor/id "system"
                    :actor/type :system
                    :actor/capabilities
                    #{:object-container/import-material
                      :source/ingest
                      :facet-master/activate}})]
     (assoc
      (core/action-request
       {:request-id (:request-id m)
        :request-type :object-container/import-material
        :time-ms (:created-at m)
        :actor actor
        :target {:target/kind :object-container-import
                 :target/id (:import-key m)
                 :target/address {:object/key material/master-id}}
        :action {:action/type :object-container/import-material
                 :action/capability :object-container/import-material
                 :action/params {:source/family :facet-master
                                 :source/format :edn}}
        :routing/key [:object-container/import material/master-id]
        :payload payload
        :provenance {:source/type :facet-master
                     :source/ref source-ref}})
      :partition/key material/master-id
      :object/key material/master-id
      :import/key (:import-key m)
      :source/family :facet-master
      :source/format :edn
      :idempotency/key (:import-key m)
      :material/fingerprint fingerprint
      :facet-master/revision-id (:revision-id m)))))

(defn- append-and-read!
  [runtime request]
  (let [already-decided? (some? (ocr/read-decision runtime request))]
    (ocr/append-object-container-request! runtime request :ack)
    {:decision (ocr/read-decision runtime request)
     :replay? already-decided?}))

(defn import-candidate!
  ([runtime source] (import-candidate! runtime source {}))
  ([runtime source opts]
   (let [request (candidate-import-request source opts)
         {:keys [decision replay?]} (append-and-read! runtime request)
         accepted? (oc/decision-accepted? decision)]
     (when (and accepted? (not replay?))
       (swap! util-fns/!ingest-epoch-atom inc))
     {:request request
      :decision decision
      :accepted? accepted?
      :replay? replay?
      :revision-id (:facet-master/revision-id request)})))

(defn read-master
  "Resolve latest and active independently. The pointer is an ordinary
   revisioned OC container; its current content names the worn material
   revision. All reads route to the same fm:provenance partition."
  [runtime]
  (let [latest (ocr/read-current-revision runtime document-id)
        pointer (ocr/read-current-revision runtime active-pointer-container-id)
        active-id (:content-text pointer)
        active (when (string? active-id)
                 (ocr/read-revision runtime active-id))]
    {:facet-master-id material/master-id
     :latest-revision latest
     :active-pointer pointer
     :active-revision active}))

(defn activate!
  ([runtime revision-id] (activate! runtime revision-id {}))
  ([runtime revision-id opts]
   (let [candidate (ocr/read-revision runtime revision-id)
         compiled (when candidate
                    (material/compile-source (:content-text candidate)))
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
                            (core/random-id "req"))
             request
             (oc/object-edit-request
              :object-container
              active-pointer-container-id
              revision-id
              {:object/key material/master-id
               :document/container-id document-id
               :request/id request-id
               :time-ms (:time-ms opts)
               :revision/id
               (str "rev:" material/master-id ":active:"
                    (core/sha-256 request-id))
               :edit/client-id (str "facet-master-activation:" request-id)
               :edit/seq 0
               :edit/lineage-key active-pointer-container-id
               :idempotency/key
               (or (:idempotency/key opts)
                   (:idempotency-key opts)
                   (str "facet-master/activate:" material/master-id ":"
                        revision-id ":" request-id))
               :actor (or (:actor opts)
                          {:actor/id "system"
                           :actor/type :system
                           :actor/capabilities #{:object/edit}})
               :provenance
               (or (:provenance opts)
                   {:source/type :facet-master-activation
                    :source/ref material/master-id})})
             {:keys [decision replay?]} (append-and-read! runtime request)
             accepted? (oc/decision-accepted? decision)]
         (when (and accepted? (not replay?))
           (swap! util-fns/!ingest-epoch-atom inc))
         {:request request
          :decision decision
          :accepted? accepted?
          :replay? replay?
          :revision-id revision-id})))))

(defn ensure-master!
  "Idempotently install the byte-identical v0 material and point active at it
   only when the durable pointer is absent."
  [runtime]
  (let [before (read-master runtime)
        imported (when (nil? (:latest-revision before))
                   (import-candidate!
                    runtime material/default-source
                    {:request/id "facet-master-provenance-v0"
                     :time-ms 0
                     :include-active-pointer? true}))
        state (read-master runtime)
        revision-id (or (some-> state :latest-revision :revision-id)
                        (:revision-id imported))
        activated (when (and revision-id (nil? (:active-pointer state)))
                    (activate!
                     runtime revision-id
                     {:request/id "facet-master-provenance-activate-v0"
                      :time-ms 0
                      :idempotency/key
                      (str "facet-master/activate:" material/master-id
                           ":" revision-id ":bootstrap")}))]
    {:import imported
     :activation activated
     :state (read-master runtime)}))

(defn malformed-drill!
  "Write a deterministic malformed candidate and attempt to activate it. The
   imported candidate revision is the retained trace; the pointer cannot move
   because validation closes before an activation request is submitted."
  [runtime drill-id]
  (let [drill-id (str drill-id)
        source (str "{:facet-master/id " (pr-str material/master-id)
                    " :drill/id " (pr-str drill-id))
        hash (oc/source-hash source)
        imported (import-candidate!
                  runtime source
                  {:request/id (str "facet-master-drill-import-"
                                    (subs hash 0 16))})
        activation (activate!
                    runtime (:revision-id imported)
                    {:request/id (str "facet-master-drill-activate-"
                                     (core/sha-256 drill-id))
                     :idempotency/key
                     (str "facet-master/activate:" material/master-id ":"
                          (:revision-id imported) ":drill:" drill-id)})]
    {:drill-id drill-id
     :candidate-revision-id (:revision-id imported)
     :import-decision (:decision imported)
     :activation-decision (:decision activation)
     :activation-errors (:errors activation)
     :state (read-master runtime)}))
