(ns softland.runtime.attacks.unwatched-standing
  "Attack 4: a `standing` flow that throws while opening must end its
  connection rather than leaving the connection open and unwatched, and must
  not leave a poisoned root entry behind in `standing`."
  (:require [softland.auth.client :as auth]
            [softland.command.client :as client]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.standing :as standing]
            [softland.wire.proof :refer [check finish]]))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy-a (h/start-copy! {:cluster cluster :stand-in stand-in})
            copy-b (h/start-copy! {:cluster cluster :stand-in stand-in})]
        (try
          (h/setup-first-person! copy-a)
          (let [url-a (:url copy-a)
                url-b (:url copy-b)
                signed (drive/sign-in url-a (assoc h/first-person :call-her "Sid"))
                cookie (:cookie signed)
                me-b (h/line url-b (:credential (drive/program url-b "admin" (fn [c] (drive/yes url-b cookie c {})))) nil)
                p1 (drive/program url-a "prog-1" (fn [c] (drive/yes url-a cookie c {})))
                p2 (drive/program url-a "prog-2" (fn [c] (drive/yes url-a cookie c {})))
                p3 (drive/program url-a "prog-3" (fn [c] (drive/yes url-a cookie c {})))
                orig-watch-root auth/watch-root
                orig-listen standing/listen!
                throw-watch-once? (atom true)
                throw-listen-once? (atom false)]
            (with-redefs [auth/watch-root
                          (fn [c root f]
                            (if (compare-and-set! throw-watch-once? true false)
                              (throw (RuntimeException. "watch-root threw on open"))
                              (orig-watch-root c root f)))
                          standing/listen!
                          (fn [st root f]
                            (if (compare-and-set! throw-listen-once? true false)
                              (throw (RuntimeException. "standing/listen! threw"))
                              (orig-listen st root f)))]
              ;; 1. `auth/watch-root` throws on `conn-1`'s opening: `conn-1` must
              ;;    not stay open unwatched, and a later connection `conn-2`
              ;;    under the same root must still get a working watch.
              (let [conn-1 (h/line url-a (:credential p1) nil)
                    _ (h/say me-b {:word :end-sign-in :sign-in (:sign-in p1)})
                    conn-1-closed? (h/wait-for (fn [] (:gone @(:state (:line conn-1)))) 2500)
                    conn-1-who (h/say conn-1 {:word :who-am-i} 2000)
                    conn-2 (h/line url-a (:credential p2) nil)
                    _ (h/say me-b {:word :end-sign-in :sign-in (:sign-in p2)})
                    conn-2-bye (h/wait-for (fn [] (when (= :find-her-sign-in (:could-not (client/bye (:line conn-2))))
                                                    (client/bye (:line conn-2))))
                                           3000)
                    ;; 2. `standing/listen!` throws directly inside `seam/standing`:
                    ;;    `line.clj`'s `m/reduce` failure callback must end `conn-3`.
                    _ (reset! throw-listen-once? true)
                    conn-3 (h/line url-a (:credential p3) nil)
                    _ (h/say me-b {:word :end-sign-in :sign-in (:sign-in p3)})
                    conn-3-closed? (h/wait-for (fn [] (:gone @(:state (:line conn-3)))) 2500)
                    conn-3-who (h/say conn-3 {:word :who-am-i} 2000)]
                (check "A standing that fails does not leave its connection unwatched."
                       (and (some? conn-1-closed?)
                            (some? (:could-not conn-1-who))
                            (some? conn-2-bye)
                            (some? conn-3-closed?)
                            (some? (:could-not conn-3-who)))
                       {:conn-1 {:gone conn-1-closed? :bye (client/bye (:line conn-1)) :who-after-end conn-1-who}
                        :conn-2-same-root-recovery {:bye conn-2-bye}
                        :conn-3 {:gone conn-3-closed? :bye (client/bye (:line conn-3)) :who-after-end conn-3-who}})
                (h/close-line! conn-1)
                (h/close-line! conn-2)
                (h/close-line! conn-3)))
            (h/close-line! me-b))
          (finally
            (h/stop-copy! copy-a)
            (h/stop-copy! copy-b))))))
  (finish))
