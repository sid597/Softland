(ns probe.push
  "What can Rama's push (foreign-proxy) watch? One in-process cluster, Rama 1.6.0.

  This is a probe, not a design. Its tables are shaped after the questions of
  the read sheet's W11 (11 October 2026): one place, a run of places, an entity
  as one value, and 'what points at a thing' kept as a set. It takes nothing
  from the store's code but the shape of one table: a top-level map from a
  place key, \"<layer> <entity> <key>\", to a row.

  Run:  clojure -M:run        (from this folder)
  It prints, for each case: whether the push opened, the value it gave, and
  for each write after that whether a push came, how long after the append
  call, and which kind of diff Rama sent."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.aggs :as aggs]
            [clojure.string :as str]))

(defmodule Probe [setup topologies]
  (declare-depot setup *writes (hash-by :layer))
  (let [s (stream-topology topologies "w")]
    ;; FLAT. The store's shape: top-level map, place key -> row.
    (declare-pstate s $$flat {String (fixed-keys-schema {:value String :stamp Long})})
    ;; NESTED. layer -> a map of place -> row that keeps its elements apart.
    (declare-pstate s $$nested {String (map-schema String
                                                   (fixed-keys-schema {:value String :stamp Long})
                                                   {:subindex? true})})
    ;; ENTITY AS ONE VALUE. "<layer> <entity>" -> a plain map of key -> value.
    (declare-pstate s $$entity {String (map-schema String String)})
    ;; POINTS AT, as one value. "<layer> <key> <target>" -> a plain set of entities.
    (declare-pstate s $$idx-plain {String (set-schema String)})
    ;; POINTS AT, elements kept apart.
    (declare-pstate s $$idx-sub {String (set-schema String {:subindex? true})})
    (<<sources s
      (source> *writes :> {:keys [*op *layer *a *b *c]})
      (<<cond
        (case> (= *op :flat-put))
        (hash-map :value *b :stamp *c :> *row)
        (local-transform> [(keypath *a) (termval *row)] $$flat)

        (case> (= *op :flat-value))
        (local-transform> [(keypath *a) :value (termval *b)] $$flat)

        (case> (= *op :flat-del))
        (local-transform> [(keypath *a) NONE>] $$flat)

        (case> (= *op :nested-put))
        (hash-map :value *b :stamp *c :> *row)
        (local-transform> [(keypath *layer *a) (termval *row)] $$nested)

        (case> (= *op :nested-del))
        (local-transform> [(keypath *layer *a) NONE>] $$nested)

        (case> (= *op :entity-put))
        (local-transform> [(keypath *a *b) (termval *c)] $$entity)

        (case> (= *op :plain-add))
        (+compound $$idx-plain {*a (aggs/+set-agg *b)})

        (case> (= *op :plain-del))
        (+compound $$idx-plain {*a (aggs/+set-remove-agg *b)})

        (case> (= *op :sub-add))
        (+compound $$idx-sub {*a (aggs/+set-agg *b)})

        (case> (= *op :sub-del))
        (+compound $$idx-sub {*a (aggs/+set-remove-agg *b)})

        (default>)
        (println "PROBE unknown op" *op)))))

;; ---------------------------------------------------------------- the hand

(def L "L1")

(defn- root-msg [^Throwable t]
  (let [chain (take-while some? (iterate #(.getCause ^Throwable %) t))
        r     (last chain)]
    (str (.getSimpleName (class r)) ": "
         (-> (or (.getMessage ^Throwable r) "") (str/split #"\n") first (#(subs % 0 (min 260 (count %))))))))

(defn- show [v]
  (let [s (pr-str v)] (if (> (count s) 300) (str (subs s 0 300) " …") s)))

(defn- diff-name [d]
  (if (nil? d) "nil" (let [s (str d)] (str (.getSimpleName (class d)) " " (subs s 0 (min 200 (count s)))))))

(defn- ms [t0 t1] (format "%.1f" (/ (double (- t1 t0)) 1e6)))

(defn run-case
  "label, the table's client, a description of the path, the path, and the
  writes to make after the push is open: pairs of a description and a thunk."
  [label ps path-text path writes]
  (println (str "\nCASE " label))
  (println (str "  path   " path-text))
  (let [seen  (atom [])
        proxy (try
                (foreign-proxy path ps
                               {:pkey L
                                :callback-fn (fn [newval diff oldval]
                                               (swap! seen conj {:t (System/nanoTime) :new newval :diff diff}))})
                (catch Throwable t
                  (println (str "  OPEN   refused — " (root-msg t)))
                  nil))]
    (when proxy
      (println (str "  OPEN   ok — value now " (show @proxy)))
      (doseq [[desc thunk] writes]
        (reset! seen [])
        (let [t0 (System/nanoTime)]
          (try (thunk) (catch Throwable t (println (str "  WRITE  failed — " (root-msg t)))))
          (Thread/sleep 500)
          (println (str "  write  " desc))
          (if (empty? @seen)
            (println "         no push")
            (doseq [{:keys [t new diff]} @seen]
              (println (str "         push " (ms t0 t) " ms after the append call · " (diff-name diff)))
              (println (str "         value now " (show new)))))))
      (try (.close ^java.io.Closeable proxy) (catch Throwable _ nil)))))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc Probe {:tasks 4 :threads 2})
    (let [m      (get-module-name Probe)
          depot  (foreign-depot ipc m "*writes")
          flat   (foreign-pstate ipc m "$$flat")
          nested (foreign-pstate ipc m "$$nested")
          entity (foreign-pstate ipc m "$$entity")
          plain  (foreign-pstate ipc m "$$idx-plain")
          sub    (foreign-pstate ipc m "$$idx-sub")
          w!     (fn [op a b c] (foreign-append! depot {:op op :layer L :a a :b b :c c} :ack))
          pk     (fn [e k] (str L " " e " " k))]
      (println "\nPROBE START — Rama" (or (some-> (Class/forName "com.rpl.rama.RamaModule") .getPackage .getImplementationVersion) "1.6.0 (from deps.edn)"))
      ;; seed
      (w! :flat-put (pk "e1" "k1") "one" 1) (w! :flat-put (pk "e1" "k2") "two" 1) (w! :flat-put (pk "e2" "k1") "other" 1)
      (w! :nested-put "e1 k1" "one" 1) (w! :nested-put "e1 k2" "two" 1) (w! :nested-put "e2 k1" "other" 1)
      (w! :entity-put (str L " e1") "k1" "one") (w! :entity-put (str L " e1") "k2" "two")
      (w! :plain-add (str L " :parent canvas-1") "block-1" nil) (w! :plain-add (str L " :parent canvas-1") "block-2" nil)
      (w! :sub-add (str L " :parent canvas-1") "block-1" nil) (w! :sub-add (str L " :parent canvas-1") "block-2" nil)
      (println "seeded. flat e1 k1 reads as" (show (foreign-select-one [(keypath (pk "e1" "k1"))] flat {:pkey L})))

      ;; ---------------- FLAT: the store's shape
      (run-case "F1  flat · one place" flat
                "[(keypath \"L1 e1 k1\")]" [(keypath (pk "e1" "k1"))]
                [["put the same place again, whole row" #(w! :flat-put (pk "e1" "k1") "one-b" 2)]
                 ["set only :value inside the row"      #(w! :flat-value (pk "e1" "k1") "one-c" nil)]
                 ["put ANOTHER place of the entity"     #(w! :flat-put (pk "e1" "k2") "two-b" 2)]
                 ["remove the place"                    #(w! :flat-del (pk "e1" "k1") nil nil)]
                 ["put the place back"                  #(w! :flat-put (pk "e1" "k1") "one-d" 3)]])

      (run-case "F2  flat · a run of places = one entity" flat
                "[(sorted-map-range \"L1 e1 \" \"L1 e1!\")]" [(sorted-map-range (str L " e1 ") (str L " e1!"))]
                [["change a place inside the run"       #(w! :flat-put (pk "e1" "k1") "one-e" 4)]
                 ["set only :value of a place inside"   #(w! :flat-value (pk "e1" "k2") "two-c" nil)]
                 ["a NEW place inside the run"          #(w! :flat-put (pk "e1" "k9") "nine" 4)]
                 ["a place OUTSIDE the run (entity e2)" #(w! :flat-put (pk "e2" "k1") "other-b" 4)]
                 ["remove a place inside the run"       #(w! :flat-del (pk "e1" "k9") nil nil)]])

      (run-case "F3  flat · a run that is empty when the push opens" flat
                "[(sorted-map-range \"L1 e7 \" \"L1 e7!\")]" [(sorted-map-range (str L " e7 ") (str L " e7!"))]
                [["the first place of a new entity"     #(w! :flat-put (pk "e7" "k1") "born" 5)]])

      (run-case "F4  flat · the values of a run, gathered" flat
                "[(subselect (sorted-map-range \"L1 e1 \" \"L1 e1!\") MAP-VALS)]"
                [(subselect (sorted-map-range (str L " e1 ") (str L " e1!")) MAP-VALS)]
                [["change a place inside the run"       #(w! :flat-put (pk "e1" "k1") "one-f" 6)]])

      (run-case "F5  flat · how many places a run holds" flat
                "[(sorted-map-range \"L1 e1 \" \"L1 e1!\") (view count)]"
                [(sorted-map-range (str L " e1 ") (str L " e1!")) (view count)]
                [["a NEW place inside the run"          #(w! :flat-put (pk "e1" "k8") "eight" 6)]])

      (run-case "F6  flat · the whole table" flat
                "STAY" STAY
                [["change any place"                    #(w! :flat-put (pk "e1" "k1") "one-g" 7)]])

      ;; ---------------- NESTED: a layer's places kept apart under the layer
      (run-case "N1  nested · one place" nested
                "[(keypath \"L1\" \"e1 k1\")]" [(keypath L "e1 k1")]
                [["change that place"                   #(w! :nested-put "e1 k1" "one-b" 2)]
                 ["change another place"                #(w! :nested-put "e1 k2" "two-b" 2)]])

      (run-case "N2  nested · the whole layer (elements kept apart)" nested
                "[(keypath \"L1\")]" [(keypath L)]
                [["change a place"                      #(w! :nested-put "e1 k1" "one-c" 3)]])

      (run-case "N3  nested · a run of places = one entity" nested
                "[(keypath \"L1\") (sorted-map-range \"e1 \" \"e1!\")]" [(keypath L) (sorted-map-range "e1 " "e1!")]
                [["change a place inside the run"       #(w! :nested-put "e1 k1" "one-d" 4)]
                 ["a NEW place inside the run"          #(w! :nested-put "e1 k9" "nine" 4)]
                 ["a place OUTSIDE the run (entity e2)" #(w! :nested-put "e2 k1" "other-b" 4)]
                 ["remove a place inside the run"       #(w! :nested-del "e1 k9" nil nil)]])

      ;; ---------------- ENTITY AS ONE VALUE
      (run-case "E1  an entity held as one value" entity
                "[(keypath \"L1 e1\")]" [(keypath (str L " e1"))]
                [["change one key of the entity"        #(w! :entity-put (str L " e1") "k1" "one-b")]
                 ["a NEW key of the entity"             #(w! :entity-put (str L " e1") "k9" "nine")]])

      ;; ---------------- WHAT POINTS AT A THING
      (run-case "P1  points-at · the set held as one value" plain
                "[(keypath \"L1 :parent canvas-1\")]" [(keypath (str L " :parent canvas-1"))]
                [["a block is added to the canvas"      #(w! :plain-add (str L " :parent canvas-1") "block-3" nil)]
                 ["a block is taken off the canvas"     #(w! :plain-del (str L " :parent canvas-1") "block-1" nil)]])

      (run-case "S1  points-at · the set with elements kept apart, whole" sub
                "[(keypath \"L1 :parent canvas-1\")]" [(keypath (str L " :parent canvas-1"))]
                [["a block is added to the canvas"      #(w! :sub-add (str L " :parent canvas-1") "block-3" nil)]])

      (run-case "S2  points-at · elements kept apart, a run of the set" sub
                "[(keypath \"L1 :parent canvas-1\") (sorted-set-range \"\" \"~\")]"
                [(keypath (str L " :parent canvas-1")) (sorted-set-range "" "~")]
                [["a block is added to the canvas"      #(w! :sub-add (str L " :parent canvas-1") "block-4" nil)]
                 ["a block is taken off the canvas"     #(w! :sub-del (str L " :parent canvas-1") "block-1" nil)]])

      (run-case "S3  points-at · elements kept apart, how many" sub
                "[(keypath \"L1 :parent canvas-1\") (view count)]"
                [(keypath (str L " :parent canvas-1")) (view count)]
                [["a block is added to the canvas"      #(w! :sub-add (str L " :parent canvas-1") "block-5" nil)]])

      (println "\nPROBE END")))
  (shutdown-agents)
  (System/exit 0))
