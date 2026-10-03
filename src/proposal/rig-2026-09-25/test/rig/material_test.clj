(ns rig.material-test
  "Repo material as facts (PLAN-citation.md section 1), pure: names and
  kinds the way the server's adapter gives them, sections, the per-line
  tables, and the facts of readings across revisions, where a form's next
  fact replaces its last (the same-function claim). No cluster; one read of
  this repository's own history for `read-file`."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rig.material :as m]
            [rig.store.envelope :as env]))

(def run-a [:layer :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000a"])
(def run-b [:layer :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000b"])
(def run-c [:layer :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000c"])

(deftest heads-names-kinds
  (testing "the server adapter's table and resolution"
    (is (= {:head "defn" :kind :clj/fn :name "foo"} (m/form-head "(defn foo [x] x)")))
    (is (= "bar" (:name (m/form-head "(defn- ^:private bar [] 1)"))) "through metadata on the name")
    (is (= "baz" (:name (m/form-head "(def ^{:doc \"d\"} baz 1)"))))
    (is (= "old" (:name (m/form-head "^:deprecated (defn old [] 1)"))) "through metadata on the form")
    (is (= {:head "defmethod" :kind :clj/multi :name "render::circle"}
           (m/form-head "(defmethod render :circle [s] s)")) "a defmethod carries its dispatch value")
    (is (= :clj/electric-fn (:kind (m/form-head "(e/defn View [] 1)"))))
    (is (= {:head "ns" :kind :clj/ns :name "ns"} (m/form-head "(ns rig.material (:require [x]))")) "one ns a file")
    (is (= :clj/reader-cond (:kind (m/form-head "#?(:clj (defn x []))"))))
    (is (nil? (:name (m/form-head "#?(:clj (defn x []))"))) "a reader conditional is unnamed")
    (is (= {:head "comment" :kind :clj/rich-comment :name nil} (m/form-head "(comment (foo))")))
    (is (nil? (:name (m/form-head "(s/def ::thing int?)"))) "a keyword is not a name")
    (is (= {:head nil :kind :clj/other :name nil} (m/form-head "{:a 1}")))
    (is (= "real-name" (:name (m/form-head "(defn #_ignored real-name [] 1)"))) "a discard is blank")
    (is (nil? (:name (m/form-head "(defn \\( [] 1)"))) "a char is not a name")
    (is (= "x" (:name (m/form-head "(defn x \"a doc with ( and ) and \\\" inside\" [] 1)")))))
  (testing "the additions (P-C2)"
    (is (= {:head "deframaop" :kind :clj/rama-op :name "write-decided>"} (m/form-head "(deframaop write-decided> [*a] (:>))")))
    (is (= :clj/rama-fn (:kind (m/form-head "(deframafn open-row> [] 1)"))))
    (is (= {:head "defschema" :kind :clj/def-other :name "Foo"} (m/form-head "(defschema Foo {:a 1})")))
    (is (= :clj/other (:kind (m/form-head "(println 1)")))))
  (testing "never throws"
    (is (map? (m/form-head "(")))
    (is (map? (m/form-head "")))
    (is (map? (m/form-head nil)))))

(deftest duplicates
  (is (= ["a" "b" "a~2" nil "a~3"]
         (mapv :name (m/dedup-names [{:name "a"} {:name "b"} {:name "a"} {:name nil} {:name "a"}])))))

(deftest cutting
  (let [text (str/join "\n" [";; head" "(ns a)" "" "(defn f []" "  1)" "" "(comment x)" "(defn g [] 2)"])
        c (m/cut-text "src/a.clj" text 8)]
    (is (= :forms (:cut c)))
    (is (= [["ns" [2 2]] ["f" [4 5]] [nil [7 7]] ["g" [8 8]]] (mapv (juxt :name :lines) (:units c))))
    (is (= [nil 0 nil 1 1 nil 2 3] (:unit-by-line c)) "a comment and a blank hold no unit")
    (is (= [] (:sections c)))
    (is (every? #(re-matches #"[0-9a-f]{32}" (:digest %)) (:units c)))
    (is (not= (:digest (nth (:units c) 1)) (:digest (nth (:units c) 3)))))
  (let [text (str/join "\n" ["# Top" "intro" "" "## A" "a text" "" "### A1" "deep" "" "## B" "b text"])
        c (m/cut-text "doc.md" text 11)]
    (is (= :blocks (:cut c)))
    (is (every? #(= :passage (:kind %)) (:units c)))
    (is (= [[1 "Top" [1 11]] [2 "A" [4 9]] [3 "A1" [7 9]] [2 "B" [10 11]]]
           (mapv (juxt :level :title :lines) (:sections c))))
    (is (= [0 0 0 1 1 1 2 2 2 3 3] (:section-by-line c)) "the innermost section holds each line"))
  (is (= :unreadable (:error (m/cut-text "src/b.clj" "(defn f [" 1)))))

(defn- read-of
  "A `read-file` result from text, without git."
  [commit path text]
  (let [lines (str/split text #"\n" -1)
        n (count lines)]
    (merge {:commit commit :lines lines} (m/cut-text path text n))))

(def req {:repo :softland :line :main :path "src/a.clj"})
(def file (m/file-thing :softland :main "src/a.clj"))

(defn- by-k [facts k] (filterv #(= k (:k %)) facts))

(deftest readings-across-revisions
  (let [r1 (read-of "c1" "src/a.clj" (str/join "\n" ["(ns a)" "(defn f [] 1)" "(defn g [] 2)"]))
        f1 (:facts (m/reading-facts (assoc req :rev "c1") file r1 nil run-a))
        reading1 (first f1)]
    (testing "a first reading: the reading, the text, a fact a named form, nothing replaced"
      (is (= [:material/file :material/text :material/form :material/form :material/form] (mapv :k f1)))
      (is (every? #(nil? (:replaces %)) f1))
      (is (= file (:e reading1)))
      (is (= {"ns" [run-a 2] "f" [run-a 3] "g" [run-a 4]} (get-in reading1 [:v :heads])))
      (is (= [run-a 1] (get-in reading1 [:v :text-fid])))
      (is (= (m/form-thing :softland :main "src/a.clj" "f") (:e (nth f1 3))))
      (is (= [2 2] (get-in (nth f1 3) [:v :lines])))
      (is (every? env/edn-value? (map :v f1)) "every value is data the store holds"))
    (let [prev {:fid [run-a 0] :value (:v reading1)}
          ;; f moves down a line and is unchanged; g is gone; h is new
          r2 (read-of "c2" "src/a.clj" (str/join "\n" ["(ns a)" "" "(defn f [] 1)" "(defn h [] 3)"]))
          f2 (:facts (m/reading-facts (assoc req :rev "c2") file r2 prev run-b))
          reading2 (first f2)
          form (fn [nm] (first (filter #(= nm (get-in % [:v :name])) (by-k f2 :material/form))))]
      (testing "the next reading: each fact replaces its thing's last (the same-function claim)"
        (is (= [run-a 0] (:replaces reading2)))
        (is (= [run-a 1] (:replaces (second f2))) "the text replaces the text")
        (is (= [run-a 3] (:replaces (form "f"))) "f, same name same file, replaces f")
        (is (= [3 3] (get-in (form "f") [:v :lines])))
        (is (= (get-in (nth f1 3) [:v :digest]) (get-in (form "f") [:v :digest])) "same text, same digest")
        (is (nil? (:replaces (form "h"))) "a new name replaces nothing")
        (is (= {:gone true :name "g"} (select-keys (:v (form "g")) [:gone :name])) "g left: a gone fact")
        (is (= [run-a 4] (:replaces (form "g"))))
        (is (= #{"g"} (get-in reading2 [:v :gone])))
        (is (= [run-b 5] (get-in reading2 [:v :heads "g"])) "the gone fact is g's head now"))
      (let [prev2 {:fid [run-b 0] :value (:v reading2)}
            r3 (read-of "c3" "src/a.clj" (str/join "\n" ["(ns a)" "(defn g [] 22)"]))
            f3 (:facts (m/reading-facts (assoc req :rev "c3") file r3 prev2 run-c))
            form3 (fn [nm] (first (filter #(= nm (get-in % [:v :name])) (by-k f3 :material/form))))]
        (testing "a name that comes back replaces its gone fact"
          (is (= [run-b 5] (:replaces (form3 "g"))))
          (is (nil? (get-in (form3 "g") [:v :gone])))
          (is (= #{"f" "h"} (get-in (first f3) [:v :gone]))))
        (testing "a missing file: every present name is gone"
          (let [f4 (:facts (m/reading-facts (assoc req :rev "c4") file {:error :missing-path :commit "c4"} prev2 run-c))]
            (is (true? (get-in (first f4) [:v :missing])))
            (is (= #{"ns" "f" "h"} (set (map #(get-in % [:v :name]) (by-k f4 :material/form)))))
            (is (every? #(get-in % [:v :gone]) (by-k f4 :material/form)))
            (is (= #{"g" "ns" "f" "h"} (get-in (first f4) [:v :gone])))))
        (testing "an unreadable reading: no form, no gone fact, the heads carried"
          (let [f5 (:facts (m/reading-facts (assoc req :rev "c5") file {:error :unreadable :commit "c5" :lines ["(defn f ["]}
                                            prev2 run-c))]
            (is (= [:material/file :material/text] (mapv :k f5)))
            (is (= (get-in reading2 [:v :heads]) (get-in (first f5) [:v :heads])))
            (is (= :unreadable (get-in (first f5) [:v :error])))))))))

(deftest the-step-refuses-as-data
  (let [ok {:request (assoc req :rev "HEAD") :file file :prev [] :run run-a :same :name-in-file}]
    (is (= :unknown-same-rule (:error (m/reading-step "." :softland (assoc ok :same :by-hash)))))
    (is (= :bad-request (:error (m/reading-step "." :other ok))) "another repository's request")
    (is (= :wrong-file-thing (:error (m/reading-step "." :softland (assoc ok :file :file/nope)))))
    (is (= :no-run-name (:error (m/reading-step "." :softland (assoc ok :run nil)))))
    (is (= :previous-unreadable (:error (m/reading-step "." :softland (assoc ok :prev [{:fid [run-a 0] :erased-at 5}])))))))

(deftest reads-this-repository
  (let [repo (str/trim (:out (clojure.java.shell/sh "git" "rev-parse" "--show-toplevel")))
        ;; the rig's gate event, on its branch's first build of it
        r (m/read-file repo "1e63217ee28434ce50859d9c7a4158c32331f266"
                       "src/proposal/rig-2026-09-25/src/rig/store/gate_event.clj")]
    (is (= ["ns" "write-decided>" "record-or-decide>"] (mapv :name (:units r))))
    (is (= [[4 32] [34 95] [97 166]] (mapv :lines (:units r))))
    (is (= 166 (count (:lines r)) (count (:unit-by-line r))))
    (is (= :missing-path (:error (m/read-file repo "1e63217ee28434ce50859d9c7a4158c32331f266" "no/such.clj"))))))
