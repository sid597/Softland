(ns softland.auth.oracle-proof
  "Focused self-checks of the non-production Auth oracle shim, especially the
  new optional `:driven-by` field on `open-session` and `who-is`."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.auth.client :as auth]
            [softland.auth.module :as auth-module]
            [softland.wire.credential :as credential]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable]
           [java.util UUID]))

(defn -main [& _]
  (let [cluster (rtest/create-ipc)]
    (try
      (rtest/launch-module! cluster auth-module/Auth {:tasks 4 :threads 2})
      (let [mod-name (rama/get-module-name auth-module/Auth)
            c1 (auth/open cluster mod-name)
            c2 (auth/open cluster mod-name)
            p (UUID/randomUUID)
            g (UUID/randomUUID)
            h (credential/hash-of (credential/make-secret))
            now 1000000
            _ (auth/invite c1 {:person p :claim {:by :email :email "a@b.com"} :invited-by :system :at now})
            _ (auth/enter c1 {:account [:google "1"] :says {} :email "a@b.com" :code-hash nil
                              :own-layer (UUID/randomUUID)
                              :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at now})
            _ (auth/begin-sign-in c1 {:root p :actor p :sign-in g :kind :program :hash h
                                      :until (+ now 100000) :label "cli" :at now})
            seen-watch (atom [])
            stop (auth/watch-root c2 p (fn [v] (swap! seen-watch conj v)))
            s-absent (UUID/randomUUID)
            s-person (UUID/randomUUID)
            s-model  (UUID/randomUUID)
            s-bad    (UUID/randomUUID)]
        (check "open-session without :driven-by records nil (:driven-by absent means not said), readable across copies via who-is."
               (and (= {:open s-absent} (auth/open-session c1 {:root p :sign-in g :session s-absent
                                                               :until (+ now 50000) :at now}))
                    (nil? (get-in (auth/who-is c2 {:root p :sign-in g :session s-absent}) [:session :driven-by]))))
        (check "open-session with {:kind :person} persists :driven-by in who-is and repeat open-session does not overwrite it."
               (and (= {:open s-person} (auth/open-session c1 {:root p :sign-in g :session s-person
                                                               :until (+ now 50000) :at now
                                                               :driven-by {:kind :person}}))
                    (= {:open s-person} (auth/open-session c2 {:root p :sign-in g :session s-person
                                                               :until (+ now 80000) :at (+ now 100)
                                                               :driven-by {:kind :model :model "claude-opus"}}))
                    (let [sess (:session (auth/who-is c1 {:root p :sign-in g :session s-person}))]
                      (and (= {:kind :person} (:driven-by sess))
                           (= (+ now 80000) (:until sess))))))
        (check "open-session with {:kind :model :model text} persists :driven-by in who-is even after close-session, and repeat open-session after close returns {:open S} without reopening."
               (and (= {:open s-model} (auth/open-session c1 {:root p :sign-in g :session s-model
                                                              :until (+ now 50000) :at now
                                                              :driven-by {:kind :model :model "gemini-4"}}))
                    (= {:closed s-model} (auth/close-session c2 {:root p :session s-model :at (+ now 200)}))
                    (= {:open s-model} (auth/open-session c2 {:root p :sign-in g :session s-model
                                                              :until (+ now 90000) :at (+ now 300)}))
                    (let [sess (:session (auth/who-is c1 {:root p :sign-in g :session s-model}))]
                      (and (true? (:closed? sess))
                           (= (+ now 50000) (:until sess))
                           (= {:kind :model :model "gemini-4"} (:driven-by sess))))))
        (check "Invalid :driven-by values and non-map inputs never throw and return {:could-not :read-the-event}."
               (and (= {:could-not :read-the-event} (auth/invite c1 [:not-a-map]))
                    (= {:could-not :read-the-event} (auth/open-session c1 [1 2 3]))
                    (= {:could-not :read-the-event} (auth/who-is c1 '(1)))
                    (= {:could-not :read-the-event}
                       (auth/open-session c1 {:root p :sign-in g :session s-bad :until (+ now 50000) :at now
                                              :driven-by {:kind :robot}}))
                    (= {:could-not :read-the-event}
                       (auth/open-session c1 {:root p :sign-in g :session s-bad :until (+ now 50000) :at now
                                              :driven-by {:kind :model :model (apply str (repeat 257 "x"))}}))
                    (= {:could-not :read-the-event}
                       (auth/open-session c1 {:root p :sign-in g :session s-bad :until (+ now 50000) :at now
                                              :driven-by {:kind :person :extra 1}}))
                    (= {:could-not :read-the-event}
                       (auth/open-session c1 {:root p :sign-in g :session s-bad :until (+ now 50000) :at now
                                              :driven-by {:kind :model}}))
                    (nil? (:session (auth/who-is c1 {:root p :sign-in g :session s-bad})))))
        (Thread/sleep 200)
        (check "watch-root snapshots do not carry :driven-by."
               (and (seq @seen-watch)
                    (not-any? (fn [snap] (some #(contains? % :driven-by) (vals (:sessions snap))))
                              @seen-watch)))
        (stop)
        (let [after-stop (count @seen-watch)
              s-after (UUID/randomUUID)]
          (auth/open-session c1 {:root p :sign-in g :session s-after :until (+ now 50000) :at now})
          (Thread/sleep 200)
          (check "A stopped watch-root callback is never called again."
                 (= after-stop (count @seen-watch))))
        (auth/close! c1)
        (auth/close! c2))
      (finally
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
