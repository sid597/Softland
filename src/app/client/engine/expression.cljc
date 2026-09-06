(ns app.client.engine.expression
  "Run a tool's arithmetic written as data.

   Input: an expression string over named numbers, and a scope map. Output:
   a compiled expression that reports the names it reads, and a number when
   evaluated. No retained state.

   A tool's behaviour (how pressure becomes width, how a taper falls off) is
   a function; a function is data only if code below the waist can execute
   it. This is that executor for arithmetic: `+ - * / ^`, parentheses, a
   fixed set of functions, `pi`, and any name the scope supplies. The names
   an expression reads are the only fields it consumes, so a caller can build
   its scope from exactly those and report the rest as unread.

   Folder map: README.md."
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]))

(def ^:private token-pattern
  #"[A-Za-z_][A-Za-z0-9_]*|[0-9]*\.?[0-9]+(?:[eE][+-]?[0-9]+)?|\*\*|[-+*/^(),]")

(def functions
  "Name → [arity fn]. The vocabulary an expression may call."
  {"min" [2 min] "max" [2 max] "abs" [1 #(Math/abs (double %))]
   "sqrt" [1 #(Math/sqrt (double %))] "pow" [2 #(Math/pow (double %1) (double %2))]
   "sin" [1 #(Math/sin (double %))] "cos" [1 #(Math/cos (double %))]
   "exp" [1 #(Math/exp (double %))] "floor" [1 #(Math/floor (double %))]
   "clamp" [3 (fn [x a b] (min b (max a x)))]
   "step" [2 (fn [edge x] (if (< x edge) 0.0 1.0))]
   "smoothstep" [3 (fn [a b x]
                     (let [t (min 1.0 (max 0.0 (/ (- x a) (- b a))))]
                       (* t t (- 3.0 (* 2.0 t)))))]
   "mix" [3 (fn [a b t] (+ a (* (- b a) t)))]})

(defn- fail
  "Message and data → throws a named parse error."
  [message data]
  (throw (ex-info message (assoc data :error-type :expression/parse))))

(defn- number-token?
  [token]
  (re-matches #"[0-9]*\.?[0-9]+(?:[eE][+-]?[0-9]+)?" token))

(defn- name-token?
  [token]
  (re-matches #"[A-Za-z_][A-Za-z0-9_]*" token))

(declare parse-expr)

(defn- parse-atom
  "Tokens and index → [node next-index]. A node is a vector AST."
  [tokens i]
  (let [token (get tokens i)]
    (cond
      (nil? token) (fail "Expression ends early" {:index i})
      (= "(" token)
      (let [[node j] (parse-expr tokens (inc i))]
        (when-not (= ")" (get tokens j)) (fail "Missing )" {:index j}))
        [node (inc j)])
      (number-token? token)
      [[:number #?(:clj (Double/parseDouble token) :cljs (js/parseFloat token))]
       (inc i)]
      (name-token? token)
      (if (= "(" (get tokens (inc i)))
        (let [[args j]
              (loop [j (+ i 2) args []]
                (if (= ")" (get tokens j))
                  [args j]
                  (let [[arg k] (parse-expr tokens j)
                        args (conj args arg)]
                    (case (get tokens k)
                      "," (recur (inc k) args)
                      ")" [args k]
                      (fail (str "Missing ) after " token) {:index k})))))]
          (when-not (contains? functions token)
            (fail (str "Unknown function " token) {:name token}))
          (let [[arity] (functions token)]
            (when-not (= arity (count args))
              (fail (str token " takes " arity " arguments")
                    {:name token :given (count args)})))
          [[:call token args] (inc j)])
        (if (= "pi" token)
          [[:number Math/PI] (inc i)]
          [[:name token] (inc i)]))
      :else (fail (str "Unexpected " token) {:index i}))))

(defn- parse-unary
  [tokens i]
  (case (get tokens i)
    "-" (let [[node j] (parse-unary tokens (inc i))] [[:neg node] j])
    "+" (parse-unary tokens (inc i))
    (let [[base j] (parse-atom tokens i)]
      (if (contains? #{"^" "**"} (get tokens j))
        (let [[exponent k] (parse-unary tokens (inc j))]
          [[:pow base exponent] k])
        [base j]))))

(defn- parse-binary
  "Left-associative level: parse operands with `below`, join on `ops`."
  [below ops tokens i]
  (loop [[left j] (below tokens i)]
    (if-let [op (ops (get tokens j))]
      (let [[right k] (below tokens (inc j))]
        (recur [[op left right] k]))
      [left j])))

(defn- parse-term [tokens i]
  (parse-binary parse-unary {"*" :mul "/" :div} tokens i))

(defn- parse-expr [tokens i]
  (parse-binary parse-term {"+" :add "-" :sub} tokens i))

(defn- names-in
  "AST → set of the names it reads."
  [node]
  (case (first node)
    :name #{(second node)}
    :number #{}
    :call (reduce into #{} (map names-in (nth node 2)))
    (reduce into #{} (map names-in (rest node)))))

(defn- evaluate-node
  "AST and scope (string name → number) → number; an unknown name throws."
  [node scope]
  (case (first node)
    :number (second node)
    :name (let [value (get scope (second node))]
            (when-not (number? value)
              (throw (ex-info (str "Unknown name " (second node))
                              {:error-type :expression/unknown-name
                               :name (second node)})))
            value)
    :neg (- (evaluate-node (second node) scope))
    :add (+ (evaluate-node (second node) scope) (evaluate-node (nth node 2) scope))
    :sub (- (evaluate-node (second node) scope) (evaluate-node (nth node 2) scope))
    :mul (* (evaluate-node (second node) scope) (evaluate-node (nth node 2) scope))
    :div (/ (evaluate-node (second node) scope) (double (evaluate-node (nth node 2) scope)))
    :pow (Math/pow (double (evaluate-node (second node) scope))
                   (double (evaluate-node (nth node 2) scope)))
    :call (let [[_ f] (functions (second node))]
            (apply f (map #(evaluate-node % scope) (nth node 2))))))

(defn compile
  "Expression string → {:ast :names}; a malformed expression throws
   :expression/parse.

   Names are strings; `pi` is a constant, function names are not names.
   Precedence: unary minus binds looser than `^`, so `-p^2` is `-(p^2)`."
  [source]
  (let [tokens (vec (re-seq token-pattern (str source)))
        [ast j] (parse-expr tokens 0)]
    (when (< j (count tokens))
      (fail (str "Unexpected " (get tokens j)) {:index j}))
    {:ast ast :names (names-in ast) :source (str source)}))

(defn evaluate
  "Compiled expression and scope (string → number) → number.

   Division by zero follows the platform's double arithmetic; the caller
   decides what a non-finite width means."
  [{:keys [ast]} scope]
  (double (evaluate-node ast scope)))

(defn scope-from
  "Compiled expression, a map with keyword keys, and extra string → number
   entries → the scope holding only the names the expression reads.

   A number the expression never names is not consumed by it; callers report
   such fields as unread rather than silently accepting them."
  [{:keys [names]} record extra]
  (reduce (fn [scope name]
            (if (contains? extra name)
              (assoc scope name (get extra name))
              (let [value (get record (keyword name))]
                (cond-> scope (number? value) (assoc name value)))))
          {}
          names))

(defn describe
  "Compiled expression → its source with the names it reads, for logs."
  [{:keys [source names]}]
  (str source " over " (str/join ", " (sort names))))
