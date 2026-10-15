(ns softland.runtime.service
  "The runtime as a service: who is acting, made true in front of the
  store's door and read path, on a cluster. It requires Rama through the
  door and the read path, reads a clock, takes a random source, and, at a
  sign-in, fetches Google's keys over the network. So it is not pure.

  A service holds an opened door (`softland.store.door`) and an opened read
  path (`softland.store.read-path`) on the module `Store`, and drives the
  runtime's pure parts with them: the start (`softland.runtime.start`), a
  sign-in and a second account linked (`softland.runtime.sign-in`), and the
  runtime's other work as the system (`softland.runtime.system-steps`).
  Each of those is steps that are safe to run again; this namespace reads
  what a step asks, sends the act it gives, waits for the answer, and asks
  again (`softland.runtime.steps/drive`). An actor's act and read go
  through the binding (`softland.runtime.binding`), which puts her name on
  them, and through blocks and limits, before the door or the read path.

  Every call but `open` takes the service first, and gives data:
  {:ok …}, or {:refuse word …}. None throws for what a caller gave: a throw
  inside a call is given back as {:refuse :runtime-threw :threw :said}.
  Each call that acts or reads for an actor, or acts as the system, refuses
  `:runtime-not-started` until `start!` has given {:ok …} in this process;
  `answer-of`, `leave!`, `read-as-system` and `step-io` do not ask. The words of this namespace:
  `words`; a call that drives steps gives their words too.

  An actor is what `actor-of` gave: {:id :kind :session}, and an agent's
  `:run`. A call that takes an actor trusts it: the caller got it from a
  token at this request, and the service does not check the token again.

  The calls, by who makes them:

    the runtime's own   open, start!
    a person signing in sign-in!, link-account!, sign-out!
    any actor           actor-of, act!, read!, read-live!, leave!,
                        answer-of, permission-of, give-right!, end-right!
    any actor, of herself  own-identity
    a person            rename!, make-agent!, copy-agent!, make-token!,
                        pending-requests, approve-token!, end-token!
    an agent            ask-token!, collect-token!, start-run!
    Sid's command line  invite!, link-by-admin!, block!, unblock!,
                        set-limits!
    the proofs          read-as-system, step-io, sign-in-input,
                        sent-in-last-minute

  State. Durable state is the store's; the service writes it only through
  the door, as the system (the steps) or as an actor (`act!`). The memory of
  the process holds, in atoms of the service:

    the system's session    the id of this start of the runtime, which every
                            act of the system names (`start!`)
    acts sent               for each identity, what counted toward it in the
                            last minute: its own acts, those of the agents
                            and programs below it in their chains of makers,
                            and the requests made on their behalf, each with
                            when, in which layer, under which keys and how
                            many bytes. The limits and a block's rate are
                            counted from it
    blocked counts          for each sender, its count of blocked attempts,
                            from the record's count at its first blocked
                            attempt in this process
    token requests          each pending request of an agent or a program
                            for a token, until it is collected or lapses
                            after `request-lapse-ms` (ten minutes); an
                            approved one holds its token until collected

  and a map of turns, one object for each sender that was blocked and each
  request being approved, so that one sender's blocked attempts write their
  count in order, and one request is approved once. A request's turn goes
  with the request; a blocked sender's stays for the process. All of it is lost when
  the process ends: the counts of a minute start again from none, a pending
  request is gone, and an approved token that was not collected is held by
  no one (its session stays in the record, and can be ended).

  It reads no rule from memory: each act reads the rules of the sender and
  of each identity above it in its chain of makers from the rules layer,
  each sign-in fetches Google's keys, and each `actor-of` reads the
  identity and the session. What it
  borrows: the cluster handle, which its opener owns; the door's and the
  read path's handles of Rama's client.

  Blocks and limits (BUILD.md, defaults 12 to 15). A sender is held with
  its chain of makers: an agent or a program, and the person who made it
  (`levels-of`). A sender's act is stopped at the runtime when a block of
  the system on the sender, or on one above it, reaches it; or when the
  count of acts in the last minute of the sender, or of one above it, is
  at its limit of acts or over it, or the bytes of the last minute with
  this act's would pass its limit of bytes. Every act counts toward the
  sender and toward each one above it, so a person's agents and programs
  share her limit and her block (the build session's ruling of 15 October
  2026, after the review found that a program dodged its person's block
  and limit). A limit is the rule on the identity's own id when there is
  one, else its kind's. A stopped attempt never reaches the door. It is counted, and
  `:blocked/count` is written to the sender's record layer when the count
  reaches 1, 2, 4, 8 and so on, so the record's count is never more than
  half behind and a flood writes few acts; the first few attempts, as many
  as `:rule/keep-first` says, are kept as `:blocked/attempt`, each whole
  when within the store's bound on a value, else its digest and size
  (`softland.runtime.system-steps/attempt-value`). A read that a block
  stops is a blocked attempt too. A request that the runtime carries out as
  the system on an actor's behalf (a sign-in of a linked account, a name,
  an agent or a program made or copied, a token made or approved, a run,
  an account linked) is held to the blocks of her chain when they name no
  keys and no layers, and counted toward its limits of acts
  (`on-behalf-stop`). An act from an actor
  without a right is not stopped here: it goes to the door, and the gate logs and refuses it
  (12 October 2026, entry 7). Only blocks and limits stop at the runtime,
  and the binding's refusal of a name that is not the actor's.

  Order of an act's way: the binding; then the blocks and limits of the
  sender's chain, read from the rules layer, and the counts; then the door,
  and the answer awaited for `:wait-ms`. An act that the binding refuses is not counted
  as a blocked attempt.

  What is a session's choice here, each Sid's to change: the numbers of
  `rule-defaults` that it marks as a session's; that only a person changes
  a name, makes an agent or a token, and copies an agent (who may change,
  run, ask or copy an agent is open, BUILD.md, \"Later\"); that any person
  may copy any agent, with its description when she can see it; that a
  request on an actor's behalf is held to the whole blocks of its chain and
  counted toward its limits; that the runtime reads an agent's description
  as its maker when it records a run; the short code of a token request;
  and the words.

  What is the build session's, 15 October 2026, after the final review,
  each Sid's to change: a block and a limit reach up the chain of makers;
  a limit of bytes a minute; a program's default limit is a person's, and a
  rule on an id comes before its kind's; a sign-in of a linked account is
  held to her block and limit; a sitting ends after `:rule/sitting-ms`; an
  ended session keeps its hash, and its end is told only after the secret
  matched; a new token request drops the agent's oldest rather than being
  refused; an approval is recorded beside its session; an agent or a
  program given a right with no key list gets `#{:*}`.

  Approach. The file has these sections, in this order: the words and the
  numbers; the service as a value; the clock and the random source; the
  calls of the door and the read path; driving steps; the start and
  sign-in; actors; blocks and limits; requests on her behalf; acts and
  reads; names; agents, tokens and runs; rights; her own identity; the
  admin calls; the proofs' calls."
  (:require [softland.runtime.binding :as binding]
            [softland.runtime.google :as google]
            [softland.runtime.ids :as ids]
            [softland.runtime.records :as records]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.start :as start]
            [softland.runtime.steps :as steps]
            [softland.runtime.system-steps :as system-steps]
            [softland.runtime.token :as token]
            [softland.store.act :as act]
            [softland.store.door :as door]
            [softland.store.door.text :as text]
            [softland.store.read-path :as read-path])
  (:import [com.nimbusds.jose.jwk JWKSet]
           [java.io Writer]
           [java.net URL]
           [java.nio ByteBuffer]
           [java.security SecureRandom]
           [java.util UUID]
           [java.util.concurrent ConcurrentHashMap]
           [java.util.function Function]))

;; ------------------------------------------------------ words and numbers

(def words
  "The words of this namespace's own refusals, beside those of the steps it
  drives and of the binding:

    :runtime-threw            a call threw inside; it carries :threw, the
                              class, and :said, the message
    :runtime-not-started      `start!` has not given {:ok …} in this process
    :no-google-client-id      the service was opened without one
    :google-keys-not-fetched  the key set could not be had
    :token-not-valid          the token is not in its form, names no
                              identity or session of each other, or its
                              secret is not the kept one
    :session-ended            the token's session was ended
    :run-not-the-sessions     the run given is not one of the session's
    :actor-not-known          the actor is not one that `actor-of` gives
    :only-a-person            the call is a person's, and the actor is no
                              person
    :blocked                  a block of the system, of the sender or of one
                              above it in its chain of makers, stops the
                              attempt
    :over-limit               the sender, or one above it, is at its limit
                              of acts a minute (`:bound :acts`) or would
                              pass its limit of bytes (`:bound :bytes`)
    :rules-not-read           the sender's block or limit could not be read
    :no-right-in-layer        the actor holds no right there that she can see
    :agent-not-found          no agent or program has that id
    :not-the-maker            the actor did not make that agent or program
    :request-not-known        no pending request has that id: never made,
                              collected, lapsed, or dropped for a newer one
    :not-approved-yet         the request is not approved
    :input-not-whole          a limit given to `set-limits!` names nothing
    :act-refused              an act of the system was refused, or not
                              answered; it carries the answer
    :identity-not-a-person    the id given to link an account to is no person

  A word of the door or the read path is given as {:refuse its word …},
  with what it carries. The words are a session's, Sid's to change."
  [:runtime-threw :runtime-not-started :no-google-client-id :google-keys-not-fetched
   :token-not-valid :session-ended :run-not-the-sessions :actor-not-known :only-a-person
   :blocked :over-limit :rules-not-read :no-right-in-layer :agent-not-found :not-the-maker
   :request-not-known :not-approved-yet :input-not-whole :act-refused
   :identity-not-a-person])

(def google-certs-url
  "Where Google publishes the keys of its ID tokens, by Google's page
  \"Verify the Google ID token on your server side\", read 15 October 2026."
  "https://www.googleapis.com/oauth2/v3/certs")

(def rule-defaults
  "The rules that `start!` writes where the rules layer holds none
  (`softland.runtime.start`, step 5):

    acts a minute      600 for each kind of actor. A person at a browser
                       makes a few acts a second at most; an agent and a
                       program are held to the same, and a program that
                       needs more, the importer among them, is given a rule
                       on its own id (`set-limits!`). The program's 600 is
                       the build session's, 15 October 2026, after the
                       review found that a program at 60,000 dodged its
                       person's limit; the person's and the agent's are a
                       session's.
    bytes a minute     64 MiB for each kind, counted on the one text of
                       each act (the build session's).
    a freed name       waits thirty days before another takes it (a
                       session's).
    attempts kept      eight blocked attempts are kept whole (a session's).
    a sitting          lasts seven days from its sign-in (the build
                       session's).

  Each number is Sid's to change. An opener can give others under
  `:rules`, and a rule in the store is never written over at a start."
  {:acts-per-minute {:person 600 :agent 600 :program 600}
   :bytes-per-minute {:person (* 64 1024 1024) :agent (* 64 1024 1024) :program (* 64 1024 1024)}
   :name-wait-ms (* 30 24 60 60 1000)
   :keep-first 8
   :sitting-ms (* 7 24 60 60 1000)})

(def default-wait-ms
  "How long a call waits for an act's answer, by default: long enough for a
  batch of the base on the in-process cluster. A number of the build."
  10000)

(def request-lapse-ms
  "How long a pending token request lives in the process's memory: ten
  minutes (the build session's plan)."
  600000)

(def max-requests
  "How many token requests the process holds at once, of all identities.
  A number of the build: it bounds the memory that unsigned callers can
  take."
  1024)

(def max-requests-per-identity
  "How many pending token requests one agent or program can have at once.
  A number of the build."
  8)

(def ^:private minute-ms
  "A minute, in milliseconds: the window of the counts."
  60000)

;; ---------------------------------------------------- the service as a value

(deftype Service [module door read-path opts system-session sent swept blocked requests turns]
  Object
  (toString [_] (str "#<runtime service " module ">")))

(defmethod print-method Service [^Service s ^Writer w]
  (.write w (.toString s)))

(defmethod print-dup Service [^Service s ^Writer w]
  (.write w (.toString s)))

(alter-meta! #'->Service assoc
             :private true
             :doc "Makes an opened service. `open` is the one caller. It holds the
  module's name; the door and the read path, borrowed; its options; and its
  memory, each an atom: the system's session, the acts sent in the last
  minute, when that was last swept, the blocked counts and the token
  requests; and the map of turns. It prints as its module's name and
  nothing else, so that no token that a request holds is printed.")

(defonce ^:private secure-random
  (SecureRandom.))

(defn- secure-random-bytes
  "The default random source: n bytes of the JVM's SecureRandom."
  [n]
  (let [b (byte-array n)]
    (.nextBytes ^SecureRandom secure-random b)
    b))

(defn fetch-google-keys
  "The default of `:google-keys`. Takes nothing. Gives Google's key set,
  fetched over HTTPS from `google-certs-url`, with five seconds to connect
  and five to read, and at most 512 KiB. It throws when the fetch fails;
  `sign-in!` turns that into `:google-keys-not-fetched`. Nothing keeps the
  set: a sign-in is once a sitting (PROGRESS.md, \"Google's ID token\")."
  ^JWKSet []
  (JWKSet/load (URL. google-certs-url) 5000 5000 (* 512 1024)))

(defn open
  "Opens the service. Takes a cluster handle of Rama's, the name that Rama
  gives the module `Store`, and options:

    :google-client-id  the client id of Sid's Google client, a string
    :google-keys       a function of no argument that gives a JWKSet;
                       default `fetch-google-keys`
    :clock             a function of no argument that gives the time in
                       milliseconds; default System/currentTimeMillis
    :random-bytes      a function of n that gives n random bytes; default
                       the JVM's SecureRandom
    :wait-ms           how long an act's answer is waited for; default
                       `default-wait-ms`
    :rules             the rules' defaults that `start!` writes when
                       missing, merged over `rule-defaults`

  Gives the service, or {:refuse word …} with the word of the door's or the
  read path's opening (`:find-the-module`). It opens one door and one read
  path; the opener owns the cluster handle. A process opens one service:
  two would hold two read paths, which share no push. It writes nothing:
  `start!` is the first act."
  [cluster module-name opts]
  (try
    (let [d (door/open cluster module-name)
          rp (when-not (:could-not d) (read-path/open cluster module-name))
          o (or opts {})]
      (cond
        (:could-not d) (assoc (dissoc d :could-not) :refuse (:could-not d))
        (:could-not rp) (assoc (dissoc rp :could-not) :refuse (:could-not rp))
        :else
        (->Service module-name d rp
                   {:google-client-id (:google-client-id o)
                    :google-keys (or (:google-keys o) fetch-google-keys)
                    :clock (or (:clock o) #(System/currentTimeMillis))
                    :random-bytes (or (:random-bytes o) secure-random-bytes)
                    :wait-ms (or (:wait-ms o) default-wait-ms)
                    :rules (merge-with (fn [a b] (if (and (map? a) (map? b)) (merge a b) b))
                                       rule-defaults (:rules o))}
                   (atom nil) (atom {}) (atom 0) (atom {}) (atom {}) (ConcurrentHashMap.))))
    (catch Throwable t
      {:refuse :runtime-threw :threw (.getName (class t)) :said (.getMessage t)})))

;; ------------------------------------------------ the clock, the random source

(defn- now
  "The time, from the service's clock, in milliseconds."
  ^long [^Service s]
  (long ((:clock (.-opts s)))))

(defn- random-bytes
  "n random bytes from the service's random source."
  ^bytes [^Service s n]
  ((:random-bytes (.-opts s)) n))

(defn- fresh-uuid
  "A random UUID, version 4, made from 16 bytes of the service's random
  source, so that every id the service makes comes from the one source an
  opener can hand in."
  [^Service s]
  (let [b (random-bytes s 16)]
    (aset-byte b 6 (unchecked-byte (bit-or (bit-and (aget b 6) 0x0f) 0x40)))
    (aset-byte b 8 (unchecked-byte (bit-or (bit-and (aget b 8) 0x3f) 0x80)))
    (let [bb (ByteBuffer/wrap b)]
      (UUID. (.getLong bb) (.getLong bb)))))

(def ^:private code-letters
  "The letters of a request's short code: no 0, 1, I, L, O or U, so that it
  reads aloud and copies without a mistake."
  "ABCDEFGHJKMNPQRSTVWXYZ23456789")

(defn- short-code
  "Eight letters of `code-letters`, from random bytes: the code that an
  agent shows and its person compares before she approves a token."
  [^Service s]
  (apply str (map #(nth code-letters (mod (bit-and (long %) 0xff) (count code-letters)))
                  (random-bytes s 8))))

;; ----------------------------------------- the calls of the door and read path

(defmacro ^:private guarded
  "Runs the body, and gives {:refuse :runtime-threw :threw :said} for any
  throw, so that no call of the service throws. An interrupt's mark is set
  again."
  [& body]
  `(try ~@body
        (catch Throwable t#
          (when (instance? InterruptedException t#) (.interrupt (Thread/currentThread)))
          {:refuse :runtime-threw :threw (.getName (class t#)) :said (.getMessage t#)})))

(defn- word->refuse
  "Takes a word of the door or the read path, a map with `:could-not`.
  Gives {:refuse that word …} with what it carries."
  [w]
  (assoc (dissoc w :could-not) :refuse (:could-not w)))

(defn- read-once
  "Reads one query with the read path, as it is: the runtime's own read.
  Gives an answer or a word of the read path."
  [^Service s query]
  (read-path/read-once (.-read-path s) query))

(defn- send-act
  "Sends one act through the door and waits for its answer for `:wait-ms`.
  Gives {:act-id :digest :answer}, the answer as `door/answer-of` gives it
  (a word of the door among them), or the door's word when the send itself
  could not be made."
  [^Service s the-act]
  (let [d (.-door s)
        sent (door/send-act d the-act)]
    (if (:could-not sent)
      sent
      (assoc (select-keys sent [:act-id :digest])
             :answer (door/answer-of d (:act-id sent) (:digest sent) (:wait-ms (.-opts s)))))))

(defn- outcome-of
  "Sends an act and gives its outcome, as the steps take one: the answer,
  or the door's word."
  [s the-act]
  (let [r (send-act s the-act)]
    (if (:could-not r) r (:answer r))))

(defn- system-act!
  "Sends an act of the system and gives {:ok {:act-id :answer}} when it was
  accepted, or {:refuse :act-refused :answer …} when it was refused or not
  answered within the wait, or the door's word."
  [s the-act]
  (let [r (send-act s the-act)]
    (cond
      (:could-not r) (word->refuse r)
      (steps/accepted? (:answer r)) {:ok (select-keys r [:act-id :answer])}
      :else {:refuse :act-refused :act-id (:act-id r) :answer (:answer r)})))

;; ------------------------------------------------------------ driving steps

(defn- system-session
  "The id of this start of the runtime, or nil before `start!`."
  [^Service s]
  @(.-system-session s))

(defn- drive
  "Drives a step function on one input with the service's read and send
  (`softland.runtime.steps/drive`). Takes the service, the step function,
  which takes an input and what is known, and the input. The input is a
  value made once, before the first round: its fresh ids and its time stay
  the same through the run, so an act that a step gives again is the same
  act, and the steps know it was sent. Gives {:ok done} without `:do`, or
  {:refuse word …} with what the refusal carries."
  [s step input]
  (let [r (steps/drive #(step input %) #(read-once s %) #(outcome-of s %))]
    (if (= :done (:do r))
      {:ok (dissoc r :do :rounds :known)}
      (assoc (dissoc r :do :word :known) :refuse (:word r)))))

(defmacro ^:private when-started
  "Gives `:runtime-not-started` before `start!`; else runs the body with the
  system's session bound to the symbol given."
  [[sym s] & body]
  `(if-let [~sym (system-session ~s)]
     (do ~@body)
     {:refuse :runtime-not-started}))

(defn- identity-query
  "The query of an identity's record in `identities`, as the system."
  [id]
  (steps/entity-query ids/system ids/identities id false))

(defn- identity-of
  "Reads an identity's record as the system. Gives {:ok record}, the map of
  its keys that hold a value (empty when there is none), or the read path's
  word as a refusal."
  [s id]
  (let [a (read-once s (identity-query id))]
    (if (steps/failed? a) (word->refuse a) {:ok (steps/record-of a id)})))

(defn- actor?
  "True for an actor of the form that `actor-of` gives."
  [actor]
  (and (map? actor) (uuid? (:id actor)) (uuid? (:session actor))
       (contains? #{:person :agent :program} (:kind actor))))

(defn- person?
  "True for an actor who is a person."
  [actor]
  (and (actor? actor) (= :person (:kind actor))))

;; ------------------------------------------------------- the start, sign-in

(defn start!
  "The runtime's start: drives `softland.runtime.start/start-step` with a
  fresh session of the system and the rules' defaults of the service. The
  base and the system's layers are made when missing, their settings
  written, the system's identity and name, the rules' defaults where none
  is, and this start as a session of the system. Gives {:ok {:session s}}
  and holds s as the system's session of this process, or the refusal of
  the steps. Safe to call again: a second call is a new start, with a new
  session."
  [^Service s]
  (guarded
   (let [session (fresh-uuid s)
         r (drive s start/start-step {:system-session session :rules (:rules (.-opts s))})]
     (if (:ok r)
       (do (reset! (.-system-session s) session)
           {:ok {:session session}})
       r))))

(defn- google-claims
  "Checks a Google ID token: the key set fetched, then
  `softland.runtime.google/check-id-token` against the client id and the
  service's clock. Gives {:ok claims} or {:refuse word}."
  [^Service s id-token]
  (let [{:keys [google-client-id google-keys]} (.-opts s)]
    (if-not (string? google-client-id)
      {:refuse :no-google-client-id}
      (let [key-set (try (google-keys) (catch Throwable _ nil))]
        (if-not (instance? JWKSet key-set)
          {:refuse :google-keys-not-fetched}
          (google/check-id-token id-token key-set google-client-id (now s)))))))

(defn sign-in-input
  "Takes the service, the claims of a checked Google ID token, and an
  invitation's code or nil. Gives the input of
  `softland.runtime.sign-in/sign-in-step`, with fresh ids, a fresh secret
  and a provisional name from the service's random source, and the
  system's session. What must hold: the service has started. `sign-in!`
  calls it; the proof calls it to drive a sign-in that it cuts."
  [s claims code]
  {:account (assoc claims :kind :google)
   :code code
   :fresh {:person (fresh-uuid s) :record-layer (fresh-uuid s) :own-layer (fresh-uuid s)
           :session (fresh-uuid s) :secret (random-bytes s 32)
           :name (ids/provisional-name (random-bytes s 8))}
   :system-session (system-session s)})

(declare on-behalf-stop count-only! rules-of)

(defn- sign-in-stop
  "Holds a sign-in of an account that is linked to a person to her blocks
  and her limit, before any step of the sign-in runs: the link is read, and
  her request is held as `on-behalf-stop` holds one, so a whole block
  refuses it `:blocked` and each sign-in counts toward her limit (the build
  session's ruling of 15 October 2026, after the review: one Google ID
  token signed in again and again, and a blocked person still wrote by
  signing in). An account with no link has no person to hold yet. Gives
  nil when the sign-in goes on, or a refusal."
  [s claims]
  (let [a (read-once s (steps/place-query ids/system ids/ways-in (ids/account-id :google (:sub claims)) :way-in/person))]
    (if (steps/failed? a)
      (word->refuse a)
      (when-let [p (steps/value-at a)]
        (on-behalf-stop s {:id p :kind :person} {:asked :sign-in})))))

(defn sign-in!
  "A person's sign-in at a browser: Google's ID token checked, then
  `softland.runtime.sign-in/sign-in-step` driven to the end. Takes the
  service, the ID token's text as the browser sent it, and an invitation's
  code or nil. Gives {:ok {:person :session :token :first-sign-in?}}: her
  id, her sitting, the sitting's token (which the store keeps only as its
  hash), and whether this sign-in made her link; or a refusal: a word of
  the check of the token, `:blocked` or `:over-limit` for a linked account
  whose person is held (`sign-in-stop`), `:no-invitation` for a stranger,
  or another word of the steps.

  The sitting ends at the time that the rule `:rule/sitting-ms` on
  `:person` gives from now (`:session/ends-at`; the build session's, 15
  October 2026), and `actor-of` refuses it after. A first sign-in, which
  makes her, is counted toward her limit once it is done. No ID token is
  kept: the same token signs in again while Google's `exp` holds, each
  time held to her limit.

  Safe to run again: a sign-in that a crash cut is finished by the next
  sign-in of the same account, and makes no second person
  (`softland.runtime.sign-in`)."
  [s id-token code]
  (guarded
   (when-started [_ s]
     (let [checked (google-claims s id-token)]
       (if-let [claims (:ok checked)]
         (let [person-rules (rules-of s :person)]
           (or (when (:refuse person-rules) person-rules)
               (sign-in-stop s claims)
               (let [ms (get-in person-rules [:ok :rule/sitting-ms])
                     input (cond-> (sign-in-input s claims code)
                             (int? ms) (assoc :sitting-ends-at (+ (now s) (long ms))))
                     r (drive s sign-in/sign-in-step input)]
                 (when (get-in r [:ok :first-sign-in?])
                   (count-only! s (get-in r [:ok :person])))
                 r)))
         checked)))))

(defn link-account!
  "A second account linked to a person who is signed in: Google's ID token
  checked, then `softland.runtime.sign-in/link-account-step` driven. Takes
  the service, the person (an actor), and the ID token of the account to
  link. Gives {:ok {:person :account}}, the account's id; or a refusal:
  `:account-linked-to-another`, `:only-a-person-links-an-account`, or a
  word of the check."
  [s actor id-token]
  (guarded
   (when-started [sys s]
     (let [checked (google-claims s id-token)]
       (if-let [claims (:ok checked)]
         (or (when (actor? actor) (on-behalf-stop s actor {:asked :link-account}))
             (let [input {:actor actor :account (assoc claims :kind :google) :system-session sys}]
               (drive s sign-in/link-account-step input)))
         checked)))))

;; ------------------------------------------------------------------- actors

(defn actor-of
  "Who holds a token. Takes the service, a session's token as its holder
  sent it (`sl1.<identity>.<session>.<secret>`), and, for an agent that
  acts in a run, the run's id. Gives {:ok actor}, {:id :kind :session} and
  `:run` when one was given; or a refusal: `:token-not-valid`,
  `:session-ended`, `:run-not-the-sessions`.

  It reads, as the system: the identity in `identities` (its kind and record
  layer), then the session in that record layer, and the run beside it when
  one is given. The session must be of that identity and keep the hash of
  this token (`softland.runtime.token/matches?`, in constant time); only
  then is it asked whether the session was ended, or, for a sitting, whether
  its `:session/ends-at` has passed by the service's clock. So a token with
  a wrong secret is `:token-not-valid` whether or not its session ended,
  and only the holder of the right secret is told `:session-ended` (the
  build session's ruling of 15 October 2026). A run must be the agent's,
  in this session. A person's sitting,
  an agent's conversation and a program's run are each a session here.
  Two round trips, the second holding two reads at once when a run is
  given; nothing is kept between calls."
  ([s the-token] (actor-of s the-token nil))
  ([s the-token run]
   (guarded
    (when-started [_ s]
      (if-let [{:keys [identity session]} (token/parse-token the-token)]
        (let [held (identity-of s identity)]
          (if (:refuse held)
            held
            (let [{kind :identity/kind r :identity/record-layer} (:ok held)]
              (cond
                (not (and (contains? #{:person :agent :program} kind) (some? r))) {:refuse :token-not-valid}
                (and (some? run) (not (and (uuid? run) (= :agent kind)))) {:refuse :run-not-the-sessions}
                :else
                (let [run-read (when run (future (read-once s (steps/entity-query ids/system r run false))))
                      sa (read-once s (steps/entity-query ids/system r session false))
                      ra (when run-read @run-read)
                      sh (steps/record-of sa session)]
                  (cond
                    (steps/failed? sa) (word->refuse sa)
                    (and run (steps/failed? ra)) (word->refuse ra)
                    (not= identity (:session/of sh)) {:refuse :token-not-valid}
                    (not (token/matches? the-token (:session/token-hash sh))) {:refuse :token-not-valid}
                    (some? (:session/ended sh)) {:refuse :session-ended}
                    (and (int? (:session/ends-at sh)) (<= (long (:session/ends-at sh)) (now s))) {:refuse :session-ended}
                    (and run (not= session (:run/session (steps/record-of ra run)))) {:refuse :run-not-the-sessions}
                    :else {:ok (cond-> {:id identity :kind kind :session session} run (assoc :run run))}))))))
        {:refuse :token-not-valid})))))

(defn sign-out!
  "Ends the actor's own session: a person's sitting, or the session that an
  agent's or a program's token named. Takes the service and the actor.
  Gives {:ok {:session :ended-before?}} or the refusal of
  `softland.runtime.system-steps/end-session-step`. `actor-of` then answers
  its token `:session-ended`."
  [s actor]
  (guarded
   (when-started [sys s]
     (if-not (actor? actor)
       {:refuse :actor-not-known}
       (drive s system-steps/end-session-step
                  {:identity (:id actor) :session (:session actor) :by (:id actor)
                   :now-ms (now s) :system-session sys})))))

;; -------------------------------------------------------- blocks and limits

(def ^:private max-chain
  "How many identities a sender's chain of makers holds at most, the sender
  counted: the bound of the walk up `:identity/made-by`. Today a chain is a
  person alone, or an agent or a program and the person who made it. A
  number of the build."
  8)

(defn- rules-of
  "Reads every rule that the rules layer holds on one entity (an identity's
  id, or a kind of actor), as the system: one read of the entity. Gives
  {:ok {key value}} or {:refuse :rules-not-read :could-not word}."
  [s entity]
  (let [a (read-once s (steps/entity-query ids/system ids/rules entity false))]
    (if (steps/failed? a) {:refuse :rules-not-read :could-not a} {:ok (steps/record-of a entity)})))

(defn- chain-of
  "A sender's chain of makers: the sender, the identity that made it, the
  one that made that, and so on, up to a person, to an identity that names
  no maker, or to `max-chain`. Takes the service, the sender's id and its
  kind. Gives {:ok [{:id :kind} …]}, the sender first, or a refusal of a
  read. A person is the top of her chain and costs no read; each identity
  below her costs one read of its own record and one of its maker's. An id
  met twice ends the walk."
  [s sender kind]
  (loop [chain [{:id sender :kind kind}]]
    (let [{:keys [id kind]} (peek chain)]
      (if (or (= :person kind) (>= (count chain) max-chain))
        {:ok chain}
        (let [held (identity-of s id)
              m (:identity/made-by (:ok held))]
          (cond
            (:refuse held) held
            (or (nil? m) (some #(= m (:id %)) chain)) {:ok chain}
            :else (let [mh (identity-of s m)]
                    (if (:refuse mh)
                      mh
                      (recur (conj chain {:id m :kind (:identity/kind (:ok mh))}))))))))))

(defn- levels-of
  "The rules that hold a sender: for each identity of its chain of makers
  (`chain-of`), the system's block of it and its limits of acts and of
  bytes a minute. Each limit is the rule on the identity's own id when one
  is there, else its kind's (the rule on an id is the build session's, 15
  October 2026: a program that needs more than a person's limit is given a
  rule of its own). The rules of every id and kind of the chain are read at
  once, one read each. Gives {:ok [{:id :kind :block :acts :bytes} …]}, the
  sender first, or {:refuse :rules-not-read …}: a sender whose rules cannot
  be read is not let through."
  [s sender kind]
  (let [chain (chain-of s sender kind)]
    (if (:refuse chain)
      (assoc chain :refuse :rules-not-read)
      (let [entities (distinct (concat (map :id (:ok chain)) (keep :kind (:ok chain))))
            reads (mapv (fn [e] [e (future (rules-of s e))]) entities)
            got (into {} (map (fn [[e f]] [e @f])) reads)]
        (or (some #(when (:refuse %) %) (vals got))
            {:ok (mapv (fn [{:keys [id kind]}]
                         (let [own (:ok (get got id))
                               of-kind (:ok (get got kind))]
                           {:id id :kind kind
                            :block (:rule/block own)
                            :acts (or (:rule/acts-per-minute own) (:rule/acts-per-minute of-kind))
                            :bytes (or (:rule/bytes-per-minute own) (:rule/bytes-per-minute of-kind))}))
                       (:ok chain))})))))

(defn- act-keys
  "The keys of an act's facts, as far as they can be read: at most the
  door's bound on the facts of an act."
  [the-act]
  (let [fs (:facts the-act)]
    (into #{} (keep #(when (map? %) (:k %))) (when (sequential? fs) (take 256 fs)))))

(defn- act-bytes
  "The size of an act as the binding passed it on: the UTF-8 bytes of its
  one text (`softland.store.door.text/text`), which is the text the door
  writes. One walk of the act."
  [the-act]
  (long (text/utf8-count (text/text the-act))))

(defn- in-window
  "The entries of a sender's acts that are within the last minute."
  [entries now-ms]
  (into [] (filter #(< (- now-ms minute-ms) (:at %))) entries))

(defn- in-reach
  "How many of the entries a block reaches: in one of its layers, when it
  names some, and under one of its keys, when it names some."
  [block entries]
  (count (filter (fn [{:keys [layer keys]}]
                   (and (or (not (contains? block :layers)) (contains? (:layers block) layer))
                        (or (not (contains? block :keys)) (some #(act/allows-key? (:keys block) %) keys))))
                 entries)))

(defn- block-stops-act?
  "Whether the block stops the act, given the count of acts in its reach
  (`softland.runtime.binding/block-stops-act?`). An act whose facts cannot
  be read is judged by its layer alone."
  [block the-act n]
  (try (binding/block-stops-act? block the-act n)
       (catch Throwable _ (binding/block-stops-act? block {:layer (:layer the-act) :facts []} n))))

(defn- stop-of
  "Takes the levels of a sender (`levels-of`), the windows of the last
  minute of each level's id, the thing judged (an act, or a request as an
  act with no layer and no facts), its entry, and a function that gives the
  block of a level that reaches the thing (the block itself for an act;
  `whole-block` for a request). Gives nil when it goes on, or
  {:refuse word :by id :bound …}: `:blocked` when a block of any level
  stops it; else `:over-limit`, with `:bound :acts` when a level's count of
  the last minute is at its limit or over it, or `:bound :bytes` when that
  level's bytes of the last minute and this thing's would pass its limit.
  The blocks of every level are asked before any limit."
  [levels windows the-act entry block-of]
  (or (some (fn [{:keys [id block]}]
              (let [b (block-of block)]
                (when (and (map? b) (block-stops-act? b the-act (in-reach b (get windows id))))
                  {:refuse :blocked :by id})))
            levels)
      (some (fn [{:keys [id acts bytes]}]
              (let [es (get windows id)]
                (cond
                  (binding/over-limit? acts (count es))
                  {:refuse :over-limit :by id :bound :acts}

                  (and (int? bytes) (< bytes (+ (long (:bytes entry)) (reduce + 0 (map :bytes es)))))
                  {:refuse :over-limit :by id :bound :bytes}

                  :else nil)))
            levels)))

(defn- sweep
  "Takes the map of acts sent, and the time. Gives it without the ids that
  have nothing in the last minute."
  [m now-ms]
  (into {} (keep (fn [[id es]] (let [es (in-window es now-ms)] (when (seq es) [id es])))) m))

(defn- count-or-stop!
  "Holds a thing to the blocks and limits of each level of its sender, and
  counts it toward each level when it goes on: every act counts toward the
  sender's own limit and toward the limit of each identity above it in its
  chain of makers, so an agent's or a program's acts count toward its
  person's (the build session's ruling of 15 October 2026, after the review
  found that a program dodged its person's block and limit). Takes the
  service, the levels, the thing, its entry {:at :layer :keys :bytes}, and
  the block function of `stop-of`. Gives `stop-of`'s refusal, or nil when
  the thing was counted. The test and the count are one change of the atom,
  so two things of one chain at once cannot both take the last place. Once
  a minute the ids with nothing in the last minute are swept out."
  [^Service s levels the-act entry block-of]
  (let [t (:at entry)
        ids (distinct (map :id levels))
        windows-of (fn [m] (into {} (map (fn [id] [id (in-window (get m id) t)])) ids))
        step (fn [m]
               (let [ws (windows-of m)
                     stop (stop-of levels ws the-act entry block-of)]
                 (reduce (fn [m id] (assoc m id (if stop (get ws id) (conj (get ws id) entry)))) m ids)))
        [before _] (swap-vals! (.-sent s) step)]
    (when (< minute-ms (- t @(.-swept s)))
      (reset! (.-swept s) t)
      (swap! (.-sent s) sweep t))
    (stop-of levels (windows-of before) the-act entry block-of)))

(defn- with-turn
  "Runs f with the turn of a key: one call at a time for one key, in this
  process. The turns are plain objects in a map that grows by one for each
  key that takes a turn."
  [^Service s k f]
  (let [^ConcurrentHashMap turns (.-turns s)
        o (.computeIfAbsent turns k (reify Function (apply [_ _] (Object.))))]
    (locking o (f))))

(defn- blocked!
  "Counts a blocked attempt of a sender and writes what the record keeps of
  it (`softland.runtime.system-steps/blocked-facts`): the count at 1, 2, 4,
  8 and so on, and the attempt while within `:rule/keep-first`. Takes the
  service, the sender, the thing stopped (an act, a query, or what a
  request asked), and why. Gives the count. The attempt is counted on the
  sender, also when a block or a limit of its maker stopped it. The count
  starts, at the sender's first blocked attempt in this process, from the
  record's `:blocked/count`. One sender's attempts take turns, so their
  counts are written in order. A write that fails leaves the count in
  memory, to be written at the next power of two."
  [s sender thing why]
  (with-turn s [:blocked sender]
    (fn []
      (let [held (:ok (identity-of s sender))
            r (:identity/record-layer held)
            ^Service svc s
            n0 (or (get @(.-blocked svc) sender)
                   (when r
                     (let [a (read-once s (steps/place-query ids/system r sender :blocked/count))]
                       (when-not (steps/failed? a) (steps/value-at a))))
                   0)
            n (inc (long n0))]
        (swap! (.-blocked svc) assoc sender n)
        (when r
          (let [ka (read-once s (steps/place-query ids/system ids/rules :blocked :rule/keep-first))
                keep-first (when-not (steps/failed? ka) (steps/value-at ka))
                fs (system-steps/blocked-facts sender n keep-first (fresh-uuid s)
                                               (system-steps/attempt-value thing why (now s)))]
            (when (seq fs)
              (send-act s (steps/system-act {:in {:system-session (system-session s)}} r fs)))))
        n))))

(defn- stopped
  "What a caller is told of a stop: the word, and for a limit which bound."
  [stop]
  (select-keys stop [:refuse :bound]))

;; ------------------------------------------------- requests on her behalf

(defn- whole-block
  "Takes a block, or nil. Gives it when it names no keys and no layers, so
  that it reaches whatever its sender asks; else nil."
  [block]
  (when (and (map? block) (not (contains? block :keys)) (not (contains? block :layers)))
    block))

(defn- on-behalf-stop
  "Holds a request that the runtime carries out as the system on an
  actor's behalf (a sign-in of a linked account, a name changed, an agent
  or a program made or copied, a token made or approved, a run recorded,
  an account linked) to the blocks and limits of her chain of makers. Takes
  the service, the actor ({:id :kind} is enough), and what she asked, a
  small map kept as the attempt when it is stopped. A block reaches such a
  request when it names no keys and no layers: a block of some keys or
  layers is a no to acts of that kind, and the request is no act of hers.
  The request is counted toward the limit of acts of each level, with no
  bytes, since it writes acts. Gives nil when it goes on, or {:refuse word
  …}: `:blocked` or `:over-limit` (each counted as a blocked attempt), or
  `:rules-not-read`. That these requests are held so is a session's choice,
  widened to the chain by the build session."
  [s actor what]
  (let [levels (levels-of s (:id actor) (:kind actor))]
    (if (:refuse levels)
      levels
      (when-let [stop (count-or-stop! s (:ok levels) {:layer nil :facts []}
                                      {:at (now s) :layer nil :keys #{} :bytes 0} whole-block)]
        (blocked! s (:id actor) what (:refuse stop))
        (stopped stop)))))

(defn- count-only!
  "Counts a thing toward one id's limit, with no test: a person's first
  sign-in, which has no person to hold to a rule until it is done."
  [^Service s id]
  (let [t (now s)]
    (swap! (.-sent s) (fn [m] (assoc m id (conj (in-window (get m id) t) {:at t :layer nil :keys #{} :bytes 0}))))
    nil))

;; ---------------------------------------------------------- acts and reads

(defn act!
  "An actor's act, made true and sent. Takes the service, the actor, and her
  act in the act's one form, without `:who` and `:session` or with hers.
  Gives {:ok {:act-id :digest :answer :session}}: the act-id and digest that
  the door gave, the answer as `softland.store.door/answer-of` gives it
  after `:wait-ms` ({:answer :accepted …}, {:answer :refused :reason word
  …}, or {:answer :nothing-yet} when the base's batch was not committed
  within the wait), and the session that the act names (her sitting, or
  an agent's run). Or a refusal before the door: a word of the binding
  (`softland.runtime.binding/act-words`), `:blocked`, `:over-limit` with
  `:bound` `:acts` or `:bytes`, `:rules-not-read`, or a word of the door.

  The way: the binding; the blocks and limits of the sender and of each
  identity above it in its chain of makers, read from the rules layer, and
  the counts of the last minute (`count-or-stop!`); then the door. A
  refusal by the gate is an answer, not a refusal of this call: the act is
  in the log."
  [s actor the-act]
  (guarded
   (when-started [_ s]
     (let [bound (binding/bind-act actor the-act)]
       (if (:refuse bound)
         bound
         (let [a (:ok bound)
               sender (:id actor)
               levels (levels-of s sender (:kind actor))]
           (if (:refuse levels)
             levels
             (if-let [stop (count-or-stop! s (:ok levels) a
                                           {:at (now s) :layer (:layer a) :keys (act-keys a) :bytes (act-bytes a)}
                                           identity)]
               (do (blocked! s sender a (:refuse stop)) (stopped stop))
               (let [r (send-act s a)]
                 (if (:could-not r)
                   (word->refuse r)
                   {:ok (assoc r :session (:session a))}))))))))))

(defn answer-of
  "The answer of an act read again, for an act whose answer was not there
  within the wait. Takes the service, the act-id and the digest that `act!`
  gave, and a wait in milliseconds or nil. Gives {:ok answer} or the door's
  word."
  [^Service s act-id digest wait-ms]
  (guarded
   (let [a (door/answer-of (.-door s) act-id digest (or wait-ms 0))]
     (if (:could-not a) (word->refuse a) {:ok a}))))

(defn- read-stop
  "Holds a read to the system's blocks of its asker and of each identity
  above her in her chain of makers (`levels-of`); a read is not counted
  toward a limit. Gives nil when the read goes on, or a refusal: `:blocked`,
  counted as a blocked attempt of the asker, or `:rules-not-read`."
  [s actor query]
  (let [levels (levels-of s (:id actor) (:kind actor))]
    (cond
      (:refuse levels) levels
      (some #(and (map? (:block %)) (binding/block-stops-read? (:block %) query)) (:ok levels))
      (do (blocked! s (:id actor) query :blocked) {:refuse :blocked})
      :else nil)))

(defn read!
  "A read once, as the actor, or as no one signed in when the actor is nil.
  Takes the service, the actor or nil, and a query
  (`softland.store.read-path.query`) that names no asker or hers. Gives
  {:ok answer} (`softland.store.read-path.answer`), or a refusal: a word of
  the binding (`softland.runtime.binding/query-words`), `:blocked`,
  `:rules-not-read`, or a word of the read path. Who may see each layer is
  the read path's to say: a layer set to everyone is read by anyone; one
  set to `:signed-in`, by any actor; one of members, by those with a
  right. A signed-in actor's read is held to the system's blocks of her and
  of whoever made her, up her chain of makers."
  [s actor query]
  (guarded
   (when-started [_ s]
     (let [bound (binding/bind-query actor query)]
       (if (:refuse bound)
         bound
         (let [q (:ok bound)]
           (or (when actor (read-stop s actor q))
               (let [a (read-once s q)]
                 (if (steps/failed? a) (word->refuse a) {:ok a})))))))))

(defn read-live!
  "A read kept live, as `read!` binds and holds it. Takes the service, the
  actor or nil, a query, and the asker's function, which the read path
  calls first with the whole answer and then with each change, on a thread
  of Rama's client; it must return at once
  (`softland.store.read-path/read-live`). Gives {:ok {:now :leave-with}}, or
  a refusal. Limit: the read is not ended when the actor's session ends;
  the caller leaves it."
  [^Service s actor query f]
  (guarded
   (when-started [_ s]
     (let [bound (binding/bind-query actor query)]
       (if (:refuse bound)
         bound
         (let [q (:ok bound)]
           (or (when actor (read-stop s actor q))
               (let [r (read-path/read-live (.-read-path s) q f)]
                 (if (:could-not r) (word->refuse r) {:ok r})))))))))

(defn leave!
  "Leaves a live read. Takes the service and the handle that `read-live!`
  gave under `:leave-with`. Gives {:ok :left}, or {:refuse :not-open} for a
  read left or ended before."
  [^Service s handle]
  (guarded
   (if (= :left (read-path/leave (.-read-path s) handle))
     {:ok :left}
     {:refuse :not-open})))

;; ------------------------------------------------------------------ names

(defn rename!
  "A name changed (`softland.runtime.system-steps/rename-step`). Takes the
  service, the person who asks, the new name, and, to name an agent or a
  program that she made, its id. Gives {:ok {:identity :name}}, or a
  refusal: `:only-a-person`, `:not-the-maker`, `:name-not-allowed`,
  `:name-taken`, `:name-waits` with until when.

  Only a person changes a name: her own, or an agent's or a program's that
  she made. That is a session's choice: an agent changes no setting unless
  its person allows it (BUILD.md, default 8), and the system's layers have
  no way yet for her to allow it."
  ([s actor new-name] (rename! s actor new-name nil))
  ([s actor new-name of]
   (guarded
    (when-started [sys s]
      (cond
        (not (person? actor)) {:refuse :only-a-person}
        :else
        (let [target (or of (:id actor))
              maker-ok (if (= target (:id actor))
                         {:ok true}
                         (let [held (identity-of s target)]
                           (cond
                             (:refuse held) held
                             (= (:id actor) (:identity/made-by (:ok held))) {:ok true}
                             :else {:refuse :not-the-maker})))]
          (if (:refuse maker-ok)
            maker-ok
            (or (on-behalf-stop s actor {:asked :rename :identity target :name new-name})
                (drive s system-steps/rename-step {:identity target :name new-name :now-ms (now s)
                                                   :system-session sys})))))))))

;; ------------------------------------------------ agents, tokens and runs

(defn- write-description!
  "The description of an agent or a program, written by its maker in her
  own layer, on its id (`:agent/description`; PROGRESS.md, \"Keys\"), as
  her act through `act!`. Gives what `act!` gives."
  [s actor own-layer id description]
  (act! s actor {:version act/version
                 :layer own-layer
                 :permission [(:id actor) own-layer own-layer]
                 :facts [{:e id :k :agent/description :v description}]}))

(defn- make-identity!
  "Drives the making of an agent or a program, then writes its
  description when there is one. Gives {:ok {:id :name}} with
  `:description`, what the description's act gave, when one was written."
  [s actor kind the-name description copied-from]
  (let [made (drive s system-steps/make-identity-step
                        {:kind kind :maker (:id actor) :name the-name
                         :fresh {:id (fresh-uuid s) :record-layer (fresh-uuid s)}
                         :copied-from copied-from :now-ms (now s) :system-session (system-session s)})]
    (if-let [{:keys [id name maker-own-layer]} (:ok made)]
      {:ok (cond-> {:id id :name name}
             (and description maker-own-layer)
             (assoc :description (write-description! s actor maker-own-layer id description)))}
      made)))

(defn make-agent!
  "An agent or a program made by a person (BUILD.md, default 7). Takes the
  service, the person, and {:kind :agent or :program (default :agent)
  :name text :description {:instructions text :tools any :engine {:provider
  :model :effort}}}, the description optional. Gives {:ok {:id :name}} and,
  when a description was given, `:description` with what her act gave; or
  a refusal: `:only-a-person`, a word of the description's check
  (`softland.runtime.records/check`, at where), or a word of the making
  (`softland.runtime.system-steps/make-identity-step`): `:name-taken`,
  `:name-waits`, `:name-not-allowed`.

  The identity, its name, its record layer with `:see` to it and to her,
  are the system's acts; the description is hers, in her own layer."
  [s actor {:keys [kind name description] :or {kind :agent}}]
  (guarded
   (when-started [_ s]
     (let [checked (when description (records/check :agent-description {:agent/description description}))]
       (cond
         (not (person? actor)) {:refuse :only-a-person}
         (:refuse checked) checked
         :else (or (on-behalf-stop s actor {:asked :make-agent :kind kind :name name})
                   (make-identity! s actor kind name (get-in checked [:ok :agent/description]) nil)))))))

(defn copy-agent!
  "A copy of an agent or a program: a new identity of the same kind, made
  by the person who copies, with `:identity/copied-from` the one it was
  made from (BUILD.md, default 7). Takes the service, the person, the id of
  the one to copy, and {:name text}. The description is copied when she can
  see it, in the maker's own layer, as herself; else the copy has none.
  Gives {:ok {:id :name}}, and `:description` when one was copied; or a
  refusal: `:only-a-person`, `:agent-not-found`, or a word of the making.
  That any person may copy any agent is a session's choice (who may copy
  an agent is open, BUILD.md, \"Later\")."
  [s actor agent-id {:keys [name]}]
  (guarded
   (when-started [_ s]
     (if-not (person? actor)
       {:refuse :only-a-person}
       (let [src (identity-of s agent-id)
             {kind :identity/kind maker :identity/made-by} (:ok src)]
         (cond
           (:refuse src) src
           (not (contains? #{:agent :program} kind)) {:refuse :agent-not-found}
           :else
           (or
            (on-behalf-stop s actor {:asked :copy-agent :of agent-id :name name})
            (let [maker-held (:ok (identity-of s maker))
                 o (:identity/own-layer maker-held)
                 seen (when o (read! s actor {:asks [:place agent-id :agent/description] :layers [o]}))
                 description (when (= :value (get-in seen [:ok :place :holds]))
                               (get-in seen [:ok :place :value]))]
             (make-identity! s actor kind name description agent-id)))))))))

(defn make-token!
  "A token made by a person for an agent or a program she made, which she
  then pastes where it runs (BUILD.md, default 5). Takes the service, the
  person, and the agent's id. Gives {:ok {:token :session}}: the token's
  text, which the store keeps only as its hash, and its session; or a
  refusal: `:only-a-person`, `:not-the-maker`."
  [s actor agent-id]
  (guarded
   (when-started [sys s]
     (if-not (person? actor)
       {:refuse :only-a-person}
       (or
        (on-behalf-stop s actor {:asked :make-token :for agent-id})
        (let [r (drive s system-steps/session-step
                          {:identity agent-id :made-by (:id actor)
                           :fresh {:session (fresh-uuid s) :secret (random-bytes s 32)}
                           :system-session sys})]
         (if-let [{:keys [token session]} (:ok r)]
           {:ok {:token token :session session}}
           r)))))))

(defn- lapse-requests!
  "Drops the token requests whose ten minutes have passed, and their turns."
  [^Service s]
  (let [t (now s)
        [before after] (swap-vals! (.-requests s)
                                   (fn [m] (into {} (remove (fn [[_ r]] (<= (+ (:asked-at r) request-lapse-ms) t))) m)))]
    (doseq [id (keys before) :when (not (contains? after id))]
      (.remove ^ConcurrentHashMap (.-turns s) [:request id]))))

(defn- drop-one
  "Takes the map of requests and a test of a request. Gives the map without
  the one request that passes the test and goes first: one not approved
  before one approved, then the oldest."
  [m pred]
  (if-let [[id] (first (sort-by (fn [[_ r]] [(if (:token r) 1 0) (:asked-at r)])
                                (filter (fn [[_ r]] (pred r)) m)))]
    (dissoc m id)
    m))

(defn ask-token!
  "An agent or a program asks for a token, to be approved by its maker in
  her browser (BUILD.md, default 5). Takes the service and the agent's id.
  Gives {:ok {:request :code :lapses-at}}: the request's id, which the agent
  keeps to collect the token; a short code that the agent shows and its
  person compares before she approves; and when the request lapses. Or a
  refusal: `:agent-not-found`.

  The request lives in the process's memory only, and lapses after ten
  minutes (`request-lapse-ms`), approved or not; a process that ends drops
  it. Anyone can ask for any agent: the code is how its person tells her
  agent's request from another's. A new request is never refused for room:
  when the agent holds `max-requests-per-identity` requests, its first one
  to go is dropped (one not approved before one approved, then the
  oldest), and when the process holds `max-requests`, the process's first
  one to go is dropped, so that asks in its name cannot shut an agent out
  (the build session's ruling of 15 October 2026, after the review)."
  [^Service s agent-id]
  (guarded
   (when-started [_ s]
     (let [held (identity-of s agent-id)
           {kind :identity/kind maker :identity/made-by the-name :identity/name} (:ok held)]
       (cond
         (:refuse held) held
         (not (and (contains? #{:agent :program} kind) (uuid? maker))) {:refuse :agent-not-found}
         :else
         (let [_ (lapse-requests! s)
               id (fresh-uuid s)
               t (now s)
               req {:identity agent-id :made-by maker :name the-name :code (short-code s) :asked-at t}
               step (fn [m]
                      (let [m (if (>= (count (filter #(= agent-id (:identity %)) (vals m))) max-requests-per-identity)
                                (drop-one m #(= agent-id (:identity %)))
                                m)
                            m (if (>= (count m) max-requests) (drop-one m (constantly true)) m)]
                        (assoc m id req)))
               [before after] (swap-vals! (.-requests s) step)]
           (doseq [k (keys before) :when (not (contains? after k))]
             (.remove ^ConcurrentHashMap (.-turns s) [:request k]))
           {:ok {:request id :code (:code req) :lapses-at (+ t request-lapse-ms)}}))))))

(defn pending-requests
  "The token requests that wait for a person's approval: those of agents
  and programs that she made, not approved, not lapsed. Takes the service
  and the person. Gives {:ok [{:request :identity :name :code :asked-at}]},
  the oldest first, or `:only-a-person`. It reads the process's memory and
  nothing of the store."
  [^Service s actor]
  (guarded
   (when-started [_ s]
     (if-not (person? actor)
       {:refuse :only-a-person}
       (do (lapse-requests! s)
           {:ok (->> @(.-requests s)
                     (keep (fn [[id r]] (when (and (= (:id actor) (:made-by r)) (nil? (:token r)))
                                          (assoc (select-keys r [:identity :name :code :asked-at]) :request id))))
                     (sort-by :asked-at)
                     vec)})))))

(defn approve-token!
  "A person approves a token request of an agent or a program she made:
  the token's session is made, as `make-token!` makes one, with the
  request's id, its code and when it was asked written beside it
  (`:session/asked`), so the record tells an approved token from a pasted
  one; and the token is held with the request until the agent collects it.
  Takes the service, the person, and the request's id. Gives
  {:ok {:request :session}}, or a refusal: `:only-a-person`,
  `:request-not-known`, `:not-the-maker`, or a word of
  `softland.runtime.system-steps/session-step`. The id must be a UUID of a
  request that is held before any turn is taken, so unknown ids take no
  memory. One request is approved once: a second approval gives the
  session of the first."
  [^Service s actor request]
  (guarded
   (when-started [sys s]
     (lapse-requests! s)
     (cond
       (not (person? actor)) {:refuse :only-a-person}
       (not (and (uuid? request) (contains? @(.-requests s) request))) {:refuse :request-not-known}
       :else
       (let [result
             (with-turn s [:request request]
               (fn []
                 (let [r (get @(.-requests s) request)]
                   (cond
                     (nil? r) {:refuse :request-not-known}
                     (not= (:id actor) (:made-by r)) {:refuse :not-the-maker}
                     (:session r) {:ok {:request request :session (:session r)}}
                     :else
                     (or
                      (on-behalf-stop s actor {:asked :approve-token :for (:identity r)})
                      (let [made (drive s system-steps/session-step
                                        {:identity (:identity r) :made-by (:id actor)
                                         :fresh {:session (fresh-uuid s) :secret (random-bytes s 32)}
                                         :asked {:request request :code (:code r) :asked-at (:asked-at r)}
                                         :system-session sys})]
                        (if-let [{:keys [token session]} (:ok made)]
                          (do (swap! (.-requests s) (fn [m] (if (contains? m request)
                                                              (update m request assoc :token token :session session)
                                                              m)))
                              {:ok {:request request :session session}})
                          made)))))))]
         (when-not (contains? @(.-requests s) request)
           (.remove ^ConcurrentHashMap (.-turns s) [:request request]))
         result)))))

(defn collect-token!
  "An agent or a program collects the token of its approved request, once.
  Takes the service and the request's id. Gives {:ok {:token :session
  :identity}} and drops the request; or a refusal: `:not-approved-yet`, or
  `:request-not-known` for a request never made, collected, or lapsed."
  [^Service s request]
  (guarded
   (when-started [_ s]
     (lapse-requests! s)
     (let [[before _] (swap-vals! (.-requests s) (fn [m] (if (get-in m [request :token]) (dissoc m request) m)))
           r (get before request)]
       (cond
         (nil? r) {:refuse :request-not-known}
         (nil? (:token r)) {:refuse :not-approved-yet}
         :else (do (.remove ^ConcurrentHashMap (.-turns s) [:request request])
                   {:ok {:token (:token r) :session (:session r) :identity (:identity r)}}))))))

(defn end-token!
  "Ends a session, so that its token matches nothing
  (`softland.runtime.system-steps/end-session-step`). Takes the service,
  the actor, the session's id, and the id of the identity whose session it
  is when that is not the actor: an agent or a program that she made. The
  actor may end her own sessions, and those of the identities she made.
  Gives {:ok {:session :ended-before?}} or a refusal: `:actor-not-known`,
  `:may-not-end-this-session`, `:session-not-found`."
  ([s actor session-id] (end-token! s actor session-id nil))
  ([s actor session-id of]
   (guarded
    (when-started [sys s]
      (if-not (actor? actor)
        {:refuse :actor-not-known}
        (drive s system-steps/end-session-step
                   {:identity (or of (:id actor)) :session session-id :by (:id actor)
                    :now-ms (now s) :system-session sys}))))))

(defn start-run!
  "An agent's run recorded in its record layer (BUILD.md, default 9;
  `softland.runtime.system-steps/run-step`). Takes the service, the agent
  (an actor of kind :agent, from its token), and {:engine {:provider
  :model :effort} :context value :human-in-loop bool :cause value}. Gives
  {:ok {:run :description}}: the run's id, and the fact id of the
  description that it names, or nil. The agent's acts then name the run:
  its actor is `actor-of` with the run, or the actor with `:run` set. Or a
  refusal: `:only-an-agent-runs`, `:session-ended`, a word of the run's
  record."
  [s actor run]
  (guarded
   (when-started [sys s]
     (if-not (and (actor? actor) (= :agent (:kind actor)))
       {:refuse :only-an-agent-runs}
       (or
        (on-behalf-stop s actor {:asked :start-run})
        (drive s system-steps/run-step {:agent (:id actor) :session (:session actor) :run run
                                         :fresh {:run (fresh-uuid s)} :system-session sys}))))))

;; ------------------------------------------------------------------ rights

(defn permission-of
  "The actor's permission in a layer, as she reads it. Takes the service,
  the actor and a layer. Gives {:ok permission-id}, or a refusal:
  `:no-right-in-layer` when her place holds none that she may see, or a
  refusal of `read!`."
  [s actor layer]
  (guarded
   (let [r (read! s actor {:asks [:place (:id actor) :softland.store/permission]
                           :layers [layer] :stores-own-facts true})]
     (if (:refuse r)
       r
       (if-let [p (steps/value-at (:ok r))]
         {:ok p}
         {:refuse :no-right-in-layer})))))

(defn give-right!
  "A right given by the actor in a layer, as her act through `act!`: a
  permission on the one it is given to, beneath her own permission there,
  allowing what she says (`:see`, `:see-and-act`, or a set of key patterns).
  Left out, a person is given a permission with no key list, which allows
  every key that hers allows; an agent or a program is given `#{:*}`, every
  plain key and no store key, so that it gives no right and changes no
  setting unless she names those keys (BUILD.md, default 8; the build
  session's ruling of 15 October 2026). Takes the service, the actor, and
  {:layer :to :allows}. Gives what `act!` gives, with `:permission`, the
  permission given, under `:ok`; or a refusal of `permission-of`. Whether
  she may give it, and whether it allows no more than hers, is the gate's
  to decide."
  [s actor {:keys [layer to allows]}]
  (guarded
   (let [mine (permission-of s actor layer)]
     (if-let [p (:ok mine)]
       (let [allows (if (some? allows)
                      allows
                      (when (contains? #{:agent :program} (:identity/kind (:ok (identity-of s to))))
                        #{:*}))
             given (cond-> [to layer layer p] (some? allows) (conj allows))
             r (act! s actor {:version act/version :layer layer :permission p
                              :facts [{:e to :k :softland.store/permission :v given}]})]
         (if (:ok r) (assoc-in r [:ok :permission] given) r))
       mine))))

(defn end-right!
  "A right ended by the actor, as her act through `act!`: the retract of
  the permission on its holder, under her own permission in the layer.
  The gate takes it from one above the right in its chain (BUILD.md,
  default 11). Takes the service, the actor, and {:layer :of}. Gives what
  `act!` gives, or a refusal of `permission-of`."
  [s actor {:keys [layer of]}]
  (guarded
   (let [mine (permission-of s actor layer)]
     (if-let [p (:ok mine)]
       (act! s actor {:version act/version :layer layer :permission p
                      :facts [{:e of :k :softland.store/permission}]})
       mine))))

(defn own-identity
  "The actor's own identity, as the system keeps it in `identities`: her
  kind, name, record layer, and, for a person, her own layer; for an agent
  or a program, who made it and what it was copied from. Takes the service
  and the actor. Gives {:ok record}, the map of the identity's keys that
  hold a value, or a refusal: `:actor-not-known`, or a word of the read
  path. The system's layers are members only, and the system reads them
  for her: an identity reads its own record and no other."
  [s actor]
  (guarded
   (when-started [_ s]
     (if-not (actor? actor)
       {:refuse :actor-not-known}
       (identity-of s (:id actor))))))

;; ---------------------------------------------------------- the admin calls

(defn invite!
  "An invitation made by Sid's command line (BUILD.md, default 4): its
  code's hash and the rights it gives, recorded in `invitations` by the
  system (`softland.runtime.sign-in/invitation`). Takes the service and
  {:gives [{:layer :allows}] :made-by id}, the maker `:system` when left
  out. Each right is given beneath the system's first permission in its
  layer at the person's first sign-in. Gives {:ok {:code :invitation}}: the
  code to hand to the person, which the store never holds; or a refusal."
  [s {:keys [gives made-by]}]
  (guarded
   (when-started [sys s]
     (let [id (fresh-uuid s)
           made (sign-in/invitation {:invitation id :secret (random-bytes s 32) :gives (vec gives)
                                     :made-by (or made-by ids/system) :system-session sys})]
       (if-let [{:keys [code] the-act :act} (:ok made)]
         (let [r (system-act! s the-act)]
           (if (:ok r) {:ok {:code code :invitation id}} r))
         made)))))

(defn link-by-admin!
  "An account linked to a person by Sid's command line, for a person who
  lost every account (BUILD.md, default 3). Takes the service, the account
  {:kind :google :sub text}, and the person's id. Gives
  {:ok {:person :account}}, or a refusal: `:identity-not-a-person`,
  `:account-linked-to-another`, `:input-not-whole`. The steps are those of a
  second account linked (`softland.runtime.sign-in/link-account-step`),
  for her."
  [s account person]
  (guarded
   (when-started [sys s]
     (let [held (identity-of s person)]
       (cond
         (:refuse held) held
         (not= :person (:identity/kind (:ok held))) {:refuse :identity-not-a-person}
         :else
         (drive s sign-in/link-account-step {:actor {:id person :kind :person} :account account
                                                :system-session sys}))))))

(defn block!
  "A block of a sender by the system, written in the rules layer on the
  sender (`:rule/block`, written again over a block there): {:keys #{key
  pattern} :layers #{layer} :per-minute n}, each part optional; a block
  with no `:keys` and no `:per-minute` stops every act and read in its
  layers, or everywhere when it names none (BUILD.md, default 12). Takes
  the service, the sender's id and the block. Gives {:ok {:sender}}, or a
  refusal: a word of the block's check, at where, or `:act-refused`."
  [s sender block]
  (guarded
   (when-started [sys s]
     (let [fs (records/facts-of :block sender {:rule/block block})]
       (if-let [facts (:ok fs)]
         (let [r (system-act! s (steps/system-act {:in {:system-session sys}} ids/rules facts))]
           (if (:ok r) {:ok {:sender sender}} r))
         fs)))))

(defn set-limits!
  "Limits written by Sid's command line, in the rules layer, on a kind of
  actor (`:person`, `:agent`, `:program`) or on one identity's id: acts a
  minute (`:rule/acts-per-minute`) and bytes a minute
  (`:rule/bytes-per-minute`), each written again over what is there. A
  rule on an id is read before its kind's (`levels-of`), so a program that
  needs more than a person's limit is given its own. Takes the service, the
  entity, and {:acts-per-minute n :bytes-per-minute n}, either or both.
  Gives {:ok {:entity}}, or a refusal: `:input-not-whole` for neither, a
  word of the rule's check, at where, or `:act-refused`."
  [s entity limits]
  (guarded
   (when-started [sys s]
     (let [parts (concat (when (contains? limits :acts-per-minute)
                           [(records/facts-of :limit entity {:rule/acts-per-minute (:acts-per-minute limits)})])
                         (when (contains? limits :bytes-per-minute)
                           [(records/facts-of :byte-limit entity {:rule/bytes-per-minute (:bytes-per-minute limits)})]))]
       (cond
         (empty? parts) {:refuse :input-not-whole}
         (some :refuse parts) (first (filter :refuse parts))
         :else (let [r (system-act! s (steps/system-act {:in {:system-session sys}} ids/rules (mapcat :ok parts)))]
                 (if (:ok r) {:ok {:entity entity}} r)))))))

(defn unblock!
  "A block of a sender lifted: the retract of `:rule/block` on it. Takes
  the service and the sender's id. Gives {:ok {:sender}} or a refusal."
  [s sender]
  (guarded
   (when-started [sys s]
     (let [r (system-act! s (steps/system-act {:in {:system-session sys}} ids/rules
                                              [{:e sender :k :rule/block}]))]
       (if (:ok r) {:ok {:sender sender}} r)))))

;; ---------------------------------------------------------- the proofs' calls

(defn read-as-system
  "A read as the system, past the binding and the blocks: the runtime's own
  read, for the proofs and Sid's command line. Takes the service and a
  query; its asker is set to the system. Gives {:ok answer} or the read
  path's word as a refusal. An actor's read goes through `read!`."
  [s query]
  (guarded
   (let [a (read-once s (-> query (dissoc :not-signed-in) (assoc :asker ids/system)))]
     (if (steps/failed? a) (word->refuse a) {:ok a}))))

(defn sent-in-last-minute
  "What this process counted toward an id's limits in the last minute: its
  own acts and requests, and, for a person, those of the agents and
  programs she made. Takes the service and the id. Gives {:ok {:acts n
  :bytes n}}. It reads the process's memory and nothing of the store; for
  the proofs and Sid's command line."
  [^Service s id]
  (guarded
   (let [es (in-window (get @(.-sent s) id) (now s))]
     {:ok {:acts (count es) :bytes (reduce + 0 (map :bytes es))}})))

(defn step-io
  "The read and the send that the service drives its steps with, as
  {:read! f :send! f}: `:read!` reads one query as it is, `:send!` sends one
  act and gives its outcome after the wait. For the proofs, which drive a
  sign-in and cut it. Whoever holds them acts as the system."
  [s]
  {:read! #(read-once s %) :send! #(outcome-of s %)})
