(ns softland.wire.watching
  "What a live read tells a reader who is slower than the changes, and what
  an answer is after a change. Pure code. It needs no cluster.

  The read path tells a live asker one map at a time
  (`softland.store.read-path`): `{:anew answer}`, the whole answer in the
  place of everything before; `{:changed {place place-answer}}`, the places
  that a change names, each with what the read gives there now; and
  `{:ended word}`, once, at the end.

  A reader that is slower than the changes cannot be given \"the newest,
  and none between\": a change that is dropped leaves its place wrong on
  her side for good. And nothing may wait without a bound. So what waits
  for a slow reader is merged by place: the newest change of each place is
  kept, and a whole answer anew takes the place of all that waits. That is
  right because a place never comes with an answer that is older than one
  that came before (the read path's own promise), so of two changes of one
  place the later one is the present. What waits is then never more than
  one whole answer, one change for each place, and one end.

  A thing that waits has the form of the messages themselves: a map that
  can hold `:anew`, `:changed` and `:ended`, any of the three. So `merged`
  takes two messages and gives a thing of the same form, and it is the
  function that Missionary's `relieve` needs.

  Inputs and results, by call:

    merged        what waits, and the next message -> what waits now
    messages      what waits -> the messages to hand on, in order
    through       a test -> a transducer that ends after the first thing
                  that passes the test
    answer-after  an answer and a message -> the answer after the message

  State and effects. None. Nothing here keeps an answer: `answer-after` is
  for a face whose reader wants the answer as one value over time, a
  screen, and that face holds the value.

  Callers. `softland.runtime.seam`, for `merged`, `messages` and `through`;
  the faces, for `answer-after`.")

(defn merged
  "Takes what waits for a reader and the message that came after it; both
  are maps that can hold `:anew`, `:changed` and `:ended`. Gives what waits
  now.

  Nothing comes after an end: what waits with an end stays as it is. A
  whole answer anew takes the place of the answer and the changes that
  wait. Else the changes are put together, and of two changes of one place
  the later one stays."
  [waits message]
  (cond
    (contains? waits :ended) waits
    (contains? message :anew) message
    :else (cond-> waits
            (contains? message :changed) (update :changed merge (:changed message))
            (contains? message :ended) (assoc :ended (:ended message)))))

(defn messages
  "Takes what waits for a reader. Gives the messages to hand her, in order:
  the whole answer when one waits, then the changed places when any wait,
  then the end when the read ended."
  [waits]
  (cond-> []
    (contains? waits :anew) (conj {:anew (:anew waits)})
    (seq (:changed waits)) (conj {:changed (:changed waits)})
    (contains? waits :ended) (conj {:ended (:ended waits)})))

(defn through
  "Takes a test of one argument. Gives a transducer that hands each thing
  on and ends after the first thing for which the test holds: that thing is
  the last one handed on."
  [last?]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result x]
       (let [result (rf result x)]
         (if (last? x) (ensure-reduced result) result))))))

(defn answer-after
  "Takes an answer of the read path, or nil, and one message of a live
  read. Gives the answer after the message.

  A whole answer anew is the answer. A change of a read of one place puts
  its place-answer under `:place`. A change of a read of many places puts
  each place-answer under its place in `:places`, and takes a place out
  whose place-answer says `:no-fact`: the whole answer holds a place only
  where a lane that she may see holds a fact
  (`softland.store.read-path.answer`). An end changes nothing."
  [answer message]
  (cond
    (contains? message :anew) (:anew message)
    (and answer (contains? message :changed))
    (if (contains? answer :place)
      (reduce (fn [answer [_ place-answer]] (assoc answer :place place-answer))
              answer
              (:changed message))
      (reduce (fn [answer [place place-answer]]
                (if (= :no-fact (:holds place-answer))
                  (update answer :places dissoc place)
                  (update answer :places assoc place place-answer)))
              answer
              (:changed message)))
    :else answer))
