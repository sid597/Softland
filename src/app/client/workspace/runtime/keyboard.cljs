(ns app.client.workspace.runtime.keyboard
  "Global keyboard consumer. The ground owns the whole key grammar
   (first-light A P2b T9): Escape blurs the focused block or discards the
   ephemeral anchor — an abandoned anchor leaves NOTHING (moment 1). The dev
   workspace's editor/panel/settings consumers died with that surface
   (dead-path census, Sid's ruling 2026-08-15)."
  (:require [missionary.core :as m]
            [app.client.workspace.ground :as ground]))

(defn global-keys-consumer
  [_atoms <global-keys]
  (->> <global-keys
       (m/reduce
         (fn [_ event]
           (when (and event (ground/ground-active?))
             (case (:type event)
               :escape (ground/escape!)
               nil))
           nil)
         nil)))
