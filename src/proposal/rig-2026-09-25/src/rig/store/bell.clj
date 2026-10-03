(ns rig.store.bell
  "Push for a standing reader (the pointer screen): Rama proxies on the three
  things in the store that change when something a reader holds may have
  changed. A proxy is pushed each change by the store; nothing here polls.

  - A one-owner layer's bell, `[L :last-admitted]` in `$$layers`: the stamp
    of the layer's last admitted act, set in the stream gate's one write
    group (rig.store.gate-event `write-decided>`).
  - The micro store's settled frontier, `[:frontier]` in `$$micro-task`,
    which every task holds: it moves when a batch is visible on every task,
    so it rings for every shared layer at once.
  - A person's entry, `[p]` in `$$persons`, on one task: a person forget's
    fan-out rewrites it on every task after it destroys the person's lock
    there, so it rings once the values that die with them are erased on
    that task (a ring from the forget act alone could come before).

  A ring is not a read. It carries a stamp, a frontier or a person entry,
  never a fact or a value, and the reader shows nothing from it: what it
  shows after a ring it reads through the one exit (rig.store.read-exit),
  which records the read. Callbacks run on Rama's proxy threads; a caller
  hands the ring to its own thread and returns at once."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx]))

(defn- ring-on
  "A proxy callback that calls `f` with the new value when it differs from
  the old one, and returns nil."
  [f]
  (fn [new-val _diff old-val]
    (when (not= new-val old-val) (f new-val))
    nil))

(defn layer-bell
  "A proxy on one-owner layer `layer`'s last admitted stamp. `f` is called
  with the new stamp on each change. Blocks until the proxy holds its first
  value; returns the ProxyState, which the caller closes with `close!`."
  [store layer f]
  (foreign-proxy [(keypath layer :last-admitted)] (:layers store) {:callback-fn (ring-on f)}))

(defn frontier-bell
  "A proxy on the micro store's settled frontier as task 0's key routes it
  (any task holds the frontier). `f` is called with the new frontier on each
  change. Returns the ProxyState."
  [store f]
  (foreign-proxy [(keypath :frontier)] (:task store) {:pkey 0 :callback-fn (ring-on f)}))

(defn task-keys
  "One key per task, found once: keywords `:bell-0`, `:bell-1`, ... until each
  task id the module has is routed to by one of them, as the micro store's
  own `micro-client/task-of` reports the routing (the hash every partitioner
  here shares). A vector indexed by task id."
  [store]
  (let [n (rx/task-count store)]
    (loop [i 0 found {}]
      (if (or (= n (count found)) (> i (* 64 n)))
        (mapv found (range n))
        (let [k (keyword (str "bell-" i))
              t (mc/task-of store k)]
          (recur (inc i) (if (contains? found t) found (assoc found t k))))))))

(defn person-bell
  "A proxy on person `p`'s entry as the task that `pkey` routes to holds it.
  `f` is called with the new entry on each change (a forget sets
  `:erased-at` and clears `:lock`). Returns the ProxyState."
  [store p pkey f]
  (foreign-proxy [(keypath p)] (:persons store) {:pkey pkey :callback-fn (ring-on f)}))
