(ns gate-histories.record
  "A file of things that were said, sent through the store's own gate in
  memory, one act for each. Pure code: no cluster, and nothing is kept.

  Why. The first thing to live in Softland should be Sid's own material: the
  record of what he and the sessions said. Before anything is designed for
  that, this asks the store one question: does it take that record as it is?
  The answer names what an importer has to do.

  What it reads. A file with one EDN map a line, in the order said:

    {:id \"45c7127b\" :kind :line :ts \"2026-11-21T09:28:38Z\" :ms 1795...
     :sid \"d5632e19-...\" :tool \"claude\" :text \"...\"}

  `:kind` is `:line` for a line of Sid's and `:reply` for a session's text.
  The file is made outside the repository and is not in it: the record is
  private.

  The form that it sends is a bet, made to get an answer, and no decision:

  - one layer, `:record`. Sid holds its first permission and gives the
    importer one that allows acting;
  - the who of every act is the importer. Who said the thing is a fact of
    the act: Sid's own case of the importer and Alice
    (src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 2);
  - one act for one thing said, on an entity named by the record's id (a
    keyword: the store takes no string as an id), with
    the facts `:said/by`, `:said/at`, `:said/in`, `:said/tool` and
    `:said/text`; and `:claimed-when`, the time when it was said;
  - a text over the bound on one value is cut into parts, each a fact on an
    entity of its own, `<id>.1`, `<id>.2` and on, in the same act. The cut
    is by bytes and knows nothing of sentences.

  Run it from this folder:

    clojure -M:record /path/to/record.edn

  It prints how many acts the gate took and refused, by word; how many
  things had to be cut; the rows of a kind that the walk does not know, by
  kind (`gate-histories.walk`, `:aside`); and how long it took. It prints
  nothing that was said."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gate-histories.walk :as gate]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration])
  (:import [java.nio.charset StandardCharsets]))

(def ^:private layer :record)
(def ^:private owner :sid)
(def ^:private importer :importer)
(def ^:private owner-permission [owner layer layer])
(def ^:private importer-permission [importer layer layer owner-permission :see-and-act])

(def part-bytes
  "The most bytes of text in one part of a thing that is cut. Under the
  store's bound on one value, with room for the quotes and the escapes of
  the value's one text."
  30000)

(defn- thing
  "A thing as it arrives on the streaming depot."
  [n who permission facts extra]
  (merge {:act-id [layer :streaming (java.util.UUID. 1 (long n))]
          :depot :streaming
          declaration/time-part 1795000000000
          :version act/version
          :who who
          :layer layer
          :permission permission
          :session :import-1
          :facts (vec facts)}
         extra))

(defn- parts
  "Cuts a text into strings of at most `part-bytes` UTF-8 bytes each, on
  the boundaries of characters. A text within the bound is one part."
  [^String text]
  (if (<= (alength (.getBytes text StandardCharsets/UTF_8)) part-bytes)
    [text]
    (loop [from 0 out []]
      (if (>= from (.length text))
        out
        (let [to (loop [to (min (.length text) (+ from part-bytes))]
                   ;; shorten until the bytes fit and no pair of chars is split
                   (if (or (> (alength (.getBytes (.substring text from to) StandardCharsets/UTF_8)) part-bytes)
                           (and (< to (.length text)) (Character/isLowSurrogate (.charAt text to))))
                     (recur (dec to))
                     to))]
          (recur to (conj out (.substring text from to))))))))

(defn- said
  "The act for one thing said, as its maker, the importer, sends it."
  [n {:keys [id kind ts ms sid tool text]}]
  (let [cut (parts (or text ""))
        by (if (= :line kind) "sid" (str "session " sid))
        ;; An id of the store is a keyword or a UUID, never a string, and a
        ;; keyword must be one that reads back. The record's ids are eight
        ;; hex characters and most begin with a digit, which the name of a
        ;; keyword with a namespace cannot. So each gets a letter in front.
        id (keyword "said" (str "x" id))]
    (thing n importer importer-permission
           (concat [{:e id :k :said/by :v by}
                    {:e id :k :said/at :v ts}
                    {:e id :k :said/in :v sid}
                    {:e id :k :said/tool :v tool}]
                   (when (< 1 (count cut)) [{:e id :k :said/parts :v (count cut)}])
                   (map-indexed (fn [i part]
                                  {:e (if (zero? i) id (keyword "said" (str (name id) "." i))) :k :said/text :v part})
                                cut))
           (when (pos? ms) {:claimed-when ms}))))

(defn -main
  "Sends the making act, the give to the importer, and one act for each line
  of the file. Prints the counts."
  [path]
  (let [started (System/nanoTime)
        making (thing 0 owner owner-permission
                      [{:e layer :k :softland.store/depot :v :streaming}
                       {:e owner :k :softland.store/permission :v owner-permission}]
                      nil)
        give (thing 1 owner owner-permission
                    [{:e importer :k :softland.store/permission :v importer-permission}]
                    nil)
        start (reduce (fn [rec sent] (:record (gate/decide rec :streaming sent)))
                      gate/empty-record
                      [making give])]
    (with-open [reader (io/reader path)]
      (let [result
            (reduce
             (fn [{:keys [rec n] :as state} line]
               (let [row (edn/read-string line)
                     sent (said n row)
                     {:keys [record outcome]} (gate/decide rec :streaming sent)
                     verdict (or (:verdict outcome) (:stray outcome))
                     text-bytes (alength (.getBytes ^String (or (:text row) "") StandardCharsets/UTF_8))
                     cut? (< part-bytes text-bytes)]
                 (-> state
                     (assoc :rec record :n (inc n))
                     (update-in [:verdicts verdict] (fnil inc 0))
                     (update-in [:kinds (:kind row)] (fnil inc 0))
                     (cond-> cut? (update :cut inc))
                     (cond-> (< declaration/max-value-bytes text-bytes) (update :over-the-bound inc))
                     (update :most-facts max (count (:facts sent)))
                     (update :bytes + text-bytes))))
             {:rec start :n 2 :verdicts {} :kinds {} :cut 0 :over-the-bound 0 :most-facts 0 :bytes 0}
             (line-seq reader))
            seconds (/ (- (System/nanoTime) started) 1e9)
            rec (:rec result)]
        (println "things said:" (:kinds result))
        (println "the gate's answers:" (:verdicts result))
        (println (format "text: %.1f MB; %d things over the store's bound on one value (%d bytes); %d cut into parts; most facts in one act: %d"
                         (/ (:bytes result) 1e6) (:over-the-bound result) declaration/max-value-bytes
                         (:cut result) (:most-facts result)))
        (println (format "the record after: %d rows of facts, %d answers, last stamp %d"
                         (count (:facts rec)) (count (:answers rec)) (get (:last-stamp rec) layer)))
        (println "rows of a kind that the walk does not know, kept aside, by kind:"
                 (let [aside (frequencies (map first (:aside rec)))] (if (empty? aside) "none" aside)))
        (println (format "%.1f seconds, %.0f acts a second, one thread, in memory"
                         seconds (/ (- (:n result) 2) seconds)))))))
