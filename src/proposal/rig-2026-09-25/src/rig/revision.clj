;; IMPORTANT: Before modifying this file, re-read PLAN-revision-reader.md.
(ns rig.revision
  "The revision reader: a file at a git revision, cut into units, each only
  its content and its position. Plain Clojure: no Rama, no store namespace,
  no library beyond Clojure's own.

  Prose (Markdown, and any file that is not Clojure) is cut into passages,
  which are blocks; Clojure (.clj .cljc .cljs .edn) into its top-level forms;
  a span is the lines a person points at. A unit is exactly
  {:content s :position {:lines [first last] :chars [start end]}}: lines
  1-based and inclusive, chars 0-based and end-exclusive, counted in UTF-16
  code units of the text decoded from UTF-8, so (subs text start end) is the
  content, exactly, nothing normalized.

  No identity. No ids, no names used as keys, no hashes offered as identity.
  A result's :commit and :path are the frame its units' positions are
  counted in, not a claim that a unit at one revision is a unit at another;
  whatever says two units are the same is a claim with an actor, and not
  this code's.

  Never throws. Every failure is a map with :error, and an exception nothing
  foresaw is {:error :internal} (an Error, such as running out of memory
  under a caller-set :limit, is not caught; F12). Calls share nothing: no
  atom, no dynamic var, no cache.

  The plan: PLAN-revision-reader.md in the rig folder
  (src/proposal/rig-2026-09-25/); the sections named below are its."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import [java.nio ByteBuffer]
           [java.nio.charset CharacterCodingException Charset CodingErrorAction
            StandardCharsets]
           [java.util Arrays Locale]))

(set! *warn-on-reflection* true)

;; ------------------------------------------------------------------ units

(defn- unit
  "A unit: exactly its content and its position (section 3)."
  [^String text first-line last-line start end]
  {:content  (subs text start end)
   :position {:lines [(long first-line) (long last-line)]
              :chars [(long start) (long end)]}})

(defn- bad-argument
  "A malformed input (the error table's :bad-argument row)."
  [argument value]
  {:error :bad-argument :argument argument :value value})

(defn- internal
  "The last resort (section 5): an exception nothing above foresaw, as data.
  Its stack trace survives only as :detail text (G10)."
  [^Throwable e]
  {:error  :internal
   :detail (str/join "\n" (cons (str e) (map str (take 8 (.getStackTrace e)))))})

;; ------------------------------------------------------------------ lines

(defn- line-index
  "The text's lines (sections 3 and 8): `:starts`, the char each line begins
  at, and `:ends`, where its content ends, just before its terminator (a
  \"\\n\", with a \"\\r\" directly before it part of the terminator); one
  entry a line, as int arrays. The line count is the number of \"\\n\", plus
  one when the text does not end with one; an empty text has no lines."
  [^String text]
  (let [n (.length text)]
    (loop [i 0, start 0, starts (transient []), ends (transient [])]
      (cond
        (= i n)
        (let [open? (< start n)]
          {:starts (int-array (persistent! (cond-> starts open? (conj! start))))
           :ends   (int-array (persistent! (cond-> ends open? (conj! n))))})

        (= \newline (.charAt text i))
        (recur (inc i) (inc i) (conj! starts start)
               (conj! ends (if (and (> i start) (= \return (.charAt text (dec i))))
                             (dec i)
                             i)))

        :else (recur (inc i) start starts ends)))))

(defn- line-of
  "The 1-based line holding char c (section 3), from line-index's starts."
  [starts c]
  (let [r (Arrays/binarySearch ^ints starts (int c))]
    (if (neg? r) (- (inc r)) (inc r))))

;; ------------------------------------------------------------------ blocks

(defn- indentation
  "[columns index] of a line's leading spaces and tabs (section 6): the
  columns they take, a tab advancing to the next multiple of four, and the
  index of the first char after them."
  [^String s]
  (let [n (.length s)]
    (loop [i 0, col 0]
      (if (< i n)
        (let [c (.charAt s i)]
          (cond (= c \space) (recur (inc i) (inc col))
                (= c \tab)   (recur (inc i) (+ col (- 4 (rem col 4))))
                :else        [col i]))
        [col i]))))

(defn- heading?
  "Rule 2: up to three spaces, one to six #, then a space, a tab or the end
  of the line."
  [^String s]
  (let [n  (.length s)
        sp (loop [i 0] (if (and (< i 3) (< i n) (= \space (.charAt s i))) (recur (inc i)) i))
        hs (loop [j sp] (if (and (< j n) (= \# (.charAt s j))) (recur (inc j)) j))]
    (and (<= 1 (- hs sp) 6)
         (or (= hs n) (= \space (.charAt s hs)) (= \tab (.charAt s hs))))))

(defn- fence-open
  "Rule 3: the fence a line opens, {:ch :len}, when its first non-blank chars
  are three or more backticks or three or more tildes; else nil."
  [^String s]
  (let [n (.length s)
        i (loop [i 0] (if (and (< i n) (Character/isWhitespace (.charAt s i))) (recur (inc i)) i))]
    (when (< i n)
      (let [c (.charAt s i)]
        (when (or (= c \`) (= c \~))
          (let [j (loop [j i] (if (and (< j n) (= c (.charAt s j))) (recur (inc j)) j))]
            (when (>= (- j i) 3)
              {:ch c :len (- j i)})))))))

(defn- fence-close?
  "Rule 3: the line holds only the fence's char, repeated at least as many
  times as it opened with, and whitespace."
  [{:keys [ch len]} ^String s]
  (let [t (str/trim s)]
    (and (>= (count t) len) (every? #(= ch %) t))))

(defn- ascii-digit? [c] (<= 48 (int (char c)) 57))

(defn- lowercase-letter? [c] (<= 97 (int (char c)) 122))

(defn- list-item
  "Rule 4: the list item a line starts, {:indent :col :number}, or nil. After
  its indentation, -, * or +, or one to nine digits, optionally one lowercase
  letter, then . or ); then a space, a tab or the end of the line. `:col` is
  the column where the item's text begins, after the marker and the spaces
  after it (rule 5); `:number` is an ordered marker's number, nil for a
  bullet."
  [^String s]
  (let [n (.length s)
        [indent i] (indentation s)
        indent (long indent)
        i (long i)]
    (when (< i n)
      (let [c (.charAt s i)
            [m number]
            (cond
              (or (= c \-) (= c \*) (= c \+)) [(inc i) nil]
              (ascii-digit? c)
              (let [j (long (loop [j i] (if (and (< j n) (ascii-digit? (.charAt s j))) (recur (inc j)) j)))
                    k (if (and (< j n) (lowercase-letter? (.charAt s j))) (inc j) j)]
                (when (and (<= (- j i) 9) (< k n) (let [d (.charAt s k)] (or (= d \.) (= d \)))))
                  [(inc k) (Long/parseLong (subs s i j))]))
              :else nil)]
        (when (and m (let [m (long m)]
                       (or (= m n) (let [d (.charAt s m)] (or (= d \space) (= d \tab))))))
          (let [m (long m)]
            {:indent indent
             :col    (loop [j m, col (+ indent (- m i))]
                       (if (< j n)
                         (let [d (.charAt s j)]
                           (cond (= d \space) (recur (inc j) (inc col))
                                 (= d \tab)   (recur (inc j) (+ col (- 4 (rem col 4))))
                                 :else        col))
                         col))
             :number number}))))))

(defn- close-block
  "The open block, if any, appended to the blocks cut so far."
  [out open]
  (if open (conj! out [(:first open) (:last open)]) out))

(defn- open-block
  "Line l, content s, with no block open: [open out] (section 6)."
  [out l s]
  (cond
    (str/blank? s) [nil out]
    (heading? s)   [nil (conj! out [l l])]
    :else
    (if-let [f (fence-open s)]
      [{:kind :fence :first l :last l :fence f} out]
      (if-let [it (list-item s)]
        [{:kind :item :first l :last l :indent (:indent it) :col (:col it)} out]
        [{:kind :para :first l :last l :indent (first (indentation s))} out]))))

(defn- block-step
  "Line l, content s, through the six rules (section 6): [open out] after it.
  An open block is {:kind :first :last ...}: a :fence block, an :item (with
  its first line's :indent, the :col its text begins at, :gap once a blank
  line follows it, and :fence while a fence inside it is open), or a :para
  (with its first line's :indent). A block's :last is always a non-blank
  line."
  [open out l s]
  (cond
    (nil? open) (open-block out l s)

    ;; Rule 3: inside a fence no other rule applies.
    (:fence open)
    (cond (fence-close? (:fence open) s)
          (if (= :fence (:kind open))
            [nil (conj! out [(:first open) l])]
            [(assoc open :fence nil :last l) out])
          (str/blank? s) [open out]
          :else          [(assoc open :last l) out])

    ;; Rule 1, and rule 5's wait: after a blank line an item continues only
    ;; if the next non-blank line is indented to its text.
    (str/blank? s)
    (if (= :item (:kind open))
      [(assoc open :gap true) out]
      [nil (close-block out open)])

    ;; Rule 2: a heading ends whatever is open, an item included.
    (heading? s) [nil (conj! (close-block out open) [l l])]

    ;; Rules 3 to 5 inside an item.
    (= :item (:kind open))
    (let [ind (long (first (indentation s)))
          f   (fence-open s)
          it  (list-item s)]
      (cond
        (:gap open) (if (>= ind (long (:col open)))
                      [(assoc open :gap false :last l :fence f) out]
                      (open-block (close-block out open) l s))
        f           (if (>= ind (long (:col open)))
                      [(assoc open :last l :fence f) out]
                      (open-block (close-block out open) l s))
        (and it (<= (long (:indent it)) (long (:indent open))))
        (open-block (close-block out open) l s)
        :else       [(assoc open :last l) out]))

    ;; Rules 3, 4 and 6 in a paragraph: a fence ends it; an item not indented
    ;; deeper than its first line interrupts it only as a bullet or the
    ;; number 1; anything else is more of its text.
    :else
    (let [it (list-item s)]
      (if (or (fence-open s)
              (and it
                   (<= (long (:indent it)) (long (:indent open)))
                   (let [k (:number it)] (or (nil? k) (= 1 k)))))
        (open-block (close-block out open) l s)
        [(assoc open :last l) out]))))

(defn- block-ranges
  "The blocks of a text's lines, given as their contents, as [first last]
  line pairs in file order (section 6)."
  [lines]
  (loop [l 1, ls (seq lines), open nil, out (transient [])]
    (if ls
      (let [[open out] (block-step open out l (first ls))]
        (recur (inc l) (next ls) open out))
      (persistent! (close-block out open)))))

(defn blocks
  "Section 6, the block cut: a passage is a block. The text's non-blank lines
  are cut into runs of whole lines by six rules: blank lines end blocks; a
  heading is a block by itself; a fence is kept whole; a list item is a
  block; a list item keeps its continuation; everything else is paragraph
  text. Each unit starts at the first char of its first line and ends just
  before its last line's terminator.

  Returns {:units [unit ...]} in file order; text that is not a string is
  {:error :bad-argument :argument :text :value text}. Never throws."
  [text]
  (try
    (if-not (string? text)
      (bad-argument :text text)
      (let [{:keys [starts ends]} (line-index text)
            ^ints starts starts
            ^ints ends ends
            lines (mapv #(subs text (aget starts (int %)) (aget ends (int %)))
                        (range (alength starts)))]
        {:units (mapv (fn [[a b]]
                        (unit text a b
                              (aget starts (int (dec (long a))))
                              (aget ends (int (dec (long b))))))
                      (block-ranges lines))}))
    (catch Exception e (internal e))))

;; ------------------------------------------------------------------ forms

(defn- terminating?
  "Clojure's terminating chars: a token ends at one (LispReader's macro
  chars other than #, ' and %; section 7)."
  [c]
  (case (char c)
    (\" \; \@ \^ \` \~ \( \) \[ \] \{ \} \\) true
    false))

(defn- token-char?
  "A char that continues a token: not whitespace as Clojure's reader counts
  it (Character/isWhitespace, or a comma; F9) and not a terminating char."
  [c]
  (let [c (char c)]
    (not (or (Character/isWhitespace c) (= c \,) (terminating? c)))))

(defn- token-end
  "Just past the token chars that start at i."
  [^String text i]
  (let [n (.length text)]
    (loop [j (long i)]
      (if (and (< j n) (token-char? (.charAt text j))) (recur (inc j)) j))))

(defn- string-end
  "Just past the closing quote of the string or regex whose opening quote is
  at q, a backslash taking the next char; nil when the text ends first."
  [^String text q]
  (let [n (.length text)]
    (loop [j (inc (long q))]
      (when (< j n)
        (let [c (.charAt text j)]
          (cond (= c \\) (recur (+ j 2))
                (= c \") (inc j)
                :else    (recur (inc j))))))))

(defn- skip-blank
  "The first index at or after i that is not whitespace, a comma or inside a
  comment; a comment starts at ; or #! and runs to the end of the line."
  [^String text i]
  (let [n (.length text)]
    (loop [i (long i)]
      (if (< i n)
        (let [c (.charAt text i)]
          (cond
            (or (Character/isWhitespace c) (= c \,)) (recur (inc i))
            (or (= c \;) (and (= c \#) (< (inc i) n) (= \! (.charAt text (inc i)))))
            (let [nl (.indexOf text (int 10) (int i))]
              (if (neg? nl) n (recur (long nl))))
            :else i))
        i))))

(defn- dispatch
  "The token a # starts at s (section 7, F9). After #: { and ( open; \" is a
  regex; ' and = are prefixes; ^ is metadata; _ discards; ? and ?@ are
  reader conditionals; : a namespaced map's prefix with its namespace; # a
  symbolic value such as ##Inf; ! a comment (taken by skip-blank); a char
  that can start a symbol is a tag. Anything else, the end of the text
  included, is :bad-dispatch; so is <, which Clojure's reader reserves for
  unreadable forms."
  [^String text s]
  (let [n (.length text)
        s (long s)
        d (inc s)]
    (if (>= d n)
      {:reason :bad-dispatch :pos s}
      (let [c (.charAt text d)]
        (case c
          \{ {:t :open :s s :e (+ s 2) :close \}}
          \( {:t :open :s s :e (+ s 2) :close \)}
          \" (if-let [e (string-end text d)]
               {:t :atom :s s :e e}
               {:reason :unclosed-string :pos d})
          (\' \=) {:t :prefix :s s :e (+ s 2) :takes 1}
          \^ {:t :prefix :s s :e (+ s 2) :takes 2}
          \_ {:t :discard :s s :e (+ s 2)}
          \? {:t :prefix :s s :takes 1
              :e (if (and (< (+ s 2) n) (= \@ (.charAt text (+ s 2)))) (+ s 3) (+ s 2))}
          \: {:t :prefix :s s :e (token-end text (+ s 2)) :takes 1}
          \# {:t :atom :s s :e (token-end text (+ s 2))}
          (if (and (token-char? c) (not (Character/isDigit c)) (not= c \<))
            {:t :prefix :s s :e (token-end text d) :takes 1}
            {:reason :bad-dispatch :pos s}))))))

(defn- lex
  "The next token at or after i (section 7), nil at the end of the text. A
  token is {:t :s :e ...}: :atom; :open with the :close it expects; :close
  with its char :c; :prefix with the data it :takes (one, or two for
  metadata); :discard; or :char-eof, a backslash that ends the text. A
  lexical error is {:reason :pos}."
  [^String text i]
  (let [n (.length text)
        s (long (skip-blank text i))]
    (when (< s n)
      (let [c (.charAt text s)]
        (case c
          \( {:t :open :s s :e (inc s) :close \)}
          \[ {:t :open :s s :e (inc s) :close \]}
          \{ {:t :open :s s :e (inc s) :close \}}
          (\) \] \}) {:t :close :s s :e (inc s) :c c}
          \" (if-let [e (string-end text s)]
               {:t :atom :s s :e e}
               {:reason :unclosed-string :pos s})
          \\ (if (< (inc s) n)
               {:t :atom :s s :e (token-end text (+ s 2))}
               {:t :char-eof :s s :e (inc s)})
          (\' \` \@) {:t :prefix :s s :e (inc s) :takes 1}
          \~ {:t :prefix :s s :takes 1
              :e (if (and (< (inc s) n) (= \@ (.charAt text (inc s)))) (+ s 2) (inc s))}
          \^ {:t :prefix :s s :e (inc s) :takes 2}
          \# (dispatch text s)
          {:t :atom :s s :e (token-end text (inc s))})))))

(defn- scan
  "Section 7's scanner: {:spans [[start end] ...]}, the chars of the text's
  top-level units, or {:reason :pos} where it cannot find where a form ends.
  Lexical and iterative, one pass: a stack of the closers it expects and, at
  depth 0, the data the open unit (or skipped region) still needs."
  [^String text]
  (loop [i 0, stack [], need 0, start 0, skip? false, out (transient [])]
    (let [tok (lex text i)]
      (cond
        (nil? tok)
        (if (or (pos? need) (seq stack))
          {:reason :unclosed :pos start}
          {:spans (persistent! out)})

        (:reason tok) tok

        :else
        (let [s    (long (:s tok))
              e    (long (:e tok))
              top? (empty? stack)]
          (case (:t tok)
            :char-eof {:reason :unclosed :pos (if (or (pos? need) (not top?)) start s)}

            :open (if (and top? (zero? need))
                    (recur e (conj stack (:close tok)) 1 s false out)
                    (recur e (conj stack (:close tok)) need start skip? out))

            :close (cond
                     top?                         {:reason :unexpected-close :pos s}
                     (not= (:c tok) (peek stack)) {:reason :mismatched-close :pos s}
                     (> (count stack) 1)          (recur e (pop stack) need start skip? out)
                     :else                        ; a datum completes at depth 0
                     (let [need (dec need)]
                       (if (zero? need)
                         (recur e [] 0 0 false (if skip? out (conj! out [start e])))
                         (recur e [] need start skip? out))))

            :atom (cond
                    (not top?)   (recur e stack need start skip? out)
                    (zero? need) (recur e stack 0 0 false (conj! out [s e]))
                    :else        (let [need (dec need)]
                                   (if (zero? need)
                                     (recur e stack 0 0 false (if skip? out (conj! out [start e])))
                                     (recur e stack need start skip? out))))

            :prefix (let [takes (long (:takes tok))]
                      (cond
                        (not top?)   (recur e stack need start skip? out)
                        (zero? need) (recur e stack takes s false out)
                        :else        (recur e stack (+ need (dec takes)) start skip? out)))

            :discard (cond
                       (not top?)   (recur e stack need start skip? out)
                       (zero? need) (recur e stack 1 s true out)
                       :else        (recur e stack (inc need) start skip? out))))))))

(defn forms
  "Section 7, the form cut: a function at a revision is read as the
  top-level form that holds it, and every top-level form is a unit, whatever
  its head; the reader does not decide which forms are functions. A
  docstring is inside its form. Comments, whitespace and top-level #_
  discards (with the datum each discards) are outside every unit. Reader
  conditionals, metadata and other prefixes belong to the form they precede,
  which starts at its first prefix char. Namespaced keywords and maps are
  tokens; no alias is resolved and nothing is evaluated.

  Returns {:units [unit ...]} in file order, or {:error :unreadable :at
  {:line :char} :reason r} where the scanner cannot find where a form ends,
  r one of :unclosed, :unclosed-string, :mismatched-close,
  :unexpected-close, :bad-dispatch. Text that is not a string is
  :bad-argument. Never throws."
  [text]
  (try
    (if-not (string? text)
      (bad-argument :text text)
      (let [r      (scan text)
            starts (:starts (line-index text))]
        (if-let [reason (:reason r)]
          {:error  :unreadable
           :at     {:line (line-of starts (:pos r)) :char (long (:pos r))}
           :reason reason}
          {:units (mapv (fn [[s e]]
                          (unit text (line-of starts s) (line-of starts (dec (long e))) s e))
                        (:spans r))})))
    (catch Exception e (internal e))))

;; ------------------------------------------------------------------ spans

(defn span
  "Section 8: lines `first-line` through `last-line` of the text as one
  unit, whole lines, blank lines included, nothing trimmed. The text's line
  count is its number of \"\\n\", plus one when it does not end with one; an
  empty text has no lines.

  Returns {:unit unit}, or {:error :bad-argument :argument :lines :value
  [first-line last-line] :line-count n} unless both are integers with
  1 <= first-line <= last-line <= n; text that is not a string is
  :bad-argument with :argument :text. Never throws."
  [text first-line last-line]
  (try
    (if-not (string? text)
      (bad-argument :text text)
      (let [{:keys [starts ends]} (line-index text)
            ^ints starts starts
            ^ints ends ends
            n (alength starts)]
        (if (and (integer? first-line) (integer? last-line) (<= 1 first-line last-line n))
          {:unit (unit text first-line last-line
                       (aget starts (int (dec (long first-line))))
                       (aget ends (int (dec (long last-line)))))}
          (assoc (bad-argument :lines [first-line last-line]) :line-count (long n)))))
    (catch Exception e (internal e))))

;; ------------------------------------------------------------------ step 0

(def ^:private default-limit
  "Step 3's byte budget unless a read names another (section 5, G12): 1 MiB."
  1048576)

(defn- utf-8-encoding?
  "Whether a charset name (the JVM's sun.jnu.encoding) names UTF-8."
  [enc]
  (boolean (and (string? enc)
                (try (= StandardCharsets/UTF_8 (Charset/forName enc))
                     (catch Exception _ false)))))

(defn- ascii? [s] (every? #(< (int (char %)) 128) s))

(defn- malformed-rev?
  "Step 0: blank, an option's leading -, or holding NUL, \\n or \\r."
  [rev]
  (or (str/blank? rev)
      (str/starts-with? rev "-")
      (boolean (some #{\u0000 \newline \return} rev))))

(defn- malformed-path?
  "Step 0: blank, absolute, ending in /, holding NUL, or with an empty, . or
  .. segment. Paths in a git tree are always relative and normalized."
  [path]
  (or (str/blank? path)
      (str/starts-with? path "/")
      (str/ends-with? path "/")
      (str/includes? path "\u0000")
      (boolean (some #{"" "." ".."} (str/split path #"/" -1)))))

(defn- input-error
  "Step 0 (section 5, F6, F14): the first malformed input among those every
  read takes, as a :bad-argument error without the inputs attached, or nil.
  `enc` is the JVM's sun.jnu.encoding, the encoding arguments reach git in:
  unless it is UTF-8, a non-ASCII rev or path is refused. Runs no git."
  [repo rev path opts enc]
  (cond
    (or (not (string? repo)) (str/blank? repo)) (bad-argument :repo repo)
    (or (not (string? rev)) (malformed-rev? rev)) (bad-argument :rev rev)
    (or (not (string? path)) (malformed-path? path)) (bad-argument :path path)
    (and (not (utf-8-encoding? enc)) (not (ascii? rev)))
    (assoc (bad-argument :rev rev) :encoding enc)
    (and (not (utf-8-encoding? enc)) (not (ascii? path)))
    (assoc (bad-argument :path path) :encoding enc)
    (not (map? opts)) (bad-argument :opts opts)
    :else (let [limit (get opts :limit default-limit)]
            (when-not (and (integer? limit) (pos? limit))
              (bad-argument :limit limit)))))

(defn- jnu-encoding [] (System/getProperty "sun.jnu.encoding"))

;; ------------------------------------------------------------------ git

(defn- git-env
  "An environment minus every variable whose name starts with GIT_ (F5): a
  hook exports GIT_DIR and GIT_WORK_TREE, which would have git read that
  repository instead of the one named. clojure.java.shell's :env replaces
  the whole environment, so git gets this filtered copy of the JVM's."
  [env]
  (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_"))) env))

(defn- run-git
  "Runs git with `args` as an argument vector and no shell (section 5), in
  the JVM's environment filtered by git-env and the JVM's working directory
  (neither of clojure.java.shell's dynamic defaults is consulted):
  {:exit :out :err}, stdout as bytes when `bytes?`, else as UTF-8 text;
  {:failed detail} when the process cannot start."
  [git args bytes?]
  (try
    (apply shell/sh git (concat args [:env (git-env (System/getenv)) :dir nil
                                      :out-enc (if bytes? :bytes "UTF-8")]))
    (catch java.io.IOException e {:failed (str e)})))

(defn- git-failed
  "A git step that could not start, or exited in a way the step does not
  expect."
  [r]
  (if (:failed r)
    {:error :git-failed :detail (:failed r)}
    {:error :git-failed :exit (:exit r) :detail (:err r)}))

(def ^:private commit-id #"[0-9a-f]{40}|[0-9a-f]{64}")

(defn- resolve-commit
  "Step 1 (section 5, F1, F2): the commit `rev` names, as {:commit id}, or an
  error. Exit 0 with one commit id on stdout is the commit; exit 0 with
  anything else, or exit 1, names no commit; exit 128 is settled by
  rev-parse --git-dir: the repository is there and git refused the revision,
  or it is not a repository."
  [git repo rev]
  (let [r (run-git git ["-C" repo "rev-parse" "--verify" "--quiet" "--end-of-options"
                        (str rev "^{commit}")]
                   false)]
    (cond
      (:failed r)        (git-failed r)
      (= 0 (:exit r))    (let [out (str/trim (:out r))]
                           (if (re-matches commit-id out)
                             {:commit out}
                             {:error :unknown-revision}))
      (= 1 (:exit r))    {:error :unknown-revision}
      (= 128 (:exit r))  (let [g (run-git git ["-C" repo "rev-parse" "--git-dir"] false)]
                           (cond
                             (:failed g)     (git-failed g)
                             (= 0 (:exit g)) {:error :unknown-revision :detail (:err r)}
                             :else           {:error  :not-a-repository
                                              :detail (if (str/blank? (:err r)) (:err g) (:err r))}))
      :else              (git-failed r))))

(defn- tree-entry
  "Step 2's record parser (section 5): among ls-tree -z -l's NUL-terminated
  records, each `<mode> <type> <oid> <size>\\t<path>`, the one whose path
  is exactly `path`: {:oid :size} for a blob of mode 100644 or 100755; a
  directory, a symlink or a submodule is :not-a-file with its :entry; no
  such record is :missing-path."
  [out path]
  (let [record (some (fn [^String rec]
                       (let [tab (.indexOf rec "\t")]
                         (when (and (>= tab 0) (= path (subs rec (inc tab))))
                           (subs rec 0 tab))))
                     (str/split out #"\u0000"))]
    (if (nil? record)
      {:error :missing-path}
      (let [[mode type oid size] (str/split (str/trim record) #" +")]
        (cond
          (= "040000" mode) {:error :not-a-file :entry :directory}
          (= "120000" mode) {:error :not-a-file :entry :symlink}
          (= "160000" mode) {:error :not-a-file :entry :submodule}
          (and (= "blob" type) (contains? #{"100644" "100755"} mode)
               (string? oid) (string? size) (re-matches #"[0-9]+" size))
          {:oid oid :size (Long/parseLong size)}
          :else {:error :git-failed :detail (str "unexpected ls-tree record: " record)})))))

(defn- find-entry
  "Step 2 (section 5): the entry at exactly `path` in the commit's tree.
  --literal-pathspecs keeps pathspec magic out; --full-tree makes paths
  repository-relative whatever the working directory."
  [git repo commit path]
  (let [r (run-git git ["--literal-pathspecs" "-C" repo "ls-tree" "-z" "--full-tree" "-l"
                        commit "--" path]
                   false)]
    (if (or (:failed r) (not= 0 (:exit r)))
      (git-failed r)
      (tree-entry (:out r) path))))

(defn- read-blob
  "Step 4 (section 5): the blob's bytes exactly, by plumbing (no pager,
  colour, textconv, filters or line-ending conversion), as {:bytes}."
  [git repo oid]
  (let [r (run-git git ["-C" repo "cat-file" "blob" oid] true)]
    (if (or (:failed r) (not= 0 (:exit r)))
      (git-failed r)
      {:bytes (:out r)})))

(defn- bytes->text
  "Steps 5 and 6 (section 5): :binary when a NUL is among the first 8,000
  bytes, git's own test; else strict UTF-8, reporting malformed and
  unmappable input rather than replacing it, :not-utf-8 when that fails; a
  byte order mark stays as the text's first char. {:text s} otherwise."
  [bs]
  (let [^bytes bs bs
        window (min 8000 (alength bs))]
    (if (loop [i 0] (cond (= i window) false
                          (zero? (aget bs i)) true
                          :else (recur (inc i))))
      {:error :binary}
      (try
        (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                        (.onMalformedInput CodingErrorAction/REPORT)
                        (.onUnmappableCharacter CodingErrorAction/REPORT))]
          {:text (str (.decode decoder (ByteBuffer/wrap bs)))})
        (catch CharacterCodingException _ {:error :not-utf-8})))))

(defn- read-text
  "Steps 0 to 6 (section 5): {:rev :commit :path :text}, the whole file at
  the commit `rev` names, or an error carrying the inputs as given, and
  :commit once it is known. `git` is the executable; the entries pass
  \"git\", from PATH (F4). Private, because a whole file offered as a step a
  tool can call invites a file-grain reference; a tool asks for units or a
  span (section 4). Every command names the commit id, never `rev`, so a
  ref that moves during the read cannot split it across two snapshots."
  [git repo rev path opts]
  (let [inputs {:repo repo :rev rev :path path}]
    (if-let [e (input-error repo rev path opts (jnu-encoding))]
      (merge e inputs)
      (let [c (resolve-commit git repo rev)]
        (if (:error c)
          (merge c inputs)
          (let [commit (:commit c)
                at     (assoc inputs :commit commit)
                entry  (find-entry git repo commit path)
                limit  (get opts :limit default-limit)]
            (cond
              (:error entry) (merge entry at)
              (> (long (:size entry)) limit)
              (merge {:error :too-large :size (:size entry) :limit limit} at)
              :else
              (let [blob (read-blob git repo (:oid entry))
                    text (when-not (:error blob) (bytes->text (:bytes blob)))]
                (cond
                  (:error blob) (merge blob at)
                  (:error text) (merge text at)
                  :else {:rev rev :commit commit :path path :text (:text text)})))))))))

;; ------------------------------------------------------------------ entries

(defn cut-for
  "Section 4 (G1): the default cut for a path until first facts carry one:
  :forms when the path, lowercased, ends in .clj, .cljc, .cljs or .edn,
  :blocks for every other path, a value that is not a string included.
  Never throws; like every public function its body sits in the catch that
  gives :internal (section 5), which nothing in it can reach."
  [path]
  (try
    (if (and (string? path)
             (let [p (.toLowerCase ^String path Locale/ROOT)]
               (boolean (some #(.endsWith p ^String %) [".clj" ".cljc" ".cljs" ".edn"]))))
      :forms
      :blocks)
    (catch Exception e (internal e))))

(defn read-units
  "Sections 4 and 5: the file at `path` in the commit `rev` names, cut into
  units. `repo` is a directory in the repository, `rev` any git revision
  string, `path` repository-relative. `opts`: :cut, :blocks (section 6) or
  :forms (section 7), by default (cut-for path); :limit, the byte budget,
  a positive integer, inclusive, by default 1,048,576.

  Returns {:rev :commit :path :cut :units}: `rev` and `path` as given, the
  40-hex (or 64-hex) commit the revision named when read, the cut that made
  the units. Or an error, a map with :error (the plan's error table),
  carrying :repo, :rev and :path as given and :commit once known. Runs git
  from PATH, three processes a read (four when git exits 128 on the
  revision); needs git 2.30 or later (F3). Never throws."
  ([repo rev path] (read-units repo rev path {}))
  ([repo rev path opts]
   (let [inputs {:repo repo :rev rev :path path}]
     (try
       (if-let [e (or (input-error repo rev path opts (jnu-encoding))
                      (when (and (contains? opts :cut) (not (#{:blocks :forms} (:cut opts))))
                        (bad-argument :cut (:cut opts))))]
         (merge e inputs)
         (let [cut (get opts :cut (cut-for path))
               r   (read-text "git" repo rev path opts)]
           (if (:error r)
             r
             (let [u (if (= :forms cut) (forms (:text r)) (blocks (:text r)))]
               (if (:error u)
                 (merge u inputs {:commit (:commit r)})
                 {:rev rev :commit (:commit r) :path path :cut cut :units (:units u)})))))
       (catch Exception e (merge (internal e) inputs))))))

(defn read-span
  "Sections 4, 5 and 8 (G13): lines `first-line` through `last-line` of the
  file at `path` in the commit `rev` names, as one unit of whole lines.
  `opts`: :limit, as for read-units.

  Returns {:rev :commit :path :unit}, or an error as read-units gives them:
  lines that are not two integers with 1 <= first-line <= last-line are
  :bad-argument before git runs; a last line past the file's end is
  :bad-argument after the read, with :line-count. Never throws."
  ([repo rev path first-line last-line] (read-span repo rev path first-line last-line {}))
  ([repo rev path first-line last-line opts]
   (let [inputs {:repo repo :rev rev :path path}]
     (try
       (if-let [e (or (input-error repo rev path opts (jnu-encoding))
                      (when-not (and (integer? first-line) (integer? last-line)
                                     (<= 1 first-line last-line))
                        (bad-argument :lines [first-line last-line])))]
         (merge e inputs)
         (let [r (read-text "git" repo rev path opts)]
           (if (:error r)
             r
             (let [s (span (:text r) first-line last-line)]
               (if (:error s)
                 (merge s inputs {:commit (:commit r)})
                 {:rev rev :commit (:commit r) :path path :unit (:unit s)})))))
       (catch Exception e (merge (internal e) inputs))))))
