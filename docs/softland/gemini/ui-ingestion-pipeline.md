# UI Ingestion Pipeline (MVP Plan)

> **Status:** Planning Phase (Brainstormed Feb 2026)
> **Goal:** Prove the end-to-end extraction of generic UI components (like Shadcn/React/HTML) into pure Clojure WebGPU `rt-node` blueprints without relying on the DOM or CSS.

## The Problem
We want to build a "Chameleon UI System" that can adopt the design feel of whatever data it is displaying (Linear, GitHub, Notion). To do this, we need a generalized pipeline that can ingest existing UI designs and convert them into our WebGPU-native layout format (`rt-node` trees). 

For the MVP, we are skipping the global Rama registry and proving the mechanics by saving the generated blueprints directly to the local codebase.

## The Architecture (MVP)

### 1. The Context (Engine Rules)
The Extractor (an LLM) needs to understand the target language. We define a strict schema based on our existing virtual layout engine:
- **Primitives:** `rt-node` (Quad, Text, Shadow).
- **Layout:** `:direction (:column/:row)`, `:gap`, `:padding`, `:align`.
- **Styling:** `dt` (Design Tokens) for colors, radii, and shadows.

### 2. The Extraction Step
You provide a raw UI artifact (e.g., Shadcn React code, Tailwind HTML). 
The AI acts as the Extractor. It translates the DOM cruft and CSS into a clean, parameterized Clojure function (the "Mold").

### 3. Local Storage (The Component Library)
Instead of a Rama Depot, the generated UI components will be saved into the project tree as pure ClojureScript files.
**Target Directory:** `src/app/client/webgpu/components/by_gemini/`

Each file will follow a standard namespace and signature:
```clojure
(ns app.client.webgpu.components.by-gemini.shadcn-card
  (:require [app.client.webgpu.loop :refer [rt-node dt]]))

(defn ui-shadcn-card [id bounds slots props]
  ;; Extracted layout and styling logic here...
  (rt-node id :container bounds ...))
```

### 4. Verification and Wiring
To prove the component works:
1. We import the generated component into `loop.cljs`.
2. We replace an existing hardcoded UI section (or build a small "component playground" screen) to render the new component.
3. We visually verify that our internal layout engine (`resolve-layout`) correctly positioned and sized everything without a browser engine.

## Step-by-Step Execution Plan

- [ ] **Step 1:** Create the target namespace directory (`src/app/client/webgpu/components/by_gemini/`).
- [ ] **Step 2:** Formulate the "Extractor Prompt" — a set of rules that teaches the AI how to map Tailwind/React to `rt-node`.
- [ ] **Step 3:** Feed the Extractor a specific target component (e.g., a Shadcn Button or Card).
- [ ] **Step 4:** Save the generated output to the new components directory.
- [ ] **Step 5:** Wire the generated component into `loop.cljs` and render it.
- [ ] **Step 6:** Refine the prompt based on visual and layout feedback until the extraction is 1-to-1 reliable.

## Future Generalization (Post-MVP)
Once we prove we can reliably extract and render components locally:
1. **Move to Rama:** Shift the storage from `.cljs` files to `*blueprints-depot` and `$$blueprints-pstate`.
2. **SCI Hydration:** Evaluate the EDN strings dynamically on the client so UI updates instantly across all instances without recompilation.
3. **Automated Verification:** Add headless mathematical verification via `resolve-layout` before accepting a blueprint.
