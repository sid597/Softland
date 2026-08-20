(ns app.server.rama.util-fns)

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
