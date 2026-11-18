(ns softland.store.setup-proof
  "The proof of `softland.store.setup` on Rama's in-process cluster: the
  first who of a store, its first act, signed acts through the door, and
  the kept first act. It needs Rama. One command, from the repository
  root:

    clojure -M:store-module -m softland.store.setup-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  What it holds. On a store with nothing in it: that the signed making act
  of the base, by a pair that the setup makes, is accepted; that a second
  one, by another pair, and the unsigned system's are refused; that an act
  is accepted as the first who's only when it is signed with the first
  pair's keeper, and that an act that names that who and is signed with
  another keeper, is not signed at all, or is a signed act sent under
  another act-id, is refused; that the first who gives a second who a
  permission, and the second acts with her own keeper and no other; that a
  layer of the streaming gate that a signed act made takes signed acts of
  its holder alone; that the kept first act reads back from its file as it
  was written; and that sending it again changes no row: the answer that
  comes back is the first one, and the next decision of the base has the
  stamp that it would have had. Then, on a second store with nothing in
  it: that the kept first act makes the base for the same who, whose
  keeper then acts there, and that another pair's first act is refused
  there.

  Approach. The proof reads the store through the door alone: each check
  holds the word of an answer, and where it matters its stamp. It opens no
  PState and reads no depot. That the rows of a signed act are the rows
  that the pure functions give is the pure proof's, through the walk in
  memory; this proof shows that the gates on a cluster decide signed acts
  so.

  State. Two in-process clusters, one after the other, each closed when
  its checks end; their files are Rama's and temporary. Three pairs, made
  at the start with the JVM's secure random source, whose private halves
  stay in the memory of this process. One file, the kept first act, under
  the JVM's temporary directory: the proof writes it, reads it and deletes
  it. It draws random numbers, so no two runs sign the same bytes.

  What it does not reach is in the folder's README."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.door :as door]
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.setup :as setup])
  (:import [java.io File]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- check
  "Checks that what was observed is what must be, and prints the check's
  sentence after PASSED or FAILED, so that a run lists what it checked. Gives
  what was observed."
  [what expected observed]
  (println (if (is (= expected observed) what) "PASSED" "FAILED") what)
  observed)

;; ------------------------------------------------------- the proof's own state

(def ^:private tasks "The number of tasks of each of the proof's clusters." 4)

(def ^:private wait-ms
  "The bound that the proof gives a wait for an answer, in milliseconds. An
  act of the base is answered when its batch is committed."
  30000)

(def ^:dynamic ^:private *door*
  "The door of the store that the checks of the moment run on. The hook
  binds it, once for each of the two stores."
  nil)

(defonce ^{:private true
           :doc "The three pairs of the run: the first who's, a second who's and a
  stranger's. Each is what `softland.store.setup/new-pair` gave: a who and a
  keeper."}
  pairs (delay {:first (setup/new-pair) :second (setup/new-pair) :stranger (setup/new-pair)}))

(defonce ^{:private true
           :doc "What one check hands to a later one, by name."}
  kept (atom {}))

(defn- who-of "The who of one of the run's pairs." [which] (:who (get @pairs which)))
(defn- keeper-of "The keeper of one of the run's pairs." [which] (:keeper (get @pairs which)))

(defn- first-own "The base's first permission, the first who's." [] [(who-of :first) :base :base])

(defn- seconds-in-base
  "The second who's permission in the base, beneath the first: see and act."
  []
  [(who-of :second) :base :base (first-own) :see-and-act])

;; ------------------------------------------------------------ through the door

(defn- word
  "Takes what a send of the setup or of this proof gave. Gives one word: the
  door's own word when the door could not; `:accepted`; the gate's reason
  for a refusal; or the answer's own word, `:nothing-yet` or `:not-mine`."
  [result]
  (or (:could-not result)
      (let [answer (:answer result)]
        (case (:answer answer)
          :accepted :accepted
          :refused (:reason answer)
          (:answer answer)))))

(defn- stamp "The stamp of the answer of what a send gave." [result] (:stamp (:answer result)))

(defn- signed-act
  "A maker's signed act with no signature: of version 2, that names `who`
  under `permission` in `layer`, with these facts."
  [who layer permission & facts]
  {:version act/signed-version :who who :layer layer :permission permission
   :session :setup-proof :facts (vec facts)})

(defn- send-signed!
  "Signs an act with the keeper of one of the run's pairs and sends it."
  [which act]
  (setup/send-signed! *door* (keeper-of which) act wait-ms))

(defn- send-as-it-is!
  "Sends a thing through the door as it is, with no signing: an unsigned
  act, or a signed act that keeps the signature that it has. The door
  makes a new act-id for it. Gives the door's word, or the act-id, the
  digest and the answer within the wait."
  [thing]
  (let [sent (door/send-act *door* thing)]
    (if (:could-not sent)
      sent
      {:act-id (:act-id sent)
       :digest (:digest sent)
       :answer (door/answer-of *door* (:act-id sent) (:digest sent) wait-ms)})))

(defn- note
  "A fact with a plain value on a new entity."
  [says]
  {:e (keyword (str "note-" (count (:notes (swap! kept update :notes (fnil conj []) says))))) :k :text :v says})

;; ---------------------------------------------------------------- the checks

(deftest the-first-act-makes-the-base
  (let [made (setup/make-the-base! *door* (:first @pairs) :setup-proof wait-ms)]
    (swap! kept assoc :made made)
    (check "into a store with nothing in it, the signed making act of the base by a new pair is accepted, with the base's first stamp"
           [:accepted 0] [(word made) (stamp made)])
    (check "what was sent is the store's own signed making act for that pair's who, with a signature of 128 hex characters, and its who is a signing who"
           [true true true]
           [(= (dissoc (:act made) :signature) (first-act/signed-making-act :base (who-of :first) :setup-proof))
            (boolean (re-matches #"[0-9a-f]{128}" (:signature (:act made))))
            (act/signing-who? (who-of :first))])
    (shown "the first act of the store, accepted with stamp" (stamp made) "| its who:" (who-of :first)
           "| its act-id:" (pr-str (:act-id made)))))

(deftest no-second-first-act
  (let [strangers (setup/make-the-base! *door* (:stranger @pairs) :setup-proof wait-ms)
        again (setup/make-the-base! *door* (:first @pairs) :setup-proof wait-ms)
        unsigned (send-as-it-is! {:version 1 :who :system :layer :base :permission [:system :base :base] :session :setup-proof
                                  :facts [{:e :base :k :softland.store/depot :v :microbatch}
                                          {:e :system :k :softland.store/permission :v [:system :base :base]}]})]
    (check "after the first act, a right signed making act of the base by another pair is refused: its who holds no permission in a base that is made"
           :permission-does-not-stand (word strangers))
    (check "the first pair's own making act, signed again under a new act-id, is refused too: the base's first permission takes one write"
           :permission-fact-not-beneath-the-acts-own (word again))
    (check "and so is the unsigned making act of the base by the id :system"
           :permission-does-not-stand (word unsigned))
    (shown "after the first act, three more making acts of the base: another pair's" (word strangers)
           "| the first pair's again" (word again) "| the unsigned system's" (word unsigned))))

(deftest only-its-keeper-acts-as-the-first-who
  (let [honest (send-signed! :first (signed-act (who-of :first) :base (first-own) (note "the first who wrote this")))
        forged (send-signed! :stranger (signed-act (who-of :first) :base (first-own) (note "a stranger signed this")))
        unsigned (send-as-it-is! {:version 1 :who (who-of :first) :layer :base :permission (first-own) :session :setup-proof
                                  :facts [(note "nobody signed this")]})
        replayed (send-as-it-is! (:act honest))
        changed (door/send-act *door* (assoc (:act honest) :facts [(note "this was changed after it was signed")]) (:act-id honest))
        changed-answer (door/answer-of *door* (:act-id honest) (:digest changed) wait-ms)
        as-herself (send-signed! :stranger (signed-act (who-of :stranger) :base (first-own) (note "a stranger under the first permission")))]
    (swap! kept assoc :honest honest)
    (check "an act that names the first who and is signed with the first pair's keeper is accepted"
           :accepted (word honest))
    (check "an act that names the first who and is signed with another pair's keeper is refused"
           :signature-does-not-fit (word forged))
    (check "an unsigned act that names the first who, under its permission, is refused"
           :who-needs-a-signature (word unsigned))
    (check "the first who's accepted act, as it was signed, sent again under another act-id is refused: the act-id is inside what was signed"
           :signature-does-not-fit (word replayed))
    (check "a changed copy of that act, sent under its own act-id, gets no decision: the act-id has its answer, and the door says that the answer is not the copy's"
           :not-mine (:answer changed-answer))
    (check "another who, signed with her own keeper, under the first permission is refused"
           :who-not-the-permissions-holder (word as-herself))
    (shown "as the first who: with its keeper" (word honest) "| with another keeper" (word forged) "| unsigned" (word unsigned)
           "| its act under another act-id" (word replayed) "| a changed copy under its own act-id" (:answer changed-answer)
           "| another who under the first permission" (word as-herself))))

(deftest the-first-who-gives-and-a-second-who-acts
  (let [give (send-signed! :first (signed-act (who-of :first) :base (first-own)
                                              {:e (who-of :second) :k :softland.store/permission :v (seconds-in-base)}))
        acts (send-signed! :second (signed-act (who-of :second) :base (seconds-in-base) (note "the second who wrote this")))
        forged (send-signed! :first (signed-act (who-of :second) :base (seconds-in-base) (note "the first who signed as the second")))
        to-plain (send-signed! :first (signed-act (who-of :first) :base (first-own)
                                                  {:e :carol :k :softland.store/permission :v [:carol :base :base (first-own)]}))
        plain (send-as-it-is! {:version 1 :who :carol :layer :base :permission [:carol :base :base (first-own)] :session :setup-proof
                               :facts [(note "a plain id wrote this")]})]
    (check "the first who gives a second who a permission in the base, see and act, with one signed act"
           :accepted (word give))
    (check "the second who acts in the base under her own permission, signed with her own keeper"
           :accepted (word acts))
    (check "an act that names the second who and is signed with the first pair's keeper is refused: to hold the permission above is not to hold her private half"
           :signature-does-not-fit (word forged))
    (check "a signed give to a plain id is refused, and an unsigned act of that id under the permission that was not given is refused"
           [:gives-to-a-who-with-no-public-half :permission-does-not-stand] [(word to-plain) (word plain)])
    (shown "the first who gives a second who see and act:" (word give) "| the second who acts with her keeper:" (word acts)
           "| the first pair signs as the second who:" (word forged) "| a give to a plain id:" (word to-plain))))

(deftest a-layer-of-the-streaming-gate-made-by-a-signed-act
  (let [team-own [(who-of :second) :team :team]
        made (send-signed! :second (first-act/signed-making-act :team (who-of :second) :setup-proof))
        acts (send-signed! :second (signed-act (who-of :second) :team team-own {:e :team-note :k :text :v "the holder wrote this"}))
        forged (send-signed! :first (signed-act (who-of :second) :team team-own {:e :team-forged :k :text :v "the first pair signed this"}))
        outsider (send-signed! :first (signed-act (who-of :first) :team [(who-of :first) :team :team] {:e :team-forged :k :text :v "no permission here"}))
        unsigned (send-as-it-is! {:version 1 :who (who-of :second) :layer :team :permission team-own :session :setup-proof
                                  :facts [{:e :team-forged :k :text :v "nobody signed this"}]})]
    (check "a layer of the streaming gate is made by the signed making act of its holder, and its holder acts in it with her keeper"
           [:accepted :accepted] [(word made) (word acts)])
    (check "in that layer an act that names its holder and is signed with another keeper is refused, an unsigned act that names her is refused, and the base's first who holds no permission there"
           [:signature-does-not-fit :who-needs-a-signature :permission-does-not-stand]
           [(word forged) (word unsigned) (word outsider)])
    (shown "a streaming layer made by a signed act: made" (word made) "| its holder acts" (word acts) "| another keeper as its holder"
           (word forged) "| unsigned as its holder" (word unsigned) "| the base's first who, who holds nothing there" (word outsider))))

(deftest the-kept-first-act
  (let [made (:made @kept)
        file (File/createTempFile "softland-first-act" ".edn")
        path (.getPath file)]
    (try
      (let [written (setup/keep-first-act! path made)
            read-back (setup/kept-first-act path)
            before (send-signed! :first (signed-act (who-of :first) :base (first-own) (note "before the first act is sent again")))
            again (setup/send-again! *door* read-back wait-ms)
            after (send-signed! :first (signed-act (who-of :first) :base (first-own) (note "after the first act was sent again")))]
        (swap! kept assoc :first-act read-back)
        (check "the kept first act is written to a file and reads back as it was written: its act-id, and the signed act as its maker gave it, the seven parts that she filled and no other"
               [path true #{:act :act-id} #{:version :who :layer :permission :session :facts :signature}]
               [written (= (select-keys made [:act :act-id]) read-back) (set (keys read-back)) (set (keys (:act read-back)))])
        (check "the file's text is EDN that reads to the same map, and a file that is not there gives nil"
               [true nil]
               [(= read-back (edn/read-string (slurp path))) (setup/kept-first-act (str path ".not-there"))])
        (check "the kept first act, sent again to the store that holds it, is answered with its first answer: accepted, with the base's first stamp"
               [:accepted 0] [(word again) (stamp again)])
        (check "the second send was not decided: the base's next decision has the stamp that follows the decision before the send"
               (inc (stamp before)) (stamp after))
        (shown "the kept first act, sent again:" (word again) "with stamp" (stamp again)
               "| the base's decisions before and after the send have the stamps" (stamp before) "and" (stamp after)))
      (finally (.delete file)))))

(deftest a-store-with-nothing-in-it-takes-the-kept-first-act
  (let [again (setup/send-again! *door* (:first-act @kept) wait-ms)
        acts (send-signed! :first (signed-act (who-of :first) :base (first-own) {:e :note-in-the-second-store :k :text :v "the first who wrote this"}))
        strangers (setup/make-the-base! *door* (:stranger @pairs) :setup-proof wait-ms)
        second-who (send-signed! :second (signed-act (who-of :second) :base (seconds-in-base) {:e :note-of-the-second :k :text :v "no give was made here"}))]
    (check "a second store with nothing in it takes the kept first act: it is decided there, accepted, with the base's first stamp"
           [:accepted 0] [(word again) (stamp again)])
    (check "in that store the same who acts with the same keeper, another pair's first act is refused, and the second who of the first store holds nothing: a store holds only what its own line gave"
           [:accepted :permission-does-not-stand :permission-does-not-stand]
           [(word acts) (word strangers) (word second-who)])
    (shown "the kept first act in a second store with nothing in it:" (word again) "| the first who acts there:" (word acts)
           "| another pair's first act:" (word strangers) "| the second who of the first store:" (word second-who))))

(defn- on-a-new-store
  "Launches the module on a new in-process cluster, opens a door on it,
  runs the checks given with that door bound, and closes the cluster."
  [checks]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (binding [*door* (door/open ipc (rama/get-module-name module/Store))]
      (run! (fn [a-check] (a-check)) checks))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them, on two
  stores one after the other. Later checks build on what earlier ones
  made; the last runs on a second store with nothing in it, with the
  first act that the first store's checks kept."
  []
  (on-a-new-store [the-first-act-makes-the-base
                   no-second-first-act
                   only-its-keeper-acts-as-the-first-who
                   the-first-who-gives-and-a-second-who-acts
                   a-layer-of-the-streaming-gate-made-by-a-signed-act
                   the-kept-first-act])
  (on-a-new-store [a-store-with-nothing-in-it-takes-the-kept-first-act]))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.setup-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
