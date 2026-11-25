(ns softland.store.door.digest
  "The digest of a thing: SHA-256 over its one text, with the time on the
  act left out. Pure code. It needs no cluster.

  One function that the door and both gates share. A gate keeps the digest
  of each thing that it decides, in the answer under the thing's act-id, and
  the read by act-id gives it back. The door gives its maker the digest of
  what it sent, as hex text, and compares that text with the answer's: it
  keeps no thing. So a maker tells its own act's answer from the answer of
  another act under the same act-id. A gate also keeps the digest of each
  stray.

  Inputs and results, by call:

    digest         anything -> 32 bytes
    hex-of         anything -> the digest as hex text, 64 characters
    weighed        anything and a bound -> the digest and the thing's
                   weight, in one walk
    act-for-a-yes  a maker's act, or a thing as it arrives -> the act that a
                   yes for one act names, in its one form, or nil
    hex-for-a-yes  the same -> the hex text that a yes for that act holds,
                   or nil, also for an act that the declaration refuses
                   under every yes: the maker's call
    hex-of-the-act the same -> the same text with no check of the act: the
                   gate's, for a thing that its declaration took
    is-digest-of?  a digest as hex text, and anything -> true or false
    readable-hex-of  anything -> the digest as hex text, or nil for a thing
                     that throws while it is read

  Two digests, of two things. The digest of a thing, `digest` and
  `hex-of`, is of the thing as it arrived: it holds the act-id, the depot
  and the maker's syntax, so that a maker tells her own answer from another
  thing's under the same act-id. The digest that a yes for one act holds,
  `hex-for-a-yes`, is of the act as its maker made it, in the one form that
  `softland.store.act/parse` gives, without the four parts that are no part
  of the act that a yes names: the act-id, the depot, the time and the
  permission. So a maker, or whoever says yes to her act, makes it before
  the act has an act-id or a permission, and the gate makes the same of the
  thing that arrives under any act-id of hers: one walk for both, and for
  the maker the declaration's check of her act first, so that no yes is
  made for an act that no gate can take (`hex-of-the-act` is the gate's,
  which the declaration has checked already).

  What the digest of a thing is taken of. The thing as it arrived, each value plain,
  without the time on the act: when the thing is a plain map, a map that is
  no record, its top-level `:door-time` is taken out first
  (`softland.store.door.declaration/time-part` holds the name). Anything
  else is taken as it is, and a `:door-time` deeper in a thing stays. Why
  the time is left out: a maker's second send of one act under one act-id
  carries another time, and both sends must read the one answer as their
  own; a gate keeps the digest of the first thing to arrive. So two things
  that differ only in that part have one digest, and so have a thing with
  the part and the same thing without it.

  The one text of the thing (`softland.store.door.text`) is fed to the hash
  piece by piece as the thing is walked, so no whole text of a large thing
  is built. One difference from the text as a string, for speed: a
  run of bytes is fed as `#bytes`, its count, and the bytes themselves, not
  as hex. For a thing that holds no bytes and no top-level `:door-time` the
  digest is the SHA-256 of the UTF-8 of `text`.

  Why the whole 32 bytes are kept. With two things of one digest, a
  different act would read the first act's answer as its own. The full
  width of a standard hash needs no argument about what short enough means.

  The weight, in the same walk. A gate keeps each thing that it decides a
  second time, for the read by stamp, up to a bound in weight
  (`softland.store.gate.decision/kept-bound`). The walk that feeds the
  digest is the walk of the thing's one text, so `weighed` counts the bytes
  of that text as it feeds the hash: the weight of a thing is the number of
  bytes of the UTF-8 of its one text as it arrived, the time on it too, and
  for each part that the text does not write out whole what that part
  holds (`softland.store.door.weight`). It walks the thing once, where a
  digest and a weight apart would walk it twice.

  State and effects. None. Each function is pure, and none throws."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.door.weight :as weight])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn- sha-256
  "A new SHA-256 hash, empty. Every JVM has the algorithm."
  ^MessageDigest []
  (MessageDigest/getInstance "SHA-256"))

(defn- without-the-time
  "Takes anything. Gives what a digest is taken of: a plain map, a map that
  is no record, without its top-level `:door-time`
  (`softland.store.door.declaration/time-part`); anything else as it is. A
  plain map that does not hold the part comes back equal to itself.

  It never throws. A map that throws when a part is taken out of it comes
  back as it is: a sorted map whose keys cannot be compared with a keyword
  is one, and such a map cannot hold the part. So each such thing keeps a
  digest of its own.

  Not decided: kept for now. That a digest leaves the time out is one of
  the three places of the working answer on a second send
  (`softland.store.door/send-act` names the three); what a second send of
  the same act carries as its time is Sid's to say. What follows from it
  beside the second send: a thing with no time and the same thing with a
  time have one digest. So a thing that was appended without the door and
  refused `:no-door-time` would be read as her own answer by a maker who
  later sends the same act through the door under the same act-id. That
  is derived from this function and the walk's check of the answer under
  the act-id; no run made it."
  [x]
  (if (and (map? x) (not (record? x)))
    (try (dissoc x declaration/time-part)
         (catch Throwable _ x))
    x))

(defn digest
  "Takes anything. Gives its digest: 32 bytes, the same in every process for
  the same thing, taken of the thing without the time on the act
  (`without-the-time`). It never throws: a thing that throws while it is
  read has the digest of the text #unreadable, as
  `softland.store.door.text/text` gives that thing. Cost: one walk of the
  thing."
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
  "Takes anything. Gives its digest as hex text: 64 lower-case characters,
  the hex of what `digest` gives, so the time on the act is left out here
  too. This is the form in which a caller gets a digest, and in which a
  gate keeps the digest of a stray."
  [x]
  (text/hex (digest x)))

(defn- the-time-entry
  "Takes anything. Gives [the time's value, whether another entry is beside
  it] when it is a plain map that holds the part of the time on the act
  (`softland.store.door.declaration/time-part`), the part that
  `without-the-time` takes out; nil for anything else. It never throws: a
  map that throws when the part is looked up cannot hold it, as
  `without-the-time` says."
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
  word: one object, made once, which `weigh-the-time!` alone catches."
  (ex-info "the weight has its word" {}))

(defn- weigh-the-time!
  "Adds the bytes of the time's own text to a weight in the making, as the
  thing's one text writes it, one collection deep
  (`softland.store.door.text/feed-part`); a part of it that the text does
  not write out whole is weighed for what it holds, as in the rest of the
  thing. Takes the time's value, the function that adds bytes, the function
  that weighs such a part, and the array that holds the weight's word. The
  walk stops as soon as the weight has a word: past its bound, the weight
  is `:too-heavy`, so the walk is bounded by the bound, but for a set,
  whose text is made whole before it is given, as the digest's walk makes
  a set's. A time that throws while it is read is `:not-weighed`. It never
  throws."
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
  "The digest of a thing and its weight, in one walk. Takes anything, and a
  bound, a whole number not below zero. Gives {:digest the 32 bytes that
  `digest` gives for the thing, :weight w}.

  The weight is the number of bytes of the UTF-8 of the one text of the
  thing as it arrived (`softland.store.door.text/text`), where the text
  writes the thing out whole; a run of bytes counts as its text does, two
  hex digits for each byte and two quotes. For each part that the text does
  not write out whole, a part that it writes as #other or #too-deep and a
  decimal, what the part holds is added (`softland.store.door.weight`).
  The walk is that of `digest`, which leaves the time on the act out, so
  the time's entry is added after it: its name, a space, the bytes of the
  time's own text as the thing's text writes it (`weigh-the-time!`), and
  the space that parts it from an entry beside it. So a thing that the text
  writes out whole weighs the bytes of its one text, the time on it too,
  whatever the time holds.

  w is that weight, a long, when it is not over the bound; `:too-heavy`
  when it passes the bound; `:not-weighed` when the thing holds a part of a
  class that the weight does not know, or throws while it is read. The word
  that the walk finds first is the one given. Once the weight passes the
  bound no part is weighed further, and the weight keeps no number: so the
  work of the weight beyond the digest's own walk is bounded by the bound,
  but for a set in the time, whose text is made whole before it is weighed,
  as the digest's walk makes the text of a set.
  The digest's walk goes to its end whatever the weight: the digest is of
  the whole thing.

  A thing that the declaration takes holds no part that its text does not
  write out, but its decimals, which are weighed from their length with no
  walk. It never throws: a thing that throws while it is read has the
  digest of #unreadable, as `digest` gives it, and is not weighed. Cost: the
  walk of `digest`, and one sum for each piece of its text."
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
  "The map of shapes by which the act that a yes names is read:
  `softland.store.act/shapes` as `act.clj` declares it, and one entry more,
  `:act-for-a-yes`: the maker's `:act` without its `:permission`, each other
  part as `act.clj` declares it, in its order and with its options. So the
  one form that it gives is the one form of `softland.store.act/parse`
  without the permission, part for part, and is never written a second
  time."
  (assoc act/shapes
         :act-for-a-yes (into [:parts] (remove #(= :permission (first %))) (rest (get act/shapes :act)))))

(def ^:private no-part-of-the-act-a-yes-names
  "The four parts that a thing can hold and that are no part of the act
  that a yes names: the door's three additions, the act-id, the depot and
  the time on the act (`softland.store.door.declaration/time-part`), and
  the act's permission. A yes names its act before the act has any of
  them: the act names its permission, and the permission names the act."
  [:act-id :depot declaration/time-part :permission])

(defn act-for-a-yes
  "Takes anything: a maker's act as she made it, with its permission or
  without one; or a thing as it arrives on a depot, with the door's three
  additions. Gives the act that a yes for one act names: the act in the one
  form that `softland.store.act/parse` gives, without its act-id, its depot,
  its time and its permission, and with every other part, its who and its
  session among them. Each part that the maker left out is there as the
  one form has it, nil or the declared default; facts are a vector, each
  with its four parts; each integer is a long. Gives nil for a thing whose
  other parts are no act: not a plain map, a part that the form does not
  name, a part that is wrong. It never throws.

  So `5` and `5N`, facts as a list or as a vector, and an empty part left
  out or given are one act here, as they are one act to `parse`: for any
  act that `parse` takes, this gives what `parse` gives without
  `:permission`. A maker who has no permission yet is given it all the
  same, since the permission is no part of it."
  [x]
  (when (and (map? x) (not (record? x)))
    (let [[verdict one-form] (try (act/as-shape yes-shapes :act-for-a-yes
                                                (apply dissoc x no-part-of-the-act-a-yes-names))
                                  (catch Throwable _ [:no nil]))]
      (when (= :ok verdict) one-form))))

(defn hex-of-the-act
  "The digest that a yes for one act holds in its sixth slot
  (`softland.store.act/covers`): SHA-256 over the one text of the act that
  the yes names (`act-for-a-yes`), as 64 hex characters. Takes what
  `act-for-a-yes` takes. Gives nil where that gives nil. It holds the act
  to nothing more: it is the gate's, for a thing that its declaration took
  already (`softland.store.gate.decision/hex-for-its-permission`), and a
  maker calls `hex-for-a-yes`, which gives the same text for every act that
  the declaration can take. It never throws. Cost: one reading of the act's
  parts, as `parse` reads them, and one walk of its one form into the
  hash."
  [x]
  (when-let [one-form (act-for-a-yes x)]
    (hex-of one-form)))

(def ^:private shortest-holder
  "The id whose text is the shortest that an id of the form can have, two
  characters: a keyword of one letter. The yes of `under-the-least-yes` is
  given beneath the first permission of this holder, so that no yes that a
  giver can give is shorter in text."
  :a)

(def ^:private a-digest
  "A text in the form of a digest, 64 hex characters. Every digest has that
  length, so the text of a yes is as long whatever act it names."
  (apply str (repeat 64 \0)))

(defn- replaced-uuids
  "Takes a maker's act, as she gave it. Gives the set of the uuids that the
  act-ids in its facts' `:replaces` hold, so that an act-id can be made
  that no fact of the act replaces. It never throws: what is not a fact id
  gives none."
  [x]
  (into #{}
        (keep (fn [fact]
                (try (let [[[_ _ u]] (:replaces fact)] (when (uuid? u) u))
                     (catch Throwable _ nil))))
        (try (seq (:facts x)) (catch Throwable _ nil))))

(defn- under-the-least-yes
  "Takes a maker's act, as she gave it, and the act in its one form
  (`act-for-a-yes`). Gives the thing as the door would append it under the
  least demanding yes there is: an act-id of the act's lane whose uuid no
  fact of the act replaces, its depot as the act-id says, the time 0, and
  as its permission a yes for one act of the act's who beneath the first
  permission of `shortest-holder`, whose text is no longer than that of any
  yes that a giver can give. So the declaration refuses this thing only
  when it refuses the act under every yes, every act-id and every time:
  what it holds against the permission, the bound on an act's bytes and
  the limit of an id, is held here at its least. The act's own parts are
  as the maker gave them."
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
  who asks a yes for her act: what `hex-of-the-act` gives, for an act that
  the declaration takes. Takes what `act-for-a-yes` takes, a maker's act or
  a thing as it arrives. Gives nil where that gives nil, and for an act
  that the declaration of the door refuses under every yes that could name
  it (`softland.store.door.declaration/check`, of the act as
  `under-the-least-yes` makes it): too many facts, a value or an act over
  its bound, a place named twice, a key of the store's that is no store
  key, an id over its limit, and every other rule of the form. A yes for
  such an act could never be used: the gate refuses its act at the
  declaration, under the yes as under any permission. It never throws.

  The gate makes the same text of the thing that arrives under a yes
  (`hex-of-the-act`), so the two never make two texts of one act. The act
  goes under any act-id of its maker's: the act-id is no part of what the
  yes names. What it cannot hold: a giver who makes the text by another
  hand gives a yes that names an act which no gate takes, and the gate
  cannot tell, since a yes names its act by the digest alone; and the bound
  on an act's bytes and the limit of an id are held under the shortest
  yes, so a long permission of the giver's can still bring an act of the
  form over them. Cost: the declaration's check of the act, and the walk
  of `hex-of-the-act`."
  [x]
  (when-let [one-form (act-for-a-yes x)]
    (when (:ok (declaration/check (under-the-least-yes x one-form)))
      (hex-of one-form))))

(defn is-digest-of?
  "The comparison of a sender that still holds its thing, as the hand of the
  module's proof does. Takes a digest as hex text, as the read by act-id
  gives it, and the thing that was sent, with the time on the act or
  without it. Gives true when the digest is the digest of that thing: the
  answer is then the answer of the sender's own act. False for a digest
  that is nil or of another thing. The door does not call it: it holds no
  thing after a send, and compares two hex texts
  (`softland.store.door.words/answer`)."
  [hex thing]
  (= hex (hex-of thing)))

(defn readable-hex-of
  "The digest that tells a throw. Takes anything. Gives its digest as hex
  text, the 64 characters that `hex-of` gives; or nil for a thing that
  throws while it is read, where `hex-of` gives the digest of
  #unreadable. The door takes the digest of
  the thing that it is about to append with this, so that it appends no
  thing that it could not read to its end. It never throws. Cost: one walk
  of the thing, the walk of `digest`. Limit: a thing with no end does not
  come back from it."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (text/hex (.digest md))
      (catch Throwable _ nil))))
