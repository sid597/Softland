(ns app.server.page.reply-to-block
  "Pure construction of an addressed resident reply and its prompt.
   Build request data from a subject, text and caller-measured turn/position
   context. Portal evidence includes the addressed wearer and the bounded
   preceding context selected by worn/invocation-material. Master ids are
   derived from those retained rows. No request is appended here.
   Prompt composition concatenates supplied episode seed, optional precontext
   label, portal briefing and text; it performs no reads or model calls and
   retains no state."
  (:require [app.server.worn.invocation-material :as invocation-material]
            [clojure.string :as str]))

(def verb-name :resident/reply-to-block)
(def verb-version 1)
(def release-ref "softland://verb-release/resident-reply-to-block/v1")

(defn- bounded-wearers
  "Keep every row for entity-id plus at most precontext-depth rows preceding
   its first occurrence in the supplied order. Return [] if the subject is absent;
   this selects by row position, not by a durable conversation query."
  [wearers entity-id precontext]
  (let [wearers (vec (or wearers []))
        depth (invocation-material/precontext-depth precontext)
        addressed-index
        (first
         (keep-indexed
          (fn [i row]
            (when (= entity-id (:wearer/entity-id row)) i))
          wearers))
        addressed
        (filterv #(= entity-id (:wearer/entity-id %)) wearers)]
    (if (nil? addressed-index)
      []
      (let [start (max 0 (- addressed-index depth))
            preceding (subvec wearers start addressed-index)]
        (into preceding addressed)))))

(defn narrowed-portal-open
  "Build portal parameters around the authoritative entity-id.
   Keep its wearer rows plus bounded preceding rows selected by precontext;
   without an addressed row, keep none. Derive master-ids from retained facets
   and mark :narrowed? true so an empty set does not request the full registry.
   Caller-supplied master ids cannot widen this selection."
  [{:keys [entity-id wearers conversation-id precontext]}]
  (let [here (bounded-wearers wearers entity-id precontext)
        master-ids (->> here
                        (mapcat :wearer/facets)
                        (keep :wearer/master-id)
                        distinct
                        (sort-by str)
                        vec)]
    {:entity-id entity-id
     :wearers here
     :conversation-id conversation-id
     :precontext precontext
     :master-ids master-ids
     :narrowed? true}))

(defn request
  "Build reply request data for `subject`, or nil for a missing subject or
   blank text. Copy caller-supplied turn/time/position/scene fields and optional
   thread/conversation ids; narrow the supplied wearer evidence for portal-open.
   Does not validate the subject's durable existence or append the request."
  [{:keys [subject text position turn-id time-ms scene-context prev-turn-id
           thread-id conversation-id wearers precontext]}]
  (when (and (some? subject) (not (str/blank? (or text ""))))
    (cond-> {:source-unit-id subject
             :content-text text
             :position position
             :turn-id turn-id
             :time-ms time-ms
             :scene-context scene-context
             :prev-turn-id prev-turn-id
             :portal-open
             (narrowed-portal-open
              {:entity-id subject
               :wearers wearers
               :precontext precontext
               :conversation-id conversation-id})}
      thread-id (assoc :thread-id thread-id)
      conversation-id (assoc :conversation-id conversation-id))))

(defn compose-resident-prompt
  "The resident sees durable lane inheritance, then the exact narrowed portal
   briefing, then the inhabitant's pinned utterance. Nil projections are empty;
   the raw turn record remains the unprefixed `text`."
  ([episode-seed portal-briefing text]
   (compose-resident-prompt episode-seed portal-briefing text nil))
  ([episode-seed portal-briefing text precontext]
   (str (or episode-seed "")
        (when (contains? invocation-material/precontext-vocabulary precontext)
          (str "[invocation precontext · " precontext "]\n"))
        (or portal-briefing "")
        (or text ""))))
