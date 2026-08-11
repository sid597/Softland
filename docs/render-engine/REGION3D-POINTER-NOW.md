# REGION3D / POINTER — NOW

## STANDING

Region3D pointer interaction resolves every event into one typed packet
(css · region-local · region-device + matching viewports, scale S = dpr ×
effective outer scale); consumers declare their representation and never
cross-pair. Navigation is pointer-anchored (similarity dolly about the
anchor; grab-plane glued pan, start-camera rays). One wheel court in
`scroll.cljs` gives each wheel event exactly one camera; the gizmo picks
the geometry it paints (segment metric, glass-declared slop, hover).
Desired-size pick authority and the one-pick-road chain are preserved.

## NOW

- 2026-08-11 — CONTRACT CUT (Fable 5, this session; born of Sid driving
  the seam demo: pointer drift + gizmo confusion). Four structural defects
  pinned (D1 no anchor · D2 pan cross-pair · D3 dual wheel dispatch ·
  D4 gizmo ray split / pick≠paint / device-fixed slop). Fresh-eyes round
  at Sid's hand: PENDING. Implementation: NOT STARTED.
