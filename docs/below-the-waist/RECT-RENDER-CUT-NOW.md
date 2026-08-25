# Rect rendering cut — NOW

Status: BUILT + RECEIPTED 2026-08-26; SID ACCEPTANCE PENDING.
Source: Sid direct request, 2026-08-26.
CUT: rect tree/store/runtime/selection; frame/chrome/Region3D adapters; 2D rect and Gaussian rounded-shadow GPU families; coupled tests and goldens.
KEPT: image/glyph quads, clip/scissor bounds, path polygons, generic frame graph/compositor, Region3D kernel and shadow maps.
RAMA FENCE: no server/Rama source, PState, `/mnt/data/rama`, archive, or durable data mutation.
TRIPWIRES: 9 obsolete client paths absent; deleted-family symbol matches 0; 73 tests / 803 assertions green.
RECEIPT: render-engine verifier green; CLJS compile 0 warnings; six GPU guards and three representative goldens pass.
FOREIGN DEBT: the object-container namespace warning predates this cut; no correction attempted.
CHANGED FILES: derived from `git diff --name-only`; 75 source/test/artifact files plus this receipt and the board.
NEXT: Sid accepts or reopens the rendering boundary.
