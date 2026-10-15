(ns softland.runtime.start
  "The runtime's start, as steps that are safe to run again: the base and
  the system's layers made when missing, their settings written, the
  system's own identity and name, and this start recorded as a session of
  the system. Pure code. It needs no cluster.

  Only the runtime's own steps act as the system: making the base, and
  making a person and her layers at her first sign-in (BUILD.md, default
  17). These are the first of those steps. The step function gives the
  next thing to do from what is known (`softland.runtime.steps`); a caller
  drives it at each start of the runtime, before any sign-in.

  In order:

    1. the base, made by the system: `[:system :base :base]`, the base's
       first permission. Its depot is microbatch
       (`softland.store.gate.first-act/depot-at-making`);
    2. who may see the base: `:everyone`, written when no fact says it
       (Sid's decision 8 of 15 October 2026; BUILD.md, default 16). A fact
       already there is left as it is;
    3. for each system layer (`softland.runtime.ids/system-layers`): made
       with the system as its top, then its settings: the keys it marks as
       taking one write (`softland.runtime.records/one-write-keys`), and who
       may see it, `:members`, so only those given a right see it. A
       setting that differs is written again, so a new version of the
       runtime's records marks its keys at its first start;
    4. the system's identity in `identities`, and its name, \"system\";
    5. the rules' defaults, when the input gives them: in `rules`, the limit
       of acts a minute for each kind of actor (`:rule/acts-per-minute` on
       `:person`, `:agent`, `:program`; BUILD.md, default 15) and of bytes
       a minute (`:rule/bytes-per-minute`, the build session's), the wait
       before a freed name is taken again (`:rule/name-wait-ms` on `:names`;
       default 2), how many blocked attempts are kept whole
       (`:rule/keep-first` on `:blocked`; default 14), and how long a
       person's sitting lasts (`:rule/sitting-ms` on `:person`, the build
       session's). Each is written when
       its place holds no fact, in one act; a rule already there, or ended
       by a retract, is left as it is, so a rule that Sid changed is not
       written over at the next start;
    6. this start, as a session of the system in its record layer
       (PROGRESS.md: each start of the runtime is a session).

  The input is {:system-session uuid}: the id of this start's session, made
  by the caller at random. Every act of the start names it as its session.
  It can hold `:rules`, the defaults of step 5:

    {:acts-per-minute  {:person n :agent n :program n}
     :bytes-per-minute {:person n :agent n :program n}
     :name-wait-ms n
     :keep-first n
     :sitting-ms n}

  each part optional, each n a whole number from zero. With no `:rules`,
  step 5 writes nothing. The numbers are the caller's
  (`softland.runtime.service/rule-defaults`).

  Inputs and results, by call:

    start-step  the input, what is known -> the next thing to do; done is
                {:do :done :session uuid}

  The words of a refusal, beside the steps' own: `words`.

  State and effects. None. Each function is pure.

  Callers. `softland.runtime.service/start!`, which drives it on a cluster
  at each start of the runtime, with the rules' defaults; and the pure
  proof, on a model of the store, without them."
  (:require [softland.runtime.ids :as ids]
            [softland.runtime.records :as records]
            [softland.runtime.steps :as steps]))

(def words
  "The words with which the start stops, beside those of the steps
  (`:read-failed`, `:step-refused`, `:act-not-answered`):

    :input-not-whole          the system's session is no UUID
    :layer-not-the-systems    a layer of the system, or the base, is made
                              and its top is not the system
    :system-identity-not-whole  the system's identity holds another kind,
                              or another record layer
    :system-name-taken        the name \"system\" points at another
    :rules-not-whole          the `:rules` of the input is not of its form"
  [:input-not-whole :layer-not-the-systems :system-identity-not-whole :system-name-taken
   :rules-not-whole])

(defn- made-by-the-system
  "The step that makes a layer whose top is the system, when it is not
  made. Takes the layer. Reads the system's permission in it: the first
  permission `[:system layer layer]` means made; a layer left out as not
  made is made; anything else stops the start."
  [layer]
  [[:made layer]
   (fn [ctx]
     (let [q (steps/place-query ids/system layer ids/system :softland.store/permission)]
       (steps/reading ctx [q]
         (fn [[a]]
           (cond
             (= [ids/system layer layer] (steps/value-at a)) [:go ctx]
             (= :not-made (steps/left-out-why a layer)) (steps/acting ctx (steps/making-act ctx layer ids/system) ctx)
             :else [:refuse :layer-not-the-systems {:layer layer}])))))])

(def ^:private base-seen-by-everyone
  "The step that writes who may see the base, `:everyone`, when no fact
  says it."
  [[:base-seen-by-everyone]
   (fn [ctx]
     (let [q (steps/place-query ids/system ids/base ids/base :softland.store/who-may-see)]
       (steps/reading ctx [q]
         (fn [[a]]
           (if (= :no-fact (steps/holds-at a))
             (steps/acting ctx (steps/system-act ctx ids/base [{:e ids/base :k :softland.store/who-may-see :v :everyone}]) ctx)
             [:go ctx])))))])

(defn- settings-of
  "Takes a system layer. Gives the settings that the runtime writes on it:
  the keys it marks as taking one write, and who may see it."
  [layer]
  {:softland.store/one-write-keys (records/one-write-keys layer)
   :softland.store/who-may-see :members})

(defn- settings
  "The step that writes a system layer's settings that are missing or
  differ (`settings-of`), in one act."
  [layer]
  [[:settings layer]
   (fn [ctx]
     (let [q (steps/entity-query ids/system layer layer true)]
       (steps/reading ctx [q]
         (fn [[a]]
           (let [held (steps/record-of a layer)
                 facts (into [] (keep (fn [[k v]] (when (not= v (get held k)) {:e layer :k k :v v})))
                             (sort-by str (settings-of layer)))]
             (if (empty? facts)
               [:go ctx]
               (steps/acting ctx (steps/system-act ctx layer facts) ctx)))))))])

(def ^:private system-record
  "The system's identity, as `softland.runtime.records` has it."
  {:identity/kind :system
   :identity/name "system"
   :identity/record-layer ids/system-record})

(def ^:private system-identity
  "The step that writes the system's identity when it is not there."
  [[:system-identity]
   (fn [ctx]
     (let [q (steps/entity-query ids/system ids/identities ids/system false)]
       (steps/reading ctx [q]
         (fn [[a]]
           (let [held (steps/record-of a ids/system)]
             (cond
               (empty? held)
               (steps/acting ctx (steps/system-act ctx ids/identities
                                                   (:ok (records/facts-of :system-identity ids/system system-record)))
                             ctx)

               (= (dissoc system-record :identity/name) (dissoc held :identity/name)) [:go ctx]
               :else [:refuse :system-identity-not-whole {:held held}]))))))])

(def ^:private system-name
  "The step that claims the name \"system\" for the system when it is not
  claimed."
  [[:system-name]
   (fn [ctx]
     (let [entity (ids/name-entity "system")
           q (steps/place-query ids/system ids/names entity :name/of)]
       (steps/reading ctx [q]
         (fn [[a]]
           (cond
             (= ids/system (steps/value-at a)) [:go ctx]
             (= :no-fact (steps/holds-at a))
             (steps/acting ctx (steps/system-act ctx ids/names (:ok (records/facts-of :name entity {:name/of ids/system}))) ctx)
             :else [:refuse :system-name-taken])))))])

(defn- rule-facts
  "Takes the `:rules` of the input. Gives {:ok facts}, the facts of each
  default that it gives, each a record of `softland.runtime.records` on its
  entity in `rules`; or {:refuse word :at where} for a part that is not of
  its form. Nil or an empty map gives no fact."
  [rules]
  (if-not (and (or (nil? rules) (map? rules))
               (every? #(let [v (get rules %)] (or (nil? v) (map? v))) [:acts-per-minute :bytes-per-minute]))
    {:refuse :record-not-in-shape :at :rules}
    (let [wanted (concat (for [[kind n] (:acts-per-minute rules)]
                           [:limit kind {:rule/acts-per-minute n}])
                         (for [[kind n] (:bytes-per-minute rules)]
                           [:byte-limit kind {:rule/bytes-per-minute n}])
                         (when (contains? rules :sitting-ms)
                           [[:sitting :person {:rule/sitting-ms (:sitting-ms rules)}]])
                         (when (contains? rules :name-wait-ms)
                           [[:name-wait :names {:rule/name-wait-ms (:name-wait-ms rules)}]])
                         (when (contains? rules :keep-first)
                           [[:keep-first :blocked {:rule/keep-first (:keep-first rules)}]]))]
      (reduce (fn [acc [record-name entity record]]
                (cond
                  (and (contains? #{:limit :byte-limit} record-name)
                       (not (contains? #{:person :agent :program} entity)))
                  (reduced {:refuse :record-not-in-shape :at entity})

                  :else
                  (let [got (records/facts-of record-name entity record)]
                    (if (:ok got) (update acc :ok into (:ok got)) (reduced got)))))
              {:ok []}
              wanted))))

(def ^:private rules-defaults
  "The step that writes each rule's default whose place holds no fact, in
  one act (the namespace docstring, step 5)."
  [[:rules-defaults]
   (fn [ctx]
     (let [wanted (:ok (rule-facts (get-in ctx [:in :rules])))]
       (if (empty? wanted)
         [:go ctx]
         (let [queries (mapv (fn [{:keys [e k]}] (steps/place-query ids/system ids/rules e k)) wanted)]
           (steps/reading ctx queries
             (fn [answers]
               (let [missing (keep (fn [[f a]] (when (= :no-fact (steps/holds-at a)) f))
                                   (map vector wanted answers))]
                 (if (empty? missing)
                   [:go ctx]
                   (steps/acting ctx (steps/system-act ctx ids/rules missing) ctx)))))))))])

(def ^:private this-start
  "The step that records this start as a session of the system, in the
  system's record layer. The session is fresh, so there is nothing to read:
  once its act is accepted the start is done."
  [[:this-start]
   (fn [ctx]
     (let [s (get-in ctx [:in :system-session])
           facts (:ok (records/facts-of :session s {:session/of ids/system
                                                    :session/kind :program-run
                                                    :session/made-by ids/system}))]
       (steps/acting ctx (steps/system-act ctx ids/system-record facts) ctx)))])

(def steps
  "The start's steps, in order."
  (into [(made-by-the-system ids/base) base-seen-by-everyone]
        (concat (mapcat (fn [layer] [(made-by-the-system layer) (settings layer)]) ids/system-layers)
                [system-identity system-name rules-defaults this-start])))

(defn start-step
  "Takes the input, {:system-session uuid} and maybe `:rules` (the
  namespace docstring), and what is known (`softland.runtime.steps`). Gives
  the next thing to do, or done: {:do :done :session uuid}. Gives
  {:do :refuse :word :input-not-whole} when the session is no UUID, and
  `:rules-not-whole`, with where, when a rule's default is not of its form."
  [input known]
  (cond
    (not (uuid? (:system-session input))) {:do :refuse :word :input-not-whole}
    (:refuse (rule-facts (:rules input))) {:do :refuse :word :rules-not-whole
                                           :at (:at (rule-facts (:rules input)))}
    :else
    (steps/run-steps steps {:in input :known known}
                     (fn [ctx] {:do :done :session (get-in ctx [:in :system-session])}))))
