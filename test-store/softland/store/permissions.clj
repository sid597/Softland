(ns softland.store.permissions
  "Party ids and permissions for the store's proofs.

  In Loop 1 each function returns today's form — bare party keywords or UUIDs,
  nested permission vectors, and facts on the holder, the session or the yes's
  id — while `src-store/` is untouched and every check sentence stays as it is.
  Loop 4 flips these functions to the shapes of `CONTRACT.md` §2.1–2.3."
  (:refer-clojure :exclude [agent])
  (:require [softland.store.act :as act]
            [softland.store.gate.first-act :as first-act]))

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

(defn long-person
  "A person party keyword whose printed text has `n` characters, colon counted.
  Loop 1: `:aaa...`. Loop 4: `:person.aaa...` of the same length `n`."
  [n]
  (keyword (apply str (repeat (dec n) "a"))))

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

(defn act-permission
  "An exact-act yes permission on `yes-uuid` for `digest` (`CONTRACT.md` §2.3 `[:exact-act digest]`).
  `opts` (optional): `{:allows word, :granted-in lane, :given-by who, :lent-from uuid}`.
  Loop 1 returns `[holder lane granted-in parent allows [:act yes-uuid digest]]`."
  ([holder lane parent yes-uuid digest]
   (act-permission holder lane parent yes-uuid digest nil))
  ([holder lane parent yes-uuid digest {:keys [allows granted-in]}]
   [holder lane (or granted-in lane) parent (or allows :see-and-act) [:act yes-uuid digest]]))

(defn deep-permission
  "A chain of `n` permissions in `lane` held by `holder` (`n` >= 1, first counted),
  or `n` steps beneath `root` when `root` is given."
  ([holder lane n]
   (reduce (fn [parent _] (given-permission holder lane parent))
           (first-permission holder lane)
           (range (dec n))))
  ([holder lane root n]
   (reduce (fn [parent _] (given-permission holder lane parent))
           root
           (range n))))

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
   (let [[kind target] (act/covers permission)
         [e k] (case kind
                 :run [target :softland.store/run-permission]
                 :act [target :softland.store/act-permission]
                 [(nth permission 0) :softland.store/permission])]
     (cond-> {:e e :k k :v permission}
       (contains? opts :replaces) (assoc :replaces (:replaces opts))))))

(defn end-fact
  "The fact map that ends `permission` (`CONTRACT.md` §2.3: `:v` absent/nil).
  `opts` (optional): `{:replaces fact-id}`."
  ([permission] (end-fact permission nil))
  ([permission opts]
   (dissoc (permission-fact permission opts) :v)))

(defn end-act-id-fact
  "The fact map that ends an exact-act yes permission known by its `yes-uuid`
  (e.g. when building the maker's act before the yes's digest is computed).
  Loop 1: `{:e yes-uuid :k :softland.store/act-permission}`.
  Loop 4: `{:e yes-uuid :k :softland.store/permission}`."
  ([yes-uuid] (end-act-id-fact yes-uuid nil))
  ([yes-uuid opts]
   (cond-> {:e yes-uuid :k :softland.store/act-permission}
     (contains? opts :replaces) (assoc :replaces (:replaces opts)))))

(defn making-facts
  "The facts of a lane's making act (`CONTRACT.md` §2.1–2.3).
  Loop 1: depot fact and `holder`'s first permission fact."
  ([lane holder] (making-facts lane holder holder))
  ([lane holder given-by]
   (let [own (first-permission holder lane {:given-by given-by})
         depot (first-act/depot-at-making lane)]
     [{:e lane :k :softland.store/depot :v depot}
      (permission-fact own)])))

(defn making-act
  "A maker's making act for `lane` (`CONTRACT.md` §2.2).
  Loop 1 includes `:permission`; Loop 4 omits `:permission` on making acts."
  [lane who holder session]
  (let [own (first-permission holder lane {:given-by who})
        depot (first-act/depot-at-making lane)]
    {:version 1
     :who who
     :lane lane
     :permission (permission-id own)
     :session session
     :facts [{:e lane :k :softland.store/depot :v depot}
             (permission-fact own)]}))
