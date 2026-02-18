(ns app.server.review-pack
  "Review Pack v0 domain logic.

   Purpose:
   - bounded review artifact for faster reviewer confidence
   - strict evidence requirements for core claims
   - commit-pinned anchors + publish snapshot hash for integrity"
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]))

(def ^:private section-keys
  [:intent :scope :change-summary :decisions :evidence :risks-unknowns])

(def ^:private section-budgets
  {:intent 1400
   :scope 2200
   :change-summary 5000
   :decisions 50
   :evidence 200
   :risks-unknowns 60})

(def ^:private decision-statuses
  #{:accepted :rejected :parked})

(def ^:private anchor-kinds
  #{:code :test :command :context :link})

(defonce !review-packs
  (atom {}))

(defn- now-ms []
  (System/currentTimeMillis))

(defn- fresh-id []
  (str (java.util.UUID/randomUUID)))

(defn- non-empty-string?
  [x]
  (and (string? x) (not (str/blank? x))))

(defn- trim-str
  [x]
  (some-> x str str/trim))

(defn- keep-non-empty
  [x]
  (let [s (trim-str x)]
    (when (non-empty-string? s) s)))

(defn- normalize-span
  [span]
  (let [line-start (long (max 1 (or (:line-start span) 1)))
        line-end (long (max line-start (or (:line-end span) line-start)))
        col-start (long (max 1 (or (:col-start span) 1)))
        col-end (long (max col-start (or (:col-end span) col-start)))]
    {:line-start line-start
     :line-end line-end
     :col-start col-start
     :col-end col-end}))

(defn- normalize-changed-files
  [changed-files]
  (->> changed-files
       (keep (fn [f]
               (let [path (keep-non-empty (:path f))
                     rationale (keep-non-empty (:rationale f))
                     risk-tag (let [r (:risk-tag f)]
                                (cond
                                  (keyword? r) r
                                  (string? r) (keyword (str/lower-case (str/trim r)))
                                  :else :normal))]
                 (when path
                   {:path path
                    :rationale rationale
                    :risk-tag risk-tag}))))
       vec))

(defn- normalize-decisions
  [decisions]
  (->> decisions
       (keep (fn [d]
               (let [status (cond
                              (keyword? (:status d)) (:status d)
                              (string? (:status d)) (keyword (str/lower-case (str/trim (:status d))))
                              :else :parked)
                     rationale (keep-non-empty (:rationale d))
                     summary (keep-non-empty (:summary d))]
                 (when (or rationale summary)
                   {:id (or (keep-non-empty (:id d)) (fresh-id))
                    :status (if (contains? decision-statuses status) status :parked)
                    :summary (or summary rationale)
                    :rationale (or rationale summary "")
                    :timestamp (long (or (:timestamp d) (now-ms)))}))))
       vec))

(defn- normalize-evidence
  [anchors]
  (->> anchors
       (keep (fn [a]
               (let [kind (cond
                            (keyword? (:kind a)) (:kind a)
                            (string? (:kind a)) (keyword (str/lower-case (str/trim (:kind a))))
                            :else :code)
                     file-path (keep-non-empty (:file-path a))
                     commit (keep-non-empty (:commit a))
                     snippet (or (keep-non-empty (:snippet a)) "")
                     claim-id (keep-non-empty (:claim-id a))
                     confidence (double (max 0.0 (min 1.0 (or (:confidence a) 0.5))))]
                 (when (or file-path (= kind :link))
                   {:id (or (keep-non-empty (:id a)) (fresh-id))
                    :kind (if (contains? anchor-kinds kind) kind :code)
                    :claim-id claim-id
                    :file-path file-path
                    :commit commit
                    :span (normalize-span (or (:span a) {}))
                    :snippet snippet
                    :confidence confidence}))))
       vec))

(defn- normalize-risks-unknowns
  [items]
  (->> items
       (keep (fn [r]
               (let [text (keep-non-empty (:text r))
                     kind (cond
                            (keyword? (:kind r)) (:kind r)
                            (string? (:kind r)) (keyword (str/lower-case (str/trim (:kind r))))
                            :else :risk)
                     severity (cond
                                (keyword? (:severity r)) (:severity r)
                                (string? (:severity r)) (keyword (str/lower-case (str/trim (:severity r))))
                                :else :medium)]
                 (when text
                   {:id (or (keep-non-empty (:id r)) (fresh-id))
                    :kind (if (#{:risk :unknown} kind) kind :risk)
                    :severity (if (#{:low :medium :high} severity) severity :medium)
                    :text text}))))
       vec))

(defn- normalize-claims
  [claims]
  (->> claims
       (keep (fn [c]
               (let [text (keep-non-empty (:text c))
                     evidence-ids (->> (:evidence-ids c)
                                       (map keep-non-empty)
                                       (filter some?)
                                       vec)
                     core? (true? (:core? c))]
                 (when text
                   {:id (or (keep-non-empty (:id c)) (fresh-id))
                    :text text
                    :core? core?
                    :evidence-ids evidence-ids}))))
       vec))

(defn- normalize-sections
  [sections]
  {:intent (or (keep-non-empty (:intent sections)) "")
   :scope (or (keep-non-empty (:scope sections)) "")
   :change-summary (or (keep-non-empty (:change-summary sections)) "")
   :decisions (normalize-decisions (:decisions sections))
   :evidence (normalize-evidence (:evidence sections))
   :risks-unknowns (normalize-risks-unknowns (:risks-unknowns sections))})

(defn- count-or-size
  [v]
  (if (string? v)
    (count v)
    (count (or v []))))

(defn- validate-section-budgets
  [sections]
  (->> section-budgets
       (keep (fn [[k max-size]]
               (let [actual (count-or-size (get sections k))]
                 (when (> actual max-size)
                   {:type :budget-exceeded
                    :section k
                    :max max-size
                    :actual actual}))))
       vec))

(defn- validate-required-sections
  [sections]
  (let [missing (->> section-keys
                     (keep (fn [k]
                             (let [v (get sections k)]
                               (when (or (nil? v)
                                         (and (string? v) (str/blank? v)))
                                 k))))
                     vec)]
    (if (seq missing)
      [{:type :missing-sections :sections missing}]
      [])))

(defn- validate-changed-files
  [changed-files]
  (->> changed-files
       (keep (fn [f]
               (cond
                 (not (non-empty-string? (:path f)))
                 {:type :invalid-changed-file :message "missing file path"}

                 (not (non-empty-string? (:rationale f)))
                 {:type :invalid-changed-file
                  :file (:path f)
                  :message "missing rationale"}

                 :else nil)))
       vec))

(defn- validate-evidence-anchors
  [anchors]
  (->> anchors
       (keep (fn [a]
               (cond
                 (and (not= (:kind a) :link) (not (non-empty-string? (:file-path a))))
                 {:type :invalid-evidence
                  :anchor-id (:id a)
                  :message "missing file-path"}

                 (not (non-empty-string? (:commit a)))
                 {:type :invalid-evidence
                  :anchor-id (:id a)
                  :message "missing commit (anchors must be commit-pinned)"}

                 :else nil)))
       vec))

(defn- validate-core-claims
  [claims evidence]
  (let [evidence-ids (set (map :id evidence))]
    (->> claims
         (keep (fn [claim]
                 (when (:core? claim)
                   (let [ids (vec (:evidence-ids claim))
                         missing (vec (remove evidence-ids ids))]
                     (cond
                       (empty? ids)
                       {:type :core-claim-missing-evidence
                        :claim-id (:id claim)
                        :message "core claim has no evidence anchors"}

                       (seq missing)
                       {:type :core-claim-invalid-evidence
                        :claim-id (:id claim)
                        :missing-anchor-ids missing}

                       :else nil)))))
         vec)))

(defn- validate-pack
  [pack]
  (let [sections (:sections pack)
        claims (:claims pack)
        evidence (get-in pack [:sections :evidence])]
    (vec (concat
           (validate-required-sections sections)
           (validate-section-budgets sections)
           (validate-changed-files (:changed-files pack))
           (validate-evidence-anchors evidence)
           (validate-core-claims claims evidence)))))

(defn- canonicalize
  "Recursively convert map keys to sorted maps for deterministic hashing."
  [x]
  (walk/postwalk
    (fn [v]
      (if (map? v)
        (into (sorted-map) v)
        v))
    x))

(defn- sha256-hex
  [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str s) "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn- snapshot-hash
  [pack]
  (-> pack
      (dissoc :feedback :updated-at :published-at)
      canonicalize
      pr-str
      sha256-hex))

(defn create-review-pack!
  "Create Review Pack v0 draft.
   Returns {:ok true :pack ...} or {:ok false :errors [...]}."
  [request]
  (let [pack-id (or (keep-non-empty (:pack-id request)) (fresh-id))
        now (now-ms)
        sections (normalize-sections (or (:sections request) {}))
        claims (normalize-claims (:claims request))
        changed-files (normalize-changed-files (:changed-files request))
        pack {:pack-id pack-id
              :issue-ref (keep-non-empty (:issue-ref request))
              :pr-ref (keep-non-empty (:pr-ref request))
              :branch (keep-non-empty (:branch request))
              :author (or (keep-non-empty (:author request)) "unknown")
              :status :draft
              :version 1
              :created-at now
              :updated-at now
              :published-at nil
              :snapshot-sha nil
              :constraints {:evidence-bar :strict-core-claims
                            :section-budgets section-budgets}
              :changed-files changed-files
              :claims claims
              :sections sections
              :feedback []}
        errors (validate-pack pack)]
    (if (seq errors)
      {:ok false :errors errors}
      (do
        (swap! !review-packs assoc pack-id pack)
        {:ok true
         :pack-id pack-id
         :pack pack}))))

(defn get-review-pack
  [pack-id]
  (get @!review-packs pack-id))

(defn list-review-packs
  []
  (->> @!review-packs
       vals
       (sort-by (juxt :updated-at :created-at) #(compare %2 %1))
       vec))

(defn publish-review-pack!
  [pack-id request]
  (if-let [pack (get-review-pack pack-id)]
    (if (= :published (:status pack))
      {:ok false
       :error :already-published
       :message "Review pack already published"}
      (let [merged (-> pack
                       (assoc :pr-ref (or (keep-non-empty (:pr-ref request)) (:pr-ref pack)))
                       (assoc :published-by (or (keep-non-empty (:published-by request))
                                                (keep-non-empty (:author request))
                                                "unknown"))
                       (assoc :updated-at (now-ms)))
            errors (validate-pack merged)]
        (if (seq errors)
          {:ok false :errors errors}
          (let [published-at (now-ms)
                snapshot (snapshot-hash merged)
                final-pack (assoc merged
                                  :status :published
                                  :published-at published-at
                                  :updated-at published-at
                                  :snapshot-sha snapshot)]
            (swap! !review-packs assoc pack-id final-pack)
            {:ok true
             :pack-id pack-id
             :snapshot-sha snapshot
             :pack final-pack}))))
    {:ok false
     :error :not-found
     :message "Review pack not found"}))

(defn add-feedback!
  [pack-id request]
  (if-let [pack (get-review-pack pack-id)]
    (let [comment (keep-non-empty (:comment request))]
      (if-not comment
        {:ok false
         :error :invalid-feedback
         :message "Feedback comment is required"}
        (let [feedback {:id (fresh-id)
                        :section (or (keep-non-empty (:section request)) "general")
                        :by (or (keep-non-empty (:by request)) "reviewer")
                        :role (or (keep-non-empty (:role request)) "reviewer")
                        :comment comment
                        :created-at (now-ms)}
              updated (-> pack
                          (update :feedback (fnil conj []) feedback)
                          (assoc :updated-at (now-ms)))]
          (swap! !review-packs assoc pack-id updated)
          {:ok true
           :feedback feedback
           :feedback-count (count (:feedback updated))
           :pack-id pack-id})))
    {:ok false
     :error :not-found
     :message "Review pack not found"}))

(defn- decision-counts
  [decisions]
  (reduce (fn [m d] (update m (:status d) (fnil inc 0)))
          {:accepted 0 :rejected 0 :parked 0}
          decisions))

(defn- risk-counts
  [items]
  (reduce (fn [m r] (update m (:kind r) (fnil inc 0)))
          {:risk 0 :unknown 0}
          items))

(defn review-pack-summary
  [pack-id {:keys [base-url]}]
  (if-let [pack (get-review-pack pack-id)]
    (let [sections (:sections pack)
          decisions (:decisions sections)
          evidence (:evidence sections)
          risks (:risks-unknowns sections)
          claims (:claims pack)
          core-claims (count (filter :core? claims))
          changed-file-count (count (:changed-files pack))
          dc (decision-counts decisions)
          rc (risk-counts risks)
          link-path (str "/review-pack/" (:pack-id pack))
          pack-url (if (non-empty-string? base-url)
                     (str (str/replace base-url #"/$" "") link-path)
                     link-path)
          compact-intent (let [s (or (:intent sections) "")]
                           (if (> (count s) 280) (str (subs s 0 280) "...") s))
          compact-change (let [s (or (:change-summary sections) "")]
                           (if (> (count s) 500) (str (subs s 0 500) "...") s))
          summary {:pack-id (:pack-id pack)
                   :status (:status pack)
                   :version (:version pack)
                   :issue-ref (:issue-ref pack)
                   :pr-ref (:pr-ref pack)
                   :snapshot-sha (:snapshot-sha pack)
                   :intent compact-intent
                   :change-summary compact-change
                   :changed-file-count changed-file-count
                   :core-claim-count core-claims
                   :evidence-anchor-count (count evidence)
                   :decision-counts dc
                   :risk-counts rc
                   :updated-at (:updated-at pack)
                   :pack-url pack-url}
          pr-template (str
                        "### Review Pack\n"
                        "- Pack ID: `" (:pack-id pack) "`\n"
                        (when (:snapshot-sha pack)
                          (str "- Snapshot: `" (:snapshot-sha pack) "`\n"))
                        "- Intent: " compact-intent "\n"
                        "- Changed files: " changed-file-count "\n"
                        "- Decisions: accepted " (:accepted dc) ", rejected " (:rejected dc) ", parked " (:parked dc) "\n"
                        "- Evidence anchors: " (count evidence) " (core claims: " core-claims ")\n"
                        "- Risks: " (:risk rc) ", Unknowns: " (:unknown rc) "\n"
                        "- Full artifact: " pack-url "\n")]
      {:ok true
       :summary summary
       :pr-comment-template pr-template})
    {:ok false
     :error :not-found
     :message "Review pack not found"}))
