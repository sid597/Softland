(ns app.server.cascade
  "In-process cascade declarations and best-effort dispatch."
  (:require [clojure.tools.logging :as log]))

;; CONTRACT T2/T5: declarations are inert data. Trigger names identify
;; in-process emission points; neither rows nor triggers are durable truth.
(def ^:private declared-rows
  [{:cascade/id :cascade/material-autotag
    :cascade/trigger :episode/turn-durable
    :cascade/handler 'app.server-jetty/run-ambient-autotag!
    :cascade/effect-class :external-via-derived-worker
    :cascade/idempotency
    "run-id derived in material_circulation from (object-key, input-hash, autotag-version, salt) — identical input = one run forever; recorded runs never re-invoke the adapter; a NEW key is minted only by input change or explicit salt (no lifecycle transitions)."
    :cascade/actor "llm:material-autotag/v1"}])

(defn rows
  "Return the declaration table in fire order."
  []
  ;; CONTRACT T7: the acts stratum must remain enumerable.
  declared-rows)

(defn react!
  "Dispatch every row declared for `trigger`, preserving declaration order in
   the immediate receipts. Each handler runs in its own best-effort future."
  [ctx trigger payload]
  (->> (rows)
       (filter #(= trigger (:cascade/trigger %)))
       (mapv
        (fn [{:cascade/keys [id handler]}]
          (log/info "[CASCADE]"
                    {:cascade/id id
                     :cascade/trigger trigger})
          ;; CONTRACT T3/T4: retain the call site's future-per-act threading,
          ;; and isolate every resolution/invocation failure from sibling rows.
          (future
            (try
              ((requiring-resolve handler) ctx payload)
              (catch Throwable t
                (log/warn
                 "[CASCADE][FAILED]"
                 {:cascade/id id
                  :cascade/trigger trigger
                  :source-unit-id (:source-unit-id payload)
                  :error (.getMessage t)}))))
          {:cascade/id id
           :dispatched? true}))))
