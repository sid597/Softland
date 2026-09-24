(ns rig.store.inject
  "Test-only crash hooks (P14, R3). A test arms a point for a name; the gate
  throws there, which Rama treats as fatal to the worker, so the worker
  restarts and the record replays: the in-process cluster's stand-in for a
  crash. The in-process cluster runs every task in this JVM, so module code
  sees the atom. A kept store would have no such hook.")

(defonce armed (atom {}))  ; [point name] -> times left to throw
(defonce fired (atom []))  ; [point name] in the order they fired

(defn arm!
  "Throw at `point` the next `times` times the gate reaches it for `nm`."
  ([point nm] (arm! point nm 1))
  ([point nm times] (swap! armed assoc [point nm] times)))

(defn reset-all! [] (reset! armed {}) (reset! fired []))

(defn point!
  "Called by the gate at a named point for the offer it is deciding."
  [point nm]
  (when (seq @armed)
    (let [k [point nm]
          left (get @armed k 0)]
      (when (pos? left)
        (swap! armed update k dec)
        (swap! fired conj k)
        (throw (ex-info "injected crash" {:point point :name nm})))))
  nil)
