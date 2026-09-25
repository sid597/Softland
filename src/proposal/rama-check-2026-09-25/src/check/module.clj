(ns check.module
  "Two throwaway modules for checking the Rama claims the formal model assumes.

   StampModule: a stream topology that keeps $$stamps {key -> stamp}. It stands
   in for the one-owner stream store whose stamps a microbatch gate would need
   to learn.

   CheckModule: one stream topology 's' with two sources (retry :individual and
   retry :none) and one microbatch topology 'mb'. Every event is a two-hop
   write: on partition A it ticks a per-partition clock, appends an entry with
   the stamp, and increments a count; then it hops to partition B and appends
   there. Failure and blocking points are named :a-mid (between the two writes
   on A) and :b (on B, before B's write) for the stream, and :mb-b for the
   microbatch. The microbatch event also reads the stream topology's clock in
   the same module and the mirror of StampModule's $$stamps, to see what a
   micro gate can learn about what an offer stood on."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [check.faults :as faults]))

(defmodule StampModule [setup topologies]
  (declare-depot setup *stamp-depot (hash-by :k))
  (let [s (stream-topology topologies "st")]
    (declare-pstate s $$stamps {String Long})
    (<<sources s
      (source> *stamp-depot :> {:keys [*k *stamp]})
      (|hash *k)
      (local-transform> [(keypath *k) (termval *stamp)] $$stamps))))

(def stamp-module-name (get-module-name StampModule))

(defmodule CheckModule [setup topologies]
  (declare-depot setup *s-retry (hash-by :ka))
  (declare-depot setup *s-none (hash-by :ka))
  (declare-depot setup *mb-depot (hash-by :ka))
  (declare-depot setup *vis-depot (hash-by :ka))
  (mirror-pstate setup $$stamps stamp-module-name "$$stamps")

  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$s-a {String (vector-schema Object)})
    (declare-pstate s $$s-a-count {String Long})
    (declare-pstate s $$s-clock {Long Long})
    (declare-pstate s $$s-b {String (vector-schema Object)})
    (<<sources s
      ;; --- source 1: at-least-once (default retry mode) ---
      (source> *s-retry {:retry-mode :individual} :> {:keys [*id *ka *kb]})
      (|hash *ka)
      (faults/attempt! *id :a)
      (ops/current-task-id :> *ta)
      (local-transform> [(keypath *ta) (nil->val 0) (term inc)] $$s-clock)
      (local-select> (keypath *ta) $$s-clock :> *stamp)
      (hash-map :id *id :stamp *stamp :task *ta :> *entry-a)
      (local-transform> [(keypath *ka) (nil->val []) AFTER-ELEM (termval *entry-a)] $$s-a)
      (faults/maybe-fail! *id :a-mid)
      (local-transform> [(keypath *ka) (nil->val 0) (term inc)] $$s-a-count)
      (|hash *kb)
      (faults/attempt! *id :b)
      (faults/maybe-block! *id :b)
      (faults/maybe-fail! *id :b)
      (ops/current-task-id :> *tb)
      (hash-map :id *id :stamp *stamp :from *ka :task *tb :> *entry-b)
      (local-transform> [(keypath *kb) (nil->val []) AFTER-ELEM (termval *entry-b)] $$s-b)

      ;; --- source 2: at-most-once, to look at what a failure leaves behind ---
      (source> *s-none {:retry-mode :none} :> {:keys [*id *ka *kb]})
      (|hash *ka)
      (faults/attempt! *id :a)
      (ops/current-task-id :> *ta)
      (local-transform> [(keypath *ta) (nil->val 0) (term inc)] $$s-clock)
      (local-select> (keypath *ta) $$s-clock :> *stamp)
      (hash-map :id *id :stamp *stamp :task *ta :> *entry-a)
      (local-transform> [(keypath *ka) (nil->val []) AFTER-ELEM (termval *entry-a)] $$s-a)
      (faults/maybe-fail! *id :a-mid)
      (local-transform> [(keypath *ka) (nil->val 0) (term inc)] $$s-a-count)
      (|hash *kb)
      (faults/attempt! *id :b)
      (faults/maybe-block! *id :b)
      (faults/maybe-fail! *id :b)
      (ops/current-task-id :> *tb)
      (hash-map :id *id :stamp *stamp :from *ka :task *tb :> *entry-b)
      (local-transform> [(keypath *kb) (nil->val []) AFTER-ELEM (termval *entry-b)] $$s-b)))

  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$mb-a {String (vector-schema Object)})
    (declare-pstate mb $$mb-clock {Long Long})
    (declare-pstate mb $$mb-b {String (vector-schema Object)})
    (declare-pstate mb $$mb-vis {String Long})
    (declare-pstate mb $$mb-fill {String (map-schema Long Long {:subindex? true})})
    (<<sources mb
      ;; --- two-hop write, with reads of what the offer stood on ---
      (source> *mb-depot :> %mb)
      (%mb :> {:keys [*id *ka *kb]})
      (|hash *ka)
      (faults/attempt! *id :mb-a)
      (ops/current-task-id :> *ta)
      (local-transform> [(keypath *ta) (nil->val 0) (term inc)] $$mb-clock)
      (local-select> (keypath *ta) $$mb-clock :> *stamp)
      ;; same module, owned by the stream topology: committed view
      (local-select> (keypath *ta) $$s-clock :> *s-clock)
      (hash-map :id *id :stamp *stamp :task *ta :s-clock *s-clock :> *entry-a)
      (local-transform> [(keypath *ka) (nil->val []) AFTER-ELEM (termval *entry-a)] $$mb-a)
      ;; other module, through a mirror: a read, async, committed view
      (select> (keypath *ka) $$stamps :> *stood-on)
      (faults/note! *id :stood-on-read *stood-on)
      (|hash *kb)
      (faults/attempt! *id :mb-b)
      (faults/maybe-block! *id :mb-b)
      (faults/maybe-fail! *id :mb-b)
      (ops/current-task-id :> *tb)
      (hash-map :id *id :stamp *stamp :from *ka :task *tb
                :stood-on *stood-on :s-clock *s-clock :> *entry-b)
      (local-transform> [(keypath *kb) (nil->val []) AFTER-ELEM (termval *entry-b)] $$mb-b)

      ;; --- visibility probe: one round number written to A and B, plus filler
      ;;     writes to make each task's commit take longer ---
      (source> *vis-depot :> %vis)
      (%vis :> {:keys [*round *ka *kb *filler]})
      (|hash *ka)
      (local-transform> [(keypath *ka) (termval *round)] $$mb-vis)
      (anchor> <va>)
      (<<branch <va>
        (ops/range> 0 *filler :> *i)
        (local-transform> [(keypath *ka *i) (termval *round)] $$mb-fill))
      (|hash *kb)
      (local-transform> [(keypath *kb) (termval *round)] $$mb-vis)
      (ops/range> 0 *filler :> *j)
      (local-transform> [(keypath *kb *j) (termval *round)] $$mb-fill))))
