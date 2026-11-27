(ns softland.runtime.permissions
  "Party ids and permissions for the runtime's proofs.

  In Loop 1 each function returns today's form — bare party ids, nested
  permission vectors, and facts on the holder or session — while the
  runtime and store are untouched and every check sentence stays as it is.
  Loop 4 flips these functions to the shapes of `CONTRACT.md` §2.1–2.3."
  (:refer-clojure :exclude [agent]))

;; ----------------------------------------------------------------- party ids

(defn person
  "A person party id in the store (`CONTRACT.md` §2.1: `:person.<id>` in Loop 4;
  `id` as given in Loop 1)."
  [id]
  id)

(defn agent
  "An agent party id in the store (`CONTRACT.md` §2.1: `:agent.<id>` in Loop 4;
  `id` as given in Loop 1)."
  [id]
  id)

(defn run
  "A run party id in the store (`CONTRACT.md` §2.1: `:run.<id>` in Loop 4;
  `id` as given in Loop 1)."
  [id]
  id)

;; --------------------------------------------------------------- permissions

(defn first-permission
  "A lane's first permission (`CONTRACT.md` §2.1, §2.3).
  `opts` (optional): `{:given-by who, :granted-in lane, :id uuid}`.
  Loop 1 returns `[holder lane granted-in]`."
  ([holder lane] (first-permission holder lane nil))
  ([holder lane {:keys [granted-in]}]
   [holder lane (or granted-in lane)]))

(defn given-permission
  "A permission given beneath `parent` for `lane` (`CONTRACT.md` §2.3).
  `allows-or-opts` (optional) is either an allows word (`:see` or `:see-and-act`)
  or a map `{:allows word, :granted-in lane, :given-by who, :id uuid, :lent-from uuid}`.
  Loop 1 returns `[holder lane granted-in parent]` or `[holder lane granted-in parent allows]`."
  ([holder lane parent] (given-permission holder lane parent nil))
  ([holder lane parent allows-or-opts]
   (let [opts (if (map? allows-or-opts) allows-or-opts {:allows allows-or-opts})
         allows (:allows opts)
         granted-in (:granted-in opts lane)]
     (cond-> [holder lane granted-in parent]
       (some? allows) (conj allows)))))

(defn run-permission
  "A sitting-scoped permission beneath `parent` (`CONTRACT.md` §2.3 `:session`).
  `opts` (optional): `{:granted-in lane, :given-by who, :id uuid, :lent-from uuid}`.
  Loop 1 returns `[holder lane granted-in parent allows [:run session]]`."
  ([holder lane parent session]
   (run-permission holder lane parent session :see-and-act nil))
  ([holder lane parent session allows]
   (run-permission holder lane parent session allows nil))
  ([holder lane parent session allows {:keys [granted-in]}]
   [holder lane (or granted-in lane) parent (or allows :see-and-act) [:run session]]))

;; ---------------------------------------------------- act slot & store facts

(defn permission-id
  "What an `:act` puts under `:permission` (`CONTRACT.md` §2.2).
  Loop 1: the permission vector itself. Loop 4: its `UUID`."
  [permission]
  permission)

(defn permission-fact
  "The fact map that grants `permission` (`CONTRACT.md` §2.3).
  `opts` (optional): `{:replaces fact-id}`."
  ([permission] (permission-fact permission nil))
  ([permission opts]
   (let [scope (nth permission 5 nil)
         [e k] (if (and (sequential? scope) (= :run (first scope)))
                 [(second scope) :softland.store/run-permission]
                 [(nth permission 0) :softland.store/permission])]
     (cond-> {:e e :k k :v permission}
       (contains? opts :replaces) (assoc :replaces (:replaces opts))))))

(defn end-fact
  "The fact map that ends `permission` (`CONTRACT.md` §2.3: `:v` absent/nil).
  `opts` (optional): `{:replaces fact-id}`."
  ([permission] (end-fact permission nil))
  ([permission opts]
   (dissoc (permission-fact permission opts) :v)))
