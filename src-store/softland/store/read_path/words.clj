(ns softland.store.read-path.words
  "Everything that the read path says to a caller in place of an answer,
  declared as data. Pure code.

  A word says what the read path could not do. It is no judgment of a fact
  and no answer: a read that failed has never said \"no fact\". No answer
  holds `:could-not`, and no word holds a part of an answer. A word is kept
  in no record.

  The door has a word spelled `:find-the-module` for its own opening; the
  read path's is its own. The read path's pure proof holds that no other
  word here is a gate's reason word, a word of a stray or one of the door's.")

(def words
  "Each word reads after \"the read path could not\". `:keep-the-read-live`
  is given to an asker's function, under `:ended`, and by no call."
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
    :says "a run of a lane that the asker may see holds more places than one read may, or the read needs more checks of nearer lanes than one read may"}

   :keep-more-reads-live
   {:from #{:read-live}
    :says "the read path already holds as many pushes on one table under one routing id as it may, and this read needs one more there; the read opened none of them, and a read asked once is not held to that number"}

   :keep-the-read-live
   {:from #{:the-askers-function}
    :says "the live read ended; the function is not called again, and the caller asks again"}

   :find-the-line-whole
   {:from #{:read-once}
    :says "a read of a lane's acts found no row of the lane's line at a stamp where one must be: up to the lane's last stamp, at a stamp of the asker's own, or the thing that a row says is kept; the read gave no answer that ends there"}

   :keep-a-read-of-acts-live
   {:from #{:read-live}
    :says "a read of a lane's acts is asked once and is not kept live; the caller asks again from where the last read ended"}})

(def carried
  "What a word can carry. A word carries no row, no value and no place of a
  lane."
  {:module    "the name of the module that the read path was to be opened on"
   :why       "the word with which the check of a query turned the thing away, one of `softland.store.read-path.query/reasons`"
   :at        "the part of the query where the check stopped"
   :threw     "the name of the class of what Rama's client threw"
   :said      "the message of what Rama's client threw"
   :waited-ms "how long the call waited, in milliseconds"
   :what      "what was too many: `:places-of-one-run`, `:checks-of-one-read`, or `:pushes-under-one-routing-id`"
   :most      "the greatest number of them that one read may hold, or for the pushes that the read path may hold"
   :because   "why a live read ended, one of `because`"
   :stamp     "the first stamp of a lane's line that the read did not find whole"})

(def because
  "Why a live read ends. A caller gets one under `:because`, in the word
  `:keep-the-read-live`."
  {:the-store-ended-a-push
   "Rama ended a push that the read holds"
   :a-step-did-not-settle
   "a change made the open read want more, round after round, up to its bound on rounds; it ended rather than tell an answer of sources that it did not hold"
   :a-run-grew-too-large
   "a run of the read came to hold more places than one read may, or the read came to need more checks than one read may; the word carries `:what` and `:most`"
   :a-read-it-came-to-need-failed
   "a push that the open read came to need was not given: its opening threw, or its wait passed; the word carries what that carried"
   :no-room-for-a-push-it-came-to-need
   "the open read came to need a push where the read path already holds as many pushes as it may; none was opened, and the word carries `:what` and `:most`"
   :its-function-threw
   "the asker's function threw while it was given a change"})

(def left-out-words
  "Why a lane of a read gave nothing. An answer holds one under `:why`, for
  each lane that it left out."
  {:may-not-see "the lane is made, and the asker may not see it"
   :not-made    "the lane is not made"})

(defn word
  "Nil for a word that `words` does not hold, so that no word is made that
  is not declared. A part of an answer among what is carried is dropped,
  and `:could-not` cannot be given: a word is never an answer."
  [the-word carries]
  (when (contains? words the-word)
    (assoc (dissoc (or carries {}) :lanes :left-out :place :places :orders :acts :next :last-stamp)
           :could-not the-word)))
