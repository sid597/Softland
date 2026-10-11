(ns probe.push2
  "Second run, after push.clj showed that one push can watch a run of places.
  Asks what the first run left open:
    SIZE     a run of 5,000 and of 20,000 places: how long to open, and does one
             change still come as one small diff?
    SMALL IN BIG   a run of 3 places inside a table of 25,000: does opening it
             cost by the run or by the table?
    WINDOW   the first 3 places of a run, live: what happens when a new place
             sorts into the window?
    ALL      a range that covers everything on one task
    BATCH    the same run on a table that a microbatch topology writes (the
             base's kind)

  Run:  clojure -M:run2"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [clojure.string :as str]))

(defmodule Probe2 [setup topologies]
  (declare-depot setup *writes (hash-by :layer))
  (declare-depot setup *mb-writes (hash-by :layer))
  (let [s (stream-topology topologies "w")]
    (declare-pstate s $$flat {String (fixed-keys-schema {:value String :stamp Long})})
    (declare-pstate s $$small {String String})
    (<<sources s
      (source> *writes :> {:keys [*op *layer *a *b *c]})
      (<<cond
        (case> (= *op :flat-put))
        (hash-map :value *b :stamp *c :> *row)
        (local-transform> [(keypath *a) (termval *row)] $$flat)

        (case> (= *op :flat-del))
        (local-transform> [(keypath *a) NONE>] $$flat)

        ;; *a is a key prefix, *b the first number, *c the number after the last
        (case> (= *op :flat-bulk))
        (ops/range> *b *c :> *i)
        (format "%s%06d" *a *i :> *place)
        (hash-map :value "v" :stamp 1 :> *row)
        (local-transform> [(keypath *place) (termval *row)] $$flat)

        (case> (= *op :small-put))
        (local-transform> [(keypath *a) (termval *b)] $$small)

        (default>)
        (println "PROBE unknown op" *op))))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$mb-flat {String (fixed-keys-schema {:value String :stamp Long})})
    (<<sources mb
      (source> *mb-writes :> %microbatch)
      (%microbatch :> {:keys [*a *b *c]})
      (hash-map :value *b :stamp *c :> *row)
      (local-transform> [(keypath *a) (termval *row)] $$mb-flat))))

(def L "L1")

(defn- root-msg [^Throwable t]
  (let [r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))]
    (str (.getSimpleName (class r)) ": "
         (-> (or (.getMessage ^Throwable r) "") (str/split #"\n") first (#(subs % 0 (min 260 (count %))))))))

(defn- show [v]
  (cond
    (and (coll? v) (> (count v) 6)) (str "a " (.getSimpleName (class v)) " of " (count v) " entries")
    :else (let [s (pr-str v)] (if (> (count s) 300) (str (subs s 0 300) " …") s))))

(defn- diff-name [d]
  (if (nil? d) "nil" (let [s (str d)] (str (.getSimpleName (class d)) " " (subs s 0 (min 160 (count s)))
                                           (when (> (count s) 160) (str " … (" (count s) " chars)"))))))

(defn- ms [t0 t1] (format "%.1f" (/ (double (- t1 t0)) 1e6)))

(defn run-case [label ps path-text path wait-ms writes]
  (println (str "\nCASE " label))
  (println (str "  path   " path-text))
  (let [seen  (atom [])
        t-open (System/nanoTime)
        proxy (try
                (foreign-proxy path ps
                               {:pkey L
                                :callback-fn (fn [newval diff oldval]
                                               (swap! seen conj {:t (System/nanoTime) :new newval :diff diff}))})
                (catch Throwable t
                  (println (str "  OPEN   refused — " (root-msg t)))
                  nil))]
    (when proxy
      (println (str "  OPEN   ok in " (ms t-open (System/nanoTime)) " ms — value now " (show @proxy)))
      (doseq [[desc thunk] writes]
        (reset! seen [])
        (let [t0 (System/nanoTime)]
          (try (thunk) (catch Throwable t (println (str "  WRITE  failed — " (root-msg t)))))
          (Thread/sleep (long wait-ms))
          (println (str "  write  " desc))
          (if (empty? @seen)
            (println (str "         no push within " wait-ms " ms"))
            (doseq [{:keys [t new diff]} @seen]
              (println (str "         push " (ms t0 t) " ms after the append call · " (diff-name diff)))
              (println (str "         value now " (show new)))))))
      (try (.close ^java.io.Closeable proxy) (catch Throwable _ nil)))))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc Probe2 {:tasks 4 :threads 2})
    (let [m       (get-module-name Probe2)
          depot   (foreign-depot ipc m "*writes")
          mbdepot (foreign-depot ipc m "*mb-writes")
          flat    (foreign-pstate ipc m "$$flat")
          small   (foreign-pstate ipc m "$$small")
          mbflat  (foreign-pstate ipc m "$$mb-flat")
          w!      (fn [op a b c] (foreign-append! depot {:op op :layer L :a a :b b :c c} :ack))
          mb!     (fn [a b c] (foreign-append! mbdepot {:layer L :a a :b b :c c} :append-ack))
          timed   (fn [what f] (let [t0 (System/nanoTime) r (f)] (println (str "  " what " took " (ms t0 (System/nanoTime)) " ms")) r))]
      (println "\nPROBE 2 START")
      ;; an entity of 3 places, and one of 5,000
      (w! :flat-put (str L " e1 k1") "one" 1) (w! :flat-put (str L " e1 k2") "two" 1) (w! :flat-put (str L " e1 k3") "three" 1)
      (timed "seeding 5,000 places of entity big" #(w! :flat-bulk (str L " big k") 0 5000))
      (println "  the run of big counts" (foreign-select-one [(sorted-map-range (str L " big ") (str L " big!")) (view count)] flat {:pkey L}))

      (run-case "Z1  a run of 5,000 places" flat
                "[(sorted-map-range \"L1 big \" \"L1 big!\")]" [(sorted-map-range (str L " big ") (str L " big!"))] 500
                [["change one place of the 5,000" #(w! :flat-put (str L " big k002500") "changed" 2)]
                 ["a NEW place in the run"        #(w! :flat-put (str L " big k9new") "new" 2)]
                 ["remove that place"             #(w! :flat-del (str L " big k9new") nil nil)]])

      (timed "seeding 15,000 more places of entity big" #(w! :flat-bulk (str L " big k") 5000 20000))
      (println "  the run of big counts" (foreign-select-one [(sorted-map-range (str L " big ") (str L " big!")) (view count)] flat {:pkey L}))

      (run-case "Z2  a run of 20,000 places" flat
                "[(sorted-map-range \"L1 big \" \"L1 big!\")]" [(sorted-map-range (str L " big ") (str L " big!"))] 500
                [["change one place of the 20,000" #(w! :flat-put (str L " big k012500") "changed" 3)]])

      (run-case "Z3  a run of 3 places, in a table that now holds 20,003" flat
                "[(sorted-map-range \"L1 e1 \" \"L1 e1!\")]" [(sorted-map-range (str L " e1 ") (str L " e1!"))] 500
                [["change one of the 3"            #(w! :flat-put (str L " e1 k2") "two-b" 2)]
                 ["change one of the 20,000 others" #(w! :flat-put (str L " big k000007") "changed" 3)]])

      (run-case "Z4  a window: the first 3 places of the big run" flat
                "[(sorted-map-range-from \"L1 big \" {:max-amt 3})]" [(sorted-map-range-from (str L " big ") {:max-amt 3})] 500
                [["change a place inside the window"          #(w! :flat-put (str L " big k000001") "changed" 4)]
                 ["a NEW place that sorts INTO the window"    #(w! :flat-put (str L " big a-first") "first" 4)]
                 ["change a place far outside the window"     #(w! :flat-put (str L " big k019000") "changed" 4)]])
      (println "  after Z4, a fresh read of the same window gives"
               (pr-str (keys (foreign-select-one [(sorted-map-range-from (str L " big ") {:max-amt 3})] flat {:pkey L}))))

      (w! :small-put "a" "1" nil) (w! :small-put "b" "2" nil)
      (run-case "Z5  a range that covers everything on the task" small
                "[(sorted-map-range \"\" \"~\")]" [(sorted-map-range "" "~")] 500
                [["change a key"   #(w! :small-put "a" "1b" nil)]
                 ["a NEW key"      #(w! :small-put "c" "3" nil)]])

      (mb! (str L " e1 k1") "one" 1) (mb! (str L " e1 k2") "two" 1)
      (Thread/sleep 3000)
      (run-case "Z6  a run on a table that a microbatch topology writes" mbflat
                "[(sorted-map-range \"L1 e1 \" \"L1 e1!\")]" [(sorted-map-range (str L " e1 ") (str L " e1!"))] 3000
                [["change a place inside the run"  #(mb! (str L " e1 k1") "one-b" 2)]
                 ["a NEW place inside the run"     #(mb! (str L " e1 k9") "nine" 2)]
                 ["a place OUTSIDE the run"        #(mb! (str L " e2 k1") "other" 2)]])

      (println "\nPROBE 2 END")))
  (shutdown-agents)
  (System/exit 0))
