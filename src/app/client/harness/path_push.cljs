(ns app.client.harness.path-push
  "Push-edge receipts through the real renderer, atlas and queue.
   Takes a device and shared buffers; gives measured row-write, population
   isolation and compaction results. Owns temporary test systems only."
  (:require [app.client.engine.buffer-pool :as pool]
            [clojure.set :as set]
            [app.client.engine.coverage :as coverage]
            [app.client.path.construction :as construction]
            [app.client.path.records :as records]
            [app.client.path.frame :as frame]
            [app.client.path.renderer :as renderer]))

(def view {:zoom 1.0 :pan [0.0 0.0]})

(defn init-fixtures!
  "Harness system with caller-owned fixture identities. Replacing a whole
   test picture is explicit; scale-trace edits use push! directly."
  [device format camera groups-buffer initial-view & options]
  (assoc (apply renderer/init-path-system device format camera groups-buffer initial-view options)
         :!fixture-ids (atom #{})))

(defn combine [receipts]
  (let [last (last receipts)]
    (merge last
           {:changed? (boolean (some :changed? receipts))
            :reran (apply merge-with set/union (map :reran receipts))}
           (into {} (for [k [:derivations :packs :instance-writes :row-ranges-written :row-comparisons]]
                      [k (reduce + 0 (keep k receipts))])))))

(defn fixture!
  "Explicitly replace a harness picture, then drive its view. The caller
   knows all pictured ids; this is setup, never the population edit route."
  [system items view groups]
  (let [ids (set (map :id items)) gone (set/difference @(:!fixture-ids system) ids)
        removed (renderer/push! system {:remove gone})
        framed (renderer/frame! system view)
        pushed (renderer/push! system {:upsert (into {} (map (juxt :id #(dissoc % :id))) items)
                                       :order (mapv :id items) :groups groups})]
    (reset! (:!fixture-ids system) ids)
    (combine [removed framed pushed])))

(deftype CountedRow [value comparisons]
  IEquiv
  (-equiv [_ other] (swap! comparisons inc) (= value other)))

(defn- counted [system f]
  (let [^js device (:device system) ^js queue (.-queue device) ^js original (.-writeBuffer queue)
        pack-fn (:pack-fn @(:pool system)) writes (atom []) packed (atom 0)
        comparisons (atom 0) input-checks (atom 0) item-key frame/item-key]
    ;; Install comparison probes outside the measured call. The former pool
    ;; walk would compare these 1600 prior rows; the direct writer reads none.
    (swap! (:pool system) assoc :prev-items (mapv #(CountedRow. % comparisons) (renderer/prepared-rows system)))
    (set! (.-writeBuffer queue)
          (fn [buffer offset ^js data & more]
            (when (identical? buffer (:buffer @(:pool system)))
              (swap! writes conj {:offset offset :bytes (.-byteLength data)}))
            (.apply original queue (to-array (concat [buffer offset data] more)))))
    (swap! (:pool system) assoc :pack-fn (fn [row] (swap! packed inc) (pack-fn row)))
    (try
      (let [receipt (with-redefs [pool/batch-update-pool! (fn [& _] (throw (js/Error. "population row comparison invoked")))
                                 coverage/retain! (fn [& _] (throw (js/Error. "population atlas scan invoked")))
                                 frame/item-key (fn [& args] (swap! input-checks inc) (apply item-key args))] (f))]
        {:receipt receipt :queue-writes @writes :packed-rows @packed
         :row-comparisons @comparisons :item-input-checks @input-checks})
      (finally (set! (.-writeBuffer queue) original) (swap! (:pool system) assoc :pack-fn pack-fn)))))

(defn run-checks!
  "Real 1600-placement edit and atlas compaction → receipt. Range writes
   are observed on GPUQueue, with both population-scanning APIs forbidden."
  [device camera groups-buffer groups]
  (let [system (renderer/init-path-system device "rgba8unorm" camera groups-buffer view :initial-capacity 1600)
        rect (construction/construct {:path/material-id :shared :path/revision 1
                                       :path/source {:kind :rect :x 0 :y 0 :w 16 :h 16}
                                       :path/paint {:fill {:rule :nonzero :color [1 0 0 1]}}})
        item {:path/material rect :container 0}
        first-push (renderer/push! system {:upsert (zipmap (range 1600) (repeat item)) :order (vec (range 1600)) :groups groups})
        edit (counted system #(renderer/push! system {:upsert {20 (assoc-in item [:path/material :path/paint :fill :color] [0 1 0 1])}}))
        pan (counted system #(renderer/frame! system {:zoom 1.0 :pan [0.5 0.25]}))
        removed (renderer/push! system {:remove #{20}})
        survivor-range (renderer/item-range system 21)
        _ (renderer/destroy-path-system! system)
        compact (renderer/init-path-system device "rgba8unorm" camera groups-buffer view :initial-capacity 4)
        big {:path/material (construction/construct records/harness-z) :container 0}
        _ (renderer/push! compact {:upsert {:big big} :order [:big] :groups groups})
        _ (renderer/push! compact {:upsert {:small item} :order [:small :big]})
        before (first (renderer/prepared-rows compact))
        drop (counted compact #(renderer/push! compact {:remove #{:big}}))
        after (first (renderer/prepared-rows compact))
        _ (renderer/destroy-path-system! compact)
        fresh (renderer/init-path-system device "rgba8unorm" camera groups-buffer view)
        _ (renderer/push! fresh {:upsert {:small item} :order [:small] :groups groups})
        fresh-row (first (renderer/prepared-rows fresh))
        matches-fresh? (= (vec (coverage/pack-instance after)) (vec (coverage/pack-instance fresh-row)))
        _ (renderer/destroy-path-system! fresh)
        exact? (and (= 1600 (:instances first-push))
                    (= #{20} (get-in edit [:receipt :reran :rows]))
                    (empty? (get-in edit [:receipt :reran :geometry]))
                    (empty? (get-in edit [:receipt :reran :pack]))
                    (= #{20} (get-in edit [:receipt :visited]))
                    (= [{:offset (* 20 coverage/instance-stride) :bytes coverage/instance-stride}] (:queue-writes edit))
                    (= 1 (:packed-rows edit))
                    (= 1 (:item-input-checks edit)) (zero? (:row-comparisons edit))
                    (empty? (:queue-writes pan)) (empty? (get-in pan [:receipt :visited]))
                    (= [20 1] survivor-range) (= 1599 (:instances removed))
                    (not= (:slot before) (:slot after))
                    (= #{:small} (get-in drop [:receipt :reran :rows]))
                    (= 1 (:packed-rows drop)) matches-fresh?)]
    {:population 1600 :edit edit :unsnapped-pan pan
     :removal {:instances (:instances removed) :next-range survivor-range}
     :compaction {:before-slot (:slot before) :after-slot (:slot after) :counted drop :matches-fresh? matches-fresh?}
     :pass? exact?}))
