# Softland in Softland — close receipt

- Source frozen: `ed93ec5`; documentation checkpoint before close: `c4cb1a3`.
- Registered receipt passed: `bin/inland check` — 5 tests / 72 assertions; `bin/inland verify` — 7 checks, no failures or browser errors.
- Durable demonstrated workspace: http://localhost:8127/?workspace=receipt-1789146056386
- Real Claude `haiku` activity `41ae5677-12d5-49a7-9c4b-009511af43eb`: complete; stream exit 0; 451 input / 336 output tokens; $0.003121; 0 retry events; 2 assistant messages.
- At initiating-view close it was observed running; final durable status is complete. No automatic retry was used.
- Narrow reads 4→3; support 2→1→0; headline/Region3D prep unchanged; view counters opened 1311, closed 1309, changes 28 (two bootstrap/reopen reads remain by design).
- Recovery replaced app JVM 1236079→1243113 and retrieved both instruments plus accepted reply from isolated Rama storage.
- Goldens: `evidence/01-instrument.png`, `02-opened.png`, `03-kept.png`; factual receipt and focused log sit beside them.
- Close launch is `bin/inland serve` after `bin/inland stop-app`: http://localhost:8127/; `/health` 200 and test controls disabled.
- Runtime is `.inland-runtime`; cluster is retained. Acceptance and any permanent adoption are Sid's word.
