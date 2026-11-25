(ns piece3-statements.generator
  "Generator of Piece 3 histories using `clojure.test.check.generators`.

  Generates histories across both streaming lanes (`:lab`) and the microbatch
  base (`:base`), combining:
  - pre-making refusals (from strangers and future holders, so stamp 0 can be
    a refusal and Q3 is exercised before `:make-lane` runs);
  - `:make-lane`;
  - permission gives (`:see`, `:see-and-act`, `:run-permission`,
    `:act-permission`, and depth-2/3 chains `:first -> :ben -> :cal`), ends
    (direct and ancestor ends), and re-gives;
  - writes and toggles of `:softland.store/who-may-see` and
    `:softland.store/who-may-read-acts`, plus refused attempts on the setting
    by given holders, wrong entities, retracts, invalid values, and marks;
  - taken acts carrying `:session`, `:based-on`, `:because-of`, `:subjects`,
    and `:claimed-when`, both ordinary and heavy (~400 KiB);
  - refused things from senders with standing (`:replaced-fact-not-current`
    etc.), both `<= 256 KiB` and heavy (`~450 KiB`, between 256 KiB and
    32 MiB);
  - refused things from senders with no standing, both `<= 256 KiB` and heavy
    (`~450 KiB > 256 KiB`);
  - declaration-refused raw things (`:no-door-time`, `:value-too-large` at
    `~100 KiB <= 256 KiB` and `~350 KiB > 256 KiB`, missing/unusable `:who`);
  - strays, things on the other gate's depot, and duplicate arrivals under a
    used `act-id` (same act with another `:door-time`, with `:door-time` after
    `:no-door-time`, and a different act);
  - checkpoint `:read-sweep` steps mid-history and at the end."
  (:require [clojure.test.check.generators :as gen])
  (:import [java.util UUID]))

(defn det-uuid
  "Deterministic UUID from two integers so shrinking and seed replay are 100%
  reproducible."
  ^UUID [hi lo]
  (UUID. (long hi) (long lo)))

(def blob-50k (apply str (repeat 50000 "a")))
(def blob-100k (apply str (repeat 100000 "b")))
(def blob-350k (apply str (repeat 350000 "c")))

(def heavy-9-facts
  "9 plain facts of 50 KB each (~450 KB total): passes `declaration`
  (`50 KB <= 64 KB`, `450 KB <= 18 MB`), exceeds 256 KiB, and is well under
  32 MiB."
  (mapv (fn [i] {:e (keyword (str "heavy-" i)) :k :payload :v blob-50k})
        (range 9)))

(def medium-2-facts
  "2 plain facts of 45 KB each (~90 KB total): `<= 256 KiB`, passes
  `declaration`, and exceeds a tight `64 KiB` read byte bound."
  [{:e :med-0 :k :payload :v (apply str (repeat 45000 "m"))}
   {:e :med-1 :k :payload :v (apply str (repeat 45000 "n"))}])

(defn step-gen [idx]
  (let [u1 (det-uuid 1000 idx)
        u2 (det-uuid 2000 idx)]
    (gen/frequency
     [[4 (gen/return {:op :make-lane :uuid u1})]
      [5 (gen/let [who (gen/elements [:ben :cal])
                   allows (gen/elements [:see :see-and-act])]
           {:op :give-lane-perm :holder who :allows allows :uuid u1})]
      [4 (gen/let [who (gen/elements [:ben :cal :dov])
                   run-id (gen/elements [:run-1 :run-2])
                   allows (gen/elements [:see :see-and-act])]
           {:op :give-run-perm :holder who :run run-id :allows allows :uuid u1})]
      [3 (gen/return {:op :give-act-perm :holder :nia :uuid u1 :target-uuid u2})]
      [4 (gen/let [target (gen/elements [[:holder :ben] [:holder :cal] [:run :run-1]])]
           {:op :end-perm :target target :uuid u1})]
      [5 (gen/let [v (gen/elements [:everyone :members])]
           {:op :set-who-may-see :value v :uuid u1})]
      [6 (gen/let [v (gen/elements [:everyone :members])]
           {:op :set-who-may-read-acts :value v :uuid u1})]
      [4 (gen/let [kind (gen/elements [:given-holder :wrong-entity :retract :bad-word :mark-it])]
           {:op :bad-acts-setting :kind kind :uuid u1})]
      [6 (gen/let [sender (gen/elements [:first :ben :cal])
                   heavy? (gen/elements [false false true])]
           {:op :taken-act :sender sender :heavy? heavy? :uuid u1})]
      [5 (gen/let [heavy? (gen/elements [false true])]
           {:op :refused-with-standing :heavy? heavy? :uuid u1 :stale-uuid u2})]
      [5 (gen/let [sender (gen/elements [:zed :eve :ben :cal])
                   heavy? (gen/elements [false true])]
           {:op :refused-no-standing :sender sender :heavy? heavy? :uuid u1})]
      [5 (gen/let [kind (gen/elements [:no-time-small :no-time-heavy
                                       :val-100k :val-350k
                                       :no-who :bad-who-string :bad-who-long])]
           {:op :refused-declaration :kind kind :uuid u1})]
      [3 (gen/let [kind (gen/elements [:not-a-map :no-act-id :other-lane :other-gate-act-id])]
           {:op :stray :kind kind :uuid u1})]
      [3 (gen/return {:op :other-gate-arrival :uuid u1})]
      [4 (gen/let [kind (gen/elements [:same-new-time :different-act :time-after-no-time])]
           {:op :duplicate-act-id :kind kind :uuid u1})]
      [5 (gen/return {:op :read-sweep})]])))

(def history-gen
  "Generates `{:lane (:lab|:base) :steps [...]}` of 14 to 28 steps, always
  ending with a `:read-sweep`."
  (gen/let [lane (gen/elements [:lab :base])
            n (gen/choose 14 26)
            steps (apply gen/tuple (map step-gen (range 1 (inc n))))]
    {:lane lane
     :steps (conj (vec steps) {:op :read-sweep})}))
