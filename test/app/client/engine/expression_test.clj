(ns app.client.engine.expression-test
  (:require [app.client.engine.expression :as expression]
            [clojure.test :refer [deftest is testing]]))

(defn- ev [source scope]
  (expression/evaluate (expression/compile source) scope))

(deftest precedence-and-functions
  (testing "arithmetic precedence, right-associative power, unary minus looser than power"
    (is (= 14.0 (ev "2 + 3 * 4" {})))
    (is (= 512.0 (ev "2 ^ 3 ^ 2" {})))
    (is (= -4.0 (ev "-2^2" {})))
    (is (= 4.0 (ev "(-2)^2" {})))
    (is (= 2.5 (ev "10 / 4" {}))))
  (testing "the function vocabulary"
    (is (= 3.0 (ev "max(1, 3)" {})))
    (is (= 0.5 (ev "clamp(2, 0, 0.5)" {})))
    (is (= 1.0 (ev "step(0.5, 0.7)" {})))
    (is (= 0.5 (ev "smoothstep(0, 1, 0.5)" {})))
    (is (= 1.5 (ev "mix(1, 2, 0.5)" {})))
    (is (< (Math/abs (- Math/PI (ev "pi" {}))) 1e-12))))

(deftest names-are-what-the-expression-reads
  (let [e (expression/compile "size * (1 - thinning * (1 - p))")]
    (is (= #{"size" "thinning" "p"} (:names e)))
    (is (= 10.0 (expression/evaluate e {"size" 10 "thinning" 0.6 "p" 1.0})))
    (is (= 4.0 (expression/evaluate e {"size" 10 "thinning" 0.6 "p" 0.0})))
    (is (= {"size" 10 "thinning" 0.6 "p" 0.3}
           (expression/scope-from e {:size 10 :thinning 0.6 :streamline 0.35} {"p" 0.3}))
        "a tool number the expression never names is not in its scope")))

(deftest malformed-expressions-name-their-error
  (doseq [source ["size *" "foo(1)" "(1 + 2" "1 2" "max(1)"]]
    (is (= :expression/parse
           (try (expression/compile source) nil
                (catch Exception e (:error-type (ex-data e)))))
        source))
  (is (= :expression/unknown-name
         (try (ev "size * p" {"size" 8}) nil
              (catch Exception e (:error-type (ex-data e)))))))
