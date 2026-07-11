(ns app.face-gate-fixes-test
  "W2 G26 gate-fix regressions (2026-07-11). One test per falsification
   finding fixed at the gate — each would FAIL against the pre-fix code:
   - arsenal HIGH: non-numeric worn-at-ms must die at the ingress guard
     (post-guard it throws in wear-order-key and wedges the partition);
   - adapter HIGH: edge idempotency keys are STABLE across imports (the
     import-key component defeated the rk journal on every edited save);
   - adapter HIGH: rename ghosts — the unregister event removes roster rows;
   - adapter MED: provenance targets join across name and object-key forms;
   - server INT: face→projection resolution rides the arsenal roster;
   - adapter LOW: unicode whitespace rejected in face names."
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-arsenal :as fa]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.object-container.assembly-adapter :as aa]))

(deftest guard-rejects-non-numeric-worn-at-ms
  (testing "the poison-event class dies at the guard, never inside the topology"
    (let [base {:event/type :face/wear :face/name "boxes-face" :wear/id "w1"}]
      (is (fa/valid-face-event? (assoc base :wear/worn-at-ms 123)))
      (is (not (fa/valid-face-event? (assoc base :wear/worn-at-ms "123"))))
      (is (not (fa/valid-face-event? (assoc base :wear/worn-at-ms :now))))
      (is (not (fa/valid-face-event? (assoc base :wear/worn-at-ms {:t 1}))))
      (is (not (fa/valid-face-event? (assoc base :wear/worn-at-ms nil)))))
    (is (fa/valid-face-event? {:event/type :face/unregistered :face/name "x"})
        "the unregister event passes the guard")
    (is (not (fa/valid-face-event? {:event/type :face/unregistered :face/name ""}))
        "blank name still rejected")))

(deftest edge-idempotency-key-stable-across-imports
  (testing "same provenance, different import-key (an edited save) → SAME key"
    (let [identity-1 {:object-key "asm:grid" :document-id "oc:doc:asm:grid"
                      :import-key "imp:asm:asm:grid:sha-A" :created-at 1
                      :provenance {:based-on "outline-face"}}
          identity-2 (assoc identity-1 :import-key "imp:asm:asm:grid:sha-B"
                            :created-at 2)
          [spec-1] (aa/edge-specs identity-1)
          [spec-2] (aa/edge-specs identity-2)]
      (is (= (:idempotency-key spec-1) (:idempotency-key spec-2))
          "edited re-import dedups at the rk journal, not a racy read-back")
      (is (= (:request-id spec-1) (:request-id spec-2)))
      (is (not= (:note spec-1) (:note spec-2))
          "the import-key stays visible in the note (provenance)"))))

(deftest edge-targets-join-across-name-and-object-key-forms
  (testing "§17 sanctions both forms; they must resolve to ONE target"
    (let [by-name (aa/edge-specs {:object-key "asm:a" :document-id "oc:doc:asm:a"
                                  :import-key "k" :created-at 1
                                  :provenance {:based-on "foo"}})
          by-key  (aa/edge-specs {:object-key "asm:a" :document-id "oc:doc:asm:a"
                                  :import-key "k" :created-at 1
                                  :provenance {:based-on "asm:foo"}})]
      (is (= (:to (first by-name)) (:to (first by-key)))
          "face name and face object-key mint the SAME doc-container target"))))

(deftest projection-resolution-rides-the-roster
  (testing "a registered face routes to :conversation; unknown stays honest"
    (with-redefs [fa/read-face (fn [_ n] (when (= n "boxes-face")
                                           {:face-name n :object-key "asm:boxes-face"}))]
      (let [ctx {:arsenal-rt ::stub}]
        (is (= :conversation (fp/resolve-projection-kind ctx :boxes-face))
            "keyword form of a registered face")
        (is (= :conversation (fp/resolve-projection-kind ctx "boxes-face"))
            "string form of a registered face")
        (is (= :conversation (fp/resolve-projection-kind ctx :outline))
            "W1 static entry still routes")
        (is (= :assembly (fp/resolve-projection-kind ctx :assembly))
            "direct projection addressing untouched")
        (is (= :nope (fp/resolve-projection-kind ctx :nope))
            "unregistered face resolves to itself → :unknown-projection")))
    (testing "no arsenal in ctx → static + identity only (W1 behavior)"
      (is (= :conversation (fp/resolve-projection-kind {} :outline)))
      (is (= :boxes-face (fp/resolve-projection-kind {} :boxes-face))))))

(deftest unicode-whitespace-rejected-in-names
  (is (not (aa/valid-assembly-name? "bad name")) "NBSP")
  (is (not (aa/valid-assembly-name? "bad name")) "ASCII space")
  (is (aa/valid-assembly-name? "good-name")))

(deftest unregister-removes-roster-entry-ipc
  (testing "register → listed; unregister → gone; double-unregister → no-op (rename-ghost fix)"
    (let [rt (fa/start-face-arsenal-runtime! {:wear-log-path
                                              (str (System/getProperty "java.io.tmpdir")
                                                   "/gate-fix-wal-" (System/nanoTime) ".ednl")})]
      (try
        (fa/register-face! rt {:face-name "ghost-face" :object-key "asm:ghost-face"
                               :import-key "imp:asm:asm:ghost-face:x" :status :candidate
                               :valid? true :source-ref "/tmp/ghost.edn"})
        (is (some? (fa/read-face rt "ghost-face")))
        (fa/unregister-face! rt {:face-name "ghost-face"})
        (is (nil? (fa/read-face rt "ghost-face")) "the ghost is gone")
        (is (= [] (->> (fa/list-faces rt) (filter #(= "ghost-face" (:face-name %))) vec)))
        (fa/unregister-face! rt {:face-name "ghost-face"})
        (is (nil? (fa/read-face rt "ghost-face")) "idempotent")
        (finally (fa/close-face-arsenal-runtime! rt))))))
