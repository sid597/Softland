;; IMPORTANT: Before modifying this file, re-read PLAN-dependents.md section 5.
(ns rig.store.dependents-query
  "The read of `[:dependents x]` (PLAN-dependents.md section 5), as the query
  topology `read-dependents`, on the layer's home task. The read exit's
  `read-pattern` hands a parsed lookup here by name, so this namespace may
  require `rig.store.reads` without a cycle.

  The query checks what it is given before it reads, as the shared queries
  invoked by name do (review finding 5): the layer and the reader readable
  keywords, the moment a stamp, the pattern a parsed lookup with a limit;
  the layer not of the shared kind; the reader allowed to see it. Anything
  else is refused as data, since an exception in a query topology is fatal
  to the worker.

  It pages over `:ix-dep` with the exit's own page loop, groups the kept
  entries by dependent act, and for each act admitted by the moment reads
  its log once and opens every row through the one seam, each shown with
  `:on`, the stood-on facts of x that act named. An entry whose act is not
  admitted by the moment is skipped, so an entry the log no longer implies
  (a restore) shows nothing. The limit counts the rows shown (finding 4):
  the read stops before an act that would pass it, or inside the first act
  when that act alone passes it, and marks the answer partial. Every loop
  yields. The answer is the exit's pattern answer: the rows, the matched
  facts and their fingerprint, and the mark."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.dependents :as dependents]
            [rig.store.envelope :as env]
            [rig.store.reads :as reads]))

(defn args-refusal
  "Why the query's arguments are not a lookup it can run, as a refusal, or
  nil. Total."
  [layer for pp m]
  (try
    (when-not (and (env/readable-keyword? layer) (env/readable-keyword? for)
                   (int? m) (<= 0 m)
                   (map? pp) (dependents/kind? pp) (= :ix-dep (:ix pp))
                   (string? (:prefix pp))
                   (int? (:limit pp)) (<= 1 (:limit pp) 10000))
      {:refused :bad-read})
    (catch Throwable _ {:refused :bad-read})))

(defn either "Whether either mark says more exists." [a b] (boolean (or a b)))

(defn shown
  "One dependent fact's row: the exit's point row, with `:on`."
  [fid stamp row o on]
  (try (assoc (reads/point-row fid stamp row o) :on on)
       (catch Throwable _ {:fid fid :unreadable :does-not-open :on on})))

(deframaop dependents-read>
  "The lookup's read on the layer's home task, for reader `*for`, as of
  moment `*m`."
  [*layer *for *pp *m]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (args-refusal *layer *for *pp *m :> *bad)
    (<<if (some? *bad)
      (:> *bad)
     (else>)
      (local-select> [(keypath *layer :settings)] $$layers :> *settings)
      (<<cond
        (case> (reads/shared-layer? *settings))
        (:> {:refused :bad-pattern})

        (case> (not (reads/visible? *settings *for)))
        (:> {:refused :not-visible})

        (default>)
        (get *pp :ix :> *pix)
        (get *pp :limit :> *lim)
        (reads/page-init *pp *m :> *st0)
        (loop<- [*pst *st0 :> *pfinal]
          (yield-if-overtime)
          (get *pst :from :> *pfrom)
          (get *pst :page :> *ppage)
          (local-select> [(keypath *layer *pix) (sorted-map-range-from *pfrom *ppage)] $$layers
                         {:allow-yield? true} :> *psub)
          (reads/page-entries *psub :> *pents)
          (reads/page-step *pst *pents {} :> *pst2)
          (<<if (get *pst2 :done?)
            (:> *pst2)
           (else>)
            (continue> *pst2)))
        (get *pfinal :kept :> *kept)
        (get *pfinal :more? :> *more?)
        (dependents/act-groups *kept :> *groups)
        ;; each act once; the limit counts the rows shown
        (loop<- [*gt *groups *acc [] *pc {} :> *rows *cut]
          (yield-if-overtime)
          (<<cond
            (case> (empty? *gt))
            (:> *acc false)

            (case> (>= (count *acc) *lim))
            (:> *acc true)

            (default>)
            (first *gt :> [*act *on])
            (local-select> [(keypath *layer :answers *act)] $$layers :> *rec)
            (<<if (reads/yes-by? *rec *m)
              (get *rec :stamp :> *s)
              (local-select> [(keypath *layer :log *act) (subselect ALL)] $$layers
                             {:allow-yield? true} :> *arows)
              (loop<- [*i 0 *racc *acc *rpc *pc :> *acc2 *pc2 *rcut]
                (yield-if-overtime)
                (<<cond
                  (case> (>= *i (count *arows)))
                  (:> *racc *rpc false)

                  (case> (>= (count *racc) *lim))
                  (:> *racc *rpc true)

                  (default>)
                  (nth *arows *i :> *row)
                  (reads/fid-of *act *i :> *fid)
                  (reads/open-row-with> *layer *fid *row *s *m *rpc :> *o *rpc2)
                  (shown *fid *s *row *o *on :> *r)
                  (continue> (inc *i) (conj *racc *r) *rpc2)))
              (<<if *rcut
                (:> *acc2 true)
               (else>)
                (continue> (rest *gt) *acc2 *pc2))
             (else>)
              (continue> (rest *gt) *acc *pc))))
        ;; shown as opaque, as every read through the exit (one seek a distinct key)
        (reads/shown-keys *rows :> *ks)
        (loop<- [*kt *ks *kacc {} :> *krows]
          (yield-if-overtime)
          (<<if (empty? *kt)
            (:> *kacc)
           (else>)
            (first *kt :> *k)
            (local-select> [(keypath *layer :key-rows *k)] $$layers :> *krow)
            (continue> (rest *kt) (assoc *kacc *k *krow))))
        (reads/mark-opaque *rows *krows :> *mrows)
        (either *more? *cut :> *partial)
        (reads/pattern-answer *layer *m *pp *mrows *partial :> *answer)
        (:> *answer)))))

(defn declare-queries!
  "The query `read-dependents`, which `rig.store.reads`' `read-pattern`
  invokes by name for a parsed `[:dependents x]`, after its own checks."
  [topologies]
  #_:clj-kondo/ignore
  (<<query-topology topologies "read-dependents" [*layer *for *pp *m :> *answer]
    (|hash *layer)
    (dependents-read> *layer *for *pp *m :> *answer)
    (|origin)))
