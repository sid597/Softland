(ns runtime.model.run
  "Runs test.check over random interleavings of multi-copy sign-ins, mid-flight
   runtime crashes, depot re-classing, retries, agent branch layers, permission
   delegation/revocation/re-giving, live watches, trails, and multi-act promotes.

   Usage:
     cd src/proposal/runtime-2026-11-21/model
     clojure -M:run [num-tests] [seed]"
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [runtime.model.core :as m]
            [runtime.model.properties :as props]))

(def gen-copy (gen/elements [:c0 :c1]))
(def gen-subject (gen/elements ["sub-alice" "sub-bob"]))
(def gen-entity (gen/elements [:e0 :e1 :e2]))
(def gen-val (gen/elements ["v1" "v2" "v3" "v4"]))

(def gen-step
  "Generates abstract steps parameterized by small indices into whatever sessions,
   layers, and permissions currently exist in the state, so random walks hit
   deep stateful combinations constantly."
  (gen/frequency
   [[14 (gen/tuple (gen/return :sign-in) gen-copy gen-subject gen/boolean)]
    [16 (gen/tuple (gen/return :send-note) gen-copy gen/nat gen/nat gen-entity gen-val
                   (gen/frequency [[9 (gen/return false)] [1 (gen/return true)]]))]
    [8  (gen/tuple (gen/return :retry-intent) gen-copy gen/nat)]
    [5  (gen/tuple (gen/return :reclass-layer) gen/nat)]
    [5  (gen/tuple (gen/return :rotate-secret) gen/nat)]
    [8  (gen/tuple (gen/return :attach-agent) gen-copy gen/nat gen/nat gen/boolean)]
    [7  (gen/tuple (gen/return :subgrant) gen-copy gen/nat gen/nat)]
    [8  (gen/tuple (gen/return :end-perm) gen-copy gen/nat gen/nat gen/nat)]
    [8  (gen/tuple (gen/return :regive-perm) gen-copy gen/nat gen/nat gen/nat)]
    [6  (gen/tuple (gen/return :write-settings) gen-copy gen/nat gen/nat
                   (gen/elements [:anyone :holders]))]
    [8  (gen/tuple (gen/return :open-watch) gen-copy gen/nat (gen/elements [:layer :trail]) gen/nat)]
    [6  (gen/tuple (gen/return :end-session) gen/nat)]
    [7  (gen/tuple (gen/return :read-layer) gen/nat gen/nat)]
    [5  (gen/tuple (gen/return :read-trail) gen/nat gen/nat)]
    [5  (gen/tuple (gen/return :move-chunk) gen/nat gen/nat)]
    [8  (gen/tuple (gen/return :promote) gen-copy gen/nat gen/nat gen/boolean)]
    [4  (gen/tuple (gen/return :crash-copy) gen-copy)]]))

(defn- sort-det [coll]
  (sort-by pr-str coll))

(defn- nth-mod [coll idx]
  (when (seq coll)
    (nth (vec coll) (mod idx (count coll)))))

(defn apply-step [st step]
  (let [op (first step)
        sids (sort-det (keys (get-in st [:auth :sessions])))
        lids (sort-det (keys (get-in st [:store :layers])))
        branch-lids (sort-det (for [[lid lm] (get-in st [:store :layers])
                                    :when (get-in lm [:settings :builds-on])]
                                lid))]
    (case op
      :sign-in
      (let [[_ copy sub crash?] step]
        (m/op-sign-in st copy sub crash?))

      :send-note
      (let [[_ copy s-idx l-idx e v spoof?] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid)
          (let [actor (get-in st [:auth :sessions sid :actor])
                lm (get-in st [:store :layers lid])
                actor-pids (sort-det (filter #(= actor (m/perm-holder %)) (keys (:perms lm))))
                pid (or (nth-mod actor-pids s-idx) [actor lid lid])]
            (m/op-send-note st copy sid lid pid e v spoof?))
          st))

      :retry-intent
      (let [[_ copy idx] step]
        (m/op-retry-intent st copy idx))

      :reclass-layer
      (let [[_ l-idx] step
            lid (nth-mod lids l-idx)]
        (if lid (m/op-reclass-layer st lid) st))

      :rotate-secret
      (let [[_ s-idx] step
            sid (nth-mod sids s-idx)]
        (if sid (m/op-rotate-secret st sid) st))

      :attach-agent
      (let [[_ copy s-idx l-idx shared?] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid) (m/op-attach-agent st copy sid lid shared?) st))

      :subgrant
      (let [[_ copy s-idx l-idx] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid) (m/op-subgrant-permission st copy sid lid) st))

      :end-perm
      (let [[_ copy s-idx l-idx p-idx] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid)
          (let [non-root-pids (sort-det (filter #(some? (m/perm-parent %))
                                                (keys (get-in st [:store :layers lid :perms]))))
                target-pid (nth-mod non-root-pids p-idx)]
            (if target-pid (m/op-end-permission st copy sid lid target-pid) st))
          st))

      :regive-perm
      (let [[_ copy s-idx l-idx p-idx] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid)
          (let [ended (sort-det (get-in st [:store :layers lid :ended-perms]))
                old-pid (nth-mod ended p-idx)]
            (if old-pid (m/op-regive-permission st copy sid lid (m/perm-holder old-pid)) st))
          st))

      :write-settings
      (let [[_ copy s-idx l-idx vis] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid) (m/op-write-settings st copy sid lid vis) st))

      :open-watch
      (let [[_ copy s-idx kind t-idx] step
            sid (nth-mod sids s-idx)
            target-id (if (= :layer kind) (nth-mod lids t-idx) (nth-mod sids t-idx))]
        (if (and sid target-id) (m/op-open-watch st copy sid [kind target-id]) st))

      :end-session
      (let [[_ s-idx] step
            sid (nth-mod sids s-idx)]
        (if sid (m/op-end-session st sid) st))

      :read-layer
      (let [[_ s-idx l-idx] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid) (m/op-read-layer st sid lid) st))

      :read-trail
      (let [[_ r-idx t-idx] step
            r-sid (nth-mod sids r-idx)
            t-sid (nth-mod sids t-idx)]
        (if (and r-sid t-sid) (m/op-read-trail st r-sid t-sid) st))

      :move-chunk
      (let [[_ s-idx l-idx] step
            sid (nth-mod sids s-idx)
            lid (nth-mod lids l-idx)]
        (if (and sid lid) (m/op-record-movement-chunk st sid lid [[0 0] [10 12] [20 25]]) st))

      :promote
      (let [[_ copy s-idx b-idx crash?] step
            sid (nth-mod sids s-idx)
            blid (nth-mod branch-lids b-idx)]
        (if (and sid blid) (m/op-promote-branch st copy sid blid crash?) st))

      :crash-copy
      (let [[_ copy] step]
        (m/op-crash-copy st copy)))))

(defn run-history [config steps]
  (reduce apply-step (m/initial-state config) steps))

(def configurations
  [[:naive-all-underspecified      m/naive-config]
   [:baseline                      m/baseline-config]
   [:R1-back-content-tuple-perm-id (assoc m/baseline-config :perm-id :content-tuple)]
   [:R1b-back-any-actor-settings   (assoc m/baseline-config :settings-guard :any-actor)]
   [:R2-back-uncoordinated-signin  (assoc m/baseline-config :two-module-bootstrap :uncoordinated)]
   [:R3a-back-runtime-fills-depot  (assoc m/baseline-config :act-wire-id :client-uuid-runtime-depot)]
   [:R3b-back-runtime-random-uuid  (assoc m/baseline-config :act-wire-id :runtime-mints-random)]
   [:R4-back-watch-checked-at-open (assoc m/baseline-config :watch-standing :check-at-open-only)]
   [:R5-back-naive-promote         (assoc m/baseline-config :promote-form :naive-overwrite)]])

(def gen-perm-step
  "Focused generator mix around permission delegation, ending, and re-giving."
  (gen/frequency
   [[4 (gen/tuple (gen/return :sign-in) gen-copy (gen/return "sub-alice") (gen/return false))]
    [6 (gen/tuple (gen/return :attach-agent) gen-copy (gen/return 0) (gen/return 0) (gen/return true))]
    [6 (gen/tuple (gen/return :subgrant) gen-copy gen/nat (gen/return 0))]
    [6 (gen/tuple (gen/return :end-perm) gen-copy (gen/return 0) (gen/return 0) (gen/return 0))]
    [6 (gen/tuple (gen/return :regive-perm) gen-copy (gen/return 0) (gen/return 0) (gen/return 0))]
    [6 (gen/tuple (gen/return :send-note) gen-copy gen/nat (gen/return 0) gen-entity gen-val (gen/return false))]]))

(def gen-history
  (gen/frequency
   [[3 (gen/vector gen-step 1 35)]
    [1 (gen/vector gen-perm-step 1 20)]]))

(defn check-one [config check-fn num-tests seed]
  (let [p (prop/for-all [steps gen-history]
            (nil? (check-fn (run-history config steps))))
        res (tc/quick-check num-tests p :seed seed)]
    (if (:pass? res)
      {:status :holds :num-tests num-tests}
      (let [shrunk (first (get-in res [:shrunk :smallest]))
            violation (check-fn (run-history config shrunk))]
        {:status :fails
         :at-test (inc (:num-tests res))
         :shrunk-len (count shrunk)
         :shrunk-trace shrunk
         :violation violation}))))

(defn -main [& args]
  (let [num-tests (if (seq args) (parse-long (first args)) 1000)
        seed      (if (> (count args) 1) (parse-long (second args)) 20261121)]
    (println (format "Running runtime-model: %d histories/cell, seed %d" num-tests seed))
    (println)
    ;; Header
    (println (format "%-32s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s"
                     "configuration"
                     "P1 1-RT" "P2 sign-in" "P3 perms" "P4 settings" "P5 watches" "P6 promote" "P7 trails"))
    (println (apply str (repeat 135 "-")))
    (let [failures (atom [])]
      (doseq [[cfg-name cfg] configurations]
        (let [cells (mapv (fn [[pname pfn]]
                            (let [r (check-one cfg pfn num-tests seed)]
                              (when (= :fails (:status r))
                                (swap! failures conj {:config cfg-name :prop pname :result r}))
                              (if (= :holds (:status r))
                                "holds"
                                (format "FAIL (%d)" (:at-test r)))))
                          props/all-properties)]
          (println (apply format "%-32s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s"
                          (name cfg-name) cells))))
      (println)
      (println "=== Shrunk Minimal Counterexamples (Single-Flip Falsifications) ===")
      (doseq [{:keys [config prop result]} @failures
              :when (not= :naive-all-underspecified config)]
        (println (format "\n[%s × %s] failed at history #%d, shrunk to %d steps:"
                         (name config) (name prop) (:at-test result) (:shrunk-len result)))
        (println "  trace:    " (pr-str (:shrunk-trace result)))
        (println "  violation:" (pr-str (:violation result)))))))
