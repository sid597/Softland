(ns app.server.rama.verb-release
  "P8 driver over the existing Object Container public API.

   A verb release is ordinary addressable material: eight ordered EDN nodes
   imported through the already-shipped Clojure adapter. Source code and test
   bodies are NEVER copied into the release. They are named by committed git
   blob, repo path, derived unit, and source anchor. The stable source ref has
   versioned verb identity; importing different release bytes advances normal
   OC source history while an identical retry converges.

   This is deliberately not a Rama module, topology, depot, or PState."
  (:require [app.server.rama.code-atoms :as code-atoms]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.clojure-adapter :as clj-adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.attention-material :as attention]
            [app.shared.binding-material :as binding-material]
            [app.shared.reply-to-block :as reply-to-block]
            [app.shared.verb-registry :as verb-registry]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def release-ref reply-to-block/release-ref)

(def release-kinds
  [:wish :implementation :test-receipt :build-receipt
   :verb :binding :activation :worn])

(defn code-address
  "Resolve one named form at a committed HEAD to the address the code lane
   stores. Throws when the path or form does not exist; a release may never
   carry a best-effort anchor."
  [repo-root head path block-path]
  (let [blob-sha (code-atoms/head-blob-sha repo-root head path)
        _ (when (str/blank? blob-sha)
            (throw (ex-info "release path absent at commit"
                            {:head head :path path})))
        cut (code-atoms/cut-named-units
             blob-sha (code-atoms/blob-text repo-root blob-sha))
        unit (get-in cut [:units block-path])]
    (when-not unit
      (throw (ex-info "release form anchor absent at commit"
                      {:head head :path path :block-path block-path})))
    {:blob-sha blob-sha
     :path path
     :block-path block-path
     :source-id (:source-id cut)
     :unit-id (:unit-id unit)
     :source-anchor-id (oc/source-anchor-id (:unit-id unit))}))

(defn manifest
  "Build the fixed P8 release chain. Exactly one verb node and one binding node
   are possible by construction; callers supply only evidence addresses,
   observed receipts, and the deployment activation."
  [{:keys [wish-address implementation-addresses test-receipt build-receipt
           activation worn]}]
  [{:release/step 0
    :release/kind :wish
    :release/address wish-address}
   {:release/step 1
    :release/kind :implementation
    :release/addresses (vec implementation-addresses)}
   {:release/step 2
    :release/kind :test-receipt
    :release/address (:address test-receipt)
    :release/receipt (dissoc test-receipt :address)}
   {:release/step 3
    :release/kind :build-receipt
    :release/address (:address build-receipt)
    :release/receipt (dissoc build-receipt :address)}
   {:release/step 4
    :release/kind :verb
    :release/verb
    {:verb/name reply-to-block/verb-name
     :verb/version reply-to-block/verb-version
     :verb/effect-class :durable-via-request
     :verb/release-ref release-ref}}
   {:release/step 5
    :release/kind :binding
    :release/binding
    {:master-id attention/master-id
     :grammar attention/reply-bindings-grammar-version
     :revision-id (:revision-id activation)
     :row attention/reply-binding-row}}
   {:release/step 6
    :release/kind :activation
    :release/activation activation}
   {:release/step 7
    :release/kind :worn
    :release/worn worn}])

(defn- address?
  [x]
  (and (map? x)
       (every? #(not (str/blank? (str (get x %))))
               [:blob-sha :path :block-path :source-id :unit-id
                :source-anchor-id])))

(defn release-errors
  "Closed validation at the release boundary. In addition to the fixed chain,
   reject embedded source/code text so the release can only point at committed
   material, never become a copied shadow of it."
  [nodes]
  (let [nodes (vec nodes)
        verb-nodes (filterv #(= :verb (:release/kind %)) nodes)
        binding-nodes (filterv #(= :binding (:release/kind %)) nodes)
        verb (:release/verb (first verb-nodes))
        binding (:release/binding (first binding-nodes))
        forbidden? (some
                    (fn [x]
                      (and (map? x)
                           (some #(contains? x %)
                                 [:code/text :content-text :source/raw-text
                                  :source-raw-text])))
                    (tree-seq coll? seq nodes))
        addresses (concat
                   (:release/addresses (second nodes))
                   (keep :release/address
                         [(nth nodes 2 nil) (nth nodes 3 nil)]))]
    (cond-> []
      (not= release-kinds (mapv :release/kind nodes))
      (conj {:type :verb-release/chain-shape})

      (not= (vec (range (count nodes))) (mapv :release/step nodes))
      (conj {:type :verb-release/step-order})

      (not= 1 (count verb-nodes))
      (conj {:type :verb-release/verb-count :actual (count verb-nodes)})

      (not= 1 (count binding-nodes))
      (conj {:type :verb-release/binding-count :actual (count binding-nodes)})

      (not (and (= reply-to-block/verb-name (:verb/name verb))
                (= reply-to-block/verb-version (:verb/version verb))
                (= :durable-via-request (:verb/effect-class verb))
                (= release-ref (:verb/release-ref verb))
                (verb-registry/known? (:verb/name verb) (:verb/version verb))))
      (conj {:type :verb-release/verb-invalid :verb verb})

      (not (and (= attention/master-id (:master-id binding))
                (= attention/reply-bindings-grammar-version
                   (:grammar binding))
                (= attention/reply-binding-row (:row binding))
                (binding-material/valid-row? (:row binding) true)))
      (conj {:type :verb-release/binding-invalid :binding binding})

      (or (empty? addresses) (not-every? address? addresses))
      (conj {:type :verb-release/address-invalid})

      forbidden?
      (conj {:type :verb-release/copied-source-forbidden}))))

(defn release-source
  "One top-level EDN map per chain node. The Clojure adapter cuts each form into
   its own DerivedUnitRow and SourceAnchorRow, making every step addressable."
  [nodes]
  (let [errors (release-errors nodes)]
    (when (seq errors)
      (throw (ex-info "invalid verb release" {:errors errors})))
    (str (str/join "\n" (map pr-str nodes)) "\n")))

(defn import-request
  [nodes {:keys [request-id time-ms]}]
  (clj-adapter/clojure-source-import-request
   (release-source nodes)
   release-ref
   {:request/id request-id
    :time-ms time-ms
    :claimed/at-ms time-ms
    :actor {:actor/id "softland:p8"
            :actor/type :system
            :actor/capabilities #{:object-container/import-material
                                  :source/ingest
                                  :object/edit}}
    :provenance {:source/type :verb-release
                 :source/ref release-ref}}))

(defn append-release!
  [runtime nodes opts]
  (let [request (import-request nodes opts)]
    (ocr/append-object-container-request! runtime request :ack)
    {:request request
     :decision (ocr/await-object-container-decision runtime request 20000)
     :source-id (get-in request [:payload :source-artifacts 0 :source-id])}))

(defn read-release
  "Read latest source by stable release ref, its whole material bundle, then
   the fixed eight unit targets named by that bundle. These joins are all
   server-side inside the interaction-table projection: the portal still makes
   one client roundtrip and performs zero client joins."
  [runtime]
  (if-let [source (ocr/read-latest-source-by-ref runtime release-ref)]
    (let [source-id (:source-id source)
          bundle (ocr/read-common-material-for-source runtime source-id)
          unit-refs (sort-by :order-key (:derived-units bundle))
          nodes (mapv (fn [ref]
                        (some-> (ocr/read-unit runtime (:target-id ref))
                                :content-text
                                edn/read-string))
                      unit-refs)
          errors (release-errors nodes)]
      {:release/ref release-ref
       :release/found? true
       :release/source-id source-id
       :release/source-hash (:source-hash source)
       :release/nodes nodes
       :release/anchors (vec (:anchors bundle))
       :release/complete? (empty? errors)
       :release/errors errors})
    {:release/ref release-ref
     :release/found? false
     :release/nodes []
     :release/anchors []
     :release/complete? false
     :release/errors [{:type :verb-release/not-found}]}))
