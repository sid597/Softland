(ns app.face-gate-fixes-test
  "W2 G26 gate-fix regressions (2026-07-11). One test per falsification
   finding retained after the assembly-import cut:
   - arsenal HIGH: non-numeric worn-at-ms must die at the ingress guard
     (post-guard it throws in wear-order-key and wedges the partition);
   - arsenal HIGH: rename ghosts — the unregister event removes roster rows;
   - server INT: face→projection resolution rides the arsenal roster."
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-arsenal :as fa]
            [app.server.rama.face-projection :as fp]))

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
        (is (= :face-list (fp/resolve-projection-kind ctx :face-list))
            "the surviving direct projection address stays available")
        (is (= :nope (fp/resolve-projection-kind ctx :nope))
            "unregistered face resolves to itself → :unknown-projection")))
    (testing "no arsenal in ctx → static + identity only (W1 behavior)"
      (is (= :conversation (fp/resolve-projection-kind {} :outline)))
      (is (= :boxes-face (fp/resolve-projection-kind {} :boxes-face))))))

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
