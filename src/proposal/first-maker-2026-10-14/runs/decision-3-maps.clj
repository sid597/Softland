;; A throwaway run of the decision-3 agent, 15 October 2026, at HEAD c90c7bcc.
;; Not code of the road. It drives the store's PURE gate functions
;; (door/declaration check, gate/decision places-needed, places, writes) over
;; act maps, with a map from place key to row standing in for the record. No
;; cluster runs, and the standing of a permission is not checked (every act
;; here is the system in the base, which the walk's acts 0 and 1 showed pass).
;; Every id, key and session is an instance value, not a pick.
;; Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/runs/decision-3-maps.clj

(require '[softland.store.act :as act]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.gate.decision :as decision]
         '[softland.store.record.rows :as rows]
         '[softland.store.read-path.query :as query]
         '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         '[clojure.set :as set])

;; ---------------------------------------------- the walk's helpers, copied
;; (from runs/walk-parse.clj 20-121, so as not to rerun it and rewrite its edn)

(defn git-show [rev path]
  (let [{:keys [exit out err]} (sh "git" "show" (str rev ":" path))]
    (when-not (zero? exit) (throw (ex-info err {:rev rev :path path})))
    out))
(defn git-blob [rev path] (str/trim (:out (sh "git" "rev-parse" (str rev ":" path)))))
(defn name-uuid [s] (java.util.UUID/nameUUIDFromBytes (.getBytes ^String s "UTF-8")))
(defn act-uuid [n] (java.util.UUID/fromString (format "a0000000-0000-4000-8000-%012d" n)))
(defn act-id [n] [:base :microbatch (act-uuid n)])

(defn top-forms [text]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. text))
        forms (binding [*read-eval* false]
                (loop [acc []]
                  (let [f (read {:eof ::eof :read-cond :allow} rdr)]
                    (if (= ::eof f) acc (recur (conj acc f))))))
        lines (str/split-lines text)
        starts (mapv #(:line (meta %)) forms)
        ends (conj (mapv dec (rest starts)) (count lines))]
    (mapv (fn [f s e] {:form f :line s :text (str/trimr (str/join "\n" (subvec lines (dec s) e)))})
          forms starts ends)))

(defn part-of [{:keys [form text line]}]
  (let [[kind nm & more] form
        doc (when (string? (first more)) (first more))]
    {:kind (keyword (name kind)) :name (name nm) :doc doc :text text :line line}))

(def system-act-parts {:version 1 :who :system :layer :base :permission [:system :base :base]})

(defn clj-facts
  "The walk's cut A: ids by namespace and name (end A of this page)."
  [path rev]
  (let [parts (map part-of (top-forms (git-show rev path)))
        [ns-part & defs] parts
        ns-id (keyword (:name ns-part))
        f (name-uuid path)]
    (into [{:e f :k :file/path :v path} {:e f :k :file/blob :v (git-blob rev path)}]
          (concat
           [{:e ns-id :k :code/in-file :v f} {:e ns-id :k :code/doc :v (:doc ns-part)}]
           (mapcat (fn [{:keys [kind name doc text]}]
                     (let [d (keyword (:name ns-part) name)]
                       [{:e d :k :code/in :v ns-id} {:e d :k :code/kind :v kind}
                        {:e d :k :code/doc :v doc} {:e d :k :code/text :v text}]))
                   defs)))))

(defn index-of-place [facts e k]
  (first (keep-indexed (fn [i f] (when (and (= e (:e f)) (= k (:k f))) i)) facts)))

(defn replacement-facts [before after before-act-n]
  (let [by-place (fn [fs] (into {} (map (fn [f] [[(:e f) (:k f)] (:v f)])) fs))
        b (by-place before) a (by-place after)
        replaces (fn [[e k]] [(act-id before-act-n) (index-of-place before e k)])]
    (vec (concat
          (for [f after :let [p [(:e f) (:k f)]] :when (and (contains? b p) (not= (b p) (:v f)))]
            (assoc f :replaces (replaces p)))
          (for [f before :let [p [(:e f) (:k f)]] :when (not (contains? a p))]
            {:e (:e f) :k (:k f) :replaces (replaces p)})
          (for [f after :let [p [(:e f) (:k f)]] :when (not (contains? b p))] f)))))

(defn counts [facts]
  {:replace (count (filter #(and (:replaces %) (some? (:v %))) facts))
   :retract (count (filter #(and (:replaces %) (nil? (:v %))) facts))
   :new     (count (remove :replaces facts))
   :total   (count facts)})

(defn arrived [a n] (assoc a :act-id (act-id n) :depot :microbatch declaration/time-part 1760500000000))
(defn word [r] (if (:ok r) :ok (:refuse r)))

;; ------------------------------------------------- a gate over pure functions

(defn gate
  "Decides act map `a` arriving as act n against `table` (place key -> row),
  with the store's pure checks: the door's declaration check, then the
  checks of the places, then the writes. Gives {:word :table}."
  [table a n stamp]
  (let [checked (declaration/check (arrived a n))]
    (if-not (:ok checked)
      {:word (:refuse checked) :table table}
      (let [arr (:ok checked)
            needed (decision/places-needed arr false)
            rows-read (into {} (map (fn [k] [k (get table k)])) needed)
            fault (decision/places :microbatch arr false rows-read)]
        (if fault
          {:word fault :table table :needed (count needed)}
          (let [ws (decision/writes (act-id n) arr nil stamp 1760500000000 (byte-array 32)
                                    {:since-by-place {} :place-rows {} :level-from stamp})]
            {:word :ok :needed (count needed)
             :table (reduce (fn [t [which k row]] (if (= :fact which) (assoc t k row) t)) table ws)}))))))

(defn act-of [facts session] (merge system-act-parts {:session session :facts facts}))

(def doc-place (rows/place-key :base :softland.store.gate.stamp/next-stamp :code/doc))
(defn by-act-of [table k] (when-let [r (get table k)] [(:by-act r) (:by-index r)]))

;; ---------------------------------------------------------------- the runs

(def stamp-path "src-store/softland/store/gate/stamp.clj")
(def before-facts (clj-facts stamp-path "bb6b6cc1"))
(def after-facts (clj-facts stamp-path "0e6182b9"))
(def act-1 (act-of before-facts :s1))
(def act-7 (act-of (replacement-facts before-facts after-facts 1) :s2))
(def doc-v (:v (first (filter #(and (= :softland.store.gate.stamp/next-stamp (:e %)) (= :code/doc (:k %))) before-facts))))
(def act-2 (act-of [{:e :softland.store.gate.stamp/next-stamp :k :code/doc :v (str doc-v " (edited)")}] :s3))

(println "RUN 1. A fact with no :replaces on a place that holds a fact (walk claim, decision.clj 233-247)")
(let [r1 (gate {} act-1 1 1)
      r2 (gate (:table r1) act-2 2 2)
      r7 (gate (:table r2) act-7 7 3)]
  (println "  act 1 (stamp.clj at bb6b6cc1, 36 facts):" (:word r1) "; next-stamp doc row written by" (by-act-of (:table r1) doc-place))
  (println "  act 2 (one fact on that place, no :replaces): places read" (:needed r2) "; word" (:word r2)
           "; row now written by" (by-act-of (:table r2) doc-place))
  (println "  act 7 after act 2 (names act 1's fact [u1 26]):" (:word r7)))

(println "RUN 2. The same replacement sent twice, with and without :replaces")
(let [r1 (gate {} act-1 1 1)
      r7 (gate (:table r1) act-7 7 2)
      r7b (gate (:table r7) act-7 70 3)
      blind (act-of (mapv #(dissoc % :replaces) (:facts act-7)) :s4)
      rb1 (gate (:table r1) blind 71 2)
      rb2 (gate (:table rb1) blind 72 3)]
  (println "  act 7 with :replaces: first" (:word r7) ", sent again" (:word r7b))
  (println "  act 7 with every :replaces taken out: first" (:word rb1) ", sent again" (:word rb2)))

(println "RUN 3. A retract with no :replaces on a place nothing ever wrote")
(let [r (gate {} (act-of [{:e :softland.store.gate.stamp/never-was :k :code/doc}] :s5) 3 1)
      k (rows/place-key :base :softland.store.gate.stamp/never-was :code/doc)]
  (println "  word" (:word r) "; the read of that place gives" (rows/read-value (get (:table r) k))))

(println "RUN 4. The real rename 085bca4a (softland.store.stamp -> softland.store.gate.stamp)")
(def old-path "src-store/softland/store/stamp.clj")
(def old-facts (clj-facts old-path "085bca4a^"))
(def new-facts (clj-facts stamp-path "085bca4a"))
(let [a8 (act-of (replacement-facts old-facts new-facts 80) :s6)]
  (println "  end A, ids by namespace and name:" (counts (:facts a8)) "; parse" (word (act/parse a8))
           "; door check" (word (declaration/check (arrived a8 81)))))
(let [defs (fn [rev path] (->> (top-forms (git-show rev path)) rest (map part-of)
                               (map (fn [p] [(:name p) (name-uuid (:text p))])) (into {})))
      o (defs "085bca4a^" old-path) n (defs "085bca4a" stamp-path)
      o2 (defs "bb6b6cc1" stamp-path) n2 (defs "0e6182b9" stamp-path)]
  (println "  end C, ids by content (a name-based uuid of the form's text): definitions whose id is the same across 085bca4a:"
           (count (filter #(= (o %) (n %)) (keys o))) "of" (count o))
  (println "  end C across 0e6182b9: definitions kept by name" (count (set/intersection (set (keys o2)) (set (keys n2))))
           "; of those, the same id" (count (filter #(= (o2 %) (n2 %)) (set/intersection (set (keys o2)) (set (keys n2)))))))
;; end E: the store's own id, with the name as a fact on it. Instance uuids.
(let [file-id #uuid "f0000000-0000-4000-8000-000000000001"
      ns-id   #uuid "f0000000-0000-4000-8000-000000000002"
      before  [{:e file-id :k :file/path :v old-path}
               {:e file-id :k :file/blob :v (git-blob "085bca4a^" old-path)}
               {:e ns-id :k :code/name :v "softland.store.stamp"}
               {:e ns-id :k :code/in-file :v file-id}]
      after   [{:e file-id :k :file/path :v stamp-path}
               {:e file-id :k :file/blob :v (git-blob "085bca4a" stamp-path)}
               {:e ns-id :k :code/name :v "softland.store.gate.stamp"}
               {:e ns-id :k :code/in-file :v file-id}]
      ae (act-of (replacement-facts before after 82) :s7)
      q {:asks [:points-at :code/name "softland.store.stamp"] :asker :system :layers [:base]}]
  (println "  end E, a store id with the name as a fact (each definition's :code/in names ns-id, unchanged):"
           (counts (:facts ae)) "; parse" (word (act/parse ae)) "; door check" (word (declaration/check (arrived ae 83))))
  (println "  end E, the road finds the entity by its old name: query check" (word (query/check q))
           "(needs :code/name marked; the base marks none, first_act.clj 148)"))
;; end D: the walk's act 8 plus one link fact
(let [link {:e :softland.store.gate.stamp/next-stamp :k :code/was :v :softland.store.stamp/next-stamp}
      a (act-of [link] :s8)]
  (println "  end D, a link fact under a key such as :code/was: parse" (word (act/parse a))
           "; door check" (word (declaration/check (arrived a 84)))))

(println "RUN 5. The same bytes at two paths (blob 7e6c32a9) under ids by content")
(let [h (name-uuid "7e6c32a9 bytes") f1 (name-uuid ".claude/skills/rama/references/unique-ids.md")
      f2 (name-uuid ".agents/skills/rama/references/unique-ids.md")
      two (act-of [{:e h :k :md/in-file :v f1} {:e h :k :md/in-file :v f2}] :s9)
      one (act-of [{:e h :k :md/in-files :v #{f1 f2}}] :s9)]
  (println "  two facts, one per path: door check" (word (declaration/check (arrived two 90))))
  (println "  one fact with a set of paths: door check" (word (declaration/check (arrived one 91)))))

(println "RUN 6. The system marks :code/name in the base, so that a part can be found by its name (end E)")
(let [mark (act-of [{:e :base :k :softland.store/marked-keys :v #{:code/name}}] :s10)
      r (gate {} mark 100 1)]
  (println "  parse" (word (act/parse mark)) "; door check" (word (declaration/check (arrived mark 100)))
           "; checks of the places" (:word r) "(standing not run)"))
