(ns softland.server.words
  "The words: everything that a system outside our build can say to the
  server, declared as data, with the check of that form; and everything that
  the server says in place of a result. Pure code. It needs no cluster.

  A system that we do not compile, a command-line program, an agent's
  harness, a converter, speaks a few words of data. A word is a plain map
  whose `:word` names it:

    {:word :who-am-i :wait-ms n}                    who the credential
                                                    stands for
    {:word :act-ids  :layer layer :count n}         n act-ids of a layer
    {:word :send     :act act :act-id act-id}       one act -> a receipt
    {:word :send-all :acts [[act-id act] ...]}      acts of one maker, in
                                                    order -> their receipts
    {:word :answer   :act-id act-id :digest text    the answer of an act
                     :wait-ms n}
    {:word :read     :query query}                  a read, once
    {:word :watch    :query query}                  a read, kept live: a
                                                    stream
    {:word :sign-out}                               ends the session of the
                                                    credential that says it

  There is no word for a feature. A new thing that a system can do is a new
  key in a layer, written with `:send` and read with `:read`. A word that
  does not exist yet is one entry of `words` and one case of the seam.

  The forms that a word carries are the store's, less the parts that the
  server writes. An act is the maker's act (`softland.store.act`) without
  `:who` and `:session`; a query is the read path's
  (`softland.store.read-path.query`) without `:asker` and `:not-signed-in`.
  The server adds them from the credential, so a system has no place in
  which to claim a name. The server is the maker's hand here, as the door
  is behind it: the door adds the act-id, the depot and the time to what a
  maker gives (src-store/README.md, 6 October 2026, entry 2), and the
  server adds who gives it. That a system sends the act without the two
  parts, and not the whole act for the server to compare, is a session's
  choice of 20 November 2026 (call 2 of
  src/proposal/server-2026-11-20/PROPOSAL.md), for Sid to turn.

  What the check reads, and what it leaves. It is made as the act's check
  and the query's are, of the same things: `shapes` holds one shape for
  each word, in the language of `softland.store.act/shapes`, and
  `softland.store.act/as-shape` reads it. That shape holds every part of a
  word but the one that carries a form of the store's: `:act`, `:acts` or
  `:query`. Of such a part the check tests only what the server's own
  additions need: that it is a plain map, and that it does not already hold
  a part that the server writes. The store judges the rest. An act whose
  form is wrong goes to the door and its depot, and a gate refuses it under
  its act-id (src-store/README.md, 12 October 2026, entry 7); a query is
  checked by the read path.

  Inputs and results, by call:

    check       anything -> {:ok word} in its one form, or
                {:refuse why :at part}
    could-not   one of the server's own words, and what it carries -> the
                word as a system gets it

  State and effects. None. `check` never throws.

  Callers. `softland.server.http`, which checks each thing that arrives
  before the seam is called, and the faces that tell a system what the
  server could not do."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]))

;; ------------------------------------------------------------- the numbers

(def max-frame-bytes
  "How many bytes one word of somebody who is signed in can be, as it
  arrives. A face tests it before it reads the frame. It is the store's
  bound on what one act can write into rows
  (`softland.store.door.declaration/max-act-bytes`) and one MiB for the
  form around the values: an act that a gate can take fits, and a thing
  that is larger can only be refused, so the server does not put it into a
  log that is never trimmed. A number of the build, kept and not decided.
  It is no bound on how much one sender puts into the log, which the
  store's page names as not built (src-store/README.md, 12 October 2026,
  entry 7): it is of one request."
  (+ declaration/max-act-bytes (* 1024 1024)))

(def max-visitor-frame-bytes
  "How many bytes one word can be when no credential came with it that
  stands for somebody. A visitor says `:who-am-i`, `:read` and `:watch`,
  which are small; she sends no act. So somebody with no account cannot
  make the server read a large frame. A number of the build, kept and not
  decided."
  65536)

(def max-act-ids
  "How many act-ids one word can ask for. The door has no bound of its own
  on the count. A number of the build, kept and not decided."
  4096)

(def max-acts-of-one-word
  "How many acts one `:send-all` can carry. A number of the build, kept and
  not decided."
  1024)

(def longest-wait-ms
  "The longest wait that a word can ask for, in milliseconds. A longer one
  is cut to it; a system that waits longer asks again. A number of the
  build, kept and not decided."
  60000)

;; ---------------------------------------------------- the declared words

(def server-parts
  "The parts that the server writes into a form of the store's, by the part
  of a word that carries the form. A thing that arrives holding one of them
  is turned away: the server writes over nothing."
  {:act   [:who :session]
   :acts  [:who :session]
   :query [:asker :not-signed-in]})

(def words
  "The words, each with what it says, the part that carries a form of the
  store's (nil for none), whether it needs a credential that stands for a
  person, and whether its result is one thing or a stream."
  {:who-am-i {:says "who the credential stands for; with a wait, the answer comes when the credential stands for somebody, or when the wait is over"
              :carries nil :needs-a-who false :gives :one}
   :act-ids  {:says "act-ids of one layer, ahead of sending"
              :carries nil :needs-a-who true :gives :one}
   :send     {:says "one act into its depot; the result is the door's receipt, {:act-id :digest :ack}"
              :carries :act :needs-a-who true :gives :one}
   :send-all {:says "acts of one maker, each under its act-id, sent in order; the result is a receipt for each, and ends at the first that the door could not send"
              :carries :acts :needs-a-who true :gives :one}
   :answer   {:says "the answer of an act, by its act-id and the digest of its receipt"
              :carries nil :needs-a-who true :gives :one}
   :read     {:says "a read, once"
              :carries :query :needs-a-who false :gives :one}
   :watch    {:says "a read, kept live: the whole answer, then each change by place, until the stream is left"
              :carries :query :needs-a-who false :gives :a-stream}
   :sign-out {:says "ends the session of the credential that says it, on every face and every copy of the server"
              :carries nil :needs-a-who true :gives :one}})

(def shapes
  "The words' map of shapes: each entry of `softland.store.act/shapes`, and
  one shape for each word, named by the word. A word's shape holds each of
  its parts but the one that carries a form of the store's (`words`,
  `:carries`), which the engine does not read. A part that is left out
  holds nil in the one form, or the default that the shape gives."
  (assoc act/shapes
         :who-am-i [:parts
                    [:word    [:enum :who-am-i]]
                    [:wait-ms [:int {:min 0}] {:optional true :default 0 :reason :wait-is-no-span}]]
         :act-ids  [:parts
                    [:word  [:enum :act-ids]]
                    [:layer :id {:absent :layer-not-named :reason :layer-not-named}]
                    [:count [:int {:min 1 :below (inc max-act-ids)}]
                     {:optional true :default 1 :reason :count-out-of-range}]]
         :send     [:parts
                    [:word   [:enum :send]]
                    [:act-id :act-id {:optional true :reason :act-id-has-no-form}]]
         :send-all [:parts
                    [:word [:enum :send-all]]]
         :answer   [:parts
                    [:word    [:enum :answer]]
                    [:act-id  :act-id {:absent :act-id-not-given :reason :act-id-has-no-form}]
                    [:digest  :value  {:absent :digest-not-given :reason :digest-is-no-text}]
                    [:wait-ms [:int {:min 0}] {:optional true :default 0 :reason :wait-is-no-span}]]
         :read     [:parts
                    [:word [:enum :read]]]
         :watch    [:parts
                    [:word [:enum :watch]]]
         :sign-out [:parts
                    [:word [:enum :sign-out]]]))

(def reasons
  "The words with which `check` turns a thing away. Each names what is
  wrong.

    :not-a-word          the thing is no plain map, or threw while read
    :word-not-known      its `:word` names none of `words`
    :part-not-known      it holds a part that its word does not have
    :part-has-no-form    a part that names no reason of its own did not fit
    :not-given           the part that carries the store's form is absent
    :not-a-plain-map     that part, or an act of `:acts`, is no plain map
    :holds-a-part-that-the-server-writes
                         an act holds `:who` or `:session`, or a query
                         `:asker` or `:not-signed-in`
    :acts-not-a-series   `:acts` is no vector of [act-id act] pairs, or it
                         is empty
    :too-many-acts       `:acts` holds more than `max-acts-of-one-word`
    :digest-is-no-text   the digest is no text

  and each word that a part of `shapes` names."
  (into #{:not-a-word :word-not-known :part-not-known :part-has-no-form :not-given
          :not-a-plain-map :holds-a-part-that-the-server-writes :acts-not-a-series
          :too-many-acts :digest-is-no-text}
        (for [word (keys words)
              [_ _ opts] (rest (get shapes word))
              reason (keep #(get opts %) [:reason :absent :empty])]
          reason)))

;; ------------------------------------------------------------------ the check

(defn- plain-map?
  "True for a map that is no record."
  [x]
  (and (map? x) (not (record? x))))

(defn- carried-fault
  "Takes the name of the part that carries a form of the store's, `:act` or
  `:query`, and the thing under it. Gives nil when the server can add its
  parts to it, or {:refuse why :at part}: the thing is absent, is no plain
  map, or already holds a part that the server writes."
  [part thing]
  (cond
    (nil? thing) {:refuse :not-given :at part}
    (not (plain-map? thing)) {:refuse :not-a-plain-map :at part}
    :else (when-let [held (some (fn [k] (when (contains? thing k) k)) (get server-parts part))]
            {:refuse :holds-a-part-that-the-server-writes :at held})))

(defn- acts-fault
  "Takes the thing under `:acts`. Gives nil for a vector of 1 to
  `max-acts-of-one-word` pairs, each an act-id and an act to which the
  server can add its parts; or {:refuse why :at part}. The count is taken
  with a bound, before any pair is read."
  [acts]
  (cond
    (nil? acts) {:refuse :not-given :at :acts}
    (not (vector? acts)) {:refuse :acts-not-a-series :at :acts}
    (empty? acts) {:refuse :acts-not-a-series :at :acts}
    (< max-acts-of-one-word (count acts)) {:refuse :too-many-acts :at :acts}
    :else (some (fn [pair]
                  (cond
                    (not (and (vector? pair) (= 2 (count pair)))) {:refuse :acts-not-a-series :at :acts}
                    (not (act/has-shape? :act-id (nth pair 0))) {:refuse :act-id-has-no-form :at :acts}
                    :else (carried-fault :act (nth pair 1))))
                acts)))

(defn- part-at-fault
  "Takes a word's name and a thing that the engine refused under that
  word's shape. Gives the part at which it stopped: the first part that the
  shape does not hold, or else the first declared part, in the declared
  order, that does not fit by itself."
  [word thing]
  (let [entries (rest (get shapes word))
        declared (into #{} (map first) entries)]
    (or (first (remove declared (keys thing)))
        (some (fn [[part :as entry]]
                (when (= :no (first (act/as-shape shapes [:parts entry] (select-keys thing [part]))))
                  part))
              entries))))

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
              [verdict got] (act/as-shape shapes word (if carries (dissoc thing carries) thing))]
          (cond
            (= :no verdict)
            {:refuse (case got
                       :unknown-part :part-not-known
                       (nil :malformed) :part-has-no-form
                       got)
             :at (part-at-fault word (if carries (dissoc thing carries) thing))}

            (and (= :answer word) (not (string? (:digest got))))
            {:refuse :digest-is-no-text :at :digest}

            :else
            (or (case carries
                  nil nil
                  :acts (acts-fault (get thing :acts))
                  (carried-fault carries (get thing carries)))
                {:ok (cond-> got
                       carries (assoc carries (get thing carries))
                       (contains? got :wait-ms) (update :wait-ms min longest-wait-ms))})))))))

(defn check
  "The check of a word. Takes anything. Gives {:ok word}, with the word in
  its one form, or {:refuse why :at part}, with one of `reasons` and the
  part at which the check stopped. It never throws.

  In order. The thing must be a plain map whose `:word` names a word. Then
  the word's shape: a part that the word does not have, or the first part
  that is wrong in the declared order, gives its reason. Then the part
  that carries a form of the store's: it must be there, a plain map, and
  hold none of the parts that the server writes.

  The one form. Each part of the word's shape is there, nil or its default
  when it was left out; a wait is cut to `longest-wait-ms`. The part that
  carries a form of the store's is as it arrived: the check reads nothing
  inside it but whether it holds a part of the server's.

  Limits. It does not judge an act or a query: the store does. A thing
  that has no end does not come back from it; a face bounds the frame
  before it reads one."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :not-a-word :at nil})))

;; ------------------------------------------- what the server could not do

(def could-nots
  "The server's own words: what it says in place of a result when it could
  not do a thing. Each reads after \"the server could not\". A word of the
  door or of the read path passes through the server as it is; none of
  these is one of them, and none is a judgment of an act: a gate's refusal
  is in the act's answer.

  Each has what it says and the status that the face over HTTP gives it."
  {:read-the-frame
   {:says "the request held no one EDN form within the bounds; `:why` is `:too-large`, `:a-token-too-long`, `:not-edn`, `:empty` or `:more-than-one-form`"
    :status 400}
   :read-the-word
   {:says "the thing is no word of the form; `:why` is the check's reason and `:at` the part"
    :status 400}
   :find-her-session
   {:says "the word needs a who, or the credential given stands for nobody now; `:why` is `:no-credential`, `:not-a-token`, `:not-known`, `:ended` or `:over`"
    :status 401}
   :reach-the-sessions
   {:says "the read of the credential's row threw; a read that failed has not said that there is no session"
    :status 503}
   :begin-her-session
   {:says "the session was not begun; `:why` is `:credential-in-use` when the hash already stands for another session, or what Rama threw"
    :status 409}})

(defn could-not
  "Makes one of the server's own words as a system gets it. Takes the word,
  one of `could-nots`, and a map of what it carries, or nil. Gives the map
  of what is carried with `:could-not` in it; nil for a word that
  `could-nots` does not hold."
  [the-word carries]
  (when (contains? could-nots the-word)
    (assoc (or carries {}) :could-not the-word)))
