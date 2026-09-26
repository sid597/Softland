(ns rig.store.tools-test
  "Phase 6's proof on an in-process cluster of 4 tasks
  (PLAN-tools-and-grammars.md 7.2): grammar facts checked at the gate (G1
  to G7) and the minimal runner finding and running a tool by matching (R1
  to R5), over the model's toy keys `:note` and `:mention` and, for the
  opaque case only, a test key `:blob` (V-F11).

  The machinery count's zero: the test grammar and the test tool below are
  EDN data in this file, written through the door as facts. Nothing under
  `src/` names `:mention-count`, and the stream gate and the exit read
  `:mention`'s and `:note`'s grammar only from these facts.

  Layers: the model's one-owner world (`client/seed!`, no grammars), the
  toy grammars written by this file into the layers that use them, and a
  fresh layer per G case that changes a grammar, so each case reads only
  its own. Gus, a person made here, is the one forgotten (G2, G7), so Bob
  stays for the runner's cases. The micro gate's cases (G4's two
  envelopes of one batch) wait for phase 6b (D-P1)."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.runner :as runner]))

;; ------------------------------------------------ the test grammar and tool

(def mention-grammar
  "The proof's test grammar (the model's `:mention`: a value that names one
  or two people, README 46)."
  {:e :mention :k :grammar
   :v {:shape [:map {:persons [:set-of [:keyword] 1 2]} {:open? true}]
       :subjects-at [:persons] :opaque false :index #{}}})

(def note-grammar
  "`:note`'s grammar: any value, indexed by value (what the read exit's
  constant said before phase 6)."
  {:e :note :k :grammar :v {:shape [:any] :subjects-at nil :opaque false :index #{:by-value}}})

(def mention-count
  "The proof's test tool (5.1): for each mention in its layer, a note
  counting the people it names."
  {:e :mention-count :k :tool
   :v {:matches [:k :mention]
       :signature {:in :match :out #{:note} :rows? false}
       :permission [:mention-count :alice :alice]
       :recipe [{:name :count-note :do :emit :e [:in :e] :k :note
                 :v [:map {:token [:str [:count [:in :v :persons]] " named"]}]}]}})

(defn- tool-fact
  "A test tool fact: its id, what it matches, its output keys, its layer."
  [id matches out layer & {:keys [rows?] :or {rows? false}}]
  {:e id :k :tool
   :v {:matches matches
       :signature {:in :match :out out :rows? rows?}
       :permission [id layer layer]
       :recipe [{:name :out :do :emit :e [:in :e] :k (first out) :v [:map {:from [:in :k]}]}]}})

;; ----------------------------------------------------------------- helpers

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- act [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- answer [a] (select-keys a [:answer :reason]))

(deftest tools-and-grammars
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (rx/connect ipc)
          send! (fn [o] (c/offer-until-answered! st o))
          ok! (fn [o] (let [a (send! o)] (is (= :yes (:answer a)) (pr-str (:facts o) a)) a))
          grammars! (fn [layer gs] (ok! (act :operator layer gs)))
          layer! (fn [layer & grammar-facts]
                   (ok! (c/make-layer-offer layer {:kind :personal :owner :alice}))
                   (ok! (c/grant-offer st [:alice layer layer]))
                   (when (seq grammar-facts) (grammars! layer (vec grammar-facts))))
          alice! (fn [layer facts & more] (send! (apply act :alice layer facts more)))
          rd (fn [layer pattern]
               (rx/read! st {:reader :alice :reader-kind :person :working layer :permission [:alice layer layer]
                             :layer layer :read [:pattern pattern]}))
          values (fn [r] (set (map :value (:rows r))))
          wrap (fn [layer nm i]
                 (let [row (c/raw-row st layer [nm i])]
                   (select-keys (c/lock-row st layer (:lock-id row)) [:required :any-of])))
          opens (fn [layer nm i] (set (keys (c/opens? st layer [nm i]))))]

      (testing "the model's world, with no grammar in any layer; Gus, the person forgotten below"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (= :yes (:answer (c/make-person! st :gus)))))

      (testing "the toy grammars, written as facts: the only place the store learns them (T-FR1, T-FR7)"
        (doseq [l [:alice :alice-hand :alice-agent]] (grammars! l [mention-grammar note-grammar]))
        (let [row (foreign-select-one [(keypath :alice :key-rows :mention)] (:layers st))]
          (is (= [:persons] (get-in row [:grammar :subjects-at])) "projected by the layer's gate into its key row")
          (is (false? (:used row)))))

      ;; ------------------------------------------------------------ G1
      (testing "G1: a value refused by its key's grammar, as data, and recorded; the same values admitted with no grammar"
        (layer! :g1 mention-grammar)
        (layer! :g1-bare)
        (let [three (alice! :g1 [{:e :e1 :k :mention :v {:persons #{:bob :gus :dan}}}])
              text (alice! :g1 [{:e :e1 :k :mention :v {:persons "bob"}}])]
          (is (= {:answer :no :reason :value-shape} (answer three)) "three people: the grammar says one or two")
          (is (= {:answer :no :reason :value-shape} (answer text)) "persons as a string")
          (is (= :value-shape (:reason (c/record st (:name three)))) "recorded: the lookup by name finds it")
          (is (= :yes (:answer (alice! :g1 [{:e :e1 :k :mention :v {:persons #{:bob}}}]))) "the worker lived"))
        (is (= :yes (:answer (alice! :g1-bare [{:e :e1 :k :mention :v {:persons #{:bob :gus :dan}}}]))))
        (is (= :yes (:answer (alice! :g1-bare [{:e :e2 :k :mention :v {:persons "bob"}}])))
            "a layer with no grammar: the permissive default (4.6, T-RC2)"))

      ;; ------------------------------------------------------------ G2
      (let [g2-first (atom nil) g2-second (atom nil)]
        (testing "G2: subjects taken from the grammar reach the value's wrap"
          (layer! :g2 mention-grammar)
          (let [o (act :alice :g2 [{:e :e1 :k :mention :v {:persons #{:gus}} :mark #{:die-with-any}}])
                a (send! o)]
            (reset! g2-first (:name o))
            (is (= :yes (:answer a)))
            (is (= #{:alice :gus} (:subjects (c/record st (:name o)))) "owner and the grammar's Gus")
            (is (= {:required [:alice :gus] :any-of []} (wrap :g2 (:name o) 0)) "marked: every subject required"))
          (is (= :yes (:answer (send! (act :operator :g2 [(assoc-in mention-grammar [:v :subjects-at] nil)]))))
              "a grammar naming no subjects, same hints: admitted though the key is used")
          (let [o (act :alice :g2 [{:e :e2 :k :mention :v {:persons #{:gus}} :mark #{:die-with-any}}])
                a (send! o)]
            (reset! g2-second (:name o))
            (is (= :yes (:answer a)))
            (is (= #{:alice} (:subjects (c/record st (:name o)))))
            (is (= {:required [:alice] :any-of []} (wrap :g2 (:name o) 0))))
          (is (= {:required [:alice :gus] :any-of []} (wrap :g2 @g2-first 0))
              "the grammar changed after it: the earlier value keeps its wrap (E9, G1 to G3)"))

        ;; ---------------------------------------------------------- G7
        (let [g7 (atom {})]
          (testing "G7: a :die-with-any mark on a key with no grammar (V-F3, T-RC12)"
            (layer! :g7)
            (let [marked (act :alice :g7 [{:e :e1 :k :mention :v {:persons #{:gus}} :mark #{:die-with-any}}])
                  carried (act :alice :g7 [{:e :e2 :k :mention :v {:persons #{:gus}} :mark #{:die-with-any}}] :subjects #{:gus})
                  unmarked (act :alice :g7 [{:e :e3 :k :mention :v {:persons #{:gus}}}])
                  unmarked-g (act :alice :g1 [{:e :e3 :k :mention :v {:persons #{:gus}}}])]
              (doseq [o [marked carried unmarked unmarked-g]] (is (= :yes (:answer (send! o)))))
              (swap! g7 assoc :marked (:name marked) :carried (:name carried))
              (is (= #{:alice} (:subjects (c/record st (:name marked)))) "no grammar: the owner and what the offer carries")
              (is (= {:required [:alice] :any-of []} (wrap :g7 (:name marked) 0)) "the mark adds nothing, and the offerer is not told")
              (is (= {:required [:alice :gus] :any-of []} (wrap :g7 (:name carried) 0)) "Gus carried: required")
              (is (= {:required [:alice] :any-of []} (wrap :g7 (:name unmarked) 0)))
              (is (= {:required [:alice] :any-of []} (wrap :g1 (:name unmarked-g) 0))
                  "unmarked, with or without a grammar: the owner alone (V-F2)")))
          (testing "Gus's forget: G2's first no longer opens, its second does; G7's carried dies, its marked lives"
            (is (= :yes (:answer (c/forget-person! st :gus))))
            (is (= #{:erased-at} (opens :g2 @g2-first 0)) "the subjects came from facts")
            (is (= #{:value} (opens :g2 @g2-second 0)))
            (is (= #{:value} (opens :g7 (:marked @g7) 0)))
            (is (= #{:erased-at} (opens :g7 (:carried @g7) 0))))))

      ;; ------------------------------------------------------------ G3
      (testing "G3: an opaque key (a test key; ruling 6)"
        (layer! :g3 {:e :blob :k :grammar :v {:shape [:any] :subjects-at nil :opaque true :index #{}}})
        (is (= :yes (:answer (alice! :g3 [{:e :e1 :k :blob :v "an opaque string"}]))) "no shape check")
        (is (= :yes (:answer (alice! :g3 [{:e :e2 :k :blob :v {:persons #{:bob}}}]))) "no subjects read from it")
        (is (= {:refused :opaque} (rd :g3 [:kv :blob "an opaque string"])) "no matching")
        (let [kv (into {} (foreign-select [(keypath :g3 :ix-kv) ALL] (:layers st)))]
          (is (not-any? #(str/starts-with? % (str "blob" reads/sep)) (keys kv)) "no value index entry"))
        (let [r (rd :g3 [:k :blob])]
          (is (= 2 (count (:rows r))))
          (is (every? #(true? (:opaque %)) (:rows r)) "shown as opaque, beside its value")
          (is (= #{"an opaque string" {:persons #{:bob}}} (values r))))
        (let [fid (:fid (first (filter #(string? (:value %)) (:rows (rd :g3 [:k :blob])))))]
          (is (= :yes (:answer (c/forget-value! st :alice :g3 fid))) "an opaque value forgotten (E9, G2 x forget)")
          (let [rows (:rows (rd :g3 [:k :blob]))]
            (is (= #{:erased} (set (for [r rows :when (= fid (:fid r))] (if (contains? r :erased-at) :erased :shown)))))
            (is (= 1 (count (filter :opaque rows))) "the other still shown as opaque")))
        (is (= :malformed-control (:reason (send! (act :operator :g3 [{:e :blob2 :k :grammar :v {:shape [:any] :subjects-at [:persons] :opaque true :index #{}}}])))))
        (is (= :malformed-control (:reason (send! (act :operator :g3 [{:e :blob2 :k :grammar :v {:shape [:any] :subjects-at nil :opaque true :index #{:by-value}}}]))))))

      ;; ------------------------------------------------------------ G4
      (testing "G4: index hints read from grammar facts"
        (layer! :g4 mention-grammar note-grammar)
        (is (= :yes (:answer (alice! :g4 [{:e :e1 :k :note :v {:token "x"}} {:e :e2 :k :mention :v {:persons #{:bob}}}]))))
        (is (= #{{:token "x"}} (values (rd :g4 [:kv :note {:token "x"}]))) "by value, as its grammar says")
        (is (= {:refused :not-indexed} (rd :g4 [:kv :mention {:persons #{:bob}}])))
        (is (= :yes (:answer (send! (act :operator :alice-hand [(update-in note-grammar [:v :index] disj :by-value)])))) "before any :note there")
        (is (= :yes (:answer (alice! :alice-hand [{:e :e1 :k :note :v "h"}]))))
        (is (= {:refused :not-indexed} (rd :alice-hand [:kv :note "h"])))
        (let [by-value (update-in mention-grammar [:v :index] conj :by-value)]
          (is (= :grammar-change-needs-rebuild (:reason (send! (act :operator :g4 [by-value])))) ":g4 holds a mention")
          (is (= :yes (:answer (send! (act :operator :alice-agent [by-value])))) ":alice-agent holds none")
          (is (= :yes (:answer (alice! :alice-agent [{:e :e1 :k :mention :v {:persons #{:bob}}}]))))
          (is (= #{{:persons #{:bob}}} (values (rd :alice-agent [:kv :mention {:persons #{:bob}}]))))
          (testing "V-F4: the act's own value under the key counts as a use"
            (is (= :grammar-change-needs-rebuild
                   (:reason (send! (act :operator :alice-hand [by-value {:e :e9 :k :mention :v {:persons #{:bob}}}])))))
            (is (= :yes (:answer (send! (act :operator :alice-hand [by-value])))) "the grammar alone")
            (is (= :yes (:answer (alice! :alice-hand [{:e :e2 :k :mention :v {:persons #{:bob}}}]))) "then the value")
            (is (= #{{:persons #{:bob}}} (values (rd :alice-hand [:kv :mention {:persons #{:bob}}])))))))

      ;; ------------------------------------------------------------ G5
      (testing "G5: grammar admission"
        (layer! :g5)
        (ok! (c/grant-offer st [:mention-count :g5 :g5]))
        (is (= :malformed-control (:reason (send! (act :operator :g5 [(assoc-in note-grammar [:v :shape] [:no-such-shape])])))))
        (is (= :malformed-control (:reason (send! (act :operator :g5 [(assoc note-grammar :e :permission)])))) "a store key's grammar is compiled")
        (is (= :malformed-control (:reason (send! (act :operator :g5 [note-grammar (assoc-in note-grammar [:v :index] #{})])))) "two for one key")
        (is (= :control-not-allowed (:reason (send! (act :mention-count :g5 [note-grammar]))))
            "the tool holds a permission in the layer, but is neither the operator nor the owner"))

      ;; ------------------------------------------------------------ G6
      (testing "G6: a value is checked under the grammar in force before its act"
        (layer! :g6 mention-grammar)
        (let [one (assoc-in mention-grammar [:v :shape] [:map {:persons [:set-of [:keyword] 1 1]} {:open? true}])]
          (is (= :yes (:answer (alice! :g6 [one {:e :e1 :k :mention :v {:persons #{:alice :bob}}}])))
              "a stricter grammar and a mention of two people in one act: the value is checked under the grammar before")
          (is (= :value-shape (:reason (alice! :g6 [{:e :e2 :k :mention :v {:persons #{:alice :bob}}}])))
              "the next act's is checked under the stricter one")))

      ;; ------------------------------------------------------------ R1 to R4
      (let [first-pass (atom nil)]
        (testing "R1: the runner finds the test tool by matching and runs it once per match"
          (ok! (act :operator :alice [mention-count]))
          (ok! (c/grant-offer st [:mention-count :alice :alice]))
          (ok! (act :alice :alice [{:e :e1 :k :mention :v {:persons #{:bob}}}]))
          (ok! (act :alice :alice [{:e :e2 :k :mention :v {:persons #{:alice :bob}}}]))
          (let [rep (runner/run-pass! st {:layer :alice})
                [line & more] (:tools rep)]
            (reset! first-pass rep)
            (say "R1 report" (pr-str rep))
            (is (nil? more) "one tool")
            (is (= :mention-count (:id line)))
            (is (= [:yes :yes] (mapv :answer (:runs line))) "two runs, answered yes")
            (is (= {{:token "1 named"} :e1 {:token "2 named"} :e2}
                   (into {} (map (juxt :value :e)) (:rows (rd :alice [:k :note])))))
            (is (every? #(= :mention-count (:who (c/record st (first (:fid %))))) (:rows (rd :alice [:k :note])))
                "each by :mention-count")))

        (testing "R2: once per match"
          (let [rep (runner/run-pass! st {:layer :alice})]
            (is (= 2 (count (:runs (first (:tools rep))))))
            (is (every? :recorded (:runs (first (:tools rep)))) "both ran before: answered from the record")
            (is (= 2 (count (:rows (rd :alice [:k :note])))) "no new fact"))
          (ok! (act :alice :alice [{:e :e3 :k :mention :v {:persons #{:bob}}}]))
          (let [runs (:runs (first (:tools (runner/run-pass! st {:layer :alice}))))]
            (is (= 1 (count (remove :recorded runs))) "exactly one new run")
            (is (= :yes (:answer (first (remove :recorded runs))))))
          (testing "two runners at once over a new match: one output, and neither is refused :name-taken (V-F5)"
            (ok! (act :alice :alice [{:e :e5 :k :mention :v {:persons #{:alice}}}]))
            (let [passes (mapv deref [(future (runner/run-pass! st {:layer :alice}))
                                      (future (runner/run-pass! st {:layer :alice}))])
                  e5-runs (for [p passes l (:tools p) r (:runs l)
                                :when (= :e5 (:e (first (filter #(= (:match r) (:fid %)) (:rows (rd :alice [:k :mention]))))))]
                            r)]
              (say "two runners, the new match's runs" (pr-str e5-runs))
              (is (= 2 (count e5-runs)))
              (is (every? #(or (= :yes (:answer %)) (= :yes (get-in % [:recorded :answer]))) e5-runs)
                  "each a yes: offered, answered from the record, or found recorded by the lookup")
              (is (= 1 (count (filter #(= :e5 (:e %)) (:rows (rd :alice [:k :note]))))) "one output for the match"))))

        (testing "R3: its reads recorded with its preference"
          (let [rep @first-pass
                line (first (:tools rep))
                entry (first (c/facts st :alice (:entry line)))
                tools-entry (first (c/facts st :alice (:entry (:tools-read rep))))]
            (is (= :mention-count (:who (c/record st (:entry line)))))
            (is (= [:read/pattern :matched [:k :mention] 2 false]
                   [(:k entry) (get-in entry [:v :role]) (get-in entry [:v :pattern]) (get-in entry [:v :count])
                    (contains? (:v entry) :exact)])
                "the short line: no exact list (ruling 3's default for a tool)")
            (is (= :operator (:who (c/record st (:entry (:tools-read rep))))))
            (is (= [:stood-on [:k :tool]] [(get-in tools-entry [:v :role]) (get-in tools-entry [:v :pattern])])))
          (ok! (act :operator :alice [(tool-fact :mention-rows [:k :mention] #{:tag} :alice :rows? true)]))
          (ok! (c/grant-offer st [:mention-rows :alice :alice]))
          (let [rep (runner/run-pass! st {:layer :alice})
                line (first (filter #(= :mention-rows (:id %)) (:tools rep)))
                entry (first (c/facts st :alice (:entry line)))]
            (is (= (count (:rows (rd :alice [:k :mention]))) (count (:runs line))) "one run per mention in the layer")
            (is (= (set (map (fn [r] [(:fid r) (:stamp r)]) (:rows (rd :alice [:k :mention]))))
                   (set (get-in entry [:v :exact])))
                "a tool whose signature asks for rows: the exact matched pairs")))

        (testing "R4: its outputs admitted under its permission, standing on the match and the tool"
          (let [line (first (:tools @first-pass))
                tool-row (first (filter #(= :mention-count (:e %)) (:rows (rd :alice [:k :tool]))))]
            (doseq [run (:runs line)
                    :let [rec (c/record st (:name run))
                          stood (c/stood-on st :alice (:name run))
                          matched-stamp (:stamp (c/record st (first (:match run))))]]
              (is (= [:mention-count [:mention-count :alice :alice] (first (:match run))]
                     [(:who rec) (:permission rec) (:because-of rec)]))
              (is (= {(:match run) matched-stamp (:fid tool-row) (:stamp tool-row)} stood)
                  "based-on: the matched fact and the tool fact, with their stamps")
              (is (< (max matched-stamp (:stamp tool-row)) (:stamp rec)) "stamped after both")))
          (ok! (c/revoke-offer st [:mention-count :alice :alice]))
          (ok! (act :alice :alice [{:e :e4 :k :mention :v {:persons #{:bob}}}]))
          (let [rep (runner/run-pass! st {:layer :alice})
                line (first (filter #(= :mention-count (:id %)) (:tools rep)))]
            (say "R4 after the revoke" (pr-str line))
            (is (= :permission-revoked (get-in line [:read :refused])) "its entry refused: nothing shown, nothing run")
            (is (nil? (:runs line))))
          (let [o (act :mention-count :alice [{:e :e4 :k :note :v {:token "by hand"}}] :session (c/default-session :mention-count))
                a (send! o)]
            (is (= :permission-revoked (:reason a)))
            (is (= :permission-revoked (:reason (c/record st (:name o)))) "recorded"))))

      ;; ------------------------------------------------------------ R6
      (testing "R6: a replaced tool: the new version runs over the existing matches, the old one no longer runs (D7)"
        (layer! :r6 mention-grammar note-grammar)
        (ok! (c/grant-offer st [:versioned :r6 :r6]))
        (let [v (fn [n] {:matches [:k :mention] :signature {:in :match :out #{:note} :rows? false}
                         :permission [:versioned :r6 :r6]
                         :recipe [{:name :out :do :emit :e [:in :e] :k :note :v [:map {:version n}]}]})
              t1 (act :operator :r6 [{:e :versioned :k :tool :v (v 1)}])]
          (ok! t1)
          (ok! (act :alice :r6 [{:e :e1 :k :mention :v {:persons #{:bob}}}]))
          (is (= [:yes] (mapv :answer (:runs (first (:tools (runner/run-pass! st {:layer :r6})))))))
          (ok! (act :operator :r6 [{:e :versioned :k :tool :v (v 2) :replaces [(:name t1) 0]}]))
          (let [rep (runner/run-pass! st {:layer :r6})]
            (is (= 1 (count (:tools rep))) "the replaced version is not run")
            (is (= [:yes] (mapv :answer (:runs (first (:tools rep))))) "the new one runs over the existing match"))
          (is (= #{{:version 1} {:version 2}} (set (map :value (:rows (rd :r6 [:k :note])))))
              "the old version's output stays")))

      ;; ------------------------------------------------------------ R5
      (testing "R5: a loop refused, in stamp order, before anything runs"
        (layer! :r5 mention-grammar note-grammar)
        (ok! (act :alice :r5 [{:e :e1 :k :mention :v {:persons #{:bob}}} {:e :e2 :k :note :v "n"}]))
        (doseq [[id m out] [[:self-note [:k :note] #{:note}]
                            [:a-tool [:k :mention] #{:note}]
                            [:b-tool [:k :note] #{:mention}]
                            [:all-tool [:all] #{:tag}]
                            [:entries-tool [:k :read/pattern] #{:note}]]]
          (ok! (act :operator :r5 [(tool-fact id m out :r5)]))
          (ok! (c/grant-offer st [id :r5 :r5])))
        (let [passes [(runner/run-pass! st {:layer :r5}) (runner/run-pass! st {:layer :r5})]
              by-id (fn [rep] (into {} (map (fn [l] [(or (:id l) (:tool l)) l])) (:tools rep)))
              tool-fids (into {} (map (juxt :e :fid)) (:rows (rd :r5 [:k :tool])))]
          (say "R5 first pass" (pr-str (first passes)))
          (doseq [id [:self-note :b-tool :all-tool :entries-tool]]
            (is (= :tool-loop (:refused (get (by-id (first passes)) (get tool-fids id)))) (str id)))
          (is (= [:yes] (mapv :answer (:runs (get (by-id (first passes)) :a-tool)))) "A runs")
          (is (every? :recorded (:runs (get (by-id (second passes)) :a-tool))))
          (let [writers (set (map #(:who (c/record st (first (:fid %)))) (concat (:rows (rd :r5 [:k :note]))
                                                                                  (:rows (rd :r5 [:k :mention]))
                                                                                  (:rows (rd :r5 [:k :read/pattern])))))]
            (is (empty? (set/intersection writers #{:self-note :b-tool :all-tool :entries-tool}))
                "no output and no read entry by a refused tool, after two passes")))))))
