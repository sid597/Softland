(ns softland.wire.watching
  "What a live read tells a reader who is slower than the changes, and what
  an answer is after a change. Pure code.

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

  Nothing here keeps an answer: `answer-after` is for a face whose reader
  wants the answer as one value over time, a screen, and that face holds
  the value.")

(defn merged
  "Nothing comes after an end: what waits with an end stays as it is."
  [waits message]
  (cond
    (contains? waits :ended) waits
    (contains? message :anew) message
    :else (cond-> waits
            (contains? message :changed) (update :changed merge (:changed message))
            (contains? message :ended) (assoc :ended (:ended message)))))

(defn messages
  [waits]
  (cond-> []
    (contains? waits :anew) (conj {:anew (:anew waits)})
    (seq (:changed waits)) (conj {:changed (:changed waits)})
    (contains? waits :ended) (conj {:ended (:ended waits)})))

(defn through
  "A transducer that ends after the first thing for which the test holds:
  that thing is the last one handed on."
  [last?]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result x]
       (let [result (rf result x)]
         (if (last? x) (ensure-reduced result) result))))))

(defn answer-after
  "`answer` is an answer of the read path, or nil. A change of a read of
  many places takes a place out whose place-answer says `:no-fact-given`:
  the whole answer holds a place only where the read gives a fact
  (`softland.store.read-path.answer/no-fact-given`)."
  [answer message]
  (cond
    (contains? message :anew) (:anew message)
    (and answer (contains? message :changed))
    (if (contains? answer :place)
      (reduce (fn [answer [_ place-answer]] (assoc answer :place place-answer))
              answer
              (:changed message))
      (reduce (fn [answer [place place-answer]]
                (if (= :no-fact-given (:holds place-answer))
                  (update answer :places dissoc place)
                  (update answer :places assoc place place-answer)))
              answer
              (:changed message)))
    :else answer))
