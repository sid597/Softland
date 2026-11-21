(ns auth-blind.generators
  "test.check generators for Auth's 18-call client contract (`spine/auth/SPEC.md`),
  including repeated writes across one or two runtime clients, advancing clock
  times that expire sign-ins, sessions and asks, `:driven-by` options on
  `open-session`, and malformed inputs."
  (:require [clojure.test.check.generators :as gen])
  (:import [java.util UUID]
           [java.util.concurrent.atomic AtomicLong]))

(def ^:private trial-seq
  "Monotonic sequence so every generated trial uses globally disjoint UUIDs,
  emails, account subjects, hashes and 8-char ask codes — allowing the exact
  same suite to run against a shared Rama cluster with zero cross-trial state
  collision."
  (AtomicLong. 1000))

(defn next-trial-id! []
  (.incrementAndGet trial-seq))

(defn trial-uuid [trial-id slot]
  (UUID. (long trial-id) (long slot)))

(defn trial-hash [trial-id slot]
  (format "%032x%032x" (long trial-id) (long slot)))

(defn trial-code [trial-id slot]
  ;; Exactly 8 characters, unique per (trial-id, slot) for millions of values
  (let [n (+ (* (mod (long trial-id) 100000) 1000) (mod (long slot) 1000))]
    (format "%08d" n)))

(defn trial-email [trial-id slot]
  (str "p" trial-id "-" slot "@softland.example"))

(defn trial-subject [trial-id slot]
  (str "sub-" trial-id "-" slot))

;; ---------------------------------------------------------------------------
;; Leaf generators
;; ---------------------------------------------------------------------------

(def gen-provider
  (gen/elements [:google :github :oidc]))

(def gen-kind
  (gen/elements [:browser :program]))

(def gen-short-text
  (gen/one-of [(gen/elements ["cli" "notebook" "agent-worker" "review-bot" "x"])
               (gen/fmap #(apply str (repeat % \m)) (gen/choose 1 256))]))

(def gen-opt-label
  (gen/one-of [(gen/return nil) gen-short-text]))

(def gen-driven-by
  "Tonight's spec addition: optional `:driven-by` on `open-session`, nil/absent,
  `{:kind :person}`, or `{:kind :model :model <text <= 256 chars>}`."
  (gen/frequency
   [[2 (gen/return :absent)]
    [2 (gen/return nil)]
    [3 (gen/return {:kind :person})]
    [4 (gen/fmap (fn [model-name] {:kind :model :model model-name})
                 (gen/one-of [(gen/elements ["claude-opus-4-6" "gpt-5" "gemini-3"])
                              (gen/fmap #(apply str (repeat % \z)) (gen/choose 1 256))]))]]))

(def gen-says
  (gen/map (gen/elements ["name" "email" "picture" "locale"])
           (gen/elements ["Ada" "Ben" "https://example/pic" "en"])
           {:min-elements 0 :max-elements 4}))

;; ---------------------------------------------------------------------------
;; Symbolic 18-call history generator
;; ---------------------------------------------------------------------------

(def gen-step
  "Generates one symbolic step of an Auth history covering all 18 contract
  functions (`open`, `close!`, `invite`, `enter`, `own-layer-made`,
  `begin-sign-in`, `replace-secret`, `end-sign-in`, `make-actor`, `end-actor`,
  `open-session`, `close-session`, `ask`, `answer-ask`, `who-is`, `ask-of`,
  `under`, `watch-root`), plus explicit `:repeat` of an earlier write and
  `:advance-time`."
  (gen/frequency
   [[5 (gen/fmap (fn [[by-code? client-idx dt]]
                   {:op :invite :by-code? by-code? :client client-idx :dt dt})
                 (gen/tuple gen/boolean (gen/choose 0 1) (gen/choose 0 50)))]

    [2 (gen/fmap (fn [[idx conflict-kind dt]]
                   {:op :invite-conflict :pick idx :kind conflict-kind :dt dt})
                 (gen/tuple gen/nat (gen/elements [:same-claim :same-person :exact-repeat]) (gen/choose 0 50)))]

    [6 (gen/fmap (fn [[pick provider says use-linked? dt]]
                   {:op :enter :pick pick :provider provider :says says :use-linked? use-linked? :dt dt})
                 (gen/tuple gen/nat gen-provider gen-says gen/boolean (gen/choose 0 50)))]

    [3 (gen/fmap (fn [[pick dt]] {:op :own-layer-made :pick pick :dt dt})
                 (gen/tuple gen/nat (gen/choose 0 50)))]

    [6 (gen/fmap (fn [[r-pick a-pick kind span label client-idx dt]]
                   {:op :begin-sign-in :r-pick r-pick :a-pick a-pick :kind kind
                    :span span :label label :client client-idx :dt dt})
                 (gen/tuple gen/nat gen/nat gen-kind
                            (gen/one-of [(gen/choose -20 0) (gen/choose 100 1000)])
                            gen-opt-label (gen/choose 0 1) (gen/choose 0 50)))]

    [4 (gen/fmap (fn [[g-pick span prev-span wrong? dt]]
                   {:op :replace-secret :g-pick g-pick :span span :prev-span prev-span
                    :wrong? wrong? :dt dt})
                 (gen/tuple gen/nat (gen/choose 100 1000) (gen/choose 50 500)
                            (gen/frequency [[4 (gen/return false)] [1 (gen/return true)]])
                            (gen/choose 0 50)))]

    [3 (gen/fmap (fn [[g-pick dt]] {:op :end-sign-in :g-pick g-pick :dt dt})
                 (gen/tuple gen/nat (gen/choose 0 50)))]

    [5 (gen/fmap (fn [[r-pick b-pick label dt]]
                   {:op :make-actor :r-pick r-pick :b-pick b-pick :label label :dt dt})
                 (gen/tuple gen/nat gen/nat gen-short-text (gen/choose 0 50)))]

    [3 (gen/fmap (fn [[r-pick a-pick dt]]
                   {:op :end-actor :r-pick r-pick :a-pick a-pick :dt dt})
                 (gen/tuple gen/nat gen/nat (gen/choose 0 50)))]

    [6 (gen/fmap (fn [[g-pick span driven-by dt]]
                   {:op :open-session :g-pick g-pick :span span :driven-by driven-by :dt dt})
                 (gen/tuple gen/nat
                            (gen/one-of [(gen/choose -10 0) (gen/choose 80 800)])
                            gen-driven-by
                            (gen/choose 0 50)))]

    [3 (gen/fmap (fn [[s-pick span driven-by dt]]
                   {:op :refresh-session :s-pick s-pick :span span :driven-by driven-by :dt dt})
                 (gen/tuple gen/nat (gen/choose -100 600) gen-driven-by (gen/choose 0 50)))]

    [3 (gen/fmap (fn [[s-pick dt]] {:op :close-session :s-pick s-pick :dt dt})
                 (gen/tuple gen/nat (gen/choose 0 50)))]

    [4 (gen/fmap (fn [[asks span dt]] {:op :ask :asks asks :span span :dt dt})
                 (gen/tuple gen-short-text (gen/choose 50 600) (gen/choose 0 50)))]

    [4 (gen/fmap (fn [[u-pick r-pick mode span label dt]]
                   {:op :answer-ask :u-pick u-pick :r-pick r-pick :mode mode
                    :span span :label label :dt dt})
                 (gen/tuple gen/nat gen/nat
                            (gen/elements [:yes-self :yes-existing :yes-new-agent :no])
                            (gen/choose 100 800) gen-short-text (gen/choose 0 50)))]

    [4 (gen/fmap (fn [[idx other-client? vary-extra? dt]]
                   {:op :repeat-write :pick idx :other-client? other-client?
                    :vary-extra? vary-extra? :dt dt})
                 (gen/tuple gen/nat gen/boolean gen/boolean (gen/choose 0 50)))]

    [4 (gen/fmap (fn [jump] {:op :advance-time :jump jump})
                 (gen/choose 150 1200))]

    [3 (gen/fmap (fn [[g-pick s-pick]] {:op :who-is :g-pick g-pick :s-pick s-pick})
                 (gen/tuple gen/nat gen/nat))]

    [2 (gen/fmap (fn [u-pick] {:op :ask-of :u-pick u-pick}) gen/nat)]

    [2 (gen/fmap (fn [r-pick] {:op :under :r-pick r-pick}) gen/nat)]

    [2 (gen/fmap (fn [[r-pick stop?]] {:op :watch-root :r-pick r-pick :stop? stop?})
                 (gen/tuple gen/nat gen/boolean))]

    [1 (gen/return {:op :open-and-close-temp-client})]]))

(def gen-history
  (gen/vector gen-step 5 45))

;; ---------------------------------------------------------------------------
;; Invalid call input generator (for `:read-the-event`)
;; ---------------------------------------------------------------------------

(defn invalid-inputs-for-trial [trial-id]
  (let [u1 (trial-uuid trial-id 1)
        u2 (trial-uuid trial-id 2)
        u3 (trial-uuid trial-id 3)
        h1 (trial-hash trial-id 1)
        h2 (trial-hash trial-id 2)
        c1 (trial-code trial-id 1)
        over-256 (apply str (repeat 257 \x))
        over-2048 (apply str (repeat 2049 \y))
        says-33 (into {} (map (fn [i] [(str "k" i) "v"]) (range 33)))]
    [[:invite "not-a-map"]
     [:invite {:person "not-uuid" :claim {:by :email :email "a@b"} :invited-by :system :at 1}]
     [:invite {:person u1 :claim {:by :email :email over-256} :invited-by :system :at 1}]
     [:invite {:person u1 :claim {:by :code :hash "UPPERCASE_OR_SHORT"} :invited-by :system :at 1}]
     [:enter {:account ["not-kw" "sub"] :says {} :own-layer u1 :making {:uuid u2 :session u3} :at 1}]
     [:enter {:account [:google over-256] :says {} :own-layer u1 :making {:uuid u2 :session u3} :at 1}]
     [:enter {:account [:google "sub"] :says says-33 :own-layer u1 :making {:uuid u2 :session u3} :at 1}]
     [:enter {:account [:google "sub"] :says {"k" over-2048} :own-layer u1 :making {:uuid u2 :session u3} :at 1}]
     [:enter {:account [:google "sub"] :says {} :own-layer :not-a-uuid :making {:uuid u2 :session u3} :at 1}]
     [:own-layer-made {:person u1 :at 1N}]
     [:begin-sign-in {:root u1 :actor u1 :sign-in u2 :kind :mobile :hash h1 :until 10 :at 1}]
     [:begin-sign-in {:root u1 :actor u1 :sign-in u2 :kind :browser :hash h1 :until 10 :label over-256 :at 1}]
     [:replace-secret {:root u1 :sign-in u2 :old-hash "bad" :new-hash h2 :until 20 :previous-until 15 :at 1}]
     [:end-sign-in {:root u1 :sign-in "bad" :at 1}]
     [:make-actor {:root u1 :actor u2 :made-by u1 :label over-256 :at 1}]
     [:end-actor {:root u1 :actor u2 :at "now"}]
     [:open-session {:root u1 :sign-in u2 :session u3 :until 20 :at 1 :driven-by {:kind :robot}}]
     [:open-session {:root u1 :sign-in u2 :session u3 :until 20 :at 1 :driven-by {:kind :model}}]
     [:open-session {:root u1 :sign-in u2 :session u3 :until 20 :at 1 :driven-by {:kind :model :model over-256}}]
     [:open-session {:root u1 :sign-in u2 :session u3 :until 20 :at 1 :driven-by {:kind :person :extra 1}}]
     [:close-session {:root u1 :session "bad" :at 1}]
     [:ask {:code "7chars!" :hash h1 :asks "cli" :ends 20 :at 1}]
     [:ask {:code "9chars!!!" :hash h1 :asks "cli" :ends 20 :at 1}]
     [:ask {:code c1 :hash h1 :asks over-256 :ends 20 :at 1}]
     [:answer-ask {:code c1 :yes? "yes" :root u1 :actor u1 :sign-in u2 :until 20 :at 1}]
     [:who-is {:root "bad" :sign-in u2 :session nil}]
     [:ask-of "short"]
     [:under "not-a-uuid"]]))
