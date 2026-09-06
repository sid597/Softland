"""Independent expected geometry for the nonlinear-width counterexample.

The source is C(t)=(20t,0), radius R(t)=10t^2, 0<=t<=1.
Q=(10,5). The distance-to-disc function is 40-Lipschitz in t:
the centre moves at speed 20 and radius changes at at most speed 20.
Every t is within 1/(2N) of one sampled t, so the sampled minimum minus
40/(2N) is a lower bound for the entire continuous sweep, not just samples.
"""
import json
import math
from pathlib import Path

n = 100000
t, minimum = min(((i/n, math.hypot(10-20*i/n, 5)-10*(i/n)**2)
                  for i in range(n+1)), key=lambda row: row[1])
lower = minimum - 40/(2*n)
result = {"source": {"center": "(20t,0)", "radius": "10t^2", "t": [0, 1]},
          "query": [10, 5], "gridIntervals": n, "sampledMinimum": minimum,
          "sampledArgmin": t, "lipschitzConstant": 40,
          "certifiedDistanceLowerBound": lower,
          "pixelSquareRadius": math.sqrt(0.5),
          "wholePixelOutside": lower > math.sqrt(0.5)}
assert result["wholePixelOutside"]
Path(__file__).with_name("oracle.json").write_text(json.dumps(result, indent=2)+"\n")
print(json.dumps(result, indent=2))
