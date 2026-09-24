(ns formal.scenarios
  "Fixed histories for the cases Sid named, each played under a config:
  which values a reader can still open at the end, what the reads showed
  of a promotion, and whether the properties object."
  (:require [clojure.string :as str]
            [formal.model :as m]
            [formal.properties :as props]))

(defn- fact [e k & [persons marked?]]
  {:e e :k k :mention persons :replaces :none
   :mark (if marked? #{:die-with-any} #{}) :other-layer nil})

(defn- offer [who layer & facts]
  [:offer {:who who :layer layer :facts (vec facts) :stood-on nil :times 1}])

(defn- offer-citing [where who layer & facts]
  [:offer {:who who :layer layer :facts (vec facts) :stood-on nil :times 1 :cite where}])

(def ^:private settle
  "Both gates finish what is queued."
  [[:work 0] [:work 1] [:work 2] [:batch]])

(defn- h [& parts] (vec (apply concat parts)))

(def a-cases
  "Change A's checks as Sid named them, and last the case he ruled on 25
  September. Each is [what is checked, history, expected],
  where expected maps [layer key persons] to :open or :erased at the end."
  [["A: in Alice's own layer a note and a mention of Bob both die with Alice"
    (h [(offer :alice :alice (fact :e0 :note) (fact :e1 :mention #{:bob}))] settle
       [[:forget-person :alice]])
    {:values {[:alice :note nil] :erased [:alice :mention #{:bob}] :erased}}]
   ["A: in the group a mention of Bob alone dies with Bob"
    (h [(offer :bob :group (fact :e0 :mention #{:bob}))] settle [[:forget-person :bob]])
    {:values {[:group :mention #{:bob}] :erased}}]
   ["A: in the group a plain note about no one survives both forgets"
    (h [(offer :bob :group (fact :e0 :note))] settle [[:forget-person :bob] [:forget-person :alice]])
    {:values {[:group :note nil] :open}}]
   ["A: in the group a value about Alice and Bob survives Alice alone"
    (h [(offer :bob :group (fact :e0 :mention #{:alice :bob}))] settle [[:forget-person :alice]])
    {:values {[:group :mention #{:alice :bob}] :open}}]
   ["A: in the group a value about Alice and Bob survives Bob alone"
    (h [(offer :bob :group (fact :e0 :mention #{:alice :bob}))] settle [[:forget-person :bob]])
    {:values {[:group :mention #{:alice :bob}] :open}}]
   ["A: ... and dies when both are forgotten ('either one alone')"
    (h [(offer :bob :group (fact :e0 :mention #{:alice :bob}))] settle
       [[:forget-person :bob] [:forget-person :alice]])
    {:values {[:group :mention #{:alice :bob}] :erased}}]
   ["A: in the group a value about Alice and Bob, marked, dies with Bob"
    (h [(offer :bob :group (fact :e0 :mention #{:alice :bob} true))] settle [[:forget-person :bob]])
    {:values {[:group :mention #{:alice :bob}] :erased}}]
   ["A, ruled 25 September: in Alice's own layer a mention of Bob survives Bob (7b)"
    (h [(offer :alice :alice (fact :e1 :mention #{:bob}))] settle [[:forget-person :bob]])
    {:values {[:alice :mention #{:bob}] :open}}]])

(def b-cases
  "Change B: the two pending states either side of the read-out, and where
  a forget falls against it. :shown lists what each read showed of the one
  promotion, the run's closing read last."
  [["B: a value forget queued before the read-out: pending, then refused"
    (h [(offer :alice :alice (fact :e0 :note))] settle
       [[:promote 0 :group 1] [:forget-value 0] [:step 0] [:read :now] [:step 0] [:read :now]
        [:step 0]])
    {:values {[:alice :note nil] :erased} :shown [:pending :pending :refused]}]
   ["B: Alice forgotten before the read-out: pending, then refused"
    (h [(offer :alice :alice (fact :e0 :note))] settle
       [[:promote 0 :group 1] [:step 0] [:read :now] [:forget-person :alice] [:work 0]])
    {:values {[:alice :note nil] :erased} :shown [:pending :refused]}]
   ["B: a value forget after the read-out: crossed, then done; the copy stays"
    (h [(offer :alice :alice (fact :e0 :note))] settle
       [[:promote 0 :group 1] [:work 0] [:read :now] [:forget-value 0] [:work 0] [:read :now]
        [:batch] [:read :now]])
    {:values {[:alice :note nil] :erased [:group :note nil] :open} :shown [:crossed :crossed :done :done]}]
   ["B: Alice forgotten after the read-out: crossed, then done; the copy stays"
    (h [(offer :alice :alice (fact :e0 :note))] settle
       [[:promote 0 :group 1] [:work 0] [:read :now] [:forget-person :alice] [:batch] [:read :now]])
    {:values {[:alice :note nil] :erased [:group :note nil] :open} :shown [:crossed :done :done]}]])

(def d-cases
  "Change D: a revocation in the other store, falling between the micro
  gate's prepare and its commit; and one in the layer the permission
  governs. A value that never landed shows :missing."
  [["D: a group write under Alice's session permission; the session revokes it between prepare and commit"
    [(offer-citing :session :alice :group (fact :e0 :note)) [:prepare] [:revoke [:alice :group :session]]
     [:step 1] [:commit]]
    {:values {[:group :note nil] :missing}}]
   ["D: a group write under her group permission, revoked in the group first"
    [[:revoke [:alice :group :own]] [:batch] (offer :alice :group (fact :e0 :note)) [:batch]]
    {:values {[:group :note nil] :missing}}]])

(defn- find-fact [st [layer k persons]]
  (some #(when (and (= layer (:layer %)) (= k (:k %)) (= persons (get-in % [:v :persons]))) %)
        (m/all-facts st)))

(defn play
  "One case under one config: [matches? what-happened objections]."
  [config [_ history expect] checks]
  (let [st (m/run config history)
        values (into {} (for [[ref _] (:values expect)
                              :let [f (find-fact st ref)]]
                          [ref (cond (nil? f) :missing (m/readable? st f) :open :else :erased)]))
        shown (vec (for [r (:reads st) pr (:promotions r)] (:status pr)))
        seen (cond-> {:values values} (:shown expect) (assoc :shown shown))
        objections (for [pk checks
                         :let [v ((second (props/properties pk)) st)]
                         :when v]
                     [pk v])]
    [(= seen expect) seen objections]))

(defn- ref-str [[layer k persons]]
  (str (name layer) " " (name k) (when persons (str " " (str/join "+" (map name (sort persons)))))))

(defn report
  "Each case under each named config, one line each, with what the named
  checks object to."
  [cases named-configs checks]
  (doseq [[cname cfg] named-configs]
    (println "  under" (name cname))
    (doseq [c cases]
      (let [[ok seen objections] (play cfg c checks)]
        (println (format "    %-9s %s" (if ok "as said" "DIFFERS") (first c)))
        (when-not ok
          (println "              saw:"
                   (str/join ", " (for [[r o] (:values seen)] (str (ref-str r) " " (name o))))
                   (if (:shown seen) (str "; shown " (str/join " " (map name (:shown seen)))) "")))
        (doseq [[pk v] objections]
          (println "              " (name pk) "objects:"
                   (pr-str (dissoc v :forgotten :value :act-subjects :read-at-tick))))))))
