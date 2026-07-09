(ns app.server.rama.object-container.block-distiller-test
  "block-distiller Phase 0 — PURE tests: free cut (G3), actor resolution
   (G1/§16.1), span property + anchor honesty (G11/G6), id determinism, fence
   atomicity (N2), NUL round-trip (T8), and the F1 read-string spike over both
   the fixture and the REAL 7c80ce2a file. No IPC (CONTRACT §6). Gates that need
   the container/relation kernels (G4/G5 physical, G6 physical, G7-G10) land in
   the Phase 1+ IPC deftests."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.rama.object-container.transcript-adapter :as tra]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.dogfood.transcript :as tr]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

;; ===========================================================================
;; fixture plumbing
;; ===========================================================================

(def fixture-path "test/resources/block-distiller/fixture.jsonl")
(def golden-path "test/resources/block-distiller/golden.edn")

(defn- inject-nul
  "Replace the ASCII [NUL] sentinel with a real NUL char (T8). The fixture file
   stays git-safe ASCII; the test exercises the real control byte through the
   pr-str/read-string pipeline."
  [x]
  (cond
    (string? x)     (str/replace x "[NUL]" (str (char 0)))
    (map? x)        (into (empty x) (map (fn [[k v]] [k (inject-nul v)])) x)
    (sequential? x) (mapv inject-nul x)
    :else           x))

(defn fixture-lines []
  (remove str/blank? (str/split-lines (slurp fixture-path))))

(defn redacted-events
  "Parse + redact each fixture line exactly as the transcript adapter stores it,
   then inject the real NUL. This is the redacted-payload shape distill-event
   consumes in production (via read-string of the stored source-raw-text)."
  []
  (mapv (fn [l]
          (-> (tr/parse-json-line l)
              tr/redact-payload-with-redactions
              :payload
              inject-nul))
        (fixture-lines)))

(defn distilled []
  (map-indexed (fn [i p] (bd/distill-event p i)) (redacted-events)))

(defn golden-projection
  "Stable, human-readable projection of the distillation — the checked-in
   golden. Captures class/actor/delegation per event and form/span/text per
   block (G3 asserts exact equality)."
  [ds]
  (mapv (fn [d]
          {:order         (:order d)
           :event-key     (:event-key d)
           :raw-type      (:raw-type d)
           :class         (get-in d [:class :class])
           :reason        (get-in d [:class :reason])
           :actor         (get-in d [:production-event :production/actor])
           :is-sidechain  (get-in d [:production-event :delegation/is-sidechain])
           :on-behalf-of  (get-in d [:production-event :delegation/on-behalf-of])
           :parent-uuid   (get-in d [:production-event :delegation/parent-uuid])
           :blocks        (mapv (fn [b] {:block-path (:block-path b)
                                         :part-path  (:part-path b)
                                         :unit-kind  (:unit-kind b)
                                         :actor      (:actor b)
                                         :span       [(:start-offset b) (:end-offset b)]
                                         :text       (:text b)})
                                (:blocks d))})
        ds))

(defn- find-real-transcript
  "The real example chat (spans only per CONTRACT §11); nil if absent so the
   suite stays green off-box."
  []
  (let [dir (io/file (str (System/getProperty "user.home")
                          "/.claude/projects/-mnt-data-projects-Softland"))]
    (when (.isDirectory dir)
      (first (filter #(re-find #"^7c80ce2a-.*\.jsonl$" (.getName ^java.io.File %))
                     (.listFiles dir))))))

;; ===========================================================================
;; G3 — free cut golden (forms + spans + exact text)
;; ===========================================================================

(deftest g3-free-cut-golden
  (testing "distill-event over the fixture matches the checked-in golden exactly"
    (is (= (edn/read-string (slurp golden-path))
           (golden-projection (distilled))))))

;; ===========================================================================
;; G1 — classification + actor resolution (SPEC §3, §3.4, §16.1)
;; ===========================================================================

(deftest g1-classification
  (let [ds (distilled)]
    (testing "every event receives a class from the versioned classifier"
      (is (every? #(#{:river :debris} (get-in % [:class :class])) ds))
      (is (every? #(= bd/river-debris-classifier-id (:classifier-id %))
                  (map #(bd/classify-event % (bd/event-type* %)) (redacted-events)))))
    (testing "harness ops + isMeta + command wrappers are debris (SPEC §3.2)"
      (is (= [:debris :debris :debris :debris :debris :debris :river :river :river :river :river]
             (map #(get-in % [:class :class]) ds))))
    (testing "the debris/river ratio is real — most of the stream is not river"
      (is (= 6 (count (filter #(= :debris (get-in % [:class :class])) ds))))
      (is (= 5 (count (filter #(= :river (get-in % [:class :class])) ds)))))))

(deftest f3b-surfaceless-river-counts-as-river
  ;; Gate Round-2 Finding 1: a river-class event whose only part is EMPTY (an empty
  ;; tool_result) produces no surface import, but SPEC §3.1 still demands a durable
  ;; versioned class row — and it must count as RIVER, not debris.
  (let [object-key (tid/transcript-object-key "src" "conv-empty")
        parsed {:type "user" :uuid "u-empty-tr"
                :message {:role "user"
                          :content [{:type "tool_result" :tool_use_id "t1" :content ""}]}}
        distilled (bd/distill-event parsed 0)]
    (is (= :river (get-in distilled [:class :class])) "empty tool_result is river-class (material)")
    (is (empty? (:blocks distilled)) "an empty part yields no blocks")
    (is (nil? (bd/event-import-request object-key parsed 0)) "surfaceless river event has NO surface import")
    (let [{:keys [request class]} (bd/class-hint-import-request object-key parsed 0)]
      (is (= :river class) "counted as RIVER, not debris (Finding 1)")
      (is (some? request) "a durable class-hint import IS produced (SPEC §3.1)")
      (is (= [:river] (mapv :entry-kind (:projection-hints (:payload request))))
          "the durable class row carries entry-kind :river"))))

(deftest g1-actor-resolution-law
  (testing "role ≠ actor (SPEC §3.4 / §16.1): no tool_result or meta is ever the human"
    (doseq [[i parsed] (map-indexed vector (redacted-events))]
      (doseq [part (bd/event-parts parsed)]
        (let [actor (bd/resolve-actor parsed part)]
          (cond
            (= :tool-result (:part-type part))
            (is (= "tool" actor) (str "event " i " tool_result must resolve to tool, got " actor))

            (:isMeta parsed)
            (is (= "harness" actor) (str "event " i " isMeta must resolve to harness"))

            (= "assistant" (bd/message-role parsed))
            (is (= "claude-fable-5" actor) (str "event " i " assistant → model id"))

            (= :human-message (:part-type part))
            (is (str/starts-with? actor "human:") (str "event " i " human message → human:*")))))))
  (testing "no block anywhere is a human-attributed tool_result surface"
    (doseq [d (distilled), b (:blocks d)]
      (is (not (and (str/starts-with? (str (:actor b)) "human")
                    (= :tool-result-span (:unit-kind b))))))))

(deftest g2-delegation-chain
  (testing "sidechain assistant carries on-behalf-of = parent (SPEC §7.1)"
    (let [ds (distilled)
          sub (first (filter #(get-in % [:production-event :delegation/is-sidechain]) ds))]
      (is (some? sub))
      (is (= "a-1" (get-in sub [:production-event :delegation/on-behalf-of])))
      (is (= "claude-fable-5" (get-in sub [:production-event :production/actor])))))
  (testing "non-sidechain events have no on-behalf-of"
    (let [main (first (filter #(and (= "assistant" (:raw-type %))
                                    (not (get-in % [:production-event :delegation/is-sidechain])))
                              (distilled)))]
      (is (nil? (get-in main [:production-event :delegation/on-behalf-of]))))))

;; ===========================================================================
;; G11 — span property + G6 anchor honesty (pure)
;; ===========================================================================

(deftest g11-span-property
  (testing "every block span is in-bounds, non-empty, surrogate-safe, and slices honestly"
    (doseq [[i parsed] (map-indexed vector (redacted-events))]
      (let [d (bd/distill-event parsed i)
            part-by-path (into {} (map (juxt :part-path identity) (:parts d)))]
        (doseq [b (:blocks d)]
          (let [^String ptext (:text (get part-by-path (:part-path b)))
                s (long (:start-offset b))
                e (long (:end-offset b))
                n (count ptext)]
            (is (and (<= 0 s) (< s e) (<= e n))
                (str "in-bounds+non-empty " (:block-path b) " span [" s " " e ") in " n))
            (is (= (:text b) (subs ptext s e))
                (str "anchor honesty (G6) — subs==text " (:block-path b)))
            (when (> s 0)
              (is (not (and (Character/isHighSurrogate (.charAt ptext (dec s)))
                            (Character/isLowSurrogate (.charAt ptext s))))
                  (str "no surrogate split at start " (:block-path b))))
            (when (< e n)
              (is (not (and (Character/isHighSurrogate (.charAt ptext (dec e)))
                            (Character/isLowSurrogate (.charAt ptext e))))
                  (str "no surrogate split at end " (:block-path b))))))))))

;; ===========================================================================
;; §A ids — deterministic + correctly shaped (N1 routing constraints)
;; ===========================================================================

(deftest id-minters
  (testing "deterministic + shaped for object-key routing (N1)"
    (is (= "du:chat:H:sense-block-v0:000001:00:000000"
           (bd/derived-unit-id "chat:H" (bd/block-path 1 0 0))))
    (is (= "000012:03:000007" (bd/block-path 12 3 7)))
    (is (str/starts-with? (bd/per-part-source-id "chat:H" "uuid" "content/0") "src:tr:chat:H:")
        "per-part source-ids MUST reuse src:tr: so extract-object-key routes them (N1b)")
    (is (str/starts-with? (bd/import-key "chat:H" "u") "imp:tr:chat:H:sb:")
        "imp:tr:<ok>:sb: routes via extract-object-key yet stays distinct from the transcript adapter's imp:tr:<ok>:<hash> (F2/R4)")
    (is (= (bd/edge-idempotency-key "a" :produced "b") (bd/edge-idempotency-key "a" :produced "b")))
    (is (not= (bd/edge-idempotency-key "a" :produced "b") (bd/edge-idempotency-key "a" :grounds "b")))
    (is (not= (bd/per-part-source-id "chat:H" "u1" "content/0")
              (bd/per-part-source-id "chat:H" "u2" "content/0"))
        "distinct (event,part) ⇒ distinct surface id — no collision with the transcript adapter")))

;; ===========================================================================
;; §F — fence / blockquote / table atomicity (N2, SPEC §4.2 MUST)
;; ===========================================================================

(deftest fence-atomicity
  (testing "a code fence is never split"
    (let [blocks (bd/free-cut-text "before\n\n```clojure\n(a)\n(b)\n```\n\nafter")]
      (is (= [:prose-para :code-fence :prose-para] (mapv :unit-kind blocks)))
      (is (some #(and (= :code-fence (:unit-kind %))
                      (str/includes? (:text %) "(a)")
                      (str/includes? (:text %) "(b)"))
                blocks))))
  (testing "an unclosed fence runs to end-of-text (never splits)"
    (let [blocks (bd/free-cut-text "intro\n\n```\nno close here\nmore")]
      (is (= :code-fence (:unit-kind (last blocks))))
      (is (str/includes? (:text (last blocks)) "more"))))
  (testing "a blockquote is atomic per quote"
    (let [blocks (bd/free-cut-text "p\n\n> line one\n> line two\n\nq")]
      (is (= [:prose-para :blockquote :prose-para] (mapv :unit-kind blocks)))))
  (testing "a GFM table is atomic"
    (let [blocks (bd/free-cut-text "| a | b |\n| --- | --- |\n| 1 | 2 |")]
      (is (= [:table] (mapv :unit-kind blocks)))))
  (testing "human subs collapse to :human-sub form (SPEC §4.6)"
    (is (= [:human-sub :human-sub]
           (mapv :unit-kind (bd/free-cut-text "para one\n\npara two" {:human? true}))))))

;; ===========================================================================
;; T8 — NUL survives pr-str → read-string (the fixture's control-byte slot)
;; ===========================================================================

(deftest nul-roundtrip
  (testing "a NUL byte survives the store round-trip (pr-str → read-string)"
    (let [payload {:message {:content [{:type "tool_result" :content (str "a" (char 0) "b")}]}}]
      (is (= payload (edn/read-string (pr-str payload))))
      (is (str/includes? (get-in (edn/read-string (pr-str payload))
                                 [:message :content 0 :content])
                         (str (char 0))))))
  (testing "the fixture's tool_result carries the injected NUL"
    (let [tr-event (nth (redacted-events) 8)
          content (get-in tr-event [:message :content 0 :content])]
      (is (str/includes? content (str (char 0)))))))

;; ===========================================================================
;; F1 — read-string spike: river round-trips; debris (that would throw) never read
;; ===========================================================================

(deftest f1-read-string-spike-fixture
  (testing "every RIVER event round-trips pr-str → read-string (the safety property the driver relies on)"
    (doseq [[i parsed] (map-indexed vector (redacted-events))]
      (when (= :river (:class (bd/classify-event parsed (bd/event-type* parsed))))
        (is (= parsed (edn/read-string (pr-str parsed)))
            (str "river event " i " (" (bd/event-type* parsed) ") must round-trip")))))
  (testing "debris is classified out and never cut — a non-round-trippable debris payload is never read"
    (doseq [d (distilled)]
      (when (= :debris (get-in d [:class :class]))
        (is (empty? (:blocks d)) (str "debris event " (:order d) " produces no blocks")))))
  (testing "FINDING (F1 does NOT reproduce): file-history-snapshot path keys round-trip fine;
            the real guarded shape is a whitespace key, which does not occur in 7c80ce2a"
    (let [fhs (first (filter #(= "file-history-snapshot" (bd/event-type* %)) (redacted-events)))]
      (is (= :debris (:class (bd/classify-event fhs (bd/event-type* fhs)))))
      (is (= fhs (edn/read-string (pr-str fhs)))
          "leading-slash path keys parse to ns=\"\" and DO round-trip — plan F1's mechanism was wrong")
      ;; the actual non-EDN shape the driver's classify-first + try/catch guards:
      (is (thrown? Exception
                   (edn/read-string (pr-str {:snapshot {(keyword "/p with space.txt") {:content "x"}}})))
          "a whitespace-containing key IS non-EDN — classify-first is defense-in-depth for this class"))))

(deftest f1-read-string-spike-real-file
  (testing "F1 holds on the REAL 7c80ce2a file: every river event round-trips"
    (if-let [real (find-real-transcript)]
      (let [events (->> (str/split-lines (slurp real))
                        (remove str/blank?)
                        (map (fn [l] (:payload (tr/redact-payload-with-redactions (tr/parse-json-line l))))))
            river (filter #(= :river (:class (bd/classify-event % (bd/event-type* %)))) events)]
        (is (pos? (count river)) "the real file has river events")
        (doseq [p river]
          (is (= p (edn/read-string (pr-str p)))
              (str "real river event (" (bd/event-type* p) ") must round-trip"))))
      (println "  [skip] real 7c80ce2a transcript not present — real-file spike skipped"))))

;; ===========================================================================
;; F2 — delegation-home spike (STOP-CLAUSE gate). CONTRACT §9 / PLAN §7/F2.
;; Does an extra :production-event record key on a per-part SourceArtifactRow
;; survive the $$source-artifacts-by-id round-trip? GREEN ⇒ option A (delegation
;; rides the surface, ZERO existing-file edit). RED ⇒ STOP → Sid (option B edits
;; transcript_adapter.clj:255, outside the allowlist). This is the FIRST IPC test:
;; if it fails, the rest of P1 does not get built.
;; ===========================================================================

(deftest f2-production-event-survives-source-round-trip
  (testing "an extra :production-event key on a per-part surface survives the PState round-trip"
    (let [rt (bd/start-distiller-runtime!)
          oc-rt (:oc-rt rt)]
      (try
        (let [object-key "chat:F2SPIKE"
              event-uuid "evt-f2"
              part {:part-path "content/0" :part-index 0 :part-type :text :text "hello world"}
              block {:block-path (bd/block-path 0 0 0) :unit-kind :prose-para
                     :text "hello world" :start-offset 0 :end-offset 11}
              prod-event {:production/class :river
                          :production/actor "claude-fable-5"
                          :delegation/is-sidechain true
                          :delegation/on-behalf-of "a-1"
                          :delegation/prompt-id "p-1"
                          :delegation/parent-uuid "a-1"}
              ctx {:object-key object-key
                   :event-uuid event-uuid
                   :document-container-id (tid/chat-message-id object-key (core/sha-256 event-uuid))
                   :event-id (str "evt:" object-key ":f2")
                   :created-at-ms 123
                   :production-event prod-event}
              {:keys [surface units anchors]} (bd/part-rows ctx part "claude-fable-5" [block])
              payload (bd/import-payload object-key [surface] units anchors [])
              request (bd/import-request object-key event-uuid payload {:time-ms 123})
              _ (ocr/append-object-container-request! oc-rt request)
              decision (ocr/await-object-container-decision oc-rt request 20000)
              source-id (bd/per-part-source-id object-key event-uuid "content/0")
              stored (ocr/read-source oc-rt source-id)]
          (is (some? decision) "import produced a decision")
          (is (= :accepted (:status decision))
              (str "import accepted (else the spike can't judge F2): " (:reason decision)
                   " " (pr-str (:errors decision))))
          (is (some? stored)
              "per-part surface physically readable — direct-build object-key routing OK (N1)")
          (is (= prod-event (:production-event stored))
              "F2 VERDICT: the extra :production-event record key survives $$source-artifacts-by-id"))
        (finally (ocr/close-object-container-runtime! oc-rt))))))

;; ===========================================================================
;; P1 IPC gates — full import + physical PState readers (T7, validation-only).
;; ONE shared runtime hosts the one-unit routing smoke + G3(phys)/G6/G11(phys)/
;; G4/G5 (all read disjoint keys from one distilled fixture; the object_container_
;; test one-launch pattern). Barrier = :append-ack + await-decision. Physical
;; reads (foreign-select) see the negative invariants the query API would mask.
;; ===========================================================================

(def fixture-source :claude-code)
(def fixture-conversation-id "block-fixture-conv")

(defn fixture-obs
  "A transcript observation for one fixture line — the shape the transcript
   adapter ingests, mirroring production. Redacts + injects the real NUL, and
   OMITS :transcript/redacted-preview so source-raw-text = pr-str(full payload) —
   the R4 substrate the driver read-strings (PHASE_0 §2/f)."
  [i line]
  (let [payload (-> (tr/parse-json-line line)
                    tr/redact-payload-with-redactions
                    :payload
                    inject-nul)]
    {:transcript/source fixture-source
     :transcript/conversation-id fixture-conversation-id
     :transcript/message-uuid (or (:uuid payload) (:messageId payload) (str "evt-" i))
     :transcript/redacted-payload payload
     :source/file-key "file:block-fixture"
     :source/file-id "fid:block-fixture"
     :source/file-path "/tmp/block-fixture.jsonl"
     :source/file-generation-key "gen-1"
     :source/byte-offset (* 1000 i)                 ; strictly increasing ⇒ projection order = file order
     :source/byte-length (long (count (.getBytes line "UTF-8")))
     :source/line-hash (oc/source-hash line)}))

(defn ingest-fixture!
  "Ingest ALL fixture lines via the transcript adapter (creates the per-message
   surfaces + conversation projection the driver reads — the R4/F3 substrate)."
  [oc-rt]
  (doseq [[i line] (map-indexed vector (fixture-lines))]
    (let [request (tra/transcript-observation-import-request (fixture-obs i line))]
      (ocr/append-object-container-request! oc-rt request)
      (ocr/await-object-container-decision oc-rt request 20000))))

(defn read-unit-physical
  "Direct $$derived-units-by-id read (T7 validation-only; routes by
   partition-by-object-key on the unit-id, N1)."
  [oc-rt unit-id]
  (foreign-select-one [(keypath unit-id)] (:derived-units-by-id oc-rt)))

(defn read-anchor-physical
  "The single anchor for a unit — $$source-anchors-by-target[unit-id] holds one
   entry (target-id = unit-id; the anchor-id is its subindex key, never routed)."
  [oc-rt unit-id]
  (first (foreign-select [(keypath unit-id) MAP-VALS] (:source-anchors-by-target oc-rt))))

(defn river-blocks
  "Every river block from the PURE distillation (already golden-checked), tagged
   with its event-uuid — the expectation G3/G6/G11 physical rows must match."
  []
  (for [d (distilled)
        :when (= :river (get-in d [:class :class]))
        b (:blocks d)]
    (assoc b :event-uuid (:event-key d))))

(defn river-surfaces
  "Every per-part surface the driver mints (one per non-empty river part)."
  [object-key]
  (for [d (distilled)
        :when (= :river (get-in d [:class :class]))
        part (:parts d)
        :when (not (str/blank? (str (:text part))))]
    {:source-id (bd/per-part-source-id object-key (:event-key d) (:part-path part))
     :text (:text part)}))

(deftest distiller-import-gates
  (let [rt (bd/start-distiller-runtime!)
        oc-rt (:oc-rt rt)]
    (try
      (ingest-fixture! oc-rt)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt
                                               :source fixture-source
                                               :conversation-id fixture-conversation-id})
            object-key (:object-key summary)
            uid #(bd/derived-unit-id object-key (:block-path %))]

        (testing "driver classified 5 river / 6 debris, every import accepted"
          (is (= 5 (:river summary)))
          (is (= 6 (:debris summary)))
          (is (every? #(= :accepted (:status %)) (:decisions summary))
              (str "rejections: " (pr-str (remove #(= :accepted (:status %)) (:decisions summary))))))

        (testing "routing smoke (N1) — a known river unit is physically present"
          (is (some? (read-unit-physical oc-rt (bd/derived-unit-id object-key "000006:00:000000")))
              "if nil, du:/src:tr: id shape mis-routed via extract-object-key"))

        (testing "F2 — import-key routes via extract-object-key: every river import-completion is foreign-readable"
          ;; imp:tr:<ok>:sb:<hash> strips to chat:<hex> (extract-object-key), so a FOREIGN
          ;; read-import-completion hits the task the topology wrote on. The old imp:sense-block:
          ;; prefix fell to :else → whole-string partition → nil for an existing completion (T4).
          ;; This is the FIRST reader of import-completions in the suite (units route fine on du:).
          (doseq [euid (->> (distilled)
                            (filter #(= :river (get-in % [:class :class])))
                            (map :event-key))]
            (is (some? (ocr/read-import-completion oc-rt (bd/import-key object-key euid)))
                (str "import-completion foreign-readable for river event " euid
                     " — F2 routing (nil here = extract-object-key mis-route)"))))

        (testing "G3(physical) — unit-kinds + spans + text match the pure golden"
          (doseq [b (river-blocks)]
            (let [u (read-unit-physical oc-rt (uid b))
                  a (read-anchor-physical oc-rt (uid b))]
              (is (some? u) (str "unit present " (:block-path b)))
              (is (= (:unit-kind b) (:unit-kind u)) (str "unit-kind " (:block-path b)))
              (is (= (:block-path b) (:block-path u)) (str "block-path " (:block-path b)))
              (is (= bd/distiller-id (:distiller-id u)) (str "distiller-id " (:block-path b)))
              (is (= (:text b) (:derived-content-text u)) (str "derived text " (:block-path b)))
              (is (some? a) (str "anchor present " (:block-path b)))
              (is (= :derived-unit (:target-kind a)) (str "target-kind " (:block-path b)))
              (is (= (uid b) (:target-id a)) (str "target-id = unit-id " (:block-path b)))
              (is (= [(long (:start-offset b)) (long (:end-offset b))]
                     [(long (:start-offset a)) (long (:end-offset a))])
                  (str "span " (:block-path b))))))

        (testing "G6 — anchor honesty: subs(raw,span) == text == derived-content-text; hash matches"
          (doseq [b (river-blocks)]
            (let [u (read-unit-physical oc-rt (uid b))
                  a (read-anchor-physical oc-rt (uid b))
                  raw (:source-raw-text (ocr/read-source oc-rt (:source-id u)))
                  sliced (subs raw (long (:start-offset a)) (long (:end-offset a)))]
              (is (= (:text b) sliced) (str "subs==block text " (:block-path b)))
              (is (= (:derived-content-text u) sliced) (str "derived==span " (:block-path b)))
              (is (= (:derived-content-hash u) (oc/source-hash (:text b)))
                  (str "derived-content-hash==sha(text) " (:block-path b))))))

        (testing "G6 — planted secret absent from EVERY stored row (redaction inherited)"
          (doseq [{:keys [source-id]} (river-surfaces object-key)]
            (let [s (ocr/read-source oc-rt source-id)]
              (is (some? s) (str "surface stored " source-id))
              (is (not (str/includes? (str (:source-raw-text s)) "sk-FAKE"))
                  (str "no secret in surface " source-id))))
          (doseq [b (river-blocks)]
            (is (not (str/includes? (str (:derived-content-text (read-unit-physical oc-rt (uid b)))) "sk-FAKE"))
                (str "no secret in unit " (:block-path b)))))

        (testing "G11(physical) — spans in-bounds, non-empty, surrogate-safe"
          (doseq [b (river-blocks)]
            (let [a (read-anchor-physical oc-rt (uid b))
                  ^String raw (:source-raw-text (ocr/read-source oc-rt (:source-id (read-unit-physical oc-rt (uid b)))))
                  s (long (:start-offset a)) e (long (:end-offset a)) n (count raw)]
              (is (and (<= 0 s) (< s e) (<= e n)) (str "in-bounds+non-empty " (:block-path b) " [" s " " e ") in " n))
              (when (> s 0)
                (is (not (and (Character/isHighSurrogate (.charAt raw (dec s)))
                              (Character/isLowSurrogate (.charAt raw s))))
                    (str "no surrogate split at start " (:block-path b))))
              (when (< e n)
                (is (not (and (Character/isHighSurrogate (.charAt raw (dec e)))
                              (Character/isLowSurrogate (.charAt raw e))))
                    (str "no surrogate split at end " (:block-path b)))))))

        (testing "G4 — idempotence: re-run writes zero new units AND is ACCEPTED (not conflict-rejected)"
          (let [before (into {} (for [b (river-blocks)] [(uid b) (read-unit-physical oc-rt (uid b))]))
                counts-before (into {} (for [{:keys [source-id]} (river-surfaces object-key)]
                                         [source-id (count (ocr/read-source-derived-units oc-rt source-id))]))
                rerun (bd/distill-conversation! {:oc-rt oc-rt :source fixture-source
                                                 :conversation-id fixture-conversation-id})
                after (into {} (for [b (river-blocks)] [(uid b) (read-unit-physical oc-rt (uid b))]))
                counts-after (into {} (for [{:keys [source-id]} (river-surfaces object-key)]
                                        [source-id (count (ocr/read-source-derived-units oc-rt source-id))]))]
            ;; N1: the before/after reads must actually SEE units — else {}=={} greens vacuously.
            (is (every? some? (vals before)) "every river unit physically present before the re-run")
            (is (= before after) "re-run leaves every unit byte-identical")
            (is (= counts-before counts-after) "re-run adds no new units to any source")
            ;; F3: distinguish a clean idempotent replay from a fingerprint-CONFLICT reject. A
            ;; conflict-reject ALSO writes no rows → before==after → the gate would green on a
            ;; determinism regression (e.g. a wall-clock leaking into a fingerprinted field). Pin
            ;; that the re-run re-classified the SAME river set and every import came back :accepted.
            (is (= (:river summary) (:river rerun)) "re-run re-classifies the same river count (determinism)")
            (is (every? #(= :accepted (:status %)) (:decisions rerun))
                (str "re-run imports ACCEPTED — clean replay, not a fingerprint-conflict reject: "
                     (pr-str (remove #(= :accepted (:status %)) (:decisions rerun)))))))

        (testing "G5 — strata: a 2nd distiller-id adds disjoint units; sense-block-v0 byte-unchanged"
          (let [v0-uid (bd/derived-unit-id object-key "000006:00:000000")
                v0-before (read-unit-physical oc-rt v0-uid)
                test-distiller "sense-block-test-v1"
                bp (bd/block-path 0 0 0)
                test-uid (str "du:" object-key ":" test-distiller ":" bp)
                test-src (bd/per-part-source-id object-key "test-stratum" "content/0")
                text "stratum two"
                dci (tid/chat-message-id object-key (core/sha-256 "test-stratum"))
                surface (oc/->SourceArtifactRow test-src (bd/per-part-source-ref "test-stratum" "content/0")
                                                (oc/source-hash text) :transcript text dci
                                                (long (count (.getBytes text "UTF-8"))) 0 "tool" "evt:test")
                unit (oc/->DerivedUnitRow test-uid dci test-src :prose-para bp nil
                                          (oc/source-anchor-id test-uid) text (oc/source-hash text)
                                          test-distiller 1 "evt:test")
                anchor (oc/->SourceAnchorRow (oc/source-anchor-id test-uid) :derived-unit test-uid test-src
                                             (bd/per-part-source-ref "test-stratum" "content/0")
                                             (oc/source-hash text) 0 (long (count text)) bp "evt:test")
                request (bd/import-request object-key "test-stratum-evt"
                                           (bd/import-payload object-key [surface] [unit] [anchor] [])
                                           {:time-ms 0})]
            (ocr/append-object-container-request! oc-rt request)
            (let [decision (ocr/await-object-container-decision oc-rt request 20000)]
              (is (= :accepted (:status decision)) (str "2nd stratum import accepted: " (pr-str (:errors decision)))))
            (is (some? (read-unit-physical oc-rt test-uid))
                "2nd distiller unit lands under disjoint du:...:sense-block-test-v1:...")
            (is (= v0-before (read-unit-physical oc-rt v0-uid))
                "sense-block-v0 unit byte-unchanged after the 2nd stratum")))

        ;; ---- P2 gates ----------------------------------------------------
        (testing "G1 — classification ledger: 4 river marked; 6 debris retained-but-unmarked"
          (let [conv-id (tid/chat-conversation-id object-key)
                proj (ocr/read-transcript-conversation-projection oc-rt conv-id "" 100000)
                sb-river (filter #(and (str/starts-with? (str (:order-key %)) "sb:")
                                       (= :river (:entry-kind %)))
                                 proj)
                message-rows (filter #(= :message (:entry-kind %)) proj)]
            (is (= 5 (count sb-river))
                "5 river events in the class ledger (entry-kind :river, sb:-namespaced key)")
            (is (every? #(= :transcript-conversation-projection (:projection-kind %)) sb-river))
            (is (= #{"u-human" "a-1" "u-tr" "a-sub" "u-solo"} (set (map :message-uuid sb-river)))
                "the ledger names exactly the 5 river events")
            ;; SPEC §3.2 physically: debris = retained (transcript :message row) + unmarked (no sb: river hint)
            (is (= 11 (count message-rows))
                "all 11 events retained as transcript :message rows (debris retained, SPEC §3.2)")
            (is (= 6 (- (count message-rows) (count sb-river)))
                "6 retained-but-unmarked events = debris (counts match golden 5 river / 6 debris)")))

        (testing "F3 — every debris event has a DURABLE versioned class row (SPEC §3.1)"
          (let [conv-id (tid/chat-conversation-id object-key)
                proj (ocr/read-transcript-conversation-projection oc-rt conv-id "" 100000)
                sb-debris (filter #(and (str/starts-with? (str (:order-key %)) "sb:")
                                        (= :debris (:entry-kind %)))
                                  proj)
                sb-river (filter #(and (str/starts-with? (str (:order-key %)) "sb:")
                                       (= :river (:entry-kind %)))
                                 proj)]
            (is (= 6 (count sb-debris))
                "all 6 debris events durably classified (entry-kind :debris) — materialized, NOT inferred")
            (is (every? #(= :transcript-conversation-projection (:projection-kind %)) sb-debris))
            (is (every? #(str/starts-with? (str (:content-preview %))
                                           (str bd/river-debris-classifier-id "/"))
                        sb-debris)
                "each debris row carries the classifier VERSION + reason (distinguishable from never-classified)")
            (is (= 11 (+ (count sb-river) (count sb-debris)))
                "all 11 events durably classified (5 river + 6 debris) — no event left unclassified")
            (is (= 6 (:debris summary)) "driver debris count agrees with the durable ledger")
            (is (every? #(= :accepted (:status %)) (:class-hint-decisions summary))
                (str "every hint-only class import ACCEPTED (F3 OC relaxation): "
                     (pr-str (remove #(= :accepted (:status %)) (:class-hint-decisions summary)))))))

        (testing "G1 — actor law (physical): created-by == resolve-actor; NO tool_result/meta is human"
          (doseq [[parsed d] (map vector (redacted-events) (distilled))
                  :when (= :river (get-in d [:class :class]))
                  part (:parts d)
                  :when (not (str/blank? (str (:text part))))]
            (let [expected (bd/resolve-actor parsed part)
                  source-id (bd/per-part-source-id object-key (:event-key d) (:part-path part))
                  cb (:created-by (ocr/read-source oc-rt source-id))]
              (is (= expected cb)
                  (str "created-by == resolve-actor for " (:part-path part) " of " (:event-key d)))
              (when (= :tool-result (:part-type part))
                (is (= "tool" cb)
                    (str "tool_result never human-attributed (SPEC §3.4/§16.1): " (:event-key d)))))))

        (testing "G2 — delegation chain (physical): :production-event on the surface"
          (let [sub-pe (:production-event
                        (ocr/read-source oc-rt (bd/per-part-source-id object-key "a-sub" "content/0")))]
            (is (some? sub-pe) "sidechain surface carries :production-event")
            (is (= "claude-fable-5" (:production/actor sub-pe)) "sub-agent actor = model id")
            (is (true? (:delegation/is-sidechain sub-pe)) "is-sidechain true")
            (is (= "a-1" (:delegation/on-behalf-of sub-pe)) "on-behalf-of = invoking assistant"))
          (let [main-pe (:production-event
                         (ocr/read-source oc-rt (bd/per-part-source-id object-key "a-1" "content/0")))]
            (is (some? main-pe) "main assistant surface carries :production-event")
            (is (false? (:delegation/is-sidechain main-pe)) "main is not a sidechain")
            (is (nil? (:delegation/on-behalf-of main-pe)) "non-sidechain has no on-behalf-of"))))
      (finally (ocr/close-object-container-runtime! oc-rt)))))

;; ===========================================================================
;; P3a — SC1 relation-kind registration micro-gate (CONTRACT §12 / PLAN §5, the
;; ONE authorized relation_kernel.clj edit). The block distiller's mechanical
;; floor (:produced/:grounds) + composition (:assembled-from/:refines) need these
;; kinds registered in rk/relation-kinds. This gate proves the edit landed: each
;; newly-authorized kind asserts :accepted (target-kind :block, asserter-type
;; :machine — the verify-first check that RK does NOT reject :machine the way OC's
;; import path did); an UNREGISTERED kind still rejects :relation/kind-unregistered
;; (the T6 mush guard is intact, not made permissive). RK is MICROBATCH: barrier =
;; wait-for-microbatch-processed-count (submit!==+1 cumulative), never :append-ack.
;; ===========================================================================

(def ^:private rk-topo "relation-kernel-topology")

(deftest p3a-relation-kinds-registered
  (let [rt        (rk/start-relation-runtime! {:tasks (rand-nth [2 4]) :threads 2})
        !appended (atom 0)
        submit!   (fn [req] (rk/append-relation-request! rt req) (swap! !appended inc))
        drain!    (fn [] (rtest/wait-for-microbatch-processed-count
                          (:ipc rt) (:module-name rt) rk-topo @!appended 30000))
        asserter  bd/mechanical-asserter
        mk-edge   (fn [kind]
                    ;; distinct :block endpoints per kind ⇒ disjoint relation-ids,
                    ;; one launch, no cross-talk. ->target-ref supplies :target-key
                    ;; (verbatim unit-id for :block; N7). Deterministic idem key.
                    (let [from (rk/->target-ref :block (str "du:chat:P3A:sense-block-v0:from-" (name kind)))
                          to   (rk/->target-ref :block (str "du:chat:P3A:sense-block-v0:to-" (name kind)))
                          idem (str "p3a-" (name kind))]
                      {:kind kind
                       :rid  (rk/relation-id-for kind from to asserter)
                       :idem idem
                       :req  (rk/assert-request {:kind kind :from from :to to
                                                 :asserter-actor-id asserter
                                                 :asserter-type bd/mechanical-asserter-type
                                                 :asserted-at-ms 1000 :sent-at-ms 1000
                                                 :request-id idem :idempotency-key idem})}))
        new-kinds  [:grounds :assembled-from :refines]
        bogus-kind :sense-block-bogus-unregistered]
    (try
      (let [edges (mapv mk-edge new-kinds)
            bogus (mk-edge bogus-kind)]
        (doseq [e edges] (submit! (:req e)))
        (submit! (:req bogus))
        (drain!)

        (testing "each newly-authorized kind (:grounds :assembled-from :refines) is registered → :accepted"
          (doseq [{:keys [kind rid idem]} edges]
            (let [decision (rk/read-decision-by-idempotency rt rid idem)
                  row      (rk/read-relation-row rt rid)]
              (is (= :accepted (:status decision)) (str kind ": decision :accepted (kind now registered)"))
              (is (some? row) (str kind ": edge row present in $$relations-by-id"))
              (is (= kind (:relation-kind row)) (str kind ": row carries the kind"))
              (is (= :asserted (:relation-status row)) (str kind ": :asserted"))
              (is (= asserter (:asserter-actor-id row)) (str kind ": asserter = sense-block/mechanical@1"))
              (is (= bd/mechanical-asserter-type (:asserter-type row))
                  (str kind ": asserter-type :machine accepted by RK (verify-first — no set rejection)")))))

        (testing "the T6 mush guard is intact — an UNREGISTERED kind still rejects"
          (let [{:keys [rid idem]} bogus
                decision (rk/read-decision-by-idempotency rt rid idem)
                row      (rk/read-relation-row rt rid)]
            (is (= :rejected (:status decision)) "unregistered kind → :rejected")
            (is (= :relation/kind-unregistered (:reason decision)) "reason = :relation/kind-unregistered")
            (is (nil? row) "no edge row written for the rejected request"))))
      (finally (rk/close-relation-runtime! rt)))))

;; ===========================================================================
;; P3b — mechanical edge floor. Pure plan test (no IPC): pair (tool_use,
;; tool_result) by id, write→produced / read→grounds, unpaired→hole. SPEC §11.3.
;; ===========================================================================

(deftest mechanical-edge-plan-pure
  (testing "the floor pairs tool_use/tool_result by id; write→produced, read→grounds; unpaired→hole"
    (let [object-key "chat:PLAN"
          river (filter #(= :river (get-in % [:class :class])) (distilled))
          plan (bd/mechanical-edge-plan object-key river)
          by-tuid (into {} (map (juxt :tool-use-id identity)) plan)]
      (is (= 3 (count plan)) "3 tool edges: Edit, Read, Write")
      (is (= :produced (:kind (by-tuid "tu-1"))) "Edit → produced")
      (is (= :grounds  (:kind (by-tuid "tu-2"))) "Read → grounds")
      (is (= :produced (:kind (by-tuid "tu-3"))) "Write → produced")
      (is (:paired? (by-tuid "tu-1")))
      (is (:paired? (by-tuid "tu-2")))
      (is (false? (:paired? (by-tuid "tu-3"))) "tu-3 (Write) has no tool_result → unpaired")
      (is (= (bd/derived-unit-id object-key (bd/block-path 7 2 0)) (:from-unit-id (by-tuid "tu-1")))
          "produced `from` = a-1's Edit tool_use block (event 7, part 2, block 0)")
      (is (= (bd/derived-unit-id object-key (bd/block-path 8 0 0)) (:to-unit-id (by-tuid "tu-1")))
          "produced `to` = u-tr's tool_result(tu-1) coarse block (event 8, part 0, block 0)")
      (is (= (bd/derived-unit-id object-key (bd/block-path 8 1 0)) (:to-unit-id (by-tuid "tu-2")))
          "grounds `to` = u-tr's tool_result(tu-2) coarse block (event 8, part 1)")
      (is (str/includes? (:to-unit-id (by-tuid "tu-3")) "pending-result:tu-3")
          "unpaired Write → hole endpoint (the awaited-but-absent result, §9)")
      (is (= 3 (count (distinct (map :relation-id plan)))) "distinct relation-ids")
      (is (every? #(= (bd/edge-idempotency-key (:from-unit-id %) (:kind %) (:to-unit-id %))
                      (:idempotency-key %)) plan)
          "each edge carries the relation-scoped sha256(from∥kind∥to) idempotency key (T11)"))))

;; ===========================================================================
;; P3b IPC gates — G8 (produced/grounds to REAL coarse blocks) + G9 (holes).
;; Launches BOTH modules ({:relations? true}, two IPCs — N4). RK is microbatch:
;; barrier = processed-count (cumulative), never :append-ack. Reads: RK V1
;; $$relations-by-id + OC $$derived-units-by-id (validation-only, T7).
;; ===========================================================================

(deftest mechanical-edge-gates
  (let [rt (bd/start-distiller-runtime! {:relations? true})
        {:keys [oc-rt rk-rt]} rt]
    (try
      (ingest-fixture! oc-rt)                       ; the R4/F3 substrate (transcript ingest)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt :rk-rt rk-rt
                                               :source fixture-source
                                               :conversation-id fixture-conversation-id})
            edges (:edges summary)
            by-tuid (into {} (map (juxt :tool-use-id identity)) edges)
            edge-row (fn [tuid] (rk/read-relation-row rk-rt (:relation-id (by-tuid tuid))))]
        ;; barrier: wait until the microbatch has processed all edge appends.
        (rtest/wait-for-microbatch-processed-count
         (:ipc rk-rt) (:module-name rk-rt) rk-topo (:edge-count summary) 30000)

        (testing "the floor asserted exactly 3 tool edges (Edit→produced, Read→grounds, Write→produced)"
          (is (= 3 (:edge-count summary)))
          (is (= #{"tu-1" "tu-2" "tu-3"} (set (keys by-tuid)))))

        (testing "G8 — produced edge (Edit, paired) resolves to a REAL coarse tool_result block"
          (let [e (by-tuid "tu-1") row (edge-row "tu-1")]
            (is (some? row) "produced edge present in $$relations-by-id")
            (is (= :produced (:relation-kind row)))
            (is (= :asserted (:relation-status row)))
            (is (= bd/mechanical-asserter (:asserter-actor-id row))
                "silver by asserter (N6): asserter = sense-block/mechanical@1")
            (is (= bd/mechanical-asserter-type (:asserter-type row)) "asserter-type :machine")
            (is (= :block (:target-kind (:from row))) "from target-kind :block (N7)")
            (is (= :block (:target-kind (:to row)))   "to target-kind :block (N7)")
            (is (some? (read-unit-physical oc-rt (:from-unit-id e))) "from = P1-minted tool_use(Edit) block")
            (let [to-unit (read-unit-physical oc-rt (:to-unit-id e))]
              (is (some? to-unit) "to = the P3b demand-minted coarse tool_result block (N3 option A)")
              (is (= :tool-result-span (:unit-kind to-unit)) "coarse block form = :tool-result-span")
              (is (str/includes? (str (:derived-content-text to-unit)) "File /tmp/foo.clj edited")
                  "coarse block spans the whole tool_result text [0,len)"))))

        (testing "G8 — grounds edge (Read, paired) resolves to a REAL coarse tool_result block"
          (let [e (by-tuid "tu-2") row (edge-row "tu-2")]
            (is (some? row) "grounds edge present")
            (is (= :grounds (:relation-kind row)))
            (is (= :asserted (:relation-status row)))
            (is (= bd/mechanical-asserter (:asserter-actor-id row)))
            (is (some? (read-unit-physical oc-rt (:from-unit-id e))) "from = tool_use(Read) block")
            (let [to-unit (read-unit-physical oc-rt (:to-unit-id e))]
              (is (some? to-unit) "to = demand-minted coarse block")
              (is (= :tool-result-span (:unit-kind to-unit)))
              (is (str/includes? (str (:derived-content-text to-unit)) "defn foo")
                  "coarse block spans the Read result text"))))

        (testing "G9 — holes first-class: the unpaired Write edge persists with an ABSENT :block endpoint"
          (let [e (by-tuid "tu-3") row (edge-row "tu-3")]
            (is (some? row) "the hole edge persists in $$relations-by-id (dangling target legal, rk:288)")
            (is (= :produced (:relation-kind row)))
            (is (= :asserted (:relation-status row)))
            (is (= :block (:target-kind (:to row))) "the hole is still target-kind :block (an absent block)")
            (is (false? (:paired? e)) "tu-3 (Write) had no tool_result")
            (is (some? (read-unit-physical oc-rt (:from-unit-id e))) "from = tool_use(Write) block exists")
            (is (nil? (read-unit-physical oc-rt (:to-unit-id e)))
                "the hole endpoint is provably ABSENT from $$derived-units-by-id (the frontier, §9)")))

        (testing "the paired edges are dual-homed — readable from the from-block's target index"
          (doseq [tuid ["tu-1" "tu-2"]]
            (let [e (by-tuid tuid)
                  tk (:target-key (:from-ref e))
                  from-side (get (rk/read-relations-for-targets rk-rt [tk] nil false) tk)]
              (is (some #(= (:relation-id e) (:relation-id %)) from-side)
                  (str tuid " edge readable from its from-block target index")))))

        (testing "G4-style — a full re-run asserts ZERO new edges + no duplicate coarse block (idempotent)"
          (let [coarse-ids   (keep #(when (:paired? %) (:to-unit-id %)) edges)
                edges-before (into {} (for [e edges] [(:relation-id e) (edge-row (:tool-use-id e))]))
                coarse-before (into {} (for [id coarse-ids] [id (read-unit-physical oc-rt id)]))
                summary2 (bd/distill-conversation! {:oc-rt oc-rt :rk-rt rk-rt
                                                    :source fixture-source
                                                    :conversation-id fixture-conversation-id})]
            (rtest/wait-for-microbatch-processed-count
             (:ipc rk-rt) (:module-name rk-rt) rk-topo
             (+ (:edge-count summary) (:edge-count summary2)) 30000)
            (let [edges-after (into {} (for [e edges] [(:relation-id e) (edge-row (:tool-use-id e))]))
                  coarse-after (into {} (for [id coarse-ids] [id (read-unit-physical oc-rt id)]))]
              (is (= 3 (:edge-count summary2)) "re-run plans the same 3 edges")
              (is (= edges-before edges-after) "every edge row byte-identical after re-run (journal replay, 0 writes)")
              (is (= 2 (count coarse-ids)) "two paired coarse blocks (tu-1, tu-2)")
              (is (= coarse-before coarse-after)
                  "coarse tool_result blocks byte-identical after re-run (OC import dedup on the deterministic key)")))))
      (finally (bd/close-distiller-runtime! rt)))))

;; ===========================================================================
;; P4a — refinement (gate G7). SPEC §5 demand law: mint a FINER block inside an
;; existing coarse block, on the SAME surface, with the engagement as provenance;
;; the coarse block + siblings + surface persist byte-identically (§5.2 adds never
;; invalidates), and its marks persist. Dual runtime; distills WITHOUT edges (OC
;; river blocks only) so the RK carries just this test's appends. RK microbatch
;; barrier = processed-count (cumulative), never :append-ack.
;; ===========================================================================

(defn- block-by-path
  "The pure river block with this block-path (from the golden-checked distillation)."
  [bp]
  (first (filter #(= bp (:block-path %)) (river-blocks))))

(deftest g7-refinement
  (let [rt (bd/start-distiller-runtime! {:relations? true})
        {:keys [oc-rt rk-rt]} rt]
    (try
      (ingest-fixture! oc-rt)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt :source fixture-source
                                               :conversation-id fixture-conversation-id})
            object-key (:object-key summary)
            uid #(bd/derived-unit-id object-key (:block-path %))
            ;; refine the first prose-para of a-1's text part (content/1) — its surface
            ;; ALSO holds a code-fence + a 2nd prose-para, so this proves refine! leaves
            ;; SIBLING units on the same re-declared surface untouched, not only the coarse.
            coarse   (block-by-path "000007:01:000000")     ; :prose-para [0 17] "Here is the plan:"
            sibling  (block-by-path "000007:01:000001")     ; :code-fence [19 58] (same surface)
            coarse-id  (uid coarse)
            sibling-id (uid sibling)
            coarse-before  (read-unit-physical oc-rt coarse-id)
            sibling-before (read-unit-physical oc-rt sibling-id)
            surface-id     (:source-id coarse-before)
            surface-before (ocr/read-source oc-rt surface-id)
            ;; a mark on the coarse block (a :references edge TO it) — §5.2 marks persist
            mark-from (rk/->target-ref :block (str "du:" object-key ":sense-block-v0:marker-m1"))
            mark-to   (rk/->target-ref :block coarse-id)
            mark-idem "g7-mark-references"
            mark-rid  (rk/relation-id-for :references mark-from mark-to bd/mechanical-asserter)
            _ (rk/append-relation-request!
               rk-rt (rk/assert-request {:kind :references :from mark-from :to mark-to
                                         :asserter-actor-id bd/mechanical-asserter
                                         :asserter-type bd/mechanical-asserter-type
                                         :asserted-at-ms 4000 :sent-at-ms 4000
                                         :request-id mark-idem :idempotency-key mark-idem}))
            sub [0 4]                                        ; "Here" — a proper child of [0 17], ASCII-safe
            result (bd/refine! {:oc-rt oc-rt :rk-rt rk-rt
                                :coarse-unit-id coarse-id :sub-span sub
                                :engagement "marker needs a finer target"
                                :asserted-at-ms 5000})
            finer-id (:finer-unit-id result)]
        ;; RK barrier: the mark edge (1) + the :refines edge (1) = 2 cumulative.
        (rtest/wait-for-microbatch-processed-count
         (:ipc rk-rt) (:module-name rk-rt) rk-topo 2 30000)

        (testing "the OC import for the finer block was accepted"
          (is (= :accepted (:status (:decision result)))
              (str "refine! import rejected: " (pr-str (:decision result)))))

        (testing "G7 — the finer unit is present with the sub-span text (§5.1 demand-mint)"
          (let [finer (read-unit-physical oc-rt finer-id)
                fa    (read-anchor-physical oc-rt finer-id)]
            (is (some? finer) "finer unit in $$derived-units-by-id")
            (is (= (:unit-kind coarse-before) (:unit-kind finer)) "finer block inherits the coarse form")
            (is (= surface-id (:source-id finer)) "finer block lives on the SAME surface as the coarse block")
            (is (= "Here" (:derived-content-text finer)) "finer text = the sub-span slice")
            (is (= (subs (:source-raw-text surface-before) 0 4) (:derived-content-text finer))
                "finer derived-content-text == subs(surface, sub-span) (anchor honesty, G6)")
            (is (some? fa) "finer anchor present")
            (is (= [0 4] [(long (:start-offset fa)) (long (:end-offset fa))]) "finer anchor span == sub-span")
            (is (= surface-id (:source-id fa)) "finer anchor targets the same surface")))

        (testing "G7 — the coarse unit + its sibling + the surface are UNCHANGED (§5.2 adds never invalidates)"
          (is (= coarse-before (read-unit-physical oc-rt coarse-id)) "coarse unit byte-identical after refine!")
          (is (= sibling-before (read-unit-physical oc-rt sibling-id))
              "a SIBLING unit on the same re-declared surface is byte-identical (re-declare is idempotent)")
          (is (= surface-before (ocr/read-source oc-rt surface-id))
              "the immutable surface is byte-identical (re-declared, not drifted — the termval-overwrite risk)"))

        (testing "G7 — the :refines edge finer→coarse is in $$relations-by-id, engagement in :note"
          (let [row (rk/read-relation-row rk-rt (:relation-id result))]
            (is (some? row) "refines edge present")
            (is (= :refines (:relation-kind row)))
            (is (= :asserted (:relation-status row)))
            (is (= bd/mechanical-asserter (:asserter-actor-id row)) "silver by asserter (N6)")
            (is (= :block (:target-kind (:from row))))
            (is (= finer-id (:target-id (:from row))) "from = the finer block")
            (is (= coarse-id (:target-id (:to row)))  "to = the coarse block")
            (is (= "marker needs a finer target" (:note row))
                "the engagement is the refinement's provenance (§5.1 MUST), carried in :note")))

        (testing "G7 — the coarse block's mark persists (§5.2 the coarser block persists WITH its marks)"
          (let [mark (rk/read-relation-row rk-rt mark-rid)]
            (is (some? mark) "the :references mark on the coarse block survives refine!")
            (is (= :asserted (:relation-status mark)))
            (is (= coarse-id (:target-id (:to mark))) "still pointing at the coarse block")))

        (testing "G7 — refine! is idempotent: a re-refine of the same sub-span resolves to the same id, writes nothing (§6.1/§14)"
          (let [finer-before (read-unit-physical oc-rt finer-id)
                edge-before  (rk/read-relation-row rk-rt (:relation-id result))
                result2 (bd/refine! {:oc-rt oc-rt :rk-rt rk-rt
                                     :coarse-unit-id coarse-id :sub-span sub
                                     :engagement "marker needs a finer target"
                                     :asserted-at-ms 5000})]
            ;; the duplicate append is still CONSUMED by the microbatch (offset advances) → count 3.
            (rtest/wait-for-microbatch-processed-count
             (:ipc rk-rt) (:module-name rk-rt) rk-topo 3 30000)
            (is (= finer-id (:finer-unit-id result2)) "re-refine resolves to the SAME finer unit-id (§6.1 identity)")
            (is (= finer-before (read-unit-physical oc-rt finer-id)) "finer unit byte-identical after re-refine (§6.1 resolve-reuse — no re-mint)")
            (is (= edge-before (rk/read-relation-row rk-rt (:relation-id result)))
                "the :refines edge is byte-identical after re-refine (RK journal replay, 0 writes)"))))
      (finally (bd/close-distiller-runtime! rt)))))

(deftest f4-refine-resolves-existing-identity
  ;; F4 / SPEC §6.1: refining the WHOLE-message block down to a span already
  ;; occupied by a structural sub-block must RESOLVE to that sub-block's unit —
  ;; never mint a second (surface, span) identity via the refine: path. Fixture
  ;; u-human (order 6): whole-message [0 101] + human-sub [0 34] on one surface.
  (let [rt (bd/start-distiller-runtime! {:relations? true})
        {:keys [oc-rt rk-rt]} rt]
    (try
      (ingest-fixture! oc-rt)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt :source fixture-source
                                               :conversation-id fixture-conversation-id})
            object-key (:object-key summary)
            uid    #(bd/derived-unit-id object-key (:block-path %))
            whole  (block-by-path "000006:00:000000")   ; :human-message [0 101]
            sub    (block-by-path "000006:00:000001")   ; :human-sub     [0 34], same surface
            whole-id (uid whole)
            sub-id   (uid sub)
            sub-before (read-unit-physical oc-rt sub-id)
            surface-id (:source-id sub-before)
            units-before (count (:derived-units
                                 (ocr/read-common-material-for-source
                                  oc-rt surface-id [:derived-units] {} 100000)))
            ;; the id refine! WOULD mint if it ignored the incumbent (the F4 bug)
            would-be-refine-id (bd/derived-unit-id
                                object-key (bd/refine-block-path surface-id 0 34))
            result (bd/refine! {:oc-rt oc-rt :rk-rt rk-rt
                                :coarse-unit-id whole-id :sub-span [0 34]
                                :engagement "engage the first structural sub"
                                :asserted-at-ms 6000})]
        ;; reuse still asserts ONE :refines edge → cumulative processed count 1.
        (rtest/wait-for-microbatch-processed-count
         (:ipc rk-rt) (:module-name rk-rt) rk-topo 1 30000)

        (testing "F4 — the coincident span RESOLVES to the existing sub-block; no duplicate minted"
          (is (true? (:resolved? result)) "refine! reported a resolve, not a mint")
          (is (= sub-id (:finer-unit-id result))
              "finer id = the incumbent free-cut sub-block, NOT a refine: id")
          (is (not= would-be-refine-id sub-id) "sanity: the refine: path id differs from the free-cut id")
          (is (nil? (:decision result)) "no OC import ran on reuse (nothing to mint)")
          (is (nil? (read-unit-physical oc-rt would-be-refine-id))
              "the duplicate refine: unit was NOT minted (SPEC §6.1: one identity per (surface,span))")
          (is (= sub-before (read-unit-physical oc-rt sub-id)) "the incumbent sub-block is byte-identical")
          (is (= units-before
                 (count (:derived-units
                         (ocr/read-common-material-for-source
                          oc-rt surface-id [:derived-units] {} 100000))))
              "no new (surface,span) unit row was added to the surface"))

        (testing "F4 — the :refines edge points from the RESOLVED unit to the coarse block"
          (let [row (rk/read-relation-row rk-rt (:relation-id result))]
            (is (some? row) "refines edge present")
            (is (= :refines (:relation-kind row)))
            (is (= sub-id (:target-id (:from row))) "from = the resolved incumbent, not a phantom id")
            (is (= whole-id (:target-id (:to row))) "to = the coarse whole-message block")
            (is (= "engage the first structural sub" (:note row))
                "the engagement rides :note (§5.1), even on a resolve"))))
      (finally (bd/close-distiller-runtime! rt)))))

;; ===========================================================================
;; P4b — assembly & transclusion (gate G10). SPEC §8: assembly IS a production event
;; — inputs = blocks, output = a NEW `assembled` surface + one :assembled-from edge
;; per source block; the source blocks are RE-ADDRESSED (edges + occurrence position
;; in :note), never copied (their rows stay byte-identical). Same dual-runtime shape.
;; ===========================================================================

(deftest g10-assembly
  (let [rt (bd/start-distiller-runtime! {:relations? true})
        {:keys [oc-rt rk-rt]} rt]
    (try
      (ingest-fixture! oc-rt)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt :source fixture-source
                                               :conversation-id fixture-conversation-id})
            object-key (:object-key summary)
            uid #(bd/derived-unit-id object-key (:block-path %))
            ;; two semantically-distinct river blocks, from different events + surfaces.
            b1-block (block-by-path "000007:00:000000")     ; :thinking  "Let me think about this 😀 problem carefully."
            b2-block (block-by-path "000009:00:000000")     ; :prose-para "Subagent reasoning result."
            b1 (uid b1-block) b2 (uid b2-block)
            b1-before (read-unit-physical oc-rt b1)
            b2-before (read-unit-physical oc-rt b2)
            result (bd/assemble! {:oc-rt oc-rt :rk-rt rk-rt :block-ids [b1 b2] :asserted-at-ms 6000})
            asm-src (:assembled-source-id result)
            asm-uid (:assembled-unit-id result)]
        (rtest/wait-for-microbatch-processed-count
         (:ipc rk-rt) (:module-name rk-rt) rk-topo (:edge-count result) 30000)

        (testing "the OC import for the assembled surface was accepted"
          (is (= :accepted (:status (:decision result)))
              (str "assemble! import rejected: " (pr-str (:decision result)))))

        (testing "G10 — a NEW `assembled` surface + block exist (§8.1 assembly is a production event)"
          (let [surface (ocr/read-source oc-rt asm-src)
                aunit   (read-unit-physical oc-rt asm-uid)
                aanchor (read-anchor-physical oc-rt asm-uid)]
            (is (some? surface) "assembled surface in $$source-artifacts-by-id")
            (is (= bd/assembled-source-format (:source-format surface)) "surface source-format = :assembled")
            (is (some? aunit) "assembled block in $$derived-units-by-id")
            (is (= :assembled (:unit-kind aunit)) "assembled block form = :assembled (SPEC §4.6)")
            (is (= (str (:text b1-block) "\n" (:text b2-block)) (:source-raw-text surface))
                "assembled material = the source texts joined in order (transclusion rendering)")
            (is (= (:source-raw-text surface) (:derived-content-text aunit)) "assembled block spans the whole surface")
            (is (= [0 (count (:source-raw-text surface))]
                   [(long (:start-offset aanchor)) (long (:end-offset aanchor))]) "assembled anchor = [0,len)")))

        (testing "G10 — one :assembled-from edge per source block, position in :note (§8.3 occurrence)"
          (is (= 2 (:edge-count result)))
          (doseq [e (:assembled-from result)]
            (let [row (rk/read-relation-row rk-rt (:relation-id e))]
              (is (some? row) (str "assembled-from edge present for source block " (:source-block-id e)))
              (is (= :assembled-from (:relation-kind row)))
              (is (= :asserted (:relation-status row)))
              (is (= bd/mechanical-asserter (:asserter-actor-id row)) "silver by asserter (N6)")
              (is (= :block (:target-kind (:from row))))
              (is (= asm-uid (:target-id (:from row))) "from = the assembled block")
              (is (= (:source-block-id e) (:target-id (:to row))) "to = the source block")
              (is (= (str "occurrence position " (:position e)) (:note row))
                  "the edge IS the occurrence record — position in :note (§8.3, re-addressed not copied)"))))

        (testing "G10 — the source blocks are RE-ADDRESSED, not copied (§8.2): their rows are UNCHANGED"
          (is (= b1-before (read-unit-physical oc-rt b1)) "source block 1 byte-identical after assembly")
          (is (= b2-before (read-unit-physical oc-rt b2)) "source block 2 byte-identical after assembly"))

        (testing "G10 — assemble! is idempotent: re-assembling the same blocks resolves to the same surface, writes nothing (§6.1/§14)"
          (let [surface-before (ocr/read-source oc-rt asm-src)
                aunit-before   (read-unit-physical oc-rt asm-uid)
                edges-before   (into {} (for [e (:assembled-from result)]
                                          [(:relation-id e) (rk/read-relation-row rk-rt (:relation-id e))]))
                result2 (bd/assemble! {:oc-rt oc-rt :rk-rt rk-rt :block-ids [b1 b2] :asserted-at-ms 6000})]
            ;; the 2 duplicate edge appends are still CONSUMED (offset advances) → count 4.
            (rtest/wait-for-microbatch-processed-count
             (:ipc rk-rt) (:module-name rk-rt) rk-topo 4 30000)
            (is (= asm-src (:assembled-source-id result2)) "re-assemble resolves to the SAME surface id (§6.1)")
            (is (= asm-uid (:assembled-unit-id result2)) "same assembled unit-id")
            (is (= surface-before (ocr/read-source oc-rt asm-src)) "assembled surface byte-identical after re-assemble")
            (is (= aunit-before (read-unit-physical oc-rt asm-uid)) "assembled block byte-identical after re-assemble")
            (is (= edges-before (into {} (for [e (:assembled-from result)]
                                           [(:relation-id e) (rk/read-relation-row rk-rt (:relation-id e))])))
                "every :assembled-from edge byte-identical after re-assemble (RK journal replay, 0 writes)"))))
      (finally (bd/close-distiller-runtime! rt)))))

;; ===========================================================================
;; P5 — river-page (G12/G13) + the guarded REAL 7c80ce2a receipt.
;; ===========================================================================

(defn- expected-river-page
  []
  (vec
   (for [d (distilled)
         :when (= :river (get-in d [:class :class]))
         b (:blocks d)]
     {:event-uuid (:event-key d)
      :actor (:actor b)
      :form (:unit-kind b)
      :text (:text b)
      :part-path (:part-path b)
      :block-path (:block-path b)})))

(deftest river-page-gates
  (let [rt (bd/start-distiller-runtime!)
        oc-rt (:oc-rt rt)]
    (try
      (ingest-fixture! oc-rt)
      (let [summary (bd/distill-conversation! {:oc-rt oc-rt
                                               :source fixture-source
                                               :conversation-id fixture-conversation-id})
            page (bd/river-page {:oc-rt oc-rt :object-key (:object-key summary)}
                                bd/max-river-page-size)
            expected (expected-river-page)
            plan (:river-page/read-plan (meta page))]
        (testing "river blocks render in conversation/part/span order with exact stored material"
          (is (= expected
                 (mapv #(select-keys % [:event-uuid :actor :form :text
                                        :part-path :block-path])
                       page)))
          (is (= (mapv :order page) (vec (sort (map :order page))))
              "the persisted order keys are monotonically ordered")
          (is (every? (comp string? :text) page))
          (is (every? (comp keyword? :form) page)))

        (testing "limit caps the returned blocks without changing their order"
          (let [small (bd/river-page {:oc-rt oc-rt :object-key (:object-key summary)} 3)
                small-plan (:river-page/read-plan (meta small))]
            (is (= (vec (take 3 expected))
                   (mapv #(select-keys % [:event-uuid :actor :form :text
                                          :part-path :block-path])
                         small)))
            (is (<= (:events-read small-plan) 3))
            (is (<= (:surfaces-read small-plan) 3))
            (is (<= (:unit-reads small-plan) 3))
            ;; F2 — a capped page is FLAGGED, never silently short.
            (is (= 5 (:river-events-total small-plan))
                "the plan reports the TRUE river total (5), not the page size")
            (is (<= (:river-events-rendered small-plan) 3))
            (is (= (count small) (:blocks-returned small-plan)))
            (is (true? (:truncated? small-plan))
                "a capped page is truncated — the consumer must re-page (F2)")
            (is (false? (:page-complete? small-plan)))))

        (testing "G12 — measured composition plan stays inside the hard page seek bound"
          (is (= 1 (:projection-range-seeks plan)))
          (is (= (+ 1
                    (:input-source-point-seeks plan)
                    (:common-material-range-seeks plan)
                    (:unit-point-seeks plan))
                 (:seek-count plan)))
          (is (<= (:events-read plan) bd/max-river-page-size))
          (is (<= (:surfaces-read plan) bd/max-river-page-size))
          (is (<= (:unit-reads plan) bd/max-river-page-size))
          (is (<= (:seek-count plan) (:seek-bound plan)))
          ;; F2 — the seek plan is HONEST: on a single-distiller page every read
          ;; ref renders a block (foreign strata are filtered BEFORE read-unit),
          ;; so unit-reads never exceeds the blocks actually returned.
          (is (= (:unit-reads plan) (:blocks-returned plan))
              "no wasted/foreign point-reads inflate the plan")
          ;; F2 — the full page consumes all 5 river events → complete, not truncated.
          (is (= 5 (:river-events-total plan)))
          (is (= 5 (:river-events-rendered plan)))
          (is (false? (:truncated? plan)) "the full page is complete (no ceiling hit)")
          (is (true? (:page-complete? plan))))

        (testing "G13 — rendered ids/forms/text are the persisted query results"
          (is (every? #(str/starts-with? (:unit-id %) "du:") page))
          (is (every? #(str/starts-with? (:source-id %) "src:tr:") page))
          (is (= (mapv :text expected) (mapv :text page)))))
      (finally (bd/close-distiller-runtime! rt)))))

(defn real-example-river-page-receipt!
  "P5 definition-of-done receipt. Ingest the REAL 7c80ce2a file through the
   transcript→Object Container product path, then start RK and distill through
   both runtimes. The stagger keeps transcript operational mirrors on the OC IPC.
   Pretty-prints page 1 plus the measured G12 read plan; returns :absent off-box."
  []
  (if-let [file (find-real-transcript)]
    (let [runtime (atom (bd/start-distiller-runtime!))]
      (try
        (let [oc-rt (:oc-rt @runtime)
              request (tr/transcript-request
                       :transcript/harvest
                       {:transcript/request-id "block-distiller-p5-real-7c80ce2a"
                        :transcript/source :claude-code
                        :transcript/paths [(.getPath ^java.io.File file)]
                        :time-ms 0})
              observations (vec (tr/read-jsonl-observations request file 0))
              conversation-ids (vec (distinct (map :transcript/conversation-id observations)))
              conversation-id (some #(when (str/starts-with? (str %) "7c80ce2a") %)
                                    conversation-ids)
              _ (when-not conversation-id
                  (throw (ex-info "real receipt did not find the 7c80ce2a conversation"
                                  {:file (.getPath ^java.io.File file)
                                   :conversation-ids conversation-ids})))
              harvest (tr/harvest-transcripts-into-object-container! oc-rt request)
              _ (when-not (= :complete (:status harvest))
                  (throw (ex-info "real receipt transcript ingest failed" {:harvest harvest})))
              rk-rt (rk/start-relation-runtime!)
              _ (swap! runtime assoc :rk-rt rk-rt)
              summary (bd/distill-conversation! {:oc-rt oc-rt
                                                 :rk-rt rk-rt
                                                 :source :claude-code
                                                 :conversation-id conversation-id})
              page (bd/river-page {:oc-rt oc-rt :object-key (:object-key summary)} 32)]
          (pprint/pprint {:file (.getPath ^java.io.File file)
                          :conversation-id conversation-id
                          :harvest harvest
                          :distillation (select-keys summary [:object-key :river :debris
                                                              :edge-count])
                          :read-plan (:river-page/read-plan (meta page))})
          (pprint/pprint page)
          {:harvest harvest :distillation summary :page page})
        (finally (bd/close-distiller-runtime! @runtime))))
    :absent))

(comment
  ;; P5 real-chat receipt (guarded by find-real-transcript; :absent off-box):
  (real-example-river-page-receipt!))


(comment
  ;; Regenerate the golden from THIS namespace's own projection (single source
  ;; of truth), then eyeball before committing:
  (require '[clojure.pprint :as pp])
  (spit golden-path (with-out-str (pp/pprint (golden-projection (distilled))))))
