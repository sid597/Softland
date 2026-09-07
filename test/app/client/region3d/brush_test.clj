(ns app.client.region3d.brush-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.data.json :as json]
            [app.client.region3d.records :as records]
            [app.client.region3d.capabilities :as capabilities]
            [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]
            [app.client.engine.surface :as surface])
  (:import [java.security MessageDigest]
           [java.io ByteArrayInputStream]
           [javax.imageio ImageIO]))

(defn sha [painting]
  (apply str (map #(format "%02x" (bit-and 255 %)) (.digest (MessageDigest/getInstance "SHA-256") (vb/float-bytes (:data painting))))))
(defn run
  ([record opts] (run record {:host records/host} opts))
  ([record scope opts] (executor/run (records/program-record record) scope capabilities/table opts)))
(defn retained [run output] (assoc (get-in run [:results output]) :subject (get-in run [:subjects output])))
(defn painting-hash [r] (sha (get-in r [:results :painting])))
(def expected-hash "30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579")
(def expected-colors [[0.25 0 0.5 0.75] [0.5390625 0 0.328125 0.8671875] [0.5 0 0 0.5] [0.6631851196289062 0 0.2650909423828125 0.9282760620117188]])
(def expected-carries [[0.8125 0 0.125 0.9375] [0.744140625 0 0.17578125 0.919921875] [0.68310546875 0 0.1318359375 0.81494140625] [0.6781253814697266 0 0.16514968872070312 0.8432750701904297]])
(defn near-colors [a b] (and (= (count a) (count b)) (every? #(< (Math/abs (double %)) 1.0e-9) (map - a b))))

(deftest cold-authors-fixtures-keep-their-authored-inputs
  (let [reach (json/read-str (slurp "test/app/fixtures/region3d/cold-reach.json") :key-fn keyword)
        dab (json/read-str (slurp "test/app/fixtures/region3d/cold-reach-dab.json") :key-fn keyword)
        painting (:painting records/cold-dab)]
    (is (= (:support reach) (:support records/cold-reach)))
    (is (= (:tool reach) (select-keys (:tool records/cold-reach) [:seed :radius])))
    (is (= (:support dab) (:support records/cold-dab)))
    (is (= (:tool dab) (:tool records/cold-dab)))
    (is (= (get-in dab [:painting :grid]) [(:width painting) (:height painting)]))
    (is (= (get-in dab [:painting :domain]) (get-in painting [:domain :rect])))
    (is (= (get-in dab [:painting :initial]) (:initial painting)))
    (is (= (mapv (comp keyword :op) (get-in dab [:program :steps]))
           (mapv :op (rest (get-in records/cold-dab [:program :steps])))))))

(deftest the-definers-four-dabs-through-the-shared-executor-and-compositor
  (let [r (run records/pickup {:budget 1})]
    (is (= :complete (:status r)) (pr-str (dissoc r :request :continuation)))
    (when (= :complete (:status r))
      (let [history (:history r) painting (get-in r [:results :painting])]
        (is (every? true? (map near-colors expected-colors (map #(get-in % [:steps 1 :color]) history))))
        (is (every? true? (map near-colors expected-carries (map #(get-in % [:state-after :carry]) history))))
        (is (= [["kA" "kB" "pickup-G@0"] ["kA" "kB" "pickup-G@1"] ["kA" "pickup-G@2"] ["kA" "kB" "pickup-G@3"]]
               (mapv #(get-in % [:steps 1 :contributors]) history)))
        (is (= [401 321 405 401] (mapv #(get-in % [:steps 4 :changed]) history)))
        (is (= expected-hash (sha painting)))
        (is (= [4 "pickup-G:3/painted"] ((juxt :revision :key) painting)))
        (let [png (ImageIO/read (ByteArrayInputStream. (surface/png-bytes painting)))]
          (is (= [64 32] [(.getWidth png) (.getHeight png)]))
          (is (pos? (bit-and 0xff000000 (.getRGB png 51 15)))))))))

(deftest eager-delayed-and-encoded-continuations-keep-the-same-history
  (let [eager (run records/pickup {:budget 1}) held (run records/pickup {:budget 0})
        c (:continuation held) encoded (executor/encode c)
        resumed (executor/resume c capabilities/table {:budget 1})
        restored (executor/resume (executor/decode encoded capabilities/table) capabilities/table {:budget 1})
        checkpoint (:continuation (run records/pickup {:budget 1 :until 2}))]
    (is (= [:suspended :pending 0] ((juxt :status :reason :at) held)))
    (is (= [] (:history c)))
    (is (= [1 0 0 1] (get-in c [:state :carry])))
    (is (= 0 (get-in c [:state :painting :revision])))
    (is (= ["kA" "pickup-G@0"] (get-in held [:read :known])) "the transparent painting was consulted too")
    (is (re-find #"B@0 → M@2" (get-in held [:missing 0 :missing])))
    (is (vb/equal? c (:continuation (run records/pickup {:budget 0}))))
    (doseq [r [resumed restored (executor/resume checkpoint capabilities/table {:budget 1})]]
      (is (= :complete (:status r)))
      (doseq [k [:state :history :results :subjects]] (is (vb/equal? (k eager) (k r)) (name k))))
    (is (= :recipe-differs (:reason (executor/resume c capabilities/table {:scope {:host (assoc records/host :R 201)} :budget 1}))))
    (let [bad (assoc-in c [:state :painting :width] 5)]
      (is (= :load (:reason (executor/decode (vb/encode bad) capabilities/table)))))
    (println "COATING" {:sha256 (painting-hash eager) :checkpoint-bytes (alength encoded) :carry (get-in eager [:state :carry])})))

(deftest provisional-schedules-are-declared-and-distinct
  (let [record (assoc-in records/pickup [:program :each :steps 1 :pending] :provisional)
        first-only (:continuation (run record {:budget 0 :until 1}))
        mixed (executor/resume first-only capabilities/table {:budget 1})
        all (run record {:budget 0})]
    (is (= "c1cf81702b98fb287cbbb1432166d81e7a55750d3306c3934cd09528960d2c3d" (painting-hash mixed)))
    (is (near-colors [0.875 0 0 0.875] (get-in mixed [:history 0 :state-after :carry])))
    (is (near-colors [0.7258682250976562 0 0.0880279541015625 0.8138961791992188] (get-in mixed [:state :carry])))
    (is (= [true false false false] (mapv #(boolean (get-in % [:steps 1 :provisional])) (:history mixed))))
    (is (near-colors [0.773040771484375 0 0 0.773040771484375] (get-in all [:state :carry])))
    (is (.startsWith (painting-hash all) "dd7d3355"))
    (is (every? #(get-in % [:steps 1 :provisional]) (:history all)))))

(deftest answers-refuse-edited-inputs-and-are-consumed-once
  (let [demand (run records/pickup {:stop-at :picked :budget 1})
        answer (select-keys demand [:request :value])
        reverse-order (assoc-in records/pickup [:coating :order] ["kB" "kA"])
        stronger (assoc-in records/pickup [:tool :pickup] 0.5)]
    (doseq [r [reverse-order stronger]]
      (let [stale (run r {:budget 1 :answer answer})]
        (is (= [:stale :recipe] ((juxt :status :reason) stale)))
        (is (= [] (:history stale)))))
    (is (= "ae8c231cbfe1ddb4c568913cced70bd8ce8988a25af93d66958c6c14bbb6963a" (painting-hash (run reverse-order {:budget 1}))))
    (is (= "4df8a532ec4048f5489927daff4e7dd3635853c66c8d6bb09ebe04a18fc5f0ce" (painting-hash (run stronger {:budget 1}))))
    (let [none (run (assoc-in records/pickup [:coating :order] nil) {:budget 1})]
      (is (= [:suspended :needs-policy] ((juxt :status :reason) none)))
      (is (= ["kA" "kB"] (:missing none))))
    (let [longer (update records/pickup :events conj {:id "later" :curve "kA/arc-AB" :t 0.8})
          c (:continuation (run longer {:budget 1 :until 4}))
          r (executor/resume c capabilities/table {:record (records/program-record records/pickup) :budget 1 :answer answer})]
      (is (= :answer-not-consumed (:reason r)))
      (is (= 4 (count (:history r)))))))

(deftest returned-regions-and-paintings-are-reusable-under-their-subjects
  (let [reach (run records/reach {}) clip (retained reach :region)
        opts {:records {"reach@0" (records/program-record records/reach)}}
        scope {:host records/host :inputs {:clip clip}}
        a (run records/clip-record scope opts) b (run records/cold-dab scope opts)]
    (is (= :complete (:status a)))
    (is (= 34 (get-in a [:results :painting :changed])))
    (is (= :complete (:status b)))
    (is (= 6096 (get-in b [:results :painting :changed])))
    (is (.startsWith (painting-hash b) "26da35e3"))
    (is (= :subject (:reason (run records/cold-dab (assoc-in scope [:inputs :clip] (retained (run (assoc-in records/reach [:tool :radius] 140) {}) :region)) opts)))))
  (let [first-run (run records/pickup {:budget 1}) painting (retained first-run :painting)
        second-record (-> records/pickup
                          (assoc :id "again" :inputs {:previous {:kind :painting :from {:record "pickup" :output :painting}}})
                          ;; probe-4 builds reuse from its edited pickup=.5 variant.
                          (assoc-in [:tool :pickup] 0.5)
                          (assoc-in [:program :each :state :painting] [:get :inputs :previous]))
        scope {:host records/host :inputs {:previous painting}}
        opts {:budget 1 :records {"pickup" (records/program-record records/pickup)}}
        r (run second-record scope opts)
        held (run second-record scope (assoc opts :budget 0))
        restored (executor/resume (executor/decode (executor/encode (:continuation held)) capabilities/table) capabilities/table {:budget 1})]
    (is (= :complete (:status r)) (pr-str (select-keys r [:status :reason :error :data])))
    (is (= "a5aba6ed27c234df463b322995e052bba3e3c4fa7871e453d4a0400270cd887a" (painting-hash r)))
    (is (= "b39a8b14209c6af505cfaa318d2a971cad1ae28f68089b32146b75a9af3e3dad"
           (painting-hash (run (assoc-in second-record [:tool :pickup] 0.25) scope opts))))
    (println "REUSE" {:pickup 0.5 :sha256 (painting-hash r)})
    (is (= (painting-hash r) (painting-hash restored)))
    (let [other (retained (run (assoc-in records/pickup [:tool :pickup] 0.5) {:budget 1}) :painting)]
      (is (= :subject (:reason (run second-record (assoc-in scope [:inputs :previous] other) opts)))))
    (is (= :subject (:reason (run second-record (update-in scope [:inputs :previous] dissoc :subject) opts))))
    (is (= :subject (:reason (run second-record (assoc-in scope [:inputs :previous :subject :out] :wrong) opts))))))
