;; decision-4: a throwaway evaluation, not code. It asks the store's pure gate
;; functions at HEAD what they say of the act maps of the four ends.
(require '[softland.store.gate.first-act :as fa]
         '[softland.store.gate.decision :as d]
         '[softland.store.door.text :as text])
(defn row [v] {:value (text/text v)})
(def sys-base [:system :base :base])
(def sid #uuid "00000000-0000-4000-8000-000000000001")
(def L #uuid "00000000-0000-4000-8000-000000000002")
(def sid-own [sid L L])
(defn act [who layer perm facts] {:version 1 :who who :layer layer :permission perm :session :run-1 :facts facts})
(def making-base (act :system :base sys-base [{:e :base :k :softland.store/depot :v :microbatch} {:e :system :k :softland.store/permission :v sys-base}]))
(def making-sid-by-system (act :system L sid-own [{:e L :k :softland.store/depot :v :streaming} {:e sid :k :softland.store/permission :v sid-own}]))
(def making-tools (act :system :tools [:system :tools :tools] [{:e :tools :k :softland.store/depot :v :streaming} {:e :system :k :softland.store/permission :v [:system :tools :tools]}]))
(println "A making base, microbatch gate:" (fa/making-act? :microbatch making-base))
(println "A making base sent by any program as :system (no sender check): same map, standing with empty rows ->" (d/standing :microbatch making-base nil nil))
(println "A system's ingest act in base, its permission row there ->" (d/standing :microbatch (act :system :base sys-base [{:e :doc-1 :k :text :v "x"}]) (row sys-base) nil))
(println "B making Sid's layer by :system, streaming gate ->" (fa/making-act? :streaming making-sid-by-system))
(println "B later act by :system in Sid's layer under Sid's permission (no row at (L, :system), depot row present) ->"
         (d/standing :streaming (act :system L sid-own [{:e :x :k :text :v "x"}]) nil (row :streaming)))
(println "B later act by :system in Sid's layer under [:system L L] ->"
         (d/standing :streaming (act :system L [:system L L] [{:e :x :k :text :v "x"}]) nil (row :streaming)))
(println "C making :tools by :system on streaming ->" (fa/making-act? :streaming making-tools) "; on microbatch ->" (fa/making-act? :microbatch making-tools))
(def ing-in-base [:ingester-1 :base :base sys-base])
(println "D give in base, check (g) ->" (#'d/permission-fact-fault (act :system :base sys-base []) {:e :ingester-1 :k :softland.store/permission :v ing-in-base}))
(println "D give into another layer from the base ->" (#'d/permission-fact-fault (act :system :base sys-base []) {:e :ingester-1 :k :softland.store/permission :v [:ingester-1 L L sys-base]}))
(println "D ingester act in base, its row there ->" (d/standing :microbatch (act :ingester-1 :base ing-in-base [{:e :x :k :text :v "x"}]) (row ing-in-base) nil))
(def sid-in-base [sid :base :base sys-base])
(println "E Sid in base beneath the system's, his row there ->" (d/standing :microbatch (act sid :base sid-in-base [{:e :x :k :text :v "x"}]) (row sid-in-base) nil))
(def ing-under-sid [:ingester-1 :base :base sid-in-base])
(println "D under E: Sid gives the importer a right beneath his own in base, check (g) ->" (#'d/permission-fact-fault (act sid :base sid-in-base []) {:e :ingester-1 :k :softland.store/permission :v ing-under-sid}))
(println "D under E: the importer's act, its row there ->" (d/standing :microbatch (act :ingester-1 :base ing-under-sid [{:e :x :k :text :v "x"}]) (row ing-under-sid) nil))
(println "D under E: the act's one form passes the declaration's parse ->" (contains? (softland.store.act/parse (act :ingester-1 :base ing-under-sid [{:e :x :k :text :v "x"}])) :ok))
;; Added after review: one importer id serves one person. A second person's give of the same id in the base.
(def carol-in-base [:carol :base :base sys-base])
(def carol-gives (act :carol :base carol-in-base [{:e :ingester-1 :k :softland.store/permission :v [:ingester-1 :base :base carol-in-base]}]))
(def ing-place (softland.store.record.rows/place-key :base :ingester-1 :softland.store/permission))
(println "One id, one person: Carol's give of :ingester-1 in base, place already holding Sid's give ->" (d/places :microbatch carol-gives false {ing-place (row ing-under-sid)}) "; place empty ->" (d/places :microbatch carol-gives false {}))
