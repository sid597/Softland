(ns app.client.text.shaping-correction-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.client.text.layout :as tl]))

(defn- utf16-clusters
  "Synthetic deterministic clusters. `pair?` keeps the astral test character
   atomic, so the suite can prove cuts are provider-cluster cuts rather than
   code-unit cuts."
  [text]
  (loop [offset 0 out []]
    (if (>= offset (.length ^String text))
      out
      (let [high? (Character/isHighSurrogate (.charAt ^String text offset))
            pair? (and high? (< (inc offset) (.length ^String text))
                       (Character/isLowSurrogate
                        (.charAt ^String text (inc offset))))
            end (+ offset (if pair? 2 1))
            s (subs text offset end)
            width (cond
                    (= s " ") 100
                    (= s "\t") 150
                    (= s "\u00a0") 125
                    pair? 600
                    :else 100)]
        (recur end (conj out {:text s :start offset :end end :width width}))))))

(defn- shaped-line [text]
  (let [[glyphs clusters advance]
        (reduce
         (fn [[glyphs clusters x] {:keys [text start end width]}]
           (let [glyph {:glyph-id (hash text)
                        :glyph-id-kind :font-glyph-index
                        :font-id :mock/proportional
                        :font-revision "mock-variable-v1"
                        :cluster-start start :cluster-end end
                        :advance [width 0] :offset [0 0] :position [x 0]
                        :ink-bounds {:xBearing 0 :yBearing 80
                                     :width width :height 100}
                        :direction :ltr}
                 cluster {:source-start start :source-end end
                          :direction :ltr
                          :font-revision "mock-variable-v1"
                          :left x :right (+ x width)}]
             [(conj glyphs glyph) (conj clusters cluster) (+ x width)]))
         [[] [] 0]
         (utf16-clusters text))]
    {:runs (if (seq glyphs)
             [{:source-start 0 :source-end (.length ^String text)
               :direction :ltr :font-id :mock/proportional
               :font-revision "mock-variable-v1" :upem 1000
               :glyphs glyphs}]
             [])
     :glyphs glyphs :clusters clusters :advance advance
     :base-direction :ltr}))

(defn- synthetic-provider
  ([] (synthetic-provider nil))
  ([reference-fault]
   {:face-id :mock/proportional
    :face-revision "mock-variable-v1"
    :shaper-id :mock/harfbuzz
    :shaper-version "8.3"
    :upem 1000
    :features ["kern" "liga" "clig"]
    :variations {:wght 425 :wdth 92}
    :axes {:wght {:min 100 :default 400 :max 800}
           :wdth {:min 75 :default 100 :max 125}}
    :fallback-chain [{:id :mock/cjk :revision "cjk-v1"}]
    :metrics {:ascender 800 :descender -200 :lineGap 100}
    :shape-line
    (fn [text _opts]
      (if (and (= text " ") reference-fault)
        (case reference-fault
          :empty (assoc (shaped-line text) :clusters [])
          :missing (dissoc (shaped-line text) :advance)
          :non-numeric (assoc (shaped-line text) :advance "100")
          :zero (assoc (shaped-line text) :advance 0)
          :negative (assoc (shaped-line text) :advance -100))
        (shaped-line text)))}))

(defn- split-runs
  "Give the synthetic line at least `run-count` source-contiguous runs while
   retaining the same glyph/cluster geometry."
  [line run-count]
  (let [glyphs (:glyphs line)
        chunk-size (max 1 (long (Math/ceil (/ (count glyphs)
                                               (double run-count)))))
        chunks (partition-all chunk-size glyphs)]
    (assoc line :runs
           (mapv (fn [chunk]
                   {:source-start (reduce min (map :cluster-start chunk))
                    :source-end (reduce max (map :cluster-end chunk))
                    :direction :ltr :font-id :mock/proportional
                    :font-revision "mock-variable-v1" :upem 1000
                    :glyphs (vec chunk)})
                 chunks))))

(defn- pathological-provider
  [{:keys [shuffle?]}]
  (assoc (synthetic-provider)
         :shape-line
         (fn [text _opts]
           (let [line (split-runs (shaped-line text) 3)]
             (if (and shuffle? (> (count (:glyphs line)) 3))
               ;; Deterministic non-monotonic provider order. Geometry remains
               ;; attached to each glyph; the layout index must preserve this
               ;; visual order while indexing source ownership.
               (update line :glyphs #(vec (concat (take-nth 2 %)
                                                  (take-nth 2 (rest %)))))
               line)))))

(defn- layout-input
  ([text] (layout-input text {}))
  ([text overrides]
   (merge {:text text :provider (synthetic-provider)
           :font-size 10 :line-height 14 :baseline-offset 10
           :origin [0 0] :wrap-policy :block-greedy :wrap-col 5
           :source-id :g1 :source-revision "r1"}
          overrides)))

(defn- body-lines [layout]
  (vec (remove #(= :header (first (:source-range %))) (:lines layout))))

(defn- offsets [source-range]
  (mapv tl/source-index-offset source-range))

(deftest block-greedy-semantic-table
  (testing "the worked example consumes the entire internal whitespace run"
    (let [layout (tl/layout (layout-input "abc   def"))
          [a b] (body-lines layout)]
      (is (= ["abc" "def"] (mapv :text [a b])))
      (is (= [0 6] (offsets (:source-range a))))
      (is (= [3 6] (offsets (:consumed-range a))))
      (is (= [6 9] (offsets (:source-range b))))
      (is (= 1.0 (:reference-advance layout)))
      (is (= 5.0 (:inline-size layout)))))
  (testing "leading whitespace is paint, while internal SPACE and TAB break"
    (is (= ["  abc" "def"]
           (mapv :text (body-lines (tl/layout (layout-input "  abc def"))))))
    (is (= ["abc" "def"]
           (mapv :text (body-lines (tl/layout (layout-input "abc\tdef")))))))
  (testing "NBSP is never a candidate"
    (let [[line] (body-lines
                  (tl/layout (layout-input (str "a" \u00a0 " b")
                                           {:wrap-col 2.25})))]
      (is (= (str "a" \u00a0) (:text line)))
      (is (= [2 3] (offsets (:consumed-range line))))))
  (testing "hard cuts progress by one provider cluster and can leave whitespace"
    (is (= ["😀" "a"]
           (mapv :text (body-lines
                        (tl/layout (layout-input "😀a" {:wrap-col 1}))))))
    (let [lines (body-lines
                 (tl/layout (layout-input "a      z" {:wrap-col 2})))]
      (is (= "a" (:text (first lines))))
      (is (= [1 7] (offsets (:consumed-range (first lines)))))
      (is (= "z" (:text (second lines))))))
  (testing "mid-word, empty, hard-line empties, and trailing whitespace"
    (is (= ["abc" "def"]
           (mapv :text (body-lines
                        (tl/layout (layout-input "abcdef" {:wrap-col 3}))))))
    (is (= [""] (mapv :text (body-lines (tl/layout (layout-input ""))))))
    (is (= ["a" "" "b" ""]
           (mapv :text (body-lines (tl/layout (layout-input "a\n\nb\n"))))))
    (let [lines (body-lines (tl/layout (layout-input "abc   ")))]
      (is (= ["abc  " " "] (mapv :text lines)))
      (is (every? #(nil? (:consumed-range %)) lines))))
  (testing "headers are shaped once, never wrapped, and retain exact domains"
    (let [layout (tl/layout (layout-input "abc def"
                                          {:wrap-col 3
                                           :headers ["long header" "h2"]}))
          [h0 h1 & body] (:lines layout)]
      (is (= ["long header" "h2"] (mapv :text [h0 h1])))
      (is (= [:header 0 [0 11]] (:source-range h0)))
      (is (= [:header 1 [0 2]] (:source-range h1)))
      (is (nil? (:consumed-range h0)))
      (is (= [:header 0 0]
             (:index (tl/caret-result layout 0 0))))
      (is (= [:header 0 11]
             (:index (tl/caret-result layout 0 11))))
      (is (= "long" (:text (tl/copy-result layout [:header 0 [0 4]]))))
      (is (= ["abc" "def"] (mapv :text body)))
      ;; body initial + 2 final segments + 2 header shapes; reference excluded.
      (is (= 5 (get-in layout [:receipts :proportionality :shape-calls]))))))

(deftest provider-fault-totality
  (testing "a nonempty body with no provider clusters stays one total line"
    (let [provider (assoc (synthetic-provider)
                          :shape-line (fn [text opts]
                                        (if (= text "😀x")
                                          {:runs [] :glyphs [] :clusters []
                                           :advance 999}
                                          (shaped-line text))))
          layout (tl/layout (layout-input "😀x" {:provider provider
                                                  :wrap-col 1}))]
      (is (= ["😀x"] (mapv :text (body-lines layout))))
      (is (= 1 (get-in layout [:receipts :provider-fault])))))
  (testing "every invalid reference shape produces one fault and unbounded body"
    (doseq [fault [:empty :missing :non-numeric :zero :negative]]
      (let [layout (tl/layout (layout-input "abcdef"
                                            {:provider (synthetic-provider fault)
                                             :wrap-col 1}))]
        (is (= ["abcdef"] (mapv :text (body-lines layout))) (name fault))
        (is (= 1 (get-in layout [:receipts :provider-fault])) (name fault))
        (is (nil? (:reference-advance layout)) (name fault))
        (is (nil? (:inline-size layout)) (name fault))))))

(deftest consumed-range-reader-law
  (let [layout (tl/layout (layout-input "abc   def"))
        first-line (first (body-lines layout))
        painted-end (get-in first-line [:logical-bounds :w])]
    (testing "all consumed offsets stay at painted end; consumed-end is next start"
      (doseq [col [3 4 5]]
        (is (= painted-end (get-in (tl/caret-result layout 0 col) [:rect :x]))))
      (is (= 0.0 (get-in (tl/caret-result layout 0 6) [:rect :x])))
      (is (= (tl/tagged-index 6) (:index (tl/caret-result layout 0 6)))))
    (testing "hit after paint chooses consumed-start for one and many spaces"
      (is (= (tl/tagged-index 3)
             (:index (tl/hit-test-result layout [(+ painted-end 20) 2]))))
      (let [one (tl/layout (layout-input "abc def"))]
        (is (= (tl/tagged-index 3)
               (:index (tl/hit-test-result one [100 2]))))))
    (testing "consumed-only selections have zero width"
      (is (zero? (get-in (tl/selection-result layout 0 4 5) [:rect :w])))
      (is (zero? (get-in (tl/selection-result layout 0 3 6) [:rect :w]))))
    (testing "copy is source-based across and wholly inside the break"
      (is (= "  " (:text (tl/copy-result layout
                                         [(tl/tagged-index 4)
                                          (tl/tagged-index 6)]))))
      (is (= "bc   de" (:text (tl/copy-result layout
                                              [(tl/tagged-index 1)
                                               (tl/tagged-index 8)])))))))

(def pinned-key-input
  {:address [:opaque-text :g1-root]
   :stamp "rev-7"
   :body-text "abc" :header-texts ["noise" "prose"]
   :provider (synthetic-provider)
   :font-size 14 :line-height 20 :baseline-offset 14
   :wrap-policy :block-greedy :wrap-col 40})

(def pinned-key
  [[[:opaque-text :g1-root]
    [:stamped "rev-7"]
    ["abc" ["noise" "prose"]]]
   [:mock/proportional "mock-variable-v1" :mock/harfbuzz "8.3"
    ["kern" "liga" "clig"] {:wght 425 :wdth 92}
    {:wght {:min 100 :default 400 :max 800}
     :wdth {:min 75 :default 100 :max 125}}
    [{:id :mock/cjk :revision "cjk-v1"}] 1000 800 -200 100]
   [2 14 20 14 :block-greedy [:columns 40] "und" :bidi {:columns 4}
    [:utf-16-code-unit 1]]])

(deftest exact-layout-key-uses-an-opaque-address
  (is (= pinned-key (tl/layout-key pinned-key-input)))
  (is (= [:unstamped]
         (get-in (tl/layout-key (dissoc pinned-key-input :stamp)) [0 1])))
  (testing "only declared semantic inputs affect identity"
    (is (not= (tl/layout-key pinned-key-input)
              (tl/layout-key (assoc pinned-key-input :body-text "abd"))))
    (is (not= (tl/layout-key pinned-key-input)
              (tl/layout-key (assoc pinned-key-input :address :other))))
    (is (= (tl/layout-key pinned-key-input)
           (tl/layout-key (assoc pinned-key-input :bindings {:x 1}
                                 :contribution-stamp 9 :attention :hot
                                 :origin [99 40] :zoom 100 :hover? true
                                 :sig :must-not-enter))))
    (is (not= (tl/layout-key pinned-key-input)
              (tl/layout-key (assoc pinned-key-input
                                    :header-texts ["noise" "changed"]))))
    (is (not= (tl/layout-key pinned-key-input)
              (tl/layout-key (assoc pinned-key-input :body-text "folded")))))
  (testing "reference shape occurs on a miss, never on a cache hit or unbounded"
    (let [address (get-in pinned-key [0 0])
          input (layout-input "abc def")
          key (tl/layout-key pinned-key-input)
          miss (tl/layout-cache-acquire (tl/empty-layout-cache) address key
                                        #(tl/layout input))
          hit (tl/layout-cache-acquire (:cache miss) address key
                                       #(throw (ex-info "hit built" {})))
          unbounded (tl/layout (layout-input "abc def" {:wrap-col nil}))]
      (is (= 1 (get-in miss [:result :receipts :reference-shapes])))
      (is (:hit? hit))
      (is (= 0 (get-in unbounded [:receipts :reference-shapes])))
      (is (nil? (:reference-advance unbounded))))))

(defn- result [id]
  {:layout/id id :retained {:id id}})

(deftest cache-lifecycle-digest-and-oracle
  (let [a [:ground-text :a :block-root 0]
        aux [:ground-text :a :notice 0]
        b [:ground-text :b :block-root 0]
        k1 [:k 1] k2 [:k 2] ka [:ka] kb [:kb]
        c0 (tl/empty-layout-cache)
        m1 (tl/layout-cache-acquire c0 a k1 #(result "a"))
        m2 (tl/layout-cache-acquire (:cache m1) a k2 #(result "b"))
        m3 (tl/layout-cache-acquire (:cache m2) aux ka #(result "aux"))
        m4 (tl/layout-cache-acquire (:cache m3) b kb #(result "other"))
        surviving (tl/layout-cache-remove-addresses (:cache m4) [aux])
        dead (tl/layout-cache-remove-addresses surviving [a b])
        dead-again (tl/layout-cache-remove-addresses dead [a b])]
    (is (= {:size 1 :address-count 1 :hits 0 :misses 1 :replacements 0
            :oracle-checks 0 :oracle-mismatches 0
            :id-digest "0eb5b8d6f81bc677da8a08567cc4fa9a06a57e9ec8da85ed73a7f62727996002"}
           (tl/layout-cache-report (:cache m1))))
    (is (= 1 (:size (tl/layout-cache-report (:cache m1)))))
    (is (= {a k2} (:address->key (:cache m2))))
    (is (= {k2 (result "b")} (:key->result (:cache m2))))
    (is (= 1 (:replacements (tl/layout-cache-report (:cache m2)))))
    (is (= #{a b} (set (keys (:address->key surviving)))))
    (is (= #{k2 kb} (set (keys (:key->result surviving)))))
    (is (= "d852c641db817b824f48293c60fb140c518bed8247588a39019ab87a0b5a26b1"
           (:id-digest (tl/layout-cache-report surviving))))
    (is (empty? (:address->key dead)))
    (is (empty? (:key->result dead)))
    (is (= dead dead-again)))
  (testing "one thousand revisions replace in place"
    (let [address [:ground-text :a :block-root 0]
          cache (reduce (fn [cache i]
                          (:cache (tl/layout-cache-acquire
                                   cache address [:k i] #(result (str i)))))
                        (tl/empty-layout-cache) (range 1000))]
      (is (= 1 (:size (tl/layout-cache-report cache))))
      (is (= 1 (:address-count (tl/layout-cache-report cache))))
      (is (= 999 (:replacements cache)))))
  (testing "positive oracle checks full values; stale seed is rejected"
    (let [address [:ground-text :a :block-root 0]
          key [:k]
          fresh (result "fresh")
          miss (tl/layout-cache-acquire (tl/empty-layout-cache) address key
                                        (constantly fresh))
          good (tl/layout-cache-acquire (:cache miss) address key
                                        (constantly fresh) :oracle? true)
          stale-cache (assoc-in (:cache miss) [:key->result key]
                                (result "stale"))
          bad (tl/layout-cache-acquire stale-cache address key
                                       (constantly fresh) :oracle? true)]
      (is (:oracle-match? good))
      (is (false? (:oracle-match? bad)))
      (is (= 1 (get-in bad [:cache :oracle-mismatches])))
      (is (= 1 (get-in bad [:cache :oracle-checks]))))))

(deftest cache-counter-reset-preserves-owned-values
  (let [address [:ground-text :reset]
        key [:key :reset]
        value (result "reset-layout")
        acquired (:cache (tl/layout-cache-acquire
                          (tl/empty-layout-cache) address key (constantly value)))
        hit (:cache (tl/layout-cache-acquire
                     acquired address key (constantly value)))
        reset (tl/layout-cache-reset-counters hit)]
    (is (= (:address->key hit) (:address->key reset)))
    (is (= (:key->result hit) (:key->result reset)))
    (is (= (:id-digest (tl/layout-cache-report hit))
           (:id-digest (tl/layout-cache-report reset))))
    (is (= {:hits 0 :misses 0 :replacements 0
            :oracle-checks 0 :oracle-mismatches 0}
           (select-keys reset
                        [:hits :misses :replacements
                         :oracle-checks :oracle-mismatches])))))

;; ---------------------------------------------------------------------------
;; G1 "Linearity, constants bound" fixtures.
;;
;; The contracted pathological fixture is ONE source line, >=4,000 clusters,
;; >=3 runs, mixed break-whitespace, cluster-monotonic glyph order. `path-unit`
;; carries BOTH members of the section 5.2 break-whitespace class (U+0020 and
;; U+0009) and no newline, so any repetition of it is exactly one source line
;; whose wrap cuts consume both a lone-SPACE run and a SPACE+TAB run.

(def ^:private path-unit "ab \tcd ef ")

(defn- pathological-text [units] (apply str (repeat units path-unit)))

;; G1's growth row reads "work-units(4x input) <= 5 * work-units(1x input)", so
;; the 4x fixture is literally four repetitions of the 1x text, and that 4x text
;; is the pathological fixture (4,400 code units => >=4,000 clusters).
(def ^:private growth-1x-units 110)
(def ^:private pathological-1x (pathological-text growth-1x-units))
(def ^:private pathological-4x (pathological-text (* 4 growth-1x-units)))

(defn- pathological-layout [text shuffle?]
  (tl/layout (layout-input text
                           {:provider (pathological-provider {:shuffle? shuffle?})
                            :wrap-col 40})))

(defn- work-shape
  "G, C, R and the retained proportionality receipt of one layout."
  [layout]
  {:g (count (mapcat tl/line-glyphs (:lines layout)))
   :c (count (mapcat tl/line-clusters (:lines layout)))
   :r (count (tl/result-runs layout))
   :receipt (get-in layout [:receipts :proportionality])})

(defn- monotonic-line? [line]
  (every? (fn [[a b]]
            (<= (long (:cluster-start a)) (long (:cluster-start b))))
          (partition 2 1 (tl/line-glyphs line))))

(defn- consumed-text [text line]
  (when-let [range (:consumed-range line)]
    (let [[start end] (offsets range)]
      (subs text start end))))

(defn- sort-allowance
  "G1's one permitted sort: G*ceil(log2(G+1))."
  [glyph-count]
  (* glyph-count
     (long (Math/ceil (/ (Math/log (inc glyph-count)) (Math/log 2))))))

(deftest pathological-linearity-constants-bound
  (let [layout (pathological-layout pathological-4x false)
        {:keys [g c r receipt]} (work-shape layout)
        consumed (keep #(consumed-text pathological-4x %) (body-lines layout))]
    (testing "the fixture is exactly G1's contracted pathological shape"
      (is (not (str/includes? pathological-4x "\n")))
      (is (= 1 (count (get-in layout [:receipts :source-lines]))))
      (is (= [pathological-4x] (get-in layout [:receipts :source-lines])))
      (is (>= (count pathological-4x) 4000))
      (is (>= c 4000))
      (is (>= r 3))
      (testing "mixed break-whitespace, both classes actually consumed"
        (is (some #(= " " %) consumed))
        (is (some #(str/includes? % "\t") consumed))
        (is (every? #(re-matches #"[ \t]+" %) consumed)))
      (testing "cluster-monotonic glyph order"
        (is (every? monotonic-line? (:lines layout)))))
    (testing "work-units <= 4*(G+C+R), covering index build AND wrap-cut selection"
      (is (<= (tl/work-units receipt) (* 4 (+ g c r))))
      (is (tl/within-work-bound? receipt g c r))
      (testing "every counter of the exact vocabulary is instrumented"
        (is (every? #(contains? receipt %) tl/work-counter-keys))
        (is (pos? (:wrap-candidate-visits receipt)))))
    (testing "per source line, shape-calls <= 1 + final-segment count"
      (is (<= (:shape-calls receipt) (inc (count (:lines layout))))))))

(deftest work-unit-growth-row
  (testing "the 4x fixture is literally four repetitions of the 1x input"
    (is (= pathological-4x (apply str (repeat 4 pathological-1x)))))
  (let [one-work (tl/work-units
                  (:receipt (work-shape
                             (pathological-layout pathological-1x false))))
        four-work (tl/work-units
                   (:receipt (work-shape
                              (pathological-layout pathological-4x false))))]
    (is (pos? one-work))
    (is (<= four-work (* 5 one-work)))))

(deftest shuffled-glyph-order-linearity-and-exact-index-vectors
  (let [monotonic (pathological-layout pathological-4x false)
        shuffled (pathological-layout pathological-4x true)
        {:keys [g c r receipt]} (work-shape shuffled)]
    (testing "the twin is genuinely non-monotonic over identical content"
      (is (every? monotonic-line? (:lines monotonic)))
      (is (some (complement monotonic-line?) (:lines shuffled)))
      (is (= (mapv :text (:lines monotonic)) (mapv :text (:lines shuffled))))
      (is (= (mapv :consumed-range (:lines monotonic))
             (mapv :consumed-range (:lines shuffled)))))
    (testing "work-units <= 4*(G+C+R) + G*ceil(log2(G+1))"
      (is (<= (tl/work-units receipt) (+ (* 4 (+ g c r)) (sort-allowance g))))
      (is (tl/within-work-bound? receipt g c r :non-monotonic? true)))
    (testing "non-monotonic sources retain EXACT index vectors, not the span"
      (let [range [(tl/tagged-index 4) (tl/tagged-index 12)]
            m (tl/glyphs-in-source-range (first (:lines monotonic)) range)
            s (tl/glyphs-in-source-range (first (:lines shuffled)) range)]
        (is (= 8 (:visited-glyphs m)))
        (is (= 8 (:visited-glyphs s)))
        (is (= [4 12] (:glyph-span m)))
        ;; The shuffled [min,max) span is strictly wider than the selection:
        ;; proof the selection reads :glyph-indexes and never the span.
        (is (> (- (second (:glyph-span s)) (first (:glyph-span s)))
               (:visited-glyphs s)))
        (is (= (frequencies (map :character (:glyphs m)))
               (frequencies (map :character (:glyphs s)))))
        (is (= (mapv :cluster-start (:glyphs m))
               (vec (sort (map :cluster-start (:glyphs s))))))))
    (testing "the provider's paint order survives into the retained line"
      (let [base (mapv :cluster-start
                       (tl/line-glyphs (first (:lines monotonic))))]
        (is (= (vec (concat (take-nth 2 base) (take-nth 2 (rest base))))
               (mapv :cluster-start
                     (tl/line-glyphs (first (:lines shuffled))))))))))

;; ---------------------------------------------------------------------------
;; G1 "Captured seeded negatives" (a) and (b): the rejected strategies are RUN,
;; not described. Each walks the same retained fixture the positive rows use,
;; produces the same ANSWER as the indexed road, and emits its cost through the
;; same counter vocabulary -- so the gate predicates judge a measurement.

(defn- legacy-per-cluster-glyph-scan
  "Negative (a): the rejected O(C*G) cluster-membership strategy (shaped-layout's
   old per-cluster whole-glyph filter). Every cluster re-scans its line's WHOLE
   glyph vector; each visit increments G1's own `:glyph-visits`."
  [lines]
  (let [!visits (volatile! 0)
        membership
        (mapv (fn [line]
                (let [glyphs (tl/line-glyphs line)
                      n (count glyphs)]
                  (mapv (fn [cluster]
                          (loop [i 0 out []]
                            (if (>= i n)
                              out
                              (let [glyph (nth glyphs i)]
                                (vswap! !visits inc)
                                (recur (inc i)
                                       (if (and (= (:source-start cluster)
                                                   (:cluster-start glyph))
                                                (= (:source-end cluster)
                                                   (:cluster-end glyph)))
                                         (conj out i)
                                         out))))))
                        (remove :consumed? (tl/line-clusters line)))))
              lines)]
    {:membership membership
     :receipt {:glyph-visits @!visits}}))

(defn- indexed-cluster-glyph-membership
  "The linear road's answer: the retained exact index vectors."
  [lines]
  (mapv (fn [line]
          (mapv :glyph-indexes (tl/glyph-span-index-view line)))
        lines))

(defn- legacy-full-vector-span-scan
  "Negative (b): the rejected per-op full-glyph-vector reader (the renderer's
   per-op glyph filter). Selecting a source range visits EVERY glyph of the op;
   the cost is emitted through G6's own `:visited-glyphs`/`:glyph-span-count`."
  [line [start end]]
  (let [glyphs (tl/line-glyphs line)
        n (count glyphs)
        !visited (volatile! 0)
        selected (loop [i 0 out []]
                   (if (>= i n)
                     out
                     (let [glyph (nth glyphs i)
                           [glyph-start _]
                           (offsets (:source-range (:cluster glyph)))]
                       (vswap! !visited inc)
                       (recur (inc i)
                              (if (and (<= start glyph-start)
                                       (< glyph-start end))
                                (conj out glyph)
                                out)))))]
    {:glyphs selected
     :visited-glyphs @!visited
     :glyph-span-count (count selected)}))

(deftest indexed-span-selection-and-captured-negatives
  (let [layout (pathological-layout pathological-4x false)
        {:keys [g c r]} (work-shape layout)
        line (first (:lines layout))
        range [(tl/tagged-index 4) (tl/tagged-index 12)]
        selected (tl/glyphs-in-source-range line range)]
    (testing "the indexed span road visits only the selected span"
      (is (= 8 (:visited-glyphs selected)))
      (is (tl/within-span-bound?
           {:visited-glyphs (:visited-glyphs selected) :glyph-span-count 8})))
    (testing "negative (a): the instrumented per-cluster whole-glyph strategy"
      (let [legacy (legacy-per-cluster-glyph-scan (:lines layout))]
        ;; Same answer as the linear indexer -- a real strategy, not a literal.
        (is (= (indexed-cluster-glyph-membership (:lines layout))
               (:membership legacy)))
        (is (pos? (:glyph-visits (:receipt legacy))))
        (is (> (:glyph-visits (:receipt legacy)) (* 4 (+ g c r))))
        (is (false? (tl/within-work-bound? (:receipt legacy) g c r)))
        ;; Rejected even with the shuffled fixture's sort allowance granted.
        (is (false? (tl/within-work-bound? (:receipt legacy) g c r
                                           :non-monotonic? true)))))
    (testing "negative (b): the instrumented full-vector per-op reader"
      (let [legacy (legacy-full-vector-span-scan line [4 12])]
        (is (= (mapv :character (:glyphs selected))
               (mapv :character (:glyphs legacy))))
        (is (= (count (tl/line-glyphs line)) (:visited-glyphs legacy)))
        (is (> (:visited-glyphs legacy) (:glyph-span-count legacy)))
        (is (false? (tl/within-span-bound? legacy)))))
    (testing "the T0 legacy road keeps its contiguous-subvector selection"
      (let [legacy (tl/layout {:text "abcdef" :char-advance 8
                               :font-size 14 :line-height 20})
            legacy-line (first (:lines legacy))
            legacy-selected
            (tl/glyphs-in-source-range
             legacy-line [(tl/tagged-index 1) (tl/tagged-index 4)])]
        (is (= "bcd" (apply str (map :character (:glyphs legacy-selected)))))
        (is (= [1 4] (:glyph-span legacy-selected)))
        (is (= 3 (:visited-glyphs legacy-selected)))))
    (is (= line (tl/line-by-id layout (:line/id line))))
    (is (<= (:visited-lines
             (tl/clip-result (assoc-in layout [:constraints :clip]
                                       {:left 0 :right 20})
                             {:layout-line-id (:line/id line)
                              :from 4 :to 12
                              :text (subs pathological-4x 4 12)
                              :x 0 :y 10}))
            2))))

(deftest multiline-span-index-uses-the-lines-source-domain
  (testing "first and later document lines both select their retained glyphs"
    (let [layout (tl/layout (layout-input "first\nlater" {:wrap-policy :none
                                                           :wrap-col nil}))
          [first-line later-line] (body-lines layout)
          first-selection (tl/glyphs-in-source-range
                           first-line (:paint-source-range first-line))
          later-selection (tl/glyphs-in-source-range
                           later-line (:paint-source-range later-line))]
      (is (= [0 5] (offsets (:source-range first-line))))
      (is (= [6 11] (offsets (:source-range later-line))))
      (is (= [0 1]
             ((juxt :source-start :source-end)
              (first (tl/glyph-span-index-view first-line)))))
      (is (= [6 7]
             ((juxt :source-start :source-end)
              (first (tl/glyph-span-index-view later-line)))))
      (is (= "first" (apply str (map :character (:glyphs first-selection)))))
      (is (= "later" (apply str (map :character (:glyphs later-selection)))))
      (is (= 5 (:visited-glyphs first-selection)))
      (is (= 5 (:visited-glyphs later-selection)))))
  (testing "wrapped later-line segments retain absolute body spans"
    (let [layout (tl/layout (layout-input "top\nabcdef" {:wrap-col 3}))
          lines (body-lines layout)
          wrapped-a (nth lines 1)
          wrapped-b (nth lines 2)
          select #(tl/glyphs-in-source-range % (:paint-source-range %))]
      (is (= [4 7] (offsets (:source-range wrapped-a))))
      (is (= [7 10] (offsets (:source-range wrapped-b))))
      (is (= [4 5]
             ((juxt :source-start :source-end)
              (first (tl/glyph-span-index-view wrapped-a)))))
      (is (= [7 8]
             ((juxt :source-start :source-end)
              (first (tl/glyph-span-index-view wrapped-b)))))
      (is (= "abc" (apply str (map :character (:glyphs (select wrapped-a))))))
      (is (= "def" (apply str (map :character (:glyphs (select wrapped-b))))))))
  (testing "header span indices remain local to each per-header domain"
    (let [layout (tl/layout (layout-input "body" {:wrap-policy :none
                                                   :wrap-col nil
                                                   :headers ["head"]}))
          header (first (:lines layout))
          selection (tl/glyphs-in-source-range
                     header (:paint-source-range header))]
      (is (= [:header 0 [0 4]] (:source-range header)))
      (is (= [0 1]
             ((juxt :source-start :source-end)
              (first (tl/glyph-span-index-view header)))))
      (is (= "head" (apply str (map :character (:glyphs selection)))))
      (is (= 4 (:visited-glyphs selection))))))
