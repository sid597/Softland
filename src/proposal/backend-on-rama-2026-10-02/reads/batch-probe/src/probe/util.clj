(ns probe.util
  "What every run of the batch probe shares: one log with one clock, waits
  that end, and the watchers that look at a row from outside.

  The code of a topology and the code that watches it run in one JVM here,
  since the cluster is Rama's in-process one. So a line that a topology says
  and a line that a watcher says go to one log, in the order of one clock.
  That is the whole reason for this namespace. Nothing here is a form for the
  store."
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.util.concurrent.locks LockSupport]))

(defonce ^{:doc "The nanosecond at which this JVM loaded the probe. Every time in a log is counted from it."}
  start
  (System/nanoTime))

(defonce ^{:doc "The open writer of the run's log file, or nil before `open-log!`."}
  log-writer
  (atom nil))

(defn ms
  "Gives the milliseconds since `start`, as a double."
  []
  (/ (double (- (System/nanoTime) start)) 1e6))

(defn open-log!
  "Takes a path. Opens it as the run's log, replacing what was there. Each
  line that `say` gets from then on is written to it."
  [path]
  (io/make-parents path)
  (reset! log-writer (io/writer path)))

(defn say
  "Takes parts of a line. Writes them as one line with the time since `start`
  in front: to the run's log, and to stdout behind the word PROBE, so that
  the line can be told from Rama's own lines. May be called from any thread,
  a task's thread too."
  [& parts]
  (let [line (str (format "%10.1f  " (ms)) (str/join " " parts))]
    (locking log-writer
      (println (str "PROBE " line))
      (when-let [^java.io.Writer w @log-writer]
        (.write w line)
        (.write w "\n")
        (.flush w)))))

(defn root-message
  "Takes a throwable. Gives the class and the first line of the message of
  its innermost cause, cut to 240 characters."
  [^Throwable t]
  (let [^Throwable r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))
        m (-> (or (.getMessage r) "") (str/split #"\n") first)]
    (str (.getSimpleName (class r)) ": " (subs m 0 (min 240 (count m))))))

(defn wait-until
  "Takes a function of no arguments and a limit in milliseconds. Calls the
  function every millisecond until it gives a true value or the limit has
  passed. Gives that value, or nil at the limit. It never waits longer."
  [f limit-ms]
  (let [end (+ (System/nanoTime) (* (long limit-ms) 1000000))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/nanoTime) end)
            (LockSupport/parkNanos 1000000)
            (recur))))))

(defn within
  "Takes a limit in milliseconds and a function of no arguments. Runs the
  function on another thread. Gives its value, or :timed-out when the limit
  passed first, or [:threw message] when it threw. The other thread is left
  to end by itself."
  [limit-ms f]
  (let [fut (future (try (f) (catch Throwable t [:threw (root-message t)])))]
    (deref fut limit-ms :timed-out)))

(defn watch
  "Starts a thread that reads a value again and again and says each change.
  Takes a label for the lines, a function of no arguments that reads, an
  atom that holds true when the thread must end, an atom that holds a vector
  to which each change is added as {:t ms :label :v value}, and the pause
  between two reads in nanoseconds. A read that throws counts as the value
  [:read-threw message]. Gives the thread."
  [label read-fn stop seen pause-nanos]
  (doto (Thread.
         (fn []
           (loop [last-value ::nothing-read-yet]
             (when-not @stop
               (let [v (try (read-fn) (catch Throwable t [:read-threw (root-message t)]))]
                 (when (not= v last-value)
                   (swap! seen conj {:t (ms) :label label :v v})
                   (say label "now gives" (pr-str v)))
                 (LockSupport/parkNanos (long pause-nanos))
                 (recur v))))))
    (.setDaemon true)
    (.setName (str "watch " label))
    (.start)))
