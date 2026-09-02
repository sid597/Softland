(ns app.client.image.component-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [app.client.engine.limits :as limits]
            [app.client.image.component :as image])
  (:import (java.nio.file Files Path)
           (java.security MessageDigest)))

(def digest-a (apply str (repeat 64 "a")))
(def digest-b (apply str (repeat 64 "b")))

(def source-a
  {:image/digest digest-a
   :image/color-tag :srgb
   :image/width 32
   :image/height 16
   :image/bytes-route {:kind :fixture :path "images/a.png"}
   :image/alpha-association :straight})

(def component-a
  {:image/component-id :component/a
   :image/revision 1
   :image/source-digest digest-a
   :image/color-tag :srgb
   :image/intrinsic-size [32 16]
   :image/provenance {:actor :fixture :act :generate}
   :image/rect {:x 10 :y 20 :w 32 :h 16}
   :image/crop {:x 0 :y 0 :w 32 :h 16}
   :image/paint {:tint {:rgba [0.25 0.5 0.75 0.8]
                        :color-space :srgb
                        :alpha-association :straight}
                 :opacity 0.5}})

(def claimed-corpus-pressures
  #{:two-extents :atlas-overflow :alpha-association-pair
    :embedded-icc :untagged-rejection :digest-mismatch :unresolvable-digest
    :partially-clipped})

(defn assert-corpus-coverage!
  [fixture-pressure->gates]
  (let [actual (set (keys fixture-pressure->gates))
        missing (seq (sort (remove actual claimed-corpus-pressures)))
        unconsumed (seq (sort (for [[pressure gates] fixture-pressure->gates
                                   :when (empty? gates)] pressure)))]
    (when (or missing unconsumed)
      (throw (ex-info "Image corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))

(defn- rejection-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(defn- instance-input [component]
  {:rect (:image/rect component)
   :uv [0.0 0.0 1.0 1.0]
   :tint (get-in component [:image/paint :tint])
   :opacity (get-in component [:image/paint :opacity])
   :buffer-index 7})

(deftest i2-rows-travel-and-schema-rejects-by-name
  (testing "verified bytes, not a caller assertion, establish source identity"
    (let [registry (image/register-verified-source
                    (image/empty-source-registry) source-a digest-a)]
      (is (= source-a (image/resolve-source registry digest-a)))
      (is (= registry (image/register-verified-source registry source-a digest-a))
          "identical re-registration is idempotent")
      (is (thrown? clojure.lang.ExceptionInfo
                   (image/register-verified-source registry source-a digest-b)))))
  (testing "source and component rows survive EDN without admission metadata"
    (let [source-row (edn/read-string (pr-str source-a))
          component-row (edn/read-string (pr-str component-a))]
      (is (nil? (meta source-row)))
      (is (nil? (meta component-row)))
      (is (= source-row (image/validate-source! source-row)))
      (is (= component-row (image/validate-component! component-row)))
      (is (= (image/canonical-component component-a)
             (image/canonical-component component-row)))
      (is (= (image/component-cache-key component-a :mips-v1 :default)
             (image/component-cache-key component-row :mips-v1 :default)
             [digest-a 1 :mips-v1 :default]))
      (is (= (image/instance-words (instance-input component-a))
             (image/instance-words (instance-input component-row))
             [10 20 32 16 0.0 0.0 1.0 1.0 0.25 0.5 0.75 0.4 7]))))
  (testing "closed source and component schemas name each rejection"
    (doseq [[row error-type path]
            [[(assoc component-a :future/key true)
              :schema/unknown-key [:future/key]]
             [(assoc component-a :image/extensions {})
              :schema/unknown-key [:image/extensions]]
             [(assoc source-a :image/ingress-evidence {:reader :asserted})
              :schema/unknown-key [:image/ingress-evidence]]
             [(dissoc source-a :image/bytes-route)
              :schema/invalid-value [:image/bytes-route]]
             [(assoc-in component-a [:image/paint :tint :rgba]
                        [0.0 0.25 0.5 0.75 1.0])
              :schema/invalid-value [:image/paint :tint :rgba]]
             [(assoc-in component-a [:image/rect :h] 0)
              :schema/invalid-value [:image/rect :h]]
             [(assoc-in component-a [:image/paint :opacity] 1.5)
              :schema/invalid-value [:image/paint :opacity]]
             [(assoc component-a :image/crop [0 0 32 16])
              :schema/map-required [:image/crop]]]]
      (is (= {:error-type error-type :path path}
             (select-keys (rejection-data #(if (contains? row :image/digest)
                                           (image/validate-source! row)
                                           (image/validate-component! row)))
                          [:error-type :path]))))))

(deftest g7-mip-chain-and-placement-laws
  (testing "full mip chain counts, sizes, and bytes are exact"
    (is (= 4 (image/mip-level-count 8 4)))
    (is (= [[8 4] [4 2] [2 1] [1 1]] (image/mip-sizes 8 4)))
    (is (= 172 (limits/texture-bytes "rgba8unorm" 8 4 4 1))))
  (testing "small sources shelf-pack deterministically without padded overlap"
    (let [a0 (image/empty-atlas)
          p1 (image/placement-plan a0 digest-a {:width 32 :height 16})
          p2 (image/placement-plan (:atlas p1) digest-b
                                   {:width 16 :height 16})
          q1 (image/placement-plan (image/empty-atlas) digest-a
                                   {:width 32 :height 16})
          q2 (image/placement-plan (:atlas q1) digest-b
                                   {:width 16 :height 16})]
      (is (= :atlas (:tier p1)))
      (is (= :atlas (:tier p2)))
      (is (= [(:placement p1) (:placement p2)]
             [(:placement q1) (:placement q2)]))
      (is (<= (+ (:x (:placement p1)) (:width (:placement p1))
                   (:padding (:placement p1)))
              (- (:x (:placement p2)) (:padding (:placement p2)))))))
  (testing "large and overflow sources fall back to dedicated"
    (is (= :dedicated
           (:tier (image/placement-plan (image/empty-atlas) digest-a
                                        {:width 256 :height 256}))))
    (let [tiny-atlas (assoc-in (image/empty-atlas) [:config :height] 8)]
      (is (= :dedicated
             (:tier (image/placement-plan tiny-atlas digest-a
                                          {:width 8 :height 8})))))))

(deftest g4-g8-geometry-and-clip-laws
  (let [quad {:x 10 :y 20 :w 100 :h 50}]
    (is (= :inside (image/classify-quad quad [11 21])))
    (is (= :boundary (image/classify-quad quad [10 20])))
    (is (= :outside (image/classify-quad quad [111 21])))
    (is (image/half-open-hit? quad [10 20]))
    (is (not (image/half-open-hit? quad [110 70])))
    (let [clipped (image/clip-placement quad
                                        {:x 0 :y 0 :w 200 :h 100}
                                        {:x 35 :y 30 :w 50 :h 30})]
      (is (= {:x 35 :y 30 :w 50 :h 30} (:placement clipped)))
      (is (< (Math/abs (- 60.0 (get-in clipped [:crop :h]))) 1.0e-12))
      (is (= {:x 50.0 :y 20.0 :w 100.0}
             (dissoc (:crop clipped) :h)))
      (is (= [0.25 0.2 0.75 0.8]
             (image/crop->uv [200 100] (:crop clipped)))))))

(deftest g5-sub-draw-runs-preserve-draw-item-order
  (let [draw-items [{:id :a :image/binding-key :atlas}
             {:id :b :image/binding-key :atlas}
             {:id :c :image/binding-key :dedicated-c}
             {:id :d :image/binding-key :atlas}]
        runs (image/contiguous-binding-runs draw-items)]
    (is (= [:atlas :dedicated-c :atlas] (mapv :binding-key runs)))
    (is (= [0 2 3] (mapv :first-instance runs)))
    (is (= [2 1 1] (mapv :instance-count runs)))
    (is (= [[:a :b] [:c] [:d]]
           (mapv #(mapv :id (:draw-items %)) runs)))))

(deftest g7-corpus-pressure-coverage-check-is-executable
  (let [coverage {:two-extents #{:g4}
                  :atlas-overflow #{:g7}
                  :alpha-association-pair #{:g6}
                  :embedded-icc #{:g6}
                  :untagged-rejection #{:g7}
                  :digest-mismatch #{:g7}
                  :unresolvable-digest #{:g9}
                  :partially-clipped #{:g3 :g8}}]
    (is (true? (assert-corpus-coverage! coverage)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (assert-corpus-coverage! (dissoc coverage :embedded-icc))))))

(def ^:private fixture-digests
  {"alpha-reference-straight.png"
   "b38bcacf6298ec057c4e1b03fefa2116440d8bdd2ea31bb79dcd94396856d608"
   "atlas-opaque-srgb.png"
   "6e744e448e1480b510b3cd8aef86b5e2c9b8367bbafdb3e1586946c779b265eb"
   "clip-stripes-srgb.png"
   "8000e248f9550d364286a5b873a77813040762998f087f7521f0900532730c15"
   "coverage-white-srgb.png"
   "71bafd65a2358f69ab1e3086058fa4180760a54f87b0b58cd75579b3ed74d81e"
   "dedicated-alpha-premultiplied.png"
   "28840b6c70cb6dcd01f2f4ae0304e9c38070fcb2c1505f25d03111b158b6f30b"
   "dedicated-alpha-straight.png"
   "c859086c6a2cd8171f4cd429fd9d62529dd97c53e5ae9f131ce216b90af7e402"
   "profiled-linear-rgb.png"
   "8e0358b9e3830abfdbff58faf3a19907d3fd2c2fd4a79d4cc6156acbb09fabea"
   "seam-byte-srgb.png"
   "836e03f287a153693eecd3b75e4c8ffefbc38407f1e7784a4c2a8f4471f6650a"})

(defn- file-sha256 [path]
  (let [bytes (Files/readAllBytes (Path/of path (make-array String 0)))
        digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and (int %) 0xff)) digest))))

(deftest g7-committed-corpus-bytes-are-pinned
  (doseq [[filename expected] fixture-digests]
    (let [path (str "test/app/fixtures/render_engine/images/" filename)]
      (is (Files/isRegularFile (Path/of path (make-array String 0))
                               (make-array java.nio.file.LinkOption 0))
          filename)
      (is (= expected (file-sha256 path)) filename))))
