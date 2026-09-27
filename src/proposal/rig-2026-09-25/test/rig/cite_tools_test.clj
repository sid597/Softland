(ns rig.cite-tools-test
  "The first tool end to end (PLAN-citation.md section 8), on an in-process
  cluster of 4 tasks: the four tools of tools/citation.edn written as facts,
  run by the driver's own functions (rig.cite.run) over a scratch
  repository whose cited functions move, change and go between the commit
  a document was written at and HEAD. Every citation found is bound or not
  bound yet, never skipped; the report's states at HEAD are true, moved and
  stale where the history says; the binding stands on the form; the walk
  found what stood on the old reading through the store's lookup; a pass
  after quiet adds nothing; nothing under src/ names a tool (the machinery
  count's zero). Last, the reader in the base, where the pointer session
  puts material. The scratch repository is left in the system's temporary
  folder."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama.test :as rtest]
            [rig.cite.run :as run]
            [rig.material :as m]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.module :as module]
            [rig.store.read-exit :as rx]
            [rig.store.runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- clean-env [] (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_"))) (System/getenv)))

(defn- sh! [dir env & args]
  (let [r (apply shell/sh (concat args [:dir dir :env (merge (clean-env) env)]))]
    (assert (zero? (:exit r)) (str args " " (:err r)))
    (str/trim (:out r))))

(defn- commit! [dir t msg files]
  (doseq [[p content] files]
    (io/make-parents (io/file dir p))
    (spit (io/file dir p) content))
  (sh! dir {} "git" "add" "-A")
  (sh! dir {"GIT_AUTHOR_DATE" (str t " +0000") "GIT_COMMITTER_DATE" (str t " +0000")
            "GIT_AUTHOR_NAME" "t" "GIT_AUTHOR_EMAIL" "t@t" "GIT_COMMITTER_NAME" "t" "GIT_COMMITTER_EMAIL" "t@t"}
       "git" "commit" "-q" "-m" msg)
  (sh! dir {} "git" "rev-parse" "HEAD"))

(def doc-1 (str "The function `src/a.clj:4-5`, then `a.clj:7` and `a.clj:9`.\n"
                "A comment `a.clj:1`, a note `notes.md:3`, a missing `x.clj:1`, a gap `a.clj:8`.\n"))

(defn- scratch
  "c1: a document cites f, g and h in src/a.clj. c2: f moves down a line, g's
  text changes, h goes. c3: the document cites f again, as it now stands.
  c4: f moves back up; so the third reading's walk passes through the
  second walk's marks (the chain's second link)."
  []
  (let [dir (str (Files/createTempDirectory "cite-tools-test" (make-array FileAttribute 0)))]
    (sh! dir {} "git" "init" "-q" "-b" "main")
    (let [c1 (commit! dir 1790000000 "one"
                      {"src/a.clj" (str/join "\n" [";; a header" "(ns a)" "" "(defn f []" "  1)" "" "(defn g [] 2)" ""
                                                   "(defn h [] 3)" ""])
                       "notes.md" "# Notes\n\nsome text\n"
                       "docs/d.md" doc-1})
          c2 (commit! dir 1790001000 "two"
                      {"src/a.clj" (str/join "\n" [";; a header" "" "(ns a)" "" "(defn f []" "  1)" "" "(defn g [] 22)" ""])})
          c3 (commit! dir 1790002000 "three"
                      {"docs/d.md" (str doc-1 "Again `a.clj:5-6`.\n")})
          c4 (commit! dir 1790003000 "four"
                      {"src/a.clj" (str/join "\n" [";; a header" "(ns a)" "" "(defn f []" "  1)" "" "(defn g [] 22)" ""])})]
      {:dir dir :c1 c1 :c2 c2 :c3 c3 :c4 c4})))

(def tool-ids #{:material-reader :cite-finder :cite-binder :cite-marker})

(deftest the-tool-end-to-end
  (let [{:keys [dir c1 c3 c4]} (scratch)]
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 1})
      (let [st (rx/connect ipc)
            out (run/run! {:st st} {:repo dir :repo-id :softland :owner :sid :layer :softland
                                    :heads {:main "main"} :docs [[:main "docs/d.md"]]
                                    :tools "tools/citation.edn"})
            rows (:rows out)
            by-text (into {} (map (fn [r] [(get-in r [:found :value :text]) r])) rows)
            state (fn [t] (select-keys (:state (get by-text t)) [:state :why :cited-now]))]
        (say "plan" (pr-str (:plan out)))
        (doseq [r rows] (say (get-in r [:found :value :text]) (pr-str (:state r))))
        (testing "every citation found, each bound or not bound yet, none skipped"
          (is (= 8 (count rows)))
          (is (every? #(or (:bound %) (:unbound %)) rows)))
        (testing "the reading plan: the commits the citations were written at, then HEAD"
          (is (= {[:main "src/a.clj"] [c1 c3 c4]} (:plan out))))
        (testing "the states at HEAD, each since the citation was written"
          (is (= {:state :true :why :same} (state "src/a.clj:4-5")) "f moved down and back: as written again")
          (is (= {:state :moved :why :moved :cited-now [4 5]} (state "a.clj:5-6")) "written at c3, f has moved up")
          (is (= {:state :stale :why :changed} (state "a.clj:7")) "g's text changed")
          (is (= {:state :stale :why :gone} (state "a.clj:9")) "h is gone")
          (is (= {:state :not-bound-yet :why :not-inside-one-form} (state "a.clj:1")) "a comment")
          (is (= {:state :not-bound-yet :why :not-inside-one-form} (state "a.clj:8")) "a blank line")
          (is (= {:state :not-bound-yet :why :markdown-target} (state "notes.md:3")))
          (is (= {:state :not-bound-yet :why :path-not-found} (state "x.clj:1"))))
        (testing "the binding stands on the form: the store's lookup finds it from the form's fact"
          (let [b (get-in (by-text "src/a.clj:4-5") [:bound])
                form-fid (get-in b [:value :form :fid])
                r (rx/read! st {:reader :sid :reader-kind :person :working :softland :permission [:sid :softland :softland]
                                :layer :softland :read [:pattern [:dependents form-fid]]})]
            (is (some #(= (:fid b) (:fid %)) (:rows r)) "the bound fact stood on f's fact at c1")))
        (testing "the walk found what stood on the old reading, through the lookup, link by link"
          (let [walks (sort-by :stamp (get-in out [:gathered :walks]))]
            (is (= 3 (count walks)) "one walk a reading")
            (is (= [0 3 4] (mapv #(get-in % [:value :marked]) walks))
                "the second walk marks f, g and h; the third finds them again in the second's marks, and c3's citation")))
        (testing "a pass after quiet offers nothing new"
          (let [rep (runner/run-pass! st {:layer :softland :config {:repos {:softland dir}} :limit 10000})]
            (is (empty? (for [l (:tools rep) r (:runs l) :when (contains? r :answer)] r)))))
        (testing "the report says it"
          (let [md (run/report-markdown rows {:heads (:heads out) :at "test"})]
            (is (str/includes? md "moved: now lines 4-5"))
            (is (str/includes? md "**8 citations.**"))))
        (testing "the machinery count's zero: nothing under src/ names a tool"
          (let [srcs (filter #(str/ends-with? (str %) ".clj") (file-seq (io/file "src")))]
            (is (empty? (for [f srcs t tool-ids :when (str/includes? (slurp f) (str t))] [(str f) t])))))

        (testing "the base, where the pointer session puts material: no tool runs there on the rig"
          ;; locks/lease-under: a writer who is no person leases under the layer's
          ;; person owner, and the base's owner is the root actor, so a tool's lease
          ;; is refused and its read entry with it (:no-such-lock). A finding, not
          ;; this build's; the road that works is the same step as operator code.
          (is (every? #(= :yes (:answer %)) (c/seed-base! st [:sid])))
          (let [[reader] (run/tool-facts "tools/citation.edn" :base)
                file (m/file-thing :softland :main "src/a.clj")
                request {:repo :softland :line :main :rev c1 :path "src/a.clj"}]
            (run/write-tools! st :base [reader])
            (c/offer-until-answered! st (c/build {:who :sid :layer :base :class :by-layer :permission [:sid :base :base]
                                                  :facts [{:e file :k :material/read :v request}]}))
            (let [rep (runner/run-pass! st {:layer :base :config {:repos {:softland dir}}})]
              (say "base, the tool" (pr-str (:tools rep)))
              (is (= :no-such-lock (get-in (first (:tools rep)) [:read :refused]))
                  "the tool's own match read is refused"))
            (let [nm (env/make-name :base :by-layer)
                  r (m/reading-step dir :softland {:request request :file file :prev [] :run nm :same :name-in-file})
                  a (c/offer-until-answered! st (c/build {:name nm :who :operator :layer :base :class :by-layer
                                                          :facts (:facts r)}))
                  rows (:rows (rx/read! st {:reader :sid :reader-kind :person :working :base :permission [:sid :base :base]
                                            :layer :base :read [:pattern [:k :material/form]]}))]
              (is (= :yes (:answer a)) "the same reading, offered by operator code as the operator")
              (is (= #{"ns" "f" "g" "h"} (set (map (comp :name :value) rows)))))))))))
