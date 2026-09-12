(ns app.shared.studio
  "Studio P1's closed, data-only material vocabulary.

   This namespace is shared because the client compiles drawing gestures into
   the anatomy seven-op grammar while the canonical server owner validates the
   exact same gate marker and report shapes. It owns no I/O and no runtime
   state."
  (:require [clojure.string :as str]
            [app.shared.anatomy-material :as anatomy]))

;; ---------------------------------------------------------------------------
;; Draft anatomy — a normal block plus material rectangle/status parts
;; ---------------------------------------------------------------------------

(def frame-part-id :studio-frame)
(def status-part-id :studio-status)

(def frame-part
  {:part/id frame-part-id
   :part/prim :block-attention-box
   :part/when :always
   :part/order 25
   :part/props
   {:w [:view :block-w]
    :h [:view :block-h]
    :pad [:wear :attention :attention/hit-padding]
    :border-width 2.0
    :border-color [0.56 0.62 0.72 0.95]
    :bg [0.18 0.22 0.30 0.12]}})

(def status-part
  {:part/id status-part-id
   :part/prim :block-notice
   :part/when :always
   :part/order 125
   :part/props
   {:notice "Draft"
    :refusal nil
    :line-count [:view :line-count]
    :w [:view :block-w]
    :line-h [:view :line-h]
    :font-size [:view :font-size]}})

(defn- part-ids [form]
  (into #{} (keep :part/id) (:anatomy/parts form)))

(declare canonical-source)

(defn compose-edits
  "Apply a gesture's ordered seven-op script, validating the whole candidate
   after every op. The receipt is deliberately data-only and preserves the
   one-to-one gesture -> grammar-op audit trail (CONTRACT T5/TRAP-6)."
  [form edits]
  (loop [candidate form
         remaining (vec edits)
         receipts []]
    (if-let [edit (first remaining)]
      (let [result (anatomy/edit-candidate candidate edit)]
        (if (= :candidate (:status result))
          (recur (:form result)
                 (subvec remaining 1)
                 (conj receipts {:edit edit :status :candidate}))
          {:status :refused
           :form nil
           :source nil
           :op-receipts (conj receipts
                              {:edit edit
                               :status :refused
                               :errors (:errors result)})
           :errors (:errors result)}))
      {:status :candidate
       :form candidate
       :source (canonical-source candidate)
       :op-receipts receipts
       :errors []})))

(defn draft-candidate
  "Turn any valid block anatomy form into a real Studio draft using only
   existing `:add` operations. Re-entry is idempotent."
  ([form] (draft-candidate form {}))
  ([form frame-props]
   (let [ids (part-ids form)
        frame (update frame-part :part/props merge frame-props)
        edits (cond-> []
                (not (contains? ids frame-part-id))
                (conj {:edit/op :add :part frame})
                (not (contains? ids status-part-id))
                (conj {:edit/op :add :part status-part}))]
     (compose-edits form edits))))

(defn promotable-candidate
  "Remove only the visible provisional status through the seven-op grammar.
   The rectangle and every user retune remain component material."
  [form]
  (if (contains? (part-ids form) status-part-id)
    (compose-edits form [{:edit/op :remove :part/id status-part-id}])
    (compose-edits form [])))

(defn retune-frame-candidate
  "One direct-at-the-pixels visible retune. `props` is merged by the existing
   `:retune-props` operation; no Studio-only writer exists."
  [form props]
  (compose-edits form [{:edit/op :retune-props
                        :part/id frame-part-id
                        :part/props props}]))

(defn draft-form?
  [form]
  (contains? (part-ids form) status-part-id))

;; ---------------------------------------------------------------------------
;; Test-gated accept — exact durable marker and report forms
;; ---------------------------------------------------------------------------

(def gate-version 0)
(def report-version 0)

(def instance-shapes
  #{:empty :wrapped-multiline :marks-or-machine})

(def check-ids
  #{:candidate/build :render/invariants :interaction/invariants})

(def flow-ids
  #{:focus :type :bound-gesture :render :interaction})

(def result-values #{:pass :fail})

(def marker-keys
  #{:studio-gate/version
    :studio-gate/master-id
    :studio-gate/candidate-revision-id
    :studio-gate/candidate-content-sha256
    :studio-gate/expected-instances
    :studio-gate/report})

(def report-keys
  #{:studio-report/version
    :studio-report/id
    :studio-report/master-id
    :studio-report/candidate-revision-id
    :studio-report/candidate-content-sha256
    :studio-report/tested-instances
    :studio-report/checks
    :studio-report/flows
    :studio-report/result
    :studio-report/actor
    :studio-report/time-ms
    :studio-report/request-id})

(def instance-keys #{:instance/id :instance/shape})
(def check-keys #{:check/id :check/result :check/receipt})
(def flow-keys #{:flow/id :flow/result :flow/receipt})
(def actor-keys #{:actor/id :actor/type})

(defn canonical-source
  "Stable bytes for content-hash identity across REPL and plain-program
   printer settings."
  [value]
  (binding [*print-namespace-maps* false]
    (pr-str value)))

(defn data-only?
  "Closed recursive EDN values for durable receipts. Lists, symbols, tagged
   values, records, and executable values are deliberately excluded."
  [value]
  (cond
    (or (nil? value)
        (boolean? value)
        (string? value)
        (keyword? value)
        (number? value)) true
    (vector? value) (every? data-only? value)
    (set? value) (every? data-only? value)
    (and (map? value) (not (record? value)))
    (and (every? data-only? (keys value))
         (every? data-only? (vals value)))
    :else false))

(defn instance-row?
  [row]
  (and (map? row)
       (= instance-keys (set (keys row)))
       (string? (:instance/id row))
       (not (str/blank? (:instance/id row)))
       (contains? instance-shapes (:instance/shape row))))

(defn instance-set?
  [rows]
  (and (vector? rows)
       (= 3 (count rows))
       (every? instance-row? rows)
       (= 3 (count (set (map :instance/id rows))))
       (= instance-shapes (set (map :instance/shape rows)))))

(defn marker-form
  [{:keys [master-id candidate-revision-id candidate-content-sha256
           expected-instances report]}]
  {:studio-gate/version gate-version
   :studio-gate/master-id (str master-id)
   :studio-gate/candidate-revision-id (str candidate-revision-id)
   :studio-gate/candidate-content-sha256 (str candidate-content-sha256)
   :studio-gate/expected-instances (vec expected-instances)
   :studio-gate/report report})

(defn marker-errors
  [marker]
  (cond-> []
    (not (map? marker))
    (conj {:type :studio-gate/not-a-map})

    (and (map? marker) (not= marker-keys (set (keys marker))))
    (conj {:type :studio-gate/keys-invalid})

    (and (map? marker) (not= gate-version (:studio-gate/version marker)))
    (conj {:type :studio-gate/version-invalid})

    (and (map? marker)
         (or (str/blank? (str (:studio-gate/master-id marker)))
             (str/blank? (str (:studio-gate/candidate-revision-id marker)))
             (str/blank? (str (:studio-gate/candidate-content-sha256 marker)))))
    (conj {:type :studio-gate/identity-invalid})

    (and (map? marker)
         (not (instance-set? (:studio-gate/expected-instances marker))))
    (conj {:type :studio-gate/expected-instances-invalid})

    (and (map? marker)
         (some? (:studio-gate/report marker))
         (not (map? (:studio-gate/report marker))))
    (conj {:type :studio-gate/report-invalid})))

(defn valid-marker? [marker]
  (empty? (marker-errors marker)))

(defn report-form
  [{:keys [id master-id candidate-revision-id candidate-content-sha256
           tested-instances checks flows result actor time-ms request-id]}]
  {:studio-report/version report-version
   :studio-report/id (str id)
   :studio-report/master-id (str master-id)
   :studio-report/candidate-revision-id (str candidate-revision-id)
   :studio-report/candidate-content-sha256 (str candidate-content-sha256)
   :studio-report/tested-instances (vec tested-instances)
   :studio-report/checks (vec checks)
   :studio-report/flows (vec flows)
   :studio-report/result result
   :studio-report/actor {:actor/id (str (:actor/id actor))
                         :actor/type (:actor/type actor)}
   :studio-report/time-ms time-ms
   :studio-report/request-id (str request-id)})

(defn- result-row?
  [row keys* id-key ids]
  (and (map? row)
       (= keys* (set (keys row)))
       (contains? ids (get row id-key))
       (contains? result-values
                  (get row (if (= id-key :check/id)
                             :check/result
                             :flow/result)))
       (data-only? (get row (if (= id-key :check/id)
                              :check/receipt
                              :flow/receipt)))))

(defn- exact-result-rows?
  [rows keys* id-key ids]
  (and (vector? rows)
       (= (count ids) (count rows))
       (= ids (set (map id-key rows)))
       (every? #(result-row? % keys* id-key ids) rows)))

(defn report-errors
  "Validate a report against its durable marker. Passing requires every
   individual check and real-flow receipt to pass; a terminal `:pass` cannot
   mask a failed row."
  [report marker]
  (let [checks (:studio-report/checks report)
        flows (:studio-report/flows report)
        result (:studio-report/result report)]
    (cond-> []
      (not (map? report))
      (conj {:type :studio-report/not-a-map})

      (and (map? report) (not= report-keys (set (keys report))))
      (conj {:type :studio-report/keys-invalid})

      (and (map? report) (not= report-version (:studio-report/version report)))
      (conj {:type :studio-report/version-invalid})

      (and (map? report)
           (some #(str/blank? (str %))
                 [(:studio-report/id report)
                  (:studio-report/master-id report)
                  (:studio-report/candidate-revision-id report)
                  (:studio-report/candidate-content-sha256 report)
                  (:studio-report/request-id report)]))
      (conj {:type :studio-report/identity-invalid})

      (and (map? report)
           (not (instance-set? (:studio-report/tested-instances report))))
      (conj {:type :studio-report/tested-instances-invalid})

      (and (map? report)
           (not (exact-result-rows? checks check-keys :check/id check-ids)))
      (conj {:type :studio-report/checks-invalid})

      (and (map? report)
           (not (exact-result-rows? flows flow-keys :flow/id flow-ids)))
      (conj {:type :studio-report/flows-invalid})

      (and (map? report) (not (contains? result-values result)))
      (conj {:type :studio-report/result-invalid})

      (and (map? report)
           (= :pass result)
           (or (not-every? #(= :pass (:check/result %)) checks)
               (not-every? #(= :pass (:flow/result %)) flows)))
      (conj {:type :studio-report/pass-rows-not-all-pass})

      (and (map? report)
           (not (and (map? (:studio-report/actor report))
                     (= actor-keys
                        (set (keys (:studio-report/actor report))))
                     (not (str/blank?
                           (str (get-in report [:studio-report/actor
                                                :actor/id]))))
                     (keyword? (get-in report [:studio-report/actor
                                               :actor/type])))))
      (conj {:type :studio-report/actor-invalid})

      (and (map? report)
           (not (and (integer? (:studio-report/time-ms report))
                     (not (neg? (:studio-report/time-ms report))))))
      (conj {:type :studio-report/time-invalid})

      (and (map? report) (map? marker)
           (not= (:studio-gate/master-id marker)
                 (:studio-report/master-id report)))
      (conj {:type :studio-report/master-mismatch})

      (and (map? report) (map? marker)
           (not= (:studio-gate/candidate-revision-id marker)
                 (:studio-report/candidate-revision-id report)))
      (conj {:type :studio-report/candidate-mismatch})

      (and (map? report) (map? marker)
           (not= (:studio-gate/candidate-content-sha256 marker)
                 (:studio-report/candidate-content-sha256 report)))
      (conj {:type :studio-report/content-mismatch})

      (and (map? report) (map? marker)
           (not= (:studio-gate/expected-instances marker)
                 (:studio-report/tested-instances report)))
      (conj {:type :studio-report/instance-set-mismatch}))))

(defn valid-report?
  [report marker]
  (empty? (report-errors report marker)))
