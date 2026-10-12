;; A probe of the review of the whole, for ruling 86: the String that a fact's
;; row holds for its value (`softland.store.record.rows/held-value`).
;;
;; Two questions, both on Rama's in-process cluster:
;;   1. Is a row whose String holds U+0000 written and read back the same, by
;;      a stream topology and by a microbatch topology?
;;   2. Does a push on one such row, and on a run of such rows, get one change
;;      for each write? Rows that hold a byte array are run beside them.
;;
;; A probe: its module is no code of the store, and no form or name comes from
;; it. It takes one thing from the store, the pair of functions under test.
;; Run from the repository root:
;;   clojure -M:store-module -i src/proposal/backend-on-rama-2026-10-02/reads/runs/review-probe-held-value.clj
(ns probe.held-value
  (:use [com.rpl.rama] [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [softland.store.record.rows :as rows])
  (:import [java.util Arrays Random]))

(def bytes-class (Class/forName "[B"))

(defn text-row [held stamp] {:value held :stamp stamp})
(defn bytes-row [held stamp] {:value (rows/plain-bytes held) :stamp stamp})
(defn seen-same
  "What a topology says of the row that it reads before it writes: no row yet,
  or whether the row's String is the String that now arrived."
  [before held]
  (cond (nil? before) "first" (= (:value before) held) "same" :else "differs"))

(defmodule HeldValueProbe [setup topologies]
  (declare-depot setup *stream-writes (hash-by :route))
  (declare-depot setup *batch-writes (hash-by :route))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$s-rows {String (fixed-keys-schema {:value String :stamp Long})})
    (declare-pstate s $$s-bytes {String (fixed-keys-schema {:value bytes-class :stamp Long})})
    (declare-pstate s $$s-seen {String String})
    (<<sources s
      (source> *stream-writes :> {:keys [*k *held *stamp]})
      (local-select> (keypath *k) $$s-rows :> *before)
      (seen-same *before *held :> *same)
      (local-transform> [(keypath *k) (termval *same)] $$s-seen)
      (text-row *held *stamp :> *row)
      (bytes-row *held *stamp :> *row-of-bytes)
      (local-transform> [(keypath *k) (termval *row)] $$s-rows)
      (local-transform> [(keypath *k) (termval *row-of-bytes)] $$s-bytes)))
  (let [m (microbatch-topology topologies "m")]
    (declare-pstate m $$m-rows {String (fixed-keys-schema {:value String :stamp Long})})
    (declare-pstate m $$m-seen {String String})
    (<<sources m
      (source> *batch-writes :> %microbatch)
      (%microbatch :> {:keys [*k *held *stamp]})
      (local-select> (keypath *k) $$m-rows :> *before)
      (seen-same *before *held :> *same)
      (local-transform> [(keypath *k) (termval *same)] $$m-seen)
      (text-row *held *stamp :> *row)
      (local-transform> [(keypath *k) (termval *row)] $$m-rows))))

(defn say [& xs] (println (apply print-str "PROBE" xs)) (flush))

(defn bytes-of [& xs] (byte-array (map unchecked-byte xs)))

(def values
  "Each [name bytes]. The first seven take the marked form; the last three are
  text and take the text itself."
  [["all 256 bytes, 0x00 to 0xFF" (byte-array (map unchecked-byte (range 256)))]
   ["U+0000 and then text" (.getBytes "\u0000\"text after a zero\"" "UTF-8")]
   ["one zero byte" (bytes-of 0)]
   ["a lead byte with no tail, C3 28" (bytes-of 0xC3 0x28)]
   ["a surrogate in UTF-8, ED A0 80" (bytes-of 0xED 0xA0 0x80)]
   ["an overlong slash, C0 AF" (bytes-of 0xC0 0xAF)]
   ["65,508 random bytes" (let [b (byte-array 65508)] (.nextBytes (Random. 86) b) b)]
   ["text of one, two, three and four bytes a letter" (.getBytes "\"a é ✓ 𝄞 \\u0000 inside\"" "UTF-8")]
   ["text with U+0000 inside and not first" (.getBytes "\"a\u0000b\"" "UTF-8")]
   ["no bytes" (byte-array 0)]])

(with-open [ipc (rtest/create-ipc)]
  (rtest/launch-module! ipc HeldValueProbe {:tasks 4 :threads 2})
  (let [m (get-module-name HeldValueProbe)
        stream-depot (foreign-depot ipc m "*stream-writes")
        batch-depot (foreign-depot ipc m "*batch-writes")
        table (fn [n] (foreign-pstate ipc m n))
        batches (atom 0)
        stream! (fn [k held stamp] (foreign-append! stream-depot {:route "L" :k k :held held :stamp stamp} :ack))
        batch! (fn [k held stamp]
                 (foreign-append! batch-depot {:route "L" :k k :held held :stamp stamp} :ack)
                 (rtest/wait-for-microbatch-processed-count ipc m "m" (swap! batches inc)))
        one (fn [n k] (foreign-select-one (keypath k) (table n) {:pkey "L"}))]

    ;; ------------------------------------------------ 1. written and read back
    (doseq [[i [what plain]] (map-indexed vector values)]
      (let [held (rows/held-value plain)
            k (str "L v " i)]
        (dotimes [turn 2] (stream! k held turn) (batch! k held turn))
        (let [s-row (one "$$s-rows" k)
              m-row (one "$$m-rows" k)
              back? (fn [row] (and (= held (:value row))
                                   (Arrays/equals ^bytes plain ^bytes (rows/plain-bytes (:value row)))))]
          (say "form:" what
               "| bytes" (alength ^bytes plain)
               "| held as" (if (.startsWith ^String held "\u0000") "U+0000 and a character for each byte" "the text itself")
               "| characters" (.length ^String held)
               "| the pair gives the bytes back" (Arrays/equals ^bytes plain ^bytes (rows/plain-bytes held))
               "| stream: read back the same" (back? s-row) ", the topology's own read of the row before" (one "$$s-seen" k)
               "| microbatch: read back the same" (back? m-row) ", the topology's own read of the row before" (one "$$m-seen" k)))))

    ;; --------------------------------------------------------- 2. the pushes
    (let [not-text (fn [i] (rows/held-value (bytes-of 0xFF 0xFE (mod i 256) (quot i 256))))
          seen (atom [])
          cb (fn [which] (fn [new diff old] (swap! seen conj [which (.getSimpleName (class diff))])))
          tally (fn [] (into (sorted-map) (map (fn [[which calls]] [which (frequencies (map second calls))]))
                             (group-by first @seen)))]
      (doseq [i (range 5)] (stream! (str "L p k" i) (not-text i) i) (batch! (str "L p k" i) (not-text i) i))
      (let [pushes (vec (for [[which n] [["stream, rows with a U+0000 String" "$$s-rows"]
                                         ["stream, rows with a byte array" "$$s-bytes"]
                                         ["microbatch, rows with a U+0000 String" "$$m-rows"]]
                              [kind the-path] [["a run of five rows" (sorted-map-range "L p " "L p!")]
                                               ["one row" (keypath "L p k1")]]]
                          (foreign-proxy the-path (table n) {:pkey "L" :callback-fn (cb (str which ", " kind))})))]
        (Thread/sleep 1000)
        (reset! seen [])
        (doseq [i (range 20)] (stream! "L p k1" (not-text (+ 100 i)) (+ 100 i)) (Thread/sleep 100))
        (Thread/sleep 2500)
        (say "20 writes of one row by the stream topology, 100 ms apart; calls of each push by the class of its diff:")
        (doseq [[which n] (tally)] (say "  " which (pr-str n)))
        (reset! seen [])
        (doseq [i (range 20)] (batch! "L p k1" (not-text (+ 200 i)) (+ 200 i)))
        (Thread/sleep 2500)
        (say "20 writes of one row by the microbatch topology, each a batch of its own:")
        (doseq [[which n] (tally)] (say "  " which (pr-str n)))
        (reset! seen [])
        (doseq [i (range 100)] (stream! "L p k1" (not-text (+ 300 i)) (+ 300 i)))
        (Thread/sleep 2500)
        (say "100 writes of one row by the stream topology, one straight after the other's ack:")
        (doseq [[which n] (tally)] (say "  " which (pr-str n)))
        (reset! seen [])
        (stream! "L p k9" (not-text 999) 999)
        (batch! "L p k9" (not-text 999) 999)
        (Thread/sleep 2500)
        (say "one new row in the run, by each topology:")
        (doseq [[which n] (tally)] (say "  " which (pr-str n)))
        (say "after all writes, the row that each table holds at the one row is the last one written:"
             (= (not-text 399) (:value (one "$$s-rows" "L p k1")))
             (= (not-text 219) (:value (one "$$m-rows" "L p k1"))))
        (run! close! pushes)))))
(say "END")
(shutdown-agents)
(System/exit 0)
