(ns app.server.rama.dogfood-world-test
  (:require [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.dogfood.world :as world]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn with-world-runtime
  [f]
  (let [runtime (world/start-world-runtime!)]
    (try
      (f runtime)
      (finally
        (world/close-world-runtime! runtime)))))

(defn append-and-await-decision!
  [runtime request]
  (world/append-world-action! runtime request)
  (world/await-decision runtime (:request/id request)))

(defn append-send-and-await-run!
  [runtime {:keys [world-thread-id world-turn-id bundle-id run-id thread-id request-id]}]
  (let [request (world/compose-and-send-request
                  world-thread-id
                  (str "Prompt for " run-id)
                  {:request-id request-id
                   :time-ms 100
                   :payload {:world-turn/id world-turn-id
                             :context-bundle/id bundle-id
                             :llm-turn-run/id run-id
                             :llm-thread/id thread-id
                             :executor/task-id llm/pending-task-id}})]
    (append-and-await-decision! runtime request)
    (llm/await-run runtime run-id #(= :pending (:status %)))
    request))

(defn append-approval-observation!
  [runtime {:keys [run-id thread-id approval-id native-id]}]
  (llm/append-observation!
    runtime
    (llm/observation
      run-id
      thread-id
      :codex/approval-request
      0
      {:observation-id (str approval-id "/obs")
       :approval/id approval-id
       :approval/type :exec
       :native/json-rpc-request-id native-id
       :codex/event-method "item/cmdExec/requestApproval"
       :codex/event-params {:cmd "echo approval"}}))
  (llm/await-materialized
    #(llm/read-pending-approval runtime approval-id)
    some?))

(defn append-raw-item-observation!
  [runtime {:keys [run-id thread-id item-id text sequence]}]
  (llm/append-observation!
    runtime
    (llm/observation
      run-id
      thread-id
      :codex/item-completed
      sequence
      {:observation-id (str item-id "/obs/" sequence)
       :llm-item/id item-id
       :content/text text}))
  (llm/await-run runtime run-id #(<= (long sequence) (long (:last-seq %))))
  (llm/await-materialized #(llm/read-item-by-id runtime item-id) some?))

(defn relation-targets
  [relations]
  (set (map :to/object-id (vals (:out relations)))))

(defn relation-sources
  [relations]
  (set (map :from/object-id (vals (:in relations)))))

(defn materialized-projection-snapshot
  []
  (with-world-runtime
    (fn [runtime]
      (let [request (world/compose-and-send-request
                      "chat-projection"
                      "Render the projections."
                      {:request-id "req-projection"
                       :time-ms 17
                       :title "Projection chat"
                       :payload {:world-turn/id "WT-projection"
                                 :context-bundle/id "B-projection"
                                 :llm-turn-run/id "run-projection"
                                 :llm-thread/id "llm-thread-projection"
                                 :executor/task-id llm/pending-task-id}})
            _ (append-and-await-decision! runtime request)
            _ (llm/await-run runtime "run-projection" #(= :pending (:status %)))
            thread-object-id (world/catalog-object-id :world-thread "chat-projection")
            turn-object-id (world/catalog-object-id :world-turn "WT-projection")
            bundle-object-id (world/catalog-object-id :context-bundle "B-projection")
            run-object-id (world/catalog-object-id :llm-turn-run "run-projection")]
        {:chat-canvas (world/await-materialized
                        #(world/read-chat-canvas-projection runtime "chat-projection")
                        #(= ["WT-projection"] (:turn-order %)))
         :run-detail (llm/await-materialized
                       #(llm/read-run-detail-projection runtime "run-projection")
                       #(= :pending (:status %)))
         :object-detail {:thread (world/await-materialized
                                   #(world/read-object-detail-projection runtime thread-object-id)
                                   #(= :world-thread (:object/type %)))
                         :turn (world/await-materialized
                                 #(world/read-object-detail-projection runtime turn-object-id)
                                 #(= :world-turn (:object/type %)))
                         :bundle (world/await-materialized
                                   #(world/read-object-detail-projection runtime bundle-object-id)
                                   #(= :context-bundle (:object/type %)))
                         :run (world/await-materialized
                                #(world/read-object-detail-projection runtime run-object-id)
                                #(= :llm-turn-run (:object/type %)))}
         :relations {:thread (world/await-materialized
                               #(world/read-object-relations-projection runtime thread-object-id)
                               #(= #{turn-object-id} (relation-targets %)))
                     :turn (world/await-materialized
                             #(world/read-object-relations-projection runtime turn-object-id)
                             #(and (= #{thread-object-id} (relation-sources %))
                                   (= #{bundle-object-id run-object-id}
                                      (relation-targets %))))
                     :bundle (world/await-materialized
                               #(world/read-object-relations-projection runtime bundle-object-id)
                               #(and (= #{turn-object-id} (relation-sources %))
                                     (= #{run-object-id} (relation-targets %))))
                     :run (world/await-materialized
                            #(world/read-object-relations-projection runtime run-object-id)
                            #(= #{turn-object-id bundle-object-id}
                                (relation-sources %)))}}))))

(deftest world-thread-create-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-thread/create materializes a user-facing WorldThread"
        (let [request (world/world-thread-create-request
                        "chat-A"
                        {:request-id "req-create-chat-A"
                         :time-ms 1
                         :title "Chat A"
                         :actor {:actor/id "sid"
                                 :actor/type :human}})
              decision (append-and-await-decision! runtime request)
              thread (world/await-thread runtime "chat-A" some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= [:world-thread "chat-A"] (:routing/key decision)))
          (is (= ["req-create-chat-A/event/world-thread"] (:event/ids decision)))
          (is (= "Chat A" (:title thread)))
          (is (= :active (:status thread)))
          (is (= 0 (:turn-count thread)))
          (is (= [] (world/read-turns-by-thread runtime "chat-A")))
          (is (= :world-thread/created
                 (:event/type (world/read-event runtime (:event/id decision))))))))))

(deftest compose-and-send-freezes-context-bundle-test
  (with-world-runtime
    (fn [runtime]
      (testing "compose-and-send creates thread, turn, order row, and immutable bundle"
        (let [request (world/compose-and-send-request
                        "chat-B"
                        "Explain the contract."
                        {:request-id "req-send-chat-B"
                         :time-ms 10
                         :title "Contract chat"
                         :payload {:world-turn/id "WT-1"
                                   :context-bundle/id "B-1"
                                   :refs [{:object/id "note-17"}
                                          {:slice/id "slice-3"}]
                                   :llm/options {:agent/kind :codex
                                                 :model "gpt-5.2-codex"
                                                 :approval-policy :on-request
                                                 :sandbox :workspace-write
                                                 :cwd "/mnt/data/projects/Softland"
                                                 :executor/pool :not-bundle-owned}}})
              decision (append-and-await-decision! runtime request)
              thread (world/await-thread runtime "chat-B" #(= 1 (:turn-count %)))
              turn (world/await-turn runtime "WT-1" some?)
              bundle (world/await-materialized
                       #(world/read-context-bundle runtime "B-1")
                       some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= ["req-send-chat-B/event/world-thread"
                  "req-send-chat-B/event/world-turn"
                  "req-send-chat-B/event/context-bundle"
                  "req-send-chat-B/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "Contract chat" (:title thread)))
          (is (= ["WT-1"] (world/read-turns-by-thread runtime "chat-B")))
          (is (= :compose-and-send (:world-turn/kind turn)))
          (is (= "B-1" (:context-bundle/id turn)))
          (is (= "B-1" (world/read-context-bundle-by-turn runtime "WT-1")))
          (is (= {:agent/kind :codex
                  :model "gpt-5.2-codex"
                  :approval-policy :on-request
                  :sandbox :workspace-write
                  :cwd "/mnt/data/projects/Softland"}
                 (:execution/options bundle)))
          (is (not (contains? (:execution/options bundle) :executor/pool)))
          (is (str/starts-with? (:context-bundle/hash bundle) "sha256:"))
          (is (str/includes? (:rendered/model-input bundle)
                             "Explain the contract."))
          (is (str/includes? (:rendered/model-input bundle) "object:note-17"))
          (is (str/includes? (:rendered/model-input bundle) "slice:slice-3")))))))

(deftest world-catalog-materializes-eager-objects-test
  (with-world-runtime
    (fn [runtime]
      (testing "accepted world facts become catalog objects and relation edges"
        (let [request (world/compose-and-send-request
                        "chat-catalog"
                        "Catalog this send."
                        {:request-id "req-catalog"
                         :time-ms 15
                         :title "Catalog chat"
                         :payload {:world-turn/id "WT-catalog"
                                   :context-bundle/id "B-catalog"
                                   :llm-turn-run/id "run-catalog"
                                   :llm-thread/id "llm-thread-catalog"}})
              _decision (append-and-await-decision! runtime request)
              thread-object-id (world/catalog-object-id :world-thread "chat-catalog")
              turn-object-id (world/catalog-object-id :world-turn "WT-catalog")
              bundle-object-id (world/catalog-object-id :context-bundle "B-catalog")
              run-object-id (world/catalog-object-id :llm-turn-run "run-catalog")
              thread-object (world/await-materialized
                              #(world/read-object runtime thread-object-id)
                              some?)
              turn-object (world/await-materialized
                            #(world/read-object runtime turn-object-id)
                            some?)
              bundle-object (world/await-materialized
                              #(world/read-object runtime bundle-object-id)
                              some?)
              run-object (world/await-materialized
                           #(world/read-object runtime run-object-id)
                           some?)
              thread-out (world/await-materialized
                           #(world/read-artifact-graph runtime thread-object-id)
                           #(= #{turn-object-id}
                               (set (map :to/object-id (vals %)))))
              turn-out (world/await-materialized
                         #(world/read-artifact-graph runtime turn-object-id)
                         #(= #{bundle-object-id run-object-id}
                             (set (map :to/object-id (vals %)))))
              bundle-out (world/await-materialized
                           #(world/read-artifact-graph runtime bundle-object-id)
                           #(= #{run-object-id}
                               (set (map :to/object-id (vals %)))))
              run-in (world/await-materialized
                       #(world/read-artifact-graph-in runtime run-object-id)
                       #(= #{turn-object-id bundle-object-id}
                           (set (map :from/object-id (vals %)))))]
          (is (= :world-thread (:object/type thread-object)))
          (is (= :world-turn (:object/type turn-object)))
          (is (= :context-bundle (:object/type bundle-object)))
          (is (= :llm-turn-run (:object/type run-object)))
          (is (= "Catalog chat" (:title thread-object)))
          (is (= "B-catalog" (:context-bundle/id turn-object)))
          (is (= "WT-catalog" (:world-turn/id bundle-object)))
          (is (= "llm-thread-catalog" (:llm-thread/id run-object)))
          (is (= #{turn-object-id}
                 (set (map :to/object-id (vals thread-out)))))
          (is (= #{bundle-object-id run-object-id}
                 (set (map :to/object-id (vals turn-out)))))
          (is (= #{run-object-id}
                 (set (map :to/object-id (vals bundle-out)))))
          (is (= #{turn-object-id bundle-object-id}
                 (set (map :from/object-id (vals run-in))))))))))

(deftest projection-rebuildability-test
  (testing "projection PStates are rebuildable from the same canonical World and LLM inputs"
    (let [snapshot-a (materialized-projection-snapshot)
          snapshot-b (materialized-projection-snapshot)
          chat-canvas (:chat-canvas snapshot-a)
          run-detail (:run-detail snapshot-a)
          thread-detail (get-in snapshot-a [:object-detail :thread])
          turn-relations (get-in snapshot-a [:relations :turn])]
      (is (= snapshot-a snapshot-b))
      (is (= :chat-canvas (:projection/type chat-canvas)))
      (is (= :world-canonical-pstates (:projection/source chat-canvas)))
      (is (= "Projection chat" (:title chat-canvas)))
      (is (= "WT-projection" (get-in chat-canvas [:latest-turn :world-turn/id])))
      (is (= :llm-run-detail (:projection/type run-detail)))
      (is (= :object-detail (:projection/type thread-detail)))
      (is (= :object-relations (:projection/type turn-relations)))
      (is (= #{(world/catalog-object-id :world-thread "chat-projection")}
             (relation-sources turn-relations)))
      (is (= #{(world/catalog-object-id :context-bundle "B-projection")
               (world/catalog-object-id :llm-turn-run "run-projection")}
             (relation-targets turn-relations))))))

(deftest slice-raw-immutability-test
  (with-world-runtime
    (fn [runtime]
      (testing "slice snapshots do not mutate raw LLM items or get rewritten by later observations"
        (let [ids {:world-thread-id "chat-slice-immutability"
                   :world-turn-id "WT-slice-source-send"
                   :bundle-id "B-slice-source"
                   :run-id "run-slice-source"
                   :thread-id "llm-thread-slice-source"
                   :request-id "req-slice-source-send"}
              item-id "item-slice-source"
              original-text "Original raw answer."
              mutated-text "Mutated duplicate answer."
              _ (append-send-and-await-run! runtime ids)
              raw-item (append-raw-item-observation!
                         runtime
                         {:run-id (:run-id ids)
                          :thread-id (:thread-id ids)
                          :item-id item-id
                          :text original-text
                          :sequence 0})
              raw-object-id (world/catalog-object-id :llm-item item-id)
              slice-request (world/world-only-turn-request
                              :world-turn/slice-create
                              (:world-thread-id ids)
                              {:request-id "req-slice-create"
                               :time-ms 210
                               :payload {:world-turn/id "WT-slice-create"
                                         :slice/id "slice-immutability"
                                         :prompt/text "Make this excerpt public."
                                         :source {:llm-item/id item-id
                                                  :llm-turn-run/id (:run-id ids)
                                                  :llm-thread/id (:thread-id ids)
                                                  :content/text original-text
                                                  :content/hash (:content/hash raw-item)}}})]
          (is (nil? (world/read-object runtime raw-object-id)))
          (append-and-await-decision! runtime slice-request)
          (let [slice (world/await-materialized
                        #(world/read-slice runtime "slice-immutability")
                        some?)]
            (append-raw-item-observation!
              runtime
              {:run-id (:run-id ids)
               :thread-id (:thread-id ids)
               :item-id item-id
               :text mutated-text
               :sequence 1})
            (is (= original-text (:snapshot/text slice)))
            (is (= (:content/hash raw-item) (:source/content-hash slice)))
            (is (= original-text
                   (:content/text (llm/read-item-by-id runtime item-id))))
            (is (= original-text
                   (get-in (llm/read-items-by-run runtime (:run-id ids))
                           [item-id :content/text])))
            (is (= :llm-item
                   (:object/type
                    (world/await-materialized
                      #(world/read-object runtime raw-object-id)
                      some?))))))))))

(deftest slice-source-hash-test
  (with-world-runtime
    (fn [runtime]
      (testing "a slice records both its snapshot hash and the source content hash"
        (let [create-request (world/world-thread-create-request
                               "chat-slice-hash"
                               {:request-id "req-slice-hash-thread"
                                :time-ms 220})
              source-hash (world/content-hash "larger source material")
              slice-request (world/world-only-turn-request
                              :world-turn/slice-create
                              "chat-slice-hash"
                              {:request-id "req-slice-hash"
                               :time-ms 221
                               :payload {:world-turn/id "WT-slice-hash"
                                         :slice/id "slice-hash"
                                         :snapshot/text "selected source"
                                         :source {:llm-item/id "item-slice-hash"
                                                  :content/hash source-hash}}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime slice-request)
          (let [slice (world/await-materialized
                        #(world/read-slice runtime "slice-hash")
                        some?)]
            (is (= source-hash (:source/content-hash slice)))
            (is (= (world/content-hash "selected source") (:snapshot/hash slice)))
            (is (not= (:source/content-hash slice) (:snapshot/hash slice)))))))))

(deftest derivative-renders-as-user-authored-test
  (with-world-runtime
    (fn [runtime]
      (testing "derivatives are stored and rendered as user-authored material"
        (let [create-request (world/world-thread-create-request
                               "chat-derivative"
                               {:request-id "req-derivative-thread"
                                :time-ms 230})
              derivative-request (world/world-only-turn-request
                                   :world-turn/derivative-create
                                   "chat-derivative"
                                   {:request-id "req-derivative"
                                    :time-ms 231
                                    :actor {:actor/id "sid"
                                            :actor/type :human}
                                    :payload {:world-turn/id "WT-derivative"
                                              :derivative/id "derivative-user"
                                              :content/text "My rewritten understanding."
                                              :source {:llm-item/id "item-derivative-source"
                                                       :content/text "model draft"}}})
              send-request (world/compose-and-send-request
                             "chat-derivative"
                             "Use this derivative."
                             {:request-id "req-derivative-send"
                              :time-ms 232
                              :payload {:world-turn/id "WT-derivative-send"
                                        :context-bundle/id "B-derivative-send"
                                        :llm-turn-run/id "run-derivative-send"
                                        :refs [{:derivative/id "derivative-user"}]}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime derivative-request)
          (let [derivative (world/await-materialized
                             #(world/read-derivative runtime "derivative-user")
                             some?)
                derivative-object (world/await-materialized
                                    #(world/read-object
                                       runtime
                                       (world/catalog-object-id :derivative "derivative-user"))
                                    some?)]
            (is (= :user (:authorship derivative)))
            (is (= :user-authored (:render/as derivative)))
            (is (= :user (:authorship derivative-object))))
          (append-and-await-decision! runtime send-request)
          (let [bundle (world/await-materialized
                         #(world/read-context-bundle runtime "B-derivative-send")
                         some?)]
            (is (str/includes? (:rendered/model-input bundle)
                               "user-authored-derivative:derivative-user"))))))))

(deftest world-only-turn-no-llm-side-effect-test
  (with-world-runtime
    (fn [runtime]
      (testing "slice, comment, and derivative turns stay world-only"
        (let [create-request (world/world-thread-create-request
                               "chat-world-only-material"
                               {:request-id "req-world-only-material-thread"
                                :time-ms 240})
              slice-request (world/world-only-turn-request
                              :world-turn/slice-create
                              "chat-world-only-material"
                              {:request-id "req-world-only-slice"
                               :time-ms 241
                               :payload {:world-turn/id "WT-world-only-slice"
                                         :slice/id "slice-world-only"
                                         :snapshot/text "world only slice"
                                         :source {:llm-item/id "item-world-only"
                                                  :content/text "raw"}}})
              comment-request (world/world-only-turn-request
                                :world-turn/comment-create
                                "chat-world-only-material"
                                {:request-id "req-world-only-comment"
                                 :time-ms 242
                                 :payload {:world-turn/id "WT-world-only-comment"
                                           :overlay/id "overlay-world-only"
                                           :prompt/text "A note on the raw item."
                                           :source {:llm-item/id "item-world-only"
                                                    :content/text "raw"}}})
              derivative-request (world/world-only-turn-request
                                   :world-turn/derivative-create
                                   "chat-world-only-material"
                                   {:request-id "req-world-only-derivative"
                                    :time-ms 243
                                    :payload {:world-turn/id "WT-world-only-derivative"
                                              :derivative/id "derivative-world-only"
                                              :content/text "user rewrite"
                                              :source {:llm-item/id "item-world-only"
                                                       :content/text "raw"}}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime slice-request)
          (append-and-await-decision! runtime comment-request)
          (append-and-await-decision! runtime derivative-request)
          (is (some? (world/await-materialized
                       #(world/read-slice runtime "slice-world-only")
                       some?)))
          (is (some? (world/await-materialized
                       #(world/read-overlay runtime "overlay-world-only")
                       some?)))
          (is (some? (world/await-materialized
                       #(world/read-derivative runtime "derivative-world-only")
                       some?)))
          (is (nil? (world/read-context-bundle-by-turn runtime "WT-world-only-slice")))
          (is (nil? (world/read-llm-run-by-turn runtime "WT-world-only-slice")))
          (is (nil? (world/read-context-bundle-by-turn runtime "WT-world-only-comment")))
          (is (nil? (world/read-llm-run-by-turn runtime "WT-world-only-comment")))
          (is (nil? (world/read-context-bundle-by-turn runtime "WT-world-only-derivative")))
          (is (nil? (world/read-llm-run-by-turn runtime "WT-world-only-derivative")))
          (is (empty? (llm/read-pending runtime llm/pending-task-id))))))))

(deftest fork-span-softland-anchor-test
  (with-world-runtime
    (fn [runtime]
      (testing "fork-from-span anchors a child world on a reusable slice and Codex thread fork"
        (let [parent-thread-id "chat-fork-parent"
              child-thread-id "chat-fork-child"
              parent-native-thread-id "codex-native-parent"
              child-native-thread-id "codex-native-child"
              source-text "The span that becomes its own local world."
              source-hash (world/content-hash source-text)
              create-parent (world/world-thread-create-request
                              parent-thread-id
                              {:request-id "req-fork-parent"
                               :time-ms 250
                               :title "Fork parent"})
              fork-request (world/world-action-request
                             :world-thread/fork-from-span
                             child-thread-id
                             {:request-id "req-fork-child"
                              :time-ms 251
                              :title "Fork child"
                              :payload {:parent-thread/id parent-thread-id
                                        :world-turn/id "WT-fork-child"
                                        :context-bundle/id "B-fork-child"
                                        :slice/id "slice-fork-anchor"
                                        :llm-turn-run/id "run-fork-child"
                                        :llm-thread/id "llm-thread-fork-child"
                                        :prompt/text "Explore the forked span."
                                        :refs [{:slice/id "slice-fork-anchor"}]
                                        :source {:llm-item/id "item-fork-source"
                                                 :content/text source-text
                                                 :content/hash source-hash}
                                        :fork/from-native-thread-id parent-native-thread-id
                                        :native/codex-thread-id child-native-thread-id
                                        :executor/task-id llm/pending-task-id}})]
          (append-and-await-decision! runtime create-parent)
          (let [decision (append-and-await-decision! runtime fork-request)
                child-thread (world/await-thread runtime child-thread-id #(= 1 (:turn-count %)))
                slice (world/await-materialized
                        #(world/read-slice runtime "slice-fork-anchor")
                        some?)
                bundle (world/await-materialized
                         #(world/read-context-bundle runtime "B-fork-child")
                         some?)
                llm-request (world/await-materialized
                              #(world/read-llm-run-request runtime "run-fork-child")
                              some?)
                llm-run (llm/await-run runtime "run-fork-child" #(= :pending (:status %)))]
            (is (= :accepted (:decision/status decision)))
            (is (= parent-thread-id (:parent-thread/id child-thread)))
            (is (contains? (world/read-thread-graph runtime parent-thread-id)
                           child-thread-id))
            (is (= source-text (:snapshot/text slice)))
            (is (= source-hash (:source/content-hash slice)))
            (is (str/includes? (:rendered/model-input bundle)
                               "slice:slice-fork-anchor"))
            (is (= parent-native-thread-id
                   (get-in llm-request [:executor :fork/from-native-thread-id])))
            (is (= child-native-thread-id
                   (get-in llm-request [:executor :native/thread-id])))
            (is (= parent-native-thread-id (:fork/from-native-thread-id llm-run)))
            (is (= child-native-thread-id (:native/codex-thread-id llm-run)))))))))

(deftest fork-binding-before-turn-start-test
  (with-world-runtime
    (fn [runtime]
      (testing "the executor does not start a forked child turn until native binding is durable"
        (let [parent-thread-id "chat-fork-wait-parent"
              child-thread-id "chat-fork-wait-child"
              adapter-called? (atom false)
              create-parent (world/world-thread-create-request
                              parent-thread-id
                              {:request-id "req-fork-wait-parent"
                               :time-ms 260})
              fork-request (world/world-action-request
                             :world-thread/fork-from-span
                             child-thread-id
                             {:request-id "req-fork-wait-child"
                              :time-ms 261
                              :payload {:parent-thread/id parent-thread-id
                                        :world-turn/id "WT-fork-wait-child"
                                        :context-bundle/id "B-fork-wait-child"
                                        :slice/id "slice-fork-wait"
                                        :llm-turn-run/id "run-fork-wait-child"
                                        :llm-thread/id "llm-thread-fork-wait-child"
                                        :prompt/text "Wait for fork binding."
                                        :source {:llm-item/id "item-fork-wait"
                                                 :content/text "fork wait source"}
                                        :fork/from-native-thread-id "codex-native-parent-wait"
                                        :executor/task-id llm/pending-task-id}})]
          (append-and-await-decision! runtime create-parent)
          (append-and-await-decision! runtime fork-request)
          (llm/await-run runtime "run-fork-wait-child" #(= :pending (:status %)))
          (let [result (llm/run-one-pending-with-adapter!
                         runtime
                         {:executor-id "executor-fork-wait"
                          :adapter (fn [_ctx]
                                     (reset! adapter-called? true)
                                     [])})
                run (llm/read-run runtime "run-fork-wait-child")]
            (is (false? (:spawned? result)))
            (is (= :fork-binding-not-durable (:reason result)))
            (is (false? @adapter-called?))
            (is (= :pending (:status run)))
            (is (nil? (:claimed-by run)))))))))

(deftest patch-proposal-creation-test
  (with-world-runtime
    (fn [runtime]
      (testing "patch-like LLM observations become pending world proposals"
        (let [ids {:world-thread-id "chat-patch-proposal"
                   :world-turn-id "WT-patch-source"
                   :bundle-id "B-patch-source"
                   :run-id "run-patch-proposal"
                   :thread-id "llm-thread-patch-proposal"
                   :request-id "req-patch-source"}
              proposal-id "patch-proposal-1"
              _ (append-send-and-await-run! runtime ids)]
          (world/append-llm-observation!
            runtime
            (llm/observation
              (:run-id ids)
              (:thread-id ids)
              :codex/patch-proposal
              0
              {:observation-id "obs-patch-proposal-1"
               :world-thread/id (:world-thread-id ids)
               :world-turn/id (:world-turn-id ids)
               :patch-proposal/id proposal-id
               :turn-diff/id "turn-diff-1"
               :summary/text "Update the parser."
               :patch/files [{:path "src/app/parser.clj"
                              :hunks 2}]}))
          (let [proposal (world/await-materialized
                           #(world/read-patch-proposal runtime proposal-id)
                           some?)]
            (is (= :pending (:status proposal)))
            (is (= (:run-id ids) (:llm-turn-run/id proposal)))
            (is (= (:world-thread-id ids) (:world-thread/id proposal)))
            (is (= "Update the parser." (:summary/text proposal)))
            (is (= [{:path "src/app/parser.clj" :hunks 2}]
                   (:patch/files proposal)))))))))

(deftest patch-accept-reject-world-turns-test
  (with-world-runtime
    (fn [runtime]
      (testing "patch acceptance and rejection are world turns, not tool approvals"
        (let [ids {:world-thread-id "chat-patch-resolution"
                   :world-turn-id "WT-patch-resolution-source"
                   :bundle-id "B-patch-resolution-source"
                   :run-id "run-patch-resolution"
                   :thread-id "llm-thread-patch-resolution"
                   :request-id "req-patch-resolution-source"}
              accept-id "patch-proposal-accept"
              reject-id "patch-proposal-reject"
              _ (append-send-and-await-run! runtime ids)]
          (doseq [[proposal-id sequence] [[accept-id 0] [reject-id 1]]]
            (world/append-llm-observation!
              runtime
              (llm/observation
                (:run-id ids)
                (:thread-id ids)
                :codex/patch-proposal
                sequence
                {:observation-id (str "obs-" proposal-id)
                 :world-thread/id (:world-thread-id ids)
                 :world-turn/id (:world-turn-id ids)
                 :patch-proposal/id proposal-id
                 :summary/text proposal-id})))
          (world/await-materialized
            #(world/read-patch-proposal runtime accept-id)
            #(= :pending (:status %)))
          (world/await-materialized
            #(world/read-patch-proposal runtime reject-id)
            #(= :pending (:status %)))
          (let [accept-request (world/world-only-turn-request
                                 :world-turn/patch-accept
                                 (:world-thread-id ids)
                                 {:request-id "req-patch-accept"
                                  :time-ms 270
                                  :payload {:world-turn/id "WT-patch-accept"
                                            :patch-proposal/id accept-id
                                            :prompt/text "Accept this patch."}})
                reject-request (world/world-only-turn-request
                                 :world-turn/patch-reject
                                 (:world-thread-id ids)
                                 {:request-id "req-patch-reject"
                                  :time-ms 271
                                  :payload {:world-turn/id "WT-patch-reject"
                                            :patch-proposal/id reject-id
                                            :prompt/text "Reject this patch."
                                            :reason :not-right-shape}})
                accept-decision (append-and-await-decision! runtime accept-request)
                reject-decision (append-and-await-decision! runtime reject-request)
                accepted (world/await-materialized
                           #(world/read-patch-proposal runtime accept-id)
                           #(= :accepted (:status %)))
                rejected (world/await-materialized
                           #(world/read-patch-proposal runtime reject-id)
                           #(= :rejected (:status %)))]
            (is (= :accepted (:decision/status accept-decision)))
            (is (= :accepted (:decision/status reject-decision)))
            (is (= "WT-patch-accept" (:resolution/world-turn-id accepted)))
            (is (= "WT-patch-reject" (:resolution/world-turn-id rejected)))
            (is (= :not-right-shape (:reason rejected)))
            (is (nil? (:llm-control/type accept-decision)))
            (is (nil? (:llm-control/type reject-decision)))
            (is (nil? (world/read-llm-control-by-turn runtime "WT-patch-accept")))
            (is (nil? (world/read-llm-control-by-turn runtime "WT-patch-reject")))))))))

(deftest world-only-turn-no-context-bundle-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-only turns are ordered material but do not freeze a model input"
        (let [create-request (world/world-thread-create-request
                               "chat-C"
                               {:request-id "req-create-chat-C"
                                :time-ms 20})
              comment-request (world/world-only-turn-request
                                :world-turn/comment-create
                                "chat-C"
                                {:request-id "req-comment-chat-C"
                                 :time-ms 21
                                 :payload {:world-turn/id "WT-comment"
                                           :prompt/text "Actually, pin this nuance."
                                           :refs [{:world-turn/id "WT-older"}]}})]
          (append-and-await-decision! runtime create-request)
          (let [decision (append-and-await-decision! runtime comment-request)
                turn (world/await-turn runtime "WT-comment" some?)
                thread (world/await-thread runtime "chat-C" #(= 1 (:turn-count %)))]
            (is (= :accepted (:decision/status decision)))
            (is (= ["req-comment-chat-C/event/world-turn"] (:event/ids decision)))
            (is (= :world-turn/comment-create (:world-turn/kind turn)))
            (is (= "Actually, pin this nuance." (:prompt/text turn)))
            (is (= ["WT-comment"] (world/read-turns-by-thread runtime "chat-C")))
            (is (= 1 (:turn-count thread)))
            (is (nil? (world/read-context-bundle-by-turn runtime "WT-comment")))))))))

(deftest world-only-turn-requires-thread-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-only turns cannot invent their containing WorldThread"
        (let [request (world/world-only-turn-request
                        :world-turn/comment-create
                        "missing-chat"
                        {:request-id "req-comment-missing"
                         :time-ms 30
                         :payload {:world-turn/id "WT-missing"
                                   :prompt/text "No container."}})
              decision (append-and-await-decision! runtime request)]
          (is (= :rejected (:decision/status decision)))
          (is (= :world-thread/not-found (:decision/reason decision)))
          (is (nil? (world/read-turn runtime "WT-missing"))))))))

(deftest world-first-send-test
  (with-world-runtime
    (fn [runtime]
      (testing "the user send enters World first and World derives the LLM run request"
        (let [request (world/compose-and-send-request
                        "chat-world-first"
                        "Start from the world."
                        {:request-id "req-world-first"
                         :time-ms 40
                         :idempotency-key "idem-world-first"
                         :payload {:world-turn/id "WT-world-first"
                                   :context-bundle/id "B-world-first"
                                   :llm-turn-run/id "run-world-first"
                                   :llm-thread/id "llm-thread-world-first"
                                   :executor/task-id llm/pending-task-id}})
              decision (append-and-await-decision! runtime request)
              llm-request (world/await-materialized
                            #(world/read-llm-run-request runtime "run-world-first")
                            some?)
              llm-decision (llm/await-decision runtime "run-world-first")
              llm-run (llm/await-run runtime "run-world-first"
                                     #(= :pending (:status %)))]
          (is (= :accepted (:decision/status decision)))
          (is (= "WT-world-first" (:world-turn/id decision)))
          (is (= "B-world-first" (:context-bundle/id decision)))
          (is (= "run-world-first" (:llm-turn-run/id decision)))
          (is (= :llm/turn-run-request (:request/type llm-request)))
          (is (= "WT-world-first" (:world-turn/id llm-request)))
          (is (= "B-world-first" (:context-bundle/id llm-request)))
          (is (= "req-world-first/llm-request" (:request/id llm-request)))
          (is (= :accepted (:decision/status llm-decision)))
          (is (= "B-world-first" (:context-bundle/id llm-run)))
          (is (contains?
                (llm/await-materialized
                  #(llm/read-pending runtime llm/pending-task-id)
                  #(contains? % "run-world-first"))
                "run-world-first")))))))

(deftest context-bundle-before-run-test
  (with-world-runtime
    (fn [runtime]
      (testing "World materializes the bundle that the bridged LLM run points at"
        (let [request (world/compose-and-send-request
                        "chat-bundle-before-run"
                        "Freeze this before execution."
                        {:request-id "req-bundle-before-run"
                         :time-ms 50
                         :payload {:world-turn/id "WT-bundle-before-run"
                                   :context-bundle/id "B-bundle-before-run"
                                   :llm-turn-run/id "run-bundle-before-run"}})
              decision (append-and-await-decision! runtime request)
              bundle (world/await-materialized
                       #(world/read-context-bundle runtime "B-bundle-before-run")
                       some?)
              llm-run (llm/await-run runtime "run-bundle-before-run"
                                     #(= :pending (:status %)))]
          (is (= "B-bundle-before-run" (:context-bundle/id bundle)))
          (is (= (:context-bundle/hash bundle) (:context-bundle/hash decision)))
          (is (= (:context-bundle/id bundle) (:context-bundle/id llm-run)))
          (is (= "run-bundle-before-run"
                 (world/read-llm-run-by-turn runtime "WT-bundle-before-run"))))))))

(deftest one-bundle-per-run-test
  (with-world-runtime
    (fn [runtime]
      (testing "one compose-and-send creates exactly one bundle and one run"
        (let [request (world/compose-and-send-request
                        "chat-one-bundle"
                        "One run, one bundle."
                        {:request-id "req-one-bundle"
                         :time-ms 60
                         :payload {:world-turn/id "WT-one-bundle"
                                   :context-bundle/id "B-one-bundle"
                                   :llm-turn-run/id "run-one-bundle"}})
              decision (append-and-await-decision! runtime request)
              bundle-id (world/await-materialized
                          #(world/read-context-bundle-by-turn runtime "WT-one-bundle")
                          some?)
              run-id (world/await-materialized
                       #(world/read-llm-run-by-turn runtime "WT-one-bundle")
                       some?)
              llm-run (llm/await-run runtime "run-one-bundle"
                                     #(= :pending (:status %)))]
          (is (= "B-one-bundle" bundle-id))
          (is (= "run-one-bundle" run-id))
          (is (= "B-one-bundle" (:context-bundle/id llm-run)))
          (is (= "run-one-bundle" (:llm-turn-run/id decision)))
          (is (= ["WT-one-bundle"] (world/read-turns-by-thread runtime "chat-one-bundle"))))))))

(deftest accepted-decision-fields-test
  (with-world-runtime
    (fn [runtime]
      (testing "accepted compose decisions expose the sibling facts needed by projections"
        (let [request (world/compose-and-send-request
                        "chat-decision-fields"
                        "Expose the accepted fields."
                        {:request-id "req-decision-fields"
                         :time-ms 70
                         :payload {:world-turn/id "WT-decision-fields"
                                   :context-bundle/id "B-decision-fields"
                                   :llm-turn-run/id "run-decision-fields"
                                   :llm-thread/id "llm-thread-decision-fields"}})
              decision (append-and-await-decision! runtime request)]
          (is (= :accepted (:decision/status decision)))
          (is (= "req-decision-fields/event/world-turn" (:event/id decision)))
          (is (= ["req-decision-fields/event/world-thread"
                  "req-decision-fields/event/world-turn"
                  "req-decision-fields/event/context-bundle"
                  "req-decision-fields/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "chat-decision-fields" (:world-thread/id decision)))
          (is (= "WT-decision-fields" (:world-turn/id decision)))
          (is (= "B-decision-fields" (:context-bundle/id decision)))
          (is (str/starts-with? (:context-bundle/hash decision) "sha256:"))
          (is (= "llm-thread-decision-fields" (:llm-thread/id decision)))
          (is (= "run-decision-fields" (:llm-turn-run/id decision)))
          (is (= "req-decision-fields/llm-request" (:llm/request-id decision))))))))

(deftest idempotency-test
  (with-world-runtime
    (fn [runtime]
      (testing "replaying the same send key does not mint duplicate turns, bundles, or runs"
        (let [request-a (world/compose-and-send-request
                          "chat-idempotent"
                          "Send once."
                          {:request-id "req-idem-A"
                           :time-ms 80
                           :idempotency-key "idem-same-send"
                           :payload {:world-turn/id "WT-idem-A"
                                     :context-bundle/id "B-idem-A"
                                     :llm-turn-run/id "run-idem-A"
                                     :llm-thread/id "llm-thread-idem"}})
              request-b (world/compose-and-send-request
                          "chat-idempotent"
                          "Send once, replayed."
                          {:request-id "req-idem-B"
                           :time-ms 81
                           :idempotency-key "idem-same-send"
                           :payload {:world-turn/id "WT-idem-B"
                                     :context-bundle/id "B-idem-B"
                                     :llm-turn-run/id "run-idem-B"
                                     :llm-thread/id "llm-thread-idem"}})
              decision-a (append-and-await-decision! runtime request-a)
              _ (llm/await-run runtime "run-idem-A" #(= :pending (:status %)))
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :accepted (:decision/status decision-a)))
          (is (= :accepted (:decision/status decision-b)))
          (is (:idempotency/replayed? decision-b))
          (is (= (:event/ids decision-a) (:event/ids decision-b)))
          (is (= "WT-idem-A" (:world-turn/id decision-b)))
          (is (= "B-idem-A" (:context-bundle/id decision-b)))
          (is (= "run-idem-A" (:llm-turn-run/id decision-b)))
          (is (= ["WT-idem-A"] (world/read-turns-by-thread runtime "chat-idempotent")))
          (is (= "B-idem-A" (world/read-context-bundle-by-turn runtime "WT-idem-A")))
          (is (nil? (world/read-context-bundle-by-turn runtime "WT-idem-B")))
          (is (some? (world/read-context-bundle runtime "B-idem-A")))
          (is (nil? (world/read-context-bundle runtime "B-idem-B")))
          (is (some? (world/read-llm-run-request runtime "run-idem-A")))
          (is (nil? (world/read-llm-run-request runtime "run-idem-B")))
          (is (some? (llm/read-run runtime "run-idem-A")))
          (is (nil? (llm/read-run runtime "run-idem-B")))
          (is (= "run-idem-A"
                 (:llm-turn-run/id
                  (world/read-send-by-idempotency runtime "idem-same-send")))))))))

(deftest approval-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "approval resolution enters World and derives an LLM control record"
        (let [ids {:world-thread-id "chat-approval"
                   :world-turn-id "WT-approval-send"
                   :bundle-id "B-approval"
                   :run-id "run-approval-world-first"
                   :thread-id "llm-thread-approval"
                   :request-id "req-approval-send"}
              approval-id "approval-world-first"
              native-id 44
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id native-id})
              control-request (world/world-only-turn-request
                                :world-turn/tool-approval-resolve
                                (:world-thread-id ids)
                                {:request-id "req-approval-resolve"
                                 :time-ms 110
                                 :payload {:world-turn/id "WT-approval-resolve"
                                           :llm-turn-run/id (:run-id ids)
                                           :approval/id approval-id
                                           :native/json-rpc-request-id native-id
                                           :decision :approved}})
              decision (append-and-await-decision! runtime control-request)
              run (llm/await-run runtime (:run-id ids) #(= :running (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-approval-resolve/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= "WT-approval-resolve" (:world-turn/id decision)))
          (is (= "req-approval-resolve/llm-control"
                 (world/read-llm-control-by-turn runtime "WT-approval-resolve")))
          (is (= :approval/resolve (:control/type control)))
          (is (= native-id (:native/json-rpc-request-id control)))
          (is (= :running (:status run)))
          (is (= :approved (:status approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?))))))))

(deftest approval-is-not-patch-acceptance-test
  (with-world-runtime
    (fn [runtime]
      (testing "tool approval resolution is separate from patch acceptance"
        (let [ids {:world-thread-id "chat-approval-not-patch"
                   :world-turn-id "WT-approval-not-patch-send"
                   :bundle-id "B-approval-not-patch"
                   :run-id "run-approval-not-patch"
                   :thread-id "llm-thread-approval-not-patch"
                   :request-id "req-approval-not-patch-send"}
              approval-id "approval-not-patch"
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 55})
              approval-request (world/world-only-turn-request
                                 :world-turn/tool-approval-resolve
                                 (:world-thread-id ids)
                                 {:request-id "req-approval-not-patch"
                                  :time-ms 120
                                  :payload {:world-turn/id "WT-approval-not-patch"
                                            :llm-turn-run/id (:run-id ids)
                                            :approval/id approval-id
                                            :native/json-rpc-request-id 55
                                            :decision :approved}})
              patch-request (world/world-only-turn-request
                              :world-turn/patch-accept
                              (:world-thread-id ids)
                              {:request-id "req-patch-accept-not-approval"
                               :time-ms 121
                               :payload {:world-turn/id "WT-patch-accept"
                                         :prompt/text "accept patch"}})
              approval-decision (append-and-await-decision! runtime approval-request)
              patch-decision (append-and-await-decision! runtime patch-request)]
          (is (= :approval/resolve (:llm-control/type approval-decision)))
          (is (= :world-turn/tool-approval-resolve
                 (:world-turn/kind (world/read-turn runtime "WT-approval-not-patch"))))
          (is (= :accepted (:decision/status patch-decision)))
          (is (= :world-turn/patch-accept
                 (:world-turn/kind (world/read-turn runtime "WT-patch-accept"))))
          (is (nil? (:llm-control/type patch-decision)))
          (is (nil? (world/read-llm-control-by-turn runtime "WT-patch-accept"))))))))

(deftest cancel-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "cancel enters World first and derives an LLM cancel control"
        (let [ids {:world-thread-id "chat-cancel"
                   :world-turn-id "WT-cancel-send"
                   :bundle-id "B-cancel"
                   :run-id "run-cancel-world-first"
                   :thread-id "llm-thread-cancel"
                   :request-id "req-cancel-send"}
              _ (append-send-and-await-run! runtime ids)
              request (world/world-only-turn-request
                        :world-turn/cancel
                        (:world-thread-id ids)
                        {:request-id "req-cancel"
                         :time-ms 130
                         :payload {:world-turn/id "WT-cancel"
                                   :llm-turn-run/id (:run-id ids)
                                   :reason :user-request}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :cancelled (:status %)))
              control (llm/await-materialized
                        #(llm/read-control runtime "req-cancel/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :turn/cancel (:llm-control/type decision)))
          (is (= :turn/cancel (:control/type control)))
          (is (= :cancelled (:status run)))
          (is (nil? (llm/await-materialized
                      #(get (llm/read-pending runtime llm/pending-task-id)
                            (:run-id ids))
                      nil?))))))))

(deftest compaction-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "compaction request enters World first and records an LLM control"
        (let [ids {:world-thread-id "chat-compact"
                   :world-turn-id "WT-compact-send"
                   :bundle-id "B-compact"
                   :run-id "run-compact-world-first"
                   :thread-id "llm-thread-compact"
                   :request-id "req-compact-send"}
              _ (append-send-and-await-run! runtime ids)
              request (world/world-only-turn-request
                        :world-turn/compact-request
                        (:world-thread-id ids)
                        {:request-id "req-compact"
                         :time-ms 140
                         :payload {:world-turn/id "WT-compact"
                                   :llm-turn-run/id (:run-id ids)
                                   :strategy :summarize-prefix}})
              decision (append-and-await-decision! runtime request)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-compact/llm-control")
                        some?)
              run (llm/read-run runtime (:run-id ids))]
          (is (= :accepted (:decision/status decision)))
          (is (= :compact/request (:llm-control/type decision)))
          (is (= :compact/request (:control/type control)))
          (is (= :pending (:status run)))
          (is (= [{:control/id "req-compact/llm-control"
                   :time-ms 140
                   :actor {:actor/id "system" :actor/type :system}
                   :payload {:world-thread/id "chat-compact"
                             :world-turn/id "WT-compact"
                             :llm-turn-run/id "run-compact-world-first"
                             :strategy :summarize-prefix}}]
                 (:compactions run))))))))

(deftest approval-timeout-test
  (with-world-runtime
    (fn [runtime]
      (testing "approval timeout is durably recorded as an expired approval"
        (let [ids {:world-thread-id "chat-timeout"
                   :world-turn-id "WT-timeout-send"
                   :bundle-id "B-timeout"
                   :run-id "run-approval-timeout"
                   :thread-id "llm-thread-timeout"
                   :request-id "req-timeout-send"}
              approval-id "approval-timeout"
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 66})
              request (world/world-only-turn-request
                        :world-turn/tool-approval-resolve
                        (:world-thread-id ids)
                        {:request-id "req-approval-timeout"
                         :time-ms 150
                         :payload {:world-turn/id "WT-approval-timeout"
                                   :llm-turn-run/id (:run-id ids)
                                   :approval/id approval-id
                                   :native/json-rpc-request-id 66
                                   :decision :expired
                                   :reason :timeout}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :failed (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= :expired (:status approval)))
          (is (= :expired (:decision approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?)))
          (is (= :approval/declined (get-in run [:error :reason])))
          (is (= :expired (get-in run [:error :decision]))))))))
