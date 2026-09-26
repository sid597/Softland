;; IMPORTANT: Before modifying this file, re-read PLAN-micro-store.md §B and
;; RIG.md R19 (the walk, not the cascade).
(ns rig.store.permit
  "The permission check both gates call (PLAN-micro-store.md §B, default R7;
  rig choices M20, M21; RIG.md R19). Pure and total.

  A permission id is P8's triple `[who layer in]`, or `[who layer in parent]`
  with `parent` a permission id: a narrower permission beneath its parent,
  at most four deep (root, session, agent, tool), which `env/pid?` enforces.
  A revoke writes one row; everything below it is cut because every check
  walks the cited permission's chain and meets the revoked row (R19: the
  walk, not the cascade, so a revoke never writes once per permission
  beneath it).

  The rows a check reads are the chain's own, one per element, on the task
  that holds the layer's permission index. For a re-classed layer the caller
  merges each element's rows from both stores first (M5: live iff granted in
  either and revoked in neither).

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key.")

(def max-depth "How many permissions one chain may hold (root, session, agent, tool)." 4)

(defn parent
  "The permission a narrower one sits beneath, or nil for a root triple."
  [pid]
  (when (and (vector? pid) (= 4 (count pid))) (nth pid 3)))

(defn chain
  "`[pid parent ... top]`, from the id alone, with no read: the cited
  permission first, its root last. [] for nil or anything that is not a
  vector; never longer than `max-depth` + 1, so a malformed id cannot make
  it walk far (the parser refuses a deeper one before any gate sees it)."
  [pid]
  (loop [p pid out []]
    (if (and (vector? p) (<= 3 (count p) 4) (<= (count out) max-depth))
      (recur (parent p) (conj out p))
      out)))

(defn- holder [pid] (nth pid 0))
(defn- layer-of [pid] (nth pid 1))
(defn- kept-in [pid] (nth pid 2))

(defn refusal
  "Why a cited permission does not let this offer write, or nil: one of the
  model's four reasons, in the model's order (model.clj `refusal`, under
  `:permissions-in-their-layer`). For an offer that is not exempt (the
  caller checks the exemption):

  - `:permission-does-not-cover-this` when the cited pid is nil, its holder
    is neither the offer's `:who` nor its `:session` (M21, [PV-F11]: the
    model's holders are persons and must be `:who`; a session may hold a
    permission beneath its person's), or its layer is not the offer's;
  - `:permission-from-another-layer` when the `in` of any element of its
    chain, or the layer of any ancestor, is not the offer's layer: a gate
    checks only permissions kept in layers it orders;
  - `:no-permission` when any element has no grant in `rows`;
  - `:permission-revoked` when any element is revoked.

  `rows` maps pid to its row, `{:granted fid :revoked fid}` (either nil)."
  [offer rows]
  (let [pid (:permission offer)
        layer (:layer offer)
        ids (chain pid)]
    (cond
      (or (empty? ids)
          (not (contains? (into #{} (remove nil?) [(:who offer) (:session offer)]) (holder pid)))
          (not= layer (layer-of pid)))
      :permission-does-not-cover-this

      (some #(or (not= layer (kept-in %)) (not= layer (layer-of %))) ids)
      :permission-from-another-layer

      (some #(nil? (:granted (get rows %))) ids)
      :no-permission

      (some #(some? (:revoked (get rows %))) ids)
      :permission-revoked)))
