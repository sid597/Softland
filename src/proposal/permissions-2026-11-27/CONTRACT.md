# Contract: Frozen Shapes, PState Schemas & Query Signature (Permissions Rebuild)

**Status:** FROZEN at Loop 0c (27 November 2026).  
**Scope:** Data shapes, ID conventions, bounds, PState schemas, and query topology signature **only**. Behavioral gate and read rules are **not** stated here; derive them from `PERMISSIONS.md` §6 and `BUILD.md`.

---

## 1. Sid's Explicit Calls (Binding)

1. **Permissions & store atoms (`PERMISSIONS.md` §6):** Any number of parties per lane and permissions per party (item 1); two atoms `:see` and `:write`, give only what you hold (item 2); `:covers` apart from lane order (item 3); no duration field, end is an act (item 4); `:given-by` (history) and `:lent-from` (optional permission UUID) on the permission record, and an ordinary act carries one permission ID (item 5); `:owner` is a right apart from lane maker, owners act alone, and a lane always keeps $\ge 1$ non-lent owner (items 6, 21); several permission changes in one act pass or fail together and replay idempotently (item 7); whom an agent/run acts for is per permission via `:lent-from` (items 8, 11); holding `:see` or `:write` lets a party lend to agents/runs while passing to another person needs `:give` (item 19); public access is a permission in the log to `:anyone` (reading) or `:anyone-signed-in` (writing), never to an agent/run directly (item 20).
2. **Party ID in the store (Loop 0c Call `1A`):** Space-free readable party keywords carrying party kind: `:person.<id>`, `:agent.<id>`, `:run.<id>` (minted in `runtime/seam` as `:person.<uuid>`, `:agent.<uuid>`, `:run.<uuid>` while `Auth` keeps plain internal `UUID`s; `:run.<id>` is reserved for real runs under Item 8 / F28 and **never** put on today's login `:session`), `:system`, and holder-only `:anyone` / `:anyone-signed-in`.
3. **Single-task atomic rights query + `Loop 0d` adversarial caching gate (Loop 0c Call `2`, `PERMISSIONS.md` §6 item 23):** Facts stay on direct PState pushes; rights are read as a single atomic unit on the task via one `<<query-topology` re-invoked on push invalidation signals (zero stored `:ancestors` copy). Before `Loop 2A` Part 2 or `Loop 2B` locks in live-read logic, a dedicated adversarial session (`Loop 0d`, honoring `AGENTS.md` stop on caching) first tests whether a push-only design on Rama 1.6.0 can avoid a remembered query answer altogether, and if not, attacks watch-set completeness (`[t_query, t_watch]` gap), per-open-view isolation, and proxy error/reconnect resync.
4. **Agent lending boundaries (Loop 0c Calls `3a` & `3b`):** Neither store nor `runtime/seam` adds an `Auth` `made-by` check on lends (`BUILD.md` Batch 2 stands as written; store shapes keep no maker slot on agent IDs). A permission held by `:anyone` / `:anyone-signed-in` is a normal permission UUID that a signed-in person may name in `:lent-from` for an agent/run (`BUILD.md` Confirmed clarifications).
5. **Store words & `:who-may-read-acts` (9 Oct Entry 4 + Loop 0c Call `4`):** Keep `:permission` on `:act` and `:softland.store/permission` as the store key. Sid confirmed leaving `:softland.store/who-may-read-acts` as it is in this build (except Batch 3 lets any `:owner` write it).

---

## 2. Engineering Defaults (Shapes, Schemas & Signatures)

### 2.1 Party IDs, Permission IDs & Deterministic Initial UUIDs
```clojure
;; Author party (:who on :act, :given-by on permission, non-nil :asker on query):
;; un-namespaced readable-keyword matching #"(?:person|agent|run)\..+" (<= 256 chars) or :system
author-party = :person.<id> | :agent.<id> | :run.<id> | :system

;; Holder party (:holder on :softland.store/permission — never valid as :who or :asker):
holder-party = author-party | :anyone | :anyone-signed-in

;; Permission ID: plain :uuid (maker-minted).
;; Initial lane permissions (deterministic from lane id-text alone — never from person, zero Auth schema change):
;;   first owner permission  = UUIDv8("softland/first-permission/" + id-text(lane))
;;   initial :anyone on open = UUIDv8("softland/first-anyone/"     + id-text(lane))
```

### 2.2 Maker's `:act`, `:arrived-act` & Store Keys (`act.clj`, `declaration.clj`, `digest.clj`)
```clojure
;; :act keeps its 10 part names; :permission becomes optional :uuid (nil/absent on making act or own-end);
;; act/rules = [] (:permission-id nested vector shape and :permission-is-in-the-acts-lane removed).
;; digest/hex-for-a-yes continues to strip [:act-id :depot :door-time :permission].
{:version 1  :who author-party  :lane id  :permission uuid-or-nil  :session id
 :based-on {fact-id carried-stamp}  :because-of act-id-or-nil  :claimed-when int-or-nil
 :subjects #{id}  :facts [fact ...]}

;; declaration/store-keys (8 -> 5; :run-permission, :act-permission, :who-may-see removed):
;;   :softland.store/depot             (:depot-word,             sits on lane,      setting, written once)
;;   :softland.store/permission        (:permission-value,       sits on perm-uuid, not setting, changeable)
;;   :softland.store/who-may-read-acts (:who-may-read-acts-word, sits on lane,      setting, written again)
;;   :softland.store/builds-on         (:lane-list,              sits on lane,      setting, written again)
;;   :softland.store/marked-keys       (:key-set,                sits on lane,      setting, written again)
```

### 2.3 `:softland.store/permission` Fact Value, End, Yes-Use & Bounds
```clojure
;; Grant or in-place change at {:e perm-uuid :k :softland.store/permission :v permission-value}:
{:holder    holder-party
 :allows    {:see   true                        ;; optional boolean
             :write :all                        ;; optional :all
             :give  #{:see :write :give :owner} ;; optional non-empty subset of passable keys
             :owner true}                       ;; optional boolean (at least one key in :allows present)
 :covers    [:default-group lane]               ;; or [:exact-act 64-hex-digest]
 :session   id                                  ;; optional :id (sitting scope until F28; omitted/nil = any)
 :given-by  author-party                        ;; granting act's :who
 :under     uuid                                ;; optional :uuid (granting act's :permission; nil when none)
 :lent-from uuid}                               ;; optional :uuid (nil = stands on its own)
;; Day-1 allows presets: see = {:see true}; sitting/yes write = {:see true :write :all} / {:write :all};
;;   member = {:see true :write :all :give #{:see :write :give}};
;;   owner  = {:see true :write :all :give #{:see :write :give :owner} :owner true}.
;; Explicit end: {:e perm-uuid :k :softland.store/permission :v nil} (fact-row with :value nil & :by-index).
;; Exact-act use: rows/use-row at (lane, perm-uuid, :softland.store/permission) (:value nil, no :by-index).
;; Chain depth bound: max-lent-depth = 32 (<= 31 :lent-from hops, cycle-free).
```

### 2.4 PState Schemas & Single-Task Rights Query (`tables.clj`, `walk.clj`, `module.clj`, `read_path.clj`)
```clojure
;; Existing 5 PStates (answers, facts, facts-by-value, line, things): schemas unchanged
;;   (:who String = id-text(author-party); :permission String = id-text(perm-uuid), absent when nil).
;; 7th PState per gate ($$streaming-permissions-by-holder, $$microbatch-permissions-by-holder):
;;   {String Long}  key "<lane> <holder> <session-or-*> <perm-uuid>" -> stamp
;;   (holds un-ended [:default-group lane] permissions; copied in walk/copies-of as :holder-permission & |all).
;; $$streaming-task & $$microbatch-task add:
;;   :owners (map-schema String (set-schema UUID {:subindex? true})
;;                       {:subindex? true :subindex-options {:track-size? false}})
;;   (lane-text -> standing non-lent :owner perm-uuids on deciding task; zero |all copy).

(<<query-topology topologies "lane-rights" [*routing-id *lane *asker *session :> *snapshot]
  (|hash *routing-id) ... (|origin)) ;; zero intermediate hops, zero yields
;; *routing-id: lane (:streaming fact reads & all acts.clj line reads incl. :base); home (:base fact reads).
;; *asker: author-party or nil; *session: id or nil; max holder-scan/chain bounded (max-lent-depth = 32).
;; Live-read invalidation watches (no :read-after): own-rows in facts; "<lane> :anyone * " and (when signed in)
;;   "<lane> <asker> * " + "<lane> <asker> <session> " in permissions-by-holder; and (lane, g, :softland.store/permission)
;;   in facts for each lender g along active :lent-from chains.
```
