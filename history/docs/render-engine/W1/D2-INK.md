## 6. D2=A ink citizen

### 6.1 Authoritative grammar

The ink instance binds Contract M as follows:

```clojure
{:family/id :shape/ink
 :family/version 1
 :material
 {:id <stable-material-id>
  :revision <revision>
  :provenance <actor+act+parents+timestamps/source>
  :lifecycle <draft|settled>
  :space {:local <declared> :source-bounds <bounds> :normalization <transform>}
  :paths
  [{:path/id <stable-subpath-id>
    :role <open|closed>
    :centerline
    {:representation :ordered-segments
     :knots [{:knot/id <stable-id>
              :position [x y]
              :pressure <normalized-value>
              :gesture-time <time-or-explicitly-absent>
              :source-event-ids <provenance references>} ...]
     :segments [{:segment/id <stable-id>
                 :kind <line|quadratic|cubic>
                 :from <knot-id> :to <knot-id>
                 :controls <stable control identities>} ...]}
    :stroke {:width/profile <mapping from pressure>
             :cap <cap> :join <join> :miter-limit <n> :dash <pattern>}
    :fill <none|paint-ref>
    :fill-rule <nonzero|even-odd>
    :contour-role <open|outer|hole>} ...]
  :paints {:stroke <paint-ref> :fill <paint-ref> :opacity <n> :blend <mode>}
  :topology-extension <stable endpoint/segment reference seam>
  :export-policy <projection+losses>}

 :instance
 {:id <stable-instance-id>
  :material/id <stable-material-id>
  :material/revision <revision>
  :affine <local-to-parent transform>
  :scene-order <Contract-O reference>}

 :derived-outline
 {:authority :cache-only
  :source-revision <material revision>
  :algorithm {:id <stroke-expander> :version <version>}
  :pressure-normalization <version>
  :smoothing-resampling <version+tolerance>
  :self-intersection <versioned rule>
  :lod/normalization/backend/regime <declared>
  :digest <deterministic digest>}}
```

The ordered centerline knots/segments plus their pressure profile are **one
authority**. There is no parallel authoritative sample array. Raw device events
may be retained under provenance and referenced by knots; they do not become a
second geometry body. Gesture time/provenance may enrich re-entry, but a missing
time stream must be explicit. The outline is never independently editable
truth. A boolean or crop result may be a new material with causal provenance;
it does not mutate the cache into a second authority.

During hand, a centerline-distance/width reader may serve pick and cheap
coverage. During settle/scene, a derived outline/mesh/analytic road may serve.
All readers are projections of the same pinned centerline-pressure revision and
stroke-expansion algorithm and must pass Contract G. A change in expansion law
creates a new algorithm version; old settled revisions remain reconstructible.

Stable subpath, knot, segment, and endpoint references keep a lawful extension
seam for ratified branching vector networks. W1 does not force freehand ink and
branching networks into one family, but it forbids an identity model that would
require destroying existing path/segment identity to add junctions.

### 6.2 The twelve D2 falsifiers are admission law

| `W0-C.md` §6.4 case | Required surviving evidence |
|---:|---|
| 1 | Fast pressure-varying open stroke preserves ordered samples, pressure normalization, round-cap semantics, hand coverage/pick, and settled derivation identity. |
| 2 | One centerline revision switches acute miter/bevel/round join semantics through an explicit material edit; each outline is deterministic and version-tagged. |
| 3 | Self-intersecting closed loop records fill rule; mathematical classification and export agree under both nonzero and even-odd. |
| 4 | Compound shape records outer/hole roles; nested affine transforms preserve classification, order, pick, and stable identities. |
| 5 | Erase/split is a semantic edit over stable sample/segment IDs with causal provenance; no private outline reconstruction becomes truth. |
| 6 | Re-thicken/restyle edits width/profile/paint against the same centerline authority; prior and new derived outlines remain attributable. |
| 7 | Boolean use produces a named derived/new material with source revisions and declared loss; cached outline is never promoted silently to coequal truth. |
| 8 | One control-point/sample edit is addressable, revisioned, replayable, and invalidates only derivations keyed to that source revision. |
| 9 | Crop/mask and SVG/PDF projections name geometry/color/provenance losses and remain linked to the source material/revision. |
| 10 | Boundary pixels replay over default `[0.1,8]` and legal `[0.01,1000]`, tagged by extent, normalization, precision, backend, format, and lifecycle; half ties follow §4.4. |
| 11 | Hand → settle → scene keeps material/instance identity and produces no undeclared coverage-isocontour, pick, order, or color discontinuity. |
| 12 | A branch/junction fixture exercises the stable topology extension without deciding family merger by accident or destroying existing segment identity. |

Failure of any row halts that ink road for repair. It never removes ink, holes,
open paths, booleans, export, or vector networks from the campaign.

