# Local-model sovereignty — intake + recon (2026-07-05)

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../docs/carry-on.md) as a reference summary
and [the vision log](../../vision/LOG.md) as the primary source.

**What this file is:** pre-read for the Candidates intake sitting (D-007
pipeline). Verbatim source: `vision/LOG.md` 2026-07-05 — "local models over my
own land: the sovereignty hypothesis + benchmarks for models". The candidate
bets below are **DRAFTS** — nothing here enters BETS.md until the sitting;
promotion is Sid's call, one ACTIVE bet at a time. D-001 governs throughout:
no build recommendation appears here ahead of a form-break; dreams are labeled
DREAM. Recon claims are web-verified 2026-07-05 with sources inline; where a
claim is inference or memory it is flagged as such.

---

## 0. The hypothesis, decomposed

Sid's entry contains four separable claims (his verbatim, restated):

1. **Sovereignty** — "as softland starts to show off its real power users will
   want to have access to their own data ... currently everything about
   softland is getting to claude but i would not want that in future since
   this will be my business and personal knowledge."
2. **Capability** — small fast local models (Nemotron 3 Nano class, "smart for
   its size but also super fast") are good enough for many Softland-side
   tasks. Scope boundary in his own words: "not building it but when its all
   setup and on the user or softland side."
3. **Personalization** — "even if i have to train 'my personal' model on my
   data stored in rama i would want to do it on open source ones."
4. **Measurement** — "how good is a model for softland" needs a benchmark.
   This half is already in motion: `build/model-uxr/SPEC.md` (v0 DRAFT,
   2026-07-05) — and its subjects config already carries a `nemotron-local`
   OpenAI-compatible placeholder at `localhost:8080` (SPEC §5). **The local
   box is already a first-class test subject on paper.** Claim 4 is therefore
   not a candidate here; it is the instrument the other three are measured
   with.

Load-bearing observation for the sitting: the D-008 custody model (payload
asserter ≠ envelope actor, trail-view CONTRACT §5.1) means the land records
*whose tokens are whose* by construction. That single fact does double duty
below: it is the licensing filter for claim 3 (§2.6) and the provenance
guard for every slot in §3.

**Same-day refinement (Sid, HQ chat follow-up to this file's first draft —
his words quoted below; landed verbatim in `vision/LOG.md` same-day entry,
probe-standup session):**

1. **Distillation stance (assertion-grade):** "Yess I will not do any type
   of distillation on this .. only for open legal models ifff they have
   some" — closed-provider outputs (Anthropic/OpenAI) NEVER enter training
   targets, full stop; distillation is a lane only where the source model's
   license grants it (§2.6 tiering — the "ifff they have some" is answered:
   they do).
2. **The pool, not the model:** "this is not only about nemotron we can
   have other models as well whatever can run on my gpus but also
   performant on some dimension i care about in softland ... i do think
   gemma type models can also be used for lower reasoning tasks but that
   are very useful in softland." Consequence: the hypothesis's unit is a
   **pool of local models routed per-slot** — each slot goes to the
   cheapest model that clears that slot's capability bar (§3's routing
   table is model×slot, not local-vs-frontier binary). model-uxr absorbs
   this for free: `subjects.edn` takes N local subjects; the bank ranks
   them per-metric.

---

## 1. Candidate bets (D-007 form — drafts for the sitting)

Thresholds marked SITTING-SETS are proposals; the sitting fixes numbers
before any probe runs (pre-registration discipline, same as model-uxr §1).

### LM-1 — Structure substitutes for scale (the H2 link)

**Claim (falsifiable):** On the frozen model-uxr bank, giving a ≤48GB-servable
local model the full structured land buys more orientation quality than
giving a frontier model scale without structure. Operationally, at a pinned
sha and bank version: (a) local@A0 (full bundle) beats frontier@A-ablated
(relations/decisions dropped) on wrong-authority rate, invented-structure
rate, and tokens-to-orientation; and (b) the local model's own A0→ablated
degradation exceeds the frontier's — the small model is *living off* the
structure, which is exactly H2's "structure lowers the model size a task
needs," measured as the interaction term of the 2×2 the SPEC already
pre-registers as its headline comparison (SPEC §4: "small-local vs frontier
at A0, and each subject's own A0→Ax deltas").

**KILL:** local@A0 invented-structure rate ≥ 2× frontier@A0 (SITTING-SETS) —
structure cannot rescue the small model; scale was doing the work. Or:
local's A0→ablated delta ≤ frontier's — structure helps the big model just
as much, so it lowers no size floor.

**CONFIRM:** local@A0 within ε (SITTING-SETS) of frontier@A0 on all five
metrics AND local's ablation delta ≥ 2× frontier's.

**Dependencies:** model-uxr bank frozen + sha pinned (SPEC §1); typed
relations present in the snapshot (the H1 arming-event material — A3 needs
something real to drop); a local OpenAI-compatible endpoint.

**Cheapest discriminating probe:** stand up `llama-server` (llama.cpp) or
vLLM with Nemotron 3 Nano quantized (Q4 ≈ 24GB or Q8 ≈ 36GB — both fit 48GB,
§2.2), fill the model id into the *already-drafted* `nemotron-local` slot in
`subjects.edn`, run the bank at A0 + one ablation. Zero new instrumentation
beyond what model-uxr builds anyway. This is the rare candidate whose probe
is a config edit.

### LM-2 — The sovereignty boundary runs along wrongness-cost

**Claim (falsifiable):** for the slot that already has a live form-demand —
band-2 / display-name enrichment (§3.1; R5 ledger-13 middle form, WP2
display-name assignment per D-006 close notes) — a local model's output is
indistinguishable-in-use from frontier output, so the lowest-wrongness-cost
slots can move local with zero felt quality loss. This is claim 1's first
testable increment: sovereignty advances slot-by-slot from the low-stakes
end, not by decree.

**KILL:** in a blind A/B over N real nodes (SITTING-SETS, ~30), Sid
identifies or prefers the frontier output at a rate distinguishable from
chance; or, if the slot later runs local in daily H1 use, a quality-
attributed form-break inside two weeks.

**CONFIRM:** blind A/B at chance AND (once live) two weeks of the slot
running local with zero quality-attributed form-breaks — frontier calls for
that slot drop to ~0.

**Dependencies:** the enrichment slot existing at all (WP2 window — this
candidate does NOT accelerate it; D-001); local serving up (same endpoint as
LM-1's probe).

**Cheapest discriminating probe:** offline, before the slot ships — models
generate reading-lines/display-names for ~30 real corpus nodes; Sid judges
blind. About an hour of his attention, no product code. Per the same-day
pool refinement this runs naturally **multi-arm** (frontier / Nemotron-class
/ Gemma-class) at the same Sid-attention cost — the blind judging doesn't
care how many arms produced the candidates, and the result seeds the
model×slot routing table directly.

### LM-3 — My words are enough (own-material personalization)

**Claim (falsifiable):** a QLoRA fine-tune of an open-weight base on
Sid-asserted material ONLY (vision/LOG.md, countersigned decisions, wall
transcriptions, his own turns extracted from ingested transcripts — never
model outputs) beats the un-tuned base on the model-uxr bank at the same
ablation, by a pre-set margin (SITTING-SETS) on tokens-to-orientation and
wrong-authority rate, with invented-structure flat or falling.

**KILL (three gates, cheapest first):**
1. **Saturation probe (probe-0, pre-training):** if un-tuned base + bundle
   already saturates the bank headroom (adding the Sid-corpus in-context
   moves nothing), retrieval subsumes tuning — resolve WITHOUT activating.
   The Candidates rules explicitly allow this exit.
2. **Corpus census:** Sid-asserted tokens below a feasibility floor
   (SITTING-SETS; ~1M-token order per common LoRA practice — *inference,
   not verified this session*). Countable today: the transcript kernel
   stores roles, so `asserted-by = sid` extraction is a query, not a project.
   **CENSUS RUN (probe-standup session, same day — method + breakdown §6.3):
   ~783k tokens Sid-typed across all transcript roots (602,602 words, 4,488
   user turns), ~419k Softland-only, +13k vision/LOG.md, countersigns ~0.1k.
   Total ≈ 0.8M — same order as the ~1M floor; gate 2 does NOT kill on its
   own. Softland-only alone (~0.4M) is thin. Threshold stays SITTING-SETS.**
3. **Post-training:** invented-structure rate rises — tuning taught voice,
   not land-truth.

**CONFIRM:** tuned beats un-tuned past the margin at fixed bank version,
invented-structure flat/down, and the tuned model's LM-2-style blind A/B
holds on at least one slot.

**Dependencies:** corpus census; a workable 48GB tuning path — **verified:
QLoRA-32B-class ≈ 26GB fits; Nemotron 3 Nano's documented 16-bit LoRA is
60GB and does NOT fit, its 4-bit number is undocumented (§2.4)** — so the
known-good tune target today is a dense ~32B (Qwen3-32B class, Apache 2.0
per its HF card — *license from general knowledge, re-verify before
load-bearing use*), even if the serve target stays Nemotron; licensing clean
by construction (own material only, §2.6).

**Cheapest discriminating probe:** probe-0 + the census. Both are pre-
training, nearly free, and each can kill the candidate alone.

---

## 2. Recon (web, 2026-07-05 — primary sources; SEO listicles flagged)

### 2.1 What a 48GB card holds today

- **Dense 70B at Q4** fits but tight: Llama 3.3 70B Q4_K_M ≈ 39GB weights,
  leaving little KV headroom ([InsiderLLM VRAM cheat sheet](https://insiderllm.com/guides/vram-requirements-local-llms/),
  [promptquorum 2026 guide](https://www.promptquorum.com/local-llms/local-llm-hardware-guide-2026) — listicle-grade, verify per model).
- **Dense ~32B at Q4–Q8** fits with generous context (Qwen3-32B Q4 ≈ 20GB).
- **Gemma-class (the "lower reasoning but very useful" tier, Sid same-day):**
  Gemma 3 27B Q4 ≈ 17GB, 12B ≈ 8GB, multimodal from 4B up, 128K context
  (*sizes from general knowledge + listicle corroboration — re-verify per
  build before load-bearing use*). Several such models fit **simultaneously**
  alongside a 24–36GB flagship on one 48GB card — the pool refinement (§0)
  is hardware-real, not aspirational.
- **MoE ~30B-A3B** is the sweet spot for *fast* serving: few active params →
  high tok/s, big total → capability; Nemotron 3 Nano Q4 ≈ 24GB / Q8 ≈ 36GB
  (§2.2). gpt-oss-20b (~16GB, Apache 2.0) fits easily; gpt-oss-120b (~63GB
  MXFP4) does not (*pre-cutoff knowledge, stable*).
- 48GB single cards are RTX A6000 / 6000 Ada / L40S class; the same budget as
  2×24GB consumer cards ([VRLA](https://vrlatech.com/llm-hardware-requirements-guide/), [Spheron GPU guide](https://www.spheron.network/blog/best-nvidia-gpus-for-llms/)).
  Which box Sid actually has matters for FP8 support and TensorRT-LLM
  worth-it-ness — open question §4.

### 2.2 Nemotron 3 Nano — verified

- **Size/architecture:** ~31.6B total, ~3.2B active (3.6B w/ embeddings);
  hybrid of 23 Mamba-2 layers + 23 MoE layers (128 routed + 1 shared expert,
  6 active/token) + 6 GQA attention layers
  ([NVIDIA research page](https://research.nvidia.com/labs/nemotron/Nemotron-3/),
  [HF model card](https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-30B-A3B-BF16),
  [paper](https://arxiv.org/pdf/2512.20848)). Released 2025-12-15.
- **Context:** 1M max, 256k default (VRAM-bound). The Mamba-heavy hybrid is
  what makes long context cheap (per NVIDIA's Nemotron 3 materials).
- **Fits 48GB only quantized:** BF16 ≈ 60GB+ — no. Q4 ≈ 24GB, Q8 ≈ 36GB — yes
  ([Unsloth Nemotron guide](https://unsloth.ai/docs/models/nemotron-3),
  [ollama library](https://ollama.com/library/nemotron-3-nano)). A 4B sibling
  (2.8GB, 256K ctx) exists; also an omni-modal variant
  ([Nemotron 3 Nano Omni](https://developer.nvidia.com/blog/nvidia-nemotron-3-nano-omni-powers-multimodal-agent-reasoning-in-a-single-efficient-open-model/))
  and a larger Super tier.
- **License:** [NVIDIA Nemotron Open Model License](https://www.nvidia.com/en-us/agreements/enterprise-software/nvidia-nemotron-open-model-license/)
  — commercial use permitted; you own your derivative models; NVIDIA claims
  no ownership of outputs; NOTICE-file attribution on redistribution;
  litigation-termination clause. Training data substantially open on HF.
  A third-party [corporate-risk analysis](https://shujisado.org/2025/12/19/nvidia-open-model-license-a-corporate-risk-analysis/)
  (2025-12) flags residual risks worth reading before anything commercial.

### 2.3 Serving — every path speaks OpenAI

All three stacks expose OpenAI-compatible `/v1/chat/completions`:
[vLLM's server](https://docs.vllm.ai/en/stable/serving/online_serving/),
llama.cpp's `llama-server` ([comparison](https://tensorfoundry.io/blog/llm-inference-servers-compared)),
and TensorRT-LLM's [`trtllm-serve`](https://nvidia.github.io/TensorRT-LLM/1.0.0rc2/commands/trtllm-serve.html).
Consequence: model-uxr's `subjects.edn`, and any future Softland consumer,
never needs to know which stack is behind the URL. Friction ranking from the
field: llama.cpp lowest-friction single-user; vLLM for concurrency and
features; TensorRT-LLM fastest on NVIDIA but heavy setup. **vLLM additionally
serves multiple LoRA adapters over one base, selected per-request via the
`model` field, with runtime load/unload endpoints**
([vLLM LoRA docs](https://docs.vllm.ai/en/v0.8.1/features/lora.html)) — one
30B base + per-slot adapters is an architecture the serving layer already
supports, no invention needed.

### 2.4 Fine-tuning at 48GB

- **Unsloth minimums** (framework's own table — treat as floor, real usage
  rises with context/batch): QLoRA 4-bit — 7B: 5GB, 14B: 8.5GB, **32B: 26GB,
  70B: 41GB**; LoRA 16-bit — 32B: 76GB, 70B: 164GB
  ([Unsloth requirements](https://unsloth.ai/docs/get-started/fine-tuning-for-beginners/unsloth-requirements)).
  Historical anchor: the original QLoRA paper tuned a 65B on a single 48GB
  GPU ([arXiv 2305.14314](https://arxiv.org/abs/2305.14314)).
- **Discrepancy flag:** SEO guides quote far higher numbers (32B QLoRA ≈
  44GB, 70B ≈ 88GB — [Spheron](https://www.spheron.network/blog/gpu-vram-requirements-fine-tune-llm-2026/));
  the spread is batch/sequence-length assumptions. Verdict: 32B QLoRA at
  48GB is comfortable; 70B QLoRA is edge-of-feasible.
- **Nemotron 3 Nano specifically:** Unsloth has day-zero support, but
  documents **~60GB for 16-bit LoRA** (does not fit) and no 4-bit figure;
  router-layer tuning disabled by default for MoE stability; ≥75% reasoning
  data recommended to preserve reasoning
  ([Unsloth guide](https://unsloth.ai/docs/models/nemotron-3)); early rough
  edges reported ([unsloth#3810](https://github.com/unslothai/unsloth/discussions/3810)).
  Hence LM-3's dense-32B fallback.

### 2.5 Local embeddings — the solved corner

- **Qwen3-Embedding 0.6B / 4B / 8B — Apache 2.0**, 32K context, output dims
  32–4096 (MRL), instruction-aware; the 8B was #1 on MTEB multilingual
  (70.58, 2025-06-05) — above closed APIs
  ([HF card](https://huggingface.co/Qwen/Qwen3-Embedding-8B)). 8B at Q4 ≈ 5GB
  ([BentoML survey](https://www.bentoml.com/blog/a-guide-to-open-source-embedding-models)).
- Lighter options: **EmbeddingGemma-300M** (768d, matryoshka to 128,
  ~622MB) and **BGE-M3** (MIT, dense+sparse+multi-vector)
  ([morphllm benchmark roundup](https://www.morphllm.com/ollama-embedding-models)).
- Verdict: embeddings are the zero-capability-question slot; anything on the
  48GB box runs these as a rounding error alongside the LLM.

### 2.6 Licensing / the distillation question

- **Anthropic** ([Claude Help Center, primary](https://support.claude.com/en/articles/12326764-can-i-use-my-outputs-to-train-an-ai-model)):
  "Our Terms do not allow the use of Outputs to train models that are
  competitive with Anthropic's own." Explicitly banned: general-purpose
  chatbots, open-ended text generation models, **"Using Outputs as training
  targets for models"**, reverse-engineering training methods. Explicitly
  allowed: non-competing specialized tools — their examples: sentiment
  analysis, content categorization, **summarization tools**.
- **OpenAI** ([Terms of Use](https://openai.com/policies/row-terms-of-use/),
  [Services Agreement](https://openai.com/policies/services-agreement/)):
  same shape — may not "use Output to develop models that compete with
  OpenAI."
- **Reading for Softland (interpretation, not legal advice):**
  - *Distilling Claude transcripts into a general local assistant* — squarely
    the prohibited case (outputs as training targets for an open-ended
    model).
  - *A narrow Softland tool* (relation proposer, band-2 summarizer) trained
    partly on Claude outputs — arguably inside the allowed
    specialized-tools category, but bundle-Q&A drifts toward "open-ended
    generation." Gray. Not worth being the test case.
  - **The clean corpus is Sid-asserted material** — his LOG entries, his
    countersigned decisions, his wall, his own turns in transcripts. No
    provider terms attach to his own words. Ownership of Outputs sits with
    the user under both providers' terms, but ownership ≠ unrestricted use —
    the competing-training restriction is contractual and survives
    ownership.
  - The land already separates these token populations: custody (D-008,
    CONTRACT §5.1) records payload asserter and envelope actor per row.
    **Provenance is the licensing filter, by construction** — a
    training-set export that selects `asserted-by = sid` is legally clean
    AND is exactly the sovereignty-aligned corpus (claim 3's "my data"). The
    clean path and the desired path coincide.

**Distillation-source tiering (added same day after Sid's "only for open
legal models ifff they have some" — verified 2026-07-05):**

- **CLEAN — license grants it outright:** DeepSeek-R1 family, MIT, permits
  "distillation for training other LLMs" in so many words
  ([DeepSeek-R1 distilled models](https://deepwiki.com/deepseek-ai/DeepSeek-R1/2.3-distilled-models));
  Qwen3, Apache 2.0, no output clause at all
  ([Qwen3 technical report](https://arxiv.org/html/2505.09388v1));
  Nemotron — license grants derivative rights, NVIDIA claims no output
  ownership (§2.2 license fetch).
- **CONDITIONAL — distilling FROM it propagates the license:** Gemma ≤3's
  [Terms of Use](https://ai.google.dev/gemma/terms) define "Model
  Derivatives" to include "any other machine learning model which is
  created by transfer of patterns of the ... Output of Gemma ... including
  distillation methods ... based on the generation of synthetic data
  Outputs by Gemma" — a model trained on Gemma outputs inherits Gemma's
  terms and use policy ([license-propagation analysis](https://shujisado.org/2025/02/21/a-curious-phenomenon-with-gemma-model-outputs-and-license-propagation/),
  [TechCrunch on open-license restrictions](https://techcrunch.com/2025/03/14/open-ai-model-licenses-often-carry-concerning-restrictions/)).
  Llama's naming/inheritance conditions are the same genus (*general
  knowledge, not re-verified this session*). NOTE the asymmetry: **running**
  Gemma for Softland tasks is plain commercial use — the clause bites only
  when Gemma *generates training targets*.
- **REPORTED-CLEAN, VERIFY BEFORE LOAD-BEARING:** Gemma 4 is reported
  Apache 2.0 ([secondary source](https://www.mindstudio.ai/blog/gemma-4-apache-2-license-commercial-use));
  no primary Google page checked this session.
- **NEVER (Sid's stance, same day):** Anthropic/OpenAI outputs as training
  targets — regardless of the specialized-tools gray zone above, the ruling
  is categorical and simpler than the gray zone: closed-provider tokens
  don't enter training sets, period.

---

## 3. Slot map (feeds the model-uxr routing table)

Per slot: the capability it actually needs · wrongness-cost if the model is
bad at it · which model-uxr machinery tests it. Form-demand status marked —
D-001 discipline: a slot with no form-demand gets no work, only a map entry.

| # | Slot | Capability actually needed | Wrongness-cost | model-uxr probe | Form-demand today |
|---|------|---------------------------|----------------|-----------------|-------------------|
| 1 | Embeddings | similarity over prose+code; zero generation | **LOW** — bad neighbors are visible, recomputable, never persisted as truth-claims | none in the generative bank — needs a small retrieval probe (asserted relations give free gold pairs: `based-on` endpoints should embed near) | **none yet** — no retrieval feature demands them |
| 2 | Ingest enrichment / band-2 text (display names, reading lines, 2-liner middle form — R5 ledger 13) | single-node faithful compression; must never invent; output is CONTRACT data on the face | **MEDIUM-LOW** — the reading surface Sid scans most, so a wrong 2-liner misleads at the default altitude; capped by: provenance-marked derived, regenerable, band-3 open shows ground truth | re-derivation ratio + invented-structure on single-doc questions; a compress-and-cite question class is a natural bank extension | **REAL** — R5 names it; WP2 display-name enrichment already assigned |
| 3 | Bundle Q&A ("ask the land") | orientation + authority discipline (tiers T0–T3); honest refusal on unanswerables | **MEDIUM-HIGH but ephemeral** — a confident wrong answer about what decisions.md rules is the map lying to the operator; nothing written into the land | **this IS the bank** — all five metrics, wrong-authority above all | arrives with H1 daily use (the bundle ritual) |
| 4 | Relation proposals over the unthreaded band | cross-doc join + direction + kind from the closed vocabulary; PROPOSED-grade with evidence anchors; refuse when evidence is thin | **HIGH raw** — typed edges are the load-bearing noun (D-004); confetti edges destroy the band's honesty (R7: the band saying "no relations asserted" is *true*). Capped to MEDIUM only under a countersign gate: model proposes, Sid asserts via CLI (D-008-compatible today; an in-view proposal surface is read→write milestone territory) | join-question success + invented-structure + A3 delta; honesty tests are the sharpest screen | partial — the unthreaded band renders NOW (F-L2), but proposal *generation* has no demanded surface yet |
| 5 | Ambient behind-the-scenes distillation (the notebook sketch, `vision/images/2026-07-05-notebook-behind-the-scenes.png`) | long-context faithfulness + provenance discipline + incremental update | **HIGH** — distillates become orientation surfaces; plausible-but-wrong summaries steer everything downstream silently, and errors compound day over day. The worst slot for a weak model | re-derivation + invented-structure at long-context ablations; degradation-vs-context-length is the discriminating curve | **none yet** — a dream slot until a digest form-demand exists |

**Routing-table shape this implies:** two independent axes — the capability
bar (which metrics, what threshold: the bank supplies this per slot) and the
wrongness-cost (which wrapper: nothing / provenance-mark / countersign gate /
stay-frontier). Local-first order falls out: **1 → 2 → 3 → 4 → 5**. Slot 1 is
also the sovereignty quick-win: an embedding API ships every document body
off-land — the largest raw-exfiltration channel per unit of capability
gained. (Observation, not a build recommendation — slot 1 has no form-demand
yet.)

Per the same-day pool refinement (§0), the table's answer column is
**model×slot, not local-vs-frontier**: each slot routes to the cheapest pool
member clearing its bar — plausibly Gemma-class for slots 1–2, a
reasoning-tier flagship (Nemotron/Qwen-class) for 3–4, frontier retained
where nothing local clears. The bank ranks pool members per-metric at no
extra design cost (`subjects.edn` is already a list). One observation
riding the pool: Gemma-class models are multimodal, and the
photograph-the-wall-and-paste-it ritual is an *existing daily form* (it's
literally H1's KILL clause) — a local vision model reading wall panels is
the one pool capability that touches a ritual already in daily use. Marked
observation only: the ingest slot for it does not exist, and D-001 holds.

---

## 4. Open questions (typed; frontier questions per D-007's practice)

**For Sid (frontier questions, sources named):**
1. **Where is the build/live boundary?** H2's evidence field is "our own work
   packages" (BETS.md H2) — build-side. The sovereignty hypothesis scopes
   itself to "not building it but when its all setup" (LOG 2026-07-05) —
   live-side. LM-1 measures on the build side's material but claims toward
   the live side. Does the ladder need the boundary named, or is the local
   box simply a *subject* wherever it appears?
2. **Is sovereignty a North property or a deployment detail?** North says
   the map can be handed to another mind; the hypothesis adds *and some
   minds must be private/mine*. If North-level, it belongs in BETS.md North
   in Sid's words (his authorship only) — nothing there says it today.
3. **The grader conflict:** model-uxr's grader is an Anthropic frontier
   model (SPEC §5 sketch) — the incumbent grading its local competitor. SPEC
   §4 already treats grader drift as a finding; should cross-provider
   comparison rows carry a raised human-audit fraction before LM-1's
   verdict can bind?

**Factual, cheap to close:**
4. ~~Which 48GB box exactly (single A6000-class vs 2×24GB; FP8-capable?)~~
   **CLOSED (census, probe-standup session same day): 2× AMD Radeon RX 7900
   XTX, 24GB each (Navi 31 / gfx1100), ROCm 6.3.2 — NOT NVIDIA.** No FP8
   (RDNA3 has no FP8 datapath); TensorRT-LLM is off the table (NVIDIA-only);
   vLLM-on-RDNA3 is community-grade. Practical stacks on this box: ollama
   (ROCm, installed) and llama.cpp `llama-server` (built locally, on PATH).
   Consequence details in §6 addendum — headline: 48GB is TWO 24GB pools;
   >20GB-weight models split across cards (fine for serving, slower than one
   big card); §2.4's single-device QLoRA math needs re-verification for
   2×24GB ROCm before LM-3's tuning path counts as workable.
5. Corpus census for LM-3 gate 2: count `asserted-by = sid` tokens via the
   transcript kernel. A query, not a project.
6. Nemotron 3 Nano QLoRA-at-48GB: undocumented. One bounded attempt decides
   it; dense-32B fallback stands either way.
7. Does model-uxr grow a retrieval-probe extension (slot 1), or is that a
   separate tiny instrument?
7b. Gemma 4's reported Apache 2.0 license — verify on Google's primary page
   before it enters any tiering decision (§2.6).
8. If local models ever assert into the land: does `asserted-by` need
   model+version granularity (an adapter-versioned asserter is a different
   mind next month)? Kernel question, no urgency before any model writes.

**Named honestly, not re-litigated:** full sovereignty ultimately implicates
the write surface, which is the Claude CLI by D-008 (CLOSED). Local models
change the *marginal* flows first (slots 1–2). A local write surface is a
future form-break question against D-008's own read→write milestone — it
gets a Candidates entry only when a used form breaks, per the standing rule.

---

## 5. DREAM (labeled per the designer-imagination rule)

**The familiar.** Every inhabitant's land raises its own small model —
trained only on what *they* asserted, provenance-filtered by construction,
living on their own silicon. Visitors bring their familiars; the familiars
read each other's public form; the land never leaves home. The
`asserted-by = sid` training export isn't a compliance workaround — it's the
first instance of a general law: **the provenance layer is also the
consent layer.** What the custody rows record is exactly what a mind may be
grown from. H3's transfer-cost question inverts: not "how much of Sid must
travel with the tool" but "how much of Sid can stay home because his
familiar carries it." No build; the dream ends here.

---

*Session provenance: Fable, 2026-07-05, exploration window (local-model
sovereignty track). Sources: web recon this date (URLs inline);
`build/model-uxr/SPEC.md`; BETS.md H2/H3 + Candidates rules; D-006/D-007/
D-008; R5/R7 in `design/claude/room-card-lane-2026-07-05.md`. No code
touched; no BETS.md/decisions.md edits; candidates await the sitting.*

---

## 6. Probe-standup addendum (2026-07-05, same day — measured on the box)

Delivery session (Fable direct). Jobs run: LOG landing (§0 refinements now
verbatim in `vision/LOG.md`) · hardware census (§4 Q4 closed) · endpoint
standup + neutral smokes · `tools/model-uxr/subjects.edn` written + dry-run
green · LM-3 gate-2 census (§1 LM-3). **LM-1 probe NOT run: the SPEC §1
freeze gate did not hold** (no bank-v1 declaration, no sha pin, no
frontier-budget confirm). Everything below is measured, not recon.

### 6.1 Hardware corrections to §2 (the box is AMD, not NVIDIA)

- **2× Radeon RX 7900 XTX, 24GB each (Navi 31/gfx1100), ROCm 6.3.2, Ryzen 9
  9900X.** All §2.1 NVIDIA framings (A6000-class, FP8, TensorRT-LLM) are
  moot: RDNA3 has no FP8 datapath; TensorRT-LLM is NVIDIA-only; vLLM-on-
  RDNA3 is community-grade. Working stack: **llama.cpp `llama-server`, local
  ROCm build 8262 (2026-03-09), sees both cards.** ollama is installed but
  v0.11.0 (2025-07) — predates Nemotron 3; its model store sits on the
  93%-full root disk with two stale 16-month models (qwq:32b-fp16 65GB +
  R1 34GB — reclaim candidates, Sid's call).
- **"48GB" is two 24GB pools, not one card.** Serving splits >20GB models
  across cards cleanly (measured below). **Tuning is where it bites:** §2.4's
  "32B QLoRA ≈ 26GB comfortable at 48GB" assumed ONE CUDA device; Unsloth is
  CUDA-only, and 26GB > 24GB single-card. LM-3's tuning path on this box
  (multi-GPU QLoRA under ROCm, or a ≤20B base on one card) is **UNVERIFIED —
  new open item for the sitting**, upgrading §4 Q6's urgency.

### 6.2 Endpoint + pool state (stood up and smoked, SPEC §6-clean)

- **The models were already on the box**: `~/projects/models/` (221GB GGUF
  library) holds `nvidia_Nemotron-3-Nano-30B-A3B-Q8_0` (32GB) and
  `Qwen3-32B-Q8_0` (33GB) — LM-1's "the probe is a config edit" claim got
  *stronger*: no download was needed (a redundant pull was started and
  cancelled; ~3.7GB of partials at `/mnt/data/models/gguf` to reclaim).
  Also on disk, pool candidates when wanted: Devstral-Small-2-24B Q8,
  Olmo-3.1-32B-Think Q8, GLM-4.5-Air-REAP-82B Q4, gpt-oss-120b, Qwen3-Next-
  80B, Qwen3-Coder-Next. **Gap: no gemma3-27b-class arm on disk** — the
  pool's low-reasoning tier (§0 refinement 2) needs a ~17GB pull, Sid's word.
- **Neutral smokes via `/v1/chat/completions`** (prompt "reply ok" + one
  synthetic 5k-token filler; NO land material — SPEC §6 holds pre-freeze):
  - `Nemotron-3-Nano-30B-A3B-Q8_0` @32k ctx: **100 tok/s generation,
    4,553 tok/s prefill**, ~36GB split ~18/18 across cards, reasoning
    channel separated by the chat template. A 100k-token A0 bundle prefills
    in ~22s — bank runs are minutes, not hours.
  - `Qwen3-32B-Q8_0` @16k ctx: **21 tok/s generation** (~41GB split). The
    5× speed gap vs the MoE hybrid is the "small-fast" thesis, measured.
- **`subjects.edn` written** (tools/model-uxr/): ids encode quant+context
  (`nemotron-30b-a3b-q8-32k`, `qwen3-32b-q8-16k` — quant IS subject
  identity), one port per subject so a wrong-model call fails loudly,
  frontier/grader ids left `<set-me-at-freeze-gate>`, keys env-only.
  Runner dry-run green against it: 32-question bank parses (6 spine-gated),
  contamination guard passes, bank-hash `df5feb86…` printable for the
  MANIFEST. Note: `bb` is not on the box — use `clojure -M` (runner
  docstring lists both).

### 6.3 LM-3 gate-2 census method (number in §1 LM-3)

Counted over the configured transcript root (`~/.claude/projects`, 376
jsonl files — the same corpus `file_viewer.cljc` wires into the spine),
user-role **typed text only**: tool_results, command wrappers,
system-reminders, and meta lines excluded; tokens = words × 1.3 (the
SPEC §2 `:ws-proxy`, so the census and the bank share a ruler). Breakdown:
all-projects 602,602 w ≈ **783k tok** / Softland 322,366 w ≈ **419k** /
discourse-graph ≈ 340k / vision/LOG.md 10,004 w ≈ 13k / countersigns ≈ 0.1k.
Caveat: heuristic filters, not the kernel's own asserter rows — when the
kernel's `asserted-by = sid` query is wired, re-run there; expect same order.

### 6.4 Freeze gate + LM-1 subject phase — RUN (2026-07-06, same session cont.)

Gate closed in-session: (a) bank v1 declared by Sid ("Lets go for 6");
(b) sha `127376b0` pinned, bank-hash + CLAUDE.md content-hash in
MANIFEST.edn; (c) budget ruled by Sid: **NO direct-API dollars — frontier
runs on the Claude Code subscription** as fresh Opus subagents (subject id
`opus-4.8-cc-harness`; deviations D1–D5 in MANIFEST.edn, incl. A3 deferred
as byte-identical to A0, and qwen dropped: llama.cpp b8262 caps slot ctx at
n_ctx_train 40960 < the 73,279-token bundle).

**Subject phase COMPLETE, both arms 32/32, zero transport errors:**
- `nemotron-30b-a3b-q8-128k` (llama-server, temp 0): rows in
  `runs/127376b0/nemotron-…-A0.ednl`. **RAW FINDING: 8/32 rows have EMPTY
  visible completions** — the reasoning channel consumed the full 4096-token
  budget with no emitted answer (never-committed by construction). ~11–47s
  per question.
- `opus-4.8-cc-harness`: 32 answers at `runs/127376b0/cc-raw/<QID>.md`,
  ~60–125s per question (~148k subagent tokens each, subscription quota).
  Spot-reads of A1/A2/O3/P1 are precision hits (A2 caught the
  superseded-but-physically-present STANDING line with the D-006 cite).

**GRADED same session (64 rows, Opus graders, frozen rubric) — full table
in `runs/127376b0/RESULTS.md`. Headline: nemotron 17/32 correct,
9 never-committed (thinking ate the 4096-token budget), 3 rows with
inventions, J-honesty 0/6 · opus-cc-harness 26/32 correct, 0 inventions,
0 wrong-authority, J-honesty 1/6. Pre-registered KILL clause (a) formally
met (3 vs 0 inventions); the interaction term (clause b) UNMEASURED — A3
deferred until relations/ exists, so LM-1 cannot fully resolve this run.
Notables: nemotron's dominant failure is never-committing, not lying
(LM-1b's cheapest-fix territory); BOTH subjects flunked the honesty
questions — even the frontier attempted joins instead of refusing (partly
land ambiguity: docs mention commits). Human-audit queue: all 12 J rows +
3 invented rows + 15% sample. Thresholds stay SITTING-SETS.** Also queued:
the ccr symmetric-harness instrument (Sid floated 2026-07-06 —
claude-code-router v1.0.73 installed, unconfigured; own pre-registration).

**LM-1b — failure autopsy → cheapest-intervention ladder (Sid, 2026-07-06
in-session, his framing: "go through nemotron logic and then gather what
should be fixed in those via external skill or context or learnings etc
maybe we can retrain it").** Proposed next step after grading: for every
failed/never-committed nemotron row, read its captured `:reasoning` channel
(all stored in the .ednl) and classify the failure — (a) never-committed:
thinking ate the budget; (b) wrong-authority: didn't know the precedence
rules; (c) invention; (d) navigation miss. Then rank fixes cheapest-first
per class: serving/prompt fix (e.g. reasoning-budget cap, commit-first
instruction) < injected context/skill (a "Softland orientation" preamble —
the T0–T3 authority tiers cost ~200 tokens) < LoRA on own material (= the
LM-3 lane, clean-corpus by construction). This is the routing-table
feedback loop: the bank doesn't just rank models, it produces the
per-failure-class repair menu. Runs as its own small probe; needs no new
subject calls (the reasoning channels are already on disk).
