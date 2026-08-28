(ns app.server.rama.ingest-epoch
  "Shared ingest epoch counter.
   Takes: increments from accepted imports and watcher activity.
   Gives: the current ingest epoch to readers that refresh derived views.
   Holds: !ingest-epoch-atom.")

;; Session-local notification only. Durable truth remains in Rama; this atom
;; carries no values, resets on restart, and has no rebuild obligation.
(def transitional-mirror-quarantine
  {:kernel-contract? false
   :durable? false
   :reset-on-restart? true
   :rebuild-path :none
   :durable-audit :compat-record-events
   :mirrors '[!ingest-epoch-atom]})

(defonce !ingest-epoch-atom (atom 0))
