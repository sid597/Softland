(ns softland.store.door.words
  "Everything that the door says to a maker, declared as data: the door's own
  fourteen words, the four answers, and the two acks. Pure code. It needs no
  cluster, and it requires nothing outside Clojure.

  A call of the door gives its result, or one of the door's own words. A word
  says what the door could not do. It is no judgment of the act: a gate
  judges an act, and a gate's refusal is in the record. A word is kept in no
  record.

  A word, as a maker gets it, is a map:

    :could-not  the word, one of the fourteen of `words`. Each reads after
                \"the door could not\"
    :appended   :nothing or :not-known, said of the maker's own act: nothing
                was appended for it, or the door does not know whether the
                depot holds it
    and what the word carries, each under its own name (`carried`)

  No other result of the door holds `:could-not`. A word holds no `:answer`
  and no `:reason`, so it can never be read as a gate's refusal. The one
  gate's word that a word can carry is inside `:request-answer`, and it is
  the answer of the door's own request for locks, never of the maker's act.

  Inputs and results, by call:

    word    one of the fourteen, and a map of what it carries -> the word as
            a maker gets it, or nil for a word that `words` does not hold
    answer  what the read by act-id gave, and the digest that the maker gave
            -> one of the four answers

  `acks` is a map from a depot word to the word of its ack.

  State and effects. None. Each function is pure.

  Callers. `softland.store.door` and `softland.store.door.additions`.

  Not here. A gate's reason words: `softland.store.act/reasons` and
  `softland.store.door.declaration/reasons`. The words of a stray:
  `softland.store.door.declaration/stray-reasons`. No word of this
  namespace is one of them, and the pure proof holds that.")

(def words
  "The door's own fourteen words. Each has the calls that can give it, what
  it says, and its `:appended`.

  Why fourteen. One word names one thing that the door could not do, and a
  word about a request for locks says what became of the request, since a
  request is an act in a depot and in the record whatever becomes of the
  maker's act. With fewer words a maker would have to read a second word
  inside the first to learn what to do next. Where in the maker's thing the
  door stopped is no word of its own: it is carried as data, under `:at`."
  {:find-the-module
   {:from     #{:open}
    :says     "the cluster gave no depot or no query of the module under that name; no door is opened"
    :appended :nothing}

   :make-an-act-id
   {:from     #{:act-id-for :act-ids-for :send-act}
    :says     "no act-id can be made for this layer, or the act-id given is not one of this layer, or the count of a series is no fixed-width integer of 1 or more"
    :appended :nothing}

   :read-the-layers-depot
   {:from     #{:act-id-for :act-ids-for :send-act}
    :says     "the read of the layer's depot threw; a read that failed has not said that the layer is not made"
    :appended :nothing}

   :read-the-act
   {:from     #{:send-act}
    :says     "the thing is no plain map, or it threw while the door read it"
    :appended :nothing}

   :add-its-parts
   {:from     #{:send-act}
    :says     "the thing already holds a part that the door adds: :act-id or :depot on the act, :sealed or :lock-id on a fact with a value to seal; the door writes over nothing"
    :appended :nothing}

   :seal-a-value
   {:from     #{:send-act}
    :says     "a value cannot be sealed so that it reads back: it is no value of the form, it is in a fact that is a map and no plain map, its sealed bytes would be over the limit, or the seal gave nothing"
    :appended :nothing}

   :seal-that-many-values
   {:from     #{:send-act}
    :says     "the act holds more values to seal than one act can hold facts; no lock is taken"
    :appended :nothing}

   :have-its-request-accepted
   {:from     #{:send-act}
    :says     "the door asked for locks under the act's who and permission, and the record's answer to that request is no accept; the request is on its depot and its answer is in the record; nothing is recorded under the act's own act-id"
    :appended :nothing}

   :append-its-request-cleanly
   {:from     #{:send-act}
    :says     "the append of the door's request for locks threw; the door does not know whether the depot holds the request, and remembers it as in doubt"
    :appended :nothing}

   :get-locks-within-its-wait
   {:from     #{:send-act}
    :says     "the door's wait for locks passed: its request, or one that an earlier call left, has no answer yet; or another call was still getting locks for this stock; or the wait ended after the door had read an earlier call's request to its end and before it could send its own. The request that the word names stays open and is read at the next need; a word that names none leaves none of this call open"
    :appended :nothing}

   :read-its-requests-answer
   {:from     #{:send-act}
    :says     "the read of the answer of the door's request for locks threw; the request stays open"
    :appended :nothing}

   :read-its-requests-locks
   {:from     #{:send-act}
    :says     "the request was accepted, and the read of its locks threw, or gave fewer locks that can seal than the act needs. After a read that threw, which the word then names by its class, the request stays open and the next call reads its answer again and then its locks; after a read that came back, the request is ended and what it gave is in the stock"
    :appended :nothing}

   :append-cleanly
   {:from     #{:send-act}
    :says     "the append of the act threw; the door does not know whether the depot holds the thing; the maker reads the record with the act-id and the digest that the word carries"
    :appended :not-known}

   :read-the-answer
   {:from     #{:answer-of}
    :says     "the read by act-id threw; none of the four answers is true"
    :appended :nothing}})

(def carried
  "What a word can carry, by name, with what each is. A word carries no lock,
  no run of bytes, no thing that was sent, and nothing that the door read of
  the record but the one answer of a request for locks."
  {:at             "the place in the maker's thing where the door stopped, by the thing's own part names: a part of the act, [:facts i], [:facts i part], or :count for the count of a series"
   :module         "the name of the module that the door was to be opened on"
   :layer          "the layer whose depot the door could not read"
   :most           "the greatest number that the door takes: of values to seal in one act, or of bytes in one sealed value"
   :sealed-bytes   "how many bytes the sealed value would have"
   :request        "the act-id of the door's request for locks that the word is about"
   :request-answer "the answer of that request, as `answer-of` gives one, with the gate's reason word"
   :waited-ms      "how long the call waited for locks, in milliseconds"
   :act-id         "the act-id of the maker's act, or of the read"
   :digest         "the digest of the thing whose append threw, as hex text"
   :threw          "the name of the class of what Rama's client threw"
   :said           "the message of what Rama's client threw"})

(defn word
  "Makes one of the door's own words as a maker gets it. Takes the word, one
  of the fourteen, and a map of what it carries, or nil. Gives the map of
  what is carried with `:could-not` and the word's `:appended` in it. An
  `:answer` or a `:reason` among what is carried is left out, and
  `:could-not` and `:appended` cannot be given: a word is never one of the
  four answers. Gives nil for a word that `words` does not hold, so that no
  word is made that is not declared."
  [the-word carries]
  (when-let [entry (get words the-word)]
    (assoc (dissoc (or carries {}) :answer :reason)
           :could-not the-word
           :appended (:appended entry))))

(def answers
  "The four answers of the door's `answer-of`, each with what it says.
  \"Nothing yet\" and \"not mine\" are the contract's phrases as keywords."
  {:accepted    "the record holds an answer under the act-id whose digest is the digest given, and a gate accepted the act; it carries the stamp of the decision"
   :refused     "the record holds an answer under the act-id whose digest is the digest given, and a gate refused the act; it carries the gate's reason word and the stamp"
   :nothing-yet "the read gave nothing; it does not mean refused, and it does not mean that no depot holds a thing"
   :not-mine    "the record holds an answer under the act-id whose digest is not the digest given; it says nothing more"})

(defn answer
  "The answer that the door gives from one read by act-id. Takes what the
  read gave, nil or {:verdict :reason :stamp :digest}, and the digest that
  the maker gave, the hex text that the door gave it when it sent. Gives:

    {:answer :nothing-yet}                     the read gave nil
    {:answer :not-mine}                        an answer with another digest
    {:answer :accepted :stamp s}               the maker's own act, accepted
    {:answer :refused :reason word :stamp s}   the maker's own act, refused

  It compares two hex texts and takes no digest of anything. A digest that
  is nil, or is no text, is the digest of no thing: \"not mine\" when an
  answer is there, \"nothing yet\" when none is. The stamp is part of the
  record's answer and no fact; a maker needs it to write `:based-on`."
  [read digest]
  (cond
    (nil? read)
    {:answer :nothing-yet}

    (not (and (string? digest) (= digest (:digest read))))
    {:answer :not-mine}

    (= :accepted (:verdict read))
    {:answer :accepted :stamp (:stamp read)}

    :else
    {:answer :refused :reason (:reason read) :stamp (:stamp read)}))

(def acks
  "The word of the ack of an append, by the depot word of the depot that
  took it. The two acks say different things to a maker, so each has its own
  word, and neither is ever the answer: a maker reads the answer by act-id.

    :gate-ran     the streaming depot's ack, at the ack level :ack: the
                  streaming gate has processed the thing and its rows are
                  visible
    :depot-holds  the microbatch depot's ack, at the ack level :append-ack:
                  the depot holds the thing. It says nothing about the
                  decision"
  {:streaming  :gate-ran
   :microbatch :depot-holds})
