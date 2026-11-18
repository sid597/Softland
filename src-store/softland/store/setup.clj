(ns softland.store.setup
  "Setting a store up: the first who of a store, its first act, and the
  sending of a signed act. It requires the door, and so Rama's client; it
  makes no call of Rama itself.

  Why it is here. Every act names a who, and a gate can hold that the
  sender of a signed act is that who (`softland.store.door.signature`). The
  first act of a store has nothing before it to be held against, so it
  does not identify the store's first who: it defines it. It is the signed
  making act of the base, whose `:who` is a public half and which is
  signed with the private half of the same pair
  (`softland.store.gate.first-act/signed-making-act`). The gate of the base
  takes the first right making act to arrive and no second one. So whoever
  sets a store up makes a pair, sends that act, and reads its answer: an
  accept says that the base is hers, and a refusal that another act made
  it first. That the first act carries its who's public half and is signed
  with it is Sid's choice of 18 November 2026; this form of it is the
  builder's.

  Inputs and results, by call:

    new-pair         nothing -> {:who a signing who, :keeper a keeper}
    send-signed!     a door, a keeper, a maker's signed act with no
                     signature, and a wait in milliseconds -> what was
                     sent and its answer, or a word
    make-the-base!   a door, a pair, a session id, and a wait -> the same,
                     for the store's first act
    keep-first-act!  a file's path, and what `make-the-base!` gave -> the
                     path, or nil
    kept-first-act   a file's path -> the kept first act, or nil
    send-again!      a door, a kept first act, and a wait -> its answer,
                     or a word

  A keeper is a function that is handed the bytes to sign and gives the 64
  bytes of an Ed25519 signature back. It alone touches a private half.
  The one keeper that this build makes is `new-pair`'s: it holds the
  private half in the memory of this process, gives it to nobody, and
  writes it nowhere. So the who of such a pair can act while the process
  lives and never after. That is enough for a store that is thrown away,
  which is every store of this build. A store that is kept needs a keeper
  that lasts and that does not give the private half out; none is built,
  and which one it is is Sid's to say.

  The kept first act. It is the signed act and its act-id, as text in a
  file: what was sent, without the time on it. It holds a public half and
  a signature and nothing that must be hidden. Sent again to the store
  that holds it, it changes nothing: a gate answers an act-id once, and
  the answer that comes back is the first one. Sent to a store with
  nothing in it, it makes the base for the same who. A store that is new
  and a store that lost its rows look alike from inside, so the file is
  what tells its holder which of the two she has.

  State and effects. The namespace owns no state. `new-pair` draws from the
  JVM's secure random source. `send-signed!`, `make-the-base!` and
  `send-again!` make the door's round trips, one append among them, and
  wait for an answer. `keep-first-act!` writes one file, and
  `kept-first-act` reads one.

  Callers. The proof of this namespace. No program of this build sets a
  store up: there is no store that is kept, and nothing starts one.

  Not here. A keeper that lasts. The give of a permission to a second who:
  it is an ordinary signed act, sent with `send-signed!`. What a server
  does at its start with a store whose first who is not the one that it
  was told to expect."
  (:require [clojure.edn :as edn]
            [softland.store.door :as door]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.signature :as signature]
            [softland.store.gate.first-act :as first-act])
  (:import [java.security KeyPairGenerator Signature]))

(defn new-pair
  "Makes a new pair. Gives a map: `:who`, the signing who that holds the
  public half; and `:keeper`, a function that is handed bytes and gives
  the 64 bytes of their Ed25519 signature, made with the private half.
  The private half is inside the keeper and nowhere else: this function
  gives it to no caller, prints it nowhere and writes no file. When the
  process ends it is gone, and nothing can sign as that who again.

  It draws from the JVM's secure random source, so it is not pure, and two
  calls give two pairs."
  []
  (let [pair (.generateKeyPair (KeyPairGenerator/getInstance "Ed25519"))
        private (.getPrivate pair)]
    {:who (signature/who-of (.getPublic pair))
     :keeper (fn [^bytes to-sign]
               (.sign (doto (Signature/getInstance "Ed25519")
                        (.initSign private)
                        (.update to-sign))))}))

(defn- signed
  "Takes a keeper, a maker's signed act with no signature, and the act-id
  that the door gave for it. Gives the act with its `:signature`, or nil
  when the keeper made none. What is signed is the thing that the door
  will append, built by the door's own function
  (`softland.store.door.additions/thing-to-append`), without its time and
  its signature (`softland.store.door.declaration/what-is-signed`): so the
  sender signs what the gate checks."
  [keeper act act-id]
  (let [thing (:thing (additions/thing-to-append {:thing act} act-id 0))]
    (when-let [made (and thing (signature/sign keeper (declaration/what-is-signed thing)))]
      (assoc act :signature made))))

(defn send-signed!
  "Signs one act and sends it. Takes a door; a keeper; a maker's act of
  version 2 whose `:who` is the keeper's who, with no `:signature`; and
  how long to wait for the answer, in milliseconds. Gives a map:

    :act     the signed act, as it was sent
    :act-id  the act-id that the door gave for it ahead
    :digest  the digest that the door gave for what it appended
    :answer  what `softland.store.door/answer-of` gave within the wait

  or one of the door's own words, when the door could not make the act-id
  or send the act; or `{:could-not :sign-the-act}`, with nothing appended,
  when the keeper made no signature.

  The steps: one read for the act-id, of the layer's depot; the signature,
  which needs the act-id, since the act-id is inside what is signed; one
  append; and the read of the answer, with the wait. An accept says that
  the gate took the act as its who's. It does not check that the keeper's
  who is the act's `:who`: an act signed with another pair is appended, and
  its gate refuses it `:signature-does-not-fit`."
  [door keeper act wait-ms]
  (let [act-id (door/act-id-for door (:layer act))]
    (if (:could-not act-id)
      act-id
      (if-let [signed-act (signed keeper act act-id)]
        (let [sent (door/send-act door signed-act act-id)]
          (if (:could-not sent)
            sent
            {:act signed-act
             :act-id act-id
             :digest (:digest sent)
             :answer (door/answer-of door act-id (:digest sent) wait-ms)}))
        {:could-not :sign-the-act :appended :nothing}))))

(defn make-the-base!
  "Sends the first act of a store. Takes a door; a pair, as `new-pair`
  gives one; a session id; and how long to wait for the answer, in
  milliseconds. Builds the signed making act of the base for the pair's
  who (`softland.store.gate.first-act/signed-making-act`), and signs and
  sends it (`send-signed!`). Gives what `send-signed!` gives.

  What the answer says. `:accepted`: this act made the base, and the
  pair's who holds the base's first permission, with nothing above it.
  `:refused`: the base was made before this act arrived, by another act;
  the store's first who is not this pair's, and the caller holds a store
  that is not hers to set up. `:nothing-yet`: the wait passed first, and
  nothing is known. The caller must read the answer: nothing else tells
  her whose the base is.

  Limit. It keeps no file; `keep-first-act!` does."
  [door pair session wait-ms]
  (send-signed! door
                (:keeper pair)
                (first-act/signed-making-act first-act/base (:who pair) session)
                wait-ms))

(defn keep-first-act!
  "Writes the kept first act to a file. Takes the file's path, and what
  `make-the-base!` gave for an act that was sent. Writes `:act` and
  `:act-id` of it as one map, as EDN text, and gives the path; or writes
  nothing and gives nil, when the text does not read back as the map.

  The file holds a public half and a signature. It holds no private half
  and nothing else that must be hidden; whoever reads it can send the
  first act again, which makes no base that is not its who's. It writes
  over a file that is there."
  [path made]
  (let [kept (select-keys made [:act :act-id])
        as-text (pr-str kept)]
    (when (and (:act kept) (:act-id kept) (= kept (edn/read-string as-text)))
      (spit path as-text)
      path)))

(defn kept-first-act
  "Reads a kept first act back. Takes the file's path. Gives the map that
  `keep-first-act!` wrote, `:act` and `:act-id`, or nil for a file that is
  not there or does not hold such a map as EDN. It never throws."
  [path]
  (try
    (let [kept (edn/read-string (slurp path))]
      (when (and (map? kept) (:act kept) (:act-id kept))
        kept))
    (catch Throwable _ nil)))

(defn send-again!
  "Sends a kept first act again, under its own act-id. Takes a door; a kept
  first act, as `kept-first-act` gives one; and how long to wait for the
  answer, in milliseconds. Gives a map, `:digest` and `:answer`, or one of
  the door's own words.

  What it does to a store. One more record on the base's depot, always. In
  a store that holds the act's answer, no row changes: the gate finds the
  answer under the act-id and decides nothing, and the answer that comes
  back is the first one, with its stamp. In a store with nothing in it,
  the act is decided now, and makes the base for its who. In a store whose
  base another act made, it is refused. The time on the second send is
  another, which neither the signature nor the digest holds, so the
  signature fits and the answer is read as the sender's own."
  [door kept wait-ms]
  (let [sent (door/send-act door (:act kept) (:act-id kept))]
    (if (:could-not sent)
      sent
      {:digest (:digest sent)
       :answer (door/answer-of door (:act-id kept) (:digest sent) wait-ms)})))
