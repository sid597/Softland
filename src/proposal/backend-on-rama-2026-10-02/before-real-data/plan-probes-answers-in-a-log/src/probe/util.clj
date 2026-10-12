(ns probe.util
  "What the two runs of this probe share: one log with one clock, and a wait
  that ends. The ways are those of the stream probe of 12 October
  (`../stream-probe/src/probe/util.clj`), written again so that this folder
  requires nothing outside itself. Nothing here is a form for the store."
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.util.concurrent.locks LockSupport]))

(defonce ^{:doc "The nanosecond at which this JVM loaded the probe."} start (System/nanoTime))

(defonce ^{:doc "The open writer of the run's log file, or nil."} log-writer (atom nil))

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
  "Takes parts of a line. Writes them as one line, with the time since
  `start` in front, to the run's log and to stdout behind the word PROBE."
  [& parts]
  (let [line (str (format "%10.1f  " (ms)) (str/join " " parts))]
    (locking log-writer
      (println (str "PROBE " line))
      (when-let [^java.io.Writer w @log-writer]
        (.write w line) (.write w "\n") (.flush w)))))

(defn root-message
  "Takes a throwable. Gives the class and the first line of the message of its innermost cause."
  [^Throwable t]
  (let [^Throwable r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))
        m (-> (or (.getMessage r) "") (str/split #"\n") first)]
    (str (.getSimpleName (class r)) ": " (subs m 0 (min 160 (count m))))))

(defn wait-until
  "Takes a function of no arguments and a limit in milliseconds. Calls it
  every 5 ms until it gives a true value or the limit has passed; a call that
  throws counts as nothing yet. Gives that value, or nil at the limit."
  [f limit-ms]
  (let [end (+ (System/nanoTime) (* (long limit-ms) 1000000))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/nanoTime) end)
            (LockSupport/parkNanos 5000000)
            (recur))))))

(defn stats
  "Takes numbers of milliseconds. Gives how many, the least, the middle and the most, as text."
  [xs]
  (if (empty? xs)
    "none"
    (let [s (vec (sort xs)) n (count s)]
      (format "count %d · least %.2f · middle %.2f · most %.2f ms"
              n (double (first s)) (double (s (quot n 2))) (double (last s))))))
