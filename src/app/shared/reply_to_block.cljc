(ns app.shared.reply-to-block
  "Pure P8 seam for the resident verb born from the BANKED
   `reply to just this block` wish.

   This namespace owns no effects. It freezes the addressed subject into the
   durable request, narrows the portal OPEN to evidence worn by that subject,
   and composes the resident prompt from deterministic projections. The client
   and server both call the same narrowing law; a forged or stale client
   `:entity-id` therefore cannot widen the server-side briefing."
  (:require [clojure.string :as str]))

(def verb-name :resident/reply-to-block)
(def verb-version 1)
(def release-ref "softland://verb-release/resident-reply-to-block/v1")

(defn narrowed-portal-open
  "Return the only portal-open shape this verb may carry.

   `entity-id` is authoritative. All other wearer rows are discarded and the
   master set is derived from the surviving contribution stamps, never accepted
   as a caller-supplied widening knob. `:narrowed? true` tells the portal that
   an honestly empty master set means EMPTY rather than `open the registry`."
  [{:keys [entity-id wearers conversation-id]}]
  (let [here (->> wearers
                  (filter #(= entity-id (:wearer/entity-id %)))
                  vec)
        master-ids (->> here
                        (mapcat :wearer/facets)
                        (keep :wearer/master-id)
                        distinct
                        (sort-by str)
                        vec)]
    {:entity-id entity-id
     :wearers here
     :conversation-id conversation-id
     :master-ids master-ids
     :narrowed? true}))

(defn request
  "Build the durable request body for exactly `subject`.

   The caller supplies mechanism measurements (turn/time/position/thread);
   this function makes the deictic guarantee testable: source id, content, and
   portal evidence all name the same subject. Blank material yields nil."
  [{:keys [subject text position turn-id time-ms scene-context prev-turn-id
           thread-id conversation-id wearers]}]
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
               :conversation-id conversation-id})}
      thread-id (assoc :thread-id thread-id)
      conversation-id (assoc :conversation-id conversation-id))))

(defn compose-resident-prompt
  "The resident sees durable lane inheritance, then the exact narrowed portal
   briefing, then the inhabitant's pinned utterance. Nil projections are empty;
   the raw turn record remains the unprefixed `text`."
  [episode-seed portal-briefing text]
  (str (or episode-seed "") (or portal-briefing "") (or text "")))
