## 3. Contract O — one ordered scene truth

### 3.1 Binding data shape

The world compiler emits one immutable ordered tape per world/registry revision.
Illustrative syntax; field meanings are binding:

```clojure
{:scene-order/version 1
 :world/revision <durable-or-projection-revision>
 :entries
 [{:entry/id <stable-entry-id>
   :material/id <stable-material-id>
   :material/revision <revision>
   :instance/id <stable-instance-id>
   :family/id <registered-family-id>
   :order {:stratum <world|overlay|region-composite>
           :pass-class <declared-compositor-class>
           :stack-path [[<context-id> <layer> <sibling-rank>] ...]
           :part-rank <integer>
           :stable-tie <stable-entry-id>}
   :paint <registered-batch-reference>
   :pick <geometry-pick-reference|none>
   :visibility <shared-clip-mask-opacity-reference>
   :region-router <none|registered-3d-router>} ...]
 :order-hash <deterministic-hash>}
```

`stack-path` is semantic scene structure, outermost context first. `layer` is
one component of that path, never the whole truth. `sibling-rank` comes from the
material/instance structure. `part-rank` orders one material's subparts (for
example shadow before surface, surface before inline chrome). `stable-tie` is
used only after every semantic field ties; it cannot replace missing sibling
order. Family identity and registration call order are not semantic z-order.

### 3.2 The two projections

1. **Paint** traverses the tape forward. A frame graph may split entries across
   passes only if the final composition is equivalent to that forward order.
   Transparent entries, masks, group targets, backdrop inputs, and region
   composites therefore carry explicit order dependencies.
2. **Pick** filters the same tape to entries whose effective visibility and
   pick policy permit a hit, then traverses that projection in reverse. It does
   not re-sort by layer, family, container, map iteration, slot name, or
   registration order.
3. Clips, masks, hidden state, group visibility, and instance transforms are
   shared references read by both projections. A pick-only hit slop may widen
   geometry under Contract G; it cannot change the order token.
4. An entry with `:pick :none` remains in paint order and disappears only from
   the pick projection. Multiple paint entries for one semantic instance may
   point at one pick owner; the reverse projection de-duplicates by that owner
   after the first hit.
5. A 3D region is one ordered compositor entry in the 2D tape. Its registered
   router owns depth/order inside the region and returns the resolved inner
   identity; outer 2D siblings still resolve through the tape.
6. Exact boundary or hit-slop overlap between multiple entries is resolved by
   reverse tape order. Geometry iteration order never decides the winner.

Batching may merge only order-contiguous compatible entries, or carry an
explicit indirection that reproduces tape order. It may not reorder transparent
or pickable entries for pipeline convenience. A family registry may select a
producer; it may not inject a family tie-break above semantic stack structure.

### 3.3 Contract-O receipts

- **O1 · totality/determinism:** shuffle registry insertion and map construction;
  the same world produces the same order hash and entry sequence.
- **O2 · cross-family overlap:** rect, text, path, image, overlay, shadow/effect,
  and region-composite fixtures overlap with opaque reference paint; reverse
  pick returns the topmost scene entry among those whose declared pick geometry
  hits. Transparent/alpha-aware variants follow their explicit pick policy.
- **O3 · structure:** nested containers, equal layers, repeated material
  instances, and stable sibling reorder produce the declared changes only.
- **O4 · visibility:** clip, mask, hidden, group opacity-zero, and region routing
  affect both projections according to their declared policies.
- **O5 · batching falsifier:** randomized batch/family registration produces
  byte-identical pixels, pick identities, and order hash.
- **O6 · static fence:** after W2-B, adding a family changes its registration and
  implementation, never a hand-positioned central draw/pick branch.

