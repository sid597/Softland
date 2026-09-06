// The four records the executor runs on the bench and in Node (the fold of attack 4, 2026-09-06): the reach tool,
// the two-record sequence, the coating-reading brush (the definer's attack 4 records.json in the executor's grammar)
// and a dab through a retained clip. Plain data; the bench embeds this file verbatim, the panel's executor card loads
// one into its paste box, probe-4.mjs runs them in Node. A pasted record is the same JSON with any change Sid makes.
var X_RECORDS = {
  "reach": {
    "id": "reach@0",
    "what": "the reach tool on G (the definer's attack 2 at ×2): a region of surface distance about a seed, retained with its subject, bounds, area and chart pieces; another construction keeps it as a clip",
    "support": {
      "id": "G",
      "revision": 0
    },
    "tool": {
      "seed": [
        3.141592653589793,
        1.0471975511965976
      ],
      "radius": 150,
      "distance": "surface"
    },
    "program": {
      "steps": [
        {
          "out": "region",
          "op": "surface-region",
          "support": "support",
          "seed": "tool.seed",
          "radius": "tool.radius",
          "distance": "tool.distance"
        }
      ],
      "return": [
        "region"
      ]
    }
  },
  "sequence": {
    "id": "sequence@0",
    "what": "the two-record sequence on G (the definer's attack 2 §4 at ×2): one authored point on kA read through the retained bindings at G@0, G@1 (φ, a chart change) and G@2 (χ∘φ, the merge into M with the sheared chart); the same two source records compose to the same colour at every revision; at G@2 the merge step of B's binding costs one unit of work, and a demand that grants none is pending",
    "support": {
      "id": "G"
    },
    "coating": {
      "layers": [
        "A@0",
        "B@0"
      ],
      "order": [
        "kA",
        "kB"
      ]
    },
    "tool": {
      "curve": "kA/arc-AB",
      "t": 0.65
    },
    "events": [
      {
        "id": "G@0",
        "revision": 0
      },
      {
        "id": "G@1",
        "revision": 1
      },
      {
        "id": "G@2",
        "revision": 2
      }
    ],
    "program": {
      "each": "events",
      "state": {
        "last": null
      },
      "steps": [
        {
          "out": "at",
          "op": "curve-point",
          "curve": "tool.curve",
          "t": "tool.t"
        },
        {
          "out": "read",
          "op": "read-surface",
          "support": "support",
          "revision": "event.revision",
          "point": "at",
          "layers": [
            "coating"
          ],
          "order": "coating.order"
        }
      ],
      "next": {
        "last": "read"
      },
      "return": [
        "state.last"
      ]
    }
  },
  "pickup": {
    "id": "coating-pickup@0",
    "what": "the coating-reading brush on G (the definer's attack 4 records.json, in the executor's grammar): four authored dabs on kA's arc; each reads the composed coating through the retained bindings with the brush's own previous painting above it, mixes the sample into the carry, constructs a disc of surface distance and paints it at half opacity",
    "support": {
      "id": "G",
      "revision": 2
    },
    "coating": {
      "layers": [
        "A@0",
        "B@0"
      ],
      "bindings": [
        "binding-A@2",
        "binding-B@2"
      ],
      "order": [
        "kA",
        "kB"
      ],
      "interpretation": "linear-light premultiplied rgba; no lighting, no screen lease"
    },
    "painting": {
      "id": "pickup-G",
      "grid": [
        64,
        32
      ],
      "domain": [
        588.3185307179587,
        194,
        80,
        30
      ],
      "chart": "S0",
      "filter": "nearest",
      "initial": [
        0,
        0,
        0,
        0
      ]
    },
    "tool": {
      "carry": [
        1,
        0,
        0,
        1
      ],
      "pickup": 0.25,
      "opacity": 0.5,
      "radius": 8.75
    },
    "events": [
      {
        "id": "dab-0",
        "curve": "kA/arc-AB",
        "t": 0.65
      },
      {
        "id": "dab-1",
        "curve": "kA/arc-AB",
        "t": 0.7
      },
      {
        "id": "dab-2",
        "curve": "kA/arc-AB",
        "t": 0.41
      },
      {
        "id": "dab-3",
        "curve": "kA/arc-AB",
        "t": 0.65
      }
    ],
    "program": {
      "each": "events",
      "state": {
        "carry": "tool.carry",
        "painting": "painting.initial"
      },
      "steps": [
        {
          "out": "at",
          "op": "curve-point",
          "curve": "event.curve",
          "t": "event.t"
        },
        {
          "out": "picked",
          "op": "read-surface",
          "support": "support",
          "point": "at",
          "layers": [
            "coating",
            "state.painting"
          ],
          "order": "coating.order",
          "filter": "painting.filter"
        },
        {
          "out": "carry",
          "op": "mix",
          "a": "state.carry",
          "b": "picked.color",
          "amount": "tool.pickup"
        },
        {
          "out": "footprint",
          "op": "surface-region",
          "support": "support",
          "point": "at",
          "radius": "tool.radius",
          "distance": "surface"
        },
        {
          "out": "painting",
          "op": "paint",
          "painting": "state.painting",
          "region": "footprint",
          "rgba": "carry",
          "opacity": "tool.opacity"
        }
      ],
      "next": {
        "carry": "carry",
        "painting": "painting"
      },
      "return": [
        "state.painting",
        "state.carry"
      ]
    }
  },
  "clip": {
    "id": "clip-dab@0",
    "what": "reuse of a retained result: one dab of the coating brush's pigment painted at a point 144.55 mm from the reach seed, deposited only through a surface region another record returned (the reach tool's), supplied as the input `clip` with its subject declared; a region under a different subject is refused",
    "support": {
      "id": "G",
      "revision": 0
    },
    "inputs": {
      "clip": {
        "kind": "surface-region",
        "id": "<the id of the reach record's returned region>"
      }
    },
    "painting": {
      "id": "clip-G",
      "grid": [
        64,
        32
      ],
      "domain": [
        900,
        180,
        100,
        60
      ],
      "chart": "S0",
      "filter": "nearest",
      "initial": [
        0,
        0,
        0,
        0
      ]
    },
    "tool": {
      "seed": [
        4.71238898038469,
        1.0471975511965976
      ],
      "radius": 4,
      "rgba": [
        0.5,
        0,
        0,
        0.5
      ],
      "opacity": 1
    },
    "events": [
      {
        "id": "dab-q1"
      }
    ],
    "program": {
      "each": "events",
      "state": {
        "painting": "painting.initial"
      },
      "steps": [
        {
          "out": "footprint",
          "op": "surface-region",
          "support": "support",
          "seed": "tool.seed",
          "radius": "tool.radius",
          "distance": "surface"
        },
        {
          "out": "painting",
          "op": "paint",
          "painting": "state.painting",
          "region": "footprint",
          "rgba": "tool.rgba",
          "opacity": "tool.opacity",
          "clip": "inputs.clip"
        }
      ],
      "next": {
        "painting": "painting"
      },
      "return": [
        "state.painting"
      ]
    }
  }
};
