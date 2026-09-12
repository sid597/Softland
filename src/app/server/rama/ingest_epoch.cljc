(ns app.server.rama.ingest-epoch
  "Process-local notification counter shared by selected write callers.
   Ingest, page, worn, and episode code increment !ingest-epoch-atom after selected
   imports or edits. No presentation subscription was traced in the current source;
   an increment alone does not establish view refresh. The atom contains no material
   or durable progress, resets on process restart, and does not observe every Rama
   write. The quarantine map describes this transitional notification role.")

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
