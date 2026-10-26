;; A throwaway run of the walk agent, 14-15 October 2026. Not code of the
;; road and not an adapter: it builds the act maps of the walk pages from the
;; real files in git, as instance values, and gives each to the act's pure
;; check (softland.store.act/parse). Some also go to the door's pure check
;; (softland.store.door.declaration/check) and the read path's query check.
;; Every id, key and session in here is an instance value: the decision that
;; owns it is named on the walk pages. Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/runs/walk-parse.clj
;; It writes runs/walk-act-maps.edn and prints a summary.

(require '[softland.store.act :as act]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.read-path.query :as query]
         '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def out-dir "src/proposal/first-maker-2026-10-14/runs/")

(defn git-show [rev path]
  (let [{:keys [exit out err]} (sh "git" "show" (str rev ":" path))]
    (when-not (zero? exit) (throw (ex-info err {:rev rev :path path})))
    out))

(defn git-blob [rev path]
  (str/trim (:out (sh "git" "rev-parse" (str rev ":" path)))))

(defn name-uuid
  "Instance value: a name-based UUID (version 3) of a text, such as a path."
  [s]
  (java.util.UUID/nameUUIDFromBytes (.getBytes ^String s "UTF-8")))

(defn act-uuid
  "Instance value: the uuid of the act-id that the door would give act n."
  [n]
  (java.util.UUID/fromString (format "a0000000-0000-4000-8000-%012d" n)))

(defn act-id [n] [:base :microbatch (act-uuid n)])

;; ------------------------------------------------------ reading a .clj file

(defn top-forms
  "The top-level forms of a Clojure text, each with its first line and its
  text (from its first line to the line before the next form)."
  [text]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. text))
        forms (binding [*read-eval* false]
                (loop [acc []]
                  (let [f (read {:eof ::eof :read-cond :allow} rdr)]
                    (if (= ::eof f) acc (recur (conj acc f))))))
        lines (str/split-lines text)
        starts (mapv #(:line (meta %)) forms)
        ends (conj (mapv dec (rest starts)) (count lines))]
    (mapv (fn [f s e]
            {:form f :line s
             :text (str/trimr (str/join "\n" (subvec lines (dec s) e)))})
          forms starts ends)))

(defn part-of
  "One part of a .clj file: its kind, name, docstring and text."
  [{:keys [form text line]}]
  (let [[kind nm & more] form
        doc (when (string? (first more)) (first more))]
    {:kind (keyword (name kind)) :name (name nm) :doc doc :text text :line line}))

;; ---------------------------------------------- the cut chosen as an instance

(def system-act-parts
  "Instance values of decision 4: the system acts in the base."
  {:version 1 :who :system :layer :base :permission [:system :base :base]})

(defn file-facts [path rev]
  (let [f (name-uuid path)]
    [{:e f :k :file/path :v path}
     {:e f :k :file/blob :v (git-blob rev path)}]))

(defn clj-facts
  "Instance cut A for a .clj file: the file, the namespace and each
  definition are entities. Ids: the file a name-based UUID of its path; the
  namespace and each definition a readable keyword of their names. Keys:
  readable keywords. Each definition: :code/in, :code/kind, :code/doc,
  :code/text."
  [path rev]
  (let [parts (map part-of (top-forms (git-show rev path)))
        [ns-part & defs] parts
        ns-id (keyword (:name ns-part))
        f (name-uuid path)]
    (into (file-facts path rev)
          (concat
           [{:e ns-id :k :code/in-file :v f}
            {:e ns-id :k :code/doc :v (:doc ns-part)}]
           (mapcat (fn [{:keys [kind name doc text]}]
                     (let [d (keyword (:name ns-part) name)]
                       [{:e d :k :code/in :v ns-id}
                        {:e d :k :code/kind :v kind}
                        {:e d :k :code/doc :v doc}
                        {:e d :k :code/text :v text}]))
                   defs)))))

(defn index-of-place
  "The index in an act's facts of the fact on [e k]."
  [facts e k]
  (first (keep-indexed (fn [i f] (when (and (= e (:e f)) (= k (:k f))) i)) facts)))

(defn replacement-facts
  "The facts that take the before-facts to the after-facts: a fact with a
  new value replaces the old (with :replaces), a place that is gone is
  retracted (no :v, with :replaces), a new place is a new fact."
  [before after before-act-n]
  (let [by-place (fn [fs] (into {} (map (fn [f] [[(:e f) (:k f)] (:v f)])) fs))
        b (by-place before) a (by-place after)
        replaces (fn [[e k]] [(act-id before-act-n) (index-of-place before e k)])]
    (vec
     (concat
      (for [f after :let [p [(:e f) (:k f)]] :when (and (contains? b p) (not= (b p) (:v f)))]
        (assoc f :replaces (replaces p)))
      (for [f before :let [p [(:e f) (:k f)]] :when (not (contains? a p))]
        {:e (:e f) :k (:k f) :replaces (replaces p)})
      (for [f after :let [p [(:e f) (:k f)]] :when (not (contains? b p))]
        f)))))

;; ------------------------------------------------- reading the README section

(def readme "src-store/README.md")
(def section-anchor
  "The section's anchor as the README's own link writes it (README.md line 41)."
  "decisions-of-14-october-2026--order-and-time-apart")

(defn section-lines [rev from to]
  (subvec (str/split-lines (git-show rev readme)) (dec from) to))

(defn cites [text open close]
  (into (sorted-set) (map second) (re-seq (re-pattern (str open "([0-9a-f]{8})" close)) text)))

(defn md-facts
  "Instance cut A for the section: the file, the section and each numbered
  entry are entities, each a name-based UUID of path#anchor[/number]. The
  sub-bullets and the quoted lines stay inside an entry's text; the record
  ids that an entry quotes are facts on it, Sid's typed lines apart from a
  session's texts."
  [rev]
  (let [lines (section-lines rev 984 1029)
        heading (subs (first lines) 3)
        entry-starts (keep-indexed (fn [i l] (when (re-find #"^\d+\. \*\*" l) i)) lines)
        intro (str/trim (str/join "\n" (subvec lines 1 (first entry-starts))))
        f (name-uuid readme)
        s (name-uuid (str readme "#" section-anchor))
        entries (map (fn [st en] (str/trimr (str/join "\n" (subvec lines st en))))
                     entry-starts (concat (rest entry-starts) [(count lines)]))]
    (into (file-facts readme rev)
          (concat
           [{:e s :k :md/in-file :v f}
            {:e s :k :md/heading :v heading}
            {:e s :k :md/level :v 2}
            {:e s :k :md/text :v intro}]
           (mapcat (fn [n text]
                     (let [e (name-uuid (str readme "#" section-anchor "/" n))]
                       [{:e e :k :md/in :v s}
                        {:e e :k :md/number :v n}
                        {:e e :k :md/title :v (second (re-find #"\*\*(.+?)\*\*" (str/replace text #"\n\s*" " ")))}
                        {:e e :k :md/text :v text}
                        {:e e :k :md/cites-sid :v (cites text "‹" "›")}
                        {:e e :k :md/cites-session :v (cites text "«" "»")}]))
                   (iterate inc 1) entries)))))

;; ------------------------------------------------------------ the act maps

(def stamp-path "src-store/softland/store/gate/stamp.clj")
(def before-facts (clj-facts stamp-path "bb6b6cc1"))
(def after-facts (clj-facts stamp-path "0e6182b9"))
(def readme-facts (md-facts "0e6182b9"))
(def next-stamp-doc (:v (first (filter #(and (= :softland.store.gate.stamp/next-stamp (:e %)) (= :code/doc (:k %))) before-facts))))

(def entry-3-index (index-of-place readme-facts (name-uuid (str readme "#" section-anchor "/3")) :md/text))
(def entry-3-text (:v (nth readme-facts entry-3-index)))

(def unique-ids ".claude/skills/rama/references/unique-ids.md")
(def unique-ids-lines (str/split-lines (git-show "c90c7bcc" unique-ids)))
(defn lines-text [from to] (str/join "\n" (subvec unique-ids-lines (dec from) to)))

(def key-as-uuid #uuid "6b65792d-0000-4000-8000-00000000d0c0")

(def acts
  [{:n 0 :page "walk-1" :step 3
    :note "Act 0: the making act of the base, in the form making-act? names (first_act.clj 81-122). Needed only if the store the road writes to has no base yet. Instance values: :session (decision 4)."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e :base :k :softland.store/depot :v :microbatch}
                                          {:e :system :k :softland.store/permission :v [:system :base :base]}]})
    :arrive-as 0}
   {:n 1 :page "walk-1" :step 3
    :note "Act 1: stamp.clj as it was at bb6b6cc1 (the parent of 0e6182b9), instance cut A: file, namespace and eight definitions as entities; readable-keyword keys. Instance values: every id and key (decision 2), :session (decision 4)."
    :act (merge system-act-parts {:session :walk-instance-session :facts before-facts})
    :arrive-as 1}
   {:n 2 :page "walk-1" :step 3
    :note "K5 side A: next-stamp's docstring under a readable-keyword key. The same fact as act 1 holds, alone."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e :softland.store.gate.stamp/next-stamp :k :code/doc :v next-stamp-doc}]})}
   {:n 3 :page "walk-1" :step 3
    :note "K5 side B, a key that is an id as a UUID: the key's word and grammar are facts about it. The key is a UUID; parse is expected to refuse, since a key is a readable keyword only (act.clj leaves, :key)."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e key-as-uuid :k :key/word :v "doc"}
                                          {:e key-as-uuid :k :key/grammar :v :string}
                                          {:e :softland.store.gate.stamp/next-stamp :k key-as-uuid :v next-stamp-doc}]})}
   {:n 4 :page "walk-1" :step 3
    :note "K5 side B, a key that is an id as an opaque readable keyword: the same three facts with the key :k-6f1c0d. Its word and grammar still need keys of their own (:key/word, :key/grammar)."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e :k-6f1c0d :k :key/word :v "doc"}
                                          {:e :k-6f1c0d :k :key/grammar :v :string}
                                          {:e :softland.store.gate.stamp/next-stamp :k :k-6f1c0d :v next-stamp-doc}]})}
   {:n 5 :page "walk-1" :step 2
    :note "The cut not chosen for the file's id: the path itself as a keyword. Its name holds '/', which a readable keyword cannot."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e (keyword stamp-path) :k :file/blob :v (git-blob "bb6b6cc1" stamp-path)}]})}
   {:n 6 :page "walk-1" :step 2
    :note "Code as data, not as text: next-stamp's arglist and body as Clojure data (a vector of symbols; a list). Symbols and lists are no values of the form (act.clj value?)."
    :act (merge system-act-parts {:session :walk-instance-session
                                  :facts [{:e :softland.store.gate.stamp/next-stamp :k :code/arglist :v '[last-stamp]}
                                          {:e :softland.store.gate.stamp/next-stamp :k :code/body :v '(if last-stamp (unchecked-inc (long last-stamp)) 0)}]})}
   {:n 7 :page "walk-1" :step 6
    :note "Act 7: the replacement act for the real change at 0e6182b9 against act 1: changed values replace their fact (:replaces [act-id-of-act-1 index]); the places of the seven deleted definitions are retracted (no :v, with :replaces). Instance: identity by namespace and name (decision 3, not picked)."
    :act (merge system-act-parts {:session :walk-instance-session-2
                                  :facts (replacement-facts before-facts after-facts 1)})
    :arrive-as 7}
   {:n 8 :page "walk-1" :step 7
    :note "Instance (hypothetical, had the road run on 8 October): the real rename of 085bca4a, softland.store.stamp -> softland.store.gate.stamp, under identity by namespace and name: next-stamp's four places retracted under the old id and written under the new; :based-on carries the old fact ids as what the act came from (stamps are instance values)."
    :act (merge system-act-parts
                {:session :walk-instance-session-3
                 :based-on {[(act-id 80) 26] 5 [(act-id 80) 27] 5}
                 :facts [{:e :softland.store.stamp/next-stamp :k :code/in :replaces [(act-id 80) 24]}
                         {:e :softland.store.stamp/next-stamp :k :code/kind :replaces [(act-id 80) 25]}
                         {:e :softland.store.stamp/next-stamp :k :code/doc :replaces [(act-id 80) 26]}
                         {:e :softland.store.stamp/next-stamp :k :code/text :replaces [(act-id 80) 27]}
                         {:e :softland.store.gate.stamp/next-stamp :k :code/in :v :softland.store.gate.stamp}
                         {:e :softland.store.gate.stamp/next-stamp :k :code/kind :v :defn}
                         {:e :softland.store.gate.stamp/next-stamp :k :code/doc :v next-stamp-doc}
                         {:e :softland.store.gate.stamp/next-stamp :k :code/text :v "(defn next-stamp ...)"}]})}
   {:n 9 :page "walk-2" :step 3
    :note "Act 9: the section 'Decisions of 14 October 2026' at 0e6182b9 (= HEAD), instance cut A: file, section and five entries as entities with name-based UUIDs of path#anchor[/number]; readable-keyword keys."
    :act (merge system-act-parts {:session :walk-instance-session-2 :facts readme-facts})
    :arrive-as 9}
   {:n 10 :page "walk-2" :step 3
    :note "K5 side B for an entry: entry 3's text under a key that is an opaque readable keyword, with its word and grammar as facts."
    :act (merge system-act-parts {:session :walk-instance-session-2
                                  :facts [{:e :k-7a2e11 :k :key/word :v "text"}
                                          {:e :k-7a2e11 :k :key/grammar :v :string}
                                          {:e (name-uuid (str readme "#" section-anchor "/3")) :k :k-7a2e11 :v entry-3-text}]})}
   {:n 11 :page "walk-2" :step 6
    :note "Act 11, the added instance of a change (hypothetical): a later decision adds a sentence to entry 3, the way 0e6182b9 added one to three earlier entries (README.md 183-184, 333-335, 887-888). The added sentence is a placeholder, not Sid's words."
    :act (merge system-act-parts {:session :walk-instance-session-3
                                  :facts [{:e (name-uuid (str readme "#" section-anchor "/3")) :k :md/text
                                           :v (str entry-3-text " Changed on <a later day>: <what changed> (entry <n> of that day).")
                                           :replaces [(act-id 9) entry-3-index]}]})
    :arrive-as 11}
   {:n 12 :page "walk-2" :step 2
    :note "The cut not chosen: the whole README as one value on the file. 72,062 bytes at HEAD. The value is elided in this file; the run used the whole text from git."
    :act (merge system-act-parts {:session :walk-instance-session-2
                                  :facts [{:e (name-uuid readme) :k :file/text :v (git-show "c90c7bcc" readme)}]})
    :arrive-as 12 :elide true}
   {:n 13 :page "walk-2" :step 7
    :note "Instance (real file): .claude/skills/rama/references/unique-ids.md has '### Key points' at lines 67 and 93, under two different '##' parents. With a section id made of the bare heading, both sections are one entity and one place."
    :act (merge system-act-parts {:session :walk-instance-session-2
                                  :facts [{:e (name-uuid (str unique-ids "#key-points")) :k :md/text :v (lines-text 67 73)}
                                          {:e (name-uuid (str unique-ids "#key-points")) :k :md/text :v (lines-text 93 99)}]})
    :arrive-as 13}])

(defn arrived [act n]
  (assoc act :act-id (act-id n) :depot :microbatch declaration/time-part 1760500000000))

(defn word [result] (if (:ok result) :ok (:refuse result)))

(def results
  (mapv (fn [{:keys [act arrive-as] :as m}]
          (cond-> (assoc m :parse-word (word (act/parse act)))
            arrive-as (assoc :door-check-word (word (declaration/check (arrived act arrive-as))))))
        acts))

;; ---------------------------------------------------------------- queries

(def queries
  [{:n 1 :note "One place: next-stamp's docstring in the base, asked by the system."
    :query {:asks [:place :softland.store.gate.stamp/next-stamp :code/doc] :asker :system :layers [:base]}}
   {:n 2 :note "One entity, every place of next-stamp, with who wrote each fact and its time."
    :query {:asks [:entity :softland.store.gate.stamp/next-stamp] :asker :system :layers [:base] :with #{:writer :time}}}
   {:n 3 :note "One place under a key that is a UUID."
    :query {:asks [:place :softland.store.gate.stamp/next-stamp key-as-uuid] :asker :system :layers [:base]}}
   {:n 4 :note "One entry of the README section, by its instance id, with the permission."
    :query {:asks [:entity (name-uuid (str readme "#" section-anchor "/3"))] :not-signed-in true :layers [:base] :with #{:permission}}}])

(def query-results
  (mapv (fn [q] (assoc q :check-word (let [r (query/check (:query q))] (if (:ok r) :ok [(:refuse r) (:at r)]))))
        queries))

;; ----------------------- one act per commit: how many facts would 0e6182b9 need

(defn blocks
  "Approximate: the text of a file split at lines that begin with '(' (a
  .clj) or '#' (a .md), keyed by the block's first line."
  [text start]
  (let [lines (str/split-lines text)]
    (loop [ls lines cur nil acc {}]
      (if-let [l (first ls)]
        (if (str/starts-with? l start)
          (recur (rest ls) [l [l]] (if cur (assoc acc (first cur) (str/join "\n" (second cur))) acc))
          (recur (rest ls) (when cur [(first cur) (conj (second cur) l)]) acc))
        (if cur (assoc acc (first cur) (str/join "\n" (second cur))) acc)))))

(def commit-count
  (let [files (str/split-lines (:out (sh "git" "show" "--name-only" "--format=" "0e6182b9")))]
    (for [f files
          :let [start (cond (str/ends-with? f ".clj") "(" (str/ends-with? f ".md") "#" :else nil)]
          :when start
          :let [b (blocks (git-show "bb6b6cc1" f) start)
                a (blocks (git-show "0e6182b9" f) start)
                changed (count (filter #(and (contains? b %) (not= (b %) (a %))) (keys a)))
                added (count (remove #(contains? b %) (keys a)))
                removed (count (remove #(contains? a %) (keys b)))]]
      {:file f :changed changed :added added :removed removed
       :facts-cut-a (if (= start "(") (+ (* 2 changed) (* 4 added) (* 4 removed))
                        (+ (* 1 changed) (* 4 added) (* 4 removed)))})))

;; ---------------------------------------------------------------- output

(defn for-file [m]
  (cond-> (dissoc m :elide :arrive-as)
    (:elide m) (update-in [:act :facts] (fn [fs] (mapv #(assoc % :v "<elided: the whole of src-store/README.md at c90c7bcc, 72,062 bytes; walk-parse.clj builds it from git>") fs)))))

(spit (str out-dir "walk-act-maps.edn")
      (with-out-str
        (println ";; The act maps of the walk pages (walk-1-stamp.md, walk-2-readme.md), each with the")
        (println ";; word that softland.store.act/parse gave, and for some the word of the door's check")
        (println ";; (softland.store.door.declaration/check) on the act as it would arrive: act-id")
        (println ";; [:base :microbatch <instance uuid>], depot :microbatch, a fixed time.")
        (println ";; Made by runs/walk-parse.clj on the :store alias, 14-15 October 2026. Every id, key,")
        (println ";; session and act-id uuid is an instance value; the owning decision is named on the pages.")
        (binding [*print-length* nil *print-level* nil]
          (pp/pprint (mapv for-file results)))))

(println "ACT MAPS:" (count results))
(doseq [{:keys [n page step parse-word door-check-word act]} results]
  (println (format "  act %2d  %-6s step %d  facts %3d  parse %-12s door-check %s"
                   n page step (count (:facts act)) parse-word (or door-check-word "-"))))
(println "QUERIES:")
(doseq [{:keys [n check-word note]} query-results]
  (println (format "  query %d  check %-28s %s" n (pr-str check-word) note)))
(println "VALUE BYTES: stamp.clj before" (count (.getBytes (git-show "bb6b6cc1" stamp-path) "UTF-8"))
         "after" (count (.getBytes (git-show "0e6182b9" stamp-path) "UTF-8"))
         "README at HEAD" (count (.getBytes (git-show "c90c7bcc" readme) "UTF-8"))
         "max-value-bytes" declaration/max-value-bytes)
(println "ACT 7 FACTS: replaced" (count (filter #(and (:replaces %) (contains? % :v) (some? (:v %))) (get-in results [7 :act :facts])))
         "retracted" (count (filter #(and (:replaces %) (nil? (:v %))) (get-in results [7 :act :facts])))
         "new" (count (remove :replaces (get-in results [7 :act :facts]))))
(doseq [f (get-in results [7 :act :facts])]
  (println "   " (pr-str (:e f)) (:k f) (if (contains? f :v) (if (some? (:v f)) "replace" "retract") "retract") (pr-str (:replaces f))))
(println "ONE ACT PER COMMIT, 0e6182b9, approximate facts under cut A:"
         (reduce + (map :facts-cut-a commit-count)) "in" (count commit-count) "files of .clj and .md; max-facts" declaration/max-facts)
(doseq [c commit-count] (println "   " c))
