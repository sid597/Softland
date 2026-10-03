;; IMPORTANT: Before modifying this file, re-read PLAN-tools-and-grammars.md
;; 6 (the minimal runner) and its V-F5 to V-F7 fixes, and
;; BUILD_NOTES-tools-and-grammars.md (D-P2: the door leases as the tool).
(ns rig.store.runner
  "The minimal runner (PLAN-tools-and-grammars.md 6; SPEC phase 6: \"a
  minimal runner finds tools by matching\"): operator code beside the door
  (rig.store.client) and the one read exit (rig.store.read-exit), as the
  exit is, and the recipe executor's first runner. Not a topology: its
  reads go through the one exit, which waits for its entry's answer before
  it shows anything, and its outputs are offers through the door, which
  seals them at the edge; topology code can do neither (6.1).

  One pass over layer L (`run-pass!`):
  1. find the tools by matching: the exit's pattern read `[:k :tool]` in L,
     read by the operator, role `:stood-on`; no tool is known to compiled
     code;
  2. read each as a tool (`recipe/parse-tool`), and refuse those that
     would feed themselves (`recipe/loop-free`);
  3. for each tool left, in its fact's stamp order: read its matches
     through the exit as the tool (its id the reader, kind `:tool`, its
     read-entry preference, role `:matched`, under its permission); run
     its recipe over each matched row that shows a value; look up every
     run's derived name at once (`client/lookup-many`) and offer through
     the door only the runs with no answer yet, each standing on the
     matched fact and the tool fact with their stamps, because of the
     matched act, under the tool's permission, in the tool's door session,
     claimed at the later stamp's millisecond: every part a function of
     (L, tool fact, matched fact), so a rerun is a retry and each match
     runs once (6.3).
     The citation (PLAN-citation.md section 3): the names are looked up
     before any recipe runs, and a recipe runs only for a match with no
     answer yet, then offers at once; so the matches of one tool run in the
     read's order (for `[:k k]`, by entity, then stamp), each seeing what
     the ones before it wrote. A recipe may read the store through the exit
     as the tool, and name more facts its act stands on.

  The door leases for the tool as the tool: in a layer with a person
  owner, a lease by a writer who is no person is sealed under the owner's
  lock (wave 1's W1-1), so the runner needs no lease step of its own (D-P2).

  Holds no state between passes. Never throws: a refused read, a tool it
  cannot parse, a loop, a failed step, a refused offer and an error from
  the door are each a line of the report.

  The spec fixes (H-1): a run is named with the layer's class and offered
  through the door of the gate that orders the layer, so a pass over a
  layer re-classed by entity runs at the micro gate; a match counts as run
  when its name under either class tag has a record, so a match run before
  the re-class does not run again after it.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.store.client :as c]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx]
            [rig.store.recipe :as recipe]))

(def default-limit "Rows a pass reads per pattern read (the exit's default)." 1000)

(defn- failed
  "A report line for a throwable from the door or the exit (a door that
  cannot lease, an answer that cannot be had): data, never a throw."
  [^Throwable t]
  {:refused :runner-error :error (or (.getMessage t) (str (class t)))})

(defn- shown? "A matched row that shows a value (not erased, not unreadable, not a retract)." [r]
  (and (map? r) (contains? r :value) (some? (:value r))))

(defn tools-in
  "The tool facts a `[:k :tool]` read found, as `[[fid row] ...]` in the
  order of their stamps: those that show a value and that no other tool
  fact in the page replaces (a replaced tool is not run; its replacement
  is). Pure."
  [rows]
  (let [replaced (into #{} (keep :replaces) rows)]
    (->> rows
         (filter shown?)
         (remove #(contains? replaced (:fid %)))
         (sort-by (juxt :stamp #(env/canonical (:fid %))))
         (mapv (fn [r] [(:fid r) r])))))

(defn output-act
  "The act one run offers (6.2 c, T-FR5): `:who` the tool's id, its
  permission, the tool's door session, standing on the matched fact and
  the tool fact with their stamps (based-on as fact ids with their stamps,
  CONCLUSION R4), because of the matched act (ruling 3: trigger is already
  because-of), naming no subjects, claimed at the millisecond of the later
  stood-on stamp (V-F5), under the run's derived name. Pure: the same
  (layer, tool fact, matched fact) give the same act.

  The citation (PLAN-citation.md section 3, P-C14): `also`, the pairs the
  recipe's `:stand-on` steps named, joins what the act stands on, and the
  claimed millisecond is that of the latest stamp it stood on, so no run
  claims a time before its own basis (review finding 11)."
  ([layer class tool-id tool-fid tool-stamp tool row facts]
   (output-act layer class tool-id tool-fid tool-stamp tool row facts {}))
  ([layer class tool-id tool-fid tool-stamp tool row facts also]
   (c/build {:name (recipe/run-name layer class tool-fid (:fid row))
             :who tool-id
             :layer layer
             :class class
             :permission (:permission tool)
             :session (c/default-session tool-id)
             :stood-on (merge also {(:fid row) (:stamp row), tool-fid tool-stamp})
             :because-of (first (:fid row))
             :subjects #{}
             :claimed-when (hlc/ms-of (reduce max (long (:stamp row)) (cons (long tool-stamp) (map long (vals also)))))
             :facts facts})))

(defn- offer-run
  "Offer one run's act through the door of the gate its name is tagged for
  (the spec fixes, H-1); its answer, or the door's error, as data."
  [store act]
  (try
    (let [a (mc/offer-into! store act)]
      (select-keys a [:answer :reason :stamp]))
    (catch Throwable t (failed t))))

(defn- tagged [nm class] (assoc nm 1 class))

(defn answered-runs
  "The records of the runs among `names` already answered, {name record},
  looked up once per store (the spec fixes, H-1): the `:by-layer` tag of
  every name on the stream side (`client/lookup-many`, one read), and, in a
  layer whose class is by entity, the `:by-entity` tag on the micro side
  (`micro-client/lookup-many`, one query)."
  [store layer class names]
  (merge (c/lookup-many store layer (mapv #(tagged % :by-layer) names))
         (when (= :by-entity class) (mc/lookup-many store (mapv #(tagged % :by-entity) names)))))

(defn run-record
  "The record of a run under either class tag of its name, or nil."
  [answered nm]
  (or (get answered nm) (get answered (tagged nm :by-layer)) (get answered (tagged nm :by-entity))))

(defn recipe-read
  "The read function a recipe's `:read` step calls (the citation's,
  PLAN-citation.md section 3): a pattern read through the one exit as the
  tool, recorded in the layer like its match read, with the role
  `:stood-on` (P-C12). Data, never a throw."
  [store {:keys [layer owner limit]} tool-id tool]
  (fn [pattern]
    (try
      (rx/read! store {:reader tool-id :reader-kind :tool :rows? (:rows? tool) :role :stood-on
                       :for owner :working layer :permission (:permission tool)
                       :layer layer :read [:pattern pattern] :limit limit})
      (catch Throwable t (failed t)))))

(defn- run-tool
  "Step 3 for one tool: its match read, its runs, their answers.

  The citation (PLAN-citation.md section 3): every match's run is named
  first and the names answered already are looked up before any recipe
  runs, so a recipe runs only for a match that has not run, and its reads
  are recorded only then; each recipe gets the name its act will have
  (`:run` in its row) and the tool's read function."
  [store {:keys [layer class owner limit config] :as ctx} [tool-fid row tool]]
  (let [tool-id (:e row)
        tool-stamp (:stamp row)
        read (try
               (rx/read! store {:reader tool-id :reader-kind :tool :rows? (:rows? tool) :role :matched
                                :for owner :working layer :permission (:permission tool)
                                :layer layer :read [:pattern (:matches tool)] :limit limit})
               (catch Throwable t (failed t)))]
    (if (contains? read :refused)
      {:tool tool-fid :id tool-id :read read}
      (let [matched (filter shown? (:rows read))
            named (mapv (fn [m] [m (recipe/run-name layer class tool-fid (:fid m))]) matched)
            answered (try (answered-runs store layer class (mapv second named))
                          (catch Throwable _ {}))
            config (assoc config :read (recipe-read store ctx tool-id tool))]
        {:tool tool-fid
         :id tool-id
         :entry (:entry read)
         :mark (:mark read)
         :matches (count (:rows read))
         :runs (mapv (fn [[m nm]]
                       (if-let [rec (run-record answered nm)]
                         {:match (:fid m) :name nm :recorded (select-keys rec [:answer :reason :stamp])}
                         (let [r (recipe/run tool (assoc m :run nm) config)]
                           (cond
                             (seq (:facts r))
                             (let [act (output-act layer class tool-id tool-fid tool-stamp tool m (:facts r) (:stood-on r))]
                               (assoc (offer-run store act) :match (:fid m) :name nm))
                             ;; the citation: an act of no facts is refused on its face and
                             ;; recorded nowhere, so a run with nothing to say is not offered
                             (contains? r :facts) {:refused :step-failed :error :no-output :match (:fid m)}
                             :else (assoc r :match (:fid m))))))
                     named)}))))

(defn run-pass!
  "One pass of the runner over layer `layer` (6.2). `limit` the rows each
  pattern read may show (default 1,000); `config` the operator's
  configuration, `{:repos {id path}}`, the repositories a tool's
  `:revision/*` steps may read by id (V-F13; empty by default, so no tool
  reads a repository unless the operator names one).

  Returns the report: `{:layer :tools-read {...} :tools [line ...]}`, a
  line per tool fact found: `{:tool fid :refused :malformed-tool :why r}`,
  `{:tool fid :refused :tool-loop}`, `{:tool fid :id id :read {:refused
  r}}` (its match read refused, nothing run), or `{:tool fid :id id :entry
  name :mark m :matches n :runs [...]}` with each run `{:match fid :name nm
  :answer a :reason r :stamp s}` (offered now), `{:match fid :name nm
  :recorded {...}}` (ran before), or `{:match fid :refused :step-failed
  ...}` (nothing offered). A tool whose read is marked `:partial` ran on
  its first `limit` matches only. Never throws."
  [store {:keys [layer limit config] :or {limit default-limit config {}}}]
  (try
    (let [settings (c/settings store layer)
          owner (:owner settings)
          ctx {:layer layer :class (or (:class settings) :by-layer) :owner owner :limit limit :config config}
          found (if (env/readable-keyword? owner)
                  (try
                    (rx/read! store {:reader :operator :reader-kind :tool :rows? false :role :stood-on
                                     :for owner :working layer :permission nil
                                     :layer layer :read [:pattern [:k :tool]] :limit limit})
                    (catch Throwable t (failed t)))
                  {:refused :no-such-layer})]
      (if (contains? found :refused)
        {:layer layer :tools-read found :tools []}
        (let [parsed (mapv (fn [[fid row]] [fid row (recipe/parse-tool (:value row) gate/store-key?)])
                           (tools-in (:rows found)))
              malformed (vec (for [[fid _ p] parsed :when (:refused p)] (assoc p :tool fid)))
              ok (vec (for [[fid row p] parsed :when (:ok p)] [fid row (:ok p)]))
              {:keys [run refused]} (recipe/loop-free (mapv (fn [[fid _ t]] [fid t]) ok) gate/store-key?)
              runnable (into #{} (map first) run)
              loops (vec (for [[fid _ _] ok :when (contains? refused fid)] {:tool fid :refused :tool-loop}))
              lines (vec (for [[fid row t] ok :when (contains? runnable fid)]
                           (try (run-tool store ctx [fid row t])
                                (catch Throwable e (assoc (failed e) :tool fid)))))]
          {:layer layer
           :tools-read (select-keys found [:entry :mark :matched])
           :tools (-> malformed (into loops) (into lines))})))
    (catch Throwable t {:layer layer :tools-read (failed t) :tools []})))
