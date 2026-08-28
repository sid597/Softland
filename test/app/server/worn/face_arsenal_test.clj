(ns app.server.worn.face-arsenal-test
  "Durable face-arsenal coverage after the browser assembly channel was cut.

   The arsenal module and its stored roster/wear truth remain. These tests use
   its public API directly; they do not recreate the deleted filesystem
   classifier, assembly compiler, importer, or wear-time assembly projection.
   The historical imp:asm routing unit remains because existing durable Object
   Container rows still carry those identities."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-arsenal :as fa]
            [app.server.page.face-projection :as fp]
            [app.server.rama.object-container :as oc])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest historical-assembly-object-key-routing-remains
  (testing "preserved imp:asm keys still route to their historical object-key"
    (let [import-key
          "imp:asm:asm:boxes-w2d-test:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"]
      (is (= "asm:boxes-w2d-test" (oc/extract-object-key import-key)))
      (is (= (oc/partition-by-object-key 4 import-key)
             (oc/partition-by-object-key 4 "asm:boxes-w2d-test")))
      (is (not= import-key (oc/extract-object-key import-key)))
      (is (not= "asm" (oc/extract-object-key import-key)))))
  (testing "existing import families remain unchanged"
    (is (= "abc123" (oc/extract-object-key "imp:md:abc123:deadbeef")))
    (is (= "abc123" (oc/extract-object-key "imp:clj:abc123:deadbeef")))
    (is (= "chat:ffff"
           (oc/extract-object-key "imp:tr:chat:ffff:sb:deadbeef")))
    (is (= "plain" (oc/extract-object-key "plain")))))

(deftest face-projection-remains-read-only
  (testing "face_projection.clj contains no writes, topologies, or raw PState paths"
    (let [source (slurp "src/app/server/page/face_projection.clj")]
      (doseq [forbidden ["defmodule" "<<sources" "local-transform>" "->transform"
                         "foreign-append" "append-object-container-request"
                         "append-relation-request" "declare-depot" "declare-pstate"
                         "stream-topology" "microbatch-topology" "query-topology"
                         "foreign-select" "foreign-pstate" "keypath" "local-select"]]
        (is (not (str/includes? source forbidden))
            (str "must not contain write/topology/raw-path form: " forbidden)))
      (is (= #{"ocr/read-source" "ocr/read-unit" "ocr/read-revision-history"}
             (set (re-seq #"ocr/[a-z\-]+" source)))
          "Object Container reads use named runtime APIs")
      (is (= #{"face-arsenal/read-face" "face-arsenal/list-faces"
               "face-arsenal/read-wear-count"}
             (set (re-seq #"face-arsenal/[a-z\-]+" source)))
          "arsenal reads use its named public surface"))))

(deftest face-list-projection-is-total
  (testing "missing or corrupt arsenal state yields honest empty data"
    (let [missing (fp/face-list-projection {:arsenal-rt nil} {})
          corrupt (fp/face-list-projection
                   {:arsenal-rt {:faces-by-name :not-a-pstate}}
                   {})]
      (is (= [] (:faces missing)))
      (is (= :arsenal-unavailable (:face-list/error missing)))
      (is (= [] (:faces corrupt)))
      (is (= :arsenal-read-failed (:face-list/error corrupt)))))
  (testing "the surviving registry entry dispatches through serve"
    (let [result (fp/serve {:arsenal-rt nil} {:face :face-list})]
      (is (map? result))
      (is (contains? result :faces))))
  (testing "the deleted assembly projection cannot be addressed through serve"
    (let [result (fp/serve {} {:face :assembly :address "preserved"})]
      (is (= :unknown-projection (:conversation/error result)))
      (is (not (contains? result :assembly/source))))))

(defn- scratch-dir!
  [prefix]
  (str (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- register-fixture!
  [runtime face-name valid? status]
  (fa/register-face!
   runtime
   {:face-name face-name
    :object-key (str "asm:" face-name)
    :import-key (str "imp:asm:asm:" face-name ":preserved")
    :status status
    :valid? valid?
    :source-ref (str "preserved://" face-name ".edn")}))

(deftest ^:ipc face-arsenal-roster-wear-and-replay-remain-durable
  (let [scratch (scratch-dir! "face-arsenal")
        wear-log (str scratch "/face-wear-log.ednl")
        arsenal (fa/start-face-arsenal-runtime! {:wear-log-path wear-log})]
    (try
      (register-fixture! arsenal "boxes-w2d-test" true :candidate)
      (register-fixture! arsenal "outline-face" true :active)
      (register-fixture! arsenal "malformed-w2d-test" false :candidate)

      (testing "the surviving roster stores pointers and status, not recipe material"
        (let [row (fa/read-face arsenal "boxes-w2d-test")]
          (is (= "asm:boxes-w2d-test" (:object-key row)))
          (is (= :candidate (:status row)))
          (is (true? (:valid? row)))
          (is (= #{:face-name :object-key :import-key :status :valid? :source-ref
                   :registered-at-ms}
                 (set (keys (into {} row)))))))

      (testing "wears are server-stamped, ordered, counted, and written to the bridge log"
        (let [before-ms (System/currentTimeMillis)
              first-wear
              (fa/record-wear! arsenal {:wear-id "w2d-wear-1"
                                        :face-name "boxes-w2d-test"
                                        :wearer "sid"
                                        :address "chat:w2d-test-conversation"})]
          (is (<= before-ms
                  (:wear/worn-at-ms first-wear)
                  (System/currentTimeMillis)))
          (is (= 1 (:wear-count
                    (fa/read-wear-count arsenal "boxes-w2d-test"))))
          (is (= first-wear
                 (-> wear-log slurp str/split-lines first edn/read-string))))
        (fa/record-wear! arsenal {:wear-id "w2d-wear-2"
                                  :face-name "boxes-w2d-test"
                                  :wearer "sid"
                                  :address "chat:w2d-test-conversation"})
        (let [events (fa/read-wear-events arsenal "boxes-w2d-test")]
          (is (= ["w2d-wear-1" "w2d-wear-2"] (mapv :wear-id events)))
          (is (apply <= (mapv :worn-at-ms events)))))

      (testing "a duplicate wear id is a journaled no-op"
        (let [journaled
              (fa/read-wear-journal-entry arsenal "boxes-w2d-test" "w2d-wear-1")]
          (fa/record-wear! arsenal {:wear-id "w2d-wear-1"
                                    :face-name "boxes-w2d-test"
                                    :wearer "sid"
                                    :address "chat:w2d-test-conversation"})
          (is (= 2 (:wear-count
                    (fa/read-wear-count arsenal "boxes-w2d-test"))))
          (is (= 2 (count (fa/read-wear-events arsenal "boxes-w2d-test"))))
          (is (= journaled
                 (fa/read-wear-journal-entry
                  arsenal "boxes-w2d-test" "w2d-wear-1")))))

      (testing "replay into a fresh module reproduces wear counts and stamps"
        (let [original-events (fa/read-wear-events arsenal "boxes-w2d-test")
              original-count (fa/read-wear-count arsenal "boxes-w2d-test")
              replayed (fa/start-face-arsenal-runtime! {:wear-log-path wear-log})]
          (try
            (is (= {:replayed 3 :failed 0}
                   (fa/replay-wear-log! replayed)))
            (let [count-row (fa/read-wear-count replayed "boxes-w2d-test")
                  events (fa/read-wear-events replayed "boxes-w2d-test")]
              (is (= (:wear-count original-count) (:wear-count count-row)))
              (is (= (:last-worn-ms original-count) (:last-worn-ms count-row)))
              (is (= (mapv (juxt :wear-id :worn-at-ms) original-events)
                     (mapv (juxt :wear-id :worn-at-ms) events))))
            (let [empty-roster (fp/face-list-projection
                                {:arsenal-rt replayed} {})]
              (is (= [] (:faces empty-roster)))
              (is (nil? (:face-list/error empty-roster))))
            (finally
              (fa/close-face-arsenal-runtime! replayed)))))

      (testing "face-list serves the retained roster and wear facts"
        (let [result (fp/serve {:arsenal-rt arsenal}
                               {:face :face-list :params {}})
              by-name (into {} (map (juxt :name identity)) (:faces result))]
          (is (contains? by-name "boxes-w2d-test"))
          (is (contains? by-name "outline-face"))
          (is (false? (:valid? (get by-name "malformed-w2d-test"))))
          (is (= 2 (:wear-count (get by-name "boxes-w2d-test"))))
          (is (pos? (long (:last-worn-ms
                           (get by-name "boxes-w2d-test")))))))
      (finally
        (fa/close-face-arsenal-runtime! arsenal)))))
