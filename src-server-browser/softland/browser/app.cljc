(ns softland.browser.app
  "The browser's face of the server: one Electric program, whose server half
  calls the seam (`softland.server.seam`) directly and whose client half
  draws a plain page in the DOM. It is a first slice: it proves that a tab
  can sign in, keep a live read open, and send an act, through the same
  seam that the face over HTTP stands on.

  What it takes. The server boots it with two values that never cross the
  wire: the seam, and the token that the socket's cookie carried, or nil.
  The client boots it with the page's state (`page-state`), which lives
  outside the program so that it outlasts a socket that closes and opens
  again. `electric-boot` is the one function that boots it, on both peers.

  What it gives. A page: while her standing is being found, \"…\"; when it
  is live, her id and her own lane's id, a form to write one fact, the
  list of her acts under way or answered, and a form to watch an entity,
  whose live answer is shown as a table; for any other standing, \"You are
  not signed in.\" and a link to the sign-in page.

  The three rules that this slice exists to keep, and where each is kept:

    a. The actor is a value of the server and never crosses the wire. Her
       standing, which holds the actor, is held in `Main`'s server half;
       the actor is handed to `Signed-in`, `Writing`, `Act` and `Watching`
       and used only inside their `e/server` expressions. The namespaces
       of the seam, of sign-in and of watching are required on the server
       peer alone, so a call of the seam written on the client does not
       compile. What the client is sent about her is plain data:
       `seam/about` and her own lane's id. Every call of the seam takes
       the server's actor, and every act and query is built on the server
       from her who and her own lane (`act-of`, `watched`). The client
       sends only an entity, a key, a value, and an act-id that the server
       gave it, which the server tests against her own lane before it
       sends (`sent-and-answered`).
    b. No state that sockets share lives in an atom of the server. This
       namespace defines no state on the server. The state of a socket is
       Electric's, in the program that the socket's opening starts; the
       state of a page is the client's (`page-state`).
    c. The socket's upgrade checks `Origin`: in `softland.browser.host`,
       before this program is booted.

  An act from the browser. The client holds the acts under way as state:
  a map from a local id to {:entity :key :value}, to which the steps add
  `:act-id`, then `:digest` and `:answer`. For each item, `Act` does the
  next step that the item lacks: on the server it asks one act-id of her
  own lane, and the client keeps it; then, with that act-id, the server
  sends the act and reads its answer, waiting up to `answer-wait-ms`, and
  the client keeps both. Electric starts a new server program at each
  opening of the socket, and the client program starts again with it, so
  a step whose branch mounts again runs again. Because the act-id is asked
  first and kept on the client, a send that runs again is the same act
  under the same act-id, which the store decides once. An item that holds
  an `:answer` does nothing; one that holds an `:act-id` never asks for
  another.

  A live read. `watched` folds the seam's flow with
  `softland.server.watching/answer-after` on the server, and the answer is
  sent to the client as one value each time it changes.

  State. On the server, none of its own: the standing, the answers and the
  live read are Electric's, for as long as the socket's program runs. On
  the client, the page's state, made once for each page by
  `softland.browser.boot`.

  Limits of this slice. An answer `:nothing-yet` after the wait is kept as
  the answer, and the act is not asked about again. The acts under way are
  lost when the page is reloaded. Every change of a live read sends the
  whole answer."
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            #?(:cljs [cljs.reader :as reader])
            #?(:clj [softland.server.seam :as seam])
            #?(:clj [softland.server.sign-in :as sign-in])
            #?(:clj [softland.server.watching :as watching])
            #?(:clj [softland.server.words :as words])
            #?(:clj [softland.store.door.declaration :as declaration])))

(def answer-wait-ms
  "How long the server waits for an act's answer after it sent the act, in
  milliseconds. A number of this slice, kept and not decided."
  5000)

;; ------------------------------------------------------------ the server's half

#?(:clj
   (defn- standing-of
     "Takes the seam and a token or nil. Gives a continuous flow of her
     standing (`softland.server.seam/standing`), which starts at
     {:state :finding} until the seam's first standing comes, and keeps the
     newest when the reader is slower. A live standing holds the actor: it
     stays on the server."
     [the-seam token]
     (->> (seam/standing the-seam token)
          (m/reductions {} {:state :finding})
          (m/relieve {}))))

#?(:clj
   (defn- act-of
     "Builds the act of one fact in her own lane. Takes the actor, her own
     lane's id, and an entity, a key and a value. Gives the act in the
     store's form without `:who` and `:session`, which the seam adds. The
     who of the permission is the actor's, never one that the client named."
     [actor lane entity k v]
     {:version 1
      :lane lane
      :permission [(seam/who-of actor) lane lane]
      :facts [{:e entity :k k :v v}]}))

#?(:clj
   (defn- one-act-id
     "Asks the seam for one act-id of her own lane. Takes the seam, the
     actor and her own lane's id. Gives a task of {:act-id id}, or of
     {:answer word} when the seam gave a word in place of act-ids, so that
     the item is done and shows why."
     [the-seam actor lane]
     (m/sp
      (let [got (m/? (seam/act-ids the-seam actor lane 1))]
        (if (and (vector? got) (= 1 (count got)))
          {:act-id (first got)}
          {:answer got})))))

#?(:clj
   (defn- sent-and-answered
     "Sends one act under an act-id that the client kept, and reads its
     answer. Takes the seam, the actor, her own lane's id, the act-id that
     the client handed back, and the entity, key and value. Gives a task of
     {:digest :answer}, or of {:answer word} when nothing could be sent.

     The act-id is tested first: it must have the form of an act-id and
     name her own lane in its first slot
     (`softland.store.door.declaration/act-id-in-lane?`). Otherwise
     nothing is sent, and the answer is the server's word `:read-the-word`
     at `:act-id`. A send under the same act-id again is the same act, so
     this task may run again when the socket opens again."
     [the-seam actor lane act-id entity k v]
     (m/sp
      (if-not (declaration/act-id-in-lane? lane act-id)
        {:answer (words/could-not :read-the-word {:why :act-id-of-another-lane :at :act-id})}
        (let [receipt (m/? (seam/send the-seam actor (act-of actor lane entity k v) act-id))]
          (if (:could-not receipt)
            {:answer receipt}
            {:digest (:digest receipt)
             :answer (m/? (seam/answer the-seam actor act-id (:digest receipt) answer-wait-ms))}))))))

#?(:clj
   (defn- held-after
     "Takes what is held of a live read, {:answer answer-or-nil} and maybe
     `:ended`, and one message of the seam's watch. Gives what is held
     after it: the answer after the message
     (`softland.server.watching/answer-after`), and the end's word when
     the message is the end."
     [held message]
     (cond-> (update held :answer watching/answer-after message)
       (contains? message :ended) (assoc :ended (:ended message)))))

#?(:clj
   (defn- watched
     "Opens a live read of one entity from her own lane. Takes the seam,
     the actor, her own lane's id and the entity. Gives a continuous flow
     of what is held of the read: {:answer nil} first, then the answer
     after each message, with `:ended` once the read ended. The query is
     built here: the client names only the entity."
     [the-seam actor lane entity]
     (->> (seam/watch the-seam actor {:asks [:entity entity] :from-lane lane})
          (m/reductions held-after {:answer nil})
          (m/relieve {}))))

;; ------------------------------------------------------------ the client's half

(defn page-state
  "Makes the state of one page, which the client keeps across the socket's
  closing and opening: `:acts`, an atom of the acts under way, a sorted map
  from a local id to an item; and `:watching`, an atom of {:entity e} for
  the entity that is watched, or nil. `softland.browser.boot` makes it once
  for each page."
  []
  {:acts (atom (sorted-map))
   :watching (atom nil)})

#?(:cljs
   (defn- field-edn
     "Takes a form element and the name of one of its inputs. Gives
     [:ok value] for the input's text read as one EDN form, or [:no why]
     for an empty text or one that does not read."
     [form field]
     (let [text (.-value (.namedItem (.-elements form) field))]
       (if (= "" (.trim text))
         [:no (str "The " field " is empty.")]
         (try [:ok (reader/read-string text)]
              (catch :default _ [:no (str "The " field " is no EDN: " text)]))))))

#?(:cljs
   (defn- add-act!
     "Takes the atom of the acts under way, an atom for a problem to show,
     and the form element of the write. Reads the entity, the key and the
     value, and adds an item under the next local id; or puts the problem
     in its atom and adds nothing. Gives nil."
     [!acts !problem form]
     (let [read (mapv (fn [field] (field-edn form field)) ["entity" "key" "value"])]
       (if-let [[_ why] (first (filter (comp #{:no} first) read))]
         (reset! !problem why)
         (let [[[_ entity] [_ k] [_ v]] read]
           (reset! !problem nil)
           (swap! !acts (fn [acts]
                          (assoc acts (inc (or (some-> (rseq acts) first key) 0))
                                 {:entity entity :key k :value v})))))
       nil)))

#?(:cljs
   (defn- watch-from-form!
     "Takes the atom of the entity watched, an atom for a problem to show,
     and the form element of the watch. Reads the entity and puts
     {:entity e} in the first atom; or puts the problem in the second.
     Gives nil."
     [!watching !problem form]
     (let [[outcome got] (field-edn form "entity")]
       (if (= :ok outcome)
         (do (reset! !problem nil) (reset! !watching {:entity got}))
         (reset! !problem got))
       nil)))

(defn- answer-text
  "Takes the answer that an item holds: one of the door's four answers, or
  a word. Gives a short text of it for the page."
  [answer]
  (cond
    (:could-not answer) (str "could not: " (pr-str answer))
    (= :refused (:answer answer)) (str "refused: " (pr-str (:reason answer)))
    (keyword? (:answer answer)) (name (:answer answer))
    :else (pr-str answer)))

(defn- state-text
  "Takes an item of the acts under way. Gives the text of its state: asking
  for an act-id, sending, or its answer."
  [item]
  (cond
    (contains? item :answer) (answer-text (:answer item))
    (contains? item :act-id) "sending"
    :else "asking for an act-id"))

(defn- place-text
  "Takes a place-answer of the read path. Gives the text of its cell in
  the table: the value and its stamp, or what the place holds instead."
  [place-answer]
  (case (:holds place-answer)
    :value (str (pr-str (:value place-answer)) " (" (pr-str (:stamp place-answer)) ")")
    (pr-str place-answer)))

;; ------------------------------------------------------------ the program

(e/defn Not-signed-in
  "Draws what a tab sees when her standing is not live. Takes nothing."
  []
  (e/client
    (dom/p (dom/props {:id "standing"})
           (dom/text "You are not signed in. ")
           (dom/a (dom/props {:href "/"}) (dom/text "Sign in")))))

(e/defn Answer-table
  "Draws what is held of a live read: a table of key -> value (stamp) for
  each place of the answer, the lanes that the read left out, and the
  read's end when it ended. Takes the entity and what is held, as plain
  data that crossed the wire."
  [entity held]
  (e/client
    (let [answer (:answer held)]
      (dom/p (dom/text "Watching " (pr-str entity) (if (nil? answer) " …" "")))
      (dom/table
        (dom/props {:id "watched"})
        (e/for [[[_ k] place-answer] (e/diff-by key (sort-by (comp pr-str key) (:places answer)))]
          (dom/tr
            (dom/td (dom/text (pr-str k)))
            (dom/td (dom/text (place-text place-answer))))))
      (when (seq (:left-out answer))
        (dom/p (dom/text "Left out: " (pr-str (:left-out answer)))))
      (when (contains? held :ended)
        (dom/p (dom/props {:id "watch-ended"})
               (dom/text "The live read ended: " (pr-str (:ended held))))))))

(e/defn Watching
  "Draws the form \"Watch entity\" and the live answer of the entity named
  there, read from her own lane. Takes the seam and the actor, used on the
  server alone; her own lane's id; and the page's state. A new entity
  leaves the read before and opens another."
  [the-seam actor lane page]
  (e/client
    (let [!watching (:watching page)
          watching (e/watch !watching)
          !problem (atom nil)
          problem (e/watch !problem)]
      (dom/h3 (dom/text "Watch entity"))
      (dom/form
        (dom/props {:id "watch"})
        (dom/input (dom/props {:name "entity" :placeholder "entity, as EDN"}))
        (dom/text " ")
        (dom/button (dom/props {:type "submit"}) (dom/text "Watch"))
        (dom/On "submit" (fn [event]
                           (.preventDefault event)
                           (watch-from-form! !watching !problem (.-currentTarget event)))
                nil))
      (when (some? problem) (dom/p (dom/text problem)))
      (when-some [{entity :entity} watching]
        (let [held (e/server (e/input (watched the-seam actor lane entity)))]
          (Answer-table entity held))))))

(e/defn Act
  "One act under way: draws it, and does its next step. Takes the seam and
  the actor, used on the server alone; her own lane's id; the atom of the
  acts under way; the item's local id; and the item.

  An item without `:act-id` asks one on the server, and the client keeps
  it. An item with `:act-id` and no `:answer` has the server send the act
  under that act-id and read its answer; the client keeps the digest and
  the answer. An item with `:answer` does nothing. The client hands the
  server only the act-id, the entity, the key and the value."
  [the-seam actor lane !acts id item]
  (e/client
    (let [{entity :entity k :key v :value act-id :act-id} item]
      (dom/li (dom/text (pr-str entity) " " (pr-str k) " " (pr-str v) " — " (state-text item)))
      (cond
        (contains? item :answer) nil

        (nil? act-id)
        (let [got (e/server (e/Task (one-act-id the-seam actor lane)))]
          (swap! !acts update id merge got)
          nil)

        :else
        (let [done (e/server (e/Task (sent-and-answered the-seam actor lane act-id entity k v)))]
          (swap! !acts update id merge done)
          nil)))))

(e/defn Writing
  "Draws the form that writes one fact in her own lane, and the list of
  her acts under way or answered. Takes the seam and the actor, used on the
  server alone; her own lane's id; and the page's state."
  [the-seam actor lane page]
  (e/client
    (let [!acts (:acts page)
          acts (e/watch !acts)
          !problem (atom nil)
          problem (e/watch !problem)]
      (dom/h3 (dom/text "Write"))
      (dom/form
        (dom/props {:id "write"})
        (dom/input (dom/props {:name "entity" :placeholder "entity, as EDN"}))
        (dom/text " ")
        (dom/input (dom/props {:name "key" :placeholder "key, as EDN"}))
        (dom/text " ")
        (dom/input (dom/props {:name "value" :placeholder "value, as EDN"}))
        (dom/text " ")
        (dom/button (dom/props {:type "submit"}) (dom/text "Write"))
        (dom/On "submit" (fn [event]
                           (.preventDefault event)
                           (add-act! !acts !problem (.-currentTarget event)))
                nil))
      (when (some? problem) (dom/p (dom/text problem)))
      (dom/ul
        (dom/props {:id "acts"})
        (e/for [[id item] (e/diff-by key acts)]
          (Act the-seam actor lane !acts id item))))))

(e/defn Signed-in
  "Draws the page of somebody whose standing is live. Takes the seam and
  her actor, which stay on the server, and the page's state. Her own
  lane's id is read once on the server (`sign-in/own-lane-of`); the
  client is sent her `seam/about` and that id, and nothing else of her."
  [the-seam actor page]
  (e/server
    ;; `Main` calls this once for each live session, and takes the call away
    ;; when the standing ends; the test holds also for a caller that hands
    ;; on a value that holds no actor.
    (when (seam/actor? actor)
      (let [about (seam/about actor)
            lane (e/Task (sign-in/own-lane-of the-seam (seam/who-of actor)) ::finding)]
        (e/client
          (dom/div
            (dom/p (dom/text "Your id is ") (dom/code (dom/props {:id "who"}) (dom/text (str (:who about)))))
            (dom/p (dom/text "Your own lane is ")
                   (dom/code (dom/props {:id "own-lane"})
                             (dom/text (cond (= ::finding lane) "…"
                                             (nil? lane) "not found"
                                             :else (str lane)))))
            (e/server
              (when (uuid? lane)
                (e/client
                  (Writing the-seam actor lane page)
                  (Watching the-seam actor lane page))))))))))

(e/defn Main
  "The program of one socket. Takes the seam and the token, which are the
  server's and never cross the wire, and the page's state, which is the
  client's. Draws into the element `softland` of the page. Her standing is
  held here, on the server, for as long as the socket is open: when it is
  live, the actor in it is handed to `Signed-in`, one occurrence for her
  session; when it ends, as at a sign-out on any face, that occurrence is
  taken away and the page turns to `Not-signed-in`."
  [the-seam token page]
  (e/client
    (binding [dom/node (.getElementById js/document "softland")]
      ;; An element that Electric made keeps its children in the order in
      ;; which they are written; the page's own element does not.
      (dom/div
        (dom/h2 (dom/text "Softland"))
        (e/server
          (let [standing (e/input (standing-of the-seam token))
                state (:state standing)]
            ;; A live standing is one occurrence, keyed by her session. When
            ;; the standing ends, the occurrence is taken away and is never
            ;; handed the next standing, which holds no actor.
            (e/for [live (e/diff-by :session (if (= :live state) [standing] []))]
              (Signed-in the-seam (:actor live) page))
            (case state
              :live nil
              :finding (e/client (dom/p (dom/props {:id "standing"}) (dom/text "…")))
              (Not-signed-in))))))))

(defn electric-boot
  "Boots the program on one peer; both peers expand it here, so that they
  compile one program. On the server it takes the seam and the token from
  the socket's cookie, or nil, and gives the server's half for Electric's
  socket adapter; the client's argument is a hole there. On the client it
  takes the page's state (`page-state`) and gives the client's half, a
  task; the server's two arguments are holes there."
  #?(:clj  [the-seam token]
     :cljs [page])
  #?(:clj  (e/boot-server {} Main (e/server the-seam) (e/server token) (e/client (e/amb)))
     :cljs (e/boot-client {} Main (e/server (e/amb)) (e/server (e/amb)) (e/client page))))
