;; IMPORTANT: Before modifying this file, re-read PLAN-revision-reader.md.
(ns rig.revision-test
  "The revision reader's tests, 1 to 25 of PLAN-revision-reader.md section
  11, each deftest named by its number. The oracles are git's own output and
  Clojure's own reader, run here and never the code under test: `git show`
  (a different command from the reader's cat-file), `git rev-parse`, `git
  diff --numstat` from the empty tree, and the read loop with section 7's
  bindings; the line reading the invariants use is this namespace's own.
  The fixtures are commits of this repository, read from the worktree the
  tests run in (F7). Every result is read through `through`, which checks
  tests 19 and 20 on it. No Rama namespace is loaded and no cluster starts.

  Run from the rig folder: clojure -M:test rig.revision-test"
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [rig.revision :as rev])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io StringReader]))

;; ------------------------------------------------------------------ fixtures

(def rig "The rig folder, repository-relative." "src/proposal/rig-2026-09-25/")

(defn- clean-env
  "The environment the oracles run git in, as the reader's (F5): the JVM's
  minus every variable whose name starts with GIT_."
  []
  (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_"))) (System/getenv)))

(defn- git
  "Runs git as an oracle: {:exit :out :err}, stdout as bytes when asked."
  [args & {:keys [bytes?]}]
  (apply shell/sh "git" (concat args [:env (clean-env) :dir nil]
                                (when bytes? [:out-enc :bytes]))))

(def fx
  "Every fixture (section 11), full ids and repository-relative paths.
  `:repo` is the toplevel of the worktree the tests run in, from git
  rev-parse --show-toplevel run once from the rig folder (F7); every
  worktree of this repository shares one object store, so each commit reads
  the same bytes from any of them. `ce6ebaeb` and `d9e619b5` are ancestors
  of main; the rest stay reachable while the rig branch, or a merge that
  keeps its history, does. If it is ever squash-merged and deleted, re-point
  them here."
  (let [main-ce6 "ce6ebaeb01bbc8ea33d9d6ef25c47f9d641a1ee7"
        main-d9e "d9e619b575fb00140d56ad196f3da2520fb815f1"
        rig-e53  "e53bc6d01a44cfb8a4967703cf2dc82446f88c76"
        rig-7a7  "7a7403bd7850539e58c37da0fd0936bf46214f9b"
        rig-456  "45627e45d412b2c4c1b1224594823adc47e59d99"
        rig-ea5  "ea52c424515ae06b5d283da82cef8296a320f825"]
    {:repo     (str/trim (:out (git ["rev-parse" "--show-toplevel"])))
     :readme   {:commit main-ce6 :path "docs/builds/inland/README.md"}
     :symlink  {:commit main-ce6 :path "AGENTS.md"}
     :png      {:commit main-ce6
                :path   (str "history/docs/below-the-waist/path-kind/production/"
                             "receipts/png/gpu-path-production-empty-clip.png")}
     :no-rig   {:commit main-ce6 :path (str rig "RIG.md")}
     :geometry {:commit main-d9e :path "src-inland/softland/inland/geometry.cljc"}
     :rig-1    {:commit rig-e53 :path (str rig "RIG.md")}
     :rig-2    {:commit rig-7a7 :path (str rig "RIG.md")}
     :src      {:commit rig-7a7 :path (str rig "src")}
     :deps     {:commit rig-7a7 :path (str rig "deps.edn")}
     :gate-1   {:commit rig-456 :path (str rig "src/rig/store/gate.clj")}
     :gate-2   {:commit rig-ea5 :path (str rig "src/rig/store/gate.clj")}
     :envelope {:commit rig-ea5 :path (str rig "src/rig/store/envelope.clj")}}))

(def text-fixtures [:readme :rig-1 :rig-2 :gate-1 :gate-2 :envelope :geometry :deps])

(def clojure-fixtures [:gate-1 :gate-2 :envelope :geometry :deps])

(def empty-tree "The empty tree's id, the numstat oracle's base."
  "4b825dc642cb6eb9a060e54bf8d69288fbee4904")

;; ------------------------------------------------------------------ oracles

(defn- show-bytes
  "git show <commit>:<path>, the bytes of a fixture."
  [{:keys [commit path]}]
  (let [r (git ["-C" (:repo fx) "show" "--no-textconv" (str commit ":" path)] :bytes? true)]
    (assert (zero? (:exit r)) (str "git show failed: " (:err r)))
    (:out r)))

(defn- show [f] (String. ^bytes (show-bytes f) "UTF-8"))

(defn- rev-parse [rev]
  (str/trim (:out (git ["-C" (:repo fx) "rev-parse" (str rev "^{commit}")]))))

(defn- numstat
  "git diff --numstat from the empty tree: [added deleted], \"-\" for binary."
  [{:keys [commit path]}]
  (->> (git ["-C" (:repo fx) "diff" "--numstat" "--no-ext-diff" "--no-textconv"
             empty-tree commit "--" path])
       :out str/split-lines first (#(str/split % #"\t")) (take 2) vec))

(def ^:private permissive
  "A resolver that resolves every alias to itself, so ::alias/k reads."
  (reify clojure.lang.LispReader$Resolver
    (currentNS [_] 'user)
    (resolveClass [_ s] s)
    (resolveAlias [_ s] s)
    (resolveVar [_ s] s)))

(defn- with-plan-reader
  "Calls f under section 7's reader bindings."
  [f]
  (binding [*read-eval*              false
            *reader-resolver*        permissive
            *default-data-reader-fn* tagged-literal]
    (f)))

(def ^:private read-opts {:eof ::eof :read-cond :preserve})

(defn- reader-forms
  "Clojure's reader over a whole text: each top-level datum's start line from
  its :line metadata (only lists carry it; nil otherwise) and its end line
  from the reader's line number after the read."
  [^String text]
  (with-plan-reader
    #(let [rdr (LineNumberingPushbackReader. (StringReader. text))]
       (loop [acc []]
         (let [x (read read-opts rdr)]
           (if (identical? ::eof x)
             acc
             (recur (conj acc {:line (:line (meta x)) :end (.getLineNumber rdr)}))))))))

(defn- reads-as-one-datum? [^String s]
  (with-plan-reader
    #(let [rdr (LineNumberingPushbackReader. (StringReader. s))
           a   (read read-opts rdr)
           b   (read read-opts rdr)]
       (and (not (identical? ::eof a)) (identical? ::eof b)))))

(defn- reads-as-nothing? [^String s]
  (with-plan-reader
    #(identical? ::eof (read read-opts (LineNumberingPushbackReader. (StringReader. s))))))

(defn- line-spans
  "A text's lines as [start content-end] char pairs, by section 3's
  convention, computed here from the text alone: a line ends at \"\\n\", a
  \"\\r\" directly before it is part of its terminator, a final line without
  one counts, an empty text has none."
  [^String text]
  (if (= "" text)
    []
    (let [parts (str/split text #"\n" -1)
          lines (if (str/ends-with? text "\n") (vec (butlast parts)) parts)
          n     (count lines)]
      (loop [k 0, pos 0, acc []]
        (if (= k n)
          acc
          (let [p   ^String (nth lines k)
                len (count p)
                nl? (or (< (inc k) n) (str/ends-with? text "\n"))
                end (if (and nl? (str/ends-with? p "\r")) (+ pos len -1) (+ pos len))]
            (recur (inc k) (+ pos len 1) (conj acc [pos end]))))))))

(defn- line-of-char
  "The 1-based line holding char c, by line-spans."
  [spans c]
  (count (take-while #(<= (first %) c) spans)))

(defn- fence-line? [s] (boolean (re-find #"^\s*(```|~~~)" s)))

(defn- item-line? [s] (boolean (re-find #"^[ \t]*([-*+]|[0-9]{1,9}[a-z]?[.)])([ \t]|$)" s)))

(defn- block-invariant-failures
  "Section 6's invariants over one text's blocks, by this namespace's own
  line reading: nil when all hold, else the failures. Blocks are in file
  order and do not overlap; every non-blank line is in exactly one block;
  every block begins and ends on a non-blank line; a blank line is inside a
  block only when the block opened with a fence or a list item; content and
  chars are the block's lines exactly, and no unit ends in \"\\r\"."
  [^String text units]
  (let [ls        (line-spans text)
        n         (count ls)
        content   (fn [k] (let [[s e] (nth ls (dec k))] (subs text s e)))
        blank?    (fn [k] (str/blank? (content k)))
        ranges    (mapv #(-> % :position :lines) units)
        in-range? (fn [[a b]] (and (integer? a) (integer? b) (<= 1 a b n)))
        cover     (frequencies (mapcat (fn [[a b]] (range a (inc b))) (filter in-range? ranges)))]
    (seq
     (concat
      (for [r ranges :when (not (in-range? r))] [:out-of-range r])
      (for [[[_ b] [c _]] (partition 2 1 ranges) :when (not (< b c))] [:order-or-overlap b c])
      (for [k (range 1 (inc n)) :when (and (not (blank? k)) (not= 1 (cover k 0)))]
        [:non-blank-line-not-in-exactly-one-block k])
      (for [[a b :as r] ranges :when (in-range? r) :when (or (blank? a) (blank? b))]
        [:block-begins-or-ends-blank r])
      (for [[a b :as r] ranges :when (in-range? r)
            k (range a (inc b))
            :when (and (blank? k) (not (fence-line? (content a))) (not (item-line? (content a))))]
        [:blank-line-outside-fence-or-item r k])
      (for [u units
            :let [[s e] (-> u :position :chars) [a b :as r] (-> u :position :lines)]
            :when (in-range? r)
            :when (not (and (= s (first (nth ls (dec a))))
                            (= e (second (nth ls (dec b))))
                            (= (:content u) (subs text s e))))]
        [:position r])
      (for [u units :when (str/ends-with? (:content u) "\r")]
        [:ends-in-cr (-> u :position :lines)])))))

;; ------------------------------------------------------------------ the helper every test reads through

(def ^:private contract-keys
  #{:rev :commit :path :cut :units :unit :text :content :position :lines :chars})

(defn- all-keys [x]
  (cond (map? x)  (concat (keys x) (mapcat all-keys (vals x)))
        (coll? x) (mapcat all-keys x)
        :else     nil))

(defn- two-naturals? [v]
  (and (vector? v) (= 2 (count v)) (every? #(and (integer? %) (not (neg? %))) v)))

(defn- unit-shaped? [u]
  (and (map? u)
       (= #{:content :position} (set (keys u)))
       (string? (:content u))
       (map? (:position u))
       (= #{:lines :chars} (set (keys (:position u))))
       (two-naturals? (-> u :position :lines))
       (two-naturals? (-> u :position :chars))))

(defn- identity-free?
  "Test 20's walk over one successful result: every key anywhere is a
  contract key; every unit is exactly content and position, each position
  exactly lines and chars, two non-negative integers each; :commit is only
  at the top; the top holds no map but its units."
  [r]
  (boolean
   (and (map? r)
        (every? contract-keys (all-keys r))
        (every? unit-shaped? (concat (:units r) (when (contains? r :unit) [(:unit r)])))
        (not-any? #{:commit} (all-keys (dissoc r :commit)))
        (not-any? map? (vals (dissoc r :units :unit))))))

(defn- through
  "The helper every test reads through: no result is :internal (test 19),
  and every successful one passes test 20's walk."
  [r]
  (is (not= :internal (:error r)) (str "an :internal result: " (:detail r)))
  (when-not (contains? r :error)
    (is (identity-free? r) (str "outside the contract: " (pr-str (distinct (all-keys r))))))
  r)

(defn- units-of
  ([f] (units-of f {}))
  ([f opts] (through (rev/read-units (:repo fx) (:commit f) (:path f) opts))))

(defn- text-of [f]
  (through (#'rev/read-text "git" (:repo fx) (:commit f) (:path f) {})))

(defn- blocks [text] (through (rev/blocks text)))
(defn- forms [text] (through (rev/forms text)))
(defn- span [text a b] (through (rev/span text a b)))

(defn- lines-of [r] (mapv #(-> % :position :lines) (:units r)))
(defn- block-lines [text] (lines-of (blocks text)))
(defn- form-contents [text] (mapv :content (:units (forms text))))
(defn- line-count [text] (:line-count (rev/span text 1 Long/MAX_VALUE)))

;; ------------------------------------------------------------------ A. faithful to git

(deftest t01-read-text-is-git-show
  (doseq [k text-fixtures]
    (testing (name k)
      (let [f (fx k), r (text-of f), bs ^bytes (show-bytes f)]
        (is (= (String. bs "UTF-8") (:text r)) "git show's bytes, decoded")
        (is (= (rev-parse (:commit f)) (:commit r)) "rev-parse's commit")
        (is (= [(:commit f) (:path f)] [(:rev r) (:path r)]) "rev and path as given")
        (is (= 10 (last bs)) "the fixture ends in a newline")
        (is (= (count (filter #(= 10 %) bs)) (line-count (:text r)))
            "git's newline count is the reader's line count")))))

(defn- byte-start-of-line
  "The byte offset where line k starts in git show's bytes."
  [^bytes bs k]
  (->> bs (keep-indexed (fn [i b] (when (= 10 b) (inc i)))) (cons 0) (drop (dec k)) first))

(deftest t02-positions-are-git-show-coordinates
  (doseq [k text-fixtures]
    (let [f    (fx k)
          text (show f)
          gl   (str/split text #"\n" -1)
          ls   (line-spans text)
          line-start (fn [a] (+ (reduce + (map count (take (dec a) gl))) (dec a)))]
      (doseq [cut (if (some #{k} clojure-fixtures) [:blocks :forms] [:blocks])]
        (testing (str (name k) " as " (name cut))
          (let [r (units-of f {:cut cut})]
            (is (= cut (:cut r)))
            (is (seq (:units r)))
            (doseq [u (:units r)
                    :let [[s e] (-> u :position :chars) [a b] (-> u :position :lines)]]
              (is (= (:content u) (subs text s e)) "subs text start end is the content")
              (is (= a (line-of-char ls s)) "the first line is the first char's")
              (if (= :blocks cut)
                (do (is (= (:content u) (str/join "\n" (subvec gl (dec a) b)))
                        "git show's lines first to last, joined by newlines")
                    (is (= s (line-start a)) "earlier lines' chars plus one per newline"))
                (is (= b (line-of-char ls (dec e))) "the last line is the last char's"))))))
      (testing (str (name k) " as spans")
        (let [n (count ls)]
          (doseq [[a b] [[1 1] [1 n] [n n] [(quot n 2) (min n (+ 3 (quot n 2)))]]
                  :let [u (:unit (span text a b))]]
            (is (= (:content u) (str/join "\n" (subvec gl (dec a) b))))
            (is (= (line-start a) (-> u :position :chars first))))))))
  (testing "chars are not bytes: the README fixture"
    (let [f     (fx :readme)
          bs    (show-bytes f)
          by    (into {} (map (juxt #(-> % :position :lines) identity)) (:units (units-of f)))
          start #(-> (by %) :position :chars first)]
      (is (= 34 (start [3 6])))
      (is (= 36 (byte-start-of-line bs 3)))
      (is (= 2597 (start [56 60])))
      (is (= 2601 (byte-start-of-line bs 56)))))
  (testing "read-span through git agrees with the block it covers"
    (let [f (fx :readme)
          r (through (rev/read-span (:repo fx) (:commit f) (:path f) 44 45))]
      (is (= {:lines [44 45] :chars [1732 1878]} (-> r :unit :position)))
      (is (= (subs (show f) 1732 1878) (-> r :unit :content))))))

(def golden-readme
  "Section 6's golden cut of the README fixture, by line range."
  [[1 1] [3 6] [8 10] [12 12] [14 14] [16 18] [20 23] [25 29] [31 34] [36 40]
   [42 42] [44 45] [46 48] [49 51] [52 55] [56 60] [62 63] [65 65] [67 68]
   [70 74] [76 79] [81 85] [87 92] [94 96]])

(deftest t03-the-golden-block-cut
  (let [r  (units-of (fx :readme))
        by (into {} (map (juxt #(-> % :position :lines) identity)) (:units r))]
    (is (= :blocks (:cut r)) "a .md path defaults to blocks")
    (is (= 24 (count (:units r))))
    (is (= golden-readme (lines-of r)))
    (doseq [[lines chars] [[[1 1] [0 32]] [[3 6] [34 328]] [[16 18] [624 647]]
                           [[44 45] [1732 1878]] [[56 60] [2597 2973]] [[94 96] [4426 4696]]]]
      (is (= chars (-> (by lines) :position :chars)) (str lines)))
    (is (= "# Inland — run and use the build" (:content (by [1 1]))))
    (is (= "```sh\nbin/inland up\n```" (:content (by [16 18]))))
    (is (str/starts-with? (:content (by [44 45])) "1. Point at Orb and Ring."))))

(deftest t04-block-invariants-on-every-fixture
  (doseq [k text-fixtures]
    (testing (name k)
      (is (nil? (block-invariant-failures (show (fx k)) (:units (units-of (fx k) {:cut :blocks}))))))))

;; ------------------------------------------------------------------ B. faithful to Clojure's reader

(def form-table
  "Section 7's table, checked against Clojure's reader when the plan was cut."
  {:gate-1   {:count 23 :first [[2 10] [12 15] [17 20] [24 24]] :last [229 237]}
   :gate-2   {:count 28 :first [[2 14] [16 19] [21 24] [26 30]] :last [298 307]}
   :envelope {:count 51 :first [[2 18]] :last [377 382]}
   :geometry {:count 11 :first [[1 7]] :last [39 53]}
   :deps     {:count 1 :ends-at 19}})

(defn- gaps
  "The text outside the units: before the first, between each two, after the last."
  [^String text units]
  (let [bounds (concat [0] (mapcat #(-> % :position :chars) units) [(count text)])]
    (map (fn [[s e]] (subs text s e)) (partition 2 bounds))))

(deftest t05-forms-agree-with-clojures-reader
  (doseq [k clojure-fixtures]
    (testing (name k)
      (let [f      (fx k)
            text   (show f)
            r      (units-of f)
            units  (:units r)
            lines  (lines-of r)
            oracle (reader-forms text)
            table  (form-table k)]
        (is (= :forms (:cut r)) "the extension picks forms")
        (is (= (count oracle) (count units)) "the reader's form count")
        (doseq [[[a b] o] (map vector lines oracle)]
          (when (:line o) (is (= (:line o) a) "the first line against the reader's :line"))
          (is (= (:end o) b) "the last line against the reader's line number after the read"))
        (is (= (:count table) (count units)) "section 7's count")
        (when-let [firsts (:first table)] (is (= firsts (vec (take (count firsts) lines)))))
        (when-let [lst (:last table)] (is (= lst (peek lines))))
        (when-let [e (:ends-at table)]
          (is (= e (second (peek lines))))
          (is (= (line-of-char (line-spans text) (str/index-of text "{")) (first (peek lines)))
              "the one map starts at its brace, after the comments"))
        (doseq [u units]
          (is (reads-as-one-datum? (:content u)) (str "one datum, then the end: " (-> u :position :lines))))
        (doseq [g (gaps text units)]
          (is (reads-as-nothing? g) (str "reads as nothing: " (pr-str (subs g 0 (min 60 (count g)))))))))))

;; ------------------------------------------------------------------ C. two revisions

(deftest t06-a-passage-at-two-revisions
  (let [heading "## Phase 0: the two Rama claims, and the cross-module read"
        r1      (units-of (fx :rig-1))
        r2      (units-of (fx :rig-2))
        holding (fn [r] (mapv #(-> % :position :lines) (filter #(= heading (:content %)) (:units r))))]
    (is (not= (:commit r1) (:commit r2)))
    (is (= (:path r1) (:path r2)))
    (is (= [[29 29]] (holding r1)) "a one-line block at line 29 in the first")
    (is (= [[209 209]] (holding r2)) "and at line 209 in the second")
    (testing "both reads are faithful to git"
      (doseq [[k r] [[:rig-1 r1] [:rig-2 r2]] :let [text (show (fx k))]]
        (is (nil? (block-invariant-failures text (:units r))))
        (is (every? #(= (:content %) (apply subs text (-> % :position :chars))) (:units r)))))
    (testing "the outputs claim no link: only this test's content match finds the heading in each (F13)"
      (is (= #{:rev :commit :path :cut :units} (set (keys r1)) (set (keys r2))))
      (is (every? #(= #{:content :position} (set (keys %))) (concat (:units r1) (:units r2)))))))

(deftest t07-a-function-at-two-revisions
  (let [u1      (:units (units-of (fx :gate-1)))
        u2      (:units (units-of (fx :gate-2)))
        holding (fn [units s] (filter #(str/includes? (:content %) s) units))
        lines   (fn [us] (mapv #(-> % :position :lines) us))]
    (testing "perm-entity: the same four lines, moved"
      (is (= [[17 20]] (lines (holding u1 "(defn perm-entity"))))
      (is (= [[21 24]] (lines (holding u2 "(defn perm-entity"))))
      (is (= (map :content (holding u1 "(defn perm-entity")) (map :content (holding u2 "(defn perm-entity")))))
    (testing "refusal: changed"
      (is (= [[101 114]] (lines (holding u1 "(defn refusal"))))
      (is (= [[125 141]] (lines (holding u2 "(defn refusal"))))
      (is (not= (map :content (holding u1 "(defn refusal")) (map :content (holding u2 "(defn refusal")))))
    (testing "intake: only in the second"
      (is (empty? (holding u1 "(defn intake")))
      (is (= 1 (count (holding u2 "(defn intake")))))))

(deftest t08-a-moving-revision-and-repeated-reads
  (let [path    (str rig "deps.edn")
        head    (rev-parse "HEAD")
        by-head (through (rev/read-units (:repo fx) "HEAD" path))
        by-id   (through (rev/read-units (:repo fx) head path))]
    (is (= head (:commit by-head)) "HEAD's commit as rev-parse gives it at test time")
    (is (= "HEAD" (:rev by-head)) "the revision as given")
    (is (= (dissoc by-head :rev) (dissoc by-id :rev)) "equal to the read by that id, but for :rev")
    (is (= (units-of (fx :readme)) (units-of (fx :readme))) "two reads of one commit and path are equal")
    (is (= (through (rev/read-span (:repo fx) "HEAD" path 1 3))
           (assoc (through (rev/read-span (:repo fx) head path 1 3)) :rev "HEAD"))))
  (testing "a short id resolves to the full commit"
    (is (= (:commit (fx :rig-1)) (:commit (units-of (assoc (fx :rig-1) :commit "e53bc6d0"))))))
  (testing "a repo named by a directory inside it reads repository-relative paths (--full-tree)"
    (let [r (through (rev/read-units (str (:repo fx) "/" rig) (:commit (fx :readme)) (:path (fx :readme))))]
      (is (= golden-readme (lines-of r))))))

;; ------------------------------------------------------------------ D. every error path

(defn- bad?
  "A :bad-argument error naming `argument`, with its :value."
  [r argument]
  (and (= :bad-argument (:error r)) (= argument (:argument r)) (contains? r :value)))

(deftest t09-bad-arguments
  (let [repo (:repo fx)
        {c :commit p :path} (fx :readme)
        ru   (fn [& args] (through (apply rev/read-units args)))
        rs   (fn [& args] (through (apply rev/read-span args)))]
    (testing "rev"
      (doseq [v ["" nil "-p" "--output=x" "a\nb" "a\rb" "a\u0000b" :HEAD]]
        (let [r (ru repo v p)]
          (is (bad? r :rev) (pr-str v))
          (is (= {:repo repo :rev v :path p} (select-keys r [:repo :rev :path])) "the inputs as given")
          (is (not (contains? r :commit)) "refused before git runs"))))
    (testing "path"
      (doseq [v ["" "/etc/hosts" "../x" "a/./b" "a//b" (str rig "src/") "a\u0000b" 42]]
        (is (bad? (ru repo c v) :path) (pr-str v))))
    (testing "repo"
      (doseq [v ["" "  " nil]]
        (is (bad? (ru v c p) :repo) (pr-str v))))
    (testing "opts and its keys"
      (is (bad? (ru repo c p []) :opts))
      (is (bad? (ru repo c p nil) :opts))
      (doseq [v [0 -1 1.5 "10" nil]]
        (is (bad? (ru repo c p {:limit v}) :limit) (pr-str v)))
      (is (bad? (ru repo c p {:cut :nope}) :cut))
      (is (bad? (ru repo c p {:cut nil}) :cut)))
    (testing "span lines, refused before git runs"
      (doseq [[a b] [[0 1] [5 4] [nil 1] [1.5 2] [1 "2"]]]
        (let [r (rs repo c p a b)]
          (is (bad? r :lines) (pr-str [a b]))
          (is (= [a b] (:value r)))
          (is (not (contains? r :commit))))))
    (testing "a span past the end, after the read"
      (let [r (rs repo c p 1 97)]
        (is (bad? r :lines))
        (is (= 96 (:line-count r)) "the README has 96 lines")
        (is (= c (:commit r)))))
    (testing "the pure functions"
      (is (bad? (blocks nil) :text))
      (is (bad? (forms nil) :text))
      (is (bad? (forms 42) :text))
      (is (bad? (span nil 1 1) :text))
      (doseq [[a b] [[0 1] [2 1] [nil 1] [1.5 2]]]
        (let [r (span "a\nb\n" a b)]
          (is (bad? r :lines) (pr-str [a b]))
          (is (= 2 (:line-count r))))))
    (testing "a non-ASCII rev or path while the JVM's encoding is not UTF-8 (F14)"
      (let [check #'rev/input-error]
        (is (= {:error :bad-argument :argument :path :value "é.md" :encoding "ANSI_X3.4-1968"}
               (check repo c "é.md" {} "ANSI_X3.4-1968")))
        (is (= [:rev "ANSI_X3.4-1968"] ((juxt :argument :encoding) (check repo "é" p {} "ANSI_X3.4-1968"))))
        (is (nil? (check repo c "é.md" {} "UTF-8")) "UTF-8 lets it through")
        (is (nil? (check repo c "é.md" {} "utf8")) "by charset, not by spelling")
        (is (nil? (check repo c p {} "ANSI_X3.4-1968")) "ASCII passes any encoding")
        (is (= :path (:argument (check repo c "é.md" {} nil))) "an unknown encoding is not UTF-8"))
      (let [enc (System/getProperty "sun.jnu.encoding")
            r   (ru repo c "é.md")]
        (if (#'rev/utf-8-encoding? enc)
          (is (= :missing-path (:error r)) (str "under " enc " the path reaches git, and is not there"))
          (is (= [:path enc] ((juxt :argument :encoding) r)) (str "under " enc " it is refused")))))))

(deftest t10-not-a-repository
  (doseq [repo ["/nonexistent-rig-reader" "/proc"]]
    (let [r (through (rev/read-units repo (:commit (fx :readme)) (:path (fx :readme))))]
      (is (= :not-a-repository (:error r)) repo)
      (is (and (string? (:detail r)) (not (str/blank? (:detail r)))) "git's stderr says which")))
  (is (not (.exists (io/file "/nonexistent-rig-reader"))) "nothing was written"))

(deftest t11-unknown-revision
  (let [repo (:repo fx), p (:path (fx :readme))]
    (doseq [v ["no-such-rev-2026" (apply str (repeat 40 "0")) "e53bc6d0^{tree}"
               "^HEAD" "ce6ebaeb..7a7403bd"]]
      (let [r (through (rev/read-units repo v p))]
        (is (= :unknown-revision (:error r)) v)
        (is (not (contains? r :detail)) "git exited 0 or 1: no :detail")
        (is (not (contains? r :commit)))))
    (let [r (through (rev/read-units repo "HEAD@{99999}" p))]
      (is (= :unknown-revision (:error r)) "git exits 128 inside a good repository (F1)")
      (is (string? (:detail r)) "with :detail"))))

(deftest t12-missing-path
  (let [r (units-of (fx :no-rig))]
    (is (= :missing-path (:error r)) "RIG.md at the commit before the rig existed")
    (is (= (:commit (fx :no-rig)) (:commit r))))
  (is (= :missing-path (:error (units-of {:commit (:commit (fx :rig-2)) :path (str rig "RIG.md/x")})))
      "a path below a file")
  (is (= :missing-path (:error (units-of {:commit (:commit (fx :readme)) :path ":(top)AGENTS.md"})))
      "pathspec magic is not applied"))

(deftest t13-not-a-file
  (is (= [:not-a-file :directory] ((juxt :error :entry) (units-of (fx :src)))))
  (is (= [:not-a-file :symlink] ((juxt :error :entry) (units-of (fx :symlink)))))
  (let [parse #'rev/tree-entry
        id    (apply str (repeat 40 "a"))]
    (is (= {:error :not-a-file :entry :submodule}
           (parse (str "160000 commit " id "       -\tvendor/x\u0000") "vendor/x"))
        "a submodule, through the record parser")
    (is (= {:oid id :size 4701} (parse (str "100644 blob " id "    4701\tdocs/x.md\u0000") "docs/x.md")))
    (is (= {:oid id :size 9} (parse (str "100755 blob " id "       9\tbin/x\u0000") "bin/x")))
    (is (= {:error :missing-path} (parse (str "100644 blob " id "      12\tvendor/xy\u0000") "vendor/x"))
        "only the exact path matches")
    (is (= {:error :missing-path} (parse "" "a")) "no records")))

(deftest t14-too-large
  (let [f (fx :rig-2)]
    (is (= {:error :too-large :size 26033 :limit 1000}
           (select-keys (units-of f {:limit 1000}) [:error :size :limit])))
    (is (= :too-large (:error (units-of f {:limit 26032}))))
    (is (= :blocks (:cut (units-of f {:limit 26033}))) "the budget is inclusive")))

(deftest t15-binary
  (is (= :binary (:error (units-of (fx :png)))))
  (is (= ["-" "-"] (numstat (fx :png))) "git calls the PNG binary")
  (is (= ["96" "0"] (numstat (fx :readme))) "and the README text"))

(deftest t16-not-utf-8
  (let [decode #'rev/bytes->text
        bs     (fn [& xs] (byte-array (map unchecked-byte xs)))
        a      (int \a)]
    (is (= {:error :not-utf-8} (decode (bs 0x63 0x61 0x66 0xE9 0x0A)))
        "Latin-1 \"café\": no NUL, so :not-utf-8, not :binary")
    (is (= {:error :not-utf-8} (decode (bs 0x61 0xC0 0x80))) "an overlong encoding, no NUL")
    (is (= {:error :binary} (decode (bs 0x61 0x00 0x62))))
    (is (= {:error :binary} (decode (byte-array (concat (repeat 7999 a) [0])))) "a NUL at byte 7,999")
    (is (= {:text (str (apply str (repeat 8000 "a")) "\u0000")}
           (decode (byte-array (concat (repeat 8000 a) [0]))))
        "a NUL at byte 8,000 is past git's window, and valid UTF-8")
    (is (= {:text "\uFEFF# T"} (decode (bs 0xEF 0xBB 0xBF 0x23 0x20 0x54))) "a byte order mark stays")
    (is (= {:text "—"} (decode (bs 0xE2 0x80 0x94))) "three bytes, one char")))

(deftest t17-unreadable
  (doseq [[text reason at] [["(defn f [x]" :unclosed {:line 1 :char 0}]
                            ["(a]" :mismatched-close {:line 1 :char 2}]
                            [")" :unexpected-close {:line 1 :char 0}]
                            ["\"abc" :unclosed-string {:line 1 :char 0}]
                            ["#\"abc" :unclosed-string {:line 1 :char 1}]
                            ["'" :unclosed {:line 1 :char 0}]
                            ["^:m" :unclosed {:line 1 :char 0}]
                            ["#_" :unclosed {:line 1 :char 0}]
                            ["\\" :unclosed {:line 1 :char 0}]
                            ["# x" :bad-dispatch {:line 1 :char 0}]
                            ["#<x>" :bad-dispatch {:line 1 :char 0}]
                            ["(ok)\n#" :bad-dispatch {:line 2 :char 5}]
                            ["(a)\n  (b" :unclosed {:line 2 :char 6}]
                            ["' )" :unexpected-close {:line 1 :char 2}]
                            ["(a \\" :unclosed {:line 1 :char 0}]]]
    (is (= {:error :unreadable :reason reason :at at} (forms text)) (pr-str text)))
  (testing "through a read: the README read as forms, with the read's keys"
    (let [f (fx :readme)
          r (units-of f {:cut :forms})]
      (is (= {:error :unreadable :reason :bad-dispatch :at {:line 1 :char 0}
              :repo (:repo fx) :rev (:commit f) :path (:path f) :commit (:commit f)}
             r)))))

(deftest t18-git-failed
  (let [f (fx :readme)
        r (through (#'rev/read-text "/nonexistent/git" (:repo fx) (:commit f) (:path f) {}))]
    (is (= :git-failed (:error r)))
    (is (string? (:detail r))))
  (testing "the executable is not an option (F4): :git in opts opens no door"
    (is (= golden-readme (lines-of (units-of (fx :readme) {:git "/nonexistent/git"})))))
  (testing "a non-zero exit where the step expects none"
    (let [repo (:repo fx), zeros (apply str (repeat 40 "0"))]
      (is (= [:git-failed 128] ((juxt :error :exit) (#'rev/find-entry "git" repo zeros "x")))
          "step 2 on a commit the store lacks")
      (is (= [:git-failed 128] ((juxt :error :exit) (#'rev/read-blob "git" repo zeros)))
          "step 4 on a blob the store lacks")
      (is (= [:git-failed 2] ((juxt :error :exit) (#'rev/resolve-commit "ls" repo "HEAD")))
          "step 1 when the program exits 2, which is neither 0, 1 nor 128 (GNU ls refuses --verify)")))
  (testing "git runs without inherited GIT_ variables (F5)"
    (is (= {"PATH" "/usr/bin" "MY_GIT_X" "k"}
           (#'rev/git-env {"GIT_DIR" "/x" "GIT_WORK_TREE" "/y" "PATH" "/usr/bin" "MY_GIT_X" "k"})))
    (let [child  (set (map #(first (str/split % #"=" 2))
                           (str/split-lines (:out (#'rev/run-git "env" [] false)))))
          parent (set (keys (System/getenv)))]
      (is (contains? child "PATH") "the child sees the rest of the environment")
      (is (empty? (filter #(str/starts-with? % "GIT_") child))
          (str "no GIT_ variable reaches the child; the JVM has "
               (pr-str (filter #(str/starts-with? % "GIT_") parent)))))))

(deftest t19-no-internal-anywhere
  (testing "the helper every test reads through asserts it; odd inputs here too"
    (through (rev/forms (apply str (repeat 100000 "("))))
    (through (rev/forms (str (apply str (repeat 20000 "[")) (apply str (repeat 20000 "]")))))
    (through (rev/blocks (apply str (repeat 2000 "- a\n  ```\n\n"))))
    (through (rev/blocks "\r\r\n\r"))
    (through (rev/span "\r" 1 1))
    (through (rev/read-units (:repo fx) (:commit (fx :readme)) (:path (fx :readme)) {:limit (bigint 1e30)})))
  (testing "the last resort is data"
    (let [r (#'rev/internal (Exception. "boom"))]
      (is (= :internal (:error r)))
      (is (str/starts-with? (:detail r) "java.lang.Exception: boom"))))
  (testing "cut-for is total"
    (is (= [:forms :forms :forms :forms :forms :blocks :blocks :blocks :blocks]
           (mapv rev/cut-for ["a.clj" "a.cljc" "a.cljs" "a.edn" "A/B.CLJ" "a.md" "a.clj.md" 42 nil])))))

;; ------------------------------------------------------------------ E. no identity

(deftest t20-no-identity
  (let [f       (fx :readme)
        results {:read-units-blocks (units-of f)
                 :read-units-forms  (units-of (fx :gate-2))
                 :read-span         (through (rev/read-span (:repo fx) (:commit f) (:path f) 44 45))
                 :read-text         (text-of (fx :deps))
                 :blocks            (blocks "# a\n\ntext")
                 :forms             (forms "(a) b")
                 :span              (span "a\nb" 1 2)}
        top     {:read-units-blocks #{:rev :commit :path :cut :units}
                 :read-units-forms  #{:rev :commit :path :cut :units}
                 :read-span         #{:rev :commit :path :unit}
                 :read-text         #{:rev :commit :path :text}
                 :blocks            #{:units}
                 :forms             #{:units}
                 :span              #{:unit}}]
    (doseq [[k r] results]
      (testing (name k)
        (is (not (contains? r :error)))
        (is (identity-free? r))
        (is (= (top k) (set (keys r))) "exactly these keys at the top")
        (when (:commit r) (is (re-matches #"[0-9a-f]{40}" (:commit r)) "the snapshot's commit"))))
    (testing "the walk is not vacuous: it refuses any other key or shape"
      (let [u  {:content "x" :position {:lines [1 1] :chars [0 1]}}
            ok {:rev "HEAD" :commit "abc" :path "p" :cut :blocks :units [u]}]
        (is (identity-free? ok))
        (doseq [b [(assoc ok :id 1) (assoc ok :hash "h") (assoc ok :sha "s") (assoc ok :index 0)
                   (assoc ok :name "f") (assoc ok :kind :defn) (assoc ok :meta {:a 1})
                   (assoc-in ok [:units 0 :name] "f") (assoc-in ok [:units 0 :commit] "abc")
                   (assoc-in ok [:units 0 :position :kind] :defn)
                   (assoc-in ok [:units 0 :position :lines] [1 1 1])
                   (assoc-in ok [:units 0 :position :chars] [-1 1])
                   (assoc-in ok [:units 0 :position :chars] [0 1.0])
                   (update-in ok [:units 0] dissoc :content)]]
          (is (not (identity-free? b)) (pr-str b)))))))

;; ------------------------------------------------------------------ F. rules on literal strings

(deftest t21-block-rules
  (doseq [[label text expected]
          [["a fence holding blank lines and a # line is one block"
            "para\n\n```\ncode\n\n# not a heading\n```\nafter" [[1 1] [3 7] [8 8]]]
           ["an unclosed fence runs to the last non-blank line" "```\na\n\nb\n\n" [[1 4]]]
           ["a tilde fence closes on a longer run of its own char only"
            "~~~\n```\n~~~~\nnext" [[1 3] [4 4]]]
           ["an item keeps an indented paragraph after a blank line" "- item\n\n  more" [[1 3]]]
           ["and ends at one that is not indented" "- item\n\nnot indented" [[1 1] [3 3]]]
           ["2026. wrapped in a paragraph does not split it" "The year\n2026. Then more" [[1 2]]]
           ["1. directly under a paragraph line does" "A label\n1. First" [[1 1] [2 2]]]
           ["a bullet interrupts a paragraph, 1) does, 3) does not"
            "p\n- x\n\nq\n1) y\n\nr\n3) z" [[1 1] [2 2] [4 4] [5 5] [7 8]]]
           ["7b. after item 7 starts a block" "7. seven\n7b. seven b\n8. eight" [[1 1] [2 2] [3 3]]]
           ["any number starts a new item under an item" "1. a\n2026. b" [[1 1] [2 2]]]
           ["a nested item stays in its parent (F8)" "- a\n  - b\n    - c\n- d" [[1 3] [4 4]]]
           ["a bullet indented deeper than a paragraph's first line stays in it" "p\n  - x" [[1 2]]]
           ["a heading directly under a paragraph line splits it" "text\n# Heading\nmore" [[1 1] [2 2] [3 3]]]
           ["a heading inside an item ends it" "- a\n# H\n- b" [[1 1] [2 2] [3 3]]]
           ["headings: up to three spaces, one to six #, then a space, a tab or the end"
            "p\n   # three\np\n    # four\np\n####### seven\np\n#tag\np\n#\np\n#\tt"
            [[1 1] [2 2] [3 9] [10 10] [11 11] [12 12]]]
           ["a lazy continuation directly below an item" "1. a\nlazy line\n2. b" [[1 2] [3 3]]]
           ["a fence inside an item, indented to its text, stays in it"
            "- a\n  ```\n  x\n\n  ```\n- b" [[1 5] [6 6]]]
           ["a fence under an item, less indented, ends it" "1. a\n```\nx\n```" [[1 1] [2 4]]]
           ["a fence ends a paragraph and is its own block" "p\n```\nx\n```\nq" [[1 1] [2 4] [5 5]]]
           ["a spaced thematic break reads as an item (F8)" "para\n* * *" [[1 1] [2 2]]]
           ["an unspaced one, like a setext underline, is paragraph text" "para\n***\nTitle\n===" [[1 4]]]
           ["a tab-indented continuation" "- a\n\n\tmore\n1. b\n\n\tmore" [[1 3] [4 6]]]
           ["a byte order mark before # Title makes that line paragraph text (G8)"
            "\uFEFF# Title\nnext" [[1 2]]]]]
    (testing label
      (is (= expected (block-lines text)))
      (is (nil? (block-invariant-failures text (:units (blocks text)))))))
  (testing "CRLF: blank lines still end blocks, content keeps an inner \\r\\n, no unit ends in \\r"
    (let [text "a\r\nb\r\n\r\nc\r\n"
          r    (blocks text)]
      (is (= [[1 2] [4 4]] (lines-of r)))
      (is (= ["a\r\nb" "c"] (mapv :content (:units r))))
      (is (= [[0 4] [8 9]] (mapv #(-> % :position :chars) (:units r))))
      (is (nil? (block-invariant-failures text (:units r))))))
  (testing "an empty text and a blank text have no blocks"
    (is (= [] (block-lines "")))
    (is (= [] (block-lines "\n \n\t\n")))))

(deftest t22-form-rules
  (doseq [[label text expected]
          [["two forms on one line" "(a) (b)" ["(a)" "(b)"]]
           ["two forms touching" "(a)(b)" ["(a)" "(b)"]]
           ["a comment holding ) and \" is outside every form" "; ) \" ]\n(a)" ["(a)"]]
           ["and inside a form is part of it" "(a ; ) \"\n b)" ["(a ; ) \"\n b)"]]
           ["character literals" "(f \\( \\) \\; \\\" \\\\ \\newline \\space \\u00e9)"
            ["(f \\( \\) \\; \\\" \\\\ \\newline \\space \\u00e9)"]]
           ["character literals at the top" "\\( \\) \\newline\\space \\\\\\a"
            ["\\(" "\\)" "\\newline" "\\space" "\\\\" "\\a"]]
           ["a string holding (; and an escaped quote" "\"(;\\\"\" x" ["\"(;\\\"\"" "x"]]
           ["a regex holding ) and an escaped quote" "#\"[)\\\"]\" y" ["#\"[)\\\"]\"" "y"]]
           ["sets, fn literals, symbolic values" "#{} #() ##Inf ##-Inf" ["#{}" "#()" "##Inf" "##-Inf"]]
           ["tags" "#inst \"2026-09-26\" #foo/bar {} #js[1]" ["#inst \"2026-09-26\"" "#foo/bar {}" "#js[1]"]]
           ["metadata starts the form" "^:private x ^{:a 1} y #^:a z ^:a ^:b w"
            ["^:private x" "^{:a 1} y" "#^:a z" "^:a ^:b w"]]
           ["metadata on the line above" "^:private\n(defn f [])" ["^:private\n(defn f [])"]]
           ["quote and its kin" "'a `b ~c ~@d @e #'f #=(g)" ["'a" "`b" "~c" "~@d" "@e" "#'f" "#=(g)"]]
           ["reader conditionals at the top, one unit each" "#?(:clj a :cljs b) #?@(:clj [a])"
            ["#?(:clj a :cljs b)" "#?@(:clj [a])"]]
           ["namespaced maps" "#:a{:b 1} #::{:b 1} #::s{:b 1}" ["#:a{:b 1}" "#::{:b 1}" "#::s{:b 1}"]]
           ["namespaced keywords" "::alias/k :a/b ::k" ["::alias/k" ":a/b" "::k"]]
           ["a top-level discard is skipped" "#_ x" []]
           ["and two" "#_ #_ a b" []]
           ["and only its one datum" "#_ x y" ["y"]]
           ["a discard inside a form is kept" "(a #_ b c)" ["(a #_ b c)"]]
           ["a discard inside a prefixed form is kept" "^:m #_ x y '#_ a b" ["^:m #_ x y" "'#_ a b"]]
           ["#! on line 1 is a comment" "#!/usr/bin/env bb\n(a)" ["(a)"]]
           ["a comment above a form belongs to no unit; the docstring to its form"
            ";; about f\n(defn f \"doc (\" [])" ["(defn f \"doc (\" [])"]]
           ["commas are whitespace" "a,b ,, c" ["a" "b" "c"]]
           ["' # % continue a symbol" "foo' foo# a%b" ["foo'" "foo#" "a%b"]]
           ["a token ends at a terminating char" "a\"s\"b(c)" ["a" "\"s\"" "b" "(c)"]]
           ["an unknown tag still forms a unit (semantic errors pass)" "#nope 1" ["#nope 1"]]
           ["a string may span lines" "\"a\nb\" c" ["\"a\nb\"" "c"]]]]
    (testing label
      (is (= expected (form-contents text)))))
  (testing "positions: a unit's lines are those of its first and last chars"
    (is (= [{:lines [2 3] :chars [5 11]}]
           (mapv :position (:units (forms ";; c\n(a\n b) ; d\n"))))))
  (testing "nesting 10,000 deep: no stack overflow"
    (let [text (str (apply str (repeat 10000 "(")) (apply str (repeat 10000 ")")))]
      (is (= [{:lines [1 1] :chars [0 20000]}] (mapv :position (:units (forms text)))))))
  (testing "an astral char counts two chars, in a symbol and in a string"
    (is (= [{:content "a\uD83D\uDE00b" :position {:lines [1 1] :chars [0 4]}}
            {:content "\"\uD83D\uDE00\"" :position {:lines [1 1] :chars [5 9]}}]
           (:units (forms "a\uD83D\uDE00b \"\uD83D\uDE00\""))))))

(deftest t23-span-rules
  (let [text "a\nb\n\nc\n"]
    (is (= {:content "a" :position {:lines [1 1] :chars [0 1]}} (:unit (span text 1 1))) "the first line")
    (is (= {:content "c" :position {:lines [4 4] :chars [5 6]}} (:unit (span text 4 4))) "the last line")
    (is (= {:content "" :position {:lines [3 3] :chars [4 4]}} (:unit (span text 3 3))) "a blank line")
    (is (= {:content "a\nb\n\nc" :position {:lines [1 4] :chars [0 6]}} (:unit (span text 1 4)))
        "the whole file")
    (is (= 4 (:line-count (span text 1 5)))))
  (testing "a text without a final newline: its last line counts"
    (is (= {:content "b" :position {:lines [2 2] :chars [2 3]}} (:unit (span "a\nb" 2 2))))
    (is (= 2 (:line-count (span "a\nb" 1 3)))))
  (testing "an empty text: every span is :bad-argument"
    (doseq [[a b] [[1 1] [1 2] [0 0]]]
      (is (= {:error :bad-argument :argument :lines :value [a b] :line-count 0} (span "" a b)))))
  (testing "nothing trimmed; CRLF keeps its inner \\r\\n, not its last \\r"
    (is (= "  " (:content (:unit (span "  \n" 1 1)))))
    (is (= "a\r\nb" (:content (:unit (span "a\r\nb\r\n" 1 2)))))
    (is (= "\n" (:content (:unit (span "x\n\n\ny" 2 3)))) "two blank lines are one newline")))

;; ------------------------------------------------------------------ G. properties

(def ^:private block-line-kinds
  "Test 24's lines: blank, heading, text, bullet, ordered, lettered, indented
  and fence lines, and a few that read as none of them."
  ["" "  " "\t" "# Heading" "### Three" "   ## Indented heading" "####### seven" "#tag"
   "text line" "  indented text" "2026. Then" "trailing  " "> quote" "| a | b |" "---" "***"
   "- bullet" "* star" "+ plus" "  - nested" "    - deeper" "\t- tabbed" "-"
   "1. one" "2) two" "10. ten" "7b. lettered" "  3. indented ordered"
   "    indented code" "\tcode" "```" "```clj" "~~~" "````" "  ```" "   ~~~ x" "* * *" "- - -"
   "\uFEFFbom"])

(def ^:private block-texts
  (gen/let [lines  (gen/vector (gen/elements block-line-kinds) 0 40)
            sep    (gen/elements ["\n" "\r\n"])
            final? gen/boolean]
    (str (str/join sep lines) (when (and final? (seq lines)) sep))))

(defspec t24-blocks-keep-their-invariants 200
  (prop/for-all [text block-texts]
    (let [r (rev/blocks text)]
      (and (not (contains? r :error))
           (nil? (block-invariant-failures text (:units r)))))))

(def ^:private form-atoms
  "Test 22's atoms, each a whole datum."
  ["foo" "foo'" "foo#" "a.b/c" "->x" "%" "%1" "nil" ":k" ":a/b" "::alias/k" "42" "-1.5" "1/2"
   "\\(" "\\)" "\\;" "\\\"" "\\\\" "\\newline" "\\space" "\\u00e9"
   "\"s\"" "\"(;\\\"\"" "\"a\\\\\"" "\"a\nb\"" "#\"[)\\\"]\"" "##Inf" "##-Inf"
   "a\uD83D\uDE00b" "\"\uD83D\uDE00\""])

(def ^:private inner-seps
  "What separates data inside a form: whitespace, commas, comments holding
  delimiters and quotes."
  (gen/elements [" " "\n" ", " "\t" "\r\n" " ; ( \" ] }\n"]))

(defn- joined
  "Strings from `xs-gen` with a separator from `sep-gen` between each two."
  [xs-gen sep-gen]
  (gen/bind xs-gen
            (fn [xs]
              (gen/fmap #(apply str (interleave xs (concat % [""])))
                        (gen/vector sep-gen (max 0 (dec (count xs))))))))

(def ^:private datum
  "A random datum built from test 22's atoms and prefixes, nested to random
  depth. None starts with a bare #_, so each is exactly one datum at the
  top."
  (gen/recursive-gen
   (fn [inner]
     (let [element (gen/frequency [[6 inner] [1 (gen/fmap #(str "#_ " %) inner)]])
           coll    (fn [open close]
                     (gen/fmap #(str open % close) (joined (gen/vector element 0 4) inner-seps)))]
       (gen/one-of
        [(coll "(" ")") (coll "[" "]") (coll "{" "}") (coll "#{" "}") (coll "#(" ")")
         (gen/fmap (partial apply str)
                   (gen/tuple (gen/elements ["'" "`" "~" "~@" "@" "#'" "#="]) inner))
         (gen/fmap (fn [[m x]] (str m " " x))
                   (gen/tuple (gen/elements ["^:private" "^{:a 1}" "#^:a" "^:a ^:b" "^String"]) inner))
         (gen/fmap (fn [[p a b]] (str p "#_ " a " " b))
                   (gen/tuple (gen/elements ["'" "^:m "]) inner inner))
         (gen/fmap #(str "#?" %) (coll "(" ")"))
         (gen/fmap #(str "#?@" %) (coll "(" ")"))
         (gen/fmap #(str "#:a" %) (coll "{" "}"))
         (gen/fmap #(str "#::" %) (coll "{" "}"))
         (gen/fmap #(str "#foo/bar " %) inner)
         (gen/return "#inst \"2026-09-26\"")])))
   (gen/elements form-atoms)))

(def ^:private top-seps
  "What separates top-level data: whitespace, commas, comments holding
  delimiters and quotes, #! lines, and top-level discards."
  (gen/fmap (partial apply str)
            (gen/vector (gen/one-of [(gen/elements [" " "\n" "," "\t" "\r\n"
                                                    " ; a ( \" ] } comment\n" " #! x ( \" \n"])
                                     (gen/fmap #(str " #_ " % " ") datum)])
                        1 3)))

(defn- place
  "The text of `units` with `lead` before, `trail` after and `seps` between,
  and the [start end] chars each unit was placed at."
  [lead units seps trail]
  (let [sb (StringBuilder. ^String lead)]
    (loop [i 0, spans []]
      (if (= i (count units))
        {:text (str (.append sb ^String trail)) :spans spans :units units}
        (let [s (.length sb)
              e (.length (.append sb ^String (nth units i)))]
          (when (< (inc i) (count units)) (.append sb ^String (nth seps i)))
          (recur (inc i) (conj spans [s e])))))))

(def ^:private form-texts
  (gen/let [units (gen/vector datum 0 6)
            seps  (gen/vector top-seps (count units))
            lead  (gen/one-of [(gen/return "") top-seps])
            trail (gen/one-of [(gen/return "") top-seps])]
    (place lead units seps trail)))

(defspec t25-forms-give-the-placed-spans 200
  (prop/for-all [{:keys [text spans units]} form-texts]
    (let [r  (rev/forms text)
          ls (line-spans text)]
      (and (not (contains? r :error))
           (= spans (mapv #(-> % :position :chars) (:units r)))
           (= units (mapv :content (:units r)))
           (every? (fn [u]
                     (let [[s e] (-> u :position :chars)]
                       (= [(line-of-char ls s) (line-of-char ls (dec e))] (-> u :position :lines))))
                   (:units r))))))
