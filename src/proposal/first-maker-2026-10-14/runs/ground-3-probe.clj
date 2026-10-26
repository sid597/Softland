;; Ground check, section 3: the break agent's probe of two claims, by value.
;; A run, not code: it requires the store's pure namespaces and changes nothing.
;; Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/runs/ground-3-probe.clj \
;;     > src/proposal/first-maker-2026-10-14/runs/ground-3-probe-output.txt 2>&1
;; Each printed line begins with a probe number, so the findings can cite it.

(require '[softland.store.gate.stamp :as stamp]
         '[softland.store.gate.decision :as decision]
         '[softland.store.record.rows :as rows]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.door.digest :as digest]
         '[clojure.string :as str])

(defn- try-it [f]
  (try (let [v (f)] [:gave v (when (some? v) (.getSimpleName (class v)))])
       (catch Throwable e [:threw (.getSimpleName (class e))])))

(defn- say [n & xs] (println (str "P" n) (str/join " " (map pr-str xs))))

(def tp declaration/time-part)
(def now 1700000000000)
(def uid (java.util.UUID. 0 1))
(def good
  {:act-id [:alice :streaming uid] :depot :streaming tp now
   :version 1 :who :alice :layer :alice :permission [:alice :alice :alice] :session :alice-s1
   :facts [{:e :note-1 :k :text :v "a text"}]})
(defn at-time [t] (assoc good tp t))
(defn verdict [checked] (if (contains? checked :ok) :ok (:refuse checked)))

;; P1. next-stamp with odd last stamps.
(doseq [[what x] [["nil" nil] ["false" false] ["0" 0] ["-5" -5] ["Long/MAX_VALUE" Long/MAX_VALUE]
                  ["1.5 (a double)" 1.5] ["\"x\" (a string)" "x"] ["(int 41)" (int 41)]]]
  (say 1 what '-> (try-it #(stamp/next-stamp x))))

;; P2. usable-time against the declaration's check, for times the proof does not list.
;; A disagreement would be an accept whose time-on is nil (fact-row would throw) or the reverse.
(doseq [[what t] [["(short -1)" (short -1)] ["(byte 0)" (byte 0)] ["Integer/MAX_VALUE" Integer/MAX_VALUE]
                  ["(int -1)" (int -1)] ["1N, a small BigInt" 1N] ["0.0" 0.0] ["Double/NaN" Double/NaN]
                  ["1/2, a ratio" 1/2] ["true" true] ["a keyword" :now] ["\\a, a char" \a]
                  ["(Long. 5)" (Long/valueOf 5)] ["Long/MAX_VALUE" Long/MAX_VALUE]]]
  (let [thing (at-time t)
        usable (declaration/usable-time thing)
        word (verdict (declaration/check thing))]
    (say 2 what 'usable-time usable 'check word
         (if (= (some? usable) (= :ok word)) 'agree 'DISAGREE))))

;; P3. The digest leaves the time out: two times, no time, a nil time, a deeper :door-time.
(let [d (fn [x] (digest/hex-of x))]
  (say 3 "same act at two times, one digest:" (= (d (at-time now)) (d (at-time (inc now)))))
  (say 3 "same act with no time, the same digest:" (= (d (at-time now)) (d (dissoc good tp))))
  (say 3 "same act with a time of nil, the same digest:" (= (d (at-time now)) (d (at-time nil))))
  (say 3 "same act with a time of -1 (refused :no-door-time), the same digest:" (= (d (at-time now)) (d (at-time -1))))
  (say 3 "a :door-time inside a fact is kept in the digest:"
       (not= (d good) (d (assoc-in good [:facts 0 tp] 1)))))

;; P4. What a refusal writes, with a time, with none, and for each word kind.
(let [act-id [:alice :streaming uid]
      dg (digest/digest good)]
  (say 4 "refusal with a time:" (decision/writes act-id nil :malformed 7 now dg {}))
  (say 4 "refusal with no time:" (decision/writes act-id nil :no-door-time 7 nil dg {}))
  (say 4 "refusal of an act whose in-hand holds rows, :layer-not-made:"
       (mapv first (decision/writes act-id (:ok (declaration/check good)) :layer-not-made 7 now dg
                                    {:since-by-place {"x" 1} :place-rows {"x" {:stamp 0}} :level-from 7}))))

;; P5. An accept: the time is in no key; two times give the same keys and the same stamps.
(let [act-id [:alice :streaming uid]
      act (:ok (declaration/check good))
      dg (digest/digest good)
      t1 4242424242424
      t2 9191919191919
      in {:since-by-place {} :place-rows {} :level-from 7}
      w1 (decision/writes act-id act nil 7 t1 dg in)
      w2 (decision/writes act-id act nil 7 t2 dg in)
      strip (fn [ws] (mapv (fn [[which k row]] [which k (if (map? row) (dissoc row :time) row)]) ws))]
  (say 5 "accept writes:" (mapv (fn [[which k]] [which k]) w1))
  (say 5 "any key holds the time's digits:" (boolean (some (fn [[_ k]] (and (string? k) (str/includes? k (str t1)))) w1)))
  (say 5 "two times, same rows but for :time:" (= (strip w1) (strip w2)))
  (say 5 "an accept with no time (the declaration's refusal is what keeps this out):"
       (try-it #(decision/writes act-id act nil 7 nil dg in))))

;; P6. The keys made of a fact under a marked key hold a stamp, never a time.
(say 6 "by-value-key:" (rows/by-value-key :alice :text 12 (rows/value-digest-of "a text") :note-1))
(say 6 "by-value-key with a since past the wrap:" (rows/by-value-key :alice :text Long/MIN_VALUE "d" :note-1))

;; P7. The comparison in the facts by value across the wrap of the stamp (stamp.clj 36-37, 51).
;; A key marked at stamp MAX-1; the place's row before was written at MIN (after the wrap);
;; a new value replaces it. Before the wrap the entry of the value before would go out.
(let [bvw @#'decision/by-value-writes
      act {:layer :alice}
      fact {:e :note-1 :k :text}
      entry {:entry true}
      new-bytes (rows/plain-bytes "\"new\"")]
  (say 7 "before the wrap (since 5, row before at 9):"
       (mapv (fn [[w _ r]] [w (some? r)]) (bvw act fact new-bytes {:stamp 9 :value "\"old\""} 5 entry)))
  (say 7 "across the wrap (since MAX-1, row before at MIN):"
       (mapv (fn [[w _ r]] [w (some? r)]) (bvw act fact new-bytes {:stamp Long/MIN_VALUE :value "\"old\""} (dec Long/MAX_VALUE) entry))))

;; P8. Strays get no answer and no stamp: a timeless thing with a usable act-id is no stray.
(doseq [[what x] [["timeless, usable act-id" (dissoc good tp)]
                  ["act-id of the other gate, timeless" (dissoc (assoc good :act-id [:alice :microbatch uid]) tp)]
                  ["act-id of another layer" (assoc good :layer :bob)]
                  ["no act-id" (dissoc good :act-id)]]]
  (say 8 what '-> (declaration/stray-reason :streaming x)))

;; P9. The other time on an act: the maker's :claimed-when. Its form refuses; its sign does not.
(doseq [[what v] [["-1" -1] ["1.5" 1.5] ["Long/MAX_VALUE" Long/MAX_VALUE]]]
  (say 9 ":claimed-when" what '-> (verdict (declaration/check (assoc good :claimed-when v)))))

;; P10. The answer row: a time on a refused thing is kept; none for a thing without one.
(say 10 (rows/answer-row :permission-does-not-stand 3 now (byte-array 0)))
(say 10 (rows/answer-row :no-door-time 3 nil (byte-array 0)))
(say 10 "answer-of a refused row:" (dissoc (rows/answer-of (rows/answer-row :no-door-time 3 nil (byte-array 32))) :digest))
