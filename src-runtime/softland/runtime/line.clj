(ns softland.runtime.line
  "The line: one connection that carries many words and many watches. It is
  the face for everything that speaks the words of the wire
  (`softland.wire.words`): the command line, an agent's harness, and the
  browser's client when it is built. It requires Missionary, the seam and
  the wire's forms. It knows no socket: whatever carries the connection
  hands it two functions, one that sends a text and one that closes
  (`softland.runtime.jetty` does, for a WebSocket).

  One connection is one caller, in one session. The caller is made before
  the connection opens, from the credential that came with the request
  (`softland.runtime.seam/caller`), and stays the caller for the
  connection's life. So a sign-in that ends has to end the connection: the
  line holds the seam's `standing` beside everything that is open, and when
  that says an end, it tells the system why, leaves every watch, and
  closes.

  What crosses, each as one EDN form in one text frame
  (`softland.wire.frame`):

    from the runtime, first        {:hello about}
    from the system                a word with a number: {:id n :word ...}
    from the runtime, for a word   {:id n :gives result}
    for a watch, under the number  {:id n :anew answer}
    of the word that opened it     {:id n :changed {place place-answer}}
                                   {:id n :ended word}
    from the runtime, last         {:bye word}

  The number is the system's own, an integer of 0 or more, and says which
  word a result is for. A watch keeps the number of its `:watch` until it
  ends or is left with `{:word :leave :watch n}`.

  One word at a time. The words of one connection are done in the order in
  which they arrived, one after another, on one thread of the connection's
  own: two acts that a maker sends one after the other reach their depot in
  that order. A send waits for its answer by default, and the next word
  waits behind it; a maker that wants to send on at once names
  `:wait-ms 0`, or sends a series with `:send-all`, and reads answers with
  `:answer`. A watch does not hold the line: its messages are written as
  they come, between the results of words.

  What is bounded. A frame's bytes, by what carries the connection, before
  the frame is in memory. A frame's longest token, before it is read as
  EDN (`softland.wire.frame`). The words that wait on one connection, at
  `max-waiting`: a system that sends faster than its words are done is
  closed. The watches of one connection, at `max-watches`.

  The calls:

    open      the seam, a caller, and the two functions -> a connection
    text!     a connection and one text frame -> nil, at once
    closed!   a connection -> nil: the carrier says that it is gone

  State. Nothing that lasts. A connection holds, in the memory of the
  process: its caller; the words that wait; its watches, each with the
  function that leaves it; and whether it is closed. All of it goes when
  the connection does."
  (:require [missionary.core :as m]
            [softland.runtime.seam :as seam]
            [softland.wire.frame :as frame]
            [softland.wire.words :as words])
  (:import [java.io Writer]
           [java.util.concurrent LinkedBlockingQueue]))

(def max-waiting
  "How many words can wait on one connection. A number of the build, kept
  and not decided."
  256)

(def max-watches
  "How many watches one connection can hold. The read path bounds its own
  pushes beside this (`softland.store.read-path`, \"How many pushes\"). A
  number of the build, kept and not decided."
  256)

(deftype Connection [the-seam caller send! close! queue state]
  Object
  (toString [_] "#<connection>"))

(defmethod print-method Connection [^Connection connection ^Writer w]
  (.write w (.toString connection)))

(alter-meta! #'->Connection assoc
             :private true
             :doc "Makes a connection: the seam and the caller, borrowed; `send!`, a
  function of a text, and `close!`, a function of a status and a reason,
  from what carries the connection; the queue of texts that wait; and
  `state`, an atom of {:closed? :watches {number leave} :standing leave}.")

(declare end!)

(defn- say!
  "Sends one form to the system. Takes a connection and a thing. One write
  at a time for a connection: a result and a watch's message never mix. A
  write that throws means that the system went away, and ends the connection."
  [^Connection connection thing]
  (let [failed?
        (locking connection
          (when-not (:closed? @(.-state connection))
            (try ((.-send! connection) (frame/write thing))
                 false
                 (catch Throwable _ true))))]
    (when failed?
      (end! connection 1006 "send failed"))))

(defn- end!
  "Ends a connection: leaves every watch, stops listening for the caller's
  standing, interrupts the connection's worker thread, and closes what carries
  it. Takes the connection, and a status and a reason for the carrier. It does
  its work once."
  [^Connection connection status reason]
  (let [[before _] (swap-vals! (.-state connection) assoc :closed? true :watches {} :standing nil)]
    (when-not (:closed? before)
      (doseq [{:keys [leave]} (vals (:watches before))]
        (try (when leave (leave)) (catch Throwable _ nil)))
      (when-let [leave (:standing before)]
        (try (leave) (catch Throwable _ nil)))
      (let [^LinkedBlockingQueue q (.-queue connection)]
        (.clear q)
        (.offer q ::closed))
      (when-let [^Thread worker (:worker before)]
        (when-not (identical? worker (Thread/currentThread))
          (.interrupt worker)))
      (try ((.-close! connection) status reason) (catch Throwable _ nil))))
  nil)

(defn- watch!
  "Opens a watch under the number of its word. Takes the connection, the
  number and the word. Each message of the seam's flow is sent with the
  number in it. When the flow ends, the number is free again."
  [^Connection connection id word]
  (let [state (.-state connection)]
    (cond
      (contains? (:watches @state) id)
      (say! connection {:id id :ended (words/could-not :find-the-watch {:why :number-in-use})})

      (<= max-watches (count (:watches @state)))
      (say! connection {:id id :ended (words/could-not :find-the-watch {:why :too-many :most max-watches})})

      :else
      (let [token (Object.)
            done (fn [_]
                   (swap! state (fn [s]
                                  (if (identical? token (get-in s [:watches id :token]))
                                    (update s :watches dissoc id)
                                    s))))
            ;; The flow can end before its cancel is kept, so the place is
            ;; taken first with a unique token and filled after.
            _ (swap! state assoc-in [:watches id] {:token token :leave (fn [] nil)})
            leave ((m/reduce (fn [_ message] (say! connection (assoc message :id id)) nil)
                             nil
                             (seam/watch (.-the-seam connection) (.-caller connection) (:query word)))
                   done done)]
        (swap! state (fn [s]
                       (if (identical? token (get-in s [:watches id :token]))
                         (assoc-in s [:watches id :leave] leave)
                         s)))
        (when (:closed? @state) (leave))))))

(defn- word!
  "Does one frame. Takes the connection and the frame's text."
  [^Connection connection text]
  (let [[outcome form] (frame/read-one text)]
    (if (= :no outcome)
      (say! connection (words/could-not :read-the-frame {:why form}))
      (let [id (when (and (map? form) (not (record? form))) (get form :id))]
        (if-not (and (integer? id) (<= 0 id))
          (say! connection (words/could-not :read-the-word {:why :no-number :at :id}))
          (let [{word :ok why :refuse at :at} (words/check (dissoc form :id))]
            (cond
              why
              (say! connection {:id id :gives (words/could-not :read-the-word {:why why :at at})})

              (= :watch (:word word))
              (watch! connection id word)

              (= :leave (:word word))
              (let [{:keys [leave]} (get (:watches @(.-state connection)) (:watch word))]
                (if leave
                  (do (swap! (.-state connection) update :watches dissoc (:watch word))
                      (leave)
                      (say! connection {:id id :gives {:left (:watch word)}}))
                  (say! connection {:id id :gives (words/could-not :find-the-watch {:why :not-known})})))

              :else
              (let [result (try (m/? (seam/call (.-the-seam connection) (.-caller connection) word))
                                (catch Throwable t
                                  (words/could-not :do-the-word {:threw (.getName (class t))})))]
                (say! connection {:id id :gives result})))))))))

(defn open
  "Opens a connection. Takes the seam; a caller
  (`softland.runtime.seam/caller`, or its `visitor`); and a map of the two
  functions of what carries the connection: `:send!`, which takes a text
  and writes it as one frame, and `:close!`, which takes a status and a
  reason. Gives the connection.

  It says hello, starts the one thread that does the connection's words in
  their order, and begins to hold the caller's standing: when that says an
  end, the system is told `{:bye word}` and the connection is ended."
  [the-seam caller {:keys [send! close!]}]
  (let [connection (->Connection the-seam caller send! close!
                                 (LinkedBlockingQueue. (int max-waiting))
                                 (atom {:closed? false :watches {} :standing nil}))]
    (say! connection {:hello (seam/about caller)})
    (let [worker (.start (Thread/ofVirtual)
                         (fn []
                           (try
                             (loop []
                               (let [text (.take ^LinkedBlockingQueue (.-queue connection))]
                                 (when-not (or (= ::closed text) (:closed? @(.-state connection)))
                                   (try (word! connection text) (catch Throwable _ nil))
                                   (recur))))
                             (catch InterruptedException _ nil))))]
      (swap! (.-state connection) assoc :worker worker)
      (when (:closed? @(.-state connection)) (.interrupt worker)))
    (let [leave ((m/reduce (fn [_ word]
                             (say! connection {:bye word})
                             (end! connection 1008 "the sign-in or the session ended")
                             nil)
                           nil
                           (seam/standing the-seam caller))
                 (fn [_] nil) (fn [_] nil))]
      (swap! (.-state connection) assoc :standing leave)
      (when (:closed? @(.-state connection)) (leave)))
    connection))

(defn text!
  "Takes a connection and the text of one frame that arrived. It puts the
  text with the words that wait and returns at once: the thread of what
  carries the connection does none of the word's work. A connection on
  which `max-waiting` words wait already is ended."
  [^Connection connection text]
  (when-not (.offer ^LinkedBlockingQueue (.-queue connection) text)
    (say! connection {:bye (words/could-not :read-the-frame {:why :too-many-waiting :most max-waiting})})
    (end! connection 1008 "too many words wait"))
  nil)

(defn closed!
  "Takes a connection whose carrier says that it is gone. Leaves every
  watch and stops the connection's thread."
  [connection]
  (end! connection 1000 "closed"))

(defn open?
  "Takes a connection. Gives true while it is not ended."
  [^Connection connection]
  (not (:closed? @(.-state connection))))
