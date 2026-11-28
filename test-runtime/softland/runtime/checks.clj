(ns softland.runtime.checks
  "The checks of the runtime as a system outside it meets it: over real
  requests to its pages, and over the line with the command line's own
  client, as a program and, for the words that only she may say there, as
  her browser. It requires the client library, the wire's forms and the proofs'
  driver, and nothing of the runtime: it is handed addresses.

  Two proofs run these same checks. `softland.runtime.proof` starts three
  copies of the runtime in its own process, on Rama's in-process cluster.
  `softland.runtime.cluster-proof` starts them as three processes on a
  cluster that stays up, and then kills some. So each sentence here holds
  across copies of the runtime whichever way they are run.

  What the caller hands `run!`:

    :a :b     the addresses of two copies of the runtime with the seam's
              default numbers
    :c        the address of a copy whose secrets, sessions and previous
              secrets last a few seconds (`short-numbers`)
    :first    the account of the first person, whom setup made:
              {:sub :email :name}
    :left     a function of no argument that gives what the copies still
              hold open, {:live-reads n :watched n}, or nil when the proof
              cannot see it
    :tag      a text that is in no account of an earlier run: the accounts
              that the checks make carry it, so that the checks can be run
              again on a cluster that keeps what an earlier run wrote

  Each copy runs with the stand-in for Google, so an account is whatever
  the form says; what makes an account a person is an invitation.

  State. It writes people, lanes and acts into the store and Auth that the
  copies stand on."
  (:require [clojure.string :as str]
            [softland.command.client :as client]
            [softland.runtime.drive :as drive]
            [softland.runtime.permissions :as perm]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check]]
            [softland.wire.words :as words])
  (:import [java.net.http WebSocket]
           [java.util UUID]
           [java.util.concurrent CompletableFuture]))

(def short-numbers
  "The seam's numbers of the copy `:c`: a secret lasts six seconds, the
  secret before it one second, a session four, and an invitation four."
  {:secret-ms 6000 :previous-ms 1000 :session-ms 4000 :invitation-ms 4000})

;; ------------------------------------------------------------------- helpers

(defn- wait-for
  "Calls a function of no argument until it gives a thing that is truthy
  or the span passes. Gives the thing, or nil."
  [f ms]
  (let [ends (+ (System/currentTimeMillis) ms)]
    (loop []
      (or (f)
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 50)
            (recur))))))

(defn- sleep-until
  "Waits until a moment of the clock, in milliseconds since 1970, and gives
  at once when it has passed. The copies run on this machine, so a time
  that a copy reports is on the same clock."
  [moment]
  (let [ms (- (long moment) (System/currentTimeMillis))]
    (when (pos? ms) (Thread/sleep ms))))

(defn- line
  "Opens the line at an address. Takes the address, a credential's text or
  nil, and a session's id or nil. Gives {:line the connection :hello what
  the runtime said first}, or the word with which the opening failed."
  [url credential session]
  (let [connection (client/connect {:url url :credential credential :session session})]
    (if (:could-not connection)
      connection
      {:line connection :hello (client/hello connection)})))

(defn- close-line!
  "Closes a line that `line` opened; nothing for one that did not open.
  Gives nil."
  [opened]
  (when-let [connection (:line opened)]
    (client/close! connection)))

(defn- say
  "Says a word on an opened line and waits for its result."
  [{:keys [line]} word]
  (client/say line word 40000))

(defn- raw-frame!
  "Writes a form as one frame on an opened line's socket, past the library:
  the library reads its own frame before it sends it, and sends none that
  the runtime could not read."
  [{:keys [line]} form]
  (.join ^CompletableFuture (.sendText ^WebSocket (:socket line) (frame/write form) true)))

(defn- act-of
  "Builds one act of one fact. Takes a hello, the lane, the permission
  that the act names, and the fact's three parts."
  [hello lane permission e k v]
  {:version 1 :who (:who hello) :session (:session hello) :lane lane :permission (perm/permission-id permission)
   :facts [{:e e :k k :v v}]})

(defn- place
  "Reads one place as the line's caller. Gives the read's answer."
  [opened lane e k]
  (say opened {:word :read :query {:asks [:place e k] :asker (:who (:hello opened)) :lanes [lane]
                                   :with #{:writer}}}))

(defn- watching
  "Opens a watch of one place on a line. Gives {:id the watch's number
  :seen an atom of the messages that came}."
  [{:keys [line hello]} lane e k]
  (let [seen (atom [])]
    {:seen seen
     :id (client/watch line {:asks [:place e k] :asker (:who hello) :lanes [lane]}
                       (fn [message] (swap! seen conj message)))}))

(defn- value-seen
  "Takes what a watch saw. Gives the value of the place after its last
  message that held one."
  [seen]
  (some (fn [message]
          (or (get-in message [:anew :place :value])
              (some (fn [[_ place-answer]] (:value place-answer)) (:changed message))))
        (reverse seen)))

;; ----------------------------------------------------------------- the checks

(defn run!
  "Runs every check against the copies that the caller hands it, as the
  namespace docstring says. Gives nil; each check prints and counts
  itself."
  [{:keys [a b c left tag] first-person :first}]
  (let [left (or left (fn [] nil))
        account (fn [n word]
                  {:sub (str tag "-" n) :email (str word "-" tag "@example.com") :name (str/capitalize word)})]

    ;; ------------------------------------------------ who may become a person
    (let [stranger (drive/sign-in a (account 999 "stranger"))]
      (check "An account that proves no invitation is turned away with 403 and gets no cookie."
             (and (= 403 (:status stranger)) (nil? (:cookie stranger))) stranger))

    (let [first-in (drive/sign-in a (assoc first-person :call-her "Sid"))
          again (drive/sign-in b first-person)
          cookie (:cookie first-in)]
      (check "The first person's first sign-in gives her browser a credential, and sends her to the box, which offers her provider's name for her."
             (and (seq cookie) (:named? first-in) (= (:name first-person) (:offered first-in)) (= 303 (:name-status first-in)))
             (dissoc first-in :cookie))
      (check "A later sign-in of the same account, through another copy of the runtime, finds the same person and does not send her to the box again."
             (and (= (:person first-in) (:person again)) (false? (:named? again)))
             (dissoc again :cookie))

      ;; ------------------------------------------------- the two carriers
      (check "A browser's credential in the header of a program stands for nobody: the line is not opened, with 401."
             (= 401 (:status (line a cookie nil))))

      ;; ------------------------------------------------ a program's sign-in
      (let [refused (drive/program a "a program that she refuses"
                                   (fn [code] (drive/yes a cookie code {:say "no"})))
            foreign (drive/program a "a program whose yes comes from another site"
                                   (fn [code] (drive/yes a cookie code {:origin "http://evil.example"})))
            mine (drive/program a "the proof's command line"
                                (fn [code] (drive/yes b cookie code {:agent "the proof's command line"})))
            poll (fn [form] (second (frame/read-one (:body (drive/request :post (str a "/sign-in/program/poll") {:edn form})))))
            wrong (poll {:code "ABCD2345" :asking-secret (credential/make-secret)})
            ;; An ask that nobody answers, by the two requests themselves.
            secret (credential/make-secret)
            asking-secret (credential/make-secret)
            ask (fn [form] (second (frame/read-one (:body (drive/request :post (str a "/sign-in/program") {:edn form})))))
            waiting (ask {:hash (credential/hash-of secret) :asking-hash (credential/hash-of asking-secret)
                          :asks "a program that nobody answers"})
            with-the-credential's (poll {:code (:code waiting) :asking-secret secret})
            with-another (poll {:code (:code waiting) :asking-secret (credential/make-secret)})
            with-its-own (poll {:code (:code waiting) :asking-secret asking-secret})
            in-the-old-form (poll {:code (:code waiting) :secret secret})
            one-secret (ask {:hash (credential/hash-of secret) :asking-hash (credential/hash-of secret)
                             :asks "a program with one secret"})
            one-hash (ask {:hash (credential/hash-of secret) :asks "a program of before"})]
        (check "A program whose ask she answers with no is told no, and has no credential."
               (= {:state :no} refused) refused)
        (check "Her yes is taken only from this runtime's own page: a post from another site leaves the program waiting."
               (= :waiting (:state foreign)) foreign)
        (check "A program whose ask she answers with yes, at another copy than the one it asked, is told the three parts of its credential, and its actor is an agent of hers and not she: no program is a person."
               (and (:credential mine) (= (:person first-in) (:root mine)) (uuid? (:actor mine)) (not= (:root mine) (:actor mine)))
               (dissoc mine :credential))
        (let [code (atom nil)
              posted (atom nil)
              as-her (drive/program a "a program that asks to be she"
                                    (fn [shown]
                                      (reset! code shown)
                                      (reset! posted [(drive/request :post (str a "/yes")
                                                                     {:cookie cookie :origin a :form (drive/yes-form shown {:as-herself true})})
                                                      (drive/request :post (str a "/yes")
                                                                     {:cookie cookie :origin a :form (drive/yes-form shown {:mine (:person first-in)})})])))
              [as-me as-her-id] @posted]
          (check "A yes that names her for a program is refused, at the page and in Auth: the page does not take a yes as herself, nor offers one, and a yes that names her own id as an agent of hers is refused by Auth as a person's; the program still waits."
                 (and (= 400 (:status as-me))
                      (= 409 (:status as-her-id)) (str/includes? (:body as-her-id) "never signed in as you")
                      (= :waiting (:state as-her))
                      (not (str/includes? (:body (drive/request :get (str a "/yes?code=" @code) {:cookie cookie})) "value=\"me\"")))
                 [(select-keys as-me [:status :body]) (select-keys as-her-id [:status :body]) as-her]))
        (check "A program that asks about a code with a secret that is not the ask's learns nothing: no ask is found."
               (= :find-the-ask (:could-not wrong)) wrong)
        (check "A program that waits is answered against its secret for asking alone: with it the ask is said to wait; with the secret that becomes its credential in that place, the answer is the one that a wrong secret and an unknown code get, :find-the-ask."
               (and (= :waiting (:state with-its-own))
                    (= [:find-the-ask :find-the-ask] [(:could-not with-the-credential's) (:could-not with-another)])
                    (= with-the-credential's with-another wrong))
               [with-its-own with-the-credential's with-another])
        (check "The runtime takes no ask that would put the credential's secret on the wire before her yes, and no poll that carries a secret under the name of before: an ask whose two hashes are one, an ask with one hash, and a poll in the form {:code :secret} are each not read."
               (= [:read-the-word :read-the-word :read-the-word]
                  [(:could-not one-secret) (:could-not one-hash) (:could-not in-the-old-form)])
               [one-secret one-hash in-the-old-form])
        (check "A program's credential in a browser's cookie stands for nobody: with it she is not signed in at the pages, and it says no yes."
               (let [other (drive/program a "a program that waits"
                                          (fn [code] (drive/yes a (:credential mine) code {})))]
                 (= :waiting (:state other))))

        ;; ------------------------------------- what a program is lent at her yes
        (let [her (drive/browser-line a cookie)
              her-who (:who (:hello her))
              her-lane (:own-lane (:hello her))
              lend-of (fn [p] (:permission (credential/lent-at-yes (:root p) (:sign-in p))))
              post-yes (fn [code answer] (drive/request :post (str a "/yes")
                                                        {:cookie cookie :origin a :form (drive/yes-form code answer)}))
              pages (atom {})
              signed (fn [asks answer]
                       (drive/program a asks (fn [code]
                                               (swap! pages assoc asks (post-yes code answer)))))
              both (signed "a program lent both" {:agent "lent both"})
              looker (signed "a program lent to see" {:agent "a looker" :write false})
              writer (signed "a program lent to write" {:agent "a writer" :see false})
              idle (signed "a program lent nothing" {:agent "an idle one" :see false :write false})
              twice-code (atom nil)
              twice (drive/program a "a program whose yes is posted twice"
                                   (fn [code]
                                     (reset! twice-code code)
                                     (swap! pages assoc :twice [(post-yes code {:agent "twice"}) (post-yes code {:agent "twice"})])))
              changed-code (atom nil)
              changed (drive/program a "a program whose page is posted again with another box ticked"
                                     (fn [code]
                                       (reset! changed-code code)
                                       (swap! pages assoc :changed [(post-yes code {:agent "changed" :write false})
                                                                    (post-yes code {:agent "changed"})])))
              other-browser (:cookie again)
              _elsewhere (drive/program a "a program whose page is posted again from her other browser"
                                        (fn [code]
                                          (swap! pages assoc :elsewhere
                                                 [(post-yes code {:agent "elsewhere"})
                                                  (drive/request :post (str a "/yes")
                                                                 {:cookie other-browser :origin a
                                                                  :form (drive/yes-form code {:agent "elsewhere"})})])))
              ;; A program writes under the lend of its own sign-in, or under
              ;; the one that is named: an agent signed in again holds the lend
              ;; of its first sign-in.
              try-it (fn try-it
                       ([p n] (try-it p n (lend-of p)))
                       ([p n lend]
                       (let [opened (line a (:credential p) nil)
                             hello (:hello opened)
                             wrote (say opened {:word :send :act (act-of hello her-lane lend (keyword (str "lent-" n)) :v n)
                                                :act-id (UUID/randomUUID)})
                             seen (:value (:place (place opened her-lane :lend-shelf :colour)))]
                         (close-line! opened)
                         {:who (:who hello) :wrote (get-in wrote [:answer :answer]) :seen seen})))
              _ (say her {:word :send :act (act-of (:hello her) her-lane (perm/first-permission her-who her-lane) :lend-shelf :colour :green)
                          :act-id (UUID/randomUUID)})
              tried (into {} (map (fn [[k p n]] [k (try-it p n)])) [[:both both 1] [:looker looker 2] [:writer writer 3] [:idle idle 4]])
              body (fn [asks] (:body (get @pages asks)))]
          (check "A program that she approves as a new agent, with both boxes ticked as the page offers them, writes in her own lane under the agent's own who and the permission that her yes lent it, and sees her lane; the page says what it was lent."
                 (and (= (perm/agent (:actor both)) (:who (:both tried))) (not= her-who (:who (:both tried)))
                      (= :accepted (:wrote (:both tried))) (= :green (:seen (:both tried)))
                      (= (:who (:both tried)) (:writer (:place (place her her-lane :lent-1 :v))))
                      (str/includes? (body "a program lent both") "lent to see and to write"))
                 [(:both tried) (body "a program lent both")])
          (check "With a box unticked the program cannot do that thing: lent to see alone, its write is refused and it sees; lent to write alone, it writes and sees nothing; lent nothing, it is signed in, its write is refused and it sees nothing, and the page says that it holds nothing."
                 (and (= [:refused :green] ((juxt :wrote :seen) (:looker tried)))
                      (= [:accepted nil] ((juxt :wrote :seen) (:writer tried)))
                      (= [:refused nil] ((juxt :wrote :seen) (:idle tried)))
                      (str/includes? (body "a program lent nothing") "holds nothing"))
                 [tried (body "a program lent nothing")])
          (check "The lend is her own act: the permission that a program reads under its lend's id is held by its agent, allows what she ticked, covers her lane, is given by her and lent from her first permission, and she is its writer."
                 (let [read-it (fn [p] (say her {:word :read :query {:asks [:place (lend-of p) :softland.store/permission]
                                                                      :asker her-who :lanes [her-lane]
                                                                      :stores-own-facts true :with #{:writer}}}))
                       first-id (perm/permission-id (perm/first-permission her-who her-lane))
                       of-looker (:place (read-it looker))]
                   (and (= {:holder (perm/agent (:actor looker)) :allows {:see true} :covers [:default-group her-lane]
                            :given-by her-who :under first-id :lent-from first-id}
                           (:value of-looker))
                        (= her-who (:writer of-looker))
                        (nil? (:value (:place (read-it idle)))))))
          (check "Her page posted twice lends once: both posts say that the program is lent what she ticked, and the program writes under that one lend."
                 (let [[one two] (:twice @pages)]
                   (and (= [200 200] [(:status one) (:status two)])
                        (str/includes? (:body two) "lent to see and to write")
                        (= :accepted (:wrote (try-it twice 5)))))
                 (mapv #(select-keys % [:status :body]) (:twice @pages)))
          (let [[lent-first posted-again] (:changed @pages)
                [_ from-elsewhere] (:elsewhere @pages)
                tried-changed (try-it changed 7)
                _ended (say her {:word :send
                                 :act (act-of (:hello her) her-lane (perm/first-permission her-who her-lane)
                                              (lend-of changed) :softland.store/permission nil)
                                 :act-id (UUID/randomUUID)})
                after-end (post-yes @changed-code {:agent "changed" :write false})
                bodies (map :body [posted-again from-elsewhere after-end])]
            (check "After her yes the page says what her lend holds as it stands, whatever the send answered: the same page posted again with another box ticked is answered 409, with what the agent is lent, to see, and how to change it, and its write is still refused; posted again from her other browser sign-in, 200, lent to see and to write; posted again after she ended the lend, 409, lent nothing. No answer says to lend it later."
                   (and (= [200 409 200 409] (mapv :status [lent-first posted-again from-elsewhere after-end]))
                        (str/includes? (:body posted-again) "it is lent to see in your own lane")
                        (str/includes? (:body posted-again) "start the program's sign-in again as this agent")
                        (= [:refused :green] ((juxt :wrote :seen) tried-changed))
                        (str/includes? (:body from-elsewhere) "lent to see and to write")
                        (str/includes? (:body after-end) "this page has lent it nothing")
                        (not-any? #(str/includes? % "later") bodies))
                   [(mapv #(select-keys % [:status :body]) [lent-first posted-again from-elsewhere after-end]) tried-changed]))
          (let [looked-at (let [shown (atom nil)]
                            ;; An ask that waits, which she only looks at; the
                            ;; driver waits while the runtime paces this address.
                            (drive/program a "a program that she looks at" (fn [code] (reset! shown code)))
                            @shown)
                page (:body (drive/request :get (str a "/yes?code=" looked-at) {:cookie cookie}))
                again (signed "the agent that was lent both, again" {:mine (:actor both)})
                woken (signed "the idle agent, again, lent to see" {:mine (:actor idle) :see true})
                tried-again (try-it again 6 (lend-of both))
                first-stands (line a (:credential both) nil)
                named-lent-both (fn [] (count (filter #(= "lent both" (:label %)) (:actors (say her {:word :under})))))
                before (named-lent-both)
                answered-as-mine (drive/program a "a program answered as an agent of hers, then by the form of a new agent of that name"
                                                (fn [code]
                                                  (swap! pages assoc :answered-as-mine
                                                         [(post-yes code {:mine (:actor both)}) (post-yes code {:agent "lent both"})])))
                _ended (say her {:word :end-agent :agent (:actor writer)})
                cannot (drive/program a "a program answered as an agent that ended, then as one that is not hers"
                                      (fn [code]
                                        (swap! pages assoc :cannot
                                               [(post-yes code {:mine (:actor writer) :see true})
                                                (post-yes code {:mine (UUID/randomUUID) :see true})])))]
            (close-line! first-stands)
            (check "A yes is judged by the actor that it named, not by a name: on one page, a yes as an agent of hers with no box ticked, then the form of a new agent of that agent's name with both boxes, is answered 409, answered before; it makes no agent and lends nothing."
                   (let [[as-mine as-new] (:answered-as-mine @pages)
                         lend (say her {:word :read :query {:asks [:place (lend-of answered-as-mine) :softland.store/permission]
                                                            :asker her-who :lanes [her-lane] :stores-own-facts true}})]
                     (and (= [200 409] (mapv :status [as-mine as-new]))
                          (str/includes? (:body as-new) "already answered before")
                          (= (:actor both) (:actor answered-as-mine))
                          (= before (named-lent-both))
                          (nil? (:value (:place lend)))))
                   (mapv #(select-keys % [:status :body]) (:answered-as-mine @pages)))
            (check "A yes by hand as an agent of hers that ended, or as an agent that is not hers, is answered 409: that agent cannot be signed in, and she can choose again; the program still waits."
                   (and (= [409 409] (mapv :status (:cannot @pages)))
                        (every? #(and (str/includes? (:body %) "cannot be signed in") (str/includes? (:body %) "choose again"))
                                (:cannot @pages))
                        (= :waiting (:state cannot)))
                   [(mapv #(select-keys % [:status :body]) (:cannot @pages)) cannot])
            (check "The yes page lists her live agents by name, each to be signed in again, with both boxes unticked, and says that such an agent keeps what it holds."
                   (and (str/includes? page (str "value=\"" (:actor both) "\""))
                        (str/includes? page "lent both")
                        (str/includes? page "It keeps what it holds")
                        (str/includes? page "name=\"as\" value=\"my-agent\"")
                        (= 4 (count (re-seq #"type=\"checkbox\"" page)))
                        (= 2 (count (re-seq #"value=\"yes\" checked>" page))))
                   page)
            (check "The same agent signed in a second time, with no box ticked, is a new sign-in of that agent that holds what the agent held: it writes under the first lend, the page says that it keeps what it holds, and the first sign-in stands."
                   (and (= (:actor both) (:actor again)) (not= (:sign-in both) (:sign-in again))
                        (= (:who (:both tried)) (:who tried-again)) (= :accepted (:wrote tried-again))
                        (str/includes? (body "the agent that was lent both, again") "keeps what it holds")
                        (uuid? (:session (:hello first-stands))))
                   [tried-again (body "the agent that was lent both, again") (:hello first-stands)])
            (check "An agent of hers signed in again with a box ticked is lent that besides what it holds: the agent that was lent nothing then sees her lane, under the second lend."
                   (let [opened (line a (:credential woken) nil)
                         seen (:value (:place (place opened her-lane :lend-shelf :colour)))]
                     (close-line! opened)
                     (and (= (:actor idle) (:actor woken)) (= :green seen)))))
          (close-line! her))

        (let [me (line a (:credential mine) nil)
              hello (:hello me)
              who (:who hello)
              lane (:own-lane hello)
              own (perm/lent-at-yes hello lane)
              she (perm/person (:person first-in))]
          (check "The line says hello with who the program is, the agent of hers that her yes made, her root, a new session and her own lane; and a connection with no credential is a visitor."
                 (and (= (perm/agent (:actor mine)) who) (= (:person first-in) (:root hello)) (uuid? (:session hello)) (uuid? lane)
                      (= {:visitor true} (:hello (line a nil nil))))
                 hello)
          (check "Her name is a fact on her own id in her own lane, and she is its writer: the box's act was hers, and the runtime wrote no who into it."
                 (let [answer (place me lane she :person/name)]
                   (and (= "Sid" (:value (:place answer))) (= she (:writer (:place answer)))))
                 (place me lane she :person/name))

          ;; ------------------------------------------------------- an act
          (let [uuid (UUID/randomUUID)
                act (act-of hello lane own :chair-7 :colour :green)
                sent (say me {:word :send :act act :act-id uuid})
                other (line b (:credential mine) (:session hello))
                again (say other {:word :send :act act :act-id uuid})
                another (say other {:word :send :act (act-of hello lane own :chair-7 :colour :red) :act-id uuid})]
            (check "A send is one round trip: the whole act goes in with the maker's UUID, and the door's receipt comes back with the answer that a gate gave, accepted. Its act-id is the act's lane and the maker's UUID, and names no depot."
                   (and (= :accepted (get-in sent [:answer :answer])) (= [lane uuid] (:act-id sent)) (string? (:digest sent)))
                   sent)
            (check "The same act sent again under the same UUID, through another copy of the runtime and in the same session, has the same digest and reads the one answer: it is taken once."
                   (and (= (:digest sent) (:digest again)) (= (:answer sent) (:answer again)))
                   [sent again])
            (check "Another act under the used UUID is not hers to read: the answer says not mine, and the place holds what the first act wrote."
                   (and (= :not-mine (get-in another [:answer :answer]))
                        (= :green (:value (:place (place me lane :chair-7 :colour)))))
                   another)
            (check "A program that names the session that it was in stays in it at another copy; one that names a session that is not its own is not let in."
                   (and (= (:session hello) (:session (:hello other)))
                        (= 401 (:status (line b (:credential mine) (UUID/randomUUID)))))
                   (:hello other)))

          ;; --------------------------------------------- nobody acts for another
          (let [uuid (UUID/randomUUID)
                as-system (say me {:word :send :act (assoc (act-of hello lane own :chair-7 :colour :blue) :who :system)
                                   :act-id uuid})
                as-session (say me {:word :send :act (assoc (act-of hello lane own :chair-7 :colour :blue) :session (UUID/randomUUID))
                                    :act-id uuid})
                unsent (say me {:word :answer :act-id [lane uuid] :digest (apply str (repeat 64 "0"))})
                as-asker (say me {:word :read :query {:asks [:place :chair-7 :colour] :asker :system :lanes [lane]}})
                visitor (line a nil nil)]
            (check "An act that names another who than its sender, or another session than the one it is sent in, is refused by the runtime, and nothing is sent for it: nothing is under its UUID."
                   (= [{:could-not :send-as-another :at :who} {:could-not :send-as-another :at :session} {:answer :nothing-yet}]
                      [as-system as-session unsent])
                   [as-system as-session unsent])
            (check "A query that names another asker than its sender is refused, and a visitor's query that names any asker is refused."
                   (= [{:could-not :send-as-another :at :asker} {:could-not :send-as-another :at :asker}]
                      [as-asker (say visitor {:word :read :query {:asks [:place :chair-7 :colour] :asker who :lanes [lane]}})]))
            (check "A visitor sends no act, and reads her lane as somebody who may not see it."
                   (and (= :find-her-sign-in (:could-not (say visitor {:word :send :act (act-of hello lane own :a :b 1)
                                                                       :act-id (UUID/randomUUID)})))
                        (nil? (:value (:place (say visitor {:word :read :query {:asks [:place :chair-7 :colour]
                                                                                :not-signed-in true :lanes [lane]}}))))))
            (check "An act of a wrong form is sent all the same, and a gate refuses it under its act-id: the log keeps it."
                   (= :refused (get-in (say me {:word :send :act (assoc (act-of hello lane own :a :b 1) :version 99)
                                                :act-id (UUID/randomUUID)})
                                       [:answer :answer]))))

          ;; ------------------------------------------------------- a series
          (let [acts (mapv (fn [n] {:act (act-of hello lane own :counter :n n) :act-id (UUID/randomUUID)}) (range 5))
                sent (say me {:word :send-all :acts acts})]
            (check "A series of acts is sent in order and gives a result for each, and the last of them is what a read gives."
                   (and (= 5 (count (:results sent)))
                        (every? (fn [r] (= :accepted (get-in r [:answer :answer]))) (:results sent))
                        (= 4 (:value (:place (place me lane :counter :n)))))
                   sent))

          ;; ------------------------------------------- one line, many watches
          (let [one (watching me lane :chair-7 :colour)
                two (watching me lane :lamp-2 :lit)
                first-colour (wait-for (fn [] (value-seen @(:seen one))) 10000)]
            (say me {:word :send :act (act-of hello lane own :lamp-2 :lit true) :act-id (UUID/randomUUID)})
            (say me {:word :send :act (act-of hello lane own :chair-7 :colour :amber) :act-id (UUID/randomUUID)})
            (check "Two watches on one connection each give the whole answer first and then the change of their own place, while words go on being answered on the same connection."
                   (and (= :green first-colour)
                        (wait-for (fn [] (= :amber (value-seen @(:seen one)))) 10000)
                        (wait-for (fn [] (true? (value-seen @(:seen two)))) 10000)
                        (contains? (first @(:seen one)) :anew)
                        (not-any? (fn [message] (some (fn [[[e _] _]] (= :lamp-2 e)) (:changed message))) @(:seen one)))
                   {:one @(:seen one) :two @(:seen two)})
            (check "A watch that is left is told nothing more, and its number is free: a leave of it a second time finds no watch."
                   (let [seen-before (count @(:seen two))]
                     (and (= {:left (:id two)} (client/leave (:line me) (:id two)))
                          (do (say me {:word :send :act (act-of hello lane own :lamp-2 :lit false) :act-id (UUID/randomUUID)})
                              (Thread/sleep 500)
                              (= seen-before (count @(:seen two))))
                          (= :find-the-watch (:could-not (client/leave (:line me) (:id two)))))))

            ;; ------------------------------------------ a session that ends
            (let [twin (line b (:credential mine) (:session hello))]
              (check "A session that is closed through one copy of the runtime ends the connection that another copy holds in it: that connection is told why, its watch is told an end, and a word on it reaches nobody."
                     (and (= {:closed (:session hello)} (say twin {:word :close-session}))
                          (wait-for (fn [] (= :find-her-session (:could-not (client/bye (:line me))))) 10000)
                          (wait-for (fn [] (some :ended @(:seen one))) 10000)
                          (:could-not (say me {:word :who-am-i})))
                     {:bye (client/bye (:line me)) :seen (last @(:seen one))})
              (check "A closed session is not entered again, and the sign-in that it was under opens a new one."
                     (and (= 401 (:status (line a (:credential mine) (:session hello))))
                          (uuid? (:session (:hello (line a (:credential mine) nil))))))
              (check "No live read and no watched root is left behind by connections that ended."
                     (let [nothing {:live-reads 0 :watched 0}]
                       (or (nil? (left))
                           (wait-for (fn [] (let [now (left)] (when (= 0 (:live-reads now)) true))) 10000)))
                     (left))))

          ;; ------------------------------------------------------- an agent
          ;; `me` is the proof's command line, an agent of hers; what only she
          ;; may say is said at her browser, `her`.
          (let [me (line a (:credential mine) nil)
                her (drive/browser-line a cookie)
                hello (:hello me)
                secret (credential/make-secret)
                uuid (UUID/randomUUID)
                made (say me {:word :make-agent :uuid uuid :label "importer" :hash (credential/hash-of secret)})
                made-again (say me {:word :make-agent :uuid uuid :label "importer" :hash (credential/hash-of secret)})
                agent-credential (credential/text (:root made) (:sign-in made) secret)
                agent (line b agent-credential nil)
                agent-hello (:hello agent)
                agent-own (perm/first-permission (:who agent-hello) lane {:id (UUID/randomUUID)})
                before (say agent {:word :send :act (act-of agent-hello lane agent-own :crate-1 :packed true)
                                   :act-id (UUID/randomUUID)})
                given (perm/given-permission (:who agent-hello) lane own :see-and-act)
                gave (say me {:word :send
                              :act {:version 1 :who who :session (:session hello) :lane lane :permission (perm/permission-id own)
                                    :facts [(perm/permission-fact given)]}
                              :act-id (UUID/randomUUID)})
                after (say agent {:word :send :act (act-of agent-hello lane given :crate-1 :packed true)
                                  :act-id (UUID/randomUUID)})]
            (check "An agent that her program makes has a who of its own, which is neither her id, nor the program's, nor the UUID that her program sent, and the same UUID again makes the same agent."
                   (and (uuid? (:made made)) (not= (:root hello) (:made made)) (not= (:actor mine) (:made made)) (not= uuid (:made made))
                        (= (:made made) (:made made-again)) (= (:root hello) (:root made)))
                   made)
            (check "The agent signs in as a program with its own credential: the line says that it is the agent, beneath her as its root, and gives it her own lane's id."
                   (and (= (perm/agent (:made made)) (:who agent-hello)) (= (:root hello) (:root agent-hello)) (= lane (:own-lane agent-hello)))
                   agent-hello)
            (check "An agent acts only under what it was given: before her program lends it a permission, from what her yes lent the program, a gate refuses its act, and after, a gate accepts it and the fact names the agent as its writer."
                   (and (= :refused (get-in before [:answer :answer]))
                        (= :accepted (get-in gave [:answer :answer]))
                        (= :accepted (get-in after [:answer :answer]))
                        (= (:who agent-hello) (:writer (:place (place me lane :crate-1 :packed)))))
                   [before gave after])
            (check "A word that is a person's own is refused to an agent: it invites nobody and lists nothing of her sign-ins."
                   (= [:do-that-as-an-agent :do-that-as-an-agent]
                      [(:could-not (say agent {:word :invite :uuid (UUID/randomUUID) :claim {:by :email :email (:email (account 5 "nobody"))}}))
                       (:could-not (say agent {:word :under}))]))
            (let [claim {:by :email :email (:email (account 5 "nobody"))}
                  said (into {} (map (fn [[k word]] [k (:could-not (say me word))]))
                             {:invite {:word :invite :uuid (UUID/randomUUID) :claim claim}
                              :invite-herself {:word :invite :uuid (UUID/randomUUID) :claim claim :herself true}
                              :take-back-invite {:word :take-back-invite :claim claim}
                              :accounts {:word :accounts}
                              :unlink-account {:word :unlink-account :account [:google (str tag "-5")] :link (UUID/randomUUID)}
                              :under {:word :under}
                              :end-sign-in {:word :end-sign-in :sign-in (:sign-in mine)}})
                  only-hers (set (for [[word {:keys [who]}] words/words :when (= :a-person who)] word))]
              (check "A command line says none of the words that only a person may say: signed in at her yes, through the command line's own client, each word that the wire's table gives to a person alone, and `:invite` with `:herself`, is refused as said by an agent, and its sign-in stands."
                     (and (= (conj only-hers :invite-herself) (set (keys said)))
                          (every? #{:do-that-as-an-agent} (vals said))
                          (= who (:who (say me {:word :who-am-i}))))
                     said))
            (let [listed (say her {:word :under})]
              (check "She sees what is under her: the agent, with who made it, whose ending ends it, which is its maker, and its name, and her sign-ins that Auth watches, the agent's among them."
                     (and (some (fn [actor] (and (= (:made made) (:actor actor)) (= (:actor mine) (:made-by actor) (:ends-with actor))
                                                 (= "importer" (:label actor))))
                                (:actors listed))
                          (some (fn [sign-in] (= (:sign-in made) (:sign-in sign-in))) (:sign-ins listed)))
                     listed))
            (check "An agent that her program ends, through another copy than the one it is connected to, is told that its sign-in ended, and its credential then stands for nobody."
                   (and (= {:ended (:made made)} (say me {:word :end-agent :agent (:made made)}))
                        (wait-for (fn [] (= :find-her-sign-in (:could-not (client/bye (:line agent))))) 10000)
                        (= 401 (:status (line a agent-credential nil))))
                   (client/bye (:line agent)))
            (check "An agent that is not beneath the caller is not found, whatever id is named."
                   (= :find-the-agent (:could-not (say me {:word :end-agent :agent (UUID/randomUUID)}))))

            ;; ---------------------------------------------- an invitation
            (let [code (credential/make-secret)
                  invited (say her {:word :invite :uuid (UUID/randomUUID) :claim {:by :code :hash (credential/hash-of code)}})
                  asha (account 2 "asha")
                  by-email (say her {:word :invite :uuid (UUID/randomUUID)
                                    :claim {:by :email :email (str/upper-case (:email asha))}})
                  ;; Both sign-ins are let go at one moment, each on a thread of its
                  ;; own, and the time that each was in flight is kept, so that
                  ;; the check can hold that they were in flight together.
                  both (let [go (java.util.concurrent.CountDownLatch. 1)
                             runs (mapv (fn [url]
                                          (future
                                            (.await go)
                                            (let [began (System/nanoTime)
                                                  signed (drive/sign-in url asha)]
                                              (assoc signed :in-flight [began (System/nanoTime)]))))
                                        [a b])]
                         (.countDown go)
                         (mapv deref runs))
                  together? (let [[[b1 e1] [b2 e2]] (map :in-flight both)]
                              (and (< b1 e2) (< b2 e1)))
                  guest (drive/sign-in b (assoc (account 3 "guest") :invited code))
                  late (drive/sign-in a (assoc (account 4 "late") :invited code))]
              (check "A person invites by a code: the account that brings the code becomes the person whom the invitation made, and a second account with the same code is turned away."
                     (and (= (:invited invited) (:person guest)) (= 403 (:status late)))
                     [invited (dissoc guest :cookie) (dissoc late :cookie)])
              (check "Two first sign-ins of one invited account at the same moment, through two copies of the runtime, give one person: the one whom the invitation made, whose email was given in other letters. The two were in flight together."
                     (and together?
                          (= (:invited by-email) (:person (first both)) (:person (second both))))
                     {:together? together? :by-email by-email :both (mapv #(dissoc % :cookie) both)})
              (check "Both of those sign-ins lead to one own lane."
                     (let [lanes (doall (map (fn [signed]
                                                (let [program (drive/program a "asha's program"
                                                                             (fn [code] (drive/yes a (:cookie signed) code {})))]
                                                  (:own-lane (:hello (line a (:credential program) nil)))))
                                              both))]
                       (and (uuid? (first lanes)) (apply = lanes)))))

            ;; ------------------------- her accounts, and an invitation's end
            ;; Bea is invited by the first person, signs in, and at a browser
            ;; names herself for a second account, and for a third address
            ;; whose account never comes. Each step is a word of the wire.
            (let [bea (account 6 "bea")
                  work (account 7 "bea-work")
                  never (account 8 "bea-never")
                  bea-uuid (UUID/randomUUID)
                  invited-bea (say her {:word :invite :uuid bea-uuid :claim {:by :email :email (:email bea)}})
                  bea-in (drive/sign-in a bea)
                  at-browser (drive/browser-line b (:cookie bea-in))
                  her-program (let [p (drive/program a "bea's program" (fn [code] (drive/yes a (:cookie bea-in) code {})))]
                                (line a (:credential p) nil))
                  from-a-program (say her-program {:word :invite :uuid (UUID/randomUUID) :herself true
                                                   :claim {:by :email :email (:email work)}})
                  herself (say at-browser {:word :invite :uuid (UUID/randomUUID) :herself true
                                           :claim {:by :email :email (:email work)}})
                  work-in (drive/sign-in a work)
                  waiting (say at-browser {:word :invite :uuid (UUID/randomUUID) :herself true
                                           :claim {:by :email :email (:email never)}})
                  listed (say at-browser {:word :accounts})]
              (check "Two accounts enter as one person: at a browser she names herself under the address of a second account, and that account then signs in as she; the same word from a program that she signed in is refused, since no program is she, and her accounts are the two, in the order in which they were linked, each with its link."
                     (and (= (:invited invited-bea) (:person bea-in))
                          (= :do-that-as-an-agent (:could-not from-a-program))
                          (= {:invited (:person bea-in)} herself waiting)
                          (= (:person bea-in) (:person work-in))
                          (= [[:google (:sub bea)] [:google (:sub work)]] (mapv :account (:accounts listed)))
                          (every? (comp uuid? :link) (:accounts listed)))
                     {:from-a-program from-a-program :herself herself :work (dissoc work-in :cookie) :listed listed})

              (let [other (account 9 "not-bea")
                    again (account 10 "bea-again")
                    named-anew (say her {:word :invite :uuid bea-uuid :claim {:by :email :email (:email other)}})
                    other-in (drive/sign-in b other)
                    said-again (say her {:word :invite :uuid bea-uuid :claim {:by :email :email (:email bea)}})
                    again-in (drive/sign-in b (assoc again :email (:email bea)))]
                (check "A person cannot name another person who has entered: the UUID of Bea's invitation under another claim names another person, whose account does not enter as she; and the same word as her invitation, said again after she entered by it, gives her and writes nothing, so no account of her address that is not hers enters as she."
                       (and (:invited named-anew) (not= (:person bea-in) (:invited named-anew))
                            (= (:invited named-anew) (:person other-in))
                            (= invited-bea said-again)
                            (= 403 (:status again-in)))
                       [named-anew (dissoc other-in :cookie) said-again (dissoc again-in :body)]))

              (let [cleo (account 11 "cleo")
                    invited-cleo (say her {:word :invite :uuid (UUID/randomUUID) :claim {:by :email :email (:email cleo)}})
                    by-another (say at-browser {:word :take-back-invite :claim {:by :email :email (:email cleo)}})
                    by-inviter (say her {:word :take-back-invite :claim {:by :email :email (:email cleo)}})
                    once-more (say her {:word :take-back-invite :claim {:by :email :email (:email cleo)}})
                    cleo-in (drive/sign-in a cleo)]
                (check "Only whoever invited takes an invitation back: another person is told that no invitation of hers waits under the claim, the inviter takes it back, and then none waits; an invitation that was taken back lets nobody in."
                       (and (:invited invited-cleo)
                            (= :find-the-invitation (:could-not by-another))
                            (= {:taken-back (:invited invited-cleo)} by-inviter)
                            (= :find-the-invitation (:could-not once-more))
                            (= 403 (:status cleo-in)))
                       [by-another by-inviter once-more (dissoc cleo-in :body)]))

              ;; The copy `c` makes invitations that last seconds.
              (let [dora (account 12 "dora")
                    dora-uuid (UUID/randomUUID)
                    dora-word {:word :invite :uuid dora-uuid :claim {:by :email :email (:email dora)}}
                    at-c (drive/browser-line c cookie)
                    invited-dora (say at-c dora-word)
                    _ (Thread/sleep (+ 500 (long (:invitation-ms short-numbers))))
                    dora-in (drive/sign-in a dora)
                    renewed (say at-c dora-word)
                    dora-again (drive/sign-in a dora)]
                (close-line! at-c)
                (check "An invitation that is over lets nobody in: the account of its address, signing in after the invitation's end, is turned away."
                       (and (:invited invited-dora) (= 403 (:status dora-in)))
                       [invited-dora (dissoc dora-in :body)])
                (check "An invitation that is over is renewed by the same word, for the same person, and she enters as that person."
                       (and (= invited-dora renewed) (= (:invited invited-dora) (:person dora-again)))
                       [invited-dora renewed (dissoc dora-again :cookie :body)]))

              (let [[first-link work-link] (:accounts listed)
                    not-hers (say her {:word :unlink-account :account (:account first-link) :link (:link first-link)})
                    unlinked (mapv (fn [{:keys [account link]}]
                                     (say at-browser {:word :unlink-account :account account :link link}))
                                   [first-link work-link])
                    taken-back (say at-browser {:word :take-back-invite :claim {:by :email :email (:email never)}})
                    after (say at-browser {:word :accounts})
                    never-in (drive/sign-in b never)
                    anew (say her {:word :invite :uuid (UUID/randomUUID) :claim {:by :email :email (:email bea)}})
                    bea-again (drive/sign-in b bea)]
                (check "After she unlinks each of her accounts and takes back the invitation that she named, she has no account, the address of that invitation lets nobody in, and her first address can be invited as another person, as whom her first account then enters; the connection that she unlinked through stays, and nobody else unlinks her account."
                       (and (= [:take-that :another-persons] [(:could-not not-hers) (:why not-hers)])
                            (= [{:unlinked (:person bea-in)} {:unlinked (:person bea-in)}] unlinked)
                            (= {:taken-back (:person bea-in)} taken-back)
                            (= {:accounts []} after)
                            (= 403 (:status never-in))
                            (:invited anew) (not= (:person bea-in) (:invited anew))
                            (= (:invited anew) (:person bea-again))
                            (= (:who (:hello at-browser)) (:who (say at-browser {:word :who-am-i}))))
                       {:not-hers not-hers :unlinked unlinked :taken-back taken-back :after after
                        :never (dissoc never-in :body) :anew anew :bea-again (dissoc bea-again :cookie)}))
              (close-line! at-browser)
              (close-line! her-program))

            ;; ------------------------------------------- a secret is replaced
            ;; The times are read from what the copy `c` reports: the end of
            ;; the secret that `held` came with is in its hello, and the end
            ;; of the new secret is in the replacing's result. The secret is
            ;; replaced when half of the first one's time is left, so that
            ;; the new one outlasts it by that half. Every connection that
            ;; names `session` is at `c`, whose sessions last seconds, and
            ;; each but `held` is closed at once: what keeps the session past
            ;; the time that it was opened with is `held` alone.
            (let [{:keys [secret-ms previous-ms session-ms]} short-numbers
                  short (drive/program c "a program at the copy with short times"
                                       (fn [code] (drive/yes c cookie code {})))
                  held (line c (:credential short) nil)
                  session (:session (:hello held))
                  came-with-until (long (or (:until (:hello held)) 0))
                  alone (let [opened (line c (:credential short) nil)]
                          (close-line! opened)
                          {:session (:session (:hello opened)) :opened (System/currentTimeMillis)})
                  _ (sleep-until (- came-with-until (quot secret-ms 2)))
                  secret (credential/make-secret)
                  replaced (say held {:word :replace-secret :hash (credential/hash-of secret)})
                  renewed (credential/text (:root short) (:sign-in short) secret)
                  new-until (long (or (:until replaced) 0))
                  replaced-at (- new-until secret-ms)
                  before (line a (:credential short) nil)
                  before-ms (- (System/currentTimeMillis) replaced-at)
                  _ (close-line! before)
                  in-session (line c renewed session)
                  in-session-done (System/currentTimeMillis)
                  _ (close-line! in-session)]
              (check "A sign-in's secret is replaced by one that the program made: the new credential is let in, in the same session, and the secret before it stands for a moment and then for nobody."
                     (and (= (:sign-in short) (:replaced replaced))
                          (= session (:session (:hello in-session)))
                          (= (:who (:hello held)) (:who (:hello before)))
                          (do (sleep-until (+ replaced-at previous-ms 500))
                              (= 401 (:status (line a (:credential short) nil)))))
                     {:replaced replaced :before-ms before-ms :before (or (:hello before) before)})
              (check "A connection that was opened under the secret before stays open after the replacing, and after the time of the secret that it came with has passed: what is held is the sign-in and the session, and both stand."
                     (do (sleep-until (+ came-with-until 500))
                         (and (< came-with-until (System/currentTimeMillis) new-until)
                              (nil? (client/bye (:line held)))
                              (= (:who (:hello held)) (:who (say held {:word :who-am-i})))))
                     (client/bye (:line held)))
              (check "A session that is held by a connection is moved on past the time that it was opened with, and one that nobody holds is over when its time has passed."
                     (do (sleep-until (+ (max in-session-done (:opened alone)) session-ms 500))
                         (and (nil? (client/bye (:line held)))
                              (= session (:session (say held {:word :who-am-i})))
                              (let [again (line c renewed session)]
                                (close-line! again)
                                (= session (:session (:hello again))))
                              (= 401 (:status (line c renewed (:session alone))))
                              (let [control (line c renewed nil)]
                                (close-line! control)
                                (uuid? (:session (:hello control))))))
                     (client/bye (:line held)))
              (check "A sign-in whose secret is not replaced is over when its time passes: the connection that rests on it is told so by the runtime's own timer, at a copy that did not begin it."
                     (let [brief (drive/program c "a program that does not replace its secret"
                                                (fn [code] (drive/yes c cookie code {})))
                           resting (line a (:credential brief) nil)]
                       (and (:hello resting)
                            (wait-for (fn [] (= :find-her-sign-in (:could-not (client/bye (:line resting))))) 12000)
                            (= 401 (:status (line b (:credential brief) nil)))))))

            ;; -------------------------------------------------- a sign-out
            (let [out (drive/program a "a program that signs out" (fn [code] (drive/yes a cookie code {})))
                  here (line a (:credential out) nil)
                  there (line b (:credential out) nil)
                  third (drive/program a "a program that she ends" (fn [code] (drive/yes a cookie code {})))
                  resting (line b (:credential third) nil)]
              (check "A sign-out through one copy of the runtime ends the connection that another copy holds for the same sign-in, and the credential then stands for nobody at either."
                     (and (= {:ended (:sign-in out)} (say here {:word :sign-out}))
                          (wait-for (fn [] (= :find-her-sign-in (:could-not (client/bye (:line there))))) 10000)
                          (= [401 401] [(:status (line a (:credential out) nil)) (:status (line b (:credential out) nil))])))
              (check "She ends one of her sign-ins by its id, and the program that rested on it is told; a sign-in that is not hers is not found."
                     (and (= {:ended (:sign-in third)} (say her {:word :end-sign-in :sign-in (:sign-in third)}))
                          (wait-for (fn [] (= :find-her-sign-in (:could-not (client/bye (:line resting))))) 10000)
                          (= :find-the-sign-in (:could-not (say her {:word :end-sign-in :sign-in (UUID/randomUUID)}))))
                     (client/bye (:line resting))))

            ;; ------------------------------------------------------ the bounds
            (let [visitor (line a nil nil)
                  long-number (bigint (apply str (repeat 70000 "7")))]
              (check "The library sends no frame that the runtime could not read: a word whose one token is longer than any value that the store takes gives `:read-the-frame` with `:a-token-too-long` at once, as a watch of it does, and the runtime is sent nothing and says nothing."
                     (let [query {:asks [:place :n long-number] :asker who :lanes [lane]}
                           held-before (count (:watches @(:state (:line me))))
                           started (System/nanoTime)
                           word (client/say (:line me) {:word :read :query query} 30000)
                           watch (client/watch (:line me) query (fn [_] nil))
                           ms (quot (- (System/nanoTime) started) 1000000)]
                       (and (= {:could-not :read-the-frame :why :a-token-too-long} word watch)
                            (< ms 10000)
                            (nil? (client/said (:line me)))
                            (empty? (:waits @(:state (:line me))))
                            (= held-before (count (:watches @(:state (:line me)))))))
                     (client/said (:line me)))
              (check "A frame whose one token is longer than any value that the store takes is turned away before it is read as EDN: the runtime says that it could not read the frame, in a word that names no number, and the connection goes on. The frame is sent here past the library, which would not send it."
                     (do (raw-frame! me {:id 900000 :word :read :query {:asks [:place :n long-number] :asker who :lanes [lane]}})
                         (and (wait-for (fn [] (= {:could-not :read-the-frame :why :a-token-too-long} (client/said (:line me)))) 10000)
                              (= who (:who (say me {:word :who-am-i})))))
                     (client/said (:line me)))
              (check "A visitor's frame over the small bound is not taken into memory: the connection is closed. The frame is sent here past the library."
                     (do (raw-frame! visitor {:id 900000 :word :read :query {:asks [:place :x (apply str (repeat 70000 "x"))]
                                                                             :not-signed-in true :lanes [lane]}})
                         (wait-for (fn [] (:gone @(:state (:line visitor)))) 5000))))

            (close-line! her)

            ;; ----------------------------------------------- the pages' sign-out
            (check "A sign-out at the pages from another site's page is not taken, and one from the runtime's own page ends the browser's sign-in and takes the cookie away."
                   (let [foreign (drive/request :post (str a "/sign-out") {:cookie cookie :origin "http://evil.example"})
                         own (drive/request :post (str a "/sign-out") {:cookie cookie :origin a})
                         after (drive/request :get (str a "/") {:cookie cookie})]
                     (and (= 400 (:status foreign))
                          (= 303 (:status own)) (= "" (:cookie own))
                          (not (str/includes? (:body after) "You are signed in")))))))))))
