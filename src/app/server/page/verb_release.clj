(ns app.server.page.verb-release
  "Build, import and inspect the fixed reply-to-block release evidence chain.
   code-address resolves a committed Git form through ingest/code-import;
   manifest assembles eight EDN nodes from supplied evidence. Validation checks
   the implemented chain/address fields, not whether receipts were executed.
   import-request delegates source cutting to the Clojure ingest adapter.

   append-release! borrows an ObjectContainer runtime, appends the import and
   waits for its decision; read-release joins stored source, bundle and units.
   This namespace owns neither runtime handles nor durable state. Importing
   release evidence does not itself install a verb or activate a binding."
  (:require [app.server.ingest.code-import :as code-import]
            [app.server.rama.object-container :as oc]
            [app.server.ingest.clojure-adapter :as clj-adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.worn.attention-material :as attention]
            [app.server.worn.binding-material :as binding-material]
            [app.server.page.reply-to-block :as reply-to-block]
            [app.server.page.verb-registry :as verb-registry]
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
  (let [blob-sha (code-import/head-blob-sha repo-root head path)
        _ (when (str/blank? blob-sha)
            (throw (ex-info "release path absent at commit"
                            {:head head :path path})))
        cut (code-import/cut-named-units
             blob-sha (code-import/blob-text repo-root blob-sha))
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
  "Build the ordered eight-node reply-to-block release chain from supplied
   addresses, test/build receipts, activation and worn evidence. This is pure
   construction; release-errors performs the subsequent shape checks."
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
  "Check required address fields are present as nonblank string forms; does not resolve the address."
  [x]
  (and (map? x)
       (every? #(not (str/blank? (str (get x %))))
               [:blob-sha :path :block-path :source-id :unit-id
                :source-anchor-id])))

(defn release-errors
  "Return validation errors for chain kind/order, the fixed verb and binding,
   implementation/test/build address fields, and forbidden embedded source keys.
   Address checks require nonblank fields but do not resolve Git objects. The
   wish address, activation/worn payloads and receipt outcomes are not validated
   here; a complete? result is limited to these implemented checks."
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
  "Validate/serialize nodes and build a Clojure-source import request under
   release-ref, using caller request-id/time-ms and the fixed system actor.
   No append occurs here; malformed release data throws."
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
  "Build and append the import with :ack, then wait up to 20 seconds for its
   stored decision. Return request, decision and source-id. decision may be nil
   on timeout or rejected; returning this map does not imply acceptance.
   Borrow runtime handles and propagate append/read exceptions."
  [runtime nodes opts]
  (let [request (import-request nodes opts)]
    (ocr/append-object-container-request! runtime request :ack)
    {:request request
     :decision (ocr/await-object-container-decision runtime request 20000)
     :source-id (get-in request [:payload :source-artifacts 0 :source-id])}))

(defn read-release
  "Read the latest stored source at release-ref, its material bundle and each
   referenced unit in order. Parse node EDN and return nodes, anchors and the
   release-errors result; missing source returns an explicit not-found map.
   Does not cap the stored unit references at eight or catch read/parse errors.
   The interaction-table projection catches those errors at its call site."
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
