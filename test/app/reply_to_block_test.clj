(ns app.reply-to-block-test
  "P8 — one addressable resident verb, one material binding, and a release
   chain that points at committed material instead of copying it."
  (:require [app.server.rama.object-container.clojure-adapter :as clj-adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.material-portal :as material-portal]
            [app.server.rama.verb-release :as release]
            [app.shared.attention-material :as attention]
            [app.shared.invocation-material :as invocation]
            [app.shared.reply-to-block :as reply]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn- address
  [suffix]
  {:blob-sha (str "blob-" suffix)
   :path (str "src/app/" suffix ".cljc")
   :block-path suffix
   :source-id (str "src:" suffix)
   :unit-id (str "du:" suffix)
   :source-anchor-id (str "sa:du:" suffix)})

(def valid-manifest
  (release/manifest
   {:wish-address
    {:blob-sha "wish-blob"
     :path "docs/current-mental-model/vision/LOG.md"
     :line-start 727
     :line-end 736}
    :implementation-addresses
    [(address "reply-to-block") (address "verb-registry")
     (address "attention-material")]
    :test-receipt
    {:address (address "reply-to-block-test")
     :command "clj -X:test fast"
     :status :pass
     :tests 1
     :assertions 1}
    :build-receipt
    {:address (address "client-build-witness")
     :command "npm run build"
     :status :pass
     :warnings 0}
    :activation
    {:request-id "p8-activate"
     :revision-id "rev:p8-attention-v3"
     :time-ms 8000}
    :worn
    {:master-id attention/master-id
     :revision-id "rev:p8-attention-v3"
     :entity-id "du:addressed-block"}}))

(deftest narrowed-open-cannot-leak-an-unaddressed-block
  (let [addressed {:wearer/entity-id "du:a"
                   :wearer/facets
                   [{:wearer/master-id "fm:attention"}
                    {:wearer/master-id "fm:text-body"}]}
        unrelated {:wearer/entity-id "du:b"
                   :wearer/facets
                   [{:wearer/master-id "fm:provenance"}]}
        open (reply/narrowed-portal-open
              {:entity-id "du:a"
               :conversation-id "conv:1"
               :wearers [unrelated addressed]})]
    (is (= "du:a" (:entity-id open)))
    (is (= [addressed] (:wearers open)))
    (is (= ["fm:attention" "fm:text-body"] (:master-ids open)))
    (is (true? (:narrowed? open)))
    (is (= "conv:1" (:conversation-id open)))))

(deftest durable-request-pins-the-addressed-material
  (let [request
        (reply/request
         {:subject "du:a"
          :text "reply from this revision"
          :position {:x 1 :y 2}
          :turn-id "turn:1"
          :time-ms 100
          :thread-id "thread:1"
          :conversation-id "conv:1"
          :wearers [{:wearer/entity-id "du:b" :wearer/facets []}
                    {:wearer/entity-id "du:a"
                     :wearer/facets
                     [{:wearer/master-id "fm:attention"}]}]})]
    (is (= "du:a" (:source-unit-id request)))
    (is (= "reply from this revision" (:content-text request)))
    (is (= ["du:a"] (mapv :wearer/entity-id
                           (get-in request [:portal-open :wearers]))))
    (is (= ["fm:attention"]
           (get-in request [:portal-open :master-ids])))
    (is (nil? (reply/request {:subject "du:a" :text " \n "}))
        "blank material never mints a durable act")))

(deftest resident-prompt-keeps-projection-byte-order
  (is (= "SEED\nPORTAL\nUSER"
         (reply/compose-resident-prompt "SEED\n" "PORTAL\n" "USER")))
  (is (= "USER" (reply/compose-resident-prompt nil nil "USER"))))

(deftest p2-precontext-is-bounded-material-widening
  (let [wearer (fn [id master]
                 {:wearer/entity-id id
                  :wearer/facets [{:wearer/master-id master}]})
        too-old (wearer "du:old" "fm:provenance")
        context-a (wearer "du:c1" "fm:foldable")
        context-b (wearer "du:c2" "fm:threaded")
        addressed (wearer "du:a" "fm:attention")
        after (wearer "du:after" "fm:text-body")
        open (reply/narrowed-portal-open
              {:entity-id "du:a"
               :conversation-id "conv:1"
               :precontext "thread+2"
               :wearers [too-old context-a context-b addressed after]})]
    (is (= ["du:c1" "du:c2" "du:a"]
           (mapv :wearer/entity-id (:wearers open))))
    (is (= ["fm:attention" "fm:foldable" "fm:threaded"]
           (:master-ids open)))
    (is (= "thread+2" (:precontext open)))
    (is (not-any? #{"du:old" "du:after"}
                  (map :wearer/entity-id (:wearers open))))
    (is (= (str "[invocation precontext · thread+2]\n"
                "PORTAL\nUSER")
           (reply/compose-resident-prompt
            nil "PORTAL\n" "USER" "thread+2"))))
  (testing "malformed material can never widen the addressed open"
    (let [rows [{:wearer/entity-id "du:other" :wearer/facets []}
                {:wearer/entity-id "du:a" :wearer/facets []}]
          open (reply/narrowed-portal-open
                {:entity-id "du:a"
                 :precontext "whole-world"
                 :wearers rows})]
      (is (= ["du:a"] (mapv :wearer/entity-id (:wearers open))))
      (is (= 0 (invocation/precontext-depth "whole-world"))))))

(deftest release-is-eight-addressable-nodes-with-one-verb-and-one-binding
  (is (empty? (release/release-errors valid-manifest)))
  (is (= release/release-kinds (mapv :release/kind valid-manifest)))
  (is (= 1 (count (filter #(= :verb (:release/kind %)) valid-manifest))))
  (is (= 1 (count (filter #(= :binding (:release/kind %)) valid-manifest))))
  (is (= attention/reply-binding-row
         (get-in valid-manifest [5 :release/binding :row])))
  (is (not (str/includes? (release/release-source valid-manifest)
                          "defn reply-to-block!"))
      "the chain points at source anchors; it never copies implementation")
  (testing "the package stop is executable, not prose"
    (is (some #(= :verb-release/chain-shape (:type %))
              (release/release-errors
               (conj valid-manifest (nth valid-manifest 4)))))
    (is (some #(= :verb-release/chain-shape (:type %))
              (release/release-errors
               (conj valid-manifest (nth valid-manifest 5)))))))

(deftest release-import-cuts-every-chain-step-into-material
  (let [request (release/import-request
                 valid-manifest {:request-id "release-test" :time-ms 9000})
        units (get-in request [:payload :derived-units])
        anchors (get-in request [:payload :source-anchors])]
    (is (= :object-container/import-material (:request/type request)))
    (is (= release/release-ref
           (get-in request [:target :target/address :source/ref])))
    (is (= 8 (count units)) "one DerivedUnitRow per release step")
    (is (= 9 (count anchors)) "one document anchor plus eight unit anchors")
    (is (every? :source-anchor-id units))))

(deftest release-replay-follows-the-bundle-unit-refs
  (let [refs (mapv (fn [i]
                     {:target-id (str "unit:" i)
                      :order-key (format "%06d" i)})
                   (range 8))
        by-id (into {}
                    (map-indexed
                     (fn [i node]
                       [(str "unit:" i) {:content-text (pr-str node)}])
                     valid-manifest))]
    (with-redefs [ocr/read-latest-source-by-ref
                  (fn [_ _] {:source-id "src:release"
                             :source-hash "hash:release"})
                  ocr/read-common-material-for-source
                  (fn [_ _] {:derived-units (reverse refs)
                             :anchors []})
                  ocr/read-unit (fn [_ unit-id] (get by-id unit-id))]
      (let [replay (release/read-release :runtime)]
        (is (true? (:release/found? replay)))
        (is (true? (:release/complete? replay)))
        (is (= release/release-kinds
               (mapv :release/kind (:release/nodes replay))))
        (is (empty? (:release/errors replay)))))))

(deftest narrowed-portal-open-prices-only-the-addressed-masters
  (let [result
        (material-portal/open
         {:oc-rt nil}
         (constantly {})
         {:entity-id "du:a"
          :wearers []
          :master-ids ["fm:attention"]
          :narrowed? true})]
    (is (= 1 (get-in result [:portal/query-plan :plan/masters-priced])))
    (is (= ["fm:attention"] (vec (keys (:portal/masters result))))))
  (let [result
        (material-portal/open
         {:oc-rt nil}
         (constantly {})
         {:entity-id "du:a"
          :wearers []
          :master-ids []
          :narrowed? true})]
    (is (= 0 (get-in result [:portal/query-plan :plan/masters-priced]))
        "an honestly empty narrowed open never widens to the registry")))

(deftest portal-proves-the-release-row-is-worn
  (let [bindings-of @(ns-resolve 'app.server.rama.material-portal 'bindings-of)
        revision-id (get-in valid-manifest
                            [5 :release/binding :revision-id])
        table-row
        {:table/gesture :key/eval
         :table/phase :complete
         :table/modifiers :any
         :table/site :block/user-hit-area
         :table/tier :master
         :table/facet :attention
         :table/master-id attention/master-id
         :table/revision-id revision-id
         :table/floor? false
         :table/verb reply/verb-name
         :table/verb-version reply/verb-version
         :table/effect-class :durable-via-request
         :table/priority 10}
        release {:release/ref release/release-ref
                 :release/found? true
                 :release/complete? true
                 :release/nodes valid-manifest}
        result
        (bindings-of
         {:interaction-table/rows [table-row]
          :interaction-table/conflicts []
          :interaction-table/releases [release]}
         #{:block/user-hit-area})]
    (is (true? (get-in result [:bindings/releases 0 :release/worn?])))
    (is (= table-row
           (get-in result [:bindings/releases 0 :release/worn-row])))
    (is (= [:portal/bindings :bindings/releases]
           (get-in result [:bindings/releases 0 :release/query-path])))))

(deftest client-and-server-use-the-versioned-seam
  (let [ground (slurp "src/app/client/workspace/ground.cljs")
        server (slurp "src/app/server_jetty.clj")
        portal (slurp "src/app/client/workspace/face_wiring.cljs")]
    (is (str/includes? ground ":eval (dispatch-key-eval!)"))
    (is (str/includes? ground
                       "(register-verb! :resident/reply-to-block"))
    (is (str/includes? server
                       "reply-to-block/compose-resident-prompt"))
    (is (str/includes? server
                       "reply-to-block/narrowed-portal-open"))
    (is (str/includes? server
                       "invocation-wear-for-source"))
    (is (str/includes? server
                       ":invocation/precontext"))
    (is (str/includes? portal ":release (fn"))
    (is (str/includes? portal "portal->js")
        "release extends the namespace-preserving P7 console convention")))
