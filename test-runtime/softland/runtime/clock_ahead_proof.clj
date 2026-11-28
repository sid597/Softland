(ns softland.runtime.clock-ahead-proof
  "What the runtime does when a copy whose clock is far ahead has written
  under a person's root. Such a write thins what Auth watches under the
  root: every sign-in and session leaves the watched value, and none of
  them has ended. On Rama's in-process cluster, with two copies of the
  runtime in one JVM. One command, from the repository root, behind the
  machine's cluster lock:

    clojure -M:runtime -m softland.runtime.clock-ahead-proof [without-the-count]

  The write from the clock that is ahead is made through Auth's client
  itself, with an `:at` ten years on: the copies of one JVM share one
  clock, so no copy can be given a wrong one.

  With `without-the-count`, the same checks run over a client of Auth that
  hands on no count of ends. The sentence of the end must then fail: it is
  the broken copy that the sentence is held against.

  State. A cluster that is thrown away at the end."
  (:require [softland.auth.client :as auth]
            [softland.auth.schema :as schema]
            [softland.command.client :as client]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.seam :as seam]
            [softland.wire.proof :refer [check finish]])
  (:import [java.util UUID]))

(def ^:private ten-years-ms
  (* 10 365 24 60 60 1000))

(defn- checks! []
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy-a (h/start-copy! {:cluster cluster :stand-in stand-in})
            copy-b (h/start-copy! {:cluster cluster :stand-in stand-in})]
        (try
          (h/setup-first-person! copy-a)
          (let [url-a (:url copy-a)
                url-b (:url copy-b)
                cookie (:cookie (drive/sign-in url-a (assoc h/first-person :call-her "Sid")))
                ;; Her programs are agents of hers; what only she may say, she says
                ;; at her browser, at the copy b.
                of-hers (fn [url asks] (drive/program url asks (fn [code] (drive/yes url cookie code {}))))
                kept (of-hers url-a "a program that is kept")
                ended (of-hers url-a "a program that is ended")
                admin-line (drive/browser-line url-b cookie)
                admin (:hello admin-line)
                kept-line (h/line url-a (:credential kept) nil)
                ended-line (h/line url-a (:credential ended) nil)
                root (:root (:hello kept-line))
                auth-client (seam/auth-of (:the-seam copy-b))
                listed (fn [] (set (map :sign-in (:sign-ins (h/say admin-line {:word :under})))))
                before (listed)
                ;; A copy whose clock is ten years ahead writes under her root.
                wrote (auth/close-session auth-client {:root root :session (UUID/randomUUID)
                                                       :at (+ (System/currentTimeMillis) ten-years-ms)})
                after (listed)
                ended-early? (h/wait-for (fn [] (or (client/bye (:line kept-line)) (client/bye (:line ended-line)))) 1500)
                still (h/say kept-line {:word :who-am-i} 5000)
                read (auth/who-is auth-client {:root root :sign-in (:sign-in kept) :session (:session (:hello kept-line))})]
            (check "The write from the clock that is ahead thinned what Auth watches under her root: her three sign-ins, her browser's and two of programs, were listed before it, and none is listed after it."
                   (and (= {:refused :no-such-session} wrote)
                        (every? before (map :sign-in [admin kept ended]))
                        (empty? after))
                   {:wrote wrote :before (count before) :after (count after)})
            (check "A write from a clock far ahead ends nothing: a connection that another copy holds under that root is not ended, its word is still answered, and Auth says of its sign-in and its session that nobody ended the one or closed the other."
                   (and (nil? ended-early?)
                        (= (:who (:hello kept-line)) (:who still))
                        (false? (get-in read [:sign-in :ended?]))
                        (false? (get-in read [:session :closed?])))
                   {:ended-early ended-early? :still still :read read})
            (let [said (h/say admin-line {:word :end-sign-in :sign-in (:sign-in ended)})
                  told (h/wait-for (fn [] (= :find-her-sign-in (:could-not (client/bye (:line ended-line))))) 10000)]
              (check "An end reaches a copy that holds a thing which Auth no longer watches: a sign-in that left the watched value is ended through another copy, and the copy that holds its connection ends it within ten seconds, with :find-her-sign-in; the other connection of that root goes on."
                     (and (= {:ended (:sign-in ended)} said)
                          (true? told)
                          (nil? (client/bye (:line kept-line))))
                     {:said said :bye (client/bye (:line ended-line)) :kept-bye (client/bye (:line kept-line))}))
            (let [again (h/line url-b (:credential kept) nil)
                  opened (h/say kept-line {:word :open-session})
                  refused (h/line url-b (:credential ended) nil)]
              (check "A sign-in that Auth no longer watches still stands for its holder: a new connection under it is opened at another copy, in a new session, and the word :open-session opens one more; the sign-in that was ended stands for nobody."
                     (and (uuid? (:session (:hello again)))
                          (not= (:session (:hello again)) (:session (:hello kept-line)))
                          (uuid? (:open opened))
                          (= :find-her-sign-in (:could-not refused)))
                     {:again (dissoc again :line) :opened opened :refused refused})
              (h/close-line! again))
            (doseq [line [admin-line kept-line ended-line]]
              (h/close-line! line)))
          (finally
            (h/stop-copy! copy-a)
            (h/stop-copy! copy-b)))))))

(defn -main
  "Runs every check, prints the counts, and ends the process with a status.
  With `without-the-count` as its argument, Auth's client hands on a
  watched value whose count of ends never changes."
  [& [mode]]
  (if (= "without-the-count" mode)
    (let [live-with schema/live-with]
      (println "Over a client of Auth that hands on no count of ends: the sentence of the end must fail.")
      (with-redefs [schema/live-with (fn [proj entry]
                                       (if (instance? softland.auth.schema.LiveEnds entry)
                                         proj
                                         (live-with proj entry)))]
        (checks!)))
    (checks!))
  (finish))
