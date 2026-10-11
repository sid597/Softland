(ns probe.say
  "What the two runs of this folder share: one log with one clock, and a
  wait that ends. The code of a topology and the code that looks at it run
  in one JVM, since the cluster is Rama's in-process one, so their lines go
  to one log in the order of one clock. Nothing here is a form for the
  store."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defonce ^{:doc "The nanosecond at which this JVM loaded the probe."}
  start (System/nanoTime))

(defonce ^{:doc "The open writer of the run's log, or nil."}
  log-writer (atom nil))

(defn ms
  "Gives the milliseconds since `start`."
  []
  (/ (double (- (System/nanoTime) start)) 1e6))

(defn open-log!
  "Takes a path. Opens it as the run's log, replacing what was there."
  [path]
  (io/make-parents path)
  (reset! log-writer (io/writer path)))

(defn say
  "Takes the parts of a line. Writes them as one line, with the time since
  `start` in front, to the run's log and to stdout behind the word PROBE."
  [& parts]
  (let [line (str (format "%9.1f  " (ms)) (str/join " " parts))]
    (locking log-writer
      (println (str "PROBE " line))
      (when-let [^java.io.Writer w @log-writer]
        (.write w line) (.write w "\n") (.flush w)))))

(defn root-message
  "Takes a throwable. Gives the class and the first line of the message of
  its innermost cause, cut to 200 characters."
  [^Throwable t]
  (let [^Throwable r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))
        m (first (str/split (or (.getMessage r) "") #"\n"))]
    (str (.getSimpleName (class r)) ": " (subs m 0 (min 200 (count m))))))

(defn wait-until
  "Takes a function of no arguments and a limit in milliseconds. Calls the
  function every 5 ms until it gives a true value or the limit has passed.
  A call that throws counts as no value. Gives the value, or nil."
  [f limit-ms]
  (let [end (+ (System/currentTimeMillis) (long limit-ms))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/currentTimeMillis) end)
            (Thread/sleep 5)
            (recur))))))

(defn within
  "Takes a limit in milliseconds and a function of no arguments. Runs it on
  another thread. Gives its value, :timed-out, or [:threw message]."
  [limit-ms f]
  (deref (future (try (f) (catch Throwable t [:threw (root-message t)]))) limit-ms :timed-out))
