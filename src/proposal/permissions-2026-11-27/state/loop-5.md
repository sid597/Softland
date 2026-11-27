status:     DONE
commit:     recorded in git log on main (this commit)
verified:   blind cluster breakers (break_gates 20/20 + break_reads_runtime 30 probes) run & re-verified; 2 read-path breaks fixed & proved; all 8 test-store proofs (2,882 assertions), gate-histories (2000 1), test-wire, test-auth, test-server, test-runtime + 7 attacks, read/gate measures, and 102/102 refusal keywords 100% green
next:       Sid morning review: (1) approve drafted Decisions entry below for src-store/README.md; (2) review 3 mechanical ALLOWLIST additions in state/loop-4.md; (3) decide Open Calls A (:anyone-signed-in :give/:owner) & B (live same-act write+revoke) if desired

# Loop 5: Blind Cluster Break, Fixes, Cold Diff Read, and Drafted `src-store/README.md` Decisions Entry

## 1. Blind Cluster Break & Fixes Landed

Two uncontaminated Loop 5 Breaker subagents attacked `Store`, `door`, `read-path`, `Auth`, `runtime/seam`, `sign-in`, and the `command` CLI on 4-task Rama `1.6.0` `InProcessCluster`s behind `cluster.lock` ([loop-5-break.md](file:///mnt/data/projects/Softland/src/proposal/permissions-2026-11-27/state/loop-5-break.md)):

1. **Fixed — `read-acts-once` torn read between `own-rows` and `"lane-rights"` (`lanes.clj`, `acts.clj`):**
   - `read-acts-once` previously read `:softland.store/who-may-read-acts` from `own-rows` and `:standing` from `"lane-rights"` in two separate calls, allowing an atomic act flipping `[:who-may-read-acts :members, open :anyone :see]` to combine old `:everyone` with new `:visitor`.
   - Fixed in `src-store/softland/store/read_path/lanes.clj` and `acts.clj` so `:acts` `"lane-rights"` reads `:softland.store/who-may-read-acts` inside the same single-turn task snapshot (`leaked-things=0`, zero extra RPCs), with a permanent cluster regression check in `test-store/softland/store/read_path_proof.clj` (`a-read-of-acts-reads-standing-and-setting-at-one-moment`).
2. **Fixed — Direct `"lane-rights"` `*asker` party guard (`lanes.clj:95`):**
   - Tightened `lanes/valid-rights-query?` from `declaration/usable-id?` to `(and (act/author-party? asker) (declaration/id-within-the-limit? asker))` so direct foreign calls with `:anyone`, `:anyone-signed-in`, `:asha`, or raw UUIDs return `nil` immediately (`CONTRACT.md` §2.1 & §2.4), proved in `permission_proof.clj` and `read_path_proof.clj`.
3. **Fixed — `permission_proof.clj` namespace docstring (`permission_proof.clj:14-18`):**
   - Removed the stale Loop-1 note stating the module and live read were not yet built.

## 2. Cold Check Against `PERMISSIONS.md` §6 (Items 1–26) & `BUILD.md` (Batches 1–4)

- **Batch 1 (give ID, multiple gives per holder, never-given vs ended vs used-up, self-end):** Held across pure, `gate-histories`, and cluster proofs; live viewer push count measured (`4` shared on `:anyone` open lane, `5` lent agent, `6` standing private member).
- **Batch 2 (standing vs lent, `:give` right, borrowing by agents/runs, 4 enders, lowered-giver revocation guard):** Held across pure, `gate-histories`, and cluster proofs (up to depth `32`, cycle-free).
- **Batch 3 (1-act atomic multi-change, in-place `change` preserving ID/holder/giver/lender and updating `permissions-by-holder` stamp, owners & 1-act lane hand-over):** Held across `:streaming` and `:base`.
- **Batch 4 (`:anyone` & `:anyone-signed-in` in the log, no code-constant public access, §12 statement-to-prove with named exceptions, non-flicker 1-act live transfer on `:streaming` and cross-task `:base`, `:system` initial owner of `:base`):** Held end-to-end across store and runtime suites.
- **`src-store/README.md`, `ALLOWLIST.md`, `gate-histories/`:** Zero edits made (`git diff baa19ffe..HEAD -- src-store/README.md src/proposal/permissions-2026-11-27/ALLOWLIST.md` empty on `ALLOWLIST.md` since Loop 0c and empty on `src-store/README.md`).

## 3. Open Items for Sid's Morning Review

1. **Owner ending another owner's permission while an owner remains (`BUILD.md:149-150`):** Implemented per items 18 + 21 read together; Sid was not asked this combined case directly (`PERMISSIONS.md` §12 open question 4).
2. **`:anyone-signed-in` `:allows` upper bound (`loop-5-break.md` Open Call A):** Locked `gate-histories` Reading #6 (`gate-histories/README.md:447`) holds that `:anyone` allows `{:see true}` only while `:anyone-signed-in` holds whatever it is given. If an owner grants `owner-allows` to `:anyone-signed-in` and ends her own owner give, any signed-in visitor can claim sole ownership in 1 act. Capping `:anyone-signed-in` to `{:see, :write}` (and excluding `act/everyone` from `owner-of-the-lane?`) is a 4-line change in `decision.clj` + `gate-histories/reference.clj` if Sid wants it turned.
3. **Same-act `[write-secret, revoke-viewer]` push callback order (`loop-5-break.md` Open Call B):** Under `Loop 0d`'s separate plain pushes, revoking a viewer in act $N$ and writing confidential data in act $N+1$ (or `read-once`) withholds the new fact, whereas bundling a new fact and a revoke in the *same* act can deliver the plain fact push before the rights push arrives (`{:changed ...}` followed immediately by `{:anew {:left-out [{:why :may-not-see}]}}`).
4. **Three purely mechanical check-sentence updates outside `ALLOWLIST.md`:** Listed with exact `file:line`, `BEFORE`, and `AFTER` in [loop-4.md](file:///mnt/data/projects/Softland/src/proposal/permissions-2026-11-27/state/loop-4.md) (`pure_proof.clj:1171`, `read_path_pure_proof.clj:2688`, `read_path_pure_proof.clj:2708`).

---

## 4. Drafted Entry for `src-store/README.md` (DO NOT WRITE UNTIL SID APPROVES)

> Per `BUILD.md:48-49`, `AGENTS.md` ("Who decides what"), and `PERMISSIONS.md` §6 item 15, this dated section is drafted here for Sid's review and has **not** been written into `src-store/README.md`.

```markdown
## Decisions of 26 and 27 November 2026 — a permission has an id, what is lent or stands, owners, and everyone

Sid made these on 26 and 27 November 2026 across three passes: in the
permissions working session, in the design-pitfalls review session of the same
night, and in the permissions build orchestrator session on 27 November. The
page that holds his words and the session readings built on them is
`src/proposal/permissions-2026-11-27/PERMISSIONS.md` (section 6, items 1 to 26;
section 12), with its four-batch build sheet in
`src/proposal/permissions-2026-11-27/BUILD.md` and its frozen stored shapes in
`src/proposal/permissions-2026-11-27/CONTRACT.md`. Words in quotation marks
with an id of the record are lines that he typed; replies from the working and
orchestrator sessions whose record ids had not yet been indexed when
`PERMISSIONS.md` was written are given word for word as recorded there. What a
session drew from an answer is marked as a session's, and what stays open is
named. All of the first slice is built across `src-store/` and its callers, and
the tenth making of `test-store/softland/store/run_again_fixture/` was made for
it.

1. **A permission is a give on an id of its own, an act names the give it acts
   under, and a holder can have any number of permissions in a lane.** Sid
   decided: on multiple permissions per holder, "each instance can have
   different permisiion based on different uses it has or work so yes"
   (‹95754ddf›, item 1; entry 6 of 20 and 21 November); on an act naming the
   give's id instead of carrying the permission written out inside its digest,
   "5. yeah" (‹d0f62e81›, item 5). It replaces the written-out vector
   permission of 6 October and 20–21 November, the one-permission-per-holder
   place `(lane, holder)`, and the separate keys
   `:softland.store/run-permission` and `:softland.store/act-permission`. What
   comes with it, in the stored form (`CONTRACT.md` sections 2.1 to 2.4):
   - A give is a fact under `:softland.store/permission` whose entity `:e` is a
     UUID that its giver makes (and for a lane's first owner permission and its
     first `:anyone` permission, a deterministic version-8 UUID derived from
     the lane's id in `softland.store.gate.first-act`). Its value holds
     `:holder`, `:allows`, `:covers` (`[:default-group lane]` or
     `[:exact-act digest]`), `:given-by`, and when present `:session`, `:under`
     and `:lent-from`.
   - An act carries `:permission` as an optional UUID: omitted (`nil`) on a
     lane's making act and on an act that only ends permissions its author
     holds, and one UUID on every other act today. Each row of a fact holds the
     36-character text of that UUID, or no `:permission` when `nil`.
   - An id takes one permission for good: a permission never given, one that
     ended, and one for an exact act that was used are answered apart, and
     giving again after an end or a use is a new permission on a new UUID.
   - Each gate keeps a seventh table, `permissions-by-holder` (copied to every
     task in the base's gate), mapping each standing or changed default-group
     permission's `[lane, holder, session-or-*, id]` key to the stamp of its
     latest grant or change, and removing it on end.

2. **A party's id in the store says what kind of party it is.** Sid decided:
   "1a" (item 22), choosing visible party prefixes in the store while Auth
   keeps plain UUIDs and login `:session` is not renamed into a run. An author
   or an asker is `:person.<id>`, `:agent.<id>`, `:run.<id>`, or `:system`; a
   permission's `:holder` is one of those or everyone (`:anyone`,
   `:anyone-signed-in`), and everyone holds and never acts or asks.

3. **See and write are the two rights; passing to another person needs the
   right to give, while lending what you hold to an agent or a run comes with
   holding it.** Sid decided: on see and write as the two atoms, with what may
   be given on composed inside the permission, "2. yeah this makes sense to me"
   (‹d0f62e81›, item 2); on splitting lending to one's own agents and runs from
   sharing with another person, "1. agree with your 2 split" (item 19); on
   keeping `Auth` `made-by` out of the gate and `seam` in this slice, "yeah
   confirm all 4 ..." and "yes" (item 24); on separating inviting named people
   from publishing to `:anyone` or `:anyone-signed-in`, "I go with option b as
   stated" (item 26). What comes with it:
   - `:allows` holds some of `:see true`, `:write :all`, `:give #{...}`, and
     `:owner true` (which requires all rights, `:everyone` in `:give` included).
   - Whoever holds `:see` or `:write` may lend it (`:lent-from` naming the give
     acted under) to an `:agent.<id>` or `:run.<id>` without holding `:give`.
     Passing a right to a `:person.<id>` requires that right inside `:give`;
     granting or raising for `:anyone` or `:anyone-signed-in` also requires
     `:everyone` inside `:give`, passed on only when the giver's `:give` holds
     both `:give` and `:everyone`.
   - Not built in this slice: runs as declared parties with start/end acts
     (items 8 and 11), write narrowed by kind of act, and checking whose agent
     an agent is at the store boundary. Today's login sitting stays on
     `:session` until runs land.

4. **A give to a person stands on its own unless lent; a give to an agent or a
   run is always lent, is capped at each moment by what its lenders hold, and
   falls when any lender above it ends.** Sid decided: "5. yeah" (‹d0f62e81›,
   item 5). It replaces entry 1 of 20 and 21 November for people invited into a
   lane: a person whose permission stands on its own (`:lent-from nil`) stays
   when the one who let her in loses hers, whereas a lent permission (`:lent-from`
   set, mandatory for `:agent.<id>` and `:run.<id>`, bounded at 32 in
   `softland.store.gate.chain`) never allows or covers more than every
   permission above it allows right now, and falls when any of them ends. What
   is made under a lent permission, or raised on a lent permission, is lent
   from it.

5. **Four parties may end a give, and a change in place keeps a give's id.**
   Sid decided: on who may end a give that is not their own, "2 yes,"
   (‹b09854a6›, item 18), and on demoted givers, "1. agree" (item 26). A
   permission is ended by: (a) its `:holder` (including a see-only holder, in
   an act naming that permission or `nil`); (b) its `:given-by`, in an act
   under the `:under` permission she gave it with, while that permission still
   stands and still lets her pass what the target holds to its holder; (c) the
   `:holder` of a permission it is `:lent-from`, directly or further up with no
   ended link between; or (d) an owner of what it covers, under her owner
   permission. Being covered by `:anyone` or `:anyone-signed-in` makes nobody
   its `:holder` for ending another person's borrow (item 24). A session's
   derivation (`PERMISSIONS.md` section 12, point 2; `BUILD.md` batch 3):
   writing a new `:allows` at an existing permission's UUID changes it in place
   without ending what was lent from it, requiring what giving needs for rights
   added and what ending needs for rights taken away.

6. **Several permission changes in one act pass or fail as one; owning is a
   give apart from the maker; a lane always keeps a standing owner and can be
   handed over in one act.** Sid decided: on atomic multi-change acts and
   one-act transfer, "7. yes" (‹d0f62e81›, item 7); on separating owner from
   maker, "6. yes this was mistake, glad we caught it" (‹d0f62e81›, item 6); on
   multiple owners acting alone, keeping at least one owner, and handing over a
   lane in one act, "4. both yes" (‹e17932a7›) and "4. your recommendation"
   (item 21). It replaces entry 3 of 20 and 21 November ("only the layer's root
   holder writes its settings") and the rule that a lane's first permission
   never ends:
   - Each change in one act is checked against what its author held just before
     the act, and the result is checked as a whole: no permission named twice,
     no new permission left `:lent-from` one the same act ends, and at least
     one non-lent, non-sitting default-group `:owner` permission remaining in
     the gate's `:owners` set.
   - Any owner writes the lane's settings (`:softland.store/builds-on`,
     `:softland.store/marked-keys`, `:softland.store/who-may-read-acts`).
   - A session's reading of items 18 and 21 together, not put to Sid as its own
     case (`PERMISSIONS.md` section 12, open question 4): one owner may end or
     lower another owner's permission while at least one standing owner
     remains.

7. **"Seen by everyone" is a permission in the log to `:anyone` or
   `:anyone-signed-in`, and live views read permissions as one atomic unit.**
   Sid decided: on everyone as a holder in the log rather than a lane setting
   or a constant in code, "3. yes" (‹e17932a7›) and "3 yes" (item 20); on
   leaving `:softland.store/who-may-read-acts` in place for now (written by any
   owner), "yes" (item 25); on reading permissions as one atomic unit on the
   lane's task and validating live views against the read set's stamps after
   the adversarial caching examination (`Loop 0d`), "2. can't we do them as a
   single atomic unit?", "go", and "1. agree" (items 23 and 26). What comes
   with it:
   - `:softland.store/who-may-see` is gone, and no value in code opens `:base`
     or any other lane; `:base` stays owned initially by `:system` and is
     opened by granting `{:see true}` to `:anyone` in the log. `:anyone` holds
     seeing only; `:anyone-signed-in` covers signed-in people (never agents or
     runs directly, though a person may lend everyone's permission to an agent
     or a run under item 24).
   - Each gate exposes the single-turn query topology `"lane-rights"`, which
     returns `{:standing ... :read-set ...}` (and `:who-may-read-acts` for a
     read of `:acts`). A live read holds that standing only while its open Rama
     pushes match `:read-set` entry for entry (`:not-known-yet` otherwise), so
     a one-act permission transfer never shows a live viewer an intermediate
     `:may-not-see` state, on a streaming lane or on `:base` read from another
     task.
   - Open (`PERMISSIONS.md` section 9 and section 12): groups smaller than a
     lane, runs as parties and taking `:session` off the act, `refer`, conflict
     raising on two replacements of one fact (item 10), who sees refused acts
     and the store's own facts (items 11 and 12 of section 12), whether one
     owner may end another owner, and whether `:anyone-signed-in` is capped to
     seeing and writing alone.
```
