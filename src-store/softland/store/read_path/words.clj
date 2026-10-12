(ns softland.store.read-path.words
  "Everything that the read path says to a caller in place of an answer,
  declared as data: its own eight words, the five reasons for which a live
  read ends, and the two words for a layer that gave nothing. Pure code. It
  needs no cluster, and it requires nothing outside Clojure.

  A call of the read path gives its result, or one of the read path's own
  words. A word says what the read path could not do. It is no judgment of
  a fact and no answer: an answer says what the tables hold, and a word says
  that the read path does not know. A read that failed has never said \"no
  fact\". A word is kept in no record.

  A word, as a caller gets it, is a map:

    :could-not  the word, one of the eight of `words`. Each reads after
                \"the read path could not\"
    and what the word carries, each under its own name (`carried`)

  No answer holds `:could-not`, and no word holds a part of an answer.

  Inputs and results, by call:

    word  one of the eight, and a map of what it carries -> the word as a
          caller gets it, or nil for a word that `words` does not hold

  State and effects. None. `word` is pure.

  Callers. `softland.store.read-path`.

  Not here. The words with which the check of a query turns a thing away:
  `softland.store.read-path.query/reasons`; a word of this namespace,
  `:read-the-query`, carries one of them under `:why`. A gate's reason
  words, the words of a stray and the door's own words are other sets, and
  the read path's pure proof holds that no word of this namespace is one of
  them, but for `:find-the-module`: the door has a word of that spelling for
  its own opening, and the read path's is its own.")

(def words
  "The read path's own eight words. Each has the calls that can give it and
  what it says. `:keep-the-read-live` is given to an asker's function, under
  `:ended`, and by no call."
  {:find-the-module
   {:from #{:open}
    :says "the cluster gave no table of the module under that name; no read path is opened"}

   :read-the-query
   {:from #{:read-once :read-live}
    :says "the thing is no query of the form; no call was made to Rama"}

   :reach-the-store
   {:from #{:read-once :read-live}
    :says "a call of Rama's client threw; a read that failed has not said that there is no fact"}

   :get-an-answer-within-its-wait
   {:from #{:read-once :read-live}
    :says "the read path's wait, one bound for the whole call, passed before Rama gave every value, or the read did not settle within its rounds"}

   :read-inside-an-askers-function
   {:from #{:read-once :read-live}
    :says "the call was made on a thread on which the read path is calling an asker's function; no call was made to Rama"}

   :serve-a-read-that-large
   {:from #{:read-once :read-live}
    :says "a run of a layer that the asker may see holds more places than one read may, or the read needs more checks of nearer layers than one read may"}

   :keep-more-reads-live
   {:from #{:read-live}
    :says "the read path already holds as many pushes on one table under one routing id as it may, and this read needs one more there; the read opened none of them, and a read asked once is not held to that number"}

   :keep-the-read-live
   {:from #{:the-askers-function}
    :says "the live read ended; the function is not called again, and the caller asks again"}})

(def carried
  "What a word can carry, by name, with what each is. A word carries no row,
  no value and no place of a layer."
  {:module    "the name of the module that the read path was to be opened on"
   :why       "the word with which the check of a query turned the thing away, one of `softland.store.read-path.query/reasons`"
   :at        "the part of the query where the check stopped"
   :threw     "the name of the class of what Rama's client threw"
   :said      "the message of what Rama's client threw"
   :waited-ms "how long the call waited, in milliseconds"
   :what      "what was too many: `:places-of-one-run`, `:checks-of-one-read`, or `:pushes-under-one-routing-id`"
   :most      "the greatest number of them that one read may hold, or for the pushes that the read path may hold"
   :because   "why a live read ended, one of `because`"})

(def because
  "The five reasons for which a live read ends, each with what it says. A
  caller gets one under `:because`, in the word `:keep-the-read-live`."
  {:the-store-ended-a-push
   "Rama ended a push that the read holds"
   :a-run-grew-too-large
   "a run of the read came to hold more places than one read may, or the read came to need more checks than one read may; the word carries `:what` and `:most`"
   :a-read-it-came-to-need-failed
   "a push that the open read came to need was not given: its opening threw, or its wait passed; the word carries what that carried"
   :no-room-for-a-push-it-came-to-need
   "the open read came to need a push where the read path already holds as many pushes as it may; none was opened, and the word carries `:what` and `:most`"
   :its-function-threw
   "the asker's function threw while it was given a change"})

(def left-out-words
  "The two words for why a layer of a read gave nothing, each with what it
  says. An answer holds one under `:why`, for each layer that it left out
  (`softland.store.read-path.layers/left-out`)."
  {:may-not-see "the layer is made, and the asker may not see it"
   :not-made    "the layer is not made"})

(defn word
  "Makes one of the read path's own words as a caller gets it. Takes the
  word, one of the eight, and a map of what it carries, or nil. Gives the
  map of what is carried with `:could-not` in it. A part of an answer among
  what is carried is left out (`:layers`, `:left-out`, `:place`, `:places`,
  `:orders`), and `:could-not` cannot be given: a word is never an answer.
  Gives nil for a word that `words` does not hold, so that no word is made
  that is not declared."
  [the-word carries]
  (when (contains? words the-word)
    (assoc (dissoc (or carries {}) :layers :left-out :place :places :orders)
           :could-not the-word)))
