;; IMPORTANT: Before modifying this file, re-read PLAN-citation.md section 1.
(ns rig.material
  "Repo material as facts (PLAN-citation.md, section 1): a file at a git
  revision read into the store, the shapes of those facts, and the ids of
  the things they are on. Plain Clojure over `rig.revision`, which it does
  not change: the rig reader cuts a text into units with content and
  position and no identity; this namespace names the Clojure forms the way
  the server's adapter does (`src/app/server/ingest/clojure_adapter.clj`),
  cuts Markdown sections, tabulates which unit holds each line, and writes
  the facts of a reading, where identity across revisions is a claim.

  A file is a thing; a named Clojure form is a thing; a file at a revision
  is a fact on its file's thing, never a thing of its own (\"a new version
  is a new fact on the same thing\"). One reading is one act of the reading
  tool: the reading fact, the text fact, one fact per named form, and one
  gone fact per name that left. Each replaces the previous fact of its
  thing and key; for a form that replace is the tool's claim that the same
  name in the same file is the same function (P-C4).

  Placeholders (PLAN-citation.md section 9): the digest (P-C1), the kind
  table's additions (P-C2), derived thing ids (P-C3), the same-function
  rule (P-C4), every reading restating every form (P-C5).

  Pure except `read-file`, which runs git through `rig.revision`. Total:
  every public function returns data, an error as `{:error ...}`."
  (:require [clojure.string :as str]
            [rig.revision :as revision]
            [rig.store.envelope :as env]))

(set! *warn-on-reflection* true)

;; ------------------------------------------------------------------ ids

(defn digest
  "A unit's digest (P-C1): HMAC-SHA256 over its content under the rig's
  secret (`rig.store.envelope/hmac-hex`; the rig constraint that
  fingerprints over values are keyed), the first 128 bits as 32 hex digits."
  [content]
  (subs (env/hmac-hex (str "softland.material.digest/1\n" content)) 0 32))

(defn thing-id
  "The id of a thing derived from its source (P-C3): `kind` the id's
  namespace (\"file\", \"form\", \"doc\", \"cite\"), and a name of `h` and
  the first 128 bits of HMAC-SHA256 over the canonical EDN of `parts` under
  the rig's secret, so the same source always names the same thing, a
  re-read is a retry, and an id does not confirm a guessed path to whoever
  lacks the secret. The frame's direction is a random id plus a registry
  cell and same-as (open items 17 and 60); nothing here reads meaning out
  of an id."
  [kind & parts]
  (keyword kind (str "h" (subs (env/hmac-hex (str "softland.material.thing/1\n" (env/canonical (vec parts)))) 0 32))))

(defn file-thing
  "The thing a file is: its repository id, its line of work, its path."
  [repo line path]
  (thing-id "file" repo line path))

(defn form-thing
  "The thing a named form is: its file's source and its name."
  [repo line path name]
  (thing-id "form" repo line path name))

;; ---------------------------------------------------------- a small reader

(defn- terminating?
  "Clojure's terminating chars: a token ends at one."
  [c]
  (case (char c)
    (\" \; \@ \^ \` \~ \( \) \[ \] \{ \} \\) true
    false))

(defn- token-char? [c]
  (let [c (char c)]
    (not (or (Character/isWhitespace c) (= c \,) (terminating? c)))))

(defn- token-end [^String s i]
  (let [n (.length s)]
    (loop [j (long i)]
      (if (and (< j n) (token-char? (.charAt s j))) (recur (inc j)) j))))

(defn- string-end
  "Just past the closing quote of the string whose opening quote is at q,
  or the text's length when it never closes."
  [^String s q]
  (let [n (.length s)]
    (loop [j (inc (long q))]
      (cond (>= j n) n
            (= \\ (.charAt s j)) (recur (+ j 2))
            (= \" (.charAt s j)) (inc j)
            :else (recur (inc j))))))

(defn- line-end [^String s i]
  (let [nl (.indexOf s (int 10) (int i))] (if (neg? nl) (.length s) nl)))

(defn- balanced-end
  "Just past the closer matching the opener at i, skipping strings, regexes,
  char literals and comments; the text's length when it never closes."
  [^String s i]
  (let [n (.length s)]
    (loop [j (long i) depth 0]
      (if (>= j n)
        n
        (let [c (.charAt s j)]
          (case c
            (\( \[ \{) (recur (inc j) (inc depth))
            (\) \] \}) (if (= 1 depth) (inc j) (recur (inc j) (dec depth)))
            \" (recur (long (string-end s j)) depth)
            \\ (recur (+ j 2) depth)
            \; (recur (long (line-end s j)) depth)
            (recur (inc j) depth)))))))

(declare datum-end)

(defn- skip-blank
  "The first index at or after i that is not whitespace, a comma, a comment
  (`;` or `#!` to the end of the line) or a `#_` discard with its datum."
  [^String s i]
  (let [n (.length s)]
    (loop [i (long i)]
      (if (>= i n)
        i
        (let [c (.charAt s i)
              d (when (< (inc i) n) (.charAt s (inc i)))]
          (cond
            (or (Character/isWhitespace c) (= c \,)) (recur (inc i))
            (or (= c \;) (and (= c \#) (= d \!))) (recur (long (line-end s i)))
            (and (= c \#) (= d \_)) (recur (long (datum-end s (skip-blank s (+ i 2)))))
            :else i))))))

(defn- datum-end
  "Just past the datum that starts at i (a blank-free index): a collection,
  a string, a char, a token, or a prefix with what it takes (quote, syntax
  quote, unquote, deref, metadata with its target, var quote, reader
  conditional, namespaced map, tagged literal)."
  [^String s i]
  (let [n (.length s)
        i (long i)]
    (if (>= i n)
      n
      (let [c (.charAt s i)
            d (when (< (inc i) n) (.charAt s (inc i)))
            next-datum (fn [j] (datum-end s (skip-blank s j)))]
        (case c
          (\( \[ \{) (balanced-end s i)
          \" (string-end s i)
          \\ (token-end s (min n (+ i 2)))
          (\' \` \@) (next-datum (inc i))
          \~ (next-datum (if (= d \@) (+ i 2) (inc i)))
          \^ (next-datum (next-datum (inc i)))
          \# (case d
               (\{ \() (balanced-end s (inc i))
               \" (string-end s (inc i))
               (\' \=) (next-datum (+ i 2))
               \^ (next-datum (next-datum (+ i 2)))
               \_ (next-datum (next-datum (+ i 2)))
               \? (next-datum (if (and (< (+ i 2) n) (= \@ (.charAt s (+ i 2)))) (+ i 3) (+ i 2)))
               \: (next-datum (token-end s (+ i 2)))
               \# (token-end s (+ i 2))
               (if (nil? d) (inc i) (next-datum (token-end s (inc i)))))
          (if (token-char? c) (token-end s i) (inc i)))))))

(defn- through-meta
  "The index of the datum that metadata at i (if any) applies to."
  [^String s i]
  (loop [i (long i)]
    (let [n (.length s)]
      (if (and (< i n) (= \^ (.charAt s i)))
        (recur (long (skip-blank s (datum-end s (skip-blank s (inc i))))))
        (if (and (< (inc i) n) (= \# (.charAt s i)) (= \^ (.charAt s (inc i))))
          (recur (long (skip-blank s (datum-end s (skip-blank s (+ i 2))))))
          i)))))

(defn- symbol-token
  "The token at i when it reads as a symbol, else nil: not a keyword, a
  number, a char, a string, nil, true or false."
  [^String s i]
  (let [n (.length s)]
    (when (and (< i n) (token-char? (.charAt s i)))
      (let [^String t (subs s i (token-end s i))
            c0 (.charAt t 0)
            c1 (when (> (count t) 1) (.charAt t 1))]
        (when-not (or (= c0 \:)
                      (Character/isDigit c0)
                      (and (#{\+ \-} c0) c1 (Character/isDigit (char c1)))
                      (#{"nil" "true" "false"} t))
          t)))))

;; ---------------------------------------------------------- names and kinds

(def head-kinds
  "The server adapter's closed table (clojure_adapter.clj `head-name->kind`),
  over a head symbol's unqualified name."
  {"ns" :clj/ns "def" :clj/def "defonce" :clj/def "defn" :clj/fn "defn-" :clj/fn
   "defrecord" :clj/record "deftype" :clj/record "defprotocol" :clj/protocol
   "defmacro" :clj/macro "defmulti" :clj/multi "defmethod" :clj/multi
   "deftest" :clj/test "defmodule" :clj/module "comment" :clj/rich-comment})

(def added-kinds
  "This reader's additions (P-C2): the store's own Rama definitions."
  {"deframaop" :clj/rama-op "deframafn" :clj/rama-fn})

(def named-kinds
  "Kinds whose next datum, when a symbol, is the form's name (the server's
  `name-carrying-kinds`, the additions, and any other def-prefixed head)."
  #{:clj/def :clj/fn :clj/record :clj/protocol :clj/macro :clj/multi :clj/test
    :clj/module :clj/electric-fn :clj/rama-op :clj/rama-fn :clj/def-other})

(defn kind-of
  "A head symbol's text to its kind: `e/defn` first, then the tables by the
  unqualified name, then any other head starting with `def` (P-C2)."
  [head]
  (let [slash (str/index-of head "/")
        [nsp nm] (if (and slash (pos? slash) (< slash (dec (count head))))
                   [(subs head 0 slash) (subs head (inc slash))]
                   [nil head])]
    (cond
      (and (= nm "defn") (= nsp "e")) :clj/electric-fn
      (contains? head-kinds nm) (get head-kinds nm)
      (contains? added-kinds nm) (get added-kinds nm)
      (str/starts-with? nm "def") :clj/def-other
      :else :clj/other)))

(defn form-head
  "A top-level form's head, kind and raw name, read from its content
  without evaluation: `{:head :kind :name}`. A top-level reader conditional
  is `:clj/reader-cond`; a form that is not a list is `:clj/other`; `ns` is
  named \"ns\" (one a file, as the server's block path is); a defmethod's
  name is its multimethod's, \":\" and its dispatch value's text. Metadata
  on the form, its head or its name is looked through. Total."
  [content]
  (try
    (let [^String s (str content)
          n (.length s)
          i (through-meta s (skip-blank s 0))]
      (cond
        (and (< (inc i) n) (= \# (.charAt s i)) (= \? (.charAt s (inc i))))
        {:head (if (and (< (+ i 2) n) (= \@ (.charAt s (+ i 2)))) "#?@" "#?")
         :kind :clj/reader-cond :name nil}

        (and (< i n) (= \( (.charAt s i)))
        (let [h (through-meta s (skip-blank s (inc i)))
              head (symbol-token s h)
              kind (if head (kind-of head) :clj/other)
              after-head (when head (skip-blank s (token-end s h)))
              name-at (when after-head (through-meta s after-head))
              raw (cond
                    (= kind :clj/ns) "ns"
                    (contains? named-kinds kind) (symbol-token s name-at)
                    :else nil)
              nm (if (and raw (= "defmethod" (some-> head (str/split #"/") last)))
                   (let [d (skip-blank s (token-end s name-at))]
                     (if (< d n)
                       (str raw ":" (str/trim (subs s d (min n (datum-end s d)))))
                       raw))
                   raw)]
          {:head head :kind kind :name nm})

        :else {:head nil :kind :clj/other :name nil}))
    (catch Exception _ {:head nil :kind :clj/other :name nil})))

(defn dedup-names
  "Units in file order with duplicate names suffixed `~2`, `~3` (the
  server's R7): the first keeps its name."
  [units]
  (first (reduce (fn [[out seen] u]
                   (if-let [nm (:name u)]
                     (let [c (get seen nm 0)]
                       [(conj out (if (zero? c) u (assoc u :name (str nm "~" (inc c)))))
                        (assoc seen nm (inc c))])
                     [(conj out u) seen]))
                 [[] {}] units)))

;; ----------------------------------------------------------- Markdown

(defn heading-level
  "The level of a Markdown heading line, 1 to 6, or nil (the rig reader's
  rule 2: up to three spaces, one to six `#`, then a space, a tab or the
  end of the line)."
  [line]
  (when-let [[_ hashes] (re-matches #" {0,3}(#{1,6})(?:[ \t].*)?" (str line))]
    (count hashes)))

(defn heading-title [line]
  (-> (str line) (str/replace #"^ {0,3}#{1,6}[ \t]*" "") (str/replace #"[ \t]+#+[ \t]*$" "") str/trim))

(defn sections
  "Markdown sections from the passages (units with :lines and :content): a
  heading passage opens a section of its level that runs to the line
  before the next heading of the same or a higher level, else to
  `last-line`. `[{:i :level :title :lines [first last]}]` in file order."
  [units last-line]
  (let [heads (vec (keep (fn [u]
                           (let [first-line (first (str/split-lines (str (:content u))))]
                             (when-let [lv (heading-level first-line)]
                               {:level lv :title (heading-title first-line) :at (first (:lines u))})))
                         units))]
    (vec (map-indexed
          (fn [i {:keys [level title at]}]
            (let [next-at (some (fn [h] (when (and (> (:at h) at) (<= (:level h) level)) (:at h)))
                                (subvec heads (inc i)))]
              {:i i :level level :title title :lines [at (if next-at (dec next-at) last-line)]}))
          heads))))

(defn by-line
  "A vector of `line-count` entries, index line − 1, holding the index of
  the range from `ranges` ({:i :lines [a b]}) covering the line; later
  ranges in `ranges` overwrite earlier ones where they overlap."
  [line-count ranges]
  (persistent!
   (reduce (fn [v {:keys [i lines]}]
             (let [[a b] lines]
               (reduce (fn [v l] (if (<= 1 l line-count) (assoc! v (dec l) i) v))
                       v (range a (inc b)))))
           (transient (vec (repeat line-count nil)))
           ranges)))

;; ------------------------------------------------------------ one reading

(defn- text-lines
  "The text's lines as the rig reader counts them: `n` of them, each without
  its terminator."
  [text n]
  (if (zero? n) [] (vec (take n (concat (str/split text #"\r?\n" -1) (repeat ""))))))

(defn read-text
  "The whole text of `path` at the commit `rev` names, through the rig
  reader's public API: `rig.revision/read-span` over lines 1 to the line
  count, which a first call past the end reports. `{:commit :text
  :line-count}`, or the reader's error (`:missing-path`, `:binary`,
  `:not-utf-8`, `:too-large`, `:unknown-revision`, ...)."
  [repo rev path]
  (let [probe (revision/read-span repo rev path 1 Long/MAX_VALUE)]
    (cond
      (and (= :bad-argument (:error probe)) (contains? probe :line-count))
      (let [n (long (:line-count probe))]
        (if (zero? n)
          {:commit (:commit probe) :text "" :line-count 0}
          (let [r (revision/read-span repo rev path 1 n)]
            (if (:error r)
              r
              {:commit (:commit r) :text (get-in r [:unit :content]) :line-count n}))))
      (:error probe) probe
      :else {:commit (:commit probe) :text (get-in probe [:unit :content]) :line-count 1})))

(defn cut-text
  "A text cut as `path` says (`rig.revision/cut-for`): Clojure into forms
  with heads, kinds and names, anything else into passages and sections;
  each unit `{:i :kind :head :name :lines :chars :digest}`, with the
  per-line tables. `{:cut :units :unit-by-line :sections :section-by-line}`
  or `{:error :unreadable ...}` when the form scanner cannot find where a
  form ends."
  [path text line-count]
  (let [cut (revision/cut-for path)
        r (if (= :forms cut) (revision/forms text) (revision/blocks text))]
    (if (:error r)
      (select-keys r [:error :at :reason])
      (let [units (dedup-names
                   (vec (map-indexed
                         (fn [i {:keys [content position]}]
                           (let [h (if (= :forms cut) (form-head content) {:head nil :kind :passage :name nil})]
                             (merge {:i i :lines (:lines position) :chars (:chars position)
                                     :digest (digest content)}
                                    h)))
                         (:units r))))
            secs (if (= :forms cut)
                   []
                   (sections (map (fn [u raw] {:lines (:lines u) :content (:content raw)}) units (:units r))
                             line-count))]
        {:cut cut
         :units units
         :unit-by-line (by-line line-count units)
         :sections secs
         :section-by-line (if (= :forms cut) [] (by-line line-count (sort-by :level secs)))}))))

(def file-errors
  "The read errors that are properties of the file at its revision, and so
  facts a reading records: missing, not a file, binary, not UTF-8, too
  large, forms the scanner cannot end (review finding 6). Any other error
  (git failed, an unknown revision, not a repository) is the environment's,
  and fails the step so the run is retried."
  #{:missing-path :not-a-file :binary :not-utf-8 :too-large :unreadable})

(defn file-error? [e] (contains? file-errors e))

(defn read-file
  "`path` at the commit `rev` names, read and cut: `{:commit :lines :cut
  :units :unit-by-line :sections :section-by-line}`, or `{:error r
  :commit?}`. Runs git through `rig.revision`."
  [repo rev path]
  (let [t (read-text repo rev path)]
    (if (:error t)
      (select-keys t [:error :commit :size :limit :detail])
      (let [c (cut-text path (:text t) (:line-count t))]
        (merge {:commit (:commit t) :lines (text-lines (:text t) (:line-count t))}
               (if (:error c) {:error :unreadable :unreadable c} c))))))

(defn- present-names
  "The names a reading value holds as present forms."
  [v]
  (into #{} (keep :name) (:units v)))

(defn reading-facts
  "The facts of one reading (PLAN-citation.md 1.3), in order: the reading on
  the file's thing, its text, one `:material/form` per named form, one gone
  fact per name the previous reading held present and this one does not.
  `request` is `{:repo :line :rev :path}`; `file` the file's thing; `read`
  what `read-file` gave; `prev` the previous reading's row (`:fid` and
  `:value`) or nil; `run` the name this act will have, so every fact id in
  the reading's value is absolute. Every fact replaces its thing's previous
  fact for its key when there is one. A missing file makes every present
  name gone; an unreadable one writes no form and no gone fact and carries
  the previous heads. Returns `{:facts [...]}`. Pure."
  [{:keys [repo line rev path]} file read prev run]
  (let [pv (:value prev)
        heads (or (:heads pv) {})
        gone-before (or (:gone pv) #{})
        base {:repo repo :line line :path path :rev rev :commit (:commit read)}
        missing? (= :missing-path (:error read))
        error (when (and (:error read) (not missing?)) (:error read))
        units (if (:error read) [] (:units read))
        named (filterv :name units)
        form-at (into {} (map-indexed (fn [j u] [(:name u) (+ 2 j)])) named)
        units (mapv (fn [u] (if-let [nm (:name u)]
                              (assoc u :thing (form-thing repo line path nm) :fid [run (long (get form-at nm))])
                              u))
                    units)
        left (if error
               []
               (vec (sort (remove (set (keys form-at))
                                  (remove gone-before (keys heads))))))
        gone-at (into {} (map-indexed (fn [j nm] [nm (+ 2 (count named) j)])) left)
        new-heads (if error
                    heads
                    (merge heads
                           (into {} (map (fn [[nm j]] [nm [run (long j)]])) form-at)
                           (into {} (map (fn [[nm j]] [nm [run (long j)]])) gone-at)))
        gone (if error gone-before (into (reduce disj gone-before (keys form-at)) left))
        value (cond-> (merge base {:cut (:cut read) :units units
                                   :unit-by-line (or (:unit-by-line read) [])
                                   :sections (or (:sections read) [])
                                   :section-by-line (or (:section-by-line read) [])
                                   :heads new-heads :gone gone :text-fid [run 1]})
                missing? (assoc :missing true)
                error (assoc :error error))
        replaces (fn [nm] (get heads nm))]
    {:facts (-> [{:e file :k :material/file :v value :replaces (:fid prev)}
                 {:e file :k :material/text
                  :v (cond-> {:commit (:commit read) :lines (or (:lines read) [])}
                       missing? (assoc :missing true)
                       error (assoc :error error))
                  :replaces (:text-fid pv)}]
                (into (for [u (filter :name units)]
                        {:e (:thing u) :k :material/form
                         :v (merge base (select-keys u [:i :name :kind :head :lines :chars :digest]))
                         :replaces (replaces (:name u))}))
                (into (for [nm left]
                        {:e (form-thing repo line path nm) :k :material/form
                         :v (assoc base :gone true :name nm)
                         :replaces (replaces nm)})))}))

(defn reading-step
  "The vocabulary's `:material/reading` step (PLAN-citation.md 3): the
  request's file at its revision, read in the repository at `repo-path`
  that the recipe's literal repository id `repo-id` names, as the facts of
  one reading after `prev-rows` (the rows of `[:latest file
  :material/file]`, at most one). The request must be `{:repo :line :rev
  :path}` for this repository and `file` its file's thing; the rule for the
  same function across revisions is `same` (only `:name-in-file`, P-C4).
  A read error that is not the file's own (`file-errors`) fails the step.
  `{:facts [...]}` or `{:error ...}`. Total."
  [repo-path repo-id {:keys [request file prev run same]}]
  (try
    (let [{:keys [repo line rev path]} request
          prev-row (first prev)]
      (cond
        (not= :name-in-file same) {:error :unknown-same-rule :same same}
        (not (and (map? request) (= repo repo-id) (env/readable-keyword? line)
                  (string? rev) (string? path)))
        {:error :bad-request}
        (not= file (file-thing repo line path)) {:error :wrong-file-thing}
        (and prev-row (not (map? (:value prev-row)))) {:error :previous-unreadable}
        (not (and (vector? run) (env/valid-name? run))) {:error :no-run-name}
        :else (let [rd (read-file repo-path rev path)]
                (if (and (:error rd) (not (file-error? (:error rd))))
                  {:error :read-failed :read (:error rd)}
                  (reading-facts request file rd prev-row run)))))
    (catch Exception e {:error :internal :detail (str e)})))
