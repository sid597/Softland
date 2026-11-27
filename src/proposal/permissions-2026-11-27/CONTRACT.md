# Contract: the shapes of the permission fix

Loop 0b, 27 November 2026. A session's page (Window B), derived from
`PERMISSIONS.md` (P) sections 6 and 12, `BUILD.md`, `LOOP0-EMPIRICAL-FACTS.md`
(L) and today's code; `PLAN.md` and `HANDOFF-implementer.md` were not read.
Shapes, PState schemas and query signatures only: what a gate or a read does
with them is `BUILD.md`'s. Nothing here is built or run. B1 to B4 name the
batch in which a shape lands.

## 1. Sid's explicit calls

**Made.** Section 2 rests on these and on nothing else of his. P section 6,
items 1 (any number of parties on a lane, and of gives each), 2 (two atoms,
see and write), 3 (what a give covers is apart from the lane), 4 (no field
for how long), 5 (who gave it and what it is lent from are two things; "lent
from" is one id on the give; an act carries the id of its give), 6 and 21
(owning is a right), 8 and 11 (a run is a party, in no batch; whom it acts
for is said per give), 19 (lending to one's own agents) and 20 ("everyone"
holds, as anyone and as anyone signed in). His second reply to 19 to 21,
which P could not yet cite, is ‹2d49ab0b›. `src-store/README.md`, 9 October
entry 1 (a person's id is a UUID that says nothing) and 11 and 12 October
entry 3 (a read is done in the runtime, straight from the tables).

**His to make.** A shape of section 2 waits on each; (a) is what it takes.

- **S1. A party, with its kind.** Item 5 needs a gate to tell a person from
  an agent. (a) `[kind id]` wherever a party acts or holds. Auth's ids stay.
  One UUID under two kinds is two parties, so a holder named with the wrong
  kind is a give nobody can use. Every act and give says in plain what kind
  its author and holder are, and `:who`, `:asker` and the texts in rows and
  keys change form, once. (b) The kind in the id's own bits: no form
  changes; Auth must mint by kind, `:system` needs a rule of its own, each
  new kind a code point, and the id says something. Not offered: the kind
  looked up in Auth or in a fact. A gate reads only its own lane's rows.
- **S2. What Auth keeps.** A personal lane's making act is built again at
  each try from what Auth fixed at her first `enter` (`sign_in.clj:89-101`),
  and now names a give's id. (a) Auth fixes a fourth value, `making-give`:
  four records and the `enter` steps of Auth change. (b) The runtime derives
  it from `making-uuid`: Auth stays; a formula that changes between two
  tries leaves a lane that cannot be confirmed. Either way who made an agent
  (`made-by`) stays in Auth and enters no act.
- **S3. `:lent-from` on each give (items 5, 8, 11, 19).** Decided. Not shown
  to him: the store cannot tell whose an agent is, so "her own agents" is
  any agent id she lends to, another person's too. (a) Leave it; no shape.
  (b) The door vouches on the act that the agent is beneath the giver: one
  door-added part, and a read of Auth at each lend. Not offered: an owner
  kept on the party, against items 8 and 11.
- **S4. `:sitting`, apart from `:covers`.** Today's permission "for a run"
  is good for one login sitting, and holds that in the slot for what is
  covered. (a) A field of its own until runs are parties: a stored word that
  is to leave again, beside the act's `:session` for the same id. (b) No
  such give in the slice: what is built is gone until then. Not offered:
  the field named `:run`.
- **S5. Rights by one query.** It changes 11 and 12 October entry 3 for a
  read's rights; facts stay straight from the tables. Rights read as rows of
  their own showed `:may-not-see` inside one act of transfer (L 1, 2); one
  query on the task showed none in 30 swaps (L 3). (a) `gives-that-stand`,
  with pushes as the signal to ask again. The runtime then holds the query's
  last answer, which Rama does not keep level as it does a push's value: the
  root page's stop on caching asks for its own examination first. (b) Rights
  from pushes as today: no kept answer, and by L 1 B4's "never a state in
  between" does not hold.
- **S6. Access in a setting.** `:softland.store/who-may-read-acts` says who
  reads a lane's acts whole and starts from a value in code
  (`first_act.clj:76`). B4 says no access comes from either. No reply of
  his names it, and this page leaves it.
- **S7. Words.** `give`, `give-terms`, `give-end`, `:lent-from`, `:sitting`,
  `:everything`, `:default-group` and `:exact-act` are sessions' words in
  what every act keeps (‹7cd204aa›, "what is a give?"; P section 9, item 14).

## 2. Engineering defaults

A session's, each his to turn. They fit today's code as read on 27 November.
No real data is kept, so none is fixed for good yet.

```clojure
;; party (S1). Kept free: [:run id] (item 8), [:group id]
[:person uuid] [:agent uuid] [:system :system] [:everyone :anyone] [:everyone :signed-in]
;; act, as its maker makes it: :permission is no part; the other parts stay
{:who party  :under [give]}                         ; none or one give today
;; a give: a uuid that its giver makes, and three places on it
{:e give :k :softland.store/give                    ; written once, for good
 :v {:holder party  :lent-from give  :sitting id}}  ; the last two can be absent
{:e give :k :softland.store/give-terms :v {:allows allows :covers covers}}
{:e give :k :softland.store/give-end   :v true}     ; written once: its end
allows = {:see true} | {:write true} | {:see true :write true}
       | {:see true :write true :give :all} | :everything
covers = [:default-group lane] | [:exact-act digest-hex]
;; :lent-from absent: it stands on its own; B1: always the :under give. :sitting absent: any
;; {:write true}: today's yes. {:see true :write true}: today's give for one sitting
;; :give: what may be given on. :everything: a lane's first give; B3: an owner's
;; the digest leaves :under out. [:everyone …] holds from B4
;; a making act: :under [], the depot fact, one give of :everything; B4: a give to everyone too
;; no store keys: the three of a permission (B1), :softland.store/who-may-see (B4)
;; a query: :asker is a party. Auth (S2a): :making {:uuid :session :give}
```

Three places, because the three differ in when they are written: who holds a
give never changes, its terms change (B3), its end comes once. Who gave it,
changed it or ended it, and under which give, are the `:who` and `:under` of
each row, which a gate writes. The implementer's record (P section 12) holds
all of it in one value, with the end as a fact with no value.

```clojure
;; softland.store.record.tables: one schema for both gates, as today
facts, facts-by-value  :who String (the party's text); :permission String -> :under UUID
line                   :who String
gives-by-holder        {String (fixed-keys-schema {:stamp Long :lent-from UUID})}  ; new
  ;; key "<lane> <kind> <id> <sitting or *> <give>": a row for each give that is not ended
  ;; and covers a group; :stamp is that of the last act that wrote a place of the give.
  ;; Streaming: on the lane's task. Microbatch: on every task, with the facts (walk/copies-of)
task                   :owners (map-schema String Long {:subindex-options {:track-size? false}})
  ;; B3. Key "<lane> <give>": the gives of :everything with no :lent-from
;; the use of a give for one act: today's row with no :by-index, at the give's end place

(<<query-topology topologies "gives-that-stand" [*routing-id *lane *party *sitting :> *held]
  (|hash *routing-id) … (|origin))                  ; no other partitioner (L 3)
;; *routing-id: the lane; for a microbatch lane the read's home (sources.clj, source-of)
;; *party: a party or nil. *sitting: an id or nil. Each is tested before a key is made of it
;; *held: {:made? boolean  :more? boolean            ; :more?: :gives was cut at a bound
;;         :gives [{:give uuid :holder party :sitting id :allows allows :covers covers
;;                  :chain [give]}]}                 ; as they stand, nearest lender first
;; a live read watches: the lane's own rows; in gives-by-holder "<lane> <kind> <id> " and
;; "<lane> everyone "; in facts the three places of each give of :chain
;; answer-by-act-id and depot-of-lane stay as they are
```
