(ns rig.cite-test
  "The citation's steps (PLAN-citation.md section 2), without a cluster:
  finding citations in a line, choosing a file, and, over a scratch git
  repository with two lines of work and set commit times, the finder's
  facts (written-at from blame, resolution in its own line then another),
  the binder's (bound, late, and every not-bound reason) and the marker's
  (true, moved, stale). The scratch repository is made in the system's
  temporary folder and left there."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rig.cite :as cite]
            [rig.cite.run :as run]
            [rig.material :as m])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; ------------------------------------------------------------- finding

(deftest finding-in-a-line
  (is (= [{:col 1 :text "src/a.clj:3" :path-text "src/a.clj" :lines [3 3] :inherited? false}]
         (cite/find-in-line "src/a.clj:3")))
  (is (= [[3 4] [6 6]] (mapv :lines (cite/find-in-line "`src/a.clj:3-4` and `a.clj:6`"))))
  (is (= [12 14] (:lines (first (cite/find-in-line "RIG.md:12–14")))) "an en dash")
  (testing "continuations take the path before them"
    (let [cs (cite/find-in-line "(`src/x/m.clj:20-23`, `:99-105`, `:7`) and `s.clj:475`, `:676`")]
      (is (= [["src/x/m.clj" [20 23] false] ["src/x/m.clj" [99 105] true] ["src/x/m.clj" [7 7] true]
              ["s.clj" [475 475] false] ["s.clj" [676 676] true]]
             (mapv (juxt :path-text :lines :inherited?) cs)))
      (is (= ":99-105" (:text (second cs))))))
  (testing "the path in backticks with the colon outside, and bare continuations"
    (is (= [["rama/envelope.clj" [18 19] false]]
           (mapv (juxt :path-text :lines :inherited?) (cite/find-in-line "see `rama/envelope.clj`:18-19 there"))))
    (is (= [[66 70] [226 226]] (mapv :lines (cite/find-in-line "`page/matter_room.cljc`:66-70 and :226"))))
    (is (= [["envelope.clj" [18 56] false] ["envelope.clj" [81 86] true] ["envelope.clj" [113 119] true]
            ["envelope.clj" [367 367] true]]
           (mapv (juxt :path-text :lines :inherited?) (cite/find-in-line "`envelope.clj:18-56, 81-86, 113-119, 367` (CONCLUSION)"))))
    (is (= [[113 113] [628 657]] (mapv :lines (cite/find-in-line "`object_container.clj:113, 628-657`"))))
    (is (= [[12 12]] (mapv :lines (cite/find-in-line "gate.clj:12, 3 tests ran"))) "a number before a word is prose"))
  (testing "not citations"
    (is (empty? (cite/find-in-line "example.com:8080 and localhost:3000")) "a host, not a file")
    (is (empty? (cite/find-in-line "https://github.com/x/y.clj:12")) "a URL's path")
    (is (empty? (cite/find-in-line "time 12:30, ratio 3:4")))
    (is (= ["a.clj:1"] (mapv :text (cite/find-in-line "a.clj:1:5"))) "a column after a line is not a continuation"))
  (is (= [2 1] ((juxt :line :col) (second (cite/find-citations "x.md:1\nb.clj:2"))))))

(deftest choosing-a-file
  (is (= {:path "src/a.clj"} (cite/choose (cite/candidates #{"src/a.clj" "src/b.clj"} "a.clj") "docs/d.md")))
  (is (= {:reason :not-found} (cite/choose (cite/candidates #{"src/a.clj"} "z.clj") "docs/d.md")))
  (is (= {:path "docs/b.clj"} (cite/choose (cite/candidates #{"docs/b.clj" "src/b.clj"} "b.clj") "docs/d.md"))
      "the one sharing the document's directory")
  (is (= :ambiguous (:reason (cite/choose (cite/candidates #{"x/b.clj" "y/b.clj"} "b.clj") "docs/d.md"))))
  (is (= ["src/a.clj"] (cite/candidates #{"src/a.clj" "src/aa.clj" "xsrc/a.clj"} "./src/a.clj"))
      "a whole path segment, a leading ./ dropped"))

;; ------------------------------------------------------ a scratch repository

(defn- clean-env [] (into {} (remove (fn [[k _]] (str/starts-with? k "GIT_"))) (System/getenv)))

(defn- sh! [dir env & args]
  (let [r (apply shell/sh (concat args [:dir dir :env (merge (clean-env) env)]))]
    (assert (zero? (:exit r)) (str args " " (:err r)))
    (str/trim (:out r))))

(defn- commit!
  "Write the files and commit them at unix time t; the commit's id."
  [dir t msg files]
  (doseq [[p content] files]
    (io/make-parents (io/file dir p))
    (spit (io/file dir p) content))
  (sh! dir {} "git" "add" "-A")
  (sh! dir {"GIT_AUTHOR_DATE" (str t " +0000") "GIT_COMMITTER_DATE" (str t " +0000")
            "GIT_AUTHOR_NAME" "t" "GIT_AUTHOR_EMAIL" "t@t" "GIT_COMMITTER_NAME" "t" "GIT_COMMITTER_EMAIL" "t@t"}
       "git" "commit" "-q" "-m" msg)
  (sh! dir {} "git" "rev-parse" "HEAD"))

(def a1 (str/join "\n" [";; a header" "(ns a)" "" "(defn f []" "  1)" "" "(defn g [] 2)" "(comment scratch)" ""]))

(defonce scratch
  (delay
    (let [dir (str (Files/createTempDirectory "cite-test" (make-array FileAttribute 0)))]
      (sh! dir {} "git" "init" "-q" "-b" "main")
      (let [c1 (commit! dir 1790000000 "one"
                        {"src/a.clj" a1
                         "notes.md" "# Notes\n\nsome text\n"
                         "docs/d.md" "See `src/a.clj:4-5` and `a.clj:7`, `:8`.\nAlso `a.clj:1`, `notes.md:3` and `x.clj:1`.\n"})
            _ (sh! dir {} "git" "checkout" "-q" "-b" "other")
            o1 (commit! dir 1790000500 "other one" {"src/only_other.clj" "(ns only-other)\n(defn o [] 1)\n"})
            _ (sh! dir {} "git" "checkout" "-q" "main")
            c2 (commit! dir 1790001000 "two"
                        {"docs/d.md" (str "See `src/a.clj:4-5` and `a.clj:7`, `:8`.\nAlso `a.clj:1`, `notes.md:3` and `x.clj:1`.\n"
                                          "Later `a.clj:3-7`, `:30`, `only_other.clj:2`.\n")})]
        {:dir dir :c1 c1 :c2 c2 :o1 o1}))))

(defn- found-by-text [facts]
  (into {} (for [f facts :when (= :cite/found (:k f))] [(get-in f [:v :text]) (:v f)])))

(deftest the-finder
  (let [{:keys [dir c1 c2 o1]} @scratch
        req {:repo :r :line :main :rev c2 :path "docs/d.md" :lines {:main c2 :other o1}}
        r (cite/find-step dir :r {:request req :doc (cite/doc-thing :r :main "docs/d.md")})
        by (found-by-text (:facts r))]
    (is (= 9 (count (filter #(= :cite/found (:k %)) (:facts r)))))
    (is (= {:line :main :path "docs/d.md" :commit c2 :found 9}
           (select-keys (:v (last (:facts r))) [:line :path :commit :found])) "the document's scanned fact")
    (testing "written at the commit that last changed its line"
      (is (= c1 (:written (get by "src/a.clj:4-5"))))
      (is (= c2 (:written (get by "a.clj:3-7")))))
    (testing "resolved in the document's own line of work, at that commit"
      (is (= {:line :main :path "src/a.clj" :commit c1 :file (m/file-thing :r :main "src/a.clj")}
             (:resolved (get by "src/a.clj:4-5"))))
      (is (= :clojure (:target (get by "a.clj:7"))))
      (is (= {:path-text "a.clj" :lines [8 8] :inherited? true} (select-keys (get by ":8") [:path-text :lines :inherited?])))
      (is (= :markdown (:target (get by "notes.md:3"))))
      (is (= {:reason :not-found} (:unresolved (get by "x.clj:1")))))
    (testing "else in another line of work, at its last commit before"
      (is (= {:line :other :path "src/only_other.clj" :commit o1}
             (dissoc (:resolved (get by "only_other.clj:2")) :file))))
    (testing "a document that does not read is still scanned"
      (let [bad (cite/find-step dir :r {:request (assoc req :path "nope.md") :doc (cite/doc-thing :r :main "nope.md")})]
        (is (= [:cite/scanned] (mapv :k (:facts bad))))
        (is (= :missing-path (get-in (first (:facts bad)) [:v :error])))))
    (is (= :wrong-doc-thing (:error (cite/find-step dir :r {:request req :doc :doc/nope}))))))

;; ------------------------------------------------------- binding, marking

(def run-a [:l :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000a"])
(def run-b [:l :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000b"])

(defn- reading-row
  "The reading fact of `text` as the reader tool would write it, as the exit
  shows its row: {:fid :stamp :value :e :replaces}."
  [run stamp text prev]
  (let [lines (str/split text #"\n" -1)
        rd (merge {:commit (str "k" stamp) :lines lines} (m/cut-text "src/a.clj" text (count lines)))
        f (first (:facts (m/reading-facts {:repo :r :line :main :rev (str "k" stamp) :path "src/a.clj"}
                                          (m/file-thing :r :main "src/a.clj") rd prev run)))]
    {:fid [run 0] :stamp stamp :e (:e f) :k :material/file :replaces (:replaces f) :value (:v f)}))

(defn- found [lines commit]
  {:target :clojure :lines lines :resolved {:line :main :path "src/a.clj" :commit commit
                                            :file (m/file-thing :r :main "src/a.clj")}})

(deftest the-binder
  (let [r1 (reading-row run-a 100 a1 nil)
        bind (fn [lines & [rows]] (cite/bind-step {:found (found lines "k100") :found-fid [run-b 3] :cite :cite/h1
                                                   :readings (or rows [r1]) :target :named-form}))]
    (testing "bound to the named form holding both ends, standing on it and the reading"
      (let [b (bind [4 5])
            f (first (:facts b))]
        (is (= [:cite/bound] (mapv :k (:facts b))))
        (is (= {:name "f" :lines [4 5] :fid [run-a 3]} (select-keys (get-in f [:v :form]) [:name :lines :fid])))
        (is (= [4 5] (get-in f [:v :cited])))
        (is (= [[[run-a 3] 100] [[run-a 0] 100]] (:stand-on b)))))
    (testing "not bound yet, as a fact, with the reason"
      (is (= :not-inside-one-form (get-in (bind [1 1]) [:facts 0 :v :reason])) "a comment line")
      (is (= :not-inside-one-form (get-in (bind [4 7]) [:facts 0 :v :reason])) "two forms")
      (is (= [:cite/bound "f" [4 6] [4 5]]
             (let [f (first (:facts (bind [4 6])))] [(:k f) (get-in f [:v :form :name]) (get-in f [:v :cited]) (get-in f [:v :within])]))
          "a blank line past the function is set aside: still f")
      (is (= :not-inside-one-form (get-in (bind [6 6]) [:facts 0 :v :reason])) "a blank line alone")
      (is (= [:inside-unnamed-form :clj/rich-comment]
             ((juxt :reason :kind) (get-in (bind [8 8]) [:facts 0 :v]))))
      (is (= :lines-past-end (get-in (bind [30 30]) [:facts 0 :v :reason])))
      (is (= :markdown-target (get-in (cite/bind-step {:found {:target :markdown :lines [1 1] :resolved {:path "n.md"}}
                                                       :cite :cite/h1 :readings [] :target :named-form})
                                      [:facts 0 :v :reason])))
      (is (= :markdown-target (get-in (cite/bind-step {:found {:target :markdown :lines [1 1]
                                                               :unresolved {:reason :ambiguous :candidates ["a" "b"]}}
                                                       :cite :cite/h1 :readings [] :target :named-form})
                                      [:facts 0 :v :reason]))
          "a passage citation is not bound for being one, before its path")
      (is (= :ambiguous-path (get-in (cite/bind-step {:found {:target :clojure :lines [1 1]
                                                              :unresolved {:reason :ambiguous :candidates ["a" "b"]}}
                                                      :cite :cite/h1 :readings [] :target :named-form})
                                     [:facts 0 :v :reason])))
      (is (= :unknown-bind-rule (:error (cite/bind-step {:found (found [4 5] "k100") :cite :cite/h1 :readings [r1]
                                                         :target :lines})))
          "a rule outside the menu is refused as data"))
    (testing "no reading at its commit yet: the run fails and is retried"
      (is (= :no-reading-yet (:error (bind [4 5] [])))))
    (testing "bound late: marked against the latest reading in the same act"
      (let [r2 (reading-row run-b 200 (str "\n" a1) {:fid (:fid r1) :value (:value r1)})
            b (bind [4 5] [r1 r2])]
        (is (= [:cite/bound :cite/mark] (mapv :k (:facts b))))
        (is (= {:state :moved :cited-now [5 6]} (select-keys (get-in b [:facts 1 :v]) [:state :cited-now])))
        (is (some #{[[run-b 0] 200]} (:stand-on b)) "it stands on the latest reading too")))))

(deftest the-marker
  (let [r1 (reading-row run-a 100 a1 nil)
        bound (fn [nm lines cited digest]
                {:fid [[:l :by-layer :offer #uuid "01920000-0000-7000-8000-0000000000b1"] 0] :stamp 150 :k :cite/bound
                 :e (keyword "cite" (str "h" nm))
                 :value {:cite (keyword "cite" (str "h" nm)) :cited cited
                         :form {:name nm :lines lines :digest digest}}})
        dig (fn [nm] (:digest (first (filter #(= nm (:name %)) (get-in r1 [:value :units])))))
        ;; f moves down a line; g changes; the comment stays; ns stays
        r2 (reading-row run-b 200 (str/join "\n" [";; a header" "(ns a)" "" "" "(defn f []" "  1)" "" "(defn g [] 3)"])
                        {:fid (:fid r1) :value (:value r1)})
        deps [(bound "f" [4 5] [4 5] (dig "f")) (bound "g" [7 7] [7 7] (dig "g")) (bound "ns" [2 2] [2 2] (dig "ns"))
              (bound "gone" [1 1] [1 1] "x")
              ;; a mark of f's citation from an earlier walk: the same citation, one mark
              {:fid [[:l :by-layer :offer #uuid "01920000-0000-7000-8000-0000000000b2"] 0] :stamp 160 :k :cite/mark
               :e :cite/hf :value {:cite :cite/hf :bound {:form {:name "f" :lines [4 5] :digest (dig "f")} :cited [4 5]}}}
              {:fid [[:l :by-layer :offer #uuid "01920000-0000-7000-8000-0000000000b3"] 0] :stamp 170 :k :cite/walk
               :e :file/h :value {:found 0}}]
        out (cite/check-step {:reading r2 :dependents deps :show :state-and-lines})
        marks (into {} (for [f (:facts out) :when (= :cite/mark (:k f))] [(:e f) (:v f)]))]
    (is (= 4 (count marks)) "one mark a citation; the walk fact is not a citation")
    (is (= {:state :moved :cited-now [5 6] :now [5 6]} (select-keys (marks :cite/hf) [:state :cited-now :now])))
    (is (= {:state :stale :why :changed} (select-keys (marks :cite/hg) [:state :why])))
    (is (= {:state :true} (select-keys (marks :cite/hns) [:state])))
    (is (= {:state :stale :why :gone} (select-keys (marks :cite/hgone) [:state :why])))
    (is (= [run-b 0] (get-in (marks :cite/hf) [:against :reading])))
    (is (= {:was [run-a 0] :found 6 :marked 4}
           (select-keys (:v (last (:facts out))) [:was :found :marked])) "the walk says what it found")
    (is (some #{[[run-b 0] 200]} (:stand-on out)) "the marks stand on the new reading")
    (testing "a first reading: nothing to walk, the walk still recorded"
      (let [first-walk (cite/check-step {:reading r1 :dependents [] :show :state-and-lines})]
        (is (= [:cite/walk] (mapv :k (:facts first-walk))))
        (is (nil? (get-in first-walk [:facts 0 :v :was])))))
    (testing "a walk over a gap is refused: the previous reading's walk is not among what stood on it"
      (is (= :previous-walk-missing
             (:error (cite/check-step {:reading r2 :show :state-and-lines
                                       :dependents (vec (remove #(= :cite/walk (:k %)) deps))})))))
    (testing "a snapshot that no longer opens: marked on every walk, never dropped"
      (let [walk-row {:fid [run-a 9] :stamp 140 :k :cite/walk :e :file/h :value {:found 0}}
            out (cite/check-step {:reading r2 :show :state-and-lines
                                  :dependents [walk-row {:fid [run-a 7] :stamp 150 :k :cite/bound :e :cite/hlost :erased-at 180}]})
            lost (first (:facts out))
            ;; the next walk finds the lost mark (its act stood on the reading) and the walk
            out2 (cite/check-step {:reading (assoc r2 :fid [run-b 0] :replaces [run-b 0]) :show :state-and-lines
                                   :dependents [{:fid [run-b 30] :stamp 210 :k :cite/mark :e :cite/hlost :value (:v lost)}
                                                {:fid [run-b 31] :stamp 210 :k :cite/walk :e :file/h :value {:found 2}}]})]
        (is (= {:state :stale :why :snapshot-erased} (select-keys (:v lost) [:state :why])))
        (is (= 1 (get-in (last (:facts out)) [:v :marked])))
        (is (some #{[[run-a 7] 150]} (:stand-on out)) "the lost mark stands on the row it read")
        (is (= [:cite/hlost :snapshot-erased] ((juxt :e (comp :why :v)) (first (:facts out2)))) "and again on the next walk")))
    (testing "every mark carries the binding snapshot in one shape"
      (let [row {:fid [run-a 11] :stamp 150 :k :cite/bound :e :cite/hw
                 :value {:cite :cite/hw :cited [4 6] :within [4 5] :form {:name "f" :lines [4 5] :digest (dig "f")}}}
            out (cite/check-step {:reading r2 :show :state-and-lines
                                  :dependents [row {:fid [run-a 12] :stamp 140 :k :cite/walk :e :file/h :value {:found 0}}]})]
        (is (= [4 5] (get-in (first (:facts out)) [:v :bound :within])))))
    (is (= :unknown-show-rule (:error (cite/check-step {:reading r2 :dependents [] :show :everything}))))
    (testing "the file gone, or unreadable"
      (is (= :file-gone (:why (cite/state-against {:form {:name "f"}} {:missing true}))))
      (is (= :unreadable (:why (cite/state-against {:form {:name "f"}} {:error :binary})))))))

(deftest a-miss-renders
  (let [bound {:e :cite/hm :fid [run-a 1] :stamp 5
               :value {:reading [run-a 0] :form {:name "f" :lines [1 2] :commit "0123456789abcdef"}}}
        head {:fid [run-b 0] :stamp 9 :value {:commit "fedcba9876543210"}}
        st (run/state-at-head {:bound bound :marks []} head)
        row {:found {:value {:text "a.clj:1-2" :written "0123456789abcdef" :resolved {:path "src/a.clj"}}}
             :bound bound :doc "d.md" :line 3 :col 1 :state st}
        md (run/report-markdown [row] {:heads {:main "fedcba9876543210"} :at "test"})]
    (is (= {:state :miss :why :not-marked-against-head} (select-keys st [:state :why])))
    (is (str/includes? md "MISS: not-marked-against-head") "the report renders a miss instead of stopping")))
