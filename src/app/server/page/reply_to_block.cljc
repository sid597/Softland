(ns app.server.page.reply-to-block
  "A resident reply narrowed to one addressed block.
   Takes: an addressed block id, portal data, and resident context.
   Gives: a narrowed durable request and deterministic resident prompt.
   Holds nothing."
  (:require [app.server.worn.invocation-material :as invocation-material]
            [clojure.string :as str]))

(def verb-name :resident/reply-to-block)
(def verb-version 1)
(def release-ref "softland://verb-release/resident-reply-to-block/v1")

(defn- bounded-wearers
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
  "Return the only portal-open shape this verb may carry.

   `entity-id` is authoritative. All other wearer rows are discarded and the
   master set is derived from the surviving contribution stamps, never accepted
   as a caller-supplied widening knob. `:narrowed? true` tells the portal that
   an honestly empty master set means EMPTY rather than `open the registry`."
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
  "Build the durable request body for exactly `subject`.

   The caller supplies mechanism measurements (turn/time/position/thread);
   this function makes the deictic guarantee testable: source id, content, and
   portal evidence all name the same subject. Blank material yields nil."
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
