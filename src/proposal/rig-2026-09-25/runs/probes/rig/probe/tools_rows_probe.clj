(ns rig.probe.tools-rows-probe
  "Probe for PLAN-tools-and-grammars.md, two assumptions: (1) Rama 1.6.0
  takes a per-layer subindexed map of key rows whose row holds a nested
  fixed-keys-schema, a vector-schema of keywords and a set-schema, with nil
  in the nested and vector fields; (2) one foreign-select-one with `submap`
  over a subindexed map returns only the named entries that exist, in one
  call. Not the store: a two-field stand-in for $$layers."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

(def row-schema
  (fixed-keys-schema
   {:used    Boolean
    :grammar (fixed-keys-schema
              {:fid         clojure.lang.PersistentVector
               :stamp       Long
               :shape       String
               :subjects-at (vector-schema clojure.lang.Keyword)
               :opaque      Boolean
               :index       (set-schema clojure.lang.Keyword)})}))

(defmodule KeyRows [setup topologies]
  (declare-depot setup *in (hash-by first))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$layers
                    {clojure.lang.Keyword
                     (fixed-keys-schema
                      {:keys    (map-schema clojure.lang.Keyword row-schema
                                            {:subindex-options {:track-size? false}})
                       :answers (map-schema clojure.lang.PersistentVector Long
                                            {:subindex-options {:track-size? false}})})})
    (<<sources s
      (source> *in :> [*layer *kind *k *v])
      (<<if (= *kind :row)
        (local-transform> [(keypath *layer :keys *k) (termval *v)] $$layers)
       (else>)
        (local-transform> [(keypath *layer :answers *k) (termval *v)] $$layers)))))

(defn- nm [] [:alice :by-layer :offer (java.util.UUID/randomUUID)])

(deftest rows-and-submap
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc KeyRows {:tasks 4 :threads 2})
    (let [mn (get-module-name KeyRows)
          depot (foreign-depot ipc mn "*in")
          layers (foreign-pstate ipc mn "$$layers")
          g-note {:fid [(nm) 0] :stamp 5 :shape "[:any]" :subjects-at nil
                  :opaque false :index #{:by-value}}
          g-mention {:fid [(nm) 1] :stamp 6
                     :shape "[:map {:persons [:set-of [:keyword] 1 2]} {:open? true}]"
                     :subjects-at [:persons] :opaque false :index #{}}
          [n1 n2 n3] [(nm) (nm) (nm)]]
      (foreign-append! depot [:alice :row :note {:used true :grammar g-note}])
      (foreign-append! depot [:alice :row :mention {:used false :grammar g-mention}])
      (foreign-append! depot [:alice :row :blob {:used true :grammar nil}])
      (foreign-append! depot [:alice :ans n1 1])
      (foreign-append! depot [:alice :ans n2 2])
      (let [note (foreign-select-one [(keypath :alice :keys :note)] layers)
            mention (foreign-select-one [(keypath :alice :keys :mention)] layers)
            blob (foreign-select-one [(keypath :alice :keys :blob)] layers)
            none (foreign-select-one [(keypath :alice :keys :absent)] layers)
            sub (foreign-select-one [(keypath :alice :answers) (submap [n1 n3 n2])] layers)]
        (println "ROW-NOTE" (pr-str note))
        (println "ROW-MENTION" (pr-str mention))
        (println "ROW-BLOB" (pr-str blob))
        (println "ROW-ABSENT" (pr-str none))
        (println "SUBMAP-OF-3-NAMES-2-PRESENT" (pr-str (into {} (map (fn [[k v]] [(nth k 3) v])) sub))
                 "count" (count sub))
        (is (= g-note (:grammar note)))
        (is (nil? (get-in note [:grammar :subjects-at])))
        (is (= [:persons] (get-in mention [:grammar :subjects-at])))
        (is (= {:used true :grammar nil} (select-keys blob [:used :grammar])))
        (is (nil? none))
        (is (= {n1 1 n2 2} (into {} sub)))))))
