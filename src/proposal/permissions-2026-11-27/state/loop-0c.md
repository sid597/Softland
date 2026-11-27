status:     WAITING-ON-SID
commit:     44a0dda1+loop0c
verified:   Adversarial code + Rama break of CONTRACT.md & ALLOWLIST.md against src-store/, src-runtime/, PERMISSIONS.md §6, src-store/README.md, and window-a-contract-draft.md (5 breaks + 3 Fable wins identified)
next:       Sid: decide Calls 1-3 (Party ID, S5 rights snapshot query vs 11-12 Oct Entry 3, agent lend boundary) -> freeze CONTRACT.md + ALLOWLIST.md -> run Loop 1 || Loop 2A

# Loop 0c: Adversarial Break & 2-Family Comparison of `CONTRACT.md` + `ALLOWLIST.md`

## 1. Five Real Breaks Caught in `CONTRACT.md` (`44a0dda1`)

1. **`S1(a)` vector party `[:person uuid]` breaks `:e`, `:subjects`, and space-delimited keys (`sign_in.clj:136,154-155`, `act.clj:67,73,222`, `rows.clj:6-11`):**
   - Plain facts sit on `who` (`{:e who :k :person/name :v name}`) and `:subjects #{who}`, where `:id` accepts only `readable-keyword?` or `uuid?` (never vectors).
   - `(pr-str [:person uuid])` contains `" "` (ASCII 32), violating `rows.clj:9` ("No printed text of an id holds a space, so one space parts the parts") across `place-key`, `by-who-key`, and range scans.
   - *Fix:* Space-free party keywords `:person.<uuid>`, `:agent.<uuid>`, `:run.<uuid>`, `:system`, `:anyone`, `:anyone-signed-in` keep 100% of `S1(a)`'s properties while remaining valid space-free `:id`s everywhere (or kind-tagged `UUIDv8`).

2. **Collective holders `[:everyone ...]` allowed inside `:who` (`CONTRACT.md:78-80`) enables public-lane close exploit:**
   - Because `party` includes `[:everyone :anyone]` and `:act` sets `{:who party :under []}`, a sender can submit `{:who [:everyone :anyone] :under []}` to end a public grant as "its own holder", violating `PERMISSIONS.md:569, 985-986`.
   - *Fix:* Separate `author-party` (`person` / `agent` / `run` / `:system`, valid in `:who` and non-nil `:asker`) from `holder-party` (`author-party` + `:anyone` / `:anyone-signed-in`, valid in `:holder`).

3. **`S2(b)` deriving initial permission ID from `making-uuid` breaks `name!`, `setup!`, CLI, and server callers (`sign_in.clj:95, 140-157`, `command/main.clj:315`, `server/sign_in.clj:167`):**
   - After `enter!` completes, `Caller` carries `own-lane`, NOT `making-uuid`. Deriving the initial permission UUID deterministically from `lane` (`UUIDv8("softland/first-permission/" + id-text(lane))`) works in `enter!` AND all post-login callers with zero `Auth` schema changes and zero derivation from `person` (9 Oct Entry 1).

4. **Renaming `:permission` -> `:under` and splitting `:softland.store/permission` into `:give` / `:give-terms` / `:give-end` (`CONTRACT.md:80-85`) breaks 9 Oct Entry 4 and triples RocksDB seeks & ancestor proxies:**
   - `src-store/README.md:463-479` records Sid's explicit word `:softland.store/permission` and `:permission` on `:act`, and Sid pushed back on `"give"` as a noun (`‹7cd204aa›`).
   - Three places per permission cost **3 RocksDB point seeks** per permission check ($3d$ seeks along a `:lent-from` chain) and **3 `foreign-proxy-async` handles per ancestor** in live reads. One `:softland.store/permission` map (`{:holder :allows :covers :session :given-by :under :lent-from}`) preserves immutable `:given-by`/`:under` on in-place change, distinguishes `never-given` / `ended` / `used-up`, and costs **1 seek** and **1 proxy**.

5. **Rama `acts.clj` `:base` routing mismatch + missing `:lent-from` depth/cycle bound (`CONTRACT.md:86-87, 116-122`, `acts.clj:12-16`, `walk.clj:82-95`):**
   - `acts.clj:12-16` routes `:base` act-line reads to `:base`'s deciding task (not `home`) so rights align with `:last-stamp` and `$$microbatch-line`.
   - Non-yielding `walk/decide` and the rights query topology require an explicit `:lent-from` chain cap (`max-depth = 32` + cycle guard) so flat UUID chains cannot stall a single-threaded task.
   - `allows` union (`map | :everything`, `:give :all`) mixes types and cannot express partial pass-on (`:give #{:see}`); uniform map `{:see :write :give :owner}` handles all day-1 and future values cleanly.

## 2. What `Window B` (`Fable max`) Nailed — Keep in Frozen Contract

1. **`:exact-act` tag in `:covers` (`[:exact-act <64-hex-digest>]`):** Keeps `:act` free for permissions over existing acts (`‹45c7127b›`).
2. **`<sitting-or-*>` segment in holder-index key:** Lets a reader in session `S` watch only `*` + `S` prefixes so grants in other sittings never wake `S`.
3. **Public-lane `:lent-from` end question:** Clarified that only the party that *is* `:holder` counts as lender-holder under Item 18 (strangers covered by `:anyone` cannot end another person's agent borrow `:lent-from` the `:anyone` grant).
4. **411-sentence `ALLOWLIST.md` categorized into `BUILD.md`-forced vs. contract-default** (plus we will verify `pure_proof.clj:915` `:who` `:id-too-long` and the boundary checks in code when freezing `ALLOWLIST.md`).
