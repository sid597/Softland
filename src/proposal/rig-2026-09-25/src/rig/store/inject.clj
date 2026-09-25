(ns rig.store.inject
  "Test-only hooks in the gate (P14, R3). The gate calls `point!` at named
  points for the offer it is handling: `:seen` once the record parsed,
  `:recorded` when its name's record answered it, `:before-writes` and
  `:after-writes` around the writes of a fresh decision.

  A test can arm a point for a name; the gate throws there, which Rama
  treats as fatal to the worker, so the worker restarts and the record
  replays: the in-process cluster's stand-in for a crash. A test can also
  watch a name, so every point the gate reaches for it is recorded, which is
  how a replay is observed. The in-process cluster runs every task in this
  JVM, so module code sees these atoms. A kept store would have no such
  hook.")

(defonce armed (atom {}))     ; [point name] -> times left to throw
(defonce fired (atom []))     ; [point name] in the order they fired
(defonce watched (atom #{}))  ; names whose every point is recorded
(defonce passes (atom []))    ; [point name] for watched names, in order

(defn arm!
  "Throw at `point` the next `times` times the gate reaches it for `nm`."
  ([point nm] (arm! point nm 1))
  ([point nm times] (swap! armed assoc [point nm] times)))

(defn watch!
  "Record every point the gate reaches for these names."
  [& nms]
  (swap! watched into nms))

(defn reset-all! []
  (reset! armed {}) (reset! fired []) (reset! watched #{}) (reset! passes []))

(defn count-of
  "How many times the gate reached `point` for the watched name `nm`."
  [point nm]
  (count (filter #{[point nm]} @passes)))

(defn fired-count
  "How many injected crashes fired at `point` for `nm`."
  [point nm]
  (count (filter #{[point nm]} @fired)))

(defn point!
  "Called by the gate at a named point for the offer it is handling."
  [point nm]
  (when (contains? @watched nm)
    (swap! passes conj [point nm]))
  (when (seq @armed)
    (let [k [point nm]
          left (get @armed k 0)]
      (when (pos? left)
        (swap! armed update k dec)
        (swap! fired conj k)
        (throw (ex-info "injected crash" {:point point :name nm})))))
  nil)
