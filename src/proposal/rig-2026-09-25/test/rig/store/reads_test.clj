(ns rig.store.reads-test
  "Stage 5a's pure functions, no cluster (PLAN-read-exit.md, 'Namespaces and
  tests'): parsers, addresses and their order, index writes, purge and
  rebuild writes, the page loop, the fingerprint, the read entry's facts, the
  entry's moments in the gate's stamp rule, and property tests that every
  function the topologies call is total (T14's pure half).

  The page loop is driven here exactly as the query's dataflow drives it:
  pages read from `:from`, `:page` entries each, the `[:kv]` candidates
  opened in order until limit + 1 match, then `page-step`.

  Wave 1 (phases 2 and 3 merged): the open step's body is phase 2's
  `rig.store.locks/open-row>`, so the pure tests drive the page loop and
  the rebuild with its pure half, `locks/open-with`, over plaintext rows (a
  control value's row, a retract; what the tests build); the gate's
  offers are sealed at the door, so the gate's tests seal them with locks
  they make up and decide with the lock context the event would read."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.walk :as walk]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.grammar :as grammar]
            [rig.store.locks :as locks]
            [rig.store.reads :as reads]
            [rig.store.toy-grammars :as tg])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ helpers

(def hints reads/seed-hints)
(def z "\u0000")

(defn- nm
  ([] (env/make-name :alice :by-layer))
  ([layer] (env/make-name layer :by-layer)))

(defn- row [e k v & {:keys [replaces mark]}]
  {:e e :k k :v (env/encode-value v) :replaces replaces :mark (or mark #{})})

(defn- fid-of [n i] [n (long i)])

(defn- open-row
  "The open step over one row, as phase 2's pure half gives it with no lock
  context (`locks/open-with`): what `reads/open-row>` returns for a row
  that holds no sealed value (a plaintext row, a retract), which is what
  these pure tests build. Same arguments as the seam."
  [_layer _fid row stamp T]
  (locks/open-with row stamp T nil nil {}))

(defn- bytes->vec
  "Data with every byte array replaced by its bytes as a vector, so two
  reads of the same bytes compare equal (a byte array compares by
  identity)."
  [x]
  (walk/postwalk #(if (bytes? %) (vec %) %) x))

(defn- seal-raw
  "A plaintext offer sealed as the door seals it, under locks made up here:
  each value fact under its own lock, cited as [lease-name i]. [raw
  delivered]."
  [raw]
  (let [lease [(:layer raw) :by-layer :offer (UUID/randomUUID)]
        idx (keep-indexed (fn [i f] (when (env/value-fact? f) i)) (:facts raw))
        ids (zipmap idx (map (fn [j] [lease (long j)]) (range)))
        Ks (into {} (map (fn [id] [id (locks/fresh-lock)])) (vals ids))]
    [(update raw :facts (fn [fs] (vec (map-indexed (fn [i f]
                                                     (if-let [id (get ids i)]
                                                       (-> f (dissoc :v) (assoc :sealed (locks/seal (get Ks id) (locks/canonical-bytes (:v f))) :lock-id id))
                                                       f))
                                                   fs))))
     Ks]))

(defn- lx-for
  "The lock context the gate's event would read for `offer`: the delivered
  locks, the value checks over them, the persons given, the fresh draw."
  [offer settings delivered persons]
  ;; phase 6: the grammars come from the layer's key rows, here the toy grammars' (D-P4)
  (let [rows (tg/rows)
        rv (locks/value-context offer settings delivered (grammar/grammars-of rows))]
    (assoc (locks/lock-context delivered rv persons nil nil (locks/fresh-for offer settings rv)) :key-rows rows)))

(defn- drive
  "The query's page loop over a sorted map of address -> entry, as the
  dataflow runs it; `open` stands for `open-row>`."
  ([pp m smap] (drive pp m smap (fn [e] (open-row :alice (:fid e) e (:stamp e) m))))
  ([pp m smap open]
   (loop [st (reads/page-init pp m) pages 0 sizes []]
     (let [page (take (:page st) (subseq smap >= (:from st)))
           entries (reads/page-entries page)
           cands (reads/kv-candidates st entries)
           opened (loop [cs cands need (reads/kv-need st) got {}]
                    (if (or (empty? cs) (<= need 0))
                      got
                      (let [[a e] (first cs) o (open e)]
                        (recur (rest cs) (reads/need-after need (reads/kv-hit? st o)) (assoc got a o)))))
           st2 (reads/page-step st entries opened)
           sizes (conj sizes (:page st))]
       (if (:done? st2)
         (assoc st2 :pages (inc pages) :sizes sizes)
         (recur st2 (inc pages) sizes))))))

(defn- index-of-act
  "The index fields one act's writes produce, as {field {address entry}}."
  [w]
  (reduce (fn [m [f a e]] (assoc-in m [f a] e))
          {:ix-ek (sorted-map) :ix-ke (sorted-map) :ix-kv (sorted-map) :ix-of (into {} (:index-of w))}
          (:index-put w)))

;; --------------------------------------------------------------- addresses

(deftest addresses
  (let [n1 (nm)
        a (fn [field & {:as parts}] (reads/address field parts))]
    (testing "parts joined by U+0000, stamps as 16 hex digits, the fact id's index as 8"
      (is (= (str "e0" z "note" z "000000000000000c" z (env/canonical n1) z "00000002")
             (a :ix-ek :e :e0 :k :note :stamp 12 :fid [n1 2])))
      (is (= (str "note" z "e0" z "000000000000000c" z (env/canonical n1) z "00000002")
             (a :ix-ke :e :e0 :k :note :stamp 12 :fid [n1 2])))
      (is (= (str "note" z "00000003" z "\"x\"" z "000000000000000c" z (env/canonical n1) z "00000002")
             (a :ix-kv :k :note :vtext "\"x\"" :stamp 12 :fid [n1 2])))
      (is (= (str "read/pattern" z "e" z) (subs (a :ix-ke :e :e :k :read/pattern :stamp 1 :fid [n1 0]) 0 15))
          "a namespaced key is its printed text without the colon"))
    (testing "String order is stamp order, and within one act the act's order (index 10 after 9)"
      (let [addrs (for [s [5 12 (hlc/pack 1790000000000 3)]] (a :ix-ek :e :e0 :k :note :stamp s :fid [n1 0]))]
        (is (= addrs (sort addrs))))
      (is (neg? (compare (a :ix-ek :e :e0 :k :note :stamp 7 :fid [n1 9])
                         (a :ix-ek :e :e0 :k :note :stamp 7 :fid [n1 10])))
          "D5: the canonical text of [name 10] would sort before [name 9]"))
    (testing "a prefix is a range: e0's entries lie in [e0␀, e0␁) and e0a's do not"
      (let [addrs (sort (for [e [:e :e0 :e0a :e1] k [:mention :note] s [5 12]]
                          (a :ix-ek :e e :k k :stamp s :fid [n1 0])))
            in (filter #(and (not (neg? (compare % (str "e0" z)))) (neg? (compare % (str "e0" "\u0001")))) addrs)]
        (is (= 4 (count in)))
        (is (every? #(str/starts-with? % (str "e0" z)) in))))
    (testing "the value index's length prefix: a value whose text holds U+0000 cannot make another value's prefix"
      (let [v1 (str "a" z "b") v2 "a"
            t1 (env/encode-value v1) t2 (env/encode-value v2)
            p2 (#'reads/kv-prefix :note t2)]
        (is (not (str/starts-with? (a :ix-kv :k :note :vtext t1 :stamp 1 :fid [n1 0]) p2)))))))

;; ----------------------------------------------------------------- parsers

(deftest parse-pattern
  (let [p (fn [pat & [limit as-of]] (reads/parse-pattern pat limit as-of hints))]
    (testing "the six forms, with the index and prefix each reads"
      (is (= [:all :ix-ek ""] ((juxt :kind :ix :prefix) (p [:all]))))
      (is (= [:e :ix-ek (str "e0" z)] ((juxt :kind :ix :prefix) (p [:e :e0]))))
      (is (= [:ek :ix-ek (str "e0" z "note" z)] ((juxt :kind :ix :prefix) (p [:ek :e0 :note]))))
      (is (= [:latest :ix-ek (str "e0" z "note" z)] ((juxt :kind :ix :prefix) (p [:latest :e0 :note]))))
      (is (= [:k :ix-ke (str "note" z)] ((juxt :kind :ix :prefix) (p [:k :note]))))
      (is (= [:kv :ix-kv (str "note" z "00000003" z "\"x\"" z)] ((juxt :kind :ix :prefix) (p [:kv :note "x"])))))
    (testing "defaults and bounds (RC2): limit 1,000 by default, 1 to 10,000 allowed"
      (is (= 1000 (:limit (p [:all]))))
      (is (= 1 (:limit (p [:all] 1))))
      (is (= 10000 (:limit (p [:all] 10000))))
      (is (= {:refused :bad-pattern} (p [:all] 0)))
      (is (= {:refused :bad-pattern} (p [:all] 10001)))
      (is (= {:refused :bad-pattern} (p [:all] "10"))))
    (testing "a malformed moment is refused; nil means now"
      (is (nil? (:as-of (p [:all]))))
      (is (= 5 (:as-of (p [:all] nil 5))))
      (is (= {:refused :bad-pattern} (p [:all] nil -1)))
      (is (= {:refused :bad-pattern} (p [:all] nil "5"))))
    (testing "anything outside the language is refused as data"
      (doseq [bad [nil 42 "[:all]" '(:all) [:all :x] [:e] [:e "e0"] [:e (keyword "a b")] [:ek :e0] [:latest :e0]
                   [:k] [:zz :e0] [:kv :note] {:kind :all}]]
        (is (= {:refused :bad-pattern} (p bad)) (pr-str bad))))
    (testing "[:kv]: by value only when hinted; an opaque key is refused :opaque, even if also hinted"
      (is (= {:refused :not-indexed} (p [:kv :mention "x"])))
      (is (= {:refused :opaque} (reads/parse-pattern [:kv :note "x"] nil nil (assoc hints :opaque #{:note}))))
      (is (= {:refused :bad-pattern} (p [:kv :note nil])) "D10: a retract is never indexed by value")
      (is (= {:refused :bad-pattern} (p [:kv :note 1/3])) "not EDN the store holds"))
    (testing "a [:kv] value is normalised as the gate normalises it, and nests in at most 28 collections"
      (is (= [:kv :note 7] (:pattern (p [:kv :note (int 7)]))))
      (let [nest (fn [n] (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))]
        (is (= :kv (:kind (p [:kv :note (nest 28)]))))
        (is (= {:refused :bad-pattern} (p [:kv :note (nest 29)])))))))

(deftest parse-point
  (let [n1 (nm)]
    (testing "a non-empty vector of at most 1,000 well-formed fact ids, normalised"
      (is (= {:fids [[n1 0]] :as-of nil} (reads/parse-point :alice [[n1 0]] nil)))
      (is (= [[n1 3]] (:fids (reads/parse-point :alice [(subvec [:x n1 (int 3)] 1)] nil)))
          "a fact id that is a subvec, with an Integer index, is rebuilt as a PersistentVector and a long")
      (is (instance? clojure.lang.PersistentVector (first (:fids (reads/parse-point :alice [(subvec [:x n1 (int 3)] 1)] nil)))))
      (is (= 1000 (count (:fids (reads/parse-point :alice (vec (repeat 1000 [n1 0])) 7))))))
    (testing "refused :bad-read: not a vector, empty (D6), too long, a malformed id, a malformed moment"
      (doseq [[what fids as-of] [["a list" (list [n1 0]) nil] ["empty" [] nil] ["1,001 ids" (vec (repeat 1001 [n1 0])) nil]
                                 ["a bare name" [n1] nil] ["a negative index" [[n1 -1]] nil]
                                 ["a string name" [[(str n1) 0]] nil] ["a negative moment" [[n1 0]] -5]
                                 ["a moment that is text" [[n1 0]] "now"]]]
        (is (= {:refused :bad-read} (reads/parse-point :alice fids as-of)) what)))
    (testing "check-layer: a layer that is not a readable keyword is refused before any read"
      (is (= {:refused :bad-read} (reads/check-layer {:fids [[n1 0]]} "alice" :bad-read)))
      (is (= {:fids [[n1 0]]} (reads/check-layer {:fids [[n1 0]]} :alice :bad-read)))
      (is (= {:refused :x} (reads/check-layer {:refused :x} "alice" :bad-read)) "a refusal stays itself"))))

(deftest moments-and-visibility
  (testing "FR2: min(asked, clock); nil asked is the clock"
    (is (= 100 (reads/moment nil 100)))
    (is (= 40 (reads/moment 40 100)))
    (is (= 100 (reads/moment 1000000 100)) "a moment past the clock records the clock"))
  (testing "RC6, F4, F12: personal, hand and agent layers to their owner; the base to anyone; a missing layer like a private one"
    (let [s (fn [kind owner] {:kind kind :owner owner :class :by-layer :grain :per-value})]
      (is (reads/visible? (s :personal :alice) :alice))
      (is (reads/visible? (s :hand :alice) :alice))
      (is (reads/visible? (s :agent :alice) :alice) "an agent reads its session layer for Alice")
      (is (not (reads/visible? (s :personal :alice) :bob)))
      (is (not (reads/visible? (s :agent :alice) :agent-a)) "never decided for an agent's own id")
      (is (reads/visible? (s :base :operator) :bob))
      (is (not (reads/visible? (s :group :alice) :alice)) "a shared kind is not this exit's")
      (is (= (reads/visible? nil :bob) (reads/visible? (s :personal :alice) :bob) false))
      (is (not (reads/visible? (s :personal :alice) "alice")))))
  (testing "D7: a layer re-classed by entity is not read here"
    (is (reads/placed-by-layer? {:class :by-layer}))
    (is (not (reads/placed-by-layer? {:class :by-entity})))))

;; ----------------------------------------------------------- index writes

(deftest index-writes
  (let [n1 (nm)
        rows [(row :e0 :note "x") (row :e0 :mention {:persons #{:bob}}) (row :e1 :note nil :replaces [n1 0])
              (row :read-1 :read/pattern {:layer :alice :moment {:stamp 3}}) (row :alice :kind :personal)]
        w (reads/index-writes hints :alice n1 rows 42)
        ix (index-of-act w)]
    (testing "an :ix-ek and an :ix-ke entry per fact; the entry is the row with its id and stamp (F6)"
      (is (= 5 (count (:ix-ek ix)) (count (:ix-ke ix))))
      (is (= (assoc (rows 0) :fid [n1 0] :stamp 42)
             (get-in ix [:ix-ek (reads/address :ix-ek {:e :e0 :k :note :stamp 42 :fid [n1 0]})]))))
    (testing "an :ix-kv entry and its :ix-of set only for a value-indexed key with a value (RC11: no retract)"
      (let [kv (reads/address :ix-kv {:k :note :vtext "\"x\"" :stamp 42 :fid [n1 0]})]
        (is (= [kv] (keys (:ix-kv ix))))
        (is (= (assoc (rows 0) :fid [n1 0] :stamp 42) (get-in ix [:ix-kv kv])) "a value entry always carries the row")
        (is (= {[n1 0] #{kv}} (:ix-of ix)))))
    (testing "F7: a :no-copy key's id entries carry no value fields and :copy false"
      (let [e (get-in ix [:ix-ek (reads/address :ix-ek {:e :read-1 :k :read/pattern :stamp 42 :fid [n1 3]})])]
        (is (false? (:copy e)))
        (is (not (contains? e :v)))
        (is (= [:read-1 :read/pattern [n1 3] 42] ((juxt :e :k :fid :stamp) e)))))
    (testing "an opaque key is never indexed by value, even when hinted by value"
      (is (empty? (:index-of (reads/index-writes (assoc hints :opaque #{:note}) :alice n1 rows 42)))))
    (testing "an admitted act deletes nothing; a replay of the same act writes the same lists"
      (is (= [] (:index-del w)))
      (is (= w (reads/index-writes hints :alice n1 rows 42))))
    (testing "the plaintext arity (D9): texts given by the caller decide the value index"
      (let [sealed (assoc (rows 0) :v nil)
            w2 (reads/index-writes hints :alice n1 [sealed] 42 ["\"x\""])]
        (is (= 1 (count (:index-of w2))))))
    (testing "total: a failure gives empty lists and :index-error, never a throw"
      (is (:index-error (reads/index-writes hints :alice n1 [{:e nil :k nil}] 42)))
      (is (:index-error (reads/index-writes hints :alice n1 "rows" 42))))))

(deftest gate-integration
  (let [[raw Ks] (seal-raw (-> {:version 1 :who :alice :layer :alice :class :by-layer
                                :permission [:alice :alice :alice] :session :s1 :stood-on {} :subjects #{}
                                :facts [{:e :e0 :k :note :v "x"} {:e :e0 :k :mention :v {:persons #{:bob}}}]}
                               (assoc :name (nm))))
        offer (:ok (env/parse raw))
        settings {:kind :personal :owner :alice :class :by-layer :grain :per-value}
        persons {:alice {:lock (locks/fresh-lock) :erased-at nil} :bob {:lock (locks/fresh-lock) :erased-at nil}}
        prow {[:alice :alice :alice] {:granted [(nm) 0]}}
        lx (lx-for offer settings Ks persons)]
    (testing "the offer is sealed: the gate's rows hold no plaintext"
      (is (some? offer))
      (is (every? #(nil? (:v %)) (:facts offer))))
    (testing "decide carries the index lists for a yes, from the rows as the gate writes them and the texts it opened (wave 1)"
      (let [d (gate/decide offer settings prow {} 0 1000 (env/digest offer) lx)
            opened (:values (:read lx))]
        (is (= :decide (:kind d)))
        (is (= :yes (get-in d [:record :answer])))
        (is (every? #(nil? (:v %)) (:log d)) "the log rows carry the sealed bytes, never the text")
        (is (= ["\"x\"" (env/encode-value {:persons #{:bob}})] (gate/plain-texts (:log d) opened))
            "the texts the gate opened, one per fact")
        (is (= (bytes->vec (select-keys (reads/index-writes hints :alice (:name offer) (:log d) (:stamp d)
                                                            (gate/plain-texts (:log d) opened))
                                        [:index-put :index-of :index-del]))
               (bytes->vec (select-keys d [:index-put :index-of :index-del]))))
        (is (= [7 1] [(count (:index-put d)) (count (:index-of d))])
            "3 id entries per fact (stage 5b's :ix-s the third) and a value entry for the note, from its opened text; its :ix-of set")
        (let [[_ kv-address] (first (filter #(= :ix-kv (first %)) (:index-put d)))]
          (is (str/includes? kv-address "\"x\"") "the value index is addressed by the opened text"))))
    (testing "a sealed value the gate could not open gives no value entry: the act is refused, nothing indexed"
      (let [d (gate/decide offer settings prow {} 0 1000 (env/digest offer) (lx-for offer settings {} persons))]
        (is (= :does-not-open (get-in d [:record :reason])))
        (is (= [[] [] []] ((juxt :index-put :index-of :index-del) d)))))
    (testing "and empty lists for a no"
      (let [d (gate/decide (assoc offer :who :bob :permission [:bob :alice :alice]) settings {} {} 0 1000 "d" lx)]
        (is (= :no (get-in d [:record :answer])))
        (is (= [[] [] []] ((juxt :index-put :index-of :index-del) d)))))))

;; ------------------------------------------------------------------- purge

(deftest purge-writes
  (let [n1 (nm)
        r (assoc (row :e0 :note "secret") :sealed "bytes" :lock "k" :digest "d" :lock-id "L1")
        kv (reads/address :ix-kv {:k :note :vtext "\"secret\"" :stamp 42 :fid [n1 0]})
        w (reads/purge-writes [n1 0] r 42 #{kv} 99)]
    (testing "the id entries become tombstones: no value field, the forget's date, the id, entity, key and stamp"
      (is (= 3 (count (:index-put w))))
      (doseq [[f a t] (:index-put w)]
        (is (#{:ix-ek :ix-ke :ix-s} f))
        (is (= (reads/address f {:e :e0 :k :note :stamp 42 :fid [n1 0]}) a) "computed from the row, no value needed")
        (is (= {:e :e0 :k :note :replaces nil :mark #{} :lock-id "L1" :fid [n1 0] :stamp 42 :erased-at 99} t))))
    (testing "the value entry and the fact's :ix-of entry are deleted"
      (is (= [[:ix-kv kv] [:ix-of [n1 0]]] (:index-del w)))
      (is (= [] (:index-of w))))
    (testing "no value text anywhere in what a purge writes"
      (is (not (str/includes? (pr-str (:index-put w)) "secret"))))
    (testing "a fact with no value entry: only its tombstones and the :ix-of delete"
      (is (= [[:ix-of [n1 0]]] (:index-del (reads/purge-writes [n1 0] (row :e0 :mention 1) 42 nil 99)))))
    (testing "merge-writes concatenates purges, as phase 2's person forget will"
      (let [w2 (reads/purge-writes [n1 1] (row :e1 :note "b") 42 nil 99)
            m (reads/merge-writes w w2)]
        (is (= 6 (count (:index-put m))))
        (is (= 3 (count (:index-del m))))))
    (testing "total"
      (is (= reads/no-index-writes (reads/purge-writes "fid" nil nil nil nil))))))

;; ----------------------------------------------------------------- rebuild

(deftest rebuild-writes
  (let [n1 (nm) n2 (nm)
        rows1 [(row :e0 :note "x") (row :e0 :mention {:persons #{:bob}})]
        rows2 [(row :e0 :note "y" :replaces [n1 0]) (row :e1 :note nil) (row :r :read/pattern {:moment {:stamp 3}})]
        opens (fn [n s rows] (vec (for [[i r] (map-indexed vector rows)] (open-row :alice [n i] r s nil))))
        acts [{:name n1 :stamp 10 :rows rows1 :opens (opens n1 10 rows1)}
              {:name n2 :stamp 20 :rows rows2 :opens (opens n2 20 rows2)}]
        admitted (reads/merge-writes (reads/index-writes hints :alice n1 rows1 10)
                                     (reads/index-writes hints :alice n2 rows2 20))
        imp (reads/implied hints :alice acts)]
    (testing "put pages imply exactly what admission wrote (T10's pure half)"
      (is (= (index-of-act admitted) imp) "implied equals the admitted entries, field by field")
      (is (= (set (:index-put admitted)) (set (:index-put (reads/put-page-writes hints :alice acts)))))
      (is (= [] (:index-del (reads/put-page-writes hints :alice acts))) "a put page never deletes"))
    (testing "purge agrees with rebuild: an erased open gives purge's tombstone and no value entry"
      (let [erased [{:name n1 :stamp 10 :rows rows1 :opens [{:erased-at 55} (second (opens n1 10 rows1))]}]
            w (reads/put-page-writes hints :alice erased)
            p (reads/purge-writes [n1 0] (rows1 0) 10 nil 55)]
        (is (= (set (:index-put p)) (set (filter #(= [n1 0] (:fid (nth % 2))) (:index-put w)))))
        (is (empty? (:index-of w)) "no :ix-kv set for the erased note")))
    (testing "a value that does not open gets its id entries and no value entry"
      (let [w (reads/put-page-writes hints :alice [{:name n1 :stamp 10 :rows [(rows1 0)] :opens [{:unreadable :does-not-open}]}])]
        (is (= 3 (count (:index-put w))))
        (is (empty? (:index-of w)))))
    (testing "sweep: an implied entry is kept, an unimplied one deleted, a drifted one rewritten (D4)"
      (let [rec {:answer :yes :stamp 10}
            ek0 (reads/address :ix-ek {:e :e0 :k :note :stamp 10 :fid [n1 0]})
            right (get-in imp [:ix-ek ek0])
            stale-a (reads/address :ix-ek {:e :zz :k :note :stamp 10 :fid [n1 0]})
            found {:rec rec :row (rows1 0) :open (open-row :alice [n1 0] (rows1 0) 10 nil)}
            w (reads/sweep-page-writes hints :alice :ix-ek
                                       [[ek0 right] [stale-a right] [ek0 (assoc right :v "\"drift\"")]]
                                       [found found found])]
        (is (= [[:ix-ek stale-a]] (:index-del w)))
        (is (= [[:ix-ek ek0 right]] (:index-put w)))
        (is (= 1 (:deleted w)))))
    (testing "sweep: an entry whose act is not a yes, or has no such row, is deleted"
      (let [ek0 (reads/address :ix-ek {:e :e0 :k :note :stamp 10 :fid [n1 0]})
            e (get-in imp [:ix-ek ek0])]
        (is (= [[:ix-ek ek0]] (:index-del (reads/sweep-page-writes hints :alice :ix-ek [[ek0 e]] [{:rec {:answer :no :stamp 10}}]))))
        (is (= [[:ix-ek ek0]] (:index-del (reads/sweep-page-writes hints :alice :ix-ek [[ek0 e]] [{:rec {:answer :yes :stamp 10} :row nil}]))))
        (is (= [[:ix-ek ek0]] (:index-del (reads/sweep-page-writes hints :alice :ix-ek [[ek0 e]]
                                                                   [{:rec {:answer :yes :stamp 11} :row (rows1 0)}])))
            "another stamp")))
    (testing "sweep of :ix-kv: an unimplied value entry goes, and so does its address from the fact's :ix-of set"
      (let [kv (some (fn [[a e]] (when (= [n1 0] (:fid e)) a)) (:ix-kv imp))
            bogus (str kv "x")
            f {:rec {:answer :yes :stamp 10} :row (rows1 0) :open {:erased-at 5} :ix-of #{kv bogus}}
            w (reads/sweep-page-writes hints :alice :ix-kv [[kv (get-in imp [:ix-kv kv])]] [f])]
        (is (= [[:ix-kv kv]] (:index-del w)) "the value is erased now, so its value entry is not implied")
        (is (= [[[n1 0] #{bogus}]] (:index-of w)) "the set keeps only what was not removed")))
    (testing "sweep of :ix-of: a set that differs is rewritten; one with nothing implied is deleted"
      (let [kv (some (fn [[a e]] (when (= [n1 0] (:fid e)) a)) (:ix-kv imp))
            f {:rec {:answer :yes :stamp 10} :row (rows1 0) :open {:value "x" :stamp 10}}]
        (is (= [] (:index-of (reads/sweep-page-writes hints :alice :ix-of [[[n1 0] #{kv}]] [f]))))
        (is (= [[[n1 0] #{kv}]] (:index-of (reads/sweep-page-writes hints :alice :ix-of [[[n1 0] #{kv "other"}]] [f]))))
        (is (= [[:ix-of [n1 0]]] (:index-del (reads/sweep-page-writes hints :alice :ix-of [[[n1 0] #{kv}]] [(assoc f :open {:erased-at 3})]))))))
    (testing "total"
      (is (= 0 (:deleted (reads/sweep-page-writes hints :alice :ix-ek "x" 42))))
      (is (= reads/no-index-writes (reads/put-page-writes hints :alice "x"))))))

;; --------------------------------------------------------------- the pages

(defn- entries-for
  "A sorted map of :ix-ek entries for facts [e k stamp] of one act each."
  [facts]
  (into (sorted-map)
        (for [[e k s v] facts
              :let [n (nm) fid [n 0] r (row e k v)]]
          [(reads/address :ix-ek {:e e :k k :stamp s :fid fid}) (assoc r :fid fid :stamp s)])))

(deftest page-loop
  (let [five (entries-for (for [i (range 5)] [:t5 (keyword (str "n" i)) (+ 10 i) (str "v" i)]))
        others (entries-for [[:t4 :note 1 "a"] [:t6 :note 2 "b"]])
        smap (merge five others)
        pp (fn [pat limit] (reads/parse-pattern pat limit nil hints))]
    (testing "T5: limit 5 of five facts is complete; 3 and 4 are partial, holding the first in address order"
      (let [r5 (drive (pp [:e :t5] 5) 100 smap)
            r3 (drive (pp [:e :t5] 3) 100 smap)
            r4 (drive (pp [:e :t5] 4) 100 smap)]
        (is (= [5 false] [(count (:kept r5)) (:more? r5)]))
        (is (= [3 true] [(count (:kept r3)) (:more? r3)]))
        (is (= [4 true] [(count (:kept r4)) (:more? r4)]))
        (is (= (take 3 (keys five)) (map :address (:kept r3))))
        (is (not (some #{(nth (keys five) 3)} (map :address (:kept r3))))
            "F3: the fourth only decided the mark")))
    (testing "F8: 500 later entries on :e1 and one on :e0 at s0: [:all] as of s0, limit 10, scans at most 176 and is partial"
      (let [big (merge (entries-for [[:e0 :note 5 "s0"]])
                       (entries-for (for [i (range 500)] [:e1 :note (+ 100 i) (str "w" i)])))
            r (drive (pp [:all] 10) 5 big)]
        (is (<= (:scanned r) 176))
        (is (= 176 (:scanned r)) "the pages are capped by what is left of the budget")
        (is (:more? r))
        (is (= 1 (count (:kept r))))
        (testing "and without the budget's cut the same read would have been complete"
          (is (= 1 (count (filter #(<= (:stamp %) 5) (vals big))))))))
    (testing "doubling pages: 16, 32, 64 ... until the range ends"
      (let [many (entries-for (for [i (range 100)] [:e2 :note (+ 10 i) (str "x" i)]))
            r (drive (pp [:e :e2] 1000) 1000 many)]
        (is (= [16 32 64] (:sizes r)))
        (is (= 100 (count (:kept r))))
        (is (not (:more? r)))))
    (testing "as of a moment: entries stamped later are skipped, not kept; [:ek] folds the moment into its end bound"
      (let [r (drive (pp [:e :t5] 100) 12 smap)]
        (is (= [10 11 12] (map (comp :stamp :entry) (:kept r)))))
      (let [[from end] (reads/bounds (pp [:ek :t5 :n1] 100) 11)]
        (is (= (str "t5" z "n1" z) from))
        (is (= (str "t5" z "n1" z "000000000000000c") end))))
    (testing "an empty read: one page, nothing kept, complete"
      (let [r (drive (pp [:e :nobody] 100) 100 smap)]
        (is (= [0 false 1] [(count (:kept r)) (:more? r) (:pages r)]))))
    (testing "F5: a [:kv] candidate counts only when it opens to the pattern's value"
      (let [n1 (nm) n2 (nm)
            kv (fn [n s] (let [fid [n 0] r (row :t19 :note "same")]
                           [(reads/address :ix-kv {:k :note :vtext "\"same\"" :stamp s :fid fid}) (assoc r :fid fid :stamp s)]))
            m (into (sorted-map) [(kv n1 10) (kv n2 11)])
            erased-n1 (fn [e] (if (= n1 (first (:fid e))) {:erased-at 77} (open-row :alice (:fid e) e (:stamp e) 100)))
            r (drive (pp [:kv :note "same"] 10) 100 m erased-n1)]
        (is (= [[n2 0]] (map (comp :fid :entry) (:kept r))) "the erased candidate is not kept")
        (is (= 1 (:matched r)) "nor counted")
        (is (not (:more? r)))))
    (testing "[:latest]: the tail entry counts only with the pattern's prefix"
      (let [pl (pp [:latest :t5 :n1] 10)
            [a e] (first (subseq five >= (str "t5" z "n1" z)))]
        (is (= 1 (count (:kept (reads/tail-state pl {a e} 100)))))
        (is (= 0 (count (:kept (reads/tail-state pl {"zz" e} 100)))))
        (is (= 0 (count (:kept (reads/tail-state pl {} 100)))))))))

;; ------------------------------------------------------------- fingerprint

(deftest fingerprint
  (let [n1 (nm) n2 (nm)
        pairs #{[[n1 0] 10] [[n2 1] 20]}]
    (testing "T7: the bytes it hashes hold ids and stamps only; the label and the set's canonical text"
      (let [bs (String. ^bytes (reads/fingerprint-bytes pairs) "UTF-8")]
        (is (str/starts-with? bs "softland.read-fp/1\n"))
        (is (= (str "softland.read-fp/1\n" (env/canonical pairs)) bs))
        (doseq [secret ["secret value" "\"x\"" ":token"]]
          (is (not (str/includes? bs secret))))))
    (testing "a set: the order the pairs arrive in does not matter; one pair more or less does"
      (is (= (reads/fingerprint [[[n1 0] 10] [[n2 1] 20]]) (reads/fingerprint [[[n2 1] 20] [[n1 0] 10]])))
      (is (not= (reads/fingerprint pairs) (reads/fingerprint #{[[n1 0] 10]})))
      (is (not= (reads/fingerprint pairs) (reads/fingerprint #{[[n1 0] 10] [[n2 1] 21]})) "another stamp"))
    (testing "keyed and hex: 64 lowercase hex digits, not the unkeyed SHA-256 of the bytes"
      (let [fp (reads/fingerprint pairs)
            sha (let [md (java.security.MessageDigest/getInstance "SHA-256")]
                  (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md (reads/fingerprint-bytes pairs)))))]
        (is (re-matches #"[0-9a-f]{64}" fp))
        (is (not= sha fp))))
    (testing "the empty set has a fingerprint (an empty read's line carries it, T4)"
      (is (string? (reads/fingerprint #{}))))
    (testing "it takes [fid stamp] pairs only: anything else gives nil"
      (is (nil? (reads/fingerprint [[[n1 0] "value"]])))
      (is (nil? (reads/fingerprint ["value"])))
      (is (nil? (reads/fingerprint "value"))))))

;; ------------------------------------------------------------- read entry

(deftest entry-facts
  (let [n1 (nm) enm (nm :alice-hand)
        ent (reads/entry-entity enm)
        ;; :recorded-pattern as the module's pattern answers carry it (step R, R-2)
        pans {:layer :alice :moment {:stamp 99} :kind :pattern :pattern [:e :e0] :recorded-pattern [:e :e0]
              :matched [[[n1 0] 10] [[n1 1] 10]] :mark :partial :fingerprint "ab" :fp-secret :read-fp/v1}
        kv [:kv :note {:a [1 #{:b}]}]
        spec (fn [kind & [rows?]] {:entry-name enm :role :shown :reader-kind kind :rows? rows?})]
    (testing "FR5, FR8: one line for a pattern read, about one fresh entity from the entry's name"
      (let [[f :as fs] (reads/entry-facts pans (spec :person))]
        (is (= 1 (count fs)))
        (is (= [ent :read/pattern] [(:e f) (:k f)]))
        (is (= {:layer :alice :moment {:stamp 99} :role :shown :pattern [:e :e0] :mark :partial :count 2
                :fingerprint "ab" :fp-secret :read-fp/v1 :exact [[[n1 0] 10] [[n1 1] 10]]}
               (:v f)))))
    (testing "ruling 3: a person's and a model's line carry the exact list; a tool's only when it asks"
      (is (contains? (:v (first (reads/entry-facts pans (spec :model)))) :exact))
      (is (not (contains? (:v (first (reads/entry-facts pans (spec :tool)))) :exact)))
      (is (contains? (:v (first (reads/entry-facts pans (spec :tool true)))) :exact)))
    (testing "FR7: a row per fact id of a point read, naming how it came out and never the value"
      (let [ans {:layer :alice :moment {:stamp 99} :kind :point
                 :rows [{:fid [n1 0] :stamp 10 :value "secret"} {:fid [n1 1] :stamp 10 :erased-at 50}
                        {:fid [n1 2] :stamp 10 :unreadable :does-not-open} {:fid [n1 3] :absent true}]}
            fs (reads/entry-facts ans (spec :person))]
        (is (= [:value :erased :unreadable :absent] (map (comp :shown :v) fs)))
        (is (every? #(= :read/point (:k %)) fs))
        (is (= [10 10 10 nil] (map (comp :stamp :v) fs)))
        (is (not (str/includes? (pr-str fs) "secret")))))
    (testing "an empty pattern read gives its line with count 0"
      (is (= 0 (:count (:v (first (reads/entry-facts (assoc pans :matched []) (spec :person))))))))
    (testing "every entry fact is an offer the envelope accepts once the door has sealed it (wave 1: its facts are value facts)"
      (doseq [facts [(reads/entry-facts pans (spec :person))
                     (reads/entry-facts (assoc pans :pattern kv :recorded-pattern (reads/recorded-pattern kv)) (spec :tool))]]
        (let [raw {:version 1 :name enm :who :alice :layer :alice-hand :class :by-layer
                   :permission [:alice :alice-hand :alice-hand] :session :s1 :stood-on {} :subjects #{}
                   :facts facts}]
          (is (= :not-sealed (:refuse (env/parse raw))) "as plaintext the gate refuses it on its face")
          (is (contains? (env/parse (first (seal-raw raw))) :ok) "sealed, the envelope accepts it")
          (is (every? env/value-fact? facts) "every entry fact is sealed at the door, like any value"))))
    (testing "total"
      (is (nil? (reads/entry-facts {:kind :pattern} {:entry-name "x"}))))))

(deftest entry-moments-and-the-stamp
  (let [n1 (nm)
        line (fn [s] {:e :read-1 :k :read/pattern :v {:layer :alice :moment {:stamp s}}})]
    (testing "F1: the moments of :read/* facts, bounded like a carried stamp"
      (is (= [7 9] (reads/entry-moments [(line 7) {:e :e :k :note :v {:moment {:stamp 5}}} (line 9)])))
      (is (= [] (reads/entry-moments [(assoc (line 7) :v "not a line")])))
      (is (= [] (reads/entry-moments [(line env/max-carried-stamp)])))
      (is (= [] (reads/entry-moments [(line -1)])))
      (is (= [] (reads/entry-moments "garbage"))))
    (testing "T18 pure: an act with one line whose moment is above the task's clock and wall is stamped moment + 1"
      (let [m (hlc/pack 5000 7)
            offer {:stood-on {} :facts [(line m)]}]
        (is (= (inc m) (gate/stamp-for offer {} (hlc/pack 10 0) 10)))
        (is (= (hlc/pack 6000 0) (gate/stamp-for offer {} 0 6000)) "the wall still wins when later")
        (is (= (hlc/pack 10 1) (gate/stamp-for {:stood-on {} :facts [(line env/max-carried-stamp)]} {} (hlc/pack 10 0) 10))
            "a moment at max-carried-stamp adds nothing")
        (is (= (hlc/pack 10 1) (gate/stamp-for {:stood-on {} :facts [(assoc (line m) :k :note)]} {} (hlc/pack 10 0) 10))
            "an ordinary fact's value is not a moment")))))

;; ------------------------------------------------------------ index ops

(deftest index-op
  (let [n1 (nm)]
    (testing "the ops, normalised"
      (is (= {:layer :a :op :rebuild-put :after nil :acts 3} (reads/index-op {:layer :a :op :rebuild-put :acts 3})))
      (is (= (into [] n1) (:after (reads/index-op {:layer :a :op :rebuild-put :after (subvec (into [:x] n1) 1) :acts 3})))
          "V1: a subvec cursor comes back a PersistentVector")
      (is (= :rebuild-sweep (:op (reads/index-op {:layer :a :op :rebuild-sweep :field :ix-of :after [n1 0] :entries 5}))))
      (is (= :rebuild-sweep (:op (reads/index-op {:layer :a :op :rebuild-sweep :field :ix-ek :after "x" :entries 512}))))
      (is (= :purge (:op (reads/index-op {:layer :a :op :purge :fid [n1 0] :forget-stamp 5}))))
      (is (= :drop (:op (reads/index-op {:layer :a :op :drop :field :ix-kv :entries 1}))))
      (is (= :put (:op (reads/index-op {:layer :a :op :put :field :ix-ek :address "x" :fid [n1 0] :stamp 1 :e :e :k :k})))))
    (testing "anything else is {:refuse :bad-op}"
      (doseq [bad [nil 42 "op" {} {:layer "a" :op :drop :field :ix-ek :entries 1}
                   {:layer :a :op :rebuild-put :acts 0} {:layer :a :op :rebuild-put :acts 257}
                   {:layer :a :op :rebuild-put :after "n" :acts 1}
                   {:layer :a :op :rebuild-sweep :field :ix-zz :entries 1}
                   {:layer :a :op :rebuild-sweep :field :ix-of :after "x" :entries 1}
                   {:layer :a :op :rebuild-sweep :field :ix-ek :entries 513}
                   {:layer :a :op :purge :fid "f" :forget-stamp 1} {:layer :a :op :purge :fid [n1 0] :forget-stamp -1}
                   {:layer :a :op :purge :fid [n1 0] :forget-stamp env/max-carried-stamp}
                   {:layer :a :op :put :field :ix-of :address "x" :fid [n1 0] :stamp 1 :e :e :k :k}
                   {:layer :a :op :explode}]]
        (is (= {:refuse :bad-op} (reads/index-op bad)) (pr-str bad))))))

(deftest open-shapes
  (let [n1 (nm)]
    (testing "the open step's return shapes over plaintext rows, phase 2's pure half (the seam's body since wave 1)"
      (is (= {:value "x" :stamp 5} (open-row :alice [n1 0] (row :e :note "x") 5 nil)))
      (is (= {:value nil :stamp 5} (open-row :alice [n1 0] (row :e :note nil) 5 10)) "a retract")
      (is (= {:unreadable :after-moment} (open-row :alice [n1 0] (row :e :note "x") 11 10)))
      (is (= {:unreadable :no-such-fact} (open-row :alice [n1 0] nil 5 10)))
      (is (= {:unreadable :no-such-fact} (open-row :alice [n1 0] (row :e :note "x") nil 10)))
      (is (= {:unreadable :does-not-open} (open-row :alice [n1 0] {:v "{:unclosed"} 5 10))))
    (testing "a sealed row: its lock record opens it; its ledger entry dates its erasure, whatever the moment (I-L7)"
      (let [K (locks/fresh-lock)
            persons {:alice {:lock (locks/fresh-lock) :erased-at nil}}
            w (locks/wrap-of :alice #{} false)
            rec (locks/wrap K w persons (repeatedly (locks/seals-needed w) locks/fresh-nonce))
            sealed-row {:e :e :k :note :sealed (locks/seal K (locks/canonical-bytes "y")) :lock-id [n1 0] :lock rec}]
        (is (= {:value "y" :stamp 5} (locks/open-with sealed-row 5 nil nil rec persons)))
        (is (= {:erased-at 9} (locks/open-with sealed-row 5 7 {:stamp 9 :how :excised} rec persons))
            "erased after the moment: the date shows, and nothing else")
        (is (= {:erased-at 3} (locks/open-with sealed-row 5 nil nil rec {:alice {:lock nil :erased-at 3}}))
            "the owner forgotten: the wrap closed on her date")))))

;; ------------------------------------------------------ property tests (T14)

(def garbage
  (gen/one-of [gen/any-printable-equatable
               (gen/vector gen/any-printable-equatable 0 5)
               (gen/map gen/keyword gen/any-printable-equatable {:max-elements 5})
               (gen/return nil)
               (gen/return [:kv :note (loop [v 1 i 0] (if (< i 40) (recur [v] (inc i)) v))])]))

(deftest totality
  (testing "T14: every function the topologies call is total on generated garbage"
    (let [r (tc/quick-check
             300
             (prop/for-all [a garbage b garbage c garbage d garbage e garbage]
               (let [calls [#(reads/parse-pattern a b c) #(reads/parse-point a b c) #(reads/check-layer a b c)
                            #(reads/index-writes hints a b c d) #(reads/purge-writes a b c d e)
                            #(reads/put-page-writes hints a b) #(reads/sweep-page-writes hints a b c d)
                            #(reads/implied hints a [{:name b :stamp c :rows d :opens e}])
                            #(reads/entry-moments a) #(reads/entry-facts a b) #(reads/fingerprint a)
                            #(reads/index-op a) #(open-row a b c d e) #(reads/visible? a b) #(reads/purge-fid a)
                            #(reads/tail-state {:prefix "x"} a 5) #(reads/page-entries a)
                            #(reads/point-row a b c d) #(reads/shown-row a b)
                            #(reads/pattern-answer a 5 {:pattern b} [] c)]]
                 (every? (fn [f] (try (f) true (catch Throwable _ false))) calls)))
             :seed 20260926)]
      (is (:pass? r) (pr-str (select-keys r [:fail :shrunk]))))))

(def gen-offer
  "Offers the gate would parse: one layer, one to six facts over a few
  entities and keys (the store's read keys among them), values of assorted
  EDN shapes."
  (gen/let [facts (gen/vector (gen/hash-map :e (gen/elements [:e0 :e1 :alice :read-x])
                                            :k (gen/elements [:note :mention :read/pattern :read/point :tag])
                                            :v (gen/one-of [gen/string gen/large-integer gen/keyword-ns
                                                            (gen/return nil) (gen/vector gen/small-integer)
                                                            (gen/map gen/keyword gen/string-alphanumeric {:max-elements 3})
                                                            (gen/hash-map :moment (gen/hash-map :stamp gen/large-integer))]))
                              1 6)]
    (-> {:version 1 :who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
         :stood-on {} :subjects #{} :facts facts}
        (assoc :name (nm)))))

(deftest generated-offers
  (testing "index-writes has no error on any sealed offer the gate would admit; replay-equal; one value entry per note (wave 1: sealed at the door, texts from the gate's opening)"
    (let [settings {:kind :personal :owner :alice :class :by-layer :grain :per-value}
          persons {:alice {:lock (locks/fresh-lock) :erased-at nil}}
          prow {[:alice :alice :alice] {:granted [(nm) 0]}}
          r (tc/quick-check
             300
             (prop/for-all [raw gen-offer]
               (let [[sealed Ks] (seal-raw (assoc raw :session :s1))
                     p (env/parse sealed)]
                 (or (contains? p :refuse)
                     (let [o (:ok p)
                           lx (lx-for o settings Ks persons)
                           d (gate/decide o settings prow {} 0 1000 (env/digest o) lx)
                           rows (:log d)
                           texts (gate/plain-texts rows (:values (:read lx)))
                           w (reads/index-writes hints :alice (:name o) rows 12345 texts)]
                       (or (not= :yes (get-in d [:record :answer]))
                           (and (not (:index-error w))
                                (= (bytes->vec w) (bytes->vec (reads/index-writes hints :alice (:name o) rows 12345 texts)))
                                (= (* 2 (count rows)) (count (filter #(#{:ix-ek :ix-ke} (first %)) (:index-put w))))
                                (= (count (filter (fn [[f t]] (and (= :note (:k f)) (some? t))) (map vector (:facts raw) texts)))
                                   (count (:index-of w)))
                                (every? #(nil? (:v %)) (filter #(some? (:sealed %)) rows))
                                (every? #(or (nil? %) (= % (env/encode-value (env/decode-value %)))) texts)))))))
             :seed 20260926)]
      (is (:pass? r) (pr-str (select-keys r [:fail :shrunk]))))))
