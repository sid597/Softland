(ns softland.wire.words
  "The words: everything that a system can say to the runtime over the
  line, declared as data, with the check of that form; and everything that
  the runtime says in place of a result. Pure code.

  There is no word for a feature of the product. A new thing that a system
  can do is a new key in a lane, written with `:send` and read with
  `:read`. `:read` and `:watch` name no lane themselves: they carry a
  query, and what a query can ask grows with the read path. A session's
  trail would be read by the same two words; that is not designed (the
  runtime's page, entry 5).

  The whole form goes on the wire. An act is the store's own maker's act
  (`softland.store.act`), with its `:who` and its `:session`; a query is
  the read path's (`softland.store.read-path.query`), with its `:asker` or
  with `:not-signed-in true`. The runtime holds those parts against the
  caller and refuses a difference (`softland.runtime.seam`); it strips
  nothing and writes nothing into them. The act-id is the maker's too: a
  random UUID, which the runtime completes with the lane and its depot. A
  maker that sends the same act under the same UUID a second time, after a
  break, sends the same thing, and a gate takes it once.

  A system chooses no id but an act's UUID. `:invite` and `:make-agent`
  carry a `:uuid` that the system made; the runtime derives the id of the
  person or of the agent from it and from the caller's root, and gives the
  id back (`softland.runtime.seam`).

  What the check reads, and what it leaves. A word's shape, in the language
  of `softland.store.act/shapes`, holds every part of a word but the one
  that carries a form of the store's: `:act`, `:acts` or `:query`. Of such
  a part the check tests only that it is a plain map. The store judges the
  rest: an act whose form is wrong goes to its depot, and a gate refuses it
  under its act-id, as for a maker who may not act (src-store/README.md,
  12 October 2026, entry 7); a query is checked by the read path.

  `check` never throws."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.wire.credential :as credential]))

;; ------------------------------------------------------------- the numbers

(def max-frame-bytes
  "How many bytes one frame of somebody who is signed in can be. The line
  sets it on the connection, so a longer frame is never taken into memory.
  It is the store's bound on what one act can write into rows
  (`softland.store.door.declaration/max-act-bytes`) and one MiB for the
  form around the values: an act that a gate can take fits."
  (+ declaration/max-act-bytes (* 1024 1024)))

(def max-visitor-frame-bytes
  "How many bytes one frame can be on a connection that stands for nobody.
  A visitor reads and watches, which are small, and sends no act."
  65536)

(def max-acts-of-one-word
  "How many acts one `:send-all` can carry."
  1024)

(def max-text
  "How many characters a label or what a program says of itself can have.
  Auth's own bound on a text (its spec, \"The client's contract\")."
  256)

(def default-wait-ms
  "How long a `:send` waits for its answer when the word names no wait."
  10000)

(def longest-wait-ms
  "The longest wait that a word can ask for, in milliseconds. A longer one
  is cut to it; a system that waits longer asks again."
  60000)

;; ---------------------------------------------------- the declared words

(def words
  "`:carries` is the part that carries a form of the store's, nil for none.
  `:who` is who may say the word: `:anybody`, a visitor too; `:an-actor`,
  whoever is signed in; `:a-person`, a person herself and no agent.
  `:in-a-session` marks a word that only a caller who is in a session
  says. The two are the rule itself and not a second telling of it: the
  runtime reads them to hold a word against its caller
  (`softland.runtime.seam/word-fault`).
  `:can-end-its-connection` marks a word whose work can end the sign-in or
  the session that the connection it was said on stands on. The line reads
  it: the result of such a word is written before the connection is told
  its end (`softland.runtime.line`)."
  {:who-am-i       {:says "who the connection is: her who, her root, her session, her own lane; or that it stands for nobody"
                    :carries nil :who :anybody}
   :send           {:says "one act into its depot, and its answer: the door's receipt with the answer that a gate gave, or `:nothing-yet` when the wait passed"
                    :carries :act :who :an-actor :in-a-session true}
   :send-all       {:says "acts of one maker, each under its own UUID, sent in order; a result for each that was tried, ending at the first that could not be sent"
                    :carries :acts :who :an-actor :in-a-session true}
   :answer         {:says "the answer of an act that was sent before, by its act-id and the digest of its receipt"
                    :carries nil :who :an-actor}
   :read           {:says "a read, once"
                    :carries :query :who :anybody}
   :watch          {:says "a read, kept live: the whole answer, then each change by place, until it is left or ends"
                    :carries :query :who :anybody}
   :leave          {:says "leaves a watch of this connection, named by the number of the word that opened it"
                    :carries nil :who :anybody}
   :replace-secret {:says "the sign-in's secret becomes the one whose hash is given; the session stays as it is"
                    :carries nil :who :an-actor}
   :sign-out       {:says "ends the sign-in of this connection, on every copy of the runtime"
                    :carries nil :who :an-actor :can-end-its-connection true}
   :open-session   {:says "opens a new session under this sign-in, optionally saying whether a person or a model drives it"
                    :carries nil :who :an-actor}
   :close-session  {:says "closes the session of this connection"
                    :carries nil :who :an-actor :in-a-session true :can-end-its-connection true}
   :invite         {:says "makes a person who waits for an account that proves the claim"
                    :carries nil :who :a-person}
   :make-agent     {:says "makes an agent beneath the caller, and a sign-in for it under the hash of a secret that the caller made"
                    :carries nil :who :an-actor}
   :end-agent      {:says "ends an agent beneath the caller, with every actor that ends with it and every sign-in of each"
                    :carries nil :who :an-actor}
   :under          {:says "the actors of the caller's root, and its live sign-ins"
                    :carries nil :who :a-person}
   :end-sign-in    {:says "ends one sign-in of the caller's root"
                    :carries nil :who :a-person :can-end-its-connection true}})

(def ^:private wait-part
  [:wait-ms [:int {:min 0}] {:optional true :reason :wait-is-no-span}])

(def shapes
  "Each entry of `softland.store.act/shapes`, and one shape for each word.
  A word's shape holds each of its parts but the one that carries a form of
  the store's (`words`, `:carries`), which the engine does not read. A part
  that is left out holds nil in the one form."
  (assoc act/shapes
         :who-am-i       [:parts [:word [:enum :who-am-i]]]
         :send           [:parts
                          [:word   [:enum :send]]
                          [:act-id :uuid {:absent :act-id-not-given :reason :act-id-is-no-uuid}]
                          wait-part]
         :send-all       [:parts
                          [:word [:enum :send-all]]
                          wait-part]
         :answer         [:parts
                          [:word    [:enum :answer]]
                          [:act-id  :act-id {:absent :act-id-not-given :reason :act-id-has-no-form}]
                          [:digest  :value  {:absent :digest-not-given :reason :digest-is-no-text}]
                          wait-part]
         :read           [:parts [:word [:enum :read]]]
         :watch          [:parts [:word [:enum :watch]]]
         :leave          [:parts
                          [:word  [:enum :leave]]
                          [:watch [:int {:min 0}] {:absent :watch-not-named :reason :watch-not-named}]]
         :replace-secret [:parts
                          [:word [:enum :replace-secret]]
                          [:hash :value {:absent :hash-not-given :reason :hash-has-no-form}]]
         :sign-out       [:parts [:word [:enum :sign-out]]]
         :open-session   [:parts
                          [:word [:enum :open-session]]
                          [:driven-by :value {:optional true :reason :driven-by-has-no-form}]]
         :close-session  [:parts [:word [:enum :close-session]]]
         :invite         [:parts
                          [:word  [:enum :invite]]
                          [:uuid  :uuid  {:absent :uuid-not-given :reason :uuid-is-no-uuid}]
                          [:claim :value {:absent :claim-not-given :reason :claim-has-no-form}]]
         :make-agent     [:parts
                          [:word  [:enum :make-agent]]
                          [:uuid  :uuid  {:absent :uuid-not-given :reason :uuid-is-no-uuid}]
                          [:label :value {:absent :label-not-given :reason :label-is-no-text}]
                          [:hash  :value {:absent :hash-not-given :reason :hash-has-no-form}]]
         :end-agent      [:parts
                          [:word  [:enum :end-agent]]
                          [:agent :uuid {:absent :agent-not-named :reason :agent-is-no-uuid}]]
         :under          [:parts [:word [:enum :under]]]
         :end-sign-in    [:parts
                          [:word    [:enum :end-sign-in]]
                          [:sign-in :uuid {:absent :sign-in-not-named :reason :sign-in-is-no-uuid}]]))

(def reasons
  "The words with which `check` turns a thing away. Each names what is
  wrong.

    :not-a-word          the thing is no plain map, or threw while read
    :word-not-known      its `:word` names none of `words`
    :part-not-known      it holds a part that its word does not have
    :part-has-no-form    a part that names no reason of its own did not fit
    :not-given           the part that carries the store's form is absent
    :not-a-plain-map     that part, or an act of `:acts`, is no plain map
    :acts-not-a-series   `:acts` is no vector of {:act :act-id} maps, or it
                         is empty
    :too-many-acts       `:acts` holds more than `max-acts-of-one-word`
    :digest-is-no-text   the digest is no text
    :hash-has-no-form    the hash is not 64 lower-case hex characters
    :label-is-no-text    the label is no text of 1 to `max-text` characters
    :claim-has-no-form     the claim is neither {:by :email :email text} nor
                           {:by :code :hash hash}
    :driven-by-has-no-form :driven-by is neither {:kind :person} nor
                           {:kind :model :model text}

  and each word that a part of `shapes` names."
  (into #{:not-a-word :word-not-known :part-not-known :part-has-no-form :not-given
          :not-a-plain-map :acts-not-a-series :too-many-acts :digest-is-no-text
          :hash-has-no-form :label-is-no-text :claim-has-no-form :driven-by-has-no-form
          :act-id-is-no-uuid}
        (for [word (keys words)
              [_ _ opts] (rest (get shapes word))
              reason (keep #(get opts %) [:reason :absent :empty])]
          reason)))

;; ------------------------------------------------------------------ the check

(defn- plain-map?
  [x]
  (and (map? x) (not (record? x))))

(defn text?
  [x]
  (and (string? x) (<= 1 (count x) max-text)))

(defn driven-by?
  [x]
  (and (plain-map? x)
       (case (:kind x)
         :person (= #{:kind} (set (keys x)))
         :model (and (= #{:kind :model} (set (keys x)))
                     (text? (:model x)))
         false)))

(defn claim?
  [x]
  (and (plain-map? x)
       (case (:by x)
         :email (and (= #{:by :email} (set (keys x)))
                     (text? (:email x))
                     (boolean (re-matches #"[^@\s]+@[^@\s]+" (:email x))))
         :code (and (= #{:by :hash} (set (keys x)))
                    (credential/hash? (:hash x)))
         false)))

(defn- carried-fault
  [part thing]
  (cond
    (nil? thing) {:refuse :not-given :at part}
    (not (plain-map? thing)) {:refuse :not-a-plain-map :at part}))

(defn- acts-fault
  "The count is taken with a bound, before any entry is read."
  [acts]
  (cond
    (nil? acts) {:refuse :not-given :at :acts}
    (not (vector? acts)) {:refuse :acts-not-a-series :at :acts}
    (empty? acts) {:refuse :acts-not-a-series :at :acts}
    (< max-acts-of-one-word (count acts)) {:refuse :too-many-acts :at :acts}
    :else (some (fn [entry]
                  (cond
                    (not (and (plain-map? entry) (= #{:act :act-id} (set (keys entry)))))
                    {:refuse :acts-not-a-series :at :acts}
                    (not (uuid? (:act-id entry))) {:refuse :act-id-is-no-uuid :at :acts}
                    :else (carried-fault :act (:act entry))))
                acts)))

(defn- part-at-fault
  "`thing` is one that the engine refused under the word's shape. Gives the
  first part that the shape does not hold, or else the first declared part,
  in the declared order, that does not fit by itself."
  [word thing]
  (let [entries (rest (get shapes word))
        declared (into #{} (map first) entries)]
    (or (first (remove declared (keys thing)))
        (some (fn [[part :as entry]]
                (when (= :no (first (act/as-shape shapes [:parts entry] (select-keys thing [part]))))
                  part))
              entries))))

(defn- own-fault
  "For the parts whose form the engine's leaves cannot say."
  [word]
  (cond
    (and (contains? word :digest) (not (string? (:digest word)))) {:refuse :digest-is-no-text :at :digest}
    (and (contains? word :hash) (not (credential/hash? (:hash word)))) {:refuse :hash-has-no-form :at :hash}
    (and (contains? word :label) (not (text? (:label word)))) {:refuse :label-is-no-text :at :label}
    (and (contains? word :claim) (not (claim? (:claim word)))) {:refuse :claim-has-no-form :at :claim}
    (and (contains? word :driven-by) (some? (:driven-by word)) (not (driven-by? (:driven-by word))))
    {:refuse :driven-by-has-no-form :at :driven-by}))

(defn- check*
  "The body of `check`, with no guard around it. It can throw; `check`
  turns each throw into `:not-a-word`."
  [thing]
  (if-not (plain-map? thing)
    {:refuse :not-a-word :at nil}
    (let [word (get thing :word)
          entry (get words word)]
      (if-not entry
        {:refuse :word-not-known :at :word}
        (let [carries (:carries entry)
              bare (if carries (dissoc thing carries) thing)
              [verdict got] (act/as-shape shapes word bare)]
          (if (= :no verdict)
            {:refuse (case got
                       :unknown-part :part-not-known
                       (nil :malformed) :part-has-no-form
                       got)
             :at (part-at-fault word bare)}
            (or (own-fault got)
                (case carries
                  nil nil
                  :acts (acts-fault (get thing :acts))
                  (carried-fault carries (get thing carries)))
                {:ok (cond-> got
                       carries (assoc carries (get thing carries))
                       (and (contains? got :wait-ms) (nil? (:wait-ms got)))
                       (assoc :wait-ms (if (= :answer word) 0 default-wait-ms))
                       (some? (:wait-ms got)) (update :wait-ms min longest-wait-ms))})))))))

(defn check
  "Gives {:ok word}, with the word in its one form, or
  {:refuse why :at part}, with one of `reasons` and the part at which the
  check stopped. It never throws.

  In order: the word's name; its shape; the parts whose form a shape cannot
  say; then the part that carries a form of the store's.

  The one form. Each part of the word's shape is there, nil when it was
  left out. A wait that was left out is `default-wait-ms` for a send and 0
  for `:answer`; a wait is cut to `longest-wait-ms`. The part that carries
  a form of the store's is as it arrived: the check reads nothing inside
  it.

  Limits. It does not judge an act or a query: the store does. It does not
  hold a who against a caller: the seam does. A thing that has no end does
  not come back from it; the line bounds a frame before it is read."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :not-a-word :at nil})))

;; ------------------------------------------- what the runtime could not do

(def could-nots
  "The runtime's own words: what it says in place of a result when it could
  not do a thing. Each reads after \"the runtime could not\". A word of the
  door or of the read path passes through the runtime as it is; none of
  these is one of them, and none is a judgment of an act: a gate's refusal
  is in the act's answer. `:status` is what a face over HTTP gives the
  word."
  {:read-the-frame
   {:says "the text held no one EDN form within the bounds; `:why` is `:too-large`, `:a-token-too-long`, `:not-edn`, `:empty` or `:more-than-one-form`"
    :status 400}
   :read-the-word
   {:says "the thing is no word of the form, or its number is that of a live watch; `:why` is the check's reason and `:at` the part, or, from the line, `:no-number` or `:number-in-use` at `:id`"
    :status 400}
   :find-her-sign-in
   {:says "the word needs a who, or the credential stands for nobody now; `:why` is `:no-credential`, `:not-a-credential`, `:not-known`, `:ended` or `:over`"
    :status 401}
   :find-her-session
   {:says "the session that was named is not open under this sign-in; `:why` is `:not-known`, `:closed` or `:not-hers`"
    :status 401}
   :send-as-another
   {:says "the act or the query names another who, session or asker than the caller's; `:at` is the part. Nobody acts for another"
    :status 403}
   :do-that-as-an-agent
   {:says "the word is a person's own, and the caller is an agent"
    :status 403}
   :find-the-agent
   {:says "the agent that was named is not beneath the caller; `:why` is `:not-known` or `:not-beneath`"
    :status 404}
   :find-the-sign-in
   {:says "the sign-in that was named is not one of the caller's root"
    :status 404}
   :find-the-watch
   {:says "this connection holds no watch under that number"
    :status 404}
   :find-the-ask
   {:says "no sign-in is under way under that code and secret"
    :status 404}
   :take-that
   {:says "Auth refused the thing; `:why` is Auth's word"
    :status 409}
   :take-more
   {:says "a limit was reached, and the same thing can be done later; `:why` is `:sign-in-paced`, `:polls-paced`, `:sessions-paced`, `:too-many-watches`, `:too-many-watches-on-a-connection` or `:too-many-waiting`, and `:most` is the limit"
    :status 429}
   :reach-auth
   {:says "a call of Auth threw, or Auth's watch was lost; a read that failed has not said that there is no sign-in"
    :status 503}
   :do-the-word
   {:says "the word's work threw in the runtime; nothing is said of what was done, and the system reads the record"
    :status 500}})

(defn could-not
  "`carries` is a map of what the word carries, or nil. Nil for a word that
  `could-nots` does not hold."
  [the-word carries]
  (when (contains? could-nots the-word)
    (assoc (or carries {}) :could-not the-word)))
