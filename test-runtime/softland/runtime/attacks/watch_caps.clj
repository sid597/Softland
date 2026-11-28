(ns softland.runtime.attacks.watch-caps
  "Attack 7: live watches are capped for each root and for visitors, in each
  copy of the runtime.

  Verifies:
  1. The runtime's default numbers define `:max-watches-per-root` and
     `:max-visitor-watches`.
  2. In copy `a`, a root (a person and an agent beneath her, across separate
     connections) can open up to `:max-watches-per-root` live watches; the
     next watch under that root in copy `a` ends with `:could-not` and no
     `:anew`, while another root in copy `a` and the same root in copy `b`
     can still open watches, and leaving a watch frees its slot.
  3. In copy `a`, visitors across separate connections can open up to
     `:max-visitor-watches` live watches; the next visitor watch in copy `a`
     ends with `:could-not` and no `:anew`, while a visitor in copy `b` can
     still open a watch, and closing a visitor connection frees its slots."
  (:require [softland.command.client :as client]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.seam :as seam]
            [softland.wire.credential :as credential]
            [softland.wire.proof :refer [check finish]])
  (:import [java.util UUID]))

(defn- open-watch!
  "Opens one watch on `opened` for `query` and waits for its first message
  (`:anew` or `:ended`)."
  [opened query]
  (let [msgs (atom [])
        id (client/watch (:line opened) query (fn [m] (swap! msgs conj m)))]
    (h/wait-for (fn [] (seq @msgs)) 5000)
    {:id id :msgs msgs :first (first @msgs)}))

(defn- anew? [w]
  (contains? (:first w) :anew))

(defn- capped? [w]
  (and (some? (get-in (:first w) [:ended :could-not]))
       (not-any? :anew @(:msgs w))))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy-default (h/start-copy! {:cluster cluster :stand-in stand-in})
            caps {:max-watches-per-root 4 :max-visitor-watches 3}
            copy-a (h/start-copy! {:cluster cluster :stand-in stand-in :numbers caps})
            copy-b (h/start-copy! {:cluster cluster :stand-in stand-in :numbers caps})]
        (try
          (h/setup-first-person! copy-default)
          (let [defaults (seam/numbers-of (:the-seam copy-default))
                has-keys? (and (contains? defaults :max-watches-per-root)
                               (contains? defaults :max-visitor-watches))
                url-a (:url copy-a)
                url-b (:url copy-b)
                ;; Person 1's program, an agent of hers, + an agent beneath it (same `:root`).
                p1-in (drive/sign-in url-a (assoc h/first-person :call-her "Sid"))
                p1-prog (drive/program url-a "p1" (fn [c] (drive/yes url-a (:cookie p1-in) c {})))
                p1-conn-a (h/line url-a (:credential p1-prog) nil)
                p1-conn-b (h/line url-b (:credential p1-prog) nil)
                p1-who (:who (:hello p1-conn-a))
                p1-lane (:own-lane (:hello p1-conn-a))
                agent-secret (credential/make-secret)
                made-agent (h/say p1-conn-a {:word :make-agent :uuid (UUID/randomUUID)
                                             :label "watcher-agent"
                                             :hash (credential/hash-of agent-secret)})
                agent-cred (credential/text (:root made-agent) (:sign-in made-agent) agent-secret)
                agent-conn-a (h/line url-a agent-cred nil)
                ;; Person 2 (a second `:root`).
                inv-code (credential/make-secret)
                ;; She invites at her browser: no program may.
                _ (let [at-browser (drive/browser-line url-a (:cookie p1-in))]
                    (h/say at-browser {:word :invite :uuid (UUID/randomUUID)
                                       :claim {:by :code :hash (credential/hash-of inv-code)}})
                    (h/close-line! at-browser))
                p2-in (drive/sign-in url-a {:sub "2000002" :email "second@example.com"
                                            :name "Second" :invited inv-code :call-her "Asha"})
                p2-prog (drive/program url-a "p2" (fn [c] (drive/yes url-a (:cookie p2-in) c {})))
                p2-conn-a (h/line url-a (:credential p2-prog) nil)
                p2-who (:who (:hello p2-conn-a))
                p2-lane (:own-lane (:hello p2-conn-a))
                ;; Root 1 opens 2 watches as her program + 2 watches as its agent on copy-a (= 4).
                r1-w1 (open-watch! p1-conn-a {:asks [:place :e1 :k] :asker p1-who :lanes [p1-lane]})
                r1-w2 (open-watch! p1-conn-a {:asks [:place :e2 :k] :asker p1-who :lanes [p1-lane]})
                r1-w3 (open-watch! agent-conn-a {:asks [:place :e3 :k] :asker (:who (:hello agent-conn-a)) :lanes [p1-lane]})
                r1-w4 (open-watch! agent-conn-a {:asks [:place :e4 :k] :asker (:who (:hello agent-conn-a)) :lanes [p1-lane]})
                ;; 5th watch under Root 1 on copy-a must be capped.
                r1-w5-over (open-watch! p1-conn-a {:asks [:place :e5 :k] :asker p1-who :lanes [p1-lane]})
                ;; Root 2 on copy-a and Root 1 on copy-b must still succeed.
                r2-on-a (open-watch! p2-conn-a {:asks [:place :e1 :k] :asker p2-who :lanes [p2-lane]})
                r1-on-b (open-watch! p1-conn-b {:asks [:place :e6 :k] :asker p1-who :lanes [p1-lane]})
                ;; Leaving one watch of Root 1 on copy-a frees a slot.
                _ (client/leave (:line p1-conn-a) (:id r1-w1))
                r1-after-leave (open-watch! p1-conn-a {:asks [:place :e7 :k] :asker p1-who :lanes [p1-lane]})
                ;; Visitors: 2 watches on vis-a1 + 1 watch on vis-a2 (= 3) on copy-a.
                vis-a1 (h/line url-a nil nil)
                vis-a2 (h/line url-a nil nil)
                vis-a3 (h/line url-a nil nil)
                vis-b1 (h/line url-b nil nil)
                vq (fn [e] {:asks [:place e :k] :not-signed-in true :lanes [p1-lane]})
                v-w1 (open-watch! vis-a1 (vq :v1))
                v-w2 (open-watch! vis-a1 (vq :v2))
                v-w3 (open-watch! vis-a2 (vq :v3))
                ;; 4th visitor watch on copy-a must be capped.
                v-w4-over (open-watch! vis-a3 (vq :v4))
                ;; Visitor on copy-b must still succeed.
                v-on-b (open-watch! vis-b1 (vq :v5))
                ;; Closing vis-a1 frees its visitor slots on copy-a.
                _ (h/close-line! vis-a1)
                v-reopened? (h/wait-for (fn []
                                          (let [w (open-watch! vis-a3 (vq :v6))]
                                            (when (anew? w)
                                              (client/leave (:line vis-a3) (:id w))
                                              true)))
                                        2000)]
            (check "Live watches are capped for each root and for visitors, in each copy."
                   (and has-keys?
                        (every? anew? [r1-w1 r1-w2 r1-w3 r1-w4])
                        (capped? r1-w5-over)
                        (anew? r2-on-a)
                        (anew? r1-on-b)
                        (anew? r1-after-leave)
                        (every? anew? [v-w1 v-w2 v-w3])
                        (capped? v-w4-over)
                        (anew? v-on-b)
                        (true? v-reopened?))
                   {:default-number-keys (sort (keys defaults))
                    :root-5th-on-a (:first r1-w5-over)
                    :root-2-on-a (:first r2-on-a)
                    :root-1-on-b (:first r1-on-b)
                    :root-after-leave (:first r1-after-leave)
                    :visitor-4th-on-a (:first v-w4-over)
                    :visitor-on-b (:first v-on-b)
                    :visitor-reopened? v-reopened?})
            (doseq [c [p1-conn-a p1-conn-b agent-conn-a p2-conn-a vis-a1 vis-a2 vis-a3 vis-b1]]
              (h/close-line! c)))
          (finally
            (h/stop-copy! copy-b)
            (h/stop-copy! copy-a)
            (h/stop-copy! copy-default))))))
  (finish))
