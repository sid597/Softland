;; The judge A2 declaration and the coincident-center variation. Run from
;; either worktree with clj -M:test -i test/app/client/path/probe_a2.clj;
;; to use the read-only baseline's source from main, prepend its src path
;; with -Sdeps. Reports values; the regression assertion is in nib_test.clj.
(require '[app.client.path.source :as source]
         '[app.client.path.stroke :as stroke]
         '[app.client.path.component :as component]
         '[app.client.path.pack :as pack])

(let [samples [[10.0 50.0 0.1] [12.0 50.0 1.0]]
      shapes (for [[label ss tool] [[:literal samples {}]
                                     [:declared samples {:size 16 :width [:* [:get :size] [:get :p]] :fit :polyline}]
                                     [:coincident [[12.0 50.0 0.1] [12.0 50.0 1.0]]
                                      {:size 16 :width [:* [:get :size] [:get :p]] :fit :polyline}]]]
               ;; The baseline string evaluator and production EDN evaluator
               ;; express the same supplied rule; defaults are left alone.
               (let [tool (if (ns-resolve 'app.client.path.source 'width-function)
                            (cond-> tool (:width tool) (assoc :width "size * p")) tool)
                     path (:path (source/build {:kind :pen :samples ss} tool))
                     skin (:path (stroke/envelope path (component/stroke-defaults {}) {:tip :nib :tolerance 0.01}))
                     packed (:pack (pack/pack-region skin 0.001 {}))]
                 {:case label :widths (mapv :width (get-in path [:subpaths 0 :knots]))
                  :inside-19-50 (pack/inside? packed :nonzero 19.0 50.0)}))]
  (prn (vec shapes)))
(System/exit 0)
