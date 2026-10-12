(ns probe.seen
  "What the probe says of one thing, by the store's own pure functions: its
  digest, its one text, and what the act's check gives for it. Pure code.

  One function, `seen-of`, is called in three places with the same thing: by
  the client before it appends (what the door would give a maker), inside a
  topology on the thing as it came off the depot (what a gate would keep),
  and by the client on the record that it reads back from the depot's log
  (what a rebuild would read). The run compares the three.

  Every string that `seen-of` gives is ASCII: a character outside the
  printable ASCII range is written as a backslash, u and four hex digits. So
  what a topology says of a thing crosses Rama's own writing of a row
  unchanged, whatever Rama does to other strings.

  Nothing here is a form for the store."
  (:require [softland.store.act :as act]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]))

(defn shown
  "Takes a string. Gives it with each character outside the printable ASCII
  range written as \\uXXXX, a surrogate too. A long string is cut to its
  first 300 and last 120 characters."
  [^String s]
  (let [s (if (> (.length s) 480)
            (str (subs s 0 300) " ...(" (.length s) " chars)... " (subs s (- (.length s) 120)))
            s)
        sb (StringBuilder.)]
    (dotimes [i (.length s)]
      (let [c (.charAt s i) n (int c)]
        (if (and (>= n 0x20) (< n 0x7f))
          (.append sb c)
          (.append sb (format "\\u%04X" n)))))
    (.toString sb)))

(defn id-of
  "Takes anything. Gives the uuid of its act-id as text, or \"no-id\". It
  never throws."
  [thing]
  (try
    (let [id (when (map? thing) (get thing :act-id))]
      (if (and (vector? id) (= 3 (count id))) (str (nth id 2)) "no-id"))
    (catch Throwable _ "no-id")))

(defn- class-name-of
  "The name of the class of a thing, or \"nil\"."
  [x]
  (if (nil? x) "nil" (.getName (class x))))

(defn- classes
  "Takes a value. Gives the names of the classes in it, each once, in the
  order of a walk, as one string. At most twelve."
  [x]
  (let [kids (fn [c] (cond (record? c) nil
                           (map? c) (mapcat identity c)
                           (coll? c) (seq c)
                           :else nil))
        all (tree-seq #(boolean (and (coll? %) (not (record? %)))) kids x)]
    (apply str (interpose " " (take 12 (distinct (map class-name-of (take 5000 all))))))))

(defn- first-fact
  "The first fact of a thing, when the thing is a map whose :facts is a
  sequence; else nil."
  [thing]
  (let [facts (when (map? thing) (get thing :facts))]
    (when (sequential? facts) (first facts))))

(defn seen-of
  "Takes anything. Gives a map of strings:

    :digest       the hex of the store's digest of the thing
    :text         the store's one text of the thing, shown in ASCII and cut
    :text-length  the count of characters of the whole text
    :facts-class  the class of its :facts
    :v-class      the class of the first fact's :v
    :v-classes    the classes inside that :v
    :v-text       the one text of that :v as it is, shown and cut
    :parse        \"ok\", or the word with which the act's check refuses the
                  thing without its :act-id and :depot
    :row-text     for an :ok, the one text of the first fact's :v in its
                  normal form: what a fact's row holds for the value
    :row-bytes    the count of the UTF-8 bytes of that text
    :row-digest   the hex of the SHA-256 of those bytes: the value digest

  It never throws: a throw gives {:threw the message}."
  [thing]
  (try
    (let [t (text/text thing)
          fact (first-fact thing)
          v (when (map? fact) (get fact :v))
          parsed (act/parse (if (map? thing) (dissoc thing :act-id :depot) thing))
          normal (when (:ok parsed) (:v (first (:facts (:ok parsed)))))
          row (when (:ok parsed) (text/text normal))]
      {:digest (digest/hex-of thing)
       :text (shown t)
       :text-length (str (count t))
       :facts-class (class-name-of (when (map? thing) (get thing :facts)))
       :v-class (class-name-of v)
       :v-classes (classes v)
       :v-text (shown (text/text v))
       :parse (if (:ok parsed) "ok" (str (:refuse parsed)))
       :row-text (if row (shown row) "-")
       :row-bytes (if row (str (alength ^bytes (text/utf8 normal))) "-")
       :row-digest (if row
                     (text/hex (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                        ^bytes (text/utf8 normal)))
                     "-")})
    (catch Throwable e
      {:threw (shown (str e))})))
