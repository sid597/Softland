# Auth verification

The checks of Auth, the module that holds sign-in. Their source is
[Auth's spec](../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md).

| Immediate child | Responsibility |
|---|---|
| [softland/auth/](softland/auth/README.md) | The reference of Auth's contract in plain Clojure, the proof that holds it to the spec, and the questions of the spec. |

```sh
clojure -M:auth -m softland.auth.reference-proof
```

The alias `:auth` of `deps.edn` puts `src-auth` and `test-auth` on the classpath,
with Clojure and Rama. The reference and its proof need nothing beyond Clojure:
no cluster, and no library passed with `-Sdeps`.
