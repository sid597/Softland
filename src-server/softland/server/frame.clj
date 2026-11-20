(ns softland.server.frame
  "One frame of the wire: the text of one EDN form, read and written. Pure
  code. It requires nothing outside Clojure.

  The values of the store are keywords, UUIDs, sets and numbers of any
  width (src-store/MODEL.md, \"The words\"), so the wire carries EDN, as
  text. A client that speaks JSON, an agent's harness among them, carries
  that text inside its own form.

  Inputs and results, by call:

    read-one  a text -> [:ok the one form in it], or [:no why]
    write     anything that prints as EDN -> its text, on one line

  What is read. `clojure.edn` and nothing else: no code is evaluated, and a
  tag that EDN does not know is no form. `#uuid` and `#inst` are EDN's own.
  A text holds exactly one form; anything after it but blank space is
  turned away, so that a frame cannot carry a second thing past the check.
  A text that is nested so deep that the reader runs out of stack is turned
  away as no EDN.

  What is not bounded here. The size: a face tests the bytes of a frame
  against `softland.server.words/max-frame-bytes` before it reads one.

  State and effects. None. Neither call throws."
  (:require [clojure.edn :as edn])
  (:import [java.io PushbackReader StringReader]))

(defn read-one
  "Takes a text. Gives [:ok form] for a text that holds exactly one EDN
  form, or [:no why]: `:empty` for a text with no form, `:not-edn` for a
  text that EDN cannot read, `:more-than-one-form` for a text with
  something after its first form."
  [text]
  (if-not (string? text)
    [:no :not-edn]
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
