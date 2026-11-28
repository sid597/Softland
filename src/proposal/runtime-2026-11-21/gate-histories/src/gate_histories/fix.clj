(ns gate-histories.fix
  "The store of the permission fix as a gate under test: each thing of a
  history goes through the store's own door additions and its own walk
  (`softland.store.gate.walk`), against a record held as a map
  (`softland.store.record.in-memory`), and the lane is read through the
  store's own rights (`softland.store.read-path.lanes`). Pure code: no
  cluster. Written by the session that built the kernel, not by the
  judge's; the rules and the statement to prove are not touched.

  What is judged here is the store's pure code: the declaration, the
  checks, the rows that a decision writes, and the read of an asker's
  rights asked once. The walk is not a copy: it is the function that a
  gate drives against its tables. That a gate's module drives it so, on a
  cluster, is the module's proof.

  How a thing of a history is said to the store. A history's thing is
  already in the store's form, but for three things. It carries no
  `:version`: the maker's act gets the store's one version. A part that it
  holds as nil is left out. And a give for one exact act names its act by
  a text that the history made up (`gate-histories.histories`, `yes-text`),
  where the store names it by the digest of the act as its maker made it:
  the text is replaced, everywhere it stands in a thing, by the store's
  own digest of the act that the history wrote down for it
  (`softland.store.door.digest/hex-for-a-yes`). A text that names no act
  that anybody made stays as it is.

  What is not sent, and is counted by its word:

    :a-place-named-twice   an act that names one place in two facts,
                           under a plain key or a setting's. The store's
                           declaration refuses it for its form, before any
                           row is read, and the rules do not hold the form
                           of a thing: they take such an act.
    :a-permission-named-twice
                           an act that names one permission's id in two
                           facts. The same refusal of the declaration. The
                           rules refuse it too, as a give changed twice in
                           one act, but as their last rule, where the
                           store's word comes before any row is read; so
                           the store's word for it is the same under a
                           give that was never given and under one that
                           ended, which `apart` would count against it.
                           The store's own proof holds the refusal.
    :an-act-whose-author-is-everyone
                           an act whose `:who` is `:anyone` or
                           `:anyone-signed-in`. Everyone holds and never
                           acts (CONTRACT.md, section 2.1), so the store's
                           declaration refuses the form. A history sends
                           one where a step has the holder of a give write
                           and that holder is everyone; the rules take it
                           as an act of a party.

  `questions` names what a difference falls under when it is one of the
  judge's readings that this store is built otherwise on, or cannot keep
  with the rows that the contract gives it. Each is held by a test of the
  thing and of what stood, not by the two answers alone. Such a thing is
  taken out of its history for both and counted, and the run prints the
  count: it is a question for Sid, not a break and not a fix."
  (:require [gate-histories.histories :as h]
            [gate-histories.reference :as reference]
            [softland.store.act :as act]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.words :as words]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.in-memory :as memory]
            [softland.store.record.rows :as record]))

(def ^:private now "The time on each thing, as the door adds it. No rule reads it." 1700000000000)
(def ^:private lane h/lane)

(def questions
  "The readings of the judge on which this store differs, each with what
  the difference is."
  {"reading 4" "one further up a lent chain ends, or lowers, a give that fell: the rules take it; the store refuses it, because the row of the ended give above no longer says what that give was lent from, so the store cannot tell that the author's give is above it"
   "reading 5" "a raise that no give could make: the rules hold a raise to what its author may pass on and to nothing else; the store also holds it to what a give is held to, so that a right is added to a lent give only under the give it is lent from, to a standing give only under a standing give, and to no give that covers more than the act's own"
   "reading 6" "an owner raises anyone's give past seeing by a change: the rules take it, since they hold anyone at all to seeing at the give alone; the store refuses it at the change too"
   "the bound" "a read asked once answers `:too-many-permissions` where the asker's runs hold more permissions than one turn examines (CONTRACT.md, section 2.4): the rules know no bound, and say whether she sees"})

;; ------------------------------------------------- a thing, said to the store

(declare said)

(defn- hex-of-the-text
  "The store's digest of the act that a history's text names, or the text
  itself when it names no act that anybody made."
  [named text]
  (or (when-let [maker (named text)]
        (digest/hex-for-a-yes (said named maker)))
      text))

(defn- said-value
  "What a give says, as the store takes it: no part that is nil, and the
  store's digest where the history's text stands."
  [named v]
  (let [v (into {} (remove (comp nil? val)) v)]
    (cond-> v
      (= :exact-act (first (:covers v)))
      (update :covers (fn [[kind text]] [kind (hex-of-the-text named text)])))))

(defn said
  "A thing of a history, or the act that a give for one exact act names, as
  a maker hands it to the store's door."
  [named thing]
  (cond-> {:version act/version
           :who (:who thing)
           :lane (:lane thing)
           :session (:session thing)
           :facts (mapv (fn [fact]
                          (if (and (= reference/permission-key (:k fact)) (map? (:v fact)))
                            (update fact :v #(said-value named %))
                            fact))
                        (:facts thing))}
    (some? (:permission thing)) (assoc :permission (:permission thing))))

(defn- through-the-door
  "The thing that the door appends for a maker's act
  (`softland.store.door.additions`). Throws when the door would append
  nothing: a history sends only what the door appends."
  [maker act-id]
  (let [reading (additions/reading maker act-id)
        built (when-not (:could-not reading) (additions/thing-to-append reading act-id now))]
    (or (:thing built)
        (throw (ex-info "the door appends nothing for this act" {:act maker :word (if (:could-not reading) reading built)})))))

(defn- told
  "What the door tells the sender of a thing when she reads her answer: the
  answer under the thing's act-id, held against the digest that the door
  gave her when it sent the thing."
  [rec thing]
  (let [[lane act-uuid] (:act-id thing)]
    (words/answer (record/answer-of (get (:answers rec) (record/answer-key lane act-uuid)))
                  (digest/readable-hex-of thing))))

;; --------------------------------------------------------------- the questions

(defn- takes-away?
  [ref fact]
  (let [target (reference/give ref (:e fact))]
    (and (= :change (reference/fact-kind ref fact))
         (not (reference/allows-within? (:allows target) (:allows (:v fact)))))))

(defn- through-an-ended-give?
  "True for a fact that ends, or lowers, a give which its author may end by
  the rules only as the holder of a give further up, where a give between
  the two was ended."
  [ref who under fact]
  (let [target (reference/give ref (:e fact))]
    (boolean
     (and under target
          (or (= :end (reference/fact-kind ref fact)) (takes-away? ref fact))
          (not (reference/ends-as-holder? ref who under target))
          (not (reference/ends-as-giver? ref who under target))
          (not (reference/ends-as-owner? ref who under target))
          (reference/ends-as-lender? ref who under target)
          (some #(reference/ended? ref %)
                (take-while #(not= (:id under) %) (reference/lent-chain ref (:e fact))))))))

(defn- raises-as-no-give-could?
  "True for a change that adds to what a give allows where a give of what
  is added could not be made in its place: the give is lent from another
  give than the one that the act names; or it stands on its own and the
  act's give is lent; or it covers more than the act's give."
  [ref under fact]
  (let [target (reference/give ref (:e fact))]
    (boolean
     (and under target
          (= :change (reference/fact-kind ref fact))
          (seq (reference/allows-added (:allows (:v fact)) (:allows target)))
          (or (and (:lent-from target) (not= (:lent-from target) (:id under)))
              (and (nil? (:lent-from target)) (some? (:lent-from under)))
              (not (reference/within-scope? target (reference/holds-now ref (:id under)))))))))

(defn- raises-anyone-past-seeing?
  [ref fact]
  (and (= :change (reference/fact-kind ref fact))
       (= :anyone (:holder (reference/give ref (:e fact))))
       (not= {:see true} (:allows (:v fact)))))

(defn question
  "The reading that a difference on a thing falls under, or nil. Takes the
  gate's state and the rules' state before the thing, the thing, the
  rules' judgment and the store's outcome."
  [_gs ref sent ruled outcome]
  (let [word (:verdict outcome)
        under (reference/give ref (:permission sent))]
    (when (and (:accept? ruled) (not= :accepted word))
      (cond
        (and (contains? #{:may-not-end-the-permission :may-not-lower-the-permission} word)
             (some #(through-an-ended-give? ref (:who sent) under %) (:facts sent)))
        "reading 4"

        (and (contains? #{:permission-not-lent-from-the-acts-own :made-under-a-lent-permission-is-lent
                          :permission-covers-more-than-the-acts-own} word)
             (some #(raises-as-no-give-could? ref under %) (:facts sent)))
        "reading 5"

        (and (= :anyone-only-sees word)
             (some #(raises-anyone-past-seeing? ref %) (:facts sent)))
        "reading 6"))))

(defn see-question
  "What a difference on what an asker may see falls under, or nil: the
  store's read answers `:too-many-permissions`, the word of its bound, and
  the gives that the asker's runs hold over the lane, her own and
  everyone's as far as she holds through everyone, ended ones apart, are
  more than the store examines in one turn. Held by that count in the
  rules' state, not by the two words alone: the word with fewer gives than
  the bound is a break. Added by the judge's session on 28 November 2026,
  when the store of Loop 3 brought the word."
  [_gs ref asker _session gate-word _rules-word]
  (when (and (= :too-many-permissions gate-word)
             (< lanes/max-holder-permissions
                (count (filter (fn [[id g]]
                                 (and (= [:default-group lane] (:covers g))
                                      (not (reference/ended? ref id))
                                      (reference/covers-party? (:holder g) asker)))
                               (:gives ref)))))
    "the bound"))

;; -------------------------------------------------------------------- the gate

(def gate
  "The store of the permission fix as a gate under test
  (`gate-histories.run`). Its state is `:rec`, the record."
  {:name "the store of the permission fix: its own door additions, walk and rights, against a record in memory"
   :profile :full
   :words? true
   :start {:rec memory/empty-record}
   :send (fn [gs _ref named sent]
           (let [act-id (:act-id sent)
                 [_ act-uuid] act-id
                 thing (through-the-door (said named sent) act-id)
                 answered? (some? (get (:answers (:rec gs)) (record/answer-key lane act-uuid)))]
             (if-let [word (when-not answered?
                             (cond (contains? act/everyone (:who sent)) :an-act-whose-author-is-everyone
                                   (= :place-named-twice (:refuse (declaration/check thing)))
                                   (let [ids (keep #(when (= reference/permission-key (:k %)) (:e %)) (:facts sent))]
                                     (if (= (count ids) (count (distinct ids)))
                                       :a-place-named-twice
                                       :a-permission-named-twice))))]
               {:skip word}
               (let [{:keys [record outcome]} (memory/decide (:rec gs) :streaming thing)]
                 {:verdict (cond (:repeat outcome) :act-id-answered
                                 (:stray outcome) [:stray (:stray outcome)]
                                 :else (:verdict outcome))
                  :decided? (contains? outcome :stamp)
                  :wrote? (not (and (identical? (:facts record) (:facts (:rec gs)))
                                    (identical? (:by-holder record) (:by-holder (:rec gs)))
                                    (identical? (:owners record) (:owners (:rec gs)))))
                  :told (told record thing)
                  :gs (assoc gs :rec record)}))))
   :sees (fn [gs pairs]
           (mapv (fn [[asker session]] (memory/may-see (:rec gs) lane asker session)) pairs))
   :excuse question
   :see-excuse see-question})

;; ---------------------------------------------- the store, one rule broken

(defn- without-word
  "In place of one of the store's checks: the check, but nil where it gives
  this word. So the store takes what it refused for that word."
  [word]
  (fn [real] (fn [& args] (let [said (apply real args)] (when-not (= word said) said)))))

(defn- another-word
  "In place of one of the store's checks: the check, with one word said as
  another."
  [word other]
  (fn [real] (fn [& args] (let [said (apply real args)] (if (= word said) other said)))))

(def broken-stores
  "The store of the permission fix with one rule broken on purpose, each as
  [what, the var, a function that takes the function in place and gives
  the broken one]. Each must be caught: a history must break a statement
  under it. The store's files are not changed: the function is put in
  place for the run only.

  Three words of the store are not here. No history sends what
  `:changing-what-a-permission-covers-not-built` and
  `:change-changes-nothing` refuse. And the store without `:no-permission`
  takes nothing more than the store with it: each fact of an act that
  names no permission is then refused by its own check, with another word
  (run: 2,000 histories under seed 1 showed no difference). The store's own
  proof holds that word.

  One broken copy is left out while the store has no
  `softland.store.read-path.lanes/holders-for`: \"a read looks for
  everyone's permissions for an agent too, and for nobody's own\". The
  store of Loop 3 finds a read's holders another way (`holder-plan`), and
  the copy over it is the builder's to write again. The judge's session
  made the entry one that is there only when the function is (28 November
  2026), so that this namespace loads against that store. That session's
  only other change to this file is `see-question`, with its line in
  `questions`."
  (into
   (vec (for [word [:permission-does-not-allow-writing :only-an-owner-writes-a-setting :store-key-takes-one-write
                    :a-permission-id-takes-one-permission :permission-not-given-by-the-acts-who
                    :permission-not-under-the-acts-own :permission-not-lent-from-the-acts-own
                    :made-under-a-lent-permission-is-lent :an-agent-or-a-run-only-borrows :anyone-only-sees
                    :anyone-signed-in-only-sees-and-writes
                    :permission-covers-more-than-the-acts-own :permission-gives-more-than-its-giver-may
                    :permission-lent-too-deep :a-change-keeps-holder-giver-and-lender :may-not-lower-the-permission
                    :no-permission-to-end :permission-already-ended :permission-already-used
                    :may-not-end-the-permission :permission-lent-from-one-the-act-ends :lane-would-have-no-owner]]
          [(str "the store takes what it refuses with " word) #'decision/places (without-word word)]))
   (filterv
    some?
    [["a lent permission still stands at the gate after the one above it ended"
     #'chain/stands?
     (fn [_] (fn [lane id rows] (some? (record/held-permission (get rows (chain/place lane id))))))]
    ["a chain reads as whole, at the gate and at a read, when a permission above has ended"
     #'chain/climb
     (fn [real]
       (fn [lane id rows]
         (let [climbed (real lane id rows)]
           (cond-> climbed
             (and (seq (:values climbed)) (not (:wants climbed))) (assoc :whole? true)))))]
    ["what is lent keeps what it allows when its lender is lowered"
     #'chain/allows-now
     (fn [_] (fn [values] (:allows (first values))))]
    ["an agent or a run holds what everyone holds, on its own"
     #'chain/held-by?
     (fn [_]
       (fn [holder party]
         (boolean (or (and (some? party) (= holder party))
                      (= :anyone holder)
                      (and (= :anyone-signed-in holder) (some? party))))))]
    ["an act is taken under a permission whoever sends it, and in whatever sitting"
     #'decision/holder-or-sitting-fault
     (fn [_] (fn [_act _says] nil))]
    ["a permission for one exact act takes any act"
     #'decision/hex-for-its-permission
     (fn [_]
       (fn [_thing act rows]
         (second (:covers (record/held-permission (get rows (decision/permission-place act)))))))]
    ["a permission for one exact act is used more than once: its use is not written"
     #'decision/use-of-its-permission
     (fn [_] (fn [& _] []))]
    ["an act under a permission that was never given is answered as under one that ended"
     #'decision/standing
     (another-word :permission-never-given :permission-ended)]
    ["an end of a permission that was never given is answered as an end of one that had ended"
     #'decision/places
     (another-word :no-permission-to-end :permission-already-ended)]
    ["a give and an end write nothing in the order by holder, nor among the owners"
     #'decision/permission-writes
     (fn [_] (fn [& _] []))]
    ["a read lets a holder see under a permission that allows writing and not seeing"
     #'lanes/sees-under?
     (fn [_]
       (fn [lane asker session values]
         (let [says (first values)]
           (boolean (and (chain/held-by? (:holder says) asker)
                         (= [:default-group lane] (:covers says))
                         (or (nil? (:session says)) (= session (:session says)))
                         (seq (chain/allows-now values)))))))]
    (when-let [holders-for (resolve 'softland.store.read-path.lanes/holders-for)]
      ["a read looks for everyone's permissions for an agent too, and for nobody's own"
       holders-for
       (fn [_] (fn [_asker] [:anyone :anyone-signed-in]))])])))
