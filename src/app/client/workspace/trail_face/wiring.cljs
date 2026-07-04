(ns app.client.workspace.trail-face.wiring
  "The ONLY cljs file under trail_face/ (S2 exempts it by design):
   atoms, watches, coverage loading, remote-pull glue. Everything below
   this file is pure (data, geometry) -> data.
   Seam law (S1): this file touches NO server names - the Electric pull
   happens in electric_flow/file_viewer; this glue only moves data
   between client atoms."
  (:require [app.client.workspace.trail-face.sanitize :as san]))

(defn load-coverage!
  "Fetch the REAL font_atlas.json once; pass the coverage set as DATA
   into the pure sanitizer layer (OP-1/OP-31). On failure the face
   renders unsanitized rather than not at all - logged loudly."
  [!trail-coverage]
  (-> (js/fetch "/font_atlas.json")
      (.then #(.json %))
      (.then (fn [j]
               (let [cov (san/coverage-set (js->clj j))]
                 (js/console.log "[TRAIL-FACE] atlas coverage loaded" (count cov))
                 (reset! !trail-coverage cov))))
      (.catch (fn [e]
                (js/console.warn "[TRAIL-FACE] coverage load FAILED - ops render unsanitized" e)))))

(defn install-trail-face-wiring!
  "Wire the trail-face pull loop (OP-29/OP-30 glue):
   - !trail-face-state (set by /trail command, expansion clicks) derives
     !trail-request - the value Electric watches and pulls against.
   - !trail-data (Electric pull result) splits into
     !trail-text / !trail-feed / !trail-bundles for the compute flows.
   - remote ingest epoch mirrors into (:!ingest-epoch atoms) and, after a
     client-side debounce >= 1s (INV-19 - the epoch PUSH is the only
     re-pull trigger; no polling anywhere), re-arms the request."
  [atoms {:keys [!trail-request !trail-data !ingest-epoch-remote]}]
  (let [{:keys [!trail-face-state !trail-text !trail-feed !trail-bundles
                !ingest-epoch]} atoms
        !last-pull-epoch (atom 0)
        !debounce (atom nil)
        request! (fn []
                   (let [s @!trail-face-state
                         addr (:address s)
                         params (when (and (seq? addr) (map? (second addr)))
                                  (second addr))
                         now (js/Date.now)
                         ;; arrival-side window ONLY (WP1 s9.10). An explicit
                         ;; window in the address is respected (pinned);
                         ;; otherwise a rolling last-24h window, re-stamped on
                         ;; each epoch re-pull so the feed stays near-live.
                         window (or (:window params)
                                    {:from-ms (- now 86400000) :to-ms now})]
                     (reset! !trail-request
                             (when (:face s)
                               {:face (:face s)
                                :address addr
                                :window window
                                :order (:order s :arrival)
                                ;; entry-keys are [target-id arrival-ms];
                                ;; the pull wants target ids
                                :expanded (vec (distinct (map first (:expanded s #{}))))
                                :epoch @!last-pull-epoch}))))]
    (add-watch !trail-face-state :trail-face-request
               (fn [_ _ _ _] (request!)))
    (when !ingest-epoch-remote
      (add-watch !ingest-epoch-remote :trail-face-epoch
                 (fn [_ _ _ new-epoch]
                   (when (number? new-epoch)
                     (reset! !ingest-epoch new-epoch)
                     (when-let [t @!debounce] (js/clearTimeout t))
                     (reset! !debounce
                             (js/setTimeout
                               (fn []
                                 (reset! !last-pull-epoch new-epoch)
                                 (request!))
                               1000))))))
    (when !trail-data
      (add-watch !trail-data :trail-face-data
                 (fn [_ _ _ data]
                   (when data
                     (when (contains? data :text)
                       (reset! !trail-text (:text data)))
                     (when (contains? data :feed)
                       (reset! !trail-feed (:feed data)))
                     (when (contains? data :bundles)
                       (reset! !trail-bundles (or (:bundles data) {})))))))
    (request!)))
