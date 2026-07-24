(ns app.server.rama.object-container.facet-master
  "Shared second-wearer adapter for revisioned facet material. Every master
   uses the same existing object-container import, immutable revision, and
   explicit revisioned active-pointer conventions; no facet owns a parallel
   truth or activation path."
  (:require [clojure.string :as str]
            [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.util-fns :as util-fns]
            [app.shared.facet-material :as facet-material]))

(defn master-id
  [spec]
  (:facet-master/id spec))

(defn source-ref
  [spec]
  (:facet-master/source-ref spec))

(defn document-id
  [spec]
  (oc/document-id-for-object-key (master-id spec)))

(defn active-pointer-container-id
  [spec]
  (oc/block-container-id (master-id spec) "active-pointer"))

(defn- master-slug
  [spec]
  (let [id (master-id spec)]
    (if (str/starts-with? id "fm:")
      (subs id 3)
      id)))

(defn import-key
  [spec source-hash]
  (str "imp:fm:" (master-id spec) ":" source-hash))

(defn materialization
  "Pure revisioned-OC materialization for one candidate of any known master."
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
        request-id (or (:request/id opts) (core/random-id "req"))
        created-at (long (or (:time-ms opts) (core/now-ms)))
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
        pointer-revision-row
        (when pointer-revision-id
          (oc/->RevisionRow
           pointer-revision-id pointer-container-id nil revision-id
           (oc/source-hash revision-id)
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
  [runtime request]
  (let [already-decided? (some? (ocr/read-decision runtime request))]
    (ocr/append-object-container-request! runtime request :ack)
    {:decision (ocr/read-decision runtime request)
     :replay? already-decided?}))

(defn import-candidate!
  ([runtime spec source] (import-candidate! runtime spec source {}))
  ([runtime spec source opts]
   (let [request (candidate-import-request spec source opts)
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
  "Resolve latest and active independently through the same OC reads for every
   master. The pointer current revision names the worn immutable revision."
  [runtime spec]
  (let [latest (ocr/read-current-revision runtime (document-id spec))
        pointer (ocr/read-current-revision
                 runtime (active-pointer-container-id spec))
        active-id (:content-text pointer)
        active (when (string? active-id)
                 (ocr/read-revision runtime active-id))]
    {:facet-master-id (master-id spec)
     :latest-revision latest
     :active-pointer pointer
     :active-revision active}))

(defn activate!
  ([runtime spec revision-id] (activate! runtime spec revision-id {}))
  ([runtime spec revision-id opts]
   (let [master-id (master-id spec)
         document-id (document-id spec)
         pointer-container-id (active-pointer-container-id spec)
         candidate (ocr/read-revision runtime revision-id)
         compiled (when candidate
                    (facet-material/compile-source
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
                            (core/random-id "req"))
             request
             (oc/object-edit-request
              :object-container
              pointer-container-id
              revision-id
              {:object/key master-id
               :document/container-id document-id
               :request/id request-id
               :time-ms (:time-ms opts)
               :revision/id
               (str "rev:" master-id ":active:"
                    (core/sha-256 request-id))
               :edit/client-id (str "facet-master-activation:" request-id)
               :edit/seq 0
               :edit/lineage-key pointer-container-id
               :idempotency/key
               (or (:idempotency/key opts)
                   (:idempotency-key opts)
                   (str "facet-master/activate:" master-id ":"
                        revision-id ":" request-id))
               :actor (or (:actor opts)
                          {:actor/id "system"
                           :actor/type :system
                           :actor/capabilities #{:object/edit}})
               :provenance
               (or (:provenance opts)
                   {:source/type :facet-master-activation
                    :source/ref master-id})})
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
  "Idempotently install one master's v0 bytes and pointer only when absent.
   For provenance this mints the same request IDs and source bytes P1 used."
  [runtime spec]
  (let [slug (master-slug spec)
        before (read-master runtime spec)
        imported (when (nil? (:latest-revision before))
                   (import-candidate!
                    runtime spec
                    (facet-material/source-for
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
  "Explicit, deterministic grammar migration: import the exact new bytes and
   activate that immutable revision. Old revisions keep their original
   grammar meaning and remain rewearable."
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
                          (core/sha-256 drill-id))
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
