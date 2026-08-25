(ns app.client.substrate.frame-inputs-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [app.client.substrate.frame-inputs :as frame-inputs]))

(deftest s1-camera-only-has-no-semantic-family
  (let [device (Object.)
        stable (Object.)
        inputs {:device device :text-sys stable :text-sys-token [stable 3]
                :path-zoom-regime :path/normal
                :connector-zoom-regime :path/normal
                :region-lease-size [512 512] :region-encode-scale 1.12}]
    (is (= #{} (frame-inputs/changed-families inputs inputs)))
    (is (empty? (set/intersection
                 frame-inputs/forbidden-declared-inputs
                 (reduce set/union #{}
                         (vals frame-inputs/family-input-declarations)))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"Raw camera input"
         (frame-inputs/validate-declarations!
          (update frame-inputs/family-input-declarations
                  :render.family/path conj :zoom)
          frame-inputs/quantization-doors)))))

(deftest retired-overlay-plumbing-is-not-a-frame-input
  (let [retired-inputs #{:sidebar-pool-info
                         :cmd-panel-visible
                         :settings-line-count
                         :settings-visible
                         :agent-visible}
        declared-inputs (reduce set/union #{}
                                (vals frame-inputs/family-input-declarations))]
    (is (empty? (set/intersection retired-inputs declared-inputs)))))

(deftest s2-change-is-family-scoped-and-same-frame
  (let [device (Object.)
        prior {:device device :paths [:old] :text-sys-token [:text 2]}
        current (assoc prior :text-sys-token [:text 3])]
    (is (= #{:render.family/msdf :render.family/slug}
           (frame-inputs/changed-families prior current)))))

(deftest family-scoped-maintenance-does-not-visit-unchanged-families
  (let [path-old {:family/id :render.family/path :entry/id :path-old}
        text-old {:family/id :render.family/msdf :entry/id :text-old}
        text-new {:family/id :render.family/msdf :entry/id :text-new}
        key-fn (juxt :family/id :entry/id)
        delta (frame-inputs/family-entry-delta
               {:render.family/path #{(key-fn path-old)}
                :render.family/msdf #{(key-fn text-old)}}
               [text-new] #{:render.family/msdf} key-fn)]
    (is (= #{(key-fn text-old)} (:remove delta)))
    (is (= [text-new] (:insert delta)))
    (is (= #{(key-fn path-old)}
           (get-in delta [:next-keys :render.family/path])))))

(deftest cross-family-entries-live-and-die-with-their-producer
  ;; Twin receipt 2026-08-09: connector-minted label entries carry the text
  ;; family's :family/id (the label paint door). Bucketing by :family/id
  ;; removed them on every text produce and dropped them on every connector
  ;; produce. The delta buckets by :frame/producer, falling back to :family/id.
  (let [label {:family/id :render.family/msdf :entry/id :conn-label
               :frame/producer :render.family/connector}
        text-old {:family/id :render.family/msdf :entry/id :text-old}
        text-new {:family/id :render.family/msdf :entry/id :text-new}
        mesh {:family/id :render.family/connector :entry/id :conn-mesh
              :frame/producer :render.family/connector}
        key-fn (juxt :family/id :entry/id)
        index {:render.family/msdf #{(key-fn text-old)}
               :render.family/connector #{(key-fn label) (key-fn mesh)}}]
    (testing "a text-only produce never removes the connector-produced label"
      (let [delta (frame-inputs/family-entry-delta
                   index [text-new] #{:render.family/msdf} key-fn)]
        (is (= #{(key-fn text-old)} (:remove delta)))
        (is (= #{(key-fn label) (key-fn mesh)}
               (get-in delta [:next-keys :render.family/connector])))))
    (testing "a connector-only produce carries its cross-family label"
      (let [delta (frame-inputs/family-entry-delta
                   index [label mesh] #{:render.family/connector} key-fn)]
        (is (= #{} (:remove delta)))
        (is (= [label mesh] (:insert delta)))))))

(deftest s3-camera-doors-are-versioned-and-quantized
  (testing "within one geometric step retains the same encode rung"
    (is (= (frame-inputs/region-encode-rung 1.0)
           (frame-inputs/region-encode-rung 1.119))))
  (testing "the next step crosses exactly at the registered ratio"
    (is (< (frame-inputs/region-encode-rung 1.0)
           (frame-inputs/region-encode-rung frame-inputs/region-encode-step))))
  (is (= 256 frame-inputs/region-lease-quant))
  (is (= 4096 frame-inputs/region-lease-max)))

(deftest s4-first-frame-and-device-loss-produce-all-families
  (let [old-device (Object.)
        new-device (Object.)
        prior {:device old-device}
        current {:device new-device}]
    (is (= frame-inputs/family-ids
           (frame-inputs/changed-families nil prior)))
    (is (= frame-inputs/family-ids
           (frame-inputs/changed-families prior current)))))

(deftest s4-lying-prepare-is-caught-by-compare-only-twin
  (let [retained [{:entry/id :same :paint {:instance-count 1}}]
        batch [{:entry/id :same :paint {:instance-count 2}}]]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"diverged from batch oracle"
         (frame-inputs/assert-twin-equal! retained batch 1)))
    (is (true? (frame-inputs/assert-twin-equal! retained retained 1)))))

(deftest s5-shape-revision-is-paired-with-system-identity
  (let [identity (Object.)
        revision (atom 7)
        system {:frame-input/identity identity :!shape-rev revision}
        before (frame-inputs/system-token system)]
    (frame-inputs/bump-shape-rev! system)
    (is (identical? identity (first before)))
    (is (= [identity 8] (frame-inputs/system-token system)))))

(deftest s5-payload-carrier-does-not-open-the-shape-gate
  (let [identity (Object.)
        prior {:frame-input/identity identity
               :frame-input/shape-rev 4
               :buffer :old :draw-count 3}
        payload-change (assoc prior :buffer :new :draw-count 9)
        shape-change (assoc payload-change :frame-input/shape-rev 5)]
    (is (frame-inputs/input-value-same? prior payload-change))
    (is (not (frame-inputs/input-value-same? prior shape-change)))))
