;; IMPORTANT: Before modifying this file, re-read PLAN-dependents.md.
(ns rig.store.dependents
  "The lookup from a fact to what stood on it (PLAN-dependents.md): an index
  in each one-owner layer, `:ix-dep`, on the dependent's side, at act grain.
  When the stream gate admits an act that stood on facts, the same event
  writes one entry per fact it stood on, holding ids only: the stood-on
  fact and its stamp as carried, the dependent act's name and stamp, and
  how many facts it has. A read `[:dependents x]` through the one exit
  (`rig.store.dependents-query`) shows the facts of every act that stood on
  x (a fact id) or on any fact of x (an act name), each opened from the log,
  with `:on`, the stood-on facts of x that act named.

  This namespace is pure and holds the index's shape: its schema, the
  entries of an admitted act, the parse and range of the pattern. Its call
  sites: `rig.store.module` merges `layer-fields` into a layer's schema;
  `rig.store.gate/decide*` appends `index-writes` to an admitted act's
  `:index-put`, which `rig.store.gate-event/write-decided>` writes unchanged;
  `rig.store.reads` delegates `[:dependents x]`'s parse and range here,
  hands the read to the `read-dependents` query, and rebuilds the field
  from each act's `:stood-on` in its put pages; shared layers and standing
  reads refuse the pattern (`kind?`, `pattern?`).

  Placeholders (PLAN-dependents.md section 10): the dependent's side, one
  layer a lookup (P-D1); act grain (P-D2); the stream store only (P-D3); no
  standing read (P-D4); a rebuild by put pages, stale entries skipped at
  read (P-D5); the store writes nothing on finding a dependent but the read
  entry (P-D6). Nothing here names a key, a tool or a citation.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [rig.store.envelope :as env]))

;; ---------------------------------------------------------------- addresses

(def sep "The part separator of an address, U+0000, as the read exit's." "\u0000")

(defn hex16 "A stamp as 16 hex digits, so String order is stamp order." [stamp] (format "%016x" (long stamp)))

(defn norm-fid "A fact id with its name a vector and its index a long." [[nm idx]] [(into [] nm) (long idx)])

(defn fid-text
  "A fact id's part of an address: its name's canonical EDN text, U+0000,
  and its index as 8 hex digits (the read exit's `fid-text` form, kept here
  so this namespace does not require `rig.store.reads`, which requires it)."
  [[nm idx]]
  (str (env/canonical (into [] nm)) sep (format "%08x" (long idx))))

(defn address
  "The address of one entry: the stood-on fact, the dependent act's stamp,
  the dependent act's name. Unique per (stood-on fact, dependent act); for
  one stood-on fact, its dependents sort by stamp."
  [on stamp act]
  (str (fid-text on) sep (hex16 stamp) sep (env/canonical (into [] act))))

;; ------------------------------------------------------------------- schema

(defn entry-schema
  "An entry, ids only: the stood-on fact and its stamp as the dependent
  carried it, the dependent act's name and stamp, and its count of facts."
  []
  (fixed-keys-schema {:on clojure.lang.PersistentVector
                      :on-stamp Long
                      :act clojure.lang.PersistentVector
                      :stamp Long
                      :n Long}))

(defn layer-fields
  "The field this adds to a one-owner layer's value in `$$layers`."
  []
  {:ix-dep (map-schema String (entry-schema) {:subindex-options {:track-size? false}})})

(def field "The index field." :ix-dep)

;; ------------------------------------------------------------------- writes

(defn index-writes
  "The entries of an admitted act (PLAN-dependents.md section 4): one
  `[:ix-dep address entry]` per fact it stood on (`stood-on`, the parsed
  offer's `{fid stamp}`), in the order of the stood-on facts' text; `nm` the
  act's name, `stamp` its stamp, `log` its rows (only their count is kept).
  Total: nil when an input is not what the envelope's parse gives, which
  the gate's `decide` turns into its unrecorded `:gate-error`."
  [nm stamp log stood-on]
  (try
    (let [act (into [] nm)
          n (long (count log))]
      (vec (for [[on on-stamp] (sort-by (comp fid-text key) stood-on)
                 :let [on (norm-fid on)]]
             [:ix-dep (address on stamp act)
              {:on on :on-stamp (long on-stamp) :act act :stamp (long stamp) :n n}])))
    (catch Throwable _ nil)))

(defn implied
  "What a finished rebuild leaves, `{address entry}`, for acts shaped as the
  read exit's put pages take them (`{:name :stamp :rows :stood-on}`)."
  [acts]
  (into {} (map (fn [[_ a e]] [a e]))
        (mapcat (fn [{:keys [name stamp rows stood-on]}] (index-writes name stamp rows stood-on)) acts)))

;; -------------------------------------------------------------------- reads

(def kinds "The parsed kinds of `[:dependents x]`." #{:dependents :dependents-act})

(defn kind? "Whether a parsed pattern is a lookup of dependents." [pp] (contains? kinds (:kind pp)))

(defn pattern?
  "Whether a pattern as a reader gives it, unparsed, is a lookup of
  dependents: what a standing read's opening checks before it reads."
  [p]
  (and (vector? p) (= :dependents (first p))))

(defn parse-pattern
  "`[:dependents x]` for the read exit's `parse-pattern`: x a fact id (what
  stood on that fact) or an act name (what stood on any fact of that act).
  `{:kind :pattern :ix :prefix}` to merge into the parse, or nil when x is
  neither."
  [x]
  (cond
    (env/fid? x) (let [f (norm-fid x)]
                   {:kind :dependents :pattern [:dependents f] :ix :ix-dep :prefix (str (fid-text f) sep)})
    (env/valid-name? x) (let [n (into [] x)]
                          {:kind :dependents-act :pattern [:dependents n] :ix :ix-dep
                           :prefix (str (env/canonical n) sep)})
    :else nil))

(defn bounds
  "Where a lookup's range starts and ends as of moment m: for a fact, the
  dependent's stamp follows the prefix, so the moment folds into the end;
  for an act, the range is the prefix and the page loop filters by stamp."
  [pp m]
  (let [p (:prefix pp)]
    (case (:kind pp)
      :dependents [p (str p (hex16 (inc (long m))))]
      :dependents-act [p (str (subs p 0 (dec (count p))) "\u0001")]
      ;; any other kind: an empty range, never a throw
      ["" ""])))

(defn act-groups
  "A lookup's kept entries (the page loop's `:kept`, in address order) as
  the dependent acts they name, each once, in order of first appearance:
  `[[act #{on ...}] ...]`, with the stood-on facts of x each act named."
  [kept]
  (let [order (distinct (map #(get-in % [:entry :act]) kept))
        on (reduce (fn [m k] (update m (get-in k [:entry :act]) (fnil conj #{}) (get-in k [:entry :on])))
                   {} kept)]
    (mapv (fn [a] [a (get on a)]) order)))
