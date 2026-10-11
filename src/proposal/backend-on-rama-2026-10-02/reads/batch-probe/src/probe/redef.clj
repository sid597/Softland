(ns probe.redef
  "The run for B5: can a check make an attempt of a module fail without a
  hook in the module's source, by putting another function in the place of a
  plain Clojure function that the topology calls?

  Four modules, each one depot and one microbatch topology that writes each
  thing to a PState after it called plain functions of this namespace:

    RedefClosure calls `direct-closure` from the dataflow. Before the launch
                 the function is replaced by one that closes over an atom.
    RedefBefore  calls `direct-before` from the dataflow. The function is
                 replaced BEFORE the module is launched, by a function that
                 is itself held by a var and closes over nothing.
    RedefAfter   calls `direct-after` from the dataflow, and `outer-after`,
                 which calls `inner-after` from plain Clojure code. Both are
                 replaced AFTER the launch, one at a time.
    RedefWith    calls `direct-with` from the dataflow. The module is
                 launched inside `with-redefs`, and a thing is sent inside
                 it and another after it ended.

  A replacement throws on its first call and gives its argument from then
  on. Each call of a replacement says so, and `note!` says each attempt, so
  the log shows whether the replacement ran and whether the batch was tried
  again. Which replacements have thrown is kept in `thrown`, an atom that a
  var holds: the first run of this probe showed that Rama refuses to launch
  a module whose dataflow calls a function that closes over an atom.

  Run:  clojure -M:redef   (from this folder; the log is logs/redef.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u]))

(defn direct-closure "Called from RedefClosure's dataflow. Gives its argument." [v] v)
(defn direct-before "Called from RedefBefore's dataflow. Gives its argument." [v] v)
(defn direct-after "Called from RedefAfter's dataflow. Gives its argument." [v] v)
(defn inner-after "Called from `outer-after`, in plain Clojure code. Gives its argument." [v] v)
(defn outer-after "Called from RedefAfter's dataflow. Calls `inner-after`." [v] (inner-after v))
(defn direct-with "Called from RedefWith's dataflow. Gives its argument." [v] v)

(defn note!
  "The dataflow calls it for each thing, before the plain functions. Takes
  the module's label, the batch id and the thing's value. Says them: one
  line for each attempt that got the thing."
  [label batch v]
  (u/say "INSIDE " label ": an attempt of batch" batch "got" (pr-str v))
  nil)

(defonce ^{:doc "The names of the replacements that have thrown. Memory of this process."}
  thrown (atom #{}))

(defn throw-once!
  "Takes the name of a replaced function. Throws the first time it is called
  with that name and gives nil from then on, and says which of the two it
  did."
  [fn-name]
  (let [[before _] (swap-vals! thrown conj fn-name)]
    (if (contains? before fn-name)
      (u/say "INSIDE  the REPLACEMENT of" fn-name "ran: it gives its argument")
      (do (u/say "INSIDE  the REPLACEMENT of" fn-name "ran: it throws, this once")
          (throw (ex-info (str "probe: the replacement of " fn-name " throws once") {}))))))

(defn closure-over-an-atom
  "Gives a function that closes over an atom of its own: the kind of
  replacement that a check would write first."
  []
  (let [calls (atom 0)]
    (fn [v] (swap! calls inc) v)))

(defn replacement-of-direct-before "Throws once, then gives its argument." [v] (throw-once! "direct-before") v)
(defn replacement-of-direct-after "Throws once, then gives its argument." [v] (throw-once! "direct-after") v)
(defn replacement-of-inner-after "Throws once, then gives its argument." [v] (throw-once! "inner-after") v)
(defn replacement-of-direct-with "Throws once, then gives its argument." [v] (throw-once! "direct-with") v)

(defmodule RedefClosure [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$seen {String Object})
    (<<sources mb
      (source> *things :> %things)
      (%things :> {:keys [*k *v]})
      (direct-closure *v :> *out)
      (local-transform> [(keypath *k) (termval *out)] $$seen))))

(defmodule RedefBefore [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$seen {String Object})
    (<<sources mb
      (source> *things :> %things)
      (%things :> {:keys [*k *v]})
      (ops/current-microbatch-id :> *mbid)
      (note! "RedefBefore" *mbid *v)
      (direct-before *v :> *out)
      (local-transform> [(keypath *k) (termval *out)] $$seen))))

(defmodule RedefAfter [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$seen {String Object})
    (<<sources mb
      (source> *things :> %things)
      (%things :> {:keys [*k *v]})
      (ops/current-microbatch-id :> *mbid)
      (note! "RedefAfter" *mbid *v)
      (direct-after *v :> *mid)
      (outer-after *mid :> *out)
      (local-transform> [(keypath *k) (termval *out)] $$seen))))

(defmodule RedefWith [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$seen {String Object})
    (<<sources mb
      (source> *things :> %things)
      (%things :> {:keys [*k *v]})
      (ops/current-microbatch-id :> *mbid)
      (note! "RedefWith" *mbid *v)
      (direct-with *v :> *out)
      (local-transform> [(keypath *k) (termval *out)] $$seen))))

(defn- send-and-wait!
  "Takes the cluster, a module and a value. Appends one thing with that
  value and waits up to 30 s for its row. Says whether the row came."
  [ipc module v]
  (let [m (get-module-name module)
        depot (foreign-depot ipc m "*things")
        seen (foreign-pstate ipc m "$$seen")]
    (u/say "OUTSIDE a thing with the value" (pr-str v) "is appended")
    (foreign-append! depot {:k "k" :v v} :append-ack)
    (u/say "OUTSIDE its row came:"
           (boolean (u/wait-until #(= v (foreign-select-one (keypath "k") seen)) 30000)))
    (Thread/sleep 300)))

(defn -main [& _]
  (u/open-log! "logs/redef.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (u/say "RUN     Rama 1.6.0, in-process, each module 4 tasks and 2 threads")

      (u/say "RUN     ---- (a0) replaced BEFORE the launch by a function that closes over an atom ----")
      (alter-var-root #'direct-closure (constantly (closure-over-an-atom)))
      (u/say "OUTSIDE the launch gave"
             (pr-str (u/within 60000 #(do (rtest/launch-module! ipc RedefClosure {:tasks 4 :threads 2}) :launched))))

      (u/say "RUN     ---- (a) replaced BEFORE the launch by a function that a var holds ----")
      (alter-var-root #'direct-before (constantly replacement-of-direct-before))
      (rtest/launch-module! ipc RedefBefore {:tasks 4 :threads 2})
      (send-and-wait! ipc RedefBefore "a-1")
      (send-and-wait! ipc RedefBefore "a-2")

      (u/say "RUN     ---- (b) replaced AFTER the launch ----")
      (rtest/launch-module! ipc RedefAfter {:tasks 4 :threads 2})
      (send-and-wait! ipc RedefAfter "b-1, nothing replaced")
      (alter-var-root #'direct-after (constantly replacement-of-direct-after))
      (u/say "OUTSIDE direct-after, which the DATAFLOW calls, is now replaced")
      (send-and-wait! ipc RedefAfter "b-2, after direct-after was replaced")
      (alter-var-root #'inner-after (constantly replacement-of-inner-after))
      (u/say "OUTSIDE inner-after, which PLAIN CODE calls, is now replaced")
      (send-and-wait! ipc RedefAfter "b-3, after inner-after was replaced")
      (send-and-wait! ipc RedefAfter "b-4")
      (u/say "RUN     ---- (b) goes on: the module is updated with itself, Rama's way to restart its workers in a test ----")
      (u/say "OUTSIDE update-module! gave" (pr-str (u/within 90000 #(do (rtest/update-module! ipc RedefAfter) :updated))))
      (send-and-wait! ipc RedefAfter "b-5, after the restart")
      (send-and-wait! ipc RedefAfter "b-6")

      (u/say "RUN     ---- (c) with-redefs around the launch and one thing; another thing after it ended ----")
      (with-redefs [direct-with replacement-of-direct-with]
        (rtest/launch-module! ipc RedefWith {:tasks 4 :threads 2})
        (send-and-wait! ipc RedefWith "c-1, inside with-redefs")
        (send-and-wait! ipc RedefWith "c-2, inside with-redefs"))
      (u/say "OUTSIDE with-redefs has ended: the first function is back in its var")
      (send-and-wait! ipc RedefWith "c-3, after with-redefs")
      (u/say "RUN     END"))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
