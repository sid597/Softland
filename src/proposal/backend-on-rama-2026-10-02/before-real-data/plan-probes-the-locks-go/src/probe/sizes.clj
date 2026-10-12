(ns probe.sizes
  "The counts and the times that section D of the plan rests on. Pure: no
  cluster. It calls the store's own pure functions and changes none.

  Parts:

    S1  one value at the bound of 65,536 bytes, of three kinds, and two small
        ones: the bytes of its one text; the time of each walk that a gate
        makes of it, today and after; and of the seal's opening, which goes.
    S2  a permission of each depth: its text, alone, as an act's own and as
        a given one; where the text writes #too-deep; whether it is a value;
        and, with the shape's number set to 32 for the run (the var is set
        back after), what the act's check and the declaration's check say.
    S3  what a permission of a depth costs each row, and an act of 256
        facts.
    S4  the largest thing that today's limits take, and the same under a
        permission of depth 32: its counts, and the time of the digest.
    S5  two things seen on the way: a string that is not well-formed UTF-16,
        and a map whose keys become equal in the normal form.

  Run, from this folder:  clojure -M:sizes     The log is logs/sizes.log.
  A time is the middle of some runs on one machine, in one JVM, after a
  warm-up: a floor, not a measure of a gate."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.seal :as seal]
            [softland.store.door.text :as text]
            [softland.store.record.rows :as rows])
  (:import [java.util UUID]))

(defonce ^:private out (atom nil))

(defn- say [& parts]
  (let [line (str/join " " parts)]
    (when-let [w @out] (.write ^java.io.Writer w (str line "\n")) (.flush ^java.io.Writer w))
    (println line)))

(defn- utf8-count-of-string
  "The count of the UTF-8 bytes of a string, with no array made. A lone
  surrogate counts 1, as the JVM's encoder writes it as one byte."
  ^long [^String s]
  (let [n (.length s)]
    (loop [i 0 total 0]
      (if (>= i n)
        total
        (let [c (int (.charAt s i))]
          (cond
            (< c 0x80) (recur (inc i) (inc total))
            (< c 0x800) (recur (inc i) (+ total 2))
            (and (<= 0xD800 c 0xDBFF) (< (inc i) n)
                 (<= 0xDC00 (int (.charAt s (inc i))) 0xDFFF)) (recur (+ i 2) (+ total 4))
            (<= 0xD800 c 0xDFFF) (recur (inc i) (inc total))
            :else (recur (inc i) (+ total 3))))))))

(defn- counted
  "The count of the bytes that the digest of a thing is taken of: one walk of
  the store's `text/feed`, with a counter in place of the hash."
  ^long [x]
  (let [n (long-array 1)]
    (text/feed x
               (fn [^String s] (aset n 0 (+ (aget n 0) (utf8-count-of-string s))))
               (fn [b] (aset n 0 (+ (aget n 0) (alength ^bytes b)))))
    (aget n 0)))

(defn- micros
  "The middle time of `runs` calls of f, in microseconds, after `warm` calls."
  [warm runs f]
  (dotimes [_ warm] (f))
  (let [ts (vec (sort (repeatedly runs #(let [t (System/nanoTime)] (f) (/ (- (System/nanoTime) t) 1000.0)))))]
    (nth ts (quot (count ts) 2))))

(defn- fmt [x] (format "%.1f" (double x)))

;; ---- S1 -------------------------------------------------------------------

(defn- s1-value! [label v warm runs]
  (let [normal (act/normal-value v)
        ^bytes plain (text/utf8 normal)
        lock (seal/new-lock)
        place [:layer (UUID. 0 1) :k]
        sealed (seal/seal lock place plain)
        thing {:act-id [:layer :streaming (UUID. 0 2)] :depot :streaming :version 1 :who (UUID. 0 3)
               :layer :layer :permission [(UUID. 0 3) :layer :layer] :session (UUID. 0 4)
               :facts [{:e (UUID. 0 1) :k :k :v v}]}
        sealed-thing (assoc thing :facts [{:e (UUID. 0 1) :k :k :sealed sealed :lock-id [[:layer :streaming (UUID. 0 5)] 0]}])
        row-string (rows/held-value plain)]
    (say "S1" label "· bytes of the one text of its normal form:" (alength plain)
         "· the counting walk gives:" (counted normal)
         "· (= row String, (text/text normal)):" (= row-string (text/text normal)))
    (say "S1" label "· microseconds, the middle of" runs "runs:"
         "\n     value? (the engine's leaf):" (fmt (micros warm runs #(act/value? v)))
         "\n     normal-value (the engine's leaf):" (fmt (micros warm runs #(act/normal-value v)))
         "\n     the bytes counted by building them, (alength (text/utf8 v)):" (fmt (micros warm runs #(alength ^bytes (text/utf8 normal))))
         "\n     the bytes counted by a walk that builds no whole text:" (fmt (micros warm runs #(counted normal)))
         "\n     the row's String made as the plain branch of fact-row makes it today, (held-value (text/utf8 v)):" (fmt (micros warm runs #(rows/held-value (text/utf8 normal))))
         "\n     the row's String made straight, (text/text v):" (fmt (micros warm runs #(text/text normal)))
         "\n     the value digest from the row's String, (value-digest (plain-bytes s)):" (fmt (micros warm runs #(rows/value-digest (rows/plain-bytes row-string))))
         "\n     the digest of the thing with the value plain:" (fmt (micros warm runs #(digest/digest thing)))
         "\n     TODAY the digest of the thing with the value sealed:" (fmt (micros warm runs #(digest/digest sealed-thing)))
         "\n     TODAY the opening of the seal (seal/open):" (fmt (micros warm runs #(seal/open lock place sealed)))
         "\n     TODAY the row's String from the opened bytes (held-value):" (fmt (micros warm runs #(rows/held-value plain))))))

(defn- s1! []
  (say "---- S1: one value, and each walk of it ----")
  (s1-value! "a string of 65,534 a" (apply str (repeat 65534 "a")) 100 101)
  (s1-value! "a vector of 9,362 longs" (vec (range 100000 109362)) 100 101)
  (s1-value! "a map of 520 small maps"
             (into {} (map (fn [i] [(keyword (str "k" i)) {:a (apply str (repeat 60 "x")) :b [1 2 3 i] :c #{:x :y} :d nil}])
                           (range 520)))
             60 61)
  (s1-value! "a string of 21,845 times U+20AC (3 bytes each)" (apply str (repeat 21845 (char 0x20AC))) 100 101)
  (s1-value! "the string \"hello\"" "hello" 2000 2001)
  (s1-value! "a small map" {:title "a note" :tags #{:a :b} :at [1 2]} 2000 2001))

;; ---- S2 and S3 ------------------------------------------------------------

(def ^:private long-id
  "A keyword whose printed text has 256 characters, the limit of an id."
  (keyword (apply str (repeat 255 "k"))))

(defn- permission
  "A permission id of depth d in a layer. `id` gives each who and granted-in."
  [layer d id]
  (reduce (fn [parent _] [(id) layer layer parent])
          [(id) layer layer]
          (range (dec d))))

(defn- thing-under
  "A thing as it arrives, with the own permission given and the facts given."
  [layer own facts]
  {:act-id [layer :streaming (UUID. 0 9)] :depot :streaming :version 1 :who (nth own 0)
   :layer layer :permission own :session (UUID. 0 4) :facts facts})

(defn- give
  "A thing as it arrives: a give of a permission of depth d by the holder of
  the one above it."
  [layer d id]
  (let [above (permission layer (dec d) id)
        holder (id)
        given [holder layer layer above]]
    {:thing (thing-under layer above [{:e holder :k :softland.store/permission :v given}])
     :given given}))

(defn- too-deep? [x] (str/includes? (text/text x) "#too-deep"))

(defn- s2! []
  (say "---- S2: a permission of each depth ----")
  (say "S2 act/max-depth:" act/max-depth "· text/max-depth:" text/max-depth
       "· act/max-permission-depth today:" act/max-permission-depth)
  (doseq [[ids id] [["UUID ids" #(UUID/randomUUID)] ["ids of 256 characters" (constantly long-id)]]
          d [1 4 5 16 31 32 33 61 62 63 64 65]]
    (let [layer (if (= "UUID ids" ids) (UUID. 0 7) long-id)
          p (permission layer d id)
          own (thing-under layer p [{:e (UUID. 0 1) :k :k}])
          given (thing-under layer (permission layer 1 id) [{:e (nth p 0) :k :softland.store/permission :v p}])]
      (say "S2" ids "· depth" d
           "· bytes of its text alone:" (alength ^bytes (text/utf8 p))
           "· #too-deep in its text alone:" (too-deep? p)
           "· in the text of a thing that holds it as the act's own:" (too-deep? own)
           "· in the text of a thing that holds it as a fact's value:" (too-deep? given)
           "· value?:" (act/value? p)
           "· has the shape today:" (act/has-shape? :permission-id p))))
  (say "S2 with the shape's number set to" act/max-depth "for this run (the store's code is not changed):")
  (with-redefs-fn {#'softland.store.act/nesting {:permission-id act/max-depth}}
    (fn []
      (doseq [d [4 5 31 32 33 34 64 1000 100000]]
        (let [id #(UUID/randomUUID)
              layer (UUID. 0 7)
              p (permission layer d id)
              own-act {:version 1 :who (nth p 0) :layer layer :permission p :session (UUID. 0 4)
                       :facts [{:e (UUID. 0 1) :k :k}]}
              parsed (act/parse own-act)
              t-parse (micros 20 21 #(act/parse own-act))
              t-digest (micros 20 21 #(digest/digest own-act))]
          (say "S2 an act whose own permission has depth" d
               "· the act's check:" (if (:ok parsed) "ok" (:refuse parsed))
               "· microseconds of the check:" (fmt t-parse)
               "· of the digest of the act:" (fmt t-digest))))
      (doseq [d [2 5 32 33 34]]
        (let [{:keys [thing given]} (give (UUID. 0 7) d #(UUID/randomUUID))
              got (declaration/check thing)]
          (say "S2 a give of a permission of depth" d "by the holder of one of depth" (dec d)
               "· the declaration's check:" (if (:ok got) "ok" (:refuse got))
               "· value? of the given permission:" (act/value? given)
               "· microseconds of the check:" (fmt (micros 20 21 #(declaration/check thing))))))
      (let [p (conj (permission (UUID. 0 7) 32 #(UUID/randomUUID)) :see)]
        (say "S2 a permission of depth 32 with the fifth slot · value?:" (act/value? p)
             "· has the shape:" (act/has-shape? :permission-id p)))
      (let [p (permission (UUID. 0 7) 32 #(UUID/randomUUID))
            q (permission long-id 32 (constantly long-id))]
        (say "S2 microseconds of (text/text permission) at depth 32 · UUID ids:" (fmt (micros 500 501 #(text/text p)))
             "· ids of 256 characters:" (fmt (micros 500 501 #(text/text q))))
        (say "S2 microseconds of (text/text permission) at depth 4 · UUID ids:"
             (fmt (micros 500 501 #(text/text (permission (UUID. 0 7) 4 (fn [] (UUID. 0 8))))))
             "· ids of 256 characters:" (fmt (micros 500 501 #(text/text (permission long-id 4 (constantly long-id))))))))))

(defn- s3! []
  (say "---- S3: what a permission costs each row, and an act of 256 facts ----")
  (doseq [[ids id layer who] [["UUID ids" #(UUID/randomUUID) (UUID. 0 7) (UUID. 0 3)]
                              ["ids of 256 characters" (constantly long-id) long-id long-id]]
          d [1 4 5 6 8 16 32]]
    (let [p (permission layer d id)
          per-row (+ (alength ^bytes (text/utf8 p)) (alength ^bytes (text/utf8 who)))
          mib (* 1024 1024)]
      (say "S3" ids "· depth" d
           "· bytes of the text of the permission and of the who, which each fact's row holds:" per-row
           "· times 256 facts:" (* 256 per-row)
           "· the most facts for which that product stays within 1,048,576:" (min 256 (quot mib per-row))))))

;; ---- S4 -------------------------------------------------------------------

(defn- largest-thing
  "The largest thing that the limits take: 256 facts, each a value whose one
  text is 65,536 bytes under an entity and a key of 256 characters, each
  naming a fact to replace; 1,024 entries of :based-on; 256 subjects; an own
  permission of the depth given, with ids of 256 characters."
  [d]
  (let [layer long-id
        ids (fn [word n] (mapv (fn [i] (keyword (str (apply str (repeat (- 255 (count (str word i))) "x")) word i))) (range n)))
        value (apply str (repeat 65534 "v"))
        es (ids "e" 256)
        fact-id (fn [i] [[layer :streaming (UUID. 0 i)] 255])]
    (-> (thing-under layer (permission layer d (constantly long-id))
                     (mapv (fn [e i] {:e e :k long-id :v value :replaces (fact-id i)}) es (range)))
        (assoc :based-on (into {} (map (fn [i] [(fact-id (+ 1000 i)) (dec (bit-shift-left 1 62))]) (range 1024)))
               :subjects (set (ids "s" 256))
               :because-of [layer :streaming (UUID. 0 1)]
               :claimed-when Long/MAX_VALUE))))

(defn- s4! []
  (say "---- S4: the largest thing ----")
  (doseq [d [4 32]]
    (let [thing (largest-thing d)
          p (:permission thing)
          per-row (+ (alength ^bytes (text/utf8 p)) (alength ^bytes (text/utf8 (:who thing))))
          values (reduce + (map #(alength ^bytes (text/utf8 (:v %))) (:facts thing)))
          whole (counted thing)]
      (say "S4 own permission of depth" d "with ids of 256 characters"
           "· bytes of the one text of the whole thing (what the digest is taken of):" whole
           "· bytes of its 256 values:" values
           "· bytes of who and permission in each row:" per-row "· times 256:" (* 256 per-row)
           "· values plus that product:" (+ values (* 256 per-row))
           "· the whole text plus that product:" (+ whole (* 256 per-row)))
      (say "S4 depth" d "· milliseconds, the middle of 5 runs: the digest of the thing:"
           (fmt (/ (micros 2 5 #(digest/digest thing)) 1000.0))
           "· a count of the bytes of each of its 256 values by a walk:"
           (fmt (/ (micros 2 5 #(reduce + (map (fn [f] (counted (:v f))) (:facts thing)))) 1000.0))
           "· the 256 row Strings made straight:"
           (fmt (/ (micros 2 5 #(count (mapv (fn [f] (text/text (:v f))) (:facts thing)))) 1000.0))
           "· the text of who and permission made 256 times:"
           (fmt (/ (micros 2 5 #(dotimes [_ 256] (text/text p) (text/text (:who thing)))) 1000.0))))))

;; ---- S5 -------------------------------------------------------------------

(defn- shown [^String s]
  (apply str (map (fn [c] (let [n (int c)] (if (and (>= n 0x20) (< n 0x7f)) (str c) (format "\\u%04X" n)))) s)))

(defn- s5! []
  (say "---- S5: two things seen on the way ----")
  (let [s (str "a" (char 0xD83D) "b")
        other "a?b"]
    (say "S5 a string with a lone surrogate, a U+D83D b · value?:" (act/value? s)
         "· its one text:" (shown (text/text s))
         "· the UTF-8 of that text, as hex:" (text/hex (text/utf8 s))
         "· what today's door seals and a gate keeps, (held-value (text/utf8 s)):" (shown (rows/held-value (text/utf8 s)))
         "· its digest equals the digest of the string a ? b:" (= (digest/hex-of s) (digest/hex-of other))
         "· the two texts are equal:" (= (text/text s) (text/text other))))
  (let [m (hash-map 1.0M :a (BigDecimal. "1.00") :b)
        s (hash-set 1.0M (BigDecimal. "1.00") 0.0 -0.0)
        n (act/normal-value m)]
    (say "S5 a map built with the keys 1.0M and 1.00M · (= 1.0M 1.00M):" (= 1.0M (BigDecimal. "1.00"))
         "· entries:" (count m) "· its one text:" (text/text m)
         "· entries of its normal form:" (count n) "· the text of the normal form:" (text/text n)
         "· a set built of 1.0M, 1.00M, 0.0 and -0.0 has" (count s) "members, and its normal form"
         (count (act/normal-value s)) ":" (text/text s) "->" (text/text (act/normal-value s))))
  (let [v [5N (biginteger 7)]]
    (say "S5 a vector of a big integer and a BigInteger that fit a long · its one text as given:" (text/text v)
         "· the text of its normal form:" (text/text (act/normal-value v)))))

(defn -main [& _]
  (io/make-parents "logs/sizes.log")
  (reset! out (io/writer "logs/sizes.log"))
  (say "RUN Clojure" (clojure-version) "· Java" (System/getProperty "java.version") "· pure, no cluster")
  (s1!) (s2!) (s3!) (s4!) (s5!)
  (say "RUN END")
  (shutdown-agents))
