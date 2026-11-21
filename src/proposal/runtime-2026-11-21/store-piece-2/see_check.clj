;; The generated histories of gate-histories/, with the statement `see` asked
;; again after piece 2 of the store's line, and two mutants more.
;;
;; Written by the Claude session that finished piece 2 (21 November 2026). It
;; changes no file of gate-histories/, whose working tree holds another
;; session's uncommitted work: it loads that code and puts three things in
;; place for the run only.
;;
;;   1. `seeing`: the adapter to the read path. The read path's `standing`
;;      now takes whether the asker holds a permission that stands, of either
;;      kind (`softland.store.read-path.layers/either`), so the adapter of
;;      gate-histories threw an ArityException and the run turned `see` off.
;;      This one calls the read path as `softland.store.read-path.sources`
;;      does: `permission-state`, `run-permission-state`, `either`, `standing`.
;;   2. `see-violation`: asks for each asker, signed in and not, in no run and
;;      in each run of a history. The statements are the other session's own
;;      `gate-histories.reference/may-see`, which already takes a run (R8).
;;   3. two mutants: R8 (a run's permission lets any asker in that run see)
;;      and R10 (a permission for a run or an act gives).
;;
;; Run from src/proposal/runtime-2026-11-21/gate-histories:
;;   clojure -J-Xss16m -M -i ../store-piece-2/see_check.clj -e "(gate-histories.run/-main \"20000\" \"20261121\")"
;;   clojure -J-Xss16m -M -i ../store-piece-2/see_check.clj -e "(gate-histories.run/-main \"mutants\" \"3000\" \"1\")"
;;
;; It belongs in gate-histories/ itself, in `run.clj`, when the session that
;; owns that folder takes it: until then it is here, beside the piece that it
;; checks.

(require '[gate-histories.run :as run]
         '[gate-histories.reference :as reference]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.gate.chain :as chain]
         '[softland.store.gate.decision]
         '[softland.store.read-path.layers :as layers]
         '[softland.store.record.rows :as record])

(defn seeing
  "What the read path says of one asker and the layer, from the record, for
  a read asked in `run`, or in no run when it is nil."
  [facts who run signed-in?]
  (let [layer run/layer
        own-rows (into {}
                       (keep (fn [k] (let [place (record/place-key layer layer k)]
                                       (when-let [row (get facts place)] [place row]))))
                       [:softland.store/depot :softland.store/who-may-see
                        :softland.store/builds-on :softland.store/marked-keys])
        rows-above (fn [row]
                     (if-let [held (record/held-permission row)]
                       (into {} (map (fn [k] [k (get facts k)])) (chain/places-above layer held))
                       {}))
        permission-row (get facts (record/place-key layer who :softland.store/permission))
        for-the-layer (layers/permission-state layer permission-row (rows-above permission-row))
        run-row (when run (get facts (record/place-key layer run :softland.store/run-permission)))
        for-the-run (if run
                      (layers/run-permission-state layer who run-row (rows-above run-row))
                      :none)]
    (layers/standing layer own-rows (layers/either for-the-layer for-the-run) signed-in?)))

(defn see-violation
  "The statement `see` after one decision. Gives nil, or the first asker
  and run for whom the read path and the statements differ. A read names a
  run only with an asker, so one who is not signed in asks in no run."
  [facts ref]
  (first
   (for [who @#'run/askers
         signed-in? [true false]
         in-run (if signed-in? (cons nil run/runs) [nil])
         :let [read-path (seeing facts who in-run signed-in?)
               statements (reference/may-see ref who in-run signed-in?)]
         :when (not= read-path statements)]
     {:statement :see
      :what (str "the read path says " (name read-path) " and the statements say " (name statements)
                 " for " (name who) (when in-run (str " in " (name in-run)))
                 (when-not signed-in? ", not signed in"))})))

(alter-var-root #'run/see-violation (constantly see-violation))
(reset! run/see-asked? true)

(alter-var-root
 #'run/mutants
 into
 [["R8: a run's permission lets any asker who asks in that run see"
   #'softland.store.read-path.layers/run-permission-state
   (fn [layer _asker run-row chain-rows]
     (if-let [held (record/held-permission run-row)]
       (if (chain/stands? layer held (assoc chain-rows (chain/place layer held) run-row)) :stands :none)
       :none))]
  ["R10: a permission for a run or for an act gives"
   #'softland.store.gate.decision/permission-fact-fault
   (fn [act fact]
     (let [given (:v fact)]
       (when (and (contains? declaration/permission-keys (:k fact)) (some? given))
         (cond
           (not= (:layer act) (nth given 1)) :permission-fact-for-another-layer
           (not= (:layer act) (nth given 2)) :permission-fact-granted-in-another-layer
           (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
           :else nil))))]])
