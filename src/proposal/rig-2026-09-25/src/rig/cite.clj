;; IMPORTANT: Before modifying this file, re-read PLAN-citation.md section 2.
(ns rig.cite
  "The citation that stays true (PLAN-citation.md, section 2): a `path:line`
  or `path:start-end` in a document, found, bound to the named form holding
  those lines at the revision its line was written, and marked true, moved
  or stale when the file's next reading lands. Three steps of the recipe
  vocabulary live here, each a pure function of its inputs apart from git:

  - `find-step` (`:cite/find`): a document at a revision into `:cite/found`
    facts, one a citation, and a `:cite/scanned` fact;
  - `bind-step` (`:cite/bind`): a found citation and its file's readings
    into `:cite/bound` or `:cite/unbound`, and a mark when bound late;
  - `check-step` (`:cite/check`): a file's new reading and what stood on the
    previous one into one `:cite/mark` a citation found, and a `:cite/walk`.

  A mark compares with the snapshot taken at binding, so a state is always
  \"since it was written\". Placeholders (PLAN-citation.md section 9): the
  citation forms (P-C6), written-at as the blame commit (P-C7), path
  resolution (P-C8), late binding (P-C9), what a stale citation shows
  (P-C10).

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key. Never
  throws: every step returns `{:facts ...}` or `{:error ...}`."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [rig.material :as m]
            [rig.store.envelope :as env]))

(set! *warn-on-reflection* true)

;; ------------------------------------------------------------------- git

(defn- git-env
  "The JVM's environment minus every GIT_ variable, as rig.revision runs git."
  []
  (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_"))) (System/getenv)))

(defn- git
  "git with an argument vector and no shell, in `repo`: stdout as text, or
  nil when git fails."
  [repo & args]
  (try
    (let [r (apply shell/sh "git" "-C" repo (concat args [:env (git-env) :out-enc "UTF-8"]))]
      (when (zero? (:exit r)) (:out r)))
    (catch Exception _ nil)))

(defn blame
  "The commit that last changed each line of `path` at `rev`, and each
  commit's committer time: `{:line->commit {n sha} :time {sha seconds}}`,
  from `git blame --porcelain`, or nil."
  [repo rev path]
  (when-let [out (git repo "blame" "--porcelain" rev "--" path)]
    (loop [ls (str/split-lines out) cur nil acc {:line->commit {} :time {}}]
      (if-let [l (first ls)]
        (if-let [[_ sha final] (re-matches #"([0-9a-f]{40}) \d+ (\d+)(?: \d+)?" l)]
          (recur (rest ls) sha (assoc-in acc [:line->commit (Long/parseLong final)] sha))
          (if-let [[_ t] (re-matches #"committer-time (\d+)" l)]
            (recur (rest ls) cur (assoc-in acc [:time cur] (Long/parseLong t)))
            (recur (rest ls) cur acc)))
        acc))))

(defn tree-paths
  "Every file path in the commit's tree, as a set, or nil."
  [repo commit]
  (when-let [out (git repo "ls-tree" "-r" "-z" "--name-only" "--full-tree" commit)]
    (into #{} (remove str/blank?) (str/split out #"\u0000"))))

(defn commit-before
  "The last commit reachable from `head` whose committer time is at or before
  `seconds`: `{:commit c}`, `{:commit nil}` when there is none, or nil when
  git fails."
  [repo head seconds]
  (when-let [out (git repo "rev-list" "-1" (str "--before=" seconds) head)]
    {:commit (not-empty (str/trim out))}))

(defn- git-failed!
  "Stop a step whose git read failed: the run fails and is retried on the
  next pass, instead of recording the failure as a fact about the document
  or the citation (review finding 6)."
  [what x]
  (throw (ex-info "git failed" {::git-failed what :at x})))

(defn commit-time
  "A commit's committer time in seconds, or nil."
  [repo commit]
  (some-> (git repo "show" "-s" "--format=%ct" commit) str/trim not-empty Long/parseLong))

;; ------------------------------------------------------------- finding

(def extensions
  "The extensions a cited path may end in (P-C6): source and text files, so
  a host and port such as `example.com:8080` is not a citation."
  ["clj" "cljc" "cljs" "edn" "md" "markdown" "txt" "java" "js" "mjs" "cjs" "ts" "tsx" "jsx"
   "json" "html" "css" "scss" "sh" "bash" "zsh" "py" "rb" "go" "rs" "c" "h" "cpp" "hpp"
   "kt" "scala" "sql" "yml" "yaml" "toml" "xml" "csv" "properties" "gradle" "log"])

(def ^:private cite-rx
  (re-pattern (str "(?<![A-Za-z0-9_./~+\\-])([A-Za-z0-9_~.][A-Za-z0-9_./~+\\-]*\\.(?:"
                   (str/join "|" (sort-by (comp - count) extensions))
                   "))`?:(\\d+)(?:\\s*[-–]\\s*(\\d+))?(?![\\d\\w])")))

(def ^:private continuation-rx
  #"^(\s*`?\s*(?:,|and|;|&)\s*`?\s*):(\d+)(?:\s*[-–]\s*(\d+))?(?![\d\w])")

(def ^:private bare-continuation-rx
  "A bare line number after a comma or `and`, when a delimiter follows it
  (`envelope.clj:18-56, 81-86, 367`), so prose such as `12, 3 tests` is not
  one (P-C6)."
  #"^(\s*`?\s*(?:,|and)\s*`?\s*)(\d+)(?:\s*[-–]\s*(\d+))?(?=\s*(?:[`,;.)\]]|and\b|$))")

(defn- range-of [a b] (let [a (Long/parseLong a)] [a (if b (Long/parseLong b) a)]))

(defn find-in-line
  "The citations in one line of text (P-C6): `path:N` and `path:N-M` (a
  hyphen or an en dash), the path in backticks with the colon outside
  (`` `path`:N ``), and continuations after a citation, which take its
  path: `:N` or `:N-M` after a comma, `and`, `;` or `&`, and a bare `N` or
  `N-M` after a comma or `and` when a delimiter follows it. Each `{:col
  :text :path-text :lines [a b] :inherited?}`, col 1-based."
  [^String line]
  (let [^java.util.regex.Matcher mt (re-matcher cite-rx line)]
    (loop [out []]
      (if (.find mt)
        (let [path (.group mt 1)
              primary {:col (inc (.start mt)) :text (.group mt 0) :path-text path
                       :lines (range-of (.group mt 2) (.group mt 3)) :inherited? false}
              conts (loop [pos (.end mt) acc []]
                      (if-let [[whole sepr a b] (or (re-find continuation-rx (subs line pos))
                                                    (re-find bare-continuation-rx (subs line pos)))]
                        (let [start (+ pos (count sepr))]
                          (recur (+ pos (count whole))
                                 (conj acc {:col (inc start) :text (subs line start (+ pos (count whole)))
                                            :path-text path :lines (range-of a b) :inherited? true})))
                        acc))]
          (recur (-> out (conj primary) (into conts))))
        out))))

(defn find-citations
  "Every citation in a text, in order: `find-in-line` over each line, each
  with its 1-based `:line`."
  [text]
  (vec (for [[i l] (map-indexed vector (str/split (str text) #"\r?\n" -1))
             c (find-in-line l)]
         (assoc c :line (inc i)))))

;; ----------------------------------------------------------- resolution

(defn target-kind
  "What a cited path names, by its extension: `:clojure` (the forms cut),
  `:markdown`, or `:other`."
  [path]
  (let [p (str/lower-case (str path))]
    (cond
      (some #(str/ends-with? p %) [".clj" ".cljc" ".cljs" ".edn"]) :clojure
      (some #(str/ends-with? p %) [".md" ".markdown"]) :markdown
      :else :other)))

(defn- normalized [path-text]
  (-> path-text (str/replace #"^\./+" "") (str/replace #"^/+" "")))

(defn- dir-parts [path] (vec (butlast (str/split path #"/"))))

(defn- shared-prefix [a b] (count (take-while true? (map = a b))))

(defn candidates
  "The paths of `paths` a citation's path text can name: the path itself, or
  a path ending in `/` and the path text."
  [paths path-text]
  (let [p (normalized path-text)
        tail (str "/" p)]
    (vec (sort (filter #(or (= % p) (str/ends-with? % tail)) paths)))))

(defn choose
  "One path from the candidates (P-C8): the only one; else the ones sharing
  the longest run of directories with the citing document, when that
  leaves one. `{:path p}` or `{:reason :ambiguous :candidates [...]}` or
  `{:reason :not-found}`."
  [cands doc-path]
  (case (count cands)
    0 {:reason :not-found}
    1 {:path (first cands)}
    (let [dd (dir-parts doc-path)
          scored (group-by #(shared-prefix dd (dir-parts %)) cands)
          best (get scored (apply max (keys scored)))]
      (if (= 1 (count best))
        {:path (first best)}
        {:reason :ambiguous :candidates (vec (take 8 cands))}))))

(defn resolve-path
  "Which file a citation names (P-C8), memoizing trees and commits in
  `cache` (an atom local to one step): the document's own line of work at
  the commit its line was written, then each other line of work at its last
  commit at or before that commit's time. Several candidates in a line of
  work end the search there, ambiguous. `{:line :path :commit}` or
  `{:reason ... :candidates?}`; a git read that fails throws, and the step
  that called it fails."
  [repo cache {:keys [line lines]} doc-path path-text written written-time]
  (let [tree (fn [c] (or (get-in @cache [:tree c])
                         (let [t (or (tree-paths repo c) (git-failed! :ls-tree c))]
                           (swap! cache assoc-in [:tree c] t) t)))
        before (fn [head] (let [k [head written-time]]
                            (if (contains? (:before @cache) k)
                              (get-in @cache [:before k])
                              (let [r (or (commit-before repo head written-time) (git-failed! :rev-list head))
                                    c (:commit r)]
                                (swap! cache assoc-in [:before k] c) c))))
        tries (cons [line written]
                    (for [[l head] (sort-by (comp str key) lines)
                          :when (not= l line)
                          :let [c (when written-time (before head))]
                          :when c]
                      [l c]))]
    (loop [ts tries]
      (if-let [[l c] (first ts)]
        (let [r (choose (candidates (tree c) path-text) doc-path)]
          (cond
            (:path r) {:line l :path (:path r) :commit c}
            ;; several candidates in a line of work: that line meant one of them
            (= :ambiguous (:reason r)) (assoc r :line l :commit c)
            :else (recur (rest ts))))
        {:reason :not-found}))))

;; ------------------------------------------------------------- the finder

(defn doc-thing [repo line path] (m/thing-id "doc" repo line path))

(defn cite-thing
  "A citation's thing (P-C3): its document's source, the document's commit,
  and where it sits."
  [repo line path commit at-line col]
  (m/thing-id "cite" repo line path commit at-line col))

(defn find-step
  "The vocabulary's `:cite/find` step: the document a `:cite/doc` request
  names (`{:repo :line :rev :path :lines {line head-commit}}`, on the
  document's thing `doc`), read at its revision in the repository at
  `repo-path`, into one `:cite/found` fact a citation and one
  `:cite/scanned` fact. A document missing, binary or too large is recorded
  as such; a git read that fails fails the step, so the run is retried.
  `{:facts [...]}` or `{:error ...}`. Total."
  [repo-path repo-id {:keys [request doc]}]
  (try
    (let [{:keys [repo line rev path lines]} request]
      (cond
        (not (and (map? request) (= repo repo-id) (env/readable-keyword? line)
                  (string? rev) (string? path) (map? lines)))
        {:error :bad-request}

        (not= doc (doc-thing repo line path)) {:error :wrong-doc-thing}

        :else
        (let [t (m/read-text repo-path rev path)]
          (cond
            ;; a property of the document (missing, binary, too large): a fact
            (m/file-error? (:error t))
            {:facts [{:e doc :k :cite/scanned
                      :v {:line line :path path :rev rev :commit (:commit t) :error (:error t) :found 0}}]}
            ;; anything else (git failed, an unknown revision): retried next pass
            (:error t) {:error :read-failed :read (:error t)}
            :else
            (let [commit (:commit t)
                  found (find-citations (:text t))
                  bl (when (seq found) (or (blame repo-path commit path) (git-failed! :blame path)))
                  cache (atom {})
                  facts (vec (for [c found
                                   :let [written (get-in bl [:line->commit (:line c)])
                                         wtime (get-in bl [:time written])
                                         res (if written
                                               (resolve-path repo-path cache request path (:path-text c) written wtime)
                                               {:reason :no-written-commit})
                                         target (target-kind (or (:path res) (:path-text c)))
                                         cite (cite-thing repo line path commit (:line c) (:col c))]]
                               {:e cite :k :cite/found
                                :v (cond-> {:doc {:line line :path path :commit commit}
                                            :at {:line (:line c) :col (:col c)}
                                            :text (:text c)
                                            :path-text (:path-text c)
                                            :lines (:lines c)
                                            :written written
                                            :target target
                                            :inherited? (:inherited? c)}
                                     (:path res) (assoc :resolved (assoc res :file (m/file-thing repo (:line res) (:path res))))
                                     (not (:path res)) (assoc :unresolved res))}))]
              {:facts (conj facts {:e doc :k :cite/scanned
                                   :v {:line line :path path :rev rev :commit commit :found (count facts)}})})))))
    (catch clojure.lang.ExceptionInfo e
      (if-let [what (::git-failed (ex-data e))]
        {:error :git-failed :git what}
        {:error :internal :detail (str e)}))
    (catch Exception e {:error :internal :detail (str e)})))

;; ---------------------------------------------------------------- states

(defn state-against
  "A citation's state against a reading (P-C10), from the snapshot taken at
  binding (`{:form {:name :digest :lines} :cited [a b]}`): true (same text,
  same lines), moved (same text, other lines: the cited lines now), or stale
  (the text changed, the form gone, the file gone or unreadable)."
  [{:keys [form cited]} rv]
  (let [{:keys [name digest lines]} form
        u (first (filter #(= name (:name %)) (:units rv)))]
    (cond
      (:missing rv) {:state :stale :why :file-gone}
      (:error rv) {:state :stale :why :unreadable :error (:error rv)}
      (nil? u) {:state :stale :why :gone}
      (not= digest (:digest u)) {:state :stale :why :changed :now (:lines u)}
      (= lines (:lines u)) {:state :true :why :same}
      :else (let [d (- (first (:lines u)) (first lines))]
              {:state :moved :why :moved :now (:lines u) :cited-now (mapv #(+ % d) cited)}))))

(defn- mark
  "One `:cite/mark` fact: the citation's state against the reading row `r`
  (`{:fid :stamp :value}`), and the pairs its act stands on for it."
  [cite snapshot r]
  (let [rv (:value r)
        st (state-against snapshot rv)
        u (first (filter #(= (get-in snapshot [:form :name]) (:name %)) (:units rv)))]
    {:fact {:e cite :k :cite/mark
            :v (merge st {:cite cite :bound snapshot
                          :against (cond-> {:reading (:fid r) :commit (:commit rv)}
                                     u (assoc :form (:fid u) :lines (:lines u) :digest (:digest u)))})}
     :stand-on (cond-> [[(:fid r) (:stamp r)]]
                 u (conj [(:fid u) (:stamp r)]))}))

;; ------------------------------------------------------------- the binder

(defn- unbound [cite found-fid reason & {:as more}]
  {:facts [{:e cite :k :cite/unbound :v (merge {:cite cite :found found-fid :reason reason} more)}]
   :stand-on []})

(defn within-units
  "The cited lines without the lines at either end that no unit holds
  (blank lines and comments between forms), as `[first last]`, or nil when
  no cited line is in a unit: a citation that runs one blank line past its
  function still names it (P-C17). `ubl` is a reading's `:unit-by-line`."
  [ubl a b]
  (let [in (fn [l] (some? (nth ubl (dec l) nil)))
        ls (filter in (range a (inc b)))]
    (when (seq ls) [(first ls) (last ls)])))

(def bind-rules
  "What a citation binds to (P-C17, the pointer's lever later): the named
  form holding both ends of its lines, once the lines at either end that no
  unit holds are set aside (`within-units`). The menu is this code; the
  pick is the tool fact's `:target`."
  #{:named-form})

(defn bind-step
  "The vocabulary's `:cite/bind` step: a found citation (`found`, the
  `:cite/found` value; `found-fid`; `cite`, its thing) and the rows of its
  file's readings (`[:ek file :material/file]`) into a `:cite/bound` fact
  standing on the named form and the reading at the commit it was written,
  or a `:cite/unbound` fact with the reason: a passage or other target
  first, then a path that did not resolve. No reading at that commit yet is
  `{:error :no-reading-yet}`, so the run is retried on the next pass. Bound
  to a reading that is no longer the file's latest, it is also marked
  against the latest in the same act (P-C9). `target` is the rule, one of
  `bind-rules`. `{:facts :stand-on}`. Total."
  [{:keys [found found-fid cite readings target]}]
  (try
    (let [{:keys [resolved unresolved lines]} found
          kind (:target found)
          rows (filterv #(map? (:value %)) readings)
          [a b] lines]
      (cond
        (not (contains? bind-rules target)) {:error :unknown-bind-rule :target target}
        (not (map? found)) {:error :bad-found}
        (= :markdown kind) (unbound cite found-fid :markdown-target :path (:path resolved)
                                    :unresolved (:reason unresolved))
        (not= :clojure kind) (unbound cite found-fid :other-target :path (:path resolved)
                                      :unresolved (:reason unresolved))
        unresolved (unbound cite found-fid (case (:reason unresolved)
                                             :ambiguous :ambiguous-path
                                             :not-found :path-not-found
                                             (:reason unresolved))
                            :candidates (:candidates unresolved))
        :else
        (let [at (last (filter #(= (:commit resolved) (get-in % [:value :commit])) rows))
              head (last rows)]
          (if (nil? at)
            {:error :no-reading-yet :commit (:commit resolved)}
            (let [rv (:value at)
                  n (count (:unit-by-line rv))
                  base {:path (:path resolved) :line (:line resolved) :commit (:commit resolved)}]
              (cond
                (:missing rv) (unbound cite found-fid :file-missing :at base)
                (:error rv) (unbound cite found-fid :unreadable :at base :error (:error rv))
                (not (and (integer? a) (integer? b) (<= 1 a b))) (unbound cite found-fid :bad-range :at base)
                (> b n) (unbound cite found-fid :lines-past-end :at base :line-count n)
                :else
                (let [[wa wb :as within] (within-units (:unit-by-line rv) a b)
                      ua (when wa (nth (:unit-by-line rv) (dec wa)))
                      ub (when wb (nth (:unit-by-line rv) (dec wb)))
                      u (when (and ua (= ua ub)) (nth (:units rv) ua))]
                  (cond
                    (nil? u) (unbound cite found-fid :not-inside-one-form :at base)
                    (nil? (:name u)) (unbound cite found-fid :inside-unnamed-form :at base :kind (:kind u)
                                              :unit-lines (:lines u))
                    :else
                    (let [snapshot {:form {:thing (:thing u) :fid (:fid u) :name (:name u) :kind (:kind u)
                                           :lines (:lines u) :digest (:digest u) :commit (:commit rv)}
                                    :cited [a b] :within within}
                          bound {:e cite :k :cite/bound
                                 :v (merge base snapshot {:cite cite :found found-fid :reading (:fid at)})}
                          own [[(:fid u) (:stamp at)] [(:fid at) (:stamp at)]]
                          late (when (and head (not= (:fid head) (:fid at))) (mark cite snapshot head))]
                      {:facts (cond-> [bound] late (conj (:fact late)))
                       :stand-on (into own (:stand-on late))})))))))))
    (catch Exception e {:error :internal :detail (str e)})))

;; ------------------------------------------------------------- the marker

(defn- snapshot-of
  "The citation and its binding snapshot a dependent fact carries: a
  `:cite/bound` fact's own, or a `:cite/mark`'s `:bound`."
  [row]
  (let [v (:value row)]
    (case (:k row)
      :cite/bound [(:cite v) (select-keys v [:form :cited :within])]
      :cite/mark [(:cite v) (:bound v)]
      nil)))

(def show-rules
  "What a mark shows (P-C10, open item 44): the state, why, and where the
  form and the cited lines are now. The menu is this code; the pick is the
  tool fact's `:show`."
  #{:state-and-lines})

(def ^:private citation-keys #{:cite/bound :cite/mark})

(defn check-step
  "The vocabulary's `:cite/check` step: a file's new reading (`reading`,
  `{:fid :stamp :e :replaces :value}`) and the rows of `[:dependents A]`, A
  the previous reading's act (`dependents`, none for a first reading), into
  one `:cite/mark` a citation found among them, against the new reading,
  and one `:cite/walk` on the file's thing that says what the walk found.

  The walk is a chain: each walk stands on its reading, so the next walk
  finds it among what stood on that reading. A reading whose previous
  reading was not walked yet is not walked either
  (`:previous-walk-missing`, so the run fails and is retried; review
  finding 2): a walk that ran over a gap would carry on without the
  citations the missing walk should have carried, silently and for good.
  A citation whose binding snapshot no longer opens (forgotten), or a mark
  carrying none, is marked stale, `:snapshot-erased`, on every walk, and the
  mark stands on the row it read, so the next walk finds it again (finding
  3). `show` is the rule, one of `show-rules`. `{:facts :stand-on}`. Total."
  [{:keys [reading dependents show]}]
  (try
    (cond
      (not (contains? show-rules show)) {:error :unknown-show-rule :show show}

      (and (some? (:replaces reading)) (not-any? #(= :cite/walk (:k %)) dependents))
      {:error :previous-walk-missing :was (:replaces reading)}

      :else
      (let [cited (filter #(contains? citation-keys (:k %)) dependents)
            parsed (for [row cited
                         :let [[c snap] (when (map? (:value row)) (snapshot-of row))
                               c (or c (:e row))]
                         :when (env/readable-keyword? c)]
                     {:cite c :snap (when (map? snap) snap) :row row})
            cites (->> parsed
                       (filter :snap)
                       (reduce (fn [m {:keys [cite snap row]}] (if (contains? m cite) m (assoc m cite [snap row]))) {})
                       (sort-by (comp str key)))
            erased (->> parsed
                        (remove :snap)
                        (remove #(contains? (set (map key cites)) (:cite %)))
                        (reduce (fn [m {:keys [cite row]}] (if (contains? m cite) m (assoc m cite row))) {})
                        (sort-by (comp str key)))
            marks (for [[c [snap row]] cites
                        :let [mk (mark c snap reading)]]
                    (update mk :stand-on conj [(:fid row) (:stamp row)]))
            lost (for [[c row] erased]
                   {:fact {:e c :k :cite/mark
                           :v {:cite c :state :stale :why :snapshot-erased
                               :against {:reading (:fid reading) :commit (get-in reading [:value :commit])}}}
                    :stand-on [[(:fid row) (:stamp row)]]})
            walk {:e (:e reading) :k :cite/walk
                  :v {:reading (:fid reading) :commit (get-in reading [:value :commit])
                      :was (:replaces reading) :found (count dependents)
                      :marked (+ (count cites) (count erased))}}]
        {:facts (-> (mapv :fact marks) (into (map :fact lost)) (conj walk))
         :stand-on (vec (distinct (concat (mapcat :stand-on marks) (mapcat :stand-on lost))))}))
    (catch Exception e {:error :internal :detail (str e)})))
