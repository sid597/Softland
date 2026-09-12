## 5. Contract M — material citizenship

### 5.1 Admission template

Every atom family supplies one versioned citizenship descriptor:

```clojure
{:family/id <stable-keyword>
 :family/version <integer>

 :grammar {:schema/version <integer>
           :material-fields <closed-required/open-extension schema>
           :instance-fields <schema>
           :validation <fail-closed-validator-id+version>
           :defaults <explicit-versioned-defaults>
           :edit-operations <semantic-operation-ids>
           :serialization <canonical-encoding-version>
           :export-projections <formats+intentional-loss>}

 :pick {:geometry <Contract-G descriptor reference>
        :scene-order <Contract-O reference>
        :visibility <clip/mask/depth/opacity policy>
        :modalities <pointer/pen/touch/accessibility policies>
        :result <stable semantic identity + subpart/cluster route>}

 :provenance {:material-id <stable>
              :revision <stable>
              :parents <causal predecessors>
              :author/actor <human|agent|importer identity>
              :act <creation/edit/import/derive act>
              :source-assets <ids+digests+licenses where applicable>
              :derivations <source revision + algorithm/version/regime>
              :draft-settle <lifecycle record>}

 :versioning {:schema-version <integer>
              :algorithm-versions <semantic derivations>
              :migration <total explicit migration chain>
              :unknown-field-policy <preserve|reject, never silent drop>
              :cache-invalidation <source+algorithm+regime key>
              :compatibility <reader/writer bounds>}

 :render {:order <Contract-O entry producer>
          :geometry <Contract-G descriptor>
          :color-alpha <Contract-C descriptor>
          :resources <lifetime+budget owner>
          :regimes <complete legal-domain matrix>}

 :receipts <citizenship receipt IDs>}
```

Grammar owns meaning. Pick exposes that meaning through the same geometry and
order as paint. Provenance makes the material re-enterable. Versioning prevents
an implementation upgrade from silently changing durable meaning. Render fields
are projections and may be rebuilt.

### 5.2 Minimum citizenship receipt set

No atom is admitted without all applicable receipts:

- **M1 grammar:** canonical round-trip, malformed-input rejection, explicit
  defaults, unknown-field policy, and semantic edit operations.
- **M2 identity:** material identity survives edit/reload/export projection;
  instance identity survives transform, nesting, reuse, and reorder.
- **M3 order:** Contract-O overlap and deterministic order-hash receipts.
- **M4 geometry:** Contract-G interior/coverage/tie/slop receipts across default
  and legal zoom, extent, normalization, precision, lifecycle, and backend.
- **M5 provenance:** source → edit act → revision → derivation/cache can be
  explained and replayed without trusting caller-asserted metadata.
- **M6 versioning:** old fixtures migrate deterministically; algorithm changes
  create a new version/cache key; rollback/re-read preserves old meaning.
- **M7 lifecycle:** the same semantic identity moves hand → settle → scene with
  no undeclared visual, pick, or order discontinuity.
- **M8 persistence/replay:** durable reload and recorded replay recover grammar,
  identity, order, geometry, color, and provenance.
- **M9 export:** each promised projection states losses and preserves identity/
  provenance linkage; unsupported meaning fails visibly rather than flattening
  silently.
- **M10 resources:** allocation, lifetime, cleanup, budget failure, device loss,
  and asset-unavailable behavior are named and machine-driven.
- **M11 color:** Contract-C reference composites, texture transfer/alpha tags,
  group opacity, intermediate/present equivalence, and export color metadata.
- **M12 corpus:** the family-specific adversarial corpus—including every
  ratified envelope pressure it claims—is durable and replayable.

