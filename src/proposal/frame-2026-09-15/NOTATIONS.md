# The frame in its native notations, with the walk as a trace (15 September 2026)

The session's answer to Sid's question about [PICTURE.md](PICTURE.md): is there
a better representation than the prose. Written by Claude Fable 5.1 at maximum
effort, from the pasted picture alone; no repo file was read for it. Everything
below is derived from that text. Positions are marked as such. Nothing was
checked against the repository. The trace is a hand simulation of the rules,
which is where an error is most likely; the closing paragraph names the
executable model that would remove the hand.

---

**The view first.** The prose is not one kind of content. It is five, interleaved: shapes, topology, rules, a trace, and choices, plus arguments. Prose is the native form only for the arguments. Each of the other four has a form that is shorter to load and, more importantly, can fail. A shape written as data has to say what is in every slot. A walk written as a trace has to land every step on a fact with a basis or admit it cannot. So the better representation is not one notation but a small set, each native to its content, with the trace as the load-bearing piece because it is the only one that can be wrong in a way you can see. The whole set is below. The frame carried the walk. Writing it as data forced seven choices the prose left implicit and surfaced eight things the prose implies but does not say. Those are at the end and are the actual payoff.

## The frame in its native notations

**The envelope, as data.** This is what the floor decides for the life of the system. Every "decided" item is visible here either as a slot or as an absence. EDN is notation here, not a store choice.

```clojure
;; ─── A FACT ──────────────────────────────────────────────────────────────────
;; The only record shape the store holds. Append-only.
{:seq     41             ; assigned by the gate. This IS the revision.
 :thing   f1             ; an id. Nothing about it is intrinsic. No :type slot. No is-a.
 :attr    :source        ; the kind. Patterns match on this.
 :value   "(defn f …)"   ; shaped by (grammar-of :source), itself a fact with a seq
 :actor   alice          ; whose authority. A person, an agent, ingest, ci, the gate.
 :time    t41            ; assigned by the gate. Never read as "now" by anyone.
 :layer   A              ; the context axis. The only axis that is a slot.
 :basis   [...]          ; the reads this stood on. Three entry shapes, below.
 :request 38}            ; seq of the fact whose landing started this pass.
                         ; nil = this claim originates a pass.

;; ─── A BASIS ENTRY ───────────────────────────────────────────────────────────
;; The prose's "thing, revision, exact" is only the first of these.
{:kind :point   :fact 12                 :exact? true}   ; I read this fact
{:kind :pattern :pattern p  :as-of 38    :exact? true}   ; I read all matching p, as of seq 38
{:kind :outside :ref "git:c3e"}                          ; I stood on what the store does not hold

;; ─── A CLAIM ─────────────────────────────────────────────────────────────────
;; A fact before the gate. One slot each way, nothing else differs:
;;   claim = fact − #{:seq :time} + #{:expected}
;;   fact  = claim − #{:expected} + #{:seq :time}
{:expected 12}           ; the seq I believe is current for (thing attr layer).
                         ; nil = I believe the row is absent.

;; ─── A DECISION ──────────────────────────────────────────────────────────────
;; The gate's answer. A fact like any other. Its basis is exactly what the gate checked.
{:thing claim-41  :attr :decision
 :value {:outcome :admitted  :seq 41  :rule pol-7}                       ; or:
 :value {:outcome :rejected  :reason :revision  :lost-to 40  :rule pol-7}
 :actor gate  :layer base
 :basis [{:kind :point :fact <grammar>} {:kind :point :fact 40} {:kind :point :fact pol-7}]}

;; ─── A CONSUMER ──────────────────────────────────────────────────────────────
;; A fact whose value is a signature and a body. Matched, never named.
{:thing s1  :attr :consumer
 :value {:signature {:demands {:kinds      #{:watching}                    ; index by kind
                               :conditions [[sess :tab (= :instrument)]]}  ; read against ctx at match time
                     :yields  {:kind :surface  :nature :conclusion}}       ; nature is enforced, not chosen
         :regime    #{:inside :instant}   ; derived by the floor from the body's leaves, not declared
         :body      …}}                   ; opaque to the medium. Steps over leaves and names resolved at use.

;; ─── A CONTEXT ───────────────────────────────────────────────────────────────
;; What an asker brings to a read. Not a fact. Built by resolve from policy facts.
{:actor alice  :layers [S A base]}        ; nearest first. visible? = "is this layer in my list"
```

★ Insight ─────────────────────────────────────
- The gate is a slot swap plus three checks. A claim carries `:expected`; a fact carries `:seq` and `:time`. Nothing else differs, so "only the gate writes" is a statement about who may perform that swap.
- There is no producer slot. Resolving a rule's name is a read, so the rule that fired sits in the basis like any other fact it read. Actor stays free to mean authority.
- Only `:layer` is a slot. The other three axes are read off the fact or its body. Four axes, one column.
─────────────────────────────────────────────────

**The topology.** Where the one gate sits, what loops back through which inlet, and what is paper rather than picture.

```
              ╔═══════════ RUNTIME · the one consumer that is not a fact · its revision is ═══════════╗
              ║                                                                                      ║
  OUTSIDE     ║  outside inlet ──┐                                                                   ║
  commit      ║                  │                                                                   ║
  tick    ────╫─────────────────▶├─▶ GATE ──▶ STORE ──▶ RESOLVE ──▶ MATCH ──▶ CONSUMERS ─┬─▶ conclusions
  reply       ║                  │   shape     append    visibility   by kind   runners   │   never stored
  sensor      ║  inside inlet ───┘   revision  only      nearest      then      budgets   │   feed the next read
  pointer     ║       ▲              policy    facts     layer wins   conditions owners   │   recompute on move
  instance    ║       │              only writer                                         │
              ║       │                                                                  │
              ║       └────────────── claims from inside (request = triggering seq) ◀────┤
              ║                                                                          │
              ║                                              outward crossings ◀─────────┘
              ║                                              screen · host · minds · instances
              ╚═════════════════════════════════════════════════════╪════════════════════════════════╝
      ▲                                                             │
      └──── inward return · identity, address, basis, and sometimes request are manufactured here ────┘
```

**The pass.** Five stations, two properties on every arc, one loop closing it.

```
      ┌──────────────────────── ⑤ maintain: support moved → re-trigger ──────────────────────────┐
      │                                                                                          │
      ▼                                                                                          │
  ① TRIGGER ────▶ ② READ ────▶ ③ DERIVE ────▶ ④ ASSERT | CROSS ────────────────────────▶ ⑤ MAINTAIN
  why now         with ctx      by a runner     claim → gate → fact | decision            rerun policy of the boundary
  · demand        · by value    · owner if long cross → screen | host | mind              · on change     (topology)
  · event         · by identity · budget                        │                         · while watched (mounted read)
  · basis moved   · by version  · lifecycle                     └─▶ inward return         · on a budget   (batch)
  · owner's claim · by pattern                                       → outside claim → ①
  · outside claim status: value | absent | pending | failed
                  support: seq + exact?
  ════════════ on every arc: PROVENANCE (actor, basis) and STATUS. Not steps. Properties. ════════════
```

**The rules, as predicates.** These are the parts of the prose that are definitions. Written this way you can see which are floor and which are material: a predicate over facts that any inhabitant could rewrite is material.

```clojure
;; ─── THE GATE. Only writer. Three checks: two floor, one material. ────────────
(defn gate [claim store]
  (let [grammar (grammar-of store (:attr claim))                                     ; a fact
        current (current-row store (:thing claim) (:attr claim) (:layer claim))
        rule    (policy-matching store (:actor claim) (:layer claim) (:attr claim))] ; facts
    (cond
      (not (shaped? grammar (:value claim)))    (reject :shape    [grammar])
      (not= (:expected claim) (:seq current))   (reject :revision [current] :lost-to (:seq current))
      (nil? rule)                               (reject :policy   [])
      :else (admit (-> claim (dissoc :expected) (assoc :seq (next-seq store) :time (now)))
                   :rule rule  :basis [grammar current rule]))))
;; (now) appears exactly once in the frame: here, as a stamp. Never as a read.
;; For :attr :consumer the shape check also derives :regime from the body's leaves
;; and refuses :nature :conclusion on an :outside body. Nature is enforced here.

;; ─── RESOLVE. Where reads leave the store. No consumer can skip it. ───────────
(defn resolve [store ctx thing attr]
  (or (some #(latest store thing attr %) (:layers ctx))     ; nearest active layer wins
      {:status :absent}))
;; → {:status :value :fact f} | {:status :absent} | {:status :pending} | {:status :failed}
;; every call leaves a basis entry on the caller. Status is part of what was read.

;; ─── MATCH. How consumers are found. Nobody routes. ───────────────────────────
(defn match [fact store ctx]
  (->> (consumers store ctx)
       (filter #(contains? (-> % :value :signature :demands :kinds) (:attr fact)))      ; index by kind
       (filter #(every? (fn [[t a pred]] (pred (resolve store ctx t a)))                 ; condition reads
                        (-> % :value :signature :demands :conditions)))))
;; cost = candidates-by-kind × condition-reads. The number to watch at tens of agents per person.

(defn miss? [fact store ctx]
  (> (count (filter #(= :claim (-> % :value :signature :yields :nature))
                    (match fact store ctx)))
     1))
;; Only claim-yielders can collide: they are interpretations. Conclusions never compete.
;; A collision is recorded as a fact, never resolved by priority.

;; ─── KIND AS A DEGREE · TYPE, COMPUTED · AFFORDANCE, COMPUTED ────────────────
(defn degree     [fact store ctx]  (count (match fact store ctx)))
(defn attrs-of   [thing store ctx] (set (map :attr (visible store ctx thing))))   ; the only "type"
(defn affordances [at store ctx]
  (match {:attr :pointed :value {:target at}} store (relax-session-conditions ctx)))
;; a halo is this set painted, each entry with the session fact it still needs.

;; ─── STALENESS · EXACTNESS. Definitions, not floor. ───────────────────────────
(defn superseded? [store ctx {:keys [fact]}]
  (let [f (fact-at store fact)]
    (some #(and (= (:thing %) (:thing f)) (= (:attr %) (:attr f)) (= (:layer %) (:layer f))
                (> (:seq %) (:seq f)) (visible? ctx %))
          store)))
(defn grown? [store ctx {:keys [pattern as-of]}]
  (some #(and (> (:seq %) as-of) (matches? pattern %) (visible? ctx %)) store))

(defn stale? [store ctx x]                      ; x is a fact or a conclusion. One rule.
  (some #(case (:kind %)
           :point   (superseded? store ctx %)
           :pattern (grown? store ctx %)
           :outside false)                      ; an anchor is what it is
        (:basis x)))
;; on a fact       → painted as "made against an old version". Nothing re-runs.
;; on a conclusion → maintain re-triggers its consumer.

(defn unreliable? [x] (some #(false? (:exact? %)) (:basis x)))           ; local to x's own reads
(defn doubtful?   [store x]                                               ; follows the chain
  (or (unreliable? x)
      (some #(and (= :point (:kind %)) (doubtful? store (fact-at store (:fact %))))
            (:basis x))))
;; "the map must not lie" is a view that paints doubtful? as loudly as stale?.

;; ─── THE JOURNEY. Free, because every crossing left a fact with an actor and a basis.
(defn journey [store actor day]
  (for [f store :when (and (= actor (:actor f)) (on? day (:time f)))]
    {:did f
     :saw (for [b (:basis f) :when (= :point (:kind b))
                :let [g (fact-at store (:fact b))] :when (= :presented (:attr g))] g)}))
(defn history [store thing] (filter #(= thing (:thing %)) store))        ; the other grouping
```

**Choices, as tables.** Lookups. Nothing here needs to be read in order.

The four axes:

| axis | values | question it answers | where it lives |
|---|---|---|---|
| context | base · candidate · session · personal | who may see it, which wins | a slot: `:layer`. Nearest active wins. Promotion is an admitted claim. |
| strata | material · Softland code · host | what changing it costs | derived from a body: interpreted steps, a compiled fn, or outside. Rebuild is a crossing to the host. |
| grain | thing · version | is this current | derived: `:thing` versus `:seq`. Staleness reads it. |
| tier | local signal · session fact · accepted fact | is this real yet | derived: before the gate, after it in a session layer, after promotion. Optimism is painting tier one as tier three. |

Tier overlaps context on its second and third values. What tier adds is only the first: not yet a fact.

The edge types:

| edge | from → to | what crosses | in the trace below |
|---|---|---|---|
| match | fact → consumer | kind in pattern, conditions hold | 20 matches the selection rule and the halo |
| read | consumer → fact | seq, status, exactness | every basis entry |
| resolve | context → fact | which layer won | A over base on the relation |
| claim | consumer → gate | value, basis, expected | 30 |
| decision | gate → claim | outcome, reason, rule | 25d, 30d, 37d |
| continuity | version → thing | asserted identity, with its method | 34 |
| presentation | conclusion → agent | the support it was built from | 19, 39 |
| reference | body → definition | a name, resolved to a read at use | point 7 in 19's support |

Nature by regime:

```
                     instant                            long-running
  inside   ┌────────────────────────────────┬────────────────────────────────────┐
  may be   │ view · staleness · match ·     │ batch index · migration            │
  determ.  │ placement · editor             │ owned, budgeted, lifecycle         │
           │ → conclusion                   │ → conclusion, or progress claims   │
  outside  ├────────────────────────────────┼────────────────────────────────────┤
  never    │ tick · pointer · keystroke ·   │ rebuild · model call · ingest ·    │
  replay.  │ sensor reading                 │ ci · erasure                       │
           │ → claim (an event fact)        │ → claim (outcome) + an effect      │
           └────────────────────────────────┴────────────────────────────────────┘
  The rule: :conclusion requires a replayable body. The floor derives regime from
  leaves; the gate refuses a :conclusion signature on an :outside body.
```

Levels, computed:

| consumer | demands | reads | shape |
|---|---|---|---|
| a view over passages | `{:kinds #{:text}}` | passage facts | same |
| a view over that view | `{:kinds #{:consumer}}` | the definition fact | same |
| a view over the running floor | `{:kinds #{:unit}}` | facts ingest made from the runtime's forms | same |

Level is following `:demands` upward until a pattern would have to match the runtime. That is the fixed point, mechanically.

The three view kinds:

| kind | pointed thing | the rule yields | seen next by |
|---|---|---|---|
| ordinary | material | a claim about material | views over that material |
| point on point | a definition | a new revision of it | every body that names it, next pass |
| composite | two demands in one workspace | session facts coordinating them | both, as repetition resolved at use |

Instances, as siblings:

| frame | instance | note from the prose |
|---|---|---|
| log | Rama depot | one log instance |
| derived index | PStates | placement's output |
| runner, rerun on change, hosts the gate | Rama topology | |
| runner, rerun while watched, records support | Electric tracked reads | |
| runner for outward crossings | engine executor with capability tables | |
| presentation | renderers | |
| inward return: identity and continuity | ingest lane | |
| gate with actor sets and resident policy | Inland admission | |
| placement | Inland index by event and demand kind | |
| maintain | Inland proxy on a keypath | most portable; nearest the abstraction |
| meaning of pointing as material | Inland pointing rule | |
| kind two view | Inland editor view | |
| placement and match | Canonical verb and projection registries | written as code, not facts |
| claims | Canonical matter-room routes | without basis |
| gate shape check | Worn facet compile | |
| gate per family | May three-depot design | upfront partition; regimes, not families |

Vocabulary:

| was | becomes | because |
|---|---|---|
| compiled / computed | resolved at use / rebuilt through the host | two regimes with different latency and reach |
| tool | consumer, or definition | the abstraction, or the fact holding it |
| CRUD | claim, gate, fact | no update or delete in a fact store; assertions and tombstones |
| readback | outside inlet plus maintain | |
| normal data | low degree | few current demands |
| layer | one of four axes, named | |
| projection | conclusion | Sid's word, kept |
| compiler, deploy, readback | one enactment: request claim, outcome claim, inward return | |

## The walk as a trace

Every step of the walk, as the facts it leaves and the conclusions it does not. A row with a seq is a fact that passed the gate. Indented lines are what happened between facts and were never stored. `✓` and `✗` after a basis entry are `:exact?`. `req` is `:request`, `exp` is `:expected`. Layers: `base`, `A` is alice's durable layer, `S` is her session layer. Passage continuity is elided.

The seed. Rows 7 through 12 are the point: adding a consumer is an ordinary claim.

```
seq  thing  attr         value                                            actor   layer  basis                    req  exp
  1  ev0    :commit      {:ref git:a1c}                                   host    base   [outside git:a1c]        nil  nil
  2  f1     :source      "(defn f [x] …)"                                 ingest  base   [point 1]                1    nil
  3  f1     :continuity  {:name "f" :hash h1 :by :name}                   ingest  base   [point 1]                1    nil
  4  p1     :text        "In the beginning …"                             ingest  base   [point 1]                1    nil
  5  c1     :text        "Only Sid decides."                              ingest  base   [point 1]                1    nil
  6  chk1   :result      {:of f1 :status :pass}                           ci      base   [point 2 ✓]              1    nil
  7  s1     :consumer    demands #{:watching} cond [sess :tab = :instrument] · yields :surface/:conclusion
                         body "members of the watched q; :text|:source of each; relations among them;
                               marks via stl against the previous presentation; paint"
                                                                          alice   A      []                       nil  nil
  8  halo   :consumer    demands #{:pointed} · yields :affordance/:conclusion
                         body "affordances at target; paint around it"    alice   A      []                       nil  nil
  9  sel    :consumer    demands #{:pointed} cond [sess :selected absent] [sess :mode absent]
                         yields :selected/:claim                          alice   A      []                       nil  nil
 10  rel    :consumer    demands #{:pointed} cond [sess :selected present] [sess :mode = :relate]
                         yields :relation/:claim                          alice   A      []                       nil  nil
 11  insp   :consumer    demands #{:pointed} cond [sess :mode = :inspect] · yields :editor/:conclusion
                                                                          alice   A      []                       nil  nil
 12  stl    :consumer    invoked by reference from bodies · yields :marks/:conclusion
                         body "for each support entry of the previous presentation: superseded? → mark;
                               for each read: doubtful? → mark"           alice   A      []                       nil  nil
 13  q1     :members     #{p1 f1 c1}                                      alice   A      [point 4 ✓, 2 ✓, 5 ✓]    nil  nil
 14  sess   :tab         :instrument                                      alice   S      []                       nil  nil
 15  sess   :watching    q1                                               alice   S      []                       nil  nil
 16  sess   :presented   {:by [s1 7] :to alice
                          :support [point 7, 12, 13, 4, 2, 5, pattern {:attr :relation :among #{p1 f1 c1}} as-of 15]}
                                                                          rt      S      [= support]              15   nil
 17  ev1    :commit      {:ref git:b2d}                                   host    base   [outside git:b2d]        nil  nil
 18  p1     :text        "In the beginning, again …"                      ingest  base   [point 17, point 4 ✓]    17   4
```

The walk.

```
── 2 · the surface shows things; one has moved on ──────────────────────────────────────────
   trigger   basis moved: 18 supersedes 4, and point 4 is in 16's support. Rerun policy: while watched (15).
   read      q1 :members → 13 ✓ · p1 :text → 18 ✓ · f1 :source → 2 ✓ · c1 :text → 5 ✓ · relations among → ∅ as-of 18
             s1 by name → 7 ✓  (reference resolved at use; this read is why editing s1 later re-triggers it)
             stl by name → 12 ✓ · previous presentation by s1 to alice → 16 ✓
   derive    surface = [p1@18 ·moved   f1@2   c1@5]      conclusion. Not stored.
             the mark: 16 had point 4, this pass has 18. stl computed it inside this pass.
             "last looked at" is 16 itself. There is no separate last-seen state.
   cross     to the screen. Presentation across a boundary is a claim:
 19  sess  :presented  {:by [s1 7] :to alice :support [point 7, 12, 16, 13, 18, 2, 5, pattern rel as-of 18]}
                                                                          rt      S      [= support]              18   nil

── 3 · they point at the function ──────────────────────────────────────────────────────────
   inward    the screen yields (x,y): a local signal. Tier one. Not a fact.
             the runtime manufactures an address from 19's painted layout: (x,y) → f1
 20  sess  :pointed    {:target f1 :via 19}                               alice   S      [point 19 ✓]             nil  nil
             req nil: this originates a pass. Its basis is manufactured from the presentation it answers.
   trigger   event: 20 landed → match on :pointed under ctx {alice [S A base]}:
               halo ✓  (no conditions)
               sel  ✓  (selected absent ✓ · mode absent ✓)
               rel  ✗  (selected absent)
               insp ✗  (mode absent)
             claim-yielders matched: {sel}. One. No miss. halo is a conclusion; it does not compete.
 21  sess  :selected   f1                                                 alice   S      [point 9, point 20]      20   nil
             actor is alice: the rule ran under her authority. The rule is point 9, in the basis by reference.

── 4 · around it appears what can be done here ─────────────────────────────────────────────
   derive    (halo) affordances at f1 under ctx after 21, session conditions relaxed:
               rel   needs [sess :mode = :relate]    → offer "relate"
               insp  needs [sess :mode = :inspect]   → offer "inspect"
               sel   needs [sess :selected absent]   → not offered; already selected
             conclusion. Painted as the halo. Nothing was listed anywhere.
 22  sess  :presented  {:by [halo 8] :to alice :support [point 8, 20, 21, pattern {:attr :consumer :kinds ∋ :pointed} as-of 21]}
                                                                          rt      S      [= support]              20   nil

── 5 · "relate this to that clause" ────────────────────────────────────────────────────────
 23  sess  :mode       :relate                                            alice   S      [point 22 ✓]             nil  nil
 24  sess  :pointed    {:target c1 :via 19}                               alice   S      [point 19 ✓]             nil  nil
   trigger   event: 24 → match :pointed:
               halo ✓ · sel ✗ (selected present) · rel ✓ (selected present, mode relate) · insp ✗ (mode ≠ inspect)
             claim-yielders: {rel}. One.
   read      (rel) sess :selected → 21 ✓ · f1 :source → 2 ✓ · c1 :text → 5 ✓
   derive    a relation will be pointed at later, to mark it and to re-assert it, so it needs an id.
             rel1 is minted. Minting is this claim. There is no separate mint fact. A thing is an id.
   gate      shape ✓ grammar :relation · expected nil = current nil ✓ · policy pol-A: alice → A ✓
 25  rel1  :relation   {:from f1 :to c1 :how :governed-by}                alice   A      [point 10, 21, 24, 2 ✓, 5 ✓]  24  nil
 25d claim-25 :decision {:outcome :admitted :seq 25 :rule pol-A}          gate    base   [point grammar-rel, point pol-A]

── 6 · the relation appears with who made it and when ──────────────────────────────────────
   trigger   basis moved: 25 grows the pattern read in 19's support (relations among {p1 f1 c1} as-of 18).
   read      as in step 2; the pattern now → {25}
   derive    surface = [p1@18   f1@2   c1@5   rel1@25 · alice · t25]    who and when are slots on 25. Nothing extra.
 26  sess  :presented  {:by [s1 7] :to alice :support [point 7, 12, 19, 13, 18, 2, 5, 25, pattern rel as-of 25]}
                                                                          rt      S      [= support]              25   nil

── 7 · they notice the check result is missing ─────────────────────────────────────────────
   nothing enters. Noticing is not a fact. Check: 6 is not in 26's support; s1's body never read chk1.

── 8 · they point at the surface itself; its definition opens ──────────────────────────────
 27  sess  :mode       :inspect                                           alice   S      [point 26 ✓]             nil  nil
 28  sess  :pointed    {:target s1 :via 26}                               alice   S      [point 26 ✓]             nil  nil
             the surface's own frame is addressable because 26 says :by [s1 7]. Same crossing as step 3.
   trigger   event: 28 → halo ✓ · sel ✗ (mode present) · rel ✗ (mode ≠ relate) · insp ✓
             claim-yielders: {}. insp yields a conclusion. No miss.
   read      (insp) s1 :consumer → 7 ✓
   derive    editor over the value of 7.      conclusion. Kind two: the pointed thing is a definition.
 29  sess  :presented  {:by [insp 11] :to alice :support [point 11, 28, 7]}
                                                                          rt      S      [= support]              28   nil

── 9 · add a read and a place to paint it; save ────────────────────────────────────────────
   claim     {:thing s1 :attr :consumer
              :value {… body + "chk1 :result of f1; paint beside f1"}
              :actor alice :layer A :basis [point 7 ✓, point 29] :request 28 :expected 7}
   gate      shape ✓ grammar :consumer. Floor derives regime from leaves: inside, instant. :conclusion allowed ✓
             expected 7 = current row (s1 :consumer A) → 7 ✓
             policy pol-A ✓
 30  s1    :consumer   {… new body …}                                     alice   A      [point 7 ✓, point 29]    28   7
 30d claim-30 :decision {:outcome :admitted :seq 30 :rule pol-A}          gate    base   [grammar-consumer, point 7, pol-A]

── 10 · the surface shows the result; the next click uses the new rule ─────────────────────
   trigger   basis moved: 30 supersedes 7, and point 7 is in 26's support. The definition was read by reference.
   read      name s1 → 30 ✓ (resolution at use, next pass). New body: chk1 :result → 6 ✓, plus the rest.
   derive    surface = [p1@18   f1@2 ·pass   c1@5   rel1@25]
 31  sess  :presented  {:by [s1 30] :to alice :support [point 30, 12, 26, 13, 18, 2, 6, 5, 25, pattern rel as-of 30]}
                                                                          rt      S      [= support]              30   nil
             ▲ the pass boundary, as data: 26 is :by [s1 7]; 31 is :by [s1 30]. Between them one fact landed.

── 11 · overnight someone commits; the function changes ────────────────────────────────────
 32  ev2   :commit     {:ref git:c3e :author bob}                         host    base   [outside git:c3e]        nil  nil
   trigger   event: 32 → match :commit → ingest (outside regime; may only yield claims)
   read      (ingest) f1 :continuity → 3 ✗    the name "f" matches; the hash does not. This read is not exact.
 33  f1    :source     "(defn f [x y] …)"                                 ingest  base   [point 32, point 3 ✗]    32   2
 34  f1    :continuity {:name "f" :hash h2 :prev h1 :by :name}            ingest  base   [point 32, point 3 ✗]    32   3
   enactment ci runs on the host. The land never evaluates 33. The outcome returns as a claim:
 35  chk1  :result     {:of f1 :status :fail}                             ci      base   [point 33 ✓]             32   6

── 12 · morning: the relation is marked as made against the old version; the new one offered
   trigger   basis moved: 33 supersedes 2. Point 2 is in 25's basis and in 31's support.
   derive    (stl inside s1) stale?(25) → true, via point 2 → 33.   painted on rel1: "made against f1@2; f1 is now @33"
                             doubtful?(33) → true, via point 3 ✗.   painted on f1: "same function by name only"
             (s1) f1@33 · chk1@35 ·fail
 36  sess  :presented  {:by [s1 30] :to alice :support [point 30, 12, 31, 13, 18, 33, 35, 5, 25, pattern rel as-of 35]}
                                                                          rt      S      [= support]              35   nil
   affordance a re-assert rule, not seeded above, with condition stale?(target). Its offer is the halo at rel1.
             if alice accepts:
 37  rel1  :relation   {:from f1 :to c1 :how :governed-by}                alice   A      [point 10, 33 ✓, 5 ✓, 25]  nil  25
             25 is now superseded. Whatever read 25 is stale. Correctly.

── 13 · 14 · ask a model to summarize; the answer says what it was given ───────────────────
 38  q1    :request    {:kind :summarize}                                 alice   A      [point 36 ✓]             nil  nil
             point 36 is what she was looking at when she asked. The journey's context, for free.
   trigger   event: 38 → match :request → lane (outside regime; claims only)
   read      (lane) q1 :members → 13 · p1 → 18 · f1 → 33 · c1 → 5 · rel1 → 37 · chk1 → 35
   cross     to a mind. Presentation across a boundary is a claim:
 39  req38 :presented  {:by [lane L] :to m1 :support [point 13, 18, 33, 5, 37, 35]}
                                                                          lane    A      [= support, point 38]    38   nil
   inward    m1's text arrives. The return manufactures its basis and its request from 39:
 40  q1    :summary    "f governs c1. Its check now fails. …"             m1      A      [point 39]               38   nil
             "what it was given" = 40 → 39 → :support. No rendered prompt is stored.

── 15 · what did I do yesterday, and what was I looking at ─────────────────────────────────
   conclusion. Not stored. journey(alice, yesterday):
     did 20   pointed f1               saw 19
     did 23   chose relate             saw 22
     did 24   pointed c1               saw 19
     did 25   related f1 → c1          saw 19 (via 24), 21
     did 27, 28  inspected s1          saw 26
     did 30   edited s1                saw 29
     did 37   re-asserted rel1         saw 36
     did 38   asked for a summary      saw 36
   history(f1) instead: 2, 3, 33, 34, and every fact whose basis touches them: 6, 25, 35, 37, 39.
```

A side branch the walk does not take, for concurrency and branching:

```
── bob re-asserts rel1 in layer A first ────────────────────────────────────────────────────
 36' rel1  :relation   {:from f1 :to c1 :how :tested-by}                  bob     A      [point 10, 33, 5, 25]    nil  25
             admitted, if pol-A lets bob write A.
   alice's 37 arrives with expected 25. The current row is now 36'.
 37d claim-37 :decision {:outcome :rejected :reason :revision :lost-to 36' :rule pol-A}
                                                                          gate    base   [point 36', pol-A]
   nothing merges. Alice's reply is a context: a candidate layer C over A.
 37' rel1  :relation   {…}                                                alice   C      […]                      nil  nil
             admitted in C. Nearest active wins for her: C over A → 37'. For bob: A → 36'.
   promotion of C into A is a claim. Its conflict with 36' is a miss fact, not a blocked door.
   had bob written his own layer B, neither is rejected: two contexts, diffable by re-deriving under each.
```

★ Insight ─────────────────────────────────────
- The trace is a falsification instrument, not an illustration. Every step had to land on a seq with a basis, or the frame would have needed a slot it does not have. Seven times it needed a choice. Zero times it needed a slot.
- The last-looked-at state, the journey, and the pass boundary all came out as reads over presentation facts. Nothing was captured for them. They are patterns over crossings that were already recorded, which is the claim the picture's first insight made and the trace now backs.
─────────────────────────────────────────────────

## What the trace found

**Forced.** Writing it as data required a choice the prose did not make. Each is a position Sid can redline.

1. **Basis entries have three shapes.** Point read, pattern read with a frontier, outside anchor. The prose gives only the first. A read that returned absent is a pattern read, and it goes stale by growing, not by being superseded.
2. **Revision is the gate's sequence, per fact.** Superseded is the relation: same thing, attr, and layer, later seq, visible. Per-thing revision would make every conclusion stale on any change to the thing. A per-attribute counter would just be the seq again.
3. **Claim and fact differ by one slot each way.** Expected in, seq and time out. The gate's whole transformation is that swap plus three checks.
4. **There is no producer slot.** The consumer that produced a claim is in the basis, because resolving its name was a read. Actor is whose authority it ran under. This is what lets an agent's claim be attributable to the agent and traceable to the rule at once.
5. **Multiplicity lives in things, never in attributes.** A relation is a thing because something will point at it. Every thing, attr, and layer has one current row, so expected revision is always meaningful and cardinality never enters the envelope.
6. **Exactness belongs to resolution, never to anchoring.** An outside anchor is what it is. The inexact read in the trace is ingest reading the old continuity fact by name. Continuity's method goes in the claim's value as `:by`, and exactness stays two valued. That is a position on the picture's open question about a third value.
7. **The miss condition is narrower than "two rules match."** It is two claim-yielding consumers matching one trigger in one layer. Conclusions never compete because they are not choices. Without this narrowing, every point that triggers both a halo and a selection is a miss.

**Revealed.** The prose implies it; the data says it out loud.

8. **The presentation claim is the last-looked-at record.** "Moved on since you last looked" is staleness of the previous presentation. There is no other state.
9. **Request is nil when a claim originates a pass.** Every bare outside arrival originates: a commit, a tick, a pointer. A return through the outside inlet continues a pass only when the presentation it answers was made under a request still open, and then the inward return copies that request. Pointing at a watched surface originates. A model's reply to a prompt continues. Both are visible in the envelope without an inlet slot.
10. **Affordance is match with session conditions relaxed,** painted with what each rule still needs. The prose says "match now." After a selection with no mode set, match now offers nothing, so the walk needs match one context fact ahead.
11. **Staleness is one rule with two consequences.** On a fact it is a mark. On a conclusion it is a re-trigger.
12. **Doubt propagates by a view that follows basis chains,** not by a property of a read. Exactness is local. The map-must-not-lie painter walks the chain.
13. **Decisions are frozen facts with a basis,** so "which past decisions would go differently under current policy" is derivable. The prose does not say this. The shape gives it.
14. **The pass boundary is two rows apart.** One presentation is by the surface at its old seq, the next is by the surface at its new seq. Demonstration item six is checkable by reading two facts.
15. **A consumer is triggered by match or invoked by reference, and both are reads.** The staleness view had to be invoked from the surface's body rather than matched on presentations, or it would trigger on its own output. Reference is how a definition participates in a pass without being a trigger, and it leaves support like any read.

**Still open after the trace.**

16. Whether every recompute that reaches a person is a presentation claim. That is the volume driver. The tier axis is what would keep it cheap, and that is a store concern the trace cannot settle.
17. Where decisions live and whether they take seqs from the same sequence. Store concern.
18. What the request of a demand-opened pass is when no single fact triggered it. The trace uses the fact whose landing started the pass and puts the standing watch in the basis.

**What the walk exercised** of the nine demonstrations:

| demonstration | exercised | where |
|---|---|---|
| 1 point at a fact, follow basis to reads | yes | 40 → 39 → support; 25 → 2, 5, 21, 24 |
| 2 conclusion shows status, staleness, exactness | yes | marks at step 12; doubtful via 3 ✗ |
| 3 inside and outside pass one gate; decisions carry rule and basis | yes | 20 and 32 from outside, 25 and 30 from inside; 25d, 30d, 37d |
| 4 add a consumer by asserting signature and body | yes | seeds 7 to 12 are ordinary claims |
| 5 add a kind by asserting grammar and placement | no | the walk asserts no new kind |
| 6 editing a definition changes the next pass only, boundary visible | yes | 26 by [s1 7], 31 by [s1 30] |
| 7 rebuild leaves a running-revision claim | partly | 35 is a check outcome; no running-revision claim |
| 8 two contexts diffed by re-deriving | gestured | side branch, layers A and C |
| 9 a miss is a fact | no | the seeds were made exclusive; no miss occurred |

Five fully, two partly, two not. The walk is a strong test of the read side and a weak test of kinds, rebuild, and misses. A second walk should be built to hit five, seven, and nine.

**The open questions, with leans:**

| question | what settles it | picture's lean | session's lean |
|---|---|---|---|
| placement as asserted rule or maintained conclusion | whether any index must outlive its rule | doubts it | maintained; an index is a conclusion over its rule |
| migration when a grammar or the envelope changes | the first one | | an enactment with an owner; detection is already drift |
| exactness with two or three values | | a third mark | two, with `:by` in the continuity value |
| progress from long consumers as claims or only outcome | the first rebuild activity | | claims, with the request copied; the trace's request rule already carries it |
| a runner as material, admitted not deployed | its own evidence bar | | not architecture now |
| erasure as enactment | the first real second person | tombstone above, drop below | same; the tombstone is a fact, the drop is a crossing |

**What stays prose,** because it is argument and no notation improves it:

- Why only replayable bodies may emit conclusions.
- Why the pass boundary tames self-reference. It is an argument about when names resolve.
- Why this and not event sourcing, Datalog, entity systems, or blackboards.
- What must remain outside forever, and why self-editing depth is free until the runtime.
- Who chooses shapes, and the leaf vocabulary as the gauge.
- The pressure positions: torn reads, a lying enactor, erasure, federation, agents as inhabitants.

Each of these is short in the picture and should stay short.

**The next representation up.** An executable reference model: the envelope as data, the rules as functions, the seed and the walk as a vector of claims, and a run that folds them through the gate and match. Roughly two hundred lines of Clojure. It would take the session's hand out of the trace. The seqs, the supports, and the marks would be produced rather than written, which is where an error is most likely. It would also let demonstrations five, eight, and nine be tested directly: assert a kind, diff two layers, assert an overlapping rule and watch the miss fact land. It is not the implementation and should not become one. It is the frame's own claim about itself, made replayable. It has not been built.

Three of the forced choices, the basis shapes, no producer slot, and multiplicity in things, change what the first workpiece's facts must carry. The exercised list says which demonstrations the walk leaves untested.
