## 8. Contract C — color and alpha

### 8.1 Canonical meanings

1. Durable/material solid colors and gradient stops are **straight
   (unassociated) RGBA tagged with a color space**. The baseline authoring space
   is `sRGB`; imported image/video/profiled assets carry their declared profile
   or an explicit `unknown` refusal policy. Untagged RGB is invalid at family
   admission.
2. The logical scene working space is **linear-light sRGB with premultiplied
   alpha**. A wider-gamut working space may be added by an explicit versioned
   capability; it cannot silently reinterpret baseline sRGB material.
3. Decode/transfer into linear occurs exactly once at resource/material ingress.
   Premultiplication occurs after coverage and effective opacity are known and
   before compositing. Presentation/export encodes to the declared output color
   space exactly once.
4. Every texture declares color space, channel meaning, and alpha association
   (`straight`, `premultiplied`, or `opaque`). Coverage/ID/normal/depth textures
   are data, not color, and never receive an sRGB transfer.

For straight material color `[r,g,b,a]`, coverage `c`, and additional effective
opacity `o`, the source entering the compositor is:

```text
A = clamp(a × c × o, 0, 1)
src = [linear_srgb(r,g,b) × A, A]
```

Premultiplied source-over is the binding default:

```text
out.rgb = src.rgb + dst.rgb × (1 - src.a)
out.a   = src.a   + dst.a   × (1 - src.a)
```

The corresponding blend factors are `one / one-minus-src-alpha` for both color
and alpha. Non-source-over blend modes are named operators in the graph; they
still state input/output association and working space.

### 8.2 Coverage, opacity, groups, and presentation

- AA/stroke/glyph/image-mask coverage multiplies both premultiplied RGB and
  alpha through `A`; it never changes unassociated material color.
- Group opacity is applied once to the composited group's premultiplied RGBA,
  not independently to every child. This is why group targets are semantic
  resources in W4.
- Linear gradients interpolate decoded linear-light stops by default. Any other
  interpolation space is explicit material policy and versioned.
- Intermediate scene/group/region targets declare format, transfer, alpha
  association, clear value, and load/store behavior. Transparent intermediates
  clear to `[0,0,0,0]`; opaque presentation may clear/resolve to an explicit
  background.
- An `*-srgb` texture/target may perform the declared hardware transfer; an
  `*unorm` target requires the declared shader/presentation transfer. The plan
  proves exactly one conversion, never zero or two.
- The canvas is configured premultiplied today
  (`src/app/electric_flow.cljc:745-766`); the presentation pass must therefore
  provide premultiplied pixels in its declared output color space. Canvas
  format selection itself is not a color-management contract.

The present live pipelines are migration input, not precedent: they return
unassociated RGB with alpha coverage and use `src-alpha` even for alpha
accumulation (`src/app/client/substrate/webgpu/renderer.cljs:614-629`,
`src/app/client/substrate/webgpu/renderer.cljs:801-816`,
`src/app/client/substrate/webgpu/renderer.cljs:860-878`). W2-B's scene-color
migration must make Contract C code-real before transparent group/effect
composition relies on it.

### 8.3 Contract-C receipts

- **C1 reference source-over:** opaque, transparent, and overlapping translucent
  primaries match a CPU linear-premultiplied reference, including output alpha.
- **C2 coverage:** SDF/MSDF/Slug/path/image-mask coverage changes alpha and
  premultiplied RGB together; no dark/bright fringe on non-black backgrounds.
- **C3 group equivalence:** direct paint equals an isolated group at opacity 1;
  group opacity differs lawfully from per-child opacity on overlaps.
- **C4 transfer:** tagged sRGB solids/gradients and profiled test images are
  equivalent across direct present, intermediate present, readback, and export;
  exactly-one-transfer sentinels catch double/missing conversion.
- **C5 alpha association:** straight and premultiplied texture fixtures convert
  to the same scene value; mis-tagged inputs fail visibly and diagnostically.
- **C6 format/backend:** every enabled target/texture format records color space,
  transfer, precision, alpha association, adapter/browser fingerprint, and
  regime; fallback preserves semantics or halts the rung.
- **C7 deterministic export:** PNG/video/vector export records output profile,
  alpha association, blend limitations, and any intentional loss.

