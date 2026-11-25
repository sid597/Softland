(ns softland.store.door.digest
  "The digest of a thing: SHA-256 over its one text, with the time on the
  act left out. Pure code. The door and both gates share it, and no
  function throws.

  Two digests, of two things. The digest of a thing, `digest` and `hex-of`,
  is of the thing as it arrived: it holds the act-id, the depot and the
  maker's syntax, so that a maker tells her own answer from another thing's
  under the same act-id. The digest that a yes for one act holds,
  `hex-for-a-yes`, is of the act as its maker made it, in the one form that
  `softland.store.act/parse` gives, without the act-id, the depot, the time
  and the permission. So it is made before the act has an act-id or a
  permission, and the gate makes the same of the thing that arrives under
  any act-id of hers.

  Why the time is left out: a maker's second send of one act under one
  act-id carries another time, and both sends must read the one answer as
  their own; a gate keeps the digest of the first thing to arrive. Only the
  top-level `:door-time` of a plain map is taken out; a `:door-time` deeper
  in a thing stays.

  The one text of the thing (`softland.store.door.text`) is fed to the hash
  piece by piece as the thing is walked, so no whole text of a large thing
  is built. One difference from the text as a string, for speed: a run of
  bytes is fed as `#bytes`, its count, and the bytes themselves, not as
  hex. For a thing that holds no bytes and no top-level `:door-time` the
  digest is the SHA-256 of the UTF-8 of `text`.

  Why the whole 32 bytes are kept. With two things of one digest, a
  different act would read the first act's answer as its own. The full
  width of a standard hash needs no argument about what short enough means.

  `weighed` counts the weight in the walk that feeds the digest: it walks
  the thing once, where a digest and a weight apart would walk it twice."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.door.weight :as weight])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn- sha-256
  "Every JVM has the algorithm."
  ^MessageDigest []
  (MessageDigest/getInstance "SHA-256"))

(defn- without-the-time
  "What a digest is taken of. It never throws. A map that throws when a part
  is taken out of it comes back as it is: a sorted map whose keys cannot be
  compared with a keyword is one, and such a map cannot hold the part.

  A thing with no time and the same thing with a time have one digest. So a
  thing that was appended without the door and refused `:no-door-time`
  would be read as her own answer by a maker who later sends the same act
  through the door under the same act-id. That is derived from this
  function and the walk's check of the answer under the act-id; no run
  made it."
  {:softland/whose-call
   [{:state :not-decided
     :what "That a digest leaves the time out is one of the three places of
            the working answer on a second send
            (`softland.store.door/send-act` names the three). Kept for now;
            what a second send of the same act carries as its time is Sid's
            to say."}]}
  [x]
  (if (and (map? x) (not (record? x)))
    (try (dissoc x declaration/time-part)
         (catch Throwable _ x))
    x))

(defn digest
  "32 bytes, the same in every process for the same thing. It never throws:
  a thing that throws while it is read has the digest of the text
  #unreadable, as `softland.store.door.text/text` gives that thing."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (.digest md)
      (catch Throwable _
        (.digest (sha-256) (.getBytes "#unreadable" StandardCharsets/UTF_8))))))

(defn hex-of
  "The digest as hex text: 64 lower-case characters."
  [x]
  (text/hex (digest x)))

(defn- the-time-entry
  "[the time's value, whether another entry is beside it] for a plain map
  that holds the part of the time; nil for anything else. It never throws."
  [x]
  (try (when (and (map? x) (not (record? x)) (contains? x declaration/time-part))
         [(get x declaration/time-part) (< 1 (count x))])
       (catch Throwable _ nil)))

(def ^:private time-name-bytes
  "The bytes of the name of the time's part and of the space after it, as
  the one text of a map writes an entry: `:door-time `."
  (inc (count (str declaration/time-part))))

(def ^:private past-the-bound
  "What the walk of the time's text throws to stop, once the weight has its
  word. `weigh-the-time!` alone catches it."
  (ex-info "the weight has its word" {}))

(defn- weigh-the-time!
  "Adds the bytes of the time's own text to a weight in the making, as the
  thing's one text writes it, one collection deep. `word` is the array that
  holds the weight's word. The walk stops as soon as the weight has a word,
  so it is bounded by the bound, but for a set, whose text is made whole
  before it is given. A time that throws while it is read is
  `:not-weighed`. It never throws."
  [t add! held! ^objects word]
  (let [stop! (fn [] (when (some? (aget word 0)) (throw past-the-bound)))]
    (try
      (text/feed-part t 1
                      (fn [^String s] (add! (text/utf8-count s)) (stop!))
                      (fn [b] (add! (+ 2 (* 2 (alength ^bytes b)))) (stop!))
                      (fn [part] (held! part) (stop!)))
      (catch Throwable e
        (when-not (identical? e past-the-bound)
          (aset word 0 :not-weighed))))))

(defn weighed
  "The digest of a thing and its weight, in one walk: {:digest :weight}.
  `bound` is a whole number not below zero.

  The weight is the number of bytes of the UTF-8 of the one text of the
  thing as it arrived; a run of bytes counts as its text does, two hex
  digits for each byte and two quotes. For each part that the text does not
  write out whole, what the part holds is added
  (`softland.store.door.weight`). The digest's walk leaves the time out, so
  the time's entry is added after it: its name, a space, the bytes of the
  time's own text, and the space that parts it from an entry beside it.

  `:weight` is a long when it is not over the bound; `:too-heavy` when it
  passes the bound; `:not-weighed` when the thing holds a part of a class
  that the weight does not know, or throws while it is read. The word that
  the walk finds first is the one given. Once the weight passes the bound
  no part is weighed further, so the work of the weight beyond the digest's
  own walk is bounded by the bound, but for a set in the time. The digest's
  walk goes to its end whatever the weight: the digest is of the whole
  thing.

  It never throws: a thing that throws while it is read has the digest of
  #unreadable and is not weighed."
  [x bound]
  (let [bound (long bound)
        md (sha-256)
        total (long-array 1)
        word (object-array 1)
        add! (fn [^long n]
               (aset total 0 (+ (aget total 0) n))
               (when (and (nil? (aget word 0)) (> (aget total 0) bound))
                 (aset word 0 :too-heavy)))
        held! (fn [part]
                (when (nil? (aget word 0))
                  (let [w (weight/weight-of part (- bound (aget total 0)))]
                    (if (keyword? w) (aset word 0 w) (add! w)))))]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (let [b (.getBytes s StandardCharsets/UTF_8)]
                                   (.update md b)
                                   (add! (alength b))))
                 (fn [b] (.update md ^bytes b)
                   (add! (+ 2 (* 2 (alength ^bytes b)))))
                 held!)
      (let [d (.digest md)]
        (when-let [[t beside?] (the-time-entry x)]
          (add! (+ time-name-bytes (if beside? 1 0)))
          (weigh-the-time! t add! held! word))
        {:digest d :weight (or (aget word 0) (aget total 0))})
      (catch Throwable _
        {:digest (.digest (sha-256) (.getBytes "#unreadable" StandardCharsets/UTF_8))
         :weight :not-weighed}))))

(def ^:private yes-shapes
  "`softland.store.act/shapes` and one entry more, `:act-for-a-yes`: the
  maker's `:act` without its `:permission`. So the one form that it gives
  is the one form of `softland.store.act/parse` without the permission, and
  is never written a second time."
  (assoc act/shapes
         :act-for-a-yes (into [:parts] (remove #(= :permission (first %))) (rest (get act/shapes :act)))))

(def ^:private no-part-of-the-act-a-yes-names
  "A yes names its act before the act has any of them: the act names its
  permission, and the permission names the act."
  [:act-id :depot declaration/time-part :permission])

(defn act-for-a-yes
  "The act that a yes for one act names: the one form that
  `softland.store.act/parse` gives, without its act-id, its depot, its time
  and its permission. `x` is a maker's act, with its permission or without
  one, or a thing as it arrives on a depot. nil for a thing whose other
  parts are no act. It never throws.

  So `5` and `5N`, facts as a list or as a vector, and an empty part left
  out or given are one act here, as they are one act to `parse`."
  [x]
  (when (and (map? x) (not (record? x)))
    (let [[verdict one-form] (try (act/as-shape yes-shapes :act-for-a-yes
                                                (apply dissoc x no-part-of-the-act-a-yes-names))
                                  (catch Throwable _ [:no nil]))]
      (when (= :ok verdict) one-form))))

(defn hex-of-the-act
  "The digest that a yes for one act holds in its sixth slot. It holds the
  act to nothing more than `act-for-a-yes` does: it is the gate's, for a
  thing that its declaration took already. A maker calls `hex-for-a-yes`,
  which gives the same text for every act that the declaration can take.
  It never throws."
  [x]
  (when-let [one-form (act-for-a-yes x)]
    (hex-of one-form)))

(def ^:private shortest-holder
  "An id whose text is the shortest that an id of the form can have, two
  characters. The yes of `under-the-least-yes` is given beneath the first
  permission of this holder, so that no yes that a giver can give is
  shorter in text."
  :a)

(def ^:private a-digest
  "A text in the form of a digest. Every digest has that length, so the text
  of a yes is as long whatever act it names."
  (apply str (repeat 64 \0)))

(defn- replaced-uuids
  "The uuids of the act-ids that the act's facts replace, so that an act-id
  can be made that no fact of the act replaces. It never throws."
  [x]
  (into #{}
        (keep (fn [fact]
                (try (let [[[_ _ u]] (:replaces fact)] (when (uuid? u) u))
                     (catch Throwable _ nil))))
        (try (seq (:facts x)) (catch Throwable _ nil))))

(defn- under-the-least-yes
  "The thing as the door would append it under the least demanding yes
  there is: an act-id whose uuid no fact of the act replaces, the time 0,
  and a yes whose text is no longer than that of any yes that a giver can
  give. So the declaration refuses this thing only when it refuses the act
  under every yes, every act-id and every time: what it holds against the
  permission, the bound on an act's bytes and the limit of an id, is held
  here at its least."
  [x one-form]
  (let [{:keys [who lane]} one-form
        taken (replaced-uuids x)
        uuid (first (remove taken (map #(java.util.UUID. 0 (long %)) (range))))]
    (assoc (apply dissoc x no-part-of-the-act-a-yes-names)
           :act-id [lane :streaming uuid]
           :depot :streaming
           declaration/time-part 0
           :permission [who lane lane [shortest-holder lane lane] :see-and-act [:act uuid a-digest]])))

(defn hex-for-a-yes
  "The digest that a yes for one act holds in its sixth slot, for a maker
  who asks a yes for her act. nil where `act-for-a-yes` gives nil, and for
  an act that the declaration refuses under every yes that could name it
  (`under-the-least-yes`): a yes for such an act could never be used. It
  never throws.

  The gate makes the same text of the thing that arrives under a yes
  (`hex-of-the-act`). The act-id is no part of what the yes names, so the
  act goes under any act-id of its maker's.

  What it cannot hold: a giver who makes the text by another hand gives a
  yes that names an act which no gate takes, and the gate cannot tell,
  since a yes names its act by the digest alone. And the bound on an act's
  bytes and the limit of an id are held under the shortest yes, so a long
  permission of the giver's can still bring an act of the form over them."
  [x]
  (when-let [one-form (act-for-a-yes x)]
    (when (:ok (declaration/check (under-the-least-yes x one-form)))
      (hex-of one-form))))

(defn is-digest-of?
  "The comparison of a sender that still holds its thing. The door does not
  call it: it holds no thing after a send, and compares two hex texts
  (`softland.store.door.words/answer`)."
  [hex thing]
  (= hex (hex-of thing)))

(defn readable-hex-of
  "The digest that tells a throw: nil for a thing that throws while it is
  read, where `hex-of` gives the digest of #unreadable. The door takes the
  digest of the thing that it is about to append with this, so that it
  appends no thing that it could not read to its end. A thing with no end
  does not come back from it.

  It never throws."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (text/hex (.digest md))
      (catch Throwable _ nil))))
