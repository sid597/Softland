(ns softland.server.frame
  "One frame of the wire: the text of one EDN form, read and written. Pure
  code. It requires Clojure and one number of the store's declaration.

  The values of the store are keywords, UUIDs, sets and numbers of any
  width (src-store/MODEL.md, \"The words\"), so the wire carries EDN, as
  text. A client that speaks JSON, an agent's harness among them, carries
  that text inside its own form.

  Inputs and results, by call:

    read-one  a text -> [:ok the one form in it], or [:no why]
    write     anything that prints as EDN -> its text, on one line
    longest-token
              a text -> the length of its longest run of characters that
              EDN reads as one token

  What is read. `clojure.edn` and nothing else: no code is evaluated, and a
  tag that EDN does not know is no form. `#uuid` and `#inst` are EDN's own.
  A text holds exactly one form; anything after it but blank space is
  turned away, so that a frame cannot carry a second thing past the check.
  A text that is nested so deep that the reader runs out of stack is turned
  away as no EDN.

  What a read may cost. The bytes of a frame do not bound the time that
  reading it takes: EDN reads a long run of digits as one number, and the
  JVM makes a number of n digits in time that grows with n times n. One
  request of 1.6 million digits, with no credential, held a thread of the
  server for 22 seconds (the review of 20 November 2026 ran it). So before
  a text is read, its longest token is held to `max-token-chars`: a text
  with a longer one is no frame. The number is the store's own bound on the
  text of one value, so no act that a gate takes, and no query that finds a
  thing, is turned away by it: a value whose text is that long holds no
  longer run. The scan knows nothing of strings and so cannot be led
  astray by one: a run inside a string is held to the same bound, which a
  value that a gate takes keeps too.

  What is not bounded here. The size: a face tests the bytes of a frame
  against the bounds of `softland.server.words` before it reads one.

  State and effects. None. Neither call throws."
  (:require [clojure.edn :as edn]
            [softland.store.door.declaration :as declaration])
  (:import [java.io PushbackReader StringReader]))

(def max-token-chars
  "How many characters one token of a frame can have: the store's bound on
  the bytes of one value's text
  (`softland.store.door.declaration/max-value-bytes`). Reading one number
  of that length takes some tens of milliseconds."
  declaration/max-value-bytes)

(defn longest-token
  "Takes a text. Gives the length of its longest run of characters that
  EDN's reader takes as one token: a run with no blank space, no comma, and
  none of the characters at which the reader ends a number or a symbol
  (`\"` `;` `^` `(` `)` `[` `]` `{` `}` `\\` `#`). One pass, with no
  knowledge of where a string begins or ends."
  [^String text]
  (let [n (.length text)]
    (loop [i 0 run 0 longest 0]
      (if (= i n)
        (max run longest)
        (let [ch (.charAt text i)]
          (if (or (Character/isWhitespace ch)
                  (case ch (\, \" \; \^ \( \) \[ \] \{ \} \\ \#) true false))
            (recur (inc i) 0 (max run longest))
            (recur (inc i) (inc run) longest)))))))

(defn read-one
  "Takes a text. Gives [:ok form] for a text that holds exactly one EDN
  form, or [:no why]: `:a-token-too-long` for a text whose longest token is
  over `max-token-chars`, which is tested before anything is read;
  `:empty` for a text with no form; `:not-edn` for a text that EDN cannot
  read; `:more-than-one-form` for a text with something after its first
  form."
  [text]
  (cond
    (not (string? text)) [:no :not-edn]
    (< max-token-chars (longest-token text)) [:no :a-token-too-long]
    :else
    (try
      (let [reader (PushbackReader. (StringReader. text))
            end (Object.)
            form (edn/read {:eof end} reader)]
        (cond
          (identical? end form) [:no :empty]
          (identical? end (edn/read {:eof end} reader)) [:ok form]
          :else [:no :more-than-one-form]))
      (catch Throwable _ [:no :not-edn]))))

(defn write
  "Takes a thing that prints as EDN. Gives its text on one line: Clojure
  prints a line end inside a string as two characters, so a stream of
  frames is one frame on each line. Maps
  print whole, with no shared namespace in front, so that a reader that is
  no Clojure reads them."
  [thing]
  (binding [*print-length* nil
            *print-level* nil
            *print-namespace-maps* false
            *print-meta* false
            *print-readably* true]
    (pr-str thing)))
