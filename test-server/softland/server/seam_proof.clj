(ns softland.server.seam-proof
  "The proof of the seam on Rama's in-process cluster: the server's module,
  its client, the seam and what sign-in writes, against the store's own
  module, door and read path. One command, from the repository root:

    clojure -M:server -m softland.server.seam-proof

  It starts one in-process cluster with four tasks and two threads,
  launches the modules `Store` and `Sessions` in it, and opens two copies
  of what a server holds: two clients of the sessions and two seams over
  one door and one read path. What must reach every copy of the server is
  held between those two: a session that begins or ends through one copy
  is seen by a flow that the other holds.

  State. The cluster's files are temporary and are Rama's. The proof makes
  lanes and acts in the store that it started, and ends with the cluster
  closed."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [missionary.core :as m]
            [softland.server.module :as module]
            [softland.server.proof :refer [check finish]]
            [softland.server.seam :as seam]
            [softland.server.session.credential :as credential]
            [softland.server.sessions :as sessions]
            [softland.server.sign-in :as sign-in]
            [softland.server.watching :as watching]
            [softland.store.act :as act]
            [softland.store.door :as door]
            [softland.store.module :as store]
            [softland.store.read-path :as read-path])
  (:import [java.util UUID]
           [java.util.concurrent CountDownLatch TimeUnit]))

(defn- collected
  "Runs a flow and puts each thing that it gives into a new atom, with
  `::done` when the flow has ended. Gives [the atom, a function that
  cancels the flow]. When `hold` is given, a latch, each thing is taken
  only after the latch opens: a reader that is slower than the changes."
  ([flow] (collected flow nil))
  ([flow ^CountDownLatch hold]
   (let [got (atom [])
         cancel ((m/reduce (fn [_ x]
                             (when hold (.await hold 30 TimeUnit/SECONDS))
                             (swap! got conj (if (map? x) (assoc x ::virtual? (.isVirtual (Thread/currentThread))) x))
                             nil)
                           nil flow)
                 (fn [_] (swap! got conj ::done))
                 (fn [_] (swap! got conj ::done)))]
     [got cancel])))

(defn- until
  "Waits until a function of no argument gives a true thing, for five
  seconds at most. Gives what it gave last."
  [f]
  (loop [n 0]
    (let [got (f)]
      (if (or got (<= 100 n))
        got
        (do (Thread/sleep 50) (recur (inc n)))))))

(defn- states [got] (mapv #(if (map? %) (:state %) %) @got))
(defn- kinds [got] (mapv #(cond (not (map? %)) % (:anew %) :anew (:changed %) :changed (:ended %) :ended) @got))

(defn- write
  "Gives the act that writes one fact into a lane under a person's first
  permission there, without `:who` and `:session`."
  [who lane entity k v]
  {:version 1 :lane lane :permission (sign-in/permission-id (sign-in/first-permission who lane)) :facts [{:e entity :k k :v v}]})

(defn- run-checks [cluster]
  (let [store-name (rama/get-module-name store/Store)
        sessions-name (rama/get-module-name module/Sessions)
        the-door (door/open cluster store-name)
        the-read-path (read-path/open cluster store-name)
        sessions-a (sessions/open cluster sessions-name)
        sessions-b (sessions/open cluster sessions-name)
        a (seam/open {:door the-door :read-path the-read-path :sessions sessions-a})
        b (seam/open {:door the-door :read-path the-read-path :sessions sessions-b})
        short (seam/open {:door the-door :read-path the-read-path :sessions sessions-a :numbers {:session-ms 1500}})]

    ;; ------------------------------------------------ what sign-in writes
    (check "The setup of sign-in ends ready, and a second run of it, through another copy of the server, ends ready and writes nothing that changes that."
           (= [:ready :ready] [(m/? (sign-in/setup a)) (m/? (sign-in/setup b))]))
    (let [claims {"sub" "108234567890" "email" "asha@example.com" "email_verified" true "name" "Asha"}
          asha (m/? (sign-in/person-for a claims))
          again (m/? (sign-in/person-for b (assoc claims "name" "Asha K")))
          system (seam/system a)
          says (m/? (seam/read a system {:asks [:place (:who asha) sign-in/says-key] :lanes [sign-in/people]}))]
      (check "A first sign-in of an account gives a person: her party id and her own lane's id."
             (and (= :person (act/party-kind (:who asha))) (uuid? (:own-lane asha)) (not= (:who asha) (:own-lane asha))) asha)
      (check "A later sign-in of the same account, through another copy of the server, finds the same person and the same lane."
             (= asha again) again)
      (check "What Google said at her latest sign-in is on her entity, under Google's own names, for the system to read."
             (= "Asha K" (get (:value (:place says)) "name")) says)
      (check "Each act of sign-in that is about a person names her id in a fact of its own, in the store's own form of an act, which holds no part for it: the act that writes what Google said has her as the entity of its fact, and the act that makes her own lane has her as the holder of its first permission. The act that makes the system's lane names nobody but the system."
             (let [whole (fn [act] (:ok (act/parse (assoc act :who :system :session (UUID/randomUUID)))))
                   holders (fn [act] (set (keep (comp :holder :v) (:facts act))))
                   person (:who asha)
                   said (whole (#'sign-in/in-people [{:e person :k sign-in/says-key :v {"sub" "1"}}]))
                   hers (whole (#'sign-in/making-act (:own-lane asha) person))
                   systems (whole (#'sign-in/making-act sign-in/people :system))]
               (= [#{person} #{person} #{:system} [false false false]]
                  [(set (map :e (:facts said))) (holders hers) (holders systems)
                   (mapv #(contains? % :subjects) [said hers systems])])))
      (check "Her own lane is where sign-in said: `own-lane-of` gives it for her id, and nothing for an id that is no person."
             (= [(:own-lane asha) nil] [(m/? (sign-in/own-lane-of a (:who asha))) (m/? (sign-in/own-lane-of a (UUID/randomUUID)))]))
      (let [both (m/? (m/join vector
                              (sign-in/person-for a {"sub" "2000000001"})
                              (sign-in/person-for b {"sub" "2000000001"})))]
        (check "Two first sign-ins of one account at the same moment, through two copies of the server, give one person."
               (and (= :person (act/party-kind (:who (first both)))) (= (first both) (second both))) both))

      ;; ------------------------------------------------------------- who
      (let [token (credential/make)
            hash (credential/hash-of token)
            [standings cancel-standing] (collected (seam/standing a token))
            mallory (UUID/randomUUID)]
        (check "No credential is a visitor, and a visitor has no who."
               (let [actor (m/? (seam/actor a nil))] (and (seam/actor? actor) (nil? (seam/who-of actor)))))
        (check "A token that nobody said yes to, and a text that is no token, are told so and are not made a visitor."
               (= [{:could-not :find-her-session :why :not-known} {:could-not :find-her-session :why :not-a-token}]
                  [(m/? (seam/actor a token)) (m/? (seam/actor a "nonsense"))]))
        (check "A flow of a credential's standing says first that it is not known."
               (= [:not-known] (until #(when (= 1 (count @standings)) (states standings)))) (states standings))
        (let [begun (m/? (seam/begin b (:who asha) hash))
              actor (m/? (seam/actor a token))]
          (check "A session begun through one copy of the server is an actor at the other, with her who and a session id."
                 (and (seam/actor? actor) (= (:who asha) (seam/who-of actor)) (= (:session begun) (seam/session-of actor)))
                 [begun actor])
          (check "The flow that the other copy holds is told that the credential is live, with her actor, and no faster reader is needed for it."
                 (= [:not-known :live] (until #(when (= 2 (count @standings)) (states standings)))) (states standings))
          (check "An actor prints as the word and nothing of who she is."
                 (= "#<actor>" (pr-str actor) (str actor)))
          (check "A second beginning under the same hash, for another who, is refused, and the credential still stands for the first."
                 (and (= {:could-not :begin-her-session :why :credential-in-use} (m/? (seam/begin a mallory hash)))
                      (= (:who asha) (seam/who-of (m/? (seam/actor b token))))))

          ;; ----------------------------------------------------------- the tasks
          (let [lane (:own-lane asha)
                who (:who asha)
                [act-id other-id] (m/? (seam/act-ids a actor lane 2))
                act (write who lane :chair-7 :colour :green)
                receipt (m/? (seam/send a actor act act-id))
                answer (m/? (seam/answer a actor act-id (:digest receipt) 2000))
                again (m/? (seam/send b actor act act-id))
                read-back (m/? (seam/read a actor {:asks [:place :chair-7 :colour] :from-lane lane :with #{:writer}}))]
            (check "The seam gives act-ids of a lane ahead of sending, and gives a visitor none."
                   (and (= [lane] (pop act-id)) (not= act-id other-id)
                        (= :find-her-session (:could-not (m/? (seam/act-ids a (seam/visitor) lane 1))))))
            (check "A send gives the door's receipt, the act-id and the digest and the ack, and the answer read with them says accepted."
                   (and (= act-id (:act-id receipt)) (string? (:digest receipt)) (= :gate-ran (:ack receipt))
                        (= :accepted (:answer answer)))
                   [receipt answer])
            (check "The same act sent again under the same act-id, through another copy, has the same digest and reads the one answer: it is done once."
                   (and (= (:digest receipt) (:digest again))
                        (= answer (m/? (seam/answer b actor act-id (:digest again) 2000)))))
            (check "Another act under the used act-id is not hers to read: the answer says not mine."
                   (let [other (m/? (seam/send a actor (write who lane :chair-7 :colour :red) act-id))]
                     (= {:answer :not-mine} (m/? (seam/answer a actor act-id (:digest other) 2000)))))
            (check "The fact that a read gives back names her as its writer: the who of the act was the actor's, written by the seam."
                   (and (= :green (:value (:place read-back))) (= who (:writer (:place read-back)))) read-back)
            (check "An act that already holds a who, or a session, is turned away by the seam and nothing is sent for it."
                   (= [{:could-not :read-the-word :why :holds-a-part-that-the-server-writes :at :who}
                       {:could-not :read-the-word :why :holds-a-part-that-the-server-writes :at :session}]
                      [(m/? (seam/send a actor (assoc act :who :system) nil))
                       (m/? (seam/send a actor (assoc act :session (UUID/randomUUID)) nil))]))
            (check "A query that already holds its asker is turned away by the seam."
                   (= :read-the-word (:could-not (m/? (seam/read a actor {:asks [:place :chair-7 :colour] :from-lane lane :asker :system})))))
            (check "An act of a wrong form is sent all the same, and a gate refuses it under its act-id: the log keeps it."
                   (let [wrong (m/? (seam/send a actor {:version 9 :lane lane :permission (sign-in/permission-id (sign-in/first-permission who lane)) :facts []} nil))]
                     (= :refused (:answer (m/? (seam/answer a actor (:act-id wrong) (:digest wrong) 2000))))))
            (check "A visitor sends nothing, and reads her lane as somebody who may not see it."
                   (and (= {:could-not :find-her-session :why :no-credential} (m/? (seam/send a (seam/visitor) act nil)))
                        (= [{:lane lane :why :may-not-see}]
                           (:left-out (m/? (seam/read a (seam/visitor) {:asks [:place :chair-7 :colour] :lanes [lane]}))))))
            (check "A person who is signed in may not see the lane of the people."
                   (= [{:lane sign-in/people :why :may-not-see}]
                      (:left-out (m/? (seam/read a actor {:asks [:points-at sign-in/account-key "108234567890"] :lanes [sign-in/people]})))))
            (let [ids (m/? (seam/act-ids a actor lane 3))
                  results (m/? (seam/send-all a actor (mapv (fn [id n] [id (write who lane :chair-8 :n n)]) ids [1 2 3])))
                  after-three (:value (:place (m/? (seam/read a actor {:asks [:place :chair-8 :n] :from-lane lane}))))
                  stopped (m/? (seam/send-all a actor [[(first (m/? (seam/act-ids a actor lane 1))) (write who lane :chair-8 :n 4)]
                                                      [[:another-lane (UUID/randomUUID)] (write who lane :chair-8 :n 5)]
                                                      [(first (m/? (seam/act-ids a actor lane 1))) (write who lane :chair-8 :n 6)]]))]
              (check "A series of acts is sent in order and gives a receipt for each, and the last of them is what a read gives."
                     (and (= ids (mapv :act-id results)) (= 3 after-three))
                     [results after-three])
              (check "A series ends at the first act that the door could not send: the word is its last thing, and no act after it is tried."
                     (and (= 2 (count stopped)) (:act-id (first stopped)) (:could-not (second stopped))
                          (= 4 (:value (:place (m/? (seam/read a actor {:asks [:place :chair-8 :n] :from-lane lane}))))))
                     stopped))

            ;; ---------------------------------------------------------- the flow
            (let [query {:asks [:entity :desk-1] :from-lane lane}
                  [watched cancel-watch] (collected (seam/watch a actor query))]
              (check "A watch gives the whole answer first."
                     (= [:anew] (until #(when (seq @watched) (kinds watched)))) (kinds watched))
              (m/? (seam/send b actor (write who lane :desk-1 :height 72) nil))
              (check "A watch is told the place that an act changed, with what the read gives there now, on a thread that is not Rama's."
                     (let [message (until #(second @watched))]
                       (and (= 72 (:value (get (:changed message) [:desk-1 :height]))) (::virtual? message)))
                     @watched)
              (cancel-watch)
              (check "A watch that is cancelled leaves its live read: the read path holds no read and no push."
                     (= {:live-reads 0 :pushes 0} (until #(let [n (read-path/open-count the-read-path)] (when (= 0 (:live-reads n)) n))))
                     (read-path/open-count the-read-path)))
            (let [query {:asks [:entity :desk-2] :from-lane lane}
                  hold (CountDownLatch. 1)
                  [watched cancel-watch] (collected (seam/watch a actor query) hold)]
              (Thread/sleep 500)
              (doseq [n (range 30)]
                (m/? (seam/send a actor (write who lane :desk-2 (nth [:x :y :z] (mod n 3)) n) nil)))
              (.countDown hold)
              (let [fresh (m/? (seam/read a actor query))
                    told (until #(when (= (:places fresh)
                                          (:places (reduce watching/answer-after nil (remove keyword? @watched))))
                                   @watched))]
                (check "A reader who was held back while 30 acts changed three places is handed far fewer than 30 things, and what she then holds is the answer that a read gives now."
                       (and told (< (count told) 12)) (kinds watched)))
              (cancel-watch))

            ;; ---------------------------------------- a session that ends, ends
            (let [[watched _] (collected (seam/watch a actor {:asks [:entity :desk-1] :from-lane lane}))]
              (until #(seq @watched))
              (check "The session is ended through the other copy of the server, by an actor that the other copy made from the same credential."
                     (= [:ok] (m/? (seam/sign-out b (m/? (seam/actor b token))))))
              (check "The watch that this copy holds is told the end, with the word that her session ended, and the flow is over."
                     (= [:anew :ended ::done] (until #(when (= ::done (last @watched)) (kinds watched))))
                     @watched)
              (check "The end's word says why."
                     (= {:could-not :find-her-session :why :ended} (:ended (second @watched))))
              (check "The flow of her standing is told the end and is over."
                     (= [:not-known :live :ended ::done] (until #(when (= ::done (last @standings)) (states standings))))
                     (states standings))
              (check "The credential is no actor any more, at either copy."
                     (= [{:could-not :find-her-session :why :ended} {:could-not :find-her-session :why :ended}]
                        [(m/? (seam/actor a token)) (m/? (seam/actor b token))]))
              (check "An actor that was made before her session ended opens no watch: the flow gives the end and nothing else."
                     (let [[stale _] (collected (seam/watch a actor {:asks [:entity :desk-1] :from-lane lane}))]
                       (and (= [:ended ::done] (until #(when (= ::done (last @stale)) (kinds stale))))
                            (= {:could-not :find-her-session :why :ended} (:ended (first @stale))))))
              (check "No live read is left behind."
                     (= 0 (:live-reads (until #(let [n (read-path/open-count the-read-path)] (when (= 0 (:live-reads n)) n))))))))
          (cancel-standing))

        ;; ------------------------------------------------------- a session's time
        (let [token (credential/make)
              hash (credential/hash-of token)
              _ (m/? (seam/begin short (:who asha) hash))
              actor (m/? (seam/actor short token))
              [watched _] (collected (seam/watch short actor {:asks [:entity :desk-1] :from-lane (:own-lane asha)}))
              [standings _] (collected (seam/standing short token))]
          (check "A session whose time passes ends its watch and the flow of its standing, by the server's own timer."
                 (and (= [:anew :ended ::done] (until #(when (= ::done (last @watched)) (kinds watched))))
                      (= {:could-not :find-her-session :why :over} (:ended (second @watched)))
                      (= [:live :over ::done] (until #(when (= ::done (last @standings)) (states standings)))))
                 [(kinds watched) (states standings)])
          (check "After its time the credential is no actor, and an actor that a face still held sends nothing."
                 (and (= {:could-not :find-her-session :why :over} (m/? (seam/actor a token)))
                      (= {:could-not :find-her-session :why :over}
                         (m/? (seam/send a actor (write (:who asha) (:own-lane asha) :desk-1 :late true) nil))))))

        ;; -------------------------------------------------------------- her no
        (let [asked (credential/hash-of (credential/make))
              hers (credential/hash-of (credential/make))
              ben (m/? (sign-in/person-for a {"sub" "3000000003"}))
              ben-token (credential/make)
              _ (m/? (seam/begin a (:who ben) (credential/hash-of ben-token)))
              ben-actor (m/? (seam/actor a ben-token))
              asha-token (credential/make)
              _ (m/? (seam/begin a (:who asha) (credential/hash-of asha-token)))
              asha-actor (m/? (seam/actor a asha-token))
              _ (m/? (seam/begin a (:who asha) hers))]
          (check "Her no to a hash that stands for nobody refuses it for good: a later yes does not begin a session."
                 (and (= [:ok] (m/? (seam/refuse a asha-actor asked)))
                      (= :credential-in-use (:why (m/? (seam/begin a (:who asha) asked))))))
          (check "A person who knows the hash of another person's credential cannot end that session with a no."
                 (and (= {:could-not :find-her-session :why :not-hers} (m/? (seam/refuse a ben-actor hers)))
                      (= :live (:state (second (sessions/row-of sessions-a hers))))))
          (check "Her no to a hash that stands for her ends that session."
                 (and (= [:ok] (m/? (seam/refuse a asha-actor hers)))
                      (= :ended (:state (second (sessions/row-of sessions-a hers))))))
          (check "An actor ends her own session, and a visitor has none to end."
                 (and (= [:ok] (m/? (seam/sign-out a ben-actor)))
                      (= :ended (:why (m/? (seam/actor a ben-token))))
                      (= :find-her-session (:could-not (m/? (seam/sign-out a (seam/visitor))))))))))

    (seam/close! a) (seam/close! b) (seam/close! short)
    (sessions/close! sessions-a) (sessions/close! sessions-b)))

(defn -main
  "Starts the cluster, runs every check, closes the cluster, prints the
  counts, and ends the process with status 0 when none failed."
  [& _]
  (with-open [cluster (rtest/create-ipc)]
    (rtest/launch-module! cluster store/Store {:tasks 4 :threads 2})
    (rtest/launch-module! cluster module/Sessions {:tasks 4 :threads 2})
    (try (run-checks cluster)
         (catch Throwable t
           (check "The proof ran to its end without a throw." false (str t))
           (.printStackTrace t))))
  (finish))
