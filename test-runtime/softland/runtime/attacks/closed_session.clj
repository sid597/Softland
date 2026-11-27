(ns softland.runtime.attacks.closed-session
  "Attack 3: Question 17 — a closed session kept in use through `moved-on!`'s
  reading of `{:open S}` when no push reaches the copy.

  Under Auth's spec, a repeat `open-session` on a session that is already
  closed returns `{:open S}` and leaves the session closed; only `who-is`
  says that it is closed. When the session closes right before `moved-on!`'s
  `open-session` and no push reaches the copy that holds the session, the
  copy must not trust `{:open S}` as having moved the session on for another
  half-span: by its next check (at most ~1 s later), the connection must end
  and refuse further acts in the closed session."
  (:require [softland.auth.client :as auth]
            [softland.command.client :as client]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.permissions :as perm]
            [softland.wire.proof :refer [check finish]])
  (:import [java.util UUID]))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      ;; Session span is 10 s (half-span ~5.05 s); when not moved on, the
      ;; seam's next check is at `(min left 1000)` = 1 s.
      (let [copy-a (h/start-copy! {:cluster cluster :stand-in stand-in})
            copy-c (h/start-copy! {:cluster cluster :stand-in stand-in
                                   :numbers {:secret-ms 60000
                                             :previous-ms 2000
                                             :session-ms 10000}})]
        (try
          (h/setup-first-person! copy-a)
          (let [signed (drive/sign-in (:url copy-a) (assoc h/first-person :call-her "Sid"))
                prog (drive/program (:url copy-a) "closed-session-probe"
                                    (fn [code] (drive/yes (:url copy-a) (:cookie signed) code {})))
                drop-pushes? (atom false)
                orig-watch-root auth/watch-root
                orig-open-session auth/open-session
                closed-before-renew (atom nil)]
            (with-redefs [auth/watch-root
                          (fn [c root f]
                            (orig-watch-root c root
                                             (fn [v]
                                               (when-not @drop-pushes?
                                                 (f v)))))
                          auth/open-session
                          (fn [c m]
                            ;; Leave initial session creation untouched; intercept
                            ;; the half-span renewal of `target-session` on copy-c,
                            ;; close the session in Auth with pushes suppressed,
                            ;; and let `open-session` return Auth's repeat `{:open S}`.
                            (when-let [{:keys [session root closed-now?]} @closed-before-renew]
                              (when (and (= session (:session m))
                                         (not closed-now?))
                                (reset! drop-pushes? true)
                                (auth/close-session c {:root root :session session :at (System/currentTimeMillis)})
                                (swap! closed-before-renew assoc :closed-now? true :closed-at (System/currentTimeMillis))))
                            (orig-open-session c m))]
              (let [held (h/line (:url copy-c) (:credential prog) nil)
                    hello (:hello held)
                    who (:who hello)
                    session (:session hello)
                    lane (:own-lane hello)
                    own (perm/first-permission who lane)]
                (reset! closed-before-renew {:session session :root (:root hello) :closed-now? false})
                ;; Wait for the half-span check (~5.05 s) to trigger renewal.
                (h/wait-for (fn [] (:closed-now? @closed-before-renew)) 8000)
                ;; Wait up to 1.8 s for the next check (due within <= 1 s when
                ;; `moved-on!` does not falsely extend `session-until` by 5 s).
                (let [ended-in-time? (h/wait-for (fn [] (= :find-her-session (:could-not (client/bye (:line held)))))
                                                 1800)
                      act-after (h/say held {:word :send
                                             :act {:version 1 :who who :session session
                                                   :lane lane :permission (perm/permission-id own)
                                                   :facts [{:e :after-close :k :v :v :took}]}
                                             :act-id (UUID/randomUUID)}
                                       3000)
                      bye-now (client/bye (:line held))]
                  (check "A session that is closed is not kept in use by the copy that holds it, past its next check."
                         (and (true? ended-in-time?)
                              (= :find-her-session (:could-not bye-now))
                              (not= :accepted (get-in act-after [:answer :answer])))
                         {:closed-before-renew? (:closed-now? @closed-before-renew)
                          :bye-after-next-check bye-now
                          :act-sent-in-closed-session act-after}))
                (h/close-line! held))))
          (finally
            (h/stop-copy! copy-c)
            (h/stop-copy! copy-a))))))
  (finish))
