(ns judge-codex-projection
  "Diagnostic caller-argument substitution; no builder source is changed."
  (:require [app.client.harness.core :as core]
            [app.client.region3d.on-plane-renderer :as placed]))

(defn init []
  (let [original placed/prepare-placements!
        calls (atom [])]
    ;; The existing tree fixture has an 80 x 88 encode viewport and a
    ;; 256 x 256 compositor lease. This is a bounded causal experiment,
    ;; not a proposed rule for reconstructing leases in production.
    (set! placed/prepare-placements!
          (fn [system prior placements maintained camera cache]
            (let [change? (and (= [80 88] (:viewport camera))
                               (seq placements))
                  actual (cond-> camera change? (assoc :viewport [256 256]))]
              (when change?
                (swap! calls conj {:before (:viewport camera)
                                   :after (:viewport actual)}))
              (original system prior placements maintained actual cache))))
    (-> (core/run-region3d-floor-harness!)
        (.then (fn [result]
                 (set! (.-__renderVerifierResult js/window)
                       (clj->js (assoc result :judge-experiment
                                       {:argument-substitutions @calls})))
                 (set! (.-__renderVerifierDone js/window) true)))
        (.catch (fn [error]
                  (set! (.-__renderVerifierResult js/window)
                        #js {:fatal (str error) :stack (.-stack error)})
                  (set! (.-__renderVerifierDone js/window) true)))
        (.finally (fn [] (set! placed/prepare-placements! original))))))
