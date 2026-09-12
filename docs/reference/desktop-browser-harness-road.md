# Desktop browser harness road

Use this reference to avoid re-deriving Chrome/CDP startup. It separates two
different instruments whose evidence must never be merged.

## Headed desktop / product acceptance

Repo-proven starting conditions (`history/docs/durable-ground/NOW.md`, 2026-07-17):

- run Chrome headed on the desktop with `DISPLAY=:0`;
- expose CDP on port `9222`;
- enable unsafe WebGPU and Vulkan with
  `--enable-unsafe-webgpu --enable-features=Vulkan`;
- the command panel may boot open, so `Ctrl+K` can close rather than open it;
- scripted keystrokes used window `KeyboardEvent`s and needed at least 50ms
  per character.

Inferred launch template — useful, but not itself established by that receipt.
Resolve each variable first. `CHROME_BIN` is the installed executable,
`PROFILE_DIR` is a freshly created directory explicitly owned by this run,
and `RECEIPT_URL` is the contract-named target:

```sh
DISPLAY=:0 "$CHROME_BIN" \
  --user-data-dir="$PROFILE_DIR" \
  --remote-debugging-port=9222 \
  --enable-unsafe-webgpu \
  --enable-features=Vulkan \
  "$RECEIPT_URL"
```

Before interaction, record the URL, display, Chrome version, adapter/device,
CDP endpoint/port, worktree HEAD, and dirty custody. Optionally check
`http://127.0.0.1:9222/json/version` as an operator readiness probe; that HTTP
probe is not a repo-proven requirement. Then drive only the contract-named
interaction. Product acceptance remains Sid's unscaffolded felt pass; CDP is
transport, not an acceptance substitute.

Never implicitly reuse or delete an existing profile directory. Do not use a
personal profile, copy auth material, or turn atom-specific `window.__...`
hooks into a general road.

## Headless Puppeteer / mechanical verifier

The maintained automation road is `test/render_engine/run_verifier.mjs`. It:

- chooses Chrome from `RENDER_VERIFIER_CHROME` or `/usr/bin/google-chrome`;
- launches Puppeteer headless with `--no-sandbox`, unsafe WebGPU, SwiftShader,
  and `WebGPU,UnsafeWebGPU`;
- opens a CDP session for browser permissions;
- writes mechanical verifier receipts under `target/render-verifier/`.

Use the checked-in npm verifier command rather than rebuilding a one-off
Puppeteer harness. Golden-update/amendment flags and atom-specific globals are
not part of this general road.

## Evidence classification

- Headed desktop + attested adapter/device + Sid's task: lived/product evidence;
  hardware is claimed only when the environment receipt identifies it.
- Headed + CDP automation: mechanical interaction evidence under the recorded
  capture conditions.
- Headless + SwiftShader: deterministic verifier evidence only.
- SwiftShader `DEVICE-LOST`, blank pixels, or software-adapter timings do not
  prove a headed/hardware renderer defect or product acceptance.

If the required receipt needs tooling not present on these two roads, record
`RECEIPT PENDING`. Do not invent a browser harness inside a close-receipt pass.
